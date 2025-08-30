package com.mem0.llm.impl;

import com.mem0.llm.LLMProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 硅基流动大语言模型提供者实现 / SiliconFlow LLM Provider Implementation
 * 
 * <p>中文描述：</p>
 * 本类实现了硅基流动平台大语言模型的提供者接口，支持通过SiliconFlow API调用平台上的各种开源大模型。
 * 硅基流动是一个专业的AI推理服务平台，提供高性能、低成本的大模型API服务，支持包括Qwen、ChatGLM、
 * LLaMA等多种主流开源模型的推理服务。
 * 
 * <p>English Description:</p>
 * This class implements the LLM provider interface for SiliconFlow platform's large language models,
 * supporting various open-source model invocations through the SiliconFlow API.
 * SiliconFlow is a professional AI inference service platform that provides high-performance,
 * low-cost large model API services, supporting inference for various mainstream open-source models
 * including Qwen, ChatGLM, LLaMA, and others.
 * 
 * <p>主要功能 / Key Features:</p>
 * <ul>
 *   <li>支持多种开源模型调用 / Support for various open-source model invocations</li>
 *   <li>OpenAI兼容的API格式 / OpenAI-compatible API format</li>
 *   <li>文本生成和对话完成 / Text generation and chat completion</li>
 *   <li>可配置的生成参数 / Configurable generation parameters</li>
 *   <li>异步API调用支持 / Asynchronous API call support</li>
 *   <li>高性能低延迟推理 / High-performance low-latency inference</li>
 * </ul>
 * 
 * <p>支持的模型 / Supported Models:</p>
 * <ul>
 *   <li>Qwen系列: qwen/Qwen2-7B-Instruct, qwen/Qwen2-72B-Instruct等</li>
 *   <li>ChatGLM系列: THUDM/chatglm3-6b等</li>
 *   <li>LLaMA系列: meta-llama/Meta-Llama-3-8B-Instruct等</li>
 *   <li>其他开源模型 / Other open-source models</li>
 * </ul>
 * 
 * <p>配置要求 / Configuration Requirements:</p>
 * <ul>
 *   <li>API Key: 硅基流动平台API密钥 / SiliconFlow platform API key</li>
 *   <li>Model: 模型名称，如qwen/Qwen2-7B-Instruct / Model name like qwen/Qwen2-7B-Instruct</li>
 *   <li>Endpoint: SiliconFlow API端点URL / SiliconFlow API endpoint URL</li>
 * </ul>
 * 
 * <p>使用示例 / Usage Example:</p>
 * <pre>{@code
 * // 基础配置 / Basic configuration
 * SiliconFlowLLMProvider provider = new SiliconFlowLLMProvider("your-siliconflow-api-key");
 * 
 * // 自定义配置 / Custom configuration
 * SiliconFlowLLMProvider provider = new SiliconFlowLLMProvider(
 *     "api-key",
 *     "https://api.siliconflow.cn/v1/chat/completions",
 *     "qwen/Qwen2-7B-Instruct",
 *     0.7,    // temperature
 *     1024,   // maxTokens
 *     0.7     // topP
 * );
 * 
 * // 文本生成 / Text generation
 * LLMRequest request = new LLMRequest("请解释一下机器学习的基本概念", config);
 * CompletableFuture<LLMResponse> response = provider.generateCompletion(request);
 * 
 * // 对话完成 / Chat completion
 * List<ChatMessage> messages = Arrays.asList(
 *     new ChatMessage(ChatMessage.Role.USER, "什么是深度学习？")
 * );
 * CompletableFuture<LLMResponse> chatResponse = provider.generateChatCompletion(messages, config);
 * }</pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class SiliconFlowLLMProvider implements LLMProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(SiliconFlowLLMProvider.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    // 硅基流动API配置
    private final String apiKey;
    private final String apiUrl;
    private final String modelName;
    private final OkHttpClient httpClient;
    
    // 默认参数
    private final double temperature;
    private final int maxTokens;
    private final double topP;
    
    public SiliconFlowLLMProvider(String apiKey) {
        this(apiKey, "https://api.siliconflow.cn/v1/chat/completions", 
             "qwen/Qwen2-7B-Instruct", 0.7, 1024, 0.7);
    }
    
    public SiliconFlowLLMProvider(String apiKey, String apiUrl, String modelName, 
                                 double temperature, int maxTokens, double topP) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.modelName = modelName;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.topP = topP;
        
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();
        
        logger.info("硅基流动LLM提供者初始化完成 - 模型: {}, API: {}", modelName, apiUrl);
    }

    @Override
    public CompletableFuture<LLMResponse> generateCompletion(LLMRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String prompt = request.getPrompt();
                logger.debug("开始生成文本，提示词长度: {}", prompt.length());
                
                // 构建请求JSON
                String requestBody = buildRequestBody(prompt, request.getConfig());
                
                // 创建请求
                Request httpRequest = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, JSON))
                    .build();
                
                // 发送请求
                try (Response response = httpClient.newCall(httpRequest).execute()) {
                    String responseBody = response.body().string();
                    
                    if (response.isSuccessful()) {
                        String content = parseResponse(responseBody);
                        return new LLMResponse(content, estimateTokens(content), modelName, "stop");
                    } else {
                        String error = String.format("硅基流动API请求失败: %d - %s", response.code(), responseBody);
                        logger.error(error);
                        throw new RuntimeException(error);
                    }
                }
                
            } catch (Exception e) {
                logger.error("硅基流动文本生成失败", e);
                throw new RuntimeException("硅基流动文本生成失败: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<LLMResponse> generateChatCompletion(List<ChatMessage> messages, LLMConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("开始对话，消息数量: {}", messages.size());
                
                // 构建对话请求
                String requestBody = buildChatRequestBody(messages, config);
                
                Request chatRequest = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, JSON))
                    .build();
                
                try (Response response = httpClient.newCall(chatRequest).execute()) {
                    String responseBody = response.body().string();
                    
                    if (response.isSuccessful()) {
                        String content = parseResponse(responseBody);
                        return new LLMResponse(content, estimateTokens(content), modelName, "stop");
                    } else {
                        String error = String.format("硅基流动对话请求失败: %d - %s", response.code(), responseBody);
                        logger.error(error);
                        throw new RuntimeException(error);
                    }
                }
                
            } catch (Exception e) {
                logger.error("硅基流动对话失败", e);
                throw new RuntimeException("硅基流动对话失败: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<LLMResponse> generate(String prompt, double temperature, int maxTokens) {
        LLMConfig config = new LLMConfig();
        config.setTemperature(temperature);
        config.setMaxTokens(maxTokens);
        config.setModel(this.modelName);
        
        LLMRequest request = new LLMRequest(prompt, config);
        return generateCompletion(request);
    }
    
    @Override
    public String getProviderName() {
        return "SiliconFlow";
    }
    
    @Override
    public boolean supportsStreaming() {
        return false;
    }

    @Override
    public void close() {
        // OkHttp client doesn't need explicit closing
        logger.info("硅基流动LLM提供者已关闭");
    }
    
    /**
     * 构建请求体JSON (OpenAI格式)
     */
    private String buildRequestBody(String prompt, LLMConfig config) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"model\":\"").append(modelName).append("\",");
        json.append("\"messages\":[{\"role\":\"user\",\"content\":\"").append(escapeJson(prompt)).append("\"}],");
        json.append("\"stream\":false,");
        
        // 使用配置参数或默认值
        double temp = config != null ? config.getTemperature() : temperature;
        int maxTok = config != null ? config.getMaxTokens() : maxTokens;
        double tp = config != null ? config.getTopP() : topP;
        
        json.append("\"temperature\":").append(temp).append(",");
        json.append("\"max_tokens\":").append(maxTok).append(",");
        json.append("\"top_p\":").append(tp);
        json.append("}");
        
        return json.toString();
    }
    
    /**
     * 构建对话请求体JSON
     */
    private String buildChatRequestBody(List<ChatMessage> messages, LLMConfig config) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"model\":\"").append(modelName).append("\",");
        json.append("\"messages\":[");
        
        // 添加消息
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) json.append(",");
            ChatMessage msg = messages.get(i);
            String role = msg.getRole().name().toLowerCase();
            json.append("{\"role\":\"").append(role).append("\",");
            json.append("\"content\":\"").append(escapeJson(msg.getContent())).append("\"}");
        }
        
        json.append("],");
        json.append("\"stream\":false,");
        
        // 使用配置参数或默认值
        double temp = config != null ? config.getTemperature() : temperature;
        int maxTok = config != null ? config.getMaxTokens() : maxTokens;
        double tp = config != null ? config.getTopP() : topP;
        
        json.append("\"temperature\":").append(temp).append(",");
        json.append("\"max_tokens\":").append(maxTok).append(",");
        json.append("\"top_p\":").append(tp);
        json.append("}");
        
        return json.toString();
    }
    
    /**
     * 解析响应 (OpenAI格式)
     */
    private String parseResponse(String responseBody) {
        try {
            // 简单的JSON解析 - 寻找content字段
            if (responseBody.contains("\"content\":")) {
                int startIndex = responseBody.indexOf("\"content\":\"") + 11;
                int endIndex = responseBody.indexOf("\"", startIndex);
                if (endIndex > startIndex) {
                    return responseBody.substring(startIndex, endIndex)
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\");
                }
            }
            
            logger.warn("无法解析硅基流动响应: {}", responseBody);
            return "解析响应失败";
            
        } catch (Exception e) {
            logger.error("解析硅基流动响应时出错", e);
            throw new RuntimeException("解析响应失败: " + e.getMessage());
        }
    }
    
    /**
     * JSON字符串转义
     */
    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    /**
     * 估算token数量（简单实现）
     */
    private int estimateTokens(String text) {
        // 简单的token估算，实际应该使用专门的tokenizer
        return Math.max(1, text.length() / 4);
    }
}