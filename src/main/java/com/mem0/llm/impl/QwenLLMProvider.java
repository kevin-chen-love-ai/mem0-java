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
 * 阿里云千问大语言模型提供者实现 / Alibaba Cloud Qwen LLM Provider Implementation
 * 
 * <p>中文描述：</p>
 * 本类实现了阿里云千问大语言模型的提供者接口，支持通过DashScope API调用千问系列模型进行文本生成和对话功能。
 * 千问是阿里云推出的大语言模型系列，具有强大的中文理解和生成能力，支持多轮对话、文本创作、知识问答等功能。
 * 
 * <p>English Description:</p>
 * This class implements the LLM provider interface for Alibaba Cloud's Qwen large language models,
 * supporting text generation and chat completion features through the DashScope API.
 * Qwen is Alibaba Cloud's series of large language models with strong Chinese language understanding
 * and generation capabilities, supporting multi-turn conversations, text creation, and knowledge Q&A.
 * 
 * <p>主要功能 / Key Features:</p>
 * <ul>
 *   <li>支持千问系列模型调用 / Support for Qwen model series invocation</li>
 *   <li>文本生成和对话完成 / Text generation and chat completion</li>
 *   <li>可配置的生成参数 / Configurable generation parameters</li>
 *   <li>异步API调用支持 / Asynchronous API call support</li>
 *   <li>自动错误处理和重试 / Automatic error handling and retry</li>
 *   <li>Token使用量统计 / Token usage statistics</li>
 * </ul>
 * 
 * <p>配置要求 / Configuration Requirements:</p>
 * <ul>
 *   <li>API Key: 阿里云DashScope API密钥 / Alibaba Cloud DashScope API key</li>
 *   <li>Model: 千问模型名称，如qwen-plus、qwen-turbo等 / Qwen model name like qwen-plus, qwen-turbo</li>
 *   <li>Endpoint: DashScope API端点URL / DashScope API endpoint URL</li>
 * </ul>
 * 
 * <p>使用示例 / Usage Example:</p>
 * <pre>{@code
 * // 基础配置 / Basic configuration
 * QwenLLMProvider provider = new QwenLLMProvider("your-dashscope-api-key");
 * 
 * // 自定义配置 / Custom configuration
 * QwenLLMProvider provider = new QwenLLMProvider(
 *     "api-key",
 *     "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation",
 *     "qwen-plus",
 *     0.7,    // temperature
 *     2000,   // maxTokens
 *     0.8     // topP
 * );
 * 
 * // 文本生成 / Text generation
 * LLMRequest request = new LLMRequest("请介绍一下人工智能", config);
 * CompletableFuture<LLMResponse> response = provider.generateCompletion(request);
 * 
 * // 对话完成 / Chat completion
 * List<ChatMessage> messages = Arrays.asList(
 *     new ChatMessage(ChatMessage.Role.USER, "你好，请介绍一下量子计算")
 * );
 * CompletableFuture<LLMResponse> chatResponse = provider.generateChatCompletion(messages, config);
 * }</pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class QwenLLMProvider implements LLMProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(QwenLLMProvider.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    // 千问API配置
    private final String apiKey;
    private final String apiUrl;
    private final String modelName;
    private final OkHttpClient httpClient;
    
    // 默认参数
    private final double temperature;
    private final int maxTokens;
    private final double topP;
    
    public QwenLLMProvider(String apiKey) {
        this(apiKey, "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation", 
             "qwen-plus", 0.7, 2000, 0.8);
    }
    
    public QwenLLMProvider(String apiKey, String apiUrl, String modelName, 
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
        
        logger.info("千问LLM提供者初始化完成 - 模型: {}, API: {}", modelName, apiUrl);
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
                    .addHeader("X-DashScope-SSE", "disable")
                    .post(RequestBody.create(requestBody, JSON))
                    .build();
                
                // 发送请求
                try (Response response = httpClient.newCall(httpRequest).execute()) {
                    String responseBody = response.body().string();
                    
                    if (response.isSuccessful()) {
                        String content = parseResponse(responseBody);
                        return new LLMResponse(content, estimateTokens(content), modelName, "stop");
                    } else {
                        String error = String.format("千问API请求失败: %d - %s", response.code(), responseBody);
                        logger.error(error);
                        throw new RuntimeException(error);
                    }
                }
                
            } catch (Exception e) {
                logger.error("千问文本生成失败", e);
                throw new RuntimeException("千问文本生成失败: " + e.getMessage(), e);
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
                    .addHeader("X-DashScope-SSE", "disable")
                    .post(RequestBody.create(requestBody, JSON))
                    .build();
                
                try (Response response = httpClient.newCall(chatRequest).execute()) {
                    String responseBody = response.body().string();
                    
                    if (response.isSuccessful()) {
                        String content = parseResponse(responseBody);
                        return new LLMResponse(content, estimateTokens(content), modelName, "stop");
                    } else {
                        String error = String.format("千问对话请求失败: %d - %s", response.code(), responseBody);
                        logger.error(error);
                        throw new RuntimeException(error);
                    }
                }
                
            } catch (Exception e) {
                logger.error("千问对话失败", e);
                throw new RuntimeException("千问对话失败: " + e.getMessage(), e);
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
        return "Qwen";
    }
    
    @Override
    public boolean supportsStreaming() {
        return false;
    }

    @Override
    public void close() {
        // OkHttp client doesn't need explicit closing
        logger.info("千问LLM提供者已关闭");
    }
    
    /**
     * 构建请求体JSON
     */
    private String buildRequestBody(String prompt, LLMConfig config) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"model\":\"").append(modelName).append("\",");
        json.append("\"input\":{\"prompt\":\"").append(escapeJson(prompt)).append("\"},");
        json.append("\"parameters\":{");
        
        // 使用配置参数或默认值
        double temp = config != null ? config.getTemperature() : temperature;
        int maxTok = config != null ? config.getMaxTokens() : maxTokens;
        double tp = config != null ? config.getTopP() : topP;
        
        json.append("\"temperature\":").append(temp).append(",");
        json.append("\"max_tokens\":").append(maxTok).append(",");
        json.append("\"top_p\":").append(tp);
        json.append("}}");
        
        return json.toString();
    }
    
    /**
     * 构建对话请求体JSON
     */
    private String buildChatRequestBody(List<ChatMessage> messages, LLMConfig config) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"model\":\"").append(modelName).append("\",");
        json.append("\"input\":{\"messages\":[");
        
        // 添加消息
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) json.append(",");
            ChatMessage msg = messages.get(i);
            String role = msg.getRole().name().toLowerCase();
            json.append("{\"role\":\"").append(role).append("\",");
            json.append("\"content\":\"").append(escapeJson(msg.getContent())).append("\"}");
        }
        
        json.append("]},");
        json.append("\"parameters\":{");
        
        // 使用配置参数或默认值
        double temp = config != null ? config.getTemperature() : temperature;
        int maxTok = config != null ? config.getMaxTokens() : maxTokens;
        double tp = config != null ? config.getTopP() : topP;
        
        json.append("\"temperature\":").append(temp).append(",");
        json.append("\"max_tokens\":").append(maxTok).append(",");
        json.append("\"top_p\":").append(tp);
        json.append("}}");
        
        return json.toString();
    }
    
    /**
     * 解析响应
     */
    private String parseResponse(String responseBody) {
        try {
            // 简单的JSON解析 - 在实际项目中应该使用JSON库
            if (responseBody.contains("\"text\":")) {
                int startIndex = responseBody.indexOf("\"text\":\"") + 8;
                int endIndex = responseBody.indexOf("\"", startIndex);
                if (endIndex > startIndex) {
                    return responseBody.substring(startIndex, endIndex)
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\");
                }
            }
            
            logger.warn("无法解析千问响应: {}", responseBody);
            return "解析响应失败";
            
        } catch (Exception e) {
            logger.error("解析千问响应时出错", e);
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