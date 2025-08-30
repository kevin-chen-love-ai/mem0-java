package com.mem0.config;

import java.util.Arrays;
import java.util.List;

/**
 * AIConfiguration - AI高级功能配置
 * 
 * 集中管理所有AI高级功能相关的配置参数。
 * 包括内存压缩引擎和自适应学习系统的配置。
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 2024-12-20
 */
public class AIConfiguration extends BaseConfiguration {
    
    public AIConfiguration() {
        super("mem0.ai");
    }
    
    @Override
    protected void loadDefaultConfiguration() {
        // Memory Compression 配置
        setConfigValue("compression.compressionThreshold", 0.8);
        setConfigValue("compression.maxCompressionRatio", 10);
        setConfigValue("compression.enableSemanticCompression", true);
        setConfigValue("compression.enableRedundancyRemoval", true);
        setConfigValue("compression.enableTemporalCompression", true);
        setConfigValue("compression.temporalCompressionDays", 30);
        setConfigValue("compression.importanceThreshold", 0.3);
        setConfigValue("compression.maxCompressedMemories", 1000);
        setConfigValue("compression.enableAutoCompression", true);
        setConfigValue("compression.autoCompressionIntervalHours", 24);
        setConfigValue("compression.compressionStrategies", Arrays.asList(
            "semantic", "redundancy", "temporal", "summary"));
        setConfigValue("compression.enableDecompression", true);
        setConfigValue("compression.enableCompressionMetrics", true);
        
        // Semantic Compression 策略配置
        setConfigValue("compression.semantic.similarityThreshold", 0.85);
        setConfigValue("compression.semantic.maxGroupSize", 10);
        setConfigValue("compression.semantic.enableContentMerging", true);
        setConfigValue("compression.semantic.preserveImportantSegments", true);
        setConfigValue("compression.semantic.maxMergedContentLength", 2000);
        
        // Redundancy Removal 策略配置
        setConfigValue("compression.redundancy.exactDuplicateOnly", false);
        setConfigValue("compression.redundancy.contentSimilarityThreshold", 0.95);
        setConfigValue("compression.redundancy.enableHashComparison", true);
        setConfigValue("compression.redundancy.preserveNewest", true);
        setConfigValue("compression.redundancy.maxDuplicateGroup", 20);
        
        // Temporal Compression 策略配置
        setConfigValue("compression.temporal.timeWindowHours", 24);
        setConfigValue("compression.temporal.enableDecayWeighting", true);
        setConfigValue("compression.temporal.decayRate", 0.1);
        setConfigValue("compression.temporal.minMemoriesPerWindow", 3);
        setConfigValue("compression.temporal.preserveKeyEvents", true);
        
        // Content Summary 策略配置
        setConfigValue("compression.summary.maxSummaryLength", 200);
        setConfigValue("compression.summary.minContentLength", 100);
        setConfigValue("compression.summary.enableKeywordPreservation", true);
        setConfigValue("compression.summary.summaryQuality", 0.7);
        setConfigValue("compression.summary.enableStructuredSummary", true);
        
        // Adaptive Learning 配置
        setConfigValue("learning.enableBehaviorLearning", true);
        setConfigValue("learning.enablePatternAnalysis", true);
        setConfigValue("learning.enableRecommendations", true);
        setConfigValue("learning.enablePredictiveOptimization", true);
        setConfigValue("learning.learningWindowDays", 30);
        setConfigValue("learning.learningRate", 0.1);
        setConfigValue("learning.minInteractionsForLearning", 5);
        setConfigValue("learning.confidenceThreshold", 0.7);
        setConfigValue("learning.maxRecommendations", 10);
        setConfigValue("learning.enableContinuousLearning", true);
        setConfigValue("learning.learningUpdateIntervalMinutes", 60);
        
        // User Behavior Learning 配置
        setConfigValue("learning.behavior.trackSearchPatterns", true);
        setConfigValue("learning.behavior.trackAccessPatterns", true);
        setConfigValue("learning.behavior.trackContentPreferences", true);
        setConfigValue("learning.behavior.trackTimePatterns", true);
        setConfigValue("learning.behavior.maxHistorySize", 10000);
        setConfigValue("learning.behavior.interactionTypes", Arrays.asList(
            "MEMORY_CREATE", "MEMORY_READ", "MEMORY_UPDATE", "MEMORY_DELETE",
            "SEARCH_QUERY", "RECOMMENDATION_CLICK", "MEMORY_SHARE"));
        setConfigValue("learning.behavior.enableSatisfactionTracking", true);
        setConfigValue("learning.behavior.defaultSatisfactionScore", 0.5);
        
        // Pattern Analysis 配置
        setConfigValue("learning.pattern.enableCoOccurrenceAnalysis", true);
        setConfigValue("learning.pattern.enableTemporalClustering", true);
        setConfigValue("learning.pattern.enableTopicCorrelation", true);
        setConfigValue("learning.pattern.enableSequentialAnalysis", true);
        setConfigValue("learning.pattern.enableWorkflowDetection", true);
        setConfigValue("learning.pattern.minPatternStrength", 0.3);
        setConfigValue("learning.pattern.minPatternFrequency", 0.1);
        setConfigValue("learning.pattern.maxPatternsPerUser", 100);
        setConfigValue("learning.pattern.patternExpiryDays", 90);
        
        // Recommendation Engine 配置
        setConfigValue("learning.recommendation.enableTopicBasedRecommendations", true);
        setConfigValue("learning.recommendation.enableContentTypeRecommendations", true);
        setConfigValue("learning.recommendation.enableTimeBasedRecommendations", true);
        setConfigValue("learning.recommendation.enableKeywordRecommendations", true);
        setConfigValue("learning.recommendation.enableCollaborativeFiltering", false);
        setConfigValue("learning.recommendation.recommendationAlgorithms", Arrays.asList(
            "topic_based", "content_type", "time_based", "keyword_based"));
        setConfigValue("learning.recommendation.enableExplanations", true);
        setConfigValue("learning.recommendation.minRecommendationScore", 0.2);
        
        // User Profile 配置
        setConfigValue("learning.profile.enableTopicPreferences", true);
        setConfigValue("learning.profile.enableContentTypePreferences", true);
        setConfigValue("learning.profile.enableSearchPatterns", true);
        setConfigValue("learning.profile.enableTimePatterns", true);
        setConfigValue("learning.profile.enableKeywordTracking", true);
        setConfigValue("learning.profile.maxTopicCategories", 20);
        setConfigValue("learning.profile.maxContentTypes", 10);
        setConfigValue("learning.profile.maxKeywords", 50);
        setConfigValue("learning.profile.profileUpdateThreshold", 10);
        setConfigValue("learning.profile.enableProfilePersistence", true);
        
        // ML Model 配置
        setConfigValue("ml.enableMachineLearning", false);
        setConfigValue("ml.enableDeepLearning", false);
        setConfigValue("ml.enableNeuralNetworks", false);
        setConfigValue("ml.enableReinforcementLearning", false);
        setConfigValue("ml.modelUpdateIntervalHours", 24);
        setConfigValue("ml.enableModelValidation", true);
        setConfigValue("ml.validationSplitRatio", 0.2);
        setConfigValue("ml.enableFeatureSelection", true);
        setConfigValue("ml.maxFeatures", 1000);
        
        // Performance 配置
        setConfigValue("performance.enableAsyncProcessing", true);
        setConfigValue("performance.threadPoolSize", 8);
        setConfigValue("performance.enableCaching", true);
        setConfigValue("performance.cacheSize", 10000);
        setConfigValue("performance.cacheTTLMinutes", 60);
        setConfigValue("performance.enableMetrics", true);
        setConfigValue("performance.enableProfiling", false);
        setConfigValue("performance.maxProcessingTime", 30000);
        setConfigValue("performance.enableLoadBalancing", true);
        
        // Data Management 配置
        setConfigValue("data.enableDataVersioning", true);
        setConfigValue("data.enableDataBackup", true);
        setConfigValue("data.backupIntervalHours", 6);
        setConfigValue("data.maxBackupCount", 10);
        setConfigValue("data.enableDataCompression", true);
        setConfigValue("data.enableDataEncryption", false);
        setConfigValue("data.enableDataValidation", true);
        setConfigValue("data.enableDataCleaning", true);
        setConfigValue("data.dataRetentionDays", 365);
        
        // Security & Privacy 配置
        setConfigValue("security.enablePrivacyProtection", true);
        setConfigValue("security.enableDataAnonymization", false);
        setConfigValue("security.enableAccessControl", false);
        setConfigValue("security.enableAuditLogging", true);
        setConfigValue("security.enableSecureStorage", false);
        setConfigValue("security.enableDataObfuscation", false);
        setConfigValue("security.enableGDPRCompliance", false);
        
        // Monitoring & Alerting 配置
        setConfigValue("monitoring.enableSystemMonitoring", true);
        setConfigValue("monitoring.enablePerformanceMonitoring", true);
        setConfigValue("monitoring.enableErrorTracking", true);
        setConfigValue("monitoring.enableUsageTracking", true);
        setConfigValue("monitoring.monitoringIntervalMinutes", 5);
        setConfigValue("monitoring.enableAlerting", false);
        setConfigValue("monitoring.alertThresholds", Arrays.asList(
            "cpu_usage:80", "memory_usage:80", "error_rate:5"));
    }
    
