package com.mem0.unit.core;

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
import com.mem0.util.TestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EnhancedMemoryService 简化集成测试
 * 使用TestConfiguration统一配置真实Provider进行测试
 */
public class EnhancedMemoryServiceSimpleTest {

    private VectorStore vectorStore;
    private GraphStore graphStore;
    private EmbeddingProvider embeddingProvider;
    private LLMProvider llmProvider;
    private MemoryClassifier memoryClassifier;
    private MemoryConflictDetector conflictDetector;
    private MemoryMergeStrategy mergeStrategy;
    private MemoryImportanceScorer importanceScorer;
    private MemoryForgettingManager forgettingManager;

    private EnhancedMemoryService memoryService;

    @BeforeEach
    void setUp() throws Exception {
        // 使用TestConfiguration获取统一配置的组件
        vectorStore = TestConfiguration.getVectorStore();
        graphStore = TestConfiguration.getGraphStore();
        embeddingProvider = TestConfiguration.getEmbeddingProvider();
        llmProvider = TestConfiguration.getLLMProvider();
        
        // 创建核心组件
        memoryClassifier = TestConfiguration.createMemoryClassifier();
        conflictDetector = TestConfiguration.createConflictDetector();
        mergeStrategy = TestConfiguration.createMergeStrategy();
        importanceScorer = TestConfiguration.createImportanceScorer();
        forgettingManager = new MemoryForgettingManager();
        
        // 如果关键组件不可用，跳过测试
        if (vectorStore == null || graphStore == null || embeddingProvider == null || llmProvider == null) {
            System.out.println("Warning: Core providers not available, some tests may be skipped");
            return;
        }
        
        // 创建服务实例
        if (memoryClassifier != null && conflictDetector != null && mergeStrategy != null && importanceScorer != null) {
            memoryService = new EnhancedMemoryService(
                vectorStore, graphStore, embeddingProvider, llmProvider,
                memoryClassifier, conflictDetector, mergeStrategy, 
                importanceScorer, forgettingManager
            );
        }
    }
    
    @AfterEach
    void tearDown() throws Exception {
        // TestConfiguration会管理Provider的生命周期
    }

    @Test
    void testAddMemorySuccess() throws Exception {
        if (TestConfiguration.shouldSkipTest("testAddMemorySuccess", true, true)) {
            return;
        }
        
        if (memoryService == null) {
            System.out.println("Skipping testAddMemorySuccess - memory service not initialized");
            return;
        }
        
        // 准备测试数据
        String content = "用户喜欢喝咖啡";
        String userId = "user123";
        
        // 执行测试
        CompletableFuture<String> future = memoryService.addEnhancedMemory(content, userId, null, null, null, null);
        String memoryId = future.get();
        
        // 验证结果
        assertNotNull(memoryId, "内存ID不应该为空");
        assertTrue(memoryId.length() > 0, "内存ID长度应该大于0");
        System.out.println("Successfully added memory with ID: " + memoryId);
    }

    @Test
    void testAddMemoryWithMetadata() throws Exception {
        if (TestConfiguration.shouldSkipTest("testAddMemoryWithMetadata", true, true)) {
            return;
        }
        
        if (memoryService == null) {
            System.out.println("Skipping testAddMemoryWithMetadata - memory service not initialized");
            return;
        }
        
        // 准备测试数据
        String content = "用户完成了Java认证考试";
        String userId = "user456";
        java.util.Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("category", "achievement");
        metadata.put("date", "2024-01-15");
        
        // 执行测试
        CompletableFuture<String> future = memoryService.addEnhancedMemory(
            content, userId, null, null, null, metadata);
        String memoryId = future.get();
        
        // 验证结果
        assertNotNull(memoryId, "内存ID不应该为空");
        assertTrue(memoryId.length() > 0, "内存ID长度应该大于0");
        System.out.println("Successfully added memory with metadata, ID: " + memoryId);
    }

    @Test
    void testSearchMemories() throws Exception {
        if (TestConfiguration.shouldSkipTest("testSearchMemories", true, true)) {
            return;
        }
        
        if (memoryService == null) {
            System.out.println("Skipping testSearchMemories - memory service not initialized");
            return;
        }
        
        // 准备测试数据
        String userId = "search-user";
        String content1 = "用户擅长Java编程";
        String content2 = "用户有Spring框架经验";
        
        // 先添加一些内存
        CompletableFuture<String> future1 = memoryService.addEnhancedMemory(content1, userId, null, null, null, null);
        CompletableFuture<String> future2 = memoryService.addEnhancedMemory(content2, userId, null, null, null, null);
        
        String memoryId1 = future1.get();
        String memoryId2 = future2.get();
        
        assertNotNull(memoryId1);
        assertNotNull(memoryId2);
        
        // 等待索引更新
        Thread.sleep(1000);
        
        // 搜索内存
        CompletableFuture<List<EnhancedMemory>> searchFuture = 
            memoryService.searchEnhancedMemories("编程", userId, 10);
        List<EnhancedMemory> results = searchFuture.get();
        
        // 验证结果
        assertNotNull(results, "搜索结果不应该为空");
        System.out.println("Found " + results.size() + " memories related to programming");
    }

    @Test
    void testServiceComponentsAvailability() {
        // 测试组件可用性
        System.out.println("Component availability:");
        System.out.println("  VectorStore: " + (vectorStore != null));
        System.out.println("  GraphStore: " + (graphStore != null));
        System.out.println("  EmbeddingProvider: " + (embeddingProvider != null));
        System.out.println("  LLMProvider: " + (llmProvider != null));
        System.out.println("  MemoryClassifier: " + (memoryClassifier != null));
        System.out.println("  ConflictDetector: " + (conflictDetector != null));
        System.out.println("  MergeStrategy: " + (mergeStrategy != null));
        System.out.println("  ImportanceScorer: " + (importanceScorer != null));
        System.out.println("  MemoryService: " + (memoryService != null));

        // 验证基本组件可用性
        if (TestConfiguration.areAllProvidersAvailable()) {
            assertNotNull(memoryService, "当所有Provider可用时，MemoryService应该被初始化");
        }
    }
}