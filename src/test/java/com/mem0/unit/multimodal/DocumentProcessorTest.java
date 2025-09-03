package com.mem0.unit.multimodal;

import com.mem0.multimodal.DocumentProcessor;
import com.mem0.multimodal.MultimodalMemoryProcessor.MultimodalConfiguration;
import com.mem0.multimodal.MultimodalMemoryProcessor.ModalityProcessResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for DocumentProcessor functionality
 */
public class DocumentProcessorTest {
    
    private DocumentProcessor documentProcessor;
    
    @BeforeEach
    void setUp() {
        MultimodalConfiguration config = new MultimodalConfiguration();
        documentProcessor = new DocumentProcessor(config);
    }
    
    @Test
    void testProcessTextFile() throws Exception {
        String textContent = "This is a test document with some content.";
        byte[] content = textContent.getBytes(StandardCharsets.UTF_8);
        
        CompletableFuture<ModalityProcessResult> result = documentProcessor.processContent(content, "test.txt");
        ModalityProcessResult processResult = result.get();
        
        assertNotNull(processResult);
        assertNotNull(processResult.getExtractedText());
        assertTrue(processResult.getExtractedText().contains("test document"));
        assertTrue(processResult.getExtractedText().contains("content"));
    }
    
    @Test
    void testProcessEmptyDocument() throws Exception {
        byte[] content = new byte[0];
        
        CompletableFuture<ModalityProcessResult> result = documentProcessor.processContent(content, "empty.txt");
        ModalityProcessResult processResult = result.get();
        
        assertNotNull(processResult);
        assertNotNull(processResult.getExtractedText());
        assertTrue(processResult.getExtractedText().isEmpty());
    }
    
    @Test
    void testProcessNullContent() throws Exception {
        CompletableFuture<ModalityProcessResult> result = documentProcessor.processContent(null, "test.txt");
        ModalityProcessResult processResult = result.get();
        
        assertNotNull(processResult);
        assertNotNull(processResult.getExtractedText());
        assertTrue(processResult.getExtractedText().isEmpty());
    }
    
    @Test
    void testProcessUnsupportedFormat() throws Exception {
        byte[] content = "some content".getBytes(StandardCharsets.UTF_8);
        
        CompletableFuture<ModalityProcessResult> result = documentProcessor.processContent(content, "test.xyz");
        ModalityProcessResult processResult = result.get();
        
        // Should handle unsupported formats gracefully
        assertNotNull(processResult);
        assertNotNull(processResult.getExtractedText());
        assertTrue(processResult.getExtractedText().contains("some content"));
    }
    
    @Test
    void testProcessPDFDocument() throws Exception {
        // Create a minimal PDF-like structure for testing
        String pdfContent = "This looks like PDF content for testing";
        byte[] pdfBytes = pdfContent.getBytes(StandardCharsets.UTF_8);
        
        CompletableFuture<ModalityProcessResult> result = documentProcessor.processContent(pdfBytes, "test.pdf");
        ModalityProcessResult processResult = result.get();
        
        assertNotNull(processResult);
        assertNotNull(processResult.getExtractedText());
        // PDF processing should handle the content gracefully
        assertFalse(processResult.getExtractedText() == null);
    }
    
    @Test
    void testProcessLargeDocument() throws Exception {
        // Create a large text document
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            largeContent.append("This is line ").append(i).append(" of a large document. ");
            largeContent.append("It contains various information and content for testing purposes.\n");
        }
        
        byte[] content = largeContent.toString().getBytes(StandardCharsets.UTF_8);
        
        CompletableFuture<ModalityProcessResult> result = documentProcessor.processContent(content, "large.txt");
        ModalityProcessResult processResult = result.get();
        
        assertNotNull(processResult);
        assertNotNull(processResult.getExtractedText());
        assertTrue(processResult.getExtractedText().length() > 1000);
        assertTrue(processResult.getExtractedText().contains("line 0"));
        assertTrue(processResult.getExtractedText().contains("line 99"));
    }
    
    @Test
    void testProcessDocumentWithSpecialCharacters() throws Exception {
        String content = "特殊字符测试 Special characters test éñüñ 中文测试 русский 日本語 العربية";
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        
        CompletableFuture<ModalityProcessResult> result = documentProcessor.processContent(bytes, "special.txt");
        ModalityProcessResult processResult = result.get();
        
        assertNotNull(processResult);
        assertNotNull(processResult.getExtractedText());
        assertTrue(processResult.getExtractedText().contains("特殊字符"));
        assertTrue(processResult.getExtractedText().contains("Special"));
        assertTrue(processResult.getExtractedText().contains("中文"));
    }
    
    @Test
    void testConcurrentProcessing() throws Exception {
        String content1 = "Document 1 content for concurrent testing";
        String content2 = "Document 2 content for concurrent testing";
        String content3 = "Document 3 content for concurrent testing";
        
        CompletableFuture<ModalityProcessResult> result1 = documentProcessor.processContent(
            content1.getBytes(StandardCharsets.UTF_8), "doc1.txt");
        CompletableFuture<ModalityProcessResult> result2 = documentProcessor.processContent(
            content2.getBytes(StandardCharsets.UTF_8), "doc2.txt");
        CompletableFuture<ModalityProcessResult> result3 = documentProcessor.processContent(
            content3.getBytes(StandardCharsets.UTF_8), "doc3.txt");
        
        // Wait for all to complete
        CompletableFuture<Void> allOf = CompletableFuture.allOf(result1, result2, result3);
        allOf.get();
        
        ModalityProcessResult processResult1 = result1.get();
        ModalityProcessResult processResult2 = result2.get();
        ModalityProcessResult processResult3 = result3.get();
        
        assertNotNull(processResult1);
        assertNotNull(processResult2);
        assertNotNull(processResult3);
        
        assertTrue(processResult1.getExtractedText().contains("Document 1"));
        assertTrue(processResult2.getExtractedText().contains("Document 2"));
        assertTrue(processResult3.getExtractedText().contains("Document 3"));
    }
    
    @Test
    void testGetSupportedFormats() {
        // Test that the processor handles different formats without throwing exceptions
        assertDoesNotThrow(() -> {
            documentProcessor.processContent("content".getBytes(), "test.pdf").get();
            documentProcessor.processContent("content".getBytes(), "test.doc").get();
            documentProcessor.processContent("content".getBytes(), "test.docx").get();
            documentProcessor.processContent("content".getBytes(), "test.txt").get();
        });
    }
    
    @Test
    void testProcessResultMetadata() throws Exception {
        String content = "Test document with metadata";
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        
        CompletableFuture<ModalityProcessResult> result = documentProcessor.processContent(bytes, "metadata.txt");
        ModalityProcessResult processResult = result.get();
        
        assertNotNull(processResult);
        assertNotNull(processResult.getExtractedText());
        assertNotNull(processResult.getMetadata());
        assertTrue(processResult.getProcessingTime() >= 0);
    }
}