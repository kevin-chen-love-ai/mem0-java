package com.mem0.examples.advanced;

import com.mem0.config.EmbeddingConfiguration;
import com.mem0.embedding.EmbeddingProvider;
import com.mem0.embedding.EmbeddingProviderFactory;
import com.mem0.embedding.EmbeddingProviderFactory.ProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * EmbeddingExample - 嵌入向量使用示例
 * 
 * 展示如何在实际项目中使用mem0-java的嵌入向量功能。
 * 包括不同提供者的创建、使用、配置管理等功能的使用示例。
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 2024-12-20
 */
public class EmbeddingExample {
    
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingExample.class);
    
    public static void main(String[] args) {
        logger.info("=== 嵌入向量服务使用示例 ===");
        
        // 示例1：使用TF-IDF提供者
        demonstrateTFIDFProvider();
        
        // 示例2：使用高性能TF-IDF提供者
        demonstrateHighPerformanceTFIDFProvider();
        
        // 示例3：使用Mock提供者
        demonstrateMockProvider();
        
        // 示例4：使用工厂模式创建提供者
        demonstrateProviderFactory();
        
        // 示例5：配置管理示例
        demonstrateConfigurationManagement();
        
        // 示例6：批量处理示例
        demonstrateBatchProcessing();
        
        // 示例7：错误处理示例
        demonstrateErrorHandling();
        
        // 清理资源
        cleanupResources();
    }
    
    /**
     * 演示TF-IDF提供者的使用
     */
    private static void demonstrateTFIDFProvider() {
        logger.info("=== TF-IDF 提供者使用示例 ===");
        
        try {
            // 创建TF-IDF提供者
            EmbeddingProvider provider = EmbeddingProviderFactory.createTFIDF();
            
            // 基本信息
            logger.info("提供者名称: {}", provider.getProviderName());
            logger.info("向量维度: {}", provider.getDimension());
            logger.info("健康状态: {}", provider.isHealthy());
            
            // 单个文本嵌入
            String text = "这是一个测试文档，用于演示TF-IDF嵌入向量的生成";
            CompletableFuture<List<Float>> embeddingFuture = provider.embed(text);
            List<Float> embedding = embeddingFuture.join();
            
            logger.info("文本: {}", text);
            logger.info("嵌入向量长度: {}", embedding.size());
            logger.info("向量前5个值: {}", embedding.subList(0, Math.min(5, embedding.size())));
            
        } catch (Exception e) {
            logger.error("TF-IDF提供者示例执行失败", e);
        }
    }
    
    /**
     * 演示高性能TF-IDF提供者的使用
     */
    private static void demonstrateHighPerformanceTFIDFProvider() {
        logger.info("=== 高性能 TF-IDF 提供者使用示例 ===");
        
        try {
            // 创建高性能TF-IDF提供者
            EmbeddingProvider provider = EmbeddingProviderFactory.createHighPerformanceTFIDF();
            
            // 基本信息
            logger.info("提供者名称: {}", provider.getProviderName());
            logger.info("向量维度: {}", provider.getDimension());
            
            // 多个文本的嵌入
            List<String> texts = Arrays.asList(
                "人工智能是未来技术发展的重要方向",
                "机器学习算法在各个领域都有广泛应用",
                "自然语言处理技术帮助计算机理解人类语言"
            );
            
            CompletableFuture<List<List<Float>>> embeddingsFuture = provider.embedBatch(texts);
            List<List<Float>> embeddings = embeddingsFuture.join();
            
            logger.info("处理了 {} 个文本", embeddings.size());
            for (int i = 0; i < texts.size(); i++) {
                logger.info("文本{}: {} -> 向量维度: {}", 
                          i + 1, texts.get(i), embeddings.get(i).size());
            }
            
        } catch (Exception e) {
            logger.error("高性能TF-IDF提供者示例执行失败", e);
        }
    }
    
    /**
     * 演示Mock提供者的使用
     */
    private static void demonstrateMockProvider() {
        logger.info("=== Mock 提供者使用示例 ===");
        
        try {
            // 创建Mock提供者
            EmbeddingProvider provider = EmbeddingProviderFactory.createMock();
            
            // 基本信息
            logger.info("提供者名称: {}", provider.getProviderName());
            logger.info("向量维度: {}", provider.getDimension());
            
            // 测试嵌入
            String testText = "Mock provider test text";
            List<Float> embedding = provider.embed(testText).join();
            
            logger.info("测试文本: {}", testText);
            logger.info("Mock向量长度: {}", embedding.size());
            logger.info("Mock向量示例值: {}", embedding.subList(0, Math.min(3, embedding.size())));
            
        } catch (Exception e) {
            logger.error("Mock提供者示例执行失败", e);
        }
    }
    
    /**
     * 演示提供者工厂的使用
     */
    private static void demonstrateProviderFactory() {
        logger.info("=== 提供者工厂使用示例 ===");
        
        try {
            // 使用枚举创建提供者
            EmbeddingProvider tfidfProvider = EmbeddingProviderFactory.create(ProviderType.TFIDF, null);
            EmbeddingProvider mockProvider = EmbeddingProviderFactory.create(ProviderType.MOCK, null);
            
            logger.info("通过工厂创建的提供者:");
            logger.info("- TF-IDF: {}, 维度: {}", tfidfProvider.getProviderName(), tfidfProvider.getDimension());
            logger.info("- Mock: {}, 维度: {}", mockProvider.getProviderName(), mockProvider.getDimension());
            
            // 获取默认提供者
            EmbeddingProvider defaultProvider = EmbeddingProviderFactory.getDefaultProvider();
            logger.info("默认提供者: {}", defaultProvider.getProviderName());
            
            // 检查缓存状态
            int cachedCount = EmbeddingProviderFactory.getCachedProviderCount();
            logger.info("缓存的提供者数量: {}", cachedCount);
            
            // 健康状态检查
            int healthyCount = EmbeddingProviderFactory.checkHealthStatus();
            logger.info("健康的提供者数量: {}", healthyCount);
            
        } catch (Exception e) {
            logger.error("提供者工厂示例执行失败", e);
        }
    }
    
    /**
     * 演示配置管理
     */
    private static void demonstrateConfigurationManagement() {
        logger.info("=== 配置管理使用示例 ===");
        
        try {
            // 创建嵌入配置
            EmbeddingConfiguration config = new EmbeddingConfiguration();
            
            // 读取配置信息
            logger.info("默认提供者类型: {}", config.getProviderType());
            logger.info("是否启用缓存: {}", config.isCacheEnabled());
            logger.info("缓存最大大小: {}", config.getCacheMaxSize());
            logger.info("TF-IDF词汇表大小: {}", config.getTFIDFVocabularySize());
            logger.info("性能线程池大小: {}", config.getPerformanceThreadPoolSize());
            
            // OpenAI配置（如果有API密钥）
            String openaiApiKey = config.getOpenAIApiKey();
            if (openaiApiKey != null && !openaiApiKey.isEmpty()) {
                logger.info("OpenAI配置可用:");
                logger.info("- 模型: {}", config.getOpenAIModel());
                logger.info("- 向量维度: {}", config.getOpenAIDimension());
                logger.info("- 超时时间: {} 秒", config.getOpenAITimeoutSeconds());
            } else {
                logger.info("OpenAI API密钥未配置，跳过OpenAI配置展示");
            }
            
            // 阿里云配置（如果有API密钥）
            String aliyunApiKey = config.getAliyunApiKey();
            if (aliyunApiKey != null && !aliyunApiKey.isEmpty()) {
                logger.info("阿里云配置可用:");
                logger.info("- 模型: {}", config.getAliyunModel());
                logger.info("- 向量维度: {}", config.getAliyunDimension());
                logger.info("- 文本类型: {}", config.getAliyunTextType());
            } else {
                logger.info("阿里云API密钥未配置，跳过阿里云配置展示");
            }
            
        } catch (Exception e) {
            logger.error("配置管理示例执行失败", e);
        }
    }
    
    /**
     * 演示批量处理
     */
    private static void demonstrateBatchProcessing() {
        logger.info("=== 批量处理使用示例 ===");
        
        try {
            EmbeddingProvider provider = EmbeddingProviderFactory.createTFIDF();
            
            // 准备批量文本
            List<String> batchTexts = Arrays.asList(
                "第一个文档：介绍机器学习的基本概念",
                "第二个文档：深度学习在图像识别中的应用",
                "第三个文档：自然语言处理的发展历程",
                "第四个文档：人工智能的伦理与社会影响",
                "第五个文档：未来AI技术的发展趋势"
            );
            
            // 记录开始时间
            long startTime = System.currentTimeMillis();
            
            // 批量处理
            CompletableFuture<List<List<Float>>> batchFuture = provider.embedBatch(batchTexts);
            List<List<Float>> batchResults = batchFuture.join();
            
            // 记录结束时间
            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;
            
            logger.info("批量处理完成:");
            logger.info("- 文档数量: {}", batchTexts.size());
            logger.info("- 处理时间: {} ms", processingTime);
            logger.info("- 平均每个文档: {:.2f} ms", processingTime / (double) batchTexts.size());
            
            // 验证结果
            for (int i = 0; i < batchResults.size(); i++) {
                List<Float> embedding = batchResults.get(i);
                logger.info("文档{} 向量维度: {}", i + 1, embedding.size());
            }
            
        } catch (Exception e) {
            logger.error("批量处理示例执行失败", e);
        }
    }
    
    /**
     * 演示错误处理
     */
    private static void demonstrateErrorHandling() {
        logger.info("=== 错误处理使用示例 ===");
        
        try {
            EmbeddingProvider provider = EmbeddingProviderFactory.createTFIDF();
            
            // 测试空文本处理
            try {
                provider.embed("").join();
                logger.warn("空文本处理应该失败，但没有抛出异常");
            } catch (Exception e) {
                logger.info("正确处理空文本错误: {}", e.getMessage());
            }
            
            // 测试null文本处理
            try {
                provider.embed(null).join();
                logger.warn("null文本处理应该失败，但没有抛出异常");
            } catch (Exception e) {
                logger.info("正确处理null文本错误: {}", e.getMessage());
            }
            
            // 测试空列表处理
            try {
                provider.embedBatch(Arrays.asList()).join();
                logger.warn("空列表处理应该失败，但没有抛出异常");
            } catch (Exception e) {
                logger.info("正确处理空列表错误: {}", e.getMessage());
            }
            
            // 测试包含null的列表
            try {
                provider.embedBatch(Arrays.asList("正常文本", null, "另一个正常文本")).join();
                logger.warn("包含null的列表处理应该失败，但没有抛出异常");
            } catch (Exception e) {
                logger.info("正确处理包含null的列表错误: {}", e.getMessage());
            }
            
        } catch (Exception e) {
            logger.error("错误处理示例执行失败", e);
        }
    }
    
    /**
     * 清理资源
     */
    private static void cleanupResources() {
        logger.info("=== 清理资源 ===");
        
        try {
            // 关闭所有提供者
            EmbeddingProviderFactory.closeAll();
            logger.info("所有嵌入提供者已关闭");
            
        } catch (Exception e) {
            logger.error("清理资源时发生错误", e);
        }
    }
    
    /**
     * 演示OpenAI提供者的使用（需要有效的API密钥）
     */
    public static void demonstrateOpenAIProvider() {
        logger.info("=== OpenAI 提供者使用示例 ===");
        
        // 注意：这需要有效的OpenAI API密钥
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("OpenAI API密钥未设置，跳过OpenAI提供者示例");
            logger.info("请设置环境变量 OPENAI_API_KEY 来测试OpenAI功能");
            return;
        }
        
        try {
            // 创建OpenAI提供者
            EmbeddingProvider provider = EmbeddingProviderFactory.createOpenAI(apiKey);
            
            logger.info("OpenAI提供者创建成功:");
            logger.info("- 提供者名称: {}", provider.getProviderName());
            logger.info("- 向量维度: {}", provider.getDimension());
            logger.info("- 健康状态: {}", provider.isHealthy());
            
            // 测试嵌入
            String text = "Hello, this is a test for OpenAI embedding service.";
            CompletableFuture<List<Float>> embeddingFuture = provider.embed(text);
            List<Float> embedding = embeddingFuture.join();
            
            logger.info("OpenAI嵌入结果:");
            logger.info("- 文本: {}", text);
            logger.info("- 向量维度: {}", embedding.size());
            logger.info("- 向量样本: {}", embedding.subList(0, Math.min(3, embedding.size())));
            
        } catch (Exception e) {
            logger.error("OpenAI提供者示例执行失败", e);
        }
    }
    
    /**
     * 演示阿里云提供者的使用（需要有效的API密钥）
     */
    public static void demonstrateAliyunProvider() {
        logger.info("=== 阿里云 提供者使用示例 ===");
        
        // 注意：这需要有效的阿里云API密钥
        String apiKey = System.getenv("ALIYUN_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("阿里云API密钥未设置，跳过阿里云提供者示例");
            logger.info("请设置环境变量 ALIYUN_API_KEY 来测试阿里云功能");
            return;
        }
        
        try {
            // 创建阿里云提供者
            EmbeddingProvider provider = EmbeddingProviderFactory.createAliyun(apiKey);
            
            logger.info("阿里云提供者创建成功:");
            logger.info("- 提供者名称: {}", provider.getProviderName());
            logger.info("- 向量维度: {}", provider.getDimension());
            logger.info("- 健康状态: {}", provider.isHealthy());
            
            // 测试中文嵌入
            String text = "你好，这是一个阿里云嵌入服务的测试。";
            CompletableFuture<List<Float>> embeddingFuture = provider.embed(text);
            List<Float> embedding = embeddingFuture.join();
            
            logger.info("阿里云嵌入结果:");
            logger.info("- 文本: {}", text);
            logger.info("- 向量维度: {}", embedding.size());
            logger.info("- 向量样本: {}", embedding.subList(0, Math.min(3, embedding.size())));
            
        } catch (Exception e) {
            logger.error("阿里云提供者示例执行失败", e);
        }
    }
}