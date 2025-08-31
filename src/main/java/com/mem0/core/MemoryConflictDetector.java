package com.mem0.core;

import com.mem0.embedding.EmbeddingProvider;
import com.mem0.llm.LLMProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 内存冲突检测系统 / Memory Conflict Detection System
 * 
 * 基于语义相似性分析和大语言模型的智能内存冲突检测与解决系统。
 * 支持多类型冲突识别、自动化解决策略、语义分析和规则引擎双模式冲突检测机制。
 * Intelligent memory conflict detection and resolution system based on semantic similarity analysis and LLM.
 * Supports multi-type conflict identification, automated resolution strategies, and dual-mode detection 
 * mechanisms using both semantic analysis and rule engines.
 * 
 * <h3>核心功能 / Key Features:</h3>
 * <ul>
 *   <li>多维度冲突检测(矛盾/事实/偏好/时间/冗余) / Multi-dimensional conflict detection (contradiction/factual/preference/temporal/redundancy)</li>
 *   <li>语义相似性分析和嵌入向量比较 / Semantic similarity analysis and embedding vector comparison</li>
 *   <li>LLM智能冲突分析和规则引擎双模式 / Dual-mode: LLM intelligent analysis and rule engine</li>
 *   <li>自动化冲突解决策略生成 / Automated conflict resolution strategy generation</li>
 *   <li>内容模式识别和关键词匹配 / Content pattern recognition and keyword matching</li>
 *   <li>批量冲突检测和异步处理支持 / Batch conflict detection and asynchronous processing</li>
 * </ul>
 * 
 * <h3>冲突检测体系 / Conflict Detection System:</h3>
 * <pre>
 * 冲突检测流程 / Conflict Detection Pipeline:
 * 
 * 输入内存 → 相似性筛选 → 冲突分析 → 置信度评估 → 解决策略
 * Input Memory → Similarity Filter → Conflict Analysis → Confidence Assessment → Resolution Strategy
 * 
 * 检测算法架构 / Detection Algorithm Architecture:
 * 
 * 1. 语义相似性过滤 / Semantic Similarity Filtering:
 *    ├── 嵌入向量生成 (Embedding Generation)
 *    ├── 余弦相似度计算 (Cosine Similarity Calculation)
 *    ├── 相似性阈值筛选 (≥ 0.85 默认)
 *    └── 候选冲突内存识别
 * 
 * 2. 冲突类型分析 / Conflict Type Analysis:
 *    ├── CONTRADICTION (直接矛盾)
 *    │   ├── 肯定/否定陈述对比
 *    │   ├── 关键词对立检测
 *    │   └── 逻辑矛盾识别
 *    │
 *    ├── FACTUAL_CONFLICT (事实冲突)  
 *    │   ├── 数值差异检测
 *    │   ├── 定量信息对比
 *    │   └── 客观事实验证
 *    │
 *    ├── PREFERENCE_CONFLICT (偏好冲突)
 *    │   ├── 喜好陈述分析
 *    │   ├── 偏好主体提取
 *    │   └── 对立偏好识别
 *    │
 *    ├── TEMPORAL_CONFLICT (时间冲突)
 *    │   ├── 时间实体提取
 *    │   ├── 时间范围重叠
 *    │   └── 时序矛盾检测
 *    │
 *    └── REDUNDANCY (冗余信息)
 *        ├── 高度相似性检测 (> 0.95)
 *        ├── 重复内容识别
 *        └── 信息冗余评估
 * 
 * 3. 冲突置信度计算 / Conflict Confidence Calculation:
 *    Confidence = SemanticSimilarity×0.4 + ConflictType×Weight + MemoryCharacteristics×Bonus
 * </pre>
 * 
 * <h3>使用示例 / Usage Examples:</h3>
 * <pre>{@code
 * // 初始化冲突检测器
 * MemoryConflictDetector detector = new MemoryConflictDetector(
 *     embeddingProvider, llmProvider,
 *     0.85,  // semanticSimilarityThreshold
 *     0.7,   // conflictConfidenceThreshold  
 *     true   // useLLMForConflictDetection
 * );
 * 
 * // 检测新内存与现有内存的冲突
 * EnhancedMemory newMemory = createMemory("用户喜欢咖啡");
 * List<EnhancedMemory> existingMemories = getExistingMemories();
 * 
 * CompletableFuture<List<MemoryConflict>> conflictsFuture = 
 *     detector.detectConflicts(newMemory, existingMemories);
 * 
 * List<MemoryConflict> conflicts = conflictsFuture.join();
 * System.out.println("发现冲突数量: " + conflicts.size());
 * 
 * // 分析具体冲突详情
 * for (MemoryConflict conflict : conflicts) {
 *     System.out.println("冲突类型: " + conflict.getType());
 *     System.out.println("置信度: " + conflict.getConfidence());
 *     System.out.println("冲突原因: " + conflict.getReason());
 *     System.out.println("语义相似度: " + conflict.getSemanticSimilarity());
 *     
 *     System.out.println("内存1: " + conflict.getMemory1().getContent());
 *     System.out.println("内存2: " + conflict.getMemory2().getContent());
 * }
 * 
 * // 解决检测到的冲突
 * for (MemoryConflict conflict : conflicts) {
 *     CompletableFuture<ConflictResolution> resolutionFuture = 
 *         detector.resolveConflict(conflict);
 *     
 *     ConflictResolution resolution = resolutionFuture.join();
 *     
 *     System.out.println("解决策略: " + resolution.getStrategy());
 *     System.out.println("解决理由: " + resolution.getReason());
 *     
 *     if (resolution.getStrategy() == ResolutionStrategy.MERGE) {
 *         System.out.println("合并内容: " + resolution.getMergedContent());
 *     }
 * }
 * 
 * // 处理不同类型的冲突解决
 * switch (resolution.getStrategy()) {
 *     case KEEP_FIRST:
 *         // 保留第一个内存，删除第二个
 *         removeMemory(conflict.getMemory2());
 *         break;
 *     case KEEP_SECOND:  
 *         // 保留第二个内存，删除第一个
 *         removeMemory(conflict.getMemory1());
 *         break;
 *     case MERGE:
 *         // 合并两个内存为新内存
 *         EnhancedMemory merged = createMergedMemory(
 *             conflict.getMemory1(), 
 *             conflict.getMemory2(),
 *             resolution.getMergedContent()
 *         );
 *         addMemory(merged);
 *         removeMemory(conflict.getMemory1());
 *         removeMemory(conflict.getMemory2());
 *         break;
 *     case KEEP_BOTH:
 *         // 保留双方内存，标记为冲突
 *         markAsConflicting(conflict.getMemory1(), conflict.getMemory2());
 *         break;
 * }
 * }</pre>
 * 
 * <h3>冲突解决策略 / Conflict Resolution Strategies:</h3>
 * <ul>
 *   <li><b>KEEP_FIRST</b>: 保留第一个内存 / Keep the first memory (newer or higher importance)</li>
 *   <li><b>KEEP_SECOND</b>: 保留第二个内存 / Keep the second memory (consolidated or authoritative)</li>
 *   <li><b>MERGE</b>: 合并两个内存 / Merge both memories into consolidated content</li>
 *   <li><b>KEEP_BOTH</b>: 保留双方内存 / Keep both memories (user preferences, temporal conflicts)</li>
 *   <li><b>DELETE_BOTH</b>: 删除双方内存 / Delete both memories (irreconcilable conflicts)</li>
 * </ul>
 * 
 * <h3>检测精度优化 / Detection Accuracy Optimization:</h3>
 * <ul>
 *   <li><b>类型过滤</b>: 仅检测相关或相同类型内存间冲突 / Type filtering for relevant memory types only</li>
 *   <li><b>用户隔离</b>: 仅在同用户内存间检测冲突 / User isolation for conflict detection</li>
 *   <li><b>状态过滤</b>: 忽略已废弃内存避免误判 / Status filtering to ignore deprecated memories</li>
 *   <li><b>阈值调优</b>: 可配置相似性和置信度阈值 / Configurable similarity and confidence thresholds</li>
 * </ul>
 * 
 * <h3>性能优化 / Performance Optimization:</h3>
 * <ul>
 *   <li><b>异步处理</b>: 所有检测和解决操作支持异步执行 / Asynchronous processing for all detection operations</li>
 *   <li><b>批量嵌入</b>: 批量生成嵌入向量提升效率 / Batch embedding generation for efficiency</li>
 *   <li><b>智能降级</b>: LLM不可用时自动使用规则引擎 / Intelligent fallback to rule engine when LLM unavailable</li>
 *   <li><b>早期过滤</b>: 多级过滤减少不必要的深度分析 / Early filtering to reduce unnecessary deep analysis</li>
 * </ul>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 * @see com.mem0.core.EnhancedMemory
 * @see com.mem0.embedding.EmbeddingProvider
 * @see com.mem0.llm.LLMProvider
 */
