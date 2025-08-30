package com.mem0.unit.vector;

import com.mem0.store.VectorStore;
import com.mem0.vector.impl.InMemoryVectorStore;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

/**
 * InMemoryVectorStore 单元测试
 * 覆盖所有向量存储操作和边界条件
 */
public class InMemoryVectorStoreTest {

    private InMemoryVectorStore vectorStore;
    private final String testCollection = "test_collection";
    private final int testDimension = 128;

    @Before
    public void setUp() {
        vectorStore = new InMemoryVectorStore();
    }

    @After
    public void tearDown() throws Exception {
        if (vectorStore != null) {
            vectorStore.close().get();
        }
    }

    @Test
    public void testCreateCollection_Success() throws Exception {
        // 执行测试
        CompletableFuture<Void> future = vectorStore.createCollection(testCollection, testDimension);
        future.get(); // 不应该抛出异常

        // 验证集合存在
        CompletableFuture<Boolean> existsFuture = vectorStore.collectionExists(testCollection);
        assertTrue("集合应该存在", existsFuture.get());
    }

    @Test
    public void testCreateCollection_DuplicateName() throws Exception {
        // 先创建一个集合
        vectorStore.createCollection(testCollection, testDimension).get();

        // 尝试创建同名集合
        try {
            vectorStore.createCollection(testCollection, testDimension).get();
            fail("应该抛出异常，因为集合已存在");
        } catch (ExecutionException e) {
            assertTrue("应该包含集合已存在的错误信息", 
                       e.getCause().getMessage().contains("已存在"));
        }
    }

    @Test
    public void testCreateCollection_InvalidDimension() throws Exception {
        // 测试无效维度
        try {
            vectorStore.createCollection(testCollection, -1).get();
            fail("应该抛出异常，因为维度无效");
        } catch (ExecutionException e) {
            assertTrue("应该包含维度无效的错误信息",
                       e.getCause().getMessage().contains("维度"));
        }
    }

    @Test
    public void testCollectionExists_True() throws Exception {
        // 创建集合
        vectorStore.createCollection(testCollection, testDimension).get();

        // 检查存在性
        CompletableFuture<Boolean> future = vectorStore.collectionExists(testCollection);
        assertTrue("集合应该存在", future.get());
    }

    @Test
    public void testCollectionExists_False() throws Exception {
        // 检查不存在的集合
        CompletableFuture<Boolean> future = vectorStore.collectionExists("nonexistent");
        assertFalse("集合不应该存在", future.get());
    }

    @Test
    public void testInsert_Success() throws Exception {
        // 准备测试数据
        vectorStore.createCollection(testCollection, testDimension).get();
        List<Float> vector = createTestVector(testDimension);
        Map<String, Object> metadata = createTestMetadata();

        // 执行插入
        CompletableFuture<String> future = vectorStore.insert(testCollection, vector, metadata);
        String id = future.get();

        // 验证结果
        assertNotNull("插入后应该返回ID", id);
        assertFalse("ID不应该为空", id.isEmpty());
    }

    @Test
    public void testInsert_NonexistentCollection() throws Exception {
        // 尝试向不存在的集合插入
        List<Float> vector = createTestVector(testDimension);
        Map<String, Object> metadata = createTestMetadata();

        try {
            vectorStore.insert("nonexistent", vector, metadata).get();
            fail("应该抛出异常，因为集合不存在");
        } catch (ExecutionException e) {
            assertTrue("应该包含集合不存在的错误信息",
                       e.getCause().getMessage().contains("不存在"));
        }
    }

    @Test
    public void testInsert_WrongDimension() throws Exception {
        // 创建集合
        vectorStore.createCollection(testCollection, testDimension).get();
        
        // 使用错误维度的向量
        List<Float> wrongVector = createTestVector(testDimension + 10);
        Map<String, Object> metadata = createTestMetadata();

        try {
            vectorStore.insert(testCollection, wrongVector, metadata).get();
            fail("应该抛出异常，因为向量维度不匹配");
        } catch (ExecutionException e) {
            assertTrue("应该包含维度不匹配的错误信息",
                       e.getCause().getMessage().contains("维度"));
        }
    }

