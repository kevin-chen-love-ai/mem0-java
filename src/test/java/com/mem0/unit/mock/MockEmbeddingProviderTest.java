package com.mem0.unit.mock;

import com.mem0.embedding.impl.MockEmbeddingProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class MockEmbeddingProviderTest {
    
    private MockEmbeddingProvider embeddingProvider;
    
    @BeforeEach
    void setUp() {
        embeddingProvider = new MockEmbeddingProvider();
    }
    
    @Test
    void testGetDimensions() {
        assertEquals(128, embeddingProvider.getDimension());
    }
    
    @Test
    void testGetProviderName() {
        assertEquals("Mock", embeddingProvider.getProviderName());
    }
    
    @Test
    void testGetModelName() {
        assertEquals("mock-embedding-model", embeddingProvider.getModelName());
    }
    
    @Test
    @Timeout(5)
    void testEmbedSingleText() throws Exception {
        String text = "This is a test sentence for embedding";
        
        List<Float> embedding = embeddingProvider.embed(text).get(5, TimeUnit.SECONDS);
        
        assertNotNull(embedding);
        assertEquals(1536, embedding.size());
        
        // Check that embedding values are reasonable (normalized vector)
        double magnitude = Math.sqrt(embedding.stream().mapToDouble(f -> f * f).sum());
        assertEquals(1.0, magnitude, 0.01); // Should be approximately unit length
        
        // Test determinism - same text should produce same embedding
        List<Float> embedding2 = embeddingProvider.embed(text).get(5, TimeUnit.SECONDS);
        assertEquals(embedding, embedding2);
    }
    
    @Test
    @Timeout(5)
    void testEmbedDifferentTexts() throws Exception {
        String text1 = "First test sentence";
        String text2 = "Second test sentence";
        
        List<Float> embedding1 = embeddingProvider.embed(text1).get(5, TimeUnit.SECONDS);
        List<Float> embedding2 = embeddingProvider.embed(text2).get(5, TimeUnit.SECONDS);
        
        assertNotNull(embedding1);
        assertNotNull(embedding2);
        assertEquals(1536, embedding1.size());
        assertEquals(1536, embedding2.size());
        
        // Different texts should produce different embeddings
        assertNotEquals(embedding1, embedding2);
    }
    
    @Test
    @Timeout(5)
    void testBatchEmbed() throws Exception {
        List<String> texts = Arrays.asList(
            "First text for batch embedding",
            "Second text for batch embedding",
            "Third text for batch embedding"
        );
        
        List<List<Float>> embeddings = embeddingProvider.embedBatch(texts).get(5, TimeUnit.SECONDS);
        
        assertNotNull(embeddings);
        assertEquals(3, embeddings.size());
        
        for (int i = 0; i < embeddings.size(); i++) {
            List<Float> embedding = embeddings.get(i);
            assertEquals(1536, embedding.size());
            
            // Check normalization
            double magnitude = Math.sqrt(embedding.stream().mapToDouble(f -> f * f).sum());
            assertEquals(1.0, magnitude, 0.01);
            
            // Verify consistency with single embed
            List<Float> singleEmbed = embeddingProvider.embed(texts.get(i)).get(5, TimeUnit.SECONDS);
            assertEquals(singleEmbed, embedding);
        }
    }
    
    @Test
    @Timeout(5)
    void testEmptyText() throws Exception {
        String emptyText = "";
        
        List<Float> embedding = embeddingProvider.embed(emptyText).get(5, TimeUnit.SECONDS);
        
        assertNotNull(embedding);
        assertEquals(1536, embedding.size());
        
        // Even empty text should produce a normalized embedding
        double magnitude = Math.sqrt(embedding.stream().mapToDouble(f -> f * f).sum());
        assertEquals(1.0, magnitude, 0.01);
    }
    
    @Test
    @Timeout(5)
    void testBatchEmbedEmpty() throws Exception {
        List<String> emptyList = Arrays.asList();
        
        List<List<Float>> embeddings = embeddingProvider.embedBatch(emptyList).get(5, TimeUnit.SECONDS);
        
        assertNotNull(embeddings);
        assertEquals(0, embeddings.size());
    }
}