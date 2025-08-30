package com.mem0.llm.impl;

import com.mem0.llm.LLMProvider;
import com.mem0.llm.LLMResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于规则的LLM提供者 / Rule-based LLM provider for text analysis and generation
 * 
 * <p>实现了一个基于预定义规则和模式匹配的语言模型提供者，用于处理基本的文本分析、分类和生成任务。
 * 适合在没有外部LLM服务时使用，提供基础的智能文本处理功能。</p>
 * 
 * <p>Implements a language model provider based on predefined rules and pattern matching 
 * for handling basic text analysis, classification, and generation tasks. 
 * Suitable for use when external LLM services are unavailable, providing fundamental 
 * intelligent text processing capabilities.</p>
 * 
 * <p>主要特性 / Key features:</p>
 * <ul>
 *   <li>记忆类型自动分类 / Automatic memory type classification</li>
 *   <li>记忆冲突检测和分析 / Memory conflict detection and analysis</li>
 *   <li>记忆合并建议生成 / Memory merge suggestion generation</li>
 *   <li>RAG查询响应生成 / RAG query response generation</li>
 *   <li>模式匹配和规则引擎 / Pattern matching and rule engine</li>
 *   <li>可扩展的模板系统 / Extensible template system</li>
 * </ul>
 * 
 * <p>使用示例 / Usage example:</p>
 * <pre>{@code
 * // 创建规则基础LLM提供者
 * RuleBasedLLMProvider provider = new RuleBasedLLMProvider();
 * 
 * // 分类记忆
 * String classifyPrompt = "classify: I prefer coffee over tea";
 * LLMResponse response = provider.generate(classifyPrompt, 0.5, 100).join();
 * 
 * // 冲突分析
 * String conflictPrompt = "analyze conflict between memories...";
 * LLMResponse conflictResponse = provider.generate(conflictPrompt, 0.7, 200).join();
 * }</pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class RuleBasedLLMProvider implements LLMProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(RuleBasedLLMProvider.class);
    
    private final Map<String, Pattern> classificationPatterns;
    private final Map<String, List<String>> templates;
    private final Random random;
    
    public RuleBasedLLMProvider() {
        this.random = new Random();
        this.classificationPatterns = initializeClassificationPatterns();
        this.templates = initializeTemplates();
        logger.info("Initialized RuleBasedLLMProvider");
    }
    
    @Override
    public CompletableFuture<LLMResponse> generateCompletion(LLMRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String prompt = request.getPrompt();
                logger.debug("Generating response for prompt: {}", prompt.substring(0, Math.min(prompt.length(), 100)) + "...");
                
                String content;
                if (prompt.toLowerCase().contains("classify")) {
                    content = classifyMemory(prompt);
                } else if (prompt.toLowerCase().contains("conflict")) {
                    content = analyzeConflict(prompt);
                } else if (prompt.toLowerCase().contains("merge")) {
                    content = suggestMerge(prompt);
                } else if (prompt.toLowerCase().contains("rag") || prompt.toLowerCase().contains("context")) {
                    content = generateRAGResponse(prompt);
                } else {
                    content = generateGenericResponse(prompt, 0.7);
                }
                
                // Create proper LLMResponse
                return new LLMResponse(content, content.length() / 4, "rule-based-llm", "stop");
            } catch (Exception e) {
                logger.error("Failed to generate response", e);
                return createErrorResponse("Failed to generate response: " + e.getMessage());
            }
        });
    }
    
    private String classifyMemory(String prompt) {
        String text = extractContentFromPrompt(prompt);
        
        // Rule-based memory type classification
        for (Map.Entry<String, Pattern> entry : classificationPatterns.entrySet()) {
            if (entry.getValue().matcher(text.toLowerCase()).find()) {
                return entry.getKey().toUpperCase();
            }
        }
        
        // Fallback classification based on common patterns
        if (containsPersonalPronouns(text) && containsTimeIndicators(text)) {
            return "EPISODIC";
        } else if (containsPreferenceWords(text)) {
            return "PREFERENCE";
        } else if (containsInstructionalWords(text)) {
            return "PROCEDURAL";
        } else if (containsFactualIndicators(text)) {
            return "FACTUAL";
        } else if (containsRelationshipWords(text)) {
            return "RELATIONSHIP";
        } else if (containsTimePatterns(text)) {
            return "TEMPORAL";
        } else if (containsContextualWords(text)) {
            return "CONTEXTUAL";
        }
        
        return "SEMANTIC"; // Default fallback
    }
    
    private String analyzeConflict(String prompt) {
        String text = prompt.toLowerCase();
        
        // Extract the two memories being compared
        List<String> memories = extractMemoriesFromConflictPrompt(prompt);
        
        if (memories.size() < 2) {
            return "NO_CONFLICT: Insufficient information to detect conflicts";
        }
        
        String memory1 = memories.get(0).toLowerCase();
        String memory2 = memories.get(1).toLowerCase();
        
        // Rule-based conflict detection
        if (hasDirectContradiction(memory1, memory2)) {
            return "CONFLICT: Direct contradiction detected between the statements";
        } else if (hasPreferenceConflict(memory1, memory2)) {
            return "CONFLICT: Conflicting preferences detected";
        } else if (hasFactualConflict(memory1, memory2)) {
            return "CONFLICT: Contradictory factual information";
        } else if (hasTemporalConflict(memory1, memory2)) {
            return "CONFLICT: Temporal inconsistency detected";
        } else if (hasSimilarContent(memory1, memory2)) {
            return "DUPLICATE: Very similar content detected";
        } else if (hasSemanticOverlap(memory1, memory2)) {
            return "OVERLAP: Related content that may need consolidation";
        }
        
        return "NO_CONFLICT: No significant conflicts detected";
    }
    
    private String suggestMerge(String prompt) {
        List<String> memories = extractMemoriesFromConflictPrompt(prompt);
        
        if (memories.size() < 2) {
            return "KEEP_SECOND: Default resolution";
        }
        
        String memory1 = memories.get(0);
        String memory2 = memories.get(1);
        
        // Rule-based conflict resolution
        if (isMoreRecent(memory2, memory1)) {
            return "KEEP_SECOND: More recent information should be preferred";
        } else if (isMoreDetailed(memory1, memory2)) {
            return "KEEP_FIRST: More detailed information should be retained";
        } else if (canBeMerged(memory1, memory2)) {
            return "MERGE: " + createMergedContent(memory1, memory2);
        } else if (bothAreValid(memory1, memory2)) {
            return "KEEP_BOTH: Both memories contain unique valid information";
        }
        
        return "KEEP_SECOND: Default to keeping newer information";
    }
    
    private String generateRAGResponse(String prompt) {
        // Extract query and context from RAG prompt
        String query = extractQueryFromRAGPrompt(prompt);
        List<String> contexts = extractContextFromRAGPrompt(prompt);
        
        if (contexts.isEmpty()) {
            return "I don't have enough information to answer that question.";
        }
        
        // Generate response based on context
        StringBuilder response = new StringBuilder();
        
        if (query.toLowerCase().contains("preference")) {
            response.append("Based on the available information, ");
            response.append(synthesizePreferences(contexts));
        } else if (query.toLowerCase().contains("skill") || query.toLowerCase().contains("know")) {
            response.append("According to the records, ");
            response.append(synthesizeSkills(contexts));
        } else if (query.toLowerCase().contains("experience")) {
            response.append("From the available context, ");
            response.append(synthesizeExperiences(contexts));
        } else {
            response.append("Based on the available information: ");
            response.append(synthesizeGeneral(contexts));
        }
        
        return response.toString();
    }
    
    private String generateGenericResponse(String prompt, double temperature) {
        List<String> possibleResponses = templates.getOrDefault("generic", Arrays.asList(
            "I understand your request.",
            "That's an interesting point.",
            "Let me consider that information.",
            "I've processed your input.",
            "Thank you for the information."
        ));
        
        if (temperature > 0.7) {
            // High temperature - more random selection
            return possibleResponses.get(random.nextInt(possibleResponses.size()));
        } else {
            // Low temperature - more deterministic
            return possibleResponses.get(0);
        }
    }
    
    private Map<String, Pattern> initializeClassificationPatterns() {
        Map<String, Pattern> patterns = new HashMap<>();
        
        // Episodic patterns
        patterns.put("episodic", Pattern.compile("\\b(yesterday|last week|when i|remember when|that time|experience|happened)\\b"));
        
        // Procedural patterns
        patterns.put("procedural", Pattern.compile("\\b(how to|steps to|process|method|procedure|tutorial|guide|instruction)\\b"));
        
        // Preference patterns
        patterns.put("preference", Pattern.compile("\\b(prefer|like|favorite|better than|rather|choose|enjoy)\\b"));
        
        // Factual patterns
        patterns.put("factual", Pattern.compile("\\b(is|are|was|were|fact|true|definition|capital|located|created)\\b"));
        
        // Relationship patterns
        patterns.put("relationship", Pattern.compile("\\b(manager|colleague|friend|partner|team|works with|reports to)\\b"));
        
        // Temporal patterns
        patterns.put("temporal", Pattern.compile("\\b(every|daily|weekly|schedule|meeting|appointment|deadline)\\b"));
        
        // Contextual patterns
        patterns.put("contextual", Pattern.compile("\\b(in the|at the|when in|during|context|environment|situation)\\b"));
        
        return patterns;
    }
    
    private Map<String, List<String>> initializeTemplates() {
        Map<String, List<String>> templates = new HashMap<>();
        
        templates.put("conflict_resolution", Arrays.asList(
            "KEEP_FIRST: The original information appears more reliable",
            "KEEP_SECOND: The newer information seems more accurate",
            "MERGE: Both pieces of information can be combined",
            "KEEP_BOTH: Both memories contain unique information"
        ));
        
        templates.put("rag_responses", Arrays.asList(
            "Based on the available information, ",
            "According to the records, ",
            "From what I can see in the context, ",
            "The information suggests that "
        ));
        
        return templates;
    }
    
    // Helper methods for pattern matching
    private boolean containsPersonalPronouns(String text) {
        return Pattern.compile("\\b(i|my|me|myself|mine)\\b").matcher(text.toLowerCase()).find();
    }
    
    private boolean containsTimeIndicators(String text) {
        return Pattern.compile("\\b(yesterday|today|tomorrow|last|next|when|ago|recently)\\b").matcher(text.toLowerCase()).find();
    }
    
    private boolean containsPreferenceWords(String text) {
        return Pattern.compile("\\b(prefer|like|love|hate|favorite|better|best|worst)\\b").matcher(text.toLowerCase()).find();
    }
    
    private boolean containsInstructionalWords(String text) {
        return Pattern.compile("\\b(how|to|step|process|method|guide|tutorial|instruction)\\b").matcher(text.toLowerCase()).find();
    }
    
    private boolean containsFactualIndicators(String text) {
        return Pattern.compile("\\b(is|are|was|were|fact|definition|located|created|founded)\\b").matcher(text.toLowerCase()).find();
    }
    
    private boolean containsRelationshipWords(String text) {
        return Pattern.compile("\\b(manager|colleague|friend|team|works|reports|relationship)\\b").matcher(text.toLowerCase()).find();
    }
    
    private boolean containsTimePatterns(String text) {
        return Pattern.compile("\\b(every|daily|weekly|monthly|schedule|meeting|deadline)\\b").matcher(text.toLowerCase()).find();
    }
    
    private boolean containsContextualWords(String text) {
        return Pattern.compile("\\b(in|at|during|when|where|context|environment|situation)\\b").matcher(text.toLowerCase()).find();
    }
    
    // Conflict detection helpers
    private boolean hasDirectContradiction(String memory1, String memory2) {
        String[] negationPairs = {
            "like,hate", "prefer,dislike", "good,bad", "yes,no", "true,false", "correct,incorrect"
        };
        
        for (String pair : negationPairs) {
            String[] words = pair.split(",");
            if (memory1.contains(words[0]) && memory2.contains(words[1]) ||
                memory1.contains(words[1]) && memory2.contains(words[0])) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean hasPreferenceConflict(String memory1, String memory2) {
        return (containsPreferenceWords(memory1) && containsPreferenceWords(memory2)) &&
               !hasSimilarContent(memory1, memory2);
    }
    
    private boolean hasFactualConflict(String memory1, String memory2) {
        return containsFactualIndicators(memory1) && containsFactualIndicators(memory2) &&
               hasDirectContradiction(memory1, memory2);
    }
    
    private boolean hasTemporalConflict(String memory1, String memory2) {
        return containsTimePatterns(memory1) && containsTimePatterns(memory2) &&
               !hasSimilarContent(memory1, memory2);
    }
    
    private boolean hasSimilarContent(String memory1, String memory2) {
        String[] words1 = memory1.split("\\s+");
        String[] words2 = memory2.split("\\s+");
        
        Set<String> set1 = new HashSet<>(Arrays.asList(words1));
        Set<String> set2 = new HashSet<>(Arrays.asList(words2));
        
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        
        double similarity = (double) intersection.size() / union.size();
        return similarity > 0.6; // 60% word overlap threshold
    }
    
    private boolean hasSemanticOverlap(String memory1, String memory2) {
        return hasSimilarContent(memory1, memory2) && calculateSimilarity(memory1, memory2) > 0.3;
    }
    
    private double calculateSimilarity(String text1, String text2) {
        // Simple word overlap similarity
        Set<String> words1 = new HashSet<>(Arrays.asList(text1.toLowerCase().split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(text2.toLowerCase().split("\\s+")));
        
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    // Extract content from various prompt formats
    private String extractContentFromPrompt(String prompt) {
        // Simple extraction - look for content after "classify" or "content:"
        String[] lines = prompt.split("\\n");
        for (String line : lines) {
            if (line.toLowerCase().contains("content:") || line.toLowerCase().contains("text:")) {
                return line.substring(line.indexOf(":") + 1).trim();
            }
        }
        
        // If no specific format, return the whole prompt
        return prompt;
    }
    
    private List<String> extractMemoriesFromConflictPrompt(String prompt) {
        List<String> memories = new ArrayList<>();
        
        // Look for memory patterns in the prompt
        String[] sections = prompt.split("Memory [12]:|Existing:|New:");
        
        for (int i = 1; i < sections.length; i++) {
            String section = sections[i].trim();
            if (!section.isEmpty()) {
                memories.add(section.split("\\n")[0].trim());
            }
        }
        
        return memories;
    }
    
    private String extractQueryFromRAGPrompt(String prompt) {
        String[] lines = prompt.split("\\n");
        for (String line : lines) {
            if (line.toLowerCase().contains("query:") || line.toLowerCase().contains("question:")) {
                return line.substring(line.indexOf(":") + 1).trim();
            }
        }
        return prompt.split("\\n")[0]; // Default to first line
    }
    
    private List<String> extractContextFromRAGPrompt(String prompt) {
        List<String> contexts = new ArrayList<>();
        
        String[] sections = prompt.split("Context:|Memory:");
        for (int i = 1; i < sections.length; i++) {
            String context = sections[i].trim();
            if (!context.isEmpty()) {
                contexts.add(context.split("\\n")[0].trim());
            }
        }
        
        return contexts;
    }
    
    // Synthesis methods for RAG responses
    private String synthesizePreferences(List<String> contexts) {
        StringBuilder prefs = new StringBuilder();
        for (String context : contexts) {
            if (containsPreferenceWords(context)) {
                if (prefs.length() > 0) prefs.append(" and ");
                prefs.append(context.toLowerCase());
            }
        }
        return prefs.length() > 0 ? prefs.toString() : "no specific preferences found in the records.";
    }
    
    private String synthesizeSkills(List<String> contexts) {
        Set<String> skills = new HashSet<>();
        for (String context : contexts) {
            // Extract potential skills/technologies
            String[] words = context.toLowerCase().split("\\s+");
            for (String word : words) {
                if (word.matches(".*(?:java|python|javascript|sql|spring|react|node).*")) {
                    skills.add(word);
                }
            }
        }
        return skills.isEmpty() ? "various skills are mentioned in the records." : 
               "skills include: " + String.join(", ", skills);
    }
    
    private String synthesizeExperiences(List<String> contexts) {
        List<String> experiences = new ArrayList<>();
        for (String context : contexts) {
            if (containsTimeIndicators(context) && containsPersonalPronouns(context)) {
                experiences.add(context);
            }
        }
        return experiences.isEmpty() ? "some experiences are recorded." : 
               experiences.get(0) + (experiences.size() > 1 ? " and other experiences." : "");
    }
    
    private String synthesizeGeneral(List<String> contexts) {
        if (contexts.isEmpty()) return "No relevant information found.";
        
        // Return a summary of the first few contexts
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < Math.min(3, contexts.size()); i++) {
            if (summary.length() > 0) summary.append(" Additionally, ");
            summary.append(contexts.get(i));
        }
        
        return summary.toString();
    }
    
    // Merge content creation
    private String createMergedContent(String memory1, String memory2) {
        return "Combined information: " + memory1 + " and " + memory2;
    }
    
    private boolean isMoreRecent(String memory1, String memory2) {
        // Simple heuristic based on recent time words
        return containsTimeIndicators(memory1) && memory1.contains("recent");
    }
    
    private boolean isMoreDetailed(String memory1, String memory2) {
        return memory1.length() > memory2.length() * 1.5;
    }
    
    private boolean canBeMerged(String memory1, String memory2) {
        return hasSimilarContent(memory1, memory2) && !hasDirectContradiction(memory1, memory2);
    }
    
    private boolean bothAreValid(String memory1, String memory2) {
        return !hasSimilarContent(memory1, memory2) && !hasDirectContradiction(memory1, memory2);
    }
    
    private LLMResponse createErrorResponse(String error) {
        return new LLMResponse("Error: " + error, error.length() / 4, "rule-based-llm", "error");
    }
    
    private Map<String, Integer> createUsage(int promptLength, int responseLength) {
        Map<String, Integer> usage = new HashMap<>();
        usage.put("prompt_tokens", promptLength / 4); // Rough approximation
        usage.put("completion_tokens", responseLength / 4);
        usage.put("total_tokens", usage.get("prompt_tokens") + usage.get("completion_tokens"));
        return usage;
    }
    
    @Override
    public CompletableFuture<LLMResponse> generateChatCompletion(List<ChatMessage> messages, LLMConfig config) {
        // Convert chat messages to a single prompt
        StringBuilder promptBuilder = new StringBuilder();
        for (ChatMessage message : messages) {
            promptBuilder.append(message.getRole().toString().toLowerCase()).append(": ").append(message.getContent()).append("\\n");
        }
        
        LLMRequest request = new LLMRequest(promptBuilder.toString(), config);
        return generateCompletion(request);
    }
    
    @Override
    public CompletableFuture<LLMResponse> generate(String prompt, double temperature, int maxTokens) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Generating response for prompt: {}", prompt.substring(0, Math.min(prompt.length(), 100)) + "...");
                
                String content;
                if (prompt.toLowerCase().contains("classify")) {
                    content = classifyMemory(prompt);
                } else if (prompt.toLowerCase().contains("conflict")) {
                    content = analyzeConflict(prompt);
                } else if (prompt.toLowerCase().contains("merge")) {
                    content = suggestMerge(prompt);
                } else if (prompt.toLowerCase().contains("rag") || prompt.toLowerCase().contains("context")) {
                    content = generateRAGResponse(prompt);
                } else {
                    content = generateGenericResponse(prompt, temperature);
                }
                
                // Limit content based on maxTokens (rough approximation: 1 token = 4 characters)
                int maxCharacters = maxTokens * 4;
                if (content.length() > maxCharacters) {
                    content = content.substring(0, maxCharacters) + "...";
                }
                
                // Create proper LLMResponse
                return new LLMResponse(content, Math.min(content.length() / 4, maxTokens), "rule-based-llm", "stop");
            } catch (Exception e) {
                logger.error("Failed to generate response", e);
                return createErrorResponse("Failed to generate response: " + e.getMessage());
            }
        });
    }
    
    @Override
    public String getProviderName() {
        return "RuleBased";
    }
    
    @Override
    public boolean supportsStreaming() {
        return false;
    }
    
    @Override
    public void close() {
        logger.info("Closing RuleBasedLLMProvider");
        classificationPatterns.clear();
        templates.clear();
    }
}