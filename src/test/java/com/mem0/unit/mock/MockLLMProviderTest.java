package com.mem0.unit.mock;

import com.mem0.llm.LLMProvider;
import com.mem0.llm.MockLLMProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class MockLLMProviderTest {
    
    private MockLLMProvider llmProvider;
    
    @BeforeEach
    void setUp() {
        llmProvider = new MockLLMProvider();
    }
    
    @Test
    void testGetProviderName() {
        assertEquals("Mock", llmProvider.getProviderName());
    }
    
    @Test
    void testSupportsStreaming() {
        assertFalse(llmProvider.supportsStreaming());
    }
    
    @Test
    @Timeout(5)
    void testGenerateCompletion() throws Exception {
        String prompt = "Write a short story about a robot learning to cook";
        LLMProvider.LLMConfig config = new LLMProvider.LLMConfig("mock-model");
        config.setMaxTokens(500);
        config.setTemperature(0.7);
        
        LLMProvider.LLMRequest request = new LLMProvider.LLMRequest(prompt, config);
        LLMProvider.LLMResponse response = llmProvider.generateCompletion(request).get(5, TimeUnit.SECONDS);
        
        assertNotNull(response);
        assertNotNull(response.getContent());
        assertFalse(response.getContent().trim().isEmpty());
        assertTrue(response.getContent().contains("Mock completion response"));
        assertTrue(response.getContent().contains(prompt.substring(0, Math.min(50, prompt.length()))));
        
        assertEquals(100, response.getTokensUsed());
        assertEquals("mock-model", response.getModel());
        assertEquals("stop", response.getFinishReason());
    }
    
    @Test
    @Timeout(5)
    void testGenerateChatCompletion() throws Exception {
        List<LLMProvider.ChatMessage> messages = Arrays.asList(
            new LLMProvider.ChatMessage(LLMProvider.ChatMessage.Role.SYSTEM, 
                "You are a helpful assistant that provides cooking advice."),
            new LLMProvider.ChatMessage(LLMProvider.ChatMessage.Role.USER, 
                "How do I make scrambled eggs?"),
            new LLMProvider.ChatMessage(LLMProvider.ChatMessage.Role.ASSISTANT, 
                "To make scrambled eggs, crack eggs into a bowl, whisk them, and cook in a pan."),
            new LLMProvider.ChatMessage(LLMProvider.ChatMessage.Role.USER, 
                "What temperature should I use?")
        );
        
        LLMProvider.LLMConfig config = new LLMProvider.LLMConfig("mock-chat-model");
        config.setTemperature(0.8);
        config.setMaxTokens(200);
        
        LLMProvider.LLMResponse response = llmProvider.generateChatCompletion(messages, config)
            .get(5, TimeUnit.SECONDS);
        
        assertNotNull(response);
        assertNotNull(response.getContent());
        assertFalse(response.getContent().trim().isEmpty());
        
        assertTrue(response.getContent().contains("Mock response"));
        assertTrue(response.getContent().contains("What temperature should I use?"));
        
        assertEquals(150, response.getTokensUsed());
        assertEquals("mock-chat-model", response.getModel());
        assertEquals("stop", response.getFinishReason());
    }
    
    @Test
    @Timeout(5)
    void testGenerateChatCompletionSingleMessage() throws Exception {
        List<LLMProvider.ChatMessage> messages = Arrays.asList(
            new LLMProvider.ChatMessage(LLMProvider.ChatMessage.Role.USER, "Hello!")
        );
        
        LLMProvider.LLMConfig config = new LLMProvider.LLMConfig();
        
        LLMProvider.LLMResponse response = llmProvider.generateChatCompletion(messages, config)
            .get(5, TimeUnit.SECONDS);
        
        assertNotNull(response);
        assertNotNull(response.getContent());
        assertTrue(response.getContent().contains("Hello!"));
        assertEquals("stop", response.getFinishReason());
    }
    
    @Test
    @Timeout(5)
    void testGenerateCompletionWithLongPrompt() throws Exception {
        // Create a long prompt to test truncation
        StringBuilder sb = new StringBuilder();
        String basePrompt = "This is a very long prompt that should be truncated in the mock response. ";
        for (int i = 0; i < 10; i++) {
            sb.append(basePrompt);
        }
        String longPrompt = sb.toString();
        
        LLMProvider.LLMConfig config = new LLMProvider.LLMConfig();
        LLMProvider.LLMRequest request = new LLMProvider.LLMRequest(longPrompt, config);
        
        LLMProvider.LLMResponse response = llmProvider.generateCompletion(request).get(5, TimeUnit.SECONDS);
        
        assertNotNull(response);
        assertTrue(response.getContent().contains("..."));
    }
    
    @Test
    @Timeout(5)
    void testGenerateChatCompletionNoUserMessage() throws Exception {
        List<LLMProvider.ChatMessage> messages = Arrays.asList(
            new LLMProvider.ChatMessage(LLMProvider.ChatMessage.Role.SYSTEM, 
                "You are a helpful assistant.")
        );
        
        LLMProvider.LLMConfig config = new LLMProvider.LLMConfig();
        
        LLMProvider.LLMResponse response = llmProvider.generateChatCompletion(messages, config)
            .get(5, TimeUnit.SECONDS);
        
        assertNotNull(response);
        assertTrue(response.getContent().contains("No user message"));
    }
}