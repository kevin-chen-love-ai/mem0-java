package com.mem0.unit.core;

import com.mem0.Mem0;
import com.mem0.config.Mem0Config;
import com.mem0.core.EnhancedMemory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Fast memory tests using mock providers to avoid network timeouts
 */
public class FastMemoryTest {
    
    private Mem0 mem0;
    private String testUserId;

    @Before
    public void setUp() {
        testUserId = "fast-test-user-" + System.currentTimeMillis();
        
        // Create config with mock providers for speed
        Mem0Config config = new Mem0Config();
        
        // Use mock/simple providers to avoid network calls
        config.getLlm().setProvider("rulebased");
        config.getEmbedding().setProvider("tfidf");
        config.getVectorStore().setProvider("inmemory");
        config.getGraphStore().setProvider("inmemory");
        
        mem0 = new Mem0(config);
    }

    @After
    public void tearDown() {
        if (mem0 != null) {
            try {
                mem0.close();
            } catch (Exception e) {
                System.err.println("Warning: Failed to close mem0: " + e.getMessage());
            }
        }
    }

    @Test(timeout = 15000) // 15 second timeout
    public void testBasicMemoryAddAndSearch() throws Exception {
        // Test add memory
        String content = "User likes Java programming";
        String memoryId = mem0.add(content, testUserId).get(5, TimeUnit.SECONDS);
        
        assertNotNull("Memory ID should not be null", memoryId);
        assertFalse("Memory ID should not be empty", memoryId.isEmpty());

        // Test search (may return empty list with mock providers, that's ok)
        List<EnhancedMemory> searchResults = mem0.search("Java", testUserId, 5)
                .get(5, TimeUnit.SECONDS);
        assertNotNull("Search results should not be null", searchResults);
        System.out.println("Found " + searchResults.size() + " memories for 'Java' search");
    }

    @Test(timeout = 10000) // 10 second timeout
    public void testGetAllMemories() throws Exception {
        // Add a couple of memories
        mem0.add("Memory 1: User is from Beijing", testUserId).get(3, TimeUnit.SECONDS);
        mem0.add("Memory 2: User studies computer science", testUserId).get(3, TimeUnit.SECONDS);

        // Get all memories
        List<EnhancedMemory> allMemories = mem0.getAll(testUserId).get(3, TimeUnit.SECONDS);
        
        assertNotNull("All memories list should not be null", allMemories);
        assertTrue("Should have at least 2 memories", allMemories.size() >= 2);
        System.out.println("Found " + allMemories.size() + " total memories");
    }

    @Test(timeout = 8000) // 8 second timeout
    public void testMemoryDeletion() throws Exception {
        // Add memory
        String content = "Temporary memory for deletion test";
        String memoryId = mem0.add(content, testUserId).get(3, TimeUnit.SECONDS);
        assertNotNull("Memory ID should not be null", memoryId);

        // Delete memory
        mem0.delete(memoryId).get(3, TimeUnit.SECONDS);

        // Verify deletion (get should return null)
        EnhancedMemory deletedMemory = mem0.get(memoryId).get(3, TimeUnit.SECONDS);
        assertNull("Deleted memory should be null", deletedMemory);
        System.out.println("Memory deletion test passed");
    }

    @Test(timeout = 5000) // 5 second timeout
    public void testConfigurationValidation() {
        // Test that our configuration is valid
        Mem0Config config = new Mem0Config();
        config.getLlm().setProvider("rulebased");
        config.getEmbedding().setProvider("tfidf");
        config.getVectorStore().setProvider("inmemory");
        config.getGraphStore().setProvider("inmemory");
        
        // Should not throw exception
        try (Mem0 testMem0 = new Mem0(config)) {
            assertNotNull("Mem0 should be created successfully", testMem0);
            System.out.println("Configuration validation passed");
        }
    }

    @Test(timeout = 5000) // 5 second timeout
    public void testBuilderPattern() throws Exception {
        // Test Mem0 builder with fast providers
        try (Mem0 builderMem0 = Mem0.builder()
                .llm("rulebased", "dummy-key")
                .embedding("tfidf", "dummy-key")
                .build()) {
            
            assertNotNull("Builder-created Mem0 should not be null", builderMem0);
            
            // Test basic operation
            String memoryId = builderMem0.add("Builder test memory", testUserId).get(3, TimeUnit.SECONDS);
            assertNotNull("Memory ID from builder instance should not be null", memoryId);
            System.out.println("Builder pattern test passed");
        }
    }
}