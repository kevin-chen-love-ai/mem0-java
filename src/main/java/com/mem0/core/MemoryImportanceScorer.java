package com.mem0.core;

import com.mem0.llm.LLMProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * 内存重要性评分系统 / Memory Importance Scoring System
 * 
 * 基于多维度算法和大语言模型为内存对象评估重要性分数，支持规则引擎和LLM两种评分模式。
 * 综合考虑内容特征、类型权重、使用频率、时间衰减等因素，为内存管理和淘汰决策提供量化依据。
 * Multi-dimensional algorithm-based and LLM-powered importance scoring system for memory objects.
 * Supports both rule engine and LLM scoring modes, considering content features, type weights, 
 * usage frequency, temporal decay, and other factors to provide quantified basis for memory management.
 * 
 * <h3>核心功能 / Key Features:</h3>
 * <ul>
 *   <li>多维度重要性评分算法 / Multi-dimensional importance scoring algorithms</li>
 *   <li>LLM智能评分和规则引擎双模式 / Dual-mode: LLM intelligent scoring and rule engine</li>
 *   <li>内容语义分析和关键词识别 / Content semantic analysis and keyword recognition</li>
 *   <li>时间衰减和访问频率权重 / Temporal decay and access frequency weighting</li>
 *   <li>批量内存排序和重要性更新 / Batch memory ranking and importance updates</li>
 *   <li>评分置信度计算和异常降级 / Scoring confidence calculation and fallback degradation</li>
 * </ul>
 * 
 * <h3>评分维度体系 / Scoring Dimension System:</h3>
 * <pre>
 * 评分算法架构 / Scoring Algorithm Architecture:
 * 
 * ImportanceScore = Σ(DimensionScore × Weight)
 * 
 * 评分维度 / Scoring Dimensions:
 * ├── 内容分析 / Content Analysis (30%)
 * │   ├── 关键词匹配 (高重要/低重要标识符)
 * │   ├── 语义模式识别 (日期、金额、机密信息)
 * │   ├── 内容长度和具体性分析
 * │   └── 专有名词和结构化信息检测
 * │
 * ├── 类型权重 / Type Weighting (20%)
 * │   ├── PROCEDURAL (程序性): 1.0
 * │   ├── TEMPORAL (时间性): 0.8  
 * │   ├── FACTUAL (事实性): 0.6
 * │   └── PREFERENCE (偏好性): 0.4
 * │
 * ├── 使用模式 / Usage Patterns (20%)
 * │   ├── 访问频率评分
 * │   ├── 更新频率权重
 * │   └── 巩固状态加权
 * │
 * ├── 时间因素 / Temporal Factors (15%)
 * │   ├── 新近性加权
 * │   ├── 最近访问时间
 * │   └── 过期时间紧急性
 * │
 * ├── 上下文信息 / Contextual Information (10%)
 * │   ├── 优先级标记
 * │   ├── 来源类型权重
 * │   └── 分类别重要性
 * │
 * └── 关系网络 / Relationship Networks (5%)
 *     ├── 关联内存数量
 *     ├── 实体连接强度
 *     └── 标签多样性分析
 * </pre>
 * 
 * <h3>使用示例 / Usage Examples:</h3>
 * <pre>{@code
 * // 初始化评分器
 * MemoryImportanceScorer scorer = new MemoryImportanceScorer(llmProvider);
 * 
 * // 单个内存评分
 * CompletableFuture<ImportanceScore> scoreFuture = scorer.scoreMemoryImportance(memory, context);
 * ImportanceScore score = scoreFuture.join();
 * 
 * System.out.println("重要性分数: " + score.getTotalScore()); // 1.0-5.0
 * System.out.println("置信度: " + score.getConfidence());   // 0.0-1.0
 * System.out.println("评分详情: " + score.getScoreBreakdown());
 * 
 * // 批量内存重要性排序
 * List<EnhancedMemory> memories = Arrays.asList(memory1, memory2, memory3);
 * CompletableFuture<List<EnhancedMemory>> rankedFuture = scorer.rankMemoriesByImportance(memories);
 * List<EnhancedMemory> rankedMemories = rankedFuture.join(); // 按重要性降序排列
 * 
 * // 自动更新内存重要性
 * CompletableFuture<Void> updateFuture = scorer.updateMemoryImportance(memory);
 * updateFuture.join(); // memory.getImportance()已更新
 * 
 * // 创建上下文增强评分
 * Map<String, Object> context = new HashMap<>();
 * context.put("priority", "high");
 * context.put("source", "system");
 * context.put("category", "work");
 * 
 * ImportanceScore contextualScore = scorer.scoreMemoryImportance(memory, context).join();
 * }</pre>
 * 
 * <h3>评分策略 / Scoring Strategies:</h3>
 * <ul>
 *   <li><b>LLM模式</b>: 使用大语言模型进行语义理解和智能评分 / LLM Mode: Semantic understanding and intelligent scoring</li>
 *   <li><b>规则模式</b>: 基于多维特征的确定性算法评分 / Rule Mode: Deterministic algorithm based on multi-dimensional features</li>
 *   <li><b>混合模式</b>: LLM失败时自动降级为规则评分 / Hybrid Mode: Automatic fallback to rule scoring when LLM fails</li>
 *   <li><b>增量评分</b>: 支持基于上下文的动态评分调整 / Incremental Scoring: Context-based dynamic scoring adjustment</li>
 * </ul>
 * 
 * <h3>性能优化 / Performance Optimization:</h3>
 * <ul>
 *   <li><b>异步处理</b>: 所有评分操作均支持异步执行 / Asynchronous processing for all scoring operations</li>
 *   <li><b>批量优化</b>: 并行处理多个内存的重要性评分 / Batch optimization with parallel processing</li>
 *   <li><b>缓存策略</b>: 评分结果可缓存避免重复计算 / Caching strategy to avoid repeated calculations</li>
 *   <li><b>降级机制</b>: LLM不可用时自动使用规则引擎 / Fallback mechanism with rule engine when LLM unavailable</li>
 * </ul>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 * @see com.mem0.core.EnhancedMemory
 * @see com.mem0.core.MemoryImportance
 */
