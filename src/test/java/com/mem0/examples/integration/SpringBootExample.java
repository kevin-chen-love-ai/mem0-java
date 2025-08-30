package com.mem0.examples.integration;

import com.mem0.Mem0;
import com.mem0.core.EnhancedMemory;
import com.mem0.core.MemoryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@SpringBootApplication
@EnableConfigurationProperties
public class SpringBootExample implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(SpringBootExample.class);
    
    @Autowired
    private Mem0 mem0;
    
    // Java 8 compatible helper method for creating maps
    private static Map<String, Object> createMap(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }
    
    private static Map<String, Object> createMap(String k1, Object v1) {
        Map<String, Object> map = new HashMap<>();
        map.put(k1, v1);
        return map;
    }
    
    public static void main(String[] args) {
        SpringApplication.run(SpringBootExample.class, args);
    }
    
    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting Mem0 Spring Boot Example");
        
        try {
            runDemo();
        } catch (Exception e) {
            logger.error("Demo failed", e);
        }
        
        logger.info("Mem0 Spring Boot Example completed");
    }
    
    private void runDemo() throws Exception {
        String userId = "spring-demo-user";
        
        logger.info("=== Mem0 Spring Boot Demo ===");
        
        // Add some memories
        logger.info("1. Adding memories...");
        
        Map<String, Object> workMetadata = new HashMap<>();
        workMetadata.put("category", "work");
        workMetadata.put("priority", "high");
        
        CompletableFuture<String> memory1 = mem0.add(
            "User is a Spring Boot developer working on microservices",
            userId,
            MemoryType.FACTUAL.getValue(),
            workMetadata
        );
        
        CompletableFuture<String> memory2 = mem0.add(
            "User prefers using Spring Data JPA for database access",
            userId,
            MemoryType.PREFERENCE.getValue(),
            createMap("category", "technology", "domain", "database")
        );
        
        CompletableFuture<String> memory3 = mem0.add(
            "How to configure Spring Boot application properties for different environments",
            userId,
            MemoryType.PROCEDURAL.getValue(),
            createMap("category", "knowledge", "topic", "configuration")
        );
        
        // Wait for all memories to be added
        CompletableFuture.allOf(memory1, memory2, memory3).get();
        
        String memory1Id = memory1.get();
        String memory2Id = memory2.get();
        String memory3Id = memory3.get();
        
        logger.info("Added memories: {}, {}, {}", memory1Id, memory2Id, memory3Id);
        
        // Search memories
        logger.info("2. Searching memories...");
        
        List<EnhancedMemory> searchResults = mem0.search(
            "What does the user work with?", userId, 3
        ).get();
        
        logger.info("Found {} memories:", searchResults.size());
        for (EnhancedMemory memory : searchResults) {
            logger.info("  - [{}] {} (score: {:.3f})", 
                memory.getType(), 
                memory.getContent().substring(0, Math.min(50, memory.getContent().length())) + "...",
                memory.getRelevanceScore());
        }
        
        // Get all memories
        logger.info("3. Getting all memories...");
        
        List<EnhancedMemory> allMemories = mem0.getAll(userId).get();
        logger.info("Total memories: {}", allMemories.size());
        
        for (EnhancedMemory memory : allMemories) {
            logger.info("  - {} [{}] {} (importance: {}, access count: {})",
                memory.getId(),
                memory.getType(),
                memory.getContent().substring(0, Math.min(40, memory.getContent().length())) + "...",
                memory.getImportance(),
                memory.getAccessCount());
        }
        
        // Create relationships
        logger.info("4. Creating relationships...");
        
        String relationshipId = mem0.createRelationship(
            memory1Id, memory2Id, "RELATED_TO",
            createMap("reason", "Both are about user's work preferences")
        ).get();
        
        logger.info("Created relationship: {}", relationshipId);
        
        // Get related memories
        List<EnhancedMemory> relatedMemories = mem0.getRelated(memory1Id, "RELATED_TO").get();
        logger.info("Found {} related memories to {}", relatedMemories.size(), memory1Id);
        
        // RAG Query
        logger.info("5. RAG Query...");
        
        String response = mem0.queryWithRAG(
            "What kind of developer is the user and what technologies do they prefer?",
            userId,
            5,
            "You are a helpful assistant with access to user's profile information."
        ).get();
        
        logger.info("RAG Response: {}", response);
        
        // Memory statistics
        logger.info("6. Memory statistics...");
        
        Mem0.MemoryStatistics stats = mem0.getStatistics(userId).get();
        logger.info("Statistics: {}", stats);
        
        // Demonstrate advanced features
        logger.info("7. Advanced features...");
        
        // Memory classification
        MemoryType classifiedType = mem0.classifyMemory(
            "User always uses Maven for dependency management"
        ).get();
        logger.info("Classified type: {}", classifiedType);
        
        // Conflict detection
        // Add a potentially conflicting memory
        String conflictingMemoryId = mem0.add(
            "User prefers Gradle over Maven for build management",
            userId
        ).get();
        
        logger.info("Added potentially conflicting memory: {}", conflictingMemoryId);
        
        // Update importance scores
        mem0.updateImportanceScores(userId).get();
        logger.info("Updated importance scores");
        
        // Process memory decay (simulation)
        int forgottenCount = mem0.processMemoryDecay(userId).get();
        logger.info("Processed memory decay, {} memories forgotten", forgottenCount);
        
        // Final statistics
        Mem0.MemoryStatistics finalStats = mem0.getStatistics(userId).get();
        logger.info("Final statistics: {}", finalStats);
        
        logger.info("Demo completed successfully!");
    }
}