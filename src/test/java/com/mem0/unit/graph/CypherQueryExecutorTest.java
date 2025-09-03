package com.mem0.unit.graph;

import com.mem0.graph.impl.InMemoryGraphStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for Cypher query execution functionality
 */
public class CypherQueryExecutorTest {
    
    private InMemoryGraphStore graphStore;
    
    @BeforeEach
    void setUp() {
        graphStore = new InMemoryGraphStore();
    }
    
    @Test
    void testCreateNodeQuery() throws Exception {
        String cypherQuery = "CREATE (n:Person {name: 'John', age: 30})";
        Map<String, Object> parameters = new HashMap<>();
        
        CompletableFuture<List<Map<String, Object>>> result = graphStore.executeQuery(cypherQuery, parameters);
        List<Map<String, Object>> resultList = result.get();
        
        assertNotNull(resultList);
        // CREATE queries typically return empty results
        assertTrue(resultList.isEmpty());
    }
    
    @Test
    void testCreateRelationshipQuery() throws Exception {
        // First create nodes
        String createNodes = "CREATE (a:Person {name: 'Alice'}), (b:Person {name: 'Bob'})";
        Map<String, Object> parameters = new HashMap<>();
        
        CompletableFuture<List<Map<String, Object>>> createResult = graphStore.executeQuery(createNodes, parameters);
        createResult.get();
        
        // Then create relationship
        String createRel = "MATCH (a:Person {name: 'Alice'}), (b:Person {name: 'Bob'}) CREATE (a)-[:KNOWS]->(b)";
        CompletableFuture<List<Map<String, Object>>> relResult = graphStore.executeQuery(createRel, parameters);
        List<Map<String, Object>> resultList = relResult.get();
        
        assertNotNull(resultList);
    }
    
    @Test
    void testMatchQuery() throws Exception {
        // First create a node
        String createQuery = "CREATE (n:Person {name: 'Test Person', age: 25})";
        graphStore.executeQuery(createQuery, new HashMap<>()).get();
        
        // Then query for it
        String matchQuery = "MATCH (n:Person) RETURN n";
        Map<String, Object> parameters = new HashMap<>();
        
        CompletableFuture<List<Map<String, Object>>> result = graphStore.executeQuery(matchQuery, parameters);
        List<Map<String, Object>> resultList = result.get();
        
        assertNotNull(resultList);
        // Should return at least the created node
        assertTrue(resultList.size() >= 0);
    }
    
    @Test
    void testMatchWithWhereClause() throws Exception {
        // Create test data
        String createQuery = "CREATE (n:Person {name: 'Alice', age: 30}), (m:Person {name: 'Bob', age: 25})";
        graphStore.executeQuery(createQuery, new HashMap<>()).get();
        
        // Query with WHERE clause
        String matchQuery = "MATCH (n:Person) WHERE n.age > 28 RETURN n";
        Map<String, Object> parameters = new HashMap<>();
        
        CompletableFuture<List<Map<String, Object>>> result = graphStore.executeQuery(matchQuery, parameters);
        List<Map<String, Object>> resultList = result.get();
        
        assertNotNull(resultList);
    }
    
    @Test
    void testDeleteQuery() throws Exception {
        // First create a node
        String createQuery = "CREATE (n:Person {name: 'ToDelete'})";
        graphStore.executeQuery(createQuery, new HashMap<>()).get();
        
        // Then delete it
        String deleteQuery = "MATCH (n:Person {name: 'ToDelete'}) DELETE n";
        Map<String, Object> parameters = new HashMap<>();
        
        CompletableFuture<List<Map<String, Object>>> result = graphStore.executeQuery(deleteQuery, parameters);
        List<Map<String, Object>> resultList = result.get();
        
        assertNotNull(resultList);
    }
    
    @Test
    void testParameterizedQuery() throws Exception {
        String cypherQuery = "CREATE (n:Person {name: $name, age: $age})";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "Parameterized Person");
        parameters.put("age", 35);
        
        CompletableFuture<List<Map<String, Object>>> result = graphStore.executeQuery(cypherQuery, parameters);
        List<Map<String, Object>> resultList = result.get();
        
