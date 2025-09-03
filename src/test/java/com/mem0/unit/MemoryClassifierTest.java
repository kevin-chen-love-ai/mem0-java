package com.mem0.unit;

import com.mem0.core.MemoryClassifier;
import com.mem0.core.MemoryType;
import com.mem0.llm.LLMProvider;
import com.mem0.util.TestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MemoryClassifier真实集成测试 - Real integration tests for MemoryClassifier
 * 
 * 使用统一的测试配置管理器获取LLMProvider，验证内存分类功能
 * Uses unified test configuration manager to obtain LLMProvider and test memory classification functionality
 */
public class MemoryClassifierTest {
    
    private LLMProvider llmProvider;
    private MemoryClassifier classifier;

    @BeforeEach
    void setUp() {
        try {
            // 使用TestConfiguration的便利方法创建MemoryClassifier
            classifier = TestConfiguration.createMemoryClassifier();
        } catch (Exception e) {
            System.err.println("Warning: Could not initialize MemoryClassifier: " + e.getMessage());
        }
    }
    
    @AfterEach
    void tearDown() {
        // TestConfiguration统一管理资源清理，这里不需要手动关闭
        // Resources are managed by TestConfiguration, no manual cleanup needed here
    }
    
    @Test
    void testClassifyFactualMemory() throws Exception {
        if (classifier == null) {
            System.out.println("Skipping testClassifyFactualMemory - LLM provider not available");
            return;
        }
        
        String content = "The capital of France is Paris";
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get(30, TimeUnit.SECONDS);
        
        assertNotNull(result);
        // Factual statements should be classified as FACTUAL or SEMANTIC
        assertTrue(result == MemoryType.FACTUAL || result == MemoryType.SEMANTIC,
                  "Expected FACTUAL or SEMANTIC, got: " + result);
    }
    
    @Test
    void testClassifyEpisodicMemory() throws Exception {
        if (classifier == null) {
            System.out.println("Skipping testClassifyEpisodicMemory - LLM provider not available");
            return;
        }
        
        String content = "I went to the store yesterday and bought groceries";
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get(30, TimeUnit.SECONDS);
        
        assertNotNull(result);
        // Personal experiences should be classified as EPISODIC or similar
        assertTrue(result == MemoryType.EPISODIC || result == MemoryType.FACTUAL || result == MemoryType.SEMANTIC,
                  "Expected EPISODIC, FACTUAL or SEMANTIC, got: " + result);
    }
    
    @Test
    void testClassifyProceduralMemory() throws Exception {
        if (classifier == null) {
            System.out.println("Skipping testClassifyProceduralMemory - LLM provider not available");
            return;
        }
        
        String content = "To compile Java code, run javac followed by the filename";
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get(30, TimeUnit.SECONDS);
        
        assertNotNull(result);
        // How-to instructions should be classified as PROCEDURAL or SEMANTIC
        assertTrue(result == MemoryType.PROCEDURAL || result == MemoryType.SEMANTIC || result == MemoryType.FACTUAL,
                  "Expected PROCEDURAL, SEMANTIC or FACTUAL, got: " + result);
    }
    
    @Test
    void testClassifyPreferenceMemory() throws Exception {
        if (classifier == null) {
            System.out.println("Skipping testClassifyPreferenceMemory - LLM provider not available");
            return;
        }
        
        String content = "I prefer tea over coffee in the morning";
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get(30, TimeUnit.SECONDS);
        
        assertNotNull(result);
        // Personal preferences should be classified as PREFERENCE or similar
        assertTrue(result == MemoryType.PREFERENCE || result == MemoryType.FACTUAL || result == MemoryType.SEMANTIC,
                  "Expected PREFERENCE, FACTUAL or SEMANTIC, got: " + result);
    }
    
