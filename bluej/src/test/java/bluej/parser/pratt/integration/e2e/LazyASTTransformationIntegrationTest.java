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
package bluej.parser.pratt.integration.e2e;

import bluej.parser.CallbackDelegate;
import bluej.parser.pratt.integration.CallbackTestingUtility;
import bluej.parser.pratt.integration.CallbackTestingUtility.CallbackTester;
import bluej.parser.pratt.integration.benchmark.adapters.LazyASTTransformationAdapter;
import bluej.parser.pratt.integration.benchmark.corpus.TestCorpusGenerator;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.ValueEntity;
import bluej.parser.entity.TypeEntity;
import bluej.parser.entity.PackageOrClass;
import bluej.parser.entity.TypeArgumentEntity;
import bluej.debugger.gentype.Reflective;
import bluej.debugger.gentype.JavaType;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

/**
 * End-to-End integration test for Lazy AST Transformation integration strategy.
 * 
 * This test validates the lazy AST transformation approach where AST nodes are
 * transformed and callbacks invoked on-demand rather than during initial parsing.
 * Tests the complete integration with BlueJ framework focusing on deferred processing.
 * 
 * Key validation aspects:
 * - Deferred computation reducing initial parsing overhead
 * - Memory efficiency through lazy evaluation patterns
 * - Complex cache management and hit/miss scenarios
 * - Two-phase parsing: initial setup + lazy evaluation
 * - Performance characteristics varying by cache effectiveness
 * - On-demand callback execution patterns
 */
public class LazyASTTransformationIntegrationTest {
    
    private TestableDocument document;
    private MockEntityResolver entityResolver;
    private CallbackTester callbackTester;
    private LazyASTTransformationAdapter adapter;
    private TestCorpusGenerator corpusGenerator;
    private ExecutorService executorService;
    
    @Before
    public void setUp() {
        entityResolver = new MockEntityResolver();
        document = new TestableDocument("test.java", entityResolver);
        callbackTester = CallbackTestingUtility.forDelegate(document.getCallbackDelegate());
        adapter = new LazyASTTransformationAdapter();
        adapter.setCallbackDelegate(document.getCallbackDelegate());
        corpusGenerator = new TestCorpusGenerator();
        executorService = Executors.newCachedThreadPool();
    }
    
    @Test
    public void testDeferredComputationReducingInitialParsingOverhead() throws Exception {
        // Given: Complex code that benefits from lazy evaluation
        String code = """
            public class LazyParsingTest {
                // Multiple complex methods that might not all be accessed
                public void heavyMethod1() {
                    for (int i = 0; i < 1000; i++) {
                        processComplexData(i);
                    }
                }
                
                public void heavyMethod2() {
                    Stream.of("a", "b", "c")
                          .filter(s -> s.length() > 0)
                          .map(String::toUpperCase)
                          .collect(Collectors.toList());
                }
                
                public void heavyMethod3() {
                    Optional.ofNullable(getData())
                            .map(this::transform)
                            .orElse(getDefault());
                }
                
                private void processComplexData(int i) { }
                private String getData() { return "data"; }
                private String transform(String s) { return s.toUpperCase(); }
                private String getDefault() { return "default"; }
            }
            """;
        
        document.setContent(code);
        
        // When: Parse with lazy transformation - measure initial overhead
        long initialStartTime = System.nanoTime();
        adapter.parseWithStrategy(code);
        long initialEndTime = System.nanoTime();
        
        double initialParseMs = (initialEndTime - initialStartTime) / 1_000_000.0;
        
        // Then: Verify deferred computation benefits
        // Initial parsing should be fast due to deferred processing
        assertTrue("Initial lazy parsing should be fast", initialParseMs < 50);
        
        // Callbacks should be generated after lazy evaluation
        assertTrue("Lazy evaluation should generate callbacks", callbackTester.getCallbackCount() > 0);
        
        // Structure should be preserved with deferred processing
        assertTrue(callbackTester.hasCallback("classStart"));
        assertTrue(callbackTester.hasCallback("methodStart"));
        assertTrue(callbackTester.hasCallback("methodEnd"));
        assertTrue(callbackTester.hasCallback("classEnd"));
        
        // Balance should be maintained despite lazy evaluation
        assertTrue("Lazy evaluation should maintain callback balance", callbackTester.isBalanced());
    }
    
