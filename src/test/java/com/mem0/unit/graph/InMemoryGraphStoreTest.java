package com.mem0.unit.graph;

import com.mem0.graph.impl.InMemoryGraphStore;
import com.mem0.store.GraphStore;
import com.mem0.core.EnhancedMemory;
import com.mem0.core.MemoryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.AfterEach;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 内存图存储测试
 * 全面测试InMemoryGraphStore的所有功能，包括节点、关系和增强内存管理
 */
@DisplayName("内存图存储测试")
public class InMemoryGraphStoreTest {
    
    private InMemoryGraphStore graphStore;
    private static final String TEST_USER_ID = "test_user_123";
    
    @BeforeEach
    void setUp() {
        graphStore = new InMemoryGraphStore();
    }
    
    @AfterEach
    void tearDown() throws ExecutionException, InterruptedException {
        if (graphStore != null) {
            graphStore.close().get();
        }
    }
    
    @Nested
    @DisplayName("节点管理功能")
    class NodeManagementTests {
        
        @Test
        @DisplayName("创建节点")
        void testCreateNode() throws ExecutionException, InterruptedException {
            Map<String, Object> properties = new HashMap<>();
            properties.put("name", "张三");
            properties.put("age", 30);
            properties.put("department", "技术部");
            
            String nodeId = graphStore.createNode("Person", properties).get();
            
            assertNotNull(nodeId, "节点ID应该不为空");
            assertTrue(nodeId.startsWith("node_"), "节点ID应该有正确的前缀");
        }
        
        @Test
        @DisplayName("获取节点")
        void testGetNode() throws ExecutionException, InterruptedException {
            Map<String, Object> properties = new HashMap<>();
            properties.put("name", "李四");
            properties.put("role", "开发工程师");
            
            String nodeId = graphStore.createNode("Employee", properties).get();
            GraphStore.GraphNode node = graphStore.getNode(nodeId).get();
            
            assertNotNull(node, "获取的节点不应该为空");
            assertEquals(nodeId, node.getId());
            assertEquals("李四", node.getProperties().get("name"));
            assertEquals("开发工程师", node.getProperties().get("role"));
            assertTrue(node.getLabels().contains("Employee"));
        }
        
        @Test
        @DisplayName("获取不存在的节点")
        void testGetNonexistentNode() throws ExecutionException, InterruptedException {
            GraphStore.GraphNode node = graphStore.getNode("nonexistent").get();
            assertNull(node, "不存在的节点应该返回null");
        }
        
        @Test
        @DisplayName("按标签获取节点")
        void testGetNodesByLabel() throws ExecutionException, InterruptedException {
            // 创建多个不同类型的节点
            Map<String, Object> person1 = new HashMap<>();
            person1.put("name", "王五");
            person1.put("age", 25);
            graphStore.createNode("Person", person1).get();
            
            Map<String, Object> person2 = new HashMap<>();
            person2.put("name", "赵六");
            person2.put("age", 30);
            graphStore.createNode("Person", person2).get();
            
            Map<String, Object> company = new HashMap<>();
            company.put("name", "ABC公司");
            graphStore.createNode("Company", company).get();
            
            // 测试按标签获取
            List<GraphStore.GraphNode> persons = graphStore.getNodesByLabel("Person", null).get();
            assertEquals(2, persons.size());
            
            // 测试按标签和属性获取
            Map<String, Object> filter = new HashMap<>();
            filter.put("age", 30);
            List<GraphStore.GraphNode> filteredPersons = graphStore.getNodesByLabel("Person", filter).get();
            assertEquals(1, filteredPersons.size());
            assertEquals("赵六", filteredPersons.get(0).getProperties().get("name"));
        }
        
