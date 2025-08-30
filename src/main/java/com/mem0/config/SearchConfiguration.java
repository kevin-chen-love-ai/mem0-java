package com.mem0.config;

import java.util.Arrays;
import java.util.List;

/**
 * SearchConfiguration - 搜索系统配置
 * 
 * 集中管理所有搜索相关的配置参数。
 * 包括语义搜索、混合搜索、搜索过滤器的配置。
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 2024-12-20
 */
public class SearchConfiguration extends BaseConfiguration {
    
    public SearchConfiguration() {
        super("mem0.search");
    }
    
    @Override
    protected void loadDefaultConfiguration() {
        // Semantic Search 配置
        setConfigValue("semantic.vectorDimension", 512);
        setConfigValue("semantic.enableEmbedding", true);
        setConfigValue("semantic.enableQueryExpansion", true);
        setConfigValue("semantic.enableContextualSearch", true);
        setConfigValue("semantic.enableRanking", true);
        setConfigValue("semantic.maxResults", 50);
        setConfigValue("semantic.relevanceThreshold", 0.3);
        setConfigValue("semantic.rankingAlpha", 0.6);
        setConfigValue("semantic.rankingBeta", 0.3);
        setConfigValue("semantic.rankingGamma", 0.1);
        setConfigValue("semantic.cacheSize", 10000);
        setConfigValue("semantic.cacheTTLMinutes", 30);
        setConfigValue("semantic.enablePerformanceOptimization", true);
        
        // Hybrid Search 配置
        setConfigValue("hybrid.semanticWeight", 0.6);
        setConfigValue("hybrid.keywordWeight", 0.3);
        setConfigValue("hybrid.fuzzyWeight", 0.1);
        setConfigValue("hybrid.maxResults", 50);
        setConfigValue("hybrid.relevanceThreshold", 0.3);
        setConfigValue("hybrid.enablePersonalization", true);
        setConfigValue("hybrid.enablePerformanceOptimization", true);
        setConfigValue("hybrid.cacheSize", 1000);
        
        // Search Filter 配置
        setConfigValue("filter.enableIndexOptimization", true);
        setConfigValue("filter.enableParallelProcessing", true);
        setConfigValue("filter.parallelThreshold", 100);
        setConfigValue("filter.enableStatistics", true);
        setConfigValue("filter.maxFilterResults", 1000);
        setConfigValue("filter.defaultRelevanceThreshold", 0.0);
        
        // 查询处理配置
        setConfigValue("query.enablePreprocessing", true);
        setConfigValue("query.enableSynonymExpansion", true);
        setConfigValue("query.enableSpellCorrection", false);
        setConfigValue("query.maxQueryLength", 500);
        setConfigValue("query.minQueryLength", 1);
        setConfigValue("query.enableQueryLogging", true);
        setConfigValue("query.expansionMaxTerms", 10);
        
        // 索引配置
        setConfigValue("index.enableAutoUpdate", true);
        setConfigValue("index.updateIntervalMinutes", 10);
        setConfigValue("index.enableCompression", true);
        setConfigValue("index.maxIndexSize", 100000);
        setConfigValue("index.enableBackup", true);
        setConfigValue("index.backupIntervalHours", 24);
        
        // 排名配置
        setConfigValue("ranking.enableMLRanking", false);
        setConfigValue("ranking.enablePersonalizedRanking", true);
        setConfigValue("ranking.enableTemporalRanking", true);
        setConfigValue("ranking.temporalDecayRate", 0.05);
        setConfigValue("ranking.importanceWeight", 0.4);
        setConfigValue("ranking.recencyWeight", 0.3);
        setConfigValue("ranking.personalizedWeight", 0.3);
        
        // 性能配置
        setConfigValue("performance.enableMetrics", true);
        setConfigValue("performance.enableProfiling", false);
        setConfigValue("performance.searchTimeoutMs", 10000);
        setConfigValue("performance.maxConcurrentSearches", 100);
        setConfigValue("performance.enableResultCaching", true);
        setConfigValue("performance.resultCacheTTLMinutes", 15);
        
        // 高级功能配置
        setConfigValue("advanced.enableFacetedSearch", true);
        setConfigValue("advanced.enableSearchSuggestions", true);
        setConfigValue("advanced.enableAutoComplete", true);
        setConfigValue("advanced.enableSearchHistory", true);
        setConfigValue("advanced.maxSearchHistory", 100);
        setConfigValue("advanced.enableRelevanceFeedback", true);
    }
    
