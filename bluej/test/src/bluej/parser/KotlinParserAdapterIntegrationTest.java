/*
 This file is part of the BlueJ program. 
 Copyright (C) 2025  Michael Kolling and John Rosenberg

 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 

 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 

 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 

 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.parser;

import bluej.parser.CallbackDelegate.SourceType;
import bluej.parser.KotlinParserAdapter.ParserType;
import bluej.parser.KotlinParserAdapter.PerformanceMetrics;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.nodes.NodeTree;
import bluej.parser.entity.EntityResolver;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;

import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.StringReader;
import java.io.Reader;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Integration tests for KotlinParserAdapter with CallbackDelegate.
 * 
 * <p>This test suite verifies the complete integration between:
 * <ul>
 *   <li>KotlinParserAdapter dynamic parser selection</li>
 *   <li>CallbackDelegate callback propagation</li>
 *   <li>SourceParser interaction</li>
 *   <li>Real Kotlin source parsing scenarios</li>
 * </ul>
 * 
 * @since BlueJ 5.4.0
 * @author BlueJ Development Team
 */
@RunWith(JUnit4.class)
public class KotlinParserAdapterIntegrationTest {
    
    private EntityResolver mockEntityResolver;
    private CallbackDelegate testDelegate;
    private Properties originalConfig;
    
    // Test tracking variables
    private AtomicInteger packageCallbackCount;
    private AtomicInteger importCallbackCount;
    private AtomicInteger classCallbackCount;
    private AtomicInteger methodCallbackCount;
    private AtomicBoolean errorOccurred;
    
    @Before
    public void setUp() {
        // Save original configuration
        originalConfig = new Properties();
        originalConfig.putAll(System.getProperties());
        
        // Enable performance monitoring for tests
        System.setProperty("kotlin.parser.performance.monitoring", "true");
        System.setProperty("kotlin.parser.fallback.enabled", "true");
        
        // Create mock entity resolver
        mockEntityResolver = mock(EntityResolver.class);
        
        // Initialize tracking
        packageCallbackCount = new AtomicInteger(0);
        importCallbackCount = new AtomicInteger(0);
        classCallbackCount = new AtomicInteger(0);
        methodCallbackCount = new AtomicInteger(0);
        errorOccurred = new AtomicBoolean(false);
        
        // Create test delegate with callback implementations
        testDelegate = new CallbackDelegate() {
            @Override
            public void beginPackage(String packageName) {
                packageCallbackCount.incrementAndGet();
            }
            
            @Override
            public void beginImport(String importName, boolean isStatic) {
                importCallbackCount.incrementAndGet();
            }
            
            @Override
            public void beginClass(String className, SourceType sourceType) {
                classCallbackCount.incrementAndGet();
            }
            
            @Override
            public void beginMethod(String methodName, String returnType) {
                methodCallbackCount.incrementAndGet();
            }
            
            @Override
            public void parseError(String message, int line, int column) {
                errorOccurred.set(true);
            }
        };
    }
    
    @After
    public void tearDown() {
        // Restore original configuration
        System.setProperties(originalConfig);
    }
    
    // ==================== Basic Integration Tests ====================
    
    @Test
    public void testBasicKotlinSourceParsing() {
        String kotlinSource = 
            "package com.example.test\n" +
            "\n" +
            "import java.util.List\n" +
            "import kotlin.collections.ArrayList\n" +
            "\n" +
            "class TestClass {\n" +
            "    fun testMethod(): String {\n" +
            "        return \"test\"\n" +
            "    }\n" +
            "}\n";
        
        // Create parser with adapter
        StringReader reader = new StringReader(kotlinSource);
        SourceParser sourceParser = new SourceParser(reader, mockEntityResolver, "TestClass.kt");
        sourceParser.setDelegate(testDelegate);
        
        KotlinParserAdapter adapter = new KotlinParserAdapter(sourceParser);
        adapter.switchParser(ParserType.LEGACY);
        
        // Parse the source
        ParsedCUNode result = adapter.parseCU();
        
        // Verify callbacks were triggered
        assertTrue("Package callback should be triggered", packageCallbackCount.get() > 0);
        assertTrue("Import callbacks should be triggered", importCallbackCount.get() >= 2);
        assertTrue("Class callback should be triggered", classCallbackCount.get() > 0);
        assertTrue("Method callback should be triggered", methodCallbackCount.get() > 0);
        assertFalse("No errors should occur", errorOccurred.get());
        
        // Verify performance metrics were recorded
        PerformanceMetrics metrics = adapter.getLegacyMetrics();
        assertTrue("Parse count should be > 0", metrics.getParseCount() > 0);
    }
    
