# Mem0 Java Examples 示例

这个目录包含了各种使用 Mem0 Java SDK 的示例代码，涵盖了从基础用法到高级集成的多种场景。

This directory contains various example code for using the Mem0 Java SDK, covering scenarios from basic usage to advanced integrations.

## 📁 目录结构 Directory Structure

```
examples/
├── basic/                   # 基础使用示例 Basic Usage Examples
│   └── BasicUsageExample.java
├── spring/                  # Spring Boot 集成示例 Spring Boot Integration
│   ├── SpringBootIntegrationExample.java
│   └── application.yml
├── custom/                  # 自定义LLM提供者示例 Custom LLM Providers
│   └── CustomLLMProviderExample.java
├── vector/                  # 向量数据库集成 Vector Database Integration
│   └── VectorDatabaseExample.java
├── graph/                   # 图数据库连接 Graph Database Connections
│   └── GraphDatabaseExample.java
├── redis/                   # Redis集成示例 Redis Integration
│   └── RedisIntegrationExample.java
└── README.md               # 本文件 This file
```

## 🚀 快速开始 Quick Start

### 前提条件 Prerequisites

- Java 8 或更高版本 Java 8 or higher
- Maven 3.6+ 或 Gradle 6.0+

### 依赖配置 Dependencies

Maven:
```xml
<dependency>
    <groupId>com.mem0</groupId>
    <artifactId>mem0-java</artifactId>
    <version>1.0.0</version>
</dependency>
```

Gradle:
```gradle
implementation 'com.mem0:mem0-java:1.0.0'
```

## 📋 示例详情 Example Details

### 1. 基础使用示例 Basic Usage Example
**文件位置 File Location**: `basic/BasicUsageExample.java`

展示了 Mem0 的核心功能：
- 添加内存 Memory addition
- 更新内存 Memory updates  
- 搜索内存 Memory search
- 批量操作 Batch operations

```java
Mem0 mem0 = new Mem0(config);
String memoryId = mem0.add("I love drinking coffee in the morning", "user123");
List<Memory> results = mem0.search("coffee", "user123");
```

### 2. Spring Boot 集成示例 Spring Boot Integration
**文件位置 File Location**: `spring/SpringBootIntegrationExample.java`

完整的 Spring Boot REST API 集成：
- RESTful API 端点 RESTful API endpoints
- 配置管理 Configuration management
- 依赖注入 Dependency injection
- 错误处理 Error handling

**主要端点 Main Endpoints**:
- `POST /api/memory` - 添加内存 Add memory
- `GET /api/memory/search` - 搜索内存 Search memories
- `PUT /api/memory/{id}` - 更新内存 Update memory
- `DELETE /api/memory/{id}` - 删除内存 Delete memory

### 3. 自定义LLM提供者 Custom LLM Providers
**文件位置 File Location**: `custom/CustomLLMProviderExample.java`

支持多种LLM服务的实现：
- **Claude LLM Provider**: Anthropic Claude API 集成
- **Gemini LLM Provider**: Google Gemini API 集成
- **Local LLM Provider**: 支持 Ollama 等本地模型
- **Enterprise LLM Provider**: 企业级负载均衡和故障转移

```java
// Claude 示例 Claude Example
config.setLlmProvider(new ClaudeLLMProvider("your-anthropic-api-key"));

// 本地LLM示例 Local LLM Example  
config.setLlmProvider(new LocalLLMProvider("http://localhost:11434"));
```

### 4. 向量数据库集成 Vector Database Integration
**文件位置 File Location**: `vector/VectorDatabaseExample.java`

支持多种向量数据库：
- **Pinecone**: 云端向量数据库服务
- **Weaviate**: 开源向量搜索引擎
- **Qdrant**: 高性能向量相似度搜索
- **Custom Vector Store**: 自定义向量存储实现

```java
// Pinecone 示例 Pinecone Example
config.setVectorStore(new PineconeVectorStore("api-key", "environment", "index"));

// Weaviate 示例 Weaviate Example
config.setVectorStore(new WeaviateVectorStore("http://localhost:8080", "api-key"));
```

