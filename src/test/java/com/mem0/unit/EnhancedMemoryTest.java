package com.mem0.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 增强内存对象单元测试 - Unit tests for EnhancedMemory class
 * 
 * <p>此测试类验证EnhancedMemory类的所有核心功能，包括内存创建、访问跟踪、更新管理、
 * 重要性评估、衰减计算、关系管理等高级特性。确保内存对象的行为符合预期，
 * 并且在各种场景下能够正确处理内存生命周期。</p>
 * 
 * <p>This test class verifies all core functionalities of the EnhancedMemory class,
 * including memory creation, access tracking, update management, importance assessment,
 * decay calculation, relationship management, and other advanced features. It ensures
 * that memory objects behave as expected and handle memory lifecycle correctly
 * in various scenarios.</p>
 * 
 * <h3>测试覆盖范围 / Test Coverage:</h3>
 * <ul>
 *   <li>内存对象创建和初始化 / Memory object creation and initialization</li>
 *   <li>工厂方法测试 / Factory method testing</li>
 *   <li>访问和更新跟踪 / Access and update tracking</li>
 *   <li>内存巩固和废弃机制 / Memory consolidation and deprecation mechanisms</li>
 *   <li>TTL和过期管理 / TTL and expiration management</li>
 *   <li>年龄计算和衰减评分 / Age calculation and decay scoring</li>
 *   <li>关系管理和相似度计算 / Relationship management and similarity calculation</li>
 *   <li>元数据和标签管理 / Metadata and tag management</li>
 *   <li>重要性动态调整 / Dynamic importance adjustment</li>
 *   <li>对象相等性和哈希码 / Object equality and hash code</li>
 * </ul>
 * 
 * <h3>测试策略 / Testing Strategy:</h3>
 * <ul>
 *   <li>使用JUnit 5测试框架 / Uses JUnit 5 testing framework</li>
 *   <li>每个测试方法独立运行 / Each test method runs independently</li>
 *   <li>通过@BeforeEach设置测试环境 / Test environment setup via @BeforeEach</li>
 *   <li>覆盖正常和边界情况 / Covers normal and edge cases</li>
 *   <li>验证业务逻辑正确性 / Validates business logic correctness</li>
 * </ul>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 * @see EnhancedMemory
 * @see MemoryType
 * @see MemoryImportance
 */
public class EnhancedMemoryTest {
    
    private EnhancedMemory memory;
    private String memoryId;
    private String content;
    private String userId;
    
    @BeforeEach
    void setUp() {
        memoryId = "test-memory-123";
        content = "This is a test memory content for unit testing";
        userId = "test-user-456";
        
        memory = new EnhancedMemory(memoryId, content, userId);
    }
    
    @Test
    void testMemoryCreation() {
        assertEquals(memoryId, memory.getId());
        assertEquals(content, memory.getContent());
        assertEquals(userId, memory.getUserId());
        assertEquals(MemoryType.SEMANTIC, memory.getType());
        assertEquals(MemoryImportance.MEDIUM, memory.getImportance());
        assertEquals(1, memory.getAccessCount()); // Created with first access
        assertEquals(0, memory.getUpdateCount());
        assertTrue(memory.getConfidenceScore() > 0);
        assertFalse(memory.isConsolidated());
        assertFalse(memory.isDeprecated());
        assertNotNull(memory.getCreatedAt());
        assertNotNull(memory.getContentHash());
    }
    
