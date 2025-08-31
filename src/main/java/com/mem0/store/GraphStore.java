package com.mem0.store;

import com.mem0.core.EnhancedMemory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 图数据库存储接口 - Graph database store interface
 * 
 * <p>此接口定义了图数据库的标准操作规范，提供了节点、关系的创建、查询、
 * 更新和删除功能，以及专门针对内存对象的图存储操作。它支持复杂的图查询
 * 和关系分析，为内存系统提供强大的关联关系管理能力。</p>
 * 
 * <p>This interface defines standard operational specifications for graph databases,
 * providing creation, querying, updating, and deletion functionalities for nodes
 * and relationships, as well as specialized graph storage operations for memory objects.
 * It supports complex graph queries and relationship analysis, providing powerful
 * associative relationship management capabilities for memory systems.</p>
 * 
 * <h3>核心功能特性 / Core Functional Features:</h3>
 * <ul>
 *   <li><strong>节点管理</strong> - 创建、查询、更新、删除图节点 / Node management - create, query, update, delete graph nodes</li>
 *   <li><strong>关系管理</strong> - 建立、查询、修改节点间的关系 / Relationship management - establish, query, modify relationships between nodes</li>
 *   <li><strong>图查询</strong> - 支持Cypher等图查询语言执行复杂查询 / Graph querying - supporting complex queries with Cypher and other graph query languages</li>
 *   <li><strong>内存专用操作</strong> - 针对内存对象优化的专门接口 / Memory-specific operations - specialized interfaces optimized for memory objects</li>
 *   <li><strong>关系遍历</strong> - 多跳关系查找和图结构分析 / Relationship traversal - multi-hop relationship finding and graph structure analysis</li>
 *   <li><strong>异步处理</strong> - 所有操作均支持异步执行避免阻塞 / Asynchronous processing - all operations support asynchronous execution to avoid blocking</li>
 * </ul>
 * 
 * <h3>图数据库架构 / Graph Database Architecture:</h3>
 * <pre>
 * 图存储数据模型 / Graph Storage Data Model:
 * 
 * ┌─ NODES (节点) ──────────────────────────────┐
 * │ ┌─ Memory Node ─────────────────────────┐   │
 * │ │ id: "mem_123"                         │   │
 * │ │ labels: ["Memory", "Semantic"]        │   │
 * │ │ properties: {                         │   │
 * │ │   content: "User likes coffee"        │   │
 * │ │   type: "preference"                  │   │
 * │ │   importance: 4                       │   │
 * │ │   created_at: "2024-01-01T10:00:00"   │   │
 * │ │ }                                     │   │
 * │ └───────────────────────────────────────┘   │
 * │ ┌─ Entity Node ─────────────────────────┐   │
 * │ │ id: "entity_456"                      │   │
 * │ │ labels: ["Entity", "Person"]          │   │
 * │ │ properties: {                         │   │
 * │ │   name: "John Doe"                    │   │
 * │ │   type: "person"                      │   │
 * │ │ }                                     │   │
 * │ └───────────────────────────────────────┘   │
 * └─────────────────────────────────────────────┘
 * 
 * ┌─ RELATIONSHIPS (关系) ──────────────────────┐
 * │ Memory ──[MENTIONS]──> Entity               │
 * │ Memory ──[SIMILAR_TO]──> Memory             │
 * │ Memory ──[CONFLICTS_WITH]──> Memory         │
 * │ Memory ──[DERIVED_FROM]──> Memory           │
 * │ Entity ──[RELATED_TO]──> Entity             │
 * └─────────────────────────────────────────────┘
 * </pre>
 * 
 * <h3>使用示例 / Usage Examples:</h3>
 * <pre>{@code
 * // 创建图存储实例 (通常通过工厂或配置获取)
 * GraphStore graphStore = getGraphStore(); // Neo4j, ArangoDB, Amazon Neptune等
 * 
 * // === 基础节点操作 / Basic Node Operations ===
 * 
 * // 创建内存节点
 * Map<String, Object> memoryProps = new HashMap<>();
 * memoryProps.put("content", "用户喜欢喝咖啡");
 * memoryProps.put("type", "preference");
 * memoryProps.put("importance", 4);
 * memoryProps.put("user_id", "user123");
 * 
 * CompletableFuture<String> nodeIdFuture = 
 *     graphStore.createNode("Memory", memoryProps);
 * String memoryNodeId = nodeIdFuture.join();
 * 
 * // 创建实体节点
 * Map<String, Object> entityProps = new HashMap<>();
 * entityProps.put("name", "咖啡");
 * entityProps.put("type", "beverage");
 * 
 * CompletableFuture<String> entityNodeIdFuture = 
 *     graphStore.createNode("Entity", entityProps);
 * String entityNodeId = entityNodeIdFuture.join();
 * 
 * // === 关系操作 / Relationship Operations ===
 * 
 * // 建立内存与实体的关系
 * Map<String, Object> relationProps = new HashMap<>();
 * relationProps.put("strength", 0.9);
 * relationProps.put("created_at", LocalDateTime.now().toString());
 * 
 * CompletableFuture<String> relationshipFuture = graphStore.createRelationship(
 *     memoryNodeId, entityNodeId, "MENTIONS", relationProps
 * );
 * String relationshipId = relationshipFuture.join();
 * 
 * // === 查询操作 / Query Operations ===
 * 
 * // 查询节点
 * CompletableFuture<GraphNode> nodeFuture = graphStore.getNode(memoryNodeId);
 * GraphStore.GraphNode memoryNode = nodeFuture.join();
 * System.out.println("内存内容: " + memoryNode.getProperties().get("content"));
 * 
 * // 查询相关节点
 * CompletableFuture<List<GraphStore.GraphNode>> connectedFuture = 
 *     graphStore.findConnectedNodes(memoryNodeId, "MENTIONS", 2);
 * List<GraphStore.GraphNode> connectedNodes = connectedFuture.join();
 * 
 * // 执行Cypher查询
 * String cypher = "MATCH (m:Memory)-[r:MENTIONS]->(e:Entity) " +
 *                 "WHERE m.user_id = $userId " +
 *                 "RETURN m, r, e ORDER BY r.strength DESC LIMIT $limit";
 * 
 * Map<String, Object> params = new HashMap<>();
 * params.put("userId", "user123");
 * params.put("limit", 10);
 * 
 * CompletableFuture<List<Map<String, Object>>> queryFuture = 
 *     graphStore.executeQuery(cypher, params);
 * List<Map<String, Object>> results = queryFuture.join();
 * 
 * // === 内存专用操作 / Memory-Specific Operations ===
 * 
 * // 添加内存对象
 * EnhancedMemory memory = createEnhancedMemory();
 * CompletableFuture<Void> addMemoryFuture = graphStore.addMemory(memory);
 * addMemoryFuture.join();
 * 
 * // 搜索相关内存
 * CompletableFuture<List<EnhancedMemory>> searchFuture = 
 *     graphStore.searchMemories("咖啡", "user123", 10);
 * List<EnhancedMemory> relatedMemories = searchFuture.join();
 * 
 * // 建立内存关系
 * CompletableFuture<Void> addRelationFuture = graphStore.addRelationship(
 *     memory1.getId(), memory2.getId(), "SIMILAR_TO", relationProps
 * );
 * addRelationFuture.join();
 * 
 * // === 更新和删除 / Update and Delete ===
 * 
 * // 更新节点属性
 * Map<String, Object> updateProps = new HashMap<>();
 * updateProps.put("importance", 5);
 * updateProps.put("last_updated", LocalDateTime.now().toString());
 * 
 * CompletableFuture<Void> updateFuture = graphStore.updateNode(memoryNodeId, updateProps);
 * updateFuture.join();
 * 
 * // 删除关系
 * CompletableFuture<Void> deleteRelationFuture = 
 *     graphStore.deleteRelationship(relationshipId);
 * deleteRelationFuture.join();
 * 
 * // 关闭连接
 * CompletableFuture<Void> closeFuture = graphStore.close();
 * closeFuture.join();
 * }</pre>
 * 
 * <h3>图查询能力 / Graph Query Capabilities:</h3>
 * <ul>
 *   <li><strong>路径查询</strong> - 查找节点间的最短路径和所有路径 / Path queries - finding shortest paths and all paths between nodes</li>
 *   <li><strong>邻居查询</strong> - 查找指定跳数内的邻居节点 / Neighbor queries - finding neighbor nodes within specified hops</li>
 *   <li><strong>模式匹配</strong> - 基于图模式的复杂查询匹配 / Pattern matching - complex query matching based on graph patterns</li>
 *   <li><strong>聚合分析</strong> - 对图结构进行统计和聚合分析 / Aggregation analysis - statistical and aggregation analysis on graph structures</li>
 *   <li><strong>子图提取</strong> - 提取满足条件的子图结构 / Subgraph extraction - extracting subgraph structures meeting conditions</li>
 * </ul>
 * 
 * <h3>实现特性 / Implementation Features:</h3>
 * <ul>
 *   <li><strong>数据库无关</strong> - 支持Neo4j、ArangoDB、Amazon Neptune等多种图数据库 / Database agnostic - supporting various graph databases</li>
 *   <li><strong>事务支持</strong> - 支持ACID事务保证数据一致性 / Transaction support - ACID transactions for data consistency</li>
 *   <li><strong>连接池管理</strong> - 高效的数据库连接池管理 / Connection pool management - efficient database connection pooling</li>
 *   <li><strong>错误恢复</strong> - 完善的错误处理和恢复机制 / Error recovery - comprehensive error handling and recovery mechanisms</li>
 *   <li><strong>性能优化</strong> - 索引优化和查询性能调优 / Performance optimization - index optimization and query performance tuning</li>
 * </ul>
 * 
 * <h3>适用场景 / Suitable Scenarios:</h3>
 * <ul>
 *   <li><strong>知识图谱</strong> - 构建和查询大规模知识图谱 / Knowledge graphs - building and querying large-scale knowledge graphs</li>
 *   <li><strong>关系分析</strong> - 分析实体间的复杂关系网络 / Relationship analysis - analyzing complex relationship networks between entities</li>
 *   <li><strong>推荐系统</strong> - 基于图结构的个性化推荐 / Recommendation systems - graph-based personalized recommendations</li>
 *   <li><strong>社交网络</strong> - 社交关系图的存储和分析 / Social networks - storage and analysis of social relationship graphs</li>
 *   <li><strong>语义搜索</strong> - 基于语义关系的智能搜索 / Semantic search - intelligent search based on semantic relationships</li>
 * </ul>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 * @see EnhancedMemory
 * @see VectorStore
 * @see MemoryStore
 */
