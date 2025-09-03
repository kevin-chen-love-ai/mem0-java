package com.mem0.security;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Secure string implementation for storing sensitive data like API keys, passwords, etc.
 * This class provides basic protection against memory dumps and accidental logging.
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public final class SecureString implements AutoCloseable {
    
    private volatile char[] value;
    private volatile boolean cleared = false;
    
    /**
     * Create a SecureString from a regular string
     * 
     * @param plaintext the sensitive string to protect
     */
    public SecureString(String plaintext) {
        if (plaintext == null) {
            this.value = new char[0];
        } else {
            this.value = plaintext.toCharArray();
        }
    }
    
    /**
     * Create a SecureString from a char array
     * 
     * @param value the sensitive char array to protect
     */
    public SecureString(char[] value) {
        if (value == null) {
            this.value = new char[0];
        } else {
            this.value = Arrays.copyOf(value, value.length);
        }
    }
    
    /**
     * Get the value as a string (use with caution)
     * 
     * @return the plain text value
     * @throws IllegalStateException if the value has been cleared
     */
    public String getValue() {
        checkCleared();
        return new String(value);
    }
    
    /**
     * Get the value as a char array copy
     * 
     * @return copy of the char array
     * @throws IllegalStateException if the value has been cleared
     */
    public char[] getCharArray() {
        checkCleared();
        return Arrays.copyOf(value, value.length);
    }
    
    /**
     * Check if the value is empty
     * 
     * @return true if empty or cleared
     */
    public boolean isEmpty() {
        return cleared || value == null || value.length == 0;
    }
    
    /**
     * Check if the value has been cleared
     * 
     * @return true if cleared
     */
    public boolean isCleared() {
        return cleared;
    }
    
    /**
     * Clear the sensitive data from memory
     */
    public void clear() {
        if (!cleared && value != null) {
            Arrays.fill(value, '\0');
            cleared = true;
        }
    }
    
    @Override
    public void close() {
        clear();
    }
    
    @Override
    public String toString() {
        if (cleared) {
            return "SecureString[CLEARED]";
        }
        if (value == null || value.length == 0) {
            return "SecureString[EMPTY]";
        }
        // Never expose the actual value in toString()
        return "SecureString[length=" + value.length + ", hash=" + System.identityHashCode(this) + "]";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SecureString)) return false;
        
        SecureString other = (SecureString) obj;
        if (this.cleared != other.cleared) return false;
        if (this.cleared) return true; // Both cleared
        
        return Arrays.equals(this.value, other.value);
    }
    
    @Override
    public int hashCode() {
        if (cleared || value == null) return 0;
        return Arrays.hashCode(value);
    }
    
    @Override
    protected void finalize() throws Throwable {
        try {
            clear();
        } finally {
            super.finalize();
        }
    }
    
    private void checkCleared() {
        if (cleared) {
            throw new IllegalStateException("SecureString has been cleared and cannot be used");
        }
    }
}