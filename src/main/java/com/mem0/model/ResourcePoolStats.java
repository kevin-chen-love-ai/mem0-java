package com.mem0.model;

import java.time.Instant;

/**
 * 资源池统计信息
 */
public class ResourcePoolStats {
    
    private final String poolName;
    private final int totalResources;
    private final int activeResources;
    private final int idleResources;
    private final int maxPoolSize;
    private final long totalBorrowCount;
    private final long totalReturnCount;
    private final double avgBorrowTimeMs;
    private final double avgWaitTimeMs;
    private final int waitingRequests;
    private final Instant lastActivity;
    private final long createdCount;
    private final long destroyedCount;
    private final int errorCount;
    
    public ResourcePoolStats(String poolName, int totalResources, int activeResources, 
                           int idleResources, int maxPoolSize, long totalBorrowCount, 
                           long totalReturnCount, double avgBorrowTimeMs, double avgWaitTimeMs,
                           int waitingRequests, Instant lastActivity, long createdCount,
                           long destroyedCount, int errorCount) {
        this.poolName = poolName;
        this.totalResources = totalResources;
        this.activeResources = activeResources;
        this.idleResources = idleResources;
        this.maxPoolSize = maxPoolSize;
        this.totalBorrowCount = totalBorrowCount;
        this.totalReturnCount = totalReturnCount;
        this.avgBorrowTimeMs = avgBorrowTimeMs;
        this.avgWaitTimeMs = avgWaitTimeMs;
        this.waitingRequests = waitingRequests;
        this.lastActivity = lastActivity;
        this.createdCount = createdCount;
        this.destroyedCount = destroyedCount;
        this.errorCount = errorCount;
    }
    
    // Getters
    public String getPoolName() { return poolName; }
    public int getTotalResources() { return totalResources; }
    public int getActiveResources() { return activeResources; }
    public int getIdleResources() { return idleResources; }
    public int getMaxPoolSize() { return maxPoolSize; }
    public long getTotalBorrowCount() { return totalBorrowCount; }
    public long getTotalReturnCount() { return totalReturnCount; }
    public double getAvgBorrowTimeMs() { return avgBorrowTimeMs; }
    public double getAvgWaitTimeMs() { return avgWaitTimeMs; }
    public int getWaitingRequests() { return waitingRequests; }
    public Instant getLastActivity() { return lastActivity; }
    public long getCreatedCount() { return createdCount; }
    public long getDestroyedCount() { return destroyedCount; }
    public int getErrorCount() { return errorCount; }
    
    /**
     * 获取池利用率 (0.0 - 1.0)
     */
    public double getUtilizationRate() {
        return maxPoolSize > 0 ? (double) activeResources / maxPoolSize : 0.0;
    }
    
    /**
     * 获取池健康状态
     */
    public boolean isHealthy() {
        return getUtilizationRate() < 0.9 && 
               waitingRequests == 0 && 
               errorCount < totalBorrowCount * 0.05;
    }
    
    /**
     * 获取池效率评分 (0-100)
     */
    public double getEfficiencyScore() {
        double utilizationScore = Math.min(getUtilizationRate() * 1.2, 1.0) * 40;
        double waitScore = waitingRequests == 0 ? 30 : Math.max(0, 30 - waitingRequests);
        double errorScore = errorCount == 0 ? 30 : Math.max(0, 30 - errorCount);
        return utilizationScore + waitScore + errorScore;
    }
    
    /**
     * 获取资源回收率
     */
    public double getReturnRate() {
        return totalBorrowCount > 0 ? (double) totalReturnCount / totalBorrowCount : 1.0;
    }
    
    /**
     * 获取资源泄漏数量
     */
    public long getLeakedResources() {
        return totalBorrowCount - totalReturnCount;
    }
    
    /**
     * 获取池状态描述
     */
    public String getStatusDescription() {
        if (!isHealthy()) {
            if (getUtilizationRate() >= 0.9) return "过载";
            if (waitingRequests > 0) return "拥堵";
            if (errorCount > 0) return "异常";
        }
        return "正常";
    }
    
    @Override
    public String toString() {
        return String.format(
            "ResourcePoolStats{pool='%s', total=%d, active=%d, idle=%d, max=%d, " +
            "util=%.1f%%, waiting=%d, borrows=%d, returns=%d, leaks=%d, errors=%d, status='%s'}",
            poolName, totalResources, activeResources, idleResources, maxPoolSize,
            getUtilizationRate() * 100, waitingRequests, totalBorrowCount, totalReturnCount,
            getLeakedResources(), errorCount, getStatusDescription()
        );
    }
}