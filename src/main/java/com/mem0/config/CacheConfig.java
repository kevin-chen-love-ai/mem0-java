package com.mem0.config;

/**
 * 缓存配置类
 * 包含多级缓存的所有配置参数
 */
public class CacheConfig {
    
    // L1缓存配置
    private final int l1MaxSize;
    private final long l1TtlMs;
    private final long l1CleanupIntervalMs;
    
    // L2缓存(Redis)配置
    private final String redisHost;
    private final int redisPort;
    private final int redisDatabase;
    private final String redisPassword;
    private final long l2TtlMs;
    
    // 操作超时配置
    private final long operationTimeoutMs;
    private final long connectionTimeoutMs;
    
    // 性能配置
    private final boolean enableAsyncWrite;
    private final int maxConcurrentOperations;

    private CacheConfig(Builder builder) {
        this.l1MaxSize = builder.l1MaxSize;
        this.l1TtlMs = builder.l1TtlMs;
        this.l1CleanupIntervalMs = builder.l1CleanupIntervalMs;
        this.redisHost = builder.redisHost;
        this.redisPort = builder.redisPort;
        this.redisDatabase = builder.redisDatabase;
        this.redisPassword = builder.redisPassword;
        this.l2TtlMs = builder.l2TtlMs;
        this.operationTimeoutMs = builder.operationTimeoutMs;
        this.connectionTimeoutMs = builder.connectionTimeoutMs;
        this.enableAsyncWrite = builder.enableAsyncWrite;
        this.maxConcurrentOperations = builder.maxConcurrentOperations;
    }

    public static CacheConfig defaultConfig() {
        return new Builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getter方法
    public int getL1MaxSize() { return l1MaxSize; }
    public long getL1TtlMs() { return l1TtlMs; }
    public long getL1CleanupIntervalMs() { return l1CleanupIntervalMs; }
    public String getRedisHost() { return redisHost; }
    public int getRedisPort() { return redisPort; }
    public int getRedisDatabase() { return redisDatabase; }
    public String getRedisPassword() { return redisPassword; }
    public long getL2TtlMs() { return l2TtlMs; }
    public long getOperationTimeoutMs() { return operationTimeoutMs; }
    public long getConnectionTimeoutMs() { return connectionTimeoutMs; }
    public boolean isEnableAsyncWrite() { return enableAsyncWrite; }
    public int getMaxConcurrentOperations() { return maxConcurrentOperations; }

    @Override
    public String toString() {
        return String.format("CacheConfig{L1大小=%d, L1TTL=%dms, Redis=%s:%d, L2TTL=%dms, 异步写入=%s}",
            l1MaxSize, l1TtlMs, redisHost, redisPort, l2TtlMs, enableAsyncWrite);
    }

    public static class Builder {
        private int l1MaxSize = 10000;
        private long l1TtlMs = 1800000; // 30分钟
        private long l1CleanupIntervalMs = 300000; // 5分钟
        private String redisHost = "localhost";
        private int redisPort = 6379;
        private int redisDatabase = 0;
        private String redisPassword = null;
        private long l2TtlMs = 3600000; // 1小时
        private long operationTimeoutMs = 5000; // 5秒
        private long connectionTimeoutMs = 3000; // 3秒
        private boolean enableAsyncWrite = true;
        private int maxConcurrentOperations = 100;

        public Builder l1MaxSize(int l1MaxSize) {
            this.l1MaxSize = l1MaxSize;
            return this;
        }

        public Builder l1TtlMs(long l1TtlMs) {
            this.l1TtlMs = l1TtlMs;
            return this;
        }

        public Builder l1CleanupIntervalMs(long l1CleanupIntervalMs) {
            this.l1CleanupIntervalMs = l1CleanupIntervalMs;
            return this;
        }

        public Builder redisHost(String redisHost) {
            this.redisHost = redisHost;
            return this;
        }

        public Builder redisPort(int redisPort) {
            this.redisPort = redisPort;
            return this;
        }

        public Builder redisDatabase(int redisDatabase) {
            this.redisDatabase = redisDatabase;
            return this;
        }

        public Builder redisPassword(String redisPassword) {
            this.redisPassword = redisPassword;
            return this;
        }

        public Builder l2TtlMs(long l2TtlMs) {
            this.l2TtlMs = l2TtlMs;
            return this;
        }

        public Builder operationTimeoutMs(long operationTimeoutMs) {
            this.operationTimeoutMs = operationTimeoutMs;
            return this;
        }

        public Builder connectionTimeoutMs(long connectionTimeoutMs) {
            this.connectionTimeoutMs = connectionTimeoutMs;
            return this;
        }

        public Builder enableAsyncWrite(boolean enableAsyncWrite) {
            this.enableAsyncWrite = enableAsyncWrite;
            return this;
        }

        public Builder maxConcurrentOperations(int maxConcurrentOperations) {
            this.maxConcurrentOperations = maxConcurrentOperations;
            return this;
        }

        public CacheConfig build() {
            return new CacheConfig(this);
        }
    }
}