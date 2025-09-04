package com.mem0.examples.vector;

import com.mem0.Mem0;
import com.mem0.store.VectorStore;
import com.mem0.model.VectorEntry;
import com.mem0.core.EnhancedMemory;
import com.mem0.embedding.impl.MockEmbeddingProvider;
import com.mem0.llm.MockLLMProvider;
import com.mem0.vector.impl.InMemoryVectorStore;
import com.mem0.graph.impl.DefaultInMemoryGraphStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 向量数据库集成示例 - Vector Database Integration Example
 * 
 * 展示如何集成不同的向量数据库和自定义向量存储
 * Demonstrates integration with vector databases and custom vector stores
 * 
 * NOTE: These are simplified demo implementations showing the integration patterns.
 * For production use, implement with proper HTTP clients and error handling.
 */
public class VectorDatabaseExample {
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== Vector Database Integration Example ===\n");
        
        // 1. 内存向量存储示例
        System.out.println("1. Testing In-Memory Vector Store:");
        testInMemoryVectorStore();
        
        // 2. 自定义向量存储示例  
        System.out.println("\n2. Testing Custom Vector Store:");
        testCustomVectorStore();
        
        System.out.println("\n=== Example completed successfully! ===");
    }
    
    private static void testInMemoryVectorStore() throws Exception {
        Mem0 mem0 = Mem0.builder()
            .vectorStore(new InMemoryVectorStore())
            .graphStore(new DefaultInMemoryGraphStore())
            .llmProvider(new MockLLMProvider())
            .embeddingProvider(new MockEmbeddingProvider())
            .build();
        
        String memoryId = mem0.add("In-memory vector store is fast for development", "inmemory-user").get();
        System.out.println("   ✓ Added memory to in-memory store: " + memoryId);
        
        List<EnhancedMemory> results = mem0.search("vector store", "inmemory-user", 5).get();
        System.out.println("   ✓ Search returned " + results.size() + " results");
        
        mem0.close();
    }
    
    private static void testCustomVectorStore() throws Exception {
        Mem0 mem0 = Mem0.builder()
            .vectorStore(new CustomVectorStore())
            .graphStore(new DefaultInMemoryGraphStore()) 
            .llmProvider(new MockLLMProvider())
            .embeddingProvider(new MockEmbeddingProvider())
            .build();
        
        String memoryId = mem0.add("Custom vector stores allow flexible implementations", "custom-user").get();
        System.out.println("   ✓ Added memory to custom vector store: " + memoryId);
        
        mem0.close();
    }
}

/**
 * 自定义向量存储实现 - Custom Vector Store Implementation
 * This demonstrates how to implement a custom vector store for mem0-java
 */
class CustomVectorStore implements VectorStore {
    private static final Logger logger = LoggerFactory.getLogger(CustomVectorStore.class);
    
    private final Map<String, VectorEntry> vectors = new ConcurrentHashMap<>();
    
    @Override
    public CompletableFuture<String> insert(String collectionName, List<Float> embedding, 
                                           Map<String, Object> metadata) {
        return CompletableFuture.supplyAsync(() -> {
            String id = UUID.randomUUID().toString();
            VectorEntry entry = new VectorEntry(id, embedding, metadata);
            vectors.put(collectionName + ":" + id, entry);
            
            logger.debug("Inserted vector into custom store: {}", id);
            return id;
        });
    }
    
    @Override
    public CompletableFuture<List<VectorEntry>> search(String collectionName, List<Float> queryEmbedding, 
                                                       Map<String, Object> filter, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<VectorEntry> results = new ArrayList<>();
            
            vectors.entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith(collectionName + ":"))
                    .forEach(entry -> {
                        VectorEntry vectorEntry = entry.getValue();
                        // 简单的余弦相似度计算
                        double similarity = calculateCosineSimilarity(queryEmbedding, vectorEntry.getEmbedding());
                        
                        // 创建搜索结果
                        results.add(vectorEntry);
                    });
            
            return results.stream()
                    .limit(limit)
                    .collect(java.util.stream.Collectors.toList());
        });
    }
    
    @Override
    public CompletableFuture<Void> delete(String collectionName, String id) {
        return CompletableFuture.runAsync(() -> {
            vectors.remove(collectionName + ":" + id);
            logger.debug("Deleted vector from custom store: {}", id);
        });
    }
    
    @Override
    public CompletableFuture<Boolean> collectionExists(String collectionName) {
        return CompletableFuture.supplyAsync(() -> {
            return vectors.keySet().stream()
                    .anyMatch(key -> key.startsWith(collectionName + ":"));
        });
    }
    
    @Override
    public CompletableFuture<Void> createCollection(String collectionName, int dimension) {
        return CompletableFuture.runAsync(() -> {
            logger.debug("Created collection: {} (dimension: {})", collectionName, dimension);
        });
    }
    
    @Override
    public CompletableFuture<Void> dropCollection(String collectionName) {
        return CompletableFuture.runAsync(() -> {
            vectors.entrySet().removeIf(entry -> entry.getKey().startsWith(collectionName + ":"));
            logger.debug("Dropped collection: {}", collectionName);
        });
    }
    
    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.runAsync(() -> {
            vectors.clear();
            logger.info("Custom vector store closed");
        });
    }
    
    private double calculateCosineSimilarity(List<Float> vec1, List<Float> vec2) {
        if (vec1.size() != vec2.size()) return 0.0;
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vec1.size(); i++) {
            dotProduct += vec1.get(i) * vec2.get(i);
            norm1 += vec1.get(i) * vec1.get(i);
            norm2 += vec2.get(i) * vec2.get(i);
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}