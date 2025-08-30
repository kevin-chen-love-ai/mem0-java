package com.mem0.template;

import java.util.List;
import java.util.Map;

/**
 * 提示词模板接口 / Prompt Template Interface
 * 
 * 定义了构建AI提示词的标准接口，支持动态内容注入和上下文整合。
 * 用于RAG查询、内存检索和智能对话中的提示词生成和格式化。
 * Defines the standard interface for building AI prompts, supporting dynamic content injection and context integration.
 * Used for prompt generation and formatting in RAG queries, memory retrieval, and intelligent conversations.
 * 
 * <h3>核心功能 / Key Features:</h3>
 * <ul>
 *   <li>动态提示词构建 / Dynamic prompt construction</li>
 *   <li>上下文和内存注入 / Context and memory injection</li>
 *   <li>模板参数化和定制 / Template parameterization and customization</li>
 *   <li>多种模板格式支持 / Multiple template format support</li>
 *   <li>检索内容整合 / Retrieved content integration</li>
 * </ul>
 * 
 * <h3>模板架构 / Template Architecture:</h3>
 * <pre>
 * PromptTemplate
 *       │
 *       ├── BasicRAGTemplate     (基础RAG模板)
 *       ├── ConversationTemplate (对话模板)
 *       ├── SummaryTemplate      (摘要模板)
 *       ├── ClassificationTemplate (分类模板)
 *       └── CustomTemplate       (自定义模板)
 * 
 * PromptContext {
 *   userQuery         // 用户查询
 *   systemMessage     // 系统消息
 *   retrievedMemories // 检索内存列表
 *   additionalContext // 额外上下文
 * }
 * </pre>
 * 
 * <h3>使用示例 / Usage Examples:</h3>
 * <pre>{@code
 * // 创建RAG提示词模板
 * PromptTemplate template = new BasicRAGTemplate();
 * 
 * // 构建上下文
 * PromptContext context = new PromptContext("用户想了解咖啡知识");
 * context.setSystemMessage("你是一个专业的咖啡顾问");
 * 
 * List<RetrievedMemory> memories = Arrays.asList(
 *     new RetrievedMemory("用户喜欢拿铁", 0.95, null, "preference"),
 *     new RetrievedMemory("用户对意式咖啡感兴趣", 0.88, null, "interest")
 * );
 * context.setRetrievedMemories(memories);
 * 
 * // 生成提示词
 * String prompt = template.buildPrompt(context);
 * 
 * // 自定义模板实现
 * PromptTemplate customTemplate = new PromptTemplate() {
 *     @Override
 *     public String buildPrompt(PromptContext context) {
 *         StringBuilder prompt = new StringBuilder();
 *         prompt.append("系统: ").append(context.getSystemMessage()).append("\n\n");
 *         
 *         if (context.getRetrievedMemories() != null) {
 *             prompt.append("相关记忆:\n");
 *             for (RetrievedMemory memory : context.getRetrievedMemories()) {
 *                 prompt.append("- ").append(memory.getContent()).append("\n");
 *             }
 *         }
 *         
 *         prompt.append("\n用户询问: ").append(context.getUserQuery());
 *         return prompt.toString();
 *     }
 *     
 *     @Override
 *     public String getName() { return "CustomTemplate"; }
 *     
 *     @Override
 *     public String getDescription() { return "自定义提示词模板"; }
 * };
 * }</pre>
 * 
 * <h3>模板最佳实践 / Template Best Practices:</h3>
 * <ul>
 *   <li><b>内容优先级</b>: 系统消息 > 检索内容 > 用户查询 / Content priority: System message > Retrieved content > User query</li>
 *   <li><b>长度控制</b>: 控制模板总长度避免token溢出 / Length control: Control template length to avoid token overflow</li>
 *   <li><b>格式一致</b>: 保持提示词格式的一致性 / Format consistency: Maintain consistent prompt formatting</li>
 *   <li><b>上下文相关</b>: 根据检索内容调整模板结构 / Context relevance: Adjust template structure based on retrieved content</li>
 * </ul>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 * @see com.mem0.template.impl.BasicRAGTemplate
 * @see com.mem0.core.EnhancedMemoryService
 */
public interface PromptTemplate {
    
    String buildPrompt(PromptContext context);
    
    String getName();
    
    String getDescription();
    
    static class PromptContext {
        private String userQuery;
        private String systemMessage;
        private List<RetrievedMemory> retrievedMemories;
        private Map<String, Object> additionalContext;
        
        public PromptContext() {}
        
        public PromptContext(String userQuery) {
            this.userQuery = userQuery;
        }
        
        // Getters and setters
        public String getUserQuery() { return userQuery; }
        public void setUserQuery(String userQuery) { this.userQuery = userQuery; }
        
        public String getSystemMessage() { return systemMessage; }
        public void setSystemMessage(String systemMessage) { this.systemMessage = systemMessage; }
        
        public List<RetrievedMemory> getRetrievedMemories() { return retrievedMemories; }
        public void setRetrievedMemories(List<RetrievedMemory> retrievedMemories) { this.retrievedMemories = retrievedMemories; }
        
        public Map<String, Object> getAdditionalContext() { return additionalContext; }
        public void setAdditionalContext(Map<String, Object> additionalContext) { this.additionalContext = additionalContext; }
    }
    
    static class RetrievedMemory {
        private final String content;
        private final double relevanceScore;
        private final Map<String, Object> metadata;
        private final String memoryType;
        
        public RetrievedMemory(String content, double relevanceScore, 
                             Map<String, Object> metadata, String memoryType) {
            this.content = content;
            this.relevanceScore = relevanceScore;
            this.metadata = metadata;
            this.memoryType = memoryType;
        }
        
        public String getContent() { return content; }
        public double getRelevanceScore() { return relevanceScore; }
        public Map<String, Object> getMetadata() { return metadata; }
        public String getMemoryType() { return memoryType; }
    }
}