    @Test
    public void testBatchInsert_Success() throws Exception {
        // 准备测试数据
        vectorStore.createCollection(testCollection, testDimension).get();
        
        List<List<Float>> vectors = Arrays.asList(
            createTestVector(testDimension),
            createTestVector(testDimension),
            createTestVector(testDimension)
        );
        
        List<Map<String, Object>> metadataList = Arrays.asList(
            createTestMetadata("item1"),
            createTestMetadata("item2"),
            createTestMetadata("item3")
        );

        // 执行批量插入
        CompletableFuture<List<String>> future = vectorStore.batchInsert(testCollection, vectors, metadataList);
        List<String> ids = future.get();

        // 验证结果
        assertNotNull("批量插入应该返回ID列表", ids);
        assertEquals("ID数量应该匹配", vectors.size(), ids.size());
        
        for (String id : ids) {
            assertNotNull("每个ID都不应该为空", id);
            assertFalse("每个ID都不应该为空字符串", id.isEmpty());
        }
    }

    @Test
    public void testBatchInsert_MismatchedSizes() throws Exception {
        // 创建集合
        vectorStore.createCollection(testCollection, testDimension).get();
        
        // 准备不匹配大小的数据
        List<List<Float>> vectors = Arrays.asList(createTestVector(testDimension));
        List<Map<String, Object>> metadataList = Arrays.asList(
            createTestMetadata("item1"),
            createTestMetadata("item2") // 多一个元数据
        );

        try {
            vectorStore.batchInsert(testCollection, vectors, metadataList).get();
            fail("应该抛出异常，因为向量和元数据数量不匹配");
        } catch (ExecutionException e) {
            assertTrue("应该包含数量不匹配的错误信息",
                       e.getCause().getMessage().contains("数量"));
        }
    }

    @Test
    public void testSearch_Success() throws Exception {
        // 准备测试数据
        vectorStore.createCollection(testCollection, testDimension).get();
        
        // 插入测试向量
        List<Float> vector1 = createTestVector(testDimension);
        List<Float> vector2 = createSimilarVector(vector1);
        
        vectorStore.insert(testCollection, vector1, createTestMetadata("item1")).get();
        vectorStore.insert(testCollection, vector2, createTestMetadata("item2")).get();

        // 执行搜索
        CompletableFuture<List<VectorStore.VectorSearchResult>> future = 
            vectorStore.search(testCollection, vector1, 5, null);
        List<VectorStore.VectorSearchResult> results = future.get();

        // 验证结果
        assertNotNull("搜索结果不应该为空", results);
        assertFalse("应该有搜索结果", results.isEmpty());
        assertTrue("结果数量应该合理", results.size() <= 5);
        
        // 验证第一个结果应该是完全匹配的
        VectorStore.VectorSearchResult firstResult = results.get(0);
        assertTrue("第一个结果相似度应该很高", firstResult.getScore() > 0.99f);
    }

    @Test
    public void testSearch_WithFilter() throws Exception {
        // 准备测试数据
        vectorStore.createCollection(testCollection, testDimension).get();
        
        // 插入带不同标签的向量
        Map<String, Object> metadata1 = createTestMetadata("item1");
        metadata1.put("category", "A");
        Map<String, Object> metadata2 = createTestMetadata("item2");
        metadata2.put("category", "B");
        
        vectorStore.insert(testCollection, createTestVector(testDimension), metadata1).get();
        vectorStore.insert(testCollection, createTestVector(testDimension), metadata2).get();

        // 使用过滤器搜索
        Map<String, Object> filter = Collections.singletonMap("category", "A");
        CompletableFuture<List<VectorStore.VectorSearchResult>> future = 
            vectorStore.search(testCollection, createTestVector(testDimension), 5, filter);
        List<VectorStore.VectorSearchResult> results = future.get();

        // 验证结果
        assertNotNull("搜索结果不应该为空", results);
        
        for (VectorStore.VectorSearchResult result : results) {
            assertEquals("所有结果都应该匹配过滤器", "A", 
                        result.getMetadata().get("category"));
        }
    }

