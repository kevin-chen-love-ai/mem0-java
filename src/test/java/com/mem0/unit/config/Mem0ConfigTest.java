package com.mem0.unit.config;

import com.mem0.config.Mem0Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.HashMap;
import java.util.Properties;

/**
 * Mem0Config配置加载功能测试
 * Test cases for Mem0Config configuration loading functionality
 */
public class Mem0ConfigTest {
    
    private Properties testProperties;
    private Map<String, Object> testConfigMap;
    
    @BeforeEach
    void setUp() {
        // 准备测试用的Properties对象
        testProperties = new Properties();
        testProperties.setProperty("mem0.vectorstore.provider", "milvus");
        testProperties.setProperty("mem0.vectorstore.host", "test-host");
        testProperties.setProperty("mem0.vectorstore.port", "9530");
        
        testProperties.setProperty("mem0.graphstore.provider", "neo4j");
        testProperties.setProperty("mem0.graphstore.uri", "bolt://test-host:7687");
        testProperties.setProperty("mem0.graphstore.username", "test-user");
        testProperties.setProperty("mem0.graphstore.password", "test-pass");
        
        testProperties.setProperty("mem0.llm.provider", "qwen");
        testProperties.setProperty("mem0.llm.apikey", "test-llm-key");
        testProperties.setProperty("mem0.llm.model", "qwen-test");
        testProperties.setProperty("mem0.llm.temperature", "0.8");
        testProperties.setProperty("mem0.llm.max_tokens", "2000");
        
        testProperties.setProperty("mem0.embedding.provider", "aliyun");
        testProperties.setProperty("mem0.embedding.apikey", "test-embedding-key");
        testProperties.setProperty("mem0.embedding.model", "test-embedding-model");
        
        // 准备测试用的Map对象
        testConfigMap = new HashMap<>();
        testConfigMap.put("mem0.vectorstore.provider", "milvus");
        testConfigMap.put("mem0.vectorstore.host", "map-host");
        testConfigMap.put("mem0.vectorstore.port", 8530);
        testConfigMap.put("mem0.llm.provider", "qwen");
        testConfigMap.put("mem0.llm.apikey", "map-llm-key");
    }
    
    @Test
    void testDefaultConfig() {
        Mem0Config config = new Mem0Config();
        
        // 验证默认配置
        assertNotNull(config.getVectorStore());
        assertNotNull(config.getGraphStore());
        assertNotNull(config.getLlm());
        assertNotNull(config.getEmbedding());
        
        assertEquals("milvus", config.getVectorStore().getProvider());
        assertEquals("localhost", config.getVectorStore().getHost());
        assertEquals(19530, config.getVectorStore().getPort());
        
        assertEquals("neo4j", config.getGraphStore().getProvider());
        assertEquals("bolt://localhost:7687", config.getGraphStore().getUri());
        assertEquals("neo4j", config.getGraphStore().getUsername());
        assertEquals("password", config.getGraphStore().getPassword());
    }
    
    @Test
    void testLoadFromProperties() {
        Mem0Config config = new Mem0Config();
        config.loadFromProperties(testProperties);
        
        // 验证向量存储配置
        assertEquals("milvus", config.getVectorStore().getProvider());
        assertEquals("test-host", config.getVectorStore().getHost());
        assertEquals(9530, config.getVectorStore().getPort());
        
        // 验证图存储配置
        assertEquals("neo4j", config.getGraphStore().getProvider());
        assertEquals("bolt://test-host:7687", config.getGraphStore().getUri());
        assertEquals("test-user", config.getGraphStore().getUsername());
        assertEquals("test-pass", config.getGraphStore().getPassword());
        
        // 验证LLM配置
        assertEquals("qwen", config.getLlm().getProvider());
        assertEquals("test-llm-key", config.getLlm().getApiKey());
        assertEquals("qwen-test", config.getLlm().getModel());
        assertEquals(0.8, config.getLlm().getTemperature(), 0.01);
        assertEquals(2000, config.getLlm().getMaxTokens());
        
        // 验证嵌入配置
        assertEquals("aliyun", config.getEmbedding().getProvider());
        assertEquals("test-embedding-key", config.getEmbedding().getApiKey());
        assertEquals("test-embedding-model", config.getEmbedding().getModel());
    }
    
    @Test
    void testLoadFromMap() {
        Mem0Config config = new Mem0Config();
        config.loadFromMap(testConfigMap);
        
        // 验证从Map加载的配置
        assertEquals("milvus", config.getVectorStore().getProvider());
        assertEquals("map-host", config.getVectorStore().getHost());
        assertEquals(8530, config.getVectorStore().getPort());
        
        assertEquals("qwen", config.getLlm().getProvider());
        assertEquals("map-llm-key", config.getLlm().getApiKey());
    }
    
    @Test
    void testPropertiesConstructor() {
        Mem0Config config = new Mem0Config(testProperties);
        
        // 验证通过Properties构造的配置
        assertEquals("test-host", config.getVectorStore().getHost());
        assertEquals("test-llm-key", config.getLlm().getApiKey());
    }
    
