package examples.redis;

import com.mem0.Mem0;
import com.mem0.config.Mem0Configuration;
import com.mem0.embedding.impl.MockEmbeddingProvider;
import com.mem0.llm.MockLLMProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Pipeline;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.time.Duration;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Redis集成示例 - Redis Integration Example
 * 
 * 展示如何使用Redis作为缓存层和会话存储
 * Demonstrates using Redis as cache layer and session storage
 */
public class RedisIntegrationExample {
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== Redis Integration Example ===\n");
        
        // 1. Redis缓存示例
        System.out.println("1. Testing Redis Cache Integration:");
        testRedisCache();
        
        // 2. Redis会话存储示例
        System.out.println("\n2. Testing Redis Session Storage:");
        testRedisSessionStorage();
        
        // 3. Redis发布/订阅示例
        System.out.println("\n3. Testing Redis Pub/Sub:");
        testRedisPubSub();
        
        // 4. Redis分布式锁示例
        System.out.println("\n4. Testing Redis Distributed Lock:");
        testRedisDistributedLock();
        
        // 5. 完整的Mem0+Redis集成
        System.out.println("\n5. Testing Complete Mem0 + Redis Integration:");
        testMem0WithRedis();
        
        System.out.println("\n=== Example completed successfully! ===");
    }
    
    private static void testRedisCache() throws Exception {
        RedisCache cache = new RedisCache("localhost", 6379);
        
        // 测试基本缓存操作
        cache.set("test:key1", "Hello Redis!", 60);
        System.out.println("   ✓ Set cache value");
        
        Optional<String> value = cache.get("test:key1");
        System.out.println("   ✓ Get cache value: " + value.orElse("null"));
        
        // 测试对象缓存
        Map<String, Object> data = Map.of(
            "name", "John Doe",
            "age", 30,
            "skills", List.of("Java", "Python", "Redis")
        );
        
        cache.setObject("user:1", data, 300);
        System.out.println("   ✓ Set object cache");
        
        Optional<Map<String, Object>> userData = cache.getObject("user:1", Map.class);
        System.out.println("   ✓ Get object cache: " + userData.orElse(null));
        
        cache.close();
    }
    
    private static void testRedisSessionStorage() throws Exception {
        RedisSessionStore sessionStore = new RedisSessionStore("localhost", 6379);
        
        String sessionId = sessionStore.createSession("user123");
        System.out.println("   ✓ Created session: " + sessionId);
        
        sessionStore.setSessionData(sessionId, "preference", "dark_mode");
        sessionStore.setSessionData(sessionId, "language", "en");
        System.out.println("   ✓ Set session data");
        
        Optional<String> preference = sessionStore.getSessionData(sessionId, "preference");
        System.out.println("   ✓ Get session data: " + preference.orElse("null"));
        
        sessionStore.extendSession(sessionId, 1800); // 30分钟
        System.out.println("   ✓ Extended session TTL");
        
        sessionStore.close();
    }
    
    private static void testRedisPubSub() throws Exception {
        RedisPubSub pubSub = new RedisPubSub("localhost", 6379);
        
        // 设置消息监听器
        pubSub.subscribe("mem0:events", message -> {
            System.out.println("   ✓ Received message: " + message);
        });
        
        // 发布消息
        Thread.sleep(1000); // 等待订阅生效
        pubSub.publish("mem0:events", "Memory updated: user123");
        System.out.println("   ✓ Published message");
        
        Thread.sleep(1000); // 等待消息处理
        pubSub.close();
    }
    
    private static void testRedisDistributedLock() throws Exception {
        RedisDistributedLock lock = new RedisDistributedLock("localhost", 6379);
        
        String lockKey = "mem0:lock:user123";
        boolean acquired = lock.acquire(lockKey, "worker-1", 30);
        
        if (acquired) {
            System.out.println("   ✓ Acquired distributed lock");
            
            // 模拟一些工作
            Thread.sleep(1000);
            
            boolean released = lock.release(lockKey, "worker-1");
            System.out.println("   ✓ Released distributed lock: " + released);
        } else {
            System.out.println("   ✗ Failed to acquire lock");
        }
        
        lock.close();
    }
    
    private static void testMem0WithRedis() throws Exception {
        Mem0Configuration config = new Mem0Configuration();
        
        // 使用Redis作为缓存层
        config.setCacheProvider(new RedisCacheProvider("localhost", 6379));
        config.setLlmProvider(new MockLLMProvider());
        config.setEmbeddingProvider(new MockEmbeddingProvider());
        
        Mem0 mem0 = new Mem0(config);
        
        // 添加记忆（会使用Redis缓存）
        String memoryId = mem0.add("Redis integration makes mem0 faster", "redis-user");
        System.out.println("   ✓ Added cached memory: " + memoryId);
        
        // 搜索记忆（结果会被缓存）
        List<com.mem0.core.Memory> results = mem0.search("Redis", "redis-user");
        System.out.println("   ✓ Search with caching: " + results.size() + " results");
        
        mem0.close();
    }
}

