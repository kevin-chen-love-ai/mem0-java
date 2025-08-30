package com.mem0.config;

import java.util.Arrays;
import java.util.List;

/**
 * MultimodalConfiguration - 多模态内存配置
 * 
 * 集中管理所有多模态内存处理相关的配置参数。
 * 包括图像、音频、视频、文档处理器的配置以及存储配置。
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 2024-12-20
 */
public class MultimodalConfiguration extends BaseConfiguration {
    
    public MultimodalConfiguration() {
        super("mem0.multimodal");
    }
    
    @Override
    protected void loadDefaultConfiguration() {
        // 存储配置
        setConfigValue("storage.basePath", "./multimodal_storage");
        setConfigValue("storage.maxFileSize", 100 * 1024 * 1024); // 100MB
        setConfigValue("storage.enableCompression", true);
        setConfigValue("storage.compressionQuality", 0.8);
        setConfigValue("storage.enableCaching", true);
        setConfigValue("storage.cacheSize", 1000);
        setConfigValue("storage.cacheTTLMinutes", 60);
        setConfigValue("storage.enableCleanup", true);
        setConfigValue("storage.cleanupIntervalHours", 24);
        setConfigValue("storage.retentionDays", 365);
        
        // 通用处理配置
        setConfigValue("processing.enableContentAnalysis", true);
        setConfigValue("processing.enableThumbnailGeneration", true);
        setConfigValue("processing.enableTranscription", true);
        setConfigValue("processing.enableOCR", true);
        setConfigValue("processing.enableFeatureExtraction", true);
        setConfigValue("processing.enableParallelProcessing", true);
        setConfigValue("processing.threadPoolSize", 4);
        setConfigValue("processing.processingTimeoutMs", 30000);
        setConfigValue("processing.maxRetryAttempts", 3);
        
        // 图像处理配置
        setConfigValue("image.supportedFormats", Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp"));
        setConfigValue("image.thumbnailWidth", 200);
        setConfigValue("image.thumbnailHeight", 200);
        setConfigValue("image.thumbnailFormat", "jpg");
        setConfigValue("image.thumbnailQuality", 0.8);
        setConfigValue("image.enableOCR", true);
        setConfigValue("image.enableColorAnalysis", true);
        setConfigValue("image.enableEdgeDetection", true);
        setConfigValue("image.enableQualityAssessment", true);
        setConfigValue("image.maxResolution", 4096);
        setConfigValue("image.enableMetadataExtraction", true);
        
        // 音频处理配置
        setConfigValue("audio.supportedFormats", Arrays.asList("mp3", "wav", "aac", "flac", "ogg"));
        setConfigValue("audio.enableTranscription", true);
        setConfigValue("audio.enableFeatureExtraction", true);
        setConfigValue("audio.enableNoiseReduction", false);
        setConfigValue("audio.enableVolumeNormalization", false);
        setConfigValue("audio.maxDurationSeconds", 3600); // 1 hour
        setConfigValue("audio.transcriptionLanguage", "auto");
        setConfigValue("audio.enableSpeakerDiarization", false);
        setConfigValue("audio.enableMoodDetection", false);
        
        // 视频处理配置
        setConfigValue("video.supportedFormats", Arrays.asList("mp4", "avi", "mov", "mkv", "wmv", "flv"));
        setConfigValue("video.enableKeyFrameExtraction", true);
        setConfigValue("video.keyFrameCount", 5);
        setConfigValue("video.thumbnailTimestamp", 0.1); // 10% into video
        setConfigValue("video.enableMotionDetection", true);
        setConfigValue("video.enableSceneDetection", true);
        setConfigValue("video.enableAudioExtraction", true);
        setConfigValue("video.maxResolution", "1920x1080");
        setConfigValue("video.maxFrameRate", 60);
        setConfigValue("video.maxDurationSeconds", 7200); // 2 hours
        
        // 文档处理配置
        setConfigValue("document.supportedFormats", Arrays.asList("pdf", "doc", "docx", "txt", "md", "rtf", "odt"));
        setConfigValue("document.enableTextExtraction", true);
        setConfigValue("document.enableStructureAnalysis", true);
        setConfigValue("document.enableKeywordExtraction", true);
        setConfigValue("document.enableSentimentAnalysis", true);
        setConfigValue("document.enableLanguageDetection", true);
        setConfigValue("document.enableSummaryGeneration", true);
        setConfigValue("document.maxPages", 1000);
        setConfigValue("document.maxTextLength", 1000000); // 1M characters
        setConfigValue("document.summaryMaxLength", 500);
        
        // 特征提取配置
        setConfigValue("features.enableVectorEmbedding", true);
        setConfigValue("features.vectorDimension", 512);
        setConfigValue("features.enableSimilaritySearch", true);
        setConfigValue("features.enableClustering", false);
        setConfigValue("features.enableClassification", false);
        setConfigValue("features.enableHashGeneration", true);
        setConfigValue("features.hashAlgorithm", "md5");
        
        // 缓存配置
        setConfigValue("cache.enableProcessingCache", true);
        setConfigValue("cache.enableFeatureCache", true);
        setConfigValue("cache.enableThumbnailCache", true);
        setConfigValue("cache.processingCacheSize", 1000);
        setConfigValue("cache.featureCacheSize", 5000);
        setConfigValue("cache.thumbnailCacheSize", 2000);
        setConfigValue("cache.defaultTTLMinutes", 60);
        
        // 性能配置
        setConfigValue("performance.enableMetrics", true);
        setConfigValue("performance.enableProfiling", false);
        setConfigValue("performance.enableResourceMonitoring", true);
        setConfigValue("performance.maxMemoryUsageMB", 2048);
        setConfigValue("performance.maxCPUUsagePercent", 80);
        setConfigValue("performance.enableLoadBalancing", true);
        
        // 安全配置
        setConfigValue("security.enableFileValidation", true);
        setConfigValue("security.enableVirusScanning", false);
        setConfigValue("security.enableContentFiltering", false);
        setConfigValue("security.allowedMimeTypes", Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp",
            "audio/mpeg", "audio/wav", "audio/aac", "audio/flac", "audio/ogg",
            "video/mp4", "video/avi", "video/quicktime", "video/x-msvideo",
            "application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain", "text/markdown"
        ));
        setConfigValue("security.enableAccessControl", false);
        setConfigValue("security.enableEncryption", false);
        
        // API集成配置
        setConfigValue("api.enableExternalOCR", false);
        setConfigValue("api.enableExternalSpeechToText", false);
        setConfigValue("api.enableExternalTranslation", false);
        setConfigValue("api.enableExternalAnalysis", false);
        setConfigValue("api.requestTimeoutMs", 30000);
        setConfigValue("api.maxRequestsPerMinute", 60);
        setConfigValue("api.enableRetry", true);
        setConfigValue("api.maxRetryAttempts", 3);
        setConfigValue("api.retryDelayMs", 1000);
    }
    
