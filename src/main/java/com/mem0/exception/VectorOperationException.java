package com.mem0.exception;

/**
 * Exception thrown when vector operations fail
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class VectorOperationException extends Mem0Exception {
    
    public VectorOperationException(String message) {
        super(ErrorCode.VECTOR_OPERATION_ERROR, message);
    }
    
    public VectorOperationException(String message, Throwable cause) {
        super(ErrorCode.VECTOR_OPERATION_ERROR, message, cause);
    }
}