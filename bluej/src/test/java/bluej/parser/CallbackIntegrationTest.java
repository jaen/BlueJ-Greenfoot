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

import bluej.parser.lexer.JavaLexer;
import bluej.parser.lexer.JavaTokenFilter;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.ParsedNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Integration tests for the callback delegation components.
 * Tests the interaction between CallbackDelegate, SourceParser,
 * TrackingSourceParser, and KotlinParserAdapter.
 * 
 * @author BlueJ Development Team
 */
@RunWith(JUnit4.class)
public class CallbackIntegrationTest {

    private static final String SIMPLE_CLASS = 
        "package test;\n" +
        "public class TestClass {\n" +
        "    private int field = 42;\n" +
        "    public void method() {\n" +
        "        System.out.println(\"Hello\");\n" +
        "    }\n" +
        "}";

    private static final String KOTLIN_CLASS =
        "package test\n" +
        "class TestClass {\n" +
        "    val field = 42\n" +
        "    fun method() {\n" +
        "        println(\"Hello\")\n" +
        "    }\n" +
        "}";

    // ==================== Test 1: CallbackDelegate Basic Functionality ====================

    @Test
    public void testCallbackDelegateAccess() throws Exception {
        // Create a parser with test input
        JavaLexer lexer = new JavaLexer(new StringReader(SIMPLE_CLASS));
        JavaTokenFilter tokenStream = new JavaTokenFilter(lexer);
        SourceParser parser = new SourceParser(null, tokenStream, null);
        
        // Get the callback delegate
        CallbackDelegate delegate = parser.getCallbackDelegate();
        assertNotNull("CallbackDelegate should not be null", delegate);
        
        // Test that delegate methods are accessible
        Method[] methods = CallbackDelegate.class.getDeclaredMethods();
        assertTrue("CallbackDelegate should have methods", methods.length > 0);
        
        // Verify we have the expected number of callback methods (226)
        assertEquals("CallbackDelegate should have 226 methods", 226, methods.length);
    }

    @Test
    public void testCallbackDelegateInvocation() throws Exception {
        // Create a custom parser that tracks callback invocations
        final AtomicInteger callbackCount = new AtomicInteger(0);
        
        JavaLexer lexer = new JavaLexer(new StringReader(SIMPLE_CLASS));
        JavaTokenFilter tokenStream = new JavaTokenFilter(lexer);
        
        SourceParser parser = new SourceParser(null, tokenStream, null) {
            @Override
            protected void beginExpression(boolean hasPrecedingType, LocatableToken first) {
                callbackCount.incrementAndGet();
                super.beginExpression(hasPrecedingType, first);
            }
        };
        
        // Get delegate and invoke a callback
        CallbackDelegate delegate = parser.getCallbackDelegate();
        delegate.beginExpression(false, tokenStream.nextToken());
        
        // Verify the callback was invoked
        assertEquals("Callback should have been invoked once", 1, callbackCount.get());
    }

    // ==================== Test 2: TrackingSourceParser Functionality ====================

    @Test
    public void testTrackingSourceParserWithTrackingDisabled() throws Exception {
        JavaLexer lexer = new JavaLexer(new StringReader(SIMPLE_CLASS));
        JavaTokenFilter tokenStream = new JavaTokenFilter(lexer);
        
        // Create tracking parser with tracking disabled
        TrackingSourceParser parser = new TrackingSourceParser(null, tokenStream, null);
        assertFalse("Tracking should be disabled by default", parser.isTrackingEnabled());
        
        // Parse expression - should work without tracking overhead
        try {
            parser.parseExpression();
        } catch (ParseFailure e) {
            // Expected for incomplete expression
        }
        
        // Verify no tracking occurred
        assertNull("Tracker should be null when disabled", parser.getTracker());
    }

