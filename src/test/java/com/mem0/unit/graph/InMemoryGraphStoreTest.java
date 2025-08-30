package com.mem0.unit.graph;

import com.mem0.core.EnhancedMemory;
import com.mem0.graph.GraphStore;
import com.mem0.graph.impl.InMemoryGraphStore;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

/**
 * InMemoryGraphStore 单元测试
 * 覆盖所有图存储操作和关系管理
 */
public class InMemoryGraphStoreTest {

    private InMemoryGraphStore graphStore;
    private final String testUserId = "test_user_123";

    @Before
    public void setUp() {
        graphStore = new InMemoryGraphStore();
    }

    @After
    public void tearDown() throws Exception {
        if (graphStore != null) {
            graphStore.close().get();
        }
    }

    @Test
    public void testAddMemory_Success() throws Exception {
        // 准备测试数据
        EnhancedMemory memory = createTestMemory("mem1", "用户喜欢咖啡", testUserId);

        // 执行添加
        CompletableFuture<Void> future = graphStore.addMemory(memory);
        future.get(); // 不应该抛出异常

        // 验证内存被添加
        CompletableFuture<EnhancedMemory> getFuture = graphStore.getMemory("mem1");
        EnhancedMemory retrieved = getFuture.get();

        assertNotNull("应该能检索到添加的内存", retrieved);
        assertEquals("ID应该匹配", "mem1", retrieved.getId());
        assertEquals("内容应该匹配", "用户喜欢咖啡", retrieved.getContent());
        assertEquals("用户ID应该匹配", testUserId, retrieved.getUserId());
    }

    @Test
    public void testAddMemory_NullMemory() throws Exception {
        // 测试添加null内存
        try {
            CompletableFuture<Void> future = graphStore.addMemory(null);
            future.get();
            fail("应该抛出异常，因为内存为null");
        } catch (ExecutionException e) {
            assertTrue("应该包含null相关错误信息",
                       e.getCause().getMessage().contains("null") ||
                       e.getCause() instanceof NullPointerException);
        }
    }

    @Test
    public void testAddMemory_DuplicateId() throws Exception {
        // 测试添加重复ID的内存
        EnhancedMemory memory1 = createTestMemory("duplicate", "第一个内存", testUserId);
        EnhancedMemory memory2 = createTestMemory("duplicate", "第二个内存", testUserId);

        // 添加第一个
        graphStore.addMemory(memory1).get();

        // 尝试添加重复ID的内存
        try {
            graphStore.addMemory(memory2).get();
            fail("应该抛出异常，因为ID重复");
        } catch (ExecutionException e) {
            assertTrue("应该包含重复ID的错误信息",
                       e.getCause().getMessage().contains("已存在") ||
                       e.getCause().getMessage().contains("duplicate"));
        }
    }

    @Test
    public void testGetMemory_Success() throws Exception {
        // 先添加内存
        EnhancedMemory memory = createTestMemory("get_test", "获取测试", testUserId);
        graphStore.addMemory(memory).get();

        // 获取内存
        CompletableFuture<EnhancedMemory> future = graphStore.getMemory("get_test");
        EnhancedMemory retrieved = future.get();

        assertNotNull("应该能获取到内存", retrieved);
        assertEquals("ID应该匹配", "get_test", retrieved.getId());
        assertEquals("内容应该匹配", "获取测试", retrieved.getContent());
    }

    @Test
    public void testGetMemory_NonexistentId() throws Exception {
        // 获取不存在的内存
        CompletableFuture<EnhancedMemory> future = graphStore.getMemory("nonexistent");
        EnhancedMemory result = future.get();

        assertNull("不存在的内存应该返回null", result);
    }

    @Test
    public void testUpdateMemory_Success() throws Exception {
        // 先添加内存
        EnhancedMemory originalMemory = createTestMemory("update_test", "原始内容", testUserId);
        graphStore.addMemory(originalMemory).get();

        // 更新内存
        EnhancedMemory updatedMemory = createTestMemory("update_test", "更新后的内容", testUserId);
        CompletableFuture<Void> future = graphStore.updateMemory(updatedMemory);
        future.get(); // 不应该抛出异常

        // 验证更新
        EnhancedMemory retrieved = graphStore.getMemory("update_test").get();
        assertNotNull("更新后应该能获取到内存", retrieved);
        assertEquals("内容应该已更新", "更新后的内容", retrieved.getContent());
    }

