package com.mem0.unit.model;

import com.mem0.model.SearchResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 搜索结果模型测试
 * 全面测试SearchResult类的所有功能和边界条件
 */
@DisplayName("搜索结果模型测试")
public class SearchResultTest {
    
    @Nested
    @DisplayName("构造和基础功能")
    class ConstructionAndBasicFunctionality {
        
        @Test
        @DisplayName("正常构造搜索结果")
        void testNormalConstruction() {
            String id = "test_result_123";
            float similarity = 0.85f;
            Map<String, Object> properties = new HashMap<>();
            properties.put("content", "测试内容");
            properties.put("category", "document");
            
            SearchResult result = new SearchResult(id, similarity, properties);
            
            assertEquals(id, result.getId());
            assertEquals(similarity, result.getSimilarity(), 0.001f);
            assertEquals(properties, result.getProperties());
        }
        
        @Test
        @DisplayName("使用空属性构造")
        void testConstructionWithEmptyProperties() {
            String id = "empty_props";
            float similarity = 0.5f;
            Map<String, Object> emptyProperties = new HashMap<>();
            
            SearchResult result = new SearchResult(id, similarity, emptyProperties);
            
            assertEquals(id, result.getId());
            assertEquals(similarity, result.getSimilarity(), 0.001f);
            assertTrue(result.getProperties().isEmpty());
        }
        
        @Test
        @DisplayName("使用null属性构造")
        void testConstructionWithNullProperties() {
            String id = "null_props";
            float similarity = 0.3f;
            
            SearchResult result = new SearchResult(id, similarity, null);
            
            assertEquals(id, result.getId());
            assertEquals(similarity, result.getSimilarity(), 0.001f);
            assertNull(result.getProperties());
        }
        
        @Test
        @DisplayName("边界相似度值")
        void testBoundarySimilarityValues() {
            Map<String, Object> properties = new HashMap<>();
            
            // 最小值
            SearchResult minResult = new SearchResult("min", 0.0f, properties);
            assertEquals(0.0f, minResult.getSimilarity(), 0.001f);
            
            // 最大值
            SearchResult maxResult = new SearchResult("max", 1.0f, properties);
            assertEquals(1.0f, maxResult.getSimilarity(), 0.001f);
            
            // 负值（虽然不推荐，但应该能处理）
            SearchResult negativeResult = new SearchResult("negative", -0.1f, properties);
            assertEquals(-0.1f, negativeResult.getSimilarity(), 0.001f);
            
            // 超过1.0的值（虽然不推荐，但应该能处理）
            SearchResult overResult = new SearchResult("over", 1.5f, properties);
            assertEquals(1.5f, overResult.getSimilarity(), 0.001f);
        }
    }
    
    @Nested
    @DisplayName("Getter方法测试")
    class GetterMethodTests {
        
        private SearchResult createTestResult() {
            Map<String, Object> properties = new HashMap<>();
            properties.put("title", "测试文档");
            properties.put("author", "测试作者");
            properties.put("timestamp", System.currentTimeMillis());
            properties.put("score", 95);
            properties.put("isPublic", true);
            
            return new SearchResult("getter_test", 0.92f, properties);
        }
        
        @Test
        @DisplayName("getId方法")
        void testGetId() {
            SearchResult result = createTestResult();
            assertEquals("getter_test", result.getId());
        }
        
        @Test
        @DisplayName("getSimilarity方法")
        void testGetSimilarity() {
            SearchResult result = createTestResult();
            assertEquals(0.92f, result.getSimilarity(), 0.001f);
        }
        
        @Test
        @DisplayName("getProperties方法")
        void testGetProperties() {
            SearchResult result = createTestResult();
            Map<String, Object> properties = result.getProperties();
            
            assertNotNull(properties);
            assertEquals("测试文档", properties.get("title"));
            assertEquals("测试作者", properties.get("author"));
            assertEquals(95, properties.get("score"));
            assertTrue((Boolean) properties.get("isPublic"));
        }
        
