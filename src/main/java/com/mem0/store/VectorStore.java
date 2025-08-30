package com.mem0.store;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 向量存储接口 / Vector Store Interface
 * 
 * 定义向量数据库的标准操作接口，支持高维向量的存储、检索、管理和相似性搜索。
 * 为内存系统提供语义搜索和向量化内存检索的核心存储能力，支持多种向量数据库实现。
 * Defines standard operations interface for vector databases, supporting storage, retrieval, 
 * management and similarity search of high-dimensional vectors. Provides core storage capabilities
 * for semantic search and vectorized memory retrieval in memory systems.
 * 
 * <h3>核心功能 / Key Features:</h3>
 * <ul>
 *   <li>向量集合管理(创建/删除/检查存在性) / Vector collection management (create/delete/existence check)</li>
 *   <li>高维向量存储和批量插入操作 / High-dimensional vector storage and batch insertion</li>
 *   <li>基于余弦相似度的语义搜索 / Semantic search based on cosine similarity</li>
 *   <li>元数据过滤和条件查询支持 / Metadata filtering and conditional query support</li>
 *   <li>向量文档CRUD操作和生命周期管理 / Vector document CRUD operations and lifecycle management</li>
 *   <li>异步操作和高并发支持 / Asynchronous operations and high concurrency support</li>
 * </ul>
 * 
 * <h3>操作接口体系 / Operation Interface System:</h3>
 * <pre>
 * VectorStore 接口架构 / Interface Architecture:
 * 
 * ┌─────────────────────────────────────────────────────────┐
 * │                   VectorStore                           │
 * ├─────────────────────────────────────────────────────────┤
 * │  集合管理 / Collection Management                        │
 * │  ├─ createCollection()    // 创建向量集合              │
 * │  ├─ collectionExists()    // 检查集合存在              │
 * │  └─ dropCollection()      // 删除向量集合              │
 * │                                                         │
 * │  向量操作 / Vector Operations                            │
 * │  ├─ insert()             // 单条向量插入               │
 * │  ├─ batchInsert()        // 批量向量插入               │
 * │  ├─ search()             // 相似性向量搜索             │
 * │  ├─ get()                // 根据ID获取向量文档          │
 * │  └─ delete()             // 删除向量记录               │
 * │                                                         │
 * │  高级操作 / Advanced Operations                          │
 * │  ├─ deleteByFilter()     // 条件删除                  │
 * │  └─ close()              // 资源清理                  │
 * └─────────────────────────────────────────────────────────┘
 * 
 * 数据模型 / Data Models:
 * ├─ VectorDocument        // 向量文档(ID + Vector + Metadata)
 * └─ VectorSearchResult    // 搜索结果(ID + Score + Metadata + Vector)
 * </pre>
 * 
 * <h3>使用示例 / Usage Examples:</h3>
 * <pre>{@code
 * // 初始化向量存储
 * VectorStore vectorStore = new MilvusVectorStore("localhost:19530");
 * // 或使用内存实现: VectorStore vectorStore = new InMemoryVectorStore();
 * 
 * // 创建向量集合
 * String collectionName = "memory_embeddings";
 * int vectorDimension = 768; // 例如使用BERT嵌入维度
 * 
 * CompletableFuture<Void> createFuture = vectorStore.createCollection(collectionName, vectorDimension);
 * createFuture.join(); // 等待创建完成
 * 
 * // 插入向量数据
 * List<Float> memoryVector = Arrays.asList(0.1f, 0.2f, 0.3f, ...); // 768维向量
 * Map<String, Object> metadata = new HashMap<>();
 * metadata.put("memory_id", "mem_001");
 * metadata.put("user_id", "user_123");
 * metadata.put("importance", 4);
 * metadata.put("content", "用户喜欢喝咖啡");
 * metadata.put("created_at", System.currentTimeMillis());
 * 
 * CompletableFuture<String> insertFuture = vectorStore.insert(collectionName, memoryVector, metadata);
 * String vectorId = insertFuture.join();
 * System.out.println("插入向量ID: " + vectorId);
 * 
 * // 批量插入向量
 * List<List<Float>> vectorBatch = Arrays.asList(vector1, vector2, vector3);
 * List<Map<String, Object>> metadataBatch = Arrays.asList(metadata1, metadata2, metadata3);
 * 
 * CompletableFuture<List<String>> batchInsertFuture = 
 *     vectorStore.batchInsert(collectionName, vectorBatch, metadataBatch);
 * List<String> batchIds = batchInsertFuture.join();
 * System.out.println("批量插入向量数量: " + batchIds.size());
 * 
 * // 语义搜索相似向量
 * List<Float> queryVector = Arrays.asList(0.15f, 0.25f, 0.35f, ...); // 查询向量
 * int topK = 10; // 返回最相似的10个结果
 * Map<String, Object> searchFilter = new HashMap<>();
 * searchFilter.put("user_id", "user_123"); // 只搜索特定用户的向量
 * searchFilter.put("importance", ">= 3");  // 只搜索重要性>=3的向量
 * 
 * CompletableFuture<List<VectorSearchResult>> searchFuture = 
 *     vectorStore.search(collectionName, queryVector, topK, searchFilter);
 * List<VectorSearchResult> results = searchFuture.join();
 * 
 * // 处理搜索结果
 * for (VectorSearchResult result : results) {
 *     System.out.println("向量ID: " + result.getId());
 *     System.out.println("相似度分数: " + result.getScore()); // 0.0-1.0
 *     System.out.println("内存内容: " + result.getMetadata().get("content"));
 *     System.out.println("重要性: " + result.getMetadata().get("importance"));
 *     System.out.println("---");
 * }
 * 
 * // 获取特定向量文档
 * CompletableFuture<VectorDocument> getFuture = vectorStore.get(collectionName, vectorId);
 * VectorDocument document = getFuture.join();
 * if (document != null) {
 *     System.out.println("向量维度: " + document.getVector().size());
 *     System.out.println("元数据: " + document.getMetadata());
 * }
 * 
 * // 删除向量
 * CompletableFuture<Void> deleteFuture = vectorStore.delete(collectionName, vectorId);
 * deleteFuture.join();
 * 
 * // 批量条件删除
 * Map<String, Object> deleteFilter = new HashMap<>();
 * deleteFilter.put("user_id", "user_to_remove");
 * CompletableFuture<Void> filterDeleteFuture = vectorStore.deleteByFilter(collectionName, deleteFilter);
 * filterDeleteFuture.join();
 * 
 * // 清理资源
 * vectorStore.close().join();
 * }</pre>
 * 
 * <h3>实现建议 / Implementation Guidelines:</h3>
 * <ul>
 *   <li><b>异步处理</b>: 所有操作均返回CompletableFuture支持异步执行 / All operations return CompletableFuture for async execution</li>
 *   <li><b>批量优化</b>: 实现批量操作以提升大数据量处理效率 / Implement batch operations for improved performance with large datasets</li>
 *   <li><b>错误处理</b>: 妥善处理网络异常、存储满载等异常情况 / Proper handling of network exceptions, storage overflow, etc.</li>
 *   <li><b>连接池</b>: 使用连接池管理数据库连接避免频繁建连 / Use connection pooling to avoid frequent connection establishment</li>
 *   <li><b>索引优化</b>: 合理配置向量索引参数平衡搜索速度和精度 / Properly configure vector index parameters to balance search speed and accuracy</li>
 * </ul>
 * 
 * <h3>常见实现 / Common Implementations:</h3>
 * <ul>
 *   <li><b>MilvusVectorStore</b>: 基于Milvus的分布式向量数据库实现 / Distributed vector database implementation based on Milvus</li>
 *   <li><b>InMemoryVectorStore</b>: 基于内存的向量存储，适用于开发测试 / In-memory vector storage for development and testing</li>
 *   <li><b>HighPerformanceVectorStore</b>: 高性能向量存储实现 / High-performance vector storage implementation</li>
 *   <li><b>PineconeVectorStore</b>: 基于Pinecone云服务的向量存储 / Pinecone cloud service based vector storage</li>
 * </ul>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 * @see com.mem0.store.MilvusVectorStore
 * @see com.mem0.vector.impl.InMemoryVectorStore
 * @see com.mem0.embedding.EmbeddingProvider
 */
