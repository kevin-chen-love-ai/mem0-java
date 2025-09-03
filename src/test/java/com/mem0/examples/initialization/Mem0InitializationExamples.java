package com.mem0.examples.initialization;

import com.mem0.Mem0;
import com.mem0.config.Mem0Config;
import com.mem0.llm.LLMProvider;
import com.mem0.llm.impl.QwenLLMProvider;
import com.mem0.llm.impl.RuleBasedLLMProvider;
import com.mem0.embedding.EmbeddingProvider;
import com.mem0.embedding.impl.AliyunEmbeddingProvider;
import com.mem0.embedding.impl.SimpleTFIDFEmbeddingProvider;
import com.mem0.store.VectorStore;
import com.mem0.store.GraphStore;
import com.mem0.vector.impl.InMemoryVectorStore;
import com.mem0.graph.impl.InMemoryGraphStore;
import com.mem0.util.TestConfiguration;

import java.util.Properties;
import java.io.InputStream;

/**
 * Mem0 Initialization Examples - Java 8 Compatible
 * 
 * Demonstrates various ways to initialize Mem0 instances:
 * 1. Default initialization with TestConfiguration
 * 2. Programmatic configuration
 * 3. Environment variable based initialization
 * 4. Custom provider initialization
 * 5. Step-by-step component initialization
 * 6. Test environment specific initialization
 * 
 * All examples are compatible with Java 8 and use existing codebase components.
 * 
 * @author system
 * @version 1.0
 */
public class Mem0InitializationExamples {