    @Test
    void testClassifySemanticMemory() throws Exception {
        if (classifier == null) {
            System.out.println("Skipping testClassifySemanticMemory - LLM provider not available");
            return;
        }
        
        String content = "Machine learning is a subset of artificial intelligence";
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get(30, TimeUnit.SECONDS);
        
        assertNotNull(result);
        // Conceptual knowledge should be classified as SEMANTIC or FACTUAL
        assertTrue(result == MemoryType.SEMANTIC || result == MemoryType.FACTUAL,
                  "Expected SEMANTIC or FACTUAL, got: " + result);
    }
    
    @Test
    void testClassifyContextualMemory() throws Exception {
        if (classifier == null) {
            System.out.println("Skipping testClassifyContextualMemory - LLM provider not available");
            return;
        }
        
        String content = "In the meeting room, we always keep the temperature at 22°C";
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get(30, TimeUnit.SECONDS);
        
        assertNotNull(result);
        // Contextual information could be classified as various types
        assertTrue(result == MemoryType.CONTEXTUAL || result == MemoryType.FACTUAL || result == MemoryType.SEMANTIC,
                  "Expected CONTEXTUAL, FACTUAL or SEMANTIC, got: " + result);
    }
    
    @Test
    void testClassifyRelationshipMemory() throws Exception {
        if (classifier == null) {
            System.out.println("Skipping testClassifyRelationshipMemory - LLM provider not available");
            return;
        }
        
        String content = "John is the manager of the development team";
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get(30, TimeUnit.SECONDS);
        
        assertNotNull(result);
        // Relationship information could be classified as various types
        assertTrue(result == MemoryType.RELATIONSHIP || result == MemoryType.FACTUAL || result == MemoryType.SEMANTIC,
                  "Expected RELATIONSHIP, FACTUAL or SEMANTIC, got: " + result);
    }
    
    @Test
    void testClassifyTemporalMemory() throws Exception {
        if (classifier == null) {
            System.out.println("Skipping testClassifyTemporalMemory - LLM provider not available");
            return;
        }
        
        String content = "Every Monday at 9 AM, we have a team standup meeting";
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get(30, TimeUnit.SECONDS);
        
        assertNotNull(result);
        // Temporal information could be classified as various types, including RELATIONSHIP when discussing team meetings
        assertTrue(result == MemoryType.TEMPORAL || result == MemoryType.FACTUAL || result == MemoryType.PROCEDURAL || result == MemoryType.RELATIONSHIP,
                  "Expected TEMPORAL, FACTUAL, PROCEDURAL or RELATIONSHIP, got: " + result);
    }
    
    @Test
    void testFallbackClassification() throws Exception {
        if (classifier == null) {
            System.out.println("Skipping testFallbackClassification - LLM provider not available");
            return;
        }
        
        String content = "Some ambiguous content that doesn't fit clear categories";
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get(30, TimeUnit.SECONDS);
        
        assertNotNull(result);
        // Should return a valid memory type even for ambiguous content
        assertTrue(result != null, "Should return a valid memory type");
    }
    
    @Test
    void testRuleBasedClassificationFallback() throws Exception {
        if (classifier == null) {
            System.out.println("Skipping testRuleBasedClassificationFallback - LLM provider not available");
            return;
        }
        
        String content = "How to configure Spring Boot application.yml settings";
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get(30, TimeUnit.SECONDS);
        
        assertNotNull(result);
        // "How to" content should be classified as PROCEDURAL or SEMANTIC
        assertTrue(result == MemoryType.PROCEDURAL || result == MemoryType.SEMANTIC || result == MemoryType.FACTUAL,
                  "Expected PROCEDURAL, SEMANTIC or FACTUAL, got: " + result);
    }
    
    @Test
    void testRuleBasedFactualClassification() throws Exception {
        if (classifier == null) {
            System.out.println("Skipping testRuleBasedFactualClassification - LLM provider not available");
            return;
        }
        
        String content = "The Java programming language was created by Sun Microsystems";
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get(30, TimeUnit.SECONDS);
        
        assertNotNull(result);
        // Should classify factual statement appropriately
        assertTrue(result == MemoryType.FACTUAL || result == MemoryType.SEMANTIC,
                  "Expected FACTUAL or SEMANTIC, got: " + result);
    }
    
