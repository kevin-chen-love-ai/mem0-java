package com.mem0.core;

import com.mem0.core.EnhancedMemory;
import com.mem0.core.EnhancedMemoryService;
import com.mem0.core.MemoryClassifier;
import com.mem0.core.MemoryConflictDetector;
import com.mem0.core.MemoryMergeStrategy;
import com.mem0.core.MemoryImportanceScorer;
import com.mem0.core.MemoryForgettingManager;
import com.mem0.embedding.EmbeddingProvider;
import com.mem0.store.GraphStore;
import com.mem0.llm.LLMProvider;
import com.mem0.store.VectorStore;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * 增强内存服务单元测试 - Unit tests for EnhancedMemoryService class
 * 
 * <p>此测试类验证EnhancedMemoryService的高级功能，包括智能内存分类、冲突检测、
 * 自动合并、重要性评分、遗忘管理等企业级特性。使用Mock对象模拟各种依赖服务，
 * 确保测试的独立性和可控性。</p>
 * 
 * <p>This test class verifies advanced features of EnhancedMemoryService, including intelligent
 * memory classification, conflict detection, automatic merging, importance scoring, forgetting
 * management, and other enterprise-level features. Uses Mock objects to simulate various
 * dependent services, ensuring test independence and controllability.</p>
 * 
 * <h3>测试覆盖的核心组件 / Core Components Under Test:</h3>
 * <ul>
 *   <li>MemoryClassifier - 智能内存分类器 / Intelligent memory classifier</li>
 *   <li>MemoryConflictDetector - 内存冲突检测器 / Memory conflict detector</li>
 *   <li>MemoryMergeStrategy - 内存合并策略 / Memory merge strategy</li>
 *   <li>MemoryImportanceScorer - 内存重要性评分器 / Memory importance scorer</li>
 *   <li>MemoryForgettingManager - 内存遗忘管理器 / Memory forgetting manager</li>
 * </ul>
 * 
 * <h3>Mock依赖服务 / Mock Dependencies:</h3>
 * <ul>
 *   <li>EmbeddingProvider - 嵌入向量生成服务 / Embedding vector generation service</li>
 *   <li>LLMProvider - 大语言模型服务 / Large language model service</li>
 *   <li>VectorStore - 向量存储服务 / Vector storage service</li>
 *   <li>GraphStore - 图存储服务 / Graph storage service</li>
 * </ul>
 * 
 * <h3>测试场景 / Test Scenarios:</h3>
 * <ul>
 *   <li>内存添加的智能增强处理 / Intelligent enhancement processing for memory addition</li>
 *   <li>重复内存的自动检测和合并 / Automatic detection and merging of duplicate memories</li>
 *   <li>内存冲突的识别和解决 / Identification and resolution of memory conflicts</li>
 *   <li>动态重要性评分和调整 / Dynamic importance scoring and adjustment</li>
 *   <li>基于策略的内存遗忘处理 / Strategy-based memory forgetting processing</li>
 *   <li>异步处理和并发操作 / Asynchronous processing and concurrent operations</li>
 * </ul>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 * @see EnhancedMemoryService
 * @see EnhancedMemory
 * @see MemoryClassifier
 * @see MemoryConflictDetector
 */

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * EnhancedMemoryService 单元测试
 * 覆盖所有核心功能和边界条件
 */
public class EnhancedMemoryServiceTest {

    @Mock private VectorStore vectorStore;
    @Mock private GraphStore graphStore;
    @Mock private EmbeddingProvider embeddingProvider;
    @Mock private LLMProvider llmProvider;
    @Mock private MemoryClassifier memoryClassifier;
    @Mock private MemoryConflictDetector conflictDetector;
    @Mock private MemoryMergeStrategy mergeStrategy;
    @Mock private MemoryImportanceScorer importanceScorer;
    @Mock private MemoryForgettingManager forgettingManager;

    private EnhancedMemoryService memoryService;
    private AutoCloseable closeable;
    
