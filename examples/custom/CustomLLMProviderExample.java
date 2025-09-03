package examples.custom;

import com.mem0.Mem0;
import com.mem0.config.Mem0Configuration;
import com.mem0.embedding.impl.MockEmbeddingProvider;
import com.mem0.llm.LLMProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
// Note: For Java 8 compatibility, replace java.net.http with OkHttp or Apache HttpClient
// import okhttp3.*;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

// Java 11+ imports - replace with OkHttp for Java 8 compatibility
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * 自定义LLM提供者示例 - Custom LLM Provider Example
 * 
 * 展示如何实现自定义的LLM提供者，支持多种LLM服务
 * Demonstrates how to implement custom LLM providers for various LLM services
 * 
 * NOTE: This example is for demonstration purposes only. For Java 8 compatibility,
 * replace java.net.http with OkHttp or Apache HttpClient.
 */
public class CustomLLMProviderExample {
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== Custom LLM Provider Example ===\n");
        
        // 1. 使用自定义Claude LLM提供者
        System.out.println("1. Testing Custom Claude LLM Provider:");
        testClaudeLLMProvider();
        
        // 2. 使用自定义Gemini LLM提供者
        System.out.println("\n2. Testing Custom Gemini LLM Provider:");
        testGeminiLLMProvider();
        
        // 3. 使用本地LLM提供者
        System.out.println("\n3. Testing Local LLM Provider:");
        testLocalLLMProvider();
        
        System.out.println("\n=== Example completed successfully! ===");
    }
    
    private static void testClaudeLLMProvider() throws Exception {
        Mem0Configuration config = new Mem0Configuration();
        config.setLlmProvider(new ClaudeLLMProvider("your-anthropic-api-key"));
        config.setEmbeddingProvider(new MockEmbeddingProvider());
        
        Mem0 mem0 = new Mem0(config);
        
        // 测试基本操作
        String memoryId = mem0.add("I love reading science fiction books", "claude-user");
        System.out.println("   ✓ Added memory with Claude LLM: " + memoryId);
        
        mem0.close();
    }
    
    private static void testGeminiLLMProvider() throws Exception {
        Mem0Configuration config = new Mem0Configuration();
        config.setLlmProvider(new GeminiLLMProvider("your-google-api-key"));
        config.setEmbeddingProvider(new MockEmbeddingProvider());
        
        Mem0 mem0 = new Mem0(config);
        
        String memoryId = mem0.add("I work in artificial intelligence research", "gemini-user");
        System.out.println("   ✓ Added memory with Gemini LLM: " + memoryId);
        
        mem0.close();
    }
    
    private static void testLocalLLMProvider() throws Exception {
        Mem0Configuration config = new Mem0Configuration();
        config.setLlmProvider(new LocalLLMProvider("http://localhost:11434")); // Ollama默认端口
        config.setEmbeddingProvider(new MockEmbeddingProvider());
        
        Mem0 mem0 = new Mem0(config);
        
        String memoryId = mem0.add("I prefer using local AI models for privacy", "local-user");
        System.out.println("   ✓ Added memory with Local LLM: " + memoryId);
        
        mem0.close();
    }
}

/**
 * Claude LLM提供者实现 - Claude LLM Provider Implementation
 */
class ClaudeLLMProvider implements LLMProvider {
    private static final Logger logger = LoggerFactory.getLogger(ClaudeLLMProvider.class);
    
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Executor executor;
    
    public ClaudeLLMProvider(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
        this.executor = Executors.newCachedThreadPool();
    }
    
    @Override
    public CompletableFuture<LLMResponse> generateChatCompletion(List<ChatMessage> messages, LLMConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 构建Claude API请求
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", config.getModel() != null ? config.getModel() : "claude-3-sonnet-20240229");
                requestBody.put("max_tokens", config.getMaxTokens() != null ? config.getMaxTokens() : 1000);
                requestBody.put("temperature", config.getTemperature() != null ? config.getTemperature() : 0.7);
                requestBody.put("messages", convertMessages(messages));
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.anthropic.com/v1/messages"))
                        .header("Content-Type", "application/json")
                        .header("x-api-key", apiKey)
                        .header("anthropic-version", "2023-06-01")
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Claude API error: " + response.body());
                }
                
                // 解析响应
                Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
                List<Map<String, Object>> content = (List<Map<String, Object>>) responseBody.get("content");
                String text = (String) content.get(0).get("text");
                
                Map<String, Object> usage = (Map<String, Object>) responseBody.get("usage");
                int tokensUsed = (Integer) usage.get("input_tokens") + (Integer) usage.get("output_tokens");
                
                return new LLMResponse(text, tokensUsed, "claude-3-sonnet", "stop");
                
            } catch (Exception e) {
                logger.error("Error calling Claude API", e);
                throw new RuntimeException("Claude API call failed", e);
            }
        }, executor);
    }
    
    private List<Map<String, String>> convertMessages(List<ChatMessage> messages) {
        return messages.stream()
                .map(msg -> {
                    Map<String, String> msgMap = new HashMap<>();
                    msgMap.put("role", msg.getRole());
                    msgMap.put("content", msg.getContent());
                    return msgMap;
                })
                .collect(java.util.stream.Collectors.toList());
    }
}

/**
 * Gemini LLM提供者实现 - Gemini LLM Provider Implementation
 */
class GeminiLLMProvider implements LLMProvider {
    private static final Logger logger = LoggerFactory.getLogger(GeminiLLMProvider.class);
    
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Executor executor;
    
    public GeminiLLMProvider(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
        this.executor = Executors.newCachedThreadPool();
    }
    
