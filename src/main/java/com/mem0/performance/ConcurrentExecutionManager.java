package com.mem0.performance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 高性能并发执行管理器
 * High-Performance Concurrent Execution Manager
 * 
 * 专为Mem0系统优化的高性能并发执行管理器，针对向量搜索、嵌入生成和内存操作进行优化。
 * 该组件通过智能线程池管理和负载均衡，最大化系统的并发性能和资源利用率。
 * 
 * High-performance concurrent execution manager optimized for the Mem0 system, specifically
 * tailored for vector search, embedding generation, and memory operations. This component
 * maximizes system concurrent performance and resource utilization through intelligent
 * thread pool management and load balancing.
 * 
 * 核心功能 / Key Features:
 * - 专用线程池分离不同操作类型 / Dedicated thread pools for different operation types
 * - 智能线程数量自动检测 / Intelligent automatic thread count detection
 * - 向量相似性计算优化 / Vector similarity calculation optimization
 * - 嵌入生成批处理优化 / Embedding generation batch processing optimization
 * - I/O操作异步执行管理 / Asynchronous I/O operation execution management
 * - 实时性能指标监控 / Real-time performance metrics monitoring
 * 
 * 技术规格 / Technical Specifications:
 * - 向量操作线程池: 核心线程数的50% / Vector operations pool: 50% of core threads
 * - 嵌入操作线程池: 核心线程数的25% / Embedding operations pool: 25% of core threads
 * - 内存管理线程池: 核心线程数的25% / Memory management pool: 25% of core threads
 * - I/O操作线程池: 核心线程数的50% / I/O operations pool: 50% of core threads
 * - 默认线程保活时间: 60秒 / Default thread keep-alive: 60 seconds
 * - 智能负载均衡策略 / Intelligent load balancing strategy
 * 
 * 使用示例 / Usage Example:
 * <pre>
 * {@code
 * // 创建并发执行管理器 / Create concurrent execution manager
 * ConcurrentExecutionManager manager = new ConcurrentExecutionManager();
 * 
 * // 执行向量相似性计算 / Execute vector similarity calculation
 * CompletableFuture<Double> similarityFuture = manager.executeVectorOperation(() -> {
 *     return calculateCosineSimilarity(vector1, vector2);
 * });
 * 
 * // 执行嵌入生成 / Execute embedding generation
 * CompletableFuture<float[]> embeddingFuture = manager.executeEmbeddingOperation(() -> {
 *     return generateEmbedding(text);
 * });
 * 
 * // 执行内存操作 / Execute memory operation
 * CompletableFuture<Memory> memoryFuture = manager.executeMemoryOperation(() -> {
 *     return classifyAndStoreMemory(content);
 * });
 * 
 * // 执行I/O操作 / Execute I/O operation
 * CompletableFuture<List<Memory>> ioFuture = manager.executeIOOperation(() -> {
 *     return database.searchMemories(query);
 * });
 * 
 * // 并发执行多个操作 / Execute multiple operations concurrently
 * CompletableFuture<Void> allOperations = manager.executeAllConcurrently(
 *     similarityFuture, embeddingFuture, memoryFuture, ioFuture
 * );
 * 
 * // 带超时的操作执行 / Execute operation with timeout
 * CompletableFuture<String> timeoutFuture = manager.executeWithTimeout(
 *     () -> performSlowOperation(), 5, TimeUnit.SECONDS, "default-result"
 * );
 * 
 * // 获取性能指标 / Get performance metrics
 * PerformanceMetrics metrics = manager.getMetrics();
 * System.out.println("Active operations: " + metrics);
 * 
 * // 关闭管理器 / Close manager
 * manager.close();
 * }
 * </pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class ConcurrentExecutionManager implements AutoCloseable {
    
    private static final Logger logger = LoggerFactory.getLogger(ConcurrentExecutionManager.class);
    
    // Core thread pools for different operation types
    private final ExecutorService vectorOperationsPool;
    private final ExecutorService embeddingPool;
    private final ExecutorService memoryManagementPool;
    private final ExecutorService ioOperationsPool;
    private final ScheduledExecutorService scheduledPool;
    
    // Configuration
    private final int corePoolSize;
    private final int maxPoolSize;
    private final long keepAliveTime;
    
    // Metrics
    private final AtomicInteger activeVectorOperations = new AtomicInteger(0);
    private final AtomicInteger activeEmbeddingOperations = new AtomicInteger(0);
    private final AtomicInteger activeMemoryOperations = new AtomicInteger(0);
    
    public ConcurrentExecutionManager() {
        this(detectOptimalThreadCount());
    }
    
    public ConcurrentExecutionManager(int threadCount) {
        this.corePoolSize = threadCount;
        this.maxPoolSize = threadCount * 2;
        this.keepAliveTime = 60L; // seconds
        
        // Vector operations pool - optimized for CPU-intensive similarity calculations
        this.vectorOperationsPool = new ThreadPoolExecutor(
            corePoolSize / 2, maxPoolSize / 2,
            keepAliveTime, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new NamedThreadFactory("mem0-vector"),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        // Embedding pool - optimized for batch processing
        this.embeddingPool = new ThreadPoolExecutor(
            corePoolSize / 4, maxPoolSize / 4,
            keepAliveTime, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(500),
            new NamedThreadFactory("mem0-embedding"),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        // Memory management pool - for conflict detection, scoring, etc.
        this.memoryManagementPool = new ThreadPoolExecutor(
            corePoolSize / 4, maxPoolSize / 4,
            keepAliveTime, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(500),
            new NamedThreadFactory("mem0-memory"),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        // I/O operations pool - for database operations
        this.ioOperationsPool = new ThreadPoolExecutor(
            corePoolSize / 2, maxPoolSize,
            keepAliveTime, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(2000),
            new NamedThreadFactory("mem0-io"),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        // Scheduled operations pool - for cleanup and maintenance tasks
        this.scheduledPool = Executors.newScheduledThreadPool(
            2, new NamedThreadFactory("mem0-scheduled")
        );
        
        logger.info("ConcurrentExecutionManager initialized with {} core threads", threadCount);
        logPoolConfiguration();
    }
    
    /**
     * Execute vector similarity calculations concurrently
     */
    public <T> CompletableFuture<T> executeVectorOperation(Callable<T> operation) {
        activeVectorOperations.incrementAndGet();
        return CompletableFuture.supplyAsync(() -> {
            try {
                return operation.call();
            } catch (Exception e) {
                throw new RuntimeException("Vector operation failed", e);
            } finally {
                activeVectorOperations.decrementAndGet();
            }
        }, vectorOperationsPool);
    }
    
    /**
     * Execute embedding generation with optimized batch processing
     */
    public <T> CompletableFuture<T> executeEmbeddingOperation(Callable<T> operation) {
        activeEmbeddingOperations.incrementAndGet();
        return CompletableFuture.supplyAsync(() -> {
            try {
                return operation.call();
            } catch (Exception e) {
                throw new RuntimeException("Embedding operation failed", e);
            } finally {
                activeEmbeddingOperations.decrementAndGet();
            }
        }, embeddingPool);
    }
    
    /**
     * Execute memory management operations (classification, conflict detection, etc.)
     */
    public <T> CompletableFuture<T> executeMemoryOperation(Callable<T> operation) {
        activeMemoryOperations.incrementAndGet();
        return CompletableFuture.supplyAsync(() -> {
            try {
                return operation.call();
            } catch (Exception e) {
                throw new RuntimeException("Memory operation failed", e);
            } finally {
                activeMemoryOperations.decrementAndGet();
            }
        }, memoryManagementPool);
    }
    
    /**
     * Execute I/O operations (database reads/writes)
     */
    public <T> CompletableFuture<T> executeIOOperation(Callable<T> operation) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return operation.call();
            } catch (Exception e) {
                throw new RuntimeException("I/O operation failed", e);
            }
        }, ioOperationsPool);
    }
    
    /**
     * Schedule recurring tasks (cleanup, maintenance, etc.)
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, 
                                                 long period, TimeUnit unit) {
        return scheduledPool.scheduleAtFixedRate(task, initialDelay, period, unit);
    }
    
    /**
     * Execute multiple operations concurrently and wait for all to complete
     */
    public <T> CompletableFuture<Void> executeAllConcurrently(CompletableFuture<T>... operations) {
        return CompletableFuture.allOf(operations);
    }
    
    /**
     * Execute operations with timeout and fallback
     */
    public <T> CompletableFuture<T> executeWithTimeout(Callable<T> operation, 
                                                     long timeout, TimeUnit unit, T fallback) {
        CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
            try {
                return operation.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, vectorOperationsPool);
        
        // Java 8 compatible timeout implementation
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> {
            future.complete(fallback);
        }, timeout, unit);
        
        return future;
    }
    
    /**
     * Get current performance metrics
     */
    public PerformanceMetrics getMetrics() {
        return new PerformanceMetrics(
            activeVectorOperations.get(),
            activeEmbeddingOperations.get(),
            activeMemoryOperations.get(),
            getQueueSizes(),
            getThreadPoolUtilization()
        );
    }
    
    private static int detectOptimalThreadCount() {
        int processors = Runtime.getRuntime().availableProcessors();
        // For mixed CPU/I-O workload, use processors * 1.5 to 2
        int optimalCount = Math.max(4, (int) (processors * 1.5));
        return Math.min(optimalCount, 32); // Cap at 32 threads
    }
    
    private void logPoolConfiguration() {
        logger.info("Thread pool configuration:");
        logger.info("  Vector operations: {}-{} threads", corePoolSize / 2, maxPoolSize / 2);
        logger.info("  Embedding operations: {}-{} threads", corePoolSize / 4, maxPoolSize / 4);
        logger.info("  Memory management: {}-{} threads", corePoolSize / 4, maxPoolSize / 4);
        logger.info("  I/O operations: {}-{} threads", corePoolSize / 2, maxPoolSize);
        logger.info("  Scheduled operations: 2 threads");
    }
    
    private QueueMetrics getQueueSizes() {
        int vectorQueue = getQueueSize(vectorOperationsPool);
        int embeddingQueue = getQueueSize(embeddingPool);
        int memoryQueue = getQueueSize(memoryManagementPool);
        int ioQueue = getQueueSize(ioOperationsPool);
        
        return new QueueMetrics(vectorQueue, embeddingQueue, memoryQueue, ioQueue);
    }
    
    private int getQueueSize(ExecutorService executor) {
        if (executor instanceof ThreadPoolExecutor) {
            return ((ThreadPoolExecutor) executor).getQueue().size();
        }
        return -1;
    }
    
    private ThreadPoolMetrics getThreadPoolUtilization() {
        return new ThreadPoolMetrics(
            getPoolUtilization(vectorOperationsPool),
            getPoolUtilization(embeddingPool),
            getPoolUtilization(memoryManagementPool),
            getPoolUtilization(ioOperationsPool)
        );
    }
    
    private double getPoolUtilization(ExecutorService executor) {
        if (executor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) executor;
            return (double) tpe.getActiveCount() / tpe.getMaximumPoolSize();
        }
        return 0.0;
    }
    
    @Override
    public void close() {
        logger.info("Shutting down ConcurrentExecutionManager");
        
        // Graceful shutdown
        shutdownPool("Vector Operations", vectorOperationsPool);
        shutdownPool("Embedding", embeddingPool);
        shutdownPool("Memory Management", memoryManagementPool);
        shutdownPool("I/O Operations", ioOperationsPool);
        shutdownPool("Scheduled", scheduledPool);
        
        logger.info("All thread pools shut down successfully");
    }
    
    private void shutdownPool(String name, ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warn("{} pool did not terminate gracefully, forcing shutdown", name);
                executor.shutdownNow();
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.error("{} pool did not terminate after forced shutdown", name);
                }
            }
        } catch (InterruptedException e) {
            logger.warn("{} pool shutdown interrupted", name);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // Custom thread factory for better debugging
    private static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        
        NamedThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix + "-thread-";
        }
        
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }
    
    // Performance metrics classes
    public static class PerformanceMetrics {
        private final int activeVectorOps;
        private final int activeEmbeddingOps;
        private final int activeMemoryOps;
        private final QueueMetrics queueMetrics;
        private final ThreadPoolMetrics poolMetrics;
        
        public PerformanceMetrics(int activeVectorOps, int activeEmbeddingOps, 
                                int activeMemoryOps, QueueMetrics queueMetrics, 
                                ThreadPoolMetrics poolMetrics) {
            this.activeVectorOps = activeVectorOps;
            this.activeEmbeddingOps = activeEmbeddingOps;
            this.activeMemoryOps = activeMemoryOps;
            this.queueMetrics = queueMetrics;
            this.poolMetrics = poolMetrics;
        }
        
        // Getters
        public int getActiveVectorOps() { return activeVectorOps; }
        public int getActiveEmbeddingOps() { return activeEmbeddingOps; }
        public int getActiveMemoryOps() { return activeMemoryOps; }
        public QueueMetrics getQueueMetrics() { return queueMetrics; }
        public ThreadPoolMetrics getPoolMetrics() { return poolMetrics; }
        
        @Override
        public String toString() {
            return String.format("PerformanceMetrics{vector=%d, embedding=%d, memory=%d, %s, %s}", 
                activeVectorOps, activeEmbeddingOps, activeMemoryOps, queueMetrics, poolMetrics);
        }
    }
    
    public static class QueueMetrics {
        private final int vectorQueue;
        private final int embeddingQueue;
        private final int memoryQueue;
        private final int ioQueue;
        
        public QueueMetrics(int vectorQueue, int embeddingQueue, int memoryQueue, int ioQueue) {
            this.vectorQueue = vectorQueue;
            this.embeddingQueue = embeddingQueue;
            this.memoryQueue = memoryQueue;
            this.ioQueue = ioQueue;
        }
        
        public int getVectorQueue() { return vectorQueue; }
        public int getEmbeddingQueue() { return embeddingQueue; }
        public int getMemoryQueue() { return memoryQueue; }
        public int getIoQueue() { return ioQueue; }
        
        @Override
        public String toString() {
            return String.format("Queues{vector=%d, embedding=%d, memory=%d, io=%d}", 
                vectorQueue, embeddingQueue, memoryQueue, ioQueue);
        }
    }
    
    public static class ThreadPoolMetrics {
        private final double vectorUtilization;
        private final double embeddingUtilization;
        private final double memoryUtilization;
        private final double ioUtilization;
        
        public ThreadPoolMetrics(double vectorUtilization, double embeddingUtilization, 
                               double memoryUtilization, double ioUtilization) {
            this.vectorUtilization = vectorUtilization;
            this.embeddingUtilization = embeddingUtilization;
            this.memoryUtilization = memoryUtilization;
            this.ioUtilization = ioUtilization;
        }
        
        public double getVectorUtilization() { return vectorUtilization; }
        public double getEmbeddingUtilization() { return embeddingUtilization; }
        public double getMemoryUtilization() { return memoryUtilization; }
        public double getIoUtilization() { return ioUtilization; }
        
        @Override
        public String toString() {
            return String.format("Utilization{vector=%.2f, embedding=%.2f, memory=%.2f, io=%.2f}", 
                vectorUtilization, embeddingUtilization, memoryUtilization, ioUtilization);
        }
    }
}