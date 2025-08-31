package com.mem0.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Milvus向量数据库存储实现类
 * 
 * 此类实现了高性能向量存储和搜索功能。
 * 目前使用内存存储作为基础实现，支持向量的存储、搜索和删除操作。
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class MilvusVectorStore implements VectorStore {
    
    private static final Logger logger = LoggerFactory.getLogger(MilvusVectorStore.class);
    
    private final String host;
    private final int port;
    private volatile boolean connected = true;
    
    // 内存存储
    private final Map<String, CollectionInfo> collections = new ConcurrentHashMap<>();
    private final Map<String, Map<String, VectorDocument>> vectorStorage = new ConcurrentHashMap<>();
    
    /**
     * 集合信息内部类
     */
    private static class CollectionInfo {
        private final String name;
        private final int dimension;
        private final long createdAt;
        
        public CollectionInfo(String name, int dimension) {
            this.name = name;
            this.dimension = dimension;
            this.createdAt = System.currentTimeMillis();
        }
        
        public String getName() { return name; }
        public int getDimension() { return dimension; }
        public long getCreatedAt() { return createdAt; }
    }
    
    /**
     * 构造函数 - 通过连接字符串初始化
     * 
     * @param connectionString 连接字符串，格式为 "host:port"
     */
    public MilvusVectorStore(String connectionString) {
        // 解析连接字符串
        String[] parts = connectionString.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid connection string format. Expected 'host:port'");
        }
        
        this.host = parts[0];
        try {
            this.port = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port number in connection string: " + parts[1]);
        }
        
        logger.info("Initialized MilvusVectorStore for {}:{}", host, port);
    }
    
    private void ensureConnected() {
        if (!connected) {
            throw new IllegalStateException("MilvusVectorStore is not connected");
        }
    }
    
    @Override
    public CompletableFuture<Void> createCollection(String collectionName, int dimension) {
        return CompletableFuture.supplyAsync(() -> {
            ensureConnected();
            
            if (collections.containsKey(collectionName)) {
                logger.warn("Collection {} already exists", collectionName);
                throw new IllegalStateException("Collection " + collectionName + " already exists");
            }
            
            if (dimension <= 0) {
                throw new IllegalArgumentException("Dimension must be positive, got: " + dimension);
            }
            
            // 创建集合
            CollectionInfo collectionInfo = new CollectionInfo(collectionName, dimension);
            collections.put(collectionName, collectionInfo);
            vectorStorage.put(collectionName, new ConcurrentHashMap<>());
            
            logger.info("Created collection '{}' with dimension {}", collectionName, dimension);
            return null;
        });
    }
    
    @Override
    public CompletableFuture<Boolean> collectionExists(String collectionName) {
        return CompletableFuture.supplyAsync(() -> {
            ensureConnected();
            boolean exists = collections.containsKey(collectionName);
            logger.debug("Collection '{}' exists: {}", collectionName, exists);
            return exists;
        });
    }
    
    @Override
    public CompletableFuture<Void> dropCollection(String collectionName) {
        return CompletableFuture.supplyAsync(() -> {
            ensureConnected();
            
            if (!collections.containsKey(collectionName)) {
                logger.warn("Collection {} does not exist", collectionName);
                throw new IllegalStateException("Collection " + collectionName + " does not exist");
            }
            
            // 删除集合和所有向量数据
            collections.remove(collectionName);
            Map<String, VectorDocument> removed = vectorStorage.remove(collectionName);
            int removedCount = removed != null ? removed.size() : 0;
            
            logger.info("Dropped collection '{}' and removed {} vectors", collectionName, removedCount);
            return null;
        });
    }
    
    @Override
    public CompletableFuture<String> insert(String collectionName, List<Float> vector, 
                                          Map<String, Object> metadata) {
        return CompletableFuture.supplyAsync(() -> {
            ensureConnected();
            
            CollectionInfo collection = collections.get(collectionName);
            if (collection == null) {
                throw new IllegalStateException("Collection " + collectionName + " does not exist");
            }
            
            if (vector == null || vector.isEmpty()) {
                throw new IllegalArgumentException("Vector cannot be null or empty");
            }
            
            if (vector.size() != collection.getDimension()) {
                throw new IllegalArgumentException(
                    String.format("Vector dimension %d does not match collection dimension %d", 
                                vector.size(), collection.getDimension()));
            }
            
            // 生成向量ID
            String vectorId = UUID.randomUUID().toString();
            
            // 创建向量文档
            Map<String, Object> safeMetadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
            safeMetadata.put("inserted_at", System.currentTimeMillis());
            
            VectorDocument document = new VectorDocument(vectorId, new ArrayList<>(vector), safeMetadata);
            
            // 存储向量
            vectorStorage.get(collectionName).put(vectorId, document);
            
            logger.debug("Inserted vector {} into collection '{}'", vectorId, collectionName);
            return vectorId;
        });
    }
    
    @Override
    public CompletableFuture<List<String>> batchInsert(String collectionName, 
                                                      List<List<Float>> vectors,
                                                      List<Map<String, Object>> metadataList) {
        return CompletableFuture.supplyAsync(() -> {
            ensureConnected();
            
            if (vectors == null || vectors.isEmpty()) {
                throw new IllegalArgumentException("Vectors list cannot be null or empty");
            }
            
            if (metadataList != null && vectors.size() != metadataList.size()) {
                throw new IllegalArgumentException("Vectors and metadata lists must have the same size");
            }
            
            CollectionInfo collection = collections.get(collectionName);
            if (collection == null) {
                throw new IllegalStateException("Collection " + collectionName + " does not exist");
            }
            
            List<String> insertedIds = new ArrayList<>();
            Map<String, VectorDocument> collectionStorage = vectorStorage.get(collectionName);
            
            for (int i = 0; i < vectors.size(); i++) {
                List<Float> vector = vectors.get(i);
                
                if (vector == null || vector.isEmpty()) {
                    throw new IllegalArgumentException("Vector at index " + i + " cannot be null or empty");
                }
                
                if (vector.size() != collection.getDimension()) {
                    throw new IllegalArgumentException(
                        String.format("Vector at index %d has dimension %d, expected %d", 
                                    i, vector.size(), collection.getDimension()));
                }
                
                // 生成向量ID
                String vectorId = UUID.randomUUID().toString();
                
                // 创建向量文档
                Map<String, Object> metadata = (metadataList != null && i < metadataList.size()) 
                    ? new HashMap<>(metadataList.get(i)) 
                    : new HashMap<>();
                metadata.put("inserted_at", System.currentTimeMillis());
                metadata.put("batch_index", i);
                
                VectorDocument document = new VectorDocument(vectorId, new ArrayList<>(vector), metadata);
                collectionStorage.put(vectorId, document);
                insertedIds.add(vectorId);
            }
            
            logger.info("Batch inserted {} vectors into collection '{}'", insertedIds.size(), collectionName);
            return insertedIds;
        });
    }
    
    @Override
    public CompletableFuture<List<VectorSearchResult>> search(String collectionName,
                                                             List<Float> queryVector,
                                                             int topK,
                                                             Map<String, Object> filter) {
        return CompletableFuture.supplyAsync(() -> {
            ensureConnected();
            
            CollectionInfo collection = collections.get(collectionName);
            if (collection == null) {
                throw new IllegalStateException("Collection " + collectionName + " does not exist");
            }
            
            if (queryVector == null || queryVector.isEmpty()) {
                throw new IllegalArgumentException("Query vector cannot be null or empty");
            }
            
            if (queryVector.size() != collection.getDimension()) {
                throw new IllegalArgumentException(
                    String.format("Query vector dimension %d does not match collection dimension %d", 
                                queryVector.size(), collection.getDimension()));
            }
            
            if (topK <= 0) {
                throw new IllegalArgumentException("topK must be positive, got: " + topK);
            }
            
            Map<String, VectorDocument> collectionStorage = vectorStorage.get(collectionName);
            if (collectionStorage.isEmpty()) {
                logger.debug("No vectors found in collection '{}'", collectionName);
                return Collections.emptyList();
            }
            
            // 执行向量搜索
            List<VectorSearchResult> allResults = new ArrayList<>();
            
            for (VectorDocument document : collectionStorage.values()) {
                // 应用过滤器
                if (filter != null && !matchesFilter(document.getMetadata(), filter)) {
                    continue;
                }
                
                // 计算余弦相似度
                float similarity = calculateCosineSimilarity(queryVector, document.getVector());
                
                VectorSearchResult result = new VectorSearchResult(
                    document.getId(),
                    similarity,
                    new HashMap<>(document.getMetadata()),
                    new ArrayList<>(document.getVector())
                );
                
                allResults.add(result);
            }
            
            // 按相似度降序排序并取前topK个结果
            List<VectorSearchResult> topResults = allResults.stream()
                .sorted((a, b) -> Float.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .collect(Collectors.toList());
            
            logger.debug("Found {} results for search in collection '{}' (requested topK: {})", 
                        topResults.size(), collectionName, topK);
            
            return topResults;
        });
    }
    
    @Override
    public CompletableFuture<Void> delete(String collectionName, String id) {
        return CompletableFuture.supplyAsync(() -> {
            ensureConnected();
            
            Map<String, VectorDocument> collectionStorage = vectorStorage.get(collectionName);
            if (collectionStorage == null) {
                throw new IllegalStateException("Collection " + collectionName + " does not exist");
            }
            
            VectorDocument removed = collectionStorage.remove(id);
            if (removed == null) {
                logger.warn("Vector with id {} not found in collection '{}'", id, collectionName);
            } else {
                logger.debug("Deleted vector {} from collection '{}'", id, collectionName);
            }
            
            return null;
        });
    }
    
    @Override
    public CompletableFuture<Void> deleteByFilter(String collectionName, Map<String, Object> filter) {
        return CompletableFuture.supplyAsync(() -> {
            ensureConnected();
            
            Map<String, VectorDocument> collectionStorage = vectorStorage.get(collectionName);
            if (collectionStorage == null) {
                throw new IllegalStateException("Collection " + collectionName + " does not exist");
            }
            
            if (filter == null || filter.isEmpty()) {
                logger.warn("Empty filter provided for deleteByFilter, no vectors deleted");
                return null;
            }
            
            // 查找匹配的向量
            List<String> toDelete = new ArrayList<>();
            for (VectorDocument document : collectionStorage.values()) {
                if (matchesFilter(document.getMetadata(), filter)) {
                    toDelete.add(document.getId());
                }
            }
            
            // 删除匹配的向量
            int deletedCount = 0;
            for (String id : toDelete) {
                if (collectionStorage.remove(id) != null) {
                    deletedCount++;
                }
            }
            
            logger.info("Deleted {} vectors from collection '{}' using filter", 
                       deletedCount, collectionName);
            
            return null;
        });
    }
    
    @Override
    public CompletableFuture<VectorDocument> get(String collectionName, String id) {
        return CompletableFuture.supplyAsync(() -> {
            ensureConnected();
            
            Map<String, VectorDocument> collectionStorage = vectorStorage.get(collectionName);
            if (collectionStorage == null) {
                throw new IllegalStateException("Collection " + collectionName + " does not exist");
            }
            
            VectorDocument document = collectionStorage.get(id);
            if (document == null) {
                logger.debug("Vector with id {} not found in collection '{}'", id, collectionName);
                return null;
            }
            
            // 返回文档的副本以避免外部修改
            return new VectorDocument(
                document.getId(),
                new ArrayList<>(document.getVector()),
                new HashMap<>(document.getMetadata())
            );
        });
    }
    
    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.supplyAsync(() -> {
            if (connected) {
                // 清理资源
                collections.clear();
                vectorStorage.clear();
                connected = false;
                
                logger.info("Closed MilvusVectorStore connection to {}:{}", host, port);
            }
            return null;
        });
    }
    
    /**
     * 计算两个向量的余弦相似度
     */
    private float calculateCosineSimilarity(List<Float> vector1, List<Float> vector2) {
        if (vector1.size() != vector2.size()) {
            throw new IllegalArgumentException("Vector dimensions do not match");
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vector1.size(); i++) {
            dotProduct += vector1.get(i) * vector2.get(i);
            norm1 += vector1.get(i) * vector1.get(i);
            norm2 += vector2.get(i) * vector2.get(i);
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0f;
        }
        
        return (float) (dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2)));
    }
    
    /**
     * 检查元数据是否匹配过滤条件
     */
    private boolean matchesFilter(Map<String, Object> metadata, Map<String, Object> filter) {
        for (Map.Entry<String, Object> filterEntry : filter.entrySet()) {
            String filterKey = filterEntry.getKey();
            Object filterValue = filterEntry.getValue();
            
            if (!metadata.containsKey(filterKey)) {
                return false;
            }
            
            Object metadataValue = metadata.get(filterKey);
            
            // 处理不同类型的过滤条件
            if (filterValue instanceof String && ((String) filterValue).startsWith(">=")) {
                // 数值比较：>= 操作
                try {
                    double filterNum = Double.parseDouble(((String) filterValue).substring(2).trim());
                    double metadataNum = Double.parseDouble(metadataValue.toString());
                    if (metadataNum < filterNum) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
            } else if (filterValue instanceof String && ((String) filterValue).startsWith(">")) {
                // 数值比较：> 操作
                try {
                    double filterNum = Double.parseDouble(((String) filterValue).substring(1).trim());
                    double metadataNum = Double.parseDouble(metadataValue.toString());
                    if (metadataNum <= filterNum) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
            } else {
                // 精确匹配
                if (!Objects.equals(metadataValue, filterValue)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * 获取连接状态
     */
    public boolean isConnected() {
        return connected;
    }
    
    /**
     * 获取主机地址
     */
    public String getHost() {
        return host;
    }
    
    /**
     * 获取端口号
     */
    public int getPort() {
        return port;
    }
    
    /**
     * 获取集合数量
     */
    public int getCollectionCount() {
        return collections.size();
    }
    
    /**
     * 获取指定集合的向量数量
     */
    public int getVectorCount(String collectionName) {
        Map<String, VectorDocument> collectionStorage = vectorStorage.get(collectionName);
        return collectionStorage != null ? collectionStorage.size() : 0;
    }
}
