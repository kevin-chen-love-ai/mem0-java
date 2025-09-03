package examples.basic;

import com.mem0.Mem0;
import com.mem0.config.Mem0Configuration;
import com.mem0.core.Memory;
import com.mem0.embedding.EmbeddingProvider;
import com.mem0.embedding.impl.MockEmbeddingProvider;
import com.mem0.llm.LLMProvider;
import com.mem0.llm.MockLLMProvider;

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
        
        // 1. 创建配置
        Mem0Configuration config = createBasicConfiguration();
        
        // 2. 初始化Mem0实例
        Mem0 mem0 = new Mem0(config);
        
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
    
    private static Mem0Configuration createBasicConfiguration() {
        System.out.println("1. Creating basic configuration...");
        
        Mem0Configuration config = new Mem0Configuration();
        
        // 使用Mock提供者进行演示
        config.setLlmProvider(new MockLLMProvider());
        config.setEmbeddingProvider(new MockEmbeddingProvider());
        
        // 基础配置
        config.setUserId("demo-user");
        config.setApplicationName("BasicExample");
        
        System.out.println("   ✓ Configuration created\n");
        return config;
    }
    
    private static void runBasicOperations(Mem0 mem0) throws Exception {
        System.out.println("2. Running basic memory operations...");
        
        // 添加记忆
        System.out.println("   Adding memories:");
        
        String memory1 = mem0.add("I love drinking coffee in the morning");
        System.out.println("   ✓ Added memory 1: " + memory1);
        
        String memory2 = mem0.add("My favorite programming language is Java");
        System.out.println("   ✓ Added memory 2: " + memory2);
        
        String memory3 = mem0.add("I work as a software engineer at TechCorp");
        System.out.println("   ✓ Added memory 3: " + memory3);
        
        // 获取所有记忆
        List<Memory> allMemories = mem0.getAll();
        System.out.println("   ✓ Total memories: " + allMemories.size());
        
        // 更新记忆
        System.out.println("\n   Updating memory:");
        mem0.update(memory1, "I prefer tea over coffee in the afternoon");
        System.out.println("   ✓ Updated memory 1");
        
        System.out.println("   ✓ Basic operations completed\n");
    }
    
    private static void runBatchOperations(Mem0 mem0) throws Exception {
        System.out.println("3. Running batch operations...");
        
        // 批量添加记忆
        List<String> batchTexts = List.of(
            "I enjoy reading technical books",
            "My office is located in downtown",
            "I have 5 years of experience in backend development",
            "I graduated from Computer Science program"
        );
        
        List<String> batchIds = mem0.addBatch(batchTexts);
        System.out.println("   ✓ Added " + batchIds.size() + " memories in batch");
        
        // 获取用户的所有记忆
        List<Memory> userMemories = mem0.getAll("demo-user");
        System.out.println("   ✓ User now has " + userMemories.size() + " memories");
        
        System.out.println("   ✓ Batch operations completed\n");
    }
    
    private static void runSearchOperations(Mem0 mem0) throws Exception {
        System.out.println("4. Running search operations...");
        
        // 搜索相关记忆
        String query = "What do you know about my work?";
        List<Memory> workRelated = mem0.search(query, "demo-user");
        
        System.out.println("   Search query: \"" + query + "\"");
        System.out.println("   ✓ Found " + workRelated.size() + " related memories:");
        
        for (Memory memory : workRelated) {
            System.out.println("     - " + memory.getContent());
        }
        
        // 获取记忆历史
        String memoryId = workRelated.get(0).getId();
        List<Memory> history = mem0.getHistory(memoryId);
        System.out.println("   ✓ Memory history entries: " + history.size());
        
        System.out.println("   ✓ Search operations completed\n");
    }
}