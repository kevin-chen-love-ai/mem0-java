package com.mem0.benchmark;

import com.mem0.memory.ConcurrentMemoryManager;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 测试数据生成器
 * 为基准测试生成各种类型的测试数据
 */
public class TestDataGenerator {
    
    // 预定义的文本模板
    private static final String[] TEXT_TEMPLATES = {
        "用户%s在%s进行了%s操作，结果是%s",
        "系统在%s记录了一条关于%s的信息：%s",
        "任务%s已于%s完成，状态为%s，备注：%s", 
        "会议纪要：%s会议在%s召开，主要讨论了%s，决定%s",
        "客户%s在%s提出了关于%s的问题，处理方案：%s",
        "项目%s的进度更新：%s阶段已完成，当前状态%s，下一步计划%s",
        "技术文档：%s模块的实现细节，采用%s技术，特点是%s，注意事项：%s",
        "用户反馈：关于%s功能的建议，用户认为%s，建议改进：%s，优先级：%s"
    };
    
    private static final String[] ENTITIES = {
        "张三", "李四", "王五", "赵六", "钱七", "孙八", "周九", "吴十",
        "用户系统", "订单模块", "支付服务", "消息队列", "数据库", "缓存层", "API网关", "监控系统",
        "登录功能", "搜索引擎", "推荐系统", "数据分析", "报表生成", "文件上传", "权限管理", "日志服务"
    };
    
    private static final String[] ACTIONS = {
        "创建", "更新", "删除", "查询", "导入", "导出", "同步", "备份", "恢复", "优化",
        "登录", "注销", "注册", "验证", "授权", "审批", "发布", "撤销", "暂停", "启动"
    };
    
    private static final String[] STATUSES = {
        "成功", "失败", "进行中", "待处理", "已完成", "已取消", "已暂停", "待审核", "已批准", "已拒绝"
    };
    
    private static final String[] TIME_EXPRESSIONS = {
        "今天上午", "昨天下午", "本周二", "上个月", "2024年1月", "春节期间", "周末", "工作日",
        "早晨8点", "中午12点", "下午3点", "晚上9点", "深夜时分", "凌晨时刻"
    };
    
    private static final String[] COMMON_QUERIES = {
        "如何使用", "什么是", "怎么配置", "为什么", "如何解决", "最佳实践", "常见问题",
        "性能优化", "安全设置", "故障排查", "升级方案", "兼容性", "扩展功能", "集成方案"
    };
    
    private final Random random = ThreadLocalRandom.current();

    /**
     * 生成指定长度的文本
     */
    public String generateText(int targetLength) {
        if (targetLength <= 0) {
            return "";
        }
        
        StringBuilder text = new StringBuilder();
        
        while (text.length() < targetLength) {
            // 选择模板和参数
            String template = TEXT_TEMPLATES[random.nextInt(TEXT_TEMPLATES.length)];
            String[] params = new String[4];
            
            for (int i = 0; i < 4; i++) {
                params[i] = generateRandomParam();
            }
            
            String sentence = String.format(template, (Object[]) params);
            
            if (text.length() + sentence.length() <= targetLength + 50) { // 允许50字符的误差
                if (text.length() > 0) {
                    text.append(" ");
                }
                text.append(sentence);
            } else {
                break;
            }
        }
        
        return text.toString();
    }

    /**
     * 生成用户ID
     */
    public String generateUserId() {
        return "user_" + random.nextInt(1000) + "_" + System.currentTimeMillis() % 10000;
    }

    /**
     * 生成搜索查询
     */
    public String generateSearchQuery() {
        String query = COMMON_QUERIES[random.nextInt(COMMON_QUERIES.length)];
        String entity = ENTITIES[random.nextInt(ENTITIES.length)];
        return query + entity;
    }

    /**
     * 生成内存创建请求列表
     */
    public List<ConcurrentMemoryManager.MemoryCreationRequest> generateCreationRequests(int count) {
        List<ConcurrentMemoryManager.MemoryCreationRequest> requests = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            String content = generateText(80 + random.nextInt(120)); // 80-200字符
            String userId = generateUserId();
            Map<String, Object> metadata = generateMetadata();
            
            requests.add(new ConcurrentMemoryManager.MemoryCreationRequest(content, userId, metadata));
        }
        
