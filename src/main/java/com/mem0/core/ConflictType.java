package com.mem0.core;

/**
 * 内存冲突类型枚举 / Memory Conflict Type Enumeration
 * 
 * 定义内存系统中可能出现的各种冲突类型，用于冲突检测、分析和解决策略选择。
 * 每种冲突类型对应不同的检测算法和解决方案，支持精确的冲突分类和处理。
 * Defines various types of conflicts that can occur in the memory system for conflict detection,
 * analysis, and resolution strategy selection. Each conflict type corresponds to different
 * detection algorithms and solutions, supporting precise conflict classification and handling.
 * 
 * <h3>冲突类型分类 / Conflict Type Classification:</h3>
 * <ul>
 *   <li><b>SEMANTIC (语义冲突)</b>: 内容语义层面的矛盾和不一致 / Semantic contradictions and inconsistencies</li>
 *   <li><b>FACTUAL (事实冲突)</b>: 客观事实信息的直接冲突 / Direct conflicts in factual information</li>
 *   <li><b>PREFERENCE (偏好冲突)</b>: 用户偏好和选择的不一致 / Inconsistencies in user preferences and choices</li>
 *   <li><b>TEMPORAL (时间冲突)</b>: 时间相关信息的矛盾 / Contradictions in time-related information</li>
 *   <li><b>CONTEXTUAL (上下文冲突)</b>: 上下文环境信息的冲突 / Conflicts in contextual environment information</li>
 *   <li><b>PROCEDURAL (程序冲突)</b>: 操作步骤和流程的不一致 / Inconsistencies in operational procedures and workflows</li>
 * </ul>
 * 
 * <h3>使用示例 / Usage Examples:</h3>
 * <pre>{@code
 * // 检测冲突类型
 * ConflictType conflictType = determineConflictType(memory1, memory2);
 * 
 * switch (conflictType) {
 *     case SEMANTIC:
 *         System.out.println("检测到语义冲突");
 *         handleSemanticConflict(memory1, memory2);
 *         break;
 *     case FACTUAL:
 *         System.out.println("检测到事实冲突");
 *         handleFactualConflict(memory1, memory2);
 *         break;
 *     case PREFERENCE:
 *         System.out.println("检测到偏好冲突");
 *         handlePreferenceConflict(memory1, memory2);
 *         break;
 *     default:
 *         System.out.println("未知冲突类型: " + conflictType.getValue());
 * }
 * 
 * // 获取冲突类型值
 * String conflictValue = ConflictType.TEMPORAL.getValue();
 * System.out.println("冲突类型值: " + conflictValue); // 输出: temporal
 * 
 * // 冲突严重性评估
 * int severity = getConflictSeverity(conflictType);
 * switch (conflictType) {
 *     case FACTUAL:
 *         severity = 5; // 最高优先级
 *         break;
 *     case SEMANTIC:
 *         severity = 4; // 高优先级
 *         break;
 *     case PREFERENCE:
 *         severity = 2; // 中等优先级
 *         break;
 *     default:
 *         severity = 1; // 低优先级
 * }
 * }</pre>
 * 
 * <h3>冲突检测特征 / Conflict Detection Characteristics:</h3>
 * <ul>
 *   <li><b>SEMANTIC</b>: 基于词汇语义分析和上下文理解 / Based on lexical semantic analysis and contextual understanding</li>
 *   <li><b>FACTUAL</b>: 基于客观数据和事实验证 / Based on objective data and fact verification</li>
 *   <li><b>PREFERENCE</b>: 基于用户行为模式和选择历史 / Based on user behavior patterns and choice history</li>
 *   <li><b>TEMPORAL</b>: 基于时间序列和时间戳分析 / Based on time series and timestamp analysis</li>
 *   <li><b>CONTEXTUAL</b>: 基于环境条件和上下文匹配 / Based on environmental conditions and context matching</li>
 *   <li><b>PROCEDURAL</b>: 基于操作流程和步骤序列 / Based on operational workflows and step sequences</li>
 * </ul>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 * @see com.mem0.core.MemoryConflictDetector
 */
public enum ConflictType {
    SEMANTIC("semantic"),
    FACTUAL("factual"),
    PREFERENCE("preference"),
    TEMPORAL("temporal"),
    CONTEXTUAL("contextual"),
    PROCEDURAL("procedural");
    
    private final String value;
    
    ConflictType(String value) {
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