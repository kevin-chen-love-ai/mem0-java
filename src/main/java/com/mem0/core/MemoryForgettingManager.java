package com.mem0.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 内存遗忘管理系统 / Memory Forgetting Management System
 * 
 * 基于艾宾浩斯遗忘曲线和多策略遗忘算法的智能内存淘汰管理系统。
 * 支持渐进式衰减、重要性评估、访问模式分析等多维度遗忘策略，为内存系统提供科学的清理和维护机制。
 * Intelligent memory eviction management system based on Ebbinghaus forgetting curve and multi-strategy algorithms.
 * Supports gradual decay, importance assessment, access pattern analysis and multi-dimensional forgetting strategies,
 * providing scientific cleanup and maintenance mechanisms for memory systems.
 * 
 * <h3>核心功能 / Key Features:</h3>
 * <ul>
 *   <li>基于艾宾浩斯遗忘曲线的科学衰减模型 / Scientific decay model based on Ebbinghaus forgetting curve</li>
 *   <li>多策略遗忘策略引擎(渐进/激进/保守/重要性/访问) / Multi-strategy forgetting engine (gradual/aggressive/conservative/importance/access)</li>
 *   <li>内存衰减分数计算和保留评估 / Memory decay scoring and retention assessment</li>
 *   <li>智能剪枝算法(LRU/重要性/时间/衰减/均衡策略) / Intelligent pruning algorithms (LRU/importance/temporal/decay/balanced)</li>
 *   <li>内存强化和巩固机制 / Memory reinforcement and consolidation mechanisms</li>
 *   <li>批量处理和异步执行支持 / Batch processing and asynchronous execution support</li>
 * </ul>
 * 
 * <h3>遗忘策略体系 / Forgetting Strategy System:</h3>
 * <pre>
 * 艾宾浩斯遗忘曲线模型 / Ebbinghaus Forgetting Curve Model:
 * 
 * R(t) = e^(-t/S)
 * 其中 R(t) = 保留率, t = 时间, S = 内存强度
 * Where R(t) = retention rate, t = time, S = memory strength
 * 
 * 遗忘决策算法 / Forgetting Decision Algorithm:
 * 
 * DecayScore = forgettingCurveBase * e^(-combinedDecay / forgettingCurveDecay)
 * ShouldForget = DecayScore > RetentionThreshold
 * 
 * 策略分类 / Strategy Categories:
 * ├── NEVER_FORGET (永不遗忘)
 * │   └── 适用于关键内存和巩固记忆
 * │
 * ├── GRADUAL_DECAY (渐进衰减) 
 * │   ├── 默认策略，平衡保留和清理
 * │   └── 衰减阈值: 0.2 (可配置)
 * │
 * ├── AGGRESSIVE_FORGETTING (激进遗忘)
 * │   ├── 快速清理，节省存储空间
 * │   └── 衰减阈值: 0.4 (更低保留)
 * │
 * ├── CONSERVATIVE_FORGETTING (保守遗忘)
 * │   ├── 谨慎清理，最大化保留
 * │   └── 衰减阈值: 0.1 (更高保留)
 * │
 * ├── IMPORTANCE_BASED (重要性驱动)
 * │   ├── 基于内存重要性调整阈值
 * │   └── 阈值 = 基础阈值 * (6-重要性分数)/5
 * │
 * └── ACCESS_BASED (访问频率驱动)
 *     ├── 基于访问模式调整阈值
 *     └── 高频访问内存阈值减半
 * </pre>
 * 
 * <h3>使用示例 / Usage Examples:</h3>
 * <pre>{@code
 * // 初始化遗忘管理器
 * MemoryForgettingManager forgettingManager = new MemoryForgettingManager(
 *     0.5,   // forgettingCurveBase
 *     0.1,   // forgettingCurveDecay  
 *     0.2    // memoryRetentionThreshold
 * );
 * 
 * // 批量处理内存衰减
 * List<EnhancedMemory> memories = getMemoriesFromStore();
 * CompletableFuture<List<EnhancedMemory>> processedFuture = 
 *     forgettingManager.processMemoryDecay(memories);
 * 
 * List<EnhancedMemory> remainingMemories = processedFuture.join();
 * System.out.println("保留内存数量: " + remainingMemories.size());
 * 
 * // 识别需要遗忘的内存
 * CompletableFuture<List<EnhancedMemory>> forgettableFuture = 
 *     forgettingManager.identifyMemoriesForForgetting(memories);
 * 
 * List<EnhancedMemory> toForget = forgettableFuture.join();
 * System.out.println("待遗忘内存: " + toForget.size());
 * 
 * // 计算单个内存的衰减信息
 * EnhancedMemory memory = memories.get(0);
 * CompletableFuture<MemoryDecayInfo> decayInfoFuture = 
 *     forgettingManager.calculateMemoryDecay(memory);
 * 
 * MemoryDecayInfo decayInfo = decayInfoFuture.join();
 * System.out.println("衰减分数: " + decayInfo.getDecayScore());
 * System.out.println("保留分数: " + decayInfo.getRetentionScore());
 * System.out.println("应该遗忘: " + decayInfo.shouldForget());
 * System.out.println("预计遗忘天数: " + decayInfo.getDaysUntilForgetting());
 * 
 * // 强化重要内存
 * CompletableFuture<Void> reinforceFuture = 
 *     forgettingManager.reinforceMemory(memory);
 * reinforceFuture.join();
 * 
 * System.out.println("内存已强化，新置信度: " + memory.getConfidenceScore());
 * 
 * // 智能剪枝操作
 * CompletableFuture<List<EnhancedMemory>> prunedFuture = 
 *     forgettingManager.pruneOldMemories(memories, 100, 
 *         MemoryForgettingManager.PruningStrategy.BALANCED);
 * 
 * List<EnhancedMemory> prunedMemories = prunedFuture.join();
 * System.out.println("剪枝后内存数量: " + prunedMemories.size());
 * }</pre>
 * 
 * <h3>剪枝策略 / Pruning Strategies:</h3>
 * <ul>
 *   <li><b>LEAST_RECENTLY_USED</b>: LRU策略，保留最近访问的内存 / Keep most recently accessed memories</li>
 *   <li><b>LEAST_IMPORTANT</b>: 重要性策略，保留高重要性内存 / Keep high-importance memories</li>
 *   <li><b>OLDEST_FIRST</b>: 时间策略，保留较新的内存 / Keep newer memories</li>
 *   <li><b>LOWEST_DECAY_SCORE</b>: 衰减策略，保留低衰减分数内存 / Keep low-decay-score memories</li>
 *   <li><b>BALANCED</b>: 均衡策略，综合考虑多个因素 / Balanced strategy considering multiple factors</li>
 * </ul>
 * 
 * <h3>衰减因子分析 / Decay Factor Analysis:</h3>
 * <ul>
 *   <li><b>age_factor</b>: 内存年龄影响 / Memory age impact</li>
 *   <li><b>access_recency</b>: 最近访问时间 / Recent access time</li>
 *   <li><b>access_frequency</b>: 访问频率权重 / Access frequency weight</li>
 *   <li><b>importance_score</b>: 重要性分数影响 / Importance score impact</li>
 *   <li><b>confidence_score</b>: 置信度分数权重 / Confidence score weight</li>
 *   <li><b>relationship_count</b>: 关系连接数量 / Relationship connection count</li>
 *   <li><b>is_consolidated</b>: 巩固状态加权 / Consolidation status weight</li>
 * </ul>
 * 
 * <h3>性能优化 / Performance Optimization:</h3>
 * <ul>
 *   <li><b>异步处理</b>: 所有操作均支持异步执行避免阻塞 / Asynchronous processing to avoid blocking</li>
 *   <li><b>批量优化</b>: 并行处理大量内存的衰减计算 / Batch optimization for processing large memory sets</li>
 *   <li><b>智能阈值</b>: 动态调整遗忘阈值提升效率 / Dynamic threshold adjustment for efficiency</li>
 *   <li><b>内存强化</b>: 重要内存自动强化机制 / Automatic reinforcement for important memories</li>
 * </ul>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 * @see com.mem0.core.EnhancedMemory
 * @see com.mem0.core.MemoryImportance
 */
