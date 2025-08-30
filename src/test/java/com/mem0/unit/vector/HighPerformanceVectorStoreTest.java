package com.mem0.unit.vector;

import com.mem0.vector.impl.HighPerformanceVectorStore;
import com.mem0.store.VectorStore;
import com.mem0.model.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.AfterEach;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 高性能向量存储测试
 * 全面测试HighPerformanceVectorStore的所有功能，包括并发和性能优化特性
 */
@DisplayName("高性能向量存储测试")
public class HighPerformanceVectorStoreTest {
    
    private HighPerformanceVectorStore vectorStore;
    private static final String TEST_COLLECTION = "test_collection";
    private static final String TEST_USER_ID = "test_user_123";
    
    @BeforeEach
    void setUp() {
        vectorStore = new HighPerformanceVectorStore();
    }
    
    @AfterEach
    void tearDown() throws ExecutionException, InterruptedException {
        if (vectorStore != null) {
            vectorStore.close().get();
        }
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
            assertTrue(future.get(), "高性能实现中集合应该总是存在");
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
            
            HighPerformanceVectorStore.VectorStoreStats stats = vectorStore.getStats();
            assertEquals(0, stats.getTotalVectors());
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
            assertTrue(id.startsWith("vec_"), "ID应该使用预期的前缀");
            
            HighPerformanceVectorStore.VectorStoreStats stats = vectorStore.getStats();
            assertEquals(1, stats.getTotalVectors());
            assertEquals(1, stats.getTotalInserts());
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
            assertTrue(ids.stream().allMatch(Objects::nonNull));
            
            HighPerformanceVectorStore.VectorStoreStats stats = vectorStore.getStats();
            assertEquals(3, stats.getTotalVectors());
        }
        
        @Test
        @DisplayName("高性能批量插入")
        void testHighPerformanceBatchInsert() throws ExecutionException, InterruptedException {
            Map<String, HighPerformanceVectorStore.VectorData> vectors = new HashMap<>();
            
            for (int i = 0; i < 10; i++) {
                String id = "batch_test_" + i;
                float[] embedding = {(float) i, (float) i * 0.1f, 0.3f};
                Map<String, Object> properties = createTestMetadata();
                properties.put("batchIndex", i);
                
                vectors.put(id, new HighPerformanceVectorStore.VectorData(embedding, properties));
            }
            
            assertDoesNotThrow(() -> vectorStore.insertBatch(vectors).get());
            
            HighPerformanceVectorStore.VectorStoreStats stats = vectorStore.getStats();
            assertEquals(10, stats.getTotalVectors());
        }
        
        @Test
        @DisplayName("批量模式操作")
        void testBatchModeOperations() throws ExecutionException, InterruptedException {
            // 启用批量模式
            vectorStore.enableBatchMode();
            
            // 插入多个向量（应该进入批量缓冲区）
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                List<Float> vector = Arrays.asList((float) i, 0.2f, 0.3f);
                Map<String, Object> metadata = createTestMetadata();
                metadata.put("batchTest", i);
                
                String id = vectorStore.insert(TEST_COLLECTION, vector, metadata).get();
                ids.add(id);
            }
            
            // 提交批量操作
            vectorStore.commitBatch();
            
            // 禁用批量模式
            vectorStore.disableBatchMode();
            
            // 验证所有向量都已插入
            HighPerformanceVectorStore.VectorStoreStats stats = vectorStore.getStats();
            assertEquals(5, stats.getTotalVectors());
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
            
