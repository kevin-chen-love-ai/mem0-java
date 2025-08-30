package com.mem0.performance;

import com.mem0.Mem0;
import com.mem0.config.Mem0Config;
import com.mem0.core.EnhancedMemory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 简单性能基准测试
 * 验证系统在不同负载下的性能表现
 */
public class SimplePerformanceBenchmark {
    
    private static final Logger logger = LoggerFactory.getLogger(SimplePerformanceBenchmark.class);
    
    private Mem0 mem0;
    
    @Before
    public void setUp() {
        logger.info("初始化性能测试环境");
        
        Mem0Config config = new Mem0Config();
        config.getVectorStore().setProvider("inmemory");
        config.getGraphStore().setProvider("inmemory");
        config.getEmbedding().setProvider("tfidf");
        config.getLlm().setProvider("rulebased");
            
        mem0 = new Mem0(config);
        logger.info("Mem0 实例初始化完成");
    }
    
    @After
    public void tearDown() throws Exception {
        if (mem0 != null) {
            mem0.close();
            logger.info("Mem0 实例已关闭");
        }
    }
    
    @Test
    public void benchmarkSequentialOperations() throws Exception {
        logger.info("开始顺序操作性能测试");
        
        String userId = "benchmark-user";
        int numOperations = 1000;
        
        // 测试顺序添加性能
        long startTime = System.currentTimeMillis();
        List<String> memoryIds = new ArrayList<>();
        
        for (int i = 0; i < numOperations; i++) {
            String content = "基准测试内存 " + i + " - 这是用于性能测试的示例内容";
            CompletableFuture<String> future = mem0.add(content, userId);
            String memoryId = future.get();
            memoryIds.add(memoryId);
            
            if (i % 100 == 0) {
                logger.info("已完成 {} 个添加操作", i);
            }
        }
        
        long addTime = System.currentTimeMillis() - startTime;
        double avgAddTime = addTime / (double) numOperations;
        
        logger.info("顺序添加 {} 个内存耗时 {} ms，平均 {:.2f} ms/个", 
                    numOperations, addTime, avgAddTime);
        
        // 测试顺序搜索性能
        startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 100; i++) {
            CompletableFuture<List<EnhancedMemory>> future = mem0.search("基准测试", userId);
            List<EnhancedMemory> results = future.get();
            
            if (i % 20 == 0) {
                logger.info("搜索 {} - 找到 {} 个结果", i, results.size());
            }
        }
        
        long searchTime = System.currentTimeMillis() - startTime;
        double avgSearchTime = searchTime / 100.0;
        
        logger.info("100 次搜索耗时 {} ms，平均 {:.2f} ms/次", searchTime, avgSearchTime);
        