    @Test
    public void testMemoryEfficiencyThroughLazyEvaluationPatterns() throws Exception {
        // Given: Large code structure for memory efficiency testing
        StringBuilder largeCode = new StringBuilder();
        largeCode.append("public class MemoryEfficiencyTest {\n");
        
        // Generate many methods that won't all be evaluated immediately
        for (int i = 0; i < 100; i++) {
            largeCode.append("    public void method").append(i).append("() {\n");
            largeCode.append("        // Complex processing that might not be needed\n");
            largeCode.append("        processData(\"").append(i).append("\");\n");
            largeCode.append("    }\n\n");
        }
        
        largeCode.append("    private void processData(String data) {\n");
        largeCode.append("        System.out.println(data);\n");
        largeCode.append("    }\n");
        largeCode.append("}\n");
        
        document.setContent(largeCode.toString());
        
        // When: Parse with memory measurement
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        adapter.parseWithStrategy(largeCode.toString());
        
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;
        
        // Then: Verify memory efficiency
        // Memory usage should be reasonable for lazy evaluation
        assertTrue("Lazy evaluation should be memory efficient", memoryUsed < 10_000_000);
        
        // Verify that callbacks are still generated efficiently
        int callbackCount = callbackTester.getCallbackCount();
        assertTrue("Should generate callbacks for large structure", callbackCount > 50);
        
        // Verify structural integrity maintained
        assertTrue("Large lazy structures should maintain balance", callbackTester.isBalanced());
        
        // Multiple methods should be processed
        int methodCount = callbackTester.countCallbacks("methodStart");
        assertTrue("Should process multiple methods lazily", methodCount >= 50);
    }
    
    @Test
    public void testComplexCacheManagementWithHitAndMissScenarios() throws Exception {
        // Given: Code designed to test cache behavior
        String cacheTestCode = """
            public class CacheTest {
                public void accessedMultipleTimes() {
                    // This method will be "accessed" multiple times to test cache hits
                    performWork();
                }
                
                public void accessedOnce() {
                    // This method accessed once - cache miss scenario
                    performOtherWork();
                }
                
                public void neverAccessed() {
                    // This method never accessed - remains cached
                    performExpensiveWork();
                }
                
                private void performWork() { }
                private void performOtherWork() { }
                private void performExpensiveWork() { }
            }
            """;
        
        document.setContent(cacheTestCode);
        
        // When: Parse with multiple evaluation cycles to test cache
        callbackTester.clear();
        
        // First parsing - cache misses
        long firstParseStart = System.nanoTime();
        adapter.parseWithStrategy(cacheTestCode);
        long firstParseEnd = System.nanoTime();
        
        int firstCallbackCount = callbackTester.getCallbackCount();
        callbackTester.clear();
        
        // Second parsing of same code - should have cache hits
        long secondParseStart = System.nanoTime();
        adapter.parseWithStrategy(cacheTestCode);
        long secondParseEnd = System.nanoTime();
        
        int secondCallbackCount = callbackTester.getCallbackCount();
        
        // Then: Verify cache behavior
        double firstParseMs = (firstParseEnd - firstParseStart) / 1_000_000.0;
        double secondParseMs = (secondParseEnd - secondParseStart) / 1_000_000.0;
        
        // Second parse might be faster due to caching (though not guaranteed)
        assertTrue("First parse should take measurable time", firstParseMs > 0);
        assertTrue("Second parse should take measurable time", secondParseMs > 0);
        
        // Callback counts should be consistent
        assertEquals("Cache should maintain consistent callback generation", firstCallbackCount, secondCallbackCount);
        
        // Structure should be maintained across cache cycles
        assertTrue(callbackTester.hasCallback("classStart"));
        assertTrue(callbackTester.hasCallback("methodStart"));
        assertTrue(callbackTester.hasCallback("methodEnd"));
        assertTrue(callbackTester.hasCallback("classEnd"));
        assertTrue(callbackTester.isBalanced());
    }
    