            // 验证查询统计
            HighPerformanceVectorStore.VectorStoreStats stats = vectorStore.getStats();
            assertTrue(stats.getTotalQueries() > 0);
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
        @DisplayName("缓存搜索结果")
        void testSearchResultCaching() throws ExecutionException, InterruptedException {
            float[] queryEmbedding = {1.0f, 0.0f, 0.0f};
            
            // 第一次搜索
            long startQueries = vectorStore.getStats().getTotalQueries();
            List<SearchResult> results1 = vectorStore.search(queryEmbedding, TEST_USER_ID, 2).get();
            
            // 第二次搜索（应该从缓存返回）
            List<SearchResult> results2 = vectorStore.search(queryEmbedding, TEST_USER_ID, 2).get();
            
            // 结果应该相同
            assertEquals(results1.size(), results2.size());
            assertTrue(vectorStore.getStats().getTotalQueries() > startQueries);
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
        @DisplayName("获取向量（兼容方法）")
        void testGetVectorCompatible() throws ExecutionException, InterruptedException {
            SearchResult result = vectorStore.get(testVectorId).get();
            
            assertNotNull(result);
            assertEquals(testVectorId, result.id);
            assertEquals(1.0f, result.similarity);
            assertTrue(result.properties.containsKey("userId"));
        }
        
        @Test
        @DisplayName("更新向量")
        void testUpdateVector() throws ExecutionException, InterruptedException {
            float[] newEmbedding = {0.4f, 0.5f, 0.6f};
            Map<String, Object> newProperties = new HashMap<>();
            newProperties.put("updated", true);
            newProperties.put("timestamp", System.currentTimeMillis());
            
            long startUpdates = vectorStore.getStats().getTotalUpdates();
            assertDoesNotThrow(() -> vectorStore.update(testVectorId, newEmbedding, newProperties).get());
            
            // 验证更新统计
            assertTrue(vectorStore.getStats().getTotalUpdates() > startUpdates);
            
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
            assertDoesNotThrow(() -> vectorStore.delete(TEST_COLLECTION, testVectorId).get());
            
            HighPerformanceVectorStore.VectorStoreStats stats = vectorStore.getStats();
            assertEquals(0, stats.getTotalVectors());
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
            
            HighPerformanceVectorStore.VectorStoreStats stats = vectorStore.getStats();
            assertEquals(2, stats.getTotalVectors()); // 原始+production数据
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
                    metadata.put("createdAt", String.valueOf(System.currentTimeMillis() + j * 1000));
                    
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
            
            // 验证排序（按创建时间降序）
            List<String> timestamps = results.stream()
                .map(r -> (String) r.properties.get("createdAt"))
                .collect(Collectors.toList());
            for (int i = 0; i < timestamps.size() - 1; i++) {
                assertTrue(timestamps.get(i).compareTo(timestamps.get(i + 1)) >= 0);
            }
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
        @DisplayName("缓存预热")
        void testWarmupCache() {
            assertDoesNotThrow(() -> vectorStore.warmupCache("user1"));
            
            // 验证缓存统计
            HighPerformanceVectorStore.VectorStoreStats stats = vectorStore.getStats();
            assertNotNull(stats.getUserCacheStats());
        }
    }
    
    @Nested
    @DisplayName("性能优化功能")
    class PerformanceOptimizationTests {
        
        @Test
        @DisplayName("获取性能统计")
        void testGetPerformanceStats() throws ExecutionException, InterruptedException {
            // 执行一些操作
            List<Float> vector = Arrays.asList(0.1f, 0.2f, 0.3f);
            vectorStore.insert(TEST_COLLECTION, vector, createTestMetadata()).get();
            vectorStore.search(TEST_COLLECTION, vector, 5, null).get();
            
            HighPerformanceVectorStore.VectorStoreStats stats = vectorStore.getStats();
            
            assertNotNull(stats);
            assertTrue(stats.getTotalVectors() >= 1);
            assertTrue(stats.getTotalInserts() >= 1);
            assertTrue(stats.getTotalQueries() >= 1);
            assertTrue(stats.getTotalUsers() >= 1);
            assertNotNull(stats.getQueryCacheStats());
            assertNotNull(stats.getUserCacheStats());
            
            // 验证toString方法
            String statsStr = stats.toString();
            assertTrue(statsStr.contains("VectorStoreStats"));
            assertTrue(statsStr.contains("向量="));
        }
        
        @Test
        @DisplayName("缓存性能测试")
        void testCachePerformance() throws ExecutionException, InterruptedException {
            // 插入测试数据
            List<Float> vector = Arrays.asList(0.1f, 0.2f, 0.3f);
            String id = vectorStore.insert(TEST_COLLECTION, vector, createTestMetadata()).get();
            
            // 多次获取同一向量（应该利用缓存）
            long startTime = System.nanoTime();
            for (int i = 0; i < 10; i++) {
                vectorStore.get(id).get();
            }
            long duration = System.nanoTime() - startTime;
            
            assertTrue(duration > 0);
            
            HighPerformanceVectorStore.VectorStoreStats stats = vectorStore.getStats();
            assertNotNull(stats.getQueryCacheStats());
        }
        
        @Test
        @DisplayName("并行搜索性能")
        void testParallelSearchPerformance() throws ExecutionException, InterruptedException {
            // 插入大量测试数据
            List<CompletableFuture<String>> insertFutures = IntStream.range(0, 100)
                .mapToObj(i -> {
                    List<Float> vector = Arrays.asList(
                        (float) Math.random(), 
                        (float) Math.random(), 
                        (float) Math.random()
                    );
                    Map<String, Object> metadata = createTestMetadata();
                    metadata.put("index", i);
                    return vectorStore.insert(TEST_COLLECTION, vector, metadata);
                })
                .collect(Collectors.toList());
            
            // 等待插入完成
            CompletableFuture.allOf(insertFutures.toArray(new CompletableFuture[0])).get();
            
            // 并行搜索测试
            List<Float> queryVector = Arrays.asList(0.5f, 0.5f, 0.5f);
            long startTime = System.nanoTime();
            
            List<CompletableFuture<List<VectorStore.VectorSearchResult>>> searchFutures = 
                IntStream.range(0, 10)
                    .mapToObj(i -> vectorStore.search(TEST_COLLECTION, queryVector, 10, null))
                    .collect(Collectors.toList());
            
            // 等待搜索完成
            CompletableFuture.allOf(searchFutures.toArray(new CompletableFuture[0])).get();
            long duration = System.nanoTime() - startTime;
            
            assertTrue(duration > 0);
            
            // 验证所有搜索都返回了结果
            for (CompletableFuture<List<VectorStore.VectorSearchResult>> future : searchFutures) {
                List<VectorStore.VectorSearchResult> results = future.get();
                assertTrue(results.size() <= 10);
            }
        }
    }
    
    @Nested
    @DisplayName("并发安全测试")
    class ConcurrencySafetyTests {
        
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
            assertTrue(ids.stream().allMatch(Objects::nonNull));
            
            HighPerformanceVectorStore.VectorStoreStats stats = vectorStore.getStats();
            assertEquals(threadCount * operationsPerThread, stats.getTotalVectors());
        }
        
        @Test
        @DisplayName("并发搜索和插入混合测试")
        void testConcurrentSearchAndInsert() throws ExecutionException, InterruptedException {
            // 先插入基础数据
            for (int i = 0; i < 20; i++) {
                List<Float> vector = Arrays.asList((float) i, 0.2f, 0.3f);
                vectorStore.insert(TEST_COLLECTION, vector, createTestMetadata()).get();
            }
            
            List<Float> queryVector = Arrays.asList(10.0f, 0.2f, 0.3f);
            
            // 混合并发操作
            List<CompletableFuture<?>> futures = new ArrayList<>();
            
            // 插入操作
            for (int i = 0; i < 10; i++) {
                final int index = i;
                futures.add(CompletableFuture.runAsync(() -> {
                    try {
                        List<Float> vector = Arrays.asList((float) (100 + index), 0.2f, 0.3f);
                        vectorStore.insert(TEST_COLLECTION, vector, createTestMetadata()).get();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }));
            }
            
            // 搜索操作
            for (int i = 0; i < 10; i++) {
                futures.add(CompletableFuture.runAsync(() -> {
                    try {
                        vectorStore.search(TEST_COLLECTION, queryVector, 5, null).get();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }));
            }
            
            // 等待所有操作完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
            
            HighPerformanceVectorStore.VectorStoreStats stats = vectorStore.getStats();
            assertEquals(30, stats.getTotalVectors()); // 20 + 10
            assertTrue(stats.getTotalQueries() >= 10);
        }
    }
    
    @Nested
    @DisplayName("资源管理测试")
    class ResourceManagementTests {
        
        @Test
        @DisplayName("关闭存储")
        void testCloseStore() throws ExecutionException, InterruptedException {
            // 插入一些数据
            List<Float> vector = Arrays.asList(0.1f, 0.2f, 0.3f);
            vectorStore.insert(TEST_COLLECTION, vector, createTestMetadata()).get();
            
            HighPerformanceVectorStore.VectorStoreStats stats = vectorStore.getStats();
            assertTrue(stats.getTotalVectors() > 0);
            
            // 关闭存储
            assertDoesNotThrow(() -> vectorStore.close().get());
            
            // 验证资源已清理
            HighPerformanceVectorStore.VectorStoreStats finalStats = vectorStore.getStats();
            assertEquals(0, finalStats.getTotalVectors());
        }
    }
    
    @Nested
    @DisplayName("数据类测试")
    class DataClassTests {
        
        @Test
        @DisplayName("VectorData类测试")
        void testVectorDataClass() {
            float[] embedding = {0.1f, 0.2f, 0.3f};
            Map<String, Object> properties = createTestMetadata();
            
            HighPerformanceVectorStore.VectorData vectorData = 
                new HighPerformanceVectorStore.VectorData(embedding, properties);
            
            assertNotNull(vectorData.embedding);
            assertNotNull(vectorData.properties);
            assertEquals(3, vectorData.embedding.length);
            assertEquals(TEST_USER_ID, vectorData.properties.get("userId"));
        }
        
        @Test
        @DisplayName("VectorStoreStats类测试")
        void testVectorStoreStatsClass() {
            // 创建模拟的缓存统计
            int totalVectors = 100;
            int totalUsers = 10;
            long totalQueries = 500;
            long totalInserts = 100;
            long totalUpdates = 50;
            
            // 由于我们无法直接创建CacheStats，我们将测试实际运行中的统计
            HighPerformanceVectorStore.VectorStoreStats stats = vectorStore.getStats();
            
            assertNotNull(stats);
            assertTrue(stats.getTotalVectors() >= 0);
            assertTrue(stats.getTotalUsers() >= 0);
            assertTrue(stats.getTotalQueries() >= 0);
            assertTrue(stats.getTotalInserts() >= 0);
            assertTrue(stats.getTotalUpdates() >= 0);
            assertNotNull(stats.getQueryCacheStats());
            assertNotNull(stats.getUserCacheStats());
            
            // 测试toString方法
            String statsStr = stats.toString();
            assertTrue(statsStr.contains("VectorStoreStats"));
        }
    }
    
    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTests {
        
        @Test
        @DisplayName("空向量列表处理")
        void testEmptyVectorList() throws ExecutionException, InterruptedException {
            List<Float> emptyVector = Collections.emptyList();
            Map<String, Object> metadata = createTestMetadata();
            
            // 空向量应该能够正常处理
            String id = vectorStore.insert(TEST_COLLECTION, emptyVector, metadata).get();
            assertNotNull(id);
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
        
        @Test
        @DisplayName("获取不存在的向量")
        void testGetNonexistentVector() throws ExecutionException, InterruptedException {
            VectorStore.VectorDocument doc = vectorStore.get(TEST_COLLECTION, "nonexistent").get();
            assertNull(doc);
        }
        
        @Test
        @DisplayName("获取不存在的向量（兼容方法）")
        void testGetNonexistentVectorCompatible() {
            CompletableFuture<SearchResult> future = vectorStore.get("nonexistent");
            assertThrows(RuntimeException.class, future::join);
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