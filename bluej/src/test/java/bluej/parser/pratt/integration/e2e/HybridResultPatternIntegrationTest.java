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

import bluej.debugger.gentype.JavaType;
import bluej.parser.CallbackDelegate;
import bluej.parser.pratt.integration.CallbackTestingUtility;
import bluej.parser.pratt.integration.CallbackTestingUtility.CallbackTester;
import bluej.parser.pratt.integration.benchmark.adapters.HybridResultPatternAdapter;
import bluej.parser.pratt.integration.benchmark.corpus.TestCorpusGenerator;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.ValueEntity;
import bluej.parser.entity.TypeEntity;
import bluej.parser.entity.PackageOrClass;
import bluej.parser.entity.TypeArgumentEntity;
import bluej.debugger.gentype.Reflective;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;

/**
 * End-to-End integration test for Hybrid Result Pattern integration strategy.
 * 
 * This test validates the hybrid result pattern approach that combines monadic
 * error handling with callback delegation for AST construction. Tests the complete
 * integration with BlueJ framework focusing on deferred callback execution.
 * 
 * Key validation aspects:
 * - ParseResult monad for error tracking and propagation
 * - Deferred callback emission through lambda closures
 * - Error accumulation across multiple parsing operations
 * - Callback integrity during partial failures
 * - Performance characteristics of hybrid result handling
 */
public class HybridResultPatternIntegrationTest {
    
    private TestableDocument document;
    private MockEntityResolver entityResolver;
    private CallbackTester callbackTester;
    private HybridResultPatternAdapter adapter;
    private TestCorpusGenerator corpusGenerator;
    
    @Before
    public void setUp() {
        entityResolver = new MockEntityResolver();
        document = new TestableDocument("test.java", entityResolver);
        callbackTester = CallbackTestingUtility.forDelegate(document.getCallbackDelegate());
        adapter = new HybridResultPatternAdapter();
        adapter.setCallbackDelegate(document.getCallbackDelegate());
        corpusGenerator = new TestCorpusGenerator();
    }
    
    @Test
    public void testMonadicErrorHandlingWithDeferredCallbacks() throws Exception {
        // Given: Code that will be parsed in monadic chain
        String code = """
            package com.example;
            import java.util.List;
            
            public class MonadicTest {
                private List<String> items;
                
                public void processItems() {
                    items.stream().forEach(System.out::println);
                }
            }
            """;
        
        document.setContent(code);
        
        // When: Parse with hybrid result pattern
        Object result = adapter.parseWithStrategy(code);
        
        // Then: Verify monadic result handling
        assertNotNull("Hybrid result should not be null", result);
        
        // Verify deferred callbacks were executed
        assertTrue(callbackTester.hasCallback("importStart"));
        assertTrue(callbackTester.hasCallback("importEnd"));
        assertTrue(callbackTester.hasCallback("classStart"));
        assertTrue(callbackTester.hasCallback("methodStart"));
        assertTrue(callbackTester.hasCallback("methodEnd"));
        assertTrue(callbackTester.hasCallback("classEnd"));
        
        // Verify callback balance maintained through monadic chain
        assertTrue("Monadic chain should maintain callback balance", callbackTester.isBalanced());
    }
    
    @Test
    public void testDeferredCallbackEmissionWithLambdaClosures() throws Exception {
        // Given: Complex code requiring deferred processing
        String code = """
            public class DeferredTest {
                public void complexMethod() {
                    // First parsing phase - structure identified
                    CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                        return processData();
                    });
                    
                    // Second phase - callbacks emitted
                    future.thenAccept(result -> {
                        System.out.println("Result: " + result);
                    });
                }
                
                private String processData() {
                    return "processed";
                }
            }
            """;
        
        document.setContent(code);
        
        // When: Parse with deferred emission
        adapter.parseWithStrategy(code);
        
        // Then: Verify deferred callback patterns
        // Callbacks should be emitted in logical groups
        assertTrue(callbackTester.hasCallback("classStart"));
        assertTrue(callbackTester.hasCallback("methodStart"));
        assertTrue(callbackTester.hasCallback("exprStart"));
        assertTrue(callbackTester.hasCallback("exprEnd"));
        assertTrue(callbackTester.hasCallback("methodEnd"));
        assertTrue(callbackTester.hasCallback("classEnd"));
        
        // Verify callback count reflects deferred emission
        int callbackCount = callbackTester.getCallbackCount();
        assertTrue("Deferred emission should generate multiple callbacks", callbackCount >= 6);
        
        // Verify balance maintained despite deferred execution
        assertTrue("Deferred callbacks should maintain balance", callbackTester.isBalanced());
    }
    
