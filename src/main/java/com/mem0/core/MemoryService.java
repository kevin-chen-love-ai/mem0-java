package com.mem0.core;

/**
 * 记忆服务核心类 / Core memory service for managing user memories and RAG queries
 * 
 * <p>提供统一的接口来管理用户记忆，包括添加、搜索、更新和删除记忆，以及基于检索增强生成(RAG)的查询功能。
 * 整合了向量存储、图存储、嵌入提供者和LLM提供者，为用户提供智能记忆管理服务。</p>
 * 
 * <p>Provides a unified interface for managing user memories, including adding, searching, updating, 
 * and deleting memories, as well as Retrieval-Augmented Generation (RAG) based queries. 
 * Integrates vector store, graph store, embedding provider, and LLM provider to deliver 
 * intelligent memory management services.</p>
 * 
 * <p>主要特性 / Key features:</p>
 * <ul>
 *   <li>记忆的CRUD操作 / CRUD operations for memories</li>
 *   <li>基于向量相似度的记忆搜索 / Vector similarity-based memory search</li>
 *   <li>图结构的记忆关系管理 / Graph-based memory relationship management</li>
 *   <li>RAG查询和智能回答生成 / RAG queries and intelligent response generation</li>
 *   <li>多种存储后端支持 / Multiple storage backend support</li>
 *   <li>异步操作支持 / Asynchronous operation support</li>
 * </ul>
 * 
 * <p>使用示例 / Usage example:</p>
 * <pre>{@code
 * // 创建配置和服务
 * Mem0Config config = new Mem0Config();
 * MemoryService memoryService = new MemoryService(config);
 * 
 * // 添加记忆
 * String memoryId = memoryService.addMemory("User likes coffee", "user123").join();
 * 
 * // 搜索相关记忆
 * List<Memory> memories = memoryService.searchMemories("coffee preferences", "user123", 5).join();
 * 
 * // RAG查询
 * String response = memoryService.queryWithRAG("What do I like to drink?", "user123").join();
 * }</pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */

