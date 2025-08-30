package com.mem0.performance;

import com.mem0.Mem0;
import com.mem0.config.Mem0Config;
import com.mem0.core.EnhancedMemory;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

/**
 * 全面性能基准测试
 * 测试各种场景下的性能指标，包括吞吐量、延迟、内存使用等
 */
public class ComprehensivePerformanceBenchmark {
    
    private static final Logger logger = LoggerFactory.getLogger(ComprehensivePerformanceBenchmark.class);
    
    private Mem0 mem0;
    private ExecutorService executorService;
    private PerformanceMetrics metrics;
    
    @Before
    public void setUp() {
        logger.info("初始化性能基准测试环境");
        
        Mem0Config config = new Mem0Config();
        config.getVectorStore().setProvider("inmemory");
        config.getGraphStore().setProvider("inmemory");
        config.getEmbedding().setProvider("tfidf");
        config.getLlm().setProvider("rulebased");
            
        mem0 = new Mem0(config);
        executorService = Executors.newFixedThreadPool(50);
        metrics = new PerformanceMetrics();
        
        // JVM预热
        warmupJVM();
        
        logger.info("性能测试环境初始化完成");
    }
    
    @After
    public void tearDown() throws Exception {
        if (executorService != null) {
            executorService.shutdown();
            executorService.awaitTermination(60, TimeUnit.SECONDS);
        }
        if (mem0 != null) {
            mem0.close();
        }
        
        // 输出最终性能报告
        metrics.printFinalReport();
        
        logger.info("性能测试环境已关闭");
    }
    
    private void warmupJVM() {
        logger.info("JVM预热开始...");
        
        try {
            String userId = "warmup_user";
            
            // 预热操作
            for (int i = 0; i < 100; i++) {
                String content = "预热记忆 " + i;
                mem0.add(content, userId).get();
            }
            
            for (int i = 0; i < 50; i++) {
                mem0.search("预热", userId).get();
            }
            
            // 清理预热数据
            mem0.getAll(userId).get().forEach(memory -> {
                try {
                    mem0.delete(memory.getId()).get();
                } catch (Exception e) {
                    // 忽略预热清理错误
                }
            });
            
            logger.info("JVM预热完成");
        } catch (Exception e) {
            logger.warn("JVM预热过程出现异常", e);
        }
    }
    