    @Test
    public void testUpdateMemory_NonexistentMemory() throws Exception {
        // 尝试更新不存在的内存
        EnhancedMemory memory = createTestMemory("nonexistent_update", "内容", testUserId);
        
        try {
            CompletableFuture<Void> future = graphStore.updateMemory(memory);
            future.get();
            fail("应该抛出异常，因为内存不存在");
        } catch (ExecutionException e) {
            assertTrue("应该包含内存不存在的错误信息",
                       e.getCause().getMessage().contains("不存在") ||
                       e.getCause().getMessage().contains("not found"));
        }
    }

    @Test
    public void testDeleteMemory_Success() throws Exception {
        // 先添加内存
        EnhancedMemory memory = createTestMemory("delete_test", "删除测试", testUserId);
        graphStore.addMemory(memory).get();

        // 验证内存存在
        assertNotNull("删除前内存应该存在", graphStore.getMemory("delete_test").get());

        // 删除内存
        CompletableFuture<Void> future = graphStore.deleteMemory("delete_test");
        future.get(); // 不应该抛出异常

        // 验证删除
        assertNull("删除后内存应该不存在", graphStore.getMemory("delete_test").get());
    }

    @Test
    public void testDeleteMemory_NonexistentId() throws Exception {
        // 删除不存在的内存 - 应该是幂等操作，不抛出异常
        CompletableFuture<Void> future = graphStore.deleteMemory("nonexistent_delete");
        future.get(); // 不应该抛出异常
    }

    @Test
    public void testGetUserMemories_Success() throws Exception {
        // 为同一用户添加多个内存
        EnhancedMemory memory1 = createTestMemory("user_mem1", "用户内存1", testUserId);
        EnhancedMemory memory2 = createTestMemory("user_mem2", "用户内存2", testUserId);
        EnhancedMemory memory3 = createTestMemory("other_mem", "其他用户内存", "other_user");

        graphStore.addMemory(memory1).get();
        graphStore.addMemory(memory2).get();
        graphStore.addMemory(memory3).get();

        // 获取指定用户的内存
        CompletableFuture<List<EnhancedMemory>> future = graphStore.getUserMemories(testUserId);
        List<EnhancedMemory> userMemories = future.get();

        assertNotNull("用户内存列表不应该为空", userMemories);
        assertEquals("应该有2个用户内存", 2, userMemories.size());

        // 验证所有返回的内存都属于指定用户
        for (EnhancedMemory memory : userMemories) {
            assertEquals("所有内存都应该属于指定用户", testUserId, memory.getUserId());
        }

        // 验证包含预期的内存
        assertTrue("应该包含用户内存1", 
                   userMemories.stream().anyMatch(m -> "user_mem1".equals(m.getId())));
        assertTrue("应该包含用户内存2",
                   userMemories.stream().anyMatch(m -> "user_mem2".equals(m.getId())));
    }

    @Test
    public void testGetUserMemories_NoMemories() throws Exception {
        // 获取没有内存的用户的内存列表
        CompletableFuture<List<EnhancedMemory>> future = 
            graphStore.getUserMemories("user_with_no_memories");
        List<EnhancedMemory> userMemories = future.get();

        assertNotNull("内存列表不应该为null", userMemories);
        assertTrue("没有内存的用户应该返回空列表", userMemories.isEmpty());
    }

    @Test
    public void testGetMemoryHistory_Success() throws Exception {
        // 添加多个内存（模拟历史）
        EnhancedMemory memory1 = createTestMemory("hist1", "历史1", testUserId);
        EnhancedMemory memory2 = createTestMemory("hist2", "历史2", testUserId);
        EnhancedMemory memory3 = createTestMemory("hist3", "历史3", testUserId);

        // 按时间顺序添加
        graphStore.addMemory(memory1).get();
        Thread.sleep(10); // 确保时间差
        graphStore.addMemory(memory2).get();
        Thread.sleep(10);
        graphStore.addMemory(memory3).get();

        // 获取历史记录
        CompletableFuture<List<EnhancedMemory>> future = graphStore.getMemoryHistory(testUserId);
        List<EnhancedMemory> history = future.get();

        assertNotNull("历史记录不应该为空", history);
        assertEquals("应该有3个历史记录", 3, history.size());

        // 验证时间排序（应该按时间顺序）
        LocalDateTime prevTime = null;
        for (EnhancedMemory memory : history) {
            if (prevTime != null) {
                assertTrue("历史记录应该按时间排序",
                           !memory.getCreatedAt().isBefore(prevTime));
            }
            prevTime = memory.getCreatedAt();
        }
    }

