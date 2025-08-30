package com.mem0.unit.vector;

import com.mem0.vector.impl.InMemoryVectorStore;
import com.mem0.store.VectorStore;
import com.mem0.model.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 内存向量存储测试
 * 全面测试InMemoryVectorStore的所有功能
 */
@DisplayName("内存向量存储测试")
public class InMemoryVectorStoreTest {
    
    private InMemoryVectorStore vectorStore;
    private static final String TEST_COLLECTION = "test_collection";
    private static final String TEST_USER_ID = "test_user_123";
    
    @BeforeEach
    void setUp() {
        vectorStore = new InMemoryVectorStore();
    }
    
    @Nested
    @DisplayName("集合管理功能")
    class CollectionManagementTests {
        
        @Test
        @DisplayName("创建集合")
        void testCreateCollection() throws ExecutionException, InterruptedException {
            CompletableFuture<Void> future = vectorStore.createCollection(TEST_COLLECTION, 128);
            assertDoesNotThrow(() -> future.get());
        }
        
        @Test
        @DisplayName("检查集合存在")
        void testCollectionExists() throws ExecutionException, InterruptedException {
            CompletableFuture<Boolean> future = vectorStore.collectionExists(TEST_COLLECTION);
            assertTrue(future.get(), "内存实现中集合应该总是存在");
        }
        
        @Test
        @DisplayName("删除集合")
        void testDropCollection() throws ExecutionException, InterruptedException {
            // 先插入一些数据
            List<Float> vector = Arrays.asList(0.1f, 0.2f, 0.3f);
            Map<String, Object> metadata = createTestMetadata();
            vectorStore.insert(TEST_COLLECTION, vector, metadata).get();
            
            // 删除集合应该清空所有数据
            vectorStore.dropCollection(TEST_COLLECTION).get();
            assertEquals(0, vectorStore.getTotalVectorCount());
        }
    }
    
    @Nested
    @DisplayName("向量插入功能")
    class VectorInsertionTests {
        
        @Test
        @DisplayName("单个向量插入")
        void testSingleVectorInsert() throws ExecutionException, InterruptedException {
            List<Float> vector = Arrays.asList(0.1f, 0.2f, 0.3f);
            Map<String, Object> metadata = createTestMetadata();
            
            String id = vectorStore.insert(TEST_COLLECTION, vector, metadata).get();
            
            assertNotNull(id, "插入后应该返回有效的ID");
            assertEquals(1, vectorStore.getTotalVectorCount());
        }
        
        @Test
        @DisplayName("批量向量插入")
        void testBatchVectorInsert() throws ExecutionException, InterruptedException {
            List<List<Float>> vectors = Arrays.asList(
                Arrays.asList(0.1f, 0.2f, 0.3f),
                Arrays.asList(0.4f, 0.5f, 0.6f),
                Arrays.asList(0.7f, 0.8f, 0.9f)
            );
            
            List<Map<String, Object>> metadataList = IntStream.range(0, 3)
                .mapToObj(i -> {
                    Map<String, Object> metadata = createTestMetadata();
                    metadata.put("index", i);
                    return metadata;
                })
                .collect(Collectors.toList());
            
            List<String> ids = vectorStore.batchInsert(TEST_COLLECTION, vectors, metadataList).get();
            
            assertEquals(3, ids.size());
            assertEquals(3, vectorStore.getTotalVectorCount());
            assertTrue(ids.stream().allMatch(Objects::nonNull));
        }
        
        @Test
        @DisplayName("批量插入参数不匹配异常")
        void testBatchInsertMismatchedParameters() {
            List<List<Float>> vectors = Arrays.asList(Arrays.asList(0.1f, 0.2f));
            List<Map<String, Object>> metadataList = Arrays.asList(
                createTestMetadata(),
                createTestMetadata()
            );
            
            CompletableFuture<List<String>> future = 
                vectorStore.batchInsert(TEST_COLLECTION, vectors, metadataList);
            
            assertThrows(RuntimeException.class, future::join);
        }
        
