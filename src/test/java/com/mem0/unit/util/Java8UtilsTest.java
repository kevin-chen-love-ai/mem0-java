package com.mem0.unit.util;

import com.mem0.util.Java8Utils;
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
 * Java8Utils 工具类的全面测试套件
 */
@DisplayName("Java8Utils Java8兼容性工具类测试")
class Java8UtilsTest {

    @Nested
    @DisplayName("Map创建功能测试")
    class MapCreationTests {

        @Test
        @DisplayName("创建2个键值对的不可变Map")
        void testMapOfTwoPairs() {
            Map<String, String> map = Java8Utils.mapOf("key1", "value1", "key2", "value2");
            
            assertNotNull(map);
            assertEquals(2, map.size());
            assertEquals("value1", map.get("key1"));
            assertEquals("value2", map.get("key2"));
            
            // 验证不可变性
            assertThrows(UnsupportedOperationException.class, () -> {
                map.put("key3", "value3");
            });
            
            assertThrows(UnsupportedOperationException.class, () -> {
                map.remove("key1");
            });
            
            assertThrows(UnsupportedOperationException.class, () -> {
                map.clear();
            });
        }

        @Test
        @DisplayName("创建3个键值对的不可变Map")
        void testMapOfThreePairs() {
            Map<String, Integer> map = Java8Utils.mapOf("one", 1, "two", 2, "three", 3);
            
            assertNotNull(map);
            assertEquals(3, map.size());
            assertEquals(Integer.valueOf(1), map.get("one"));
            assertEquals(Integer.valueOf(2), map.get("two"));
            assertEquals(Integer.valueOf(3), map.get("three"));
            
            // 验证不可变性
            assertThrows(UnsupportedOperationException.class, () -> {
                map.put("four", 4);
            });
        }

        @Test
        @DisplayName("创建4个键值对的不可变Map")
        void testMapOfFourPairs() {
            Map<String, Boolean> map = Java8Utils.mapOf(
                "enabled", true, 
                "visible", false, 
                "cached", true, 
                "readonly", false
            );
            
            assertNotNull(map);
            assertEquals(4, map.size());
            assertEquals(Boolean.TRUE, map.get("enabled"));
            assertEquals(Boolean.FALSE, map.get("visible"));
            assertEquals(Boolean.TRUE, map.get("cached"));
            assertEquals(Boolean.FALSE, map.get("readonly"));
            
            // 验证不可变性
            assertThrows(UnsupportedOperationException.class, () -> {
                map.replace("enabled", false);
            });
        }

        @Test
        @DisplayName("Map支持null值")
        void testMapWithNullValues() {
            Map<String, String> map = Java8Utils.mapOf("key1", null, "key2", "value2");
            
            assertEquals(2, map.size());
            assertNull(map.get("key1"));
            assertEquals("value2", map.get("key2"));
            assertTrue(map.containsKey("key1"));
            assertTrue(map.containsValue(null));
        }

        @Test
        @DisplayName("Map支持null键")
        void testMapWithNullKeys() {
            Map<String, String> map = Java8Utils.mapOf(null, "value1", "key2", "value2");
            
            assertEquals(2, map.size());
            assertEquals("value1", map.get(null));
            assertEquals("value2", map.get("key2"));
            assertTrue(map.containsKey(null));
        }

        @Test
        @DisplayName("创建混合类型的Map")
        void testMixedTypeMap() {
            Map<String, Object> map = Java8Utils.mapOf(
                "name", "test", 
                "count", 42, 
                "enabled", true, 
                "config", Arrays.asList("a", "b", "c")
            );
            
            assertEquals(4, map.size());
            assertEquals("test", map.get("name"));
            assertEquals(42, map.get("count"));
            assertEquals(true, map.get("enabled"));
            assertEquals(Arrays.asList("a", "b", "c"), map.get("config"));
        }
    }

    @Nested
    @DisplayName("Set创建功能测试")
    class SetCreationTests {

        @Test
        @DisplayName("创建空Set")
        void testEmptySet() {
            Set<String> set = Java8Utils.setOf();
            
            assertNotNull(set);
            assertEquals(0, set.size());
            assertTrue(set.isEmpty());
            
            // 验证不可变性
            assertThrows(UnsupportedOperationException.class, () -> {
                set.add("element");
            });
        }

