package com.mem0.unit.model;

import com.mem0.model.GraphRelationship;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 图关系模型测试
 * 全面测试GraphRelationship类的所有功能和边界条件
 */
@DisplayName("图关系模型测试")
public class GraphRelationshipTest {
    
    @Nested
    @DisplayName("构造和基础功能")
    class ConstructionAndBasicFunctionality {
        
        @Test
        @DisplayName("正常构造图关系")
        void testNormalConstruction() {
            String id = "rel_123";
            String sourceNodeId = "node_001";
            String targetNodeId = "node_002";
            String type = "FRIENDS_WITH";
            Map<String, Object> properties = new HashMap<>();
            properties.put("since", "2023-01-15");
            properties.put("strength", 0.8);
            properties.put("interaction_count", 156);
            
            GraphRelationship relationship = new GraphRelationship(id, sourceNodeId, targetNodeId, type, properties);
            
            assertEquals(id, relationship.getId());
            assertEquals(sourceNodeId, relationship.getSourceNodeId());
            assertEquals(targetNodeId, relationship.getTargetNodeId());
            assertEquals(type, relationship.getType());
            assertNotNull(relationship.getProperties());
            assertTrue(relationship.getCreatedTime() > 0);
        }
        
        @Test
        @DisplayName("使用空属性构造")
        void testConstructionWithEmptyProperties() {
            String id = "empty_props";
            String sourceNodeId = "source_001";
            String targetNodeId = "target_001";
            String type = "CONNECTS";
            Map<String, Object> emptyProperties = new HashMap<>();
            
            GraphRelationship relationship = new GraphRelationship(id, sourceNodeId, targetNodeId, type, emptyProperties);
            
            assertEquals(id, relationship.getId());
            assertEquals(sourceNodeId, relationship.getSourceNodeId());
            assertEquals(targetNodeId, relationship.getTargetNodeId());
            assertEquals(type, relationship.getType());
            assertNotNull(relationship.getProperties());
            assertTrue(relationship.getProperties().isEmpty());
        }
        
        @Test
        @DisplayName("使用null属性构造")
        void testConstructionWithNullProperties() {
            String id = "null_props";
            String sourceNodeId = "source_002";
            String targetNodeId = "target_002";
            String type = "RELATES_TO";
            
            GraphRelationship relationship = new GraphRelationship(id, sourceNodeId, targetNodeId, type, null);
            
            assertEquals(id, relationship.getId());
            assertEquals(sourceNodeId, relationship.getSourceNodeId());
            assertEquals(targetNodeId, relationship.getTargetNodeId());
            assertEquals(type, relationship.getType());
            assertNotNull(relationship.getProperties()); // 应该创建空的ConcurrentHashMap
            assertTrue(relationship.getProperties().isEmpty());
        }
        
        @Test
        @DisplayName("构造时设置创建时间")
        void testCreationTimeSet() {
            long beforeCreation = System.currentTimeMillis();
            
            GraphRelationship relationship = new GraphRelationship(
                "time_test", "source", "target", "TEST", new HashMap<>());
            
            long afterCreation = System.currentTimeMillis();
            
            long createdTime = relationship.getCreatedTime();
            assertTrue(createdTime >= beforeCreation);
            assertTrue(createdTime <= afterCreation);
        }
        
        @Test
        @DisplayName("属性使用ConcurrentHashMap")
        void testPropertiesUseConcurrentHashMap() {
            Map<String, Object> originalProperties = new HashMap<>();
            originalProperties.put("key", "value");
            
            GraphRelationship relationship = new GraphRelationship(
                "concurrent_test", "source", "target", "TEST", originalProperties);
            
            Map<String, Object> relationshipProperties = relationship.getProperties();
            assertTrue(relationshipProperties instanceof ConcurrentHashMap);
        }
    }
    
    @Nested
    @DisplayName("Getter方法测试")
    class GetterMethodTests {
        
        private GraphRelationship createTestRelationship() {
            Map<String, Object> properties = new HashMap<>();
            properties.put("weight", 0.75);
            properties.put("category", "business");
            properties.put("validated", true);
            properties.put("score", 95.5);
            
            return new GraphRelationship("getter_test", "source_node", "target_node", "BUSINESS_RELATION", properties);
        }
        
