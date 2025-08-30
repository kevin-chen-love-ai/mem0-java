package com.mem0.template;

import com.mem0.llm.LLMProvider.ChatMessage;
import java.util.ArrayList;
import java.util.List;

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