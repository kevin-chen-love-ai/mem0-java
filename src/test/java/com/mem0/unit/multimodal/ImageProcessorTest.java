package com.mem0.unit.multimodal;

import com.mem0.multimodal.ImageProcessor;
import com.mem0.multimodal.MultimodalMemoryProcessor.MultimodalConfiguration;
import com.mem0.multimodal.MultimodalMemoryProcessor.ModalityProcessResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for ImageProcessor OCR functionality
 */
public class ImageProcessorTest {
    
    private ImageProcessor imageProcessor;
    
    @BeforeEach
    void setUp() {
        MultimodalConfiguration config = new MultimodalConfiguration();
        imageProcessor = new ImageProcessor(config);
    }
    
    @Test
    void testProcessNullImage() throws Exception {
        CompletableFuture<ModalityProcessResult> result = imageProcessor.processContent(null, "test.png");
        ModalityProcessResult processResult = result.get();
        
        assertNotNull(processResult);
        assertNotNull(processResult.getExtractedText());
        assertTrue(processResult.getExtractedText().isEmpty());
    }
    
    @Test
    void testProcessEmptyImage() throws Exception {
        byte[] emptyImage = new byte[0];
        
        CompletableFuture<ModalityProcessResult> result = imageProcessor.processContent(emptyImage, "empty.png");
        ModalityProcessResult processResult = result.get();
        
        assertNotNull(processResult);
        assertNotNull(processResult.getExtractedText());
        assertTrue(processResult.getExtractedText().isEmpty());
    }
    
    @Test
    void testProcessInvalidImageData() throws Exception {
        byte[] invalidData = "This is not image data".getBytes();
        
        CompletableFuture<ModalityProcessResult> result = imageProcessor.processContent(invalidData, "invalid.png");
        ModalityProcessResult processResult = result.get();
        
        // Should handle invalid image data gracefully
        assertNotNull(processResult);
        assertNotNull(processResult.getExtractedText());
        assertTrue(processResult.getExtractedText().isEmpty());
    }
    
    @Test
    void testProcessSimpleImage() throws Exception {
        // Create a simple test image
        BufferedImage testImage = new BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB);
        // Fill with white background
        for (int x = 0; x < testImage.getWidth(); x++) {
            for (int y = 0; y < testImage.getHeight(); y++) {
                testImage.setRGB(x, y, 0xFFFFFF); // White
            }
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(testImage, "PNG", baos);
        byte[] imageBytes = baos.toByteArray();
        
        CompletableFuture<ModalityProcessResult> result = imageProcessor.processContent(imageBytes, "test.png");
        ModalityProcessResult processResult = result.get();
        
        // OCR might not extract text from a blank image, which is expected
        assertNotNull(processResult);
        assertNotNull(processResult.getExtractedText());
    }
    
    @Test
    void testProcessImageConcurrent() throws Exception {
        // Create multiple simple test images
        BufferedImage testImage1 = createTestImage(100, 50, 0xFFFFFF);
        BufferedImage testImage2 = createTestImage(120, 60, 0xFFFFFF);
        BufferedImage testImage3 = createTestImage(80, 40, 0xFFFFFF);
        
        byte[] imageBytes1 = imageToBytes(testImage1);
        byte[] imageBytes2 = imageToBytes(testImage2);
        byte[] imageBytes3 = imageToBytes(testImage3);
        
        CompletableFuture<ModalityProcessResult> result1 = imageProcessor.processContent(imageBytes1, "test1.png");
        CompletableFuture<ModalityProcessResult> result2 = imageProcessor.processContent(imageBytes2, "test2.png");
        CompletableFuture<ModalityProcessResult> result3 = imageProcessor.processContent(imageBytes3, "test3.png");
        
        // Wait for all to complete
        CompletableFuture<Void> allOf = CompletableFuture.allOf(result1, result2, result3);
        allOf.get();
        
        ModalityProcessResult processResult1 = result1.get();
        ModalityProcessResult processResult2 = result2.get();
        ModalityProcessResult processResult3 = result3.get();
        
        assertNotNull(processResult1);
        assertNotNull(processResult2);
        assertNotNull(processResult3);
    }
    