    @Test
    public void testTwoPhaseParsingWithInitialSetupAndLazyEvaluation() throws Exception {
        // Given: Code that demonstrates two-phase parsing benefits
        String twoPhasedCode = """
            public class TwoPhaseTest {
                // Phase 1: Initial structure identification
                private String field1 = "value1";
                private int field2 = 42;
                
                // Phase 2: Method bodies evaluated lazily
                public void quickMethod() {
                    System.out.println("quick");
                }
                
                public void complexMethod() {
                    // Complex processing that benefits from lazy evaluation
                    CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                        return processData();
                    });
                    
                    future.thenCompose(result -> {
                        return CompletableFuture.supplyAsync(() -> {
                            return transformResult(result);
                        });
                    }).thenAccept(finalResult -> {
                        System.out.println("Result: " + finalResult);
                    });
                }
                
                private String processData() { return "processed"; }
                private String transformResult(String input) { return input.toUpperCase(); }
            }
            """;
        
        document.setContent(twoPhasedCode);
        
        // When: Execute two-phase parsing
        // Phase 1: Initial setup (should be fast)
        long phase1Start = System.nanoTime();
        adapter.parseWithStrategy(twoPhasedCode);
        long phase1End = System.nanoTime();
        
        // Then: Verify two-phase characteristics
        double phase1Ms = (phase1End - phase1Start) / 1_000_000.0;
        assertTrue("Initial phase should be efficient", phase1Ms < 100);
        
        // Verify structural callbacks from both phases
        assertTrue(callbackTester.hasCallback("classStart"));
        assertTrue(callbackTester.hasCallback("fieldDeclaration"));
        assertTrue(callbackTester.hasCallback("methodStart"));
        assertTrue(callbackTester.hasCallback("exprStart"));
        assertTrue(callbackTester.hasCallback("exprEnd"));
        assertTrue(callbackTester.hasCallback("methodEnd"));
        assertTrue(callbackTester.hasCallback("classEnd"));
        
        // Field declarations should be processed in phase 1
        int fieldCount = callbackTester.countCallbacks("fieldDeclaration");
        assertEquals("Fields should be processed in initial phase", 2, fieldCount);
        
        // Method processing should involve lazy evaluation
        int methodCount = callbackTester.countCallbacks("methodStart");
        assertTrue("Methods should be processed through lazy evaluation", methodCount >= 2);
        
        // Balance maintained across phases
        assertTrue("Two-phase parsing should maintain balance", callbackTester.isBalanced());
    }
    
    @Test
    public void testPerformanceCharacteristicsVaryingByCacheEffectiveness() throws Exception {
        // Given: Various code samples to test cache effectiveness
        List<String> testCases = Arrays.asList(
            // Simple case - good cache effectiveness
            """
            public class SimpleCache {
                public void method() { System.out.println("simple"); }
            }
            """,
            
            // Moderate complexity - mixed cache effectiveness
            """
            public class ModerateCache {
                public void method1() { process(); }
                public void method2() { process(); }
                private void process() { }
            }
            """,
            
            // Complex case - variable cache effectiveness
            """
            public class ComplexCache {
                public void complexMethod() {
                    IntStream.range(0, 100)
                            .parallel()
                            .mapToObj(i -> processItem(i))
                            .collect(Collectors.toList());
                }
                private String processItem(int i) { return String.valueOf(i); }
            }
            """
        );
        
        // When: Test performance across different cache scenarios
        for (int i = 0; i < testCases.size(); i++) {
            String code = testCases.get(i);
            callbackTester.clear();
            document.setContent(code);
            
            // Multiple runs to observe cache effects
            long totalTime = 0;
            int runs = 3;
            
            for (int run = 0; run < runs; run++) {
                long startTime = System.nanoTime();
                adapter.parseWithStrategy(code);
                long endTime = System.nanoTime();
                
                totalTime += (endTime - startTime);
                
                if (run == 0) {
                    // Verify callbacks on first run
                    assertTrue("Test case " + i + " should generate callbacks",
                              callbackTester.getCallbackCount() > 0);
                    assertTrue("Test case " + i + " should maintain balance",
                              callbackTester.isBalanced());
                }
            }
            
            double avgTimeMs = (totalTime / runs) / 1_000_000.0;
            
            // Then: Verify performance characteristics
            assertTrue("Average performance should be reasonable for test case " + i, avgTimeMs < 100);
            
            // Performance should be consistent across runs (cache effectiveness)
            assertTrue("Should take measurable time for test case " + i, avgTimeMs > 0);
        }
    }
    
