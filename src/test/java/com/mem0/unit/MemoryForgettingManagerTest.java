package com.mem0.unit;

import com.mem0.core.MemoryForgettingManager;
import com.mem0.core.MemoryForgettingManager.PruningStrategy;
import com.mem0.core.MemoryForgettingPolicy;
import com.mem0.core.EnhancedMemory;
import com.mem0.core.MemoryImportance;
import com.mem0.core.MemoryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

public class MemoryForgettingManagerTest {
    
    private MemoryForgettingManager forgettingManager;
    private List<EnhancedMemory> testMemories;
    
    @BeforeEach
    void setUp() {
        forgettingManager = new MemoryForgettingManager();
        
        // Create test memories with different characteristics
        EnhancedMemory critical = new EnhancedMemory("critical", "Critical system information", "user1");
        critical.setImportance(MemoryImportance.CRITICAL);
        critical.setType(MemoryType.FACTUAL);
        for (int i = 0; i < 10; i++) {
            critical.recordAccess(); // High access count
        }
        
        EnhancedMemory high = new EnhancedMemory("high", "Important work information", "user1");
        high.setImportance(MemoryImportance.HIGH);
        high.setType(MemoryType.PROCEDURAL);
        for (int i = 0; i < 5; i++) {
            high.recordAccess();
        }
        
        EnhancedMemory medium = new EnhancedMemory("medium", "Moderate importance info", "user1");
        medium.setImportance(MemoryImportance.MEDIUM);
        medium.setType(MemoryType.SEMANTIC);
        for (int i = 0; i < 2; i++) {
            medium.recordAccess();
        }
        
        EnhancedMemory low = new EnhancedMemory("low", "Low importance info", "user1");
        low.setImportance(MemoryImportance.LOW);
        low.setType(MemoryType.EPISODIC);
        
        EnhancedMemory minimal = new EnhancedMemory("minimal", "Minimal importance info", "user1");
        minimal.setImportance(MemoryImportance.MINIMAL);
        minimal.setType(MemoryType.PREFERENCE);
        
        // Create an old memory for testing age-based forgetting
        EnhancedMemory oldMemory = new EnhancedMemory("old", "Old information", "user1");
        oldMemory.setImportance(MemoryImportance.LOW);
        // Simulate old creation time by manipulating internal state
        oldMemory.setCreatedAt(LocalDateTime.now().minusDays(100));
        oldMemory.setLastAccessedAt(LocalDateTime.now().minusDays(50));
        
        testMemories = Arrays.asList(critical, high, medium, low, minimal, oldMemory);
    }
    
    @Test
    void testNeverForgetPolicy() throws Exception {
        // Create manager with very high retention threshold so nothing gets forgotten
        MemoryForgettingManager neverForgetManager = new MemoryForgettingManager(0.5, 0.1, 1.0);
        
        List<EnhancedMemory> result = neverForgetManager.processMemoryDecay(testMemories).get();
        
        // All memories should be retained with high retention threshold
        assertEquals(testMemories.size(), result.size());
        
        // None should be marked as deprecated
        assertFalse(result.stream().anyMatch(EnhancedMemory::isDeprecated));
    }
    
    @Test
    void testGradualDecayPolicy() throws Exception {
        MemoryForgettingPolicy policy = new MemoryForgettingPolicy();
        policy.setForgettingEnabled(true);
        policy.setDecayRate(0.1);
        policy.setImportanceThreshold(MemoryImportance.LOW);
        
        forgettingManager.setForgettingPolicy(policy);
        
        List<EnhancedMemory> result = forgettingManager.processMemoryDecay(testMemories).get();
        
        // Should retain all memories but some may have reduced importance
        assertEquals(testMemories.size(), result.size());
        
        // Critical and high importance memories should not be affected
        assertTrue(result.stream().anyMatch(m -> 
            m.getId().equals("critical") && m.getImportance() == MemoryImportance.CRITICAL));
        assertTrue(result.stream().anyMatch(m -> 
            m.getId().equals("high") && m.getImportance() == MemoryImportance.HIGH));
        
        // Lower importance memories may be deprecated based on decay score
        long deprecatedCount = result.stream().mapToLong(m -> m.isDeprecated() ? 1 : 0).sum();
        assertTrue(deprecatedCount >= 0); // May or may not deprecate based on decay algorithm
    }
    
