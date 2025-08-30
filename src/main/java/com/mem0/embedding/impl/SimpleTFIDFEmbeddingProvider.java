package com.mem0.embedding.impl;

import com.mem0.embedding.EmbeddingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 简单TF-IDF嵌入提供者 / Simple TF-IDF based embedding provider for text similarity
 * 
 * <p>实现了基于TF-IDF（词频-逆文档频率）算法的文本嵌入提供者，将文本转换为数值向量表示。
 * 使用简化的TF-IDF算法和哈希映射技术将稀疏向量投影到固定维度的密集向量空间中。</p>
 * 
 * <p>Implements a text embedding provider based on TF-IDF (Term Frequency-Inverse Document Frequency) 
 * algorithm to convert text into numerical vector representations. 
 * Uses simplified TF-IDF algorithm and hash mapping techniques to project sparse vectors 
 * into fixed-dimension dense vector space.</p>
 * 
 * <p>主要特性 / Key features:</p>
 * <ul>
 *   <li>TF-IDF向量化计算 / TF-IDF vectorization computation</li>
 *   <li>动态词汇表构建 / Dynamic vocabulary construction</li>
 *   <li>固定维度向量输出 / Fixed-dimension vector output</li>
 *   <li>批量文本嵌入处理 / Batch text embedding processing</li>
 *   <li>向量标准化和相似度计算 / Vector normalization and similarity computation</li>
 *   <li>语料库预训练支持 / Corpus pre-training support</li>
 * </ul>
 * 
 * <p>使用示例 / Usage example:</p>
 * <pre>{@code
 * // 创建TF-IDF嵌入提供者
 * SimpleTFIDFEmbeddingProvider provider = new SimpleTFIDFEmbeddingProvider(10000, 300);
 * 
 * // 单文本嵌入
 * List<Float> embedding = provider.embed("This is a sample text").join();
 * 
 * // 批量文本嵌入
 * List<String> texts = Arrays.asList("text1", "text2", "text3");
 * List<List<Float>> embeddings = provider.embedBatch(texts).join();
 * 
 * // 使用语料库预训练
 * provider.preTrainWithCorpus(trainingTexts);
 * }</pre>
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 1.0
 */
