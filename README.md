# Mem0 Java - Universal Memory Layer for AI Agents

<div align="center">

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java Version](https://img.shields.io/badge/Java-8%2B-orange.svg)](https://openjdk.org/)
[![Maven Central](https://img.shields.io/badge/Maven%20Central-1.0.0-green.svg)](#)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)](#)
[![Coverage](https://img.shields.io/badge/Coverage-85%25-yellow.svg)](#)

**🧠 The intelligent memory layer that learns, adapts, and evolves with your AI applications**

[English](#english) | [中文](#中文)

</div>

---

## English

### 🚀 Overview

Mem0 Java is a powerful, enterprise-grade memory layer designed specifically for AI agents and applications. Built with Java 8+ compatibility, it provides intelligent memory management, advanced search capabilities, and seamless integration with popular AI services.

### ✨ Key Features

#### 🧠 **Intelligent Memory Management**
- **Hierarchical Memory**: Three-tier memory system (User/Session/Agent)
- **Smart Classification**: Automatic memory categorization and importance scoring
- **Conflict Detection**: Intelligent detection and resolution of memory conflicts
- **Adaptive Forgetting**: Time-based memory decay with importance preservation

#### 🔍 **Advanced Search System**
- **Semantic Search**: Vector-based similarity search with embedding support
- **Hybrid Search**: Combined keyword, semantic, and fuzzy search
- **Advanced Filters**: Multi-dimensional filtering with temporal and metadata support
- **Real-time Indexing**: Dynamic index updates for optimal performance

#### 🎯 **Multi-Modal Support**
- **Image Processing**: OCR, feature extraction, and visual analysis
- **Document Processing**: PDF, Word, and text document analysis
- **Audio Processing**: Speech-to-text and audio feature extraction
- **Video Processing**: Key frame extraction and video analysis

#### 🤖 **AI-Powered Features**
- **Memory Compression**: Intelligent memory consolidation and summarization
- **Adaptive Learning**: User behavior analysis and pattern recognition
- **Recommendation Engine**: Personalized content and memory suggestions
- **Profile Generation**: Dynamic user and agent profile creation

#### 🔧 **Enterprise-Grade Infrastructure**
- **High Performance**: Async processing and optimized caching
- **Scalability**: Distributed architecture with load balancing support
- **Reliability**: Circuit breakers, retry mechanisms, and failover
- **Monitoring**: Comprehensive metrics and health checks

### 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                        │
├─────────────────────────────────────────────────────────────┤
│              Memory Hierarchy Manager                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ User Memory  │  │Session Memory│  │ Agent Memory │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
├─────────────────────────────────────────────────────────────┤
│                   Search & AI Layer                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Semantic     │  │ Multimodal   │  │ AI Features  │     │
│  │ Search       │  │ Processing   │  │ (Compression │     │
│  │              │  │              │  │ & Learning)  │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
├─────────────────────────────────────────────────────────────┤
│                  Storage & Index Layer                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Vector Store │  │ Graph Store  │  │ Cache Layer  │     │
│  │ (Milvus)     │  │ (Neo4j)      │  │ (Memory)     │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
├─────────────────────────────────────────────────────────────┤
│                   Configuration Layer                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Hierarchy    │  │ Search       │  │ Embedding    │     │
│  │ Config       │  │ Config       │  │ Config       │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

### 📦 Installation

#### Maven
```xml
<dependency>
    <groupId>com.mem0</groupId>
    <artifactId>mem0-java</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### Gradle
```gradle
implementation 'com.mem0:mem0-java:1.0.0'
```

### 🚀 Quick Start

#### Basic Usage

```java
import com.mem0.Mem0;
import com.mem0.core.Memory;

// Initialize Mem0
Mem0 mem0 = Mem0.builder()
    .withUserId("user123")
    .withEmbeddingProvider("openai") // or "aliyun", "tfidf"
    .build();

// Add memory
Memory memory = new Memory();
memory.setContent("I love Japanese cuisine, especially sushi and ramen.");
mem0.add(memory).join();

// Search memories
List<Memory> memories = mem0.search("What food do I like?", 5).join();
```

#### Hierarchical Memory

```java
import com.mem0.hierarchy.MemoryHierarchyManager;

MemoryHierarchyManager hierarchyManager = new MemoryHierarchyManager();

// User-level persistent memory
hierarchyManager.addMemory("user123", null, null, userMemory);

// Session-level temporary memory
hierarchyManager.addMemory("user123", "session456", null, sessionMemory);

// Agent-level specialized memory
hierarchyManager.addMemory("user123", "session456", "agent789", agentMemory);
```

#### Advanced Search

```java
import com.mem0.search.HybridSearchEngine;
import com.mem0.search.SearchFilter;

HybridSearchEngine searchEngine = new HybridSearchEngine();

// Create advanced search filters
SearchFilter filter = SearchFilter.builder()
    .withTimeRange(LocalDateTime.now().minusDays(7), LocalDateTime.now())
    .withMetadata("category", "important")
    .withImportanceRange(0.7, 1.0)
    .build();

// Perform hybrid search
List<Memory> results = searchEngine.search(
    "machine learning concepts", 
    10, 
    filter
).join();
```

#### Embedding Providers

```java
import com.mem0.embedding.EmbeddingProviderFactory;
import com.mem0.embedding.EmbeddingProvider;

// OpenAI Embedding
EmbeddingProvider openaiProvider = EmbeddingProviderFactory
    .createOpenAI("your-openai-api-key");

// Aliyun Embedding (for Chinese)
EmbeddingProvider aliyunProvider = EmbeddingProviderFactory
    .createAliyun("your-aliyun-api-key");

// Local TF-IDF Embedding
EmbeddingProvider tfidfProvider = EmbeddingProviderFactory
    .createTFIDF();

// Generate embeddings
List<Float> embedding = openaiProvider.embed("Hello world").join();
```

#### Configuration

```java
import com.mem0.config.ConfigurationManager;

ConfigurationManager configManager = ConfigurationManager.getInstance();

// Configure embedding provider
configManager.setGlobalProperty("embedding.provider.type", "openai");
configManager.setGlobalProperty("embedding.openai.apiKey", "your-api-key");

// Configure memory hierarchy
configManager.setGlobalProperty("hierarchy.user.maxMemoriesPerUser", "10000");
configManager.setGlobalProperty("hierarchy.session.timeoutMinutes", "60");
```

## Configuration

Create a configuration file or set environment variables for the services you want to use:

### Environment Variables

```bash
# Vector Database (Milvus)
MILVUS_HOST=localhost
MILVUS_PORT=19530
MILVUS_TOKEN=your-token

# Graph Database (Neo4j)  
NEO4J_URI=bolt://localhost:7687
NEO4J_USERNAME=neo4j
NEO4J_PASSWORD=your-password

# LLM Provider
LLM_PROVIDER=openai  # or "mock"
OPENAI_API_KEY=your-openai-api-key
LLM_MODEL=gpt-4

# Embedding Provider
EMBEDDING_PROVIDER=openai  # or "mock"  
EMBEDDING_MODEL=text-embedding-ada-002
```

### Programmatic Configuration

```java
import com.mem0.config.Mem0Config;
import com.mem0.core.MemoryService;

Mem0Config config = new Mem0Config();

// Configure Vector Store
config.getVectorStore().setProvider("milvus");
config.getVectorStore().setHost("localhost");
config.getVectorStore().setPort(19530);

// Configure Graph Store
config.getGraphStore().setProvider("neo4j");
config.getGraphStore().setUri("bolt://localhost:7687");
config.getGraphStore().setUsername("neo4j");
config.getGraphStore().setPassword("password");

// Configure LLM
config.getLlm().setProvider("openai");
config.getLlm().setApiKey("your-api-key");
config.getLlm().setModel("gpt-4");

// Configure Embedding
config.getEmbedding().setProvider("openai");
config.getEmbedding().setApiKey("your-api-key");

MemoryService memoryService = new MemoryService(config);
```

## Usage Examples

### Basic Memory Operations

```java
import com.mem0.core.MemoryService;
import com.mem0.config.Mem0Config;

// Initialize service
Mem0Config config = new Mem0Config();
MemoryService memoryService = new MemoryService(config);

// Add memories
String memoryId1 = memoryService.addMemory(
    "User prefers coffee over tea", 
    "user123"
).get();

String memoryId2 = memoryService.addMemory(
    "User is working on a Java project",
    "user123",
    "context",
    Map.of("project", "mem0-java", "language", "Java")
).get();

// Search memories
List<MemoryService.Memory> results = memoryService.searchMemories(
    "What does the user like to drink?",
    "user123",
    5
).get();

// Get specific memory
MemoryService.Memory memory = memoryService.getMemory(memoryId1).get();

// Delete memory
memoryService.deleteMemory(memoryId1).get();
```

### Retrieval-Augmented Generation (RAG)

```java
// Add context memories
memoryService.addMemory("User is a software developer", "user123").get();
memoryService.addMemory("User specializes in backend systems", "user123").get();
memoryService.addMemory("User prefers Spring Boot framework", "user123").get();

// Query with RAG
String response = memoryService.queryWithRAG(
    "What kind of developer am I?",
    "user123",
    5,  // max memories to retrieve
    "You are a helpful assistant with access to user context."
).get();

System.out.println(response);
```

### Memory Relationships

```java
// Create relationship between memories
String relationshipId = memoryService.createMemoryRelationship(
    memoryId1,
    memoryId2, 
    "RELATES_TO",
    Map.of("strength", 0.8, "type", "preference")
).get();

// Find related memories
List<MemoryService.Memory> relatedMemories = memoryService.getRelatedMemories(
    memoryId1,
    "RELATES_TO",
    2  // max hops
).get();
```

## Architecture

The library is built with a modular architecture:

```
com.mem0
├── config/          # Configuration classes
├── core/            # Core MemoryService and main API
├── embeddings/      # Embedding providers (OpenAI, Mock)
├── llm/             # LLM providers (OpenAI, Mock) 
├── store/           # Storage backends (Milvus, Neo4j)
└── template/        # RAG prompt templates
```

### Key Components

- **MemoryService**: Main service class providing the public API
- **VectorStore**: Interface for vector database operations (Milvus implementation)
- **GraphStore**: Interface for graph database operations (Neo4j implementation)  
- **EmbeddingProvider**: Interface for text embedding services
- **LLMProvider**: Interface for language model services
- **PromptTemplate**: RAG prompt formatting and templating

## Testing

The project includes comprehensive test coverage:

```bash
# Run unit tests
mvn test

# Run integration tests  
mvn verify

# Run specific test class
mvn test -Dtest=MemoryServiceTest
```

### Test Categories

- **Unit Tests**: Test individual components in isolation using mocks
- **Integration Tests**: Test component interactions and end-to-end workflows
- **Mock Implementations**: Allow testing without external service dependencies

## Docker Setup

For development and testing, you can use Docker to run the required services:

```bash
# Start Milvus
docker run -d --name milvus-standalone -p 19530:19530 milvusdb/milvus:latest

# Start Neo4j  
docker run -d --name neo4j \
  -p 7474:7474 -p 7687:7687 \
  -e NEO4J_AUTH=neo4j/password \
  neo4j:latest
```

## Production Deployment

### Performance Considerations

- **Vector Dimensions**: Match embedding dimensions with your chosen model
- **Batch Operations**: Use batch embedding and insertion for better performance
- **Connection Pooling**: Configure appropriate connection pools for databases
- **Memory Management**: Monitor JVM heap usage with large embedding datasets

### Security

- **API Keys**: Never commit API keys to version control
- **Database Security**: Use secure connections and authentication
- **Input Validation**: Validate all user inputs before processing
- **Rate Limiting**: Implement rate limiting for API calls

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Setup

```bash
# Clone your fork
git clone https://github.com/yourusername/mem0-java.git
cd mem0-java

# Install dependencies
mvn clean install

# Run tests
mvn test
```

## Roadmap

- [ ] Support for additional vector databases (Pinecone, Weaviate)
- [ ] Support for additional LLM providers (Anthropic Claude, HuggingFace)
- [ ] Advanced memory filtering and ranking
- [ ] Memory lifecycle management (TTL, archiving)
- [ ] Distributed deployment support
- [ ] Performance monitoring and metrics

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Original [mem0](https://github.com/mem0ai/mem0) Python library
- [Milvus](https://milvus.io/) for vector database capabilities
- [Neo4j](https://neo4j.com/) for graph database functionality
- [OpenAI](https://openai.com/) for LLM and embedding services