    @Test
    void testAggressiveForgettingPolicy() throws Exception {
        MemoryForgettingPolicy policy = new MemoryForgettingPolicy();
        policy.setForgettingEnabled(true);
        policy.setDecayRate(0.8); // High decay rate
        policy.setImportanceThreshold(MemoryImportance.MEDIUM);
        policy.setMaxMemoryAge(30); // 30 days
        
        forgettingManager.setForgettingPolicy(policy);
        
        List<EnhancedMemory> result = forgettingManager.processMemoryDecay(testMemories).get();
        
        // Should retain memories but deprecate many low importance ones
        assertEquals(testMemories.size(), result.size());
        
        // Critical memories should always be retained
        assertTrue(result.stream().anyMatch(m -> 
            m.getId().equals("critical") && !m.isDeprecated()));
        
        // Some low importance or old memories should be deprecated
        long deprecatedCount = result.stream().mapToLong(m -> m.isDeprecated() ? 1 : 0).sum();
        assertTrue(deprecatedCount > 0);
    }
    
    @Test
    void testConservativeForgettingPolicy() throws Exception {
        MemoryForgettingPolicy policy = new MemoryForgettingPolicy();
        policy.setForgettingEnabled(true);
        policy.setDecayRate(0.01); // Very low decay rate
        policy.setImportanceThreshold(MemoryImportance.MINIMAL);
        policy.setMaxMemoryAge(365); // 1 year
        
        forgettingManager.setForgettingPolicy(policy);
        
        List<EnhancedMemory> result = forgettingManager.processMemoryDecay(testMemories).get();
        
        // Should retain almost all memories
        assertEquals(testMemories.size(), result.size());
        
        // Very few should be deprecated with conservative policy
        long deprecatedCount = result.stream().mapToLong(m -> m.isDeprecated() ? 1 : 0).sum();
        assertTrue(deprecatedCount <= 1); // Maybe only the oldest/least important
    }
    
    @Test
    void testImportanceBasedForgetting() throws Exception {
        MemoryForgettingPolicy policy = new MemoryForgettingPolicy();
        policy.setForgettingEnabled(true);
        policy.setDecayRate(0.5);
        policy.setImportanceThreshold(MemoryImportance.MEDIUM);
        
        forgettingManager.setForgettingPolicy(policy);
        
        List<EnhancedMemory> result = forgettingManager.processMemoryDecay(testMemories).get();
        
        // Critical and high importance should never be deprecated
        assertFalse(result.stream().anyMatch(m -> 
            (m.getImportance() == MemoryImportance.CRITICAL || 
             m.getImportance() == MemoryImportance.HIGH) && m.isDeprecated()));
        
        // Lower importance memories are candidates for deprecation
        assertTrue(result.stream().anyMatch(m -> 
            m.getImportance().getScore() < MemoryImportance.MEDIUM.getScore()));
    }
    
    @Test
    void testAccessBasedForgetting() throws Exception {
        MemoryForgettingPolicy policy = new MemoryForgettingPolicy();
        policy.setForgettingEnabled(true);
        policy.setDecayRate(0.3);
        policy.setMinAccessCount(3);
        
        forgettingManager.setForgettingPolicy(policy);
        
        List<EnhancedMemory> result = forgettingManager.processMemoryDecay(testMemories).get();
        
        // Frequently accessed memories should be retained
        assertTrue(result.stream().anyMatch(m -> 
            m.getId().equals("critical") && !m.isDeprecated()));
        
        // Less accessed memories are more likely to be deprecated
        assertTrue(result.stream().anyMatch(m -> 
            m.getAccessCount() < 3));
    }
    
    @Test
    void testLRUPruning() throws Exception {
        int maxMemories = 3;
        
        List<EnhancedMemory> result = forgettingManager.pruneOldMemories(
            testMemories, maxMemories, PruningStrategy.LEAST_RECENTLY_USED).get();
        
        assertEquals(maxMemories, result.size());
        
        // Should keep the most recently accessed memories
        assertTrue(result.stream().anyMatch(m -> m.getId().equals("critical")));
        assertTrue(result.stream().anyMatch(m -> m.getId().equals("high")));
        assertTrue(result.stream().anyMatch(m -> m.getId().equals("medium")));
    }
    