    @Override
    protected void validateConfiguration() throws IllegalArgumentException {
        // 验证语义搜索配置
        if (getInt("semantic.vectorDimension", 0) <= 0) {
            throw new IllegalArgumentException("semantic.vectorDimension must be positive");
        }
        
        if (getInt("semantic.maxResults", 0) <= 0) {
            throw new IllegalArgumentException("semantic.maxResults must be positive");
        }
        
        double relevanceThreshold = getDouble("semantic.relevanceThreshold", 0.0);
        if (relevanceThreshold < 0.0 || relevanceThreshold > 1.0) {
            throw new IllegalArgumentException("semantic.relevanceThreshold must be between 0.0 and 1.0");
        }
        
        // 验证混合搜索配置
        double semanticWeight = getDouble("hybrid.semanticWeight", 0.0);
        double keywordWeight = getDouble("hybrid.keywordWeight", 0.0);
        double fuzzyWeight = getDouble("hybrid.fuzzyWeight", 0.0);
        
        if (semanticWeight < 0.0 || semanticWeight > 1.0) {
            throw new IllegalArgumentException("hybrid.semanticWeight must be between 0.0 and 1.0");
        }
        if (keywordWeight < 0.0 || keywordWeight > 1.0) {
            throw new IllegalArgumentException("hybrid.keywordWeight must be between 0.0 and 1.0");
        }
        if (fuzzyWeight < 0.0 || fuzzyWeight > 1.0) {
            throw new IllegalArgumentException("hybrid.fuzzyWeight must be between 0.0 and 1.0");
        }
        
        double totalWeight = semanticWeight + keywordWeight + fuzzyWeight;
        if (Math.abs(totalWeight - 1.0) > 0.01) {
            throw new IllegalArgumentException("Sum of hybrid search weights must equal 1.0");
        }
        
        // 验证查询处理配置
        int maxQueryLength = getInt("query.maxQueryLength", 0);
        int minQueryLength = getInt("query.minQueryLength", 0);
        
        if (maxQueryLength <= 0) {
            throw new IllegalArgumentException("query.maxQueryLength must be positive");
        }
        if (minQueryLength < 0) {
            throw new IllegalArgumentException("query.minQueryLength cannot be negative");
        }
        if (minQueryLength >= maxQueryLength) {
            throw new IllegalArgumentException("query.minQueryLength must be less than maxQueryLength");
        }
        
        // 验证性能配置
        if (getInt("performance.searchTimeoutMs", 0) <= 0) {
            throw new IllegalArgumentException("performance.searchTimeoutMs must be positive");
        }
        
        if (getInt("performance.maxConcurrentSearches", 0) <= 0) {
            throw new IllegalArgumentException("performance.maxConcurrentSearches must be positive");
        }
    }
    
    // Semantic Search 配置访问方法
    public int getVectorDimension() {
        return getInt("semantic.vectorDimension", 512);
    }
    
    public boolean isEnableEmbedding() {
        return getBoolean("semantic.enableEmbedding", true);
    }
    
    public boolean isEnableQueryExpansion() {
        return getBoolean("semantic.enableQueryExpansion", true);
    }
    
    public boolean isEnableContextualSearch() {
        return getBoolean("semantic.enableContextualSearch", true);
    }
    
    public boolean isEnableRanking() {
        return getBoolean("semantic.enableRanking", true);
    }
    
    public int getSemanticMaxResults() {
        return getInt("semantic.maxResults", 50);
    }
    
    public double getSemanticRelevanceThreshold() {
        return getDouble("semantic.relevanceThreshold", 0.3);
    }
    
    public double getRankingAlpha() {
        return getDouble("semantic.rankingAlpha", 0.6);
    }
    
    public double getRankingBeta() {
        return getDouble("semantic.rankingBeta", 0.3);
    }
    
    public double getRankingGamma() {
        return getDouble("semantic.rankingGamma", 0.1);
    }
    
    public int getSemanticCacheSize() {
        return getInt("semantic.cacheSize", 10000);
    }
    
    public int getSemanticCacheTTLMinutes() {
        return getInt("semantic.cacheTTLMinutes", 30);
    }
    
    public boolean isEnableSemanticPerformanceOptimization() {
        return getBoolean("semantic.enablePerformanceOptimization", true);
    }
    
    // Hybrid Search 配置访问方法
    public double getSemanticWeight() {
        return getDouble("hybrid.semanticWeight", 0.6);
    }
    
    public double getKeywordWeight() {
        return getDouble("hybrid.keywordWeight", 0.3);
    }
    
    public double getFuzzyWeight() {
        return getDouble("hybrid.fuzzyWeight", 0.1);
    }
    
    public int getHybridMaxResults() {
        return getInt("hybrid.maxResults", 50);
    }
    
    public double getHybridRelevanceThreshold() {
        return getDouble("hybrid.relevanceThreshold", 0.3);
    }
    
    public boolean isEnablePersonalization() {
        return getBoolean("hybrid.enablePersonalization", true);
    }
    
