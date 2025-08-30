package com.mem0.unit.embedding;

import com.mem0.embedding.EmbeddingProvider;
import com.mem0.embedding.EmbeddingProviderFactory;
import com.mem0.embedding.EmbeddingProviderFactory.ProviderType;
import com.mem0.embedding.impl.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 嵌入提供者工厂单元测试
 * Embedding Provider Factory Unit Tests
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
@DisplayName("Embedding Provider Factory Tests")
class EmbeddingProviderFactoryTest {
    
    @BeforeEach
    void setUp() {
        // Clear cache before each test
        EmbeddingProviderFactory.clearCache();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up after each test
        EmbeddingProviderFactory.closeAll();
    }
    
    @Nested
    @DisplayName("OpenAI Provider Creation Tests")
    class OpenAIProviderCreationTests {
        
        @Test
        @DisplayName("Should create OpenAI provider with API key")
        void shouldCreateOpenAIProviderWithApiKey() {
            EmbeddingProvider provider = EmbeddingProviderFactory.createOpenAI("test-api-key");
            
            assertNotNull(provider);
            assertInstanceOf(OpenAIEmbeddingProvider.class, provider);
            assertEquals("OpenAI", provider.getProviderName());
            assertEquals(1536, provider.getDimension());
        }
        
        @Test
        @DisplayName("Should create OpenAI provider with custom model")
        void shouldCreateOpenAIProviderWithCustomModel() {
            EmbeddingProvider provider = EmbeddingProviderFactory.createOpenAI("test-api-key", "text-embedding-3-small");
            
            assertNotNull(provider);
            assertInstanceOf(OpenAIEmbeddingProvider.class, provider);
            assertEquals("OpenAI", provider.getProviderName());
        }
        
        @Test
        @DisplayName("Should cache OpenAI provider instances")
        void shouldCacheOpenAIProviderInstances() {
            EmbeddingProvider provider1 = EmbeddingProviderFactory.createOpenAI("test-api-key", "text-embedding-ada-002");
            EmbeddingProvider provider2 = EmbeddingProviderFactory.createOpenAI("test-api-key", "text-embedding-ada-002");
            
            assertSame(provider1, provider2);
            assertEquals(1, EmbeddingProviderFactory.getCachedProviderCount());
        }
        
        @Test
        @DisplayName("Should create different instances for different models")
        void shouldCreateDifferentInstancesForDifferentModels() {
            EmbeddingProvider provider1 = EmbeddingProviderFactory.createOpenAI("test-api-key", "text-embedding-ada-002");
            EmbeddingProvider provider2 = EmbeddingProviderFactory.createOpenAI("test-api-key", "text-embedding-3-small");
            
            assertNotSame(provider1, provider2);
            assertEquals(2, EmbeddingProviderFactory.getCachedProviderCount());
        }
    }
    
    @Nested
    @DisplayName("Aliyun Provider Creation Tests")
    class AliyunProviderCreationTests {
        
        @Test
        @DisplayName("Should create Aliyun provider with API key")
        void shouldCreateAliyunProviderWithApiKey() {
            EmbeddingProvider provider = EmbeddingProviderFactory.createAliyun("test-api-key");
            
            assertNotNull(provider);
            assertInstanceOf(AliyunEmbeddingProvider.class, provider);
            assertEquals("Aliyun", provider.getProviderName());
            assertEquals(1536, provider.getDimension());
        }
        
        @Test
        @DisplayName("Should create Aliyun provider with custom model")
        void shouldCreateAliyunProviderWithCustomModel() {
            EmbeddingProvider provider = EmbeddingProviderFactory.createAliyun("test-api-key", "text-embedding-v2");
            
            assertNotNull(provider);
            assertInstanceOf(AliyunEmbeddingProvider.class, provider);
            assertEquals("Aliyun", provider.getProviderName());
        }
        
        @Test
        @DisplayName("Should cache Aliyun provider instances")
        void shouldCacheAliyunProviderInstances() {
            EmbeddingProvider provider1 = EmbeddingProviderFactory.createAliyun("test-api-key", "text-embedding-v1");
            EmbeddingProvider provider2 = EmbeddingProviderFactory.createAliyun("test-api-key", "text-embedding-v1");
            
            assertSame(provider1, provider2);
            assertEquals(1, EmbeddingProviderFactory.getCachedProviderCount());
        }
    }
    
