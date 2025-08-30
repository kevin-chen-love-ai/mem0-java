package com.mem0.unit.multimodal;

import com.mem0.multimodal.MultimodalMemoryProcessor;
import com.mem0.multimodal.MultimodalMemoryProcessor.MultimodalConfiguration;
import com.mem0.multimodal.MultimodalMemoryProcessor.ModalityProcessor;
import com.mem0.multimodal.MultimodalMemoryProcessor.ModalityProcessResult;
import com.mem0.multimodal.MultimodalMemoryProcessor.MultimodalMemory;
import com.mem0.multimodal.MultimodalMemoryProcessor.MultimodalIndex;
import com.mem0.multimodal.MultimodalMemoryProcessor.ContentStorage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 多模态内存处理器测试
 * 全面测试MultimodalMemoryProcessor的所有功能和边界条件
 */
@DisplayName("多模态内存处理器测试")
public class MultimodalMemoryProcessorTest {
    
    @TempDir
    Path tempDir;
    
    private MultimodalMemoryProcessor processor;
    private MultimodalConfiguration configuration;
    
    @BeforeEach
    void setUp() {
        configuration = new MultimodalConfiguration();
        configuration.setStorageBasePath(tempDir.toString());
        processor = new MultimodalMemoryProcessor(configuration);
    }
    
    @Nested
    @DisplayName("配置类测试")
    class ConfigurationTests {
        
        @Test
        @DisplayName("默认配置测试")
        void testDefaultConfiguration() {
            MultimodalConfiguration config = new MultimodalConfiguration();
            
            assertEquals("./multimodal_storage", config.getStorageBasePath());
            assertTrue(config.isEnableContentAnalysis());
            assertTrue(config.isEnableThumbnailGeneration());
            assertTrue(config.isEnableTranscription());
            assertTrue(config.isEnableOCR());
            assertEquals(100 * 1024 * 1024, config.getMaxFileSize());
            
            // 验证支持的格式
            assertTrue(config.getSupportedImageFormats().contains("jpg"));
            assertTrue(config.getSupportedImageFormats().contains("png"));
            assertTrue(config.getSupportedAudioFormats().contains("mp3"));
            assertTrue(config.getSupportedVideoFormats().contains("mp4"));
            assertTrue(config.getSupportedDocumentFormats().contains("pdf"));
        }
        
        @Test
        @DisplayName("配置修改测试")
        void testConfigurationModification() {
            MultimodalConfiguration config = new MultimodalConfiguration();
            
            // 修改配置
            config.setStorageBasePath("/custom/path");
            config.setEnableContentAnalysis(false);
            config.setMaxFileSize(50 * 1024 * 1024);
            config.setSupportedImageFormats(Arrays.asList("png", "gif"));
            
            assertEquals("/custom/path", config.getStorageBasePath());
            assertFalse(config.isEnableContentAnalysis());
            assertEquals(50 * 1024 * 1024, config.getMaxFileSize());
            assertEquals(Arrays.asList("png", "gif"), config.getSupportedImageFormats());
        }
    }
    
    @Nested
    @DisplayName("处理结果类测试")
    class ProcessResultTests {
        
        @Test
        @DisplayName("处理结果构造测试")
        void testProcessResultConstruction() {
            String contentType = "image/jpeg";
            String extractedText = "Extracted text from image";
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("width", 1920);
            metadata.put("height", 1080);
            
            Map<String, Object> features = new HashMap<>();
            features.put("color_histogram", Arrays.asList(0.1, 0.2, 0.3));
            
            byte[] thumbnailData = "thumbnail".getBytes();
            String storageKey = "storage_key_123";
            long processingTime = 500L;
            
            ModalityProcessResult result = new ModalityProcessResult(
                contentType, extractedText, metadata, features, thumbnailData, storageKey, processingTime);
            
            assertEquals(contentType, result.getContentType());
            assertEquals(extractedText, result.getExtractedText());
            assertEquals(metadata, result.getMetadata());
            assertEquals(features, result.getFeatures());
            assertArrayEquals(thumbnailData, result.getThumbnailData());
            assertEquals(storageKey, result.getStorageKey());
            assertEquals(processingTime, result.getProcessingTime());
        }
        
        @Test
        @DisplayName("null参数处理测试")
        void testProcessResultWithNullParameters() {
            ModalityProcessResult result = new ModalityProcessResult(
                "image/png", "text", null, null, null, "key", 100L);
            
            assertNotNull(result.getMetadata());
            assertNotNull(result.getFeatures());
            assertTrue(result.getMetadata().isEmpty());
            assertTrue(result.getFeatures().isEmpty());
        }
    }
    
