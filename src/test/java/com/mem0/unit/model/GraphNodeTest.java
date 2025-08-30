package com.mem0.unit.model;

import com.mem0.model.GraphNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 图节点模型测试
 * 全面测试GraphNode类的所有功能和边界条件
 */
@DisplayName("图节点模型测试")
public class GraphNodeTest {
    
    @Nested
    @DisplayName("构造和基础功能")
    class ConstructionAndBasicFunctionality {
        
        @Test
        @DisplayName("正常构造图节点")
        void testNormalConstruction() {
            String id = "node_123";
            String type = "Person";
            Map<String, Object> properties = new HashMap<>();
            properties.put("name", "张三");
            properties.put("age", 30);
            properties.put("city", "北京");
            
            GraphNode node = new GraphNode(id, type, properties);
            
            assertEquals(id, node.getId());
            assertEquals(type, node.getType());
            assertNotNull(node.getProperties());
            assertTrue(node.getLastAccessTime() > 0);
        }
        
        @Test
        @DisplayName("使用空属性构造")
        void testConstructionWithEmptyProperties() {
            String id = "empty_props";
            String type = "EmptyNode";
            Map<String, Object> emptyProperties = new HashMap<>();
            
            GraphNode node = new GraphNode(id, type, emptyProperties);
            
            assertEquals(id, node.getId());
            assertEquals(type, node.getType());
            assertNotNull(node.getProperties());
            assertTrue(node.getProperties().isEmpty());
        }
        
        @Test
        @DisplayName("使用null属性构造")
        void testConstructionWithNullProperties() {
            String id = "null_props";
            String type = "NullNode";
            
            GraphNode node = new GraphNode(id, type, null);
            
            assertEquals(id, node.getId());
            assertEquals(type, node.getType());
            assertNotNull(node.getProperties()); // 应该创建空的ConcurrentHashMap
            assertTrue(node.getProperties().isEmpty());
        }
        
        @Test
        @DisplayName("构造时设置初始访问时间")
        void testInitialAccessTimeSet() {
            long beforeCreation = System.currentTimeMillis();
            
            GraphNode node = new GraphNode("time_test", "TestNode", new HashMap<>());
            
            long afterCreation = System.currentTimeMillis();
            
            long accessTime = node.getLastAccessTime();
            assertTrue(accessTime >= beforeCreation);
            assertTrue(accessTime <= afterCreation);
        }
        
        @Test
        @DisplayName("属性使用ConcurrentHashMap")
        void testPropertiesUseConcurrentHashMap() {
            Map<String, Object> originalProperties = new HashMap<>();
            originalProperties.put("key", "value");
            
            GraphNode node = new GraphNode("concurrent_test", "TestNode", originalProperties);
            
            Map<String, Object> nodeProperties = node.getProperties();
            assertTrue(nodeProperties instanceof ConcurrentHashMap);
        }
    }
    
    @Nested
    @DisplayName("Getter方法测试")
    class GetterMethodTests {
        
        private GraphNode createTestNode() {
            Map<String, Object> properties = new HashMap<>();
            properties.put("title", "测试节点");
            properties.put("version", "1.0");
            properties.put("active", true);
            properties.put("score", 95.5);
            
            return new GraphNode("getter_test", "TestType", properties);
        }
        
        @Test
        @DisplayName("getId方法")
        void testGetId() {
            GraphNode node = createTestNode();
            assertEquals("getter_test", node.getId());
        }
        
        @Test
        @DisplayName("getType方法")
        void testGetType() {
            GraphNode node = createTestNode();
            assertEquals("TestType", node.getType());
        }
        
        @Test
        @DisplayName("getProperties方法")
        void testGetProperties() {
            GraphNode node = createTestNode();
            Map<String, Object> properties = node.getProperties();
            
            assertNotNull(properties);
            assertEquals("测试节点", properties.get("title"));
            assertEquals("1.0", properties.get("version"));
            assertTrue((Boolean) properties.get("active"));
            assertEquals(95.5, (Double) properties.get("score"), 0.001);
        }
        
