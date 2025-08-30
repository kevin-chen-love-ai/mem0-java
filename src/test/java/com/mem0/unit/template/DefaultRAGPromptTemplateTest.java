package com.mem0.unit.template;

import com.mem0.template.DefaultRAGPromptTemplate;
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
 * DefaultRAGPromptTemplate 的全面测试套件
 */
@DisplayName("DefaultRAGPromptTemplate 默认RAG提示词模板测试")
class DefaultRAGPromptTemplateTest {

    private DefaultRAGPromptTemplate template;
    private PromptContext context;

    @BeforeEach
    void setUp() {
        template = new DefaultRAGPromptTemplate();
        context = new PromptContext("用户想了解机器学习的基础知识");
    }

    @Nested
    @DisplayName("基础功能测试")
    class BasicFunctionalityTests {

        @Test
        @DisplayName("获取模板名称")
        void testGetName() {
            assertEquals("DefaultRAG", template.getName());
        }

        @Test
        @DisplayName("获取模板描述")
        void testGetDescription() {
            String description = template.getDescription();
            
            assertNotNull(description);
            assertFalse(description.trim().isEmpty());
            assertTrue(description.contains("Retrieval-Augmented Generation"));
            assertTrue(description.contains("retrieved memories"));
        }

        @Test
        @DisplayName("最简单的提示词构建")
        void testBasicPromptBuilding() {
            String prompt = template.buildPrompt(context);
            
            assertNotNull(prompt);
            assertFalse(prompt.trim().isEmpty());
            assertTrue(prompt.contains("用户想了解机器学习的基础知识"));
            assertTrue(prompt.contains("System:"));
            assertTrue(prompt.contains("User:"));
            assertTrue(prompt.contains("Assistant:"));
        }

        @Test
        @DisplayName("默认系统消息使用")
        void testDefaultSystemMessage() {
            String prompt = template.buildPrompt(context);
            
            assertTrue(prompt.contains("AI assistant with access to retrieved memories"));
            assertTrue(prompt.contains("accurate and helpful responses"));
        }
    }

    @Nested
    @DisplayName("系统消息处理测试")
    class SystemMessageTests {

        @Test
        @DisplayName("自定义系统消息")
        void testCustomSystemMessage() {
            String customMessage = "你是一个专业的机器学习教授，擅长用简单易懂的方式解释复杂概念。";
            context.setSystemMessage(customMessage);
            
            String prompt = template.buildPrompt(context);
            
            assertTrue(prompt.contains(customMessage));
            assertFalse(prompt.contains("AI assistant with access to retrieved memories"));
        }

        @Test
        @DisplayName("空系统消息处理")
        void testEmptySystemMessage() {
            context.setSystemMessage("");
            
            String prompt = template.buildPrompt(context);
            
            // 空字符串不会触发默认系统消息，应该直接使用空字符串
            assertTrue(prompt.contains("System: "));
            assertFalse(prompt.contains("AI assistant with access to retrieved memories"));
        }

        @Test
        @DisplayName("空白系统消息处理")
        void testWhitespaceSystemMessage() {
            context.setSystemMessage("   ");
            
            String prompt = template.buildPrompt(context);
            
            // 空白字符串应该被保留，不使用默认消息
            assertTrue(prompt.contains("System:    "));
        }

        @Test
        @DisplayName("多行系统消息")
        void testMultilineSystemMessage() {
            String multilineMessage = "你是一个AI助手。\n请遵循以下原则：\n1. 准确性第一\n2. 简洁明了\n3. 友好helpful";
            context.setSystemMessage(multilineMessage);
            
            String prompt = template.buildPrompt(context);
            
            assertTrue(prompt.contains(multilineMessage));
            assertTrue(prompt.contains("准确性第一"));
            assertTrue(prompt.contains("友好helpful"));
        }
    }

    @Nested
    @DisplayName("检索内存处理测试")
    class RetrievedMemoryTests {