    public boolean isEnableHybridPerformanceOptimization() {
        return getBoolean("hybrid.enablePerformanceOptimization", true);
    }
    
    public int getHybridCacheSize() {
        return getInt("hybrid.cacheSize", 1000);
    }
    
    // Search Filter 配置访问方法
    public boolean isEnableIndexOptimization() {
        return getBoolean("filter.enableIndexOptimization", true);
    }
    
    public boolean isEnableParallelProcessing() {
        return getBoolean("filter.enableParallelProcessing", true);
    }
    
    public int getParallelThreshold() {
        return getInt("filter.parallelThreshold", 100);
    }
    
    public boolean isEnableStatistics() {
        return getBoolean("filter.enableStatistics", true);
    }
    
    public int getMaxFilterResults() {
        return getInt("filter.maxFilterResults", 1000);
    }
    
    public double getDefaultRelevanceThreshold() {
        return getDouble("filter.defaultRelevanceThreshold", 0.0);
    }
    
    // Query Processing 配置访问方法
    public boolean isEnablePreprocessing() {
        return getBoolean("query.enablePreprocessing", true);
    }
    
    public boolean isEnableSynonymExpansion() {
        return getBoolean("query.enableSynonymExpansion", true);
    }
    
    public boolean isEnableSpellCorrection() {
        return getBoolean("query.enableSpellCorrection", false);
    }
    
    public int getMaxQueryLength() {
        return getInt("query.maxQueryLength", 500);
    }
    
    public int getMinQueryLength() {
        return getInt("query.minQueryLength", 1);
    }
    
    public boolean isEnableQueryLogging() {
        return getBoolean("query.enableQueryLogging", true);
    }
    
    public int getExpansionMaxTerms() {
        return getInt("query.expansionMaxTerms", 10);
    }
    
    // Index 配置访问方法
    public boolean isEnableAutoUpdate() {
        return getBoolean("index.enableAutoUpdate", true);
    }
    
    public int getUpdateIntervalMinutes() {
        return getInt("index.updateIntervalMinutes", 10);
    }
    
    public boolean isEnableCompression() {
        return getBoolean("index.enableCompression", true);
    }
    
    public int getMaxIndexSize() {
        return getInt("index.maxIndexSize", 100000);
    }
    
    public boolean isEnableBackup() {
        return getBoolean("index.enableBackup", true);
    }
    
    public int getBackupIntervalHours() {
        return getInt("index.backupIntervalHours", 24);
    }
    
    // Ranking 配置访问方法
    public boolean isEnableMLRanking() {
        return getBoolean("ranking.enableMLRanking", false);
    }
    
    public boolean isEnablePersonalizedRanking() {
        return getBoolean("ranking.enablePersonalizedRanking", true);
    }
    
    public boolean isEnableTemporalRanking() {
        return getBoolean("ranking.enableTemporalRanking", true);
    }
    
    public double getTemporalDecayRate() {
        return getDouble("ranking.temporalDecayRate", 0.05);
    }
    
    public double getImportanceWeight() {
        return getDouble("ranking.importanceWeight", 0.4);
    }
    
    public double getRecencyWeight() {
        return getDouble("ranking.recencyWeight", 0.3);
    }
    
    public double getPersonalizedWeight() {
        return getDouble("ranking.personalizedWeight", 0.3);
    }
    
    // Performance 配置访问方法
    public boolean isEnableMetrics() {
        return getBoolean("performance.enableMetrics", true);
    }
    
    public boolean isEnableProfiling() {
        return getBoolean("performance.enableProfiling", false);
    }
    
    public int getSearchTimeoutMs() {
        return getInt("performance.searchTimeoutMs", 10000);
    }
    
    public int getMaxConcurrentSearches() {
        return getInt("performance.maxConcurrentSearches", 100);
    }
    
    public boolean isEnableResultCaching() {
        return getBoolean("performance.enableResultCaching", true);
    }
    
    public int getResultCacheTTLMinutes() {
        return getInt("performance.resultCacheTTLMinutes", 15);
    }
    
    // Advanced Features 配置访问方法
    public boolean isEnableFacetedSearch() {
        return getBoolean("advanced.enableFacetedSearch", true);
    }
    
    public boolean isEnableSearchSuggestions() {
        return getBoolean("advanced.enableSearchSuggestions", true);
    }
    
    public boolean isEnableAutoComplete() {
        return getBoolean("advanced.enableAutoComplete", true);
    }
    
    public boolean isEnableSearchHistory() {
        return getBoolean("advanced.enableSearchHistory", true);
    }
    
    public int getMaxSearchHistory() {
        return getInt("advanced.maxSearchHistory", 100);
    }
    
    public boolean isEnableRelevanceFeedback() {
        return getBoolean("advanced.enableRelevanceFeedback", true);
    }
}