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
package bluej.parser.pratt;

import bluej.extensions2.SourceType;
import bluej.parser.SourceParser;
import bluej.parser.lexer.JavaTokenFilter;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.pratt.InfixParselet;
import bluej.parser.pratt.PrefixParselet;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit tests for the ParseletRegistry class.
 *
 * <p>These tests validate the registry's ability to manage parselets, including
 * registration, retrieval, precedence management, parent delegation, and
 * thread-safe operations.</p>
 *
 * @author BlueJ Development Team
 * @since BlueJ 5.4.0
 */
public class ParseletRegistryTest {

    /**
     * Mock prefix parselet for testing.
     */
    private static class MockPrefixParselet implements PrefixParselet {
        private final String name;

        MockPrefixParselet(String name) {
            this.name = name;
        }

        @Override
        public ParseResult<ParsedNode> parse(KotlinPrattParser parser, LocatableToken token) {
            return ParseResult.success(null); // Mock implementation
        }

        @Override
        public String getName() {
            return name;
        }
    }

    /**
     * Mock infix parselet for testing.
     */
    private static class MockInfixParselet implements InfixParselet {
        private final String name;
        private final int precedence;

        MockInfixParselet(String name, int precedence) {
            this.name = name;
            this.precedence = precedence;
        }

        @Override
        public int getPrecedence() {
            return precedence;
        }

        @Override
        public ParseResult<ParsedNode> parse(KotlinPrattParser parser, ParsedNode left, LocatableToken token) {
            return ParseResult.success(null); // Mock implementation
        }

        @Override
        public String getName() {
            return name;
        }
    }

    // ========== Basic Registration Tests ==========

    /**
     * Test basic prefix parselet registration.
     */
    @Test
    public void testPrefixRegistration() {
        ParseletRegistry registry = new ParseletRegistry();
        PrefixParselet parselet = new MockPrefixParselet("test");

        assertNull("Should return null for non-existent parselet",
                  registry.getPrefix(JavaTokenTypes.IDENT));

        PrefixParselet previous = registry.register(JavaTokenTypes.IDENT, parselet);
        assertNull("Should return null when registering first parselet", previous);

        PrefixParselet retrieved = registry.getPrefix(JavaTokenTypes.IDENT);
        assertSame("Should retrieve registered parselet", parselet, retrieved);
    }

    /**
     * Test basic infix parselet registration.
     */
    @Test
    public void testInfixRegistration() {
        ParseletRegistry registry = new ParseletRegistry();
        InfixParselet parselet = new MockInfixParselet("plus", 10);

        assertNull("Should return null for non-existent parselet",
                  registry.getInfix(JavaTokenTypes.PLUS));

        InfixParselet previous = registry.register(JavaTokenTypes.PLUS, parselet);
        assertNull("Should return null when registering first parselet", previous);

        InfixParselet retrieved = registry.getInfix(JavaTokenTypes.PLUS);
        assertSame("Should retrieve registered parselet", parselet, retrieved);
    }

    /**
     * Test replacing existing parselets.
     */
    @Test
    public void testParseletReplacement() {
        ParseletRegistry registry = new ParseletRegistry();
        PrefixParselet parselet1 = new MockPrefixParselet("first");
        PrefixParselet parselet2 = new MockPrefixParselet("second");

        registry.register(JavaTokenTypes.IDENT, parselet1);
        PrefixParselet previous = registry.register(JavaTokenTypes.IDENT, parselet2);

        assertSame("Should return previous parselet", parselet1, previous);
        assertSame("Should retrieve new parselet", parselet2,
                  registry.getPrefix(JavaTokenTypes.IDENT));
    }

    /**
     * Test null parselet rejection.
     */
    @Test
    public void testNullParseletRejection() {
        ParseletRegistry registry = new ParseletRegistry();

        try {
            registry.register(JavaTokenTypes.IDENT, (PrefixParselet) null);
            fail("Should throw NullPointerException for null prefix parselet");
        } catch (NullPointerException e) {
            assertTrue("Exception message should mention parselet",
                      e.getMessage().toLowerCase().contains("parselet"));
        }

        try {
            registry.register(JavaTokenTypes.PLUS, (InfixParselet) null);
            fail("Should throw NullPointerException for null infix parselet");
        } catch (NullPointerException e) {
            assertTrue("Exception message should mention parselet",
                      e.getMessage().toLowerCase().contains("parselet"));
        }
    }

