package com.mem0.config;

import com.mem0.Mem0;
import com.mem0.config.Mem0Config;
import com.mem0.core.*;
import com.mem0.embedding.EmbeddingProvider;
import com.mem0.embedding.impl.OpenAIEmbeddingProvider;
import com.mem0.embedding.impl.SimpleTFIDFEmbeddingProvider;
import com.mem0.llm.LLMProvider;
import com.mem0.llm.impl.RuleBasedLLMProvider;
import com.mem0.llm.OpenAIProvider;
import com.mem0.store.VectorStore;
import com.mem0.store.GraphStore;
import com.mem0.store.Neo4jGraphStore;
import com.mem0.store.MilvusVectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Mem0 Spring Boot自动配置类
 * Mem0 Spring Boot Auto-Configuration Class
 * 
 * 该类提供Mem0框架与Spring Boot的自动集成配置，自动创建和配置所有必要的Bean，
 * 包括存储提供商、AI服务、内存处理组件等，实现开箱即用的Spring Boot集成。
 * 
 * This class provides automatic integration configuration for the Mem0 framework with Spring Boot,
 * automatically creating and configuring all necessary beans including storage providers,
 * AI services, memory processing components, enabling out-of-the-box Spring Boot integration.
 * 
 * 主要功能 / Key Features:
 * • 自动Bean配置 / Automatic bean configuration
 * • 条件化配置 / Conditional configuration
 * • 多提供商支持 / Multi-provider support
 * • 配置属性绑定 / Configuration property binding
 * • 依赖注入管理 / Dependency injection management
 * • 启动顺序控制 / Startup order control
 * 
 * 配置示例 / Configuration Example:
 * <pre>{@code
 * # application.yml 自动配置示例
 * mem0:
 *   vector-store:
 *     provider: milvus
 *     host: localhost
 *     port: 19530
 *   graph-store:
 *     provider: neo4j
 *     uri: bolt://localhost:7687
 *     username: neo4j
 *     password: password
 *   llm:
 *     provider: openai
 *     api-key: ${OPENAI_API_KEY}
 *     model: gpt-3.5-turbo
 *   embedding:
 *     provider: openai
 *     api-key: ${OPENAI_API_KEY}
 *     model: text-embedding-ada-002
 * }</pre>
 * 
 * 使用示例 / Usage Example:
 * <pre>{@code
 * // Spring Boot应用中使用Mem0 / Using Mem0 in Spring Boot application
 * @RestController
 * public class MemoryController {
 *     
 *     @Autowired
 *     private Mem0 mem0;
 *     
 *     @PostMapping("/memories")
 *     public String addMemory(@RequestBody String content) {
 *         return mem0.add(content, "user-123");
 *     }
 *     
 *     @GetMapping("/memories/{userId}")
 *     public List<Memory> getMemories(@PathVariable String userId) {
 *         return mem0.getAll(userId);
 *     }
 * }
 * 
 * // 自定义配置覆盖 / Custom configuration override
 * @Configuration
 * public class CustomMem0Configuration {
 *     
 *     @Bean
 *     @Primary
 *     public LLMProvider customLLMProvider() {
 *         return new CustomLLMProvider();
 *     }
 * }
 * }</pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
