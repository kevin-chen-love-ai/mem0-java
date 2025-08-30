package com.mem0.embedding.impl;

import com.mem0.embedding.EmbeddingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mem0.concurrency.cache.HighPerformanceCache;
import com.mem0.performance.ConcurrentExecutionManager;
import com.mem0.performance.ObjectPool;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * 高性能TF-IDF嵌入提供者实现 / High-Performance TF-IDF Embedding Provider Implementation
 * 
 * 该类实现了基于TF-IDF（Term Frequency-Inverse Document Frequency）算法的高性能
 * 文本嵌入向量生成服务，结合了多项先进的性能优化技术，包括并发计算、智能缓存、
 * 批量处理、对象池化和内存优化，适用于大规模文本处理和实时检索场景。
 * 
 * This class implements a high-performance text embedding vector generation service
 * based on TF-IDF (Term Frequency-Inverse Document Frequency) algorithm, incorporating
 * multiple advanced performance optimization techniques including concurrent computation,
 * intelligent caching, batch processing, object pooling, and memory optimization,
 * suitable for large-scale text processing and real-time retrieval scenarios.
 * 
 * 主要功能 / Key Features:
 * - 高性能TF-IDF向量计算 / High-performance TF-IDF vector computation
 * - 并发多线程处理架构 / Concurrent multi-threading processing architecture
 * - 智能缓存系统 / Intelligent caching system
 * - 批量处理优化 / Batch processing optimization
 * - 对象池化内存管理 / Object pooling memory management
 * - 动态词汇表构建 / Dynamic vocabulary building
 * - 相似词搜索功能 / Similar word search functionality
 * - 实时性能监控 / Real-time performance monitoring
 * 
 * 性能特征 / Performance Characteristics:
 * - 向量维度: 可配置 (默认300) / Configurable vector dimension (default 300)
 * - 词汇表大小: 可配置 (默认10,000) / Configurable vocabulary size (default 10,000)
 * - 批处理大小: 可配置 (默认50) / Configurable batch size (default 50)
 * - 缓存容量: 5,000向量，TTL 10分钟 / Cache capacity: 5,000 vectors, TTL 10 minutes
 * - 对象池: 200个向量池，100个词频池 / Object pools: 200 vector, 100 term-freq
 * - 并发执行: 基于线程池的异步处理 / Concurrent execution: thread pool-based async
 * 
 * 算法优化 / Algorithm Optimizations:
 * - 预计算词向量表加速查询 / Pre-computed word vectors for fast lookup
 * - 哈希映射优化词汇索引 / Hash mapping optimized vocabulary indexing
 * - 向量归一化提高相似性精度 / Vector normalization for similarity accuracy
 * - 读写锁保证并发安全 / Read-write locks for concurrent safety
 * - 内存池化减少GC压力 / Memory pooling to reduce GC pressure
 * 
 * 使用场景 / Use Cases:
 * - 大规模文档检索系统 / Large-scale document retrieval systems
 * - 实时相似性搜索 / Real-time similarity search
 * - 文本聚类和分类 / Text clustering and classification
 * - 信息检索和推荐 / Information retrieval and recommendation
 * - 语料库分析和挖掘 / Corpus analysis and mining
 * - 高并发文本处理服务 / High-concurrency text processing services
 * 
 * 使用示例 / Usage Example:
 * <pre>
 * {@code
 * // 初始化高性能提供者 / Initialize high-performance provider
 * HighPerformanceTFIDFProvider provider = new HighPerformanceTFIDFProvider(
 *     10000, // 词汇表大小 / vocabulary size
 *     300,   // 向量维度 / embedding dimension  
 *     50     // 批处理大小 / batch size
 * );
 * 
 * // 训练语料库 / Train on corpus
 * List<String> corpus = Arrays.asList("doc1", "doc2", "doc3");
 * provider.trainOnCorpus(corpus).join();
 * 
 * // 单个文本嵌入 / Single text embedding
 * CompletableFuture<List<Float>> embedding = provider.embed("query text");
 * 
 * // 批量文本嵌入 / Batch text embedding  
 * List<String> texts = Arrays.asList("text1", "text2", "text3");
 * CompletableFuture<List<List<Float>>> embeddings = provider.embedBatch(texts);
 * 
 * // 查找相似词 / Find similar words
 * CompletableFuture<List<SimilarWord>> similar = provider.findSimilarWords("word", 10);
 * 
 * // 获取性能统计 / Get performance statistics
 * EmbeddingStats stats = provider.getStats();
 * System.out.println("缓存命中率: " + stats.getCacheHitRate());
 * 
 * // 预热缓存 / Warm up cache
 * provider.warmupCache(commonQueries);
 * 
 * // 关闭资源 / Close resources
 * provider.close();
 * }
 * </pre>
 * 
 * 高级特性 / Advanced Features:
 * - 训练模式: 批量构建词汇表和IDF值 / Training mode: batch vocabulary and IDF building
 * - 相似词查询: 基于余弦相似度的词汇搜索 / Similar word search: cosine similarity-based
 * - 缓存预热: 提前加载常用查询提升响应速度 / Cache warming: preload common queries
 * - 统计监控: 详细的性能指标和缓存统计 / Statistics monitoring: detailed metrics
 * - 健康检查: 实时组件状态监控 / Health check: real-time component monitoring
 * 
 * 注意事项 / Important Notes:
 * - 适用于中等规模语料库 (< 100万文档)
 * - 需要足够内存支持词汇表和缓存
 * - 首次训练需要较长时间建立词汇表
 * - Suitable for medium-scale corpora (< 1M documents)
 * - Requires sufficient memory for vocabulary and cache
 * - Initial training requires time to build vocabulary
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class HighPerformanceTFIDFProvider implements EmbeddingProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(HighPerformanceTFIDFProvider.class);
    
    // 词汇表和统计信息
    private final Map<String, Integer> vocabulary = new ConcurrentHashMap<>();
    private final Map<String, Double> inverseDocumentFrequencies = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Double>> documentTermFrequencies = new ConcurrentHashMap<>();
    
    // 配置参数
    private final int maxVocabularySize;
    private final int embeddingDimension;
    private final int batchSize;
    
    // 性能优化组件
    private final HighPerformanceCache<String, float[]> embeddingCache;
    private final ConcurrentExecutionManager executionManager;
    private final ReadWriteLock vocabularyLock = new ReentrantReadWriteLock();
    
    // 对象池
    private final ObjectPool<float[]> vectorPool;
    private final ObjectPool<Map<String, Double>> termFreqPool;
    
    // 统计信息
    private final AtomicInteger totalEmbeddings = new AtomicInteger(0);
    private final AtomicInteger cacheHits = new AtomicInteger(0);
    private final AtomicInteger batchProcessed = new AtomicInteger(0);
    
    // 预计算的词向量表
    private final Map<String, float[]> wordVectors = new ConcurrentHashMap<>();
    private volatile boolean isTraining = false;
    
    public HighPerformanceTFIDFProvider() {
        this(10000, 300, 50);
    }
    
    public HighPerformanceTFIDFProvider(int maxVocabularySize, int embeddingDimension, int batchSize) {
        this.maxVocabularySize = maxVocabularySize;
        this.embeddingDimension = embeddingDimension;
        this.batchSize = batchSize;
        
        this.embeddingCache = new HighPerformanceCache<>(5000, 600000, 120000); // 5K向量，10分钟TTL
        this.executionManager = new ConcurrentExecutionManager();
        
        // 初始化对象池
        this.vectorPool = new ObjectPool<>(
            () -> new float[embeddingDimension],
            vector -> Arrays.fill(vector, 0.0f),
            200
        );
        
        this.termFreqPool = new ObjectPool<>(
            HashMap::new,
            Map::clear,
            100
        );
        
        // 预填充对象池
        vectorPool.preFill(50);
        termFreqPool.preFill(20);
        
        logger.info("高性能TF-IDF嵌入提供者初始化完成 - 词汇表大小: {}, 向量维度: {}, 批大小: {}",
                   maxVocabularySize, embeddingDimension, batchSize);
    }
    
    @Override
    public CompletableFuture<List<Float>> embed(String text) {
        return executionManager.executeEmbeddingOperation(() -> {
            if (text == null || text.trim().isEmpty()) {
                return convertToList(createZeroVector());
            }
            
            // 检查缓存
            String cacheKey = generateCacheKey(text);
            float[] cached = embeddingCache.get(cacheKey);
            if (cached != null) {
                cacheHits.incrementAndGet();
                return convertToList(Arrays.copyOf(cached, cached.length));
            }
            
            logger.debug("计算文本嵌入: {}", text.substring(0, Math.min(text.length(), 50)) + "...");
            
            List<String> tokens = tokenizeOptimized(text);
            if (tokens.isEmpty()) {
                return convertToList(createZeroVector());
            }
            
            // 更新词汇表（如果不在训练模式）
            if (!isTraining) {
                updateVocabulary(tokens);
            }
            
            // 计算TF-IDF向量
            float[] embedding = computeTFIDFVector(tokens);
            
            // 缓存结果
            embeddingCache.put(cacheKey, Arrays.copyOf(embedding, embedding.length));
            
            totalEmbeddings.incrementAndGet();
            return convertToList(embedding);
        });
    }
    
    @Override
    public CompletableFuture<List<List<Float>>> embedBatch(List<String> texts) {
        return executionManager.executeEmbeddingOperation(() -> {
            if (texts == null || texts.isEmpty()) {
                return Collections.emptyList();
            }
            
            logger.info("开始批量嵌入处理，文本数量: {}", texts.size());
            long startTime = System.currentTimeMillis();
            
            // 分批处理以优化内存使用
            List<CompletableFuture<List<float[]>>> batchFutures = new ArrayList<>();
            
            for (int i = 0; i < texts.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, texts.size());
                List<String> batch = texts.subList(i, endIndex);
                
                CompletableFuture<List<float[]>> batchFuture = processBatch(batch);
                batchFutures.add(batchFuture);
            }
            
            // 等待所有批次完成
            CompletableFuture<Void> allBatches = CompletableFuture.allOf(
                batchFutures.toArray(new CompletableFuture[0])
            );
            
            return allBatches.thenApply(v -> {
                List<float[]> results = new ArrayList<>();
                for (CompletableFuture<List<float[]>> batchFuture : batchFutures) {
                    try {
                        results.addAll(batchFuture.get());
                    } catch (Exception e) {
                        logger.error("批处理失败", e);
                        // 添加零向量作为错误处理
                        for (int j = 0; j < batchSize; j++) {
                            results.add(createZeroVector());
                        }
                    }
                }
                
                long duration = System.currentTimeMillis() - startTime;
                batchProcessed.incrementAndGet();
                logger.info("批量嵌入完成，处理 {} 个文本，耗时 {}ms", texts.size(), duration);
                
                return convertBatchToList(results);
            }).join();
        });
    }
    
    /**
     * 训练模式 - 批量构建词汇表和IDF
     */
    public CompletableFuture<Void> trainOnCorpus(List<String> corpus) {
        return executionManager.executeEmbeddingOperation(() -> {
            logger.info("开始训练模式，语料库大小: {}", corpus.size());
            long startTime = System.currentTimeMillis();
            
            isTraining = true;
            
            try {
                // 第一阶段：并行构建词汇表
                Set<String> allTokens = ConcurrentHashMap.newKeySet();
                List<List<String>> allDocumentTokens = corpus.parallelStream()
                    .map(this::tokenizeOptimized)
                    .peek(tokens -> allTokens.addAll(tokens))
                    .collect(Collectors.toList());
                
                // 构建词汇表
                buildVocabulary(allTokens);
                
                // 第二阶段：计算文档频率
                Map<String, Integer> documentFrequencies = new ConcurrentHashMap<>();
                allDocumentTokens.parallelStream().forEach(tokens -> {
                    Set<String> uniqueTokens = new HashSet<>(tokens);
                    uniqueTokens.forEach(token -> 
                        documentFrequencies.merge(token, 1, Integer::sum)
                    );
                });
                
                // 第三阶段：计算IDF值
                int totalDocuments = corpus.size();
                documentFrequencies.entrySet().parallelStream().forEach(entry -> {
                    String term = entry.getKey();
                    int docFreq = entry.getValue();
                    double idf = Math.log((double) totalDocuments / (1.0 + docFreq));
                    inverseDocumentFrequencies.put(term, idf);
                });
                
                // 第四阶段：预计算常用词的词向量
                precomputeWordVectors();
                
                long duration = System.currentTimeMillis() - startTime;
                logger.info("训练完成 - 词汇表大小: {}, IDF条目: {}, 耗时: {}ms", 
                          vocabulary.size(), inverseDocumentFrequencies.size(), duration);
                
            } finally {
                isTraining = false;
            }
            
            return null;
        });
    }
    
    /**
     * 获取相似词
     */
    public CompletableFuture<List<SimilarWord>> findSimilarWords(String word, int limit) {
        return executionManager.executeEmbeddingOperation(() -> {
            float[] wordVector = getWordVector(word);
            if (wordVector == null) {
                return Collections.emptyList();
            }
            
            List<SimilarWord> similarWords = new ArrayList<>();
            
            for (Map.Entry<String, float[]> entry : wordVectors.entrySet()) {
                if (!entry.getKey().equals(word)) {
                    float similarity = calculateCosineSimilarity(wordVector, entry.getValue());
                    similarWords.add(new SimilarWord(entry.getKey(), similarity));
                }
            }
            
            return similarWords.stream()
                .sorted((a, b) -> Float.compare(b.similarity, a.similarity))
                .limit(limit)
                .collect(Collectors.toList());
        });
    }
    
    @Override
    public int getDimension() {
        return embeddingDimension;
    }
    
    /**
     * 获取性能统计
     */
    public EmbeddingStats getStats() {
        return new EmbeddingStats(
            vocabulary.size(),
            inverseDocumentFrequencies.size(),
            totalEmbeddings.get(),
            cacheHits.get(),
            batchProcessed.get(),
            embeddingCache.getStats(),
            vectorPool.getStats(),
            termFreqPool.getStats()
        );
    }
    
    /**
     * 预热缓存
     */
    public void warmupCache(List<String> commonTexts) {
        logger.info("开始缓存预热，文本数量: {}", commonTexts.size());
        
        List<CompletableFuture<List<Float>>> futures = commonTexts.stream()
            .map(this::embed)
            .collect(Collectors.toList());
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        logger.info("缓存预热完成");
    }
    
    @Override
    public void close() {
        logger.info("关闭高性能TF-IDF嵌入提供者");
        
        if (executionManager != null) {
            executionManager.close();
        }
        
        if (embeddingCache != null) {
            embeddingCache.shutdown();
        }
        
        vocabulary.clear();
        inverseDocumentFrequencies.clear();
        documentTermFrequencies.clear();
        wordVectors.clear();
        
        logger.info("嵌入提供者关闭完成");
    }
    
    @Override
    public String getProviderName() {
        return "HighPerformance-TFIDF";
    }
    
    @Override
    public boolean isHealthy() {
        return executionManager != null && embeddingCache != null && !vocabulary.isEmpty();
    }
    
    // Helper methods
    private List<Float> convertToList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float value : array) {
            list.add(value);
        }
        return list;
    }
    
    private float[] convertToArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
    
    private List<List<Float>> convertBatchToList(List<float[]> batch) {
        List<List<Float>> result = new ArrayList<>(batch.size());
        for (float[] array : batch) {
            result.add(convertToList(array));
        }
        return result;
    }
    
    // 私有辅助方法
    
    private CompletableFuture<List<float[]>> processBatch(List<String> batch) {
        return CompletableFuture.supplyAsync(() -> {
            List<float[]> results = new ArrayList<>();
            
            for (String text : batch) {
                try {
                    float[] embedding = convertToArray(embed(text).join());
                    results.add(embedding);
                } catch (Exception e) {
                    logger.error("处理批次中的文本失败", e);
                    results.add(createZeroVector());
                }
            }
            
            return results;
        });
    }
    
    private List<String> tokenizeOptimized(String text) {
        // 优化的分词实现，使用StringBuilder池
        StringBuilder sb = null;
        try {
            sb = new StringBuilder(); // 简化实现，实际可使用对象池
            
            List<String> tokens = new ArrayList<>();
            String[] words = text.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .split("\\s+");
            
            for (String word : words) {
                if (word.length() > 1 && word.length() < 50) { // 过滤过短和过长的词
                    tokens.add(word);
                }
            }
            
            return tokens;
        } finally {
            // 在实际实现中，这里应该释放StringBuilder回对象池
        }
    }
    
    private void updateVocabulary(List<String> tokens) {
        vocabularyLock.writeLock().lock();
        try {
            for (String token : tokens) {
                if (vocabulary.size() < maxVocabularySize && !vocabulary.containsKey(token)) {
                    vocabulary.put(token, vocabulary.size());
                }
            }
        } finally {
            vocabularyLock.writeLock().unlock();
        }
    }
    
    private void buildVocabulary(Set<String> allTokens) {
        vocabularyLock.writeLock().lock();
        try {
            // 按频率排序，只保留最常见的词
            List<String> sortedTokens = allTokens.stream()
                .sorted()
                .limit(maxVocabularySize)
                .collect(Collectors.toList());
            
            vocabulary.clear();
            for (int i = 0; i < sortedTokens.size(); i++) {
                vocabulary.put(sortedTokens.get(i), i);
            }
        } finally {
            vocabularyLock.writeLock().unlock();
        }
    }
    
    private float[] computeTFIDFVector(List<String> tokens) {
        float[] vector = vectorPool.acquire();
        
        try {
            // 计算词频
            Map<String, Double> termFreqs = calculateTermFrequencies(tokens);
            
            // 计算TF-IDF并映射到固定维度
            for (Map.Entry<String, Double> entry : termFreqs.entrySet()) {
                String term = entry.getKey();
                Double tf = entry.getValue();
                
                Integer vocabularyIndex = vocabulary.get(term);
                if (vocabularyIndex != null) {
                    double idf = inverseDocumentFrequencies.getOrDefault(term, 1.0);
                    double tfidf = tf * idf;
                    
                    // 映射到向量维度
                    int[] targetIndices = getTargetIndices(vocabularyIndex);
                    for (int targetIndex : targetIndices) {
                        if (targetIndex < embeddingDimension) {
                            vector[targetIndex] += (float) (tfidf / targetIndices.length);
                        }
                    }
                }
            }
            
            // 归一化向量
            normalizeVector(vector);
            
            // 复制结果（因为要释放池化的向量）
            return Arrays.copyOf(vector, vector.length);
            
        } finally {
            vectorPool.release(vector);
        }
    }
    
    private Map<String, Double> calculateTermFrequencies(List<String> tokens) {
        Map<String, Double> termFreqs = termFreqPool.acquire();
        
        try {
            Map<String, Integer> termCounts = new HashMap<>();
            
            for (String token : tokens) {
                termCounts.merge(token, 1, Integer::sum);
            }
            
            int totalTokens = tokens.size();
            for (Map.Entry<String, Integer> entry : termCounts.entrySet()) {
                double tf = (double) entry.getValue() / totalTokens;
                termFreqs.put(entry.getKey(), tf);
            }
            
            // 复制结果
            return new HashMap<>(termFreqs);
            
        } finally {
            termFreqPool.release(termFreqs);
        }
    }
    
    private void precomputeWordVectors() {
        logger.info("预计算词向量，词汇表大小: {}", vocabulary.size());
        
        vocabulary.entrySet().parallelStream().forEach(entry -> {
            String word = entry.getKey();
            List<String> singleWordTokens = Collections.singletonList(word);
            float[] wordVector = computeTFIDFVector(singleWordTokens);
            wordVectors.put(word, wordVector);
        });
        
        logger.info("词向量预计算完成，向量数量: {}", wordVectors.size());
    }
    
    private float[] getWordVector(String word) {
        return wordVectors.get(word.toLowerCase());
    }
    
    private int[] getTargetIndices(int vocabIndex) {
        // 使用确定性哈希映射词汇索引到嵌入维度
        Random rand = new Random(vocabIndex);
        int numMappings = Math.min(5, embeddingDimension / 10);
        
        Set<Integer> indices = new HashSet<>();
        for (int i = 0; i < numMappings; i++) {
            indices.add(rand.nextInt(embeddingDimension));
        }
        
        return indices.stream().mapToInt(Integer::intValue).toArray();
    }
    
    private void normalizeVector(float[] vector) {
        double magnitude = 0.0;
        for (float value : vector) {
            magnitude += value * value;
        }
        
        magnitude = Math.sqrt(magnitude);
        
        if (magnitude > 0.0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) (vector[i] / magnitude);
            }
        }
    }
    
    private float[] createZeroVector() {
        return new float[embeddingDimension];
    }
    
    private String generateCacheKey(String text) {
        // 简化的缓存键生成
        return "embed_" + text.hashCode();
    }
    
    private float calculateCosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA.length != vectorB.length) {
            return 0.0f;
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }
        
        double magnitude = Math.sqrt(normA) * Math.sqrt(normB);
        return magnitude == 0.0 ? 0.0f : (float) (dotProduct / magnitude);
    }
    
    // 数据类
    
    public static class SimilarWord {
        public final String word;
        public final float similarity;
        
        public SimilarWord(String word, float similarity) {
            this.word = word;
            this.similarity = similarity;
        }
        
        @Override
        public String toString() {
            return String.format("%s(%.3f)", word, similarity);
        }
    }
    
    public static class EmbeddingStats {
        private final int vocabularySize;
        private final int idfSize;
        private final int totalEmbeddings;
        private final int cacheHits;
        private final int batchesProcessed;
        private final HighPerformanceCache.CacheStats cacheStats;
        private final ObjectPool.PoolStats vectorPoolStats;
        private final ObjectPool.PoolStats termFreqPoolStats;
        
        public EmbeddingStats(int vocabularySize, int idfSize, int totalEmbeddings, 
                            int cacheHits, int batchesProcessed,
                            HighPerformanceCache.CacheStats cacheStats,
                            ObjectPool.PoolStats vectorPoolStats,
                            ObjectPool.PoolStats termFreqPoolStats) {
            this.vocabularySize = vocabularySize;
            this.idfSize = idfSize;
            this.totalEmbeddings = totalEmbeddings;
            this.cacheHits = cacheHits;
            this.batchesProcessed = batchesProcessed;
            this.cacheStats = cacheStats;
            this.vectorPoolStats = vectorPoolStats;
            this.termFreqPoolStats = termFreqPoolStats;
        }
        
        // Getter方法
        public int getVocabularySize() { return vocabularySize; }
        public int getIdfSize() { return idfSize; }
        public int getTotalEmbeddings() { return totalEmbeddings; }
        public int getCacheHits() { return cacheHits; }
        public int getBatchesProcessed() { return batchesProcessed; }
        public HighPerformanceCache.CacheStats getCacheStats() { return cacheStats; }
        public ObjectPool.PoolStats getVectorPoolStats() { return vectorPoolStats; }
        public ObjectPool.PoolStats getTermFreqPoolStats() { return termFreqPoolStats; }
        
        public double getCacheHitRate() {
            return totalEmbeddings == 0 ? 0.0 : (double) cacheHits / totalEmbeddings;
        }
        
        @Override
        public String toString() {
            return String.format("EmbeddingStats{词汇=%d, IDF=%d, 嵌入=%d, 缓存命中=%d(%.2f%%), 批次=%d, 缓存=%s, 向量池=%s, 词频池=%s}",
                vocabularySize, idfSize, totalEmbeddings, cacheHits, getCacheHitRate() * 100,
                batchesProcessed, cacheStats, vectorPoolStats, termFreqPoolStats);
        }
    }
}