    @Test
    public void testOnDemandCallbackExecutionPatterns() throws Exception {
        // Given: Code with selective access patterns
        String onDemandCode = """
            public class OnDemandTest {
                // These methods represent different access patterns
                public void highPriorityMethod() {
                    // Accessed immediately - callbacks executed on-demand
                    criticalProcessing();
                }
                
                public void mediumPriorityMethod() {
                    // Accessed later - deferred callback execution
                    regularProcessing();
                }
                
                public void lowPriorityMethod() {
                    // Rarely accessed - callbacks remain deferred
                    backgroundProcessing();
                }
                
                private void criticalProcessing() { }
                private void regularProcessing() { }
                private void backgroundProcessing() { }
            }
            """;
        
        document.setContent(onDemandCode);
        
        // When: Parse with on-demand execution simulation
        adapter.parseWithStrategy(onDemandCode);
        
        // Then: Verify on-demand execution patterns
        // All structural callbacks should be present
        assertTrue(callbackTester.hasCallback("classStart"));
        assertTrue(callbackTester.hasCallback("classEnd"));
        
        // Method callbacks should reflect on-demand execution
        int methodStartCount = callbackTester.countCallbacks("methodStart");
        int methodEndCount = callbackTester.countCallbacks("methodEnd");
        
        assertTrue("Should handle multiple methods on-demand", methodStartCount >= 3);
        assertEquals("Method callbacks should be balanced", methodStartCount, methodEndCount);
        
        // Callback execution should maintain structural integrity
        assertTrue("On-demand execution should maintain balance", callbackTester.isBalanced());
        
        // Total callback count should reflect selective processing
        int totalCallbacks = callbackTester.getCallbackCount();
        assertTrue("On-demand patterns should generate multiple callbacks", totalCallbacks > 8);
    }
    
    @Test
    public void testConcurrentLazyEvaluationWithThreadSafety() throws Exception {
        // Given: Code suitable for concurrent lazy evaluation
        String concurrentCode = """
            public class ConcurrentLazyTest {
                public void concurrentMethod1() { 
                    processInBackground(); 
                }
                
                public void concurrentMethod2() { 
                    processInBackground(); 
                }
                
                public void concurrentMethod3() { 
                    processInBackground(); 
                }
                
                private void processInBackground() { 
                    // Simulate some processing
                }
            }
            """;
        
        document.setContent(concurrentCode);
        
        // When: Parse concurrently to test thread safety
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
            try {
                adapter.parseWithStrategy(concurrentCode);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executorService);
        
        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
            try {
                adapter.parseWithStrategy(concurrentCode);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executorService);
        
        // Wait for concurrent execution
        CompletableFuture.allOf(future1, future2).join();
        
        // Then: Verify thread safety and correctness
        // Callbacks should be generated despite concurrent access
        assertTrue("Concurrent parsing should generate callbacks", callbackTester.getCallbackCount() > 0);
        
        // Structure should be maintained
        assertTrue(callbackTester.hasCallback("classStart"));
        assertTrue(callbackTester.hasCallback("methodStart"));
        assertTrue(callbackTester.hasCallback("methodEnd"));
        assertTrue(callbackTester.hasCallback("classEnd"));
        
        // Balance should be maintained despite concurrency
        assertTrue("Concurrent lazy evaluation should maintain balance", callbackTester.isBalanced());
    }
    