import com.mem0.config.Mem0Config;
import com.mem0.embedding.*;
import com.mem0.embedding.impl.SimpleTFIDFEmbeddingProvider;
import com.mem0.embedding.impl.OpenAIEmbeddingProvider;
import com.mem0.llm.*;
import com.mem0.llm.impl.RuleBasedLLMProvider;
import com.mem0.llm.OpenAIProvider;
import com.mem0.store.VectorStore;
import com.mem0.store.GraphStore;
import com.mem0.store.Neo4jGraphStore;
import com.mem0.store.MilvusVectorStore;
import com.mem0.template.*;
import com.mem0.template.PromptTemplate.PromptContext;
import com.mem0.template.PromptTemplate.RetrievedMemory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MemoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryService.class);
    
    private final VectorStore vectorStore;
    private final GraphStore graphStore;
    private final EmbeddingProvider embeddingProvider;
    private final LLMProvider llmProvider;
    private final PromptTemplate promptTemplate;
    private final ChatRAGPromptTemplate chatPromptTemplate;
    private final String defaultCollectionName;
    
    public MemoryService(Mem0Config config) {
        this.defaultCollectionName = "memories";
        
        // Initialize components based on configuration
        this.vectorStore = createVectorStore(config.getVectorStore());
        this.graphStore = createGraphStore(config.getGraphStore());
        this.embeddingProvider = createEmbeddingProvider(config.getEmbedding());
        this.llmProvider = createLLMProvider(config.getLlm());
        this.promptTemplate = new DefaultRAGPromptTemplate();
        this.chatPromptTemplate = new ChatRAGPromptTemplate();
        
        // Initialize collections if needed
        initializeCollections();
    }
    
    public CompletableFuture<String> addMemory(String content, String userId) {
        return addMemory(content, userId, null, null);
    }
    
    public CompletableFuture<String> addMemory(String content, String userId, String memoryType, 
                                             Map<String, Object> metadata) {
        logger.info("Adding memory for user: {}", userId);
        
        return embeddingProvider.embed(content)
            .thenCompose(embedding -> {
                Map<String, Object> memoryMetadata = new HashMap<>();
                if (metadata != null) {
                    memoryMetadata.putAll(metadata);
                }
                memoryMetadata.put("content", content);
                memoryMetadata.put("userId", userId);
                memoryMetadata.put("memoryType", memoryType != null ? memoryType : "general");
                memoryMetadata.put("createdAt", LocalDateTime.now().toString());
                
                return vectorStore.insert(defaultCollectionName, embedding, memoryMetadata);
            })
            .thenCompose(memoryId -> {
                // Also add to graph store as a node
                Map<String, Object> nodeProperties = new HashMap<>();
                nodeProperties.put("id", memoryId);
                nodeProperties.put("content", content);
                nodeProperties.put("userId", userId);
                nodeProperties.put("memoryType", memoryType != null ? memoryType : "general");
                nodeProperties.put("createdAt", LocalDateTime.now().toString());
                
                return graphStore.createNode("Memory", nodeProperties)
                    .thenApply(nodeId -> {
                        logger.debug("Added memory with ID: {} and graph node: {}", memoryId, nodeId);
                        return memoryId;
                    });
            });
    }
    
    public CompletableFuture<List<Memory>> searchMemories(String query, String userId, int topK) {
        logger.info("Searching memories for user: {} with query: {}", userId, query);
        
        return embeddingProvider.embed(query)
            .thenCompose(queryEmbedding -> {
                Map<String, Object> filter = new HashMap<>();
                filter.put("userId", userId);
                
                return vectorStore.search(defaultCollectionName, queryEmbedding, topK, filter);
            })
            .thenApply(searchResults -> {
                return searchResults.stream()
                    .map(result -> {
                        Map<String, Object> metadata = result.getMetadata();
                        return new Memory(
                            result.getId(),
                            (String) metadata.get("content"),
                            (String) metadata.get("userId"),
                            (String) metadata.get("memoryType"),
                            result.getScore(),
                            metadata
                        );
                    })
                    .collect(Collectors.toList());
            });
    }
    
    public CompletableFuture<String> queryWithRAG(String query, String userId) {
        return queryWithRAG(query, userId, 5, null);
    }
    
    public CompletableFuture<String> queryWithRAG(String query, String userId, int maxMemories, 
                                                 String systemMessage) {
        logger.info("RAG query for user: {} - {}", userId, query);
        
        return searchMemories(query, userId, maxMemories)
            .thenCompose(memories -> {
                // Build prompt context
                PromptContext context = new PromptContext(query);
                context.setSystemMessage(systemMessage);
                
                List<RetrievedMemory> retrievedMemories = memories.stream()
                    .map(memory -> new RetrievedMemory(
                        memory.getContent(),
                        memory.getRelevanceScore(),
                        memory.getMetadata(),
                        memory.getMemoryType()
                    ))
                    .collect(Collectors.toList());
                
                context.setRetrievedMemories(retrievedMemories);
                
                // Generate response using chat completions
                List<LLMProvider.ChatMessage> messages = chatPromptTemplate.buildChatMessages(context);
                
                com.mem0.llm.LLMProvider.LLMConfig llmConfig = new com.mem0.llm.LLMProvider.LLMConfig();
                llmConfig.setMaxTokens(1000);
                llmConfig.setTemperature(0.7);
                
                return llmProvider.generateChatCompletion(messages, llmConfig);
            })
            .thenApply(response -> {
                logger.debug("Generated RAG response: {}", response.getContent());
                return response.getContent();
            });
    }
    
    public CompletableFuture<Memory> getMemory(String memoryId) {
        return vectorStore.get(defaultCollectionName, memoryId)
            .thenApply(document -> {
                if (document == null) {
                    return null;
                }
                
                Map<String, Object> metadata = document.getMetadata();
                return new Memory(
                    document.getId(),
                    (String) metadata.get("content"),
                    (String) metadata.get("userId"),
                    (String) metadata.get("memoryType"),
                    0.0, // No relevance score for direct get
                    metadata
                );
            });
    }
    
    public CompletableFuture<List<Memory>> getAllMemories(String userId) {
        Map<String, Object> filter = new HashMap<>();
        filter.put("userId", userId);
        return graphStore.getNodesByLabel("Memory", filter)
            .thenApply(nodes -> {
                return nodes.stream()
                    .map(node -> {
                        Map<String, Object> properties = node.getProperties();
                        return new Memory(
                            (String) properties.get("id"),
                            (String) properties.get("content"),
                            (String) properties.get("userId"),
                            (String) properties.get("memoryType"),
                            0.0,
                            properties
                        );
                    })
                    .collect(Collectors.toList());
            });
    }
    
    public CompletableFuture<Void> deleteMemory(String memoryId) {
        logger.info("Deleting memory: {}", memoryId);
        
        // Delete from both vector store and graph store
        CompletableFuture<Void> vectorDelete = vectorStore.delete(defaultCollectionName, memoryId);
        CompletableFuture<Void> graphDelete = graphStore.deleteNode(memoryId);
        
        return CompletableFuture.allOf(vectorDelete, graphDelete);
    }
    
    public CompletableFuture<String> createMemoryRelationship(String sourceMemoryId, String targetMemoryId, 
                                                            String relationshipType) {
        return createMemoryRelationship(sourceMemoryId, targetMemoryId, relationshipType, null);
    }
    
    public CompletableFuture<String> createMemoryRelationship(String sourceMemoryId, String targetMemoryId,
                                                            String relationshipType, 
                                                            Map<String, Object> properties) {
        logger.info("Creating relationship: {} -[{}]-> {}", sourceMemoryId, relationshipType, targetMemoryId);
        
        Map<String, Object> relProps = new HashMap<>();
        if (properties != null) {
            relProps.putAll(properties);
        }
        relProps.put("createdAt", LocalDateTime.now().toString());
        
        return graphStore.createRelationship(sourceMemoryId, targetMemoryId, relationshipType, relProps);
    }
    
    public CompletableFuture<List<Memory>> getRelatedMemories(String memoryId, String relationshipType, int maxHops) {
        return graphStore.findConnectedNodes(memoryId, relationshipType, maxHops)
            .thenApply(nodes -> {
                return nodes.stream()
                    .map(node -> {
                        Map<String, Object> properties = node.getProperties();
                        return new Memory(
                            (String) properties.get("id"),
                            (String) properties.get("content"),
                            (String) properties.get("userId"),
                            (String) properties.get("memoryType"),
                            0.0,
                            properties
                        );
                    })
                    .collect(Collectors.toList());
            });
    }
    
    public CompletableFuture<Void> close() {
        logger.info("Closing MemoryService");
        
        CompletableFuture<Void> vectorClose = vectorStore.close();
        CompletableFuture<Void> graphClose = graphStore.close();
        
        return CompletableFuture.allOf(vectorClose, graphClose);
    }
    
    private void initializeCollections() {
        try {
            vectorStore.collectionExists(defaultCollectionName)
                .thenCompose(exists -> {
                    if (!exists) {
                        logger.info("Creating default collection: {}", defaultCollectionName);
                        return vectorStore.createCollection(defaultCollectionName, embeddingProvider.getDimensions());
                    }
                    return CompletableFuture.completedFuture(null);
                })
                .get(); // Wait for completion during initialization
        } catch (Exception e) {
            logger.warn("Failed to initialize collections: {}", e.getMessage());
        }
    }
    
    private VectorStore createVectorStore(Mem0Config.VectorStoreConfig config) {
        switch (config.getProvider().toLowerCase()) {
            case "milvus":
                return new MilvusVectorStore(config.getHost(), config.getPort(), config.getToken());
            default:
                throw new IllegalArgumentException("Unsupported vector store provider: " + config.getProvider());
        }
    }
    
    private GraphStore createGraphStore(Mem0Config.GraphStoreConfig config) {
        switch (config.getProvider().toLowerCase()) {
            case "neo4j":
                return new Neo4jGraphStore(config.getUri(), config.getUsername(), config.getPassword());
            default:
                throw new IllegalArgumentException("Unsupported graph store provider: " + config.getProvider());
        }
    }
    
    private EmbeddingProvider createEmbeddingProvider(Mem0Config.EmbeddingConfig config) {
        switch (config.getProvider().toLowerCase()) {
            case "openai":
                return new OpenAIEmbeddingProvider(config.getApiKey(), config.getModel());
            case "mock":
            default:
                return new SimpleTFIDFEmbeddingProvider();
        }
    }
    
    private LLMProvider createLLMProvider(Mem0Config.LLMConfig config) {
        switch (config.getProvider().toLowerCase()) {
            case "openai":
                return new OpenAIProvider(config.getApiKey());
            case "mock":
            default:
                return new RuleBasedLLMProvider();
        }
    }
    
    public static class Memory {
        private final String id;
        private final String content;
        private final String userId;
        private final String memoryType;
        private final double relevanceScore;
        private final Map<String, Object> metadata;
        
        public Memory(String id, String content, String userId, String memoryType,
                     double relevanceScore, Map<String, Object> metadata) {
            this.id = id;
            this.content = content;
            this.userId = userId;
            this.memoryType = memoryType;
            this.relevanceScore = relevanceScore;
            this.metadata = metadata;
        }
        
        // Getters
        public String getId() { return id; }
        public String getContent() { return content; }
        public String getUserId() { return userId; }
        public String getMemoryType() { return memoryType; }
        public double getRelevanceScore() { return relevanceScore; }
        public Map<String, Object> getMetadata() { return metadata; }
        
        @Override
        public String toString() {
            return String.format("Memory{id='%s', content='%s', userId='%s', memoryType='%s', relevanceScore=%.3f}", 
                id, content.length() > 50 ? content.substring(0, 50) + "..." : content, 
                userId, memoryType, relevanceScore);
        }
    }
}