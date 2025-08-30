package com.mem0.unit.template;

import com.mem0.template.ChatRAGPromptTemplate;
import com.mem0.template.PromptTemplate.PromptContext;
import com.mem0.template.PromptTemplate.RetrievedMemory;
import com.mem0.llm.LLMProvider.ChatMessage;
import com.mem0.llm.LLMProvider.ChatMessage.Role;
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
 * ChatRAGPromptTemplate 的全面测试套件
 */
@DisplayName("ChatRAGPromptTemplate 聊天RAG提示词模板测试")
class ChatRAGPromptTemplateTest {

    private ChatRAGPromptTemplate template;
    private PromptContext context;

    @BeforeEach
    void setUp() {
        template = new ChatRAGPromptTemplate();
        context = new PromptContext("请帮我理解深度学习和机器学习的区别");
    }

    @Nested
    @DisplayName("基础功能测试")
    class BasicFunctionalityTests {

        @Test
        @DisplayName("获取模板名称")
        void testGetName() {
            assertEquals("ChatRAG", template.getName());
        }

        @Test
        @DisplayName("获取模板描述")
        void testGetDescription() {
            String description = template.getDescription();
            
            assertNotNull(description);
            assertFalse(description.trim().isEmpty());
            assertTrue(description.contains("Chat-based"));
            assertTrue(description.contains("Retrieval-Augmented Generation"));
            assertTrue(description.contains("structured chat messages"));
        }

        @Test
        @DisplayName("基础聊天消息构建")
        void testBasicChatMessageBuilding() {
            List<ChatMessage> messages = template.buildChatMessages(context);
            
            assertNotNull(messages);
            assertEquals(2, messages.size());
            
            // 验证系统消息
            ChatMessage systemMessage = messages.get(0);
            assertEquals(Role.SYSTEM, systemMessage.getRole());
            assertNotNull(systemMessage.getContent());
            assertTrue(systemMessage.getContent().contains("AI assistant"));
            
            // 验证用户消息
            ChatMessage userMessage = messages.get(1);
            assertEquals(Role.USER, userMessage.getRole());
            assertEquals("请帮我理解深度学习和机器学习的区别", userMessage.getContent());
        }

        @Test
        @DisplayName("默认系统消息验证")
        void testDefaultSystemMessage() {
            List<ChatMessage> messages = template.buildChatMessages(context);
            
            ChatMessage systemMessage = messages.get(0);
            String content = systemMessage.getContent();
            
            assertTrue(content.contains("AI assistant with access to retrieved memories"));
            assertTrue(content.contains("accurate and helpful responses"));
            assertTrue(content.contains("be specific about which memory"));
        }
    }

    @Nested
    @DisplayName("系统消息处理测试")
    class SystemMessageTests {

        @Test
        @DisplayName("自定义系统消息")
        void testCustomSystemMessage() {
            String customMessage = "你是一个专业的人工智能研究员，专门研究深度学习和机器学习技术。";
            context.setSystemMessage(customMessage);
            
            List<ChatMessage> messages = template.buildChatMessages(context);
            ChatMessage systemMessage = messages.get(0);
            
            assertTrue(systemMessage.getContent().contains(customMessage));
            assertFalse(systemMessage.getContent().contains("AI assistant with access"));
        }

        @Test
        @DisplayName("null系统消息使用默认值")
        void testNullSystemMessage() {
            context.setSystemMessage(null);
            
            List<ChatMessage> messages = template.buildChatMessages(context);
            ChatMessage systemMessage = messages.get(0);
            
            assertTrue(systemMessage.getContent().contains("AI assistant with access to retrieved memories"));
        }

        @Test
        @DisplayName("空字符串系统消息")
        void testEmptySystemMessage() {
            context.setSystemMessage("");
            
            List<ChatMessage> messages = template.buildChatMessages(context);
            ChatMessage systemMessage = messages.get(0);
            
            // 空字符串应该被使用，而不是默认消息
            assertFalse(systemMessage.getContent().contains("AI assistant with access"));
            assertTrue(systemMessage.getContent().startsWith(""));
        }
    }