### 5. 图数据库连接 Graph Database Connections
**文件位置 File Location**: `graph/GraphDatabaseExample.java`

支持多种图数据库：
- **Neo4j**: 使用 Cypher 查询的图数据库
- **ArangoDB**: 多模型数据库（文档、键值、图）
- **Amazon Neptune**: AWS 托管的图数据库服务
- **Custom Graph Store**: 内存图存储实现

```java
// Neo4j 示例 Neo4j Example
config.setGraphStore(new Neo4jGraphStore("bolt://localhost:7687", "neo4j", "password"));

// ArangoDB 示例 ArangoDB Example
config.setGraphStore(new ArangoDBGraphStore("http://localhost:8529", "root", "password", "db"));
```

### 6. Redis集成示例 Redis Integration
**文件位置 File Location**: `redis/RedisIntegrationExample.java`

全面的 Redis 集成模式：
- **Redis缓存 Redis Caching**: 基础缓存操作
- **会话存储 Session Storage**: 用户会话管理
- **发布/订阅 Pub/Sub**: 消息传递模式
- **分布式锁 Distributed Locking**: 并发控制
- **集群支持 Cluster Support**: Redis 集群配置

```java
// Redis缓存示例 Redis Cache Example
config.setCacheProvider(new RedisCacheProvider("localhost", 6379));

// Redis集群示例 Redis Cluster Example
config.setCacheProvider(new RedisClusterCache(clusterNodes));
```

## 🔧 运行示例 Running Examples

### 命令行运行 Command Line Execution

```bash
# 编译项目 Compile project
mvn clean compile

# 运行基础示例 Run basic example
java -cp target/classes examples.basic.BasicUsageExample

# 运行Spring Boot示例 Run Spring Boot example
java -cp target/classes examples.spring.SpringBootIntegrationExample
```

### IDE 运行 IDE Execution

1. 导入项目到 IDE Import project to IDE
2. 设置环境变量 Set environment variables:
   - `OPENAI_API_KEY`: OpenAI API 密钥
   - `ANTHROPIC_API_KEY`: Anthropic API 密钥
   - `PINECONE_API_KEY`: Pinecone API 密钥
3. 直接运行 main 方法 Run main method directly

## 📝 配置说明 Configuration Guide

### 环境变量 Environment Variables

```bash
export OPENAI_API_KEY="your-openai-api-key"
export ANTHROPIC_API_KEY="your-anthropic-api-key"
export PINECONE_API_KEY="your-pinecone-api-key"
export PINECONE_ENVIRONMENT="your-pinecone-environment"
export NEO4J_PASSWORD="your-neo4j-password"
export REDIS_PASSWORD="your-redis-password"
```

### Spring Boot 配置 Spring Boot Configuration

在 `application.yml` 中配置 Mem0：

```yaml
mem0:
  llm:
    provider: openai
    api-key: ${OPENAI_API_KEY}
    model: gpt-4
  vector:
    provider: pinecone
    api-key: ${PINECONE_API_KEY}
  cache:
    provider: redis
    host: localhost
    port: 6379
```

## 🛠️ 常见问题 Troubleshooting

### 连接问题 Connection Issues

1. **API 密钥错误 API Key Error**: 确保设置了正确的环境变量
2. **网络超时 Network Timeout**: 检查网络连接和防火墙设置
3. **版本兼容性 Version Compatibility**: 确保使用兼容的Java版本

### 性能优化 Performance Optimization

1. **连接池配置 Connection Pool**: 适当配置HTTP客户端连接池
2. **批处理 Batch Processing**: 使用批量操作提高效率
3. **缓存策略 Caching Strategy**: 合理使用缓存减少API调用

## 🔗 相关资源 Related Resources

- [Mem0 官方文档 Official Documentation](https://mem0.ai)
- [Java SDK API 文档 Java SDK API Documentation](https://docs.mem0.ai/java)
- [问题反馈 Issue Tracker](https://github.com/mem0/mem0-java/issues)

## 📄 许可证 License

MIT License - 详见项目根目录的 LICENSE 文件