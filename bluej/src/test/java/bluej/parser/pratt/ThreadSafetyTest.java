package bluej.parser.pratt;

import bluej.parser.CallbackDelegate;
import bluej.parser.LocatableToken;
import bluej.parser.JavaTokenTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Thread safety tests for ParseScope and ScopeManager.
 * 
 * While the ParseScope is designed for single-threaded parser operation,
 * these tests validate that the implementation handles concurrent access
 * gracefully and maintains data integrity under stress conditions.
 * 
 * Coverage Areas:
 * - Synchronized access to scope stack
 * - Concurrent scope creation and destruction
 * - Thread-safe callback invocation
 * - Stack corruption prevention
 * - Memory visibility and consistency
 * - Stress testing under high concurrency
 * - Deadlock prevention
 * - Exception safety in concurrent scenarios
 */
@DisplayName("Thread Safety Tests")
public class ThreadSafetyTest {

    @Mock
    private CallbackDelegate mockDelegate;
    
    @Mock
    private LocatableToken mockToken;
    
    private ScopeManager scopeManager;
    private ExecutorService executorService;
    
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
        
        // Create thread pool for concurrent tests
        executorService = Executors.newFixedThreadPool(10);
    }
    
    @Nested
    @DisplayName("Basic Thread Safety Tests")
    class BasicThreadSafetyTests {
        
        @Test
        @DisplayName("Should handle concurrent scope creation safely")
        @Timeout(10)
        void shouldHandleConcurrentScopeCreationSafely() throws InterruptedException {
            final int THREAD_COUNT = 10;
            final int SCOPES_PER_THREAD = 100;
            final CountDownLatch startLatch = new CountDownLatch(1);
            final CountDownLatch completionLatch = new CountDownLatch(THREAD_COUNT);
            final AtomicInteger successCount = new AtomicInteger(0);
            final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
            
            // Create multiple threads that create and close scopes concurrently
            for (int i = 0; i < THREAD_COUNT; i++) {
                executorService.submit(() -> {
                    try {
                        startLatch.await(); // Wait for all threads to be ready
                        
                        for (int j = 0; j < SCOPES_PER_THREAD; j++) {
                            try (ParseScope scope = scopeManager.beginExpression(mockToken, false)) {
                                // Scope is active
                                assertNotNull(scope);
                                assertEquals(ScopeType.EXPRESSION, scope.getType());
                            }
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }
            
            // Start all threads simultaneously
            startLatch.countDown();
            
            // Wait for completion
            assertTrue(completionLatch.await(10, TimeUnit.SECONDS), 
                "Threads should complete within timeout");
            
            // Verify results
            assertTrue(exceptions.isEmpty(), 
                "No exceptions should occur: " + exceptions);
            assertEquals(THREAD_COUNT * SCOPES_PER_THREAD, successCount.get(),
                "All scope operations should succeed");
            
            // Verify callback pairing (should be called correct number of times)
            verify(mockDelegate, times(THREAD_COUNT * SCOPES_PER_THREAD))
                .beginExpression(any(), anyBoolean());
            verify(mockDelegate, times(THREAD_COUNT * SCOPES_PER_THREAD))
                .endExpression(any(), anyBoolean());
        }
        
        @Test
        @DisplayName("Should maintain stack integrity under concurrent access")
        @Timeout(10)
        void shouldMaintainStackIntegrityUnderConcurrentAccess() throws InterruptedException {
            final int THREAD_COUNT = 5;
            final CountDownLatch startLatch = new CountDownLatch(1);
            final CountDownLatch completionLatch = new CountDownLatch(THREAD_COUNT);
            final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
            
            for (int i = 0; i < THREAD_COUNT; i++) {
                final int threadId = i;
                executorService.submit(() -> {
                    try {
                        startLatch.await();
                        
                        // Each thread creates nested scopes
                        try (ParseScope outer = scopeManager.beginStatement(mockToken)) {
                            Thread.sleep(1); // Small delay to increase chance of interleaving
                            try (ParseScope inner = scopeManager.beginExpression(mockToken, false)) {
                                Thread.sleep(1);
                                // Verify scope properties
                                assertEquals(ScopeType.STATEMENT, outer.getType());
                                assertEquals(ScopeType.EXPRESSION, inner.getType());
                                assertTrue(inner.getDepth() > outer.getDepth());
                            }
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }
            
            startLatch.countDown();
            assertTrue(completionLatch.await(10, TimeUnit.SECONDS));
            
            assertTrue(exceptions.isEmpty(), 
                "Stack integrity should be maintained: " + exceptions);
        }
        
        @Test
        @DisplayName("Should handle concurrent scope failures safely")
        @Timeout(10)
        void shouldHandleConcurrentScopeFailuresSafely() throws InterruptedException {
            final int THREAD_COUNT = 8;
            final CountDownLatch startLatch = new CountDownLatch(1);
            final CountDownLatch completionLatch = new CountDownLatch(THREAD_COUNT);
            final AtomicInteger failedScopeCount = new AtomicInteger(0);
            final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
            
            for (int i = 0; i < THREAD_COUNT; i++) {
                final boolean shouldFail = (i % 2 == 0); // Half the threads fail their scopes
                executorService.submit(() -> {
                    try {
                        startLatch.await();
                        
                        try (ParseScope scope = scopeManager.beginStatement(mockToken)) {
                            if (shouldFail) {
                                scope.markFailed();
                                failedScopeCount.incrementAndGet();
                            }
                            Thread.sleep(1); // Small delay
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }
            
            startLatch.countDown();
            assertTrue(completionLatch.await(10, TimeUnit.SECONDS));
            
            assertTrue(exceptions.isEmpty(), 
                "Concurrent failures should be handled safely: " + exceptions);
            assertEquals(THREAD_COUNT / 2, failedScopeCount.get(),
                "Expected number of failed scopes");
        }
    }
    
    @Nested
    @DisplayName("Stress Testing")
    class StressTests {
        
        @RepeatedTest(5)
        @DisplayName("Should handle high-frequency scope operations")
        @Timeout(15)
        void shouldHandleHighFrequencyScopeOperations() throws InterruptedException {
            final int THREAD_COUNT = 20;
            final int OPERATIONS_PER_THREAD = 500;
            final CountDownLatch startLatch = new CountDownLatch(1);
            final CountDownLatch completionLatch = new CountDownLatch(THREAD_COUNT);
            final AtomicInteger totalOperations = new AtomicInteger(0);
            final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
            
            for (int i = 0; i < THREAD_COUNT; i++) {
                executorService.submit(() -> {
                    try {
                        startLatch.await();
                        
                        for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                            // Rapid scope creation and destruction
                            try (ParseScope scope = scopeManager.beginExpression(mockToken, false)) {
                                // Minimal work in scope
                                assertNotNull(scope.getType());
                            }
                            totalOperations.incrementAndGet();
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }
            
            startLatch.countDown();
            assertTrue(completionLatch.await(15, TimeUnit.SECONDS));
            
            assertTrue(exceptions.isEmpty(), 
                "High-frequency operations should not cause errors: " + exceptions);
            assertEquals(THREAD_COUNT * OPERATIONS_PER_THREAD, totalOperations.get());
        }
        
        @Test
        @DisplayName("Should handle mixed concurrent operations")
        @Timeout(10)
        void shouldHandleMixedConcurrentOperations() throws InterruptedException {
            final int THREAD_COUNT = 12;
            final CountDownLatch startLatch = new CountDownLatch(1);
            final CountDownLatch completionLatch = new CountDownLatch(THREAD_COUNT);
            final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
            
            for (int i = 0; i < THREAD_COUNT; i++) {
                final int threadType = i % 4; // 4 different operation types
                executorService.submit(() -> {
                    try {
                        startLatch.await();
                        
                        switch (threadType) {
                            case 0: // Simple scopes
                                for (int j = 0; j < 50; j++) {
                                    try (ParseScope scope = scopeManager.beginExpression(mockToken, false)) {
                                        // Simple operation
                                    }
                                }
                                break;
                                
                            case 1: // Nested scopes
                                for (int j = 0; j < 25; j++) {
                                    try (ParseScope outer = scopeManager.beginStatement(mockToken)) {
                                        try (ParseScope inner = scopeManager.beginExpression(mockToken, false)) {
                                            // Nested operation
                                        }
                                    }
                                }
                                break;
                                
                            case 2: // Failed scopes
                                for (int j = 0; j < 50; j++) {
                                    try (ParseScope scope = scopeManager.beginStatement(mockToken)) {
                                        scope.markFailed();
                                    }
                                }
                                break;
                                
                            case 3: // Mixed scope types
                                for (int j = 0; j < 20; j++) {
                                    try (ParseScope method = scopeManager.beginMethodBody(mockToken)) {
                                        try (ParseScope block = scopeManager.beginBlock(mockToken)) {
                                            try (ParseScope expr = scopeManager.beginExpression(mockToken, false)) {
                                                // Complex nested operation
                                            }
                                        }
                                    }
                                }
                                break;
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }
            
            startLatch.countDown();
            assertTrue(completionLatch.await(10, TimeUnit.SECONDS));
            
            assertTrue(exceptions.isEmpty(), 
                "Mixed operations should not cause errors: " + exceptions);
        }
    }
    
    @Nested
    @DisplayName("Exception Safety Under Concurrency")
    class ConcurrentExceptionSafetyTests {
        
        @Test
        @DisplayName("Should handle concurrent exceptions safely")
        @Timeout(10)
        void shouldHandleConcurrentExceptionsSafely() throws InterruptedException {
            final int THREAD_COUNT = 10;
            final CountDownLatch startLatch = new CountDownLatch(1);
            final CountDownLatch completionLatch = new CountDownLatch(THREAD_COUNT);
            final AtomicInteger exceptionCount = new AtomicInteger(0);
            final List<Exception> unexpectedExceptions = Collections.synchronizedList(new ArrayList<>());
            
            for (int i = 0; i < THREAD_COUNT; i++) {
                final boolean shouldThrow = (i % 3 == 0); // Some threads throw exceptions
                executorService.submit(() -> {
                    try {
                        startLatch.await();
                        
                        try {
                            try (ParseScope scope = scopeManager.beginStatement(mockToken)) {
                                if (shouldThrow) {
                                    throw new RuntimeException("Intentional test exception");
                                }
                                Thread.sleep(1);
                            }
                        } catch (RuntimeException e) {
                            if (e.getMessage().equals("Intentional test exception")) {
                                exceptionCount.incrementAndGet();
                            } else {
                                unexpectedExceptions.add(e);
                            }
                        }
                    } catch (Exception e) {
                        unexpectedExceptions.add(e);
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }
            
            startLatch.countDown();
            assertTrue(completionLatch.await(10, TimeUnit.SECONDS));
            
            assertTrue(unexpectedExceptions.isEmpty(), 
                "No unexpected exceptions should occur: " + unexpectedExceptions);
            
            // Verify that callbacks are still properly paired despite exceptions
            verify(mockDelegate, times(THREAD_COUNT)).beginElement(any());
            verify(mockDelegate, times(THREAD_COUNT)).endElement(any(), anyBoolean());
        }
        
        @Test
        @DisplayName("Should maintain consistency during concurrent failures")
        @Timeout(10)
        void shouldMaintainConsistencyDuringConcurrentFailures() throws InterruptedException {
            final int THREAD_COUNT = 8;
            final CountDownLatch startLatch = new CountDownLatch(1);
            final CountDownLatch completionLatch = new CountDownLatch(THREAD_COUNT);
            final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
            
            for (int i = 0; i < THREAD_COUNT; i++) {
                final int threadId = i;
                executorService.submit(() -> {
                    try {
                        startLatch.await();
                        
                        // Create nested scopes with various failure scenarios
                        try (ParseScope outer = scopeManager.beginStatement(mockToken)) {
                            if (threadId % 4 == 0) {
                                outer.markFailed();
                            }
                            
                            try (ParseScope inner = scopeManager.beginExpression(mockToken, false)) {
                                if (threadId % 4 == 1) {
                                    inner.markFailed();
                                }
                                
                                if (threadId % 4 == 2) {
                                    throw new RuntimeException("Test exception");
                                }
                                
                                Thread.sleep(1);
                            }
                        } catch (RuntimeException e) {
                            // Expected for some threads
                            if (!e.getMessage().equals("Test exception")) {
                                exceptions.add(e);
                            }
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }
            
            startLatch.countDown();
            assertTrue(completionLatch.await(10, TimeUnit.SECONDS));
            
            assertTrue(exceptions.isEmpty(), 
                "Consistency should be maintained during failures: " + exceptions);
        }
    }
    
    @Nested
    @DisplayName("Memory Consistency Tests")
    class MemoryConsistencyTests {
        
        @Test
        @DisplayName("Should ensure proper memory visibility")
        @Timeout(10)
        void shouldEnsureProperMemoryVisibility() throws InterruptedException {
            final int THREAD_COUNT = 6;
            final CountDownLatch startLatch = new CountDownLatch(1);
            final CountDownLatch completionLatch = new CountDownLatch(THREAD_COUNT);
            final AtomicReference<ParseScope> sharedScope = new AtomicReference<>();
            final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
            final List<ScopeType> observedTypes = Collections.synchronizedList(new ArrayList<>());
            
            // One thread creates a scope and stores it
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    ParseScope scope = scopeManager.beginStatement(mockToken);
                    sharedScope.set(scope);
                    Thread.sleep(10); // Give other threads time to observe
                    scope.close();
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    completionLatch.countDown();
                }
            });
            
            // Other threads observe the shared scope
            for (int i = 1; i < THREAD_COUNT; i++) {
                executorService.submit(() -> {
                    try {
                        startLatch.await();
                        Thread.sleep(5); // Wait for scope to be created
                        
                        ParseScope observed = sharedScope.get();
                        if (observed != null) {
                            observedTypes.add(observed.getType());
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }
            
            startLatch.countDown();
            assertTrue(completionLatch.await(10, TimeUnit.SECONDS));
            
            assertTrue(exceptions.isEmpty(), 
                "Memory visibility should not cause exceptions: " + exceptions);
            
            // All observing threads should see the same scope type
            for (ScopeType type : observedTypes) {
                assertEquals(ScopeType.STATEMENT, type, 
                    "All threads should observe consistent scope type");
            }
        }
        
        @Test
        @DisplayName("Should handle concurrent scope state changes")
        @Timeout(10)
        void shouldHandleConcurrentScopeStateChanges() throws InterruptedException {
            final int READER_COUNT = 5;
            final CountDownLatch startLatch = new CountDownLatch(1);
            final CountDownLatch completionLatch = new CountDownLatch(READER_COUNT + 1);
            final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
            final List<Boolean> failureStates = Collections.synchronizedList(new ArrayList<>());
            
            // Create a scope that will be shared
            ParseScope sharedScope = scopeManager.beginStatement(mockToken);
            
            // One thread modifies the scope state
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    Thread.sleep(5); // Let readers start observing
                    sharedScope.markFailed(); // Change state
                    Thread.sleep(5); // Let readers observe the change
                    sharedScope.close();
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    completionLatch.countDown();
                }
            });
            
            // Multiple threads read the scope state
            for (int i = 0; i < READER_COUNT; i++) {
                executorService.submit(() -> {
                    try {
                        startLatch.await();
                        
                        // Read state multiple times
                        for (int j = 0; j < 10; j++) {
                            failureStates.add(sharedScope.isFailed());
                            Thread.sleep(1);
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }
            
            startLatch.countDown();
            assertTrue(completionLatch.await(10, TimeUnit.SECONDS));
            
            assertTrue(exceptions.isEmpty(), 
                "Concurrent state changes should not cause exceptions: " + exceptions);
            
            // Should observe both false and true states (state transition)
            assertTrue(failureStates.contains(false), "Should observe initial false state");
            assertTrue(failureStates.contains(true), "Should observe changed true state");
        }
    }
    
    @Nested
    @DisplayName("Deadlock Prevention Tests")
    class DeadlockPreventionTests {
        
        @Test
        @DisplayName("Should not deadlock with nested concurrent operations")
        @Timeout(15)
        void shouldNotDeadlockWithNestedConcurrentOperations() throws InterruptedException {
            final int THREAD_COUNT = 10;
            final CountDownLatch startLatch = new CountDownLatch(1);
            final CountDownLatch completionLatch = new CountDownLatch(THREAD_COUNT);
            final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
            
            for (int i = 0; i < THREAD_COUNT; i++) {
                executorService.submit(() -> {
                    try {
                        startLatch.await();
                        
                        // Create deeply nested scopes that could potentially deadlock
                        try (ParseScope s1 = scopeManager.beginStatement(mockToken)) {
                            Thread.sleep(1);
                            try (ParseScope s2 = scopeManager.beginExpression(mockToken, false)) {
                                Thread.sleep(1);
                                try (ParseScope s3 = scopeManager.beginBlock(mockToken)) {
                                    Thread.sleep(1);
                                    try (ParseScope s4 = scopeManager.beginMethodBody(mockToken)) {
                                        Thread.sleep(1);
                                        // Deep nesting completed successfully
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }
            
            startLatch.countDown();
            
            // This test will timeout if deadlock occurs
            assertTrue(completionLatch.await(15, TimeUnit.SECONDS), 
                "Operations should complete without deadlock");
            
            assertTrue(exceptions.isEmpty(), 
                "No exceptions should occur during nested operations: " + exceptions);
        }
    }
}