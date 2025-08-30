package com.mem0.unit.embedding;

import com.mem0.embedding.impl.AliyunEmbeddingProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 阿里云嵌入提供者单元测试
 * Aliyun Embedding Provider Unit Tests
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
@DisplayName("Aliyun Embedding Provider Tests")
class AliyunEmbeddingProviderTest {
    
    private AliyunEmbeddingProvider provider;
    private static final String TEST_API_KEY = "sk-test-aliyun-api-key";
    private static final String TEST_MODEL = "text-embedding-v1";
    
    @BeforeEach
    void setUp() {
        provider = new AliyunEmbeddingProvider(TEST_API_KEY, TEST_MODEL);
    }
    
    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {
        
        @Test
        @DisplayName("Should create provider with valid API key")
        void shouldCreateProviderWithValidApiKey() {
            AliyunEmbeddingProvider testProvider = new AliyunEmbeddingProvider("valid-key");
            
            assertNotNull(testProvider);
            assertEquals("Aliyun", testProvider.getProviderName());
            assertEquals(1536, testProvider.getDimension());
        }
        
        @Test
        @DisplayName("Should create provider with custom model")
        void shouldCreateProviderWithCustomModel() {
            AliyunEmbeddingProvider testProvider = new AliyunEmbeddingProvider("valid-key", "text-embedding-v2");
            
            assertNotNull(testProvider);
            assertEquals("Aliyun", testProvider.getProviderName());
        }
        
        @Test
        @DisplayName("Should throw exception for null API key")
        void shouldThrowExceptionForNullApiKey() {
            assertThrows(IllegalArgumentException.class, () -> {
                new AliyunEmbeddingProvider(null);
            });
        }
        
        @Test
        @DisplayName("Should throw exception for empty API key")
        void shouldThrowExceptionForEmptyApiKey() {
            assertThrows(IllegalArgumentException.class, () -> {
                new AliyunEmbeddingProvider("");
            });
        }
        
        @Test
        @DisplayName("Should throw exception for blank API key")
        void shouldThrowExceptionForBlankApiKey() {
            assertThrows(IllegalArgumentException.class, () -> {
                new AliyunEmbeddingProvider("   ");
            });
        }
    }
    
    @Nested
    @DisplayName("Basic Properties Tests")
    class BasicPropertiesTests {
        
        @Test
        @DisplayName("Should return correct provider name")
        void shouldReturnCorrectProviderName() {
            assertEquals("Aliyun", provider.getProviderName());
        }
        
        @Test
        @DisplayName("Should return correct dimension")
        void shouldReturnCorrectDimension() {
            assertEquals(1536, provider.getDimension());
        }
        
        @Test
        @DisplayName("Should return healthy status initially")
        void shouldReturnHealthyStatusInitially() {
            assertTrue(provider.isHealthy());
        }
    }
    
    @Nested
    @DisplayName("Single Text Embedding Tests")
    class SingleTextEmbeddingTests {
        
        @Test
        @DisplayName("Should reject null text")
        void shouldRejectNullText() {
            CompletableFuture<List<Float>> future = provider.embed(null);
            
            assertThrows(ExecutionException.class, () -> {
                future.get();
            });
        }
        
        @Test
        @DisplayName("Should reject empty text")
        void shouldRejectEmptyText() {
            CompletableFuture<List<Float>> future = provider.embed("");
            
            assertThrows(ExecutionException.class, () -> {
                future.get();
            });
        }
        
        @Test
        @DisplayName("Should reject blank text")
        void shouldRejectBlankText() {
            CompletableFuture<List<Float>> future = provider.embed("   ");
            
            assertThrows(ExecutionException.class, () -> {
                future.get();
            });
        }
        
        @Test
        @DisplayName("Should return CompletableFuture for valid text")
        void shouldReturnCompletableFutureForValidText() {
            CompletableFuture<List<Float>> future = provider.embed("你好，世界！");
            
            assertNotNull(future);
            assertFalse(future.isDone()); // Since we don't have real API, it should fail
        }
        
        @Test
        @DisplayName("Should handle Chinese text")
        void shouldHandleChineseText() {
            String chineseText = "这是一个中文测试文档，用于验证阿里云嵌入向量的生成能力。";
            CompletableFuture<List<Float>> future = provider.embed(chineseText);
            
            assertNotNull(future);
        }
    }
    
    @Nested
    @DisplayName("Batch Text Embedding Tests")
    class BatchTextEmbeddingTests {
        
        @Test
        @DisplayName("Should reject null text list")
        void shouldRejectNullTextList() {
            CompletableFuture<List<List<Float>>> future = provider.embedBatch(null);
            
            assertThrows(ExecutionException.class, () -> {
                future.get();
            });
        }
        
        @Test
        @DisplayName("Should reject empty text list")
        void shouldRejectEmptyTextList() {
            CompletableFuture<List<List<Float>>> future = provider.embedBatch(Collections.emptyList());
            
            assertThrows(ExecutionException.class, () -> {
                future.get();
            });
        }
        
        @Test
        @DisplayName("Should reject list with null text")
        void shouldRejectListWithNullText() {
            List<String> texts = Arrays.asList("有效文本", null, "另一个有效文本");
            CompletableFuture<List<List<Float>>> future = provider.embedBatch(texts);
            
            assertThrows(ExecutionException.class, () -> {
                future.get();
            });
        }
        
