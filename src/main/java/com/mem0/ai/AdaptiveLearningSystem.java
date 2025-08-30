package com.mem0.ai;

import com.mem0.memory.Memory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * AdaptiveLearningSystem - 自适应学习系统
 * 
 * 提供基于用户行为和内存使用模式的自适应学习功能。
 * 通过持续学习用户偏好、使用习惯和内存模式，优化内存管理策略。
 * 
 * 主要功能：
 * 1. 用户行为学习 - 分析用户的搜索、访问、创建模式
 * 2. 内存使用模式分析 - 识别高频、重要、相关的内存
 * 3. 个性化推荐 - 基于学习结果提供个性化内容推荐
 * 4. 动态策略调整 - 根据学习结果调整内存管理策略
 * 5. 预测性优化 - 预测用户需求并提前优化内存布局
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 2024-12-20
 */
public class AdaptiveLearningSystem {
    
    private static final Logger logger = LoggerFactory.getLogger(AdaptiveLearningSystem.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final LearningConfiguration configuration;
    private final UserBehaviorAnalyzer behaviorAnalyzer;
    private final MemoryPatternAnalyzer patternAnalyzer;
    private final RecommendationEngine recommendationEngine;
    private final LearningStatistics learningStatistics;
    private final Map<String, UserProfile> userProfiles;
    
    /**
     * 学习配置
     */
    public static class LearningConfiguration {
        private boolean enableBehaviorLearning = true;
        private boolean enablePatternAnalysis = true;
        private boolean enableRecommendations = true;
        private boolean enablePredictiveOptimization = true;
        private int learningWindowDays = 30; // 学习窗口期（天）
        private double learningRate = 0.1; // 学习率
        private int minInteractionsForLearning = 5; // 最小交互次数
        private double confidenceThreshold = 0.7; // 置信度阈值
        private int maxRecommendations = 10; // 最大推荐数量
        
        // Getters and setters
        public boolean isEnableBehaviorLearning() { return enableBehaviorLearning; }
        public void setEnableBehaviorLearning(boolean enableBehaviorLearning) { this.enableBehaviorLearning = enableBehaviorLearning; }
        
        public boolean isEnablePatternAnalysis() { return enablePatternAnalysis; }
        public void setEnablePatternAnalysis(boolean enablePatternAnalysis) { this.enablePatternAnalysis = enablePatternAnalysis; }
        
        public boolean isEnableRecommendations() { return enableRecommendations; }
        public void setEnableRecommendations(boolean enableRecommendations) { this.enableRecommendations = enableRecommendations; }
        
        public boolean isEnablePredictiveOptimization() { return enablePredictiveOptimization; }
        public void setEnablePredictiveOptimization(boolean enablePredictiveOptimization) { this.enablePredictiveOptimization = enablePredictiveOptimization; }
        
        public int getLearningWindowDays() { return learningWindowDays; }
        public void setLearningWindowDays(int learningWindowDays) { this.learningWindowDays = learningWindowDays; }
        
        public double getLearningRate() { return learningRate; }
        public void setLearningRate(double learningRate) { this.learningRate = learningRate; }
        
        public int getMinInteractionsForLearning() { return minInteractionsForLearning; }
        public void setMinInteractionsForLearning(int minInteractionsForLearning) { this.minInteractionsForLearning = minInteractionsForLearning; }
        
        public double getConfidenceThreshold() { return confidenceThreshold; }
        public void setConfidenceThreshold(double confidenceThreshold) { this.confidenceThreshold = confidenceThreshold; }
        
        public int getMaxRecommendations() { return maxRecommendations; }
        public void setMaxRecommendations(int maxRecommendations) { this.maxRecommendations = maxRecommendations; }
    }
    
    /**
     * 用户行为事件
     */
    public static class UserInteractionEvent {
        private final String userId;
        private final String sessionId;
        private final InteractionType interactionType;
        private final String targetMemoryId;
        private final String query;
        private final Map<String, Object> context;
        private final LocalDateTime timestamp;
        private final double satisfaction; // 满意度评分 0-1
        
        public enum InteractionType {
            MEMORY_CREATE,
            MEMORY_READ,
            MEMORY_UPDATE,
            MEMORY_DELETE,
            SEARCH_QUERY,
            RECOMMENDATION_CLICK,
            MEMORY_SHARE,
            SESSION_START,
            SESSION_END
        }
        
        public UserInteractionEvent(String userId, String sessionId, InteractionType interactionType,
                                  String targetMemoryId, String query, Map<String, Object> context,
                                  double satisfaction) {
            this.userId = userId;
            this.sessionId = sessionId;
            this.interactionType = interactionType;
            this.targetMemoryId = targetMemoryId;
            this.query = query;
            this.context = context != null ? context : new HashMap<>();
            this.satisfaction = satisfaction;
            this.timestamp = LocalDateTime.now();
        }
        
        // Getters
        public String getUserId() { return userId; }
        public String getSessionId() { return sessionId; }
        public InteractionType getInteractionType() { return interactionType; }
        public String getTargetMemoryId() { return targetMemoryId; }
        public String getQuery() { return query; }
        public Map<String, Object> getContext() { return context; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public double getSatisfaction() { return satisfaction; }
    }
    
    /**
     * 用户画像
     */
    public static class UserProfile {
        private final String userId;
        private final Map<String, Double> topicPreferences;
        private final Map<String, Double> contentTypePreferences;
        private final Map<String, Integer> searchPatterns;
        private final Map<String, Double> timePatterns;
        private final List<String> frequentKeywords;
        private final double averageSatisfaction;
        private final int totalInteractions;
        private final LocalDateTime lastUpdated;
        private final Map<String, Object> personalizedSettings;
        
        public UserProfile(String userId, Map<String, Double> topicPreferences,
                         Map<String, Double> contentTypePreferences, Map<String, Integer> searchPatterns,
                         Map<String, Double> timePatterns, List<String> frequentKeywords,
                         double averageSatisfaction, int totalInteractions,
                         Map<String, Object> personalizedSettings) {
            this.userId = userId;
            this.topicPreferences = topicPreferences != null ? topicPreferences : new HashMap<>();
            this.contentTypePreferences = contentTypePreferences != null ? contentTypePreferences : new HashMap<>();
            this.searchPatterns = searchPatterns != null ? searchPatterns : new HashMap<>();
            this.timePatterns = timePatterns != null ? timePatterns : new HashMap<>();
            this.frequentKeywords = frequentKeywords != null ? frequentKeywords : new ArrayList<>();
            this.averageSatisfaction = averageSatisfaction;
            this.totalInteractions = totalInteractions;
            this.lastUpdated = LocalDateTime.now();
            this.personalizedSettings = personalizedSettings != null ? personalizedSettings : new HashMap<>();
        }
        
        // Getters
        public String getUserId() { return userId; }
        public Map<String, Double> getTopicPreferences() { return topicPreferences; }
        public Map<String, Double> getContentTypePreferences() { return contentTypePreferences; }
        public Map<String, Integer> getSearchPatterns() { return searchPatterns; }
        public Map<String, Double> getTimePatterns() { return timePatterns; }
        public List<String> getFrequentKeywords() { return frequentKeywords; }
        public double getAverageSatisfaction() { return averageSatisfaction; }
        public int getTotalInteractions() { return totalInteractions; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public Map<String, Object> getPersonalizedSettings() { return personalizedSettings; }
    }
    
    /**
     * 内存模式
     */
    public static class MemoryPattern {
        private final String patternId;
        private final PatternType patternType;
        private final List<String> relatedMemoryIds;
        private final double strength; // 模式强度
        private final double frequency; // 出现频率
        private final Map<String, Object> characteristics;
        private final LocalDateTime discoveredAt;
        
        public enum PatternType {
            SEQUENTIAL_ACCESS,    // 顺序访问模式
            CO_OCCURRENCE,       // 共现模式
            TEMPORAL_CLUSTERING, // 时间聚类模式
            TOPIC_CORRELATION,   // 主题相关模式
            USER_WORKFLOW       // 用户工作流模式
        }
        
        public MemoryPattern(String patternId, PatternType patternType, List<String> relatedMemoryIds,
                           double strength, double frequency, Map<String, Object> characteristics) {
            this.patternId = patternId;
            this.patternType = patternType;
            this.relatedMemoryIds = relatedMemoryIds != null ? relatedMemoryIds : new ArrayList<>();
            this.strength = strength;
            this.frequency = frequency;
            this.characteristics = characteristics != null ? characteristics : new HashMap<>();
            this.discoveredAt = LocalDateTime.now();
        }
        
        // Getters
        public String getPatternId() { return patternId; }
        public PatternType getPatternType() { return patternType; }
        public List<String> getRelatedMemoryIds() { return relatedMemoryIds; }
        public double getStrength() { return strength; }
        public double getFrequency() { return frequency; }
        public Map<String, Object> getCharacteristics() { return characteristics; }
        public LocalDateTime getDiscoveredAt() { return discoveredAt; }
    }
    
    /**
     * 推荐结果
     */
    public static class RecommendationResult {
        private final List<MemoryRecommendation> recommendations;
        private final Map<String, Object> explanations;
        private final double overallConfidence;
        private final String recommendationId;
        private final LocalDateTime generatedAt;
        
        public static class MemoryRecommendation {
            private final String memoryId;
            private final double relevanceScore;
            private final String reason;
            private final Map<String, Object> context;
            
            public MemoryRecommendation(String memoryId, double relevanceScore, String reason,
                                      Map<String, Object> context) {
                this.memoryId = memoryId;
                this.relevanceScore = relevanceScore;
                this.reason = reason;
                this.context = context != null ? context : new HashMap<>();
            }
            
            // Getters
            public String getMemoryId() { return memoryId; }
            public double getRelevanceScore() { return relevanceScore; }
            public String getReason() { return reason; }
            public Map<String, Object> getContext() { return context; }
        }
        
        public RecommendationResult(List<MemoryRecommendation> recommendations,
                                  Map<String, Object> explanations, double overallConfidence) {
            this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
            this.explanations = explanations != null ? explanations : new HashMap<>();
            this.overallConfidence = overallConfidence;
            this.recommendationId = UUID.randomUUID().toString();
            this.generatedAt = LocalDateTime.now();
        }
        
        // Getters
        public List<MemoryRecommendation> getRecommendations() { return recommendations; }
        public Map<String, Object> getExplanations() { return explanations; }
        public double getOverallConfidence() { return overallConfidence; }
        public String getRecommendationId() { return recommendationId; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
    }
    
    /**
     * 学习统计
     */
    public static class LearningStatistics {
        private final AtomicLong totalInteractions = new AtomicLong(0);
        private final AtomicLong totalPatternsDiscovered = new AtomicLong(0);
        private final AtomicLong totalRecommendationsGenerated = new AtomicLong(0);
        private final Map<String, AtomicLong> interactionTypeCount = new ConcurrentHashMap<>();
        private final Map<String, Double> patternAccuracy = new ConcurrentHashMap<>();
        private final Map<String, Double> recommendationAccuracy = new ConcurrentHashMap<>();
        
        public void recordInteraction(UserInteractionEvent.InteractionType type) {
            totalInteractions.incrementAndGet();
            interactionTypeCount.computeIfAbsent(type.name(), k -> new AtomicLong(0)).incrementAndGet();
        }
        
        public void recordPatternDiscovered(String patternType, double accuracy) {
            totalPatternsDiscovered.incrementAndGet();
            patternAccuracy.put(patternType, accuracy);
        }
        
        public void recordRecommendation(String recommendationType, double accuracy) {
            totalRecommendationsGenerated.incrementAndGet();
            recommendationAccuracy.put(recommendationType, accuracy);
        }
        
        // Getters
        public long getTotalInteractions() { return totalInteractions.get(); }
        public long getTotalPatternsDiscovered() { return totalPatternsDiscovered.get(); }
        public long getTotalRecommendationsGenerated() { return totalRecommendationsGenerated.get(); }
        public Map<String, Long> getInteractionTypeCount() {
            return interactionTypeCount.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
        }
        public Map<String, Double> getPatternAccuracy() { return new HashMap<>(patternAccuracy); }
        public Map<String, Double> getRecommendationAccuracy() { return new HashMap<>(recommendationAccuracy); }
    }
    
    /**
     * 构造函数
     * 
     * @param configuration 学习配置
     */
    public AdaptiveLearningSystem(LearningConfiguration configuration) {
        this.configuration = configuration != null ? configuration : new LearningConfiguration();
        this.behaviorAnalyzer = new UserBehaviorAnalyzer();
        this.patternAnalyzer = new MemoryPatternAnalyzer();
        this.recommendationEngine = new RecommendationEngine();
        this.learningStatistics = new LearningStatistics();
        this.userProfiles = new ConcurrentHashMap<>();
        
        logger.info("AdaptiveLearningSystem initialized");
    }
    
    /**
     * 默认构造函数
     */
    public AdaptiveLearningSystem() {
        this(new LearningConfiguration());
    }
    
    /**
     * 记录用户交互事件
     * 
     * @param event 交互事件
     * @return 异步处理任务
     */
    public CompletableFuture<Void> recordInteraction(UserInteractionEvent event) {
        return CompletableFuture.runAsync(() -> {
            try {
                // 更新学习统计
                learningStatistics.recordInteraction(event.getInteractionType());
                
                // 更新用户行为分析
                if (configuration.isEnableBehaviorLearning()) {
                    behaviorAnalyzer.processInteraction(event);
                }
                
                // 更新用户画像
                updateUserProfile(event);
                
                logger.debug("Recorded interaction: {} for user {}", 
                           event.getInteractionType(), event.getUserId());
                
            } catch (Exception e) {
                logger.error("Error recording interaction", e);
            }
        });
    }
    
    /**
     * 分析内存模式
     * 
     * @param memories 内存列表
     * @param userId 用户ID
     * @return 发现的模式列表
     */
    public CompletableFuture<List<MemoryPattern>> analyzeMemoryPatterns(List<Memory> memories, String userId) {
        if (!configuration.isEnablePatternAnalysis()) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return patternAnalyzer.discoverPatterns(memories, userId);
            } catch (Exception e) {
                logger.error("Error analyzing memory patterns for user: " + userId, e);
                return new ArrayList<>();
            }
        });
    }
    
    /**
     * 生成个性化推荐
     * 
     * @param userId 用户ID
     * @param context 上下文信息
     * @param candidateMemories 候选内存列表
     * @return 推荐结果
     */
    public CompletableFuture<RecommendationResult> generateRecommendations(
            String userId, Map<String, Object> context, List<Memory> candidateMemories) {
        
        if (!configuration.isEnableRecommendations()) {
            return CompletableFuture.completedFuture(
                new RecommendationResult(new ArrayList<>(), new HashMap<>(), 0.0));
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                UserProfile userProfile = userProfiles.get(userId);
                if (userProfile == null) {
                    logger.debug("No user profile found for user: {}", userId);
                    return new RecommendationResult(new ArrayList<>(), new HashMap<>(), 0.0);
                }
                
                return recommendationEngine.generateRecommendations(
                    userProfile, context, candidateMemories);
                
            } catch (Exception e) {
                logger.error("Error generating recommendations for user: " + userId, e);
                return new RecommendationResult(new ArrayList<>(), new HashMap<>(), 0.0);
            }
        });
    }
    
    /**
     * 获取用户画像
     * 
     * @param userId 用户ID
     * @return 用户画像
     */
    public UserProfile getUserProfile(String userId) {
        return userProfiles.get(userId);
    }
    
    /**
     * 更新用户画像
     * 
     * @param event 交互事件
     */
    private void updateUserProfile(UserInteractionEvent event) {
        String userId = event.getUserId();
        
        UserProfile currentProfile = userProfiles.get(userId);
        if (currentProfile == null) {
            // 创建新的用户画像
            currentProfile = createInitialUserProfile(userId);
        }
        
        // 基于事件更新画像
        UserProfile updatedProfile = updateProfileWithEvent(currentProfile, event);
        userProfiles.put(userId, updatedProfile);
    }
    
    /**
     * 创建初始用户画像
     * 
     * @param userId 用户ID
     * @return 初始用户画像
     */
    private UserProfile createInitialUserProfile(String userId) {
        return new UserProfile(
            userId,
            new HashMap<>(),
            new HashMap<>(),
            new HashMap<>(),
            new HashMap<>(),
            new ArrayList<>(),
            0.5, // 初始满意度
            0,
            new HashMap<>()
        );
    }
    
    /**
     * 基于事件更新用户画像
     * 
     * @param currentProfile 当前画像
     * @param event 交互事件
     * @return 更新后的画像
     */
    private UserProfile updateProfileWithEvent(UserProfile currentProfile, UserInteractionEvent event) {
        // 更新主题偏好
        Map<String, Double> topicPreferences = new HashMap<>(currentProfile.getTopicPreferences());
        if (event.getQuery() != null) {
            String topic = extractTopicFromQuery(event.getQuery());
            topicPreferences.merge(topic, configuration.getLearningRate(), Double::sum);
        }
        
        // 更新内容类型偏好
        Map<String, Double> contentTypePreferences = new HashMap<>(currentProfile.getContentTypePreferences());
        String contentType = (String) event.getContext().get("contentType");
        if (contentType != null) {
            contentTypePreferences.merge(contentType, configuration.getLearningRate(), Double::sum);
        }
        
        // 更新搜索模式
        Map<String, Integer> searchPatterns = new HashMap<>(currentProfile.getSearchPatterns());
        if (event.getInteractionType() == UserInteractionEvent.InteractionType.SEARCH_QUERY) {
            String pattern = categorizeSearchPattern(event.getQuery());
            searchPatterns.merge(pattern, 1, Integer::sum);
        }
        
        // 更新时间模式
        Map<String, Double> timePatterns = new HashMap<>(currentProfile.getTimePatterns());
        String timeSlot = categorizeTimeSlot(event.getTimestamp());
        timePatterns.merge(timeSlot, configuration.getLearningRate(), Double::sum);
        
        // 更新频繁关键词
        List<String> frequentKeywords = new ArrayList<>(currentProfile.getFrequentKeywords());
        if (event.getQuery() != null) {
            List<String> queryKeywords = extractKeywords(event.getQuery());
            for (String keyword : queryKeywords) {
                if (!frequentKeywords.contains(keyword) && frequentKeywords.size() < 20) {
                    frequentKeywords.add(keyword);
                }
            }
        }
        
        // 更新平均满意度
        double newAverageSatisfaction = (currentProfile.getAverageSatisfaction() * currentProfile.getTotalInteractions() + 
                                       event.getSatisfaction()) / (currentProfile.getTotalInteractions() + 1);
        
        return new UserProfile(
            currentProfile.getUserId(),
            topicPreferences,
            contentTypePreferences,
            searchPatterns,
            timePatterns,
            frequentKeywords,
            newAverageSatisfaction,
            currentProfile.getTotalInteractions() + 1,
            currentProfile.getPersonalizedSettings()
        );
    }
    
    /**
     * 从查询中提取主题
     * 
     * @param query 查询内容
     * @return 主题
     */
    private String extractTopicFromQuery(String query) {
        // 简化的主题提取
        String lowerQuery = query.toLowerCase();
        
        if (lowerQuery.contains("技术") || lowerQuery.contains("编程") || lowerQuery.contains("代码")) {
            return "technology";
        } else if (lowerQuery.contains("工作") || lowerQuery.contains("项目") || lowerQuery.contains("任务")) {
            return "work";
        } else if (lowerQuery.contains("学习") || lowerQuery.contains("知识") || lowerQuery.contains("研究")) {
            return "learning";
        } else if (lowerQuery.contains("生活") || lowerQuery.contains("日常") || lowerQuery.contains("个人")) {
            return "personal";
        } else {
            return "general";
        }
    }
    
    /**
     * 分类搜索模式
     * 
     * @param query 查询内容
     * @return 搜索模式
     */
    private String categorizeSearchPattern(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "empty";
        }
        
        if (query.length() < 5) {
            return "short";
        } else if (query.length() > 50) {
            return "detailed";
        } else if (query.contains("?")) {
            return "question";
        } else if (query.split("\\s+").length == 1) {
            return "keyword";
        } else {
            return "phrase";
        }
    }
    
    /**
     * 分类时间段
     * 
     * @param timestamp 时间戳
     * @return 时间段
     */
    private String categorizeTimeSlot(LocalDateTime timestamp) {
        int hour = timestamp.getHour();
        
        if (hour >= 6 && hour < 12) {
            return "morning";
        } else if (hour >= 12 && hour < 18) {
            return "afternoon";
        } else if (hour >= 18 && hour < 22) {
            return "evening";
        } else {
            return "night";
        }
    }
    
    /**
     * 提取关键词
     * 
     * @param text 文本
     * @return 关键词列表
     */
    private List<String> extractKeywords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        return Arrays.stream(text.toLowerCase().split("\\s+"))
            .filter(word -> word.length() > 2)
            .filter(word -> !isStopWord(word))
            .distinct()
            .limit(5)
            .collect(Collectors.toList());
    }
    
    /**
     * 检查是否为停用词
     * 
     * @param word 单词
     * @return 是否为停用词
     */
    private boolean isStopWord(String word) {
        Set<String> stopWords = new HashSet<>();
        Collections.addAll(stopWords, "the", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by",
            "是", "的", "了", "在", "有", "和", "就", "不", "人", "都", "一", "个", "上", "也");
        return stopWords.contains(word.toLowerCase());
    }
    
    /**
     * 获取学习统计
     * 
     * @return 学习统计对象
     */
    public LearningStatistics getLearningStatistics() {
        return learningStatistics;
    }
    
    /**
     * 获取学习配置
     * 
     * @return 学习配置
     */
    public LearningConfiguration getConfiguration() {
        return configuration;
    }
    
    /**
     * 获取所有用户画像
     * 
     * @return 用户画像映射
     */
    public Map<String, UserProfile> getAllUserProfiles() {
        return new HashMap<>(userProfiles);
    }
    
    // 内部组件类
    
    /**
     * 用户行为分析器
     */
    private class UserBehaviorAnalyzer {
        private final Map<String, List<UserInteractionEvent>> userInteractionHistory = new ConcurrentHashMap<>();
        
        public void processInteraction(UserInteractionEvent event) {
            String userId = event.getUserId();
            userInteractionHistory.computeIfAbsent(userId, k -> new ArrayList<>()).add(event);
            
            // 保持历史记录在合理范围内
            List<UserInteractionEvent> history = userInteractionHistory.get(userId);
            if (history.size() > 1000) { // 最多保留1000条记录
                history.subList(0, history.size() - 1000).clear();
            }
        }
        
        public List<UserInteractionEvent> getUserHistory(String userId) {
            return userInteractionHistory.getOrDefault(userId, new ArrayList<>());
        }
    }
    
    /**
     * 内存模式分析器
     */
    private class MemoryPatternAnalyzer {
        public List<MemoryPattern> discoverPatterns(List<Memory> memories, String userId) {
            List<MemoryPattern> patterns = new ArrayList<>();
            
            try {
                // 发现共现模式
                patterns.addAll(discoverCoOccurrencePatterns(memories));
                
                // 发现时间聚类模式
                patterns.addAll(discoverTemporalClusteringPatterns(memories));
                
                // 发现主题相关模式
                patterns.addAll(discoverTopicCorrelationPatterns(memories));
                
                // 更新统计
                for (MemoryPattern pattern : patterns) {
                    learningStatistics.recordPatternDiscovered(
                        pattern.getPatternType().name(), pattern.getStrength());
                }
                
            } catch (Exception e) {
                logger.error("Error discovering patterns", e);
            }
            
            return patterns;
        }
        
        private List<MemoryPattern> discoverCoOccurrencePatterns(List<Memory> memories) {
            List<MemoryPattern> patterns = new ArrayList<>();
            
            // 简化的共现分析
            Map<String, Integer> wordCounts = new HashMap<>();
            Map<String, Set<String>> wordToMemories = new HashMap<>();
            
            for (Memory memory : memories) {
                String[] words = memory.getContent().toLowerCase().split("\\s+");
                for (String word : words) {
                    if (word.length() > 3 && !isStopWord(word)) {
                        wordCounts.merge(word, 1, Integer::sum);
                        wordToMemories.computeIfAbsent(word, k -> new HashSet<>()).add(memory.getId());
                    }
                }
            }
            
            // 找出频繁共现的词汇组合
            for (Map.Entry<String, Integer> entry : wordCounts.entrySet()) {
                if (entry.getValue() >= 3) { // 至少出现3次
                    String word = entry.getKey();
                    Set<String> relatedMemories = wordToMemories.get(word);
                    
                    if (relatedMemories.size() >= 2) {
                        MemoryPattern pattern = new MemoryPattern(
                            UUID.randomUUID().toString(),
                            MemoryPattern.PatternType.CO_OCCURRENCE,
                            new ArrayList<>(relatedMemories),
                            (double) entry.getValue() / memories.size(),
                            (double) entry.getValue() / memories.size(),
                            mapOf("keyword", word, "occurrences", entry.getValue())
                        );
                        patterns.add(pattern);
                    }
                }
            }
            
            return patterns;
        }
        
        private List<MemoryPattern> discoverTemporalClusteringPatterns(List<Memory> memories) {
            List<MemoryPattern> patterns = new ArrayList<>();
            
            // 按时间排序
            List<Memory> sortedMemories = memories.stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .collect(Collectors.toList());
            
            // 寻找时间聚类
            List<List<Memory>> clusters = new ArrayList<>();
            List<Memory> currentCluster = new ArrayList<>();
            
            for (int i = 0; i < sortedMemories.size(); i++) {
                Memory memory = sortedMemories.get(i);
                
                if (currentCluster.isEmpty()) {
                    currentCluster.add(memory);
                } else {
                    Memory lastMemory = currentCluster.get(currentCluster.size() - 1);
                    long hoursDiff = ChronoUnit.HOURS.between(lastMemory.getCreatedAt(), memory.getCreatedAt());
                    
                    if (hoursDiff <= 2) { // 2小时内的视为同一聚类
                        currentCluster.add(memory);
                    } else {
                        if (currentCluster.size() >= 2) {
                            clusters.add(new ArrayList<>(currentCluster));
                        }
                        currentCluster.clear();
                        currentCluster.add(memory);
                    }
                }
            }
            
            // 添加最后一个聚类
            if (currentCluster.size() >= 2) {
                clusters.add(currentCluster);
            }
            
            // 创建模式
            for (List<Memory> cluster : clusters) {
                List<String> memoryIds = cluster.stream()
                    .map(Memory::getId)
                    .collect(Collectors.toList());
                
                MemoryPattern pattern = new MemoryPattern(
                    UUID.randomUUID().toString(),
                    MemoryPattern.PatternType.TEMPORAL_CLUSTERING,
                    memoryIds,
                    (double) cluster.size() / memories.size(),
                    1.0,
                    mapOf("clusterSize", cluster.size(), 
                          "timeSpan", ChronoUnit.HOURS.between(
                              cluster.get(0).getCreatedAt(), 
                              cluster.get(cluster.size() - 1).getCreatedAt()))
                );
                patterns.add(pattern);
            }
            
            return patterns;
        }
        
        private List<MemoryPattern> discoverTopicCorrelationPatterns(List<Memory> memories) {
            List<MemoryPattern> patterns = new ArrayList<>();
            
            // 基于元数据中的主题信息寻找相关性
            Map<String, List<Memory>> topicGroups = memories.stream()
                .filter(m -> m.getMetadata().containsKey("topic"))
                .collect(Collectors.groupingBy(m -> (String) m.getMetadata().get("topic")));
            
            for (Map.Entry<String, List<Memory>> entry : topicGroups.entrySet()) {
                if (entry.getValue().size() >= 3) { // 至少3个相关内存
                    List<String> memoryIds = entry.getValue().stream()
                        .map(Memory::getId)
                        .collect(Collectors.toList());
                    
                    MemoryPattern pattern = new MemoryPattern(
                        UUID.randomUUID().toString(),
                        MemoryPattern.PatternType.TOPIC_CORRELATION,
                        memoryIds,
                        (double) entry.getValue().size() / memories.size(),
                        (double) entry.getValue().size() / memories.size(),
                        mapOf("topic", entry.getKey(), "memoryCount", entry.getValue().size())
                    );
                    patterns.add(pattern);
                }
            }
            
            return patterns;
        }
    }
    
    /**
     * 推荐引擎
     */
    private class RecommendationEngine {
        public RecommendationResult generateRecommendations(UserProfile userProfile, 
                                                          Map<String, Object> context,
                                                          List<Memory> candidateMemories) {
            
            List<RecommendationResult.MemoryRecommendation> recommendations = new ArrayList<>();
            Map<String, Object> explanations = new HashMap<>();
            
            try {
                // 基于主题偏好的推荐
                recommendations.addAll(generateTopicBasedRecommendations(userProfile, candidateMemories));
                
                // 基于内容类型偏好的推荐
                recommendations.addAll(generateContentTypeBasedRecommendations(userProfile, candidateMemories));
                
                // 基于时间模式的推荐
                recommendations.addAll(generateTimeBasedRecommendations(userProfile, candidateMemories, context));
                
                // 基于频繁关键词的推荐
                recommendations.addAll(generateKeywordBasedRecommendations(userProfile, candidateMemories));
                
                // 去重并排序
                recommendations = recommendations.stream()
                    .collect(Collectors.toMap(
                        RecommendationResult.MemoryRecommendation::getMemoryId,
                        r -> r,
                        (existing, replacement) -> existing.getRelevanceScore() > replacement.getRelevanceScore() ? existing : replacement
                    ))
                    .values()
                    .stream()
                    .sorted((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()))
                    .limit(configuration.getMaxRecommendations())
                    .collect(Collectors.toList());
                
                // 计算整体置信度
                double overallConfidence = recommendations.stream()
                    .mapToDouble(RecommendationResult.MemoryRecommendation::getRelevanceScore)
                    .average()
                    .orElse(0.0);
                
                // 生成解释
                explanations.put("totalCandidates", candidateMemories.size());
                explanations.put("selectedCount", recommendations.size());
                explanations.put("userInteractions", userProfile.getTotalInteractions());
                explanations.put("userSatisfaction", userProfile.getAverageSatisfaction());
                
                // 更新统计
                learningStatistics.recordRecommendation("combined", overallConfidence);
                
            } catch (Exception e) {
                logger.error("Error generating recommendations", e);
            }
            
            return new RecommendationResult(recommendations, explanations, 
                                          Math.min(recommendations.size() / 10.0, 1.0));
        }
        
        private List<RecommendationResult.MemoryRecommendation> generateTopicBasedRecommendations(
                UserProfile userProfile, List<Memory> candidateMemories) {
            
            List<RecommendationResult.MemoryRecommendation> recommendations = new ArrayList<>();
            
            for (Memory memory : candidateMemories) {
                String topic = extractTopicFromQuery(memory.getContent());
                Double topicPreference = userProfile.getTopicPreferences().get(topic);
                
                if (topicPreference != null && topicPreference > 0.1) {
                    recommendations.add(new RecommendationResult.MemoryRecommendation(
                        memory.getId(),
                        topicPreference,
                        "Based on your interest in " + topic,
                        mapOf("topic", topic, "preference", topicPreference)
                    ));
                }
            }
            
            return recommendations;
        }
        
        private List<RecommendationResult.MemoryRecommendation> generateContentTypeBasedRecommendations(
                UserProfile userProfile, List<Memory> candidateMemories) {
            
            List<RecommendationResult.MemoryRecommendation> recommendations = new ArrayList<>();
            
            for (Memory memory : candidateMemories) {
                String contentType = (String) memory.getMetadata().get("contentType");
                if (contentType != null) {
                    Double typePreference = userProfile.getContentTypePreferences().get(contentType);
                    
                    if (typePreference != null && typePreference > 0.1) {
                        recommendations.add(new RecommendationResult.MemoryRecommendation(
                            memory.getId(),
                            typePreference * 0.8, // 权重稍低
                            "You frequently access " + contentType + " content",
                            mapOf("contentType", contentType, "preference", typePreference)
                        ));
                    }
                }
            }
            
            return recommendations;
        }
        
        private List<RecommendationResult.MemoryRecommendation> generateTimeBasedRecommendations(
                UserProfile userProfile, List<Memory> candidateMemories, Map<String, Object> context) {
            
            List<RecommendationResult.MemoryRecommendation> recommendations = new ArrayList<>();
            
            String currentTimeSlot = categorizeTimeSlot(LocalDateTime.now());
            Double timePreference = userProfile.getTimePatterns().get(currentTimeSlot);
            
            if (timePreference != null && timePreference > 0.2) {
                // 推荐在相似时间段创建的内存
                for (Memory memory : candidateMemories) {
                    String memoryTimeSlot = categorizeTimeSlot(LocalDateTime.ofInstant(memory.getCreatedAt(), java.time.ZoneId.systemDefault()));
                    if (memoryTimeSlot.equals(currentTimeSlot)) {
                        recommendations.add(new RecommendationResult.MemoryRecommendation(
                            memory.getId(),
                            timePreference * 0.6,
                            "You're typically active with similar content at this time",
                            mapOf("timeSlot", currentTimeSlot, "preference", timePreference)
                        ));
                    }
                }
            }
            
            return recommendations;
        }
        
        private List<RecommendationResult.MemoryRecommendation> generateKeywordBasedRecommendations(
                UserProfile userProfile, List<Memory> candidateMemories) {
            
            List<RecommendationResult.MemoryRecommendation> recommendations = new ArrayList<>();
            
            for (Memory memory : candidateMemories) {
                List<String> memoryKeywords = extractKeywords(memory.getContent());
                
                double keywordScore = 0.0;
                List<String> matchedKeywords = new ArrayList<>();
                
                for (String keyword : memoryKeywords) {
                    if (userProfile.getFrequentKeywords().contains(keyword)) {
                        keywordScore += 0.2;
                        matchedKeywords.add(keyword);
                    }
                }
                
                if (keywordScore > 0) {
                    recommendations.add(new RecommendationResult.MemoryRecommendation(
                        memory.getId(),
                        Math.min(keywordScore, 1.0),
                        "Contains keywords you frequently search: " + String.join(", ", matchedKeywords),
                        mapOf("matchedKeywords", matchedKeywords, "keywordScore", keywordScore)
                    ));
                }
            }
            
            return recommendations;
        }
    }
    
    // Java 8兼容的Map创建辅助方法
    private static Map<String, Object> mapOf(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }
    
    private static Map<String, Object> mapOf(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4) {
        Map<String, Object> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        return map;
    }
}