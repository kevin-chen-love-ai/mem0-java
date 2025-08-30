package com.mem0.graph.impl;

import com.mem0.store.GraphStore;
import com.mem0.store.GraphStore.GraphNode;
import com.mem0.store.GraphStore.GraphRelationship;
import com.mem0.core.EnhancedMemory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * 内存图存储实现，支持增强型内存管理
 * In-memory graph storage implementation with enhanced memory management
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class InMemoryGraphStore implements GraphStore {
    
    private static final Logger logger = Logger.getLogger(InMemoryGraphStore.class.getName());
    
    // Graph storage
    private final Map<String, Map<String, Object>> nodes = new ConcurrentHashMap<>();
    private final Map<String, GraphRelationship> relationships = new ConcurrentHashMap<>();
    
    // Memory-specific storage
    private final Map<String, EnhancedMemory> memories = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> userMemories = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> memoryRelationships = new ConcurrentHashMap<>();
    
    // === Memory-specific methods (expected by tests) ===
    
    /**
     * 添加内存到图存储
     */
    public CompletableFuture<Void> addMemory(EnhancedMemory memory) {
        return CompletableFuture.supplyAsync(() -> {
            if (memory == null) {
                throw new IllegalArgumentException("Memory cannot be null");
            }
            
            String memoryId = memory.getId();
            if (memories.containsKey(memoryId)) {
                throw new IllegalArgumentException("Memory with ID " + memoryId + " already exists");
            }
            
            try {
                memories.put(memoryId, memory);
                
                // Add to user memories index
                String userId = memory.getUserId();
                userMemories.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(memoryId);
                
                logger.log(Level.FINE, "Memory added successfully: " + memoryId);
                return null;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to add memory: " + memoryId, e);
                throw new RuntimeException("Failed to add memory", e);
            }
        });
    }
    
    /**
     * 获取内存
     */
    public CompletableFuture<EnhancedMemory> getMemory(String memoryId) {
        return CompletableFuture.supplyAsync(() -> {
            return memories.get(memoryId);
        });
    }
    
    /**
     * 更新内存
     */
    public CompletableFuture<Void> updateMemory(EnhancedMemory memory) {
        return CompletableFuture.supplyAsync(() -> {
            if (memory == null) {
                throw new IllegalArgumentException("Memory cannot be null");
            }
            
            String memoryId = memory.getId();
            if (!memories.containsKey(memoryId)) {
                throw new IllegalArgumentException("Memory with ID " + memoryId + " does not exist");
            }
            
            try {
                memories.put(memoryId, memory);
                logger.log(Level.FINE, "Memory updated successfully: " + memoryId);
                return null;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to update memory: " + memoryId, e);
                throw new RuntimeException("Failed to update memory", e);
            }
        });
    }
    
    /**
     * 删除内存
     */
    public CompletableFuture<Void> deleteMemory(String memoryId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                EnhancedMemory memory = memories.remove(memoryId);
                if (memory != null) {
                    // Remove from user memories index
                    String userId = memory.getUserId();
                    Set<String> userMems = userMemories.get(userId);
                    if (userMems != null) {
                        userMems.remove(memoryId);
                    }
                    
                    // Remove any relationships involving this memory
                    memoryRelationships.remove(memoryId);
                }
                
                logger.log(Level.FINE, "Memory deleted successfully: " + memoryId);
                return null;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to delete memory: " + memoryId, e);
                throw new RuntimeException("Failed to delete memory", e);
            }
        });
    }
    
    /**
     * 获取用户的所有内存
     */
    public CompletableFuture<List<EnhancedMemory>> getUserMemories(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            Set<String> userMems = userMemories.get(userId);
            if (userMems == null || userMems.isEmpty()) {
                return new ArrayList<>();
            }
            
            return userMems.stream()
                    .map(memories::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        });
    }
    
    /**
     * 获取内存历史记录
     */
    public CompletableFuture<List<EnhancedMemory>> getMemoryHistory(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            Set<String> userMems = userMemories.get(userId);
            if (userMems == null || userMems.isEmpty()) {
                return new ArrayList<>();
            }
            
            return userMems.stream()
                    .map(memories::get)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(EnhancedMemory::getCreatedAt))
                    .collect(Collectors.toList());
        });
    }
    
    /**
     * 搜索内存
     */
    public CompletableFuture<List<EnhancedMemory>> searchMemories(String query, String userId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            if (query == null || query.trim().isEmpty()) {
                return new ArrayList<>();
            }
            
            Set<String> userMems = userMemories.get(userId);
            if (userMems == null || userMems.isEmpty()) {
                return new ArrayList<>();
            }
            
            String lowerQuery = query.toLowerCase();
            return userMems.stream()
                    .map(memories::get)
                    .filter(Objects::nonNull)
                    .filter(memory -> memory.getContent().toLowerCase().contains(lowerQuery))
                    .limit(limit)
                    .collect(Collectors.toList());
        });
    }
    
    /**
     * 添加内存关系
     */
    public CompletableFuture<Void> addRelationship(String fromMemoryId, String toMemoryId, 
                                                  String relationshipType, Map<String, Object> properties) {
        return CompletableFuture.supplyAsync(() -> {
            if (!memories.containsKey(fromMemoryId)) {
                throw new IllegalArgumentException("Source memory " + fromMemoryId + " does not exist");
            }
            if (!memories.containsKey(toMemoryId)) {
                throw new IllegalArgumentException("Target memory " + toMemoryId + " does not exist");
            }
            
            try {
                String relationshipId = "rel_" + System.currentTimeMillis() + "_" + Math.random();
                GraphRelationship relationship = new GraphRelationship(relationshipId, relationshipType,
                                                                      fromMemoryId, toMemoryId, properties);
                relationships.put(relationshipId, relationship);
                
                // Add to memory relationships index
                memoryRelationships.computeIfAbsent(fromMemoryId, k -> ConcurrentHashMap.newKeySet()).add(relationshipId);
                memoryRelationships.computeIfAbsent(toMemoryId, k -> ConcurrentHashMap.newKeySet()).add(relationshipId);
                
                logger.log(Level.FINE, "Memory relationship added: " + relationshipId);
                return null;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to add memory relationship", e);
                throw new RuntimeException("Failed to add memory relationship", e);
            }
        });
    }
    
    // === GraphStore interface implementation ===
    
    @Override
    public CompletableFuture<String> createNode(String label, Map<String, Object> properties) {
        return CompletableFuture.supplyAsync(() -> {
            String nodeId = "node_" + System.currentTimeMillis() + "_" + Math.random();
            Map<String, Object> nodeProps = new HashMap<>(properties);
            nodeProps.put("label", label);
            nodes.put(nodeId, nodeProps);
            return nodeId;
        });
    }
    
    @Override
    public CompletableFuture<String> createRelationship(String sourceNodeId, String targetNodeId, 
                                                       String relationshipType, Map<String, Object> properties) {
        return CompletableFuture.supplyAsync(() -> {
            String relationshipId = "rel_" + System.currentTimeMillis() + "_" + Math.random();
            GraphRelationship relationship = new GraphRelationship(relationshipId, relationshipType, 
                                                                   sourceNodeId, targetNodeId, properties);
            relationships.put(relationshipId, relationship);
            return relationshipId;
        });
    }
    
    @Override
    public CompletableFuture<GraphNode> getNode(String nodeId) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> nodeProps = nodes.get(nodeId);
            if (nodeProps == null) {
                return null;
            }
            String label = (String) nodeProps.get("label");
            List<String> labels = label != null ? Collections.singletonList(label) : Collections.emptyList();
            return new GraphNode(nodeId, labels, nodeProps);
        });
    }
    
    @Override
    public CompletableFuture<List<GraphNode>> getNodesByLabel(String label, Map<String, Object> properties) {
        return CompletableFuture.supplyAsync(() -> {
            List<GraphNode> result = new ArrayList<>();
            for (Map.Entry<String, Map<String, Object>> entry : nodes.entrySet()) {
                Map<String, Object> nodeProps = entry.getValue();
                if (label.equals(nodeProps.get("label"))) {
                    boolean matches = true;
                    if (properties != null) {
                        for (Map.Entry<String, Object> prop : properties.entrySet()) {
                            if (!Objects.equals(nodeProps.get(prop.getKey()), prop.getValue())) {
                                matches = false;
                                break;
                            }
                        }
                    }
                    if (matches) {
                        List<String> labels = Collections.singletonList(label);
                        result.add(new GraphNode(entry.getKey(), labels, nodeProps));
                    }
                }
            }
            return result;
        });
    }
    
    @Override
    public CompletableFuture<List<GraphRelationship>> getRelationships(String nodeId, String relationshipType) {
        return CompletableFuture.supplyAsync(() -> {
            List<GraphRelationship> result = new ArrayList<>();
            for (GraphRelationship rel : relationships.values()) {
                if ((rel.getSourceNodeId().equals(nodeId) || rel.getTargetNodeId().equals(nodeId)) &&
                    (relationshipType == null || relationshipType.equals(rel.getType()))) {
                    result.add(rel);
                }
            }
            return result;
        });
    }
    
    @Override
    public CompletableFuture<List<GraphNode>> findConnectedNodes(String nodeId, String relationshipType, int maxHops) {
        return CompletableFuture.supplyAsync(() -> {
            // Simplified implementation - just direct connections
            List<GraphNode> result = new ArrayList<>();
            for (GraphRelationship rel : relationships.values()) {
                String connectedNodeId = null;
                if (rel.getSourceNodeId().equals(nodeId) && (relationshipType == null || relationshipType.equals(rel.getType()))) {
                    connectedNodeId = rel.getTargetNodeId();
                } else if (rel.getTargetNodeId().equals(nodeId) && (relationshipType == null || relationshipType.equals(rel.getType()))) {
                    connectedNodeId = rel.getSourceNodeId();
                }
                
                if (connectedNodeId != null) {
                    GraphNode node = getNode(connectedNodeId).join();
                    if (node != null) {
                        result.add(node);
                    }
                }
            }
            return result;
        });
    }
    
    @Override
    public CompletableFuture<Void> updateNode(String nodeId, Map<String, Object> properties) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> existingProps = nodes.get(nodeId);
            if (existingProps != null) {
                existingProps.putAll(properties);
            }
            return null;
        });
    }
    
    @Override
    public CompletableFuture<Void> updateRelationship(String relationshipId, Map<String, Object> properties) {
        return CompletableFuture.supplyAsync(() -> {
            GraphRelationship rel = relationships.get(relationshipId);
            if (rel != null) {
                rel.getProperties().putAll(properties);
            }
            return null;
        });
    }
    
    @Override
    public CompletableFuture<Void> deleteNode(String nodeId) {
        return CompletableFuture.supplyAsync(() -> {
            nodes.remove(nodeId);
            // Also remove related relationships
            relationships.entrySet().removeIf(entry -> {
                GraphRelationship rel = entry.getValue();
                return rel.getSourceNodeId().equals(nodeId) || rel.getTargetNodeId().equals(nodeId);
            });
            return null;
        });
    }
    
    @Override
    public CompletableFuture<Void> deleteRelationship(String relationshipId) {
        return CompletableFuture.supplyAsync(() -> {
            relationships.remove(relationshipId);
            return null;
        });
    }
    
    @Override
    public CompletableFuture<List<Map<String, Object>>> executeQuery(String cypher, Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> {
            // Simplified implementation
            return Collections.emptyList();
        });
    }
    
    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                nodes.clear();
                relationships.clear();
                memories.clear();
                userMemories.clear();
                memoryRelationships.clear();
                logger.log(Level.INFO, "InMemoryGraphStore closed successfully");
                return null;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error closing InMemoryGraphStore", e);
                throw new RuntimeException("Failed to close graph store", e);
            }
        });
    }
}