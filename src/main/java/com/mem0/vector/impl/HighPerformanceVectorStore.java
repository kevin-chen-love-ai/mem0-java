package com.mem0.vector.impl;

import com.mem0.store.VectorStore;
import com.mem0.model.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mem0.concurrency.cache.HighPerformanceCache;
import com.mem0.performance.ConcurrentExecutionManager;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * 高性能向量存储实现
 * 支持并发搜索、缓存优化、批量操作、索引加速
 */
public class HighPerformanceVectorStore implements VectorStore {
    
    private static final Logger logger = LoggerFactory.getLogger(HighPerformanceVectorStore.class);
    
    // 主存储
    private final Map<String, VectorEntry> vectors = new ConcurrentHashMap<>();
    private final Map<String, List<String>> userMemories = new ConcurrentHashMap<>();
    
    // 性能优化组件
    private final HighPerformanceCache<String, SearchResult> queryCache;
    private final HighPerformanceCache<String, List<SearchResult>> userCache;
    private final ConcurrentExecutionManager executionManager;
    
    // 批量操作缓冲区
    private final Map<String, VectorEntry> pendingInserts = new ConcurrentHashMap<>();
    private volatile boolean batchMode = false;
    
    // 向量索引（用于加速搜索）
    private final Map<Integer, Set<String>> dimensionIndex = new ConcurrentHashMap<>();
    private final ReadWriteLock indexLock = new ReentrantReadWriteLock();
    
    // 统计信息
    private volatile long totalQueries = 0;
    private volatile long totalInserts = 0;
    private volatile long totalUpdates = 0;
    
    public HighPerformanceVectorStore() {
        this.queryCache = new HighPerformanceCache<>(5000, 300000, 60000); // 5K条目，5分钟TTL
        this.userCache = new HighPerformanceCache<>(1000, 600000, 120000);  // 1K用户，10分钟TTL
        this.executionManager = new ConcurrentExecutionManager();
        
        logger.info("高性能VectorStore初始化完成，启用缓存和并发优化");
    }
    
    /**
     * 向量条目内部类
     */
    private static class VectorEntry {
        final String id;
        final float[] embedding;
        final Map<String, Object> properties;
        final long createdTime;
        volatile long lastAccessTime;
        
        VectorEntry(String id, float[] embedding, Map<String, Object> properties) {
            this.id = id;
            this.embedding = Arrays.copyOf(embedding, embedding.length);
            this.properties = new HashMap<>(properties);
            this.createdTime = System.currentTimeMillis();
            this.lastAccessTime = this.createdTime;
        }
        
        void updateAccess() {
            this.lastAccessTime = System.currentTimeMillis();
        }
    }
    
    @Override
    public CompletableFuture<Void> createCollection(String collectionName, int dimension) {
        return CompletableFuture.supplyAsync(() -> {
            logger.debug("创建向量集合: {} (维度: {})", collectionName, dimension);
            // 内存实现中，集合是动态创建的，这里只是记录日志
            return null;
        });
    }
    
    @Override
    public CompletableFuture<Boolean> collectionExists(String collectionName) {
        return CompletableFuture.supplyAsync(() -> {
            // 内存实现中，集合总是存在的
            return true;
        });
    }
    
    @Override
    public CompletableFuture<Void> dropCollection(String collectionName) {
        return CompletableFuture.supplyAsync(() -> {
            logger.debug("删除向量集合: {}", collectionName);
            // 内存实现中，清空所有数据
            vectors.clear();
            userMemories.clear();
            queryCache.clear();
            userCache.clear();
            return null;
        });
    }
    
