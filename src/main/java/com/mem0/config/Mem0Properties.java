package com.mem0.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Mem0 Spring配置属性类
 * Mem0 Spring Configuration Properties Class
 * 
 * 该类用于管理Spring Boot应用中的Mem0配置属性，通过@ConfigurationProperties注解
 * 自动绑定application.yml或application.properties文件中的配置项。
 * 
 * This class manages Mem0 configuration properties in Spring Boot applications,
 * automatically binding configuration items from application.yml or application.properties
 * files through the @ConfigurationProperties annotation.
 * 
 * 主要功能 / Key Features:
 * • Spring Boot配置属性绑定 / Spring Boot configuration property binding
 * • 类型安全的配置管理 / Type-safe configuration management
 * • 配置验证支持 / Configuration validation support
 * • 默认值管理 / Default value management
 * • 环境变量支持 / Environment variable support
 * • 配置热重载 / Configuration hot reloading
 * 
 * 配置示例 / Configuration Example:
 * <pre>{@code
 * # application.yml 配置示例
 * mem0:
 *   vector-store:
 *     provider: milvus
 *     host: localhost
 *     port: 19530
 *     token: your-token
 *     database: mem0
 *   graph-store:
 *     provider: neo4j
 *     uri: bolt://localhost:7687
 *     username: neo4j
 *     password: password
 *   llm:
 *     provider: openai
 *     api-key: your-openai-key
 *     model: gpt-3.5-turbo
 *     temperature: 0.7
 *     max-tokens: 1000
 *   embedding:
 *     provider: openai
 *     api-key: your-openai-key
 *     model: text-embedding-ada-002
 * }</pre>
 * 
 * 使用示例 / Usage Example:
 * <pre>{@code
 * // 在Spring Boot应用中自动注入 / Auto-inject in Spring Boot application
 * @Service
 * public class MemoryService {
 *     private final Mem0Properties properties;
 *     
 *     public MemoryService(Mem0Properties properties) {
 *         this.properties = properties;
 *     }
 *     
 *     public void initializeMemory() {
 *         String vectorProvider = properties.getVectorStore().getProvider();
 *         String graphUri = properties.getGraphStore().getUri();
 *         // 使用配置进行初始化...
 *     }
 * }
 * 
 * // 配置验证示例 / Configuration validation example
 * @ConfigurationPropertiesBinding
 * @Validated
 * public class ValidatedMem0Properties extends Mem0Properties {
 *     @NotNull
 *     @Override
 *     public VectorStoreProperties getVectorStore() {
 *         return super.getVectorStore();
 *     }
 * }
 * }</pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
@ConfigurationProperties(prefix = "mem0")
public class Mem0Properties {
    
    private VectorStoreProperties vectorStore = new VectorStoreProperties();
    private GraphStoreProperties graphStore = new GraphStoreProperties();
    private LLMProperties llm = new LLMProperties();
    private EmbeddingProperties embedding = new EmbeddingProperties();
    
    public VectorStoreProperties getVectorStore() {
        return vectorStore;
    }
    
    public void setVectorStore(VectorStoreProperties vectorStore) {
        this.vectorStore = vectorStore;
    }
    
    public GraphStoreProperties getGraphStore() {
        return graphStore;
    }
    
    public void setGraphStore(GraphStoreProperties graphStore) {
        this.graphStore = graphStore;
    }
    
    public LLMProperties getLlm() {
        return llm;
    }
    
    public void setLlm(LLMProperties llm) {
        this.llm = llm;
    }
    
    public EmbeddingProperties getEmbedding() {
        return embedding;
    }
    
    public void setEmbedding(EmbeddingProperties embedding) {
        this.embedding = embedding;
    }
    
    public static class VectorStoreProperties {
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
        public void setAdditionalConfig(Map<String, Object> additionalConfig) { 
            this.additionalConfig = additionalConfig; 
        }
    }
    
    public static class GraphStoreProperties {
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
        public void setAdditionalConfig(Map<String, Object> additionalConfig) { 
            this.additionalConfig = additionalConfig; 
        }
    }
    
    public static class LLMProperties {
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
        public void setAdditionalConfig(Map<String, Object> additionalConfig) { 
            this.additionalConfig = additionalConfig; 
        }
    }
    
    public static class EmbeddingProperties {
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
        public void setAdditionalConfig(Map<String, Object> additionalConfig) { 
            this.additionalConfig = additionalConfig; 
        }
    }
}