    @Test
    public void testCallbackDelegatePropagation() {
        // Create custom delegate to track specific callbacks
        final CountDownLatch beginClassLatch = new CountDownLatch(1);
        final CountDownLatch endClassLatch = new CountDownLatch(1);
        final AtomicBoolean beginCalled = new AtomicBoolean(false);
        final AtomicBoolean endCalled = new AtomicBoolean(false);
        
        CallbackDelegate customDelegate = new CallbackDelegate() {
            @Override
            public void beginClass(String className, SourceType sourceType) {
                beginCalled.set(true);
                assertEquals("TestClass", className);
                assertEquals(SourceType.CLASS, sourceType);
                beginClassLatch.countDown();
            }
            
            @Override
            public void endClass(String className) {
                endCalled.set(true);
                assertEquals("TestClass", className);
                endClassLatch.countDown();
            }
        };
        
        String kotlinSource = "class TestClass { }";
        
        StringReader reader = new StringReader(kotlinSource);
        SourceParser sourceParser = new SourceParser(reader, mockEntityResolver, "TestClass.kt");
        sourceParser.setDelegate(customDelegate);
        
        KotlinParserAdapter adapter = new KotlinParserAdapter(sourceParser);
        
        // Parse the source
        adapter.parseTypeDef();
        
        // Wait for callbacks with timeout
        try {
            assertTrue("Begin class callback should be called", 
                      beginClassLatch.await(5, TimeUnit.SECONDS));
            assertTrue("End class callback should be called", 
                      endClassLatch.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            fail("Interrupted while waiting for callbacks");
        }
        
        assertTrue("Begin callback was called", beginCalled.get());
        assertTrue("End callback was called", endCalled.get());
    }
    
    // ==================== Parser Switching Tests ====================
    
    @Test
    public void testDynamicParserSwitching() {
        String kotlinSource = "class TestClass { fun test() {} }";
        
        StringReader reader = new StringReader(kotlinSource);
        SourceParser sourceParser = new SourceParser(reader, mockEntityResolver, "TestClass.kt");
        sourceParser.setDelegate(testDelegate);
        
        KotlinParserAdapter adapter = new KotlinParserAdapter(sourceParser);
        
        // Parse with legacy parser
        adapter.switchParser(ParserType.LEGACY);
        adapter.parseTypeDef();
        
        PerformanceMetrics legacyMetrics = adapter.getLegacyMetrics();
        long legacyParseCount = legacyMetrics.getParseCount();
        
        // Reset and parse with Pratt parser
        reader = new StringReader(kotlinSource);
        sourceParser = new SourceParser(reader, mockEntityResolver, "TestClass.kt");
        sourceParser.setDelegate(testDelegate);
        adapter = new KotlinParserAdapter(sourceParser);
        
        adapter.switchParser(ParserType.PRATT);
        adapter.parseTypeDef();
        
        PerformanceMetrics prattMetrics = adapter.getPrattMetrics();
        long prattParseCount = prattMetrics.getParseCount();
        
        // Verify both parsers were used
        assertTrue("Legacy parser should have metrics", legacyParseCount > 0);
        assertTrue("Pratt parser should have metrics", prattParseCount > 0);
    }
    
    @Test
    public void testAutoParserSelection() {
        // Test with simple code (should select legacy)
        String simpleSource = "class SimpleClass { }";
        
        StringReader reader = new StringReader(simpleSource);
        SourceParser sourceParser = new SourceParser(reader, mockEntityResolver, "SimpleClass.kt");
        sourceParser.setDelegate(testDelegate);
        
        KotlinParserAdapter adapter = new KotlinParserAdapter(sourceParser);
        adapter.switchParser(ParserType.AUTO);
        
        adapter.parseTypeDef();
        
        // For simple code, auto mode should work with either parser
        assertNotNull("Should have selected a parser", adapter.getCurrentParserType());
        
        // Test with complex code (would prefer Pratt if available)
        String complexSource = 
            "class ComplexClass<T : Any> where T : Comparable<T> {\n" +
            "    inline fun <reified R> process(value: T): R? {\n" +
            "        return null\n" +
            "    }\n" +
            "}";
        
        reader = new StringReader(complexSource);
        sourceParser = new SourceParser(reader, mockEntityResolver, "ComplexClass.kt");
        sourceParser.setDelegate(testDelegate);
        
        adapter = new KotlinParserAdapter(sourceParser);
        adapter.switchParser(ParserType.AUTO);
        
        adapter.parseTypeDef();
        
        // Auto mode should handle complex code
        assertNotNull("Should handle complex code", adapter.getCurrentParserType());
    }
    
    // ==================== Fallback Mechanism Tests ====================
    
    @Test
    public void testFallbackMechanism() {
        // Create a source that might cause issues
        String problematicSource = 
            "class TestClass {\n" +
            "    // This might cause parser issues\n" +
            "    val property: ((Int) -> Unit)? = null\n" +
            "}";
        
        StringReader reader = new StringReader(problematicSource);
        SourceParser sourceParser = new SourceParser(reader, mockEntityResolver, "TestClass.kt");
        sourceParser.setDelegate(testDelegate);
        
        KotlinParserAdapter adapter = new KotlinParserAdapter(sourceParser);
        
        // Ensure fallback is enabled
        System.setProperty("kotlin.parser.fallback.enabled", "true");
        
        // Try parsing with Pratt first
        adapter.switchParser(ParserType.PRATT);
        ParsedCUNode result = adapter.parseCU();
        
        // Even if Pratt fails, fallback should ensure we get a result
        assertNotNull("Should get a result even with fallback", result);
        
        // Check if fallback was triggered
        PerformanceMetrics prattMetrics = adapter.getPrattMetrics();
        PerformanceMetrics legacyMetrics = adapter.getLegacyMetrics();
        
        // At least one parser should have processed it
        assertTrue("At least one parser should have metrics", 
                  prattMetrics.getParseCount() > 0 || legacyMetrics.getParseCount() > 0);
    }
    
    // ==================== Performance Monitoring Tests ====================
    
    @Test
    public void testPerformanceMetricsAccumulation() {
        KotlinParserAdapter adapter = new KotlinParserAdapter(
            new SourceParser(new StringReader(""), mockEntityResolver, "test.kt"));
        
        // Switch to legacy and perform operations
        adapter.switchParser(ParserType.LEGACY);
        
        for (int i = 0; i < 10; i++) {
            adapter.parseExpression();
        }
        
        PerformanceMetrics legacyMetrics = adapter.getLegacyMetrics();
        assertEquals("Should have 10 parse operations", 10, legacyMetrics.getParseCount());
        
        // Switch to Pratt and perform operations
        adapter.switchParser(ParserType.PRATT);
        
        for (int i = 0; i < 5; i++) {
            adapter.parseStatement();
        }
        
        PerformanceMetrics prattMetrics = adapter.getPrattMetrics();
        assertEquals("Should have 5 parse operations", 5, prattMetrics.getParseCount());
        
        // Legacy metrics should remain unchanged
        assertEquals("Legacy metrics should still be 10", 10, legacyMetrics.getParseCount());
    }
    
    @Test
    public void testCombinedMetrics() {
        KotlinParserAdapter adapter = new KotlinParserAdapter(
            new SourceParser(new StringReader(""), mockEntityResolver, "test.kt"));
        
        // Perform operations with both parsers
        adapter.switchParser(ParserType.LEGACY);
        adapter.parseExpression();
        adapter.parseStatement();
        
        adapter.switchParser(ParserType.PRATT);
        adapter.parseTypeDef();
        adapter.parseImportStatement();
        
        // Get combined metrics
        PerformanceMetrics combinedMetrics = adapter.getCombinedMetrics();
        
        assertEquals("Combined parse count should be 4", 
                    4, combinedMetrics.getParseCount());
    }
    
    // ==================== Thread Safety Tests ====================
    
    @Test
    public void testConcurrentCallbackDelegation() throws InterruptedException {
        final int threadCount = 10;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(threadCount);
        final AtomicInteger totalCallbacks = new AtomicInteger(0);
        
        // Create thread-safe delegate
        CallbackDelegate threadSafeDelegate = new CallbackDelegate() {
            @Override
            public void beginClass(String className, SourceType sourceType) {
                totalCallbacks.incrementAndGet();
            }
        };
        
        String source = "class TestClass { }";
        
        // Create threads
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            threads[i] = new Thread(() -> {
                try {
                    startLatch.await(); // Wait for signal to start
                    
                    StringReader reader = new StringReader(source);
                    SourceParser sourceParser = new SourceParser(reader, mockEntityResolver, 
                                                                 "TestClass" + threadNum + ".kt");
                    sourceParser.setDelegate(threadSafeDelegate);
                    
                    KotlinParserAdapter adapter = new KotlinParserAdapter(sourceParser);
                    adapter.parseTypeDef();
                    
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            });
            threads[i].start();
        }
        
        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for completion
        assertTrue("All threads should complete", endLatch.await(10, TimeUnit.SECONDS));
        
        // Verify callbacks were triggered by all threads
        assertEquals("Should have callbacks from all threads", 
                    threadCount, totalCallbacks.get());
    }
    
    // ==================== Error Handling Tests ====================
    
    @Test
    public void testErrorCallbackPropagation() {
        String invalidSource = "class { invalid syntax }";
        
        final AtomicBoolean errorCallbackReceived = new AtomicBoolean(false);
        final AtomicInteger errorLine = new AtomicInteger(-1);
        
        CallbackDelegate errorDelegate = new CallbackDelegate() {
            @Override
            public void parseError(String message, int line, int column) {
                errorCallbackReceived.set(true);
                errorLine.set(line);
            }
        };
        
        StringReader reader = new StringReader(invalidSource);
        SourceParser sourceParser = new SourceParser(reader, mockEntityResolver, "Invalid.kt");
        sourceParser.setDelegate(errorDelegate);
        
        KotlinParserAdapter adapter = new KotlinParserAdapter(sourceParser);
        
        // Try to parse invalid source
        adapter.parseTypeDef();
        
        // Error callback should have been triggered
        assertTrue("Error callback should be triggered for invalid syntax", 
                  errorCallbackReceived.get());
        assertTrue("Error line should be set", errorLine.get() >= 0);
    }
    
    // ==================== Real Kotlin Features Tests ====================
    
    @Test
    public void testKotlinSpecificFeatures() {
        // Test data classes
        String dataClassSource = 
            "data class Person(val name: String, val age: Int)";
        
        testKotlinFeature(dataClassSource, "data class");
        
        // Test sealed classes
        String sealedClassSource = 
            "sealed class Result {\n" +
            "    data class Success(val data: String) : Result()\n" +
            "    data class Error(val message: String) : Result()\n" +
            "}";
        
        testKotlinFeature(sealedClassSource, "sealed class");
        
        // Test inline functions
        String inlineFunctionSource = 
            "class Utils {\n" +
            "    inline fun <reified T> process(value: Any): T? {\n" +
            "        return value as? T\n" +
            "    }\n" +
            "}";
        
        testKotlinFeature(inlineFunctionSource, "inline function");
        
        // Test extension functions
        String extensionFunctionSource = 
            "fun String.reverse(): String {\n" +
            "    return this.reversed()\n" +
            "}";
        
        testKotlinFeature(extensionFunctionSource, "extension function");
    }
    
    private void testKotlinFeature(String source, String featureName) {
        StringReader reader = new StringReader(source);
        SourceParser sourceParser = new SourceParser(reader, mockEntityResolver, "Test.kt");
        sourceParser.setDelegate(testDelegate);
        
        KotlinParserAdapter adapter = new KotlinParserAdapter(sourceParser);
        
        // Reset counters
        classCallbackCount.set(0);
        methodCallbackCount.set(0);
        errorOccurred.set(false);
        
        // Parse the feature
        ParsedCUNode result = adapter.parseCU();
        
        // Basic validation - should parse without errors
        assertNotNull("Should parse " + featureName, result);
        assertFalse("Should not have errors parsing " + featureName, errorOccurred.get());
    }
}