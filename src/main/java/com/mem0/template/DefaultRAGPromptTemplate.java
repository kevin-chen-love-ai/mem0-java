package com.mem0.template;

import java.util.List;

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