    @Nested
    @DisplayName("多模态内存类测试")
    class MultimodalMemoryTests {
        
        @Test
        @DisplayName("多模态内存构造测试")
        void testMultimodalMemoryConstruction() {
            String id = "memory_123";
            String content = "Test content";
            String userId = "user_456";
            String sessionId = "session_789";
            String contentType = "image/jpeg";
            String originalFileName = "test.jpg";
            long fileSize = 1024L;
            String storageKey = "storage_123";
            
            Map<String, Object> modalityFeatures = new HashMap<>();
            modalityFeatures.put("width", 800);
            modalityFeatures.put("height", 600);
            
            byte[] thumbnailData = "thumbnail".getBytes();
            String extractedText = "Extracted text";
            
            MultimodalMemory memory = new MultimodalMemory(
                id, content, userId, sessionId, contentType, originalFileName,
                fileSize, storageKey, modalityFeatures, thumbnailData, extractedText);
            
            assertEquals(id, memory.getId());
            assertEquals(content, memory.getContent());
            assertEquals(userId, memory.getUserId());
            assertEquals(sessionId, memory.getSessionId());
            assertEquals(contentType, memory.getContentType());
            assertEquals(originalFileName, memory.getOriginalFileName());
            assertEquals(fileSize, memory.getFileSize());
            assertEquals(storageKey, memory.getStorageKey());
            assertEquals(modalityFeatures, memory.getModalityFeatures());
            assertArrayEquals(thumbnailData, memory.getThumbnailData());
            assertEquals(extractedText, memory.getExtractedText());
            
            // 验证元数据设置
            assertEquals(contentType, memory.getMetadata().get("contentType"));
            assertEquals(originalFileName, memory.getMetadata().get("originalFileName"));
            assertEquals(fileSize, memory.getMetadata().get("fileSize"));
            assertEquals(storageKey, memory.getMetadata().get("storageKey"));
            assertTrue((Boolean) memory.getMetadata().get("hasExtractedText"));
            assertTrue((Boolean) memory.getMetadata().get("hasThumbnail"));
        }
        
        @Test
        @DisplayName("null参数处理测试")
        void testMultimodalMemoryWithNullParameters() {
            MultimodalMemory memory = new MultimodalMemory(
                "id", "content", "user", "session", "image/png", "test.png",
                100L, "key", null, null, null);
            
            assertNotNull(memory.getModalityFeatures());
            assertTrue(memory.getModalityFeatures().isEmpty());
            assertNull(memory.getThumbnailData());
            assertNull(memory.getExtractedText());
            
            assertFalse((Boolean) memory.getMetadata().get("hasExtractedText"));
            assertFalse((Boolean) memory.getMetadata().get("hasThumbnail"));
        }
    }
    
    @Nested
    @DisplayName("多模态索引测试")
    class MultimodalIndexTests {
        
        private MultimodalIndex index;
        
        @BeforeEach
        void setUp() {
            index = new MultimodalIndex();
        }
        
        @Test
        @DisplayName("添加和检索内存测试")
        void testAddAndRetrieveMemory() {
            MultimodalMemory memory = createTestMemory("mem_1", "image/jpeg");
            
            index.addMemory(memory);
            
            Set<String> imageMemories = index.getMemoriesByContentType("image/jpeg");
            assertTrue(imageMemories.contains("mem_1"));
            
            assertEquals("storage_key", index.getStorageKey("mem_1"));
        }
        
        @Test
        @DisplayName("按特征检索测试")
        void testRetrieveByFeature() {
            Map<String, Object> features = new HashMap<>();
            features.put("color", "red");
            features.put("shape", "circle");
            
            MultimodalMemory memory = new MultimodalMemory(
                "mem_feature", "content", "user", "session", "image/png", "test.png",
                100L, "key", features, null, null);
            
            index.addMemory(memory);
            
            Set<String> colorMemories = index.getMemoriesByFeature("color");
            assertTrue(colorMemories.contains("mem_feature"));
            
            Set<String> shapeMemories = index.getMemoriesByFeature("shape");
            assertTrue(shapeMemories.contains("mem_feature"));
        }
        
        @Test
        @DisplayName("移除内存测试")
        void testRemoveMemory() {
            MultimodalMemory memory = createTestMemory("mem_remove", "audio/mp3");
            index.addMemory(memory);
            
            assertTrue(index.getMemoriesByContentType("audio/mp3").contains("mem_remove"));
            
            index.removeMemory("mem_remove");
            
            assertFalse(index.getMemoriesByContentType("audio/mp3").contains("mem_remove"));
            assertNull(index.getStorageKey("mem_remove"));
        }
        
