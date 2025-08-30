package com.mem0.unit.config;

import com.mem0.config.EmbeddingConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 嵌入配置单元测试
 * Embedding Configuration Unit Tests
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
@DisplayName("Embedding Configuration Tests")
class EmbeddingConfigurationTest {
    
    private EmbeddingConfiguration config;
    
    @BeforeEach
    void setUp() {
        config = new EmbeddingConfiguration();
    }
    
    @Nested
    @DisplayName("Basic Configuration Tests")
    class BasicConfigurationTests {
        
        @Test
        @DisplayName("Should return correct config prefix")
        void shouldReturnCorrectConfigPrefix() {
            assertEquals("mem0.embedding", config.getConfigPrefix());
        }
        
        @Test
        @DisplayName("Should have default provider type")
        void shouldHaveDefaultProviderType() {
            String providerType = config.getProviderType();
            assertEquals("tfidf", providerType);
        }
        
        @Test
        @DisplayName("Should have default provider")
        void shouldHaveDefaultProvider() {
            String defaultProvider = config.getDefaultProvider();
            assertEquals("tfidf", defaultProvider);
        }
    }
    
    @Nested
    @DisplayName("OpenAI Configuration Tests")
    class OpenAIConfigurationTests {
        
        @Test
        @DisplayName("Should have default OpenAI API key")
        void shouldHaveDefaultOpenAIApiKey() {
            String apiKey = config.getOpenAIApiKey();
            assertEquals("", apiKey);
        }
        
        @Test
        @DisplayName("Should have default OpenAI API URL")
        void shouldHaveDefaultOpenAIApiUrl() {
            String apiUrl = config.getOpenAIApiUrl();
            assertEquals("https://api.openai.com/v1/embeddings", apiUrl);
        }
        
        @Test
        @DisplayName("Should have default OpenAI model")
        void shouldHaveDefaultOpenAIModel() {
            String model = config.getOpenAIModel();
            assertEquals("text-embedding-ada-002", model);
        }
        
        @Test
        @DisplayName("Should have default OpenAI dimension")
        void shouldHaveDefaultOpenAIDimension() {
            int dimension = config.getOpenAIDimension();
            assertEquals(1536, dimension);
        }
        
        @Test
        @DisplayName("Should have default OpenAI timeout")
        void shouldHaveDefaultOpenAITimeout() {
            int timeout = config.getOpenAITimeoutSeconds();
            assertEquals(30, timeout);
        }
        
        @Test
        @DisplayName("Should have default OpenAI max retries")
        void shouldHaveDefaultOpenAIMaxRetries() {
            int maxRetries = config.getOpenAIMaxRetries();
            assertEquals(3, maxRetries);
        }
        
        @Test
        @DisplayName("Should have OpenAI cache enabled by default")
        void shouldHaveOpenAICacheEnabledByDefault() {
            boolean cacheEnabled = config.isOpenAICacheEnabled();
            assertTrue(cacheEnabled);
        }
        
        @Test
        @DisplayName("Should have default OpenAI cache size")
        void shouldHaveDefaultOpenAICacheSize() {
            int cacheSize = config.getOpenAICacheSize();
            assertEquals(10000, cacheSize);
        }
        
        @Test
        @DisplayName("Should have default OpenAI cache TTL")
        void shouldHaveDefaultOpenAICacheTTL() {
            int cacheTTL = config.getOpenAICacheTTLMinutes();
            assertEquals(60, cacheTTL);
        }
    }
    
    @Nested
    @DisplayName("Aliyun Configuration Tests")
    class AliyunConfigurationTests {
        
        @Test
        @DisplayName("Should have default Aliyun API key")
        void shouldHaveDefaultAliyunApiKey() {
            String apiKey = config.getAliyunApiKey();
            assertEquals("", apiKey);
        }
        
        @Test
        @DisplayName("Should have default Aliyun API URL")
        void shouldHaveDefaultAliyunApiUrl() {
            String apiUrl = config.getAliyunApiUrl();
            assertEquals("https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding", apiUrl);
        }
        
