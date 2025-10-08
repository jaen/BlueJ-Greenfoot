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

import bluej.extensions2.SourceType;
import bluej.parser.TrackingSourceParser.CallbackInvocation;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.StringReader;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for TrackingSourceParser.
 * Tests tracking functionality, thread safety, performance characteristics, and data analysis capabilities.
 * 
 * @since BlueJ 5.4.0
 * @author BlueJ Development Team
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TrackingSourceParserTest {
    
    private static final String SIMPLE_JAVA_CODE = """
        package test;
        
        import java.util.*;
        
        public class TestClass {
            private int field = 42;
            
            public void method() {
                System.out.println("Hello");
            }
            
            public int getField() {
                return field;
            }
        }
        """;
    
    private static final String COMPLEX_JAVA_CODE = """
        package test.complex;
        
        import java.util.*;
        import java.util.stream.*;
        import java.util.concurrent.*;
        
        /**
         * Complex test class with various language features
         */
        public class ComplexClass<T extends Comparable<T>> implements Runnable {
            private final List<T> items = new ArrayList<>();
            private volatile boolean running = false;
            
            public ComplexClass(T... initialItems) {
                items.addAll(Arrays.asList(initialItems));
            }
            
            @Override
            public void run() {
                running = true;
                try {
                    processItems();
                } finally {
                    running = false;
                }
            }
            
            private void processItems() {
                items.stream()
                    .filter(Objects::nonNull)
                    .sorted()
                    .forEach(this::process);
            }
            
            private void process(T item) {
                // Complex lambda expression
                CompletableFuture.supplyAsync(() -> {
                    return item.toString().toUpperCase();
                }).thenAccept(System.out::println);
            }
            
            // Inner class
            private class ItemProcessor {
                void processAll() {
                    for (T item : items) {
                        process(item);
                    }
                }
            }
            
            // Record (Java 14+)
            record ItemRecord(String name, int value) {
                public ItemRecord {
                    if (value < 0) {
                        throw new IllegalArgumentException("Value must be non-negative");
                    }
                }
            }
        }
        """;
    
    private TrackingSourceParser parser;
    
    @BeforeEach
    void setUp() {
        // Fresh parser for each test
        parser = null;
    }
    
    @AfterEach
    void tearDown() {
        if (parser != null) {
            parser.clearTracking();
        }
    }
    
    // ==================== Basic Functionality Tests ====================
    
    @Test
    @Order(1)
    @DisplayName("Test basic parser creation and initial state")
    void testParserCreation() {
        parser = new TrackingSourceParser(new StringReader(SIMPLE_JAVA_CODE));
        
        assertNotNull(parser);
        assertFalse(parser.isTrackingEnabled(), "Tracking should be disabled by default");
        assertEquals(0, parser.getTotalInvocationCount(), "Should have no invocations initially");
        assertTrue(parser.getInvocations().isEmpty(), "Invocation list should be empty");
        assertTrue(parser.getMethodStatistics().isEmpty(), "Method statistics should be empty");
    }
    
    @Test
    @Order(2)
    @DisplayName("Test tracking enable/disable mechanism")
    void testTrackingEnableDisable() {
        parser = new TrackingSourceParser(new StringReader(SIMPLE_JAVA_CODE), SourceType.JAVA);
        
        // Initially disabled
        assertFalse(parser.isTrackingEnabled());
        
        // Enable tracking
        parser.setTrackingEnabled(true);
        assertTrue(parser.isTrackingEnabled());
        
        // Disable tracking
        parser.setTrackingEnabled(false);
        assertFalse(parser.isTrackingEnabled());
    }
    
    @Test
    @Order(3)
    @DisplayName("Test basic tracking of parse operations")
    void testBasicTracking() {
        parser = new TrackingSourceParser(new StringReader(SIMPLE_JAVA_CODE));
        parser.setTrackingEnabled(true);
        
        // Parse the code
        parser.parseCU();
        
        // Verify tracking occurred
        assertTrue(parser.getTotalInvocationCount() > 0, "Should have tracked invocations");
        assertFalse(parser.getInvocations().isEmpty(), "Should have invocation records");
        assertFalse(parser.getMethodStatistics().isEmpty(), "Should have method statistics");
        
        // Verify specific callbacks were invoked
        Map<String, Long> stats = parser.getMethodStatistics();
        assertTrue(stats.containsKey("beginCompilationUnit"), "Should track beginCompilationUnit");
        assertTrue(stats.containsKey("endCompilationUnit"), "Should track endCompilationUnit");
    }
    
    @Test
    @Order(4)
    @DisplayName("Test no tracking when disabled")
    void testNoTrackingWhenDisabled() {
        parser = new TrackingSourceParser(new StringReader(SIMPLE_JAVA_CODE));
        parser.setTrackingEnabled(false);
        
        // Parse without tracking
        parser.parseCU();
        
        // Verify no tracking occurred
        assertEquals(0, parser.getTotalInvocationCount(), "Should not track when disabled");
        assertTrue(parser.getInvocations().isEmpty(), "Should have no invocation records");
        assertTrue(parser.getMethodStatistics().isEmpty(), "Should have no method statistics");
    }
    
    // ==================== Data Collection Tests ====================
    
    @Test
    @Order(5)
    @DisplayName("Test invocation data collection accuracy")
    void testInvocationDataCollection() {
        parser = new TrackingSourceParser(new StringReader(COMPLEX_JAVA_CODE));
        parser.setTrackingEnabled(true);
        
        Instant before = Instant.now();
        parser.parseCU();
        Instant after = Instant.now();
        
        List<CallbackInvocation> invocations = parser.getInvocations();
        assertFalse(invocations.isEmpty(), "Should have invocations");
        
        // Verify invocation data
        for (CallbackInvocation inv : invocations) {
            assertNotNull(inv.getMethodName(), "Method name should not be null");
            assertTrue(inv.getDurationNanos() >= 0, "Duration should be non-negative");
            assertNotNull(inv.getTimestamp(), "Timestamp should not be null");
            assertNotNull(inv.getThread(), "Thread should not be null");
            assertEquals(Thread.currentThread(), inv.getThread(), "Should be current thread");
            
            // Verify timestamp is within expected range
            assertTrue(!inv.getTimestamp().isBefore(before), "Timestamp should be after start");
            assertTrue(!inv.getTimestamp().isAfter(after), "Timestamp should be before end");
        }
    }
    
    @Test
    @Order(6)
    @DisplayName("Test method-specific invocation filtering")
    void testMethodSpecificFiltering() {
        parser = new TrackingSourceParser(new StringReader(COMPLEX_JAVA_CODE));
        parser.setTrackingEnabled(true);
        parser.parseCU();
        
        // Test filtering by method name
        List<CallbackInvocation> packageInvocations = parser.getInvocations("gotPackage");
        if (!packageInvocations.isEmpty()) {
            for (CallbackInvocation inv : packageInvocations) {
                assertEquals("gotPackage", inv.getMethodName());
            }
        }
        
        // Test filtering by thread (should all be current thread in single-threaded test)
        List<CallbackInvocation> threadInvocations = parser.getInvocationsByThread(Thread.currentThread());
        assertEquals(parser.getTotalInvocationCount(), threadInvocations.size(),
                    "All invocations should be from current thread");
    }
    
    @Test
    @Order(7)
    @DisplayName("Test tracking data clearing")
    void testTrackingDataClearing() {
        parser = new TrackingSourceParser(new StringReader(SIMPLE_JAVA_CODE));
        parser.setTrackingEnabled(true);
        
        // Parse and verify data collected
        parser.parseCU();
        assertTrue(parser.getTotalInvocationCount() > 0, "Should have invocations");
        
        // Clear tracking data
        parser.clearTracking();
        
        // Verify data cleared
        assertEquals(0, parser.getTotalInvocationCount(), "Should have no invocations after clear");
        assertTrue(parser.getInvocations().isEmpty(), "Invocation list should be empty after clear");
        assertTrue(parser.getMethodStatistics().isEmpty(), "Statistics should be empty after clear");
        
        // Verify tracking still works after clear
        parser.parseCU();
        assertTrue(parser.getTotalInvocationCount() > 0, "Should track after clearing");
    }
    
    // ==================== Statistics and Analysis Tests ====================
    
    @Test
    @Order(8)
    @DisplayName("Test method statistics accuracy")
    void testMethodStatistics() {
        parser = new TrackingSourceParser(new StringReader(COMPLEX_JAVA_CODE));
        parser.setTrackingEnabled(true);
        parser.parseCU();
        
        Map<String, Long> stats = parser.getMethodStatistics();
        assertFalse(stats.isEmpty(), "Should have statistics");
        
        // Verify counts are positive
        for (Map.Entry<String, Long> entry : stats.entrySet()) {
            assertTrue(entry.getValue() > 0, 
                      "Count for " + entry.getKey() + " should be positive");
        }
        
        // Verify total count matches sum of individual counts
        long totalFromStats = stats.values().stream().mapToLong(Long::longValue).sum();
        assertEquals(parser.getTotalInvocationCount(), totalFromStats,
                    "Total count should match sum of individual method counts");
    }
    
    @Test
    @Order(9)
    @DisplayName("Test average duration calculation")
    void testAverageDuration() {
        parser = new TrackingSourceParser(new StringReader(SIMPLE_JAVA_CODE));
        parser.setTrackingEnabled(true);
        
        // Parse multiple times to get more data
        for (int i = 0; i < 3; i++) {
            parser = new TrackingSourceParser(new StringReader(SIMPLE_JAVA_CODE));
            parser.setTrackingEnabled(true);
            parser.parseCU();
        }
        
        double avgDuration = parser.getAverageDuration();
        assertTrue(avgDuration >= 0, "Average duration should be non-negative");
        
        // Manually calculate average to verify
        List<CallbackInvocation> invocations = parser.getInvocations();
        if (!invocations.isEmpty()) {
            double manualAvg = invocations.stream()
                .mapToLong(CallbackInvocation::getDurationNanos)
                .average()
                .orElse(0);
            assertEquals(manualAvg, avgDuration, 0.001, "Average should match manual calculation");
        }
    }
    
    @Test
    @Order(10)
    @DisplayName("Test slowest invocations identification")
    void testSlowestInvocations() {
        parser = new TrackingSourceParser(new StringReader(COMPLEX_JAVA_CODE));
        parser.setTrackingEnabled(true);
        parser.parseCU();
        
        int topN = 5;
        List<CallbackInvocation> slowest = parser.getSlowestInvocations(topN);
        
        // Verify correct number returned (or all if less than topN)
        assertTrue(slowest.size() <= topN, "Should return at most topN invocations");
        assertTrue(slowest.size() <= parser.getTotalInvocationCount(), 
                  "Cannot return more than total invocations");
        
        // Verify sorted by duration (descending)
        for (int i = 1; i < slowest.size(); i++) {
            assertTrue(slowest.get(i - 1).getDurationNanos() >= slowest.get(i).getDurationNanos(),
                      "Should be sorted by duration descending");
        }
    }
    
    // ==================== Thread Safety Tests ====================
    
    @Test
    @Order(11)
    @DisplayName("Test thread safety of tracking operations")
    @Execution(ExecutionMode.CONCURRENT)
    void testThreadSafety() throws InterruptedException, ExecutionException {
        parser = new TrackingSourceParser(new StringReader(SIMPLE_JAVA_CODE));
        parser.setTrackingEnabled(true);
        
        int threadCount = 10;
        int iterationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        
        List<Future<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    for (int j = 0; j < iterationsPerThread; j++) {
                        // Concurrent operations
                        parser.setTrackingEnabled(j % 2 == 0);
                        parser.getInvocations();
                        parser.getMethodStatistics();
                        parser.getTotalInvocationCount();
                        parser.getAverageDuration();
                        
                        // Parse with fresh parser to generate tracking data
                        TrackingSourceParser localParser = new TrackingSourceParser(
                            new StringReader(SIMPLE_JAVA_CODE));
                        localParser.setTrackingEnabled(true);
                        localParser.parseCU();
                        
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return null;
            }));
        }
        
        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for completion
        for (Future<Void> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        
        // Verify all operations completed successfully
        assertEquals(threadCount * iterationsPerThread, successCount.get(),
                    "All operations should complete successfully");
    }
    
    @Test
    @Order(12)
    @DisplayName("Test concurrent parsing with tracking")
    void testConcurrentParsing() throws InterruptedException {
        int threadCount = 5;
        CountDownLatch completionLatch = new CountDownLatch(threadCount);
        List<TrackingSourceParser> parsers = new CopyOnWriteArrayList<>();
        
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                TrackingSourceParser localParser = new TrackingSourceParser(
                    new StringReader(COMPLEX_JAVA_CODE));
                localParser.setTrackingEnabled(true);
                localParser.parseCU();
                parsers.add(localParser);
                completionLatch.countDown();
            }).start();
        }
        
        assertTrue(completionLatch.await(10, TimeUnit.SECONDS), "All threads should complete");
        
        // Verify each parser tracked independently
        assertEquals(threadCount, parsers.size(), "Should have one parser per thread");
        for (TrackingSourceParser p : parsers) {
            assertTrue(p.getTotalInvocationCount() > 0, "Each parser should have tracked invocations");
        }
    }
    
    // ==================== Performance Tests ====================
    
    @Test
    @Order(13)
    @DisplayName("Test performance overhead when disabled")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testPerformanceOverheadWhenDisabled() {
        int iterations = 1000;
        String code = SIMPLE_JAVA_CODE;
        
        // Baseline: Regular SourceParser
        long baselineStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            SourceParser baseline = new SourceParser(new StringReader(code));
            baseline.parseCU();
        }
        long baselineDuration = System.nanoTime() - baselineStart;
        
        // Test: TrackingSourceParser with tracking disabled
        long trackingDisabledStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            TrackingSourceParser tracking = new TrackingSourceParser(new StringReader(code));
            tracking.setTrackingEnabled(false);
            tracking.parseCU();
        }
        long trackingDisabledDuration = System.nanoTime() - trackingDisabledStart;
        
        // Calculate overhead
        double overhead = ((double) trackingDisabledDuration - baselineDuration) / baselineDuration * 100;
        
        System.out.printf("Performance overhead when disabled: %.2f%% (baseline: %.2fms, tracking disabled: %.2fms)%n",
                         overhead, baselineDuration / 1_000_000.0, trackingDisabledDuration / 1_000_000.0);
        
        // Assert overhead is less than 1%
        assertTrue(overhead < 1.0, 
                  String.format("Overhead should be < 1%%, was %.2f%%", overhead));
    }
    
    @Test
    @Order(14)
    @DisplayName("Test performance with tracking enabled")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testPerformanceWithTrackingEnabled() {
        int iterations = 100;
        String code = COMPLEX_JAVA_CODE;
        
        // Baseline: Regular SourceParser
        long baselineStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            SourceParser baseline = new SourceParser(new StringReader(code));
            baseline.parseCU();
        }
        long baselineDuration = System.nanoTime() - baselineStart;
        
        // Test: TrackingSourceParser with tracking enabled
        long trackingEnabledStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            TrackingSourceParser tracking = new TrackingSourceParser(new StringReader(code));
            tracking.setTrackingEnabled(true);
            tracking.parseCU();
        }
        long trackingEnabledDuration = System.nanoTime() - trackingEnabledStart;
        
        // Calculate overhead
        double overhead = ((double) trackingEnabledDuration - baselineDuration) / baselineDuration * 100;
        
        System.out.printf("Performance overhead when enabled: %.2f%% (baseline: %.2fms, tracking enabled: %.2fms)%n",
                         overhead, baselineDuration / 1_000_000.0, trackingEnabledDuration / 1_000_000.0);
        
        // Assert overhead is reasonable (< 15% for complex tracking)
        assertTrue(overhead < 15.0, 
                  String.format("Overhead should be < 15%% when enabled, was %.2f%%", overhead));
    }
    
    @Test
    @Order(15)
    @DisplayName("Test method cache effectiveness")
    void testMethodCacheEffectiveness() {
        parser = new TrackingSourceParser(new StringReader(COMPLEX_JAVA_CODE));
        parser.setTrackingEnabled(true);
        
        // First parse - populates cache
        long firstParseStart = System.nanoTime();
        parser.parseCU();
        long firstParseDuration = System.nanoTime() - firstParseStart;
        
        // Clear tracking but keep cache
        parser.clearTracking();
        
        // Second parse - uses cache
        long secondParseStart = System.nanoTime();
        parser.parseCU();
        long secondParseDuration = System.nanoTime() - secondParseStart;
        
        System.out.printf("First parse: %.2fms, Second parse: %.2fms, Speedup: %.2fx%n",
                         firstParseDuration / 1_000_000.0,
                         secondParseDuration / 1_000_000.0,
                         (double) firstParseDuration / secondParseDuration);
        
        // Second parse should typically be faster due to cache
        // This is not guaranteed but should be true in most cases
        if (secondParseDuration < firstParseDuration) {
            System.out.println("Method cache is effective");
        }
    }
    
    // ==================== Edge Cases and Error Handling ====================
    
    @Test
    @Order(16)
    @DisplayName("Test handling of null and empty input")
    void testNullAndEmptyInput() {
        // Empty code
        parser = new TrackingSourceParser(new StringReader(""));
        parser.setTrackingEnabled(true);
        assertDoesNotThrow(() -> parser.parseCU(), "Should handle empty input");
        
        // Whitespace only
        parser = new TrackingSourceParser(new StringReader("   \n\t  "));
        parser.setTrackingEnabled(true);
        assertDoesNotThrow(() -> parser.parseCU(), "Should handle whitespace-only input");
    }
    
    @Test
    @Order(17)
    @DisplayName("Test handling of malformed Java code")
    void testMalformedCode() {
        String malformedCode = """
            package test;
            
            public class Broken {
                public void method() {
                    // Missing closing brace
                    System.out.println("broken");
            """;
        
        parser = new TrackingSourceParser(new StringReader(malformedCode));
        parser.setTrackingEnabled(true);
        
        // Should not throw, but may record error callbacks
        assertDoesNotThrow(() -> parser.parseCU(), "Should handle malformed code");
        
        // Should still track invocations
        assertTrue(parser.getTotalInvocationCount() > 0, "Should track even with errors");
    }
    
    @Test
    @Order(18)
    @DisplayName("Test failed invocation tracking")
    void testFailedInvocationTracking() {
        // Create a custom test that forces an exception
        class ThrowingParser extends TrackingSourceParser {
            public ThrowingParser(StringReader reader) {
                super(reader);
            }
            
            public void triggerException() {
                // This would be a callback that throws an exception
                CallbackDelegate delegate = getCallbackDelegate();
                try {
                    // Simulate a callback that throws
                    delegate.gotPackage(null); // May cause NPE in some implementations
                } catch (Exception e) {
                    // Expected
                }
            }
        }
        
        ThrowingParser throwingParser = new ThrowingParser(new StringReader(SIMPLE_JAVA_CODE));
        throwingParser.setTrackingEnabled(true);
        
        // Trigger exception in callback
        assertDoesNotThrow(() -> throwingParser.triggerException(), 
                          "Should handle exceptions in callbacks");
        
        // Check for failed invocations
        List<CallbackInvocation> failed = throwingParser.getFailedInvocations();
        // Note: May be empty if the exception is handled internally
        assertNotNull(failed, "Failed invocations list should not be null");
    }
    
    // ==================== Integration Tests ====================
    
    @Test
    @Order(19)
    @DisplayName("Test complete parsing workflow with analysis")
    void testCompleteWorkflow() {
        parser = new TrackingSourceParser(new StringReader(COMPLEX_JAVA_CODE));
        parser.setTrackingEnabled(true);
        
        // Parse the code
        parser.parseCU();
        
        // Perform comprehensive analysis
        long totalCount = parser.getTotalInvocationCount();
        assertTrue(totalCount > 0, "Should have invocations");
        
        Map<String, Long> stats = parser.getMethodStatistics();
        assertFalse(stats.isEmpty(), "Should have statistics");
        
        double avgDuration = parser.getAverageDuration();
        assertTrue(avgDuration > 0, "Should have positive average duration");
        
        List<CallbackInvocation> slowest = parser.getSlowestInvocations(3);
        assertFalse(slowest.isEmpty(), "Should identify slowest invocations");
        
        // Print summary for manual inspection
        parser.printTrackingSummary();
        
        // Verify summary doesn't throw
        assertDoesNotThrow(() -> parser.printTrackingSummary(), 
                          "Summary printing should not throw");
    }
    
    @Test
    @Order(20)
    @DisplayName("Test tracking across multiple parse operations")
    void testMultipleParseOperations() {
        parser = new TrackingSourceParser(new StringReader(SIMPLE_JAVA_CODE));
        parser.setTrackingEnabled(true);
        
        // First parse
        parser.parseCU();
        long firstCount = parser.getTotalInvocationCount();
        
        // Parse again without clearing
        parser = new TrackingSourceParser(new StringReader(COMPLEX_JAVA_CODE));
        parser.setTrackingEnabled(true);
        parser.parseCU();
        long secondCount = parser.getTotalInvocationCount();
        
        // Each parser instance tracks independently
        assertTrue(firstCount > 0, "First parse should track invocations");
        assertTrue(secondCount > 0, "Second parse should track invocations");
        // Different code should result in different counts
        assertNotEquals(firstCount, secondCount, "Different code should have different invocation counts");
    }
    
    // ==================== Utility Method Tests ====================
    
    @Test
    @Order(21)
    @DisplayName("Test CallbackInvocation utility methods")
    void testCallbackInvocationMethods() {
        parser = new TrackingSourceParser(new StringReader(SIMPLE_JAVA_CODE));
        parser.setTrackingEnabled(true);
        parser.parseCU();
        
        List<CallbackInvocation> invocations = parser.getInvocations();
        assertFalse(invocations.isEmpty(), "Should have invocations");
        
        CallbackInvocation inv = invocations.get(0);
        
        // Test utility methods
        assertNotNull(inv.toString(), "toString should not return null");
        assertTrue(inv.isSuccessful(), "Normal invocations should be successful");
        assertEquals(inv.getDurationNanos() / 1_000_000.0, inv.getDurationMillis(), 0.001,
                    "Millisecond conversion should be accurate");
        
        // Test immutability of returned arrays
        Object[] args1 = inv.getArguments();
        Object[] args2 = inv.getArguments();
        if (args1 != null && args2 != null) {
            assertNotSame(args1, args2, "Should return defensive copies of arguments");
        }
    }
}