        return requests;
    }

    /**
     * 生成元数据
     */
    public Map<String, Object> generateMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        
        // 添加随机元数据
        metadata.put("category", ENTITIES[random.nextInt(ENTITIES.length)]);
        metadata.put("priority", random.nextInt(5) + 1);
        metadata.put("source", "test_generator");
        metadata.put("timestamp", System.currentTimeMillis());
        
        if (random.nextBoolean()) {
            metadata.put("tags", generateRandomTags());
        }
        
        if (random.nextBoolean()) {
            metadata.put("version", "v" + random.nextInt(5) + "." + random.nextInt(10));
        }
        
        return metadata;
    }

    /**
     * 生成常用查询列表
     */
    public List<String> generateCommonQueries(int count) {
        List<String> queries = new ArrayList<>();
        Set<String> usedQueries = new HashSet<>();
        
        while (queries.size() < count) {
            String query = generateSearchQuery();
            if (usedQueries.add(query)) {
                queries.add(query);
            }
        }
        
        return queries;
    }

    /**
     * 生成测试用户列表
     */
    public List<String> generateTestUsers(int count) {
        List<String> users = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            users.add("test_user_" + i + "_" + random.nextInt(1000));
        }
        
        return users;
    }

    /**
     * 生成变长文本（用于压力测试）
     */
    public String generateVariableLengthText() {
        int length = 50 + random.nextInt(500); // 50-550字符
        return generateText(length);
    }

    /**
     * 生成相似文本（用于测试去重和冲突解决）
     */
    public String generateSimilarText(String originalText, double similarityRatio) {
        if (originalText == null || originalText.isEmpty()) {
            return generateText(100);
        }
        
        String[] words = originalText.split("\\s+");
        List<String> wordList = new ArrayList<>(Arrays.asList(words));
        
        // 保留一定比例的原始词汇
        int keepCount = (int) (words.length * similarityRatio);
        Collections.shuffle(wordList);
        
        List<String> keptWords = wordList.subList(0, Math.min(keepCount, wordList.size()));
        
        // 添加一些新词汇
        int newWordsCount = words.length - keepCount;
        for (int i = 0; i < newWordsCount; i++) {
            keptWords.add(generateRandomParam());
        }
        
        Collections.shuffle(keptWords);
        return String.join(" ", keptWords);
    }

    /**
     * 生成批量相似文本
     */
    public List<String> generateSimilarTextBatch(String baseText, int count, double similarityRatio) {
        List<String> similarTexts = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            similarTexts.add(generateSimilarText(baseText, similarityRatio));
        }
        
        return similarTexts;
    }

    /**
     * 生成性能测试场景数据
     */
    public TestScenario generateTestScenario(String scenarioName, int operationCount) {
        return new TestScenario(
            scenarioName,
            generateCreationRequests(operationCount),
            generateCommonQueries(Math.min(operationCount / 10, 50)),
            generateTestUsers(Math.min(operationCount / 20, 100))
        );
    }

    // 私有辅助方法

    private String generateRandomParam() {
        switch (random.nextInt(4)) {
            case 0: return ENTITIES[random.nextInt(ENTITIES.length)];
            case 1: return ACTIONS[random.nextInt(ACTIONS.length)];
            case 2: return STATUSES[random.nextInt(STATUSES.length)];
            case 3: return TIME_EXPRESSIONS[random.nextInt(TIME_EXPRESSIONS.length)];
            default: return "随机参数" + random.nextInt(1000);
        }
    }

    private List<String> generateRandomTags() {
        List<String> tags = new ArrayList<>();
        int tagCount = 1 + random.nextInt(4); // 1-4个标签
        
        Set<String> usedTags = new HashSet<>();
        
        for (int i = 0; i < tagCount; i++) {
            String tag = ENTITIES[random.nextInt(ENTITIES.length)];
            if (usedTags.add(tag)) {
                tags.add(tag);
            }
        }
        
        return tags;
    }

    /**
     * 测试场景数据类
     */
    public static class TestScenario {
        private final String scenarioName;
        private final List<ConcurrentMemoryManager.MemoryCreationRequest> creationRequests;
        private final List<String> searchQueries;
        private final List<String> testUsers;

        public TestScenario(String scenarioName,
                          List<ConcurrentMemoryManager.MemoryCreationRequest> creationRequests,
                          List<String> searchQueries,
                          List<String> testUsers) {
            this.scenarioName = scenarioName;
            this.creationRequests = new ArrayList<>(creationRequests);
            this.searchQueries = new ArrayList<>(searchQueries);
            this.testUsers = new ArrayList<>(testUsers);
        }

        public String getScenarioName() { return scenarioName; }
        public List<ConcurrentMemoryManager.MemoryCreationRequest> getCreationRequests() { 
            return Collections.unmodifiableList(creationRequests); 
        }
        public List<String> getSearchQueries() { 
            return Collections.unmodifiableList(searchQueries); 
        }
        public List<String> getTestUsers() { 
            return Collections.unmodifiableList(testUsers); 
        }

        @Override
        public String toString() {
            return String.format("TestScenario{名称=%s, 创建请求=%d, 查询=%d, 用户=%d}",
                scenarioName, creationRequests.size(), searchQueries.size(), testUsers.size());
        }
    }
}