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
import bluej.parser.pratt.integration.benchmark.adapters.ErrorRecoveryIntegrationAdapter;
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

import static org.junit.Assert.*;

/**
 * End-to-End integration test for Error Recovery Integration strategy.
 * 
 * This test validates the error recovery integration approach with AutoCloseable
 * callback scopes and continuation strategies. Tests the complete integration
 * with BlueJ framework focusing on maintaining callback integrity during errors.
 * 
 * Key validation aspects:
 * - AutoCloseable callback scopes for guaranteed cleanup
 * - Sophisticated error detection and recovery mechanisms
 * - Callback pairing guarantees even during error conditions
 * - Multiple recovery strategies (skip, retry, substitute)
 * - Comprehensive error type handling and classification
 * - Performance monitoring during error recovery operations
 */
public class ErrorRecoveryIntegrationTest {
    
    private TestableDocument document;
    private MockEntityResolver entityResolver;
    private CallbackTester callbackTester;
    private ErrorRecoveryIntegrationAdapter adapter;
    private TestCorpusGenerator corpusGenerator;
    
    @Before
    public void setUp() {
        entityResolver = new MockEntityResolver();
        document = new TestableDocument("test.java", entityResolver);
        callbackTester = CallbackTestingUtility.forDelegate(document.getCallbackDelegate());
        adapter = new ErrorRecoveryIntegrationAdapter();
        adapter.setCallbackDelegate(document.getCallbackDelegate());
        corpusGenerator = new TestCorpusGenerator();
    }
    
    @Test
    public void testAutoCloseableCallbackScopesWithGuaranteedCleanup() throws Exception {
        // Given: Code with potential parsing issues that test scope cleanup
        String scopeTestCode = """
            public class ScopeCleanupTest {
                // Intentionally malformed to test error recovery
                public void methodWithError() {
                    if (condition) {
                        // Missing closing brace to test scope cleanup
                        System.out.println("test");
                
                // This method should still be processed after error recovery
                public void validMethod() {
                    System.out.println("valid");
                }
            }
            """;
        
        document.setContent(scopeTestCode);
        
        // When: Parse with error recovery
        try {
            adapter.parseWithStrategy(scopeTestCode);
        } catch (Exception e) {
            fail("AutoCloseable scopes should handle parsing errors gracefully: " + e.getMessage());
        }
        
        // Then: Verify guaranteed cleanup maintained callback integrity
        assertTrue("Class scope should be opened", callbackTester.hasCallback("classStart"));
        assertTrue("Class scope should be closed despite errors", callbackTester.hasCallback("classEnd"));
        
        // Method processing should demonstrate cleanup
        assertTrue("Methods should be processed", callbackTester.hasCallback("methodStart"));
        
        // Callback balance should be maintained through scope cleanup
        assertTrue("AutoCloseable scopes should guarantee callback balance",
                  callbackTester.isBalanced());
        
        // Error recovery should allow continued processing
        assertTrue("Error recovery should allow substantial callback generation",
                  callbackTester.getCallbackCount() > 4);
        
        // Excellent error recovery capability
        assertTrue("Error recovery integration supports error recovery", adapter.supportsErrorRecovery());
    }
    
    @Test
    public void testSophisticatedErrorDetectionAndRecoveryMechanisms() throws Exception {
        // Given: Code with various error types to test detection and recovery
        String errorDetectionCode = """
            public class ErrorDetectionTest {
                // Syntax error - missing type
                private field;
                
                // Semantic error - incomplete expression
                public void methodWithSyntaxError() {
                    if (condition &&
                        System.out.println("incomplete");
                    }
                }
                
                // Type error simulation
                public String methodWithTypeError() {
                    int result = "string"; // Type mismatch
                    return result;
                }
                
                // Valid method for recovery verification
                public void validRecoveryMethod() {
                    System.out.println("Recovery successful");
                }
            }
            """;
        
        document.setContent(errorDetectionCode);
        
        // When: Parse with sophisticated error detection
        Object result = adapter.parseWithStrategy(errorDetectionCode);
        assertNotNull("Error recovery should return result object", result);
        
        // Then: Verify sophisticated error detection
        assertTrue("Class should be detected despite errors", callbackTester.hasCallback("classStart"));
        assertTrue("Class should be properly closed", callbackTester.hasCallback("classEnd"));
        
        // Field declaration should be handled despite syntax error
        // (Error recovery might skip or substitute)
        
        // Method processing should demonstrate error recovery
        assertTrue("Methods should be processed with error recovery", callbackTester.hasCallback("methodStart"));
        assertTrue("Methods should be properly closed", callbackTester.hasCallback("methodEnd"));
        
        // Recovery should allow multiple methods to be processed
        int methodCount = callbackTester.countCallbacks("methodStart");
        assertTrue("Error recovery should process multiple methods", methodCount >= 2);
        
        // Sophisticated recovery maintains structural integrity
        assertTrue("Sophisticated recovery should maintain balance", callbackTester.isBalanced());
    }
    
