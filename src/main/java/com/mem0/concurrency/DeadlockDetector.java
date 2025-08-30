package com.mem0.concurrency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 死锁检测器
 * 检测和预防分布式锁的死锁情况
 */
public class DeadlockDetector {
    
    private static final Logger logger = LoggerFactory.getLogger(DeadlockDetector.class);
    
    // 锁依赖图：记录线程等待的锁和持有的锁
    private final Map<String, LockInfo> lockInfoMap = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> waitForGraph = new ConcurrentHashMap<>();
    
    // 统计信息
    private final AtomicInteger totalDeadlocksDetected = new AtomicInteger(0);
    private final AtomicInteger totalDeadlocksResolved = new AtomicInteger(0);
    private final AtomicLong totalDetectionRuns = new AtomicLong(0);
    
    // 检测历史
    private final List<DeadlockEvent> deadlockHistory = Collections.synchronizedList(new ArrayList<>());
    private final int maxHistorySize = 100;

    /**
     * 记录线程等待锁
     */
    public void recordWaitForLock(String threadId, String lockName, String currentLock) {
        synchronized (this) {
            // 记录锁信息
            LockInfo lockInfo = lockInfoMap.computeIfAbsent(lockName, LockInfo::new);
            lockInfo.addWaiter(threadId);
            
            // 更新等待图
            if (currentLock != null) {
                waitForGraph.computeIfAbsent(threadId, k -> ConcurrentHashMap.newKeySet()).add(lockName);
            }
            
            logger.debug("记录锁等待: 线程={}, 等待锁={}, 当前锁={}", threadId, lockName, currentLock);
        }
    }

    /**
     * 记录线程获得锁
     */
    public void recordLockAcquired(String threadId, String lockName) {
        synchronized (this) {
            // 更新锁信息
            LockInfo lockInfo = lockInfoMap.computeIfAbsent(lockName, LockInfo::new);
            lockInfo.setHolder(threadId);
            lockInfo.removeWaiter(threadId);
            
            // 从等待图中移除
            Set<String> waitingLocks = waitForGraph.get(threadId);
            if (waitingLocks != null) {
                waitingLocks.remove(lockName);
                if (waitingLocks.isEmpty()) {
                    waitForGraph.remove(threadId);
                }
            }
            
            logger.debug("记录锁获得: 线程={}, 锁={}", threadId, lockName);
        }
    }

    /**
     * 记录线程释放锁
     */
    public void recordLockReleased(String threadId, String lockName) {
        synchronized (this) {
            // 更新锁信息
            LockInfo lockInfo = lockInfoMap.get(lockName);
            if (lockInfo != null && threadId.equals(lockInfo.getHolder())) {
                lockInfo.setHolder(null);
                
                // 如果没有等待者，移除锁信息
                if (lockInfo.getWaiters().isEmpty()) {
                    lockInfoMap.remove(lockName);
                }
            }
            
            logger.debug("记录锁释放: 线程={}, 锁={}", threadId, lockName);
        }
    }

    /**
     * 检测死锁
     */
    public List<Deadlock> detectDeadlocks() {
        synchronized (this) {
            totalDetectionRuns.incrementAndGet();
            
            List<Deadlock> deadlocks = new ArrayList<>();
            Set<String> visited = new HashSet<>();
            Set<String> recursionStack = new HashSet<>();
            
            // 对每个等待的线程进行深度优先搜索
            for (String threadId : waitForGraph.keySet()) {
                if (!visited.contains(threadId)) {
                    List<String> cycle = detectCycle(threadId, visited, recursionStack, new ArrayList<>());
                    if (cycle != null && !cycle.isEmpty()) {
                        Deadlock deadlock = createDeadlock(cycle);
                        deadlocks.add(deadlock);
                        
                        // 记录死锁事件
                        DeadlockEvent event = new DeadlockEvent(System.currentTimeMillis(), deadlock);
                        recordDeadlockEvent(event);
                        
                        totalDeadlocksDetected.incrementAndGet();
                        logger.warn("检测到死锁: {}", deadlock);
                    }
                }
            }
            
            // 清理过期的锁信息
            cleanupExpiredLocks();
            
            return deadlocks;
        }
    }

    /**
     * 尝试解决死锁
     */
    public boolean resolveDeadlock(Deadlock deadlock) {
        logger.info("尝试解决死锁: {}", deadlock);
        
        try {
            // 策略1：终止优先级最低的线程
            String victimThread = selectVictimThread(deadlock);
            if (victimThread != null) {
                interruptThread(victimThread);
                totalDeadlocksResolved.incrementAndGet();
                logger.info("通过中断线程解决死锁: 受害者线程={}", victimThread);
                return true;
            }
            
            // 策略2：强制释放某个锁
            String lockToRelease = selectLockToRelease(deadlock);
            if (lockToRelease != null) {
                forceReleaseLock(lockToRelease);
                totalDeadlocksResolved.incrementAndGet();
                logger.info("通过强制释放锁解决死锁: 锁={}", lockToRelease);
                return true;
            }
            
            logger.warn("无法自动解决死锁: {}", deadlock);
            return false;
            
        } catch (Exception e) {
            logger.error("解决死锁时发生错误", e);
            return false;
        }
    }

