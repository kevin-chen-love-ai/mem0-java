package com.mem0.core;

public enum ConflictResolutionStrategy {
    KEEP_FIRST("keep_first"),
    KEEP_SECOND("keep_second"),
    MERGE("merge"),
    KEEP_BOTH("keep_both"),
    DELETE_BOTH("delete_both");
    
    private final String value;
    
    ConflictResolutionStrategy(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
}