    @Override
    public CompletableFuture<LLMResponse> generateChatCompletion(List<ChatMessage> messages, LLMConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String model = config.getModel() != null ? config.getModel() : "gemini-pro";
                
                // 构建Gemini API请求
                Map<String, Object> generationConfig = new HashMap<>();
                generationConfig.put("temperature", config.getTemperature() != null ? config.getTemperature() : 0.7);
                generationConfig.put("maxOutputTokens", config.getMaxTokens() != null ? config.getMaxTokens() : 1000);
                
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("contents", convertMessagesToGeminiFormat(messages));
                requestBody.put("generationConfig", generationConfig);
                
                String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", 
                                         model, apiKey);
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Gemini API error: " + response.body());
                }
                
                // 解析响应
                Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                String text = (String) parts.get(0).get("text");
                
                return new LLMResponse(text, 100, model, "stop"); // Gemini不返回token使用量
                
            } catch (Exception e) {
                logger.error("Error calling Gemini API", e);
                throw new RuntimeException("Gemini API call failed", e);
            }
        }, executor);
    }
    
    private List<Map<String, Object>> convertMessagesToGeminiFormat(List<ChatMessage> messages) {
        return messages.stream()
                .map(msg -> {
                    Map<String, String> textPart = new HashMap<>();
                    textPart.put("text", msg.getContent());
                    
                    Map<String, Object> msgMap = new HashMap<>();
                    msgMap.put("role", convertRoleToGemini(msg.getRole()));
                    msgMap.put("parts", java.util.Arrays.asList(textPart));
                    return msgMap;
                })
                .collect(java.util.stream.Collectors.toList());
    }
    
    private String convertRoleToGemini(String role) {
        switch (role) {
            case "system":
            case "user":
                return "user";
            case "assistant":
                return "model";
            default:
                return "user";
        }
    }
}

/**
 * 本地LLM提供者实现（支持Ollama等） - Local LLM Provider Implementation
 */
class LocalLLMProvider implements LLMProvider {
    private static final Logger logger = LoggerFactory.getLogger(LocalLLMProvider.class);
    
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Executor executor;
    
    public LocalLLMProvider(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
        this.executor = Executors.newCachedThreadPool();
    }
    
    @Override
    public CompletableFuture<LLMResponse> generateChatCompletion(List<ChatMessage> messages, LLMConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String model = config.getModel() != null ? config.getModel() : "llama2";
                
                // 构建Ollama API请求
                Map<String, Object> options = new HashMap<>();
                options.put("temperature", config.getTemperature() != null ? config.getTemperature() : 0.7);
                options.put("num_predict", config.getMaxTokens() != null ? config.getMaxTokens() : 1000);
                
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", model);
                requestBody.put("messages", convertMessages(messages));
                requestBody.put("stream", false);
                requestBody.put("options", options);
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "api/chat"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                        .timeout(Duration.ofMinutes(5)) // 本地模型可能需要更长时间
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Local LLM API error: " + response.body());
                }
                
                // 解析响应
                Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
                Map<String, Object> message = (Map<String, Object>) responseBody.get("message");
                String content = (String) message.get("content");
                
                return new LLMResponse(content, 100, model, "stop");
                
            } catch (Exception e) {
                logger.error("Error calling local LLM API", e);
                throw new RuntimeException("Local LLM API call failed", e);
            }
        }, executor);
    }
    
    private List<Map<String, String>> convertMessages(List<ChatMessage> messages) {
        return messages.stream()
                .map(msg -> {
                    Map<String, String> msgMap = new HashMap<>();
                    msgMap.put("role", msg.getRole());
                    msgMap.put("content", msg.getContent());
                    return msgMap;
                })
                .collect(java.util.stream.Collectors.toList());
    }
}

/**
 * 企业级LLM提供者（支持负载均衡和故障转移）
 * Enterprise LLM Provider with Load Balancing and Failover
 */
class EnterpriseLLMProvider implements LLMProvider {
    private static final Logger logger = LoggerFactory.getLogger(EnterpriseLLMProvider.class);
    
    private final List<LLMProvider> providers;
    private int currentProviderIndex = 0;
    
    public EnterpriseLLMProvider(List<LLMProvider> providers) {
        if (providers == null || providers.isEmpty()) {
            throw new IllegalArgumentException("At least one LLM provider is required");
        }
        this.providers = providers;
    }
    
    @Override
    public CompletableFuture<LLMResponse> generateChatCompletion(List<ChatMessage> messages, LLMConfig config) {
        return generateWithFailover(messages, config, 0);
    }
    
    private CompletableFuture<LLMResponse> generateWithFailover(List<ChatMessage> messages, LLMConfig config, int attempt) {
        if (attempt >= providers.size()) {
            return CompletableFuture.failedFuture(new RuntimeException("All LLM providers failed"));
        }
        
        LLMProvider currentProvider = providers.get((currentProviderIndex + attempt) % providers.size());
        
        return currentProvider.generateChatCompletion(messages, config)
                .exceptionally(throwable -> {
                    logger.warn("LLM provider {} failed, attempting failover", currentProvider.getClass().getSimpleName(), throwable);
                    return null;
                })
                .thenCompose(response -> {
                    if (response != null) {
                        // 成功，更新当前提供者索引
                        currentProviderIndex = (currentProviderIndex + attempt) % providers.size();
                        return CompletableFuture.completedFuture(response);
                    } else {
                        // 失败，尝试下一个提供者
                        return generateWithFailover(messages, config, attempt + 1);
                    }
                });
    }
}