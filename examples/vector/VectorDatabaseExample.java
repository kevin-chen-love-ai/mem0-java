package examples.vector;

import com.mem0.Mem0;
import com.mem0.config.Mem0Configuration;
import com.mem0.store.VectorStore;
import com.mem0.store.VectorStore.VectorEntry;
import com.mem0.store.VectorStore.SearchResult;
import com.mem0.embedding.impl.MockEmbeddingProvider;
import com.mem0.llm.MockLLMProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 向量数据库集成示例 - Vector Database Integration Example
 * 
 * 展示如何集成不同的向量数据库：Pinecone、Weaviate、Qdrant等
 * Demonstrates integration with various vector databases: Pinecone, Weaviate, Qdrant, etc.
 */
public class VectorDatabaseExample {
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== Vector Database Integration Example ===\n");
        
        // 1. Pinecone集成示例
        System.out.println("1. Testing Pinecone Integration:");
        testPineconeIntegration();
        
        // 2. Weaviate集成示例
        System.out.println("\n2. Testing Weaviate Integration:");
        testWeaviateIntegration();
        
        // 3. Qdrant集成示例
        System.out.println("\n3. Testing Qdrant Integration:");
        testQdrantIntegration();
        
        // 4. 自定义向量存储示例
        System.out.println("\n4. Testing Custom Vector Store:");
        testCustomVectorStore();
        
        System.out.println("\n=== Example completed successfully! ===");
    }
    
    private static void testPineconeIntegration() throws Exception {
        Mem0Configuration config = new Mem0Configuration();
        config.setVectorStore(new PineconeVectorStore(
            "your-pinecone-api-key",
            "your-environment", 
            "mem0-index"
        ));
        config.setLlmProvider(new MockLLMProvider());
        config.setEmbeddingProvider(new MockEmbeddingProvider());
        
        Mem0 mem0 = new Mem0(config);
        
        String memoryId = mem0.add("Pinecone is a vector database service", "pinecone-user");
        System.out.println("   ✓ Added memory to Pinecone: " + memoryId);
        
        List<com.mem0.core.Memory> results = mem0.search("vector database", "pinecone-user");
        System.out.println("   ✓ Search returned " + results.size() + " results");
        
        mem0.close();
    }
    
    private static void testWeaviateIntegration() throws Exception {
        Mem0Configuration config = new Mem0Configuration();
        config.setVectorStore(new WeaviateVectorStore("http://localhost:8080", "your-api-key"));
        config.setLlmProvider(new MockLLMProvider());
        config.setEmbeddingProvider(new MockEmbeddingProvider());
        
        Mem0 mem0 = new Mem0(config);
        
        String memoryId = mem0.add("Weaviate is an open-source vector search engine", "weaviate-user");
        System.out.println("   ✓ Added memory to Weaviate: " + memoryId);
        
        mem0.close();
    }
    
    private static void testQdrantIntegration() throws Exception {
        Mem0Configuration config = new Mem0Configuration();
        config.setVectorStore(new QdrantVectorStore("http://localhost:6333", "your-api-key"));
        config.setLlmProvider(new MockLLMProvider());
        config.setEmbeddingProvider(new MockEmbeddingProvider());
        
        Mem0 mem0 = new Mem0(config);
        
        String memoryId = mem0.add("Qdrant provides high-performance vector similarity search", "qdrant-user");
        System.out.println("   ✓ Added memory to Qdrant: " + memoryId);
        
        mem0.close();
    }
    
    private static void testCustomVectorStore() throws Exception {
        Mem0Configuration config = new Mem0Configuration();
        config.setVectorStore(new CustomVectorStore());
        config.setLlmProvider(new MockLLMProvider());
        config.setEmbeddingProvider(new MockEmbeddingProvider());
        
        Mem0 mem0 = new Mem0(config);
        
        String memoryId = mem0.add("Custom vector stores allow flexible implementations", "custom-user");
        System.out.println("   ✓ Added memory to custom vector store: " + memoryId);
        
        mem0.close();
    }
}

/**
 * Pinecone向量存储实现 - Pinecone Vector Store Implementation
 */
class PineconeVectorStore implements VectorStore {
    private static final Logger logger = LoggerFactory.getLogger(PineconeVectorStore.class);
    
