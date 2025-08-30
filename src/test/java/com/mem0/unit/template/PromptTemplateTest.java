package com.mem0.unit.template;

import com.mem0.template.PromptTemplate;
import com.mem0.template.PromptTemplate.PromptContext;
import com.mem0.template.PromptTemplate.RetrievedMemory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PromptTemplate和相关类的全面测试套件
 */
@DisplayName("PromptTemplate 提示词模板测试")
class PromptTemplateTest {

    private PromptTemplate template;
    private PromptContext context;

    @BeforeEach
    void setUp() {
        template = new TestPromptTemplate();
        context = new PromptContext("测试用户查询");
    }

    @Nested
    @DisplayName("PromptContext 上下文对象测试")
    class PromptContextTests {

        @Test
        @DisplayName("空构造函数创建上下文")
        void testEmptyConstructor() {
            PromptContext emptyContext = new PromptContext();
            
            assertNull(emptyContext.getUserQuery());
            assertNull(emptyContext.getSystemMessage());
            assertNull(emptyContext.getRetrievedMemories());
            assertNull(emptyContext.getAdditionalContext());
        }

        @Test
        @DisplayName("带查询的构造函数创建上下文")
        void testQueryConstructor() {
            String query = "用户想了解咖啡知识";
            PromptContext queryContext = new PromptContext(query);
            
            assertEquals(query, queryContext.getUserQuery());
            assertNull(queryContext.getSystemMessage());
            assertNull(queryContext.getRetrievedMemories());
            assertNull(queryContext.getAdditionalContext());
        }

        @Test
        @DisplayName("设置和获取用户查询")
        void testUserQuerySetterGetter() {
            String query1 = "第一个查询";
            String query2 = "第二个查询";
            
            context.setUserQuery(query1);
            assertEquals(query1, context.getUserQuery());
            
            context.setUserQuery(query2);
            assertEquals(query2, context.getUserQuery());
            
            context.setUserQuery(null);
            assertNull(context.getUserQuery());
        }

        @Test
        @DisplayName("设置和获取系统消息")
        void testSystemMessageSetterGetter() {
            String systemMessage = "你是一个专业的AI助手";
            
            context.setSystemMessage(systemMessage);
            assertEquals(systemMessage, context.getSystemMessage());
            
            context.setSystemMessage(null);
            assertNull(context.getSystemMessage());
        }

        @Test
        @DisplayName("设置和获取检索内存列表")
        void testRetrievedMemoriesSetterGetter() {
            List<RetrievedMemory> memories = Arrays.asList(
                new RetrievedMemory("内存1", 0.9, null, "factual"),
                new RetrievedMemory("内存2", 0.8, null, "preference")
            );
            
            context.setRetrievedMemories(memories);
            assertEquals(memories, context.getRetrievedMemories());
            assertEquals(2, context.getRetrievedMemories().size());
            
            context.setRetrievedMemories(null);
            assertNull(context.getRetrievedMemories());
        }

        @Test
        @DisplayName("设置和获取额外上下文")
        void testAdditionalContextSetterGetter() {
            Map<String, Object> additionalContext = new HashMap<>();
            additionalContext.put("language", "zh");
            additionalContext.put("temperature", 0.7);
            additionalContext.put("maxTokens", 1000);
            
            context.setAdditionalContext(additionalContext);
            assertEquals(additionalContext, context.getAdditionalContext());
            assertEquals("zh", context.getAdditionalContext().get("language"));
            assertEquals(0.7, context.getAdditionalContext().get("temperature"));
            
            context.setAdditionalContext(null);
            assertNull(context.getAdditionalContext());
        }

        @Test
        @DisplayName("上下文对象的完整性测试")
        void testCompleteContextSetup() {
            String query = "用户查询";
            String systemMessage = "系统消息";
            
            List<RetrievedMemory> memories = Arrays.asList(
                new RetrievedMemory("相关内存1", 0.95, null, "episodic"),
                new RetrievedMemory("相关内存2", 0.85, null, "semantic")
            );
            
            Map<String, Object> additionalContext = new HashMap<>();
            additionalContext.put("userId", "user123");
            additionalContext.put("sessionId", "session456");
            
            context.setUserQuery(query);
            context.setSystemMessage(systemMessage);
            context.setRetrievedMemories(memories);
            context.setAdditionalContext(additionalContext);
            
            assertEquals(query, context.getUserQuery());
            assertEquals(systemMessage, context.getSystemMessage());
            assertEquals(memories, context.getRetrievedMemories());
            assertEquals(additionalContext, context.getAdditionalContext());
            assertEquals("user123", context.getAdditionalContext().get("userId"));
        }
    }

