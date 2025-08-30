package com.mem0.config;

import java.util.Arrays;
import java.util.List;

/**
 * HierarchyConfiguration - 分层内存管理配置
 * 
 * 集中管理所有分层内存管理相关的配置参数。
 * 包括User、Session、Agent三级内存的配置以及层级管理器的配置。
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 2024-12-20
 */
public class HierarchyConfiguration extends BaseConfiguration {
    
    public HierarchyConfiguration() {
        super("mem0.hierarchy");
    }
    
    @Override
    protected void loadDefaultConfiguration() {
        // User Memory 配置
        setConfigValue("user.maxMemoriesPerUser", 10000);
        setConfigValue("user.enableProfileGeneration", true);
        setConfigValue("user.enableRecommendations", true);
        setConfigValue("user.enableInterestEvolution", true);
        setConfigValue("user.profileUpdateThreshold", 5);
        setConfigValue("user.interestDecayRate", 0.1);
        setConfigValue("user.maxRecommendations", 20);
        setConfigValue("user.cacheTTLMinutes", 60);
        
        // Session Memory 配置
        setConfigValue("session.maxContextWindow", 50);
        setConfigValue("session.enableContextCompression", true);
        setConfigValue("session.enableTemporaryPreferences", true);
        setConfigValue("session.sessionTimeoutMinutes", 120);
        setConfigValue("session.maxTemporaryPreferences", 10);
        setConfigValue("session.compressionThreshold", 40);
        setConfigValue("session.transferThreshold", 0.7);
        setConfigValue("session.cleanupIntervalMinutes", 30);
        
        // Agent Memory 配置
        setConfigValue("agent.maxDomainKnowledge", 1000);
        setConfigValue("agent.maxTaskTemplates", 100);
        setConfigValue("agent.maxBestPractices", 200);
        setConfigValue("agent.enableKnowledgeSharing", true);
        setConfigValue("agent.enablePerformanceTracking", true);
        setConfigValue("agent.knowledgeExpiryDays", 365);
        setConfigValue("agent.shareConfidenceThreshold", 0.8);
        setConfigValue("agent.maxSharedKnowledge", 50);
        
        // Hierarchy Manager 配置
        setConfigValue("manager.enableCrossHierarchySearch", true);
        setConfigValue("manager.enableIntelligentRouting", true);
        setConfigValue("manager.enableConflictResolution", true);
        setConfigValue("manager.enablePerformanceMonitoring", true);
        setConfigValue("manager.searchTimeout", 5000);
        setConfigValue("manager.routingCacheSize", 1000);
        setConfigValue("manager.maxSearchResults", 100);
        setConfigValue("manager.conflictResolutionStrategy", "priority_based");
        setConfigValue("manager.performanceReportInterval", 60);
        
        // 通用配置
        setConfigValue("common.enableAsyncProcessing", true);
        setConfigValue("common.threadPoolSize", 10);
        setConfigValue("common.maxRetryAttempts", 3);
        setConfigValue("common.retryDelayMs", 1000);
        setConfigValue("common.enableDetailedLogging", false);
        setConfigValue("common.metricsCollectionEnabled", true);
    }
    
    @Override
    protected void validateConfiguration() throws IllegalArgumentException {
        // 验证User Memory配置
        if (getInt("user.maxMemoriesPerUser", 0) <= 0) {
            throw new IllegalArgumentException("user.maxMemoriesPerUser must be positive");
        }
        
        if (getInt("user.profileUpdateThreshold", 0) <= 0) {
            throw new IllegalArgumentException("user.profileUpdateThreshold must be positive");
        }
        
        double interestDecayRate = getDouble("user.interestDecayRate", 0.0);
        if (interestDecayRate < 0.0 || interestDecayRate > 1.0) {
            throw new IllegalArgumentException("user.interestDecayRate must be between 0.0 and 1.0");
        }
        
        // 验证Session Memory配置
        if (getInt("session.maxContextWindow", 0) <= 0) {
            throw new IllegalArgumentException("session.maxContextWindow must be positive");
        }
        
        if (getInt("session.sessionTimeoutMinutes", 0) <= 0) {
            throw new IllegalArgumentException("session.sessionTimeoutMinutes must be positive");
        }
        
        double transferThreshold = getDouble("session.transferThreshold", 0.0);
        if (transferThreshold < 0.0 || transferThreshold > 1.0) {
            throw new IllegalArgumentException("session.transferThreshold must be between 0.0 and 1.0");
        }
        
        // 验证Agent Memory配置
        if (getInt("agent.maxDomainKnowledge", 0) <= 0) {
            throw new IllegalArgumentException("agent.maxDomainKnowledge must be positive");
        }
        
        double shareConfidenceThreshold = getDouble("agent.shareConfidenceThreshold", 0.0);
        if (shareConfidenceThreshold < 0.0 || shareConfidenceThreshold > 1.0) {
            throw new IllegalArgumentException("agent.shareConfidenceThreshold must be between 0.0 and 1.0");
        }
        
        // 验证Hierarchy Manager配置
        if (getInt("manager.searchTimeout", 0) <= 0) {
            throw new IllegalArgumentException("manager.searchTimeout must be positive");
        }
        
        String conflictStrategy = getString("manager.conflictResolutionStrategy", "");
        List<String> validStrategies = Arrays.asList("priority_based", "timestamp_based", "importance_based", "user_preference");
        if (!validStrategies.contains(conflictStrategy)) {
            throw new IllegalArgumentException("Invalid conflict resolution strategy: " + conflictStrategy);
        }
        
        // 验证通用配置
        if (getInt("common.threadPoolSize", 0) <= 0) {
            throw new IllegalArgumentException("common.threadPoolSize must be positive");
        }
        
        if (getInt("common.maxRetryAttempts", 0) < 0) {
            throw new IllegalArgumentException("common.maxRetryAttempts cannot be negative");
        }
    }
    