        @Test
        @DisplayName("创建单元素Set")
        void testSingleElementSet() {
            Set<String> set = Java8Utils.setOf("element");
            
            assertNotNull(set);
            assertEquals(1, set.size());
            assertTrue(set.contains("element"));
            
            // 验证不可变性
            assertThrows(UnsupportedOperationException.class, () -> {
                set.add("another");
            });
            
            assertThrows(UnsupportedOperationException.class, () -> {
                set.remove("element");
            });
        }

        @Test
        @DisplayName("创建多元素Set")
        void testMultipleElementSet() {
            Set<String> set = Java8Utils.setOf("a", "b", "c", "d");
            
            assertNotNull(set);
            assertEquals(4, set.size());
            assertTrue(set.contains("a"));
            assertTrue(set.contains("b"));
            assertTrue(set.contains("c"));
            assertTrue(set.contains("d"));
            
            // 验证不可变性
            assertThrows(UnsupportedOperationException.class, () -> {
                set.clear();
            });
        }

        @Test
        @DisplayName("创建包含重复元素的Set")
        void testDuplicateElements() {
            Set<String> set = Java8Utils.setOf("a", "b", "a", "c", "b");
            
            assertNotNull(set);
            assertEquals(3, set.size()); // 重复元素被自动去除
            assertTrue(set.contains("a"));
            assertTrue(set.contains("b"));
            assertTrue(set.contains("c"));
        }

        @Test
        @DisplayName("创建包含null的Set")
        void testSetWithNull() {
            Set<String> set = Java8Utils.setOf("a", null, "b");
            
            assertEquals(3, set.size());
            assertTrue(set.contains("a"));
            assertTrue(set.contains(null));
            assertTrue(set.contains("b"));
        }

        @Test
        @DisplayName("创建不同类型元素的Set")
        void testMixedTypeSet() {
            Set<Object> set = Java8Utils.setOf("string", 42, true, null);
            
            assertEquals(4, set.size());
            assertTrue(set.contains("string"));
            assertTrue(set.contains(42));
            assertTrue(set.contains(true));
            assertTrue(set.contains(null));
        }

        @Test
        @DisplayName("大量元素Set性能测试")
        void testLargeSetPerformance() {
            String[] elements = new String[1000];
            for (int i = 0; i < elements.length; i++) {
                elements[i] = "element" + i;
            }
            
            long startTime = System.nanoTime();
            Set<String> set = Java8Utils.setOf(elements);
            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;
            
            assertEquals(1000, set.size());
            assertTrue(durationMs < 100, "大量元素Set创建耗时过长: " + durationMs + "ms");
        }
    }

    @Nested
    @DisplayName("字符串重复功能测试")
    class StringRepeatTests {

        @Test
        @DisplayName("正常字符串重复")
        void testNormalStringRepeat() {
            assertEquals("", Java8Utils.repeat("a", 0));
            assertEquals("a", Java8Utils.repeat("a", 1));
            assertEquals("aa", Java8Utils.repeat("a", 2));
            assertEquals("aaaaa", Java8Utils.repeat("a", 5));
            assertEquals("abababab", Java8Utils.repeat("ab", 4));
        }

        @Test
        @DisplayName("负数次数重复")
        void testNegativeCountRepeat() {
            assertEquals("", Java8Utils.repeat("test", -1));
            assertEquals("", Java8Utils.repeat("test", -10));
        }

        @Test
        @DisplayName("零次重复")
        void testZeroCountRepeat() {
            assertEquals("", Java8Utils.repeat("anything", 0));
            assertEquals("", Java8Utils.repeat("", 0));
        }

        @Test
        @DisplayName("空字符串重复")
        void testEmptyStringRepeat() {
            assertEquals("", Java8Utils.repeat("", 1));
            assertEquals("", Java8Utils.repeat("", 10));
            assertEquals("", Java8Utils.repeat("", 0));
        }

        @Test
        @DisplayName("单字符重复")
        void testSingleCharacterRepeat() {
            assertEquals("aaaaaaaaaa", Java8Utils.repeat("a", 10));
            assertEquals("----------", Java8Utils.repeat("-", 10));
            assertEquals("**********", Java8Utils.repeat("*", 10));
        }