        @Test
        @DisplayName("使用float数组插入向量")
        void testInsertWithFloatArray() throws ExecutionException, InterruptedException {
            float[] embedding = {0.1f, 0.2f, 0.3f};
            Map<String, Object> properties = createTestMetadata();
            String id = "test_vector_1";
            
            vectorStore.insert(id, embedding, properties).get();
            
            assertEquals(1, vectorStore.getTotalVectorCount());
            assertTrue(vectorStore.getAllUsers().contains(TEST_USER_ID));
        }
    }
    
    @Nested
    @DisplayName("向量搜索功能")
    class VectorSearchTests {
        
        @BeforeEach
        void setUpSearchData() throws ExecutionException, InterruptedException {
            // 插入测试数据
            List<List<Float>> vectors = Arrays.asList(
                Arrays.asList(1.0f, 0.0f, 0.0f),  // 与查询向量完全匹配
                Arrays.asList(0.8f, 0.6f, 0.0f),  // 部分匹配
                Arrays.asList(0.0f, 1.0f, 0.0f),  // 垂直向量
                Arrays.asList(-1.0f, 0.0f, 0.0f)  // 反向向量
            );
            
            List<Map<String, Object>> metadataList = IntStream.range(0, 4)
                .mapToObj(i -> {
                    Map<String, Object> metadata = createTestMetadata();
                    metadata.put("index", i);
                    metadata.put("type", i < 2 ? "similar" : "different");
                    return metadata;
                })
                .collect(Collectors.toList());
            
            vectorStore.batchInsert(TEST_COLLECTION, vectors, metadataList).get();
        }
        
        @Test
        @DisplayName("基础向量搜索")
        void testBasicVectorSearch() throws ExecutionException, InterruptedException {
            List<Float> queryVector = Arrays.asList(1.0f, 0.0f, 0.0f);
            
            List<VectorStore.VectorSearchResult> results = 
                vectorStore.search(TEST_COLLECTION, queryVector, 2, null).get();
            
            assertEquals(2, results.size());
            // 第一个结果应该是完全匹配的向量
            assertTrue(results.get(0).getScore() > 0.9f);
        }
        
        @Test
        @DisplayName("带过滤条件的搜索")
        void testSearchWithFilter() throws ExecutionException, InterruptedException {
            List<Float> queryVector = Arrays.asList(1.0f, 0.0f, 0.0f);
            Map<String, Object> filter = new HashMap<>();
            filter.put("userId", TEST_USER_ID);
            filter.put("type", "similar");
            
            List<VectorStore.VectorSearchResult> results = 
                vectorStore.search(TEST_COLLECTION, queryVector, 10, filter).get();
            
            assertEquals(2, results.size());
            assertTrue(results.stream()
                .allMatch(r -> "similar".equals(r.getMetadata().get("type"))));
        }
        
        @Test
        @DisplayName("用户特定搜索")
        void testUserSpecificSearch() throws ExecutionException, InterruptedException {
            float[] queryEmbedding = {1.0f, 0.0f, 0.0f};
            
            List<SearchResult> results = vectorStore.search(queryEmbedding, TEST_USER_ID, 3).get();
            
            assertEquals(3, results.size());
            // 结果应该按相似度降序排列
            for (int i = 0; i < results.size() - 1; i++) {
                assertTrue(results.get(i).similarity >= results.get(i + 1).similarity);
            }
        }
        
        @Test
        @DisplayName("空结果搜索")
        void testSearchWithNoResults() throws ExecutionException, InterruptedException {
            List<Float> queryVector = Arrays.asList(1.0f, 0.0f, 0.0f);
            Map<String, Object> filter = new HashMap<>();
            filter.put("userId", "nonexistent_user");
            
            List<VectorStore.VectorSearchResult> results = 
                vectorStore.search(TEST_COLLECTION, queryVector, 10, filter).get();
            
            assertTrue(results.isEmpty());
        }
    }
    
    @Nested
    @DisplayName("向量管理功能")
    class VectorManagementTests {
        
        private String testVectorId;
        
        @BeforeEach
        void setUpManagementData() throws ExecutionException, InterruptedException {
            List<Float> vector = Arrays.asList(0.1f, 0.2f, 0.3f);
            Map<String, Object> metadata = createTestMetadata();
            testVectorId = vectorStore.insert(TEST_COLLECTION, vector, metadata).get();
        }
        