    // ========== Precedence Tests ==========

    /**
     * Test precedence retrieval for infix parselets.
     */
    @Test
    public void testPrecedenceRetrieval() {
        ParseletRegistry registry = new ParseletRegistry();

        assertEquals("Should return 0 for non-existent parselet",
                    0, registry.getPrecedence(JavaTokenTypes.PLUS));

        InfixParselet plusParselet = new MockInfixParselet("plus", 10);
        registry.register(JavaTokenTypes.PLUS, plusParselet);

        assertEquals("Should return parselet's precedence",
                    10, registry.getPrecedence(JavaTokenTypes.PLUS));

        InfixParselet multiplyParselet = new MockInfixParselet("multiply", 20);
        registry.register(JavaTokenTypes.STAR, multiplyParselet);

        assertEquals("Should return correct precedence for multiply",
                    20, registry.getPrecedence(JavaTokenTypes.STAR));
    }

    // ========== Query Methods Tests ==========

    /**
     * Test hasPrefix method.
     */
    @Test
    public void testHasPrefix() {
        ParseletRegistry registry = new ParseletRegistry();

        assertFalse("Should return false for non-existent prefix",
                   registry.hasPrefix(JavaTokenTypes.IDENT));

        registry.register(JavaTokenTypes.IDENT, new MockPrefixParselet("ident"));

        assertTrue("Should return true for registered prefix",
                  registry.hasPrefix(JavaTokenTypes.IDENT));
        assertFalse("Should return false for other token types",
                   registry.hasPrefix(JavaTokenTypes.PLUS));
    }

    /**
     * Test hasInfix method.
     */
    @Test
    public void testHasInfix() {
        ParseletRegistry registry = new ParseletRegistry();

        assertFalse("Should return false for non-existent infix",
                   registry.hasInfix(JavaTokenTypes.PLUS));

        registry.register(JavaTokenTypes.PLUS, new MockInfixParselet("plus", 10));

        assertTrue("Should return true for registered infix",
                  registry.hasInfix(JavaTokenTypes.PLUS));
        assertFalse("Should return false for other token types",
                   registry.hasInfix(JavaTokenTypes.IDENT));
    }

    /**
     * Test count methods.
     */
    @Test
    public void testCounts() {
        ParseletRegistry registry = new ParseletRegistry();

        assertEquals("Should have 0 prefix parselets initially", 0, registry.getPrefixCount());
        assertEquals("Should have 0 infix parselets initially", 0, registry.getInfixCount());

        registry.register(JavaTokenTypes.IDENT, new MockPrefixParselet("ident"));
        assertEquals("Should have 1 prefix parselet", 1, registry.getPrefixCount());
        assertEquals("Should still have 0 infix parselets", 0, registry.getInfixCount());

        registry.register(JavaTokenTypes.PLUS, new MockInfixParselet("plus", 10));
        assertEquals("Should still have 1 prefix parselet", 1, registry.getPrefixCount());
        assertEquals("Should have 1 infix parselet", 1, registry.getInfixCount());

        registry.register(JavaTokenTypes.LITERAL_int, new MockPrefixParselet("int"));
        registry.register(JavaTokenTypes.MINUS, new MockInfixParselet("minus", 10));
        assertEquals("Should have 2 prefix parselets", 2, registry.getPrefixCount());
        assertEquals("Should have 2 infix parselets", 2, registry.getInfixCount());
    }

    /**
     * Test token type retrieval.
     */
    @Test
    public void testTokenTypeRetrieval() {
        ParseletRegistry registry = new ParseletRegistry();

        registry.register(JavaTokenTypes.IDENT, new MockPrefixParselet("ident"));
        registry.register(JavaTokenTypes.LITERAL_int, new MockPrefixParselet("int"));
        registry.register(JavaTokenTypes.PLUS, new MockInfixParselet("plus", 10));
        registry.register(JavaTokenTypes.MINUS, new MockInfixParselet("minus", 10));

        Set<Integer> prefixTypes = registry.getPrefixTokenTypes();
        assertEquals("Should have 2 prefix token types", 2, prefixTypes.size());
        assertTrue("Should contain IDENT", prefixTypes.contains(JavaTokenTypes.IDENT));
        assertTrue("Should contain LITERAL_int", prefixTypes.contains(JavaTokenTypes.LITERAL_int));

        Set<Integer> infixTypes = registry.getInfixTokenTypes();
        assertEquals("Should have 2 infix token types", 2, infixTypes.size());
        assertTrue("Should contain PLUS", infixTypes.contains(JavaTokenTypes.PLUS));
        assertTrue("Should contain MINUS", infixTypes.contains(JavaTokenTypes.MINUS));
    }

