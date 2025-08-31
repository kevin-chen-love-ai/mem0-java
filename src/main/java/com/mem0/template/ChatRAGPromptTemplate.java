package com.mem0.template;

import com.mem0.llm.LLMProvider.ChatMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * 对话式RAG提示词模板类 - Chat-based RAG prompt template class
 * 
 * <p>此类实现了专门针对对话场景优化的检索增强生成(RAG)提示词模板。
 * 它将检索到的内存和上下文信息结构化地组织成聊天消息格式，
 * 为大语言模型提供更清晰的信息层次和更好的理解能力。</p>
 * 
 * <p>This class implements a Retrieval-Augmented Generation (RAG) prompt template
 * specifically optimized for conversational scenarios. It structurally organizes
 * retrieved memories and contextual information into chat message format,
 * providing clearer information hierarchy and better comprehension for
 * large language models.</p>
 * 
 * <h3>核心特性 / Core Features:</h3>
 * <ul>
 *   <li><strong>结构化消息组织</strong> - 将检索内容组织成清晰的系统消息结构 / Structured message organization - organizing retrieved content into clear system message structure</li>
 *   <li><strong>多层次上下文融合</strong> - 整合内存、元数据和额外上下文信息 / Multi-level context integration - integrating memories, metadata, and additional context</li>
 *   <li><strong>相关性标注</strong> - 为每个检索内存添加相关性评分信息 / Relevance annotation - adding relevance scores to each retrieved memory</li>
 *   <li><strong>类型分类标识</strong> - 明确标识内存的类型和特征 / Type classification identification - clearly identifying memory types and characteristics</li>
 *   <li><strong>模块化设计</strong> - 支持灵活的消息构建和定制化 / Modular design - supporting flexible message construction and customization</li>
 * </ul>
 * 
 * <h3>消息结构设计 / Message Structure Design:</h3>
 * <pre>
 * 聊天消息模板结构 / Chat Message Template Structure:
 * 
 * ┌─ SYSTEM MESSAGE ─────────────────────────────┐
 * │ 1. 基础系统指令 / Base System Instructions   │
 * │ 2. 检索内存部分 / Retrieved Memories Section │
 * │    ├─ [Memory 1] (relevance: 0.95, type: preference) │
 * │    ├─ [Memory 2] (relevance: 0.87, type: factual)    │
 * │    └─ [Memory N] (relevance: 0.72, type: episodic)   │
 * │ 3. 额外上下文 / Additional Context Section    │
 * │    ├─ user_id: user123                       │
 * │    ├─ session_id: session456                 │
 * │    └─ priority: high                         │
 * └─────────────────────────────────────────────┘
 * ┌─ USER MESSAGE ───────────────────────────────┐
 * │ 用户查询内容 / User Query Content             │
 * └─────────────────────────────────────────────┘
 * </pre>
 * 
 * <h3>使用示例 / Usage Examples:</h3>
 * <pre>{@code
 * // 创建对话式RAG模板
 * ChatRAGPromptTemplate template = new ChatRAGPromptTemplate();
 * 
 * // 构建提示词上下文
 * PromptTemplate.PromptContext context = new PromptTemplate.PromptContext();
 * context.setUserQuery("用户喜欢什么类型的音乐？");
 * context.setSystemMessage("你是一个了解用户偏好的AI助手，请基于检索到的信息回答问题。");
 * 
 * // 添加检索到的内存
 * List<PromptTemplate.RetrievedMemory> memories = new ArrayList<>();
 * memories.add(new PromptTemplate.RetrievedMemory(
 *     "用户经常听古典音乐和爵士乐", 
 *     0.92, 
 *     "preference"
 * ));
 * memories.add(new PromptTemplate.RetrievedMemory(
 *     "用户上周购买了莫扎特的专辑", 
 *     0.85, 
 *     "episodic"
 * ));
 * memories.add(new PromptTemplate.RetrievedMemory(
 *     "用户不喜欢重金属音乐", 
 *     0.78, 
 *     "preference"
 * ));
 * context.setRetrievedMemories(memories);
 * 
 * // 添加额外上下文
 * Map<String, Object> additionalContext = new HashMap<>();
 * additionalContext.put("user_id", "music_lover_123");
 * additionalContext.put("query_intent", "preference_inquiry");
 * additionalContext.put("response_style", "conversational");
 * context.setAdditionalContext(additionalContext);
 * 
 * // 构建聊天消息
 * List<ChatMessage> messages = template.buildChatMessages(context);
 * 
 * // 检查生成的消息
 * for (ChatMessage message : messages) {
 *     System.out.println("角色: " + message.getRole());
 *     System.out.println("内容: " + message.getContent());
 *     System.out.println("---");
 * }
 * 
 * // 使用生成的消息调用LLM
 * LLMProvider llmProvider = getLLMProvider();
 * LLMProvider.LLMConfig config = new LLMProvider.LLMConfig();
 * config.setTemperature(0.3);
 * config.setMaxTokens(300);
 * 
 * CompletableFuture<LLMProvider.LLMResponse> responseFuture = 
 *     llmProvider.generateChatCompletion(messages, config);
 * 
 * LLMProvider.LLMResponse response = responseFuture.join();
 * System.out.println("AI回答: " + response.getContent());
 * 
 * // 模板信息
 * System.out.println("模板名称: " + template.getName());
 * System.out.println("模板描述: " + template.getDescription());
 * }</pre>
 * 
 * <h3>模板优势 / Template Advantages:</h3>
 * <ul>
 *   <li><strong>清晰的信息层次</strong> - 明确区分不同类型的信息源 / Clear information hierarchy - clearly distinguishing different types of information sources</li>
 *   <li><strong>相关性透明</strong> - 显示每个内存的相关性评分帮助理解 / Relevance transparency - showing relevance scores for each memory to aid understanding</li>
 *   <li><strong>类型感知</strong> - 标识内存类型帮助LLM做出更准确的推理 / Type awareness - identifying memory types to help LLM make more accurate reasoning</li>
 *   <li><strong>上下文完整</strong> - 提供完整的对话上下文信息 / Complete context - providing complete conversational context information</li>
 *   <li><strong>易于调试</strong> - 结构化格式便于问题诊断和优化 / Easy debugging - structured format facilitates problem diagnosis and optimization</li>
 * </ul>
 * 
 * <h3>适用场景 / Applicable Scenarios:</h3>
 * <ul>
 *   <li><strong>客服对话</strong> - 基于用户历史提供个性化服务 / Customer service - providing personalized service based on user history</li>
 *   <li><strong>智能助手</strong> - 利用用户偏好和历史进行智能推荐 / Intelligent assistant - using user preferences and history for smart recommendations</li>
 *   <li><strong>教育辅导</strong> - 根据学习记录提供定制化教学 / Educational tutoring - providing customized teaching based on learning records</li>
 *   <li><strong>医疗咨询</strong> - 基于病历和症状提供专业建议 / Medical consultation - providing professional advice based on medical records and symptoms</li>
 *   <li><strong>内容创作</strong> - 利用知识库辅助创作和编辑 / Content creation - using knowledge base to assist creation and editing</li>
 * </ul>
 * 
 * <h3>扩展性设计 / Extensibility Design:</h3>
 * <ul>
 *   <li><strong>模板定制</strong> - 支持自定义系统消息和格式规范 / Template customization - supporting custom system messages and format specifications</li>
 *   <li><strong>内容过滤</strong> - 可扩展内存筛选和排序逻辑 / Content filtering - extensible memory filtering and sorting logic</li>
 *   <li><strong>格式适配</strong> - 支持不同LLM的消息格式适配 / Format adaptation - supporting message format adaptation for different LLMs</li>
 *   <li><strong>性能优化</strong> - 支持消息长度限制和截断策略 / Performance optimization - supporting message length limits and truncation strategies</li>
 * </ul>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 * @see PromptTemplate
 * @see DefaultRAGPromptTemplate
 * @see ChatMessage
 */
