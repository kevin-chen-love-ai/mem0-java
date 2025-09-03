package com.mem0.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration validator with comprehensive validation rules
 * 
 * Validates Mem0Config instances to ensure they are properly configured
 * and will not cause runtime failures.
 */
public class ConfigurationValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationValidator.class);
    
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;
        
        public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = new ArrayList<>(errors);
            this.warnings = new ArrayList<>(warnings);
        }
        
        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
        
        public boolean hasWarnings() { return !warnings.isEmpty(); }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ValidationResult{valid=").append(valid);
            if (!errors.isEmpty()) {
                sb.append(", errors=").append(errors);
            }
            if (!warnings.isEmpty()) {
                sb.append(", warnings=").append(warnings);
            }
            sb.append("}");
            return sb.toString();
        }
    }
    
    /**
     * Validate complete Mem0Config
     */
    public static ValidationResult validate(Mem0Config config) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (config == null) {
            errors.add("Configuration is null");
            return new ValidationResult(false, errors, warnings);
        }
        
        // Validate each component
        validateVectorStoreConfig(config.getVectorStore(), errors, warnings);
        validateGraphStoreConfig(config.getGraphStore(), errors, warnings);
        validateLLMConfig(config.getLlm(), errors, warnings);
        validateEmbeddingConfig(config.getEmbedding(), errors, warnings);
        
        boolean isValid = errors.isEmpty();
        return new ValidationResult(isValid, errors, warnings);
    }
    
    /**
     * Validate and throw exception if invalid
     */
    public static void validateAndThrow(Mem0Config config) {
        ValidationResult result = validate(config);
        
        if (!result.isValid()) {
            String errorMessage = "Configuration validation failed: " + String.join(", ", result.getErrors());
            throw new IllegalArgumentException(errorMessage);
        }
        
        // Log warnings
        for (String warning : result.getWarnings()) {
            logger.warn("Configuration warning: {}", warning);
        }
    }
    
    private static void validateVectorStoreConfig(Mem0Config.VectorStoreConfig config, 
                                                List<String> errors, List<String> warnings) {
        if (config == null) {
            errors.add("Vector store configuration is null");
            return;
        }
        
        String provider = config.getProvider();
        if (provider == null || provider.trim().isEmpty()) {
            warnings.add("Vector store provider not specified, will use default");
        } else {
            switch (provider.toLowerCase()) {
                case "milvus":
                    validateMilvusConfig(config, errors, warnings);
                    break;
                case "inmemory":
                    // No additional validation needed for in-memory
                    break;
                default:
                    warnings.add("Unknown vector store provider: " + provider);
                    break;
            }
        }
    }
    
    private static void validateMilvusConfig(Mem0Config.VectorStoreConfig config, 
                                           List<String> errors, List<String> warnings) {
        if (config.getHost() == null || config.getHost().trim().isEmpty()) {
            errors.add("Milvus host is required");
        }
        
        if (config.getPort() <= 0 || config.getPort() > 65535) {
            errors.add("Milvus port must be between 1 and 65535");
        }
        
        if (config.getToken() == null || config.getToken().trim().isEmpty()) {
            warnings.add("Milvus token not provided, may cause authentication issues");
        }
    }
    
    private static void validateGraphStoreConfig(Mem0Config.GraphStoreConfig config, 
                                               List<String> errors, List<String> warnings) {
        if (config == null) {
            errors.add("Graph store configuration is null");
            return;
        }
        
        String provider = config.getProvider();
        if (provider == null || provider.trim().isEmpty()) {
            warnings.add("Graph store provider not specified, will use default");
        } else {
            switch (provider.toLowerCase()) {
                case "neo4j":
                    validateNeo4jConfig(config, errors, warnings);
                    break;
                case "inmemory":
                    // No additional validation needed for in-memory
                    break;
                default:
                    warnings.add("Unknown graph store provider: " + provider);
                    break;
            }
        }
    }
    
    private static void validateNeo4jConfig(Mem0Config.GraphStoreConfig config, 
                                          List<String> errors, List<String> warnings) {
        if (config.getUri() == null || config.getUri().trim().isEmpty()) {
            errors.add("Neo4j URI is required");
        } else if (!config.getUri().startsWith("bolt://") && !config.getUri().startsWith("neo4j://")) {
            warnings.add("Neo4j URI should typically start with bolt:// or neo4j://");
        }
        
        if (config.getUsername() == null || config.getUsername().trim().isEmpty()) {
            warnings.add("Neo4j username not provided, may cause authentication issues");
        }
        
        if (config.getPassword() == null || config.getPassword().trim().isEmpty()) {
            warnings.add("Neo4j password not provided, may cause authentication issues");
        }
    }
    
    private static void validateLLMConfig(Mem0Config.LLMConfig config, 
                                        List<String> errors, List<String> warnings) {
        if (config == null) {
            errors.add("LLM configuration is null");
            return;
        }
        
        String provider = config.getProvider();
        if (provider == null || provider.trim().isEmpty()) {
            warnings.add("LLM provider not specified, will use default");
            return;
        }
        
        switch (provider.toLowerCase()) {
            case "openai":
            case "qwen":
                if (config.getApiKey() == null || config.getApiKey().trim().isEmpty()) {
                    errors.add(provider + " LLM provider requires API key");
                } else if (isPlaceholderKey(config.getApiKey())) {
                    warnings.add(provider + " API key appears to be a placeholder");
                }
                break;
            case "rulebased":
            case "mock":
                // No API key required
                break;
            default:
                warnings.add("Unknown LLM provider: " + provider);
                break;
        }
        
        // Validate model parameters
        Double temperature = config.getTemperature();
        if (temperature != null && (temperature < 0.0 || temperature > 2.0)) {
            errors.add("LLM temperature must be between 0.0 and 2.0");
        }
        
        Integer maxTokens = config.getMaxTokens();
        if (maxTokens != null && (maxTokens <= 0 || maxTokens > 32000)) {
            warnings.add("LLM max tokens should be between 1 and 32000");
        }
    }
    
    private static void validateEmbeddingConfig(Mem0Config.EmbeddingConfig config, 
                                              List<String> errors, List<String> warnings) {
        if (config == null) {
            errors.add("Embedding configuration is null");
            return;
        }
        
        String provider = config.getProvider();
        if (provider == null || provider.trim().isEmpty()) {
            warnings.add("Embedding provider not specified, will use default");
            return;
        }
        
        switch (provider.toLowerCase()) {
            case "openai":
            case "aliyun":
                if (config.getApiKey() == null || config.getApiKey().trim().isEmpty()) {
                    errors.add(provider + " embedding provider requires API key");
                } else if (isPlaceholderKey(config.getApiKey())) {
                    warnings.add(provider + " API key appears to be a placeholder");
                }
                break;
            case "tfidf":
            case "mock":
                // No API key required
                break;
            default:
                warnings.add("Unknown embedding provider: " + provider);
                break;
        }
    }
    
    private static boolean isPlaceholderKey(String apiKey) {
        if (apiKey == null) return true;
        String key = apiKey.toLowerCase();
        return key.contains("your-api-key") || 
               key.contains("placeholder") || 
               key.contains("dummy") ||
               key.equals("test-key") ||
               key.equals("fake-key");
    }
}