package com.mem0.graph.impl;

import com.mem0.store.GraphStore;
import com.mem0.store.GraphStore.GraphNode;
import com.mem0.store.GraphStore.GraphRelationship;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 高性能图数据库存储实现
 * 支持并发访问、缓存优化、批量操作、图遍历算法优化
 */
public class HighPerformanceGraphStore implements GraphStore {
    
    // 主存储
    private final Map<String, MemoryNode> nodes = new ConcurrentHashMap<>();
    private final Map<String, Relationship> relationships = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> userNodes = new ConcurrentHashMap<>();
    
    // 简化版本缓存
    private final Map<String, Map<String, Object>> nodeCache = new ConcurrentHashMap<>();
    private final Map<String, List<String>> relationshipCache = new ConcurrentHashMap<>();
    
    // 索引结构
    private final Map<String, Map<Object, Set<String>>> propertyIndex = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> relationshipTypeIndex = new ConcurrentHashMap<>();
    
    // ID生成器
    private final AtomicLong relationshipIdCounter = new AtomicLong(0);
    
    // 统计信息
    private volatile long totalNodeOperations = 0;
    private volatile long totalRelationshipOperations = 0;
    private volatile long totalQueries = 0;
    
    public HighPerformanceGraphStore() {
        // 简化版本构造器
        System.out.println("高性能GraphStore初始化完成，启用缓存和并发优化");
    }
    
    /**
     * 内存节点类
     */
    private static class MemoryNode {
        final String id;
        final Map<String, Object> properties;
        final Set<String> incomingRelationships;
        final Set<String> outgoingRelationships;
        final long createdTime;
        volatile long lastAccessTime;
        
        MemoryNode(String id, Map<String, Object> properties) {
            this.id = id;
            this.properties = new ConcurrentHashMap<>(properties);
            this.incomingRelationships = ConcurrentHashMap.newKeySet();
            this.outgoingRelationships = ConcurrentHashMap.newKeySet();
            this.createdTime = System.currentTimeMillis();
            this.lastAccessTime = this.createdTime;
        }
        
        void updateAccess() {
            this.lastAccessTime = System.currentTimeMillis();
        }
    }
    
    /**
     * 关系类
     */
    private static class Relationship {
        final String id;
        final String fromNodeId;
        final String toNodeId;
        final String type;
        final Map<String, Object> properties;
        final long createdTime;
        volatile long lastAccessTime;
        
        Relationship(String id, String fromNodeId, String toNodeId, String type, Map<String, Object> properties) {
            this.id = id;
            this.fromNodeId = fromNodeId;
            this.toNodeId = toNodeId;
            this.type = type;
            this.properties = new ConcurrentHashMap<>(properties);
            this.createdTime = System.currentTimeMillis();
            this.lastAccessTime = this.createdTime;
        }
        
        void updateAccess() {
            this.lastAccessTime = System.currentTimeMillis();
        }
    }
    
    @Override
    public CompletableFuture<String> createNode(String label, Map<String, Object> properties) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String nodeId = "node_" + System.currentTimeMillis() + "_" + Math.random();
                
                MemoryNode node = new MemoryNode(nodeId, properties);
                nodes.put(nodeId, node);
                
                // 更新用户索引
                String userId = (String) properties.get("userId");
                if (userId != null) {
                    userNodes.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(nodeId);
                }
                
                // 更新属性索引
                updatePropertyIndex(nodeId, properties);
                
                // 清理缓存
                invalidateNodeCache(nodeId);
                
