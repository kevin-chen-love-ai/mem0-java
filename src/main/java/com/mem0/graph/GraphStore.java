package com.mem0.graph;

import com.mem0.model.GraphNode;
import com.mem0.model.GraphRelationship;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 图存储接口，用于管理内存关系图和节点关系
 * Graph storage interface for managing memory relationship graphs and node connections
 * 
 * <p>提供以下功能 / Provides the following capabilities:</p>
 * <ul>
 *   <li>节点创建和管理 / Node creation and management</li>
 *   <li>关系创建和查询 / Relationship creation and querying</li>
 *   <li>图遍历和路径查找 / Graph traversal and path finding</li>
 *   <li>节点和关系的CRUD操作 / CRUD operations for nodes and relationships</li>
 *   <li>图数据库的健康监控 / Health monitoring for graph database</li>
 * </ul>
 * 
 * <p>使用示例 / Usage example:</p>
 * <pre>{@code
 * GraphStore graphStore = new InMemoryGraphStore();
 * 
 * // 创建节点
 * Map<String, Object> nodeProps = Map.of("name", "张三", "age", 30);
 * String nodeId = graphStore.createNode("Person", nodeProps).join();
 * 
 * // 创建关系
 * Map<String, Object> relProps = Map.of("since", "2020-01-01");
 * String relId = graphStore.createRelationship(nodeId1, nodeId2, "KNOWS", relProps).join();
 * 
 * // 查询相关节点
 * List<GraphNode> connected = graphStore.findConnectedNodes(nodeId, "KNOWS", 2).join();
 * }</pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public interface GraphStore {
    
    /**
     * 创建新节点 / Create a new node
     * 
     * @param label 节点标签 / Node label
     * @param properties 节点属性 / Node properties
     * @return 异步返回新创建的节点ID / Async return of newly created node ID
     */
    CompletableFuture<String> createNode(String label, Map<String, Object> properties);
    
    /**
     * 创建节点间的关系 / Create a relationship between nodes
     * 
     * @param fromNodeId 源节点ID / Source node ID
     * @param toNodeId 目标节点ID / Target node ID
     * @param type 关系类型 / Relationship type
     * @param properties 关系属性 / Relationship properties
     * @return 异步返回新创建的关系ID / Async return of newly created relationship ID
     */
    CompletableFuture<String> createRelationship(String fromNodeId, String toNodeId, 
                                                String type, Map<String, Object> properties);
    
    /**
     * 根据标签和属性查找节点 / Find nodes by label and properties
     * 
     * @param label 节点标签 / Node label
     * @param properties 查询属性 / Query properties
     * @return 异步返回匹配的节点列表 / Async return of matching nodes
     */
    CompletableFuture<List<GraphNode>> findNodes(String label, Map<String, Object> properties);
    
    /**
     * 根据类型和属性查找关系 / Find relationships by type and properties
     * 
     * @param type 关系类型 / Relationship type
     * @param properties 查询属性 / Query properties
     * @return 异步返回匹配的关系列表 / Async return of matching relationships
     */
    CompletableFuture<List<GraphRelationship>> findRelationships(String type, 
                                                               Map<String, Object> properties);
    
    /**
     * 根据ID获取节点 / Get node by ID
     * 
     * @param id 节点ID / Node ID
     * @return 异步返回节点对象，不存在时返回null / Async return of node object, null if not found
     */
    CompletableFuture<GraphNode> getNodeById(String id);
    
    /**
     * 根据ID获取关系 / Get relationship by ID
     * 
     * @param id 关系ID / Relationship ID
     * @return 异步返回关系对象，不存在时返回null / Async return of relationship object, null if not found
     */
    CompletableFuture<GraphRelationship> getRelationshipById(String id);
    
    /**
     * 删除节点 / Delete a node
     * 
     * @param id 节点ID / Node ID
     * @return 异步返回删除结果 / Async return of deletion result
     */
    CompletableFuture<Boolean> deleteNode(String id);
    
    /**
     * 删除关系 / Delete a relationship
     * 
     * @param id 关系ID / Relationship ID
     * @return 异步返回删除结果 / Async return of deletion result
     */
    CompletableFuture<Boolean> deleteRelationship(String id);
    
    /**
     * 删除所有数据 / Delete all data
     * 
     * @return 异步完成信号 / Async completion signal
     */
    CompletableFuture<Void> deleteAll();
    
    /**
     * 统计节点数量 / Count nodes
     * 
     * @return 异步返回节点总数 / Async return of total node count
     */
    CompletableFuture<Long> countNodes();
    
    /**
     * 统计关系数量 / Count relationships
     * 
     * @return 异步返回关系总数 / Async return of total relationship count
     */
    CompletableFuture<Long> countRelationships();
    
    /**
     * 根据标签获取节点 / Get nodes by label
     * 
     * @param label 节点标签 / Node label
     * @param properties 筛选属性 / Filter properties
     * @return 异步返回匹配的节点列表 / Async return of matching nodes
     */
    CompletableFuture<List<GraphNode>> getNodesByLabel(String label, Map<String, Object> properties);
    
    /**
     * 查找连接的节点 / Find connected nodes
     * 
     * @param nodeId 起始节点ID / Starting node ID
     * @param relationshipType 关系类型，null表示所有类型 / Relationship type, null for all types
     * @param maxDepth 最大遍历深度 / Maximum traversal depth
     * @return 异步返回连接的节点列表 / Async return of connected nodes
     */
    CompletableFuture<List<GraphNode>> findConnectedNodes(String nodeId, String relationshipType, int maxDepth);
    
    // Additional methods required by implementations
    
    /**
     * 更新节点属性 / Update node properties
     * 
     * @param nodeId 节点ID / Node ID
     * @param properties 新的属性值 / New property values
     * @return 异步完成信号 / Async completion signal
     */
    CompletableFuture<Void> updateNode(String nodeId, Map<String, Object> properties);
    
    /**
     * 添加内存到图中 / Add memory to graph
     * 
     * @param memory 增强型内存对象 / Enhanced memory object
     * @return 异步完成信号 / Async completion signal
     */
    CompletableFuture<Void> addMemory(com.mem0.core.EnhancedMemory memory);
    
    /**
     * 更新图中的内存 / Update memory in graph
     * 
     * @param memory 更新的内存对象 / Updated memory object
     * @return 异步完成信号 / Async completion signal
     */
    CompletableFuture<Void> updateMemory(com.mem0.core.EnhancedMemory memory);
    
    /**
     * 从图中删除内存 / Delete memory from graph
     * 
     * @param memoryId 内存ID / Memory ID
     * @return 异步完成信号 / Async completion signal
     */
    CompletableFuture<Void> deleteMemory(String memoryId);
    
    /**
     * 获取指定的内存 / Get specified memory
     * 
     * @param memoryId 内存ID / Memory ID
     * @return 异步返回内存对象，不存在时返回null / Async return of memory object, null if not found
     */
    CompletableFuture<com.mem0.core.EnhancedMemory> getMemory(String memoryId);
    
    /**
     * 关闭图存储连接和资源 / Close graph store connection and resources
     */
    void close();
    
    /**
     * 检查图存储健康状态 / Check graph store health status
     * 
     * @return true表示健康 / true if healthy
     */
    boolean isHealthy();
}