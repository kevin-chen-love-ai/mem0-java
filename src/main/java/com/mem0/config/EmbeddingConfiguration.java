package com.mem0.config;

/**
 * 嵌入向量配置类 / Embedding Configuration Class
 * 
 * 该类负责管理嵌入向量服务的所有配置参数，包括不同提供者的配置、
 * 性能参数、缓存策略、监控设置等。支持多种嵌入服务提供商的统一配置管理。
 * 
 * This class manages all configuration parameters for embedding vector services,
 * including configurations for different providers, performance parameters,
 * caching strategies, monitoring settings, etc. Supports unified configuration
 * management for multiple embedding service providers.
 * 
 * 配置项说明 / Configuration Items:
 * - 提供者配置: OpenAI, 阿里云, TF-IDF等 / Provider configs: OpenAI, Aliyun, TF-IDF, etc.
 * - 性能配置: 并发、批处理、超时等 / Performance configs: concurrency, batching, timeout, etc.
 * - 缓存配置: 大小、TTL、压缩等 / Cache configs: size, TTL, compression, etc.
 * - 监控配置: 指标、健康检查、使用跟踪等 / Monitoring configs: metrics, health check, usage tracking, etc.
 * - 安全配置: 密钥验证、加密、审计等 / Security configs: key validation, encryption, auditing, etc.
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class EmbeddingConfiguration extends BaseConfiguration {
    
    public EmbeddingConfiguration() {
        super("mem0.embedding");
    }
    
    /**
     * 获取配置前缀
     * Get configuration prefix
     * 
     * @return 配置前缀 / Configuration prefix
     */
    @Override
    public String getConfigPrefix() {
        return "mem0.embedding";
    }
    
    @Override
    protected void loadDefaultConfiguration() {
        // 加载默认配置
        setConfigValue("provider.type", "tfidf");
        setConfigValue("openai.model", "text-embedding-ada-002");
        setConfigValue("openai.maxTokens", 8192);
        setConfigValue("aliyun.model", "text-embedding-v1");
        setConfigValue("aliyun.dimension", 1536);
        setConfigValue("tfidf.vectorSize", 100);
        setConfigValue("tfidf.minDocFreq", 2);
        setConfigValue("performance.cacheEnabled", true);
        setConfigValue("performance.batchSize", 100);
        setConfigValue("performance.timeout", 30000);
    }
    
    // ==================== Provider Configuration ====================
    
    /**
     * 获取默认提供者类型
     * Get default provider type
     * 
     * @return 提供者类型 / Provider type
     */
    public String getProviderType() {
        return getStringValue("provider.type", "tfidf");
    }
    
    /**
     * 获取默认提供者
     * Get default provider
     * 
     * @return 默认提供者 / Default provider
     */
    public String getDefaultProvider() {
        return getStringValue("provider.default", "tfidf");
    }
    
    // ==================== OpenAI Configuration ====================
    
    /**
     * 获取OpenAI API密钥
     * Get OpenAI API key
     * 
     * @return API密钥 / API key
     */
    public String getOpenAIApiKey() {
        return getStringValue("openai.apiKey", "");
    }
    
    /**
     * 获取OpenAI API URL
     * Get OpenAI API URL
     * 
     * @return API URL
     */
    public String getOpenAIApiUrl() {
        return getStringValue("openai.apiUrl", "https://api.openai.com/v1/embeddings");
    }
    
    /**
     * 获取OpenAI模型名称
     * Get OpenAI model name
     * 
     * @return 模型名称 / Model name
     */
    public String getOpenAIModel() {
        return getStringValue("openai.model", "text-embedding-ada-002");
    }
    
    /**
     * 获取OpenAI向量维度
     * Get OpenAI vector dimension
     * 
     * @return 向量维度 / Vector dimension
     */
    public int getOpenAIDimension() {
        return getIntValue("openai.dimension", 1536);
    }
    
    /**
     * 获取OpenAI请求超时时间（秒）
     * Get OpenAI request timeout in seconds
     * 
     * @return 超时时间 / Timeout seconds
     */
    public int getOpenAITimeoutSeconds() {
        return getIntValue("openai.timeoutSeconds", 30);
    }
    
    /**
     * 获取OpenAI最大重试次数
     * Get OpenAI maximum retry attempts
     * 
     * @return 最大重试次数 / Maximum retry attempts
     */
    public int getOpenAIMaxRetries() {
        return getIntValue("openai.maxRetries", 3);
    }
    
    /**
     * 是否启用OpenAI缓存
     * Whether to enable OpenAI caching
     * 
     * @return 是否启用缓存 / Whether caching is enabled
     */
    public boolean isOpenAICacheEnabled() {
        return getBooleanValue("openai.enableCache", true);
    }
    
    /**
     * 获取OpenAI缓存大小
     * Get OpenAI cache size
     * 
     * @return 缓存大小 / Cache size
     */
    public int getOpenAICacheSize() {
        return getIntValue("openai.cacheSize", 10000);
    }
    
    /**
     * 获取OpenAI缓存TTL（分钟）
     * Get OpenAI cache TTL in minutes
     * 
     * @return 缓存TTL / Cache TTL
     */
    public int getOpenAICacheTTLMinutes() {
        return getIntValue("openai.cacheTTLMinutes", 60);
    }
    
    // ==================== Aliyun Configuration ====================
    
    /**
     * 获取阿里云API密钥
     * Get Aliyun API key
     * 
     * @return API密钥 / API key
     */
    public String getAliyunApiKey() {
        return getStringValue("aliyun.apiKey", "");
    }
    
    /**
     * 获取阿里云API URL
     * Get Aliyun API URL
     * 
     * @return API URL
     */
    public String getAliyunApiUrl() {
        return getStringValue("aliyun.apiUrl", "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding");
    }
    
    /**
     * 获取阿里云模型名称
     * Get Aliyun model name
     * 
     * @return 模型名称 / Model name
     */
    public String getAliyunModel() {
        return getStringValue("aliyun.model", "text-embedding-v1");
    }
    
    /**
     * 获取阿里云向量维度
     * Get Aliyun vector dimension
     * 
     * @return 向量维度 / Vector dimension
     */
    public int getAliyunDimension() {
        return getIntValue("aliyun.dimension", 1536);
    }
    
    /**
     * 获取阿里云请求超时时间（秒）
     * Get Aliyun request timeout in seconds
     * 
     * @return 超时时间 / Timeout seconds
     */
    public int getAliyunTimeoutSeconds() {
        return getIntValue("aliyun.timeoutSeconds", 30);
    }
    
    /**
     * 获取阿里云最大重试次数
     * Get Aliyun maximum retry attempts
     * 
     * @return 最大重试次数 / Maximum retry attempts
     */
    public int getAliyunMaxRetries() {
        return getIntValue("aliyun.maxRetries", 3);
    }
    
    /**
     * 是否启用阿里云缓存
     * Whether to enable Aliyun caching
     * 
     * @return 是否启用缓存 / Whether caching is enabled
     */
    public boolean isAliyunCacheEnabled() {
        return getBooleanValue("aliyun.enableCache", true);
    }
    
    /**
     * 获取阿里云缓存大小
     * Get Aliyun cache size
     * 
     * @return 缓存大小 / Cache size
     */
    public int getAliyunCacheSize() {
        return getIntValue("aliyun.cacheSize", 10000);
    }
    
    /**
     * 获取阿里云缓存TTL（分钟）
     * Get Aliyun cache TTL in minutes
     * 
     * @return 缓存TTL / Cache TTL
     */
    public int getAliyunCacheTTLMinutes() {
        return getIntValue("aliyun.cacheTTLMinutes", 60);
    }
    
    /**
     * 获取阿里云文本类型
     * Get Aliyun text type
     * 
     * @return 文本类型 / Text type
     */
    public String getAliyunTextType() {
        return getStringValue("aliyun.textType", "document");
    }
    
    // ==================== TF-IDF Configuration ====================
    
    /**
     * 获取TF-IDF词汇表大小
     * Get TF-IDF vocabulary size
     * 
     * @return 词汇表大小 / Vocabulary size
     */
    public int getTFIDFVocabularySize() {
        return getIntValue("tfidf.vocabularySize", 10000);
    }
    
    /**
     * 获取TF-IDF最小词频
     * Get TF-IDF minimum term frequency
     * 
     * @return 最小词频 / Minimum term frequency
     */
    public int getTFIDFMinTermFrequency() {
        return getIntValue("tfidf.minTermFrequency", 1);
    }
    
    /**
     * 获取TF-IDF最大文档频率
     * Get TF-IDF maximum document frequency
     * 
     * @return 最大文档频率 / Maximum document frequency
     */
    public double getTFIDFMaxDocumentFrequency() {
        return getDoubleValue("tfidf.maxDocumentFrequency", 0.95);
    }
    
    /**
     * 获取TF-IDF最小文档频率
     * Get TF-IDF minimum document frequency
     * 
     * @return 最小文档频率 / Minimum document frequency
     */
    public double getTFIDFMinDocumentFrequency() {
        return getDoubleValue("tfidf.minDocumentFrequency", 0.01);
    }
    
    /**
     * 是否启用TF-IDF标准化
     * Whether to enable TF-IDF normalization
     * 
     * @return 是否启用标准化 / Whether normalization is enabled
     */
    public boolean isTFIDFNormalizationEnabled() {
        return getBooleanValue("tfidf.enableNormalization", true);
    }
    
    /**
     * 是否启用TF-IDF停用词过滤
     * Whether to enable TF-IDF stopwords filtering
     * 
     * @return 是否启用停用词过滤 / Whether stopwords filtering is enabled
     */
    public boolean isTFIDFStopwordsEnabled() {
        return getBooleanValue("tfidf.enableStopwords", true);
    }
    
    /**
     * 是否启用TF-IDF词干提取
     * Whether to enable TF-IDF stemming
     * 
     * @return 是否启用词干提取 / Whether stemming is enabled
     */
    public boolean isTFIDFStemmingEnabled() {
        return getBooleanValue("tfidf.enableStemming", false);
    }
    
    // ==================== High-Performance TF-IDF Configuration ====================
    
    /**
     * 获取高性能TF-IDF词汇表大小
     * Get high-performance TF-IDF vocabulary size
     * 
     * @return 词汇表大小 / Vocabulary size
     */
    public int getHighPerformanceTFIDFVocabularySize() {
        return getIntValue("highPerformanceTfidf.vocabularySize", 50000);
    }
    
    /**
     * 获取高性能TF-IDF线程池大小
     * Get high-performance TF-IDF thread pool size
     * 
     * @return 线程池大小 / Thread pool size
     */
    public int getHighPerformanceTFIDFThreadPoolSize() {
        return getIntValue("highPerformanceTfidf.threadPoolSize", 8);
    }
    
    /**
     * 获取高性能TF-IDF批处理大小
     * Get high-performance TF-IDF batch size
     * 
     * @return 批处理大小 / Batch size
     */
    public int getHighPerformanceTFIDFBatchSize() {
        return getIntValue("highPerformanceTfidf.batchSize", 1000);
    }
    
    // ==================== Mock Configuration ====================
    
    /**
     * 获取模拟提供者向量维度
     * Get mock provider vector dimension
     * 
     * @return 向量维度 / Vector dimension
     */
    public int getMockDimension() {
        return getIntValue("mock.dimension", 128);
    }
    
    /**
     * 是否启用模拟提供者随机化
     * Whether to enable mock provider randomization
     * 
     * @return 是否启用随机化 / Whether randomization is enabled
     */
    public boolean isMockRandomizationEnabled() {
        return getBooleanValue("mock.enableRandomization", true);
    }
    
    /**
     * 获取模拟提供者随机种子
     * Get mock provider random seed
     * 
     * @return 随机种子 / Random seed
     */
    public long getMockRandomSeed() {
        return getLongValue("mock.randomSeed", 42L);
    }
    
    /**
     * 获取模拟提供者响应延迟（毫秒）
     * Get mock provider response delay in milliseconds
     * 
     * @return 响应延迟 / Response delay
     */
    public int getMockResponseDelayMs() {
        return getIntValue("mock.responseDelayMs", 10);
    }
    
    // ==================== Cache Configuration ====================
    
    /**
     * 是否启用缓存
     * Whether to enable caching
     * 
     * @return 是否启用缓存 / Whether caching is enabled
     */
    public boolean isCacheEnabled() {
        return getBooleanValue("cache.enabled", true);
    }
    
    /**
     * 获取缓存最大大小
     * Get cache maximum size
     * 
     * @return 缓存最大大小 / Cache maximum size
     */
    public int getCacheMaxSize() {
        return getIntValue("cache.maxSize", 50000);
    }
    
    /**
     * 获取缓存TTL（分钟）
     * Get cache TTL in minutes
     * 
     * @return 缓存TTL / Cache TTL
     */
    public int getCacheTTLMinutes() {
        return getIntValue("cache.ttlMinutes", 120);
    }
    
    /**
     * 是否启用缓存统计
     * Whether to enable cache statistics
     * 
     * @return 是否启用缓存统计 / Whether cache statistics is enabled
     */
    public boolean isCacheStatisticsEnabled() {
        return getBooleanValue("cache.enableStatistics", true);
    }
    
    /**
     * 是否启用缓存压缩
     * Whether to enable cache compression
     * 
     * @return 是否启用缓存压缩 / Whether cache compression is enabled
     */
    public boolean isCacheCompressionEnabled() {
        return getBooleanValue("cache.enableCompression", false);
    }
    
    // ==================== Performance Configuration ====================
    
    /**
     * 是否启用异步处理
     * Whether to enable async processing
     * 
     * @return 是否启用异步处理 / Whether async processing is enabled
     */
    public boolean isAsyncEnabled() {
        return getBooleanValue("performance.enableAsync", true);
    }
    
    /**
     * 获取性能线程池大小
     * Get performance thread pool size
     * 
     * @return 线程池大小 / Thread pool size
     */
    public int getPerformanceThreadPoolSize() {
        return getIntValue("performance.threadPoolSize", 16);
    }
    
    /**
     * 获取队列大小
     * Get queue size
     * 
     * @return 队列大小 / Queue size
     */
    public int getQueueSize() {
        return getIntValue("performance.queueSize", 1000);
    }
    
    /**
     * 是否启用批处理
     * Whether to enable batching
     * 
     * @return 是否启用批处理 / Whether batching is enabled
     */
    public boolean isBatchingEnabled() {
        return getBooleanValue("performance.enableBatching", true);
    }
    
    /**
     * 获取最大批处理大小
     * Get maximum batch size
     * 
     * @return 最大批处理大小 / Maximum batch size
     */
    public int getMaxBatchSize() {
        return getIntValue("performance.maxBatchSize", 100);
    }
    
    /**
     * 获取批处理超时时间（毫秒）
     * Get batch timeout in milliseconds
     * 
     * @return 批处理超时时间 / Batch timeout
     */
    public int getBatchTimeoutMs() {
        return getIntValue("performance.batchTimeoutMs", 500);
    }
    
    // ==================== Monitoring Configuration ====================
    
    /**
     * 是否启用指标监控
     * Whether to enable metrics monitoring
     * 
     * @return 是否启用指标监控 / Whether metrics monitoring is enabled
     */
    public boolean isMetricsEnabled() {
        return getBooleanValue("monitoring.enableMetrics", true);
    }
    
    /**
     * 是否启用健康检查
     * Whether to enable health check
     * 
     * @return 是否启用健康检查 / Whether health check is enabled
     */
    public boolean isHealthCheckEnabled() {
        return getBooleanValue("monitoring.enableHealthCheck", true);
    }
    
    /**
     * 获取健康检查间隔（分钟）
     * Get health check interval in minutes
     * 
     * @return 健康检查间隔 / Health check interval
     */
    public int getHealthCheckIntervalMinutes() {
        return getIntValue("monitoring.healthCheckIntervalMinutes", 5);
    }
    
    /**
     * 是否启用使用跟踪
     * Whether to enable usage tracking
     * 
     * @return 是否启用使用跟踪 / Whether usage tracking is enabled
     */
    public boolean isUsageTrackingEnabled() {
        return getBooleanValue("monitoring.enableUsageTracking", true);
    }
    
    /**
     * 获取使用报告间隔（分钟）
     * Get usage report interval in minutes
     * 
     * @return 使用报告间隔 / Usage report interval
     */
    public int getUsageReportIntervalMinutes() {
        return getIntValue("monitoring.usageReportIntervalMinutes", 15);
    }
    
    // ==================== Security Configuration ====================
    
    /**
     * 是否启用API密钥验证
     * Whether to enable API key validation
     * 
     * @return 是否启用API密钥验证 / Whether API key validation is enabled
     */
    public boolean isApiKeyValidationEnabled() {
        return getBooleanValue("security.enableApiKeyValidation", true);
    }
    
    /**
     * 是否启用请求签名
     * Whether to enable request signing
     * 
     * @return 是否启用请求签名 / Whether request signing is enabled
     */
    public boolean isRequestSigningEnabled() {
        return getBooleanValue("security.enableRequestSigning", false);
    }
    
    /**
     * 是否启用加密
     * Whether to enable encryption
     * 
     * @return 是否启用加密 / Whether encryption is enabled
     */
    public boolean isEncryptionEnabled() {
        return getBooleanValue("security.enableEncryption", false);
    }
    
    /**
     * 是否启用审计日志
     * Whether to enable audit logging
     * 
     * @return 是否启用审计日志 / Whether audit logging is enabled
     */
    public boolean isAuditLoggingEnabled() {
        return getBooleanValue("security.enableAuditLogging", false);
    }
    
    // ==================== Retry Configuration ====================
    
    /**
     * 获取最大重试次数
     * Get maximum retry attempts
     * 
     * @return 最大重试次数 / Maximum retry attempts
     */
    public int getRetryMaxAttempts() {
        return getIntValue("retry.maxAttempts", 3);
    }
    
    /**
     * 获取退避策略
     * Get backoff strategy
     * 
     * @return 退避策略 / Backoff strategy
     */
    public String getRetryBackoffStrategy() {
        return getStringValue("retry.backoffStrategy", "exponential");
    }
    
    /**
     * 获取初始延迟时间（毫秒）
     * Get initial delay in milliseconds
     * 
     * @return 初始延迟时间 / Initial delay
     */
    public int getRetryInitialDelayMs() {
        return getIntValue("retry.initialDelayMs", 1000);
    }
    
    /**
     * 获取最大延迟时间（毫秒）
     * Get maximum delay in milliseconds
     * 
     * @return 最大延迟时间 / Maximum delay
     */
    public int getRetryMaxDelayMs() {
        return getIntValue("retry.maxDelayMs", 30000);
    }
    
    /**
     * 获取延迟倍增因子
     * Get delay multiplier
     * 
     * @return 延迟倍增因子 / Delay multiplier
     */
    public double getRetryMultiplier() {
        return getDoubleValue("retry.multiplier", 2.0);
    }
    
    // ==================== Circuit Breaker Configuration ====================
    
    /**
     * 是否启用熔断器
     * Whether to enable circuit breaker
     * 
     * @return 是否启用熔断器 / Whether circuit breaker is enabled
     */
    public boolean isCircuitBreakerEnabled() {
        return getBooleanValue("circuitBreaker.enabled", false);
    }
    
    /**
     * 获取熔断器失败阈值
     * Get circuit breaker failure threshold
     * 
     * @return 失败阈值 / Failure threshold
     */
    public int getCircuitBreakerFailureThreshold() {
        return getIntValue("circuitBreaker.failureThreshold", 50);
    }
    
    /**
     * 获取熔断器恢复超时时间（秒）
     * Get circuit breaker recovery timeout in seconds
     * 
     * @return 恢复超时时间 / Recovery timeout
     */
    public int getCircuitBreakerRecoveryTimeoutSeconds() {
        return getIntValue("circuitBreaker.recoveryTimeoutSeconds", 60);
    }
    
    /**
     * 获取熔断器监控周期（秒）
     * Get circuit breaker monitoring period in seconds
     * 
     * @return 监控周期 / Monitoring period
     */
    public int getCircuitBreakerMonitoringPeriodSeconds() {
        return getIntValue("circuitBreaker.monitoringPeriodSeconds", 10);
    }
    
    // ==================== Load Balancing Configuration ====================
    
    /**
     * 是否启用负载均衡
     * Whether to enable load balancing
     * 
     * @return 是否启用负载均衡 / Whether load balancing is enabled
     */
    public boolean isLoadBalancingEnabled() {
        return getBooleanValue("loadBalancing.enabled", false);
    }
    
    /**
     * 获取负载均衡策略
     * Get load balancing strategy
     * 
     * @return 负载均衡策略 / Load balancing strategy
     */
    public String getLoadBalancingStrategy() {
        return getStringValue("loadBalancing.strategy", "round_robin");
    }
    
    /**
     * 是否启用负载均衡健康检查
     * Whether to enable load balancing health check
     * 
     * @return 是否启用健康检查 / Whether health check is enabled
     */
    public boolean isLoadBalancingHealthCheckEnabled() {
        return getBooleanValue("loadBalancing.healthCheckEnabled", true);
    }
    
    /**
     * 获取负载均衡健康检查间隔（秒）
     * Get load balancing health check interval in seconds
     * 
     * @return 健康检查间隔 / Health check interval
     */
    public int getLoadBalancingHealthCheckIntervalSeconds() {
        return getIntValue("loadBalancing.healthCheckIntervalSeconds", 30);
    }
}