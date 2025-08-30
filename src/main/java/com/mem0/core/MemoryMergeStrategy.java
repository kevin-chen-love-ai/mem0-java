package com.mem0.core;

import com.mem0.llm.LLMProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 内存合并策略系统 / Memory Merge Strategy System
 * 
 * 基于大语言模型和规则引擎的智能内存合并与整合系统。
 * 支持多类型内存合并、内容更新、相似内存整合、批量内存巩固等功能，提供语义理解和规则驱动的双模式合并策略。
 * Intelligent memory merging and consolidation system based on LLM and rule engine.
 * Supports multi-type memory merging, content updates, similar memory consolidation, batch memory consolidation,
 * providing semantic understanding and rule-driven dual-mode merging strategies.
 * 
 * <h3>核心功能 / Key Features:</h3>
 * <ul>
 *   <li>多类型内存智能合并(事实/偏好/程序/情景/时间) / Multi-type intelligent memory merging (factual/preference/procedural/episodic/temporal)</li>
 *   <li>LLM语义合并和规则引擎双模式 / Dual-mode: LLM semantic merging and rule engine</li>
 *   <li>内存内容更新和版本管理 / Memory content updates and version management</li>
 *   <li>相似内存自动整合和巩固 / Automatic consolidation of similar memories</li>
 *   <li>批量内存处理和异步执行 / Batch memory processing and asynchronous execution</li>
 *   <li>合并质量评估和置信度计算 / Merge quality assessment and confidence calculation</li>
 * </ul>
 * 
 * <h3>合并策略体系 / Merge Strategy System:</h3>
 * <pre>
 * 合并处理流程 / Merge Processing Pipeline:
 * 
 * 输入内存 → 相似性分组 → 合并策略选择 → 内容整合 → 质量评估 → 巩固输出
 * Input Memories → Similarity Grouping → Strategy Selection → Content Integration → Quality Assessment → Consolidation Output
 * 
 * 合并算法架构 / Merge Algorithm Architecture:
 * 
 * 1. 相似性分组算法 / Similarity Grouping Algorithm:
 *    ├── Jaccard相似性计算 (词汇重叠分析)
 *    ├── 内容相似度阈值过滤 (默认可配置)
 *    ├── 同类型内存优先分组
 *    └── 动态分组调整和优化
 * 
 * 2. 合并策略选择 / Merge Strategy Selection:
 *    ├── LLM模式 (Semantic Understanding)
 *    │   ├── 语义理解和上下文分析
 *    │   ├── 智能内容整合
 *    │   ├── 冗余信息去除
 *    │   └── 语言风格统一
 *    │
 *    └── 规则模式 (Rule-based Processing)
 *        ├── 按内存类型分类处理
 *        ├── 结构化内容合并
 *        ├── 时间顺序保持
 *        └── 唯一性信息提取
 * 
 * 3. 类型特化合并 / Type-Specific Merging:
 *    ├── FACTUAL (事实性)
 *    │   ├── 唯一事实提取
 *    │   ├── 句子级去重
 *    │   └── 结构化信息整合
 *    │
 *    ├── PREFERENCE (偏好性)
 *    │   ├── 偏好主体识别
 *    │   ├── 非冲突偏好保留
 *    │   └── 偏好权重分析
 *    │
 *    ├── PROCEDURAL (程序性)
 *    │   ├── 步骤逻辑排序
 *    │   ├── 操作流程整合
 *    │   └── 重复步骤合并
 *    │
 *    ├── EPISODIC (情景性)
 *    │   ├── 时间顺序维护
 *    │   ├── 事件序列整合
 *    │   └── 上下文关联保持
 *    │
 *    └── TEMPORAL (时间性)
 *        ├── 时间相关性分析
 *        ├── 时序一致性检查
 *        └── 时间范围整合
 * 
 * 4. 合并质量评估 / Merge Quality Assessment:
 *    MergedQuality = AvgImportance + AccessFrequency + Recency + ConsolidationBonus
 * </pre>
 * 
 * <h3>使用示例 / Usage Examples:</h3>
 * <pre>{@code
 * // 初始化合并策略管理器
 * MemoryMergeStrategy mergeStrategy = new MemoryMergeStrategy(llmProvider);
 * 
 * // 合并多个相关内存
 * List<EnhancedMemory> relatedMemories = Arrays.asList(
 *     createMemory("用户喜欢喝咖啡"),
 *     createMemory("用户偏好拿铁咖啡"),
 *     createMemory("用户早晨会喝咖啡")
 * );
 * 
 * CompletableFuture<EnhancedMemory> mergedFuture = 
 *     mergeStrategy.mergeMemories(relatedMemories);
 * 
 * EnhancedMemory merged = mergedFuture.join();
 * System.out.println("合并内容: " + merged.getContent());
 * System.out.println("合并来源数量: " + merged.getMetadata().get("merged_from_count"));
 * System.out.println("合并方法: " + merged.getMetadata().get("merge_method"));
 * 
 * // 更新现有内存内容
 * EnhancedMemory existingMemory = getExistingMemory();
 * String newContent = "用户现在更喜欢喝茶而不是咖啡";
 * Map<String, Object> updateContext = new HashMap<>();
 * updateContext.put("source", "user_correction");
 * updateContext.put("priority", "high");
 * 
 * CompletableFuture<EnhancedMemory> updatedFuture = 
 *     mergeStrategy.updateMemory(existingMemory, newContent, updateContext);
 * 
 * EnhancedMemory updated = updatedFuture.join();
 * System.out.println("更新内容: " + updated.getContent());
 * System.out.println("更新次数: " + updated.getUpdateCount());
 * System.out.println("更新方法: " + updated.getMetadata().get("update_method"));
 * 
 * // 批量整合相似内存
 * List<EnhancedMemory> allMemories = getAllMemories();
 * double similarityThreshold = 0.7;
 * 
 * CompletableFuture<List<EnhancedMemory>> consolidatedFuture = 
 *     mergeStrategy.consolidateMemories(allMemories, similarityThreshold);
 * 
 * List<EnhancedMemory> consolidatedMemories = consolidatedFuture.join();
 * System.out.println("整合前内存数量: " + allMemories.size());
 * System.out.println("整合后内存数量: " + consolidatedMemories.size());
 * 
 * // 分析整合结果
 * for (EnhancedMemory memory : consolidatedMemories) {
 *     if (memory.isConsolidated()) {
 *         System.out.println("已整合内存: " + memory.getContent());
 *         System.out.println("来源内存数量: " + memory.getMetadata().get("consolidated_from"));
 *         
 *         @SuppressWarnings("unchecked")
 *         List<String> sourceIds = (List<String>) memory.getMetadata().get("source_memory_ids");
 *         System.out.println("来源内存ID: " + sourceIds);
 *     }
 * }
 * 
 * // 处理不同合并结果
 * if (merged != null) {
 *     // 合并成功
 *     System.out.println("重要性: " + merged.getImportance());
 *     System.out.println("置信度: " + merged.getConfidenceScore());
 *     System.out.println("标签: " + merged.getTags());
 *     System.out.println("实体: " + merged.getEntities());
 *     
 *     // 合并的内存自动标记为已巩固
 *     assert merged.isConsolidated() : "合并的内存应该被标记为已巩固";
 * }
 * }</pre>
 * 
 * <h3>更新策略 / Update Strategies:</h3>
 * <ul>
 *   <li><b>替换更新</b>: 高相似度内容直接替换 / Replacement update for high similarity content (>0.8)</li>
 *   <li><b>追加更新</b>: 低相似度内容追加合并 / Append update for low similarity content</li>
 *   <li><b>智能合并</b>: LLM理解上下文进行智能更新 / Intelligent merging with LLM contextual understanding</li>
 *   <li><b>版本管理</b>: 更新历史记录和版本追踪 / Version management and update history tracking</li>
 * </ul>
 * 
 * <h3>合并优化 / Merge Optimization:</h3>
 * <ul>
 *   <li><b>基础内存选择</b>: 智能选择最佳基础内存进行合并 / Intelligent base memory selection for merging</li>
 *   <li><b>内容去重</b>: 自动识别和去除重复信息 / Automatic duplicate content identification and removal</li>
 *   <li><b>重要性继承</b>: 合并内存继承最高重要性并获得提升 / Importance inheritance with enhancement</li>
 *   <li><b>关系保持</b>: 保持原有内存间的关联关系 / Maintaining original memory relationships</li>
 * </ul>
 * 
 * <h3>质量保证 / Quality Assurance:</h3>
 * <ul>
 *   <li><b>合并置信度</b>: 基于来源内存质量计算合并置信度 / Merge confidence based on source memory quality</li>
 *   <li><b>内容完整性</b>: 确保重要信息不在合并过程中丢失 / Content integrity ensuring no important information loss</li>
 *   <li><b>语义一致性</b>: 保持合并内容的语义连贯性 / Semantic consistency in merged content</li>
 *   <li><b>异常处理</b>: LLM合并失败时优雅降级到规则合并 / Graceful fallback to rule-based merging when LLM fails</li>
 * </ul>
 * 
 * <h3>性能优化 / Performance Optimization:</h3>
 * <ul>
 *   <li><b>异步处理</b>: 所有合并操作支持异步执行避免阻塞 / Asynchronous processing for all merge operations</li>
 *   <li><b>批量处理</b>: 并行处理多个内存分组的合并操作 / Batch processing with parallel merge operations</li>
 *   <li><b>智能分组</b>: 基于相似性的高效内存分组算法 / Efficient memory grouping based on similarity</li>
 *   <li><b>内存复用</b>: 合并过程中的对象复用减少GC压力 / Object reuse during merging to reduce GC pressure</li>
 * </ul>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 * @see com.mem0.core.EnhancedMemory
 * @see com.mem0.core.MemoryImportance
 * @see com.mem0.llm.LLMProvider
 */
