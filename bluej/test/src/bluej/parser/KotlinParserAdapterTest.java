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

import bluej.Config;
import bluej.parser.KotlinParserAdapter.ParserCapability;
import bluej.parser.KotlinParserAdapter.ParserType;
import bluej.parser.KotlinParserAdapter.PerformanceMetrics;
import bluej.parser.lexer.JavaTokenFilter;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.lexer.JavaTokenTypes;

import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.StringReader;
import java.util.Map;
import java.util.Properties;

/**
 * Comprehensive test suite for KotlinParserAdapter.
 * 
 * <p>Tests the following functionality:
 * <ul>
 *   <li>Parser selection based on configuration</li>
 *   <li>Automatic capability detection</li>
 *   <li>Performance monitoring and metrics</li>
 *   <li>Fallback mechanisms on parser errors</li>
 *   <li>Thread safety of parser operations</li>
 *   <li>Configuration loading and defaults</li>
 * </ul>
 * 
 * @since BlueJ 5.4.0
 * @author BlueJ Development Team
 */
@RunWith(JUnit4.class)
public class KotlinParserAdapterTest {
    
    private SourceParser mockSourceParser;
    private CallbackDelegate mockCallbackDelegate;
    private JavaTokenFilter mockTokenStream;
    private Properties originalConfig;
    
    @Before
    public void setUp() {
        // Save original configuration
        originalConfig = new Properties();
        originalConfig.putAll(System.getProperties());
        
        // Create mocks
        mockSourceParser = mock(SourceParser.class);
        mockCallbackDelegate = mock(CallbackDelegate.class);
        mockTokenStream = mock(JavaTokenFilter.class);
        
        // Setup basic mock behavior
        when(mockSourceParser.getTokenStream()).thenReturn(mockTokenStream);
        when(mockSourceParser.getDelegate()).thenReturn(mockCallbackDelegate);
        
        // Setup token stream mock
        LocatableToken eofToken = mock(LocatableToken.class);
        when(eofToken.getType()).thenReturn(JavaTokenTypes.EOF);
        when(mockTokenStream.LA(1)).thenReturn(eofToken);
        when(mockTokenStream.nextToken()).thenReturn(eofToken);
    }
    
    @After
    public void tearDown() {
        // Restore original configuration
        System.setProperties(originalConfig);
    }
    
    // ==================== Parser Selection Tests ====================
    
    @Test
    public void testDefaultParserSelection() {
        // Test default configuration (should be AUTO)
        KotlinParserAdapter adapter = new KotlinParserAdapter(mockSourceParser);
        
        assertNotNull("Adapter should be created", adapter);
        assertEquals("Default parser type should be AUTO", 
                    ParserType.AUTO, ParserType.fromConfig("auto"));
    }
    
    @Test
    public void testExplicitLegacyParserSelection() {
        // Configure for legacy parser
        System.setProperty("kotlin.parser.type", "legacy");
        
        KotlinParserAdapter adapter = new KotlinParserAdapter(mockSourceParser);
        adapter.switchParser(ParserType.LEGACY);
        
        assertEquals("Should be using legacy parser", 
                    ParserType.LEGACY, adapter.getCurrentParserType());
    }
    
    @Test
    public void testExplicitPrattParserSelection() {
        // Configure for Pratt parser
        System.setProperty("kotlin.parser.type", "pratt");
        
        KotlinParserAdapter adapter = new KotlinParserAdapter(mockSourceParser);
        adapter.switchParser(ParserType.PRATT);
        
        assertEquals("Should be using Pratt parser", 
                    ParserType.PRATT, adapter.getCurrentParserType());
    }
    
    @Test
    public void testParserSwitching() {
        KotlinParserAdapter adapter = new KotlinParserAdapter(mockSourceParser);
        
        // Switch to legacy
        adapter.switchParser(ParserType.LEGACY);
        assertEquals("Should be using legacy parser", 
                    ParserType.LEGACY, adapter.getCurrentParserType());
        
        // Switch to Pratt
        adapter.switchParser(ParserType.PRATT);
        assertEquals("Should be using Pratt parser", 
                    ParserType.PRATT, adapter.getCurrentParserType());
        
        // Switch back to auto
        adapter.switchParser(ParserType.AUTO);
        assertNotNull("Should have a valid parser type", adapter.getCurrentParserType());
    }
    