public class SimpleTFIDFEmbeddingProvider implements EmbeddingProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleTFIDFEmbeddingProvider.class);
    
    private final Map<String, Integer> vocabulary = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Double>> documentTermFrequencies = new ConcurrentHashMap<>();
    private final Map<String, Double> inverseDocumentFrequencies = new ConcurrentHashMap<>();
    private final int maxVocabularySize;
    private final int embeddingDimension;
    
    public SimpleTFIDFEmbeddingProvider() {
        this(10000, 300);
    }
    
    public SimpleTFIDFEmbeddingProvider(int maxVocabularySize, int embeddingDimension) {
        this.maxVocabularySize = maxVocabularySize;
        this.embeddingDimension = embeddingDimension;
        logger.info("Initialized SimpleTFIDFEmbeddingProvider with vocab size: {} and dimension: {}", 
                   maxVocabularySize, embeddingDimension);
    }
    
    @Override
    public CompletableFuture<List<Float>> embed(String text) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (text == null || text.trim().isEmpty()) {
                    return convertToList(createZeroVector());
                }
                
                logger.debug("Embedding text: {}", text.substring(0, Math.min(text.length(), 50)) + "...");
                
                List<String> tokens = tokenize(text);
                updateVocabulary(tokens);
                
                Map<String, Double> termFrequencies = calculateTermFrequencies(tokens);
                updateInverseDocumentFrequencies(termFrequencies.keySet());
                
                return convertToList(createTFIDFVector(termFrequencies));
            } catch (Exception e) {
                logger.error("Failed to embed text", e);
                return convertToList(createRandomVector()); // Fallback to random vector
            }
        });
    }
    
    @Override
    public CompletableFuture<List<List<Float>>> embedBatch(List<String> texts) {
        return CompletableFuture.<List<List<Float>>>supplyAsync(() -> {
            try {
                logger.debug("Embedding batch of {} texts", texts.size());
                
                List<List<Float>> embeddings = new ArrayList<>();
                for (String text : texts) {
                    embeddings.add(embed(text).join());
                }
                
                return embeddings;
            } catch (Exception e) {
                logger.error("Failed to embed batch", e);
                // Return random vectors as fallback
                List<List<Float>> result = new ArrayList<>();
                for (String text : texts) {
                    result.add(convertToList(createRandomVector()));
                }
                return result;
            }
        });
    }
    
    @Override
    public int getDimension() {
        return embeddingDimension;
    }
    
    @Override
    public void close() {
        logger.info("Closing SimpleTFIDFEmbeddingProvider");
        vocabulary.clear();
        documentTermFrequencies.clear();
        inverseDocumentFrequencies.clear();
    }
    
    private List<String> tokenize(String text) {
        // Simple tokenization: lowercase, split on non-alphanumeric, remove empty
        return Arrays.stream(text.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .split("\\s+"))
                .filter(token -> !token.isEmpty() && token.length() > 1)
                .collect(Collectors.toList());
    }
    
    private synchronized void updateVocabulary(List<String> tokens) {
        for (String token : tokens) {
            if (vocabulary.size() < maxVocabularySize && !vocabulary.containsKey(token)) {
                vocabulary.put(token, vocabulary.size());
            }
        }
    }
    
    private Map<String, Double> calculateTermFrequencies(List<String> tokens) {
        Map<String, Integer> termCounts = new HashMap<>();
        
        for (String token : tokens) {
            termCounts.put(token, termCounts.getOrDefault(token, 0) + 1);
        }
        
        Map<String, Double> termFrequencies = new HashMap<>();
        int totalTokens = tokens.size();
        
        for (Map.Entry<String, Integer> entry : termCounts.entrySet()) {
            double tf = (double) entry.getValue() / totalTokens;
            termFrequencies.put(entry.getKey(), tf);
        }
        
        return termFrequencies;
    }
    
    private synchronized void updateInverseDocumentFrequencies(Set<String> uniqueTerms) {
        // For simplicity, we'll use a basic IDF calculation
        // In a real implementation, you'd maintain document counts
        for (String term : uniqueTerms) {
            if (!inverseDocumentFrequencies.containsKey(term)) {
                // Simple IDF calculation based on term rarity
                double idf = Math.log(1.0 + (double) maxVocabularySize / (1.0 + vocabulary.getOrDefault(term, maxVocabularySize)));
                inverseDocumentFrequencies.put(term, idf);
            }
        }
    }
    
    private float[] createTFIDFVector(Map<String, Double> termFrequencies) {
        float[] vector = new float[embeddingDimension];
        
        // Create a sparse TF-IDF vector and then project to fixed dimension
        Map<Integer, Double> sparseVector = new HashMap<>();
        
        for (Map.Entry<String, Double> entry : termFrequencies.entrySet()) {
            String term = entry.getKey();
            Double tf = entry.getValue();
            
            Integer vocabularyIndex = vocabulary.get(term);
            if (vocabularyIndex != null) {
                double idf = inverseDocumentFrequencies.getOrDefault(term, 1.0);
                double tfidf = tf * idf;
                sparseVector.put(vocabularyIndex, tfidf);
            }
        }
        
        // Project sparse vector to fixed dimension using hash-based mapping
        for (Map.Entry<Integer, Double> entry : sparseVector.entrySet()) {
            int vocabIndex = entry.getKey();
            double value = entry.getValue();
            
            // Map vocabulary index to embedding dimension using modulo and hash
            int[] targetIndices = getTargetIndices(vocabIndex);
            
            for (int targetIndex : targetIndices) {
                if (targetIndex < embeddingDimension) {
                    vector[targetIndex] += (float) (value / targetIndices.length);
                }
            }
        }
        
        // Normalize the vector
        normalizeVector(vector);
        
        return vector;
    }
    
    private int[] getTargetIndices(int vocabIndex) {
        // Simple hash-based mapping to distribute vocabulary terms across embedding dimensions
        Random rand = new Random(vocabIndex); // Use vocab index as seed for reproducibility
        int numMappings = Math.min(5, embeddingDimension / 10); // Map each term to multiple dimensions
        
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
    
    private float[] createRandomVector() {
        Random rand = new Random();
        float[] vector = new float[embeddingDimension];
        
        // Create a random unit vector
        for (int i = 0; i < embeddingDimension; i++) {
            vector[i] = (float) rand.nextGaussian();
        }
        
        normalizeVector(vector);
        return vector;
    }
    
    // Additional utility methods
    public int getVocabularySize() {
        return vocabulary.size();
    }
    
    public Set<String> getVocabulary() {
        return new HashSet<>(vocabulary.keySet());
    }
    
    public void clearVocabulary() {
        vocabulary.clear();
        documentTermFrequencies.clear();
        inverseDocumentFrequencies.clear();
        logger.info("Vocabulary cleared");
    }
    
    /**
     * Pre-train the embedding provider with a corpus of texts
     */
    public void preTrainWithCorpus(List<String> corpus) {
        logger.info("Pre-training with corpus of {} documents", corpus.size());
        
        List<Map<String, Double>> allTermFrequencies = new ArrayList<>();
        
        // Process all documents to build vocabulary and term frequencies
        for (String document : corpus) {
            List<String> tokens = tokenize(document);
            updateVocabulary(tokens);
            Map<String, Double> termFreqs = calculateTermFrequencies(tokens);
            allTermFrequencies.add(termFreqs);
        }
        
        // Calculate IDF values based on document frequency
        Map<String, Integer> documentFrequencies = new HashMap<>();
        for (Map<String, Double> termFreqs : allTermFrequencies) {
            for (String term : termFreqs.keySet()) {
                documentFrequencies.put(term, documentFrequencies.getOrDefault(term, 0) + 1);
            }
        }
        
        // Update IDF values
        int totalDocuments = corpus.size();
        for (Map.Entry<String, Integer> entry : documentFrequencies.entrySet()) {
            String term = entry.getKey();
            int docFreq = entry.getValue();
            double idf = Math.log((double) totalDocuments / (1.0 + docFreq));
            inverseDocumentFrequencies.put(term, idf);
        }
        
        logger.info("Pre-training completed. Vocabulary size: {}, IDF entries: {}", 
                   vocabulary.size(), inverseDocumentFrequencies.size());
    }
    
    @Override
    public String getProviderName() {
        return "Simple-TFIDF";
    }
    
    @Override
    public boolean isHealthy() {
        return !vocabulary.isEmpty();
    }
    
    
    // Helper methods
    private List<Float> convertToList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float value : array) {
            list.add(value);
        }
        return list;
    }
}