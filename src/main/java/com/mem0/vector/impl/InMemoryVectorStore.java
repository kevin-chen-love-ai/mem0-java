package com.mem0.vector.impl;

import com.mem0.store.VectorStore;
import com.mem0.model.SearchResult;
import com.mem0.exception.VectorOperationException;
import com.mem0.exception.MemoryValidationException;
import com.mem0.constants.MemoryConstants;
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
    private final Map<String, Integer> collections = new ConcurrentHashMap<>();
    
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
            // Input validation
            if (collectionName == null || collectionName.trim().isEmpty()) {
                throw new MemoryValidationException("Collection name cannot be null or empty");
            }
            if (dimension <= 0 || dimension > MemoryConstants.MAX_VECTOR_DIMENSION) {
                throw new MemoryValidationException("维度必须在1到" + MemoryConstants.MAX_VECTOR_DIMENSION + "之间");
            }
            
            // Check if collection already exists
            if (collections.containsKey(collectionName)) {
                throw new MemoryValidationException("集合'" + collectionName + "'已存在");
            }
            
            // Create collection by storing its dimension
            collections.put(collectionName, dimension);
            
            logger.debug("创建向量集合: {} (维度: {})", collectionName, dimension);
            return null;
        });
    }
    
    @Override
    public CompletableFuture<Boolean> collectionExists(String collectionName) {
        return CompletableFuture.supplyAsync(() -> {
            // Input validation
            if (collectionName == null || collectionName.trim().isEmpty()) {
                throw new IllegalArgumentException("Collection name cannot be null or empty");
            }
            
            // Check if collection was explicitly created
            return collections.containsKey(collectionName);
        });
    }
    
    @Override
    public CompletableFuture<Void> dropCollection(String collectionName) {
        return CompletableFuture.supplyAsync(() -> {
            // Input validation
            if (collectionName == null || collectionName.trim().isEmpty()) {
                throw new IllegalArgumentException("Collection name cannot be null or empty");
            }
            
            logger.debug("删除向量集合: {}", collectionName);
            
            // Remove collection from tracking
            collections.remove(collectionName);
            
            // Remove only vectors belonging to this collection
            vectors.entrySet().removeIf(entry -> 
                collectionName.equals(entry.getValue().properties.get("collection")));
            
            return null;
        });
    }
    
    @Override
    public CompletableFuture<String> insert(String collectionName, List<Float> vector, Map<String, Object> metadata) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Input validation
                validateInsertInput(collectionName, vector, metadata);
                
                String id = java.util.UUID.randomUUID().toString();
                logger.debug("插入向量: {}", id);
                
                // 转换List<Float>到float[]
                float[] embedding = new float[vector.size()];
                for (int i = 0; i < vector.size(); i++) {
                    embedding[i] = vector.get(i);
                }
                
                // Add collection info to metadata
                Map<String, Object> fullMetadata = new java.util.HashMap<>(metadata != null ? metadata : new java.util.HashMap<>());
                fullMetadata.put("collection", collectionName);
                
                VectorEntry entry = new VectorEntry(id, embedding, fullMetadata);
                vectors.put(id, entry);
                
                // 按用户跟踪
                String userId = (String) fullMetadata.get("userId");
                if (userId != null) {
                    userMemories.compute(userId, (k, v) -> {
                        List<String> userMems = v;
                        if (userMems == null) {
                            userMems = new ArrayList<>();
                        }
                        userMems.add(id);
                        return userMems;
                    });
                }
                
                logger.debug("向量插入成功: {}", id);
                return id;
            } catch (MemoryValidationException e) {
                // Re-throw validation exceptions as-is
                throw e;
            } catch (Exception e) {
                logger.error("Vector insert operation failed for collection: {}", collectionName, e);
                throw new VectorOperationException("Failed to insert vector into collection: " + collectionName, e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<String>> batchInsert(String collectionName, 
                                                      List<List<Float>> vectors,
                                                      List<Map<String, Object>> metadataList) {
        return CompletableFuture.supplyAsync(() -> {
            // Input validation
            if (collectionName == null || collectionName.trim().isEmpty()) {
                throw new MemoryValidationException("Collection name cannot be null or empty");
            }
            if (vectors == null || vectors.isEmpty()) {
                throw new MemoryValidationException("Vectors list cannot be null or empty");
            }
            if (metadataList == null) {
                throw new MemoryValidationException("Metadata list cannot be null");
            }
            if (vectors.size() != metadataList.size()) {
                throw new MemoryValidationException("向量和元数据数量不匹配");
            }
            if (vectors.size() > MemoryConstants.MAX_BATCH_SIZE) { // Reasonable batch size limit
                throw new MemoryValidationException("Batch size too large (max " + MemoryConstants.MAX_BATCH_SIZE + " vectors)");
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
                
            } catch (MemoryValidationException | VectorOperationException e) {
                // Re-throw specific exceptions as-is
                throw e;
            } catch (Exception e) {
                logger.error("Batch insert operation failed for collection: {}", collectionName, e);
                throw new VectorOperationException("Failed to batch insert vectors into collection: " + collectionName, e);
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
                // Input validation
                validateSearchInput(collectionName, queryVector, topK, filter);
                
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
            } catch (MemoryValidationException e) {
                // Re-throw validation exceptions as-is
                throw e;
            } catch (Exception e) {
                logger.error("Vector search operation failed for collection: {}", collectionName, e);
                throw new VectorOperationException("Failed to search vectors in collection: " + collectionName, e);
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
                // Input validation
                if (collectionName == null || collectionName.trim().isEmpty()) {
                    throw new MemoryValidationException("Collection name cannot be null or empty");
                }
                if (id == null || id.trim().isEmpty()) {
                    throw new MemoryValidationException("Vector ID cannot be null or empty");
                }
                
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
            } catch (MemoryValidationException e) {
                // Re-throw validation exceptions as-is
                throw e;
            } catch (Exception e) {
                logger.error("Vector delete operation failed for ID: {}", id, e);
                throw new VectorOperationException("Failed to delete vector with ID: " + id, e);
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
                    userMemories.compute(userId, (k, v) -> {
                        List<String> userMems = v;
                        if (userMems == null) {
                            userMems = new ArrayList<>();
                        }
                        userMems.add(id);
                        return userMems;
                    });
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
            // Input validation
            if (collectionName == null || collectionName.trim().isEmpty()) {
                throw new IllegalArgumentException("Collection name cannot be null or empty");
            }
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("Vector ID cannot be null or empty");
            }
            
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
                // Input validation
                if (collectionName == null || collectionName.trim().isEmpty()) {
                    throw new IllegalArgumentException("Collection name cannot be null or empty");
                }
                if (filter == null || filter.isEmpty()) {
                    throw new IllegalArgumentException("Filter cannot be null or empty for safety");
                }
                validateMetadata(filter);
                
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
        
        // 向量化计算优化
        for (int i = 0; i < vectorA.length; i++) {
            double aVal = vectorA[i];
            double bVal = vectorB[i];
            dotProduct += aVal * bVal;
            normA += aVal * aVal;
            normB += bVal * bVal;
        }
        
        double magnitude = Math.sqrt(normA) * Math.sqrt(normB);
        return magnitude == 0.0 ? 0.0f : (float) (dotProduct / magnitude);
    }
    
    /**
     * Calculate Euclidean distance between two vectors
     */
    private float calculateEuclideanDistance(float[] vectorA, float[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("Vectors must have the same length");
        }
        
        double sum = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            double diff = vectorA[i] - vectorB[i];
            sum += diff * diff;
        }
        
        return (float) Math.sqrt(sum);
    }
    
    /**
     * Calculate dot product similarity between two vectors
     */
    private float calculateDotProductSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("Vectors must have the same length");
        }
        
        double dotProduct = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
        }
        
        return (float) dotProduct;
    }
    
    /**
     * Calculate Manhattan distance between two vectors
     */
    private float calculateManhattanDistance(float[] vectorA, float[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("Vectors must have the same length");
        }
        
        float sum = 0.0f;
        for (int i = 0; i < vectorA.length; i++) {
            sum += Math.abs(vectorA[i] - vectorB[i]);
        }
        
        return sum;
    }
    
    /**
     * Calculate angular distance (based on cosine similarity)
     */
    private float calculateAngularDistance(float[] vectorA, float[] vectorB) {
        float cosineSim = calculateCosineSimilarity(vectorA, vectorB);
        // Convert to angular distance: arccos(cosine_similarity) / π
        return (float) (Math.acos(Math.max(-1.0, Math.min(1.0, cosineSim))) / Math.PI);
    }
    
    /**
     * Enum for similarity metrics
     */
    public enum SimilarityMetric {
        COSINE,
        EUCLIDEAN,
        DOT_PRODUCT,
        MANHATTAN,
        ANGULAR
    }
    
    /**
     * Calculate similarity using specified metric
     */
    private float calculateSimilarity(float[] vectorA, float[] vectorB, SimilarityMetric metric) {
        switch (metric) {
            case COSINE:
                return calculateCosineSimilarity(vectorA, vectorB);
            case EUCLIDEAN:
                // Convert distance to similarity (higher is better)
                return 1.0f / (1.0f + calculateEuclideanDistance(vectorA, vectorB));
            case DOT_PRODUCT:
                return calculateDotProductSimilarity(vectorA, vectorB);
            case MANHATTAN:
                // Convert distance to similarity (higher is better)
                return 1.0f / (1.0f + calculateManhattanDistance(vectorA, vectorB));
            case ANGULAR:
                // Convert distance to similarity (higher is better)
                return 1.0f - calculateAngularDistance(vectorA, vectorB);
            default:
                return calculateCosineSimilarity(vectorA, vectorB);
        }
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
        collections.clear();
    }
    
    // Input validation methods
    private void validateInsertInput(String collectionName, List<Float> vector, Map<String, Object> metadata) {
        if (collectionName == null || collectionName.trim().isEmpty()) {
            throw new MemoryValidationException("Collection name cannot be null or empty");
        }
        
        if (collectionName.length() > MemoryConstants.MAX_COLLECTION_NAME_LENGTH) {
            throw new MemoryValidationException("Collection name too long (max " + MemoryConstants.MAX_COLLECTION_NAME_LENGTH + " characters)");
        }
        
        // Check if collection exists
        Integer expectedDimension = collections.get(collectionName);
        if (expectedDimension == null) {
            throw new MemoryValidationException("集合不存在: " + collectionName);
        }
        
        if (vector == null || vector.isEmpty()) {
            throw new MemoryValidationException("Vector cannot be null or empty");
        }
        
        if (vector.size() > MemoryConstants.MAX_VECTOR_DIMENSION) { // Reasonable limit for vector dimensions
            throw new MemoryValidationException("Vector dimension too large (max " + MemoryConstants.MAX_VECTOR_DIMENSION + ")");
        }
        
        // Check if vector dimension matches collection dimension
        if (vector.size() != expectedDimension) {
            throw new MemoryValidationException("向量维度不匹配: 期望 " + expectedDimension + ", 实际 " + vector.size());
        }
        
        // Check for null values in vector
        for (int i = 0; i < vector.size(); i++) {
            Float value = vector.get(i);
            if (value == null || !Float.isFinite(value)) {
                throw new MemoryValidationException("Vector contains invalid value at index " + i + ": " + value);
            }
        }
        
        if (metadata != null) {
            validateMetadata(metadata);
        }
    }
    
    private void validateSearchInput(String collectionName, List<Float> queryVector, int topK, Map<String, Object> filter) {
        if (collectionName == null || collectionName.trim().isEmpty()) {
            throw new MemoryValidationException("Collection name cannot be null or empty");
        }
        
        if (queryVector == null || queryVector.isEmpty()) {
            throw new MemoryValidationException("Query vector cannot be null or empty");
        }
        
        if (topK <= 0 || topK > MemoryConstants.MAX_SEARCH_TOPK) { // Reasonable limit
            throw new MemoryValidationException("TopK must be between 1 and " + MemoryConstants.MAX_SEARCH_TOPK);
        }
        
        // Check for null values in query vector
        for (int i = 0; i < queryVector.size(); i++) {
            Float value = queryVector.get(i);
            if (value == null || !Float.isFinite(value)) {
                throw new MemoryValidationException("Query vector contains invalid value at index " + i + ": " + value);
            }
        }
        
        if (filter != null) {
            validateMetadata(filter);
        }
    }
    
    private void validateMetadata(Map<String, Object> metadata) {
        if (metadata.size() > MemoryConstants.MAX_METADATA_FIELDS_COUNT) { // Reasonable limit for metadata fields
            throw new MemoryValidationException("Too many metadata fields (max " + MemoryConstants.MAX_METADATA_FIELDS_COUNT + ")");
        }
        
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (key == null || key.trim().isEmpty()) {
                throw new MemoryValidationException("Metadata key cannot be null or empty");
            }
            
            if (key.length() > MemoryConstants.MAX_METADATA_KEY_LENGTH) {
                throw new MemoryValidationException("Metadata key too long (max " + MemoryConstants.MAX_METADATA_KEY_LENGTH + " characters): " + key);
            }
            
            // Validate value types and sizes
            if (value != null) {
                if (value instanceof String) {
                    String strValue = (String) value;
                    if (strValue.length() > MemoryConstants.MAX_METADATA_STRING_VALUE_LENGTH) { // Reasonable limit for string values
                        throw new MemoryValidationException("Metadata string value too long (max " + MemoryConstants.MAX_METADATA_STRING_VALUE_LENGTH + " characters) for key: " + key);
                    }
                } else if (value instanceof Number) {
                    Number numValue = (Number) value;
                    if (!Double.isFinite(numValue.doubleValue())) {
                        throw new MemoryValidationException("Metadata numeric value is invalid for key: " + key);
                    }
                } else if (value instanceof Boolean) {
                    // Boolean is allowed
                } else if (value instanceof java.util.Map || value instanceof java.util.List) {
                    // Allow complex objects like Map and List for flexible metadata
                    // In production, you might want to serialize these to JSON strings
                } else {
                    // Allow other types but warn about potential serialization issues
                    logger.debug("Metadata contains non-primitive type for key '{}': {}", key, value.getClass().getSimpleName());
                }
            }
        }
    }
}