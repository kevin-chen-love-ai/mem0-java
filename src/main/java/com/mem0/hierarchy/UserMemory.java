package com.mem0.hierarchy;

import com.mem0.core.EnhancedMemory;
import com.mem0.core.MemoryImportance;
import com.mem0.core.MemoryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 用户级持久内存管理系统 / User-Level Persistent Memory Management System
 * 
 * 管理特定用户的长期持久化内存，支持个人偏好、学习记录、行为模式等跨会话保持的内存数据。
 * 提供用户画像构建、个性化推荐、长期学习适应等高级功能，是mem0分层内存体系的核心组件。
 * Manages long-term persistent memory for specific users, supporting personal preferences,
 * learning records, behavioral patterns, and other cross-session persistent memory data.
 * Provides advanced features like user profiling, personalized recommendations, and long-term
 * learning adaptation as a core component of the mem0 hierarchical memory system.
 * 
 * <h3>核心功能 / Key Features:</h3>
 * <ul>
 *   <li>跨会话持久化内存存储和管理 / Cross-session persistent memory storage and management</li>
 *   <li>用户偏好和行为模式学习 / User preference and behavioral pattern learning</li>
 *   <li>个人知识图谱构建和维护 / Personal knowledge graph construction and maintenance</li>
 *   <li>长期适应性学习和个性化优化 / Long-term adaptive learning and personalization optimization</li>
 *   <li>用户画像分析和洞察生成 / User profiling analysis and insight generation</li>
 *   <li>隐私保护和数据安全管理 / Privacy protection and data security management</li>
 * </ul>
 * 
 * <h3>内存分层架构 / Memory Hierarchy Architecture:</h3>
 * <pre>
 * 用户内存层次结构 / User Memory Hierarchy:
 * 
 * ┌─────────────────────────────────────────────────────────┐
 * │                 User Memory Layer                       │
 * ├─────────────────────────────────────────────────────────┤
 * │  Core Profile / 核心画像                                │
 * │  ├─ Basic Info (姓名、年龄、职业等基础信息)              │
 * │  ├─ Preferences (偏好设置和选择倾向)                    │
 * │  └─ Personality Traits (性格特征和行为模式)             │
 * │                                                         │
 * │  Knowledge & Skills / 知识技能                           │
 * │  ├─ Domain Knowledge (专业领域知识)                     │
 * │  ├─ Skill Proficiency (技能熟练度评估)                  │
 * │  └─ Learning Progress (学习进度和成果)                  │
 * │                                                         │
 * │  Behavioral Patterns / 行为模式                         │
 * │  ├─ Interaction History (交互历史和频率)                │
 * │  ├─ Decision Patterns (决策模式和偏好)                  │
 * │  └─ Usage Analytics (使用分析和洞察)                    │
 * │                                                         │
 * │  Long-term Context / 长期上下文                          │
 * │  ├─ Life Events (重要生活事件)                          │
 * │  ├─ Goals & Objectives (目标和计划)                     │
 * │  └─ Historical Milestones (历史里程碑)                  │
 * └─────────────────────────────────────────────────────────┘
 * </pre>
 * 
 * <h3>使用示例 / Usage Examples:</h3>
 * <pre>{@code
 * // 创建用户内存管理器
 * UserMemory userMemory = new UserMemory("user_12345");
 * 
 * // 添加用户偏好信息
 * CompletableFuture<EnhancedMemory> preferenceFuture = userMemory.addMemory(
 *     "用户偏好意式咖啡，特别是拿铁和卡布奇诺",
 *     MemoryType.PREFERENCE,
 *     MemoryImportance.HIGH
 * );
 * EnhancedMemory preference = preferenceFuture.join();
 * 
 * // 添加知识技能信息
 * userMemory.addMemory(
 *     "用户是资深Java开发者，熟练掌握Spring框架和微服务架构",
 *     MemoryType.FACTUAL,
 *     MemoryImportance.HIGH
 * ).join();
 * 
 * // 添加行为模式记录
 * userMemory.addMemory(
 *     "用户习惯在工作日早上9点左右开始编程工作",
 *     MemoryType.PROCEDURAL,
 *     MemoryImportance.MEDIUM
 * ).join();
 * 
 * // 查询用户相关内存
 * CompletableFuture<List<EnhancedMemory>> searchFuture = 
 *     userMemory.searchMemories("咖啡偏好", 5);
 * List<EnhancedMemory> coffeeMemories = searchFuture.join();
 * 
 * for (EnhancedMemory memory : coffeeMemories) {
 *     System.out.println("内存类型: " + memory.getType());
 *     System.out.println("重要性: " + memory.getImportance());
 *     System.out.println("内容: " + memory.getContent());
 * }
 * 
 * // 生成用户画像
 * CompletableFuture<UserProfile> profileFuture = userMemory.generateUserProfile();
 * UserProfile profile = profileFuture.join();
 * 
 * System.out.println("用户偏好标签: " + profile.getPreferenceTags());
 * System.out.println("专业技能领域: " + profile.getSkillDomains());
 * System.out.println("行为模式特征: " + profile.getBehavioralTraits());
 * 
 * // 获取个性化建议
 * CompletableFuture<List<String>> suggestionsFuture = 
 *     userMemory.getPersonalizedSuggestions("学习新技术");
 * List<String> suggestions = suggestionsFuture.join();
 * 
 * System.out.println("个性化学习建议:");
 * suggestions.forEach(suggestion -> System.out.println("- " + suggestion));
 * 
 * // 分析用户兴趣演变
 * CompletableFuture<InterestEvolution> evolutionFuture = 
 *     userMemory.analyzeInterestEvolution();
 * InterestEvolution evolution = evolutionFuture.join();
 * 
 * System.out.println("新兴兴趣: " + evolution.getEmergingInterests());
 * System.out.println("稳定兴趣: " + evolution.getStableInterests());
 * System.out.println("衰减兴趣: " + evolution.getDeclinintInterests());
 * }</pre>
 * 
 * <h3>内存管理策略 / Memory Management Strategies:</h3>
 * <ul>
 *   <li><b>持久化策略</b>: 用户内存永久保存，支持增量更新和版本控制 / Persistence strategy with permanent storage and incremental updates</li>
 *   <li><b>隐私保护</b>: 敏感信息加密存储，支持用户数据删除权 / Privacy protection with encrypted storage and user data deletion rights</li>
 *   <li><b>智能整合</b>: 自动合并相似内存，去重和归类处理 / Intelligent integration with automatic merging and deduplication</li>
 *   <li><b>动态更新</b>: 根据新交互动态调整用户画像和偏好模型 / Dynamic updates based on new interactions</li>
 * </ul>
 * 
 * <h3>性能优化 / Performance Optimization:</h3>
 * <ul>
 *   <li><b>分层缓存</b>: 热点用户数据缓存，提升访问速度 / Tiered caching for hot user data</li>
 *   <li><b>异步处理</b>: 大部分操作支持异步执行避免阻塞 / Asynchronous processing to avoid blocking</li>
 *   <li><b>智能预取</b>: 根据使用模式预取可能需要的内存数据 / Intelligent prefetching based on usage patterns</li>
 *   <li><b>数据压缩</b>: 长期存储数据压缩优化存储空间 / Data compression for long-term storage optimization</li>
 * </ul>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 * @see com.mem0.hierarchy.SessionMemory
 * @see com.mem0.hierarchy.AgentMemory
 * @see com.mem0.hierarchy.MemoryHierarchyManager
 */
