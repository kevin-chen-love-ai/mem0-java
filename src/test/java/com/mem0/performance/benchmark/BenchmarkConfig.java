package com.mem0.performance.benchmark;

/**
 * 基准测试配置
 * 包含所有测试参数和设置
 */
public class BenchmarkConfig {
    
    // 基础测试参数
    private final int warmupIterations;
    private final int measurementIterations;
    private final int operationsPerThread;
    private final int concurrencyLevel;
    
    // 超时设置
    private final long operationTimeoutMs;
    private final long concurrentTestTimeoutMs;
    private final long throughputTestTimeoutMs;
    private final long cacheWarmupTimeoutMs;
    
    // 持续测试参数
    private final long sustainedTestDurationMs;
    
    // 内存测试参数
    private final long memoryTestIntervalMs;
    private final int maxMemoryTestObjects;

    private BenchmarkConfig(Builder builder) {
        this.warmupIterations = builder.warmupIterations;
        this.measurementIterations = builder.measurementIterations;
        this.operationsPerThread = builder.operationsPerThread;
        this.concurrencyLevel = builder.concurrencyLevel;
        this.operationTimeoutMs = builder.operationTimeoutMs;
        this.concurrentTestTimeoutMs = builder.concurrentTestTimeoutMs;
        this.throughputTestTimeoutMs = builder.throughputTestTimeoutMs;
        this.cacheWarmupTimeoutMs = builder.cacheWarmupTimeoutMs;
        this.sustainedTestDurationMs = builder.sustainedTestDurationMs;
        this.memoryTestIntervalMs = builder.memoryTestIntervalMs;
        this.maxMemoryTestObjects = builder.maxMemoryTestObjects;
    }

    public static BenchmarkConfig defaultConfig() {
        return new Builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getter方法
    public int getWarmupIterations() { return warmupIterations; }
    public int getMeasurementIterations() { return measurementIterations; }
    public int getOperationsPerThread() { return operationsPerThread; }
    public int getConcurrencyLevel() { return concurrencyLevel; }
    public long getOperationTimeoutMs() { return operationTimeoutMs; }
    public long getConcurrentTestTimeoutMs() { return concurrentTestTimeoutMs; }
    public long getThroughputTestTimeoutMs() { return throughputTestTimeoutMs; }
    public long getCacheWarmupTimeoutMs() { return cacheWarmupTimeoutMs; }
    public long getSustainedTestDurationMs() { return sustainedTestDurationMs; }
    public long getMemoryTestIntervalMs() { return memoryTestIntervalMs; }
    public int getMaxMemoryTestObjects() { return maxMemoryTestObjects; }

    @Override
    public String toString() {
        return String.format("BenchmarkConfig{预热=%d, 测量=%d, 每线程操作=%d, 并发度=%d, 操作超时=%dms, 持续测试=%dms}",
            warmupIterations, measurementIterations, operationsPerThread, concurrencyLevel, 
            operationTimeoutMs, sustainedTestDurationMs);
    }

    public static class Builder {
        private int warmupIterations = 100;
        private int measurementIterations = 1000;
        private int operationsPerThread = 100;
        private int concurrencyLevel = 8;
        private long operationTimeoutMs = 5000;
        private long concurrentTestTimeoutMs = 300000; // 5分钟
        private long throughputTestTimeoutMs = 60000;  // 1分钟
        private long cacheWarmupTimeoutMs = 30000;     // 30秒
        private long sustainedTestDurationMs = 120000; // 2分钟
        private long memoryTestIntervalMs = 1000;      // 1秒
        private int maxMemoryTestObjects = 50000;

        public Builder warmupIterations(int warmupIterations) {
            this.warmupIterations = warmupIterations;
            return this;
        }

        public Builder measurementIterations(int measurementIterations) {
            this.measurementIterations = measurementIterations;
            return this;
        }

        public Builder operationsPerThread(int operationsPerThread) {
            this.operationsPerThread = operationsPerThread;
            return this;
        }

        public Builder concurrencyLevel(int concurrencyLevel) {
            this.concurrencyLevel = concurrencyLevel;
            return this;
        }

        public Builder operationTimeoutMs(long operationTimeoutMs) {
            this.operationTimeoutMs = operationTimeoutMs;
            return this;
        }

        public Builder concurrentTestTimeoutMs(long concurrentTestTimeoutMs) {
            this.concurrentTestTimeoutMs = concurrentTestTimeoutMs;
            return this;
        }

        public Builder throughputTestTimeoutMs(long throughputTestTimeoutMs) {
            this.throughputTestTimeoutMs = throughputTestTimeoutMs;
            return this;
        }

        public Builder cacheWarmupTimeoutMs(long cacheWarmupTimeoutMs) {
            this.cacheWarmupTimeoutMs = cacheWarmupTimeoutMs;
            return this;
        }

        public Builder sustainedTestDurationMs(long sustainedTestDurationMs) {
            this.sustainedTestDurationMs = sustainedTestDurationMs;
            return this;
        }

        public Builder memoryTestIntervalMs(long memoryTestIntervalMs) {
            this.memoryTestIntervalMs = memoryTestIntervalMs;
            return this;
        }

        public Builder maxMemoryTestObjects(int maxMemoryTestObjects) {
            this.maxMemoryTestObjects = maxMemoryTestObjects;
            return this;
        }

        public BenchmarkConfig build() {
            return new BenchmarkConfig(this);
        }
    }
}