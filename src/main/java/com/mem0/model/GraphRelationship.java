package com.mem0.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图关系数据模型 / Graph Relationship Data Model
 * 
 * 图关系表示图数据库中连接两个节点的边，定义了节点之间的语义连接。该类封装了
 * 关系的标识信息、连接的源节点和目标节点、关系类型以及关系的属性数据。
 * 
 * Graph relationship represents an edge connecting two nodes in graph databases, 
 * defining semantic connections between nodes. This class encapsulates relationship 
 * identification information, connected source and target nodes, relationship type, 
 * and relationship property data.
 * 
 * 主要功能 / Key Features:
 * - 关系标识：提供唯一的关系标识符
 * - 节点连接：明确定义源节点和目标节点的连接关系
 * - 类型分类：支持关系类型标签，表达不同的语义连接
 * - 动态属性：支持关系的自定义属性存储
 * - 创建跟踪：记录关系创建时间，支持时序分析
 * 
 * - Relationship identification: Provide unique relationship identifier
 * - Node connection: Clearly define connection between source and target nodes
 * - Type classification: Support relationship type labels for different semantic connections
 * - Dynamic properties: Support custom property storage for relationships
 * - Creation tracking: Record relationship creation time for temporal analysis
 * 
 * 数据结构 / Data Structure:
 * - id: 关系的唯一标识符 / Unique identifier for the relationship
 * - sourceNodeId: 源节点标识符 / Source node identifier
 * - targetNodeId: 目标节点标识符 / Target node identifier
 * - type: 关系类型标签 / Relationship type label
 * - properties: 关系属性映射 / Relationship properties mapping
 * - createdTime: 关系创建时间戳 / Relationship creation timestamp
 * 
 * 使用示例 / Usage Example:
 * <pre>{@code
 * // Create a friendship relationship
 * Map<String, Object> properties = new HashMap<>();
 * properties.put("since", "2023-01-15");
 * properties.put("strength", 0.8);
 * properties.put("interaction_count", 156);
 * 
 * GraphRelationship friendship = new GraphRelationship(
 *     "rel_001", "person_001", "person_002", "FRIEND", properties);
 * 
 * // Access relationship information
 * String relationshipId = friendship.getId();
 * String sourceId = friendship.getSourceNodeId();
 * String targetId = friendship.getTargetNodeId();
 * String relationType = friendship.getType();
 * 
 * // Get relationship properties
 * Object since = friendship.getProperties().get("since");
 * }</pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class GraphRelationship {
    private final String id;
    private final String sourceNodeId;
    private final String targetNodeId;
    private final String type;
    private final Map<String, Object> properties;
    private volatile long createdTime;

    public GraphRelationship(String id, String sourceNodeId, String targetNodeId, 
                           String type, Map<String, Object> properties) {
        this.id = id;
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
        this.type = type;
        this.properties = new ConcurrentHashMap<>();
        if (properties != null) {
            // Filter out null values since ConcurrentHashMap doesn't allow them
            properties.forEach((key, value) -> {
                if (value != null) {
                    this.properties.put(key, value);
                }
            });
        }
        this.createdTime = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getSourceNodeId() {
        return sourceNodeId;
    }

    public String getTargetNodeId() {
        return targetNodeId;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    @Override
    public String toString() {
        return String.format("GraphRelationship{id='%s', source='%s', target='%s', type='%s'}", 
            id, sourceNodeId, targetNodeId, type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphRelationship that = (GraphRelationship) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}