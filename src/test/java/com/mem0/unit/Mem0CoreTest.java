package com.mem0.core;

import com.mem0.config.Mem0Config;
import com.mem0.embedding.EmbeddingProvider;
import com.mem0.embedding.impl.SimpleTFIDFEmbeddingProvider;
import com.mem0.store.VectorStore;
import com.mem0.vector.impl.InMemoryVectorStore;
import com.mem0.store.GraphStore;
import com.mem0.llm.LLMProvider;
import com.mem0.llm.impl.RuleBasedLLMProvider;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;

/**
 * 核心组件单元测试
 */
public class Mem0CoreTest {
    
    private static final Logger logger = LoggerFactory.getLogger(Mem0CoreTest.class);
    
    private EmbeddingProvider embeddingProvider;
    private VectorStore vectorStore;
    private GraphStore graphStore;
    private LLMProvider llmProvider;
    private EnhancedMemoryService memoryService;
    
    @Before
    public void setUp() throws Exception {
        logger.info("初始化核心组件");
        
        // 初始化组件
        embeddingProvider = new SimpleTFIDFEmbeddingProvider();
        vectorStore = new InMemoryVectorStore();
        // Create a simple in-memory GraphStore implementation
        graphStore = new GraphStore() {
            private final java.util.concurrent.ConcurrentHashMap<String, Object> nodes = new java.util.concurrent.ConcurrentHashMap<>();
            
            @Override
            public java.util.concurrent.CompletableFuture<String> createNode(String label, java.util.Map<String, Object> properties) {
                String id = java.util.UUID.randomUUID().toString();
                nodes.put(id, properties);
                return java.util.concurrent.CompletableFuture.completedFuture(id);
            }
            
            @Override
            public java.util.concurrent.CompletableFuture<java.util.List<com.mem0.store.GraphStore.GraphNode>> getNodesByLabel(String label, java.util.Map<String, Object> filter) {
                return java.util.concurrent.CompletableFuture.completedFuture(new java.util.ArrayList<>());
            }
            
            @Override
            public java.util.concurrent.CompletableFuture<String> createRelationship(String sourceId, String targetId, String type, java.util.Map<String, Object> properties) {
                return java.util.concurrent.CompletableFuture.completedFuture(java.util.UUID.randomUUID().toString());
            }
            
            @Override
            public java.util.concurrent.CompletableFuture<java.util.List<com.mem0.store.GraphStore.GraphNode>> findConnectedNodes(String nodeId, String relationshipType, int maxHops) {
                return java.util.concurrent.CompletableFuture.completedFuture(new java.util.ArrayList<>());
            }
            
            @Override
            public java.util.concurrent.CompletableFuture<Void> deleteNode(String nodeId) {
                nodes.remove(nodeId);
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }
            
            @Override
            public java.util.concurrent.CompletableFuture<Void> close() {
                nodes.clear();
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }
            
            @Override
            public java.util.concurrent.CompletableFuture<java.util.List<java.util.Map<String, Object>>> executeQuery(String query, java.util.Map<String, Object> parameters) {
                return java.util.concurrent.CompletableFuture.completedFuture(new java.util.ArrayList<>());
            }
            
            @Override
            public java.util.concurrent.CompletableFuture<Void> deleteRelationship(String relationshipId) {
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }
            
            @Override
            public java.util.concurrent.CompletableFuture<Void> updateRelationship(String relationshipId, java.util.Map<String, Object> properties) {
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }
            
            @Override
            public java.util.concurrent.CompletableFuture<Void> updateNode(String nodeId, java.util.Map<String, Object> properties) {
                if (nodes.containsKey(nodeId)) {
                    nodes.put(nodeId, properties);
                }
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }
            
            @Override
            public java.util.concurrent.CompletableFuture<java.util.List<com.mem0.store.GraphStore.GraphRelationship>> getRelationships(String nodeId, String relationshipType) {
                return java.util.concurrent.CompletableFuture.completedFuture(new java.util.ArrayList<>());
            }
            
            @Override
            public java.util.concurrent.CompletableFuture<com.mem0.store.GraphStore.GraphNode> getNode(String nodeId) {
                if (nodes.containsKey(nodeId)) {
                    java.util.Map<String, Object> properties = (java.util.Map<String, Object>) nodes.get(nodeId);
                    java.util.List<String> labels = new java.util.ArrayList<>();
                    labels.add("Memory");
                    return java.util.concurrent.CompletableFuture.completedFuture(
                        new com.mem0.store.GraphStore.GraphNode(nodeId, labels, properties)
                    );
                }
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }
        };
        llmProvider = new RuleBasedLLMProvider();
        
        // 初始化内存服务
        Mem0Config config = new Mem0Config();
        config.getVectorStore().setProvider("inmemory");
        config.getGraphStore().setProvider("inmemory");
        config.getEmbedding().setProvider("tfidf");
        config.getLlm().setProvider("rulebased");
            
        // 创建必要的组件
        MemoryClassifier memoryClassifier = new MemoryClassifier(llmProvider);
        MemoryConflictDetector conflictDetector = new MemoryConflictDetector(embeddingProvider, llmProvider);
        MemoryMergeStrategy mergeStrategy = new MemoryMergeStrategy(llmProvider);
        MemoryImportanceScorer importanceScorer = new MemoryImportanceScorer(llmProvider);
        MemoryForgettingManager forgettingManager = new MemoryForgettingManager();
        
        memoryService = new EnhancedMemoryService(
            vectorStore, graphStore, embeddingProvider, llmProvider,
            memoryClassifier, conflictDetector, mergeStrategy, importanceScorer, forgettingManager
        );
        
        logger.info("核心组件初始化完成");
    }
    