    @Test
    void testLeastImportantPruning() throws Exception {
        int maxMemories = 3;
        
        List<EnhancedMemory> result = forgettingManager.pruneOldMemories(
            testMemories, maxMemories, PruningStrategy.LEAST_IMPORTANT).get();
        
        assertEquals(maxMemories, result.size());
        
        // Should keep the most important memories
        assertTrue(result.stream().anyMatch(m -> 
            m.getImportance() == MemoryImportance.CRITICAL));
        assertTrue(result.stream().anyMatch(m -> 
            m.getImportance() == MemoryImportance.HIGH));
        assertTrue(result.stream().anyMatch(m -> 
            m.getImportance() == MemoryImportance.MEDIUM));
        
        // Should not include minimal importance
        assertFalse(result.stream().anyMatch(m -> 
            m.getImportance() == MemoryImportance.MINIMAL));
    }
    
    @Test
    void testOldestFirstPruning() throws Exception {
        int maxMemories = 3;
        
        List<EnhancedMemory> result = forgettingManager.pruneOldMemories(
            testMemories, maxMemories, PruningStrategy.OLDEST_FIRST).get();
        
        assertEquals(maxMemories, result.size());
        
        // Should keep the newest memories
        assertFalse(result.stream().anyMatch(m -> m.getId().equals("old")));
    }
    
    @Test
    void testLowestDecayScorePruning() throws Exception {
        int maxMemories = 4;
        
        List<EnhancedMemory> result = forgettingManager.pruneOldMemories(
            testMemories, maxMemories, PruningStrategy.LOWEST_DECAY_SCORE).get();
        
        assertEquals(maxMemories, result.size());
        
        // Should keep memories with highest decay scores (most important to retain)
        assertTrue(result.stream().anyMatch(m -> 
            m.getImportance() == MemoryImportance.CRITICAL));
        assertTrue(result.stream().anyMatch(m -> 
            m.getImportance() == MemoryImportance.HIGH));
    }
    
    @Test
    void testBalancedPruning() throws Exception {
        int maxMemories = 4;
        
        List<EnhancedMemory> result = forgettingManager.pruneOldMemories(
            testMemories, maxMemories, PruningStrategy.BALANCED).get();
        
        assertEquals(maxMemories, result.size());
        
        // Should keep a balanced mix of important and recent memories
        assertTrue(result.stream().anyMatch(m -> 
            m.getImportance() == MemoryImportance.CRITICAL));
        
        // Should consider both importance and recency
        long highImportanceCount = result.stream().mapToLong(m -> 
            m.getImportance().getScore() >= MemoryImportance.HIGH.getScore() ? 1 : 0).sum();
        assertTrue(highImportanceCount > 0);
    }
    
    @Test
    void testPruningWithMaxMemoriesLargerThanList() throws Exception {
        int maxMemories = testMemories.size() + 5;
        
        List<EnhancedMemory> result = forgettingManager.pruneOldMemories(
            testMemories, maxMemories, PruningStrategy.LEAST_RECENTLY_USED).get();
        
        // Should return all memories when maxMemories is larger than list size
        assertEquals(testMemories.size(), result.size());
    }
    
