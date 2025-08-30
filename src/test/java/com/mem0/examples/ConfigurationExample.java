package com.mem0.examples;

import com.mem0.config.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

/**
 * ConfigurationExample - 配置使用示例
 * 
 * 展示如何在实际项目中使用mem0-java的配置系统。
 * 包括配置创建、预设应用、环境适配、动态配置等功能的使用示例。
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 2024-12-20
 */
public class ConfigurationExample {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationExample.class);
    
    public static void main(String[] args) {
        // 示例1：使用ConfigurationManager获取配置
        demonstrateConfigurationManager();
        
        // 示例2：使用ConfigurationFactory创建配置
        demonstrateConfigurationFactory();
        
        // 示例3：配置监听器的使用
        demonstrateConfigurationListener();
        
        // 示例4：配置验证和导出
        demonstrateConfigurationValidation();
        
        // 示例5：动态配置更新
        demonstrateDynamicConfiguration();
    }
    
    /**
     * 演示ConfigurationManager的使用
     */
    private static void demonstrateConfigurationManager() {
        logger.info("=== ConfigurationManager 使用示例 ===");
        
        // 获取ConfigurationManager实例
        ConfigurationManager configManager = ConfigurationManager.getInstance();
        
        // 获取各模块配置
        HierarchyConfiguration hierarchyConfig = configManager.getHierarchyConfiguration();
        SearchConfiguration searchConfig = configManager.getSearchConfiguration();
        MultimodalConfiguration multimodalConfig = configManager.getMultimodalConfiguration();
        AIConfiguration aiConfig = configManager.getAIConfiguration();
        
        // 使用配置
        logger.info("用户最大内存数量: {}", hierarchyConfig.getMaxMemoriesPerUser());
        logger.info("语义搜索向量维度: {}", searchConfig.getVectorDimension());
        logger.info("最大文件大小: {} MB", multimodalConfig.getMaxFileSize() / 1024 / 1024);
        logger.info("学习率: {}", aiConfig.getLearningRate());
        
        // 获取全局属性
        String appName = configManager.getGlobalProperty("app.name", "unknown");
        boolean isDebug = configManager.getGlobalProperty("env.debug", false);
        logger.info("应用名称: {}, 调试模式: {}", appName, isDebug);
        
        // 获取配置摘要
        Object summary = configManager.getConfigurationSummary();
        logger.info("配置摘要: {}", summary);
    }
    
    /**
     * 演示ConfigurationFactory的使用
     */
    private static void demonstrateConfigurationFactory() {
        logger.info("=== ConfigurationFactory 使用示例 ===");
        
        // 创建开发环境的层级配置
        HierarchyConfiguration devHierarchyConfig = ConfigurationFactory.createConfiguration(
            HierarchyConfiguration.class, "development", "development");
        logger.info("开发环境 - 线程池大小: {}", devHierarchyConfig.getThreadPoolSize());
        
        // 创建生产环境的搜索配置
        SearchConfiguration prodSearchConfig = ConfigurationFactory.createConfiguration(
            SearchConfiguration.class, "production", "production");
        logger.info("生产环境 - 最大并发搜索: {}", prodSearchConfig.getMaxConcurrentSearches());
        
        // 创建高性能预设的AI配置
        AIConfiguration highPerfAIConfig = ConfigurationFactory.createConfiguration(
            AIConfiguration.class, "high-performance");
        logger.info("高性能预设 - AI线程池大小: {}", highPerfAIConfig.getAIThreadPoolSize());
        
        // 获取可用预设
        Object availablePresets = ConfigurationFactory.getAvailablePresets();
        logger.info("可用预设: {}", availablePresets);
    }
    
    /**
     * 演示配置监听器的使用
     */
    private static void demonstrateConfigurationListener() {
        logger.info("=== Configuration Listener 使用示例 ===");
        
        ConfigurationManager configManager = ConfigurationManager.getInstance();
        
        // 添加配置监听器
        configManager.addConfigurationListener("app.*", (key, oldValue, newValue) -> {
            logger.info("配置变更通知 - Key: {}, Old: {}, New: {}", key, oldValue, newValue);
        });
        
        // 修改全局属性触发监听器
        configManager.setGlobalProperty("app.test", "test_value");
        configManager.setGlobalProperty("app.test", "updated_value");
        
        // 移除监听器
        configManager.removeConfigurationListener("app.*");
    }
    
    /**
     * 演示配置验证和导出
     */
    private static void demonstrateConfigurationValidation() {
        logger.info("=== Configuration Validation 使用示例 ===");
        
        ConfigurationManager configManager = ConfigurationManager.getInstance();
        
        // 验证所有配置
        Object validationResults = configManager.validateAllConfigurations();
        logger.info("配置验证结果: {}", validationResults);
        
        // 导出配置到Properties格式
        String exportedConfig = configManager.exportToProperties();
        logger.info("导出的配置内容长度: {} 字符", exportedConfig.length());
        
        // 可以将导出的配置保存到文件
        // Files.writeString(Paths.get("exported_config.properties"), exportedConfig);
    }
    
    /**
     * 演示动态配置更新
     */
    private static void demonstrateDynamicConfiguration() {
        logger.info("=== Dynamic Configuration 使用示例 ===");
        
        ConfigurationManager configManager = ConfigurationManager.getInstance();
        
        // 获取搜索配置
        SearchConfiguration searchConfig = configManager.getSearchConfiguration();
        int originalMaxResults = searchConfig.getSemanticMaxResults();
        logger.info("原始最大搜索结果数: {}", originalMaxResults);
        
        // 动态更新配置
        searchConfig.setConfigValue("semantic.maxResults", 100);
        int updatedMaxResults = searchConfig.getSemanticMaxResults();
        logger.info("更新后最大搜索结果数: {}", updatedMaxResults);
        
        // 重新加载配置
        configManager.reloadConfiguration(SearchConfiguration.class);
        int reloadedMaxResults = searchConfig.getSemanticMaxResults();
        logger.info("重新加载后最大搜索结果数: {}", reloadedMaxResults);
        
        // 重新加载所有配置
        configManager.reloadConfigurations();
        logger.info("所有配置已重新加载");
    }
    
    /**
     * 演示特定场景下的配置使用
     */
    public static void demonstrateScenarioBasedConfiguration() {
        logger.info("=== Scenario-Based Configuration 使用示例 ===");
        
        // 场景1：开发环境 - 启用调试和详细日志
        if (ConfigurationManager.getInstance().isDevelopmentEnvironment()) {
            HierarchyConfiguration devConfig = ConfigurationFactory.createConfiguration(
                HierarchyConfiguration.class, "development", "development");
            logger.info("开发环境配置 - 详细日志: {}", devConfig.isEnableDetailedLogging());
        }
        
        // 场景2：生产环境 - 高性能配置
        if (ConfigurationManager.getInstance().isProductionEnvironment()) {
            SearchConfiguration prodConfig = ConfigurationFactory.createConfiguration(
                SearchConfiguration.class, "production", "production");
            logger.info("生产环境配置 - 结果缓存: {}", prodConfig.isEnableResultCaching());
        }
        
        // 场景3：容器环境 - 资源限制配置
        MultimodalConfiguration containerConfig = ConfigurationFactory.createConfiguration(
            MultimodalConfiguration.class, "low-resource", "docker");
        logger.info("容器环境配置 - 线程池大小: {}", containerConfig.getThreadPoolSize());
        
        // 场景4：云环境 - 自动扩展配置
        AIConfiguration cloudConfig = ConfigurationFactory.createConfiguration(
            AIConfiguration.class, "high-performance", "aws");
        logger.info("云环境配置 - 负载均衡: {}", cloudConfig.isEnableAILoadBalancing());
    }
    
    /**
     * 演示配置的单元测试友好性
     */
    public static void demonstrateTestFriendlyConfiguration() {
        logger.info("=== Test-Friendly Configuration 使用示例 ===");
        
        // 创建测试专用的配置实例
        HierarchyConfiguration testConfig = ConfigurationFactory.createConfiguration(
            HierarchyConfiguration.class, "test");
        
        // 验证测试配置的特点
        logger.info("测试配置 - 用户最大内存: {}", testConfig.getMaxMemoriesPerUser());
        logger.info("测试配置 - 会话超时: {} 分钟", testConfig.getSessionTimeoutMinutes());
        
        // 临时修改配置用于测试
        testConfig.setConfigValue("user.maxMemoriesPerUser", 10);
        testConfig.setConfigValue("session.sessionTimeoutMinutes", 5);
        
        logger.info("临时测试配置 - 用户最大内存: {}", testConfig.getMaxMemoriesPerUser());
        logger.info("临时测试配置 - 会话超时: {} 分钟", testConfig.getSessionTimeoutMinutes());
        
        // 重置配置
        testConfig.clearConfiguration();
        logger.info("配置已重置");
    }
    
    /**
     * 演示配置的性能监控
     */
    public static void demonstrateConfigurationPerformance() {
        logger.info("=== Configuration Performance 使用示例 ===");
        
        ConfigurationManager configManager = ConfigurationManager.getInstance();
        
        // 测量配置获取性能
        long startTime = System.nanoTime();
        
        for (int i = 0; i < 1000; i++) {
            HierarchyConfiguration config = configManager.getHierarchyConfiguration();
            int maxMemories = config.getMaxMemoriesPerUser();
            // 模拟使用配置
        }
        
        long endTime = System.nanoTime();
        double avgTime = (endTime - startTime) / 1000000.0 / 1000; // 转换为毫秒
        
        logger.info("1000次配置获取平均耗时: {:.3f} ms", avgTime);
        
        // 测量配置验证性能
        startTime = System.nanoTime();
        Object validationResults = configManager.validateAllConfigurations();
        endTime = System.nanoTime();
        double validationTime = (endTime - startTime) / 1000000.0;
        
        logger.info("配置验证耗时: {:.3f} ms", validationTime);
        logger.info("验证结果: {}", validationResults);
    }
}