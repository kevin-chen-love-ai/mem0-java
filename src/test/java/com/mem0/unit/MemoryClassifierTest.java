package com.mem0.core;

import com.mem0.llm.LLMProvider;
import com.mem0.llm.LLMResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class MemoryClassifierTest {
    
    @Mock
    private LLMProvider llmProvider;
    
    private MemoryClassifier classifier;
    
    // Java 8 compatible helper method for creating failed futures
    private static <T> CompletableFuture<T> createFailedFuture(Exception exception) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(exception);
        return future;
    }
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        classifier = new MemoryClassifier(llmProvider);
    }
    
    @Test
    void testClassifyFactualMemory() throws Exception {
        String content = "The capital of France is Paris";
        
        // Mock LLM response for factual memory
        LLMResponse mockResponse = new LLMResponse("FACTUAL", "mock-model", 10, 100L, null);
        when(llmProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(mockResponse));
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get();
        
        assertEquals(MemoryType.FACTUAL, result);
        verify(llmProvider, times(1)).generate(anyString(), anyDouble(), anyInt());
    }
    
    @Test
    void testClassifyEpisodicMemory() throws Exception {
        String content = "I went to the store yesterday and bought groceries";
        
        // Mock LLM response for episodic memory
        LLMResponse mockResponse = new LLMResponse("EPISODIC", "mock-model", 10, 100L, null);
        when(llmProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(mockResponse));
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get();
        
        assertEquals(MemoryType.EPISODIC, result);
        verify(llmProvider, times(1)).generate(anyString(), anyDouble(), anyInt());
    }
    
    @Test
    void testClassifyProceduralMemory() throws Exception {
        String content = "To compile Java code, run javac followed by the filename";
        
        // Mock LLM response for procedural memory
        LLMResponse mockResponse = new LLMResponse("PROCEDURAL", "mock-model", 10, 100L, null);
        when(llmProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(mockResponse));
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get();
        
        assertEquals(MemoryType.PROCEDURAL, result);
        verify(llmProvider, times(1)).generate(anyString(), anyDouble(), anyInt());
    }
    
    @Test
    void testClassifyPreferenceMemory() throws Exception {
        String content = "I prefer tea over coffee in the morning";
        
        // Mock LLM response for preference memory
        LLMResponse mockResponse = new LLMResponse("PREFERENCE", "mock-model", 10, 100L, null);
        when(llmProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(mockResponse));
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get();
        
        assertEquals(MemoryType.PREFERENCE, result);
        verify(llmProvider, times(1)).generate(anyString(), anyDouble(), anyInt());
    }
    
    @Test
    void testClassifySemanticMemory() throws Exception {
        String content = "Machine learning is a subset of artificial intelligence";
        
        // Mock LLM response for semantic memory
        LLMResponse mockResponse = new LLMResponse("SEMANTIC", "mock-model", 10, 100L, null);
        when(llmProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(mockResponse));
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get();
        
        assertEquals(MemoryType.SEMANTIC, result);
        verify(llmProvider, times(1)).generate(anyString(), anyDouble(), anyInt());
    }
    
    @Test
    void testClassifyContextualMemory() throws Exception {
        String content = "In the meeting room, we always keep the temperature at 22°C";
        
        // Mock LLM response for contextual memory
        LLMResponse mockResponse = new LLMResponse("CONTEXTUAL", "mock-model", 10, 100L, null);
        when(llmProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(mockResponse));
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get();
        
        assertEquals(MemoryType.CONTEXTUAL, result);
        verify(llmProvider, times(1)).generate(anyString(), anyDouble(), anyInt());
    }
    
    @Test
    void testClassifyRelationshipMemory() throws Exception {
        String content = "John is the manager of the development team";
        
        // Mock LLM response for relationship memory
        LLMResponse mockResponse = new LLMResponse("RELATIONSHIP", "mock-model", 10, 100L, null);
        when(llmProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(mockResponse));
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get();
        
        assertEquals(MemoryType.RELATIONSHIP, result);
        verify(llmProvider, times(1)).generate(anyString(), anyDouble(), anyInt());
    }
    
    @Test
    void testClassifyTemporalMemory() throws Exception {
        String content = "Every Monday at 9 AM, we have a team standup meeting";
        
        // Mock LLM response for temporal memory
        LLMResponse mockResponse = new LLMResponse("TEMPORAL", "mock-model", 10, 100L, null);
        when(llmProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(mockResponse));
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get();
        
        assertEquals(MemoryType.TEMPORAL, result);
        verify(llmProvider, times(1)).generate(anyString(), anyDouble(), anyInt());
    }
    
    @Test
    void testFallbackClassification() throws Exception {
        String content = "Some ambiguous content that doesn't fit clear categories";
        
        // Mock LLM response with unexpected format
        LLMResponse mockResponse = new LLMResponse("UNKNOWN_TYPE", "mock-model", 10, 100L, null);
        when(llmProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(mockResponse));
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get();
        
        // Should fallback to SEMANTIC as default
        assertEquals(MemoryType.SEMANTIC, result);
        verify(llmProvider, times(1)).generate(anyString(), anyDouble(), anyInt());
    }
    
    @Test
    void testRuleBasedClassificationFallback() throws Exception {
        String content = "How to configure Spring Boot application.yml settings";
        
        // Mock LLM provider throwing exception
        when(llmProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenReturn(createFailedFuture(new RuntimeException("LLM service unavailable")));
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get();
        
        // Should use rule-based fallback - "how to" indicates procedural
        assertEquals(MemoryType.PROCEDURAL, result);
        verify(llmProvider, times(1)).generate(anyString(), anyDouble(), anyInt());
    }
    
    @Test
    void testRuleBasedFactualClassification() throws Exception {
        String content = "The Java programming language was created by Sun Microsystems";
        
        // Mock LLM provider throwing exception
        when(llmProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenReturn(createFailedFuture(new RuntimeException("LLM service unavailable")));
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get();
        
        // Should use rule-based fallback - factual statement pattern
        assertEquals(MemoryType.FACTUAL, result);
        verify(llmProvider, times(1)).generate(anyString(), anyDouble(), anyInt());
    }
    
    @Test
    void testRuleBasedPreferenceClassification() throws Exception {
        String content = "I like using IntelliJ IDEA more than Eclipse";
        
        // Mock LLM provider throwing exception
        when(llmProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenReturn(createFailedFuture(new RuntimeException("LLM service unavailable")));
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get();
        
        // Should use rule-based fallback - preference indicators
        assertEquals(MemoryType.PREFERENCE, result);
        verify(llmProvider, times(1)).generate(anyString(), anyDouble(), anyInt());
    }
    
    @Test
    void testRuleBasedEpisodicClassification() throws Exception {
        String content = "Last week I attended a conference about microservices";
        
        // Mock LLM provider throwing exception
        when(llmProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenReturn(createFailedFuture(new RuntimeException("LLM service unavailable")));
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get();
        
        // Should use rule-based fallback - temporal indicators suggest episodic
        assertEquals(MemoryType.EPISODIC, result);
        verify(llmProvider, times(1)).generate(anyString(), anyDouble(), anyInt());
    }
    
    @Test
    void testEmptyContentHandling() throws Exception {
        String content = "";
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get();
        
        // Empty content should default to SEMANTIC
        assertEquals(MemoryType.SEMANTIC, result);
        verify(llmProvider, never()).generate(anyString(), anyDouble(), anyInt());
    }
    
    @Test
    void testNullContentHandling() throws Exception {
        String content = null;
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get();
        
        // Null content should default to SEMANTIC
        assertEquals(MemoryType.SEMANTIC, result);
        verify(llmProvider, never()).generate(anyString(), anyDouble(), anyInt());
    }
    
    @Test
    void testWhitespaceContentHandling() throws Exception {
        String content = "   \n\t  ";
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get();
        
        // Whitespace-only content should default to SEMANTIC
        assertEquals(MemoryType.SEMANTIC, result);
        verify(llmProvider, never()).generate(anyString(), anyDouble(), anyInt());
    }
    
    @Test
    void testCaseInsensitiveClassification() throws Exception {
        String content = "User prefers dark mode interface";
        
        // Mock LLM response with lowercase
        LLMResponse mockResponse = new LLMResponse("preference", "mock-model", 10, 100L, null);
        when(llmProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(mockResponse));
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get();
        
        assertEquals(MemoryType.PREFERENCE, result);
        verify(llmProvider, times(1)).generate(anyString(), anyDouble(), anyInt());
    }
    
    @Test
    void testPartialResponseMatching() throws Exception {
        String content = "The algorithm complexity is O(n log n)";
        
        // Mock LLM response with extra text
        LLMResponse mockResponse = new LLMResponse("This is clearly FACTUAL information about algorithm complexity", "mock-model", 10, 100L, null);
        when(llmProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(mockResponse));
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get();
        
        assertEquals(MemoryType.FACTUAL, result);
        verify(llmProvider, times(1)).generate(anyString(), anyDouble(), anyInt());
    }
}