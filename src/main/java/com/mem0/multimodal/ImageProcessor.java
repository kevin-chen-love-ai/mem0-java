package com.mem0.multimodal;

import com.mem0.multimodal.MultimodalMemoryProcessor.ModalityProcessor;
import com.mem0.multimodal.MultimodalMemoryProcessor.ModalityProcessResult;
import com.mem0.multimodal.MultimodalMemoryProcessor.MultimodalConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * ImageProcessor - 图像处理器
 * 
 * 专门处理图像类型的多模态内容，提供图像分析、特征提取、OCR文字识别等功能。
 * 支持多种图像格式的处理和缩略图生成。
 * 
 * 主要功能：
 * 1. 图像基本信息提取 - 尺寸、格式、颜色空间等
 * 2. 图像特征分析 - 颜色分布、纹理特征、边缘检测等
 * 3. OCR文字识别 - 提取图像中的文字内容
 * 4. 缩略图生成 - 生成不同尺寸的预览图
 * 5. 图像质量评估 - 亮度、对比度、清晰度等指标
 * 
 * @author kevin.chen
 * @version 1.0
 * @since 2024-12-20
 */
public class ImageProcessor implements ModalityProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageProcessor.class);
    
    private final MultimodalConfiguration configuration;
    
    // 缩略图配置
    private static final int THUMBNAIL_WIDTH = 200;
    private static final int THUMBNAIL_HEIGHT = 200;
    private static final String THUMBNAIL_FORMAT = "jpg";
    
    /**
     * 构造函数
     * 
     * @param configuration 多模态配置
     */
    public ImageProcessor(MultimodalConfiguration configuration) {
        this.configuration = configuration;
        logger.info("ImageProcessor initialized");
    }
    
    @Override
    public CompletableFuture<ModalityProcessResult> processContent(byte[] content, String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                // Check for null or empty content
                if (content == null || content.length == 0) {
                    logger.warn("Empty or null image content for file: {}", filePath);
                    long processingTime = System.currentTimeMillis() - startTime;
                    return new ModalityProcessResult("image", "", new HashMap<>(), new HashMap<>(), null, null, processingTime);
                }
                
                // 解析图像
                BufferedImage image = null;
                try {
                    image = ImageIO.read(new ByteArrayInputStream(content));
                } catch (Exception e) {
                    logger.warn("Failed to read image data for file: {}, error: {}", filePath, e.getMessage());
                }
                
                if (image == null) {
                    logger.warn("Unable to read image: {}, treating as invalid image", filePath);
                    long processingTime = System.currentTimeMillis() - startTime;
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("error", "Invalid image format");
                    metadata.put("processingTime", processingTime);
                    return new ModalityProcessResult("image", "", metadata, new HashMap<>(), null, null, processingTime);
                }
                
                // 提取基本信息
                Map<String, Object> metadata = extractBasicImageInfo(image, filePath, content.length);
                
                // 提取特征
                Map<String, Object> features = extractImageFeatures(image).join();
                
                // OCR文字识别
                String extractedText = "";
                if (configuration.isEnableOCR()) {
                    extractedText = performOCR(image);
                }
                
                // 生成缩略图
                byte[] thumbnailData = null;
                if (configuration.isEnableThumbnailGeneration()) {
                    thumbnailData = generateThumbnail(content, filePath).join();
                }
                
                long processingTime = System.currentTimeMillis() - startTime;
                
                // 添加处理统计到元数据
                metadata.put("processingTime", processingTime);
                metadata.put("ocrEnabled", configuration.isEnableOCR());
                metadata.put("thumbnailGenerated", thumbnailData != null);
                
                logger.debug("Processed image: {} in {}ms", filePath, processingTime);
                
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
                logger.warn("Error processing image: {}, error: {}", filePath, e.getMessage());
                long processingTime = System.currentTimeMillis() - startTime;
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("error", e.getMessage());
                metadata.put("processingTime", processingTime);
                return new ModalityProcessResult("image", "", metadata, new HashMap<>(), null, null, processingTime);
            }
        });
    }
    
    @Override
    public CompletableFuture<Map<String, Object>> extractFeatures(byte[] content, String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
                if (image == null) {
                    return new HashMap<>();
                }
                
                return extractImageFeatures(image).join();
                
            } catch (Exception e) {
                logger.error("Error extracting image features: " + filePath, e);
                return new HashMap<>();
            }
        });
    }
    
    @Override
    public CompletableFuture<byte[]> generateThumbnail(byte[] content, String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(content));
                if (originalImage == null) {
                    return null;
                }
                
                // 计算缩略图尺寸（保持宽高比）
                int originalWidth = originalImage.getWidth();
                int originalHeight = originalImage.getHeight();
                
                double scale = Math.min(
                    (double) THUMBNAIL_WIDTH / originalWidth,
                    (double) THUMBNAIL_HEIGHT / originalHeight
                );
                
                int thumbnailWidth = (int) (originalWidth * scale);
                int thumbnailHeight = (int) (originalHeight * scale);
                
                // 创建缩略图
                BufferedImage thumbnail = new BufferedImage(
                    thumbnailWidth, thumbnailHeight, BufferedImage.TYPE_INT_RGB);
                
                Graphics2D g2d = thumbnail.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                                   RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, 
                                   RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                                   RenderingHints.VALUE_ANTIALIAS_ON);
                
                g2d.drawImage(originalImage, 0, 0, thumbnailWidth, thumbnailHeight, null);
                g2d.dispose();
                
                // 转换为字节数组
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(thumbnail, THUMBNAIL_FORMAT, baos);
                
                logger.debug("Generated thumbnail for image: {} ({}x{} -> {}x{})",
                           filePath, originalWidth, originalHeight, thumbnailWidth, thumbnailHeight);
                
                return baos.toByteArray();
                
            } catch (Exception e) {
                logger.error("Error generating thumbnail for image: " + filePath, e);
                return null;
            }
        });
    }
    
    /**
     * 提取图像基本信息
     * 
     * @param image 图像对象
     * @param filePath 文件路径
     * @param fileSize 文件大小
     * @return 基本信息映射
     */
    private Map<String, Object> extractBasicImageInfo(BufferedImage image, String filePath, long fileSize) {
        Map<String, Object> info = new HashMap<>();
        
        // 基本尺寸信息
        info.put("width", image.getWidth());
        info.put("height", image.getHeight());
        info.put("aspectRatio", (double) image.getWidth() / image.getHeight());
        info.put("pixelCount", image.getWidth() * image.getHeight());
        
        // 文件信息
        info.put("fileSize", fileSize);
        info.put("fileSizeKB", fileSize / 1024);
        info.put("format", getImageFormat(filePath));
        
        // 颜色信息
        info.put("colorModel", image.getColorModel().getClass().getSimpleName());
        info.put("hasAlpha", image.getColorModel().hasAlpha());
        info.put("colorSpace", image.getColorModel().getColorSpace().getType());
        info.put("bitsPerPixel", image.getColorModel().getPixelSize());
        
        // 图像类型
        info.put("imageType", getImageTypeString(image.getType()));
        
        return info;
    }
    
    /**
     * 提取图像特征
     * 
     * @param image 图像对象
     * @return 特征映射
     */
    private CompletableFuture<Map<String, Object>> extractImageFeatures(BufferedImage image) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> features = new HashMap<>();
            
            try {
                // 颜色特征
                features.putAll(extractColorFeatures(image));
                
                // 纹理特征
                features.putAll(extractTextureFeatures(image));
                
                // 质量评估
                features.putAll(assessImageQuality(image));
                
                // 形状特征
                features.putAll(extractShapeFeatures(image));
                
                logger.debug("Extracted {} image features", features.size());
                
            } catch (Exception e) {
                logger.error("Error extracting image features", e);
            }
            
            return features;
        });
    }
    
    /**
     * 提取颜色特征
     * 
     * @param image 图像对象
     * @return 颜色特征映射
     */
    private Map<String, Object> extractColorFeatures(BufferedImage image) {
        Map<String, Object> features = new HashMap<>();
        
        // RGB直方图
        int[] redHist = new int[256];
        int[] greenHist = new int[256];
        int[] blueHist = new int[256];
        
        int width = image.getWidth();
        int height = image.getHeight();
        long totalBrightness = 0;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                
                redHist[red]++;
                greenHist[green]++;
                blueHist[blue]++;
                
                totalBrightness += (red + green + blue) / 3;
            }
        }
        
        int totalPixels = width * height;
        
        // 平均亮度
        features.put("averageBrightness", (double) totalBrightness / totalPixels);
        
        // 主导颜色
        features.put("dominantRed", findDominantValue(redHist));
        features.put("dominantGreen", findDominantValue(greenHist));
        features.put("dominantBlue", findDominantValue(blueHist));
        
        // 颜色分布统计
        features.put("colorVarianceRed", calculateVariance(redHist, totalPixels));
        features.put("colorVarianceGreen", calculateVariance(greenHist, totalPixels));
        features.put("colorVarianceBlue", calculateVariance(blueHist, totalPixels));
        
        return features;
    }
    
    /**
     * 提取纹理特征
     * 
     * @param image 图像对象
     * @return 纹理特征映射
     */
    private Map<String, Object> extractTextureFeatures(BufferedImage image) {
        Map<String, Object> features = new HashMap<>();
        
        // 转换为灰度图像
        BufferedImage grayImage = convertToGrayscale(image);
        
        int width = grayImage.getWidth();
        int height = grayImage.getHeight();
        
        // 边缘密度（简化的Sobel算子）
        int edgeCount = 0;
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int gx = getGrayValue(grayImage, x + 1, y) - getGrayValue(grayImage, x - 1, y);
                int gy = getGrayValue(grayImage, x, y + 1) - getGrayValue(grayImage, x, y - 1);
                int gradient = Math.abs(gx) + Math.abs(gy);
                
                if (gradient > 30) { // 阈值
                    edgeCount++;
                }
            }
        }
        
        features.put("edgeDensity", (double) edgeCount / (width * height));
        
        // 纹理粗糙度（局部方差）
        double totalVariance = 0;
        int windowSize = 3;
        int validWindows = 0;
        
        for (int y = windowSize; y < height - windowSize; y += windowSize) {
            for (int x = windowSize; x < width - windowSize; x += windowSize) {
                double variance = calculateLocalVariance(grayImage, x, y, windowSize);
                totalVariance += variance;
                validWindows++;
            }
        }
        
        features.put("textureRoughness", validWindows > 0 ? totalVariance / validWindows : 0.0);
        
        return features;
    }
    
    /**
     * 评估图像质量
     * 
     * @param image 图像对象
     * @return 质量评估映射
     */
    private Map<String, Object> assessImageQuality(BufferedImage image) {
        Map<String, Object> quality = new HashMap<>();
        
        // 清晰度评估（基于拉普拉斯算子）
        double sharpness = calculateSharpness(image);
        quality.put("sharpness", sharpness);
        quality.put("sharpnessLevel", categorizeSharpness(sharpness));
        
        // 对比度评估
        double contrast = calculateContrast(image);
        quality.put("contrast", contrast);
        quality.put("contrastLevel", categorizeContrast(contrast));
        
        // 噪声评估（简化）
        double noise = estimateNoise(image);
        quality.put("noiseLevel", noise);
        quality.put("noiseCategory", categorizeNoise(noise));
        
        return quality;
    }
    
    /**
     * 提取形状特征
     * 
     * @param image 图像对象
     * @return 形状特征映射
     */
    private Map<String, Object> extractShapeFeatures(BufferedImage image) {
        Map<String, Object> features = new HashMap<>();
        
        // 基本几何特征
        features.put("width", image.getWidth());
        features.put("height", image.getHeight());
        features.put("aspectRatio", (double) image.getWidth() / image.getHeight());
        
        // 形状分类
        String shapeCategory = categorizeImageShape(image.getWidth(), image.getHeight());
        features.put("shapeCategory", shapeCategory);
        
        // 分辨率类别
        String resolutionCategory = categorizeResolution(image.getWidth(), image.getHeight());
        features.put("resolutionCategory", resolutionCategory);
        
        return features;
    }
    
    /**
     * 执行OCR文字识别
     * 
     * @param image 图像对象
     * @return 识别出的文字
     */
    private String performOCR(BufferedImage image) {
        try {
            // Check if Tesseract is available
            Class.forName("net.sourceforge.tess4j.Tesseract");
            
            // 使用Tesseract进行OCR
            net.sourceforge.tess4j.Tesseract tesseract = new net.sourceforge.tess4j.Tesseract();
            
            // 配置Tesseract
            tesseract.setLanguage("chi_sim+eng"); // 支持中文和英文
            tesseract.setPageSegMode(1); // 自动页面分割
            tesseract.setOcrEngineMode(1); // 使用神经网络LSTM引擎
            
            // 预处理图像以提高OCR准确性
            BufferedImage preprocessedImage = preprocessImageForOCR(image);
            
            // 执行OCR
            String result = tesseract.doOCR(preprocessedImage);
            
            if (result != null && !result.trim().isEmpty()) {
                // 清理OCR结果
                String cleanedText = cleanOCRResult(result);
                logger.debug("OCR extracted {} characters from image", cleanedText.length());
                return cleanedText;
            }
            
            return "";
            
        } catch (ClassNotFoundException e) {
            logger.warn("Tesseract OCR library not available, skipping OCR processing");
            return "";
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            logger.warn("Tesseract native library not found, skipping OCR processing: {}", e.getMessage());
            return "";
        } catch (Exception e) {
            logger.warn("OCR processing failed, falling back to basic text detection: {}", e.getMessage());
            
            // Fallback: 简单的文字区域检测
            if (hasTextLikeRegions(image)) {
                return "Text regions detected but OCR extraction failed. Please check Tesseract installation.";
            }
            
            return "";
        }
    }
    
    /**
     * 预处理图像以提高OCR准确性
     */
    private BufferedImage preprocessImageForOCR(BufferedImage image) {
        // 转换为灰度图
        BufferedImage grayImage = convertToGrayscale(image);
        
        // 调整图像大小以提高OCR效果
        int targetWidth = Math.max(image.getWidth(), 300);
        int targetHeight = Math.max(image.getHeight(), 300);
        
        if (image.getWidth() < targetWidth || image.getHeight() < targetHeight) {
            double scaleX = (double) targetWidth / image.getWidth();
            double scaleY = (double) targetHeight / image.getHeight();
            double scale = Math.min(scaleX, scaleY);
            
            int newWidth = (int) (image.getWidth() * scale);
            int newHeight = (int) (image.getHeight() * scale);
            
            BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D g2d = scaledImage.createGraphics();
            
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            
            g2d.drawImage(grayImage, 0, 0, newWidth, newHeight, null);
            g2d.dispose();
            
            return scaledImage;
        }
        
        return grayImage;
    }
    
    /**
     * 清理OCR结果
     */
    private String cleanOCRResult(String ocrText) {
        if (ocrText == null) return "";
        
        return ocrText
            // 移除多余的空白字符
            .replaceAll("\\s+", " ")
            // 移除非打印字符
            .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "")
            // 修正常见的OCR错误
            .replace("0", "O") // 数字0误识别为字母O的情况
            .replace("1", "l") // 数字1误识别为字母l的情况  
            .trim();
    }
    
    /**
     * 检测是否有文字区域
     * 
     * @param image 图像对象
     * @return 是否有文字区域
     */
    private boolean hasTextLikeRegions(BufferedImage image) {
        // 简化的文字区域检测逻辑
        BufferedImage grayImage = convertToGrayscale(image);
        int width = grayImage.getWidth();
        int height = grayImage.getHeight();
        
        int horizontalEdges = 0;
        int verticalEdges = 0;
        
        // 检测水平和垂直边缘
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int current = getGrayValue(grayImage, x, y);
                int right = getGrayValue(grayImage, x + 1, y);
                int down = getGrayValue(grayImage, x, y + 1);
                
                if (Math.abs(current - right) > 50) horizontalEdges++;
                if (Math.abs(current - down) > 50) verticalEdges++;
            }
        }
        
        // 文字通常有较多的边缘特征
        double edgeDensity = (double) (horizontalEdges + verticalEdges) / (width * height);
        return edgeDensity > 0.1; // 经验阈值
    }
    
    // 辅助方法
    
    private String determineContentType(String filePath) {
        String extension = getFileExtension(filePath).toLowerCase();
        return "image/" + extension;
    }
    
    private String generateStorageKey(String filePath) {
        return UUID.randomUUID().toString() + "_" + System.currentTimeMillis();
    }
    
    private String getFileExtension(String filePath) {
        int lastDot = filePath.lastIndexOf('.');
        return lastDot > 0 ? filePath.substring(lastDot + 1) : "jpg";
    }
    
    private String getImageFormat(String filePath) {
        return getFileExtension(filePath).toUpperCase();
    }
    
    private String getImageTypeString(int type) {
        switch (type) {
            case BufferedImage.TYPE_INT_RGB: return "INT_RGB";
            case BufferedImage.TYPE_INT_ARGB: return "INT_ARGB";
            case BufferedImage.TYPE_INT_ARGB_PRE: return "INT_ARGB_PRE";
            case BufferedImage.TYPE_INT_BGR: return "INT_BGR";
            case BufferedImage.TYPE_3BYTE_BGR: return "3BYTE_BGR";
            case BufferedImage.TYPE_4BYTE_ABGR: return "4BYTE_ABGR";
            case BufferedImage.TYPE_4BYTE_ABGR_PRE: return "4BYTE_ABGR_PRE";
            case BufferedImage.TYPE_BYTE_GRAY: return "BYTE_GRAY";
            case BufferedImage.TYPE_USHORT_GRAY: return "USHORT_GRAY";
            case BufferedImage.TYPE_BYTE_BINARY: return "BYTE_BINARY";
            case BufferedImage.TYPE_BYTE_INDEXED: return "BYTE_INDEXED";
            case BufferedImage.TYPE_USHORT_565_RGB: return "USHORT_565_RGB";
            case BufferedImage.TYPE_USHORT_555_RGB: return "USHORT_555_RGB";
            default: return "CUSTOM";
        }
    }
    
    private BufferedImage convertToGrayscale(BufferedImage image) {
        BufferedImage grayImage = new BufferedImage(
            image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = grayImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return grayImage;
    }
    
    private int getGrayValue(BufferedImage grayImage, int x, int y) {
        return grayImage.getRGB(x, y) & 0xFF;
    }
    
    private int findDominantValue(int[] histogram) {
        int maxCount = 0;
        int dominantValue = 0;
        
        for (int i = 0; i < histogram.length; i++) {
            if (histogram[i] > maxCount) {
                maxCount = histogram[i];
                dominantValue = i;
            }
        }
        
        return dominantValue;
    }
    
    private double calculateVariance(int[] histogram, int totalPixels) {
        double mean = 0;
        for (int i = 0; i < histogram.length; i++) {
            mean += i * histogram[i];
        }
        mean /= totalPixels;
        
        double variance = 0;
        for (int i = 0; i < histogram.length; i++) {
            double diff = i - mean;
            variance += diff * diff * histogram[i];
        }
        
        return variance / totalPixels;
    }
    
    private double calculateLocalVariance(BufferedImage grayImage, int centerX, int centerY, int windowSize) {
        double sum = 0;
        double sumSquares = 0;
        int count = 0;
        
        for (int y = centerY - windowSize; y <= centerY + windowSize; y++) {
            for (int x = centerX - windowSize; x <= centerX + windowSize; x++) {
                if (x >= 0 && x < grayImage.getWidth() && y >= 0 && y < grayImage.getHeight()) {
                    int value = getGrayValue(grayImage, x, y);
                    sum += value;
                    sumSquares += value * value;
                    count++;
                }
            }
        }
        
        if (count == 0) return 0;
        
        double mean = sum / count;
        return (sumSquares / count) - (mean * mean);
    }
    
    private double calculateSharpness(BufferedImage image) {
        BufferedImage grayImage = convertToGrayscale(image);
        int width = grayImage.getWidth();
        int height = grayImage.getHeight();
        
        double totalVariance = 0;
        
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int center = getGrayValue(grayImage, x, y);
                int laplacian = -8 * center +
                               getGrayValue(grayImage, x-1, y-1) +
                               getGrayValue(grayImage, x, y-1) +
                               getGrayValue(grayImage, x+1, y-1) +
                               getGrayValue(grayImage, x-1, y) +
                               getGrayValue(grayImage, x+1, y) +
                               getGrayValue(grayImage, x-1, y+1) +
                               getGrayValue(grayImage, x, y+1) +
                               getGrayValue(grayImage, x+1, y+1);
                
                totalVariance += laplacian * laplacian;
            }
        }
        
        return totalVariance / ((width - 2) * (height - 2));
    }
    
    private double calculateContrast(BufferedImage image) {
        BufferedImage grayImage = convertToGrayscale(image);
        int width = grayImage.getWidth();
        int height = grayImage.getHeight();
        
        int minValue = 255;
        int maxValue = 0;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = getGrayValue(grayImage, x, y);
                minValue = Math.min(minValue, value);
                maxValue = Math.max(maxValue, value);
            }
        }
        
        return (double) (maxValue - minValue) / 255.0;
    }
    
    private double estimateNoise(BufferedImage image) {
        BufferedImage grayImage = convertToGrayscale(image);
        int width = grayImage.getWidth();
        int height = grayImage.getHeight();
        
        double totalVariation = 0;
        int count = 0;
        
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int center = getGrayValue(grayImage, x, y);
                int neighbors = getGrayValue(grayImage, x-1, y) +
                               getGrayValue(grayImage, x+1, y) +
                               getGrayValue(grayImage, x, y-1) +
                               getGrayValue(grayImage, x, y+1);
                
                double avgNeighbor = neighbors / 4.0;
                totalVariation += Math.abs(center - avgNeighbor);
                count++;
            }
        }
        
        return count > 0 ? totalVariation / count : 0.0;
    }
    
    private String categorizeSharpness(double sharpness) {
        if (sharpness < 100) return "blurry";
        if (sharpness < 1000) return "moderate";
        return "sharp";
    }
    
    private String categorizeContrast(double contrast) {
        if (contrast < 0.3) return "low";
        if (contrast < 0.7) return "moderate";
        return "high";
    }
    
    private String categorizeNoise(double noise) {
        if (noise < 5) return "low";
        if (noise < 15) return "moderate";
        return "high";
    }
    
    private String categorizeImageShape(int width, int height) {
        double ratio = (double) width / height;
        if (Math.abs(ratio - 1.0) < 0.1) return "square";
        if (ratio > 1.5) return "landscape";
        if (ratio < 0.67) return "portrait";
        return "rectangular";
    }
    
    private String categorizeResolution(int width, int height) {
        int pixels = width * height;
        if (pixels < 100000) return "low"; // < 0.1MP
        if (pixels < 1000000) return "medium"; // < 1MP
        if (pixels < 5000000) return "high"; // < 5MP
        return "very_high"; // >= 5MP
    }
    
    @Override
    public List<String> getSupportedFormats() {
        return configuration.getSupportedImageFormats();
    }
    
    @Override
    public String getProcessorName() {
        return "ImageProcessor";
    }
}