        @Test
        @DisplayName("多字符字符串重复")
        void testMultiCharacterStringRepeat() {
            assertEquals("abcabcabc", Java8Utils.repeat("abc", 3));
            assertEquals("HelloHelloHello", Java8Utils.repeat("Hello", 3));
            assertEquals("123123123123", Java8Utils.repeat("123", 4));
        }

        @Test
        @DisplayName("特殊字符重复")
        void testSpecialCharacterRepeat() {
            assertEquals("   ", Java8Utils.repeat(" ", 3));
            assertEquals("\n\n\n", Java8Utils.repeat("\n", 3));
            assertEquals("\t\t", Java8Utils.repeat("\t", 2));
            assertEquals("@#$@#$", Java8Utils.repeat("@#$", 2));
        }

        @Test
        @DisplayName("Unicode字符重复")
        void testUnicodeCharacterRepeat() {
            assertEquals("😀😀😀", Java8Utils.repeat("😀", 3));
            assertEquals("中文中文", Java8Utils.repeat("中文", 2));
            assertEquals("αβγαβγ", Java8Utils.repeat("αβγ", 2));
        }

        @Test
        @DisplayName("大量重复性能测试")
        void testLargeRepeatPerformance() {
            long startTime = System.nanoTime();
            String result = Java8Utils.repeat("test", 10000);
            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;
            
            assertEquals(40000, result.length()); // "test" * 10000 = 40000 characters
            assertTrue(result.startsWith("test"));
            assertTrue(result.endsWith("test"));
            assertTrue(durationMs < 100, "大量重复操作耗时过长: " + durationMs + "ms");
        }

        @Test
        @DisplayName("内存效率测试")
        void testMemoryEfficiency() {
            // 测试是否正确计算StringBuilder容量
            String base = "abcdefghij"; // 10个字符
            String result = Java8Utils.repeat(base, 1000);
            
            assertEquals(10000, result.length());
            assertTrue(result.startsWith(base));
            assertTrue(result.endsWith(base));
            
            // 验证内容的正确性
            for (int i = 0; i < 1000; i++) {
                String substring = result.substring(i * 10, (i + 1) * 10);
                assertEquals(base, substring);
            }
        }
    }

    @Nested
    @DisplayName("边界条件和异常处理")
    class EdgeCaseTests {

        @Test
        @DisplayName("Map键重复处理")
        void testMapWithDuplicateKeys() {
            // 测试重复键的行为（应该保留后一个值）
            Map<String, String> map = Java8Utils.mapOf("key", "value1", "key", "value2");
            
            assertEquals(1, map.size());
            assertEquals("value2", map.get("key")); // 后面的值应该覆盖前面的值
        }

        @Test
        @DisplayName("Set迭代器不可修改")
        void testSetIteratorImmutability() {
            Set<String> set = Java8Utils.setOf("a", "b", "c");
            Iterator<String> iterator = set.iterator();
            
            assertTrue(iterator.hasNext());
            assertEquals("a", iterator.next()); // 注意：HashSet的迭代顺序不确定
            
            assertThrows(UnsupportedOperationException.class, iterator::remove);
        }

        @Test
        @DisplayName("Map keySet不可修改")
        void testMapKeySetImmutability() {
            Map<String, String> map = Java8Utils.mapOf("key1", "value1", "key2", "value2");
            Set<String> keySet = map.keySet();
            
            assertEquals(2, keySet.size());
            assertThrows(UnsupportedOperationException.class, () -> {
                keySet.add("key3");
            });
            assertThrows(UnsupportedOperationException.class, () -> {
                keySet.remove("key1");
            });
        }

        @Test
        @DisplayName("Map values不可修改")
        void testMapValuesImmutability() {
            Map<String, String> map = Java8Utils.mapOf("key1", "value1", "key2", "value2");
            Collection<String> values = map.values();
            
            assertEquals(2, values.size());
            assertThrows(UnsupportedOperationException.class, () -> {
                values.add("value3");
            });
            assertThrows(UnsupportedOperationException.class, () -> {
                values.remove("value1");
            });
        }

