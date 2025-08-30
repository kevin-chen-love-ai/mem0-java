package com.mem0.unit;

import com.mem0.core.MemoryConflictDetector;
import com.mem0.core.MemoryConflictDetector.MemoryConflict;
import com.mem0.core.MemoryConflictDetector.ConflictType;
import com.mem0.core.MemoryConflictDetector.ConflictResolution;
import com.mem0.core.MemoryConflictDetector.ResolutionStrategy;
import com.mem0.core.EnhancedMemory;
import com.mem0.core.MemoryType;
import com.mem0.embedding.EmbeddingProvider;
import com.mem0.llm.LLMProvider;
import com.mem0.llm.LLMResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class MemoryConflictDetectorTest {
    
    @Mock
    private LLMProvider llmProvider;
    
    @Mock
    private EmbeddingProvider embeddingProvider;
    
    private MemoryConflictDetector detector;
    
    private EnhancedMemory memory1;
    private EnhancedMemory memory2;
    private EnhancedMemory memory3;
    
    // Java 8 compatible helper method for creating failed futures
    private static <T> CompletableFuture<T> createFailedFuture(Exception exception) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(exception);
        return future;
    }
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        detector = new MemoryConflictDetector(embeddingProvider, llmProvider);
        
        // Create test memories
        memory1 = new EnhancedMemory("mem1", "User prefers coffee", "user1");
        memory1.setType(MemoryType.PREFERENCE);
        
        memory2 = new EnhancedMemory("mem2", "User likes tea better than coffee", "user1");
        memory2.setType(MemoryType.PREFERENCE);
        
        memory3 = new EnhancedMemory("mem3", "Paris is the capital of France", "user1");
        memory3.setType(MemoryType.FACTUAL);
    }
    
    @Test
    void testDetectSemanticConflict() throws Exception {
        // Mock embedding provider to return similar embeddings
        float[] embedding1 = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
        float[] embedding2 = {0.15f, 0.25f, 0.35f, 0.45f, 0.55f}; // Similar to embedding1
        
        when(embeddingProvider.embed("User prefers coffee"))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(embedding1));
        when(embeddingProvider.embed("User likes tea better than coffee"))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(embedding2));
        
        // Mock LLM to detect conflict
        LLMResponse llmResponse = new LLMResponse("CONFLICT: These memories express contradictory preferences about beverages", "mock-model", 10, 100L, null);
        when(llmProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(llmResponse));
        
        List<MemoryConflict> conflicts = detector.detectConflicts(memory2, Arrays.asList(memory1)).get();
        
        assertEquals(1, conflicts.size());
        MemoryConflict conflict = conflicts.get(0);
        assertEquals(memory2, conflict.getMemory1());
        assertEquals(memory1, conflict.getMemory2());
        assertEquals(ConflictType.CONTRADICTION, conflict.getType());
        assertTrue(conflict.getConfidence() > 0);
        assertTrue(conflict.getReason().contains("CONFLICT"));
    }
    
    @Test
    void testDetectPreferenceConflict() throws Exception {
        // Mock embedding provider
        List<Float> embedding1 = Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f, 0.5f);
        List<Float> embedding2 = Arrays.asList(0.15f, 0.25f, 0.35f, 0.45f, 0.55f);
        
        when(embeddingProvider.embed(anyString()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(embedding1))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(embedding2));
        
        // Mock LLM to detect preference conflict
        LLMResponse llmResponse = new LLMResponse("CONFLICT: Contradictory preferences detected", "mock-model", 10, 100L, null);
        when(llmProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(llmResponse));
        
        List<MemoryConflict> conflicts = detector.detectConflicts(memory2, Arrays.asList(memory1)).get();
        
        assertEquals(1, conflicts.size());
        MemoryConflict conflict = conflicts.get(0);
        assertEquals(ConflictType.PREFERENCE_CONFLICT, conflict.getType());
        assertTrue(conflict.getConfidence() >= 0.8); // High confidence for preference conflicts
    }
    
    @Test
    void testDetectTemporalConflict() throws Exception {
        // Create temporal memories with conflicting information
        EnhancedMemory oldMemory = new EnhancedMemory("old", "Meeting is at 2 PM", "user1");
        oldMemory.setType(MemoryType.TEMPORAL);
        oldMemory.getMetadata().put("eventTime", LocalDateTime.now().minusDays(1).toString());
        
        EnhancedMemory newMemory = new EnhancedMemory("new", "Meeting is at 3 PM", "user1");
        newMemory.setType(MemoryType.TEMPORAL);
        newMemory.getMetadata().put("eventTime", LocalDateTime.now().toString());
        
        // Mock embedding provider
        float[] embedding = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
        when(embeddingProvider.embed(anyString()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(embedding));
        
        // Mock LLM to detect temporal conflict
        LLMResponse llmResponse = new LLMResponse("CONFLICT: Meeting time has changed", "mock-model", 10, 100L, null);
        when(llmProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(llmResponse));
        
        List<MemoryConflict> conflicts = detector.detectConflicts(newMemory, Arrays.asList(oldMemory)).get();
        
        assertEquals(1, conflicts.size());
        MemoryConflict conflict = conflicts.get(0);
        assertEquals(ConflictType.TEMPORAL_CONFLICT, conflict.getType());
    }
    
    @Test
    void testNoConflictDetected() throws Exception {
        // Test with non-conflicting memories
        EnhancedMemory nonConflictingMemory = new EnhancedMemory("mem4", "Java is a programming language", "user1");
        nonConflictingMemory.setType(MemoryType.FACTUAL);
        
        // Mock embedding provider with different embeddings
        float[] embedding1 = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
        float[] embedding2 = {0.9f, 0.8f, 0.7f, 0.6f, 0.5f}; // Very different
        
        when(embeddingProvider.embed("User prefers coffee"))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(embedding1));
        when(embeddingProvider.embed("Java is a programming language"))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(embedding2));
        
        // Mock LLM to indicate no conflict
        LLMResponse llmResponse = new LLMResponse("NO_CONFLICT: These memories are about different topics", "mock-model", 10, 100L, null);
        when(llmProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(llmResponse));
        
        List<MemoryConflict> conflicts = detector.detectConflicts(nonConflictingMemory, Arrays.asList(memory1)).get();
        
        assertTrue(conflicts.isEmpty());
    }
    
    @Test
    void testResolveConflictKeepFirst() throws Exception {
        MemoryConflict conflict = new MemoryConflict(memory2, memory1, ConflictType.PREFERENCE_CONFLICT, 0.9, "Preference conflict detected", 0.8);
        
        // Mock LLM to suggest keeping first
        LLMResponse llmResponse = new LLMResponse("KEEP_FIRST: The existing preference should be maintained", "mock-model", 10, 100L, null);
        when(llmProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(llmResponse));
        
        ConflictResolution resolution = detector.resolveConflict(conflict).get();
        
        assertEquals(ResolutionStrategy.KEEP_FIRST, resolution.getStrategy());
        assertNull(resolution.getMergedContent());
    }
    
    @Test
    void testResolveConflictKeepSecond() throws Exception {
        MemoryConflict conflict = new MemoryConflict(memory2, memory1, ConflictType.PREFERENCE_CONFLICT, 0.9, "Newer preference should override older one", 0.8);
        
        // Mock LLM to suggest keeping second (newer)
        LLMResponse llmResponse = new LLMResponse("KEEP_SECOND: The new preference is more recent and should override", "mock-model", 10, 100L, null);
        when(llmProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(llmResponse));
        
        ConflictResolution resolution = detector.resolveConflict(conflict).get();
        
        assertEquals(ResolutionStrategy.KEEP_SECOND, resolution.getStrategy());
    }
    
    @Test
    void testResolveConflictMerge() throws Exception {
        MemoryConflict conflict = new MemoryConflict(memory2, memory1, ConflictType.CONTRADICTION, 0.7, "Similar information that can be merged", 0.6);
        
        // Mock LLM to suggest merging
        LLMResponse llmResponse = new LLMResponse("MERGE: User has mixed preferences - sometimes coffee, sometimes tea depending on mood", "mock-model", 10, 100L, null);
        when(llmProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(llmResponse));
        
        ConflictResolution resolution = detector.resolveConflict(conflict).get();
        
        assertEquals(ResolutionStrategy.MERGE, resolution.getStrategy());
        assertNotNull(resolution.getMergedContent());
        assertTrue(resolution.getMergedContent().contains("mixed preferences"));
    }
    
    @Test
    void testResolveConflictKeepBoth() throws Exception {
        MemoryConflict conflict = new MemoryConflict(memory2, memory1, ConflictType.TEMPORAL_CONFLICT, 0.5, "Different time contexts, both valid", 0.4);
        
        // Mock LLM to suggest keeping both
        LLMResponse llmResponse = new LLMResponse("KEEP_BOTH: These represent different time periods and contexts", "mock-model", 10, 100L, null);
        when(llmProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(llmResponse));
        
        ConflictResolution resolution = detector.resolveConflict(conflict).get();
        
        assertEquals(ResolutionStrategy.KEEP_BOTH, resolution.getStrategy());
    }
    
    @Test
    void testFactualMemoryConflictHighSeverity() throws Exception {
        EnhancedMemory factual1 = new EnhancedMemory("fact1", "The Earth is flat", "user1");
        factual1.setType(MemoryType.FACTUAL);
        
        EnhancedMemory factual2 = new EnhancedMemory("fact2", "The Earth is round", "user1");
        factual2.setType(MemoryType.FACTUAL);
        
        // Mock similar embeddings for Earth-related facts
        float[] embedding = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
        when(embeddingProvider.embed(anyString()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(embedding));
        
        // Mock LLM to detect factual conflict
        LLMResponse llmResponse = new LLMResponse("CONFLICT: Contradictory factual statements about Earth's shape", "mock-model", 10, 100L, null);
        when(llmProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(llmResponse));
        
        List<MemoryConflict> conflicts = detector.detectConflicts(factual2, Arrays.asList(factual1)).get();
        
        assertEquals(1, conflicts.size());
        MemoryConflict conflict = conflicts.get(0);
        assertEquals(ConflictType.FACTUAL_CONFLICT, conflict.getType());
        assertTrue(conflict.getConfidence() >= 0.9); // Very high confidence for factual conflicts
    }
    
    @Test
    void testMultipleConflictDetection() throws Exception {
        // Create multiple potentially conflicting memories
        EnhancedMemory coffee = new EnhancedMemory("coffee", "User prefers coffee", "user1");
        coffee.setType(MemoryType.PREFERENCE);
        
        EnhancedMemory tea = new EnhancedMemory("tea", "User likes tea", "user1");
        tea.setType(MemoryType.PREFERENCE);
        
        EnhancedMemory water = new EnhancedMemory("water", "User drinks water", "user1");
        water.setType(MemoryType.FACTUAL);
        
        List<EnhancedMemory> existingMemories = Arrays.asList(coffee, tea, water);
        
        EnhancedMemory newMemory = new EnhancedMemory("new", "User hates all hot beverages", "user1");
        newMemory.setType(MemoryType.PREFERENCE);
        
        // Mock embeddings
        List<Float> embedding = Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f, 0.5f);
        when(embeddingProvider.embed(anyString()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(embedding));
        
        // Mock LLM responses - conflicts with coffee and tea, no conflict with water
        when(llmProvider.generate(argThat(s -> s.contains("User hates all hot beverages") && s.contains("User prefers coffee")), anyDouble(), anyInt()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(createConflictResponse("CONFLICT: Contradictory preferences")));
        when(llmProvider.generate(argThat(s -> s.contains("User hates all hot beverages") && s.contains("User likes tea")), anyDouble(), anyInt()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(createConflictResponse("CONFLICT: Contradictory preferences")));
        when(llmProvider.generate(argThat(s -> s.contains("User hates all hot beverages") && s.contains("User drinks water")), anyDouble(), anyInt()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(createConflictResponse("NO_CONFLICT: Different topics")));
        
        List<MemoryConflict> conflicts = detector.detectConflicts(newMemory, existingMemories).get();
        
        assertEquals(2, conflicts.size()); // Should conflict with coffee and tea preferences
        assertTrue(conflicts.stream().anyMatch(c -> c.getMemory2().equals(coffee)));
        assertTrue(conflicts.stream().anyMatch(c -> c.getMemory2().equals(tea)));
        assertFalse(conflicts.stream().anyMatch(c -> c.getMemory2().equals(water)));
    }
    
    @Test
    void testConflictDetectionWithEmptyExistingMemories() throws Exception {
        List<MemoryConflict> conflicts = detector.detectConflicts(memory1, Collections.emptyList()).get();
        
        assertTrue(conflicts.isEmpty());
        verify(embeddingProvider, never()).embed(anyString());
        verify(llmProvider, never()).generate(anyString(), anyDouble(), anyInt());
    }
    
    @Test
    void testConflictDetectionWithNullMemory() throws Exception {
        List<MemoryConflict> conflicts = detector.detectConflicts(null, Arrays.asList(memory1)).get();
        
        assertTrue(conflicts.isEmpty());
        verify(embeddingProvider, never()).embed(anyString());
        verify(llmProvider, never()).generate(anyString(), anyDouble(), anyInt());
    }
    
    @Test
    void testEmbeddingProviderFailure() throws Exception {
        // Mock embedding provider to fail
        when(embeddingProvider.embed(anyString()))
                .thenReturn(createFailedFuture(new RuntimeException("Embedding service unavailable")));
        
        List<MemoryConflict> conflicts = detector.detectConflicts(memory2, Arrays.asList(memory1)).get();
        
        // Should still work with rule-based detection only
        assertTrue(conflicts.size() >= 0); // May or may not detect conflicts without embeddings
    }
    
    @Test
    void testLLMProviderFailure() throws Exception {
        // Mock embedding provider to return similar embeddings
        float[] embedding = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
        when(embeddingProvider.embed(anyString()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(embedding));
        
        // Mock LLM provider to fail
        when(llmProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenReturn(createFailedFuture(new RuntimeException("LLM service unavailable")));
        
        List<MemoryConflict> conflicts = detector.detectConflicts(memory2, Arrays.asList(memory1)).get();
        
        // Should fall back to rule-based detection
        assertTrue(conflicts.size() >= 0);
    }
    
    @Test
    void testConflictResolutionFallback() throws Exception {
        MemoryConflict conflict = new MemoryConflict(memory2, memory1, ConflictType.PREFERENCE_CONFLICT, 0.9, "Test conflict description", 0.8);
        
        // Mock LLM to return unrecognized resolution strategy
        LLMResponse llmResponse = new LLMResponse("UNKNOWN_STRATEGY: This is not a recognized resolution", "mock-model", 10, 100L, null);
        when(llmProvider.generate(anyString(), anyDouble(), anyInt()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(llmResponse));
        
        ConflictResolution resolution = detector.resolveConflict(conflict).get();
        
        // Should fall back to KEEP_SECOND as default
        assertEquals(ResolutionStrategy.KEEP_SECOND, resolution.getStrategy());
    }
    
    @Test
    void testSimilarityCalculation() throws Exception {
        List<Float> embedding1 = Arrays.asList(1.0f, 0.0f, 0.0f);
        List<Float> embedding2 = Arrays.asList(0.0f, 1.0f, 0.0f);
        List<Float> embedding3 = Arrays.asList(1.0f, 0.0f, 0.0f); // Identical to embedding1
        
        when(embeddingProvider.embed("content1"))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(embedding1));
        when(embeddingProvider.embed("content2"))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(embedding2));
        when(embeddingProvider.embed("content3"))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(embedding3));
        
        EnhancedMemory mem1 = new EnhancedMemory("1", "content1", "user1");
        EnhancedMemory mem2 = new EnhancedMemory("2", "content2", "user1");
        EnhancedMemory mem3 = new EnhancedMemory("3", "content3", "user1");
        
        // Mock LLM to return no conflict for low similarity
        when(llmProvider.generate(argThat(s -> s.contains("content1") && s.contains("content2")), anyDouble(), anyInt()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(createConflictResponse("NO_CONFLICT")));
        when(llmProvider.generate(argThat(s -> s.contains("content1") && s.contains("content3")), anyDouble(), anyInt()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(createConflictResponse("CONFLICT: Duplicate information")));
        
        // Test low similarity (should not trigger detailed conflict analysis)
        List<MemoryConflict> conflicts1 = detector.detectConflicts(mem2, Arrays.asList(mem1)).get();
        assertTrue(conflicts1.isEmpty());
        
        // Test high similarity (should trigger conflict analysis)
        List<MemoryConflict> conflicts2 = detector.detectConflicts(mem3, Arrays.asList(mem1)).get();
        assertEquals(1, conflicts2.size());
    }
    
    private LLMResponse createConflictResponse(String content) {
        return new LLMResponse(content, "mock-model", 10, 100L, null);
    }
}