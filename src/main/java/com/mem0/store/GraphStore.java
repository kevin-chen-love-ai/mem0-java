package com.mem0.store;

import com.mem0.core.EnhancedMemory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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