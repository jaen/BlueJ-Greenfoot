package bluej.parser.pratt;

import bluej.parser.CallbackDelegate;
import bluej.parser.LocatableToken;
import bluej.parser.JavaTokenTypes;
import bluej.parser.TokenOperations;
import bluej.parser.SourceParser;
import bluej.parser.NodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for migrated parsing methods with ParseScope.
 * 
 * Tests the three key methods that have been migrated to use the ParseScope
 * system: parseTypeDef(), processFunction(), and parseStmtBlock().
 * These tests validate that the methods correctly integrate with the scope
 * management system while maintaining their original parsing behavior.
 * 
 * Coverage Areas:
 * - parseTypeDef() integration with TYPE_BODY scopes
 * - processFunction() integration with METHOD_BODY scopes  
 * - parseStmtBlock() integration with BLOCK scopes
 * - Scope metadata tracking (name, position)
 * - Error handling in migrated methods
 * - Callback delegation through existing parser field
 * - Integration with existing parser infrastructure
 */
@DisplayName("Method Integration Tests")
public class MethodIntegrationTest {

    @Mock
    private CallbackDelegate mockDelegate;
    
    @Mock
    private TokenOperations mockTokenOps;
    
    @Mock
    private SourceParser mockSourceParser;
    
    @Mock
    private NodeFactory mockNodeFactory;
    
    @Mock
    private LocatableToken mockToken;
    
    @Mock
    private LocatableToken mockClassToken;
    
    @Mock
    private LocatableToken mockMethodToken;
    
    @Mock
    private LocatableToken mockBlockToken;
    
    private KotlinPrattParser parser;
    private CallbackTracker callbackTracker;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup mock tokens
        setupMockToken(mockToken, "identifier", JavaTokenTypes.IDENTIFIER);
        setupMockToken(mockClassToken, "class", JavaTokenTypes.LITERAL_class);
        setupMockToken(mockMethodToken, "fun", JavaTokenTypes.LITERAL_fun);
        setupMockToken(mockBlockToken, "{", JavaTokenTypes.LCURLY);
        
        // Setup source parser to return callback delegate
        when(mockSourceParser.getCallbackDelegate()).thenReturn(mockDelegate);
        
        // Create parser instance
        parser = new KotlinPrattParser(mockTokenOps, mockSourceParser, mockNodeFactory);
        
