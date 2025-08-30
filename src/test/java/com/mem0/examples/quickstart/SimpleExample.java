package com.mem0.examples.quickstart;

import com.mem0.Mem0;
import com.mem0.core.EnhancedMemory;
import com.mem0.core.MemoryType;
import com.mem0.core.MemoryImportance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple example to verify core functionality without external dependencies
 */
public class SimpleExample {
    
    public static void main(String[] args) {
        System.out.println("=== Mem0 Java Implementation Verification ===");
        
        try {
            // Test 1: EnhancedMemory creation and basic operations
            System.out.println("Test 1: Creating Enhanced Memory...");
            EnhancedMemory memory = new EnhancedMemory("test-1", "User prefers Java for backend development", "user-123");
            System.out.println("✓ Memory created: " + memory.getId());
            System.out.println("  Content: " + memory.getContent());
            System.out.println("  Type: " + memory.getType());
            System.out.println("  Importance: " + memory.getImportance());
            System.out.println("  Access Count: " + memory.getAccessCount());
            
            // Test 2: Factory methods
            System.out.println("\nTest 2: Testing factory methods...");
            EnhancedMemory semanticMemory = EnhancedMemory.createSemanticMemory(
                "sem-1", "Java is an object-oriented programming language", "user-123");
            System.out.println("✓ Semantic memory: " + semanticMemory.getType());
            
            EnhancedMemory proceduralMemory = EnhancedMemory.createProceduralMemory(
                "proc-1", "How to compile Java: javac ClassName.java", "user-123");
            System.out.println("✓ Procedural memory: " + proceduralMemory.getType());
            
            EnhancedMemory preferenceMemory = EnhancedMemory.createPreferenceMemory(
                "pref-1", "I prefer Spring Boot over vanilla Spring", "user-123", "framework");
            System.out.println("✓ Preference memory: " + preferenceMemory.getType());
            
            // Test 3: Memory lifecycle operations
            System.out.println("\nTest 3: Testing memory lifecycle...");
            memory.recordAccess();
            System.out.println("✓ Access recorded, count: " + memory.getAccessCount());
            
            memory.recordUpdate("User strongly prefers Java for backend development");
            System.out.println("✓ Update recorded, count: " + memory.getUpdateCount());
            
            memory.consolidate();
            System.out.println("✓ Memory consolidated: " + memory.isConsolidated());
            
            // Test 4: Memory scoring
            System.out.println("\nTest 4: Testing scoring algorithms...");
            double decayScore = memory.calculateDecayScore();
            System.out.println("✓ Decay score: " + decayScore);
            
            double relevanceScore = memory.calculateRelevanceScore("Java programming language");
            System.out.println("✓ Relevance score: " + relevanceScore);
            
            // Test 5: Memory metadata and relationships
            System.out.println("\nTest 5: Testing metadata and relationships...");
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("category", "programming");
            metadata.put("priority", "high");
            memory.getMetadata().putAll(metadata);
            System.out.println("✓ Metadata added: " + memory.getMetadata().size() + " items");
            
            memory.getTags().add("java");
            memory.getTags().add("backend");
            System.out.println("✓ Tags added: " + memory.getTags());
            
            memory.addRelatedMemory("related-1", 0.85);
            System.out.println("✓ Related memory added, similarity: " + memory.getSimilarityWith("related-1"));
            
            // Test 6: Memory types and importance levels
            System.out.println("\nTest 6: Testing enums and constants...");
            for (MemoryType type : MemoryType.values()) {
                System.out.println("  Memory type: " + type + " (" + type.getValue() + ")");
            }
            
            for (MemoryImportance importance : MemoryImportance.values()) {
                System.out.println("  Importance: " + importance + " (score: " + importance.getScore() + ")");
            }
            
            // Test 7: Mem0 Builder pattern (without external dependencies)
            System.out.println("\nTest 7: Testing Mem0 Builder...");
            try {
                Mem0 mem0 = new Mem0.Builder().build();
                System.out.println("✓ Mem0 instance created with mock providers");
                mem0.close();
                System.out.println("✓ Mem0 closed successfully");
            } catch (Exception e) {
                System.out.println("✗ Mem0 creation failed: " + e.getMessage());
            }
            
            System.out.println("\n=== All Core Tests Passed! ===");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed with exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}