    @Test
    public void testErrorAccumulationAcrossParsings() throws Exception {
        // Given: Code with potential parsing challenges
        String codeWithIssues = """
            public class ErrorAccumulationTest {
                // Missing type in declaration
                private items;
                
                public void methodWithIssues() {
                    // Incomplete expression
                    if (condition &&
                        System.out.println("test");
                    }
                }
                
                // This method should still be parsed correctly
                public void validMethod() {
                    System.out.println("Valid");
                }
            }
            """;
        
        document.setContent(codeWithIssues);
        
        // When: Parse with error accumulation
        try {
            adapter.parseWithStrategy(codeWithIssues);
            // Should not throw - if we get here, test passes this assertion
        } catch (Exception e) {
            fail("Should not throw exception during error accumulation: " + e.getMessage());
        }
        
        // Then: Verify partial success with error accumulation
        assertTrue("Error accumulation should still generate callbacks",
                  callbackTester.getCallbackCount() > 0);
        
        // Valid parts should still be processed
        assertTrue(callbackTester.hasCallback("classStart"));
        assertTrue(callbackTester.hasCallback("classEnd"));
        
        // Hybrid pattern has excellent error recovery
        assertTrue("Hybrid pattern supports error recovery", adapter.supportsErrorRecovery());
    }
    
    @Test
    public void testCallbackIntegrityDuringPartialFailures() throws Exception {
        // Given: Code designed to test partial failure scenarios
        String partialFailureCode = """
            public class PartialFailureTest {
                public void successfulMethod() {
                    System.out.println("This should work");
                }
                
                public void problematicMethod() {
                    // Simulate parsing complexity that might fail
                    Map<String, Function<String, CompletableFuture<Optional<Result>>>>
                        complexStructure = new HashMap<>();
                }
                
                public void anotherSuccessfulMethod() {
                    return "success";
                }
            }
            """;
        
        document.setContent(partialFailureCode);
        
        // When: Parse with partial failure handling
        adapter.parseWithStrategy(partialFailureCode);
        
        // Then: Verify callback integrity maintained
        assertTrue("Class parsing should succeed", callbackTester.hasCallback("classStart"));
        assertTrue("Class should be properly closed", callbackTester.hasCallback("classEnd"));
        
        // Method parsing should maintain integrity
        assertTrue(callbackTester.hasCallback("methodStart"));
        assertTrue(callbackTester.hasCallback("methodEnd"));
        
        // Balance should be maintained despite partial failures
        assertTrue("Partial failures should not break callback balance", callbackTester.isBalanced());
        
        // Multiple successful operations should be tracked
        int methodStartCount = callbackTester.countCallbacks("methodStart");
        int methodEndCount = callbackTester.countCallbacks("methodEnd");
        assertEquals("Method callbacks should be balanced", methodStartCount, methodEndCount);
    }
    
    @Test
    public void testLightweightResultObjectsWithCallbackReferences() throws Exception {
        // Given: Code that tests result object efficiency
        String code = """
            public class ResultObjectTest {
                private final String constant = "value";
                
                public ResultObjectTest(String param) {
                    this.field = param;
                }
                
                public Optional<String> getProcessedValue() {
                    return Optional.ofNullable(constant)
                                  .map(String::toUpperCase);
                }
            }
            """;
        
        document.setContent(code);
        
        // When: Parse and measure result characteristics
        long startTime = System.nanoTime();
        Object result = adapter.parseWithStrategy(code);
        long endTime = System.nanoTime();
        
        // Then: Verify lightweight result handling
        assertNotNull("Result object should be created", result);
        
        // Performance should be efficient for hybrid pattern
        double executionMs = (endTime - startTime) / 1_000_000.0;
        assertTrue("Hybrid pattern should be efficient", executionMs < 50);
        
        // Callbacks should reference lightweight structures
        assertTrue(callbackTester.hasCallback("classStart"));
        assertTrue(callbackTester.hasCallback("fieldDeclaration"));
        assertTrue(callbackTester.hasCallback("methodStart"));
        assertTrue(callbackTester.isBalanced());
    }
    