        @Test
        @DisplayName("getId方法")
        void testGetId() {
            GraphRelationship relationship = createTestRelationship();
            assertEquals("getter_test", relationship.getId());
        }
        
        @Test
        @DisplayName("getSourceNodeId方法")
        void testGetSourceNodeId() {
            GraphRelationship relationship = createTestRelationship();
            assertEquals("source_node", relationship.getSourceNodeId());
        }
        
        @Test
        @DisplayName("getTargetNodeId方法")
        void testGetTargetNodeId() {
            GraphRelationship relationship = createTestRelationship();
            assertEquals("target_node", relationship.getTargetNodeId());
        }
        
        @Test
        @DisplayName("getType方法")
        void testGetType() {
            GraphRelationship relationship = createTestRelationship();
            assertEquals("BUSINESS_RELATION", relationship.getType());
        }
        
        @Test
        @DisplayName("getProperties方法")
        void testGetProperties() {
            GraphRelationship relationship = createTestRelationship();
            Map<String, Object> properties = relationship.getProperties();
            
            assertNotNull(properties);
            assertEquals(0.75, (Double) properties.get("weight"), 0.001);
            assertEquals("business", properties.get("category"));
            assertTrue((Boolean) properties.get("validated"));
            assertEquals(95.5, (Double) properties.get("score"), 0.001);
        }
        
        @Test
        @DisplayName("getCreatedTime方法")
        void testGetCreatedTime() {
            GraphRelationship relationship = createTestRelationship();
            long createdTime = relationship.getCreatedTime();
            
            assertTrue(createdTime > 0);
            assertTrue(createdTime <= System.currentTimeMillis());
        }
    }
    
    @Nested
    @DisplayName("属性管理测试")
    class PropertyManagementTests {
        
        @Test
        @DisplayName("不同数据类型的属性")
        void testVariousPropertyTypes() {
            Map<String, Object> properties = new HashMap<>();
            properties.put("stringProp", "测试关系属性");
            properties.put("intProp", 42);
            properties.put("doubleProp", 3.14159);
            properties.put("booleanProp", true);
            properties.put("nullProp", null);
            
            GraphRelationship relationship = new GraphRelationship(
                "types_test", "source", "target", "TEST", properties);
            
            Map<String, Object> retrievedProps = relationship.getProperties();
            assertEquals("测试关系属性", retrievedProps.get("stringProp"));
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
            
            GraphRelationship relationship = new GraphRelationship(
                "modify_test", "source", "target", "TEST", properties);
            
            // 修改返回的属性map
            Map<String, Object> retrievedProps = relationship.getProperties();
            retrievedProps.put("modified", "new_value");
            
            // 原始对象应该也被修改
            assertTrue(relationship.getProperties().containsKey("modified"));
        }
        
        @Test
        @DisplayName("复杂对象作为属性值")
        void testComplexObjectProperties() {
            Map<String, Object> nestedMap = new HashMap<>();
            nestedMap.put("nested", "value");
            
            String[] stringArray = {"item1", "item2", "item3"};
            
            Map<String, Object> properties = new HashMap<>();
            properties.put("nestedMap", nestedMap);
            properties.put("stringArray", stringArray);
            
            GraphRelationship relationship = new GraphRelationship(
                "complex_props", "source", "target", "TEST", properties);
            
            Map<String, Object> retrievedProps = relationship.getProperties();
            
            @SuppressWarnings("unchecked")
            Map<String, Object> retrievedNested = (Map<String, Object>) retrievedProps.get("nestedMap");
            assertEquals("value", retrievedNested.get("nested"));
            
            String[] retrievedArray = (String[]) retrievedProps.get("stringArray");
            assertArrayEquals(stringArray, retrievedArray);
        }
        
