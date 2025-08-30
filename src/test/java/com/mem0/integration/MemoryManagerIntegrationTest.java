package com.mem0.integration;

import com.mem0.memory.ConcurrentMemoryManager;
import com.mem0.core.EnhancedMemory;
import com.mem0.pipeline.AsyncMemoryPipeline;
import com.mem0.performance.ConcurrentExecutionManager;
import com.mem0.concurrency.ConcurrencyController;
import com.mem0.monitoring.PerformanceMonitor;
import com.mem0.embedding.impl.HighPerformanceTFIDFProvider;
import com.mem0.vector.impl.HighPerformanceVectorStore;
import com.mem0.store.GraphStore;
import com.mem0.graph.impl.HighPerformanceGraphStore;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 内存管理器集成测试
 * 测试完整的内存管理流程和高级功能
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MemoryManagerIntegrationTest {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryManagerIntegrationTest.class);
    
    private static ConcurrentMemoryManager memoryManager;
    private static AsyncMemoryPipeline pipeline;
    private static ConcurrentExecutionManager concurrentExecutionManager;
    private static ConcurrencyController concurrencyController;
    private static PerformanceMonitor performanceMonitor;
    private static HighPerformanceTFIDFProvider embeddingProvider;
    private static HighPerformanceVectorStore vectorStore;
    private static HighPerformanceGraphStore graphStore;
    
    // 测试数据
    private static final String TEST_USER_ID = "test_user_001";
    private static final String TEST_CONTENT_1 = "这是一个关于机器学习的测试内容，包含了人工智能和深度学习的相关信息。";
    private static final String TEST_CONTENT_2 = "Java是一种面向对象的编程语言，广泛应用于企业级应用开发。";
    private static final String TEST_CONTENT_3 = "分布式系统需要考虑一致性、可用性和分区容错性，这就是CAP定理。";
    
    @BeforeAll
    static void setUp() {
        logger.info("开始初始化集成测试环境");
        
        try {
            // 初始化核心组件
            performanceMonitor = new PerformanceMonitor();
            performanceMonitor.startMonitoring();
            
            concurrentExecutionManager = new ConcurrentExecutionManager();
            concurrencyController = new ConcurrencyController(performanceMonitor);
            
            embeddingProvider = new HighPerformanceTFIDFProvider();
            vectorStore = new HighPerformanceVectorStore();
            graphStore = new HighPerformanceGraphStore();
            
            pipeline = new AsyncMemoryPipeline(
                embeddingProvider, vectorStore, graphStore,
                concurrentExecutionManager, performanceMonitor
            );
            
            memoryManager = new ConcurrentMemoryManager(
                pipeline, concurrencyController, performanceMonitor
            );
            
            logger.info("集成测试环境初始化完成");
            
        } catch (Exception e) {
            logger.error("集成测试环境初始化失败", e);
            fail("测试环境初始化失败: " + e.getMessage());
        }
    }
    
    @AfterAll
    static void tearDown() {
        logger.info("开始清理集成测试环境");
        
        try {
            if (memoryManager != null) {
                memoryManager.shutdown().get(10, TimeUnit.SECONDS);
            }
            
            if (pipeline != null) {
                pipeline.shutdown().get(10, TimeUnit.SECONDS);
            }
            
            if (concurrencyController != null) {
                concurrencyController.shutdown().get(10, TimeUnit.SECONDS);
            }
            
            if (performanceMonitor != null) {
                performanceMonitor.shutdown();
            }
            
            if (embeddingProvider != null) {
                embeddingProvider.close();
            }
            
            logger.info("集成测试环境清理完成");
            
        } catch (Exception e) {
            logger.error("清理测试环境时发生错误", e);
        }
    }

    @Test
    @Order(1)
    @DisplayName("测试单个内存创建")
    void testSingleMemoryCreation() {
        logger.info("测试单个内存创建");
        
        assertDoesNotThrow(() -> {
            CompletableFuture<String> future = memoryManager.createMemory(
                TEST_CONTENT_1, TEST_USER_ID, createTestMetadata()
            );
            
            String memoryId = future.get(10, TimeUnit.SECONDS);
            
            assertNotNull(memoryId, "内存ID不应为空");
            assertTrue(memoryId.startsWith("mem_"), "内存ID格式错误");
            
            logger.info("内存创建成功，ID: {}", memoryId);
        });
    }

    @Test
    @Order(2)
    @DisplayName("测试批量内存创建")
    void testBatchMemoryCreation() {
        logger.info("测试批量内存创建");
        
        assertDoesNotThrow(() -> {
            List<ConcurrentMemoryManager.MemoryCreationRequest> requests = Arrays.asList(
                new ConcurrentMemoryManager.MemoryCreationRequest(TEST_CONTENT_1, TEST_USER_ID, createTestMetadata()),
                new ConcurrentMemoryManager.MemoryCreationRequest(TEST_CONTENT_2, TEST_USER_ID, createTestMetadata()),
                new ConcurrentMemoryManager.MemoryCreationRequest(TEST_CONTENT_3, TEST_USER_ID, createTestMetadata())
            );
            
            CompletableFuture<List<String>> future = memoryManager.createMemoriesBatch(requests);
            List<String> memoryIds = future.get(30, TimeUnit.SECONDS);
            
            assertNotNull(memoryIds, "批量创建结果不应为空");
            assertEquals(3, memoryIds.size(), "应该创建3个内存");
            
            for (String memoryId : memoryIds) {
                assertNotNull(memoryId, "内存ID不应为空");
            }
            
            logger.info("批量内存创建成功，数量: {}", memoryIds.size());
        });
    }

    @Test
    @Order(3)
    @DisplayName("测试内存查询")
    void testMemoryRetrieval() {
        logger.info("测试内存查询");
        
        assertDoesNotThrow(() -> {
            // 先创建一个内存
            String memoryId = memoryManager.createMemory(
                TEST_CONTENT_1, TEST_USER_ID, createTestMetadata()
            ).get(10, TimeUnit.SECONDS);
            
            // 查询内存
            CompletableFuture<EnhancedMemory> future = memoryManager.getMemory(memoryId);
            EnhancedMemory memory = future.get(10, TimeUnit.SECONDS);
            
            assertNotNull(memory, "查询到的内存不应为空");
            assertEquals(memoryId, memory.getId(), "内存ID应该匹配");
            assertEquals(TEST_CONTENT_1, memory.getContent(), "内存内容应该匹配");
            assertEquals(TEST_USER_ID, memory.getUserId(), "用户ID应该匹配");
            
            logger.info("内存查询成功，内容长度: {}", memory.getContent().length());
        });
    }

    @Test
    @Order(4)
    @DisplayName("测试相似内存搜索")
    void testSimilarMemorySearch() {
        logger.info("测试相似内存搜索");
        
        assertDoesNotThrow(() -> {
            // 先创建一些内存
            memoryManager.createMemory(TEST_CONTENT_1, TEST_USER_ID, createTestMetadata()).get();
            memoryManager.createMemory(TEST_CONTENT_2, TEST_USER_ID, createTestMetadata()).get();
            memoryManager.createMemory(TEST_CONTENT_3, TEST_USER_ID, createTestMetadata()).get();
            
            // 等待索引建立
            Thread.sleep(1000);
            
            // 搜索相似内容
            String searchQuery = "机器学习和人工智能";
            CompletableFuture<List<EnhancedMemory>> future = memoryManager.searchSimilarMemories(
                searchQuery, TEST_USER_ID, 10, 0.1f
            );
            
            List<EnhancedMemory> results = future.get(15, TimeUnit.SECONDS);
            
            assertNotNull(results, "搜索结果不应为空");
            assertTrue(results.size() >= 1, "应该至少找到一个相似内存");
            
            for (EnhancedMemory result : results) {
                assertEquals(TEST_USER_ID, result.getUserId(), "搜索结果应该属于正确的用户");
            }
            
            logger.info("相似内存搜索成功，结果数: {}", results.size());
        });
    }

    @Test
    @Order(5)
    @DisplayName("测试内存更新")
    void testMemoryUpdate() {
        logger.info("测试内存更新");
        
        assertDoesNotThrow(() -> {
            // 创建内存
            String memoryId = memoryManager.createMemory(
                TEST_CONTENT_1, TEST_USER_ID, createTestMetadata()
            ).get(10, TimeUnit.SECONDS);
            
            // 更新内容
            String updatedContent = TEST_CONTENT_1 + " 这是更新后的内容。";
            Map<String, Object> updatedMetadata = createTestMetadata();
            updatedMetadata.put("updated", true);
            
            CompletableFuture<Boolean> updateFuture = memoryManager.updateMemory(
                memoryId, updatedContent, updatedMetadata
            );
            
            Boolean updateResult = updateFuture.get(10, TimeUnit.SECONDS);
            assertTrue(updateResult, "内存更新应该成功");
            
            // 验证更新
            EnhancedMemory updatedMemory = memoryManager.getMemory(memoryId).get(10, TimeUnit.SECONDS);
            assertNotNull(updatedMemory, "更新后的内存应该存在");
            assertEquals(updatedContent, updatedMemory.getContent(), "内容应该已更新");
            assertTrue((Boolean) updatedMemory.getMetadata().get("updated"), "元数据应该已更新");
            
            logger.info("内存更新成功");
        });
    }

    @Test
    @Order(6)
    @DisplayName("测试用户内存列表查询")
    void testUserMemoriesRetrieval() {
        logger.info("测试用户内存列表查询");
        
        assertDoesNotThrow(() -> {
            // 为用户创建多个内存
            String userId = "test_user_" + System.currentTimeMillis();
            
            List<CompletableFuture<String>> futures = Arrays.asList(
                memoryManager.createMemory(TEST_CONTENT_1, userId, createTestMetadata()),
                memoryManager.createMemory(TEST_CONTENT_2, userId, createTestMetadata()),
                memoryManager.createMemory(TEST_CONTENT_3, userId, createTestMetadata())
            );
            
            // 等待所有内存创建完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);
            
            // 查询用户的所有内存
            CompletableFuture<List<EnhancedMemory>> future = memoryManager.getUserMemories(userId);
            List<EnhancedMemory> userMemories = future.get(15, TimeUnit.SECONDS);
            
            assertNotNull(userMemories, "用户内存列表不应为空");
            assertEquals(3, userMemories.size(), "应该有3个内存");
            
            for (EnhancedMemory memory : userMemories) {
                assertEquals(userId, memory.getUserId(), "所有内存都应该属于正确的用户");
            }
            
            logger.info("用户内存查询成功，用户: {}, 内存数: {}", userId, userMemories.size());
        });
    }

    @Test
    @Order(7)
    @DisplayName("测试内存删除")
    void testMemoryDeletion() {
        logger.info("测试内存删除");
        
        assertDoesNotThrow(() -> {
            // 创建内存
            String memoryId = memoryManager.createMemory(
                TEST_CONTENT_1, TEST_USER_ID, createTestMetadata()
            ).get(10, TimeUnit.SECONDS);
            
            // 验证内存存在
            EnhancedMemory memory = memoryManager.getMemory(memoryId).get(10, TimeUnit.SECONDS);
            assertNotNull(memory, "内存应该存在");
            
            // 删除内存
            CompletableFuture<Boolean> deleteFuture = memoryManager.deleteMemory(memoryId);
            Boolean deleteResult = deleteFuture.get(10, TimeUnit.SECONDS);
            assertTrue(deleteResult, "内存删除应该成功");
            
            // 验证内存已删除
            EnhancedMemory deletedMemory = memoryManager.getMemory(memoryId).get(10, TimeUnit.SECONDS);
            assertNull(deletedMemory, "内存应该已被删除");
            
            logger.info("内存删除成功");
        });
    }

    @Test
    @Order(8)
    @DisplayName("测试并发操作")
    void testConcurrentOperations() {
        logger.info("测试并发操作");
        
        assertDoesNotThrow(() -> {
            int concurrencyLevel = 10;
            int operationsPerThread = 5;
            String baseUserId = "concurrent_user_";
            
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            for (int i = 0; i < concurrencyLevel; i++) {
                String userId = baseUserId + i;
                
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        for (int j = 0; j < operationsPerThread; j++) {
                            // 创建内存
                            String content = "并发测试内容 " + Thread.currentThread().getName() + " - " + j;
                            String memoryId = memoryManager.createMemory(content, userId, createTestMetadata())
                                .get(15, TimeUnit.SECONDS);
                            
                            assertNotNull(memoryId, "并发创建的内存ID不应为空");
                            
                            // 查询内存
                            EnhancedMemory memory = memoryManager.getMemory(memoryId)
                                .get(10, TimeUnit.SECONDS);
                            assertNotNull(memory, "并发查询的内存不应为空");
                        }
                    } catch (Exception e) {
                        logger.error("并发操作失败", e);
                        throw new RuntimeException(e);
                    }
                });
                
                futures.add(future);
            }
            
            // 等待所有并发操作完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(60, TimeUnit.SECONDS);
            
            logger.info("并发操作测试完成，并发度: {}, 每线程操作数: {}", concurrencyLevel, operationsPerThread);
        });
    }

    @Test
    @Order(9)
    @DisplayName("测试系统性能监控")
    void testPerformanceMonitoring() {
        logger.info("测试系统性能监控");
        
        assertDoesNotThrow(() -> {
            // 执行一些操作来生成性能数据
            for (int i = 0; i < 10; i++) {
                String content = "性能测试内容 " + i;
                memoryManager.createMemory(content, TEST_USER_ID, createTestMetadata()).get();
            }
            
            // 等待性能数据收集
            Thread.sleep(2000);
            
            // 检查性能统计
            Object managerStats = memoryManager.getStats();
            assertNotNull(managerStats, "管理器统计信息不应为空");
            
        Object performanceSnapshot = performanceMonitor.getLatestSnapshot();
            assertNotNull(performanceSnapshot, "性能快照不应为空");
            
        Object performanceReport = performanceMonitor.generateReport();
            assertNotNull(performanceReport, "性能报告不应为空");
            
            logger.info("性能监控测试完成");
            logger.info("管理器统计: {}", managerStats);
        });
    }

    @Test
    @Order(10)
    @DisplayName("测试系统健康检查")
    void testSystemHealthCheck() {
        logger.info("测试系统健康检查");
        
        assertDoesNotThrow(() -> {
            // 检查内存管理器健康状态
        Object healthStatus = memoryManager.checkHealth();
            assertNotNull(healthStatus, "健康状态不应为空");
            
            logger.info("系统健康状态: {}", healthStatus);
            
            // 检查并发控制器健康状态
        Object concurrencyHealth = concurrencyController.checkHealth();
            assertNotNull(concurrencyHealth, "并发控制健康状态不应为空");
            
            logger.info("并发控制健康状态: {}", concurrencyHealth);
        });
    }

    @Test
    @Order(11)
    @DisplayName("测试冲突解决")
    void testConflictResolution() {
        logger.info("测试冲突解决");
        
        assertDoesNotThrow(() -> {
            String userId = "conflict_test_user";
            String similarContent1 = "这是一个关于Java编程的内容，包含面向对象的概念。";
            String similarContent2 = "这是一个关于Java编程语言的内容，讲述面向对象编程的理念。";
            
            // 创建相似内容，测试冲突解决
            String memoryId1 = memoryManager.createMemory(similarContent1, userId, createTestMetadata())
                .get(10, TimeUnit.SECONDS);
            
            String memoryId2 = memoryManager.createMemory(similarContent2, userId, createTestMetadata())
                .get(10, TimeUnit.SECONDS);
            
            assertNotNull(memoryId1, "第一个内存应该创建成功");
            assertNotNull(memoryId2, "第二个内存应该创建成功");
            
            // 验证内容是否被正确处理
            EnhancedMemory memory1 = memoryManager.getMemory(memoryId1).get();
            EnhancedMemory memory2 = memoryManager.getMemory(memoryId2).get();
            
            assertNotNull(memory1, "第一个内存应该存在");
            assertNotNull(memory2, "第二个内存应该存在");
            
            logger.info("冲突解决测试完成");
        });
    }

    // 辅助方法

    private Map<String, Object> createTestMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("category", "test");
        metadata.put("priority", 1);
        metadata.put("created_by", "integration_test");
        metadata.put("timestamp", System.currentTimeMillis());
        return metadata;
    }

    private void logTestCompletion(String testName) {
        logger.info("测试完成: {}", testName);
    }
}