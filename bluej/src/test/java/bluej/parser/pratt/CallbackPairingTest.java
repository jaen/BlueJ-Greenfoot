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
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for callback pairing validation in ParseScope.
 * 
 * This test suite ensures that every begin callback has exactly one corresponding
 * end callback, maintaining the critical guarantee of callback pairing even during
 * error scenarios, exceptions, and complex nested parsing structures.
 * 
 * Coverage Areas:
 * - Basic callback pairing (1:1 begin/end ratio)
 * - Nested scope callback ordering (LIFO)
 * - Exception safety (callbacks paired even with exceptions)
 * - Error recovery (failed scopes still get end callbacks)
 * - Complex parsing scenarios
 * - Callback sequence validation
 * - Stack integrity during errors
 */
@DisplayName("Callback Pairing Validation Tests")
public class CallbackPairingTest {

    @Mock
    private CallbackDelegate mockDelegate;
    
    @Mock
    private LocatableToken mockToken;
    
    private ScopeManager scopeManager;
    private CallbackTracker callbackTracker;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup mock token
        when(mockToken.getText()).thenReturn("test");
        when(mockToken.getType()).thenReturn(JavaTokenTypes.IDENTIFIER);
        when(mockToken.getLine()).thenReturn(1);
        when(mockToken.getColumn()).thenReturn(1);
        
        // Create scope manager with debug mode for better error tracking
        scopeManager = new ScopeManager(mockDelegate, true);
        callbackTracker = new CallbackTracker();
        
