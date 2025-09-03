package com.mem0;

import com.mem0.config.Mem0Config;
import com.mem0.core.*;
import com.mem0.embedding.EmbeddingProvider;
import com.mem0.embedding.impl.SimpleTFIDFEmbeddingProvider;
import com.mem0.llm.LLMProvider;
import com.mem0.llm.impl.RuleBasedLLMProvider;
import com.mem0.store.VectorStore;
import com.mem0.vector.impl.InMemoryVectorStore;
import com.mem0.store.GraphStore;
import com.mem0.graph.impl.DefaultInMemoryGraphStore;
import com.mem0.factory.ProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Mem0 - AI智能体的通用内存层 / Universal Memory Layer for AI Agents
 * 
 * Mem0 Java实现的主入口类，为AI应用提供智能内存管理，支持向量数据库、图数据库和高级内存生命周期管理。
 * Main entry point for the Mem0 Java implementation providing intelligent memory management
 * for AI applications with support for vector databases, graph databases, and advanced
 * memory lifecycle management.
 * 
 * <h3>核心功能 / Key Features:</h3>
 * <ul>
 *   <li>智能内存分类和冲突检测 / Intelligent memory classification and conflict detection</li>
 *   <li>多后端存储支持(向量/图数据库) / Multi-backend storage support (vector/graph databases)</li>
 *   <li>RAG查询和语义搜索 / RAG queries and semantic search</li>
 *   <li>内存重要性评分和遗忘管理 / Memory importance scoring and forgetting management</li>
 *   <li>异步操作和并发优化 / Asynchronous operations and concurrency optimization</li>
 *   <li>内存合并和去重策略 / Memory consolidation and deduplication strategies</li>
 *   <li>关系图谱构建和查询 / Relationship graph construction and querying</li>
 * </ul>
 * 
 * <h3>架构概览 / Architecture Overview:</h3>
 * <pre>
 * ┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
 * │   Mem0 API     │────│ EnhancedMemory   │────│   VectorStore   │
 * │    (主接口)     │    │   Service        │    │  (向量存储)      │
 * └─────────────────┘    └──────────────────┘    └─────────────────┘
 *          │                       │                       │
 *          │              ┌────────────────┐              │
 *          └──────────────│  GraphStore    │──────────────┘
 *                         │   (图存储)      │
 *                         └────────────────┘
 *          │                       │                       │
 * ┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
 * │MemoryClassifier │    │ ConflictDetector │    │ImportanceScorer │
 * │  (内存分类器)    │    │  (冲突检测器)     │    │  (重要性评分)    │
 * └─────────────────┘    └──────────────────┘    └─────────────────┘
 * </pre>
 * 
 * <h3>使用示例 / Usage Examples:</h3>
 * <pre>{@code
 * // 基础使用
 * Mem0 mem0 = new Mem0();
 * String memoryId = mem0.add("用户喜欢喝咖啡", "user123").join();
 * List<EnhancedMemory> results = mem0.search("咖啡偏好", "user123").join();
 * 
 * // 从配置文件初始化
 * Mem0 mem0 = new Mem0("config/mem0.properties");
 * 
 * // 配置构建器
 * Mem0 mem0 = Mem0.builder()
 *     .vectorStore("milvus", "localhost", 19530)
 *     .llm("openai", "your-api-key", "gpt-4")
 *     .build();
 * 
 * // 使用Builder加载配置文件
 * Mem0 mem0 = Mem0.builder()
 *     .loadFromFile("mem0.properties")
 *     .build();
 * 
 * // 从类路径加载配置
 * Mem0 mem0 = Mem0.builder()
 *     .loadFromClasspath("config/mem0.properties")
 *     .build();
 * 
 * // 从环境变量加载配置
 * Mem0 mem0 = Mem0.builder()
 *     .loadFromEnvironment()
 *     .build();
 * 
 * // RAG查询
 * String response = mem0.queryWithRAG("推荐一些饮品", "user123").join();
 * 
 * // 高级内存管理
 * mem0.consolidate("user123", 0.8).join(); // 合并相似内存
 * mem0.processMemoryDecay("user123").join(); // 处理内存衰减
 * }</pre>
 * 
 * <h3>线程安全性 / Thread Safety:</h3>
 * 此类是线程安全的，所有操作都通过并发安全的组件和异步操作实现。
 * This class is thread-safe with all operations implemented through concurrent-safe components and asynchronous operations.
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 * @see com.mem0.core.EnhancedMemoryService
 * @see com.mem0.config.Mem0Config
 * @see com.mem0.core.EnhancedMemory
 */