    @Test
    void testLoadFromClasspath() {
        Mem0Config config = new Mem0Config();
        config.loadFromClasspath("test-mem0.properties");
        
        // 验证从类路径加载的配置
        assertEquals("milvus", config.getVectorStore().getProvider());
        assertEquals("localhost", config.getVectorStore().getHost());
        assertEquals(19530, config.getVectorStore().getPort());
        
        assertEquals("neo4j", config.getGraphStore().getProvider());
        assertEquals("bolt://localhost:7687", config.getGraphStore().getUri());
        assertEquals("neo4j", config.getGraphStore().getUsername());
        assertEquals("neo4j123", config.getGraphStore().getPassword());
        
        assertEquals("qwen", config.getLlm().getProvider());
        assertEquals("sk-test-api-key-for-testing", config.getLlm().getApiKey());
        assertEquals("qwen-plus", config.getLlm().getModel());
        
        assertEquals("aliyun", config.getEmbedding().getProvider());
        assertEquals("sk-test-embedding-api-key", config.getEmbedding().getApiKey());
        assertEquals("text-embedding-v1", config.getEmbedding().getModel());
    }
    
    @Test
    void testStaticFactoryMethods() {
        // 测试静态工厂方法
        Mem0Config config1 = Mem0Config.fromProperties(testProperties);
        assertNotNull(config1);
        assertEquals("test-host", config1.getVectorStore().getHost());
        
        Mem0Config config2 = Mem0Config.fromMap(testConfigMap);
        assertNotNull(config2);
        assertEquals("map-host", config2.getVectorStore().getHost());
        
        Mem0Config config3 = Mem0Config.fromClasspath("test-mem0.properties");
        assertNotNull(config3);
        assertEquals("qwen", config3.getLlm().getProvider());
        
        Mem0Config config4 = Mem0Config.fromEnvironment();
        assertNotNull(config4);
        // 环境变量测试依赖于运行环境，这里只验证不为null
    }
    
    @Test
    void testChainedConfiguration() {
        // 测试链式配置调用
        Mem0Config config = new Mem0Config()
            .loadFromProperties(testProperties)
            .loadFromMap(testConfigMap); // Map配置会覆盖Properties配置
            
        // 验证最后的Map配置生效
        assertEquals("map-host", config.getVectorStore().getHost());
        assertEquals(8530, config.getVectorStore().getPort());
        assertEquals("map-llm-key", config.getLlm().getApiKey());
    }
    
    @Test
    void testConfigurationWithMissingFile() {
        // 测试加载不存在的配置文件
        Mem0Config config = new Mem0Config();
        Mem0Config result = config.loadFromFile("non-existent-file.properties");
        
        // 应该返回当前实例，保持默认配置
        assertSame(config, result);
        assertEquals("localhost", config.getVectorStore().getHost()); // 保持默认值
    }
    
    @Test
    void testConfigurationWithInvalidValues() {
        Properties invalidProps = new Properties();
        invalidProps.setProperty("mem0.vectorstore.port", "invalid-number");
        invalidProps.setProperty("mem0.llm.temperature", "invalid-double");
        invalidProps.setProperty("mem0.llm.max_tokens", "not-a-number");
        
        Mem0Config config = new Mem0Config();
        config.loadFromProperties(invalidProps);
        
        // 应该使用默认值
        assertEquals(19530, config.getVectorStore().getPort());
        assertEquals(0.7, config.getLlm().getTemperature(), 0.01);
        assertEquals(1000, config.getLlm().getMaxTokens());
    }
    
    @Test
    void testEnvironmentVariableOverride() {
        // 这个测试需要设置环境变量，在实际环境中可能不会通过
        // 但展示了如何测试环境变量覆盖功能
        Properties props = new Properties();
        props.setProperty("mem0.vectorstore.host", "props-host");
        
        // 如果有系统属性，应该覆盖Properties文件的值
        System.setProperty("mem0.vectorstore.host", "system-host");
        
        Mem0Config config = new Mem0Config();
        config.loadFromProperties(props);
        
        // 系统属性应该优先
        assertEquals("system-host", config.getVectorStore().getHost());
        
        // 清理系统属性
        System.clearProperty("mem0.vectorstore.host");
    }
    
    @Test 
    void testAdditionalConfiguration() {
        Properties props = new Properties();
        props.setProperty("mem0.vectorstore.custom.timeout", "30000");
        props.setProperty("mem0.vectorstore.custom.retries", "3");
        props.setProperty("mem0.llm.custom.endpoint", "https://custom.api.com");
        
        Mem0Config config = new Mem0Config();
        config.loadFromProperties(props);
        
        // 验证额外配置被正确加载
        Map<String, Object> vectorAdditional = config.getVectorStore().getAdditionalConfig();
        assertEquals("30000", vectorAdditional.get("custom.timeout"));
        assertEquals("3", vectorAdditional.get("custom.retries"));
        
        Map<String, Object> llmAdditional = config.getLlm().getAdditionalConfig();
        assertEquals("https://custom.api.com", llmAdditional.get("custom.endpoint"));
    }
}