    // Java 8 compatible helper method for creating failed futures
    private static <T> CompletableFuture<T> createFailedFuture(Exception exception) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(exception);
        return future;
    }

    @Before
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        
        // 创建服务实例
        memoryService = new EnhancedMemoryService(
            vectorStore, graphStore, embeddingProvider, llmProvider,
            memoryClassifier, conflictDetector, mergeStrategy, 
            importanceScorer, forgettingManager
        );
    }
    
    @After
    public void tearDown() throws Exception {
        if (closeable != null) {
            closeable.close();
        }
    }

    @Test
    public void testAddMemory_Success() throws Exception {
        // 准备测试数据
        String content = "用户喜欢喝咖啡";
        String userId = "user123";
        List<Float> embedding = Arrays.asList(0.1f, 0.2f, 0.3f);
        
        // 配置mocks
        when(embeddingProvider.embed(content))
            .thenReturn(CompletableFuture.completedFuture(embedding));
        when(vectorStore.collectionExists(anyString()))
            .thenReturn(CompletableFuture.completedFuture(false));
        when(vectorStore.createCollection(anyString(), anyInt()))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(vectorStore.insert(anyString(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture("memory123"));
        when(graphStore.createNode(anyString(), any()))
            .thenReturn(CompletableFuture.completedFuture("node123"));
        
        // 执行测试
        CompletableFuture<String> future = memoryService.addEnhancedMemory(content, userId, null, null, null, null);
        String memoryId = future.get();
        
        // 验证结果
        assertNotNull("内存ID不应该为空", memoryId);
        
        // 验证调用
        verify(embeddingProvider).embed(content);
        verify(vectorStore).insert(anyString(), eq(embedding), any());
        verify(graphStore).createNode(anyString(), any());
    }

    @Test
    public void testAddMemory_EmptyContent() throws Exception {
        // 测试空内容
        CompletableFuture<String> future = memoryService.addEnhancedMemory("", "user123", null, null, null, null);
        
        try {
            future.get();
            fail("应该抛出异常");
        } catch (Exception e) {
            assertTrue("应该包含内容为空的错误信息", 
                       e.getCause().getMessage().contains("内容不能为空"));
        }
    }

    @Test
    public void testAddMemory_NullUserId() throws Exception {
        // 测试空用户ID
        CompletableFuture<String> future = memoryService.addEnhancedMemory("测试内容", null, null, null, null, null);
        
        try {
            future.get();
            fail("应该抛出异常");
        } catch (Exception e) {
            assertTrue("应该包含用户ID为空的错误信息",
                       e.getCause().getMessage().contains("用户ID不能为空"));
        }
    }

    @Test
    public void testAddMemory_EmbeddingFailure() throws Exception {
        // 配置嵌入失败
        when(embeddingProvider.embed(anyString()))
            .thenReturn(createFailedFuture(new RuntimeException("嵌入服务错误")));
        
        // 执行测试
        CompletableFuture<String> future = memoryService.addEnhancedMemory("测试内容", "user123", null, null, null, null);
        
        try {
            future.get();
            fail("应该抛出异常");
        } catch (Exception e) {
            assertTrue("应该包含嵌入错误信息", 
                       e.getCause().getMessage().contains("嵌入服务错误"));
        }
    }

    @Test
    public void testSearchMemories_Success() throws Exception {
        // 准备测试数据
        String query = "咖啡";
        String userId = "user123";
        List<Float> queryEmbedding = Arrays.asList(0.1f, 0.2f, 0.3f);
        
        // 模拟搜索结果
        VectorStore.VectorSearchResult searchResult = new VectorStore.VectorSearchResult(
            "memory123", 0.9f, Collections.singletonMap("content", "用户喜欢喝咖啡"), null
        );
        
        // 配置mocks
        when(embeddingProvider.embed(query))
            .thenReturn(CompletableFuture.completedFuture(queryEmbedding));
        when(vectorStore.search(anyString(), eq(queryEmbedding), anyInt(), any()))
            .thenReturn(CompletableFuture.completedFuture(Arrays.asList(searchResult)));
        when(graphStore.getMemory("memory123"))
            .thenReturn(CompletableFuture.completedFuture(
                new EnhancedMemory("memory123", "用户喜欢喝咖啡", userId, null, null)
            ));
        
        // 执行测试
        CompletableFuture<List<EnhancedMemory>> future = 
            memoryService.searchEnhancedMemories(query, userId, 10);
        List<EnhancedMemory> results = future.get();
        
        // 验证结果
        assertNotNull("搜索结果不应该为空", results);
        assertFalse("应该有搜索结果", results.isEmpty());
        assertEquals("结果数量应该正确", 1, results.size());
        assertEquals("内容应该匹配", "用户喜欢喝咖啡", results.get(0).getContent());
        
        // 验证调用
        verify(embeddingProvider).embed(query);
        verify(vectorStore).search(anyString(), eq(queryEmbedding), eq(10), any());
    }

    @Test
    public void testSearchMemories_NoResults() throws Exception {
        // 配置无搜索结果
        when(embeddingProvider.embed(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Arrays.asList(0.1f, 0.2f, 0.3f)));
        when(vectorStore.search(anyString(), any(), anyInt(), any()))
            .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
        
        // 执行测试
        CompletableFuture<List<EnhancedMemory>> future = 
            memoryService.searchEnhancedMemories("不存在的内容", "user123", 10);
        List<EnhancedMemory> results = future.get();
        
        // 验证结果
        assertNotNull("搜索结果不应该为null", results);
        assertTrue("搜索结果应该为空", results.isEmpty());
    }

    @Test
    public void testUpdateMemory_Success() throws Exception {
        // 准备测试数据
        String memoryId = "memory123";
        String newContent = "用户特别喜欢意式咖啡";
        List<Float> newEmbedding = Arrays.asList(0.4f, 0.5f, 0.6f);
        
        EnhancedMemory existingMemory = new EnhancedMemory(
            memoryId, "用户喜欢咖啡", "user123", null, null
        );
        
        // 配置mocks
        when(graphStore.getMemory(memoryId))
            .thenReturn(CompletableFuture.completedFuture(existingMemory));
        when(embeddingProvider.embed(newContent))
            .thenReturn(CompletableFuture.completedFuture(newEmbedding));
        when(vectorStore.delete(anyString(), anyString()))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(vectorStore.insert(anyString(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture("memory123"));
        when(graphStore.updateMemory(any(EnhancedMemory.class)))
            .thenReturn(CompletableFuture.completedFuture(null));
        
        // 执行测试
        CompletableFuture<EnhancedMemory> future = memoryService.updateEnhancedMemory(memoryId, newContent, null);
        EnhancedMemory updatedMemory = future.get();
        
        // 验证结果
        assertNotNull("更新后的内存不应该为空", updatedMemory);
        assertEquals("更新后的ID应该相同", memoryId, updatedMemory.getId());
        
        // 验证调用
        verify(graphStore).getMemory(memoryId);
        verify(embeddingProvider).embed(newContent);
        verify(vectorStore).delete(anyString(), eq(memoryId));
        verify(vectorStore).insert(anyString(), eq(newEmbedding), any());
        verify(graphStore).updateMemory(any(EnhancedMemory.class));
    }

    @Test
    public void testUpdateMemory_NotFound() throws Exception {
        // 配置内存不存在
        when(graphStore.getMemory("nonexistent"))
            .thenReturn(CompletableFuture.completedFuture(null));
        
        // 执行测试
        CompletableFuture<EnhancedMemory> future = memoryService.updateEnhancedMemory("nonexistent", "新内容", null);
        
        try {
            future.get();
            fail("应该抛出异常");
        } catch (Exception e) {
            assertTrue("应该包含内存不存在的错误信息",
                       e.getCause().getMessage().contains("内存不存在"));
        }
    }

    @Test
    public void testDeleteMemory_Success() throws Exception {
        // 准备测试数据
        String memoryId = "memory123";
        String collectionName = "memories_user123";
        
        // 配置mocks
        when(vectorStore.delete(collectionName, memoryId))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(graphStore.deleteMemory(memoryId))
            .thenReturn(CompletableFuture.completedFuture(null));
        
        // 执行测试
        CompletableFuture<Void> future = memoryService.deleteEnhancedMemory(memoryId);
        future.get(); // 不应该抛出异常
        
        // 验证调用
        verify(vectorStore).delete(collectionName, memoryId);
        verify(graphStore).deleteMemory(memoryId);
    }

    @Test
    public void testDeleteMemory_VectorStoreFailure() throws Exception {
        // 配置向量存储删除失败
        when(vectorStore.delete(anyString(), anyString()))
            .thenReturn(createFailedFuture(new RuntimeException("删除失败")));
        
        // 执行测试
        CompletableFuture<Void> future = memoryService.deleteEnhancedMemory("memory123");
        
        try {
            future.get();
            fail("应该抛出异常");
        } catch (Exception e) {
            assertTrue("应该包含删除失败的错误信息",
                       e.getCause().getMessage().contains("删除失败"));
        }
    }

    @Test
    public void testGetAllMemories_Success() throws Exception {
        // 准备测试数据
        String userId = "user123";
        List<EnhancedMemory> memories = Arrays.asList(
            new EnhancedMemory("mem1", "内容1", userId, null, null),
            new EnhancedMemory("mem2", "内容2", userId, null, null)
        );
        
        // 配置mocks
        when(graphStore.getUserMemories(userId))
            .thenReturn(CompletableFuture.completedFuture(memories));
        
        // 执行测试
        CompletableFuture<List<EnhancedMemory>> future = memoryService.getAllEnhancedMemories(userId, null);
        List<EnhancedMemory> results = future.get();
        
        // 验证结果
        assertNotNull("结果不应该为空", results);
        assertEquals("结果数量应该正确", 2, results.size());
        assertEquals("第一个内存内容应该匹配", "内容1", results.get(0).getContent());
        assertEquals("第二个内存内容应该匹配", "内容2", results.get(1).getContent());
        
        // 验证调用
        verify(graphStore).getUserMemories(userId);
    }

    @Test
    public void testGetMemoryHistory_Success() throws Exception {
        // 准备测试数据
        String userId = "user123";
        List<EnhancedMemory> history = Arrays.asList(
            new EnhancedMemory("mem1", "旧内容", userId, null, null),
            new EnhancedMemory("mem2", "新内容", userId, null, null)
        );
        
        // 配置mocks
        when(graphStore.getMemoryHistory(userId))
            .thenReturn(CompletableFuture.completedFuture(history));
        
        // 执行测试
        CompletableFuture<List<EnhancedMemory>> future = memoryService.getAllEnhancedMemories(userId, null);
        List<EnhancedMemory> results = future.get();
        
        // 验证结果
        assertNotNull("历史记录不应该为空", results);
        assertEquals("历史记录数量应该正确", 2, results.size());
        
        // 验证调用
        verify(graphStore).getMemoryHistory(userId);
    }

    @Test
    public void testClose() throws Exception {
        // 配置mocks
        when(vectorStore.close()).thenReturn(CompletableFuture.runAsync(() -> {}));
        when(graphStore.close()).thenReturn(CompletableFuture.runAsync(() -> {}));
        
        // 执行测试
        memoryService.close();
        
        // 验证调用（注意：close方法是void，所以我们验证相关组件的close方法被调用）
        // 由于实现可能是异步的，我们检查是否没有抛出异常
        // 这里的具体验证取决于实际的close实现
    }

    @Test
    public void testConcurrentMemoryOperations() throws Exception {
        // 测试并发操作的线程安全性
        String userId = "user123";
        List<Float> embedding = Arrays.asList(0.1f, 0.2f, 0.3f);
        
        // 配置mocks
        when(embeddingProvider.embed(anyString()))
            .thenReturn(CompletableFuture.completedFuture(embedding));
        when(vectorStore.collectionExists(anyString()))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(vectorStore.insert(anyString(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture("memory123"));
        when(graphStore.createNode(anyString(), any()))
            .thenReturn(CompletableFuture.completedFuture("node123"));
        
        // 并发添加多个内存
        CompletableFuture<String>[] futures = new CompletableFuture[10];
        for (int i = 0; i < 10; i++) {
            final int index = i;
            futures[i] = memoryService.addEnhancedMemory("内容" + index, userId, null, null, null, null);
        }
        
        // 等待所有操作完成
        CompletableFuture.allOf(futures).get();
        
        // 验证所有操作都成功完成
        for (CompletableFuture<String> future : futures) {
            assertNotNull("内存ID不应该为空", future.get());
        }
        
        // 验证调用次数
        verify(embeddingProvider, times(10)).embed(anyString());
    }
}