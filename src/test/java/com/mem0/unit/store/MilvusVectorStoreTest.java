package com.mem0.unit.store;

import com.mem0.store.MilvusVectorStore;
import com.mem0.store.VectorStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MilvusVectorStore单元测试
 */
@DisplayName("MilvusVectorStore Tests")
public class MilvusVectorStoreTest {
    
    private MilvusVectorStore vectorStore;
    
    @BeforeEach
    void setUp() {
        vectorStore = new MilvusVectorStore("localhost:19530");
    }
    
    @Test
    @DisplayName("应该正确初始化MilvusVectorStore")
    void shouldInitializeCorrectly() {
        assertNotNull(vectorStore);
        assertEquals("localhost", vectorStore.getHost());
        assertEquals(19530, vectorStore.getPort());
        assertTrue(vectorStore.isConnected());
    }
    
    @Test
    @DisplayName("应该抛出异常当连接字符串格式无效时")
    void shouldThrowExceptionForInvalidConnectionString() {
        assertThrows(IllegalArgumentException.class, () -> {
            new MilvusVectorStore("invalid-format");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new MilvusVectorStore("localhost:invalid-port");
        });
    }
    
    @Test
    @DisplayName("应该创建集合")
    void shouldCreateCollection() throws ExecutionException, InterruptedException {
        String collectionName = "test_collection";
        int dimension = 128;
        
        vectorStore.createCollection(collectionName, dimension).get();
        
        assertTrue(vectorStore.collectionExists(collectionName).get());
        assertEquals(1, vectorStore.getCollectionCount());
    }
    
    @Test
    @DisplayName("应该抛出异常当创建已存在的集合时")
    void shouldThrowExceptionWhenCreatingExistingCollection() throws ExecutionException, InterruptedException {
        String collectionName = "test_collection";
        int dimension = 128;
        
        vectorStore.createCollection(collectionName, dimension).get();
        
        assertThrows(ExecutionException.class, () -> {
            vectorStore.createCollection(collectionName, dimension).get();
        });
    }
    
    @Test
    @DisplayName("应该插入向量")
    void shouldInsertVector() throws ExecutionException, InterruptedException {
        String collectionName = "test_collection";
        int dimension = 128;
        
        vectorStore.createCollection(collectionName, dimension).get();
        
        List<Float> vector = generateRandomVector(dimension);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("content", "test content");
        metadata.put("importance", 5);
        
        String vectorId = vectorStore.insert(collectionName, vector, metadata).get();
        
        assertNotNull(vectorId);
        assertEquals(1, vectorStore.getVectorCount(collectionName));
        
        VectorStore.VectorDocument document = vectorStore.get(collectionName, vectorId).get();
        assertNotNull(document);
        assertEquals(vectorId, document.getId());
        assertEquals(vector, document.getVector());
        assertEquals("test content", document.getMetadata().get("content"));
        assertEquals(5, document.getMetadata().get("importance"));
    }
    
    @Test
    @DisplayName("应该批量插入向量")
    void shouldBatchInsertVectors() throws ExecutionException, InterruptedException {
        String collectionName = "test_collection";
        int dimension = 128;
        
        vectorStore.createCollection(collectionName, dimension).get();
        
        List<List<Float>> vectors = Arrays.asList(
            generateRandomVector(dimension),
            generateRandomVector(dimension),
            generateRandomVector(dimension)
        );
        
        List<Map<String, Object>> metadataList = Arrays.asList(
            createMetadata("content1", 1),
            createMetadata("content2", 2),
            createMetadata("content3", 3)
        );
        
        List<String> vectorIds = vectorStore.batchInsert(collectionName, vectors, metadataList).get();
        
        assertEquals(3, vectorIds.size());
        assertEquals(3, vectorStore.getVectorCount(collectionName));
        
        for (int i = 0; i < vectorIds.size(); i++) {
            VectorStore.VectorDocument document = vectorStore.get(collectionName, vectorIds.get(i)).get();
            assertNotNull(document);
            assertEquals(vectors.get(i), document.getVector());
            assertEquals(metadataList.get(i).get("content"), document.getMetadata().get("content"));
        }
    }
    
    @Test
    @DisplayName("应该搜索相似向量")
    void shouldSearchSimilarVectors() throws ExecutionException, InterruptedException {
        String collectionName = "test_collection";
        int dimension = 128;
        
        vectorStore.createCollection(collectionName, dimension).get();
        
        // 插入一些测试向量
        List<Float> vector1 = generateRandomVector(dimension);
        List<Float> vector2 = generateRandomVector(dimension);
        List<Float> vector3 = generateRandomVector(dimension);
        
        vectorStore.insert(collectionName, vector1, createMetadata("content1")).get();
        vectorStore.insert(collectionName, vector2, createMetadata("content2")).get();
        vectorStore.insert(collectionName, vector3, createMetadata("content3")).get();
        
        // 搜索相似向量
        List<VectorStore.VectorSearchResult> results = vectorStore.search(collectionName, vector1, 2, null).get();
        
        assertEquals(2, results.size());
        assertTrue(results.get(0).getScore() >= results.get(1).getScore()); // 按相似度降序排序
    }
    