    private final String apiKey;
    private final String environment;
    private final String indexName;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public PineconeVectorStore(String apiKey, String environment, String indexName) {
        this.apiKey = apiKey;
        this.environment = environment;
        this.indexName = indexName;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public CompletableFuture<String> insertVector(String id, List<Float> embedding, 
                                                 Map<String, Object> metadata, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> vector = Map.of(
                    "id", id,
                    "values", embedding,
                    "metadata", Map.of(
                        "userId", userId,
                        "content", metadata.getOrDefault("content", ""),
                        "timestamp", System.currentTimeMillis()
                    )
                );
                
                Map<String, Object> requestBody = Map.of("vectors", List.of(vector));
                
                String url = String.format("https://%s-%s.svc.%s.pinecone.io/vectors/upsert", 
                                          indexName, environment, environment);
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .header("Api-Key", apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Pinecone upsert failed: " + response.body());
                }
                
                logger.debug("Successfully inserted vector into Pinecone: {}", id);
                return id;
                
            } catch (Exception e) {
                logger.error("Error inserting vector into Pinecone", e);
                throw new RuntimeException("Pinecone insertion failed", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<SearchResult>> searchVectors(List<Float> queryEmbedding, 
                                                              String userId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> requestBody = Map.of(
                    "vector", queryEmbedding,
                    "topK", limit,
                    "includeMetadata", true,
                    "filter", Map.of("userId", userId)
                );
                
                String url = String.format("https://%s-%s.svc.%s.pinecone.io/query", 
                                          indexName, environment, environment);
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .header("Api-Key", apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Pinecone query failed: " + response.body());
                }
                
                Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
                List<Map<String, Object>> matches = (List<Map<String, Object>>) responseBody.get("matches");
                
                return matches.stream()
                        .map(match -> {
                            String id = (String) match.get("id");
                            double score = ((Number) match.get("score")).doubleValue();
                            Map<String, Object> metadata = (Map<String, Object>) match.get("metadata");
                            
                            return new SearchResult(id, score, metadata);
                        })
                        .collect(java.util.stream.Collectors.toList());
                
            } catch (Exception e) {
                logger.error("Error querying Pinecone", e);
                throw new RuntimeException("Pinecone query failed", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> deleteVector(String id, String userId) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> requestBody = Map.of("ids", List.of(id));
                
                String url = String.format("https://%s-%s.svc.%s.pinecone.io/vectors/delete", 
                                          indexName, environment, environment);
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .header("Api-Key", apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Pinecone delete failed: " + response.body());
                }
                
                logger.debug("Successfully deleted vector from Pinecone: {}", id);
                
            } catch (Exception e) {
                logger.error("Error deleting vector from Pinecone", e);
                throw new RuntimeException("Pinecone deletion failed", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> updateVector(String id, List<Float> embedding, 
                                               Map<String, Object> metadata, String userId) {
        return insertVector(id, embedding, metadata, userId).thenApply(result -> null);
    }
    
    @Override
    public CompletableFuture<VectorEntry> getVector(String id, String userId) {
        return CompletableFuture.completedFuture(null); // Pinecone doesn't support direct get by ID
    }
    
    @Override
    public CompletableFuture<List<VectorEntry>> getAllVectors(String userId) {
        return CompletableFuture.completedFuture(Collections.emptyList()); // Expensive operation in Pinecone
    }
    
    @Override
    public CompletableFuture<Long> getVectorCount(String userId) {
        return CompletableFuture.completedFuture(0L); // Requires separate stats API call
    }
}

/**
 * Weaviate向量存储实现 - Weaviate Vector Store Implementation
 */
class WeaviateVectorStore implements VectorStore {
    private static final Logger logger = LoggerFactory.getLogger(WeaviateVectorStore.class);
    
    private final String endpoint;
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String className = "MemoryVector";
    
    public WeaviateVectorStore(String endpoint, String apiKey) {
        this.endpoint = endpoint.endsWith("/") ? endpoint : endpoint + "/";
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public CompletableFuture<String> insertVector(String id, List<Float> embedding, 
                                                 Map<String, Object> metadata, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> properties = new HashMap<>(metadata);
                properties.put("userId", userId);
                properties.put("vectorId", id);
                
                Map<String, Object> object = Map.of(
                    "class", className,
                    "id", id,
                    "properties", properties,
                    "vector", embedding
                );
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint + "v1/objects"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(object)))
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Weaviate insert failed: " + response.body());
                }
                
                logger.debug("Successfully inserted vector into Weaviate: {}", id);
                return id;
                
            } catch (Exception e) {
                logger.error("Error inserting vector into Weaviate", e);
                throw new RuntimeException("Weaviate insertion failed", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<SearchResult>> searchVectors(List<Float> queryEmbedding, 
                                                              String userId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String graphql = String.format("""
                    {
                      Get {
                        %s(
                          nearVector: {
                            vector: %s
                          }
                          where: {
                            path: ["userId"]
                            operator: Equal
                            valueString: "%s"
                          }
                          limit: %d
                        ) {
                          vectorId
                          content
                          _additional {
                            certainty
                            vector
                          }
                        }
                      }
                    }
                    """, className, embedding ArrayToString(queryEmbedding), userId, limit);
                
                Map<String, Object> requestBody = Map.of("query", graphql);
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint + "v1/graphql"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Weaviate search failed: " + response.body());
                }
                
                // 解析GraphQL响应
                Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                Map<String, Object> get = (Map<String, Object>) data.get("Get");
                List<Map<String, Object>> results = (List<Map<String, Object>>) get.get(className);
                
                return results.stream()
                        .map(result -> {
                            String id = (String) result.get("vectorId");
                            Map<String, Object> additional = (Map<String, Object>) result.get("_additional");
                            double certainty = ((Number) additional.get("certainty")).doubleValue();
                            
                            Map<String, Object> metadata = new HashMap<>();
                            metadata.put("content", result.get("content"));
                            
                            return new SearchResult(id, certainty, metadata);
                        })
                        .collect(java.util.stream.Collectors.toList());
                
            } catch (Exception e) {
                logger.error("Error searching Weaviate", e);
                throw new RuntimeException("Weaviate search failed", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> deleteVector(String id, String userId) {
        return CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint + "v1/objects/" + id))
                        .header("Authorization", "Bearer " + apiKey)
                        .DELETE()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 204) {
                    throw new RuntimeException("Weaviate delete failed: " + response.body());
                }
                
                logger.debug("Successfully deleted vector from Weaviate: {}", id);
                
            } catch (Exception e) {
                logger.error("Error deleting vector from Weaviate", e);
                throw new RuntimeException("Weaviate deletion failed", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> updateVector(String id, List<Float> embedding, 
                                               Map<String, Object> metadata, String userId) {
        return insertVector(id, embedding, metadata, userId).thenApply(result -> null);
    }
    
    @Override
    public CompletableFuture<VectorEntry> getVector(String id, String userId) {
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<List<VectorEntry>> getAllVectors(String userId) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }
    
    @Override
    public CompletableFuture<Long> getVectorCount(String userId) {
        return CompletableFuture.completedFuture(0L);
    }
    
    private String embeddingArrayToString(List<Float> embedding) {
        return "[" + String.join(",", embedding.stream()
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toList())) + "]";
    }
}

/**
 * Qdrant向量存储实现 - Qdrant Vector Store Implementation
 */
class QdrantVectorStore implements VectorStore {
    private static final Logger logger = LoggerFactory.getLogger(QdrantVectorStore.class);
    
    private final String endpoint;
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String collectionName = "mem0_vectors";
    
    public QdrantVectorStore(String endpoint, String apiKey) {
        this.endpoint = endpoint.endsWith("/") ? endpoint : endpoint + "/";
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public CompletableFuture<String> insertVector(String id, List<Float> embedding, 
                                                 Map<String, Object> metadata, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> payload = new HashMap<>(metadata);
                payload.put("userId", userId);
                
                Map<String, Object> point = Map.of(
                    "id", id,
                    "vector", embedding,
                    "payload", payload
                );
                
                Map<String, Object> requestBody = Map.of("points", List.of(point));
                
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint + "collections/" + collectionName + "/points"))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)));
                
                if (apiKey != null && !apiKey.isEmpty()) {
                    requestBuilder.header("api-key", apiKey);
                }
                
                HttpRequest request = requestBuilder.build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Qdrant insert failed: " + response.body());
                }
                
                logger.debug("Successfully inserted vector into Qdrant: {}", id);
                return id;
                
            } catch (Exception e) {
                logger.error("Error inserting vector into Qdrant", e);
                throw new RuntimeException("Qdrant insertion failed", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<SearchResult>> searchVectors(List<Float> queryEmbedding, 
                                                              String userId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> requestBody = Map.of(
                    "vector", queryEmbedding,
                    "limit", limit,
                    "with_payload", true,
                    "filter", Map.of(
                        "must", List.of(Map.of(
                            "key", "userId",
                            "match", Map.of("value", userId)
                        ))
                    )
                );
                
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint + "collections/" + collectionName + "/points/search"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)));
                