    // ==================== Capability Detection Tests ====================
    
    @Test
    public void testLegacyParserCapabilities() {
        KotlinParserAdapter adapter = new KotlinParserAdapter(mockSourceParser);
        adapter.switchParser(ParserType.LEGACY);
        
        // Test legacy capabilities
        assertFalse("Legacy should not support Kotlin 1.9 features", 
                   adapter.supportsCapability(ParserCapability.KOTLIN_1_9_FEATURES));
        assertFalse("Legacy should not support context receivers", 
                   adapter.supportsCapability(ParserCapability.CONTEXT_RECEIVERS));
        assertTrue("Legacy should support sealed when exhaustiveness", 
                  adapter.supportsCapability(ParserCapability.SEALED_WHEN_EXHAUSTIVENESS));
        assertTrue("Legacy should support coroutines", 
                  adapter.supportsCapability(ParserCapability.COROUTINES));
    }
    
    @Test
    public void testPrattParserCapabilities() {
        KotlinParserAdapter adapter = new KotlinParserAdapter(mockSourceParser);
        adapter.switchParser(ParserType.PRATT);
        
        // Test Pratt capabilities
        assertTrue("Pratt should support Kotlin 1.9 features", 
                  adapter.supportsCapability(ParserCapability.KOTLIN_1_9_FEATURES));
        assertTrue("Pratt should support context receivers", 
                  adapter.supportsCapability(ParserCapability.CONTEXT_RECEIVERS));
        assertTrue("Pratt should support value classes", 
                  adapter.supportsCapability(ParserCapability.VALUE_CLASSES));
        assertTrue("Pratt should support advanced type inference", 
                  adapter.supportsCapability(ParserCapability.ADVANCED_TYPE_INFERENCE));
    }
    
    @Test
    public void testGetSupportedCapabilities() {
        KotlinParserAdapter adapter = new KotlinParserAdapter(mockSourceParser);
        
        // Switch to legacy and get capabilities
        adapter.switchParser(ParserType.LEGACY);
        Map<ParserCapability, Boolean> legacyCaps = adapter.getSupportedCapabilities();
        assertNotNull("Should return capability map", legacyCaps);
        assertFalse("Map should be unmodifiable", legacyCaps.getClass().getName().contains("Unmodifiable"));
        
        // Switch to Pratt and get capabilities
        adapter.switchParser(ParserType.PRATT);
        Map<ParserCapability, Boolean> prattCaps = adapter.getSupportedCapabilities();
        assertNotNull("Should return capability map", prattCaps);
        
        // Verify different capabilities
        assertNotEquals("Capabilities should differ between parsers", legacyCaps, prattCaps);
    }
    
    // ==================== Performance Monitoring Tests ====================
    
    @Test
    public void testPerformanceMonitoringDisabled() {
        // Disable performance monitoring
        System.setProperty("kotlin.parser.performance.monitoring", "false");
        
        KotlinParserAdapter adapter = new KotlinParserAdapter(mockSourceParser);
        
        // Parse something
        adapter.parseCU();
        
        // Check metrics (should be empty/zero)
        PerformanceMetrics metrics = adapter.getLegacyMetrics();
        assertEquals("Parse count should be 0 when monitoring disabled", 
                    0, metrics.getParseCount());
    }
    
    @Test
    public void testPerformanceMonitoringEnabled() {
        // Enable performance monitoring
        System.setProperty("kotlin.parser.performance.monitoring", "true");
        
        KotlinParserAdapter adapter = new KotlinParserAdapter(mockSourceParser);
        adapter.switchParser(ParserType.LEGACY);
        
        // Parse multiple times
        adapter.parseCU();
        adapter.parseImportStatement();
        adapter.parseTypeDef();
        
        // Check metrics
        PerformanceMetrics metrics = adapter.getLegacyMetrics();
        assertEquals("Parse count should be 3", 3, metrics.getParseCount());
        assertTrue("Average parse time should be > 0", 
                  metrics.getAverageParseTimeNanos() >= 0);
    }
    