public class Mem0 implements AutoCloseable {
    
    private static final Logger logger = LoggerFactory.getLogger(Mem0.class);
    
    // Core components
    private final Mem0Config config;
    private final VectorStore vectorStore;
    private final GraphStore graphStore;
    private final EmbeddingProvider embeddingProvider;
    private final LLMProvider llmProvider;
    
    // Memory management components
    private final MemoryClassifier memoryClassifier;
    private final MemoryConflictDetector conflictDetector;
    private final MemoryMergeStrategy mergeStrategy;
    private final MemoryImportanceScorer importanceScorer;
    private final MemoryForgettingManager forgettingManager;
    
    // Core service
    private final EnhancedMemoryService memoryService;
    
    /**
     * Create Mem0 instance with default configuration
     */
    public Mem0() {
        this(new Mem0Config());
    }
    
    /**
     * Create Mem0 instance from configuration file path
     * 通过配置文件路径创建Mem0实例
     * 
     * @param configFilePath 配置文件路径 (Configuration file path)
     */
    public Mem0(String configFilePath) {
        this(Mem0Config.fromFile(configFilePath));
    }
    
    /**
     * Create Mem0 instance with direct provider injection
     */
    public Mem0(VectorStore vectorStore, GraphStore graphStore, 
               EmbeddingProvider embeddingProvider, LLMProvider llmProvider) {
        this.config = new Mem0Config();
        
        // Use provided implementations
        this.vectorStore = vectorStore != null ? vectorStore : new InMemoryVectorStore();
        this.graphStore = graphStore != null ? graphStore : createDefaultGraphStore();
        this.embeddingProvider = embeddingProvider != null ? embeddingProvider : new SimpleTFIDFEmbeddingProvider();
        this.llmProvider = llmProvider != null ? llmProvider : new RuleBasedLLMProvider();
        
        // Initialize memory management components
        this.memoryClassifier = new MemoryClassifier(this.llmProvider);
        this.conflictDetector = new MemoryConflictDetector(this.embeddingProvider, this.llmProvider);
        this.mergeStrategy = new MemoryMergeStrategy(this.llmProvider);
        this.importanceScorer = new MemoryImportanceScorer(this.llmProvider);
        this.forgettingManager = new MemoryForgettingManager();
        
        // Initialize enhanced memory service
        this.memoryService = new EnhancedMemoryService(
            this.vectorStore, this.graphStore, this.embeddingProvider, this.llmProvider,
            memoryClassifier, conflictDetector, mergeStrategy, 
            importanceScorer, forgettingManager
        );
        
        logger.info("Mem0 initialized with direct provider injection");
    }
    
    /**
     * Create Mem0 instance with custom configuration
     */
    public Mem0(Mem0Config config) {
        // Validate configuration
        ProviderFactory.validateConfiguration(config);
        this.config = config;
        
        // Initialize core providers using factory
        this.vectorStore = ProviderFactory.createVectorStore(config.getVectorStore());
        this.graphStore = ProviderFactory.createGraphStore(config.getGraphStore());
        this.embeddingProvider = ProviderFactory.createEmbeddingProvider(config.getEmbedding());
        this.llmProvider = ProviderFactory.createLLMProvider(config.getLlm());
        
        // Initialize memory management components
        this.memoryClassifier = new MemoryClassifier(llmProvider);
        this.conflictDetector = new MemoryConflictDetector(embeddingProvider, llmProvider);
        this.mergeStrategy = new MemoryMergeStrategy(llmProvider);
        this.importanceScorer = new MemoryImportanceScorer(llmProvider);
        this.forgettingManager = new MemoryForgettingManager();
        
        // Initialize enhanced memory service
        this.memoryService = new EnhancedMemoryService(
            vectorStore, graphStore, embeddingProvider, llmProvider,
            memoryClassifier, conflictDetector, mergeStrategy, 
            importanceScorer, forgettingManager
        );
        
        logger.info("Mem0 initialized with providers: vector={}, graph={}, embedding={}, llm={}", 
            config.getVectorStore().getProvider(),
            config.getGraphStore().getProvider(), 
            config.getEmbedding().getProvider(),
            config.getLlm().getProvider());
    }
    
    // ================== Public API Methods ==================
    