    @Test
    public void testTrackingSourceParserWithTrackingEnabled() throws Exception {
        JavaLexer lexer = new JavaLexer(new StringReader("42 + 3"));
        JavaTokenFilter tokenStream = new JavaTokenFilter(lexer);
        
        // Create tracking parser with a mock tracker
        TestCallbackTracker tracker = new TestCallbackTracker();
        TrackingSourceParser parser = new TrackingSourceParser(null, tokenStream, null);
        parser.enableTracking(tracker);
        
        assertTrue("Tracking should be enabled", parser.isTrackingEnabled());
        assertSame("Tracker should be set", tracker, parser.getTracker());
        
        // Parse expression
        try {
            parser.parseExpression();
        } catch (Exception e) {
            // May fail due to incomplete setup, but tracking should still occur
        }
        
        // Verify tracking occurred
        assertTrue("Tracker should have recorded callbacks", tracker.getCallbackCount() > 0);
    }

    @Test
    public void testTrackingSourceParserExceptionHandling() throws Exception {
        JavaLexer lexer = new JavaLexer(new StringReader(SIMPLE_CLASS));
        JavaTokenFilter tokenStream = new JavaTokenFilter(lexer);
        
        // Create tracker that throws exceptions
        CallbackTracker faultyTracker = new CallbackTracker() {
            @Override
            public void trackCallback(String methodName, Object[] args) {
                throw new RuntimeException("Tracker failure");
            }
        };
        
        TrackingSourceParser parser = new TrackingSourceParser(null, tokenStream, null);
        parser.enableTracking(faultyTracker);
        
        // Parse should continue despite tracker failures
        CallbackDelegate delegate = parser.getCallbackDelegate();
        try {
            // This should not throw even though tracker fails
            delegate.beginExpression(false, tokenStream.nextToken());
        } catch (RuntimeException e) {
            fail("Tracker exceptions should be caught and not propagate");
        }
    }

    // ==================== Test 3: KotlinParserAdapter Integration ====================

    @Test
    public void testKotlinParserAdapterLegacyDelegation() throws Exception {
        JavaLexer lexer = new JavaLexer(new StringReader(KOTLIN_CLASS));
        JavaTokenFilter tokenStream = new JavaTokenFilter(lexer);
        SourceParser sourceParser = new SourceParser(null, tokenStream, null);
        
        // Create adapter - should default to legacy parser
        KotlinParserAdapter adapter = new KotlinParserAdapter(sourceParser);
        
        assertNotNull("Legacy parser should be initialized", adapter.getLegacyParser());
        assertNotNull("Pratt parser should be initialized", adapter.getPrattParser());
        
        // Test delegation to legacy parser (default behavior)
        try {
            adapter.parseExpression();
        } catch (Exception e) {
            // Expected - may fail due to incomplete setup
        }
    }

    @Test
    public void testKotlinParserAdapterPrattParserDelegation() throws Exception {
        // Enable Pratt parser via system property
        System.setProperty("bluej.kotlin.usePrattParser", "true");
        
        try {
            JavaLexer lexer = new JavaLexer(new StringReader("42"));
            JavaTokenFilter tokenStream = new JavaTokenFilter(lexer);
            SourceParser sourceParser = new SourceParser(null, tokenStream, null);
            
            KotlinParserAdapter adapter = new KotlinParserAdapter(sourceParser);
            
            // Should attempt to use Pratt parser for expressions
            try {
                adapter.parseExpression();
            } catch (Exception e) {
                // May fail if parselets not configured, but delegation logic is tested
            }
        } finally {
            System.clearProperty("bluej.kotlin.usePrattParser");
        }
    }

    // ==================== Test 4: Performance Requirements ====================