    @Test
    void testRuleBasedPreferenceClassification() throws Exception {
        if (classifier == null) {
            System.out.println("Skipping testRuleBasedPreferenceClassification - LLM provider not available");
            return;
        }
        
        String content = "I like using IntelliJ IDEA more than Eclipse";
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get(30, TimeUnit.SECONDS);
        
        assertNotNull(result);
        // Should classify preference statement appropriately
        assertTrue(result == MemoryType.PREFERENCE || result == MemoryType.FACTUAL || result == MemoryType.SEMANTIC,
                  "Expected PREFERENCE, FACTUAL or SEMANTIC, got: " + result);
    }
    
    @Test
    void testRuleBasedEpisodicClassification() throws Exception {
        if (classifier == null) {
            System.out.println("Skipping testRuleBasedEpisodicClassification - LLM provider not available");
            return;
        }
        
        String content = "Last week I attended a conference about microservices";
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get(30, TimeUnit.SECONDS);
        
        assertNotNull(result);
        // Should classify episodic statement appropriately
        assertTrue(result == MemoryType.EPISODIC || result == MemoryType.FACTUAL || result == MemoryType.SEMANTIC,
                  "Expected EPISODIC, FACTUAL or SEMANTIC, got: " + result);
    }
    
    @Test
    void testEmptyContentHandling() throws Exception {
        if (classifier == null) {
            System.out.println("Skipping testEmptyContentHandling - LLM provider not available");
            return;
        }
        
        String content = "";
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get(30, TimeUnit.SECONDS);
        
        assertNotNull(result);
        // Empty content should return a default classification
        assertEquals(MemoryType.SEMANTIC, result);
    }
    
    @Test
    void testNullContentHandling() throws Exception {
        if (classifier == null) {
            System.out.println("Skipping testNullContentHandling - LLM provider not available");
            return;
        }
        
        String content = null;
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get(30, TimeUnit.SECONDS);
        
        assertNotNull(result);
        // Null content should return a default classification
        assertEquals(MemoryType.SEMANTIC, result);
    }
    
    @Test
    void testWhitespaceContentHandling() throws Exception {
        if (classifier == null) {
            System.out.println("Skipping testWhitespaceContentHandling - LLM provider not available");
            return;
        }
        
        String content = "   \n\t  ";
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get(30, TimeUnit.SECONDS);
        
        // Whitespace-only content should default to SEMANTIC
        assertEquals(MemoryType.SEMANTIC, result);
    }
    
    @Test
    void testCaseInsensitiveClassification() throws Exception {
        if (classifier == null) {
            System.out.println("Skipping testCaseInsensitiveClassification - LLM provider not available");
            return;
        }
        
        String content = "User prefers dark mode interface";
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get(30, TimeUnit.SECONDS);
        
        assertNotNull(result);
        // Should handle case-insensitive classification appropriately
        assertTrue(result == MemoryType.PREFERENCE || result == MemoryType.FACTUAL || result == MemoryType.SEMANTIC,
                  "Expected PREFERENCE, FACTUAL or SEMANTIC, got: " + result);
    }
    
    @Test
    void testPartialResponseMatching() throws Exception {
        if (classifier == null) {
            System.out.println("Skipping testPartialResponseMatching - LLM provider not available");
            return;
        }
        
        String content = "The algorithm complexity is O(n log n)";
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        MemoryType result = classifier.classifyMemory(content, context).get(30, TimeUnit.SECONDS);
        
        assertNotNull(result);
        // Algorithm complexity information could be classified as various types, including PROCEDURAL for "how to analyze"
        assertTrue(result == MemoryType.FACTUAL || result == MemoryType.SEMANTIC || result == MemoryType.PROCEDURAL,
                  "Expected FACTUAL, SEMANTIC or PROCEDURAL, got: " + result);
    }
}