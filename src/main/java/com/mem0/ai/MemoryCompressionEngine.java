package com.mem0.ai;

import com.mem0.memory.Memory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MemoryCompressionEngine - 内存压缩引擎
 * 
 * 提供智能的内存压缩和优化功能，通过多种压缩策略减少内存存储空间，
 * 同时保持重要信息的完整性和可检索性。
 * 
 * 主要功能：
 * 1. 智能内容压缩 - 基于重要性和相似性的内容压缩
 * 2. 冗余数据去除 - 识别和合并重复或相似的内存
 * 3. 时间衰减压缩 - 根据时间权重对历史内存进行压缩
 * 4. 分层压缩策略 - 不同类型内存采用不同的压缩算法
 * 5. 压缩质量控制 - 平衡压缩率与信息保真度
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 2024-12-20
 */
public class MemoryCompressionEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryCompressionEngine.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final CompressionConfiguration configuration;
    private final Map<String, CompressionStrategy> compressionStrategies;
    private final CompressionStatistics statistics;
    private final Map<String, CompressedMemory> compressedMemoryCache;
    
    /**
     * 压缩配置
     */
    public static class CompressionConfiguration {
        private double compressionThreshold = 0.8; // 相似度阈值
        private int maxCompressionRatio = 10; // 最大压缩比例
        private boolean enableSemanticCompression = true;
        private boolean enableRedundancyRemoval = true;
        private boolean enableTemporalCompression = true;
        private int temporalCompressionDays = 30; // 时间压缩阈值（天）
        private double importanceThreshold = 0.3; // 重要性阈值
        private int maxCompressedMemories = 1000; // 最大压缩内存数量
        
        // Getters and setters
        public double getCompressionThreshold() { return compressionThreshold; }
        public void setCompressionThreshold(double compressionThreshold) { this.compressionThreshold = compressionThreshold; }
        
        public int getMaxCompressionRatio() { return maxCompressionRatio; }
        public void setMaxCompressionRatio(int maxCompressionRatio) { this.maxCompressionRatio = maxCompressionRatio; }
        
        public boolean isEnableSemanticCompression() { return enableSemanticCompression; }
        public void setEnableSemanticCompression(boolean enableSemanticCompression) { this.enableSemanticCompression = enableSemanticCompression; }
        
        public boolean isEnableRedundancyRemoval() { return enableRedundancyRemoval; }
        public void setEnableRedundancyRemoval(boolean enableRedundancyRemoval) { this.enableRedundancyRemoval = enableRedundancyRemoval; }
        
        public boolean isEnableTemporalCompression() { return enableTemporalCompression; }
        public void setEnableTemporalCompression(boolean enableTemporalCompression) { this.enableTemporalCompression = enableTemporalCompression; }
        
        public int getTemporalCompressionDays() { return temporalCompressionDays; }
        public void setTemporalCompressionDays(int temporalCompressionDays) { this.temporalCompressionDays = temporalCompressionDays; }
        
        public double getImportanceThreshold() { return importanceThreshold; }
        public void setImportanceThreshold(double importanceThreshold) { this.importanceThreshold = importanceThreshold; }
        
        public int getMaxCompressedMemories() { return maxCompressedMemories; }
        public void setMaxCompressedMemories(int maxCompressedMemories) { this.maxCompressedMemories = maxCompressedMemories; }
    }
    
    /**
     * 压缩策略接口
     */
    public interface CompressionStrategy {
        /**
         * 执行压缩
         * 
         * @param memories 待压缩的内存列表
         * @return 压缩结果
         */
        CompletableFuture<CompressionResult> compress(List<Memory> memories);
        
        /**
         * 获取策略名称
         * 
         * @return 策略名称
         */
        String getStrategyName();
        
        /**
         * 是否支持该内存类型
         * 
         * @param memory 内存对象
         * @return 是否支持
         */
        boolean supportsMemory(Memory memory);
    }
    
    /**
     * 压缩结果
     */
    public static class CompressionResult {
        private final List<CompressedMemory> compressedMemories;
        private final List<String> removedMemoryIds;
        private final CompressionStatisticsSnapshot statistics;
        private final String compressionId;
        private final LocalDateTime timestamp;
        
        public CompressionResult(List<CompressedMemory> compressedMemories,
                               List<String> removedMemoryIds,
                               CompressionStatisticsSnapshot statistics) {
            this.compressedMemories = compressedMemories;
            this.removedMemoryIds = removedMemoryIds;
            this.statistics = statistics;
            this.compressionId = UUID.randomUUID().toString();
            this.timestamp = LocalDateTime.now();
        }
        
        public static class CompressionStatisticsSnapshot {
            private final int originalCount;
            private final int compressedCount;
            private final long originalSize;
            private final long compressedSize;
            private final double compressionRatio;
            private final long processingTime;
            private final Map<String, Integer> strategyBreakdown;
            
            public CompressionStatisticsSnapshot(int originalCount, int compressedCount,
                                               long originalSize, long compressedSize,
                                               double compressionRatio, long processingTime,
                                               Map<String, Integer> strategyBreakdown) {
                this.originalCount = originalCount;
                this.compressedCount = compressedCount;
                this.originalSize = originalSize;
                this.compressedSize = compressedSize;
                this.compressionRatio = compressionRatio;
                this.processingTime = processingTime;
                this.strategyBreakdown = strategyBreakdown;
            }
            
            // Getters
            public int getOriginalCount() { return originalCount; }
            public int getCompressedCount() { return compressedCount; }
            public long getOriginalSize() { return originalSize; }
            public long getCompressedSize() { return compressedSize; }
            public double getCompressionRatio() { return compressionRatio; }
            public long getProcessingTime() { return processingTime; }
            public Map<String, Integer> getStrategyBreakdown() { return strategyBreakdown; }
        }
        
        // Getters
        public List<CompressedMemory> getCompressedMemories() { return compressedMemories; }
        public List<String> getRemovedMemoryIds() { return removedMemoryIds; }
        public CompressionStatisticsSnapshot getStatistics() { return statistics; }
        public String getCompressionId() { return compressionId; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    /**
     * 压缩内存
     */
    public static class CompressedMemory extends Memory {
        private final List<String> originalMemoryIds;
        private final CompressionMethod compressionMethod;
        private final double compressionRatio;
        private final String originalContent;
        private final Map<String, Object> compressionMetadata;
        
        public enum CompressionMethod {
            SEMANTIC_MERGE,      // 语义合并
            REDUNDANCY_REMOVAL,  // 冗余去除
            TEMPORAL_DECAY,      // 时间衰减
            CONTENT_SUMMARY,     // 内容摘要
            HIERARCHICAL        // 分层压缩
        }
        
        public CompressedMemory(String id, String content, String userId, String sessionId,
                              List<String> originalMemoryIds, CompressionMethod compressionMethod,
                              double compressionRatio, String originalContent,
                              Map<String, Object> compressionMetadata) {
            super(id, content, compressionMetadata);
            this.setUserId(userId);
            this.setSessionId(sessionId);
            this.originalMemoryIds = originalMemoryIds != null ? originalMemoryIds : new ArrayList<>();
            this.compressionMethod = compressionMethod;
            this.compressionRatio = compressionRatio;
            this.originalContent = originalContent;
            this.compressionMetadata = compressionMetadata != null ? compressionMetadata : new HashMap<>();
            
            // 标记为压缩内存
            getMetadata().put("isCompressed", true);
            getMetadata().put("compressionMethod", compressionMethod.name());
            getMetadata().put("compressionRatio", compressionRatio);
            getMetadata().put("originalMemoryCount", this.originalMemoryIds.size());
        }
        
        // Getters
        public List<String> getOriginalMemoryIds() { return originalMemoryIds; }
        public CompressionMethod getCompressionMethod() { return compressionMethod; }
        public double getCompressionRatio() { return compressionRatio; }
        public String getOriginalContent() { return originalContent; }
        public Map<String, Object> getCompressionMetadata() { return compressionMetadata; }
    }
    
    /**
     * 压缩统计
     */
    public static class CompressionStatistics {
        private long totalCompressions = 0;
        private long totalOriginalMemories = 0;
        private long totalCompressedMemories = 0;
        private long totalOriginalSize = 0;
        private long totalCompressedSize = 0;
        private final Map<String, Long> strategyUsage = new ConcurrentHashMap<>();
        private final Map<String, Double> strategyEfficiency = new ConcurrentHashMap<>();
        
        public void recordCompression(String strategy, int originalCount, int compressedCount,
                                    long originalSize, long compressedSize) {
            totalCompressions++;
            totalOriginalMemories += originalCount;
            totalCompressedMemories += compressedCount;
            totalOriginalSize += originalSize;
            totalCompressedSize += compressedSize;
            
            strategyUsage.merge(strategy, 1L, Long::sum);
            
            double efficiency = originalSize > 0 ? (double) compressedSize / originalSize : 1.0;
            strategyEfficiency.put(strategy, efficiency);
        }
        
        public double getOverallCompressionRatio() {
            return totalOriginalSize > 0 ? (double) totalCompressedSize / totalOriginalSize : 1.0;
        }
        
        public double getAverageCompressionRatio() {
            return totalCompressions > 0 ? 
                ((double) totalCompressedMemories / totalOriginalMemories) : 1.0;
        }
        
        // Getters
        public long getTotalCompressions() { return totalCompressions; }
        public long getTotalOriginalMemories() { return totalOriginalMemories; }
        public long getTotalCompressedMemories() { return totalCompressedMemories; }
        public long getTotalOriginalSize() { return totalOriginalSize; }
        public long getTotalCompressedSize() { return totalCompressedSize; }
        public Map<String, Long> getStrategyUsage() { return new HashMap<>(strategyUsage); }
        public Map<String, Double> getStrategyEfficiency() { return new HashMap<>(strategyEfficiency); }
    }
    
    /**
     * 构造函数
     * 
     * @param configuration 压缩配置
     */
    public MemoryCompressionEngine(CompressionConfiguration configuration) {
        this.configuration = configuration != null ? configuration : new CompressionConfiguration();
        this.compressionStrategies = new ConcurrentHashMap<>();
        this.statistics = new CompressionStatistics();
        this.compressedMemoryCache = new ConcurrentHashMap<>();
        
        // 初始化压缩策略
        initializeCompressionStrategies();
        
        logger.info("MemoryCompressionEngine initialized with {} strategies", 
                   compressionStrategies.size());
    }
    
    /**
     * 默认构造函数
     */
    public MemoryCompressionEngine() {
        this(new CompressionConfiguration());
    }
    
    /**
     * 初始化压缩策略
     */
    private void initializeCompressionStrategies() {
        if (configuration.isEnableSemanticCompression()) {
            compressionStrategies.put("semantic", new SemanticCompressionStrategy());
        }
        
        if (configuration.isEnableRedundancyRemoval()) {
            compressionStrategies.put("redundancy", new RedundancyRemovalStrategy());
        }
        
        if (configuration.isEnableTemporalCompression()) {
            compressionStrategies.put("temporal", new TemporalCompressionStrategy());
        }
        
        // 通用内容摘要策略
        compressionStrategies.put("summary", new ContentSummaryStrategy());
        
        logger.info("Initialized {} compression strategies", compressionStrategies.size());
    }
    
    /**
     * 执行内存压缩
     * 
     * @param memories 待压缩的内存列表
     * @return 压缩结果
     */
    public CompletableFuture<CompressionResult> compressMemories(List<Memory> memories) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                // 筛选可压缩的内存
                List<Memory> compressibleMemories = identifyCompressibleMemories(memories);
                
                if (compressibleMemories.isEmpty()) {
                    logger.info("No compressible memories found");
                    return createEmptyCompressionResult(startTime);
                }
                
                // 按策略分组内存
                Map<String, List<Memory>> strategyGroups = groupMemoriesByStrategy(compressibleMemories);
                
                // 执行各种压缩策略
                List<CompressedMemory> allCompressedMemories = new ArrayList<>();
                List<String> allRemovedIds = new ArrayList<>();
                Map<String, Integer> strategyBreakdown = new HashMap<>();
                
                for (Map.Entry<String, List<Memory>> entry : strategyGroups.entrySet()) {
                    String strategyName = entry.getKey();
                    List<Memory> strategyMemories = entry.getValue();
                    
                    CompressionStrategy strategy = compressionStrategies.get(strategyName);
                    if (strategy != null && !strategyMemories.isEmpty()) {
                        CompressionResult strategyResult = strategy.compress(strategyMemories).join();
                        
                        allCompressedMemories.addAll(strategyResult.getCompressedMemories());
                        allRemovedIds.addAll(strategyResult.getRemovedMemoryIds());
                        strategyBreakdown.put(strategyName, strategyMemories.size());
                    }
                }
                
                // 计算压缩统计
                long originalSize = calculateTotalSize(memories);
                long compressedSize = calculateCompressedSize(allCompressedMemories);
                double compressionRatio = originalSize > 0 ? (double) compressedSize / originalSize : 1.0;
                
                CompressionResult.CompressionStatisticsSnapshot statisticsSnapshot = 
                    new CompressionResult.CompressionStatisticsSnapshot(
                        memories.size(),
                        allCompressedMemories.size(),
                        originalSize,
                        compressedSize,
                        compressionRatio,
                        System.currentTimeMillis() - startTime,
                        strategyBreakdown
                    );
                
                // 更新全局统计
                statistics.recordCompression("combined", memories.size(), allCompressedMemories.size(),
                                           originalSize, compressedSize);
                
                // 缓存压缩结果
                for (CompressedMemory compressed : allCompressedMemories) {
                    compressedMemoryCache.put(compressed.getId(), compressed);
                }
                
                CompressionResult result = new CompressionResult(
                    allCompressedMemories, allRemovedIds, statisticsSnapshot);
                
                logger.info("Memory compression completed: {} -> {} memories, ratio: {:.2f}, time: {}ms",
                           memories.size(), allCompressedMemories.size(), compressionRatio,
                           statisticsSnapshot.getProcessingTime());
                
                return result;
                
            } catch (Exception e) {
                logger.error("Error during memory compression", e);
                throw new RuntimeException("Memory compression failed", e);
            }
        });
    }
    
    /**
     * 解压缩内存
     * 
     * @param compressedMemory 压缩内存
     * @return 解压缩的内存列表
     */
    public CompletableFuture<List<Memory>> decompressMemory(CompressedMemory compressedMemory) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 根据压缩方法进行解压缩
                switch (compressedMemory.getCompressionMethod()) {
                    case SEMANTIC_MERGE:
                        return decompressSemanticMerge(compressedMemory);
                    case REDUNDANCY_REMOVAL:
                        return decompressRedundancyRemoval(compressedMemory);
                    case TEMPORAL_DECAY:
                        return decompressTemporalDecay(compressedMemory);
                    case CONTENT_SUMMARY:
                        return decompressContentSummary(compressedMemory);
                    case HIERARCHICAL:
                        return decompressHierarchical(compressedMemory);
                    default:
                        return Arrays.asList(compressedMemory);
                }
                
            } catch (Exception e) {
                logger.error("Error decompressing memory: " + compressedMemory.getId(), e);
                return Arrays.asList(compressedMemory);
            }
        });
    }
    
    /**
     * 识别可压缩的内存
     * 
     * @param memories 内存列表
     * @return 可压缩的内存列表
     */
    private List<Memory> identifyCompressibleMemories(List<Memory> memories) {
        return memories.stream()
            .filter(memory -> {
                // 检查是否已经是压缩内存
                if (memory.getMetadata().getOrDefault("isCompressed", false).equals(true)) {
                    return false;
                }
                
                // 检查重要性阈值
                Double importance = (Double) memory.getMetadata().get("importance");
                if (importance != null && importance < configuration.getImportanceThreshold()) {
                    return true;
                }
                
                // 检查时间条件
                if (configuration.isEnableTemporalCompression()) {
                    LocalDateTime createdAt = LocalDateTime.ofInstant(memory.getCreatedAt(), java.time.ZoneId.systemDefault());
                    if (createdAt.isBefore(LocalDateTime.now().minus(
                        configuration.getTemporalCompressionDays(), ChronoUnit.DAYS))) {
                        return true;
                    }
                }
                
                return false;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 按策略分组内存
     * 
     * @param memories 内存列表
     * @return 策略分组映射
     */
    private Map<String, List<Memory>> groupMemoriesByStrategy(List<Memory> memories) {
        Map<String, List<Memory>> groups = new HashMap<>();
        
        for (Memory memory : memories) {
            for (Map.Entry<String, CompressionStrategy> entry : compressionStrategies.entrySet()) {
                String strategyName = entry.getKey();
                CompressionStrategy strategy = entry.getValue();
                
                if (strategy.supportsMemory(memory)) {
                    groups.computeIfAbsent(strategyName, k -> new ArrayList<>()).add(memory);
                    break; // 每个内存只分配给一个策略
                }
            }
        }
        
        return groups;
    }
    
    /**
     * 创建空压缩结果
     * 
     * @param startTime 开始时间
     * @return 空压缩结果
     */
    private CompressionResult createEmptyCompressionResult(long startTime) {
        CompressionResult.CompressionStatisticsSnapshot statistics = 
            new CompressionResult.CompressionStatisticsSnapshot(
                0, 0, 0, 0, 1.0, System.currentTimeMillis() - startTime, new HashMap<>());
        
        return new CompressionResult(new ArrayList<>(), new ArrayList<>(), statistics);
    }
    
    /**
     * 计算总大小
     * 
     * @param memories 内存列表
     * @return 总大小（字节）
     */
    private long calculateTotalSize(List<Memory> memories) {
        return memories.stream()
            .mapToLong(memory -> memory.getContent().length())
            .sum();
    }
    
    /**
     * 计算压缩后大小
     * 
     * @param compressedMemories 压缩内存列表
     * @return 压缩后大小（字节）
     */
    private long calculateCompressedSize(List<CompressedMemory> compressedMemories) {
        return compressedMemories.stream()
            .mapToLong(memory -> memory.getContent().length())
            .sum();
    }
    
    // 解压缩方法
    
    private List<Memory> decompressSemanticMerge(CompressedMemory compressedMemory) {
        // 语义合并的解压缩：分解为原始内存片段
        List<Memory> decompressed = new ArrayList<>();
        
        // 从压缩元数据中恢复原始信息
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> originalSegments = 
            (List<Map<String, Object>>) compressedMemory.getCompressionMetadata().get("originalSegments");
        
        if (originalSegments != null) {
            for (int i = 0; i < originalSegments.size(); i++) {
                Map<String, Object> segment = originalSegments.get(i);
                String segmentContent = (String) segment.get("content");
                
                Memory decompressedMemory = new Memory(
                    UUID.randomUUID().toString(),
                    segmentContent,
                    new HashMap<>()
                );
                decompressedMemory.setUserId(compressedMemory.getUserId());
                decompressedMemory.setSessionId(compressedMemory.getSessionId());
                
                decompressedMemory.getMetadata().put("decompressedFrom", compressedMemory.getId());
                decompressedMemory.getMetadata().put("originalSegmentIndex", i);
                
                decompressed.add(decompressedMemory);
            }
        }
        
        return decompressed;
    }
    
    private List<Memory> decompressRedundancyRemoval(CompressedMemory compressedMemory) {
        // 冗余去除的解压缩：恢复去重前的内存
        List<Memory> decompressed = new ArrayList<>();
        
        String baseContent = compressedMemory.getContent();
        int originalCount = (Integer) compressedMemory.getCompressionMetadata()
            .getOrDefault("originalCount", 1);
        
        for (int i = 0; i < originalCount; i++) {
            Memory decompressedMemory = new Memory(
                UUID.randomUUID().toString(),
                baseContent,
                new HashMap<>()
            );
            decompressedMemory.setUserId(compressedMemory.getUserId());
            decompressedMemory.setSessionId(compressedMemory.getSessionId());
            
            decompressedMemory.getMetadata().put("decompressedFrom", compressedMemory.getId());
            decompressedMemory.getMetadata().put("duplicateIndex", i);
            
            decompressed.add(decompressedMemory);
        }
        
        return decompressed;
    }
    
    private List<Memory> decompressTemporalDecay(CompressedMemory compressedMemory) {
        // 时间衰减的解压缩：恢复时间序列内存
        List<Memory> decompressed = new ArrayList<>();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> timeSlices = 
            (List<Map<String, Object>>) compressedMemory.getCompressionMetadata().get("timeSlices");
        
        if (timeSlices != null) {
            for (Map<String, Object> slice : timeSlices) {
                String content = (String) slice.get("content");
                String timestamp = (String) slice.get("timestamp");
                
                Memory decompressedMemory = new Memory(
                    UUID.randomUUID().toString(),
                    content,
                    new HashMap<>()
                );
                decompressedMemory.setUserId(compressedMemory.getUserId());
                decompressedMemory.setSessionId(compressedMemory.getSessionId());
                
                decompressedMemory.getMetadata().put("decompressedFrom", compressedMemory.getId());
                decompressedMemory.getMetadata().put("originalTimestamp", timestamp);
                
                decompressed.add(decompressedMemory);
            }
        }
        
        return decompressed;
    }
    
    private List<Memory> decompressContentSummary(CompressedMemory compressedMemory) {
        // 内容摘要的解压缩：从摘要推断原始内容
        Memory expandedMemory = new Memory(
            UUID.randomUUID().toString(),
            compressedMemory.getOriginalContent() != null ? 
                compressedMemory.getOriginalContent() : compressedMemory.getContent(),
            new HashMap<>()
        );
        expandedMemory.setUserId(compressedMemory.getUserId());
        expandedMemory.setSessionId(compressedMemory.getSessionId());
        
        expandedMemory.getMetadata().put("decompressedFrom", compressedMemory.getId());
        expandedMemory.getMetadata().put("wasCompressedAsSummary", true);
        
        return Arrays.asList(expandedMemory);
    }
    
    private List<Memory> decompressHierarchical(CompressedMemory compressedMemory) {
        // 分层压缩的解压缩：递归解压缩各层
        List<Memory> decompressed = new ArrayList<>();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> hierarchyLevels = 
            (List<Map<String, Object>>) compressedMemory.getCompressionMetadata().get("hierarchyLevels");
        
        if (hierarchyLevels != null) {
            for (int level = 0; level < hierarchyLevels.size(); level++) {
                Map<String, Object> levelData = hierarchyLevels.get(level);
                @SuppressWarnings("unchecked")
                List<String> levelContents = (List<String>) levelData.get("contents");
                
                if (levelContents != null) {
                    for (String content : levelContents) {
                        Memory decompressedMemory = new Memory(
                            UUID.randomUUID().toString(),
                            content,
                            new HashMap<>()
                        );
                        decompressedMemory.setUserId(compressedMemory.getUserId());
                        decompressedMemory.setSessionId(compressedMemory.getSessionId());
                        
                        decompressedMemory.getMetadata().put("decompressedFrom", compressedMemory.getId());
                        decompressedMemory.getMetadata().put("hierarchyLevel", level);
                        
                        decompressed.add(decompressedMemory);
                    }
                }
            }
        }
        
        return decompressed;
    }
    
    /**
     * 获取压缩统计
     * 
     * @return 压缩统计对象
     */
    public CompressionStatistics getCompressionStatistics() {
        return statistics;
    }
    
    /**
     * 获取压缩配置
     * 
     * @return 压缩配置
     */
    public CompressionConfiguration getConfiguration() {
        return configuration;
    }
    
    /**
     * 获取压缩内存缓存
     * 
     * @return 缓存映射
     */
    public Map<String, CompressedMemory> getCompressedMemoryCache() {
        return new HashMap<>(compressedMemoryCache);
    }
    
    /**
     * 清除压缩缓存
     */
    public void clearCompressionCache() {
        compressedMemoryCache.clear();
        logger.info("Compression cache cleared");
    }
    
    // 内部策略实现类（基础版本，实际项目中应该进一步细化）
    
    /**
     * 语义压缩策略
     */
    private class SemanticCompressionStrategy implements CompressionStrategy {
        @Override
        public CompletableFuture<CompressionResult> compress(List<Memory> memories) {
            return CompletableFuture.supplyAsync(() -> {
                // 简化的语义压缩实现
                List<CompressedMemory> compressed = new ArrayList<>();
                List<String> removed = new ArrayList<>();
                
                // 按内容相似性分组
                Map<String, List<Memory>> similarGroups = groupBySimilarity(memories);
                
                for (Map.Entry<String, List<Memory>> entry : similarGroups.entrySet()) {
                    List<Memory> group = entry.getValue();
                    if (group.size() > 1) {
                        // 合并相似内存
                        CompressedMemory merged = mergeSemanticallySimilar(group);
                        compressed.add(merged);
                        
                        // 标记原内存为已移除
                        for (Memory memory : group) {
                            removed.add(memory.getId());
                        }
                    }
                }
                
                CompressionResult.CompressionStatisticsSnapshot stats = 
                    new CompressionResult.CompressionStatisticsSnapshot(
                        memories.size(), compressed.size(), 
                        calculateTotalSize(memories), calculateCompressedSize(compressed),
                        0.0, 0, new HashMap<>());
                
                return new CompressionResult(compressed, removed, stats);
            });
        }
        
        private Map<String, List<Memory>> groupBySimilarity(List<Memory> memories) {
            // 简化的相似性分组
            Map<String, List<Memory>> groups = new HashMap<>();
            for (Memory memory : memories) {
                String key = generateSimilarityKey(memory.getContent());
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(memory);
            }
            return groups;
        }
        
        private String generateSimilarityKey(String content) {
            // 简化的相似性键生成
            return content.toLowerCase().replaceAll("\\s+", "").substring(0, Math.min(50, content.length()));
        }
        
        private CompressedMemory mergeSemanticallySimilar(List<Memory> memories) {
            StringBuilder mergedContent = new StringBuilder();
            List<String> originalIds = new ArrayList<>();
            List<Map<String, Object>> originalSegments = new ArrayList<>();
            
            for (Memory memory : memories) {
                originalIds.add(memory.getId());
                mergedContent.append(memory.getContent()).append(" ");
                
                Map<String, Object> segment = new HashMap<>();
                segment.put("content", memory.getContent());
                segment.put("timestamp", memory.getCreatedAt().toString());
                originalSegments.add(segment);
            }
            
            Map<String, Object> compressionMetadata = new HashMap<>();
            compressionMetadata.put("originalSegments", originalSegments);
            compressionMetadata.put("mergeReason", "semantic_similarity");
            
            return new CompressedMemory(
                UUID.randomUUID().toString(),
                mergedContent.toString().trim(),
                memories.get(0).getUserId(),
                memories.get(0).getSessionId(),
                originalIds,
                CompressedMemory.CompressionMethod.SEMANTIC_MERGE,
                (double) memories.size(),
                mergedContent.toString(),
                compressionMetadata
            );
        }
        
        @Override
        public String getStrategyName() { return "SemanticCompression"; }
        
        @Override
        public boolean supportsMemory(Memory memory) {
            return memory.getContent().length() > 50; // 支持较长的文本内容
        }
    }
    
    /**
     * 冗余去除策略
     */
    private class RedundancyRemovalStrategy implements CompressionStrategy {
        @Override
        public CompletableFuture<CompressionResult> compress(List<Memory> memories) {
            return CompletableFuture.supplyAsync(() -> {
                List<CompressedMemory> compressed = new ArrayList<>();
                List<String> removed = new ArrayList<>();
                
                Map<String, List<Memory>> duplicateGroups = findDuplicates(memories);
                
                for (List<Memory> group : duplicateGroups.values()) {
                    if (group.size() > 1) {
                        CompressedMemory deduplicated = createDeduplicatedMemory(group);
                        compressed.add(deduplicated);
                        
                        for (Memory memory : group) {
                            removed.add(memory.getId());
                        }
                    }
                }
                
                CompressionResult.CompressionStatisticsSnapshot stats = 
                    new CompressionResult.CompressionStatisticsSnapshot(
                        memories.size(), compressed.size(),
                        calculateTotalSize(memories), calculateCompressedSize(compressed),
                        0.0, 0, new HashMap<>());
                
                return new CompressionResult(compressed, removed, stats);
            });
        }
        
        private Map<String, List<Memory>> findDuplicates(List<Memory> memories) {
            Map<String, List<Memory>> duplicates = new HashMap<>();
            for (Memory memory : memories) {
                String contentHash = Integer.toString(memory.getContent().hashCode());
                duplicates.computeIfAbsent(contentHash, k -> new ArrayList<>()).add(memory);
            }
            return duplicates;
        }
        
        private CompressedMemory createDeduplicatedMemory(List<Memory> duplicates) {
            Memory representative = duplicates.get(0);
            List<String> originalIds = duplicates.stream()
                .map(Memory::getId)
                .collect(Collectors.toList());
            
            Map<String, Object> compressionMetadata = new HashMap<>();
            compressionMetadata.put("originalCount", duplicates.size());
            compressionMetadata.put("deduplicationReason", "exact_duplicate");
            
            return new CompressedMemory(
                UUID.randomUUID().toString(),
                representative.getContent(),
                representative.getUserId(),
                representative.getSessionId(),
                originalIds,
                CompressedMemory.CompressionMethod.REDUNDANCY_REMOVAL,
                duplicates.size(),
                representative.getContent(),
                compressionMetadata
            );
        }
        
        @Override
        public String getStrategyName() { return "RedundancyRemoval"; }
        
        @Override
        public boolean supportsMemory(Memory memory) {
            return true; // 支持所有内存类型
        }
    }
    
    /**
     * 时间衰减压缩策略
     */
    private class TemporalCompressionStrategy implements CompressionStrategy {
        @Override
        public CompletableFuture<CompressionResult> compress(List<Memory> memories) {
            return CompletableFuture.supplyAsync(() -> {
                List<CompressedMemory> compressed = new ArrayList<>();
                List<String> removed = new ArrayList<>();
                
                // 按时间分组压缩
                Map<String, List<Memory>> timeGroups = groupByTimeWindow(memories);
                
                for (List<Memory> group : timeGroups.values()) {
                    if (group.size() > 1) {
                        CompressedMemory timeCompressed = createTimeCompressedMemory(group);
                        compressed.add(timeCompressed);
                        
                        for (Memory memory : group) {
                            removed.add(memory.getId());
                        }
                    }
                }
                
                CompressionResult.CompressionStatisticsSnapshot stats = 
                    new CompressionResult.CompressionStatisticsSnapshot(
                        memories.size(), compressed.size(),
                        calculateTotalSize(memories), calculateCompressedSize(compressed),
                        0.0, 0, new HashMap<>());
                
                return new CompressionResult(compressed, removed, stats);
            });
        }
        
        private Map<String, List<Memory>> groupByTimeWindow(List<Memory> memories) {
            Map<String, List<Memory>> timeGroups = new HashMap<>();
            for (Memory memory : memories) {
                String timeKey = memory.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString(); // 按日分组
                timeGroups.computeIfAbsent(timeKey, k -> new ArrayList<>()).add(memory);
            }
            return timeGroups;
        }
        
        private CompressedMemory createTimeCompressedMemory(List<Memory> memories) {
            StringBuilder summary = new StringBuilder();
            List<String> originalIds = new ArrayList<>();
            List<Map<String, Object>> timeSlices = new ArrayList<>();
            
            memories.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));
            
            for (Memory memory : memories) {
                originalIds.add(memory.getId());
                summary.append(memory.getContent()).append("; ");
                
                Map<String, Object> slice = new HashMap<>();
                slice.put("content", memory.getContent());
                slice.put("timestamp", memory.getCreatedAt().toString());
                timeSlices.add(slice);
            }
            
            Map<String, Object> compressionMetadata = new HashMap<>();
            compressionMetadata.put("timeSlices", timeSlices);
            compressionMetadata.put("timeWindow", memories.get(0).getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString());
            
            return new CompressedMemory(
                UUID.randomUUID().toString(),
                "Daily summary: " + summary.toString().trim(),
                memories.get(0).getUserId(),
                memories.get(0).getSessionId(),
                originalIds,
                CompressedMemory.CompressionMethod.TEMPORAL_DECAY,
                memories.size(),
                summary.toString(),
                compressionMetadata
            );
        }
        
        @Override
        public String getStrategyName() { return "TemporalCompression"; }
        
        @Override
        public boolean supportsMemory(Memory memory) {
            Instant threshold = Instant.now().minus(
                configuration.getTemporalCompressionDays(), ChronoUnit.DAYS);
            return memory.getCreatedAt().isBefore(threshold);
        }
    }
    
    /**
     * 内容摘要策略
     */
    private class ContentSummaryStrategy implements CompressionStrategy {
        @Override
        public CompletableFuture<CompressionResult> compress(List<Memory> memories) {
            return CompletableFuture.supplyAsync(() -> {
                List<CompressedMemory> compressed = new ArrayList<>();
                List<String> removed = new ArrayList<>();
                
                for (Memory memory : memories) {
                    if (memory.getContent().length() > 200) { // 长内容才进行摘要
                        CompressedMemory summarized = createSummarizedMemory(memory);
                        compressed.add(summarized);
                        removed.add(memory.getId());
                    }
                }
                
                CompressionResult.CompressionStatisticsSnapshot stats = 
                    new CompressionResult.CompressionStatisticsSnapshot(
                        memories.size(), compressed.size(),
                        calculateTotalSize(memories), calculateCompressedSize(compressed),
                        0.0, 0, new HashMap<>());
                
                return new CompressionResult(compressed, removed, stats);
            });
        }
        
        private CompressedMemory createSummarizedMemory(Memory memory) {
            String summary = generateSummary(memory.getContent());
            
            Map<String, Object> compressionMetadata = new HashMap<>();
            compressionMetadata.put("summarizationRatio", (double) summary.length() / memory.getContent().length());
            compressionMetadata.put("originalLength", memory.getContent().length());
            
            return new CompressedMemory(
                UUID.randomUUID().toString(),
                summary,
                memory.getUserId(),
                memory.getSessionId(),
                Arrays.asList(memory.getId()),
                CompressedMemory.CompressionMethod.CONTENT_SUMMARY,
                (double) memory.getContent().length() / summary.length(),
                memory.getContent(),
                compressionMetadata
            );
        }
        
        private String generateSummary(String content) {
            // 简化的摘要生成：取前几个句子
            String[] sentences = content.split("[.!?]+");
            if (sentences.length <= 2) {
                return content;
            }
            
            StringBuilder summary = new StringBuilder();
            int maxSentences = Math.min(2, sentences.length);
            
            for (int i = 0; i < maxSentences; i++) {
                summary.append(sentences[i].trim());
                if (i < maxSentences - 1) {
                    summary.append(". ");
                }
            }
            
            summary.append("...");
            return summary.toString();
        }
        
        @Override
        public String getStrategyName() { return "ContentSummary"; }
        
        @Override
        public boolean supportsMemory(Memory memory) {
            return memory.getContent().length() > 100; // 支持较长的内容
        }
    }
}