/**
 * Redis缓存实现 - Redis Cache Implementation
 */
class RedisCache {
    private static final Logger logger = LoggerFactory.getLogger(RedisCache.class);
    
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    
    public RedisCache(String host, int port) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(20);
        poolConfig.setMaxIdle(10);
        poolConfig.setMinIdle(2);
        
        this.jedisPool = new JedisPool(poolConfig, host, port);
        this.objectMapper = new ObjectMapper();
        
        // 测试连接
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.ping();
            logger.info("Successfully connected to Redis at {}:{}", host, port);
        } catch (Exception e) {
            logger.error("Failed to connect to Redis", e);
            throw new RuntimeException("Redis connection failed", e);
        }
    }
    
    public void set(String key, String value, int ttlSeconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (ttlSeconds > 0) {
                jedis.setex(key, ttlSeconds, value);
            } else {
                jedis.set(key, value);
            }
            logger.debug("Set cache key: {} with TTL: {}", key, ttlSeconds);
        }
    }
    
    public Optional<String> get(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get(key);
            logger.debug("Get cache key: {} -> {}", key, value != null ? "found" : "not found");
            return Optional.ofNullable(value);
        }
    }
    
    public void setObject(String key, Object object, int ttlSeconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = objectMapper.writeValueAsString(object);
            if (ttlSeconds > 0) {
                jedis.setex(key, ttlSeconds, json);
            } else {
                jedis.set(key, json);
            }
            logger.debug("Set object cache key: {} with TTL: {}", key, ttlSeconds);
        } catch (Exception e) {
            logger.error("Error setting object cache", e);
            throw new RuntimeException("Redis object set failed", e);
        }
    }
    
    public <T> Optional<T> getObject(String key, Class<T> clazz) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get(key);
            if (json != null) {
                T object = objectMapper.readValue(json, clazz);
                logger.debug("Get object cache key: {} -> found", key);
                return Optional.of(object);
            }
            logger.debug("Get object cache key: {} -> not found", key);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Error getting object cache", e);
            return Optional.empty();
        }
    }
    
    public void delete(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key);
            logger.debug("Deleted cache key: {}", key);
        }
    }
    
    public boolean exists(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.exists(key);
        }
    }
    
    public void close() {
        if (jedisPool != null) {
            jedisPool.close();
            logger.info("Redis cache closed");
        }
    }
}

/**
 * Redis会话存储 - Redis Session Store
 */
class RedisSessionStore {
    private static final Logger logger = LoggerFactory.getLogger(RedisSessionStore.class);
    private static final String SESSION_PREFIX = "session:";
    private static final int DEFAULT_SESSION_TTL = 3600; // 1小时
    
    private final JedisPool jedisPool;
    