    @Test
    public void testSearchMemories_Success() throws Exception {
        // 添加测试数据
        EnhancedMemory memory1 = createTestMemory("search1", "用户喜欢喝咖啡", testUserId);
        EnhancedMemory memory2 = createTestMemory("search2", "用户喜欢喝茶", testUserId);
        EnhancedMemory memory3 = createTestMemory("search3", "用户不喜欢运动", testUserId);

        graphStore.addMemory(memory1).get();
        graphStore.addMemory(memory2).get();
        graphStore.addMemory(memory3).get();

        // 搜索包含"喜欢"的内存
        CompletableFuture<List<EnhancedMemory>> future = 
            graphStore.searchMemories("喜欢", testUserId, 10);
        List<EnhancedMemory> results = future.get();

        assertNotNull("搜索结果不应该为空", results);
        assertTrue("应该找到匹配的结果", results.size() > 0);

        // 验证所有结果都包含搜索词
        for (EnhancedMemory memory : results) {
            assertTrue("搜索结果应该包含搜索词", 
                       memory.getContent().contains("喜欢"));
        }
    }

    @Test
    public void testSearchMemories_NoResults() throws Exception {
        // 添加测试数据
        EnhancedMemory memory = createTestMemory("search_no_result", "测试内容", testUserId);
        graphStore.addMemory(memory).get();

        // 搜索不存在的内容
        CompletableFuture<List<EnhancedMemory>> future = 
            graphStore.searchMemories("不存在的内容", testUserId, 10);
        List<EnhancedMemory> results = future.get();

        assertNotNull("搜索结果不应该为null", results);
        assertTrue("搜索不存在的内容应该返回空结果", results.isEmpty());
    }

    @Test
    public void testSearchMemories_LimitResults() throws Exception {
        // 添加多个匹配的内存
        for (int i = 1; i <= 10; i++) {
            EnhancedMemory memory = createTestMemory("limit" + i, "测试内容" + i, testUserId);
            graphStore.addMemory(memory).get();
        }

        // 限制搜索结果数量
        CompletableFuture<List<EnhancedMemory>> future = 
            graphStore.searchMemories("测试", testUserId, 5);
        List<EnhancedMemory> results = future.get();

        assertNotNull("搜索结果不应该为空", results);
        assertTrue("搜索结果数量应该不超过限制", results.size() <= 5);
    }

    @Test
    public void testAddRelationship_Success() throws Exception {
        // 先添加两个内存
        EnhancedMemory memory1 = createTestMemory("rel1", "内存1", testUserId);
        EnhancedMemory memory2 = createTestMemory("rel2", "内存2", testUserId);
        
        graphStore.addMemory(memory1).get();
        graphStore.addMemory(memory2).get();

        // 添加关系
        Map<String, Object> relationshipProps = new HashMap<>();
        relationshipProps.put("type", "RELATED");
        relationshipProps.put("strength", 0.8);

        CompletableFuture<Void> future = graphStore.addRelationship(
            "rel1", "rel2", "RELATED", relationshipProps);
        future.get(); // 不应该抛出异常

        // 验证关系存在 - 这里需要实现getRelationships方法来验证
        // 暂时通过其他方式验证关系的存在
    }

    @Test
    public void testAddRelationship_NonexistentMemory() throws Exception {
        // 尝试在不存在的内存之间添加关系
        Map<String, Object> props = new HashMap<>();
        
        try {
            CompletableFuture<Void> future = graphStore.addRelationship(
                "nonexistent1", "nonexistent2", "RELATED", props);
            future.get();
            fail("应该抛出异常，因为内存不存在");
        } catch (ExecutionException e) {
            assertTrue("应该包含内存不存在的错误信息",
                       e.getCause().getMessage().contains("不存在") ||
                       e.getCause().getMessage().contains("not found"));
        }
    }

