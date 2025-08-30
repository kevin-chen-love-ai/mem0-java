package com.mem0.search;

import com.mem0.memory.Memory;
import com.mem0.core.EnhancedMemory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * HybridSearchEngine - 混合搜索引擎
 * 
 * 结合语义搜索和传统搜索的优势，提供更全面和准确的搜索结果。
 * 支持多种搜索策略的智能融合，动态权重调整和性能优化。
 * 
 * 主要功能：
 * 1. 混合搜索策略 - 语义搜索 + 关键词搜索 + 模糊搜索
 * 2. 智能结果融合 - 多维度评分和排名算法
 * 3. 搜索策略优化 - 基于历史表现的动态权重调整
 * 4. 实时性能监控 - 搜索质量和响应时间统计
 * 5. 个性化搜索 - 基于用户行为的搜索偏好学习
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 2024-12-20
 */
public class HybridSearchEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(HybridSearchEngine.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // 搜索引擎组件
    private final SemanticSearchEngine semanticEngine;
    private final Map<String, List<Memory>> keywordIndex;
    private final Map<String, SearchStrategy> searchStrategies;
    private final SearchPerformanceMonitor performanceMonitor;
    
    // 配置参数
    private final HybridSearchConfiguration configuration;
    
    // 性能统计
    private final AtomicLong totalSearchCount = new AtomicLong(0);
    private final Map<String, Double> strategyPerformance = new ConcurrentHashMap<>();
    
    /**
     * 混合搜索配置
     */
    public static class HybridSearchConfiguration {
        private double semanticWeight = 0.6;
        private double keywordWeight = 0.3;
        private double fuzzyWeight = 0.1;
        private int maxResults = 50;
        private double relevanceThreshold = 0.3;
        private boolean enablePersonalization = true;
        private boolean enablePerformanceOptimization = true;
        private int cacheSize = 1000;
        
        // Getters and setters
        public double getSemanticWeight() { return semanticWeight; }
        public void setSemanticWeight(double semanticWeight) { this.semanticWeight = semanticWeight; }
        
        public double getKeywordWeight() { return keywordWeight; }
        public void setKeywordWeight(double keywordWeight) { this.keywordWeight = keywordWeight; }
        
        public double getFuzzyWeight() { return fuzzyWeight; }
        public void setFuzzyWeight(double fuzzyWeight) { this.fuzzyWeight = fuzzyWeight; }
        
        public int getMaxResults() { return maxResults; }
        public void setMaxResults(int maxResults) { this.maxResults = maxResults; }
        
        public double getRelevanceThreshold() { return relevanceThreshold; }
        public void setRelevanceThreshold(double relevanceThreshold) { this.relevanceThreshold = relevanceThreshold; }
        
        public boolean isEnablePersonalization() { return enablePersonalization; }
        public void setEnablePersonalization(boolean enablePersonalization) { this.enablePersonalization = enablePersonalization; }
        
        public boolean isEnablePerformanceOptimization() { return enablePerformanceOptimization; }
        public void setEnablePerformanceOptimization(boolean enablePerformanceOptimization) { this.enablePerformanceOptimization = enablePerformanceOptimization; }
        
        public int getCacheSize() { return cacheSize; }
        public void setCacheSize(int cacheSize) { this.cacheSize = cacheSize; }
    }
    
    /**
     * 搜索策略接口
     */
    public interface SearchStrategy {
        /**
         * 执行搜索
         * 
         * @param query 查询内容
         * @param memories 内存数据
         * @param context 搜索上下文
         * @return 搜索结果
         */
        List<HybridSearchResult.SearchResultItem> search(String query, List<Memory> memories, SearchContext context);
        
        /**
         * 获取策略名称
         * 
         * @return 策略名称
         */
        String getStrategyName();
        
        /**
         * 获取策略权重
         * 
         * @return 权重值
         */
        double getWeight();
        
        /**
         * 更新策略权重
         * 
         * @param weight 新权重值
         */
        void setWeight(double weight);
    }
    
    /**
     * 混合搜索结果
     */
    public static class HybridSearchResult {
        private final List<SearchResultItem> results;
        private final Map<String, Double> strategyContributions;
        private final SearchStatistics statistics;
        private final String searchId;
        private final LocalDateTime timestamp;
        
        public HybridSearchResult(List<SearchResultItem> results, 
                                Map<String, Double> strategyContributions,
                                SearchStatistics statistics) {
            this.results = results;
            this.strategyContributions = strategyContributions;
            this.statistics = statistics;
            this.searchId = UUID.randomUUID().toString();
            this.timestamp = LocalDateTime.now();
        }
        
        public static class SearchResultItem {
            private final Memory memory;
            private final double relevanceScore;
            private final double semanticScore;
            private final double keywordScore;
            private final double fuzzyScore;
            private final String matchType;
            private final List<String> matchedTerms;
            private final Map<String, Object> debugInfo;
            
            public SearchResultItem(Memory memory, double relevanceScore, double semanticScore,
                                  double keywordScore, double fuzzyScore, String matchType,
                                  List<String> matchedTerms, Map<String, Object> debugInfo) {
                this.memory = memory;
                this.relevanceScore = relevanceScore;
                this.semanticScore = semanticScore;
                this.keywordScore = keywordScore;
                this.fuzzyScore = fuzzyScore;
                this.matchType = matchType;
                this.matchedTerms = matchedTerms;
                this.debugInfo = debugInfo;
            }
            
            // Getters
            public Memory getMemory() { return memory; }
            public double getRelevanceScore() { return relevanceScore; }
            public double getSemanticScore() { return semanticScore; }
            public double getKeywordScore() { return keywordScore; }
            public double getFuzzyScore() { return fuzzyScore; }
            public String getMatchType() { return matchType; }
            public List<String> getMatchedTerms() { return matchedTerms; }
            public Map<String, Object> getDebugInfo() { return debugInfo; }
        }
        
        public static class SearchStatistics {
            private final long totalProcessingTime;
            private final long semanticSearchTime;
            private final long keywordSearchTime;
            private final long fuzzySearchTime;
            private final long resultFusionTime;
            private final int totalCandidates;
            private final int filteredResults;
            private final Map<String, Integer> strategyResultCounts;
            
            public SearchStatistics(long totalProcessingTime, long semanticSearchTime,
                                  long keywordSearchTime, long fuzzySearchTime,
                                  long resultFusionTime, int totalCandidates,
                                  int filteredResults, Map<String, Integer> strategyResultCounts) {
                this.totalProcessingTime = totalProcessingTime;
                this.semanticSearchTime = semanticSearchTime;
                this.keywordSearchTime = keywordSearchTime;
                this.fuzzySearchTime = fuzzySearchTime;
                this.resultFusionTime = resultFusionTime;
                this.totalCandidates = totalCandidates;
                this.filteredResults = filteredResults;
                this.strategyResultCounts = strategyResultCounts;
            }
            
            // Getters
            public long getTotalProcessingTime() { return totalProcessingTime; }
            public long getSemanticSearchTime() { return semanticSearchTime; }
            public long getKeywordSearchTime() { return keywordSearchTime; }
            public long getFuzzySearchTime() { return fuzzySearchTime; }
            public long getResultFusionTime() { return resultFusionTime; }
            public int getTotalCandidates() { return totalCandidates; }
            public int getFilteredResults() { return filteredResults; }
            public Map<String, Integer> getStrategyResultCounts() { return strategyResultCounts; }
        }
        
        // Getters
        public List<SearchResultItem> getResults() { return results; }
        public Map<String, Double> getStrategyContributions() { return strategyContributions; }
        public SearchStatistics getStatistics() { return statistics; }
        public String getSearchId() { return searchId; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    /**
     * 搜索上下文
     */
    public static class SearchContext {
        private final String userId;
        private final String sessionId;
        private final Map<String, Object> userPreferences;
        private final List<String> searchHistory;
        private final Map<String, Double> categoryWeights;
        private final boolean enableDebug;
        
        public SearchContext(String userId, String sessionId, Map<String, Object> userPreferences,
                           List<String> searchHistory, Map<String, Double> categoryWeights,
                           boolean enableDebug) {
            this.userId = userId;
            this.sessionId = sessionId;
            this.userPreferences = userPreferences != null ? userPreferences : new HashMap<>();
            this.searchHistory = searchHistory != null ? searchHistory : new ArrayList<>();
            this.categoryWeights = categoryWeights != null ? categoryWeights : new HashMap<>();
            this.enableDebug = enableDebug;
        }
        
        // Getters
        public String getUserId() { return userId; }
        public String getSessionId() { return sessionId; }
        public Map<String, Object> getUserPreferences() { return userPreferences; }
        public List<String> getSearchHistory() { return searchHistory; }
        public Map<String, Double> getCategoryWeights() { return categoryWeights; }
        public boolean isEnableDebug() { return enableDebug; }
    }
    
    /**
     * 性能监控器
     */
    public static class SearchPerformanceMonitor {
        private final Map<String, List<Long>> responseTimeHistory = new ConcurrentHashMap<>();
        private final Map<String, List<Double>> relevanceHistory = new ConcurrentHashMap<>();
        private final Map<String, AtomicLong> strategyUsageCount = new ConcurrentHashMap<>();
        
        public void recordSearch(String strategy, long responseTime, double averageRelevance) {
            responseTimeHistory.computeIfAbsent(strategy, k -> new ArrayList<>()).add(responseTime);
            relevanceHistory.computeIfAbsent(strategy, k -> new ArrayList<>()).add(averageRelevance);
            strategyUsageCount.computeIfAbsent(strategy, k -> new AtomicLong(0)).incrementAndGet();
        }
        
        public double getAverageResponseTime(String strategy) {
            List<Long> times = responseTimeHistory.get(strategy);
            return times != null && !times.isEmpty() ? 
                times.stream().mapToLong(Long::longValue).average().orElse(0.0) : 0.0;
        }
        
        public double getAverageRelevance(String strategy) {
            List<Double> relevances = relevanceHistory.get(strategy);
            return relevances != null && !relevances.isEmpty() ?
                relevances.stream().mapToDouble(Double::doubleValue).average().orElse(0.0) : 0.0;
        }
        
        public long getUsageCount(String strategy) {
            AtomicLong count = strategyUsageCount.get(strategy);
            return count != null ? count.get() : 0;
        }
        
        public Map<String, Object> getPerformanceReport() {
            Map<String, Object> report = new HashMap<>();
            Set<String> strategies = new HashSet<>();
            strategies.addAll(responseTimeHistory.keySet());
            strategies.addAll(relevanceHistory.keySet());
            
            for (String strategy : strategies) {
                Map<String, Object> strategyReport = new HashMap<>();
                strategyReport.put("averageResponseTime", getAverageResponseTime(strategy));
                strategyReport.put("averageRelevance", getAverageRelevance(strategy));
                strategyReport.put("usageCount", getUsageCount(strategy));
                report.put(strategy, strategyReport);
            }
            
            return report;
        }
    }
    
    /**
     * 构造函数
     * 
     * @param semanticEngine 语义搜索引擎
     * @param configuration 搜索配置
     */
    public HybridSearchEngine(SemanticSearchEngine semanticEngine, HybridSearchConfiguration configuration) {
        this.semanticEngine = semanticEngine;
        this.configuration = configuration != null ? configuration : new HybridSearchConfiguration();
        this.keywordIndex = new ConcurrentHashMap<>();
        this.searchStrategies = new ConcurrentHashMap<>();
        this.performanceMonitor = new SearchPerformanceMonitor();
        
        // 初始化搜索策略
        initializeSearchStrategies();
        
        logger.info("HybridSearchEngine initialized with configuration: {}", 
                   this.configuration.toString());
    }
    
    /**
     * 初始化搜索策略
     */
    private void initializeSearchStrategies() {
        // 语义搜索策略
        searchStrategies.put("semantic", new SemanticSearchStrategy());
        
        // 关键词搜索策略
        searchStrategies.put("keyword", new KeywordSearchStrategy());
        
        // 模糊搜索策略
        searchStrategies.put("fuzzy", new FuzzySearchStrategy());
        
        logger.info("Initialized {} search strategies", searchStrategies.size());
    }
    
    /**
     * 构建搜索索引
     * 
     * @param memories 内存数据列表
     * @return 异步构建任务
     */
    public CompletableFuture<Void> buildSearchIndex(List<Memory> memories) {
        return CompletableFuture.runAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                // 转换Memory到EnhancedMemory
                List<EnhancedMemory> enhancedMemories = memories.stream()
                    .map(memory -> new EnhancedMemory(
                        memory.getId(),
                        memory.getContent(),
                        memory.getUserId(),
                        memory.getMetadata() != null ? memory.getMetadata() : new java.util.HashMap<>()
                    ))
                    .collect(java.util.stream.Collectors.toList());
                
                // 构建语义搜索索引
                semanticEngine.buildSearchIndex(enhancedMemories).join();
                
                // 构建关键词索引
                buildKeywordIndex(memories);
                
                long duration = System.currentTimeMillis() - startTime;
                logger.info("Built hybrid search index for {} memories in {}ms", 
                           memories.size(), duration);
                
            } catch (Exception e) {
                logger.error("Error building hybrid search index", e);
                throw new RuntimeException("Failed to build search index", e);
            }
        });
    }
    
    /**
     * 构建关键词索引
     * 
     * @param memories 内存数据列表
     */
    private void buildKeywordIndex(List<Memory> memories) {
        keywordIndex.clear();
        
        for (Memory memory : memories) {
            String content = memory.getContent().toLowerCase();
            String[] words = content.split("\\s+");
            
            for (String word : words) {
                // 清理单词
                String cleanWord = word.replaceAll("[^a-zA-Z0-9\u4e00-\u9fa5]", "");
                if (cleanWord.length() > 1) {
                    keywordIndex.computeIfAbsent(cleanWord, k -> new ArrayList<>()).add(memory);
                }
            }
        }
        
        logger.info("Built keyword index with {} unique terms", keywordIndex.size());
    }
    
    /**
     * 执行混合搜索
     * 
     * @param query 查询内容
     * @param memories 内存数据列表
     * @return 搜索结果
     */
    public CompletableFuture<HybridSearchResult> search(String query, List<Memory> memories) {
        return search(query, memories, null);
    }
    
    /**
     * 执行混合搜索
     * 
     * @param query 查询内容
     * @param memories 内存数据列表
     * @param context 搜索上下文
     * @return 搜索结果
     */
    public CompletableFuture<HybridSearchResult> search(String query, List<Memory> memories, SearchContext context) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            totalSearchCount.incrementAndGet();
            
            final SearchContext finalContext = (context == null) 
                ? new SearchContext(null, null, null, null, null, false) 
                : context;
            
            try {
                // 执行各种搜索策略
                Map<String, List<HybridSearchResult.SearchResultItem>> strategyResults = new HashMap<>();
                Map<String, Long> strategyTimings = new HashMap<>();
                
                for (Map.Entry<String, SearchStrategy> entry : searchStrategies.entrySet()) {
                    String strategyName = entry.getKey();
                    SearchStrategy strategy = entry.getValue();
                    
                    long strategyStartTime = System.currentTimeMillis();
                    List<HybridSearchResult.SearchResultItem> results = 
                        strategy.search(query, memories, finalContext);
                    long strategyDuration = System.currentTimeMillis() - strategyStartTime;
                    
                    strategyResults.put(strategyName, results);
                    strategyTimings.put(strategyName, strategyDuration);
                    
                    // 记录策略性能
                    double avgRelevance = results.stream()
                        .mapToDouble(HybridSearchResult.SearchResultItem::getRelevanceScore)
                        .average().orElse(0.0);
                    performanceMonitor.recordSearch(strategyName, strategyDuration, avgRelevance);
                }
                
                // 融合搜索结果
                long fusionStartTime = System.currentTimeMillis();
                List<HybridSearchResult.SearchResultItem> fusedResults = 
                    fuseSearchResults(strategyResults, finalContext);
                long fusionDuration = System.currentTimeMillis() - fusionStartTime;
                
                // 计算策略贡献度
                Map<String, Double> contributions = calculateStrategyContributions(strategyResults, fusedResults);
                
                // 统计信息
                Map<String, Integer> strategyResultCounts = strategyResults.entrySet().stream()
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().size()
                    ));
                
                HybridSearchResult.SearchStatistics statistics = 
                    new HybridSearchResult.SearchStatistics(
                        System.currentTimeMillis() - startTime,
                        strategyTimings.getOrDefault("semantic", 0L),
                        strategyTimings.getOrDefault("keyword", 0L),
                        strategyTimings.getOrDefault("fuzzy", 0L),
                        fusionDuration,
                        memories.size(),
                        fusedResults.size(),
                        strategyResultCounts
                    );
                
                // 性能优化
                if (configuration.isEnablePerformanceOptimization()) {
                    optimizeStrategyWeights();
                }
                
                HybridSearchResult result = new HybridSearchResult(fusedResults, contributions, statistics);
                
                logger.debug("Hybrid search completed for query '{}' in {}ms, found {} results",
                           query, statistics.getTotalProcessingTime(), fusedResults.size());
                
                return result;
                
            } catch (Exception e) {
                logger.error("Error in hybrid search for query: " + query, e);
                throw new RuntimeException("Hybrid search failed", e);
            }
        });
    }
    
    /**
     * 融合多个搜索策略的结果
     * 
     * @param strategyResults 各策略的搜索结果
     * @param context 搜索上下文
     * @return 融合后的结果
     */
    private List<HybridSearchResult.SearchResultItem> fuseSearchResults(
            Map<String, List<HybridSearchResult.SearchResultItem>> strategyResults,
            SearchContext context) {
        
        Map<String, HybridSearchResult.SearchResultItem> resultMap = new HashMap<>();
        
        // 合并所有结果
        for (Map.Entry<String, List<HybridSearchResult.SearchResultItem>> entry : strategyResults.entrySet()) {
            String strategyName = entry.getKey();
            double strategyWeight = getStrategyWeight(strategyName);
            
            for (HybridSearchResult.SearchResultItem item : entry.getValue()) {
                String memoryId = item.getMemory().getId();
                
                if (resultMap.containsKey(memoryId)) {
                    // 已存在，合并评分
                    HybridSearchResult.SearchResultItem existing = resultMap.get(memoryId);
                    double newScore = combineScores(existing.getRelevanceScore(), 
                                                   item.getRelevanceScore(), strategyWeight);
                    
                    // 创建新的合并结果项
                    resultMap.put(memoryId, createMergedResultItem(existing, item, newScore));
                } else {
                    // 新结果，直接添加
                    resultMap.put(memoryId, item);
                }
            }
        }
        
        // 排序和过滤
        return resultMap.values().stream()
            .filter(item -> item.getRelevanceScore() >= configuration.getRelevanceThreshold())
            .sorted((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()))
            .limit(configuration.getMaxResults())
            .collect(Collectors.toList());
    }
    
    /**
     * 合并两个结果项
     * 
     * @param existing 已存在的结果项
     * @param newItem 新的结果项
     * @param combinedScore 合并后的评分
     * @return 合并后的结果项
     */
    private HybridSearchResult.SearchResultItem createMergedResultItem(
            HybridSearchResult.SearchResultItem existing,
            HybridSearchResult.SearchResultItem newItem,
            double combinedScore) {
        
        List<String> combinedTerms = new ArrayList<>(existing.getMatchedTerms());
        combinedTerms.addAll(newItem.getMatchedTerms());
        
        Map<String, Object> combinedDebugInfo = new HashMap<>(existing.getDebugInfo());
        combinedDebugInfo.putAll(newItem.getDebugInfo());
        
        return new HybridSearchResult.SearchResultItem(
            existing.getMemory(),
            combinedScore,
            Math.max(existing.getSemanticScore(), newItem.getSemanticScore()),
            Math.max(existing.getKeywordScore(), newItem.getKeywordScore()),
            Math.max(existing.getFuzzyScore(), newItem.getFuzzyScore()),
            "hybrid",
            combinedTerms.stream().distinct().collect(Collectors.toList()),
            combinedDebugInfo
        );
    }
    
    /**
     * 合并评分
     * 
     * @param score1 评分1
     * @param score2 评分2
     * @param weight 权重
     * @return 合并后的评分
     */
    private double combineScores(double score1, double score2, double weight) {
        return Math.max(score1, score2 * weight);
    }
    
    /**
     * 获取策略权重
     * 
     * @param strategyName 策略名称
     * @return 权重值
     */
    private double getStrategyWeight(String strategyName) {
        SearchStrategy strategy = searchStrategies.get(strategyName);
        return strategy != null ? strategy.getWeight() : 0.0;
    }
    
    /**
     * 计算策略贡献度
     * 
     * @param strategyResults 策略结果
     * @param finalResults 最终结果
     * @return 贡献度映射
     */
    private Map<String, Double> calculateStrategyContributions(
            Map<String, List<HybridSearchResult.SearchResultItem>> strategyResults,
            List<HybridSearchResult.SearchResultItem> finalResults) {
        
        Map<String, Double> contributions = new HashMap<>();
        
        for (String strategy : strategyResults.keySet()) {
            long contributedCount = finalResults.stream()
                .filter(result -> strategyResults.get(strategy).stream()
                    .anyMatch(item -> item.getMemory().getId().equals(result.getMemory().getId())))
                .count();
            
            double contribution = finalResults.isEmpty() ? 0.0 : 
                (double) contributedCount / finalResults.size();
            contributions.put(strategy, contribution);
        }
        
        return contributions;
    }
    
    /**
     * 优化策略权重
     */
    private void optimizeStrategyWeights() {
        Map<String, Object> performanceReport = performanceMonitor.getPerformanceReport();
        
        for (Map.Entry<String, Object> entry : performanceReport.entrySet()) {
            String strategy = entry.getKey();
            @SuppressWarnings("unchecked")
            Map<String, Object> metrics = (Map<String, Object>) entry.getValue();
            
            double relevance = (Double) metrics.get("averageRelevance");
            double responseTime = (Double) metrics.get("averageResponseTime");
            
            // 基于性能调整权重
            SearchStrategy searchStrategy = searchStrategies.get(strategy);
            if (searchStrategy != null) {
                double currentWeight = searchStrategy.getWeight();
                double performanceScore = relevance / Math.max(responseTime / 1000.0, 0.1);
                double newWeight = currentWeight * (1 + performanceScore * 0.1);
                
                // 限制权重范围
                newWeight = Math.max(0.1, Math.min(1.0, newWeight));
                searchStrategy.setWeight(newWeight);
            }
        }
    }
    
    // 搜索策略实现类
    
    /**
     * 语义搜索策略
     */
    private class SemanticSearchStrategy implements SearchStrategy {
        private double weight = 0.6;
        
        @Override
        public List<HybridSearchResult.SearchResultItem> search(String query, List<Memory> memories, SearchContext context) {
            try {
                // 创建搜索配置
                SemanticSearchEngine.SearchConfiguration config = 
                    new SemanticSearchEngine.SearchConfiguration();
                config.setMaxResults(Math.min(10, memories.size()));
                config.setSemanticThreshold(0.3);
                
                SemanticSearchEngine.SemanticSearchResult semanticResult = 
                    semanticEngine.search(query, config).join();
                
                return semanticResult.getResults().stream()
                    .map(item -> {
                        // 转换EnhancedMemory到Memory
                        EnhancedMemory enhancedMem = item.getMemory();
                        Memory memory = new Memory(enhancedMem.getId(), enhancedMem.getContent(), enhancedMem.getMetadata());
                        memory.setUserId(enhancedMem.getUserId());
                        
                        Map<String, Object> debugInfo = new HashMap<>();
                        if (item.getScoreBreakdown() != null) {
                            debugInfo.put("scoreBreakdown", item.getScoreBreakdown().toString());
                        }
                        
                        return new HybridSearchResult.SearchResultItem(
                            memory,
                            item.getFinalScore(),
                            item.getFinalScore(),
                            0.0,
                            0.0,
                            "semantic",
                            Arrays.asList(query),
                            debugInfo
                        );
                    })
                    .collect(Collectors.toList());
                    
            } catch (Exception e) {
                logger.error("Error in semantic search", e);
                return new ArrayList<>();
            }
        }
        
        @Override
        public String getStrategyName() { return "semantic"; }
        
        @Override
        public double getWeight() { return weight; }
        
        @Override
        public void setWeight(double weight) { this.weight = weight; }
    }
    
    /**
     * 关键词搜索策略
     */
    private class KeywordSearchStrategy implements SearchStrategy {
        private double weight = 0.3;
        
        @Override
        public List<HybridSearchResult.SearchResultItem> search(String query, List<Memory> memories, SearchContext context) {
            List<HybridSearchResult.SearchResultItem> results = new ArrayList<>();
            String[] queryTerms = query.toLowerCase().split("\\s+");
            
            for (Memory memory : memories) {
                String content = memory.getContent().toLowerCase();
                double keywordScore = calculateKeywordScore(content, queryTerms);
                
                if (keywordScore > 0.1) {
                    List<String> matchedTerms = Arrays.stream(queryTerms)
                        .filter(content::contains)
                        .collect(Collectors.toList());
                    
                    Map<String, Object> debugInfo = new HashMap<>();
                    debugInfo.put("matchedTerms", matchedTerms);
                    debugInfo.put("keywordScore", keywordScore);
                    
                    results.add(new HybridSearchResult.SearchResultItem(
                        memory,
                        keywordScore,
                        0.0,
                        keywordScore,
                        0.0,
                        "keyword",
                        matchedTerms,
                        debugInfo
                    ));
                }
            }
            
            return results;
        }
        
        private double calculateKeywordScore(String content, String[] queryTerms) {
            int matches = 0;
            int totalTerms = queryTerms.length;
            
            for (String term : queryTerms) {
                if (content.contains(term)) {
                    matches++;
                }
            }
            
            return totalTerms > 0 ? (double) matches / totalTerms : 0.0;
        }
        
        @Override
        public String getStrategyName() { return "keyword"; }
        
        @Override
        public double getWeight() { return weight; }
        
        @Override
        public void setWeight(double weight) { this.weight = weight; }
    }
    
    /**
     * 模糊搜索策略
     */
    private class FuzzySearchStrategy implements SearchStrategy {
        private double weight = 0.1;
        
        @Override
        public List<HybridSearchResult.SearchResultItem> search(String query, List<Memory> memories, SearchContext context) {
            List<HybridSearchResult.SearchResultItem> results = new ArrayList<>();
            
            for (Memory memory : memories) {
                double fuzzyScore = calculateFuzzyScore(query, memory.getContent());
                
                if (fuzzyScore > 0.3) {
                    Map<String, Object> debugInfo = new HashMap<>();
                    debugInfo.put("fuzzyScore", fuzzyScore);
                    debugInfo.put("algorithm", "levenshtein");
                    
                    results.add(new HybridSearchResult.SearchResultItem(
                        memory,
                        fuzzyScore,
                        0.0,
                        0.0,
                        fuzzyScore,
                        "fuzzy",
                        Arrays.asList(query),
                        debugInfo
                    ));
                }
            }
            
            return results;
        }
        
        private double calculateFuzzyScore(String query, String content) {
            // 简化的模糊匹配算法
            String[] queryWords = query.toLowerCase().split("\\s+");
            String[] contentWords = content.toLowerCase().split("\\s+");
            
            double maxScore = 0.0;
            for (String queryWord : queryWords) {
                for (String contentWord : contentWords) {
                    double similarity = calculateLevenshteinSimilarity(queryWord, contentWord);
                    maxScore = Math.max(maxScore, similarity);
                }
            }
            
            return maxScore;
        }
        
        private double calculateLevenshteinSimilarity(String s1, String s2) {
            int distance = levenshteinDistance(s1, s2);
            int maxLength = Math.max(s1.length(), s2.length());
            return maxLength > 0 ? 1.0 - (double) distance / maxLength : 1.0;
        }
        
        private int levenshteinDistance(String s1, String s2) {
            int[][] dp = new int[s1.length() + 1][s2.length() + 1];
            
            for (int i = 0; i <= s1.length(); i++) {
                dp[i][0] = i;
            }
            for (int j = 0; j <= s2.length(); j++) {
                dp[0][j] = j;
            }
            
            for (int i = 1; i <= s1.length(); i++) {
                for (int j = 1; j <= s2.length(); j++) {
                    int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                    dp[i][j] = Math.min(Math.min(
                        dp[i - 1][j] + 1,      // 删除
                        dp[i][j - 1] + 1),     // 插入
                        dp[i - 1][j - 1] + cost // 替换
                    );
                }
            }
            
            return dp[s1.length()][s2.length()];
        }
        
        @Override
        public String getStrategyName() { return "fuzzy"; }
        
        @Override
        public double getWeight() { return weight; }
        
        @Override
        public void setWeight(double weight) { this.weight = weight; }
    }
    
    // Getters
    public HybridSearchConfiguration getConfiguration() { return configuration; }
    public SearchPerformanceMonitor getPerformanceMonitor() { return performanceMonitor; }
    public long getTotalSearchCount() { return totalSearchCount.get(); }
    public Map<String, Double> getStrategyPerformance() { return new HashMap<>(strategyPerformance); }
}