    // User Memory 配置访问方法
    public int getMaxMemoriesPerUser() {
        return getInt("user.maxMemoriesPerUser", 10000);
    }
    
    public boolean isEnableProfileGeneration() {
        return getBoolean("user.enableProfileGeneration", true);
    }
    
    public boolean isEnableRecommendations() {
        return getBoolean("user.enableRecommendations", true);
    }
    
    public boolean isEnableInterestEvolution() {
        return getBoolean("user.enableInterestEvolution", true);
    }
    
    public int getProfileUpdateThreshold() {
        return getInt("user.profileUpdateThreshold", 5);
    }
    
    public double getInterestDecayRate() {
        return getDouble("user.interestDecayRate", 0.1);
    }
    
    public int getMaxRecommendations() {
        return getInt("user.maxRecommendations", 20);
    }
    
    public int getCacheTTLMinutes() {
        return getInt("user.cacheTTLMinutes", 60);
    }
    
    // Session Memory 配置访问方法
    public int getMaxContextWindow() {
        return getInt("session.maxContextWindow", 50);
    }
    
    public boolean isEnableContextCompression() {
        return getBoolean("session.enableContextCompression", true);
    }
    
    public boolean isEnableTemporaryPreferences() {
        return getBoolean("session.enableTemporaryPreferences", true);
    }
    
    public int getSessionTimeoutMinutes() {
        return getInt("session.sessionTimeoutMinutes", 120);
    }
    
    public int getMaxTemporaryPreferences() {
        return getInt("session.maxTemporaryPreferences", 10);
    }
    
    public int getCompressionThreshold() {
        return getInt("session.compressionThreshold", 40);
    }
    
    public double getTransferThreshold() {
        return getDouble("session.transferThreshold", 0.7);
    }
    
    public int getCleanupIntervalMinutes() {
        return getInt("session.cleanupIntervalMinutes", 30);
    }
    
    // Agent Memory 配置访问方法
    public int getMaxDomainKnowledge() {
        return getInt("agent.maxDomainKnowledge", 1000);
    }
    
    public int getMaxTaskTemplates() {
        return getInt("agent.maxTaskTemplates", 100);
    }
    
    public int getMaxBestPractices() {
        return getInt("agent.maxBestPractices", 200);
    }
    
    public boolean isEnableKnowledgeSharing() {
        return getBoolean("agent.enableKnowledgeSharing", true);
    }
    
    public boolean isEnablePerformanceTracking() {
        return getBoolean("agent.enablePerformanceTracking", true);
    }
    
    public int getKnowledgeExpiryDays() {
        return getInt("agent.knowledgeExpiryDays", 365);
    }
    
    public double getShareConfidenceThreshold() {
        return getDouble("agent.shareConfidenceThreshold", 0.8);
    }
    
    public int getMaxSharedKnowledge() {
        return getInt("agent.maxSharedKnowledge", 50);
    }
    
    // Hierarchy Manager 配置访问方法
    public boolean isEnableCrossHierarchySearch() {
        return getBoolean("manager.enableCrossHierarchySearch", true);
    }
    
    public boolean isEnableIntelligentRouting() {
        return getBoolean("manager.enableIntelligentRouting", true);
    }
    
    public boolean isEnableConflictResolution() {
        return getBoolean("manager.enableConflictResolution", true);
    }
    
    public boolean isEnablePerformanceMonitoring() {
        return getBoolean("manager.enablePerformanceMonitoring", true);
    }
    
    public int getSearchTimeout() {
        return getInt("manager.searchTimeout", 5000);
    }
    
    public int getRoutingCacheSize() {
        return getInt("manager.routingCacheSize", 1000);
    }
    
    public int getMaxSearchResults() {
        return getInt("manager.maxSearchResults", 100);
    }
    
    public String getConflictResolutionStrategy() {
        return getString("manager.conflictResolutionStrategy", "priority_based");
    }
    
    public int getPerformanceReportInterval() {
        return getInt("manager.performanceReportInterval", 60);
    }
    
    // 通用配置访问方法
    public boolean isEnableAsyncProcessing() {
        return getBoolean("common.enableAsyncProcessing", true);
    }
    
    public int getThreadPoolSize() {
        return getInt("common.threadPoolSize", 10);
    }
    
    public int getMaxRetryAttempts() {
        return getInt("common.maxRetryAttempts", 3);
    }
    
    public long getRetryDelayMs() {
        return getLong("common.retryDelayMs", 1000L);
    }
    
    public boolean isEnableDetailedLogging() {
        return getBoolean("common.enableDetailedLogging", false);
    }
    
    public boolean isMetricsCollectionEnabled() {
        return getBoolean("common.metricsCollectionEnabled", true);
    }
}