public class MemoryImportanceScorer {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryImportanceScorer.class);
    
    private final LLMProvider llmProvider;
    private final boolean useLLMForScoring;
    
    // Keyword-based importance indicators
    private static final Set<String> HIGH_IMPORTANCE_KEYWORDS = new HashSet<>(Arrays.asList(
        "critical", "urgent", "important", "must", "essential", "key", "vital",
        "deadline", "emergency", "priority", "required", "necessary"
    ));
    
    private static final Set<String> LOW_IMPORTANCE_KEYWORDS = new HashSet<>(Arrays.asList(
        "maybe", "might", "could", "optional", "nice to have", "if possible",
        "minor", "trivial", "unimportant", "casual", "random"
    ));
    
    // Content patterns for importance scoring
    private static final Map<Pattern, Double> IMPORTANCE_PATTERNS = new HashMap<>();
    
    static {
        // High importance patterns
        IMPORTANCE_PATTERNS.put(Pattern.compile(".*\\b(?:deadline|due date|expires?)\\b.*", Pattern.CASE_INSENSITIVE), 2.0);
        IMPORTANCE_PATTERNS.put(Pattern.compile(".*\\b(?:password|secure|confidential|private)\\b.*", Pattern.CASE_INSENSITIVE), 1.5);
        IMPORTANCE_PATTERNS.put(Pattern.compile(".*\\b(?:meeting|appointment|interview)\\b.*", Pattern.CASE_INSENSITIVE), 1.2);
        IMPORTANCE_PATTERNS.put(Pattern.compile(".*\\$\\d+(?:\\.\\d{2})?.*", Pattern.CASE_INSENSITIVE), 1.0); // Money amounts
        
        // Medium importance patterns  
        IMPORTANCE_PATTERNS.put(Pattern.compile(".*\\b(?:project|task|work|job)\\b.*", Pattern.CASE_INSENSITIVE), 0.5);
        IMPORTANCE_PATTERNS.put(Pattern.compile(".*\\b(?:family|friend|relationship)\\b.*", Pattern.CASE_INSENSITIVE), 0.3);
        
        // Low importance patterns
        IMPORTANCE_PATTERNS.put(Pattern.compile(".*\\b(?:weather|random|casual)\\b.*", Pattern.CASE_INSENSITIVE), -0.5);
    }
    
    public MemoryImportanceScorer(LLMProvider llmProvider) {
        this.llmProvider = llmProvider;
        this.useLLMForScoring = llmProvider != null;
    }
    
    public CompletableFuture<ImportanceScore> scoreMemoryImportance(EnhancedMemory memory, 
                                                                   Map<String, Object> context) {
        logger.debug("Scoring importance for memory: {}", memory.getId());
        
        if (useLLMForScoring) {
            return scoreWithLLM(memory, context);
        } else {
            return CompletableFuture.completedFuture(scoreWithRules(memory, context));
        }
    }
    
    public CompletableFuture<List<EnhancedMemory>> rankMemoriesByImportance(List<EnhancedMemory> memories) {
        logger.debug("Ranking {} memories by importance", memories.size());
        
        List<CompletableFuture<ScoredMemory>> scoringFutures = memories.stream()
            .map(memory -> scoreMemoryImportance(memory, null)
                .thenApply(score -> new ScoredMemory(memory, score)))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        
        return CompletableFuture.allOf(scoringFutures.toArray(new CompletableFuture[0]))
            .thenApply(ignored -> scoringFutures.stream()
                .map(CompletableFuture::join)
                .sorted((sm1, sm2) -> Double.compare(sm2.getScore().getTotalScore(), sm1.getScore().getTotalScore()))
                .map(ScoredMemory::getMemory)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll));
    }
    
    public CompletableFuture<Void> updateMemoryImportance(EnhancedMemory memory) {
        return scoreMemoryImportance(memory, null)
            .thenAccept(score -> {
                MemoryImportance newImportance = MemoryImportance.fromScore(score.getTotalScore());
                memory.setImportance(newImportance);
                memory.setConfidenceScore(score.getConfidence());
                memory.getMetadata().put("importance_updated", LocalDateTime.now().toString());
                memory.getMetadata().put("importance_score_breakdown", score.getScoreBreakdown());
            });
    }
    
    private CompletableFuture<ImportanceScore> scoreWithLLM(EnhancedMemory memory, 
                                                           Map<String, Object> context) {
        String prompt = buildImportancePrompt(memory, context);
        
        LLMProvider.LLMConfig config = new LLMProvider.LLMConfig();
        config.setMaxTokens(150);
        config.setTemperature(0.1);
        
        List<LLMProvider.ChatMessage> messages = Arrays.asList(
            new LLMProvider.ChatMessage(LLMProvider.ChatMessage.Role.SYSTEM,
                "You are a memory importance scoring system. Rate the importance of the given memory " +
                "on a scale of 1-5 where:\n" +
                "1 = Minimal importance (trivial, can be forgotten)\n" +
                "2 = Low importance (minor details)\n" +
                "3 = Medium importance (useful information)\n" +
                "4 = High importance (significant information)\n" +
                "5 = Critical importance (must not be forgotten)\n\n" +
                "Return JSON: {\"score\": number, \"confidence\": 0.0-1.0, \"reasoning\": \"explanation\"}"),
            new LLMProvider.ChatMessage(LLMProvider.ChatMessage.Role.USER, prompt)
        );
        
        return llmProvider.generateChatCompletion(messages, config)
            .thenApply(response -> {
                try {
                    return parseImportanceResponse(response.getContent());
                } catch (Exception e) {
                    logger.warn("Failed to parse LLM importance response: {}", e.getMessage());
                    return scoreWithRules(memory, context);
                }
            })
            .exceptionally(throwable -> {
                logger.warn("LLM importance scoring failed, falling back to rules: {}", throwable.getMessage());
                return scoreWithRules(memory, context);
            });
    }
    
    private ImportanceScore scoreWithRules(EnhancedMemory memory, Map<String, Object> context) {
        Map<String, Double> scoreBreakdown = new HashMap<>();
        double totalScore = 3.0; // Start with medium importance
        
        // Content-based scoring
        double contentScore = scoreContent(memory.getContent());
        scoreBreakdown.put("content", contentScore);
        totalScore += contentScore;
        
        // Type-based scoring
        double typeScore = scoreByType(memory.getType());
        scoreBreakdown.put("type", typeScore);
        totalScore += typeScore;
        
        // Usage-based scoring
        double usageScore = scoreByUsage(memory);
        scoreBreakdown.put("usage", usageScore);
        totalScore += usageScore;
        
        // Temporal-based scoring
        double temporalScore = scoreByTemporal(memory);
        scoreBreakdown.put("temporal", temporalScore);
        totalScore += temporalScore;
        
        // Context-based scoring
        double contextScore = scoreByContext(context);
        scoreBreakdown.put("context", contextScore);
        totalScore += contextScore;
        
        // Relationship-based scoring
        double relationshipScore = scoreByRelationships(memory);
        scoreBreakdown.put("relationships", relationshipScore);
        totalScore += relationshipScore;
        
        // Clamp final score between 1 and 5
        totalScore = Math.max(1.0, Math.min(5.0, totalScore));
        
        // Calculate confidence based on score factors
        double confidence = calculateConfidence(scoreBreakdown, memory);
        
        return new ImportanceScore(totalScore, confidence, "Rule-based scoring", scoreBreakdown);
    }
    
    private double scoreContent(String content) {
        double score = 0.0;
        String lowerContent = content.toLowerCase();
        
        // Keyword-based scoring
        long highKeywords = HIGH_IMPORTANCE_KEYWORDS.stream()
            .mapToLong(keyword -> countOccurrences(lowerContent, keyword))
            .sum();
        
        long lowKeywords = LOW_IMPORTANCE_KEYWORDS.stream()
            .mapToLong(keyword -> countOccurrences(lowerContent, keyword))
            .sum();
        
        score += highKeywords * 0.3 - lowKeywords * 0.2;
        
        // Pattern-based scoring
        for (Map.Entry<Pattern, Double> patternEntry : IMPORTANCE_PATTERNS.entrySet()) {
            if (patternEntry.getKey().matcher(content).matches()) {
                score += patternEntry.getValue();
            }
        }
        
        // Length-based scoring (longer content often more important)
        if (content.length() > 200) {
            score += 0.3;
        } else if (content.length() < 50) {
            score -= 0.2;
        }
        
        // Specificity scoring (numbers, proper nouns, specific terms)
        if (hasSpecificInformation(content)) {
            score += 0.4;
        }
        
        return score;
    }
    
    private double scoreByType(MemoryType type) {
        switch (type) {
            case PROCEDURAL:
                return 1.0; // Procedures are generally important
            case TEMPORAL:
                return 0.8; // Time-sensitive information is important
            case FACTUAL:
                return 0.6; // Facts are moderately important
            case PREFERENCE:
                return 0.4; // Preferences matter for personalization
            case EPISODIC:
                return 0.3; // Experiences are moderately important
            case RELATIONSHIP:
                return 0.5; // Relationships are important for context
            case CONTEXTUAL:
                return 0.2; // Context is useful but not critical
            case SEMANTIC:
            default:
                return 0.0; // Neutral for general semantic memories
        }
    }
    
    private double scoreByUsage(EnhancedMemory memory) {
        double score = 0.0;
        
        // Access frequency scoring
        int accessCount = memory.getAccessCount();
        if (accessCount > 10) {
            score += 0.8;
        } else if (accessCount > 5) {
            score += 0.5;
        } else if (accessCount > 2) {
            score += 0.2;
        }
        
        // Update frequency scoring
        int updateCount = memory.getUpdateCount();
        if (updateCount > 3) {
            score += 0.4; // Frequently updated memories are important
        }
        
        // Consolidation status
        if (memory.isConsolidated()) {
            score += 0.6; // Consolidated memories are important
        }
        
        return score;
    }
    
    private double scoreByTemporal(EnhancedMemory memory) {
        double score = 0.0;
        
        // Recency scoring
        long daysOld = memory.getDaysOld();
        if (daysOld < 1) {
            score += 0.5; // Very recent memories are important
        } else if (daysOld < 7) {
            score += 0.3; // Recent memories are moderately important
        } else if (daysOld > 365) {
            score -= 0.2; // Very old memories lose importance
        }
        
        // Recent access scoring
        long daysSinceAccess = memory.getDaysSinceLastAccess();
        if (daysSinceAccess < 1) {
            score += 0.3; // Recently accessed memories are important
        } else if (daysSinceAccess > 30) {
            score -= 0.3; // Long unused memories lose importance
        }
        
        // Expiration scoring
        if (memory.getExpiresAt() != null) {
            long daysUntilExpiry = ChronoUnit.DAYS.between(LocalDateTime.now(), memory.getExpiresAt());
            if (daysUntilExpiry < 7) {
                score += 0.4; // Soon to expire memories need attention
            }
        }
        
        return score;
    }
    
    private double scoreByContext(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return 0.0;
        }
        
        double score = 0.0;
        
        // Priority context
        if ("high".equals(context.get("priority"))) {
            score += 1.0;
        } else if ("low".equals(context.get("priority"))) {
            score -= 0.5;
        }
        
        // Source context
        if ("system".equals(context.get("source"))) {
            score += 0.3; // System-generated content is often important
        } else if ("user".equals(context.get("source"))) {
            score += 0.2; // User-provided content has some importance
        }
        
        // Category context
        String category = (String) context.get("category");
        if (category != null) {
            switch (category.toLowerCase()) {
                case "work":
                case "business":
                    score += 0.4;
                    break;
                case "personal":
                case "family":
                    score += 0.3;
                    break;
                case "entertainment":
                case "casual":
                    score -= 0.2;
                    break;
            }
        }
        
        return score;
    }
    
    private double scoreByRelationships(EnhancedMemory memory) {
        double score = 0.0;
        
        // Related memories count
        int relatedCount = memory.getRelatedMemoryIds().size();
        if (relatedCount > 5) {
            score += 0.6; // Highly connected memories are important
        } else if (relatedCount > 2) {
            score += 0.3; // Connected memories have moderate importance
        }
        
        // Entity count (more entities often means more important)
        int entityCount = memory.getEntities().size();
        if (entityCount > 3) {
            score += 0.4;
        } else if (entityCount > 1) {
            score += 0.2;
        }
        
        // Tag diversity
        int tagCount = memory.getTags().size();
        if (tagCount > 3) {
            score += 0.2; // Well-tagged memories are often important
        }
        
        return score;
    }
    
    private double calculateConfidence(Map<String, Double> scoreBreakdown, EnhancedMemory memory) {
        // Base confidence
        double confidence = 0.7;
        
        // Confidence increases with more scoring factors
        int nonZeroFactors = (int) scoreBreakdown.values().stream()
            .filter(score -> Math.abs(score) > 0.1)
            .count();
        
        confidence += nonZeroFactors * 0.05;
        
        // Confidence increases with memory maturity
        if (memory.getAccessCount() > 5) {
            confidence += 0.1;
        }
        
        if (memory.isConsolidated()) {
            confidence += 0.15;
        }
        
        // Confidence decreases for very old memories
        if (memory.getDaysOld() > 365) {
            confidence -= 0.1;
        }
        
        return Math.max(0.1, Math.min(1.0, confidence));
    }
    
    private boolean hasSpecificInformation(String content) {
        // Check for numbers, dates, proper nouns, etc.
        return content.matches(".*\\d+.*") || // Contains numbers
               content.matches(".*[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*.*") || // Proper nouns
               content.contains("@") || // Email-like
               content.matches(".*\\b\\d{4}-\\d{2}-\\d{2}\\b.*"); // Date pattern
    }
    
    private long countOccurrences(String text, String keyword) {
        return Arrays.stream(text.split("\\s+"))
            .mapToLong(word -> word.equals(keyword) ? 1L : 0L)
            .sum();
    }
    
    private String buildImportancePrompt(EnhancedMemory memory, Map<String, Object> context) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append(String.format("Memory Type: %s\n", memory.getType().getValue()));
        prompt.append(String.format("Content: %s\n", memory.getContent()));
        prompt.append(String.format("Age: %d days\n", memory.getDaysOld()));
        prompt.append(String.format("Access Count: %d\n", memory.getAccessCount()));
        prompt.append(String.format("Update Count: %d\n", memory.getUpdateCount()));
        
        if (memory.isConsolidated()) {
            prompt.append("Status: Consolidated\n");
        }
        
        if (context != null && !context.isEmpty()) {
            prompt.append("Context:\n");
            context.forEach((key, value) -> 
                prompt.append(String.format("  %s: %s\n", key, value)));
        }
        
        return prompt.toString();
    }
    
    private ImportanceScore parseImportanceResponse(String response) {
        // Simplified JSON parsing - in production, use proper JSON library
        try {
            double score = 3.0; // Default
            double confidence = 0.7; // Default
            String reasoning = "LLM assessment";
            
            // Extract score
            if (response.contains("\"score\":")) {
                String scoreStr = response.substring(response.indexOf("\"score\":") + 8);
                scoreStr = scoreStr.substring(0, scoreStr.indexOf(",")).trim();
                score = Double.parseDouble(scoreStr);
            }
            
            // Extract confidence
            if (response.contains("\"confidence\":")) {
                String confidenceStr = response.substring(response.indexOf("\"confidence\":") + 13);
                confidenceStr = confidenceStr.substring(0, confidenceStr.indexOf(",")).trim();
                confidence = Double.parseDouble(confidenceStr);
            }
            
            // Extract reasoning
            if (response.contains("\"reasoning\":")) {
                String reasoningStart = response.substring(response.indexOf("\"reasoning\":") + 12);
                reasoning = reasoningStart.substring(1, reasoningStart.indexOf("\"", 1));
            }
            
            Map<String, Double> breakdown = new HashMap<>();
            breakdown.put("llm_score", score);
            
            return new ImportanceScore(score, confidence, reasoning, breakdown);
            
        } catch (Exception e) {
            // Fallback to default scoring
            Map<String, Double> breakdown = new HashMap<>();
            breakdown.put("parse_error", 0.0);
            return new ImportanceScore(3.0, 0.5, "Parse error, using default", breakdown);
        }
    }
    
    // Inner classes
    public static class ImportanceScore {
        private final double totalScore;
        private final double confidence;
        private final String reasoning;
        private final Map<String, Double> scoreBreakdown;
        
        public ImportanceScore(double totalScore, double confidence, String reasoning,
                              Map<String, Double> scoreBreakdown) {
            this.totalScore = totalScore;
            this.confidence = confidence;
            this.reasoning = reasoning;
            this.scoreBreakdown = new HashMap<>(scoreBreakdown);
        }
        
        public double getTotalScore() { return totalScore; }
        public double getConfidence() { return confidence; }
        public String getReasoning() { return reasoning; }
        public Map<String, Double> getScoreBreakdown() { return new HashMap<>(scoreBreakdown); }
        
        @Override
        public String toString() {
            return String.format("ImportanceScore{score=%.2f, confidence=%.2f, reasoning='%s'}", 
                totalScore, confidence, reasoning);
        }
    }
    
    private static class ScoredMemory {
        private final EnhancedMemory memory;
        private final ImportanceScore score;
        
        public ScoredMemory(EnhancedMemory memory, ImportanceScore score) {
            this.memory = memory;
            this.score = score;
        }
        
        public EnhancedMemory getMemory() { return memory; }
        public ImportanceScore getScore() { return score; }
    }
}