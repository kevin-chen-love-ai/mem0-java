package com.mem0.template;

import java.util.List;

/**
 * 默认RAG提示词模板类 - Default RAG prompt template class
 * 
 * <p>此类实现了标准的检索增强生成(RAG)提示词模板，提供了将用户查询
 * 与检索到的内存信息以及额外上下文整合的基础功能。它采用简洁明了的
 * 文本格式组织信息，适用于大多数RAG场景的基础需求。</p>
 * 
 * <p>This class implements a standard Retrieval-Augmented Generation (RAG) prompt
 * template, providing basic functionality to integrate user queries with retrieved
 * memory information and additional context. It uses a clear and concise text format
 * to organize information, suitable for basic requirements in most RAG scenarios.</p>
 * 
 * <h3>核心功能 / Core Features:</h3>
 * <ul>
 *   <li><strong>标准化格式</strong> - 提供统一的RAG提示词结构和组织方式 / Standardized format - providing unified RAG prompt structure and organization</li>
 *   <li><strong>内存整合</strong> - 将检索内存按相关性排序并清晰展示 / Memory integration - displaying retrieved memories sorted by relevance clearly</li>
 *   <li><strong>上下文融合</strong> - 无缝整合额外的上下文信息 / Context fusion - seamlessly integrating additional contextual information</li>
 *   <li><strong>指令引导</strong> - 为AI助手提供明确的回答指导原则 / Instruction guidance - providing clear response guidelines for AI assistants</li>
 *   <li><strong>兼容性好</strong> - 适用于各种大语言模型和RAG系统 / Good compatibility - suitable for various LLMs and RAG systems</li>
 * </ul>
 * 
 * <h3>提示词结构 / Prompt Structure:</h3>
 * <pre>
 * 默认RAG提示词模板结构 / Default RAG Prompt Template Structure:
 * 
 * ┌─ SYSTEM SECTION ─────────────────────────────┐
 * │ System: [系统指令和角色定义]                 │
 * │ System: [System instructions and role def]   │
 * └─────────────────────────────────────────────┘
 * ┌─ RETRIEVED MEMORIES SECTION ─────────────────┐
 * │ === RETRIEVED MEMORIES ===                  │
 * │ Memory 1 (relevance: 0.95):                │
 * │ [内存内容1]                                 │
 * │ Type: [内存类型1]                           │
 * │                                             │
 * │ Memory 2 (relevance: 0.87):                │
 * │ [内存内容2]                                 │
 * │ Type: [内存类型2]                           │
 * │ === END RETRIEVED MEMORIES ===              │
 * └─────────────────────────────────────────────┘
 * ┌─ ADDITIONAL CONTEXT SECTION ─────────────────┐
 * │ === ADDITIONAL CONTEXT ===                  │
 * │ key1: value1                                │
 * │ key2: value2                                │
 * │ === END ADDITIONAL CONTEXT ===              │
 * └─────────────────────────────────────────────┘
 * ┌─ USER QUERY SECTION ─────────────────────────┐
 * │ User: [用户查询内容]                        │
 * │ User: [User query content]                  │
 * └─────────────────────────────────────────────┘
 * ┌─ ASSISTANT INSTRUCTION ──────────────────────┐
 * │ Assistant: Please provide a helpful response │
 * │ based on the user's question and the        │
 * │ retrieved memories above. Be accurate and    │
 * │ cite specific memories when relevant.        │
 * └─────────────────────────────────────────────┘
 * </pre>
 * 
 * <h3>使用示例 / Usage Examples:</h3>
 * <pre>{@code
 * // 创建默认RAG模板实例
 * DefaultRAGPromptTemplate template = new DefaultRAGPromptTemplate();
 * 
 * // 构建提示词上下文
 * PromptTemplate.PromptContext context = new PromptTemplate.PromptContext();
 * context.setUserQuery("请告诉我如何制作意大利面");
 * context.setSystemMessage("你是一个专业的烹饪助手，擅长根据已有信息提供烹饪指导");
 * 
 * // 添加检索到的相关内存
 * List<PromptTemplate.RetrievedMemory> memories = new ArrayList<>();
 * memories.add(new PromptTemplate.RetrievedMemory(
 *     "意大利面需要在沸水中煮8-12分钟", 
 *     0.92, 
 *     "procedural"
 * ));
 * memories.add(new PromptTemplate.RetrievedMemory(
 *     "优质意大利面应该有适度的咬劲(al dente)", 
 *     0.85, 
 *     "factual"
 * ));
 * memories.add(new PromptTemplate.RetrievedMemory(
 *     "煮面时水中要加足量的盐", 
 *     0.78, 
 *     "procedural"
 * ));
 * context.setRetrievedMemories(memories);
 * 
 * // 添加额外上下文信息
 * Map<String, Object> additionalContext = new HashMap<>();
 * additionalContext.put("cuisine_type", "Italian");
 * additionalContext.put("difficulty_level", "beginner");
 * additionalContext.put("preparation_time", "20_minutes");
 * context.setAdditionalContext(additionalContext);
 * 
 * // 生成完整的提示词
 * String prompt = template.buildPrompt(context);
 * System.out.println("生成的提示词:");
 * System.out.println(prompt);
 * 
 * // 使用提示词调用LLM
 * LLMProvider llmProvider = getLLMProvider();
 * LLMProvider.LLMConfig config = new LLMProvider.LLMConfig();
 * config.setTemperature(0.7);
 * config.setMaxTokens(500);
 * 
 * CompletableFuture<LLMProvider.LLMResponse> responseFuture = 
 *     llmProvider.generateCompletion(prompt, config);
 * 
 * LLMProvider.LLMResponse response = responseFuture.join();
 * System.out.println("AI回答: " + response.getContent());
 * 
 * // 获取模板信息
 * System.out.println("模板名称: " + template.getName());
 * System.out.println("模板描述: " + template.getDescription());
 * 
 * // 在RAG系统中使用
 * RAGSystem ragSystem = new RAGSystem();
 * ragSystem.setPromptTemplate(template);
 * String answer = ragSystem.query("如何制作意大利面?");
 * System.out.println("RAG系统回答: " + answer);
 * }</pre>
 * 
 * <h3>模板特点 / Template Characteristics:</h3>
 * <ul>
 *   <li><strong>结构清晰</strong> - 明确的段落分隔和标识符便于解析 / Clear structure - distinct paragraph separators and identifiers for easy parsing</li>
 *   <li><strong>信息完整</strong> - 包含所有必要的RAG组件和元数据 / Complete information - including all necessary RAG components and metadata</li>
 *   <li><strong>相关性显示</strong> - 明确标示每个内存的相关性评分 / Relevance display - clearly showing relevance scores for each memory</li>
 *   <li><strong>类型标识</strong> - 标识内存类型便于AI理解和处理 / Type identification - identifying memory types for AI understanding and processing</li>
 *   <li><strong>指令明确</strong> - 为AI提供具体的回答要求和引用指导 / Clear instructions - providing specific response requirements and citation guidance for AI</li>
 * </ul>
 * 
 * <h3>适用场景 / Suitable Scenarios:</h3>
 * <ul>
 *   <li><strong>通用问答</strong> - 基于知识库的通用问题回答 / General Q&A - general question answering based on knowledge base</li>
 *   <li><strong>信息检索</strong> - 结构化信息的查询和展示 / Information retrieval - querying and displaying structured information</li>
 *   <li><strong>知识总结</strong> - 多源信息的整合和总结 / Knowledge summarization - integration and summarization of multi-source information</li>
 *   <li><strong>学习辅助</strong> - 基于学习材料的教学和解答 / Learning assistance - teaching and answering based on learning materials</li>
 *   <li><strong>内容创作</strong> - 基于参考资料的内容生成 / Content creation - content generation based on reference materials</li>
 * </ul>
 * 
 * <h3>扩展能力 / Extension Capabilities:</h3>
 * <ul>
 *   <li><strong>模板继承</strong> - 可作为基础模板被其他专用模板继承 / Template inheritance - can serve as base template for specialized templates</li>
 *   <li><strong>格式定制</strong> - 支持自定义分隔符和格式样式 / Format customization - supporting custom separators and format styles</li>
 *   <li><strong>内容过滤</strong> - 可扩展内存过滤和优先级逻辑 / Content filtering - extensible memory filtering and priority logic</li>
 *   <li><strong>多语言支持</strong> - 支持不同语言环境的提示词生成 / Multilingual support - supporting prompt generation in different language environments</li>
 * </ul>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 * @see PromptTemplate
 * @see ChatRAGPromptTemplate
 * @see PromptContext
 * @see RetrievedMemory
 */
