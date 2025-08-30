package com.mem0.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mem0.core.EnhancedMemory;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 内存冲突解决器
 * 处理内存创建和更新时的冲突，提供多种冲突解决策略
 */
public class ConflictResolver {
    
    private static final Logger logger = LoggerFactory.getLogger(ConflictResolver.class);
    
    // 相似性阈值
    private static final double SIMILARITY_THRESHOLD = 0.85;
    private static final int MIN_CONTENT_LENGTH = 10;
    
    // 常用停用词
    private static final Set<String> STOP_WORDS = createStopWords();
    
    private static Set<String> createStopWords() {
        Set<String> stopWords = new HashSet<>();
        Collections.addAll(stopWords,
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
            "of", "with", "by", "from", "up", "about", "into", "through", "during",
            "before", "after", "above", "below", "between", "among", "is", "are",
            "was", "were", "be", "been", "being", "have", "has", "had", "do", "does",
            "did", "will", "would", "could", "should", "may", "might", "must", "can",
            "这", "是", "的", "在", "和", "与", "或", "但", "如果", "因为", "所以", "然后",
            "也", "都", "就", "可以", "没有", "不是", "一个", "这个", "那个", "什么", "怎么"
        );
        return Collections.unmodifiableSet(stopWords);
    }
    
    // 文本预处理模式
    private static final Pattern PUNCTUATION_PATTERN = Pattern.compile("[\\p{Punct}\\s]+");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    /**
     * 检查内容是否相似
     */
    public boolean isSimilarContent(String content1, String content2) {
        if (content1 == null || content2 == null) {
            return false;
        }
        
        if (content1.equals(content2)) {
            return true;
        }
        
        // 长度检查
        if (content1.length() < MIN_CONTENT_LENGTH || content2.length() < MIN_CONTENT_LENGTH) {
            return false;
        }
        
        // 计算相似度
        double similarity = calculateTextSimilarity(content1, content2);
        return similarity >= SIMILARITY_THRESHOLD;
    }

    /**
     * 解决创建冲突
     */
    public ConcurrentMemoryManager.ConflictResolution resolveCreationConflict(String newContent, String userId, 
                                                                             EnhancedMemory existingMemory) {
        logger.info("解决创建冲突: 用户={}, 现有内存ID={}", userId, existingMemory.getId());
        
        // 分析冲突类型
        ConflictType conflictType = analyzeConflictType(newContent, existingMemory.getContent());
        
        switch (conflictType) {
            case DUPLICATE:
                // 重复内容，忽略新创建
                return new ConcurrentMemoryManager.ConflictResolution(
                    ConcurrentMemoryManager.ConflictStrategy.IGNORE,
                    existingMemory.getId(),
                    existingMemory.getContent()
                );
                
            case SIMILAR_WITH_UPDATES:
                // 相似内容但有更新，合并
                String mergedContent = mergeContent(existingMemory.getContent(), newContent);
                return new ConcurrentMemoryManager.ConflictResolution(
                    ConcurrentMemoryManager.ConflictStrategy.MERGE,
                    existingMemory.getId(),
                    mergedContent
                );
                
            case COMPLEMENTARY:
                // 互补内容，合并
                String complementaryMerged = mergeComplementaryContent(existingMemory.getContent(), newContent);
                return new ConcurrentMemoryManager.ConflictResolution(
                    ConcurrentMemoryManager.ConflictStrategy.MERGE,
                    existingMemory.getId(),
                    complementaryMerged
                );
                
            case CONTRADICTORY:
                // 冲突内容，保留更新的
                if (isMoreRecent(newContent, existingMemory.getContent())) {
                    return new ConcurrentMemoryManager.ConflictResolution(
                        ConcurrentMemoryManager.ConflictStrategy.REPLACE,
                        existingMemory.getId(),
                        newContent
                    );
                } else {
                    return new ConcurrentMemoryManager.ConflictResolution(
                        ConcurrentMemoryManager.ConflictStrategy.IGNORE,
                        existingMemory.getId(),
                        existingMemory.getContent()
                    );
                }
                
            default:
                // 默认策略：创建新内存
                return new ConcurrentMemoryManager.ConflictResolution(
                    ConcurrentMemoryManager.ConflictStrategy.CREATE_NEW,
                    null,
                    newContent
                );
        }
    }

