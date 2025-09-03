package com.mem0.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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
    
    private static final Logger logger = LoggerFactory.getLogger(Mem0Config.class);
    
    private VectorStoreConfig vectorStore = new VectorStoreConfig();
    private GraphStoreConfig graphStore = new GraphStoreConfig();
    private LLMConfig llm = new LLMConfig();
    private EmbeddingConfig embedding = new EmbeddingConfig();
    
    public Mem0Config() {}
    
    /**
     * 通过配置文件路径创建Mem0Config实例 / Create Mem0Config instance from configuration file path
     * 
     * @param configFilePath 配置文件路径
     */
    public Mem0Config(String configFilePath) {
        this();
        loadFromFile(configFilePath);
    }
    
    /**
     * 通过Properties对象创建Mem0Config实例 / Create Mem0Config instance from Properties object
     * 
     * @param properties Properties对象
     */
    public Mem0Config(Properties properties) {
        this();
        loadFromProperties(properties);
    }
    
    /**
     * 从配置文件加载配置 / Load configuration from file
     * 
     * @param configFilePath 配置文件路径
     * @return 当前配置实例（支持链式调用）
     */
    public Mem0Config loadFromFile(String configFilePath) {
        logger.info("Loading Mem0 configuration from file: {}", configFilePath);
        
        try {
            // 尝试从文件系统加载
            Properties properties = loadPropertiesFromFile(configFilePath);
            if (properties != null && !properties.isEmpty()) {
                loadFromProperties(properties);
                return this;
            }
            
            // 尝试从类路径加载
            return loadFromClasspath(configFilePath);
            
        } catch (Exception e) {
            logger.warn("Failed to load configuration from file: {}, keeping current configuration. Error: {}", 
                       configFilePath, e.getMessage());
            return this;
        }
    }
    
    /**
     * 从类路径加载配置文件 / Load configuration from classpath
     * 
     * @param resourcePath 资源路径
     * @return 当前配置实例（支持链式调用）
     */
    public Mem0Config loadFromClasspath(String resourcePath) {
        logger.info("Loading Mem0 configuration from classpath: {}", resourcePath);
        
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                logger.warn("Configuration file not found in classpath: {}, keeping current configuration", resourcePath);
                return this;
            }
            
            Properties properties = new Properties();
            properties.load(inputStream);
            
            return loadFromProperties(properties);
            
        } catch (IOException e) {
            logger.error("Error loading configuration from classpath: {}, keeping current configuration", resourcePath, e);
            return this;
        }
    }
    
    /**
     * 从Properties对象加载配置 / Load configuration from Properties object
     * 
     * @param properties Properties对象
     * @return 当前配置实例（支持链式调用）
     */
    public Mem0Config loadFromProperties(Properties properties) {
        logger.info("Loading Mem0 configuration from Properties object");
        
        try {
            // 加载向量存储配置
            loadVectorStoreConfig(vectorStore, properties);
            
            // 加载图存储配置
            loadGraphStoreConfig(graphStore, properties);
            
            // 加载LLM配置
            loadLLMConfig(llm, properties);
            
            // 加载嵌入配置
            loadEmbeddingConfig(embedding, properties);
            
            logger.info("Successfully loaded Mem0 configuration with providers: vector={}, graph={}, llm={}, embedding={}", 
                       vectorStore.getProvider(), graphStore.getProvider(), llm.getProvider(), embedding.getProvider());
            
            return this;
            
        } catch (Exception e) {
            logger.error("Error parsing configuration properties, keeping current configuration", e);
            return this;
        }
    }
    
    /**
     * 从Map对象加载配置 / Load configuration from Map object
     * 
     * @param configMap 配置Map
     * @return 当前配置实例（支持链式调用）
     */
    public Mem0Config loadFromMap(Map<String, Object> configMap) {
        logger.info("Loading Mem0 configuration from Map object");
        
        Properties properties = new Properties();
        for (Map.Entry<String, Object> entry : configMap.entrySet()) {
            if (entry.getValue() != null) {
                properties.setProperty(entry.getKey(), entry.getValue().toString());
            }
        }
        
        return loadFromProperties(properties);
    }
    
    /**
     * 从环境变量和系统属性加载配置 / Load configuration from environment variables and system properties
     * 
     * @return 当前配置实例（支持链式调用）
     */
    public Mem0Config loadFromEnvironment() {
        logger.info("Loading Mem0 configuration from environment variables and system properties");
        
        Properties properties = new Properties();
        
        // 加载环境变量
        System.getenv().forEach((key, value) -> {
            if (key.startsWith("MEM0_")) {
                String propKey = key.toLowerCase().replace("_", ".");
                properties.setProperty(propKey, value);
            }
        });
        
        // 加载系统属性
        System.getProperties().forEach((key, value) -> {
            String keyStr = (String) key;
            if (keyStr.startsWith("mem0.")) {
                properties.setProperty(keyStr, (String) value);
            }
        });
        
        if (!properties.isEmpty()) {
            loadFromProperties(properties);
        }
        
        return this;
    }
    
    // ================== 静态工厂方法 / Static Factory Methods ==================
    
    /**
     * 从默认配置文件创建配置实例 / Create configuration instance from default configuration file
     * 
     * @return Mem0Config实例
     */
    public static Mem0Config fromDefaultFile() {
        return fromFile("mem0.properties");
    }
    
    /**
     * 从指定配置文件创建配置实例 / Create configuration instance from specified configuration file
     * 
     * @param configFilePath 配置文件路径
     * @return Mem0Config实例
     */
    public static Mem0Config fromFile(String configFilePath) {
        return new Mem0Config(configFilePath);
    }
    
    /**
     * 从类路径配置文件创建配置实例 / Create configuration instance from classpath configuration file
     * 
     * @param resourcePath 资源路径
     * @return Mem0Config实例
     */
    public static Mem0Config fromClasspath(String resourcePath) {
        return new Mem0Config().loadFromClasspath(resourcePath);
    }
    
    /**
     * 从Properties对象创建配置实例 / Create configuration instance from Properties object
     * 
     * @param properties Properties对象
     * @return Mem0Config实例
     */
    public static Mem0Config fromProperties(Properties properties) {
        return new Mem0Config(properties);
    }
    
    /**
     * 从Map对象创建配置实例 / Create configuration instance from Map object
     * 
     * @param configMap 配置Map
     * @return Mem0Config实例
     */
    public static Mem0Config fromMap(Map<String, Object> configMap) {
        return new Mem0Config().loadFromMap(configMap);
    }
    
    /**
     * 从环境变量创建配置实例 / Create configuration instance from environment variables
     * 
     * @return Mem0Config实例
     */
    public static Mem0Config fromEnvironment() {
        return new Mem0Config().loadFromEnvironment();
    }
    
    // ================== 私有辅助方法 / Private Helper Methods ==================
    
    private void loadVectorStoreConfig(VectorStoreConfig config, Properties properties) {
        String prefix = "mem0.vectorstore.";
        
        String provider = getProperty(properties, prefix + "provider", config.getProvider());
        config.setProvider(provider);
        
        String host = getProperty(properties, prefix + "host", config.getHost());
        config.setHost(host);
        
        int port = getIntProperty(properties, prefix + "port", config.getPort());
        config.setPort(port);
        
        String token = getProperty(properties, prefix + "token", config.getToken());
        config.setToken(token);
        
        String database = getProperty(properties, prefix + "database", config.getDatabase());
        config.setDatabase(database);
        
        // 加载额外配置
        Map<String, Object> additionalConfig = loadAdditionalConfig(properties, prefix);
        if (!additionalConfig.isEmpty()) {
            config.getAdditionalConfig().putAll(additionalConfig);
        }
        
        logger.debug("Loaded vector store config: provider={}, host={}, port={}", provider, host, port);
    }
    
    private void loadGraphStoreConfig(GraphStoreConfig config, Properties properties) {
        String prefix = "mem0.graphstore.";
        
        String provider = getProperty(properties, prefix + "provider", config.getProvider());
        config.setProvider(provider);
        
        String uri = getProperty(properties, prefix + "uri", config.getUri());
        config.setUri(uri);
        
        String username = getProperty(properties, prefix + "username", config.getUsername());
        config.setUsername(username);
        
        String password = getProperty(properties, prefix + "password", config.getPassword());
        config.setPassword(password);
        
        // 加载额外配置
        Map<String, Object> additionalConfig = loadAdditionalConfig(properties, prefix);
        if (!additionalConfig.isEmpty()) {
            config.getAdditionalConfig().putAll(additionalConfig);
        }
        
        logger.debug("Loaded graph store config: provider={}, uri={}, username={}", provider, uri, username);
    }
    
    private void loadLLMConfig(LLMConfig config, Properties properties) {
        String prefix = "mem0.llm.";
        
        String provider = getProperty(properties, prefix + "provider", config.getProvider());
        config.setProvider(provider);
        
        String apiKey = getProperty(properties, prefix + "apikey", config.getApiKey());
        if (apiKey == null) {
            apiKey = getProperty(properties, prefix + "api_key", config.getApiKey());
        }
        config.setApiKey(apiKey);
        
        String model = getProperty(properties, prefix + "model", config.getModel());
        config.setModel(model);
        
        double temperature = getDoubleProperty(properties, prefix + "temperature", config.getTemperature());
        config.setTemperature(temperature);
        
        int maxTokens = getIntProperty(properties, prefix + "max_tokens", config.getMaxTokens());
        if (maxTokens == config.getMaxTokens()) {
            maxTokens = getIntProperty(properties, prefix + "maxtokens", config.getMaxTokens());
        }
        config.setMaxTokens(maxTokens);
        
        // 加载额外配置
        Map<String, Object> additionalConfig = loadAdditionalConfig(properties, prefix);
        if (!additionalConfig.isEmpty()) {
            config.getAdditionalConfig().putAll(additionalConfig);
        }
        
        logger.debug("Loaded LLM config: provider={}, model={}, temperature={}, maxTokens={}", 
                    provider, model, temperature, maxTokens);
    }
    
    private void loadEmbeddingConfig(EmbeddingConfig config, Properties properties) {
        String prefix = "mem0.embedding.";
        
        String provider = getProperty(properties, prefix + "provider", config.getProvider());
        config.setProvider(provider);
        
        String apiKey = getProperty(properties, prefix + "apikey", config.getApiKey());
        if (apiKey == null) {
            apiKey = getProperty(properties, prefix + "api_key", config.getApiKey());
        }
        config.setApiKey(apiKey);
        
        String model = getProperty(properties, prefix + "model", config.getModel());
        config.setModel(model);
        
        // 加载额外配置
        Map<String, Object> additionalConfig = loadAdditionalConfig(properties, prefix);
        if (!additionalConfig.isEmpty()) {
            config.getAdditionalConfig().putAll(additionalConfig);
        }
        
        logger.debug("Loaded embedding config: provider={}, model={}", provider, model);
    }
    
    private Map<String, Object> loadAdditionalConfig(Properties properties, String prefix) {
        Map<String, Object> additionalConfig = new HashMap<>();
        
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                String configKey = key.substring(prefix.length());
                // 跳过已知的配置键
                if (!isKnownConfigKey(configKey)) {
                    additionalConfig.put(configKey, properties.getProperty(key));
                }
            }
        }
        
        return additionalConfig;
    }
    
    private boolean isKnownConfigKey(String key) {
        return key.equals("provider") || key.equals("host") || key.equals("port") || 
               key.equals("token") || key.equals("database") || key.equals("uri") ||
               key.equals("username") || key.equals("password") || key.equals("apikey") ||
               key.equals("api_key") || key.equals("model") || key.equals("temperature") ||
               key.equals("max_tokens") || key.equals("maxtokens");
    }
    
    private String getProperty(Properties properties, String key, String defaultValue) {
        // 首先检查系统属性
        String value = System.getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }
        
        // 然后检查环境变量
        String envKey = key.toUpperCase().replace(".", "_");
        value = System.getenv(envKey);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }
        
        // 最后检查Properties文件
        value = properties.getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }
        
        return defaultValue;
    }
    
    private int getIntProperty(Properties properties, String key, int defaultValue) {
        String value = getProperty(properties, key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer value for key {}: {}, using default: {}", key, value, defaultValue);
            return defaultValue;
        }
    }
    
    private double getDoubleProperty(Properties properties, String key, double defaultValue) {
        String value = getProperty(properties, key, String.valueOf(defaultValue));
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            logger.warn("Invalid double value for key {}: {}, using default: {}", key, value, defaultValue);
            return defaultValue;
        }
    }
    
    private Properties loadPropertiesFromFile(String filePath) {
        try {
            java.io.FileInputStream fis = new java.io.FileInputStream(filePath);
            Properties properties = new Properties();
            properties.load(fis);
            fis.close();
            return properties;
        } catch (Exception e) {
            logger.debug("Could not load properties from file system: {}", filePath);
            return null;
        }
    }
    
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