    @Nested
    @DisplayName("Local Provider Creation Tests")
    class LocalProviderCreationTests {
        
        @Test
        @DisplayName("Should create TF-IDF provider")
        void shouldCreateTFIDFProvider() {
            EmbeddingProvider provider = EmbeddingProviderFactory.createTFIDF();
            
            assertNotNull(provider);
            assertInstanceOf(SimpleTFIDFEmbeddingProvider.class, provider);
            assertEquals("SimpleTFIDF", provider.getProviderName());
        }
        
        @Test
        @DisplayName("Should create high-performance TF-IDF provider")
        void shouldCreateHighPerformanceTFIDFProvider() {
            EmbeddingProvider provider = EmbeddingProviderFactory.createHighPerformanceTFIDF();
            
            assertNotNull(provider);
            assertInstanceOf(HighPerformanceTFIDFProvider.class, provider);
        }
        
        @Test
        @DisplayName("Should create mock provider")
        void shouldCreateMockProvider() {
            EmbeddingProvider provider = EmbeddingProviderFactory.createMock();
            
            assertNotNull(provider);
            assertInstanceOf(MockEmbeddingProvider.class, provider);
            assertEquals("Mock", provider.getProviderName());
        }
        
        @Test
        @DisplayName("Should cache local providers")
        void shouldCacheLocalProviders() {
            EmbeddingProvider provider1 = EmbeddingProviderFactory.createTFIDF();
            EmbeddingProvider provider2 = EmbeddingProviderFactory.createTFIDF();
            
            assertSame(provider1, provider2);
            assertEquals(1, EmbeddingProviderFactory.getCachedProviderCount());
        }
    }
    
    @Nested
    @DisplayName("Provider Type Enum Tests")
    class ProviderTypeEnumTests {
        
        @Test
        @DisplayName("Should create provider using enum - OpenAI")
        void shouldCreateProviderUsingEnumOpenAI() {
            EmbeddingProvider provider = EmbeddingProviderFactory.create(ProviderType.OPENAI, "test-api-key");
            
            assertNotNull(provider);
            assertEquals("OpenAI", provider.getProviderName());
        }
        
        @Test
        @DisplayName("Should create provider using enum - Aliyun")
        void shouldCreateProviderUsingEnumAliyun() {
            EmbeddingProvider provider = EmbeddingProviderFactory.create(ProviderType.ALIYUN, "test-api-key");
            
            assertNotNull(provider);
            assertEquals("Aliyun", provider.getProviderName());
        }
        
        @Test
        @DisplayName("Should create provider using enum - TF-IDF")
        void shouldCreateProviderUsingEnumTFIDF() {
            EmbeddingProvider provider = EmbeddingProviderFactory.create(ProviderType.TFIDF, null);
            
            assertNotNull(provider);
            assertInstanceOf(SimpleTFIDFEmbeddingProvider.class, provider);
        }
        
        @Test
        @DisplayName("Should create provider using enum - High-Performance TF-IDF")
        void shouldCreateProviderUsingEnumHighPerformanceTFIDF() {
            EmbeddingProvider provider = EmbeddingProviderFactory.create(ProviderType.HIGH_PERFORMANCE_TFIDF, null);
            
            assertNotNull(provider);
            assertInstanceOf(HighPerformanceTFIDFProvider.class, provider);
        }
        
        @Test
        @DisplayName("Should create provider using enum - Mock")
        void shouldCreateProviderUsingEnumMock() {
            EmbeddingProvider provider = EmbeddingProviderFactory.create(ProviderType.MOCK, null);
            
            assertNotNull(provider);
            assertEquals("Mock", provider.getProviderName());
        }
        
        @Test
        @DisplayName("Should get provider type from name")
        void shouldGetProviderTypeFromName() {
            assertEquals(ProviderType.OPENAI, ProviderType.fromName("openai"));
            assertEquals(ProviderType.ALIYUN, ProviderType.fromName("ALIYUN"));
            assertEquals(ProviderType.TFIDF, ProviderType.fromName("TfIdf"));
            assertEquals(ProviderType.MOCK, ProviderType.fromName("mock"));
        }
        
        @Test
        @DisplayName("Should throw exception for unknown provider type name")
        void shouldThrowExceptionForUnknownProviderTypeName() {
            assertThrows(IllegalArgumentException.class, () -> {
                ProviderType.fromName("unknown");
            });
        }
    }
    
