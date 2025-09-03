package com.mem0.unit.llm;

import com.mem0.llm.LLMProvider;
import com.mem0.util.TestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QwenLLMProvider简化集成测试 - Simplified integration tests for QwenLLMProvider
 * 
 * 使用TestConfiguration统一管理LLMProvider，验证基本功能
 * Uses TestConfiguration to manage LLMProvider uniformly and verify basic functionality
 */
public class QwenLLMProviderSimpleTest {

    private LLMProvider llmProvider;

    @BeforeEach
    void setUp() {
        llmProvider = TestConfiguration.getLLMProvider();
    }

    @Test
    void testGenerateCompletion_Success() throws Exception {
        if (TestConfiguration.shouldSkipTest("testGenerateCompletion_Success", true, false)) {
            return;
        }

        String prompt = "请简单介绍一下人工智能";
        LLMProvider.LLMConfig config = new LLMProvider.LLMConfig("qwen-plus");
        config.setTemperature(0.7);
        config.setMaxTokens(100);

        LLMProvider.LLMRequest request = new LLMProvider.LLMRequest(prompt, config);
        LLMProvider.LLMResponse response = llmProvider.generateCompletion(request).get(30, TimeUnit.SECONDS);

        assertNotNull(response);
        assertNotNull(response.getContent());
        assertFalse(response.getContent().trim().isEmpty());
        assertTrue(response.getTokensUsed() > 0);
    }

    @Test
    void testGenerateChatCompletion_Success() throws Exception {
        if (TestConfiguration.shouldSkipTest("testGenerateChatCompletion_Success", true, false)) {
            return;
        }

        List<LLMProvider.ChatMessage> messages = Arrays.asList(
            new LLMProvider.ChatMessage(LLMProvider.ChatMessage.Role.SYSTEM, "你是一个有用的助手"),
            new LLMProvider.ChatMessage(LLMProvider.ChatMessage.Role.USER, "什么是机器学习？")
        );

        LLMProvider.LLMConfig config = new LLMProvider.LLMConfig("qwen-plus");
        config.setTemperature(0.5);
        config.setMaxTokens(150);

        LLMProvider.LLMResponse response = llmProvider.generateChatCompletion(messages, config)
            .get(30, TimeUnit.SECONDS);

        assertNotNull(response);
        assertNotNull(response.getContent());
        assertFalse(response.getContent().trim().isEmpty());
        assertTrue(response.getTokensUsed() > 0);
    }

    @Test
    void testEmptyMessages() throws Exception {
        if (TestConfiguration.shouldSkipTest("testEmptyMessages", true, false)) {
            return;
        }

        List<LLMProvider.ChatMessage> emptyMessages = Arrays.asList();
        LLMProvider.LLMConfig config = new LLMProvider.LLMConfig();

        // 空消息应该抛出异常或优雅处理
        assertThrows(Exception.class, () -> {
            llmProvider.generateChatCompletion(emptyMessages, config).get(10, TimeUnit.SECONDS);
        });
    }

    @Test
    void testProviderAvailability() {
        // 这个测试总是可以运行，不需要实际的API调用
        boolean isAvailable = TestConfiguration.isLLMProviderAvailable();
        
        if (isAvailable) {
            assertNotNull(llmProvider);
            System.out.println("LLM Provider is available and ready for testing");
        } else {
            System.out.println("LLM Provider is not available - tests will be skipped");
        }
    }
}