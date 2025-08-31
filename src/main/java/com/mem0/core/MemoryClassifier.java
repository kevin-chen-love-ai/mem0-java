package com.mem0.core;

import com.mem0.llm.LLMProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * 记忆分类器，用于对记忆内容进行智能分类和分析
 * Memory classifier for intelligent classification and analysis of memory content
 * 
 * <p>该类提供两种分类方式 / This class provides two classification approaches:</p>
 * <ul>
 *   <li>基于规则的分类：使用预定义的模式和关键词 / Rule-based classification using predefined patterns and keywords</li>
 *   <li>基于LLM的分类：使用大语言模型进行更准确的分类 / LLM-based classification using large language models for more accurate classification</li>
 * </ul>
 * 
 * <p>支持的分类类型 / Supported classification types:</p>
 * <ul>
 *   <li>{@link MemoryType#SEMANTIC} - 语义记忆 / Semantic memory</li>
 *   <li>{@link MemoryType#EPISODIC} - 情景记忆 / Episodic memory</li>
 *   <li>{@link MemoryType#PROCEDURAL} - 程序记忆 / Procedural memory</li>
 *   <li>{@link MemoryType#FACTUAL} - 事实记忆 / Factual memory</li>
 *   <li>{@link MemoryType#PREFERENCE} - 偏好记忆 / Preference memory</li>
 *   <li>{@link MemoryType#TEMPORAL} - 时间记忆 / Temporal memory</li>
 *   <li>{@link MemoryType#RELATIONSHIP} - 关系记忆 / Relationship memory</li>
 * </ul>
 * 
 * <p>使用示例 / Usage example:</p>
 * <pre>{@code
 * MemoryClassifier classifier = new MemoryClassifier(llmProvider);
 * 
 * // 分类记忆类型
 * MemoryType type = classifier.classifyMemory("我喜欢吃苹果", context).join();
 * 
 * // 评估重要性
 * MemoryImportance importance = classifier.assessImportance("明天的会议", type, context).join();
 * 
 * // 提取实体
 * Set<String> entities = classifier.extractEntities("我和张三在北京开会").join();
 * }</pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class MemoryClassifier {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryClassifier.class);
    
    /** LLM提供者，用于智能分类 / LLM provider for intelligent classification */
    private final LLMProvider llmProvider;
    
    /** 是否使用LLM进行分类 / Whether to use LLM for classification */
    private final boolean useLLMClassification;
    
    /** 基于模式的分类规则 / Pattern-based classification rules */
    private static final Map<MemoryType, List<Pattern>> CLASSIFICATION_PATTERNS = new HashMap<>();
    
    /** 程序性记忆关键词 / Procedural memory keywords */
    private static final Set<String> PROCEDURAL_KEYWORDS = new HashSet<>(Arrays.asList(
        "how to", "step", "process", "method", "technique", "skill", "procedure", 
        "algorithm", "recipe", "instructions", "tutorial", "guide"
    ));
    
    /** 偏好记忆关键词 / Preference memory keywords */
    private static final Set<String> PREFERENCE_KEYWORDS = new HashSet<>(Arrays.asList(
        "like", "prefer", "favorite", "enjoy", "love", "hate", "dislike", 
        "usually", "always", "never", "typically", "tend to"
    ));
    
    /** 时间性记忆关键词 / Temporal memory keywords */
    private static final Set<String> TEMPORAL_KEYWORDS = new HashSet<>(Arrays.asList(
        "schedule", "appointment", "meeting", "deadline", "reminder", "calendar",
        "today", "tomorrow", "yesterday", "next week", "last month"
    ));
    
    /** 关系性记忆关键词 / Relationship memory keywords */
    private static final Set<String> RELATIONSHIP_KEYWORDS = new HashSet<>(Arrays.asList(
        "friend", "colleague", "family", "partner", "manager", "team", "knows",
        "works with", "married to", "related to", "connected to"
    ));
    
    static {
        // Initialize classification patterns
        CLASSIFICATION_PATTERNS.put(MemoryType.PROCEDURAL, Arrays.asList(
            Pattern.compile(".*\\b(?:how to|step \\d+|first|second|third|then|next|finally)\\b.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\b(?:process|method|technique|procedure|algorithm)\\b.*", Pattern.CASE_INSENSITIVE)
        ));
        
        CLASSIFICATION_PATTERNS.put(MemoryType.PREFERENCE, Arrays.asList(
            Pattern.compile(".*\\b(?:like|prefer|favorite|enjoy|love|hate|dislike)\\b.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\b(?:usually|always|never|typically|tend to)\\b.*", Pattern.CASE_INSENSITIVE)
        ));
        
        CLASSIFICATION_PATTERNS.put(MemoryType.TEMPORAL, Arrays.asList(
            Pattern.compile(".*\\b(?:schedule|appointment|meeting|deadline|reminder)\\b.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\b(?:today|tomorrow|next \\w+)\\b.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\b\\d{4}-\\d{2}-\\d{2}\\b.*"), // Date pattern
            Pattern.compile(".*\\b\\d{1,2}:\\d{2}\\s*(?:AM|PM)?\\b.*", Pattern.CASE_INSENSITIVE) // Time pattern
        ));
        
        CLASSIFICATION_PATTERNS.put(MemoryType.RELATIONSHIP, Arrays.asList(
            Pattern.compile(".*\\b(?:friend|colleague|family|partner|manager|team)\\b.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\b(?:knows|works with|married to|related to|connected to)\\b.*", Pattern.CASE_INSENSITIVE)
        ));
    }
    
    /**
     * 创建记忆分类器
     * Creates a memory classifier
     * 
     * <p>如果提供LLM提供者，将优先使用LLM进行分类；否则使用基于规则的分类。 / 
     * If LLM provider is provided, LLM-based classification will be used preferentially; otherwise rule-based classification will be used.</p>
     * 
     * @param llmProvider LLM提供者，可以为null（将使用规则分类） / LLM provider, can be null (will use rule-based classification)
     */
    public MemoryClassifier(LLMProvider llmProvider) {
        this.llmProvider = llmProvider;
        this.useLLMClassification = llmProvider != null;
    }
    
    /**
     * 对记忆内容进行分类
     * Classifies memory content into appropriate memory type
     * 
     * <p>根据记忆内容和上下文信息，自动分类为不同的记忆类型。 / 
     * Automatically classifies into different memory types based on memory content and context information.</p>
     * 
     * @param content 记忆内容，不能为null或空 / Memory content, cannot be null or empty
     * @param context 上下文信息，可以为null / Context information, can be null
     * @return CompletableFuture<MemoryType>，记忆类型 / CompletableFuture<MemoryType>, memory type
     * 
     * @throws IllegalArgumentException 如枟content为null或空 / if content is null or empty
     */
    public CompletableFuture<MemoryType> classifyMemory(String content, Map<String, Object> context) {
        // Handle null or empty content
        if (content == null || content.trim().isEmpty()) {
            return CompletableFuture.completedFuture(MemoryType.SEMANTIC);
        }
        
        if (useLLMClassification && llmProvider != null) {
            return classifyWithLLM(content, context);
        } else {
            return CompletableFuture.completedFuture(classifyWithRules(content, context));
        }
    }
    
    /**
     * 评估记忆的重要性
     * Assesses the importance of a memory
     * 
     * <p>基于记忆内容、类型和上下文信息，评估记忆的重要性级别。 / 
     * Assesses memory importance level based on content, type, and context information.</p>
     * 
     * @param content 记忆内容 / Memory content
     * @param type 记忆类型 / Memory type
     * @param context 上下文信息 / Context information
     * @return CompletableFuture<MemoryImportance>，记忆重要性 / CompletableFuture<MemoryImportance>, memory importance
     * 
     * @throws IllegalArgumentException 如枟content或type为null / if content or type is null
     */
    public CompletableFuture<MemoryImportance> assessImportance(String content, MemoryType type, 
                                                              Map<String, Object> context) {
        if (useLLMClassification && llmProvider != null) {
            return assessImportanceWithLLM(content, type, context);
        } else {
            return CompletableFuture.completedFuture(assessImportanceWithRules(content, type, context));
        }
    }
    
    /**
     * 从记忆内容中提取实体
     * Extracts entities from memory content
     * 
     * <p>识别和提取记忆中的重要实体，如人名、地名、组织、日期等。 / 
     * Identifies and extracts important entities from memory, such as person names, places, organizations, dates, etc.</p>
     * 
     * @param content 记忆内容 / Memory content
     * @return CompletableFuture<Set<String>>，提取的实体集合 / CompletableFuture<Set<String>>, set of extracted entities
     * 
     * @throws IllegalArgumentException 如枟content为null或空 / if content is null or empty
     */
    public CompletableFuture<Set<String>> extractEntities(String content) {
        if (useLLMClassification && llmProvider != null) {
            return extractEntitiesWithLLM(content);
        } else {
            return CompletableFuture.completedFuture(extractEntitiesWithRules(content));
        }
    }
    
    /**
     * 为记忆生成标签
     * Generates tags for memory
     * 
     * <p>基于记忆内容和类型自动生成相关标签，用于分类和检索。 / 
     * Automatically generates relevant tags based on memory content and type for categorization and search.</p>
     * 
     * @param content 记忆内容 / Memory content
     * @param type 记忆类型 / Memory type
     * @return CompletableFuture<Set<String>>，生成的标签集合 / CompletableFuture<Set<String>>, set of generated tags
     * 
     * @throws IllegalArgumentException 如枟content或type为null / if content or type is null
     */
    public CompletableFuture<Set<String>> generateTags(String content, MemoryType type) {
        if (useLLMClassification && llmProvider != null) {
            return generateTagsWithLLM(content, type);
        } else {
            return CompletableFuture.completedFuture(generateTagsWithRules(content, type));
        }
    }
    
    /**
     * 使用规则进行记忆分类
     * Classifies memory using rule-based approach
     * 
     * <p>使用预定义的模式和关键词进行分类，不依赖外部LLM服务。 / 
     * Uses predefined patterns and keywords for classification, without relying on external LLM services.</p>
     * 
     * @param content 记忆内容 / Memory content
     * @param context 上下文信息 / Context information
     * @return 分类结果 / Classification result
     */
    private MemoryType classifyWithRules(String content, Map<String, Object> context) {
        // Handle null or empty content
        if (content == null || content.trim().isEmpty()) {
            return MemoryType.SEMANTIC;
        }
        
        String lowerContent = content.toLowerCase();
        
        // Check patterns for different types first (more specific patterns)
        for (Map.Entry<MemoryType, List<Pattern>> entry : CLASSIFICATION_PATTERNS.entrySet()) {
            for (Pattern pattern : entry.getValue()) {
                if (pattern.matcher(content).matches()) {
                    return entry.getKey();
                }
            }
        }
        
        // Check for episodic patterns (personal experiences, past events) - before temporal
        if (containsEpisodicInfo(content)) {
            return MemoryType.EPISODIC;
        }
        
        // Check for factual patterns (numbers, specific facts) - before procedural
        if (containsFactualInfo(content)) {
            return MemoryType.FACTUAL;
        }
        
        // Check keywords
        if (containsKeywords(lowerContent, PROCEDURAL_KEYWORDS)) {
            return MemoryType.PROCEDURAL;
        }
        
        if (containsKeywords(lowerContent, PREFERENCE_KEYWORDS)) {
            return MemoryType.PREFERENCE;
        }
        
        if (containsKeywords(lowerContent, RELATIONSHIP_KEYWORDS)) {
            return MemoryType.RELATIONSHIP;
        }
        
        // Check for explicit temporal information (schedules, appointments) - after episodic
        if (containsTemporalInfo(content) || hasTemporalContext(context)) {
            return MemoryType.TEMPORAL;
        }
        
        // Default to semantic
        return MemoryType.SEMANTIC;
    }
    
    /**
     * 使用规则评估记忆重要性
     * Assesses memory importance using rule-based approach
     * 
     * <p>基于记忆类型、内容特征和上下文信息评估重要性。 / 
     * Assesses importance based on memory type, content features, and context information.</p>
     * 
     * @param content 记忆内容 / Memory content
     * @param type 记忆类型 / Memory type
     * @param context 上下文信息 / Context information
     * @return 重要性级别 / Importance level
     */
    private MemoryImportance assessImportanceWithRules(String content, MemoryType type, 
                                                     Map<String, Object> context) {
        int score = 3; // Start with medium importance
        
        // Type-based importance
        switch (type) {
            case PROCEDURAL:
                score += 1; // Procedures are generally important
                break;
            case PREFERENCE:
                score += 1; // User preferences are important for personalization
                break;
            case TEMPORAL:
                score += 2; // Temporal information is often critical
                break;
            case EPISODIC:
                score += 1; // Personal experiences are valuable
                break;
        }
        
        // Content-based importance indicators
        String lowerContent = content.toLowerCase();
        
        // High importance keywords
        if (containsAnyKeyword(lowerContent, Arrays.asList("important", "critical", "urgent", "must", "essential"))) {
            score += 2;
        }
        
        // Specific facts and numbers often indicate importance
        if (containsSpecificInfo(content)) {
            score += 1;
        }
        
        // Context-based importance
        if (context != null) {
            if (context.containsKey("priority") && "high".equals(context.get("priority"))) {
                score += 2;
            }
            if (context.containsKey("source") && "system".equals(context.get("source"))) {
                score += 1;
            }
        }
        
        // Clamp score between 1 and 5
        score = Math.max(1, Math.min(5, score));
        
        return MemoryImportance.fromScore(score);
    }
    
    /**
     * 使用规则提取实体
     * Extracts entities using rule-based approach
     * 
     * <p>使用正则表达式和模式匹配提取实体，包括专有名词、邮箱、电话、日期等。 / 
     * Uses regular expressions and pattern matching to extract entities including proper nouns, emails, phone numbers, dates, etc.</p>
     * 
     * @param content 记忆内容 / Memory content
     * @return 提取的实体集合 / Set of extracted entities
     */
    private Set<String> extractEntitiesWithRules(String content) {
        Set<String> entities = new HashSet<>();
        
        // Simple named entity extraction using patterns
        
        // Proper nouns (capitalized words)
        Pattern properNounPattern = Pattern.compile("\\b[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*\\b");
        java.util.regex.Matcher matcher = properNounPattern.matcher(content);
        while (matcher.find()) {
            String entity = matcher.group();
            if (entity.length() > 2 && !isCommonWord(entity)) {
                entities.add(entity);
            }
        }
        
        // Email addresses
        Pattern emailPattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
        matcher = emailPattern.matcher(content);
        while (matcher.find()) {
            entities.add(matcher.group());
        }
        
        // Phone numbers
        Pattern phonePattern = Pattern.compile("\\b\\d{3}-\\d{3}-\\d{4}\\b|\\b\\(\\d{3}\\)\\s*\\d{3}-\\d{4}\\b");
        matcher = phonePattern.matcher(content);
        while (matcher.find()) {
            entities.add(matcher.group());
        }
        
        // Dates
        Pattern datePattern = Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}\\b|\\b\\d{1,2}/\\d{1,2}/\\d{4}\\b");
        matcher = datePattern.matcher(content);
        while (matcher.find()) {
            entities.add(matcher.group());
        }
        
        return entities;
    }
    
    /**
     * 使用规则生成标签
     * Generates tags using rule-based approach
     * 
     * <p>基于记忆类型和内容关键词生成相关标签。 / 
     * Generates relevant tags based on memory type and content keywords.</p>
     * 
     * @param content 记忆内容 / Memory content
     * @param type 记忆类型 / Memory type
     * @return 生成的标签集合 / Set of generated tags
     */
    private Set<String> generateTagsWithRules(String content, MemoryType type) {
        Set<String> tags = new HashSet<>();
        
        // Add type-based tag
        tags.add(type.getValue());
        
        String lowerContent = content.toLowerCase();
        
        // Content-based tags
        if (containsKeywords(lowerContent, PREFERENCE_KEYWORDS)) {
            tags.add("preference");
        }
        
        if (containsKeywords(lowerContent, TEMPORAL_KEYWORDS)) {
            tags.add("temporal");
        }
        
        if (containsKeywords(lowerContent, RELATIONSHIP_KEYWORDS)) {
            tags.add("social");
        }
        
        // Context tags
        if (lowerContent.contains("work") || lowerContent.contains("job") || lowerContent.contains("office")) {
            tags.add("work");
        }
        
        if (lowerContent.contains("personal") || lowerContent.contains("family") || lowerContent.contains("home")) {
            tags.add("personal");
        }
        
        if (lowerContent.contains("skill") || lowerContent.contains("learn") || lowerContent.contains("knowledge")) {
            tags.add("learning");
        }
        
        return tags;
    }
    
    /**
     * 使用LLM进行记忆分类
     * Classifies memory using LLM-based approach
     * 
     * <p>使用大语言模型进行更准确的记忆分类，如果失败则回退到规则分类。 / 
     * Uses large language model for more accurate memory classification, falls back to rule-based classification if failed.</p>
     * 
     * @param content 记忆内容 / Memory content
     * @param context 上下文信息 / Context information
     * @return CompletableFuture<MemoryType>，分类结果 / CompletableFuture<MemoryType>, classification result
     */
    private CompletableFuture<MemoryType> classifyWithLLM(String content, Map<String, Object> context) {
        String prompt = buildClassificationPrompt(content, context);
        
        LLMProvider.LLMConfig config = new LLMProvider.LLMConfig();
        config.setMaxTokens(50);
        config.setTemperature(0.1); // Low temperature for consistent classification
        
        List<LLMProvider.ChatMessage> messages = Arrays.asList(
            new LLMProvider.ChatMessage(LLMProvider.ChatMessage.Role.SYSTEM, 
                "You are a memory classification system. Classify the given content into one of these types: " +
                "semantic, episodic, procedural, factual, contextual, preference, relationship, temporal. " +
                "Return only the type name."),
            new LLMProvider.ChatMessage(LLMProvider.ChatMessage.Role.USER, prompt)
        );
        
        return llmProvider.generateChatCompletion(messages, config)
            .thenApply(response -> {
                if (response == null || response.getContent() == null) {
                    logger.warn("LLM response is null, falling back to rules");
                    return classifyWithRules(content, context);
                }
                String responseContent = response.getContent().toLowerCase().trim();
                
                // Try to extract memory type from response
                MemoryType type = null;
                
                // First try exact match
                type = MemoryType.fromValue(responseContent);
                
                // If exact match returns default (SEMANTIC), try to find type in the response
                if (type == MemoryType.SEMANTIC && !responseContent.equals("semantic")) {
                    logger.debug("Exact match returned SEMANTIC, checking for type names in response: '{}'", responseContent);
                    for (MemoryType memoryType : MemoryType.values()) {
                        String nameLower = memoryType.name().toLowerCase();
                        String valueLower = memoryType.getValue().toLowerCase();
                        logger.debug("Checking for '{}' (name: '{}', value: '{}') in response: '{}'", 
                                   memoryType, nameLower, valueLower, responseContent);
                        if (responseContent.contains(nameLower) || responseContent.contains(valueLower)) {
                            type = memoryType;
                            logger.debug("Found match for type: {}", memoryType);
                            break;
                        }
                    }
                }
                
                if (type == null) {
                    logger.warn("Could not parse LLM response: {}, falling back to rules", responseContent);
                    return classifyWithRules(content, context);
                }
                
                logger.debug("Successfully parsed LLM response '{}' to type: {}", responseContent, type);
                
                logger.debug("LLM classified '{}' as {}", 
                    content.substring(0, Math.min(50, content.length())), type);
                return type;
            })
            .exceptionally(throwable -> {
                logger.warn("LLM classification failed, falling back to rules: {}", throwable.getMessage());
                return classifyWithRules(content, context);
            });
    }
    
    /**
     * 使用LLM评估记忆重要性
     * Assesses memory importance using LLM-based approach
     * 
     * <p>使用大语言模型进行更准确的重要性评估，如果失败则回退到规则评估。 / 
     * Uses large language model for more accurate importance assessment, falls back to rule-based assessment if failed.</p>
     * 
     * @param content 记忆内容 / Memory content
     * @param type 记忆类型 / Memory type
     * @param context 上下文信息 / Context information
     * @return CompletableFuture<MemoryImportance>，重要性级别 / CompletableFuture<MemoryImportance>, importance level
     */
    private CompletableFuture<MemoryImportance> assessImportanceWithLLM(String content, MemoryType type,
                                                                       Map<String, Object> context) {
        String prompt = buildImportancePrompt(content, type, context);
        
        LLMProvider.LLMConfig config = new LLMProvider.LLMConfig();
        config.setMaxTokens(30);
        config.setTemperature(0.1);
        
        List<LLMProvider.ChatMessage> messages = Arrays.asList(
            new LLMProvider.ChatMessage(LLMProvider.ChatMessage.Role.SYSTEM,
                "You are a memory importance assessment system. Rate the importance of the given memory " +
                "on a scale of 1-5 where 1=minimal, 2=low, 3=medium, 4=high, 5=critical. " +
                "Return only the number."),
            new LLMProvider.ChatMessage(LLMProvider.ChatMessage.Role.USER, prompt)
        );
        
        return llmProvider.generateChatCompletion(messages, config)
            .thenApply(response -> {
                if (response == null || response.getContent() == null) {
                    logger.warn("LLM response is null, falling back to rules");
                    return assessImportanceWithRules(content, type, context);
                }
                try {
                    int score = Integer.parseInt(response.getContent().trim());
                    return MemoryImportance.fromScore(score);
                } catch (NumberFormatException e) {
                    logger.warn("Failed to parse importance score: {}", response.getContent());
                    return assessImportanceWithRules(content, type, context);
                }
            })
            .exceptionally(throwable -> {
                logger.warn("LLM importance assessment failed, falling back to rules: {}", throwable.getMessage());
                return assessImportanceWithRules(content, type, context);
            });
    }
    
    /**
     * 使用LLM提取实体
     * Extracts entities using LLM-based approach
     * 
     * <p>使用大语言模型进行更准确的实体提取，如果失败则回退到规则提取。 / 
     * Uses large language model for more accurate entity extraction, falls back to rule-based extraction if failed.</p>
     * 
     * @param content 记忆内容 / Memory content
     * @return CompletableFuture<Set<String>>，提取的实体集合 / CompletableFuture<Set<String>>, set of extracted entities
     */
    private CompletableFuture<Set<String>> extractEntitiesWithLLM(String content) {
        String prompt = "Extract important entities (names, places, organizations, dates, etc.) from: " + content;
        
        LLMProvider.LLMConfig config = new LLMProvider.LLMConfig();
        config.setMaxTokens(100);
        config.setTemperature(0.1);
        
        List<LLMProvider.ChatMessage> messages = Arrays.asList(
            new LLMProvider.ChatMessage(LLMProvider.ChatMessage.Role.SYSTEM,
                "Extract important entities from the given text. Return them as a comma-separated list. " +
                "Focus on proper nouns, names, places, organizations, dates, and other specific entities."),
            new LLMProvider.ChatMessage(LLMProvider.ChatMessage.Role.USER, prompt)
        );
        
        return llmProvider.generateChatCompletion(messages, config)
            .thenApply(response -> {
                if (response == null || response.getContent() == null) {
                    logger.warn("LLM response is null, falling back to rules");
                    return extractEntitiesWithRules(content);
                }
                Set<String> entities = new HashSet<>();
                String[] parts = response.getContent().split(",");
                for (String part : parts) {
                    String entity = part.trim();
                    if (!entity.isEmpty() && entity.length() > 1) {
                        entities.add(entity);
                    }
                }
                return entities;
            })
            .exceptionally(throwable -> {
                logger.warn("LLM entity extraction failed, falling back to rules: {}", throwable.getMessage());
                return extractEntitiesWithRules(content);
            });
    }
    
    /**
     * 使用LLM生成标签
     * Generates tags using LLM-based approach
     * 
     * <p>使用大语言模型进行更准确的标签生成，如果失败则回退到规则生成。 / 
     * Uses large language model for more accurate tag generation, falls back to rule-based generation if failed.</p>
     * 
     * @param content 记忆内容 / Memory content
     * @param type 记忆类型 / Memory type
     * @return CompletableFuture<Set<String>>，生成的标签集合 / CompletableFuture<Set<String>>, set of generated tags
     */
    private CompletableFuture<Set<String>> generateTagsWithLLM(String content, MemoryType type) {
        String prompt = String.format("Generate relevant tags for this %s memory: %s", type.getValue(), content);
        
        LLMProvider.LLMConfig config = new LLMProvider.LLMConfig();
        config.setMaxTokens(50);
        config.setTemperature(0.2);
        
        List<LLMProvider.ChatMessage> messages = Arrays.asList(
            new LLMProvider.ChatMessage(LLMProvider.ChatMessage.Role.SYSTEM,
                "Generate 3-5 relevant tags for the given memory content. " +
                "Return them as a comma-separated list. Keep tags short and descriptive."),
            new LLMProvider.ChatMessage(LLMProvider.ChatMessage.Role.USER, prompt)
        );
        
        return llmProvider.generateChatCompletion(messages, config)
            .thenApply(response -> {
                Set<String> tags = new HashSet<>();
                String[] parts = response.getContent().split(",");
                for (String part : parts) {
                    String tag = part.trim().toLowerCase();
                    if (!tag.isEmpty() && tag.length() > 1) {
                        tags.add(tag);
                    }
                }
                return tags;
            })
            .exceptionally(throwable -> {
                logger.warn("LLM tag generation failed, falling back to rules: {}", throwable.getMessage());
                return generateTagsWithRules(content, type);
            });
    }
    
    /**
     * 构建分类提示词
     * Builds classification prompt
     * 
     * @param content 记忆内容 / Memory content
     * @param context 上下文信息 / Context information
     * @return 构建的提示词 / Built prompt
     */
    private String buildClassificationPrompt(String content, Map<String, Object> context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Content: ").append(content);
        
        if (context != null && !context.isEmpty()) {
            prompt.append("\nContext: ");
            context.forEach((key, value) -> prompt.append(key).append("=").append(value).append(" "));
        }
        
        return prompt.toString();
    }
    
    /**
     * 构建重要性评估提示词
     * Builds importance assessment prompt
     * 
     * @param content 记忆内容 / Memory content
     * @param type 记忆类型 / Memory type
     * @param context 上下文信息 / Context information
     * @return 构建的提示词 / Built prompt
     */
    private String buildImportancePrompt(String content, MemoryType type, Map<String, Object> context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Memory Type: ").append(type.getValue()).append("\n");
        prompt.append("Content: ").append(content);
        
        if (context != null && !context.isEmpty()) {
            prompt.append("\nContext: ");
            context.forEach((key, value) -> prompt.append(key).append("=").append(value).append(" "));
        }
        
        return prompt.toString();
    }
    
    /**
     * 检查内容是否包含时间信息
     * Checks if content contains temporal information
     * 
     * @param content 记忆内容 / Memory content
     * @return true如果包含时间信息 / true if contains temporal information
     */
    private boolean containsTemporalInfo(String content) {
        String lowerContent = content.toLowerCase();
        
        // Check for schedule/appointment specific keywords
        boolean hasScheduleKeywords = lowerContent.contains("schedule") || 
                                    lowerContent.contains("appointment") || 
                                    lowerContent.contains("meeting") || 
                                    lowerContent.contains("deadline") || 
                                    lowerContent.contains("reminder") || 
                                    lowerContent.contains("calendar");
        
        // Check for specific time patterns
        boolean hasTimePatterns = content.matches(".*\\b\\d{4}-\\d{2}-\\d{2}\\b.*") ||
                                 content.matches(".*\\b\\d{1,2}:\\d{2}\\s*(?:AM|PM)?\\b.*");
        
        // Check for future-oriented temporal references (not past events)
        boolean hasFutureTemporal = lowerContent.contains("tomorrow") || 
                                   lowerContent.contains("next week") || 
                                   lowerContent.contains("next month") || 
                                   lowerContent.contains("scheduled for") ||
                                   lowerContent.contains("due on") ||
                                   lowerContent.contains("every monday") ||
                                   lowerContent.contains("every tuesday") ||
                                   lowerContent.contains("every wednesday") ||
                                   lowerContent.contains("every thursday") ||
                                   lowerContent.contains("every friday") ||
                                   lowerContent.contains("every saturday") ||
                                   lowerContent.contains("every sunday");
        
        return hasScheduleKeywords || hasTimePatterns || hasFutureTemporal;
    }
    
    /**
     * 检查上下文是否包含时间信息
     * Checks if context contains temporal information
     * 
     * @param context 上下文信息 / Context information
     * @return true如果包含时间信息 / true if contains temporal information
     */
    private boolean hasTemporalContext(Map<String, Object> context) {
        if (context == null) return false;
        return context.containsKey("eventTime") || 
               context.containsKey("scheduled") ||
               context.containsKey("temporal_context");
    }
    
    /**
     * 检查内容是否包含指定关键词
     * Checks if content contains specified keywords
     * 
     * @param content 内容 / Content
     * @param keywords 关键词集合 / Set of keywords
     * @return true如果包含任一关键词 / true if contains any keyword
     */
    private boolean containsKeywords(String content, Set<String> keywords) {
        return keywords.stream().anyMatch(content::contains);
    }
    
    /**
     * 检查内容是否包含任一指定关键词
     * Checks if content contains any of the specified keywords
     * 
     * @param content 内容 / Content
     * @param keywords 关键词列表 / List of keywords
     * @return true如果包含任一关键词 / true if contains any keyword
     */
    private boolean containsAnyKeyword(String content, List<String> keywords) {
        return keywords.stream().anyMatch(content::contains);
    }
    
    /**
     * 检查内容是否包含事实信息
     * Checks if content contains factual information
     * 
     * <p>检查是否包含数字、度量、具体数据等事实信息。 / 
     * Checks for numbers, measurements, specific data, and other factual information.</p>
     * 
     * @param content 内容 / Content
     * @return true如果包含事实信息 / true if contains factual information
     */
    private boolean containsFactualInfo(String content) {
        String lowerContent = content.toLowerCase();
        // Check for numbers, measurements, specific data
        return content.matches(".*\\b\\d+(?:\\.\\d+)?\\s*(?:%|percent|kg|km|miles|dollars?|USD|EUR)\\b.*") ||
               content.matches(".*\\b\\d{4}\\b.*") || // Years
               content.contains("fact:") ||
               content.contains("data:") ||
               lowerContent.contains("was created by") ||
               lowerContent.contains("was founded by") ||
               lowerContent.contains("was invented by") ||
               lowerContent.contains("was developed by") ||
               lowerContent.contains("is the capital of") ||
               lowerContent.contains("is located in") ||
               lowerContent.contains("was established in") ||
               lowerContent.contains("complexity is") ||
               lowerContent.contains("algorithm complexity") ||
               lowerContent.contains("o(n") ||
               lowerContent.contains("o(") ||
               lowerContent.contains("big o");
    }
    
    /**
     * 检查内容是否包含情景记忆信息
     * Checks if content contains episodic memory information
     * 
     * <p>检查是否包含个人经历、过去事件等情景记忆信息。 / 
     * Checks for personal experiences, past events, and other episodic memory information.</p>
     * 
     * @param content 内容 / Content
     * @return true如果包含情景记忆信息 / true if contains episodic memory information
     */
    private boolean containsEpisodicInfo(String content) {
        String lowerContent = content.toLowerCase();
        return lowerContent.contains("i remember") ||
               lowerContent.contains("last time") ||
               lowerContent.contains("when i") ||
               lowerContent.contains("experience") ||
               lowerContent.contains("went to") ||
               lowerContent.contains("attended") ||
               lowerContent.contains("visited") ||
               lowerContent.contains("i attended") ||
               lowerContent.matches(".*\\b(?:yesterday|last week|last month|ago)\\b.*") ||
               lowerContent.matches(".*\\b(?:i went|i visited|i attended|i saw|i met)\\b.*");
    }
    
    /**
     * 检查内容是否包含具体信息
     * Checks if content contains specific information
     * 
     * <p>检查是否包含数字、专有名词、邮箱模式等具体信息。 / 
     * Checks for numbers, proper nouns, email patterns, and other specific information.</p>
     * 
     * @param content 内容 / Content
     * @return true如果包含具体信息 / true if contains specific information
     */
    private boolean containsSpecificInfo(String content) {
        return content.matches(".*\\b\\d+(?:\\.\\d+)?\\b.*") || // Numbers
               content.matches(".*\\b[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*\\b.*") || // Proper nouns
               content.contains("@") || // Email-like patterns
               content.length() > 100; // Longer content often more specific
    }
    
    /**
     * 检查是否为常用词
     * Checks if it's a common word
     * 
     * <p>用于过滤实体提取中的常用词汇。 / 
     * Used to filter common words in entity extraction.</p>
     * 
     * @param word 词汇 / Word
     * @return true如果是常用词 / true if it's a common word
     */
    private boolean isCommonWord(String word) {
        Set<String> commonWords = new HashSet<>(Arrays.asList("The", "This", "That", "These", "Those", "And", "But", "Or", "So", "If"));
        return commonWords.contains(word);
    }
}