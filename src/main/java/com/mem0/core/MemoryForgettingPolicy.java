package com.mem0.core;

/**
 * 内存遗忘策略配置类 - Memory forgetting policy configuration class
 * 
 * <p>此类定义了内存遗忘管理器的各种策略参数，用于控制内存的自动清理和遗忘行为。
 * 基于认知心理学中的遗忘理论，提供了灵活的配置选项来平衡内存使用效率和信息保留需求。</p>
 * 
 * <p>This class defines various strategy parameters for the memory forgetting manager,
 * used to control automatic memory cleanup and forgetting behavior. Based on forgetting
 * theories in cognitive psychology, it provides flexible configuration options to balance
 * memory usage efficiency and information retention requirements.</p>
 * 
 * <h3>核心参数 / Core Parameters:</h3>
 * <ul>
 *   <li><strong>遗忘开关</strong> - 是否启用自动遗忘功能 / Forgetting switch - whether to enable automatic forgetting</li>
 *   <li><strong>衰减率</strong> - 内存重要性随时间的衰减速度 / Decay rate - speed of memory importance decay over time</li>
 *   <li><strong>重要性阈值</strong> - 低于此阈值的内存会被优先清理 / Importance threshold - memories below this are prioritized for cleanup</li>
 *   <li><strong>最大年龄</strong> - 内存在系统中的最大存活天数 / Max age - maximum days a memory can survive in the system</li>
 *   <li><strong>最小访问次数</strong> - 保留内存所需的最小访问次数 / Min access count - minimum access count required to retain memory</li>
 * </ul>
 * 
 * <h3>策略类型 / Strategy Types:</h3>
 * <ul>
 *   <li><strong>保守策略</strong> - 只清理非常旧且不重要的内存 / Conservative - only clean very old and unimportant memories</li>
 *   <li><strong>平衡策略</strong> - 在保留重要信息和控制内存使用之间平衡 / Balanced - balance between retention and memory control</li>
 *   <li><strong>激进策略</strong> - 积极清理以保持系统性能 / Aggressive - actively clean to maintain system performance</li>
 * </ul>
 * 
 * <p>使用示例 / Usage Example:</p>
 * <pre>{@code
 * // 创建保守的遗忘策略
 * MemoryForgettingPolicy conservative = new MemoryForgettingPolicy();
 * conservative.setDecayRate(0.05); // 较慢的衰减
 * conservative.setImportanceThreshold(MemoryImportance.LOW);
 * conservative.setMaxMemoryAge(180); // 6个月
 * 
 * // 创建激进的遗忘策略
 * MemoryForgettingPolicy aggressive = new MemoryForgettingPolicy(
 *     true, 0.2, MemoryImportance.MEDIUM, 30, 3
 * );
 * 
 * // 应用策略到遗忘管理器
 * MemoryForgettingManager manager = new MemoryForgettingManager();
 * manager.setPolicy(conservative);
 * }</pre>
 * 
 * <h3>配置建议 / Configuration Recommendations:</h3>
 * <ul>
 *   <li><strong>生产环境</strong> - 使用保守策略，确保重要信息不会意外丢失</li>
 *   <li><strong>开发环境</strong> - 使用平衡策略，在功能测试和性能之间平衡</li>
 *   <li><strong>高负载环境</strong> - 使用激进策略，优先保证系统响应性能</li>
 * </ul>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 * @see MemoryForgettingManager
 * @see MemoryImportance
 * @see EnhancedMemory
 */
public class MemoryForgettingPolicy {
    private boolean forgettingEnabled = true;
    private double decayRate = 0.1;
    private MemoryImportance importanceThreshold = MemoryImportance.LOW;
    private int maxMemoryAge = 90; // days
    private int minAccessCount = 1;
    
    public MemoryForgettingPolicy() {}
    
    public MemoryForgettingPolicy(boolean forgettingEnabled, double decayRate, 
                                MemoryImportance importanceThreshold, int maxMemoryAge, int minAccessCount) {
        this.forgettingEnabled = forgettingEnabled;
        this.decayRate = decayRate;
        this.importanceThreshold = importanceThreshold;
        this.maxMemoryAge = maxMemoryAge;
        this.minAccessCount = minAccessCount;
    }
    
    public boolean isForgettingEnabled() {
        return forgettingEnabled;
    }
    
    public void setForgettingEnabled(boolean forgettingEnabled) {
        this.forgettingEnabled = forgettingEnabled;
    }
    
    public double getDecayRate() {
        return decayRate;
    }
    
    public void setDecayRate(double decayRate) {
        this.decayRate = decayRate;
    }
    
    public MemoryImportance getImportanceThreshold() {
        return importanceThreshold;
    }
    
    public void setImportanceThreshold(MemoryImportance importanceThreshold) {
        this.importanceThreshold = importanceThreshold;
    }
    
    public int getMaxMemoryAge() {
        return maxMemoryAge;
    }
    
    public void setMaxMemoryAge(int maxMemoryAge) {
        this.maxMemoryAge = maxMemoryAge;
    }
    
    public int getMinAccessCount() {
        return minAccessCount;
    }
    
    public void setMinAccessCount(int minAccessCount) {
        this.minAccessCount = minAccessCount;
    }
}