    // ========== Unregistration Tests ==========

    /**
     * Test parselet unregistration.
     */
    @Test
    public void testUnregistration() {
        ParseletRegistry registry = new ParseletRegistry();
        PrefixParselet prefixParselet = new MockPrefixParselet("test");
        InfixParselet infixParselet = new MockInfixParselet("plus", 10);

        registry.register(JavaTokenTypes.IDENT, prefixParselet);
        registry.register(JavaTokenTypes.PLUS, infixParselet);

        PrefixParselet removedPrefix = registry.unregisterPrefix(JavaTokenTypes.IDENT);
        assertSame("Should return removed prefix parselet", prefixParselet, removedPrefix);
        assertNull("Should no longer have prefix parselet", registry.getPrefix(JavaTokenTypes.IDENT));

        InfixParselet removedInfix = registry.unregisterInfix(JavaTokenTypes.PLUS);
        assertSame("Should return removed infix parselet", infixParselet, removedInfix);
        assertNull("Should no longer have infix parselet", registry.getInfix(JavaTokenTypes.PLUS));

        assertNull("Should return null when removing non-existent prefix",
                  registry.unregisterPrefix(JavaTokenTypes.IDENT));
        assertNull("Should return null when removing non-existent infix",
                  registry.unregisterInfix(JavaTokenTypes.PLUS));
    }

    /**
     * Test clearing registry.
     */
    @Test
    public void testClear() {
        ParseletRegistry registry = new ParseletRegistry();

        registry.register(JavaTokenTypes.IDENT, new MockPrefixParselet("ident"));
        registry.register(JavaTokenTypes.LITERAL_int, new MockPrefixParselet("int"));
        registry.register(JavaTokenTypes.PLUS, new MockInfixParselet("plus", 10));
        registry.register(JavaTokenTypes.MINUS, new MockInfixParselet("minus", 10));

        assertEquals("Should have 2 prefix parselets", 2, registry.getPrefixCount());
        assertEquals("Should have 2 infix parselets", 2, registry.getInfixCount());

        registry.clear();

        assertEquals("Should have 0 prefix parselets after clear", 0, registry.getPrefixCount());
        assertEquals("Should have 0 infix parselets after clear", 0, registry.getInfixCount());
        assertNull("Should not find prefix parselets after clear",
                  registry.getPrefix(JavaTokenTypes.IDENT));
        assertNull("Should not find infix parselets after clear",
                  registry.getInfix(JavaTokenTypes.PLUS));
    }

    // ========== Parent Registry Tests ==========

    /**
     * Test parent registry delegation for prefix parselets.
     */
    @Test
    public void testParentDelegationPrefix() {
        ParseletRegistry parent = new ParseletRegistry();
        ParseletRegistry child = new ParseletRegistry(parent);

        PrefixParselet parentParselet = new MockPrefixParselet("parent");
        parent.register(JavaTokenTypes.IDENT, parentParselet);

        PrefixParselet retrieved = child.getPrefix(JavaTokenTypes.IDENT);
        assertSame("Child should retrieve parent's parselet", parentParselet, retrieved);
        assertTrue("Child should report having prefix via parent",
                  child.hasPrefix(JavaTokenTypes.IDENT));

        PrefixParselet childParselet = new MockPrefixParselet("child");
        child.register(JavaTokenTypes.IDENT, childParselet);

        retrieved = child.getPrefix(JavaTokenTypes.IDENT);
        assertSame("Child should prefer its own parselet", childParselet, retrieved);

        retrieved = parent.getPrefix(JavaTokenTypes.IDENT);
        assertSame("Parent should still have its parselet", parentParselet, retrieved);
    }