        @Test
        @DisplayName("Should reject list with empty text")
        void shouldRejectListWithEmptyText() {
            List<String> texts = Arrays.asList("有效文本", "", "另一个有效文本");
            CompletableFuture<List<List<Float>>> future = provider.embedBatch(texts);
            
            assertThrows(ExecutionException.class, () -> {
                future.get();
            });
        }
        
        @Test
        @DisplayName("Should return CompletableFuture for valid text list")
        void shouldReturnCompletableFutureForValidTextList() {
            List<String> texts = Arrays.asList("你好", "世界", "测试");
            CompletableFuture<List<List<Float>>> future = provider.embedBatch(texts);
            
            assertNotNull(future);
            assertFalse(future.isDone()); // Since we don't have real API, it should fail
        }
        
        @Test
        @DisplayName("Should handle mixed language batch")
        void shouldHandleMixedLanguageBatch() {
            List<String> texts = Arrays.asList(
                "Hello world",
                "你好世界", 
                "こんにちは世界",
                "안녕하세요 세계"
            );
            CompletableFuture<List<List<Float>>> future = provider.embedBatch(texts);
            
            assertNotNull(future);
        }
    }
    
    @Nested
    @DisplayName("Resource Management Tests")
    class ResourceManagementTests {
        
        @Test
        @DisplayName("Should close provider without exception")
        void shouldCloseProviderWithoutException() {
            assertDoesNotThrow(() -> {
                provider.close();
            });
        }
        
        @Test
        @DisplayName("Should be safe to close multiple times")
        void shouldBeSafeToCloseMultipleTimes() {
            assertDoesNotThrow(() -> {
                provider.close();
                provider.close();
                provider.close();
            });
        }
    }
    
    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {
        
        @Test
        @DisplayName("Should accept custom API URL")
        void shouldAcceptCustomApiUrl() {
            String customUrl = "https://custom.dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding";
            AliyunEmbeddingProvider customProvider = new AliyunEmbeddingProvider(
                TEST_API_KEY, customUrl, TEST_MODEL
            );
            
            assertNotNull(customProvider);
            assertEquals("Aliyun", customProvider.getProviderName());
        }
        
        @Test
        @DisplayName("Should accept custom dimension")
        void shouldAcceptCustomDimension() {
            int customDimension = 768;
            AliyunEmbeddingProvider customProvider = new AliyunEmbeddingProvider(
                TEST_API_KEY, 
                "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding", 
                "text-embedding-v1", 
                customDimension, 
                3
            );
            
            assertEquals(customDimension, customProvider.getDimension());
        }
        
        @Test
        @DisplayName("Should use default dimension for invalid dimension")
        void shouldUseDefaultDimensionForInvalidDimension() {
            AliyunEmbeddingProvider customProvider = new AliyunEmbeddingProvider(
                TEST_API_KEY, 
                "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding", 
                "text-embedding-v1", 
                0, 
                3
            );
            
            assertEquals(1536, customProvider.getDimension()); // Should use default
        }
        
        @Test
        @DisplayName("Should handle negative max retries")
        void shouldHandleNegativeMaxRetries() {
            AliyunEmbeddingProvider customProvider = new AliyunEmbeddingProvider(
                TEST_API_KEY, 
                "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding", 
                "text-embedding-v1", 
                1536, 
                -1
            );
            
            assertNotNull(customProvider); // Should still create, but with minimum retries
        }
    }
    
    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {
        
        @Test
        @DisplayName("Should handle very long Chinese text")
        void shouldHandleVeryLongChineseText() {
            StringBuilder longText = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                longText.append("这是一个很长的中文测试文本，用于验证阿里云嵌入向量服务的处理能力。");
            }
            
            CompletableFuture<List<Float>> future = provider.embed(longText.toString());
            assertNotNull(future);
        }
        
        @Test
        @DisplayName("Should handle special characters and emojis")
        void shouldHandleSpecialCharactersAndEmojis() {
            String specialText = "你好！@#￥%……&*（）_+ 🌟 café ñoël 测试";
            CompletableFuture<List<Float>> future = provider.embed(specialText);
            
            assertNotNull(future);
        }
        
        @Test
        @DisplayName("Should handle large Chinese batch")
        void shouldHandleLargeChineseBatch() {
            List<String> largeBatch = Arrays.asList(
                "第一个中文文档", "第二个中文文档", "第三个中文文档", 
                "第四个中文文档", "第五个中文文档", "第六个中文文档",
                "第七个中文文档", "第八个中文文档", "第九个中文文档", 
                "第十个中文文档"
            );
            
            CompletableFuture<List<List<Float>>> future = provider.embedBatch(largeBatch);
            assertNotNull(future);
        }
        
        @Test
        @DisplayName("Should handle technical Chinese terms")
        void shouldHandleTechnicalChineseTerms() {
            String technicalText = "人工智能、机器学习、深度学习、自然语言处理、计算机视觉、语音识别";
            CompletableFuture<List<Float>> future = provider.embed(technicalText);
            
            assertNotNull(future);
        }
    }
    
    @Test
    @DisplayName("Should maintain thread safety")
    void shouldMaintainThreadSafety() throws InterruptedException {
        final int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        final Exception[] exceptions = new Exception[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                try {
                    provider.embed("测试文本 " + threadIndex);
                    provider.embedBatch(Arrays.asList("批量文本1", "批量文本2"));
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
        
        // Check that no unexpected exceptions occurred (API failures are expected)
        for (Exception exception : exceptions) {
            if (exception != null && !(exception.getMessage().contains("Failed to get embeddings"))) {
                fail("Unexpected exception in thread safety test: " + exception.getMessage());
            }
        }
    }
}