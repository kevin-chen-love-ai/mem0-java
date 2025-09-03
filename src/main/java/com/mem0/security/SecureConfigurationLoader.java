package com.mem0.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Secure configuration loader that handles sensitive configuration data properly
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class SecureConfigurationLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(SecureConfigurationLoader.class);
    
    private final Map<String, SecureString> secureProperties = new HashMap<>();
    private final Map<String, String> regularProperties = new HashMap<>();
    
    // Properties that should be treated as sensitive
    private static final String[] SENSITIVE_KEYS = {
        "api.key", "apikey", "password", "secret", "token", "credential",
        "openai.api.key", "aliyun.access.key", "qwen.api.key",
        "neo4j.password", "milvus.token", "database.password"
    };
    
    /**
     * Load configuration from properties file
     * 
     * @param resourcePath path to properties file
     * @throws IOException if unable to load configuration
     */
    public void loadFromProperties(String resourcePath) throws IOException {
        Properties props = new Properties();
        
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Configuration file not found: " + resourcePath);
            }
            
            props.load(is);
            
            for (String key : props.stringPropertyNames()) {
                String value = props.getProperty(key);
                
                if (isSensitiveKey(key)) {
                    secureProperties.put(key, new SecureString(value));
                    logger.debug("Loaded secure property: {} = [MASKED]", key);
                } else {
                    regularProperties.put(key, value);
                    logger.debug("Loaded property: {} = {}", key, value);
                }
            }
            
            logger.info("Configuration loaded from {}: {} properties ({} secure)", 
                       resourcePath, props.size(), secureProperties.size());
        }
    }
    
    /**
     * Load configuration from environment variables
     */
    public void loadFromEnvironment() {
        Map<String, String> env = System.getenv();
        
        for (Map.Entry<String, String> entry : env.entrySet()) {
            String key = entry.getKey().toLowerCase().replace('_', '.');
            String value = entry.getValue();
            
            // Only load mem0-related environment variables
            if (key.startsWith("mem0.") || key.startsWith("openai.") || 
                key.startsWith("aliyun.") || key.startsWith("qwen.")) {
                
                if (isSensitiveKey(key)) {
                    secureProperties.put(key, new SecureString(value));
                    logger.debug("Loaded secure environment variable: {} = [MASKED]", key);
                } else {
                    regularProperties.put(key, value);
                    logger.debug("Loaded environment variable: {} = {}", key, value);
                }
            }
        }
    }
    
    /**
     * Get a regular (non-sensitive) property
     * 
     * @param key property key
     * @return property value or null if not found
     */
    public String getProperty(String key) {
        return regularProperties.get(key);
    }
    
    /**
     * Get a regular property with default value
     * 
     * @param key property key
     * @param defaultValue default value if not found
     * @return property value or default
     */
    public String getProperty(String key, String defaultValue) {
        return regularProperties.getOrDefault(key, defaultValue);
    }
    
    /**
     * Get a secure property
     * 
     * @param key property key
     * @return secure property or null if not found
     */
    public SecureString getSecureProperty(String key) {
        return secureProperties.get(key);
    }
    
    /**
     * Check if a property exists
     * 
     * @param key property key
     * @return true if property exists
     */
    public boolean hasProperty(String key) {
        return regularProperties.containsKey(key) || secureProperties.containsKey(key);
    }
    
    /**
     * Get all non-sensitive property keys
     * 
     * @return set of property keys
     */
    public java.util.Set<String> getPropertyKeys() {
        return regularProperties.keySet();
    }
    
    /**
     * Clear all configuration data
     */
    public void clear() {
        regularProperties.clear();
        
        // Clear secure properties
        for (SecureString secureString : secureProperties.values()) {
            secureString.close();
        }
        secureProperties.clear();
        
        logger.debug("Configuration cleared");
    }
    
    /**
     * Get configuration summary for logging (without sensitive data)
     * 
     * @return configuration summary
     */
    public String getConfigurationSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Configuration Summary:\n");
        sb.append("  Regular properties: ").append(regularProperties.size()).append("\n");
        sb.append("  Secure properties: ").append(secureProperties.size()).append("\n");
        
        for (String key : regularProperties.keySet()) {
            sb.append("  ").append(key).append(" = ").append(regularProperties.get(key)).append("\n");
        }
        
        for (String key : secureProperties.keySet()) {
            sb.append("  ").append(key).append(" = [SECURE]\n");
        }
        
        return sb.toString();
    }
    
    private boolean isSensitiveKey(String key) {
        String lowerKey = key.toLowerCase();
        for (String sensitiveKey : SENSITIVE_KEYS) {
            if (lowerKey.contains(sensitiveKey)) {
                return true;
            }
        }
        return false;
    }
}