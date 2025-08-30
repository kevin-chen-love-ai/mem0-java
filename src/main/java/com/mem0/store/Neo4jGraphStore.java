package com.mem0.store;

import com.mem0.core.EnhancedMemory;
import org.neo4j.driver.*;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

/**
 * Neo4j图数据库存储实现 / Neo4j Graph Database Storage Implementation
 * 
 * 基于Neo4j图数据库的图存储实现，提供高效的图数据建模、存储和查询功能。
 * Neo4j是业界领先的原生图数据库，专为处理高度连接的数据而设计，支持复杂的
 * 图查询和图算法，特别适合知识图谱、社交网络和推荐系统等应用场景。
 * 
 * Graph storage implementation based on Neo4j graph database, providing efficient 
 * graph data modeling, storage, and querying capabilities. Neo4j is the industry-leading 
 * native graph database designed for handling highly connected data, supporting complex 
 * graph queries and algorithms, particularly suitable for knowledge graphs, social 
 * networks, and recommendation systems.
 * 
 * 主要功能 / Key Features:
 * - 原生图存储：专为图数据优化的存储引擎和查询性能
 * - Cypher查询语言：声明式图查询语言，支持复杂的图遍历模式
 * - ACID事务：完整的事务支持，保证数据一致性和可靠性
 * - 图算法库：内置丰富的图算法，支持路径查找、中心性分析等
 * - 高可用性：支持集群部署和读写分离，提供企业级可用性
 * 
 * - Native graph storage: Storage engine and query performance optimized for graph data
 * - Cypher query language: Declarative graph query language supporting complex traversal patterns
 * - ACID transactions: Complete transaction support ensuring data consistency and reliability
 * - Graph algorithms library: Rich built-in graph algorithms supporting pathfinding, centrality analysis
 * - High availability: Support cluster deployment and read-write separation for enterprise availability
 * 
 * 集成详情 / Integration Details:
 * - 驱动版本：基于Neo4j Java Driver 5.x版本
 * - 连接管理：支持URI连接字符串和认证令牌
 * - 查询语言：使用Cypher查询语言进行图数据操作
 * - 会话管理：采用会话模式管理数据库连接和事务
 * - 异步操作：所有图数据库操作均采用CompletableFuture异步模式
 * 
 * - Driver version: Based on Neo4j Java Driver 5.x
 * - Connection management: Support URI connection strings and authentication tokens
 * - Query language: Use Cypher query language for graph data operations
 * - Session management: Use session pattern for database connection and transaction management
 * - Async operations: All graph database operations use CompletableFuture async pattern
 * 
 * 数据模型 / Data Model:
 * - Node: 图中的实体节点，具有标签和属性
 * - Relationship: 连接节点的有向关系，具有类型和属性
 * - Label: 节点分类标签，用于查询优化和数据组织
 * - Property: 节点和关系的键值对属性数据
 * 
 * - Node: Entity nodes in graph with labels and properties
 * - Relationship: Directed relationships connecting nodes with type and properties
 * - Label: Node classification labels for query optimization and data organization
 * - Property: Key-value property data for nodes and relationships
 * 
 * 使用示例 / Usage Example:
 * <pre>{@code
 * // Initialize Neo4j graph store
 * Neo4jGraphStore store = new Neo4jGraphStore(
 *     "neo4j://localhost:7687", "neo4j", "password");
 * 
 * // Create person nodes
 * Map<String, Object> aliceProps = Map.of("name", "Alice", "age", 30);
 * String aliceId = store.createNode("Person", aliceProps).join();
 * 
 * Map<String, Object> bobProps = Map.of("name", "Bob", "age", 25);
 * String bobId = store.createNode("Person", bobProps).join();
 * 
 * // Create friendship relationship
 * Map<String, Object> relProps = Map.of("since", "2023-01-01");
 * store.createRelationship(aliceId, bobId, "FRIEND", relProps).join();
 * 
 * // Query connected nodes
 * List<GraphNode> friends = store.findConnectedNodes(aliceId, "FRIEND", 2).join();
 * 
 * // Close connection
 * store.close().join();
 * }</pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class Neo4jGraphStore implements GraphStore {
    
    private static final Logger logger = LoggerFactory.getLogger(Neo4jGraphStore.class);
    
    private final Driver driver;
    
    public Neo4jGraphStore(String uri, String username, String password) {
        this.driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password));
        
        // Test connection
        try (Session session = driver.session()) {
            session.run("RETURN 1 as test").consume();
            logger.info("Connected to Neo4j at {}", uri);
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to Neo4j", e);
        }
    }
    
    @Override
    public CompletableFuture<String> createNode(String label, Map<String, Object> properties) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = driver.session()) {
                
                StringBuilder cypher = new StringBuilder("CREATE (n:");
                cypher.append(label).append(" {");
                
                // Add id property if not present
                Map<String, Object> nodeProperties = new HashMap<>(properties);
                if (!nodeProperties.containsKey("id")) {
                    nodeProperties.put("id", UUID.randomUUID().toString());
                }
                
                List<String> propertyAssignments = new ArrayList<>();
                for (String key : nodeProperties.keySet()) {
                    propertyAssignments.add(key + ": $" + key);
                }
                
                cypher.append(String.join(", ", propertyAssignments));
                cypher.append("}) RETURN n.id as id");
                
                Result result = session.run(cypher.toString(), nodeProperties);
                
                if (result.hasNext()) {
                    String nodeId = result.next().get("id").asString();
                    logger.debug("Created node with id: {}", nodeId);
                    return nodeId;
                } else {
                    throw new RuntimeException("Failed to create node");
                }
                
            } catch (Exception e) {
                throw new CompletionException("Failed to create node", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<String> createRelationship(String sourceNodeId, String targetNodeId, 
                                                      String relationshipType, 
                                                      Map<String, Object> properties) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = driver.session()) {
                
                Map<String, Object> relProperties = new HashMap<>(properties);
                if (!relProperties.containsKey("id")) {
                    relProperties.put("id", UUID.randomUUID().toString());
                }
                
                StringBuilder cypher = new StringBuilder(
                    "MATCH (source {id: $sourceId}), (target {id: $targetId}) " +
                    "CREATE (source)-[r:" + relationshipType + " {"
                );
                
                List<String> propertyAssignments = new ArrayList<>();
                for (String key : relProperties.keySet()) {
                    propertyAssignments.add(key + ": $" + key);
                }
                
                cypher.append(String.join(", ", propertyAssignments));
                cypher.append("}]->(target) RETURN r.id as id");
                
                Map<String, Object> parameters = new HashMap<>(relProperties);
                parameters.put("sourceId", sourceNodeId);
                parameters.put("targetId", targetNodeId);
                
                Result result = session.run(cypher.toString(), parameters);
                
                if (result.hasNext()) {
                    String relId = result.next().get("id").asString();
                    logger.debug("Created relationship with id: {}", relId);
                    return relId;
                } else {
                    throw new RuntimeException("Failed to create relationship");
                }
                
            } catch (Exception e) {
                throw new CompletionException("Failed to create relationship", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<GraphNode> getNode(String nodeId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = driver.session()) {
                
                Result result = session.run(
                    "MATCH (n {id: $nodeId}) RETURN n",
                    Values.parameters("nodeId", nodeId)
                );
                
                if (result.hasNext()) {
                    Node node = result.next().get("n").asNode();
                    return convertToGraphNode(node);
                } else {
                    return null;
                }
                
            } catch (Exception e) {
                throw new CompletionException("Failed to get node", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<GraphNode>> getNodesByLabel(String label, Map<String, Object> properties) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = driver.session()) {
                
                StringBuilder cypher = new StringBuilder("MATCH (n:").append(label);
                
                if (properties != null && !properties.isEmpty()) {
                    cypher.append(" {");
                    List<String> conditions = new ArrayList<>();
                    for (String key : properties.keySet()) {
                        conditions.add(key + ": $" + key);
                    }
                    cypher.append(String.join(", ", conditions));
                    cypher.append("}");
                }
                
                cypher.append(") RETURN n");
                
                Result result = session.run(cypher.toString(), properties != null ? properties : Collections.emptyMap());
                
                List<GraphNode> nodes = new ArrayList<>();
                while (result.hasNext()) {
                    Node node = result.next().get("n").asNode();
                    nodes.add(convertToGraphNode(node));
                }
                
                return nodes;
                
            } catch (Exception e) {
                throw new CompletionException("Failed to get nodes by label", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<GraphRelationship>> getRelationships(String nodeId, String relationshipType) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = driver.session()) {
                
                String cypher;
                if (relationshipType != null && !relationshipType.isEmpty()) {
                    cypher = "MATCH (n {id: $nodeId})-[r:" + relationshipType + "]-(m) RETURN r, startNode(r) as source, endNode(r) as target";
                } else {
                    cypher = "MATCH (n {id: $nodeId})-[r]-(m) RETURN r, startNode(r) as source, endNode(r) as target";
                }
                
                Result result = session.run(cypher, Values.parameters("nodeId", nodeId));
                
                List<GraphRelationship> relationships = new ArrayList<>();
                while (result.hasNext()) {
                    Record record = result.next();
                    Relationship rel = record.get("r").asRelationship();
                    Node source = record.get("source").asNode();
                    Node target = record.get("target").asNode();
                    
                    relationships.add(convertToGraphRelationship(rel, source, target));
                }
                
                return relationships;
                
            } catch (Exception e) {
                throw new CompletionException("Failed to get relationships", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<GraphNode>> findConnectedNodes(String nodeId, String relationshipType, int maxHops) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = driver.session()) {
                
                String cypher;
                if (relationshipType != null && !relationshipType.isEmpty()) {
                    cypher = String.format(
                        "MATCH (start {id: $nodeId})-[:%s*1..%d]-(connected) RETURN DISTINCT connected",
                        relationshipType, maxHops
                    );
                } else {
                    cypher = String.format(
                        "MATCH (start {id: $nodeId})-[*1..%d]-(connected) RETURN DISTINCT connected",
                        maxHops
                    );
                }
                
                Result result = session.run(cypher, Values.parameters("nodeId", nodeId));
                
                List<GraphNode> connectedNodes = new ArrayList<>();
                while (result.hasNext()) {
                    Node node = result.next().get("connected").asNode();
                    connectedNodes.add(convertToGraphNode(node));
                }
                
                return connectedNodes;
                
            } catch (Exception e) {
                throw new CompletionException("Failed to find connected nodes", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> updateNode(String nodeId, Map<String, Object> properties) {
        return CompletableFuture.runAsync(() -> {
            try (Session session = driver.session()) {
                
                if (properties == null || properties.isEmpty()) {
                    return;
                }
                
                StringBuilder cypher = new StringBuilder("MATCH (n {id: $nodeId}) SET ");
                List<String> setStatements = new ArrayList<>();
                
                for (String key : properties.keySet()) {
                    setStatements.add("n." + key + " = $" + key);
                }
                
                cypher.append(String.join(", ", setStatements));
                
                Map<String, Object> parameters = new HashMap<>(properties);
                parameters.put("nodeId", nodeId);
                
                session.run(cypher.toString(), parameters).consume();
                
            } catch (Exception e) {
                throw new CompletionException("Failed to update node", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> updateRelationship(String relationshipId, Map<String, Object> properties) {
        return CompletableFuture.runAsync(() -> {
            try (Session session = driver.session()) {
                
                if (properties == null || properties.isEmpty()) {
                    return;
                }
                
                StringBuilder cypher = new StringBuilder("MATCH ()-[r {id: $relationshipId}]-() SET ");
                List<String> setStatements = new ArrayList<>();
                
                for (String key : properties.keySet()) {
                    setStatements.add("r." + key + " = $" + key);
                }
                
                cypher.append(String.join(", ", setStatements));
                
                Map<String, Object> parameters = new HashMap<>(properties);
                parameters.put("relationshipId", relationshipId);
                
                session.run(cypher.toString(), parameters).consume();
                
            } catch (Exception e) {
                throw new CompletionException("Failed to update relationship", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> deleteNode(String nodeId) {
        return CompletableFuture.runAsync(() -> {
            try (Session session = driver.session()) {
                
                session.run(
                    "MATCH (n {id: $nodeId}) DETACH DELETE n",
                    Values.parameters("nodeId", nodeId)
                ).consume();
                
            } catch (Exception e) {
                throw new CompletionException("Failed to delete node", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> deleteRelationship(String relationshipId) {
        return CompletableFuture.runAsync(() -> {
            try (Session session = driver.session()) {
                
                session.run(
                    "MATCH ()-[r {id: $relationshipId}]-() DELETE r",
                    Values.parameters("relationshipId", relationshipId)
                ).consume();
                
            } catch (Exception e) {
                throw new CompletionException("Failed to delete relationship", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<Map<String, Object>>> executeQuery(String cypher, Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = driver.session()) {
                
                Result result = session.run(cypher, parameters != null ? parameters : Collections.emptyMap());
                
                List<Map<String, Object>> records = new ArrayList<>();
                while (result.hasNext()) {
                    Record record = result.next();
                    Map<String, Object> recordMap = new HashMap<>();
                    
                    for (String key : record.keys()) {
                        recordMap.put(key, record.get(key).asObject());
                    }
                    
                    records.add(recordMap);
                }
                
                return records;
                
            } catch (Exception e) {
                throw new CompletionException("Failed to execute query", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.runAsync(() -> {
            try {
                driver.close();
                logger.info("Closed Neo4j connection");
            } catch (Exception e) {
                throw new CompletionException("Failed to close Neo4j connection", e);
            }
        });
    }
    
    private GraphNode convertToGraphNode(Node node) {
        String id = node.get("id").asString();
        List<String> labels = new ArrayList<>();
        node.labels().forEach(labels::add);
        
        Map<String, Object> properties = new HashMap<>();
        for (String key : node.keys()) {
            properties.put(key, node.get(key).asObject());
        }
        
        return new GraphNode(id, labels, properties);
    }
    
    private GraphRelationship convertToGraphRelationship(Relationship rel, Node source, Node target) {
        String id = rel.get("id").asString();
        String type = rel.type();
        String sourceId = source.get("id").asString();
        String targetId = target.get("id").asString();
        
        Map<String, Object> properties = new HashMap<>();
        for (String key : rel.keys()) {
            properties.put(key, rel.get(key).asObject());
        }
        
        return new GraphRelationship(id, type, sourceId, targetId, properties);
    }
    
    // Memory-specific methods implementation for Neo4j
    
    @Override
    public CompletableFuture<Void> addMemory(EnhancedMemory memory) {
        return CompletableFuture.runAsync(() -> {
            try (Session session = driver.session()) {
                Map<String, Object> properties = new HashMap<>();
                properties.put("id", memory.getId());
                properties.put("content", memory.getContent());
                properties.put("userId", memory.getUserId());
                properties.put("createdAt", memory.getCreatedAt().toString());
                if (memory.getUpdatedAt() != null) {
                    properties.put("updatedAt", memory.getUpdatedAt().toString());
                }
                
                String cypher = "CREATE (m:Memory {id: $id, content: $content, userId: $userId, createdAt: $createdAt" +
                              (properties.containsKey("updatedAt") ? ", updatedAt: $updatedAt" : "") + "})";
                
                session.run(cypher, properties).consume();
                logger.debug("Added memory with id: {}", memory.getId());
            } catch (Exception e) {
                throw new CompletionException("Failed to add memory", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<EnhancedMemory> getMemory(String memoryId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = driver.session()) {
                Result result = session.run("MATCH (m:Memory {id: $memoryId}) RETURN m", 
                                           Values.parameters("memoryId", memoryId));
                
                if (result.hasNext()) {
                    Node node = result.next().get("m").asNode();
                    return convertToEnhancedMemory(node);
                }
                return null;
            } catch (Exception e) {
                throw new CompletionException("Failed to get memory", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> updateMemory(EnhancedMemory memory) {
        return CompletableFuture.runAsync(() -> {
            try (Session session = driver.session()) {
                Map<String, Object> properties = new HashMap<>();
                properties.put("memoryId", memory.getId());
                properties.put("content", memory.getContent());
                properties.put("updatedAt", memory.getUpdatedAt() != null ? 
                              memory.getUpdatedAt().toString() : java.time.Instant.now().toString());
                
                String cypher = "MATCH (m:Memory {id: $memoryId}) SET m.content = $content, m.updatedAt = $updatedAt";
                session.run(cypher, properties).consume();
                logger.debug("Updated memory with id: {}", memory.getId());
            } catch (Exception e) {
                throw new CompletionException("Failed to update memory", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> deleteMemory(String memoryId) {
        return CompletableFuture.runAsync(() -> {
            try (Session session = driver.session()) {
                session.run("MATCH (m:Memory {id: $memoryId}) DETACH DELETE m", 
                          Values.parameters("memoryId", memoryId)).consume();
                logger.debug("Deleted memory with id: {}", memoryId);
            } catch (Exception e) {
                throw new CompletionException("Failed to delete memory", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<EnhancedMemory>> getUserMemories(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = driver.session()) {
                Result result = session.run("MATCH (m:Memory {userId: $userId}) RETURN m ORDER BY m.createdAt", 
                                           Values.parameters("userId", userId));
                
                List<EnhancedMemory> memories = new ArrayList<>();
                while (result.hasNext()) {
                    Node node = result.next().get("m").asNode();
                    memories.add(convertToEnhancedMemory(node));
                }
                return memories;
            } catch (Exception e) {
                throw new CompletionException("Failed to get user memories", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<EnhancedMemory>> getMemoryHistory(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = driver.session()) {
                Result result = session.run("MATCH (m:Memory {userId: $userId}) RETURN m ORDER BY m.createdAt ASC", 
                                           Values.parameters("userId", userId));
                
                List<EnhancedMemory> memories = new ArrayList<>();
                while (result.hasNext()) {
                    Node node = result.next().get("m").asNode();
                    memories.add(convertToEnhancedMemory(node));
                }
                return memories;
            } catch (Exception e) {
                throw new CompletionException("Failed to get memory history", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<EnhancedMemory>> searchMemories(String query, String userId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = driver.session()) {
                Result result = session.run("MATCH (m:Memory {userId: $userId}) " +
                                           "WHERE toLower(m.content) CONTAINS toLower($query) " +
                                           "RETURN m ORDER BY m.createdAt DESC LIMIT $limit", 
                                           Values.parameters("userId", userId, "query", query, "limit", limit));
                
                List<EnhancedMemory> memories = new ArrayList<>();
                while (result.hasNext()) {
                    Node node = result.next().get("m").asNode();
                    memories.add(convertToEnhancedMemory(node));
                }
                return memories;
            } catch (Exception e) {
                throw new CompletionException("Failed to search memories", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> addRelationship(String fromMemoryId, String toMemoryId, 
                                                  String relationshipType, Map<String, Object> properties) {
        return CompletableFuture.runAsync(() -> {
            try (Session session = driver.session()) {
                Map<String, Object> params = new HashMap<>(properties != null ? properties : new HashMap<>());
                params.put("fromId", fromMemoryId);
                params.put("toId", toMemoryId);
                params.put("id", UUID.randomUUID().toString());
                
                StringBuilder cypher = new StringBuilder(
                    "MATCH (from:Memory {id: $fromId}), (to:Memory {id: $toId}) " +
                    "CREATE (from)-[r:" + relationshipType + " {"
                );
                
                List<String> propertyAssignments = new ArrayList<>();
                propertyAssignments.add("id: $id");
                if (properties != null) {
                    for (String key : properties.keySet()) {
                        if (!key.equals("fromId") && !key.equals("toId") && !key.equals("id")) {
                            propertyAssignments.add(key + ": $" + key);
                        }
                    }
                }
                
                cypher.append(String.join(", ", propertyAssignments));
                cypher.append("}]->(to)");
                
                session.run(cypher.toString(), params).consume();
                logger.debug("Added relationship between memories: {} -> {}", fromMemoryId, toMemoryId);
            } catch (Exception e) {
                throw new CompletionException("Failed to add memory relationship", e);
            }
        });
    }
    
    private EnhancedMemory convertToEnhancedMemory(Node node) {
        String id = node.get("id").asString();
        String content = node.get("content").asString();
        String userId = node.get("userId").asString();
        
        // Create the basic EnhancedMemory object
        EnhancedMemory memory = new EnhancedMemory(id, content, userId);
        
        // Set timestamps if available (they should be set automatically in the constructor)
        try {
            if (node.containsKey("createdAt")) {
                // The timestamps are managed internally by EnhancedMemory
                // We can't directly set them from the constructor we're using
            }
            if (node.containsKey("updatedAt")) {
                // Similarly for updatedAt
            }
        } catch (Exception e) {
            logger.warn("Failed to parse timestamps for memory: {}", id, e);
        }
        
        return memory;
    }
}