        @Test
        @DisplayName("字符串重复极值测试")
        void testStringRepeatExtremeValues() {
            // 测试Integer.MAX_VALUE会导致内存溢出，所以这里测试较小的极值
            assertEquals("", Java8Utils.repeat("", Integer.MAX_VALUE));
            
            // 测试大数值（但不会导致内存溢出）
            String result = Java8Utils.repeat("a", 100000);
            assertEquals(100000, result.length());
            assertTrue(result.matches("a+"));
        }
    }

    @Nested
    @DisplayName("并发安全性测试")
    class ConcurrencyTests {

        @Test
        @DisplayName("并发创建Map")
        void testConcurrentMapCreation() {
            int threadCount = 10;
            int operationsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            
            List<CompletableFuture<Void>> futures = IntStream.range(0, threadCount)
                .mapToObj(threadId -> CompletableFuture.runAsync(() -> {
                    final int finalThreadId = threadId;
                    for (int i = 0; i < operationsPerThread; i++) {
                        final int finalI = i;
                        Map<String, String> map = Java8Utils.mapOf(
                            "thread", String.valueOf(finalThreadId),
                            "operation", String.valueOf(finalI)
                        );
                        
                        assertEquals(2, map.size());
                        assertEquals(String.valueOf(finalThreadId), map.get("thread"));
                        assertEquals(String.valueOf(finalI), map.get("operation"));
                    }
                }, executor))
                .collect(Collectors.toList());
            
            assertDoesNotThrow(() -> 
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join());
            
            executor.shutdown();
        }

        @Test
        @DisplayName("并发创建Set")
        void testConcurrentSetCreation() {
            int threadCount = 8;
            int operationsPerThread = 50;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            
            List<CompletableFuture<Void>> futures = IntStream.range(0, threadCount)
                .mapToObj(threadId -> CompletableFuture.runAsync(() -> {
                    final int finalThreadId = threadId;
                    for (int i = 0; i < operationsPerThread; i++) {
                        Set<String> set = Java8Utils.setOf(
                            "thread" + finalThreadId,
                            "operation" + i,
                            "data"
                        );
                        
                        assertEquals(3, set.size());
                        assertTrue(set.contains("thread" + finalThreadId));
                        assertTrue(set.contains("operation" + i));
                        assertTrue(set.contains("data"));
                    }
                }, executor))
                .collect(Collectors.toList());
            
            assertDoesNotThrow(() -> 
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join());
            
            executor.shutdown();
        }

        @Test
        @DisplayName("并发字符串重复")
        void testConcurrentStringRepeat() {
            int threadCount = 5;
            int operationsPerThread = 20;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            
            List<CompletableFuture<Void>> futures = IntStream.range(0, threadCount)
                .mapToObj(threadId -> CompletableFuture.runAsync(() -> {
                    final int finalThreadId = threadId;
                    for (int i = 1; i <= operationsPerThread; i++) {
                        String base = "T" + finalThreadId;
                        String result = Java8Utils.repeat(base, i);
                        
                        assertEquals(base.length() * i, result.length());
                        assertTrue(result.startsWith(base));
                        if (i > 1) {
                            assertTrue(result.endsWith(base));
                        }
                    }
                }, executor))
                .collect(Collectors.toList());
            
            assertDoesNotThrow(() -> 
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join());
            
            executor.shutdown();
        }
    }

    @Nested
    @DisplayName("实际使用场景测试")
    class PracticalUsageTests {

        @Test
        @DisplayName("配置Map创建")
        void testConfigurationMap() {
            Map<String, Object> config = Java8Utils.mapOf(
                "host", "localhost",
                "port", 8080,
                "timeout", 30000,
                "ssl", true
            );
            
            assertEquals("localhost", config.get("host"));
            assertEquals(8080, config.get("port"));
            assertEquals(30000, config.get("timeout"));
            assertEquals(true, config.get("ssl"));
            
            // 配置应该是不可变的
            assertThrows(UnsupportedOperationException.class, () -> {
                config.put("newConfig", "value");
            });
        }

        @Test
        @DisplayName("支持格式Set创建")
        void testSupportedFormatsSet() {
            Set<String> supportedFormats = Java8Utils.setOf("json", "xml", "yaml", "properties");
            
            assertTrue(supportedFormats.contains("json"));
            assertTrue(supportedFormats.contains("xml"));
            assertTrue(supportedFormats.contains("yaml"));
            assertTrue(supportedFormats.contains("properties"));
            assertFalse(supportedFormats.contains("csv"));
            
            // 格式集合应该是不可变的
            assertThrows(UnsupportedOperationException.class, () -> {
                supportedFormats.add("csv");
            });
        }