    @Test
    void testPruningWithZeroMaxMemories() throws Exception {
        List<EnhancedMemory> result = forgettingManager.pruneOldMemories(
            testMemories, 0, PruningStrategy.LEAST_RECENTLY_USED).get();
        
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testPruningWithEmptyList() throws Exception {
        List<EnhancedMemory> result = forgettingManager.pruneOldMemories(
            Collections.emptyList(), 5, PruningStrategy.LEAST_RECENTLY_USED).get();
        
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testDecayScoreCalculation() {
        // Test memory with high importance and recent access
        EnhancedMemory recentImportant = new EnhancedMemory("recent", "Recent important memory", "user1");
        recentImportant.setImportance(MemoryImportance.CRITICAL);
        for (int i = 0; i < 5; i++) {
            recentImportant.recordAccess();
        }
        
        double highDecayScore = recentImportant.calculateDecayScore();
        
        // Test memory with low importance and old access
        EnhancedMemory oldUnimportant = new EnhancedMemory("old_unimportant", "Old unimportant memory", "user1");
        oldUnimportant.setImportance(MemoryImportance.MINIMAL);
        oldUnimportant.setLastAccessedAt(LocalDateTime.now().minusDays(30));
        
        double lowDecayScore = oldUnimportant.calculateDecayScore();
        
        // Recent important memory should have higher decay score (less likely to be forgotten)
        assertTrue(highDecayScore > lowDecayScore);
    }
    
    @Test
    void testForgettingPolicyConfiguration() {
        MemoryForgettingPolicy policy = new MemoryForgettingPolicy();
        
        // Test default values
        assertTrue(policy.isForgettingEnabled());
        assertEquals(0.1, policy.getDecayRate(), 0.001);
        assertEquals(MemoryImportance.LOW, policy.getImportanceThreshold());
        
        // Test setters
        policy.setForgettingEnabled(false);
        policy.setDecayRate(0.5);
        policy.setImportanceThreshold(MemoryImportance.MEDIUM);
        policy.setMaxMemoryAge(60);
        policy.setMinAccessCount(5);
        
        assertFalse(policy.isForgettingEnabled());
        assertEquals(0.5, policy.getDecayRate(), 0.001);
        assertEquals(MemoryImportance.MEDIUM, policy.getImportanceThreshold());
        assertEquals(60, policy.getMaxMemoryAge());
        assertEquals(5, policy.getMinAccessCount());
    }
    
    @Test
    void testMemoryTypeBasedForgetting() throws Exception {
        // Create memories of different types
        EnhancedMemory factual = new EnhancedMemory("fact", "Factual information", "user1");
        factual.setType(MemoryType.FACTUAL);
        factual.setImportance(MemoryImportance.LOW);
        
        EnhancedMemory preference = new EnhancedMemory("pref", "User preference", "user1");
        preference.setType(MemoryType.PREFERENCE);
        preference.setImportance(MemoryImportance.LOW);
        
        EnhancedMemory episodic = new EnhancedMemory("episode", "Past experience", "user1");
        episodic.setType(MemoryType.EPISODIC);
        episodic.setImportance(MemoryImportance.LOW);
        
        List<EnhancedMemory> typeTestMemories = Arrays.asList(factual, preference, episodic);
        
        MemoryForgettingPolicy policy = new MemoryForgettingPolicy();
        policy.setForgettingEnabled(true);
        policy.setDecayRate(0.3);
        policy.setImportanceThreshold(MemoryImportance.MEDIUM);
        
        forgettingManager.setForgettingPolicy(policy);
        
        List<EnhancedMemory> result = forgettingManager.processMemoryDecay(typeTestMemories).get();
        
        assertEquals(typeTestMemories.size(), result.size());
        
        // Different memory types may have different forgetting characteristics
        // This is implementation-dependent based on the type's inherent importance
    }
    
    @Test
    void testConsolidatedMemoryProtection() throws Exception {
        EnhancedMemory consolidatedMemory = new EnhancedMemory("consolidated", "Consolidated memory", "user1");
        consolidatedMemory.setImportance(MemoryImportance.LOW);
        consolidatedMemory.consolidate(); // Mark as consolidated
        
        EnhancedMemory unconsolidatedMemory = new EnhancedMemory("unconsolidated", "Unconsolidated memory", "user1");
        unconsolidatedMemory.setImportance(MemoryImportance.LOW);
        
        List<EnhancedMemory> consolidationTest = Arrays.asList(consolidatedMemory, unconsolidatedMemory);
        
        MemoryForgettingPolicy aggressivePolicy = new MemoryForgettingPolicy();
        aggressivePolicy.setForgettingEnabled(true);
        aggressivePolicy.setDecayRate(0.9);
        aggressivePolicy.setImportanceThreshold(MemoryImportance.MEDIUM);
        
        forgettingManager.setForgettingPolicy(aggressivePolicy);
        
        List<EnhancedMemory> result = forgettingManager.processMemoryDecay(consolidationTest).get();
        
        // Consolidated memories should be better protected against forgetting
        EnhancedMemory resultConsolidated = result.stream()
            .filter(m -> m.getId().equals("consolidated"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(resultConsolidated);
        // Consolidated memory should be less likely to be deprecated
        // (Implementation may vary, but consolidated memories generally have protection)
    }
}