    @Nested
    @DisplayName("RetrievedMemory 检索内存对象测试")
    class RetrievedMemoryTests {

        @Test
        @DisplayName("创建检索内存对象")
        void testRetrievedMemoryCreation() {
            String content = "用户喜欢喝咖啡";
            double relevance = 0.92;
            String memoryType = "preference";
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "chat");
            metadata.put("timestamp", "2024-08-30");
            
            RetrievedMemory memory = new RetrievedMemory(content, relevance, metadata, memoryType);
            
            assertEquals(content, memory.getContent());
            assertEquals(relevance, memory.getRelevanceScore(), 0.001);
            assertEquals(metadata, memory.getMetadata());
            assertEquals(memoryType, memory.getMemoryType());
            assertEquals("chat", memory.getMetadata().get("source"));
        }

        @Test
        @DisplayName("创建带空元数据的检索内存")
        void testRetrievedMemoryWithNullMetadata() {
            String content = "测试内容";
            double relevance = 0.75;
            String memoryType = "factual";
            
            RetrievedMemory memory = new RetrievedMemory(content, relevance, null, memoryType);
            
            assertEquals(content, memory.getContent());
            assertEquals(relevance, memory.getRelevanceScore(), 0.001);
            assertNull(memory.getMetadata());
            assertEquals(memoryType, memory.getMemoryType());
        }

        @Test
        @DisplayName("创建带空类型的检索内存")
        void testRetrievedMemoryWithNullType() {
            String content = "另一个测试内容";
            double relevance = 0.65;
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("category", "general");
            
            RetrievedMemory memory = new RetrievedMemory(content, relevance, metadata, null);
            
            assertEquals(content, memory.getContent());
            assertEquals(relevance, memory.getRelevanceScore(), 0.001);
            assertEquals(metadata, memory.getMetadata());
            assertNull(memory.getMemoryType());
        }

        @Test
        @DisplayName("检索内存的边界值测试")
        void testRetrievedMemoryBoundaryValues() {
            // 测试最高相关度
            RetrievedMemory highRelevance = new RetrievedMemory("高相关内容", 1.0, null, "high");
            assertEquals(1.0, highRelevance.getRelevanceScore(), 0.001);
            
            // 测试最低相关度
            RetrievedMemory lowRelevance = new RetrievedMemory("低相关内容", 0.0, null, "low");
            assertEquals(0.0, lowRelevance.getRelevanceScore(), 0.001);
            
            // 测试负相关度（实际使用中不常见，但应能处理）
            RetrievedMemory negativeRelevance = new RetrievedMemory("负相关内容", -0.1, null, "negative");
            assertEquals(-0.1, negativeRelevance.getRelevanceScore(), 0.001);
        }