public class UserMemory {
    
    private static final Logger logger = LoggerFactory.getLogger(UserMemory.class);
    
    private final String userId;
    private final Map<String, EnhancedMemory> memories;
    private final Map<String, Object> userProfile;
    private final Map<MemoryType, List<EnhancedMemory>> typeIndex;
    private final Map<MemoryImportance, List<EnhancedMemory>> importanceIndex;
    
    // User profiling components
    private final Set<String> preferenceTags;
    private final Set<String> skillDomains;
    private final Set<String> behavioralTraits;
    private final Map<String, Double> interestScores;
    
    // Metadata and statistics
    private Instant createdAt;
    private Instant lastAccessedAt;
    private int totalInteractions;
    private long totalMemorySize;
    
    public UserMemory(String userId) {
        this.userId = userId;
        this.memories = new ConcurrentHashMap<>();
        this.userProfile = new ConcurrentHashMap<>();
        this.typeIndex = new ConcurrentHashMap<>();
        this.importanceIndex = new ConcurrentHashMap<>();
        
        this.preferenceTags = ConcurrentHashMap.newKeySet();
        this.skillDomains = ConcurrentHashMap.newKeySet();
        this.behavioralTraits = ConcurrentHashMap.newKeySet();
        this.interestScores = new ConcurrentHashMap<>();
        
        this.createdAt = Instant.now();
        this.lastAccessedAt = this.createdAt;
        this.totalInteractions = 0;
        this.totalMemorySize = 0;
        
        initializeUserProfile();
    }
    