    @Test
    public void testEmbeddingProvider() throws Exception {
        logger.info("测试嵌入提供者");
        
        String text = "这是一个测试文本，用于验证嵌入功能";
        
        CompletableFuture<List<Float>> embeddingFuture = embeddingProvider.embed(text);
        List<Float> embedding = embeddingFuture.get();
        
        assertNotNull("嵌入结果不应该为空", embedding);
        assertFalse("嵌入向量不应该为空", embedding.isEmpty());
        assertTrue("嵌入向量维度应该大于0", embedding.size() > 0);
        
        logger.info("嵌入测试通过，向量维度: {}", embedding.size());
    }
    
    @Test
    public void testVectorStore() throws Exception {
        logger.info("测试向量存储");
        
        String collectionName = "test-collection";
        
        // 创建集合
        CompletableFuture<Void> createFuture = vectorStore.createCollection(collectionName, 128);
        createFuture.get();
        
        // 检查集合是否存在
        CompletableFuture<Boolean> existsFuture = vectorStore.collectionExists(collectionName);
        boolean exists = existsFuture.get();
        assertTrue("集合应该存在", exists);
        
        logger.info("向量存储测试通过");
    }
    
    @Test
    public void testGraphStore() throws Exception {
        logger.info("测试图数据库存储");
        
        // 测试创建节点
        String nodeId = "test-node-1";
        EnhancedMemory memory = new EnhancedMemory(
            nodeId, 
            "测试内存内容", 
            "test-user",
            null,
            null
        );
        
        java.util.Map<String, Object> nodeProperties = new java.util.HashMap<>();
        nodeProperties.put("content", memory.getContent());
        nodeProperties.put("userId", memory.getUserId());
        CompletableFuture<Void> addFuture = graphStore.createNode("Memory", nodeProperties).thenApply(id -> null);
        addFuture.get();
        
        logger.info("图存储测试通过");
    }
    
    @Test
    public void testLLMProvider() throws Exception {
        logger.info("测试LLM提供者");
        
        LLMProvider.LLMRequest request = new LLMProvider.LLMRequest(
            "请分析这段文本的重要性", 
            new LLMProvider.LLMConfig("test-model")
        );
        
        CompletableFuture<LLMProvider.LLMResponse> responseFuture = llmProvider.generateCompletion(request);
        LLMProvider.LLMResponse response = responseFuture.get();
        
        assertNotNull("LLM响应不应该为空", response);
        assertNotNull("响应内容不应该为空", response.getContent());
        
        logger.info("LLM测试通过，响应: {}", response.getContent());
    }
    