    /**
     * 解决更新冲突
     */
    public ConcurrentMemoryManager.ConflictResolution resolveUpdateConflict(String memoryId, String newContent, 
                                                                           EnhancedMemory existingMemory) {
        logger.info("解决更新冲突: 内存ID={}", memoryId);
        
        // 检查是否是有意义的更新
        if (isMeaningfulUpdate(existingMemory.getContent(), newContent)) {
            // 有意义的更新，直接替换
            return new ConcurrentMemoryManager.ConflictResolution(
                ConcurrentMemoryManager.ConflictStrategy.REPLACE,
                memoryId,
                newContent
            );
        } else {
            // 微小变化，合并内容
            String mergedContent = mergeContent(existingMemory.getContent(), newContent);
            return new ConcurrentMemoryManager.ConflictResolution(
                ConcurrentMemoryManager.ConflictStrategy.MERGE,
                memoryId,
                mergedContent
            );
        }
    }

    /**
     * 解决批量创建冲突
     */
    public List<ConcurrentMemoryManager.ConflictResolution> resolveBatchConflicts(
            List<String> newContents, String userId, List<EnhancedMemory> existingMemories) {
        
        logger.info("解决批量创建冲突: 新内容数={}, 现有内存数={}", newContents.size(), existingMemories.size());
        
        List<ConcurrentMemoryManager.ConflictResolution> resolutions = new ArrayList<>();
        
        // 构建现有内存的内容映射
        Map<String, EnhancedMemory> contentMap = new HashMap<>();
        for (EnhancedMemory memory : existingMemories) {
            String normalizedContent = normalizeContent(memory.getContent());
            contentMap.put(normalizedContent, memory);
        }
        
        // 对每个新内容检查冲突
        for (String newContent : newContents) {
            String normalizedNew = normalizeContent(newContent);
            
            // 查找最相似的现有内存
            EnhancedMemory mostSimilar = findMostSimilarMemory(newContent, existingMemories);
            
            if (mostSimilar != null && isSimilarContent(newContent, mostSimilar.getContent())) {
                // 发现冲突，解决
                ConcurrentMemoryManager.ConflictResolution resolution = 
                    resolveCreationConflict(newContent, userId, mostSimilar);
                resolutions.add(resolution);
            } else {
                // 无冲突，创建新内存
                resolutions.add(new ConcurrentMemoryManager.ConflictResolution(
                    ConcurrentMemoryManager.ConflictStrategy.CREATE_NEW,
                    null,
                    newContent
                ));
            }
        }
        
        return resolutions;
    }

    // 私有辅助方法