    @Override
    public CompletableFuture<List<String>> batchInsert(String collectionName, 
                                                      List<List<Float>> vectors,
                                                      List<Map<String, Object>> metadataList) {
        return executionManager.executeIOOperation(() -> {
            if (vectors.size() != metadataList.size()) {
                throw new IllegalArgumentException("向量和元数据列表大小不匹配");
            }
            
            logger.info("开始批量插入 {} 个向量到集合: {}", vectors.size(), collectionName);
            long startTime = System.currentTimeMillis();
            
            try {
                List<String> ids = new ArrayList<>();
                
                // 批量插入
                for (int i = 0; i < vectors.size(); i++) {
                    String id = insert(collectionName, vectors.get(i), metadataList.get(i)).join();
                    ids.add(id);
                }
                
                long duration = System.currentTimeMillis() - startTime;
                logger.info("批量插入完成，耗时 {}ms", duration);
                return ids;
                
            } catch (Exception e) {
                logger.error("批量插入失败", e);
                throw new RuntimeException("批量插入失败", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<String> insert(String collectionName, List<Float> vector, Map<String, Object> metadata) {
        return executionManager.executeIOOperation(() -> {
            String id = null;
            try {
                // 生成ID
                id = generateId();
                logger.debug("插入向量: {}", id);
                
                // 转换List<Float>到float[]
                float[] embedding = new float[vector.size()];
                for (int i = 0; i < vector.size(); i++) {
                    embedding[i] = vector.get(i);
                }
                
                VectorEntry entry = new VectorEntry(id, embedding, metadata);
                
                if (batchMode) {
                    pendingInserts.put(id, entry);
                } else {
                    insertEntry(id, entry);
                }
                
                totalInserts++;
                logger.debug("向量插入成功: {}", id);
                return id;
            } catch (Exception e) {
                logger.error("向量插入失败: " + (id != null ? id : "unknown"), e);
                throw new RuntimeException("向量插入失败", e);
            }
        });
    }
    
    /**
     * 批量插入向量
     */
    public CompletableFuture<Void> insertBatch(Map<String, VectorData> vectors) {
        return executionManager.executeIOOperation(() -> {
            logger.info("开始批量插入 {} 个向量", vectors.size());
            long startTime = System.currentTimeMillis();
            
            try {
                enableBatchMode();
                
                // 并发插入
                List<CompletableFuture<String>> futures = vectors.entrySet().stream()
                    .map(entry -> {
                        // 转换float[]到List<Float>
                        float[] embedding = entry.getValue().embedding;
                        List<Float> vectorList = new ArrayList<>();
                        for (float f : embedding) {
                            vectorList.add(f);
                        }
                        // 直接插入向量，不使用VectorStore接口的insert方法
                        String vectorId = generateId();
                        VectorEntry vectorEntry = new VectorEntry(vectorId, embedding, entry.getValue().properties);
                        if (batchMode) {
                            pendingInserts.put(vectorId, vectorEntry);
                        } else {
                            insertEntry(vectorId, vectorEntry);
                        }
                        return CompletableFuture.completedFuture(vectorId);
                    })
                    .collect(Collectors.toList());
                
                // 等待所有插入完成
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                
                // 提交批量操作
                commitBatch();
                
                long duration = System.currentTimeMillis() - startTime;
                logger.info("批量插入完成，耗时 {}ms", duration);
                return null;
            } finally {
                disableBatchMode();
            }
        });
    }
    
    public CompletableFuture<Void> update(String id, float[] embedding, Map<String, Object> properties) {
        return executionManager.executeIOOperation(() -> {
            try {
                logger.debug("更新向量: {}", id);
                
                VectorEntry existingEntry = vectors.get(id);
                if (existingEntry == null) {
                    throw new RuntimeException("向量不存在: " + id);
                }
                
                // 更新属性（保留现有属性）
                Map<String, Object> updatedProperties = new HashMap<>(existingEntry.properties);
                updatedProperties.putAll(properties);
                
                VectorEntry updatedEntry = new VectorEntry(id, embedding, updatedProperties);
                vectors.put(id, updatedEntry);
                
                // 更新索引
                updateVectorIndex(id, embedding);
                
                // 清理相关缓存
                invalidateCache(id, (String) properties.get("userId"));
                
                totalUpdates++;
                logger.debug("向量更新成功: {}", id);
                return null;
            } catch (Exception e) {
                logger.error("向量更新失败: " + id, e);
                throw new RuntimeException("向量更新失败", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> delete(String collectionName, String id) {
        return executionManager.executeIOOperation(() -> {
            try {
                logger.debug("删除向量: {}", id);
                
                VectorEntry entry = vectors.remove(id);
                if (entry != null) {
                    // 从用户索引中移除
                    String userId = (String) entry.properties.get("userId");
                    if (userId != null) {
                        List<String> userMemoryList = userMemories.get(userId);
                        if (userMemoryList != null) {
                            userMemoryList.remove(id);
                            if (userMemoryList.isEmpty()) {
                                userMemories.remove(userId);
                            }
                        }
                    }
                    
                    // 从索引中移除
                    removeFromIndex(id, entry.embedding);
                    
                    // 清理缓存
                    invalidateCache(id, userId);
                    
                    logger.debug("向量删除成功: {}", id);
                    return null;
                } else {
                    logger.warn("要删除的向量不存在: {}", id);
                    return null;
                }
            } catch (Exception e) {
                logger.error("向量删除失败: " + id, e);
                throw new RuntimeException("向量删除失败", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> deleteByFilter(String collectionName, Map<String, Object> filter) {
        return executionManager.executeIOOperation(() -> {
            try {
                logger.debug("按过滤条件删除向量，过滤条件: {}", filter);
                
                // 找到匹配过滤条件的向量
                List<String> toDelete = new ArrayList<>();
                for (Map.Entry<String, VectorEntry> entry : vectors.entrySet()) {
                    VectorEntry vectorEntry = entry.getValue();
                    boolean matches = true;
                    
                    // 检查是否匹配过滤条件
                    for (Map.Entry<String, Object> filterEntry : filter.entrySet()) {
                        String key = filterEntry.getKey();
                        Object expectedValue = filterEntry.getValue();
                        Object actualValue = vectorEntry.properties.get(key);
                        
                        if (!Objects.equals(actualValue, expectedValue)) {
                            matches = false;
                            break;
                        }
                    }
                    
                    if (matches) {
                        toDelete.add(entry.getKey());
                    }
                }
                
                // 删除匹配的向量
                for (String id : toDelete) {
                    VectorEntry entry = vectors.remove(id);
                    if (entry != null) {
                        // 从用户索引中移除
                        String userId = (String) entry.properties.get("userId");
                        if (userId != null) {
                            List<String> userMemoryList = userMemories.get(userId);
                            if (userMemoryList != null) {
                                userMemoryList.remove(id);
                                if (userMemoryList.isEmpty()) {
                                    userMemories.remove(userId);
                                }
                            }
                        }
                        
                        // 从索引中移除
                        removeFromIndex(id, entry.embedding);
                    }
                }
                
                logger.debug("按过滤条件删除了 {} 个向量", toDelete.size());
                return null;
                
            } catch (Exception e) {
                logger.error("按过滤条件删除向量失败", e);
                throw new RuntimeException("按过滤条件删除向量失败", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<VectorStore.VectorDocument> get(String collectionName, String id) {
        return executionManager.executeIOOperation(() -> {
            try {
                logger.debug("获取向量: {}", id);
                
                VectorEntry entry = vectors.get(id);
                if (entry == null) {
                    return null;
                }
                
                // 转换float[]到List<Float>
                float[] embeddingArray = entry.embedding;
                List<Float> embeddingList = new ArrayList<>();
                for (float f : embeddingArray) {
                    embeddingList.add(f);
                }
                
                return new VectorStore.VectorDocument(id, embeddingList, entry.properties);
                    
            } catch (Exception e) {
                logger.error("获取向量失败: " + id, e);
                throw new RuntimeException("获取向量失败", e);
            }
        });
    }
    
    // 保持向后兼容的方法
    public CompletableFuture<SearchResult> get(String id) {
        return executionManager.executeIOOperation(() -> {
            try {
                // 先检查缓存
                SearchResult cached = queryCache.get(id);
                if (cached != null) {
                    return cached;
                }
                
                logger.debug("获取向量: {}", id);
                
                VectorEntry entry = vectors.get(id);
                if (entry == null) {
                    throw new RuntimeException("向量不存在: " + id);
                }
                
                entry.updateAccess();
                SearchResult result = new SearchResult(entry.id, 1.0f, entry.properties);
                
                // 缓存结果
                queryCache.put(id, result);
                
                return result;
            } catch (Exception e) {
                logger.error("获取向量失败: " + id, e);
                throw new RuntimeException("获取向量失败", e);
            }
        });
    }
    
    public CompletableFuture<List<SearchResult>> search(float[] queryEmbedding, String userId, int limit) {
        return executionManager.executeVectorOperation(() -> {
            try {
                totalQueries++;
                
                // 生成缓存键
                String cacheKey = generateSearchCacheKey(queryEmbedding, userId, limit);
                
                // 检查缓存
                List<SearchResult> cached = userCache.get(cacheKey);
                if (cached != null) {
                    logger.debug("从缓存返回搜索结果，用户: {}，限制: {}", userId, limit);
                    return cached;
                }
                
                logger.debug("搜索向量，用户: {}，限制: {}", userId, limit);
                
                List<String> userMemoryIds = userMemories.getOrDefault(userId, Collections.emptyList());
                if (userMemoryIds.isEmpty()) {
                    return Collections.emptyList();
                }
                
                // 并行计算相似度
                List<SearchResult> results = userMemoryIds.parallelStream()
                    .map(memoryId -> {
                        VectorEntry entry = vectors.get(memoryId);
                        if (entry != null) {
                            entry.updateAccess();
                            float similarity = calculateCosineSimilarity(queryEmbedding, entry.embedding);
                            return new SearchResult(entry.id, similarity, entry.properties);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .sorted((a, b) -> Float.compare(b.similarity, a.similarity))
                    .limit(limit)
                    .collect(Collectors.toList());
                
                // 缓存结果
                userCache.put(cacheKey, results, 300000); // 5分钟缓存
                
                logger.debug("搜索完成，返回 {} 个结果", results.size());
                return results;
            } catch (Exception e) {
                logger.error("向量搜索失败，用户: " + userId, e);
                throw new RuntimeException("向量搜索失败", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<VectorStore.VectorSearchResult>> search(String collectionName,
                                                                         List<Float> queryVector,
                                                                         int topK,
                                                                         Map<String, Object> filter) {
        return executionManager.executeVectorOperation(() -> {
            try {
                totalQueries++;
                
                // 转换List<Float>到float[]
                float[] queryEmbedding = new float[queryVector.size()];
                for (int i = 0; i < queryVector.size(); i++) {
                    queryEmbedding[i] = queryVector.get(i);
                }
                
                logger.debug("搜索向量，集合: {}，topK: {}, 过滤器: {}", collectionName, topK, filter);
                
                // 根据过滤器获取候选向量
                Collection<VectorEntry> candidates;
                if (filter != null && filter.containsKey("userId")) {
                    String userId = (String) filter.get("userId");
                    List<String> userMemoryIds = userMemories.getOrDefault(userId, Collections.emptyList());
                    candidates = userMemoryIds.stream()
                        .map(vectors::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                } else {
                    candidates = vectors.values();
                }
                
                if (candidates.isEmpty()) {
                    return Collections.emptyList();
                }
                
                // 并行计算相似度
                List<VectorStore.VectorSearchResult> results = candidates.parallelStream()
                    .filter(entry -> matchesFilter(entry, filter))
                    .map(entry -> {
                        entry.updateAccess();
                        float similarity = calculateCosineSimilarity(queryEmbedding, entry.embedding);
                        
                        // 转换float[]到List<Float>
                        List<Float> vectorList = new ArrayList<>();
                        for (float f : entry.embedding) {
                            vectorList.add(f);
                        }
                        
                        return new VectorStore.VectorSearchResult(entry.id, similarity, entry.properties, vectorList);
                    })
                    .sorted((a, b) -> Float.compare(b.getScore(), a.getScore()))
                    .limit(topK)
                    .collect(Collectors.toList());
                
                logger.debug("搜索完成，返回 {} 个结果", results.size());
                return results;
            } catch (Exception e) {
                logger.error("向量搜索失败，集合: " + collectionName, e);
                throw new RuntimeException("向量搜索失败", e);
            }
        });
    }
    
    private boolean matchesFilter(VectorEntry entry, Map<String, Object> filter) {
        if (filter == null || filter.isEmpty()) {
            return true;
        }
        
        for (Map.Entry<String, Object> filterEntry : filter.entrySet()) {
            String key = filterEntry.getKey();
            Object expectedValue = filterEntry.getValue();
            Object actualValue = entry.properties.get(key);
            
            if (!Objects.equals(actualValue, expectedValue)) {
                return false;
            }
        }
        
        return true;
    }
    
    public CompletableFuture<List<SearchResult>> getAllByUser(String userId) {
        return executionManager.executeIOOperation(() -> {
            try {
                // 检查缓存
                String cacheKey = "user_all_" + userId;
                List<SearchResult> cached = userCache.get(cacheKey);
                if (cached != null) {
                    return cached;
                }
                
                logger.debug("获取用户所有向量: {}", userId);
                
                List<String> userMemoryIds = userMemories.getOrDefault(userId, Collections.emptyList());
                List<SearchResult> results = new ArrayList<>();
                
                for (String memoryId : userMemoryIds) {
                    VectorEntry entry = vectors.get(memoryId);
                    if (entry != null) {
                        entry.updateAccess();
                        results.add(new SearchResult(entry.id, 1.0f, entry.properties));
                    }
                }
                
                // 按创建时间排序（最新优先）
                results.sort((a, b) -> {
                    String timeA = (String) a.properties.getOrDefault("createdAt", "");
                    String timeB = (String) b.properties.getOrDefault("createdAt", "");
                    return timeB.compareTo(timeA);
                });
                
                // 缓存结果
                userCache.put(cacheKey, results, 600000); // 10分钟缓存
                
                return results;
            } catch (Exception e) {
                logger.error("获取用户向量失败: " + userId, e);
                throw new RuntimeException("获取用户向量失败", e);
            }
        });
    }
    
    public CompletableFuture<Long> getMemoryCount(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<String> userMemoryIds = userMemories.getOrDefault(userId, Collections.emptyList());
                return (long) userMemoryIds.size();
            } catch (Exception e) {
                logger.error("获取内存数量失败，用户: " + userId, e);
                throw new RuntimeException("获取内存数量失败", e);
            }
        });
    }
    
    public CompletableFuture<Map<String, Long>> getMemoryTypeDistribution(String userId) {
        return executionManager.executeIOOperation(() -> {
            try {
                logger.debug("获取用户内存类型分布: {}", userId);
                
                List<String> userMemoryIds = userMemories.getOrDefault(userId, Collections.emptyList());
                Map<String, Long> distribution = new HashMap<>();
                
                for (String memoryId : userMemoryIds) {
                    VectorEntry entry = vectors.get(memoryId);
                    if (entry != null) {
                        String type = (String) entry.properties.getOrDefault("type", "UNKNOWN");
                        distribution.put(type, distribution.getOrDefault(type, 0L) + 1);
                    }
                }
                
                return distribution;
            } catch (Exception e) {
                logger.error("获取内存类型分布失败，用户: " + userId, e);
                throw new RuntimeException("获取内存类型分布失败", e);
            }
        });
    }
    
    /**
     * 启用批量模式
     */
    public void enableBatchMode() {
        this.batchMode = true;
        logger.debug("批量模式已启用");
    }
    
    /**
     * 禁用批量模式
     */
    public void disableBatchMode() {
        this.batchMode = false;
        logger.debug("批量模式已禁用");
    }
    
    /**
     * 提交批量操作
     */
    public void commitBatch() {
        if (!pendingInserts.isEmpty()) {
            logger.info("提交批量操作，处理 {} 个待插入向量", pendingInserts.size());
            
            pendingInserts.forEach(this::insertEntry);
            pendingInserts.clear();
            
            logger.info("批量操作提交完成");
        }
    }
    
    /**
     * 获取性能统计
     */
    public VectorStoreStats getStats() {
        return new VectorStoreStats(
            vectors.size(),
            userMemories.size(),
            totalQueries,
            totalInserts,
            totalUpdates,
            queryCache.getStats(),
            userCache.getStats()
        );
    }
    
    /**
     * 预热缓存
     */
    public void warmupCache(String userId) {
        logger.info("开始为用户 {} 预热缓存", userId);
        
        // 预加载用户的所有向量
        getAllByUser(userId).join();
        
        logger.info("用户 {} 缓存预热完成", userId);
    }
    
    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("关闭高性能向量存储");
            
            if (executionManager != null) {
                executionManager.close();
            }
            
            if (queryCache != null) {
                queryCache.shutdown();
            }
            
            if (userCache != null) {
                userCache.shutdown();
            }
            
            vectors.clear();
            userMemories.clear();
            dimensionIndex.clear();
            
            logger.info("向量存储关闭完成");
            return null;
        });
    }
    
    // 私有辅助方法
    
    private String generateId() {
        return "vec_" + System.currentTimeMillis() + "_" + Math.abs(java.util.UUID.randomUUID().hashCode());
    }
    
    private void insertEntry(String id, VectorEntry entry) {
        vectors.put(id, entry);
        
        // 更新用户索引
        String userId = (String) entry.properties.get("userId");
        if (userId != null) {
            userMemories.computeIfAbsent(userId, k -> new ArrayList<>()).add(id);
        }
        
        // 更新向量索引
        addToIndex(id, entry.embedding);
        
        // 清理相关缓存
        invalidateCache(id, userId);
    }
    
    private void addToIndex(String id, float[] embedding) {
        indexLock.writeLock().lock();
        try {
            int dimension = embedding.length;
            dimensionIndex.computeIfAbsent(dimension, k -> ConcurrentHashMap.newKeySet()).add(id);
        } finally {
            indexLock.writeLock().unlock();
        }
    }
    
    private void removeFromIndex(String id, float[] embedding) {
        indexLock.writeLock().lock();
        try {
            int dimension = embedding.length;
            Set<String> dimensionSet = dimensionIndex.get(dimension);
            if (dimensionSet != null) {
                dimensionSet.remove(id);
                if (dimensionSet.isEmpty()) {
                    dimensionIndex.remove(dimension);
                }
            }
        } finally {
            indexLock.writeLock().unlock();
        }
    }
    
    private void updateVectorIndex(String id, float[] newEmbedding) {
        VectorEntry oldEntry = vectors.get(id);
        if (oldEntry != null && oldEntry.embedding.length != newEmbedding.length) {
            // 维度改变，需要重新索引
            removeFromIndex(id, oldEntry.embedding);
            addToIndex(id, newEmbedding);
        }
    }
    
    private void invalidateCache(String id, String userId) {
        queryCache.remove(id);
        if (userId != null) {
            // 清理用户相关的缓存
            userCache.remove("user_all_" + userId);
            // 可以进一步优化，清理涉及该用户的搜索缓存
        }
    }
    
    private String generateSearchCacheKey(float[] queryEmbedding, String userId, int limit) {
        // 简化的缓存键生成（实际使用中可能需要更精确的哈希）
        int embeddingHash = Arrays.hashCode(queryEmbedding);
        return String.format("search_%s_%d_%d", userId, embeddingHash, limit);
    }
    
    /**
     * 计算余弦相似度
     */
    private float calculateCosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("向量维度必须相同");
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }
        
        double magnitude = Math.sqrt(normA) * Math.sqrt(normB);
        return magnitude == 0.0 ? 0.0f : (float) (dotProduct / magnitude);
    }
    
    // 辅助数据类
    
    public static class VectorData {
        public final float[] embedding;
        public final Map<String, Object> properties;
        
        public VectorData(float[] embedding, Map<String, Object> properties) {
            this.embedding = embedding;
            this.properties = properties;
        }
    }
    
    public static class VectorStoreStats {
        private final int totalVectors;
        private final int totalUsers;
        private final long totalQueries;
        private final long totalInserts;
        private final long totalUpdates;
        private final HighPerformanceCache.CacheStats queryCacheStats;
        private final HighPerformanceCache.CacheStats userCacheStats;
        
        public VectorStoreStats(int totalVectors, int totalUsers, long totalQueries, 
                              long totalInserts, long totalUpdates,
                              HighPerformanceCache.CacheStats queryCacheStats,
                              HighPerformanceCache.CacheStats userCacheStats) {
            this.totalVectors = totalVectors;
            this.totalUsers = totalUsers;
            this.totalQueries = totalQueries;
            this.totalInserts = totalInserts;
            this.totalUpdates = totalUpdates;
            this.queryCacheStats = queryCacheStats;
            this.userCacheStats = userCacheStats;
        }
        
        // Getter方法
        public int getTotalVectors() { return totalVectors; }
        public int getTotalUsers() { return totalUsers; }
        public long getTotalQueries() { return totalQueries; }
        public long getTotalInserts() { return totalInserts; }
        public long getTotalUpdates() { return totalUpdates; }
        public HighPerformanceCache.CacheStats getQueryCacheStats() { return queryCacheStats; }
        public HighPerformanceCache.CacheStats getUserCacheStats() { return userCacheStats; }
        
        @Override
        public String toString() {
            return String.format("VectorStoreStats{向量=%d, 用户=%d, 查询=%d, 插入=%d, 更新=%d, 查询缓存=%s, 用户缓存=%s}", 
                totalVectors, totalUsers, totalQueries, totalInserts, totalUpdates,
                queryCacheStats, userCacheStats);
        }
    }
}