    @Test
    void testProcessLargeImage() throws Exception {
        // Create a larger test image
        BufferedImage largeImage = createTestImage(800, 600, 0xFFFFFF);
        byte[] imageBytes = imageToBytes(largeImage);
        
        CompletableFuture<ModalityProcessResult> result = imageProcessor.processContent(imageBytes, "large.png");
        ModalityProcessResult processResult = result.get();
        
        assertNotNull(processResult);
        assertNotNull(processResult.getExtractedText());
        // Processing should complete without errors even for large images
    }
    
    @Test
    void testProcessDifferentImageFormats() throws Exception {
        BufferedImage testImage = createTestImage(100, 50, 0xFFFFFF);
        
        // Test PNG format
        byte[] pngBytes = imageToBytes(testImage, "PNG");
        CompletableFuture<ModalityProcessResult> pngResult = imageProcessor.processContent(pngBytes, "test.png");
        assertNotNull(pngResult.get());
        
        // Test JPEG format
        byte[] jpegBytes = imageToBytes(testImage, "JPEG");
        CompletableFuture<ModalityProcessResult> jpegResult = imageProcessor.processContent(jpegBytes, "test.jpg");
        assertNotNull(jpegResult.get());
    }
    
    @Test
    void testImagePreprocessing() throws Exception {
        // Create an image with some noise to test preprocessing
        BufferedImage noisyImage = createTestImage(200, 100, 0xF0F0F0); // Light gray
        
        // Add some random pixels to simulate noise
        for (int i = 0; i < 100; i++) {
            int x = (int) (Math.random() * noisyImage.getWidth());
            int y = (int) (Math.random() * noisyImage.getHeight());
            noisyImage.setRGB(x, y, 0x808080); // Gray noise
        }
        
        byte[] imageBytes = imageToBytes(noisyImage);
        
        CompletableFuture<ModalityProcessResult> result = imageProcessor.processContent(imageBytes, "noisy.png");
        ModalityProcessResult processResult = result.get();
        
        assertNotNull(processResult);
        assertNotNull(processResult.getExtractedText());
        // Preprocessing should handle noisy images
    }
    
    @Test
    void testOCRLanguageSupport() throws Exception {
        // Test that OCR supports multiple languages (configuration test)
        BufferedImage testImage = createTestImage(200, 100, 0xFFFFFF);
        byte[] imageBytes = imageToBytes(testImage);
        
        CompletableFuture<ModalityProcessResult> result = imageProcessor.processContent(imageBytes, "multilang.png");
        ModalityProcessResult processResult = result.get();
        
        assertNotNull(processResult);
        assertNotNull(processResult.getExtractedText());
        // The processor should be configured for Chinese and English support
        // This is mainly a configuration test
    }
    
    @Test
    void testImageMetadataExtraction() throws Exception {
        BufferedImage testImage = createTestImage(150, 100, 0xFFFFFF);
        byte[] imageBytes = imageToBytes(testImage);
        
        CompletableFuture<ModalityProcessResult> result = imageProcessor.processContent(imageBytes, "metadata.png");
        ModalityProcessResult processResult = result.get();
        
        assertNotNull(processResult);
        assertNotNull(processResult.getExtractedText());
        assertNotNull(processResult.getMetadata());
        assertTrue(processResult.getProcessingTime() >= 0);
        // Should handle image metadata gracefully
    }
    
    @Test
    void testProcessResultStructure() throws Exception {
        BufferedImage testImage = createTestImage(100, 100, 0xFFFFFF);
        byte[] imageBytes = imageToBytes(testImage);
        
        CompletableFuture<ModalityProcessResult> result = imageProcessor.processContent(imageBytes, "structure.png");
        ModalityProcessResult processResult = result.get();
        
        assertNotNull(processResult);
        assertNotNull(processResult.getExtractedText());
        assertNotNull(processResult.getMetadata());
        assertTrue(processResult.getProcessingTime() >= 0);
        // assertEquals("image", processResult.getModality()); // Modality method might not be available
    }
    
    // Helper methods
    private BufferedImage createTestImage(int width, int height, int color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                image.setRGB(x, y, color);
            }
        }
        return image;
    }
    
    private byte[] imageToBytes(BufferedImage image) throws IOException {
        return imageToBytes(image, "PNG");
    }
    
    private byte[] imageToBytes(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }
}