    public RedisSessionStore(String host, int port) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        this.jedisPool = new JedisPool(poolConfig, host, port);
    }
    
    public String createSession(String userId) {
        String sessionId = UUID.randomUUID().toString();
        String sessionKey = SESSION_PREFIX + sessionId;
        
        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, String> sessionData = new HashMap<>();
            sessionData.put("userId", userId);
            sessionData.put("createdAt", String.valueOf(System.currentTimeMillis()));
            sessionData.put("lastAccessed", String.valueOf(System.currentTimeMillis()));
            
            jedis.hmset(sessionKey, sessionData);
            jedis.expire(sessionKey, DEFAULT_SESSION_TTL);
            
            logger.debug("Created session: {} for user: {}", sessionId, userId);
            return sessionId;
        }
    }
    
    public void setSessionData(String sessionId, String key, String value) {
        String sessionKey = SESSION_PREFIX + sessionId;
        
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hset(sessionKey, key, value);
            jedis.hset(sessionKey, "lastAccessed", String.valueOf(System.currentTimeMillis()));
            
            logger.debug("Set session data: {} -> {} = {}", sessionId, key, value);
        }
    }
    
    public Optional<String> getSessionData(String sessionId, String key) {
        String sessionKey = SESSION_PREFIX + sessionId;
        
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.hget(sessionKey, key);
            jedis.hset(sessionKey, "lastAccessed", String.valueOf(System.currentTimeMillis()));
            
            logger.debug("Get session data: {} -> {} = {}", sessionId, key, value);
            return Optional.ofNullable(value);
        }
    }
    
    public Map<String, String> getAllSessionData(String sessionId) {
        String sessionKey = SESSION_PREFIX + sessionId;
        
        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, String> data = jedis.hgetAll(sessionKey);
            jedis.hset(sessionKey, "lastAccessed", String.valueOf(System.currentTimeMillis()));
            
            logger.debug("Get all session data: {} -> {} fields", sessionId, data.size());
            return data;
        }
    }
    
    public void extendSession(String sessionId, int ttlSeconds) {
        String sessionKey = SESSION_PREFIX + sessionId;
        
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.expire(sessionKey, ttlSeconds);
            logger.debug("Extended session TTL: {} -> {} seconds", sessionId, ttlSeconds);
        }
    }
    
    public void deleteSession(String sessionId) {
        String sessionKey = SESSION_PREFIX + sessionId;
        
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(sessionKey);
            logger.debug("Deleted session: {}", sessionId);
        }
    }
    
    public void close() {
        if (jedisPool != null) {
            jedisPool.close();
            logger.info("Redis session store closed");
        }
    }
}

/**
 * Redis发布/订阅 - Redis Pub/Sub
 */
class RedisPubSub {
    private static final Logger logger = LoggerFactory.getLogger(RedisPubSub.class);
    
    private final JedisPool jedisPool;
    private final Executor executor;
    private final Map<String, redis.clients.jedis.JedisPubSub> subscribers;
    
    public RedisPubSub(String host, int port) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        this.jedisPool = new JedisPool(poolConfig, host, port);
        this.executor = Executors.newCachedThreadPool();
        this.subscribers = new HashMap<>();
    }
    
    public void publish(String channel, String message) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.publish(channel, message);
            logger.debug("Published message to channel: {} -> {}", channel, message);
        }
    }
    
    public void subscribe(String channel, MessageHandler handler) {
        redis.clients.jedis.JedisPubSub subscriber = new redis.clients.jedis.JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                try {
                    handler.onMessage(message);
                } catch (Exception e) {
                    logger.error("Error handling message", e);
                }
            }
            
            @Override
            public void onSubscribe(String channel, int subscribedChannels) {
                logger.debug("Subscribed to channel: {}", channel);
            }
            
            @Override
            public void onUnsubscribe(String channel, int subscribedChannels) {
                logger.debug("Unsubscribed from channel: {}", channel);
            }
        };
        
        subscribers.put(channel, subscriber);
        
        // 在后台线程中运行订阅
        executor.execute(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.subscribe(subscriber, channel);
            } catch (Exception e) {
                logger.error("Error in subscription", e);
            }
        });
    }
    
    public void unsubscribe(String channel) {
        redis.clients.jedis.JedisPubSub subscriber = subscribers.remove(channel);
        if (subscriber != null) {
            subscriber.unsubscribe(channel);
            logger.debug("Unsubscribed from channel: {}", channel);
        }
    }
    
    public void close() {
        subscribers.values().forEach(redis.clients.jedis.JedisPubSub::unsubscribe);
        subscribers.clear();
        
        if (jedisPool != null) {
            jedisPool.close();
            logger.info("Redis pub/sub closed");
        }
    }
    
    @FunctionalInterface
    public interface MessageHandler {
        void onMessage(String message);
    }
}