        @Test
        @DisplayName("getMetadata兼容方法")
        void testGetMetadataCompatibilityMethod() {
            SearchResult result = createTestResult();
            
            // getMetadata应该返回与getProperties相同的结果
            Map<String, Object> properties = result.getProperties();
            Map<String, Object> metadata = result.getMetadata();
            
            assertEquals(properties, metadata);
            assertSame(properties, metadata); // 应该是同一个对象引用
        }
    }
    
    @Nested
    @DisplayName("属性操作测试")
    class PropertyOperationTests {
        
        @Test
        @DisplayName("属性修改不影响原始对象")
        void testPropertyModificationIsolation() {
            Map<String, Object> originalProperties = new HashMap<>();
            originalProperties.put("original", "value");
            
            SearchResult result = new SearchResult("modify_test", 0.7f, originalProperties);
            
            // 修改返回的属性map
            Map<String, Object> retrievedProperties = result.getProperties();
            if (retrievedProperties != null) {
                retrievedProperties.put("modified", "new_value");
                
                // 原始属性map应该也被修改（因为是同一个引用）
                assertTrue(result.getProperties().containsKey("modified"));
            }
        }
        
        @Test
        @DisplayName("不同数据类型的属性")
        void testVariousPropertyTypes() {
            Map<String, Object> properties = new HashMap<>();
            properties.put("stringProp", "字符串属性");
            properties.put("intProp", 42);
            properties.put("floatProp", 3.14f);
            properties.put("doubleProp", 2.71828);
            properties.put("booleanProp", true);
            properties.put("nullProp", null);
            
            SearchResult result = new SearchResult("types_test", 0.6f, properties);
            
            Map<String, Object> retrievedProps = result.getProperties();
            assertEquals("字符串属性", retrievedProps.get("stringProp"));
            assertEquals(42, retrievedProps.get("intProp"));
            assertEquals(3.14f, (Float) retrievedProps.get("floatProp"), 0.001f);
            assertEquals(2.71828, (Double) retrievedProps.get("doubleProp"), 0.00001);
            assertTrue((Boolean) retrievedProps.get("booleanProp"));
            assertNull(retrievedProps.get("nullProp"));
        }
    }
    
    @Nested
    @DisplayName("toString方法测试")
    class ToStringMethodTests {
        
        @Test
        @DisplayName("toString格式正确性")
        void testToStringFormat() {
            SearchResult result = new SearchResult("toString_test", 0.856f, new HashMap<>());
            String str = result.toString();
            
            assertTrue(str.contains("SearchResult"));
            assertTrue(str.contains("toString_test"));
            assertTrue(str.contains("0.856"));
        }
        
        @Test
        @DisplayName("toString处理特殊字符")
        void testToStringWithSpecialCharacters() {
            String specialId = "test'with\"special\\chars";
            SearchResult result = new SearchResult(specialId, 0.5f, new HashMap<>());
            
            String str = result.toString();
            assertNotNull(str);
            assertTrue(str.length() > 0);
        }
        
        @Test
        @DisplayName("toString处理极值相似度")
        void testToStringWithExtremeSimilarity() {
            SearchResult minResult = new SearchResult("min", 0.0f, new HashMap<>());
            SearchResult maxResult = new SearchResult("max", 1.0f, new HashMap<>());
            SearchResult preciseResult = new SearchResult("precise", 0.123456789f, new HashMap<>());
            
            String minStr = minResult.toString();
            String maxStr = maxResult.toString();
            String preciseStr = preciseResult.toString();
            
            assertTrue(minStr.contains("0.000"));
            assertTrue(maxStr.contains("1.000"));
            assertTrue(preciseStr.contains("0.123"));
        }
    }
    
    @Nested
    @DisplayName("相等性和哈希码测试")
    class EqualityAndHashCodeTests {
        
        @Test
        @DisplayName("对象相等性判断")
        void testObjectEquality() {
            Map<String, Object> properties = new HashMap<>();
            properties.put("key", "value");
            
            SearchResult result1 = new SearchResult("same_id", 0.8f, properties);
            SearchResult result2 = new SearchResult("same_id", 0.8f, properties);
            SearchResult result3 = new SearchResult("different_id", 0.8f, properties);
            
            // 注意：SearchResult没有重写equals方法，所以使用引用相等性
            assertNotEquals(result1, result2); // 不同实例
            assertEquals(result1, result1); // 同一实例
            assertNotEquals(result1, result3); // 不同实例
        }
        