    @Test
    public void testSearch_EmptyResults() throws Exception {
        // 创建空集合
        vectorStore.createCollection(testCollection, testDimension).get();

        // 在空集合中搜索
        CompletableFuture<List<VectorStore.VectorSearchResult>> future = 
            vectorStore.search(testCollection, createTestVector(testDimension), 5, null);
        List<VectorStore.VectorSearchResult> results = future.get();

        // 验证结果
        assertNotNull("搜索结果不应该为null", results);
        assertTrue("空集合搜索应该返回空结果", results.isEmpty());
    }

    @Test
    public void testDelete_Success() throws Exception {
        // 准备测试数据
        vectorStore.createCollection(testCollection, testDimension).get();
        String id = vectorStore.insert(testCollection, createTestVector(testDimension), 
                                      createTestMetadata()).get();

        // 执行删除
        CompletableFuture<Void> future = vectorStore.delete(testCollection, id);
        future.get(); // 不应该抛出异常

        // 验证删除效果 - 尝试获取已删除的项目
        CompletableFuture<VectorStore.VectorDocument> getFuture = vectorStore.get(testCollection, id);
        VectorStore.VectorDocument result = getFuture.get();
        assertNull("已删除的项目应该返回null", result);
    }

    @Test
    public void testDelete_NonexistentId() throws Exception {
        // 创建集合
        vectorStore.createCollection(testCollection, testDimension).get();

        // 尝试删除不存在的ID - 应该不抛出异常（幂等操作）
        CompletableFuture<Void> future = vectorStore.delete(testCollection, "nonexistent");
        future.get(); // 不应该抛出异常
    }

    @Test
    public void testDeleteByFilter_Success() throws Exception {
        // 准备测试数据
        vectorStore.createCollection(testCollection, testDimension).get();
        
        Map<String, Object> metadata1 = createTestMetadata("item1");
        metadata1.put("status", "active");
        Map<String, Object> metadata2 = createTestMetadata("item2");
        metadata2.put("status", "inactive");
        
        vectorStore.insert(testCollection, createTestVector(testDimension), metadata1).get();
        vectorStore.insert(testCollection, createTestVector(testDimension), metadata2).get();

        // 按过滤器删除
        Map<String, Object> filter = Collections.singletonMap("status", "inactive");
        CompletableFuture<Void> future = vectorStore.deleteByFilter(testCollection, filter);
        future.get(); // 不应该抛出异常

        // 验证删除效果 - 搜索剩余项目
        CompletableFuture<List<VectorStore.VectorSearchResult>> searchFuture = 
            vectorStore.search(testCollection, createTestVector(testDimension), 10, null);
        List<VectorStore.VectorSearchResult> results = searchFuture.get();

        // 验证只剩下active状态的项目
        for (VectorStore.VectorSearchResult result : results) {
            assertEquals("剩余项目应该都是active状态", "active", 
                        result.getMetadata().get("status"));
        }
    }

    @Test
    public void testGet_Success() throws Exception {
        // 准备测试数据
        vectorStore.createCollection(testCollection, testDimension).get();
        List<Float> originalVector = createTestVector(testDimension);
        Map<String, Object> originalMetadata = createTestMetadata();
        
        String id = vectorStore.insert(testCollection, originalVector, originalMetadata).get();

        // 执行获取
        CompletableFuture<VectorStore.VectorDocument> future = vectorStore.get(testCollection, id);
        VectorStore.VectorDocument document = future.get();

        // 验证结果
        assertNotNull("应该能获取到文档", document);
        assertEquals("ID应该匹配", id, document.getId());
        assertNotNull("向量不应该为空", document.getVector());
        assertEquals("向量维度应该匹配", testDimension, document.getVector().size());
        assertNotNull("元数据不应该为空", document.getMetadata());
    }

