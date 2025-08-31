package com.mem0.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mem0.performance.ConcurrentExecutionManager;
import com.mem0.embedding.EmbeddingProvider;
import com.mem0.store.VectorStore;
import com.mem0.store.GraphStore;
import com.mem0.memory.Memory;
import com.mem0.core.EnhancedMemory;
import com.mem0.concurrency.cache.HighPerformanceCache;
import com.mem0.monitoring.PerformanceMonitor;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 异步内存处理管道 / Asynchronous Memory Processing Pipeline
 * 
 * 高性能的异步内存处理管道，提供内存的创建、更新、查询和删除操作。
 * 支持并发控制、批处理优化、智能重试机制、性能监控和缓存管理。
 * High-performance asynchronous memory processing pipeline providing memory creation, update, query, and deletion operations.
 * Supports concurrency control, batch processing optimization, intelligent retry mechanisms, performance monitoring, and cache management.
 * 
 * <h3>核心功能 / Key Features:</h3>
 * <ul>
 *   <li>异步批处理和并发控制 / Asynchronous batch processing and concurrency control</li>
 *   <li>智能重试和错误恢复 / Intelligent retry and error recovery</li>
 *   <li>多级缓存和性能优化 / Multi-level caching and performance optimization</li>
 *   <li>实时监控和统计分析 / Real-time monitoring and statistical analysis</li>
 *   <li>背压控制和资源管理 / Backpressure control and resource management</li>
 *   <li>管道预热和优雅关闭 / Pipeline warming and graceful shutdown</li>
 * </ul>
 * 
 * <h3>管道架构 / Pipeline Architecture:</h3>
 * <pre>
 * ┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
 * │   Client API    │───→│ Operation Queue  │───→│  Batch Processor│
 * │   (客户端API)    │    │  (操作队列)       │    │   (批处理器)     │
 * └─────────────────┘    └──────────────────┘    └─────────────────┘
 *          │                       │                       │
 *          │                       ▼                       ▼
 * ┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
 * │ Memory Cache    │    │ Semaphore Control│    │  Retry Manager  │
 * │  (内存缓存)      │    │  (信号量控制)     │    │  (重试管理器)    │
 * └─────────────────┘    └──────────────────┘    └─────────────────┘
 *          │                       │                       │
 *          ▼                       ▼                       ▼
 * ┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
 * │  Vector Store   │    │   Graph Store    │    │  Performance    │
 * │  (向量存储)      │    │   (图存储)        │    │   Monitor       │
 * └─────────────────┘    └──────────────────┘    └─────────────────┘
 * </pre>
 * 
 * <h3>管道规格 / Pipeline Specifications:</h3>
 * <ul>
 *   <li><b>批处理大小</b>: 可配置，默认50个操作/批次 / Batch size: configurable, default 50 operations/batch</li>
 *   <li><b>并发限制</b>: 可配置，默认100个并发操作 / Concurrency limit: configurable, default 100 concurrent operations</li>
 *   <li><b>重试策略</b>: 指数退避，最大3次重试 / Retry strategy: exponential backoff, max 3 retries</li>
 *   <li><b>缓存策略</b>: 双级缓存(内存+查询缓存) / Cache strategy: dual-level cache (memory + query cache)</li>
 *   <li><b>监控指标</b>: 吞吐量、延迟、错误率、缓存命中率 / Monitoring metrics: throughput, latency, error rate, cache hit rate</li>
 * </ul>
 * 
 * <h3>使用示例 / Usage Examples:</h3>
 * <pre>{@code
 * // 创建异步管道
 * AsyncMemoryPipeline pipeline = new AsyncMemoryPipeline(
 *     embeddingProvider, vectorStore, graphStore, 
 *     executionManager, performanceMonitor
 * );
 * 
 * // 异步创建内存
 * CompletableFuture<String> createFuture = pipeline.createMemoryAsync(
 *     "用户喜欢喝咖啡", "user123", metadata
 * );
 * String memoryId = createFuture.join();
 * 
 * // 批量创建内存
 * List<MemoryCreationRequest> requests = Arrays.asList(
 *     new MemoryCreationRequest("内容1", "user123", null),
 *     new MemoryCreationRequest("内容2", "user123", null)
 * );
 * CompletableFuture<List<String>> batchFuture = pipeline.createMemoriesBatch(requests);
 * List<String> memoryIds = batchFuture.join();
 * 
 * // 异步搜索
 * CompletableFuture<List<EnhancedMemory>> searchFuture = pipeline.searchSimilarMemoriesAsync(
 *     "咖啡相关", "user123", 10, 0.7f
 * );
 * List<EnhancedMemory> results = searchFuture.join();
 * 
 * // 获取统计信息
 * PipelineStats stats = pipeline.getStats();
 * System.out.println("成功率: " + stats.getSuccessRate());
 * System.out.println("活跃批次: " + stats.getActiveBatches());
 * 
 * // 管道预热
 * List<String> commonQueries = Arrays.asList("用户偏好", "产品推荐", "历史记录");
 * pipeline.warmup(commonQueries).join();
 * 
 * // 优雅关闭
 * pipeline.shutdown().join();
 * }</pre>
 * 
 * <h3>性能优化策略 / Performance Optimization Strategies:</h3>
 * <ul>
 *   <li><b>批处理优化</b>: 自动批量处理相同类型操作 / Batch optimization: automatic batching of same-type operations</li>
 *   <li><b>并行执行</b>: 向量和图存储并行写入 / Parallel execution: concurrent vector and graph store writes</li>
 *   <li><b>缓存策略</b>: 智能缓存热点数据减少重复计算 / Cache strategy: intelligent caching of hot data to reduce redundant computation</li>
 *   <li><b>资源池化</b>: 连接池和线程池复用 / Resource pooling: connection pool and thread pool reuse</li>
 *   <li><b>背压控制</b>: 信号量控制防止资源耗尽 / Backpressure control: semaphore control to prevent resource exhaustion</li>
 * </ul>
 * 
 * <h3>线程安全性 / Thread Safety:</h3>
 * 此类是线程安全的，内部使用并发安全的组件和无锁数据结构实现高性能并发处理。
 * This class is thread-safe, using internally concurrent-safe components and lock-free data structures for high-performance concurrent processing.
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 * @see com.mem0.performance.ConcurrentExecutionManager
 * @see com.mem0.monitoring.PerformanceMonitor
 * @see com.mem0.core.EnhancedMemory
 */