    @Nested
    @DisplayName("Configuration-based Creation Tests")
    class ConfigurationBasedCreationTests {
        
        @Test
        @DisplayName("Should create OpenAI provider from configuration")
        void shouldCreateOpenAIProviderFromConfiguration() {
            Properties config = new Properties();
            config.setProperty("provider.type", "openai");
            config.setProperty("openai.apiKey", "test-api-key");
            config.setProperty("openai.model", "text-embedding-ada-002");
            
            EmbeddingProvider provider = EmbeddingProviderFactory.createFromConfig(config);
            
            assertNotNull(provider);
            assertEquals("OpenAI", provider.getProviderName());
        }
        
        @Test
        @DisplayName("Should create Aliyun provider from configuration")
        void shouldCreateAliyunProviderFromConfiguration() {
            Properties config = new Properties();
            config.setProperty("provider.type", "aliyun");
            config.setProperty("aliyun.apiKey", "test-api-key");
            config.setProperty("aliyun.model", "text-embedding-v1");
            
            EmbeddingProvider provider = EmbeddingProviderFactory.createFromConfig(config);
            
            assertNotNull(provider);
            assertEquals("Aliyun", provider.getProviderName());
        }
        
        @Test
        @DisplayName("Should create TF-IDF provider from configuration")
        void shouldCreateTFIDFProviderFromConfiguration() {
            Properties config = new Properties();
            config.setProperty("provider.type", "tfidf");
            
            EmbeddingProvider provider = EmbeddingProviderFactory.createFromConfig(config);
            
            assertNotNull(provider);
            assertInstanceOf(SimpleTFIDFEmbeddingProvider.class, provider);
        }
        
        @Test
        @DisplayName("Should create high-performance TF-IDF provider from configuration")
        void shouldCreateHighPerformanceTFIDFProviderFromConfiguration() {
            Properties config = new Properties();
            config.setProperty("provider.type", "high-performance-tfidf");
            
            EmbeddingProvider provider = EmbeddingProviderFactory.createFromConfig(config);
            
            assertNotNull(provider);
            assertInstanceOf(HighPerformanceTFIDFProvider.class, provider);
        }
        
        @Test
        @DisplayName("Should create mock provider from configuration")
        void shouldCreateMockProviderFromConfiguration() {
            Properties config = new Properties();
            config.setProperty("provider.type", "mock");
            
            EmbeddingProvider provider = EmbeddingProviderFactory.createFromConfig(config);
            
            assertNotNull(provider);
            assertEquals("Mock", provider.getProviderName());
        }
        
        @Test
        @DisplayName("Should use default provider type if not specified")
        void shouldUseDefaultProviderTypeIfNotSpecified() {
            Properties config = new Properties();
            
            EmbeddingProvider provider = EmbeddingProviderFactory.createFromConfig(config);
            
            assertNotNull(provider);
            assertInstanceOf(SimpleTFIDFEmbeddingProvider.class, provider);
        }
        
        @Test
        @DisplayName("Should fallback to TF-IDF for unknown provider type")
        void shouldFallbackToTFIDFForUnknownProviderType() {
            Properties config = new Properties();
            config.setProperty("provider.type", "unknown-provider");
            
            EmbeddingProvider provider = EmbeddingProviderFactory.createFromConfig(config);
            
            assertNotNull(provider);
            assertInstanceOf(SimpleTFIDFEmbeddingProvider.class, provider);
        }
        
        @Test
        @DisplayName("Should throw exception for OpenAI without API key")
        void shouldThrowExceptionForOpenAIWithoutApiKey() {
            Properties config = new Properties();
            config.setProperty("provider.type", "openai");
            
            assertThrows(IllegalArgumentException.class, () -> {
                EmbeddingProviderFactory.createFromConfig(config);
            });
        }
        
        @Test
        @DisplayName("Should throw exception for Aliyun without API key")
        void shouldThrowExceptionForAliyunWithoutApiKey() {
            Properties config = new Properties();
            config.setProperty("provider.type", "aliyun");
            
            assertThrows(IllegalArgumentException.class, () -> {
                EmbeddingProviderFactory.createFromConfig(config);
            });
        }
    }
    
    @Nested
    @DisplayName("Default Provider Management Tests")
    class DefaultProviderManagementTests {
        
