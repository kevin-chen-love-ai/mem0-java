package com.mem0.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能监控器
 * Performance Monitor
 * 
 * 全面的性能监控系统，收集和报告Mem0系统的各种性能指标。该组件提供实时性能监控、
 * 历史数据分析和性能警报功能，是系统性能优化和故障诊断的核心工具。
 * 
 * Comprehensive performance monitoring system that collects and reports various performance
 * metrics of the Mem0 system. This component provides real-time performance monitoring,
 * historical data analysis, and performance alerting, serving as the core tool for
 * system performance optimization and troubleshooting.
 * 
 * 核心功能 / Key Features:
 * - JVM内存和线程监控 / JVM memory and thread monitoring
 * - 垃圾回收统计分析 / Garbage collection statistics analysis
 * - 自定义计数器和计时器 / Custom counters and timers
 * - 性能快照历史管理 / Performance snapshot history management
 * - 自动性能报告生成 / Automatic performance report generation
 * - 实时性能警报检测 / Real-time performance alert detection
 * 
 * 技术规格 / Technical Specifications:
 * - 默认监控间隔: 5秒 / Default monitoring interval: 5 seconds
 * - 最大快照保存: 100个 / Maximum snapshots retained: 100
 * - 支持JMX管理Bean / JMX Management Beans support
 * - 线程安全的指标收集 / Thread-safe metrics collection
 * - 自动化清理过期数据 / Automatic cleanup of expired data
 * 
 * 使用示例 / Usage Example:
 * <pre>
 * {@code
 * // 创建性能监控器 / Create performance monitor
 * PerformanceMonitor monitor = new PerformanceMonitor(5000, 100);
 * 
 * // 启动监控 / Start monitoring
 * monitor.startMonitoring();
 * 
 * // 增加计数器 / Increment counter
 * monitor.incrementCounter("search.requests");
 * monitor.incrementCounter("embedding.generations", 5);
 * 
 * // 记录执行时间 / Record execution time
 * monitor.recordTimer("vector.similarity", 150);
 * 
 * // 设置仪表盘指标 / Set gauge metric
 * monitor.setGauge("memory.usage.percentage", 75.5);
 * 
 * // 测量方法执行时间 / Measure method execution time
 * String result = monitor.measureTime("database.query", () -> {
 *     return performDatabaseQuery();
 * });
 * 
 * // 获取性能快照 / Get performance snapshot
 * PerformanceSnapshot snapshot = monitor.getLatestSnapshot();
 * 
 * // 生成性能报告 / Generate performance report
 * PerformanceReport report = monitor.generateReport();
 * System.out.println(report);
 * 
 * // 停止监控 / Stop monitoring
 * monitor.stopMonitoring();
 * monitor.shutdown();
 * }
 * </pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class PerformanceMonitor {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitor.class);
    
    // JVM监控Bean
    private final MemoryMXBean memoryBean;
    private final ThreadMXBean threadBean;
    private final RuntimeMXBean runtimeBean;
    private final GarbageCollectorMXBean[] gcBeans;
    
    // 自定义指标
    private final Map<String, AtomicLong> customCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> customTimers = new ConcurrentHashMap<>();
    private final Map<String, Double> customGauges = new ConcurrentHashMap<>();
    
    // 监控历史
    private final LinkedList<PerformanceSnapshot> snapshots = new LinkedList<>();
    private final int maxSnapshots;
    
    // 定时任务
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> monitoringTask;
    
    // 配置
    private final long monitoringInterval;
    private volatile boolean isMonitoring = false;
    
    public PerformanceMonitor() {
        this(5000, 100); // 默认5秒间隔，保留100个快照
    }
    
    public PerformanceMonitor(long intervalMillis, int maxSnapshots) {
        this.monitoringInterval = intervalMillis;
        this.maxSnapshots = maxSnapshots;
        
        // 初始化JVM监控Bean
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();
        this.runtimeBean = ManagementFactory.getRuntimeMXBean();
        this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans().toArray(new GarbageCollectorMXBean[0]);
        
        // 初始化调度器
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "mem0-performance-monitor");
            t.setDaemon(true);
            return t;
        });
        
        logger.info("性能监控器初始化完成 - 监控间隔: {}ms, 最大快照: {}", intervalMillis, maxSnapshots);
    }
    
    /**
     * 启动性能监控
     */
    public synchronized void startMonitoring() {
        if (isMonitoring) {
            return;
        }
        
        isMonitoring = true;
        monitoringTask = scheduler.scheduleAtFixedRate(
            this::collectMetrics,
            0,
            monitoringInterval,
            TimeUnit.MILLISECONDS
        );
        
        logger.info("性能监控已启动");
    }
    
    /**
     * 停止性能监控
     */
    public synchronized void stopMonitoring() {
        if (!isMonitoring) {
            return;
        }
        
        isMonitoring = false;
        if (monitoringTask != null) {
            monitoringTask.cancel(false);
            monitoringTask = null;
        }
        
        logger.info("性能监控已停止");
    }
    
    /**
     * 增加计数器
     */
    public void incrementCounter(String name) {
        customCounters.computeIfAbsent(name, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /**
     * 增加计数器（指定增量）
     */
    public void incrementCounter(String name, long delta) {
        customCounters.computeIfAbsent(name, k -> new AtomicLong(0)).addAndGet(delta);
    }
    
    /**
     * 记录执行时间
     */
    public void recordTimer(String name, long durationMillis) {
        customTimers.computeIfAbsent(name, k -> new AtomicLong(0)).addAndGet(durationMillis);
    }
    
    /**
     * 设置仪表盘指标
     */
    public void setGauge(String name, double value) {
        customGauges.put(name, value);
    }
    
    /**
     * 测量方法执行时间的便利方法
     */
    public <T> T measureTime(String timerName, Callable<T> callable) throws Exception {
        long startTime = System.currentTimeMillis();
        try {
            return callable.call();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            recordTimer(timerName, duration);
        }
    }
    
    /**
     * 获取最新的性能快照
     */
    public PerformanceSnapshot getLatestSnapshot() {
        synchronized (snapshots) {
            return snapshots.isEmpty() ? null : snapshots.getLast();
        }
    }
    
    /**
     * 获取所有性能快照
     */
    public List<PerformanceSnapshot> getAllSnapshots() {
        synchronized (snapshots) {
            return new ArrayList<>(snapshots);
        }
    }
    
    /**
     * 获取性能报告
     */
    public PerformanceReport generateReport() {
        List<PerformanceSnapshot> allSnapshots;
        synchronized (snapshots) {
            allSnapshots = new ArrayList<>(snapshots);
        }
        
        if (allSnapshots.isEmpty()) {
            return new PerformanceReport("无性能数据", Collections.emptyMap());
        }
        
        return analyzePerformance(allSnapshots);
    }
    
    /**
     * 清理历史快照
     */
    public void clearSnapshots() {
        synchronized (snapshots) {
            snapshots.clear();
        }
        logger.info("性能快照历史已清理");
    }
    
    /**
     * 关闭监控器
     */
    public void shutdown() {
        stopMonitoring();
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        clearSnapshots();
        logger.info("性能监控器已关闭");
    }
    
    // 私有方法
    
    private void collectMetrics() {
        try {
            PerformanceSnapshot snapshot = createSnapshot();
            
            synchronized (snapshots) {
                snapshots.addLast(snapshot);
                
                // 保持快照数量在限制内
                while (snapshots.size() > maxSnapshots) {
                    snapshots.removeFirst();
                }
            }
            
            // 检查警报条件
            checkAlerts(snapshot);
            
        } catch (Exception e) {
            logger.error("收集性能指标时出错", e);
        }
    }
    
    private PerformanceSnapshot createSnapshot() {
        long timestamp = System.currentTimeMillis();
        
        // JVM内存信息
        MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemory = memoryBean.getNonHeapMemoryUsage();
        
        // 线程信息
        int threadCount = threadBean.getThreadCount();
        int peakThreadCount = threadBean.getPeakThreadCount();
        int daemonThreadCount = threadBean.getDaemonThreadCount();
        
        // 垃圾回收信息
        Map<String, GCInfo> gcInfo = new HashMap<>();
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            String name = gcBean.getName();
            long collectionCount = gcBean.getCollectionCount();
            long collectionTime = gcBean.getCollectionTime();
            gcInfo.put(name, new GCInfo(collectionCount, collectionTime));
        }
        
        // 系统信息
        long uptime = runtimeBean.getUptime();
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        
        // 自定义指标快照
        Map<String, Long> counterSnapshot = new HashMap<>();
        customCounters.forEach((k, v) -> counterSnapshot.put(k, v.get()));
        
        Map<String, Long> timerSnapshot = new HashMap<>();
        customTimers.forEach((k, v) -> timerSnapshot.put(k, v.get()));
        
        Map<String, Double> gaugeSnapshot = new HashMap<>(customGauges);
        
        return new PerformanceSnapshot(
            timestamp,
            new MemoryInfo(heapMemory, nonHeapMemory),
            new ThreadInfo(threadCount, peakThreadCount, daemonThreadCount),
            gcInfo,
            new SystemInfo(uptime, availableProcessors),
            counterSnapshot,
            timerSnapshot,
            gaugeSnapshot
        );
    }
    
    private void checkAlerts(PerformanceSnapshot snapshot) {
        // 内存使用率告警
        double heapUsageRatio = (double) snapshot.memoryInfo.heapUsed / snapshot.memoryInfo.heapMax;
        if (heapUsageRatio > 0.9) {
            logger.warn("堆内存使用率过高: {:.1f}%", heapUsageRatio * 100);
        }
        
        // 线程数告警
        if (snapshot.threadInfo.threadCount > 500) {
            logger.warn("线程数过多: {}", snapshot.threadInfo.threadCount);
        }
        
        // GC时间占比告警
        for (Map.Entry<String, GCInfo> entry : snapshot.gcInfo.entrySet()) {
            double gcTimeRatio = (double) entry.getValue().collectionTime / snapshot.systemInfo.uptime;
            if (gcTimeRatio > 0.1) { // GC时间占比超过10%
                logger.warn("GC时间占比过高: {} - {:.2f}%", entry.getKey(), gcTimeRatio * 100);
            }
        }
    }
    
    private PerformanceReport analyzePerformance(List<PerformanceSnapshot> snapshots) {
        if (snapshots.size() < 2) {
            return new PerformanceReport("数据不足，无法分析", Collections.emptyMap());
        }
        
        PerformanceSnapshot first = snapshots.get(0);
        PerformanceSnapshot last = snapshots.get(snapshots.size() - 1);
        long timespan = last.timestamp - first.timestamp;
        
        Map<String, String> analysis = new HashMap<>();
        
        // 内存趋势分析
        long memoryIncrease = last.memoryInfo.heapUsed - first.memoryInfo.heapUsed;
        analysis.put("内存趋势", 
            String.format("堆内存使用从 %s 变化到 %s，增长 %s",
                formatBytes(first.memoryInfo.heapUsed),
                formatBytes(last.memoryInfo.heapUsed),
                formatBytes(memoryIncrease)));
        
        // 线程趋势分析
        int threadIncrease = last.threadInfo.threadCount - first.threadInfo.threadCount;
        analysis.put("线程趋势",
            String.format("线程数从 %d 变化到 %d，变化 %+d",
                first.threadInfo.threadCount,
                last.threadInfo.threadCount,
                threadIncrease));
        
        // GC分析
        StringBuilder gcAnalysis = new StringBuilder();
        for (String gcName : first.gcInfo.keySet()) {
            GCInfo firstGC = first.gcInfo.get(gcName);
            GCInfo lastGC = last.gcInfo.get(gcName);
            
            if (firstGC != null && lastGC != null) {
                long gcCountIncrease = lastGC.collectionCount - firstGC.collectionCount;
                long gcTimeIncrease = lastGC.collectionTime - firstGC.collectionTime;
                
                if (gcAnalysis.length() > 0) gcAnalysis.append("; ");
                gcAnalysis.append(String.format("%s: %d次GC，耗时%dms",
                    gcName, gcCountIncrease, gcTimeIncrease));
            }
        }
        analysis.put("GC活动", gcAnalysis.toString());
        
        // 自定义指标分析
        if (!last.customCounters.isEmpty()) {
            StringBuilder counterAnalysis = new StringBuilder();
            for (Map.Entry<String, Long> entry : last.customCounters.entrySet()) {
                String name = entry.getKey();
                long currentValue = entry.getValue();
                long previousValue = first.customCounters.getOrDefault(name, 0L);
                long increase = currentValue - previousValue;
                
                if (counterAnalysis.length() > 0) counterAnalysis.append(", ");
                counterAnalysis.append(String.format("%s: %d (+%d)", name, currentValue, increase));
            }
            analysis.put("计数器", counterAnalysis.toString());
        }
        
        return new PerformanceReport(
            String.format("性能分析报告 (时间跨度: %.1f秒)", timespan / 1000.0),
            analysis
        );
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return String.format("%.1fKB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1fMB", bytes / (1024.0 * 1024));
        return String.format("%.1fGB", bytes / (1024.0 * 1024 * 1024));
    }
    
    // 数据类
    
    public static class PerformanceSnapshot {
        public final long timestamp;
        public final MemoryInfo memoryInfo;
        public final ThreadInfo threadInfo;
        public final Map<String, GCInfo> gcInfo;
        public final SystemInfo systemInfo;
        public final Map<String, Long> customCounters;
        public final Map<String, Long> customTimers;
        public final Map<String, Double> customGauges;
        
        public PerformanceSnapshot(long timestamp, MemoryInfo memoryInfo, ThreadInfo threadInfo,
                                 Map<String, GCInfo> gcInfo, SystemInfo systemInfo,
                                 Map<String, Long> customCounters, Map<String, Long> customTimers,
                                 Map<String, Double> customGauges) {
            this.timestamp = timestamp;
            this.memoryInfo = memoryInfo;
            this.threadInfo = threadInfo;
            this.gcInfo = gcInfo;
            this.systemInfo = systemInfo;
            this.customCounters = customCounters;
            this.customTimers = customTimers;
            this.customGauges = customGauges;
        }
    }
    
    public static class MemoryInfo {
        public final long heapUsed;
        public final long heapMax;
        public final long heapCommitted;
        public final long nonHeapUsed;
        public final long nonHeapMax;
        public final long nonHeapCommitted;
        
        public MemoryInfo(MemoryUsage heapMemory, MemoryUsage nonHeapMemory) {
            this.heapUsed = heapMemory.getUsed();
            this.heapMax = heapMemory.getMax();
            this.heapCommitted = heapMemory.getCommitted();
            this.nonHeapUsed = nonHeapMemory.getUsed();
            this.nonHeapMax = nonHeapMemory.getMax();
            this.nonHeapCommitted = nonHeapMemory.getCommitted();
        }
    }
    
    public static class ThreadInfo {
        public final int threadCount;
        public final int peakThreadCount;
        public final int daemonThreadCount;
        
        public ThreadInfo(int threadCount, int peakThreadCount, int daemonThreadCount) {
            this.threadCount = threadCount;
            this.peakThreadCount = peakThreadCount;
            this.daemonThreadCount = daemonThreadCount;
        }
    }
    
    public static class GCInfo {
        public final long collectionCount;
        public final long collectionTime;
        
        public GCInfo(long collectionCount, long collectionTime) {
            this.collectionCount = collectionCount;
            this.collectionTime = collectionTime;
        }
    }
    
    public static class SystemInfo {
        public final long uptime;
        public final int availableProcessors;
        
        public SystemInfo(long uptime, int availableProcessors) {
            this.uptime = uptime;
            this.availableProcessors = availableProcessors;
        }
    }
    
    public static class PerformanceReport {
        public final String summary;
        public final Map<String, String> details;
        public final long generatedAt;
        
        public PerformanceReport(String summary, Map<String, String> details) {
            this.summary = summary;
            this.details = details;
            this.generatedAt = System.currentTimeMillis();
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== 性能报告 ===\n");
            sb.append("生成时间: ").append(new Date(generatedAt)).append("\n");
            sb.append("摘要: ").append(summary).append("\n\n");
            
            details.forEach((key, value) -> 
                sb.append(key).append(": ").append(value).append("\n"));
            
            return sb.toString();
        }
    }
}