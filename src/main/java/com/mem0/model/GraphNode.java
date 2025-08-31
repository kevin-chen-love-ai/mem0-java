package com.mem0.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图节点数据模型 / Graph Node Data Model
 * 
 * 图节点是图数据库中的基本实体，代表图结构中的一个顶点。该类封装了节点的标识信息、
 * 类型分类和动态属性数据，支持图数据库中复杂关系网络的构建和管理。
 * 
 * Graph node is the fundamental entity in graph databases, representing a vertex 
 * in graph structures. This class encapsulates node identification information, 
 * type classification, and dynamic property data, supporting the construction 
 * and management of complex relationship networks in graph databases.
 * 
 * 主要功能 / Key Features:
 * - 节点标识：提供唯一的节点标识符
 * - 类型分类：支持节点类型标签，便于查询和分组
 * - 动态属性：支持键值对形式的自定义属性存储
 * - 并发安全：使用线程安全的属性映射容器
 * - 访问跟踪：记录节点访问时间，支持缓存和性能优化
 * 
 * - Node identification: Provide unique node identifier
 * - Type classification: Support node type labels for querying and grouping
 * - Dynamic properties: Support custom property storage in key-value format
 * - Concurrency safety: Use thread-safe property mapping container
 * - Access tracking: Record node access time for caching and performance optimization
 * 
 * 数据结构 / Data Structure:
 * - id: 节点的唯一标识符 / Unique identifier for the node
 * - type: 节点类型标签 / Node type label
 * - properties: 节点属性映射 / Node properties mapping
 * - lastAccessTime: 最后访问时间戳 / Last access timestamp
 * 
 * 使用示例 / Usage Example:
 * <pre>{@code
 * // Create a person node
 * Map<String, Object> properties = new HashMap<>();
 * properties.put("name", "Alice");
 * properties.put("age", 30);
 * properties.put("city", "Beijing");
 * 
 * GraphNode personNode = new GraphNode("person_001", "Person", properties);
 * 
 * // Access node information
 * String nodeId = personNode.getId();
 * String nodeType = personNode.getType();
 * Object name = personNode.getProperties().get("name");
 * 
 * // Update access tracking
 * personNode.updateAccess();
 * }</pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class GraphNode {
    private final String id;
    private final String type;
    private final Map<String, Object> properties;
    private volatile long lastAccessTime;

    public GraphNode(String id, String type, Map<String, Object> properties) {
        this.id = id;
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
        this.lastAccessTime = System.currentTimeMillis();
    }

    public void updateAccess() {
        this.lastAccessTime = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    @Override
    public String toString() {
        return String.format("GraphNode{id='%s', type='%s'}", id, type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphNode graphNode = (GraphNode) o;
        return id.equals(graphNode.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}