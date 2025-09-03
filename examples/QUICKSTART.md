# Quick Start Guide / 快速开始指南

This guide will help you get started with Mem0 Java in just a few minutes.

本指南将帮助您在几分钟内开始使用 Mem0 Java。

## Table of Contents / 目录

- [Installation](#installation--安装)
- [Basic Usage](#basic-usage--基本使用)
- [Configuration](#configuration--配置)
- [Example Projects](#example-projects--示例项目)
- [Spring Boot Integration](#spring-boot-integration--spring-boot集成)
- [Custom Providers](#custom-providers--自定义提供者)
- [Best Practices](#best-practices--最佳实践)
- [Troubleshooting](#troubleshooting--故障排除)

---

## Installation / 安装

### Prerequisites / 前置要求

- **Java 8+** (OpenJDK or Oracle JDK)
- **Maven 3.6+** or **Gradle 6.0+**

### Maven Setup

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>com.mem0</groupId>
    <artifactId>mem0-java</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle Setup

Add to your `build.gradle`:

```gradle
implementation 'com.mem0:mem0-java:1.0.0'
```

---

## Basic Usage / 基本使用

### 1. Simple Memory Operations / 简单的内存操作

```java
import com.mem0.Mem0;
import com.mem0.config.Mem0Configuration;
import com.mem0.core.Memory;
import com.mem0.embedding.impl.MockEmbeddingProvider;
import com.mem0.llm.MockLLMProvider;

public class QuickStartExample {
    public static void main(String[] args) throws Exception {
        // Create configuration with mock providers for testing
        Mem0Configuration config = new Mem0Configuration();
        config.setEmbeddingProvider(new MockEmbeddingProvider());
        config.setLlmProvider(new MockLLMProvider());
        
        // Initialize Mem0
        Mem0 mem0 = new Mem0(config);
        
        // Add memories
        String memory1 = mem0.add("I love drinking coffee in the morning", "user123");
        String memory2 = mem0.add("I work as a software developer", "user123");
        String memory3 = mem0.add("My favorite programming language is Java", "user123");
        
        System.out.println("Added memories: " + memory1 + ", " + memory2 + ", " + memory3);
        
        // Search for memories
        List<Memory> results = mem0.search("What do I like to drink?", "user123");
        System.out.println("Found " + results.size() + " memories");
        
        // Display results
        for (Memory memory : results) {
            System.out.println("- " + memory.getContent());
        }
        
        // Get all memories
        List<Memory> allMemories = mem0.getAll("user123");
        System.out.println("Total memories: " + allMemories.size());
        
        // Update a memory
        mem0.update(memory1, "I love drinking espresso in the morning", "user123");
        System.out.println("Updated memory");
        
        // Delete a memory
        mem0.delete(memory3, "user123");
        System.out.println("Deleted memory");
        
        // Close connection
        mem0.close();
    }
}
```

### 2. Batch Operations / 批量操作

```java
import java.util.Arrays;
import java.util.List;

public class BatchOperationsExample {
    public static void main(String[] args) throws Exception {
        Mem0Configuration config = new Mem0Configuration();
        config.setEmbeddingProvider(new MockEmbeddingProvider());
        config.setLlmProvider(new MockLLMProvider());
        
        Mem0 mem0 = new Mem0(config);
        
        // Batch add memories
        List<String> contents = Arrays.asList(
            "I enjoy reading science fiction books",
            "My favorite author is Isaac Asimov",
            "I prefer physical books over ebooks",
            "I usually read in the evening before bed"
        );
        
        System.out.println("Adding memories in batch...");
        for (String content : contents) {
            String memoryId = mem0.add(content, "user123");
            System.out.println("Added: " + memoryId);
        }
        
        // Search with different queries
        String[] queries = {
            "What books do I like?",
            "When do I read?",
            "Who is my favorite author?"
        };
        
        for (String query : queries) {
            List<Memory> results = mem0.search(query, "user123");
            System.out.println(query + " -> " + results.size() + " results");
        }
        
        mem0.close();
    }
}
```

---

## Configuration / 配置

### 1. Environment Variables / 环境变量

Set these environment variables for external services:

```bash
export OPENAI_API_KEY="your-openai-api-key"
export ANTHROPIC_API_KEY="your-anthropic-api-key"
export PINECONE_API_KEY="your-pinecone-api-key"
export PINECONE_ENVIRONMENT="your-pinecone-environment"
```

### 2. Configuration with Real Providers / 使用真实提供者的配置

```java
import com.mem0.embedding.impl.OpenAIEmbeddingProvider;
import com.mem0.llm.OpenAIProvider;

public class ConfigurationExample {
    public static void main(String[] args) throws Exception {
        // Create configuration with real providers
        Mem0Configuration config = new Mem0Configuration();
        
        // Set OpenAI providers
        config.setEmbeddingProvider(new OpenAIEmbeddingProvider(
            System.getenv("OPENAI_API_KEY"),
            "text-embedding-ada-002"
        ));
        config.setLlmProvider(new OpenAIProvider(
            System.getenv("OPENAI_API_KEY"),
            "gpt-4",
            0.7,
            1000
        ));
        
        Mem0 mem0 = new Mem0(config);
        
        // Now you can use real AI-powered memory operations
        String memoryId = mem0.add("I'm learning about artificial intelligence", "user123");
        List<Memory> results = mem0.search("AI knowledge", "user123");
        
        System.out.println("Using real AI providers - Found " + results.size() + " memories");
        
        mem0.close();
    }
}
```

---

## Example Projects / 示例项目

### Available Examples / 可用示例

The examples directory contains comprehensive examples for various integration scenarios:

examples目录包含各种集成场景的综合示例：

#### 1. [Basic Usage](basic/BasicUsageExample.java) / 基础使用
- Memory CRUD operations / 内存增删改查操作
- Search functionality / 搜索功能
- Batch processing / 批处理
- Error handling / 错误处理

#### 2. [Spring Boot Integration](spring/SpringBootIntegrationExample.java) / Spring Boot集成
- REST API endpoints / REST API端点
- Configuration management / 配置管理
- Dependency injection / 依赖注入
- Application properties / 应用属性配置

#### 3. [Custom LLM Providers](custom/CustomLLMProviderExample.java) / 自定义LLM提供者
- Claude LLM integration / Claude LLM集成
- Gemini LLM integration / Gemini LLM集成
- Local LLM support (Ollama) / 本地LLM支持
- Enterprise failover patterns / 企业级故障转移模式

#### 4. [Vector Databases](vector/VectorDatabaseExample.java) / 向量数据库
- Pinecone integration / Pinecone集成
- Weaviate connection / Weaviate连接
- Qdrant setup / Qdrant设置
- Custom vector stores / 自定义向量存储

#### 5. [Graph Databases](graph/GraphDatabaseExample.java) / 图数据库
- Neo4j with Cypher queries / Neo4j与Cypher查询
- ArangoDB multi-model / ArangoDB多模型
- Amazon Neptune / Amazon Neptune
- In-memory graph storage / 内存图存储

#### 6. [Redis Integration](redis/RedisIntegrationExample.java) / Redis集成
- Caching strategies / 缓存策略
- Session management / 会话管理
- Pub/Sub messaging / 发布订阅消息
- Distributed locking / 分布式锁
- Cluster support / 集群支持

### Running Examples / 运行示例

```bash
# Compile the project
mvn clean compile

# Run basic example
java -cp target/classes examples.basic.BasicUsageExample

# Run Spring Boot example
java -cp target/classes examples.spring.SpringBootIntegrationExample

# Run vector database example
java -cp target/classes examples.vector.VectorDatabaseExample
```

---

## Spring Boot Integration / Spring Boot集成

### 1. Application Configuration / 应用配置

Create `application.yml`:

```yaml
mem0:
  llm:
    provider: openai
    api-key: ${OPENAI_API_KEY:your-openai-api-key}
    model: gpt-4
    temperature: 0.7
    max-tokens: 1000
  
  embedding:
    provider: openai
    model: text-embedding-ada-002
    batch-size: 100
    cache-enabled: true
  
  vector:
    provider: inmemory  # or pinecone, weaviate, qdrant
  
  graph:
    provider: inmemory  # or neo4j, arangodb, neptune
  
  cache:
    provider: inmemory  # or redis
    ttl: 3600

spring:
  application:
    name: mem0-spring-app

server:
  port: 8080
  servlet:
    context-path: /mem0
```

### 2. REST Controller / REST控制器

```java
@RestController
@RequestMapping("/api/memory")
public class MemoryController {
    
    @Autowired
    private Mem0 mem0;
    
    @PostMapping
    public ResponseEntity<String> addMemory(@RequestBody MemoryRequest request) {
        try {
            String memoryId = mem0.add(request.getContent(), request.getUserId());
            return ResponseEntity.ok(memoryId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to add memory: " + e.getMessage());
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<Memory>> searchMemories(
            @RequestParam String query,
            @RequestParam String userId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Memory> results = mem0.search(query, userId, limit);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Memory>> getAllMemories(@PathVariable String userId) {
        try {
            List<Memory> memories = mem0.getAll(userId);
            return ResponseEntity.ok(memories);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{memoryId}")
    public ResponseEntity<String> updateMemory(
            @PathVariable String memoryId,
            @RequestBody MemoryRequest request) {
        try {
            mem0.update(memoryId, request.getContent(), request.getUserId());
            return ResponseEntity.ok("Memory updated successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update memory: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/{memoryId}")
    public ResponseEntity<String> deleteMemory(
            @PathVariable String memoryId,
            @RequestParam String userId) {
        try {
            mem0.delete(memoryId, userId);
            return ResponseEntity.ok("Memory deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete memory: " + e.getMessage());
        }
    }
}
```

---

## Custom Providers / 自定义提供者

### 1. Custom LLM Provider / 自定义LLM提供者

```java
import com.mem0.llm.LLMProvider;
import com.mem0.llm.LLMResponse;

public class CustomLLMProvider implements LLMProvider {
    private final String apiKey;
    private final HttpClient httpClient;
    
    public CustomLLMProvider(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }
    
    @Override
    public CompletableFuture<LLMResponse> generateChatCompletion(
            List<ChatMessage> messages, LLMConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            // Your custom LLM implementation
            // Call your preferred LLM service API
            
            try {
                // Build request
                String requestBody = buildRequestBody(messages, config);
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.your-llm-service.com/v1/chat"))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
                
                // Parse response
                return parseResponse(response.body());
                
            } catch (Exception e) {
                throw new RuntimeException("LLM call failed", e);
            }
        });
    }
    
    private String buildRequestBody(List<ChatMessage> messages, LLMConfig config) {
        // Implement request body building logic
        return "{}";
    }
    
    private LLMResponse parseResponse(String responseBody) {
        // Implement response parsing logic
        return new LLMResponse("Generated text", 100, "custom-model", "stop");
    }
}
```

### 2. Custom Vector Store / 自定义向量存储

```java
import com.mem0.store.VectorStore;

public class CustomVectorStore implements VectorStore {
    
    @Override
    public CompletableFuture<String> insertVector(String id, List<Float> embedding, 
                                                 Map<String, Object> metadata, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            // Your custom vector storage implementation
            // Store vector in your preferred vector database
            
            try {
                // Store vector logic here
                storeVector(id, embedding, metadata, userId);
                return id;
            } catch (Exception e) {
                throw new RuntimeException("Vector insertion failed", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<SearchResult>> searchVectors(List<Float> queryEmbedding, 
                                                              String userId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            // Your custom vector search implementation
            // Search similar vectors in your database
            
            try {
                return performVectorSearch(queryEmbedding, userId, limit);
            } catch (Exception e) {
                throw new RuntimeException("Vector search failed", e);
            }
        });
    }
    
    // Implement other required methods...
    
    private void storeVector(String id, List<Float> embedding, 
                           Map<String, Object> metadata, String userId) {
        // Custom storage logic
    }
    
    private List<SearchResult> performVectorSearch(List<Float> queryEmbedding, 
                                                  String userId, int limit) {
        // Custom search logic
        return Collections.emptyList();
    }
}
```

---

## Best Practices / 最佳实践

### 1. Resource Management / 资源管理

```java
public class ResourceManagementExample {
    public static void main(String[] args) {
        Mem0 mem0 = null;
        
        try {
            // Initialize resources
            Mem0Configuration config = new Mem0Configuration();
            config.setEmbeddingProvider(new MockEmbeddingProvider());
            config.setLlmProvider(new MockLLMProvider());
            
            mem0 = new Mem0(config);
            
            // Use resources
            String memoryId = mem0.add("Important information", "user123");
            List<Memory> results = mem0.search("important", "user123");
            
            System.out.println("Found " + results.size() + " memories");
            
        } finally {
            // Always clean up resources
            if (mem0 != null) {
                try {
                    mem0.close();
                } catch (Exception e) {
                    System.err.println("Error during cleanup: " + e.getMessage());
                }
            }
        }
    }
}
```

### 2. Error Handling / 错误处理

```java
public class ErrorHandlingExample {
    public static void main(String[] args) {
        try {
            Mem0Configuration config = new Mem0Configuration();
            config.setEmbeddingProvider(new MockEmbeddingProvider());
            config.setLlmProvider(new MockLLMProvider());
            
            Mem0 mem0 = new Mem0(config);
            
            // Add memory with error handling
            try {
                String memoryId = mem0.add("Test memory", "user123");
                System.out.println("Memory added successfully: " + memoryId);
            } catch (Exception e) {
                System.err.println("Failed to add memory: " + e.getMessage());
                // Handle specific error cases
                if (e.getMessage().contains("rate limit")) {
                    System.out.println("Rate limited. Retrying after delay...");
                    Thread.sleep(1000);
                    // Retry logic here
                }
            }
            
            // Search with error handling
            try {
                List<Memory> results = mem0.search("test query", "user123");
                System.out.println("Search completed with " + results.size() + " results");
            } catch (Exception e) {
                System.err.println("Search failed: " + e.getMessage());
                // Provide fallback behavior
                System.out.println("Using cached results or default response");
            }
            
            mem0.close();
            
        } catch (Exception e) {
            System.err.println("Initialization error: " + e.getMessage());
        }
    }
}
```

### 3. Performance Optimization / 性能优化

```java
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PerformanceExample {
    public static void main(String[] args) {
        Mem0Configuration config = new Mem0Configuration();
        config.setEmbeddingProvider(new MockEmbeddingProvider());
        config.setLlmProvider(new MockLLMProvider());
        
        Mem0 mem0 = new Mem0(config);
        ExecutorService executor = Executors.newFixedThreadPool(5);
        
        try {
            // Parallel memory operations
            List<String> contents = Arrays.asList(
                "Memory 1", "Memory 2", "Memory 3", "Memory 4", "Memory 5"
            );
            
            // Add memories in parallel
            List<CompletableFuture<String>> addFutures = contents.stream()
                    .map(content -> CompletableFuture
                            .supplyAsync(() -> {
                                try {
                                    return mem0.add(content, "user123");
                                } catch (Exception e) {
                                    System.err.println("Failed to add: " + content);
                                    return null;
                                }
                            }, executor))
                    .collect(Collectors.toList());
            
            // Wait for all to complete
            CompletableFuture<Void> allAdds = CompletableFuture.allOf(
                addFutures.toArray(new CompletableFuture[0]));
            
            allAdds.join();
            
            List<String> memoryIds = addFutures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            System.out.println("Added " + memoryIds.size() + " memories in parallel");
            
            // Parallel search operations
            List<String> queries = Arrays.asList("memory 1", "memory 2", "memory 3");
            
            List<CompletableFuture<List<Memory>>> searchFutures = queries.stream()
                    .map(query -> CompletableFuture
                            .supplyAsync(() -> {
                                try {
                                    return mem0.search(query, "user123");
                                } catch (Exception e) {
                                    System.err.println("Search failed for: " + query);
                                    return Collections.emptyList();
                                }
                            }, executor))
                    .collect(Collectors.toList());
            
            CompletableFuture<Void> allSearches = CompletableFuture.allOf(
                searchFutures.toArray(new CompletableFuture[0]));
            
            allSearches.join();
            
            int totalResults = searchFutures.stream()
                    .mapToInt(future -> future.join().size())
                    .sum();
            
            System.out.println("Total search results: " + totalResults);
            
        } finally {
            executor.shutdown();
            mem0.close();
        }
    }
}
```

---

## Troubleshooting / 故障排除

### Common Issues / 常见问题

#### 1. API Key Issues / API密钥问题

```java
public class ApiKeyTroubleshooting {
    public static void main(String[] args) {
        String apiKey = System.getenv("OPENAI_API_KEY");
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.err.println("Error: OPENAI_API_KEY environment variable not set");
            System.out.println("Please set your API key:");
            System.out.println("export OPENAI_API_KEY=your-api-key-here");
            return;
        }
        
        try {
            // Test configuration
            Mem0Configuration config = new Mem0Configuration();
            config.setEmbeddingProvider(new OpenAIEmbeddingProvider(apiKey, "text-embedding-ada-002"));
            config.setLlmProvider(new OpenAIProvider(apiKey, "gpt-3.5-turbo", 0.7, 1000));
            
            Mem0 mem0 = new Mem0(config);
            
            // Test basic operation
            String memoryId = mem0.add("Test memory", "user123");
            System.out.println("API key works! Memory ID: " + memoryId);
            
            mem0.close();
            
        } catch (Exception e) {
            System.err.println("API key validation failed: " + e.getMessage());
            
            if (e.getMessage().contains("401")) {
                System.out.println("Solution: Check if your API key is correct");
            } else if (e.getMessage().contains("429")) {
                System.out.println("Solution: You've hit rate limits. Wait and try again");
            } else if (e.getMessage().contains("timeout")) {
                System.out.println("Solution: Check your internet connection");
            }
        }
    }
}
```

#### 2. Memory Search Issues / 内存搜索问题

```java
public class SearchTroubleshooting {
    public static void main(String[] args) {
        Mem0Configuration config = new Mem0Configuration();
        config.setEmbeddingProvider(new MockEmbeddingProvider());
        config.setLlmProvider(new MockLLMProvider());
        
        Mem0 mem0 = new Mem0(config);
        
        try {
            // Add test memories
            String memory1 = mem0.add("I love coffee", "user123");
            String memory2 = mem0.add("I work in tech", "user123");
            String memory3 = mem0.add("I enjoy programming", "user123");
            
            System.out.println("Added test memories");
            
            // Test search
            List<Memory> results = mem0.search("What do I love?", "user123");
            
            if (results.isEmpty()) {
                System.out.println("No results found. Troubleshooting:");
                System.out.println("1. Check if memories were added successfully");
                System.out.println("2. Try simpler search terms");
                System.out.println("3. Verify embedding provider is working");
                
                // Debug: Get all memories
                List<Memory> allMemories = mem0.getAll("user123");
                System.out.println("Total memories in system: " + allMemories.size());
                
                for (Memory memory : allMemories) {
                    System.out.println("- " + memory.getContent());
                }
            } else {
                System.out.println("Search successful! Found " + results.size() + " results:");
                for (Memory result : results) {
                    System.out.println("- " + result.getContent());
                }
            }
            
        } catch (Exception e) {
            System.err.println("Search failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            mem0.close();
        }
    }
}
```

### Getting Help / 获取帮助

If you encounter issues not covered in this guide:

如果您遇到本指南未涵盖的问题：

1. **Check the logs** - Enable debug logging for detailed information
2. **Review example projects** - Look at the [examples directory](.) for similar use cases
3. **Search existing issues** - Look through GitHub Issues
4. **Ask the community** - Post in GitHub Discussions
5. **Contact support** - Email support

1. **检查日志** - 启用调试日志以获取详细信息
2. **查看示例项目** - 查看[示例目录](.)了解类似用例
3. **搜索现有问题** - 浏览GitHub Issues
4. **询问社区** - 在GitHub Discussions发帖
5. **联系支持** - 发送邮件联系支持

---

## Next Steps / 下一步

Now that you've completed the quick start guide, you can:

完成快速开始指南后，您可以：

1. **Explore Example Projects** - Try the various examples in this directory
2. **Spring Boot Integration** - Build REST APIs with [spring/SpringBootIntegrationExample.java](spring/SpringBootIntegrationExample.java)
3. **Custom Integrations** - Implement custom providers using the examples in [custom/](custom/)
4. **Vector Databases** - Connect to external vector databases with [vector/](vector/) examples
5. **Graph Databases** - Use graph databases with [graph/](graph/) examples
6. **Redis Integration** - Implement caching and session management with [redis/](redis/) examples

1. **探索示例项目** - 尝试此目录中的各种示例
2. **Spring Boot集成** - 使用[spring/SpringBootIntegrationExample.java](spring/SpringBootIntegrationExample.java)构建REST API
3. **自定义集成** - 使用[custom/](custom/)中的示例实现自定义提供者
4. **向量数据库** - 使用[vector/](vector/)示例连接外部向量数据库
5. **图数据库** - 使用[graph/](graph/)示例使用图数据库
6. **Redis集成** - 使用[redis/](redis/)示例实现缓存和会话管理

Happy coding with Mem0 Java! 🚀