    @Nested
    @DisplayName("检索内存处理测试")
    class RetrievedMemoryTests {

        @Test
        @DisplayName("单个检索内存")
        void testSingleRetrievedMemory() {
            RetrievedMemory memory = new RetrievedMemory(
                "深度学习是机器学习的一个子集，使用多层神经网络", 
                0.95, 
                null, 
                "definition"
            );
            context.setRetrievedMemories(Arrays.asList(memory));
            
            List<ChatMessage> messages = template.buildChatMessages(context);
            ChatMessage systemMessage = messages.get(0);
            String content = systemMessage.getContent();
            
            assertTrue(content.contains("=== RETRIEVED MEMORIES ==="));
            assertTrue(content.contains("=== END RETRIEVED MEMORIES ==="));
            assertTrue(content.contains("[Memory 1] (relevance: 0.950, type: definition)"));
            assertTrue(content.contains("深度学习是机器学习的一个子集"));
        }

        @Test
        @DisplayName("多个检索内存")
        void testMultipleRetrievedMemories() {
            List<RetrievedMemory> memories = Arrays.asList(
                new RetrievedMemory(
                    "机器学习是人工智能的一个分支，让计算机从数据中学习", 
                    0.92, 
                    null, 
                    "definition"
                ),
                new RetrievedMemory(
                    "深度学习使用神经网络模拟人脑的工作方式", 
                    0.89, 
                    null, 
                    "explanation"
                ),
                new RetrievedMemory(
                    "卷积神经网络是深度学习中用于图像处理的重要架构", 
                    0.78, 
                    null, 
                    "technical"
                )
            );
            context.setRetrievedMemories(memories);
            
            List<ChatMessage> messages = template.buildChatMessages(context);
            ChatMessage systemMessage = messages.get(0);
            String content = systemMessage.getContent();
            
            assertTrue(content.contains("[Memory 1] (relevance: 0.920, type: definition)"));
            assertTrue(content.contains("[Memory 2] (relevance: 0.890, type: explanation)"));
            assertTrue(content.contains("[Memory 3] (relevance: 0.780, type: technical)"));
            assertTrue(content.contains("机器学习是人工智能的一个分支"));
            assertTrue(content.contains("深度学习使用神经网络"));
            assertTrue(content.contains("卷积神经网络"));
        }

        @Test
        @DisplayName("无类型的检索内存")
        void testMemoryWithoutType() {
            RetrievedMemory memory = new RetrievedMemory(
                "这是一个没有类型的内存", 
                0.85, 
                null, 
                null
            );
            context.setRetrievedMemories(Arrays.asList(memory));
            
            List<ChatMessage> messages = template.buildChatMessages(context);
            ChatMessage systemMessage = messages.get(0);
            String content = systemMessage.getContent();
            
            assertTrue(content.contains("[Memory 1] (relevance: 0.850)"));
            assertFalse(content.contains("type:"));
            assertTrue(content.contains("这是一个没有类型的内存"));
        }

        @Test
        @DisplayName("空类型的检索内存")
        void testMemoryWithEmptyType() {
            RetrievedMemory memory = new RetrievedMemory(
                "这是一个空类型的内存", 
                0.72, 
                null, 
                ""
            );
            context.setRetrievedMemories(Arrays.asList(memory));
            
            List<ChatMessage> messages = template.buildChatMessages(context);
            ChatMessage systemMessage = messages.get(0);
            String content = systemMessage.getContent();
            
            assertTrue(content.contains("[Memory 1] (relevance: 0.720)"));
            assertFalse(content.contains("type:"));
        }

        @Test
        @DisplayName("空内存列表")
        void testEmptyMemoryList() {
            context.setRetrievedMemories(new ArrayList<>());
            
            List<ChatMessage> messages = template.buildChatMessages(context);
            ChatMessage systemMessage = messages.get(0);
            String content = systemMessage.getContent();
            
            assertFalse(content.contains("=== RETRIEVED MEMORIES ==="));
            assertFalse(content.contains("[Memory"));
        }