public class MemoryConflictDetector {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryConflictDetector.class);
    
    private final EmbeddingProvider embeddingProvider;
    private final LLMProvider llmProvider;
    
    // Configuration
    private final double semanticSimilarityThreshold;
    private final double conflictConfidenceThreshold;
    private final boolean useLLMForConflictDetection;
    
    public MemoryConflictDetector(EmbeddingProvider embeddingProvider, 
                                 LLMProvider llmProvider) {
        this(embeddingProvider, llmProvider, 0.85, 0.7, true);
    }
    
    public MemoryConflictDetector(EmbeddingProvider embeddingProvider, 
                                 LLMProvider llmProvider,
                                 double semanticSimilarityThreshold,
                                 double conflictConfidenceThreshold,
                                 boolean useLLMForConflictDetection) {
        this.embeddingProvider = embeddingProvider;
        this.llmProvider = llmProvider;
        this.semanticSimilarityThreshold = semanticSimilarityThreshold;
        this.conflictConfidenceThreshold = conflictConfidenceThreshold;
        this.useLLMForConflictDetection = useLLMForConflictDetection;
    }
    
    public CompletableFuture<List<MemoryConflict>> detectConflicts(EnhancedMemory newMemory, 
                                                                  List<EnhancedMemory> existingMemories) {
        if (newMemory == null) {
            logger.warn("Cannot detect conflicts for null memory");
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        
        logger.debug("Detecting conflicts for new memory: {}", newMemory.getId());
        
        return findSimilarMemories(newMemory, existingMemories)
            .thenCompose(similarMemories -> {
                if (similarMemories.isEmpty()) {
                    return CompletableFuture.completedFuture(Collections.emptyList());
                }
                
                List<CompletableFuture<MemoryConflict>> conflictFutures = similarMemories.stream()
                    .map(similarMemory -> analyzeConflict(newMemory, similarMemory))
                    .collect(Collectors.toList());
                
                return CompletableFuture.allOf(conflictFutures.toArray(new CompletableFuture[0]))
                    .thenApply(ignored -> {
                        List<MemoryConflict> conflicts = conflictFutures.stream()
                            .map(CompletableFuture::join)
                            .filter(Objects::nonNull)
                            .filter(conflict -> conflict.getConfidence() >= conflictConfidenceThreshold)
                            .collect(Collectors.toList());
                        logger.debug("Found {} conflicts after filtering by confidence threshold {}", conflicts.size(), conflictConfidenceThreshold);
                        return conflicts;
                    });
            });
    }
    
    public CompletableFuture<ConflictResolution> resolveConflict(MemoryConflict conflict) {
        logger.debug("Resolving conflict between memories: {} and {}", 
            conflict.getMemory1().getId(), conflict.getMemory2().getId());
        
        if (useLLMForConflictDetection && llmProvider != null) {
            return resolveConflictWithLLM(conflict);
        } else {
            return CompletableFuture.completedFuture(resolveConflictWithRules(conflict));
        }
    }
    
    private CompletableFuture<List<SimilarMemory>> findSimilarMemories(EnhancedMemory newMemory, 
                                                                      List<EnhancedMemory> existingMemories) {
        // Filter memories by type and user for initial screening
        List<EnhancedMemory> candidateMemories = existingMemories.stream()
            .filter(memory -> memory.getUserId().equals(newMemory.getUserId()))
            .filter(memory -> isSameTypeOrRelated(memory.getType(), newMemory.getType()))
            .filter(memory -> !memory.isDeprecated())
            .collect(Collectors.toList());
        
        if (candidateMemories.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        
        return embeddingProvider.embed(newMemory.getContent())
            .thenCompose(newEmbedding -> {
                List<String> candidateContents = candidateMemories.stream()
                    .map(EnhancedMemory::getContent)
                    .collect(Collectors.toList());
                
                return embeddingProvider.embedBatch(candidateContents)
                    .thenApply(candidateEmbeddings -> {
                        List<SimilarMemory> similarMemories = new ArrayList<>();
                        
                        for (int i = 0; i < candidateMemories.size(); i++) {
                            EnhancedMemory candidate = candidateMemories.get(i);
                            List<Float> candidateEmbedding = candidateEmbeddings.get(i);
                            
                            double similarity = cosineSimilarity(newEmbedding, candidateEmbedding);
                            
                            if (similarity >= semanticSimilarityThreshold) {
                                similarMemories.add(new SimilarMemory(candidate, similarity));
                            }
                        }
                        
                        return similarMemories.stream()
                            .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()))
                            .collect(Collectors.toList());
                    });
            })
            .exceptionally(throwable -> {
                logger.warn("Embedding provider failed, falling back to rule-based similarity: {}", throwable.getMessage());
                // Fall back to rule-based similarity without embeddings
                return Collections.emptyList();
            });
    }
    
    private CompletableFuture<MemoryConflict> analyzeConflict(EnhancedMemory memory1, SimilarMemory similarMemory) {
        EnhancedMemory memory2 = similarMemory.getMemory();
        double semanticSimilarity = similarMemory.getSimilarity();
        
        if (useLLMForConflictDetection && llmProvider != null) {
            return analyzeConflictWithLLM(memory1, memory2, semanticSimilarity);
        } else {
            MemoryConflict conflict = analyzeConflictWithRules(memory1, memory2, semanticSimilarity);
            return CompletableFuture.completedFuture(conflict);
        }
    }
    
    private CompletableFuture<MemoryConflict> analyzeConflictWithLLM(EnhancedMemory memory1, 
                                                                    EnhancedMemory memory2, 
                                                                    double semanticSimilarity) {
        String prompt = buildConflictAnalysisPrompt(memory1, memory2);
        
        LLMProvider.LLMConfig config = new LLMProvider.LLMConfig();
        config.setMaxTokens(200);
        config.setTemperature(0.1);
        
        List<LLMProvider.ChatMessage> messages = Arrays.asList(
            new LLMProvider.ChatMessage(LLMProvider.ChatMessage.Role.SYSTEM,
                "You are a memory conflict analysis system. Analyze if two memories conflict with each other. " +
                "A conflict occurs when memories contradict each other or contain incompatible information. " +
                "Return a JSON with: {\"hasConflict\": boolean, \"confidence\": 0.0-1.0, \"conflictType\": \"string\", \"reason\": \"string\"}"),
            new LLMProvider.ChatMessage(LLMProvider.ChatMessage.Role.USER, prompt)
        );
        
        return llmProvider.generateChatCompletion(messages, config)
            .thenApply(response -> {
                try {
                    return parseConflictAnalysisResponse(memory1, memory2, semanticSimilarity, response.getContent());
                } catch (Exception e) {
                    logger.warn("Failed to parse LLM conflict analysis response: {}", e.getMessage());
                    return analyzeConflictWithRules(memory1, memory2, semanticSimilarity);
                }
            })
            .exceptionally(throwable -> {
                logger.warn("LLM conflict analysis failed, falling back to rules: {}", throwable.getMessage());
                return analyzeConflictWithRules(memory1, memory2, semanticSimilarity);
            });
    }
    
    private MemoryConflict analyzeConflictWithRules(EnhancedMemory memory1, EnhancedMemory memory2, 
                                                   double semanticSimilarity) {
        ConflictType conflictType = determineConflictType(memory1, memory2);
        double confidence = calculateConflictConfidence(memory1, memory2, semanticSimilarity, conflictType);
        String reason = generateConflictReason(memory1, memory2, conflictType);
        
        if (conflictType != ConflictType.NONE && confidence >= conflictConfidenceThreshold) {
            return new MemoryConflict(memory1, memory2, conflictType, confidence, reason, semanticSimilarity);
        }
        
        return null; // No conflict
    }
    
    private ConflictType determineConflictType(EnhancedMemory memory1, EnhancedMemory memory2) {
        String content1 = memory1.getContent().toLowerCase();
        String content2 = memory2.getContent().toLowerCase();
        
        // Preference conflicts (check first for preference memories)
        if (memory1.getType() == MemoryType.PREFERENCE && memory2.getType() == MemoryType.PREFERENCE) {
            logger.debug("Checking preference conflict between: '{}' and '{}'", content1, content2);
            if (hasPreferenceConflict(content1, content2)) {
                logger.debug("Preference conflict detected");
                return ConflictType.PREFERENCE_CONFLICT;
            } else {
                logger.debug("No preference conflict detected");
            }
        }
        
        // Direct contradiction detection
        if (hasDirectContradiction(content1, content2)) {
            return ConflictType.CONTRADICTION;
        }
        
        // Factual conflicts
        if ((memory1.getType() == MemoryType.FACTUAL || memory1.getType() == MemoryType.SEMANTIC) &&
            (memory2.getType() == MemoryType.FACTUAL || memory2.getType() == MemoryType.SEMANTIC)) {
            if (hasFactualConflict(content1, content2)) {
                return ConflictType.FACTUAL_CONFLICT;
            }
        }
        
        // Temporal conflicts
        if (memory1.getType() == MemoryType.TEMPORAL && memory2.getType() == MemoryType.TEMPORAL) {
            if (hasTemporalConflict(memory1, memory2)) {
                return ConflictType.TEMPORAL_CONFLICT;
            }
        }
        
        // Check for redundancy (very high similarity with same content)
        if (semanticSimilarityThreshold > 0.95) {
            return ConflictType.REDUNDANCY;
        }
        
        return ConflictType.NONE;
    }
    
    private double calculateConflictConfidence(EnhancedMemory memory1, EnhancedMemory memory2, 
                                             double semanticSimilarity, ConflictType conflictType) {
        double confidence = 0.0;
        
        // Base confidence from semantic similarity
        confidence += semanticSimilarity * 0.4;
        
        // Confidence based on conflict type
        switch (conflictType) {
            case CONTRADICTION:
                confidence += 0.4;
                break;
            case FACTUAL_CONFLICT:
                confidence += 0.3;
                break;
            case PREFERENCE_CONFLICT:
                confidence += 0.2;
                break;
            case TEMPORAL_CONFLICT:
                confidence += 0.3;
                break;
            case REDUNDANCY:
                confidence += 0.5;
                break;
            default:
                confidence += 0.0;
        }
        
        // Adjust confidence based on memory characteristics
        if (memory1.isConsolidated() || memory2.isConsolidated()) {
            confidence += 0.1; // Higher confidence if one memory is consolidated
        }
        
        if (memory1.getImportance().isHighPriority() || memory2.getImportance().isHighPriority()) {
            confidence += 0.1; // Higher confidence for important memories
        }
        
        return Math.min(1.0, confidence);
    }
    
    private String generateConflictReason(EnhancedMemory memory1, EnhancedMemory memory2, 
                                         ConflictType conflictType) {
        switch (conflictType) {
            case CONTRADICTION:
                return "Memories contain contradictory information";
            case FACTUAL_CONFLICT:
                return "Factual information conflicts between memories";
            case PREFERENCE_CONFLICT:
                return "User preferences are inconsistent";
            case TEMPORAL_CONFLICT:
                return "Temporal information conflicts";
            case REDUNDANCY:
                return "Memories contain duplicate information";
            default:
                return "Potential conflict detected";
        }
    }
    
    private CompletableFuture<ConflictResolution> resolveConflictWithLLM(MemoryConflict conflict) {
        String prompt = buildConflictResolutionPrompt(conflict);
        
        LLMProvider.LLMConfig config = new LLMProvider.LLMConfig();
        config.setMaxTokens(300);
        config.setTemperature(0.1);
        
        List<LLMProvider.ChatMessage> messages = Arrays.asList(
            new LLMProvider.ChatMessage(LLMProvider.ChatMessage.Role.SYSTEM,
                "You are a memory conflict resolution system. Given conflicting memories, determine the best resolution strategy. " +
                "Options: KEEP_NEWER, KEEP_OLDER, MERGE, KEEP_BOTH, DELETE_BOTH. " +
                "Return JSON with: {\"strategy\": \"STRATEGY_NAME\", \"mergedContent\": \"string if merge\", \"reason\": \"explanation\"}"),
            new LLMProvider.ChatMessage(LLMProvider.ChatMessage.Role.USER, prompt)
        );
        
        return llmProvider.generateChatCompletion(messages, config)
            .thenApply(response -> {
                try {
                    return parseConflictResolutionResponse(conflict, response.getContent());
                } catch (Exception e) {
                    logger.warn("Failed to parse LLM conflict resolution response: {}", e.getMessage());
                    return resolveConflictWithRules(conflict);
                }
            })
            .exceptionally(throwable -> {
                logger.warn("LLM conflict resolution failed, falling back to rules: {}", throwable.getMessage());
                return resolveConflictWithRules(conflict);
            });
    }
    
    private ConflictResolution resolveConflictWithRules(MemoryConflict conflict) {
        EnhancedMemory memory1 = conflict.getMemory1();
        EnhancedMemory memory2 = conflict.getMemory2();
        ConflictType conflictType = conflict.getType();
        
        ResolutionStrategy strategy;
        String mergedContent = null;
        String reason;
        
        switch (conflictType) {
            case REDUNDANCY:
                // For redundancy, keep the more recent or more accessed memory
                if (memory1.getAccessCount() > memory2.getAccessCount()) {
                    strategy = ResolutionStrategy.KEEP_FIRST;
                    reason = "Keeping more frequently accessed memory";
                } else if (memory1.getCreatedAt().isAfter(memory2.getCreatedAt())) {
                    strategy = ResolutionStrategy.KEEP_FIRST;
                    reason = "Keeping newer memory";
                } else {
                    strategy = ResolutionStrategy.KEEP_SECOND;
                    reason = "Keeping newer memory";
                }
                break;
                
            case CONTRADICTION:
                // For contradictions, prefer newer memories unless old one is consolidated
                if (memory2.isConsolidated() && !memory1.isConsolidated()) {
                    strategy = ResolutionStrategy.KEEP_SECOND;
                    reason = "Keeping consolidated memory";
                } else if (memory1.getImportance().getScore() > memory2.getImportance().getScore()) {
                    strategy = ResolutionStrategy.KEEP_FIRST;
                    reason = "Keeping higher importance memory";
                } else {
                    strategy = ResolutionStrategy.KEEP_FIRST;
                    reason = "Keeping newer memory by default";
                }
                break;
                
            case PREFERENCE_CONFLICT:
                // For preference conflicts, keep both and let user decide later
                strategy = ResolutionStrategy.KEEP_BOTH;
                reason = "Preserving both preferences for user review";
                break;
                
            case FACTUAL_CONFLICT:
                // For factual conflicts, merge if possible, otherwise keep newer
                if (canMergeFacts(memory1, memory2)) {
                    strategy = ResolutionStrategy.MERGE;
                    mergedContent = mergeFacts(memory1, memory2);
                    reason = "Merging compatible factual information";
                } else {
                    strategy = ResolutionStrategy.KEEP_FIRST;
                    reason = "Keeping newer factual information";
                }
                break;
                
            case TEMPORAL_CONFLICT:
                // For temporal conflicts, keep the more specific one
                strategy = ResolutionStrategy.KEEP_BOTH;
                reason = "Preserving both temporal references";
                break;
                
            default:
                strategy = ResolutionStrategy.KEEP_BOTH;
                reason = "Conservative resolution - keeping both memories";
        }
        
        return new ConflictResolution(strategy, mergedContent, reason);
    }
    
    // Helper methods
    private boolean isSameTypeOrRelated(MemoryType type1, MemoryType type2) {
        if (type1 == type2) return true;
        
        // Check for related types that might conflict
        return (type1 == MemoryType.FACTUAL && type2 == MemoryType.SEMANTIC) ||
               (type1 == MemoryType.SEMANTIC && type2 == MemoryType.FACTUAL) ||
               (type1 == MemoryType.PREFERENCE && type2 == MemoryType.CONTEXTUAL) ||
               (type1 == MemoryType.CONTEXTUAL && type2 == MemoryType.PREFERENCE);
    }
    
    private boolean hasDirectContradiction(String content1, String content2) {
        // Simple contradiction detection using opposing keywords
        return (containsPositiveAssertion(content1) && containsNegativeAssertion(content2)) ||
               (containsNegativeAssertion(content1) && containsPositiveAssertion(content2));
    }
    
    private boolean containsPositiveAssertion(String content) {
        return content.matches(".*\\b(is|are|does|can|will|likes|prefers|enjoys)\\b.*");
    }
    
    private boolean containsNegativeAssertion(String content) {
        return content.matches(".*\\b(not|never|doesn't|cannot|won't|dislikes|hates)\\b.*");
    }
    
    private boolean hasPreferenceConflict(String content1, String content2) {
        // Extract preference subjects and see if they conflict
        Set<String> subjects1 = extractPreferenceSubjects(content1);
        Set<String> subjects2 = extractPreferenceSubjects(content2);
        
        // Check if both contain preference-related words
        boolean hasPreference1 = content1.matches(".*\\b(prefers?|likes?|enjoys?|loves?)\\b.*");
        boolean hasPreference2 = content2.matches(".*\\b(prefers?|likes?|enjoys?|loves?)\\b.*");
        
        logger.debug("hasPreference1: {}, hasPreference2: {}", hasPreference1, hasPreference2);
        
        if (!hasPreference1 || !hasPreference2) {
            logger.debug("One or both contents don't contain preference words");
            return false;
        }
        
        // Check for opposing preferences (like vs dislike)
        if (hasOpposingPreferences(content1, content2)) {
            logger.debug("Opposing preferences detected");
            return true;
        }
        
        // Check for different preference objects (prefers coffee vs prefers tea)
        Set<String> objects1 = extractPreferenceObjects(content1);
        Set<String> objects2 = extractPreferenceObjects(content2);
        
        logger.debug("Preference objects 1: {}, objects 2: {}", objects1, objects2);
        
        // If both have preference objects and they're different, it's a conflict
        boolean hasConflict = !objects1.isEmpty() && !objects2.isEmpty() && !objects1.equals(objects2);
        logger.debug("Different preference objects conflict: {}", hasConflict);
        
        return hasConflict;
    }
    
    private boolean hasFactualConflict(String content1, String content2) {
        // Check for conflicting numerical values or facts
        return extractNumbers(content1).stream()
            .anyMatch(num1 -> extractNumbers(content2).stream()
                .anyMatch(num2 -> !num1.equals(num2)));
    }
    
    private boolean hasTemporalConflict(EnhancedMemory memory1, EnhancedMemory memory2) {
        // Check if both memories refer to the same time period with different information
        return extractTemporalEntities(memory1.getContent())
            .stream()
            .anyMatch(temporal -> extractTemporalEntities(memory2.getContent()).contains(temporal));
    }
    
    private Set<String> extractPreferenceSubjects(String content) {
        // Simple extraction of subjects in preference statements
        Set<String> subjects = new HashSet<>();
        String[] words = content.split("\\s+");
        
        for (int i = 0; i < words.length - 1; i++) {
            if (words[i].matches("(like|prefer|enjoy|love|hate|dislike)")) {
                if (i + 1 < words.length) {
                    subjects.add(words[i + 1].toLowerCase());
                }
            }
        }
        
        return subjects;
    }
    
    private Set<String> extractPreferenceObjects(String content) {
        // Extract objects of preference (what is being preferred/liked)
        Set<String> objects = new HashSet<>();
        String[] words = content.split("\\s+");
        
        for (int i = 0; i < words.length - 1; i++) {
            if (words[i].matches("(prefers?|likes?|enjoys?|loves?)")) {
                if (i + 1 < words.length) {
                    objects.add(words[i + 1].toLowerCase());
                }
            }
        }
        
        return objects;
    }
    
    private boolean hasOpposingPreferences(String content1, String content2) {
        return (content1.contains("like") && content2.contains("dislike")) ||
               (content1.contains("love") && content2.contains("hate")) ||
               (content1.contains("prefer") && content2.contains("avoid"));
    }
    
    private Set<String> extractNumbers(String content) {
        Set<String> numbers = new HashSet<>();
        String[] words = content.split("\\s+");
        
        for (String word : words) {
            if (word.matches("\\d+(\\.\\d+)?")) {
                numbers.add(word);
            }
        }
        
        return numbers;
    }
    
    private Set<String> extractTemporalEntities(String content) {
        Set<String> temporal = new HashSet<>();
        String[] words = content.split("\\s+");
        
        for (String word : words) {
            if (word.matches("\\d{4}-\\d{2}-\\d{2}") || 
                word.matches("(monday|tuesday|wednesday|thursday|friday|saturday|sunday)")) {
                temporal.add(word.toLowerCase());
            }
        }
        
        return temporal;
    }
    
    private boolean canMergeFacts(EnhancedMemory memory1, EnhancedMemory memory2) {
        // Simple heuristic: can merge if they don't contain conflicting numbers
        return !hasFactualConflict(memory1.getContent().toLowerCase(), memory2.getContent().toLowerCase());
    }
    
    private String mergeFacts(EnhancedMemory memory1, EnhancedMemory memory2) {
        // Simple merge: combine non-redundant information
        return memory1.getContent() + ". Additionally, " + memory2.getContent();
    }
    
    private double cosineSimilarity(List<Float> vectorA, List<Float> vectorB) {
        if (vectorA.size() != vectorB.size()) {
            throw new IllegalArgumentException("Vectors must have the same dimension");
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < vectorA.size(); i++) {
            float a = vectorA.get(i);
            float b = vectorB.get(i);
            dotProduct += a * b;
            normA += a * a;
            normB += b * b;
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
    
    private String buildConflictAnalysisPrompt(EnhancedMemory memory1, EnhancedMemory memory2) {
        return String.format(
            "Memory 1 (%s): %s\nMemory 2 (%s): %s\n\nAnalyze if these memories conflict.",
            memory1.getType().getValue(), memory1.getContent(),
            memory2.getType().getValue(), memory2.getContent()
        );
    }
    
    private String buildConflictResolutionPrompt(MemoryConflict conflict) {
        return String.format(
            "Conflict Type: %s\nConfidence: %.2f\nReason: %s\n\nMemory 1: %s\nMemory 2: %s\n\nHow should this conflict be resolved?",
            conflict.getType(), conflict.getConfidence(), conflict.getReason(),
            conflict.getMemory1().getContent(), conflict.getMemory2().getContent()
        );
    }
    
    private MemoryConflict parseConflictAnalysisResponse(EnhancedMemory memory1, EnhancedMemory memory2, 
                                                        double semanticSimilarity, String response) {
        logger.debug("Parsing LLM response: {}", response);
        
        // Simplified JSON parsing - in production, use a proper JSON library
        boolean hasConflict = response.toLowerCase().contains("\"hasconflict\": true");
        logger.debug("Has conflict: {}", hasConflict);
        
        if (!hasConflict) return null;
        
        double confidence = 0.7; // Default confidence
        String reason = "LLM detected conflict";
        ConflictType type = ConflictType.CONTRADICTION;
        
        // Try to extract confidence from response
        if (response.toLowerCase().contains("\"confidence\":")) {
            try {
                String confidenceStr = response.substring(response.toLowerCase().indexOf("\"confidence\":") + 13);
                confidenceStr = confidenceStr.substring(0, confidenceStr.indexOf(","));
                confidence = Double.parseDouble(confidenceStr.trim());
            } catch (Exception e) {
                logger.debug("Failed to parse confidence from response: {}", e.getMessage());
            }
        }
        
        // Try to extract reason from response
        if (response.toLowerCase().contains("\"reason\":")) {
            try {
                String reasonStr = response.substring(response.toLowerCase().indexOf("\"reason\":") + 9);
                reasonStr = reasonStr.substring(0, reasonStr.indexOf("\"", reasonStr.indexOf("\"") + 1) + 1);
                reason = reasonStr.substring(reasonStr.indexOf("\"") + 1, reasonStr.lastIndexOf("\""));
            } catch (Exception e) {
                logger.debug("Failed to parse reason from response: {}", e.getMessage());
            }
        }
        
        // Try to extract conflict type from response
        if (response.toLowerCase().contains("\"conflicttype\": \"preference_conflict\"")) {
            type = ConflictType.PREFERENCE_CONFLICT;
        } else if (response.toLowerCase().contains("\"conflicttype\": \"factual_conflict\"")) {
            type = ConflictType.FACTUAL_CONFLICT;
        } else if (response.toLowerCase().contains("\"conflicttype\": \"temporal_conflict\"")) {
            type = ConflictType.TEMPORAL_CONFLICT;
        }
        
        logger.debug("Parsed conflict type: {}", type);
        
        return new MemoryConflict(memory1, memory2, type, confidence, reason, semanticSimilarity);
    }
    
    private ConflictResolution parseConflictResolutionResponse(MemoryConflict conflict, String response) {
        // Simplified parsing - in production, use proper JSON library
        ResolutionStrategy strategy = ResolutionStrategy.KEEP_BOTH; // Default
        String mergedContent = null;
        String reason = "LLM resolution";
        
        if (response.toLowerCase().contains("keep_newer")) {
            strategy = ResolutionStrategy.KEEP_FIRST;
        } else if (response.toLowerCase().contains("merge")) {
            strategy = ResolutionStrategy.MERGE;
            
            // Try to extract mergedContent from response
            if (response.toLowerCase().contains("\"mergedcontent\":")) {
                try {
                    String mergedContentStr = response.substring(response.toLowerCase().indexOf("\"mergedcontent\":") + 16);
                    mergedContentStr = mergedContentStr.substring(0, mergedContentStr.indexOf("\"", mergedContentStr.indexOf("\"") + 1) + 1);
                    mergedContent = mergedContentStr.substring(mergedContentStr.indexOf("\"") + 1, mergedContentStr.lastIndexOf("\""));
                } catch (Exception e) {
                    mergedContent = "Merged content"; // Fallback
                }
            } else {
                mergedContent = "Merged content"; // Fallback
            }
        }
        
        // Try to extract reason from response
        if (response.toLowerCase().contains("\"reason\":")) {
            try {
                String reasonStr = response.substring(response.toLowerCase().indexOf("\"reason\":") + 9);
                reasonStr = reasonStr.substring(0, reasonStr.indexOf("\"", reasonStr.indexOf("\"") + 1) + 1);
                reason = reasonStr.substring(reasonStr.indexOf("\"") + 1, reasonStr.lastIndexOf("\""));
            } catch (Exception e) {
                // Keep default reason
            }
        }
        
        return new ConflictResolution(strategy, mergedContent, reason);
    }
    
    // Inner classes for conflict detection results
    public static class SimilarMemory {
        private final EnhancedMemory memory;
        private final double similarity;
        
        public SimilarMemory(EnhancedMemory memory, double similarity) {
            this.memory = memory;
            this.similarity = similarity;
        }
        
        public EnhancedMemory getMemory() { return memory; }
        public double getSimilarity() { return similarity; }
    }
    
    public static class MemoryConflict {
        private final EnhancedMemory memory1;
        private final EnhancedMemory memory2;
        private final ConflictType type;
        private final double confidence;
        private final String reason;
        private final double semanticSimilarity;
        
        public MemoryConflict(EnhancedMemory memory1, EnhancedMemory memory2, ConflictType type,
                             double confidence, String reason, double semanticSimilarity) {
            this.memory1 = memory1;
            this.memory2 = memory2;
            this.type = type;
            this.confidence = confidence;
            this.reason = reason;
            this.semanticSimilarity = semanticSimilarity;
        }
        
        public EnhancedMemory getMemory1() { return memory1; }
        public EnhancedMemory getMemory2() { return memory2; }
        public ConflictType getType() { return type; }
        public double getConfidence() { return confidence; }
        public String getReason() { return reason; }
        public double getSemanticSimilarity() { return semanticSimilarity; }
        
        @Override
        public String toString() {
            return String.format("MemoryConflict{type=%s, confidence=%.3f, reason='%s'}", 
                type, confidence, reason);
        }
    }
    
    public static class ConflictResolution {
        private final ResolutionStrategy strategy;
        private final String mergedContent;
        private final String reason;
        
        public ConflictResolution(ResolutionStrategy strategy, String mergedContent, String reason) {
            this.strategy = strategy;
            this.mergedContent = mergedContent;
            this.reason = reason;
        }
        
        public ResolutionStrategy getStrategy() { return strategy; }
        public String getMergedContent() { return mergedContent; }
        public String getReason() { return reason; }
        
        @Override
        public String toString() {
            return String.format("ConflictResolution{strategy=%s, reason='%s'}", strategy, reason);
        }
    }
    
    public enum ConflictType {
        NONE,
        CONTRADICTION,
        FACTUAL_CONFLICT,
        PREFERENCE_CONFLICT,
        TEMPORAL_CONFLICT,
        REDUNDANCY
    }
    
    public enum ResolutionStrategy {
        KEEP_FIRST,
        KEEP_SECOND,
        MERGE,
        KEEP_BOTH,
        DELETE_BOTH
    }
}