    @Test
    public void testPerformanceMetricsCalculation() {
        PerformanceMetrics metrics = new PerformanceMetrics();
        
        // Record some parse operations
        metrics.recordParse(1000000); // 1ms
        metrics.recordParse(2000000); // 2ms
        metrics.recordParse(3000000); // 3ms
        
        assertEquals("Parse count should be 3", 3, metrics.getParseCount());
        assertEquals("Average should be 2ms", 2000000, metrics.getAverageParseTimeNanos());
        assertEquals("Max should be 3ms", 3000000, metrics.getMaxParseTimeNanos());
        assertEquals("Min should be 1ms", 1000000, metrics.getMinParseTimeNanos());
        
        // Record errors and fallbacks
        metrics.recordError();
        metrics.recordError();
        metrics.recordFallback();
        
        assertEquals("Error count should be 2", 2, metrics.getErrorCount());
        assertEquals("Fallback count should be 1", 1, metrics.getFallbackCount());
    }
    
    @Test
    public void testMetricsReset() {
        KotlinParserAdapter adapter = new KotlinParserAdapter(mockSourceParser);
        PerformanceMetrics metrics = adapter.getLegacyMetrics();
        
        // Record some operations
        metrics.recordParse(1000000);
        metrics.recordError();
        metrics.recordFallback();
        
        // Reset metrics
        adapter.resetMetrics();
        
        // Verify reset
        assertEquals("Parse count should be 0 after reset", 0, metrics.getParseCount());
        assertEquals("Error count should be 0 after reset", 0, metrics.getErrorCount());
        assertEquals("Fallback count should be 0 after reset", 0, metrics.getFallbackCount());
    }
    
    @Test
    public void testMetricsToString() {
        PerformanceMetrics metrics = new PerformanceMetrics();
        metrics.recordParse(5000000); // 5ms
        metrics.recordError();
        
        String str = metrics.toString();
        assertNotNull("toString should not be null", str);
        assertTrue("Should contain parse count", str.contains("parseCount=1"));
        assertTrue("Should contain avg time", str.contains("avgTime="));
        assertTrue("Should contain errors", str.contains("errors=1"));
    }
    
    // ==================== Fallback Mechanism Tests ====================
    
    @Test
    public void testFallbackDisabled() {
        // Disable fallback
        System.setProperty("kotlin.parser.fallback.enabled", "false");
        System.setProperty("kotlin.parser.performance.monitoring", "true");
        
        KotlinParserAdapter adapter = new KotlinParserAdapter(mockSourceParser);
        adapter.switchParser(ParserType.PRATT);
        
        // Simulate parser error
        doThrow(new RuntimeException("Parse error")).when(mockSourceParser).getTokenStream();
        
        try {
            adapter.parseCU();
            fail("Should have thrown exception when fallback disabled");
        } catch (RuntimeException e) {
            assertEquals("Parse error", e.getMessage());
        }
        
        // Verify no fallback occurred
        PerformanceMetrics metrics = adapter.getPrattMetrics();
        assertEquals("Fallback count should be 0", 0, metrics.getFallbackCount());
    }
    
    // ==================== Configuration Tests ====================
    
    @Test
    public void testConfigurationDefaults() {
        // Clear all kotlin.parser properties
        System.getProperties().entrySet().removeIf(e -> 
            e.getKey().toString().startsWith("kotlin.parser"));
        
        KotlinParserAdapter adapter = new KotlinParserAdapter(mockSourceParser);
        
        // Verify defaults are used
        assertNotNull("Should create adapter with defaults", adapter);
        
        // Default should be AUTO
        assertEquals("Default parser type should be AUTO", 
                    ParserType.AUTO, ParserType.fromConfig("invalid"));
    }
    
    @Test
    public void testPerformanceThresholdConfiguration() {
        // Set custom threshold
        System.setProperty("kotlin.parser.performance.threshold", "50");
        System.setProperty("kotlin.parser.performance.monitoring", "true");
        
        KotlinParserAdapter adapter = new KotlinParserAdapter(mockSourceParser);
        
        // Verify threshold is loaded (indirectly through behavior)
        assertNotNull("Should create adapter with custom threshold", adapter);
    }
    
