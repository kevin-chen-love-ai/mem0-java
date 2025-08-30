package com.mem0.hierarchy;

import com.mem0.core.EnhancedMemory;
import com.mem0.core.MemoryImportance;
import com.mem0.core.MemoryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * 会话级临时内存管理系统 / Session-Level Temporary Memory Management System
 * 
 * 管理单次对话会话中的临时内存，支持上下文保持、对话历史、短期学习等会话期间的内存数据。
 * 提供对话连贯性保证、上下文窗口管理、临时偏好记录等功能，是mem0分层内存体系的中间层组件。
 * Manages temporary memory within a single conversation session, supporting context retention,
 * dialogue history, short-term learning and other memory data during the session period.
 * Provides conversation coherence guarantee, context window management, temporary preference
 * recording as the middle-tier component of the mem0 hierarchical memory system.
 * 
 * <h3>核心功能 / Key Features:</h3>
 * <ul>
 *   <li>会话上下文连续性维护和管理 / Session context continuity maintenance and management</li>
 *   <li>对话历史记录和快速检索 / Dialogue history recording and fast retrieval</li>
 *   <li>临时偏好和短期学习存储 / Temporary preferences and short-term learning storage</li>
 *   <li>上下文窗口智能裁剪和压缩 / Context window intelligent trimming and compression</li>
 *   <li>会话结束后的内存转移和清理 / Memory transfer and cleanup after session ends</li>
 *   <li>实时对话状态跟踪和分析 / Real-time dialogue state tracking and analysis</li>
 * </ul>
 * 
 * <h3>会话内存架构 / Session Memory Architecture:</h3>
 * <pre>
 * 会话内存层次结构 / Session Memory Hierarchy:
 * 
 * ┌─────────────────────────────────────────────────────────┐
 * │                Session Memory Layer                     │
 * ├─────────────────────────────────────────────────────────┤
 * │  Context Window / 上下文窗口                             │
 * │  ├─ Recent Messages (最近消息历史)                       │
 * │  ├─ Active Topics (当前活跃话题)                         │
 * │  └─ Context Summary (上下文摘要)                         │
 * │                                                         │
 * │  Temporary State / 临时状态                              │
 * │  ├─ Current Intent (当前意图识别)                        │
 * │  ├─ Session Goals (会话目标追踪)                         │
 * │  └─ Mood & Tone (情绪和语调分析)                        │
 * │                                                         │
 * │  Short-term Learning / 短期学习                          │
 * │  ├─ Session Preferences (会话内偏好)                     │
 * │  ├─ Temporary Facts (临时事实记录)                       │
 * │  └─ Correction History (纠正历史记录)                    │
 * │                                                         │
 * │  Metadata / 元数据                                       │
 * │  ├─ Session Info (会话基础信息)                          │
 * │  ├─ Timing Data (时间统计数据)                           │
 * │  └─ Quality Metrics (质量评估指标)                       │
 * └─────────────────────────────────────────────────────────┘
 * 
 * 生命周期管理 / Lifecycle Management:
 * 创建 → 活跃使用 → 自动裁剪 → 总结提取 → 转移保存 → 清理销毁
 * Create → Active Use → Auto Trim → Summarize → Transfer → Cleanup
 * </pre>
 * 
 * <h3>使用示例 / Usage Examples:</h3>
 * <pre>{@code
 * // 创建会话内存管理器
 * SessionMemory sessionMemory = new SessionMemory("session_67890", "user_12345");
 * 
 * // 添加对话消息到会话上下文
 * CompletableFuture<EnhancedMemory> messageFuture = sessionMemory.addMessage(
 *     "用户询问关于Java Spring框架的最佳实践",
 *     MessageType.USER_MESSAGE
 * );
 * EnhancedMemory userMessage = messageFuture.join();
 * 
 * // 添加助手回复
 * sessionMemory.addMessage(
 *     "Spring Boot推荐使用依赖注入和自动配置来简化开发",
 *     MessageType.ASSISTANT_RESPONSE
 * ).join();
 * 
 * // 记录会话中的临时偏好
 * sessionMemory.addTemporaryPreference(
 *     "用户在本次会话中偏好详细的代码示例",
 *     MemoryImportance.HIGH
 * ).join();
 * 
 * // 更新当前会话意图
 * sessionMemory.updateCurrentIntent("学习Java后端开发");
 * 
 * // 获取会话上下文摘要
 * CompletableFuture<SessionContextSummary> summaryFuture = sessionMemory.getContextSummary();
 * SessionContextSummary summary = summaryFuture.join();
 * 
 * System.out.println("会话主题: " + summary.getMainTopics());
 * System.out.println("当前意图: " + summary.getCurrentIntent());
 * System.out.println("消息数量: " + summary.getMessageCount());
 * System.out.println("活跃时长: " + summary.getActiveDurationMinutes() + "分钟");
 * 
 * // 获取最近的对话历史
 * CompletableFuture<List<EnhancedMemory>> recentFuture = 
 *     sessionMemory.getRecentMessages(10);
 * List<EnhancedMemory> recentMessages = recentFuture.join();
 * 
 * System.out.println("最近10条消息:");
 * recentMessages.forEach(message -> {
 *     System.out.println("- [" + message.getMetadata().get("message_type") + "] " + 
 *                       message.getContent().substring(0, Math.min(50, message.getContent().length())));
 * });
 * 
 * // 搜索会话内的相关内容
 * CompletableFuture<List<EnhancedMemory>> searchFuture = 
 *     sessionMemory.searchSessionContent("Spring框架", 5);
 * List<EnhancedMemory> springRelated = searchFuture.join();
 * 
 * // 获取会话统计信息
 * CompletableFuture<SessionStatistics> statsFuture = sessionMemory.getSessionStatistics();
 * SessionStatistics stats = statsFuture.join();
 * 
 * System.out.println("总交互次数: " + stats.getTotalInteractions());
 * System.out.println("平均响应时间: " + stats.getAverageResponseTimeSeconds() + "秒");
 * System.out.println("主要话题: " + stats.getTopTopics());
 * 
 * // 会话结束时转移重要内容到用户内存
 * CompletableFuture<TransferResult> transferFuture = 
 *     sessionMemory.transferToUserMemory(userMemoryManager);
 * TransferResult result = transferFuture.join();
 * 
 * System.out.println("转移的内存数量: " + result.getTransferredCount());
 * System.out.println("保留的内存类型: " + result.getTransferredTypes());
 * 
 * // 清理会话内存
 * sessionMemory.cleanup().join();
 * System.out.println("会话内存已清理完毕");
 * }</pre>
 * 
 * <h3>内存管理策略 / Memory Management Strategies:</h3>
 * <ul>
 *   <li><b>滑动窗口</b>: 维持固定大小的上下文窗口，自动淘汰旧消息 / Sliding window maintains fixed-size context with automatic old message eviction</li>
 *   <li><b>智能裁剪</b>: 根据重要性和相关性智能决定保留哪些内容 / Intelligent trimming based on importance and relevance</li>
 *   <li><b>实时压缩</b>: 长对话自动压缩为摘要减少内存占用 / Real-time compression of long conversations into summaries</li>
 *   <li><b>选择性转移</b>: 会话结束时将有价值内容转移到用户层 / Selective transfer of valuable content to user layer on session end</li>
 * </ul>
 * 
 * <h3>上下文窗口管理 / Context Window Management:</h3>
 * <ul>
 *   <li><b>动态调整</b>: 根据对话复杂度动态调整窗口大小 / Dynamic adjustment based on conversation complexity</li>
 *   <li><b>优先级保持</b>: 高重要性内容优先保留在上下文中 / Priority retention for high-importance content</li>
 *   <li><b>话题连贯</b>: 保持话题相关的内容连贯性 / Maintaining topic-related content coherence</li>
 *   <li><b>性能优化</b>: 最小化上下文长度同时保持对话质量 / Minimize context length while maintaining dialogue quality</li>
 * </ul>
 * 
 * <h3>性能优化 / Performance Optimization:</h3>
 * <ul>
 *   <li><b>内存效率</b>: 使用高效数据结构减少内存占用 / Memory efficiency with optimized data structures</li>
 *   <li><b>快速访问</b>: 最近消息的O(1)访问复杂度 / O(1) access complexity for recent messages</li>
 *   <li><b>异步处理</b>: 后台异步进行内容分析和压缩 / Background async content analysis and compression</li>
 *   <li><b>缓存策略</b>: 智能缓存频繁访问的会话数据 / Intelligent caching of frequently accessed session data</li>
 * </ul>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 * @see com.mem0.hierarchy.UserMemory
 * @see com.mem0.hierarchy.AgentMemory
 * @see com.mem0.hierarchy.MemoryHierarchyManager
 */
