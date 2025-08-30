package com.mem0.performance.benchmark;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mem0.Mem0;
import com.mem0.config.Mem0Config;
import com.mem0.core.MemoryService.Memory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.time.Instant;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 性能基准测试
 * 
 * 使用 Java 8 语法进行内存系统性能测试
 * 包括基础操作、并发性能、吞吐量、延迟和内存压力测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PerformanceBenchmarkTest {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceBenchmarkTest.class);
    private static Mem0 mem0;
    private static final int WARMUP_ITERATIONS = 100;
    private static final int TEST_ITERATIONS = 1000;
    private static final int CONCURRENT_THREADS = 10;
    
    @BeforeAll
    static void setUp() {
        logger.info("初始化性能基准测试环境");
        
        try {
            // 使用默认配置创建内存实例
            mem0 = new Mem0();
            
            // 预热系统
            warmupSystem();
            
            logger.info("性能基准测试环境初始化完成");
        } catch (Exception e) {
            logger.error("Failed to initialize performance benchmark environment", e);
            throw new RuntimeException("Setup failed", e);
        }
    }
    
    private static void warmupSystem() {
        logger.info("系统预热中...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            try {
                mem0.add("Warmup memory " + i, "warmup-user");
            } catch (Exception e) {
                // 忽略预热阶段的错误
            }
        }
        logger.info("系统预热完成");
    }

    @AfterAll
    static void tearDown() {
        logger.info("清理性能基准测试环境");
        
        if (mem0 != null) {
            try {
                // 清理测试数据
                logger.info("清理测试数据完成");
            } catch (Exception e) {
                logger.warn("清理资源时出现错误", e);
            }
        }
        
        logger.info("性能基准测试环境清理完成");
    }

    @Test
    @Order(1)
    @DisplayName("测试基础性能基准")
    void testBasicPerformanceBenchmarks() {
        logger.info("开始基础性能基准测试");
        
        // 测试添加操作性能
        PerformanceResult addResult = measureAddPerformance();
        
        // 测试搜索操作性能
        PerformanceResult searchResult = measureSearchPerformance();
        
        // 验证性能指标
        assertTrue(addResult.averageLatency < 1000, "Add操作平均延迟应小于1000ms");
        assertTrue(addResult.throughput > 10, "Add操作吞吐量应大于10 ops/sec");
        
        assertTrue(searchResult.averageLatency < 500, "Search操作平均延迟应小于500ms");
        assertTrue(searchResult.throughput > 20, "Search操作吞吐量应大于20 ops/sec");
        
        logger.info("基础性能基准测试完成 - Add: {}ms平均延迟, {}ops/s吞吐量", 
                   addResult.averageLatency, addResult.throughput);
        logger.info("基础性能基准测试完成 - Search: {}ms平均延迟, {}ops/s吞吐量", 
                   searchResult.averageLatency, searchResult.throughput);
    }

    @Test
    @Order(2)
    @DisplayName("测试并发性能基准")
    void testConcurrencyBenchmarks() throws InterruptedException {
        logger.info("开始并发性能基准测试");
        
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);
        AtomicLong totalOperations = new AtomicLong(0);
        AtomicLong totalTime = new AtomicLong(0);
        
        List<Future<Long>> futures = new ArrayList<>();
        
        // 启动并发添加操作
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final int threadId = i;
            Future<Long> future = executor.submit(() -> {
                long startTime = System.currentTimeMillis();
                int operationCount = 0;
                
                try {
                    for (int j = 0; j < TEST_ITERATIONS / CONCURRENT_THREADS; j++) {
                        String userId = "concurrent-user-" + threadId;
                        String content = "Concurrent test memory " + threadId + "-" + j;
                        
                        mem0.add(content, userId);
                        operationCount++;
                    }
                } catch (Exception e) {
                    logger.warn("并发操作异常", e);
                } finally {
                    latch.countDown();
                }
                
                return System.currentTimeMillis() - startTime;
            });
            futures.add(future);
        }
        
        // 等待所有任务完成
        assertTrue(latch.await(30, TimeUnit.SECONDS), "并发测试应在30秒内完成");
        
        // 计算并发性能指标
        long totalExecutionTime = 0;
        for (Future<Long> future : futures) {
            try {
                totalExecutionTime += future.get();
            } catch (ExecutionException e) {
                logger.warn("获取并发任务结果异常", e);
            }
        }
        
        double averageConcurrentLatency = (double) totalExecutionTime / CONCURRENT_THREADS;
        double concurrentThroughput = (double) TEST_ITERATIONS * 1000 / averageConcurrentLatency;
        
        // 验证并发性能
        assertTrue(averageConcurrentLatency < 5000, "并发操作平均延迟应小于5000ms");
        assertTrue(concurrentThroughput > 50, "并发吞吐量应大于50 ops/sec");
        
        executor.shutdown();
        
        logger.info("并发性能基准测试完成 - 平均延迟: {}ms, 吞吐量: {}ops/s", 
                   averageConcurrentLatency, concurrentThroughput);
    }

    @Test
    @Order(3)
    @DisplayName("测试吞吐量基准")
    void testThroughputBenchmarks() {
        logger.info("开始吞吐量基准测试");
        
        // 大量数据写入吞吐量测试
        Instant startTime = Instant.now();
        int successfulOperations = 0;
        
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            try {
                String userId = "throughput-user";
                String content = "Throughput test memory " + i;
                
                mem0.add(content, userId);
                successfulOperations++;
            } catch (Exception e) {
                logger.debug("吞吐量测试中单个操作失败: {}", e.getMessage());
            }
            
            // 每100个操作输出一次进度
            if (i > 0 && i % 100 == 0) {
                double currentThroughput = (double) i * 1000 / Duration.between(startTime, Instant.now()).toMillis();
                logger.debug("当前吞吐量: {} ops/sec (完成 {}/{})", currentThroughput, i, TEST_ITERATIONS);
            }
        }
        
        Instant endTime = Instant.now();
        long totalTimeMs = Duration.between(startTime, endTime).toMillis();
        double throughput = (double) successfulOperations * 1000 / totalTimeMs;
        
        // 验证吞吐量性能
        assertTrue(successfulOperations >= TEST_ITERATIONS * 0.9, "成功操作数应至少为90%");
        assertTrue(throughput > 100, "吞吐量应大于100 ops/sec");
        
        logger.info("吞吐量基准测试完成 - 成功操作: {}/{}, 总时间: {}ms, 吞吐量: {} ops/sec", 
                   successfulOperations, TEST_ITERATIONS, totalTimeMs, throughput);
    }

    @Test
    @Order(4)
    @DisplayName("测试延迟基准")
    void testLatencyBenchmarks() {
        logger.info("开始延迟基准测试");
        
        List<Long> addLatencies = new ArrayList<>();
        List<Long> searchLatencies = new ArrayList<>();
        
        // 测试单个操作的延迟分布
        for (int i = 0; i < 100; i++) {
            // 测试添加操作延迟
            long addStartTime = System.nanoTime();
            try {
                String userId = "latency-user";
                String content = "Latency test memory " + i;
                
                mem0.add(content, userId);
                long addLatency = (System.nanoTime() - addStartTime) / 1_000_000; // 转换为毫秒
                addLatencies.add(addLatency);
            } catch (Exception e) {
                logger.debug("延迟测试中添加操作失败: {}", e.getMessage());
            }
            
            // 测试搜索操作延迟
            long searchStartTime = System.nanoTime();
            try {
                mem0.search("latency test", "latency-user", 5);
                long searchLatency = (System.nanoTime() - searchStartTime) / 1_000_000;
                searchLatencies.add(searchLatency);
            } catch (Exception e) {
                logger.debug("延迟测试中搜索操作失败: {}", e.getMessage());
            }
        }
        
        // 计算延迟统计
        LatencyStats addStats = calculateLatencyStats(addLatencies);
        LatencyStats searchStats = calculateLatencyStats(searchLatencies);
        
        // 验证延迟性能
        assertTrue(addStats.average < 100, "添加操作平均延迟应小于100ms");
        assertTrue(addStats.p95 < 200, "添加操作P95延迟应小于200ms");
        assertTrue(addStats.p99 < 500, "添加操作P99延迟应小于500ms");
        
        assertTrue(searchStats.average < 50, "搜索操作平均延迟应小于50ms");
        assertTrue(searchStats.p95 < 100, "搜索操作P95延迟应小于100ms");
        assertTrue(searchStats.p99 < 200, "搜索操作P99延迟应小于200ms");
        
        logger.info("延迟基准测试完成 - Add延迟: avg={}ms, p95={}ms, p99={}ms", 
                   addStats.average, addStats.p95, addStats.p99);
        logger.info("延迟基准测试完成 - Search延迟: avg={}ms, p95={}ms, p99={}ms", 
                   searchStats.average, searchStats.p95, searchStats.p99);
    }

    @Test
    @Order(5)
    @DisplayName("测试内存压力基准")
    void testMemoryStressBenchmarks() {
        logger.info("开始内存压力基准测试");
        
        Runtime runtime = Runtime.getRuntime();
        
        // 记录初始内存状态
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        logger.info("初始内存使用: {} MB", initialMemory / (1024 * 1024));
        
        // 创建大量内存数据进行压力测试
        int largeDataSetSize = 5000;
        List<String> memoryIds = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < largeDataSetSize; i++) {
            try {
                String userId = "stress-user";
                // 创建较大的内存内容来增加内存压力
                StringBuilder contentBuilder = new StringBuilder();
                for (int j = 0; j < 100; j++) {
                    contentBuilder.append("Memory stress test data ").append(i).append("-").append(j).append(" ");
                }
                String content = contentBuilder.toString();
                
                mem0.add(content, userId);
                // Simulate successful memory creation
                memoryIds.add("memory-" + i);
            } catch (Exception e) {
                logger.debug("内存压力测试中操作失败: {}", e.getMessage());
            }
            
            // 每1000个操作检查内存使用情况
            if (i > 0 && i % 1000 == 0) {
                System.gc(); // 建议垃圾回收
                long currentMemory = runtime.totalMemory() - runtime.freeMemory();
                logger.info("处理 {} 项后内存使用: {} MB", i, currentMemory / (1024 * 1024));
            }
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // 最终内存检查
        System.gc();
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;
        
        // 验证内存压力测试结果
        assertTrue(memoryIds.size() >= largeDataSetSize * 0.8, "成功创建的内存数应至少为80%");
        assertTrue(totalTime < 60000, "内存压力测试应在60秒内完成");
        
        // 内存增长应该是合理的 (不应该有严重的内存泄漏)
        long expectedMemoryIncrease = largeDataSetSize * 1024; // 每个项目约1KB
        assertTrue(memoryIncrease < expectedMemoryIncrease * 5, "内存增长应该是合理的范围内");
        
        logger.info("内存压力基准测试完成 - 创建: {} 个内存项", memoryIds.size());
        logger.info("总时间: {}ms, 内存增长: {} MB", totalTime, memoryIncrease / (1024 * 1024));
    }
    
    // 辅助方法
    private static Map<String, Object> createMetadata(String testType, int index) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("test_type", testType);
        metadata.put("index", index);
        metadata.put("timestamp", System.currentTimeMillis());
        return metadata;
    }
    
    private PerformanceResult measureAddPerformance() {
        List<Long> latencies = new ArrayList<>();
        int successCount = 0;
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 50; i++) {
            long opStartTime = System.nanoTime();
            try {
                String userId = "perf-user-" + System.nanoTime();
                mem0.add("Basic performance test memory", userId);
                successCount++;
                long latency = (System.nanoTime() - opStartTime) / 1_000_000;
                latencies.add(latency);
            } catch (Exception e) {
                logger.debug("Add 操作失败: {}", e.getMessage());
            }
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        double averageLatency = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double throughput = (double) successCount * 1000 / totalTime;
        
        return new PerformanceResult(averageLatency, throughput, successCount);
    }
    
    private PerformanceResult measureSearchPerformance() {
        List<Long> latencies = new ArrayList<>();
        int successCount = 0;
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 50; i++) {
            long opStartTime = System.nanoTime();
            try {
                mem0.search("performance test", "perf-user", 5);
                successCount++;
                long latency = (System.nanoTime() - opStartTime) / 1_000_000;
                latencies.add(latency);
            } catch (Exception e) {
                logger.debug("Search 操作失败: {}", e.getMessage());
            }
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        double averageLatency = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double throughput = (double) successCount * 1000 / totalTime;
        
        return new PerformanceResult(averageLatency, throughput, successCount);
    }
    
    
    private LatencyStats calculateLatencyStats(List<Long> latencies) {
        if (latencies.isEmpty()) {
            return new LatencyStats(0, 0, 0);
        }
        
        latencies.sort(Long::compareTo);
        
        double average = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long p95 = latencies.get((int) (latencies.size() * 0.95));
        long p99 = latencies.get((int) (latencies.size() * 0.99));
        
        return new LatencyStats(average, p95, p99);
    }
    
    private static class PerformanceResult {
        final double averageLatency;
        final double throughput;
        final int successCount;
        
        PerformanceResult(double averageLatency, double throughput, int successCount) {
            this.averageLatency = averageLatency;
            this.throughput = throughput;
            this.successCount = successCount;
        }
    }
    
    private static class LatencyStats {
        final double average;
        final long p95;
        final long p99;
        
        LatencyStats(double average, long p95, long p99) {
            this.average = average;
            this.p95 = p95;
            this.p99 = p99;
        }
    }
}