public class DefaultRAGPromptTemplate implements PromptTemplate {
    
    private static final String DEFAULT_SYSTEM_MESSAGE = 
        "You are an AI assistant with access to retrieved memories and context. " +
        "Use the provided information to give accurate and helpful responses.";
    
    @Override
    public String buildPrompt(PromptContext context) {
        StringBuilder promptBuilder = new StringBuilder();
        
        // Add system message
        String systemMessage = context.getSystemMessage() != null ? 
            context.getSystemMessage() : DEFAULT_SYSTEM_MESSAGE;
        promptBuilder.append("System: ").append(systemMessage).append("\n\n");
        
        // Add retrieved memories if available
        if (context.getRetrievedMemories() != null && !context.getRetrievedMemories().isEmpty()) {
            promptBuilder.append("=== RETRIEVED MEMORIES ===\n");
            
            List<RetrievedMemory> memories = context.getRetrievedMemories();
            for (int i = 0; i < memories.size(); i++) {
                RetrievedMemory memory = memories.get(i);
                promptBuilder.append(String.format("Memory %d (relevance: %.3f):\n", 
                    i + 1, memory.getRelevanceScore()));
                promptBuilder.append(memory.getContent()).append("\n");
                
                // Add memory type if available
                if (memory.getMemoryType() != null && !memory.getMemoryType().isEmpty()) {
                    promptBuilder.append("Type: ").append(memory.getMemoryType()).append("\n");
                }
                
                promptBuilder.append("\n");
            }
            promptBuilder.append("=== END RETRIEVED MEMORIES ===\n\n");
        }
        
        // Add additional context if available
        if (context.getAdditionalContext() != null && !context.getAdditionalContext().isEmpty()) {
            promptBuilder.append("=== ADDITIONAL CONTEXT ===\n");
            context.getAdditionalContext().forEach((key, value) -> {
                promptBuilder.append(key).append(": ").append(value).append("\n");
            });
            promptBuilder.append("=== END ADDITIONAL CONTEXT ===\n\n");
        }
        
        // Add user query
        promptBuilder.append("User: ").append(context.getUserQuery()).append("\n\n");
        
        // Add instruction for the assistant
        promptBuilder.append("Assistant: Please provide a helpful response based on the user's question");
        if (context.getRetrievedMemories() != null && !context.getRetrievedMemories().isEmpty()) {
            promptBuilder.append(" and the retrieved memories above");
        }
        promptBuilder.append(". Be accurate and cite specific memories when relevant.");
        
        return promptBuilder.toString();
    }
    
    @Override
    public String getName() {
        return "DefaultRAG";
    }
    
    @Override
    public String getDescription() {
        return "Default Retrieval-Augmented Generation prompt template that combines " +
               "user queries with retrieved memories and additional context.";
    }
}