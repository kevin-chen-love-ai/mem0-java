package com.mem0.performance.benchmark;

import com.mem0.util.Java8Utils;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基准测试报告
 * 汇总所有测试结果并生成报告
 */
public class BenchmarkReport {
    
    private final List<BenchmarkResult> results;
    private final long totalDurationMs;
    private final BenchmarkConfig config;
    private final long generatedAt;
    private final String reportId;

    public BenchmarkReport(List<BenchmarkResult> results, long totalDurationMs, BenchmarkConfig config) {
        this.results = new ArrayList<>(results);
        this.totalDurationMs = totalDurationMs;
        this.config = config;
        this.generatedAt = System.currentTimeMillis();
        this.reportId = "benchmark_" + generatedAt;
    }

    /**
     * 生成文本报告
     */
    public String generateTextReport() {
        StringBuilder report = new StringBuilder();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        // 报告头部
        report.append(Java8Utils.repeat("=", 80)).append("\n");
        report.append("            Mem0 Java 性能基准测试报告\n");
        report.append(Java8Utils.repeat("=", 80)).append("\n");
        report.append(String.format("报告ID: %s\n", reportId));
        report.append(String.format("生成时间: %s\n", dateFormat.format(new Date(generatedAt))));
        report.append(String.format("总测试时间: %.2f秒\n", totalDurationMs / 1000.0));
        report.append(String.format("测试配置: %s\n", config));
        report.append("\n");

        // 执行摘要
        report.append("执行摘要\n");
        report.append(Java8Utils.repeat("-", 40)).append("\n");
        report.append(String.format("总测试数: %d\n", results.size()));
        report.append(String.format("成功测试: %d\n", (int) results.stream().filter(BenchmarkResult::isSuccessful).count()));
        report.append(String.format("失败测试: %d\n", (int) results.stream().filter(r -> !r.isSuccessful()).count()));
        report.append("\n");

        // 按类型分组的结果
        Map<BenchmarkResult.TestType, List<BenchmarkResult>> resultsByType = results.stream()
            .collect(Collectors.groupingBy(BenchmarkResult::getTestType));

        // 延迟测试结果
        if (resultsByType.containsKey(BenchmarkResult.TestType.LATENCY) || 
            resultsByType.containsKey(BenchmarkResult.TestType.LATENCY_PERCENTILE)) {
            
            report.append("延迟测试结果\n");
            report.append(Java8Utils.repeat("-", 40)).append("\n");
            
            List<BenchmarkResult> latencyResults = new ArrayList<>();
            latencyResults.addAll(resultsByType.getOrDefault(BenchmarkResult.TestType.LATENCY, Collections.emptyList()));
            latencyResults.addAll(resultsByType.getOrDefault(BenchmarkResult.TestType.LATENCY_PERCENTILE, Collections.emptyList()));
            
            for (BenchmarkResult result : latencyResults) {
                BenchmarkResult.BenchmarkStatistics stats = result.getStatistics();
                report.append(String.format("  %s:\n", result.getTestName()));
                report.append(String.format("    平均延迟: %.2fms\n", stats.getMean()));
                report.append(String.format("    中位数: %dms\n", stats.getMedian()));
                report.append(String.format("    P95: %dms, P99: %dms, P99.9: %dms\n", stats.getP95(), stats.getP99(), stats.getP999()));
                report.append(String.format("    最小值: %dms, 最大值: %dms\n", stats.getMin(), stats.getMax()));
                report.append(String.format("    样本数: %d\n", stats.getSampleCount()));
                report.append("\n");
            }
        }

        // 吞吐量测试结果
        List<BenchmarkResult> throughputResults = new ArrayList<>();
        throughputResults.addAll(resultsByType.getOrDefault(BenchmarkResult.TestType.THROUGHPUT, Collections.emptyList()));
        throughputResults.addAll(resultsByType.getOrDefault(BenchmarkResult.TestType.SUSTAINED_THROUGHPUT, Collections.emptyList()));
        
        if (!throughputResults.isEmpty()) {
            report.append("吞吐量测试结果\n");
            report.append(Java8Utils.repeat("-", 40)).append("\n");
            
            for (BenchmarkResult result : throughputResults) {
                report.append(String.format("  %s:\n", result.getTestName()));
                report.append(String.format("    吞吐量: %.2f 操作/秒\n", result.getThroughput()));
                report.append(String.format("    总操作数: %d\n", result.getRawData().size()));
                report.append(String.format("    总时间: %.2f秒\n", result.getTotalTimeMs() / 1000.0));
                report.append("\n");
            }
        }

        // 并发测试结果
        if (resultsByType.containsKey(BenchmarkResult.TestType.CONCURRENCY)) {
            report.append("并发测试结果\n");
            report.append(Java8Utils.repeat("-", 40)).append("\n");
            
            List<BenchmarkResult> concurrencyResults = resultsByType.get(BenchmarkResult.TestType.CONCURRENCY);
            
            // 按并发级别排序
            concurrencyResults.sort(Comparator.comparingInt(BenchmarkResult::getConcurrencyLevel));
            
            for (BenchmarkResult result : concurrencyResults) {
                BenchmarkResult.BenchmarkStatistics stats = result.getStatistics();
                report.append(String.format("  %s (并发度: %d):\n", result.getTestName(), result.getConcurrencyLevel()));
                report.append(String.format("    吞吐量: %.2f 操作/秒\n", result.getThroughput()));
                report.append(String.format("    平均延迟: %.2fms\n", stats.getMean()));
                report.append(String.format("    P95延迟: %dms\n", stats.getP95()));
                report.append(String.format("    成功操作数: %d\n", stats.getSampleCount()));
                report.append("\n");
            }
        }

        // 内存测试结果
        List<BenchmarkResult> memoryResults = new ArrayList<>();
        memoryResults.addAll(resultsByType.getOrDefault(BenchmarkResult.TestType.MEMORY_STRESS, Collections.emptyList()));
        memoryResults.addAll(resultsByType.getOrDefault(BenchmarkResult.TestType.MEMORY_LEAK, Collections.emptyList()));
        
        if (!memoryResults.isEmpty()) {
            report.append("内存测试结果\n");
            report.append(Java8Utils.repeat("-", 40)).append("\n");
            
            for (BenchmarkResult result : memoryResults) {
                List<Long> memoryUsage = result.getRawData();
                if (!memoryUsage.isEmpty()) {
                    long initialMemory = memoryUsage.get(0);
                    long finalMemory = memoryUsage.get(memoryUsage.size() - 1);
                    long peakMemory = memoryUsage.stream().mapToLong(Long::longValue).max().orElse(0);
                    
                    report.append(String.format("  %s:\n", result.getTestName()));
                    report.append(String.format("    初始内存使用: %.2f MB\n", initialMemory / (1024.0 * 1024.0)));
                    report.append(String.format("    最终内存使用: %.2f MB\n", finalMemory / (1024.0 * 1024.0)));
                    report.append(String.format("    峰值内存使用: %.2f MB\n", peakMemory / (1024.0 * 1024.0)));
                    report.append(String.format("    内存增长: %.2f MB\n", (finalMemory - initialMemory) / (1024.0 * 1024.0)));
                    
                    if (result.getTestType() == BenchmarkResult.TestType.MEMORY_LEAK) {
                        // 检查内存泄漏趋势
                        boolean hasLeak = analyzeMemoryLeak(memoryUsage);
                        report.append(String.format("    内存泄漏检测: %s\n", hasLeak ? "疑似泄漏" : "正常"));
                    }
                    report.append("\n");
                }
            }
        }

        // 缓存测试结果
        List<BenchmarkResult> cacheResults = new ArrayList<>();
        cacheResults.addAll(resultsByType.getOrDefault(BenchmarkResult.TestType.CACHE_PERFORMANCE, Collections.emptyList()));
        cacheResults.addAll(resultsByType.getOrDefault(BenchmarkResult.TestType.CACHE_WARMUP, Collections.emptyList()));
        
        if (!cacheResults.isEmpty()) {
            report.append("缓存测试结果\n");
            report.append(Java8Utils.repeat("-", 40)).append("\n");
            
            for (BenchmarkResult result : cacheResults) {
                report.append(String.format("  %s:\n", result.getTestName()));
                
                if (result.getTestType() == BenchmarkResult.TestType.CACHE_PERFORMANCE) {
                    List<Long> metrics = result.getRawData();
                    if (metrics.size() >= 3) {
                        long hitCount = metrics.get(0);
                        long totalRequests = metrics.get(1);
                        long hitRatePercent = metrics.get(2);
                        
                        report.append(String.format("    缓存命中数: %d\n", hitCount));
                        report.append(String.format("    总请求数: %d\n", totalRequests));
                        report.append(String.format("    命中率: %d%%\n", hitRatePercent));
                    }
                } else if (result.getTestType() == BenchmarkResult.TestType.CACHE_WARMUP) {
                    report.append(String.format("    预热时间: %dms\n", result.getTotalTimeMs()));
                }
                report.append("\n");
            }
        }

        // 扩展性测试结果
        if (resultsByType.containsKey(BenchmarkResult.TestType.SCALABILITY)) {
            report.append("扩展性测试结果\n");
            report.append(Java8Utils.repeat("-", 40)).append("\n");
            
            List<BenchmarkResult> scalabilityResults = resultsByType.get(BenchmarkResult.TestType.SCALABILITY);
            scalabilityResults.sort(Comparator.comparingInt(BenchmarkResult::getConcurrencyLevel));
            
            for (BenchmarkResult result : scalabilityResults) {
                BenchmarkResult.BenchmarkStatistics stats = result.getStatistics();
                report.append(String.format("  %s (用户数: %d):\n", result.getTestName(), result.getConcurrencyLevel()));
                report.append(String.format("    总吞吐量: %.2f 操作/秒\n", result.getThroughput()));
                report.append(String.format("    平均响应时间: %.2fms\n", stats.getMean()));
                report.append(String.format("    P95响应时间: %dms\n", stats.getP95()));
                report.append(String.format("    每用户吞吐量: %.2f 操作/秒\n", result.getThroughput() / result.getConcurrencyLevel()));
                report.append("\n");
            }
        }

        // 性能建议
        report.append("性能分析与建议\n");
        report.append(Java8Utils.repeat("-", 40)).append("\n");
        report.append(generateRecommendations());
        
        // 报告尾部
        report.append("\n");
        report.append(Java8Utils.repeat("=", 80)).append("\n");
        report.append(String.format("报告生成完成 - %s\n", dateFormat.format(new Date())));
        report.append(Java8Utils.repeat("=", 80)).append("\n");
        
        return report.toString();
    }

