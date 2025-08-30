package com.mem0.multimodal;

import com.mem0.memory.Memory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MultimodalMemoryProcessor - 多模态内存处理器
 * 
 * 支持图像、音频、视频、文档等多种类型内容的内存处理。
 * 提供统一的多模态内容存储、检索、分析和转换功能。
 * 
 * 主要功能：
 * 1. 多模态内容处理 - 图像、音频、视频、文档的统一处理
 * 2. 内容特征提取 - 自动提取各类内容的关键特征
 * 3. 跨模态搜索 - 支持文本查询多模态内容
 * 4. 内容转换 - 多模态内容间的智能转换
 * 5. 存储优化 - 高效的多模态内容存储和索引
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 2024-12-20
 */
public class MultimodalMemoryProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(MultimodalMemoryProcessor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // 多模态处理器
    private final Map<String, ModalityProcessor> modalityProcessors;
    private final MultimodalConfiguration configuration;
    private final MultimodalIndex multimodalIndex;
    private final ContentStorage contentStorage;
    
    /**
     * 多模态配置
     */
    public static class MultimodalConfiguration {
        private String storageBasePath = "./multimodal_storage";
        private boolean enableContentAnalysis = true;
        private boolean enableThumbnailGeneration = true;
        private boolean enableTranscription = true;
        private boolean enableOCR = true;
        private int maxFileSize = 100 * 1024 * 1024; // 100MB
        private List<String> supportedImageFormats = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp");
        private List<String> supportedAudioFormats = Arrays.asList("mp3", "wav", "aac", "flac");
        private List<String> supportedVideoFormats = Arrays.asList("mp4", "avi", "mov", "mkv");
        private List<String> supportedDocumentFormats = Arrays.asList("pdf", "doc", "docx", "txt", "md");
        
        // Getters and setters
        public String getStorageBasePath() { return storageBasePath; }
        public void setStorageBasePath(String storageBasePath) { this.storageBasePath = storageBasePath; }
        
        public boolean isEnableContentAnalysis() { return enableContentAnalysis; }
        public void setEnableContentAnalysis(boolean enableContentAnalysis) { this.enableContentAnalysis = enableContentAnalysis; }
        
        public boolean isEnableThumbnailGeneration() { return enableThumbnailGeneration; }
        public void setEnableThumbnailGeneration(boolean enableThumbnailGeneration) { this.enableThumbnailGeneration = enableThumbnailGeneration; }
        
        public boolean isEnableTranscription() { return enableTranscription; }
        public void setEnableTranscription(boolean enableTranscription) { this.enableTranscription = enableTranscription; }
        
        public boolean isEnableOCR() { return enableOCR; }
        public void setEnableOCR(boolean enableOCR) { this.enableOCR = enableOCR; }
        
        public int getMaxFileSize() { return maxFileSize; }
        public void setMaxFileSize(int maxFileSize) { this.maxFileSize = maxFileSize; }
        
        public List<String> getSupportedImageFormats() { return supportedImageFormats; }
        public void setSupportedImageFormats(List<String> supportedImageFormats) { this.supportedImageFormats = supportedImageFormats; }
        
        public List<String> getSupportedAudioFormats() { return supportedAudioFormats; }
        public void setSupportedAudioFormats(List<String> supportedAudioFormats) { this.supportedAudioFormats = supportedAudioFormats; }
        
        public List<String> getSupportedVideoFormats() { return supportedVideoFormats; }
        public void setSupportedVideoFormats(List<String> supportedVideoFormats) { this.supportedVideoFormats = supportedVideoFormats; }
        
        public List<String> getSupportedDocumentFormats() { return supportedDocumentFormats; }
        public void setSupportedDocumentFormats(List<String> supportedDocumentFormats) { this.supportedDocumentFormats = supportedDocumentFormats; }
    }
    
    /**
     * 模态处理器接口
     */
    public interface ModalityProcessor {
        /**
         * 处理内容
         * 
         * @param content 原始内容
         * @param filePath 文件路径
         * @return 处理结果
         */
        CompletableFuture<ModalityProcessResult> processContent(byte[] content, String filePath);
        
        /**
         * 提取特征
         * 
         * @param content 内容
         * @param filePath 文件路径
         * @return 特征向量
         */
        CompletableFuture<Map<String, Object>> extractFeatures(byte[] content, String filePath);
        
        /**
         * 生成缩略图或预览
         * 
         * @param content 内容
         * @param filePath 文件路径
         * @return 缩略图数据
         */
        CompletableFuture<byte[]> generateThumbnail(byte[] content, String filePath);
        
        /**
         * 获取支持的文件格式
         * 
         * @return 支持的格式列表
         */
        List<String> getSupportedFormats();
        
        /**
         * 获取处理器名称
         * 
         * @return 处理器名称
         */
        String getProcessorName();
    }
    
    /**
     * 模态处理结果
     */
    public static class ModalityProcessResult {
        private final String contentType;
        private final String extractedText;
        private final Map<String, Object> metadata;
        private final Map<String, Object> features;
        private final byte[] thumbnailData;
        private final String storageKey;
        private final long processingTime;
        
        public ModalityProcessResult(String contentType, String extractedText, 
                                   Map<String, Object> metadata, Map<String, Object> features,
                                   byte[] thumbnailData, String storageKey, long processingTime) {
            this.contentType = contentType;
            this.extractedText = extractedText;
            this.metadata = metadata != null ? metadata : new HashMap<>();
            this.features = features != null ? features : new HashMap<>();
            this.thumbnailData = thumbnailData;
            this.storageKey = storageKey;
            this.processingTime = processingTime;
        }
        
        // Getters
        public String getContentType() { return contentType; }
        public String getExtractedText() { return extractedText; }
        public Map<String, Object> getMetadata() { return metadata; }
        public Map<String, Object> getFeatures() { return features; }
        public byte[] getThumbnailData() { return thumbnailData; }
        public String getStorageKey() { return storageKey; }
        public long getProcessingTime() { return processingTime; }
    }
    
    /**
     * 多模态内存
     */
    public static class MultimodalMemory extends Memory {
        private final String contentType;
        private final String originalFileName;
        private final long fileSize;
        private final String storageKey;
        private final Map<String, Object> modalityFeatures;
        private final byte[] thumbnailData;
        private final String extractedText;
        
        public MultimodalMemory(String id, String content, String userId, String sessionId,
                               String contentType, String originalFileName, long fileSize,
                               String storageKey, Map<String, Object> modalityFeatures,
                               byte[] thumbnailData, String extractedText) {
            super(id, content, userId, sessionId);
            this.contentType = contentType;
            this.originalFileName = originalFileName;
            this.fileSize = fileSize;
            this.storageKey = storageKey;
            this.modalityFeatures = modalityFeatures != null ? modalityFeatures : new HashMap<>();
            this.thumbnailData = thumbnailData;
            this.extractedText = extractedText;
            
            // 设置多模态相关的元数据
            getMetadata().put("contentType", contentType);
            getMetadata().put("originalFileName", originalFileName);
            getMetadata().put("fileSize", fileSize);
            getMetadata().put("storageKey", storageKey);
            getMetadata().put("hasExtractedText", extractedText != null && !extractedText.isEmpty());
            getMetadata().put("hasThumbnail", thumbnailData != null);
        }
        
        // Getters
        public String getContentType() { return contentType; }
        public String getOriginalFileName() { return originalFileName; }
        public long getFileSize() { return fileSize; }
        public String getStorageKey() { return storageKey; }
        public Map<String, Object> getModalityFeatures() { return modalityFeatures; }
        public byte[] getThumbnailData() { return thumbnailData; }
        public String getExtractedText() { return extractedText; }
    }
    
    /**
     * 多模态索引
     */
    public static class MultimodalIndex {
        private final Map<String, Set<String>> contentTypeIndex = new ConcurrentHashMap<>();
        private final Map<String, Set<String>> featureIndex = new ConcurrentHashMap<>();
        private final Map<String, String> memoryToStorageKey = new ConcurrentHashMap<>();
        
        public void addMemory(MultimodalMemory memory) {
            String memoryId = memory.getId();
            
            // 内容类型索引
            contentTypeIndex.computeIfAbsent(memory.getContentType(), k -> ConcurrentHashMap.newKeySet())
                           .add(memoryId);
            
            // 特征索引
            for (String feature : memory.getModalityFeatures().keySet()) {
                featureIndex.computeIfAbsent(feature, k -> ConcurrentHashMap.newKeySet())
                           .add(memoryId);
            }
            
            // 存储键映射
            memoryToStorageKey.put(memoryId, memory.getStorageKey());
        }
        
        public void removeMemory(String memoryId) {
            // 从内容类型索引中移除
            contentTypeIndex.values().forEach(set -> set.remove(memoryId));
            
            // 从特征索引中移除
            featureIndex.values().forEach(set -> set.remove(memoryId));
            
            // 从存储键映射中移除
            memoryToStorageKey.remove(memoryId);
        }
        
        public Set<String> getMemoriesByContentType(String contentType) {
            return contentTypeIndex.getOrDefault(contentType, new HashSet<>());
        }
        
        public Set<String> getMemoriesByFeature(String feature) {
            return featureIndex.getOrDefault(feature, new HashSet<>());
        }
        
        public String getStorageKey(String memoryId) {
            return memoryToStorageKey.get(memoryId);
        }
        
        // 统计方法
        public Map<String, Integer> getContentTypeStatistics() {
            return contentTypeIndex.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue().size()
                ));
        }
        
        public int getTotalMemoryCount() {
            return memoryToStorageKey.size();
        }
    }
    
    /**
     * 内容存储
     */
    public static class ContentStorage {
        private final String basePath;
        
        public ContentStorage(String basePath) {
            this.basePath = basePath;
            createDirectoryIfNotExists(basePath);
        }
        
        public CompletableFuture<String> storeContent(byte[] content, String fileName, String contentType) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    String storageKey = generateStorageKey(fileName, contentType);
                    String subdirectory = getSubdirectory(contentType);
                    Path fullPath = Paths.get(basePath, subdirectory, storageKey);
                    
                    createDirectoryIfNotExists(fullPath.getParent().toString());
                    Files.write(fullPath, content);
                    
                    logger.debug("Stored content with key: {}", storageKey);
                    return storageKey;
                    
                } catch (IOException e) {
                    logger.error("Error storing content", e);
                    throw new RuntimeException("Failed to store content", e);
                }
            });
        }
        
        public CompletableFuture<byte[]> retrieveContent(String storageKey, String contentType) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    String subdirectory = getSubdirectory(contentType);
                    Path fullPath = Paths.get(basePath, subdirectory, storageKey);
                    
                    if (!Files.exists(fullPath)) {
                        throw new RuntimeException("Content not found: " + storageKey);
                    }
                    
                    return Files.readAllBytes(fullPath);
                    
                } catch (IOException e) {
                    logger.error("Error retrieving content: " + storageKey, e);
                    throw new RuntimeException("Failed to retrieve content", e);
                }
            });
        }
        
        public CompletableFuture<Boolean> deleteContent(String storageKey, String contentType) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    String subdirectory = getSubdirectory(contentType);
                    Path fullPath = Paths.get(basePath, subdirectory, storageKey);
                    
                    if (Files.exists(fullPath)) {
                        Files.delete(fullPath);
                        logger.debug("Deleted content: {}", storageKey);
                        return true;
                    }
                    return false;
                    
                } catch (IOException e) {
                    logger.error("Error deleting content: " + storageKey, e);
                    return false;
                }
            });
        }
        
        private String generateStorageKey(String fileName, String contentType) {
            String timestamp = String.valueOf(System.currentTimeMillis());
            String uuid = UUID.randomUUID().toString().substring(0, 8);
            String extension = getFileExtension(fileName);
            return String.format("%s_%s_%s.%s", timestamp, uuid, contentType, extension);
        }
        
        private String getSubdirectory(String contentType) {
            if (contentType.startsWith("image/")) return "images";
            if (contentType.startsWith("audio/")) return "audio";
            if (contentType.startsWith("video/")) return "video";
            if (contentType.startsWith("application/") || contentType.startsWith("text/")) return "documents";
            return "others";
        }
        
        private String getFileExtension(String fileName) {
            int lastDot = fileName.lastIndexOf('.');
            return lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "dat";
        }
        
        private void createDirectoryIfNotExists(String path) {
            File directory = new File(path);
            if (!directory.exists()) {
                directory.mkdirs();
            }
        }
        
        // 存储统计
        public Map<String, Object> getStorageStatistics() {
            Map<String, Object> stats = new HashMap<>();
            
            try {
                File baseDir = new File(basePath);
                if (baseDir.exists()) {
                    stats.put("totalFiles", countFiles(baseDir));
                    stats.put("totalSize", calculateSize(baseDir));
                    
                    // 各类型文件统计
                    Map<String, Integer> typeStats = new HashMap<>();
                    for (String subdir : Arrays.asList("images", "audio", "video", "documents", "others")) {
                        File subFile = new File(baseDir, subdir);
                        if (subFile.exists()) {
                            typeStats.put(subdir, countFiles(subFile));
                        }
                    }
                    stats.put("typeBreakdown", typeStats);
                }
            } catch (Exception e) {
                logger.error("Error calculating storage statistics", e);
            }
            
            return stats;
        }
        
        private int countFiles(File directory) {
            if (!directory.isDirectory()) return 0;
            
            File[] files = directory.listFiles();
            if (files == null) return 0;
            
            int count = 0;
            for (File file : files) {
                if (file.isFile()) {
                    count++;
                } else if (file.isDirectory()) {
                    count += countFiles(file);
                }
            }
            return count;
        }
        
        private long calculateSize(File directory) {
            if (!directory.isDirectory()) return directory.length();
            
            long size = 0;
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        size += file.length();
                    } else if (file.isDirectory()) {
                        size += calculateSize(file);
                    }
                }
            }
            return size;
        }
    }
    
    /**
     * 构造函数
     * 
     * @param configuration 多模态配置
     */
    public MultimodalMemoryProcessor(MultimodalConfiguration configuration) {
        this.configuration = configuration != null ? configuration : new MultimodalConfiguration();
        this.modalityProcessors = new ConcurrentHashMap<>();
        this.multimodalIndex = new MultimodalIndex();
        this.contentStorage = new ContentStorage(this.configuration.getStorageBasePath());
        
        // 初始化处理器
        initializeProcessors();
        
        logger.info("MultimodalMemoryProcessor initialized with {} processors", 
                   modalityProcessors.size());
    }
    
    /**
     * 默认构造函数
     */
    public MultimodalMemoryProcessor() {
        this(new MultimodalConfiguration());
    }
    
    /**
     * 初始化处理器
     */
    private void initializeProcessors() {
        // 注册图像处理器
        modalityProcessors.put("image", new ImageProcessor(configuration));
        
        // 注册音频处理器
        modalityProcessors.put("audio", new AudioProcessor(configuration));
        
        // 注册视频处理器
        modalityProcessors.put("video", new VideoProcessor(configuration));
        
        // 注册文档处理器
        modalityProcessors.put("document", new DocumentProcessor(configuration));
        
        logger.info("Initialized {} modality processors", modalityProcessors.size());
    }
    
    /**
     * 处理多模态内容
     * 
     * @param content 原始内容
     * @param fileName 文件名
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @return 多模态内存对象
     */
    public CompletableFuture<MultimodalMemory> processMultimodalContent(byte[] content, String fileName,
                                                                       String userId, String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                // 检查文件大小
                if (content.length > configuration.getMaxFileSize()) {
                    throw new RuntimeException("File size exceeds maximum limit: " + content.length);
                }
                
                // 确定内容类型
                String contentType = determineContentType(fileName);
                String modalityType = getModalityType(contentType);
                
                // 获取对应的处理器
                ModalityProcessor processor = modalityProcessors.get(modalityType);
                if (processor == null) {
                    throw new RuntimeException("Unsupported content type: " + contentType);
                }
                
                // 处理内容
                ModalityProcessResult result = processor.processContent(content, fileName).join();
                
                // 存储原始内容
                String storageKey = contentStorage.storeContent(content, fileName, contentType).join();
                
                // 创建多模态内存对象
                String memoryId = UUID.randomUUID().toString();
                String memoryContent = result.getExtractedText() != null ? 
                    result.getExtractedText() : "Multimodal content: " + fileName;
                
                MultimodalMemory memory = new MultimodalMemory(
                    memoryId,
                    memoryContent,
                    userId,
                    sessionId,
                    contentType,
                    fileName,
                    content.length,
                    storageKey,
                    result.getFeatures(),
                    result.getThumbnailData(),
                    result.getExtractedText()
                );
                
                // 添加处理结果到元数据
                memory.getMetadata().putAll(result.getMetadata());
                memory.getMetadata().put("processingTime", result.getProcessingTime());
                memory.getMetadata().put("totalProcessingTime", System.currentTimeMillis() - startTime);
                
                // 添加到索引
                multimodalIndex.addMemory(memory);
                
                logger.info("Processed multimodal content: {} ({}ms)", fileName,
                           System.currentTimeMillis() - startTime);
                
                return memory;
                
            } catch (Exception e) {
                logger.error("Error processing multimodal content: " + fileName, e);
                throw new RuntimeException("Failed to process multimodal content", e);
            }
        });
    }
    
    /**
     * 检索多模态内容
     * 
     * @param storageKey 存储键
     * @param contentType 内容类型
     * @return 原始内容数据
     */
    public CompletableFuture<byte[]> retrieveMultimodalContent(String storageKey, String contentType) {
        return contentStorage.retrieveContent(storageKey, contentType);
    }
    
    /**
     * 按内容类型搜索
     * 
     * @param contentType 内容类型
     * @return 内存ID集合
     */
    public Set<String> searchByContentType(String contentType) {
        return multimodalIndex.getMemoriesByContentType(contentType);
    }
    
    /**
     * 按特征搜索
     * 
     * @param feature 特征名称
     * @return 内存ID集合
     */
    public Set<String> searchByFeature(String feature) {
        return multimodalIndex.getMemoriesByFeature(feature);
    }
    
    /**
     * 删除多模态内存
     * 
     * @param memory 多模态内存对象
     * @return 删除是否成功
     */
    public CompletableFuture<Boolean> deleteMultimodalMemory(MultimodalMemory memory) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 从索引中移除
                multimodalIndex.removeMemory(memory.getId());
                
                // 删除存储的内容
                boolean deleted = contentStorage.deleteContent(memory.getStorageKey(), 
                                                              memory.getContentType()).join();
                
                logger.info("Deleted multimodal memory: {} ({})", memory.getId(), deleted);
                return deleted;
                
            } catch (Exception e) {
                logger.error("Error deleting multimodal memory: " + memory.getId(), e);
                return false;
            }
        });
    }
    
    /**
     * 确定内容类型
     * 
     * @param fileName 文件名
     * @return 内容类型
     */
    private String determineContentType(String fileName) {
        String extension = getFileExtension(fileName).toLowerCase();
        
        if (configuration.getSupportedImageFormats().contains(extension)) {
            return "image/" + extension;
        }
        if (configuration.getSupportedAudioFormats().contains(extension)) {
            return "audio/" + extension;
        }
        if (configuration.getSupportedVideoFormats().contains(extension)) {
            return "video/" + extension;
        }
        if (configuration.getSupportedDocumentFormats().contains(extension)) {
            if (extension.equals("pdf")) return "application/pdf";
            if (extension.equals("doc") || extension.equals("docx")) return "application/msword";
            if (extension.equals("txt")) return "text/plain";
            if (extension.equals("md")) return "text/markdown";
            return "application/octet-stream";
        }
        
        return "application/octet-stream";
    }
    
    /**
     * 获取模态类型
     * 
     * @param contentType 内容类型
     * @return 模态类型
     */
    private String getModalityType(String contentType) {
        if (contentType.startsWith("image/")) return "image";
        if (contentType.startsWith("audio/")) return "audio";
        if (contentType.startsWith("video/")) return "video";
        if (contentType.startsWith("application/") || contentType.startsWith("text/")) return "document";
        return "unknown";
    }
    
    /**
     * 获取文件扩展名
     * 
     * @param fileName 文件名
     * @return 扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }
    
    /**
     * 获取存储统计信息
     * 
     * @return 统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 索引统计
        stats.put("totalMemories", multimodalIndex.getTotalMemoryCount());
        stats.put("contentTypeBreakdown", multimodalIndex.getContentTypeStatistics());
        
        // 存储统计
        stats.putAll(contentStorage.getStorageStatistics());
        
        // 处理器统计
        Map<String, String> processorInfo = modalityProcessors.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().getProcessorName()
            ));
        stats.put("availableProcessors", processorInfo);
        
        return stats;
    }
    
    // Getters
    public MultimodalConfiguration getConfiguration() { return configuration; }
    public MultimodalIndex getMultimodalIndex() { return multimodalIndex; }
    public ContentStorage getContentStorage() { return contentStorage; }
    public Map<String, ModalityProcessor> getModalityProcessors() { return modalityProcessors; }
}

// 模态处理器实现将在单独的文件中定义