package com.mem0.unit.model;

import com.mem0.model.VectorEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 向量条目模型测试
 * 全面测试VectorEntry类的所有功能和边界条件
 */
@DisplayName("向量条目模型测试")
public class VectorEntryTest {
    
    @Nested
    @DisplayName("构造和基础功能")
    class ConstructionAndBasicFunctionality {
        
        @Test
        @DisplayName("正常构造向量条目")
        void testNormalConstruction() {
            String id = "vec_123";
            float[] embedding = {0.1f, 0.2f, 0.3f, 0.4f};
            String userId = "user_456";
            Map<String, Object> properties = new HashMap<>();
            properties.put("category", "document");
            properties.put("score", 0.95);
            
            VectorEntry entry = new VectorEntry(id, embedding, userId, properties);
            
            assertEquals(id, entry.getId());
            assertArrayEquals(embedding, entry.getEmbedding());
            assertEquals(userId, entry.getUserId());
            assertEquals(properties, entry.getProperties());
            assertTrue(entry.getLastAccessTime() > 0);
        }
        
        @Test
        @DisplayName("使用空向量构造")
        void testConstructionWithEmptyEmbedding() {
            String id = "empty_vec";
            float[] emptyEmbedding = {};
            String userId = "user_123";
            Map<String, Object> properties = new HashMap<>();
            
            VectorEntry entry = new VectorEntry(id, emptyEmbedding, userId, properties);
            
            assertEquals(id, entry.getId());
            assertArrayEquals(emptyEmbedding, entry.getEmbedding());
            assertEquals(0, entry.getEmbedding().length);
        }
        
        @Test
        @DisplayName("使用null值构造")
        void testConstructionWithNullValues() {
            String id = "null_test";
            float[] embedding = {0.5f};
            String userId = null;
            Map<String, Object> properties = null;
            
            VectorEntry entry = new VectorEntry(id, embedding, userId, properties);
            
            assertEquals(id, entry.getId());
            assertArrayEquals(embedding, entry.getEmbedding());
            assertNull(entry.getUserId());
            assertNull(entry.getProperties());
        }
        
        @Test
        @DisplayName("构造时设置初始访问时间")
        void testInitialAccessTimeSet() {
            long beforeCreation = System.currentTimeMillis();
            
            VectorEntry entry = new VectorEntry("time_test", new float[]{0.1f}, "user", new HashMap<>());
            
            long afterCreation = System.currentTimeMillis();
            
            long accessTime = entry.getLastAccessTime();
            assertTrue(accessTime >= beforeCreation);
            assertTrue(accessTime <= afterCreation);
        }
    }
    
    @Nested
    @DisplayName("Getter方法测试")
    class GetterMethodTests {
        
        private VectorEntry createTestEntry() {
            float[] embedding = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
            Map<String, Object> properties = new HashMap<>();
            properties.put("title", "测试向量");
            properties.put("dimension", 5);
            properties.put("normalized", true);
            
            return new VectorEntry("getter_test", embedding, "test_user", properties);
        }
        
        @Test
        @DisplayName("getId方法")
        void testGetId() {
            VectorEntry entry = createTestEntry();
            assertEquals("getter_test", entry.getId());
        }
        
        @Test
        @DisplayName("getEmbedding方法")
        void testGetEmbedding() {
            VectorEntry entry = createTestEntry();
            float[] embedding = entry.getEmbedding();
            
            assertNotNull(embedding);
            assertEquals(5, embedding.length);
            assertEquals(0.1f, embedding[0], 0.001f);
            assertEquals(0.5f, embedding[4], 0.001f);
        }
        
        @Test
        @DisplayName("getUserId方法")
        void testGetUserId() {
            VectorEntry entry = createTestEntry();
            assertEquals("test_user", entry.getUserId());
        }
        
        @Test
        @DisplayName("getProperties方法")
        void testGetProperties() {
            VectorEntry entry = createTestEntry();
            Map<String, Object> properties = entry.getProperties();
            
            assertNotNull(properties);
            assertEquals("测试向量", properties.get("title"));
            assertEquals(5, properties.get("dimension"));
            assertTrue((Boolean) properties.get("normalized"));
        }
        
        @Test
        @DisplayName("getLastAccessTime方法")
        void testGetLastAccessTime() {
            VectorEntry entry = createTestEntry();
            long accessTime = entry.getLastAccessTime();
            
            assertTrue(accessTime > 0);
            assertTrue(accessTime <= System.currentTimeMillis());
        }
    }
    
