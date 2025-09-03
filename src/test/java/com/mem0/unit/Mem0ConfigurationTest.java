package com.mem0.unit;

import com.mem0.Mem0;
import com.mem0.config.Mem0Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Properties;
import java.util.HashMap;
import java.util.Map;

/**
 * Mem0配置构造函数测试
 * Test cases for Mem0 configuration-based constructors
 */
public class Mem0ConfigurationTest {
    
    private Properties testProperties;
    
    @BeforeEach
    void setUp() {
        testProperties = new Properties();
        testProperties.setProperty("mem0.vectorstore.provider", "milvus");
        testProperties.setProperty("mem0.vectorstore.host", "test-host");
        testProperties.setProperty("mem0.vectorstore.port", "19530");
        
        testProperties.setProperty("mem0.graphstore.provider", "neo4j");
        testProperties.setProperty("mem0.graphstore.uri", "bolt://test-host:7687");
        testProperties.setProperty("mem0.graphstore.username", "test-user");
        testProperties.setProperty("mem0.graphstore.password", "test-password");
        
        testProperties.setProperty("mem0.llm.provider", "qwen");
        testProperties.setProperty("mem0.llm.apikey", "test-llm-key");
        testProperties.setProperty("mem0.llm.model", "qwen-plus");
        
        testProperties.setProperty("mem0.embedding.provider", "aliyun");
        testProperties.setProperty("mem0.embedding.apikey", "test-embedding-key");
        testProperties.setProperty("mem0.embedding.model", "text-embedding-v1");
    }
    
    @Test
    void testDefaultConstructor() {
        Mem0 mem0 = new Mem0();
        assertNotNull(mem0);
        assertNotNull(mem0.getConfig());
        
        // 验证默认配置
        assertEquals("milvus", mem0.getConfig().getVectorStore().getProvider());
        assertEquals("localhost", mem0.getConfig().getVectorStore().getHost());
        assertEquals(19530, mem0.getConfig().getVectorStore().getPort());
        
        mem0.close();
    }
    
    @Test
    void testConfigFileConstructor() {
        // 测试从类路径配置文件构造
        Mem0 mem0 = new Mem0("test-mem0.properties");
        assertNotNull(mem0);
        assertNotNull(mem0.getConfig());
        
        // 验证配置被正确加载
        assertEquals("milvus", mem0.getConfig().getVectorStore().getProvider());
        assertEquals("localhost", mem0.getConfig().getVectorStore().getHost());
        assertEquals(19530, mem0.getConfig().getVectorStore().getPort());
        
        assertEquals("neo4j", mem0.getConfig().getGraphStore().getProvider());
        assertEquals("bolt://localhost:7687", mem0.getConfig().getGraphStore().getUri());
        assertEquals("neo4j", mem0.getConfig().getGraphStore().getUsername());
        assertEquals("neo4j123", mem0.getConfig().getGraphStore().getPassword());
        
        assertEquals("qwen", mem0.getConfig().getLlm().getProvider());
        assertEquals("sk-test-api-key-for-testing", mem0.getConfig().getLlm().getApiKey());
        assertEquals("qwen-plus", mem0.getConfig().getLlm().getModel());
        
        assertEquals("aliyun", mem0.getConfig().getEmbedding().getProvider());
        assertEquals("sk-test-embedding-api-key", mem0.getConfig().getEmbedding().getApiKey());
        assertEquals("text-embedding-v1", mem0.getConfig().getEmbedding().getModel());
        
        mem0.close();
    }
    
    @Test
    void testConfigFileConstructorWithNonExistentFile() {
        // 测试不存在的配置文件
        Mem0 mem0 = new Mem0("non-existent-config.properties");
        assertNotNull(mem0);
        assertNotNull(mem0.getConfig());
        
        // 应该使用默认配置
        assertEquals("localhost", mem0.getConfig().getVectorStore().getHost());
        
        mem0.close();
    }
    
    @Test
    void testBuilderWithFileConfiguration() {
        Mem0 mem0 = Mem0.builder()
            .loadFromFile("test-mem0.properties")
            .build();
            
        assertNotNull(mem0);
        assertNotNull(mem0.getConfig());
        
        // 验证配置被正确加载
        assertEquals("qwen", mem0.getConfig().getLlm().getProvider());
        assertEquals("sk-test-api-key-for-testing", mem0.getConfig().getLlm().getApiKey());
        
        mem0.close();
    }
    
    @Test
    void testBuilderWithClasspathConfiguration() {
        Mem0 mem0 = Mem0.builder()
            .loadFromClasspath("test-mem0.properties")
            .build();
            
        assertNotNull(mem0);
        assertNotNull(mem0.getConfig());
        
        // 验证配置被正确加载
        assertEquals("milvus", mem0.getConfig().getVectorStore().getProvider());
        assertEquals("neo4j", mem0.getConfig().getGraphStore().getProvider());
        
        mem0.close();
    }
    