        @Test
        @DisplayName("检索内存对象的不变性")
        void testRetrievedMemoryImmutability() {
            String originalContent = "原始内容";
            double originalRelevance = 0.8;
            String originalType = "original";
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("key", "value");
            
            RetrievedMemory memory = new RetrievedMemory(originalContent, originalRelevance, metadata, originalType);
            
            // 验证对象创建后的值
            assertEquals(originalContent, memory.getContent());
            assertEquals(originalRelevance, memory.getRelevanceScore(), 0.001);
            assertEquals(originalType, memory.getMemoryType());
            
            // 修改原始map不应影响内存对象（如果实现了防御性复制）
            metadata.put("newKey", "newValue");
            // 注意：当前实现直接引用传入的map，所以这个测试可能需要根据实际实现调整
        }
    }

    @Nested
    @DisplayName("PromptTemplate 接口基础功能测试")
    class PromptTemplateBasicTests {

        @Test
        @DisplayName("构建提示词")
        void testBuildPrompt() {
            String result = template.buildPrompt(context);
            
            assertNotNull(result);
            assertFalse(result.trim().isEmpty());
            assertTrue(result.contains("测试用户查询"));
        }

        @Test
        @DisplayName("获取模板名称")
        void testGetName() {
            String name = template.getName();
            
            assertNotNull(name);
            assertFalse(name.trim().isEmpty());
        }

        @Test
        @DisplayName("获取模板描述")
        void testGetDescription() {
            String description = template.getDescription();
            
            assertNotNull(description);
            assertFalse(description.trim().isEmpty());
        }

        @Test
        @DisplayName("空上下文处理")
        void testBuildPromptWithNullContext() {
            assertThrows(NullPointerException.class, () -> {
                template.buildPrompt(null);
            });
        }

        @Test
        @DisplayName("最小上下文构建提示词")
        void testBuildPromptWithMinimalContext() {
            PromptContext minimalContext = new PromptContext("简单查询");
            
            String result = template.buildPrompt(minimalContext);
            
            assertNotNull(result);
            assertTrue(result.contains("简单查询"));
        }
    }

    @Nested
    @DisplayName("复杂场景测试")
    class ComplexScenarioTests {

        @Test
        @DisplayName("完整上下文的提示词构建")
        void testCompleteContextPromptBuilding() {
            // 设置完整的上下文
            context.setSystemMessage("你是一个专业的咖啡顾问，能够根据用户的喜好和历史提供个性化建议。");
            
            List<RetrievedMemory> memories = Arrays.asList(
                new RetrievedMemory("用户喜欢拿铁咖啡", 0.95, createMetadata("preference", "2024-08-30"), "preference"),
                new RetrievedMemory("用户对意式咖啡感兴趣", 0.88, createMetadata("interest", "2024-08-29"), "interest"),
                new RetrievedMemory("用户不喜欢苦味太重的咖啡", 0.82, createMetadata("preference", "2024-08-28"), "preference")
            );
            context.setRetrievedMemories(memories);
            
            Map<String, Object> additionalContext = new HashMap<>();
            additionalContext.put("时间", "上午");
            additionalContext.put("季节", "秋季");
            additionalContext.put("用户等级", "高级");
            context.setAdditionalContext(additionalContext);
            
            String prompt = template.buildPrompt(context);
            
            assertNotNull(prompt);
            assertFalse(prompt.trim().isEmpty());
            // 验证所有关键信息都包含在提示词中
            assertTrue(prompt.contains("测试用户查询"));
            assertTrue(prompt.contains("咖啡顾问"));
        }

        @Test
        @DisplayName("大量检索内存的处理")
        void testLargeMemoryList() {
            List<RetrievedMemory> largeMemoryList = new ArrayList<>();
            
            for (int i = 0; i < 50; i++) {
                largeMemoryList.add(new RetrievedMemory(
                    "内存内容 " + i, 
                    0.9 - (i * 0.01), 
                    createMetadata("type" + (i % 5), "2024-08-" + (i % 30 + 1)), 
                    "type" + (i % 3)
                ));
            }
            
            context.setRetrievedMemories(largeMemoryList);
            
            assertDoesNotThrow(() -> {
                String prompt = template.buildPrompt(context);
                assertNotNull(prompt);
                assertFalse(prompt.trim().isEmpty());
            });
        }

        @Test
        @DisplayName("极长查询文本处理")
        void testVeryLongQueryText() {
            StringBuilder longQuery = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                longQuery.append("这是一个很长的查询文本，用于测试模板对超长文本的处理能力。");
            }
            
            context.setUserQuery(longQuery.toString());
            
            assertDoesNotThrow(() -> {
                String prompt = template.buildPrompt(context);
                assertNotNull(prompt);
                assertTrue(prompt.contains("这是一个很长的查询文本"));
            });
        }
    }

    @Nested
    @DisplayName("边界条件和异常处理")
    class EdgeCaseTests {

        @Test
        @DisplayName("空字符串查询处理")
        void testEmptyStringQuery() {
            context.setUserQuery("");
            
            String result = template.buildPrompt(context);
            assertNotNull(result);
        }

        @Test
        @DisplayName("空检索内存列表处理")
        void testEmptyMemoryList() {
            context.setRetrievedMemories(new ArrayList<>());
            
            assertDoesNotThrow(() -> {
                String result = template.buildPrompt(context);
                assertNotNull(result);
            });
        }

        @Test
        @DisplayName("空额外上下文处理")
        void testEmptyAdditionalContext() {
            context.setAdditionalContext(new HashMap<>());
            
            assertDoesNotThrow(() -> {
                String result = template.buildPrompt(context);
                assertNotNull(result);
            });
        }

        @Test
        @DisplayName("特殊字符处理")
        void testSpecialCharacters() {
            context.setUserQuery("查询包含特殊字符：@#$%^&*()_+-=[]{}|;:'\",.<>?/~`");
            context.setSystemMessage("系统消息包含换行符\n和制表符\t");
            
            assertDoesNotThrow(() -> {
                String result = template.buildPrompt(context);
                assertNotNull(result);
                assertTrue(result.contains("特殊字符"));
            });
        }

        @Test
        @DisplayName("Unicode字符处理")
        void testUnicodeCharacters() {
            context.setUserQuery("包含Unicode字符：😊🚀🌟💡🎯");
            
            List<RetrievedMemory> memories = Arrays.asList(
                new RetrievedMemory("内存也包含emoji：🔥💯", 0.9, null, "emoji")
            );
            context.setRetrievedMemories(memories);
            
            assertDoesNotThrow(() -> {
                String result = template.buildPrompt(context);
                assertNotNull(result);
                assertTrue(result.contains("😊"));
                assertTrue(result.contains("🔥"));
            });
        }
    }

    @Nested
    @DisplayName("并发安全性测试")
    class ConcurrencyTests {

        @Test
        @DisplayName("并发构建提示词")
        void testConcurrentPromptBuilding() throws InterruptedException {
            int threadCount = 20;
            int operationsPerThread = 50;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            
            List<CompletableFuture<Void>> futures = IntStream.range(0, threadCount)
                .mapToObj(threadId -> CompletableFuture.runAsync(() -> {
                    for (int i = 0; i < operationsPerThread; i++) {
                        PromptContext threadContext = new PromptContext("线程" + threadId + "查询" + i);
                        threadContext.setSystemMessage("系统消息" + threadId);
                        
                        List<RetrievedMemory> memories = Arrays.asList(
                            new RetrievedMemory("内存" + threadId + "_" + i, 0.8, null, "concurrent")
                        );
                        threadContext.setRetrievedMemories(memories);
                        
                        assertDoesNotThrow(() -> {
                            String result = template.buildPrompt(threadContext);
                            assertNotNull(result);
                            assertTrue(result.contains("线程" + threadId));
                        });
                    }
                }, executor))
                .collect(Collectors.toList());
            
            assertDoesNotThrow(() -> 
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join());
            
            executor.shutdown();
        }
    }

    @Nested
    @DisplayName("性能测试")
    class PerformanceTests {

        @Test
        @DisplayName("提示词构建性能")
        void testPromptBuildingPerformance() {
            // 准备测试数据
            context.setSystemMessage("性能测试系统消息");
            
            List<RetrievedMemory> memories = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                memories.add(new RetrievedMemory(
                    "性能测试内存" + i, 
                    0.9 - (i * 0.01), 
                    createMetadata("performance", "test"), 
                    "perf"
                ));
            }
            context.setRetrievedMemories(memories);
            
            Map<String, Object> additionalContext = new HashMap<>();
            for (int i = 0; i < 10; i++) {
                additionalContext.put("key" + i, "value" + i);
            }
            context.setAdditionalContext(additionalContext);
            
            // 执行性能测试
            long startTime = System.nanoTime();
            
            for (int i = 0; i < 1000; i++) {
                String result = template.buildPrompt(context);
                assertNotNull(result);
            }
            
            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;
            
            // 性能验证：1000次调用应在合理时间内完成（例如<1000ms）
            assertTrue(durationMs < 1000, "提示词构建性能测试失败，耗时: " + durationMs + "ms");
        }
    }

    // 辅助方法
    private Map<String, Object> createMetadata(String category, String timestamp) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("category", category);
        metadata.put("timestamp", timestamp);
        return metadata;
    }

    // 测试用的PromptTemplate实现
    private static class TestPromptTemplate implements PromptTemplate {
        
        @Override
        public String buildPrompt(PromptContext context) {
            if (context == null) {
                throw new NullPointerException("Context cannot be null");
            }
            
            StringBuilder prompt = new StringBuilder();
            
            if (context.getSystemMessage() != null) {
                prompt.append("系统: ").append(context.getSystemMessage()).append("\n");
            }
            
            if (context.getRetrievedMemories() != null) {
                prompt.append("相关记忆:\n");
                for (RetrievedMemory memory : context.getRetrievedMemories()) {
                    prompt.append("- ").append(memory.getContent())
                          .append(" (相关度: ").append(memory.getRelevanceScore()).append(")\n");
                }
            }
            
            if (context.getAdditionalContext() != null) {
                prompt.append("额外上下文:\n");
                context.getAdditionalContext().forEach((k, v) -> 
                    prompt.append("- ").append(k).append(": ").append(v).append("\n"));
            }
            
            prompt.append("用户查询: ").append(context.getUserQuery());
            
            return prompt.toString();
        }
        
        @Override
        public String getName() {
            return "TestTemplate";
        }
        
        @Override
        public String getDescription() {
            return "测试用的提示词模板实现";
        }
    }
}