                totalNodeOperations++;
                return nodeId;
            } catch (Exception e) {
                throw new RuntimeException("创建节点失败", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> updateNode(String nodeId, Map<String, Object> properties) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MemoryNode existingNode = nodes.get(nodeId);
                if (existingNode == null) {
                    throw new RuntimeException("节点不存在: " + nodeId);
                }
                
                // 更新属性（保留现有属性）
                Map<String, Object> oldProperties = new HashMap<>(existingNode.properties);
                existingNode.properties.putAll(properties);
                existingNode.updateAccess();
                
                // 更新属性索引
                removeFromPropertyIndex(nodeId, oldProperties);
                updatePropertyIndex(nodeId, existingNode.properties);
                
                // 清理缓存
                invalidateNodeCache(nodeId);
                
                totalNodeOperations++;
                return null;
            } catch (Exception e) {
                throw new RuntimeException("更新节点失败", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> deleteNode(String nodeId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MemoryNode node = nodes.remove(nodeId);
                if (node != null) {
                    // 从用户索引中移除
                    String userId = (String) node.properties.get("userId");
                    if (userId != null) {
                        Set<String> userNodeSet = userNodes.get(userId);
                        if (userNodeSet != null) {
                            userNodeSet.remove(nodeId);
                            if (userNodeSet.isEmpty()) {
                                userNodes.remove(userId);
                            }
                        }
                    }
                    
                    // 删除所有相关关系
                    Set<String> allRelationships = new HashSet<>();
                    allRelationships.addAll(node.incomingRelationships);
                    allRelationships.addAll(node.outgoingRelationships);
                    
                    for (String relId : allRelationships) {
                        deleteRelationshipInternal(relId);
                    }
                    
                    // 从属性索引中移除
                    removeFromPropertyIndex(nodeId, node.properties);
                    
                    // 清理缓存
                    invalidateNodeCache(nodeId);
                    
                    totalNodeOperations++;
                }
                return null;
            } catch (Exception e) {
                throw new RuntimeException("删除节点失败", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<String> createRelationship(String sourceNodeId, String targetNodeId, 
                                                       String relationshipType, Map<String, Object> properties) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MemoryNode fromNode = nodes.get(sourceNodeId);
                MemoryNode toNode = nodes.get(targetNodeId);
                
                if (fromNode == null) {
                    throw new RuntimeException("源节点不存在: " + sourceNodeId);
                }
                if (toNode == null) {
                    throw new RuntimeException("目标节点不存在: " + targetNodeId);
                }
                
                String relationshipId = "rel_" + relationshipIdCounter.incrementAndGet();
                Relationship relationship = new Relationship(relationshipId, sourceNodeId, targetNodeId, relationshipType, properties);
                
                relationships.put(relationshipId, relationship);
                fromNode.outgoingRelationships.add(relationshipId);
                toNode.incomingRelationships.add(relationshipId);
                
                // 更新关系类型索引
                relationshipTypeIndex.computeIfAbsent(relationshipType, k -> ConcurrentHashMap.newKeySet()).add(relationshipId);
                
                // 清理缓存
                invalidateRelationshipCache(sourceNodeId, targetNodeId);
                
                totalRelationshipOperations++;
                return relationshipId;
            } catch (Exception e) {
                throw new RuntimeException("创建关系失败", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<GraphRelationship>> getRelationships(String nodeId, String relationshipType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                totalQueries++;
                
                MemoryNode node = nodes.get(nodeId);
                if (node == null) {
                    return Collections.emptyList();
                }
                
                node.updateAccess();
                List<GraphRelationship> result = new ArrayList<>();
                
                // 检查出向关系
                for (String relId : node.outgoingRelationships) {
                    Relationship rel = relationships.get(relId);
                    if (rel != null && (relationshipType == null || relationshipType.equals(rel.type))) {
                        rel.updateAccess();
                        result.add(new GraphRelationship(rel.id, rel.type, rel.fromNodeId, rel.toNodeId, rel.properties));
                    }
                }
                
                // 检查入向关系
                for (String relId : node.incomingRelationships) {
                    Relationship rel = relationships.get(relId);
                    if (rel != null && (relationshipType == null || relationshipType.equals(rel.type))) {
                        rel.updateAccess();
                        result.add(new GraphRelationship(rel.id, rel.type, rel.fromNodeId, rel.toNodeId, rel.properties));
                    }
                }
                
                return result;
            } catch (Exception e) {
                throw new RuntimeException("查询关系失败", e);
            }
        });
    }
    
    // Helper method for relationship count by user (not in interface)
    public CompletableFuture<Long> getRelationshipCount(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Set<String> userNodeIds = userNodes.getOrDefault(userId, Collections.emptySet());
                long count = 0;
                
                for (Relationship rel : relationships.values()) {
                    if (userNodeIds.contains(rel.fromNodeId) || userNodeIds.contains(rel.toNodeId)) {
                        count++;
                    }
                }
                
                return count;
            } catch (Exception e) {
                throw new RuntimeException("获取用户关系数量失败", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<GraphNode> getNode(String nodeId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MemoryNode node = nodes.get(nodeId);
                if (node == null) {
                    return null;
                }
                
                node.updateAccess();
                String label = (String) node.properties.get("label");
                List<String> labels = label != null ? Collections.singletonList(label) : Collections.emptyList();
                
                return new GraphNode(nodeId, labels, new HashMap<>(node.properties));
            } catch (Exception e) {
                throw new RuntimeException("获取节点失败", e);
            }
        });
    }
    
    // Helper method (not in interface)
    public CompletableFuture<List<Map<String, Object>>> findMemoriesByProperty(String propertyName, Object propertyValue, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Set<String> candidateIds = getNodeIdsByProperty(propertyName, propertyValue);
                Set<String> userNodeIds = userNodes.getOrDefault(userId, Collections.emptySet());
                
                List<Map<String, Object>> results = new ArrayList<>();
                
                for (String nodeId : candidateIds) {
                    if (userNodeIds.contains(nodeId)) {
                        MemoryNode node = nodes.get(nodeId);
                        if (node != null) {
                            node.updateAccess();
                            Map<String, Object> nodeData = new HashMap<>(node.properties);
                            nodeData.put("id", nodeId);
                            results.add(nodeData);
                        }
                    }
                }
                
                return results;
            } catch (Exception e) {
                throw new RuntimeException("根据属性查找内存失赅", e);
            }
        });
    }
    
    // Helper method (not in interface)
    public CompletableFuture<List<String>> getMemoriesByType(String memoryType, String userId) {
        return findMemoriesByProperty("type", memoryType, userId)
            .thenApply(results -> results.stream()
                .map(result -> (String) result.get("id"))
                .collect(Collectors.toList()));
    }
    
    @Override
    public CompletableFuture<List<GraphNode>> getNodesByLabel(String label, Map<String, Object> properties) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("Getting nodes by label: " + label + " with properties: " + properties);
                
                List<GraphNode> results = new ArrayList<>();
                
                for (Map.Entry<String, MemoryNode> entry : nodes.entrySet()) {
                    MemoryNode node = entry.getValue();
                    
                    // Check if label matches (stored as "label" property)
                    if (!label.equals(node.properties.get("label"))) {
                        continue;
                    }
                    
                    // Check if all specified properties match
                    boolean matches = true;
                    if (properties != null) {
                        for (Map.Entry<String, Object> prop : properties.entrySet()) {
                            if (!Objects.equals(node.properties.get(prop.getKey()), prop.getValue())) {
                                matches = false;
                                break;
                            }
                        }
                    }
                    
                    if (matches) {
                        List<String> labels = Collections.singletonList(label);
                        GraphNode graphNode = new GraphNode(entry.getKey(), labels, new HashMap<>(node.properties));
                        results.add(graphNode);
                    }
                }
                
                return results;
            } catch (Exception e) {
                System.err.println("Failed to get nodes by label: " + label + ", error: " + e.getMessage());
                throw new RuntimeException("Failed to get nodes by label", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<GraphNode>> findConnectedNodes(String nodeId, String relationshipType, int maxDepth) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("Finding connected nodes from: " + nodeId + " with relationship: " + relationshipType + " maxDepth: " + maxDepth);
                
                Set<String> visited = new HashSet<>();
                List<GraphNode> results = new ArrayList<>();
                
                findConnectedNodesRecursive(nodeId, relationshipType, maxDepth, 0, visited, results);
                
                return results;
            } catch (Exception e) {
                System.err.println("Failed to find connected nodes from: " + nodeId + ", error: " + e.getMessage());
                throw new RuntimeException("Failed to find connected nodes", e);
            }
        });
    }
    
    private void findConnectedNodesRecursive(String nodeId, String relationshipType, int maxDepth, int currentDepth, 
                                           Set<String> visited, List<GraphNode> results) {
        if (currentDepth >= maxDepth || visited.contains(nodeId)) {
            return;
        }
        
        visited.add(nodeId);
        
        // Find all relationships from this node
        for (Relationship rel : relationships.values()) {
            String connectedNodeId = null;
            
            if (rel.fromNodeId.equals(nodeId) && (relationshipType == null || relationshipType.equals(rel.type))) {
                connectedNodeId = rel.toNodeId;
            } else if (rel.toNodeId.equals(nodeId) && (relationshipType == null || relationshipType.equals(rel.type))) {
                connectedNodeId = rel.fromNodeId;
            }
            
            if (connectedNodeId != null && !visited.contains(connectedNodeId)) {
                MemoryNode node = nodes.get(connectedNodeId);
                if (node != null) {
                    String label = (String) node.properties.get("label");
                    List<String> labels = Collections.singletonList(label != null ? label : "Node");
                    GraphNode graphNode = new GraphNode(connectedNodeId, labels, new HashMap<>(node.properties));
                    results.add(graphNode);
                    
                    // Recursive call for next depth level
                    findConnectedNodesRecursive(connectedNodeId, relationshipType, maxDepth, currentDepth + 1, visited, results);
                }
            }
        }
    }
    
    /**
     * 批量创建节点
     */
    public CompletableFuture<Void> addMemoryNodesBatch(Map<String, Map<String, Object>> nodes) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("开始批量添加 " + nodes.size() + " 个节点");
            long startTime = System.currentTimeMillis();
            
            // 并发处理
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (Map.Entry<String, Map<String, Object>> entry : nodes.entrySet()) {
                CompletableFuture<Void> future = createNode("Node", entry.getValue()).thenApply(nodeId -> null);
                futures.add(future);
            }
            
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("批量添加节点完成，耗时 " + duration + "ms");
            return null;
        });
    }
    
    /**
     * 图遍历 - 深度优先搜索
     */
    public CompletableFuture<List<String>> depthFirstTraversal(String startNodeId, String relationshipType, int maxDepth) {
        return CompletableFuture.supplyAsync(() -> {
            Set<String> visited = new HashSet<>();
            List<String> result = new ArrayList<>();
            
            dfsInternal(startNodeId, relationshipType, maxDepth, 0, visited, result);
            
            return result;
        });
    }
    
    /**
     * 图遍历 - 广度优先搜索
     */
    public CompletableFuture<List<String>> breadthFirstTraversal(String startNodeId, String relationshipType, int maxDepth) {
        return CompletableFuture.supplyAsync(() -> {
            Set<String> visited = new HashSet<>();
            List<String> result = new ArrayList<>();
            Queue<TraversalNode> queue = new LinkedList<>();
            
            queue.offer(new TraversalNode(startNodeId, 0));
            visited.add(startNodeId);
            
            while (!queue.isEmpty()) {
                TraversalNode current = queue.poll();
                result.add(current.nodeId);
                
                if (current.depth < maxDepth) {
                    List<GraphRelationship> relationships = getRelationships(current.nodeId, relationshipType).join();
                    List<String> related = new ArrayList<>();
                    for (GraphRelationship rel : relationships) {
                        if (rel.getSourceNodeId().equals(current.nodeId)) {
                            related.add(rel.getTargetNodeId());
                        } else {
                            related.add(rel.getSourceNodeId());
                        }
                    }
                    for (String relatedId : related) {
                        if (!visited.contains(relatedId)) {
                            visited.add(relatedId);
                            queue.offer(new TraversalNode(relatedId, current.depth + 1));
                        }
                    }
                }
            }
            
            return result;
        });
    }
    
    /**
     * 获取性能统计
     */
    public GraphStoreStats getStats() {
        return new GraphStoreStats(
            nodes.size(),
            relationships.size(),
            userNodes.size(),
            totalNodeOperations,
            totalRelationshipOperations,
            totalQueries
        );
    }
    
    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("关闭高性能图存储");
            
            nodeCache.clear();
            relationshipCache.clear();
            nodes.clear();
            relationships.clear();
            userNodes.clear();
            propertyIndex.clear();
            relationshipTypeIndex.clear();
            
            System.out.println("图存储关闭完成");
            return null;
        });
    }
    
    // Add missing interface methods
    
    @Override
    public CompletableFuture<List<Map<String, Object>>> executeQuery(String cypher, Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> {
            // Simple implementation - not a full Cypher parser
            throw new UnsupportedOperationException("不支持Cypher查询");
        });
    }
    
    @Override
    public CompletableFuture<Void> updateRelationship(String relationshipId, Map<String, Object> properties) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Relationship rel = relationships.get(relationshipId);
                if (rel == null) {
                    throw new RuntimeException("关系不存在: " + relationshipId);
                }
                
                rel.properties.putAll(properties);
                rel.updateAccess();
                totalRelationshipOperations++;
                
                return null;
            } catch (Exception e) {
                throw new RuntimeException("更新关系失败", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> deleteRelationship(String relationshipId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                deleteRelationshipInternal(relationshipId);
                return null;
            } catch (Exception e) {
                throw new RuntimeException("删除关系失赅", e);
            }
        });
    }

    // Additional helper methods (not in interface, remove @Override)
    public CompletableFuture<List<GraphNode>> findNodes(String label, Map<String, Object> properties) {
        return getNodesByLabel(label, properties);
    }
    
    public CompletableFuture<List<GraphRelationship>> findRelationships(String type, Map<String, Object> properties) {
        return CompletableFuture.supplyAsync(() -> {
            List<GraphRelationship> result = new ArrayList<>();
            for (Relationship rel : relationships.values()) {
                if (type.equals(rel.type)) {
                    boolean matches = true;
                    if (properties != null) {
                        for (Map.Entry<String, Object> prop : properties.entrySet()) {
                            if (!Objects.equals(rel.properties.get(prop.getKey()), prop.getValue())) {
                                matches = false;
                                break;
                            }
                        }
                    }
                    if (matches) {
                        result.add(new GraphRelationship(rel.id, rel.type, rel.fromNodeId, rel.toNodeId, rel.properties));
                    }
                }
            }
            return result;
        });
    }
    
    public CompletableFuture<GraphRelationship> getRelationshipById(String id) {
        return CompletableFuture.supplyAsync(() -> {
            Relationship rel = relationships.get(id);
            if (rel == null) {
                return null;
            }
            return new GraphRelationship(rel.id, rel.type, rel.fromNodeId, rel.toNodeId, rel.properties);
        });
    }
    
    public CompletableFuture<Void> deleteAll() {
        return CompletableFuture.supplyAsync(() -> {
            nodes.clear();
            relationships.clear();
            userNodes.clear();
            propertyIndex.clear();
            relationshipTypeIndex.clear();
            nodeCache.clear();
            relationshipCache.clear();
            return null;
        });
    }
    
    public CompletableFuture<Long> countNodes() {
        return CompletableFuture.completedFuture((long) nodes.size());
    }
    
    public CompletableFuture<Long> countRelationships() {
        return CompletableFuture.completedFuture((long) relationships.size());
    }
    
    // 私有辅助方法
    
    private void updatePropertyIndex(String nodeId, Map<String, Object> properties) {
        synchronized (propertyIndex) {
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                String propName = entry.getKey();
                Object propValue = entry.getValue();
                
                propertyIndex.computeIfAbsent(propName, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(propValue, k -> ConcurrentHashMap.newKeySet())
                    .add(nodeId);
            }
        }
    }
    
    private void removeFromPropertyIndex(String nodeId, Map<String, Object> properties) {
        synchronized (propertyIndex) {
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                String propName = entry.getKey();
                Object propValue = entry.getValue();
                
                Map<Object, Set<String>> propMap = propertyIndex.get(propName);
                if (propMap != null) {
                    Set<String> nodeSet = propMap.get(propValue);
                    if (nodeSet != null) {
                        nodeSet.remove(nodeId);
                        if (nodeSet.isEmpty()) {
                            propMap.remove(propValue);
                            if (propMap.isEmpty()) {
                                propertyIndex.remove(propName);
                            }
                        }
                    }
                }
            }
        }
    }
    
    private Set<String> getNodeIdsByProperty(String propertyName, Object propertyValue) {
        synchronized (propertyIndex) {
            Map<Object, Set<String>> propMap = propertyIndex.get(propertyName);
            if (propMap != null) {
                Set<String> nodeSet = propMap.get(propertyValue);
                return nodeSet != null ? new HashSet<>(nodeSet) : Collections.emptySet();
            }
            return Collections.emptySet();
        }
    }
    
    private void deleteRelationshipInternal(String relationshipId) {
        Relationship rel = relationships.remove(relationshipId);
        if (rel != null) {
            // 从关系类型索引中移除
            Set<String> typeSet = relationshipTypeIndex.get(rel.type);
            if (typeSet != null) {
                typeSet.remove(relationshipId);
                if (typeSet.isEmpty()) {
                    relationshipTypeIndex.remove(rel.type);
                }
            }
            
            totalRelationshipOperations++;
        }
    }
    
    private void invalidateNodeCache(String nodeId) {
        nodeCache.remove(nodeId);
    }
    
    private void invalidateRelationshipCache(String fromNodeId, String toNodeId) {
        // 简化的缓存失效策略
        relationshipCache.clear(); // 在实际应用中可以更精确地失效相关缓存
    }
    
    private void dfsInternal(String nodeId, String relationshipType, int maxDepth, int currentDepth, 
                           Set<String> visited, List<String> result) {
        if (currentDepth > maxDepth || visited.contains(nodeId)) {
            return;
        }
        
        visited.add(nodeId);
        result.add(nodeId);
        
        if (currentDepth < maxDepth) {
            List<GraphRelationship> relationships = getRelationships(nodeId, relationshipType).join();
            List<String> related = new ArrayList<>();
            for (GraphRelationship rel : relationships) {
                if (rel.getSourceNodeId().equals(nodeId)) {
                    related.add(rel.getTargetNodeId());
                } else {
                    related.add(rel.getSourceNodeId());
                }
            }
            for (String relatedId : related) {
                dfsInternal(relatedId, relationshipType, maxDepth, currentDepth + 1, visited, result);
            }
        }
    }
    
    // 辅助类
    
    private static class TraversalNode {
        final String nodeId;
        final int depth;
        
        TraversalNode(String nodeId, int depth) {
            this.nodeId = nodeId;
            this.depth = depth;
        }
    }
    
    // Simple stats without external dependencies
    public static class GraphStoreStats {
        private final int totalNodes;
        private final int totalRelationships;
        private final int totalUsers;
        private final long totalNodeOperations;
        private final long totalRelationshipOperations;
        private final long totalQueries;
        
        public GraphStoreStats(int totalNodes, int totalRelationships, int totalUsers,
                             long totalNodeOperations, long totalRelationshipOperations, long totalQueries) {
            this.totalNodes = totalNodes;
            this.totalRelationships = totalRelationships;
            this.totalUsers = totalUsers;
            this.totalNodeOperations = totalNodeOperations;
            this.totalRelationshipOperations = totalRelationshipOperations;
            this.totalQueries = totalQueries;
        }
        
        // Getter方法
        public int getTotalNodes() { return totalNodes; }
        public int getTotalRelationships() { return totalRelationships; }
        public int getTotalUsers() { return totalUsers; }
        public long getTotalNodeOperations() { return totalNodeOperations; }
        public long getTotalRelationshipOperations() { return totalRelationshipOperations; }
        public long getTotalQueries() { return totalQueries; }
        
        @Override
        public String toString() {
            return String.format("GraphStoreStats{节点=%d, 关系=%d, 用户=%d, 节点操作=%d, 关系操作=%d, 查询=%d}",
                totalNodes, totalRelationships, totalUsers, totalNodeOperations, totalRelationshipOperations, totalQueries);
        }
    }
}