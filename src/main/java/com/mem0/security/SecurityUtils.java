package com.mem0.security;

import java.security.SecureRandom;
import java.util.regex.Pattern;

/**
 * Security utilities for Mem0 operations
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public final class SecurityUtils {
    
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
    // Pattern to detect potential API keys and tokens in strings
    private static final Pattern API_KEY_PATTERN = Pattern.compile(
        "(?i)(api[_-]?key|token|secret|password|credential)\\s*[=:]\\s*['\"]?([\\w\\-\\.]{20,})['\"]?",
        Pattern.CASE_INSENSITIVE
    );
    
    // Prevent instantiation
    private SecurityUtils() {}
    
    /**
     * Sanitize a string for logging by masking potential sensitive data
     * 
     * @param input the string to sanitize
     * @return sanitized string with sensitive data masked
     */
    public static String sanitizeForLogging(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        // Mask potential API keys and tokens
        return API_KEY_PATTERN.matcher(input).replaceAll("$1=***MASKED***");
    }
    
    /**
     * Create a masked version of a sensitive string
     * 
     * @param sensitive the sensitive string
     * @return masked string showing only first and last few characters
     */
    public static String mask(String sensitive) {
        if (sensitive == null || sensitive.length() <= 8) {
            return "***";
        }
        
        int visibleChars = Math.min(3, sensitive.length() / 4);
        String prefix = sensitive.substring(0, visibleChars);
        String suffix = sensitive.substring(sensitive.length() - visibleChars);
        
        return prefix + "***" + suffix;
    }
    
    /**
     * Validate that a string looks like a valid API key
     * 
     * @param apiKey the API key to validate
     * @return true if it appears to be a valid API key format
     */
    public static boolean isValidApiKeyFormat(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = apiKey.trim();
        
        // Basic validation: should be at least 20 characters and contain alphanumeric characters
        return trimmed.length() >= 20 && 
               trimmed.length() <= 256 && 
               trimmed.matches("^[\\w\\-\\.]+$");
    }
    
    /**
     * Generate a secure random string for use as IDs or nonces
     * 
     * @param length the desired length
     * @return secure random string
     */
    public static String generateSecureRandomString(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be positive");
        }
        
        StringBuilder sb = new StringBuilder(length);
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        
        return sb.toString();
    }
    
    /**
     * Clear an array of sensitive data
     * 
     * @param data the data to clear
     */
    public static void clearSensitiveData(char[] data) {
        if (data != null) {
            java.util.Arrays.fill(data, '\0');
        }
    }
    
    /**
     * Clear an array of sensitive data
     * 
     * @param data the data to clear
     */
    public static void clearSensitiveData(byte[] data) {
        if (data != null) {
            java.util.Arrays.fill(data, (byte) 0);
        }
    }
    
    /**
     * Validate user input to prevent injection attacks
     * 
     * @param input the user input to validate
     * @param fieldName the name of the field for error messages
     * @throws IllegalArgumentException if input is invalid
     */
    public static void validateUserInput(String input, String fieldName) {
        if (input == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
        
        // Check for common injection patterns
        String[] suspiciousPatterns = {
            "<script", "javascript:", "vbscript:", "onload=", "onerror=",
            "eval(", "document.", "window.", "alert(", "prompt(",
            "<!--", "-->", "${", "#{", "%{",
            "union select", "drop table", "delete from", "insert into"
        };
        
        String lowerInput = input.toLowerCase();
        for (String pattern : suspiciousPatterns) {
            if (lowerInput.contains(pattern)) {
                throw new IllegalArgumentException(fieldName + " contains potentially unsafe content");
            }
        }
        
        // Check for excessive length
        if (input.length() > 10000) {
            throw new IllegalArgumentException(fieldName + " is too long (max 10000 characters)");
        }
    }
}