        @Test
        @DisplayName("单个内存处理")
        void testSingleMemory() {
            RetrievedMemory memory = new RetrievedMemory(
                "机器学习是人工智能的一个重要分支", 
                0.92, 
                null, 
                "definition"
            );
            context.setRetrievedMemories(Arrays.asList(memory));
            
            String prompt = template.buildPrompt(context);
            
            assertTrue(prompt.contains("=== RETRIEVED MEMORIES ==="));
            assertTrue(prompt.contains("=== END RETRIEVED MEMORIES ==="));
            assertTrue(prompt.contains("Memory 1 (relevance: 0.920)"));
            assertTrue(prompt.contains("机器学习是人工智能的一个重要分支"));
            assertTrue(prompt.contains("Type: definition"));
        }

        @Test
        @DisplayName("多个内存处理")
        void testMultipleMemories() {
            List<RetrievedMemory> memories = Arrays.asList(
                new RetrievedMemory("机器学习使用算法来学习数据中的模式", 0.95, null, "concept"),
                new RetrievedMemory("监督学习需要标记的训练数据", 0.88, null, "method"),
                new RetrievedMemory("深度学习是机器学习的子集", 0.82, null, "relationship")
            );
            context.setRetrievedMemories(memories);
            
            String prompt = template.buildPrompt(context);
            
            assertTrue(prompt.contains("Memory 1 (relevance: 0.950)"));
            assertTrue(prompt.contains("Memory 2 (relevance: 0.880)"));
            assertTrue(prompt.contains("Memory 3 (relevance: 0.820)"));
            assertTrue(prompt.contains("机器学习使用算法"));
            assertTrue(prompt.contains("监督学习需要"));
            assertTrue(prompt.contains("深度学习是机器学习"));
            assertTrue(prompt.contains("Type: concept"));
            assertTrue(prompt.contains("Type: method"));
            assertTrue(prompt.contains("Type: relationship"));
        }

        @Test
        @DisplayName("内存无类型处理")
        void testMemoryWithoutType() {
            RetrievedMemory memory = new RetrievedMemory(
                "无类型的内存内容", 
                0.75, 
                null, 
                null
            );
            context.setRetrievedMemories(Arrays.asList(memory));
            
            String prompt = template.buildPrompt(context);
            
            assertTrue(prompt.contains("无类型的内存内容"));
            assertFalse(prompt.contains("Type: null"));
            assertFalse(prompt.contains("Type: "));
        }

        @Test
        @DisplayName("内存空类型处理")
        void testMemoryWithEmptyType() {
            RetrievedMemory memory = new RetrievedMemory(
                "空类型的内存内容", 
                0.68, 
                null, 
                ""
            );
            context.setRetrievedMemories(Arrays.asList(memory));
            
            String prompt = template.buildPrompt(context);
            
            assertTrue(prompt.contains("空类型的内存内容"));
            assertFalse(prompt.contains("Type:"));
        }

        @Test
        @DisplayName("内存列表为空")
        void testEmptyMemoryList() {
            context.setRetrievedMemories(new ArrayList<>());
            
            String prompt = template.buildPrompt(context);
            
            assertFalse(prompt.contains("=== RETRIEVED MEMORIES ==="));
            assertFalse(prompt.contains("Memory 1"));
        }

        @Test
        @DisplayName("内存列表为null")
        void testNullMemoryList() {
            context.setRetrievedMemories(null);
            
            String prompt = template.buildPrompt(context);
            
            assertFalse(prompt.contains("=== RETRIEVED MEMORIES ==="));
            assertFalse(prompt.contains("Memory 1"));
        }
    }

    @Nested
    @DisplayName("额外上下文处理测试")
    class AdditionalContextTests {

        @Test
        @DisplayName("单个额外上下文")
        void testSingleAdditionalContext() {
            Map<String, Object> additionalContext = new HashMap<>();
            additionalContext.put("domain", "教育");
            context.setAdditionalContext(additionalContext);
            
            String prompt = template.buildPrompt(context);
            
            assertTrue(prompt.contains("=== ADDITIONAL CONTEXT ==="));
            assertTrue(prompt.contains("=== END ADDITIONAL CONTEXT ==="));
            assertTrue(prompt.contains("domain: 教育"));
        }

