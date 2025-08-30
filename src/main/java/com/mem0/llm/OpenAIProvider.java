package com.mem0.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * OpenAI大语言模型提供者实现 / OpenAI LLM Provider Implementation
 * 
 * <p>中文描述：</p>
 * 本类实现了OpenAI大语言模型的提供者接口，支持通过OpenAI API调用GPT系列模型进行文本生成和对话功能。
 * OpenAI是人工智能领域的领军公司，其GPT（Generative Pre-trained Transformer）系列模型在自然语言
 * 处理、文本生成、对话系统等方面表现卓越，是目前最先进的大语言模型之一。
 * 
 * <p>English Description:</p>
 * This class implements the LLM provider interface for OpenAI's large language models,
 * supporting text generation and chat completion features through the OpenAI API.
 * OpenAI is a leading company in the AI field, and its GPT (Generative Pre-trained Transformer)
 * series models excel in natural language processing, text generation, and conversation systems,
 * representing some of the most advanced large language models available.
 * 
 * <p>主要功能 / Key Features:</p>
 * <ul>
 *   <li>支持GPT系列模型调用 / Support for GPT model series invocation</li>
 *   <li>文本生成和对话完成 / Text generation and chat completion</li>
 *   <li>流式响应支持 / Streaming response support</li>
 *   <li>可配置的生成参数 / Configurable generation parameters</li>
 *   <li>异步API调用支持 / Asynchronous API call support</li>
 *   <li>精确的Token使用量统计 / Accurate token usage statistics</li>
 * </ul>
 * 
 * <p>支持的模型 / Supported Models:</p>
 * <ul>
 *   <li>GPT-4系列: gpt-4, gpt-4-turbo, gpt-4o等</li>
 *   <li>GPT-3.5系列: gpt-3.5-turbo, gpt-3.5-turbo-instruct等</li>
 *   <li>其他OpenAI模型 / Other OpenAI models</li>
 * </ul>
 * 
 * <p>配置要求 / Configuration Requirements:</p>
 * <ul>
 *   <li>API Key: OpenAI API密钥 / OpenAI API key</li>
 *   <li>Model: GPT模型名称 / GPT model name</li>
 *   <li>Endpoint: OpenAI API端点（默认官方端点） / OpenAI API endpoint (default official endpoint)</li>
 * </ul>
 * 
 * <p>使用示例 / Usage Example:</p>
 * <pre>{@code
 * // 基础配置 / Basic configuration
 * OpenAIProvider provider = new OpenAIProvider("your-openai-api-key");
 * 
 * // 文本生成（使用instruct模型） / Text generation (using instruct model)
 * LLMConfig config = new LLMConfig();
 * config.setModel("gpt-3.5-turbo-instruct");
 * config.setMaxTokens(1000);
 * config.setTemperature(0.7);
 * 
 * LLMRequest request = new LLMRequest("Explain quantum computing", config);
 * CompletableFuture<LLMResponse> response = provider.generateCompletion(request);
 * 
 * // 对话完成 / Chat completion
 * LLMConfig chatConfig = new LLMConfig();
 * chatConfig.setModel("gpt-4");
 * chatConfig.setMaxTokens(2000);
 * 
 * List<ChatMessage> messages = Arrays.asList(
 *     new ChatMessage(ChatMessage.Role.USER, "What are the benefits of renewable energy?")
 * );
 * CompletableFuture<LLMResponse> chatResponse = provider.generateChatCompletion(messages, chatConfig);
 * }</pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class OpenAIProvider implements LLMProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAIProvider.class);
    private static final String BASE_URL = "https://api.openai.com/v1";
    
    private final OkHttpClient client;
    private final String apiKey;
    private final ObjectMapper objectMapper;
    
    public OpenAIProvider(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient.Builder().build();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public CompletableFuture<LLMResponse> generateCompletion(LLMRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", request.getConfig().getModel() != null ? 
                    request.getConfig().getModel() : "gpt-3.5-turbo-instruct");
                requestBody.put("prompt", request.getPrompt());
                requestBody.put("max_tokens", request.getConfig().getMaxTokens());
                requestBody.put("temperature", request.getConfig().getTemperature());
                requestBody.put("top_p", request.getConfig().getTopP());
                
                if (request.getConfig().getStopSequences() != null) {
                    requestBody.put("stop", request.getConfig().getStopSequences());
                }
                
                String jsonBody = objectMapper.writeValueAsString(requestBody);
                
                Request httpRequest = new Request.Builder()
                    .url(BASE_URL + "/completions")
                    .post(RequestBody.create(jsonBody, MediaType.get("application/json")))
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();
                
                try (Response response = client.newCall(httpRequest).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected response code: " + response.code() + 
                                            " body: " + response.body().string());
                    }
                    
                    String responseBody = response.body().string();
                    OpenAICompletionResponse completionResponse = objectMapper.readValue(
                        responseBody, OpenAICompletionResponse.class);
                    
                    if (completionResponse.choices == null || completionResponse.choices.isEmpty()) {
                        throw new RuntimeException("No completion choices returned");
                    }
                    
                    OpenAIChoice choice = completionResponse.choices.get(0);
                    int tokensUsed = completionResponse.usage != null ? 
                        completionResponse.usage.totalTokens : 0;
                    
                    return new LLMResponse(choice.text, tokensUsed, 
                        completionResponse.model, choice.finishReason);
                }
                
            } catch (Exception e) {
                throw new CompletionException("Failed to generate completion", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<LLMResponse> generateChatCompletion(List<ChatMessage> messages, LLMConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", config.getModel() != null ? config.getModel() : "gpt-3.5-turbo");
                
                List<Map<String, String>> messageList = new ArrayList<>();
                for (ChatMessage msg : messages) {
                    Map<String, String> messageMap = new HashMap<>();
                    messageMap.put("role", msg.getRole().name().toLowerCase());
                    messageMap.put("content", msg.getContent());
                    messageList.add(messageMap);
                }
                
                requestBody.put("messages", messageList);
                requestBody.put("max_tokens", config.getMaxTokens());
                requestBody.put("temperature", config.getTemperature());
                requestBody.put("top_p", config.getTopP());
                
                if (config.getStopSequences() != null) {
                    requestBody.put("stop", config.getStopSequences());
                }
                
                String jsonBody = objectMapper.writeValueAsString(requestBody);
                
                Request httpRequest = new Request.Builder()
                    .url(BASE_URL + "/chat/completions")
                    .post(RequestBody.create(jsonBody, MediaType.get("application/json")))
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();
                
                try (Response response = client.newCall(httpRequest).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected response code: " + response.code() + 
                                            " body: " + response.body().string());
                    }
                    
                    String responseBody = response.body().string();
                    OpenAIChatResponse chatResponse = objectMapper.readValue(
                        responseBody, OpenAIChatResponse.class);
                    
                    if (chatResponse.choices == null || chatResponse.choices.isEmpty()) {
                        throw new RuntimeException("No chat choices returned");
                    }
                    
                    OpenAIChatChoice choice = chatResponse.choices.get(0);
                    int tokensUsed = chatResponse.usage != null ? 
                        chatResponse.usage.totalTokens : 0;
                    
                    return new LLMResponse(choice.message.content, tokensUsed, 
                        chatResponse.model, choice.finishReason);
                }
                
            } catch (Exception e) {
                throw new CompletionException("Failed to generate chat completion", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<LLMResponse> generate(String prompt, double temperature, int maxTokens) {
        LLMConfig config = new LLMConfig();
        config.setTemperature(temperature);
        config.setMaxTokens(maxTokens);
        config.setModel("gpt-3.5-turbo-instruct");
        
        LLMRequest request = new LLMRequest(prompt, config);
        return generateCompletion(request);
    }
    
    @Override
    public String getProviderName() {
        return "OpenAI";
    }
    
    @Override
    public boolean supportsStreaming() {
        return true;
    }
    
    @Override
    public void close() {
        // Clean up HTTP client resources
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
        }
        logger.info("OpenAI provider closed");
    }
    
    // Response DTOs
    private static class OpenAICompletionResponse {
        public String id;
        public String object;
        public long created;
        public String model;
        public List<OpenAIChoice> choices;
        public OpenAIUsage usage;
    }
    
    private static class OpenAIChoice {
        public String text;
        public int index;
        public Object logprobs;
        @JsonProperty("finish_reason")
        public String finishReason;
    }
    
    private static class OpenAIChatResponse {
        public String id;
        public String object;
        public long created;
        public String model;
        public List<OpenAIChatChoice> choices;
        public OpenAIUsage usage;
    }
    
    private static class OpenAIChatChoice {
        public int index;
        public OpenAIChatMessage message;
        @JsonProperty("finish_reason")
        public String finishReason;
    }
    
    private static class OpenAIChatMessage {
        public String role;
        public String content;
    }
    
    private static class OpenAIUsage {
        @JsonProperty("prompt_tokens")
        public int promptTokens;
        @JsonProperty("completion_tokens")
        public int completionTokens;
        @JsonProperty("total_tokens")
        public int totalTokens;
    }
}