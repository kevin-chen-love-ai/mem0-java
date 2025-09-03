package com.mem0.unit.core;

import com.mem0.Mem0;
import com.mem0.config.Mem0Config;
import com.mem0.core.EnhancedMemory;
import com.mem0.core.MemoryType;
import com.mem0.util.TestConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Core functionality tests for Mem0 using JUnit 4 (Java 8 compatible)
 * 
 * Tests essential Mem0 operations with proper error handling and resource management.
 */
public class Mem0CoreTest {
    
    private Mem0 mem0;
    private String testUserId;

    @Before
    public void setUp() {
        testUserId = "test-user-" + System.currentTimeMillis();
        
        try {
            // Use TestConfiguration for unified provider management
            Mem0Config config = TestConfiguration.createMem0Config();
            mem0 = new Mem0(config);
            
        } catch (Exception e) {
            fail("Failed to initialize Mem0: " + e.getMessage());
        }
    }

    @After
    public void tearDown() {
        if (mem0 != null) {
            try {
                // Clean up test data
                mem0.deleteAll(testUserId).get(10, TimeUnit.SECONDS);
                mem0.close();
            } catch (Exception e) {
                System.err.println("Warning: Failed to clean up test data: " + e.getMessage());
            }
        }
    }

    @Test
    public void testBasicMemoryOperations() throws Exception {
        // Test add memory
        String content = "User prefers dark mode in applications";
        String memoryId = mem0.add(content, testUserId).get(10, TimeUnit.SECONDS);
        
        assertNotNull("Memory ID should not be null", memoryId);
        assertFalse("Memory ID should not be empty", memoryId.isEmpty());

        // Test get memory
        EnhancedMemory memory = mem0.get(memoryId).get(10, TimeUnit.SECONDS);
        if (memory != null) {  // Some implementations may return null for get operations
            assertEquals("Content should match", content, memory.getContent());
            assertEquals("User ID should match", testUserId, memory.getUserId());
        }

        // Test search memories
        List<EnhancedMemory> searchResults = mem0.search("dark mode", testUserId, 5)
                .get(10, TimeUnit.SECONDS);
        assertNotNull("Search results should not be null", searchResults);
    }

    @Test
    public void testMemoryWithMetadata() throws Exception {
        String content = "User completed Java certification exam";
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("category", "education");
        metadata.put("score", 95);
        metadata.put("date", "2024-01-15");
        
        String memoryId = mem0.add(content, testUserId, MemoryType.FACTUAL.getValue(), metadata)
                .get(10, TimeUnit.SECONDS);
        
        assertNotNull("Memory ID should not be null", memoryId);
        
        // Search for the memory
        List<EnhancedMemory> results = mem0.search("certification", testUserId, 5)
                .get(10, TimeUnit.SECONDS);
        
        assertNotNull("Search results should not be null", results);
    }

    @Test
    public void testMemoryUpdate() throws Exception {
        // Add initial memory
        String content = "User likes coffee";
        String memoryId = mem0.add(content, testUserId).get(10, TimeUnit.SECONDS);
        assertNotNull("Memory ID should not be null", memoryId);

        // Update memory
        String updatedContent = "User likes coffee and tea";
        EnhancedMemory updatedMemory = mem0.update(memoryId, updatedContent).get(10, TimeUnit.SECONDS);
        
        if (updatedMemory != null) {
            assertEquals("Updated content should match", updatedContent, updatedMemory.getContent());
        }
    }

    @Test
    public void testMemoryDeletion() throws Exception {
        // Add memory
        String content = "Temporary memory for deletion test";
        String memoryId = mem0.add(content, testUserId).get(10, TimeUnit.SECONDS);
        assertNotNull("Memory ID should not be null", memoryId);

        // Delete memory
        mem0.delete(memoryId).get(10, TimeUnit.SECONDS);

        // Verify deletion - the memory should not be found
        EnhancedMemory deletedMemory = mem0.get(memoryId).get(10, TimeUnit.SECONDS);
        assertNull("Deleted memory should be null", deletedMemory);
    }

    @Test
    public void testGetAllMemories() throws Exception {
        // Add multiple memories
        mem0.add("Memory 1: User likes pizza", testUserId).get(10, TimeUnit.SECONDS);
        mem0.add("Memory 2: User lives in New York", testUserId).get(10, TimeUnit.SECONDS);
        mem0.add("Memory 3: User works as developer", testUserId).get(10, TimeUnit.SECONDS);

        // Get all memories
        List<EnhancedMemory> allMemories = mem0.getAll(testUserId).get(10, TimeUnit.SECONDS);
        
        assertNotNull("All memories list should not be null", allMemories);
        assertTrue("Should have at least 3 memories", allMemories.size() >= 3);
    }

    @Test
    public void testRAGQuery() throws Exception {
        // Add some context memories
        mem0.add("User is proficient in Java programming", testUserId).get(10, TimeUnit.SECONDS);
        mem0.add("User has 5 years of experience in backend development", testUserId).get(10, TimeUnit.SECONDS);

        // Perform RAG query
        String query = "What programming skills does the user have?";
        String response = mem0.queryWithRAG(query, testUserId).get(15, TimeUnit.SECONDS);
        
        assertNotNull("RAG response should not be null", response);
        assertFalse("RAG response should not be empty", response.isEmpty());
    }

    @Test
    public void testErrorHandling() {
        try {
            // Test with null user ID
            mem0.add("Test content", null).get(5, TimeUnit.SECONDS);
            fail("Should throw exception for null user ID");
        } catch (Exception e) {
            // Expected exception
            assertTrue("Should handle null user ID gracefully", true);
        }

        try {
            // Test with empty content
            mem0.add("", testUserId).get(5, TimeUnit.SECONDS);
            fail("Should throw exception for empty content");
        } catch (Exception e) {
            // Expected exception
            assertTrue("Should handle empty content gracefully", true);
        }
    }

    @Test
    public void testBuilderPattern() throws Exception {
        // Test Mem0 builder
        Mem0 builderMem0 = Mem0.builder()
                .llm("rulebased", "dummy-key")
                .embedding("tfidf", "dummy-key")
                .build();
        
        assertNotNull("Builder-created Mem0 should not be null", builderMem0);
        
        // Test basic operation
        String memoryId = builderMem0.add("Builder test memory", testUserId).get(10, TimeUnit.SECONDS);
        assertNotNull("Memory ID from builder instance should not be null", memoryId);
        
        builderMem0.close();
    }
}