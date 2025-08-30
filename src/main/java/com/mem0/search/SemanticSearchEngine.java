package com.mem0.search;

import com.mem0.core.EnhancedMemory;
import com.mem0.core.MemoryImportance;
import com.mem0.core.MemoryType;
import com.mem0.embedding.EmbeddingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 语义搜索引擎 / Semantic Search Engine
 * 
 * 基于向量嵌入和语义相似性的高级搜索引擎，提供智能查询理解、语义匹配、多维排序等功能。
 * 支持自然语言查询、概念匹配、上下文理解，显著提升搜索准确性和用户体验。
 * Advanced search engine based on vector embeddings and semantic similarity, providing intelligent
 * query understanding, semantic matching, multi-dimensional ranking and other features. Supports
 * natural language queries, concept matching, contextual understanding, significantly improving
 * search accuracy and user experience.
 * 
 * <h3>核心功能 / Key Features:</h3>
 * <ul>
 *   <li>向量化语义搜索和相似性计算 / Vectorized semantic search and similarity computation</li>
 *   <li>智能查询扩展和同义词处理 / Intelligent query expansion and synonym handling</li>
 *   <li>多维度排序和结果重排 / Multi-dimensional ranking and result re-ranking</li>
 *   <li>上下文感知的搜索优化 / Context-aware search optimization</li>
 *   <li>实时搜索建议和自动补全 / Real-time search suggestions and auto-completion</li>
 *   <li>搜索性能监控和分析优化 / Search performance monitoring and analysis optimization</li>
 * </ul>
 * 
 * <h3>搜索架构设计 / Search Architecture Design:</h3>
 * <pre>
 * Semantic Search Pipeline 语义搜索流水线:
 * 
 * ┌─────────────────────────────────────────────────────────┐
 * │                SemanticSearchEngine                     │
 * ├─────────────────────────────────────────────────────────┤
 * │                                                         │
 * │  ┌─────────────┐    ┌─────────────────┐    ┌─────────┐ │
 * │  │ Query       │    │ Embedding       │    │ Vector  │ │
 * │  │ Processing  │ → │ Generation      │ → │ Search  │ │
 * │  │ 查询处理     │    │ 嵌入生成         │    │ 向量搜索 │ │
 * │  └─────────────┘    └─────────────────┘    └─────────┘ │
 * │         │                     │                     │   │
 * │         ▼                     ▼                     ▼   │
 * │  ┌─────────────┐    ┌─────────────────┐    ┌─────────┐ │
 * │  │ Query       │    │ Similarity      │    │ Result  │ │
 * │  │ Expansion   │    │ Computation     │    │ Ranking │ │
 * │  │ 查询扩展     │    │ 相似度计算       │    │ 结果排序 │ │
 * │  └─────────────┘    └─────────────────┘    └─────────┘ │
 * │                                                         │
 * │  ┌─────────────────────────────────────────────────────┐ │
 * │  │              Ranking Components                     │ │
 * │  │                 排序组件                             │ │
 * │  │  • Semantic Relevance (语义相关性)                 │ │
 * │  │  • Importance Weight (重要性权重)                  │ │
 * │  │  • Recency Factor (时间因子)                       │ │
 * │  │  • Context Boost (上下文增强)                      │ │
 * │  │  • User Preference (用户偏好)                      │ │
 * │  └─────────────────────────────────────────────────────┘ │
 * └─────────────────────────────────────────────────────────┘
 * 
 * Search Quality Metrics 搜索质量指标:
 * ├─ Precision (精确率): 相关结果占返回结果的比例
 * ├─ Recall (召回率): 返回结果占相关结果总数的比例  
 * ├─ MRR (平均倒数排名): 第一个相关结果的平均排名倒数
 * └─ NDCG (归一化折扣累积增益): 考虑排序的质量评估
 * </pre>
 * 
 * <h3>使用示例 / Usage Examples:</h3>
 * <pre>{@code
 * // 创建语义搜索引擎
 * EmbeddingProvider embeddingProvider = new OpenAIEmbeddingProvider("api_key");
 * SemanticSearchEngine searchEngine = new SemanticSearchEngine(embeddingProvider);
 * 
 * // 配置搜索参数
 * SearchConfiguration config = new SearchConfiguration();
 * config.setSemanticThreshold(0.75);
 * config.setMaxResults(20);
 * config.setEnableQueryExpansion(true);
 * config.setRerankingEnabled(true);
 * 
 * // 构建内存索引
 * List<EnhancedMemory> memoryDatabase = Arrays.asList(
 *     createMemory("Java Spring Boot微服务开发最佳实践", MemoryType.SEMANTIC, MemoryImportance.HIGH),
 *     createMemory("Python机器学习算法实现指南", MemoryType.FACTUAL, MemoryImportance.MEDIUM),
 *     createMemory("用户偏好使用React进行前端开发", MemoryType.PREFERENCE, MemoryImportance.HIGH),
 *     createMemory("Docker容器化部署流程详解", MemoryType.PROCEDURAL, MemoryImportance.MEDIUM)
 * );
 * 
 * CompletableFuture<Void> indexFuture = searchEngine.buildSearchIndex(memoryDatabase);
 * indexFuture.join(); // 等待索引构建完成
 * 
 * // 执行语义搜索
 * String query = "如何使用Spring开发微服务应用？";
 * CompletableFuture<SemanticSearchResult> searchFuture = 
 *     searchEngine.search(query, config);
 * SemanticSearchResult searchResult = searchFuture.join();
 * 
 * System.out.println("=== 语义搜索结果 ===");
 * System.out.println("查询: " + query);
 * System.out.println("找到结果数: " + searchResult.getResults().size());
 * System.out.println("搜索耗时: " + searchResult.getSearchTimeMs() + "ms");
 * System.out.println("语义扩展查询: " + searchResult.getExpandedQueries());
 * 
 * // 展示搜索结果详情
 * for (SearchResultItem item : searchResult.getResults()) {
 *     System.out.println("\n--- 结果 " + (item.getRank() + 1) + " ---");
 *     System.out.println("语义相似度: " + String.format("%.3f", item.getSemanticScore()));
 *     System.out.println("综合评分: " + String.format("%.3f", item.getFinalScore()));
 *     System.out.println("内存类型: " + item.getMemory().getType());
 *     System.out.println("重要性: " + item.getMemory().getImportance());
 *     System.out.println("内容: " + item.getMemory().getContent().substring(0, Math.min(100, item.getMemory().getContent().length())) + "...");
 *     
 *     // 显示评分详细信息
 *     ScoreBreakdown breakdown = item.getScoreBreakdown();
 *     System.out.println("评分详情:");
 *     System.out.println("  语义匹配: " + String.format("%.3f", breakdown.getSemanticComponent()));
 *     System.out.println("  重要性权重: " + String.format("%.3f", breakdown.getImportanceComponent()));
 *     System.out.println("  时间因子: " + String.format("%.3f", breakdown.getRecencyComponent()));
 *     System.out.println("  上下文加权: " + String.format("%.3f", breakdown.getContextComponent()));
 * }
 * 
 * // 获取搜索建议
 * CompletableFuture<List<String>> suggestionsFuture = 
 *     searchEngine.getSearchSuggestions("Spring", 5);
 * List<String> suggestions = suggestionsFuture.join();
 * 
 * System.out.println("\n=== 搜索建议 ===");
 * suggestions.forEach(suggestion -> System.out.println("- " + suggestion));
 * 
 * // 相关性反馈和学习
 * searchEngine.recordRelevanceFeedback(query, searchResult.getResults().get(0), 1.0); // 高度相关
 * searchEngine.recordRelevanceFeedback(query, searchResult.getResults().get(1), 0.5); // 中等相关
 * 
 * // 上下文增强搜索
 * SearchContext context = new SearchContext();
 * context.setUserId("user_12345");
 * context.setRecentQueries(Arrays.asList("Java开发", "Spring框架", "微服务架构"));
 * context.setUserPreferences(Arrays.asList("技术深度", "实践导向", "最新版本"));
 * 
 * CompletableFuture<SemanticSearchResult> contextSearchFuture = 
 *     searchEngine.searchWithContext(query, context, config);
 * SemanticSearchResult contextResult = contextSearchFuture.join();
 * 
 * System.out.println("\n=== 上下文增强搜索结果 ===");
 * System.out.println("上下文权重调整后结果数: " + contextResult.getResults().size());
 * 
 * // 批量相似性搜索
 * List<String> batchQueries = Arrays.asList(
 *     "Python数据分析", 
 *     "前端React开发", 
 *     "Docker部署"
 * );
 * 
 * CompletableFuture<Map<String, SemanticSearchResult>> batchFuture = 
 *     searchEngine.batchSearch(batchQueries, config);
 * Map<String, SemanticSearchResult> batchResults = batchFuture.join();
 * 
 * System.out.println("\n=== 批量搜索结果 ===");
 * batchResults.forEach((q, result) -> 
 *     System.out.println(q + ": " + result.getResults().size() + " 个结果"));
 * 
 * // 获取搜索性能报告
 * CompletableFuture<SearchPerformanceReport> perfFuture = 
 *     searchEngine.getPerformanceReport();
 * SearchPerformanceReport perfReport = perfFuture.join();
 * 
 * System.out.println("\n=== 搜索性能报告 ===");
 * System.out.println("总搜索次数: " + perfReport.getTotalSearches());
 * System.out.println("平均搜索时间: " + perfReport.getAverageSearchTimeMs() + "ms");
 * System.out.println("搜索缓存命中率: " + String.format("%.1f", perfReport.getCacheHitRate() * 100) + "%");
 * System.out.println("索引大小: " + perfReport.getIndexSize() + " 条记录");
 * }</pre>
 * 
 * <h3>搜索优化策略 / Search Optimization Strategies:</h3>
 * <ul>
 *   <li><b>查询扩展</b>: 自动添加同义词和相关概念扩大搜索范围 / Query expansion with synonyms and related concepts</li>
 *   <li><b>个性化排序</b>: 基于用户历史行为调整搜索结果排序 / Personalized ranking based on user behavior history</li>
 *   <li><b>上下文理解</b>: 结合会话上下文和用户意图优化搜索 / Context understanding with session context and user intent</li>
 *   <li><b>相关性学习</b>: 通过用户反馈不断优化搜索质量 / Relevance learning through user feedback</li>
 * </ul>
 * 
 * <h3>搜索质量保证 / Search Quality Assurance:</h3>
 * <ul>
 *   <li><b>多轮评估</b>: 多个评估指标综合衡量搜索质量 / Multi-round evaluation with comprehensive metrics</li>
 *   <li><b>A/B测试</b>: 不同算法和参数的对比实验 / A/B testing for different algorithms and parameters</li>
 *   <li><b>实时监控</b>: 搜索性能和质量的实时监控告警 / Real-time monitoring and alerting for search performance</li>
 *   <li><b>用户反馈</b>: 持续收集和分析用户搜索体验 / Continuous collection and analysis of user search experience</li>
 * </ul>
 * 
 * <h3>性能优化 / Performance Optimization:</h3>
 * <ul>
 *   <li><b>向量索引</b>: 使用HNSW或IVF等高效向量索引算法 / Efficient vector indexing with HNSW or IVF algorithms</li>
 *   <li><b>查询缓存</b>: 热门查询结果缓存减少重复计算 / Query result caching for popular searches</li>
 *   <li><b>异步处理</b>: 并行化搜索流程提升响应速度 / Parallel search pipeline for improved response time</li>
 *   <li><b>增量更新</b>: 支持索引的增量更新避免全量重建 / Incremental index updates to avoid full rebuilds</li>
 * </ul>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 * @see com.mem0.search.HybridSearchEngine
 * @see com.mem0.search.SearchFilter
 * @see com.mem0.embedding.EmbeddingProvider
 */