        // Setup callback tracking
        setupCallbackTracking();
    }
    
    /**
     * Helper class to track callback invocations and validate pairing
     */
    private static class CallbackTracker {
        private final List<CallbackEvent> events = new ArrayList<>();
        private final AtomicInteger beginCount = new AtomicInteger(0);
        private final AtomicInteger endCount = new AtomicInteger(0);
        
        void recordBegin(String type, LocatableToken token) {
            events.add(new CallbackEvent("begin", type, token, beginCount.incrementAndGet()));
        }
        
        void recordEnd(String type, LocatableToken token, boolean included) {
            events.add(new CallbackEvent("end", type, token, endCount.incrementAndGet(), included));
        }
        
        List<CallbackEvent> getEvents() {
            return new ArrayList<>(events);
        }
        
        int getBeginCount() {
            return beginCount.get();
        }
        
        int getEndCount() {
            return endCount.get();
        }
        
        void reset() {
            events.clear();
            beginCount.set(0);
            endCount.set(0);
        }
        
        void validatePairing() {
            assertEquals(beginCount.get(), endCount.get(), 
                "Begin and end callback counts must match");
        }
        
        void validateLifoOrder() {
            // Verify that nested scopes follow LIFO (Last In, First Out) order
            List<String> beginStack = new ArrayList<>();
            
            for (CallbackEvent event : events) {
                if (event.phase.equals("begin")) {
                    beginStack.add(event.type);
                } else if (event.phase.equals("end")) {
                    assertFalse(beginStack.isEmpty(), 
                        "End callback without corresponding begin: " + event.type);
                    String lastBegin = beginStack.remove(beginStack.size() - 1);
                    assertEquals(lastBegin, event.type, 
                        "LIFO order violation: expected " + lastBegin + " but got " + event.type);
                }
            }
            
            assertTrue(beginStack.isEmpty(), 
                "Unmatched begin callbacks: " + beginStack);
        }
    }
    
    private static class CallbackEvent {
        final String phase;
        final String type;
        final LocatableToken token;
        final int sequence;
        final Boolean included;
        
        CallbackEvent(String phase, String type, LocatableToken token, int sequence) {
            this(phase, type, token, sequence, null);
        }
        
        CallbackEvent(String phase, String type, LocatableToken token, int sequence, Boolean included) {
            this.phase = phase;
            this.type = type;
            this.token = token;
            this.sequence = sequence;
            this.included = included;
        }
        
        @Override
        public String toString() {
            return String.format("%s_%s[%d]%s", phase, type, sequence, 
                included != null ? "(included=" + included + ")" : "");
        }
    }
    
    private void setupCallbackTracking() {
        // Track expression callbacks
        doAnswer(invocation -> {
            callbackTracker.recordBegin("expression", invocation.getArgument(0));
            return null;
        }).when(mockDelegate).beginExpression(any(), anyBoolean());
        
        doAnswer(invocation -> {
            callbackTracker.recordEnd("expression", invocation.getArgument(0), false);
            return null;
        }).when(mockDelegate).endExpression(any(), anyBoolean());
        
        // Track element callbacks (statements)
        doAnswer(invocation -> {
            callbackTracker.recordBegin("element", invocation.getArgument(0));
            return null;
        }).when(mockDelegate).beginElement(any());
        
        doAnswer(invocation -> {
            callbackTracker.recordEnd("element", invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(mockDelegate).endElement(any(), anyBoolean());
        
        // Track method body callbacks
        doAnswer(invocation -> {
            callbackTracker.recordBegin("methodBody", invocation.getArgument(0));
            return null;
        }).when(mockDelegate).beginMethodBody(any());
        
        doAnswer(invocation -> {
            callbackTracker.recordEnd("methodBody", invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(mockDelegate).endMethodBody(any(), anyBoolean());
        
        // Track type body callbacks
        doAnswer(invocation -> {
            callbackTracker.recordBegin("typeBody", invocation.getArgument(0));
            return null;
        }).when(mockDelegate).beginTypeBody(any());
        
        doAnswer(invocation -> {
            callbackTracker.recordEnd("typeBody", invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(mockDelegate).endTypeBody(any(), anyBoolean());
        
        // Track block callbacks
        doAnswer(invocation -> {
            callbackTracker.recordBegin("stmtblockBody", invocation.getArgument(0));
            return null;
        }).when(mockDelegate).beginStmtblockBody(any());
        
        doAnswer(invocation -> {
            callbackTracker.recordEnd("stmtblockBody", invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(mockDelegate).endStmtblockBody(any(), anyBoolean());
        
        // Track lambda body callbacks
        doAnswer(invocation -> {
            callbackTracker.recordBegin("lambdaBody", invocation.getArgument(0));
            return null;
        }).when(mockDelegate).beginLambdaBody(any());
        
        doAnswer(invocation -> {
            callbackTracker.recordEnd("lambdaBody", invocation.getArgument(0), false);
            return null;
        }).when(mockDelegate).endLambdaBody(any());
        
        // Track argument list callbacks
        doAnswer(invocation -> {
            callbackTracker.recordBegin("argumentList", invocation.getArgument(0));
            return null;
        }).when(mockDelegate).beginArgumentList(any());
        
        doAnswer(invocation -> {
            callbackTracker.recordEnd("argumentList", invocation.getArgument(0), false);
            return null;
        }).when(mockDelegate).endArgumentList(any());
    }
    
    @Nested
    @DisplayName("Basic Pairing Tests")
    class BasicPairingTests {
        
        @Test
        @DisplayName("Should guarantee 1:1 pairing for single scope")
        void shouldGuaranteeOneToOnePairingForSingleScope() {
            // When
            try (ParseScope scope = scopeManager.beginExpression(mockToken, false)) {
                // Scope is active
            }
            
            // Then
            callbackTracker.validatePairing();
            assertEquals(1, callbackTracker.getBeginCount());
            assertEquals(1, callbackTracker.getEndCount());
        }
        
        @Test
        @DisplayName("Should guarantee pairing for multiple sequential scopes")
        void shouldGuaranteePairingForMultipleSequentialScopes() {
            // When
            for (int i = 0; i < 5; i++) {
                try (ParseScope scope = scopeManager.beginExpression(mockToken, false)) {
                    // Each scope is properly closed
                }
            }
            
            // Then
            callbackTracker.validatePairing();
            assertEquals(5, callbackTracker.getBeginCount());
            assertEquals(5, callbackTracker.getEndCount());
        }
        
        @Test
        @DisplayName("Should guarantee pairing for different scope types")
        void shouldGuaranteePairingForDifferentScopeTypes() {
            // When
            try (ParseScope stmt = scopeManager.beginStatement(mockToken)) {
                // Statement scope
            }
            try (ParseScope expr = scopeManager.beginExpression(mockToken, false)) {
                // Expression scope
            }
            try (ParseScope method = scopeManager.beginMethodBody(mockToken)) {
                // Method body scope
            }
            
            // Then
            callbackTracker.validatePairing();
            assertEquals(3, callbackTracker.getBeginCount());
            assertEquals(3, callbackTracker.getEndCount());
        }
    }
    
    @Nested
    @DisplayName("Nested Scope Pairing Tests")
    class NestedScopePairingTests {
        
        @Test
        @DisplayName("Should maintain LIFO order for nested scopes")
        void shouldMaintainLifoOrderForNestedScopes() {
            // When
            try (ParseScope outer = scopeManager.beginStatement(mockToken)) {
                try (ParseScope middle = scopeManager.beginExpression(mockToken, false)) {
                    try (ParseScope inner = scopeManager.beginBlock(mockToken)) {
                        // All scopes active
                    }
                    // inner closed first (LIFO)
                }
                // middle closed second
            }
            // outer closed last
            
            // Then
            callbackTracker.validatePairing();
            callbackTracker.validateLifoOrder();
            assertEquals(3, callbackTracker.getBeginCount());
            assertEquals(3, callbackTracker.getEndCount());
        }
        
        @Test
        @DisplayName("Should handle deep nesting correctly")
        void shouldHandleDeepNestingCorrectly() {
            final int DEPTH = 10;
            
            // When - create deeply nested scopes
            List<ParseScope> scopes = new ArrayList<>();
            for (int i = 0; i < DEPTH; i++) {
                scopes.add(scopeManager.beginExpression(mockToken, false));
            }
            
            // Close in reverse order (LIFO)
            for (int i = DEPTH - 1; i >= 0; i--) {
                scopes.get(i).close();
            }
            
            // Then
            callbackTracker.validatePairing();
            callbackTracker.validateLifoOrder();
            assertEquals(DEPTH, callbackTracker.getBeginCount());
            assertEquals(DEPTH, callbackTracker.getEndCount());
        }
        
        @Test
        @DisplayName("Should handle mixed nested scope types")
        void shouldHandleMixedNestedScopeTypes() {
            // When
            try (ParseScope stmt = scopeManager.beginStatement(mockToken)) {
                try (ParseScope expr = scopeManager.beginExpression(mockToken, false)) {
                    try (ParseScope lambda = scopeManager.beginLambdaBody(mockToken, true)) {
                        try (ParseScope args = scopeManager.beginArgumentList(mockToken)) {
                            // All different scope types nested
                        }
                    }
                }
            }
            
            // Then
            callbackTracker.validatePairing();
            callbackTracker.validateLifoOrder();
            assertEquals(4, callbackTracker.getBeginCount());
            assertEquals(4, callbackTracker.getEndCount());
        }
    }
    
    @Nested
    @DisplayName("Exception Safety Tests")
    class ExceptionSafetyTests {
        
        @Test
        @DisplayName("Should maintain pairing when exception occurs in scope")
        void shouldMaintainPairingWhenExceptionOccursInScope() {
            // When/Then
            assertThrows(RuntimeException.class, () -> {
                try (ParseScope scope = scopeManager.beginExpression(mockToken, false)) {
                    throw new RuntimeException("Test exception");
                }
            });
            
            // Callbacks should still be paired
            callbackTracker.validatePairing();
            assertEquals(1, callbackTracker.getBeginCount());
            assertEquals(1, callbackTracker.getEndCount());
        }
        
        @Test
        @DisplayName("Should maintain pairing in nested scopes with exception")
        void shouldMaintainPairingInNestedScopesWithException() {
            // When/Then
            assertThrows(RuntimeException.class, () -> {
                try (ParseScope outer = scopeManager.beginStatement(mockToken)) {
                    try (ParseScope inner = scopeManager.beginExpression(mockToken, false)) {
                        throw new RuntimeException("Test exception");
                    }
                }
            });
            
            // All callbacks should still be paired
            callbackTracker.validatePairing();
            callbackTracker.validateLifoOrder();
            assertEquals(2, callbackTracker.getBeginCount());
            assertEquals(2, callbackTracker.getEndCount());
        }
        
        @Test
        @DisplayName("Should handle exception in middle of nested scopes")
        void shouldHandleExceptionInMiddleOfNestedScopes() {
            // When/Then
            assertThrows(RuntimeException.class, () -> {
                try (ParseScope outer = scopeManager.beginStatement(mockToken)) {
                    try (ParseScope middle = scopeManager.beginExpression(mockToken, false)) {
                        try (ParseScope inner = scopeManager.beginBlock(mockToken)) {
                            // Exception in innermost scope
                            throw new RuntimeException("Test exception");
                        }
                    }
                }
            });
            
            // All callbacks should be paired despite exception
            callbackTracker.validatePairing();
            callbackTracker.validateLifoOrder();
            assertEquals(3, callbackTracker.getBeginCount());
            assertEquals(3, callbackTracker.getEndCount());
        }
    }
    
    @Nested
    @DisplayName("Error Recovery Tests")
    class ErrorRecoveryTests {
        
        @Test
        @DisplayName("Should maintain pairing for failed scopes")
        void shouldMaintainPairingForFailedScopes() {
            // When
            try (ParseScope scope = scopeManager.beginStatement(mockToken)) {
                scope.markFailed();
                // Scope will be closed with included=false
            }
            
            // Then
            callbackTracker.validatePairing();
            assertEquals(1, callbackTracker.getBeginCount());
            assertEquals(1, callbackTracker.getEndCount());
            
            // Verify the end callback was called with included=false
            List<CallbackEvent> events = callbackTracker.getEvents();
            assertEquals(2, events.size());
            assertEquals("begin", events.get(0).phase);
            assertEquals("end", events.get(1).phase);
            assertEquals(false, events.get(1).included);
        }
        
        @Test
        @DisplayName("Should handle mixed success and failure states")
        void shouldHandleMixedSuccessAndFailureStates() {
            // When
            try (ParseScope successful = scopeManager.beginStatement(mockToken)) {
                try (ParseScope failed = scopeManager.beginExpression(mockToken, false)) {
                    failed.markFailed();
                    try (ParseScope alsoSuccessful = scopeManager.beginBlock(mockToken)) {
                        // Mixed success/failure states
                    }
                }
            }
            
            // Then
            callbackTracker.validatePairing();
            callbackTracker.validateLifoOrder();
            assertEquals(3, callbackTracker.getBeginCount());
            assertEquals(3, callbackTracker.getEndCount());
        }
        
        @Test
        @DisplayName("Should handle failure with exception")
        void shouldHandleFailureWithException() {
            // When/Then
            assertThrows(RuntimeException.class, () -> {
                try (ParseScope scope = scopeManager.beginStatement(mockToken)) {
                    scope.markFailed();
                    throw new RuntimeException("Test exception");
                }
            });
            
            // Callbacks should still be paired
            callbackTracker.validatePairing();
            assertEquals(1, callbackTracker.getBeginCount());
            assertEquals(1, callbackTracker.getEndCount());
            
            // Verify failed state was preserved
            List<CallbackEvent> events = callbackTracker.getEvents();
            assertEquals(false, events.get(1).included);
        }
    }
    
    @Nested
    @DisplayName("Complex Scenario Tests")
    class ComplexScenarioTests {
        
        @Test
        @DisplayName("Should handle realistic parsing scenario")
        void shouldHandleRealisticParsingScenario() {
            // Simulate parsing: class { method() { if (expr) { block } } }
            
            // When
            try (ParseScope classScope = scopeManager.beginTypeBody(mockToken)) {
                try (ParseScope methodScope = scopeManager.beginMethodBody(mockToken)) {
                    try (ParseScope ifStmt = scopeManager.beginStatement(mockToken)) {
                        try (ParseScope condition = scopeManager.beginExpression(mockToken, false)) {
                            // Condition expression
                        }
                        try (ParseScope thenBlock = scopeManager.beginBlock(mockToken)) {
                            // Then block
                        }
                    }
                }
            }
            
            // Then
            callbackTracker.validatePairing();
            callbackTracker.validateLifoOrder();
            assertEquals(5, callbackTracker.getBeginCount());
            assertEquals(5, callbackTracker.getEndCount());
        }
        
        @Test
        @DisplayName("Should handle lambda with argument list")
        void shouldHandleLambdaWithArgumentList() {
            // Simulate parsing: (args) -> { body }
            
            // When
            try (ParseScope lambda = scopeManager.beginLambdaBody(mockToken, true)) {
                try (ParseScope args = scopeManager.beginArgumentList(mockToken)) {
                    // Argument list
                }
                try (ParseScope body = scopeManager.beginBlock(mockToken)) {
                    // Lambda body
                }
            }
            
            // Then
            callbackTracker.validatePairing();
            callbackTracker.validateLifoOrder();
            assertEquals(3, callbackTracker.getBeginCount());
            assertEquals(3, callbackTracker.getEndCount());
        }
        
        @Test
        @DisplayName("Should handle error recovery in complex scenario")
        void shouldHandleErrorRecoveryInComplexScenario() {
            // When/Then
            assertThrows(RuntimeException.class, () -> {
                try (ParseScope outer = scopeManager.beginTypeBody(mockToken)) {
                    try (ParseScope method = scopeManager.beginMethodBody(mockToken)) {
                        method.markFailed(); // Method parsing failed
                        try (ParseScope stmt = scopeManager.beginStatement(mockToken)) {
                            try (ParseScope expr = scopeManager.beginExpression(mockToken, false)) {
                                throw new RuntimeException("Parse error");
                            }
                        }
                    }
                }
            });
            
            // All callbacks should still be paired
            callbackTracker.validatePairing();
            callbackTracker.validateLifoOrder();
            assertEquals(4, callbackTracker.getBeginCount());
            assertEquals(4, callbackTracker.getEndCount());
        }
    }
    
    @Nested
    @DisplayName("Callback Sequence Validation Tests")
    class CallbackSequenceValidationTests {
        
        @Test
        @DisplayName("Should maintain strict callback ordering")
        void shouldMaintainStrictCallbackOrdering() {
            // When
            try (ParseScope scope1 = scopeManager.beginStatement(mockToken)) {
                try (ParseScope scope2 = scopeManager.beginExpression(mockToken, false)) {
                    // Nested scopes
                }
            }
            
            // Then - verify exact sequence
            List<CallbackEvent> events = callbackTracker.getEvents();
            assertEquals(4, events.size());
            assertEquals("begin_element[1]", events.get(0).toString());
            assertEquals("begin_expression[2]", events.get(1).toString());
            assertEquals("end_expression[1](included=false)", events.get(2).toString());
            assertEquals("end_element[2](included=true)", events.get(3).toString());
        }
        
        @Test
        @DisplayName("Should track callback sequence numbers correctly")
        void shouldTrackCallbackSequenceNumbersCorrectly() {
            // When - create multiple scopes
            try (ParseScope s1 = scopeManager.beginStatement(mockToken)) {
                // First scope
            }
            try (ParseScope s2 = scopeManager.beginExpression(mockToken, false)) {
                // Second scope
            }
            try (ParseScope s3 = scopeManager.beginBlock(mockToken)) {
                // Third scope
            }
            
            // Then - verify sequence numbers
            List<CallbackEvent> events = callbackTracker.getEvents();
            assertEquals(6, events.size());
            
            // Begin callbacks: 1, 2, 3
            assertEquals(1, events.get(0).sequence);
            assertEquals(2, events.get(2).sequence);
            assertEquals(3, events.get(4).sequence);
            
            // End callbacks: 1, 2, 3
            assertEquals(1, events.get(1).sequence);
            assertEquals(2, events.get(3).sequence);
            assertEquals(3, events.get(5).sequence);
        }
    }
}