        @Test
        @DisplayName("属性并发修改安全性")
        void testConcurrentPropertyModification() throws InterruptedException {
            Map<String, Object> properties = new HashMap<>();
            properties.put("counter", 0);
            
            GraphRelationship relationship = new GraphRelationship(
                "concurrent_props", "source", "target", "TEST", properties);
            
            // 并发修改属性
            Thread[] threads = new Thread[10];
            for (int i = 0; i < threads.length; i++) {
                final int threadId = i;
                threads[i] = new Thread(() -> {
                    Map<String, Object> props = relationship.getProperties();
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
            Map<String, Object> finalProps = relationship.getProperties();
            assertTrue(finalProps.size() > 1000); // 原始的counter + 10*100个新属性
        }
    }
    
    @Nested
    @DisplayName("equals和hashCode测试")
    class EqualsAndHashCodeTests {
        
        @Test
        @DisplayName("相同ID的关系相等")
        void testRelationshipsWithSameIdAreEqual() {
            String id = "same_id";
            Map<String, Object> properties1 = new HashMap<>();
            properties1.put("prop1", "value1");
            
            Map<String, Object> properties2 = new HashMap<>();
            properties2.put("prop2", "value2");
            
            GraphRelationship rel1 = new GraphRelationship(id, "source1", "target1", "TYPE1", properties1);
            GraphRelationship rel2 = new GraphRelationship(id, "source2", "target2", "TYPE2", properties2);
            
            assertEquals(rel1, rel2); // 应该相等，因为ID相同
            assertEquals(rel1.hashCode(), rel2.hashCode()); // hashCode应该相同
        }
        
        @Test
        @DisplayName("不同ID的关系不相等")
        void testRelationshipsWithDifferentIdAreNotEqual() {
            Map<String, Object> properties = new HashMap<>();
            properties.put("prop", "value");
            
            GraphRelationship rel1 = new GraphRelationship("id1", "source", "target", "TYPE", properties);
            GraphRelationship rel2 = new GraphRelationship("id2", "source", "target", "TYPE", properties);
            
            assertNotEquals(rel1, rel2);
        }
        
        @Test
        @DisplayName("与自身比较相等")
        void testRelationshipEqualsItself() {
            GraphRelationship relationship = new GraphRelationship("self_test", "source", "target", "TEST", new HashMap<>());
            assertEquals(relationship, relationship);
            assertEquals(relationship.hashCode(), relationship.hashCode());
        }
        
        @Test
        @DisplayName("与null和不同类型对象比较")
        void testEqualsWithNullAndDifferentTypes() {
            GraphRelationship relationship = new GraphRelationship("test", "source", "target", "TEST", new HashMap<>());
            
            assertNotEquals(relationship, null);
            assertNotEquals(relationship, "string");
            assertNotEquals(relationship, Integer.valueOf(123));
        }
        
        @Test
        @DisplayName("hashCode一致性")
        void testHashCodeConsistency() {
            GraphRelationship relationship = new GraphRelationship("hash_test", "source", "target", "TEST", new HashMap<>());
            
            int hash1 = relationship.hashCode();
            int hash2 = relationship.hashCode();
            
            assertEquals(hash1, hash2); // 多次调用应该返回相同值
        }
        
        @Test
        @DisplayName("相等对象的hashCode相同")
        void testEqualObjectsHaveSameHashCode() {
            String id = "hash_equal_test";
            GraphRelationship rel1 = new GraphRelationship(id, "source1", "target1", "TYPE1", new HashMap<>());
            GraphRelationship rel2 = new GraphRelationship(id, "source2", "target2", "TYPE2", new HashMap<>());
            
            assertEquals(rel1, rel2);
            assertEquals(rel1.hashCode(), rel2.hashCode());
        }
    }
    
    @Nested
    @DisplayName("toString方法测试")
    class ToStringMethodTests {
        
        @Test
        @DisplayName("toString格式正确性")
        void testToStringFormat() {
            GraphRelationship relationship = new GraphRelationship(
                "toString_test", "source_123", "target_456", "CONNECTS", new HashMap<>());
            String str = relationship.toString();
            
            assertTrue(str.contains("GraphRelationship"));
            assertTrue(str.contains("toString_test"));
            assertTrue(str.contains("source_123"));
            assertTrue(str.contains("target_456"));
            assertTrue(str.contains("CONNECTS"));
        }
        
        @Test
        @DisplayName("toString处理特殊字符")
        void testToStringWithSpecialCharacters() {
            String specialId = "rel'with\"special\\chars";
            String specialSource = "source'with\"special\\chars";
            String specialTarget = "target'with\"special\\chars";
            String specialType = "TYPE'with\"special\\chars";
            
            GraphRelationship relationship = new GraphRelationship(
                specialId, specialSource, specialTarget, specialType, new HashMap<>());
            String str = relationship.toString();
            
            assertNotNull(str);
            assertTrue(str.length() > 0);
        }
        
        @Test
        @DisplayName("toString处理null值")
        void testToStringWithNullValues() {
            GraphRelationship relationship = new GraphRelationship(
                "null_test", null, null, null, new HashMap<>());
            String str = relationship.toString();
            
            assertTrue(str.contains("null"));
        }
    }
    
    @Nested
    @DisplayName("关系方向性测试")
    class RelationshipDirectionalityTests {
        
        @Test
        @DisplayName("关系方向性保持")
        void testRelationshipDirectionality() {
            String sourceId = "person_alice";
            String targetId = "person_bob";
            
            GraphRelationship relationship = new GraphRelationship(
                "friendship", sourceId, targetId, "FRIENDS_WITH", new HashMap<>());
            
            assertEquals(sourceId, relationship.getSourceNodeId());
            assertEquals(targetId, relationship.getTargetNodeId());
            
            // 反向关系应该是不同的关系对象
            GraphRelationship reverseRelationship = new GraphRelationship(
                "friendship_reverse", targetId, sourceId, "FRIENDS_WITH", new HashMap<>());
            
            assertEquals(targetId, reverseRelationship.getSourceNodeId());
            assertEquals(sourceId, reverseRelationship.getTargetNodeId());
            
            assertNotEquals(relationship, reverseRelationship); // 不同的ID，所以不相等
        }
        
        @Test
        @DisplayName("自环关系")
        void testSelfLoopRelationship() {
            String nodeId = "self_referencing_node";
            
            GraphRelationship selfLoop = new GraphRelationship(
                "self_loop", nodeId, nodeId, "SELF_REFERENCE", new HashMap<>());
            
            assertEquals(nodeId, selfLoop.getSourceNodeId());
            assertEquals(nodeId, selfLoop.getTargetNodeId());
        }
    }
    
    @Nested
    @DisplayName("不可变性测试")
    class ImmutabilityTests {
        
        @Test
        @DisplayName("核心字段不可变性")
        void testCoreFieldsImmutability() {
            String originalId = "immutable_id";
            String originalSource = "immutable_source";
            String originalTarget = "immutable_target";
            String originalType = "immutable_type";
            
            GraphRelationship relationship = new GraphRelationship(
                originalId, originalSource, originalTarget, originalType, new HashMap<>());
            
            assertEquals(originalId, relationship.getId());
            assertEquals(originalSource, relationship.getSourceNodeId());
            assertEquals(originalTarget, relationship.getTargetNodeId());
            assertEquals(originalType, relationship.getType());
            
            // 这些字段都是final的，无法修改
            // 这里只是验证getter返回正确的值
        }
        
        @Test
        @DisplayName("属性容器引用不可变性")
        void testPropertiesReferenceImmutability() {
            Map<String, Object> originalProps = new HashMap<>();
            originalProps.put("key", "value");
            
            GraphRelationship relationship = new GraphRelationship(
                "ref_test", "source", "target", "TEST", originalProps);
            
            Map<String, Object> retrievedProps1 = relationship.getProperties();
            Map<String, Object> retrievedProps2 = relationship.getProperties();
            
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
            
            GraphRelationship relationship = new GraphRelationship(
                "large_props", "source", "target", "LARGE_RELATION", largeProperties);
            
            assertEquals(10000, relationship.getProperties().size());
            assertEquals("value_5000", relationship.getProperties().get("prop_5000"));
            
            // toString应该不会因为大量属性而失败
            assertNotNull(relationship.toString());
        }
        
        @Test
        @DisplayName("属性值为大对象")
        void testLargeObjectProperties() {
            Map<String, Object> properties = new HashMap<>();
            
            // 大字符串
            StringBuilder largeString = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                largeString.append("This is a long relationship description ").append(i).append(". ");
            }
            properties.put("description", largeString.toString());
            
            // 大数组
            double[] weights = new double[10000];
            for (int i = 0; i < weights.length; i++) {
                weights[i] = Math.random();
            }
            properties.put("weights", weights);
            
            GraphRelationship relationship = new GraphRelationship(
                "large_objects", "source", "target", "COMPLEX_RELATION", properties);
            
            assertTrue(((String) relationship.getProperties().get("description")).length() > 10000);
            assertEquals(10000, ((double[]) relationship.getProperties().get("weights")).length);
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
            
            GraphRelationship relationship = new GraphRelationship(
                "concurrent", "source", "target", "TEST", properties);
            
            Thread[] readers = new Thread[10];
            for (int i = 0; i < readers.length; i++) {
                readers[i] = new Thread(() -> {
                    for (int j = 0; j < 1000; j++) {
                        assertNotNull(relationship.getId());
                        assertNotNull(relationship.getSourceNodeId());
                        assertNotNull(relationship.getTargetNodeId());
                        assertNotNull(relationship.getType());
                        assertNotNull(relationship.getProperties());
                        assertTrue(relationship.getCreatedTime() > 0);
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
        @DisplayName("并发属性操作测试")
        void testConcurrentPropertyOperations() throws InterruptedException {
            GraphRelationship relationship = new GraphRelationship(
                "concurrent_ops", "source", "target", "TEST", new HashMap<>());
            
            // 一半线程添加属性，一半线程读取属性
            Thread[] writers = new Thread[5];
            Thread[] readers = new Thread[5];
            
            for (int i = 0; i < 5; i++) {
                final int writerId = i;
                writers[i] = new Thread(() -> {
                    Map<String, Object> props = relationship.getProperties();
                    for (int j = 0; j < 100; j++) {
                        props.put("writer_" + writerId + "_key_" + j, "value_" + j);
                    }
                });
                
                readers[i] = new Thread(() -> {
                    for (int j = 0; j < 100; j++) {
                        Map<String, Object> props = relationship.getProperties();
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
            assertTrue(relationship.getProperties().size() >= 500);
        }
    }
    
    @Nested
    @DisplayName("关系类型测试")
    class RelationshipTypeTests {
        
        @Test
        @DisplayName("常见关系类型")
        void testCommonRelationshipTypes() {
            String[] commonTypes = {
                "FRIENDS_WITH", "WORKS_FOR", "LIVES_IN", "OWNS", "LIKES", 
                "FOLLOWS", "MEMBER_OF", "PARENT_OF", "SIMILAR_TO", "RELATED_TO"
            };
            
            for (String type : commonTypes) {
                GraphRelationship relationship = new GraphRelationship(
                    "test_" + type.toLowerCase(), "source", "target", type, new HashMap<>());
                
                assertEquals(type, relationship.getType());
                assertNotNull(relationship.toString());
            }
        }
        
        @Test
        @DisplayName("空和null类型处理")
        void testEmptyAndNullTypes() {
            // 空字符串类型
            GraphRelationship emptyTypeRel = new GraphRelationship(
                "empty_type", "source", "target", "", new HashMap<>());
            assertEquals("", emptyTypeRel.getType());
            
            // null类型
            GraphRelationship nullTypeRel = new GraphRelationship(
                "null_type", "source", "target", null, new HashMap<>());
            assertNull(nullTypeRel.getType());
        }
    }
    
    @Nested
    @DisplayName("时间戳测试")
    class TimestampTests {
        
        @Test
        @DisplayName("创建时间设置")
        void testCreationTimeSet() {
            long beforeCreation = System.currentTimeMillis();
            
            GraphRelationship relationship = new GraphRelationship(
                "time_test", "source", "target", "TEST", new HashMap<>());
            
            long afterCreation = System.currentTimeMillis();
            long createdTime = relationship.getCreatedTime();
            
            assertTrue(createdTime >= beforeCreation);
            assertTrue(createdTime <= afterCreation);
        }
        
        @Test
        @DisplayName("多个关系的时间顺序")
        void testMultipleRelationshipsTimeOrdering() throws InterruptedException {
            GraphRelationship rel1 = new GraphRelationship(
                "time1", "source", "target", "TEST", new HashMap<>());
            long time1 = rel1.getCreatedTime();
            
            Thread.sleep(10); // 确保时间戳不同
            
            GraphRelationship rel2 = new GraphRelationship(
                "time2", "source", "target", "TEST", new HashMap<>());
            long time2 = rel2.getCreatedTime();
            
            assertTrue(time2 > time1);
        }
    }
}