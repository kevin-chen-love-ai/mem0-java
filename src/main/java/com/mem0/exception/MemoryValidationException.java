package com.mem0.exception;

/**
 * Exception thrown when memory validation fails
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class MemoryValidationException extends Mem0Exception {
    
    public MemoryValidationException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }
    
    public MemoryValidationException(String message, Throwable cause) {
        super(ErrorCode.VALIDATION_ERROR, message, cause);
    }
}