        @Test
        @DisplayName("多个额外上下文")
        void testMultipleAdditionalContext() {
            Map<String, Object> additionalContext = new HashMap<>();
            additionalContext.put("language", "中文");
            additionalContext.put("level", "初学者");
            additionalContext.put("format", "简洁");
            additionalContext.put("examples", true);
            context.setAdditionalContext(additionalContext);
            
            String prompt = template.buildPrompt(context);
            
            assertTrue(prompt.contains("=== ADDITIONAL CONTEXT ==="));
            assertTrue(prompt.contains("=== END ADDITIONAL CONTEXT ==="));
            assertTrue(prompt.contains("language: 中文"));
            assertTrue(prompt.contains("level: 初学者"));
            assertTrue(prompt.contains("format: 简洁"));
            assertTrue(prompt.contains("examples: true"));
        }

        @Test
        @DisplayName("额外上下文为空")
        void testEmptyAdditionalContext() {
            context.setAdditionalContext(new HashMap<>());
            
            String prompt = template.buildPrompt(context);
            
            assertFalse(prompt.contains("=== ADDITIONAL CONTEXT ==="));
        }

        @Test
        @DisplayName("额外上下文为null")
        void testNullAdditionalContext() {
            context.setAdditionalContext(null);
            
            String prompt = template.buildPrompt(context);
            
            assertFalse(prompt.contains("=== ADDITIONAL CONTEXT ==="));
        }

        @Test
        @DisplayName("复杂对象类型的额外上下文")
        void testComplexAdditionalContext() {
            Map<String, Object> additionalContext = new HashMap<>();
            additionalContext.put("numbers", Arrays.asList(1, 2, 3));
            additionalContext.put("metadata", Collections.singletonMap("key", "value"));
            additionalContext.put("timestamp", new Date());
            context.setAdditionalContext(additionalContext);
            
            assertDoesNotThrow(() -> {
                String prompt = template.buildPrompt(context);
                assertTrue(prompt.contains("=== ADDITIONAL CONTEXT ==="));
            });
        }
    }

    @Nested
    @DisplayName("完整场景测试")
    class CompleteScenarioTests {

        @Test
        @DisplayName("完整的RAG提示词构建")
        void testCompleteRAGPromptBuilding() {
            // 设置自定义系统消息
            context.setSystemMessage("你是一个专业的机器学习导师，能够根据学习者的水平提供个性化的解释。");
            
            // 设置检索内存
            List<RetrievedMemory> memories = Arrays.asList(
                new RetrievedMemory(
                    "机器学习是一种人工智能技术，它使计算机能够从数据中学习模式而不需要明确编程", 
                    0.96, 
                    createMetadata("source", "textbook", "chapter", "1"), 
                    "definition"
                ),
                new RetrievedMemory(
                    "机器学习主要分为三种类型：监督学习、无监督学习和强化学习", 
                    0.91, 
                    createMetadata("source", "lecture", "topic", "classification"), 
                    "categorization"
                ),
                new RetrievedMemory(
                    "常见的机器学习算法包括线性回归、决策树、神经网络等", 
                    0.85, 
                    createMetadata("source", "reference", "section", "algorithms"), 
                    "examples"
                )
            );
            context.setRetrievedMemories(memories);
            
            // 设置额外上下文
            Map<String, Object> additionalContext = new HashMap<>();
            additionalContext.put("学习者水平", "初学者");
            additionalContext.put("希望的回答长度", "详细");
            additionalContext.put("是否需要例子", true);
            additionalContext.put("语言偏好", "中文");
            context.setAdditionalContext(additionalContext);
            
            String prompt = template.buildPrompt(context);
            
            // 验证所有组件都存在
            assertTrue(prompt.contains("专业的机器学习导师"));
            assertTrue(prompt.contains("=== RETRIEVED MEMORIES ==="));
            assertTrue(prompt.contains("Memory 1 (relevance: 0.960)"));
            assertTrue(prompt.contains("Memory 2 (relevance: 0.910)"));
            assertTrue(prompt.contains("Memory 3 (relevance: 0.850)"));
            assertTrue(prompt.contains("Type: definition"));
            assertTrue(prompt.contains("Type: categorization"));
            assertTrue(prompt.contains("Type: examples"));
            assertTrue(prompt.contains("=== ADDITIONAL CONTEXT ==="));
            assertTrue(prompt.contains("学习者水平: 初学者"));
            assertTrue(prompt.contains("是否需要例子: true"));
            assertTrue(prompt.contains("User: 用户想了解机器学习的基础知识"));
            assertTrue(prompt.contains("Assistant: Please provide a helpful response"));
            assertTrue(prompt.contains("retrieved memories above"));
            assertTrue(prompt.contains("cite specific memories"));
        }