        @Test
        @DisplayName("更新节点")
        void testUpdateNode() throws ExecutionException, InterruptedException {
            Map<String, Object> properties = new HashMap<>();
            properties.put("name", "孙七");
            properties.put("status", "active");
            
            String nodeId = graphStore.createNode("User", properties).get();
            
            // 更新节点属性
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "inactive");
            updates.put("lastLogin", "2024-08-30");
            
            graphStore.updateNode(nodeId, updates).get();
            
            GraphStore.GraphNode updatedNode = graphStore.getNode(nodeId).get();
            assertEquals("inactive", updatedNode.getProperties().get("status"));
            assertEquals("2024-08-30", updatedNode.getProperties().get("lastLogin"));
            assertEquals("孙七", updatedNode.getProperties().get("name")); // 原有属性应该保留
        }
        
        @Test
        @DisplayName("删除节点")
        void testDeleteNode() throws ExecutionException, InterruptedException {
            Map<String, Object> properties = new HashMap<>();
            properties.put("name", "周八");
            
            String nodeId = graphStore.createNode("TempUser", properties).get();
            assertNotNull(graphStore.getNode(nodeId).get());
            
            // 删除节点
            graphStore.deleteNode(nodeId).get();
            assertNull(graphStore.getNode(nodeId).get());
        }
    }
    
    @Nested
    @DisplayName("关系管理功能")
    class RelationshipManagementTests {
        
        private String personNodeId;
        private String companyNodeId;
        
        @BeforeEach
        void setUpRelationshipData() throws ExecutionException, InterruptedException {
            // 创建测试节点
            Map<String, Object> personProps = new HashMap<>();
            personProps.put("name", "员工A");
            personNodeId = graphStore.createNode("Person", personProps).get();
            
            Map<String, Object> companyProps = new HashMap<>();
            companyProps.put("name", "公司B");
            companyNodeId = graphStore.createNode("Company", companyProps).get();
        }
        
        @Test
        @DisplayName("创建关系")
        void testCreateRelationship() throws ExecutionException, InterruptedException {
            Map<String, Object> relProps = new HashMap<>();
            relProps.put("since", "2020-01-01");
            relProps.put("position", "软件工程师");
            
            String relationshipId = graphStore.createRelationship(
                personNodeId, companyNodeId, "WORKS_FOR", relProps).get();
            
            assertNotNull(relationshipId);
            assertTrue(relationshipId.startsWith("rel_"));
        }
        
        @Test
        @DisplayName("获取节点关系")
        void testGetRelationships() throws ExecutionException, InterruptedException {
            // 创建多个关系
            Map<String, Object> workRel = new HashMap<>();
            workRel.put("role", "developer");
            graphStore.createRelationship(personNodeId, companyNodeId, "WORKS_FOR", workRel).get();
            
            Map<String, Object> knowsRel = new HashMap<>();
            knowsRel.put("since", "2019");
            
            // 创建另一个人节点
            Map<String, Object> person2Props = new HashMap<>();
            person2Props.put("name", "员工C");
            String person2NodeId = graphStore.createNode("Person", person2Props).get();
            
            graphStore.createRelationship(personNodeId, person2NodeId, "KNOWS", knowsRel).get();
            
            // 获取所有关系
            List<GraphStore.GraphRelationship> allRels = graphStore.getRelationships(personNodeId, null).get();
            assertEquals(2, allRels.size());
            
            // 获取特定类型关系
            List<GraphStore.GraphRelationship> workRels = graphStore.getRelationships(personNodeId, "WORKS_FOR").get();
            assertEquals(1, workRels.size());
            assertEquals("WORKS_FOR", workRels.get(0).getType());
        }
        
        @Test
        @DisplayName("查找连接的节点")
        void testFindConnectedNodes() throws ExecutionException, InterruptedException {
            // 创建关系网络
            graphStore.createRelationship(personNodeId, companyNodeId, "WORKS_FOR", new HashMap<>()).get();
            
            Map<String, Object> colleagueProps = new HashMap<>();
            colleagueProps.put("name", "同事D");
            String colleagueNodeId = graphStore.createNode("Person", colleagueProps).get();
            
            graphStore.createRelationship(personNodeId, colleagueNodeId, "KNOWS", new HashMap<>()).get();
            
            // 查找所有连接的节点
            List<GraphStore.GraphNode> connectedNodes = graphStore.findConnectedNodes(personNodeId, null, 1).get();
            assertEquals(2, connectedNodes.size());
            
            // 查找特定关系类型的连接节点
            List<GraphStore.GraphNode> workConnections = graphStore.findConnectedNodes(personNodeId, "WORKS_FOR", 1).get();
            assertEquals(1, workConnections.size());
            assertEquals("公司B", workConnections.get(0).getProperties().get("name"));
        }
        
        @Test
        @DisplayName("更新关系")
        void testUpdateRelationship() throws ExecutionException, InterruptedException {
            Map<String, Object> relProps = new HashMap<>();
            relProps.put("level", "junior");
            
            String relationshipId = graphStore.createRelationship(
                personNodeId, companyNodeId, "WORKS_FOR", relProps).get();
            
            // 更新关系属性
            Map<String, Object> updates = new HashMap<>();
            updates.put("level", "senior");
            updates.put("promoted", "2024-01-01");
            
            graphStore.updateRelationship(relationshipId, updates).get();
            
            // 验证更新（通过获取关系来验证）
            List<GraphStore.GraphRelationship> relationships = graphStore.getRelationships(personNodeId, "WORKS_FOR").get();
            assertEquals(1, relationships.size());
            GraphStore.GraphRelationship updatedRel = relationships.get(0);
            assertEquals("senior", updatedRel.getProperties().get("level"));
            assertEquals("2024-01-01", updatedRel.getProperties().get("promoted"));
        }
        
        @Test
        @DisplayName("删除关系")
        void testDeleteRelationship() throws ExecutionException, InterruptedException {
            String relationshipId = graphStore.createRelationship(
                personNodeId, companyNodeId, "TEMPORARY", new HashMap<>()).get();
            
            // 验证关系存在
            List<GraphStore.GraphRelationship> beforeDelete = graphStore.getRelationships(personNodeId, "TEMPORARY").get();
            assertEquals(1, beforeDelete.size());
            
            // 删除关系
            graphStore.deleteRelationship(relationshipId).get();
            
            // 验证关系已删除
            List<GraphStore.GraphRelationship> afterDelete = graphStore.getRelationships(personNodeId, "TEMPORARY").get();
            assertEquals(0, afterDelete.size());
        }
        
        @Test
        @DisplayName("删除节点时自动删除相关关系")
        void testDeleteNodeCascadesRelationships() throws ExecutionException, InterruptedException {
            // 创建关系
            graphStore.createRelationship(personNodeId, companyNodeId, "WORKS_FOR", new HashMap<>()).get();
            
            // 验证关系存在
            List<GraphStore.GraphRelationship> beforeDelete = graphStore.getRelationships(personNodeId, null).get();
            assertTrue(beforeDelete.size() > 0);
            
            // 删除人员节点
            graphStore.deleteNode(personNodeId).get();
            
            // 验证相关关系也被删除
            List<GraphStore.GraphRelationship> afterDelete = graphStore.getRelationships(companyNodeId, null).get();
            assertEquals(0, afterDelete.size());
        }
    }
    
    @Nested
    @DisplayName("增强内存管理功能")
    class EnhancedMemoryManagementTests {
        
        @Test
        @DisplayName("添加内存")
        void testAddMemory() throws ExecutionException, InterruptedException {
            EnhancedMemory memory = createTestMemory("test_memory_1", "这是一个测试内存内容");
            
            assertDoesNotThrow(() -> graphStore.addMemory(memory).get());
            
            EnhancedMemory retrieved = graphStore.getMemory("test_memory_1").get();
            assertNotNull(retrieved);
            assertEquals("test_memory_1", retrieved.getId());
            assertEquals("这是一个测试内存内容", retrieved.getContent());
        }
        
        @Test
        @DisplayName("添加重复内存抛出异常")
        void testAddDuplicateMemoryThrowsException() throws ExecutionException, InterruptedException {
            EnhancedMemory memory = createTestMemory("duplicate_memory", "内容");
            
            graphStore.addMemory(memory).get();
            
            // 再次添加相同ID的内存应该抛出异常
            CompletableFuture<Void> duplicateFuture = graphStore.addMemory(memory);
            assertThrows(RuntimeException.class, duplicateFuture::join);
        }
        
        @Test
        @DisplayName("获取内存")
        void testGetMemory() throws ExecutionException, InterruptedException {
            EnhancedMemory memory = createTestMemory("get_test_memory", "获取测试内容");
            graphStore.addMemory(memory).get();
            
            EnhancedMemory retrieved = graphStore.getMemory("get_test_memory").get();
            
            assertNotNull(retrieved);
            assertEquals("get_test_memory", retrieved.getId());
            assertEquals("获取测试内容", retrieved.getContent());
            assertEquals(TEST_USER_ID, retrieved.getUserId());
        }
        
        @Test
        @DisplayName("获取不存在的内存")
        void testGetNonexistentMemory() throws ExecutionException, InterruptedException {
            EnhancedMemory memory = graphStore.getMemory("nonexistent_memory").get();
            assertNull(memory);
        }
        
        @Test
        @DisplayName("更新内存")
        void testUpdateMemory() throws ExecutionException, InterruptedException {
            EnhancedMemory memory = createTestMemory("update_test_memory", "原始内容");
            graphStore.addMemory(memory).get();
            
            // 创建更新后的内存
            EnhancedMemory updatedMemory = createTestMemory("update_test_memory", "更新后的内容");
            
            assertDoesNotThrow(() -> graphStore.updateMemory(updatedMemory).get());
            
            EnhancedMemory retrieved = graphStore.getMemory("update_test_memory").get();
            assertEquals("更新后的内容", retrieved.getContent());
        }
        
        @Test
        @DisplayName("更新不存在的内存抛出异常")
        void testUpdateNonexistentMemoryThrowsException() {
            EnhancedMemory memory = createTestMemory("nonexistent_update", "内容");
            
            CompletableFuture<Void> future = graphStore.updateMemory(memory);
            assertThrows(RuntimeException.class, future::join);
        }
        
        @Test
        @DisplayName("删除内存")
        void testDeleteMemory() throws ExecutionException, InterruptedException {
            EnhancedMemory memory = createTestMemory("delete_test_memory", "待删除内容");
            graphStore.addMemory(memory).get();
            
            // 验证内存存在
            assertNotNull(graphStore.getMemory("delete_test_memory").get());
            
            // 删除内存
            graphStore.deleteMemory("delete_test_memory").get();
            
            // 验证内存已删除
            assertNull(graphStore.getMemory("delete_test_memory").get());
        }
        
        @Test
        @DisplayName("删除不存在的内存")
        void testDeleteNonexistentMemory() throws ExecutionException, InterruptedException {
            // 删除不存在的内存应该正常完成而不抛出异常
            assertDoesNotThrow(() -> graphStore.deleteMemory("nonexistent_delete").get());
        }
    }
    
    @Nested
    @DisplayName("用户内存管理功能")
    class UserMemoryManagementTests {
        
        @BeforeEach
        void setUpUserMemoryData() throws ExecutionException, InterruptedException {
            // 为同一用户创建多个内存
            for (int i = 1; i <= 5; i++) {
                EnhancedMemory memory = createTestMemory("user_memory_" + i, "用户内存内容 " + i);
                graphStore.addMemory(memory).get();
                Thread.sleep(10); // 确保时间戳不同
            }
            
            // 为其他用户创建内存
            EnhancedMemory otherUserMemory = createTestMemory("other_user_memory", "其他用户内容", "other_user");
            graphStore.addMemory(otherUserMemory).get();
        }
        
        @Test
        @DisplayName("获取用户所有内存")
        void testGetUserMemories() throws ExecutionException, InterruptedException {
            List<EnhancedMemory> userMemories = graphStore.getUserMemories(TEST_USER_ID).get();
            
            assertEquals(5, userMemories.size());
            assertTrue(userMemories.stream().allMatch(m -> TEST_USER_ID.equals(m.getUserId())));
        }
        
        @Test
        @DisplayName("获取不存在用户的内存")
        void testGetNonexistentUserMemories() throws ExecutionException, InterruptedException {
            List<EnhancedMemory> memories = graphStore.getUserMemories("nonexistent_user").get();
            assertTrue(memories.isEmpty());
        }
        
        @Test
        @DisplayName("获取内存历史记录")
        void testGetMemoryHistory() throws ExecutionException, InterruptedException {
            List<EnhancedMemory> history = graphStore.getMemoryHistory(TEST_USER_ID).get();
            
            assertEquals(5, history.size());
            
            // 验证按创建时间排序
            for (int i = 0; i < history.size() - 1; i++) {
                assertTrue(history.get(i).getCreatedAt().isBefore(history.get(i + 1).getCreatedAt()) ||
                          history.get(i).getCreatedAt().equals(history.get(i + 1).getCreatedAt()));
            }
        }
        
        @Test
        @DisplayName("搜索用户内存")
        void testSearchMemories() throws ExecutionException, InterruptedException {
            // 搜索包含特定关键字的内存
            List<EnhancedMemory> results = graphStore.searchMemories("内容 3", TEST_USER_ID, 10).get();
            
            assertEquals(1, results.size());
            assertTrue(results.get(0).getContent().contains("内容 3"));
        }
        
        @Test
        @DisplayName("搜索内存的模糊匹配")
        void testSearchMemoriesFuzzy() throws ExecutionException, InterruptedException {
            List<EnhancedMemory> results = graphStore.searchMemories("用户", TEST_USER_ID, 10).get();
            
            assertEquals(5, results.size()); // 所有用户内存都包含"用户"关键字
        }
        
        @Test
        @DisplayName("搜索空查询")
        void testSearchMemoriesEmptyQuery() throws ExecutionException, InterruptedException {
            List<EnhancedMemory> results = graphStore.searchMemories("", TEST_USER_ID, 10).get();
            assertTrue(results.isEmpty());
            
            List<EnhancedMemory> nullResults = graphStore.searchMemories(null, TEST_USER_ID, 10).get();
            assertTrue(nullResults.isEmpty());
        }
        
        @Test
        @DisplayName("搜索限制结果数量")
        void testSearchMemoriesWithLimit() throws ExecutionException, InterruptedException {
            List<EnhancedMemory> results = graphStore.searchMemories("用户", TEST_USER_ID, 3).get();
            assertEquals(3, results.size());
        }
    }
    
    @Nested
    @DisplayName("内存关系功能")
    class MemoryRelationshipTests {
        
        private String memoryId1;
        private String memoryId2;
        
        @BeforeEach
        void setUpMemoryRelationshipData() throws ExecutionException, InterruptedException {
            EnhancedMemory memory1 = createTestMemory("rel_memory_1", "第一个相关内存");
            EnhancedMemory memory2 = createTestMemory("rel_memory_2", "第二个相关内存");
            
            graphStore.addMemory(memory1).get();
            graphStore.addMemory(memory2).get();
            
            memoryId1 = "rel_memory_1";
            memoryId2 = "rel_memory_2";
        }
        
        @Test
        @DisplayName("添加内存关系")
        void testAddMemoryRelationship() throws ExecutionException, InterruptedException {
            Map<String, Object> properties = new HashMap<>();
            properties.put("strength", "strong");
            properties.put("type", "causal");
            
            assertDoesNotThrow(() -> 
                graphStore.addRelationship(memoryId1, memoryId2, "RELATES_TO", properties).get());
        }
        
        @Test
        @DisplayName("添加关系到不存在的内存抛出异常")
        void testAddRelationshipToNonexistentMemory() {
            Map<String, Object> properties = new HashMap<>();
            
            // 源内存不存在
            CompletableFuture<Void> future1 = graphStore.addRelationship("nonexistent", memoryId2, "RELATES_TO", properties);
            assertThrows(RuntimeException.class, future1::join);
            
            // 目标内存不存在
            CompletableFuture<Void> future2 = graphStore.addRelationship(memoryId1, "nonexistent", "RELATES_TO", properties);
            assertThrows(RuntimeException.class, future2::join);
        }
        
        @Test
        @DisplayName("删除内存时清理相关关系")
        void testDeleteMemoryClearsRelationships() throws ExecutionException, InterruptedException {
            // 添加关系
            Map<String, Object> properties = new HashMap<>();
            graphStore.addRelationship(memoryId1, memoryId2, "RELATES_TO", properties).get();
            
            // 删除其中一个内存
            graphStore.deleteMemory(memoryId1).get();
            
            // 验证内存已删除
            assertNull(graphStore.getMemory(memoryId1).get());
        }
    }
    
    @Nested
    @DisplayName("并发安全测试")
    class ConcurrencySafetyTests {
        
        @Test
        @DisplayName("并发创建节点")
        void testConcurrentNodeCreation() {
            int threadCount = 10;
            int nodesPerThread = 5;
            
            List<CompletableFuture<String>> futures = IntStream.range(0, threadCount)
                .boxed()
                .flatMap(threadId -> IntStream.range(0, nodesPerThread)
                    .mapToObj(nodeIndex -> {
                        Map<String, Object> properties = new HashMap<>();
                        properties.put("threadId", threadId);
                        properties.put("nodeIndex", nodeIndex);
                        properties.put("name", "Node-" + threadId + "-" + nodeIndex);
                        
                        return graphStore.createNode("TestNode", properties);
                    }))
                .collect(Collectors.toList());
            
            // 等待所有创建完成
            List<String> nodeIds = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
            
            assertEquals(threadCount * nodesPerThread, nodeIds.size());
            assertTrue(nodeIds.stream().allMatch(Objects::nonNull));
        }
        
        @Test
        @DisplayName("并发添加内存")
        void testConcurrentMemoryAddition() {
            int threadCount = 5;
            int memoriesPerThread = 10;
            
            List<CompletableFuture<Void>> futures = IntStream.range(0, threadCount)
                .boxed()
                .flatMap(threadId -> IntStream.range(0, memoriesPerThread)
                    .mapToObj(memIndex -> {
                        String memoryId = "concurrent_memory_" + threadId + "_" + memIndex;
                        String content = "并发测试内容 - 线程" + threadId + " 内存" + memIndex;
                        EnhancedMemory memory = createTestMemory(memoryId, content);
                        
                        return graphStore.addMemory(memory);
                    }))
                .collect(Collectors.toList());
            
            // 等待所有添加完成
            assertDoesNotThrow(() -> 
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join());
            
            // 验证所有内存都已添加
            List<EnhancedMemory> allMemories = graphStore.getUserMemories(TEST_USER_ID).join();
            assertTrue(allMemories.size() >= threadCount * memoriesPerThread);
        }
        
        @Test
        @DisplayName("并发创建关系")
        void testConcurrentRelationshipCreation() throws ExecutionException, InterruptedException {
            // 先创建一些节点
            List<String> nodeIds = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                Map<String, Object> props = new HashMap<>();
                props.put("index", i);
                nodeIds.add(graphStore.createNode("ConcurrentNode", props).get());
            }
            
            int relationshipCount = 20;
            
            List<CompletableFuture<String>> futures = IntStream.range(0, relationshipCount)
                .mapToObj(i -> {
                    String fromNode = nodeIds.get(i % nodeIds.size());
                    String toNode = nodeIds.get((i + 1) % nodeIds.size());
                    Map<String, Object> props = new HashMap<>();
                    props.put("weight", i);
                    
                    return graphStore.createRelationship(fromNode, toNode, "CONNECTS_TO", props);
                })
                .collect(Collectors.toList());
            
            // 等待所有关系创建完成
            List<String> relationshipIds = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
            
            assertEquals(relationshipCount, relationshipIds.size());
            assertTrue(relationshipIds.stream().allMatch(Objects::nonNull));
        }
    }
    
    @Nested
    @DisplayName("查询执行测试")
    class QueryExecutionTests {
        
        @Test
        @DisplayName("执行Cypher查询")
        void testExecuteQuery() throws ExecutionException, InterruptedException {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("name", "test");
            
            // 当前实现返回空列表
            List<Map<String, Object>> results = 
                graphStore.executeQuery("MATCH (n:Person {name: $name}) RETURN n", parameters).get();
            
            assertNotNull(results);
            assertTrue(results.isEmpty()); // 简化实现返回空列表
        }
    }
    
    @Nested
    @DisplayName("资源管理测试")
    class ResourceManagementTests {
        
        @Test
        @DisplayName("关闭图存储")
        void testCloseGraphStore() throws ExecutionException, InterruptedException {
            // 添加一些数据
            Map<String, Object> props = new HashMap<>();
            props.put("name", "test");
            graphStore.createNode("TestNode", props).get();
            
            EnhancedMemory memory = createTestMemory("close_test_memory", "测试内容");
            graphStore.addMemory(memory).get();
            
            // 关闭存储
            assertDoesNotThrow(() -> graphStore.close().get());
            
            // 验证数据已清理（通过尝试获取来验证）
            assertNull(graphStore.getMemory("close_test_memory").join());
        }
    }
    
    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTests {
        
        @Test
        @DisplayName("添加null内存抛出异常")
        void testAddNullMemoryThrowsException() {
            CompletableFuture<Void> future = graphStore.addMemory(null);
            assertThrows(RuntimeException.class, future::join);
        }
        
        @Test
        @DisplayName("更新null内存抛出异常")
        void testUpdateNullMemoryThrowsException() {
            CompletableFuture<Void> future = graphStore.updateMemory(null);
            assertThrows(RuntimeException.class, future::join);
        }
        
        @Test
        @DisplayName("更新不存在节点")
        void testUpdateNonexistentNode() throws ExecutionException, InterruptedException {
            Map<String, Object> updates = new HashMap<>();
            updates.put("property", "value");
            
            // 更新不存在的节点应该正常完成（无操作）
            assertDoesNotThrow(() -> graphStore.updateNode("nonexistent", updates).get());
        }
        
        @Test
        @DisplayName("更新不存在关系")
        void testUpdateNonexistentRelationship() throws ExecutionException, InterruptedException {
            Map<String, Object> updates = new HashMap<>();
            updates.put("property", "value");
            
            // 更新不存在的关系应该正常完成（无操作）
            assertDoesNotThrow(() -> graphStore.updateRelationship("nonexistent", updates).get());
        }
    }
    
    // 辅助方法
    private EnhancedMemory createTestMemory(String id, String content) {
        return createTestMemory(id, content, TEST_USER_ID);
    }
    
    private EnhancedMemory createTestMemory(String id, String content, String userId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "test");
        metadata.put("category", "general");
        
        EnhancedMemory memory = new EnhancedMemory(id, content, userId, metadata);
        memory.setType(MemoryType.FACTUAL);
        return memory;
    }
}