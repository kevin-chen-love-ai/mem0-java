package com.mem0.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mem0.core.EnhancedMemory;
import com.mem0.model.MemoryShardStats;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * 内存分片
 * 管理特定分片中的内存数据，提供线程安全的CRUD操作
 */
public class MemoryShard {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryShard.class);
    
    private final int shardId;
    
    // 内存存储
    private final Map<String, EnhancedMemory> memories = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> userMemoryIndex = new ConcurrentHashMap<>();
    
    // 访问统计
    private final AtomicInteger memoryCount = new AtomicInteger(0);
    private final AtomicLong totalAccesses = new AtomicLong(0);
    private final AtomicLong lastAccessTime = new AtomicLong(System.currentTimeMillis());
    
    // 读写锁保护
    private final ReadWriteLock shardLock = new ReentrantReadWriteLock();
    
    public MemoryShard(int shardId) {
        this.shardId = shardId;
        logger.debug("内存分片初始化完成: {}", shardId);
    }

    /**
     * 添加内存到分片
     */
    public void addMemory(EnhancedMemory memory) {
        if (memory == null) {
            return;
        }
        
        shardLock.writeLock().lock();
        try {
            String memoryId = memory.getId();
            String userId = memory.getUserId();
            
            // 添加到主存储
            EnhancedMemory existingMemory = memories.put(memoryId, memory);
            
            // 更新用户索引
            userMemoryIndex.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(memoryId);
            
            // 更新统计
            if (existingMemory == null) {
                memoryCount.incrementAndGet();
            }
            totalAccesses.incrementAndGet();
            lastAccessTime.set(System.currentTimeMillis());
            
            logger.debug("内存添加到分片: {} - 内存ID: {}, 用户: {}", shardId, memoryId, userId);
            
        } finally {
            shardLock.writeLock().unlock();
        }
    }

    /**
     * 从分片获取内存
     */
    public EnhancedMemory getMemory(String memoryId) {
        if (memoryId == null) {
            return null;
        }
        
        shardLock.readLock().lock();
        try {
            EnhancedMemory memory = memories.get(memoryId);
            
            if (memory != null) {
                // 更新访问时间
                memory.updateAccessTime();
                totalAccesses.incrementAndGet();
                lastAccessTime.set(System.currentTimeMillis());
                
                logger.debug("从分片获取内存: {} - 内存ID: {}", shardId, memoryId);
            }
            
            return memory;
            
        } finally {
            shardLock.readLock().unlock();
        }
    }

    /**
     * 从分片移除内存
     */
    public EnhancedMemory removeMemory(String memoryId) {
        if (memoryId == null) {
            return null;
        }
        
        shardLock.writeLock().lock();
        try {
            EnhancedMemory removedMemory = memories.remove(memoryId);
            
            if (removedMemory != null) {
                String userId = removedMemory.getUserId();
                
                // 从用户索引中移除
                Set<String> userMemories = userMemoryIndex.get(userId);
                if (userMemories != null) {
                    userMemories.remove(memoryId);
                    
                    // 如果用户没有其他内存，移除用户索引
                    if (userMemories.isEmpty()) {
                        userMemoryIndex.remove(userId);
                    }
                }
                
                memoryCount.decrementAndGet();
                lastAccessTime.set(System.currentTimeMillis());
                
                logger.debug("从分片移除内存: {} - 内存ID: {}, 用户: {}", shardId, memoryId, userId);
            }
            
            return removedMemory;
            
        } finally {
            shardLock.writeLock().unlock();
        }
    }

    /**
     * 获取用户的所有内存
     */
    public List<EnhancedMemory> getUserMemories(String userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        
        shardLock.readLock().lock();
        try {
            Set<String> memoryIds = userMemoryIndex.get(userId);
            if (memoryIds == null || memoryIds.isEmpty()) {
                return Collections.emptyList();
            }
            
            List<EnhancedMemory> userMemories = new ArrayList<>();
            for (String memoryId : memoryIds) {
                EnhancedMemory memory = memories.get(memoryId);
                if (memory != null) {
                    memory.updateAccessTime();
                    userMemories.add(memory);
                }
            }
            
            totalAccesses.addAndGet(userMemories.size());
            lastAccessTime.set(System.currentTimeMillis());
            
            logger.debug("获取用户内存: 分片={}, 用户={}, 数量={}", shardId, userId, userMemories.size());
            
            return userMemories;
            
        } finally {
            shardLock.readLock().unlock();
        }
    }

    /**
     * 获取所有内存
     */
    public List<EnhancedMemory> getAllMemories() {
        shardLock.readLock().lock();
        try {
            List<EnhancedMemory> allMemories = new ArrayList<>(memories.values());
            
            totalAccesses.addAndGet(allMemories.size());
            lastAccessTime.set(System.currentTimeMillis());
            
            logger.debug("获取分片所有内存: {} - 数量: {}", shardId, allMemories.size());
            
            return allMemories;
            
        } finally {
            shardLock.readLock().unlock();
        }
    }

    /**
     * 检查内存是否存在
     */
    public boolean containsMemory(String memoryId) {
        if (memoryId == null) {
            return false;
        }
        
        shardLock.readLock().lock();
        try {
            return memories.containsKey(memoryId);
        } finally {
            shardLock.readLock().unlock();
        }
    }

    /**
     * 获取分片中的内存数量
     */
    public int getMemoryCount() {
        return memoryCount.get();
    }

    /**
     * 获取分片中的用户数量
     */
    public int getUserCount() {
        shardLock.readLock().lock();
        try {
            return userMemoryIndex.size();
        } finally {
            shardLock.readLock().unlock();
        }
    }

    /**
     * 清理过期内存
     */
    public int cleanupExpiredMemories(long ttlMs) {
        if (ttlMs <= 0) {
            return 0;
        }
        
        shardLock.writeLock().lock();
        try {
            long currentTime = System.currentTimeMillis();
            List<String> expiredIds = new ArrayList<>();
            
            // 找出过期的内存
            for (Map.Entry<String, EnhancedMemory> entry : memories.entrySet()) {
                EnhancedMemory memory = entry.getValue();
                if (memory.isExpired(ttlMs)) {
                    expiredIds.add(entry.getKey());
                }
            }
            
            // 移除过期内存
            int cleanedCount = 0;
            for (String memoryId : expiredIds) {
                EnhancedMemory removed = removeMemoryInternal(memoryId);
                if (removed != null) {
                    cleanedCount++;
                }
            }
            
            if (cleanedCount > 0) {
                logger.info("分片清理过期内存: {} - 清理数量: {}, 剩余数量: {}", 
                           shardId, cleanedCount, memoryCount.get());
            }
            
            return cleanedCount;
            
        } finally {
            shardLock.writeLock().unlock();
        }
    }

    /**
     * 按条件搜索内存
     */
    public List<EnhancedMemory> searchMemories(MemorySearchCriteria criteria) {
        if (criteria == null) {
            return Collections.emptyList();
        }
        
        shardLock.readLock().lock();
        try {
            List<EnhancedMemory> results = memories.values().stream()
                .filter(memory -> matchesCriteria(memory, criteria))
                .limit(criteria.getLimit())
                .collect(Collectors.toList());
            
            totalAccesses.addAndGet(results.size());
            lastAccessTime.set(System.currentTimeMillis());
            
            logger.debug("分片内存搜索: {} - 条件: {}, 结果数: {}", shardId, criteria, results.size());
            
            return results;
            
        } finally {
            shardLock.readLock().unlock();
        }
    }

    /**
     * 清空分片
     */
    public void clear() {
        shardLock.writeLock().lock();
        try {
            int removedCount = memoryCount.get();
            
            memories.clear();
            userMemoryIndex.clear();
            memoryCount.set(0);
            
            logger.info("分片清空完成: {} - 移除内存数: {}", shardId, removedCount);
            
        } finally {
            shardLock.writeLock().unlock();
        }
    }

    /**
     * 获取分片统计信息
     */
    public MemoryShardStats getStats() {
        shardLock.readLock().lock();
        try {
            return new MemoryShardStats(
                shardId,
                memoryCount.get(),
                userMemoryIndex.size(),
                totalAccesses.get(),
                lastAccessTime.get(),
                calculateAverageMemoryAge(),
                calculateMemoryDistribution()
            );
        } finally {
            shardLock.readLock().unlock();
        }
    }

    /**
     * 获取分片ID
     */
    public int getId() {
        return shardId;
    }

    // 私有辅助方法

    private EnhancedMemory removeMemoryInternal(String memoryId) {
        EnhancedMemory removedMemory = memories.remove(memoryId);
        
        if (removedMemory != null) {
            String userId = removedMemory.getUserId();
            
            // 从用户索引中移除
            Set<String> userMemories = userMemoryIndex.get(userId);
            if (userMemories != null) {
                userMemories.remove(memoryId);
                
                if (userMemories.isEmpty()) {
                    userMemoryIndex.remove(userId);
                }
            }
            
            memoryCount.decrementAndGet();
        }
        
        return removedMemory;
    }

    private boolean matchesCriteria(EnhancedMemory memory, MemorySearchCriteria criteria) {
        // 用户ID过滤
        if (criteria.getUserId() != null && !criteria.getUserId().equals(memory.getUserId())) {
            return false;
        }
        
        // 内容关键词过滤
        if (criteria.getContentKeywords() != null && !criteria.getContentKeywords().isEmpty()) {
            String content = memory.getContent().toLowerCase();
            boolean matchesAnyKeyword = criteria.getContentKeywords().stream()
                .anyMatch(keyword -> content.contains(keyword.toLowerCase()));
            
            if (!matchesAnyKeyword) {
                return false;
            }
        }
        
        // 时间范围过滤
        long memoryTime = memory.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        if (criteria.getStartTime() > 0 && memoryTime < criteria.getStartTime()) {
            return false;
        }
        
        if (criteria.getEndTime() > 0 && memoryTime > criteria.getEndTime()) {
            return false;
        }
        
        // 元数据过滤
        if (criteria.getMetadataFilters() != null && !criteria.getMetadataFilters().isEmpty()) {
            Map<String, Object> memoryMetadata = memory.getMetadata();
            
            for (Map.Entry<String, Object> filter : criteria.getMetadataFilters().entrySet()) {
                Object memoryValue = memoryMetadata.get(filter.getKey());
                if (!Objects.equals(memoryValue, filter.getValue())) {
                    return false;
                }
            }
        }
        
        return true;
    }

    private long calculateAverageMemoryAge() {
        if (memories.isEmpty()) {
            return 0;
        }
        
        long currentTime = System.currentTimeMillis();
        long totalAge = 0;
        
        for (EnhancedMemory memory : memories.values()) {
            totalAge += currentTime - memory.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        
        return totalAge / memories.size();
    }

    private Map<String, Integer> calculateMemoryDistribution() {
        Map<String, Integer> distribution = new HashMap<>();
        
        for (Set<String> userMemories : userMemoryIndex.values()) {
            int count = userMemories.size();
            String range = getCountRange(count);
            distribution.merge(range, 1, Integer::sum);
        }
        
        return distribution;
    }

    private String getCountRange(int count) {
        if (count == 1) return "1";
        if (count <= 5) return "2-5";
        if (count <= 10) return "6-10";
        if (count <= 50) return "11-50";
        if (count <= 100) return "51-100";
        return "100+";
    }

    // 内部类

    public static class MemorySearchCriteria {
        private String userId;
        private List<String> contentKeywords;
        private long startTime;
        private long endTime;
        private Map<String, Object> metadataFilters;
        private int limit = 100;

        public MemorySearchCriteria() {}

        // Builder模式
        public MemorySearchCriteria userId(String userId) {
            this.userId = userId;
            return this;
        }

        public MemorySearchCriteria contentKeywords(List<String> keywords) {
            this.contentKeywords = keywords;
            return this;
        }

        public MemorySearchCriteria timeRange(long startTime, long endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
            return this;
        }

        public MemorySearchCriteria metadataFilters(Map<String, Object> filters) {
            this.metadataFilters = filters;
            return this;
        }

        public MemorySearchCriteria limit(int limit) {
            this.limit = limit;
            return this;
        }

        // Getter方法
        public String getUserId() { return userId; }
        public List<String> getContentKeywords() { return contentKeywords; }
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public Map<String, Object> getMetadataFilters() { return metadataFilters; }
        public int getLimit() { return limit; }

        @Override
        public String toString() {
            return String.format("MemorySearchCriteria{用户=%s, 关键词=%s, 时间范围=%d-%d, 元数据过滤=%s, 限制=%d}",
                userId, contentKeywords, startTime, endTime, metadataFilters, limit);
        }
    }

    public static class MemoryShardStats {
        private final int shardId;
        private final int memoryCount;
        private final int userCount;
        private final long totalAccesses;
        private final long lastAccessTime;
        private final long averageMemoryAge;
        private final Map<String, Integer> memoryDistribution;

        public MemoryShardStats(int shardId, int memoryCount, int userCount, long totalAccesses,
                               long lastAccessTime, long averageMemoryAge, Map<String, Integer> memoryDistribution) {
            this.shardId = shardId;
            this.memoryCount = memoryCount;
            this.userCount = userCount;
            this.totalAccesses = totalAccesses;
            this.lastAccessTime = lastAccessTime;
            this.averageMemoryAge = averageMemoryAge;
            this.memoryDistribution = new HashMap<>(memoryDistribution);
        }

        // Getter方法
        public int getShardId() { return shardId; }
        public int getMemoryCount() { return memoryCount; }
        public int getUserCount() { return userCount; }
        public long getTotalAccesses() { return totalAccesses; }
        public long getLastAccessTime() { return lastAccessTime; }
        public long getAverageMemoryAge() { return averageMemoryAge; }
        public Map<String, Integer> getMemoryDistribution() { return Collections.unmodifiableMap(memoryDistribution); }

        public double getAverageMemoriesPerUser() {
            return userCount == 0 ? 0.0 : (double) memoryCount / userCount;
        }

        @Override
        public String toString() {
            return String.format("MemoryShardStats{分片=%d, 内存数=%d, 用户数=%d, 访问数=%d, 平均年龄=%dms, 平均每用户=%.1f}",
                shardId, memoryCount, userCount, totalAccesses, averageMemoryAge, getAverageMemoriesPerUser());
        }
    }
}