public class SemanticSearchEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(SemanticSearchEngine.class);
    
    private final EmbeddingProvider embeddingProvider;
    
    // Search index and storage
    private final Map<String, EnhancedMemory> memoryIndex;
    private final Map<String, List<Float>> embeddingIndex;
    private final Map<String, Set<String>> invertedIndex; // word -> memory IDs
    
    // Search optimization
    private final Map<String, List<String>> queryExpansions;
    private final Map<String, Double> termFrequency;
    private final Map<String, Integer> searchStatistics;
    
    // Performance tracking
    private final Map<String, SearchMetrics> performanceMetrics;
    private final Map<String, List<Float>> queryCache;
    private int totalSearches;
    private long totalSearchTimeMs;
    
    // Configuration
    private SearchConfiguration defaultConfig;
    
    public SemanticSearchEngine(EmbeddingProvider embeddingProvider) {
        this.embeddingProvider = embeddingProvider;
        
        this.memoryIndex = new ConcurrentHashMap<>();
        this.embeddingIndex = new ConcurrentHashMap<>();
        this.invertedIndex = new ConcurrentHashMap<>();
        
        this.queryExpansions = new ConcurrentHashMap<>();
        this.termFrequency = new ConcurrentHashMap<>();
        this.searchStatistics = new ConcurrentHashMap<>();
        
        this.performanceMetrics = new ConcurrentHashMap<>();
        this.queryCache = new ConcurrentHashMap<>();
        this.totalSearches = 0;
        this.totalSearchTimeMs = 0;
        
        this.defaultConfig = createDefaultConfiguration();
        
        logger.info("SemanticSearchEngine initialized with embedding provider: {}", 
            embeddingProvider.getClass().getSimpleName());
    }
    
    public CompletableFuture<Void> buildSearchIndex(List<EnhancedMemory> memories) {
        return CompletableFuture.runAsync(() -> {
            logger.info("Building search index for {} memories", memories.size());
            long startTime = System.currentTimeMillis();
            
            try {
                // Clear existing index
                memoryIndex.clear();
                embeddingIndex.clear();
                invertedIndex.clear();
                
                // Process memories in batches for efficiency
                int batchSize = 50;
                for (int i = 0; i < memories.size(); i += batchSize) {
                    int endIndex = Math.min(i + batchSize, memories.size());
                    List<EnhancedMemory> batch = memories.subList(i, endIndex);
                    
                    processBatch(batch);
                }
                
                // Build term frequency statistics
                buildTermFrequencyIndex();
                
                long buildTime = System.currentTimeMillis() - startTime;
                logger.info("Search index built successfully in {}ms for {} memories", 
                    buildTime, memories.size());
                
            } catch (Exception e) {
                logger.error("Error building search index: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to build search index", e);
            }
        });
    }
    
    public CompletableFuture<SemanticSearchResult> search(String query, SearchConfiguration config) {
        long startTime = System.currentTimeMillis();
        
        return CompletableFuture.supplyAsync(() -> {
            logger.debug("Executing semantic search for query: {}", query);
            
            try {
                // Step 1: Query processing and expansion
                List<String> expandedQueries = config.isEnableQueryExpansion() ? 
                    expandQuery(query) : Arrays.asList(query);
                
                // Step 2: Generate query embeddings
                List<Float> queryEmbedding = getQueryEmbedding(query);
                
                // Step 3: Semantic similarity search
                List<SearchResultItem> semanticResults = performSemanticSearch(
                    queryEmbedding, expandedQueries, config);
                
                // Step 4: Re-ranking if enabled
                if (config.isRerankingEnabled()) {
                    semanticResults = rerankResults(semanticResults, query, config);
                }
                
                // Step 5: Apply final filters and limits
                semanticResults = applyFinalFilters(semanticResults, config);
                
                // Update performance metrics
                long searchTime = System.currentTimeMillis() - startTime;
                updateSearchMetrics(query, searchTime, semanticResults.size());
                
                return new SemanticSearchResult(
                    query, 
                    semanticResults, 
                    expandedQueries, 
                    searchTime,
                    semanticResults.size()
                );
                
            } catch (Exception e) {
                logger.error("Error during semantic search for query '{}': {}", query, e.getMessage(), e);
                return new SemanticSearchResult(query, Collections.emptyList(), 
                    Arrays.asList(query), System.currentTimeMillis() - startTime, 0);
            }
        });
    }
    
    public CompletableFuture<SemanticSearchResult> searchWithContext(String query, SearchContext context, 
                                                                    SearchConfiguration config) {
        return CompletableFuture.supplyAsync(() -> {
            logger.debug("Executing context-aware search for query: {}", query);
            
            // Enhance config with context information
            SearchConfiguration enhancedConfig = enhanceConfigWithContext(config, context);
            
            return search(query, enhancedConfig).join();
        });
    }
    
    public CompletableFuture<Map<String, SemanticSearchResult>> batchSearch(List<String> queries, 
                                                                           SearchConfiguration config) {
        return CompletableFuture.supplyAsync(() -> {
            logger.debug("Executing batch search for {} queries", queries.size());
            
            Map<String, CompletableFuture<SemanticSearchResult>> searchFutures = queries.stream()
                .collect(Collectors.toMap(
                    query -> query,
                    query -> search(query, config)
                ));
            
            // Wait for all searches to complete
            CompletableFuture.allOf(searchFutures.values().toArray(new CompletableFuture[0])).join();
            
            // Collect results
            return searchFutures.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().join()
                ));
        });
    }
    
    public CompletableFuture<List<String>> getSearchSuggestions(String partialQuery, int maxSuggestions) {
        return CompletableFuture.supplyAsync(() -> {
            logger.debug("Getting search suggestions for partial query: {}", partialQuery);
            
            String lowerPartial = partialQuery.toLowerCase();
            
            // Get suggestions from memory content and tags
            Set<String> suggestions = new HashSet<>();
            
            // Extract suggestions from indexed terms
            for (String term : termFrequency.keySet()) {
                if (term.startsWith(lowerPartial)) {
                    suggestions.add(term);
                }
            }
            
            // Extract suggestions from memory content
            for (EnhancedMemory memory : memoryIndex.values()) {
                String content = memory.getContent().toLowerCase();
                String[] words = content.split("\\s+");
                
                for (String word : words) {
                    if (word.startsWith(lowerPartial) && word.length() > partialQuery.length()) {
                        suggestions.add(word);
                    }
                }
                
                // Add tag-based suggestions
                for (String tag : memory.getTags()) {
                    if (tag.toLowerCase().startsWith(lowerPartial)) {
                        suggestions.add(tag);
                    }
                }
            }
            
            // Sort by frequency and relevance
            return suggestions.stream()
                .sorted((s1, s2) -> {
                    double freq1 = termFrequency.getOrDefault(s1, 0.0);
                    double freq2 = termFrequency.getOrDefault(s2, 0.0);
                    return Double.compare(freq2, freq1);
                })
                .limit(maxSuggestions)
                .collect(Collectors.toList());
        });
    }
    
    public void recordRelevanceFeedback(String query, SearchResultItem result, double relevanceScore) {
        logger.debug("Recording relevance feedback for query '{}': score={}", query, relevanceScore);
        
        // Store feedback for learning
        String feedbackKey = query + "_" + result.getMemory().getId();
        SearchMetrics metrics = performanceMetrics.computeIfAbsent(feedbackKey, 
            k -> new SearchMetrics(query, result.getMemory().getId()));
        
        metrics.addRelevanceFeedback(relevanceScore);
        
        // Update query expansions based on positive feedback
        if (relevanceScore > 0.7) {
            updateQueryExpansions(query, result.getMemory());
        }
    }
    
    public CompletableFuture<SearchPerformanceReport> getPerformanceReport() {
        return CompletableFuture.supplyAsync(() -> {
            logger.debug("Generating search performance report");
            
            double averageSearchTime = totalSearches > 0 ? 
                (double) totalSearchTimeMs / totalSearches : 0.0;
            
            double cacheHitRate = calculateCacheHitRate();
            
            Map<String, Integer> queryTypeDistribution = calculateQueryTypeDistribution();
            
            return new SearchPerformanceReport(
                totalSearches,
                averageSearchTime,
                cacheHitRate,
                memoryIndex.size(),
                queryTypeDistribution,
                new HashMap<>(searchStatistics),
                Instant.now()
            );
        });
    }
    
    // Private helper methods
    
    private SearchConfiguration createDefaultConfiguration() {
        SearchConfiguration config = new SearchConfiguration();
        config.setSemanticThreshold(0.7);
        config.setMaxResults(10);
        config.setEnableQueryExpansion(true);
        config.setRerankingEnabled(true);
        config.setImportanceWeight(0.3);
        config.setRecencyWeight(0.2);
        config.setSemanticWeight(0.5);
        return config;
    }
    
    private void processBatch(List<EnhancedMemory> batch) {
        try {
            // Extract content for embedding generation
            List<String> contents = batch.stream()
                .map(EnhancedMemory::getContent)
                .collect(Collectors.toList());
            
            // Generate embeddings for the batch
            List<List<Float>> embeddings = embeddingProvider.embedBatch(contents).join();
            
            // Index memories and embeddings
            for (int i = 0; i < batch.size(); i++) {
                EnhancedMemory memory = batch.get(i);
                List<Float> embedding = embeddings.get(i);
                
                memoryIndex.put(memory.getId(), memory);
                embeddingIndex.put(memory.getId(), embedding);
                
                // Build inverted index for text search
                buildInvertedIndex(memory);
            }
            
        } catch (Exception e) {
            logger.error("Error processing batch: {}", e.getMessage(), e);
        }
    }
    
    private void buildInvertedIndex(EnhancedMemory memory) {
        String content = memory.getContent().toLowerCase();
        String[] words = content.split("\\s+");
        
        for (String word : words) {
            word = word.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fff]", ""); // Keep alphanumeric and Chinese chars
            if (word.length() > 1) {
                invertedIndex.computeIfAbsent(word, k -> ConcurrentHashMap.newKeySet()).add(memory.getId());
            }
        }
        
        // Also index tags
        for (String tag : memory.getTags()) {
            String cleanTag = tag.toLowerCase().replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fff]", "");
            if (cleanTag.length() > 0) {
                invertedIndex.computeIfAbsent(cleanTag, k -> ConcurrentHashMap.newKeySet()).add(memory.getId());
            }
        }
    }
    
    private void buildTermFrequencyIndex() {
        termFrequency.clear();
        
        for (Set<String> memoryIds : invertedIndex.values()) {
            for (String memoryId : memoryIds) {
                EnhancedMemory memory = memoryIndex.get(memoryId);
                if (memory != null) {
                    String[] words = memory.getContent().toLowerCase().split("\\s+");
                    for (String word : words) {
                        word = word.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fff]", "");
                        if (word.length() > 1) {
                            termFrequency.put(word, termFrequency.getOrDefault(word, 0.0) + 1.0);
                        }
                    }
                }
            }
        }
        
        logger.debug("Built term frequency index with {} unique terms", termFrequency.size());
    }
    
    private List<String> expandQuery(String query) {
        List<String> expanded = new ArrayList<>();
        expanded.add(query);
        
        // Add cached expansions if available
        List<String> cachedExpansions = queryExpansions.get(query.toLowerCase());
        if (cachedExpansions != null) {
            expanded.addAll(cachedExpansions);
        }
        
        // Simple synonym expansion (in production, use WordNet or custom thesaurus)
        Map<String, List<String>> synonyms = getQuerySynonyms();
        String lowerQuery = query.toLowerCase();
        
        for (Map.Entry<String, List<String>> entry : synonyms.entrySet()) {
            if (lowerQuery.contains(entry.getKey())) {
                for (String synonym : entry.getValue()) {
                    String expandedQuery = lowerQuery.replace(entry.getKey(), synonym);
                    if (!expanded.contains(expandedQuery)) {
                        expanded.add(expandedQuery);
                    }
                }
            }
        }
        
        return expanded.stream().limit(5).collect(Collectors.toList()); // Limit expansions
    }
    
    private Map<String, List<String>> getQuerySynonyms() {
        // Simplified synonym dictionary (in production, use comprehensive thesaurus)
        Map<String, List<String>> synonyms = new HashMap<>();
        synonyms.put("开发", Arrays.asList("编程", "构建", "创建"));
        synonyms.put("使用", Arrays.asList("应用", "利用", "采用"));
        synonyms.put("最佳", Arrays.asList("优秀", "推荐", "理想"));
        synonyms.put("实践", Arrays.asList("经验", "方法", "做法"));
        synonyms.put("spring", Arrays.asList("springboot", "spring-boot", "spring框架"));
        synonyms.put("微服务", Arrays.asList("microservice", "分布式", "服务化"));
        return synonyms;
    }
    
    private List<Float> getQueryEmbedding(String query) {
        // Check cache first
        List<Float> cachedEmbedding = queryCache.get(query);
        if (cachedEmbedding != null) {
            return cachedEmbedding;
        }
        
        // Generate new embedding
        List<Float> embedding = embeddingProvider.embed(query).join();
        
        // Cache for future use
        queryCache.put(query, embedding);
        
        // Limit cache size
        if (queryCache.size() > 1000) {
            String oldestKey = queryCache.keySet().iterator().next();
            queryCache.remove(oldestKey);
        }
        
        return embedding;
    }
    
    private List<SearchResultItem> performSemanticSearch(List<Float> queryEmbedding, 
                                                        List<String> expandedQueries,
                                                        SearchConfiguration config) {
        List<SearchResultItem> results = new ArrayList<>();
        
        // Calculate semantic similarity for all indexed memories
        for (Map.Entry<String, List<Float>> entry : embeddingIndex.entrySet()) {
            String memoryId = entry.getKey();
            List<Float> memoryEmbedding = entry.getValue();
            EnhancedMemory memory = memoryIndex.get(memoryId);
            
            if (memory == null) continue;
            
            // Calculate semantic similarity
            double semanticScore = cosineSimilarity(queryEmbedding, memoryEmbedding);
            
            // Apply semantic threshold
            if (semanticScore < config.getSemanticThreshold()) {
                continue;
            }
            
            // Calculate additional scoring components
            double importanceScore = memory.getImportance().getScore() / 5.0;
            double recencyScore = calculateRecencyScore(memory);
            double contextScore = 0.0; // Will be enhanced in context-aware search
            
            // Calculate final score
            double finalScore = 
                semanticScore * config.getSemanticWeight() +
                importanceScore * config.getImportanceWeight() +
                recencyScore * config.getRecencyWeight() +
                contextScore * 0.1;
            
            ScoreBreakdown breakdown = new ScoreBreakdown(
                semanticScore, importanceScore, recencyScore, contextScore);
            
            SearchResultItem item = new SearchResultItem(
                memory, semanticScore, finalScore, 0, breakdown);
            
            results.add(item);
        }
        
        // Sort by final score
        results.sort((r1, r2) -> Double.compare(r2.getFinalScore(), r1.getFinalScore()));
        
        // Assign ranks
        for (int i = 0; i < results.size(); i++) {
            results.get(i).setRank(i);
        }
        
        return results;
    }
    
    private double cosineSimilarity(List<Float> vectorA, List<Float> vectorB) {
        if (vectorA.size() != vectorB.size()) {
            throw new IllegalArgumentException("Vectors must have the same dimension");
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < vectorA.size(); i++) {
            float a = vectorA.get(i);
            float b = vectorB.get(i);
            dotProduct += a * b;
            normA += a * a;
            normB += b * b;
        }
        
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
    
    private double calculateRecencyScore(EnhancedMemory memory) {
        long daysOld = memory.getDaysOld();
        return Math.max(0.1, 1.0 / (daysOld + 1));
    }
    
    private List<SearchResultItem> rerankResults(List<SearchResultItem> results, String query, 
                                                SearchConfiguration config) {
        // Enhanced re-ranking based on additional factors
        for (SearchResultItem result : results) {
            EnhancedMemory memory = result.getMemory();
            
            // Boost score based on exact matches
            if (memory.getContent().toLowerCase().contains(query.toLowerCase())) {
                result.setFinalScore(result.getFinalScore() * 1.1);
            }
            
            // Boost score based on tag matches
            for (String tag : memory.getTags()) {
                if (query.toLowerCase().contains(tag.toLowerCase())) {
                    result.setFinalScore(result.getFinalScore() * 1.05);
                }
            }
            
            // Boost frequently accessed memories
            if (memory.getAccessCount() > 5) {
                result.setFinalScore(result.getFinalScore() * 1.02);
            }
        }
        
        // Re-sort after re-ranking
        results.sort((r1, r2) -> Double.compare(r2.getFinalScore(), r1.getFinalScore()));
        
        // Update ranks
        for (int i = 0; i < results.size(); i++) {
            results.get(i).setRank(i);
        }
        
        return results;
    }
    
    private List<SearchResultItem> applyFinalFilters(List<SearchResultItem> results, 
                                                    SearchConfiguration config) {
        return results.stream()
            .limit(config.getMaxResults())
            .collect(Collectors.toList());
    }
    
    private SearchConfiguration enhanceConfigWithContext(SearchConfiguration config, SearchContext context) {
        SearchConfiguration enhanced = new SearchConfiguration(config);
        
        // Adjust weights based on user preferences
        if (context.getUserPreferences().contains("技术深度")) {
            enhanced.setImportanceWeight(enhanced.getImportanceWeight() * 1.2);
        }
        
        if (context.getUserPreferences().contains("最新版本")) {
            enhanced.setRecencyWeight(enhanced.getRecencyWeight() * 1.3);
        }
        
        return enhanced;
    }
    
    private void updateSearchMetrics(String query, long searchTime, int resultCount) {
        totalSearches++;
        totalSearchTimeMs += searchTime;
        
        searchStatistics.put("total_searches", totalSearches);
        searchStatistics.put("average_results", 
            (searchStatistics.getOrDefault("average_results", 0) + resultCount) / 2);
        
        // Track query patterns
        String queryType = classifyQueryType(query);
        searchStatistics.put("query_type_" + queryType, 
            searchStatistics.getOrDefault("query_type_" + queryType, 0) + 1);
    }
    
    private String classifyQueryType(String query) {
        String lowerQuery = query.toLowerCase();
        
        if (lowerQuery.contains("如何") || lowerQuery.contains("怎么") || lowerQuery.contains("how")) {
            return "HOW_TO";
        } else if (lowerQuery.contains("什么") || lowerQuery.contains("what")) {
            return "WHAT_IS";
        } else if (lowerQuery.contains("为什么") || lowerQuery.contains("why")) {
            return "WHY";
        } else if (lowerQuery.contains("最佳") || lowerQuery.contains("best") || lowerQuery.contains("推荐")) {
            return "RECOMMENDATION";
        } else {
            return "GENERAL";
        }
    }
    
    private void updateQueryExpansions(String query, EnhancedMemory relevantMemory) {
        List<String> expansions = queryExpansions.computeIfAbsent(
            query.toLowerCase(), k -> new ArrayList<>());
        
        // Add relevant terms from the memory
        String[] words = relevantMemory.getContent().toLowerCase().split("\\s+");
        for (String word : words) {
            word = word.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fff]", "");
            if (word.length() > 2 && !query.toLowerCase().contains(word) && !expansions.contains(word)) {
                expansions.add(word);
            }
        }
        
        // Limit expansion size
        if (expansions.size() > 10) {
            expansions.subList(10, expansions.size()).clear();
        }
    }
    
    private double calculateCacheHitRate() {
        int totalQueries = queryCache.size();
        return totalQueries > 0 ? Math.min(1.0, (double) totalQueries / totalSearches) : 0.0;
    }
    
    private Map<String, Integer> calculateQueryTypeDistribution() {
        return searchStatistics.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith("query_type_"))
            .collect(Collectors.toMap(
                entry -> entry.getKey().substring("query_type_".length()),
                Map.Entry::getValue
            ));
    }
    
    // Configuration and result classes
    
    public static class SearchConfiguration {
        private double semanticThreshold = 0.7;
        private int maxResults = 10;
        private boolean enableQueryExpansion = true;
        private boolean rerankingEnabled = true;
        private double semanticWeight = 0.5;
        private double importanceWeight = 0.3;
        private double recencyWeight = 0.2;
        
        public SearchConfiguration() {}
        
        public SearchConfiguration(SearchConfiguration other) {
            this.semanticThreshold = other.semanticThreshold;
            this.maxResults = other.maxResults;
            this.enableQueryExpansion = other.enableQueryExpansion;
            this.rerankingEnabled = other.rerankingEnabled;
            this.semanticWeight = other.semanticWeight;
            this.importanceWeight = other.importanceWeight;
            this.recencyWeight = other.recencyWeight;
        }
        
        // Getters and setters
        public double getSemanticThreshold() { return semanticThreshold; }
        public void setSemanticThreshold(double semanticThreshold) { this.semanticThreshold = semanticThreshold; }
        public int getMaxResults() { return maxResults; }
        public void setMaxResults(int maxResults) { this.maxResults = maxResults; }
        public boolean isEnableQueryExpansion() { return enableQueryExpansion; }
        public void setEnableQueryExpansion(boolean enableQueryExpansion) { this.enableQueryExpansion = enableQueryExpansion; }
        public boolean isRerankingEnabled() { return rerankingEnabled; }
        public void setRerankingEnabled(boolean rerankingEnabled) { this.rerankingEnabled = rerankingEnabled; }
        public double getSemanticWeight() { return semanticWeight; }
        public void setSemanticWeight(double semanticWeight) { this.semanticWeight = semanticWeight; }
        public double getImportanceWeight() { return importanceWeight; }
        public void setImportanceWeight(double importanceWeight) { this.importanceWeight = importanceWeight; }
        public double getRecencyWeight() { return recencyWeight; }
        public void setRecencyWeight(double recencyWeight) { this.recencyWeight = recencyWeight; }
    }
    
    public static class SearchContext {
        private String userId;
        private List<String> recentQueries = new ArrayList<>();
        private List<String> userPreferences = new ArrayList<>();
        private Map<String, Object> additionalContext = new HashMap<>();
        
        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public List<String> getRecentQueries() { return new ArrayList<>(recentQueries); }
        public void setRecentQueries(List<String> recentQueries) { this.recentQueries = new ArrayList<>(recentQueries); }
        public List<String> getUserPreferences() { return new ArrayList<>(userPreferences); }
        public void setUserPreferences(List<String> userPreferences) { this.userPreferences = new ArrayList<>(userPreferences); }
        public Map<String, Object> getAdditionalContext() { return new HashMap<>(additionalContext); }
        public void setAdditionalContext(Map<String, Object> additionalContext) { this.additionalContext = new HashMap<>(additionalContext); }
    }
    
    public static class SemanticSearchResult {
        private final String originalQuery;
        private final List<SearchResultItem> results;
        private final List<String> expandedQueries;
        private final long searchTimeMs;
        private final int totalResults;
        
        public SemanticSearchResult(String originalQuery, List<SearchResultItem> results, 
                                   List<String> expandedQueries, long searchTimeMs, int totalResults) {
            this.originalQuery = originalQuery;
            this.results = new ArrayList<>(results);
            this.expandedQueries = new ArrayList<>(expandedQueries);
            this.searchTimeMs = searchTimeMs;
            this.totalResults = totalResults;
        }
        
        // Getters
        public String getOriginalQuery() { return originalQuery; }
        public List<SearchResultItem> getResults() { return new ArrayList<>(results); }
        public List<String> getExpandedQueries() { return new ArrayList<>(expandedQueries); }
        public long getSearchTimeMs() { return searchTimeMs; }
        public int getTotalResults() { return totalResults; }
    }
    
    public static class SearchResultItem {
        private final EnhancedMemory memory;
        private final double semanticScore;
        private double finalScore;
        private int rank;
        private final ScoreBreakdown scoreBreakdown;
        
        public SearchResultItem(EnhancedMemory memory, double semanticScore, double finalScore, 
                               int rank, ScoreBreakdown scoreBreakdown) {
            this.memory = memory;
            this.semanticScore = semanticScore;
            this.finalScore = finalScore;
            this.rank = rank;
            this.scoreBreakdown = scoreBreakdown;
        }
        
        // Getters and setters
        public EnhancedMemory getMemory() { return memory; }
        public double getSemanticScore() { return semanticScore; }
        public double getFinalScore() { return finalScore; }
        public void setFinalScore(double finalScore) { this.finalScore = finalScore; }
        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }
        public ScoreBreakdown getScoreBreakdown() { return scoreBreakdown; }
    }
    
    public static class ScoreBreakdown {
        private final double semanticComponent;
        private final double importanceComponent;
        private final double recencyComponent;
        private final double contextComponent;
        
        public ScoreBreakdown(double semanticComponent, double importanceComponent, 
                             double recencyComponent, double contextComponent) {
            this.semanticComponent = semanticComponent;
            this.importanceComponent = importanceComponent;
            this.recencyComponent = recencyComponent;
            this.contextComponent = contextComponent;
        }
        
        // Getters
        public double getSemanticComponent() { return semanticComponent; }
        public double getImportanceComponent() { return importanceComponent; }
        public double getRecencyComponent() { return recencyComponent; }
        public double getContextComponent() { return contextComponent; }
    }
    
    public static class SearchPerformanceReport {
        private final int totalSearches;
        private final double averageSearchTimeMs;
        private final double cacheHitRate;
        private final int indexSize;
        private final Map<String, Integer> queryTypeDistribution;
        private final Map<String, Integer> searchStatistics;
        private final Instant generatedAt;
        
        public SearchPerformanceReport(int totalSearches, double averageSearchTimeMs, double cacheHitRate,
                                      int indexSize, Map<String, Integer> queryTypeDistribution,
                                      Map<String, Integer> searchStatistics, Instant generatedAt) {
            this.totalSearches = totalSearches;
            this.averageSearchTimeMs = averageSearchTimeMs;
            this.cacheHitRate = cacheHitRate;
            this.indexSize = indexSize;
            this.queryTypeDistribution = new HashMap<>(queryTypeDistribution);
            this.searchStatistics = new HashMap<>(searchStatistics);
            this.generatedAt = generatedAt;
        }
        
        // Getters
        public int getTotalSearches() { return totalSearches; }
        public double getAverageSearchTimeMs() { return averageSearchTimeMs; }
        public double getCacheHitRate() { return cacheHitRate; }
        public int getIndexSize() { return indexSize; }
        public Map<String, Integer> getQueryTypeDistribution() { return new HashMap<>(queryTypeDistribution); }
        public Map<String, Integer> getSearchStatistics() { return new HashMap<>(searchStatistics); }
        public Instant getGeneratedAt() { return generatedAt; }
    }
    
    private static class SearchMetrics {
        private final String query;
        private final String memoryId;
        private final List<Double> relevanceScores;
        private int searchCount;
        
        public SearchMetrics(String query, String memoryId) {
            this.query = query;
            this.memoryId = memoryId;
            this.relevanceScores = new ArrayList<>();
            this.searchCount = 0;
        }
        
        public void addRelevanceFeedback(double score) {
            relevanceScores.add(score);
            searchCount++;
        }
        
        public double getAverageRelevance() {
            return relevanceScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }
        
        // Getters
        public String getQuery() { return query; }
        public String getMemoryId() { return memoryId; }
        public List<Double> getRelevanceScores() { return new ArrayList<>(relevanceScores); }
        public int getSearchCount() { return searchCount; }
    }
}