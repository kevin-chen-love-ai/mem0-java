package com.mem0.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 模拟大语言模型提供者实现 / Mock LLM Provider Implementation
 * 
 * <p>中文描述：</p>
 * 本类实现了一个模拟的大语言模型提供者，主要用于测试、开发和演示目的。该提供者不会调用任何真实的
 * LLM API服务，而是返回预定义的模拟响应。这对于单元测试、集成测试以及在没有API密钥或网络连接
 * 的情况下进行开发非常有用。
 * 
 * <p>English Description:</p>
 * This class implements a mock large language model provider primarily used for testing,
 * development, and demonstration purposes. The provider does not call any real LLM API services,
 * but instead returns predefined mock responses. This is very useful for unit testing,
 * integration testing, and development when API keys or network connections are not available.
 * 
 * <p>主要功能 / Key Features:</p>
 * <ul>
 *   <li>模拟文本生成响应 / Mock text generation responses</li>
 *   <li>模拟对话完成响应 / Mock chat completion responses</li>
 *   <li>基于输入内容的智能模拟 / Intelligent simulation based on input content</li>
 *   <li>无需真实API密钥或网络连接 / No real API keys or network connections required</li>
 *   <li>快速响应，适合测试环境 / Fast responses, suitable for testing environments</li>
 *   <li>可预测的行为模式 / Predictable behavior patterns</li>
 * </ul>
 * 
 * <p>适用场景 / Use Cases:</p>
 * <ul>
 *   <li>单元测试和集成测试 / Unit testing and integration testing</li>
 *   <li>本地开发和调试 / Local development and debugging</li>
 *   <li>演示和原型开发 / Demonstrations and prototyping</li>
 *   <li>CI/CD流水线测试 / CI/CD pipeline testing</li>
 *   <li>性能基准测试 / Performance benchmarking</li>
 * </ul>
 * 
 * <p>特点说明 / Characteristics:</p>
 * <ul>
 *   <li>零成本运行 / Zero cost operation</li>
 *   <li>无外部依赖 / No external dependencies</li>
 *   <li>确定性输出 / Deterministic output</li>
 *   <li>快速响应时间 / Fast response time</li>
 * </ul>
 * 
 * <p>使用示例 / Usage Example:</p>
 * <pre>{@code
 * // 创建模拟提供者 / Create mock provider
 * MockLLMProvider provider = new MockLLMProvider();
 * 
 * // 文本生成测试 / Text generation testing
 * LLMConfig config = new LLMConfig();
 * config.setMaxTokens(100);
 * 
 * LLMRequest request = new LLMRequest("Test prompt for mock response", config);
 * CompletableFuture<LLMResponse> response = provider.generateCompletion(request);
 * 
 * // 对话完成测试 / Chat completion testing
 * List<ChatMessage> messages = Arrays.asList(
 *     new ChatMessage(ChatMessage.Role.USER, "Hello, this is a test message")
 * );
 * CompletableFuture<LLMResponse> chatResponse = provider.generateChatCompletion(messages, config);
 * 
 * // 验证响应 / Verify responses
 * response.thenAccept(resp -> {
 *     assertNotNull(resp.getContent());
 *     assertTrue(resp.getContent().contains("Mock"));
 *     assertEquals("mock-model", resp.getModel());
 * });
 * }</pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class MockLLMProvider implements LLMProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(MockLLMProvider.class);
    
    @Override
    public CompletableFuture<LLMResponse> generateCompletion(LLMRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            logger.debug("Mock LLM completion request: {}", request.getPrompt());
            
            String mockResponse = "Mock completion response for prompt: " + 
                request.getPrompt().substring(0, Math.min(50, request.getPrompt().length())) + "...";
            
            return new LLMResponse(mockResponse, 100, "mock-model", "stop");
        });
    }
    
    @Override
    public CompletableFuture<LLMResponse> generateChatCompletion(List<ChatMessage> messages, LLMConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            logger.debug("Mock LLM chat completion request with {} messages", messages.size());
            
            StringBuilder contextBuilder = new StringBuilder();
            for (ChatMessage message : messages) {
                contextBuilder.append(message.getRole()).append(": ")
                    .append(message.getContent()).append("\n");
            }
            
            String lastUserMessage = messages.stream()
                .filter(msg -> msg.getRole() == ChatMessage.Role.USER)
                .reduce((first, second) -> second)
                .map(ChatMessage::getContent)
                .orElse("No user message");
            
            String mockResponse = "Mock response based on the conversation context. " +
                "The last user message was about: " + 
                lastUserMessage.substring(0, Math.min(100, lastUserMessage.length())) + 
                (lastUserMessage.length() > 100 ? "..." : "");
            
            return new LLMResponse(mockResponse, 150, "mock-chat-model", "stop");
        });
    }
    
    @Override
    public CompletableFuture<LLMResponse> generate(String prompt, double temperature, int maxTokens) {
        return CompletableFuture.supplyAsync(() -> {
            logger.debug("Mock LLM generate request: {}", prompt.substring(0, Math.min(50, prompt.length())));
            
            String mockResponse = "Mock generation response: " + 
                prompt.substring(0, Math.min(100, prompt.length())) + "...";
            
            // Limit response length based on maxTokens (rough approximation: 1 token = 4 characters)
            int maxCharacters = maxTokens * 4;
            if (mockResponse.length() > maxCharacters) {
                mockResponse = mockResponse.substring(0, maxCharacters) + "...";
            }
            
            return new LLMResponse(mockResponse, Math.min(mockResponse.length() / 4, maxTokens), "mock-model", "stop");
        });
    }
    
    @Override
    public String getProviderName() {
        return "Mock";
    }
    
    @Override
    public boolean supportsStreaming() {
        return false;
    }
    
    @Override
    public void close() {
        // No resources to close for mock provider
        logger.info("Mock LLM provider closed");
    }
}