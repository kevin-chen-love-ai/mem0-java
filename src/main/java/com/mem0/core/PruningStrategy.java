package com.mem0.core;

/**
 * 内存剪枝策略枚举 / Memory Pruning Strategy Enumeration
 * 
 * 定义内存系统中用于内存清理和容量管理的各种剪枝策略。
 * 每种策略基于不同的优化目标和评估维度，支持灵活的内存管理和空间优化。
 * Defines various pruning strategies used for memory cleanup and capacity management in the memory system.
 * Each strategy is based on different optimization goals and evaluation dimensions, supporting flexible
 * memory management and space optimization.
 * 
 * <h3>剪枝策略分类 / Pruning Strategy Classification:</h3>
 * <ul>
 *   <li><b>LRU (最近最少使用)</b>: 基于访问时间的经典缓存淘汰策略 / Classic cache eviction based on access time</li>
 *   <li><b>LEAST_IMPORTANT (最低重要性)</b>: 基于内存重要性评分的淘汰策略 / Eviction based on memory importance scores</li>
 *   <li><b>OLDEST_FIRST (最旧优先)</b>: 基于创建时间的先进先出策略 / FIFO strategy based on creation time</li>
 *   <li><b>LOWEST_DECAY_SCORE (最低衰减分数)</b>: 基于记忆衰减模型的淘汰策略 / Eviction based on memory decay model</li>
 *   <li><b>BALANCED (平衡策略)</b>: 综合多种因素的平衡淘汰策略 / Balanced eviction considering multiple factors</li>
 * </ul>
 * 
 * <h3>策略评估维度 / Strategy Evaluation Dimensions:</h3>
 * <pre>
 * 评估维度矩阵 / Evaluation Dimension Matrix:
 * 
 * 策略类型          访问频率  重要性  时间因子  衰减模型  综合平衡
 * LRU              ████     ░░░░    ██░░     ░░░░     ██░░
 * LEAST_IMPORTANT  ░░░░     ████    ░░░░     ░░░░     ██░░
 * OLDEST_FIRST     ░░░░     ░░░░    ████     ░░░░     ██░░
 * LOWEST_DECAY     ██░░     ██░░    ██░░     ████     ██░░
 * BALANCED         ██░░     ██░░    ██░░     ██░░     ████
 * </pre>
 * 
 * <h3>使用示例 / Usage Examples:</h3>
 * <pre>{@code
 * // 选择剪枝策略
 * PruningStrategy strategy = PruningStrategy.BALANCED;
 * 
 * // 根据策略执行剪枝
 * List<EnhancedMemory> memories = getMemoryList();
 * int targetSize = 1000;
 * 
 * switch (strategy) {
 *     case LRU:
 *         System.out.println("使用LRU策略剪枝");
 *         memories = pruneLRU(memories, targetSize);
 *         break;
 *     case LEAST_IMPORTANT:
 *         System.out.println("使用重要性策略剪枝");
 *         memories = pruneLeastImportant(memories, targetSize);
 *         break;
 *     case BALANCED:
 *         System.out.println("使用平衡策略剪枝");
 *         memories = pruneBalanced(memories, targetSize);
 *         break;
 *     default:
 *         System.out.println("默认策略: " + strategy.getValue());
 * }
 * 
 * // 策略性能分析
 * String strategyValue = PruningStrategy.LOWEST_DECAY_SCORE.getValue();
 * System.out.println("策略标识: " + strategyValue); // 输出: lowest_decay_score
 * 
 * // 策略适用场景判断
 * PruningStrategy optimalStrategy = selectOptimalStrategy(
 *     memoryCount, availableMemory, accessPattern, importanceDistribution
 * );
 * 
 * if (optimalStrategy == PruningStrategy.LRU) {
 *     System.out.println("适合高频访问场景");
 * } else if (optimalStrategy == PruningStrategy.LEAST_IMPORTANT) {
 *     System.out.println("适合重要性区分明显的场景");
 * } else if (optimalStrategy == PruningStrategy.BALANCED) {
 *     System.out.println("适合复杂业务场景");
 * }
 * }</pre>
 * 
 * <h3>策略特性对比 / Strategy Characteristics Comparison:</h3>
 * <ul>
 *   <li><b>LRU</b>: 实现简单，适用于访问模式明显的场景 / Simple implementation, suitable for clear access patterns</li>
 *   <li><b>LEAST_IMPORTANT</b>: 保留高价值内存，适用于重要性驱动的应用 / Preserves high-value memories, suitable for importance-driven applications</li>
 *   <li><b>OLDEST_FIRST</b>: 保证时间公平性，适用于时序敏感的场景 / Ensures temporal fairness, suitable for time-sensitive scenarios</li>
 *   <li><b>LOWEST_DECAY_SCORE</b>: 基于科学记忆模型，适用于智能学习系统 / Based on scientific memory model, suitable for intelligent learning systems</li>
 *   <li><b>BALANCED</b>: 综合考虑多因子，适用于复杂业务环境 / Considers multiple factors, suitable for complex business environments</li>
 * </ul>
 * 
 * <h3>性能指标 / Performance Metrics:</h3>
 * <ul>
 *   <li><b>剪枝效率</b>: 算法执行时间和计算复杂度 / Pruning efficiency: algorithm execution time and computational complexity</li>
 *   <li><b>内存保留质量</b>: 被保留内存的综合价值评分 / Memory retention quality: comprehensive value score of retained memories</li>
 *   <li><b>访问命中率</b>: 剪枝后的内存访问命中率 / Access hit rate: memory access hit rate after pruning</li>
 *   <li><b>存储空间利用率</b>: 有效内存占用与总容量的比率 / Storage utilization: ratio of effective memory usage to total capacity</li>
 * </ul>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 * @see com.mem0.core.MemoryForgettingManager
 */
public enum PruningStrategy {
    LRU("lru"),
    LEAST_IMPORTANT("least_important"),
    OLDEST_FIRST("oldest_first"),
    LOWEST_DECAY_SCORE("lowest_decay_score"),
    BALANCED("balanced");
    
    private final String value;
    
    PruningStrategy(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
}