        @Test
        @DisplayName("Should have default Aliyun model")
        void shouldHaveDefaultAliyunModel() {
            String model = config.getAliyunModel();
            assertEquals("text-embedding-v1", model);
        }
        
        @Test
        @DisplayName("Should have default Aliyun dimension")
        void shouldHaveDefaultAliyunDimension() {
            int dimension = config.getAliyunDimension();
            assertEquals(1536, dimension);
        }
        
        @Test
        @DisplayName("Should have default Aliyun timeout")
        void shouldHaveDefaultAliyunTimeout() {
            int timeout = config.getAliyunTimeoutSeconds();
            assertEquals(30, timeout);
        }
        
        @Test
        @DisplayName("Should have default Aliyun max retries")
        void shouldHaveDefaultAliyunMaxRetries() {
            int maxRetries = config.getAliyunMaxRetries();
            assertEquals(3, maxRetries);
        }
        
        @Test
        @DisplayName("Should have Aliyun cache enabled by default")
        void shouldHaveAliyunCacheEnabledByDefault() {
            boolean cacheEnabled = config.isAliyunCacheEnabled();
            assertTrue(cacheEnabled);
        }
        
        @Test
        @DisplayName("Should have default Aliyun cache size")
        void shouldHaveDefaultAliyunCacheSize() {
            int cacheSize = config.getAliyunCacheSize();
            assertEquals(10000, cacheSize);
        }
        
        @Test
        @DisplayName("Should have default Aliyun cache TTL")
        void shouldHaveDefaultAliyunCacheTTL() {
            int cacheTTL = config.getAliyunCacheTTLMinutes();
            assertEquals(60, cacheTTL);
        }
        
        @Test
        @DisplayName("Should have default Aliyun text type")
        void shouldHaveDefaultAliyunTextType() {
            String textType = config.getAliyunTextType();
            assertEquals("document", textType);
        }
    }
    
    @Nested
    @DisplayName("TF-IDF Configuration Tests")
    class TFIDFConfigurationTests {
        
        @Test
        @DisplayName("Should have default TF-IDF vocabulary size")
        void shouldHaveDefaultTFIDFVocabularySize() {
            int vocabularySize = config.getTFIDFVocabularySize();
            assertEquals(10000, vocabularySize);
        }
        
        @Test
        @DisplayName("Should have default TF-IDF min term frequency")
        void shouldHaveDefaultTFIDFMinTermFrequency() {
            int minTermFreq = config.getTFIDFMinTermFrequency();
            assertEquals(1, minTermFreq);
        }
        
        @Test
        @DisplayName("Should have default TF-IDF max document frequency")
        void shouldHaveDefaultTFIDFMaxDocumentFrequency() {
            double maxDocFreq = config.getTFIDFMaxDocumentFrequency();
            assertEquals(0.95, maxDocFreq, 0.001);
        }
        
        @Test
        @DisplayName("Should have default TF-IDF min document frequency")
        void shouldHaveDefaultTFIDFMinDocumentFrequency() {
            double minDocFreq = config.getTFIDFMinDocumentFrequency();
            assertEquals(0.01, minDocFreq, 0.001);
        }
        
        @Test
        @DisplayName("Should have TF-IDF normalization enabled by default")
        void shouldHaveTFIDFNormalizationEnabledByDefault() {
            boolean normalizationEnabled = config.isTFIDFNormalizationEnabled();
            assertTrue(normalizationEnabled);
        }
        
        @Test
        @DisplayName("Should have TF-IDF stopwords enabled by default")
        void shouldHaveTFIDFStopwordsEnabledByDefault() {
            boolean stopwordsEnabled = config.isTFIDFStopwordsEnabled();
            assertTrue(stopwordsEnabled);
        }
        
        @Test
        @DisplayName("Should have TF-IDF stemming disabled by default")
        void shouldHaveTFIDFStemmingDisabledByDefault() {
            boolean stemmingEnabled = config.isTFIDFStemmingEnabled();
            assertFalse(stemmingEnabled);
        }
    }
    
