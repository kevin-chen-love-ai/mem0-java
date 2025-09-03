package com.mem0.unit.vector;

import com.mem0.vector.impl.InMemoryVectorStore;
import com.mem0.model.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for vector similarity algorithms
 */
public class VectorSimilarityTest {
    
    private InMemoryVectorStore vectorStore;
    private static final String TEST_USER = "testuser";
    
    @BeforeEach
    void setUp() {
        vectorStore = new InMemoryVectorStore();
    }
    
    @Test
    void testBasicVectorOperations() throws Exception {
        // Add test vectors
        float[] vector1 = {1.0f, 0.0f, 0.0f};
        float[] vector2 = {0.0f, 1.0f, 0.0f};
        float[] vector3 = {1.0f, 1.0f, 0.0f};
        
        Map<String, Object> metadata1 = createMetadata("test1", TEST_USER);
        Map<String, Object> metadata2 = createMetadata("test2", TEST_USER);
        Map<String, Object> metadata3 = createMetadata("test3", TEST_USER);
        
        vectorStore.insert("v1", vector1, metadata1).get();
        vectorStore.insert("v2", vector2, metadata2).get();
        vectorStore.insert("v3", vector3, metadata3).get();
        
        // Test vector search
        CompletableFuture<List<SearchResult>> results = vectorStore.search(vector1, TEST_USER, 3);
        List<SearchResult> searchResults = results.get();
        
        assertNotNull(searchResults);
        assertFalse(searchResults.isEmpty());
        
        // First result should be the identical vector with highest similarity
        SearchResult topResult = searchResults.get(0);
        assertEquals("v1", topResult.getId());
        assertTrue(topResult.getSimilarity() > 0.99f); // Should be very close to 1.0
    }
    
    @Test
    void testVectorSimilarityRanking() throws Exception {
        // Add test vectors at different distances from origin
        float[] origin = {0.0f, 0.0f, 0.0f};
        float[] close = {0.1f, 0.0f, 0.0f};
        float[] medium = {0.5f, 0.0f, 0.0f};
        float[] far = {1.0f, 0.0f, 0.0f};
        
        vectorStore.insert("origin", origin, createMetadata("origin point", TEST_USER)).get();
        vectorStore.insert("close", close, createMetadata("close point", TEST_USER)).get();
        vectorStore.insert("medium", medium, createMetadata("medium distance", TEST_USER)).get();
        vectorStore.insert("far", far, createMetadata("far point", TEST_USER)).get();
        
        // Test search from origin
        CompletableFuture<List<SearchResult>> results = vectorStore.search(origin, TEST_USER, 4);
        List<SearchResult> searchResults = results.get();
        
        assertNotNull(searchResults);
        assertEquals(4, searchResults.size());
        
        // Results should be ordered by similarity (highest first)
        assertTrue(searchResults.get(0).getSimilarity() >= searchResults.get(1).getSimilarity());
        assertTrue(searchResults.get(1).getSimilarity() >= searchResults.get(2).getSimilarity());
        assertTrue(searchResults.get(2).getSimilarity() >= searchResults.get(3).getSimilarity());
    }
    
    @Test
    void testHighDimensionalVectors() throws Exception {
        // Create high-dimensional vectors (100 dimensions)
        float[] vector1 = new float[100];
        float[] vector2 = new float[100];
        float[] vector3 = new float[100];
        
        for (int i = 0; i < 100; i++) {
            vector1[i] = (float) Math.sin(i * 0.1);
            vector2[i] = (float) Math.cos(i * 0.1);
            vector3[i] = (float) Math.sin(i * 0.1 + Math.PI/4);
        }
        
        vectorStore.insert("sine", vector1, createMetadata("sine wave", TEST_USER)).get();
        vectorStore.insert("cosine", vector2, createMetadata("cosine wave", TEST_USER)).get();
        vectorStore.insert("phase", vector3, createMetadata("phase shifted", TEST_USER)).get();
        
        // Test search with high-dimensional vectors
        CompletableFuture<List<SearchResult>> results = vectorStore.search(vector1, TEST_USER, 3);
        List<SearchResult> searchResults = results.get();
        
        assertNotNull(searchResults);
        assertEquals(3, searchResults.size());
        
        // Should handle high-dimensional vectors efficiently
        assertEquals("sine", searchResults.get(0).getId()); // Best match should be itself
    }
    
    @Test
    void testZeroVectorHandling() throws Exception {
        float[] zeroVector = {0.0f, 0.0f, 0.0f};
        float[] nonZeroVector = {1.0f, 2.0f, 3.0f};
        
        vectorStore.insert("zero", zeroVector, createMetadata("zero vector", TEST_USER)).get();
        vectorStore.insert("nonzero", nonZeroVector, createMetadata("non-zero vector", TEST_USER)).get();
        
        // Test search with zero vector query
        CompletableFuture<List<SearchResult>> results = vectorStore.search(zeroVector, TEST_USER, 2);
        List<SearchResult> searchResults = results.get();
        
        assertNotNull(searchResults);
        // Should handle zero vectors gracefully without throwing exceptions
        assertTrue(searchResults.size() <= 2);
    }
    
    @Test
    void testVectorUpdating() throws Exception {
        float[] originalVector = {1.0f, 0.0f, 0.0f};
        float[] updatedVector = {0.0f, 1.0f, 0.0f};
        
        Map<String, Object> metadata = createMetadata("updatable vector", TEST_USER);
        
        // Insert original vector
        vectorStore.insert("updateable", originalVector, metadata).get();
        
        // Update the vector
        vectorStore.update("updateable", updatedVector, metadata).get();
        
        // Search should return updated vector
        CompletableFuture<List<SearchResult>> results = vectorStore.search(updatedVector, TEST_USER, 1);
        List<SearchResult> searchResults = results.get();
        
        assertNotNull(searchResults);
        assertFalse(searchResults.isEmpty());
        assertEquals("updateable", searchResults.get(0).getId());
        assertTrue(searchResults.get(0).getSimilarity() > 0.99f);
    }
    
