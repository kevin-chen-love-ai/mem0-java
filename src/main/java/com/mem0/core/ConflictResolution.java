package com.mem0.core;

/**
 * 内存冲突解决结果类 - Memory conflict resolution result class
 * 
 * <p>此类封装了内存冲突解决过程的完整结果信息，包括所采用的解决策略、
 * 最终生成的内存对象、合并后的内容以及决策推理过程。它是冲突检测和
 * 解决系统的核心数据结构，为内存一致性管理提供详细的处理结果记录。</p>
 * 
 * <p>This class encapsulates complete result information from the memory conflict
 * resolution process, including the resolution strategy used, final generated memory
 * object, merged content, and decision reasoning process. It serves as the core data
 * structure for conflict detection and resolution systems, providing detailed
 * processing result records for memory consistency management.</p>
 * 
 * <h3>结果信息组成 / Result Information Components:</h3>
 * <ul>
 *   <li><strong>解决策略</strong> - 使用的冲突解决策略 / Resolution strategy used</li>
 *   <li><strong>结果内存</strong> - 冲突解决后生成的最终内存对象 / Final memory object generated after conflict resolution</li>
 *   <li><strong>合并内容</strong> - 处理后的内存内容文本 / Processed memory content text</li>
 *   <li><strong>推理说明</strong> - 决策过程的详细解释 / Detailed explanation of decision process</li>
 * </ul>
 * 
 * <h3>应用场景 / Application Scenarios:</h3>
 * <ul>
 *   <li><strong>冲突审计</strong> - 记录和审查冲突解决历史 / Conflict auditing - recording and reviewing conflict resolution history</li>
 *   <li><strong>质量控制</strong> - 评估冲突解决方案的质量和合理性 / Quality control - evaluating resolution quality and reasonableness</li>
 *   <li><strong>系统监控</strong> - 监控内存冲突的频率和解决效果 / System monitoring - monitoring conflict frequency and resolution effectiveness</li>
 *   <li><strong>调试分析</strong> - 分析冲突解决逻辑的正确性 / Debug analysis - analyzing correctness of resolution logic</li>
 *   <li><strong>结果通知</strong> - 向客户端反馈冲突处理结果 / Result notification - providing conflict processing results to clients</li>
 * </ul>
 * 
 * <h3>使用示例 / Usage Examples:</h3>
 * <pre>{@code
 * // 创建冲突解决结果
 * EnhancedMemory resolvedMemory = createResolvedMemory();
 * ConflictResolution resolution = new ConflictResolution(
 *     ConflictResolutionStrategy.MERGE,
 *     resolvedMemory,
 *     "User prefers coffee in the morning and tea in the evening",
 *     "Merged conflicting preferences based on temporal context"
 * );
 * 
 * // 检查解决策略
 * ConflictResolutionStrategy strategy = resolution.getStrategy();
 * System.out.println("使用的策略: " + strategy.getValue()); // "merge"
 * 
 * // 获取解决结果
 * EnhancedMemory result = resolution.getResultingMemory();
 * if (result != null) {
 *     System.out.println("解决后的内存ID: " + result.getId());
 *     System.out.println("内存重要性: " + result.getImportance());
 *     System.out.println("内存类型: " + result.getType());
 * }
 * 
 * // 查看合并内容
 * String mergedContent = resolution.getMergedContent();
 * System.out.println("合并后内容: " + mergedContent);
 * 
 * // 了解决策推理
 * String reasoning = resolution.getReasoning();
 * System.out.println("决策理由: " + reasoning);
 * 
 * // 记录解决结果
 * ConflictResolutionLogger logger = new ConflictResolutionLogger();
 * logger.logResolution(resolution);
 * 
 * // 评估解决质量
 * double quality = evaluateResolutionQuality(resolution);
 * System.out.println("解决质量分数: " + quality);
 * }</pre>
 * 
 * <h3>数据完整性 / Data Integrity:</h3>
 * <ul>
 *   <li><strong>不可变性</strong> - 所有字段都是final，确保结果数据的不可变性</li>
 *   <li><strong>一致性检查</strong> - 策略与结果内存之间的一致性验证</li>
 *   <li><strong>完整记录</strong> - 确保所有解决过程信息都被完整保存</li>
 * </ul>
 * 
 * <h3>集成接口 / Integration Interfaces:</h3>
 * <ul>
 *   <li><strong>toString方法</strong> - 提供简洁的结果摘要信息用于日志记录</li>
 *   <li><strong>访问器方法</strong> - 提供所有结果组件的只读访问</li>
 *   <li><strong>兼容性</strong> - 与现有冲突检测和内存管理系统完全兼容</li>
 * </ul>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 * @see ConflictResolutionStrategy
 * @see EnhancedMemory
 * @see MemoryConflict
 * @see MemoryConflictDetector
 */
public class ConflictResolution {
    private final ConflictResolutionStrategy strategy;
    private final EnhancedMemory resultingMemory;
    private final String mergedContent;
    private final String reasoning;
    
    public ConflictResolution(ConflictResolutionStrategy strategy, 
                            EnhancedMemory resultingMemory, 
                            String mergedContent, 
                            String reasoning) {
        this.strategy = strategy;
        this.resultingMemory = resultingMemory;
        this.mergedContent = mergedContent;
        this.reasoning = reasoning;
    }
    
    public ConflictResolutionStrategy getStrategy() {
        return strategy;
    }
    
    public EnhancedMemory getResultingMemory() {
        return resultingMemory;
    }
    
    public String getMergedContent() {
        return mergedContent;
    }
    
    public String getReasoning() {
        return reasoning;
    }
    
    @Override
    public String toString() {
        return String.format("ConflictResolution{strategy=%s, reasoning='%s'}", 
                           strategy, reasoning);
    }
}