        @Test
        @DisplayName("null内存列表")
        void testNullMemoryList() {
            context.setRetrievedMemories(null);
            
            List<ChatMessage> messages = template.buildChatMessages(context);
            ChatMessage systemMessage = messages.get(0);
            String content = systemMessage.getContent();
            
            assertFalse(content.contains("=== RETRIEVED MEMORIES ==="));
            assertFalse(content.contains("[Memory"));
        }
    }

    @Nested
    @DisplayName("额外上下文处理测试")
    class AdditionalContextTests {

        @Test
        @DisplayName("单个额外上下文")
        void testSingleAdditionalContext() {
            Map<String, Object> additionalContext = new HashMap<>();
            additionalContext.put("用户水平", "中级");
            context.setAdditionalContext(additionalContext);
            
            List<ChatMessage> messages = template.buildChatMessages(context);
            ChatMessage systemMessage = messages.get(0);
            String content = systemMessage.getContent();
            
            assertTrue(content.contains("=== ADDITIONAL CONTEXT ==="));
            assertTrue(content.contains("=== END ADDITIONAL CONTEXT ==="));
            assertTrue(content.contains("用户水平: 中级"));
        }

        @Test
        @DisplayName("多个额外上下文")
        void testMultipleAdditionalContext() {
            Map<String, Object> additionalContext = new HashMap<>();
            additionalContext.put("学习目标", "理解基础概念");
            additionalContext.put("时间限制", "10分钟");
            additionalContext.put("语言偏好", "中文");
            additionalContext.put("需要例子", true);
            context.setAdditionalContext(additionalContext);
            
            List<ChatMessage> messages = template.buildChatMessages(context);
            ChatMessage systemMessage = messages.get(0);
            String content = systemMessage.getContent();
            
            assertTrue(content.contains("=== ADDITIONAL CONTEXT ==="));
            assertTrue(content.contains("学习目标: 理解基础概念"));
            assertTrue(content.contains("时间限制: 10分钟"));
            assertTrue(content.contains("语言偏好: 中文"));
            assertTrue(content.contains("需要例子: true"));
        }

        @Test
        @DisplayName("空额外上下文")
        void testEmptyAdditionalContext() {
            context.setAdditionalContext(new HashMap<>());
            
            List<ChatMessage> messages = template.buildChatMessages(context);
            ChatMessage systemMessage = messages.get(0);
            String content = systemMessage.getContent();
            
            assertFalse(content.contains("=== ADDITIONAL CONTEXT ==="));
        }

        @Test
        @DisplayName("null额外上下文")
        void testNullAdditionalContext() {
            context.setAdditionalContext(null);
            
            List<ChatMessage> messages = template.buildChatMessages(context);
            ChatMessage systemMessage = messages.get(0);
            String content = systemMessage.getContent();
            
            assertFalse(content.contains("=== ADDITIONAL CONTEXT ==="));
        }
    }

    @Nested
    @DisplayName("完整场景测试")
    class CompleteScenarioTests {

