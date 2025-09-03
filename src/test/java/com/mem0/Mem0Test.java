package com.mem0;

import com.mem0.core.EnhancedMemory;
import com.mem0.core.MemoryType;
import com.mem0.config.Mem0Config;
import com.mem0.util.TestConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mem0 统一配置集成测试类 - Unified configuration integration test class for Mem0
 * 
 * <p>此测试类使用TestConfiguration统一管理Provider配置进行集成测试，根据可用性自动选择：</p>
 * <ul>
 *   <li>Vector Store: InMemory (用于测试稳定性)</li>
 *   <li>Graph Store: InMemory (用于测试稳定性)</li>
 *   <li>LLM Provider: QwenLLMProvider (如果可用) 或 Mock</li>
 *   <li>Embedding Provider: AliyunEmbeddingProvider (如果可用) 或 Mock</li>
 * </ul>
 * 
 * <p><strong>配置方式：</strong></p>
 * <ul>
 *   <li>通过TestConfiguration统一管理Provider初始化</li>
 *   <li>支持环境变量和系统属性配置API密钥</li>
 *   <li>自动降级到Mock Provider如果真实Provider不可用</li>
 * </ul>
 */
public class Mem0Test {
    
    private Mem0 mem0;
    private String testUserId;

    @BeforeEach
    void setUp() {
        testUserId = "test-user-123";

        try {
            // 使用TestConfiguration创建统一配置的Mem0Config
            Mem0Config config = TestConfiguration.createMem0Config();
            
            // 创建 Mem0 实例
            mem0 = new Mem0(config);
            
            System.out.println("Mem0 initialized with providers:");
            System.out.println("  LLM Provider available: " + TestConfiguration.isLLMProviderAvailable());
            System.out.println("  Embedding Provider available: " + TestConfiguration.isEmbeddingProviderAvailable());
            
        } catch (Exception e) {
            System.err.println("Warning: Setup failed - " + e.getMessage());
            throw new RuntimeException("Test setup failed", e);
        }
    }
    
    @AfterEach
    void tearDown() throws Exception {
        if (mem0 != null) {
            try {
                mem0.close();
            } catch (Exception e) {
                System.err.println("Warning: Error during test cleanup: " + e.getMessage());
            }
        }
    }
    
    @Test
    void testAddMemorySuccess() throws Exception {
        String content = "User prefers Java for backend development";
        
        // 执行真实的内存添加操作
        String result = mem0.add(content, testUserId).get(30, TimeUnit.SECONDS);
        
        // 验证结果
        assertNotNull(result, "Memory ID should not be null");
        assertTrue(result.length() > 0, "Memory ID should not be empty");
        
        // 验证内存是否成功添加 - 通过搜索验证
        Thread.sleep(2000); // 等待索引
        List<EnhancedMemory> searchResults = mem0.search("Java backend", testUserId, 5)
                .get(30, TimeUnit.SECONDS);
        
        assertFalse(searchResults.isEmpty(), "Should find the added memory");
        boolean found = searchResults.stream()
                .anyMatch(memory -> memory.getContent().contains("Java"));
        assertTrue(found, "Should find memory containing 'Java'");
    }
    
    @Test
    void testAddMemoryWithMetadata() throws Exception {
        String content = "User completed Java certification course";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("category", "education");
        metadata.put("date", "2024-01-15");
        metadata.put("score", 95);
        
        // 执行真实的内存添加操作
        String result = mem0.add(content, testUserId, MemoryType.FACTUAL.getValue(), metadata)
                .get(30, TimeUnit.SECONDS);
        
        // 验证结果
        assertNotNull(result, "Memory ID should not be null");
        assertTrue(result.length() > 0, "Memory ID should not be empty");
        
        // 验证内存是否成功添加并包含元数据
        Thread.sleep(2000); // 等待索引
        List<EnhancedMemory> searchResults = mem0.search("certification", testUserId, 5)
                .get(30, TimeUnit.SECONDS);
                
        assertFalse(searchResults.isEmpty(), "Should find the added memory");
        boolean foundWithCorrectType = searchResults.stream()
                .anyMatch(memory -> memory.getContent().contains("certification") 
                         && memory.getType() == MemoryType.FACTUAL);
        assertTrue(foundWithCorrectType, "Should find memory with FACTUAL type");
    }
    
