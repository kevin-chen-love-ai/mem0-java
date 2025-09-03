package com.mem0.constants;

/**
 * Mem0内存系统常量定义 - Constants used throughout the Mem0 memory system
 * 
 * <p>此类定义了Mem0框架中使用的所有系统级常量，包括向量操作约束、文本处理限制、
 * 缓存配置、LLM参数和搜索相关的默认值。这些常量确保系统的一致性和可配置性。</p>
 * 
 * <p>This class defines all system-level constants used in the Mem0 framework, including
 * vector operation constraints, text processing limits, cache configuration, LLM parameters,
 * and search-related default values. These constants ensure system consistency and configurability.</p>
 * 
 * <h3>常量分类 / Constant Categories:</h3>
 * <ul>
 *   <li><strong>向量约束</strong> - 向量维度、批处理大小等限制 / Vector constraints - dimension, batch size limits</li>
 *   <li><strong>文本约束</strong> - 集合名称、元数据字段长度限制 / Text constraints - collection names, metadata field limits</li>
 *   <li><strong>缓存配置</strong> - 默认缓存大小和负载因子 / Cache configuration - default cache size and load factor</li>
 *   <li><strong>LLM配置</strong> - 语言模型的默认参数 / LLM configuration - default language model parameters</li>
 *   <li><strong>搜索配置</strong> - 相似度阈值和默认搜索限制 / Search configuration - similarity thresholds and default limits</li>
 * </ul>
 * 
 * <p>使用示例 / Usage Example:</p>
 * <pre>{@code
 * // 验证向量维度
 * if (embedding.size() > MemoryConstants.MAX_VECTOR_DIMENSION) {
 *     throw new IllegalArgumentException("Vector dimension exceeds maximum");
 * }
 * 
 * // 使用默认搜索限制
 * List<Memory> results = searchEngine.search(query, MemoryConstants.DEFAULT_SEARCH_LIMIT);
 * 
 * // 配置LLM参数
 * LLMConfig config = new LLMConfig()
 *     .setMaxTokens(MemoryConstants.DEFAULT_LLM_MAX_TOKENS)
 *     .setTemperature(MemoryConstants.DEFAULT_LLM_TEMPERATURE);
 * }</pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public final class MemoryConstants {
    
    // Prevent instantiation
    private MemoryConstants() {}
    
    // Vector constraints
    public static final int MAX_VECTOR_DIMENSION = 4096;
    public static final int MAX_BATCH_SIZE = 1000;
    public static final int MAX_SEARCH_TOPK = 10000;
    
    // Text/String constraints
    public static final int MAX_COLLECTION_NAME_LENGTH = 255;
    public static final int MAX_METADATA_KEY_LENGTH = 255;
    public static final int MAX_METADATA_STRING_VALUE_LENGTH = 10000;
    public static final int MAX_METADATA_FIELDS_COUNT = 100;
    
    // Cache configuration
    public static final int DEFAULT_MEMORY_CACHE_SIZE = 1000;
    public static final float DEFAULT_CACHE_LOAD_FACTOR = 0.75f;
    
    // LLM configuration
    public static final int DEFAULT_LLM_MAX_TOKENS = 1000;
    public static final int MEMORY_ENHANCEMENT_MAX_TOKENS = 500;
    public static final double DEFAULT_LLM_TEMPERATURE = 0.7;
    
    // Search and similarity
    public static final int DEFAULT_SEARCH_LIMIT = 10;
    public static final float MIN_SIMILARITY_THRESHOLD = 0.0f;
    public static final float MAX_SIMILARITY_THRESHOLD = 1.0f;
}