        @Test
        @DisplayName("完整的聊天RAG消息构建")
        void testCompleteChatRAGMessageBuilding() {
            // 设置自定义系统消息
            context.setSystemMessage("你是一个AI教授，专门教授深度学习课程。");
            
            // 设置检索内存
            List<RetrievedMemory> memories = Arrays.asList(
                new RetrievedMemory(
                    "机器学习是一种让计算机从数据中学习模式的技术", 
                    0.94, 
                    createMetadata("source", "textbook", "page", 15), 
                    "definition"
                ),
                new RetrievedMemory(
                    "深度学习是机器学习的一个特殊分支，使用深层神经网络", 
                    0.91, 
                    createMetadata("source", "paper", "year", 2020), 
                    "specialization"
                ),
                new RetrievedMemory(
                    "CNN、RNN、Transformer都是深度学习的重要架构", 
                    0.87, 
                    createMetadata("source", "course", "module", 3), 
                    "architecture"
                )
            );
            context.setRetrievedMemories(memories);
            
            // 设置额外上下文
            Map<String, Object> additionalContext = new HashMap<>();
            additionalContext.put("课程级别", "本科生");
            additionalContext.put("先修课程", "线性代数,统计学");
            additionalContext.put("预期时长", "15分钟");
            additionalContext.put("互动方式", "问答");
            context.setAdditionalContext(additionalContext);
            
            List<ChatMessage> messages = template.buildChatMessages(context);
            
            assertEquals(2, messages.size());
            
            // 验证系统消息
            ChatMessage systemMessage = messages.get(0);
            assertEquals(Role.SYSTEM, systemMessage.getRole());
            String systemContent = systemMessage.getContent();
            
            assertTrue(systemContent.contains("AI教授"));
            assertTrue(systemContent.contains("=== RETRIEVED MEMORIES ==="));
            assertTrue(systemContent.contains("[Memory 1] (relevance: 0.940, type: definition)"));
            assertTrue(systemContent.contains("[Memory 2] (relevance: 0.910, type: specialization)"));
            assertTrue(systemContent.contains("[Memory 3] (relevance: 0.870, type: architecture)"));
            assertTrue(systemContent.contains("机器学习是一种让计算机"));
            assertTrue(systemContent.contains("深度学习是机器学习的一个特殊"));
            assertTrue(systemContent.contains("CNN、RNN、Transformer"));
            assertTrue(systemContent.contains("=== ADDITIONAL CONTEXT ==="));
            assertTrue(systemContent.contains("课程级别: 本科生"));
            assertTrue(systemContent.contains("先修课程: 线性代数,统计学"));
            assertTrue(systemContent.contains("预期时长: 15分钟"));
            
            // 验证用户消息
            ChatMessage userMessage = messages.get(1);
            assertEquals(Role.USER, userMessage.getRole());
            assertEquals("请帮我理解深度学习和机器学习的区别", userMessage.getContent());
        }

        @Test
        @DisplayName("仅有系统消息和用户查询")
        void testMinimalChatMessages() {
            // 不设置任何检索内存和额外上下文
            List<ChatMessage> messages = template.buildChatMessages(context);
            
            assertEquals(2, messages.size());
            
            ChatMessage systemMessage = messages.get(0);
            assertEquals(Role.SYSTEM, systemMessage.getRole());
            assertTrue(systemMessage.getContent().contains("AI assistant with access"));
            assertFalse(systemMessage.getContent().contains("=== RETRIEVED MEMORIES ==="));
            assertFalse(systemMessage.getContent().contains("=== ADDITIONAL CONTEXT ==="));
            
            ChatMessage userMessage = messages.get(1);
            assertEquals(Role.USER, userMessage.getRole());
            assertEquals("请帮我理解深度学习和机器学习的区别", userMessage.getContent());
        }

        @Test
        @DisplayName("只有检索内存无额外上下文")
        void testOnlyRetrievedMemories() {
            List<RetrievedMemory> memories = Arrays.asList(
                new RetrievedMemory("关键概念说明", 0.88, null, "concept")
            );
            context.setRetrievedMemories(memories);
            
            List<ChatMessage> messages = template.buildChatMessages(context);
            
            assertEquals(2, messages.size());
            
            ChatMessage systemMessage = messages.get(0);
            assertTrue(systemMessage.getContent().contains("=== RETRIEVED MEMORIES ==="));
            assertTrue(systemMessage.getContent().contains("关键概念说明"));
            assertFalse(systemMessage.getContent().contains("=== ADDITIONAL CONTEXT ==="));
        }

        @Test
        @DisplayName("只有额外上下文无检索内存")
        void testOnlyAdditionalContext() {
            Map<String, Object> additionalContext = new HashMap<>();
            additionalContext.put("context_key", "context_value");
            context.setAdditionalContext(additionalContext);
            
            List<ChatMessage> messages = template.buildChatMessages(context);
            
            assertEquals(2, messages.size());
            
            ChatMessage systemMessage = messages.get(0);
            assertTrue(systemMessage.getContent().contains("=== ADDITIONAL CONTEXT ==="));
            assertTrue(systemMessage.getContent().contains("context_key: context_value"));
            assertFalse(systemMessage.getContent().contains("=== RETRIEVED MEMORIES ==="));
        }
    }

    @Nested
    @DisplayName("异常处理和边界测试")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("null上下文异常处理")
        void testNullContext() {
            assertThrows(Exception.class, () -> {
                template.buildChatMessages(null);
            });
        }

