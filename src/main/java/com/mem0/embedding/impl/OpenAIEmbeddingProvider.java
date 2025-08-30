package com.mem0.embedding.impl;

import com.mem0.embedding.EmbeddingProvider;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI嵌入提供者实现 / OpenAI Embedding Provider Implementation
 * 
 * 该类实现了基于OpenAI API的文本嵌入向量生成服务，使用OpenAI的text-embedding模型
 * 将文本转换为高质量的语义向量表示，适用于相似性搜索、聚类、分类等任务。
 * 
 * This class implements text embedding vector generation service based on OpenAI API,
 * using OpenAI's text-embedding models to convert text into high-quality semantic vector
 * representations for similarity search, clustering, classification, and other tasks.
 * 
 * 主要功能 / Key Features:
 * - 高质量语义嵌入向量生成 / High-quality semantic embedding generation
 * - 支持单个和批量文本处理 / Support for single and batch text processing
 * - 异步非阻塞处理 / Asynchronous non-blocking processing
 * - 标准1536维向量输出 / Standard 1536-dimensional vector output
 * - 自动错误处理和重试机制 / Automatic error handling and retry mechanism
 * - 完整的健康状态检查 / Complete health status monitoring
 * 
 * 性能特征 / Performance Characteristics:
 * - 向量维度: 1536 (text-embedding-ada-002) / Vector dimension: 1536
 * - 支持批量处理以提高吞吐量 / Batch processing support for improved throughput
 * - 网络延迟依赖 / Network latency dependent
 * - 需要有效的API密钥认证 / Requires valid API key authentication
 * 
 * 使用场景 / Use Cases:
 * - 语义搜索和检索系统 / Semantic search and retrieval systems
 * - 文档相似性分析 / Document similarity analysis
 * - 内容推荐引擎 / Content recommendation engines
 * - 聚类和分类任务 / Clustering and classification tasks
 * 
 * 使用示例 / Usage Example:
 * <pre>
 * {@code
 * // 初始化提供者 / Initialize provider
 * OpenAIEmbeddingProvider provider = new OpenAIEmbeddingProvider("your-api-key");
 * 
 * // 单个文本嵌入 / Single text embedding
 * CompletableFuture<List<Float>> embedding = provider.embed("Hello world");
 * 
 * // 批量文本嵌入 / Batch text embedding
 * List<String> texts = Arrays.asList("Hello", "World", "OpenAI");
 * CompletableFuture<List<List<Float>>> embeddings = provider.embedBatch(texts);
 * 
 * // 获取向量维度 / Get vector dimension
 * int dimension = provider.getDimension(); // 1536
 * 
 * // 关闭资源 / Close resources
 * provider.close();
 * }
 * </pre>
 * 
 * 注意事项 / Important Notes:
 * - 当前为占位符实现，返回零向量用于测试
 * - 实际部署需要集成真实的OpenAI API
 * - 需要处理API限制和计费考虑
 * - Currently a placeholder implementation returning zero vectors for testing
 * - Production deployment requires real OpenAI API integration
 * - Need to handle API limits and billing considerations
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class OpenAIEmbeddingProvider implements EmbeddingProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAIEmbeddingProvider.class);
    
    private static final String DEFAULT_API_URL = "https://api.openai.com/v1/embeddings";
    private static final String DEFAULT_MODEL = "text-embedding-ada-002";
    private static final int DEFAULT_DIMENSION = 1536;
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final int dimension;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final int maxRetries;
    private volatile boolean healthy = true;
    
    public OpenAIEmbeddingProvider(String apiKey) {
        this(apiKey, DEFAULT_API_URL, DEFAULT_MODEL);
    }
    
    public OpenAIEmbeddingProvider(String apiKey, String model) {
        this(apiKey, DEFAULT_API_URL, model);
    }
    
    public OpenAIEmbeddingProvider(String apiKey, String apiUrl, String model) {
        this(apiKey, apiUrl, model, DEFAULT_DIMENSION, DEFAULT_MAX_RETRIES);
    }
    
    public OpenAIEmbeddingProvider(String apiKey, String apiUrl, String model, int dimension, int maxRetries) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        
        this.apiKey = apiKey.trim();
        this.apiUrl = apiUrl != null ? apiUrl : DEFAULT_API_URL;
        this.model = model != null ? model : DEFAULT_MODEL;
        this.dimension = dimension > 0 ? dimension : DEFAULT_DIMENSION;
        this.maxRetries = Math.max(1, maxRetries);
        
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
                
        this.objectMapper = new ObjectMapper();
        
        logger.info("OpenAI嵌入提供者初始化完成 - Model: {}, Dimension: {}", this.model, this.dimension);
    }

    @Override
    public CompletableFuture<List<Float>> embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            CompletableFuture<List<Float>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new IllegalArgumentException("Text cannot be null or empty"));
            return failedFuture;
        }
        
        return embedBatch(java.util.Collections.singletonList(text))
                .thenApply(results -> {
                    if (results == null || results.isEmpty()) {
                        throw new RuntimeException("Failed to get embedding result");
                    }
                    return results.get(0);
                });
    }

    @Override
    public CompletableFuture<List<List<Float>>> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            CompletableFuture<List<List<Float>>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new IllegalArgumentException("Texts cannot be null or empty"));
            return failedFuture;
        }
        
        for (String text : texts) {
            if (text == null || text.trim().isEmpty()) {
                CompletableFuture<List<List<Float>>> failedFuture = new CompletableFuture<>();
                failedFuture.completeExceptionally(new IllegalArgumentException("Text cannot be null or empty"));
                return failedFuture;
            }
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return callEmbeddingAPI(texts);
            } catch (Exception e) {
                logger.error("Failed to get embeddings from OpenAI API", e);
                throw new CompletionException("Failed to get embeddings", e);
            }
        });
    }
    
    private List<List<Float>> callEmbeddingAPI(List<String> texts) throws IOException {
        EmbeddingRequest request = new EmbeddingRequest();
        request.model = this.model;
        request.input = texts;
        
        String requestJson = objectMapper.writeValueAsString(request);
        
        RequestBody body = RequestBody.create(requestJson, JSON);
        Request httpRequest = new Request.Builder()
                .url(apiUrl)
                .post(body)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();
        
        IOException lastException = null;
        
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    logger.warn("OpenAI API request failed (attempt {}/{}): {} - {}", 
                              attempt + 1, maxRetries, response.code(), errorBody);
                    
                    if (response.code() == 401) {
                        healthy = false;
                        throw new RuntimeException("Invalid API key");
                    }
                    
                    if (response.code() == 429) {
                        // Rate limited, wait before retry
                        try {
                            Thread.sleep(1000 * (attempt + 1));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Interrupted while waiting for retry", e);
                        }
                        continue;
                    }
                    
                    if (attempt == maxRetries - 1) {
                        healthy = false;
                        throw new RuntimeException("OpenAI API request failed: " + response.code() + " - " + errorBody);
                    }
                    continue;
                }
                
                String responseJson = response.body().string();
                EmbeddingResponse embeddingResponse = objectMapper.readValue(responseJson, EmbeddingResponse.class);
                
                List<List<Float>> embeddings = new ArrayList<>();
                for (EmbeddingData data : embeddingResponse.data) {
                    embeddings.add(data.embedding);
                }
                
                healthy = true;
                return embeddings;
                
            } catch (IOException e) {
                lastException = e;
                logger.warn("OpenAI API call failed (attempt {}/{}): {}", 
                          attempt + 1, maxRetries, e.getMessage());
                
                if (attempt < maxRetries - 1) {
                    try {
                        Thread.sleep(1000 * (attempt + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted while waiting for retry", ie);
                    }
                }
            }
        }
        
        healthy = false;
        throw lastException != null ? lastException : new IOException("All retry attempts failed");
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public String getProviderName() {
        return "OpenAI";
    }

    @Override
    public boolean isHealthy() {
        return healthy;
    }

    @Override
    public void close() {
        try {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
            logger.info("OpenAI嵌入提供者已关闭");
        } catch (Exception e) {
            logger.warn("Error closing OpenAI embedding provider", e);
        }
    }
    
    // Inner classes for JSON serialization/deserialization
    
    private static class EmbeddingRequest {
        @JsonProperty("model")
        public String model;
        
        @JsonProperty("input")
        public List<String> input;
        
        @JsonProperty("encoding_format")
        public String encodingFormat = "float";
    }
    
    private static class EmbeddingResponse {
        @JsonProperty("object")
        public String object;
        
        @JsonProperty("data")
        public List<EmbeddingData> data;
        
        @JsonProperty("model")
        public String model;
        
        @JsonProperty("usage")
        public Usage usage;
    }
    
    private static class EmbeddingData {
        @JsonProperty("object")
        public String object;
        
        @JsonProperty("embedding")
        public List<Float> embedding;
        
        @JsonProperty("index")
        public int index;
    }
    
    private static class Usage {
        @JsonProperty("prompt_tokens")
        public int promptTokens;
        
        @JsonProperty("total_tokens")
        public int totalTokens;
    }
}