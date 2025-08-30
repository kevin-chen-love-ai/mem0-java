package com.mem0.vector.impl;

import com.mem0.store.VectorStore;
import com.mem0.model.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 内存向量存储实现 / In-memory vector store implementation for development and testing
 * 
 * <p>提供基于内存的向量存储实现，主要用于开发和测试环境。将向量数据和元数据存储在内存的哈希映射中，
 * 支持向量的插入、更新、删除、搜索等操作，以及基于用户的向量管理功能。</p>
 * 
 * <p>Provides an in-memory vector store implementation primarily for development and testing environments. 
 * Stores vector data and metadata in memory hash maps, supporting vector insertion, update, deletion, 
 * search operations, and user-based vector management functionality.</p>
 * 
 * <p>主要特性 / Key features:</p>
 * <ul>
 *   <li>快速的向量CRUD操作 / Fast vector CRUD operations</li>
 *   <li>余弦相似度计算和搜索 / Cosine similarity calculation and search</li>
 *   <li>用户维度的向量管理 / User-based vector management</li>
 *   <li>向量计数和统计功能 / Vector counting and statistics functionality</li>
 *   <li>内存类型分布分析 / Memory type distribution analysis</li>
 *   <li>线程安全的并发操作 / Thread-safe concurrent operations</li>
 * </ul>
 * 
 * <p>使用示例 / Usage example:</p>
 * <pre>{@code
 * // 创建内存向量存储
 * InMemoryVectorStore vectorStore = new InMemoryVectorStore();
 * 
 * // 插入向量
 * float[] embedding = new float[]{0.1f, 0.2f, 0.3f};
 * Map<String, Object> metadata = Map.of("content", "text", "userId", "user123");
 * vectorStore.insert("vector1", embedding, metadata).join();
 * 
 * // 搜索相似向量
 * List<SearchResult> results = vectorStore.search(queryEmbedding, "user123", 10).join();
 * }</pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class InMemoryVectorStore implements VectorStore {
    
    private static final Logger logger = LoggerFactory.getLogger(InMemoryVectorStore.class);
    
    private final Map<String, VectorEntry> vectors = new ConcurrentHashMap<>();
    private final Map<String, List<String>> userMemories = new ConcurrentHashMap<>();
    
    private static class VectorEntry {
        final String id;
        final float[] embedding;
        final Map<String, Object> properties;
        
        VectorEntry(String id, float[] embedding, Map<String, Object> properties) {
            this.id = id;
            this.embedding = Arrays.copyOf(embedding, embedding.length);
            this.properties = new HashMap<>(properties);
        }
    }
    
    @Override
    public CompletableFuture<Void> createCollection(String collectionName, int dimension) {
        return CompletableFuture.supplyAsync(() -> {
            logger.debug("创建向量集合: {} (维度: {})", collectionName, dimension);
            // 内存实现中，集合是动态创建的
            return null;
        });
    }
    
    @Override
    public CompletableFuture<Boolean> collectionExists(String collectionName) {
        return CompletableFuture.supplyAsync(() -> true); // 内存实现中集合总是存在
    }
    
    @Override
    public CompletableFuture<Void> dropCollection(String collectionName) {
        return CompletableFuture.supplyAsync(() -> {
            logger.debug("删除向量集合: {}", collectionName);
            clear(); // 清空所有数据
            return null;
        });
    }
    
    @Override
    public CompletableFuture<String> insert(String collectionName, List<Float> vector, Map<String, Object> metadata) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String id = java.util.UUID.randomUUID().toString();
                logger.debug("插入向量: {}", id);
                
                // 转换List<Float>到float[]
                float[] embedding = new float[vector.size()];
                for (int i = 0; i < vector.size(); i++) {
                    embedding[i] = vector.get(i);
                }
                
                VectorEntry entry = new VectorEntry(id, embedding, metadata);
                vectors.put(id, entry);
                
                // 按用户跟踪
                String userId = (String) metadata.get("userId");
                if (userId != null) {
                    userMemories.computeIfAbsent(userId, k -> new ArrayList<>()).add(id);
                }
                
                logger.debug("向量插入成功: {}", id);
                return id;
            } catch (Exception e) {
                logger.error("向量插入失败", e);
                throw new RuntimeException("向量插入失败", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<String>> batchInsert(String collectionName, 
                                                      List<List<Float>> vectors,
                                                      List<Map<String, Object>> metadataList) {
        return CompletableFuture.supplyAsync(() -> {
            if (vectors.size() != metadataList.size()) {
                throw new IllegalArgumentException("向量和元数据列表大小不匹配");
            }
            
            logger.info("开始批量插入 {} 个向量到集合: {}", vectors.size(), collectionName);
            
            try {
                List<String> ids = new ArrayList<>();
                for (int i = 0; i < vectors.size(); i++) {
                    String id = insert(collectionName, vectors.get(i), metadataList.get(i)).join();
                    ids.add(id);
                }
                
                logger.info("批量插入完成，插入 {} 个向量", ids.size());
                return ids;
                
            } catch (Exception e) {
                logger.error("批量插入失败", e);
                throw new RuntimeException("批量插入失败", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<VectorStore.VectorSearchResult>> search(String collectionName,
                                                                         List<Float> queryVector,
                                                                         int topK,
                                                                         Map<String, Object> filter) {
        return CompletableFuture.supplyAsync(() -> {
            try {
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
                
                // 计算相似度
                List<VectorStore.VectorSearchResult> results = candidates.stream()
                    .filter(entry -> matchesFilter(entry, filter))
                    .map(entry -> {
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
    
    @Override
    public CompletableFuture<Void> delete(String collectionName, String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("删除向量: {}", id);
                
                VectorEntry entry = vectors.remove(id);
                if (entry != null) {
                    // 从用户跟踪中移除
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
                    logger.debug("向量删除成功: {}", id);
                    return null;
                } else {
                    logger.warn("向量不存在: {}", id);
                    return null;
                }
            } catch (Exception e) {
                logger.error("向量删除失败: " + id, e);
                throw new RuntimeException("向量删除失败", e);
            }
        });
    }
    
    public CompletableFuture<Void> insert(String id, float[] embedding, Map<String, Object> properties) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.debug("Inserting vector: {}", id);
                
                VectorEntry entry = new VectorEntry(id, embedding, properties);
                vectors.put(id, entry);
                
                // Track by user
                String userId = (String) properties.get("userId");
                if (userId != null) {
                    userMemories.computeIfAbsent(userId, k -> new ArrayList<>()).add(id);
                }
                
                logger.debug("Vector inserted successfully: {}", id);
            } catch (Exception e) {
                logger.error("Failed to insert vector: " + id, e);
                throw new RuntimeException("Failed to insert vector", e);
            }
        });
    }
    
    public CompletableFuture<Void> update(String id, float[] embedding, Map<String, Object> properties) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.debug("Updating vector: {}", id);
                
                VectorEntry existingEntry = vectors.get(id);
                if (existingEntry == null) {
                    throw new RuntimeException("Vector not found: " + id);
                }
                
                // Update properties while preserving existing ones
                Map<String, Object> updatedProperties = new HashMap<>(existingEntry.properties);
                updatedProperties.putAll(properties);
                
                VectorEntry updatedEntry = new VectorEntry(id, embedding, updatedProperties);
                vectors.put(id, updatedEntry);
                
                logger.debug("Vector updated successfully: {}", id);
            } catch (Exception e) {
                logger.error("Failed to update vector: " + id, e);
                throw new RuntimeException("Failed to update vector", e);
            }
        });
    }
    
    public CompletableFuture<Boolean> delete(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Deleting vector: {}", id);
                
                VectorEntry entry = vectors.remove(id);
                if (entry != null) {
                    // Remove from user tracking
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
                    logger.debug("Vector deleted successfully: {}", id);
                    return true;
                } else {
                    logger.warn("Vector not found for deletion: {}", id);
                    return false;
                }
            } catch (Exception e) {
                logger.error("Failed to delete vector: " + id, e);
                throw new RuntimeException("Failed to delete vector", e);
            }
        });
    }
    
    public CompletableFuture<SearchResult> get(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Getting vector: {}", id);
                
                VectorEntry entry = vectors.get(id);
                if (entry == null) {
                    throw new RuntimeException("Vector not found: " + id);
                }
                
                return new SearchResult(entry.id, 1.0f, entry.properties);
            } catch (Exception e) {
                logger.error("Failed to get vector: " + id, e);
                throw new RuntimeException("Failed to get vector", e);
            }
        });
    }
    
    public CompletableFuture<List<SearchResult>> search(float[] queryEmbedding, String userId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Searching vectors for user: {} with limit: {}", userId, limit);
                
                List<String> userMemoryIds = userMemories.getOrDefault(userId, Collections.emptyList());
                if (userMemoryIds.isEmpty()) {
                    return Collections.emptyList();
                }
                
                List<SearchResult> results = new ArrayList<>();
                
                for (String memoryId : userMemoryIds) {
                    VectorEntry entry = vectors.get(memoryId);
                    if (entry != null) {
                        float similarity = calculateCosineSimilarity(queryEmbedding, entry.embedding);
                        results.add(new SearchResult(entry.id, similarity, entry.properties));
                    }
                }
                
                // Sort by similarity descending and limit results
                return results.stream()
                        .sorted((a, b) -> Float.compare(b.similarity, a.similarity))
                        .limit(limit)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                logger.error("Failed to search vectors for user: " + userId, e);
                throw new RuntimeException("Failed to search vectors", e);
            }
        });
    }
    
    public CompletableFuture<List<SearchResult>> getAllByUser(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Getting all vectors for user: {}", userId);
                
                List<String> userMemoryIds = userMemories.getOrDefault(userId, Collections.emptyList());
                List<SearchResult> results = new ArrayList<>();
                
                for (String memoryId : userMemoryIds) {
                    VectorEntry entry = vectors.get(memoryId);
                    if (entry != null) {
                        results.add(new SearchResult(entry.id, 1.0f, entry.properties));
                    }
                }
                
                // Sort by creation time (most recent first)
                return results.stream()
                        .sorted((a, b) -> {
                            String timeA = (String) a.properties.getOrDefault("createdAt", "");
                            String timeB = (String) b.properties.getOrDefault("createdAt", "");
                            return timeB.compareTo(timeA);
                        })
                        .collect(Collectors.toList());
            } catch (Exception e) {
                logger.error("Failed to get all vectors for user: " + userId, e);
                throw new RuntimeException("Failed to get all vectors", e);
            }
        });
    }
    
    public CompletableFuture<Long> getMemoryCount(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<String> userMemoryIds = userMemories.getOrDefault(userId, Collections.emptyList());
                return (long) userMemoryIds.size();
            } catch (Exception e) {
                logger.error("Failed to get memory count for user: " + userId, e);
                throw new RuntimeException("Failed to get memory count", e);
            }
        });
    }
    
    public CompletableFuture<Map<String, Long>> getMemoryTypeDistribution(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Getting memory type distribution for user: {}", userId);
                
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
                logger.error("Failed to get memory type distribution for user: " + userId, e);
                throw new RuntimeException("Failed to get memory type distribution", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<VectorStore.VectorDocument> get(String collectionName, String id) {
        return CompletableFuture.supplyAsync(() -> {
            VectorEntry entry = vectors.get(id);
            if (entry == null) {
                return null;
            }
            
            // 转换float[]到List<Float>
            List<Float> embeddingList = new ArrayList<>();
            for (float f : entry.embedding) {
                embeddingList.add(f);
            }
            
            return new VectorStore.VectorDocument(id, embeddingList, entry.properties);
        });
    }
    
    @Override
    public CompletableFuture<Void> deleteByFilter(String collectionName, Map<String, Object> filter) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Deleting vectors by filter: {}", filter);
                
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
                    vectors.remove(id);
                    // 从用户内存索引中移除
                    for (Map.Entry<String, List<String>> userEntry : userMemories.entrySet()) {
                        userEntry.getValue().remove(id);
                    }
                }
                
                logger.debug("Deleted {} vectors by filter", toDelete.size());
                return null;
                
            } catch (Exception e) {
                logger.error("Failed to delete vectors by filter", e);
                throw new RuntimeException("Failed to delete vectors by filter", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Closing InMemoryVectorStore");
            vectors.clear();
            userMemories.clear();
            return null;
        });
    }
    
    /**
     * Calculate cosine similarity between two vectors
     */
    private float calculateCosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("Vectors must have the same length");
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
    
    // Additional utility methods
    public int getTotalVectorCount() {
        return vectors.size();
    }
    
    public Set<String> getAllUsers() {
        return new HashSet<>(userMemories.keySet());
    }
    
    public void clear() {
        logger.info("Clearing all vectors from store");
        vectors.clear();
        userMemories.clear();
    }
}