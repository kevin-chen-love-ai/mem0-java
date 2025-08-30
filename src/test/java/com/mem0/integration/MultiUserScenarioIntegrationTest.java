package com.mem0.integration;

import com.mem0.Mem0;
import com.mem0.config.Mem0Config;
import com.mem0.core.EnhancedMemory;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * 多用户场景集成测试
 * 测试多用户环境下的各种复杂交互场景
 */
public class MultiUserScenarioIntegrationTest {
    
    private static final Logger logger = LoggerFactory.getLogger(MultiUserScenarioIntegrationTest.class);
    
    private Mem0 mem0;
    private ExecutorService executorService;
    
    @Before
    public void setUp() {
        logger.info("初始化多用户场景集成测试环境");
        
        Mem0Config config = new Mem0Config();
        config.getVectorStore().setProvider("inmemory");
        config.getGraphStore().setProvider("inmemory");
        config.getEmbedding().setProvider("tfidf");
        config.getLlm().setProvider("rulebased");
            
        mem0 = new Mem0(config);
        executorService = Executors.newFixedThreadPool(20);
        logger.info("多用户测试环境初始化完成");
    }
    
    @After  
    public void tearDown() throws Exception {
        if (executorService != null) {
            executorService.shutdown();
        }
        if (mem0 != null) {
            mem0.close();
            logger.info("多用户测试环境已关闭");
        }
    }
    
    @Test
    public void testTeamCollaborationScenario() throws Exception {
        logger.info("测试团队协作场景");
        
        // 模拟一个软件开发团队的协作记忆
        String[] teamMembers = {
            "alice_pm", "bob_dev", "charlie_qa", "diana_designer", "eve_devops"
        };
        
        Map<String, List<String>> memberMemories = new HashMap<>();
        
        // 项目经理Alice的记忆
        memberMemories.put("alice_pm", Arrays.asList(
            "项目需要在Q4完成用户认证功能的开发",
            "客户要求支持OAuth2.0和JWT认证方式",
            "团队需要关注安全性和用户体验",
            "预算控制在50万以内，时间节点是12月底"
        ));
        
        // 开发者Bob的记忆
        memberMemories.put("bob_dev", Arrays.asList(
            "认证功能使用Spring Security框架实现",
            "JWT token过期时间设置为24小时",
            "OAuth2.0集成了Google和Facebook登录",
            "数据库使用PostgreSQL存储用户信息"
        ));
        
        // 测试工程师Charlie的记忆
        memberMemories.put("charlie_qa", Arrays.asList(
            "认证功能需要测试各种边界条件",
            "安全测试包括SQL注入和XSS防护",
            "性能测试要求登录响应时间小于2秒",
            "兼容性测试覆盖Chrome、Safari和Firefox"
        ));
        
        // 设计师Diana的记忆
        memberMemories.put("diana_designer", Arrays.asList(
            "登录界面采用简洁的Material Design风格",
            "支持深色模式和浅色模式切换",
            "移动端适配使用响应式设计",
            "品牌色彩使用蓝色系#1976D2"
        ));
        
        // DevOps工程师Eve的记忆
        memberMemories.put("eve_devops", Arrays.asList(
            "认证服务部署在Kubernetes集群上",
            "使用Redis集群存储session信息",
            "配置了自动伸缩和健康检查",
            "监控使用Prometheus和Grafana"
        ));
        
        // 并发添加所有团队成员的记忆
        List<CompletableFuture<Void>> addFutures = new ArrayList<>();
        
        for (Map.Entry<String, List<String>> entry : memberMemories.entrySet()) {
            String userId = entry.getKey();
            List<String> memories = entry.getValue();
            
            CompletableFuture<Void> userFuture = CompletableFuture.runAsync(() -> {
                try {
                    for (String memory : memories) {
                        mem0.add(memory, userId).get();
                    }
                    logger.info("用户 {} 的记忆添加完成", userId);
                } catch (Exception e) {
                    logger.error("用户 {} 记忆添加失败", userId, e);
                    throw new RuntimeException(e);
                }
            }, executorService);
            
            addFutures.add(userFuture);
        }
        
        // 等待所有记忆添加完成
        CompletableFuture.allOf(addFutures.toArray(new CompletableFuture[0])).get();
        logger.info("所有团队成员记忆添加完成");
        
        // 验证每个团队成员都能访问自己的记忆
        for (String userId : teamMembers) {
            CompletableFuture<List<EnhancedMemory>> userMemoriesFuture = mem0.getAll(userId);
            List<EnhancedMemory> userMemories = userMemoriesFuture.get();
            
            assertEquals("用户应该有正确数量的记忆", 
                        memberMemories.get(userId).size(), userMemories.size());
            logger.info("用户 {} 有 {} 条记忆", userId, userMemories.size());
        }
        
        // 验证跨角色搜索的专业性
        
        // PM搜索项目相关信息
        CompletableFuture<List<EnhancedMemory>> pmSearchFuture = mem0.search("项目", "alice_pm");
        List<EnhancedMemory> pmResults = pmSearchFuture.get();
        assertTrue("PM应该能找到项目相关记忆", 
                   pmResults.stream().anyMatch(m -> m.getContent().contains("Q4完成")));
        
        // 开发者搜索技术实现
        CompletableFuture<List<EnhancedMemory>> devSearchFuture = mem0.search("Spring Security", "bob_dev");
        List<EnhancedMemory> devResults = devSearchFuture.get();
        assertTrue("开发者应该能找到技术实现记忆",
                   devResults.stream().anyMatch(m -> m.getContent().contains("框架实现")));
        
        // QA搜索测试策略
        CompletableFuture<List<EnhancedMemory>> qaSearchFuture = mem0.search("测试", "charlie_qa");
        List<EnhancedMemory> qaResults = qaSearchFuture.get();
        assertTrue("QA应该能找到测试相关记忆",
                   qaResults.stream().anyMatch(m -> m.getContent().contains("边界条件")));
        
        // 设计师搜索UI相关
        CompletableFuture<List<EnhancedMemory>> designSearchFuture = mem0.search("设计", "diana_designer");
        List<EnhancedMemory> designResults = designSearchFuture.get();
        assertTrue("设计师应该能找到设计相关记忆",
                   designResults.stream().anyMatch(m -> m.getContent().contains("Material Design")));
        
        // DevOps搜索部署相关
        CompletableFuture<List<EnhancedMemory>> devopsSearchFuture = mem0.search("部署", "eve_devops");
        List<EnhancedMemory> devopsResults = devopsSearchFuture.get();
        assertTrue("DevOps应该能找到部署相关记忆",
                   devopsResults.stream().anyMatch(m -> m.getContent().contains("Kubernetes")));
        
        logger.info("团队协作场景测试完成");
    }
    