    /**
     * 获取统计信息
     */
    public DeadlockStats getStats() {
        return new DeadlockStats(
            totalDeadlocksDetected.get(),
            totalDeadlocksResolved.get(),
            totalDetectionRuns.get(),
            lockInfoMap.size(),
            waitForGraph.size(),
            new ArrayList<>(deadlockHistory)
        );
    }

    /**
     * 预检查是否可能导致死锁
     */
    public boolean wouldCauseDeadlock(String threadId, String lockName) {
        synchronized (this) {
            // 构建临时等待图
            Map<String, Set<String>> tempGraph = new HashMap<>(waitForGraph);
            tempGraph.computeIfAbsent(threadId, k -> new HashSet<>()).add(lockName);
            
            // 检查是否会形成环
            Set<String> visited = new HashSet<>();
            Set<String> recursionStack = new HashSet<>();
            
            return detectCycle(threadId, visited, recursionStack, new ArrayList<>(), tempGraph) != null;
        }
    }

    // 私有辅助方法

    private List<String> detectCycle(String currentThread, Set<String> visited, Set<String> recursionStack, List<String> path) {
        return detectCycle(currentThread, visited, recursionStack, path, waitForGraph);
    }

    private List<String> detectCycle(String currentThread, Set<String> visited, Set<String> recursionStack, 
                                    List<String> path, Map<String, Set<String>> graph) {
        if (recursionStack.contains(currentThread)) {
            // 找到环，构建环路径
            List<String> cycle = new ArrayList<>();
            int startIndex = path.indexOf(currentThread);
            for (int i = startIndex; i < path.size(); i++) {
                cycle.add(path.get(i));
            }
            cycle.add(currentThread); // 闭环
            return cycle;
        }

        if (visited.contains(currentThread)) {
            return null;
        }

        visited.add(currentThread);
        recursionStack.add(currentThread);
        path.add(currentThread);

        // 遍历当前线程等待的所有锁
        Set<String> waitingLocks = graph.get(currentThread);
        if (waitingLocks != null) {
            for (String lockName : waitingLocks) {
                // 找到持有该锁的线程
                String holderThread = findLockHolder(lockName);
                if (holderThread != null && !holderThread.equals(currentThread)) {
                    List<String> cycle = detectCycle(holderThread, visited, recursionStack, path, graph);
                    if (cycle != null) {
                        return cycle;
                    }
                }
            }
        }

        recursionStack.remove(currentThread);
        path.remove(path.size() - 1);
        return null;
    }

    private String findLockHolder(String lockName) {
        LockInfo lockInfo = lockInfoMap.get(lockName);
        return lockInfo != null ? lockInfo.getHolder() : null;
    }

    private Deadlock createDeadlock(List<String> cycle) {
        List<DeadlockNode> nodes = new ArrayList<>();
        
        for (int i = 0; i < cycle.size() - 1; i++) {
            String threadId = cycle.get(i);
            String nextThread = cycle.get(i + 1);
            
            // 找到threadId等待的锁，该锁被nextThread持有
            String waitingLock = findWaitingLock(threadId, nextThread);
            
            nodes.add(new DeadlockNode(threadId, waitingLock, nextThread));
        }
        
        return new Deadlock(nodes, System.currentTimeMillis());
    }

    private String findWaitingLock(String waiterThread, String holderThread) {
        Set<String> waitingLocks = waitForGraph.get(waiterThread);
        if (waitingLocks != null) {
            for (String lockName : waitingLocks) {
                LockInfo lockInfo = lockInfoMap.get(lockName);
                if (lockInfo != null && holderThread.equals(lockInfo.getHolder())) {
                    return lockName;
                }
            }
        }
        return "unknown";
    }

    private void recordDeadlockEvent(DeadlockEvent event) {
        deadlockHistory.add(event);
        
        // 保持历史记录大小限制
        while (deadlockHistory.size() > maxHistorySize) {
            deadlockHistory.remove(0);
        }
    }

    private String selectVictimThread(Deadlock deadlock) {
        // 简单策略：选择第一个线程作为受害者
        List<DeadlockNode> nodes = deadlock.getNodes();
        return !nodes.isEmpty() ? nodes.get(0).getThreadId() : null;
    }

    private String selectLockToRelease(Deadlock deadlock) {
        // 简单策略：选择第一个锁进行释放
        List<DeadlockNode> nodes = deadlock.getNodes();
        return !nodes.isEmpty() ? nodes.get(0).getLockName() : null;
    }

    private void interruptThread(String threadId) {
        // 在实际实现中，这里应该中断对应的线程
        // 这里只是清理等待图中的记录
        waitForGraph.remove(threadId);
        
        // 从所有锁的等待者列表中移除该线程
        for (LockInfo lockInfo : lockInfoMap.values()) {
            lockInfo.removeWaiter(threadId);
        }
        
        logger.info("模拟中断线程: {}", threadId);
    }