    /**
     * Add a new memory
     */
    public CompletableFuture<String> add(String content, String userId) {
        return add(content, userId, null, null);
    }
    
    /**
     * Add a new memory with type and metadata
     */
    public CompletableFuture<String> add(String content, String userId, String memoryType, 
                                       Map<String, Object> metadata) {
        return memoryService.addEnhancedMemory(content, userId, null, null, memoryType, metadata);
    }
    
    /**
     * Get a specific memory by ID
     */
    public CompletableFuture<EnhancedMemory> get(String memoryId) {
        return memoryService.getEnhancedMemory(memoryId);
    }
    
    /**
     * Get all memories for a user
     */
    public CompletableFuture<List<EnhancedMemory>> getAll(String userId) {
        return getAll(userId, null);
    }
    
    /**
     * Get all memories for a user, optionally filtered by type
     */
    public CompletableFuture<List<EnhancedMemory>> getAll(String userId, String memoryType) {
        return memoryService.getAllEnhancedMemories(userId, memoryType);
    }
    
    /**
     * Search for memories using semantic similarity
     */
    public CompletableFuture<List<EnhancedMemory>> search(String query, String userId) {
        return search(query, userId, 10);
    }
    
    /**
     * Search for memories with limit
     */
    public CompletableFuture<List<EnhancedMemory>> search(String query, String userId, int limit) {
        return memoryService.searchEnhancedMemories(query, userId, limit);
    }
    
    /**
     * Update an existing memory
     */
    public CompletableFuture<EnhancedMemory> update(String memoryId, String newContent) {
        return memoryService.updateEnhancedMemory(memoryId, newContent, null);
    }
    
    /**
     * Delete a memory
     */
    public CompletableFuture<Void> delete(String memoryId) {
        return memoryService.deleteEnhancedMemory(memoryId);
    }
    
    /**
     * Delete all memories for a user
     */
    public CompletableFuture<Void> deleteAll(String userId) {
        return memoryService.deleteAllEnhancedMemories(userId);
    }
    
    /**
     * Perform RAG query with retrieved memories
     */
    public CompletableFuture<String> queryWithRAG(String query, String userId) {
        return queryWithRAG(query, userId, 5, null);
    }
    
    /**
     * Perform RAG query with custom parameters
     */
    public CompletableFuture<String> queryWithRAG(String query, String userId, int maxMemories, 
                                                 String systemMessage) {
        return memoryService.queryWithRAG(query, userId, maxMemories, systemMessage);
    }
    
    /**
     * Create relationship between memories
     */
    public CompletableFuture<String> createRelationship(String sourceMemoryId, String targetMemoryId,
                                                       String relationshipType) {
        return createRelationship(sourceMemoryId, targetMemoryId, relationshipType, null);
    }
    
    /**
     * Create relationship with properties
     */
    public CompletableFuture<String> createRelationship(String sourceMemoryId, String targetMemoryId,
                                                       String relationshipType, 
                                                       Map<String, Object> properties) {
        return memoryService.createMemoryRelationship(sourceMemoryId, targetMemoryId, 
                                                     relationshipType, properties);
    }
    
    /**
     * Get related memories
     */
    public CompletableFuture<List<EnhancedMemory>> getRelated(String memoryId, String relationshipType) {
        return getRelated(memoryId, relationshipType, 2);
    }
    
    /**
     * Get related memories with max hops
     */
    public CompletableFuture<List<EnhancedMemory>> getRelated(String memoryId, String relationshipType, 
                                                            int maxHops) {
        return memoryService.getRelatedMemories(memoryId, relationshipType, maxHops);
    }
    
    // ================== Advanced Memory Management ==================
    
    /**
     * Classify memory type automatically
     */
    public CompletableFuture<MemoryType> classifyMemory(String content) {
        return classifyMemory(content, null);
    }
    
    /**
     * Classify memory with context
     */
    public CompletableFuture<MemoryType> classifyMemory(String content, Map<String, Object> context) {
        return memoryClassifier.classifyMemory(content, context);
    }
    
    /**
     * Detect conflicts between memories
     */
    public CompletableFuture<List<MemoryConflictDetector.MemoryConflict>> detectConflicts(
            String userId) {
        return memoryService.detectAllConflicts(userId);
    }
    
    /**
     * Resolve a memory conflict
     */
    public CompletableFuture<MemoryConflictDetector.ConflictResolution> resolveConflict(
            MemoryConflictDetector.MemoryConflict conflict) {
        return conflictDetector.resolveConflict(conflict);
    }
    
