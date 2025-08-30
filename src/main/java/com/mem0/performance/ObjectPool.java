package com.mem0.performance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * 高性能对象池实现
 * 用于减少频繁对象创建和GC压力
 * @param <T> 池化对象类型
 */
public class ObjectPool<T> {
    
    private static final Logger logger = LoggerFactory.getLogger(ObjectPool.class);
    
    private final ConcurrentLinkedQueue<T> pool = new ConcurrentLinkedQueue<>();
    private final Supplier<T> factory;
    private final ObjectResetter<T> resetter;
    private final int maxSize;
    
    // 统计信息
    private final AtomicInteger totalCreated = new AtomicInteger(0);
    private final AtomicInteger totalReused = new AtomicInteger(0);
    private final AtomicInteger currentSize = new AtomicInteger(0);
    
    /**
     * 对象重置接口
     * @param <T>
     */
    public interface ObjectResetter<T> {
        void reset(T obj);
    }
    
    public ObjectPool(Supplier<T> factory, ObjectResetter<T> resetter, int maxSize) {
        this.factory = factory;
        this.resetter = resetter;
        this.maxSize = maxSize;
        
        logger.debug("对象池初始化完成，最大大小: {}", maxSize);
    }
    
    /**
     * 从池中获取对象
     */
    public T acquire() {
        T obj = pool.poll();
        
        if (obj != null) {
            currentSize.decrementAndGet();
            totalReused.incrementAndGet();
            return obj;
        }
        
        // 池中没有可用对象，创建新的
        obj = factory.get();
        totalCreated.incrementAndGet();
        
        logger.debug("创建新对象，总创建数: {}", totalCreated.get());
        return obj;
    }
    
    /**
     * 将对象返回到池中
     */
    public void release(T obj) {
        if (obj == null) {
            return;
        }
        
        // 重置对象状态
        if (resetter != null) {
            try {
                resetter.reset(obj);
            } catch (Exception e) {
                logger.warn("对象重置失败，不返回池中", e);
                return;
            }
        }
        
        // 如果池未满，则返回到池中
        if (currentSize.get() < maxSize) {
            pool.offer(obj);
            currentSize.incrementAndGet();
        }
        // 否则直接丢弃，让GC回收
    }
    
    /**
     * 清空对象池
     */
    public void clear() {
        pool.clear();
        currentSize.set(0);
        logger.debug("对象池已清空");
    }
    
    /**
     * 预填充对象池
     */
    public void preFill(int count) {
        count = Math.min(count, maxSize);
        
        logger.info("开始预填充对象池，目标数量: {}", count);
        
        for (int i = 0; i < count; i++) {
            T obj = factory.get();
            pool.offer(obj);
            currentSize.incrementAndGet();
            totalCreated.incrementAndGet();
        }
        
        logger.info("对象池预填充完成，当前大小: {}", currentSize.get());
    }
    
    /**
     * 获取池统计信息
     */
    public PoolStats getStats() {
        return new PoolStats(
            totalCreated.get(),
            totalReused.get(),
            currentSize.get(),
            maxSize,
            calculateReuseRate()
        );
    }
    
    private double calculateReuseRate() {
        int created = totalCreated.get();
        int reused = totalReused.get();
        int total = created + reused;
        
        return total == 0 ? 0.0 : (double) reused / total;
    }
    
    /**
     * 池统计信息
     */
    public static class PoolStats {
        private final int totalCreated;
        private final int totalReused;
        private final int currentSize;
        private final int maxSize;
        private final double reuseRate;
        
        public PoolStats(int totalCreated, int totalReused, int currentSize, int maxSize, double reuseRate) {
            this.totalCreated = totalCreated;
            this.totalReused = totalReused;
            this.currentSize = currentSize;
            this.maxSize = maxSize;
            this.reuseRate = reuseRate;
        }
        
        public int getTotalCreated() { return totalCreated; }
        public int getTotalReused() { return totalReused; }
        public int getCurrentSize() { return currentSize; }
        public int getMaxSize() { return maxSize; }
        public double getReuseRate() { return reuseRate; }
        
        @Override
        public String toString() {
            return String.format("PoolStats{创建=%d, 复用=%d, 当前大小=%d/%d, 复用率=%.2f%%}",
                totalCreated, totalReused, currentSize, maxSize, reuseRate * 100);
        }
    }
}

/**
 * 专用对象池管理器
 */
class PoolManager {
    
    private static final Logger logger = LoggerFactory.getLogger(PoolManager.class);
    
    // 常用对象池
    private static final ObjectPool<StringBuilder> STRING_BUILDER_POOL = 
        new ObjectPool<>(StringBuilder::new, sb -> sb.setLength(0), 100);
        
    private static final ObjectPool<java.util.ArrayList<?>> ARRAY_LIST_POOL = 
        new ObjectPool<>(java.util.ArrayList::new, list -> ((java.util.ArrayList) list).clear(), 200);
        
    private static final ObjectPool<java.util.HashMap<?, ?>> HASH_MAP_POOL = 
        new ObjectPool<>(java.util.HashMap::new, map -> ((java.util.HashMap) map).clear(), 150);
    
    static {
        // 预填充常用对象池
        STRING_BUILDER_POOL.preFill(20);
        ARRAY_LIST_POOL.preFill(30);
        HASH_MAP_POOL.preFill(25);
        
        logger.info("对象池管理器初始化完成");
    }
    
    public static StringBuilder acquireStringBuilder() {
        return STRING_BUILDER_POOL.acquire();
    }
    
    public static void releaseStringBuilder(StringBuilder sb) {
        STRING_BUILDER_POOL.release(sb);
    }
    
    @SuppressWarnings("unchecked")
    public static <T> java.util.ArrayList<T> acquireArrayList() {
        return (java.util.ArrayList<T>) ARRAY_LIST_POOL.acquire();
    }
    
    public static void releaseArrayList(java.util.ArrayList<?> list) {
        ARRAY_LIST_POOL.release(list);
    }
    
    @SuppressWarnings("unchecked")
    public static <K, V> java.util.HashMap<K, V> acquireHashMap() {
        return (java.util.HashMap<K, V>) HASH_MAP_POOL.acquire();
    }
    
    public static void releaseHashMap(java.util.HashMap<?, ?> map) {
        HASH_MAP_POOL.release(map);
    }
    
    /**
     * 获取所有池的统计信息
     */
    public static String getAllPoolStats() {
        return String.format("StringBuilder: %s, ArrayList: %s, HashMap: %s",
            STRING_BUILDER_POOL.getStats(),
            ARRAY_LIST_POOL.getStats(),
            HASH_MAP_POOL.getStats());
    }
}