package com.mem0.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ConfigurationManager - 统一配置管理器
 * 
 * 集中管理所有模块的配置，提供统一的配置访问接口。
 * 支持配置热重载、配置验证、配置监听等功能。
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 2024-12-20
 */
public class ConfigurationManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);
    
    // 单例实例
    private static volatile ConfigurationManager instance;
    
    // 配置实例缓存
    private final Map<Class<? extends BaseConfiguration>, BaseConfiguration> configurationCache;
    
    // 配置监听器
    private final Map<String, ConfigurationListener> listeners;
    
    // 全局配置属性
    private final Map<String, Object> globalProperties;
    
    /**
     * 配置监听器接口
     */
    public interface ConfigurationListener {
        /**
         * 配置更新时的回调
         * 
         * @param configKey 配置键
         * @param oldValue 旧值
         * @param newValue 新值
         */
        void onConfigurationChanged(String configKey, Object oldValue, Object newValue);
    }
    
    /**
     * 私有构造函数
     */
    private ConfigurationManager() {
        this.configurationCache = new ConcurrentHashMap<>();
        this.listeners = new ConcurrentHashMap<>();
        this.globalProperties = new ConcurrentHashMap<>();
        
        // 初始化全局配置
        initializeGlobalConfiguration();
        
        logger.info("ConfigurationManager initialized");
    }
    
    /**
     * 获取单例实例
     * 
     * @return ConfigurationManager实例
     */
    public static ConfigurationManager getInstance() {
        if (instance == null) {
            synchronized (ConfigurationManager.class) {
                if (instance == null) {
                    instance = new ConfigurationManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 初始化全局配置
     */
    private void initializeGlobalConfiguration() {
        // 应用基本信息
        globalProperties.put("app.name", "mem0-java");
        globalProperties.put("app.version", "1.0.0");
        globalProperties.put("app.description", "Java implementation of mem0 memory framework");
        
        // 环境配置
        globalProperties.put("env.profile", getSystemProperty("mem0.profile", "development"));
        globalProperties.put("env.debug", getSystemProperty("mem0.debug", "false"));
        
        // 日志配置
        globalProperties.put("logging.level", getSystemProperty("mem0.logging.level", "INFO"));
        globalProperties.put("logging.enableConsole", getSystemProperty("mem0.logging.console", "true"));
        globalProperties.put("logging.enableFile", getSystemProperty("mem0.logging.file", "false"));
        globalProperties.put("logging.filePath", getSystemProperty("mem0.logging.path", "./logs/mem0.log"));
        
        // 数据库配置
        globalProperties.put("database.driver", getSystemProperty("mem0.db.driver", ""));
        globalProperties.put("database.url", getSystemProperty("mem0.db.url", ""));
        globalProperties.put("database.username", getSystemProperty("mem0.db.username", ""));
        globalProperties.put("database.password", getSystemProperty("mem0.db.password", ""));
        globalProperties.put("database.maxPoolSize", getSystemProperty("mem0.db.maxPoolSize", "10"));
        
        // 缓存配置
        globalProperties.put("cache.provider", getSystemProperty("mem0.cache.provider", "memory"));
        globalProperties.put("cache.defaultTTL", getSystemProperty("mem0.cache.ttl", "3600"));
        globalProperties.put("cache.maxSize", getSystemProperty("mem0.cache.maxSize", "10000"));
        
        // 安全配置
        globalProperties.put("security.enableAuthentication", getSystemProperty("mem0.security.auth", "false"));
        globalProperties.put("security.enableAuthorization", getSystemProperty("mem0.security.authz", "false"));
        globalProperties.put("security.enableEncryption", getSystemProperty("mem0.security.encryption", "false"));
        
        // 监控配置
        globalProperties.put("monitoring.enableMetrics", getSystemProperty("mem0.monitoring.metrics", "true"));
        globalProperties.put("monitoring.enableHealthCheck", getSystemProperty("mem0.monitoring.health", "true"));
        globalProperties.put("monitoring.metricsPort", getSystemProperty("mem0.monitoring.port", "8080"));
    }
    
    /**
     * 获取系统属性或环境变量
     * 
     * @param key 属性键
     * @param defaultValue 默认值
     * @return 属性值
     */
    private String getSystemProperty(String key, String defaultValue) {
        // 首先检查系统属性
        String value = System.getProperty(key);
        if (value != null) {
            return value;
        }
        
        // 然后检查环境变量
        String envKey = key.toUpperCase().replace(".", "_");
        value = System.getenv(envKey);
        if (value != null) {
            return value;
        }
        
        return defaultValue;
    }
    
    /**
     * 获取指定类型的配置实例
     * 
     * @param configClass 配置类
     * @param <T> 配置类型
     * @return 配置实例
     */
    @SuppressWarnings("unchecked")
    public <T extends BaseConfiguration> T getConfiguration(Class<T> configClass) {
        return (T) configurationCache.computeIfAbsent(configClass, this::createConfiguration);
    }
    
    /**
     * 创建配置实例
     * 
     * @param configClass 配置类
     * @return 配置实例
     */
    private BaseConfiguration createConfiguration(Class<? extends BaseConfiguration> configClass) {
        try {
            BaseConfiguration config = configClass.getDeclaredConstructor().newInstance();
            
            // 自动加载配置
            loadConfiguration(config);
            
            logger.info("Created configuration instance: {}", configClass.getSimpleName());
            return config;
            
        } catch (Exception e) {
            logger.error("Failed to create configuration instance: {}", configClass.getSimpleName(), e);
            throw new RuntimeException("Failed to create configuration: " + configClass.getSimpleName(), e);
        }
    }
    
    /**
     * 加载配置
     * 
     * @param config 配置实例
     */
    private void loadConfiguration(BaseConfiguration config) {
        String configPrefix = config.getConfigPrefix();
        
        // 尝试从不同来源加载配置
        try {
            // 1. 从properties文件加载
            String propertiesFile = configPrefix.replace(".", "/") + ".properties";
            config.loadFromProperties(propertiesFile);
            
            // 2. 从环境变量加载
            config.loadFromEnvironment();
            
            // 3. 从系统属性加载
            config.loadFromSystemProperties();
            
            logger.debug("Loaded configuration for: {}", configPrefix);
            
        } catch (Exception e) {
            logger.warn("Error loading configuration for {}: {}", configPrefix, e.getMessage());
        }
    }
    
    /**
     * 获取分层内存配置
     * 
     * @return HierarchyConfiguration实例
     */
    public HierarchyConfiguration getHierarchyConfiguration() {
        return getConfiguration(HierarchyConfiguration.class);
    }
    
    /**
     * 获取搜索系统配置
     * 
     * @return SearchConfiguration实例
     */
    public SearchConfiguration getSearchConfiguration() {
        return getConfiguration(SearchConfiguration.class);
    }
    
    /**
     * 获取多模态配置
     * 
     * @return MultimodalConfiguration实例
     */
    public MultimodalConfiguration getMultimodalConfiguration() {
        return getConfiguration(MultimodalConfiguration.class);
    }
    
    /**
     * 获取AI功能配置
     * 
     * @return AIConfiguration实例
     */
    public AIConfiguration getAIConfiguration() {
        return getConfiguration(AIConfiguration.class);
    }
    
    /**
     * 获取全局属性
     * 
     * @param key 属性键
     * @param defaultValue 默认值
     * @param <T> 值类型
     * @return 属性值
     */
    @SuppressWarnings("unchecked")
    public <T> T getGlobalProperty(String key, T defaultValue) {
        Object value = globalProperties.get(key);
        return value != null ? (T) value : defaultValue;
    }
    
    /**
     * 设置全局属性
     * 
     * @param key 属性键
     * @param value 属性值
     */
    public void setGlobalProperty(String key, Object value) {
        Object oldValue = globalProperties.put(key, value);
        
        // 通知监听器
        notifyListeners(key, oldValue, value);
        
        logger.debug("Set global property: {} = {}", key, value);
    }
    
    /**
     * 重新加载所有配置
     */
    public void reloadConfigurations() {
        logger.info("Reloading all configurations...");
        
        for (BaseConfiguration config : configurationCache.values()) {
            try {
                // 清除现有配置
                config.clearConfiguration();
                
                // 重新加载配置
                loadConfiguration(config);
                
                logger.info("Reloaded configuration: {}", config.getConfigPrefix());
                
            } catch (Exception e) {
                logger.error("Failed to reload configuration: {}", config.getConfigPrefix(), e);
            }
        }
        
        // 重新初始化全局配置
        initializeGlobalConfiguration();
        
        logger.info("Configuration reload completed");
    }
    
    /**
     * 重新加载指定配置
     * 
     * @param configClass 配置类
     */
    public void reloadConfiguration(Class<? extends BaseConfiguration> configClass) {
        BaseConfiguration config = configurationCache.get(configClass);
        if (config != null) {
            try {
                config.clearConfiguration();
                loadConfiguration(config);
                logger.info("Reloaded configuration: {}", config.getConfigPrefix());
            } catch (Exception e) {
                logger.error("Failed to reload configuration: {}", config.getConfigPrefix(), e);
            }
        }
    }
    
    /**
     * 添加配置监听器
     * 
     * @param key 配置键（支持通配符）
     * @param listener 监听器
     */
    public void addConfigurationListener(String key, ConfigurationListener listener) {
        listeners.put(key, listener);
        logger.debug("Added configuration listener for key: {}", key);
    }
    
    /**
     * 移除配置监听器
     * 
     * @param key 配置键
     */
    public void removeConfigurationListener(String key) {
        listeners.remove(key);
        logger.debug("Removed configuration listener for key: {}", key);
    }
    
    /**
     * 通知监听器
     * 
     * @param key 配置键
     * @param oldValue 旧值
     * @param newValue 新值
     */
    private void notifyListeners(String key, Object oldValue, Object newValue) {
        for (Map.Entry<String, ConfigurationListener> entry : listeners.entrySet()) {
            String listenerKey = entry.getKey();
            ConfigurationListener listener = entry.getValue();
            
            // 支持通配符匹配
            if (key.matches(listenerKey.replace("*", ".*"))) {
                try {
                    listener.onConfigurationChanged(key, oldValue, newValue);
                } catch (Exception e) {
                    logger.error("Error notifying configuration listener for key: {}", key, e);
                }
            }
        }
    }
    
    /**
     * 获取所有配置的摘要信息
     * 
     * @return 配置摘要
     */
    public Map<String, Object> getConfigurationSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        // 全局属性
        summary.put("global", new HashMap<>(globalProperties));
        
        // 各模块配置
        for (Map.Entry<Class<? extends BaseConfiguration>, BaseConfiguration> entry : configurationCache.entrySet()) {
            String moduleName = entry.getKey().getSimpleName().replace("Configuration", "").toLowerCase();
            BaseConfiguration config = entry.getValue();
            
            Map<String, Object> moduleConfig = new HashMap<>();
            moduleConfig.put("configPrefix", config.getConfigPrefix());
            moduleConfig.put("configCount", config.getAllConfigValues().size());
            moduleConfig.put("className", entry.getKey().getSimpleName());
            
            summary.put(moduleName, moduleConfig);
        }
        
        // 统计信息
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalModules", configurationCache.size());
        stats.put("totalListeners", listeners.size());
        stats.put("globalProperties", globalProperties.size());
        summary.put("statistics", stats);
        
        return summary;
    }
    
    /**
     * 验证所有配置
     * 
     * @return 验证结果
     */
    public Map<String, String> validateAllConfigurations() {
        Map<String, String> validationResults = new HashMap<>();
        
        for (Map.Entry<Class<? extends BaseConfiguration>, BaseConfiguration> entry : configurationCache.entrySet()) {
            String moduleName = entry.getKey().getSimpleName();
            BaseConfiguration config = entry.getValue();
            
            try {
                config.validateConfiguration();
                validationResults.put(moduleName, "Valid");
            } catch (Exception e) {
                validationResults.put(moduleName, "Invalid: " + e.getMessage());
                logger.error("Configuration validation failed for {}: {}", moduleName, e.getMessage());
            }
        }
        
        return validationResults;
    }
    
    /**
     * 导出所有配置到Properties格式
     * 
     * @return Properties格式的配置内容
     */
    public String exportToProperties() {
        StringBuilder sb = new StringBuilder();
        
        // 添加头部注释
        sb.append("# mem0-java Configuration Export\n");
        sb.append("# Generated at: ").append(java.time.LocalDateTime.now()).append("\n\n");
        
        // 全局配置
        sb.append("# Global Properties\n");
        for (Map.Entry<String, Object> entry : globalProperties.entrySet()) {
            sb.append("mem0.global.").append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
        }
        sb.append("\n");
        
        // 各模块配置
        for (BaseConfiguration config : configurationCache.values()) {
            sb.append("# ").append(config.getClass().getSimpleName()).append("\n");
            
            for (Map.Entry<String, Object> entry : config.getAllConfigValues().entrySet()) {
                sb.append(config.getConfigPrefix()).append(".").append(entry.getKey())
                  .append("=").append(entry.getValue()).append("\n");
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 清理资源
     */
    public void shutdown() {
        logger.info("Shutting down ConfigurationManager...");
        
        configurationCache.clear();
        listeners.clear();
        globalProperties.clear();
        
        logger.info("ConfigurationManager shutdown completed");
    }
    
    // 便利方法
    
    /**
     * 检查调试模式是否开启
     * 
     * @return 是否开启调试模式
     */
    public boolean isDebugEnabled() {
        return Boolean.parseBoolean(getGlobalProperty("env.debug", "false"));
    }
    
    /**
     * 获取应用环境
     * 
     * @return 环境名称
     */
    public String getEnvironment() {
        return getGlobalProperty("env.profile", "development");
    }
    
    /**
     * 检查是否为生产环境
     * 
     * @return 是否为生产环境
     */
    public boolean isProductionEnvironment() {
        return "production".equalsIgnoreCase(getEnvironment());
    }
    
    /**
     * 检查是否为开发环境
     * 
     * @return 是否为开发环境
     */
    public boolean isDevelopmentEnvironment() {
        return "development".equalsIgnoreCase(getEnvironment());
    }
}