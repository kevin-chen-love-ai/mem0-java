package com.mem0.util;

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
import com.mem0.config.Mem0Config;
import com.mem0.factory.ProviderFactory;

/**
 * 统一测试配置管理器 - Unified Test Configuration Manager
 * 
 * 提供统一的测试配置和Provider实例管理，避免在每个测试类中重复配置Provider。
 * 所有测试都可以通过这个类获取统一配置的Provider实例。
 * 
 * Provides unified test configuration and Provider instance management, 
 * avoiding duplicate Provider configuration in each test class.
 * All tests can obtain uniformly configured Provider instances through this class.
 * 
 * <h3>使用方式 / Usage:</h3>
 * <pre>{@code
 * // 基本使用 - Basic usage
 * LLMProvider llmProvider = TestConfiguration.getLLMProvider();
 * EmbeddingProvider embeddingProvider = TestConfiguration.getEmbeddingProvider();
 * 
 * // 创建组合服务 - Create composite services
 * MemoryClassifier classifier = TestConfiguration.createMemoryClassifier();
 * MemoryConflictDetector detector = TestConfiguration.createConflictDetector();
 * 
 * // 检查可用性 - Check availability
 * if (TestConfiguration.isLLMProviderAvailable()) {
 *     // 使用真实LLM进行测试
 * }
 * }</pre>
 * 
 * <h3>配置管理 / Configuration Management:</h3>
 * <ul>
 *   <li>支持环境变量配置 / Supports environment variable configuration</li>
 *   <li>支持系统属性配置 / Supports system property configuration</li>
 *   <li>提供默认配置 / Provides default configuration</li>
 *   <li>单例模式管理Provider实例 / Singleton pattern for Provider instances</li>
 *   <li>自动服务组合和依赖注入 / Automatic service composition and dependency injection</li>
 * </ul>
 * 
 * <h3>环境配置示例 / Environment Configuration Examples:</h3>
 * <pre>
 * # 系统属性 / System Properties
 * -Dtest.llm.apikey=your-llm-key
 * -Dtest.embedding.apikey=your-embedding-key
 * -Dtest.real.providers=true
 * 
 * # 环境变量 / Environment Variables
 * export TEST_LLM_APIKEY=your-llm-key
 * export TEST_EMBEDDING_APIKEY=your-embedding-key
 * export TEST_REAL_PROVIDERS=true
 * </pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class TestConfiguration {
    
    // 单例实例
    private static volatile TestConfiguration instance;
    
    // Provider实例
    private volatile LLMProvider llmProvider;
    private volatile EmbeddingProvider embeddingProvider;
    private volatile VectorStore vectorStore;
    private volatile GraphStore graphStore;
    
    // 配置常量
    private static final String DEFAULT_LLM_API_KEY = "sk-40147383d7a54917a5be08964e2d5a2f";
    private static final String DEFAULT_EMBEDDING_API_KEY = "sk-40147383d7a54917a5be08964e2d5a2f";
    private static final String DEFAULT_VECTOR_HOST = "localhost:19530";
    private static final String DEFAULT_GRAPH_URI = "bolt://localhost:7687";
    private static final String DEFAULT_GRAPH_USERNAME = "neo4j";
    private static final String DEFAULT_GRAPH_PASSWORD = "neo4j123";
    
    private TestConfiguration() {
        // 私有构造函数
    }
    
    /**
     * 获取TestConfiguration单例实例
     * 
     * @return TestConfiguration实例
     */
    public static TestConfiguration getInstance() {
        if (instance == null) {
            synchronized (TestConfiguration.class) {
                if (instance == null) {
                    instance = new TestConfiguration();
                }
            }
        }
        return instance;
    }
    
    /**
     * 获取LLM Provider实例
     * 
     * @return LLMProvider实例
     */
    public static LLMProvider getLLMProvider() {
        return getInstance().getLLMProviderInternal();
    }
    
    /**
     * 获取Embedding Provider实例
     * 
     * @return EmbeddingProvider实例
     */
    public static EmbeddingProvider getEmbeddingProvider() {
        return getInstance().getEmbeddingProviderInternal();
    }
    
    /**
     * 获取Vector Store实例
     * 
     * @return VectorStore实例
     */
    public static VectorStore getVectorStore() {
        return getInstance().getVectorStoreInternal();
    }
    
    /**
     * 获取Graph Store实例
     * 
     * @return GraphStore实例
     */
    public static GraphStore getGraphStore() {
        return getInstance().getGraphStoreInternal();
    }
    
    /**
     * 检查是否启用真实Provider测试
     * 
     * @return true如果启用真实Provider测试
     */
    public static boolean isRealProviderEnabled() {
        String enabled = System.getProperty("test.real.providers", 
                         System.getenv("TEST_REAL_PROVIDERS"));
        return "true".equalsIgnoreCase(enabled);
    }
    
    /**
     * 检查是否跳过需要外部服务的测试
     * 
     * @return true如果应该跳过外部服务测试
     */
    public static boolean shouldSkipExternalTests() {
        String skip = System.getProperty("test.skip.external", 
                      System.getenv("TEST_SKIP_EXTERNAL"));
        return "true".equalsIgnoreCase(skip);
    }
    
    /**
     * 检查LLM Provider是否可用
     * 
     * @return true如果LLM Provider可用
     */
    public static boolean isLLMProviderAvailable() {
        return getLLMProvider() != null;
    }
    
    /**
     * 检查Embedding Provider是否可用
     * 
     * @return true如果Embedding Provider可用
     */
    public static boolean isEmbeddingProviderAvailable() {
        return getEmbeddingProvider() != null;
    }
    
    /**
     * 检查所有Provider是否都可用
     * 
     * @return true如果所有Provider都可用
     */
    public static boolean areAllProvidersAvailable() {
        return isLLMProviderAvailable() && isEmbeddingProviderAvailable();
    }
    
    /**
     * 关闭所有Provider资源
     */
    public static void closeAll() {
        TestConfiguration config = getInstance();
        
        if (config.llmProvider != null) {
            try {
                config.llmProvider.close();
            } catch (Exception e) {
                System.err.println("Warning: Error closing LLM provider: " + e.getMessage());
            }
            config.llmProvider = null;
        }
        
        if (config.embeddingProvider != null) {
            try {
                config.embeddingProvider.close();
            } catch (Exception e) {
                System.err.println("Warning: Error closing Embedding provider: " + e.getMessage());
            }
            config.embeddingProvider = null;
        }
        
        if (config.vectorStore != null) {
            try {
                config.vectorStore.close();
            } catch (Exception e) {
                System.err.println("Warning: Error closing Vector store: " + e.getMessage());
            }
            config.vectorStore = null;
        }
        
        if (config.graphStore != null) {
            try {
                config.graphStore.close();
            } catch (Exception e) {
                System.err.println("Warning: Error closing Graph store: " + e.getMessage());
            }
            config.graphStore = null;
        }
    }
    
    // 内部实现方法
    private LLMProvider getLLMProviderInternal() {
        if (llmProvider == null) {
            synchronized (this) {
                if (llmProvider == null) {
                    try {
                        // Use mock provider by default for tests to avoid external API calls
                        if (isRealProviderEnabled()) {
                            String apiKey = getConfigValue("llm.apikey", DEFAULT_LLM_API_KEY);
                            llmProvider = new QwenLLMProvider(apiKey);
                        } else {
                            // Use RuleBasedLLMProvider as mock for testing
                            llmProvider = new RuleBasedLLMProvider();
                        }
                    } catch (Exception e) {
                        System.err.println("Warning: Could not initialize LLM provider: " + e.getMessage());
                        // Fallback to RuleBasedLLMProvider
                        try {
                            llmProvider = new RuleBasedLLMProvider();
                        } catch (Exception ex) {
                            return null;
                        }
                    }
                }
            }
        }
        return llmProvider;
    }
    
    private EmbeddingProvider getEmbeddingProviderInternal() {
        if (embeddingProvider == null) {
            synchronized (this) {
                if (embeddingProvider == null) {
                    try {
                        // Use mock provider by default for tests to avoid external API calls
                        if (isRealProviderEnabled()) {
                            String apiKey = getConfigValue("embedding.apikey", DEFAULT_EMBEDDING_API_KEY);
                            embeddingProvider = new AliyunEmbeddingProvider(apiKey);
                        } else {
                            // Use SimpleTFIDFEmbeddingProvider as mock for testing
                            embeddingProvider = new SimpleTFIDFEmbeddingProvider();
                        }
                    } catch (Exception e) {
                        System.err.println("Warning: Could not initialize Embedding provider: " + e.getMessage());
                        // Fallback to SimpleTFIDFEmbeddingProvider
                        try {
                            embeddingProvider = new SimpleTFIDFEmbeddingProvider();
                        } catch (Exception ex) {
                            return null;
                        }
                    }
                }
            }
        }
        return embeddingProvider;
    }
    
    private VectorStore getVectorStoreInternal() {
        if (vectorStore == null) {
            synchronized (this) {
                if (vectorStore == null) {
                    try {
                        // 对于测试，优先使用内存存储，除非明确配置了外部存储
                        String host = getConfigValue("vectorstore.host", null);
                        if (host != null && !host.isEmpty()) {
                            // 这里可以扩展支持MilvusVectorStore
                            System.out.println("External vector store not implemented in tests, using InMemoryVectorStore");
                        }
                        vectorStore = new InMemoryVectorStore();
                    } catch (Exception e) {
                        System.err.println("Warning: Could not initialize Vector store: " + e.getMessage());
                        return new InMemoryVectorStore(); // 总是返回内存存储作为fallback
                    }
                }
            }
        }
        return vectorStore;
    }
    
    private GraphStore getGraphStoreInternal() {
        if (graphStore == null) {
            synchronized (this) {
                if (graphStore == null) {
                    try {
                        // 对于测试，优先使用内存存储，除非明确配置了外部存储
                        String uri = getConfigValue("graphstore.uri", null);
                        if (uri != null && !uri.isEmpty()) {
                            // 这里可以扩展支持Neo4jGraphStore
                            System.out.println("External graph store not implemented in tests, using InMemoryGraphStore");
                        }
                        graphStore = new DefaultInMemoryGraphStore();
                    } catch (Exception e) {
                        System.err.println("Warning: Could not initialize Graph store: " + e.getMessage());
                        return new DefaultInMemoryGraphStore(); // 总是返回内存存储作为fallback
                    }
                }
            }
        }
        return graphStore;
    }
    
    /**
     * 获取配置值，支持系统属性和环境变量
     */
    private String getConfigValue(String key, String defaultValue) {
        // 优先级: 系统属性 > 环境变量 > 默认值
        String systemProperty = "test." + key;
        String envVariable = "TEST_" + key.toUpperCase().replace(".", "_");
        
        String value = System.getProperty(systemProperty);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        
        value = System.getenv(envVariable);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        
        return defaultValue;
    }
    
    /**
     * 打印当前测试配置信息
     */
    public static void printConfiguration() {
        System.out.println("=== Test Configuration ===");
        System.out.println("Real Providers Enabled: " + isRealProviderEnabled());
        System.out.println("Skip External Tests: " + shouldSkipExternalTests());
        
        TestConfiguration config = getInstance();
        System.out.println("LLM API Key: " + maskApiKey(config.getConfigValue("llm.apikey", DEFAULT_LLM_API_KEY)));
        System.out.println("Embedding API Key: " + maskApiKey(config.getConfigValue("embedding.apikey", DEFAULT_EMBEDDING_API_KEY)));
        System.out.println("Vector Store Host: " + config.getConfigValue("vectorstore.host", "InMemory"));
        System.out.println("Graph Store URI: " + config.getConfigValue("graphstore.uri", "InMemory"));
        System.out.println("========================");
    }
    
    private static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
    
    // ====================================================================================
    // 组合服务创建方法 - Composite Service Creation Methods
    // ====================================================================================
    
    /**
     * 创建MemoryClassifier实例
     * 
     * @return MemoryClassifier实例，如果LLM Provider不可用则返回null
     */
    public static com.mem0.core.MemoryClassifier createMemoryClassifier() {
        LLMProvider llmProvider = getLLMProvider();
        if (llmProvider != null) {
            return new com.mem0.core.MemoryClassifier(llmProvider);
        }
        return null;
    }
    
    /**
     * 创建MemoryConflictDetector实例
     * 
     * @return MemoryConflictDetector实例，如果必要的Provider不可用则返回null
     */
    public static com.mem0.core.MemoryConflictDetector createConflictDetector() {
        EmbeddingProvider embeddingProvider = getEmbeddingProvider();
        LLMProvider llmProvider = getLLMProvider();
        if (embeddingProvider != null && llmProvider != null) {
            return new com.mem0.core.MemoryConflictDetector(embeddingProvider, llmProvider);
        }
        return null;
    }
    
    /**
     * 创建MemoryMergeStrategy实例
     * 
     * @return MemoryMergeStrategy实例，如果LLM Provider不可用则返回null
     */
    public static com.mem0.core.MemoryMergeStrategy createMergeStrategy() {
        LLMProvider llmProvider = getLLMProvider();
        if (llmProvider != null) {
            return new com.mem0.core.MemoryMergeStrategy(llmProvider);
        }
        return null;
    }
    
    /**
     * 创建MemoryImportanceScorer实例
     * 
     * @return MemoryImportanceScorer实例，如果LLM Provider不可用则返回null
     */
    public static com.mem0.core.MemoryImportanceScorer createImportanceScorer() {
        LLMProvider llmProvider = getLLMProvider();
        if (llmProvider != null) {
            return new com.mem0.core.MemoryImportanceScorer(llmProvider);
        }
        return null;
    }
    
    /**
     * 创建完整的Mem0Config配置
     * 
     * @return 配置好的Mem0Config实例
     */
    public static com.mem0.config.Mem0Config createMem0Config() {
        com.mem0.config.Mem0Config config = new com.mem0.config.Mem0Config();
        
        // 配置使用内存存储进行测试
        config.getVectorStore().setProvider("inmemory");
        config.getGraphStore().setProvider("inmemory");
        
        // 根据Provider可用性配置LLM和Embedding
        if (isLLMProviderAvailable()) {
            config.getLlm().setProvider("qwen");
            config.getLlm().setApiKey(getInstance().getConfigValue("llm.apikey", DEFAULT_LLM_API_KEY));
        } else {
            config.getLlm().setProvider("mock");
        }
        
        if (isEmbeddingProviderAvailable()) {
            config.getEmbedding().setProvider("aliyun");
            config.getEmbedding().setApiKey(getInstance().getConfigValue("embedding.apikey", DEFAULT_EMBEDDING_API_KEY));
        } else {
            config.getEmbedding().setProvider("mock");
        }
        
        return config;
    }
    
    /**
     * 跳过测试的标准方法，提供统一的跳过逻辑
     * 
     * @param testName 测试名称
     * @param reason 跳过原因
     */
    public static void skipTest(String testName, String reason) {
        System.out.println("Skipping " + testName + " - " + reason);
    }
    
    /**
     * 检查测试是否应该跳过（Provider不可用）
     * 
     * @param testName 测试名称
     * @param requireLLM 是否需要LLM Provider
     * @param requireEmbedding 是否需要Embedding Provider
     * @return true如果应该跳过测试
     */
    public static boolean shouldSkipTest(String testName, boolean requireLLM, boolean requireEmbedding) {
        if (requireLLM && !isLLMProviderAvailable()) {
            skipTest(testName, "LLM provider not available");
            return true;
        }
        if (requireEmbedding && !isEmbeddingProviderAvailable()) {
            skipTest(testName, "Embedding provider not available");
            return true;
        }
        return false;
    }
}