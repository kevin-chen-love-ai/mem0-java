package com.mem0.benchmark;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 基准测试结果
 * 包含测试数据和统计信息
 */
public class BenchmarkResult {
    
    private final String testName;
    private final TestType testType;
    private final List<Long> rawData;
    private final long totalTimeMs;
    private final int concurrencyLevel;
    private final long timestamp;
    
    // 计算的统计指标
    private final BenchmarkStatistics statistics;

    public BenchmarkResult(String testName, TestType testType, List<Long> rawData, 
                          long totalTimeMs, int concurrencyLevel) {
        this.testName = testName;
        this.testType = testType;
        this.rawData = new ArrayList<>(rawData);
        this.totalTimeMs = totalTimeMs;
        this.concurrencyLevel = concurrencyLevel;
        this.timestamp = System.currentTimeMillis();
        this.statistics = calculateStatistics(rawData);
    }

    private BenchmarkStatistics calculateStatistics(List<Long> data) {
        if (data == null || data.isEmpty()) {
            return new BenchmarkStatistics(0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
        
        List<Long> sorted = data.stream().sorted().collect(Collectors.toList());
        int size = sorted.size();
        
        long min = sorted.get(0);
        long max = sorted.get(size - 1);
        double mean = sorted.stream().mapToLong(Long::longValue).average().orElse(0.0);
        
        // 计算中位数
        long median;
        if (size % 2 == 0) {
            median = (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2;
        } else {
            median = sorted.get(size / 2);
        }
        
        // 计算百分位数
        long p95 = getPercentile(sorted, 0.95);
        long p99 = getPercentile(sorted, 0.99);
        long p999 = getPercentile(sorted, 0.999);
        
        // 计算标准差
        double variance = data.stream()
            .mapToDouble(value -> Math.pow(value - mean, 2))
            .average()
            .orElse(0.0);
        double stdDev = Math.sqrt(variance);
        
        return new BenchmarkStatistics(min, max, mean, median, stdDev, p95, p99, p999, size);
    }

    private long getPercentile(List<Long> sorted, double percentile) {
        if (sorted.isEmpty()) return 0;
        
        int index = (int) Math.ceil(percentile * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(index);
    }

    // Getter方法
    public String getTestName() { return testName; }
    public TestType getTestType() { return testType; }
    public List<Long> getRawData() { return Collections.unmodifiableList(rawData); }
    public long getTotalTimeMs() { return totalTimeMs; }
    public int getConcurrencyLevel() { return concurrencyLevel; }
    public long getTimestamp() { return timestamp; }
    public BenchmarkStatistics getStatistics() { return statistics; }

    public double getThroughput() {
        if (totalTimeMs <= 0 || rawData.isEmpty()) {
            return 0.0;
        }
        return (double) rawData.size() * 1000.0 / totalTimeMs;
    }

    public boolean isSuccessful() {
        return !rawData.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("%s [%s] - 样本数: %d, 总时间: %dms, 并发: %d, 平均: %.2fms, P95: %dms, P99: %dms",
            testName, testType, rawData.size(), totalTimeMs, concurrencyLevel,
            statistics.getMean(), statistics.getP95(), statistics.getP99());
    }

    // 测试类型枚举
    public enum TestType {
        LATENCY("延迟测试"),
        THROUGHPUT("吞吐量测试"),
        CONCURRENCY("并发测试"),
        MEMORY_STRESS("内存压力测试"),
        MEMORY_LEAK("内存泄漏测试"),
        CACHE_PERFORMANCE("缓存性能测试"),
        CACHE_WARMUP("缓存预热测试"),
        SCALABILITY("扩展性测试"),
        SUSTAINED_THROUGHPUT("持续吞吐量测试"),
        LATENCY_PERCENTILE("延迟百分位测试");

        private final String description;

        TestType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 统计信息类
    public static class BenchmarkStatistics {
        private final long min;
        private final long max;
        private final double mean;
        private final long median;
        private final double stdDev;
        private final long p95;
        private final long p99;
        private final long p999;
        private final int sampleCount;

        public BenchmarkStatistics(long min, long max, double mean, long median, double stdDev,
                                 long p95, long p99, long p999, int sampleCount) {
            this.min = min;
            this.max = max;
            this.mean = mean;
            this.median = median;
            this.stdDev = stdDev;
            this.p95 = p95;
            this.p99 = p99;
            this.p999 = p999;
            this.sampleCount = sampleCount;
        }

        // Getter方法
        public long getMin() { return min; }
        public long getMax() { return max; }
        public double getMean() { return mean; }
        public long getMedian() { return median; }
        public double getStdDev() { return stdDev; }
        public long getP95() { return p95; }
        public long getP99() { return p99; }
        public long getP999() { return p999; }
        public int getSampleCount() { return sampleCount; }

        public double getCoefficientOfVariation() {
            return mean != 0 ? stdDev / mean : 0.0;
        }

        @Override
        public String toString() {
            return String.format("Statistics{min=%d, max=%d, mean=%.2f, median=%d, stdDev=%.2f, P95=%d, P99=%d, P99.9=%d, samples=%d}",
                min, max, mean, median, stdDev, p95, p99, p999, sampleCount);
        }
    }
}