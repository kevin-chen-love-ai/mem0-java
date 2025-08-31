package com.mem0.memory;

/**
 * 内存操作类型枚举 - Memory operation type enumeration
 * 
 * <p>此枚举定义了Mem0系统中所有支持的内存操作类型，用于标识和分类不同的内存管理操作。
 * 每种操作类型对应特定的业务逻辑和处理流程，便于系统进行操作跟踪、权限控制和性能监控。</p>
 * 
 * <p>This enumeration defines all supported memory operation types in the Mem0 system,
 * used to identify and classify different memory management operations. Each operation type
 * corresponds to specific business logic and processing flows, facilitating system operation
 * tracking, permission control, and performance monitoring.</p>
 * 
 * <h3>操作类型分类 / Operation Type Classification:</h3>
 * <ul>
 *   <li><strong>单项操作</strong> - CREATE, UPDATE, DELETE, SEARCH, MERGE, CONFLICT_RESOLUTION</li>
 *   <li><strong>批量操作</strong> - BATCH_CREATE, BATCH_UPDATE, BATCH_DELETE</li>
 * </ul>
 * 
 * <h3>操作说明 / Operation Description:</h3>
 * <ul>
 *   <li><strong>CREATE</strong> - 创建新的内存条目 / Create new memory entry</li>
 *   <li><strong>UPDATE</strong> - 更新现有内存条目 / Update existing memory entry</li>
 *   <li><strong>DELETE</strong> - 删除内存条目 / Delete memory entry</li>
 *   <li><strong>SEARCH</strong> - 搜索和检索内存 / Search and retrieve memories</li>
 *   <li><strong>MERGE</strong> - 合并相似或重复的内存 / Merge similar or duplicate memories</li>
 *   <li><strong>CONFLICT_RESOLUTION</strong> - 解决内存冲突 / Resolve memory conflicts</li>
 *   <li><strong>BATCH_CREATE</strong> - 批量创建内存条目 / Batch create memory entries</li>
 *   <li><strong>BATCH_UPDATE</strong> - 批量更新内存条目 / Batch update memory entries</li>
 *   <li><strong>BATCH_DELETE</strong> - 批量删除内存条目 / Batch delete memory entries</li>
 * </ul>
 * 
 * <p>使用示例 / Usage Example:</p>
 * <pre>{@code
 * // 在内存管理操作中使用
 * MemoryOperation operation = MemoryOperation.CREATE;
 * 
 * // 根据操作类型执行不同逻辑
 * switch (operation) {
 *     case CREATE:
 *         performCreateOperation();
 *         break;
 *     case BATCH_CREATE:
 *         performBatchCreateOperation();
 *         break;
 *     case CONFLICT_RESOLUTION:
 *         performConflictResolution();
 *         break;
 *     default:
 *         throw new UnsupportedOperationException("Unsupported operation: " + operation);
 * }
 * 
 * // 操作日志记录
 * logger.info("Executing memory operation: {}", operation.name());
 * }</pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 * @see ConcurrentMemoryManager
 * @see MemoryShard
 */
public enum MemoryOperation {
    CREATE,
    UPDATE,
    DELETE,
    SEARCH,
    MERGE,
    CONFLICT_RESOLUTION,
    BATCH_CREATE,
    BATCH_UPDATE,
    BATCH_DELETE
}