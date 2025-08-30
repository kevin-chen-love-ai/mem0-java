package com.mem0.example;

import com.mem0.config.Mem0Config;
import com.mem0.core.MemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class ExampleApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(ExampleApplication.class);
    private static final String USER_ID = "demo-user";
    
    public static void main(String[] args) {
        System.out.println("🧠 Mem0 Java Demo Application");
        System.out.println("==============================");
        
        try {
            // Initialize Mem0 with default configuration
            Mem0Config config = createDemoConfiguration();
            MemoryService memoryService = new MemoryService(config);
            
            // Demo scenarios
            runInteractiveDemo(memoryService);
            
            // Cleanup
            memoryService.close().get();
            System.out.println("\n✅ Demo completed successfully!");
            
        } catch (Exception e) {
            logger.error("Demo failed", e);
            System.err.println("❌ Demo failed: " + e.getMessage());
        }
    }
    
    private static Mem0Config createDemoConfiguration() {
        Mem0Config config = new Mem0Config();
        
        // Use mock providers for demo (no external services required)
        config.getEmbedding().setProvider("mock");
        config.getLlm().setProvider("mock");
        
        // In production, you would configure real services:
        // config.getVectorStore().setHost(System.getenv("MILVUS_HOST"));
        // config.getGraphStore().setUri(System.getenv("NEO4J_URI"));
        // config.getLlm().setApiKey(System.getenv("OPENAI_API_KEY"));
        
        return config;
    }
    
    private static void runInteractiveDemo(MemoryService memoryService) throws Exception {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("\n1. Adding some initial memories...");
        
        // Add some demo memories
        CompletableFuture.allOf(
            memoryService.addMemory("User is a software developer working with Java", USER_ID),
            memoryService.addMemory("User prefers Spring Boot for web applications", USER_ID),
            memoryService.addMemory("User enjoys reading tech blogs and documentation", USER_ID),
            memoryService.addMemory("User has experience with microservices architecture", USER_ID),
            memoryService.addMemory("User likes to drink coffee while coding", USER_ID, "preference", 
                createMap("category", "beverage", "context", "work"))
        ).get();
        
        System.out.println("✅ Added 5 initial memories");
        
        System.out.println("\n2. Searching memories...");
        
        // Search for memories
        List<MemoryService.Memory> searchResults = memoryService.searchMemories(
            "What programming technologies does the user work with?", 
            USER_ID, 
            3
        ).get();
        
        System.out.println("🔍 Search results:");
        for (int i = 0; i < searchResults.size(); i++) {
            MemoryService.Memory memory = searchResults.get(i);
            System.out.printf("  %d. [%.3f] %s\n", 
                i + 1, memory.getRelevanceScore(), memory.getContent());
        }
        
        System.out.println("\n3. RAG Query Demo...");
        
        String ragResponse = memoryService.queryWithRAG(
            "Based on what you know about me, what kind of developer am I and what do I like?",
            USER_ID,
            5,
            "You are a helpful AI assistant with access to user memories."
        ).get();
        
        System.out.println("🤖 RAG Response:");
        System.out.println("   " + ragResponse);
        
        System.out.println("\n4. Interactive Session");
        System.out.println("You can now ask questions or add memories. Type 'quit' to exit.");
        
        while (true) {
            System.out.print("\n> ");
            String input = scanner.nextLine().trim();
            
            if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")) {
                break;
            }
            
            if (input.toLowerCase().startsWith("remember ")) {
                // Add new memory
                String memoryContent = input.substring(9);
                String memoryId = memoryService.addMemory(memoryContent, USER_ID).get();
                System.out.println("💾 Added memory: " + memoryId);
                
            } else if (input.toLowerCase().startsWith("search ")) {
                // Search memories
                String query = input.substring(7);
                List<MemoryService.Memory> results = memoryService.searchMemories(query, USER_ID, 3).get();
                
                System.out.println("🔍 Found " + results.size() + " memories:");
                for (int i = 0; i < results.size(); i++) {
                    MemoryService.Memory memory = results.get(i);
                    System.out.printf("  %d. [%.3f] %s\n", 
                        i + 1, memory.getRelevanceScore(), memory.getContent());
                }
                
            } else {
                // RAG query
                try {
                    String response = memoryService.queryWithRAG(input, USER_ID).get();
                    System.out.println("🤖 " + response);
                } catch (Exception e) {
                    System.out.println("❌ Error: " + e.getMessage());
                }
            }
        }
        
        scanner.close();
    }
    
    // Helper method for Java 8 compatibility
    private static Map<String, Object> createMap(String k1, String v1, String k2, String v2) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }
}