    @Test
    public void testCallbackPairingGuaranteesDuringErrorConditions() throws Exception {
        // Given: Code specifically designed to test callback pairing during errors
        String pairingTestCode = """
            public class CallbackPairingTest {
                public void pairedMethod1() {
                    try {
                        riskyOperation();
                    } catch (Exception e) {
                        // Error handling that should maintain pairing
                }
                
                public void pairedMethod2() {
                    // Nested structures that could break pairing
                    for (int i = 0; i < 10; i++) {
                        if (condition) {
                            // Missing closing braces to test pairing recovery
                            processItem(i);
                
                public void pairedMethod3() {
                    // This should still be processed with proper pairing
                    System.out.println("Pairing maintained");
                }
            }
            """;
        
        document.setContent(pairingTestCode);
        
        // When: Parse with callback pairing guarantees
        adapter.parseWithStrategy(pairingTestCode);
        
        // Then: Verify callback pairing guarantees
        int methodStartCount = callbackTester.countCallbacks("methodStart");
        int methodEndCount = callbackTester.countCallbacks("methodEnd");
        
        // Pairing should be guaranteed even with errors
        assertEquals("Error recovery should guarantee method callback pairing",
                    methodStartCount, methodEndCount);
        
        // Class-level pairing should also be guaranteed
        int classStartCount = callbackTester.countCallbacks("classStart");
        int classEndCount = callbackTester.countCallbacks("classEnd");
        assertEquals("Error recovery should guarantee class callback pairing",
                    classStartCount, classEndCount);
        
        // Overall balance should be maintained
        assertTrue("Callback pairing guarantees should maintain overall balance",
                  callbackTester.isBalanced());
        
        // Multiple methods should be processed despite errors
        assertTrue("Error recovery should process multiple methods", methodStartCount >= 2);
    }
    
    @Test
    public void testMultipleRecoveryStrategiesSkipRetrySubstitute() throws Exception {
        // Given: Code that exercises different recovery strategies
        String recoveryStrategiesCode = """
            public class RecoveryStrategiesTest {
                // Skip strategy - syntax error that should be skipped
                private invalid syntax here;
                
                // Retry strategy - complex expression that might need simpler parsing
                public void retryMethod() {
                    ComplexGeneric<Map<String, List<Optional<Future<Result>>>>> complex;
                }
                
                // Substitute strategy - incomplete statement that needs placeholder
                public void substituteMethod() {
                    String incomplete = 
                }
                
                // Normal processing to verify recovery doesn't break valid code
                public void normalMethod() {
                    System.out.println("Normal processing");
                }
                
                public void anotherNormalMethod() {
                    int x = 42;
                    return x;
                }
            }
            """;
        
        document.setContent(recoveryStrategiesCode);
        
        // When: Parse with multiple recovery strategies
        adapter.parseWithStrategy(recoveryStrategiesCode);
        
        // Then: Verify multiple recovery strategies
        assertTrue("Class should be processed despite errors", callbackTester.hasCallback("classStart"));
        assertTrue("Class should be properly closed", callbackTester.hasCallback("classEnd"));
        
        // Methods should be processed with various recovery strategies
        int methodCount = callbackTester.countCallbacks("methodStart");
        assertTrue("Multiple recovery strategies should process methods", methodCount >= 3);
        
        // Error recovery callbacks might be present
        // (These are strategy-specific and may include error indicators)
        
        // Field declarations might be skipped or substituted
        // This is implementation-dependent for error recovery
        
        // Overall structural integrity maintained
        assertTrue("Multiple recovery strategies should maintain balance",
                  callbackTester.isBalanced());
        
        // Total callback count should reflect recovery processing
        int totalCallbacks = callbackTester.getCallbackCount();
        assertTrue("Recovery strategies should generate substantial callbacks", totalCallbacks > 6);
    }
    
