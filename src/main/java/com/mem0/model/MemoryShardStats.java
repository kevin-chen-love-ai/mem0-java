package com.mem0.model;

import java.time.Instant;

/**
 * 内存分片统计信息
 */
public class MemoryShardStats {
    
    private final String shardId;
    private final int memoryCount;
    private final long totalSize;
    private final double avgConfidence;
    private final int totalAccessCount;
    private final Instant lastAccessTime;
    private final int conflictCount;
    private final double loadFactor;
    private final long processingTimeMs;
    private final int errorCount;
    
    public MemoryShardStats(String shardId, int memoryCount, long totalSize, 
                           double avgConfidence, int totalAccessCount, 
                           Instant lastAccessTime, int conflictCount, 
                           double loadFactor, long processingTimeMs, int errorCount) {
        this.shardId = shardId;
        this.memoryCount = memoryCount;
        this.totalSize = totalSize;
        this.avgConfidence = avgConfidence;
        this.totalAccessCount = totalAccessCount;
        this.lastAccessTime = lastAccessTime;
        this.conflictCount = conflictCount;
        this.loadFactor = loadFactor;
        this.processingTimeMs = processingTimeMs;
        this.errorCount = errorCount;
    }
    
    // Getters
    public String getShardId() { return shardId; }
    public int getMemoryCount() { return memoryCount; }
    public long getTotalSize() { return totalSize; }
    public double getAvgConfidence() { return avgConfidence; }
    public int getTotalAccessCount() { return totalAccessCount; }
    public Instant getLastAccessTime() { return lastAccessTime; }
    public int getConflictCount() { return conflictCount; }
    public double getLoadFactor() { return loadFactor; }
    public long getProcessingTimeMs() { return processingTimeMs; }
    public int getErrorCount() { return errorCount; }
    
    /**
     * 获取分片健康状态
     */
    public boolean isHealthy() {
        return loadFactor < 0.8 && errorCount < memoryCount * 0.1;
    }
    
    /**
     * 获取分片性能评分 (0-100)
     */
    public double getPerformanceScore() {
        double loadScore = Math.max(0, (1.0 - loadFactor) * 40);
        double confidenceScore = avgConfidence * 30;
        double errorScore = Math.max(0, (1.0 - (double)errorCount / Math.max(1, memoryCount)) * 30);
        return loadScore + confidenceScore + errorScore;
    }
    
    /**
     * 获取平均每个内存的大小
     */
    public double getAvgMemorySize() {
        return memoryCount > 0 ? (double) totalSize / memoryCount : 0;
    }
    
    @Override
    public String toString() {
        return String.format(
            "MemoryShardStats{shardId='%s', memories=%d, size=%d, avgConf=%.2f, " +
            "accesses=%d, conflicts=%d, loadFactor=%.2f, processingTime=%dms, errors=%d, healthy=%s}",
            shardId, memoryCount, totalSize, avgConfidence, totalAccessCount, 
            conflictCount, loadFactor, processingTimeMs, errorCount, isHealthy()
        );
    }
}