    @Nested
    @DisplayName("High-Performance TF-IDF Configuration Tests")
    class HighPerformanceTFIDFConfigurationTests {
        
        @Test
        @DisplayName("Should have default high-performance TF-IDF vocabulary size")
        void shouldHaveDefaultHighPerformanceTFIDFVocabularySize() {
            int vocabularySize = config.getHighPerformanceTFIDFVocabularySize();
            assertEquals(50000, vocabularySize);
        }
        
        @Test
        @DisplayName("Should have default high-performance TF-IDF thread pool size")
        void shouldHaveDefaultHighPerformanceTFIDFThreadPoolSize() {
            int threadPoolSize = config.getHighPerformanceTFIDFThreadPoolSize();
            assertEquals(8, threadPoolSize);
        }
        
        @Test
        @DisplayName("Should have default high-performance TF-IDF batch size")
        void shouldHaveDefaultHighPerformanceTFIDFBatchSize() {
            int batchSize = config.getHighPerformanceTFIDFBatchSize();
            assertEquals(1000, batchSize);
        }
    }
    
    @Nested
    @DisplayName("Mock Configuration Tests")
    class MockConfigurationTests {
        
        @Test
        @DisplayName("Should have default mock dimension")
        void shouldHaveDefaultMockDimension() {
            int dimension = config.getMockDimension();
            assertEquals(128, dimension);
        }
        
        @Test
        @DisplayName("Should have mock randomization enabled by default")
        void shouldHaveMockRandomizationEnabledByDefault() {
            boolean randomizationEnabled = config.isMockRandomizationEnabled();
            assertTrue(randomizationEnabled);
        }
        
        @Test
        @DisplayName("Should have default mock random seed")
        void shouldHaveDefaultMockRandomSeed() {
            long randomSeed = config.getMockRandomSeed();
            assertEquals(42L, randomSeed);
        }
        
        @Test
        @DisplayName("Should have default mock response delay")
        void shouldHaveDefaultMockResponseDelay() {
            int responseDelay = config.getMockResponseDelayMs();
            assertEquals(10, responseDelay);
        }
    }
    
    @Nested
    @DisplayName("Cache Configuration Tests")
    class CacheConfigurationTests {
        
        @Test
        @DisplayName("Should have cache enabled by default")
        void shouldHaveCacheEnabledByDefault() {
            boolean cacheEnabled = config.isCacheEnabled();
            assertTrue(cacheEnabled);
        }
        
        @Test
        @DisplayName("Should have default cache max size")
        void shouldHaveDefaultCacheMaxSize() {
            int maxSize = config.getCacheMaxSize();
            assertEquals(50000, maxSize);
        }
        
        @Test
        @DisplayName("Should have default cache TTL")
        void shouldHaveDefaultCacheTTL() {
            int ttl = config.getCacheTTLMinutes();
            assertEquals(120, ttl);
        }
        
        @Test
        @DisplayName("Should have cache statistics enabled by default")
        void shouldHaveCacheStatisticsEnabledByDefault() {
            boolean statisticsEnabled = config.isCacheStatisticsEnabled();
            assertTrue(statisticsEnabled);
        }
        
        @Test
        @DisplayName("Should have cache compression disabled by default")
        void shouldHaveCacheCompressionDisabledByDefault() {
            boolean compressionEnabled = config.isCacheCompressionEnabled();
            assertFalse(compressionEnabled);
        }
    }
    
    @Nested
    @DisplayName("Performance Configuration Tests")
    class PerformanceConfigurationTests {
        
        @Test
        @DisplayName("Should have async enabled by default")
        void shouldHaveAsyncEnabledByDefault() {
            boolean asyncEnabled = config.isAsyncEnabled();
            assertTrue(asyncEnabled);
        }
        