    @Override
    protected void validateConfiguration() throws IllegalArgumentException {
        // 验证存储配置
        if (getInt("storage.maxFileSize", 0) <= 0) {
            throw new IllegalArgumentException("storage.maxFileSize must be positive");
        }
        
        double compressionQuality = getDouble("storage.compressionQuality", 0.0);
        if (compressionQuality < 0.0 || compressionQuality > 1.0) {
            throw new IllegalArgumentException("storage.compressionQuality must be between 0.0 and 1.0");
        }
        
        if (getInt("storage.retentionDays", 0) <= 0) {
            throw new IllegalArgumentException("storage.retentionDays must be positive");
        }
        
        // 验证处理配置
        if (getInt("processing.threadPoolSize", 0) <= 0) {
            throw new IllegalArgumentException("processing.threadPoolSize must be positive");
        }
        
        if (getInt("processing.processingTimeoutMs", 0) <= 0) {
            throw new IllegalArgumentException("processing.processingTimeoutMs must be positive");
        }
        
        // 验证图像配置
        if (getInt("image.thumbnailWidth", 0) <= 0 || getInt("image.thumbnailHeight", 0) <= 0) {
            throw new IllegalArgumentException("image thumbnail dimensions must be positive");
        }
        
        if (getInt("image.maxResolution", 0) <= 0) {
            throw new IllegalArgumentException("image.maxResolution must be positive");
        }
        
        // 验证音频配置
        if (getInt("audio.maxDurationSeconds", 0) <= 0) {
            throw new IllegalArgumentException("audio.maxDurationSeconds must be positive");
        }
        
        // 验证视频配置
        if (getInt("video.keyFrameCount", 0) <= 0) {
            throw new IllegalArgumentException("video.keyFrameCount must be positive");
        }
        
        double thumbnailTimestamp = getDouble("video.thumbnailTimestamp", 0.0);
        if (thumbnailTimestamp < 0.0 || thumbnailTimestamp > 1.0) {
            throw new IllegalArgumentException("video.thumbnailTimestamp must be between 0.0 and 1.0");
        }
        
        // 验证文档配置
        if (getInt("document.maxPages", 0) <= 0) {
            throw new IllegalArgumentException("document.maxPages must be positive");
        }
        
        if (getInt("document.maxTextLength", 0) <= 0) {
            throw new IllegalArgumentException("document.maxTextLength must be positive");
        }
        
        // 验证特征配置
        if (getInt("features.vectorDimension", 0) <= 0) {
            throw new IllegalArgumentException("features.vectorDimension must be positive");
        }
        
        // 验证性能配置
        if (getInt("performance.maxMemoryUsageMB", 0) <= 0) {
            throw new IllegalArgumentException("performance.maxMemoryUsageMB must be positive");
        }
        
        int cpuUsage = getInt("performance.maxCPUUsagePercent", 0);
        if (cpuUsage <= 0 || cpuUsage > 100) {
            throw new IllegalArgumentException("performance.maxCPUUsagePercent must be between 1 and 100");
        }
    }
    
