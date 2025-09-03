# Custom Provider Examples

This directory contains examples for implementing custom providers for Mem0.

## Available Providers in Codebase

### LLM Providers
The existing codebase includes these LLM providers:
- `QwenLLMProvider` - Qwen/Tongyi Qianwen LLM integration
- `RuleBasedLLMProvider` - Simple rule-based LLM for testing
- `SiliconFlowLLMProvider` - SiliconFlow API integration
- `MockLLMProvider` - Mock provider for testing

### Embedding Providers  
The existing codebase includes these embedding providers:
- `AliyunEmbeddingProvider` - Aliyun/DashScope embedding integration
- `OpenAIEmbeddingProvider` - OpenAI embedding integration
- `SimpleTFIDFEmbeddingProvider` - TF-IDF based embedding
- `HighPerformanceTFIDFProvider` - Optimized TF-IDF embedding
- `MockEmbeddingProvider` - Mock provider for testing

## How to Create Custom Providers

### Custom LLM Provider
To create a custom LLM provider, implement the `LLMProvider` interface:

```java
public class CustomLLMProvider implements LLMProvider {
    @Override
    public CompletableFuture<LLMResponse> generateResponse(String prompt) {
        // Your implementation here
        return CompletableFuture.completedFuture(new LLMResponse("Generated response", 100));
    }
    
    @Override
    public void close() throws IOException {
        // Cleanup resources
    }
}
```

### Custom Embedding Provider
To create a custom embedding provider, implement the `EmbeddingProvider` interface:

```java
public class CustomEmbeddingProvider implements EmbeddingProvider {
    @Override
    public CompletableFuture<List<Float>> generateEmbedding(String text) {
        // Your implementation here
        List<Float> embedding = new ArrayList<>();
        // Generate embedding vector
        return CompletableFuture.completedFuture(embedding);
    }
    
    @Override
    public void close() throws IOException {
        // Cleanup resources
    }
}
```

## Integration with TestConfiguration

Custom providers can be integrated with the centralized `TestConfiguration`:

```java
// In your test setup
TestConfiguration testConfig = TestConfiguration.getInstance();
CustomLLMProvider customLLM = new CustomLLMProvider();
CustomEmbeddingProvider customEmbedding = new CustomEmbeddingProvider();

// Use with Mem0Config
Mem0Config config = testConfig.createMem0Config();
// Configure to use custom providers
```

## Best Practices

1. **Java 8 Compatibility**: Ensure all custom providers are Java 8 compatible
2. **Async Operations**: Use `CompletableFuture` for non-blocking operations
3. **Resource Management**: Implement proper `close()` methods
4. **Error Handling**: Include comprehensive error handling
5. **Testing**: Write unit tests for custom providers
6. **Documentation**: Document configuration options and usage

## Examples in Other Directories

- See `../initialization/` for Mem0 setup examples
- See `../spring/` for Spring integration patterns
- See `../stores/` for custom store implementations