public interface GraphStore {
    
    CompletableFuture<String> createNode(String label, Map<String, Object> properties);
    
    CompletableFuture<String> createRelationship(String sourceNodeId, String targetNodeId, 
                                                String relationshipType, 
                                                Map<String, Object> properties);
    
    CompletableFuture<GraphNode> getNode(String nodeId);
    
    CompletableFuture<List<GraphNode>> getNodesByLabel(String label, Map<String, Object> properties);
    
    CompletableFuture<List<GraphRelationship>> getRelationships(String nodeId, String relationshipType);
    
    CompletableFuture<List<GraphNode>> findConnectedNodes(String nodeId, String relationshipType, int maxHops);
    
    CompletableFuture<Void> updateNode(String nodeId, Map<String, Object> properties);
    
    CompletableFuture<Void> updateRelationship(String relationshipId, Map<String, Object> properties);
    
    CompletableFuture<Void> deleteNode(String nodeId);
    
    CompletableFuture<Void> deleteRelationship(String relationshipId);
    
    CompletableFuture<List<Map<String, Object>>> executeQuery(String cypher, Map<String, Object> parameters);
    
    // Memory-specific methods
    CompletableFuture<Void> addMemory(EnhancedMemory memory);
    
    CompletableFuture<EnhancedMemory> getMemory(String memoryId);
    
