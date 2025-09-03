package com.mem0.exception;

/**
 * Mem0框架的基础异常类 - Base exception class for all Mem0 exceptions
 * 
 * <p>这是Mem0框架中所有异常的基础类，提供统一的异常处理机制和错误代码管理。
 * 所有Mem0相关的异常都应该继承此类，以确保异常处理的一致性和可追踪性。</p>
 * 
 * <p>This is the base class for all exceptions in the Mem0 framework, providing unified
 * exception handling mechanisms and error code management. All Mem0-related exceptions
 * should inherit from this class to ensure consistency and traceability in exception handling.</p>
 * 
 * <h3>核心功能 / Key Features:</h3>
 * <ul>
 *   <li>统一错误代码管理 / Unified error code management</li>
 *   <li>结构化异常信息 / Structured exception information</li>
 *   <li>异常链支持 / Exception chaining support</li>
 *   <li>可扩展的错误分类 / Extensible error classification</li>
 * </ul>
 * 
 * <h3>错误代码体系 / Error Code System:</h3>
 * <ul>
 *   <li>4xxx - 客户端错误 / Client errors (validation, not found, etc.)</li>
 *   <li>5xxx - 服务端错误 / Server errors (internal, storage, etc.)</li>
 * </ul>
 * 
 * <p>使用示例 / Usage Example:</p>
 * <pre>{@code
 * // 抛出带错误代码的异常
 * throw new Mem0Exception(ErrorCode.VALIDATION_ERROR, "Invalid memory content");
 * 
 * // 异常处理
 * try {
 *     memoryService.addMemory(memory);
 * } catch (Mem0Exception e) {
 *     System.err.println("Error Code: " + e.getErrorCode().getCode());
 *     System.err.println("Description: " + e.getErrorCode().getDescription());
 *     System.err.println("Message: " + e.getMessage());
 * }
 * }</pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class Mem0Exception extends RuntimeException {
    
    private final ErrorCode errorCode;
    
    public Mem0Exception(String message) {
        super(message);
        this.errorCode = ErrorCode.INTERNAL_ERROR;
    }
    
    public Mem0Exception(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.INTERNAL_ERROR;
    }
    
    public Mem0Exception(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public Mem0Exception(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    
    public enum ErrorCode {
        INTERNAL_ERROR(5000, "Internal error"),
        VALIDATION_ERROR(4000, "Validation error"),
        RESOURCE_NOT_FOUND(4004, "Resource not found"),
        DUPLICATE_RESOURCE(4009, "Resource already exists"),
        STORAGE_ERROR(5001, "Storage operation failed"),
        VECTOR_OPERATION_ERROR(5002, "Vector operation failed"),
        GRAPH_OPERATION_ERROR(5003, "Graph operation failed"),
        EMBEDDING_ERROR(5004, "Embedding generation failed"),
        LLM_ERROR(5005, "LLM operation failed");
        
        private final int code;
        private final String description;
        
        ErrorCode(int code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public int getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
    }
}