    private double calculateTextSimilarity(String text1, String text2) {
        // 预处理文本
        List<String> tokens1 = tokenize(text1);
        List<String> tokens2 = tokenize(text2);
        
        if (tokens1.isEmpty() || tokens2.isEmpty()) {
            return 0.0;
        }
        
        // 使用Jaccard相似度
        Set<String> set1 = new HashSet<>(tokens1);
        Set<String> set2 = new HashSet<>(tokens2);
        
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private List<String> tokenize(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        // 转换为小写并分词
        String[] words = PUNCTUATION_PATTERN.split(text.toLowerCase());
        List<String> tokens = new ArrayList<>();
        
        for (String word : words) {
            word = word.trim();
            if (!word.isEmpty() && word.length() > 1 && !STOP_WORDS.contains(word)) {
                tokens.add(word);
            }
        }
        
        return tokens;
    }

    private String normalizeContent(String content) {
        if (content == null) {
            return "";
        }
        
        return WHITESPACE_PATTERN.matcher(content.toLowerCase().trim()).replaceAll(" ");
    }

    private ConflictType analyzeConflictType(String newContent, String existingContent) {
        double similarity = calculateTextSimilarity(newContent, existingContent);
        
        if (similarity > 0.95) {
            return ConflictType.DUPLICATE;
        } else if (similarity > 0.7) {
            if (containsContradiction(newContent, existingContent)) {
                return ConflictType.CONTRADICTORY;
            } else if (isComplementary(newContent, existingContent)) {
                return ConflictType.COMPLEMENTARY;
            } else {
                return ConflictType.SIMILAR_WITH_UPDATES;
            }
        } else {
            return ConflictType.DIFFERENT;
        }
    }

    private boolean containsContradiction(String content1, String content2) {
        // 简化的矛盾检测：查找否定词汇
        String[] negationPatterns = {
            "not", "no", "never", "none", "neither", "nor", "cannot", "can't", "won't", "don't", "doesn't",
            "不", "没", "否", "非", "无", "未", "别", "勿"
        };
        
        List<String> tokens1 = tokenize(content1);
        List<String> tokens2 = tokenize(content2);
        
        boolean hasNegation1 = tokens1.stream().anyMatch(token -> 
            Arrays.stream(negationPatterns).anyMatch(token::contains));
        boolean hasNegation2 = tokens2.stream().anyMatch(token -> 
            Arrays.stream(negationPatterns).anyMatch(token::contains));
        
        // 如果一个有否定词，另一个没有，可能是矛盾
        return hasNegation1 != hasNegation2;
    }

    private boolean isComplementary(String content1, String content2) {
        // 检查内容是否互补（例如，一个是问题，另一个是答案）
        String[] questionWords = {"what", "how", "why", "when", "where", "who", "which", "什么", "怎么", "为什么", "什么时候", "哪里", "谁"};
        String[] answerWords = {"because", "answer", "solution", "result", "因为", "答案", "解决", "结果"};
        
        List<String> tokens1 = tokenize(content1);
        List<String> tokens2 = tokenize(content2);
        
        boolean hasQuestion1 = tokens1.stream().anyMatch(token -> 
            Arrays.stream(questionWords).anyMatch(token::contains));
        boolean hasAnswer2 = tokens2.stream().anyMatch(token -> 
            Arrays.stream(answerWords).anyMatch(token::contains));
        
        boolean hasQuestion2 = tokens2.stream().anyMatch(token -> 
            Arrays.stream(questionWords).anyMatch(token::contains));
        boolean hasAnswer1 = tokens1.stream().anyMatch(token -> 
            Arrays.stream(answerWords).anyMatch(token::contains));
        
        return (hasQuestion1 && hasAnswer2) || (hasQuestion2 && hasAnswer1);
    }

    private boolean isMoreRecent(String content1, String content2) {
        // 简化的时间判断：更长的内容可能更新
        // 在实际应用中，应该基于时间戳或版本信息
        return content1.length() > content2.length();
    }

    private String mergeContent(String existingContent, String newContent) {
        if (existingContent == null) return newContent;
        if (newContent == null) return existingContent;
        
        // 简化的合并策略：保留较长的内容，并尝试合并独特信息
        List<String> existingTokens = tokenize(existingContent);
        List<String> newTokens = tokenize(newContent);
        
        Set<String> allTokens = new LinkedHashSet<>(existingTokens);
        allTokens.addAll(newTokens);
        
        // 如果新内容更长且包含更多信息，优先使用新内容
        if (newContent.length() > existingContent.length() * 1.2) {
            return newContent + "\n\n[更新信息] " + extractUniqueInfo(existingContent, newContent);
        } else {
            return existingContent + "\n\n[补充信息] " + extractUniqueInfo(newContent, existingContent);
        }
    }

    private String mergeComplementaryContent(String content1, String content2) {
        return content1 + "\n\n[相关内容] " + content2;
    }

    private String extractUniqueInfo(String fromContent, String comparedToContent) {
        List<String> fromTokens = tokenize(fromContent);
        List<String> compareTokens = tokenize(comparedToContent);
        
        Set<String> compareSet = new HashSet<>(compareTokens);
        
        List<String> uniqueTokens = fromTokens.stream()
            .filter(token -> !compareSet.contains(token))
            .distinct()
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        
        if (uniqueTokens.isEmpty()) {
            return "";
        }
        
        return String.join(" ", uniqueTokens);
    }

    private boolean isMeaningfulUpdate(String oldContent, String newContent) {
        if (oldContent == null && newContent != null) return true;
        if (newContent == null) return false;
        
        // 检查长度变化
        double lengthRatio = (double) newContent.length() / oldContent.length();
        if (lengthRatio < 0.5 || lengthRatio > 2.0) {
            return true; // 显著的长度变化
        }
        
        // 检查内容变化
        double similarity = calculateTextSimilarity(oldContent, newContent);
        return similarity < 0.8; // 相似度低于80%认为是有意义的更新
    }

    private EnhancedMemory findMostSimilarMemory(String content, List<EnhancedMemory> memories) {
        if (memories == null || memories.isEmpty()) {
            return null;
        }
        
        double maxSimilarity = 0.0;
        EnhancedMemory mostSimilar = null;
        
        for (EnhancedMemory memory : memories) {
            double similarity = calculateTextSimilarity(content, memory.getContent());
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                mostSimilar = memory;
            }
        }
        
        return maxSimilarity >= SIMILARITY_THRESHOLD ? mostSimilar : null;
    }

    // 枚举类型

    private enum ConflictType {
        DUPLICATE,           // 重复内容
        SIMILAR_WITH_UPDATES,// 相似但有更新
        COMPLEMENTARY,       // 互补内容
        CONTRADICTORY,       // 矛盾内容
        DIFFERENT            // 不同内容
    }
}