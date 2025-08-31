package com.mem0.multimodal;

import com.mem0.multimodal.MultimodalMemoryProcessor.ModalityProcessor;
import com.mem0.multimodal.MultimodalMemoryProcessor.ModalityProcessResult;
import com.mem0.multimodal.MultimodalMemoryProcessor.MultimodalConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DocumentProcessor - 文档处理器
 * 
 * 专门处理文档类型的多模态内容，支持PDF、Word文档、文本文件等格式。
 * 提供文档解析、内容提取、结构分析、关键词提取等功能。
 * 
 * 主要功能：
 * 1. 文档内容提取 - 提取文档中的纯文本内容
 * 2. 结构分析 - 识别标题、段落、列表等文档结构
 * 3. 关键词提取 - 提取文档的关键词和主题
 * 4. 语言识别 - 检测文档的主要语言
 * 5. 文档统计 - 字数、段落数、可读性评分等
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 2024-12-20
 */
public class DocumentProcessor implements ModalityProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessor.class);
    
    private final MultimodalConfiguration configuration;
    
    // 常见停用词（简化版本）
    private static final Set<String> STOP_WORDS = createStopWords();
    
    private static Set<String> createStopWords() {
        Set<String> stopWords = new HashSet<>();
        Collections.addAll(stopWords,
            "a", "an", "and", "are", "as", "at", "be", "by", "for", "from",
            "has", "he", "in", "is", "it", "its", "of", "on", "that", "the",
            "to", "was", "will", "with", "would", "could", "should", "can",
            "may", "might", "must", "shall", "this", "these", "those", "they",
            "them", "their", "there", "then", "than", "when", "where", "who",
            "what", "why", "how", "not", "no", "yes", "have", "had", "do",
            "does", "did", "get", "got", "go", "went", "come", "came", "make",
            "made", "take", "took", "see", "saw", "know", "knew", "think",
            "thought", "say", "said", "tell", "told", "give", "gave", "find",
            "found", "use", "used", "work", "worked", "call", "called", "try",
            "tried", "ask", "asked", "need", "needed", "feel", "felt", "become",
            "became", "leave", "left", "put", "keep", "kept", "let", "begin",
            "began", "seem", "seemed", "help", "helped", "talk", "talked",
            "turn", "turned", "start", "started", "show", "showed", "hear",
            "heard", "play", "played", "run", "ran", "move", "moved", "live",
            "lived", "believe", "believed", "bring", "brought", "happen",
            "happened", "write", "wrote", "provide", "provided", "sit", "sat",
            "stand", "stood", "lose", "lost", "add", "added", "change",
            "changed", "follow", "followed", "create", "created", "speak",
            "spoke", "read", "buy", "bought", "pay", "paid", "serve", "served",
            "die", "died", "send", "sent", "expect", "expected", "build",
            "built", "stay", "stayed", "fall", "fell", "cut", "carry", "carried"
        );
        return Collections.unmodifiableSet(stopWords);
    }
    
    // 中文停用词（简化版本）
    private static final Set<String> CHINESE_STOP_WORDS = createChineseStopWords();
    
    private static Set<String> createChineseStopWords() {
        Set<String> stopWords = new HashSet<>();
        Collections.addAll(stopWords,
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一",
            "个", "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有",
            "看", "好", "自己", "这", "那", "里", "来", "他", "时候", "过", "出", "还",
            "多", "么", "为", "又", "可", "家", "学", "只", "以", "主", "回", "然",
            "果", "发", "见", "心", "走", "定", "听", "觉", "太", "该", "当", "比",
            "或", "被", "此", "些", "您", "位", "今", "其", "存", "若"
        );
        return Collections.unmodifiableSet(stopWords);
    }
    
    /**
     * 构造函数
     * 
     * @param configuration 多模态配置
     */
    public DocumentProcessor(MultimodalConfiguration configuration) {
        this.configuration = configuration;
        logger.info("DocumentProcessor initialized");
    }
    
    @Override
    public CompletableFuture<ModalityProcessResult> processContent(byte[] content, String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                // 确定文档类型
                String contentType = determineContentType(filePath);
                
                // 提取文本内容
                String extractedText = extractTextContent(content, contentType);
                
                if (extractedText == null) {
                    extractedText = "";
                }
                
                // If extracted text is empty, this is valid for empty documents
                if (extractedText.trim().isEmpty()) {
                    logger.debug("Document appears to be empty: {}", filePath);
                }
                
                // 分析文档结构
                int contentLength = content != null ? content.length : 0;
                Map<String, Object> metadata = analyzeDocumentStructure(extractedText, filePath, contentLength);
                
                // 提取特征
                Map<String, Object> features = extractDocumentFeatures(extractedText).join();
                
                // 生成文档预览（缩略图替代）
                byte[] thumbnailData = null;
                if (configuration.isEnableThumbnailGeneration()) {
                    thumbnailData = generateDocumentPreview(extractedText);
                }
                
                long processingTime = System.currentTimeMillis() - startTime;
                
                // 添加处理统计到元数据
                metadata.put("processingTime", processingTime);
                metadata.put("textExtracted", true);
                metadata.put("textLength", extractedText.length());
                
                logger.debug("Processed document: {} in {}ms", filePath, processingTime);
                
                return new ModalityProcessResult(
                    contentType,
                    extractedText,
                    metadata,
                    features,
                    thumbnailData,
                    generateStorageKey(filePath),
                    processingTime
                );
                
            } catch (Exception e) {
                logger.error("Error processing document: " + filePath, e);
                throw new RuntimeException("Failed to process document", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Map<String, Object>> extractFeatures(byte[] content, String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String contentType = determineContentType(filePath);
                String text = extractTextContent(content, contentType);
                
                if (text == null || text.trim().isEmpty()) {
                    return new HashMap<>();
                }
                
                return extractDocumentFeatures(text).join();
                
            } catch (Exception e) {
                logger.error("Error extracting document features: " + filePath, e);
                return new HashMap<>();
            }
        });
    }
    
    @Override
    public CompletableFuture<byte[]> generateThumbnail(byte[] content, String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String contentType = determineContentType(filePath);
                String text = extractTextContent(content, contentType);
                
                if (text == null || text.trim().isEmpty()) {
                    return null;
                }
                
                return generateDocumentPreview(text);
                
            } catch (Exception e) {
                logger.error("Error generating document preview: " + filePath, e);
                return null;
            }
        });
    }
    
    /**
     * 提取文本内容
     * 
     * @param content 文档字节数据
     * @param contentType 内容类型
     * @return 提取的文本内容
     */
    private String extractTextContent(byte[] content, String contentType) {
        if (content == null || content.length == 0) {
            return "";
        }
        
        if (contentType.equals("text/plain") || contentType.equals("text/markdown")) {
            // 直接读取文本文件
            return new String(content, StandardCharsets.UTF_8);
        } else if (contentType.equals("application/pdf")) {
            // PDF文件处理（需要PDF库，这里返回占位符）
            return extractPdfText(content);
        } else if (contentType.equals("application/msword")) {
            // Word文档处理（需要POI库，这里返回占位符）
            return extractWordText(content);
        }
        
        // 尝试作为文本文件读取
        try {
            return new String(content, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.warn("Unable to extract text from content type: " + contentType);
            return null;
        }
    }
    
    /**
     * 提取PDF文本
     * 
     * @param content PDF字节数据
     * @return 提取的文本
     */
    private String extractPdfText(byte[] content) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content)) {
            // 使用Apache PDFBox提取PDF文本
            try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(inputStream)) {
                org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
                
                // 配置文本提取选项
                stripper.setSortByPosition(true);
                stripper.setLineSeparator("\n");
                stripper.setWordSeparator(" ");
                
                // 提取文本
                String extractedText = stripper.getText(document);
                
                if (extractedText == null || extractedText.trim().isEmpty()) {
                    logger.warn("PDF document appears to be empty or contains no extractable text");
                    return "";
                }
                
                // 清理和格式化文本
                String cleanedText = cleanExtractedText(extractedText);
                
                logger.debug("Successfully extracted {} characters from PDF ({} bytes)", 
                           cleanedText.length(), content.length);
                
                return cleanedText;
                
            } catch (java.io.IOException e) {
                logger.warn("IO error while extracting PDF text, treating as plain text: {}", e.getMessage());
                return new String(content, StandardCharsets.UTF_8);
            } catch (Exception e) {
                logger.warn("Error during PDF text extraction, treating as plain text: {}", e.getMessage());
                return new String(content, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            logger.warn("Error extracting PDF text, treating as plain text: {}", e.getMessage());
            return new String(content, StandardCharsets.UTF_8);
        }
    }
    
    /**
     * 提取Word文档文本
     * 
     * @param content Word文档字节数据
     * @return 提取的文本
     */
    private String extractWordText(byte[] content) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content)) {
            // 检测是否为新格式(.docx)或旧格式(.doc)
            String extractedText = null;
            
            try {
                // 首先尝试作为.docx文件处理
                extractedText = extractDocxText(inputStream);
            } catch (Exception e) {
                logger.debug("Failed to parse as DOCX, trying DOC format", e);
                
                // 重置输入流
                inputStream.reset();
                
                try {
                    // 尝试作为.doc文件处理
                    extractedText = extractDocText(inputStream);
                } catch (Exception docException) {
                    logger.warn("Failed to parse as both DOCX and DOC formats, treating as plain text: {}", docException.getMessage());
                    return new String(content, StandardCharsets.UTF_8);
                }
            }
            
            if (extractedText == null || extractedText.trim().isEmpty()) {
                logger.warn("Word document appears to be empty or contains no extractable text");
                return "";
            }
            
            // 清理和格式化文本
            String cleanedText = cleanExtractedText(extractedText);
            
            logger.debug("Successfully extracted {} characters from Word document ({} bytes)", 
                       cleanedText.length(), content.length);
            
            return cleanedText;
            
        } catch (Exception e) {
            logger.warn("Error extracting Word document text, treating as plain text: {}", e.getMessage());
            return new String(content, StandardCharsets.UTF_8);
        }
    }
    
    /**
     * 提取DOCX文档文本
     */
    private String extractDocxText(ByteArrayInputStream inputStream) throws Exception {
        try (org.apache.poi.xwpf.usermodel.XWPFDocument document = new org.apache.poi.xwpf.usermodel.XWPFDocument(inputStream)) {
            org.apache.poi.xwpf.extractor.XWPFWordExtractor extractor = new org.apache.poi.xwpf.extractor.XWPFWordExtractor(document);
            
            StringBuilder text = new StringBuilder();
            
            // 提取主要文档文本
            text.append(extractor.getText());
            
            // 提取表格内容
            for (org.apache.poi.xwpf.usermodel.XWPFTable table : document.getTables()) {
                for (org.apache.poi.xwpf.usermodel.XWPFTableRow row : table.getRows()) {
                    for (org.apache.poi.xwpf.usermodel.XWPFTableCell cell : row.getTableCells()) {
                        text.append(" ").append(cell.getText());
                    }
                    text.append("\n");
                }
            }
            
            // 提取页眉页脚
            for (org.apache.poi.xwpf.usermodel.XWPFHeader header : document.getHeaderList()) {
                text.append("\n").append(header.getText());
            }
            for (org.apache.poi.xwpf.usermodel.XWPFFooter footer : document.getFooterList()) {
                text.append("\n").append(footer.getText());
            }
            
            extractor.close();
            return text.toString();
        }
    }
    
    /**
     * 提取DOC文档文本  
     */
    private String extractDocText(ByteArrayInputStream inputStream) throws Exception {
        try (org.apache.poi.hwpf.HWPFDocument document = new org.apache.poi.hwpf.HWPFDocument(inputStream)) {
            org.apache.poi.hwpf.extractor.WordExtractor extractor = new org.apache.poi.hwpf.extractor.WordExtractor(document);
            
            StringBuilder text = new StringBuilder();
            
            // 提取主要文本
            text.append(extractor.getText());
            
            // 提取页眉页脚
            text.append("\n").append(extractor.getHeaderText());
            text.append("\n").append(extractor.getFooterText());
            
            extractor.close();
            return text.toString();
        }
    }
    
    /**
     * 清理提取的文本
     */
    private String cleanExtractedText(String text) {
        if (text == null) return "";
        
        return text
            // 统一换行符
            .replaceAll("\\r\\n", "\n")
            .replaceAll("\\r", "\n")
            // 移除多余的空白字符
            .replaceAll("[ \\t]+", " ")
            // 合并多个换行符为最多两个
            .replaceAll("\\n{3,}", "\n\n")
            // 移除行首行尾空格
            .replaceAll("(?m)^[ \\t]+|[ \\t]+$", "")
            // 整体trim
            .trim();
    }
    
    /**
     * 分析文档结构
     * 
     * @param text 文档文本
     * @param filePath 文件路径
     * @param fileSize 文件大小
     * @return 结构分析结果
     */
    private Map<String, Object> analyzeDocumentStructure(String text, String filePath, long fileSize) {
        Map<String, Object> structure = new HashMap<>();
        
        // 基本信息
        structure.put("fileSize", fileSize);
        structure.put("fileSizeKB", fileSize / 1024);
        structure.put("format", getDocumentFormat(filePath));
        structure.put("textLength", text.length());
        
        // 文本统计
        String[] lines = text.split("\\r?\\n");
        String[] words = text.split("\\s+");
        String[] sentences = text.split("[.!?]+");
        
        structure.put("lineCount", lines.length);
        structure.put("wordCount", words.length);
        structure.put("sentenceCount", sentences.length);
        structure.put("characterCount", text.length());
        structure.put("characterCountNoSpaces", text.replaceAll("\\s", "").length());
        
        // 平均值
        structure.put("averageWordsPerLine", lines.length > 0 ? (double) words.length / lines.length : 0);
        structure.put("averageWordsPerSentence", sentences.length > 0 ? (double) words.length / sentences.length : 0);
        structure.put("averageCharactersPerWord", words.length > 0 ? (double) text.length() / words.length : 0);
        
        // 结构元素检测
        structure.put("hasHeadings", detectHeadings(text));
        structure.put("hasBulletPoints", detectBulletPoints(text));
        structure.put("hasNumberedLists", detectNumberedLists(text));
        structure.put("hasUrls", detectUrls(text));
        structure.put("hasEmails", detectEmails(text));
        structure.put("hasPhoneNumbers", detectPhoneNumbers(text));
        structure.put("hasDates", detectDates(text));
        
        // 语言检测
        structure.put("detectedLanguage", detectLanguage(text));
        
        // 可读性评估
        structure.put("readabilityScore", calculateReadabilityScore(text, words.length, sentences.length));
        
        return structure;
    }
    
    /**
     * 提取文档特征
     * 
     * @param text 文档文本
     * @return 特征映射
     */
    private CompletableFuture<Map<String, Object>> extractDocumentFeatures(String text) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> features = new HashMap<>();
            
            try {
                // 关键词提取
                features.putAll(extractKeywords(text));
                
                // 主题分析
                features.putAll(analyzeTopics(text));
                
                // 情感分析（简化版本）
                features.putAll(analyzeSentiment(text));
                
                // 文档类型分类
                features.put("documentCategory", categorizeDocument(text));
                
                // 复杂度分析
                features.putAll(analyzeComplexity(text));
                
                logger.debug("Extracted {} document features", features.size());
                
            } catch (Exception e) {
                logger.error("Error extracting document features", e);
            }
            
            return features;
        });
    }
    
    /**
     * 提取关键词
     * 
     * @param text 文档文本
     * @return 关键词特征映射
     */
    private Map<String, Object> extractKeywords(String text) {
        Map<String, Object> features = new HashMap<>();
        
        // 词频统计
        Map<String, Integer> wordFreq = new HashMap<>();
        String[] words = text.toLowerCase()
            .replaceAll("[^a-zA-Z0-9\u4e00-\u9fa5\\s]", " ")
            .split("\\s+");
        
        for (String word : words) {
            if (word.length() > 2 && !isStopWord(word)) {
                wordFreq.merge(word, 1, Integer::sum);
            }
        }
        
        // 获取前10个高频词
        List<Map.Entry<String, Integer>> topWords = wordFreq.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10)
            .collect(ArrayList::new, (list, entry) -> list.add(entry), ArrayList::addAll);
        
        List<String> keywords = new ArrayList<>();
        List<Integer> frequencies = new ArrayList<>();
        
        for (Map.Entry<String, Integer> entry : topWords) {
            keywords.add(entry.getKey());
            frequencies.add(entry.getValue());
        }
        
        features.put("topKeywords", keywords);
        features.put("keywordFrequencies", frequencies);
        features.put("vocabularySize", wordFreq.size());
        features.put("totalWords", words.length);
        
        return features;
    }
    
    /**
     * 主题分析
     * 
     * @param text 文档文本
     * @return 主题分析结果
     */
    private Map<String, Object> analyzeTopics(String text) {
        Map<String, Object> topics = new HashMap<>();
        
        // 简化的主题检测（基于关键词）
        Map<String, List<String>> topicKeywords = new HashMap<>();
        topicKeywords.put("technology", Arrays.asList("computer", "software", "algorithm", "data", "system", "code", "programming", "digital", "internet", "web"));
        topicKeywords.put("business", Arrays.asList("company", "market", "profit", "sales", "customer", "strategy", "management", "finance", "investment", "revenue"));
        topicKeywords.put("science", Arrays.asList("research", "study", "analysis", "experiment", "theory", "discovery", "method", "result", "conclusion", "hypothesis"));
        topicKeywords.put("education", Arrays.asList("learn", "teach", "student", "school", "course", "lesson", "knowledge", "study", "education", "training"));
        topicKeywords.put("health", Arrays.asList("health", "medical", "doctor", "patient", "treatment", "disease", "medicine", "hospital", "care", "therapy"));
        
        String lowerText = text.toLowerCase();
        Map<String, Integer> topicScores = new HashMap<>();
        
        for (Map.Entry<String, List<String>> entry : topicKeywords.entrySet()) {
            String topic = entry.getKey();
            List<String> keywords = entry.getValue();
            
            int score = 0;
            for (String keyword : keywords) {
                score += countOccurrences(lowerText, keyword);
            }
            
            if (score > 0) {
                topicScores.put(topic, score);
            }
        }
        
        // 确定主要主题
        String primaryTopic = topicScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("general");
        
        topics.put("primaryTopic", primaryTopic);
        topics.put("topicScores", topicScores);
        
        return topics;
    }
    
    /**
     * 情感分析（简化版本）
     * 
     * @param text 文档文本
     * @return 情感分析结果
     */
    private Map<String, Object> analyzeSentiment(String text) {
        Map<String, Object> sentiment = new HashMap<>();
        
        // 简化的情感词典
        List<String> positiveWords = Arrays.asList(
            "good", "great", "excellent", "amazing", "wonderful", "fantastic", "positive", "success",
            "happy", "pleased", "satisfied", "love", "like", "enjoy", "benefit", "advantage",
            "improve", "better", "best", "perfect", "优秀", "很好", "满意", "喜欢", "成功"
        );
        
        List<String> negativeWords = Arrays.asList(
            "bad", "terrible", "awful", "horrible", "negative", "failure", "problem", "issue",
            "sad", "angry", "disappointed", "hate", "dislike", "worst", "poor", "difficult",
            "challenge", "concern", "risk", "糟糕", "不好", "失败", "问题", "困难"
        );
        
        String lowerText = text.toLowerCase();
        
        int positiveCount = 0;
        int negativeCount = 0;
        
        for (String word : positiveWords) {
            positiveCount += countOccurrences(lowerText, word);
        }
        
        for (String word : negativeWords) {
            negativeCount += countOccurrences(lowerText, word);
        }
        
        int totalSentimentWords = positiveCount + negativeCount;
        
        String overallSentiment;
        double sentimentScore;
        
        if (totalSentimentWords == 0) {
            overallSentiment = "neutral";
            sentimentScore = 0.0;
        } else {
            sentimentScore = (double) (positiveCount - negativeCount) / totalSentimentWords;
            if (sentimentScore > 0.1) {
                overallSentiment = "positive";
            } else if (sentimentScore < -0.1) {
                overallSentiment = "negative";
            } else {
                overallSentiment = "neutral";
            }
        }
        
        sentiment.put("overallSentiment", overallSentiment);
        sentiment.put("sentimentScore", sentimentScore);
        sentiment.put("positiveWords", positiveCount);
        sentiment.put("negativeWords", negativeCount);
        
        return sentiment;
    }
    
    /**
     * 复杂度分析
     * 
     * @param text 文档文本
     * @return 复杂度分析结果
     */
    private Map<String, Object> analyzeComplexity(String text) {
        Map<String, Object> complexity = new HashMap<>();
        
        String[] words = text.split("\\s+");
        String[] sentences = text.split("[.!?]+");
        
        // 平均句长
        double avgSentenceLength = sentences.length > 0 ? (double) words.length / sentences.length : 0;
        
        // 长单词比例（>6个字符）
        long longWords = Arrays.stream(words)
            .mapToInt(String::length)
            .filter(length -> length > 6)
            .count();
        double longWordRatio = words.length > 0 ? (double) longWords / words.length : 0;
        
        // 复合句检测（简化）
        int complexSentences = 0;
        for (String sentence : sentences) {
            if (sentence.contains(",") && sentence.contains("and") || 
                sentence.contains("because") || sentence.contains("however") ||
                sentence.contains("therefore") || sentence.contains("although")) {
                complexSentences++;
            }
        }
        double complexSentenceRatio = sentences.length > 0 ? (double) complexSentences / sentences.length : 0;
        
        // 复杂度评分
        double complexityScore = (avgSentenceLength * 0.4 + longWordRatio * 0.4 + complexSentenceRatio * 0.2) * 100;
        
        String complexityLevel;
        if (complexityScore < 30) {
            complexityLevel = "simple";
        } else if (complexityScore < 60) {
            complexityLevel = "moderate";
        } else {
            complexityLevel = "complex";
        }
        
        complexity.put("averageSentenceLength", avgSentenceLength);
        complexity.put("longWordRatio", longWordRatio);
        complexity.put("complexSentenceRatio", complexSentenceRatio);
        complexity.put("complexityScore", complexityScore);
        complexity.put("complexityLevel", complexityLevel);
        
        return complexity;
    }
    
    /**
     * 生成文档预览
     * 
     * @param text 文档文本
     * @return 预览数据（文本摘要作为"缩略图"）
     */
    private byte[] generateDocumentPreview(String text) {
        try {
            // 生成文档摘要作为预览
            String preview = generateSummary(text);
            return preview.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Error generating document preview", e);
            return null;
        }
    }
    
    /**
     * 生成文档摘要
     * 
     * @param text 原文本
     * @return 摘要文本
     */
    private String generateSummary(String text) {
        // 简化的摘要生成：取前几个句子
        String[] sentences = text.split("[.!?]+");
        
        if (sentences.length <= 3) {
            return text;
        }
        
        StringBuilder summary = new StringBuilder();
        int maxSentences = Math.min(3, sentences.length);
        
        for (int i = 0; i < maxSentences; i++) {
            summary.append(sentences[i].trim());
            if (i < maxSentences - 1) {
                summary.append(". ");
            }
        }
        
        summary.append("...");
        
        return summary.toString();
    }
    
    // 辅助检测方法
    
    private boolean detectHeadings(String text) {
        // 检测Markdown风格标题或大写行
        String[] lines = text.split("\\n");
        for (String line : lines) {
            if (line.trim().startsWith("#") || 
                (line.length() > 0 && line.equals(line.toUpperCase()) && line.length() < 100)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean detectBulletPoints(String text) {
        return text.contains("•") || text.contains("*") || text.contains("-") && text.contains("\n-");
    }
    
    private boolean detectNumberedLists(String text) {
        Pattern pattern = Pattern.compile("^\\d+[.)].+", Pattern.MULTILINE);
        return pattern.matcher(text).find();
    }
    
    private boolean detectUrls(String text) {
        Pattern pattern = Pattern.compile("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=]+");
        return pattern.matcher(text).find();
    }
    
    private boolean detectEmails(String text) {
        Pattern pattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
        return pattern.matcher(text).find();
    }
    
    private boolean detectPhoneNumbers(String text) {
        Pattern pattern = Pattern.compile("\\b\\d{3}[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b");
        return pattern.matcher(text).find();
    }
    
    private boolean detectDates(String text) {
        Pattern pattern = Pattern.compile("\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b|\\b\\d{4}[/-]\\d{1,2}[/-]\\d{1,2}\\b");
        return pattern.matcher(text).find();
    }
    
    private String detectLanguage(String text) {
        // 简化的语言检测
        long chineseChars = text.chars().filter(ch -> ch >= 0x4e00 && ch <= 0x9fff).count();
        long totalChars = text.replaceAll("\\s", "").length();
        
        if (totalChars > 0 && (double) chineseChars / totalChars > 0.3) {
            return "chinese";
        } else {
            return "english";
        }
    }
    
    private double calculateReadabilityScore(String text, int wordCount, int sentenceCount) {
        if (wordCount == 0 || sentenceCount == 0) return 0;
        
        // 简化的可读性评分（类似Flesch Reading Ease）
        double avgSentenceLength = (double) wordCount / sentenceCount;
        double score = 206.835 - (1.015 * avgSentenceLength);
        
        return Math.max(0, Math.min(100, score));
    }
    
    private String categorizeDocument(String text) {
        String lowerText = text.toLowerCase();
        
        if (lowerText.contains("abstract") && lowerText.contains("conclusion")) {
            return "academic_paper";
        } else if (lowerText.contains("report") || lowerText.contains("summary")) {
            return "report";
        } else if (lowerText.contains("manual") || lowerText.contains("instruction")) {
            return "manual";
        } else if (lowerText.contains("email") || lowerText.contains("dear") || lowerText.contains("sincerely")) {
            return "correspondence";
        } else if (lowerText.contains("contract") || lowerText.contains("agreement")) {
            return "legal";
        } else {
            return "general";
        }
    }
    
    private boolean isStopWord(String word) {
        return STOP_WORDS.contains(word.toLowerCase()) || 
               CHINESE_STOP_WORDS.contains(word);
    }
    
    private int countOccurrences(String text, String word) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(word, index)) != -1) {
            count++;
            index += word.length();
        }
        return count;
    }
    
    private String determineContentType(String filePath) {
        String extension = getFileExtension(filePath).toLowerCase();
        
        switch (extension) {
            case "pdf": return "application/pdf";
            case "doc":
            case "docx": return "application/msword";
            case "txt": return "text/plain";
            case "md": return "text/markdown";
            default: return "text/plain";
        }
    }
    
    private String generateStorageKey(String filePath) {
        return UUID.randomUUID().toString() + "_" + System.currentTimeMillis();
    }
    
    private String getFileExtension(String filePath) {
        int lastDot = filePath.lastIndexOf('.');
        return lastDot > 0 ? filePath.substring(lastDot + 1) : "";
    }
    
    private String getDocumentFormat(String filePath) {
        return getFileExtension(filePath).toUpperCase();
    }
    
    @Override
    public List<String> getSupportedFormats() {
        return configuration.getSupportedDocumentFormats();
    }
    
    @Override
    public String getProcessorName() {
        return "DocumentProcessor";
    }
}