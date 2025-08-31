package com.mem0.core;

/**
 * 冲突解决策略枚举类 - Conflict resolution strategy enumeration class
 * 
 * <p>此枚举定义了处理内存冲突时可用的各种解决策略。当系统检测到内存之间存在
 * 矛盾或冲突时，可以根据具体需求选择合适的策略来解决冲突，确保内存系统的
 * 一致性和完整性。</p>
 * 
 * <p>This enumeration defines various resolution strategies available when handling
 * memory conflicts. When the system detects contradictions or conflicts between
 * memories, appropriate strategies can be chosen based on specific requirements
 * to resolve conflicts and ensure memory system consistency and integrity.</p>
 * 
 * <h3>策略类型 / Strategy Types:</h3>
 * <ul>
 *   <li><strong>保留第一个 (KEEP_FIRST)</strong> - 保留现有内存，丢弃新内存 / Keep existing memory, discard new memory</li>
 *   <li><strong>保留第二个 (KEEP_SECOND)</strong> - 丢弃现有内存，保留新内存 / Discard existing memory, keep new memory</li>
 *   <li><strong>合并 (MERGE)</strong> - 尝试合并两个内存的内容 / Attempt to merge content from both memories</li>
 *   <li><strong>同时保留 (KEEP_BOTH)</strong> - 保留两个内存，标记为相关 / Keep both memories, mark as related</li>
 *   <li><strong>同时删除 (DELETE_BOTH)</strong> - 删除两个内存，创建新的综合内存 / Delete both memories, create new synthesized memory</li>
 * </ul>
 * 
 * <h3>使用场景 / Usage Scenarios:</h3>
 * <ul>
 *   <li><strong>信息更新</strong> - 当新信息与旧信息冲突时的处理策略</li>
 *   <li><strong>数据清理</strong> - 清除重复或矛盾信息的自动化策略</li>
 *   <li><strong>知识整合</strong> - 整合来自不同源的信息时的冲突处理</li>
 *   <li><strong>版本管理</strong> - 处理同一信息的不同版本之间的冲突</li>
 * </ul>
 * 
 * <p>使用示例 / Usage Example:</p>
 * <pre>{@code
 * // 根据场景选择策略
 * ConflictResolutionStrategy strategy = ConflictResolutionStrategy.MERGE;
 * 
 * // 在冲突检测器中使用
 * MemoryConflictDetector detector = new MemoryConflictDetector();
 * detector.setDefaultStrategy(ConflictResolutionStrategy.KEEP_SECOND);
 * 
 * // 从字符串获取策略
 * String strategyName = "keep_first";
 * ConflictResolutionStrategy fromString = ConflictResolutionStrategy.valueOf(
 *     strategyName.toUpperCase().replace("_", "_")
 * );
 * }</pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 * @see MemoryConflictDetector
 * @see MemoryConflict
 * @see ConflictResolution
 */
public enum ConflictResolutionStrategy {
    KEEP_FIRST("keep_first"),
    KEEP_SECOND("keep_second"),
    MERGE("merge"),
    KEEP_BOTH("keep_both"),
    DELETE_BOTH("delete_both");
    
    private final String value;
    
    ConflictResolutionStrategy(String value) {
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