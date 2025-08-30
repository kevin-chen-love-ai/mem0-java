package com.mem0.core;

public class ConflictResolution {
    private final ConflictResolutionStrategy strategy;
    private final EnhancedMemory resultingMemory;
    private final String mergedContent;
    private final String reasoning;
    
    public ConflictResolution(ConflictResolutionStrategy strategy, 
                            EnhancedMemory resultingMemory, 
                            String mergedContent, 
                            String reasoning) {
        this.strategy = strategy;
        this.resultingMemory = resultingMemory;
        this.mergedContent = mergedContent;
        this.reasoning = reasoning;
    }
    
    public ConflictResolutionStrategy getStrategy() {
        return strategy;
    }
    
    public EnhancedMemory getResultingMemory() {
        return resultingMemory;
    }
    
    public String getMergedContent() {
        return mergedContent;
    }
    
    public String getReasoning() {
        return reasoning;
    }
    
    @Override
    public String toString() {
        return String.format("ConflictResolution{strategy=%s, reasoning='%s'}", 
                           strategy, reasoning);
    }
}