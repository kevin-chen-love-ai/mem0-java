package com.mem0.model;

import java.util.Map;

/**
 * 向量条目数据模型 / Vector Entry Data Model
 * 
 * 向量条目是向量存储系统中的基本数据单元，封装了向量数据及其关联的元数据信息。
 * 该类表示一个完整的向量记录，包含唯一标识符、向量嵌入数据、用户标识和自定义属性。
 * 
 * Vector entry is the fundamental data unit in vector storage systems, encapsulating 
 * vector data and its associated metadata information. This class represents a complete 
 * vector record containing unique identifier, vector embedding data, user identifier, 
 * and custom properties.
 * 
 * 主要功能 / Key Features:
 * - 向量数据存储：存储高维向量嵌入数据
 * - 元数据管理：支持自定义属性和元数据
 * - 用户关联：关联特定用户的向量数据
 * - 访问跟踪：记录最后访问时间用于缓存管理
 * - 不可变性：核心数据字段设计为不可变，保证数据一致性
 * 
 * - Vector data storage: Store high-dimensional vector embedding data
 * - Metadata management: Support custom properties and metadata
 * - User association: Associate vector data with specific users
 * - Access tracking: Record last access time for cache management
 * - Immutability: Core data fields designed as immutable for data consistency
 * 
 * 数据结构 / Data Structure:
 * - id: 向量条目的唯一标识符 / Unique identifier for the vector entry
 * - embedding: 向量嵌入数据数组 / Vector embedding data array
 * - userId: 关联的用户标识符 / Associated user identifier
 * - properties: 自定义属性映射 / Custom properties mapping
 * - lastAccessTime: 最后访问时间戳 / Last access timestamp
 * 
 * 使用示例 / Usage Example:
 * <pre>{@code
 * // Create vector entry with embedding data
 * float[] embedding = {0.1f, 0.2f, 0.3f, 0.4f};
 * Map<String, Object> properties = new HashMap<>();
 * properties.put("category", "document");
 * properties.put("score", 0.95);
 * 
 * VectorEntry entry = new VectorEntry("vec_001", embedding, "user123", properties);
 * 
 * // Access vector data
 * float[] vectorData = entry.getEmbedding();
 * String userId = entry.getUserId();
 * 
 * // Update access tracking
 * entry.updateAccess();
 * }</pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class VectorEntry {
    public final String id;
    public final float[] embedding;
    public final String userId;
    public final Map<String, Object> properties;
    private volatile long lastAccessTime;

    public VectorEntry(String id, float[] embedding, String userId, Map<String, Object> properties) {
        this.id = id;
        this.embedding = embedding;
        this.userId = userId;
        this.properties = properties;
        this.lastAccessTime = System.currentTimeMillis();
    }

    public void updateAccess() {
        this.lastAccessTime = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public String getUserId() {
        return userId;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    @Override
    public String toString() {
        return String.format("VectorEntry{id='%s', userId='%s', dim=%d}", 
            id, userId, embedding != null ? embedding.length : 0);
    }
}