        @Test
        @DisplayName("获取向量")
        void testGetVector() throws ExecutionException, InterruptedException {
            VectorStore.VectorDocument doc = vectorStore.get(TEST_COLLECTION, testVectorId).get();
            
            assertNotNull(doc);
            assertEquals(testVectorId, doc.getId());
            assertEquals(3, doc.getVector().size());
            assertTrue(doc.getMetadata().containsKey("userId"));
        }
        
        @Test
        @DisplayName("获取不存在的向量")
        void testGetNonexistentVector() throws ExecutionException, InterruptedException {
            VectorStore.VectorDocument doc = vectorStore.get(TEST_COLLECTION, "nonexistent").get();
            assertNull(doc);
        }
        
        @Test
        @DisplayName("更新向量")
        void testUpdateVector() throws ExecutionException, InterruptedException {
            float[] newEmbedding = {0.4f, 0.5f, 0.6f};
            Map<String, Object> newProperties = new HashMap<>();
            newProperties.put("updated", true);
            newProperties.put("timestamp", System.currentTimeMillis());
            
            assertDoesNotThrow(() -> vectorStore.update(testVectorId, newEmbedding, newProperties).get());
            
            SearchResult result = vectorStore.get(testVectorId).get();
            assertTrue((Boolean) result.properties.get("updated"));
        }
        
        @Test
        @DisplayName("更新不存在的向量")
        void testUpdateNonexistentVector() {
            float[] embedding = {0.1f, 0.2f, 0.3f};
            Map<String, Object> properties = new HashMap<>();
            
            CompletableFuture<Void> future = vectorStore.update("nonexistent", embedding, properties);
            assertThrows(RuntimeException.class, future::join);
        }
        
        @Test
        @DisplayName("删除向量")
        void testDeleteVector() throws ExecutionException, InterruptedException {
            assertTrue(vectorStore.delete(testVectorId).get());
            assertEquals(0, vectorStore.getTotalVectorCount());
        }
        
        @Test
        @DisplayName("删除不存在的向量")
        void testDeleteNonexistentVector() throws ExecutionException, InterruptedException {
            assertFalse(vectorStore.delete("nonexistent").get());
        }
        
        @Test
        @DisplayName("通过接口删除向量")
        void testDeleteVectorViaInterface() throws ExecutionException, InterruptedException {
            assertDoesNotThrow(() -> vectorStore.delete(TEST_COLLECTION, testVectorId).get());
            assertEquals(0, vectorStore.getTotalVectorCount());
        }
        
        @Test
        @DisplayName("按过滤条件删除向量")
        void testDeleteByFilter() throws ExecutionException, InterruptedException {
            // 插入更多测试数据
            Map<String, Object> metadata1 = createTestMetadata();
            metadata1.put("category", "test");
            vectorStore.insert(TEST_COLLECTION, Arrays.asList(0.4f, 0.5f, 0.6f), metadata1).get();
            
            Map<String, Object> metadata2 = createTestMetadata();
            metadata2.put("category", "production");
            vectorStore.insert(TEST_COLLECTION, Arrays.asList(0.7f, 0.8f, 0.9f), metadata2).get();
            
            // 按条件删除
            Map<String, Object> filter = new HashMap<>();
            filter.put("category", "test");
            vectorStore.deleteByFilter(TEST_COLLECTION, filter).get();
            
            assertEquals(2, vectorStore.getTotalVectorCount()); // 原始+production数据
        }
    }
    
    @Nested
    @DisplayName("用户管理功能")
    class UserManagementTests {
        
        @BeforeEach
        void setUpUserData() throws ExecutionException, InterruptedException {
            // 为不同用户插入数据
            String[] userIds = {"user1", "user2", "user3"};
            String[] types = {"personal", "work", "study"};
            
            for (int i = 0; i < userIds.length; i++) {
                for (int j = 0; j < 3; j++) {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("userId", userIds[i]);
                    metadata.put("type", types[i]);
                    metadata.put("index", j);
                    
                    List<Float> vector = Arrays.asList(
                        (float) (i + 0.1), 
                        (float) (j + 0.2), 
                        0.3f
                    );
                    
                    vectorStore.insert(TEST_COLLECTION, vector, metadata).get();
                }
            }
        }
        