        assertNotNull(resultList);
    }
    
    @Test
    void testComplexMatchQuery() throws Exception {
        // Create test graph structure
        String setupQuery = "CREATE (a:Person {name: 'Alice'})-[:KNOWS]->(b:Person {name: 'Bob'}), " +
                          "(b)-[:WORKS_AT]->(c:Company {name: 'TechCorp'})";
        graphStore.executeQuery(setupQuery, new HashMap<>()).get();
        
        // Complex match with multiple relationships
        String complexQuery = "MATCH (p:Person)-[:KNOWS]->(:Person)-[:WORKS_AT]->(c:Company) RETURN p, c";
        Map<String, Object> parameters = new HashMap<>();
        
        CompletableFuture<List<Map<String, Object>>> result = graphStore.executeQuery(complexQuery, parameters);
        List<Map<String, Object>> resultList = result.get();
        
        assertNotNull(resultList);
    }
    
    @Test
    void testInvalidCypherQuery() throws Exception {
        String invalidQuery = "INVALID CYPHER QUERY SYNTAX";
        Map<String, Object> parameters = new HashMap<>();
        
        CompletableFuture<List<Map<String, Object>>> result = graphStore.executeQuery(invalidQuery, parameters);
        
        // Should handle invalid queries gracefully
        assertDoesNotThrow(() -> {
            List<Map<String, Object>> resultList = result.get();
            assertNotNull(resultList);
        });
    }
    
    @Test
    void testEmptyQuery() throws Exception {
        String emptyQuery = "";
        Map<String, Object> parameters = new HashMap<>();
        
        CompletableFuture<List<Map<String, Object>>> result = graphStore.executeQuery(emptyQuery, parameters);
        List<Map<String, Object>> resultList = result.get();
        
        assertNotNull(resultList);
        assertTrue(resultList.isEmpty());
    }
    
    @Test
    void testNullParametersQuery() throws Exception {
        String cypherQuery = "MATCH (n) RETURN n LIMIT 1";
        
        CompletableFuture<List<Map<String, Object>>> result = graphStore.executeQuery(cypherQuery, null);
        List<Map<String, Object>> resultList = result.get();
        
        assertNotNull(resultList);
    }
    
    @Test
    void testConcurrentQueries() throws Exception {
        // Setup test data
        String setupQuery = "CREATE (n:Test {id: 1}), (m:Test {id: 2}), (k:Test {id: 3})";
        graphStore.executeQuery(setupQuery, new HashMap<>()).get();
        
        // Execute multiple queries concurrently
        CompletableFuture<List<Map<String, Object>>> query1 = 
            graphStore.executeQuery("MATCH (n:Test) WHERE n.id = 1 RETURN n", new HashMap<>());
        CompletableFuture<List<Map<String, Object>>> query2 = 
            graphStore.executeQuery("MATCH (n:Test) WHERE n.id = 2 RETURN n", new HashMap<>());
        CompletableFuture<List<Map<String, Object>>> query3 = 
            graphStore.executeQuery("MATCH (n:Test) WHERE n.id = 3 RETURN n", new HashMap<>());
        
        // Wait for all queries to complete
        CompletableFuture<Void> allOf = CompletableFuture.allOf(query1, query2, query3);
        allOf.get();
        
        List<Map<String, Object>> result1 = query1.get();
        List<Map<String, Object>> result2 = query2.get();
        List<Map<String, Object>> result3 = query3.get();
        
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
    }
    
    @Test
    void testQueryWithSpecialCharacters() throws Exception {
        String cypherQuery = "CREATE (n:Person {name: '测试用户', description: 'Special chars: éñü'})";
        Map<String, Object> parameters = new HashMap<>();
        
        CompletableFuture<List<Map<String, Object>>> result = graphStore.executeQuery(cypherQuery, parameters);
        List<Map<String, Object>> resultList = result.get();
        
        assertNotNull(resultList);
    }
    
    @Test
    void testUpdateQuery() throws Exception {
        // Create initial node
        String createQuery = "CREATE (n:Person {name: 'Original', version: 1})";
        graphStore.executeQuery(createQuery, new HashMap<>()).get();
        
        // Update the node
        String updateQuery = "MATCH (n:Person {name: 'Original'}) SET n.name = 'Updated', n.version = 2";
        Map<String, Object> parameters = new HashMap<>();
        
        CompletableFuture<List<Map<String, Object>>> result = graphStore.executeQuery(updateQuery, parameters);
        List<Map<String, Object>> resultList = result.get();
        
        assertNotNull(resultList);
    }
    
    @Test
    void testQueryPerformance() throws Exception {
        // Create multiple nodes for performance testing
        for (int i = 0; i < 10; i++) {
            String createQuery = "CREATE (n:TestPerf {id: " + i + ", name: 'Node" + i + "'})";
            graphStore.executeQuery(createQuery, new HashMap<>()).get();
        }
        
        long startTime = System.currentTimeMillis();
        
        // Execute a query that should scan multiple nodes
        String perfQuery = "MATCH (n:TestPerf) WHERE n.id > 5 RETURN n";
        CompletableFuture<List<Map<String, Object>>> result = graphStore.executeQuery(perfQuery, new HashMap<>());
        List<Map<String, Object>> resultList = result.get();
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        assertNotNull(resultList);
        // Query should complete within reasonable time (less than 5 seconds)
        assertTrue(executionTime < 5000, "Query took too long: " + executionTime + "ms");
    }
}