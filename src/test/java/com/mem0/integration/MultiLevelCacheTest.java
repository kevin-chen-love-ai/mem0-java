package com.mem0.integration;

import com.mem0.cache.*;
import com.mem0.config.CacheConfig;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多级缓存测试
 * 测试本地缓存+Redis的多级缓存架构
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MultiLevelCacheTest {
    
    private static final Logger logger = LoggerFactory.getLogger(MultiLevelCacheTest.class);
    
    private static MultiLevelCache<String, String> cache;
    private static CacheConfig config;

    @BeforeAll
    static void setUp() {
        logger.info("初始化多级缓存测试环境");
        
        config = CacheConfig.builder()
            .l1MaxSize(100)
            .l1TtlMs(30000) // 30秒
            .redisHost("localhost")
            .redisPort(6379)
            .l2TtlMs(60000) // 60秒
            .build();
        
        cache = new MultiLevelCache<>(config);
        
        logger.info("多级缓存测试环境初始化完成");
    }

    @AfterAll
    static void tearDown() {
        logger.info("清理多级缓存测试环境");
        
        if (cache != null) {
            cache.shutdown();
        }
        
        logger.info("多级缓存测试环境清理完成");
    }

    @Test
    @Order(1)
    @DisplayName("测试缓存基本写入和读取")
    void testBasicPutAndGet() {
        logger.info("测试缓存基本写入和读取");
        
        String key = "test_key_1";
        String value = "test_value_1";
        
        // 写入缓存
        cache.put(key, value);
        
        // 等待异步写入完成
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 读取缓存
        CompletableFuture<String> future = cache.get(key, null);
        
        assertDoesNotThrow(() -> {
            String result = future.get(5, TimeUnit.SECONDS);
            assertEquals(value, result, "缓存值应该匹配");
            logger.info("缓存读取成功: {} = {}", key, result);
        });
    }

    @Test
    @Order(2)
    @DisplayName("测试L1缓存命中")
    void testL1CacheHit() {
        logger.info("测试L1缓存命中");
        
        String key = "l1_test_key";
        String value = "l1_test_value";
        
        // 写入缓存
        cache.put(key, value);
        
        // 立即读取（应该从L1缓存命中）
        String result = cache.getSync(key, null);
        assertEquals(value, result, "L1缓存应该命中");
        
        // 验证统计信息
        MultiLevelCacheStats stats = cache.getStats();
        assertTrue(stats.getL1HitRate() > 0, "L1命中率应该大于0");
        
        logger.info("L1缓存命中测试完成，命中率: {:.2f}%", stats.getL1HitRate() * 100);
    }

    @Test
    @Order(3)
    @DisplayName("测试数据源回调")
    void testDataSourceCallback() {
        logger.info("测试数据源回调");
        
        String key = "callback_test_key";
        String expectedValue = "callback_test_value";
        
        Supplier<String> dataLoader = () -> {
            logger.info("数据源回调被调用");
            return expectedValue;
        };
        
        // 第一次访问，应该触发数据源加载
        CompletableFuture<String> future = cache.get(key, dataLoader);
        
        assertDoesNotThrow(() -> {
            String result = future.get(10, TimeUnit.SECONDS);
            assertEquals(expectedValue, result, "从数据源加载的值应该正确");
            
            // 验证统计信息
            MultiLevelCacheStats stats = cache.getStats();
            assertTrue(stats.getOverallStats().getDataLoads() > 0, "应该有数据源加载记录");
            
            logger.info("数据源回调测试完成，加载次数: {}", stats.getOverallStats().getDataLoads());
        });
    }

    @Test
    @Order(4)
    @DisplayName("测试缓存TTL")
    void testCacheTTL() {
        logger.info("测试缓存TTL");
        
        String key = "ttl_test_key";
        String value = "ttl_test_value";
        long shortTtl = 2000; // 2秒
        
        // 使用短TTL写入缓存
        cache.put(key, value, shortTtl);
        
        // 立即读取应该成功
        String result1 = cache.getSync(key, null);
        assertEquals(value, result1, "立即读取应该成功");
        
        // 等待TTL过期
        assertDoesNotThrow(() -> {
            Thread.sleep(shortTtl + 500); // 等待TTL过期
            
            // 再次读取应该失败（返回null）
            String result2 = cache.getSync(key, () -> "expired_fallback");
            assertEquals("expired_fallback", result2, "过期后应该使用数据源");
            
            logger.info("缓存TTL测试完成");
        });
    }

    @Test
    @Order(5)
    @DisplayName("测试缓存删除")
    void testCacheRemoval() {
        logger.info("测试缓存删除");
        
        String key = "removal_test_key";
        String value = "removal_test_value";
        
        // 写入缓存
        cache.put(key, value);
        
        // 验证存在
        assertTrue(cache.containsKey(key), "缓存键应该存在");
        
        // 删除缓存
        cache.remove(key);
        
        // 等待异步删除完成
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 验证已删除
        String result = cache.getSync(key, () -> "deleted_fallback");
        assertEquals("deleted_fallback", result, "删除后应该使用数据源");
        
        logger.info("缓存删除测试完成");
    }

    @Test
    @Order(6)
    @DisplayName("测试缓存预热")
    void testCacheWarmup() {
        logger.info("测试缓存预热");
        
        Map<String, Supplier<String>> warmupData = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            String key = "warmup_key_" + i;
            String value = "warmup_value_" + i;
            warmupData.put(key, () -> value);
        }
        
        CompletableFuture<Void> warmupFuture = cache.warmup(warmupData);
        
        assertDoesNotThrow(() -> {
            warmupFuture.get(15, TimeUnit.SECONDS);
            
            // 验证预热数据
            for (String key : warmupData.keySet()) {
                assertTrue(cache.containsKey(key), "预热的键应该存在: " + key);
            }
            
            logger.info("缓存预热测试完成，预热数据量: {}", warmupData.size());
        });
    }

    @Test
    @Order(7)
    @DisplayName("测试缓存清空")
    void testCacheClear() {
        logger.info("测试缓存清空");
        
        // 写入一些测试数据
        for (int i = 0; i < 5; i++) {
            cache.put("clear_test_key_" + i, "clear_test_value_" + i);
        }
        
        // 等待写入完成
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 验证缓存大小
        MultiLevelCache.CacheSize sizeBefore = cache.getSize();
        assertTrue(sizeBefore.getL1Size() > 0, "清空前L1缓存应该有数据");
        
        // 清空缓存
        cache.clear();
        
        // 等待清空完成
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 验证缓存已清空
        MultiLevelCache.CacheSize sizeAfter = cache.getSize();
        assertEquals(0, sizeAfter.getL1Size(), "清空后L1缓存应该为空");
        
        logger.info("缓存清空测试完成");
    }

    @Test
    @Order(8)
    @DisplayName("测试并发访问")
    void testConcurrentAccess() {
        logger.info("测试并发访问");
        
        int threadCount = 10;
        int operationsPerThread = 20;
        String keyPrefix = "concurrent_key_";
        String valuePrefix = "concurrent_value_";
        
        CompletableFuture<Void>[] futures = new CompletableFuture[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            
            futures[i] = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    String key = keyPrefix + threadId + "_" + j;
                    String value = valuePrefix + threadId + "_" + j;
                    
                    // 写入
                    cache.put(key, value);
                    
                    // 读取
                    String result = cache.getSync(key, () -> "fallback_" + key);
                    
                    // 验证结果
                    assertTrue(result.equals(value) || result.startsWith("fallback_"), 
                              "并发读取结果应该正确");
                }
            });
        }
        
        assertDoesNotThrow(() -> {
            CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);
            
            MultiLevelCacheStats stats = cache.getStats();
            logger.info("并发访问测试完成 - 总请求: {}, 整体命中率: {:.2f}%",
                       stats.getTotalRequests(), stats.getOverallHitRate() * 100);
        });
    }

    @Test
    @Order(9)
    @DisplayName("测试健康检查")
    void testHealthCheck() {
        logger.info("测试健康检查");
        
        MultiLevelCache.CacheHealthStatus health = cache.checkHealth();
        
        assertNotNull(health, "健康状态不应为空");
        
        // L1缓存应该总是健康的
        assertTrue(health.isL1Healthy(), "L1缓存应该健康");
        
        // L2缓存健康状态取决于Redis连接
        // 在没有Redis服务器的测试环境中，L2可能不健康
        logger.info("健康检查结果: {}", health);
        
        if (!health.isL2Healthy()) {
            logger.warn("L2缓存(Redis)不健康，问题: {}", health.getIssues());
        }
    }

    @Test
    @Order(10)
    @DisplayName("测试统计信息")
    void testStatistics() {
        logger.info("测试统计信息");
        
        // 执行一些操作来生成统计数据
        for (int i = 0; i < 20; i++) {
            String key = "stats_test_key_" + i;
            String value = "stats_test_value_" + i;
            
            cache.put(key, value);
            cache.getSync(key, () -> "fallback");
        }
        
        // 获取统计信息
        MultiLevelCacheStats stats = cache.getStats();
        
        assertNotNull(stats, "统计信息不应为空");
        assertTrue(stats.getTotalRequests() > 0, "应该有请求记录");
        
        // 生成性能摘要
        String summary = stats.generatePerformanceSummary();
        assertNotNull(summary, "性能摘要不应为空");
        assertFalse(summary.trim().isEmpty(), "性能摘要不应为空字符串");
        
        logger.info("统计信息测试完成");
        logger.info("性能摘要:\n{}", summary);
        
        // 生成详细报告
        String detailedReport = stats.generateDetailedReport();
        assertNotNull(detailedReport, "详细报告不应为空");
        
        logger.info("详细报告:\n{}", detailedReport);
    }

    @Test
    @Order(11)
    @DisplayName("测试错误处理")
    void testErrorHandling() {
        logger.info("测试错误处理");
        
        // 测试空键
        assertDoesNotThrow(() -> {
            cache.put(null, "test_value");
            String result = cache.getSync(null, () -> "fallback");
            assertEquals("fallback", result, "空键应该使用数据源");
        });
        
        // 测试空值
        assertDoesNotThrow(() -> {
            cache.put("test_key", null);
            String result = cache.getSync("test_key", () -> "fallback");
            assertEquals("fallback", result, "空值应该使用数据源");
        });
        
        // 测试数据源异常
        assertDoesNotThrow(() -> {
            Supplier<String> faultyLoader = () -> {
                throw new RuntimeException("数据源异常");
            };
            
            String result = cache.getSync("faulty_key", faultyLoader);
            assertNull(result, "数据源异常时应该返回null");
        });
        
        logger.info("错误处理测试完成");
    }
}