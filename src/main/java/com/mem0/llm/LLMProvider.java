package com.mem0.llm;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 大语言模型提供者接口
 * Large Language Model (LLM) provider interface for text generation and chat completion
 * 
 * <p>此接口定义了与各种大语言模型服务集成的核心方法，支持文本生成和对话完成功能。 / 
 * This interface defines core methods for integrating with various large language model services, supporting text generation and chat completion functionality.</p>
 * 
 * <p>支持的LLM提供者包括 / Supported LLM providers include:</p>
 * <ul>
 *   <li>OpenAI GPT 系列 / OpenAI GPT series</li>
 *   <li>Anthropic Claude</li>
 *   <li>Google Gemini/Bard</li>
 *   <li>Hugging Face Transformers</li>
 *   <li>本地部署的模型 / Locally deployed models</li>
 * </ul>
 * 
 * <p>使用示例 / Usage example:</p>
 * <pre>{@code
 * LLMProvider provider = new OpenAIProvider(apiKey);
 * 
 * // 简单文本生成
 * LLMConfig config = new LLMConfig("gpt-3.5-turbo");
 * config.setTemperature(0.7);
 * LLMRequest request = new LLMRequest("请解释人工智能", config);
 * LLMResponse response = provider.generateCompletion(request).join();
 * 
 * // 对话模式
 * List<ChatMessage> messages = Arrays.asList(
 *     new ChatMessage(ChatMessage.Role.SYSTEM, "你是一个AI助手"),
 *     new ChatMessage(ChatMessage.Role.USER, "你好")
 * );
 * LLMResponse chatResponse = provider.generateChatCompletion(messages, config).join();
 * }</pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public interface LLMProvider {
    
    /**
     * 生成文本完成响应
     * Generates text completion response
     * 
     * <p>基于给定的提示词和配置生成文本完成。适用于简单的文本生成任务。 / 
     * Generates text completion based on given prompt and configuration. Suitable for simple text generation tasks.</p>
     * 
     * @param request LLM请求对象，包含提示词和配置 / LLM request object containing prompt and configuration
     * @return CompletableFuture<LLMResponse>，包含生成的文本和相关信息 / CompletableFuture<LLMResponse> containing generated text and related information
     * 
     * @throws IllegalArgumentException 如果request为null或包含无效参数 / if request is null or contains invalid parameters
     * @throws RuntimeException 如果文本生成失败 / if text generation fails
     */
    CompletableFuture<LLMResponse> generateCompletion(LLMRequest request);
    
    /**
     * 生成对话完成响应
     * Generates chat completion response
     * 
     * <p>基于对话历史和配置生成对话响应。支持多轮对话，适用于聊天机器人和交互式AI应用。 / 
     * Generates chat response based on conversation history and configuration. Supports multi-turn conversations, suitable for chatbots and interactive AI applications.</p>
     * 
     * @param messages 对话消息列表，按时间顺序排列 / List of chat messages in chronological order
     * @param config LLM配置参数 / LLM configuration parameters
     * @return CompletableFuture<LLMResponse>，包含生成的对话响应 / CompletableFuture<LLMResponse> containing generated chat response
     * 
     * @throws IllegalArgumentException 如果messages为null/空或config无效 / if messages is null/empty or config is invalid
     * @throws RuntimeException 如果对话生成失败 / if chat generation fails
     */
    CompletableFuture<LLMResponse> generateChatCompletion(List<ChatMessage> messages, LLMConfig config);
    
    /**
     * 生成简单文本响应（简化接口）
     * Generates simple text response (simplified interface)
     * 
     * <p>这是一个简化的文本生成接口，接受提示词、温度和最大token数参数。 / 
     * This is a simplified text generation interface that accepts prompt, temperature, and max tokens parameters.</p>
     * 
     * @param prompt 提示词文本 / Prompt text
     * @param temperature 温度参数，控制输出的随机性 / Temperature parameter controlling output randomness
     * @param maxTokens 最大token数量 / Maximum number of tokens
     * @return CompletableFuture<LLMResponse>，包含生成的文本响应 / CompletableFuture<LLMResponse> containing generated text response
     * 
     * @throws IllegalArgumentException 如果prompt为null或参数无效 / if prompt is null or parameters are invalid
     * @throws RuntimeException 如果文本生成失败 / if text generation fails
     */
    CompletableFuture<LLMResponse> generate(String prompt, double temperature, int maxTokens);
    
    /**
     * 获取提供者名称
     * Gets the provider name
     * 
     * @return 提供者名称，用于标识和日志记录 / Provider name for identification and logging
     */
    String getProviderName();
    
    /**
     * 检查是否支持流式响应
     * Checks if streaming response is supported
     * 
     * <p>流式响应允许逐步接收生成的文本，提供更好的用户体验。 / 
     * Streaming response allows receiving generated text progressively, providing better user experience.</p>
     * 
     * @return true表示支持流式响应 / true if streaming response is supported
     */
    boolean supportsStreaming();
    
    /**
     * 关闭提供者并释放资源
     * Closes the provider and releases resources
     * 
     * <p>关闭所有连接，释放线程池、内存和其他系统资源。 / 
     * Closes all connections and releases thread pools, memory, and other system resources.</p>
     */
    void close();
    
    /**
     * LLM请求封装类
     * LLM request wrapper class
     * 
     * <p>封装LLM文本生成请求的所有参数，包括提示词和配置。 / 
     * Encapsulates all parameters for LLM text generation request, including prompt and configuration.</p>
     */
    static class LLMRequest {
        private final String prompt;
        private final LLMConfig config;
        
        /**
         * 创建LLM请求
         * Creates an LLM request
         * 
         * @param prompt 提示词文本 / Prompt text
         * @param config LLM配置参数 / LLM configuration parameters
         */
        public LLMRequest(String prompt, LLMConfig config) {
            this.prompt = prompt;
            this.config = config;
        }
        
        /** @return 提示词文本 / Prompt text */
        public String getPrompt() { return prompt; }
        
        /** @return LLM配置参数 / LLM configuration parameters */
        public LLMConfig getConfig() { return config; }
    }
    
    /**
     * 对话消息类
     * Chat message class
     * 
     * <p>表示对话中的一条消息，包含角色和内容。 / 
     * Represents a message in a conversation, containing role and content.</p>
     */
    static class ChatMessage {
        /**
         * 消息角色枚举
         * Message role enumeration
         * 
         * <p>定义对话中不同参与者的角色。 / 
         * Defines roles of different participants in conversation.</p>
         */
        public enum Role {
            /** 系统角色，用于设置行为和上下文 / System role for setting behavior and context */
            SYSTEM, 
            /** 用户角色，表示人类用户输入 / User role representing human user input */
            USER, 
            /** 助手角色，表示AI助手的回复 / Assistant role representing AI assistant's response */
            ASSISTANT
        }
        
        private final Role role;
        private final String content;
        
        /**
         * 创建对话消息
         * Creates a chat message
         * 
         * @param role 消息角色 / Message role
         * @param content 消息内容 / Message content
         */
        public ChatMessage(Role role, String content) {
            this.role = role;
            this.content = content;
        }
        
        /** @return 消息角色 / Message role */
        public Role getRole() { return role; }
        
        /** @return 消息内容 / Message content */
        public String getContent() { return content; }
    }
    
    /**
     * LLM响应类
     * LLM response class
     * 
     * <p>包含LLM生成的文本内容和相关元数据，如使用的token数量等。 / 
     * Contains LLM generated text content and related metadata such as token usage.</p>
     */
    static class LLMResponse {
        private final String content;
        private final int tokensUsed;
        private final String model;
        private final String finishReason;
        
        /**
         * 创建LLM响应
         * Creates an LLM response
         * 
         * @param content 生成的文本内容 / Generated text content
         * @param tokensUsed 使用的token数量 / Number of tokens used
         * @param model 使用的模型名称 / Name of the model used
         * @param finishReason 生成结束的原因 / Reason for generation completion
         */
        public LLMResponse(String content, int tokensUsed, String model, String finishReason) {
            this.content = content;
            this.tokensUsed = tokensUsed;
            this.model = model;
            this.finishReason = finishReason;
        }
        
        /** @return 生成的文本内容 / Generated text content */
        public String getContent() { return content; }
        
        /** @return 使用的token数量 / Number of tokens used */
        public int getTokensUsed() { return tokensUsed; }
        
        /** @return 使用的模型名称 / Name of the model used */
        public String getModel() { return model; }
        
        /** @return 生成结束的原因 / Reason for generation completion */
        public String getFinishReason() { return finishReason; }
        
        /**
         * 响应构建器类（用于可变响应）
         * Response builder class (for mutable responses)
         * 
         * <p>使用构建者模式创建LLMResponse对象，支持链式调用。 / 
         * Uses builder pattern to create LLMResponse objects, supports method chaining.</p>
         */
        public static class Builder {
            private String content;
            private int tokensUsed;
            private String model;
            private String finishReason;
            
            /** 设置文本内容 / Set text content */
            public Builder setContent(String content) { this.content = content; return this; }
            
            /** 设置token使用数量 / Set token usage count */
            public Builder setTokensUsed(int tokensUsed) { this.tokensUsed = tokensUsed; return this; }
            
            /** 设置模型名称 / Set model name */
            public Builder setModel(String model) { this.model = model; return this; }
            
            /** 设置结束原因 / Set finish reason */
            public Builder setFinishReason(String finishReason) { this.finishReason = finishReason; return this; }
            
            /**
             * 构建LLMResponse对象
             * Builds the LLMResponse object
             * 
             * @return 构建完成的LLMResponse对象 / Built LLMResponse object
             */
            public LLMResponse build() {
                return new LLMResponse(content, tokensUsed, model, finishReason);
            }
        }
        
        /**
         * 创建构建器实例
         * Creates a builder instance
         * 
         * @return 新的构建器实例 / New builder instance
         */
        public static Builder builder() { return new Builder(); }
    }
    
    /**
     * LLM配置类
     * LLM configuration class
     * 
     * <p>包含LLM文本生成的所有配置参数，如模型名、温度、最大token数等。 / 
     * Contains all configuration parameters for LLM text generation, such as model name, temperature, max tokens, etc.</p>
     */
    static class LLMConfig {
        /** 模型名称 / Model name */
        private String model;
        
        /** 温度参数，控制输出的随机性 / Temperature parameter controlling output randomness */
        private double temperature = 0.7;
        
        /** 最大token数量 / Maximum number of tokens */
        private int maxTokens = 1000;
        
        /** Top-p采样参数 / Top-p sampling parameter */
        private double topP = 1.0;
        
        /** Top-k采样参数 / Top-k sampling parameter */
        private int topK = 50;
        
        /** 停止序列列表 / List of stop sequences */
        private List<String> stopSequences;
        
        /**
         * 创建默认配置
         * Creates default configuration
         */
        public LLMConfig() {}
        
        /**
         * 创建指定模型的配置
         * Creates configuration with specified model
         * 
         * @param model 模型名称 / Model name
         */
        public LLMConfig(String model) {
            this.model = model;
        }
        
        // Getters and setters
        /** @return 模型名称 / Model name */
        public String getModel() { return model; }
        
        /** @param model 模型名称 / Model name */
        public void setModel(String model) { this.model = model; }
        
        /** @return 温度参数（0.0-2.0） / Temperature parameter (0.0-2.0) */
        public double getTemperature() { return temperature; }
        
        /** @param temperature 温度参数，控制输出的随机性 / Temperature parameter controlling output randomness */
        public void setTemperature(double temperature) { this.temperature = temperature; }
        
        /** @return 最大token数量 / Maximum number of tokens */
        public int getMaxTokens() { return maxTokens; }
        
        /** @param maxTokens 最大token数量 / Maximum number of tokens */
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        
        /** @return Top-p采样参数（0.0-1.0） / Top-p sampling parameter (0.0-1.0) */
        public double getTopP() { return topP; }
        
        /** @param topP Top-p采样参数 / Top-p sampling parameter */
        public void setTopP(double topP) { this.topP = topP; }
        
        /** @return Top-k采样参数 / Top-k sampling parameter */
        public int getTopK() { return topK; }
        
        /** @param topK Top-k采样参数 / Top-k sampling parameter */
        public void setTopK(int topK) { this.topK = topK; }
        
        /** @return 停止序列列表 / List of stop sequences */
        public List<String> getStopSequences() { return stopSequences; }
        
        /** @param stopSequences 停止序列列表，遇到时停止生成 / List of stop sequences, generation stops when encountered */
        public void setStopSequences(List<String> stopSequences) { this.stopSequences = stopSequences; }
    }
}