    /**
     * Test parent registry delegation for infix parselets.
     */
    @Test
    public void testParentDelegationInfix() {
        ParseletRegistry parent = new ParseletRegistry();
        ParseletRegistry child = new ParseletRegistry(parent);

        InfixParselet parentParselet = new MockInfixParselet("parent", 10);
        parent.register(JavaTokenTypes.PLUS, parentParselet);

        InfixParselet retrieved = child.getInfix(JavaTokenTypes.PLUS);
        assertSame("Child should retrieve parent's parselet", parentParselet, retrieved);
        assertEquals("Child should retrieve parent's precedence",
                    10, child.getPrecedence(JavaTokenTypes.PLUS));
        assertTrue("Child should report having infix via parent",
                  child.hasInfix(JavaTokenTypes.PLUS));

        InfixParselet childParselet = new MockInfixParselet("child", 20);
        child.register(JavaTokenTypes.PLUS, childParselet);

        retrieved = child.getInfix(JavaTokenTypes.PLUS);
        assertSame("Child should prefer its own parselet", childParselet, retrieved);
        assertEquals("Child should use its own precedence",
                    20, child.getPrecedence(JavaTokenTypes.PLUS));
    }

    // ========== Copy and Merge Tests ==========

    /**
     * Test registry copying.
     */
    @Test
    public void testCopy() {
        ParseletRegistry original = new ParseletRegistry();
        PrefixParselet prefixParselet = new MockPrefixParselet("test");
        InfixParselet infixParselet = new MockInfixParselet("plus", 10);

        original.register(JavaTokenTypes.IDENT, prefixParselet);
        original.register(JavaTokenTypes.PLUS, infixParselet);

        ParseletRegistry copy = original.copy();

        assertNotSame("Copy should be different instance", original, copy);
        assertSame("Copy should have same prefix parselet",
                  prefixParselet, copy.getPrefix(JavaTokenTypes.IDENT));
        assertSame("Copy should have same infix parselet",
                  infixParselet, copy.getInfix(JavaTokenTypes.PLUS));

        // Modify copy shouldn't affect original
        copy.register(JavaTokenTypes.MINUS, new MockInfixParselet("minus", 10));
        assertNull("Original should not have new parselet",
                  original.getInfix(JavaTokenTypes.MINUS));
    }

    /**
     * Test registry copying with parent.
     */
    @Test
    public void testCopyWithParent() {
        ParseletRegistry parent = new ParseletRegistry();
        ParseletRegistry original = new ParseletRegistry(parent);

        parent.register(JavaTokenTypes.LITERAL_int, new MockPrefixParselet("int"));
        original.register(JavaTokenTypes.IDENT, new MockPrefixParselet("ident"));

        ParseletRegistry copy = original.copy();

        assertTrue("Copy should have access to parent parselet",
                  copy.hasPrefix(JavaTokenTypes.LITERAL_int));
        assertTrue("Copy should have its own parselet",
                  copy.hasPrefix(JavaTokenTypes.IDENT));
    }

    /**
     * Test registry merging.
     */
    @Test
    public void testMerge() {
        ParseletRegistry registry1 = new ParseletRegistry();
        ParseletRegistry registry2 = new ParseletRegistry();

        PrefixParselet prefix1 = new MockPrefixParselet("registry1");
        InfixParselet infix1 = new MockInfixParselet("plus1", 10);
        registry1.register(JavaTokenTypes.IDENT, prefix1);
        registry1.register(JavaTokenTypes.PLUS, infix1);

        PrefixParselet prefix2 = new MockPrefixParselet("registry2");
        InfixParselet infix2 = new MockInfixParselet("minus2", 15);
        registry2.register(JavaTokenTypes.LITERAL_int, prefix2);
        registry2.register(JavaTokenTypes.MINUS, infix2);

        registry1.merge(registry2);

        assertSame("Should have original prefix parselet", prefix1, registry1.getPrefix(JavaTokenTypes.IDENT));
        assertSame("Should have merged prefix parselet", prefix2, registry1.getPrefix(JavaTokenTypes.LITERAL_int));
        assertSame("Should have original infix parselet", infix1, registry1.getInfix(JavaTokenTypes.PLUS));
        assertSame("Should have merged infix parselet", infix2, registry1.getInfix(JavaTokenTypes.MINUS));
    }

