package com.mem0.graph.impl;

import com.mem0.store.GraphStore;
import com.mem0.store.GraphStore.GraphNode;
import com.mem0.store.GraphStore.GraphRelationship;
import com.mem0.core.EnhancedMemory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 内存图存储实现，支持增强型内存管理
 * In-memory graph storage implementation with enhanced memory management
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class InMemoryGraphStore implements GraphStore {
    
    private static final Logger logger = LoggerFactory.getLogger(InMemoryGraphStore.class);
    
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
                userMemories.compute(userId, (k, v) -> {
                    Set<String> userMems = v;
                    if (userMems == null) {
                        userMems = ConcurrentHashMap.newKeySet();
                    }
                    userMems.add(memoryId);
                    return userMems;
                });
                
                logger.debug("Memory added successfully: {}", memoryId);
                return null;
            } catch (Exception e) {
                logger.error("Failed to add memory: {}", memoryId, e);
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
                logger.debug("Memory updated successfully: {}", memoryId);
                return null;
            } catch (Exception e) {
                logger.error("Failed to update memory: {}", memoryId, e);
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
                
                logger.debug("Memory deleted successfully: {}", memoryId);
                return null;
            } catch (Exception e) {
                logger.error("Failed to delete memory: {}", memoryId, e);
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
                memoryRelationships.compute(fromMemoryId, (k, v) -> {
                    Set<String> rels = v;
                    if (rels == null) {
                        rels = ConcurrentHashMap.newKeySet();
                    }
                    rels.add(relationshipId);
                    return rels;
                });
                memoryRelationships.compute(toMemoryId, (k, v) -> {
                    Set<String> rels = v;
                    if (rels == null) {
                        rels = ConcurrentHashMap.newKeySet();
                    }
                    rels.add(relationshipId);
                    return rels;
                });
                
                logger.debug("Memory relationship added: {}", relationshipId);
                return null;
            } catch (Exception e) {
                logger.error("Failed to add memory relationship", e);
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
            try {
                if (cypher == null || cypher.trim().isEmpty()) {
                    logger.warn("Empty Cypher query received, returning empty result");
                    return Collections.emptyList();
                }
                
                String normalizedQuery = cypher.trim().toUpperCase();
                Map<String, Object> params = parameters != null ? parameters : new HashMap<>();
                
                // Basic Cypher query parsing and execution
                if (normalizedQuery.startsWith("MATCH")) {
                    return executeMatchQuery(cypher, params);
                } else if (normalizedQuery.startsWith("CREATE")) {
                    return executeCreateQuery(cypher, params);
                } else if (normalizedQuery.startsWith("DELETE")) {
                    return executeDeleteQuery(cypher, params);
                } else if (normalizedQuery.startsWith("RETURN")) {
                    return executeReturnQuery(cypher, params);
                } else {
                    logger.warn("Unsupported Cypher query type: {}", cypher);
                    return Collections.emptyList();
                }
            } catch (Exception e) {
                logger.error("Error executing Cypher query: {}", cypher, e);
                throw new RuntimeException("Failed to execute Cypher query", e);
            }
        });
    }
    
    private List<Map<String, Object>> executeMatchQuery(String cypher, Map<String, Object> parameters) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try {
            // Simple MATCH pattern parsing
            // Support basic patterns like: MATCH (n) RETURN n
            // or MATCH (n:Label) WHERE n.property = value RETURN n
            
            if (cypher.contains("RETURN")) {
                String[] parts = cypher.split("RETURN");
                String matchPart = parts[0].trim();
                String returnPart = parts[1].trim();
                
                // Parse node label from match part
                String targetLabel = extractLabelFromMatch(matchPart);
                Map<String, Object> whereConditions = extractWhereConditions(cypher, parameters);
                
                // Find matching nodes
                for (Map.Entry<String, Map<String, Object>> entry : nodes.entrySet()) {
                    String nodeId = entry.getKey();
                    Map<String, Object> nodeProps = entry.getValue();
                    
                    // Check label match
                    if (targetLabel != null && !targetLabel.equals(nodeProps.get("label"))) {
                        continue;
                    }
                    
                    // Check where conditions
                    if (!matchesWhereConditions(nodeProps, whereConditions)) {
                        continue;
                    }
                    
                    // Build result based on return clause
                    Map<String, Object> result = buildReturnResult(nodeId, nodeProps, returnPart);
                    results.add(result);
                }
            }
        } catch (Exception e) {
            logger.error("Error in MATCH query execution", e);
        }
        
        return results;
    }
    
    private List<Map<String, Object>> executeCreateQuery(String cypher, Map<String, Object> parameters) {
        // Basic CREATE node support
        // CREATE (n:Label {property: value})
        try {
            String label = extractLabelFromCreate(cypher);
            Map<String, Object> properties = extractPropertiesFromCreate(cypher, parameters);
            
            if (label != null) {
                properties.put("label", label);
                createNode(label, properties).get();
                
                // CREATE queries without RETURN clause should return empty results
                return Collections.emptyList();
            }
        } catch (Exception e) {
            logger.error("Error in CREATE query execution", e);
        }
        
        return Collections.emptyList();
    }
    
    private List<Map<String, Object>> executeDeleteQuery(String cypher, Map<String, Object> parameters) {
        // Basic DELETE support
        // DELETE n WHERE n.id = value
        try {
            Map<String, Object> whereConditions = extractWhereConditions(cypher, parameters);
            int deletedCount = 0;
            
            List<String> toDelete = new ArrayList<>();
            for (Map.Entry<String, Map<String, Object>> entry : nodes.entrySet()) {
                if (matchesWhereConditions(entry.getValue(), whereConditions)) {
                    toDelete.add(entry.getKey());
                }
            }
            
            for (String nodeId : toDelete) {
                deleteNode(nodeId).get();
                deletedCount++;
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("deletedNodes", deletedCount);
            
            return Collections.singletonList(result);
        } catch (Exception e) {
            logger.error("Error in DELETE query execution", e);
        }
        
        return Collections.emptyList();
    }
    
    private List<Map<String, Object>> executeReturnQuery(String cypher, Map<String, Object> parameters) {
        // Simple RETURN query for constants
        try {
            String returnPart = cypher.substring(6).trim(); // Remove "RETURN"
            
            Map<String, Object> result = new HashMap<>();
            
            // Parse simple return expressions
            if (returnPart.contains("\"") || returnPart.contains("'")) {
                // String literal
                String value = returnPart.replaceAll("[\"']", "");
                result.put("value", value);
            } else if (returnPart.matches("\\d+")) {
                // Number literal
                result.put("value", Integer.parseInt(returnPart));
            } else {
                // Parameter or expression
                result.put("expression", returnPart);
            }
            
            return Collections.singletonList(result);
        } catch (Exception e) {
            logger.error("Error in RETURN query execution", e);
        }
        
        return Collections.emptyList();
    }
    
    private String extractLabelFromMatch(String matchPart) {
        // Extract label from patterns like (n:Label) or (n:Label {prop: value})
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\([^)]*:([^\\s})]+)");
        java.util.regex.Matcher matcher = pattern.matcher(matchPart);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private String extractLabelFromCreate(String cypher) {
        // Extract label from CREATE (n:Label {prop: value})
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\([^)]*:([^\\s}]+)");
        java.util.regex.Matcher matcher = pattern.matcher(cypher);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private Map<String, Object> extractWhereConditions(String cypher, Map<String, Object> parameters) {
        Map<String, Object> conditions = new HashMap<>();
        
        if (cypher.toUpperCase().contains("WHERE")) {
            try {
                String[] parts = cypher.toUpperCase().split("WHERE");
                if (parts.length > 1) {
                    String wherePart = parts[1].split("RETURN")[0].trim();
                    
                    // Simple condition parsing: n.property = value or n.property = $param
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\w+)\\.(\\w+)\\s*=\\s*([^\\s]+)");
                    java.util.regex.Matcher matcher = pattern.matcher(wherePart);
                    
                    while (matcher.find()) {
                        String property = matcher.group(2);
                        String value = matcher.group(3);
                        
                        if (value.startsWith("$")) {
                            // Parameter reference
                            String paramName = value.substring(1);
                            if (parameters.containsKey(paramName)) {
                                conditions.put(property, parameters.get(paramName));
                            }
                        } else {
                            // Literal value
                            conditions.put(property, value.replaceAll("[\"']", ""));
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Error parsing WHERE conditions", e);
            }
        }
        
        return conditions;
    }
    
    private Map<String, Object> extractPropertiesFromCreate(String cypher, Map<String, Object> parameters) {
        Map<String, Object> properties = new HashMap<>();
        
        try {
            // Extract properties from CREATE (n:Label {prop1: value1, prop2: $param2})
            int start = cypher.indexOf("{");
            int end = cypher.lastIndexOf("}");
            
            if (start >= 0 && end > start) {
                String propsPart = cypher.substring(start + 1, end);
                
                // Simple property parsing
                String[] props = propsPart.split(",");
                for (String prop : props) {
                    String[] keyValue = prop.split(":");
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim();
                        String value = keyValue[1].trim();
                        
                        if (value.startsWith("$")) {
                            // Parameter reference
                            String paramName = value.substring(1);
                            if (parameters.containsKey(paramName)) {
                                properties.put(key, parameters.get(paramName));
                            }
                        } else {
                            // Literal value
                            properties.put(key, value.replaceAll("[\"']", ""));
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error parsing CREATE properties", e);
        }
        
        return properties;
    }
    
    private boolean matchesWhereConditions(Map<String, Object> nodeProps, Map<String, Object> conditions) {
        for (Map.Entry<String, Object> condition : conditions.entrySet()) {
            Object nodeValue = nodeProps.get(condition.getKey());
            Object conditionValue = condition.getValue();
            
            if (!Objects.equals(nodeValue, conditionValue)) {
                return false;
            }
        }
        return true;
    }
    
    private Map<String, Object> buildReturnResult(String nodeId, Map<String, Object> nodeProps, String returnPart) {
        Map<String, Object> result = new HashMap<>();
        
        if ("n".equals(returnPart.trim()) || "*".equals(returnPart.trim())) {
            // Return entire node
            result.put("id", nodeId);
            result.putAll(nodeProps);
        } else {
            // Return specific properties
            String[] returnFields = returnPart.split(",");
            for (String field : returnFields) {
                field = field.trim();
                if (field.startsWith("n.")) {
                    String propName = field.substring(2);
                    result.put(propName, nodeProps.get(propName));
                } else {
                    result.put(field, nodeProps.get(field));
                }
            }
        }
        
        return result;
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
                logger.info("InMemoryGraphStore closed successfully");
                return null;
            } catch (Exception e) {
                logger.error("Error closing InMemoryGraphStore", e);
                throw new RuntimeException("Failed to close graph store", e);
            }
        });
    }
}