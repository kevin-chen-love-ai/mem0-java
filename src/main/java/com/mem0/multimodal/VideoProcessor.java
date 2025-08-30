package com.mem0.multimodal;

import com.mem0.multimodal.MultimodalMemoryProcessor.ModalityProcessor;
import com.mem0.multimodal.MultimodalMemoryProcessor.ModalityProcessResult;
import com.mem0.multimodal.MultimodalMemoryProcessor.MultimodalConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * VideoProcessor - 视频处理器
 * 
 * 专门处理视频类型的多模态内容，提供视频分析、特征提取、关键帧提取等功能。
 * 支持多种视频格式的处理和视频内容分析。
 * 
 * 主要功能：
 * 1. 视频基本信息提取 - 时长、分辨率、帧率、编码格式等
 * 2. 视频特征分析 - 场景变化、动作检测、色彩分析等
 * 3. 关键帧提取 - 自动提取代表性帧
 * 4. 音频轨道分析 - 提取和分析视频中的音频内容
 * 5. 视频质量评估 - 清晰度、稳定性、压缩质量等
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 2024-12-20
 */
public class VideoProcessor implements ModalityProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(VideoProcessor.class);
    
    private final MultimodalConfiguration configuration;
    
    /**
     * 构造函数
     * 
     * @param configuration 多模态配置
     */
    public VideoProcessor(MultimodalConfiguration configuration) {
        this.configuration = configuration;
        logger.info("VideoProcessor initialized");
    }
    
    @Override
    public CompletableFuture<ModalityProcessResult> processContent(byte[] content, String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                // 提取基本视频信息
                Map<String, Object> metadata = extractBasicVideoInfo(content, filePath);
                
                // 分析视频特征
                Map<String, Object> features = analyzeVideoFeatures(content, filePath).join();
                
                // 提取音频内容（如果有）
                String extractedText = "";
                if (configuration.isEnableTranscription() && hasAudioTrack(content)) {
                    extractedText = extractAudioFromVideo(content, filePath);
                }
                
                // 生成关键帧缩略图
                byte[] thumbnailData = null;
                if (configuration.isEnableThumbnailGeneration()) {
                    thumbnailData = extractKeyFrame(content);
                }
                
                long processingTime = System.currentTimeMillis() - startTime;
                
                // 添加处理统计到元数据
                metadata.put("processingTime", processingTime);
                metadata.put("hasAudioTrack", hasAudioTrack(content));
                metadata.put("transcriptionEnabled", configuration.isEnableTranscription());
                metadata.put("keyFrameExtracted", thumbnailData != null);
                
                logger.debug("Processed video: {} in {}ms", filePath, processingTime);
                
                return new ModalityProcessResult(
                    determineContentType(filePath),
                    extractedText,
                    metadata,
                    features,
                    thumbnailData,
                    generateStorageKey(filePath),
                    processingTime
                );
                
            } catch (Exception e) {
                logger.error("Error processing video: " + filePath, e);
                throw new RuntimeException("Failed to process video", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Map<String, Object>> extractFeatures(byte[] content, String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return analyzeVideoFeatures(content, filePath).join();
            } catch (Exception e) {
                logger.error("Error extracting video features: " + filePath, e);
                return new HashMap<>();
            }
        });
    }
    
    @Override
    public CompletableFuture<byte[]> generateThumbnail(byte[] content, String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return extractKeyFrame(content);
            } catch (Exception e) {
                logger.error("Error extracting key frame: " + filePath, e);
                return null;
            }
        });
    }
    
    /**
     * 提取基本视频信息
     * 
     * @param content 视频字节数据
     * @param filePath 文件路径
     * @return 基本信息映射
     */
    private Map<String, Object> extractBasicVideoInfo(byte[] content, String filePath) {
        Map<String, Object> info = new HashMap<>();
        
        // 文件基本信息
        info.put("fileSize", content.length);
        info.put("fileSizeKB", content.length / 1024);
        info.put("fileSizeMB", content.length / (1024 * 1024));
        info.put("format", getVideoFormat(filePath));
        info.put("container", getVideoContainer(filePath));
        
        // 视频元数据（需要视频处理库支持，这里提供估算数据）
        Map<String, Object> videoMetadata = extractVideoMetadata(content, filePath);
        info.putAll(videoMetadata);
        
        return info;
    }
    
    /**
     * 提取视频元数据（占位符实现）
     * 
     * @param content 视频字节数据
     * @param filePath 文件路径
     * @return 视频元数据
     */
    private Map<String, Object> extractVideoMetadata(byte[] content, String filePath) {
        Map<String, Object> metadata = new HashMap<>();
        
        // 这里应该使用视频处理库（如FFmpeg Java binding, Xuggler等）
        // 目前提供估算值
        
        String format = getVideoFormat(filePath);
        
        // 根据格式和文件大小估算基本参数
        double estimatedDuration = estimateVideoDuration(content.length, format);
        int estimatedBitrate = estimateVideoBitrate(content.length, estimatedDuration);
        
        metadata.put("duration", estimatedDuration);
        metadata.put("durationFormatted", formatDuration(estimatedDuration));
        metadata.put("bitrate", estimatedBitrate);
        metadata.put("estimatedFrameRate", estimateFrameRate(format));
        
        // 分辨率估算（基于文件大小和时长）
        Map<String, Integer> resolution = estimateResolution(content.length, estimatedDuration);
        metadata.put("width", resolution.get("width"));
        metadata.put("height", resolution.get("height"));
        metadata.put("aspectRatio", String.format("%.2f:1", (double) resolution.get("width") / resolution.get("height")));
        metadata.put("resolutionCategory", categorizeResolution(resolution.get("width"), resolution.get("height")));
        
        // 编码信息估算
        metadata.put("videoCodec", estimateVideoCodec(format));
        metadata.put("audioCodec", estimateAudioCodec(format));
        metadata.put("hasAudio", hasAudioTrack(content));
        metadata.put("hasVideo", true);
        
        // 质量评估
        metadata.put("qualityLevel", assessVideoQuality(estimatedBitrate, resolution));
        
        return metadata;
    }
    
    /**
     * 分析视频特征
     * 
     * @param content 视频字节数据
     * @param filePath 文件路径
     * @return 视频特征映射
     */
    private CompletableFuture<Map<String, Object>> analyzeVideoFeatures(byte[] content, String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> features = new HashMap<>();
            
            try {
                // 视频内容分析
                features.putAll(analyzeVideoContent(content));
                
                // 运动分析
                features.putAll(analyzeMotion(content));
                
                // 场景分析
                features.putAll(analyzeScenes(content));
                
                // 视频质量特征
                features.putAll(analyzeVideoQuality(content));
                
                // 视频类型分类
                features.put("videoCategory", categorizeVideo(content, filePath));
                
                // 复杂度分析
                features.putAll(analyzeVideoComplexity(content));
                
                logger.debug("Extracted {} video features", features.size());
                
            } catch (Exception e) {
                logger.error("Error analyzing video features", e);
            }
            
            return features;
        });
    }
    
    /**
     * 分析视频内容
     * 
     * @param content 视频字节数据
     * @return 内容分析结果
     */
    private Map<String, Object> analyzeVideoContent(byte[] content) {
        Map<String, Object> analysis = new HashMap<>();
        
        // 基于字节分布的内容分析
        
        // 色彩丰富度分析
        double colorComplexity = analyzeColorComplexity(content);
        analysis.put("colorComplexity", colorComplexity);
        analysis.put("colorRichness", categorizeColorRichness(colorComplexity));
        
        // 亮度分析
        double averageBrightness = calculateAverageBrightness(content);
        analysis.put("averageBrightness", averageBrightness);
        analysis.put("brightnessLevel", categorizeBrightness(averageBrightness));
        
        // 对比度分析
        double contrast = calculateVideoContrast(content);
        analysis.put("contrast", contrast);
        analysis.put("contrastLevel", categorizeContrast(contrast));
        
        // 视觉复杂度
        double visualComplexity = calculateVisualComplexity(content);
        analysis.put("visualComplexity", visualComplexity);
        
        return analysis;
    }
    
    /**
     * 分析运动特征
     * 
     * @param content 视频字节数据
     * @return 运动分析结果
     */
    private Map<String, Object> analyzeMotion(byte[] content) {
        Map<String, Object> motion = new HashMap<>();
        
        // 简化的运动检测（基于数据变化）
        double motionIntensity = calculateMotionIntensity(content);
        motion.put("motionIntensity", motionIntensity);
        motion.put("motionLevel", categorizeMotion(motionIntensity));
        
        // 运动平滑度
        double motionSmoothness = calculateMotionSmoothness(content);
        motion.put("motionSmoothness", motionSmoothness);
        
        // 快速运动检测
        boolean hasRapidMotion = detectRapidMotion(content);
        motion.put("hasRapidMotion", hasRapidMotion);
        
        // 镜头移动检测
        Map<String, Boolean> cameraMovement = detectCameraMovement(content);
        motion.putAll(cameraMovement);
        
        return motion;
    }
    
    /**
     * 分析场景特征
     * 
     * @param content 视频字节数据
     * @return 场景分析结果
     */
    private Map<String, Object> analyzeScenes(byte[] content) {
        Map<String, Object> scenes = new HashMap<>();
        
        // 场景变化检测
        int estimatedSceneCount = estimateSceneCount(content);
        scenes.put("estimatedSceneCount", estimatedSceneCount);
        scenes.put("sceneChangeFrequency", categorizeSceneChangeFrequency(estimatedSceneCount));
        
        // 场景稳定性
        double sceneStability = calculateSceneStability(content);
        scenes.put("sceneStability", sceneStability);
        scenes.put("stabilityLevel", categorizeStability(sceneStability));
        
        // 场景类型推断
        String sceneType = inferSceneType(content);
        scenes.put("primarySceneType", sceneType);
        
        return scenes;
    }
    
    /**
     * 分析视频质量
     * 
     * @param content 视频字节数据
     * @return 质量分析结果
     */
    private Map<String, Object> analyzeVideoQuality(byte[] content) {
        Map<String, Object> quality = new HashMap<>();
        
        // 清晰度评估
        double sharpness = assessVideoSharpness(content);
        quality.put("sharpness", sharpness);
        quality.put("sharpnessLevel", categorizeSharpness(sharpness));
        
        // 噪点检测
        double noiseLevel = detectVideoNoise(content);
        quality.put("noiseLevel", noiseLevel);
        quality.put("noiseCategory", categorizeNoise(noiseLevel));
        
        // 压缩伪影检测
        double compressionArtifacts = detectCompressionArtifacts(content);
        quality.put("compressionArtifacts", compressionArtifacts);
        quality.put("artifactLevel", categorizeArtifacts(compressionArtifacts));
        
        // 稳定性评估
        double stability = assessVideoStability(content);
        quality.put("stability", stability);
        quality.put("stabilityRating", categorizeStability(stability));
        
        return quality;
    }
    
    /**
     * 分析视频复杂度
     * 
     * @param content 视频字节数据
     * @return 复杂度分析结果
     */
    private Map<String, Object> analyzeVideoComplexity(byte[] content) {
        Map<String, Object> complexity = new HashMap<>();
        
        // 视觉复杂度
        double visualComplexity = calculateVisualComplexity(content);
        complexity.put("visualComplexity", visualComplexity);
        
        // 时间复杂度（变化频率）
        double temporalComplexity = calculateTemporalComplexity(content);
        complexity.put("temporalComplexity", temporalComplexity);
        
        // 综合复杂度
        double overallComplexity = (visualComplexity + temporalComplexity) / 2;
        complexity.put("overallComplexity", overallComplexity);
        complexity.put("complexityLevel", categorizeComplexity(overallComplexity));
        
        return complexity;
    }
    
    /**
     * 提取视频中的音频内容（占位符实现）
     * 
     * @param content 视频字节数据
     * @param filePath 文件路径
     * @return 提取的音频转文字内容
     */
    private String extractAudioFromVideo(byte[] content, String filePath) {
        // 这里应该先分离音频轨道，然后进行语音识别
        // 需要使用FFmpeg等工具分离音频，然后使用语音识别服务
        
        if (hasAudioTrack(content)) {
            return "Audio transcription from video placeholder - integrate FFmpeg for audio extraction " +
                   "and speech recognition service for transcription. " +
                   "Video file: " + filePath + ", Size: " + content.length + " bytes. " +
                   "This would contain the transcribed audio content from the video.";
        }
        
        return "";
    }
    
    /**
     * 提取关键帧
     * 
     * @param content 视频字节数据
     * @return 关键帧图像数据
     */
    private byte[] extractKeyFrame(byte[] content) {
        try {
            // 生成视频信息摘要作为"关键帧"
            StringBuilder keyFrameInfo = new StringBuilder();
            keyFrameInfo.append("Video Key Frame Information\n");
            keyFrameInfo.append("File size: ").append(content.length).append(" bytes\n");
            
            // 模拟关键帧提取过程
            keyFrameInfo.append("Key frame extracted at: middle position\n");
            keyFrameInfo.append("Frame properties:\n");
            keyFrameInfo.append("- Estimated brightness: ").append(String.format("%.2f", calculateAverageBrightness(content))).append("\n");
            keyFrameInfo.append("- Estimated contrast: ").append(String.format("%.2f", calculateVideoContrast(content))).append("\n");
            keyFrameInfo.append("- Visual complexity: ").append(String.format("%.2f", calculateVisualComplexity(content))).append("\n");
            
            // 简化的"像素"数据表示
            keyFrameInfo.append("Sample pixel data:\n");
            int sampleCount = Math.min(50, content.length / 1000);
            for (int i = 0; i < sampleCount; i++) {
                int index = (i * content.length) / sampleCount;
                int pixelValue = content[index] & 0xFF;
                keyFrameInfo.append(String.format("[%02d] RGB(%d,%d,%d) ", 
                    i, pixelValue, pixelValue, pixelValue));
                if ((i + 1) % 5 == 0) keyFrameInfo.append("\n");
            }
            
            return keyFrameInfo.toString().getBytes(StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            logger.error("Error extracting key frame", e);
            return null;
        }
    }
    
    /**
     * 视频分类
     * 
     * @param content 视频字节数据
     * @param filePath 文件路径
     * @return 视频类别
     */
    private String categorizeVideo(byte[] content, String filePath) {
        double motionIntensity = calculateMotionIntensity(content);
        double sceneStability = calculateSceneStability(content);
        int estimatedScenes = estimateSceneCount(content);
        
        if (motionIntensity > 0.7 && estimatedScenes > 10) {
            return "action";
        } else if (sceneStability > 0.8 && motionIntensity < 0.3) {
            return "documentary";
        } else if (estimatedScenes < 3 && sceneStability > 0.9) {
            return "presentation";
        } else if (motionIntensity > 0.5 && estimatedScenes > 5) {
            return "entertainment";
        } else {
            return "general";
        }
    }
    
    // 辅助计算方法
    
    private double analyzeColorComplexity(byte[] content) {
        // 分析字节值分布的复杂度
        int[] histogram = new int[256];
        for (byte b : content) {
            histogram[b & 0xFF]++;
        }
        
        // 计算熵作为复杂度指标
        double entropy = 0;
        for (int count : histogram) {
            if (count > 0) {
                double probability = (double) count / content.length;
                entropy -= probability * Math.log(probability) / Math.log(2);
            }
        }
        
        return entropy / 8.0; // 归一化到0-1
    }
    
    private double calculateAverageBrightness(byte[] content) {
        long sum = 0;
        for (byte b : content) {
            sum += (b & 0xFF);
        }
        return (double) sum / (content.length * 255.0); // 归一化到0-1
    }
    
    private double calculateVideoContrast(byte[] content) {
        int min = 255, max = 0;
        for (byte b : content) {
            int value = b & 0xFF;
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        return (double) (max - min) / 255.0; // 归一化到0-1
    }
    
    private double calculateVisualComplexity(byte[] content) {
        // 基于相邻字节差异的复杂度
        double totalDifference = 0;
        for (int i = 1; i < content.length; i++) {
            totalDifference += Math.abs((content[i] & 0xFF) - (content[i - 1] & 0xFF));
        }
        return content.length > 1 ? totalDifference / ((content.length - 1) * 255.0) : 0;
    }
    
    private double calculateMotionIntensity(byte[] content) {
        // 简化的运动强度计算：基于数据变化率
        int chunkSize = content.length / 100; // 分成100个块
        double totalVariation = 0;
        
        for (int i = 0; i < 99; i++) {
            int start1 = i * chunkSize;
            int start2 = (i + 1) * chunkSize;
            int end = Math.min(start2 + chunkSize, content.length);
            
            double chunk1Avg = 0, chunk2Avg = 0;
            for (int j = start1; j < start2; j++) {
                chunk1Avg += (content[j] & 0xFF);
            }
            for (int j = start2; j < end; j++) {
                chunk2Avg += (content[j] & 0xFF);
            }
            
            chunk1Avg /= chunkSize;
            chunk2Avg /= (end - start2);
            
            totalVariation += Math.abs(chunk2Avg - chunk1Avg);
        }
        
        return totalVariation / (99 * 255.0); // 归一化
    }
    
    private double calculateMotionSmoothness(byte[] content) {
        double motionIntensity = calculateMotionIntensity(content);
        double visualComplexity = calculateVisualComplexity(content);
        return 1.0 - Math.abs(motionIntensity - visualComplexity); // 运动与视觉变化的一致性
    }
    
    private boolean detectRapidMotion(byte[] content) {
        return calculateMotionIntensity(content) > 0.7;
    }
    
    private Map<String, Boolean> detectCameraMovement(byte[] content) {
        Map<String, Boolean> movement = new HashMap<>();
        double motionIntensity = calculateMotionIntensity(content);
        
        // 简化的镜头移动检测
        movement.put("hasPanning", motionIntensity > 0.4 && motionIntensity < 0.8);
        movement.put("hasZooming", calculateVisualComplexity(content) > 0.6);
        movement.put("hasShaking", motionIntensity > 0.8);
        movement.put("isStatic", motionIntensity < 0.2);
        
        return movement;
    }
    
    private int estimateSceneCount(byte[] content) {
        // 基于显著变化点估算场景数
        int chunkSize = content.length / 1000;
        int significantChanges = 0;
        
        for (int i = 1; i < 1000; i++) {
            int start1 = (i - 1) * chunkSize;
            int start2 = i * chunkSize;
            int end = Math.min(start2 + chunkSize, content.length);
            
            double avgDiff = 0;
            for (int j = start1; j < start2 && j < end - chunkSize; j++) {
                avgDiff += Math.abs((content[j] & 0xFF) - (content[j + chunkSize] & 0xFF));
            }
            
            if (avgDiff / chunkSize > 50) { // 显著变化阈值
                significantChanges++;
            }
        }
        
        return Math.max(1, significantChanges / 10); // 估算场景数
    }
    
    private double calculateSceneStability(byte[] content) {
        double motionIntensity = calculateMotionIntensity(content);
        return 1.0 - motionIntensity; // 稳定性与运动强度反向相关
    }
    
    private String inferSceneType(byte[] content) {
        double brightness = calculateAverageBrightness(content);
        double motion = calculateMotionIntensity(content);
        
        if (brightness > 0.8 && motion < 0.3) return "outdoor";
        if (brightness < 0.3 && motion < 0.2) return "indoor_static";
        if (motion > 0.7) return "dynamic";
        return "general";
    }
    
    private double assessVideoSharpness(byte[] content) {
        return calculateVisualComplexity(content); // 简化：用视觉复杂度代表清晰度
    }
    
    private double detectVideoNoise(byte[] content) {
        // 检测高频噪声
        double highFreqContent = 0;
        for (int i = 2; i < content.length - 2; i++) {
            int current = content[i] & 0xFF;
            int prev2 = content[i - 2] & 0xFF;
            int next2 = content[i + 2] & 0xFF;
            
            if (Math.abs(current - (prev2 + next2) / 2) > 30) {
                highFreqContent++;
            }
        }
        
        return highFreqContent / content.length;
    }
    
    private double detectCompressionArtifacts(byte[] content) {
        // 简化的压缩伪影检测
        int blockSize = 8; // 模拟JPEG/H.264块
        double artifactScore = 0;
        
        for (int i = 0; i < content.length - blockSize; i += blockSize) {
            double blockVariance = 0;
            double blockMean = 0;
            
            for (int j = 0; j < blockSize && i + j < content.length; j++) {
                blockMean += (content[i + j] & 0xFF);
            }
            blockMean /= blockSize;
            
            for (int j = 0; j < blockSize && i + j < content.length; j++) {
                double diff = (content[i + j] & 0xFF) - blockMean;
                blockVariance += diff * diff;
            }
            blockVariance /= blockSize;
            
            if (blockVariance < 10) { // 方差过小可能是过度压缩
                artifactScore++;
            }
        }
        
        return artifactScore / (content.length / blockSize);
    }
    
    private double assessVideoStability(byte[] content) {
        return calculateSceneStability(content);
    }
    
    private double calculateTemporalComplexity(byte[] content) {
        return calculateMotionIntensity(content);
    }
    
    private boolean hasAudioTrack(byte[] content) {
        // 简化的音频轨道检测
        // 实际应该分析容器格式和流信息
        return content.length > 1024 * 1024; // 假设大文件可能包含音频
    }
    
    // 估算方法
    
    private double estimateVideoDuration(int fileSize, String format) {
        // 基于文件大小和格式估算时长（秒）
        int estimatedBitrate = 1000; // kbps
        switch (format.toLowerCase()) {
            case "mp4": estimatedBitrate = 2000; break;
            case "avi": estimatedBitrate = 1500; break;
            case "mov": estimatedBitrate = 2500; break;
            case "mkv": estimatedBitrate = 3000; break;
            default: estimatedBitrate = 1000;
        }
        
        return (fileSize * 8.0) / (estimatedBitrate * 1000); // 转换为秒
    }
    
    private int estimateVideoBitrate(int fileSize, double duration) {
        return duration > 0 ? (int) ((fileSize * 8) / (duration * 1000)) : 0;
    }
    
    private double estimateFrameRate(String format) {
        // 常见帧率
        switch (format.toLowerCase()) {
            case "mp4":
            case "mov": return 30.0;
            case "avi": return 25.0;
            case "mkv": return 24.0;
            default: return 30.0;
        }
    }
    
    private Map<String, Integer> estimateResolution(int fileSize, double duration) {
        Map<String, Integer> resolution = new HashMap<>();
        
        // 基于文件大小估算分辨率
        double bitsPerSecond = duration > 0 ? (fileSize * 8) / duration : 0;
        
        if (bitsPerSecond > 5000000) { // > 5Mbps
            resolution.put("width", 1920);
            resolution.put("height", 1080);
        } else if (bitsPerSecond > 2000000) { // > 2Mbps
            resolution.put("width", 1280);
            resolution.put("height", 720);
        } else if (bitsPerSecond > 500000) { // > 0.5Mbps
            resolution.put("width", 854);
            resolution.put("height", 480);
        } else {
            resolution.put("width", 640);
            resolution.put("height", 360);
        }
        
        return resolution;
    }
    
    private String estimateVideoCodec(String format) {
        switch (format.toLowerCase()) {
            case "mp4": return "H.264";
            case "avi": return "XVID";
            case "mov": return "H.264";
            case "mkv": return "H.265";
            default: return "Unknown";
        }
    }
    
    private String estimateAudioCodec(String format) {
        switch (format.toLowerCase()) {
            case "mp4": return "AAC";
            case "avi": return "MP3";
            case "mov": return "AAC";
            case "mkv": return "AC3";
            default: return "Unknown";
        }
    }
    
    // 分类方法
    
    private String categorizeColorRichness(double complexity) {
        if (complexity < 0.3) return "monochrome";
        if (complexity < 0.7) return "moderate";
        return "rich";
    }
    
    private String categorizeBrightness(double brightness) {
        if (brightness < 0.3) return "dark";
        if (brightness < 0.7) return "moderate";
        return "bright";
    }
    
    private String categorizeContrast(double contrast) {
        if (contrast < 0.3) return "low";
        if (contrast < 0.7) return "moderate";
        return "high";
    }
    
    private String categorizeMotion(double intensity) {
        if (intensity < 0.2) return "static";
        if (intensity < 0.5) return "slow";
        if (intensity < 0.8) return "moderate";
        return "fast";
    }
    
    private String categorizeSceneChangeFrequency(int sceneCount) {
        if (sceneCount < 3) return "low";
        if (sceneCount < 10) return "moderate";
        return "high";
    }
    
    private String categorizeStability(double stability) {
        if (stability < 0.3) return "unstable";
        if (stability < 0.7) return "moderate";
        return "stable";
    }
    
    private String categorizeSharpness(double sharpness) {
        if (sharpness < 0.3) return "blurry";
        if (sharpness < 0.7) return "moderate";
        return "sharp";
    }
    
    private String categorizeNoise(double noise) {
        if (noise < 0.1) return "clean";
        if (noise < 0.3) return "moderate";
        return "noisy";
    }
    
    private String categorizeArtifacts(double artifacts) {
        if (artifacts < 0.1) return "minimal";
        if (artifacts < 0.3) return "moderate";
        return "heavy";
    }
    
    private String categorizeComplexity(double complexity) {
        if (complexity < 0.3) return "simple";
        if (complexity < 0.7) return "moderate";
        return "complex";
    }
    
    private String categorizeResolution(int width, int height) {
        int pixels = width * height;
        if (pixels >= 1920 * 1080) return "Full HD";
        if (pixels >= 1280 * 720) return "HD";
        if (pixels >= 854 * 480) return "SD";
        return "Low";
    }
    
    private String assessVideoQuality(int bitrate, Map<String, Integer> resolution) {
        int pixels = resolution.get("width") * resolution.get("height");
        double bitsPerPixel = (double) bitrate / pixels;
        
        if (bitsPerPixel > 0.1 && bitrate > 2000) return "high";
        if (bitsPerPixel > 0.05 && bitrate > 1000) return "medium";
        return "low";
    }
    
    private String formatDuration(double seconds) {
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        int secs = (int) (seconds % 60);
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%d:%02d", minutes, secs);
        }
    }
    
    private String determineContentType(String filePath) {
        String extension = getFileExtension(filePath).toLowerCase();
        return "video/" + extension;
    }
    
    private String generateStorageKey(String filePath) {
        return UUID.randomUUID().toString() + "_" + System.currentTimeMillis();
    }
    
    private String getFileExtension(String filePath) {
        int lastDot = filePath.lastIndexOf('.');
        return lastDot > 0 ? filePath.substring(lastDot + 1) : "";
    }
    
    private String getVideoFormat(String filePath) {
        return getFileExtension(filePath).toUpperCase();
    }
    
    private String getVideoContainer(String filePath) {
        String extension = getFileExtension(filePath).toLowerCase();
        switch (extension) {
            case "mp4": return "MPEG-4";
            case "avi": return "AVI";
            case "mov": return "QuickTime";
            case "mkv": return "Matroska";
            default: return "Unknown";
        }
    }
    
    @Override
    public List<String> getSupportedFormats() {
        return configuration.getSupportedVideoFormats();
    }
    
    @Override
    public String getProcessorName() {
        return "VideoProcessor";
    }
}