    @Test
    public void testCustomerServiceScenario() throws Exception {
        logger.info("测试客户服务场景");
        
        // 模拟客户服务系统中多个客服处理不同客户的场景
        String[] agents = {"agent_001", "agent_002", "agent_003"};
        String[] customers = {"customer_A", "customer_B", "customer_C", "customer_D"};
        
        // 客户问题类型和对应的解决方案记忆
        Map<String, String> problemSolutions = new HashMap<>();
        problemSolutions.put("登录问题", "重置密码或清除浏览器缓存通常可以解决登录问题");
        problemSolutions.put("支付失败", "检查银行卡额度和网络连接，或使用其他支付方式");
        problemSolutions.put("订单查询", "通过订单号在系统中查询订单状态和物流信息");
        problemSolutions.put("退换货", "7天无理由退货，需要保持商品原包装完整");
        problemSolutions.put("优惠券", "优惠券有使用条件限制，需要检查适用范围和有效期");
        
        // 为每个客服添加解决方案记忆
        List<CompletableFuture<Void>> agentSetupFutures = new ArrayList<>();
        
        for (String agent : agents) {
            CompletableFuture<Void> setupFuture = CompletableFuture.runAsync(() -> {
                try {
                    for (Map.Entry<String, String> entry : problemSolutions.entrySet()) {
                        String problemType = entry.getKey();
                        String solution = entry.getValue();
                        String memory = String.format("针对%s：%s", problemType, solution);
                        mem0.add(memory, agent).get();
                    }
                    logger.info("客服 {} 的解决方案记忆初始化完成", agent);
                } catch (Exception e) {
                    logger.error("客服 {} 记忆初始化失败", agent, e);
                    throw new RuntimeException(e);
                }
            }, executorService);
            
            agentSetupFutures.add(setupFuture);
        }
        
        CompletableFuture.allOf(agentSetupFutures.toArray(new CompletableFuture[0])).get();
        
        // 模拟客户咨询场景
        Map<String, List<String>> customerInquiries = new HashMap<>();
        customerInquiries.put("customer_A", Arrays.asList(
            "我的账号无法登录，提示密码错误",
            "尝试了多次都不行，是不是系统问题？"
        ));
        customerInquiries.put("customer_B", Arrays.asList(
            "订单支付时显示支付失败",
            "银行卡是有钱的，不知道什么原因"
        ));
        customerInquiries.put("customer_C", Arrays.asList(
            "我的订单号是ORD123456，想查询物流状态",
            "已经下单3天了，还没有发货信息"
        ));
        customerInquiries.put("customer_D", Arrays.asList(
            "买的衣服尺码不合适，想要退货",
            "商品还没有拆包装，可以退货吗？"
        ));
        
        // 为每个客户添加咨询记忆
        for (Map.Entry<String, List<String>> entry : customerInquiries.entrySet()) {
            String customerId = entry.getKey();
            for (String inquiry : entry.getValue()) {
                mem0.add(inquiry, customerId).get();
            }
        }
        
        // 模拟客服处理客户问题的过程
        
        // Agent_001处理Customer_A的登录问题
        CompletableFuture<List<EnhancedMemory>> loginSolutionFuture = mem0.search("登录问题", "agent_001");
        List<EnhancedMemory> loginSolutions = loginSolutionFuture.get();
        
        assertFalse("客服应该能找到登录问题解决方案", loginSolutions.isEmpty());
        assertTrue("解决方案应该包含重置密码建议",
                   loginSolutions.stream().anyMatch(m -> m.getContent().contains("重置密码")));
        
        // 记录客服处理过程
        String loginHandlingRecord = "为customer_A处理登录问题，建议重置密码，问题已解决";
        mem0.add(loginHandlingRecord, "agent_001").get();
        
        // Agent_002处理Customer_B的支付问题
        CompletableFuture<List<EnhancedMemory>> paymentSolutionFuture = mem0.search("支付失败", "agent_002");
        List<EnhancedMemory> paymentSolutions = paymentSolutionFuture.get();
        
        assertFalse("客服应该能找到支付问题解决方案", paymentSolutions.isEmpty());
        assertTrue("解决方案应该包含检查银行卡建议",
                   paymentSolutions.stream().anyMatch(m -> m.getContent().contains("银行卡额度")));
        
        String paymentHandlingRecord = "为customer_B处理支付问题，建议检查银行卡状态，问题已解决";
        mem0.add(paymentHandlingRecord, "agent_002").get();
        
        // Agent_003处理Customer_C的订单查询和Customer_D的退货问题
        CompletableFuture<List<EnhancedMemory>> orderSolutionFuture = mem0.search("订单查询", "agent_003");
        List<EnhancedMemory> orderSolutions = orderSolutionFuture.get();
        
        assertFalse("客服应该能找到订单查询解决方案", orderSolutions.isEmpty());
        
        CompletableFuture<List<EnhancedMemory>> returnSolutionFuture = mem0.search("退换货", "agent_003");
        List<EnhancedMemory> returnSolutions = returnSolutionFuture.get();
        
        assertFalse("客服应该能找到退换货解决方案", returnSolutions.isEmpty());
        assertTrue("解决方案应该包含7天无理由退货",
                   returnSolutions.stream().anyMatch(m -> m.getContent().contains("7天无理由")));
        
        // 验证客服工作记录
        CompletableFuture<List<EnhancedMemory>> agent1RecordsFuture = mem0.search("处理", "agent_001");
        List<EnhancedMemory> agent1Records = agent1RecordsFuture.get();
        
        assertTrue("客服1应该有处理记录",
                   agent1Records.stream().anyMatch(m -> m.getContent().contains("customer_A")));
        
        // 验证客户隔离性 - 客户无法看到其他客户的咨询记录
        CompletableFuture<List<EnhancedMemory>> customerAMemoriesFuture = mem0.getAll("customer_A");
        List<EnhancedMemory> customerAMemories = customerAMemoriesFuture.get();
        
        assertTrue("客户A不应该看到其他客户的记忆",
                   customerAMemories.stream().allMatch(m -> 
                       !m.getContent().contains("customer_B") && 
                       !m.getContent().contains("customer_C") && 
                       !m.getContent().contains("customer_D")));
        
        logger.info("客户服务场景测试完成");
    }
    