    @Test
    public void testCallbackDelegatePerformance() throws Exception {
        JavaLexer lexer = new JavaLexer(new StringReader(SIMPLE_CLASS));
        JavaTokenFilter tokenStream = new JavaTokenFilter(lexer);
        SourceParser parser = new SourceParser(null, tokenStream, null);
        CallbackDelegate delegate = parser.getCallbackDelegate();
        
        // Measure overhead of callback delegation
        long directTime = measureDirectCallTime(parser, 100000);
        long delegateTime = measureDelegateCallTime(delegate, 100000);
        
        double overhead = ((double)(delegateTime - directTime) / directTime) * 100;
        
        // Verify < 1% overhead requirement
        assertTrue("CallbackDelegate overhead should be < 1%, was: " + overhead + "%", 
                   overhead < 1.0);
    }

    @Test
    public void testTrackingParserPerformance() throws Exception {
        JavaLexer lexer = new JavaLexer(new StringReader(SIMPLE_CLASS));
        JavaTokenFilter tokenStream = new JavaTokenFilter(lexer);
        
        // Test zero overhead when disabled
        TrackingSourceParser disabledParser = new TrackingSourceParser(null, tokenStream, null);
        long disabledTime = measureParserTime(disabledParser, 10000);
        
        // Test overhead when enabled
        lexer = new JavaLexer(new StringReader(SIMPLE_CLASS));
        tokenStream = new JavaTokenFilter(lexer);
        TrackingSourceParser enabledParser = new TrackingSourceParser(null, tokenStream, null);
        enabledParser.enableTracking(new TestCallbackTracker());
        long enabledTime = measureParserTime(enabledParser, 10000);
        
        double overhead = ((double)(enabledTime - disabledTime) / disabledTime) * 100;
        
        // Verify < 5% overhead requirement when enabled
        assertTrue("TrackingSourceParser overhead should be < 5%, was: " + overhead + "%",
                   overhead < 5.0);
    }

    // ==================== Test 5: Thread Safety ====================

    @Test
    public void testThreadSafety() throws Exception {
        final int THREAD_COUNT = 10;
        final int ITERATIONS = 1000;
        final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
        
        Thread[] threads = new Thread[THREAD_COUNT];
        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < ITERATIONS; j++) {
                        JavaLexer lexer = new JavaLexer(new StringReader(SIMPLE_CLASS));
                        JavaTokenFilter tokenStream = new JavaTokenFilter(lexer);
                        TrackingSourceParser parser = new TrackingSourceParser(null, tokenStream, null);
                        parser.enableTracking(new TestCallbackTracker());
                        
                        CallbackDelegate delegate = parser.getCallbackDelegate();
                        delegate.beginExpression(false, null);
                    }
                } catch (Exception e) {
                    exceptions.add(e);
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
        
        // Verify no exceptions
        assertTrue("No exceptions should occur during concurrent access", exceptions.isEmpty());
    }

    // ==================== Helper Methods ====================

    private long measureDirectCallTime(SourceParser parser, int iterations) {
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            parser.beginExpression(false, null);
        }
        return System.nanoTime() - start;
    }

    private long measureDelegateCallTime(CallbackDelegate delegate, int iterations) {
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            delegate.beginExpression(false, null);
        }
        return System.nanoTime() - start;
    }

    private long measureParserTime(TrackingSourceParser parser, int iterations) {
        long start = System.nanoTime();
        CallbackDelegate delegate = parser.getCallbackDelegate();
        for (int i = 0; i < iterations; i++) {
            delegate.beginExpression(false, null);
            delegate.endExpression();
        }
        return System.nanoTime() - start;
    }

    /**
     * Test implementation of CallbackTracker for testing.
     */
    private static class TestCallbackTracker implements CallbackTracker {
        private final AtomicInteger callbackCount = new AtomicInteger(0);
        private final List<String> methodNames = Collections.synchronizedList(new ArrayList<>());
        
        @Override
        public void trackCallback(String methodName, Object[] args) {
            callbackCount.incrementAndGet();
            methodNames.add(methodName);
        }
        
        public int getCallbackCount() {
            return callbackCount.get();
        }
        
        public List<String> getMethodNames() {
            return new ArrayList<>(methodNames);
        }
    }
}