        // 输出性能报告
        logger.info("=== 顺序操作性能报告 ===");
        logger.info("添加操作吞吐量: {:.2f} ops/sec", 1000.0 / avgAddTime);
        logger.info("搜索操作吞吐量: {:.2f} ops/sec", 1000.0 / avgSearchTime);
    }
    
    @Test
    public void benchmarkConcurrentOperations() throws Exception {
        logger.info("开始并发操作性能测试");
        
        String userId = "concurrent-user";
        int numThreads = 10;
        int operationsPerThread = 100;
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        long startTime = System.currentTimeMillis();
        
        // 启动并发添加任务
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        String content = String.format("并发测试内存 T%d-I%d - 线程%d的第%d个操作", 
                                                       threadId, i, threadId, i);
                        CompletableFuture<String> future = mem0.add(content, userId);
                        future.get();
                    }
                    logger.info("线程 {} 完成所有添加操作", threadId);
                } catch (Exception e) {
                    logger.error("线程 {} 执行出错", threadId, e);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 等待所有任务完成
        latch.await();
        long totalTime = System.currentTimeMillis() - startTime;
        int totalOperations = numThreads * operationsPerThread;
        
        logger.info("并发添加 {} 个内存（{}线程x{}操作）耗时 {} ms", 
                    totalOperations, numThreads, operationsPerThread, totalTime);
        
        double throughput = totalOperations * 1000.0 / totalTime;
        logger.info("并发添加吞吐量: {:.2f} ops/sec", throughput);
        
        // 测试并发搜索
        CountDownLatch searchLatch = new CountDownLatch(numThreads);
        startTime = System.currentTimeMillis();
        
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < 50; i++) {
                        CompletableFuture<List<EnhancedMemory>> future = mem0.search("并发测试", userId);
                        List<EnhancedMemory> results = future.get();
                        
                        if (i % 10 == 0) {
                            logger.debug("线程 {} 搜索 {} - 找到 {} 个结果", threadId, i, results.size());
                        }
                    }
                } catch (Exception e) {
                    logger.error("线程 {} 搜索出错", threadId, e);
                } finally {
                    searchLatch.countDown();
                }
            });
        }
        
        searchLatch.await();
        long searchTotalTime = System.currentTimeMillis() - startTime;
        int totalSearches = numThreads * 50;
        
        double searchThroughput = totalSearches * 1000.0 / searchTotalTime;
        logger.info("并发搜索吞吐量: {:.2f} ops/sec", searchThroughput);
        
        executor.shutdown();
        
        // 输出并发性能报告
        logger.info("=== 并发操作性能报告 ===");
        logger.info("并发添加吞吐量: {:.2f} ops/sec", throughput);
        logger.info("并发搜索吞吐量: {:.2f} ops/sec", searchThroughput);
    }
    
    @Test
    public void benchmarkMemoryRetrieval() throws Exception {
        logger.info("开始内存检索性能测试");
        
        String userId = "retrieval-user";
        int numMemories = 5000;
        
        // 预先添加大量内存
        logger.info("预先添加 {} 个内存...", numMemories);
        List<String> memoryIds = new ArrayList<>();
        
        for (int i = 0; i < numMemories; i++) {
            String content = generateTestContent(i);
            CompletableFuture<String> future = mem0.add(content, userId);
            String memoryId = future.get();
            memoryIds.add(memoryId);
            
            if (i % 1000 == 0) {
                logger.info("已添加 {} 个内存", i);
            }
        }
        
        logger.info("内存添加完成，开始检索测试");
        
        // 测试不同查询的响应时间
        String[] queries = {
            "编程", "音乐", "旅行", "美食", "运动",
            "学习", "工作", "电影", "阅读", "朋友"
        };
        
        for (String query : queries) {
            long startTime = System.currentTimeMillis();
            
            CompletableFuture<List<EnhancedMemory>> future = mem0.search(query, userId);
            List<EnhancedMemory> results = future.get();
            
            long queryTime = System.currentTimeMillis() - startTime;
            
            logger.info("查询 '{}' - 耗时 {} ms，找到 {} 个结果", 
                        query, queryTime, results.size());
        }
        
        // 测试获取用户所有内存的性能
        long startTime = System.currentTimeMillis();
        CompletableFuture<List<EnhancedMemory>> future = mem0.getAll(userId);
        List<EnhancedMemory> allMemories = future.get();
        long getAllTime = System.currentTimeMillis() - startTime;
        
        logger.info("获取用户所有内存（{}个）耗时 {} ms", allMemories.size(), getAllTime);
        
        logger.info("内存检索性能测试完成");
    }
    
    private String generateTestContent(int index) {
        String[] topics = {"编程", "音乐", "旅行", "美食", "运动", "学习", "工作", "电影", "阅读", "朋友"};
        String[] actions = {"喜欢", "学习", "体验", "分享", "讨论", "探索", "享受", "参与", "观察", "创作"};
        String[] adjectives = {"有趣的", "重要的", "难忘的", "愉快的", "挑战性的", "创新的", "传统的", "现代的", "实用的", "理想的"};
        
        String topic = topics[index % topics.length];
        String action = actions[(index / topics.length) % actions.length];
        String adjective = adjectives[(index / (topics.length * actions.length)) % adjectives.length];
        
        return String.format("用户%s%s的%s%s经历，这是第%d个测试内存记录", 
                             action, adjective, topic, 
                             index % 2 == 0 ? "相关" : "领域", index);
    }
}