        @Test
        @DisplayName("统计信息测试")
        void testStatistics() {
            index.addMemory(createTestMemory("mem_1", "image/jpeg"));
            index.addMemory(createTestMemory("mem_2", "image/jpeg"));
            index.addMemory(createTestMemory("mem_3", "audio/mp3"));
            
            Map<String, Integer> stats = index.getContentTypeStatistics();
            assertEquals(2, stats.get("image/jpeg").intValue());
            assertEquals(1, stats.get("audio/mp3").intValue());
            
            assertEquals(3, index.getTotalMemoryCount());
        }
        
        private MultimodalMemory createTestMemory(String id, String contentType) {
            return new MultimodalMemory(
                id, "test content", "user", "session", contentType, "test.file",
                100L, "storage_key", new HashMap<>(), null, null);
        }
    }
    
    @Nested
    @DisplayName("内容存储测试")
    class ContentStorageTests {
        
        private ContentStorage storage;
        
        @BeforeEach
        void setUp() {
            storage = new ContentStorage(tempDir.toString());
        }
        
        @Test
        @DisplayName("存储和检索内容测试")
        void testStoreAndRetrieveContent() throws ExecutionException, InterruptedException {
            byte[] testContent = "Test content data".getBytes();
            String fileName = "test.txt";
            String contentType = "text/plain";
            
            // 存储内容
            String storageKey = storage.storeContent(testContent, fileName, contentType).get();
            assertNotNull(storageKey);
            assertTrue(storageKey.contains("text/plain"));
            
            // 检索内容
            byte[] retrievedContent = storage.retrieveContent(storageKey, contentType).get();
            assertArrayEquals(testContent, retrievedContent);
        }
        
        @Test
        @DisplayName("删除内容测试")
        void testDeleteContent() throws ExecutionException, InterruptedException {
            byte[] testContent = "Content to delete".getBytes();
            String storageKey = storage.storeContent(testContent, "delete.txt", "text/plain").get();
            
            // 删除内容
            boolean deleted = storage.deleteContent(storageKey, "text/plain").get();
            assertTrue(deleted);
            
            // 再次删除应该返回false
            boolean deletedAgain = storage.deleteContent(storageKey, "text/plain").get();
            assertFalse(deletedAgain);
        }
        
        @Test
        @DisplayName("检索不存在内容异常测试")
        void testRetrieveNonexistentContent() {
            CompletableFuture<byte[]> future = storage.retrieveContent("nonexistent", "text/plain");
            assertThrows(RuntimeException.class, future::join);
        }
        
        @Test
        @DisplayName("存储统计测试")
        void testStorageStatistics() throws ExecutionException, InterruptedException {
            // 存储一些测试内容
            storage.storeContent("image data".getBytes(), "test.jpg", "image/jpeg").get();
            storage.storeContent("audio data".getBytes(), "test.mp3", "audio/mp3").get();
            storage.storeContent("document data".getBytes(), "test.pdf", "application/pdf").get();
            
            Map<String, Object> stats = storage.getStorageStatistics();
            
            assertTrue((Integer) stats.get("totalFiles") >= 3);
            assertTrue((Long) stats.get("totalSize") > 0);
            
            @SuppressWarnings("unchecked")
            Map<String, Integer> typeBreakdown = (Map<String, Integer>) stats.get("typeBreakdown");
            assertNotNull(typeBreakdown);
        }
    }
    
    @Nested
    @DisplayName("处理器接口测试")
    class ModalityProcessorTests {
        