        @Test
        @DisplayName("获取用户所有向量")
        void testGetAllByUser() throws ExecutionException, InterruptedException {
            List<SearchResult> results = vectorStore.getAllByUser("user1").get();
            
            assertEquals(3, results.size());
            assertTrue(results.stream()
                .allMatch(r -> "user1".equals(r.properties.get("userId"))));
        }
        
        @Test
        @DisplayName("获取用户向量数量")
        void testGetMemoryCount() throws ExecutionException, InterruptedException {
            Long count = vectorStore.getMemoryCount("user2").get();
            assertEquals(3L, count.longValue());
        }
        
        @Test
        @DisplayName("获取用户内存类型分布")
        void testGetMemoryTypeDistribution() throws ExecutionException, InterruptedException {
            Map<String, Long> distribution = vectorStore.getMemoryTypeDistribution("user3").get();
            
            assertEquals(1, distribution.size());
            assertEquals(3L, distribution.get("study").longValue());
        }
        
        @Test
        @DisplayName("获取所有用户列表")
        void testGetAllUsers() {
            Set<String> users = vectorStore.getAllUsers();
            
            assertEquals(3, users.size()); // setUpUserData中创建的3个用户
            assertTrue(users.containsAll(Arrays.asList("user1", "user2", "user3")));
        }
        
        @Test
        @DisplayName("不存在用户的操作")
        void testOperationsWithNonexistentUser() throws ExecutionException, InterruptedException {
            String nonexistentUser = "nonexistent_user";
            
            List<SearchResult> results = vectorStore.getAllByUser(nonexistentUser).get();
            assertTrue(results.isEmpty());
            
            Long count = vectorStore.getMemoryCount(nonexistentUser).get();
            assertEquals(0L, count.longValue());
            
            Map<String, Long> distribution = vectorStore.getMemoryTypeDistribution(nonexistentUser).get();
            assertTrue(distribution.isEmpty());
        }
    }
    
    @Nested
    @DisplayName("相似度计算测试")
    class SimilarityCalculationTests {
        
        @Test
        @DisplayName("相同向量的相似度")
        void testIdenticalVectorsSimilarity() throws ExecutionException, InterruptedException {
            float[] embedding = {1.0f, 0.0f, 0.0f};
            Map<String, Object> metadata = createTestMetadata();
            String id = "test_similarity_vector";
            vectorStore.insert(id, embedding, metadata).get();
            
            List<SearchResult> results = vectorStore.search(embedding, TEST_USER_ID, 1).get();
            
            assertFalse(results.isEmpty());
            assertTrue(results.get(0).similarity > 0.99f); // 应该非常接近1.0
        }
        
        @Test
        @DisplayName("垂直向量的相似度")
        void testOrthogonalVectorsSimilarity() throws ExecutionException, InterruptedException {
            // 插入垂直向量
            float[] embedding1 = {1.0f, 0.0f, 0.0f};
            float[] embedding2 = {0.0f, 1.0f, 0.0f};
            
            Map<String, Object> metadata = createTestMetadata();
            vectorStore.insert("test1", embedding1, metadata).get();
            vectorStore.insert("test2", embedding2, metadata).get();
            
            // 搜索应该显示较低的相似度
            List<SearchResult> results = vectorStore.search(embedding1, TEST_USER_ID, 2).get();
            
            assertEquals(2, results.size());
            // 第一个结果是自己，第二个是垂直向量，相似度应该接近0
            assertTrue(Math.abs(results.get(1).similarity) < 0.1f);
        }
    }
    
    @Nested
    @DisplayName("资源管理测试")
    class ResourceManagementTests {
        
        @Test
        @DisplayName("清空存储")
        void testClearStore() throws ExecutionException, InterruptedException {
            // 插入一些数据
            List<Float> vector = Arrays.asList(0.1f, 0.2f, 0.3f);
            vectorStore.insert(TEST_COLLECTION, vector, createTestMetadata()).get();
            
            assertTrue(vectorStore.getTotalVectorCount() > 0);
            
            vectorStore.clear();
            
            assertEquals(0, vectorStore.getTotalVectorCount());
            assertTrue(vectorStore.getAllUsers().isEmpty());
        }
        
