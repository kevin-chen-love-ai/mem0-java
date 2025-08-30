package com.mem0.core;

public class MemoryForgettingPolicy {
    private boolean forgettingEnabled = true;
    private double decayRate = 0.1;
    private MemoryImportance importanceThreshold = MemoryImportance.LOW;
    private int maxMemoryAge = 90; // days
    private int minAccessCount = 1;
    
    public MemoryForgettingPolicy() {}
    
    public MemoryForgettingPolicy(boolean forgettingEnabled, double decayRate, 
                                MemoryImportance importanceThreshold, int maxMemoryAge, int minAccessCount) {
        this.forgettingEnabled = forgettingEnabled;
        this.decayRate = decayRate;
        this.importanceThreshold = importanceThreshold;
        this.maxMemoryAge = maxMemoryAge;
        this.minAccessCount = minAccessCount;
    }
    
    public boolean isForgettingEnabled() {
        return forgettingEnabled;
    }
    
    public void setForgettingEnabled(boolean forgettingEnabled) {
        this.forgettingEnabled = forgettingEnabled;
    }
    
    public double getDecayRate() {
        return decayRate;
    }
    
    public void setDecayRate(double decayRate) {
        this.decayRate = decayRate;
    }
    
    public MemoryImportance getImportanceThreshold() {
        return importanceThreshold;
    }
    
    public void setImportanceThreshold(MemoryImportance importanceThreshold) {
        this.importanceThreshold = importanceThreshold;
    }
    
    public int getMaxMemoryAge() {
        return maxMemoryAge;
    }
    
    public void setMaxMemoryAge(int maxMemoryAge) {
        this.maxMemoryAge = maxMemoryAge;
    }
    
    public int getMinAccessCount() {
        return minAccessCount;
    }
    
    public void setMinAccessCount(int minAccessCount) {
        this.minAccessCount = minAccessCount;
    }
}