        // Setup callback tracking
        callbackTracker = new CallbackTracker();
        setupCallbackTracking();
    }
    
    private void setupMockToken(LocatableToken token, String text, int type) {
        when(token.getText()).thenReturn(text);
        when(token.getType()).thenReturn(type);
        when(token.getLine()).thenReturn(1);
        when(token.getColumn()).thenReturn(1);
    }
    
    /**
     * Helper class to track callback sequences for validation
     */
    private static class CallbackTracker {
        private final List<String> callbackSequence = new ArrayList<>();
        
        void record(String callback) {
            callbackSequence.add(callback);
        }
        
        List<String> getSequence() {
            return new ArrayList<>(callbackSequence);
        }
        
        void reset() {
            callbackSequence.clear();
        }
        
        boolean contains(String callback) {
            return callbackSequence.contains(callback);
        }
        
        int count(String callback) {
            return (int) callbackSequence.stream().filter(c -> c.equals(callback)).count();
        }
    }
    
    private void setupCallbackTracking() {
        // Track type body callbacks
        doAnswer(invocation -> {
            callbackTracker.record("beginTypeBody");
            return null;
        }).when(mockDelegate).beginTypeBody(any());
        
        doAnswer(invocation -> {
            callbackTracker.record("endTypeBody");
            return null;
        }).when(mockDelegate).endTypeBody(any(), anyBoolean());
        
        // Track method body callbacks
        doAnswer(invocation -> {
            callbackTracker.record("beginMethodBody");
            return null;
        }).when(mockDelegate).beginMethodBody(any());
        
        doAnswer(invocation -> {
            callbackTracker.record("endMethodBody");
            return null;
        }).when(mockDelegate).endMethodBody(any(), anyBoolean());
        
        // Track statement block callbacks
        doAnswer(invocation -> {
            callbackTracker.record("beginStmtblockBody");
            return null;
        }).when(mockDelegate).beginStmtblockBody(any());
        
        doAnswer(invocation -> {
            callbackTracker.record("endStmtblockBody");
            return null;
        }).when(mockDelegate).endStmtblockBody(any(), anyBoolean());
        
        // Track element callbacks
        doAnswer(invocation -> {
            callbackTracker.record("beginElement");
            return null;
        }).when(mockDelegate).beginElement(any());
        
        doAnswer(invocation -> {
            callbackTracker.record("endElement");
            return null;
        }).when(mockDelegate).endElement(any(), anyBoolean());
        
        // Track expression callbacks
        doAnswer(invocation -> {
            callbackTracker.record("beginExpression");
            return null;
        }).when(mockDelegate).beginExpression(any(), anyBoolean());
        
        doAnswer(invocation -> {
            callbackTracker.record("endExpression");
            return null;
        }).when(mockDelegate).endExpression(any(), anyBoolean());
    }
    
    @Nested
    @DisplayName("parseTypeDef Integration Tests")
    class ParseTypeDefTests {
        
        @Test
        @DisplayName("Should integrate parseTypeDef with TYPE_BODY scope")
        void shouldIntegrateParseTypeDefWithTypeBodyScope() {
            // Setup token operations for class parsing
            when(mockTokenOps.peek()).thenReturn(mockClassToken);
            when(mockTokenOps.consume()).thenReturn(mockClassToken);
            
            // When
            ParseResult<ParsedNode> result = parser.parseTypeDef();
            
            // Then
            assertNotNull(result);
            
            // Verify TYPE_BODY scope was used
            assertTrue(callbackTracker.contains("beginTypeBody"));
            assertTrue(callbackTracker.contains("endTypeBody"));
            assertEquals(1, callbackTracker.count("beginTypeBody"));
            assertEquals(1, callbackTracker.count("endTypeBody"));
            
            // Verify callback sequence
            List<String> sequence = callbackTracker.getSequence();
            int beginIndex = sequence.indexOf("beginTypeBody");
            int endIndex = sequence.indexOf("endTypeBody");
            assertTrue(beginIndex < endIndex, "Begin should come before end");
        }
        
        @Test
        @DisplayName("Should handle parseTypeDef with nested scopes")
        void shouldHandleParseTypeDefWithNestedScopes() {
            // Setup for class with method
            when(mockTokenOps.peek())
                .thenReturn(mockClassToken)
                .thenReturn(mockMethodToken)
                .thenReturn(mockBlockToken);
            when(mockTokenOps.consume())
                .thenReturn(mockClassToken)
                .thenReturn(mockMethodToken)
                .thenReturn(mockBlockToken);
            
            // When
            ParseResult<ParsedNode> result = parser.parseTypeDef();
            
            // Then
            assertNotNull(result);
            
            // Should have proper nesting: TYPE_BODY contains METHOD_BODY
            assertTrue(callbackTracker.contains("beginTypeBody"));
            assertTrue(callbackTracker.contains("endTypeBody"));
            
            // Verify nested structure if methods are parsed within type
            List<String> sequence = callbackTracker.getSequence();
            if (sequence.contains("beginMethodBody")) {
                int typeBegin = sequence.indexOf("beginTypeBody");
                int methodBegin = sequence.indexOf("beginMethodBody");
                int methodEnd = sequence.indexOf("endMethodBody");
                int typeEnd = sequence.indexOf("endTypeBody");
                
                assertTrue(typeBegin < methodBegin, "Type should begin before method");
                assertTrue(methodBegin < methodEnd, "Method should be properly paired");
                assertTrue(methodEnd < typeEnd, "Method should end before type");
            }
        }
        
        @Test
        @DisplayName("Should handle parseTypeDef failure correctly")
        void shouldHandleParseTypeDefFailureCorrectly() {
            // Setup for parsing failure
            when(mockTokenOps.peek()).thenReturn(mockToken); // Unexpected token
            when(mockTokenOps.consume()).thenReturn(mockToken);
            
            // When
            ParseResult<ParsedNode> result = parser.parseTypeDef();
            
            // Then
            if (result.isFailure()) {
                // Even on failure, callbacks should be paired
                assertEquals(callbackTracker.count("beginTypeBody"), 
                           callbackTracker.count("endTypeBody"),
                           "Begin and end callbacks should be paired even on failure");
            }
        }
    }
    
    @Nested
    @DisplayName("processFunction Integration Tests")
    class ProcessFunctionTests {
        
        @Test
        @DisplayName("Should integrate processFunction with METHOD_BODY scope")
        void shouldIntegrateProcessFunctionWithMethodBodyScope() {
            // Setup for function parsing
            when(mockTokenOps.peek()).thenReturn(mockMethodToken);
            when(mockTokenOps.consume()).thenReturn(mockMethodToken);
            
            // When
            ParseResult<ParsedNode> result = parser.processFunction();
            
            // Then
            assertNotNull(result);
            
            // Verify METHOD_BODY scope was used
            assertTrue(callbackTracker.contains("beginMethodBody"));
            assertTrue(callbackTracker.contains("endMethodBody"));
            assertEquals(1, callbackTracker.count("beginMethodBody"));
            assertEquals(1, callbackTracker.count("endMethodBody"));
        }
        
        @Test
        @DisplayName("Should handle processFunction with parameter list")
        void shouldHandleProcessFunctionWithParameterList() {
            // Setup for function with parameters
            when(mockTokenOps.peek())
                .thenReturn(mockMethodToken)
                .thenReturn(mockToken); // parameter
            when(mockTokenOps.consume())
                .thenReturn(mockMethodToken)
                .thenReturn(mockToken);
            
            // When
            ParseResult<ParsedNode> result = parser.processFunction();
            
            // Then
            assertNotNull(result);
            
            // Should have METHOD_BODY scope
            assertTrue(callbackTracker.contains("beginMethodBody"));
            assertTrue(callbackTracker.contains("endMethodBody"));
            
            // May also have parameter-related scopes
            List<String> sequence = callbackTracker.getSequence();
            int methodBegin = sequence.indexOf("beginMethodBody");
            int methodEnd = sequence.indexOf("endMethodBody");
            assertTrue(methodBegin < methodEnd, "Method scope should be properly paired");
        }
        
        @Test
        @DisplayName("Should handle processFunction with body block")
        void shouldHandleProcessFunctionWithBodyBlock() {
            // Setup for function with body
            when(mockTokenOps.peek())
                .thenReturn(mockMethodToken)
                .thenReturn(mockBlockToken);
            when(mockTokenOps.consume())
                .thenReturn(mockMethodToken)
                .thenReturn(mockBlockToken);
            
            // When
            ParseResult<ParsedNode> result = parser.processFunction();
            
            // Then
            assertNotNull(result);
            
            // Should have both METHOD_BODY and potentially BLOCK scopes
            assertTrue(callbackTracker.contains("beginMethodBody"));
            assertTrue(callbackTracker.contains("endMethodBody"));
            
            // Verify proper nesting if block scope is created
            if (callbackTracker.contains("beginStmtblockBody")) {
                List<String> sequence = callbackTracker.getSequence();
                int methodBegin = sequence.indexOf("beginMethodBody");
                int blockBegin = sequence.indexOf("beginStmtblockBody");
                int blockEnd = sequence.indexOf("endStmtblockBody");
                int methodEnd = sequence.indexOf("endMethodBody");
                
                assertTrue(methodBegin < blockBegin, "Method should begin before block");
                assertTrue(blockBegin < blockEnd, "Block should be properly paired");
                assertTrue(blockEnd < methodEnd, "Block should end before method");
            }
        }
        
        @Test
        @DisplayName("Should handle processFunction failure with scope cleanup")
        void shouldHandleProcessFunctionFailureWithScopeCleanup() {
            // Setup for parsing failure
            when(mockTokenOps.peek()).thenReturn(mockToken); // Unexpected token
            when(mockTokenOps.consume()).thenReturn(mockToken);
            
            // When
            ParseResult<ParsedNode> result = parser.processFunction();
            
            // Then
            if (result.isFailure()) {
                // Callbacks should still be paired
                assertEquals(callbackTracker.count("beginMethodBody"), 
                           callbackTracker.count("endMethodBody"),
                           "Method body callbacks should be paired even on failure");
            }
        }
    }
    
    @Nested
    @DisplayName("parseStmtBlock Integration Tests")
    class ParseStmtBlockTests {
        
        @Test
        @DisplayName("Should integrate parseStmtBlock with BLOCK scope")
        void shouldIntegrateParseStmtBlockWithBlockScope() {
            // Setup for block parsing
            when(mockTokenOps.peek()).thenReturn(mockBlockToken);
            when(mockTokenOps.consume()).thenReturn(mockBlockToken);
            
            // When
            ParseResult<ParsedNode> result = parser.parseStmtBlock();
            
            // Then
            assertNotNull(result);
            
            // Verify BLOCK scope was used
            assertTrue(callbackTracker.contains("beginStmtblockBody"));
            assertTrue(callbackTracker.contains("endStmtblockBody"));
            assertEquals(1, callbackTracker.count("beginStmtblockBody"));
            assertEquals(1, callbackTracker.count("endStmtblockBody"));
        }
        
        @Test
        @DisplayName("Should handle parseStmtBlock with nested statements")
        void shouldHandleParseStmtBlockWithNestedStatements() {
            // Setup for block with statements
            when(mockTokenOps.peek())
                .thenReturn(mockBlockToken)
                .thenReturn(mockToken) // statement
                .thenReturn(mockToken); // another statement
            when(mockTokenOps.consume())
                .thenReturn(mockBlockToken)
                .thenReturn(mockToken)
                .thenReturn(mockToken);
            
            // When
            ParseResult<ParsedNode> result = parser.parseStmtBlock();
            
            // Then
            assertNotNull(result);
            
            // Should have BLOCK scope
            assertTrue(callbackTracker.contains("beginStmtblockBody"));
            assertTrue(callbackTracker.contains("endStmtblockBody"));
            
            // May have nested statement scopes
            if (callbackTracker.contains("beginElement")) {
                List<String> sequence = callbackTracker.getSequence();
                int blockBegin = sequence.indexOf("beginStmtblockBody");
                int blockEnd = sequence.lastIndexOf("endStmtblockBody");
                
                // All statement scopes should be within block scope
                for (int i = 0; i < sequence.size(); i++) {
                    if (sequence.get(i).equals("beginElement") || sequence.get(i).equals("endElement")) {
                        assertTrue(i > blockBegin && i < blockEnd, 
                            "Statement callbacks should be within block scope");
                    }
                }
            }
        }
        
        @Test
        @DisplayName("Should handle parseStmtBlock with nested expressions")
        void shouldHandleParseStmtBlockWithNestedExpressions() {
            // Setup for block with expressions
            when(mockTokenOps.peek())
                .thenReturn(mockBlockToken)
                .thenReturn(mockToken); // expression
            when(mockTokenOps.consume())
                .thenReturn(mockBlockToken)
                .thenReturn(mockToken);
            
            // When
            ParseResult<ParsedNode> result = parser.parseStmtBlock();
            
            // Then
            assertNotNull(result);
            
            // Should have BLOCK scope
            assertTrue(callbackTracker.contains("beginStmtblockBody"));
            assertTrue(callbackTracker.contains("endStmtblockBody"));
            
            // Verify proper nesting with expressions
            if (callbackTracker.contains("beginExpression")) {
                List<String> sequence = callbackTracker.getSequence();
                int blockBegin = sequence.indexOf("beginStmtblockBody");
                int exprBegin = sequence.indexOf("beginExpression");
                int exprEnd = sequence.indexOf("endExpression");
                int blockEnd = sequence.indexOf("endStmtblockBody");
                
                assertTrue(blockBegin < exprBegin, "Block should begin before expression");
                assertTrue(exprBegin < exprEnd, "Expression should be properly paired");
                assertTrue(exprEnd < blockEnd, "Expression should end before block");
            }
        }
        
        @Test
        @DisplayName("Should handle parseStmtBlock failure with scope cleanup")
        void shouldHandleParseStmtBlockFailureWithScopeCleanup() {
            // Setup for parsing failure
            when(mockTokenOps.peek()).thenReturn(mockToken); // Unexpected token (not {)
            when(mockTokenOps.consume()).thenReturn(mockToken);
            
            // When
            ParseResult<ParsedNode> result = parser.parseStmtBlock();
            
            // Then
            if (result.isFailure()) {
                // Callbacks should still be paired if scope was created
                assertEquals(callbackTracker.count("beginStmtblockBody"), 
                           callbackTracker.count("endStmtblockBody"),
                           "Block callbacks should be paired even on failure");
            }
        }
    }
    
    @Nested
    @DisplayName("Cross-Method Integration Tests")
    class CrossMethodIntegrationTests {
        
        @Test
        @DisplayName("Should handle class with method and block")
        void shouldHandleClassWithMethodAndBlock() {
            // Setup for complete class structure
            when(mockTokenOps.peek())
                .thenReturn(mockClassToken)
                .thenReturn(mockMethodToken)
                .thenReturn(mockBlockToken);
            when(mockTokenOps.consume())
                .thenReturn(mockClassToken)
                .thenReturn(mockMethodToken)
                .thenReturn(mockBlockToken);
            
            // When - simulate parsing class -> method -> block
            ParseResult<ParsedNode> classResult = parser.parseTypeDef();
            ParseResult<ParsedNode> methodResult = parser.processFunction();
            ParseResult<ParsedNode> blockResult = parser.parseStmtBlock();
            
            // Then
            assertNotNull(classResult);
            assertNotNull(methodResult);
            assertNotNull(blockResult);
            
            // Verify all scope types were used
            assertTrue(callbackTracker.contains("beginTypeBody"));
            assertTrue(callbackTracker.contains("beginMethodBody"));
            assertTrue(callbackTracker.contains("beginStmtblockBody"));
            assertTrue(callbackTracker.contains("endStmtblockBody"));
            assertTrue(callbackTracker.contains("endMethodBody"));
            assertTrue(callbackTracker.contains("endTypeBody"));
        }
        
        @Test
        @DisplayName("Should maintain scope isolation between methods")
        void shouldMaintainScopeIsolationBetweenMethods() {
            // When - parse multiple separate methods
            callbackTracker.reset();
            
            // First method
            when(mockTokenOps.peek()).thenReturn(mockMethodToken);
            when(mockTokenOps.consume()).thenReturn(mockMethodToken);
            ParseResult<ParsedNode> method1 = parser.processFunction();
            
            int firstMethodCallbacks = callbackTracker.getSequence().size();
            
            // Second method
            callbackTracker.reset();
            ParseResult<ParsedNode> method2 = parser.processFunction();
            
            // Then
            assertNotNull(method1);
            assertNotNull(method2);
            
            // Each method should have its own isolated scope
            assertTrue(callbackTracker.contains("beginMethodBody"));
            assertTrue(callbackTracker.contains("endMethodBody"));
            assertEquals(1, callbackTracker.count("beginMethodBody"));
            assertEquals(1, callbackTracker.count("endMethodBody"));
        }
        
        @Test
        @DisplayName("Should handle error propagation across method boundaries")
        void shouldHandleErrorPropagationAcrossMethodBoundaries() {
            // Setup for cascading failure
            when(mockTokenOps.peek()).thenReturn(mockToken); // Unexpected token
            when(mockTokenOps.consume()).thenReturn(mockToken);
            
            // When
            ParseResult<ParsedNode> typeResult = parser.parseTypeDef();
            ParseResult<ParsedNode> methodResult = parser.processFunction();
            ParseResult<ParsedNode> blockResult = parser.parseStmtBlock();
            
            // Then - even with failures, scopes should be properly managed
            // Each method should handle its own scope cleanup
            assertEquals(callbackTracker.count("beginTypeBody"), 
                       callbackTracker.count("endTypeBody"),
                       "Type body callbacks should be paired");
            assertEquals(callbackTracker.count("beginMethodBody"), 
                       callbackTracker.count("endMethodBody"),
                       "Method body callbacks should be paired");
            assertEquals(callbackTracker.count("beginStmtblockBody"), 
                       callbackTracker.count("endStmtblockBody"),
                       "Statement block callbacks should be paired");
        }
    }
    
    @Nested
    @DisplayName("Scope Metadata Integration Tests")
    class ScopeMetadataTests {
        
        @Test
        @DisplayName("Should track scope metadata correctly in parseTypeDef")
        void shouldTrackScopeMetadataCorrectlyInParseTypeDef() {
            // Setup
            when(mockTokenOps.peek()).thenReturn(mockClassToken);
            when(mockTokenOps.consume()).thenReturn(mockClassToken);
            
            // When
            ParseResult<ParsedNode> result = parser.parseTypeDef();
            
            // Then
            assertNotNull(result);
            
            // Verify that scope metadata is properly tracked
            // (This would be validated through the ScopeManager's debug info)
            ScopeDebugInfo debugInfo = parser.getScopeManager().getDebugInfo();
            assertNotNull(debugInfo);
            assertTrue(debugInfo.getTotalCreated() > 0);
            assertEquals(0, debugInfo.getCurrentDepth()); // All scopes should be closed
        }
        
        @Test
        @DisplayName("Should track scope positions correctly")
        void shouldTrackScopePositionsCorrectly() {
            // Setup with specific token positions
            LocatableToken positionToken = mock(LocatableToken.class);
            when(positionToken.getText()).thenReturn("test");
            when(positionToken.getType()).thenReturn(JavaTokenTypes.IDENTIFIER);
            when(positionToken.getLine()).thenReturn(42);
            when(positionToken.getColumn()).thenReturn(15);
            
            when(mockTokenOps.peek()).thenReturn(positionToken);
            when(mockTokenOps.consume()).thenReturn(positionToken);
            
            // When
            ParseResult<ParsedNode> result = parser.parseStmtBlock();
            
            // Then
            assertNotNull(result);
            
            // Verify position information was passed to callbacks
            verify(mockDelegate).beginStmtblockBody(positionToken);
            verify(mockDelegate).endStmtblockBody(positionToken, true);
        }
        
        @Test
        @DisplayName("Should handle scope name tracking")
        void shouldHandleScopeNameTracking() {
            // Setup with named tokens
            LocatableToken namedToken = mock(LocatableToken.class);
            when(namedToken.getText()).thenReturn("MyClass");
            when(namedToken.getType()).thenReturn(JavaTokenTypes.IDENTIFIER);
            when(namedToken.getLine()).thenReturn(1);
            when(namedToken.getColumn()).thenReturn(1);
            
            when(mockTokenOps.peek()).thenReturn(namedToken);
            when(mockTokenOps.consume()).thenReturn(namedToken);
            
            // When
            ParseResult<ParsedNode> result = parser.parseTypeDef();
            
            // Then
            assertNotNull(result);
            
            // Verify named token was used in callbacks
            verify(mockDelegate).beginTypeBody(namedToken);
            verify(mockDelegate).endTypeBody(namedToken, true);
        }
    }
    
    @Nested
    @DisplayName("Performance Integration Tests")
    class PerformanceIntegrationTests {
        
        @Test
        @DisplayName("Should maintain performance with scope overhead")
        void shouldMaintainPerformanceWithScopeOverhead() {
            // Setup for performance test
            when(mockTokenOps.peek()).thenReturn(mockMethodToken);
            when(mockTokenOps.consume()).thenReturn(mockMethodToken);
            
            // When - measure time for multiple method parses
            long startTime = System.nanoTime();
            
            for (int i = 0; i < 1000; i++) {
                callbackTracker.reset();
                ParseResult<ParsedNode> result = parser.processFunction();
                assertNotNull(result);
            }
            
            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;
            
            // Then - should complete within reasonable time
            assertTrue(durationMs < 1000, // Less than 1 second for 1000 parses
                "Scope overhead should not significantly impact performance: " + durationMs + "ms");
            
            // Verify all scopes were properly managed
            assertTrue(callbackTracker.contains("beginMethodBody"));
            assertTrue(callbackTracker.contains("endMethodBody"));
        }
        
        @Test
        @DisplayName("Should handle memory efficiency with repeated parsing")
        void shouldHandleMemoryEfficiencyWithRepeatedParsing() {
            // Setup
            when(mockTokenOps.peek()).thenReturn(mockBlockToken);
            when(mockTokenOps.consume()).thenReturn(mockBlockToken);
            
            // When - parse many blocks to test memory usage
            for (int i = 0; i < 10000; i++) {
                callbackTracker.reset();
                ParseResult<ParsedNode> result = parser.parseStmtBlock();
                assertNotNull(result);
                
                // Verify scope was properly cleaned up
                assertEquals(0, parser.getScopeManager().getCurrentDepth(),
                    "Scope stack should be empty after parsing");
            }
            
            // Then - no memory leaks should occur
            // (This would be validated through memory profiling in real scenarios)
            ScopeDebugInfo debugInfo = parser.getScopeManager().getDebugInfo();
            assertEquals(0, debugInfo.getCurrentDepth(), "No scopes should remain open");
            assertEquals(0, debugInfo.getMismatches(), "No scope mismatches should occur");
        }
    }
}