    @Test
    void testFactoryMethods() {
        // Test semantic memory factory
        EnhancedMemory semantic = EnhancedMemory.createSemanticMemory(
            "sem-1", "Semantic content", "user1");
        assertEquals(MemoryType.SEMANTIC, semantic.getType());
        assertEquals(MemoryImportance.MEDIUM, semantic.getImportance());
        
        // Test episodic memory factory
        LocalDateTime eventTime = LocalDateTime.now().minusDays(1);
        EnhancedMemory episodic = EnhancedMemory.createEpisodicMemory(
            "epi-1", "I went to the store yesterday", "user1", eventTime);
        assertEquals(MemoryType.EPISODIC, episodic.getType());
        assertEquals(MemoryImportance.HIGH, episodic.getImportance());
        assertEquals(eventTime.toString(), episodic.getMetadata().get("eventTime"));
        
        // Test procedural memory factory
        EnhancedMemory procedural = EnhancedMemory.createProceduralMemory(
            "proc-1", "How to compile Java code", "user1");
        assertEquals(MemoryType.PROCEDURAL, procedural.getType());
        assertEquals(MemoryImportance.HIGH, procedural.getImportance());
        assertTrue((Boolean) procedural.getMetadata().get("skill_based"));
        
        // Test preference memory factory
        EnhancedMemory preference = EnhancedMemory.createPreferenceMemory(
            "pref-1", "I prefer coffee over tea", "user1", "beverage");
        assertEquals(MemoryType.PREFERENCE, preference.getType());
        assertEquals("beverage", preference.getMetadata().get("preference_category"));
        assertTrue(preference.getTags().contains("preference"));
    }
    
    @Test
    void testAccessTracking() {
        int initialAccessCount = memory.getAccessCount();
        LocalDateTime initialAccessTime = memory.getLastAccessedAt();
        
        // Wait a bit to ensure time difference
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        memory.recordAccess();
        
        assertEquals(initialAccessCount + 1, memory.getAccessCount());
        assertTrue(memory.getLastAccessedAt().isAfter(initialAccessTime));
    }
    
    @Test
    void testUpdateTracking() {
        int initialUpdateCount = memory.getUpdateCount();
        LocalDateTime initialUpdateTime = memory.getUpdatedAt();
        String initialHash = memory.getContentHash();
        
        // Wait a bit to ensure time difference
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        String newContent = "Updated content for testing";
        memory.recordUpdate(newContent);
        
        assertEquals(initialUpdateCount + 1, memory.getUpdateCount());
        assertTrue(memory.getUpdatedAt().isAfter(initialUpdateTime));
        assertNotEquals(initialHash, memory.getContentHash());
        assertFalse(memory.isConsolidated()); // Should reset consolidation
    }
    
    @Test
    void testConsolidation() {
        assertFalse(memory.isConsolidated());
        MemoryImportance initialImportance = memory.getImportance();
        
        memory.consolidate();
        
        assertTrue(memory.isConsolidated());
        // Importance should increase after consolidation if it was below HIGH
        if (initialImportance.getScore() < MemoryImportance.HIGH.getScore()) {
            assertEquals(MemoryImportance.HIGH, memory.getImportance());
        }
    }
    
    @Test
    void testDeprecation() {
        assertFalse(memory.isDeprecated());
        
        memory.deprecate();
        
        assertTrue(memory.isDeprecated());
        assertEquals(MemoryImportance.MINIMAL, memory.getImportance());
    }
    
    @Test
    void testTTL() {
        assertNull(memory.getExpiresAt());
        assertFalse(memory.isExpired());
        
        memory.setTTL(1); // 1 day
        assertNotNull(memory.getExpiresAt());
        assertFalse(memory.isExpired());
        
        // Test with past expiration
        memory.setTTL(-1); // -1 day (in the past)
        assertTrue(memory.isExpired());
    }
    
    @Test
    void testAgeCalculation() {
        assertTrue(memory.getDaysOld() >= 0);
        assertTrue(memory.getDaysSinceLastAccess() >= 0);
        
        // For newly created memory, these should be 0 or very small
        assertTrue(memory.getDaysOld() < 1);
        assertTrue(memory.getDaysSinceLastAccess() < 1);
    }
    
    @Test
    void testRelationshipManagement() {
        String relatedMemoryId = "related-memory-123";
        double similarity = 0.85;
        
        assertTrue(memory.getRelatedMemoryIds().isEmpty());
        assertEquals(0.0, memory.getSimilarityWith(relatedMemoryId));
        
        memory.addRelatedMemory(relatedMemoryId, similarity);
        
        assertTrue(memory.getRelatedMemoryIds().contains(relatedMemoryId));
        assertEquals(similarity, memory.getSimilarityWith(relatedMemoryId), 0.001);
        
        memory.removeRelatedMemory(relatedMemoryId);
        
        assertFalse(memory.getRelatedMemoryIds().contains(relatedMemoryId));
        assertEquals(0.0, memory.getSimilarityWith(relatedMemoryId));
    }
    
