package com.mem0.examples.basic;

import com.mem0.Mem0;
import com.mem0.config.Mem0Config;
import com.mem0.core.EnhancedMemory;
import com.mem0.embedding.EmbeddingProvider;
import com.mem0.embedding.impl.MockEmbeddingProvider;
import com.mem0.llm.LLMProvider;
import com.mem0.llm.MockLLMProvider;
import com.mem0.vector.impl.InMemoryVectorStore;
import com.mem0.graph.impl.DefaultInMemoryGraphStore;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 基础使用示例 - Basic Usage Example
 * 
 * 展示如何使用mem0-java进行基本的内存管理操作
 * Demonstrates basic memory management operations with mem0-java
 */
public class BasicUsageExample {
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== Mem0 Java Basic Usage Example ===\n");
        
        // 1. 初始化Mem0实例
        Mem0 mem0 = createMem0Instance();
        
        // 3. 基本操作示例
        runBasicOperations(mem0);
        
        // 4. 批量操作示例
        runBatchOperations(mem0);
        
        // 5. 搜索操作示例
        runSearchOperations(mem0);
        
        // 6. 清理资源
        mem0.close();
        
        System.out.println("\n=== Example completed successfully! ===");
    }
    
    private static Mem0 createMem0Instance() {
        System.out.println("1. Creating Mem0 instance...");
        
        // 使用Builder模式创建实例
        Mem0 mem0 = Mem0.builder()
                .vectorStore(new InMemoryVectorStore())
                .graphStore(new DefaultInMemoryGraphStore()) 
                .embeddingProvider(new MockEmbeddingProvider())
                .llmProvider(new MockLLMProvider())
                .build();
        
        System.out.println("   ✓ Mem0 instance created\n");
        return mem0;
    }
    
    private static void runBasicOperations(Mem0 mem0) throws Exception {
        System.out.println("2. Running basic memory operations...");
        
        // 添加记忆
        System.out.println("   Adding memories:");
        String userId = "demo-user";
        
        String memory1 = mem0.add("I love drinking coffee in the morning", userId).get();
        System.out.println("   ✓ Added memory 1: " + memory1);
        
        String memory2 = mem0.add("My favorite programming language is Java", userId).get();
        System.out.println("   ✓ Added memory 2: " + memory2);
        
        String memory3 = mem0.add("I work as a software engineer at TechCorp", userId).get();
        System.out.println("   ✓ Added memory 3: " + memory3);
        
        // 获取所有记忆
        List<EnhancedMemory> allMemories = mem0.getAll(userId).get();
        System.out.println("   ✓ Total memories: " + allMemories.size());
        
        // 更新记忆
        System.out.println("\n   Updating memory:");
        mem0.update(memory1, "I prefer tea over coffee in the afternoon").get();
        System.out.println("   ✓ Updated memory 1");
        
        System.out.println("   ✓ Basic operations completed\n");
    }
    
    private static void runBatchOperations(Mem0 mem0) throws Exception {
        System.out.println("3. Running batch operations...");
        String userId = "demo-user";
        
        // 批量添加记忆 (一个一个添加，因为没有批量接口)
        List<String> batchTexts = Arrays.asList(
            "I enjoy reading technical books",
            "My office is located in downtown", 
            "I have 5 years of experience in backend development",
            "I graduated from Computer Science program"
        );
        
        int addedCount = 0;
        for (String text : batchTexts) {
            mem0.add(text, userId).get();
            addedCount++;
        }
        System.out.println("   ✓ Added " + addedCount + " memories");
        
        // 获取用户的所有记忆
        List<EnhancedMemory> userMemories = mem0.getAll(userId).get();
        System.out.println("   ✓ User now has " + userMemories.size() + " memories");
        
        System.out.println("   ✓ Batch operations completed\n");
    }
    
    private static void runSearchOperations(Mem0 mem0) throws Exception {
        System.out.println("4. Running search operations...");
        String userId = "demo-user";
        
        // 搜索相关记忆
        String query = "What do you know about my work?";
        List<EnhancedMemory> workRelated = mem0.search(query, userId, 5).get();
        
        System.out.println("   Search query: \"" + query + "\"");
        System.out.println("   ✓ Found " + workRelated.size() + " related memories:");
        
        for (EnhancedMemory memory : workRelated) {
            System.out.println("     - " + memory.getContent());
        }
        
        // 获取统计信息
        if (!workRelated.isEmpty()) {
            Mem0.MemoryStatistics stats = mem0.getStatistics(userId).get();
            System.out.println("   ✓ Memory statistics: " + stats.toString());
        }
        
        System.out.println("   ✓ Search operations completed\n");
    }
}