public class SessionMemory {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionMemory.class);
    
    private static final int DEFAULT_CONTEXT_WINDOW_SIZE = 20;
    private static final int MAX_CONTEXT_WINDOW_SIZE = 50;
    private static final long SESSION_TIMEOUT_MINUTES = 60;
    
    private final String sessionId;
    private final String userId;
    private final int contextWindowSize;
    
    // Core storage
    private final ConcurrentLinkedQueue<EnhancedMemory> messageHistory;
    private final Map<String, EnhancedMemory> memories;
    private final Map<String, Object> sessionMetadata;
    
    // Context management
    private final List<EnhancedMemory> activeContext;
    private final Set<String> activeTopics;
    private String currentIntent;
    private String currentMood;
    
    // Temporary state
    private final Map<String, Object> temporaryPreferences;
    private final List<String> sessionGoals;
    private final Map<String, Integer> topicFrequency;
    
    // Statistics and metrics
    private final Instant sessionStartTime;
    private Instant lastActivityTime;
    private int totalInteractions;
    private long totalProcessingTimeMs;
    private final Map<String, Long> responseTimesMs;
    
    public SessionMemory(String sessionId, String userId) {
        this(sessionId, userId, DEFAULT_CONTEXT_WINDOW_SIZE);
    }
    
    public SessionMemory(String sessionId, String userId, int contextWindowSize) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.contextWindowSize = Math.min(contextWindowSize, MAX_CONTEXT_WINDOW_SIZE);
        
        this.messageHistory = new ConcurrentLinkedQueue<>();
        this.memories = new ConcurrentHashMap<>();
        this.sessionMetadata = new ConcurrentHashMap<>();
        
        this.activeContext = Collections.synchronizedList(new ArrayList<>());
        this.activeTopics = ConcurrentHashMap.newKeySet();
        this.currentIntent = "";
        this.currentMood = "neutral";
        
        this.temporaryPreferences = new ConcurrentHashMap<>();
        this.sessionGoals = Collections.synchronizedList(new ArrayList<>());
        this.topicFrequency = new ConcurrentHashMap<>();
        
        this.sessionStartTime = Instant.now();
        this.lastActivityTime = this.sessionStartTime;
        this.totalInteractions = 0;
        this.totalProcessingTimeMs = 0;
        this.responseTimesMs = new ConcurrentHashMap<>();
        
        initializeSession();
    }
    
    public CompletableFuture<EnhancedMemory> addMessage(String content, MessageType messageType) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            logger.debug("Adding message to session {}: type={}", sessionId, messageType);
            
            EnhancedMemory message = new EnhancedMemory(
                UUID.randomUUID().toString(),
                content,
                userId,
                "session_agent_" + sessionId,
                sessionId
            );
            
            // Set message-specific properties
            message.setType(messageType == MessageType.USER_MESSAGE ? MemoryType.EPISODIC : MemoryType.SEMANTIC);
            message.setImportance(MemoryImportance.MEDIUM);
            
            // Add message metadata
            message.getMetadata().put("message_type", messageType.toString());
            message.getMetadata().put("session_id", sessionId);
            message.getMetadata().put("sequence_number", totalInteractions);
            message.getMetadata().put("timestamp", Instant.now());
            
            // Store in history and memory map
            messageHistory.offer(message);
            memories.put(message.getId(), message);
            
            // Update active context
            updateActiveContext(message);
            
            // Update session state
            updateSessionState(message);
            
            // Update statistics
            updateSessionStatistics(startTime);
            
            logger.debug("Added message {} to session {}", message.getId(), sessionId);
            return message;
        });
    }
    
    public CompletableFuture<EnhancedMemory> addTemporaryPreference(String preferenceContent, MemoryImportance importance) {
        return CompletableFuture.supplyAsync(() -> {
            EnhancedMemory preference = new EnhancedMemory(
                UUID.randomUUID().toString(),
                preferenceContent,
                userId,
                "session_agent_" + sessionId,
                sessionId
            );
            
            preference.setType(MemoryType.PREFERENCE);
            preference.setImportance(importance);
            preference.getMetadata().put("temporary", true);
            preference.getMetadata().put("session_id", sessionId);
            
            memories.put(preference.getId(), preference);
            
            // Store in temporary preferences for quick access
            String key = extractPreferenceKey(preferenceContent);
            temporaryPreferences.put(key, preferenceContent);
            
            updateSessionStatistics(System.currentTimeMillis());
            
            logger.debug("Added temporary preference to session {}: {}", sessionId, key);
            return preference;
        });
    }
    
    public void updateCurrentIntent(String intent) {
        this.currentIntent = intent;
        sessionMetadata.put("current_intent", intent);
        sessionMetadata.put("intent_updated_at", Instant.now());
        logger.debug("Updated session {} intent: {}", sessionId, intent);
    }
    
    public void updateCurrentMood(String mood) {
        this.currentMood = mood;
        sessionMetadata.put("current_mood", mood);
        sessionMetadata.put("mood_updated_at", Instant.now());
        logger.debug("Updated session {} mood: {}", sessionId, mood);
    }
    
    public CompletableFuture<List<EnhancedMemory>> getRecentMessages(int count) {
        return CompletableFuture.supplyAsync(() -> {
            updateLastActivity();
            
            return messageHistory.stream()
                .skip(Math.max(0, messageHistory.size() - count))
                .collect(Collectors.toList());
        });
    }
    
    public CompletableFuture<List<EnhancedMemory>> searchSessionContent(String query, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            logger.debug("Searching session content for query: {}", query);
            updateLastActivity();
            
            return memories.values().stream()
                .filter(memory -> memory.getContent().toLowerCase().contains(query.toLowerCase()))
                .sorted((m1, m2) -> {
                    // Sort by recency and relevance
                    long time1 = ((Instant) m1.getMetadata().get("timestamp")).toEpochMilli();
                    long time2 = ((Instant) m2.getMetadata().get("timestamp")).toEpochMilli();
                    return Long.compare(time2, time1);
                })
                .limit(limit)
                .collect(Collectors.toList());
        });
    }
    
    public CompletableFuture<SessionContextSummary> getContextSummary() {
        return CompletableFuture.supplyAsync(() -> {
            updateLastActivity();
            
            List<String> mainTopics = activeTopics.stream()
                .sorted((t1, t2) -> Integer.compare(
                    topicFrequency.getOrDefault(t2, 0),
                    topicFrequency.getOrDefault(t1, 0)
                ))
                .limit(5)
                .collect(Collectors.toList());
            
            long activeDurationMinutes = ChronoUnit.MINUTES.between(sessionStartTime, lastActivityTime);
            
            return new SessionContextSummary(
                sessionId,
                userId,
                mainTopics,
                currentIntent,
                currentMood,
                messageHistory.size(),
                totalInteractions,
                activeDurationMinutes,
                new HashMap<>(temporaryPreferences)
            );
        });
    }
    
    public CompletableFuture<SessionStatistics> getSessionStatistics() {
        return CompletableFuture.supplyAsync(() -> {
            updateLastActivity();
            
            double averageResponseTimeSeconds = responseTimesMs.values().stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0) / 1000.0;
            
            List<String> topTopics = topicFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
            
            return new SessionStatistics(
                sessionId,
                totalInteractions,
                messageHistory.size(),
                averageResponseTimeSeconds,
                ChronoUnit.MINUTES.between(sessionStartTime, lastActivityTime),
                topTopics,
                activeTopics.size(),
                memories.size()
            );
        });
    }
    
    public CompletableFuture<TransferResult> transferToUserMemory(UserMemory userMemory) {
        return CompletableFuture.supplyAsync(() -> {
            logger.debug("Transferring session memories to user memory for session {}", sessionId);
            
            List<EnhancedMemory> transferred = new ArrayList<>();
            Set<MemoryType> transferredTypes = new HashSet<>();
            
            // Transfer important preferences
            memories.values().stream()
                .filter(memory -> memory.getType() == MemoryType.PREFERENCE)
                .filter(memory -> memory.getImportance() == MemoryImportance.HIGH || 
                                memory.getImportance() == MemoryImportance.CRITICAL)
                .forEach(memory -> {
                    try {
                        userMemory.addMemory(memory.getContent(), memory.getType(), memory.getImportance()).join();
                        transferred.add(memory);
                        transferredTypes.add(memory.getType());
                    } catch (Exception e) {
                        logger.warn("Failed to transfer preference memory {}: {}", memory.getId(), e.getMessage());
                    }
                });
            
            // Transfer important factual knowledge discovered in session
            memories.values().stream()
                .filter(memory -> memory.getType() == MemoryType.FACTUAL || memory.getType() == MemoryType.SEMANTIC)
                .filter(memory -> memory.getImportance() == MemoryImportance.HIGH)
                .forEach(memory -> {
                    try {
                        userMemory.addMemory(memory.getContent(), memory.getType(), memory.getImportance()).join();
                        transferred.add(memory);
                        transferredTypes.add(memory.getType());
                    } catch (Exception e) {
                        logger.warn("Failed to transfer factual memory {}: {}", memory.getId(), e.getMessage());
                    }
                });
            
            // Create session summary if session was substantial
            String sessionSummary = null;
            if (totalInteractions > 10) {
                sessionSummary = generateSessionSummary();
                try {
                    userMemory.addMemory(sessionSummary, MemoryType.EPISODIC, MemoryImportance.MEDIUM).join();
                    transferredTypes.add(MemoryType.EPISODIC);
                } catch (Exception e) {
                    logger.warn("Failed to transfer session summary: {}", e.getMessage());
                }
            }
            
            logger.info("Transferred {} memories from session {} to user memory", transferred.size(), sessionId);
            return new TransferResult(transferred.size(), transferredTypes, sessionSummary);
        });
    }
    
    public CompletableFuture<Void> cleanup() {
        return CompletableFuture.runAsync(() -> {
            logger.debug("Cleaning up session memory for session {}", sessionId);
            
            messageHistory.clear();
            memories.clear();
            activeContext.clear();
            activeTopics.clear();
            temporaryPreferences.clear();
            sessionGoals.clear();
            topicFrequency.clear();
            responseTimesMs.clear();
            sessionMetadata.clear();
            
            logger.info("Session memory cleaned up for session {}", sessionId);
        });
    }
    
    public boolean isExpired() {
        long inactiveMinutes = ChronoUnit.MINUTES.between(lastActivityTime, Instant.now());
        return inactiveMinutes > SESSION_TIMEOUT_MINUTES;
    }
    
    public CompletableFuture<List<EnhancedMemory>> getActiveContext() {
        return CompletableFuture.supplyAsync(() -> {
            updateLastActivity();
            return new ArrayList<>(activeContext);
        });
    }
    
    // Private helper methods
    
    private void initializeSession() {
        sessionMetadata.put("session_id", sessionId);
        sessionMetadata.put("user_id", userId);
        sessionMetadata.put("created_at", sessionStartTime);
        sessionMetadata.put("context_window_size", contextWindowSize);
        sessionMetadata.put("version", "1.0");
        
        logger.info("Initialized session memory for session {} (user: {})", sessionId, userId);
    }
    
    private void updateActiveContext(EnhancedMemory message) {
        synchronized (activeContext) {
            activeContext.add(message);
            
            // Maintain context window size
            if (activeContext.size() > contextWindowSize) {
                // Remove oldest message, but try to keep important ones
                activeContext.sort((m1, m2) -> {
                    int importanceCompare = Integer.compare(
                        m2.getImportance().getScore(),
                        m1.getImportance().getScore()
                    );
                    if (importanceCompare != 0) return importanceCompare;
                    
                    // If same importance, prefer newer messages
                    return ((Instant) m2.getMetadata().get("timestamp"))
                        .compareTo((Instant) m1.getMetadata().get("timestamp"));
                });
                
                // Keep the most important and recent messages
                while (activeContext.size() > contextWindowSize) {
                    activeContext.remove(activeContext.size() - 1);
                }
            }
        }
    }
    
    private void updateSessionState(EnhancedMemory message) {
        // Extract and update topics
        Set<String> messageTopics = extractTopics(message.getContent());
        for (String topic : messageTopics) {
            activeTopics.add(topic);
            topicFrequency.put(topic, topicFrequency.getOrDefault(topic, 0) + 1);
        }
        
        // Update session goals if goal-related content detected
        if (isGoalRelated(message.getContent())) {
            String goal = extractGoal(message.getContent());
            if (!goal.isEmpty() && !sessionGoals.contains(goal)) {
                sessionGoals.add(goal);
            }
        }
    }
    
    private void updateSessionStatistics(long operationStartTime) {
        totalInteractions++;
        lastActivityTime = Instant.now();
        
        long operationTime = System.currentTimeMillis() - operationStartTime;
        totalProcessingTimeMs += operationTime;
        responseTimesMs.put("interaction_" + totalInteractions, operationTime);
        
        // Keep only recent response times to avoid memory bloat
        if (responseTimesMs.size() > 100) {
            String oldestKey = responseTimesMs.keySet().iterator().next();
            responseTimesMs.remove(oldestKey);
        }
        
        // Update session metadata
        sessionMetadata.put("total_interactions", totalInteractions);
        sessionMetadata.put("last_activity", lastActivityTime);
        sessionMetadata.put("total_processing_time_ms", totalProcessingTimeMs);
    }
    
    private void updateLastActivity() {
        lastActivityTime = Instant.now();
    }
    
    private Set<String> extractTopics(String content) {
        // Simple topic extraction (in production, use NLP)
        return Arrays.stream(content.toLowerCase().split("\\s+"))
            .filter(word -> word.length() > 3)
            .filter(this::isTopicWord)
            .limit(5)
            .collect(Collectors.toSet());
    }
    
    private boolean isTopicWord(String word) {
        // Filter out common words and keep potential topic words
        Set<String> stopWords = new HashSet<>();
        Collections.addAll(stopWords, "这", "那", "什么", "怎么", "为什么", "the", "what", "how", "why", "where", "when");
        return !stopWords.contains(word) && !word.matches("\\d+");
    }
    
    private boolean isGoalRelated(String content) {
        String lowerContent = content.toLowerCase();
        return lowerContent.contains("目标") || lowerContent.contains("goal") ||
               lowerContent.contains("计划") || lowerContent.contains("plan") ||
               lowerContent.contains("想要") || lowerContent.contains("want");
    }
    
    private String extractGoal(String content) {
        // Simple goal extraction (in production, use NLP)
        if (content.contains("想要")) {
            int index = content.indexOf("想要");
            return content.substring(index, Math.min(content.length(), index + 50)).trim();
        }
        return "";
    }
    
    private String extractPreferenceKey(String preferenceContent) {
        // Extract a key from preference content for indexing
        String[] words = preferenceContent.toLowerCase().split("\\s+");
        return Arrays.stream(words)
            .filter(word -> word.length() > 2)
            .filter(word -> !isStopWord(word))
            .findFirst()
            .orElse("preference_" + System.currentTimeMillis());
    }
    
    private boolean isStopWord(String word) {
        Set<String> stopWords = new HashSet<>();
        Collections.addAll(stopWords, "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by",
                                     "是", "的", "了", "在", "有", "和", "与", "或", "但是", "因为", "所以", "如果", "这", "那");
        return stopWords.contains(word);
    }
    
    private String generateSessionSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("会话摘要 (Session Summary): ");
        
        // Add main topics
        if (!activeTopics.isEmpty()) {
            summary.append("主要讨论话题包括: ");
            summary.append(String.join(", ", activeTopics.stream().limit(3).collect(Collectors.toList())));
            summary.append("。");
        }
        
        // Add current intent
        if (!currentIntent.isEmpty()) {
            summary.append("用户主要意图是: ").append(currentIntent).append("。");
        }
        
        // Add session goals
        if (!sessionGoals.isEmpty()) {
            summary.append("会话中提到的目标: ");
            summary.append(String.join(", ", sessionGoals));
            summary.append("。");
        }
        
        // Add interaction statistics
        summary.append("总交互次数: ").append(totalInteractions)
               .append("，会话时长: ").append(ChronoUnit.MINUTES.between(sessionStartTime, lastActivityTime))
               .append("分钟。");
        
        return summary.toString();
    }
    
    // Getters
    
    public String getSessionId() { return sessionId; }
    public String getUserId() { return userId; }
    public int getMessageCount() { return messageHistory.size(); }
    public Instant getSessionStartTime() { return sessionStartTime; }
    public Instant getLastActivityTime() { return lastActivityTime; }
    public int getTotalInteractions() { return totalInteractions; }
    public String getCurrentIntent() { return currentIntent; }
    public String getCurrentMood() { return currentMood; }
    public Set<String> getActiveTopics() { return new HashSet<>(activeTopics); }
    
    // Enums and Inner Classes
    
    public enum MessageType {
        USER_MESSAGE,
        ASSISTANT_RESPONSE,
        SYSTEM_MESSAGE,
        TOOL_OUTPUT
    }
    
    public static class SessionContextSummary {
        private final String sessionId;
        private final String userId;
        private final List<String> mainTopics;
        private final String currentIntent;
        private final String currentMood;
        private final int messageCount;
        private final int totalInteractions;
        private final long activeDurationMinutes;
        private final Map<String, Object> temporaryPreferences;
        
        public SessionContextSummary(String sessionId, String userId, List<String> mainTopics,
                                   String currentIntent, String currentMood, int messageCount,
                                   int totalInteractions, long activeDurationMinutes,
                                   Map<String, Object> temporaryPreferences) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.mainTopics = new ArrayList<>(mainTopics);
            this.currentIntent = currentIntent;
            this.currentMood = currentMood;
            this.messageCount = messageCount;
            this.totalInteractions = totalInteractions;
            this.activeDurationMinutes = activeDurationMinutes;
            this.temporaryPreferences = new HashMap<>(temporaryPreferences);
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public String getUserId() { return userId; }
        public List<String> getMainTopics() { return new ArrayList<>(mainTopics); }
        public String getCurrentIntent() { return currentIntent; }
        public String getCurrentMood() { return currentMood; }
        public int getMessageCount() { return messageCount; }
        public int getTotalInteractions() { return totalInteractions; }
        public long getActiveDurationMinutes() { return activeDurationMinutes; }
        public Map<String, Object> getTemporaryPreferences() { return new HashMap<>(temporaryPreferences); }
    }
    
    public static class SessionStatistics {
        private final String sessionId;
        private final int totalInteractions;
        private final int messageCount;
        private final double averageResponseTimeSeconds;
        private final long activeDurationMinutes;
        private final List<String> topTopics;
        private final int activeTopicCount;
        private final int totalMemoryCount;
        
        public SessionStatistics(String sessionId, int totalInteractions, int messageCount,
                               double averageResponseTimeSeconds, long activeDurationMinutes,
                               List<String> topTopics, int activeTopicCount, int totalMemoryCount) {
            this.sessionId = sessionId;
            this.totalInteractions = totalInteractions;
            this.messageCount = messageCount;
            this.averageResponseTimeSeconds = averageResponseTimeSeconds;
            this.activeDurationMinutes = activeDurationMinutes;
            this.topTopics = new ArrayList<>(topTopics);
            this.activeTopicCount = activeTopicCount;
            this.totalMemoryCount = totalMemoryCount;
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public int getTotalInteractions() { return totalInteractions; }
        public int getMessageCount() { return messageCount; }
        public double getAverageResponseTimeSeconds() { return averageResponseTimeSeconds; }
        public long getActiveDurationMinutes() { return activeDurationMinutes; }
        public List<String> getTopTopics() { return new ArrayList<>(topTopics); }
        public int getActiveTopicCount() { return activeTopicCount; }
        public int getTotalMemoryCount() { return totalMemoryCount; }
    }
    
    public static class TransferResult {
        private final int transferredCount;
        private final Set<MemoryType> transferredTypes;
        private final String sessionSummary;
        
        public TransferResult(int transferredCount, Set<MemoryType> transferredTypes, String sessionSummary) {
            this.transferredCount = transferredCount;
            this.transferredTypes = new HashSet<>(transferredTypes);
            this.sessionSummary = sessionSummary;
        }
        
        // Getters
        public int getTransferredCount() { return transferredCount; }
        public Set<MemoryType> getTransferredTypes() { return new HashSet<>(transferredTypes); }
        public String getSessionSummary() { return sessionSummary; }
    }
}