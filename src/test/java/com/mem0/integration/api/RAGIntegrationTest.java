package com.mem0.integration.api;

import com.mem0.config.Mem0Config;
import com.mem0.core.MemoryService;
import com.mem0.embedding.impl.MockEmbeddingProvider;
import com.mem0.llm.MockLLMProvider;
import com.mem0.template.ChatRAGPromptTemplate;
import com.mem0.template.DefaultRAGPromptTemplate;
import com.mem0.template.PromptTemplate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RAGIntegrationTest {
    
    private MockEmbeddingProvider embeddingProvider;
    private MockLLMProvider llmProvider;
    private DefaultRAGPromptTemplate promptTemplate;
    private ChatRAGPromptTemplate chatPromptTemplate;
    
    // Java 8 compatible helper methods
    private static Map<String, Object> createMap(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }
    
    private static <T> List<T> createList(T... items) {
        List<T> list = new java.util.ArrayList<>();
        for (T item : items) {
            list.add(item);
        }
        return list;
    }
    
    @BeforeEach
    void setUp() {
        embeddingProvider = new MockEmbeddingProvider();
        llmProvider = new MockLLMProvider();
        promptTemplate = new DefaultRAGPromptTemplate();
        chatPromptTemplate = new ChatRAGPromptTemplate();
    }
    
    @Test
    @Timeout(10)
    void testEmbeddingProviderIntegration() throws Exception {
        // Test that embeddings are consistent and properly normalized
        String text1 = "The user prefers coffee over tea";
        String text2 = "User likes to work late at night";
        
        List<Float> embedding1 = embeddingProvider.embed(text1).get();
        List<Float> embedding2 = embeddingProvider.embed(text2).get();
        
        assertEquals(embeddingProvider.getDimensions(), embedding1.size());
        assertEquals(embeddingProvider.getDimensions(), embedding2.size());
        
        // Test vector similarity (should be different for different content)
        double similarity = cosineSimilarity(embedding1, embedding2);
        assertTrue(similarity < 1.0, "Different texts should have similarity < 1.0");
        
        // Test same text produces same embedding
        List<Float> embedding1Duplicate = embeddingProvider.embed(text1).get();
        assertEquals(embedding1, embedding1Duplicate);
        
        double selfSimilarity = cosineSimilarity(embedding1, embedding1Duplicate);
        assertEquals(1.0, selfSimilarity, 0.001, "Same text should have similarity = 1.0");
    }
    
    @Test
    @Timeout(5)
    void testRAGPromptTemplateIntegration() {
        // Create mock retrieved memories
        List<PromptTemplate.RetrievedMemory> memories = createList(
            new PromptTemplate.RetrievedMemory(
                "User prefers coffee in the morning",
                0.95,
                createMap("category", "preferences", "time", "morning"),
                "preference"
            ),
            new PromptTemplate.RetrievedMemory(
                "User has a meeting with John at 3 PM",
                0.87,
                createMap("category", "schedule", "person", "John"),
                "event"
            ),
            new PromptTemplate.RetrievedMemory(
                "User is working on a Java project called mem0-java",
                0.82,
                createMap("category", "work", "language", "Java"),
                "context"
            )
        );
        
        PromptTemplate.PromptContext context = new PromptTemplate.PromptContext(
            "What are my preferences and schedule for today?"
        );
        context.setRetrievedMemories(memories);
        context.setSystemMessage("You are a helpful personal assistant.");
        
        Map<String, Object> additionalContext = new HashMap<>();
        additionalContext.put("current_time", "2024-01-15 10:30 AM");
        additionalContext.put("timezone", "PST");
        context.setAdditionalContext(additionalContext);
        
        String prompt = promptTemplate.buildPrompt(context);
        
        assertNotNull(prompt);
        assertFalse(prompt.trim().isEmpty());
        
        // Verify that all components are included in the prompt
        assertTrue(prompt.contains("You are a helpful personal assistant"));
        assertTrue(prompt.contains("coffee in the morning"));
        assertTrue(prompt.contains("meeting with John"));
        assertTrue(prompt.contains("Java project"));
        assertTrue(prompt.contains("What are my preferences and schedule"));
        assertTrue(prompt.contains("current_time: 2024-01-15 10:30 AM"));
        assertTrue(prompt.contains("Memory 1"));
        assertTrue(prompt.contains("relevance: 0.950"));
        assertTrue(prompt.contains("Type: preference"));
    }
    
    @Test
    @Timeout(5)
    void testChatRAGPromptTemplateIntegration() {
        PromptTemplate.RetrievedMemory memory = new PromptTemplate.RetrievedMemory(
            "User is a vegetarian and prefers Italian food",
            0.91,
            createMap("dietary", "vegetarian", "cuisine", "Italian"),
            "preference"
        );
        
        PromptTemplate.PromptContext context = new PromptTemplate.PromptContext(
            "What restaurant should I go to for dinner?"
        );
        context.setRetrievedMemories(createList(memory));
        context.setSystemMessage("You are a restaurant recommendation assistant.");
        
        List<com.mem0.llm.LLMProvider.ChatMessage> messages = chatPromptTemplate.buildChatMessages(context);
        
        assertNotNull(messages);
        assertEquals(2, messages.size());
        
        // Check system message
        com.mem0.llm.LLMProvider.ChatMessage systemMessage = messages.get(0);
        assertEquals(com.mem0.llm.LLMProvider.ChatMessage.Role.SYSTEM, systemMessage.getRole());
        assertTrue(systemMessage.getContent().contains("restaurant recommendation assistant"));
        assertTrue(systemMessage.getContent().contains("vegetarian"));
        assertTrue(systemMessage.getContent().contains("Italian food"));
        assertTrue(systemMessage.getContent().contains("[Memory 1]"));
        
        // Check user message
        com.mem0.llm.LLMProvider.ChatMessage userMessage = messages.get(1);
        assertEquals(com.mem0.llm.LLMProvider.ChatMessage.Role.USER, userMessage.getRole());
        assertEquals("What restaurant should I go to for dinner?", userMessage.getContent());
    }
    
    @Test
    @Timeout(10)
    void testEndToEndRAGWorkflow() throws Exception {
        // Simulate a complete RAG workflow without external dependencies
        
        // Step 1: Embed query
        String query = "What are my work preferences?";
        List<Float> queryEmbedding = embeddingProvider.embed(query).get();
        assertNotNull(queryEmbedding);
        
        // Step 2: Simulate retrieved memories (normally from vector search)
        List<PromptTemplate.RetrievedMemory> retrievedMemories = createList(
            new PromptTemplate.RetrievedMemory(
                "User prefers to work remotely from home office",
                0.89,
                createMap("category", "work", "location", "remote"),
                "preference"
            ),
            new PromptTemplate.RetrievedMemory(
                "User likes to take breaks every 2 hours",
                0.76,
                createMap("category", "work", "frequency", "2 hours"),
                "habit"
            )
        );
        
        // Step 3: Build prompt context
        PromptTemplate.PromptContext context = new PromptTemplate.PromptContext(query);
        context.setRetrievedMemories(retrievedMemories);
        context.setSystemMessage("You are an AI assistant helping with work preferences.");
        
        // Step 4: Generate chat messages
        List<com.mem0.llm.LLMProvider.ChatMessage> messages = chatPromptTemplate.buildChatMessages(context);
        
        // Step 5: Generate response using LLM
        com.mem0.llm.LLMProvider.LLMConfig llmConfig = new com.mem0.llm.LLMProvider.LLMConfig("mock-model");
        llmConfig.setTemperature(0.7);
        llmConfig.setMaxTokens(500);
        
        com.mem0.llm.LLMProvider.LLMResponse response = llmProvider.generateChatCompletion(messages, llmConfig).get();
        
        // Step 6: Verify response
        assertNotNull(response);
        assertNotNull(response.getContent());
        assertFalse(response.getContent().trim().isEmpty());
        assertTrue(response.getContent().contains("Mock response"));
        assertTrue(response.getTokensUsed() > 0);
        assertEquals("stop", response.getFinishReason());
    }
    
    @Test
    void testPromptTemplateMetadata() {
        assertEquals("DefaultRAG", promptTemplate.getName());
        assertEquals("ChatRAG", chatPromptTemplate.getName());
        
        assertNotNull(promptTemplate.getDescription());
        assertNotNull(chatPromptTemplate.getDescription());
        
        assertTrue(promptTemplate.getDescription().contains("Retrieval-Augmented Generation"));
        assertTrue(chatPromptTemplate.getDescription().contains("Chat-based"));
    }
    
    private double cosineSimilarity(List<Float> vectorA, List<Float> vectorB) {
        if (vectorA.size() != vectorB.size()) {
            throw new IllegalArgumentException("Vectors must have the same dimension");
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < vectorA.size(); i++) {
            dotProduct += vectorA.get(i) * vectorB.get(i);
            normA += vectorA.get(i) * vectorA.get(i);
            normB += vectorB.get(i) * vectorB.get(i);
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}