    @Test
    public void testUpdateNode_Success() throws Exception {
        // 先添加内存
        EnhancedMemory memory = createTestMemory("update_node", "节点更新测试", testUserId);
        graphStore.addMemory(memory).get();

        // 更新节点属性
        Map<String, Object> properties = new HashMap<>();
        properties.put("importance", 0.9);
        properties.put("category", "preference");
        properties.put("lastAccessed", System.currentTimeMillis());

        CompletableFuture<Void> future = graphStore.updateNode("update_node", properties);
        future.get(); // 不应该抛出异常

        // 验证更新 - 通过获取内存验证属性是否更新
        EnhancedMemory updated = graphStore.getMemory("update_node").get();
        assertNotNull("更新后内存应该存在", updated);
        
        // 注意：这里假设内存对象包含了节点属性
        // 实际实现可能需要额外的方法来获取节点属性
    }

    @Test
    public void testConcurrentOperations() throws Exception {
        // 测试并发操作的线程安全性
        int numThreads = 10;
        CompletableFuture<Void>[] futures = new CompletableFuture[numThreads];

        // 并发添加内存
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    for (int j = 0; j < 10; j++) {
                        String id = "concurrent_" + threadId + "_" + j;
                        EnhancedMemory memory = createTestMemory(id, 
                            "并发测试内容 " + threadId + " " + j, testUserId);
                        graphStore.addMemory(memory).get();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        // 等待所有操作完成
        CompletableFuture.allOf(futures).get();

        // 验证所有内存都被成功添加
        List<EnhancedMemory> userMemories = graphStore.getUserMemories(testUserId).get();
        assertEquals("应该有100个并发添加的内存", 100, userMemories.size());
    }

    @Test
    public void testMemoryLifecycle() throws Exception {
        // 测试完整的内存生命周期
        String memoryId = "lifecycle_test";
        
        // 1. 添加内存
        EnhancedMemory memory = createTestMemory(memoryId, "生命周期测试", testUserId);
        graphStore.addMemory(memory).get();
        
        // 验证添加
        assertNotNull("添加后应该存在", graphStore.getMemory(memoryId).get());
        
        // 2. 更新内存
        EnhancedMemory updatedMemory = createTestMemory(memoryId, "更新后的内容", testUserId);
        graphStore.updateMemory(updatedMemory).get();
        
        // 验证更新
        EnhancedMemory retrieved = graphStore.getMemory(memoryId).get();
        assertEquals("内容应该已更新", "更新后的内容", retrieved.getContent());
        
        // 3. 添加关系（与自己的关系，用于测试）
        Map<String, Object> relationProps = new HashMap<>();
        relationProps.put("type", "SELF_REFERENCE");
        graphStore.addRelationship(memoryId, memoryId, "SELF", relationProps).get();
        
        // 4. 更新节点属性
        Map<String, Object> nodeProps = new HashMap<>();
        nodeProps.put("processed", true);
        graphStore.updateNode(memoryId, nodeProps).get();
        
        // 5. 搜索验证
        List<EnhancedMemory> searchResults = graphStore.searchMemories("更新后", testUserId, 10).get();
        assertTrue("搜索应该能找到更新后的内容", 
                   searchResults.stream().anyMatch(m -> memoryId.equals(m.getId())));
        
        // 6. 删除内存
        graphStore.deleteMemory(memoryId).get();
        
        // 验证删除
        assertNull("删除后应该不存在", graphStore.getMemory(memoryId).get());
    }

    @Test
    public void testClose() throws Exception {
        // 添加一些数据
        EnhancedMemory memory = createTestMemory("close_test", "关闭测试", testUserId);
        graphStore.addMemory(memory).get();

        // 关闭图存储
        CompletableFuture<Void> future = graphStore.close();
        future.get(); // 不应该抛出异常

        // 关闭后的操作可能会失败或返回空结果，这取决于实现
        // 这里不做具体断言，因为关闭行为可能因实现而异
    }

    // 辅助方法

    private EnhancedMemory createTestMemory(String id, String content, String userId) {
        return new EnhancedMemory(id, content, userId, null, null);
    }

    private EnhancedMemory createTestMemoryWithMetadata(String id, String content, 
                                                       String userId, Map<String, Object> metadata) {
        return new EnhancedMemory(id, content, userId, null, metadata);
    }
}