        @Test
        @DisplayName("Should have default performance thread pool size")
        void shouldHaveDefaultPerformanceThreadPoolSize() {
            int threadPoolSize = config.getPerformanceThreadPoolSize();
            assertEquals(16, threadPoolSize);
        }
        
        @Test
        @DisplayName("Should have default queue size")
        void shouldHaveDefaultQueueSize() {
            int queueSize = config.getQueueSize();
            assertEquals(1000, queueSize);
        }
        
        @Test
        @DisplayName("Should have batching enabled by default")
        void shouldHaveBatchingEnabledByDefault() {
            boolean batchingEnabled = config.isBatchingEnabled();
            assertTrue(batchingEnabled);
        }
        
        @Test
        @DisplayName("Should have default max batch size")
        void shouldHaveDefaultMaxBatchSize() {
            int maxBatchSize = config.getMaxBatchSize();
            assertEquals(100, maxBatchSize);
        }
        
        @Test
        @DisplayName("Should have default batch timeout")
        void shouldHaveDefaultBatchTimeout() {
            int batchTimeout = config.getBatchTimeoutMs();
            assertEquals(500, batchTimeout);
        }
    }
    
    @Nested
    @DisplayName("Monitoring Configuration Tests")
    class MonitoringConfigurationTests {
        
        @Test
        @DisplayName("Should have metrics enabled by default")
        void shouldHaveMetricsEnabledByDefault() {
            boolean metricsEnabled = config.isMetricsEnabled();
            assertTrue(metricsEnabled);
        }
        
        @Test
        @DisplayName("Should have health check enabled by default")
        void shouldHaveHealthCheckEnabledByDefault() {
            boolean healthCheckEnabled = config.isHealthCheckEnabled();
            assertTrue(healthCheckEnabled);
        }
        
        @Test
        @DisplayName("Should have default health check interval")
        void shouldHaveDefaultHealthCheckInterval() {
            int interval = config.getHealthCheckIntervalMinutes();
            assertEquals(5, interval);
        }
        
        @Test
        @DisplayName("Should have usage tracking enabled by default")
        void shouldHaveUsageTrackingEnabledByDefault() {
            boolean usageTrackingEnabled = config.isUsageTrackingEnabled();
            assertTrue(usageTrackingEnabled);
        }
        
        @Test
        @DisplayName("Should have default usage report interval")
        void shouldHaveDefaultUsageReportInterval() {
            int interval = config.getUsageReportIntervalMinutes();
            assertEquals(15, interval);
        }
    }
    
    @Nested
    @DisplayName("Security Configuration Tests")
    class SecurityConfigurationTests {
        
        @Test
        @DisplayName("Should have API key validation enabled by default")
        void shouldHaveApiKeyValidationEnabledByDefault() {
            boolean apiKeyValidationEnabled = config.isApiKeyValidationEnabled();
            assertTrue(apiKeyValidationEnabled);
        }
        
        @Test
        @DisplayName("Should have request signing disabled by default")
        void shouldHaveRequestSigningDisabledByDefault() {
            boolean requestSigningEnabled = config.isRequestSigningEnabled();
            assertFalse(requestSigningEnabled);
        }
        
        @Test
        @DisplayName("Should have encryption disabled by default")
        void shouldHaveEncryptionDisabledByDefault() {
            boolean encryptionEnabled = config.isEncryptionEnabled();
            assertFalse(encryptionEnabled);
        }
        
        @Test
        @DisplayName("Should have audit logging disabled by default")
        void shouldHaveAuditLoggingDisabledByDefault() {
            boolean auditLoggingEnabled = config.isAuditLoggingEnabled();
            assertFalse(auditLoggingEnabled);
        }
    }
    
    @Nested
    @DisplayName("Retry Configuration Tests")
    class RetryConfigurationTests {
        
        @Test
        @DisplayName("Should have default retry max attempts")
        void shouldHaveDefaultRetryMaxAttempts() {
            int maxAttempts = config.getRetryMaxAttempts();
            assertEquals(3, maxAttempts);
        }
        