        @Test
        @DisplayName("关闭存储")
        void testCloseStore() throws ExecutionException, InterruptedException {
            // 插入一些数据
            List<Float> vector = Arrays.asList(0.1f, 0.2f, 0.3f);
            vectorStore.insert(TEST_COLLECTION, vector, createTestMetadata()).get();
            
            assertDoesNotThrow(() -> vectorStore.close().get());
            assertEquals(0, vectorStore.getTotalVectorCount());
        }
    }
    
    @Nested
    @DisplayName("并发测试")
    class ConcurrencyTests {
        
        @Test
        @DisplayName("并发插入测试")
        void testConcurrentInsert() {
            int threadCount = 10;
            int operationsPerThread = 20;
            
            List<CompletableFuture<String>> futures = IntStream.range(0, threadCount)
                .boxed()
                .flatMap(threadId -> IntStream.range(0, operationsPerThread)
                    .mapToObj(opId -> {
                        List<Float> vector = Arrays.asList(
                            (float) threadId, 
                            (float) opId, 
                            0.3f
                        );
                        Map<String, Object> metadata = createTestMetadata();
                        metadata.put("threadId", threadId);
                        metadata.put("operationId", opId);
                        
                        return vectorStore.insert(TEST_COLLECTION, vector, metadata);
                    }))
                .collect(Collectors.toList());
            
            // 等待所有操作完成
            List<String> ids = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
            
            assertEquals(threadCount * operationsPerThread, ids.size());
            assertEquals(threadCount * operationsPerThread, vectorStore.getTotalVectorCount());
            assertTrue(ids.stream().allMatch(Objects::nonNull));
        }
        
        @Test
        @DisplayName("并发搜索测试")
        void testConcurrentSearch() throws ExecutionException, InterruptedException {
            // 先插入一些数据
            for (int i = 0; i < 50; i++) {
                List<Float> vector = Arrays.asList((float) i, 0.2f, 0.3f);
                vectorStore.insert(TEST_COLLECTION, vector, createTestMetadata()).get();
            }
            
            List<Float> queryVector = Arrays.asList(25.0f, 0.2f, 0.3f);
            int threadCount = 20;
            
            List<CompletableFuture<List<VectorStore.VectorSearchResult>>> futures = 
                IntStream.range(0, threadCount)
                    .mapToObj(i -> vectorStore.search(TEST_COLLECTION, queryVector, 10, null))
                    .collect(Collectors.toList());
            
            // 等待所有搜索完成
            List<List<VectorStore.VectorSearchResult>> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
            
            assertEquals(threadCount, results.size());
            assertTrue(results.stream().allMatch(result -> result.size() <= 10));
        }
    }
    
    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTests {
        
        @Test
        @DisplayName("空向量异常")
        void testEmptyVectorException() {
            List<Float> emptyVector = Collections.emptyList();
            Map<String, Object> metadata = createTestMetadata();
            
            assertDoesNotThrow(() -> {
                String id = vectorStore.insert(TEST_COLLECTION, emptyVector, metadata).get();
                assertNotNull(id);
            });
        }
        
        @Test
        @DisplayName("空元数据处理")
        void testEmptyMetadata() throws ExecutionException, InterruptedException {
            List<Float> vector = Arrays.asList(0.1f, 0.2f, 0.3f);
            Map<String, Object> emptyMetadata = new HashMap<>();
            
            String id = vectorStore.insert(TEST_COLLECTION, vector, emptyMetadata).get();
            assertNotNull(id);
            
            VectorStore.VectorDocument doc = vectorStore.get(TEST_COLLECTION, id).get();
            assertNotNull(doc);
        }
    }
    
    // 辅助方法
    private Map<String, Object> createTestMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", TEST_USER_ID);
        metadata.put("content", "test content");
        metadata.put("type", "FACTUAL");
        metadata.put("createdAt", String.valueOf(System.currentTimeMillis()));
        metadata.put("source", "test");
        return metadata;
    }
}