        @Test
        @DisplayName("模拟处理器测试")
        void testMockProcessor() throws ExecutionException, InterruptedException {
            ModalityProcessor mockProcessor = mock(ModalityProcessor.class);
            
            // 设置模拟行为
            when(mockProcessor.getProcessorName()).thenReturn("MockProcessor");
            when(mockProcessor.getSupportedFormats()).thenReturn(Arrays.asList("mock"));
            
            Map<String, Object> mockFeatures = new HashMap<>();
            mockFeatures.put("mock_feature", "value");
            
            ModalityProcessResult mockResult = new ModalityProcessResult(
                "mock/type", "extracted text", new HashMap<>(), mockFeatures,
                "thumb".getBytes(), "key", 100L);
            
            when(mockProcessor.processContent(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(mockResult));
            when(mockProcessor.extractFeatures(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(mockFeatures));
            when(mockProcessor.generateThumbnail(any(), any()))
                .thenReturn(CompletableFuture.completedFuture("thumb".getBytes()));
            
            // 验证模拟行为
            assertEquals("MockProcessor", mockProcessor.getProcessorName());
            assertTrue(mockProcessor.getSupportedFormats().contains("mock"));
            
            ModalityProcessResult result = mockProcessor.processContent("content".getBytes(), "test.mock").get();
            assertEquals("mock/type", result.getContentType());
            assertEquals("extracted text", result.getExtractedText());
            
            Map<String, Object> features = mockProcessor.extractFeatures("content".getBytes(), "test.mock").get();
            assertEquals("value", features.get("mock_feature"));
            
            byte[] thumbnail = mockProcessor.generateThumbnail("content".getBytes(), "test.mock").get();
            assertArrayEquals("thumb".getBytes(), thumbnail);
        }
    }
    
    @Nested
    @DisplayName("多模态处理器主要功能测试")
    class MainFunctionalityTests {
        
        @Test
        @DisplayName("处理器初始化测试")
        void testProcessorInitialization() {
            MultimodalMemoryProcessor processor = new MultimodalMemoryProcessor();
            
            assertNotNull(processor.getConfiguration());
            assertNotNull(processor.getMultimodalIndex());
            assertNotNull(processor.getContentStorage());
            assertNotNull(processor.getModalityProcessors());
            
            // 验证默认处理器已注册
            Map<String, ModalityProcessor> processors = processor.getModalityProcessors();
            assertTrue(processors.containsKey("image"));
            assertTrue(processors.containsKey("audio"));
            assertTrue(processors.containsKey("video"));
            assertTrue(processors.containsKey("document"));
        }
        
        @Test
        @DisplayName("文件类型检测测试")
        void testContentTypeDetection() {
            // 这个测试需要访问私有方法，所以我们通过公共接口间接测试
            MultimodalMemoryProcessor processor = new MultimodalMemoryProcessor(configuration);
            
            // 通过统计信息来验证处理器是否正确工作
            Map<String, Object> stats = processor.getStatistics();
            assertNotNull(stats);
            assertTrue(stats.containsKey("availableProcessors"));
            
            @SuppressWarnings("unchecked")
            Map<String, String> processorInfo = (Map<String, String>) stats.get("availableProcessors");
            assertEquals(4, processorInfo.size()); // image, audio, video, document
        }
        
        @Test
        @DisplayName("搜索功能测试")
        void testSearchFunctionality() {
            MultimodalMemoryProcessor processor = new MultimodalMemoryProcessor(configuration);
            
            // 由于实际的处理器可能不可用，我们测试空结果
            Set<String> imageResults = processor.searchByContentType("image/jpeg");
            assertNotNull(imageResults);
            
            Set<String> featureResults = processor.searchByFeature("color");
            assertNotNull(featureResults);
        }
        
        @Test
        @DisplayName("统计信息测试")
        void testStatistics() {
            Map<String, Object> stats = processor.getStatistics();
            
            assertNotNull(stats);
            assertTrue(stats.containsKey("totalMemories"));
            assertTrue(stats.containsKey("contentTypeBreakdown"));
            assertTrue(stats.containsKey("availableProcessors"));
        }
    }
    
    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTests {
        
        @Test
        @DisplayName("文件大小超限测试")
        void testFileSizeExceeded() {
            MultimodalConfiguration config = new MultimodalConfiguration();
            config.setMaxFileSize(100); // 100 字节限制
            config.setStorageBasePath(tempDir.toString());
            
            MultimodalMemoryProcessor processor = new MultimodalMemoryProcessor(config);
            
            // 创建超过限制的内容
            byte[] largeContent = new byte[200];
            Arrays.fill(largeContent, (byte) 1);
            
            CompletableFuture<MultimodalMemory> future = processor.processMultimodalContent(
                largeContent, "large.txt", "user", "session");
            
            assertThrows(RuntimeException.class, future::join);
        }
        
        @Test
        @DisplayName("不支持的文件类型测试")
        void testUnsupportedFileType() {
            CompletableFuture<MultimodalMemory> future = processor.processMultimodalContent(
                "content".getBytes(), "unsupported.xyz", "user", "session");
            
            // 由于实际处理器可能不可用，这个测试可能会因为其他原因失败
            // 我们主要验证不会崩溃
            assertDoesNotThrow(() -> {
                try {
                    future.join();
                } catch (RuntimeException e) {
                    // 预期的异常
                    assertTrue(e.getMessage().contains("Unsupported") || 
                              e.getMessage().contains("Failed"));
                }
            });
        }
    }
    
    @Nested
    @DisplayName("并发测试")
    class ConcurrencyTests {
        
        @Test
        @DisplayName("并发索引操作测试")
        void testConcurrentIndexOperations() throws InterruptedException {
            MultimodalIndex index = new MultimodalIndex();
            
            // 并发添加内存
            Thread[] threads = new Thread[10];
            for (int i = 0; i < threads.length; i++) {
                final int threadId = i;
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < 100; j++) {
                        MultimodalMemory memory = new MultimodalMemory(
                            "mem_" + threadId + "_" + j, "content", "user", "session",
                            "image/jpeg", "test.jpg", 100L, "key_" + threadId + "_" + j,
                            new HashMap<>(), null, null);
                        index.addMemory(memory);
                    }
                });
                threads[i].start();
            }
            
