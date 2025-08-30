package standalone;

import com.mem0.concurrency.cache.HighPerformanceCache;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

/**
 * 独立的缓存测试，不依赖其他组件
 */
public class SimpleCacheTest {
    
    private HighPerformanceCache<String, String> cache;
    
    @Before
    public void setUp() {
        cache = new HighPerformanceCache<>(100, 1000, 100);
    }
    
    @After
    public void tearDown() {
        if (cache != null) {
            cache.shutdown();
        }
    }
    
    @Test
    public void testBasicOperations() {
        cache.put("key1", "value1");
        assertEquals("value1", cache.get("key1"));
        assertTrue(cache.containsKey("key1"));
        assertEquals(1, cache.size());
        
        cache.remove("key1");
        assertNull(cache.get("key1"));
        assertFalse(cache.containsKey("key1"));
        assertEquals(0, cache.size());
    }
    
    @Test
    public void testClear() {
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        assertEquals(2, cache.size());
        
        cache.clear();
        assertEquals(0, cache.size());
        assertNull(cache.get("key1"));
    }
    
    @Test
    public void testStats() {
        cache.put("key1", "value1");
        cache.get("key1"); // hit
        cache.get("nonexistent"); // miss
        
        HighPerformanceCache.CacheStats stats = cache.getStats();
        assertNotNull(stats);
        assertEquals(1, stats.getCurrentSize());
        assertEquals(1, stats.getHitCount());
        assertEquals(1, stats.getMissCount());
        assertEquals(2, stats.getTotalRequests());
    }
}