public class ChatRAGPromptTemplate {
    
    private static final String DEFAULT_SYSTEM_MESSAGE = 
        "You are an AI assistant with access to retrieved memories and context. " +
        "Use the provided information to give accurate and helpful responses. " +
        "When referencing memories, be specific about which memory you're using.";
    
    public List<ChatMessage> buildChatMessages(PromptTemplate.PromptContext context) {
        List<ChatMessage> messages = new ArrayList<>();
        
        // Build system message with context
        StringBuilder systemMessageBuilder = new StringBuilder();
        String baseSystemMessage = context.getSystemMessage() != null ? 
            context.getSystemMessage() : DEFAULT_SYSTEM_MESSAGE;
        systemMessageBuilder.append(baseSystemMessage);
        
        // Add retrieved memories to system message
        if (context.getRetrievedMemories() != null && !context.getRetrievedMemories().isEmpty()) {
            systemMessageBuilder.append("\n\n=== RETRIEVED MEMORIES ===\n");
            
            List<PromptTemplate.RetrievedMemory> memories = context.getRetrievedMemories();
            for (int i = 0; i < memories.size(); i++) {
                PromptTemplate.RetrievedMemory memory = memories.get(i);
                systemMessageBuilder.append(String.format("[Memory %d] (relevance: %.3f", 
                    i + 1, memory.getRelevanceScore()));
                
                if (memory.getMemoryType() != null && !memory.getMemoryType().isEmpty()) {
                    systemMessageBuilder.append(", type: ").append(memory.getMemoryType());
                }
                
                systemMessageBuilder.append(")\n");
                systemMessageBuilder.append(memory.getContent()).append("\n\n");
            }
            systemMessageBuilder.append("=== END RETRIEVED MEMORIES ===");
        }
        
        // Add additional context to system message
        if (context.getAdditionalContext() != null && !context.getAdditionalContext().isEmpty()) {
            systemMessageBuilder.append("\n\n=== ADDITIONAL CONTEXT ===\n");
            context.getAdditionalContext().forEach((key, value) -> {
                systemMessageBuilder.append(key).append(": ").append(value).append("\n");
            });
            systemMessageBuilder.append("=== END ADDITIONAL CONTEXT ===");
        }
        
        messages.add(new ChatMessage(ChatMessage.Role.SYSTEM, systemMessageBuilder.toString()));
        
        // Add user message
        messages.add(new ChatMessage(ChatMessage.Role.USER, context.getUserQuery()));
        
        return messages;
    }
    
    public String getName() {
        return "ChatRAG";
    }
    
    public String getDescription() {
        return "Chat-based Retrieval-Augmented Generation template that formats " +
               "context as structured chat messages for better LLM comprehension.";
    }
}