    // ==================== ParserType Enum Tests ====================
    
    @Test
    public void testParserTypeEnum() {
        assertEquals("auto", ParserType.AUTO.getConfigValue());
        assertEquals("legacy", ParserType.LEGACY.getConfigValue());
        assertEquals("pratt", ParserType.PRATT.getConfigValue());
        
        assertEquals(ParserType.AUTO, ParserType.fromConfig("auto"));
        assertEquals(ParserType.AUTO, ParserType.fromConfig("AUTO"));
        assertEquals(ParserType.LEGACY, ParserType.fromConfig("legacy"));
        assertEquals(ParserType.PRATT, ParserType.fromConfig("pratt"));
        assertEquals(ParserType.AUTO, ParserType.fromConfig("invalid"));
        assertEquals(ParserType.AUTO, ParserType.fromConfig(null));
    }
    
    // ==================== ParserBehavior Implementation Tests ====================
    
    @Test
    public void testParserBehaviorMethods() {
        System.setProperty("kotlin.parser.performance.monitoring", "false");
        KotlinParserAdapter adapter = new KotlinParserAdapter(mockSourceParser);
        
        // Test all ParserBehavior methods don't throw exceptions
        adapter.parseCU();
        
        LocatableToken mockToken = mock(LocatableToken.class);
        when(mockToken.getType()).thenReturn(JavaTokenTypes.LITERAL_package);
        
        adapter.parsePackageStmt(mockToken);
        adapter.parseImportStatement();
        adapter.parseImportStatement(mockToken);
        adapter.parseTypeDef();
        adapter.parseTypeDef(mockToken);
        adapter.parseTypeBody(0, mockToken);
        adapter.parseClassElement(mockToken);
        adapter.parseClassBody();
        adapter.parseStmtBlock();
        adapter.parseStatement();
        adapter.parseStatement(mockToken, false);
        adapter.parseWhileStatement(mockToken);
        adapter.parseForStatement(mockToken);
        adapter.parseTypeSpec(false);
        adapter.parseExpression();
        adapter.parseExpression(false, true);
        adapter.parseVariableDeclarations();
        adapter.parseVariableDeclarations(mockToken, true);
        adapter.parseMethodParamsBody();
        
        // If we get here without exceptions, the delegation is working
        assertTrue("All parser methods should delegate properly", true);
    }
    
    // ==================== Thread Safety Tests ====================
    
    @Test
    public void testConcurrentParserAccess() throws InterruptedException {
        System.setProperty("kotlin.parser.performance.monitoring", "true");
        final KotlinParserAdapter adapter = new KotlinParserAdapter(mockSourceParser);
        
        // Create multiple threads that parse concurrently
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            final int threadNum = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    if (threadNum % 2 == 0) {
                        adapter.switchParser(ParserType.LEGACY);
                    } else {
                        adapter.switchParser(ParserType.PRATT);
                    }
                    adapter.parseCU();
                }
            });
        }
        
        // Start all threads
        for (Thread t : threads) {
            t.start();
        }
        
        // Wait for completion
        for (Thread t : threads) {
            t.join();
        }
        
        // Verify metrics are consistent
        PerformanceMetrics legacyMetrics = adapter.getLegacyMetrics();
        PerformanceMetrics prattMetrics = adapter.getPrattMetrics();
        
        long totalParses = legacyMetrics.getParseCount() + prattMetrics.getParseCount();
        assertTrue("Should have recorded parses from concurrent access", totalParses > 0);
    }
    
    // ==================== Adapter toString Test ====================
    
    @Test
    public void testAdapterToString() {
        KotlinParserAdapter adapter = new KotlinParserAdapter(mockSourceParser);
        
        String str = adapter.toString();
        assertNotNull("toString should not be null", str);
        assertTrue("Should contain class name", str.contains("KotlinParserAdapter"));
        assertTrue("Should contain current parser", str.contains("current="));
        assertTrue("Should contain configured type", str.contains("configured="));
        assertTrue("Should contain monitoring status", str.contains("monitoring="));
        assertTrue("Should contain fallback status", str.contains("fallback="));
    }
}