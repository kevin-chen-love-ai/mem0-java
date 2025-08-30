package com.mem0.core;

public class MemoryConflict {
    private final EnhancedMemory newMemory;
    private final EnhancedMemory existingMemory;
    private final ConflictType type;
    private final double severity;
    private String description;
    
    public MemoryConflict(EnhancedMemory newMemory, EnhancedMemory existingMemory, 
                         ConflictType type, double severity) {
        this.newMemory = newMemory;
        this.existingMemory = existingMemory;
        this.type = type;
        this.severity = severity;
    }
    
    public EnhancedMemory getNewMemory() {
        return newMemory;
    }
    
    public EnhancedMemory getExistingMemory() {
        return existingMemory;
    }
    
    public ConflictType getType() {
        return type;
    }
    
    public double getSeverity() {
        return severity;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return String.format("MemoryConflict{type=%s, severity=%.2f, new=%s, existing=%s}", 
                           type, severity, newMemory.getId(), existingMemory.getId());
    }
}