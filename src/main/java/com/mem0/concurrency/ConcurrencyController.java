package com.mem0.concurrency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mem0.monitoring.PerformanceMonitor;
import com.mem0.model.ResourcePoolStats;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * 并发控制器
 * Concurrency Controller
 * 
 * 管理系统的并发访问、资源限制、请求限流和死锁检测。该组件是Mem0系统并发架构的核心，
 * 负责协调所有并发操作，确保系统在高负载下的稳定性和性能。
 * 
 * Manages system concurrent access, resource limits, request throttling, and deadlock detection.
 * This component is the core of Mem0's concurrency architecture, responsible for coordinating
 * all concurrent operations and ensuring system stability and performance under high load.
 * 
 * 核心功能 / Key Features:
 * - 并发请求控制和信号量管理 / Concurrent request control and semaphore management
 * - 用户级别的速率限制和流量控制 / User-level rate limiting and traffic control
 * - 资源池管理和生命周期控制 / Resource pool management and lifecycle control
 * - 分布式锁创建和死锁检测 / Distributed lock creation and deadlock detection
 * - 请求队列管理和优先级调度 / Request queue management and priority scheduling
 * - 系统健康状态监控 / System health status monitoring
 * 
 * 技术规格 / Technical Specifications:
 * - 默认最大并发数: 1000 / Default max concurrency: 1000
 * - 默认队列大小: 5000 / Default queue size: 5000
 * - 请求超时时间: 30秒 / Request timeout: 30 seconds
 * - 限流窗口时间: 60秒 / Rate limit window: 60 seconds
 * - 默认窗口请求限制: 100次 / Default window request limit: 100 times
 * 
 * 使用示例 / Usage Example:
 * <pre>
 * {@code
 * // 创建并发控制器 / Create concurrency controller
 * PerformanceMonitor monitor = new PerformanceMonitor();
 * ConcurrencyController controller = new ConcurrencyController(monitor);
 * 
 * // 执行受控请求 / Execute controlled request
 * CompletableFuture<String> future = controller.executeControlledRequest(
 *     "user123", "search", () -> performSearch(), RequestPriority.NORMAL
 * );
 * 
 * // 设置用户限流 / Set user rate limit
 * controller.setUserRateLimit("user123", 50, 60000);
 * 
 * // 获取或创建资源池 / Get or create resource pool
 * ResourcePool<Connection> pool = controller.getOrCreateResourcePool(
 *     "db-connections", DatabaseConnection::new, 20, 300000
 * );
 * 
 * // 创建分布式锁 / Create distributed lock
 * DistributedLock lock = controller.createDistributedLock("resource-lock", 30000);
 * }
 * </pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class ConcurrencyController {
    
    private static final Logger logger = LoggerFactory.getLogger(ConcurrencyController.class);
    
    // 配置参数
    private final int maxConcurrentRequests;
    private final int maxQueueSize;
    private final long requestTimeoutMs;
    private final long rateLimitWindowMs;
    private final int maxRequestsPerWindow;
    
    // 并发控制
    private final Semaphore requestSemaphore;
    private final BlockingQueue<QueuedRequest> requestQueue;
    private final ThreadPoolExecutor requestExecutor;
    
    // 限流控制
    private final Map<String, UserRateLimit> userRateLimits = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock rateLimitLock = new ReentrantReadWriteLock();
    
    // 资源池管理
    private final Map<String, ResourcePool> resourcePools = new ConcurrentHashMap<>();
    
    // 死锁检测
    private final DeadlockDetector deadlockDetector;
    private final ScheduledExecutorService maintenanceExecutor;
    
    // 统计信息
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong acceptedRequests = new AtomicLong(0);
    private final AtomicLong rejectedRequests = new AtomicLong(0);
    private final AtomicLong timeoutRequests = new AtomicLong(0);
    private final AtomicLong rateLimitedRequests = new AtomicLong(0);
    
    // 性能监控
    private final PerformanceMonitor performanceMonitor;
    private volatile boolean isShutdown = false;

    public ConcurrencyController(PerformanceMonitor performanceMonitor) {
        this(performanceMonitor, 1000, 5000, 30000, 60000, 100);
    }

    public ConcurrencyController(PerformanceMonitor performanceMonitor,
                               int maxConcurrentRequests,
                               int maxQueueSize,
                               long requestTimeoutMs,
                               long rateLimitWindowMs,
                               int maxRequestsPerWindow) {
        this.performanceMonitor = performanceMonitor;
        this.maxConcurrentRequests = maxConcurrentRequests;
        this.maxQueueSize = maxQueueSize;
        this.requestTimeoutMs = requestTimeoutMs;
        this.rateLimitWindowMs = rateLimitWindowMs;
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        
        // 初始化并发控制
        this.requestSemaphore = new Semaphore(maxConcurrentRequests, true);
        this.requestQueue = new LinkedBlockingQueue<>(maxQueueSize);
        
        // 创建请求执行线程池
        this.requestExecutor = new ThreadPoolExecutor(
            Math.min(10, maxConcurrentRequests / 4),
            maxConcurrentRequests,
            60L, TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            r -> {
                Thread t = new Thread(r, "mem0-concurrency-executor");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        // 初始化死锁检测器
        this.deadlockDetector = new DeadlockDetector();
        
        // 启动维护任务
        this.maintenanceExecutor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "mem0-concurrency-maintenance");
            t.setDaemon(true);
            return t;
        });
        
        startMaintenanceTasks();
        logger.info("并发控制器初始化完成 - 最大并发: {}, 队列大小: {}, 超时: {}ms", 
                   maxConcurrentRequests, maxQueueSize, requestTimeoutMs);
    }

    /**
     * 执行受控请求
     */
    public <T> CompletableFuture<T> executeControlledRequest(String userId, String requestType, 
                                                           Callable<T> request) {
        return executeControlledRequest(userId, requestType, request, RequestPriority.NORMAL);
    }

    /**
     * 执行受控请求（带优先级）
     */
    public <T> CompletableFuture<T> executeControlledRequest(String userId, String requestType, 
                                                           Callable<T> request, RequestPriority priority) {
        if (isShutdown) {
            CompletableFuture<T> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new IllegalStateException("并发控制器已关闭"));
            return failedFuture;
        }

        totalRequests.incrementAndGet();
        performanceMonitor.incrementCounter("concurrency.requests.total");

        // 检查限流
        if (!checkRateLimit(userId)) {
            rateLimitedRequests.incrementAndGet();
            performanceMonitor.incrementCounter("concurrency.requests.rate_limited");
            CompletableFuture<T> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new RateLimitExceededException("用户请求频率超限: " + userId));
            return failedFuture;
        }

        // 创建请求对象
        QueuedRequest queuedRequest = new QueuedRequest(userId, requestType, request, priority);
        
        try {
            // 尝试获取信号量
            if (requestSemaphore.tryAcquire()) {
                // 直接执行
                return executeRequestDirectly(queuedRequest);
            } else {
                // 加入队列等待
                return enqueueRequest(queuedRequest);
            }
        } catch (Exception e) {
            rejectedRequests.incrementAndGet();
            performanceMonitor.incrementCounter("concurrency.requests.rejected");
            CompletableFuture<T> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        }
    }

    /**
     * 获取资源池
     */
    public <T> ResourcePool<T> getOrCreateResourcePool(String poolName, Supplier<T> resourceFactory, 
                                                     int maxSize, long idleTimeoutMs) {
        return resourcePools.computeIfAbsent(poolName, name -> 
            new ResourcePool<>(name, resourceFactory, maxSize, idleTimeoutMs, performanceMonitor)
        );
    }

    /**
     * 创建分布式锁
     */
    public DistributedLock createDistributedLock(String lockName, long leaseTimeMs) {
        return new DistributedLock(lockName, leaseTimeMs, deadlockDetector);
    }

    /**
     * 设置用户限流规则
     */
    public void setUserRateLimit(String userId, int maxRequests, long windowMs) {
        rateLimitLock.writeLock().lock();
        try {
            UserRateLimit rateLimit = new UserRateLimit(userId, maxRequests, windowMs);
            userRateLimits.put(userId, rateLimit);
            logger.debug("设置用户限流规则: {} - {}请求/{}ms", userId, maxRequests, windowMs);
        } finally {
            rateLimitLock.writeLock().unlock();
        }
    }

    /**
     * 获取并发控制统计
     */
    public ConcurrencyStats getStats() {
        Map<String, ResourcePoolStats> poolStats = new HashMap<>();
        resourcePools.forEach((name, pool) -> {
            ResourcePool.ResourcePoolStats internalStats = pool.getStats();
            com.mem0.model.ResourcePoolStats modelStats = new com.mem0.model.ResourcePoolStats(
                internalStats.getPoolName(),
                internalStats.getCurrentSize(),
                internalStats.getActiveCount(),
                internalStats.getAvailableCount(),
                internalStats.getMaxSize(),
                internalStats.getTotalAcquisitions(),
                internalStats.getTotalReleases(),
                0.0, // avgBorrowTimeMs - not available in internal stats
                0.0, // avgWaitTimeMs - not available in internal stats
                internalStats.getWaitingCount(),
                java.time.Instant.now(), // lastActivity - use current time
                internalStats.getTotalCreated(),
                internalStats.getTotalDestroyed(),
                0 // errorCount - not available in internal stats
            );
            poolStats.put(name, modelStats);
        });
        
        return new ConcurrencyStats(
            totalRequests.get(),
            acceptedRequests.get(),
            rejectedRequests.get(),
            timeoutRequests.get(),
            rateLimitedRequests.get(),
            requestSemaphore.availablePermits(),
            requestQueue.size(),
            requestExecutor.getActiveCount(),
            requestExecutor.getCompletedTaskCount(),
            poolStats,
            deadlockDetector.getStats()
        );
    }

    /**
     * 检查系统健康状态
     */
    public HealthStatus checkHealth() {
        List<String> issues = new ArrayList<>();
        
        // 检查并发度
        if (requestSemaphore.availablePermits() == 0) {
            issues.add("所有并发槽位已占用");
        }
        
        // 检查队列
        if (requestQueue.size() > maxQueueSize * 0.8) {
            issues.add("请求队列接近满载");
        }
        
        // 检查线程池
        if (requestExecutor.getActiveCount() > requestExecutor.getCorePoolSize() * 0.9) {
            issues.add("线程池高负载运行");
        }
        
        // 检查限流状态
        long recentRateLimited = rateLimitedRequests.get();
        long recentTotal = totalRequests.get();
        if (recentTotal > 0 && (double) recentRateLimited / recentTotal > 0.1) {
            issues.add("限流率过高");
        }
        
        // 检查资源池
        for (ResourcePool pool : resourcePools.values()) {
            ResourcePool.ResourcePoolStats stats = pool.getStats();
            if (stats.getWaitingCount() > 0) {
                issues.add("资源池 " + pool.getName() + " 有等待请求");
            }
        }
        
        boolean isHealthy = issues.isEmpty();
        return new HealthStatus(isHealthy, issues);
    }

    /**
     * 优雅关闭
     */
    public CompletableFuture<Void> shutdown() {
        logger.info("开始关闭并发控制器");
        isShutdown = true;
        
        return CompletableFuture.runAsync(() -> {
            try {
                // 停止维护任务
                maintenanceExecutor.shutdown();
                maintenanceExecutor.awaitTermination(5, TimeUnit.SECONDS);
                
                // 停止请求执行器
                requestExecutor.shutdown();
                requestExecutor.awaitTermination(30, TimeUnit.SECONDS);
                
                // 关闭资源池
                for (ResourcePool pool : resourcePools.values()) {
                    pool.shutdown();
                }
                
                // 清理限流信息
                userRateLimits.clear();
                
                logger.info("并发控制器关闭完成");
                
            } catch (Exception e) {
                logger.error("关闭并发控制器时发生错误", e);
            }
        });
    }

    // 私有辅助方法

    private void startMaintenanceTasks() {
        // 定期清理过期的限流记录
        maintenanceExecutor.scheduleAtFixedRate(this::cleanupExpiredRateLimits, 
            60, 60, TimeUnit.SECONDS);
        
        // 定期检查死锁
        maintenanceExecutor.scheduleAtFixedRate(deadlockDetector::detectDeadlocks, 
            10, 10, TimeUnit.SECONDS);
            
        // 定期统计报告
        maintenanceExecutor.scheduleAtFixedRate(this::reportStats, 
            300, 300, TimeUnit.SECONDS);
    }

    private boolean checkRateLimit(String userId) {
        rateLimitLock.readLock().lock();
        try {
            UserRateLimit rateLimit = userRateLimits.get(userId);
            if (rateLimit == null) {
                // 使用默认限流规则
                rateLimit = userRateLimits.computeIfAbsent(userId, 
                    id -> new UserRateLimit(id, maxRequestsPerWindow, rateLimitWindowMs));
            }
            
            return rateLimit.tryAcquire();
        } finally {
            rateLimitLock.readLock().unlock();
        }
    }

    private <T> CompletableFuture<T> executeRequestDirectly(QueuedRequest request) {
        acceptedRequests.incrementAndGet();
        performanceMonitor.incrementCounter("concurrency.requests.accepted");
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                performanceMonitor.incrementCounter("concurrency.requests.executing");
                
                T result = performanceMonitor.measureTime("concurrency.request.execution", () -> {
                    return (T) request.callable.call();
                });
                
                performanceMonitor.incrementCounter("concurrency.requests.completed");
                return result;
            } catch (Exception e) {
                throw new RuntimeException("请求执行失败", e);
                
            } finally {
                requestSemaphore.release();
            }
        }, requestExecutor);
    }

    private <T> CompletableFuture<T> enqueueRequest(QueuedRequest request) {
        try {
            // 尝试加入队列
            if (requestQueue.offer(request)) {
                performanceMonitor.incrementCounter("concurrency.requests.queued");
                
                // 创建超时处理
                ScheduledFuture<?> timeoutFuture = maintenanceExecutor.schedule(() -> {
                    if (requestQueue.remove(request)) {
                        timeoutRequests.incrementAndGet();
                        performanceMonitor.incrementCounter("concurrency.requests.timeout");
                        request.future.completeExceptionally(new TimeoutException("请求超时"));
                    }
                }, requestTimeoutMs, TimeUnit.MILLISECONDS);
                
                request.timeoutFuture = timeoutFuture;
                
                // 尝试处理队列中的请求
                tryProcessQueue();
                
                return (CompletableFuture<T>) request.future;
            } else {
                rejectedRequests.incrementAndGet();
                performanceMonitor.incrementCounter("concurrency.requests.queue_full");
                throw new RejectedExecutionException("请求队列已满");
            }
        } catch (Exception e) {
            rejectedRequests.incrementAndGet();
            CompletableFuture<T> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        }
    }

    private void tryProcessQueue() {
        // 在单独的线程中尝试处理队列
        CompletableFuture.runAsync(() -> {
            while (!isShutdown && !requestQueue.isEmpty() && requestSemaphore.tryAcquire()) {
                QueuedRequest request = requestQueue.poll();
                if (request != null) {
                    // 取消超时任务
                    if (request.timeoutFuture != null) {
                        request.timeoutFuture.cancel(false);
                    }
                    
                    // 执行请求
                    CompletableFuture<?> execution = executeRequestDirectly(request);
                    execution.whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            request.future.completeExceptionally(throwable);
                        } else {
                            request.future.complete(result);
                        }
                    });
                }
            }
        });
    }

    private void cleanupExpiredRateLimits() {
        rateLimitLock.writeLock().lock();
        try {
            long now = System.currentTimeMillis();
            userRateLimits.entrySet().removeIf(entry -> {
                UserRateLimit rateLimit = entry.getValue();
                return now - rateLimit.getLastAccessTime() > rateLimitWindowMs * 10; // 10个窗口周期后清理
            });
        } finally {
            rateLimitLock.writeLock().unlock();
        }
    }

    private void reportStats() {
        if (logger.isInfoEnabled()) {
            ConcurrencyStats stats = getStats();
            logger.info("并发控制统计: {}", stats);
        }
    }

    // 内部类

    private static class QueuedRequest {
        final String userId;
        final String requestType;
        final Callable<?> callable;
        final RequestPriority priority;
        final CompletableFuture<Object> future;
        final long createTime;
        volatile ScheduledFuture<?> timeoutFuture;

        QueuedRequest(String userId, String requestType, Callable<?> callable, RequestPriority priority) {
            this.userId = userId;
            this.requestType = requestType;
            this.callable = callable;
            this.priority = priority;
            this.future = new CompletableFuture<>();
            this.createTime = System.currentTimeMillis();
        }
    }

    private static class UserRateLimit {
        private final String userId;
        private final int maxRequests;
        private final long windowMs;
        private final LinkedList<Long> requestTimes = new LinkedList<>();
        private volatile long lastAccessTime;

        UserRateLimit(String userId, int maxRequests, long windowMs) {
            this.userId = userId;
            this.maxRequests = maxRequests;
            this.windowMs = windowMs;
            this.lastAccessTime = System.currentTimeMillis();
        }

        synchronized boolean tryAcquire() {
            long now = System.currentTimeMillis();
            lastAccessTime = now;
            
            // 清理过期的请求记录
            while (!requestTimes.isEmpty() && now - requestTimes.peekFirst() > windowMs) {
                requestTimes.removeFirst();
            }
            
            // 检查是否超限
            if (requestTimes.size() >= maxRequests) {
                return false;
            }
            
            // 记录此次请求
            requestTimes.addLast(now);
            return true;
        }

        long getLastAccessTime() {
            return lastAccessTime;
        }
    }

    public enum RequestPriority {
        LOW(1),
        NORMAL(5),
        HIGH(10),
        URGENT(20);

        private final int weight;

        RequestPriority(int weight) {
            this.weight = weight;
        }

        public int getWeight() {
            return weight;
        }
    }

    public static class ConcurrencyStats {
        private final long totalRequests;
        private final long acceptedRequests;
        private final long rejectedRequests;
        private final long timeoutRequests;
        private final long rateLimitedRequests;
        private final int availablePermits;
        private final int queueSize;
        private final int activeThreads;
        private final long completedTasks;
        private final Map<String, ResourcePoolStats> poolStats;
        private final DeadlockDetector.DeadlockStats deadlockStats;

        public ConcurrencyStats(long totalRequests, long acceptedRequests, long rejectedRequests,
                              long timeoutRequests, long rateLimitedRequests, int availablePermits,
                              int queueSize, int activeThreads, long completedTasks,
                              Map<String, ResourcePoolStats> poolStats,
                              DeadlockDetector.DeadlockStats deadlockStats) {
            this.totalRequests = totalRequests;
            this.acceptedRequests = acceptedRequests;
            this.rejectedRequests = rejectedRequests;
            this.timeoutRequests = timeoutRequests;
            this.rateLimitedRequests = rateLimitedRequests;
            this.availablePermits = availablePermits;
            this.queueSize = queueSize;
            this.activeThreads = activeThreads;
            this.completedTasks = completedTasks;
            this.poolStats = poolStats;
            this.deadlockStats = deadlockStats;
        }

        // Getter 方法
        public long getTotalRequests() { return totalRequests; }
        public long getAcceptedRequests() { return acceptedRequests; }
        public long getRejectedRequests() { return rejectedRequests; }
        public long getTimeoutRequests() { return timeoutRequests; }
        public long getRateLimitedRequests() { return rateLimitedRequests; }
        public int getAvailablePermits() { return availablePermits; }
        public int getQueueSize() { return queueSize; }
        public int getActiveThreads() { return activeThreads; }
        public long getCompletedTasks() { return completedTasks; }
        public Map<String, ResourcePoolStats> getPoolStats() { return poolStats; }
        public DeadlockDetector.DeadlockStats getDeadlockStats() { return deadlockStats; }

        public double getAcceptanceRate() {
            return totalRequests == 0 ? 0.0 : (double) acceptedRequests / totalRequests;
        }

        @Override
        public String toString() {
            return String.format("ConcurrencyStats{总请求=%d, 接受=%d, 拒绝=%d, 超时=%d, 限流=%d, 接受率=%.2f%%, 可用许可=%d, 队列=%d, 活跃线程=%d}",
                totalRequests, acceptedRequests, rejectedRequests, timeoutRequests, rateLimitedRequests,
                getAcceptanceRate() * 100, availablePermits, queueSize, activeThreads);
        }
    }

    public static class HealthStatus {
        private final boolean healthy;
        private final List<String> issues;

        public HealthStatus(boolean healthy, List<String> issues) {
            this.healthy = healthy;
            this.issues = new ArrayList<>(issues);
        }

        public boolean isHealthy() { return healthy; }
        public List<String> getIssues() { return Collections.unmodifiableList(issues); }

        @Override
        public String toString() {
            return String.format("HealthStatus{健康=%s, 问题=%s}", healthy, issues);
        }
    }
}

class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}