package bluej.parser.pratt;

import bluej.parser.CallbackDelegate;
import bluej.parser.LocatableToken;
import bluej.parser.JavaTokenTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for ParseScope core functionality.
 * 
 * Tests the AutoCloseable scope management system that ensures automatic
 * callback pairing in the KotlinParser through try-with-resources pattern.
 * 
 * Coverage Areas:
 * - ParseScope lifecycle (creation, usage, cleanup)
 * - AutoCloseable contract compliance
 * - Scope type and metadata management
 * - Failure state tracking
 * - Token and depth information
 * - Double-close protection
 */
@DisplayName("ParseScope Core Functionality Tests")
public class ParseScopeTest {

    @Mock
    private CallbackDelegate mockDelegate;
    
    @Mock
    private LocatableToken mockToken;
    
    private ScopeManager scopeManager;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup mock token
        when(mockToken.getText()).thenReturn("test");
        when(mockToken.getType()).thenReturn(JavaTokenTypes.IDENTIFIER);
        when(mockToken.getLine()).thenReturn(1);
        when(mockToken.getColumn()).thenReturn(1);
        
        // Create scope manager with debug mode enabled for testing
        scopeManager = new ScopeManager(mockDelegate, true);
    }
    
    @Nested
    @DisplayName("ParseScope Lifecycle Tests")
    class LifecycleTests {
        
        @Test
        @DisplayName("Should create scope with correct initial state")
        void shouldCreateScopeWithCorrectInitialState() {
            // When
            ParseScope scope = scopeManager.beginExpression(mockToken, false);
            
            // Then
            assertNotNull(scope);
            assertEquals(ScopeType.EXPRESSION, scope.getType());
            assertEquals(mockToken, scope.getToken());
            assertEquals(1, scope.getDepth()); // First scope at depth 1
            assertFalse(scope.isFailed());
            
            // Verify callback was invoked
            verify(mockDelegate).beginExpression(mockToken, false);
            
            // Cleanup
            scope.close();
        }
        
        @Test
        @DisplayName("Should properly close scope and invoke end callback")
        void shouldProperlyCloseScopeAndInvokeEndCallback() {
            // Given
            ParseScope scope = scopeManager.beginExpression(mockToken, false);
            
            // When
            scope.close();
            
            // Then
            verify(mockDelegate).beginExpression(mockToken, false);
            verify(mockDelegate).endExpression(mockToken, false);
        }
        
        @Test
        @DisplayName("Should work correctly with try-with-resources")
        void shouldWorkCorrectlyWithTryWithResources() {
            // When
            try (ParseScope scope = scopeManager.beginExpression(mockToken, false)) {
                // Verify scope is active
                assertNotNull(scope);
                assertEquals(ScopeType.EXPRESSION, scope.getType());
                assertFalse(scope.isFailed());
            }
            
            // Then - callbacks should be properly paired
            verify(mockDelegate).beginExpression(mockToken, false);
            verify(mockDelegate).endExpression(mockToken, false);
        }
        
        @Test
        @DisplayName("Should handle exception in try-with-resources")
        void shouldHandleExceptionInTryWithResources() {
            // When/Then
            assertThrows(RuntimeException.class, () -> {
                try (ParseScope scope = scopeManager.beginExpression(mockToken, false)) {
                    // Verify scope is active
                    assertNotNull(scope);
                    throw new RuntimeException("Test exception");
                }
            });
            
            // Callbacks should still be properly paired even with exception
            verify(mockDelegate).beginExpression(mockToken, false);
            verify(mockDelegate).endExpression(mockToken, false);
        }
        
        @Test
        @DisplayName("Should prevent double-close")
        void shouldPreventDoubleClose() {
            // Given
            ParseScope scope = scopeManager.beginExpression(mockToken, false);
            
            // When
            scope.close();
            scope.close(); // Second close should be safe
            
            // Then - end callback should only be called once
            verify(mockDelegate, times(1)).beginExpression(mockToken, false);
            verify(mockDelegate, times(1)).endExpression(mockToken, false);
        }
    }
    
    @Nested
    @DisplayName("Scope Type Tests")
    class ScopeTypeTests {
        
        @Test
        @DisplayName("Should create expression scope correctly")
        void shouldCreateExpressionScopeCorrectly() {
            // When
            ParseScope scope = scopeManager.beginExpression(mockToken, false);
            
            // Then
            assertEquals(ScopeType.EXPRESSION, scope.getType());
            verify(mockDelegate).beginExpression(mockToken, false);
            
            scope.close();
            verify(mockDelegate).endExpression(mockToken, false);
        }
        
        @Test
        @DisplayName("Should create statement scope correctly")
        void shouldCreateStatementScopeCorrectly() {
            // When
            ParseScope scope = scopeManager.beginStatement(mockToken);
            
            // Then
            assertEquals(ScopeType.STATEMENT, scope.getType());
            verify(mockDelegate).beginElement(mockToken);
            
            scope.close();
            verify(mockDelegate).endElement(mockToken, true); // included=true by default
        }
        
        @Test
        @DisplayName("Should create method body scope correctly")
        void shouldCreateMethodBodyScopeCorrectly() {
            // When
            ParseScope scope = scopeManager.beginMethodBody(mockToken);
            
            // Then
            assertEquals(ScopeType.METHOD_BODY, scope.getType());
            verify(mockDelegate).beginMethodBody(mockToken);
            
            scope.close();
            verify(mockDelegate).endMethodBody(mockToken, true);
        }
        
        @Test
        @DisplayName("Should create type body scope correctly")
        void shouldCreateTypeBodyScopeCorrectly() {
            // When
            ParseScope scope = scopeManager.beginTypeBody(mockToken);
            
            // Then
            assertEquals(ScopeType.TYPE_BODY, scope.getType());
            verify(mockDelegate).beginTypeBody(mockToken);
            
            scope.close();
            verify(mockDelegate).endTypeBody(mockToken, true);
        }
        
        @Test
        @DisplayName("Should create block scope correctly")
        void shouldCreateBlockScopeCorrectly() {
            // When
            ParseScope scope = scopeManager.beginBlock(mockToken);
            
            // Then
            assertEquals(ScopeType.BLOCK, scope.getType());
            verify(mockDelegate).beginStmtblockBody(mockToken);
            
            scope.close();
            verify(mockDelegate).endStmtblockBody(mockToken, true);
        }
        
        @Test
        @DisplayName("Should create lambda body scope correctly")
        void shouldCreateLambdaBodyScopeCorrectly() {
            // When
            ParseScope scope = scopeManager.beginLambdaBody(mockToken, true);
            
            // Then
            assertEquals(ScopeType.LAMBDA_BODY, scope.getType());
            verify(mockDelegate).beginLambdaBody(mockToken);
            
            scope.close();
            verify(mockDelegate).endLambdaBody(mockToken);
        }
        
        @Test
        @DisplayName("Should create argument list scope correctly")
        void shouldCreateArgumentListScopeCorrectly() {
            // When
            ParseScope scope = scopeManager.beginArgumentList(mockToken);
            
            // Then
            assertEquals(ScopeType.ARGUMENT_LIST, scope.getType());
            verify(mockDelegate).beginArgumentList(mockToken);
            
            scope.close();
            verify(mockDelegate).endArgumentList(mockToken);
        }
    }
    
    @Nested
    @DisplayName("Failure State Tests")
    class FailureStateTests {
        
        @Test
        @DisplayName("Should track failure state correctly")
        void shouldTrackFailureStateCorrectly() {
            // Given
            ParseScope scope = scopeManager.beginStatement(mockToken);
            assertFalse(scope.isFailed());
            
            // When
            scope.markFailed();
            
            // Then
            assertTrue(scope.isFailed());
        }
        
        @Test
        @DisplayName("Should pass included=false when scope is failed")
        void shouldPassIncludedFalseWhenScopeIsFailed() {
            // Given
            ParseScope scope = scopeManager.beginStatement(mockToken);
            scope.markFailed();
            
            // When
            scope.close();
            
            // Then
            verify(mockDelegate).beginElement(mockToken);
            verify(mockDelegate).endElement(mockToken, false); // included=false for failed scope
        }
        
        @Test
        @DisplayName("Should pass included=true when scope is not failed")
        void shouldPassIncludedTrueWhenScopeIsNotFailed() {
            // Given
            ParseScope scope = scopeManager.beginStatement(mockToken);
            // Don't mark as failed
            
            // When
            scope.close();
            
            // Then
            verify(mockDelegate).beginElement(mockToken);
            verify(mockDelegate).endElement(mockToken, true); // included=true for successful scope
        }
        
        @Test
        @DisplayName("Should handle failure in try-with-resources")
        void shouldHandleFailureInTryWithResources() {
            // When
            try (ParseScope scope = scopeManager.beginStatement(mockToken)) {
                scope.markFailed();
                // Scope will be automatically closed with failed state
            }
            
            // Then
            verify(mockDelegate).beginElement(mockToken);
            verify(mockDelegate).endElement(mockToken, false); // included=false
        }
    }
    
    @Nested
    @DisplayName("Nested Scope Tests")
    class NestedScopeTests {
        
        @Test
        @DisplayName("Should handle nested scopes with correct depths")
        void shouldHandleNestedScopesWithCorrectDepths() {
            // When
            try (ParseScope outerScope = scopeManager.beginStatement(mockToken)) {
                assertEquals(1, outerScope.getDepth());
                
                try (ParseScope innerScope = scopeManager.beginExpression(mockToken, false)) {
                    assertEquals(2, innerScope.getDepth());
                    
                    try (ParseScope deepScope = scopeManager.beginBlock(mockToken)) {
                        assertEquals(3, deepScope.getDepth());
                    }
                }
            }
            
            // Verify all callbacks were invoked in correct order
            verify(mockDelegate).beginElement(mockToken);
            verify(mockDelegate).beginExpression(mockToken, false);
            verify(mockDelegate).beginStmtblockBody(mockToken);
            verify(mockDelegate).endStmtblockBody(mockToken, true);
            verify(mockDelegate).endExpression(mockToken, false);
            verify(mockDelegate).endElement(mockToken, true);
        }
        
        @Test
        @DisplayName("Should maintain LIFO order for nested scopes")
        void shouldMaintainLifoOrderForNestedScopes() {
            // Given
            ParseScope scope1 = scopeManager.beginStatement(mockToken);
            ParseScope scope2 = scopeManager.beginExpression(mockToken, false);
            ParseScope scope3 = scopeManager.beginBlock(mockToken);
            
            // When - close in LIFO order
            scope3.close();
            scope2.close();
            scope1.close();
            
            // Then - verify callback order
            var inOrder = inOrder(mockDelegate);
            inOrder.verify(mockDelegate).beginElement(mockToken);
            inOrder.verify(mockDelegate).beginExpression(mockToken, false);
            inOrder.verify(mockDelegate).beginStmtblockBody(mockToken);
            inOrder.verify(mockDelegate).endStmtblockBody(mockToken, true);
            inOrder.verify(mockDelegate).endExpression(mockToken, false);
            inOrder.verify(mockDelegate).endElement(mockToken, true);
        }
    }
    
    @Nested
    @DisplayName("Metadata Tests")
    class MetadataTests {
        
        @Test
        @DisplayName("Should preserve token information")
        void shouldPreserveTokenInformation() {
            // When
            ParseScope scope = scopeManager.beginExpression(mockToken, false);
            
            // Then
            assertEquals(mockToken, scope.getToken());
            assertEquals("test", scope.getToken().getText());
            assertEquals(JavaTokenTypes.IDENTIFIER, scope.getToken().getType());
        }
        
        @Test
        @DisplayName("Should handle null token gracefully")
        void shouldHandleNullTokenGracefully() {
            // When
            ParseScope scope = scopeManager.beginExpression(null, false);
            
            // Then
            assertNull(scope.getToken());
            
            // Should still work correctly
            scope.close();
            verify(mockDelegate).beginExpression(null, false);
            verify(mockDelegate).endExpression(null, false);
        }
        
        @Test
        @DisplayName("Should provide meaningful toString representation")
        void shouldProvideMeaningfulToStringRepresentation() {
            // When
            ParseScope scope = scopeManager.beginExpression(mockToken, false);
            String toString = scope.toString();
            
            // Then
            assertNotNull(toString);
            assertTrue(toString.contains("EXPRESSION"));
            assertTrue(toString.contains("depth=1"));
            assertTrue(toString.contains("failed=false"));
            assertTrue(toString.contains("test"));
            
            scope.close();
        }
    }
    
    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {
        
        @Test
        @DisplayName("Should handle rapid scope creation and destruction")
        void shouldHandleRapidScopeCreationAndDestruction() {
            // When - create and close many scopes rapidly
            for (int i = 0; i < 100; i++) {
                try (ParseScope scope = scopeManager.beginExpression(mockToken, false)) {
                    assertEquals(ScopeType.EXPRESSION, scope.getType());
                }
            }
            
            // Then - all callbacks should be properly paired
            verify(mockDelegate, times(100)).beginExpression(mockToken, false);
            verify(mockDelegate, times(100)).endExpression(mockToken, false);
        }
        
        @Test
        @DisplayName("Should handle mixed scope types")
        void shouldHandleMixedScopeTypes() {
            // When
            try (ParseScope stmt = scopeManager.beginStatement(mockToken)) {
                try (ParseScope expr = scopeManager.beginExpression(mockToken, false)) {
                    try (ParseScope block = scopeManager.beginBlock(mockToken)) {
                        try (ParseScope lambda = scopeManager.beginLambdaBody(mockToken, true)) {
                            // All scopes active
                            assertEquals(ScopeType.STATEMENT, stmt.getType());
                            assertEquals(ScopeType.EXPRESSION, expr.getType());
                            assertEquals(ScopeType.BLOCK, block.getType());
                            assertEquals(ScopeType.LAMBDA_BODY, lambda.getType());
                        }
                    }
                }
            }
            
            // Verify all callbacks were invoked
            verify(mockDelegate).beginElement(mockToken);
            verify(mockDelegate).beginExpression(mockToken, false);
            verify(mockDelegate).beginStmtblockBody(mockToken);
            verify(mockDelegate).beginLambdaBody(mockToken);
            verify(mockDelegate).endLambdaBody(mockToken);
            verify(mockDelegate).endStmtblockBody(mockToken, true);
            verify(mockDelegate).endExpression(mockToken, false);
            verify(mockDelegate).endElement(mockToken, true);
        }
    }
}