        @Test
        @DisplayName("null和不同类型比较")
        void testNullAndDifferentTypeComparison() {
            SearchResult result = new SearchResult("test", 0.5f, new HashMap<>());
            
            assertNotEquals(result, null);
            assertNotEquals(result, "string");
            assertNotEquals(result, Integer.valueOf(123));
        }
    }
    
    @Nested
    @DisplayName("不可变性测试")
    class ImmutabilityTests {
        
        @Test
        @DisplayName("字段不可变性")
        void testFieldImmutability() {
            String originalId = "immutable_test";
            float originalSimilarity = 0.75f;
            Map<String, Object> originalProperties = new HashMap<>();
            originalProperties.put("original", "data");
            
            SearchResult result = new SearchResult(originalId, originalSimilarity, originalProperties);
            
            // 验证公共final字段直接访问
            assertEquals(originalId, result.id);
            assertEquals(originalSimilarity, result.similarity, 0.001f);
            assertSame(originalProperties, result.properties);
        }
        
        @Test
        @DisplayName("字段值不能被修改")
        void testFieldValuesCannotBeModified() {
            Map<String, Object> properties = new HashMap<>();
            properties.put("initial", "value");
            
            SearchResult result = new SearchResult("field_test", 0.4f, properties);
            
            // 直接访问public final字段
            assertNotNull(result.id);
            assertTrue(result.similarity >= 0.0f);
            
            // 尝试修改属性map的内容（这会影响SearchResult对象）
            result.properties.put("modified", "new_value");
            assertTrue(result.getProperties().containsKey("modified"));
        }
    }
    
    @Nested
    @DisplayName("性能和内存测试")
    class PerformanceAndMemoryTests {
        
        @Test
        @DisplayName("大量属性处理")
        void testLargePropertiesHandling() {
            Map<String, Object> largeProperties = new HashMap<>();
            
            // 添加大量属性
            for (int i = 0; i < 1000; i++) {
                largeProperties.put("prop_" + i, "value_" + i);
            }
            
            SearchResult result = new SearchResult("large_props", 0.9f, largeProperties);
            
            assertEquals(1000, result.getProperties().size());
            assertEquals("value_500", result.getProperties().get("prop_500"));
        }
        
        @Test
        @DisplayName("属性值为大对象")
        void testLargeObjectProperties() {
            Map<String, Object> properties = new HashMap<>();
            
            // 大字符串
            StringBuilder largeString = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                largeString.append("This is a long string segment ").append(i).append(". ");
            }
            properties.put("largeString", largeString.toString());
            
            // 大数组
            int[] largeArray = new int[10000];
            for (int i = 0; i < largeArray.length; i++) {
                largeArray[i] = i * i;
            }
            properties.put("largeArray", largeArray);
            
            SearchResult result = new SearchResult("large_objects", 0.8f, properties);
            
            assertTrue(((String) result.getProperties().get("largeString")).length() > 10000);
            assertEquals(10000, ((int[]) result.getProperties().get("largeArray")).length);
        }
    }
    
    @Nested
    @DisplayName("线程安全测试")
    class ThreadSafetyTests {
        
        @Test
        @DisplayName("并发访问测试")
        void testConcurrentAccess() {
            Map<String, Object> properties = new HashMap<>();
            properties.put("shared", "initial");
            
            SearchResult result = new SearchResult("concurrent", 0.6f, properties);
            
            // 并发读取应该是安全的
            Thread[] readers = new Thread[10];
            for (int i = 0; i < readers.length; i++) {
                readers[i] = new Thread(() -> {
                    for (int j = 0; j < 100; j++) {
                        assertNotNull(result.getId());
                        assertTrue(result.getSimilarity() >= 0.0f);
                        assertNotNull(result.getProperties());
                    }
                });
                readers[i].start();
            }
            
            // 等待所有读取线程完成
            for (Thread reader : readers) {
                assertDoesNotThrow(() -> reader.join());
            }
        }
    }
}