                if (apiKey != null && !apiKey.isEmpty()) {
                    requestBuilder.header("api-key", apiKey);
                }
                
                HttpRequest request = requestBuilder.build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Qdrant search failed: " + response.body());
                }
                
                Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
                List<Map<String, Object>> results = (List<Map<String, Object>>) responseBody.get("result");
                
                return results.stream()
                        .map(result -> {
                            String id = result.get("id").toString();
                            double score = ((Number) result.get("score")).doubleValue();
                            Map<String, Object> payload = (Map<String, Object>) result.get("payload");
                            
                            return new SearchResult(id, score, payload);
                        })
                        .collect(java.util.stream.Collectors.toList());
                
            } catch (Exception e) {
                logger.error("Error searching Qdrant", e);
                throw new RuntimeException("Qdrant search failed", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> deleteVector(String id, String userId) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> requestBody = Map.of("points", List.of(id));
                
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint + "collections/" + collectionName + "/points/delete"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)));
                
                if (apiKey != null && !apiKey.isEmpty()) {
                    requestBuilder.header("api-key", apiKey);
                }
                
                HttpRequest request = requestBuilder.build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Qdrant delete failed: " + response.body());
                }
                
                logger.debug("Successfully deleted vector from Qdrant: {}", id);
                
            } catch (Exception e) {
                logger.error("Error deleting vector from Qdrant", e);
                throw new RuntimeException("Qdrant deletion failed", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> updateVector(String id, List<Float> embedding, 
                                               Map<String, Object> metadata, String userId) {
        return insertVector(id, embedding, metadata, userId).thenApply(result -> null);
    }
    
    @Override
    public CompletableFuture<VectorEntry> getVector(String id, String userId) {
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<List<VectorEntry>> getAllVectors(String userId) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }
    
    @Override
    public CompletableFuture<Long> getVectorCount(String userId) {
        return CompletableFuture.completedFuture(0L);
    }
}

/**
 * 自定义向量存储实现 - Custom Vector Store Implementation
 */
class CustomVectorStore implements VectorStore {
    private static final Logger logger = LoggerFactory.getLogger(CustomVectorStore.class);
    
    private final Map<String, VectorEntry> vectors = new ConcurrentHashMap<>();
    
    @Override
    public CompletableFuture<String> insertVector(String id, List<Float> embedding, 
                                                 Map<String, Object> metadata, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            VectorEntry entry = new VectorEntry(id, embedding, metadata, userId);
            vectors.put(userId + ":" + id, entry);
            logger.debug("Inserted vector into custom store: {}", id);
            return id;
        });
    }
    
    @Override
    public CompletableFuture<List<SearchResult>> searchVectors(List<Float> queryEmbedding, 
                                                              String userId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<SearchResult> results = new ArrayList<>();
            
            vectors.entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith(userId + ":"))
                    .forEach(entry -> {
                        VectorEntry vectorEntry = entry.getValue();
                        double similarity = calculateCosineSimilarity(queryEmbedding, vectorEntry.getEmbedding());
                        
                        SearchResult result = new SearchResult(
                            vectorEntry.getId(),
                            similarity,
                            vectorEntry.getMetadata()
                        );
                        results.add(result);
                    });
            
            return results.stream()
                    .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                    .limit(limit)
                    .collect(java.util.stream.Collectors.toList());
        });
    }
    
    @Override
    public CompletableFuture<Void> deleteVector(String id, String userId) {
        return CompletableFuture.runAsync(() -> {
            vectors.remove(userId + ":" + id);
            logger.debug("Deleted vector from custom store: {}", id);
        });
    }
    
    @Override
    public CompletableFuture<Void> updateVector(String id, List<Float> embedding, 
                                               Map<String, Object> metadata, String userId) {
        return CompletableFuture.runAsync(() -> {
            VectorEntry entry = new VectorEntry(id, embedding, metadata, userId);
            vectors.put(userId + ":" + id, entry);
            logger.debug("Updated vector in custom store: {}", id);
        });
    }
    
    @Override
    public CompletableFuture<VectorEntry> getVector(String id, String userId) {
        return CompletableFuture.supplyAsync(() -> vectors.get(userId + ":" + id));
    }
    
    @Override
    public CompletableFuture<List<VectorEntry>> getAllVectors(String userId) {
        return CompletableFuture.supplyAsync(() -> 
            vectors.entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith(userId + ":"))
                    .map(Map.Entry::getValue)
                    .collect(java.util.stream.Collectors.toList())
        );
    }
    
    @Override
    public CompletableFuture<Long> getVectorCount(String userId) {
        return CompletableFuture.supplyAsync(() -> 
            vectors.entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith(userId + ":"))
                    .count()
        );
    }
    
    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.runAsync(() -> {
            vectors.clear();
            logger.info("Custom vector store closed");
        });
    }
    
    private double calculateCosineSimilarity(List<Float> vec1, List<Float> vec2) {
        if (vec1.size() != vec2.size()) return 0.0;
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vec1.size(); i++) {
            dotProduct += vec1.get(i) * vec2.get(i);
            norm1 += vec1.get(i) * vec1.get(i);
            norm2 += vec2.get(i) * vec2.get(i);
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}