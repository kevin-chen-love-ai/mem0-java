package com.mem0.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * ConfigurationFactory - 配置工厂类
 * 
 * 提供便捷的配置创建和管理功能，支持配置预设、环境适配等功能。
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 2024-12-20
 */
public class ConfigurationFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationFactory.class);
    
    // 预设配置缓存
    private static final ConcurrentMap<String, ConfigurationPreset> presets = new ConcurrentHashMap<>();
    
    // 环境适配器缓存
    private static final ConcurrentMap<String, EnvironmentAdapter> adapters = new ConcurrentHashMap<>();
    
    /**
     * 配置预设接口
     */
    public interface ConfigurationPreset {
        /**
         * 应用预设配置
         * 
         * @param config 配置实例
         */
        void applyPreset(BaseConfiguration config);
        
        /**
         * 获取预设名称
         * 
         * @return 预设名称
         */
        String getPresetName();
        
        /**
         * 获取预设描述
         * 
         * @return 预设描述
         */
        String getDescription();
    }
    
    /**
     * 环境适配器接口
     */
    public interface EnvironmentAdapter {
        /**
         * 适配环境配置
         * 
         * @param config 配置实例
         * @param environment 环境名称
         */
        void adaptForEnvironment(BaseConfiguration config, String environment);
        
        /**
         * 检查是否支持该环境
         * 
         * @param environment 环境名称
         * @return 是否支持
         */
        boolean supportsEnvironment(String environment);
    }
    
    static {
        // 注册默认预设
        registerDefaultPresets();
        
        // 注册默认环境适配器
        registerDefaultAdapters();
    }
    
    /**
     * 注册默认预设配置
     */
    private static void registerDefaultPresets() {
        // 开发环境预设
        registerPreset(new DevelopmentPreset());
        
        // 测试环境预设
        registerPreset(new TestPreset());
        
        // 生产环境预设
        registerPreset(new ProductionPreset());
        
        // 高性能预设
        registerPreset(new HighPerformancePreset());
        
        // 低资源预设
        registerPreset(new LowResourcePreset());
    }
    
    /**
     * 注册默认环境适配器
     */
    private static void registerDefaultAdapters() {
        // 通用环境适配器
        registerAdapter(new GeneralEnvironmentAdapter());
        
        // 云环境适配器
        registerAdapter(new CloudEnvironmentAdapter());
        
        // 容器环境适配器
        registerAdapter(new ContainerEnvironmentAdapter());
    }
    
    /**
     * 创建配置实例
     * 
     * @param configClass 配置类
     * @param <T> 配置类型
     * @return 配置实例
     */
    public static <T extends BaseConfiguration> T createConfiguration(Class<T> configClass) {
        return createConfiguration(configClass, null, null);
    }
    
    /**
     * 创建配置实例并应用预设
     * 
     * @param configClass 配置类
     * @param presetName 预设名称
     * @param <T> 配置类型
     * @return 配置实例
     */
    public static <T extends BaseConfiguration> T createConfiguration(Class<T> configClass, String presetName) {
        return createConfiguration(configClass, presetName, null);
    }
    
    /**
     * 创建配置实例并应用预设和环境适配
     * 
     * @param configClass 配置类
     * @param presetName 预设名称
     * @param environment 环境名称
     * @param <T> 配置类型
     * @return 配置实例
     */
    public static <T extends BaseConfiguration> T createConfiguration(Class<T> configClass, 
                                                                     String presetName, 
                                                                     String environment) {
        try {
            // 创建配置实例
            T config = configClass.getDeclaredConstructor().newInstance();
            
            // 应用预设配置
            if (presetName != null) {
                ConfigurationPreset preset = presets.get(presetName);
                if (preset != null) {
                    preset.applyPreset(config);
                    logger.info("Applied preset '{}' to {}", presetName, configClass.getSimpleName());
                } else {
                    logger.warn("Preset '{}' not found", presetName);
                }
            }
            
            // 应用环境适配
            if (environment != null) {
                for (EnvironmentAdapter adapter : adapters.values()) {
                    if (adapter.supportsEnvironment(environment)) {
                        adapter.adaptForEnvironment(config, environment);
                        logger.info("Applied environment '{}' adaptation to {}", environment, configClass.getSimpleName());
                        break;
                    }
                }
            }
            
            // 自动加载配置文件
            String configPrefix = config.getConfigPrefix();
            String propertiesFile = configPrefix.replace(".", "/") + ".properties";
            config.loadFromProperties(propertiesFile);
            config.loadFromEnvironment();
            config.loadFromSystemProperties();
            
            // 验证配置
            config.validateConfiguration();
            
            logger.info("Created configuration: {}", configClass.getSimpleName());
            return config;
            
        } catch (Exception e) {
            logger.error("Failed to create configuration: {}", configClass.getSimpleName(), e);
            throw new RuntimeException("Failed to create configuration", e);
        }
    }
    
    /**
     * 注册配置预设
     * 
     * @param preset 预设实例
     */
    public static void registerPreset(ConfigurationPreset preset) {
        presets.put(preset.getPresetName(), preset);
        logger.debug("Registered configuration preset: {}", preset.getPresetName());
    }
    
    /**
     * 注册环境适配器
     * 
     * @param adapter 适配器实例
     */
    public static void registerAdapter(EnvironmentAdapter adapter) {
        adapters.put(adapter.getClass().getSimpleName(), adapter);
        logger.debug("Registered environment adapter: {}", adapter.getClass().getSimpleName());
    }
    
    /**
     * 获取配置预设
     * 
     * @param presetName 预设名称
     * @return 预设实例
     */
    public static ConfigurationPreset getPreset(String presetName) {
        return presets.get(presetName);
    }
    
    /**
     * 获取所有预设名称
     * 
     * @return 预设名称集合
     */
    public static java.util.Set<String> getAvailablePresets() {
        return presets.keySet();
    }
    
    /**
     * 获取所有环境适配器
     * 
     * @return 适配器集合
     */
    public static java.util.Collection<EnvironmentAdapter> getAvailableAdapters() {
        return adapters.values();
    }
    
    // 内置预设实现
    
    /**
     * 开发环境预设
     */
    private static class DevelopmentPreset implements ConfigurationPreset {
        @Override
        public void applyPreset(BaseConfiguration config) {
            // 开启详细日志
            config.setConfigValue("enableDetailedLogging", true);
            config.setConfigValue("enableDebug", true);
            
            // 降低性能要求，提高开发便利性
            if (config instanceof HierarchyConfiguration) {
                config.setConfigValue("common.threadPoolSize", 2);
                config.setConfigValue("user.cacheTTLMinutes", 10);
            }
            
            if (config instanceof SearchConfiguration) {
                config.setConfigValue("performance.enableProfiling", true);
                config.setConfigValue("query.enableQueryLogging", true);
            }
            
            if (config instanceof MultimodalConfiguration) {
                config.setConfigValue("processing.threadPoolSize", 2);
                config.setConfigValue("performance.enableProfiling", true);
            }
            
            if (config instanceof AIConfiguration) {
                config.setConfigValue("performance.enableProfiling", true);
                config.setConfigValue("learning.learningUpdateIntervalMinutes", 5);
            }
        }
        
        @Override
        public String getPresetName() { return "development"; }
        
        @Override
        public String getDescription() { return "Development environment preset with debug features enabled"; }
    }
    
    /**
     * 测试环境预设
     */
    private static class TestPreset implements ConfigurationPreset {
        @Override
        public void applyPreset(BaseConfiguration config) {
            // 快速处理，便于测试
            config.setConfigValue("enableQuickMode", true);
            
            if (config instanceof HierarchyConfiguration) {
                config.setConfigValue("user.maxMemoriesPerUser", 100);
                config.setConfigValue("session.sessionTimeoutMinutes", 30);
            }
            
            if (config instanceof SearchConfiguration) {
                config.setConfigValue("semantic.cacheSize", 100);
                config.setConfigValue("performance.searchTimeoutMs", 1000);
            }
            
            if (config instanceof MultimodalConfiguration) {
                config.setConfigValue("storage.retentionDays", 7);
                config.setConfigValue("processing.processingTimeoutMs", 5000);
            }
            
            if (config instanceof AIConfiguration) {
                config.setConfigValue("learning.learningWindowDays", 1);
                config.setConfigValue("compression.autoCompressionIntervalHours", 1);
            }
        }
        
        @Override
        public String getPresetName() { return "test"; }
        
        @Override
        public String getDescription() { return "Test environment preset with fast processing and small limits"; }
    }
    
    /**
     * 生产环境预设
     */
    private static class ProductionPreset implements ConfigurationPreset {
        @Override
        public void applyPreset(BaseConfiguration config) {
            // 优化性能和稳定性
            config.setConfigValue("enableDetailedLogging", false);
            config.setConfigValue("enableMetrics", true);
            
            if (config instanceof HierarchyConfiguration) {
                config.setConfigValue("common.threadPoolSize", 20);
                config.setConfigValue("user.maxMemoriesPerUser", 50000);
                config.setConfigValue("manager.enablePerformanceMonitoring", true);
            }
            
            if (config instanceof SearchConfiguration) {
                config.setConfigValue("performance.maxConcurrentSearches", 200);
                config.setConfigValue("semantic.cacheSize", 50000);
                config.setConfigValue("performance.enableResultCaching", true);
            }
            
            if (config instanceof MultimodalConfiguration) {
                config.setConfigValue("processing.threadPoolSize", 8);
                config.setConfigValue("performance.enableLoadBalancing", true);
                config.setConfigValue("storage.enableCompression", true);
            }
            
            if (config instanceof AIConfiguration) {
                config.setConfigValue("performance.threadPoolSize", 16);
                config.setConfigValue("performance.enableLoadBalancing", true);
                config.setConfigValue("data.enableDataBackup", true);
            }
        }
        
        @Override
        public String getPresetName() { return "production"; }
        
        @Override
        public String getDescription() { return "Production environment preset with high performance and stability"; }
    }
    
    /**
     * 高性能预设
     */
    private static class HighPerformancePreset implements ConfigurationPreset {
        @Override
        public void applyPreset(BaseConfiguration config) {
            // 最大化性能
            if (config instanceof HierarchyConfiguration) {
                config.setConfigValue("common.threadPoolSize", 50);
                config.setConfigValue("user.cacheTTLMinutes", 120);
                config.setConfigValue("manager.routingCacheSize", 5000);
            }
            
            if (config instanceof SearchConfiguration) {
                config.setConfigValue("performance.maxConcurrentSearches", 500);
                config.setConfigValue("semantic.cacheSize", 100000);
                config.setConfigValue("hybrid.cacheSize", 10000);
                config.setConfigValue("performance.enableResultCaching", true);
            }
            
            if (config instanceof MultimodalConfiguration) {
                config.setConfigValue("processing.threadPoolSize", 16);
                config.setConfigValue("cache.processingCacheSize", 5000);
                config.setConfigValue("cache.featureCacheSize", 50000);
                config.setConfigValue("performance.enableLoadBalancing", true);
            }
            
            if (config instanceof AIConfiguration) {
                config.setConfigValue("performance.threadPoolSize", 32);
                config.setConfigValue("performance.cacheSize", 50000);
                config.setConfigValue("performance.enableAsyncProcessing", true);
            }
        }
        
        @Override
        public String getPresetName() { return "high-performance"; }
        
        @Override
        public String getDescription() { return "High performance preset with maximum throughput optimization"; }
    }
    
    /**
     * 低资源预设
     */
    private static class LowResourcePreset implements ConfigurationPreset {
        @Override
        public void applyPreset(BaseConfiguration config) {
            // 最小化资源使用
            if (config instanceof HierarchyConfiguration) {
                config.setConfigValue("common.threadPoolSize", 2);
                config.setConfigValue("user.maxMemoriesPerUser", 1000);
                config.setConfigValue("user.cacheTTLMinutes", 10);
            }
            
            if (config instanceof SearchConfiguration) {
                config.setConfigValue("performance.maxConcurrentSearches", 10);
                config.setConfigValue("semantic.cacheSize", 1000);
                config.setConfigValue("hybrid.cacheSize", 100);
            }
            
            if (config instanceof MultimodalConfiguration) {
                config.setConfigValue("processing.threadPoolSize", 1);
                config.setConfigValue("cache.processingCacheSize", 100);
                config.setConfigValue("cache.featureCacheSize", 500);
                config.setConfigValue("storage.maxFileSize", 10 * 1024 * 1024); // 10MB
            }
            
            if (config instanceof AIConfiguration) {
                config.setConfigValue("performance.threadPoolSize", 2);
                config.setConfigValue("performance.cacheSize", 1000);
                config.setConfigValue("learning.maxRecommendations", 5);
            }
        }
        
        @Override
        public String getPresetName() { return "low-resource"; }
        
        @Override
        public String getDescription() { return "Low resource preset for memory and CPU constrained environments"; }
    }
    
    // 内置环境适配器实现
    
    /**
     * 通用环境适配器
     */
    private static class GeneralEnvironmentAdapter implements EnvironmentAdapter {
        @Override
        public void adaptForEnvironment(BaseConfiguration config, String environment) {
            switch (environment.toLowerCase()) {
                case "development":
                case "dev":
                    config.setConfigValue("enableDebug", true);
                    config.setConfigValue("enableDetailedLogging", true);
                    break;
                case "testing":
                case "test":
                    config.setConfigValue("enableQuickMode", true);
                    break;
                case "staging":
                case "stage":
                    config.setConfigValue("enableMonitoring", true);
                    break;
                case "production":
                case "prod":
                    config.setConfigValue("enableDetailedLogging", false);
                    config.setConfigValue("enableMetrics", true);
                    break;
            }
        }
        
        @Override
        public boolean supportsEnvironment(String environment) {
            return true; // 支持所有环境
        }
    }
    
    /**
     * 云环境适配器
     */
    private static class CloudEnvironmentAdapter implements EnvironmentAdapter {
        @Override
        public void adaptForEnvironment(BaseConfiguration config, String environment) {
            if (environment.toLowerCase().contains("cloud") || 
                environment.toLowerCase().contains("aws") ||
                environment.toLowerCase().contains("azure") ||
                environment.toLowerCase().contains("gcp")) {
                
                // 云环境特定配置
                config.setConfigValue("enableCloudOptimization", true);
                config.setConfigValue("enableAutoScaling", true);
                
                if (config instanceof MultimodalConfiguration) {
                    config.setConfigValue("storage.enableCloudStorage", true);
                }
                
                if (config instanceof AIConfiguration) {
                    config.setConfigValue("data.enableCloudBackup", true);
                }
            }
        }
        
        @Override
        public boolean supportsEnvironment(String environment) {
            String env = environment.toLowerCase();
            return env.contains("cloud") || env.contains("aws") || 
                   env.contains("azure") || env.contains("gcp");
        }
    }
    
    /**
     * 容器环境适配器
     */
    private static class ContainerEnvironmentAdapter implements EnvironmentAdapter {
        @Override
        public void adaptForEnvironment(BaseConfiguration config, String environment) {
            if (environment.toLowerCase().contains("docker") ||
                environment.toLowerCase().contains("kubernetes") ||
                environment.toLowerCase().contains("container")) {
                
                // 容器环境特定配置
                config.setConfigValue("enableContainerOptimization", true);
                
                if (config instanceof HierarchyConfiguration) {
                    config.setConfigValue("common.threadPoolSize", 4); // 容器环境通常资源有限
                }
                
                if (config instanceof MultimodalConfiguration) {
                    config.setConfigValue("storage.basePath", "/app/data"); // 容器内路径
                }
            }
        }
        
        @Override
        public boolean supportsEnvironment(String environment) {
            String env = environment.toLowerCase();
            return env.contains("docker") || env.contains("kubernetes") || 
                   env.contains("container");
        }
    }
}