public class AsyncMemoryPipeline {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncMemoryPipeline.class);
    
    // 核心组件
    private final EmbeddingProvider embeddingProvider;
    private final VectorStore vectorStore;
    private final GraphStore graphStore;
    private final ConcurrentExecutionManager executionManager;
    private final PerformanceMonitor performanceMonitor;
    
    // 管道配置
    private final int maxBatchSize;
    private final int maxRetries;
    private final long retryDelayMs;
    private final int maxConcurrentOperations;
    private final String defaultCollection = "memories";
    
    // 缓存层
    private final HighPerformanceCache<String, EnhancedMemory> memoryCache;
    private final HighPerformanceCache<String, List<EnhancedMemory>> queryCache;
    
    // 并发控制
    private final Semaphore operationSemaphore;
    private final CompletableFuture<Void> shutdownFuture = new CompletableFuture<>();
    
    // 统计信息
    private final AtomicLong totalOperations = new AtomicLong(0);
    private final AtomicLong successfulOperations = new AtomicLong(0);
    private final AtomicLong failedOperations = new AtomicLong(0);
    private final AtomicInteger activeBatches = new AtomicInteger(0);
    
    // 批处理队列
    private final BlockingQueue<PipelineOperation<?>> operationQueue;
    private final ScheduledExecutorService batchProcessor;
    private volatile boolean isShutdown = false;

    public AsyncMemoryPipeline(EmbeddingProvider embeddingProvider,
                             VectorStore vectorStore,
                             GraphStore graphStore,
                             ConcurrentExecutionManager executionManager,
                             PerformanceMonitor performanceMonitor) {
        this(embeddingProvider, vectorStore, graphStore, executionManager, performanceMonitor,
             50, 3, 1000, 100);
    }

    public AsyncMemoryPipeline(EmbeddingProvider embeddingProvider,
                             VectorStore vectorStore,
                             GraphStore graphStore,
                             ConcurrentExecutionManager executionManager,
                             PerformanceMonitor performanceMonitor,
                             int maxBatchSize,
                             int maxRetries,
                             long retryDelayMs,
                             int maxConcurrentOperations) {
        this.embeddingProvider = embeddingProvider;
        this.vectorStore = vectorStore;
        this.graphStore = graphStore;
        this.executionManager = executionManager;
        this.performanceMonitor = performanceMonitor;
        this.maxBatchSize = maxBatchSize;
        this.maxRetries = maxRetries;
        this.retryDelayMs = retryDelayMs;
        this.maxConcurrentOperations = maxConcurrentOperations;
        
        // 初始化缓存
        this.memoryCache = new HighPerformanceCache<>(10000, 1800000, 300000); // 30分钟TTL
        this.queryCache = new HighPerformanceCache<>(5000, 600000, 120000);    // 10分钟TTL
        
        // 初始化并发控制
        this.operationSemaphore = new Semaphore(maxConcurrentOperations);
        
        // 初始化批处理
        this.operationQueue = new LinkedBlockingQueue<>();
        this.batchProcessor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "mem0-pipeline-batch-processor");
            t.setDaemon(true);
            return t;
        });
        
        startBatchProcessor();
        logger.info("异步内存管道初始化完成 - 最大批大小: {}, 最大并发: {}", maxBatchSize, maxConcurrentOperations);
    }

    /**
     * 异步创建内存
     */
    public CompletableFuture<String> createMemoryAsync(String content, String userId, Map<String, Object> metadata) {
        return executeWithPipeline("createMemory", () -> {
            performanceMonitor.incrementCounter("pipeline.memory.create.requests");
            
            return performanceMonitor.measureTime("pipeline.memory.create", () -> {
                // 1. 生成嵌入向量
                CompletableFuture<List<Float>> embeddingFuture = embeddingProvider.embed(content);
                
                // 2. 创建内存对象
                String memoryId = generateMemoryId();
                EnhancedMemory memory = new EnhancedMemory(memoryId, content, userId, metadata);
                
                return embeddingFuture.thenCompose(embedding -> {
                    // 3. 并行存储到向量数据库和图数据库
                    Map<String, Object> vectorMetadata = new HashMap<>(metadata);
                    vectorMetadata.put("userId", userId);
                    vectorMetadata.put("memoryId", memoryId);
                    
                    CompletableFuture<String> vectorStoreFuture = 
                        vectorStore.insert(defaultCollection, embedding, vectorMetadata);
                    
                    Map<String, Object> nodeProperties = new HashMap<>();
                    nodeProperties.put("content", memory.getContent());
                    nodeProperties.put("userId", memory.getUserId());
                    nodeProperties.put("memoryType", memory.getType() != null ? memory.getType().toString() : "UNKNOWN");
                    nodeProperties.put("createdAt", memory.getCreatedAt());
                    
                    CompletableFuture<Void> graphStoreFuture = 
                        createGraphNode(memoryId, nodeProperties);
                    
                    return CompletableFuture.allOf(vectorStoreFuture, graphStoreFuture)
                        .thenApply(v -> {
                            // 4. 更新缓存
                            memoryCache.put(memoryId, memory);
                            
                            performanceMonitor.incrementCounter("pipeline.memory.create.success");
                            logger.debug("内存创建成功: {}", memoryId);
                            
                            return memoryId;
                        });
                });
            });
        });
    }

    /**
     * 批量创建内存
     */
    public CompletableFuture<List<String>> createMemoriesBatch(List<MemoryCreationRequest> requests) {
        return executeWithPipeline("createMemoriesBatch", () -> {
            performanceMonitor.incrementCounter("pipeline.memory.batch_create.requests");
            
            return performanceMonitor.measureTime("pipeline.memory.batch_create", () -> {
                logger.info("开始批量创建内存，数量: {}", requests.size());
                
                // 分批处理
                List<CompletableFuture<List<String>>> batchFutures = new ArrayList<>();
                
                for (int i = 0; i < requests.size(); i += maxBatchSize) {
                    int endIndex = Math.min(i + maxBatchSize, requests.size());
                    List<MemoryCreationRequest> batch = requests.subList(i, endIndex);
                    
                    CompletableFuture<List<String>> batchFuture = processBatch(batch);
                    batchFutures.add(batchFuture);
                }
                
                // 等待所有批次完成
                return CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        List<String> allResults = new ArrayList<>();
                        for (CompletableFuture<List<String>> batchFuture : batchFutures) {
                            try {
                                allResults.addAll(batchFuture.get());
                            } catch (Exception e) {
                                logger.error("批处理失败", e);
                            }
                        }
                        
                        performanceMonitor.incrementCounter("pipeline.memory.batch_create.success");
                        logger.info("批量内存创建完成，总计: {}", allResults.size());
                        
                        return allResults;
                    });
            });
        });
    }

    /**
     * 异步查询相似内存
     */
    public CompletableFuture<List<EnhancedMemory>> searchSimilarMemoriesAsync(String query, String userId, int limit, float threshold) {
        String cacheKey = generateSearchCacheKey(query, userId, limit, threshold);
        
        // 检查缓存
        List<EnhancedMemory> cached = queryCache.get(cacheKey);
        if (cached != null) {
            performanceMonitor.incrementCounter("pipeline.search.cache_hits");
            return CompletableFuture.completedFuture(cached);
        }
        
        return executeWithPipeline("searchSimilarMemories", () -> {
            performanceMonitor.incrementCounter("pipeline.search.requests");
            
            return performanceMonitor.measureTime("pipeline.search", () -> {
                // 1. 生成查询嵌入
                return embeddingProvider.embed(query)
                    .thenCompose(queryEmbedding -> {
                        // 2. 向量相似性搜索
                        Map<String, Object> searchFilter = new HashMap<>();
                        searchFilter.put("userId", userId);
                        return vectorStore.search(defaultCollection, queryEmbedding, limit * 2, searchFilter) // 搜索更多结果用于过滤
                            .thenCompose(vectorResults -> {
                                // 3. 过滤结果并获取完整内存信息
                                List<String> memoryIds = vectorResults.stream()
                                    .filter(result -> result.getScore() >= threshold)
                                    .limit(limit)
                                    .map(result -> (String) result.getMetadata().get("memoryId"))
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList());
                                
                                if (memoryIds.isEmpty()) {
                                    return CompletableFuture.completedFuture(Collections.<EnhancedMemory>emptyList());
                                }
                                
                                // 4. 批量获取内存详情
                                return getMemoriesByIds(memoryIds);
                            })
                            .thenApply(memories -> {
                                // 5. 缓存结果
                                queryCache.put(cacheKey, memories);
                                
                                performanceMonitor.incrementCounter("pipeline.search.success");
                                logger.debug("相似内存搜索完成，查询: {}, 结果数: {}", query, memories.size());
                                
                                return memories;
                            });
                    });
            });
        });
    }

    /**
     * 异步更新内存
     */
    public CompletableFuture<Boolean> updateMemoryAsync(String memoryId, String newContent, Map<String, Object> newMetadata) {
        return executeWithPipeline("updateMemory", () -> {
            performanceMonitor.incrementCounter("pipeline.memory.update.requests");
            
            return performanceMonitor.measureTime("pipeline.memory.update", () -> {
                // 1. 获取现有内存
                return getMemoryById(memoryId)
                    .thenCompose(existingMemory -> {
                        if (existingMemory == null) {
                            logger.warn("尝试更新不存在的内存: {}", memoryId);
                            return CompletableFuture.completedFuture(false);
                        }
                        
                        // 2. 生成新的嵌入向量
                        return embeddingProvider.embed(newContent)
                            .thenCompose(newEmbedding -> {
                                // 3. 创建更新的内存对象
                                EnhancedMemory updatedMemory = new EnhancedMemory(
                                    memoryId, newContent, existingMemory.getUserId(), newMetadata
                                );
                                updatedMemory.setCreatedAt(existingMemory.getCreatedAt()); // 保持原创建时间
                                updatedMemory.markAsUpdated();
                                
                                // 4. 并行更新存储
                                Map<String, Object> updateMetadata = new HashMap<>(newMetadata);
                                updateMetadata.put("userId", existingMemory.getUserId());
                                updateMetadata.put("memoryId", memoryId);
                                
                                // Delete old vector and insert new one (since update may not be available)
                                CompletableFuture<Void> vectorUpdateFuture = 
                                    vectorStore.delete(defaultCollection, memoryId)
                                        .thenCompose(v -> vectorStore.insert(defaultCollection, newEmbedding, updateMetadata)
                                            .thenApply(id -> null));
                                
                                Map<String, Object> updatedProps = new HashMap<>();
                                updatedProps.put("content", updatedMemory.getContent());
                                updatedProps.put("memoryType", updatedMemory.getType() != null ? updatedMemory.getType().toString() : "UNKNOWN");
                                updatedProps.put("updatedAt", updatedMemory.getUpdatedAt());
                                
                                CompletableFuture<Void> graphUpdateFuture = 
                                    graphStore.updateNode(memoryId, updatedProps);
                                
                                return CompletableFuture.allOf(vectorUpdateFuture, graphUpdateFuture)
                                    .thenApply(v -> {
                                        // 5. 更新缓存
                                        memoryCache.put(memoryId, updatedMemory);
                                        
                                        // 6. 清理查询缓存
                                        invalidateQueryCache(existingMemory.getUserId());
                                        
                                        performanceMonitor.incrementCounter("pipeline.memory.update.success");
                                        logger.debug("内存更新成功: {}", memoryId);
                                        
                                        return true;
                                    });
                            });
                    });
            });
        });
    }

    /**
     * 异步删除内存
     */
    public CompletableFuture<Boolean> deleteMemoryAsync(String memoryId) {
        return executeWithPipeline("deleteMemory", () -> {
            performanceMonitor.incrementCounter("pipeline.memory.delete.requests");
            
            return performanceMonitor.measureTime("pipeline.memory.delete", () -> {
                // 1. 获取内存信息用于清理缓存
                return getMemoryById(memoryId)
                    .thenCompose(memory -> {
                        if (memory == null) {
                            logger.warn("尝试删除不存在的内存: {}", memoryId);
                            return CompletableFuture.completedFuture(false);
                        }
                        
                        // 2. 并行删除
                        CompletableFuture<Void> vectorDeleteFuture = vectorStore.delete(defaultCollection, memoryId);
                        CompletableFuture<Void> graphDeleteFuture = graphStore.deleteNode(memoryId);
                        
                        return CompletableFuture.allOf(vectorDeleteFuture, graphDeleteFuture)
                            .thenApply(v -> {
                                // 3. 清理缓存
                                memoryCache.remove(memoryId);
                                invalidateQueryCache(memory.getUserId());
                                
                                performanceMonitor.incrementCounter("pipeline.memory.delete.success");
                                logger.debug("内存删除成功: {}", memoryId);
                                
                                return true;
                            });
                    });
            });
        });
    }

    /**
     * 获取管道统计信息
     */
    public PipelineStats getStats() {
        return new PipelineStats(
            totalOperations.get(),
            successfulOperations.get(),
            failedOperations.get(),
            activeBatches.get(),
            operationQueue.size(),
            operationSemaphore.availablePermits(),
            memoryCache.getStats(),
            queryCache.getStats()
        );
    }

    /**
     * 预热管道
     */
    public CompletableFuture<Void> warmup(List<String> commonQueries) {
        logger.info("开始管道预热，查询数量: {}", commonQueries.size());
        
        List<CompletableFuture<List<Float>>> embeddingFutures = commonQueries.stream()
            .map(embeddingProvider::embed)
            .collect(Collectors.toList());
        
        return CompletableFuture.allOf(embeddingFutures.toArray(new CompletableFuture[0]))
            .thenRun(() -> logger.info("管道预热完成"));
    }

    /**
     * 关闭管道
     */
    public CompletableFuture<Void> shutdown() {
        logger.info("开始关闭异步内存管道");
        isShutdown = true;
        
        return CompletableFuture.runAsync(() -> {
            try {
                // 停止批处理器
                batchProcessor.shutdown();
                if (!batchProcessor.awaitTermination(30, TimeUnit.SECONDS)) {
                    batchProcessor.shutdownNow();
                }
                
                // 等待所有操作完成
                while (operationSemaphore.availablePermits() < maxConcurrentOperations) {
                    Thread.sleep(100);
                }
                
                // 关闭缓存
                memoryCache.shutdown();
                queryCache.shutdown();
                
                logger.info("异步内存管道关闭完成");
                shutdownFuture.complete(null);
                
            } catch (Exception e) {
                logger.error("管道关闭时发生错误", e);
                shutdownFuture.completeExceptionally(e);
            }
        });
    }

    // 私有辅助方法

    private void startBatchProcessor() {
        batchProcessor.scheduleAtFixedRate(this::processPendingOperations, 100, 100, TimeUnit.MILLISECONDS);
    }

    private void processPendingOperations() {
        if (isShutdown || operationQueue.isEmpty()) {
            return;
        }

        List<PipelineOperation<?>> batch = new ArrayList<>();
        operationQueue.drainTo(batch, maxBatchSize);

        if (!batch.isEmpty()) {
            activeBatches.incrementAndGet();
            CompletableFuture.runAsync(() -> {
                try {
                    // 按操作类型分组并并行执行
                    Map<String, List<PipelineOperation<?>>> groupedOps = batch.stream()
                        .collect(Collectors.groupingBy(op -> op.operationType));

                    List<CompletableFuture<Void>> groupFutures = groupedOps.entrySet().stream()
                        .map(entry -> executeOperationGroup(entry.getKey(), entry.getValue()))
                        .collect(Collectors.toList());

                    CompletableFuture.allOf(groupFutures.toArray(new CompletableFuture[0])).join();

                } catch (Exception e) {
                    logger.error("批处理操作失败", e);
                } finally {
                    activeBatches.decrementAndGet();
                }
            });
        }
    }

    private CompletableFuture<Void> executeOperationGroup(String operationType, List<PipelineOperation<?>> operations) {
        return CompletableFuture.runAsync(() -> {
            logger.debug("执行操作组: {}, 操作数: {}", operationType, operations.size());
            
            for (PipelineOperation<?> operation : operations) {
                try {
                    operation.execute();
                } catch (Exception e) {
                    logger.error("操作执行失败: " + operationType, e);
                    operation.completeExceptionally(e);
                }
            }
        });
    }

    private <T> CompletableFuture<T> executeWithPipeline(String operationType, Callable<CompletableFuture<T>> operation) {
        if (isShutdown) {
            CompletableFuture<T> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new IllegalStateException("管道已关闭"));
            return failedFuture;
        }

        totalOperations.incrementAndGet();

        return CompletableFuture.supplyAsync(() -> {
            try {
                operationSemaphore.acquire();
                
                return executeWithRetry(operationType, operation)
                    .whenComplete((result, throwable) -> {
                        operationSemaphore.release();
                        
                        if (throwable == null) {
                            successfulOperations.incrementAndGet();
                        } else {
                            failedOperations.incrementAndGet();
                            logger.error("管道操作失败: " + operationType, throwable);
                        }
                    });
                    
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("操作被中断", e);
            }
        }).thenCompose(Function.identity());
    }

    private <T> CompletableFuture<T> executeWithRetry(String operationType, Callable<CompletableFuture<T>> operation) {
        return executeWithRetry(operationType, operation, 0);
    }

    private <T> CompletableFuture<T> executeWithRetry(String operationType, Callable<CompletableFuture<T>> operation, int attempt) {
        try {
            return operation.call().exceptionally(throwable -> {
                if (attempt < maxRetries) {
                    logger.warn("操作失败，准备重试 ({}/{}): {}", attempt + 1, maxRetries, operationType);
                    
                    try {
                        Thread.sleep(retryDelayMs * (attempt + 1)); // 指数退避
                        return executeWithRetry(operationType, operation, attempt + 1).join();
                    } catch (Exception e) {
                        throw new RuntimeException("重试失败", e);
                    }
                } else {
                    throw new RuntimeException("操作达到最大重试次数: " + operationType, throwable);
                }
            });
        } catch (Exception e) {
            CompletableFuture<T> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        }
    }

    private CompletableFuture<List<String>> processBatch(List<MemoryCreationRequest> batch) {
        activeBatches.incrementAndGet();
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("处理内存创建批次，大小: {}", batch.size());
                
                // 批量生成嵌入
                List<String> contents = batch.stream()
                    .map(MemoryCreationRequest::getContent)
                    .collect(Collectors.toList());
                
                return embeddingProvider.embedBatch(contents)
                    .thenCompose(embeddings -> {
                        // 并行创建内存
                        List<CompletableFuture<String>> creationFutures = new ArrayList<>();
                        
                        for (int i = 0; i < batch.size(); i++) {
                            MemoryCreationRequest request = batch.get(i);
                            List<Float> embedding = embeddings.get(i);
                            
                            CompletableFuture<String> future = createSingleMemory(request, embedding);
                            creationFutures.add(future);
                        }
                        
                        return CompletableFuture.allOf(creationFutures.toArray(new CompletableFuture[0]))
                            .thenApply(v -> {
                                List<String> results = new ArrayList<>();
                                for (CompletableFuture<String> future : creationFutures) {
                                    try {
                                        results.add(future.get());
                                    } catch (Exception e) {
                                        logger.error("单个内存创建失败", e);
                                        results.add(null);
                                    }
                                }
                                return results;
                            });
                    }).join();
                    
            } finally {
                activeBatches.decrementAndGet();
            }
        });
    }

    private CompletableFuture<String> createSingleMemory(MemoryCreationRequest request, List<Float> embedding) {
        String memoryId = generateMemoryId();
        EnhancedMemory memory = new EnhancedMemory(memoryId, request.getContent(), request.getUserId(), request.getMetadata());
        
        Map<String, Object> vectorMetadata = new HashMap<>(request.getMetadata());
        vectorMetadata.put("userId", request.getUserId());
        vectorMetadata.put("memoryId", memoryId);
        
        CompletableFuture<String> vectorStoreFuture = 
            vectorStore.insert(defaultCollection, embedding, vectorMetadata);
        
        Map<String, Object> nodeProps = new HashMap<>();
        nodeProps.put("id", memoryId); // 添加ID属性
        nodeProps.put("content", memory.getContent());
        nodeProps.put("userId", memory.getUserId());
        nodeProps.put("memoryType", memory.getType() != null ? memory.getType().toString() : "UNKNOWN");
        nodeProps.put("createdAt", memory.getCreatedAt());
        
        logger.debug("准备创建图节点: {}", memoryId);
        
        // 直接创建节点，使用memoryId作为节点ID
        CompletableFuture<Void> graphStoreFuture = 
            createGraphNode(memoryId, nodeProps);
        
        return CompletableFuture.allOf(vectorStoreFuture.thenApply(id -> null), graphStoreFuture)
            .thenApply(v -> {
                logger.debug("内存创建完成: {}", memoryId);
                memoryCache.put(memoryId, memory);
                return memoryId;
            })
            .exceptionally(throwable -> {
                logger.error("内存创建失败: {}", memoryId, throwable);
                throw new RuntimeException("内存创建失败", throwable);
            });
    }
    
    private CompletableFuture<Void> createGraphNode(String nodeId, Map<String, Object> properties) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("创建图节点: {}, 图存储类型: {}", nodeId, graphStore.getClass().getSimpleName());
                
                // 直接在高性能图存储中创建节点，使用指定的ID
                if (graphStore instanceof com.mem0.graph.impl.HighPerformanceGraphStore) {
                    logger.debug("使用HighPerformanceGraphStore创建节点");
                    com.mem0.graph.impl.HighPerformanceGraphStore hpGraphStore = 
                        (com.mem0.graph.impl.HighPerformanceGraphStore) graphStore;
                    hpGraphStore.createNodeWithId(nodeId, properties);
                } else {
                    logger.debug("使用默认图存储创建节点");
                    // 对于其他图存储，使用默认方法
                    graphStore.createNode("Memory", properties);
                }
                return null;
            } catch (Exception e) {
                logger.error("创建图节点失败: {}", nodeId, e);
                throw new RuntimeException("创建图节点失败", e);
            }
        });
    }

    private CompletableFuture<EnhancedMemory> getMemoryById(String memoryId) {
        // 检查缓存
        EnhancedMemory cached = memoryCache.get(memoryId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        // 从图数据库获取
        return graphStore.getNode(memoryId).thenApply(node -> {
            if (node == null) return null;
            
            Map<String, Object> props = node.getProperties();
            String content = (String) props.get("content");
            String userId = (String) props.get("userId");
            
            // Create a simplified EnhancedMemory for pipeline use
            return new EnhancedMemory(memoryId, content, userId, null);
        });
    }

    private CompletableFuture<List<EnhancedMemory>> getMemoriesByIds(List<String> memoryIds) {
        List<CompletableFuture<EnhancedMemory>> futures = memoryIds.stream()
            .map(this::getMemoryById)
            .collect(Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                List<EnhancedMemory> memories = new ArrayList<>();
                for (CompletableFuture<EnhancedMemory> future : futures) {
                    try {
                        EnhancedMemory memory = future.get();
                        if (memory != null) {
                            memories.add(memory);
                        }
                    } catch (Exception e) {
                        logger.error("获取内存失败", e);
                    }
                }
                return memories;
            });
    }

    private String generateMemoryId() {
        return "mem_" + System.currentTimeMillis() + "_" + Math.abs(UUID.randomUUID().hashCode());
    }

    private String generateSearchCacheKey(String query, String userId, int limit, float threshold) {
        return String.format("search_%s_%s_%d_%.2f", 
            Integer.toHexString(query.hashCode()), userId, limit, threshold);
    }

    private void invalidateQueryCache(String userId) {
        // 简化实现：清空整个查询缓存
        // 在生产环境中，应该只清理特定用户的缓存
        queryCache.clear();
    }

    // 内部类

    private static class PipelineOperation<T> {
        private final String operationType;
        private final Callable<T> operation;
        private final CompletableFuture<T> future;

        public PipelineOperation(String operationType, Callable<T> operation) {
            this.operationType = operationType;
            this.operation = operation;
            this.future = new CompletableFuture<>();
        }

        public void execute() {
            try {
                T result = operation.call();
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }

        public void completeExceptionally(Throwable throwable) {
            future.completeExceptionally(throwable);
        }
    }

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

    public static class PipelineStats {
        private final long totalOperations;
        private final long successfulOperations;
        private final long failedOperations;
        private final int activeBatches;
        private final int queueSize;
        private final int availablePermits;
        private final HighPerformanceCache.CacheStats memoryCacheStats;
        private final HighPerformanceCache.CacheStats queryCacheStats;

        public PipelineStats(long totalOperations, long successfulOperations, long failedOperations,
                           int activeBatches, int queueSize, int availablePermits,
                           HighPerformanceCache.CacheStats memoryCacheStats,
                           HighPerformanceCache.CacheStats queryCacheStats) {
            this.totalOperations = totalOperations;
            this.successfulOperations = successfulOperations;
            this.failedOperations = failedOperations;
            this.activeBatches = activeBatches;
            this.queueSize = queueSize;
            this.availablePermits = availablePermits;
            this.memoryCacheStats = memoryCacheStats;
            this.queryCacheStats = queryCacheStats;
        }

        public long getTotalOperations() { return totalOperations; }
        public long getSuccessfulOperations() { return successfulOperations; }
        public long getFailedOperations() { return failedOperations; }
        public int getActiveBatches() { return activeBatches; }
        public int getQueueSize() { return queueSize; }
        public int getAvailablePermits() { return availablePermits; }
        public HighPerformanceCache.CacheStats getMemoryCacheStats() { return memoryCacheStats; }
        public HighPerformanceCache.CacheStats getQueryCacheStats() { return queryCacheStats; }

        public double getSuccessRate() {
            return totalOperations == 0 ? 0.0 : (double) successfulOperations / totalOperations;
        }

        @Override
        public String toString() {
            return String.format("PipelineStats{总操作=%d, 成功=%d, 失败=%d, 成功率=%.2f%%, 活跃批次=%d, 队列=%d, 可用许可=%d, 内存缓存=%s, 查询缓存=%s}",
                totalOperations, successfulOperations, failedOperations, getSuccessRate() * 100,
                activeBatches, queueSize, availablePermits, memoryCacheStats, queryCacheStats);
        }
    }
}