    @Test
    public void benchmarkSequentialOperations() throws Exception {
        logger.info("开始顺序操作性能基准测试");
        
        String userId = "sequential_user";
        int numOperations = 1000;
        
        // 测试顺序添加性能
        PerformanceTimer addTimer = new PerformanceTimer("Sequential Add");
        
        List<String> memoryIds = new ArrayList<>();
        addTimer.start();
        
        for (int i = 0; i < numOperations; i++) {
            String content = generateTestContent(i);
            CompletableFuture<String> future = mem0.add(content, userId);
            String memoryId = future.get(30, TimeUnit.SECONDS);
            memoryIds.add(memoryId);
        }
        
        long addDuration = addTimer.stop();
        double addThroughput = numOperations * 1000.0 / addDuration;
        
        metrics.recordMetric("Sequential Add Throughput", addThroughput, "ops/sec");
        metrics.recordMetric("Sequential Add Avg Latency", addDuration / (double) numOperations, "ms");
        
        logger.info("顺序添加: {} ops, {} ms, {:.2f} ops/sec", 
                   numOperations, addDuration, addThroughput);
        
        // 测试顺序搜索性能
        PerformanceTimer searchTimer = new PerformanceTimer("Sequential Search");
        
        String[] searchQueries = {"测试", "性能", "基准", "数据", "内容"};
        int totalSearches = 500;
        
        searchTimer.start();
        
        for (int i = 0; i < totalSearches; i++) {
            String query = searchQueries[i % searchQueries.length];
            CompletableFuture<List<EnhancedMemory>> future = mem0.search(query, userId);
            List<EnhancedMemory> results = future.get(30, TimeUnit.SECONDS);
            
            // 验证搜索结果合理性
            assertTrue("搜索结果数量应该合理", results.size() <= numOperations);
        }
        
        long searchDuration = searchTimer.stop();
        double searchThroughput = totalSearches * 1000.0 / searchDuration;
        
        metrics.recordMetric("Sequential Search Throughput", searchThroughput, "ops/sec");
        metrics.recordMetric("Sequential Search Avg Latency", searchDuration / (double) totalSearches, "ms");
        
        logger.info("顺序搜索: {} ops, {} ms, {:.2f} ops/sec", 
                   totalSearches, searchDuration, searchThroughput);
        
        // 测试顺序更新性能
        PerformanceTimer updateTimer = new PerformanceTimer("Sequential Update");
        
        int numUpdates = Math.min(200, memoryIds.size()); // 更新前200个记忆
        updateTimer.start();
        
        for (int i = 0; i < numUpdates; i++) {
            String memoryId = memoryIds.get(i);
            String newContent = "更新后的内容 " + i + " - " + System.currentTimeMillis();
            CompletableFuture<EnhancedMemory> future = mem0.update(memoryId, newContent);
            future.get(30, TimeUnit.SECONDS);
        }
        
        long updateDuration = updateTimer.stop();
        double updateThroughput = numUpdates * 1000.0 / updateDuration;
        
        metrics.recordMetric("Sequential Update Throughput", updateThroughput, "ops/sec");
        metrics.recordMetric("Sequential Update Avg Latency", updateDuration / (double) numUpdates, "ms");
        
        logger.info("顺序更新: {} ops, {} ms, {:.2f} ops/sec", 
                   numUpdates, updateDuration, updateThroughput);
        
        // 测试顺序删除性能
        PerformanceTimer deleteTimer = new PerformanceTimer("Sequential Delete");
        
        int numDeletes = Math.min(100, memoryIds.size()); // 删除前100个记忆
        deleteTimer.start();
        
        for (int i = 0; i < numDeletes; i++) {
            String memoryId = memoryIds.get(i);
            CompletableFuture<Void> future = mem0.delete(memoryId);
            future.get(30, TimeUnit.SECONDS);
        }
        
        long deleteDuration = deleteTimer.stop();
        double deleteThroughput = numDeletes * 1000.0 / deleteDuration;
        
        metrics.recordMetric("Sequential Delete Throughput", deleteThroughput, "ops/sec");
        metrics.recordMetric("Sequential Delete Avg Latency", deleteDuration / (double) numDeletes, "ms");
        
        logger.info("顺序删除: {} ops, {} ms, {:.2f} ops/sec", 
                   numDeletes, deleteDuration, deleteThroughput);
    }
    
    @Test
    public void benchmarkConcurrentOperations() throws Exception {
        logger.info("开始并发操作性能基准测试");
        
        String baseUserId = "concurrent_user_";
        int numThreads = 20;
        int operationsPerThread = 100;
        int totalOperations = numThreads * operationsPerThread;
        
        // 并发添加测试
        PerformanceTimer concurrentAddTimer = new PerformanceTimer("Concurrent Add");
        CountDownLatch addLatch = new CountDownLatch(numThreads);
        AtomicLong addErrors = new AtomicLong(0);
        
        concurrentAddTimer.start();
        
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executorService.submit(() -> {
                try {
                    String userId = baseUserId + threadId;
                    
                    for (int i = 0; i < operationsPerThread; i++) {
                        String content = String.format("并发记忆 T%d-I%d: %s", 
                                                     threadId, i, generateTestContent(i));
                        CompletableFuture<String> future = mem0.add(content, userId);
                        future.get(30, TimeUnit.SECONDS);
                    }
                    
                } catch (Exception e) {
                    addErrors.incrementAndGet();
                    logger.error("并发添加失败 线程{}", threadId, e);
                } finally {
                    addLatch.countDown();
                }
            });
        }
        
