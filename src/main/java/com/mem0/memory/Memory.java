package com.mem0.memory;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * 内存对象基础模型 / Base Memory Object Model
 * 
 * 表示Mem0系统中的基础内存单元，包含内存的核心属性和元数据。作为所有内存操作的基础数据结构。
 * Represents the basic memory unit in the Mem0 system, containing core attributes and metadata of memories.
 * Serves as the foundational data structure for all memory operations.
 * 
 * <h3>核心功能 / Key Features:</h3>
 * <ul>
 *   <li>内存内容存储和管理 / Memory content storage and management</li>
 *   <li>用户和会话关联 / User and session association</li>
 *   <li>时间戳自动维护 / Automatic timestamp maintenance</li>
 *   <li>元数据扩展支持 / Metadata extension support</li>
 *   <li>内存标识和比较 / Memory identification and comparison</li>
 * </ul>
 * 
 * <h3>数据结构 / Data Structure:</h3>
 * <pre>
 * Memory {
 *   id: String          // 唯一标识符
 *   content: String     // 内存内容
 *   userId: String      // 用户ID
 *   agentId: String     // 智能体ID
 *   runId: String       // 运行ID
 *   createdAt: Instant  // 创建时间
 *   updatedAt: Instant  // 更新时间
 *   metadata: Map       // 元数据
 * }
 * </pre>
 * 
 * <h3>使用示例 / Usage Examples:</h3>
 * <pre>{@code
 * // 创建基础内存
 * Memory memory = new Memory("用户喜欢喝咖啡");
 * memory.setUserId("user123");
 * memory.setAgentId("agent456");
 * 
 * // 创建带元数据的内存
 * Map<String, Object> metadata = new HashMap<>();
 * metadata.put("category", "preference");
 * metadata.put("confidence", 0.9);
 * Memory memory = new Memory("mem_001", "用户喜欢喝咖啡", metadata);
 * 
 * // 更新内容
 * memory.setContent("用户更喜欢拿铁"); // 自动更新updatedAt时间戳
 * }</pre>
 * 
 * <h3>线程安全性 / Thread Safety:</h3>
 * 此类不是线程安全的。在并发环境中使用时，需要外部同步机制保护。
 * This class is not thread-safe. External synchronization is required when used in concurrent environments.
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 * @see com.mem0.core.EnhancedMemory
 */
public class Memory {
    
    private String id;
    private String content;
    private String userId;
    private String sessionId;
    private String agentId;
    private String runId;
    private Instant createdAt;
    private Instant updatedAt;
    private Map<String, Object> metadata;
    
    public Memory() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }
    
    public Memory(String content) {
        this();
        this.content = content;
    }
    
    public Memory(String id, String content, Map<String, Object> metadata) {
        this();
        this.id = id;
        this.content = content;
        this.metadata = metadata;
    }
    
    public Memory(String id, String content, String userId, String sessionId) {
        this();
        this.id = id;
        this.content = content;
        this.userId = userId;
        this.sessionId = sessionId;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getContent() { return content; }
    public void setContent(String content) { 
        this.content = content; 
        this.updatedAt = Instant.now();
    }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { 
        this.metadata = metadata; 
        this.updatedAt = Instant.now();
    }
    
    @Override
    public String toString() {
        return String.format("Memory{id='%s', content='%s', userId='%s'}", 
                           id, content, userId);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Memory memory = (Memory) obj;
        return Objects.equals(id, memory.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}