package com.mem0.embedding;

import com.mem0.embedding.impl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 嵌入提供者工厂类 / Embedding Provider Factory
 * 
 * 该类提供统一的嵌入提供者创建和管理功能，支持多种嵌入服务提供商的实例化，
 * 包括OpenAI、阿里云、以及本地TF-IDF等不同的嵌入向量生成服务。
 * 
 * This class provides unified embedding provider creation and management functionality,
 * supporting instantiation of various embedding service providers including OpenAI,
 * Aliyun, and local TF-IDF embedding vector generation services.
 * 
 * 主要功能 / Key Features:
 * - 多种嵌入提供者的统一创建接口 / Unified creation interface for multiple embedding providers
 * - 提供者实例缓存和复用 / Provider instance caching and reuse
 * - 配置文件支持 / Configuration file support
 * - 自动资源管理 / Automatic resource management
 * - 健康状态检查 / Health status monitoring
 * 
 * 支持的提供者 / Supported Providers:
 * - OpenAI: GPT系列模型的嵌入服务 / OpenAI embedding service for GPT models
 * - Aliyun: 阿里云机器学习平台PAI / Aliyun Machine Learning Platform PAI
 * - TF-IDF: 本地TF-IDF实现 / Local TF-IDF implementation
 * - Mock: 测试用模拟提供者 / Mock provider for testing
 * 
 * 使用示例 / Usage Example:
 * <pre>
 * {@code
 * // 创建OpenAI提供者 / Create OpenAI provider
 * EmbeddingProvider openaiProvider = EmbeddingProviderFactory.createOpenAI("your-api-key");
 * 
 * // 创建阿里云提供者 / Create Aliyun provider
 * EmbeddingProvider aliyunProvider = EmbeddingProviderFactory.createAliyun("your-api-key");
 * 
 * // 从配置创建提供者 / Create provider from configuration
 * Properties config = new Properties();
 * config.setProperty("provider.type", "openai");
 * config.setProperty("openai.apiKey", "your-api-key");
 * EmbeddingProvider provider = EmbeddingProviderFactory.createFromConfig(config);
 * 
 * // 获取默认提供者 / Get default provider
 * EmbeddingProvider defaultProvider = EmbeddingProviderFactory.getDefaultProvider();
 * 
 * // 关闭所有提供者 / Close all providers
 * EmbeddingProviderFactory.closeAll();
 * }
 * </pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class EmbeddingProviderFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingProviderFactory.class);
    
    // 提供者实例缓存 / Provider instance cache
    private static final ConcurrentMap<String, EmbeddingProvider> providerCache = new ConcurrentHashMap<>();
    
    // 默认提供者 / Default provider
    private static volatile EmbeddingProvider defaultProvider;
    
    /**
     * 提供者类型枚举 / Provider Type Enum
     */
    public enum ProviderType {
        OPENAI("openai"),
        ALIYUN("aliyun"),
        TFIDF("tfidf"),
        HIGH_PERFORMANCE_TFIDF("high-performance-tfidf"),
        MOCK("mock");
        
        private final String name;
        
        ProviderType(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
        
        public static ProviderType fromName(String name) {
            for (ProviderType type : values()) {
                if (type.name.equalsIgnoreCase(name)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown provider type: " + name);
        }
    }
    
    /**
     * 创建OpenAI嵌入提供者
     * Create OpenAI embedding provider
     * 
     * @param apiKey OpenAI API密钥 / OpenAI API key
     * @return OpenAI嵌入提供者实例 / OpenAI embedding provider instance
     */
    public static EmbeddingProvider createOpenAI(String apiKey) {
        return createOpenAI(apiKey, "text-embedding-ada-002");
    }
    
    /**
     * 创建OpenAI嵌入提供者（指定模型）
     * Create OpenAI embedding provider with specified model
     * 
     * @param apiKey OpenAI API密钥 / OpenAI API key
     * @param model 模型名称 / Model name
     * @return OpenAI嵌入提供者实例 / OpenAI embedding provider instance
     */
    public static EmbeddingProvider createOpenAI(String apiKey, String model) {
        String cacheKey = "openai:" + apiKey + ":" + model;
        return providerCache.computeIfAbsent(cacheKey, k -> {
            logger.info("创建OpenAI嵌入提供者 - Model: {}", model);
            return new OpenAIEmbeddingProvider(apiKey, model);
        });
    }
    
    /**
     * 创建阿里云嵌入提供者
     * Create Aliyun embedding provider
     * 
     * @param apiKey 阿里云API密钥 / Aliyun API key
     * @return 阿里云嵌入提供者实例 / Aliyun embedding provider instance
     */
    public static EmbeddingProvider createAliyun(String apiKey) {
        return createAliyun(apiKey, "text-embedding-v1");
    }
    
    /**
     * 创建阿里云嵌入提供者（指定模型）
     * Create Aliyun embedding provider with specified model
     * 
     * @param apiKey 阿里云API密钥 / Aliyun API key
     * @param model 模型名称 / Model name
     * @return 阿里云嵌入提供者实例 / Aliyun embedding provider instance
     */
    public static EmbeddingProvider createAliyun(String apiKey, String model) {
        String cacheKey = "aliyun:" + apiKey + ":" + model;
        return providerCache.computeIfAbsent(cacheKey, k -> {
            logger.info("创建阿里云嵌入提供者 - Model: {}", model);
            return new AliyunEmbeddingProvider(apiKey, model);
        });
    }
    
    /**
     * 创建TF-IDF嵌入提供者
     * Create TF-IDF embedding provider
     * 
     * @return TF-IDF嵌入提供者实例 / TF-IDF embedding provider instance
     */
    public static EmbeddingProvider createTFIDF() {
        String cacheKey = "tfidf:default";
        return providerCache.computeIfAbsent(cacheKey, k -> {
            logger.info("创建TF-IDF嵌入提供者");
            return new SimpleTFIDFEmbeddingProvider();
        });
    }
    
    /**
     * 创建高性能TF-IDF嵌入提供者
     * Create high-performance TF-IDF embedding provider
     * 
     * @return 高性能TF-IDF嵌入提供者实例 / High-performance TF-IDF embedding provider instance
     */
    public static EmbeddingProvider createHighPerformanceTFIDF() {
        String cacheKey = "high-performance-tfidf:default";
        return providerCache.computeIfAbsent(cacheKey, k -> {
            logger.info("创建高性能TF-IDF嵌入提供者");
            return new HighPerformanceTFIDFProvider();
        });
    }
    
    /**
     * 创建模拟嵌入提供者（用于测试）
     * Create mock embedding provider (for testing)
     * 
     * @return 模拟嵌入提供者实例 / Mock embedding provider instance
     */
    public static EmbeddingProvider createMock() {
        String cacheKey = "mock:default";
        return providerCache.computeIfAbsent(cacheKey, k -> {
            logger.info("创建模拟嵌入提供者");
            return new MockEmbeddingProvider();
        });
    }
    
    /**
     * 从配置创建嵌入提供者
     * Create embedding provider from configuration
     * 
     * @param config 配置属性 / Configuration properties
     * @return 嵌入提供者实例 / Embedding provider instance
     */
    public static EmbeddingProvider createFromConfig(Properties config) {
        String providerType = config.getProperty("provider.type", "tfidf").toLowerCase();
        
        switch (providerType) {
            case "openai":
                String openaiApiKey = config.getProperty("openai.apiKey");
                String openaiModel = config.getProperty("openai.model", "text-embedding-ada-002");
                if (openaiApiKey == null || openaiApiKey.trim().isEmpty()) {
                    throw new IllegalArgumentException("OpenAI API key is required");
                }
                return createOpenAI(openaiApiKey, openaiModel);
                
            case "aliyun":
                String aliyunApiKey = config.getProperty("aliyun.apiKey");
                String aliyunModel = config.getProperty("aliyun.model", "text-embedding-v1");
                if (aliyunApiKey == null || aliyunApiKey.trim().isEmpty()) {
                    throw new IllegalArgumentException("Aliyun API key is required");
                }
                return createAliyun(aliyunApiKey, aliyunModel);
                
            case "high-performance-tfidf":
                return createHighPerformanceTFIDF();
                
            case "tfidf":
                return createTFIDF();
                
            case "mock":
                return createMock();
                
            default:
                logger.warn("未知的提供者类型: {}, 使用默认TF-IDF提供者", providerType);
                return createTFIDF();
        }
    }
    
    /**
     * 获取默认嵌入提供者
     * Get default embedding provider
     * 
     * @return 默认嵌入提供者实例 / Default embedding provider instance
     */
    public static EmbeddingProvider getDefaultProvider() {
        if (defaultProvider == null) {
            synchronized (EmbeddingProviderFactory.class) {
                if (defaultProvider == null) {
                    logger.info("创建默认嵌入提供者 (TF-IDF)");
                    defaultProvider = createTFIDF();
                }
            }
        }
        return defaultProvider;
    }
    
    /**
     * 设置默认嵌入提供者
     * Set default embedding provider
     * 
     * @param provider 嵌入提供者实例 / Embedding provider instance
     */
    public static synchronized void setDefaultProvider(EmbeddingProvider provider) {
        if (defaultProvider != null && defaultProvider != provider) {
            try {
                defaultProvider.close();
            } catch (Exception e) {
                logger.warn("关闭旧的默认提供者时发生错误", e);
            }
        }
        defaultProvider = provider;
        logger.info("设置默认嵌入提供者: {}", provider.getProviderName());
    }
    
    /**
     * 创建指定类型的嵌入提供者
     * Create embedding provider of specified type
     * 
     * @param type 提供者类型 / Provider type
     * @param apiKey API密钥（如果需要）/ API key (if required)
     * @return 嵌入提供者实例 / Embedding provider instance
     */
    public static EmbeddingProvider create(ProviderType type, String apiKey) {
        switch (type) {
            case OPENAI:
                return createOpenAI(apiKey);
            case ALIYUN:
                return createAliyun(apiKey);
            case TFIDF:
                return createTFIDF();
            case HIGH_PERFORMANCE_TFIDF:
                return createHighPerformanceTFIDF();
            case MOCK:
                return createMock();
            default:
                throw new IllegalArgumentException("Unsupported provider type: " + type);
        }
    }
    
    /**
     * 获取已缓存的提供者数量
     * Get number of cached providers
     * 
     * @return 缓存的提供者数量 / Number of cached providers
     */
    public static int getCachedProviderCount() {
        return providerCache.size();
    }
    
    /**
     * 清除提供者缓存
     * Clear provider cache
     */
    public static void clearCache() {
        logger.info("清除嵌入提供者缓存");
        for (EmbeddingProvider provider : providerCache.values()) {
            try {
                provider.close();
            } catch (Exception e) {
                logger.warn("关闭提供者时发生错误: {}", provider.getProviderName(), e);
            }
        }
        providerCache.clear();
    }
    
    /**
     * 关闭所有嵌入提供者
     * Close all embedding providers
     */
    public static void closeAll() {
        logger.info("关闭所有嵌入提供者");
        
        // Close default provider
        if (defaultProvider != null) {
            try {
                defaultProvider.close();
            } catch (Exception e) {
                logger.warn("关闭默认提供者时发生错误", e);
            }
            defaultProvider = null;
        }
        
        // Clear cache (which also closes all cached providers)
        clearCache();
    }
    
    /**
     * 检查所有提供者的健康状态
     * Check health status of all providers
     * 
     * @return 健康的提供者数量 / Number of healthy providers
     */
    public static int checkHealthStatus() {
        int healthyCount = 0;
        for (EmbeddingProvider provider : providerCache.values()) {
            try {
                if (provider.isHealthy()) {
                    healthyCount++;
                }
            } catch (Exception e) {
                logger.warn("检查提供者健康状态时发生错误: {}", provider.getProviderName(), e);
            }
        }
        
        logger.info("健康的嵌入提供者数量: {}/{}", healthyCount, providerCache.size());
        return healthyCount;
    }
}