    @Override
    protected void validateConfiguration() throws IllegalArgumentException {
        // 验证压缩配置
        double compressionThreshold = getDouble("compression.compressionThreshold", 0.0);
        if (compressionThreshold < 0.0 || compressionThreshold > 1.0) {
            throw new IllegalArgumentException("compression.compressionThreshold must be between 0.0 and 1.0");
        }
        
        if (getInt("compression.maxCompressionRatio", 0) <= 0) {
            throw new IllegalArgumentException("compression.maxCompressionRatio must be positive");
        }
        
        double importanceThreshold = getDouble("compression.importanceThreshold", 0.0);
        if (importanceThreshold < 0.0 || importanceThreshold > 1.0) {
            throw new IllegalArgumentException("compression.importanceThreshold must be between 0.0 and 1.0");
        }
        
        // 验证学习配置
        if (getInt("learning.learningWindowDays", 0) <= 0) {
            throw new IllegalArgumentException("learning.learningWindowDays must be positive");
        }
        
        double learningRate = getDouble("learning.learningRate", 0.0);
        if (learningRate < 0.0 || learningRate > 1.0) {
            throw new IllegalArgumentException("learning.learningRate must be between 0.0 and 1.0");
        }
        
        double confidenceThreshold = getDouble("learning.confidenceThreshold", 0.0);
        if (confidenceThreshold < 0.0 || confidenceThreshold > 1.0) {
            throw new IllegalArgumentException("learning.confidenceThreshold must be between 0.0 and 1.0");
        }
        
        // 验证模式分析配置
        double minPatternStrength = getDouble("learning.pattern.minPatternStrength", 0.0);
        if (minPatternStrength < 0.0 || minPatternStrength > 1.0) {
            throw new IllegalArgumentException("learning.pattern.minPatternStrength must be between 0.0 and 1.0");
        }
        
        // 验证性能配置
        if (getInt("performance.threadPoolSize", 0) <= 0) {
            throw new IllegalArgumentException("performance.threadPoolSize must be positive");
        }
        
        if (getInt("performance.maxProcessingTime", 0) <= 0) {
            throw new IllegalArgumentException("performance.maxProcessingTime must be positive");
        }
        
        // 验证ML配置
        if (getDouble("ml.validationSplitRatio", 0.0) < 0.0 || getDouble("ml.validationSplitRatio", 0.0) > 1.0) {
            throw new IllegalArgumentException("ml.validationSplitRatio must be between 0.0 and 1.0");
        }
    }
    
