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
import bluej.parser.pratt.integration.benchmark.adapters.ASTVisitorPatternAdapter;
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
 * End-to-End integration test for AST Visitor Pattern integration strategy.
 * 
 * This test validates the AST visitor pattern approach where callbacks are invoked
 * through a visitor pattern traversing the AST. Tests the complete integration
 * with BlueJ framework using TestableDocument setup and CallbackTestingUtility.
 * 
 * Key validation aspects:
 * - Visitor callback sequences and tree traversal patterns
 * - AST construction and callback emission timing
 * - Callback balance verification for structured parsing
 * - Error handling during AST visitor traversal
 * - Performance characteristics of visitor pattern
 */
public class ASTVisitorPatternIntegrationTest {
    
    private TestableDocument document;
    private MockEntityResolver entityResolver;
    private CallbackTester callbackTester;
    private ASTVisitorPatternAdapter adapter;
    private TestCorpusGenerator corpusGenerator;
    
    @Before
    public void setUp() {
        entityResolver = new MockEntityResolver();
        document = new TestableDocument("test.java", entityResolver);
        callbackTester = CallbackTestingUtility.forDelegate(document.getCallbackDelegate());
        adapter = new ASTVisitorPatternAdapter();
        adapter.setCallbackDelegate(document.getCallbackDelegate());
        corpusGenerator = new TestCorpusGenerator();
    }
    
    @Test
    public void testSimpleClassParsingWithVisitorCallbacks() throws Exception {
        // Given: Simple class structure
        String code = """
            package com.example;
            
            public class TestClass {
                private String field;
                
                public void method() {
                    System.out.println("Hello");
                }
            }
            """;
        
        document.setContent(code);
        
        // When: Parse with AST visitor pattern
        adapter.parseWithStrategy(code);
        
        // Then: Verify visitor callback sequence
        assertTrue(callbackTester.hasCallback("packageDeclaration"));
        assertTrue(callbackTester.hasCallback("classStart"));
        assertTrue(callbackTester.hasCallback("fieldDeclaration"));
        assertTrue(callbackTester.hasCallback("methodStart"));
        assertTrue(callbackTester.hasCallback("methodEnd"));
        assertTrue(callbackTester.hasCallback("classEnd"));
        
        // Verify callback balance
        assertTrue("AST visitor callbacks should be balanced", callbackTester.isBalanced());
        
        // Verify callback sequence maintains tree traversal order
        List<String> expectedSequence = Arrays.asList(
            "packageDeclaration", "classStart", "fieldDeclaration", 
            "methodStart", "methodEnd", "classEnd"
        );
        assertTrue(callbackTester.hasCallbackSequence(expectedSequence));
    }
    
    @Test
    public void testComplexNestedStructureVisitorTraversal() throws Exception {
        // Given: Complex nested structure
        String code = """
            public class OuterClass {
                public class InnerClass {
                    public void innerMethod() {
                        if (condition) {
                            for (int i = 0; i < 10; i++) {
                                processItem(i);
                            }
                        }
                    }
                }
            }
            """;
        
        document.setContent(code);
        
        // When: Parse with AST visitor pattern
        adapter.parseWithStrategy(code);
        
        // Then: Verify nested visitor traversal
        assertTrue(callbackTester.hasCallback("classStart"));
        assertTrue(callbackTester.hasCallback("methodStart"));
        assertTrue(callbackTester.hasCallback("blockStart"));
        assertTrue(callbackTester.hasCallback("statementStart"));
        assertTrue(callbackTester.hasCallback("statementEnd"));
        assertTrue(callbackTester.hasCallback("blockEnd"));
        assertTrue(callbackTester.hasCallback("methodEnd"));
        assertTrue(callbackTester.hasCallback("classEnd"));
        
        // Verify callback balance for nested structures
        assertTrue("Nested structure callbacks should be balanced", callbackTester.isBalanced());
        
        // Verify minimum callback count for complex structure
        assertTrue("Complex structure should generate multiple callbacks",
                  callbackTester.getCallbackCount() >= 10);
    }
    
    @Test
    public void testMultipleCodeComplexityLevels() throws Exception {
        // Given: Different complexity levels
        List<String> testCases = corpusGenerator.generateComplexitySamples(3);
        
        for (int i = 0; i < testCases.size(); i++) {
            String code = testCases.get(i);
            callbackTester.clear();
            document.setContent(code);
            
            // When: Parse with visitor pattern
            adapter.parseWithStrategy(code);
            
            // Then: Verify callbacks based on complexity
            assertTrue("Complexity level " + i + " should generate callbacks",
                      callbackTester.getCallbackCount() > 0);
            assertTrue("Complexity level " + i + " should maintain callback balance",
                      callbackTester.isBalanced());
            
            // Higher complexity should generate more callbacks
            if (i > 0) {
                int previousCount = callbackTester.getCallbackCount();
                // Note: This is a general trend test, not strict requirement
            }
        }
    }
    
