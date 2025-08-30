package com.mem0.integration.lifecycle;

import com.mem0.Mem0;
import com.mem0.config.Mem0Config;
import com.mem0.core.EnhancedMemory;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * 内存生命周期集成测试
 * 测试内存从创建到删除的完整生命周期，包括各种业务场景
 */
public class MemoryLifecycleIntegrationTest {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryLifecycleIntegrationTest.class);
    
    private Mem0 mem0;
    
    @Before
    public void setUp() {
        logger.info("初始化内存生命周期集成测试环境");
        
        Mem0Config config = new Mem0Config();
        config.getVectorStore().setProvider("inmemory");
        config.getGraphStore().setProvider("inmemory");
        config.getEmbedding().setProvider("tfidf");
        config.getLlm().setProvider("rulebased");
            
        mem0 = new Mem0(config);
        logger.info("Mem0 实例初始化完成");
    }
    
    @After
    public void tearDown() throws Exception {
        if (mem0 != null) {
            mem0.close();
            logger.info("Mem0 实例已关闭");
        }
    }
    
    @Test
    public void testUserPersonalPreferencesLifecycle() throws Exception {
        logger.info("测试用户个人偏好生命周期");
        
        String userId = "preference-user";
        
        // 1. 添加初始偏好
        String initialPreference = "用户喜欢在早上喝黑咖啡，不加糖不加奶";
        CompletableFuture<String> addFuture = mem0.add(initialPreference, userId);
        String memoryId = addFuture.get();
        
        assertNotNull("偏好内存应该被成功添加", memoryId);
        logger.info("添加初始偏好，内存ID: {}", memoryId);
        
        // 2. 搜索相关偏好
        CompletableFuture<List<EnhancedMemory>> searchFuture = mem0.search("咖啡", userId);
        List<EnhancedMemory> coffeeMemories = searchFuture.get();
        
        assertFalse("应该能找到咖啡相关记忆", coffeeMemories.isEmpty());
        assertTrue("应该包含黑咖啡偏好", 
                   coffeeMemories.stream().anyMatch(m -> m.getContent().contains("黑咖啡")));
        logger.info("找到 {} 个咖啡相关记忆", coffeeMemories.size());
        
        // 3. 添加相关偏好（应该被识别为相关内容）
        String relatedPreference = "用户觉得加糖的咖啡太甜了，更喜欢原味";
        CompletableFuture<String> addRelatedFuture = mem0.add(relatedPreference, userId);
        String relatedMemoryId = addRelatedFuture.get();
        
        assertNotNull("相关偏好应该被添加", relatedMemoryId);
        logger.info("添加相关偏好，内存ID: {}", relatedMemoryId);
        
        // 4. 搜索应该返回两个相关记忆
        CompletableFuture<List<EnhancedMemory>> searchAgainFuture = mem0.search("咖啡", userId);
        List<EnhancedMemory> updatedCoffeeMemories = searchAgainFuture.get();
        
        assertTrue("应该有更多咖啡相关记忆", updatedCoffeeMemories.size() >= 2);
        logger.info("更新后找到 {} 个咖啡相关记忆", updatedCoffeeMemories.size());
        
        // 5. 更新偏好（偏好发生变化）
        String updatedPreference = "用户现在开始尝试在咖啡中加一点蜂蜜，发现味道很好";
        CompletableFuture<EnhancedMemory> updateFuture = mem0.update(memoryId, updatedPreference);
        EnhancedMemory updatedMemory = updateFuture.get();
        
        assertEquals("更新后ID应该相同", memoryId, updatedMemory.getId());
        logger.info("更新偏好，内存ID: {}", updatedMemory.getId());
        
        // 6. 验证偏好更新
        CompletableFuture<List<EnhancedMemory>> verifyUpdateFuture = mem0.search("蜂蜜", userId);
        List<EnhancedMemory> honeyMemories = verifyUpdateFuture.get();
        
        assertFalse("应该能找到蜂蜜相关记忆", honeyMemories.isEmpty());
        assertTrue("应该包含蜂蜜咖啡偏好",
                   honeyMemories.stream().anyMatch(m -> m.getContent().contains("蜂蜜")));
        logger.info("验证偏好更新成功");
        
        // 7. 获取用户所有偏好历史
        CompletableFuture<List<EnhancedMemory>> historyFuture = mem0.getHistory(userId);
        List<EnhancedMemory> preferenceHistory = historyFuture.get();
        
        assertFalse("偏好历史不应该为空", preferenceHistory.isEmpty());
        assertTrue("历史应该包含咖啡相关内容", 
                   preferenceHistory.stream().anyMatch(m -> m.getContent().contains("咖啡")));
        logger.info("获取到 {} 条偏好历史", preferenceHistory.size());
        
        // 8. 删除特定偏好
        CompletableFuture<Void> deleteFuture = mem0.delete(relatedMemoryId);
        deleteFuture.get();
        
        logger.info("删除相关偏好，内存ID: {}", relatedMemoryId);
        
        // 9. 验证删除效果
        CompletableFuture<List<EnhancedMemory>> finalSearchFuture = mem0.search("太甜", userId);
        List<EnhancedMemory> sweetMemories = finalSearchFuture.get();
        
        assertTrue("删除后不应该找到相关内容", 
                   sweetMemories.isEmpty() || 
                   sweetMemories.stream().noneMatch(m -> relatedMemoryId.equals(m.getId())));
        logger.info("验证删除效果成功");
    }
    
    @Test
    public void testWorkflowMemoryEvolution() throws Exception {
        logger.info("测试工作流程记忆演进");
        
        String userId = "workflow-user";
        
        // 模拟用户学习新技能的记忆演进过程
        
        // 第1阶段：初学者
        String beginnerMemory = "用户开始学习Python编程，觉得语法比较简单";
        CompletableFuture<String> stage1Future = mem0.add(beginnerMemory, userId);
        String stage1Id = stage1Future.get();
        
        logger.info("阶段1 - 初学者记忆: {}", stage1Id);
        
        // 第2阶段：遇到困难
        String challengeMemory = "用户在学习Python面向对象编程时遇到困难，特别是继承和多态概念";
        CompletableFuture<String> stage2Future = mem0.add(challengeMemory, userId);
        String stage2Id = stage2Future.get();
        
        logger.info("阶段2 - 困难记忆: {}", stage2Id);
        
        // 第3阶段：逐渐理解
        String understandingMemory = "用户通过大量练习和阅读文档，逐渐理解了Python的类和对象概念";
        CompletableFuture<String> stage3Future = mem0.add(understandingMemory, userId);
        String stage3Id = stage3Future.get();
        
        logger.info("阶段3 - 理解记忆: {}", stage3Id);
        
        // 第4阶段：熟练应用
        String masteryMemory = "用户现在能够熟练使用Python开发Web应用，并开始学习Django框架";
        CompletableFuture<String> stage4Future = mem0.add(masteryMemory, userId);
        String stage4Id = stage4Future.get();
        
        logger.info("阶段4 - 熟练记忆: {}", stage4Id);
        
        // 验证学习进程的记忆关联
        CompletableFuture<List<EnhancedMemory>> pythonSearchFuture = mem0.search("Python", userId);
        List<EnhancedMemory> pythonMemories = pythonSearchFuture.get();
        
        assertTrue("应该有多个Python相关记忆", pythonMemories.size() >= 3);
        logger.info("找到 {} 个Python相关记忆", pythonMemories.size());
        
        // 验证记忆的时间顺序反映了学习进程
        boolean hasBeginnerConcept = pythonMemories.stream()
            .anyMatch(m -> m.getContent().contains("语法比较简单"));
        boolean hasMasteryConcept = pythonMemories.stream()
            .anyMatch(m -> m.getContent().contains("熟练使用"));
        
        assertTrue("应该包含初学阶段记忆", hasBeginnerConcept);
        assertTrue("应该包含熟练阶段记忆", hasMasteryConcept);
        
        // 搜索特定技能点
        CompletableFuture<List<EnhancedMemory>> oopSearchFuture = mem0.search("面向对象", userId);
        List<EnhancedMemory> oopMemories = oopSearchFuture.get();
        
        assertFalse("应该能找到面向对象相关记忆", oopMemories.isEmpty());
        assertTrue("应该包含困难和理解的记忆",
                   oopMemories.stream().anyMatch(m -> m.getContent().contains("困难")) ||
                   oopMemories.stream().anyMatch(m -> m.getContent().contains("理解")));
        
        logger.info("工作流程记忆演进测试完成");
    }
    
    @Test
    public void testMemoryConflictResolution() throws Exception {
        logger.info("测试记忆冲突解决");
        
        String userId = "conflict-user";
        
        // 添加初始偏好
        String initialPreference = "用户喜欢看科幻电影，特别是太空题材的";
        CompletableFuture<String> initialFuture = mem0.add(initialPreference, userId);
        String initialId = initialFuture.get();
        
        logger.info("添加初始电影偏好: {}", initialId);
        
        // 添加冲突偏好（偏好发生变化）
        String conflictingPreference = "用户最近更喜欢看浪漫喜剧，觉得科幻电影太复杂了";
        CompletableFuture<String> conflictFuture = mem0.add(conflictingPreference, userId);
        String conflictId = conflictFuture.get();
        
        logger.info("添加冲突电影偏好: {}", conflictId);
        
        // 验证两种偏好都被保存（记录偏好变化）
        CompletableFuture<List<EnhancedMemory>> movieSearchFuture = mem0.search("电影", userId);
        List<EnhancedMemory> movieMemories = movieSearchFuture.get();
        
        assertTrue("应该有多个电影相关记忆", movieMemories.size() >= 2);
        
        boolean hasScienceFiction = movieMemories.stream()
            .anyMatch(m -> m.getContent().contains("科幻"));
        boolean hasRomanticComedy = movieMemories.stream()
            .anyMatch(m -> m.getContent().contains("浪漫喜剧"));
        
        assertTrue("应该包含科幻电影偏好", hasScienceFiction);
        assertTrue("应该包含浪漫喜剧偏好", hasRomanticComedy);
        
        logger.info("记忆冲突保存验证成功");
        
        // 添加解释性记忆（说明偏好变化原因）
        String explanationMemory = "用户偏好变化是因为最近工作压力大，想看轻松一些的内容";
        CompletableFuture<String> explanationFuture = mem0.add(explanationMemory, userId);
        String explanationId = explanationFuture.get();
        
        logger.info("添加解释性记忆: {}", explanationId);
        
        // 验证解释性记忆被正确关联
        CompletableFuture<List<EnhancedMemory>> contextSearchFuture = mem0.search("工作压力", userId);
        List<EnhancedMemory> contextMemories = contextSearchFuture.get();
        
        assertFalse("应该能找到工作压力相关记忆", contextMemories.isEmpty());
        assertTrue("应该包含偏好变化解释",
                   contextMemories.stream().anyMatch(m -> m.getContent().contains("偏好变化")));
        
        logger.info("记忆冲突解决测试完成");
    }
    
    @Test
    public void testMemoryImportanceEvolution() throws Exception {
        logger.info("测试记忆重要性演进");
        
        String userId = "importance-user";
        
        // 添加不同重要性级别的记忆
        
        // 高重要性：健康信息
        String healthMemory = "用户对花生严重过敏，接触后会有生命危险";
        CompletableFuture<String> healthFuture = mem0.add(healthMemory, userId);
        String healthId = healthFuture.get();
        
        logger.info("添加高重要性健康记忆: {}", healthId);
        
        // 中等重要性：工作信息
        String workMemory = "用户在ABC公司担任软件工程师，主要使用Java和Spring框架";
        CompletableFuture<String> workFuture = mem0.add(workMemory, userId);
        String workId = workFuture.get();
        
        logger.info("添加中等重要性工作记忆: {}", workId);
        
        // 低重要性：日常琐事
        String dailyMemory = "用户今天早上在路边看到一只可爱的小狗";
        CompletableFuture<String> dailyFuture = mem0.add(dailyMemory, userId);
        String dailyId = dailyFuture.get();
        
        logger.info("添加低重要性日常记忆: {}", dailyId);
        
        // 测试重要信息的搜索优先级
        CompletableFuture<List<EnhancedMemory>> safetySearchFuture = mem0.search("过敏", userId);
        List<EnhancedMemory> safetyMemories = safetySearchFuture.get();
        
        assertFalse("应该能找到过敏相关记忆", safetyMemories.isEmpty());
        assertTrue("应该包含花生过敏信息",
                   safetyMemories.stream().anyMatch(m -> m.getContent().contains("花生")));
        
        // 验证工作相关记忆
        CompletableFuture<List<EnhancedMemory>> workSearchFuture = mem0.search("Java", userId);
        List<EnhancedMemory> workMemories = workSearchFuture.get();
        
        assertFalse("应该能找到Java相关记忆", workMemories.isEmpty());
        assertTrue("应该包含工作信息",
                   workMemories.stream().anyMatch(m -> m.getContent().contains("软件工程师")));
        
        // 获取所有记忆，验证不同重要性记忆都被保存
        CompletableFuture<List<EnhancedMemory>> allMemoriesFuture = mem0.getAll(userId);
        List<EnhancedMemory> allMemories = allMemoriesFuture.get();
        
        assertTrue("应该包含所有三种重要性级别的记忆", allMemories.size() >= 3);
        
        boolean hasHealth = allMemories.stream().anyMatch(m -> m.getContent().contains("过敏"));
        boolean hasWork = allMemories.stream().anyMatch(m -> m.getContent().contains("工程师"));  
        boolean hasDaily = allMemories.stream().anyMatch(m -> m.getContent().contains("小狗"));
        
        assertTrue("应该包含健康记忆", hasHealth);
        assertTrue("应该包含工作记忆", hasWork);
        assertTrue("应该包含日常记忆", hasDaily);
        
        logger.info("记忆重要性演进测试完成");
    }
    
    @Test
    public void testMemoryContextualRelationships() throws Exception {
        logger.info("测试记忆上下文关系");
        
        String userId = "context-user";
        
        // 建立一系列相关的记忆链
        
        // 兴趣发现
        String interestMemory = "用户发现自己对机器学习很感兴趣";
        CompletableFuture<String> interestFuture = mem0.add(interestMemory, userId);
        String interestId = interestFuture.get();
        
        // 学习开始
        String learningMemory = "用户开始学习Python用于机器学习项目";
        CompletableFuture<String> learningFuture = mem0.add(learningMemory, userId);
        String learningId = learningFuture.get();
        
        // 技能发展
        String skillMemory = "用户掌握了scikit-learn库，能够构建基本的分类模型";
        CompletableFuture<String> skillFuture = mem0.add(skillMemory, userId);
        String skillId = skillFuture.get();
        
        // 项目应用
        String projectMemory = "用户用机器学习技能完成了一个客户分类项目";
        CompletableFuture<String> projectFuture = mem0.add(projectMemory, userId);
        String projectId = projectFuture.get();
        
        logger.info("建立记忆链完成: {} -> {} -> {} -> {}", 
                   interestId, learningId, skillId, projectId);
        
        // 验证记忆之间的关联性
        
        // 搜索机器学习应该返回整个学习链
        CompletableFuture<List<EnhancedMemory>> mlSearchFuture = mem0.search("机器学习", userId);
        List<EnhancedMemory> mlMemories = mlSearchFuture.get();
        
        assertTrue("应该找到多个机器学习相关记忆", mlMemories.size() >= 3);
        
        boolean hasInterest = mlMemories.stream()
            .anyMatch(m -> m.getContent().contains("感兴趣"));
        boolean hasLearning = mlMemories.stream()
            .anyMatch(m -> m.getContent().contains("开始学习"));
        boolean hasSkill = mlMemories.stream()
            .anyMatch(m -> m.getContent().contains("scikit-learn"));
        boolean hasProject = mlMemories.stream()
            .anyMatch(m -> m.getContent().contains("客户分类"));
        
        assertTrue("应该包含兴趣发现记忆", hasInterest);
        assertTrue("应该包含学习记忆", hasLearning);
        assertTrue("应该包含技能记忆", hasSkill);
        assertTrue("应该包含项目记忆", hasProject);
        
        // 搜索Python应该找到相关的学习记忆
        CompletableFuture<List<EnhancedMemory>> pythonSearchFuture = mem0.search("Python", userId);
        List<EnhancedMemory> pythonMemories = pythonSearchFuture.get();
        
        assertFalse("应该能找到Python相关记忆", pythonMemories.isEmpty());
        assertTrue("Python记忆应该与机器学习相关",
                   pythonMemories.stream().anyMatch(m -> m.getContent().contains("机器学习")));
        
        // 验证技能进阶的上下文连接
        CompletableFuture<List<EnhancedMemory>> projectSearchFuture = mem0.search("项目", userId);
        List<EnhancedMemory> projectMemories = projectSearchFuture.get();
        
        assertFalse("应该能找到项目相关记忆", projectMemories.isEmpty());
        assertTrue("项目记忆应该体现技能应用",
                   projectMemories.stream().anyMatch(m -> 
                       m.getContent().contains("机器学习") || m.getContent().contains("分类")));
        
        logger.info("记忆上下文关系测试完成");
    }
    
    @Test
    public void testMemoryPrivacyAndIsolation() throws Exception {
        logger.info("测试记忆隐私和隔离");
        
        String user1 = "privacy-user-1";
        String user2 = "privacy-user-2";
        
        // 用户1的敏感信息
        String user1Sensitive = "用户1的银行账号是1234567890，密码是secret123";
        CompletableFuture<String> user1Future = mem0.add(user1Sensitive, user1);
        String user1Id = user1Future.get();
        
        // 用户2的敏感信息
        String user2Sensitive = "用户2的信用卡号是9876543210，CVV是456";
        CompletableFuture<String> user2Future = mem0.add(user2Sensitive, user2);
        String user2Id = user2Future.get();
        
        logger.info("添加敏感信息 - 用户1: {}, 用户2: {}", user1Id, user2Id);
        
        // 验证用户隔离 - 用户1不能访问用户2的信息
        CompletableFuture<List<EnhancedMemory>> user1SearchFuture = mem0.search("信用卡", user1);
        List<EnhancedMemory> user1Results = user1SearchFuture.get();
        
        assertTrue("用户1不应该能搜索到用户2的信用卡信息", 
                   user1Results.isEmpty() || 
                   user1Results.stream().noneMatch(m -> m.getContent().contains("9876543210")));
        
        // 验证用户隔离 - 用户2不能访问用户1的信息  
        CompletableFuture<List<EnhancedMemory>> user2SearchFuture = mem0.search("银行", user2);
        List<EnhancedMemory> user2Results = user2SearchFuture.get();
        
        assertTrue("用户2不应该能搜索到用户1的银行信息",
                   user2Results.isEmpty() || 
                   user2Results.stream().noneMatch(m -> m.getContent().contains("1234567890")));
        
        // 验证用户可以访问自己的信息
        CompletableFuture<List<EnhancedMemory>> user1OwnSearchFuture = mem0.search("银行", user1);
        List<EnhancedMemory> user1OwnResults = user1OwnSearchFuture.get();
        
        assertFalse("用户1应该能搜索到自己的银行信息", user1OwnResults.isEmpty());
        assertTrue("应该包含用户1的银行账号",
                   user1OwnResults.stream().anyMatch(m -> m.getContent().contains("1234567890")));
        
        CompletableFuture<List<EnhancedMemory>> user2OwnSearchFuture = mem0.search("信用卡", user2);
        List<EnhancedMemory> user2OwnResults = user2OwnSearchFuture.get();
        
        assertFalse("用户2应该能搜索到自己的信用卡信息", user2OwnResults.isEmpty());
        assertTrue("应该包含用户2的信用卡号",
                   user2OwnResults.stream().anyMatch(m -> m.getContent().contains("9876543210")));
        
        // 验证获取所有记忆的隔离性
        CompletableFuture<List<EnhancedMemory>> user1AllFuture = mem0.getAll(user1);
        List<EnhancedMemory> user1All = user1AllFuture.get();
        
        assertTrue("用户1的所有记忆不应该包含用户2的信息",
                   user1All.stream().noneMatch(m -> m.getContent().contains("9876543210")));
        
        CompletableFuture<List<EnhancedMemory>> user2AllFuture = mem0.getAll(user2);  
        List<EnhancedMemory> user2All = user2AllFuture.get();
        
        assertTrue("用户2的所有记忆不应该包含用户1的信息",
                   user2All.stream().noneMatch(m -> m.getContent().contains("1234567890")));
        
        logger.info("记忆隐私和隔离测试完成");
    }
    
    @Test
    public void testMemoryConsistencyUnderConcurrentOperations() throws Exception {
        logger.info("测试并发操作下的记忆一致性");
        
        String userId = "concurrent-user";
        int numConcurrentOperations = 20;
        
        // 并发添加记忆
        CompletableFuture<String>[] addFutures = new CompletableFuture[numConcurrentOperations];
        
        for (int i = 0; i < numConcurrentOperations; i++) {
            final int index = i;
            addFutures[i] = CompletableFuture.supplyAsync(() -> {
                try {
                    String content = "并发记忆 " + index + " - 时间戳: " + System.currentTimeMillis();
                    return mem0.add(content, userId).get(30, TimeUnit.SECONDS);
                } catch (Exception e) {
                    logger.error("并发添加记忆失败: " + index, e);
                    throw new RuntimeException(e);
                }
            });
        }
        
        // 等待所有添加操作完成
        CompletableFuture.allOf(addFutures).get(60, TimeUnit.SECONDS);
        
        // 验证所有记忆都被成功添加
        CompletableFuture<List<EnhancedMemory>> allMemoriesFuture = mem0.getAll(userId);
        List<EnhancedMemory> allMemories = allMemoriesFuture.get();
        
        assertEquals("应该有正确数量的并发记忆", numConcurrentOperations, allMemories.size());
        
        // 验证每个记忆都有唯一ID
        long uniqueIds = allMemories.stream()
            .map(EnhancedMemory::getId)
            .distinct()
            .count();
        
        assertEquals("所有记忆ID应该都是唯一的", numConcurrentOperations, uniqueIds);
        
        // 并发搜索操作
        CompletableFuture<List<EnhancedMemory>>[] searchFutures = new CompletableFuture[10];
        
        for (int i = 0; i < 10; i++) {
            searchFutures[i] = CompletableFuture.supplyAsync(() -> {
                try {
                    return mem0.search("并发记忆", userId).get(30, TimeUnit.SECONDS);
                } catch (Exception e) {
                    logger.error("并发搜索失败", e);
                    throw new RuntimeException(e);
                }
            });
        }
        
        // 等待所有搜索完成并验证结果
        CompletableFuture.allOf(searchFutures).get(60, TimeUnit.SECONDS);
        
        for (CompletableFuture<List<EnhancedMemory>> searchFuture : searchFutures) {
            List<EnhancedMemory> searchResults = searchFuture.get();
            assertEquals("每次搜索都应该返回相同数量的结果", 
                        numConcurrentOperations, searchResults.size());
        }
        
        logger.info("并发操作记忆一致性测试完成");
    }
}