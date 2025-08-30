package com.mem0.example;

import com.mem0.core.EnhancedMemory;
import com.mem0.core.MemoryType;
import com.mem0.core.MemoryImportance;
import com.mem0.core.ConflictType;
import com.mem0.core.ConflictResolutionStrategy;
import com.mem0.core.PruningStrategy;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Core functionality validation without external dependencies
 */
public class CoreValidation {
    
    public static void main(String[] args) {
        System.out.println("=== Mem0 Java Core Validation ===\n");
        
        boolean allTestsPassed = true;
        
        try {
            // Test EnhancedMemory class
            allTestsPassed &= testEnhancedMemory();
            
            // Test enums
            allTestsPassed &= testEnums();
            
            // Test memory operations
            allTestsPassed &= testMemoryOperations();
            
            // Test memory relationships
            allTestsPassed &= testMemoryRelationships();
            
            // Test memory scoring
            allTestsPassed &= testMemoryScoring();
            
            // Summary
            System.out.println("\n" + repeatString("=", 50));
            if (allTestsPassed) {
                System.out.println("✅ All core functionality tests PASSED!");
                System.out.println("The mem0-java implementation is syntactically correct and functional.");
            } else {
                System.out.println("❌ Some tests FAILED!");
            }
            System.out.println(repeatString("=", 50));
            
        } catch (Exception e) {
            System.err.println("❌ Validation failed with exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String repeatString(String str, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
    
    private static boolean testEnhancedMemory() {
        System.out.println("1. Testing EnhancedMemory class...");
        try {
            // Basic creation
            EnhancedMemory memory = new EnhancedMemory(
                "mem-001", 
                "User is a Java developer with 5 years experience", 
                "user-123"
            );
            
            // Verify basic properties
            assert memory.getId().equals("mem-001");
            assert memory.getContent().contains("Java developer");
            assert memory.getUserId().equals("user-123");
            assert memory.getType() == MemoryType.SEMANTIC; // Default type
            assert memory.getImportance() == MemoryImportance.MEDIUM; // Default importance
            assert memory.getAccessCount() == 1; // Initial access
            assert memory.getUpdateCount() == 0;
            assert !memory.isConsolidated();
            assert !memory.isDeprecated();
            assert memory.getCreatedAt() != null;
            assert memory.getLastAccessedAt() != null;
            
            System.out.println("   ✓ Basic memory creation and properties");
            
            // Test factory methods
            EnhancedMemory semantic = EnhancedMemory.createSemanticMemory(
                "sem-1", "Semantic information", "user-123");
            assert semantic.getType() == MemoryType.SEMANTIC;
            assert semantic.getImportance() == MemoryImportance.MEDIUM;
            
            EnhancedMemory episodic = EnhancedMemory.createEpisodicMemory(
                "epi-1", "Past experience", "user-123", LocalDateTime.now().minusDays(1));
            assert episodic.getType() == MemoryType.EPISODIC;
            assert episodic.getImportance() == MemoryImportance.HIGH;
            
            EnhancedMemory procedural = EnhancedMemory.createProceduralMemory(
                "proc-1", "How to do something", "user-123");
            assert procedural.getType() == MemoryType.PROCEDURAL;
            assert procedural.getImportance() == MemoryImportance.HIGH;
            
            EnhancedMemory preference = EnhancedMemory.createPreferenceMemory(
                "pref-1", "User prefers X over Y", "user-123", "category");
            assert preference.getType() == MemoryType.PREFERENCE;
            assert preference.getMetadata().containsKey("preference_category");
            assert preference.getTags().contains("preference");
            
            System.out.println("   ✓ Factory methods work correctly");
            
            return true;
        } catch (Exception | AssertionError e) {
            System.out.println("   ❌ EnhancedMemory test failed: " + e.getMessage());
            return false;
        }
    }
    
    private static boolean testEnums() {
        System.out.println("\n2. Testing enum classes...");
        try {
            // Test MemoryType enum
            assert MemoryType.SEMANTIC.getValue().equals("semantic");
            assert MemoryType.EPISODIC.getValue().equals("episodic");
            assert MemoryType.PROCEDURAL.getValue().equals("procedural");
            assert MemoryType.FACTUAL.getValue().equals("factual");
            assert MemoryType.CONTEXTUAL.getValue().equals("contextual");
            assert MemoryType.PREFERENCE.getValue().equals("preference");
            assert MemoryType.RELATIONSHIP.getValue().equals("relationship");
            assert MemoryType.TEMPORAL.getValue().equals("temporal");
            
            System.out.println("   ✓ MemoryType enum");
            
            // Test MemoryImportance enum
            assert MemoryImportance.CRITICAL.getScore() == 5.0;
            assert MemoryImportance.HIGH.getScore() == 4.0;
            assert MemoryImportance.MEDIUM.getScore() == 3.0;
            assert MemoryImportance.LOW.getScore() == 2.0;
            assert MemoryImportance.MINIMAL.getScore() == 1.0;
            
            System.out.println("   ✓ MemoryImportance enum");
            
            // Test ConflictType enum
            assert ConflictType.values().length >= 5;
            System.out.println("   ✓ ConflictType enum");
            
            // Test ConflictResolutionStrategy enum
            assert ConflictResolutionStrategy.values().length >= 5;
            System.out.println("   ✓ ConflictResolutionStrategy enum");
            
            // Test PruningStrategy enum
            assert PruningStrategy.values().length >= 5;
            System.out.println("   ✓ PruningStrategy enum");
            
            return true;
        } catch (Exception | AssertionError e) {
            System.out.println("   ❌ Enum test failed: " + e.getMessage());
            return false;
        }
    }
    
    private static boolean testMemoryOperations() {
        System.out.println("\n3. Testing memory operations...");
        try {
            EnhancedMemory memory = new EnhancedMemory(
                "mem-ops", "Test memory for operations", "user-123");
            
            // Test access tracking
            int initialAccessCount = memory.getAccessCount();
            LocalDateTime initialAccessTime = memory.getLastAccessedAt();
            
            // Simulate some delay
            try { Thread.sleep(10); } catch (InterruptedException e) {}
            
            memory.recordAccess();
            assert memory.getAccessCount() == initialAccessCount + 1;
            assert memory.getLastAccessedAt().isAfter(initialAccessTime);
            
            System.out.println("   ✓ Access tracking");
            
            // Test update tracking
            int initialUpdateCount = memory.getUpdateCount();
            LocalDateTime initialUpdateTime = memory.getUpdatedAt();
            String initialHash = memory.getContentHash();
            
            try { Thread.sleep(10); } catch (InterruptedException e) {}
            
            memory.recordUpdate("Updated content for testing");
            assert memory.getUpdateCount() == initialUpdateCount + 1;
            assert memory.getUpdatedAt().isAfter(initialUpdateTime);
            assert !memory.getContentHash().equals(initialHash);
            assert !memory.isConsolidated(); // Should reset consolidation
            
            System.out.println("   ✓ Update tracking");
            
            // Test consolidation
            assert !memory.isConsolidated();
            MemoryImportance beforeImportance = memory.getImportance();
            
            memory.consolidate();
            assert memory.isConsolidated();
            // Importance may increase after consolidation
            assert memory.getImportance().getScore() >= beforeImportance.getScore();
            
            System.out.println("   ✓ Memory consolidation");
            
            // Test deprecation
            assert !memory.isDeprecated();
            memory.deprecate();
            assert memory.isDeprecated();
            assert memory.getImportance() == MemoryImportance.MINIMAL;
            
            System.out.println("   ✓ Memory deprecation");
            
            // Test TTL
            EnhancedMemory ttlMemory = new EnhancedMemory("ttl", "TTL test", "user");
            assert ttlMemory.getExpiresAt() == null;
            assert !ttlMemory.isExpired();
            
            ttlMemory.setTTL(1); // 1 day
            assert ttlMemory.getExpiresAt() != null;
            assert !ttlMemory.isExpired();
            
            ttlMemory.setTTL(-1); // Past expiration
            assert ttlMemory.isExpired();
            
            System.out.println("   ✓ TTL handling");
            
            return true;
        } catch (Exception | AssertionError e) {
            System.out.println("   ❌ Memory operations test failed: " + e.getMessage());
            return false;
        }
    }
    
    private static boolean testMemoryRelationships() {
        System.out.println("\n4. Testing memory relationships...");
        try {
            EnhancedMemory memory = new EnhancedMemory(
                "rel-test", "Relationship test memory", "user-123");
            
            String relatedMemoryId = "related-mem-456";
            double similarity = 0.85;
            
            // Initially no relationships
            assert memory.getRelatedMemoryIds().isEmpty();
            assert memory.getSimilarityWith(relatedMemoryId) == 0.0;
            
            // Add relationship
            memory.addRelatedMemory(relatedMemoryId, similarity);
            assert memory.getRelatedMemoryIds().contains(relatedMemoryId);
            assert Math.abs(memory.getSimilarityWith(relatedMemoryId) - similarity) < 0.001;
            
            // Remove relationship
            memory.removeRelatedMemory(relatedMemoryId);
            assert !memory.getRelatedMemoryIds().contains(relatedMemoryId);
            assert memory.getSimilarityWith(relatedMemoryId) == 0.0;
            
            System.out.println("   ✓ Memory relationships");
            
            return true;
        } catch (Exception | AssertionError e) {
            System.out.println("   ❌ Memory relationships test failed: " + e.getMessage());
            return false;
        }
    }
    
    private static boolean testMemoryScoring() {
        System.out.println("\n5. Testing memory scoring algorithms...");
        try {
            // Create memory with high importance and frequent access
            EnhancedMemory importantMemory = new EnhancedMemory(
                "important", "Important memory", "user-123");
            importantMemory.setImportance(MemoryImportance.CRITICAL);
            for (int i = 0; i < 10; i++) {
                importantMemory.recordAccess();
            }
            
            double importantDecayScore = importantMemory.calculateDecayScore();
            
            // Create memory with low importance and rare access
            EnhancedMemory unimportantMemory = new EnhancedMemory(
                "unimportant", "Less important memory", "user-123");
            unimportantMemory.setImportance(MemoryImportance.MINIMAL);
            // Simulate old last access
            unimportantMemory.setLastAccessedAt(LocalDateTime.now().minusDays(30));
            
            double unimportantDecayScore = unimportantMemory.calculateDecayScore();
            
            // Important memory should have higher decay score (less likely to be forgotten)
            assert importantDecayScore > unimportantDecayScore;
            
            System.out.println("   ✓ Decay score calculation");
            
            // Test relevance scoring
            EnhancedMemory queryMemory = new EnhancedMemory(
                "query-test", "Java programming language tutorial", "user-123");
            
            double relevantScore = queryMemory.calculateRelevanceScore("Java programming");
            double lessRelevantScore = queryMemory.calculateRelevanceScore("Python data science");
            
            // More relevant query should have higher score
            assert relevantScore > lessRelevantScore;
            
            System.out.println("   ✓ Relevance score calculation");
            
            return true;
        } catch (Exception | AssertionError e) {
            System.out.println("   ❌ Memory scoring test failed: " + e.getMessage());
            return false;
        }
    }
}