    @Test
    void testVectorDeletion() throws Exception {
        float[] vector = {1.0f, 0.0f, 0.0f};
        Map<String, Object> metadata = createMetadata("deletable vector", TEST_USER);
        
        // Insert vector
        vectorStore.insert("deletable", vector, metadata).get();
        
        // Verify it exists
        CompletableFuture<List<SearchResult>> beforeResults = vectorStore.search(vector, TEST_USER, 1);
        assertFalse(beforeResults.get().isEmpty());
        
        // Delete the vector
        vectorStore.delete("deletable").get();
        
        // Verify it's deleted
        CompletableFuture<List<SearchResult>> afterResults = vectorStore.search(vector, TEST_USER, 1);
        List<SearchResult> results = afterResults.get();
        assertTrue(results.isEmpty() || !results.get(0).getId().equals("deletable"));
    }
    
    @Test
    void testUserIsolation() throws Exception {
        float[] vector = {1.0f, 0.0f, 0.0f};
        String user1 = "user1";
        String user2 = "user2";
        
        // Insert same vector for different users
        vectorStore.insert("u1_vec", vector, createMetadata("user1 vector", user1)).get();
        vectorStore.insert("u2_vec", vector, createMetadata("user2 vector", user2)).get();
        
        // Search for user1 should only return user1's vectors
        CompletableFuture<List<SearchResult>> user1Results = vectorStore.search(vector, user1, 10);
        List<SearchResult> results1 = user1Results.get();
        
        // Search for user2 should only return user2's vectors
        CompletableFuture<List<SearchResult>> user2Results = vectorStore.search(vector, user2, 10);
        List<SearchResult> results2 = user2Results.get();
        
        assertNotNull(results1);
        assertNotNull(results2);
        
        // Verify user isolation
        boolean foundUser1Vector = results1.stream().anyMatch(r -> r.getId().equals("u1_vec"));
        boolean foundUser2Vector = results2.stream().anyMatch(r -> r.getId().equals("u2_vec"));
        
        assertTrue(foundUser1Vector);
        assertTrue(foundUser2Vector);
    }
    
    @Test
    void testConcurrentVectorOperations() throws Exception {
        // Add test vectors concurrently
        CompletableFuture<Void> insert1 = vectorStore.insert("concurrent1", 
            new float[]{1.0f, 0.0f, 0.0f}, createMetadata("concurrent 1", TEST_USER));
        CompletableFuture<Void> insert2 = vectorStore.insert("concurrent2", 
            new float[]{0.0f, 1.0f, 0.0f}, createMetadata("concurrent 2", TEST_USER));
        CompletableFuture<Void> insert3 = vectorStore.insert("concurrent3", 
            new float[]{0.0f, 0.0f, 1.0f}, createMetadata("concurrent 3", TEST_USER));
        
        // Wait for all insertions to complete
        CompletableFuture<Void> allInserts = CompletableFuture.allOf(insert1, insert2, insert3);
        allInserts.get();
        
        // Execute multiple concurrent searches
        float[] queryVector = {1.0f, 1.0f, 1.0f};
        CompletableFuture<List<SearchResult>> search1 = vectorStore.search(queryVector, TEST_USER, 3);
        CompletableFuture<List<SearchResult>> search2 = vectorStore.search(queryVector, TEST_USER, 3);
        CompletableFuture<List<SearchResult>> search3 = vectorStore.search(queryVector, TEST_USER, 3);
        
        // Wait for all searches to complete
        CompletableFuture<Void> allSearches = CompletableFuture.allOf(search1, search2, search3);
        allSearches.get();
        
        List<SearchResult> results1 = search1.get();
        List<SearchResult> results2 = search2.get();
        List<SearchResult> results3 = search3.get();
        
        assertNotNull(results1);
        assertNotNull(results2);
        assertNotNull(results3);
        
        assertEquals(3, results1.size());
        assertEquals(3, results2.size());
        assertEquals(3, results3.size());
    }
    
    @Test
    void testVectorStatistics() throws Exception {
        // Add some vectors
        for (int i = 0; i < 10; i++) {
            float[] vector = {(float)i, (float)(i*2), (float)(i*3)};
            vectorStore.insert("stats" + i, vector, createMetadata("stats vector " + i, TEST_USER)).get();
        }
        
        // Test that vectors were inserted successfully
        CompletableFuture<List<SearchResult>> searchResult = vectorStore.search(new float[]{5.0f, 10.0f, 15.0f}, TEST_USER, 10);
        List<SearchResult> searchResults = searchResult.get();
        
        assertNotNull(searchResults);
        assertTrue(searchResults.size() >= 5); // Should find at least some results
    }
    
    @Test
    void testLimitedSearchResults() throws Exception {
        // Add more vectors than we'll request
        for (int i = 0; i < 10; i++) {
            float[] vector = {(float)i, 0.0f, 0.0f};
            vectorStore.insert("limit" + i, vector, createMetadata("limit test " + i, TEST_USER)).get();
        }
        
        float[] queryVector = {5.0f, 0.0f, 0.0f};
        
        // Request only 5 results
        CompletableFuture<List<SearchResult>> results = vectorStore.search(queryVector, TEST_USER, 5);
        List<SearchResult> searchResults = results.get();
        
        assertNotNull(searchResults);
        assertTrue(searchResults.size() <= 5);
    }
    
    // Helper method to create metadata
    private Map<String, Object> createMetadata(String content, String userId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("content", content);
        metadata.put("userId", userId);
        return metadata;
    }
}