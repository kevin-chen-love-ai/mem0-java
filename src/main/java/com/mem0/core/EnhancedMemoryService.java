package com.mem0.core;

import com.mem0.Mem0;
import com.mem0.embedding.EmbeddingProvider;
import com.mem0.llm.LLMProvider;
import com.mem0.store.GraphStore;
import com.mem0.store.VectorStore;
import com.mem0.template.ChatRAGPromptTemplate;
import com.mem0.template.PromptTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * EnhancedMemoryService - 增强型内存服务
 * 
 * 这是Mem0的核心服务类，负责管理增强型内存的完整生命周期，包括：
 * - 内存的创建、更新、删除和搜索
 * - 智能内存分类和重要性评分
 * - 冲突检测和解决
 * - 内存合并和遗忘管理
 * - RAG（检索增强生成）查询支持
 * - 内存关系图管理
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class EnhancedMemoryService implements AutoCloseable {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedMemoryService.class);
    
    // Core stores and providers
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
    
    // Templates and configuration
    private final ChatRAGPromptTemplate chatPromptTemplate;
    private final String defaultCollectionName = "enhanced_memories";
    
    // In-memory cache for frequently accessed memories
    private final Map<String, EnhancedMemory> memoryCache = new HashMap<>();
    private final int maxCacheSize = 1000;
    
    /**
     * 构造增强型内存服务
     * 
     * @param vectorStore 向量存储实例，用于存储和检索内存向量
     * @param graphStore 图存储实例，用于管理内存关系图
     * @param embeddingProvider 嵌入提供者，用于生成文本向量
     * @param llmProvider 大语言模型提供者，用于内容分析和生成
     * @param memoryClassifier 内存分类器，用于自动分类内存类型
     * @param conflictDetector 冲突检测器，用于检测和解决内存冲突
     * @param mergeStrategy 合并策略，用于合并相似内存
     * @param importanceScorer 重要性评分器，用于评估内存重要性
     * @param forgettingManager 遗忘管理器，用于管理内存衰减和遗忘
     */
    public EnhancedMemoryService(VectorStore vectorStore,
                                GraphStore graphStore,
                                EmbeddingProvider embeddingProvider,
                                LLMProvider llmProvider,
                                MemoryClassifier memoryClassifier,
                                MemoryConflictDetector conflictDetector,
                                MemoryMergeStrategy mergeStrategy,
                                MemoryImportanceScorer importanceScorer,
                                MemoryForgettingManager forgettingManager) {
        this.vectorStore = vectorStore;
        this.graphStore = graphStore;
        this.embeddingProvider = embeddingProvider;
        this.llmProvider = llmProvider;
        this.memoryClassifier = memoryClassifier;
        this.conflictDetector = conflictDetector;
        this.mergeStrategy = mergeStrategy;
        this.importanceScorer = importanceScorer;
        this.forgettingManager = forgettingManager;
        this.chatPromptTemplate = new ChatRAGPromptTemplate();
        
        initializeCollections();
    }
    
    // ================== Core Memory Operations ==================
    
    public CompletableFuture<String> addEnhancedMemory(String content, String userId, 
                                                      String agentId, String runId,
                                                      String memoryType, 
                                                      Map<String, Object> metadata) {
        logger.info("Adding enhanced memory for user: {}", userId);
        
        return CompletableFuture.supplyAsync(() -> {
            // Create enhanced memory object
            String memoryId = UUID.randomUUID().toString();
            EnhancedMemory memory = new EnhancedMemory(memoryId, content, userId, agentId, runId);
            
            return memory;
        }).thenCompose(memory -> {
            // Classify memory type
            Map<String, Object> classificationContext = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
            if (memoryType != null) {
                classificationContext.put("suggested_type", memoryType);
            }
            
            return memoryClassifier.classifyMemory(content, classificationContext)
                .thenCompose(type -> {
                    memory.setType(type);
                    
                    // Assess importance
                    return importanceScorer.scoreMemoryImportance(memory, classificationContext);
                })
                .thenCompose(importanceScore -> {
                    memory.setImportance(MemoryImportance.fromScore(importanceScore.getTotalScore()));
                    memory.setConfidenceScore(importanceScore.getConfidence());
                    
                    // Extract entities and tags
                    CompletableFuture<Set<String>> entitiesFuture = memoryClassifier.extractEntities(content);
                    CompletableFuture<Set<String>> tagsFuture = memoryClassifier.generateTags(content, memory.getType());
                    
                    return CompletableFuture.allOf(entitiesFuture, tagsFuture)
                        .thenCompose(ignored -> {
                            memory.getEntities().addAll(entitiesFuture.join());
                            memory.getTags().addAll(tagsFuture.join());
                            
                            // Add metadata
                            if (metadata != null) {
                                memory.getMetadata().putAll(metadata);
                            }
                            memory.getMetadata().put("created_by", "enhanced_service");
                            memory.getMetadata().put("classification_confidence", importanceScore.getConfidence());
                            
                            // Check for conflicts with existing memories
                            return detectAndHandleConflicts(memory);
                        });
                })
                .thenCompose(processedMemory -> {
                    // Store in vector database
                    return embeddingProvider.embed(processedMemory.getContent())
                        .thenCompose(embedding -> {
                            Map<String, Object> vectorMetadata = createVectorMetadata(processedMemory);
                            vectorMetadata.put("id", processedMemory.getId());
                            return vectorStore.insert(defaultCollectionName, embedding, vectorMetadata);
                        })
                        .thenCompose(vectorId -> {
                            // Store in graph database
                            Map<String, Object> nodeProperties = createNodeProperties(processedMemory);
                            return graphStore.createNode("EnhancedMemory", nodeProperties);
                        })
                        .thenApply(nodeId -> {
                            // Add to cache
                            updateCache(processedMemory);
                            
                            logger.debug("Added enhanced memory: {}", processedMemory.getId());
                            return processedMemory.getId();
                        });
                });
        });
    }
    
    public CompletableFuture<EnhancedMemory> getEnhancedMemory(String memoryId) {
        // Check cache first
        EnhancedMemory cached = memoryCache.get(memoryId);
        if (cached != null) {
            cached.recordAccess();
            return CompletableFuture.completedFuture(cached);
        }
        
        // Load from storage
        return loadMemoryFromStorage(memoryId)
            .thenApply(memory -> {
                if (memory != null) {
                    memory.recordAccess();
                    updateCache(memory);
                }
                return memory;
            });
    }
    
    public CompletableFuture<List<EnhancedMemory>> getAllEnhancedMemories(String userId, String memoryType) {
        Map<String, Object> filter = new HashMap<>();
        filter.put("userId", userId);
        if (memoryType != null) {
            filter.put("memoryType", memoryType);
        }
        
        return graphStore.getNodesByLabel("EnhancedMemory", filter)
            .thenCompose(nodes -> {
                List<CompletableFuture<EnhancedMemory>> memoryFutures = nodes.stream()
                    .map(node -> {
                        String memoryId = (String) node.getProperties().get("id");
                        return getEnhancedMemory(memoryId);
                    })
                    .collect(Collectors.toList());
                
                return CompletableFuture.allOf(memoryFutures.toArray(new CompletableFuture[0]))
                    .thenApply(ignored -> memoryFutures.stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
            });
    }
    
    public CompletableFuture<List<EnhancedMemory>> searchEnhancedMemories(String query, String userId, int limit) {
        return embeddingProvider.embed(query)
            .thenCompose(queryEmbedding -> {
                Map<String, Object> filter = new HashMap<>();
                filter.put("userId", userId);
                
                return vectorStore.search(defaultCollectionName, queryEmbedding, limit, filter);
            })
            .thenCompose(searchResults -> {
                List<CompletableFuture<EnhancedMemory>> memoryFutures = ((List<VectorStore.VectorSearchResult>) searchResults).stream()
                    .map(result -> {
                        String memoryId = result.getId();
                        return getEnhancedMemory(memoryId)
                            .thenApply(memory -> {
                                if (memory != null) {
                                    memory.setRelevanceScore(result.getScore());
                                }
                                return memory;
                            });
                    })
                    .collect(Collectors.toList());
                
                return CompletableFuture.allOf(memoryFutures.toArray(new CompletableFuture[0]))
                    .thenApply(ignored -> memoryFutures.stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .sorted((m1, m2) -> Double.compare(m2.getRelevanceScore(), m1.getRelevanceScore()))
                        .collect(Collectors.toList()));
            });
    }
    
    public CompletableFuture<EnhancedMemory> updateEnhancedMemory(String memoryId, String newContent,
                                                                 Map<String, Object> updateContext) {
        return getEnhancedMemory(memoryId)
            .thenCompose(existingMemory -> {
                if (existingMemory == null) {
                    return CompletableFuture.completedFuture(null);
                }
                
                return mergeStrategy.updateMemory(existingMemory, newContent, updateContext)
                    .thenCompose(updatedMemory -> {
                        // Re-evaluate importance after update
                        return importanceScorer.updateMemoryImportance(updatedMemory)
                            .thenCompose(ignored -> {
                                // Update storage
                                return updateMemoryInStorage(updatedMemory)
                                    .thenApply(voidResult -> updatedMemory);
                            });
                    });
            });
    }
    
    public CompletableFuture<Void> deleteEnhancedMemory(String memoryId) {
        return CompletableFuture.allOf(
            vectorStore.delete(defaultCollectionName, memoryId),
            graphStore.deleteNode(memoryId)
        ).thenRun(() -> {
            memoryCache.remove(memoryId);
            logger.debug("Deleted enhanced memory: {}", memoryId);
        });
    }
    
    public CompletableFuture<Void> deleteAllEnhancedMemories(String userId) {
        return getAllEnhancedMemories(userId, null)
            .thenCompose(memories -> {
                List<CompletableFuture<Void>> deleteFutures = memories.stream()
                    .map(memory -> deleteEnhancedMemory(memory.getId()))
                    .collect(Collectors.toList());
                
                return CompletableFuture.allOf(deleteFutures.toArray(new CompletableFuture[0]));
            });
    }
    
    // ================== Advanced Memory Management ==================
    
    public CompletableFuture<List<MemoryConflictDetector.MemoryConflict>> detectAllConflicts(String userId) {
        return getAllEnhancedMemories(userId, null)
            .thenCompose(memories -> {
                List<CompletableFuture<List<MemoryConflictDetector.MemoryConflict>>> conflictFutures = new ArrayList<>();
                
                for (int i = 0; i < memories.size(); i++) {
                    EnhancedMemory memory = memories.get(i);
                    List<EnhancedMemory> otherMemories = new ArrayList<>(memories);
                    otherMemories.remove(i);
                    
                    conflictFutures.add(conflictDetector.detectConflicts(memory, otherMemories));
                }
                
                return CompletableFuture.allOf(conflictFutures.toArray(new CompletableFuture[0]))
                    .thenApply(ignored -> conflictFutures.stream()
                        .flatMap(future -> future.join().stream())
                        .distinct()
                        .collect(Collectors.toList()));
            });
    }
    
    public CompletableFuture<List<EnhancedMemory>> consolidateMemories(String userId, double similarityThreshold) {
        return getAllEnhancedMemories(userId, null)
            .thenCompose(memories -> mergeStrategy.consolidateMemories(memories, similarityThreshold))
            .thenCompose(consolidatedMemories -> {
                // Update storage for consolidated memories
                List<CompletableFuture<Void>> updateFutures = consolidatedMemories.stream()
                    .map(this::updateMemoryInStorage)
                    .collect(Collectors.toList());
                
                return CompletableFuture.allOf(updateFutures.toArray(new CompletableFuture[0]))
                    .thenApply(ignored -> consolidatedMemories);
            });
    }
    
    public CompletableFuture<Void> updateAllImportanceScores(String userId) {
        return getAllEnhancedMemories(userId, null)
            .thenCompose(memories -> {
                List<CompletableFuture<Void>> updateFutures = memories.stream()
                    .map(importanceScorer::updateMemoryImportance)
                    .collect(Collectors.toList());
                
                return CompletableFuture.allOf(updateFutures.toArray(new CompletableFuture[0]));
            });
    }
    
    public CompletableFuture<Integer> processMemoryDecay(String userId) {
        return getAllEnhancedMemories(userId, null)
            .thenCompose(memories -> forgettingManager.processMemoryDecay(memories))
            .thenCompose(survivingMemories -> {
                int forgottenCount = 0;
                
                // Update storage for surviving memories and remove forgotten ones
                List<CompletableFuture<Void>> updateFutures = new ArrayList<>();
                
                for (EnhancedMemory memory : survivingMemories) {
                    if (memory.isDeprecated()) {
                        updateFutures.add(deleteEnhancedMemory(memory.getId()));
                        forgottenCount++;
                    } else {
                        updateFutures.add(updateMemoryInStorage(memory));
                    }
                }
                
                final int finalForgottenCount = forgottenCount;
                return CompletableFuture.allOf(updateFutures.toArray(new CompletableFuture[0]))
                    .thenApply(ignored -> finalForgottenCount);
            });
    }
    
    public CompletableFuture<Integer> pruneMemories(String userId, int maxMemories, 
                                                   MemoryForgettingManager.PruningStrategy strategy) {
        return getAllEnhancedMemories(userId, null)
            .thenCompose(memories -> {
                if (memories.size() <= maxMemories) {
                    return CompletableFuture.completedFuture(0);
                }
                
                return forgettingManager.pruneOldMemories(memories, maxMemories, strategy)
                    .thenCompose(keptMemories -> {
                        // Delete pruned memories
                        Set<String> keptIds = keptMemories.stream()
                            .map(EnhancedMemory::getId)
                            .collect(Collectors.toSet());
                        
                        List<CompletableFuture<Void>> deleteFutures = memories.stream()
                            .filter(memory -> !keptIds.contains(memory.getId()))
                            .map(memory -> deleteEnhancedMemory(memory.getId()))
                            .collect(Collectors.toList());
                        
                        int prunedCount = deleteFutures.size();
                        
                        return CompletableFuture.allOf(deleteFutures.toArray(new CompletableFuture[0]))
                            .thenApply(ignored -> prunedCount);
                    });
            });
    }
    
    public CompletableFuture<String> queryWithRAG(String query, String userId, int maxMemories, 
                                                 String systemMessage) {
        return searchEnhancedMemories(query, userId, maxMemories)
            .thenCompose(memories -> {
                if (memories.isEmpty()) {
                    // No memories found, generate response without context
                    return generateStandaloneResponse(query, systemMessage);
                }
                
                // Build prompt context
                PromptTemplate.PromptContext context = new PromptTemplate.PromptContext(query);
                context.setSystemMessage(systemMessage);
                
                List<PromptTemplate.RetrievedMemory> retrievedMemories = memories.stream()
                    .map(memory -> new PromptTemplate.RetrievedMemory(
                        memory.getContent(),
                        memory.getRelevanceScore(),
                        memory.getMetadata(),
                        memory.getType().getValue()
                    ))
                    .collect(Collectors.toList());
                
                context.setRetrievedMemories(retrievedMemories);
                
                // Generate response using chat completions
                List<LLMProvider.ChatMessage> messages = chatPromptTemplate.buildChatMessages(context);
                
                LLMProvider.LLMConfig llmConfig = new LLMProvider.LLMConfig();
                llmConfig.setMaxTokens(1000);
                llmConfig.setTemperature(0.7);
                
                return llmProvider.generateChatCompletion(messages, llmConfig)
                    .thenApply(response -> {
                        // Record access for retrieved memories
                        memories.forEach(memory -> {
                            memory.recordAccess();
                            updateCache(memory);
                        });
                        
                        return response.getContent();
                    });
            });
    }
    
    public CompletableFuture<String> createMemoryRelationship(String sourceMemoryId, String targetMemoryId,
                                                             String relationshipType, 
                                                             Map<String, Object> properties) {
        Map<String, Object> relProps = new HashMap<>();
        if (properties != null) {
            relProps.putAll(properties);
        }
        relProps.put("createdAt", LocalDateTime.now().toString());
        relProps.put("createdBy", "enhanced_service");
        
        return graphStore.createRelationship(sourceMemoryId, targetMemoryId, relationshipType, relProps)
            .thenApply(relationshipId -> {
                // Update memory caches to reflect new relationships
                EnhancedMemory sourceMemory = memoryCache.get(sourceMemoryId);
                EnhancedMemory targetMemory = memoryCache.get(targetMemoryId);
                
                if (sourceMemory != null) {
                    sourceMemory.addRelatedMemory(targetMemoryId, 1.0); // Default similarity
                }
                if (targetMemory != null) {
                    targetMemory.addRelatedMemory(sourceMemoryId, 1.0);
                }
                
                return relationshipId;
            });
    }
    
    public CompletableFuture<List<EnhancedMemory>> getRelatedMemories(String memoryId, String relationshipType, 
                                                                     int maxHops) {
        return graphStore.findConnectedNodes(memoryId, relationshipType, maxHops)
            .thenCompose(nodes -> {
                List<CompletableFuture<EnhancedMemory>> memoryFutures = nodes.stream()
                    .map(node -> {
                        String relatedMemoryId = (String) node.getProperties().get("id");
                        return getEnhancedMemory(relatedMemoryId);
                    })
                    .collect(Collectors.toList());
                
                return CompletableFuture.allOf(memoryFutures.toArray(new CompletableFuture[0]))
                    .thenApply(ignored -> memoryFutures.stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
            });
    }
    
    public CompletableFuture<Mem0.MemoryStatistics> getMemoryStatistics(String userId) {
        return getAllEnhancedMemories(userId, null)
            .thenApply(memories -> {
                if (memories.isEmpty()) {
                    return new Mem0.MemoryStatistics(0, new HashMap<>(), new HashMap<>(), 0, 0, 0.0, 0.0);
                }
                
                // Calculate statistics
                Map<MemoryType, Integer> typeCount = new HashMap<>();
                Map<MemoryImportance, Integer> importanceCount = new HashMap<>();
                int consolidatedCount = 0;
                int deprecatedCount = 0;
                double totalAge = 0.0;
                double totalAccess = 0.0;
                
                for (EnhancedMemory memory : memories) {
                    typeCount.merge(memory.getType(), 1, Integer::sum);
                    importanceCount.merge(memory.getImportance(), 1, Integer::sum);
                    
                    if (memory.isConsolidated()) consolidatedCount++;
                    if (memory.isDeprecated()) deprecatedCount++;
                    
                    totalAge += memory.getDaysOld();
                    totalAccess += memory.getAccessCount();
                }
                
                double avgAge = totalAge / memories.size();
                double avgAccess = totalAccess / memories.size();
                
                return new Mem0.MemoryStatistics(
                    memories.size(), typeCount, importanceCount,
                    consolidatedCount, deprecatedCount, avgAge, avgAccess
                );
            });
    }
    
    @Override
    public void close() {
        logger.info("Closing EnhancedMemoryService");
        
        try {
            if (embeddingProvider != null) {
                embeddingProvider.close();
            }
            if (llmProvider != null) {
                llmProvider.close();
            }
        } catch (Exception e) {
            logger.error("Error closing providers", e);
        }
    }
    
    /**
     * 异步关闭方法
     */
    public CompletableFuture<Void> closeAsync() {
        logger.info("Closing EnhancedMemoryService asynchronously");
        
        return CompletableFuture.runAsync(() -> {
            close();
        });
    }
    
    // ================== Private Helper Methods ==================
    
    private CompletableFuture<EnhancedMemory> detectAndHandleConflicts(EnhancedMemory newMemory) {
        // Get existing memories for the same user
        return getAllEnhancedMemories(newMemory.getUserId(), null)
            .thenCompose(existingMemories -> {
                if (existingMemories.isEmpty()) {
                    return CompletableFuture.completedFuture(newMemory);
                }
                
                return conflictDetector.detectConflicts(newMemory, existingMemories)
                    .thenCompose(conflicts -> {
                        if (conflicts.isEmpty()) {
                            return CompletableFuture.completedFuture(newMemory);
                        }
                        
                        // Handle the first conflict found
                        MemoryConflictDetector.MemoryConflict conflict = conflicts.get(0);
                        return conflictDetector.resolveConflict(conflict)
                            .thenCompose(resolution -> {
                                return handleConflictResolution(newMemory, conflict, resolution);
                            });
                    });
            });
    }
    
    private CompletableFuture<EnhancedMemory> handleConflictResolution(
            EnhancedMemory newMemory, 
            MemoryConflictDetector.MemoryConflict conflict,
            MemoryConflictDetector.ConflictResolution resolution) {
        
        switch (resolution.getStrategy()) {
            case KEEP_FIRST:
                return CompletableFuture.completedFuture(newMemory);
                
            case KEEP_SECOND:
                // Don't add the new memory, return the existing one
                return CompletableFuture.completedFuture(conflict.getMemory2());
                
            case MERGE:
                // Merge the memories
                List<EnhancedMemory> toMerge = Arrays.asList(newMemory, conflict.getMemory2());
                return mergeStrategy.mergeMemories(toMerge);
                
            case KEEP_BOTH:
                // Tag the new memory as potentially conflicting
                newMemory.getMetadata().put("potential_conflict", conflict.getType().toString());
                newMemory.getMetadata().put("conflict_with", conflict.getMemory2().getId());
                return CompletableFuture.completedFuture(newMemory);
                
            case DELETE_BOTH:
                // Mark both for deletion (handled externally)
                newMemory.deprecate();
                return CompletableFuture.completedFuture(newMemory);
                
            default:
                return CompletableFuture.completedFuture(newMemory);
        }
    }
    
    private CompletableFuture<EnhancedMemory> loadMemoryFromStorage(String memoryId) {
        return vectorStore.get(defaultCollectionName, memoryId)
            .thenApply(document -> {
                if (document == null) {
                    return null;
                }
                
                Map<String, Object> metadata = document.getMetadata();
                EnhancedMemory memory = reconstructMemoryFromMetadata(memoryId, metadata);
                
                return memory;
            });
    }
    
    private EnhancedMemory reconstructMemoryFromMetadata(String memoryId, Map<String, Object> metadata) {
        String content = (String) metadata.get("content");
        String userId = (String) metadata.get("userId");
        String agentId = (String) metadata.get("agentId");
        String runId = (String) metadata.get("runId");
        
        EnhancedMemory memory = new EnhancedMemory(memoryId, content, userId, agentId, runId);
        
        // Restore type and importance
        String typeValue = (String) metadata.get("memoryType");
        if (typeValue != null) {
            memory.setType(MemoryType.fromValue(typeValue));
        }
        
        String importanceValue = (String) metadata.get("importance");
        if (importanceValue != null) {
            memory.setImportance(MemoryImportance.valueOf(importanceValue));
        }
        
        // Restore other properties
        Object confidenceObj = metadata.get("confidenceScore");
        if (confidenceObj instanceof Number) {
            memory.setConfidenceScore(((Number) confidenceObj).doubleValue());
        }
        
        // Restore metadata (excluding system fields)
        Set<String> systemFields = new HashSet<>(Arrays.asList("content", "userId", "agentId", "runId", "memoryType", "importance", "confidenceScore"));
        metadata.entrySet().stream()
            .filter(entry -> !systemFields.contains(entry.getKey()))
            .forEach(entry -> memory.getMetadata().put(entry.getKey(), entry.getValue()));
        
        return memory;
    }
    
    private Map<String, Object> createVectorMetadata(EnhancedMemory memory) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("content", memory.getContent());
        metadata.put("userId", memory.getUserId());
        metadata.put("agentId", memory.getAgentId());
        metadata.put("runId", memory.getRunId());
        metadata.put("memoryType", memory.getType().getValue());
        metadata.put("importance", memory.getImportance().name());
        metadata.put("confidenceScore", memory.getConfidenceScore());
        metadata.put("createdAt", memory.getCreatedAt().toString());
        metadata.put("contentHash", memory.getContentHash());
        
        // Add custom metadata
        metadata.putAll(memory.getMetadata());
        
        return metadata;
    }
    
    private Map<String, Object> createNodeProperties(EnhancedMemory memory) {
        Map<String, Object> properties = createVectorMetadata(memory);
        properties.put("id", memory.getId());
        properties.put("isConsolidated", memory.isConsolidated());
        properties.put("isDeprecated", memory.isDeprecated());
        properties.put("accessCount", memory.getAccessCount());
        
        return properties;
    }
    
    private CompletableFuture<Void> updateMemoryInStorage(EnhancedMemory memory) {
        // Update vector store
        CompletableFuture<Void> vectorUpdate = embeddingProvider.embed(memory.getContent())
            .thenCompose(embedding -> {
                Map<String, Object> metadata = createVectorMetadata(memory);
                return vectorStore.delete(defaultCollectionName, memory.getId())
                    .thenCompose(ignored -> {
                        metadata.put("id", memory.getId());
                        return vectorStore.insert(defaultCollectionName, embedding, metadata);
                    })
                    .thenApply(ignored -> null);
            });
        
        // Update graph store
        CompletableFuture<Void> graphUpdate = graphStore.updateNode(memory.getId(), createNodeProperties(memory));
        
        return CompletableFuture.allOf(vectorUpdate, graphUpdate)
            .thenRun(() -> updateCache(memory));
    }
    
    private void updateCache(EnhancedMemory memory) {
        memoryCache.put(memory.getId(), memory);
        
        // Simple LRU eviction if cache is too large
        if (memoryCache.size() > maxCacheSize) {
            String oldestKey = memoryCache.entrySet().stream()
                .min((e1, e2) -> e1.getValue().getLastAccessedAt().compareTo(e2.getValue().getLastAccessedAt()))
                .map(Map.Entry::getKey)
                .orElse(null);
            
            if (oldestKey != null) {
                memoryCache.remove(oldestKey);
            }
        }
    }
    
    private CompletableFuture<String> generateStandaloneResponse(String query, String systemMessage) {
        List<LLMProvider.ChatMessage> messages = new ArrayList<>();
        
        if (systemMessage != null) {
            messages.add(new LLMProvider.ChatMessage(LLMProvider.ChatMessage.Role.SYSTEM, systemMessage));
        } else {
            messages.add(new LLMProvider.ChatMessage(LLMProvider.ChatMessage.Role.SYSTEM,
                "You are a helpful assistant. The user has no stored memories, so provide a general response."));
        }
        
        messages.add(new LLMProvider.ChatMessage(LLMProvider.ChatMessage.Role.USER, query));
        
        LLMProvider.LLMConfig config = new LLMProvider.LLMConfig();
        config.setMaxTokens(500);
        config.setTemperature(0.7);
        
        return llmProvider.generateChatCompletion(messages, config)
            .thenApply(LLMProvider.LLMResponse::getContent);
    }
    
    private void initializeCollections() {
        try {
            vectorStore.collectionExists(defaultCollectionName)
                .thenCompose(exists -> {
                    if (!exists) {
                        logger.info("Creating enhanced memory collection: {}", defaultCollectionName);
                        return vectorStore.createCollection(defaultCollectionName, embeddingProvider.getDimensions());
                    }
                    return CompletableFuture.completedFuture(null);
                })
                .get(); // Wait for completion during initialization
        } catch (Exception e) {
            logger.warn("Failed to initialize enhanced memory collections: {}", e.getMessage());
        }
    }
}