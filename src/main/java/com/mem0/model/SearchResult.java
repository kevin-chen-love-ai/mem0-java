package com.mem0.model;

import java.util.Map;

/**
 * 搜索结果模型，用于表示向量相似性搜索的结果
 * Search result model for representing vector similarity search results
 * 
 * <p>搜索结果包含以下信息 / Search result contains the following information:</p>
 * <ul>
 *   <li>唯一标识符 / Unique identifier</li>
 *   <li>相似度评分（0.0-1.0） / Similarity score (0.0-1.0)</li>
 *   <li>相关属性和元数据 / Associated properties and metadata</li>
 * </ul>
 * 
 * <p>使用示例 / Usage example:</p>
 * <pre>{@code
 * // 创建搜索结果
 * Map<String, Object> props = Map.of("content", "用户喜欢咖啡", "type", "preference");
 * SearchResult result = new SearchResult("mem_123", 0.85f, props);
 * 
 * // 检查相似度
 * if (result.getSimilarity() > 0.8) {
 *     System.out.println("高相似度匹配: " + result.getId());
 * }
 * }</pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class SearchResult {
    /** 搜索结果的唯一标识符 / Unique identifier of the search result */
    public final String id;
    
    /** 相似度评分，范围0.0-1.0 / Similarity score, range 0.0-1.0 */
    public final float similarity;
    
    /** 相关属性和元数据 / Associated properties and metadata */
    public final Map<String, Object> properties;

    /**
     * 构造搜索结果对象 / Constructor for search result
     * 
     * @param id 唯一标识符 / Unique identifier
     * @param similarity 相似度评分，范围0.0-1.0 / Similarity score, range 0.0-1.0
     * @param properties 相关属性和元数据 / Associated properties and metadata
     */
    public SearchResult(String id, float similarity, Map<String, Object> properties) {
        this.id = id;
        this.similarity = similarity;
        this.properties = properties;
    }

    /**
     * 获取搜索结果的唯一标识符 / Get unique identifier of the search result
     * 
     * @return 标识符字符串 / Identifier string
     */
    public String getId() {
        return id;
    }

    /**
     * 获取相似度评分 / Get similarity score
     * 
     * @return 相似度评分，范围0.0-1.0 / Similarity score, range 0.0-1.0
     */
    public float getSimilarity() {
        return similarity;
    }

    /**
     * 获取搜索结果的属性和元数据 / Get properties and metadata of the search result
     * 
     * @return 属性映射 / Property map
     */
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    /**
     * 获取元数据（兼容性方法） / Get metadata (compatibility method)
     * 
     * @return 元数据映射 / Metadata map
     * @deprecated 使用 {@link #getProperties()} 替代 / Use {@link #getProperties()} instead
     */
    @Deprecated
    public Map<String, Object> getMetadata() {
        return properties;
    }

    @Override
    public String toString() {
        return String.format("SearchResult{id='%s', similarity=%.3f}", id, similarity);
    }
}