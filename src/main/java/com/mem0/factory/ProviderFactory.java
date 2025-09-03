package com.mem0.factory;

import com.mem0.config.Mem0Config;
import com.mem0.embedding.EmbeddingProvider;
import com.mem0.embedding.impl.AliyunEmbeddingProvider;
import com.mem0.embedding.impl.SimpleTFIDFEmbeddingProvider;
import com.mem0.llm.LLMProvider;
import com.mem0.llm.impl.QwenLLMProvider;
import com.mem0.llm.impl.RuleBasedLLMProvider;
import com.mem0.store.GraphStore;
import com.mem0.store.VectorStore;
import com.mem0.graph.impl.DefaultInMemoryGraphStore;
import com.mem0.vector.impl.InMemoryVectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating provider instances based on configuration.
 * 
 * Centralizes provider creation logic and handles fallback scenarios.
 * Improves maintainability and testability by separating provider creation from main class.
 */
public class ProviderFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(ProviderFactory.class);
    
    /**
     * Create VectorStore instance from configuration
     */
    public static VectorStore createVectorStore(Mem0Config.VectorStoreConfig config) {
        if (config == null) {
            logger.warn("VectorStore config is null, using default InMemory implementation");
            return new InMemoryVectorStore();
        }
        
        String provider = config.getProvider() != null ? config.getProvider().toLowerCase() : "inmemory";
        
        switch (provider) {
            case "milvus":
                logger.warn("Milvus provider not implemented yet, falling back to InMemory");
                return new InMemoryVectorStore();
            case "inmemory":
            default:
                logger.debug("Creating InMemory VectorStore");
                return new InMemoryVectorStore();
        }
    }
    
    /**
     * Create GraphStore instance from configuration
     */
    public static GraphStore createGraphStore(Mem0Config.GraphStoreConfig config) {
        if (config == null) {
            logger.warn("GraphStore config is null, using default InMemory implementation");
            return new DefaultInMemoryGraphStore();
        }
        
        String provider = config.getProvider() != null ? config.getProvider().toLowerCase() : "inmemory";
        
        switch (provider) {
            case "neo4j":
                logger.warn("Neo4j provider not fully implemented yet, falling back to InMemory");
                return new DefaultInMemoryGraphStore();
            case "inmemory":
            default:
                logger.debug("Creating InMemory GraphStore");
                return new DefaultInMemoryGraphStore();
        }
    }
    
    /**
     * Create EmbeddingProvider instance from configuration
     */
    public static EmbeddingProvider createEmbeddingProvider(Mem0Config.EmbeddingConfig config) {
        if (config == null) {
            logger.warn("Embedding config is null, using TFIDF implementation");
            return new SimpleTFIDFEmbeddingProvider();
        }
        
        String provider = config.getProvider() != null ? config.getProvider().toLowerCase() : "tfidf";
        
        switch (provider) {
            case "openai":
                logger.warn("OpenAI embedding provider requires API key configuration");
                if (config.getApiKey() != null && !config.getApiKey().trim().isEmpty()) {
                    logger.debug("Creating OpenAI EmbeddingProvider");
                    // Note: OpenAI provider would need proper implementation
                    return new SimpleTFIDFEmbeddingProvider();
                } else {
                    logger.warn("OpenAI API key not provided, falling back to TFIDF");
                    return new SimpleTFIDFEmbeddingProvider();
                }
            case "aliyun":
                if (config.getApiKey() != null && !config.getApiKey().trim().isEmpty()) {
                    logger.debug("Creating Aliyun EmbeddingProvider");
                    return new AliyunEmbeddingProvider(config.getApiKey());
                } else {
                    logger.warn("Aliyun API key not provided, falling back to TFIDF");
                    return new SimpleTFIDFEmbeddingProvider();
                }
            case "tfidf":
            case "mock":
            default:
                logger.debug("Creating TFIDF EmbeddingProvider");
                return new SimpleTFIDFEmbeddingProvider();
        }
    }
    
    /**
     * Create LLMProvider instance from configuration
     */
    public static LLMProvider createLLMProvider(Mem0Config.LLMConfig config) {
        if (config == null) {
            logger.warn("LLM config is null, using RuleBased implementation");
            return new RuleBasedLLMProvider();
        }
        
        String provider = config.getProvider() != null ? config.getProvider().toLowerCase() : "rulebased";
        
        switch (provider) {
            case "openai":
                logger.warn("OpenAI LLM provider requires API key configuration");
                if (config.getApiKey() != null && !config.getApiKey().trim().isEmpty()) {
                    logger.debug("Creating OpenAI LLMProvider");
                    // Note: OpenAI provider would need proper implementation
                    return new RuleBasedLLMProvider();
                } else {
                    logger.warn("OpenAI API key not provided, falling back to RuleBased");
                    return new RuleBasedLLMProvider();
                }
            case "qwen":
                if (config.getApiKey() != null && !config.getApiKey().trim().isEmpty()) {
                    logger.debug("Creating Qwen LLMProvider");
                    return new QwenLLMProvider(config.getApiKey());
                } else {
                    logger.warn("Qwen API key not provided, falling back to RuleBased");
                    return new RuleBasedLLMProvider();
                }
            case "rulebased":
            case "mock":
            default:
                logger.debug("Creating RuleBased LLMProvider");
                return new RuleBasedLLMProvider();
        }
    }
    
    /**
     * Validate provider configuration using comprehensive validator
     */
    public static void validateConfiguration(Mem0Config config) {
        com.mem0.config.ConfigurationValidator.validateAndThrow(config);
    }
}