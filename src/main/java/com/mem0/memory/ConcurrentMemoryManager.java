package com.mem0.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mem0.concurrency.cache.HighPerformanceCache;
import com.mem0.concurrency.ConcurrencyController;
import com.mem0.concurrency.ResourcePool;
import com.mem0.pipeline.AsyncMemoryPipeline;
import com.mem0.monitoring.PerformanceMonitor;
import com.mem0.memory.MemoryShard.MemoryShardStats;
import com.mem0.model.ResourcePoolStats;
import com.mem0.core.EnhancedMemory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * 并发内存管理器
 * 提供高性能的内存创建、更新、查询、删除和生命周期管理
 * 支持内存分片、负载均衡、冲突解决和一致性保证
 */
public class ConcurrentMemoryManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ConcurrentMemoryManager.class);
    
    // 核心组件
    private final AsyncMemoryPipeline pipeline;
    private final ConcurrencyController concurrencyController;
    private final PerformanceMonitor performanceMonitor;
    
    // 分片管理
    private final int shardCount;
    private final MemoryShard[] shards;
    private final ReadWriteLock shardLock = new ReentrantReadWriteLock();
    
    // 缓存层
    private final HighPerformanceCache<String, EnhancedMemory> memoryCache;
    private final HighPerformanceCache<String, List<String>> userMemoryCache;
    private final HighPerformanceCache<String, ConflictResolution> conflictCache;
    
    // 冲突解决
    private final ConflictResolver conflictResolver;
    
    // 生命周期管理
    private final ScheduledExecutorService lifecycleExecutor;
    private final long memoryTtlMs;
    private final long cleanupIntervalMs;
    
    // 统计信息
    private final AtomicLong totalMemories = new AtomicLong(0);
    private final AtomicLong totalCreated = new AtomicLong(0);
    private final AtomicLong totalUpdated = new AtomicLong(0);
    private final AtomicLong totalDeleted = new AtomicLong(0);
    private final AtomicInteger conflictsResolved = new AtomicInteger(0);
    
    // 批处理队列
    private final Map<String, List<MemoryOperation>> batchQueues = new ConcurrentHashMap<>();
    private final ScheduledExecutorService batchProcessor;
    
    private volatile boolean isShutdown = false;

    public ConcurrentMemoryManager(AsyncMemoryPipeline pipeline,
                                 ConcurrencyController concurrencyController,
                                 PerformanceMonitor performanceMonitor) {
        this(pipeline, concurrencyController, performanceMonitor, 16, 86400000, 300000); // 16分片, 24小时TTL, 5分钟清理
    }

    public ConcurrentMemoryManager(AsyncMemoryPipeline pipeline,
                                 ConcurrencyController concurrencyController,
                                 PerformanceMonitor performanceMonitor,
                                 int shardCount,
                                 long memoryTtlMs,
                                 long cleanupIntervalMs) {
        this.pipeline = pipeline;
        this.concurrencyController = concurrencyController;
        this.performanceMonitor = performanceMonitor;
        this.shardCount = shardCount;
        this.memoryTtlMs = memoryTtlMs;
        this.cleanupIntervalMs = cleanupIntervalMs;
        
        // 初始化分片
        this.shards = new MemoryShard[shardCount];
        for (int i = 0; i < shardCount; i++) {
            this.shards[i] = new MemoryShard(i);
        }
        
        // 初始化缓存
        this.memoryCache = new HighPerformanceCache<>(50000, memoryTtlMs, cleanupIntervalMs);
        this.userMemoryCache = new HighPerformanceCache<>(10000, 1800000, 300000); // 30分钟TTL
        this.conflictCache = new HighPerformanceCache<>(1000, 3600000, 600000);   // 1小时TTL
        
        // 初始化冲突解决器
        this.conflictResolver = new ConflictResolver();
        
        // 初始化生命周期管理
        this.lifecycleExecutor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "memory-lifecycle-manager");
            t.setDaemon(true);
            return t;
        });
        
        // 初始化批处理
        this.batchProcessor = Executors.newScheduledThreadPool(4, r -> {
            Thread t = new Thread(r, "memory-batch-processor");
            t.setDaemon(true);
            return t;
        });
        
        startBackgroundTasks();
        logger.info("并发内存管理器初始化完成 - 分片数: {}, TTL: {}ms", shardCount, memoryTtlMs);
    }

    /**
     * 创建内存（高并发优化）
     */
    public CompletableFuture<String> createMemory(String content, String userId, Map<String, Object> metadata) {
        return concurrencyController.executeControlledRequest(userId, "createMemory", () -> {
            performanceMonitor.incrementCounter("memory_manager.create.requests");
            
            return performanceMonitor.measureTime("memory_manager.create", () -> {
                // 检查冲突
                ConflictResolution conflict = checkForConflicts(content, userId);
                if (conflict != null) {
                    return handleConflictedCreation(content, userId, metadata, conflict);
                }
                
                // 使用管道创建
                return pipeline.createMemoryAsync(content, userId, metadata)
                    .thenApply(memoryId -> {
                        // 更新本地状态
                        EnhancedMemory memory = new EnhancedMemory(memoryId, content, userId, metadata);
                        updateLocalState(memory);
                        
                        totalCreated.incrementAndGet();
                        totalMemories.incrementAndGet();
                        
                        performanceMonitor.incrementCounter("memory_manager.create.success");
                        logger.debug("内存创建成功: {} for user: {}", memoryId, userId);
                        
                        return memoryId;
                    });
            });
        }).thenCompose(future -> future);
    }

    /**
     * 批量创建内存
     */
    public CompletableFuture<List<String>> createMemoriesBatch(List<MemoryCreationRequest> requests) {
        String batchId = "batch_" + System.currentTimeMillis();
        
        return concurrencyController.executeControlledRequest("system", "createBatch", () -> {
            performanceMonitor.incrementCounter("memory_manager.batch_create.requests");
            
            return performanceMonitor.measureTime("memory_manager.batch_create", () -> {
                logger.info("开始批量创建内存，批次ID: {}, 数量: {}", batchId, requests.size());
                
                // 按用户分组以优化冲突检测
                Map<String, List<MemoryCreationRequest>> userGroups = requests.stream()
                    .collect(Collectors.groupingBy(req -> req.getUserId()));
                
                List<CompletableFuture<List<String>>> groupFutures = new ArrayList<>();
                
                for (Map.Entry<String, List<MemoryCreationRequest>> entry : userGroups.entrySet()) {
                    String userId = entry.getKey();
                    List<MemoryCreationRequest> userRequests = entry.getValue();
                    
                    CompletableFuture<List<String>> groupFuture = processUserBatch(userId, userRequests);
                    groupFutures.add(groupFuture);
                }
                
                return CompletableFuture.allOf(groupFutures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        List<String> allResults = new ArrayList<>();
                        for (CompletableFuture<List<String>> future : groupFutures) {
                            try {
                                allResults.addAll(future.get());
                            } catch (Exception e) {
                                logger.error("批处理组失败", e);
                            }
                        }
                        
                        performanceMonitor.incrementCounter("memory_manager.batch_create.success");
                        logger.info("批量内存创建完成，批次ID: {}, 成功: {}", batchId, allResults.size());
                        
                        return allResults;
                    });
            });
        }).thenCompose(future -> future);
    }

    /**
     * 更新内存
     */
    public CompletableFuture<Boolean> updateMemory(String memoryId, String newContent, Map<String, Object> newMetadata) {
        return concurrencyController.executeControlledRequest("system", "updateMemory", () -> {
            performanceMonitor.incrementCounter("memory_manager.update.requests");
            
            return performanceMonitor.measureTime("memory_manager.update", () -> {
                // 获取现有内存
                return getMemory(memoryId)
                    .thenCompose(existingMemory -> {
                        if (existingMemory == null) {
                            logger.warn("尝试更新不存在的内存: {}", memoryId);
                            return CompletableFuture.completedFuture(false);
                        }
                        
                        // 检查更新冲突
                        ConflictResolution conflict = checkForUpdateConflicts(memoryId, newContent, existingMemory);
                        if (conflict != null) {
                            return handleConflictedUpdate(memoryId, newContent, newMetadata, existingMemory, conflict);
                        }
                        
                        // 执行更新
                        return pipeline.updateMemoryAsync(memoryId, newContent, newMetadata)
                            .thenApply(success -> {
                                if (success) {
                                    // 更新本地状态
                                    EnhancedMemory updatedMemory = new EnhancedMemory(
                                        memoryId, newContent, existingMemory.getUserId(), newMetadata);
                                    updatedMemory.setCreatedAt(existingMemory.getCreatedAt());
                                    updatedMemory.markAsUpdated();
                                    
                                    updateLocalState(updatedMemory);
                                    totalUpdated.incrementAndGet();
                                    
                                    performanceMonitor.incrementCounter("memory_manager.update.success");
                                    logger.debug("内存更新成功: {}", memoryId);
                                }
                                
                                return success;
                            });
                    });
            });
        }).thenCompose(future -> future);
    }

    /**
     * 删除内存
     */
    public CompletableFuture<Boolean> deleteMemory(String memoryId) {
        return concurrencyController.executeControlledRequest("system", "deleteMemory", () -> {
            performanceMonitor.incrementCounter("memory_manager.delete.requests");
            
            return performanceMonitor.measureTime("memory_manager.delete", () -> {
                return pipeline.deleteMemoryAsync(memoryId)
                    .thenApply(success -> {
                        if (success) {
                            removeFromLocalState(memoryId);
                            totalDeleted.incrementAndGet();
                            totalMemories.decrementAndGet();
                            
                            performanceMonitor.incrementCounter("memory_manager.delete.success");
                            logger.debug("内存删除成功: {}", memoryId);
                        }
                        
                        return success;
                    });
            });
        }).thenCompose(future -> future);
    }

    /**
     * 获取内存
     */
    public CompletableFuture<EnhancedMemory> getMemory(String memoryId) {
        // 快速缓存查找
        EnhancedMemory cached = memoryCache.get(memoryId);
        if (cached != null) {
            performanceMonitor.incrementCounter("memory_manager.get.cache_hit");
            return CompletableFuture.completedFuture(cached);
        }
        
        return concurrencyController.executeControlledRequest("system", "getMemory", () -> {
            performanceMonitor.incrementCounter("memory_manager.get.requests");
            
            // 从分片查找
            MemoryShard shard = getShardForMemory(memoryId);
            EnhancedMemory memory = shard.getMemory(memoryId);
            
            if (memory != null) {
                memoryCache.put(memoryId, memory);
                performanceMonitor.incrementCounter("memory_manager.get.found");
                return memory;
            }
            
            performanceMonitor.incrementCounter("memory_manager.get.not_found");
            return null;
        });
    }

    /**
     * 搜索相似内存
     */
    public CompletableFuture<List<EnhancedMemory>> searchSimilarMemories(String query, String userId, 
                                                                        int limit, float threshold) {
        return concurrencyController.executeControlledRequest(userId, "searchMemories", () -> {
            performanceMonitor.incrementCounter("memory_manager.search.requests");
            
            return performanceMonitor.measureTime("memory_manager.search", () -> {
                return pipeline.searchSimilarMemoriesAsync(query, userId, limit, threshold)
                    .thenApply(memories -> {
                        // 应用后处理过滤和排序
                        List<EnhancedMemory> processedMemories = postProcessSearchResults(memories, userId);
                        
                        performanceMonitor.incrementCounter("memory_manager.search.success");
                        logger.debug("相似内存搜索完成，查询: {}, 结果数: {}", query, processedMemories.size());
                        
                        return processedMemories;
                    });
            });
        }).thenCompose(future -> future);
    }

    /**
     * 获取用户所有内存
     */
    public CompletableFuture<List<EnhancedMemory>> getUserMemories(String userId) {
        // 检查缓存
        List<String> cachedIds = userMemoryCache.get(userId);
        if (cachedIds != null) {
            List<CompletableFuture<EnhancedMemory>> futures = cachedIds.stream()
                .map(this::getMemory)
                .collect(Collectors.toList());
            
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    return futures.stream()
                        .map(future -> {
                            try {
                                return future.get();
                            } catch (Exception e) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                });
        }
        
        return concurrencyController.executeControlledRequest(userId, "getUserMemories", () -> {
            performanceMonitor.incrementCounter("memory_manager.get_user_memories.requests");
            
            List<EnhancedMemory> userMemories = new ArrayList<>();
            List<String> memoryIds = new ArrayList<>();
            
            // 从所有分片收集用户内存
            for (MemoryShard shard : shards) {
                List<EnhancedMemory> shardMemories = shard.getUserMemories(userId);
                userMemories.addAll(shardMemories);
                memoryIds.addAll(shardMemories.stream()
                    .map(EnhancedMemory::getId)
                    .collect(Collectors.toList()));
            }
            
            // 缓存用户内存ID列表
            userMemoryCache.put(userId, memoryIds);
            
            performanceMonitor.incrementCounter("memory_manager.get_user_memories.success");
            logger.debug("获取用户内存完成，用户: {}, 数量: {}", userId, userMemories.size());
            
            return userMemories;
        });
    }

    /**
     * 获取管理器统计信息
     */
    public MemoryManagerStats getStats() {
        List<MemoryShardStats> shardStats = Arrays.stream(shards)
            .map(MemoryShard::getStats)
            .collect(Collectors.toList());
        
        return new MemoryManagerStats(
            totalMemories.get(),
            totalCreated.get(),
            totalUpdated.get(),
            totalDeleted.get(),
            conflictsResolved.get(),
            shardStats,
            memoryCache.getStats(),
            userMemoryCache.getStats(),
            conflictCache.getStats()
        );
    }

    /**
     * 健康检查
     */
    public HealthStatus checkHealth() {
        List<String> issues = new ArrayList<>();
        
        // 检查分片健康状态
        for (MemoryShard shard : shards) {
            if (shard.getMemoryCount() > 10000) { // 单分片内存过多
                issues.add("分片 " + shard.getId() + " 内存数过多: " + shard.getMemoryCount());
            }
        }
        
        // 检查缓存命中率
        double cacheHitRate = memoryCache.getStats().getHitRate();
        if (cacheHitRate < 0.7) {
            issues.add("内存缓存命中率过低: " + String.format("%.2f%%", cacheHitRate * 100));
        }
        
        // 检查冲突率
        long totalOps = totalCreated.get() + totalUpdated.get();
        if (totalOps > 0) {
            double conflictRate = (double) conflictsResolved.get() / totalOps;
            if (conflictRate > 0.1) {
                issues.add("冲突率过高: " + String.format("%.2f%%", conflictRate * 100));
            }
        }
        
        boolean isHealthy = issues.isEmpty();
        return new HealthStatus(isHealthy, issues);
    }

    /**
     * 关闭管理器
     */
    public CompletableFuture<Void> shutdown() {
        logger.info("开始关闭并发内存管理器");
        isShutdown = true;
        
        return CompletableFuture.runAsync(() -> {
            try {
                // 停止批处理器
                batchProcessor.shutdown();
                batchProcessor.awaitTermination(30, TimeUnit.SECONDS);
                
                // 停止生命周期管理
                lifecycleExecutor.shutdown();
                lifecycleExecutor.awaitTermination(30, TimeUnit.SECONDS);
                
                // 关闭缓存
                memoryCache.shutdown();
                userMemoryCache.shutdown();
                conflictCache.shutdown();
                
                // 清理分片
                for (MemoryShard shard : shards) {
                    shard.clear();
                }
                
                logger.info("并发内存管理器关闭完成");
                
            } catch (Exception e) {
                logger.error("关闭内存管理器时发生错误", e);
            }
        });
    }

    // 私有辅助方法

    private void startBackgroundTasks() {
        // 定期清理过期内存
        lifecycleExecutor.scheduleAtFixedRate(this::cleanupExpiredMemories, 
            cleanupIntervalMs, cleanupIntervalMs, TimeUnit.MILLISECONDS);
        
        // 定期处理批量操作
        batchProcessor.scheduleAtFixedRate(this::processBatchQueues, 
            1000, 1000, TimeUnit.MILLISECONDS);
            
        // 定期重新平衡分片
        lifecycleExecutor.scheduleAtFixedRate(this::rebalanceShards, 
            1800000, 1800000, TimeUnit.MILLISECONDS); // 30分钟
    }

    private MemoryShard getShardForMemory(String memoryId) {
        int shardIndex = Math.abs(memoryId.hashCode()) % shardCount;
        return shards[shardIndex];
    }

    private MemoryShard getShardForUser(String userId) {
        int shardIndex = Math.abs(userId.hashCode()) % shardCount;
        return shards[shardIndex];
    }

    private void updateLocalState(EnhancedMemory memory) {
        MemoryShard shard = getShardForMemory(memory.getId());
        shard.addMemory(memory);
        memoryCache.put(memory.getId(), memory);
        
        // 清理用户内存缓存
        userMemoryCache.remove(memory.getUserId());
    }

    private void removeFromLocalState(String memoryId) {
        MemoryShard shard = getShardForMemory(memoryId);
        EnhancedMemory memory = shard.removeMemory(memoryId);
        memoryCache.remove(memoryId);
        
        // 清理用户内存缓存
        if (memory != null) {
            userMemoryCache.remove(memory.getUserId());
        }
    }

    private ConflictResolution checkForConflicts(String content, String userId) {
        String conflictKey = generateConflictKey(content, userId);
        return conflictCache.get(conflictKey);
    }

    private ConflictResolution checkForUpdateConflicts(String memoryId, String newContent, EnhancedMemory existing) {
        // 检查内容相似性冲突
        if (conflictResolver.isSimilarContent(newContent, existing.getContent())) {
            ConflictResolution resolution = conflictResolver.resolveUpdateConflict(memoryId, newContent, existing);
            conflictCache.put(memoryId + "_update", resolution);
            return resolution;
        }
        
        return null;
    }

    private CompletableFuture<String> handleConflictedCreation(String content, String userId, 
                                                             Map<String, Object> metadata, ConflictResolution conflict) {
        conflictsResolved.incrementAndGet();
        performanceMonitor.incrementCounter("memory_manager.conflicts.creation");
        
        switch (conflict.getStrategy()) {
            case MERGE:
                return pipeline.createMemoryAsync(conflict.getMergedContent(), userId, metadata);
            case REPLACE:
                return deleteExistingAndCreate(conflict.getExistingMemoryId(), content, userId, metadata);
            case IGNORE:
                return CompletableFuture.completedFuture(conflict.getExistingMemoryId());
            default:
                return pipeline.createMemoryAsync(content, userId, metadata);
        }
    }

    private CompletableFuture<Boolean> handleConflictedUpdate(String memoryId, String newContent, 
                                                            Map<String, Object> newMetadata, EnhancedMemory existing, ConflictResolution conflict) {
        conflictsResolved.incrementAndGet();
        performanceMonitor.incrementCounter("memory_manager.conflicts.update");
        
        switch (conflict.getStrategy()) {
            case MERGE:
                return pipeline.updateMemoryAsync(memoryId, conflict.getMergedContent(), newMetadata);
            case REPLACE:
                return pipeline.updateMemoryAsync(memoryId, newContent, newMetadata);
            case IGNORE:
                return CompletableFuture.completedFuture(true);
            default:
                return pipeline.updateMemoryAsync(memoryId, newContent, newMetadata);
        }
    }

    private CompletableFuture<String> deleteExistingAndCreate(String existingId, String content, 
                                                            String userId, Map<String, Object> metadata) {
        return pipeline.deleteMemoryAsync(existingId)
            .thenCompose(deleted -> {
                if (deleted) {
                    return pipeline.createMemoryAsync(content, userId, metadata);
                } else {
                    return CompletableFuture.completedFuture(existingId);
                }
            });
    }

    private CompletableFuture<List<String>> processUserBatch(String userId, List<MemoryCreationRequest> requests) {
        // 转换为管道批处理请求
        List<AsyncMemoryPipeline.MemoryCreationRequest> pipelineRequests = requests.stream()
            .map(req -> new AsyncMemoryPipeline.MemoryCreationRequest(req.getContent(), req.getUserId(), req.getMetadata()))
            .collect(Collectors.toList());
        
        return pipeline.createMemoriesBatch(pipelineRequests);
    }

    private List<EnhancedMemory> postProcessSearchResults(List<EnhancedMemory> memories, String userId) {
        return memories.stream()
            .filter(memory -> memory.getUserId().equals(userId)) // 安全过滤
            .filter(memory -> !memory.isExpired(memoryTtlMs))     // 过滤过期内存
            .sorted((a, b) -> b.getLastAccessedAt().compareTo(a.getLastAccessedAt())) // 按访问时间排序
            .collect(Collectors.toList());
    }

    private String generateConflictKey(String content, String userId) {
        return userId + "_" + content.hashCode();
    }

    private void cleanupExpiredMemories() {
        if (isShutdown) {
            return;
        }
        
        logger.debug("开始清理过期内存");
        int cleaned = 0;
        
        for (MemoryShard shard : shards) {
            cleaned += shard.cleanupExpiredMemories(memoryTtlMs);
        }
        
        if (cleaned > 0) {
            totalDeleted.addAndGet(cleaned);
            totalMemories.addAndGet(-cleaned);
            performanceMonitor.incrementCounter("memory_manager.cleanup.expired", cleaned);
            logger.info("清理过期内存完成，清理数量: {}", cleaned);
        }
    }

    private void processBatchQueues() {
        // 处理批处理队列的逻辑
        // 这里简化实现，在实际项目中应该实现完整的批处理逻辑
    }

    private void rebalanceShards() {
        if (isShutdown) {
            return;
        }
        
        logger.debug("开始分片重新平衡");
        
        // 统计各分片的负载
        long totalShardMemories = 0;
        int[] shardSizes = new int[shardCount];
        
        for (int i = 0; i < shardCount; i++) {
            shardSizes[i] = shards[i].getMemoryCount();
            totalShardMemories += shardSizes[i];
        }
        
        if (totalShardMemories == 0) {
            return;
        }
        
        // 计算平均负载
        double averageLoad = (double) totalShardMemories / shardCount;
        double threshold = averageLoad * 0.2; // 20%的阈值
        
        // 检查是否需要重新平衡
        boolean needRebalance = false;
        for (int shardSize : shardSizes) {
            if (Math.abs(shardSize - averageLoad) > threshold) {
                needRebalance = true;
                break;
            }
        }
        
        if (needRebalance) {
            performanceMonitor.incrementCounter("memory_manager.rebalance.triggered");
            logger.info("分片负载不均衡，触发重新平衡。平均负载: {}", averageLoad);
            // 在实际项目中，这里应该实现分片重新平衡的逻辑
        }
    }

    // 内部类声明
    
    public static class MemoryCreationRequest {
        private final String content;
        private final String userId;
        private final Map<String, Object> metadata;

        public MemoryCreationRequest(String content, String userId, Map<String, Object> metadata) {
            this.content = content;
            this.userId = userId;
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }

        public String getContent() { return content; }
        public String getUserId() { return userId; }
        public Map<String, Object> getMetadata() { return metadata; }
    }

    public enum ConflictStrategy {
        MERGE, REPLACE, IGNORE, CREATE_NEW
    }

    public static class ConflictResolution {
        private final ConflictStrategy strategy;
        private final String existingMemoryId;
        private final String mergedContent;

        public ConflictResolution(ConflictStrategy strategy, String existingMemoryId, String mergedContent) {
            this.strategy = strategy;
            this.existingMemoryId = existingMemoryId;
            this.mergedContent = mergedContent;
        }

        public ConflictStrategy getStrategy() { return strategy; }
        public String getExistingMemoryId() { return existingMemoryId; }
        public String getMergedContent() { return mergedContent; }
    }

    public static class HealthStatus {
        private final boolean healthy;
        private final List<String> issues;

        public HealthStatus(boolean healthy, List<String> issues) {
            this.healthy = healthy;
            this.issues = new ArrayList<>(issues);
        }

        public boolean isHealthy() { return healthy; }
        public List<String> getIssues() { return Collections.unmodifiableList(issues); }
    }

    public static class MemoryManagerStats {
        private final long totalMemories;
        private final long totalCreated;
        private final long totalUpdated;
        private final long totalDeleted;
        private final int conflictsResolved;
        private final List<MemoryShardStats> shardStats;
        private final HighPerformanceCache.CacheStats memoryCacheStats;
        private final HighPerformanceCache.CacheStats userCacheStats;
        private final HighPerformanceCache.CacheStats conflictCacheStats;

        public MemoryManagerStats(long totalMemories, long totalCreated, long totalUpdated, long totalDeleted,
                                int conflictsResolved, List<MemoryShardStats> shardStats,
                                HighPerformanceCache.CacheStats memoryCacheStats,
                                HighPerformanceCache.CacheStats userCacheStats,
                                HighPerformanceCache.CacheStats conflictCacheStats) {
            this.totalMemories = totalMemories;
            this.totalCreated = totalCreated;
            this.totalUpdated = totalUpdated;
            this.totalDeleted = totalDeleted;
            this.conflictsResolved = conflictsResolved;
            this.shardStats = new ArrayList<>(shardStats);
            this.memoryCacheStats = memoryCacheStats;
            this.userCacheStats = userCacheStats;
            this.conflictCacheStats = conflictCacheStats;
        }

        // Getter方法
        public long getTotalMemories() { return totalMemories; }
        public long getTotalCreated() { return totalCreated; }
        public long getTotalUpdated() { return totalUpdated; }
        public long getTotalDeleted() { return totalDeleted; }
        public int getConflictsResolved() { return conflictsResolved; }
        public List<MemoryShardStats> getShardStats() { return Collections.unmodifiableList(shardStats); }
        public HighPerformanceCache.CacheStats getMemoryCacheStats() { return memoryCacheStats; }
        public HighPerformanceCache.CacheStats getUserCacheStats() { return userCacheStats; }
        public HighPerformanceCache.CacheStats getConflictCacheStats() { return conflictCacheStats; }

        @Override
        public String toString() {
            return String.format("MemoryManagerStats{总内存=%d, 创建=%d, 更新=%d, 删除=%d, 冲突解决=%d, 分片数=%d, 内存缓存=%s}",
                totalMemories, totalCreated, totalUpdated, totalDeleted, conflictsResolved, shardStats.size(), memoryCacheStats);
        }
    }
}