    @Test
    public void testMemoryService() throws Exception {
        logger.info("测试内存服务");
        
        String userId = "test-service-user";
        String content = "这是一个测试内存，用于验证内存服务功能";
        
        // 测试添加内存
        CompletableFuture<String> addFuture = memoryService.addEnhancedMemory(content, userId, null, null, null, null);
        String memoryId = addFuture.get();
        
        assertNotNull("内存ID不应该为空", memoryId);
        logger.info("成功添加内存，ID: {}", memoryId);
        
        // 测试搜索内存
        CompletableFuture<List<EnhancedMemory>> searchFuture = memoryService.searchEnhancedMemories("测试", userId, 10);
        List<EnhancedMemory> results = searchFuture.get();
        
        assertFalse("搜索结果不应该为空", results.isEmpty());
        logger.info("搜索到 {} 个相关内存", results.size());
        
        logger.info("内存服务测试通过");
    }
    
    @Test
    public void testMemoryClassification() {
        logger.info("测试内存分类");
        
        MemoryClassifier classifier = new MemoryClassifier(llmProvider);
        
        // 测试不同类型的内容分类
        String factContent = "巴黎是法国的首都";
        String preferenceContent = "我喜欢在雨天听音乐";
        String experienceContent = "昨天我去了一家很棒的餐厅";
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        
        CompletableFuture<MemoryType> factTypeFuture = classifier.classifyMemory(factContent, context);
        CompletableFuture<MemoryType> preferenceTypeFuture = classifier.classifyMemory(preferenceContent, context);
        CompletableFuture<MemoryType> experienceTypeFuture = classifier.classifyMemory(experienceContent, context);
        
        MemoryType factType = factTypeFuture.join();
        MemoryType preferenceType = preferenceTypeFuture.join();
        MemoryType experienceType = experienceTypeFuture.join();
        
        logger.info("分类结果 - 事实: {}, 偏好: {}, 经历: {}", 
                    factType, preferenceType, experienceType);
        
        // 验证分类结果
        assertNotNull("分类结果不应该为空", factType);
        assertNotNull("分类结果不应该为空", preferenceType);
        assertNotNull("分类结果不应该为空", experienceType);
        
        logger.info("内存分类测试通过");
    }
    
    @Test
    public void testMemoryImportanceScoring() {
        logger.info("测试内存重要性评分");
        
        MemoryImportanceScorer scorer = new MemoryImportanceScorer(llmProvider);
        
        // 测试不同重要性的内容
        String highImportanceContent = "我对花生过敏，这可能危及生命";
        String mediumImportanceContent = "我喜欢喝咖啡，但不喜欢茶";
        String lowImportanceContent = "今天天气很好";
        
        // 创建测试用的内存对象
        EnhancedMemory highMemory = new EnhancedMemory("high-1", highImportanceContent, "test-user", null);
        EnhancedMemory mediumMemory = new EnhancedMemory("med-1", mediumImportanceContent, "test-user", null);  
        EnhancedMemory lowMemory = new EnhancedMemory("low-1", lowImportanceContent, "test-user", null);
        
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        
        CompletableFuture<MemoryImportanceScorer.ImportanceScore> highScoreFuture = scorer.scoreMemoryImportance(highMemory, context);
        CompletableFuture<MemoryImportanceScorer.ImportanceScore> mediumScoreFuture = scorer.scoreMemoryImportance(mediumMemory, context);
        CompletableFuture<MemoryImportanceScorer.ImportanceScore> lowScoreFuture = scorer.scoreMemoryImportance(lowMemory, context);
        
        double highScore = highScoreFuture.join().getTotalScore();
        double mediumScore = mediumScoreFuture.join().getTotalScore();
        double lowScore = lowScoreFuture.join().getTotalScore();
        
        logger.info("重要性评分 - 高: {}, 中: {}, 低: {}", highScore, mediumScore, lowScore);
        
        // 验证评分结果
        assertTrue("高重要性内容分数应该最高", highScore >= mediumScore);
        assertTrue("中等重要性内容分数应该比低重要性高", mediumScore >= lowScore);
        
        logger.info("内存重要性评分测试通过");
    }
}