    @Test
    public void testSophisticatedErrorRecoveryWithCallbackIntegrity() throws Exception {
        // Given: Code specifically designed to test error recovery
        String recoveryTestCode = """
            public class ErrorRecoveryTest {
                public void normalMethod() {
                    System.out.println("normal");
                }
                
                public void errorProneMethod() {
                    // Various error scenarios
                    Map<List<Future<Optional<Result>>>, 
                        Function<Exception, CompletableFuture<Void>>> 
                        complexErrorStructure;
                    
                    // This should recover
                    try {
                        processComplexData();
                    } catch (Exception e) {
                        handleError(e);
                    }
                }
                
                public void finalMethod() {
                    cleanup();
                }
            }
            """;
        
        document.setContent(recoveryTestCode);
        
        // When: Parse with error recovery testing
        adapter.parseWithStrategy(recoveryTestCode);
        
        // Then: Verify sophisticated error recovery
        assertTrue(callbackTester.hasCallback("classStart"));
        assertTrue(callbackTester.hasCallback("classEnd"));
        
        // Error recovery should maintain callback integrity
        assertTrue("Error recovery should maintain balance", callbackTester.isBalanced());
        
        // All methods should be processed despite complexity
        int methodCount = callbackTester.countCallbacks("methodStart");
        assertTrue("Multiple methods should be processed", methodCount >= 2);
        
        // Callback pairing should be excellent
        int startCallbacks = callbackTester.countCallbacks("methodStart");
        int endCallbacks = callbackTester.countCallbacks("methodEnd");
        assertEquals("Method callback pairing should be perfect", startCallbacks, endCallbacks);
    }
    
    @Test
    public void testPerformanceCharacteristicsOfHybridResultHandling() throws Exception {
        // Given: Various complexity levels for performance testing
        List<String> testCases = corpusGenerator.generateComplexitySamples(5);
        
        for (int i = 0; i < testCases.size(); i++) {
            String code = testCases.get(i);
            callbackTester.clear();
            document.setContent(code);
            
            // When: Parse with performance measurement
            long startTime = System.nanoTime();
            adapter.parseWithStrategy(code);
            long endTime = System.nanoTime();
            
            double executionMs = (endTime - startTime) / 1_000_000.0;
            
            // Then: Verify performance characteristics
            assertTrue("Hybrid pattern should maintain good performance", executionMs < 100);
            
            // Verify callback generation scales appropriately
            int callbackCount = callbackTester.getCallbackCount();
            assertTrue("Should generate callbacks for complexity level " + i, callbackCount > 0);
            
            // Balance should be maintained at all complexity levels
            assertTrue("Balance should be maintained at complexity level " + i,
                      callbackTester.isBalanced());
        }
    }
    
    @Test
    public void testIntegrationWithTestableDocumentAndMonadicErrorHandling() throws Exception {
        // Given: Code that tests document integration with error handling
        String code = """
            import java.util.concurrent.CompletableFuture;
            import java.util.Optional;
            
            public class IntegrationTest {
                public CompletableFuture<Optional<String>> processAsync() {
                    return CompletableFuture
                        .supplyAsync(() -> getData())
                        .thenApply(Optional::ofNullable)
                        .exceptionally(ex -> Optional.empty());
                }
                
                private String getData() {
                    return entityResolver.resolve("data");
                }
            }
            """;
        
        document.setContent(code);
        
        // When: Parse with document integration
        adapter.parseWithStrategy(code);
        
        // Then: Verify document and entity resolver integration
        assertTrue("Entity resolver should be integrated",
                  entityResolver.hasResolvedEntities());
        
        assertEquals("Document content should be preserved", code, document.getContent());
        
        // Verify monadic structure callbacks
        assertTrue(callbackTester.hasCallback("importStart"));
        assertTrue(callbackTester.hasCallback("classStart"));
        assertTrue(callbackTester.hasCallback("methodStart"));
        assertTrue(callbackTester.hasCallback("exprStart"));
        assertTrue(callbackTester.hasCallback("exprEnd"));
        assertTrue(callbackTester.hasCallback("methodEnd"));
        assertTrue(callbackTester.hasCallback("classEnd"));
        
        // Verify balance with complex monadic structures
        assertTrue("Complex monadic structures should maintain balance", callbackTester.isBalanced());
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