        @Test
        @DisplayName("null用户查询处理")
        void testNullUserQuery() {
            context.setUserQuery(null);
            
            List<ChatMessage> messages = template.buildChatMessages(context);
            
            assertEquals(2, messages.size());
            ChatMessage userMessage = messages.get(1);
            assertEquals(Role.USER, userMessage.getRole());
            assertNull(userMessage.getContent());
        }

        @Test
        @DisplayName("空用户查询处理")
        void testEmptyUserQuery() {
            context.setUserQuery("");
            
            List<ChatMessage> messages = template.buildChatMessages(context);
            
            assertEquals(2, messages.size());
            ChatMessage userMessage = messages.get(1);
            assertEquals(Role.USER, userMessage.getRole());
            assertEquals("", userMessage.getContent());
        }

        @Test
        @DisplayName("极长用户查询处理")
        void testVeryLongUserQuery() {
            StringBuilder longQuery = new StringBuilder();
            for (int i = 0; i < 500; i++) {
                longQuery.append("这是一个非常长的用户查询，用于测试系统对超长文本的处理能力。");
            }
            context.setUserQuery(longQuery.toString());
            
            assertDoesNotThrow(() -> {
                List<ChatMessage> messages = template.buildChatMessages(context);
                assertEquals(2, messages.size());
                assertTrue(messages.get(1).getContent().contains("这是一个非常长的用户查询"));
            });
        }

