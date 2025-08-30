package com.mem0.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * BaseConfiguration - 配置管理基类
 * 
 * 提供配置加载、验证、更新的基础功能。
 * 支持从多种数据源加载配置：Properties文件、JSON文件、环境变量等。
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 2024-12-20
 */
public abstract class BaseConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(BaseConfiguration.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    protected final Map<String, Object> configValues = new HashMap<>();
    protected final String configPrefix;
    
    /**
     * 构造函数
     * 
     * @param configPrefix 配置前缀
     */
    protected BaseConfiguration(String configPrefix) {
        this.configPrefix = configPrefix;
        loadDefaultConfiguration();
    }
    
    /**
     * 加载默认配置
     * 子类需要实现此方法来设置默认值
     */
    protected abstract void loadDefaultConfiguration();
    
    /**
     * 验证配置
     * 子类可以覆盖此方法来进行特定的配置验证
     * 
     * @throws IllegalArgumentException 配置无效时抛出
     */
    protected void validateConfiguration() throws IllegalArgumentException {
        // 基类默认不进行验证
    }
    
    /**
     * 从Properties文件加载配置
     * 
     * @param propertiesPath Properties文件路径
     */
    public void loadFromProperties(String propertiesPath) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(propertiesPath)) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                
                for (String key : props.stringPropertyNames()) {
                    if (key.startsWith(configPrefix + ".")) {
                        String configKey = key.substring((configPrefix + ".").length());
                        String value = props.getProperty(key);
                        setConfigValue(configKey, parseValue(value));
                    }
                }
                
                logger.info("Loaded configuration from properties: {}", propertiesPath);
                validateConfiguration();
            }
        } catch (IOException e) {
            logger.warn("Failed to load configuration from properties: {}", propertiesPath, e);
        }
    }
    
    /**
     * 从环境变量加载配置
     */
    public void loadFromEnvironment() {
        String envPrefix = configPrefix.toUpperCase().replace(".", "_") + "_";
        
        System.getenv().forEach((key, value) -> {
            if (key.startsWith(envPrefix)) {
                String configKey = key.substring(envPrefix.length()).toLowerCase().replace("_", ".");
                setConfigValue(configKey, parseValue(value));
            }
        });
        
        logger.info("Loaded configuration from environment variables");
        validateConfiguration();
    }
    
    /**
     * 从系统属性加载配置
     */
    public void loadFromSystemProperties() {
        String propertyPrefix = configPrefix + ".";
        
        System.getProperties().forEach((key, value) -> {
            String keyStr = (String) key;
            if (keyStr.startsWith(propertyPrefix)) {
                String configKey = keyStr.substring(propertyPrefix.length());
                setConfigValue(configKey, parseValue((String) value));
            }
        });
        
        logger.info("Loaded configuration from system properties");
        validateConfiguration();
    }
    
    /**
     * 设置配置值
     * 
     * @param key 配置键
     * @param value 配置值
     */
    public void setConfigValue(String key, Object value) {
        configValues.put(key, value);
        logger.debug("Set config value: {} = {}", key, value);
    }
    
    /**
     * 获取配置值
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @param <T> 值类型
     * @return 配置值
     */
    @SuppressWarnings("unchecked")
    public <T> T getConfigValue(String key, T defaultValue) {
        Object value = configValues.get(key);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return (T) value;
        } catch (ClassCastException e) {
            logger.warn("Config value type mismatch for key {}, using default value", key);
            return defaultValue;
        }
    }
    
    /**
     * 获取字符串配置值
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public String getString(String key, String defaultValue) {
        return getConfigValue(key, defaultValue);
    }
    
    /**
     * 获取整数配置值
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public int getInt(String key, int defaultValue) {
        Object value = configValues.get(key);
        if (value == null) return defaultValue;
        
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid integer value for key {}: {}", key, value);
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    /**
     * 获取长整型配置值
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public long getLong(String key, long defaultValue) {
        Object value = configValues.get(key);
        if (value == null) return defaultValue;
        
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid long value for key {}: {}", key, value);
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    /**
     * 获取双精度配置值
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public double getDouble(String key, double defaultValue) {
        Object value = configValues.get(key);
        if (value == null) return defaultValue;
        
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid double value for key {}: {}", key, value);
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    /**
     * 获取布尔配置值
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = configValues.get(key);
        if (value == null) return defaultValue;
        
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }
    
    /**
     * 解析配置值
     * 
     * @param value 字符串值
     * @return 解析后的值
     */
    private Object parseValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }
        
        // 尝试解析为布尔值
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        
        // 尝试解析为数字
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                long longValue = Long.parseLong(value);
                if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
                    return (int) longValue;
                } else {
                    return longValue;
                }
            }
        } catch (NumberFormatException e) {
            // 不是数字，返回字符串
        }
        
        return value;
    }
    
    /**
     * 获取所有配置值
     * 
     * @return 配置值映射
     */
    public Map<String, Object> getAllConfigValues() {
        return new HashMap<>(configValues);
    }
    
    /**
     * 清除所有配置值
     */
    public void clearConfiguration() {
        configValues.clear();
        loadDefaultConfiguration();
        logger.info("Configuration cleared and reset to defaults");
    }
    
    /**
     * 获取配置前缀
     * 
     * @return 配置前缀
     */
    public String getConfigPrefix() {
        return configPrefix;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
               "configPrefix='" + configPrefix + '\'' +
               ", configCount=" + configValues.size() +
               '}';
    }
    
    // Alias methods for compatibility
    protected String getStringValue(String key, String defaultValue) {
        return getString(key, defaultValue);
    }
    
    protected int getIntValue(String key, int defaultValue) {
        return getInt(key, defaultValue);
    }
    
    protected double getDoubleValue(String key, double defaultValue) {
        return getDouble(key, defaultValue);
    }
    
    protected boolean getBooleanValue(String key, boolean defaultValue) {
        return getBoolean(key, defaultValue);
    }
    
    protected long getLongValue(String key, long defaultValue) {
        return getLong(key, defaultValue);
    }
}