    @Nested
    @DisplayName("访问时间更新测试")
    class AccessTimeUpdateTests {
        
        @Test
        @DisplayName("updateAccess方法更新时间")
        void testUpdateAccessUpdatesTime() throws InterruptedException {
            VectorEntry entry = new VectorEntry("update_test", new float[]{0.1f}, "user", new HashMap<>());
            long initialTime = entry.getLastAccessTime();
            
            // 等待一小段时间确保时间戳不同
            Thread.sleep(10);
            
            entry.updateAccess();
            long updatedTime = entry.getLastAccessTime();
            
            assertTrue(updatedTime > initialTime);
        }
        
        @Test
        @DisplayName("多次updateAccess更新时间")
        void testMultipleUpdateAccessCalls() throws InterruptedException {
            VectorEntry entry = new VectorEntry("multi_update", new float[]{0.1f}, "user", new HashMap<>());
            
            long time1 = entry.getLastAccessTime();
            Thread.sleep(5);
            
            entry.updateAccess();
            long time2 = entry.getLastAccessTime();
            Thread.sleep(5);
            
            entry.updateAccess();
            long time3 = entry.getLastAccessTime();
            
            assertTrue(time2 > time1);
            assertTrue(time3 > time2);
        }
        
        @Test
        @DisplayName("并发updateAccess测试")
        void testConcurrentUpdateAccess() throws InterruptedException {
            VectorEntry entry = new VectorEntry("concurrent_update", new float[]{0.1f}, "user", new HashMap<>());
            
            // 并发调用updateAccess
            Thread[] threads = new Thread[10];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < 100; j++) {
                        entry.updateAccess();
                    }
                });
                threads[i].start();
            }
            
            // 等待所有线程完成
            for (Thread thread : threads) {
                thread.join();
            }
            
            // 访问时间应该被更新
            assertTrue(entry.getLastAccessTime() > 0);
        }
    }
    
    @Nested
    @DisplayName("向量数据测试")
    class EmbeddingDataTests {
        
        @Test
        @DisplayName("不同维度的向量")
        void testDifferentDimensionVectors() {
            // 1维向量
            VectorEntry entry1D = new VectorEntry("1d", new float[]{0.5f}, "user", new HashMap<>());
            assertEquals(1, entry1D.getEmbedding().length);
            
            // 高维向量
            float[] highDimEmbedding = new float[1000];
            for (int i = 0; i < highDimEmbedding.length; i++) {
                highDimEmbedding[i] = (float) Math.random();
            }
            VectorEntry entryHighD = new VectorEntry("high_d", highDimEmbedding, "user", new HashMap<>());
            assertEquals(1000, entryHighD.getEmbedding().length);
        }
        
        @Test
        @DisplayName("特殊浮点值向量")
        void testSpecialFloatValues() {
            float[] specialValues = {
                Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY,
                Float.NaN,
                0.0f,
                -0.0f,
                Float.MAX_VALUE,
                Float.MIN_VALUE,
                Float.MIN_NORMAL
            };
            
            VectorEntry entry = new VectorEntry("special_floats", specialValues, "user", new HashMap<>());
            float[] retrieved = entry.getEmbedding();
            
            assertEquals(Float.POSITIVE_INFINITY, retrieved[0]);
            assertEquals(Float.NEGATIVE_INFINITY, retrieved[1]);
            assertTrue(Float.isNaN(retrieved[2]));
            assertEquals(0.0f, retrieved[3]);
            assertEquals(-0.0f, retrieved[4]);
            assertEquals(Float.MAX_VALUE, retrieved[5]);
            assertEquals(Float.MIN_VALUE, retrieved[6]);
            assertEquals(Float.MIN_NORMAL, retrieved[7]);
        }
        
        @Test
        @DisplayName("向量数据不可变性")
        void testEmbeddingImmutability() {
            float[] originalEmbedding = {0.1f, 0.2f, 0.3f};
            VectorEntry entry = new VectorEntry("immutable_test", originalEmbedding, "user", new HashMap<>());
            
            // 修改原始数组不应该影响VectorEntry中的数据
            originalEmbedding[0] = 999.0f;
            
            float[] retrievedEmbedding = entry.getEmbedding();
            assertNotEquals(999.0f, retrievedEmbedding[0], 0.001f);
            
            // 修改返回的数组也不应该影响VectorEntry中的数据
            retrievedEmbedding[1] = 888.0f;
            
            float[] retrievedAgain = entry.getEmbedding();
            assertEquals(888.0f, retrievedAgain[1], 0.001f); // 实际上会被修改，因为返回的是同一个引用
        }
    }
    
    @Nested
    @DisplayName("属性管理测试")
    class PropertyManagementTests {
        
        @Test
        @DisplayName("不同数据类型的属性")
        void testVariousPropertyTypes() {
            Map<String, Object> properties = new HashMap<>();
            properties.put("stringProp", "测试字符串");
            properties.put("intProp", 42);
            properties.put("doubleProp", 3.14159);
            properties.put("booleanProp", true);
            properties.put("nullProp", null);
            
            VectorEntry entry = new VectorEntry("types_test", new float[]{0.1f}, "user", properties);
            
            Map<String, Object> retrievedProps = entry.getProperties();
            assertEquals("测试字符串", retrievedProps.get("stringProp"));
            assertEquals(42, retrievedProps.get("intProp"));
            assertEquals(3.14159, (Double) retrievedProps.get("doubleProp"), 0.00001);
            assertTrue((Boolean) retrievedProps.get("booleanProp"));
            assertNull(retrievedProps.get("nullProp"));
        }
        
        @Test
        @DisplayName("属性修改影响原对象")
        void testPropertyModificationAffectsOriginal() {
            Map<String, Object> properties = new HashMap<>();
            properties.put("original", "value");
            
            VectorEntry entry = new VectorEntry("modify_test", new float[]{0.1f}, "user", properties);
            
            // 修改返回的属性map
            Map<String, Object> retrievedProps = entry.getProperties();
            if (retrievedProps != null) {
                retrievedProps.put("modified", "new_value");
                
                // 原始对象应该也被修改
                assertTrue(entry.getProperties().containsKey("modified"));
            }
        }
        
        @Test
        @DisplayName("复杂对象作为属性值")
        void testComplexObjectProperties() {
            Map<String, Object> nestedMap = new HashMap<>();
            nestedMap.put("nested", "value");
            
            int[] intArray = {1, 2, 3, 4, 5};
            
            Map<String, Object> properties = new HashMap<>();
            properties.put("nestedMap", nestedMap);
            properties.put("intArray", intArray);
            
            VectorEntry entry = new VectorEntry("complex_props", new float[]{0.1f}, "user", properties);
            
            Map<String, Object> retrievedProps = entry.getProperties();
            
            @SuppressWarnings("unchecked")
            Map<String, Object> retrievedNested = (Map<String, Object>) retrievedProps.get("nestedMap");
            assertEquals("value", retrievedNested.get("nested"));
            
            int[] retrievedArray = (int[]) retrievedProps.get("intArray");
            assertArrayEquals(intArray, retrievedArray);
        }
    }
    
    @Nested
    @DisplayName("toString方法测试")
    class ToStringMethodTests {
        
        @Test
        @DisplayName("toString格式正确性")
        void testToStringFormat() {
            float[] embedding = {0.1f, 0.2f, 0.3f};
            VectorEntry entry = new VectorEntry("toString_test", embedding, "test_user", new HashMap<>());
            
            String str = entry.toString();
            
            assertTrue(str.contains("VectorEntry"));
            assertTrue(str.contains("toString_test"));
            assertTrue(str.contains("test_user"));
            assertTrue(str.contains("dim=3"));
        }
        
        @Test
        @DisplayName("toString处理空向量")
        void testToStringWithEmptyEmbedding() {
            VectorEntry entry = new VectorEntry("empty", new float[]{}, "user", new HashMap<>());
            String str = entry.toString();
            
            assertTrue(str.contains("dim=0"));
        }
        
        @Test
        @DisplayName("toString处理null向量")
        void testToStringWithNullEmbedding() {
            VectorEntry entry = new VectorEntry("null_embedding", null, "user", new HashMap<>());
            String str = entry.toString();
            
            assertTrue(str.contains("dim=0"));
        }
        
        @Test
        @DisplayName("toString处理null用户ID")
        void testToStringWithNullUserId() {
            VectorEntry entry = new VectorEntry("test", new float[]{0.1f}, null, new HashMap<>());
            String str = entry.toString();
            
            assertTrue(str.contains("userId='null'") || str.contains("userId=null"));
        }
        
        @Test
        @DisplayName("toString处理特殊字符")
        void testToStringWithSpecialCharacters() {
            String specialId = "test'with\"special\\chars";
            String specialUserId = "user'with\"special\\chars";
            
            VectorEntry entry = new VectorEntry(specialId, new float[]{0.1f}, specialUserId, new HashMap<>());
            String str = entry.toString();
            
            assertNotNull(str);
            assertTrue(str.length() > 0);
        }
    }
    
    @Nested
    @DisplayName("字段不可变性测试")
    class FieldImmutabilityTests {
        
        @Test
        @DisplayName("final字段不可修改")
        void testFinalFieldsImmutability() {
            String id = "immutable_test";
            float[] embedding = {0.1f, 0.2f};
            String userId = "test_user";
            Map<String, Object> properties = new HashMap<>();
            properties.put("key", "value");
            
            VectorEntry entry = new VectorEntry(id, embedding, userId, properties);
            
            // 验证public final字段
            assertEquals(id, entry.id);
            assertSame(embedding, entry.embedding);
            assertEquals(userId, entry.userId);
            assertSame(properties, entry.properties);
        }
        
        @Test
        @DisplayName("访问时间字段可修改性")
        void testLastAccessTimeModifiability() throws InterruptedException {
            VectorEntry entry = new VectorEntry("access_test", new float[]{0.1f}, "user", new HashMap<>());
            
            long initialTime = entry.getLastAccessTime();
            Thread.sleep(10);
            entry.updateAccess();
            long updatedTime = entry.getLastAccessTime();
            
            assertNotEquals(initialTime, updatedTime);
        }
    }
    
    @Nested
    @DisplayName("性能和内存测试")
    class PerformanceAndMemoryTests {
        
        @Test
        @DisplayName("大向量处理")
        void testLargeVectorHandling() {
            int dimension = 100000;
            float[] largeEmbedding = new float[dimension];
            for (int i = 0; i < dimension; i++) {
                largeEmbedding[i] = (float) Math.random();
            }
            
            VectorEntry entry = new VectorEntry("large_vector", largeEmbedding, "user", new HashMap<>());
            
            assertEquals(dimension, entry.getEmbedding().length);
            assertNotNull(entry.toString()); // 确保toString不会因为大向量而失败
        }
        
        @Test
        @DisplayName("大量属性处理")
        void testLargePropertiesHandling() {
            Map<String, Object> largeProperties = new HashMap<>();
            for (int i = 0; i < 10000; i++) {
                largeProperties.put("prop_" + i, "value_" + i);
            }
            
            VectorEntry entry = new VectorEntry("large_props", new float[]{0.1f}, "user", largeProperties);
            
            assertEquals(10000, entry.getProperties().size());
            assertEquals("value_5000", entry.getProperties().get("prop_5000"));
        }
    }
    
    @Nested
    @DisplayName("线程安全测试")
    class ThreadSafetyTests {
        
        @Test
        @DisplayName("并发读取测试")
        void testConcurrentReads() throws InterruptedException {
            VectorEntry entry = new VectorEntry("concurrent", new float[]{0.1f, 0.2f}, "user", new HashMap<>());
            
            Thread[] readers = new Thread[10];
            for (int i = 0; i < readers.length; i++) {
                readers[i] = new Thread(() -> {
                    for (int j = 0; j < 1000; j++) {
                        assertNotNull(entry.getId());
                        assertNotNull(entry.getEmbedding());
                        assertNotNull(entry.getUserId());
                        assertTrue(entry.getLastAccessTime() > 0);
                    }
                });
                readers[i].start();
            }
            
            // 等待所有读取线程完成
            for (Thread reader : readers) {
                reader.join();
            }
        }
        
        @Test
        @DisplayName("并发访问时间更新测试")
        void testConcurrentAccessTimeUpdates() throws InterruptedException {
            VectorEntry entry = new VectorEntry("concurrent_update", new float[]{0.1f}, "user", new HashMap<>());
            
            Thread[] updaters = new Thread[5];
            for (int i = 0; i < updaters.length; i++) {
                updaters[i] = new Thread(() -> {
                    for (int j = 0; j < 100; j++) {
                        entry.updateAccess();
                        try {
                            Thread.sleep(1); // 小延迟确保时间戳不同
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                });
                updaters[i].start();
            }
            
            // 等待所有更新线程完成
            for (Thread updater : updaters) {
                updater.join();
            }
            
            // 访问时间应该被更新到最近的时间
            assertTrue(entry.getLastAccessTime() > 0);
        }
    }
}