        @Test
        @DisplayName("无检索内存的提示词构建")
        void testPromptBuildingWithoutRetrievedMemories() {
            context.setSystemMessage("你是一个通用AI助手。");
            
            Map<String, Object> additionalContext = new HashMap<>();
            additionalContext.put("session_id", "12345");
            additionalContext.put("timestamp", "2024-08-30");
            context.setAdditionalContext(additionalContext);
            
            String prompt = template.buildPrompt(context);
            
            assertTrue(prompt.contains("你是一个通用AI助手。"));
            assertTrue(prompt.contains("=== ADDITIONAL CONTEXT ==="));
            assertTrue(prompt.contains("session_id: 12345"));
            assertTrue(prompt.contains("User: 用户想了解机器学习的基础知识"));
            assertTrue(prompt.contains("Assistant: Please provide a helpful response"));
            assertFalse(prompt.contains("retrieved memories above")); // 无检索内存时不应该提及
            assertFalse(prompt.contains("=== RETRIEVED MEMORIES ==="));
        }

        @Test
        @DisplayName("仅有检索内存的提示词构建")
        void testPromptBuildingWithOnlyRetrievedMemories() {
            // 只设置检索内存，不设置额外上下文
            List<RetrievedMemory> memories = Arrays.asList(
                new RetrievedMemory("Python是机器学习的主要编程语言", 0.88, null, "fact")
            );
            context.setRetrievedMemories(memories);
            
            String prompt = template.buildPrompt(context);
            
            assertTrue(prompt.contains("AI assistant with access to retrieved memories")); // 默认系统消息
            assertTrue(prompt.contains("=== RETRIEVED MEMORIES ==="));
            assertTrue(prompt.contains("Python是机器学习的主要编程语言"));
            assertTrue(prompt.contains("retrieved memories above"));
            assertFalse(prompt.contains("=== ADDITIONAL CONTEXT ==="));
        }
    }

    @Nested
    @DisplayName("异常处理和边界测试")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("null上下文异常处理")
        void testNullContext() {
            assertThrows(Exception.class, () -> {
                template.buildPrompt(null);
            });
        }

        @Test
        @DisplayName("null查询处理")
        void testNullQuery() {
            context.setUserQuery(null);
            
            assertDoesNotThrow(() -> {
                String prompt = template.buildPrompt(context);
                assertTrue(prompt.contains("User: null"));
            });
        }

        @Test
        @DisplayName("极长查询处理")
        void testVeryLongQuery() {
            StringBuilder longQuery = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                longQuery.append("这是一个非常长的查询，用于测试模板对超长文本的处理能力。");
            }
            context.setUserQuery(longQuery.toString());
            