public interface VectorStore {
    
    CompletableFuture<Void> createCollection(String collectionName, int dimension);
    
    CompletableFuture<Boolean> collectionExists(String collectionName);
    
    CompletableFuture<Void> dropCollection(String collectionName);
    
    CompletableFuture<String> insert(String collectionName, List<Float> vector, 
                                   Map<String, Object> metadata);
    
    CompletableFuture<List<String>> batchInsert(String collectionName, 
                                              List<List<Float>> vectors,
                                              List<Map<String, Object>> metadataList);
    
    CompletableFuture<List<VectorSearchResult>> search(String collectionName,
                                                     List<Float> queryVector,
                                                     int topK,
                                                     Map<String, Object> filter);
    
    CompletableFuture<Void> delete(String collectionName, String id);
    
    CompletableFuture<Void> deleteByFilter(String collectionName, Map<String, Object> filter);
    
    CompletableFuture<VectorDocument> get(String collectionName, String id);
    
    CompletableFuture<Void> close();
    
    static class VectorSearchResult {
        private final String id;
        private final float score;
        private final Map<String, Object> metadata;
        private final List<Float> vector;
        
        public VectorSearchResult(String id, float score, Map<String, Object> metadata, List<Float> vector) {
            this.id = id;
            this.score = score;
            this.metadata = metadata;
            this.vector = vector;
        }
        
        public String getId() { return id; }
        public float getScore() { return score; }
        public Map<String, Object> getMetadata() { return metadata; }
        public List<Float> getVector() { return vector; }
    }
    
    static class VectorDocument {
        private final String id;
        private final List<Float> vector;
        private final Map<String, Object> metadata;
        
        public VectorDocument(String id, List<Float> vector, Map<String, Object> metadata) {
            this.id = id;
            this.vector = vector;
            this.metadata = metadata;
        }
        
        public String getId() { return id; }
        public List<Float> getVector() { return vector; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
}