    @Test
    public void testGet_NonexistentId() throws Exception {
        // 创建集合
        vectorStore.createCollection(testCollection, testDimension).get();

        // 尝试获取不存在的文档
        CompletableFuture<VectorStore.VectorDocument> future = 
            vectorStore.get(testCollection, "nonexistent");
        VectorStore.VectorDocument result = future.get();

        // 验证结果
        assertNull("不存在的文档应该返回null", result);
    }

    @Test
    public void testDropCollection_Success() throws Exception {
        // 创建集合
        vectorStore.createCollection(testCollection, testDimension).get();
        
        // 验证集合存在
        assertTrue("集合应该存在", vectorStore.collectionExists(testCollection).get());

        // 删除集合
        CompletableFuture<Void> future = vectorStore.dropCollection(testCollection);
        future.get(); // 不应该抛出异常

        // 验证集合已删除
        assertFalse("集合应该已删除", vectorStore.collectionExists(testCollection).get());
    }

    @Test
    public void testDropCollection_NonexistentCollection() throws Exception {
        // 尝试删除不存在的集合 - 应该不抛出异常（幂等操作）
        CompletableFuture<Void> future = vectorStore.dropCollection("nonexistent");
        future.get(); // 不应该抛出异常
    }

    @Test
    public void testConcurrentOperations() throws Exception {
        // 测试并发操作的线程安全性
        vectorStore.createCollection(testCollection, testDimension).get();

        // 并发插入
        CompletableFuture<String>[] insertFutures = new CompletableFuture[10];
        for (int i = 0; i < 10; i++) {
            final int index = i;
            insertFutures[i] = vectorStore.insert(testCollection, 
                createTestVector(testDimension), createTestMetadata("item" + index));
        }

        // 等待所有插入完成
        CompletableFuture.allOf(insertFutures).get();

        // 验证所有插入都成功
        Set<String> ids = new HashSet<>();
        for (CompletableFuture<String> future : insertFutures) {
            String id = future.get();
            assertNotNull("ID不应该为空", id);
            assertTrue("ID应该是唯一的", ids.add(id));
        }

        // 并发搜索
        CompletableFuture<List<VectorStore.VectorSearchResult>>[] searchFutures = 
            new CompletableFuture[5];
        for (int i = 0; i < 5; i++) {
            searchFutures[i] = vectorStore.search(testCollection, 
                createTestVector(testDimension), 5, null);
        }

        // 等待所有搜索完成
        CompletableFuture.allOf(searchFutures).get();

        // 验证所有搜索都成功
        for (CompletableFuture<List<VectorStore.VectorSearchResult>> future : searchFutures) {
            List<VectorStore.VectorSearchResult> results = future.get();
            assertNotNull("搜索结果不应该为空", results);
        }
    }

    // 辅助方法

    private List<Float> createTestVector(int dimension) {
        List<Float> vector = new ArrayList<>(dimension);
        Random random = new Random(42); // 固定种子确保测试可重现
        for (int i = 0; i < dimension; i++) {
            vector.add(random.nextFloat());
        }
        return vector;
    }

    private List<Float> createSimilarVector(List<Float> original) {
        List<Float> similar = new ArrayList<>(original.size());
        for (Float value : original) {
            // 添加小的噪音但保持相似性
            similar.add(value + (float) (Math.random() * 0.01 - 0.005));
        }
        return similar;
    }

    private Map<String, Object> createTestMetadata() {
        return createTestMetadata("default");
    }

    private Map<String, Object> createTestMetadata(String name) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", name);
        metadata.put("timestamp", System.currentTimeMillis());
        metadata.put("source", "test");
        return metadata;
    }
}