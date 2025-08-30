package com.mem0.concurrency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mem0.monitoring.PerformanceMonitor;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 通用资源池实现
 * Generic Resource Pool Implementation
 * 
 * 高性能的通用资源池，支持资源创建、复用、超时清理和监控。该组件针对Mem0系统
 * 的资源管理需求进行优化，确保资源的高效利用和自动生命周期管理。
 * 
 * High-performance generic resource pool supporting resource creation, reuse, timeout cleanup,
 * and monitoring. This component is optimized for Mem0's resource management requirements,
 * ensuring efficient resource utilization and automatic lifecycle management.
 * 
 * 核心功能 / Key Features:
 * - 线程安全的资源获取和释放 / Thread-safe resource acquisition and release
 * - 资源空闲超时自动清理 / Automatic cleanup of idle timeout resources
 * - 阻塞式和非阻塞式资源获取 / Blocking and non-blocking resource acquisition
 * - 资源等待队列管理 / Resource waiting queue management
 * - 详细的资源池统计信息 / Detailed resource pool statistics
 * - 资源生命周期监控 / Resource lifecycle monitoring
 * 
 * 技术规格 / Technical Specifications:
 * - 支持泛型资源类型 / Supports generic resource types
 * - 线程安全并发访问 / Thread-safe concurrent access
 * - 可配置的池大小限制 / Configurable pool size limits
 * - 可配置的资源空闲超时 / Configurable resource idle timeout
 * - 自动资源清理任务 / Automatic resource cleanup tasks
 * 
 * 使用示例 / Usage Example:
 * <pre>
 * {@code
 * // 创建数据库连接池 / Create database connection pool
 * ResourcePool<Connection> connectionPool = new ResourcePool<>(
 *     "db-connections",                    // 池名称 / Pool name
 *     () -> DriverManager.getConnection(), // 资源工厂 / Resource factory
 *     20,                                  // 最大大小 / Max size
 *     300000,                             // 空闲超时 / Idle timeout (5 minutes)
 *     performanceMonitor                   // 性能监控 / Performance monitor
 * );
 * 
 * // 获取资源 / Acquire resource
 * Connection conn = connectionPool.acquire();
 * try {
 *     // 使用连接 / Use connection
 *     executeQuery(conn);
 * } finally {
 *     // 释放资源 / Release resource
 *     connectionPool.release(conn);
 * }
 * 
 * // 带超时获取资源 / Acquire resource with timeout
 * Connection conn = connectionPool.acquire(5, TimeUnit.SECONDS);
 * 
 * // 获取资源池统计 / Get pool statistics
 * ResourcePoolStats stats = connectionPool.getStats();
 * System.out.println(stats);
 * }
 * </pre>
 * 
 * @param <T> 资源类型 / Resource type
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class ResourcePool<T> {
    
    private static final Logger logger = LoggerFactory.getLogger(ResourcePool.class);
    
    private final String poolName;
    private final Supplier<T> resourceFactory;
    private final int maxSize;
    private final long idleTimeoutMs;
    private final PerformanceMonitor performanceMonitor;
    
    // 资源存储
    private final BlockingQueue<PooledResource<T>> availableResources;
    private final ConcurrentHashMap<T, PooledResource<T>> allResources = new ConcurrentHashMap<>();
    
    // 等待队列
    private final BlockingQueue<ResourceRequest<T>> waitingRequests = new LinkedBlockingQueue<>();
    
    // 统计信息
    private final AtomicInteger totalCreated = new AtomicInteger(0);
    private final AtomicInteger totalDestroyed = new AtomicInteger(0);
    private final AtomicInteger currentSize = new AtomicInteger(0);
    private final AtomicInteger activeCount = new AtomicInteger(0);
    private final AtomicLong totalAcquisitions = new AtomicLong(0);
    private final AtomicLong totalReleases = new AtomicLong(0);
    
    // 清理任务
    private final ScheduledExecutorService cleanupExecutor;
    private volatile boolean isShutdown = false;

    public ResourcePool(String poolName, Supplier<T> resourceFactory, int maxSize, long idleTimeoutMs, 
                       PerformanceMonitor performanceMonitor) {
        this.poolName = poolName;
        this.resourceFactory = resourceFactory;
        this.maxSize = maxSize;
        this.idleTimeoutMs = idleTimeoutMs;
        this.performanceMonitor = performanceMonitor;
        
        this.availableResources = new LinkedBlockingQueue<>();
        
        // 启动清理任务
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "resource-pool-cleanup-" + poolName);
            t.setDaemon(true);
            return t;
        });
        
        startCleanupTask();
        logger.info("资源池初始化完成: {} - 最大大小: {}, 空闲超时: {}ms", poolName, maxSize, idleTimeoutMs);
    }

    /**
     * 获取资源（阻塞）
     */
    public T acquire() throws InterruptedException {
        return acquire(0, TimeUnit.MILLISECONDS);
    }

    /**
     * 获取资源（带超时）
     */
    public T acquire(long timeout, TimeUnit unit) throws InterruptedException {
        if (isShutdown) {
            throw new IllegalStateException("资源池已关闭: " + poolName);
        }
        
        totalAcquisitions.incrementAndGet();
        performanceMonitor.incrementCounter("resource_pool." + poolName + ".acquisitions");
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 尝试从可用资源中获取
            PooledResource<T> pooledResource = availableResources.poll();
            
            if (pooledResource != null && !pooledResource.isExpired()) {
                activeCount.incrementAndGet();
                pooledResource.markAsActive();
                
                performanceMonitor.recordTimer("resource_pool." + poolName + ".acquire_time", 
                    System.currentTimeMillis() - startTime);
                
                return pooledResource.getResource();
            }
            
            // 如果获取到过期资源，销毁它
            if (pooledResource != null) {
                destroyResource(pooledResource);
            }
            
            // 尝试创建新资源
            if (currentSize.get() < maxSize) {
                T newResource = createNewResource();
                if (newResource != null) {
                    return newResource;
                }
            }
            
            // 等待资源可用
            if (timeout > 0) {
                return waitForResource(timeout, unit, startTime);
            } else {
                return waitForResource(Long.MAX_VALUE, TimeUnit.MILLISECONDS, startTime);
            }
            
        } catch (Exception e) {
            performanceMonitor.incrementCounter("resource_pool." + poolName + ".acquisition_errors");
            logger.error("获取资源失败: " + poolName, e);
            throw e;
        }
    }

    /**
     * 释放资源
     */
    public void release(T resource) {
        if (resource == null || isShutdown) {
            return;
        }
        
        PooledResource<T> pooledResource = allResources.get(resource);
        if (pooledResource == null) {
            logger.warn("尝试释放未知资源: {}", poolName);
            return;
        }
        
        totalReleases.incrementAndGet();
        performanceMonitor.incrementCounter("resource_pool." + poolName + ".releases");
        
        activeCount.decrementAndGet();
        pooledResource.markAsIdle();
        
        // 检查是否有等待的请求
        ResourceRequest<T> waitingRequest = waitingRequests.poll();
        if (waitingRequest != null) {
            // 直接分配给等待的请求
            activeCount.incrementAndGet();
            pooledResource.markAsActive();
            waitingRequest.complete(resource);
        } else {
            // 返回到可用资源池
            availableResources.offer(pooledResource);
        }
        
        logger.debug("资源已释放: {} - 活跃: {}, 可用: {}", poolName, activeCount.get(), availableResources.size());
    }

    /**
     * 获取资源池统计信息
     */
    public ResourcePoolStats getStats() {
        return new ResourcePoolStats(
            poolName,
            totalCreated.get(),
            totalDestroyed.get(),
            currentSize.get(),
            activeCount.get(),
            availableResources.size(),
            waitingRequests.size(),
            totalAcquisitions.get(),
            totalReleases.get(),
            maxSize,
            idleTimeoutMs
        );
    }

    /**
     * 获取池名称
     */
    public String getName() {
        return poolName;
    }

    /**
     * 关闭资源池
     */
    public void shutdown() {
        logger.info("开始关闭资源池: {}", poolName);
        isShutdown = true;
        
        // 停止清理任务
        cleanupExecutor.shutdown();
        
        // 完成所有等待的请求（抛出异常）
        ResourceRequest<T> waitingRequest;
        while ((waitingRequest = waitingRequests.poll()) != null) {
            waitingRequest.completeExceptionally(new IllegalStateException("资源池已关闭"));
        }
        
        // 销毁所有资源
        for (PooledResource<T> pooledResource : allResources.values()) {
            destroyResource(pooledResource);
        }
        
        availableResources.clear();
        allResources.clear();
        
        try {
            cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        logger.info("资源池关闭完成: {}", poolName);
    }

    // 私有辅助方法

    private void startCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredResources, 
            idleTimeoutMs / 2, idleTimeoutMs / 2, TimeUnit.MILLISECONDS);
    }

    private void cleanupExpiredResources() {
        if (isShutdown) {
            return;
        }
        
        int cleanedUp = 0;
        
        // 清理过期的空闲资源
        PooledResource<T> resource;
        while ((resource = availableResources.poll()) != null) {
            if (resource.isExpired()) {
                destroyResource(resource);
                cleanedUp++;
            } else {
                // 未过期的资源放回池中
                availableResources.offer(resource);
                break; // 由于队列是有序的，后面的资源也不会过期
            }
        }
        
        if (cleanedUp > 0) {
            logger.debug("清理过期资源: {} - 清理数量: {}, 当前大小: {}", poolName, cleanedUp, currentSize.get());
            performanceMonitor.incrementCounter("resource_pool." + poolName + ".cleanup", cleanedUp);
        }
    }

    private T createNewResource() {
        if (currentSize.get() >= maxSize) {
            return null;
        }
        
        try {
            T resource = resourceFactory.get();
            if (resource != null) {
                PooledResource<T> pooledResource = new PooledResource<>(resource, idleTimeoutMs);
                allResources.put(resource, pooledResource);
                
                currentSize.incrementAndGet();
                activeCount.incrementAndGet();
                totalCreated.incrementAndGet();
                
                pooledResource.markAsActive();
                
                performanceMonitor.incrementCounter("resource_pool." + poolName + ".created");
                logger.debug("创建新资源: {} - 当前大小: {}", poolName, currentSize.get());
                
                return resource;
            }
        } catch (Exception e) {
            logger.error("创建资源失败: " + poolName, e);
            performanceMonitor.incrementCounter("resource_pool." + poolName + ".creation_errors");
        }
        
        return null;
    }

    private void destroyResource(PooledResource<T> pooledResource) {
        T resource = pooledResource.getResource();
        allResources.remove(resource);
        currentSize.decrementAndGet();
        totalDestroyed.incrementAndGet();
        
        performanceMonitor.incrementCounter("resource_pool." + poolName + ".destroyed");
        
        // 如果资源实现了Closeable或AutoCloseable接口，尝试关闭它
        try {
            if (resource instanceof AutoCloseable) {
                ((AutoCloseable) resource).close();
            }
        } catch (Exception e) {
            logger.warn("关闭资源时发生错误: " + poolName, e);
        }
    }

    private T waitForResource(long timeout, TimeUnit unit, long startTime) throws InterruptedException {
        ResourceRequest<T> request = new ResourceRequest<>();
        waitingRequests.offer(request);
        
        performanceMonitor.incrementCounter("resource_pool." + poolName + ".waits");
        
        try {
            T resource = request.get(timeout, unit);
            activeCount.incrementAndGet();
            
            long waitTime = System.currentTimeMillis() - startTime;
            performanceMonitor.recordTimer("resource_pool." + poolName + ".wait_time", waitTime);
            
            return resource;
        } catch (TimeoutException e) {
            waitingRequests.remove(request);
            performanceMonitor.incrementCounter("resource_pool." + poolName + ".timeouts");
            throw new InterruptedException("获取资源超时: " + poolName);
        } catch (ExecutionException e) {
            performanceMonitor.incrementCounter("resource_pool." + poolName + ".wait_errors");
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof InterruptedException) {
                throw (InterruptedException) cause;
            } else {
                throw new RuntimeException("等待资源时发生错误", cause);
            }
        } finally {
            // 确保请求从等待队列中移除
            waitingRequests.remove(request);
        }
    }

    // 内部类

    private static class PooledResource<T> {
        private final T resource;
        private final long idleTimeoutMs;
        private volatile long lastAccessTime;
        private volatile boolean active;

        PooledResource(T resource, long idleTimeoutMs) {
            this.resource = resource;
            this.idleTimeoutMs = idleTimeoutMs;
            this.lastAccessTime = System.currentTimeMillis();
            this.active = false;
        }

        T getResource() {
            return resource;
        }

        void markAsActive() {
            this.active = true;
            this.lastAccessTime = System.currentTimeMillis();
        }

        void markAsIdle() {
            this.active = false;
            this.lastAccessTime = System.currentTimeMillis();
        }

        boolean isExpired() {
            return !active && (System.currentTimeMillis() - lastAccessTime > idleTimeoutMs);
        }

        boolean isActive() {
            return active;
        }
    }

    private static class ResourceRequest<T> {
        private final CompletableFuture<T> future = new CompletableFuture<>();

        void complete(T resource) {
            future.complete(resource);
        }

        void completeExceptionally(Throwable throwable) {
            future.completeExceptionally(throwable);
        }

        T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return future.get(timeout, unit);
        }
    }

    public static class ResourcePoolStats {
        private final String poolName;
        private final int totalCreated;
        private final int totalDestroyed;
        private final int currentSize;
        private final int activeCount;
        private final int availableCount;
        private final int waitingCount;
        private final long totalAcquisitions;
        private final long totalReleases;
        private final int maxSize;
        private final long idleTimeoutMs;

        public ResourcePoolStats(String poolName, int totalCreated, int totalDestroyed, int currentSize,
                               int activeCount, int availableCount, int waitingCount, long totalAcquisitions,
                               long totalReleases, int maxSize, long idleTimeoutMs) {
            this.poolName = poolName;
            this.totalCreated = totalCreated;
            this.totalDestroyed = totalDestroyed;
            this.currentSize = currentSize;
            this.activeCount = activeCount;
            this.availableCount = availableCount;
            this.waitingCount = waitingCount;
            this.totalAcquisitions = totalAcquisitions;
            this.totalReleases = totalReleases;
            this.maxSize = maxSize;
            this.idleTimeoutMs = idleTimeoutMs;
        }

        // Getter 方法
        public String getPoolName() { return poolName; }
        public int getTotalCreated() { return totalCreated; }
        public int getTotalDestroyed() { return totalDestroyed; }
        public int getCurrentSize() { return currentSize; }
        public int getActiveCount() { return activeCount; }
        public int getAvailableCount() { return availableCount; }
        public int getWaitingCount() { return waitingCount; }
        public long getTotalAcquisitions() { return totalAcquisitions; }
        public long getTotalReleases() { return totalReleases; }
        public int getMaxSize() { return maxSize; }
        public long getIdleTimeoutMs() { return idleTimeoutMs; }

        public double getUtilizationRate() {
            return maxSize == 0 ? 0.0 : (double) activeCount / maxSize;
        }

        public double getHitRate() {
            return totalAcquisitions == 0 ? 0.0 : (double) (totalAcquisitions - totalCreated) / totalAcquisitions;
        }

        @Override
        public String toString() {
            return String.format("ResourcePoolStats{池=%s, 当前=%d/%d, 活跃=%d, 可用=%d, 等待=%d, 利用率=%.2f%%, 命中率=%.2f%%}",
                poolName, currentSize, maxSize, activeCount, availableCount, waitingCount,
                getUtilizationRate() * 100, getHitRate() * 100);
        }
    }
}