    // Storage 配置访问方法
    public String getStorageBasePath() {
        return getString("storage.basePath", "./multimodal_storage");
    }
    
    public int getMaxFileSize() {
        return getInt("storage.maxFileSize", 100 * 1024 * 1024);
    }
    
    public boolean isEnableCompression() {
        return getBoolean("storage.enableCompression", true);
    }
    
    public double getCompressionQuality() {
        return getDouble("storage.compressionQuality", 0.8);
    }
    
    public boolean isEnableCaching() {
        return getBoolean("storage.enableCaching", true);
    }
    
    public int getCacheSize() {
        return getInt("storage.cacheSize", 1000);
    }
    
    public int getCacheTTLMinutes() {
        return getInt("storage.cacheTTLMinutes", 60);
    }
    
    public boolean isEnableCleanup() {
        return getBoolean("storage.enableCleanup", true);
    }
    
    public int getCleanupIntervalHours() {
        return getInt("storage.cleanupIntervalHours", 24);
    }
    
    public int getRetentionDays() {
        return getInt("storage.retentionDays", 365);
    }
    
    // Processing 配置访问方法
    public boolean isEnableContentAnalysis() {
        return getBoolean("processing.enableContentAnalysis", true);
    }
    
    public boolean isEnableThumbnailGeneration() {
        return getBoolean("processing.enableThumbnailGeneration", true);
    }
    
    public boolean isEnableTranscription() {
        return getBoolean("processing.enableTranscription", true);
    }
    
    public boolean isEnableOCR() {
        return getBoolean("processing.enableOCR", true);
    }
    
    public boolean isEnableFeatureExtraction() {
        return getBoolean("processing.enableFeatureExtraction", true);
    }
    
    public boolean isEnableParallelProcessing() {
        return getBoolean("processing.enableParallelProcessing", true);
    }
    