    public static void main(String[] args) {
        try {
            System.out.println("=== Mem0 Initialization Examples ===");
            
            // Example 1: Default initialization using TestConfiguration
            System.out.println("\n1. Default Initialization with TestConfiguration:");
            demonstrateDefaultInitialization();
            
            // Example 2: Programmatic configuration
            System.out.println("\n2. Programmatic Configuration:");
            demonstrateProgrammaticConfiguration();
            
            // Example 3: Environment variable based
            System.out.println("\n3. Environment Variable Based:");
            demonstrateEnvironmentBasedConfiguration();
            
            // Example 4: Custom provider initialization
            System.out.println("\n4. Custom Provider Initialization:");
            demonstrateCustomProviderInitialization();
            
            // Example 5: Step-by-step component initialization
            System.out.println("\n5. Step-by-Step Component Initialization:");
            demonstrateStepByStepInitialization();
            
            // Example 6: Test environment specific
            System.out.println("\n6. Test Environment Specific Initialization:");
            demonstrateTestEnvironmentInitialization();
            
            System.out.println("\n=== All initialization examples completed ===");
            
        } catch (Exception e) {
            System.err.println("Error in initialization examples: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Example 1: Default initialization using TestConfiguration
     * This is the simplest and recommended way for testing
     */
    public static void demonstrateDefaultInitialization() {
        try {
            System.out.println("Creating Mem0 with default TestConfiguration...");
            
            // Use the centralized TestConfiguration for all providers
            Mem0Config config = TestConfiguration.createMem0Config();
            
            // This would create a Mem0 instance (commented to avoid actual instantiation)
            // Mem0 mem0 = new Mem0(config);
            
            System.out.println("✓ Configuration created successfully");
            System.out.println("  - LLM Provider: " + config.getLlm().getProvider());
            System.out.println("  - Embedding Provider: " + config.getEmbedding().getProvider());
            System.out.println("  - Vector Store: " + config.getVectorStore().getProvider());
            System.out.println("  - Graph Store: " + config.getGraphStore().getProvider());
            
        } catch (IllegalArgumentException e) {
            System.err.println("✗ Default initialization failed - Invalid argument: " + e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("✗ Default initialization failed - Runtime error: " + e.getMessage());
        }
    }

    /**
     * Example 2: Programmatic configuration
     * Build configuration step by step with specific settings
     */
    public static void demonstrateProgrammaticConfiguration() {
        try {
            System.out.println("Creating Mem0 with programmatic configuration...");
            
            // Create config programmatically
            Mem0Config config = new Mem0Config();
            
            // Configure LLM
            config.getLlm().setProvider("qwen");
            config.getLlm().setApiKey(validateApiKey("your-api-key"));
            config.getLlm().setModel("qwen-plus");
            config.getLlm().setTemperature(validateTemperature(0.7));
            config.getLlm().setMaxTokens(validateMaxTokens(1000));
            
            // Configure Embedding
            config.getEmbedding().setProvider("aliyun");
            config.getEmbedding().setApiKey(validateApiKey("your-embedding-key"));
            config.getEmbedding().setModel("text-embedding-v1");
            
            // Configure Vector Store
            config.getVectorStore().setProvider("inmemory");
            
            // Configure Graph Store
            config.getGraphStore().setProvider("inmemory");
            
            System.out.println("✓ Programmatic configuration created successfully");
            System.out.println("  - LLM Model: " + config.getLlm().getModel());
            System.out.println("  - LLM Temperature: " + config.getLlm().getTemperature());
            System.out.println("  - Embedding Model: " + config.getEmbedding().getModel());
            
        } catch (IllegalArgumentException e) {
            System.err.println("✗ Programmatic configuration failed - Invalid argument: " + e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("✗ Programmatic configuration failed - Runtime error: " + e.getMessage());
        }
    }

    /**
     * Example 3: Environment variable based configuration
     * Uses environment variables and system properties
     */
    public static void demonstrateEnvironmentBasedConfiguration() {
        try {
            System.out.println("Creating Mem0 with environment-based configuration...");
            
            // Create config that respects environment variables
            Mem0Config config = new Mem0Config();
            
            // Configure using environment variables or system properties with fallbacks
            String llmApiKey = System.getenv("MEM0_LLM_API_KEY");
            if (llmApiKey == null) {
                llmApiKey = System.getProperty("mem0.llm.apikey", "fallback-key");
            }
            
            String embeddingApiKey = System.getenv("MEM0_EMBEDDING_API_KEY");
            if (embeddingApiKey == null) {
                embeddingApiKey = System.getProperty("mem0.embedding.apikey", "fallback-key");
            }
            
            String vectorProvider = System.getenv("MEM0_VECTOR_PROVIDER");
            if (vectorProvider == null) {
                vectorProvider = System.getProperty("mem0.vector.provider", "inmemory");
            }
            
            config.getLlm().setProvider("qwen");
            config.getLlm().setApiKey(llmApiKey);
            config.getEmbedding().setProvider("aliyun");
            config.getEmbedding().setApiKey(embeddingApiKey);
            config.getVectorStore().setProvider(vectorProvider);
            config.getGraphStore().setProvider("inmemory");
            
            System.out.println("✓ Environment-based configuration created successfully");
            System.out.println("  - LLM API Key: " + maskApiKey(llmApiKey));
            System.out.println("  - Embedding API Key: " + maskApiKey(embeddingApiKey));
            System.out.println("  - Vector Provider: " + vectorProvider);
            
        } catch (Exception e) {
            System.err.println("✗ Environment-based configuration failed: " + e.getMessage());
        }
    }

    /**
     * Example 4: Custom provider initialization
     * Create providers manually and inject them
     */
    public static void demonstrateCustomProviderInitialization() {
        System.out.println("Creating Mem0 with custom providers...");
        
        // Use resource management for providers
        withProviders((llmProvider, embeddingProvider) -> {
            VectorStore vectorStore = new InMemoryVectorStore();
            GraphStore graphStore = new InMemoryGraphStore();
            
            // Create config and set providers
            Mem0Config config = new Mem0Config();
            config.getLlm().setProvider("custom");
            config.getEmbedding().setProvider("custom");
            config.getVectorStore().setProvider("inmemory");
            config.getGraphStore().setProvider("inmemory");
            
            // You would then inject these providers into Mem0
            // This demonstrates the concept without actual Mem0 instantiation
            
            System.out.println("✓ Custom provider initialization completed");
            System.out.println("  - LLM Provider: " + llmProvider.getClass().getSimpleName());
            System.out.println("  - Embedding Provider: " + embeddingProvider.getClass().getSimpleName());
            System.out.println("  - Vector Store: " + vectorStore.getClass().getSimpleName());
            System.out.println("  - Graph Store: " + graphStore.getClass().getSimpleName());
        });
    }

    /**
     * Example 5: Step-by-step component initialization
     * Initialize each component individually with validation
     */
    public static void demonstrateStepByStepInitialization() {
        try {
            System.out.println("Creating Mem0 with step-by-step initialization...");
            
            // Step 1: Initialize LLM Provider
            System.out.println("  Step 1: Initializing LLM Provider...");
            LLMProvider llmProvider = TestConfiguration.getLLMProvider();
            if (llmProvider != null) {
                System.out.println("    ✓ LLM Provider initialized: " + llmProvider.getClass().getSimpleName());
            } else {
                System.out.println("    ✗ LLM Provider initialization failed, using fallback");
                llmProvider = new RuleBasedLLMProvider();
            }
            
            // Step 2: Initialize Embedding Provider
            System.out.println("  Step 2: Initializing Embedding Provider...");
            EmbeddingProvider embeddingProvider = TestConfiguration.getEmbeddingProvider();
            if (embeddingProvider != null) {
                System.out.println("    ✓ Embedding Provider initialized: " + embeddingProvider.getClass().getSimpleName());
            } else {
                System.out.println("    ✗ Embedding Provider initialization failed, using fallback");
                embeddingProvider = new SimpleTFIDFEmbeddingProvider();
            }
            
            // Step 3: Initialize Vector Store
            System.out.println("  Step 3: Initializing Vector Store...");
            VectorStore vectorStore = TestConfiguration.getVectorStore();
            System.out.println("    ✓ Vector Store initialized: " + vectorStore.getClass().getSimpleName());
            
            // Step 4: Initialize Graph Store
            System.out.println("  Step 4: Initializing Graph Store...");
            GraphStore graphStore = TestConfiguration.getGraphStore();
            System.out.println("    ✓ Graph Store initialized: " + graphStore.getClass().getSimpleName());
            
            // Step 5: Create configuration
            System.out.println("  Step 5: Creating final configuration...");
            Mem0Config config = TestConfiguration.createMem0Config();
            System.out.println("    ✓ Configuration created successfully");
            
            System.out.println("✓ Step-by-step initialization completed successfully");
            
        } catch (Exception e) {
            System.err.println("✗ Step-by-step initialization failed: " + e.getMessage());
        }
    }

    /**
     * Example 6: Test environment specific initialization
     * Different initialization strategies based on test environment
     */
    public static void demonstrateTestEnvironmentInitialization() {
        try {
            System.out.println("Creating Mem0 for different test environments...");
            
            // Check test environment
            String testEnv = System.getProperty("test.environment", "unit");
            System.out.println("  Test Environment: " + testEnv);
            
            Mem0Config config;
            
            switch (testEnv) {
                case "unit":
                    System.out.println("  Initializing for Unit Testing...");
                    config = createUnitTestConfig();
                    break;
                    
                case "integration":
                    System.out.println("  Initializing for Integration Testing...");
                    config = createIntegrationTestConfig();
                    break;
                    
                case "performance":
                    System.out.println("  Initializing for Performance Testing...");
                    config = createPerformanceTestConfig();
                    break;
                    
                default:
                    System.out.println("  Initializing for Default Testing...");
                    config = TestConfiguration.createMem0Config();
                    break;
            }
            
            System.out.println("✓ Test environment specific initialization completed");
            System.out.println("  - Environment: " + testEnv);
            System.out.println("  - LLM Provider: " + config.getLlm().getProvider());
            System.out.println("  - Embedding Provider: " + config.getEmbedding().getProvider());
            
        } catch (Exception e) {
            System.err.println("✗ Test environment initialization failed: " + e.getMessage());
        }
    }

    // Helper methods

    private static LLMProvider createCustomLLMProvider() {
        // For demo purposes, use rule-based provider
        return new RuleBasedLLMProvider();
    }

    private static EmbeddingProvider createCustomEmbeddingProvider() {
        // For demo purposes, use TF-IDF provider
        return new SimpleTFIDFEmbeddingProvider();
    }

    private static Mem0Config createUnitTestConfig() {
        Mem0Config config = new Mem0Config();
        config.getLlm().setProvider("rulebased");
        config.getEmbedding().setProvider("tfidf");
        config.getVectorStore().setProvider("inmemory");
        config.getGraphStore().setProvider("inmemory");
        return config;
    }

    private static Mem0Config createIntegrationTestConfig() {
        Mem0Config config = TestConfiguration.createMem0Config();
        // Use real providers but with test settings
        config.getLlm().setMaxTokens(500); // Reduced for faster tests
        // Use test-optimized embedding settings
        return config;
    }

    private static Mem0Config createPerformanceTestConfig() {
        Mem0Config config = TestConfiguration.createMem0Config();
        // Optimized settings for performance testing
        config.getLlm().setMaxTokens(2000);
        return config;
    }

    private static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    // Validation helper methods
    
    private static String validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        if (apiKey.equals("your-api-key") || apiKey.equals("your-embedding-key")) {
            System.out.println("    WARNING: Using placeholder API key - replace with real key in production");
        }
        return apiKey;
    }
    
    private static double validateTemperature(double temperature) {
        if (temperature < 0.0 || temperature > 2.0) {
            throw new IllegalArgumentException("Temperature must be between 0.0 and 2.0, got: " + temperature);
        }
        return temperature;
    }
    
    private static int validateMaxTokens(int maxTokens) {
        if (maxTokens <= 0 || maxTokens > 8192) {
            throw new IllegalArgumentException("Max tokens must be between 1 and 8192, got: " + maxTokens);
        }
        return maxTokens;
    }
    
    /**
     * Utility method to safely create and manage providers with resource cleanup
     */
    private static void withProviders(ProviderConsumer consumer) {
        LLMProvider llmProvider = null;
        EmbeddingProvider embeddingProvider = null;
        try {
            llmProvider = createCustomLLMProvider();
            embeddingProvider = createCustomEmbeddingProvider();
            consumer.accept(llmProvider, embeddingProvider);
        } catch (Exception e) {
            System.err.println("✗ Custom provider operation failed: " + e.getMessage());
        } finally {
            // Cleanup resources
            if (llmProvider != null) {
                try {
                    llmProvider.close();
                } catch (Exception e) {
                    System.err.println("Warning: Failed to close LLM provider: " + e.getMessage());
                }
            }
            if (embeddingProvider != null) {
                try {
                    embeddingProvider.close();
                } catch (Exception e) {
                    System.err.println("Warning: Failed to close embedding provider: " + e.getMessage());
                }
            }
        }
    }
    
    @FunctionalInterface
    private interface ProviderConsumer {
        void accept(LLMProvider llm, EmbeddingProvider embedding) throws Exception;
    }

    /**
     * Utility method to print configuration details
     */
    public static void printConfigurationDetails(Mem0Config config) {
        System.out.println("Configuration Details:");
        System.out.println("  LLM:");
        System.out.println("    Provider: " + config.getLlm().getProvider());
        System.out.println("    Model: " + config.getLlm().getModel());
        System.out.println("    Temperature: " + config.getLlm().getTemperature());
        System.out.println("    Max Tokens: " + config.getLlm().getMaxTokens());
        
        System.out.println("  Embedding:");
        System.out.println("    Provider: " + config.getEmbedding().getProvider());
        System.out.println("    Model: " + config.getEmbedding().getModel());
        System.out.println("    API Key: " + (config.getEmbedding().getApiKey() != null ? "Set" : "Not set"));
        
        System.out.println("  Vector Store:");
        System.out.println("    Provider: " + config.getVectorStore().getProvider());
        
        System.out.println("  Graph Store:");
        System.out.println("    Provider: " + config.getGraphStore().getProvider());
    }
}