    /**
     * Consolidate similar memories
     */
    public CompletableFuture<List<EnhancedMemory>> consolidate(String userId, double similarityThreshold) {
        return memoryService.consolidateMemories(userId, similarityThreshold);
    }
    
    /**
     * Update importance scores for all memories
     */
    public CompletableFuture<Void> updateImportanceScores(String userId) {
        return memoryService.updateAllImportanceScores(userId);
    }
    
    /**
     * Process memory decay and forgetting
     */
    public CompletableFuture<Integer> processMemoryDecay(String userId) {
        return memoryService.processMemoryDecay(userId);
    }
    
    /**
     * Prune old memories to keep within limits
     */
    public CompletableFuture<Integer> pruneMemories(String userId, int maxMemories, 
                                                   MemoryForgettingManager.PruningStrategy strategy) {
        return memoryService.pruneMemories(userId, maxMemories, strategy);
    }
    
    /**
     * Get memory statistics for a user
     */
    public CompletableFuture<MemoryStatistics> getStatistics(String userId) {
        return memoryService.getMemoryStatistics(userId);
    }
    
    /**
     * Get memory history for a user
     */
    public CompletableFuture<List<EnhancedMemory>> getHistory(String userId) {
        return getAll(userId);
    }
    
    // ================== Configuration and Lifecycle ==================
    
    /**
     * Get current configuration
     */
    public Mem0Config getConfig() {
        return config;
    }
    
    /**
     * Get memory classifier
     */
    public MemoryClassifier getMemoryClassifier() {
        return memoryClassifier;
    }
    
    /**
     * Get conflict detector
     */
    public MemoryConflictDetector getConflictDetector() {
        return conflictDetector;
    }
    
    /**
     * Get merge strategy
     */
    public MemoryMergeStrategy getMergeStrategy() {
        return mergeStrategy;
    }
    
    /**
     * Get importance scorer
     */
    public MemoryImportanceScorer getImportanceScorer() {
        return importanceScorer;
    }
    
    /**
     * Get forgetting manager
     */
    public MemoryForgettingManager getForgettingManager() {
        return forgettingManager;
    }
    
    /**
     * Close all resources
     */
    @Override
    public void close() {
        logger.info("Closing Mem0 resources");
        
        try {
            if (memoryService != null) {
                memoryService.close();
            }
        } catch (Exception e) {
            logger.warn("Error closing memory service: {}", e.getMessage());
        }
        
        try {
            if (vectorStore != null) {
                vectorStore.close();
            }
        } catch (Exception e) {
            logger.warn("Error closing vector store: {}", e.getMessage());
        }
        
        try {
            if (graphStore != null) {
                graphStore.close();
            }
        } catch (Exception e) {
            logger.warn("Error closing graph store: {}", e.getMessage());
        }
    }
    
    
    // ================== Builder Pattern ==================
    
    public static class Builder {
        private Mem0Config config = new Mem0Config();
        private VectorStore vectorStore;
        private GraphStore graphStore;
        private EmbeddingProvider embeddingProvider;
        private LLMProvider llmProvider;
        
        public Builder vectorStore(String provider, String host, int port) {
            config.getVectorStore().setProvider(provider);
            config.getVectorStore().setHost(host);
            config.getVectorStore().setPort(port);
            return this;
        }
        
        public Builder vectorStore(String provider, String host, int port, String token) {
            vectorStore(provider, host, port);
            config.getVectorStore().setToken(token);
            return this;
        }
        
        public Builder graphStore(String provider, String uri, String username, String password) {
            config.getGraphStore().setProvider(provider);
            config.getGraphStore().setUri(uri);
            config.getGraphStore().setUsername(username);
            config.getGraphStore().setPassword(password);
            return this;
        }
        
        public Builder llm(String provider, String apiKey) {
            config.getLlm().setProvider(provider);
            config.getLlm().setApiKey(apiKey);
            return this;
        }
        
        public Builder llm(String provider, String apiKey, String model) {
            llm(provider, apiKey);
            config.getLlm().setModel(model);
            return this;
        }
        
        public Builder embedding(String provider, String apiKey) {
            config.getEmbedding().setProvider(provider);
            config.getEmbedding().setApiKey(apiKey);
            return this;
        }
        
        public Builder embedding(String provider, String apiKey, String model) {
            embedding(provider, apiKey);
            config.getEmbedding().setModel(model);
            return this;
        }
        
