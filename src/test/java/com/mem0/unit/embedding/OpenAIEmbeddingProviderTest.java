package com.mem0.unit.embedding;

import com.mem0.embedding.impl.OpenAIEmbeddingProvider;
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
 * OpenAI嵌入提供者单元测试
 * OpenAI Embedding Provider Unit Tests
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
@DisplayName("OpenAI Embedding Provider Tests")
class OpenAIEmbeddingProviderTest {
    
    private OpenAIEmbeddingProvider provider;
    private static final String TEST_API_KEY = "sk-test-api-key-for-testing";
    private static final String TEST_MODEL = "text-embedding-ada-002";
    
    @BeforeEach
    void setUp() {
        provider = new OpenAIEmbeddingProvider(TEST_API_KEY, TEST_MODEL);
    }
    
    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {
        
        @Test
        @DisplayName("Should create provider with valid API key")
        void shouldCreateProviderWithValidApiKey() {
            OpenAIEmbeddingProvider testProvider = new OpenAIEmbeddingProvider("valid-key");
            
            assertNotNull(testProvider);
            assertEquals("OpenAI", testProvider.getProviderName());
            assertEquals(1536, testProvider.getDimension());
        }
        
        @Test
        @DisplayName("Should create provider with custom model")
        void shouldCreateProviderWithCustomModel() {
            OpenAIEmbeddingProvider testProvider = new OpenAIEmbeddingProvider("valid-key", "text-embedding-3-small");
            
            assertNotNull(testProvider);
            assertEquals("OpenAI", testProvider.getProviderName());
        }
        
        @Test
        @DisplayName("Should throw exception for null API key")
        void shouldThrowExceptionForNullApiKey() {
            assertThrows(IllegalArgumentException.class, () -> {
                new OpenAIEmbeddingProvider(null);
            });
        }
        
        @Test
        @DisplayName("Should throw exception for empty API key")
        void shouldThrowExceptionForEmptyApiKey() {
            assertThrows(IllegalArgumentException.class, () -> {
                new OpenAIEmbeddingProvider("");
            });
        }
        
        @Test
        @DisplayName("Should throw exception for blank API key")
        void shouldThrowExceptionForBlankApiKey() {
            assertThrows(IllegalArgumentException.class, () -> {
                new OpenAIEmbeddingProvider("   ");
            });
        }
    }
    
    @Nested
    @DisplayName("Basic Properties Tests")
    class BasicPropertiesTests {
        
        @Test
        @DisplayName("Should return correct provider name")
        void shouldReturnCorrectProviderName() {
            assertEquals("OpenAI", provider.getProviderName());
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
            CompletableFuture<List<Float>> future = provider.embed("Hello, world!");
            
            assertNotNull(future);
            assertFalse(future.isDone()); // Since we don't have real API, it should fail
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
            List<String> texts = Arrays.asList("valid text", null, "another valid text");
            CompletableFuture<List<List<Float>>> future = provider.embedBatch(texts);
            
            assertThrows(ExecutionException.class, () -> {
                future.get();
            });
        }
        
        @Test
        @DisplayName("Should reject list with empty text")
        void shouldRejectListWithEmptyText() {
            List<String> texts = Arrays.asList("valid text", "", "another valid text");
            CompletableFuture<List<List<Float>>> future = provider.embedBatch(texts);
            
            assertThrows(ExecutionException.class, () -> {
                future.get();
            });
        }
        
        @Test
        @DisplayName("Should return CompletableFuture for valid text list")
        void shouldReturnCompletableFutureForValidTextList() {
            List<String> texts = Arrays.asList("Hello", "World", "Test");
            CompletableFuture<List<List<Float>>> future = provider.embedBatch(texts);
            
            assertNotNull(future);
            assertFalse(future.isDone()); // Since we don't have real API, it should fail
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
            String customUrl = "https://custom.openai.com/v1/embeddings";
            OpenAIEmbeddingProvider customProvider = new OpenAIEmbeddingProvider(
                TEST_API_KEY, customUrl, TEST_MODEL
            );
            
            assertNotNull(customProvider);
            assertEquals("OpenAI", customProvider.getProviderName());
        }
        
        @Test
        @DisplayName("Should accept custom dimension")
        void shouldAcceptCustomDimension() {
            int customDimension = 768;
            OpenAIEmbeddingProvider customProvider = new OpenAIEmbeddingProvider(
                TEST_API_KEY, "https://api.openai.com/v1/embeddings", "text-embedding-3-small", customDimension, 3
            );
            
            assertEquals(customDimension, customProvider.getDimension());
        }
        
        @Test
        @DisplayName("Should use default dimension for invalid dimension")
        void shouldUseDefaultDimensionForInvalidDimension() {
            OpenAIEmbeddingProvider customProvider = new OpenAIEmbeddingProvider(
                TEST_API_KEY, "https://api.openai.com/v1/embeddings", "text-embedding-ada-002", 0, 3
            );
            
            assertEquals(1536, customProvider.getDimension()); // Should use default
        }
        
        @Test
        @DisplayName("Should handle negative max retries")
        void shouldHandleNegativeMaxRetries() {
            OpenAIEmbeddingProvider customProvider = new OpenAIEmbeddingProvider(
                TEST_API_KEY, "https://api.openai.com/v1/embeddings", "text-embedding-ada-002", 1536, -1
            );
            
            assertNotNull(customProvider); // Should still create, but with minimum retries
        }
    }
    
    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {
        
        @Test
        @DisplayName("Should handle very long text")
        void shouldHandleVeryLongText() {
            StringBuilder longText = new StringBuilder();
            for (int i = 0; i < 10000; i++) {
                longText.append("This is a very long text for testing. ");
            }
            
            CompletableFuture<List<Float>> future = provider.embed(longText.toString());
            assertNotNull(future);
        }
        
        @Test
        @DisplayName("Should handle special characters")
        void shouldHandleSpecialCharacters() {
            String specialText = "Hello! @#$%^&*()_+ 你好 🌟 ñoël café";
            CompletableFuture<List<Float>> future = provider.embed(specialText);
            
            assertNotNull(future);
        }
        
        @Test
        @DisplayName("Should handle large batch")
        void shouldHandleLargeBatch() {
            List<String> largeBatch = Arrays.asList(
                "Text 1", "Text 2", "Text 3", "Text 4", "Text 5",
                "Text 6", "Text 7", "Text 8", "Text 9", "Text 10"
            );
            
            CompletableFuture<List<List<Float>>> future = provider.embedBatch(largeBatch);
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
                    provider.embed("Test text " + threadIndex);
                    provider.embedBatch(Arrays.asList("Batch text 1", "Batch text 2"));
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