    // Memory Compression 配置访问方法
    public double getCompressionThreshold() {
        return getDouble("compression.compressionThreshold", 0.8);
    }
    
    public int getMaxCompressionRatio() {
        return getInt("compression.maxCompressionRatio", 10);
    }
    
    public boolean isEnableSemanticCompression() {
        return getBoolean("compression.enableSemanticCompression", true);
    }
    
    public boolean isEnableRedundancyRemoval() {
        return getBoolean("compression.enableRedundancyRemoval", true);
    }
    
    public boolean isEnableTemporalCompression() {
        return getBoolean("compression.enableTemporalCompression", true);
    }
    
    public int getTemporalCompressionDays() {
        return getInt("compression.temporalCompressionDays", 30);
    }
    
    public double getImportanceThreshold() {
        return getDouble("compression.importanceThreshold", 0.3);
    }
    
    public int getMaxCompressedMemories() {
        return getInt("compression.maxCompressedMemories", 1000);
    }
    
    public boolean isEnableAutoCompression() {
        return getBoolean("compression.enableAutoCompression", true);
    }
    
    public int getAutoCompressionIntervalHours() {
        return getInt("compression.autoCompressionIntervalHours", 24);
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getCompressionStrategies() {
        return getConfigValue("compression.compressionStrategies", Arrays.asList("semantic", "redundancy", "temporal", "summary"));
    }
    
    public boolean isEnableDecompression() {
        return getBoolean("compression.enableDecompression", true);
    }
    
    public boolean isEnableCompressionMetrics() {
        return getBoolean("compression.enableCompressionMetrics", true);
    }
    
    // Semantic Compression 配置访问方法
    public double getSemanticSimilarityThreshold() {
        return getDouble("compression.semantic.similarityThreshold", 0.85);
    }
    
    public int getSemanticMaxGroupSize() {
        return getInt("compression.semantic.maxGroupSize", 10);
    }
    
    public boolean isEnableContentMerging() {
        return getBoolean("compression.semantic.enableContentMerging", true);
    }
    
    public boolean isPreserveImportantSegments() {
        return getBoolean("compression.semantic.preserveImportantSegments", true);
    }
    
    public int getMaxMergedContentLength() {
        return getInt("compression.semantic.maxMergedContentLength", 2000);
    }
    
    // Adaptive Learning 配置访问方法
    public boolean isEnableBehaviorLearning() {
        return getBoolean("learning.enableBehaviorLearning", true);
    }
    
    public boolean isEnablePatternAnalysis() {
        return getBoolean("learning.enablePatternAnalysis", true);
    }
    
    public boolean isEnableRecommendations() {
        return getBoolean("learning.enableRecommendations", true);
    }
    
    public boolean isEnablePredictiveOptimization() {
        return getBoolean("learning.enablePredictiveOptimization", true);
    }
    
    public int getLearningWindowDays() {
        return getInt("learning.learningWindowDays", 30);
    }
    
    public double getLearningRate() {
        return getDouble("learning.learningRate", 0.1);
    }
    
    public int getMinInteractionsForLearning() {
        return getInt("learning.minInteractionsForLearning", 5);
    }
    
    public double getConfidenceThreshold() {
        return getDouble("learning.confidenceThreshold", 0.7);
    }
    
    public int getMaxRecommendations() {
        return getInt("learning.maxRecommendations", 10);
    }
    
    public boolean isEnableContinuousLearning() {
        return getBoolean("learning.enableContinuousLearning", true);
    }
    
    public int getLearningUpdateIntervalMinutes() {
        return getInt("learning.learningUpdateIntervalMinutes", 60);
    }
    
    // Behavior Learning 配置访问方法
    public boolean isTrackSearchPatterns() {
        return getBoolean("learning.behavior.trackSearchPatterns", true);
    }
    
    public boolean isTrackAccessPatterns() {
        return getBoolean("learning.behavior.trackAccessPatterns", true);
    }
    
    public boolean isTrackContentPreferences() {
        return getBoolean("learning.behavior.trackContentPreferences", true);
    }
    
    public boolean isTrackTimePatterns() {
        return getBoolean("learning.behavior.trackTimePatterns", true);
    }
    
    public int getMaxHistorySize() {
        return getInt("learning.behavior.maxHistorySize", 10000);
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getInteractionTypes() {
        return getConfigValue("learning.behavior.interactionTypes", Arrays.asList(
            "MEMORY_CREATE", "MEMORY_READ", "MEMORY_UPDATE", "MEMORY_DELETE",
            "SEARCH_QUERY", "RECOMMENDATION_CLICK", "MEMORY_SHARE"));
    }
    
    public boolean isEnableSatisfactionTracking() {
        return getBoolean("learning.behavior.enableSatisfactionTracking", true);
    }
    
    public double getDefaultSatisfactionScore() {
        return getDouble("learning.behavior.defaultSatisfactionScore", 0.5);
    }
    
    // Pattern Analysis 配置访问方法
    public boolean isEnableCoOccurrenceAnalysis() {
        return getBoolean("learning.pattern.enableCoOccurrenceAnalysis", true);
    }
    
    public boolean isEnableTemporalClustering() {
        return getBoolean("learning.pattern.enableTemporalClustering", true);
    }
    
    public boolean isEnableTopicCorrelation() {
        return getBoolean("learning.pattern.enableTopicCorrelation", true);
    }
    
    public boolean isEnableSequentialAnalysis() {
        return getBoolean("learning.pattern.enableSequentialAnalysis", true);
    }
    
    public boolean isEnableWorkflowDetection() {
        return getBoolean("learning.pattern.enableWorkflowDetection", true);
    }
    
    public double getMinPatternStrength() {
        return getDouble("learning.pattern.minPatternStrength", 0.3);
    }
    
    public double getMinPatternFrequency() {
        return getDouble("learning.pattern.minPatternFrequency", 0.1);
    }
    
    public int getMaxPatternsPerUser() {
        return getInt("learning.pattern.maxPatternsPerUser", 100);
    }
    
    public int getPatternExpiryDays() {
        return getInt("learning.pattern.patternExpiryDays", 90);
    }
    
    // Recommendation 配置访问方法
    public boolean isEnableTopicBasedRecommendations() {
        return getBoolean("learning.recommendation.enableTopicBasedRecommendations", true);
    }
    
    public boolean isEnableContentTypeRecommendations() {
        return getBoolean("learning.recommendation.enableContentTypeRecommendations", true);
    }
    
    public boolean isEnableTimeBasedRecommendations() {
        return getBoolean("learning.recommendation.enableTimeBasedRecommendations", true);
    }
    
    public boolean isEnableKeywordRecommendations() {
        return getBoolean("learning.recommendation.enableKeywordRecommendations", true);
    }
    
    public boolean isEnableCollaborativeFiltering() {
        return getBoolean("learning.recommendation.enableCollaborativeFiltering", false);
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getRecommendationAlgorithms() {
        return getConfigValue("learning.recommendation.recommendationAlgorithms", Arrays.asList(
            "topic_based", "content_type", "time_based", "keyword_based"));
    }
    
    public boolean isEnableExplanations() {
        return getBoolean("learning.recommendation.enableExplanations", true);
    }
    
    public double getMinRecommendationScore() {
        return getDouble("learning.recommendation.minRecommendationScore", 0.2);
    }
    
    // Performance 配置访问方法
    public boolean isEnableAsyncProcessing() {
        return getBoolean("performance.enableAsyncProcessing", true);
    }
    
    public int getAIThreadPoolSize() {
        return getInt("performance.threadPoolSize", 8);
    }
    
    public boolean isEnableAICaching() {
        return getBoolean("performance.enableCaching", true);
    }
    
    public int getAICacheSize() {
        return getInt("performance.cacheSize", 10000);
    }
    
    public int getAICacheTTLMinutes() {
        return getInt("performance.cacheTTLMinutes", 60);
    }
    
    public boolean isEnableAIMetrics() {
        return getBoolean("performance.enableMetrics", true);
    }
    
    public boolean isEnableAIProfiling() {
        return getBoolean("performance.enableProfiling", false);
    }
    
    public int getMaxProcessingTime() {
        return getInt("performance.maxProcessingTime", 30000);
    }
    
    public boolean isEnableAILoadBalancing() {
        return getBoolean("performance.enableLoadBalancing", true);
    }
    
    // ML Model 配置访问方法
    public boolean isEnableMachineLearning() {
        return getBoolean("ml.enableMachineLearning", false);
    }
    
    public boolean isEnableDeepLearning() {
        return getBoolean("ml.enableDeepLearning", false);
    }
    
    public boolean isEnableNeuralNetworks() {
        return getBoolean("ml.enableNeuralNetworks", false);
    }
    
    public boolean isEnableReinforcementLearning() {
        return getBoolean("ml.enableReinforcementLearning", false);
    }
    
    public int getModelUpdateIntervalHours() {
        return getInt("ml.modelUpdateIntervalHours", 24);
    }
    
    public boolean isEnableModelValidation() {
        return getBoolean("ml.enableModelValidation", true);
    }
    
    public double getValidationSplitRatio() {
        return getDouble("ml.validationSplitRatio", 0.2);
    }
    
    public boolean isEnableFeatureSelection() {
        return getBoolean("ml.enableFeatureSelection", true);
    }
    
    public int getMaxFeatures() {
        return getInt("ml.maxFeatures", 1000);
    }
}