        public Builder vectorStore(VectorStore vectorStore) {
            this.vectorStore = vectorStore;
            return this;
        }
        
        public Builder graphStore(GraphStore graphStore) {
            this.graphStore = graphStore;
            return this;
        }
        
        public Builder embeddingProvider(EmbeddingProvider embeddingProvider) {
            this.embeddingProvider = embeddingProvider;
            return this;
        }
        
        public Builder llmProvider(LLMProvider llmProvider) {
            this.llmProvider = llmProvider;
            return this;
        }
        
        /**
         * Load configuration from file
         * 从配置文件加载配置
         * 
         * @param configFilePath 配置文件路径
         * @return Builder实例
         */
        public Builder loadFromFile(String configFilePath) {
            this.config = Mem0Config.fromFile(configFilePath);
            return this;
        }
        
        /**
         * Load configuration from classpath
         * 从类路径加载配置
         * 
         * @param resourcePath 资源路径
         * @return Builder实例
         */
        public Builder loadFromClasspath(String resourcePath) {
            this.config = Mem0Config.fromClasspath(resourcePath);
            return this;
        }
        
        /**
         * Load configuration from Properties
         * 从Properties对象加载配置
         * 
         * @param properties Properties对象
         * @return Builder实例
         */
        public Builder loadFromProperties(java.util.Properties properties) {
            this.config = Mem0Config.fromProperties(properties);
            return this;
        }
        
        /**
         * Load configuration from Map
         * 从Map对象加载配置
         * 
         * @param configMap 配置Map
         * @return Builder实例
         */
        public Builder loadFromMap(Map<String, Object> configMap) {
            this.config = Mem0Config.fromMap(configMap);
            return this;
        }
        
        /**
         * Load configuration from environment variables
         * 从环境变量加载配置
         * 
         * @return Builder实例
         */
        public Builder loadFromEnvironment() {
            this.config = Mem0Config.fromEnvironment();
            return this;
        }
        
        /**
         * Set custom configuration
         * 设置自定义配置
         * 
         * @param config 配置对象
         * @return Builder实例
         */
        public Builder config(Mem0Config config) {
            this.config = config;
            return this;
        }
        
        public Mem0 build() {
            if (vectorStore != null || graphStore != null || embeddingProvider != null || llmProvider != null) {
                return new Mem0(vectorStore, graphStore, embeddingProvider, llmProvider);
            }
            return new Mem0(config);
        }
    }
    
    /**
     * Create a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Memory statistics data class
     */
    public static class MemoryStatistics {
        private final int totalMemories;
        private final Map<MemoryType, Integer> memoryTypeCount;
        private final Map<MemoryImportance, Integer> importanceDistribution;
        private final int consolidatedMemories;
        private final int deprecatedMemories;
        private final double averageAge;
        private final double averageAccessCount;
        
        public MemoryStatistics(int totalMemories, Map<MemoryType, Integer> memoryTypeCount,
                               Map<MemoryImportance, Integer> importanceDistribution,
                               int consolidatedMemories, int deprecatedMemories,
                               double averageAge, double averageAccessCount) {
            this.totalMemories = totalMemories;
            this.memoryTypeCount = memoryTypeCount;
            this.importanceDistribution = importanceDistribution;
            this.consolidatedMemories = consolidatedMemories;
            this.deprecatedMemories = deprecatedMemories;
            this.averageAge = averageAge;
            this.averageAccessCount = averageAccessCount;
        }
        
        // Getters
        public int getTotalMemories() { return totalMemories; }
        public Map<MemoryType, Integer> getMemoryTypeCount() { return memoryTypeCount; }
        public Map<MemoryImportance, Integer> getImportanceDistribution() { return importanceDistribution; }
        public int getConsolidatedMemories() { return consolidatedMemories; }
        public int getDeprecatedMemories() { return deprecatedMemories; }
        public double getAverageAge() { return averageAge; }
        public double getAverageAccessCount() { return averageAccessCount; }
        
        @Override
        public String toString() {
            return String.format("MemoryStatistics{total=%d, consolidated=%d, deprecated=%d, avgAge=%.1f, avgAccess=%.1f}",
                totalMemories, consolidatedMemories, deprecatedMemories, averageAge, averageAccessCount);
        }
    }
    
    /**
     * Create a default GraphStore implementation
     */
    private GraphStore createDefaultGraphStore() {
        return new DefaultInMemoryGraphStore();
    }
}