    /**
     * Test merging with overwrites.
     */
    @Test
    public void testMergeWithOverwrites() {
        ParseletRegistry registry1 = new ParseletRegistry();
        ParseletRegistry registry2 = new ParseletRegistry();

        PrefixParselet prefix1 = new MockPrefixParselet("original");
        PrefixParselet prefix2 = new MockPrefixParselet("replacement");

        registry1.register(JavaTokenTypes.IDENT, prefix1);
        registry2.register(JavaTokenTypes.IDENT, prefix2);

        registry1.merge(registry2);

        assertSame("Merge should overwrite existing parselet",
                  prefix2, registry1.getPrefix(JavaTokenTypes.IDENT));
    }

    /**
     * Test null merge rejection.
     */
    @Test
    public void testNullMergeRejection() {
        ParseletRegistry registry = new ParseletRegistry();

        try {
            registry.merge(null);
            fail("Should throw NullPointerException for null merge");
        } catch (NullPointerException e) {
            assertTrue("Exception message should mention registry",
                      e.getMessage().toLowerCase().contains("registry"));
        }
    }

    // ========== Thread Safety Tests ==========

    /**
     * Test concurrent registration from multiple threads.
     */
    @Test
    public void testConcurrentRegistration() throws InterruptedException {
        final ParseletRegistry registry = new ParseletRegistry();
        final int threadCount = 10;
        final int registrationsPerThread = 100;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < registrationsPerThread; j++) {
                        int tokenType = threadId * 1000 + j;
                        PrefixParselet parselet = new MockPrefixParselet("thread" + threadId + "_" + j);
                        registry.register(tokenType, parselet);
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start all threads
        assertTrue("All threads should complete", endLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals("All registrations should succeed",
                    threadCount * registrationsPerThread, successCount.get());
        assertEquals("Registry should contain all prefix parselets",
                    threadCount * registrationsPerThread, registry.getPrefixCount());
    }

    /**
     * Test concurrent reads while writing.
     */
    @Test
    public void testConcurrentReadWrite() throws InterruptedException {
        final ParseletRegistry registry = new ParseletRegistry();
        final int iterations = 1000;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(2);
        final AtomicInteger readCount = new AtomicInteger(0);

        // Pre-populate some parselets
        for (int i = 0; i < 100; i++) {
            registry.register(i, new MockPrefixParselet("initial" + i));
        }

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Writer thread
        executor.submit(() -> {
            try {
                startLatch.await();
                for (int i = 100; i < 100 + iterations; i++) {
                    registry.register(i, new MockPrefixParselet("written" + i));
                    Thread.yield(); // Give reader a chance
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                endLatch.countDown();
            }
        });

        // Reader thread
        executor.submit(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < iterations; i++) {
                    // Read random parselets
                    int tokenType = i % 200;
                    PrefixParselet parselet = registry.getPrefix(tokenType);
                    if (parselet != null) {
                        readCount.incrementAndGet();
                    }
                    Thread.yield(); // Give writer a chance
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                endLatch.countDown();
            }
        });

        startLatch.countDown(); // Start both threads
        assertTrue("Both threads should complete", endLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertTrue("Reader should have successfully read parselets", readCount.get() > 0);
        assertEquals("Registry should contain all parselets",
                    100 + iterations, registry.getPrefixCount());
    }

    // ========== toString Tests ==========

    /**
     * Test toString representation.
     */
    public void testToString() {
        ParseletRegistry registry = new ParseletRegistry();
        String str = registry.toString();

        assertTrue("Should contain class name", str.contains("ParseletRegistry"));
        assertTrue("Should show prefix count", str.contains("prefix=0"));
        assertTrue("Should show infix count", str.contains("infix=0"));
        assertTrue("Should show no parent", str.contains("parent=none"));

        registry.register(JavaTokenTypes.IDENT, new MockPrefixParselet("test"));
        registry.register(JavaTokenTypes.PLUS, new MockInfixParselet("plus", 10));

        str = registry.toString();
        assertTrue("Should show prefix count", str.contains("prefix=1"));
        assertTrue("Should show infix count", str.contains("infix=1"));

        ParseletRegistry parent = new ParseletRegistry();
        ParseletRegistry child = new ParseletRegistry(parent);
        str = child.toString();
        assertTrue("Should show parent present", str.contains("parent=present"));
    }
}