    @Test
    void testDecayScoreCalculation() {
        double decayScore = memory.calculateDecayScore();
        assertTrue(decayScore >= 0);
        assertTrue(decayScore <= 5.0); // Should be within reasonable bounds
        
        // Higher importance should result in higher decay score
        memory.setImportance(MemoryImportance.CRITICAL);
        double highImportanceDecay = memory.calculateDecayScore();
        assertTrue(highImportanceDecay >= decayScore);
    }
    
    @Test
    void testRelevanceScoreCalculation() {
        String query = "test memory content";
        double relevanceScore = memory.calculateRelevanceScore(query);
        
        assertTrue(relevanceScore >= 0);
        
        // Query with similar content should have higher relevance
        String similarQuery = "memory content test";
        double similarRelevance = memory.calculateRelevanceScore(similarQuery);
        assertTrue(similarRelevance > 0);
        
        // Completely different query should have lower relevance
        String differentQuery = "completely unrelated query about astronomy";
        double differentRelevance = memory.calculateRelevanceScore(differentQuery);
        assertTrue(differentRelevance < similarRelevance);
    }
    
    @Test
    void testMetadataManagement() {
        assertTrue(memory.getMetadata().isEmpty());
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("category", "test");
        metadata.put("priority", "high");
        metadata.put("number", 42);
        
        memory.getMetadata().putAll(metadata);
        
        assertEquals("test", memory.getMetadata().get("category"));
        assertEquals("high", memory.getMetadata().get("priority"));
        assertEquals(42, memory.getMetadata().get("number"));
    }
    
    @Test
    void testTagsAndEntities() {
        assertTrue(memory.getTags().isEmpty());
        assertTrue(memory.getEntities().isEmpty());
        
        memory.getTags().add("important");
        memory.getTags().add("work");
        memory.getEntities().add("John Smith");
        memory.getEntities().add("project-alpha");
        
        assertTrue(memory.getTags().contains("important"));
        assertTrue(memory.getTags().contains("work"));
        assertTrue(memory.getEntities().contains("John Smith"));
        assertTrue(memory.getEntities().contains("project-alpha"));
    }
    
    @Test
    void testEqualsAndHashCode() {
        EnhancedMemory memory2 = new EnhancedMemory(memoryId, "Different content", "Different user");
        
        assertEquals(memory, memory2); // Should be equal based on ID
        assertEquals(memory.hashCode(), memory2.hashCode());
        
        EnhancedMemory memory3 = new EnhancedMemory("different-id", content, userId);
        assertNotEquals(memory, memory3);
    }
    
    @Test
    void testToString() {
        String memoryString = memory.toString();
        
        assertNotNull(memoryString);
        assertTrue(memoryString.contains(memoryId));
        assertTrue(memoryString.contains(MemoryType.SEMANTIC.toString()));
        assertTrue(memoryString.contains(MemoryImportance.MEDIUM.toString()));
    }
    
    @Test
    void testImportanceBasedOnAccess() {
        // Memory with low access count
        assertEquals(MemoryImportance.MEDIUM, memory.getImportance());
        
        // Simulate frequent access
        for (int i = 0; i < 6; i++) {
            memory.recordAccess();
        }
        
        // Importance should increase with frequent access
        assertTrue(memory.getImportance().getScore() >= MemoryImportance.MEDIUM.getScore());
        
        // Even more frequent access
        for (int i = 0; i < 6; i++) {
            memory.recordAccess();
        }
        
        assertTrue(memory.getImportance().getScore() >= MemoryImportance.HIGH.getScore());
    }
    
    @Test
    void testMemoryTypes() {
        for (MemoryType type : MemoryType.values()) {
            memory.setType(type);
            assertEquals(type, memory.getType());
        }
    }
    
    @Test
    void testMemoryImportanceLevels() {
        for (MemoryImportance importance : MemoryImportance.values()) {
            memory.setImportance(importance);
            assertEquals(importance, memory.getImportance());
        }
    }
}