package com.mem0.core;

/**
 * 内存类型枚举，用于分类不同类型的内存内容
 * Memory type enumeration for classifying different types of memory content
 * 
 * <p>内存类型基于认知心理学理论，将内存分为不同类别，每种类型具有不同的特性和生命周期</p>
 * <p>Memory types are based on cognitive psychology theories, categorizing memory into different types with distinct characteristics and lifecycles</p>
 * 
 * <h3>内存类型分类 / Memory Type Classifications:</h3>
 * <ul>
 *   <li><strong>语义记忆 (SEMANTIC)</strong> - 事实知识和概念 / Factual knowledge and concepts</li>
 *   <li><strong>情节记忆 (EPISODIC)</strong> - 个人经历和事件 / Personal experiences and events</li>
 *   <li><strong>程序记忆 (PROCEDURAL)</strong> - 技能和操作方法 / Skills and procedures</li>
 *   <li><strong>事实记忆 (FACTUAL)</strong> - 具体事实和数据 / Concrete facts and data</li>
 *   <li><strong>上下文记忆 (CONTEXTUAL)</strong> - 情境信息 / Situational information</li>
 *   <li><strong>偏好记忆 (PREFERENCE)</strong> - 用户偏好 / User preferences</li>
 *   <li><strong>关系记忆 (RELATIONSHIP)</strong> - 人际关系 / Interpersonal relationships</li>
 *   <li><strong>时间记忆 (TEMPORAL)</strong> - 时间相关信息 / Time-based information</li>
 * </ul>
 * 
 * <p>使用示例 / Usage example:</p>
 * <pre>{@code
 * // 根据内容确定内存类型
 * MemoryType userPref = MemoryType.PREFERENCE;
 * MemoryType factInfo = MemoryType.FACTUAL;
 * 
 * // 检查内存特性
 * if (userPref.isPersonal()) {
 *     System.out.println("这是个人化的内存类型");
 * }
 * 
 * // 从字符串创建类型
 * MemoryType type = MemoryType.fromValue("semantic");
 * }</pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public enum MemoryType {
    
    /** 语义记忆：事实知识和一般概念 / Semantic memory: Factual knowledge and general concepts */
    SEMANTIC("semantic", "Factual knowledge and general concepts about the world"),
    
    /** 情节记忆：个人经历和特定事件 / Episodic memory: Personal experiences and specific events */
    EPISODIC("episodic", "Personal experiences and specific events with temporal context"),
    
    /** 程序记忆：技能、习惯和执行任务的方法 / Procedural memory: Skills, habits and know-how */
    PROCEDURAL("procedural", "Skills, habits and know-how for performing tasks"),
    
    /** 事实记忆：具体的事实和数据点 / Factual memory: Concrete facts and data points */
    FACTUAL("factual", "Concrete facts and data points"),
    
    /** 上下文记忆：情境信息和环境上下文 / Contextual memory: Situational and environmental context */
    CONTEXTUAL("contextual", "Situational information and environmental context"),
    
    /** 偏好记忆：用户偏好、喜好和选择 / Preference memory: User preferences, likes and choices */
    PREFERENCE("preference", "User preferences, likes, dislikes and choices"),
    
    /** 关系记忆：社会关系和人际连接 / Relationship memory: Social relationships and connections */
    RELATIONSHIP("relationship", "Social relationships and interpersonal connections"),
    
    /** 时间记忆：基于时间的信息和时间表 / Temporal memory: Time-based information and schedules */
    TEMPORAL("temporal", "Time-based information and schedules");
    
    /** 内存类型的字符串值 / String value of the memory type */
    private final String value;
    
    /** 内存类型的描述 / Description of the memory type */
    private final String description;
    
    /**
     * 构造内存类型枚举值 / Constructor for memory type enum value
     * 
     * @param value 字符串值 / String value
     * @param description 描述信息 / Description
     */
    MemoryType(String value, String description) {
        this.value = value;
        this.description = description;
    }
    
    /**
     * 获取内存类型的字符串值 / Get string value of memory type
     * 
     * @return 字符串值 / String value
     */
    public String getValue() {
        return value;
    }
    
    /**
     * 获取内存类型的描述 / Get description of memory type
     * 
     * @return 描述信息 / Description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据字符串值创建内存类型 / Create memory type from string value
     * 
     * @param value 字符串值 / String value
     * @return 对应的内存类型，默认返回SEMANTIC / Corresponding memory type, defaults to SEMANTIC
     */
    public static MemoryType fromValue(String value) {
        if (value == null || value.isEmpty()) {
            return SEMANTIC; // Default type
        }
        
        for (MemoryType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        
        return SEMANTIC; // Fallback to default
    }
    
    /**
     * 检查是否为长期记忆类型 / Check if this is a long-term memory type
     * 
     * @return true表示长期记忆 / true if long-term memory
     */
    public boolean isLongTerm() {
        return this == SEMANTIC || this == EPISODIC || this == PROCEDURAL || this == FACTUAL;
    }
    
    /**
     * 检查是否为短期记忆类型 / Check if this is a short-term memory type
     * 
     * @return true表示短期记忆 / true if short-term memory
     */
    public boolean isShortTerm() {
        return this == CONTEXTUAL || this == TEMPORAL;
    }
    
    /**
     * 检查是否为个人化记忆类型 / Check if this is a personal memory type
     * 
     * @return true表示个人化记忆 / true if personal memory
     */
    public boolean isPersonal() {
        return this == PREFERENCE || this == RELATIONSHIP || this == EPISODIC;
    }
}