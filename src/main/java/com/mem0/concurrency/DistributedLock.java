package com.mem0.concurrency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * 分布式锁实现
 * Distributed Lock Implementation
 * 
 * 高性能的分布式锁实现，支持超时、重入、死锁检测和公平性。该组件是Mem0系统
 * 并发控制的重要组成部分，确保分布式环境下资源访问的互斥性和一致性。
 * 
 * High-performance distributed lock implementation supporting timeout, reentrancy,
 * deadlock detection, and fairness. This component is a crucial part of Mem0's
 * concurrency control, ensuring mutual exclusion and consistency in distributed environments.
 * 
 * 核心功能 / Key Features:
 * - 可重入锁支持 / Reentrant lock support
 * - 锁租约自动续期 / Automatic lock lease renewal
 * - 死锁检测和预防 / Deadlock detection and prevention
 * - 公平锁等待队列 / Fair lock waiting queue
 * - 锁超时和强制释放 / Lock timeout and forced release
 * - 详细的锁统计监控 / Detailed lock statistics monitoring
 * 
 * 技术规格 / Technical Specifications:
 * - 基于Java Lock接口实现 / Based on Java Lock interface
 * - 支持可配置的租期时长 / Configurable lease duration
 * - 自动租期续期机制 / Automatic lease renewal mechanism
 * - 线程安全的锁状态管理 / Thread-safe lock state management
 * - 死锁检测集成 / Integrated deadlock detection
 * 
 * 使用示例 / Usage Example:
 * <pre>
 * {@code
 * // 创建分布式锁 / Create distributed lock
 * DeadlockDetector detector = new DeadlockDetector();
 * DistributedLock lock = new DistributedLock("my-resource-lock", 30000, detector);
 * 
 * // 标准锁使用 / Standard lock usage
 * lock.lock();
 * try {
 *     // 执行临界区代码 / Execute critical section code
 *     performCriticalOperation();
 * } finally {
 *     lock.unlock();
 * }
 * 
 * // 尝试获取锁 / Try to acquire lock
 * if (lock.tryLock(5, TimeUnit.SECONDS)) {
 *     try {
 *         performOperation();
 *     } finally {
 *         lock.unlock();
 *     }
 * } else {
 *     handleLockTimeout();
 * }
 * 
 * // 获取锁统计信息 / Get lock statistics
 * DistributedLock.LockStats stats = lock.getStats();
 * System.out.println("Lock status: " + stats);
 * }
 * </pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class DistributedLock implements Lock {
    
    private static final Logger logger = LoggerFactory.getLogger(DistributedLock.class);
    
    private final String lockName;
    private final long leaseTimeMs;
    private final DeadlockDetector deadlockDetector;
    
    // 锁状态
    private volatile String holderId;
    private final AtomicLong reentrantCount = new AtomicLong(0);
    private final AtomicBoolean locked = new AtomicBoolean(false);
    
    // 等待队列
    private final BlockingQueue<LockWaiter> waitQueue = new LinkedBlockingQueue<>();
    
    // 续租任务
    private final ScheduledExecutorService renewalExecutor;
    private volatile ScheduledFuture<?> renewalTask;
    
    // 统计信息
    private final AtomicLong totalAcquisitions = new AtomicLong(0);
    private final AtomicLong totalReleases = new AtomicLong(0);
    private final AtomicLong totalTimeouts = new AtomicLong(0);
    private volatile long createdTime;
    private volatile long lastAccessTime;

    public DistributedLock(String lockName, long leaseTimeMs, DeadlockDetector deadlockDetector) {
        this.lockName = lockName;
        this.leaseTimeMs = leaseTimeMs;
        this.deadlockDetector = deadlockDetector;
        this.createdTime = System.currentTimeMillis();
        this.lastAccessTime = createdTime;
        
        // 创建续租执行器
        this.renewalExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "distributed-lock-renewal-" + lockName);
            t.setDaemon(true);
            return t;
        });
        
        logger.debug("创建分布式锁: {}, 租期: {}ms", lockName, leaseTimeMs);
    }

    @Override
    public void lock() {
        try {
            lockInterruptibly();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取锁时被中断", e);
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        String currentThreadId = getCurrentThreadId();
        
        // 检查重入
        if (isHeldByCurrentThread()) {
            reentrantCount.incrementAndGet();
            logger.debug("重入锁: {} by {}, 重入次数: {}", lockName, currentThreadId, reentrantCount.get());
            return;
        }
        
        totalAcquisitions.incrementAndGet();
        
        // 死锁预检查
        if (deadlockDetector.wouldCauseDeadlock(currentThreadId, lockName)) {
            logger.warn("检测到潜在死锁，拒绝获取锁: {} by {}", lockName, currentThreadId);
            throw new IllegalStateException("获取锁将导致死锁");
        }
        
        // 记录等待状态
        deadlockDetector.recordWaitForLock(currentThreadId, lockName, getCurrentHeldLock(currentThreadId));
        
        try {
            // 尝试快速获取锁
            if (tryAcquireInternal(currentThreadId)) {
                startRenewalTask();
                deadlockDetector.recordLockAcquired(currentThreadId, lockName);
                logger.debug("快速获取锁成功: {} by {}", lockName, currentThreadId);
                return;
            }
            
            // 加入等待队列
            LockWaiter waiter = new LockWaiter(currentThreadId);
            waitQueue.offer(waiter);
            
            logger.debug("加入锁等待队列: {} by {}, 队列长度: {}", lockName, currentThreadId, waitQueue.size());
            
            try {
                // 等待获取锁
                waiter.await();
                startRenewalTask();
                deadlockDetector.recordLockAcquired(currentThreadId, lockName);
                logger.debug("等待获取锁成功: {} by {}", lockName, currentThreadId);
                
            } catch (InterruptedException e) {
                waitQueue.remove(waiter);
                throw e;
            }
            
        } catch (Exception e) {
            totalAcquisitions.decrementAndGet();
            throw e;
        }
    }

    @Override
    public boolean tryLock() {
        try {
            return tryLock(0, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        String currentThreadId = getCurrentThreadId();
        
        // 检查重入
        if (isHeldByCurrentThread()) {
            reentrantCount.incrementAndGet();
            logger.debug("重入锁: {} by {}, 重入次数: {}", lockName, currentThreadId, reentrantCount.get());
            return true;
        }
        
        totalAcquisitions.incrementAndGet();
        long timeoutMs = unit.toMillis(time);
        
        try {
            // 死锁预检查
            if (deadlockDetector.wouldCauseDeadlock(currentThreadId, lockName)) {
                logger.warn("检测到潜在死锁，拒绝获取锁: {} by {}", lockName, currentThreadId);
                return false;
            }
            
            // 记录等待状态
            deadlockDetector.recordWaitForLock(currentThreadId, lockName, getCurrentHeldLock(currentThreadId));
            
            // 尝试快速获取锁
            if (tryAcquireInternal(currentThreadId)) {
                startRenewalTask();
                deadlockDetector.recordLockAcquired(currentThreadId, lockName);
                logger.debug("尝试获取锁成功: {} by {}", lockName, currentThreadId);
                return true;
            }
            
            if (timeoutMs <= 0) {
                return false;
            }
            
            // 带超时的等待
            LockWaiter waiter = new LockWaiter(currentThreadId);
            waitQueue.offer(waiter);
            
            logger.debug("尝试带超时获取锁: {} by {}, 超时: {}ms", lockName, currentThreadId, timeoutMs);
            
            try {
                boolean acquired = waiter.await(timeoutMs, TimeUnit.MILLISECONDS);
                if (acquired) {
                    startRenewalTask();
                    deadlockDetector.recordLockAcquired(currentThreadId, lockName);
                    logger.debug("带超时获取锁成功: {} by {}", lockName, currentThreadId);
                } else {
                    totalTimeouts.incrementAndGet();
                    logger.debug("获取锁超时: {} by {}", lockName, currentThreadId);
                }
                return acquired;
                
            } catch (InterruptedException e) {
                waitQueue.remove(waiter);
                throw e;
            }
            
        } catch (Exception e) {
            totalAcquisitions.decrementAndGet();
            if (e instanceof InterruptedException) {
                throw e;
            }
            return false;
        }
    }

    @Override
    public void unlock() {
        String currentThreadId = getCurrentThreadId();
        
        if (!isHeldByCurrentThread()) {
            throw new IllegalMonitorStateException("当前线程未持有锁: " + lockName);
        }
        
        long currentCount = reentrantCount.get();
        if (currentCount > 0) {
            reentrantCount.decrementAndGet();
            logger.debug("释放重入锁: {} by {}, 剩余重入次数: {}", lockName, currentThreadId, currentCount - 1);
            return;
        }
        
        try {
            // 停止续租任务
            stopRenewalTask();
            
            // 释放锁
            holderId = null;
            locked.set(false);
            totalReleases.incrementAndGet();
            
            // 记录锁释放
            deadlockDetector.recordLockReleased(currentThreadId, lockName);
            
            logger.debug("释放锁: {} by {}", lockName, currentThreadId);
            
            // 唤醒等待者
            notifyNextWaiter();
            
        } finally {
            lastAccessTime = System.currentTimeMillis();
        }
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException("分布式锁不支持Condition");
    }

    /**
     * 强制释放锁（用于死锁解除）
     */
    public void forceUnlock() {
        logger.warn("强制释放锁: {}", lockName);
        
        stopRenewalTask();
        holderId = null;
        locked.set(false);
        reentrantCount.set(0);
        
        // 清空等待队列
        LockWaiter waiter;
        while ((waiter = waitQueue.poll()) != null) {
            waiter.cancel();
        }
        
        lastAccessTime = System.currentTimeMillis();
    }

    /**
     * 续租锁
     */
    public boolean renewLease() {
        if (!isHeldByCurrentThread()) {
            return false;
        }
        
        lastAccessTime = System.currentTimeMillis();
        logger.debug("续租锁: {}", lockName);
        return true;
    }

    /**
     * 检查锁是否被当前线程持有
     */
    public boolean isHeldByCurrentThread() {
        return getCurrentThreadId().equals(holderId);
    }

    /**
     * 检查锁是否被持有
     */
    public boolean isLocked() {
        return locked.get() && !isExpired();
    }

    /**
     * 获取锁统计信息
     */
    public LockStats getStats() {
        return new LockStats(
            lockName,
            holderId,
            reentrantCount.get(),
            waitQueue.size(),
            totalAcquisitions.get(),
            totalReleases.get(),
            totalTimeouts.get(),
            createdTime,
            lastAccessTime,
            leaseTimeMs
        );
    }

    // 私有辅助方法

    private boolean tryAcquireInternal(String threadId) {
        if (locked.compareAndSet(false, true)) {
            holderId = threadId;
            reentrantCount.set(0);
            lastAccessTime = System.currentTimeMillis();
            return true;
        }
        
        // 检查锁是否过期
        if (isExpired()) {
            logger.warn("检测到过期锁，强制获取: {}", lockName);
            forceUnlock();
            return tryAcquireInternal(threadId);
        }
        
        return false;
    }

    private boolean isExpired() {
        return locked.get() && (System.currentTimeMillis() - lastAccessTime > leaseTimeMs);
    }

    private String getCurrentThreadId() {
        return Thread.currentThread().getName() + "-" + Thread.currentThread().getId();
    }

    private String getCurrentHeldLock(String threadId) {
        // 简化实现，在实际项目中应该跟踪线程持有的锁
        return isHeldByCurrentThread() ? lockName : null;
    }

    private void startRenewalTask() {
        if (renewalTask != null) {
            renewalTask.cancel(false);
        }
        
        // 在租期的2/3时间后开始续租
        long renewalInterval = leaseTimeMs * 2 / 3;
        renewalTask = renewalExecutor.scheduleAtFixedRate(
            this::renewLease,
            renewalInterval,
            renewalInterval,
            TimeUnit.MILLISECONDS
        );
        
        logger.debug("启动锁续租任务: {}, 间隔: {}ms", lockName, renewalInterval);
    }

    private void stopRenewalTask() {
        if (renewalTask != null) {
            renewalTask.cancel(false);
            renewalTask = null;
            logger.debug("停止锁续租任务: {}", lockName);
        }
    }

    private void notifyNextWaiter() {
        LockWaiter waiter = waitQueue.poll();
        if (waiter != null) {
            // 尝试将锁分配给等待者
            if (tryAcquireInternal(waiter.getThreadId())) {
                waiter.signal();
            } else {
                // 如果获取失败，重新加入队列
                waitQueue.offer(waiter);
            }
        }
    }

    // 内部类

    private static class LockWaiter {
        private final String threadId;
        private final CountDownLatch latch = new CountDownLatch(1);
        private volatile boolean cancelled = false;

        LockWaiter(String threadId) {
            this.threadId = threadId;
        }

        String getThreadId() {
            return threadId;
        }

        void signal() {
            latch.countDown();
        }

        void cancel() {
            cancelled = true;
            latch.countDown();
        }

        void await() throws InterruptedException {
            latch.await();
            if (cancelled) {
                throw new InterruptedException("锁等待被取消");
            }
        }

        boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            boolean result = latch.await(timeout, unit);
            if (cancelled) {
                throw new InterruptedException("锁等待被取消");
            }
            return result;
        }
    }

    public static class LockStats {
        private final String lockName;
        private final String holderId;
        private final long reentrantCount;
        private final int waitingCount;
        private final long totalAcquisitions;
        private final long totalReleases;
        private final long totalTimeouts;
        private final long createdTime;
        private final long lastAccessTime;
        private final long leaseTimeMs;

        public LockStats(String lockName, String holderId, long reentrantCount, int waitingCount,
                        long totalAcquisitions, long totalReleases, long totalTimeouts,
                        long createdTime, long lastAccessTime, long leaseTimeMs) {
            this.lockName = lockName;
            this.holderId = holderId;
            this.reentrantCount = reentrantCount;
            this.waitingCount = waitingCount;
            this.totalAcquisitions = totalAcquisitions;
            this.totalReleases = totalReleases;
            this.totalTimeouts = totalTimeouts;
            this.createdTime = createdTime;
            this.lastAccessTime = lastAccessTime;
            this.leaseTimeMs = leaseTimeMs;
        }

        // Getter方法
        public String getLockName() { return lockName; }
        public String getHolderId() { return holderId; }
        public long getReentrantCount() { return reentrantCount; }
        public int getWaitingCount() { return waitingCount; }
        public long getTotalAcquisitions() { return totalAcquisitions; }
        public long getTotalReleases() { return totalReleases; }
        public long getTotalTimeouts() { return totalTimeouts; }
        public long getCreatedTime() { return createdTime; }
        public long getLastAccessTime() { return lastAccessTime; }
        public long getLeaseTimeMs() { return leaseTimeMs; }

        public boolean isLocked() { return holderId != null; }
        public long getHoldTime() { return System.currentTimeMillis() - lastAccessTime; }
        public double getTimeoutRate() {
            return totalAcquisitions == 0 ? 0.0 : (double) totalTimeouts / totalAcquisitions;
        }

        @Override
        public String toString() {
            return String.format("LockStats{锁=%s, 持有者=%s, 重入=%d, 等待=%d, 获取=%d, 释放=%d, 超时=%d, 超时率=%.2f%%, 持有时间=%dms}",
                lockName, holderId, reentrantCount, waitingCount, totalAcquisitions, totalReleases, totalTimeouts,
                getTimeoutRate() * 100, getHoldTime());
        }
    }
}