            assertDoesNotThrow(() -> {
                String prompt = template.buildPrompt(context);
                assertTrue(prompt.contains("这是一个非常长的查询"));
            });
        }

        @Test
        @DisplayName("特殊字符处理")
        void testSpecialCharacters() {
            context.setUserQuery("查询包含特殊字符：<>&\"'\\n\\t\\r");
            context.setSystemMessage("系统消息包含特殊字符：@#$%^&*()");
            
            List<RetrievedMemory> memories = Arrays.asList(
                new RetrievedMemory("内存内容包含HTML标签：<html><body></body></html>", 0.8, null, "html")
            );
            context.setRetrievedMemories(memories);
            
            assertDoesNotThrow(() -> {
                String prompt = template.buildPrompt(context);
                assertTrue(prompt.contains("<>&\"'"));
                assertTrue(prompt.contains("@#$%^&*()"));
                assertTrue(prompt.contains("<html>"));
            });
        }
    }

    @Nested
    @DisplayName("性能和并发测试")
    class PerformanceTests {

        @Test
        @DisplayName("大量内存处理性能")
        void testLargeMemoryListPerformance() {
            List<RetrievedMemory> largeMemoryList = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                largeMemoryList.add(new RetrievedMemory(
                    "大量内存测试内容 " + i + " - 这是用于性能测试的长文本内容，包含了各种信息和数据。",
                    0.9 - (i * 0.005),
                    createMetadata("index", i, "category", "performance_test"),
                    "test_memory"
                ));
            }
            context.setRetrievedMemories(largeMemoryList);
            
            long startTime = System.nanoTime();
            String prompt = template.buildPrompt(context);
            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;
            
            assertNotNull(prompt);
            assertTrue(prompt.contains("Memory 1"));
            assertTrue(prompt.contains("Memory 100"));
            assertTrue(durationMs < 100, "大量内存处理耗时过长: " + durationMs + "ms");
        }

        @Test
        @DisplayName("并发提示词构建")
        void testConcurrentPromptBuilding() {
            int threadCount = 10;
            int operationsPerThread = 20;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            
            List<CompletableFuture<Void>> futures = IntStream.range(0, threadCount)
                .mapToObj(threadId -> CompletableFuture.runAsync(() -> {
                    final int finalThreadId = threadId; // Make threadId effectively final
                    for (int i = 0; i < operationsPerThread; i++) {
                        final int finalI = i; // Make i effectively final
                        PromptContext threadContext = new PromptContext("线程" + finalThreadId + "查询" + finalI);
                        threadContext.setSystemMessage("线程" + finalThreadId + "系统消息");
                        
                        List<RetrievedMemory> memories = Arrays.asList(
                            new RetrievedMemory("线程" + finalThreadId + "内存" + finalI, 0.8, null, "concurrent")
                        );
                        threadContext.setRetrievedMemories(memories);
                        
                        assertDoesNotThrow(() -> {
                            String result = template.buildPrompt(threadContext);
                            assertNotNull(result);
                            assertTrue(result.contains("线程" + finalThreadId + "查询" + finalI));
                            assertTrue(result.contains("线程" + finalThreadId + "内存" + finalI));
                        });
                    }
                }, executor))
                .collect(Collectors.toList());
            
            assertDoesNotThrow(() -> 
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join());
            
            executor.shutdown();
        }

        @Test
        @DisplayName("内存使用量测试")
        void testMemoryUsage() {
            // 创建大量数据来测试内存效率
            List<RetrievedMemory> memories = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("id", "memory_" + i);
                metadata.put("timestamp", System.currentTimeMillis());
                metadata.put("source", "memory_test");
                
                memories.add(new RetrievedMemory(
                    "内存内容 " + i + " - 包含详细信息和元数据的测试内容",
                    Math.random(),
                    metadata,
                    "memory_type_" + (i % 5)
                ));
            }
            
            Map<String, Object> additionalContext = new HashMap<>();
            for (int i = 0; i < 50; i++) {
                additionalContext.put("context_key_" + i, "context_value_" + i);
            }
            
            context.setRetrievedMemories(memories);
            context.setAdditionalContext(additionalContext);
            
            // 多次构建提示词，观察是否有内存泄漏
            for (int i = 0; i < 10; i++) {
                assertDoesNotThrow(() -> {
                    String prompt = template.buildPrompt(context);
                    assertNotNull(prompt);
                    assertTrue(prompt.contains("Memory 1000"));
                });
            }
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