@Configuration
@ConditionalOnClass(Mem0.class)
@EnableConfigurationProperties(Mem0Properties.class)
@AutoConfigureAfter(name = {"org.springframework.boot.autoconfigure.data.neo4j.Neo4jDataAutoConfiguration"})
public class Mem0AutoConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(Mem0AutoConfiguration.class);
    
    @Bean
    @ConditionalOnMissingBean
    public Mem0Config mem0Config(Mem0Properties properties, Environment environment) {
        logger.info("Creating Mem0Config from properties");
        
        Mem0Config config = new Mem0Config();
        
        // Vector store configuration
        config.getVectorStore().setProvider(properties.getVectorStore().getProvider());
        config.getVectorStore().setHost(resolveProperty(environment, "mem0.vector-store.host", 
            properties.getVectorStore().getHost()));
        config.getVectorStore().setPort(Integer.parseInt(resolveProperty(environment, "mem0.vector-store.port", 
            String.valueOf(properties.getVectorStore().getPort()))));
        config.getVectorStore().setToken(resolveProperty(environment, "mem0.vector-store.token", 
            properties.getVectorStore().getToken()));
        config.getVectorStore().setDatabase(resolveProperty(environment, "mem0.vector-store.database", 
            properties.getVectorStore().getDatabase()));
        config.getVectorStore().setAdditionalConfig(properties.getVectorStore().getAdditionalConfig());
        
        // Graph store configuration
        config.getGraphStore().setProvider(properties.getGraphStore().getProvider());
        config.getGraphStore().setUri(resolveProperty(environment, "mem0.graph-store.uri", 
            properties.getGraphStore().getUri()));
        config.getGraphStore().setUsername(resolveProperty(environment, "mem0.graph-store.username", 
            properties.getGraphStore().getUsername()));
        config.getGraphStore().setPassword(resolveProperty(environment, "mem0.graph-store.password", 
            properties.getGraphStore().getPassword()));
        config.getGraphStore().setAdditionalConfig(properties.getGraphStore().getAdditionalConfig());
        
        // LLM configuration
        config.getLlm().setProvider(properties.getLlm().getProvider());
        config.getLlm().setApiKey(resolveProperty(environment, "mem0.llm.api-key", 
            properties.getLlm().getApiKey()));
        config.getLlm().setModel(resolveProperty(environment, "mem0.llm.model", 
            properties.getLlm().getModel()));
        config.getLlm().setTemperature(properties.getLlm().getTemperature());
        config.getLlm().setMaxTokens(properties.getLlm().getMaxTokens());
        config.getLlm().setAdditionalConfig(properties.getLlm().getAdditionalConfig());
        
        // Embedding configuration
        config.getEmbedding().setProvider(properties.getEmbedding().getProvider());
        config.getEmbedding().setApiKey(resolveProperty(environment, "mem0.embedding.api-key", 
            properties.getEmbedding().getApiKey()));
        config.getEmbedding().setModel(resolveProperty(environment, "mem0.embedding.model", 
            properties.getEmbedding().getModel()));
        config.getEmbedding().setAdditionalConfig(properties.getEmbedding().getAdditionalConfig());
        
        return config;
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "mem0.vector-store.provider", havingValue = "milvus", matchIfMissing = true)
    public VectorStore milvusVectorStore(Mem0Config config) {
        logger.info("Creating Milvus VectorStore");
        return new MilvusVectorStore(
            config.getVectorStore().getHost(),
            config.getVectorStore().getPort(),
            config.getVectorStore().getToken()
        );
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "mem0.graph-store.provider", havingValue = "neo4j", matchIfMissing = true)
    public GraphStore neo4jGraphStore(Mem0Config config) {
        logger.info("Creating Neo4j GraphStore");
        return new Neo4jGraphStore(
            config.getGraphStore().getUri(),
            config.getGraphStore().getUsername(),
            config.getGraphStore().getPassword()
        );
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "mem0.embedding.provider", havingValue = "openai")
    public EmbeddingProvider openAIEmbeddingProvider(Mem0Config config) {
        logger.info("Creating OpenAI EmbeddingProvider");
        return new OpenAIEmbeddingProvider(
            config.getEmbedding().getApiKey(),
            config.getEmbedding().getModel()
        );
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "mem0.embedding.provider", havingValue = "mock", matchIfMissing = true)
    public EmbeddingProvider defaultEmbeddingProvider() {
        logger.info("Creating Default TF-IDF EmbeddingProvider");
        return new SimpleTFIDFEmbeddingProvider();
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "mem0.llm.provider", havingValue = "openai")
    public LLMProvider openAILLMProvider(Mem0Config config) {
        logger.info("Creating OpenAI LLMProvider");
        return new OpenAIProvider(config.getLlm().getApiKey());
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "mem0.llm.provider", havingValue = "mock", matchIfMissing = true)
    public LLMProvider defaultLLMProvider() {
        logger.info("Creating Default Rule-Based LLMProvider");
        return new RuleBasedLLMProvider();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public MemoryClassifier memoryClassifier(LLMProvider llmProvider) {
        logger.info("Creating MemoryClassifier");
        return new MemoryClassifier(llmProvider);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public MemoryConflictDetector memoryConflictDetector(EmbeddingProvider embeddingProvider, 
                                                        LLMProvider llmProvider) {
        logger.info("Creating MemoryConflictDetector");
        return new MemoryConflictDetector(embeddingProvider, llmProvider);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public MemoryMergeStrategy memoryMergeStrategy(LLMProvider llmProvider) {
        logger.info("Creating MemoryMergeStrategy");
        return new MemoryMergeStrategy(llmProvider);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public MemoryImportanceScorer memoryImportanceScorer(LLMProvider llmProvider) {
        logger.info("Creating MemoryImportanceScorer");
        return new MemoryImportanceScorer(llmProvider);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public MemoryForgettingManager memoryForgettingManager() {
        logger.info("Creating MemoryForgettingManager");
        return new MemoryForgettingManager();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public EnhancedMemoryService enhancedMemoryService(
            VectorStore vectorStore,
            GraphStore graphStore,
            EmbeddingProvider embeddingProvider,
            LLMProvider llmProvider,
            MemoryClassifier memoryClassifier,
            MemoryConflictDetector conflictDetector,
            MemoryMergeStrategy mergeStrategy,
            MemoryImportanceScorer importanceScorer,
            MemoryForgettingManager forgettingManager) {
        
        logger.info("Creating EnhancedMemoryService");
        return new EnhancedMemoryService(
            vectorStore, graphStore, embeddingProvider, llmProvider,
            memoryClassifier, conflictDetector, mergeStrategy, 
            importanceScorer, forgettingManager
        );
    }
    
    @Bean
    @ConditionalOnMissingBean
    public Mem0 mem0(Mem0Config config) {
        logger.info("Creating Mem0 main instance");
        return new Mem0(config);
    }
    
    private String resolveProperty(Environment environment, String key, String defaultValue) {
        String value = environment.getProperty(key);
        if (value != null) {
            return value;
        }
        
        // Also check environment variables (convert dots to underscores and uppercase)
        String envKey = key.replace(".", "_").replace("-", "_").toUpperCase();
        value = environment.getProperty(envKey);
        if (value != null) {
            return value;
        }
        
        return defaultValue;
    }
}