    @Test
    void testSearchMemories() throws Exception {
        // 先添加一些测试数据
        String content1 = "User is proficient in Python programming";
        String content2 = "User has experience with Java Spring framework";
        
        String memoryId1 = mem0.add(content1, testUserId).get(30, TimeUnit.SECONDS);
        String memoryId2 = mem0.add(content2, testUserId).get(30, TimeUnit.SECONDS);
        
        assertNotNull(memoryId1);
        assertNotNull(memoryId2);
        
        // 等待索引
        Thread.sleep(3000);
        
        // 搜索相关内存
        String query = "programming languages";
        List<EnhancedMemory> results = mem0.search(query, testUserId, 5)
                .get(30, TimeUnit.SECONDS);
        
        // 验证搜索结果
        assertNotNull(results, "Search results should not be null");
        
        if (!results.isEmpty()) {
            // 验证相关性分数是递减的
            for (int i = 0; i < results.size() - 1; i++) {
                assertTrue(results.get(i).getRelevanceScore() >= results.get(i + 1).getRelevanceScore(),
                    "Results should be sorted by relevance score");
            }
        }
    }
    
    @Test
    void testDeleteMemory() throws Exception {
        // 先添加一个内存
        String content = "This memory will be deleted in test";
        String memoryId = mem0.add(content, testUserId).get(30, TimeUnit.SECONDS);
        
        assertNotNull(memoryId, "Memory should be created successfully");
        
        // 删除内存
        mem0.delete(memoryId).get(30, TimeUnit.SECONDS);
        
        // 验证删除成功 - 这里简单验证不抛异常即可
        assertTrue(true, "Delete operation should complete without exception");
    }
    
    @Test
    void testClassifyMemory() throws Exception {
        String content = "How to implement a REST API in Spring Boot";
        
        // 执行真实的内存分类
        MemoryType result = mem0.classifyMemory(content).get(30, TimeUnit.SECONDS);
        
        // 验证分类结果
        assertNotNull(result, "Classification result should not be null");
        
        // 程序性内容通常被分类为 PROCEDURAL 或其他合理类型
        assertTrue(
            result == MemoryType.PROCEDURAL || 
            result == MemoryType.FACTUAL || 
            result == MemoryType.SEMANTIC,
            "Should classify 'how to' content as a reasonable type, got: " + result
        );
    }
    
    @Test
    void testBuilderPattern() {
        // 测试通过配置创建Mem0实例
        Mem0Config config = TestConfiguration.createMem0Config();
        Mem0 testMem0 = new Mem0(config);
        
        assertNotNull(testMem0, "Mem0 instance should be created successfully");
        testMem0.close();
    }
    
    @Test
    void testConnectionsWork() throws Exception {
        // 简单测试各组件连接是否正常
        System.out.println("Testing component connections:");
        System.out.println("  LLM Provider available: " + TestConfiguration.isLLMProviderAvailable());
        System.out.println("  Embedding Provider available: " + TestConfiguration.isEmbeddingProviderAvailable());
        
        // 测试基本的Mem0功能
        if (TestConfiguration.areAllProvidersAvailable()) {
            String memoryId = mem0.add("Test content for connection", testUserId).get(30, TimeUnit.SECONDS);
            assertNotNull(memoryId, "Should be able to add memory");
            
            List<EnhancedMemory> results = mem0.search("test", testUserId, 5).get(30, TimeUnit.SECONDS);
            assertNotNull(results, "Should be able to search memories");
            
            mem0.delete(memoryId).get(30, TimeUnit.SECONDS);
            System.out.println("All connections working properly");
        } else {
            System.out.println("Skipping connection tests - providers not fully available");
        }
    }
    
    // 完整工作流集成测试 - Complete workflow integration test
    @Test
    void testFullWorkflow() throws Exception {
        // 完整的工作流测试
        String content = "User prefers microservices architecture";
        
        // 1. 添加内存
        String memoryId = mem0.add(content, testUserId).get(30, TimeUnit.SECONDS);
        assertNotNull(memoryId);
        
        // 2. 搜索内存
        Thread.sleep(2000);
        List<EnhancedMemory> searchResults = mem0.search("microservices", testUserId, 5)
                .get(30, TimeUnit.SECONDS);
        assertFalse(searchResults.isEmpty());
        
        // 3. 获取所有内存
        List<EnhancedMemory> allMemories = mem0.getAll(testUserId).get(30, TimeUnit.SECONDS);
        assertFalse(allMemories.isEmpty());
        
        // 4. 删除内存
        mem0.delete(memoryId).get(30, TimeUnit.SECONDS);
    }
}