        addLatch.await();
        long concurrentAddDuration = concurrentAddTimer.stop();
        double concurrentAddThroughput = totalOperations * 1000.0 / concurrentAddDuration;
        
        metrics.recordMetric("Concurrent Add Throughput", concurrentAddThroughput, "ops/sec");
        metrics.recordMetric("Concurrent Add Error Rate", addErrors.get() * 100.0 / numThreads, "%");
        
        logger.info("并发添加: {} ops, {} threads, {} ms, {:.2f} ops/sec, {} errors", 
                   totalOperations, numThreads, concurrentAddDuration, 
                   concurrentAddThroughput, addErrors.get());
        
        // 并发搜索测试
        PerformanceTimer concurrentSearchTimer = new PerformanceTimer("Concurrent Search");
        CountDownLatch searchLatch = new CountDownLatch(numThreads);
        AtomicLong searchErrors = new AtomicLong(0);
        AtomicInteger totalResults = new AtomicInteger(0);
        
        String[] queries = {"并发记忆", "测试", "性能", "基准", "数据"};
        
        concurrentSearchTimer.start();
        
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executorService.submit(() -> {
                try {
                    String userId = baseUserId + threadId;
                    
                    for (int i = 0; i < 50; i++) { // 每个线程50次搜索
                        String query = queries[i % queries.length];
                        CompletableFuture<List<EnhancedMemory>> future = mem0.search(query, userId);
                        List<EnhancedMemory> results = future.get(30, TimeUnit.SECONDS);
                        totalResults.addAndGet(results.size());
                    }
                    
                } catch (Exception e) {
                    searchErrors.incrementAndGet();
                    logger.error("并发搜索失败 线程{}", threadId, e);
                } finally {
                    searchLatch.countDown();
                }
            });
        }
        
        searchLatch.await();
        long concurrentSearchDuration = concurrentSearchTimer.stop();
        int totalSearchOps = numThreads * 50;
        double concurrentSearchThroughput = totalSearchOps * 1000.0 / concurrentSearchDuration;
        
        metrics.recordMetric("Concurrent Search Throughput", concurrentSearchThroughput, "ops/sec");
        metrics.recordMetric("Concurrent Search Error Rate", searchErrors.get() * 100.0 / numThreads, "%");
        metrics.recordMetric("Avg Results Per Search", totalResults.get() / (double) totalSearchOps, "results");
        
        logger.info("并发搜索: {} ops, {} threads, {} ms, {:.2f} ops/sec, {} errors, avg {} results/search", 
                   totalSearchOps, numThreads, concurrentSearchDuration,
                   concurrentSearchThroughput, searchErrors.get(),
                   totalResults.get() / (double) totalSearchOps);
    }
    
    @Test
    public void benchmarkLatencyDistribution() throws Exception {
        logger.info("开始延迟分布性能基准测试");
        
        String userId = "latency_user";
        int numSamples = 1000;
        
        // 添加操作延迟分布测试
        List<Long> addLatencies = new ArrayList<>();
        
        for (int i = 0; i < numSamples; i++) {
            String content = generateTestContent(i);
            
            long startTime = System.nanoTime();
            CompletableFuture<String> future = mem0.add(content, userId);
            future.get();
            long endTime = System.nanoTime();
            
            long latencyNs = endTime - startTime;
            addLatencies.add(latencyNs / 1_000_000); // 转换为毫秒
        }
        
        LatencyStats addStats = calculateLatencyStats(addLatencies);
        
        metrics.recordMetric("Add P50 Latency", addStats.p50, "ms");
        metrics.recordMetric("Add P95 Latency", addStats.p95, "ms");
        metrics.recordMetric("Add P99 Latency", addStats.p99, "ms");
        metrics.recordMetric("Add Max Latency", addStats.max, "ms");
        
        logger.info("添加操作延迟统计: P50={:.2f}ms, P95={:.2f}ms, P99={:.2f}ms, Max={:.2f}ms", 
                   addStats.p50, addStats.p95, addStats.p99, addStats.max);
        
        // 搜索操作延迟分布测试
        List<Long> searchLatencies = new ArrayList<>();
        String[] searchTerms = {"测试", "性能", "基准", "数据", "内容"};
        
        for (int i = 0; i < numSamples; i++) {
            String query = searchTerms[i % searchTerms.length];
            
            long startTime = System.nanoTime();
            CompletableFuture<List<EnhancedMemory>> future = mem0.search(query, userId);
            future.get();
            long endTime = System.nanoTime();
            
            long latencyNs = endTime - startTime;
            searchLatencies.add(latencyNs / 1_000_000); // 转换为毫秒
        }
        
        LatencyStats searchStats = calculateLatencyStats(searchLatencies);
        
        metrics.recordMetric("Search P50 Latency", searchStats.p50, "ms");
        metrics.recordMetric("Search P95 Latency", searchStats.p95, "ms");
        metrics.recordMetric("Search P99 Latency", searchStats.p99, "ms");
        metrics.recordMetric("Search Max Latency", searchStats.max, "ms");
        
        logger.info("搜索操作延迟统计: P50={:.2f}ms, P95={:.2f}ms, P99={:.2f}ms, Max={:.2f}ms", 
                   searchStats.p50, searchStats.p95, searchStats.p99, searchStats.max);
    }
    
    @Test
    public void benchmarkMemoryUsage() throws Exception {
        logger.info("开始内存使用性能基准测试");
        
        Runtime runtime = Runtime.getRuntime();
        
        // 记录初始内存使用
        System.gc(); // 建议进行垃圾回收
        Thread.sleep(100); // 等待GC完成
        
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        logger.info("初始内存使用: {} MB", initialMemory / 1_048_576);
        
        String userId = "memory_test_user";
        int numMemories = 10000;
        
        // 添加大量记忆并监控内存使用
        List<String> memoryIds = new ArrayList<>();
        
        for (int i = 0; i < numMemories; i++) {
            String content = generateLargeTestContent(i); // 生成较大的测试内容
            CompletableFuture<String> future = mem0.add(content, userId);
            String memoryId = future.get();
            memoryIds.add(memoryId);
            
            // 每1000个记忆记录一次内存使用
            if (i % 1000 == 0) {
                long currentMemory = runtime.totalMemory() - runtime.freeMemory();
                long memoryIncrease = currentMemory - initialMemory;
                
                logger.info("添加 {} 个记忆后，内存增加: {} MB", 
                           i, memoryIncrease / 1_048_576);
            }
        }
        
        // 最终内存使用
        System.gc();
        Thread.sleep(100);
        
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long totalMemoryIncrease = finalMemory - initialMemory;
        double memoryPerItem = totalMemoryIncrease / (double) numMemories;
        
        metrics.recordMetric("Total Memory Increase", totalMemoryIncrease / 1_048_576.0, "MB");
        metrics.recordMetric("Memory Per Item", memoryPerItem, "bytes");
        
        logger.info("内存使用测试完成: 总增加 {} MB, 平均 {:.2f} bytes/item", 
                   totalMemoryIncrease / 1_048_576, memoryPerItem);
        
        // 测试内存释放
        int itemsToDelete = numMemories / 2;
        
        for (int i = 0; i < itemsToDelete; i++) {
            String memoryId = memoryIds.get(i);
            mem0.delete(memoryId).get();
        }
        
        System.gc();
        Thread.sleep(200);
        
        long afterDeleteMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryReclaimed = finalMemory - afterDeleteMemory;
        
        metrics.recordMetric("Memory Reclaimed", memoryReclaimed / 1_048_576.0, "MB");
        
        logger.info("删除 {} 个记忆后，回收内存: {} MB", 
                   itemsToDelete, memoryReclaimed / 1_048_576);
    }
    
    @Test 
    public void benchmarkScalabilityWithDataSize() throws Exception {
        logger.info("开始数据规模可扩展性性能基准测试");
        
        String userId = "scalability_user";
        int[] dataSizes = {100, 500, 1000, 5000, 10000};
        
        for (int dataSize : dataSizes) {
            logger.info("测试数据规模: {} 条记忆", dataSize);
            
            // 准备测试数据
            List<String> memoryIds = new ArrayList<>();
            long setupStart = System.currentTimeMillis();
            
            for (int i = 0; i < dataSize; i++) {
                String content = generateTestContent(i);
                CompletableFuture<String> future = mem0.add(content, userId);
                String memoryId = future.get();
                memoryIds.add(memoryId);
            }
            
            long setupDuration = System.currentTimeMillis() - setupStart;
            logger.info("数据准备完成: {} 条记忆, {} ms", dataSize, setupDuration);
            
            // 测试搜索性能随数据规模的变化
            String[] queries = {"测试", "性能", "基准", "数据"};
            int searchRounds = 100;
            
            long searchStart = System.currentTimeMillis();
            
            for (int i = 0; i < searchRounds; i++) {
                String query = queries[i % queries.length];
                CompletableFuture<List<EnhancedMemory>> future = mem0.search(query, userId);
                List<EnhancedMemory> results = future.get();
            }
            
            long searchDuration = System.currentTimeMillis() - searchStart;
            double avgSearchTime = searchDuration / (double) searchRounds;
            
            metrics.recordMetric("Search Time for " + dataSize + " items", avgSearchTime, "ms");
            
            logger.info("搜索性能测试: 数据规模={}, 平均搜索时间={:.2f}ms", 
                       dataSize, avgSearchTime);
            
            // 测试获取所有记忆的性能
            long getAllStart = System.currentTimeMillis();
            CompletableFuture<List<EnhancedMemory>> getAllFuture = mem0.getAll(userId);
            List<EnhancedMemory> allMemories = getAllFuture.get();
            long getAllDuration = System.currentTimeMillis() - getAllStart;
            
            metrics.recordMetric("GetAll Time for " + dataSize + " items", getAllDuration, "ms");
            
            logger.info("获取全部记忆: 数据规模={}, 时间={}ms, 返回数量={}", 
                       dataSize, getAllDuration, allMemories.size());
            
            // 清理数据，为下一轮测试做准备
            for (String memoryId : memoryIds) {
                mem0.delete(memoryId).get();
            }
        }
    }
    
    @Test
    public void benchmarkCachePerformance() throws Exception {
        logger.info("开始缓存性能基准测试");
        
        String userId = "cache_test_user";
        int numMemories = 1000;
        
        // 添加测试数据
        List<String> memoryIds = new ArrayList<>();
        for (int i = 0; i < numMemories; i++) {
            String content = generateTestContent(i);
            CompletableFuture<String> future = mem0.add(content, userId);
            memoryIds.add(future.get());
        }
        
        logger.info("缓存测试数据准备完成: {} 条记忆", numMemories);
        
        // 第一轮搜索（缓存未命中）
        String[] queries = {"测试", "性能", "基准", "数据", "内容"};
        int searchRounds = 200;
        
        List<Long> coldSearchTimes = new ArrayList<>();
        
        for (int i = 0; i < searchRounds; i++) {
            String query = queries[i % queries.length];
            
            long startTime = System.nanoTime();
            CompletableFuture<List<EnhancedMemory>> future = mem0.search(query, userId);
            future.get();
            long endTime = System.nanoTime();
            
            coldSearchTimes.add((endTime - startTime) / 1_000_000); // 转为毫秒
        }
        
        LatencyStats coldStats = calculateLatencyStats(coldSearchTimes);
        
        // 第二轮搜索（缓存命中）
        List<Long> warmSearchTimes = new ArrayList<>();
        
        for (int i = 0; i < searchRounds; i++) {
            String query = queries[i % queries.length];
            
            long startTime = System.nanoTime();
            CompletableFuture<List<EnhancedMemory>> future = mem0.search(query, userId);
            future.get();
            long endTime = System.nanoTime();
            
            warmSearchTimes.add((endTime - startTime) / 1_000_000); // 转为毫秒
        }
        
        LatencyStats warmStats = calculateLatencyStats(warmSearchTimes);
        
        // 计算缓存改善效果
        double cacheImprovement = (coldStats.p50 - warmStats.p50) / coldStats.p50 * 100;
        
        metrics.recordMetric("Cold Search P50", coldStats.p50, "ms");
        metrics.recordMetric("Warm Search P50", warmStats.p50, "ms");
        metrics.recordMetric("Cache Improvement", cacheImprovement, "%");
        
        logger.info("缓存性能测试结果:");
        logger.info("  冷搜索 P50: {:.2f}ms, P95: {:.2f}ms", coldStats.p50, coldStats.p95);
        logger.info("  热搜索 P50: {:.2f}ms, P95: {:.2f}ms", warmStats.p50, warmStats.p95);
        logger.info("  缓存改善: {:.1f}%", cacheImprovement);
    }
    
    // 辅助方法
    
    private String generateTestContent(int index) {
        String[] topics = {"测试", "性能", "基准", "数据", "内容", "系统", "功能", "优化"};
        String[] adjectives = {"高效的", "稳定的", "可靠的", "快速的", "智能的", "先进的", "创新的", "实用的"};
        String[] nouns = {"解决方案", "算法", "架构", "框架", "平台", "工具", "方法", "技术"};
        
        return String.format("%s%s%s记录%d - 时间戳: %d", 
                           adjectives[index % adjectives.length],
                           topics[index % topics.length],
                           nouns[index % nouns.length],
                           index,
                           System.currentTimeMillis());
    }
    
    private String generateLargeTestContent(int index) {
        StringBuilder content = new StringBuilder();
        content.append("大型测试记忆条目 ").append(index).append(": ");
        
        // 生成约1KB的内容
        for (int i = 0; i < 10; i++) {
            content.append("这是一段较长的测试内容，用于测试内存使用情况和性能表现。");
            content.append("内容包含各种字符和信息，模拟真实的使用场景。");
            content.append("序号: ").append(index).append("-").append(i).append(" ");
        }
        
        return content.toString();
    }
    
    private LatencyStats calculateLatencyStats(List<Long> latencies) {
        Collections.sort(latencies);
        
        int size = latencies.size();
        return new LatencyStats(
            latencies.get(size / 2).doubleValue(),              // P50
            latencies.get((int)(size * 0.95)).doubleValue(),    // P95
            latencies.get((int)(size * 0.99)).doubleValue(),    // P99
            latencies.get(size - 1).doubleValue()               // Max
        );
    }
    
    // 内部类
    
    private static class PerformanceTimer {
        private final String name;
        private long startTime;
        
        public PerformanceTimer(String name) {
            this.name = name;
        }
        
        public void start() {
            startTime = System.currentTimeMillis();
        }
        
        public long stop() {
            return System.currentTimeMillis() - startTime;
        }
    }
    
    private static class LatencyStats {
        public final double p50;
        public final double p95;
        public final double p99;
        public final double max;
        
        public LatencyStats(double p50, double p95, double p99, double max) {
            this.p50 = p50;
            this.p95 = p95;
            this.p99 = p99;
            this.max = max;
        }
    }
    
    private static class PerformanceMetrics {
        private final Map<String, Double> metrics = new LinkedHashMap<>();
        private final Map<String, String> units = new LinkedHashMap<>();
        
        public void recordMetric(String name, double value, String unit) {
            metrics.put(name, value);
            units.put(name, unit);
        }
        
        public void printFinalReport() {
            logger.info("=== 性能基准测试最终报告 ===");
            
            for (Map.Entry<String, Double> entry : metrics.entrySet()) {
                String name = entry.getKey();
                double value = entry.getValue();
                String unit = units.get(name);
                
                logger.info("{}: {:.2f} {}", name, value, unit);
            }
            
            logger.info("=== 报告结束 ===");
        }
    }
}