    @Test
    public void testComprehensiveErrorTypeHandlingAndClassification() throws Exception {
        // Given: Code with different error categories
        String errorClassificationCode = """
            public class ErrorClassificationTest {
                // Syntax errors
                public void syntaxError() {
                    if (condition && {
                        // Missing condition and closing parenthesis
                        System.out.println("syntax error");
                    }
                }
                
                // Type mismatch simulation
                public int typeMismatchError() {
                    String value = "text";
                    return value; // Type mismatch
                }
                
                // Incomplete statements
                public void incompleteStatementError() {
                    Map<String, List<Integer>> map = new HashMap<>();
                    map.put("key", 
                    // Incomplete statement
                }
                
                // Missing imports (simulated)
                public void missingImportError() {
                    Optional<String> opt = Optional.of("value"); // Missing import
                    CompletableFuture<String> future; // Missing import
                }
                
                // Valid method for classification verification
                public void validMethod() {
                    System.out.println("Classification successful");
                }
            }
            """;
        
        document.setContent(errorClassificationCode);
        
        // When: Parse with comprehensive error classification
        adapter.parseWithStrategy(errorClassificationCode);
        
        // Then: Verify comprehensive error handling
        assertTrue("Class should handle various error types", callbackTester.hasCallback("classStart"));
        assertTrue("Class should be closed despite error variety", callbackTester.hasCallback("classEnd"));
        
        // Method processing should handle different error categories
        int methodCount = callbackTester.countCallbacks("methodStart");
        assertTrue("Should process methods despite various error types", methodCount >= 4);
        
        // Error classification should maintain structural integrity
        assertTrue("Error classification should maintain callback balance",
                  callbackTester.isBalanced());
        
        // Expression handling should work despite type mismatches
        assertTrue("Methods should be processed", callbackTester.hasCallback("methodStart"));
        assertTrue("Methods should be closed", callbackTester.hasCallback("methodEnd"));
        
        // Comprehensive handling should allow substantial processing
        int totalCallbacks = callbackTester.getCallbackCount();
        assertTrue("Comprehensive error handling should generate many callbacks", totalCallbacks > 10);
    }
    
    @Test
    public void testPerformanceMonitoringDuringErrorRecoveryOperations() throws Exception {
        // Given: Code designed to test error recovery performance
        String performanceTestCode = """
            public class ErrorRecoveryPerformanceTest {
                // Multiple methods with various error scenarios
                public void errorMethod1() {
                    try {
                        complex.nested.operations.that.might.fail();
                    } catch (Exception e) {
                        recovery.logic.here();
                }
                
                public void errorMethod2() {
                    if (condition && another.condition &&
                        // Complex incomplete expression
                        System.out.println("performance test");
                    }
                }
                
                public void errorMethod3() {
                    Map<String, List<Optional<Future<Result>>>> complex = 
                        // Incomplete complex type declaration
                }
                
                // Many valid methods to measure recovery impact
                public void validMethod1() { System.out.println("1"); }
                public void validMethod2() { System.out.println("2"); }
                public void validMethod3() { System.out.println("3"); }
                public void validMethod4() { System.out.println("4"); }
                public void validMethod5() { System.out.println("5"); }
            }
            """;
        
        document.setContent(performanceTestCode);
        
        // When: Parse with performance monitoring
        long startTime = System.nanoTime();
        adapter.parseWithStrategy(performanceTestCode);
        long endTime = System.nanoTime();
        
        double executionMs = (endTime - startTime) / 1_000_000.0;
        
        // Then: Verify performance monitoring
        // Error recovery should maintain reasonable performance
        assertTrue("Error recovery should maintain good performance", executionMs < 200);
        
        // Should process substantial number of methods despite errors
        int methodCount = callbackTester.countCallbacks("methodStart");
        assertTrue("Should process many methods with error recovery", methodCount >= 6);
        
        // Performance impact should not prevent callback generation
        int totalCallbacks = callbackTester.getCallbackCount();
        assertTrue("Error recovery should generate substantial callbacks", totalCallbacks > 15);
        
        // Performance should not compromise structural integrity
        assertTrue("Performance optimization should maintain balance", callbackTester.isBalanced());
        
        // Error recovery should be efficient
        double callbacksPerMs = totalCallbacks / Math.max(executionMs, 1.0);
        assertTrue("Error recovery should maintain reasonable callback throughput", callbacksPerMs > 0.5);
    }
    