    /**
     * 生成性能建议
     */
    private String generateRecommendations() {
        StringBuilder recommendations = new StringBuilder();
        
        // 分析延迟性能
        List<BenchmarkResult> latencyResults = results.stream()
            .filter(r -> r.getTestType() == BenchmarkResult.TestType.LATENCY || 
                        r.getTestType() == BenchmarkResult.TestType.LATENCY_PERCENTILE)
            .collect(Collectors.toList());
        
        if (!latencyResults.isEmpty()) {
            OptionalDouble avgP99 = latencyResults.stream()
                .mapToDouble(r -> r.getStatistics().getP99())
                .average();
            
            if (avgP99.isPresent() && avgP99.getAsDouble() > 100) {
                recommendations.append("• P99延迟较高(").append(String.format("%.1f", avgP99.getAsDouble()))
                    .append("ms)，建议优化数据库查询和缓存策略\n");
            }
        }
        
        // 分析并发性能
        List<BenchmarkResult> concurrencyResults = results.stream()
            .filter(r -> r.getTestType() == BenchmarkResult.TestType.CONCURRENCY)
            .collect(Collectors.toList());
        
        if (concurrencyResults.size() >= 2) {
            // 检查吞吐量扩展性
            BenchmarkResult lowConcurrency = concurrencyResults.stream()
                .min(Comparator.comparingInt(BenchmarkResult::getConcurrencyLevel))
                .orElse(null);
            BenchmarkResult highConcurrency = concurrencyResults.stream()
                .max(Comparator.comparingInt(BenchmarkResult::getConcurrencyLevel))
                .orElse(null);
            
            if (lowConcurrency != null && highConcurrency != null) {
                double throughputRatio = highConcurrency.getThroughput() / lowConcurrency.getThroughput();
                double concurrencyRatio = (double) highConcurrency.getConcurrencyLevel() / lowConcurrency.getConcurrencyLevel();
                double scalingEfficiency = throughputRatio / concurrencyRatio;
                
                if (scalingEfficiency < 0.7) {
                    recommendations.append("• 并发扩展效率较低(").append(String.format("%.2f", scalingEfficiency))
                        .append(")，存在并发瓶颈，建议优化锁机制和资源池\n");
                }
            }
        }
        
        // 分析内存使用
        List<BenchmarkResult> memoryResults = results.stream()
            .filter(r -> r.getTestType() == BenchmarkResult.TestType.MEMORY_STRESS)
            .collect(Collectors.toList());
        
        for (BenchmarkResult result : memoryResults) {
            List<Long> memoryUsage = result.getRawData();
            if (!memoryUsage.isEmpty()) {
                long initialMemory = memoryUsage.get(0);
                long finalMemory = memoryUsage.get(memoryUsage.size() - 1);
                double memoryGrowthMB = (finalMemory - initialMemory) / (1024.0 * 1024.0);
                
                if (memoryGrowthMB > 100) {
                    recommendations.append("• 内存使用增长较大(").append(String.format("%.1f", memoryGrowthMB))
                        .append("MB)，建议优化对象池和垃圾回收策略\n");
                }
            }
        }
        
        // 分析缓存性能
        List<BenchmarkResult> cacheResults = results.stream()
            .filter(r -> r.getTestType() == BenchmarkResult.TestType.CACHE_PERFORMANCE)
            .collect(Collectors.toList());
        
        for (BenchmarkResult result : cacheResults) {
            List<Long> metrics = result.getRawData();
            if (metrics.size() >= 3) {
                long hitRatePercent = metrics.get(2);
                if (hitRatePercent < 80) {
                    recommendations.append("• 缓存命中率较低(").append(hitRatePercent)
                        .append("%)，建议调整缓存大小和TTL策略\n");
                }
            }
        }
        
        if (recommendations.length() == 0) {
            recommendations.append("• 整体性能表现良好，无明显瓶颈\n");
        }
        
        // 通用建议
        recommendations.append("• 建议定期进行性能回归测试以确保性能稳定性\n");
        recommendations.append("• 可考虑在生产环境中启用性能监控以实时跟踪关键指标\n");
        
        return recommendations.toString();
    }

    /**
     * 分析内存泄漏
     */
    private boolean analyzeMemoryLeak(List<Long> memoryUsage) {
        if (memoryUsage.size() < 10) {
            return false; // 样本太少无法判断
        }
        
        // 检查内存使用是否持续增长
        int growthCount = 0;
        for (int i = 1; i < memoryUsage.size(); i++) {
            if (memoryUsage.get(i) > memoryUsage.get(i - 1)) {
                growthCount++;
            }
        }
        
        // 如果超过70%的时间内存都在增长，可能存在泄漏
        return (double) growthCount / (memoryUsage.size() - 1) > 0.7;
    }

    // Getter方法
    public List<BenchmarkResult> getResults() { 
        return Collections.unmodifiableList(results); 
    }
    
    public long getTotalDurationMs() { 
        return totalDurationMs; 
    }
    
    public BenchmarkConfig getConfig() { 
        return config; 
    }
    
    public long getGeneratedAt() { 
        return generatedAt; 
    }
    
    public String getReportId() { 
        return reportId; 
    }

    @Override
    public String toString() {
        return String.format("BenchmarkReport{测试数=%d, 总时间=%dms, 生成时间=%d}",
            results.size(), totalDurationMs, generatedAt);
    }
}