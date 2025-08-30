# Mem0 Java Documentation

Welcome to the Mem0 Java documentation! This directory contains comprehensive guides, examples, and references for using Mem0 Java.

## 📚 Documentation Structure

```
docs/
├── README.md                    # This file - documentation overview
├── QUICKSTART.md               # Quick start guide for new users
└── examples/                   # Code examples and tutorials
    ├── basic/                  # Basic usage examples
    ├── advanced/               # Advanced feature examples
    ├── integration/            # Integration examples
    └── configuration/          # Configuration examples
```

## 🚀 Getting Started

If you're new to Mem0 Java, start here:

1. **[Quick Start Guide](QUICKSTART.md)** - Get up and running in minutes
2. **[Basic Examples](examples/basic/)** - Simple usage patterns
3. **[Configuration Guide](examples/configuration/)** - Set up your system
4. **[Advanced Examples](examples/advanced/)** - Explore powerful features

## 📖 Available Documentation

### Core Documentation
- **[Quick Start Guide](QUICKSTART.md)** - Complete getting started tutorial with examples
- **[Main README](../README.md)** - Project overview, installation, and basic usage

### Code Examples
All examples are located in the source code under:
- **[Test Examples](../src/test/java/com/mem0/examples/)** - Runnable example code
- **[Integration Tests](../src/test/java/com/mem0/integration/)** - Full system integration examples

## 🎯 Documentation by Feature

### Core Features
- **Memory Operations** - Add, search, update, delete memories
- **Hierarchical Memory** - User/Session/Agent three-tier system
- **Search System** - Semantic, hybrid, and filtered search
- **Configuration** - Comprehensive configuration management

### Advanced Features  
- **Embedding Providers** - OpenAI, Aliyun, and local providers
- **Multimodal Processing** - Image, document, audio, video support
- **AI Features** - Memory compression and adaptive learning
- **Enterprise Features** - Health monitoring, metrics, caching

## 📋 Quick Reference

### Key Classes
- `Mem0` - Main entry point and primary API
- `Memory` - Core data structure for memory entries
- `MemoryHierarchyManager` - Three-tier memory management
- `EmbeddingProviderFactory` - Create and manage embedding providers
- `ConfigurationManager` - Centralized configuration management

### Configuration Files
- `application.properties` - Main application configuration
- `mem0/hierarchy.properties` - Memory hierarchy settings
- `mem0/search.properties` - Search system configuration
- `mem0/embedding.properties` - Embedding provider settings
- `mem0/multimodal.properties` - Multimodal processing settings
- `mem0/ai.properties` - AI features configuration

## 🔧 Usage Patterns

### Basic Pattern
```java
// Initialize
Mem0 mem0 = Mem0.builder()
    .withUserId("user123")
    .build();

// Add memory
Memory memory = new Memory("Important information");
String id = mem0.add(memory).join();

// Search
List<Memory> results = mem0.search("important", 5).join();
```

### Advanced Pattern
```java
// Hierarchical memory
MemoryHierarchyManager hierarchy = new MemoryHierarchyManager();
hierarchy.addMemory("user123", "session456", "agent789", memory);

// Custom embedding provider
EmbeddingProvider provider = EmbeddingProviderFactory
    .createOpenAI("api-key");

// Advanced search
SearchFilter filter = SearchFilter.builder()
    .withTimeRange(start, end)
    .withMetadata("category", "important")
    .build();
```

## 🏗️ Architecture Overview

```
Application Layer
├── Mem0 (Main API)
├── MemoryHierarchyManager
└── Configuration Management

Search & AI Layer  
├── SemanticSearchEngine
├── HybridSearchEngine
├── MemoryCompressionEngine
└── AdaptiveLearningSystem

Provider Layer
├── EmbeddingProviders (OpenAI, Aliyun, TF-IDF)
├── MultimodalProcessors
└── Storage Backends
```

## 💡 Best Practices

### Resource Management
```java
// Always clean up resources
try (EmbeddingProvider provider = factory.create()) {
    // Use provider
} // Automatically closed

// Or manually
provider.close();
EmbeddingProviderFactory.closeAll();
```

### Error Handling
```java
// Handle async operations properly
mem0.add(memory)
    .exceptionally(throwable -> {
        logger.error("Failed to add memory", throwable);
        return null;
    })
    .join();
```

### Performance Optimization
```java
// Use batch operations
List<CompletableFuture<String>> futures = memories.stream()
    .map(mem0::add)
    .collect(Collectors.toList());

CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
```

## 🚨 Common Issues

### Configuration Problems
- Ensure API keys are properly set in environment variables
- Check that configuration files are in the classpath
- Validate configuration using `ConfigurationManager.validateAllConfigurations()`

### Performance Issues
- Enable caching for better performance
- Use batch operations for multiple items  
- Consider high-performance providers for local processing

### Search Problems
- Verify embedding provider is working correctly
- Check that memories have been properly indexed
- Try different search strategies (semantic vs. keyword vs. fuzzy)

## 🔗 External Resources

- **[GitHub Repository](https://github.com/mem0ai/mem0-java)** - Source code and issues
- **[Maven Central](https://central.sonatype.com/search?q=mem0-java)** - Latest releases
- **[Python Mem0](https://github.com/mem0ai/mem0)** - Original Python implementation
- **[OpenAI API](https://platform.openai.com/docs/api-reference/embeddings)** - OpenAI embedding documentation
- **[Aliyun DashScope](https://help.aliyun.com/zh/dashscope/)** - Aliyun embedding documentation

## 📞 Support

Need help? Here are your options:

1. **Documentation** - Check this documentation first
2. **Examples** - Look at the code examples in the repository
3. **GitHub Issues** - Search existing issues or create a new one
4. **GitHub Discussions** - Ask questions and share ideas with the community
5. **Email Support** - Contact support@mem0.ai for enterprise support

## 🤝 Contributing

We welcome contributions to the documentation!

- **Improve Existing Docs** - Fix typos, add clarity, update examples
- **Add New Examples** - Share your use cases and implementations
- **Translate Content** - Help make documentation available in more languages
- **Report Issues** - Let us know about missing or unclear documentation

## 📝 License

This documentation is part of the Mem0 Java project and is licensed under the Apache License 2.0. See [LICENSE](../LICENSE) for details.

---

**Happy coding with Mem0 Java!** 🧠✨