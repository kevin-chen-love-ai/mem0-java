package com.mem0.examples;

import com.mem0.Mem0;
import com.mem0.config.Mem0Config;
import com.mem0.core.EnhancedMemory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Mem0配置使用示例 / Mem0 Configuration Usage Examples
 * 
 * 演示如何通过不同方式配置和初始化Mem0实例，包括配置文件、环境变量、
 * Properties对象等多种配置方法。
 * 
 * Demonstrates how to configure and initialize Mem0 instances through various methods,
 * including configuration files, environment variables, Properties objects, etc.
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class ConfigurationExample {
    
    public static void main(String[] args) {
        System.out.println("=== Mem0 Configuration Examples ===");
        
        try {
            // 示例1: 默认配置
            example1_DefaultConfiguration();
            
            // 示例2: 从配置文件加载
            example2_ConfigurationFromFile();
            
            // 示例3: 从类路径加载配置
            example3_ConfigurationFromClasspath();
            
            // 示例4: 从Properties对象加载
            example4_ConfigurationFromProperties();
            
            // 示例5: 从Map对象加载
            example5_ConfigurationFromMap();
            
            // 示例6: 使用Builder模式配置
            example6_BuilderConfiguration();
            
            // 示例7: 混合配置方式
            example7_MixedConfiguration();
            
            // 示例8: 环境变量配置
            example8_EnvironmentConfiguration();
            
            // 示例9: 配置文件与直接配置结合
            example9_HybridConfiguration();
            
            System.out.println("\\n=== All examples completed successfully! ===");
            
        } catch (Exception e) {
            System.err.println("Error running examples: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 示例1: 使用默认配置
     */
    private static void example1_DefaultConfiguration() {
        System.out.println("\\n--- Example 1: Default Configuration ---");
        
        // 使用默认配置创建Mem0实例
        try (Mem0 mem0 = new Mem0()) {
            System.out.println("Created Mem0 with default configuration");
            
            // 显示配置信息
            Mem0Config config = mem0.getConfig();
            System.out.println("Vector Store Provider: " + config.getVectorStore().getProvider());
            System.out.println("Graph Store Provider: " + config.getGraphStore().getProvider());
            System.out.println("LLM Provider: " + config.getLlm().getProvider());
            System.out.println("Embedding Provider: " + config.getEmbedding().getProvider());
            
            // 基本功能测试
            demonstrateBasicFunctionality(mem0, "Example 1");
        }
    }
    
    /**
     * 示例2: 从配置文件加载
     */
    private static void example2_ConfigurationFromFile() {
        System.out.println("\\n--- Example 2: Configuration from File ---");
        
        // 从文件路径加载配置（如果文件不存在，会使用默认配置）
        try (Mem0 mem0 = new Mem0("config/mem0-example.properties")) {
            System.out.println("Created Mem0 from configuration file");
            
            // 显示配置信息
            displayConfiguration(mem0, "File Configuration");
            
            // 基本功能测试
            demonstrateBasicFunctionality(mem0, "Example 2");
        }
    }
    
    /**
     * 示例3: 从类路径加载配置
     */
    private static void example3_ConfigurationFromClasspath() {
        System.out.println("\\n--- Example 3: Configuration from Classpath ---");
        
        // 使用Builder从类路径加载
        try (Mem0 mem0 = Mem0.builder()
                .loadFromClasspath("mem0-example.properties")
                .build()) {
            
            System.out.println("Created Mem0 from classpath configuration");
            displayConfiguration(mem0, "Classpath Configuration");
            demonstrateBasicFunctionality(mem0, "Example 3");
        }
    }
    
    /**
     * 示例4: 从Properties对象加载
     */
    private static void example4_ConfigurationFromProperties() {
        System.out.println("\\n--- Example 4: Configuration from Properties ---");
        
        // 创建Properties对象
        Properties props = new Properties();
        props.setProperty("mem0.vectorstore.provider", "milvus");
        props.setProperty("mem0.vectorstore.host", "custom-host");
        props.setProperty("mem0.vectorstore.port", "9530");
        
        props.setProperty("mem0.graphstore.provider", "neo4j");
        props.setProperty("mem0.graphstore.uri", "bolt://custom-neo4j:7687");
        props.setProperty("mem0.graphstore.username", "custom-user");
        props.setProperty("mem0.graphstore.password", "custom-pass");
        
        props.setProperty("mem0.llm.provider", "qwen");
        props.setProperty("mem0.llm.apikey", "custom-llm-key");
        props.setProperty("mem0.llm.model", "qwen-turbo");
        props.setProperty("mem0.llm.temperature", "0.5");
        
        props.setProperty("mem0.embedding.provider", "aliyun");
        props.setProperty("mem0.embedding.apikey", "custom-embedding-key");
        props.setProperty("mem0.embedding.model", "text-embedding-v2");
        
        // 从Properties创建配置
        try (Mem0 mem0 = Mem0.builder()
                .loadFromProperties(props)
                .build()) {
            
            System.out.println("Created Mem0 from Properties object");
            displayConfiguration(mem0, "Properties Configuration");
            demonstrateBasicFunctionality(mem0, "Example 4");
        }
    }
    
    /**
     * 示例5: 从Map对象加载
     */
    private static void example5_ConfigurationFromMap() {
        System.out.println("\\n--- Example 5: Configuration from Map ---");
        
        // 创建配置Map
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("mem0.vectorstore.provider", "milvus");
        configMap.put("mem0.vectorstore.host", "map-host");
        configMap.put("mem0.vectorstore.port", 8530);
        
        configMap.put("mem0.llm.provider", "qwen");
        configMap.put("mem0.llm.apikey", "map-api-key");
        configMap.put("mem0.llm.temperature", 0.8);
        configMap.put("mem0.llm.max_tokens", 2000);
        
        // 从Map创建配置
        try (Mem0 mem0 = Mem0.builder()
                .loadFromMap(configMap)
                .build()) {
            
            System.out.println("Created Mem0 from Map object");
            displayConfiguration(mem0, "Map Configuration");
            demonstrateBasicFunctionality(mem0, "Example 5");
        }
    }
    
    /**
     * 示例6: 使用Builder模式配置
     */
    private static void example6_BuilderConfiguration() {
        System.out.println("\\n--- Example 6: Builder Configuration ---");
        
        // 使用Builder直接设置配置
        try (Mem0 mem0 = Mem0.builder()
                .vectorStore("milvus", "builder-host", 7777)
                .graphStore("neo4j", "bolt://builder-neo4j:7687", "builder-user", "builder-pass")
                .llm("qwen", "builder-llm-key", "qwen-max")
                .embedding("aliyun", "builder-embedding-key", "text-embedding-v3")
                .build()) {
            
            System.out.println("Created Mem0 using Builder pattern");
            displayConfiguration(mem0, "Builder Configuration");
            demonstrateBasicFunctionality(mem0, "Example 6");
        }
    }
    
    /**
     * 示例7: 混合配置方式
     */
    private static void example7_MixedConfiguration() {
        System.out.println("\\n--- Example 7: Mixed Configuration ---");
        
        // 先从文件加载，再用Properties覆盖
        Properties overrideProps = new Properties();
        overrideProps.setProperty("mem0.vectorstore.host", "override-host");
        overrideProps.setProperty("mem0.llm.temperature", "0.9");
        
        try (Mem0 mem0 = Mem0.builder()
                .loadFromClasspath("mem0-example.properties")  // 从文件加载基础配置
                .loadFromProperties(overrideProps)             // 用Properties覆盖特定配置
                .build()) {
            
            System.out.println("Created Mem0 with mixed configuration (file + properties override)");
            displayConfiguration(mem0, "Mixed Configuration");
            demonstrateBasicFunctionality(mem0, "Example 7");
        }
    }
    
    /**
     * 示例8: 环境变量配置
     */
    private static void example8_EnvironmentConfiguration() {
        System.out.println("\\n--- Example 8: Environment Configuration ---");
        
        // 设置一些系统属性作为示例
        System.setProperty("mem0.vectorstore.host", "env-host");
        System.setProperty("mem0.llm.provider", "qwen");
        
        try (Mem0 mem0 = Mem0.builder()
                .loadFromEnvironment()  // 从环境变量和系统属性加载
                .build()) {
            
            System.out.println("Created Mem0 from environment variables and system properties");
            displayConfiguration(mem0, "Environment Configuration");
            demonstrateBasicFunctionality(mem0, "Example 8");
        } finally {
            // 清理系统属性
            System.clearProperty("mem0.vectorstore.host");
            System.clearProperty("mem0.llm.provider");
        }
    }
    
    /**
     * 示例9: 配置文件与直接配置结合
     */
    private static void example9_HybridConfiguration() {
        System.out.println("\\n--- Example 9: Hybrid Configuration ---");
        
        // 从配置文件加载基础配置，然后用Builder方法覆盖特定设置
        try (Mem0 mem0 = Mem0.builder()
                .loadFromClasspath("mem0-example.properties")       // 加载基础配置
                .vectorStore("milvus", "hybrid-host", 6666)         // 覆盖向量存储设置
                .llm("qwen", "hybrid-api-key")                      // 覆盖LLM设置
                .build()) {
            
            System.out.println("Created Mem0 with hybrid configuration (file + direct builder methods)");
            displayConfiguration(mem0, "Hybrid Configuration");
            demonstrateBasicFunctionality(mem0, "Example 9");
        }
    }
    
    /**
     * 显示配置信息
     */
    private static void displayConfiguration(Mem0 mem0, String configType) {
        Mem0Config config = mem0.getConfig();
        System.out.println("[" + configType + "]");
        System.out.println("  Vector Store: " + config.getVectorStore().getProvider() + 
                          " @ " + config.getVectorStore().getHost() + ":" + config.getVectorStore().getPort());
        System.out.println("  Graph Store: " + config.getGraphStore().getProvider() + 
                          " @ " + config.getGraphStore().getUri());
        System.out.println("  LLM: " + config.getLlm().getProvider() + 
                          " (model: " + config.getLlm().getModel() + 
                          ", temp: " + config.getLlm().getTemperature() + ")");
        System.out.println("  Embedding: " + config.getEmbedding().getProvider() + 
                          " (model: " + config.getEmbedding().getModel() + ")");
    }
    
    /**
     * 演示基本功能
     */
    private static void demonstrateBasicFunctionality(Mem0 mem0, String exampleName) {
        try {
            String userId = "demo-user-" + System.currentTimeMillis();
            
            // 添加内存
            String memoryId = mem0.add("这是一个配置示例的测试内存", userId).get();
            System.out.println("  ✓ Added memory: " + memoryId);
            
            // 搜索内存
            List<EnhancedMemory> results = mem0.search("配置", userId, 5).get();
            System.out.println("  ✓ Found " + results.size() + " memories");
            
            // 删除内存
            mem0.delete(memoryId).get();
            System.out.println("  ✓ Deleted memory successfully");
            
        } catch (Exception e) {
            System.out.println("  ✗ Error in " + exampleName + ": " + e.getMessage());
        }
    }
    
    /**
     * 配置文件格式示例
     */
    public static void printConfigurationFormat() {
        System.out.println("\\n=== Configuration File Format Example ===");
        System.out.println("""
            # mem0.properties 配置文件示例
            
            # Vector Store Configuration
            mem0.vectorstore.provider=milvus
            mem0.vectorstore.host=localhost
            mem0.vectorstore.port=19530
            mem0.vectorstore.token=your-token
            
            # Graph Store Configuration
            mem0.graphstore.provider=neo4j
            mem0.graphstore.uri=bolt://localhost:7687
            mem0.graphstore.username=neo4j
            mem0.graphstore.password=password
            
            # LLM Configuration
            mem0.llm.provider=qwen
            mem0.llm.apikey=your-api-key
            mem0.llm.model=qwen-plus
            mem0.llm.temperature=0.7
            mem0.llm.max_tokens=1000
            
            # Embedding Configuration
            mem0.embedding.provider=aliyun
            mem0.embedding.apikey=your-embedding-key
            mem0.embedding.model=text-embedding-v1
            
            # Additional Custom Configuration
            mem0.vectorstore.custom.timeout=30000
            mem0.llm.custom.base_url=https://custom-api.com
            """);
    }
}