package com.mem0.examples;

import com.mem0.examples.initialization.Mem0InitializationExamples;
import org.junit.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Code Quality Test for Examples
 * 
 * Tests that all example methods execute without throwing exceptions
 * and produce expected output formats.
 */
public class CodeQualityTest {

    @Test
    public void testAllInitializationExamples() {
        // Capture console output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        
        try {
            System.setOut(new PrintStream(outputStream));
            System.setErr(new PrintStream(outputStream));
            
            // Run all examples - should not throw exceptions
            Mem0InitializationExamples.demonstrateDefaultInitialization();
            Mem0InitializationExamples.demonstrateProgrammaticConfiguration();
            Mem0InitializationExamples.demonstrateEnvironmentBasedConfiguration();
            Mem0InitializationExamples.demonstrateCustomProviderInitialization();
            Mem0InitializationExamples.demonstrateStepByStepInitialization();
            Mem0InitializationExamples.demonstrateTestEnvironmentInitialization();
            
            String output = outputStream.toString();
            
            // Verify expected success indicators
            assert output.contains("✓") : "Output should contain success indicators";
            assert !output.contains("Exception") : "Output should not contain uncaught exceptions";
            
            System.out.println("All initialization examples executed successfully");
            
        } finally {
            // Restore original streams
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }
    
    @Test
    public void testValidationMethods() {
        try {
            // Test parameter validation through actual method calls
            System.setProperty("test.environment", "unit");
            Mem0InitializationExamples.demonstrateTestEnvironmentInitialization();
            
            System.setProperty("test.environment", "integration");
            Mem0InitializationExamples.demonstrateTestEnvironmentInitialization();
            
            System.setProperty("test.environment", "performance");
            Mem0InitializationExamples.demonstrateTestEnvironmentInitialization();
            
            System.out.println("Validation methods work correctly");
            
        } finally {
            System.clearProperty("test.environment");
        }
    }
}