    CompletableFuture<Void> updateMemory(EnhancedMemory memory);
    
    CompletableFuture<Void> deleteMemory(String memoryId);
    
    CompletableFuture<List<EnhancedMemory>> getUserMemories(String userId);
    
    CompletableFuture<List<EnhancedMemory>> getMemoryHistory(String userId);
    
    CompletableFuture<List<EnhancedMemory>> searchMemories(String query, String userId, int limit);
    
    CompletableFuture<Void> addRelationship(String fromMemoryId, String toMemoryId, String relationshipType, Map<String, Object> properties);
    
    CompletableFuture<Void> close();
    
    static class GraphNode {
        private final String id;
        private final List<String> labels;
        private final Map<String, Object> properties;
        
        public GraphNode(String id, List<String> labels, Map<String, Object> properties) {
            this.id = id;
            this.labels = labels;
            this.properties = properties != null ? properties : new HashMap<>();
        }
        
        public String getId() { return id; }
        public List<String> getLabels() { return labels; }
        public Map<String, Object> getProperties() { return properties; }
    }
    
    static class GraphRelationship {
        private final String id;
        private final String type;
        private final String sourceNodeId;
        private final String targetNodeId;
        private final Map<String, Object> properties;
        
        public GraphRelationship(String id, String type, String sourceNodeId, String targetNodeId,
                               Map<String, Object> properties) {
            this.id = id;
            this.type = type;
            this.sourceNodeId = sourceNodeId;
            this.targetNodeId = targetNodeId;
            this.properties = properties;
        }
        
        public String getId() { return id; }
        public String getType() { return type; }
        public String getSourceNodeId() { return sourceNodeId; }
        public String getTargetNodeId() { return targetNodeId; }
        public Map<String, Object> getProperties() { return properties; }
    }
}