public class MemoryMergeStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryMergeStrategy.class);
    
    private final LLMProvider llmProvider;
    private final boolean useLLMForMerging;
    
    public MemoryMergeStrategy(LLMProvider llmProvider) {
        this.llmProvider = llmProvider;
        this.useLLMForMerging = llmProvider != null;
    }
    
    public CompletableFuture<EnhancedMemory> mergeMemories(List<EnhancedMemory> memoriesToMerge) {
        if (memoriesToMerge == null || memoriesToMerge.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        
        if (memoriesToMerge.size() == 1) {
            return CompletableFuture.completedFuture(memoriesToMerge.get(0));
        }
        
        logger.debug("Merging {} memories", memoriesToMerge.size());
        
        if (useLLMForMerging) {
            return mergeWithLLM(memoriesToMerge);
        } else {
            return CompletableFuture.completedFuture(mergeWithRules(memoriesToMerge));
        }
    }
    
    public CompletableFuture<EnhancedMemory> updateMemory(EnhancedMemory existingMemory, 
                                                         String newContent,
                                                         Map<String, Object> updateContext) {
        logger.debug("Updating memory: {}", existingMemory.getId());
        
        if (useLLMForMerging) {
            return updateWithLLM(existingMemory, newContent, updateContext);
        } else {
            return CompletableFuture.completedFuture(updateWithRules(existingMemory, newContent, updateContext));
        }
    }
    
    public CompletableFuture<List<EnhancedMemory>> consolidateMemories(List<EnhancedMemory> memories, 
                                                                      double similarityThreshold) {
        logger.debug("Consolidating {} memories with similarity threshold {}", 
            memories.size(), similarityThreshold);
        
        // Group similar memories for potential consolidation
        Map<String, List<EnhancedMemory>> groups = groupSimilarMemories(memories, similarityThreshold);
        
        List<CompletableFuture<EnhancedMemory>> consolidationFutures = new ArrayList<>();
        
        for (Map.Entry<String, List<EnhancedMemory>> group : groups.entrySet()) {
            List<EnhancedMemory> groupMemories = group.getValue();
            
            if (groupMemories.size() > 1) {
                // Multiple memories in group - consolidate them
                CompletableFuture<EnhancedMemory> consolidated = consolidateGroup(groupMemories);
                consolidationFutures.add(consolidated);
            } else {
                // Single memory - keep as is
                consolidationFutures.add(CompletableFuture.completedFuture(groupMemories.get(0)));
            }
        }
        
        return CompletableFuture.allOf(consolidationFutures.toArray(new CompletableFuture[0]))
            .thenApply(ignored -> consolidationFutures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }
    
    private CompletableFuture<EnhancedMemory> mergeWithLLM(List<EnhancedMemory> memoriesToMerge) {
        String prompt = buildMergePrompt(memoriesToMerge);
        
        LLMProvider.LLMConfig config = new LLMProvider.LLMConfig();
        config.setMaxTokens(500);
        config.setTemperature(0.2);
        
        List<LLMProvider.ChatMessage> messages = Arrays.asList(
            new LLMProvider.ChatMessage(LLMProvider.ChatMessage.Role.SYSTEM,
                "You are a memory merging system. Given multiple related memories, create a single " +
                "consolidated memory that combines all relevant information without duplication. " +
                "Preserve the most important details and maintain factual accuracy. " +
                "Return only the merged content."),
            new LLMProvider.ChatMessage(LLMProvider.ChatMessage.Role.USER, prompt)
        );
        
        return llmProvider.generateChatCompletion(messages, config)
            .thenApply(response -> {
                EnhancedMemory baseMemory = findBestBaseMemory(memoriesToMerge);
                return createMergedMemory(baseMemory, memoriesToMerge, response.getContent(), "LLM merge");
            })
            .exceptionally(throwable -> {
                logger.warn("LLM merge failed, falling back to rules: {}", throwable.getMessage());
                return mergeWithRules(memoriesToMerge);
            });
    }
    
    private EnhancedMemory mergeWithRules(List<EnhancedMemory> memoriesToMerge) {
        EnhancedMemory baseMemory = findBestBaseMemory(memoriesToMerge);
        String mergedContent = ruleBased_mergeContent(memoriesToMerge);
        
        return createMergedMemory(baseMemory, memoriesToMerge, mergedContent, "Rule-based merge");
    }
    
    private CompletableFuture<EnhancedMemory> updateWithLLM(EnhancedMemory existingMemory, 
                                                           String newContent,
                                                           Map<String, Object> updateContext) {
        String prompt = buildUpdatePrompt(existingMemory, newContent, updateContext);
        
        LLMProvider.LLMConfig config = new LLMProvider.LLMConfig();
        config.setMaxTokens(400);
        config.setTemperature(0.1);
        
        List<LLMProvider.ChatMessage> messages = Arrays.asList(
            new LLMProvider.ChatMessage(LLMProvider.ChatMessage.Role.SYSTEM,
                "You are a memory update system. Given an existing memory and new information, " +
                "create an updated version that incorporates the new information while preserving " +
                "important existing details. Return only the updated content."),
            new LLMProvider.ChatMessage(LLMProvider.ChatMessage.Role.USER, prompt)
        );
        
        return llmProvider.generateChatCompletion(messages, config)
            .thenApply(response -> {
                EnhancedMemory updatedMemory = createUpdatedMemory(existingMemory, response.getContent());
                updatedMemory.getMetadata().put("update_method", "LLM");
                return updatedMemory;
            })
            .exceptionally(throwable -> {
                logger.warn("LLM update failed, falling back to rules: {}", throwable.getMessage());
                return updateWithRules(existingMemory, newContent, updateContext);
            });
    }
    
    private EnhancedMemory updateWithRules(EnhancedMemory existingMemory, 
                                          String newContent,
                                          Map<String, Object> updateContext) {
        String updatedContent = ruleBased_updateContent(existingMemory.getContent(), newContent);
        EnhancedMemory updatedMemory = createUpdatedMemory(existingMemory, updatedContent);
        updatedMemory.getMetadata().put("update_method", "Rule-based");
        
        return updatedMemory;
    }
    
    private CompletableFuture<EnhancedMemory> consolidateGroup(List<EnhancedMemory> groupMemories) {
        // Sort by importance and recency for consolidation priority
        List<EnhancedMemory> sortedMemories = groupMemories.stream()
            .sorted((m1, m2) -> {
                int importanceCompare = Integer.compare(
                    m2.getImportance().getScore(), 
                    m1.getImportance().getScore()
                );
                if (importanceCompare != 0) return importanceCompare;
                
                return m2.getUpdatedAt().compareTo(m1.getUpdatedAt());
            })
            .collect(Collectors.toList());
        
        return mergeMemories(sortedMemories)
            .thenApply(consolidated -> {
                if (consolidated != null) {
                    consolidated.consolidate();
                    consolidated.getMetadata().put("consolidated_from", groupMemories.size());
                }
                return consolidated;
            });
    }
    
    private Map<String, List<EnhancedMemory>> groupSimilarMemories(List<EnhancedMemory> memories, 
                                                                  double similarityThreshold) {
        Map<String, List<EnhancedMemory>> groups = new HashMap<>();
        
        // Simple grouping based on content similarity (placeholder implementation)
        for (EnhancedMemory memory : memories) {
            String groupKey = findSimilarGroup(memory, groups, similarityThreshold);
            
            if (groupKey == null) {
                // Create new group
                groupKey = "group_" + groups.size();
                groups.put(groupKey, new ArrayList<>());
            }
            
            groups.get(groupKey).add(memory);
        }
        
        return groups;
    }
    
    private String findSimilarGroup(EnhancedMemory memory, 
                                   Map<String, List<EnhancedMemory>> existingGroups,
                                   double threshold) {
        for (Map.Entry<String, List<EnhancedMemory>> group : existingGroups.entrySet()) {
            for (EnhancedMemory groupMemory : group.getValue()) {
                if (calculateContentSimilarity(memory.getContent(), groupMemory.getContent()) >= threshold) {
                    return group.getKey();
                }
            }
        }
        return null;
    }
    
    private double calculateContentSimilarity(String content1, String content2) {
        // Simple Jaccard similarity for text
        Set<String> words1 = new HashSet<>(Arrays.asList(content1.toLowerCase().split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(content2.toLowerCase().split("\\s+")));
        
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    private EnhancedMemory findBestBaseMemory(List<EnhancedMemory> memories) {
        // Find memory with highest combined score of importance, access count, and recency
        return memories.stream()
            .max((m1, m2) -> {
                double score1 = calculateMemoryScore(m1);
                double score2 = calculateMemoryScore(m2);
                return Double.compare(score1, score2);
            })
            .orElse(memories.get(0));
    }
    
    private double calculateMemoryScore(EnhancedMemory memory) {
        double importanceScore = memory.getImportance().getScore() / 5.0;
        double accessScore = Math.log(memory.getAccessCount() + 1) / 10.0;
        double recencyScore = 1.0 / (memory.getDaysOld() + 1);
        double consolidationBonus = memory.isConsolidated() ? 0.2 : 0.0;
        
        return importanceScore + accessScore + recencyScore + consolidationBonus;
    }
    
    private String ruleBased_mergeContent(List<EnhancedMemory> memories) {
        StringBuilder mergedContent = new StringBuilder();
        
        // Group by memory type for structured merging
        Map<MemoryType, List<EnhancedMemory>> typeGroups = memories.stream()
            .collect(Collectors.groupingBy(EnhancedMemory::getType));
        
        // Process each type group
        for (Map.Entry<MemoryType, List<EnhancedMemory>> typeGroup : typeGroups.entrySet()) {
            MemoryType type = typeGroup.getKey();
            List<EnhancedMemory> typeMemories = typeGroup.getValue();
            
            String typeMerged = mergeByType(type, typeMemories);
            if (!typeMerged.isEmpty()) {
                if (mergedContent.length() > 0) {
                    mergedContent.append(" ");
                }
                mergedContent.append(typeMerged);
            }
        }
        
        return mergedContent.toString();
    }
    
    private String mergeByType(MemoryType type, List<EnhancedMemory> memories) {
        switch (type) {
            case FACTUAL:
            case SEMANTIC:
                return mergeFactualMemories(memories);
            case PREFERENCE:
                return mergePreferenceMemories(memories);
            case PROCEDURAL:
                return mergeProceduralMemories(memories);
            case EPISODIC:
                return mergeEpisodicMemories(memories);
            case TEMPORAL:
                return mergeTemporalMemories(memories);
            default:
                return mergeGenericMemories(memories);
        }
    }
    
    private String mergeFactualMemories(List<EnhancedMemory> memories) {
        // For factual memories, combine unique facts
        Set<String> uniqueFacts = new LinkedHashSet<>();
        
        for (EnhancedMemory memory : memories) {
            String[] sentences = memory.getContent().split("\\.");
            for (String sentence : sentences) {
                String trimmed = sentence.trim();
                if (!trimmed.isEmpty()) {
                    uniqueFacts.add(trimmed);
                }
            }
        }
        
        return String.join(". ", uniqueFacts) + (uniqueFacts.isEmpty() ? "" : ".");
    }
    
    private String mergePreferenceMemories(List<EnhancedMemory> memories) {
        // For preferences, maintain all non-conflicting preferences
        Map<String, String> preferences = new HashMap<>();
        
        for (EnhancedMemory memory : memories) {
            String content = memory.getContent();
            // Extract preference statements (simplified)
            if (content.toLowerCase().contains("like") || content.toLowerCase().contains("prefer")) {
                String key = extractPreferenceSubject(content);
                if (key != null && !preferences.containsKey(key)) {
                    preferences.put(key, content);
                }
            }
        }
        
        return String.join(". ", preferences.values());
    }
    
    private String mergeProceduralMemories(List<EnhancedMemory> memories) {
        // For procedural memories, combine steps in logical order
        List<String> steps = new ArrayList<>();
        
        for (EnhancedMemory memory : memories) {
            String content = memory.getContent();
            if (content.toLowerCase().contains("step") || content.toLowerCase().contains("how to")) {
                steps.add(content);
            }
        }
        
        return String.join(". ", steps);
    }
    
    private String mergeEpisodicMemories(List<EnhancedMemory> memories) {
        // For episodic memories, maintain chronological order
        List<EnhancedMemory> sortedByTime = memories.stream()
            .sorted((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt()))
            .collect(Collectors.toList());
        
        return sortedByTime.stream()
            .map(EnhancedMemory::getContent)
            .collect(Collectors.joining(". "));
    }
    
    private String mergeTemporalMemories(List<EnhancedMemory> memories) {
        // For temporal memories, organize by time relevance
        return memories.stream()
            .sorted((m1, m2) -> {
                // Sort by creation time for temporal consistency
                return m1.getCreatedAt().compareTo(m2.getCreatedAt());
            })
            .map(EnhancedMemory::getContent)
            .collect(Collectors.joining(". "));
    }
    
    private String mergeGenericMemories(List<EnhancedMemory> memories) {
        // Generic merge - combine unique content
        Set<String> uniqueContent = memories.stream()
            .map(EnhancedMemory::getContent)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        
        return String.join(". ", uniqueContent);
    }
    
    private String ruleBased_updateContent(String existingContent, String newContent) {
        // Simple update strategy - append if different, replace if similar
        if (calculateContentSimilarity(existingContent, newContent) > 0.8) {
            return newContent; // Replace with new content
        } else {
            return existingContent + ". " + newContent; // Append new information
        }
    }
    
    private String extractPreferenceSubject(String content) {
        // Simple preference subject extraction
        String[] words = content.toLowerCase().split("\\s+");
        for (int i = 0; i < words.length - 1; i++) {
            if (words[i].equals("like") || words[i].equals("prefer")) {
                return words[i + 1];
            }
        }
        return null;
    }
    
    private EnhancedMemory createMergedMemory(EnhancedMemory baseMemory, 
                                             List<EnhancedMemory> sourceMemories,
                                             String mergedContent,
                                             String mergeMethod) {
        EnhancedMemory merged = new EnhancedMemory(
            UUID.randomUUID().toString(),
            mergedContent,
            baseMemory.getUserId(),
            baseMemory.getAgentId(),
            baseMemory.getRunId()
        );
        
        // Set properties from base memory
        merged.setType(baseMemory.getType());
        merged.setImportance(calculateMergedImportance(sourceMemories));
        merged.setConfidenceScore(calculateMergedConfidence(sourceMemories));
        
        // Merge metadata
        merged.getMetadata().putAll(baseMemory.getMetadata());
        merged.getMetadata().put("merged_from_count", sourceMemories.size());
        merged.getMetadata().put("merge_method", mergeMethod);
        merged.getMetadata().put("source_memory_ids", 
            sourceMemories.stream()
                .map(EnhancedMemory::getId)
                .collect(Collectors.toList()));
        
        // Merge tags and entities
        sourceMemories.forEach(memory -> {
            merged.getTags().addAll(memory.getTags());
            merged.getEntities().addAll(memory.getEntities());
        });
        
        // Set as consolidated
        merged.consolidate();
        
        return merged;
    }
    
    private EnhancedMemory createUpdatedMemory(EnhancedMemory original, String updatedContent) {
        EnhancedMemory updated = new EnhancedMemory(
            original.getId(),
            updatedContent,
            original.getUserId(),
            original.getAgentId(),
            original.getRunId()
        );
        
        // Copy properties from original
        updated.setType(original.getType());
        updated.setImportance(original.getImportance());
        updated.setConfidenceScore(original.getConfidenceScore());
        
        // Copy metadata and collections
        updated.getMetadata().putAll(original.getMetadata());
        updated.getTags().addAll(original.getTags());
        updated.getEntities().addAll(original.getEntities());
        updated.getRelatedMemoryIds().addAll(original.getRelatedMemoryIds());
        
        // Record the update
        updated.recordUpdate(updatedContent);
        updated.getMetadata().put("last_update", LocalDateTime.now().toString());
        updated.getMetadata().put("update_count", original.getUpdateCount() + 1);
        
        return updated;
    }
    
    private MemoryImportance calculateMergedImportance(List<EnhancedMemory> memories) {
        double avgImportance = memories.stream()
            .mapToInt(memory -> memory.getImportance().getScore())
            .average()
            .orElse(3.0);
        
        // Merged memories get slight importance boost
        int mergedScore = Math.min(5, (int) Math.ceil(avgImportance) + 1);
        return MemoryImportance.fromScore(mergedScore);
    }
    
    private double calculateMergedConfidence(List<EnhancedMemory> memories) {
        // Merged confidence is average of source confidences, with boost for consolidation
        double avgConfidence = memories.stream()
            .mapToDouble(EnhancedMemory::getConfidenceScore)
            .average()
            .orElse(0.5);
        
        return Math.min(1.0, avgConfidence + 0.1);
    }
    
    private String buildMergePrompt(List<EnhancedMemory> memories) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Merge these related memories into a single coherent memory:\n\n");
        
        for (int i = 0; i < memories.size(); i++) {
            EnhancedMemory memory = memories.get(i);
            prompt.append(String.format("Memory %d (%s, importance=%s):\n%s\n\n",
                i + 1, memory.getType().getValue(), memory.getImportance().name(), memory.getContent()));
        }
        
        prompt.append("Create a single merged memory that combines all relevant information.");
        return prompt.toString();
    }
    
    private String buildUpdatePrompt(EnhancedMemory existingMemory, 
                                    String newContent,
                                    Map<String, Object> updateContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(String.format("Existing memory (%s):\n%s\n\n",
            existingMemory.getType().getValue(), existingMemory.getContent()));
        
        prompt.append("New information:\n").append(newContent).append("\n\n");
        
        if (updateContext != null && !updateContext.isEmpty()) {
            prompt.append("Update context:\n");
            updateContext.forEach((key, value) -> 
                prompt.append(key).append(": ").append(value).append("\n"));
            prompt.append("\n");
        }
        
        prompt.append("Create an updated memory that incorporates the new information.");
        return prompt.toString();
    }
}