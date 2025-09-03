package com.mem0.util;

import java.util.Map;
import java.util.function.Function;

/**
 * Type-safe utility for accessing Map values with proper null handling
 * 
 * Provides safe type casting and default value handling for Map operations,
 * reducing the risk of ClassCastException and NullPointerException.
 */
public final class SafeMapAccessor {
    
    private SafeMapAccessor() {
        // Utility class
    }
    
    /**
     * Safely get String value from map with null check
     */
    public static String getString(Map<String, Object> map, String key) {
        return getString(map, key, null);
    }
    
    /**
     * Safely get String value from map with default value
     */
    public static String getString(Map<String, Object> map, String key, String defaultValue) {
        if (map == null || key == null) {
            return defaultValue;
        }
        
        Object value = map.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        
        // Convert to string if possible
        if (value != null) {
            return String.valueOf(value);
        }
        
        return defaultValue;
    }
    
    /**
     * Safely get Integer value from map
     */
    public static Integer getInteger(Map<String, Object> map, String key) {
        return getInteger(map, key, null);
    }
    
    /**
     * Safely get Integer value from map with default value
     */
    public static Integer getInteger(Map<String, Object> map, String key, Integer defaultValue) {
        if (map == null || key == null) {
            return defaultValue;
        }
        
        Object value = map.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        
        return defaultValue;
    }
    
    /**
     * Safely get Double value from map
     */
    public static Double getDouble(Map<String, Object> map, String key) {
        return getDouble(map, key, null);
    }
    
    /**
     * Safely get Double value from map with default value
     */
    public static Double getDouble(Map<String, Object> map, String key, Double defaultValue) {
        if (map == null || key == null) {
            return defaultValue;
        }
        
        Object value = map.get(key);
        if (value instanceof Double) {
            return (Double) value;
        }
        
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        
        return defaultValue;
    }
    
    /**
     * Safely get Boolean value from map
     */
    public static Boolean getBoolean(Map<String, Object> map, String key) {
        return getBoolean(map, key, null);
    }
    
    /**
     * Safely get Boolean value from map with default value
     */
    public static Boolean getBoolean(Map<String, Object> map, String key, Boolean defaultValue) {
        if (map == null || key == null) {
            return defaultValue;
        }
        
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        
        if (value instanceof String) {
            String strValue = (String) value;
            if ("true".equalsIgnoreCase(strValue) || "yes".equalsIgnoreCase(strValue) || "1".equals(strValue)) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(strValue) || "no".equalsIgnoreCase(strValue) || "0".equals(strValue)) {
                return Boolean.FALSE;
            }
        }
        
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        
        return defaultValue;
    }
    
    /**
     * Safely get value with custom converter
     */
    public static <T> T getValue(Map<String, Object> map, String key, Function<Object, T> converter, T defaultValue) {
        if (map == null || key == null || converter == null) {
            return defaultValue;
        }
        
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return converter.apply(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * Check if map contains non-null value for key
     */
    public static boolean hasValue(Map<String, Object> map, String key) {
        return map != null && key != null && map.containsKey(key) && map.get(key) != null;
    }
    
    /**
     * Get value with type check
     */
    @SuppressWarnings("unchecked")
    public static <T> T getValueOfType(Map<String, Object> map, String key, Class<T> expectedType) {
        return getValueOfType(map, key, expectedType, null);
    }
    
    /**
     * Get value with type check and default value
     */
    @SuppressWarnings("unchecked")
    public static <T> T getValueOfType(Map<String, Object> map, String key, Class<T> expectedType, T defaultValue) {
        if (map == null || key == null || expectedType == null) {
            return defaultValue;
        }
        
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        
        if (expectedType.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        
        return defaultValue;
    }
}