        @Test
        @DisplayName("Should have default retry backoff strategy")
        void shouldHaveDefaultRetryBackoffStrategy() {
            String backoffStrategy = config.getRetryBackoffStrategy();
            assertEquals("exponential", backoffStrategy);
        }
        
        @Test
        @DisplayName("Should have default retry initial delay")
        void shouldHaveDefaultRetryInitialDelay() {
            int initialDelay = config.getRetryInitialDelayMs();
            assertEquals(1000, initialDelay);
        }
        
        @Test
        @DisplayName("Should have default retry max delay")
        void shouldHaveDefaultRetryMaxDelay() {
            int maxDelay = config.getRetryMaxDelayMs();
            assertEquals(30000, maxDelay);
        }
        
        @Test
        @DisplayName("Should have default retry multiplier")
        void shouldHaveDefaultRetryMultiplier() {
            double multiplier = config.getRetryMultiplier();
            assertEquals(2.0, multiplier, 0.001);
        }
    }
    
    @Nested
    @DisplayName("Circuit Breaker Configuration Tests")
    class CircuitBreakerConfigurationTests {
        
        @Test
        @DisplayName("Should have circuit breaker disabled by default")
        void shouldHaveCircuitBreakerDisabledByDefault() {
            boolean circuitBreakerEnabled = config.isCircuitBreakerEnabled();
            assertFalse(circuitBreakerEnabled);
        }
        
        @Test
        @DisplayName("Should have default circuit breaker failure threshold")
        void shouldHaveDefaultCircuitBreakerFailureThreshold() {
            int failureThreshold = config.getCircuitBreakerFailureThreshold();
            assertEquals(50, failureThreshold);
        }
        
        @Test
        @DisplayName("Should have default circuit breaker recovery timeout")
        void shouldHaveDefaultCircuitBreakerRecoveryTimeout() {
            int recoveryTimeout = config.getCircuitBreakerRecoveryTimeoutSeconds();
            assertEquals(60, recoveryTimeout);
        }
        
        @Test
        @DisplayName("Should have default circuit breaker monitoring period")
        void shouldHaveDefaultCircuitBreakerMonitoringPeriod() {
            int monitoringPeriod = config.getCircuitBreakerMonitoringPeriodSeconds();
            assertEquals(10, monitoringPeriod);
        }
    }
    
    @Nested
    @DisplayName("Load Balancing Configuration Tests")
    class LoadBalancingConfigurationTests {
        
        @Test
        @DisplayName("Should have load balancing disabled by default")
        void shouldHaveLoadBalancingDisabledByDefault() {
            boolean loadBalancingEnabled = config.isLoadBalancingEnabled();
            assertFalse(loadBalancingEnabled);
        }
        
        @Test
        @DisplayName("Should have default load balancing strategy")
        void shouldHaveDefaultLoadBalancingStrategy() {
            String strategy = config.getLoadBalancingStrategy();
            assertEquals("round_robin", strategy);
        }
        
        @Test
        @DisplayName("Should have load balancing health check enabled by default")
        void shouldHaveLoadBalancingHealthCheckEnabledByDefault() {
            boolean healthCheckEnabled = config.isLoadBalancingHealthCheckEnabled();
            assertTrue(healthCheckEnabled);
        }
        
        @Test
        @DisplayName("Should have default load balancing health check interval")
        void shouldHaveDefaultLoadBalancingHealthCheckInterval() {
            int interval = config.getLoadBalancingHealthCheckIntervalSeconds();
            assertEquals(30, interval);
        }
    }
    
    @Test
    @DisplayName("Should handle custom configuration values")
    void shouldHandleCustomConfigurationValues() {
        // Test that configuration can be dynamically updated
        config.setConfigValue("provider.type", "openai");
        assertEquals("openai", config.getProviderType());
        
        config.setConfigValue("openai.dimension", 768);
        assertEquals(768, config.getOpenAIDimension());
        
        config.setConfigValue("cache.enabled", false);
        assertFalse(config.isCacheEnabled());
    }
}