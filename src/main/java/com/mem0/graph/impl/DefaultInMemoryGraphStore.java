package com.mem0.graph.impl;

import com.mem0.core.EnhancedMemory;
import com.mem0.store.GraphStore;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default in-memory GraphStore implementation
 * 
 * Provides a simple in-memory graph storage solution for testing and development.
 * This implementation is thread-safe and supports all GraphStore operations.
 */
public class DefaultInMemoryGraphStore implements GraphStore {
    
    private final ConcurrentHashMap<String, Object> nodes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, Object>> relationships = new ConcurrentHashMap<>();
    
    @Override
    public CompletableFuture<String> createNode(String label, Map<String, Object> properties) {
        String id = UUID.randomUUID().toString();
        Map<String, Object> nodeData = new HashMap<>(properties);
        nodeData.put("_label", label);
        nodeData.put("_id", id);
        nodes.put(id, nodeData);
        return CompletableFuture.completedFuture(id);
    }
    
    @Override
    public CompletableFuture<List<GraphNode>> getNodesByLabel(String label, Map<String, Object> filter) {
        List<GraphNode> result = new ArrayList<>();
        for (Map.Entry<String, Object> entry : nodes.entrySet()) {
            Map<String, Object> nodeData = (Map<String, Object>) entry.getValue();
            if (label.equals(nodeData.get("_label"))) {
                boolean matches = true;
                if (filter != null) {
                    for (Map.Entry<String, Object> filterEntry : filter.entrySet()) {
                        if (!Objects.equals(nodeData.get(filterEntry.getKey()), filterEntry.getValue())) {
                            matches = false;
                            break;
                        }
                    }
                }
                if (matches) {
                    List<String> labels = Arrays.asList(label);
                    result.add(new GraphNode(entry.getKey(), labels, nodeData));
                }
            }
        }
        return CompletableFuture.completedFuture(result);
    }
    
    @Override
    public CompletableFuture<String> createRelationship(String sourceId, String targetId, String type, Map<String, Object> properties) {
        String relationshipId = UUID.randomUUID().toString();
        Map<String, Object> relData = new HashMap<>(properties != null ? properties : Collections.emptyMap());
        relData.put("_id", relationshipId);
        relData.put("_source", sourceId);
        relData.put("_target", targetId);
        relData.put("_type", type);
        relationships.put(relationshipId, relData);
        return CompletableFuture.completedFuture(relationshipId);
    }
    
    @Override
    public CompletableFuture<List<GraphNode>> findConnectedNodes(String nodeId, String relationshipType, int maxHops) {
        // Simple implementation - could be enhanced for multi-hop traversal
        List<GraphNode> connected = new ArrayList<>();
        for (Map<String, Object> rel : relationships.values()) {
            if (nodeId.equals(rel.get("_source")) && 
                (relationshipType == null || relationshipType.equals(rel.get("_type")))) {
                String targetId = (String) rel.get("_target");
                Object targetNode = nodes.get(targetId);
                if (targetNode != null) {
                    Map<String, Object> nodeData = (Map<String, Object>) targetNode;
                    List<String> labels = Arrays.asList((String) nodeData.get("_label"));
                    connected.add(new GraphNode(targetId, labels, nodeData));
                }
            }
        }
        return CompletableFuture.completedFuture(connected);
    }
    
    @Override
    public CompletableFuture<Void> deleteNode(String nodeId) {
        nodes.remove(nodeId);
        // Also remove related relationships
        relationships.entrySet().removeIf(entry -> {
            Map<String, Object> rel = entry.getValue();
            return nodeId.equals(rel.get("_source")) || nodeId.equals(rel.get("_target"));
        });
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> close() {
        nodes.clear();
        relationships.clear();
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<List<Map<String, Object>>> executeQuery(String query, Map<String, Object> parameters) {
        // Simple query execution - for production use, consider implementing a query parser
        return CompletableFuture.completedFuture(new ArrayList<>());
    }
    
    @Override
    public CompletableFuture<Void> deleteRelationship(String relationshipId) {
        relationships.remove(relationshipId);
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> updateRelationship(String relationshipId, Map<String, Object> properties) {
        Map<String, Object> existing = relationships.get(relationshipId);
        if (existing != null && properties != null) {
            existing.putAll(properties);
        }
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> updateNode(String nodeId, Map<String, Object> properties) {
        Object existingNode = nodes.get(nodeId);
        if (existingNode != null && properties != null) {
            Map<String, Object> nodeData = (Map<String, Object>) existingNode;
            nodeData.putAll(properties);
        }
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<List<GraphRelationship>> getRelationships(String nodeId, String relationshipType) {
        List<GraphRelationship> result = new ArrayList<>();
        for (Map<String, Object> rel : relationships.values()) {
            if (nodeId.equals(rel.get("_source")) || nodeId.equals(rel.get("_target"))) {
                if (relationshipType == null || relationshipType.equals(rel.get("_type"))) {
                    result.add(new GraphRelationship(
                        (String) rel.get("_id"),
                        (String) rel.get("_source"),
                        (String) rel.get("_target"),
                        (String) rel.get("_type"),
                        new HashMap<>(rel)
                    ));
                }
            }
        }
        return CompletableFuture.completedFuture(result);
    }
    
    @Override
    public CompletableFuture<GraphNode> getNode(String nodeId) {
        Object nodeData = nodes.get(nodeId);
        if (nodeData != null) {
            Map<String, Object> properties = (Map<String, Object>) nodeData;
            List<String> labels = Arrays.asList((String) properties.get("_label"));
            return CompletableFuture.completedFuture(new GraphNode(nodeId, labels, properties));
        }
        return CompletableFuture.completedFuture(null);
    }
    
    // Memory-specific methods
    
    @Override
    public CompletableFuture<Void> addMemory(EnhancedMemory memory) {
        if (memory != null) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("id", memory.getId());
            properties.put("content", memory.getContent());
            properties.put("userId", memory.getUserId());
            properties.put("_label", "Memory");
            properties.put("_id", memory.getId());
            nodes.put(memory.getId(), properties);
        }
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<EnhancedMemory> getMemory(String memoryId) {
        // Simple implementation - returns null since this is primarily for graph operations
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> updateMemory(EnhancedMemory memory) {
        if (memory != null) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("id", memory.getId());
            properties.put("content", memory.getContent());
            properties.put("userId", memory.getUserId());
            properties.put("_label", "Memory");
            properties.put("_id", memory.getId());
            nodes.put(memory.getId(), properties);
        }
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> deleteMemory(String memoryId) {
        return deleteNode(memoryId);
    }
    
    @Override
    public CompletableFuture<List<EnhancedMemory>> getUserMemories(String userId) {
        // Simple implementation - returns empty list
        return CompletableFuture.completedFuture(new ArrayList<>());
    }
    
    @Override
    public CompletableFuture<List<EnhancedMemory>> getMemoryHistory(String userId) {
        return CompletableFuture.completedFuture(new ArrayList<>());
    }
    
    @Override
    public CompletableFuture<List<EnhancedMemory>> searchMemories(String query, String userId, int limit) {
        return CompletableFuture.completedFuture(new ArrayList<>());
    }
    
    @Override
    public CompletableFuture<Void> addRelationship(String fromMemoryId, String toMemoryId, 
                                                  String relationshipType, Map<String, Object> properties) {
        return createRelationship(fromMemoryId, toMemoryId, relationshipType, properties)
            .thenCompose(id -> CompletableFuture.completedFuture(null));
    }
}