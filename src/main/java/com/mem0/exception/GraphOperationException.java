package com.mem0.exception;

/**
 * Exception thrown when graph operations fail
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class GraphOperationException extends Mem0Exception {
    
    public GraphOperationException(String message) {
        super(ErrorCode.GRAPH_OPERATION_ERROR, message);
    }
    
    public GraphOperationException(String message, Throwable cause) {
        super(ErrorCode.GRAPH_OPERATION_ERROR, message, cause);
    }
}