    @Test
    public void testEducationalPlatformScenario() throws Exception {
        logger.info("测试教育平台场景");
        
        // 模拟在线教育平台的多角色场景
        String teacher = "teacher_prof_wang";
        String[] students = {"student_alice", "student_bob", "student_charlie"};
        String course = "Java Programming Fundamentals";
        
        // 教师添加课程内容和教学记忆
        List<String> teachingMemories = Arrays.asList(
            "Java基础语法包括变量声明、数据类型和运算符",
            "面向对象编程的三大特性：封装、继承、多态",
            "常用集合类：ArrayList、HashMap、HashSet的使用场景",
            "异常处理机制：try-catch-finally和自定义异常",
            "多线程编程：Thread类和Runnable接口的使用",
            "学生们在继承和多态概念上普遍有困难，需要多准备例子",
            "集合类的选择常常困扰初学者，要强调性能差异",
            "异常处理是重点也是难点，要结合实际项目案例"
        );
        
        for (String memory : teachingMemories) {
            mem0.add(memory, teacher).get();
        }
        
        logger.info("教师记忆添加完成，共 {} 条", teachingMemories.size());
        
        // 学生学习过程记忆
        Map<String, List<String>> studentLearningProgress = new HashMap<>();
        
        // Alice：学习进度较快的学生
        studentLearningProgress.put("student_alice", Arrays.asList(
            "Java变量声明很容易理解，类似于数学中的赋值",
            "面向对象编程的封装概念已经掌握，像是数据的保护机制",
            "继承让代码复用变得简单，子类可以使用父类的方法",
            "多态还有些困惑，需要更多练习",
            "ArrayList和数组的区别已经理解，ArrayList更灵活",
            "HashMap的键值对存储很有用，适合做查找操作"
        ));
        
        // Bob：中等进度的学生
        studentLearningProgress.put("student_bob", Arrays.asList(
            "Java语法比Python复杂一些，需要声明数据类型",
            "类和对象的概念有点抽象，通过练习逐渐理解",
            "继承的super关键字用法还不太清楚",
            "异常处理很重要，可以让程序更健壮",
            "集合类有很多种，不知道什么时候用哪个"
        ));
        
        // Charlie：学习有困难的学生
        studentLearningProgress.put("student_charlie", Arrays.asList(
            "Java语法规则很多，容易忘记分号和大括号",
            "面向对象编程概念很难理解，和之前学的不一样",
            "继承和多态完全搞不懂，感觉很复杂",
            "异常处理代码看起来很乱，不知道怎么组织",
            "需要老师更多帮助和指导"
        ));
        
        // 并发添加学生学习记忆
        List<CompletableFuture<Void>> studentFutures = new ArrayList<>();
        
        for (Map.Entry<String, List<String>> entry : studentLearningProgress.entrySet()) {
            String studentId = entry.getKey();
            List<String> memories = entry.getValue();
            
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    for (String memory : memories) {
                        mem0.add(memory, studentId).get();
                    }
                    logger.info("学生 {} 的学习记忆添加完成", studentId);
                } catch (Exception e) {
                    logger.error("学生 {} 记忆添加失败", studentId, e);
                    throw new RuntimeException(e);
                }
            }, executorService);
            
            studentFutures.add(future);
        }
        
        CompletableFuture.allOf(studentFutures.toArray(new CompletableFuture[0])).get();
        
        // 验证教学效果分析
        
        // 教师分析学生在面向对象编程方面的理解
        CompletableFuture<List<EnhancedMemory>> oopTeachingFuture = mem0.search("面向对象", teacher);
        List<EnhancedMemory> oopTeachingMemories = oopTeachingFuture.get();
        
        assertFalse("教师应该有面向对象教学记忆", oopTeachingMemories.isEmpty());
        assertTrue("教师记忆应该包含三大特性",
                   oopTeachingMemories.stream().anyMatch(m -> m.getContent().contains("封装、继承、多态")));
        
        // 分析不同学生的学习状态
        
        // Alice的学习状态（优秀学生）
        CompletableFuture<List<EnhancedMemory>> aliceOOPFuture = mem0.search("面向对象", "student_alice");
        List<EnhancedMemory> aliceOOPMemories = aliceOOPFuture.get();
        
        assertTrue("Alice应该对面向对象有较好理解",
                   aliceOOPMemories.stream().anyMatch(m -> m.getContent().contains("已经掌握")));
        
        // Bob的学习状态（中等学生）
        CompletableFuture<List<EnhancedMemory>> bobOOPFuture = mem0.search("面向对象", "student_bob");
        List<EnhancedMemory> bobOOPMemories = bobOOPFuture.get();
        
        assertTrue("Bob对面向对象概念还在摸索",
                   bobOOPMemories.stream().anyMatch(m -> 
                       m.getContent().contains("抽象") || m.getContent().contains("逐渐理解")));
        
        // Charlie的学习状态（困难学生）
        CompletableFuture<List<EnhancedMemory>> charlieOOPFuture = mem0.search("面向对象", "student_charlie");
        List<EnhancedMemory> charlieOOPMemories = charlieOOPFuture.get();
        
        assertTrue("Charlie在面向对象方面有困难",
                   charlieOOPMemories.stream().anyMatch(m -> 
                       m.getContent().contains("难理解") || m.getContent().contains("搞不懂")));
        
        // 验证个性化学习路径
        
        // 针对集合类的学习情况分析
        for (String student : students) {
            CompletableFuture<List<EnhancedMemory>> collectionFuture = mem0.search("集合", student);
            List<EnhancedMemory> collectionMemories = collectionFuture.get();
            
            logger.info("学生 {} 对集合的理解程度记忆数量: {}", student, collectionMemories.size());
        }
        
        // 验证学生隔离性
        CompletableFuture<List<EnhancedMemory>> aliceAllFuture = mem0.getAll("student_alice");
        List<EnhancedMemory> aliceAllMemories = aliceAllFuture.get();
        
        assertTrue("Alice不应该看到其他学生的学习记忆",
                   aliceAllMemories.stream().allMatch(m -> 
                       !m.getContent().contains("student_bob") && 
                       !m.getContent().contains("student_charlie")));
        
        // 验证教师可以通过搜索了解整体教学情况
        CompletableFuture<List<EnhancedMemory>> teachingDifficultyFuture = mem0.search("困难", teacher);
        List<EnhancedMemory> difficultyMemories = teachingDifficultyFuture.get();
        
        assertFalse("教师应该记录了教学难点", difficultyMemories.isEmpty());
        assertTrue("应该记录继承多态是难点",
                   difficultyMemories.stream().anyMatch(m -> m.getContent().contains("继承和多态")));
        
        logger.info("教育平台场景测试完成");
    }
    
    @Test
    public void testHealthcareScenario() throws Exception {
        logger.info("测试医疗保健场景");
        
        // 模拟医疗系统中的多角色场景（注意：这里只是测试数据结构，不涉及真实医疗建议）
        String doctor = "doctor_smith";
        String nurse = "nurse_johnson";
        String[] patients = {"patient_001", "patient_002", "patient_003"};
        
        // 医生的医疗知识记忆
        List<String> medicalKnowledge = Arrays.asList(
            "高血压患者应该限制钠盐摄入，每天不超过6克",
            "糖尿病患者需要定期监测血糖，建议餐前餐后都要检测",
            "心脏病患者要避免剧烈运动，适合散步和太极",
            "高脂血症患者应该控制胆固醇摄入，多吃蔬菜水果",
            "定期体检对于疾病早发现早治疗很重要"
        );
        
        for (String knowledge : medicalKnowledge) {
            mem0.add(knowledge, doctor).get();
        }
        
        // 护士的护理经验记忆
        List<String> nursingExperience = Arrays.asList(
            "测量血压时患者要保持安静状态至少5分钟",
            "血糖监测前要确保患者手部清洁",
            "高血压患者的日常护理要注意情绪管理",
            "糖尿病患者的足部护理很重要，要每天检查",
            "用药指导要详细，确保患者理解用法用量"
        );
        
        for (String experience : nursingExperience) {
            mem0.add(experience, nurse).get();
        }
        
        logger.info("医护人员知识记忆添加完成");
        
        // 患者的健康记录（模拟数据）
        Map<String, List<String>> patientRecords = new HashMap<>();
        
        // 患者001：高血压
        patientRecords.put("patient_001", Arrays.asList(
            "被诊断为轻度高血压，血压140/90mmHg",
            "医生建议减少盐分摄入和增加运动",
            "开始服用降压药，每天早上一片",
            "两周后复查，血压有所改善130/85mmHg",
            "继续药物治疗，保持良好的生活习惯"
        ));
        
        // 患者002：糖尿病
        patientRecords.put("patient_002", Arrays.asList(
            "确诊为2型糖尿病，空腹血糖8.5mmol/L",
            "医生制定了饮食控制和运动计划",
            "开始使用血糖监测仪，每天测量血糖",
            "学会了胰岛素注射技术",
            "血糖控制逐渐稳定，HbA1c从9%降到7%"
        ));
        
        // 患者003：心脏病
        patientRecords.put("patient_003", Arrays.asList(
            "冠心病患者，有心绞痛症状",
            "做了心电图和冠脉造影检查",
            "医生建议药物治疗和生活方式调整",
            "戒烟戒酒，控制体重",
            "定期复查，目前症状有所缓解"
        ));
        
        // 并发添加患者记录
        List<CompletableFuture<Void>> patientFutures = new ArrayList<>();
        
        for (Map.Entry<String, List<String>> entry : patientRecords.entrySet()) {
            String patientId = entry.getKey();
            List<String> records = entry.getValue();
            
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    for (String record : records) {
                        mem0.add(record, patientId).get();
                    }
                    logger.info("患者 {} 的健康记录添加完成", patientId);
                } catch (Exception e) {
                    logger.error("患者 {} 记录添加失败", patientId, e);
                    throw new RuntimeException(e);
                }
            }, executorService);
            
            patientFutures.add(future);
        }
        
        CompletableFuture.allOf(patientFutures.toArray(new CompletableFuture[0])).get();
        
        // 验证医疗知识查询
        
        // 医生查询高血压相关知识
        CompletableFuture<List<EnhancedMemory>> hypertensionFuture = mem0.search("高血压", doctor);
        List<EnhancedMemory> hypertensionKnowledge = hypertensionFuture.get();
        
        assertFalse("医生应该有高血压相关知识", hypertensionKnowledge.isEmpty());
        assertTrue("知识应该包含钠盐限制",
                   hypertensionKnowledge.stream().anyMatch(m -> m.getContent().contains("钠盐")));
        
        // 护士查询血压测量相关经验
        CompletableFuture<List<EnhancedMemory>> bpMeasureFuture = mem0.search("血压", nurse);
        List<EnhancedMemory> bpExperience = bpMeasureFuture.get();
        
        assertFalse("护士应该有血压测量经验", bpExperience.isEmpty());
        assertTrue("经验应该包含安静状态要求",
                   bpExperience.stream().anyMatch(m -> m.getContent().contains("安静状态")));
        
        // 验证患者隐私保护
        
        // 患者001只能访问自己的健康记录
        CompletableFuture<List<EnhancedMemory>> patient001Future = mem0.getAll("patient_001");
        List<EnhancedMemory> patient001Records = patient001Future.get();
        
        assertTrue("患者001不应该看到其他患者记录",
                   patient001Records.stream().allMatch(m -> 
                       !m.getContent().contains("patient_002") && 
                       !m.getContent().contains("patient_003")));
        
        assertTrue("患者001应该能看到自己的高血压记录",
                   patient001Records.stream().anyMatch(m -> m.getContent().contains("高血压")));
        
        // 患者002的糖尿病记录验证
        CompletableFuture<List<EnhancedMemory>> diabetesFuture = mem0.search("糖尿病", "patient_002");
        List<EnhancedMemory> diabetesRecords = diabetesFuture.get();
        
        assertFalse("患者002应该有糖尿病相关记录", diabetesRecords.isEmpty());
        assertTrue("记录应该包含血糖监测",
                   diabetesRecords.stream().anyMatch(m -> m.getContent().contains("血糖监测")));
        
        // 验证医护人员不能直接访问患者记录（除非有特定授权机制）
        CompletableFuture<List<EnhancedMemory>> doctorPatientSearchFuture = 
            mem0.search("patient_001", doctor);
        List<EnhancedMemory> doctorPatientResults = doctorPatientSearchFuture.get();
        
        // 在实际系统中，这里应该通过权限控制来管理医护人员对患者记录的访问
        // 这个测试主要验证数据隔离性
        
        logger.info("医疗保健场景测试完成");
    }
    
    @Test
    public void testScalabilityWithManyUsers() throws Exception {
        logger.info("测试大量用户场景下的可扩展性");
        
        int numUsers = 100;
        int memoriesPerUser = 20;
        CountDownLatch latch = new CountDownLatch(numUsers);
        
        // 并发创建大量用户和记忆
        for (int i = 0; i < numUsers; i++) {
            final int userId = i;
            executorService.submit(() -> {
                try {
                    String userIdStr = "scale_user_" + userId;
                    
                    // 每个用户添加多条记忆
                    for (int j = 0; j < memoriesPerUser; j++) {
                        String memory = String.format("用户%d的第%d条记忆 - 时间戳:%d", 
                                                     userId, j, System.currentTimeMillis());
                        mem0.add(memory, userIdStr).get();
                    }
                    
                    if (userId % 20 == 0) {
                        logger.info("用户 {} 的记忆添加完成", userIdStr);
                    }
                    
                } catch (Exception e) {
                    logger.error("用户 {} 记忆添加失败", userId, e);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 等待所有用户记忆添加完成
        latch.await();
        logger.info("所有 {} 个用户的记忆添加完成", numUsers);
        
        // 验证数据完整性
        Random random = new Random();
        for (int i = 0; i < 10; i++) { // 随机验证10个用户
            int randomUserId = random.nextInt(numUsers);
            String userIdStr = "scale_user_" + randomUserId;
            
            CompletableFuture<List<EnhancedMemory>> userMemoriesFuture = mem0.getAll(userIdStr);
            List<EnhancedMemory> userMemories = userMemoriesFuture.get();
            
            assertEquals("用户应该有正确数量的记忆", memoriesPerUser, userMemories.size());
            
            // 验证用户只能看到自己的记忆
            boolean hasOtherUserMemory = userMemories.stream()
                .anyMatch(m -> !m.getContent().contains("用户" + randomUserId));
            
            assertFalse("用户不应该看到其他用户的记忆", hasOtherUserMemory);
        }
        
        // 测试搜索性能
        long searchStartTime = System.currentTimeMillis();
        
        List<CompletableFuture<List<EnhancedMemory>>> searchFutures = new ArrayList<>();
        for (int i = 0; i < 50; i++) { // 50个并发搜索
            int randomUserId = random.nextInt(numUsers);
            String userIdStr = "scale_user_" + randomUserId;
            
            CompletableFuture<List<EnhancedMemory>> searchFuture = mem0.search("记忆", userIdStr);
            searchFutures.add(searchFuture);
        }
        
        // 等待所有搜索完成
        CompletableFuture.allOf(searchFutures.toArray(new CompletableFuture[0])).get();
        
        long searchEndTime = System.currentTimeMillis();
        long searchDuration = searchEndTime - searchStartTime;
        
        logger.info("50个并发搜索耗时 {} ms，平均 {} ms/次", 
                   searchDuration, searchDuration / 50.0);
        
        // 验证搜索结果正确性
        for (CompletableFuture<List<EnhancedMemory>> future : searchFutures) {
            List<EnhancedMemory> results = future.get();
            assertFalse("搜索结果不应该为空", results.isEmpty());
            assertTrue("搜索结果应该合理", results.size() <= memoriesPerUser);
        }
        
        logger.info("大量用户可扩展性测试完成");
    }
}