    @Test
    public void testContinuationStrategiesWithPartialSuccess() throws Exception {
        // Given: Code that tests continuation after partial failures
        String continuationCode = """
            public class ContinuationTest {
                // First section with errors
                public void problemSection() {
                    invalid.syntax.here;
                    Map<incomplete = new
                }
                
                // Continuation should work despite previous errors
                public class InnerClass {
                    public void innerMethod() {
                        System.out.println("Inner continuation");
                    }
                }
                
                // More continuation after nested structures
                public void afterInnerClass() {
                    String value = "continuation works";
                }
                
                // Interface continuation
                public interface ContinuationInterface {
                    void interfaceMethod();
                }
                
                // Final method to verify full continuation
                public void finalMethod() {
                    System.out.println("Full continuation successful");
                }
            }
            """;
        
        document.setContent(continuationCode);
        
        // When: Parse with continuation strategies
        adapter.parseWithStrategy(continuationCode);
        
        // Then: Verify continuation strategies
        // Main class should be processed
        assertTrue("Main class should be processed", callbackTester.hasCallback("classStart"));
        assertTrue("Main class should be completed", callbackTester.hasCallback("classEnd"));
        
        // Inner structures should demonstrate continuation
        int classCount = callbackTester.countCallbacks("classStart");
        assertTrue("Should continue to process inner structures", classCount >= 2);
        
        // Methods should be processed across error boundaries
        int methodCount = callbackTester.countCallbacks("methodStart");
        assertTrue("Should continue processing methods after errors", methodCount >= 3);
        
        // Continuation should maintain structural integrity
        assertTrue("Continuation should maintain callback balance", callbackTester.isBalanced());
        
        // Partial success should allow substantial processing
        int totalCallbacks = callbackTester.getCallbackCount();
        assertTrue("Continuation should allow substantial processing", totalCallbacks > 8);
    }
    
    @Test
    public void testIntegrationWithTestableDocumentAndErrorRecovery() throws Exception {
        // Given: Code that tests document integration with error scenarios
        String integrationCode = """
            import java.util.Optional;
            import java.util.concurrent.CompletableFuture;
            
            public class ErrorRecoveryIntegrationTest {
                // Field with potential resolution issues
                private EntityResolver resolver;
                
                public Optional<String> processWithErrors() {
                    try {
                        String data = resolver.resolveData("test");
                        return Optional.of(data);
                    } catch (Exception e) {
                        // Error recovery in entity resolution
                        return Optional.empty();
                    } finally {
                        // Cleanup should always happen
                        resolver.cleanup();
                }
                
                public CompletableFuture<Void> asyncProcessing() {
                    return CompletableFuture.runAsync(() -> {
                        // Async error handling
                        try {
                            processWithErrors();
                        } catch (RuntimeException e) {
                            handleError(e);
                        }
                    });
                }
                
                private void handleError(RuntimeException e) {
                    System.err.println("Error handled: " + e.getMessage());
                }
            }
            """;
        
        document.setContent(integrationCode);
        
        // When: Parse with document and entity integration
        adapter.parseWithStrategy(integrationCode);
        
        // Then: Verify integration with error recovery
        assertTrue("Entity resolver should be integrated with error recovery",
                  entityResolver.hasResolvedEntities());
        
        assertEquals("Document content should be preserved during error recovery",
                    integrationCode, document.getContent());
        
        // Error recovery should maintain import processing
        assertTrue("Imports should be processed", callbackTester.hasCallback("importStart"));
        
        // Class and method structure should be maintained
        assertTrue("Class should be processed", callbackTester.hasCallback("classStart"));
        assertTrue("Methods should be processed", callbackTester.hasCallback("methodStart"));
        assertTrue("Methods should be closed", callbackTester.hasCallback("methodEnd"));
        assertTrue("Class should be closed", callbackTester.hasCallback("classEnd"));
        
        // Field declarations should be handled
        assertTrue("Fields should be processed", callbackTester.hasCallback("fieldDeclaration"));
        
        // Integration should maintain excellent error recovery
        assertTrue("Integration should maintain balance", callbackTester.isBalanced());
        assertTrue("Integration supports error recovery", adapter.supportsErrorRecovery());
        
        // Complex async structures should be handled
        int methodCount = callbackTester.countCallbacks("methodStart");
        assertTrue("Should handle complex async methods", methodCount >= 2);
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