    @Test
    void testBuilderWithPropertiesConfiguration() {
        Mem0 mem0 = Mem0.builder()
            .loadFromProperties(testProperties)
            .build();
            
        assertNotNull(mem0);
        assertNotNull(mem0.getConfig());
        
        // 验证配置被正确加载
        assertEquals("test-host", mem0.getConfig().getVectorStore().getHost());
        assertEquals("test-user", mem0.getConfig().getGraphStore().getUsername());
        assertEquals("test-llm-key", mem0.getConfig().getLlm().getApiKey());
        assertEquals("test-embedding-key", mem0.getConfig().getEmbedding().getApiKey());
        
        mem0.close();
    }
    
    @Test
    void testBuilderWithMapConfiguration() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("mem0.vectorstore.host", "map-host");
        configMap.put("mem0.vectorstore.port", "8080");
        configMap.put("mem0.llm.provider", "qwen");
        configMap.put("mem0.llm.apikey", "map-api-key");
        
        Mem0 mem0 = Mem0.builder()
            .loadFromMap(configMap)
            .build();
            
        assertNotNull(mem0);
        assertNotNull(mem0.getConfig());
        
        // 验证配置被正确加载
        assertEquals("map-host", mem0.getConfig().getVectorStore().getHost());
        assertEquals(8080, mem0.getConfig().getVectorStore().getPort());
        assertEquals("qwen", mem0.getConfig().getLlm().getProvider());
        assertEquals("map-api-key", mem0.getConfig().getLlm().getApiKey());
        
        mem0.close();
    }
    
    @Test
    void testBuilderWithEnvironmentConfiguration() {
        Mem0 mem0 = Mem0.builder()
            .loadFromEnvironment()
            .build();
            
        assertNotNull(mem0);
        assertNotNull(mem0.getConfig());
        
        // 环境变量配置依赖于运行环境，这里只验证实例创建成功
        mem0.close();
    }
    
    @Test
    void testBuilderWithCustomConfiguration() {
        Mem0Config customConfig = new Mem0Config();
        customConfig.getVectorStore().setHost("custom-host");
        customConfig.getVectorStore().setPort(9999);
        customConfig.getLlm().setProvider("custom-llm");
        
        Mem0 mem0 = Mem0.builder()
            .config(customConfig)
            .build();
            
        assertNotNull(mem0);
        assertNotNull(mem0.getConfig());
        
        // 验证自定义配置被正确使用
        assertEquals("custom-host", mem0.getConfig().getVectorStore().getHost());
        assertEquals(9999, mem0.getConfig().getVectorStore().getPort());
        assertEquals("custom-llm", mem0.getConfig().getLlm().getProvider());
        
        mem0.close();
    }
    
    @Test
    void testBuilderConfigurationOverride() {
        // 测试配置覆盖：先从文件加载，再用Properties覆盖
        Mem0 mem0 = Mem0.builder()
            .loadFromClasspath("test-mem0.properties")
            .loadFromProperties(testProperties) // 这个会覆盖文件中的配置
            .build();
            
        assertNotNull(mem0);
        
        // 验证Properties配置覆盖了文件配置
        assertEquals("test-host", mem0.getConfig().getVectorStore().getHost());
        assertEquals("test-user", mem0.getConfig().getGraphStore().getUsername());
        assertEquals("test-password", mem0.getConfig().getGraphStore().getPassword());
        
        mem0.close();
    }
    
    @Test
    void testBuilderMixedConfiguration() {
        // 测试混合配置：从文件加载 + 直接设置Provider
        Mem0 mem0 = Mem0.builder()
            .loadFromClasspath("test-mem0.properties")
            .vectorStore("custom-vector", "override-host", 7777)
            .build();
            
        assertNotNull(mem0);
        
        // 配置文件中的其他设置应该保留，但向量存储配置被覆盖
        assertEquals("custom-vector", mem0.getConfig().getVectorStore().getProvider());
        assertEquals("override-host", mem0.getConfig().getVectorStore().getHost());
        assertEquals(7777, mem0.getConfig().getVectorStore().getPort());
        
        // 其他配置来自文件
        assertEquals("qwen", mem0.getConfig().getLlm().getProvider());
        assertEquals("sk-test-api-key-for-testing", mem0.getConfig().getLlm().getApiKey());
        
        mem0.close();
    }
    
    @Test
    void testConfigurationIsolation() {
        // 测试配置隔离：不同实例使用不同配置
        Properties props1 = new Properties();
        props1.setProperty("mem0.vectorstore.host", "host1");
        
        Properties props2 = new Properties();
        props2.setProperty("mem0.vectorstore.host", "host2");
        
        Mem0 mem0_1 = Mem0.builder().loadFromProperties(props1).build();
        Mem0 mem0_2 = Mem0.builder().loadFromProperties(props2).build();
        
        // 验证配置隔离
        assertEquals("host1", mem0_1.getConfig().getVectorStore().getHost());
        assertEquals("host2", mem0_2.getConfig().getVectorStore().getHost());
        
        mem0_1.close();
        mem0_2.close();
    }
}