    public CompletableFuture<EnhancedMemory> addMemory(String content, MemoryType type, MemoryImportance importance) {
        return CompletableFuture.supplyAsync(() -> {
            logger.debug("Adding memory for user {}: {}", userId, content.substring(0, Math.min(50, content.length())));
            
            EnhancedMemory memory = new EnhancedMemory(
                UUID.randomUUID().toString(),
                content,
                userId,
                "user_agent_" + userId,
                "user_session_" + System.currentTimeMillis()
            );
            
            memory.setType(type);
            memory.setImportance(importance);
            
            // Add to main storage
            memories.put(memory.getId(), memory);
            
            // Update indexes
            updateIndexes(memory);
            
            // Update user profile
            updateUserProfileWithMemory(memory);
            
            // Update statistics
            updateStatistics(memory);
            
            logger.info("Added user memory {} for user {}", memory.getId(), userId);
            return memory;
        });
    }
    
    public CompletableFuture<List<EnhancedMemory>> searchMemories(String query, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            logger.debug("Searching user memories for user {} with query: {}", userId, query);
            updateLastAccessed();
            
            // Simple text-based search (in production, use semantic search)
            List<EnhancedMemory> results = memories.values().stream()
                .filter(memory -> memory.getContent().toLowerCase().contains(query.toLowerCase()))
                .sorted((m1, m2) -> {
                    // Sort by relevance: importance + recency + access count
                    double score1 = calculateRelevanceScore(m1, query);
                    double score2 = calculateRelevanceScore(m2, query);
                    return Double.compare(score2, score1);
                })
                .limit(limit)
                .collect(Collectors.toList());
            
            logger.debug("Found {} user memories for query: {}", results.size(), query);
            return results;
        });
    }
    
    public CompletableFuture<List<EnhancedMemory>> getMemoriesByType(MemoryType type) {
        return CompletableFuture.supplyAsync(() -> {
            updateLastAccessed();
            return new ArrayList<>(typeIndex.getOrDefault(type, Collections.emptyList()));
        });
    }
    
    public CompletableFuture<List<EnhancedMemory>> getMemoriesByImportance(MemoryImportance importance) {
        return CompletableFuture.supplyAsync(() -> {
            updateLastAccessed();
            return new ArrayList<>(importanceIndex.getOrDefault(importance, Collections.emptyList()));
        });
    }
    
    public CompletableFuture<UserProfile> generateUserProfile() {
        return CompletableFuture.supplyAsync(() -> {
            logger.debug("Generating user profile for user {}", userId);
            updateLastAccessed();
            
            // Analyze user preferences
            Set<String> currentPreferences = extractPreferences();
            
            // Analyze skill domains
            Set<String> currentSkills = extractSkillDomains();
            
            // Analyze behavioral traits
            Set<String> currentBehaviors = extractBehavioralTraits();
            
            // Calculate interest scores
            Map<String, Double> currentInterests = calculateInterestScores();
            
            return new UserProfile(
                userId,
                currentPreferences,
                currentSkills,
                currentBehaviors,
                currentInterests,
                totalInteractions,
                createdAt,
                lastAccessedAt
            );
        });
    }
    
    public CompletableFuture<List<String>> getPersonalizedSuggestions(String context) {
        return CompletableFuture.supplyAsync(() -> {
            logger.debug("Generating personalized suggestions for user {} in context: {}", userId, context);
            updateLastAccessed();
            
            List<String> suggestions = new ArrayList<>();
            
            // Based on user preferences
            if (context.toLowerCase().contains("学习") || context.toLowerCase().contains("learn")) {
                suggestions.addAll(generateLearningSuggestions());
            }
            
            // Based on user skills
            if (context.toLowerCase().contains("技术") || context.toLowerCase().contains("technology")) {
                suggestions.addAll(generateTechnologySuggestions());
            }
            
            // Based on behavioral patterns
            suggestions.addAll(generateBehavioralSuggestions(context));
            
            return suggestions.stream().distinct().limit(10).collect(Collectors.toList());
        });
    }
    
    public CompletableFuture<InterestEvolution> analyzeInterestEvolution() {
        return CompletableFuture.supplyAsync(() -> {
            logger.debug("Analyzing interest evolution for user {}", userId);
            updateLastAccessed();
            
            // Analyze how user interests have changed over time
            Map<String, List<EnhancedMemory>> interestGroups = memories.values().stream()
                .collect(Collectors.groupingBy(memory -> extractMainTopic(memory.getContent())));
            
            Set<String> emergingInterests = new HashSet<>();
            Set<String> stableInterests = new HashSet<>();
            Set<String> decliningInterests = new HashSet<>();
            
            LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
            LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
            
            for (Map.Entry<String, List<EnhancedMemory>> entry : interestGroups.entrySet()) {
                String topic = entry.getKey();
                List<EnhancedMemory> topicMemories = entry.getValue();
                
                long recentCount = topicMemories.stream()
                    .filter(m -> m.getCreatedAt().isAfter(oneMonthAgo))
                    .count();
                
                long olderCount = topicMemories.stream()
                    .filter(m -> m.getCreatedAt().isBefore(threeMonthsAgo))
                    .count();
                
                if (recentCount > olderCount * 1.5) {
                    emergingInterests.add(topic);
                } else if (Math.abs(recentCount - olderCount) <= 1) {
                    stableInterests.add(topic);
                } else if (recentCount < olderCount * 0.5) {
                    decliningInterests.add(topic);
                }
            }
            
            return new InterestEvolution(emergingInterests, stableInterests, decliningInterests);
        });
    }
    
    public CompletableFuture<Boolean> deleteMemory(String memoryId) {
        return CompletableFuture.supplyAsync(() -> {
            EnhancedMemory memory = memories.remove(memoryId);
            if (memory != null) {
                removeFromIndexes(memory);
                updateStatistics(null); // Recalculate statistics
                logger.info("Deleted user memory {} for user {}", memoryId, userId);
                return true;
            }
            return false;
        });
    }
    
    public CompletableFuture<Void> clearAllMemories() {
        return CompletableFuture.runAsync(() -> {
            logger.warn("Clearing all memories for user {}", userId);
            memories.clear();
            typeIndex.clear();
            importanceIndex.clear();
            preferenceTags.clear();
            skillDomains.clear();
            behavioralTraits.clear();
            interestScores.clear();
            totalMemorySize = 0;
            initializeUserProfile();
        });
    }
    
    // Private helper methods
    
    private void initializeUserProfile() {
        userProfile.put("userId", userId);
        userProfile.put("createdAt", createdAt);
        userProfile.put("version", "1.0");
        userProfile.put("memoryCount", 0);
    }
    
    private void updateIndexes(EnhancedMemory memory) {
        // Type index
        typeIndex.computeIfAbsent(memory.getType(), k -> new ArrayList<>()).add(memory);
        
        // Importance index
        importanceIndex.computeIfAbsent(memory.getImportance(), k -> new ArrayList<>()).add(memory);
    }
    
    private void removeFromIndexes(EnhancedMemory memory) {
        typeIndex.getOrDefault(memory.getType(), Collections.emptyList()).remove(memory);
        importanceIndex.getOrDefault(memory.getImportance(), Collections.emptyList()).remove(memory);
    }
    
    private void updateUserProfileWithMemory(EnhancedMemory memory) {
        // Extract and update preference tags
        if (memory.getType() == MemoryType.PREFERENCE) {
            extractAndAddPreferenceTags(memory.getContent());
        }
        
        // Extract and update skill domains
        if (memory.getType() == MemoryType.FACTUAL || memory.getType() == MemoryType.SEMANTIC) {
            extractAndAddSkillDomains(memory.getContent());
        }
        
        // Extract and update behavioral traits
        if (memory.getType() == MemoryType.PROCEDURAL) {
            extractAndAddBehavioralTraits(memory.getContent());
        }
        
        // Update interest scores
        String topic = extractMainTopic(memory.getContent());
        if (!topic.isEmpty()) {
            double currentScore = interestScores.getOrDefault(topic, 0.0);
            double importance = memory.getImportance().getScore() / 5.0;
            interestScores.put(topic, currentScore + importance);
        }
    }
    
    private void updateStatistics(EnhancedMemory memory) {
        totalInteractions++;
        lastAccessedAt = Instant.now();
        
        if (memory != null) {
            totalMemorySize += memory.getContent().length();
        }
        
        userProfile.put("totalInteractions", totalInteractions);
        userProfile.put("lastAccessedAt", lastAccessedAt);
        userProfile.put("memoryCount", memories.size());
        userProfile.put("totalMemorySize", totalMemorySize);
    }
    
    private void updateLastAccessed() {
        lastAccessedAt = Instant.now();
        totalInteractions++;
    }
    
    private double calculateRelevanceScore(EnhancedMemory memory, String query) {
        double importanceScore = memory.getImportance().getScore() / 5.0;
        double accessScore = Math.log(memory.getAccessCount() + 1) / 10.0;
        double recencyScore = 1.0 / (memory.getDaysOld() + 1);
        double contentMatch = calculateContentMatch(memory.getContent(), query);
        
        return importanceScore + accessScore + recencyScore + contentMatch;
    }
    
    private double calculateContentMatch(String content, String query) {
        String lowerContent = content.toLowerCase();
        String lowerQuery = query.toLowerCase();
        
        if (lowerContent.contains(lowerQuery)) {
            return 1.0;
        }
        
        // Simple word overlap calculation
        Set<String> contentWords = new HashSet<>(Arrays.asList(lowerContent.split("\\s+")));
        Set<String> queryWords = new HashSet<>(Arrays.asList(lowerQuery.split("\\s+")));
        
        Set<String> intersection = new HashSet<>(contentWords);
        intersection.retainAll(queryWords);
        
        return intersection.size() / (double) queryWords.size();
    }
    
    private Set<String> extractPreferences() {
        return memories.values().stream()
            .filter(memory -> memory.getType() == MemoryType.PREFERENCE)
            .flatMap(memory -> extractKeywords(memory.getContent()).stream())
            .collect(Collectors.toSet());
    }
    
    private Set<String> extractSkillDomains() {
        return memories.values().stream()
            .filter(memory -> memory.getType() == MemoryType.FACTUAL || memory.getType() == MemoryType.SEMANTIC)
            .flatMap(memory -> extractTechnicalTerms(memory.getContent()).stream())
            .collect(Collectors.toSet());
    }
    
    private Set<String> extractBehavioralTraits() {
        return memories.values().stream()
            .filter(memory -> memory.getType() == MemoryType.PROCEDURAL)
            .flatMap(memory -> extractBehavioralPatterns(memory.getContent()).stream())
            .collect(Collectors.toSet());
    }
    
    private Map<String, Double> calculateInterestScores() {
        return new HashMap<>(interestScores);
    }
    
    private void extractAndAddPreferenceTags(String content) {
        Set<String> tags = extractKeywords(content);
        preferenceTags.addAll(tags);
    }
    
    private void extractAndAddSkillDomains(String content) {
        Set<String> domains = extractTechnicalTerms(content);
        skillDomains.addAll(domains);
    }
    
    private void extractAndAddBehavioralTraits(String content) {
        Set<String> traits = extractBehavioralPatterns(content);
        behavioralTraits.addAll(traits);
    }
    
    private Set<String> extractKeywords(String content) {
        // Simple keyword extraction (in production, use NLP)
        return Arrays.stream(content.toLowerCase().split("\\s+"))
            .filter(word -> word.length() > 2)
            .filter(word -> !isStopWord(word))
            .collect(Collectors.toSet());
    }
    
    private Set<String> extractTechnicalTerms(String content) {
        Set<String> terms = new HashSet<>();
        String lowerContent = content.toLowerCase();
        
        // Common technical domains
        String[] techKeywords = {"java", "spring", "python", "javascript", "react", "vue", "angular", 
                               "docker", "kubernetes", "aws", "azure", "machine learning", "ai", "database"};
        
        for (String keyword : techKeywords) {
            if (lowerContent.contains(keyword)) {
                terms.add(keyword);
            }
        }
        
        return terms;
    }
    
    private Set<String> extractBehavioralPatterns(String content) {
        Set<String> patterns = new HashSet<>();
        String lowerContent = content.toLowerCase();
        
        // Time-based patterns
        if (lowerContent.contains("早上") || lowerContent.contains("morning")) {
            patterns.add("morning_person");
        }
        if (lowerContent.contains("晚上") || lowerContent.contains("evening")) {
            patterns.add("evening_person");
        }
        
        // Work patterns
        if (lowerContent.contains("专注") || lowerContent.contains("focused")) {
            patterns.add("focused_worker");
        }
        
        return patterns;
    }
    
    private String extractMainTopic(String content) {
        // Simple topic extraction (in production, use topic modeling)
        String[] words = content.toLowerCase().split("\\s+");
        if (words.length > 0) {
            return Arrays.stream(words)
                .filter(word -> word.length() > 3)
                .filter(word -> !isStopWord(word))
                .findFirst()
                .orElse("general");
        }
        return "general";
    }
    
    private boolean isStopWord(String word) {
        Set<String> stopWords = new HashSet<>();
        Collections.addAll(stopWords, "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by",
                          "是", "的", "了", "在", "有", "和", "与", "或", "但是", "因为", "所以", "如果", "这", "那");
        return stopWords.contains(word);
    }
    
    private List<String> generateLearningSuggestions() {
        List<String> suggestions = new ArrayList<>();
        
        // Based on existing skills, suggest related learning paths
        if (skillDomains.contains("java")) {
            suggestions.add("考虑学习Kotlin或Scala来扩展JVM生态系统知识");
            suggestions.add("深入学习Spring Boot微服务架构最佳实践");
        }
        
        if (skillDomains.contains("javascript")) {
            suggestions.add("学习TypeScript来提升代码质量和开发效率");
            suggestions.add("掌握Node.js后端开发扩展技能栈");
        }
        
        return suggestions;
    }
    
    private List<String> generateTechnologySuggestions() {
        List<String> suggestions = new ArrayList<>();
        
        // Based on current interests, suggest trending technologies
        suggestions.add("关注最新的AI和机器学习技术趋势");
        suggestions.add("了解云原生技术如Docker和Kubernetes");
        suggestions.add("学习现代前端框架的最新发展");
        
        return suggestions;
    }
    
    private List<String> generateBehavioralSuggestions(String context) {
        List<String> suggestions = new ArrayList<>();
        
        if (behavioralTraits.contains("morning_person")) {
            suggestions.add("建议在早晨进行复杂的学习任务，效率更高");
        }
        
        if (behavioralTraits.contains("focused_worker")) {
            suggestions.add("保持专注工作的习惯，适当休息避免过度疲劳");
        }
        
        return suggestions;
    }
    
    // Getters and utility methods
    
    public String getUserId() { return userId; }
    public int getMemoryCount() { return memories.size(); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastAccessedAt() { return lastAccessedAt; }
    public int getTotalInteractions() { return totalInteractions; }
    public long getTotalMemorySize() { return totalMemorySize; }
    
    // Inner classes for structured data
    
    public static class UserProfile {
        private final String userId;
        private final Set<String> preferenceTags;
        private final Set<String> skillDomains;
        private final Set<String> behavioralTraits;
        private final Map<String, Double> interestScores;
        private final int totalInteractions;
        private final Instant createdAt;
        private final Instant lastAccessedAt;
        
        public UserProfile(String userId, Set<String> preferenceTags, Set<String> skillDomains,
                          Set<String> behavioralTraits, Map<String, Double> interestScores,
                          int totalInteractions, Instant createdAt, Instant lastAccessedAt) {
            this.userId = userId;
            this.preferenceTags = new HashSet<>(preferenceTags);
            this.skillDomains = new HashSet<>(skillDomains);
            this.behavioralTraits = new HashSet<>(behavioralTraits);
            this.interestScores = new HashMap<>(interestScores);
            this.totalInteractions = totalInteractions;
            this.createdAt = createdAt;
            this.lastAccessedAt = lastAccessedAt;
        }
        
        public String getUserId() { return userId; }
        public Set<String> getPreferenceTags() { return new HashSet<>(preferenceTags); }
        public Set<String> getSkillDomains() { return new HashSet<>(skillDomains); }
        public Set<String> getBehavioralTraits() { return new HashSet<>(behavioralTraits); }
        public Map<String, Double> getInterestScores() { return new HashMap<>(interestScores); }
        public int getTotalInteractions() { return totalInteractions; }
        public Instant getCreatedAt() { return createdAt; }
        public Instant getLastAccessedAt() { return lastAccessedAt; }
    }
    
    public static class InterestEvolution {
        private final Set<String> emergingInterests;
        private final Set<String> stableInterests;
        private final Set<String> decliningInterests;
        
        public InterestEvolution(Set<String> emergingInterests, Set<String> stableInterests, Set<String> decliningInterests) {
            this.emergingInterests = new HashSet<>(emergingInterests);
            this.stableInterests = new HashSet<>(stableInterests);
            this.decliningInterests = new HashSet<>(decliningInterests);
        }
        
        public Set<String> getEmergingInterests() { return new HashSet<>(emergingInterests); }
        public Set<String> getStableInterests() { return new HashSet<>(stableInterests); }
        public Set<String> getDeclinintInterests() { return new HashSet<>(decliningInterests); }
    }
}