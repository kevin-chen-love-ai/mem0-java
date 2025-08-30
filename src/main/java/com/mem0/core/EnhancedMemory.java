package com.mem0.core;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 增强型内存对象，代表Mem0系统中的智能内存单元
 * Enhanced memory object representing an intelligent memory unit in Mem0 system
 * 
 * <p>增强型内存提供以下功能 / Enhanced memory provides the following features:</p>
 * <ul>
 *   <li>内存内容和元数据管理 / Memory content and metadata management</li>
 *   <li>生命周期跟踪和访问统计 / Lifecycle tracking and access statistics</li>
 *   <li>语义相似性和关系管理 / Semantic similarity and relationship management</li>
 *   <li>内存重要性和置信度评分 / Memory importance and confidence scoring</li>
 *   <li>标签和实体识别 / Tag and entity recognition</li>
 *   <li>时间感知的衰减机制 / Time-aware decay mechanism</li>
 * </ul>
 * 
 * <p>使用示例 / Usage example:</p>
 * <pre>{@code
 * // 创建新的增强型内存
 * Map<String, Object> metadata = Map.of("source", "chat", "language", "zh");
 * EnhancedMemory memory = new EnhancedMemory("mem_123", "用户喜欢喝咖啡", "user_456", metadata);
 * 
 * // 设置内存属性
 * memory.setType(MemoryType.PREFERENCE);
 * memory.setImportance(MemoryImportance.HIGH);
 * memory.addTag("preference");
 * memory.addEntity("咖啡");
 * 
 * // 访问内存（自动更新统计）
 * memory.access();
 * }</pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class EnhancedMemory {
    
    private final String id;
    private final String content;
    private final String userId;
    private final String agentId;
    private final String runId;
    
    private MemoryType type;
    private MemoryImportance importance;
    private double relevanceScore;
    
    // Temporal information
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastAccessedAt;
    private LocalDateTime expiresAt;
    
    // Lifecycle tracking
    private int accessCount;
    private int updateCount;
    private double confidenceScore;
    private boolean isConsolidated;
    private boolean isDeprecated;
    
    // Content metadata
    private final Map<String, Object> metadata;
    private final Set<String> tags;
    private final Set<String> entities;
    private String contentHash;
    
    // Relationship information
    private final Set<String> relatedMemoryIds;
    private final Map<String, Double> semanticSimilarities;
    
    public EnhancedMemory(String id, String content, String userId) {
        this(id, content, userId, null, null);
    }
    
    public EnhancedMemory(String id, String content, String userId, Map<String, Object> metadata) {
        this(id, content, userId, null, null);
        if (metadata != null) {
            this.metadata.putAll(metadata);
        }
    }
    
    public EnhancedMemory(String id, String content, String userId, String agentId, String runId) {
        this.id = id;
        this.content = content;
        this.userId = userId;
        this.agentId = agentId;
        this.runId = runId;
        
        // Initialize with defaults
        this.type = MemoryType.SEMANTIC;
        this.importance = MemoryImportance.MEDIUM;
        this.relevanceScore = 0.0;
        
        // Temporal initialization
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.lastAccessedAt = now;
        this.expiresAt = null; // No expiration by default
        
        // Lifecycle initialization
        this.accessCount = 1; // Created with first access
        this.updateCount = 0;
        this.confidenceScore = 1.0;
        this.isConsolidated = false;
        this.isDeprecated = false;
        
        // Collections initialization
        this.metadata = new HashMap<>();
        this.tags = new HashSet<>();
        this.entities = new HashSet<>();
        this.relatedMemoryIds = new HashSet<>();
        this.semanticSimilarities = new HashMap<>();
        
        // Generate content hash
        this.contentHash = generateContentHash(content);
    }
    
    // Factory methods for different memory types
    public static EnhancedMemory createSemanticMemory(String id, String content, String userId) {
        EnhancedMemory memory = new EnhancedMemory(id, content, userId);
        memory.setType(MemoryType.SEMANTIC);
        memory.setImportance(MemoryImportance.MEDIUM);
        return memory;
    }
    
    public static EnhancedMemory createEpisodicMemory(String id, String content, String userId, 
                                                     LocalDateTime eventTime) {
        EnhancedMemory memory = new EnhancedMemory(id, content, userId);
        memory.setType(MemoryType.EPISODIC);
        memory.setImportance(MemoryImportance.HIGH);
        memory.getMetadata().put("eventTime", eventTime.toString());
        memory.getMetadata().put("temporal_context", true);
        return memory;
    }
    
    public static EnhancedMemory createProceduralMemory(String id, String content, String userId) {
        EnhancedMemory memory = new EnhancedMemory(id, content, userId);
        memory.setType(MemoryType.PROCEDURAL);
        memory.setImportance(MemoryImportance.HIGH);
        memory.getMetadata().put("skill_based", true);
        return memory;
    }
    
    public static EnhancedMemory createPreferenceMemory(String id, String content, String userId, 
                                                       String preferenceCategory) {
        EnhancedMemory memory = new EnhancedMemory(id, content, userId);
        memory.setType(MemoryType.PREFERENCE);
        memory.setImportance(MemoryImportance.MEDIUM);
        memory.getMetadata().put("preference_category", preferenceCategory);
        memory.getTags().add("preference");
        return memory;
    }
    
    // Lifecycle methods
    public void recordAccess() {
        this.accessCount++;
        this.lastAccessedAt = LocalDateTime.now();
        updateImportanceBasedOnAccess();
    }
    
    public void recordUpdate(String newContent) {
        this.updateCount++;
        this.updatedAt = LocalDateTime.now();
        
        if (newContent != null && !newContent.equals(this.content)) {
            this.contentHash = generateContentHash(newContent);
        }
        
        // Reset consolidation status when updated
        this.isConsolidated = false;
    }
    
    public void consolidate() {
        this.isConsolidated = true;
        // Consolidated memories have higher importance
        if (this.importance.getScore() < MemoryImportance.HIGH.getScore()) {
            this.importance = MemoryImportance.HIGH;
        }
    }
    
    public void deprecate() {
        this.isDeprecated = true;
        this.importance = MemoryImportance.MINIMAL;
    }
    
    // Temporal methods
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    public long getDaysOld() {
        if (createdAt == null) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(createdAt, LocalDateTime.now());
        return Math.max(0, days); // Ensure non-negative
    }
    
    public long getDaysSinceLastAccess() {
        if (lastAccessedAt == null) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(lastAccessedAt, LocalDateTime.now());
        return Math.max(0, days); // Ensure non-negative
    }
    
    public void setTTL(long days) {
        this.expiresAt = LocalDateTime.now().plusDays(days);
    }
    
    // Relationship methods
    public void addRelatedMemory(String memoryId, double similarity) {
        this.relatedMemoryIds.add(memoryId);
        this.semanticSimilarities.put(memoryId, similarity);
    }
    
    public void removeRelatedMemory(String memoryId) {
        this.relatedMemoryIds.remove(memoryId);
        this.semanticSimilarities.remove(memoryId);
    }
    
    public double getSimilarityWith(String memoryId) {
        return this.semanticSimilarities.getOrDefault(memoryId, 0.0);
    }
    
    // Scoring and ranking
    public double calculateDecayScore() {
        long daysSinceAccess = getDaysSinceLastAccess();
        double decayFactor = Math.exp(-daysSinceAccess / 30.0); // 30-day half-life
        return importance.getScore() * decayFactor * confidenceScore;
    }
    
    public double calculateRelevanceScore(String query) {
        // This would typically use embedding similarity
        // For now, simple text matching as placeholder
        double textSimilarity = calculateTextSimilarity(content, query);
        double importanceBoost = importance.getScore() / 5.0;
        double accessBoost = Math.log(accessCount + 1) / 10.0;
        double recencyBoost = Math.exp(-getDaysSinceLastAccess() / 7.0) / 10.0;
        
        return textSimilarity * (1.0 + importanceBoost + accessBoost + recencyBoost);
    }
    
    private void updateImportanceBasedOnAccess() {
        // Frequently accessed memories become more important
        if (accessCount > 10 && importance.getScore() < MemoryImportance.HIGH.getScore()) {
            this.importance = MemoryImportance.HIGH;
        } else if (accessCount > 5 && importance.getScore() < MemoryImportance.MEDIUM.getScore()) {
            this.importance = MemoryImportance.MEDIUM;
        }
    }
    
    /**
     * 更新访问时间和计数
     * Update access time and count
     */
    public void updateAccessTime() {
        this.lastAccessedAt = LocalDateTime.now();
        this.accessCount++;
        updateImportanceBasedOnAccess();
    }
    
    private String generateContentHash(String content) {
        if (content == null) {
            return "null-content";
        }
        return String.valueOf(content.hashCode());
    }
    
    private double calculateTextSimilarity(String text1, String text2) {
        // Handle null inputs
        if (text1 == null || text2 == null) {
            return 0.0;
        }
        
        // Simple Jaccard similarity as placeholder
        Set<String> words1 = new HashSet<>(Arrays.asList(text1.toLowerCase().split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(text2.toLowerCase().split("\\s+")));
        
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    // Getters and setters
    public String getId() { return id; }
    public String getContent() { return content; }
    public String getUserId() { return userId; }
    public String getAgentId() { return agentId; }
    public String getRunId() { return runId; }
    
    public MemoryType getType() { return type; }
    public void setType(MemoryType type) { this.type = type; }
    
    public MemoryImportance getImportance() { return importance; }
    public void setImportance(MemoryImportance importance) { this.importance = importance; }
    
    public double getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(double relevanceScore) { this.relevanceScore = relevanceScore; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
    
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    
    public int getAccessCount() { return accessCount; }
    public int getUpdateCount() { return updateCount; }
    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }
    
    public boolean isConsolidated() { return isConsolidated; }
    public boolean isDeprecated() { return isDeprecated; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public Set<String> getTags() { return tags; }
    public Set<String> getEntities() { return entities; }
    public String getContentHash() { return contentHash; }
    public Set<String> getRelatedMemoryIds() { return relatedMemoryIds; }
    
    
    public Map<String, Double> getSemanticSimilarities() { return semanticSimilarities; }
    
    // Additional methods needed by other classes
    public void markAsUpdated() {
        this.updatedAt = LocalDateTime.now();
        this.updateCount++;
    }
    
    public boolean isExpired(long currentTime) {
        if (expiresAt == null) return false;
        LocalDateTime currentDateTime = LocalDateTime.ofEpochSecond(currentTime / 1000, (int)(currentTime % 1000) * 1000000, java.time.ZoneOffset.UTC);
        return expiresAt.isBefore(currentDateTime);
    }
    
    @Override
    public String toString() {
        String contentPreview = content != null ? 
            (content.length() > 50 ? content.substring(0, 50) + "..." : content) : "null";
        return String.format("EnhancedMemory{id='%s', type=%s, importance=%s, content='%s', " +
                "accessCount=%d, age=%d days, relevanceScore=%.3f}",
                id, type, importance, contentPreview,
                accessCount, getDaysOld(), relevanceScore);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnhancedMemory that = (EnhancedMemory) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