        @Test
        @DisplayName("特殊字符处理")
        void testSpecialCharacterHandling() {
            context.setUserQuery("特殊字符测试：\\n\\t\\r\"'<>&");
            context.setSystemMessage("系统消息特殊字符：@#$%^&*()[]{}");
            
            List<RetrievedMemory> memories = Arrays.asList(
                new RetrievedMemory("内存特殊字符：|\\/?;:+=`~", 0.8, null, "special")
            );
            context.setRetrievedMemories(memories);
            
            assertDoesNotThrow(() -> {
                List<ChatMessage> messages = template.buildChatMessages(context);
                assertEquals(2, messages.size());
                assertTrue(messages.get(1).getContent().contains("\\n\\t\\r"));
                assertTrue(messages.get(0).getContent().contains("@#$%^&*()"));
                assertTrue(messages.get(0).getContent().contains("|\\/?;:"));
            });
        }
    }

    @Nested
    @DisplayName("性能和并发测试")
    class PerformanceTests {

        @Test
        @DisplayName("大量内存性能测试")
        void testLargeMemoryListPerformance() {
            List<RetrievedMemory> largeMemoryList = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                largeMemoryList.add(new RetrievedMemory(
                    "大量内存测试内容 " + i + " - 详细的描述文本包含各种信息",
                    0.9 - (i * 0.002),
                    createMetadata("index", i, "category", "performance"),
                    "perf_memory_" + i
                ));
            }
            context.setRetrievedMemories(largeMemoryList);
            
            long startTime = System.nanoTime();
            List<ChatMessage> messages = template.buildChatMessages(context);
            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;
            
            assertEquals(2, messages.size());
            assertTrue(messages.get(0).getContent().contains("[Memory 200]"));
            assertTrue(durationMs < 200, "大量内存处理耗时过长: " + durationMs + "ms");
        }

        @Test
        @DisplayName("并发消息构建测试")
        void testConcurrentMessageBuilding() {
            int threadCount = 8;
            int operationsPerThread = 25;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            
            List<CompletableFuture<Void>> futures = IntStream.range(0, threadCount)
                .mapToObj(threadId -> CompletableFuture.runAsync(() -> {
                    final int finalThreadId = threadId; // Make threadId effectively final
                    for (int i = 0; i < operationsPerThread; i++) {
                        final int finalI = i; // Make i effectively final
                        PromptContext threadContext = new PromptContext("线程" + finalThreadId + "测试查询" + finalI);
                        threadContext.setSystemMessage("线程" + finalThreadId + "系统消息");
                        
                        List<RetrievedMemory> memories = Arrays.asList(
                            new RetrievedMemory("线程" + finalThreadId + "内存" + finalI, 0.85, null, "thread_test")
                        );
                        threadContext.setRetrievedMemories(memories);
                        
                        assertDoesNotThrow(() -> {
                            List<ChatMessage> messages = template.buildChatMessages(threadContext);
                            assertEquals(2, messages.size());
                            assertTrue(messages.get(1).getContent().contains("线程" + finalThreadId + "测试查询" + finalI));
                            assertTrue(messages.get(0).getContent().contains("线程" + finalThreadId + "内存" + finalI));
                        });
                    }
                }, executor))
                .collect(Collectors.toList());
            
            assertDoesNotThrow(() -> 
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join());
            
            executor.shutdown();
        }

        @Test
        @DisplayName("重复构建性能测试")
        void testRepeatedBuildingPerformance() {
            // 准备复杂的上下文
            context.setSystemMessage("性能测试专用系统消息");
            
            List<RetrievedMemory> memories = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                memories.add(new RetrievedMemory(
                    "性能测试内存 " + i + " 包含详细内容和元数据",
                    0.9 - (i * 0.01),
                    createMetadata("id", i, "type", "performance"),
                    "perf_type"
                ));
            }
            context.setRetrievedMemories(memories);
            
            Map<String, Object> additionalContext = new HashMap<>();
            for (int i = 0; i < 20; i++) {
                additionalContext.put("key" + i, "value" + i);
            }
            context.setAdditionalContext(additionalContext);
            
            // 执行重复构建性能测试
            long startTime = System.nanoTime();
            
            for (int i = 0; i < 500; i++) {
                List<ChatMessage> messages = template.buildChatMessages(context);
                assertEquals(2, messages.size());
                assertNotNull(messages.get(0).getContent());
                assertNotNull(messages.get(1).getContent());
            }
            
            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;
            
            assertTrue(durationMs < 1000, "重复构建性能测试失败，耗时: " + durationMs + "ms");
        }
    }

    @Nested
    @DisplayName("消息格式验证测试")
    class MessageFormatTests {

        @Test
        @DisplayName("消息角色正确性")
        void testMessageRoles() {
            List<RetrievedMemory> memories = Arrays.asList(
                new RetrievedMemory("测试内存", 0.9, null, "test")
            );
            context.setRetrievedMemories(memories);
            
            List<ChatMessage> messages = template.buildChatMessages(context);
            
            assertEquals(2, messages.size());
            assertEquals(Role.SYSTEM, messages.get(0).getRole());
            assertEquals(Role.USER, messages.get(1).getRole());
        }

        @Test
        @DisplayName("系统消息内容完整性")
        void testSystemMessageCompleteness() {
            context.setSystemMessage("测试系统消息");
            
            List<RetrievedMemory> memories = Arrays.asList(
                new RetrievedMemory("内存内容", 0.88, null, "test")
            );
            context.setRetrievedMemories(memories);
            
            Map<String, Object> additionalContext = Collections.singletonMap("key", "value");
            context.setAdditionalContext(additionalContext);
            
            List<ChatMessage> messages = template.buildChatMessages(context);
            ChatMessage systemMessage = messages.get(0);
            String content = systemMessage.getContent();
            
            // 验证系统消息包含所有必要部分
            assertTrue(content.contains("测试系统消息"));
            assertTrue(content.contains("=== RETRIEVED MEMORIES ==="));
            assertTrue(content.contains("内存内容"));
            assertTrue(content.contains("=== ADDITIONAL CONTEXT ==="));
            assertTrue(content.contains("key: value"));
        }

        @Test
        @DisplayName("消息顺序正确性")
        void testMessageOrder() {
            List<ChatMessage> messages = template.buildChatMessages(context);
            
            assertEquals(2, messages.size());
            assertEquals(Role.SYSTEM, messages.get(0).getRole());
            assertEquals(Role.USER, messages.get(1).getRole());
        }
    }

    // 辅助方法
    private Map<String, Object> createMetadata(String key1, Object value1, String key2, Object value2) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(key1, value1);
        metadata.put(key2, value2);
        return metadata;
    }
}