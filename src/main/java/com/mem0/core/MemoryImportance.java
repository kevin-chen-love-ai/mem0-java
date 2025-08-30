package com.mem0.core;

/**
 * 内存重要性枚举，用于评估和分类内存的重要程度
 * Memory importance enumeration for assessing and classifying the importance level of memories
 * 
 * <p>重要性等级影响内存的生命周期、访问优先级和遗忘策略</p>
 * <p>Importance levels affect memory lifecycle, access priority and forgetting policies</p>
 * 
 * <h3>重要性等级分类 / Importance Level Classifications:</h3>
 * <ul>
 *   <li><strong>关键 (CRITICAL)</strong> - 评分5，绝不能遗忘的关键信息 / Score 5, critical information that must never be forgotten</li>
 *   <li><strong>高 (HIGH)</strong> - 评分4，具有重大影响的高重要性信息 / Score 4, high importance with significant impact</li>
 *   <li><strong>中等 (MEDIUM)</strong> - 评分3，中等重要性信息 / Score 3, moderately important information</li>
 *   <li><strong>低 (LOW)</strong> - 评分2，可能随时间遗忘的低重要性信息 / Score 2, low importance that may be forgotten over time</li>
 *   <li><strong>最小 (MINIMAL)</strong> - 评分1，早期遗忘候选的最低重要性信息 / Score 1, minimal importance candidate for early forgetting</li>
 * </ul>
 * 
 * <p>使用示例 / Usage example:</p>
 * <pre>{@code
 * // 设置内存重要性
 * MemoryImportance critical = MemoryImportance.CRITICAL;
 * MemoryImportance fromScore = MemoryImportance.fromScore(4); // HIGH
 * 
 * // 检查优先级
 * if (critical.isHighPriority()) {
 *     System.out.println("高优先级内存，优先保留");
 * }
 * 
 * // 获取数值评分
 * int score = critical.getScore(); // 5
 * }</pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public enum MemoryImportance {
    
    /** 关键重要性：绝不能遗忘的关键信息 / Critical: Information that must not be forgotten */
    CRITICAL(5, "Critical information that must not be forgotten"),
    
    /** 高重要性：具有重大影响的信息 / High: Information with significant impact */
    HIGH(4, "High importance information with significant impact"), 
    
    /** 中等重要性：中等程度的重要信息 / Medium: Moderately important information */
    MEDIUM(3, "Moderately important information"),
    
    /** 低重要性：可能随时间遗忘的信息 / Low: Information that may be forgotten over time */
    LOW(2, "Low importance information that may be forgotten over time"),
    
    /** 最小重要性：早期遗忘候选信息 / Minimal: Candidate for early forgetting */
    MINIMAL(1, "Minimal importance information, candidate for early forgetting");
    
    /** 重要性数值评分（1-5） / Numerical importance score (1-5) */
    private final int score;
    
    /** 重要性等级描述 / Importance level description */
    private final String description;
    
    /**
     * 构造内存重要性枚举值 / Constructor for memory importance enum value
     * 
     * @param score 数值评分（1-5） / Numerical score (1-5)
     * @param description 描述信息 / Description
     */
    MemoryImportance(int score, String description) {
        this.score = score;
        this.description = description;
    }
    
    /**
     * 获取重要性数值评分 / Get numerical importance score
     * 
     * @return 1-5的评分值 / Score value from 1-5
     */
    public int getScore() {
        return score;
    }
    
    /**
     * 获取重要性等级描述 / Get importance level description
     * 
     * @return 描述字符串 / Description string
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据整数评分创建重要性等级 / Create importance level from integer score
     * 
     * @param score 1-5的评分值 / Score value from 1-5
     * @return 对应的重要性等级，默认返回MEDIUM / Corresponding importance level, defaults to MEDIUM
     */
    public static MemoryImportance fromScore(int score) {
        for (MemoryImportance importance : values()) {
            if (importance.score == score) {
                return importance;
            }
        }
        return MEDIUM; // Default fallback
    }
    
    /**
     * 根据浮点数评分创建重要性等级 / Create importance level from double score
     * 
     * @param score 浮点数评分，会被四舍五入为整数 / Double score, will be rounded to integer
     * @return 对应的重要性等级 / Corresponding importance level
     */
    public static MemoryImportance fromScore(double score) {
        return fromScore((int) Math.round(score));
    }
    
    /**
     * 检查是否为高优先级 / Check if this is high priority
     * 
     * @return true表示CRITICAL或HIGH / true if CRITICAL or HIGH
     */
    public boolean isHighPriority() {
        return this == CRITICAL || this == HIGH;
    }
    
    /**
     * 检查是否为低优先级 / Check if this is low priority
     * 
     * @return true表示LOW或MINIMAL / true if LOW or MINIMAL
     */
    public boolean isLowPriority() {
        return this == LOW || this == MINIMAL;
    }
}