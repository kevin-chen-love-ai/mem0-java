package com.mem0.search;

import com.mem0.memory.Memory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * SearchFilter - 高级搜索过滤器
 * 
 * 提供强大的搜索结果过滤和筛选功能，支持多维度条件组合。
 * 支持时间范围、内容类型、重要性级别、用户定义标签等多种过滤维度。
 * 
 * 主要功能：
 * 1. 多维度过滤 - 时间、类型、重要性、标签、内容长度等
 * 2. 条件组合 - AND/OR/NOT逻辑操作符支持
 * 3. 动态过滤器 - 基于用户行为的自适应过滤
 * 4. 性能优化 - 索引优化和并行处理
 * 5. 过滤统计 - 过滤效果分析和优化建议
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 2024-12-20
 */
public class SearchFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(SearchFilter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // 过滤器配置
    private final FilterConfiguration configuration;
    private final Map<String, FilterIndex> filterIndices;
    private final FilterStatistics statistics;
    
    /**
     * 过滤器配置
     */
    public static class FilterConfiguration {
        private boolean enableIndexOptimization = true;
        private boolean enableParallelProcessing = true;
        private int parallelThreshold = 100;
        private boolean enableStatistics = true;
        private int maxFilterResults = 1000;
        private double defaultRelevanceThreshold = 0.0;
        
        // Getters and setters
        public boolean isEnableIndexOptimization() { return enableIndexOptimization; }
        public void setEnableIndexOptimization(boolean enableIndexOptimization) { this.enableIndexOptimization = enableIndexOptimization; }
        
        public boolean isEnableParallelProcessing() { return enableParallelProcessing; }
        public void setEnableParallelProcessing(boolean enableParallelProcessing) { this.enableParallelProcessing = enableParallelProcessing; }
        
        public int getParallelThreshold() { return parallelThreshold; }
        public void setParallelThreshold(int parallelThreshold) { this.parallelThreshold = parallelThreshold; }
        
        public boolean isEnableStatistics() { return enableStatistics; }
        public void setEnableStatistics(boolean enableStatistics) { this.enableStatistics = enableStatistics; }
        
        public int getMaxFilterResults() { return maxFilterResults; }
        public void setMaxFilterResults(int maxFilterResults) { this.maxFilterResults = maxFilterResults; }
        
        public double getDefaultRelevanceThreshold() { return defaultRelevanceThreshold; }
        public void setDefaultRelevanceThreshold(double defaultRelevanceThreshold) { this.defaultRelevanceThreshold = defaultRelevanceThreshold; }
    }
    
    /**
     * 搜索过滤条件
     */
    public static class FilterCriteria {
        private final Map<String, Object> conditions;
        private final FilterLogic logic;
        private final List<FilterCriteria> subCriteria;
        
        public enum FilterLogic {
            AND, OR, NOT
        }
        
        public FilterCriteria() {
            this.conditions = new HashMap<>();
            this.logic = FilterLogic.AND;
            this.subCriteria = new ArrayList<>();
        }
        
        public FilterCriteria(FilterLogic logic) {
            this.conditions = new HashMap<>();
            this.logic = logic;
            this.subCriteria = new ArrayList<>();
        }
        
        // 时间范围过滤
        public FilterCriteria timeRange(LocalDateTime startTime, LocalDateTime endTime) {
            conditions.put("startTime", startTime);
            conditions.put("endTime", endTime);
            return this;
        }
        
        // 内容类型过滤
        public FilterCriteria contentType(String... types) {
            conditions.put("contentTypes", Arrays.asList(types));
            return this;
        }
        
        // 重要性级别过滤
        public FilterCriteria importanceLevel(int minLevel, int maxLevel) {
            conditions.put("minImportance", minLevel);
            conditions.put("maxImportance", maxLevel);
            return this;
        }
        
        // 标签过滤
        public FilterCriteria tags(String... tags) {
            conditions.put("requiredTags", Arrays.asList(tags));
            return this;
        }
        
        // 内容长度过滤
        public FilterCriteria contentLength(int minLength, int maxLength) {
            conditions.put("minContentLength", minLength);
            conditions.put("maxContentLength", maxLength);
            return this;
        }
        
        // 用户ID过滤
        public FilterCriteria userId(String userId) {
            conditions.put("userId", userId);
            return this;
        }
        
        // 会话ID过滤
        public FilterCriteria sessionId(String sessionId) {
            conditions.put("sessionId", sessionId);
            return this;
        }
        
        // 相关性阈值过滤
        public FilterCriteria relevanceThreshold(double threshold) {
            conditions.put("relevanceThreshold", threshold);
            return this;
        }
        
        // 正则表达式内容过滤
        public FilterCriteria contentMatches(String regex) {
            conditions.put("contentRegex", regex);
            return this;
        }
        
        // 自定义条件
        public FilterCriteria customCondition(String key, Object value) {
            conditions.put(key, value);
            return this;
        }
        
        // 添加子条件
        public FilterCriteria addSubCriteria(FilterCriteria subCriteria) {
            this.subCriteria.add(subCriteria);
            return this;
        }
        
        // Getters
        public Map<String, Object> getConditions() { return conditions; }
        public FilterLogic getLogic() { return logic; }
        public List<FilterCriteria> getSubCriteria() { return subCriteria; }
    }
    
    /**
     * 过滤结果
     */
    public static class FilterResult {
        private final List<Memory> filteredMemories;
        private final FilterStatisticsSnapshot statistics;
        private final String filterId;
        private final LocalDateTime timestamp;
        private final FilterCriteria appliedCriteria;
        
        public FilterResult(List<Memory> filteredMemories, FilterStatisticsSnapshot statistics,
                          FilterCriteria appliedCriteria) {
            this.filteredMemories = filteredMemories;
            this.statistics = statistics;
            this.appliedCriteria = appliedCriteria;
            this.filterId = UUID.randomUUID().toString();
            this.timestamp = LocalDateTime.now();
        }
        
        public static class FilterStatisticsSnapshot {
            private final int originalCount;
            private final int filteredCount;
            private final long processingTime;
            private final Map<String, Integer> filterBreakdown;
            private final double filterEfficiency;
            
            public FilterStatisticsSnapshot(int originalCount, int filteredCount, long processingTime,
                                          Map<String, Integer> filterBreakdown, double filterEfficiency) {
                this.originalCount = originalCount;
                this.filteredCount = filteredCount;
                this.processingTime = processingTime;
                this.filterBreakdown = filterBreakdown;
                this.filterEfficiency = filterEfficiency;
            }
            
            // Getters
            public int getOriginalCount() { return originalCount; }
            public int getFilteredCount() { return filteredCount; }
            public long getProcessingTime() { return processingTime; }
            public Map<String, Integer> getFilterBreakdown() { return filterBreakdown; }
            public double getFilterEfficiency() { return filterEfficiency; }
        }
        
        // Getters
        public List<Memory> getFilteredMemories() { return filteredMemories; }
        public FilterStatisticsSnapshot getStatistics() { return statistics; }
        public String getFilterId() { return filterId; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public FilterCriteria getAppliedCriteria() { return appliedCriteria; }
    }
    
    /**
     * 过滤器索引
     */
    public static class FilterIndex {
        private final String indexName;
        private final Map<Object, Set<String>> valueToMemoryIds;
        private final Map<String, Object> memoryIdToValue;
        
        public FilterIndex(String indexName) {
            this.indexName = indexName;
            this.valueToMemoryIds = new HashMap<>();
            this.memoryIdToValue = new HashMap<>();
        }
        
        public void addEntry(String memoryId, Object value) {
            valueToMemoryIds.computeIfAbsent(value, k -> new HashSet<>()).add(memoryId);
            memoryIdToValue.put(memoryId, value);
        }
        
        public Set<String> getMemoryIds(Object value) {
            return valueToMemoryIds.getOrDefault(value, new HashSet<>());
        }
        
        public void removeEntry(String memoryId) {
            Object value = memoryIdToValue.remove(memoryId);
            if (value != null) {
                Set<String> ids = valueToMemoryIds.get(value);
                if (ids != null) {
                    ids.remove(memoryId);
                    if (ids.isEmpty()) {
                        valueToMemoryIds.remove(value);
                    }
                }
            }
        }
        
        // Getters
        public String getIndexName() { return indexName; }
        public Set<Object> getDistinctValues() { return valueToMemoryIds.keySet(); }
        public int getSize() { return memoryIdToValue.size(); }
    }
    
    /**
     * 过滤器统计
     */
    public static class FilterStatistics {
        private long totalFilterOperations = 0;
        private long totalProcessingTime = 0;
        private final Map<String, Long> filterTypeUsage = new HashMap<>();
        private final Map<String, Double> filterEfficiency = new HashMap<>();
        private final List<String> optimizationSuggestions = new ArrayList<>();
        
        public void recordFilterOperation(String filterType, long processingTime, int originalCount, int filteredCount) {
            totalFilterOperations++;
            totalProcessingTime += processingTime;
            
            filterTypeUsage.merge(filterType, 1L, Long::sum);
            
            double efficiency = originalCount > 0 ? (double) filteredCount / originalCount : 1.0;
            filterEfficiency.put(filterType, efficiency);
            
            // 生成优化建议
            generateOptimizationSuggestions(filterType, efficiency, processingTime);
        }
        
        private void generateOptimizationSuggestions(String filterType, double efficiency, long processingTime) {
            if (efficiency < 0.1 && processingTime > 1000) {
                optimizationSuggestions.add("考虑为 " + filterType + " 过滤器添加索引优化");
            }
            if (processingTime > 5000) {
                optimizationSuggestions.add("考虑为 " + filterType + " 过滤器启用并行处理");
            }
        }
        
        // Getters
        public long getTotalFilterOperations() { return totalFilterOperations; }
        public long getTotalProcessingTime() { return totalProcessingTime; }
        public double getAverageProcessingTime() { 
            return totalFilterOperations > 0 ? (double) totalProcessingTime / totalFilterOperations : 0.0; 
        }
        public Map<String, Long> getFilterTypeUsage() { return new HashMap<>(filterTypeUsage); }
        public Map<String, Double> getFilterEfficiency() { return new HashMap<>(filterEfficiency); }
        public List<String> getOptimizationSuggestions() { return new ArrayList<>(optimizationSuggestions); }
    }
    
    /**
     * 构造函数
     * 
     * @param configuration 过滤器配置
     */
    public SearchFilter(FilterConfiguration configuration) {
        this.configuration = configuration != null ? configuration : new FilterConfiguration();
        this.filterIndices = new HashMap<>();
        this.statistics = new FilterStatistics();
        
        // 初始化常用索引
        initializeDefaultIndices();
        
        logger.info("SearchFilter initialized with configuration");
    }
    
    /**
     * 默认构造函数
     */
    public SearchFilter() {
        this(new FilterConfiguration());
    }
    
    /**
     * 初始化默认索引
     */
    private void initializeDefaultIndices() {
        if (configuration.isEnableIndexOptimization()) {
            filterIndices.put("userId", new FilterIndex("userId"));
            filterIndices.put("sessionId", new FilterIndex("sessionId"));
            filterIndices.put("contentType", new FilterIndex("contentType"));
            filterIndices.put("importanceLevel", new FilterIndex("importanceLevel"));
            
            logger.info("Initialized {} default filter indices", filterIndices.size());
        }
    }
    
    /**
     * 构建过滤器索引
     * 
     * @param memories 内存数据列表
     * @return 异步构建任务
     */
    public CompletableFuture<Void> buildFilterIndices(List<Memory> memories) {
        return CompletableFuture.runAsync(() -> {
            if (!configuration.isEnableIndexOptimization()) {
                return;
            }
            
            long startTime = System.currentTimeMillis();
            
            try {
                for (Memory memory : memories) {
                    // 构建用户ID索引
                    if (memory.getUserId() != null) {
                        filterIndices.get("userId").addEntry(memory.getId(), memory.getUserId());
                    }
                    
                    // 构建会话ID索引
                    if (memory.getSessionId() != null) {
                        filterIndices.get("sessionId").addEntry(memory.getId(), memory.getSessionId());
                    }
                    
                    // 构建内容类型索引
                    Map<String, Object> metadata = memory.getMetadata();
                    if (metadata.containsKey("contentType")) {
                        filterIndices.get("contentType").addEntry(memory.getId(), metadata.get("contentType"));
                    }
                    
                    // 构建重要性级别索引
                    if (metadata.containsKey("importanceLevel")) {
                        filterIndices.get("importanceLevel").addEntry(memory.getId(), metadata.get("importanceLevel"));
                    }
                }
                
                long duration = System.currentTimeMillis() - startTime;
                logger.info("Built filter indices for {} memories in {}ms", memories.size(), duration);
                
            } catch (Exception e) {
                logger.error("Error building filter indices", e);
                throw new RuntimeException("Failed to build filter indices", e);
            }
        });
    }
    
    /**
     * 执行过滤操作
     * 
     * @param memories 待过滤的内存列表
     * @param criteria 过滤条件
     * @return 过滤结果
     */
    public CompletableFuture<FilterResult> filter(List<Memory> memories, FilterCriteria criteria) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                List<Memory> filteredMemories;
                
                if (configuration.isEnableParallelProcessing() && 
                    memories.size() >= configuration.getParallelThreshold()) {
                    // 并行过滤
                    filteredMemories = memories.parallelStream()
                        .filter(memory -> matchesCriteria(memory, criteria))
                        .limit(configuration.getMaxFilterResults())
                        .collect(Collectors.toList());
                } else {
                    // 串行过滤
                    filteredMemories = memories.stream()
                        .filter(memory -> matchesCriteria(memory, criteria))
                        .limit(configuration.getMaxFilterResults())
                        .collect(Collectors.toList());
                }
                
                long processingTime = System.currentTimeMillis() - startTime;
                
                // 统计信息
                Map<String, Integer> filterBreakdown = analyzeFilterBreakdown(criteria);
                double filterEfficiency = memories.isEmpty() ? 1.0 : (double) filteredMemories.size() / memories.size();
                
                FilterResult.FilterStatisticsSnapshot statisticsSnapshot = 
                    new FilterResult.FilterStatisticsSnapshot(
                        memories.size(),
                        filteredMemories.size(),
                        processingTime,
                        filterBreakdown,
                        filterEfficiency
                    );
                
                // 记录统计
                if (configuration.isEnableStatistics()) {
                    statistics.recordFilterOperation("composite", processingTime, 
                                                   memories.size(), filteredMemories.size());
                }
                
                FilterResult result = new FilterResult(filteredMemories, statisticsSnapshot, criteria);
                
                logger.debug("Filter operation completed: {} -> {} results in {}ms",
                           memories.size(), filteredMemories.size(), processingTime);
                
                return result;
                
            } catch (Exception e) {
                logger.error("Error in filter operation", e);
                throw new RuntimeException("Filter operation failed", e);
            }
        });
    }
    
    /**
     * 检查内存是否匹配过滤条件
     * 
     * @param memory 内存对象
     * @param criteria 过滤条件
     * @return 是否匹配
     */
    private boolean matchesCriteria(Memory memory, FilterCriteria criteria) {
        boolean mainResult = evaluateConditions(memory, criteria.getConditions());
        
        if (criteria.getSubCriteria().isEmpty()) {
            return mainResult;
        }
        
        // 处理子条件
        boolean subResult = true;
        for (FilterCriteria subCriteria : criteria.getSubCriteria()) {
            boolean subMatch = matchesCriteria(memory, subCriteria);
            
            switch (criteria.getLogic()) {
                case AND:
                    subResult = subResult && subMatch;
                    break;
                case OR:
                    subResult = subResult || subMatch;
                    break;
                case NOT:
                    subResult = subResult && !subMatch;
                    break;
            }
        }
        
        return criteria.getLogic() == FilterCriteria.FilterLogic.AND ? 
               mainResult && subResult : mainResult || subResult;
    }
    
    /**
     * 评估条件
     * 
     * @param memory 内存对象
     * @param conditions 条件映射
     * @return 是否匹配
     */
    private boolean evaluateConditions(Memory memory, Map<String, Object> conditions) {
        for (Map.Entry<String, Object> condition : conditions.entrySet()) {
            String key = condition.getKey();
            Object value = condition.getValue();
            
            if (!evaluateCondition(memory, key, value)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 评估单个条件
     * 
     * @param memory 内存对象
     * @param key 条件键
     * @param value 条件值
     * @return 是否匹配
     */
    private boolean evaluateCondition(Memory memory, String key, Object value) {
        switch (key) {
            case "startTime":
                LocalDateTime startTime = (LocalDateTime) value;
                Instant startInstant = startTime.atZone(java.time.ZoneId.systemDefault()).toInstant();
                return memory.getCreatedAt().isAfter(startInstant) || memory.getCreatedAt().equals(startInstant);
                
            case "endTime":
                LocalDateTime endTime = (LocalDateTime) value;
                Instant endInstant = endTime.atZone(java.time.ZoneId.systemDefault()).toInstant();
                return memory.getCreatedAt().isBefore(endInstant) || memory.getCreatedAt().equals(endInstant);
                
            case "contentTypes":
                @SuppressWarnings("unchecked")
                List<String> types = (List<String>) value;
                String contentType = (String) memory.getMetadata().get("contentType");
                return contentType != null && types.contains(contentType);
                
            case "minImportance":
                Integer minImportance = (Integer) value;
                Integer importance = (Integer) memory.getMetadata().get("importanceLevel");
                return importance != null && importance >= minImportance;
                
            case "maxImportance":
                Integer maxImportance = (Integer) value;
                Integer importanceMax = (Integer) memory.getMetadata().get("importanceLevel");
                return importanceMax != null && importanceMax <= maxImportance;
                
            case "requiredTags":
                @SuppressWarnings("unchecked")
                List<String> requiredTags = (List<String>) value;
                @SuppressWarnings("unchecked")
                List<String> memoryTags = (List<String>) memory.getMetadata().get("tags");
                return memoryTags != null && memoryTags.containsAll(requiredTags);
                
            case "minContentLength":
                Integer minLength = (Integer) value;
                return memory.getContent().length() >= minLength;
                
            case "maxContentLength":
                Integer maxLength = (Integer) value;
                return memory.getContent().length() <= maxLength;
                
            case "userId":
                String userId = (String) value;
                return Objects.equals(memory.getUserId(), userId);
                
            case "sessionId":
                String sessionId = (String) value;
                return Objects.equals(memory.getSessionId(), sessionId);
                
            case "relevanceThreshold":
                Double threshold = (Double) value;
                Double relevance = (Double) memory.getMetadata().get("relevanceScore");
                return relevance != null && relevance >= threshold;
                
            case "contentRegex":
                String regex = (String) value;
                Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                return pattern.matcher(memory.getContent()).find();
                
            default:
                // 自定义条件
                Object metadataValue = memory.getMetadata().get(key);
                return Objects.equals(metadataValue, value);
        }
    }
    
    /**
     * 分析过滤器分解情况
     * 
     * @param criteria 过滤条件
     * @return 分解统计
     */
    private Map<String, Integer> analyzeFilterBreakdown(FilterCriteria criteria) {
        Map<String, Integer> breakdown = new HashMap<>();
        
        // 分析主条件
        for (String key : criteria.getConditions().keySet()) {
            breakdown.merge(key, 1, Integer::sum);
        }
        
        // 分析子条件
        for (FilterCriteria subCriteria : criteria.getSubCriteria()) {
            Map<String, Integer> subBreakdown = analyzeFilterBreakdown(subCriteria);
            for (Map.Entry<String, Integer> entry : subBreakdown.entrySet()) {
                breakdown.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }
        
        return breakdown;
    }
    
    /**
     * 创建快速过滤器
     * 
     * @param memories 内存列表
     * @return 快速过滤器构建器
     */
    public QuickFilter createQuickFilter(List<Memory> memories) {
        return new QuickFilter(memories, this);
    }
    
    /**
     * 快速过滤器构建器
     */
    public static class QuickFilter {
        private final List<Memory> memories;
        private final SearchFilter searchFilter;
        private final FilterCriteria criteria;
        
        public QuickFilter(List<Memory> memories, SearchFilter searchFilter) {
            this.memories = memories;
            this.searchFilter = searchFilter;
            this.criteria = new FilterCriteria();
        }
        
        public QuickFilter timeRange(LocalDateTime start, LocalDateTime end) {
            criteria.timeRange(start, end);
            return this;
        }
        
        public QuickFilter contentType(String... types) {
            criteria.contentType(types);
            return this;
        }
        
        public QuickFilter importanceLevel(int min, int max) {
            criteria.importanceLevel(min, max);
            return this;
        }
        
        public QuickFilter tags(String... tags) {
            criteria.tags(tags);
            return this;
        }
        
        public QuickFilter user(String userId) {
            criteria.userId(userId);
            return this;
        }
        
        public QuickFilter session(String sessionId) {
            criteria.sessionId(sessionId);
            return this;
        }
        
        public QuickFilter relevanceThreshold(double threshold) {
            criteria.relevanceThreshold(threshold);
            return this;
        }
        
        public QuickFilter contentMatches(String regex) {
            criteria.contentMatches(regex);
            return this;
        }
        
        public CompletableFuture<FilterResult> execute() {
            return searchFilter.filter(memories, criteria);
        }
        
        public List<Memory> executeSync() {
            try {
                return execute().join().getFilteredMemories();
            } catch (Exception e) {
                logger.error("Error in quick filter execution", e);
                return new ArrayList<>();
            }
        }
    }
    
    // Getters
    public FilterConfiguration getConfiguration() { return configuration; }
    public FilterStatistics getStatistics() { return statistics; }
    public Map<String, FilterIndex> getFilterIndices() { return filterIndices; }
}