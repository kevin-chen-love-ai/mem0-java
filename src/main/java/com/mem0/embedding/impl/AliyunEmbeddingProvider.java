package com.mem0.embedding.impl;

import com.mem0.embedding.EmbeddingProvider;
import com.fasterxml.jackson.annotation.JsonProperty;
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
 * 阿里云嵌入提供者实现 / Aliyun Embedding Provider Implementation
 * 
 * 该类实现了基于阿里云机器学习平台PAI的文本嵌入向量生成服务，使用阿里云的text-embedding模型
 * 将文本转换为高质量的语义向量表示，适用于相似性搜索、聚类、分类等任务。
 * 
 * This class implements text embedding vector generation service based on Aliyun PAI platform,
 * using Aliyun's text-embedding models to convert text into high-quality semantic vector
 * representations for similarity search, clustering, classification, and other tasks.
 * 
 * 主要功能 / Key Features:
 * - 高质量中文语义嵌入向量生成 / High-quality Chinese semantic embedding generation
 * - 支持单个和批量文本处理 / Support for single and batch text processing
 * - 同步API调用，适配标准API权限 / Synchronous API calls compatible with standard API permissions
 * - 标准1536维向量输出 / Standard 1536-dimensional vector output
 * - 自动错误处理和重试机制 / Automatic error handling and retry mechanism
 * - 完整的健康状态检查 / Complete health status monitoring
 * 
 * 性能特征 / Performance Characteristics:
 * - 向量维度: 1536 (text-embedding-v1) / Vector dimension: 1536
 * - 支持批量处理以提高吞吐量 / Batch processing support for improved throughput
 * - 网络延迟依赖 / Network latency dependent
 * - 需要有效的API密钥认证 / Requires valid API key authentication
 * - 优化中文语义理解 / Optimized for Chinese semantic understanding
 * 
 * 使用场景 / Use Cases:
 * - 中文语义搜索和检索系统 / Chinese semantic search and retrieval systems
 * - 中文文档相似性分析 / Chinese document similarity analysis
 * - 中文内容推荐引擎 / Chinese content recommendation engines
 * - 中文文本聚类和分类任务 / Chinese text clustering and classification tasks
 * 
 * 使用示例 / Usage Example:
 * <pre>
 * {@code
 * // 初始化提供者 / Initialize provider
 * AliyunEmbeddingProvider provider = new AliyunEmbeddingProvider("your-api-key");
 * 
 * // 单个文本嵌入 / Single text embedding
 * CompletableFuture<List<Float>> embedding = provider.embed("你好世界");
 * 
 * // 批量文本嵌入 / Batch text embedding
 * List<String> texts = Arrays.asList("你好", "世界", "阿里云");
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
 * - 需要有效的阿里云API密钥和访问权限
 * - 实际部署需要配置正确的endpoint和region
 * - 需要处理API限制和计费考虑
 * - Requires valid Aliyun API key and access permissions
 * - Production deployment requires correct endpoint and region configuration
 * - Need to handle API limits and billing considerations
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class AliyunEmbeddingProvider implements EmbeddingProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(AliyunEmbeddingProvider.class);
    
    private static final String DEFAULT_API_URL = "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding";
    private static final String DEFAULT_MODEL = "text-embedding-v1";
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
    
    public AliyunEmbeddingProvider(String apiKey) {
        this(apiKey, DEFAULT_API_URL, DEFAULT_MODEL);
    }
    
    public AliyunEmbeddingProvider(String apiKey, String model) {
        this(apiKey, DEFAULT_API_URL, model);
    }
    
    public AliyunEmbeddingProvider(String apiKey, String apiUrl, String model) {
        this(apiKey, apiUrl, model, DEFAULT_DIMENSION, DEFAULT_MAX_RETRIES);
    }
    
    public AliyunEmbeddingProvider(String apiKey, String apiUrl, String model, int dimension, int maxRetries) {
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
        
        logger.info("阿里云嵌入提供者初始化完成 - Model: {}, Dimension: {}", this.model, this.dimension);
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
                logger.error("Failed to get embeddings from Aliyun API", e);
                throw new CompletionException("Failed to get embeddings", e);
            }
        });
    }
    
    private List<List<Float>> callEmbeddingAPI(List<String> texts) throws IOException {
        EmbeddingRequest request = new EmbeddingRequest();
        request.model = this.model;
        request.input = new Input();
        request.input.texts = texts;
        
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
                    logger.warn("Aliyun API request failed (attempt {}/{}): {} - {}", 
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
                        throw new RuntimeException("Aliyun API request failed: " + response.code() + " - " + errorBody);
                    }
                    continue;
                }
                
                String responseJson = response.body().string();
                EmbeddingResponse embeddingResponse = objectMapper.readValue(responseJson, EmbeddingResponse.class);
                
                if (embeddingResponse.output == null || embeddingResponse.output.embeddings == null) {
                    throw new RuntimeException("Invalid response format from Aliyun API");
                }
                
                List<List<Float>> embeddings = new ArrayList<>();
                for (EmbeddingData data : embeddingResponse.output.embeddings) {
                    embeddings.add(data.embedding);
                }
                
                healthy = true;
                return embeddings;
                
            } catch (IOException e) {
                lastException = e;
                logger.warn("Aliyun API call failed (attempt {}/{}): {}", 
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
        return "Aliyun";
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
            logger.info("阿里云嵌入提供者已关闭");
        } catch (Exception e) {
            logger.warn("Error closing Aliyun embedding provider", e);
        }
    }
    
    // Inner classes for JSON serialization/deserialization
    
    private static class EmbeddingRequest {
        @JsonProperty("model")
        public String model;
        
        @JsonProperty("input")
        public Input input;
        
        @JsonProperty("parameters")
        public Parameters parameters = new Parameters();
    }
    
    private static class Input {
        @JsonProperty("texts")
        public List<String> texts;
    }
    
    private static class Parameters {
        @JsonProperty("text_type")
        public String textType = "document";
    }
    
    private static class EmbeddingResponse {
        @JsonProperty("status_code")
        public int statusCode;
        
        @JsonProperty("request_id")
        public String requestId;
        
        @JsonProperty("code")
        public String code;
        
        @JsonProperty("message")
        public String message;
        
        @JsonProperty("output")
        public Output output;
        
        @JsonProperty("usage")
        public Usage usage;
    }
    
    private static class Output {
        @JsonProperty("embeddings")
        public List<EmbeddingData> embeddings;
    }
    
    private static class EmbeddingData {
        @JsonProperty("text_index")
        public int textIndex;
        
        @JsonProperty("embedding")
        public List<Float> embedding;
    }
    
    private static class Usage {
        @JsonProperty("total_tokens")
        public int totalTokens;
    }
}