/**
 * Redis分布式锁 - Redis Distributed Lock
 */
class RedisDistributedLock {
    private static final Logger logger = LoggerFactory.getLogger(RedisDistributedLock.class);
    private static final String UNLOCK_SCRIPT = 
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "return redis.call('del', KEYS[1]) " +
        "else " +
        "return 0 " +
        "end";
    
    private final JedisPool jedisPool;
    
    public RedisDistributedLock(String host, int port) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        this.jedisPool = new JedisPool(poolConfig, host, port);
    }
    
    public boolean acquire(String lockKey, String lockValue, int ttlSeconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            String result = jedis.set(lockKey, lockValue, "NX", "EX", ttlSeconds);
            boolean acquired = "OK".equals(result);
            
            if (acquired) {
                logger.debug("Acquired lock: {} with value: {}", lockKey, lockValue);
            } else {
                logger.debug("Failed to acquire lock: {}", lockKey);
            }
            
            return acquired;
        }
    }
    
    public boolean release(String lockKey, String lockValue) {
        try (Jedis jedis = jedisPool.getResource()) {
            Object result = jedis.eval(UNLOCK_SCRIPT, Collections.singletonList(lockKey), 
                                     Collections.singletonList(lockValue));
            boolean released = "1".equals(result.toString());
            
            if (released) {
                logger.debug("Released lock: {} with value: {}", lockKey, lockValue);
            } else {
                logger.debug("Failed to release lock (not owner): {}", lockKey);
            }
            
            return released;
        }
    }
    
    public boolean extendLock(String lockKey, String lockValue, int ttlSeconds) {
        String script = 
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('expire', KEYS[1], ARGV[2]) " +
            "else " +
            "return 0 " +
            "end";
        
        try (Jedis jedis = jedisPool.getResource()) {
            Object result = jedis.eval(script, Collections.singletonList(lockKey), 
                                     Arrays.asList(lockValue, String.valueOf(ttlSeconds)));
            boolean extended = "1".equals(result.toString());
            
            if (extended) {
                logger.debug("Extended lock: {} for {} seconds", lockKey, ttlSeconds);
            }
            
            return extended;
        }
    }
    
    public void close() {
        if (jedisPool != null) {
            jedisPool.close();
            logger.info("Redis distributed lock closed");
        }
    }
}

/**
 * Redis缓存提供者（用于Mem0配置）- Redis Cache Provider for Mem0
 */
class RedisCacheProvider {
    private static final Logger logger = LoggerFactory.getLogger(RedisCacheProvider.class);
    
    private final RedisCache cache;
    
    public RedisCacheProvider(String host, int port) {
        this.cache = new RedisCache(host, port);
    }
    
    public CompletableFuture<Void> cacheMemory(String key, com.mem0.core.Memory memory, int ttlSeconds) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> memoryData = Map.of(
                    "id", memory.getId(),
                    "content", memory.getContent(),
                    "userId", memory.getUserId(),
                    "timestamp", memory.getCreatedAt()
                );
                
