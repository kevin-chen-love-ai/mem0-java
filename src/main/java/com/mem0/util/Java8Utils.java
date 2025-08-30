package com.mem0.util;

import java.util.*;

/**
 * Java 8兼容性工具类
 * Java 8 Compatibility Utility Class
 * 
 * 该类提供Java 9+版本中引入的便利方法在Java 8环境下的兼容实现，确保代码在
 * Java 8环境中能够正常运行，同时保持与新版本Java API的一致性。
 * 
 * This class provides Java 8-compatible implementations of convenience methods
 * introduced in Java 9+ versions, ensuring code compatibility in Java 8 environments
 * while maintaining consistency with newer Java API versions.
 * 
 * 主要功能 / Key Features:
 * • Map.of方法的Java 8实现 / Java 8 implementation of Map.of methods
 * • Set.of方法的Java 8实现 / Java 8 implementation of Set.of methods
 * • String.repeat方法的Java 8实现 / Java 8 implementation of String.repeat method
 * • 不可变集合创建 / Immutable collection creation
 * • 向后兼容性保证 / Backward compatibility guarantee
 * • 零依赖实现 / Zero-dependency implementation
 * 
 * 使用示例 / Usage Example:
 * <pre>{@code
 * // 创建不可变Map / Create immutable Map
 * Map<String, String> config = Java8Utils.mapOf(
 *     "host", "localhost",
 *     "port", "8080",
 *     "protocol", "https"
 * );
 * 
 * // 创建不可变Set / Create immutable Set
 * Set<String> supportedFormats = Java8Utils.setOf("json", "xml", "yaml");
 * 
 * // 字符串重复 / String repetition
 * String separator = Java8Utils.repeat("-", 50);
 * String padding = Java8Utils.repeat(" ", 10);
 * 
 * // 在配置中使用 / Usage in configuration
 * Map<String, Object> defaultConfig = Java8Utils.mapOf(
 *     "timeout", 30000,
 *     "retries", 3,
 *     "enableCache", true,
 *     "cacheSize", 1000
 * );
 * }</pre>
 * 
 * 性能注意事项 / Performance Notes:
 * <pre>{@code
 * // 推荐：少量元素时使用 / Recommended: Use for small collections
 * Map<String, String> small = Java8Utils.mapOf("key", "value");
 * 
 * // 不推荐：大量元素时使用传统方式 / Not recommended: Use traditional way for large collections
 * Map<String, String> large = new HashMap<>();
 * large.put("key1", "value1");
 * large.put("key2", "value2");
 * // ... more entries
 * large = Collections.unmodifiableMap(large);
 * }</pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public final class Java8Utils {
    
    private Java8Utils() {
        // 工具类不允许实例化
    }
    
    /**
     * 创建包含2个键值对的不可变Map (替代Map.of)
     */
    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2) {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return Collections.unmodifiableMap(map);
    }
    
    /**
     * 创建包含3个键值对的不可变Map
     */
    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3) {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return Collections.unmodifiableMap(map);
    }
    
    /**
     * 创建包含4个键值对的不可变Map
     */
    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        return Collections.unmodifiableMap(map);
    }
    
    /**
     * 创建不可变Set (替代Set.of)
     */
    @SafeVarargs
    public static <T> Set<T> setOf(T... elements) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(elements)));
    }
    
    /**
     * 字符串重复 (替代String.repeat)
     */
    public static String repeat(String str, int count) {
        if (count <= 0) return "";
        if (count == 1) return str;
        
        StringBuilder sb = new StringBuilder(str.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}