    @Test
    @DisplayName("应该使用过滤器搜索")
    void shouldSearchWithFilter() throws ExecutionException, InterruptedException {
        String collectionName = "test_collection";
        int dimension = 128;
        
        vectorStore.createCollection(collectionName, dimension).get();
        
        // 插入带有不同重要性的向量
        vectorStore.insert(collectionName, generateRandomVector(dimension), 
                          createMetadata("content1", 3)).get();
        vectorStore.insert(collectionName, generateRandomVector(dimension), 
                          createMetadata("content2", 5)).get();
        vectorStore.insert(collectionName, generateRandomVector(dimension), 
                          createMetadata("content3", 7)).get();
        
        // 搜索重要性大于等于5的向量
        Map<String, Object> filter = new HashMap<>();
        filter.put("importance", ">=5");
        List<VectorStore.VectorSearchResult> results = vectorStore.search(collectionName, 
                                                             generateRandomVector(dimension), 10, filter).get();
        
        assertEquals(2, results.size());
        for (VectorStore.VectorSearchResult result : results) {
            int importance = (Integer) result.getMetadata().get("importance");
            assertTrue(importance >= 5);
        }
    }
    
    @Test
    @DisplayName("应该删除向量")
    void shouldDeleteVector() throws ExecutionException, InterruptedException {
        String collectionName = "test_collection";
        int dimension = 128;
        
        vectorStore.createCollection(collectionName, dimension).get();
        
        List<Float> vector = generateRandomVector(dimension);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("content", "test");
        String vectorId = vectorStore.insert(collectionName, vector, metadata).get();
        
        assertEquals(1, vectorStore.getVectorCount(collectionName));
        
        vectorStore.delete(collectionName, vectorId).get();
        
        assertEquals(0, vectorStore.getVectorCount(collectionName));
        assertNull(vectorStore.get(collectionName, vectorId).get());
    }
    
    @Test
    @DisplayName("应该按过滤器删除向量")
    void shouldDeleteByFilter() throws ExecutionException, InterruptedException {
        String collectionName = "test_collection";
        int dimension = 128;
        
        vectorStore.createCollection(collectionName, dimension).get();
        
        // 插入带有不同重要性的向量
        vectorStore.insert(collectionName, generateRandomVector(dimension), 
                          createMetadata("content1", 3)).get();
        vectorStore.insert(collectionName, generateRandomVector(dimension), 
                          createMetadata("content2", 5)).get();
        vectorStore.insert(collectionName, generateRandomVector(dimension), 
                          createMetadata("content3", 7)).get();
        
        assertEquals(3, vectorStore.getVectorCount(collectionName));
        
        // 删除重要性大于等于5的向量
        Map<String, Object> filter = new HashMap<>();
        filter.put("importance", ">=5");
        vectorStore.deleteByFilter(collectionName, filter).get();
        
        assertEquals(1, vectorStore.getVectorCount(collectionName));
    }
    
    @Test
    @DisplayName("应该删除集合")
    void shouldDropCollection() throws ExecutionException, InterruptedException {
        String collectionName = "test_collection";
        int dimension = 128;
        
        vectorStore.createCollection(collectionName, dimension).get();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("content", "test");
        vectorStore.insert(collectionName, generateRandomVector(dimension), metadata).get();
        
        assertEquals(1, vectorStore.getCollectionCount());
        assertEquals(1, vectorStore.getVectorCount(collectionName));
        
        vectorStore.dropCollection(collectionName).get();
        
        assertEquals(0, vectorStore.getCollectionCount());
        assertFalse(vectorStore.collectionExists(collectionName).get());
    }
    
    @Test
    @DisplayName("应该关闭连接")
    void shouldCloseConnection() throws ExecutionException, InterruptedException {
        assertTrue(vectorStore.isConnected());
        
        vectorStore.close().get();
        
        assertFalse(vectorStore.isConnected());
    }
    
    /**
     * 创建元数据
     */
    private Map<String, Object> createMetadata(String content) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("content", content);
        return metadata;
    }
    
    /**
     * 创建带重要性的元数据
     */
    private Map<String, Object> createMetadata(String content, int importance) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("content", content);
        metadata.put("importance", importance);
        return metadata;
    }
    
    /**
     * 生成随机向量
     */
    private List<Float> generateRandomVector(int dimension) {
        List<Float> vector = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 0; i < dimension; i++) {
            vector.add(random.nextFloat());
        }
        
        return vector;
    }
}