        @Test
        @DisplayName("getLastAccessTime方法")
        void testGetLastAccessTime() {
            GraphNode node = createTestNode();
            long accessTime = node.getLastAccessTime();
            
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
            GraphNode node = new GraphNode("update_test", "TestNode", new HashMap<>());
            long initialTime = node.getLastAccessTime();
            
            // 等待一小段时间确保时间戳不同
            Thread.sleep(10);
            
            node.updateAccess();
            long updatedTime = node.getLastAccessTime();
            
            assertTrue(updatedTime > initialTime);
        }
        
        @Test
        @DisplayName("多次updateAccess更新时间")
        void testMultipleUpdateAccessCalls() throws InterruptedException {
            GraphNode node = new GraphNode("multi_update", "TestNode", new HashMap<>());
            
            long time1 = node.getLastAccessTime();
            Thread.sleep(5);
            
            node.updateAccess();
            long time2 = node.getLastAccessTime();
            Thread.sleep(5);
            
            node.updateAccess();
            long time3 = node.getLastAccessTime();
            
            assertTrue(time2 > time1);
            assertTrue(time3 > time2);
        }
        
        @Test
        @DisplayName("并发updateAccess测试")
        void testConcurrentUpdateAccess() throws InterruptedException {
            GraphNode node = new GraphNode("concurrent_update", "TestNode", new HashMap<>());
            
            // 并发调用updateAccess
            Thread[] threads = new Thread[10];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < 100; j++) {
                        node.updateAccess();
                    }
                });
                threads[i].start();
            }
            
            // 等待所有线程完成
            for (Thread thread : threads) {
                thread.join();
            }
            
            // 访问时间应该被更新
            assertTrue(node.getLastAccessTime() > 0);
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
            
            GraphNode node = new GraphNode("types_test", "TestNode", properties);
            
            Map<String, Object> retrievedProps = node.getProperties();
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
            
            GraphNode node = new GraphNode("modify_test", "TestNode", properties);
            
            // 修改返回的属性map
            Map<String, Object> retrievedProps = node.getProperties();
            retrievedProps.put("modified", "new_value");
            
            // 原始对象应该也被修改
            assertTrue(node.getProperties().containsKey("modified"));
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
            
            GraphNode node = new GraphNode("complex_props", "TestNode", properties);
            
            Map<String, Object> retrievedProps = node.getProperties();
            
            @SuppressWarnings("unchecked")
            Map<String, Object> retrievedNested = (Map<String, Object>) retrievedProps.get("nestedMap");
            assertEquals("value", retrievedNested.get("nested"));
            
            int[] retrievedArray = (int[]) retrievedProps.get("intArray");
            assertArrayEquals(intArray, retrievedArray);
        }
        
        @Test
        @DisplayName("属性并发修改安全性")
        void testConcurrentPropertyModification() throws InterruptedException {
            Map<String, Object> properties = new HashMap<>();
            properties.put("counter", 0);
            
            GraphNode node = new GraphNode("concurrent_props", "TestNode", properties);
            
            // 并发修改属性
            Thread[] threads = new Thread[10];
            for (int i = 0; i < threads.length; i++) {
                final int threadId = i;
                threads[i] = new Thread(() -> {
                    Map<String, Object> props = node.getProperties();
                    for (int j = 0; j < 100; j++) {
                        props.put("thread_" + threadId + "_prop_" + j, "value_" + j);
                    }
                });
                threads[i].start();
            }
            
            // 等待所有线程完成
            for (Thread thread : threads) {
                thread.join();
            }
            
            // 验证所有属性都被添加
            Map<String, Object> finalProps = node.getProperties();
            assertTrue(finalProps.size() > 1000); // 原始的counter + 10*100个新属性
        }
    }
    
    @Nested
    @DisplayName("equals和hashCode测试")
    class EqualsAndHashCodeTests {
        
        @Test
        @DisplayName("相同ID的节点相等")
        void testNodesWithSameIdAreEqual() {
            String id = "same_id";
            Map<String, Object> properties1 = new HashMap<>();
            properties1.put("prop1", "value1");
            
            Map<String, Object> properties2 = new HashMap<>();
            properties2.put("prop2", "value2");
            
            GraphNode node1 = new GraphNode(id, "Type1", properties1);
            GraphNode node2 = new GraphNode(id, "Type2", properties2);
            
            assertEquals(node1, node2); // 应该相等，因为ID相同
            assertEquals(node1.hashCode(), node2.hashCode()); // hashCode应该相同
        }
        
        @Test
        @DisplayName("不同ID的节点不相等")
        void testNodesWithDifferentIdAreNotEqual() {
            Map<String, Object> properties = new HashMap<>();
            properties.put("prop", "value");
            
            GraphNode node1 = new GraphNode("id1", "SameType", properties);
            GraphNode node2 = new GraphNode("id2", "SameType", properties);
            
            assertNotEquals(node1, node2);
        }
        
        @Test
        @DisplayName("与自身比较相等")
        void testNodeEqualsItself() {
            GraphNode node = new GraphNode("self_test", "TestNode", new HashMap<>());
            assertEquals(node, node);
            assertEquals(node.hashCode(), node.hashCode());
        }
        
        @Test
        @DisplayName("与null和不同类型对象比较")
        void testEqualsWithNullAndDifferentTypes() {
            GraphNode node = new GraphNode("test", "TestNode", new HashMap<>());
            
            assertNotEquals(node, null);
            assertNotEquals(node, "string");
            assertNotEquals(node, Integer.valueOf(123));
        }
        
        @Test
        @DisplayName("hashCode一致性")
        void testHashCodeConsistency() {
            GraphNode node = new GraphNode("hash_test", "TestNode", new HashMap<>());
            
            int hash1 = node.hashCode();
            int hash2 = node.hashCode();
            
            assertEquals(hash1, hash2); // 多次调用应该返回相同值
        }
        
        @Test
        @DisplayName("相等对象的hashCode相同")
        void testEqualObjectsHaveSameHashCode() {
            String id = "hash_equal_test";
            GraphNode node1 = new GraphNode(id, "Type1", new HashMap<>());
            GraphNode node2 = new GraphNode(id, "Type2", new HashMap<>());
            
            assertEquals(node1, node2);
            assertEquals(node1.hashCode(), node2.hashCode());
        }
    }
    
    @Nested
    @DisplayName("toString方法测试")
    class ToStringMethodTests {
        
        @Test
        @DisplayName("toString格式正确性")
        void testToStringFormat() {
            GraphNode node = new GraphNode("toString_test", "TestType", new HashMap<>());
            String str = node.toString();
            
            assertTrue(str.contains("GraphNode"));
            assertTrue(str.contains("toString_test"));
            assertTrue(str.contains("TestType"));
        }
        
        @Test
        @DisplayName("toString处理特殊字符")
        void testToStringWithSpecialCharacters() {
            String specialId = "test'with\"special\\chars";
            String specialType = "Type'with\"special\\chars";
            
            GraphNode node = new GraphNode(specialId, specialType, new HashMap<>());
            String str = node.toString();
            
            assertNotNull(str);
            assertTrue(str.length() > 0);
        }
        
        @Test
        @DisplayName("toString处理null类型")
        void testToStringWithNullType() {
            GraphNode node = new GraphNode("null_type_test", null, new HashMap<>());
            String str = node.toString();
            
            assertTrue(str.contains("null") || str.contains("type='null'"));
        }
    }
    
    @Nested
    @DisplayName("不可变性测试")
    class ImmutabilityTests {
        
        @Test
        @DisplayName("ID字段不可变性")
        void testIdImmutability() {
            String originalId = "immutable_id";
            GraphNode node = new GraphNode(originalId, "TestType", new HashMap<>());
            
            assertEquals(originalId, node.getId());
            
            // ID字段是final的，无法修改
            // 这里只是验证getter返回正确的值
        }
        
        @Test
        @DisplayName("类型字段不可变性")
        void testTypeImmutability() {
            String originalType = "ImmutableType";
            GraphNode node = new GraphNode("test", originalType, new HashMap<>());
            
            assertEquals(originalType, node.getType());
            
            // 类型字段是final的，无法修改
            // 这里只是验证getter返回正确的值
        }
        
        @Test
        @DisplayName("属性容器引用不可变性")
        void testPropertiesReferenceImmutability() {
            Map<String, Object> originalProps = new HashMap<>();
            originalProps.put("key", "value");
            
            GraphNode node = new GraphNode("ref_test", "TestType", originalProps);
            
            Map<String, Object> retrievedProps1 = node.getProperties();
            Map<String, Object> retrievedProps2 = node.getProperties();
            
            // 应该返回同一个对象引用
            assertSame(retrievedProps1, retrievedProps2);
        }
    }
    
    @Nested
    @DisplayName("性能和内存测试")
    class PerformanceAndMemoryTests {
        
        @Test
        @DisplayName("大量属性处理")
        void testLargePropertiesHandling() {
            Map<String, Object> largeProperties = new HashMap<>();
            for (int i = 0; i < 10000; i++) {
                largeProperties.put("prop_" + i, "value_" + i);
            }
            
            GraphNode node = new GraphNode("large_props", "LargeNode", largeProperties);
            
            assertEquals(10000, node.getProperties().size());
            assertEquals("value_5000", node.getProperties().get("prop_5000"));
            
            // toString应该不会因为大量属性而失败
            assertNotNull(node.toString());
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
            
            GraphNode node = new GraphNode("large_objects", "LargeObjectNode", properties);
            
            assertTrue(((String) node.getProperties().get("largeString")).length() > 10000);
            assertEquals(10000, ((int[]) node.getProperties().get("largeArray")).length);
        }
    }
    
    @Nested
    @DisplayName("线程安全测试")
    class ThreadSafetyTests {
        
        @Test
        @DisplayName("并发读取测试")
        void testConcurrentReads() throws InterruptedException {
            Map<String, Object> properties = new HashMap<>();
            properties.put("shared", "value");
            
            GraphNode node = new GraphNode("concurrent", "TestNode", properties);
            
            Thread[] readers = new Thread[10];
            for (int i = 0; i < readers.length; i++) {
                readers[i] = new Thread(() -> {
                    for (int j = 0; j < 1000; j++) {
                        assertNotNull(node.getId());
                        assertNotNull(node.getType());
                        assertNotNull(node.getProperties());
                        assertTrue(node.getLastAccessTime() > 0);
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
            GraphNode node = new GraphNode("concurrent_update", "TestNode", new HashMap<>());
            
            Thread[] updaters = new Thread[5];
            for (int i = 0; i < updaters.length; i++) {
                updaters[i] = new Thread(() -> {
                    for (int j = 0; j < 100; j++) {
                        node.updateAccess();
                        try {
                            Thread.sleep(1);
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
            assertTrue(node.getLastAccessTime() > 0);
        }
        
        @Test
        @DisplayName("并发属性操作测试")
        void testConcurrentPropertyOperations() throws InterruptedException {
            GraphNode node = new GraphNode("concurrent_ops", "TestNode", new HashMap<>());
            
            // 一半线程添加属性，一半线程读取属性
            Thread[] writers = new Thread[5];
            Thread[] readers = new Thread[5];
            
            for (int i = 0; i < 5; i++) {
                final int writerId = i;
                writers[i] = new Thread(() -> {
                    Map<String, Object> props = node.getProperties();
                    for (int j = 0; j < 100; j++) {
                        props.put("writer_" + writerId + "_key_" + j, "value_" + j);
                    }
                });
                
                readers[i] = new Thread(() -> {
                    for (int j = 0; j < 100; j++) {
                        Map<String, Object> props = node.getProperties();
                        assertNotNull(props);
                        // 尝试读取一些可能存在的键
                        props.get("writer_0_key_50");
                    }
                });
                
                writers[i].start();
                readers[i].start();
            }
            
            // 等待所有线程完成
            for (int i = 0; i < 5; i++) {
                writers[i].join();
                readers[i].join();
            }
            
            // 验证所有写入操作都成功
            assertTrue(node.getProperties().size() >= 500);
        }
    }
}