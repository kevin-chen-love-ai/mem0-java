package com.mem0.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Mem0配置类
 * Mem0 Configuration Class
 * 
 * 该类用于管理Mem0框架的所有配置选项，包括向量存储、图存储、LLM和嵌入模型的配置。
 * 提供了灵活的配置管理机制，支持多种存储后端和AI服务提供商。
 * 
 * This class manages all configuration options for the Mem0 framework, including vector store,
 * graph store, LLM, and embedding model configurations. It provides a flexible configuration
 * management mechanism supporting various storage backends and AI service providers.
 * 
 * 主要功能 / Key Features:
 * • 向量存储配置管理 / Vector store configuration management
 * • 图数据库配置管理 / Graph database configuration management  
 * • 大语言模型配置管理 / Large Language Model configuration management
 * • 嵌入模型配置管理 / Embedding model configuration management
 * • 多提供商支持 / Multi-provider support
 * • 动态配置更新 / Dynamic configuration updates
 * 
 * 配置示例 / Configuration Example:
 * <pre>{@code
 * // 创建配置实例 / Create configuration instance
 * Mem0Config config = new Mem0Config();
 * 
 * // 配置向量存储 / Configure vector store
 * config.getVectorStore().setProvider("milvus");
 * config.getVectorStore().setHost("localhost");
 * config.getVectorStore().setPort(19530);
 * 
 * // 配置图存储 / Configure graph store
 * config.getGraphStore().setProvider("neo4j");
 * config.getGraphStore().setUri("bolt://localhost:7687");
 * config.getGraphStore().setUsername("neo4j");
 * config.getGraphStore().setPassword("password");
 * 
 * // 配置LLM / Configure LLM
 * config.getLlm().setProvider("openai");
 * config.getLlm().setApiKey("your-api-key");
 * config.getLlm().setModel("gpt-3.5-turbo");
 * 
 * // 配置嵌入模型 / Configure embedding model
 * config.getEmbedding().setProvider("openai");
 * config.getEmbedding().setApiKey("your-api-key");
 * config.getEmbedding().setModel("text-embedding-ada-002");
 * }</pre>
 * 
 * 使用示例 / Usage Example:
 * <pre>{@code
 * // 基本使用 / Basic usage
 * Mem0Config config = new Mem0Config();
 * Mem0 mem0 = new Mem0(config);
 * 
 * // 自定义配置 / Custom configuration
 * Mem0Config customConfig = new Mem0Config();
 * customConfig.getVectorStore().setHost("your-milvus-host");
 * customConfig.getVectorStore().setPort(19530);
 * customConfig.getVectorStore().setToken("your-token");
 * 
 * // 高级配置 / Advanced configuration
 * Map<String, Object> additionalConfig = new HashMap<>();
 * additionalConfig.put("timeout", 30000);
 * additionalConfig.put("retries", 3);
 * customConfig.getVectorStore().setAdditionalConfig(additionalConfig);
 * }</pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class Mem0Config {
    
    private VectorStoreConfig vectorStore = new VectorStoreConfig();
    private GraphStoreConfig graphStore = new GraphStoreConfig();
    private LLMConfig llm = new LLMConfig();
    private EmbeddingConfig embedding = new EmbeddingConfig();
    
    public Mem0Config() {}
    
    // Getters and setters
    public VectorStoreConfig getVectorStore() { return vectorStore; }
    public void setVectorStore(VectorStoreConfig vectorStore) { this.vectorStore = vectorStore; }
    
    public GraphStoreConfig getGraphStore() { return graphStore; }
    public void setGraphStore(GraphStoreConfig graphStore) { this.graphStore = graphStore; }
    
    public LLMConfig getLlm() { return llm; }
    public void setLlm(LLMConfig llm) { this.llm = llm; }
    
    public EmbeddingConfig getEmbedding() { return embedding; }
    public void setEmbedding(EmbeddingConfig embedding) { this.embedding = embedding; }
    
    public static class VectorStoreConfig {
        private String provider = "milvus";
        private String host = "localhost";
        private int port = 19530;
        private String token;
        private String database;
        private Map<String, Object> additionalConfig = new HashMap<>();
        
        // Getters and setters
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        
        public String getDatabase() { return database; }
        public void setDatabase(String database) { this.database = database; }
        
        public Map<String, Object> getAdditionalConfig() { return additionalConfig; }
        public void setAdditionalConfig(Map<String, Object> additionalConfig) { this.additionalConfig = additionalConfig; }
    }
    
    public static class GraphStoreConfig {
        private String provider = "neo4j";
        private String uri = "bolt://localhost:7687";
        private String username = "neo4j";
        private String password = "password";
        private Map<String, Object> additionalConfig = new HashMap<>();
        
        // Getters and setters
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        
        public String getUri() { return uri; }
        public void setUri(String uri) { this.uri = uri; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public Map<String, Object> getAdditionalConfig() { return additionalConfig; }
        public void setAdditionalConfig(Map<String, Object> additionalConfig) { this.additionalConfig = additionalConfig; }
    }
    
    public static class LLMConfig {
        private String provider = "mock";
        private String apiKey;
        private String model;
        private double temperature = 0.7;
        private int maxTokens = 1000;
        private Map<String, Object> additionalConfig = new HashMap<>();
        
        // Getters and setters
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        
        public Map<String, Object> getAdditionalConfig() { return additionalConfig; }
        public void setAdditionalConfig(Map<String, Object> additionalConfig) { this.additionalConfig = additionalConfig; }
    }
    
    public static class EmbeddingConfig {
        private String provider = "mock";
        private String apiKey;
        private String model;
        private Map<String, Object> additionalConfig = new HashMap<>();
        
        // Getters and setters
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        
        public Map<String, Object> getAdditionalConfig() { return additionalConfig; }
        public void setAdditionalConfig(Map<String, Object> additionalConfig) { this.additionalConfig = additionalConfig; }
    }
}