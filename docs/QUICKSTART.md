# Quick Start Guide / 快速开始指南

This guide will help you get started with Mem0 Java in just a few minutes.

本指南将帮助您在几分钟内开始使用 Mem0 Java。

## Table of Contents / 目录

- [Installation](#installation--安装)
- [Basic Usage](#basic-usage--基本使用)
- [Hierarchical Memory](#hierarchical-memory--分层内存)
- [Advanced Search](#advanced-search--高级搜索)
- [Embedding Providers](#embedding-providers--嵌入提供者)
- [Configuration](#configuration--配置)
- [Multimodal Content](#multimodal-content--多模态内容)
- [AI Features](#ai-features--ai功能)
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
import com.mem0.core.Memory;
import java.util.List;
import java.util.Map;

public class BasicExample {
    public static void main(String[] args) {
        // Initialize Mem0 with default settings
        Mem0 mem0 = Mem0.builder()
            .withUserId("user123")
            .build();
        
        // Create and add a memory
        Memory memory = new Memory();
        memory.setContent("I love Italian food, especially pizza and pasta");
        memory.setMetadata(Map.of(
            "category", "preference", 
            "type", "food"
        ));
        
        // Add the memory
        String memoryId = mem0.add(memory).join();
        System.out.println("Added memory with ID: " + memoryId);
        
        // Search for memories
        List<Memory> results = mem0.search("What food do I like?", 5).join();
        
        // Display results
        System.out.println("Found " + results.size() + " memories:");
        for (Memory result : results) {
            System.out.println("- " + result.getContent());
        }
        
        // Get specific memory
        Memory retrievedMemory = mem0.get(memoryId).join();
        System.out.println("Retrieved: " + retrievedMemory.getContent());
        
        // Update memory
        retrievedMemory.setContent("I love Italian and Japanese food");
        mem0.update(memoryId, retrievedMemory).join();
        
        // Delete memory
        mem0.delete(memoryId).join();
        System.out.println("Memory deleted");
    }
}
```

### 2. Batch Operations / 批量操作

```java
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class BatchExample {
    public static void main(String[] args) {
        Mem0 mem0 = Mem0.builder()
            .withUserId("user123")
            .build();
        
        // Create multiple memories
        List<Memory> memories = Arrays.asList(
            new Memory("I work as a software developer"),
            new Memory("My favorite programming language is Java"),
            new Memory("I enjoy reading technical books"),
            new Memory("I live in San Francisco"),
            new Memory("I have a dog named Max")
        );
        
        // Add all memories concurrently
        List<CompletableFuture<String>> futures = memories.stream()
            .map(mem0::add)
            .collect(Collectors.toList());
        
        // Wait for all to complete
        List<String> memoryIds = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
        
        System.out.println("Added " + memoryIds.size() + " memories");
        
        // Search across all memories
        List<Memory> workResults = mem0.search("Tell me about my work", 3).join();
        List<Memory> personalResults = mem0.search("What personal information do you know?", 3).join();
        
        System.out.println("Work-related memories: " + workResults.size());
        System.out.println("Personal memories: " + personalResults.size());
    }
}
```

---

## Hierarchical Memory / 分层内存

The hierarchical memory system provides three levels: User, Session, and Agent.

分层内存系统提供三个级别：用户、会话和代理。

### 1. Three-Tier Memory System / 三层内存系统

```java
import com.mem0.hierarchy.MemoryHierarchyManager;
import com.mem0.hierarchy.UserMemory;
import com.mem0.hierarchy.SessionMemory;
import com.mem0.hierarchy.AgentMemory;

public class HierarchyExample {
    public static void main(String[] args) {
        MemoryHierarchyManager hierarchyManager = new MemoryHierarchyManager();
        
        String userId = "user123";
        String sessionId = "session456";  
        String agentId = "agent789";
        
        // User-level memory (persistent)
        Memory userMemory = new Memory("User prefers dark mode interface");
        userMemory.setMetadata(Map.of("type", "preference", "persistent", true));
        hierarchyManager.addMemory(userId, null, null, userMemory).join();
        
        // Session-level memory (temporary)
        Memory sessionMemory = new Memory("Currently working on project deployment");
        sessionMemory.setMetadata(Map.of("type", "context", "session", sessionId));
        hierarchyManager.addMemory(userId, sessionId, null, sessionMemory).join();
        
        // Agent-level memory (specialized)
        Memory agentMemory = new Memory("Code review checklist: security, performance, tests");
        agentMemory.setMetadata(Map.of("type", "knowledge", "domain", "development"));
        hierarchyManager.addMemory(userId, sessionId, agentId, agentMemory).join();
        
        // Query at different levels
        List<Memory> userMemories = hierarchyManager.getMemories(
            userId, null, null, "preferences", 5).join();
        List<Memory> sessionMemories = hierarchyManager.getMemories(
            userId, sessionId, null, "current work", 5).join();
        List<Memory> agentMemories = hierarchyManager.getMemories(
            userId, sessionId, agentId, "development knowledge", 5).join();
        
        System.out.println("User memories: " + userMemories.size());
        System.out.println("Session memories: " + sessionMemories.size());
        System.out.println("Agent memories: " + agentMemories.size());
    }
}
```

### 2. Memory Level Statistics / 内存级别统计

```java
public class HierarchyStatsExample {
    public static void main(String[] args) {
        MemoryHierarchyManager hierarchyManager = new MemoryHierarchyManager();
        
        // Get statistics for different levels
        Map<String, Object> userStats = hierarchyManager
            .getUserMemoryStatistics("user123").join();
        Map<String, Object> sessionStats = hierarchyManager
            .getSessionMemoryStatistics("session456").join();
        Map<String, Object> agentStats = hierarchyManager
            .getAgentMemoryStatistics("agent789").join();
        
        // Overall hierarchy statistics
        Map<String, Object> hierarchyStats = hierarchyManager
            .getHierarchyStatistics().join();
        
        System.out.println("Hierarchy Overview:");
        System.out.println("Total Users: " + hierarchyStats.get("totalUsers"));
        System.out.println("Total Sessions: " + hierarchyStats.get("totalSessions"));
        System.out.println("Total Agents: " + hierarchyStats.get("totalAgents"));
        
        // Cleanup expired sessions
        int cleanedSessions = hierarchyManager.cleanupExpiredSessions().join();
        System.out.println("Cleaned up " + cleanedSessions + " expired sessions");
    }
}
```

---

## Advanced Search / 高级搜索

### 1. Semantic Search / 语义搜索

```java
import com.mem0.search.SemanticSearchEngine;
import com.mem0.search.SearchResult;

public class SemanticSearchExample {
    public static void main(String[] args) {
        SemanticSearchEngine searchEngine = new SemanticSearchEngine();
        
        // Basic semantic search
        List<Memory> results = searchEngine.search(
            "artificial intelligence concepts", 10).join();
        
        // Search with scores
        List<SearchResult> resultsWithScores = searchEngine.searchWithScores(
            "machine learning algorithms", 10).join();
        
        System.out.println("Semantic search results:");
        for (SearchResult result : resultsWithScores) {
            System.out.printf("Score: %.3f - %s%n", 
                result.getScore(), result.getMemory().getContent());
        }
        
        // Search similar memories
        Memory referenceMemory = new Memory("Deep learning neural networks");
        List<Memory> similarMemories = searchEngine.searchSimilar(
            referenceMemory, 5).join();
        
        System.out.println("Found " + similarMemories.size() + " similar memories");
    }
}
```

### 2. Hybrid Search with Filters / 带过滤器的混合搜索

```java
import com.mem0.search.HybridSearchEngine;
import com.mem0.search.SearchFilter;
import java.time.LocalDateTime;

public class HybridSearchExample {
    public static void main(String[] args) {
        HybridSearchEngine hybridEngine = new HybridSearchEngine();
        
        // Create advanced search filter
        SearchFilter filter = SearchFilter.builder()
            .withTimeRange(
                LocalDateTime.now().minusDays(30), 
                LocalDateTime.now()
            )
            .withMetadata("category", "work")
            .withImportanceRange(0.7, 1.0)
            .withContentLength(10, 1000)
            .build();
        
        // Perform hybrid search with filter
        List<Memory> results = hybridEngine.search(
            "project management techniques", 10, filter).join();
        
        // Different search strategies
        List<Memory> semanticResults = hybridEngine.semanticSearch(
            "project management", 5).join();
        List<Memory> keywordResults = hybridEngine.keywordSearch(
            "project AND management", 5).join();
        List<Memory> fuzzyResults = hybridEngine.fuzzySearch(
            "projet managment", 5).join(); // Note: typos handled
        
        System.out.println("Hybrid results: " + results.size());
        System.out.println("Semantic results: " + semanticResults.size());
        System.out.println("Keyword results: " + keywordResults.size());
        System.out.println("Fuzzy results: " + fuzzyResults.size());
        
        // Get search suggestions
        List<String> suggestions = hybridEngine.getSuggestions(
            "machine lear", 5).join();
        System.out.println("Suggestions: " + suggestions);
    }
}
```

---

## Embedding Providers / 嵌入提供者

### 1. OpenAI Embedding Provider / OpenAI嵌入提供者

```java
import com.mem0.embedding.EmbeddingProvider;
import com.mem0.embedding.EmbeddingProviderFactory;

public class OpenAIExample {
    public static void main(String[] args) {
        // Create OpenAI provider
        EmbeddingProvider openaiProvider = EmbeddingProviderFactory
            .createOpenAI("your-openai-api-key");
        
        // Single text embedding
        List<Float> embedding = openaiProvider
            .embed("Hello, world!").join();
        
        System.out.println("OpenAI Embedding:");
        System.out.println("Dimension: " + openaiProvider.getDimension());
        System.out.println("Provider: " + openaiProvider.getProviderName());
        System.out.println("Vector length: " + embedding.size());
        System.out.println("First 5 values: " + embedding.subList(0, 5));
        
        // Batch embedding
        List<String> texts = Arrays.asList(
            "Artificial intelligence",
            "Machine learning",
            "Deep learning",
            "Natural language processing"
        );
        
        List<List<Float>> embeddings = openaiProvider
            .embedBatch(texts).join();
        
        System.out.println("Batch embeddings: " + embeddings.size());
        
        // Check health
        System.out.println("Provider healthy: " + openaiProvider.isHealthy());
        
        // Clean up
        openaiProvider.close();
    }
}
```

### 2. Aliyun Embedding Provider / 阿里云嵌入提供者

```java
public class AliyunExample {
    public static void main(String[] args) {
        // Create Aliyun provider (optimized for Chinese)
        EmbeddingProvider aliyunProvider = EmbeddingProviderFactory
            .createAliyun("your-aliyun-api-key");
        
        // Chinese text embedding
        List<Float> chineseEmbedding = aliyunProvider
            .embed("你好，世界！人工智能技术发展迅速。").join();
        
        System.out.println("阿里云嵌入向量:");
        System.out.println("维度: " + aliyunProvider.getDimension());
        System.out.println("提供者: " + aliyunProvider.getProviderName());
        System.out.println("向量长度: " + chineseEmbedding.size());
        
        // Mixed language batch
        List<String> mixedTexts = Arrays.asList(
            "Hello world",
            "你好世界",
            "機器學習",
            "自然语言处理"
        );
        
        List<List<Float>> mixedEmbeddings = aliyunProvider
            .embedBatch(mixedTexts).join();
        
        System.out.println("混合语言嵌入: " + mixedEmbeddings.size());
        
        aliyunProvider.close();
    }
}
```

### 3. Local TF-IDF Provider / 本地TF-IDF提供者

```java
public class LocalEmbeddingExample {
    public static void main(String[] args) {
        // Local TF-IDF provider (no API key needed)
        EmbeddingProvider tfidfProvider = EmbeddingProviderFactory.createTFIDF();
        
        // High-performance TF-IDF provider
        EmbeddingProvider highPerfProvider = EmbeddingProviderFactory
            .createHighPerformanceTFIDF();
        
        String text = "Machine learning is a subset of artificial intelligence";
        
        // Compare different local providers
        List<Float> tfidfEmbedding = tfidfProvider.embed(text).join();
        List<Float> highPerfEmbedding = highPerfProvider.embed(text).join();
        
        System.out.println("TF-IDF Embedding:");
        System.out.println("- Dimension: " + tfidfProvider.getDimension());
        System.out.println("- Provider: " + tfidfProvider.getProviderName());
        
        System.out.println("High-Performance TF-IDF:");
        System.out.println("- Dimension: " + highPerfProvider.getDimension());
        System.out.println("- Provider: " + highPerfProvider.getProviderName());
        
        // Clean up
        tfidfProvider.close();
        highPerfProvider.close();
    }
}
```

---

## Configuration / 配置

### 1. Basic Configuration / 基本配置

```java
import com.mem0.config.ConfigurationManager;
import com.mem0.config.EmbeddingConfiguration;

public class ConfigurationExample {
    public static void main(String[] args) {
        ConfigurationManager configManager = ConfigurationManager.getInstance();
        
        // Set global properties
        configManager.setGlobalProperty("embedding.provider.type", "openai");
        configManager.setGlobalProperty("embedding.openai.apiKey", "your-api-key");
        configManager.setGlobalProperty("embedding.openai.model", "text-embedding-ada-002");
        
        // Configure hierarchy settings
        configManager.setGlobalProperty("hierarchy.user.maxMemoriesPerUser", "50000");
        configManager.setGlobalProperty("hierarchy.session.timeoutMinutes", "60");
        configManager.setGlobalProperty("hierarchy.agent.retentionDays", "365");
        
        // Get embedding configuration
        EmbeddingConfiguration embeddingConfig = configManager.getEmbeddingConfiguration();
        
        System.out.println("Configuration loaded:");
        System.out.println("Provider type: " + embeddingConfig.getProviderType());
        System.out.println("OpenAI model: " + embeddingConfig.getOpenAIModel());
        System.out.println("Cache enabled: " + embeddingConfig.isCacheEnabled());
        System.out.println("Max cache size: " + embeddingConfig.getCacheMaxSize());
        
        // Validate all configurations
        Map<String, Boolean> validationResults = configManager.validateAllConfigurations();
        System.out.println("Configuration validation results: " + validationResults);
        
        // Export configuration
        String exportedConfig = configManager.exportToProperties();
        System.out.println("Exported configuration length: " + exportedConfig.length());
    }
}
```

### 2. Environment-based Configuration / 基于环境的配置

```java
import java.util.Properties;

public class EnvironmentConfigExample {
    public static void main(String[] args) {
        // Create configuration from properties
        Properties config = new Properties();
        config.setProperty("provider.type", "aliyun");
        config.setProperty("aliyun.apiKey", System.getenv("ALIYUN_API_KEY"));
        config.setProperty("aliyun.model", "text-embedding-v1");
        
        // Create provider from configuration
        EmbeddingProvider provider = EmbeddingProviderFactory.createFromConfig(config);
        
        System.out.println("Created provider: " + provider.getProviderName());
        
        // Initialize Mem0 with configuration
        Mem0 mem0 = Mem0.builder()
            .withUserId("user123")
            .withConfig(config)
            .build();
        
        // Test the configured system
        Memory testMemory = new Memory("Testing environment configuration");
        String memoryId = mem0.add(testMemory).join();
        
        List<Memory> results = mem0.search("configuration test", 5).join();
        System.out.println("Found " + results.size() + " memories");
        
        provider.close();
    }
}
```

---

## Multimodal Content / 多模态内容

### 1. Image Processing / 图像处理

```java
import com.mem0.multimodal.MultimodalMemoryProcessor;
import com.mem0.multimodal.ImageProcessor;
import com.mem0.multimodal.ProcessingResult;

public class ImageProcessingExample {
    public static void main(String[] args) {
        MultimodalMemoryProcessor processor = new MultimodalMemoryProcessor();
        ImageProcessor imageProcessor = new ImageProcessor();
        
        try {
            // Load image file
            byte[] imageData = Files.readAllBytes(Paths.get("example.jpg"));
            
            // Process image
            ProcessingResult result = processor.processImage(imageData).join();
            
            System.out.println("Image processing results:");
            System.out.println("Text extracted (OCR): " + result.getExtractedText());
            System.out.println("Features dimension: " + result.getFeatures().size());
            System.out.println("Detected objects: " + result.getMetadata().get("objects"));
            
            // Extract specific features
            List<Float> visualFeatures = imageProcessor
                .extractVisualFeatures(imageData).join();
            String ocrText = imageProcessor
                .extractTextFromImage(imageData).join();
            List<String> detectedObjects = imageProcessor
                .detectObjects(imageData).join();
            
            System.out.println("Visual features: " + visualFeatures.size());
            System.out.println("OCR text: " + ocrText);
            System.out.println("Objects: " + detectedObjects);
            
            // Create memory from processed image
            Memory imageMemory = new Memory();
            imageMemory.setContent(result.getExtractedText());
            imageMemory.setMetadata(Map.of(
                "type", "image",
                "objects", detectedObjects,
                "originalFormat", result.getMetadata().get("format")
            ));
            imageMemory.setEmbedding(result.getFeatures());
            
        } catch (IOException e) {
            System.err.println("Error processing image: " + e.getMessage());
        }
    }
}
```

### 2. Document Processing / 文档处理

```java
import com.mem0.multimodal.DocumentProcessor;

public class DocumentProcessingExample {
    public static void main(String[] args) {
        DocumentProcessor docProcessor = new DocumentProcessor();
        
        try {
            // Process PDF document
            byte[] pdfData = Files.readAllBytes(Paths.get("document.pdf"));
            ProcessingResult pdfResult = docProcessor
                .processDocument(pdfData, "pdf").join();
            
            System.out.println("PDF processing results:");
            System.out.println("Extracted text length: " + pdfResult.getExtractedText().length());
            System.out.println("Number of pages: " + pdfResult.getMetadata().get("pages"));
            
            // Extract structured content
            List<String> paragraphs = docProcessor
                .extractParagraphs(pdfData, "pdf").join();
            List<String> headings = docProcessor
                .extractHeadings(pdfData, "pdf").join();
            
            System.out.println("Paragraphs: " + paragraphs.size());
            System.out.println("Headings: " + headings.size());
            
            // Create memories for each paragraph
            for (int i = 0; i < paragraphs.size(); i++) {
                Memory paragraphMemory = new Memory();
                paragraphMemory.setContent(paragraphs.get(i));
                paragraphMemory.setMetadata(Map.of(
                    "type", "document_paragraph",
                    "source", "document.pdf",
                    "paragraph_index", i,
                    "extracted_at", LocalDateTime.now().toString()
                ));
                
                // Add to memory system
                // mem0.add(paragraphMemory).join();
            }
            
        } catch (IOException e) {
            System.err.println("Error processing document: " + e.getMessage());
        }
    }
}
```

---

## AI Features / AI功能

### 1. Memory Compression / 内存压缩

```java
import com.mem0.ai.MemoryCompressionEngine;
import com.mem0.ai.CompressionAnalysis;

public class CompressionExample {
    public static void main(String[] args) {
        MemoryCompressionEngine compressionEngine = new MemoryCompressionEngine();
        
        // Create sample memories
        List<Memory> memories = Arrays.asList(
            new Memory("I like pizza with pepperoni"),
            new Memory("Pizza is my favorite Italian food"),
            new Memory("I enjoy eating pizza on weekends"),
            new Memory("Italian cuisine, especially pizza, is delicious"),
            new Memory("Pepperoni pizza is the best type of pizza")
        );
        
        // Analyze compression potential
        CompressionAnalysis analysis = compressionEngine
            .analyzeCompressionPotential(memories).join();
        
        System.out.println("Compression Analysis:");
        System.out.println("Redundancy score: " + analysis.getRedundancyScore());
        System.out.println("Compression potential: " + analysis.getCompressionPotential());
        System.out.println("Recommended strategy: " + analysis.getRecommendedStrategy());
        
        // Perform semantic compression
        List<Memory> compressedMemories = compressionEngine
            .semanticCompression(memories, 0.85).join();
        
        System.out.println("Original memories: " + memories.size());
        System.out.println("Compressed memories: " + compressedMemories.size());
        
        // Generate summary
        String summary = compressionEngine.generateSummary(memories).join();
        System.out.println("Generated summary: " + summary);
        
        // Calculate compression ratio
        double compressionRatio = compressionEngine
            .calculateCompressionRatio(memories, compressedMemories).join();
        System.out.println("Compression ratio: " + compressionRatio);
    }
}
```

### 2. Adaptive Learning / 自适应学习

```java
import com.mem0.ai.AdaptiveLearningSystem;
import com.mem0.ai.UserProfile;
import com.mem0.ai.BehaviorPattern;

public class AdaptiveLearningExample {
    public static void main(String[] args) {
        AdaptiveLearningSystem learningSystem = new AdaptiveLearningSystem();
        String userId = "user123";
        
        // Record user behavior
        List<UserAction> actions = Arrays.asList(
            new UserAction("search", "machine learning", LocalDateTime.now()),
            new UserAction("add_memory", "Deep learning concepts", LocalDateTime.now()),
            new UserAction("search", "neural networks", LocalDateTime.now()),
            new UserAction("search", "artificial intelligence", LocalDateTime.now())
        );
        
        learningSystem.learnFromUserBehavior(userId, actions).join();
        
        // Get user profile
        UserProfile profile = learningSystem.getUserProfile(userId).join();
        
        System.out.println("User Profile:");
        System.out.println("Interests: " + profile.getInterests());
        System.out.println("Search patterns: " + profile.getSearchPatterns());
        System.out.println("Preferred topics: " + profile.getPreferredTopics());
        
        // Analyze behavior patterns
        List<BehaviorPattern> patterns = learningSystem
            .analyzeUserPatterns(userId).join();
        
        System.out.println("Detected behavior patterns:");
        for (BehaviorPattern pattern : patterns) {
            System.out.println("- " + pattern.getPatternType() + 
                              " (strength: " + pattern.getStrength() + ")");
        }
        
        // Generate recommendations
        List<String> recommendations = learningSystem
            .generateRecommendations(userId, 5).join();
        
        System.out.println("Personalized recommendations:");
        recommendations.forEach(rec -> System.out.println("- " + rec));
        
        // Predict user satisfaction
        List<Memory> searchResults = Arrays.asList(
            new Memory("Machine learning algorithms"),
            new Memory("Deep learning neural networks")
        );
        
        double predictedSatisfaction = learningSystem
            .predictUserSatisfaction(userId, "AI concepts", searchResults).join();
        
        System.out.println("Predicted user satisfaction: " + predictedSatisfaction);
    }
}
```

---

## Best Practices / 最佳实践

### 1. Resource Management / 资源管理

```java
public class ResourceManagementExample {
    public static void main(String[] args) {
        EmbeddingProvider provider = null;
        Mem0 mem0 = null;
        
        try {
            // Initialize resources
            provider = EmbeddingProviderFactory.createOpenAI("your-api-key");
            mem0 = Mem0.builder()
                .withUserId("user123")
                .withEmbeddingProvider("openai")
                .build();
            
            // Use resources
            Memory memory = new Memory("Important information to remember");
            String memoryId = mem0.add(memory).join();
            
            // Perform operations
            List<Memory> results = mem0.search("important", 5).join();
            System.out.println("Found " + results.size() + " memories");
            
        } finally {
            // Always clean up resources
            if (provider != null) {
                provider.close();
            }
            // Mem0 cleanup handled automatically
        }
        
        // Or use try-with-resources for automatic cleanup
        try (AutoCloseable providerResource = provider) {
            // Operations here
        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }
}
```

### 2. Error Handling / 错误处理

```java
import com.mem0.exception.MemoryServiceException;
import com.mem0.exception.EmbeddingException;

public class ErrorHandlingExample {
    public static void main(String[] args) {
        Mem0 mem0 = Mem0.builder()
            .withUserId("user123")
            .build();
        
        try {
            // Add memory with proper error handling
            Memory memory = new Memory("Test memory");
            String memoryId = mem0.add(memory)
                .exceptionally(throwable -> {
                    System.err.println("Failed to add memory: " + throwable.getMessage());
                    return null;
                })
                .join();
            
            if (memoryId != null) {
                System.out.println("Memory added successfully: " + memoryId);
            }
            
            // Search with error handling
            List<Memory> results = mem0.search("test query", 5)
                .handle((memories, throwable) -> {
                    if (throwable != null) {
                        System.err.println("Search failed: " + throwable.getMessage());
                        return Collections.emptyList();
                    }
                    return memories;
                })
                .join();
            
            System.out.println("Search completed with " + results.size() + " results");
            
        } catch (MemoryServiceException e) {
            System.err.println("Memory service error: " + e.getMessage());
        } catch (EmbeddingException e) {
            System.err.println("Embedding error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
        }
    }
}
```

### 3. Performance Optimization / 性能优化

```java
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class PerformanceExample {
    public static void main(String[] args) {
        Mem0 mem0 = Mem0.builder()
            .withUserId("user123")
            .withEmbeddingProvider("openai")
            .build();
        
        ExecutorService executor = Executors.newFixedThreadPool(10);
        
        try {
            // Batch operations for better performance
            List<String> contents = Arrays.asList(
                "Memory content 1",
                "Memory content 2", 
                "Memory content 3",
                "Memory content 4",
                "Memory content 5"
            );
            
            // Parallel memory addition
            List<CompletableFuture<String>> addFutures = contents.stream()
                .map(content -> CompletableFuture
                    .supplyAsync(() -> new Memory(content), executor)
                    .thenCompose(mem0::add))
                .collect(Collectors.toList());
            
            // Wait for all additions to complete
            CompletableFuture<List<String>> allAdds = CompletableFuture.allOf(
                addFutures.toArray(new CompletableFuture[0])
            ).thenApply(v -> addFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList())
            );
            
            List<String> memoryIds = allAdds.join();
            System.out.println("Added " + memoryIds.size() + " memories in parallel");
            
            // Parallel search operations
            List<String> queries = Arrays.asList("content 1", "content 2", "content 3");
            List<CompletableFuture<List<Memory>>> searchFutures = queries.stream()
                .map(query -> CompletableFuture
                    .supplyAsync(() -> mem0.search(query, 3).join(), executor))
                .collect(Collectors.toList());
            
            // Collect all search results
            CompletableFuture.allOf(searchFutures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    int totalResults = searchFutures.stream()
                        .mapToInt(future -> future.join().size())
                        .sum();
                    System.out.println("Total search results: " + totalResults);
                })
                .join();
            
        } finally {
            executor.shutdown();
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
            EmbeddingProvider provider = EmbeddingProviderFactory.createOpenAI(apiKey);
            
            // Test provider health
            if (!provider.isHealthy()) {
                System.err.println("Provider is not healthy. Check your API key.");
                return;
            }
            
            // Test with simple embedding
            List<Float> embedding = provider.embed("test").join();
            System.out.println("API key works! Embedding dimension: " + embedding.size());
            
            provider.close();
            
        } catch (Exception e) {
            System.err.println("API key validation failed: " + e.getMessage());
            
            if (e.getMessage().contains("401")) {
                System.out.println("Solution: Check if your API key is correct and has proper permissions");
            } else if (e.getMessage().contains("429")) {
                System.out.println("Solution: You've hit rate limits. Wait and try again or upgrade your plan");
            } else if (e.getMessage().contains("timeout")) {
                System.out.println("Solution: Check your internet connection and try again");
            }
        }
    }
}
```

#### 2. Memory Search Issues / 内存搜索问题

```java
public class SearchTroubleshooting {
    public static void main(String[] args) {
        Mem0 mem0 = Mem0.builder()
            .withUserId("user123")
            .build();
        
        // Add some test memories
        List<String> testContents = Arrays.asList(
            "Machine learning is a subset of AI",
            "Deep learning uses neural networks",
            "Natural language processing handles text",
            "Computer vision processes images"
        );
        
        List<String> memoryIds = testContents.stream()
            .map(content -> {
                try {
                    return mem0.add(new Memory(content)).join();
                } catch (Exception e) {
                    System.err.println("Failed to add memory: " + content);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        System.out.println("Added " + memoryIds.size() + " test memories");
        
        // Test different search approaches
        String query = "What is machine learning?";
        
        try {
            List<Memory> results = mem0.search(query, 5).join();
            
            if (results.isEmpty()) {
                System.out.println("No results found. Troubleshooting:");
                System.out.println("1. Check if memories were added successfully");
                System.out.println("2. Try simpler search terms");
                System.out.println("3. Verify embedding provider is working");
                
                // Try exact match search
                List<Memory> allMemories = mem0.getAll().join();
                System.out.println("Total memories in system: " + allMemories.size());
                
                // Try keyword-based search
                for (Memory memory : allMemories) {
                    if (memory.getContent().toLowerCase().contains("machine")) {
                        System.out.println("Found matching memory: " + memory.getContent());
                    }
                }
            } else {
                System.out.println("Search successful! Found " + results.size() + " results:");
                results.forEach(result -> System.out.println("- " + result.getContent()));
            }
            
        } catch (Exception e) {
            System.err.println("Search failed: " + e.getMessage());
            System.out.println("Possible solutions:");
            System.out.println("1. Check embedding provider configuration");
            System.out.println("2. Verify vector store connectivity");
            System.out.println("3. Check memory index status");
        }
    }
}
```

#### 3. Configuration Issues / 配置问题

```java
public class ConfigurationTroubleshooting {
    public static void main(String[] args) {
        try {
            ConfigurationManager configManager = ConfigurationManager.getInstance();
            
            // Validate all configurations
            Map<String, Boolean> validation = configManager.validateAllConfigurations();
            
            System.out.println("Configuration validation results:");
            validation.forEach((module, isValid) -> {
                System.out.println(module + ": " + (isValid ? "✓ Valid" : "✗ Invalid"));
            });
            
            // Check specific configuration issues
            EmbeddingConfiguration embeddingConfig = configManager.getEmbeddingConfiguration();
            
            if (embeddingConfig.getProviderType().equals("openai")) {
                if (embeddingConfig.getOpenAIApiKey().isEmpty()) {
                    System.err.println("Issue: OpenAI API key not configured");
                    System.out.println("Solution: Set mem0.embedding.openai.apiKey property");
                }
            }
            
            if (embeddingConfig.getProviderType().equals("aliyun")) {
                if (embeddingConfig.getAliyunApiKey().isEmpty()) {
                    System.err.println("Issue: Aliyun API key not configured");
                    System.out.println("Solution: Set mem0.embedding.aliyun.apiKey property");
                }
            }
            
            // Check cache settings
            if (!embeddingConfig.isCacheEnabled()) {
                System.out.println("Note: Embedding cache is disabled. This may impact performance.");
            }
            
            // Export current configuration for debugging
            String configExport = configManager.exportToProperties();
            System.out.println("Current configuration exported (" + configExport.length() + " characters)");
            
        } catch (Exception e) {
            System.err.println("Configuration error: " + e.getMessage());
            System.out.println("Check your configuration files and environment variables");
        }
    }
}
```

### Getting Help / 获取帮助

If you encounter issues not covered in this guide:

1. **Check the logs** - Enable debug logging for detailed information
2. **Review the API documentation** - Check [API.md](API.md) for detailed method documentation  
3. **Search existing issues** - Look through [GitHub Issues](https://github.com/mem0ai/mem0-java/issues)
4. **Ask the community** - Post in [GitHub Discussions](https://github.com/mem0ai/mem0-java/discussions)
5. **Contact support** - Email [support@mem0.ai](mailto:support@mem0.ai)

如果您遇到本指南未涵盖的问题：

1. **检查日志** - 启用调试日志以获取详细信息
2. **查看API文档** - 查看 [API.md](API.md) 了解详细的方法文档
3. **搜索现有问题** - 浏览 [GitHub Issues](https://github.com/mem0ai/mem0-java/issues)
4. **询问社区** - 在 [GitHub Discussions](https://github.com/mem0ai/mem0-java/discussions) 发帖
5. **联系支持** - 发送邮件至 [support@mem0.ai](mailto:support@mem0.ai)

---

## Next Steps / 下一步

Now that you've completed the quick start guide, you can:

完成快速开始指南后，您可以：

1. **Explore Advanced Features** - Try multimodal processing, AI features, and advanced search
2. **Integration Examples** - Look at the [examples directory](../src/test/java/com/mem0/examples/)
3. **Performance Tuning** - Optimize your configuration for your specific use case
4. **Production Deployment** - Review security and scalability considerations
5. **Contribute** - Help improve Mem0 Java by contributing code or documentation

1. **探索高级功能** - 尝试多模态处理、AI功能和高级搜索
2. **集成示例** - 查看[示例目录](../src/test/java/com/mem0/examples/)
3. **性能调优** - 针对您的具体用例优化配置
4. **生产部署** - 审查安全性和可扩展性考虑
5. **贡献代码** - 通过贡献代码或文档帮助改进Mem0 Java

Happy coding with Mem0 Java! 🚀