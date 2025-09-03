package com.mem0.unit;

import com.mem0.core.*;
import com.mem0.util.TestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mem0核心组件简化集成测试 - Simplified integration tests for Mem0 core components
 * 
 * 使用TestConfiguration统一管理Provider，验证核心组件基本功能
 * Uses TestConfiguration to manage Providers uniformly and verify core component basic functionality
 */
public class Mem0CoreSimpleTest {

    private MemoryClassifier classifier;
    private MemoryConflictDetector conflictDetector;
    private MemoryMergeStrategy mergeStrategy;
    private MemoryImportanceScorer importanceScorer;

    @BeforeEach
    void setUp() {
        // 使用TestConfiguration创建所有核心组件
        classifier = TestConfiguration.createMemoryClassifier();
        conflictDetector = TestConfiguration.createConflictDetector();
        mergeStrategy = TestConfiguration.createMergeStrategy();
        importanceScorer = TestConfiguration.createImportanceScorer();
    }

    @Test
    void testMemoryClassifierAvailability() {
        if (TestConfiguration.isLLMProviderAvailable()) {
            assertNotNull(classifier);
            System.out.println("MemoryClassifier created successfully with real LLM Provider");
        } else {
            assertNull(classifier);
            System.out.println("MemoryClassifier not available - LLM Provider not configured");
        }
    }

    @Test
    void testConflictDetectorAvailability() {
        if (TestConfiguration.areAllProvidersAvailable()) {
            assertNotNull(conflictDetector);
            System.out.println("MemoryConflictDetector created successfully with real Providers");
        } else {
            assertNull(conflictDetector);
            System.out.println("MemoryConflictDetector not available - Providers not fully configured");
        }
    }

    @Test
    void testMergeStrategyAvailability() {
        if (TestConfiguration.isLLMProviderAvailable()) {
            assertNotNull(mergeStrategy);
            System.out.println("MemoryMergeStrategy created successfully with real LLM Provider");
        } else {
            assertNull(mergeStrategy);
            System.out.println("MemoryMergeStrategy not available - LLM Provider not configured");
        }
    }

    @Test
    void testImportanceScorerAvailability() {
        if (TestConfiguration.isLLMProviderAvailable()) {
            assertNotNull(importanceScorer);
            System.out.println("MemoryImportanceScorer created successfully with real LLM Provider");
        } else {
            assertNull(importanceScorer);
            System.out.println("MemoryImportanceScorer not available - LLM Provider not configured");
        }
    }

    @Test
    void testMemoryClassifierBasicFunctionality() throws Exception {
        if (TestConfiguration.shouldSkipTest("testMemoryClassifierBasicFunctionality", true, false)) {
            return;
        }

        String content = "User likes programming in Java";
        
        MemoryType result = classifier.classifyMemory(content, new java.util.HashMap<>())
            .get(30, TimeUnit.SECONDS);
        
        assertNotNull(result);
        System.out.println("Classified '" + content + "' as: " + result);
    }

    @Test
    void testConflictDetectorBasicFunctionality() throws Exception {
        if (TestConfiguration.shouldSkipTest("testConflictDetectorBasicFunctionality", true, true)) {
            return;
        }

        EnhancedMemory memory1 = new EnhancedMemory("mem1", "User prefers coffee", "user1");
        memory1.setType(MemoryType.PREFERENCE);
        
        EnhancedMemory memory2 = new EnhancedMemory("mem2", "User likes tea", "user1");
        memory2.setType(MemoryType.PREFERENCE);
        
        java.util.List<MemoryConflictDetector.MemoryConflict> conflicts = 
            conflictDetector.detectConflicts(memory2, java.util.Arrays.asList(memory1))
                .get(30, TimeUnit.SECONDS);
        
        assertNotNull(conflicts);
        System.out.println("Detected " + conflicts.size() + " conflicts between preference memories");
    }

    @Test
    void testImportanceScorerBasicFunctionality() throws Exception {
        if (TestConfiguration.shouldSkipTest("testImportanceScorerBasicFunctionality", true, false)) {
            return;
        }

        EnhancedMemory memory = new EnhancedMemory("mem1", "This is very important information", "user1");
        memory.setType(MemoryType.FACTUAL);
        
        MemoryImportanceScorer.ImportanceScore result = importanceScorer.scoreMemoryImportance(memory, new java.util.HashMap<>()).get(30, TimeUnit.SECONDS);
        double score = result.getTotalScore();
        
        System.out.println("Importance score for memory: " + score);
        assertTrue(score >= 0.0, "Score should be non-negative, got: " + score);
        System.out.println("Score validation passed: " + score);
    }

    @Test
    void testAllComponentsIntegration() {
        boolean llmAvailable = TestConfiguration.isLLMProviderAvailable();
        boolean embeddingAvailable = TestConfiguration.isEmbeddingProviderAvailable();
        boolean allAvailable = TestConfiguration.areAllProvidersAvailable();

        System.out.println("Provider availability summary:");
        System.out.println("  LLM Provider: " + llmAvailable);
        System.out.println("  Embedding Provider: " + embeddingAvailable);
        System.out.println("  All Providers: " + allAvailable);

        // 验证组件创建逻辑
        assertEquals(llmAvailable, classifier != null);
        assertEquals(allAvailable, conflictDetector != null);
        assertEquals(llmAvailable, mergeStrategy != null);
        assertEquals(llmAvailable, importanceScorer != null);
    }
}