    private void forceReleaseLock(String lockName) {
        // 强制释放锁
        LockInfo lockInfo = lockInfoMap.get(lockName);
        if (lockInfo != null) {
            String holder = lockInfo.getHolder();
            lockInfo.setHolder(null);
            
            logger.info("强制释放锁: {} (原持有者: {})", lockName, holder);
        }
    }

    private void cleanupExpiredLocks() {
        long now = System.currentTimeMillis();
        long expireTime = 5 * 60 * 1000; // 5分钟过期
        
        Iterator<Map.Entry<String, LockInfo>> iterator = lockInfoMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, LockInfo> entry = iterator.next();
            LockInfo lockInfo = entry.getValue();
            
            if (now - lockInfo.getLastAccessTime() > expireTime) {
                iterator.remove();
                logger.debug("清理过期锁信息: {}", entry.getKey());
            }
        }
    }

    // 内部类

    private static class LockInfo {
        private final String lockName;
        private volatile String holder;
        private final Set<String> waiters = ConcurrentHashMap.newKeySet();
        private volatile long lastAccessTime;

        LockInfo(String lockName) {
            this.lockName = lockName;
            this.lastAccessTime = System.currentTimeMillis();
        }

        String getHolder() { return holder; }
        void setHolder(String holder) { 
            this.holder = holder; 
            this.lastAccessTime = System.currentTimeMillis();
        }

        Set<String> getWaiters() { return waiters; }
        void addWaiter(String threadId) { 
            waiters.add(threadId); 
            this.lastAccessTime = System.currentTimeMillis();
        }
        void removeWaiter(String threadId) { 
            waiters.remove(threadId); 
            this.lastAccessTime = System.currentTimeMillis();
        }

        long getLastAccessTime() { return lastAccessTime; }
    }

    public static class Deadlock {
        private final List<DeadlockNode> nodes;
        private final long detectionTime;

        Deadlock(List<DeadlockNode> nodes, long detectionTime) {
            this.nodes = new ArrayList<>(nodes);
            this.detectionTime = detectionTime;
        }

        public List<DeadlockNode> getNodes() { return Collections.unmodifiableList(nodes); }
        public long getDetectionTime() { return detectionTime; }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("Deadlock{");
            for (int i = 0; i < nodes.size(); i++) {
                if (i > 0) sb.append(" -> ");
                DeadlockNode node = nodes.get(i);
                sb.append(String.format("线程%s等待锁%s(持有者:%s)", 
                    node.getThreadId(), node.getLockName(), node.getHolderThreadId()));
            }
            sb.append("}");
            return sb.toString();
        }
    }

    public static class DeadlockNode {
        private final String threadId;
        private final String lockName;
        private final String holderThreadId;

        DeadlockNode(String threadId, String lockName, String holderThreadId) {
            this.threadId = threadId;
            this.lockName = lockName;
            this.holderThreadId = holderThreadId;
        }

        public String getThreadId() { return threadId; }
        public String getLockName() { return lockName; }
        public String getHolderThreadId() { return holderThreadId; }
    }

    public static class DeadlockEvent {
        private final long timestamp;
        private final Deadlock deadlock;

        DeadlockEvent(long timestamp, Deadlock deadlock) {
            this.timestamp = timestamp;
            this.deadlock = deadlock;
        }

        public long getTimestamp() { return timestamp; }
        public Deadlock getDeadlock() { return deadlock; }
    }

    public static class DeadlockStats {
        private final int totalDeadlocksDetected;
        private final int totalDeadlocksResolved;
        private final long totalDetectionRuns;
        private final int activeLocks;
        private final int waitingThreads;
        private final List<DeadlockEvent> recentEvents;

        public DeadlockStats(int totalDeadlocksDetected, int totalDeadlocksResolved, long totalDetectionRuns,
                           int activeLocks, int waitingThreads, List<DeadlockEvent> recentEvents) {
            this.totalDeadlocksDetected = totalDeadlocksDetected;
            this.totalDeadlocksResolved = totalDeadlocksResolved;
            this.totalDetectionRuns = totalDetectionRuns;
            this.activeLocks = activeLocks;
            this.waitingThreads = waitingThreads;
            this.recentEvents = new ArrayList<>(recentEvents);
        }

        public int getTotalDeadlocksDetected() { return totalDeadlocksDetected; }
        public int getTotalDeadlocksResolved() { return totalDeadlocksResolved; }
        public long getTotalDetectionRuns() { return totalDetectionRuns; }
        public int getActiveLocks() { return activeLocks; }
        public int getWaitingThreads() { return waitingThreads; }
        public List<DeadlockEvent> getRecentEvents() { return Collections.unmodifiableList(recentEvents); }

        public double getResolutionRate() {
            return totalDeadlocksDetected == 0 ? 0.0 : (double) totalDeadlocksResolved / totalDeadlocksDetected;
        }

        @Override
        public String toString() {
            return String.format("DeadlockStats{检测=%d, 解决=%d, 解决率=%.2f%%, 检测次数=%d, 活跃锁=%d, 等待线程=%d}",
                totalDeadlocksDetected, totalDeadlocksResolved, getResolutionRate() * 100,
                totalDetectionRuns, activeLocks, waitingThreads);
        }
    }
}