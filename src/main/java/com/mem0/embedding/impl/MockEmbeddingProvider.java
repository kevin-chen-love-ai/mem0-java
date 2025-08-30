package com.mem0.embedding.impl;

import com.mem0.embedding.EmbeddingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * Mock嵌入提供者实现 / Mock Embedding Provider Implementation
 * 
 * 该类提供了一个轻量级的测试和开发用嵌入向量生成服务，通过简单的算法
 * 基于文本特征生成确定性的向量表示，主要用于单元测试、原型开发和系统集成测试。
 * 
 * This class provides a lightweight embedding vector generation service for testing
 * and development, generating deterministic vector representations based on text
 * characteristics using simple algorithms, primarily for unit testing, prototyping,
 * and system integration testing.
 * 
 * 主要功能 / Key Features:
 * - 快速确定性向量生成 / Fast deterministic vector generation
 * - 基于文本特征的简单算法 / Simple algorithm based on text characteristics  
 * - 无外部依赖和网络请求 / No external dependencies or network requests
 * - 完全可预测的输出结果 / Completely predictable output results
 * - 支持单个和批量处理 / Support for single and batch processing
 * - 轻量级内存占用 / Lightweight memory footprint
 * 
 * 性能特征 / Performance Characteristics:
 * - 向量维度: 128 (测试优化) / Vector dimension: 128 (test-optimized)
 * - 极低的计算延迟 (<1ms) / Ultra-low computation latency (<1ms)
 * - 无网络IO开销 / No network IO overhead
 * - 确定性输出保证 / Deterministic output guarantee
 * - 内存高效实现 / Memory-efficient implementation
 * 
 * 算法原理 / Algorithm Principles:
 * - 基于文本长度和哈希值的特征提取 / Feature extraction based on text length and hash
 * - 使用正弦函数生成平滑向量分布 / Smooth vector distribution using sine functions
 * - 文本内容敏感的向量差异化 / Content-sensitive vector differentiation
 * - 可重现的伪随机分布 / Reproducible pseudo-random distribution
 * 
 * 使用场景 / Use Cases:
 * - 单元测试和集成测试 / Unit testing and integration testing
 * - 快速原型开发验证 / Rapid prototyping and validation
 * - 开发环境调试 / Development environment debugging
 * - 性能基准测试 / Performance benchmarking
 * - CI/CD流水线测试 / CI/CD pipeline testing
 * 
 * 使用示例 / Usage Example:
 * <pre>
 * {@code
 * // 初始化Mock提供者 / Initialize mock provider
 * MockEmbeddingProvider provider = new MockEmbeddingProvider();
 * 
 * // 单个文本嵌入 / Single text embedding
 * CompletableFuture<List<Float>> embedding = provider.embed("test text");
 * List<Float> vector = embedding.join(); // 128维向量 / 128-dim vector
 * 
 * // 批量文本嵌入 / Batch text embedding
 * List<String> texts = Arrays.asList("hello", "world", "test");
 * CompletableFuture<List<List<Float>>> embeddings = provider.embedBatch(texts);
 * 
 * // 验证确定性 / Verify deterministic behavior
 * List<Float> vector1 = provider.embed("same text").join();
 * List<Float> vector2 = provider.embed("same text").join();
 * assert vector1.equals(vector2); // 相同输入产生相同输出
 * 
 * // 获取提供者信息 / Get provider information
 * String name = provider.getProviderName(); // "Mock"
 * int dimension = provider.getDimension(); // 128
 * boolean healthy = provider.isHealthy(); // true
 * }
 * </pre>
 * 
 * 注意事项 / Important Notes:
 * - 仅用于测试目的，不适用于生产环境
 * - 生成的向量不具备真实的语义特性
 * - 相同文本始终产生相同向量
 * - Only for testing purposes, not suitable for production
 * - Generated vectors don't have real semantic properties  
 * - Same text always produces identical vectors
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class MockEmbeddingProvider implements EmbeddingProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(MockEmbeddingProvider.class);
    
    private final int dimension = 128; // 测试用的较小维度
    
    @Override
    public CompletableFuture<List<Float>> embed(String text) {
        return CompletableFuture.supplyAsync(() -> {
            // 基于文本长度和内容生成简单的测试向量
            List<Float> vector = new ArrayList<>();
            float baseValue = text.length() % 100 / 100.0f;
            
            for (int i = 0; i < dimension; i++) {
                // 生成基于文本特征的简单向量
                float value = (float) (baseValue + Math.sin(i * 0.1) * 0.1 + (text.hashCode() % 1000) / 10000.0);
                vector.add(value);
            }
            
            return vector;
        });
    }
    
    @Override
    public CompletableFuture<List<List<Float>>> embedBatch(List<String> texts) {
        return CompletableFuture.supplyAsync(() -> {
            List<List<Float>> vectors = new ArrayList<>();
            for (String text : texts) {
                vectors.add(embed(text).join());
            }
            return vectors;
        });
    }
    
    @Override
    public int getDimension() {
        return dimension;
    }
    
    @Override
    public String getProviderName() {
        return "Mock";
    }
    
    @Override
    public boolean isHealthy() {
        return true;
    }
    
    @Override
    public void close() {
        logger.info("Mock嵌入提供者已关闭");
    }
}