                cache.setObject("mem0:memory:" + key, memoryData, ttlSeconds);
                logger.debug("Cached memory: {}", key);
            } catch (Exception e) {
                logger.error("Error caching memory", e);
            }
        });
    }
    
    public CompletableFuture<Optional<com.mem0.core.Memory>> getCachedMemory(String key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<Map<String, Object>> data = cache.getObject("mem0:memory:" + key, Map.class);
                
                if (data.isPresent()) {
                    Map<String, Object> memoryData = data.get();
                    com.mem0.core.Memory memory = new com.mem0.core.Memory(
                        (String) memoryData.get("id"),
                        (String) memoryData.get("content"),
                        (String) memoryData.get("userId")
                    );
                    
                    logger.debug("Retrieved cached memory: {}", key);
                    return Optional.of(memory);
                }
                
                return Optional.empty();
            } catch (Exception e) {
                logger.error("Error retrieving cached memory", e);
                return Optional.empty();
            }
        });
    }
    
    public CompletableFuture<Void> cacheSearchResults(String query, String userId, 
                                                     List<com.mem0.core.Memory> results, int ttlSeconds) {
        return CompletableFuture.runAsync(() -> {
            try {
                String cacheKey = "mem0:search:" + userId + ":" + query.hashCode();
                cache.setObject(cacheKey, results, ttlSeconds);
                logger.debug("Cached search results for query: {}", query);
            } catch (Exception e) {
                logger.error("Error caching search results", e);
            }
        });
    }
    
    public CompletableFuture<Optional<List<com.mem0.core.Memory>>> getCachedSearchResults(String query, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String cacheKey = "mem0:search:" + userId + ":" + query.hashCode();
                Optional<List> data = cache.getObject(cacheKey, List.class);
                
                if (data.isPresent()) {
                    // 注意：这里需要更复杂的反序列化逻辑来正确恢复Memory对象
                    logger.debug("Retrieved cached search results for query: {}", query);
                    // return Optional.of(convertToMemories(data.get()));
                }
                
                return Optional.empty();
            } catch (Exception e) {
                logger.error("Error retrieving cached search results", e);
                return Optional.empty();
            }
        });
    }
    
    public void close() {
        cache.close();
    }
}

/**
 * Redis集群支持 - Redis Cluster Support
 */
class RedisClusterCache {
    private static final Logger logger = LoggerFactory.getLogger(RedisClusterCache.class);
    
    private final redis.clients.jedis.JedisCluster jedisCluster;
    private final ObjectMapper objectMapper;
    
    public RedisClusterCache(Set<redis.clients.jedis.HostAndPort> nodes) {
        redis.clients.jedis.JedisPoolConfig poolConfig = new redis.clients.jedis.JedisPoolConfig();
        poolConfig.setMaxTotal(50);
        poolConfig.setMaxIdle(20);
        poolConfig.setMinIdle(5);
        
        this.jedisCluster = new redis.clients.jedis.JedisCluster(nodes, poolConfig);
        this.objectMapper = new ObjectMapper();
        
        logger.info("Connected to Redis cluster with {} nodes", nodes.size());
    }
    
    public void set(String key, String value, int ttlSeconds) {
        if (ttlSeconds > 0) {
            jedisCluster.setex(key, ttlSeconds, value);
        } else {
            jedisCluster.set(key, value);
        }
        logger.debug("Set cluster cache key: {} with TTL: {}", key, ttlSeconds);
    }
    
    public Optional<String> get(String key) {
        String value = jedisCluster.get(key);
        return Optional.ofNullable(value);
    }
    
    public <T> void setObject(String key, T object, int ttlSeconds) {
        try {
            String json = objectMapper.writeValueAsString(object);
            set(key, json, ttlSeconds);
        } catch (Exception e) {
            logger.error("Error setting object in cluster cache", e);
            throw new RuntimeException("Redis cluster object set failed", e);
        }
    }
    
    public <T> Optional<T> getObject(String key, Class<T> clazz) {
        try {
            Optional<String> json = get(key);
            if (json.isPresent()) {
                T object = objectMapper.readValue(json.get(), clazz);
                return Optional.of(object);
            }
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Error getting object from cluster cache", e);
            return Optional.empty();
        }
    }
    
    public void close() {
        try {
            jedisCluster.close();
            logger.info("Redis cluster connection closed");
        } catch (Exception e) {
            logger.error("Error closing Redis cluster", e);
        }
    }
}