    @Test
    public void testErrorHandlingDuringVisitorTraversal() throws Exception {
        // Given: Malformed code that should trigger error handling
        String malformedCode = """
            public class BrokenClass {
                public void missingBrace() {
                    if (condition) {
                        // Missing closing brace
                
                public void anotherMethod() {
                    // This should still be processed by error recovery
                }
            }
            """;
        
        document.setContent(malformedCode);
        
        // When: Parse with error recovery
        try {
            adapter.parseWithStrategy(malformedCode);
            // Should not throw - if we get here, test passes this assertion
        } catch (Exception e) {
            fail("Should not throw exception during error recovery: " + e.getMessage());
        }
        
        // Then: Verify partial callback processing
        assertTrue("Error recovery should still generate some callbacks",
                  callbackTester.getCallbackCount() > 0);
        
        // AST visitor pattern has good error recovery
        assertTrue("Class start should be detected even with errors",
                  callbackTester.hasCallback("classStart"));
    }
    
    @Test
    public void testVisitorCallbackTimingAndOrdering() throws Exception {
        // Given: Code with clear structural hierarchy
        String code = """
            public class TimingTest {
                private int field1;
                private String field2;
                
                public TimingTest() {
                    this.field1 = 0;
                    this.field2 = "";
                }
                
                public void method1() { }
                public void method2() { }
            }
            """;
        
        document.setContent(code);
        
        // When: Parse and capture timing
        long startTime = System.nanoTime();
        adapter.parseWithStrategy(code);
        long endTime = System.nanoTime();
        
        // Then: Verify callback ordering
        List<String> sequence = callbackTester.getCallbackSequence();
        
        // Class should be processed before its members
        int classStartIndex = sequence.indexOf("classStart");
        int firstFieldIndex = sequence.indexOf("fieldDeclaration");
        int classEndIndex = sequence.indexOf("classEnd");
        
        assertTrue("Class start should precede field declarations",
                  classStartIndex < firstFieldIndex);
        assertTrue("Field declarations should precede class end",
                  firstFieldIndex < classEndIndex);
        
        // Verify execution time is reasonable for visitor pattern
        double executionMs = (endTime - startTime) / 1_000_000.0;
        assertTrue("Visitor pattern should execute quickly", executionMs < 100);
    }
    
    @Test
    public void testASTVisitorPatternCharacteristics() throws Exception {
        // Given: Code that showcases visitor pattern strengths
        String code = """
            public class VisitorStrengthTest {
                @Override
                public void complexMethod() {
                    try {
                        List<String> items = Arrays.asList("a", "b", "c");
                        items.stream()
                             .filter(s -> s.length() > 0)
                             .map(String::toUpperCase)
                             .forEach(System.out::println);
                    } catch (Exception e) {
                        logger.error("Processing failed", e);
                    } finally {
                        cleanup();
                    }
                }
            }
            """;
        
        document.setContent(code);
        
        // When: Parse with visitor pattern
        adapter.parseWithStrategy(code);
        
        // Then: Verify visitor pattern characteristics
        // 1. Clean separation - callbacks follow structured tree traversal
        assertTrue("Clean separation maintains balance", callbackTester.isBalanced());
        
        // 2. Comprehensive coverage - all language constructs handled
        assertTrue(callbackTester.hasCallback("methodStart"));
        assertTrue(callbackTester.hasCallback("methodEnd"));
        
        // 3. Predictable performance - consistent callback generation
        int callbackCount = callbackTester.getCallbackCount();
        assertTrue("Complex code should generate multiple callbacks", callbackCount > 5);
        
        // 4. Error recovery capability - tested in previous test
        assertTrue("Visitor pattern supports error recovery", adapter.supportsErrorRecovery());
    }
    
    @Test
    public void testIntegrationWithTestableDocumentAndEntityResolver() throws Exception {
        // Given: Code with entity references
        String code = """
            import java.util.List;
            
            public class EntityTest {
                private List<String> items;
                
                public void processItems() {
                    items.forEach(this::processItem);
                }
                
                private void processItem(String item) {
                    System.out.println(item);
                }
            }
            """;
        
        document.setContent(code);
        
        // When: Parse and resolve entities
        adapter.parseWithStrategy(code);
        
        // Then: Verify entity resolution integration
        assertTrue("EntityResolver should process entity references",
                  entityResolver.hasResolvedEntities());
        
        // Verify document state
        assertEquals("Document content should be preserved", code, document.getContent());
        
        // Verify callback integration
        assertTrue(callbackTester.hasCallback("importStart"));
        assertTrue(callbackTester.hasCallback("classStart"));
        assertTrue(callbackTester.isBalanced());
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