        @Test
        @DisplayName("Should create default provider")
        void shouldCreateDefaultProvider() {
            EmbeddingProvider provider = EmbeddingProviderFactory.getDefaultProvider();
            
            assertNotNull(provider);
            assertInstanceOf(SimpleTFIDFEmbeddingProvider.class, provider);
        }
        
        @Test
        @DisplayName("Should return same default provider instance")
        void shouldReturnSameDefaultProviderInstance() {
            EmbeddingProvider provider1 = EmbeddingProviderFactory.getDefaultProvider();
            EmbeddingProvider provider2 = EmbeddingProviderFactory.getDefaultProvider();
            
            assertSame(provider1, provider2);
        }
        
        @Test
        @DisplayName("Should set custom default provider")
        void shouldSetCustomDefaultProvider() {
            EmbeddingProvider customProvider = EmbeddingProviderFactory.createMock();
            EmbeddingProviderFactory.setDefaultProvider(customProvider);
            
            EmbeddingProvider defaultProvider = EmbeddingProviderFactory.getDefaultProvider();
            
            assertSame(customProvider, defaultProvider);
            assertEquals("Mock", defaultProvider.getProviderName());
        }
    }
    
    @Nested
    @DisplayName("Cache Management Tests")
    class CacheManagementTests {
        
        @Test
        @DisplayName("Should track cached provider count")
        void shouldTrackCachedProviderCount() {
            assertEquals(0, EmbeddingProviderFactory.getCachedProviderCount());
            
            EmbeddingProviderFactory.createTFIDF();
            assertEquals(1, EmbeddingProviderFactory.getCachedProviderCount());
            
            EmbeddingProviderFactory.createMock();
            assertEquals(2, EmbeddingProviderFactory.getCachedProviderCount());
        }
        
        @Test
        @DisplayName("Should clear cache")
        void shouldClearCache() {
            EmbeddingProviderFactory.createTFIDF();
            EmbeddingProviderFactory.createMock();
            assertEquals(2, EmbeddingProviderFactory.getCachedProviderCount());
            
            EmbeddingProviderFactory.clearCache();
            assertEquals(0, EmbeddingProviderFactory.getCachedProviderCount());
        }
        
        @Test
        @DisplayName("Should close all providers")
        void shouldCloseAllProviders() {
            EmbeddingProvider provider1 = EmbeddingProviderFactory.createTFIDF();
            EmbeddingProvider provider2 = EmbeddingProviderFactory.createMock();
            EmbeddingProviderFactory.setDefaultProvider(provider1);
            
            assertTrue(provider1.isHealthy());
            assertTrue(provider2.isHealthy());
            
            EmbeddingProviderFactory.closeAll();
            assertEquals(0, EmbeddingProviderFactory.getCachedProviderCount());
        }
    }
    
    @Nested
    @DisplayName("Health Check Tests")
    class HealthCheckTests {
        
        @Test
        @DisplayName("Should check health status of all providers")
        void shouldCheckHealthStatusOfAllProviders() {
            EmbeddingProviderFactory.createTFIDF();
            EmbeddingProviderFactory.createMock();
            
            int healthyCount = EmbeddingProviderFactory.checkHealthStatus();
            assertEquals(2, healthyCount);
        }
        
        @Test
        @DisplayName("Should return 0 for empty cache")
        void shouldReturn0ForEmptyCache() {
            int healthyCount = EmbeddingProviderFactory.checkHealthStatus();
            assertEquals(0, healthyCount);
        }
    }
    
    @Test
    @DisplayName("Should handle concurrent access safely")
    void shouldHandleConcurrentAccessSafely() throws InterruptedException {
        final int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        final Exception[] exceptions = new Exception[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                try {
                    EmbeddingProviderFactory.createTFIDF();
                    EmbeddingProviderFactory.createMock();
                    EmbeddingProviderFactory.getDefaultProvider();
                    EmbeddingProviderFactory.getCachedProviderCount();
                    EmbeddingProviderFactory.checkHealthStatus();
                } catch (Exception e) {
                    exceptions[threadIndex] = e;
                }
            });
        }
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Check that no exceptions occurred
        for (Exception exception : exceptions) {
            if (exception != null) {
                fail("Exception in concurrent access test: " + exception.getMessage());
            }
        }
        
        // Should have cached providers (exact count may vary due to concurrency)
        assertTrue(EmbeddingProviderFactory.getCachedProviderCount() >= 2);
    }
}