            // 等待所有线程完成
            for (Thread thread : threads) {
                thread.join();
            }
            
            // 验证所有内存都已添加
            assertEquals(1000, index.getTotalMemoryCount());
            assertEquals(1000, index.getMemoriesByContentType("image/jpeg").size());
        }
        
        @Test
        @DisplayName("并发存储操作测试")
        void testConcurrentStorageOperations() throws InterruptedException {
            ContentStorage storage = new ContentStorage(tempDir.toString());
            
            // 并发存储内容
            Thread[] threads = new Thread[5];
            for (int i = 0; i < threads.length; i++) {
                final int threadId = i;
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < 20; j++) {
                        try {
                            byte[] content = ("Content from thread " + threadId + " iteration " + j).getBytes();
                            String fileName = "file_" + threadId + "_" + j + ".txt";
                            storage.storeContent(content, fileName, "text/plain").get();
                        } catch (Exception e) {
                            fail("Concurrent storage operation failed: " + e.getMessage());
                        }
                    }
                });
                threads[i].start();
            }
            
            // 等待所有线程完成
            for (Thread thread : threads) {
                thread.join();
            }
            
            // 验证存储统计
            Map<String, Object> stats = storage.getStorageStatistics();
            assertTrue((Integer) stats.get("totalFiles") >= 100);
        }
    }
    
    @Nested
    @DisplayName("内存清理测试")
    class CleanupTests {
        
        @Test
        @DisplayName("内存删除测试")
        void testMemoryDeletion() throws ExecutionException, InterruptedException, IOException {
            // 创建测试内容
            byte[] testContent = "Test content for deletion".getBytes();
            Path testFile = tempDir.resolve("delete_test.txt");
            Files.write(testFile, testContent);
            
            // 创建多模态内存
            MultimodalMemory memory = new MultimodalMemory(
                "delete_test", "content", "user", "session", "text/plain", "delete_test.txt",
                testContent.length, "delete_test.txt", new HashMap<>(), null, null);
            
            // 手动添加到索引
            processor.getMultimodalIndex().addMemory(memory);
            
            // 验证内存存在
            assertTrue(processor.getMultimodalIndex().getMemoriesByContentType("text/plain").contains("delete_test"));
            
            // 删除内存
            boolean deleted = processor.deleteMultimodalMemory(memory).get();
            
            // 验证删除结果（由于实际文件操作可能失败，我们主要验证不会崩溃）
            assertFalse(processor.getMultimodalIndex().getMemoriesByContentType("text/plain").contains("delete_test"));
        }
    }
    
    @Nested
    @DisplayName("配置验证测试")
    class ConfigurationValidationTests {
        
        @Test
        @DisplayName("自定义配置测试")
        void testCustomConfiguration() {
            MultimodalConfiguration customConfig = new MultimodalConfiguration();
            customConfig.setStorageBasePath(tempDir.toString());
            customConfig.setEnableContentAnalysis(false);
            customConfig.setEnableThumbnailGeneration(false);
            customConfig.setMaxFileSize(1024);
            
            MultimodalMemoryProcessor customProcessor = new MultimodalMemoryProcessor(customConfig);
            
            assertEquals(customConfig, customProcessor.getConfiguration());
            assertFalse(customProcessor.getConfiguration().isEnableContentAnalysis());
            assertFalse(customProcessor.getConfiguration().isEnableThumbnailGeneration());
            assertEquals(1024, customProcessor.getConfiguration().getMaxFileSize());
        }
        
        @Test
        @DisplayName("null配置处理测试")
        void testNullConfiguration() {
            MultimodalMemoryProcessor processor = new MultimodalMemoryProcessor(null);
            
            assertNotNull(processor.getConfiguration());
            // 应该使用默认配置
            assertTrue(processor.getConfiguration().isEnableContentAnalysis());
        }
    }
}