    public int getThreadPoolSize() {
        return getInt("processing.threadPoolSize", 4);
    }
    
    public int getProcessingTimeoutMs() {
        return getInt("processing.processingTimeoutMs", 30000);
    }
    
    public int getMaxRetryAttempts() {
        return getInt("processing.maxRetryAttempts", 3);
    }
    
    // Image 配置访问方法
    @SuppressWarnings("unchecked")
    public List<String> getSupportedImageFormats() {
        return getConfigValue("image.supportedFormats", Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp"));
    }
    
    public int getThumbnailWidth() {
        return getInt("image.thumbnailWidth", 200);
    }
    
    public int getThumbnailHeight() {
        return getInt("image.thumbnailHeight", 200);
    }
    
    public String getThumbnailFormat() {
        return getString("image.thumbnailFormat", "jpg");
    }
    
    public double getThumbnailQuality() {
        return getDouble("image.thumbnailQuality", 0.8);
    }
    
    public boolean isEnableImageOCR() {
        return getBoolean("image.enableOCR", true);
    }
    
    public boolean isEnableColorAnalysis() {
        return getBoolean("image.enableColorAnalysis", true);
    }
    
    public boolean isEnableEdgeDetection() {
        return getBoolean("image.enableEdgeDetection", true);
    }
    
    public boolean isEnableQualityAssessment() {
        return getBoolean("image.enableQualityAssessment", true);
    }
    
    public int getMaxResolution() {
        return getInt("image.maxResolution", 4096);
    }
    
    public boolean isEnableMetadataExtraction() {
        return getBoolean("image.enableMetadataExtraction", true);
    }
    
    // Audio 配置访问方法
    @SuppressWarnings("unchecked")
    public List<String> getSupportedAudioFormats() {
        return getConfigValue("audio.supportedFormats", Arrays.asList("mp3", "wav", "aac", "flac", "ogg"));
    }
    
    public boolean isEnableAudioTranscription() {
        return getBoolean("audio.enableTranscription", true);
    }
    
    public boolean isEnableAudioFeatureExtraction() {
        return getBoolean("audio.enableFeatureExtraction", true);
    }
    
    public boolean isEnableNoiseReduction() {
        return getBoolean("audio.enableNoiseReduction", false);
    }
    
    public boolean isEnableVolumeNormalization() {
        return getBoolean("audio.enableVolumeNormalization", false);
    }
    
    public int getMaxDurationSeconds() {
        return getInt("audio.maxDurationSeconds", 3600);
    }
    
    public String getTranscriptionLanguage() {
        return getString("audio.transcriptionLanguage", "auto");
    }
    
    public boolean isEnableSpeakerDiarization() {
        return getBoolean("audio.enableSpeakerDiarization", false);
    }
    
    public boolean isEnableMoodDetection() {
        return getBoolean("audio.enableMoodDetection", false);
    }
    
    // Video 配置访问方法
    @SuppressWarnings("unchecked")
    public List<String> getSupportedVideoFormats() {
        return getConfigValue("video.supportedFormats", Arrays.asList("mp4", "avi", "mov", "mkv", "wmv", "flv"));
    }
    
    public boolean isEnableKeyFrameExtraction() {
        return getBoolean("video.enableKeyFrameExtraction", true);
    }
    
    public int getKeyFrameCount() {
        return getInt("video.keyFrameCount", 5);
    }
    
    public double getThumbnailTimestamp() {
        return getDouble("video.thumbnailTimestamp", 0.1);
    }
    
    public boolean isEnableMotionDetection() {
        return getBoolean("video.enableMotionDetection", true);
    }
    
    public boolean isEnableSceneDetection() {
        return getBoolean("video.enableSceneDetection", true);
    }
    
    public boolean isEnableAudioExtraction() {
        return getBoolean("video.enableAudioExtraction", true);
    }
    
    public String getMaxResolutionVideo() {
        return getString("video.maxResolution", "1920x1080");
    }
    
    public int getMaxFrameRate() {
        return getInt("video.maxFrameRate", 60);
    }
    
    public int getMaxVideoDurationSeconds() {
        return getInt("video.maxDurationSeconds", 7200);
    }
    
    // Document 配置访问方法
    @SuppressWarnings("unchecked")
    public List<String> getSupportedDocumentFormats() {
        return getConfigValue("document.supportedFormats", Arrays.asList("pdf", "doc", "docx", "txt", "md", "rtf", "odt"));
    }
    
    public boolean isEnableTextExtraction() {
        return getBoolean("document.enableTextExtraction", true);
    }
    
    public boolean isEnableStructureAnalysis() {
        return getBoolean("document.enableStructureAnalysis", true);
    }
    
    public boolean isEnableKeywordExtraction() {
        return getBoolean("document.enableKeywordExtraction", true);
    }
    
    public boolean isEnableSentimentAnalysis() {
        return getBoolean("document.enableSentimentAnalysis", true);
    }
    
    public boolean isEnableLanguageDetection() {
        return getBoolean("document.enableLanguageDetection", true);
    }
    
    public boolean isEnableSummaryGeneration() {
        return getBoolean("document.enableSummaryGeneration", true);
    }
    
    public int getMaxPages() {
        return getInt("document.maxPages", 1000);
    }
    
    public int getMaxTextLength() {
        return getInt("document.maxTextLength", 1000000);
    }
    
    public int getSummaryMaxLength() {
        return getInt("document.summaryMaxLength", 500);
    }
    
    // Features 配置访问方法
    public boolean isEnableVectorEmbedding() {
        return getBoolean("features.enableVectorEmbedding", true);
    }
    
    public int getVectorDimension() {
        return getInt("features.vectorDimension", 512);
    }
    
    public boolean isEnableSimilaritySearch() {
        return getBoolean("features.enableSimilaritySearch", true);
    }
    
    public boolean isEnableClustering() {
        return getBoolean("features.enableClustering", false);
    }
    
    public boolean isEnableClassification() {
        return getBoolean("features.enableClassification", false);
    }
    
    public boolean isEnableHashGeneration() {
        return getBoolean("features.enableHashGeneration", true);
    }
    
    public String getHashAlgorithm() {
        return getString("features.hashAlgorithm", "md5");
    }
    
    // Performance 配置访问方法
    public boolean isEnableMetrics() {
        return getBoolean("performance.enableMetrics", true);
    }
    
    public boolean isEnableProfiling() {
        return getBoolean("performance.enableProfiling", false);
    }
    
    public boolean isEnableResourceMonitoring() {
        return getBoolean("performance.enableResourceMonitoring", true);
    }
    
    public int getMaxMemoryUsageMB() {
        return getInt("performance.maxMemoryUsageMB", 2048);
    }
    
    public int getMaxCPUUsagePercent() {
        return getInt("performance.maxCPUUsagePercent", 80);
    }
    
    public boolean isEnableLoadBalancing() {
        return getBoolean("performance.enableLoadBalancing", true);
    }
    
    // Security 配置访问方法
    public boolean isEnableFileValidation() {
        return getBoolean("security.enableFileValidation", true);
    }
    
    public boolean isEnableVirusScanning() {
        return getBoolean("security.enableVirusScanning", false);
    }
    
    public boolean isEnableContentFiltering() {
        return getBoolean("security.enableContentFiltering", false);
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getAllowedMimeTypes() {
        return getConfigValue("security.allowedMimeTypes", Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp",
            "audio/mpeg", "audio/wav", "audio/aac", "audio/flac", "audio/ogg",
            "video/mp4", "video/avi", "video/quicktime", "video/x-msvideo",
            "application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain", "text/markdown"
        ));
    }
    
    public boolean isEnableAccessControl() {
        return getBoolean("security.enableAccessControl", false);
    }
    
    public boolean isEnableEncryption() {
        return getBoolean("security.enableEncryption", false);
    }
}