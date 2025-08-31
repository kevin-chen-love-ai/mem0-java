package com.mem0.core;

/**
 * 内存冲突信息类 - Memory conflict information class
 * 
 * <p>此类表示内存系统中检测到的冲突情况，包含冲突的两个内存对象、
 * 冲突类型、严重程度以及详细描述信息。它是内存冲突检测和解决
 * 系统的核心数据结构，为冲突分析和处理提供完整的上下文信息。</p>
 * 
 * <p>This class represents detected conflict situations in the memory system,
 * containing the two conflicting memory objects, conflict type, severity level,
 * and detailed description information. It serves as the core data structure
 * for memory conflict detection and resolution systems, providing complete
 * contextual information for conflict analysis and processing.</p>
 * 
 * <h3>冲突信息组成 / Conflict Information Components:</h3>
 * <ul>
 *   <li><strong>新内存对象</strong> - 触发冲突的新内存条目 / New memory object that triggers the conflict</li>
 *   <li><strong>现有内存对象</strong> - 与新内存冲突的已存在内存 / Existing memory object that conflicts with the new memory</li>
 *   <li><strong>冲突类型</strong> - 冲突的分类和性质 / Conflict classification and nature</li>
 *   <li><strong>严重程度</strong> - 冲突影响的量化评估 (0.0-1.0) / Quantified assessment of conflict impact (0.0-1.0)</li>
 *   <li><strong>描述信息</strong> - 冲突的详细文字说明 / Detailed textual description of the conflict</li>
 * </ul>
 * 
 * <h3>冲突检测场景 / Conflict Detection Scenarios:</h3>
 * <ul>
 *   <li><strong>语义冲突</strong> - 相同主题的矛盾信息 / Semantic conflicts - contradictory information on the same topic</li>
 *   <li><strong>事实冲突</strong> - 不一致的事实陈述 / Factual conflicts - inconsistent factual statements</li>
 *   <li><strong>偏好冲突</strong> - 相互矛盾的用户偏好 / Preference conflicts - contradictory user preferences</li>
 *   <li><strong>时间冲突</strong> - 时间逻辑上的不一致 / Temporal conflicts - temporal logic inconsistencies</li>
 *   <li><strong>重复冲突</strong> - 内容高度相似的重复信息 / Duplication conflicts - highly similar duplicate information</li>
 * </ul>
 * 
 * <h3>使用示例 / Usage Examples:</h3>
 * <pre>{@code
 * // 创建内存冲突对象
 * EnhancedMemory newMemory = createNewMemory("用户喜欢茶");
 * EnhancedMemory existingMemory = getExistingMemory("用户喜欢咖啡");
 * 
 * MemoryConflict conflict = new MemoryConflict(
 *     newMemory,
 *     existingMemory, 
 *     ConflictType.PREFERENCE_CONTRADICTION,
 *     0.85  // 高严重程度
 * );
 * 
 * conflict.setDescription("用户偏好冲突：新的偏好声明与现有偏好直接矛盾");
 * 
 * // 分析冲突信息
 * System.out.println("冲突类型: " + conflict.getType());
 * System.out.println("冲突严重程度: " + conflict.getSeverity());
 * System.out.println("新内存内容: " + conflict.getNewMemory().getContent());
 * System.out.println("现有内存内容: " + conflict.getExistingMemory().getContent());
 * System.out.println("冲突描述: " + conflict.getDescription());
 * 
 * // 根据严重程度进行处理
 * if (conflict.getSeverity() > 0.7) {
 *     System.out.println("高严重度冲突，需要人工干预或智能解决");
 *     handleHighSeverityConflict(conflict);
 * } else if (conflict.getSeverity() > 0.4) {
 *     System.out.println("中等严重度冲突，自动解决");
 *     autoResolveConflict(conflict);
 * } else {
 *     System.out.println("低严重度冲突，记录但暂不处理");
 *     logConflictForLaterReview(conflict);
 * }
 * 
 * // 冲突解决前的验证
 * ConflictValidator validator = new ConflictValidator();
 * if (validator.isValidConflict(conflict)) {
 *     // 执行冲突解决流程
 *     ConflictResolver resolver = new ConflictResolver();
 *     ConflictResolution resolution = resolver.resolve(conflict);
 *     
 *     System.out.println("冲突解决策略: " + resolution.getStrategy());
 *     System.out.println("解决结果: " + resolution.getReasoning());
 * }
 * 
 * // 冲突统计和分析
 * ConflictAnalyzer analyzer = new ConflictAnalyzer();
 * ConflictStatistics stats = analyzer.analyzeConflict(conflict);
 * System.out.println("涉及的内存类型: " + stats.getInvolvedTypes());
 * System.out.println("冲突频率: " + stats.getFrequencyScore());
 * }</pre>
 * 
 * <h3>严重程度评估 / Severity Assessment:</h3>
 * <ul>
 *   <li><strong>0.0 - 0.3</strong> - 低严重度：轻微不一致，可延后处理 / Low severity: Minor inconsistency, can be deferred</li>
 *   <li><strong>0.3 - 0.7</strong> - 中严重度：明显冲突，需要自动解决 / Medium severity: Obvious conflict, requires automatic resolution</li>
 *   <li><strong>0.7 - 1.0</strong> - 高严重度：严重矛盾，需要优先处理 / High severity: Serious contradiction, requires priority handling</li>
 * </ul>
 * 
 * <h3>冲突生命周期 / Conflict Lifecycle:</h3>
 * <ul>
 *   <li><strong>检测阶段</strong> - 自动识别内存间的潜在冲突 / Detection phase - automatic identification of potential conflicts</li>
 *   <li><strong>评估阶段</strong> - 分析冲突的类型和严重程度 / Assessment phase - analyzing conflict type and severity</li>
 *   <li><strong>决策阶段</strong> - 选择合适的解决策略 / Decision phase - selecting appropriate resolution strategy</li>
 *   <li><strong>解决阶段</strong> - 执行冲突解决并生成结果 / Resolution phase - executing conflict resolution and generating results</li>
 *   <li><strong>验证阶段</strong> - 验证解决方案的有效性 / Validation phase - verifying effectiveness of resolution</li>
 * </ul>
 * 
 * <h3>集成特性 / Integration Features:</h3>
 * <ul>
 *   <li><strong>标准化接口</strong> - 提供统一的冲突信息访问接口 / Standardized interface for unified conflict information access</li>
 *   <li><strong>可扩展描述</strong> - 支持动态设置详细的冲突描述信息 / Extensible description support for detailed conflict information</li>
 *   <li><strong>调试友好</strong> - toString方法提供清晰的调试信息 / Debug-friendly toString method with clear debugging information</li>
 *   <li><strong>系统兼容</strong> - 与现有内存管理和冲突解决系统完全兼容 / System compatibility with existing memory management and conflict resolution systems</li>
 * </ul>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 * @see EnhancedMemory
 * @see ConflictType
 * @see ConflictResolution
 * @see MemoryConflictDetector
 */
public class MemoryConflict {
    private final EnhancedMemory newMemory;
    private final EnhancedMemory existingMemory;
    private final ConflictType type;
    private final double severity;
    private String description;
    
    public MemoryConflict(EnhancedMemory newMemory, EnhancedMemory existingMemory, 
                         ConflictType type, double severity) {
        this.newMemory = newMemory;
        this.existingMemory = existingMemory;
        this.type = type;
        this.severity = severity;
    }
    
    public EnhancedMemory getNewMemory() {
        return newMemory;
    }
    
    public EnhancedMemory getExistingMemory() {
        return existingMemory;
    }
    
    public ConflictType getType() {
        return type;
    }
    
    public double getSeverity() {
        return severity;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return String.format("MemoryConflict{type=%s, severity=%.2f, new=%s, existing=%s}", 
                           type, severity, newMemory.getId(), existingMemory.getId());
    }
}