public class MemoryForgettingManager {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryForgettingManager.class);
    
    // Forgetting curve parameters (based on Ebbinghaus forgetting curve)
    private final double forgettingCurveBase;
    private final double forgettingCurveDecay;
    
    // Configuration
    private final ForgettingPolicy defaultPolicy;
    private final Map<MemoryType, ForgettingPolicy> typePolicies;
    private final double memoryRetentionThreshold;
    private MemoryForgettingPolicy customPolicy;
    
    public MemoryForgettingManager() {
        this(0.5, 0.1, 0.2);
    }
    
    public MemoryForgettingManager(double forgettingCurveBase, 
                                  double forgettingCurveDecay,
                                  double memoryRetentionThreshold) {
        this.forgettingCurveBase = forgettingCurveBase;
        this.forgettingCurveDecay = forgettingCurveDecay;
        this.memoryRetentionThreshold = memoryRetentionThreshold;
        
        this.defaultPolicy = ForgettingPolicy.GRADUAL_DECAY;
        this.typePolicies = initializeTypePolicies();
    }
    
    /**
     * Set a custom forgetting policy that overrides the default behavior
     * @param policy the custom policy to apply
     */
    public void setForgettingPolicy(MemoryForgettingPolicy policy) {
        this.customPolicy = policy;
        logger.debug("Custom forgetting policy set: enabled={}, decayRate={}, importanceThreshold={}", 
                    policy.isForgettingEnabled(), policy.getDecayRate(), policy.getImportanceThreshold());
    }
    
    public CompletableFuture<List<EnhancedMemory>> processMemoryDecay(List<EnhancedMemory> memories) {
        logger.debug("Processing memory decay for {} memories", memories.size());
        
        return CompletableFuture.supplyAsync(() -> {
            List<EnhancedMemory> processedMemories = new ArrayList<>();
            
            for (EnhancedMemory memory : memories) {
                if (shouldForgetMemory(memory)) {
                    logger.debug("Memory {} marked for forgetting", memory.getId());
                    memory.deprecate();
                } else {
                    // Update memory strength based on decay
                    updateMemoryStrength(memory);
                }
                processedMemories.add(memory);
            }
            
            return processedMemories;
        });
    }
    
    public CompletableFuture<List<EnhancedMemory>> identifyMemoriesForForgetting(List<EnhancedMemory> memories) {
        return CompletableFuture.supplyAsync(() -> 
            memories.stream()
                .filter(this::shouldForgetMemory)
                .collect(Collectors.toList())
        );
    }
    
    public CompletableFuture<MemoryDecayInfo> calculateMemoryDecay(EnhancedMemory memory) {
        return CompletableFuture.supplyAsync(() -> {
            double decayScore = calculateDecayScore(memory);
            double retentionScore = calculateRetentionScore(memory);
            boolean shouldForget = shouldForgetMemory(memory);
            
            ForgettingPolicy policy = getForgettingPolicy(memory);
            long daysUntilForgetting = estimateDaysUntilForgetting(memory);
            
            return new MemoryDecayInfo(decayScore, retentionScore, shouldForget, 
                                     policy, daysUntilForgetting, calculateDecayFactors(memory));
        });
    }
    
    public CompletableFuture<Void> reinforceMemory(EnhancedMemory memory) {
        return CompletableFuture.runAsync(() -> {
            logger.debug("Reinforcing memory: {}", memory.getId());
            
            memory.recordAccess();
            
            // Increase confidence score
            double currentConfidence = memory.getConfidenceScore();
            double reinforcementBoost = 0.1;
            memory.setConfidenceScore(Math.min(1.0, currentConfidence + reinforcementBoost));
            
            // Update importance if accessed frequently
            if (memory.getAccessCount() > 10) {
                MemoryImportance currentImportance = memory.getImportance();
                if (currentImportance.getScore() < MemoryImportance.HIGH.getScore()) {
                    memory.setImportance(MemoryImportance.HIGH);
                }
            }
            
            // Add reinforcement metadata
            memory.getMetadata().put("last_reinforced", LocalDateTime.now().toString());
            memory.getMetadata().put("reinforcement_count", 
                (Integer) memory.getMetadata().getOrDefault("reinforcement_count", 0) + 1);
        });
    }
    
    public CompletableFuture<List<EnhancedMemory>> pruneOldMemories(List<EnhancedMemory> memories, 
                                                                   int maxMemories,
                                                                   PruningStrategy strategy) {
        logger.debug("Pruning {} memories to max {} using strategy {}", 
            memories.size(), maxMemories, strategy);
        
        return CompletableFuture.supplyAsync(() -> {
            if (memories.size() <= maxMemories) {
                return new ArrayList<>(memories);
            }
            
            switch (strategy) {
                case LEAST_RECENTLY_USED:
                    return pruneLRU(memories, maxMemories);
                case LEAST_IMPORTANT:
                    return pruneLeastImportant(memories, maxMemories);
                case OLDEST_FIRST:
                    return pruneOldest(memories, maxMemories);
                case LOWEST_DECAY_SCORE:
                    return pruneLowestDecay(memories, maxMemories);
                case BALANCED:
                    return pruneBalanced(memories, maxMemories);
                default:
                    return pruneBalanced(memories, maxMemories);
            }
        });
    }
    
    private boolean shouldForgetMemory(EnhancedMemory memory) {
        // Check custom policy first
        if (customPolicy != null) {
            // If forgetting is disabled, never forget
            if (!customPolicy.isForgettingEnabled()) {
                return false;
            }
            
            // Check importance threshold
            if (memory.getImportance().ordinal() >= customPolicy.getImportanceThreshold().ordinal()) {
                return false;
            }
            
            // Check access count
            if (memory.getAccessCount() >= customPolicy.getMinAccessCount()) {
                return false;
            }
            
            // Check age
            if (memory.getDaysOld() <= customPolicy.getMaxMemoryAge()) {
                return false;
            }
        }
        
        // Never forget critical memories
        if (memory.getImportance() == MemoryImportance.CRITICAL) {
            return false;
        }
        
        // Never forget consolidated memories
        if (memory.isConsolidated()) {
            return false;
        }
        
        // Already deprecated
        if (memory.isDeprecated()) {
            return true;
        }
        
        // Expired memories should be forgotten
        if (memory.isExpired()) {
            return true;
        }
        
        // Calculate decay score
        double decayScore = calculateDecayScore(memory);
        
        // Apply forgetting policy
        ForgettingPolicy policy = getForgettingPolicy(memory);
        return shouldForgetBasedOnPolicy(memory, decayScore, policy);
    }
    
    private double calculateDecayScore(EnhancedMemory memory) {
        long daysSinceLastAccess = memory.getDaysSinceLastAccess();
        long daysOld = memory.getDaysOld();
        
        // Ebbinghaus forgetting curve: R(t) = e^(-t/S)
        // where R(t) is retention, t is time, S is memory strength
        double memoryStrength = calculateMemoryStrength(memory);
        double timeDecay = Math.exp(-daysSinceLastAccess / memoryStrength);
        double ageDecay = Math.exp(-daysOld / (memoryStrength * 2));
        
        // Combine time and age decay
        double combinedDecay = (timeDecay + ageDecay) / 2.0;
        
        // Apply forgetting curve
        double decayScore = forgettingCurveBase * Math.exp(-combinedDecay / forgettingCurveDecay);
        
        return Math.min(1.0, 1.0 - decayScore); // Higher decay score means more likely to forget, capped at 1.0
    }
    
    private double calculateRetentionScore(EnhancedMemory memory) {
        double importanceScore = memory.getImportance().getScore() / 5.0;
        double accessScore = Math.log(memory.getAccessCount() + 1) / 10.0;
        double confidenceScore = memory.getConfidenceScore();
        double consolidationBonus = memory.isConsolidated() ? 0.3 : 0.0;
        
        // Recency boost
        long daysSinceAccess = memory.getDaysSinceLastAccess();
        double recencyScore = Math.exp(-daysSinceAccess / 30.0) / 5.0;
        
        return Math.min(1.0, importanceScore + accessScore + confidenceScore + 
                            consolidationBonus + recencyScore);
    }
    
    private double calculateMemoryStrength(EnhancedMemory memory) {
        // Base strength from importance
        double strength = memory.getImportance().getScore();
        
        // Boost from access frequency
        strength += Math.log(memory.getAccessCount() + 1);
        
        // Boost from confidence
        strength += memory.getConfidenceScore() * 2;
        
        // Boost from relationships
        strength += Math.min(2.0, memory.getRelatedMemoryIds().size() * 0.2);
        
        // Consolidation boost
        if (memory.isConsolidated()) {
            strength += 3.0;
        }
        
        return Math.max(1.0, strength);
    }
    
    private ForgettingPolicy getForgettingPolicy(EnhancedMemory memory) {
        return typePolicies.getOrDefault(memory.getType(), defaultPolicy);
    }
    
    private boolean shouldForgetBasedOnPolicy(EnhancedMemory memory, double decayScore, 
                                            ForgettingPolicy policy) {
        double threshold = memoryRetentionThreshold;
        
        switch (policy) {
            case NEVER_FORGET:
                return false;
                
            case AGGRESSIVE_FORGETTING:
                threshold = 0.4; // Lower threshold, more aggressive
                break;
                
            case CONSERVATIVE_FORGETTING:
                threshold = 0.1; // Higher threshold, more conservative
                break;
                
            case GRADUAL_DECAY:
                // Use default threshold
                break;
                
            case IMPORTANCE_BASED:
                // Adjust threshold based on importance
                threshold = threshold * (6 - memory.getImportance().getScore()) / 5.0;
                break;
                
            case ACCESS_BASED:
                // Adjust threshold based on access patterns
                if (memory.getAccessCount() > 5) {
                    threshold = threshold / 2.0; // Harder to forget
                }
                break;
        }
        
        return decayScore > threshold;
    }
    
    private long estimateDaysUntilForgetting(EnhancedMemory memory) {
        if (memory.getImportance() == MemoryImportance.CRITICAL || memory.isConsolidated()) {
            return -1; // Never forget
        }
        
        double currentDecayScore = calculateDecayScore(memory);
        double memoryStrength = calculateMemoryStrength(memory);
        
        // Estimate based on forgetting curve
        double targetDecay = memoryRetentionThreshold;
        
        if (currentDecayScore >= targetDecay) {
            return 0; // Already ready for forgetting
        }
        
        // Calculate time to reach threshold
        double timeToForget = -memoryStrength * Math.log(targetDecay / forgettingCurveBase);
        return Math.max(1, (long) timeToForget);
    }
    
    private void updateMemoryStrength(EnhancedMemory memory) {
        double currentConfidence = memory.getConfidenceScore();
        double decayScore = calculateDecayScore(memory);
        
        // Reduce confidence based on decay
        double newConfidence = currentConfidence * (1.0 - decayScore * 0.1);
        memory.setConfidenceScore(Math.max(0.1, newConfidence));
        
        // Update metadata
        memory.getMetadata().put("last_decay_update", LocalDateTime.now().toString());
        memory.getMetadata().put("decay_score", decayScore);
    }
    
    private Map<String, Double> calculateDecayFactors(EnhancedMemory memory) {
        Map<String, Double> factors = new HashMap<>();
        
        factors.put("age_factor", (double) memory.getDaysOld());
        factors.put("access_recency", (double) memory.getDaysSinceLastAccess());
        factors.put("access_frequency", (double) memory.getAccessCount());
        factors.put("importance_score", (double) memory.getImportance().getScore());
        factors.put("confidence_score", memory.getConfidenceScore());
        factors.put("relationship_count", (double) memory.getRelatedMemoryIds().size());
        factors.put("is_consolidated", memory.isConsolidated() ? 1.0 : 0.0);
        
        return factors;
    }
    
    // Pruning strategies
    private List<EnhancedMemory> pruneLRU(List<EnhancedMemory> memories, int maxMemories) {
        return memories.stream()
            .sorted((m1, m2) -> m2.getLastAccessedAt().compareTo(m1.getLastAccessedAt()))
            .limit(maxMemories)
            .collect(Collectors.toList());
    }
    
    private List<EnhancedMemory> pruneLeastImportant(List<EnhancedMemory> memories, int maxMemories) {
        return memories.stream()
            .sorted((m1, m2) -> Integer.compare(m2.getImportance().getScore(), m1.getImportance().getScore()))
            .limit(maxMemories)
            .collect(Collectors.toList());
    }
    
    private List<EnhancedMemory> pruneOldest(List<EnhancedMemory> memories, int maxMemories) {
        return memories.stream()
            .sorted((m1, m2) -> m2.getCreatedAt().compareTo(m1.getCreatedAt()))
            .limit(maxMemories)
            .collect(Collectors.toList());
    }
    
    private List<EnhancedMemory> pruneLowestDecay(List<EnhancedMemory> memories, int maxMemories) {
        return memories.stream()
            .sorted((m1, m2) -> Double.compare(calculateDecayScore(m1), calculateDecayScore(m2)))
            .limit(maxMemories)
            .collect(Collectors.toList());
    }
    
    private List<EnhancedMemory> pruneBalanced(List<EnhancedMemory> memories, int maxMemories) {
        // Balanced strategy considers multiple factors
        return memories.stream()
            .sorted((m1, m2) -> {
                double score1 = calculateBalancedScore(m1);
                double score2 = calculateBalancedScore(m2);
                return Double.compare(score2, score1);
            })
            .limit(maxMemories)
            .collect(Collectors.toList());
    }
    
    private double calculateBalancedScore(EnhancedMemory memory) {
        double importanceWeight = 0.3;
        double recencyWeight = 0.2;
        double accessWeight = 0.2;
        double decayWeight = -0.3; // Negative because lower decay is better
        
        double importanceScore = memory.getImportance().getScore() / 5.0;
        double recencyScore = 1.0 / (memory.getDaysSinceLastAccess() + 1);
        double accessScore = Math.log(memory.getAccessCount() + 1) / 10.0;
        double decayScore = calculateDecayScore(memory);
        
        return importanceWeight * importanceScore +
               recencyWeight * recencyScore +
               accessWeight * accessScore +
               decayWeight * decayScore;
    }
    
    private Map<MemoryType, ForgettingPolicy> initializeTypePolicies() {
        Map<MemoryType, ForgettingPolicy> policies = new HashMap<>();
        
        policies.put(MemoryType.PROCEDURAL, ForgettingPolicy.CONSERVATIVE_FORGETTING);
        policies.put(MemoryType.FACTUAL, ForgettingPolicy.IMPORTANCE_BASED);
        policies.put(MemoryType.SEMANTIC, ForgettingPolicy.GRADUAL_DECAY);
        policies.put(MemoryType.EPISODIC, ForgettingPolicy.ACCESS_BASED);
        policies.put(MemoryType.PREFERENCE, ForgettingPolicy.CONSERVATIVE_FORGETTING);
        policies.put(MemoryType.TEMPORAL, ForgettingPolicy.AGGRESSIVE_FORGETTING);
        policies.put(MemoryType.CONTEXTUAL, ForgettingPolicy.GRADUAL_DECAY);
        policies.put(MemoryType.RELATIONSHIP, ForgettingPolicy.CONSERVATIVE_FORGETTING);
        
        return policies;
    }
    
    // Enums and inner classes
    public enum ForgettingPolicy {
        NEVER_FORGET,
        GRADUAL_DECAY,
        AGGRESSIVE_FORGETTING,
        CONSERVATIVE_FORGETTING,
        IMPORTANCE_BASED,
        ACCESS_BASED
    }
    
    public enum PruningStrategy {
        LEAST_RECENTLY_USED,
        LEAST_IMPORTANT,
        OLDEST_FIRST,
        LOWEST_DECAY_SCORE,
        BALANCED
    }
    
    public static class MemoryDecayInfo {
        private final double decayScore;
        private final double retentionScore;
        private final boolean shouldForget;
        private final ForgettingPolicy policy;
        private final long daysUntilForgetting;
        private final Map<String, Double> decayFactors;
        
        public MemoryDecayInfo(double decayScore, double retentionScore, boolean shouldForget,
                              ForgettingPolicy policy, long daysUntilForgetting,
                              Map<String, Double> decayFactors) {
            this.decayScore = decayScore;
            this.retentionScore = retentionScore;
            this.shouldForget = shouldForget;
            this.policy = policy;
            this.daysUntilForgetting = daysUntilForgetting;
            this.decayFactors = new HashMap<>(decayFactors);
        }
        
        public double getDecayScore() { return decayScore; }
        public double getRetentionScore() { return retentionScore; }
        public boolean shouldForget() { return shouldForget; }
        public ForgettingPolicy getPolicy() { return policy; }
        public long getDaysUntilForgetting() { return daysUntilForgetting; }
        public Map<String, Double> getDecayFactors() { return new HashMap<>(decayFactors); }
        
        @Override
        public String toString() {
            return String.format("MemoryDecayInfo{decay=%.3f, retention=%.3f, shouldForget=%s, policy=%s, daysUntil=%d}", 
                decayScore, retentionScore, shouldForget, policy, daysUntilForgetting);
        }
    }
}