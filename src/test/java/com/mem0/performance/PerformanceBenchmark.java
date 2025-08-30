package com.mem0.benchmark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mem0.memory.ConcurrentMemoryManager;
import com.mem0.pipeline.AsyncMemoryPipeline;
import com.mem0.concurrency.ConcurrencyController;
import com.mem0.monitoring.PerformanceMonitor;
import com.mem0.embedding.EmbeddingProvider;
import com.mem0.store.VectorStore;
import com.mem0.graph.GraphStore;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 性能基准测试套件
 * 提供全面的性能测试，包括并发、吞吐量、延迟和内存使用等指标
 */
public class PerformanceBenchmark {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceBenchmark.class);
    
    // 测试组件
    private final ConcurrentMemoryManager memoryManager;
    private final AsyncMemoryPipeline pipeline;
    private final ConcurrencyController concurrencyController;
    private final PerformanceMonitor performanceMonitor;
    private final EmbeddingProvider embeddingProvider;
    private final VectorStore vectorStore;
    private final GraphStore graphStore;
    
    // 测试配置
    private final BenchmarkConfig config;
    
    // 测试数据生成器
    private final TestDataGenerator dataGenerator;
    
    // 执行器
    private final ExecutorService benchmarkExecutor;
    
    // 测试结果收集
    private final List<BenchmarkResult> results = new CopyOnWriteArrayList<>();

    public PerformanceBenchmark(ConcurrentMemoryManager memoryManager,
                              AsyncMemoryPipeline pipeline,
                              ConcurrencyController concurrencyController,
                              PerformanceMonitor performanceMonitor,
                              EmbeddingProvider embeddingProvider,
                              VectorStore vectorStore,
                              GraphStore graphStore) {
        this(memoryManager, pipeline, concurrencyController, performanceMonitor, 
             embeddingProvider, vectorStore, graphStore, BenchmarkConfig.defaultConfig());
    }

    public PerformanceBenchmark(ConcurrentMemoryManager memoryManager,
                              AsyncMemoryPipeline pipeline,
                              ConcurrencyController concurrencyController,
                              PerformanceMonitor performanceMonitor,
                              EmbeddingProvider embeddingProvider,
                              VectorStore vectorStore,
                              GraphStore graphStore,
                              BenchmarkConfig config) {
        this.memoryManager = memoryManager;
        this.pipeline = pipeline;
        this.concurrencyController = concurrencyController;
        this.performanceMonitor = performanceMonitor;
        this.embeddingProvider = embeddingProvider;
        this.vectorStore = vectorStore;
        this.graphStore = graphStore;
        this.config = config;
        
        this.dataGenerator = new TestDataGenerator();
        
        this.benchmarkExecutor = Executors.newFixedThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors()),
            r -> {
                Thread t = new Thread(r, "benchmark-executor");
                t.setDaemon(false); // 确保测试完成
                return t;
            }
        );
        
        logger.info("性能基准测试初始化完成 - 配置: {}", config);
    }

    /**
     * 运行完整的基准测试套件
     */
    public BenchmarkReport runFullBenchmark() {
        logger.info("开始运行完整基准测试套件");
        long suiteStartTime = System.currentTimeMillis();
        
        List<BenchmarkResult> allResults = new ArrayList<>();
        
        try {
            // 1. 基础性能测试
            logger.info("执行基础性能测试");
            allResults.addAll(runBasicPerformanceTests());
            
            // 2. 并发性能测试
            logger.info("执行并发性能测试");
            allResults.addAll(runConcurrencyTests());
            
            // 3. 吞吐量测试
            logger.info("执行吞吐量测试");
            allResults.addAll(runThroughputTests());
            
            // 4. 延迟测试
            logger.info("执行延迟测试");
            allResults.addAll(runLatencyTests());
            
            // 5. 内存压力测试
            logger.info("执行内存压力测试");
            allResults.addAll(runMemoryStressTests());
            
            // 6. 缓存性能测试
            logger.info("执行缓存性能测试");
            allResults.addAll(runCachePerformanceTests());
            
            // 7. 扩展性测试
            logger.info("执行扩展性测试");
            allResults.addAll(runScalabilityTests());
            
            long suiteDuration = System.currentTimeMillis() - suiteStartTime;
            
            BenchmarkReport report = new BenchmarkReport(allResults, suiteDuration, config);
            logger.info("基准测试套件完成 - 总耗时: {}ms, 测试数: {}", suiteDuration, allResults.size());
            
            return report;
            
        } catch (Exception e) {
            logger.error("基准测试执行失败", e);
            throw new RuntimeException("基准测试失败", e);
        }
    }

    /**
     * 基础性能测试
     */
    public List<BenchmarkResult> runBasicPerformanceTests() {
        List<BenchmarkResult> results = new ArrayList<>();
        
        // 测试单个内存创建性能
        results.add(runSingleOperationTest("单个内存创建", () -> {
            String content = dataGenerator.generateText(100);
            String userId = dataGenerator.generateUserId();
            return memoryManager.createMemory(content, userId, new HashMap<>());
        }));
        
        // 测试内存查询性能
        String testMemoryId = createTestMemory();
        results.add(runSingleOperationTest("单个内存查询", () -> 
            memoryManager.getMemory(testMemoryId)));
        
        // 测试内存更新性能
        results.add(runSingleOperationTest("单个内存更新", () -> {
            String newContent = dataGenerator.generateText(120);
            return memoryManager.updateMemory(testMemoryId, newContent, new HashMap<>());
        }));
        
        // 测试相似性搜索性能
        results.add(runSingleOperationTest("相似性搜索", () -> {
            String query = dataGenerator.generateSearchQuery();
            String userId = dataGenerator.generateUserId();
            return memoryManager.searchSimilarMemories(query, userId, 10, 0.7f);
        }));
        
        return results;
    }

    /**
     * 并发性能测试
     */
    public List<BenchmarkResult> runConcurrencyTests() {
        List<BenchmarkResult> results = new ArrayList<>();
        
        int[] concurrencyLevels = {1, 2, 4, 8, 16, 32, 64};
        
        for (int concurrency : concurrencyLevels) {
            // 并发创建测试
            results.add(runConcurrentTest(
                String.format("并发创建测试(%d线程)", concurrency),
                concurrency,
                config.getOperationsPerThread(),
                () -> {
                    String content = dataGenerator.generateText(100);
                    String userId = dataGenerator.generateUserId();
                    return memoryManager.createMemory(content, userId, new HashMap<>());
                }
            ));
            
            // 并发搜索测试
            results.add(runConcurrentTest(
                String.format("并发搜索测试(%d线程)", concurrency),
                concurrency,
                config.getOperationsPerThread(),
                () -> {
                    String query = dataGenerator.generateSearchQuery();
                    String userId = dataGenerator.generateUserId();
                    return memoryManager.searchSimilarMemories(query, userId, 10, 0.7f);
                }
            ));
        }
        
        return results;
    }

    /**
     * 吞吐量测试
     */
    public List<BenchmarkResult> runThroughputTests() {
        List<BenchmarkResult> results = new ArrayList<>();
        
        // 批量创建吞吐量测试
        int[] batchSizes = {10, 50, 100, 500, 1000};
        
        for (int batchSize : batchSizes) {
            results.add(runThroughputTest(
                String.format("批量创建吞吐量(%d条)", batchSize),
                () -> {
                    List<ConcurrentMemoryManager.MemoryCreationRequest> requests = 
                        dataGenerator.generateCreationRequests(batchSize);
                    return memoryManager.createMemoriesBatch(requests);
                }
            ));
        }
        
        // 持续吞吐量测试
        results.add(runSustainedThroughputTest("持续吞吐量测试", config.getSustainedTestDurationMs()));
        
        return results;
    }

    /**
     * 延迟测试
     */
    public List<BenchmarkResult> runLatencyTests() {
        List<BenchmarkResult> results = new ArrayList<>();
        
        // P99延迟测试
        results.add(runLatencyPercentileTest("创建操作延迟分析", () -> {
            String content = dataGenerator.generateText(100);
            String userId = dataGenerator.generateUserId();
            return memoryManager.createMemory(content, userId, new HashMap<>());
        }, 1000));
        
        results.add(runLatencyPercentileTest("搜索操作延迟分析", () -> {
            String query = dataGenerator.generateSearchQuery();
            String userId = dataGenerator.generateUserId();
            return memoryManager.searchSimilarMemories(query, userId, 10, 0.7f);
        }, 1000));
        
        return results;
    }

    /**
     * 内存压力测试
     */
    public List<BenchmarkResult> runMemoryStressTests() {
        List<BenchmarkResult> results = new ArrayList<>();
        
        // 大量内存创建测试
        results.add(runMemoryStressTest("大量内存创建", 10000));
        
        // 内存泄漏检测
        results.add(runMemoryLeakTest("内存泄漏检测", 1000));
        
        return results;
    }

    /**
     * 缓存性能测试
     */
    public List<BenchmarkResult> runCachePerformanceTests() {
        List<BenchmarkResult> results = new ArrayList<>();
        
        // 缓存命中率测试
        results.add(runCacheHitRateTest("内存缓存命中率测试"));
        
        // 缓存预热测试
        results.add(runCacheWarmupTest("缓存预热性能测试"));
        
        return results;
    }

    /**
     * 扩展性测试
     */
    public List<BenchmarkResult> runScalabilityTests() {
        List<BenchmarkResult> results = new ArrayList<>();
        
        // 用户扩展性测试
        int[] userCounts = {100, 500, 1000, 5000};
        
        for (int userCount : userCounts) {
            results.add(runScalabilityTest(
                String.format("用户扩展性测试(%d用户)", userCount),
                userCount
            ));
        }
        
        return results;
    }

    // 私有测试方法实现

    private BenchmarkResult runSingleOperationTest(String testName, Supplier<CompletableFuture<?>> operation) {
        logger.debug("开始单操作测试: {}", testName);
        
        List<Long> latencies = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        
        int iterations = config.getWarmupIterations() + config.getMeasurementIterations();
        
        for (int i = 0; i < iterations; i++) {
            long opStart = System.nanoTime();
            
            try {
                CompletableFuture<?> future = operation.get();
                future.get(config.getOperationTimeoutMs(), TimeUnit.MILLISECONDS);
                
                long opEnd = System.nanoTime();
                
                // 跳过预热迭代
                if (i >= config.getWarmupIterations()) {
                    latencies.add((opEnd - opStart) / 1_000_000); // 转换为毫秒
                }
                
            } catch (Exception e) {
                logger.warn("操作执行失败: {}", testName, e);
            }
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        
        return new BenchmarkResult(testName, BenchmarkResult.TestType.LATENCY, latencies, totalTime, 1);
    }

    private BenchmarkResult runConcurrentTest(String testName, int concurrency, int operationsPerThread, 
                                            Supplier<CompletableFuture<?>> operation) {
        logger.debug("开始并发测试: {} - 并发度: {}", testName, concurrency);
        
        CountDownLatch latch = new CountDownLatch(concurrency);
        AtomicLong totalOperations = new AtomicLong(0);
        AtomicLong successfulOperations = new AtomicLong(0);
        List<Long> allLatencies = new CopyOnWriteArrayList<>();
        
        long startTime = System.currentTimeMillis();
        
        // 启动并发线程
        for (int i = 0; i < concurrency; i++) {
            benchmarkExecutor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        long opStart = System.nanoTime();
                        
                        try {
                            CompletableFuture<?> future = operation.get();
                            future.get(config.getOperationTimeoutMs(), TimeUnit.MILLISECONDS);
                            
                            long opEnd = System.nanoTime();
                            allLatencies.add((opEnd - opStart) / 1_000_000);
                            successfulOperations.incrementAndGet();
                            
                        } catch (Exception e) {
                            logger.debug("并发操作失败", e);
                        } finally {
                            totalOperations.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 等待所有线程完成
        try {
            latch.await(config.getConcurrentTestTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        
        return new BenchmarkResult(testName, BenchmarkResult.TestType.CONCURRENCY, allLatencies, totalTime, concurrency);
    }

    private BenchmarkResult runThroughputTest(String testName, Supplier<CompletableFuture<?>> operation) {
        logger.debug("开始吞吐量测试: {}", testName);
        
        AtomicLong operationCount = new AtomicLong(0);
        long startTime = System.currentTimeMillis();
        
        try {
            CompletableFuture<?> future = operation.get();
            Object result = future.get(config.getThroughputTestTimeoutMs(), TimeUnit.MILLISECONDS);
            
            if (result instanceof List<?>) {
                operationCount.set(((List<?>) result).size());
            } else {
                operationCount.set(1);
            }
            
        } catch (Exception e) {
            logger.error("吞吐量测试失败: {}", testName, e);
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        
        double throughput = totalTime > 0 ? (operationCount.get() * 1000.0 / totalTime) : 0.0;
        List<Long> metrics = Collections.singletonList((long) throughput);
        
        return new BenchmarkResult(testName, BenchmarkResult.TestType.THROUGHPUT, metrics, totalTime, 1);
    }

    private BenchmarkResult runSustainedThroughputTest(String testName, long durationMs) {
        logger.debug("开始持续吞吐量测试: {} - 持续时间: {}ms", testName, durationMs);
        
        AtomicLong operationCount = new AtomicLong(0);
        AtomicInteger activeThreads = new AtomicInteger(config.getConcurrencyLevel());
        List<Long> throughputSamples = new CopyOnWriteArrayList<>();
        
        long startTime = System.currentTimeMillis();
        long endTime = startTime + durationMs;
        
        // 启动工作线程
        for (int i = 0; i < config.getConcurrencyLevel(); i++) {
            benchmarkExecutor.submit(() -> {
                try {
                    while (System.currentTimeMillis() < endTime) {
                        try {
                            String content = dataGenerator.generateText(100);
                            String userId = dataGenerator.generateUserId();
                            
                            CompletableFuture<String> future = 
                                memoryManager.createMemory(content, userId, new HashMap<>());
                            future.get(config.getOperationTimeoutMs(), TimeUnit.MILLISECONDS);
                            
                            operationCount.incrementAndGet();
                            
                        } catch (Exception e) {
                            logger.debug("持续测试操作失败", e);
                        }
                    }
                } finally {
                    activeThreads.decrementAndGet();
                }
            });
        }
        
        // 采样吞吐量
        ScheduledExecutorService sampler = Executors.newSingleThreadScheduledExecutor();
        sampler.scheduleAtFixedRate(() -> {
            long currentOps = operationCount.get();
            long currentTime = System.currentTimeMillis();
            long elapsed = currentTime - startTime;
            
            if (elapsed > 0) {
                long throughput = currentOps * 1000 / elapsed;
                throughputSamples.add(throughput);
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);
        
        // 等待测试完成
        while (activeThreads.get() > 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        sampler.shutdown();
        long totalTime = System.currentTimeMillis() - startTime;
        
        return new BenchmarkResult(testName, BenchmarkResult.TestType.SUSTAINED_THROUGHPUT, 
                                 throughputSamples, totalTime, config.getConcurrencyLevel());
    }

    private BenchmarkResult runLatencyPercentileTest(String testName, Supplier<CompletableFuture<?>> operation, int sampleCount) {
        logger.debug("开始延迟百分位测试: {} - 样本数: {}", testName, sampleCount);
        
        List<Long> latencies = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < sampleCount; i++) {
            long opStart = System.nanoTime();
            
            try {
                CompletableFuture<?> future = operation.get();
                future.get(config.getOperationTimeoutMs(), TimeUnit.MILLISECONDS);
                
                long opEnd = System.nanoTime();
                latencies.add((opEnd - opStart) / 1_000_000);
                
            } catch (Exception e) {
                logger.debug("延迟测试操作失败", e);
            }
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        
        return new BenchmarkResult(testName, BenchmarkResult.TestType.LATENCY_PERCENTILE, latencies, totalTime, 1);
    }

    private BenchmarkResult runMemoryStressTest(String testName, int memoryCount) {
        logger.debug("开始内存压力测试: {} - 内存数: {}", testName, memoryCount);
        
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        List<Long> memoryUsage = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        
        // 创建大量内存
        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (int i = 0; i < memoryCount; i++) {
            String content = dataGenerator.generateText(200 + i % 300); // 变长内容
            String userId = dataGenerator.generateUserId();
            
            CompletableFuture<String> future = memoryManager.createMemory(content, userId, new HashMap<>());
            futures.add(future);
            
            // 每1000个操作采样一次内存使用
            if (i % 1000 == 0) {
                long currentMemory = runtime.totalMemory() - runtime.freeMemory();
                memoryUsage.add(currentMemory - initialMemory);
            }
        }
        
        // 等待所有操作完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        long totalTime = System.currentTimeMillis() - startTime;
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        memoryUsage.add(finalMemory - initialMemory);
        
        return new BenchmarkResult(testName, BenchmarkResult.TestType.MEMORY_STRESS, memoryUsage, totalTime, 1);
    }

    private BenchmarkResult runMemoryLeakTest(String testName, int cycles) {
        logger.debug("开始内存泄漏检测: {} - 循环数: {}", testName, cycles);
        
        Runtime runtime = Runtime.getRuntime();
        List<Long> memorySnapshots = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        
        for (int cycle = 0; cycle < cycles; cycle++) {
            // 创建一些内存
            List<CompletableFuture<String>> futures = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                String content = dataGenerator.generateText(100);
                String userId = dataGenerator.generateUserId();
                futures.add(memoryManager.createMemory(content, userId, new HashMap<>()));
            }
            
            // 等待完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            // 删除一些内存（模拟清理）
            for (int i = 0; i < 5; i++) {
                try {
                    futures.get(i).thenCompose(memoryId -> memoryManager.deleteMemory(memoryId)).join();
                } catch (Exception e) {
                    // 忽略删除失败
                }
            }
            
            // 强制GC并采样内存
            System.gc();
            try {
                Thread.sleep(100); // 等待GC完成
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            
            long currentMemory = runtime.totalMemory() - runtime.freeMemory();
            memorySnapshots.add(currentMemory);
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        
        return new BenchmarkResult(testName, BenchmarkResult.TestType.MEMORY_LEAK, memorySnapshots, totalTime, 1);
    }

    private BenchmarkResult runCacheHitRateTest(String testName) {
        logger.debug("开始缓存命中率测试: {}", testName);
        
        // 先创建一些测试数据
        List<String> memoryIds = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            String content = dataGenerator.generateText(100);
            String userId = dataGenerator.generateUserId();
            
            try {
                String memoryId = memoryManager.createMemory(content, userId, new HashMap<>()).get();
                memoryIds.add(memoryId);
            } catch (Exception e) {
                logger.debug("创建测试数据失败", e);
            }
        }
        
        // 测试缓存命中率
        long hitCount = 0;
        long totalRequests = 0;
        long startTime = System.currentTimeMillis();
        
        // 多次访问相同的内存以测试缓存
        for (int round = 0; round < 5; round++) {
            for (String memoryId : memoryIds) {
                try {
                    long reqStart = System.nanoTime();
                    memoryManager.getMemory(memoryId).get();
                    long reqEnd = System.nanoTime();
                    
                    // 快速响应可能表明缓存命中
                    if ((reqEnd - reqStart) / 1_000_000 < 10) { // 小于10ms
                        hitCount++;
                    }
                    totalRequests++;
                    
                } catch (Exception e) {
                    logger.debug("缓存测试请求失败", e);
                }
            }
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        double hitRate = totalRequests > 0 ? (double) hitCount / totalRequests : 0.0;
        
        List<Long> metrics = Arrays.asList(hitCount, totalRequests, (long)(hitRate * 100));
        
        return new BenchmarkResult(testName, BenchmarkResult.TestType.CACHE_PERFORMANCE, metrics, totalTime, 1);
    }

    private BenchmarkResult runCacheWarmupTest(String testName) {
        logger.debug("开始缓存预热测试: {}", testName);
        
        List<String> commonQueries = dataGenerator.generateCommonQueries(50);
        long startTime = System.currentTimeMillis();
        
        try {
            pipeline.warmup(commonQueries).get(config.getCacheWarmupTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.error("缓存预热失败", e);
        }
        
        long warmupTime = System.currentTimeMillis() - startTime;
        List<Long> metrics = Collections.singletonList(warmupTime);
        
        return new BenchmarkResult(testName, BenchmarkResult.TestType.CACHE_WARMUP, metrics, warmupTime, 1);
    }

    private BenchmarkResult runScalabilityTest(String testName, int userCount) {
        logger.debug("开始扩展性测试: {} - 用户数: {}", testName, userCount);
        
        List<String> userIds = IntStream.range(0, userCount)
            .mapToObj(i -> "user_" + i)
            .collect(Collectors.toList());
        
        AtomicLong totalOperations = new AtomicLong(0);
        List<Long> responseTimeSamples = new CopyOnWriteArrayList<>();
        long startTime = System.currentTimeMillis();
        
        // 为每个用户创建一些内存
        List<CompletableFuture<Void>> userTasks = userIds.stream()
            .map(userId -> CompletableFuture.runAsync(() -> {
                for (int i = 0; i < 10; i++) { // 每用户10个内存
                    long opStart = System.currentTimeMillis();
                    try {
                        String content = dataGenerator.generateText(100);
                        memoryManager.createMemory(content, userId, new HashMap<>()).get();
                        
                        long opEnd = System.currentTimeMillis();
                        responseTimeSamples.add(opEnd - opStart);
                        totalOperations.incrementAndGet();
                        
                    } catch (Exception e) {
                        logger.debug("扩展性测试操作失败", e);
                    }
                }
            }, benchmarkExecutor))
            .collect(Collectors.toList());
        
        // 等待所有任务完成
        CompletableFuture.allOf(userTasks.toArray(new CompletableFuture[0])).join();
        
        long totalTime = System.currentTimeMillis() - startTime;
        
        return new BenchmarkResult(testName, BenchmarkResult.TestType.SCALABILITY, responseTimeSamples, totalTime, userCount);
    }

    private String createTestMemory() {
        try {
            String content = dataGenerator.generateText(100);
            String userId = dataGenerator.generateUserId();
            return memoryManager.createMemory(content, userId, new HashMap<>()).get();
        } catch (Exception e) {
            logger.error("创建测试内存失败", e);
            return "test_memory_" + System.currentTimeMillis();
        }
    }

    /**
     * 关闭基准测试
     */
    public void shutdown() {
        logger.info("关闭基准测试执行器");
        
        benchmarkExecutor.shutdown();
        try {
            if (!benchmarkExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                benchmarkExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            benchmarkExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}