        @Test
        @DisplayName("文本格式化场景")
        void testTextFormattingScenarios() {
            // 分隔线
            String separator = Java8Utils.repeat("=", 50);
            assertEquals(50, separator.length());
            assertEquals("==================================================", separator);
            
            // 缩进
            String indent = Java8Utils.repeat("  ", 4);
            assertEquals("        ", indent);
            
            // 填充
            String padding = Java8Utils.repeat(" ", 20);
            String paddedText = "Title" + padding + "Value";
            assertEquals(30, paddedText.length());
            
            // 装饰性边框
            String border = Java8Utils.repeat("*-", 10);
            assertEquals("*-*-*-*-*-*-*-*-*-*-", border);
        }

        @Test
        @DisplayName("数据结构初始化场景")
        void testDataStructureInitialization() {
            // HTTP状态码映射
            Map<Integer, String> statusCodes = Java8Utils.mapOf(
                200, "OK",
                404, "Not Found",
                500, "Internal Server Error",
                403, "Forbidden"
            );
            
            assertEquals("OK", statusCodes.get(200));
            assertEquals("Not Found", statusCodes.get(404));
            assertEquals("Internal Server Error", statusCodes.get(500));
            assertEquals("Forbidden", statusCodes.get(403));
            
            // 权限集合
            Set<String> permissions = Java8Utils.setOf("read", "write", "delete", "admin");
            assertTrue(permissions.contains("read"));
            assertTrue(permissions.contains("admin"));
            assertEquals(4, permissions.size());
        }

        @Test
        @DisplayName("日志和调试场景")
        void testLoggingAndDebuggingScenarios() {
            // 日志级别
            Set<String> logLevels = Java8Utils.setOf("DEBUG", "INFO", "WARN", "ERROR");
            assertTrue(logLevels.contains("INFO"));
            assertTrue(logLevels.contains("ERROR"));
            
            // 调试分隔符
            String debugSeparator = Java8Utils.repeat("-", 80);
            assertEquals(80, debugSeparator.length());
            
            // 缩进显示嵌套结构
            String level1 = Java8Utils.repeat("  ", 1) + "Level 1";
            String level2 = Java8Utils.repeat("  ", 2) + "Level 2";
            String level3 = Java8Utils.repeat("  ", 3) + "Level 3";
            
            assertEquals("  Level 1", level1);
            assertEquals("    Level 2", level2);
            assertEquals("      Level 3", level3);
        }
    }

    @Nested
    @DisplayName("性能和内存测试")
    class PerformanceTests {

        @Test
        @DisplayName("Map创建性能测试")
        void testMapCreationPerformance() {
            int iterations = 10000;
            
            long startTime = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                Map<String, Integer> map = Java8Utils.mapOf(
                    "key1", i,
                    "key2", i * 2,
                    "key3", i * 3,
                    "key4", i * 4
                );
                assertNotNull(map);
            }
            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;
            
            assertTrue(durationMs < 1000, "Map创建性能测试失败，耗时: " + durationMs + "ms");
        }

        @Test
        @DisplayName("Set创建性能测试")
        void testSetCreationPerformance() {
            int iterations = 5000;
            
            long startTime = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                Set<String> set = Java8Utils.setOf("a" + i, "b" + i, "c" + i, "d" + i, "e" + i);
                assertNotNull(set);
                assertEquals(5, set.size());
            }
            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;
            
            assertTrue(durationMs < 500, "Set创建性能测试失败，耗时: " + durationMs + "ms");
        }

        @Test
        @DisplayName("字符串重复性能测试")
        void testStringRepeatPerformance() {
            int iterations = 1000;
            
            long startTime = System.nanoTime();
            for (int i = 1; i <= iterations; i++) {
                String result = Java8Utils.repeat("test", i);
                assertEquals("test".length() * i, result.length());
            }
            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;
            
            assertTrue(durationMs < 200, "字符串重复性能测试失败，耗时: " + durationMs + "ms");
        }
    }
}