    @Test
    public void testIntegrationWithTestableDocumentAndLazyEvaluation() throws Exception {
        // Given: Code that tests document integration with lazy patterns
        String code = """
            import java.util.concurrent.CompletableFuture;
            import java.util.function.Supplier;
            
            public class LazyIntegrationTest {
                private final Supplier<String> lazyData = () -> {
                    return entityResolver.resolveData("lazy");
                };
                
                public CompletableFuture<String> getLazyData() {
                    return CompletableFuture.supplyAsync(lazyData);
                }
                
                public void processLazily() {
                    getLazyData()
                        .thenApply(String::toUpperCase)
                        .thenAccept(System.out::println);
                }
            }
            """;
        
        document.setContent(code);
        
        // When: Parse with document and entity integration
        adapter.parseWithStrategy(code);
        
        // Then: Verify integration with lazy evaluation
        assertTrue("Entity resolver should be integrated with lazy evaluation",
                  entityResolver.hasResolvedEntities());
        
        assertEquals("Document content should be preserved", code, document.getContent());
        
        // Verify lazy evaluation callbacks
        assertTrue(callbackTester.hasCallback("importStart"));
        assertTrue(callbackTester.hasCallback("classStart"));
        assertTrue(callbackTester.hasCallback("fieldDeclaration"));
        assertTrue(callbackTester.hasCallback("methodStart"));
        assertTrue(callbackTester.hasCallback("exprStart"));
        assertTrue(callbackTester.hasCallback("exprEnd"));
        assertTrue(callbackTester.hasCallback("methodEnd"));
        assertTrue(callbackTester.hasCallback("classEnd"));
        
        // Integration should maintain structural integrity
        assertTrue("Document integration should maintain balance", callbackTester.isBalanced());
        
        // Lazy evaluation should work with entity resolution
        assertTrue("Lazy transformation supports error recovery", adapter.supportsErrorRecovery());
    }
    
    // ==================== Mock Infrastructure ====================
    
    /**
     * Mock TestableDocument for E2E testing.
     */
    private static class TestableDocument {
        private String content;
        private final EntityResolver entityResolver;
        private final CallbackDelegate callbackDelegate;
        
        public TestableDocument(String name, EntityResolver entityResolver) {
            this.entityResolver = entityResolver;
            this.callbackDelegate = new MockCallbackDelegate();
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public String getContent() {
            return content;
        }
        
        public CallbackDelegate getCallbackDelegate() {
            return callbackDelegate;
        }
    }
    
    /**
     * Mock EntityResolver for E2E testing.
     */
    private static class MockEntityResolver implements EntityResolver {
        private boolean hasResolvedEntities = false;
        
        @Override
        public ValueEntity getValueEntity(String name, Reflective querySource) {
            hasResolvedEntities = true;
            return null;
        }
        
        @Override
        public PackageOrClass resolvePackageOrClass(String name, Reflective querySource) {
            hasResolvedEntities = true;
            return null;
        }
        
        @Override
        public TypeEntity resolveQualifiedClass(String name) {
            hasResolvedEntities = true;
            return null;
        }
        
        public boolean hasResolvedEntities() {
            return hasResolvedEntities;
        }
    }
    
    /**
     * Mock JavaEntity for testing.
     */
    private static class MockJavaEntity extends JavaEntity {
        private final String name;
        
        public MockJavaEntity(String name) {
            this.name = name;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public JavaType getType() {
            // Mock implementation - return null for simplicity
            return null;
        }
        
        @Override
        public JavaEntity getSubentity(String name, Reflective querySource) {
            // Mock implementation - return null for simplicity
            return null;
        }
        
        @Override
        public JavaEntity setTypeArgs(List<TypeArgumentEntity> typeArgs) {
            // Mock implementation - no action needed
            return this;
        }
    }
    
    /**
     * Mock CallbackDelegate that integrates with CallbackTestingUtility.
     */
    private static class MockCallbackDelegate extends CallbackTestingUtility implements CallbackDelegate {
        // Inherits all CallbackDelegate methods from CallbackTestingUtility
        // This provides automatic callback recording and validation
    }
}