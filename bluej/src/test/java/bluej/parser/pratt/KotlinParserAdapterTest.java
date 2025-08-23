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

import bluej.Config;
import bluej.parser.SourceParser;
import bluej.parser.lexer.JavaTokenFilter;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.ParsedNode;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;
import java.util.List;

/**
 * Comprehensive unit tests for the KotlinParserAdapter class.
 *
 * <p>These tests verify that the adapter correctly delegates to the underlying
 * KotlinPrattParser while handling configuration and capability checking.</p>
 *
 * @author BlueJ Development Team
 * @since BlueJ 5.4.0
 */
public class KotlinParserAdapterTest extends TestCase {

    private KotlinParserAdapter adapter;
    private ParseletRegistry registry;
    private String originalConfigValue;

    @Before
    public void setUp() {
        // Save original configuration value
        originalConfigValue = System.getProperty("bluej.kotlin.usePrattParser");

        // Create a test parser setup
        String testSource = "val x = 42";
        SourceParser sourceParser = new SourceParser(new StringReader(testSource));
        JavaTokenFilter tokenStream = sourceParser.getTokenStream();

        // Create a custom registry for testing
        registry = new ParseletRegistry();

        // Create adapter with custom registry
        adapter = new KotlinParserAdapter(tokenStream, sourceParser, registry);
    }

    @After
    public void tearDown() {
        // Restore original configuration value
        if (originalConfigValue != null) {
            System.setProperty("bluej.kotlin.usePrattParser", originalConfigValue);
        } else {
            System.clearProperty("bluej.kotlin.usePrattParser");
        }

        // Clear other test properties
        System.clearProperty("bluej.kotlin.usePrattParser.statements");
        System.clearProperty("bluej.kotlin.usePrattParser.declarations");
    }

    /**
     * Test that the adapter correctly checks configuration for expression parsing.
     */
    @Test
    public void testCanParseExpressionConfiguration() {
        // Initially disabled
        System.clearProperty("bluej.kotlin.usePrattParser");
        assertFalse("Should not parse when config disabled", adapter.canParseExpression());

        // Enable configuration
        System.setProperty("bluej.kotlin.usePrattParser", "true");

        // Still false because no parselets registered
        assertFalse("Should not parse without parselets", adapter.canParseExpression());

        // Register a basic parselet
        registry.registerPrefix(JavaTokenTypes.NUM_INT, new TestPrefixParselet());

        // Now should be true
        assertTrue("Should parse when config enabled and parselets present",
                   adapter.canParseExpression());
    }

    /**
     * Test that the adapter correctly checks parser capabilities.
     */
    @Test
    public void testSupportsExpressionParsing() {
        // Initially no support
        assertFalse("Should not support without parselets",
                    adapter.supportsExpressionParsing());

        // Register literal parselets
        registry.registerPrefix(JavaTokenTypes.NUM_INT, new TestPrefixParselet());
        assertTrue("Should support with literal parselets",
                   adapter.supportsExpressionParsing());

        // Clear and register operator parselets
        registry.clear();
        registry.registerInfix(JavaTokenTypes.PLUS, new TestInfixParselet(), Precedence.SUM);
        assertTrue("Should support with operator parselets",
                   adapter.supportsExpressionParsing());

        // Clear and register identifier parselets
        registry.clear();
        registry.registerPrefix(JavaTokenTypes.IDENT, new TestPrefixParselet());
        assertTrue("Should support with identifier parselets",
                   adapter.supportsExpressionParsing());
    }

    /**
     * Test statement parsing configuration and capabilities.
     */
    @Test
    public void testCanParseStatement() {
        // Enable statement configuration
        System.setProperty("bluej.kotlin.usePrattParser.statements", "true");

        // Should still be false without expression support
        assertFalse("Statements need expression support", adapter.canParseStatement());

        // Add expression support
        registry.registerPrefix(JavaTokenTypes.NUM_INT, new TestPrefixParselet());

        // Still false without statement parselets
        assertFalse("Should not parse without statement parselets",
                    adapter.canParseStatement());

        // Add statement parselet
        registry.registerPrefix(JavaTokenTypes.LITERAL_if, new TestPrefixParselet());

        // Now should be true
        assertTrue("Should parse statements when fully configured",
                   adapter.canParseStatement());
    }

    /**
     * Test declaration parsing configuration and capabilities.
     */
    @Test
    public void testCanParseDeclaration() {
        // Enable declaration configuration
        System.setProperty("bluej.kotlin.usePrattParser.declarations", "true");

        // Should be false without declaration parselets
        assertFalse("Should not parse without declaration parselets",
                    adapter.canParseDeclaration());

        // Add declaration parselet
        registry.registerPrefix(JavaTokenTypes.LITERAL_class, new TestPrefixParselet());

        // Now should be true
        assertTrue("Should parse declarations when configured",
                   adapter.canParseDeclaration());
    }

    /**
     * Test that parseExpression returns null when not configured.
     */
    @Test
    public void testParseExpressionWhenDisabled() {
        // Ensure configuration is disabled
        System.clearProperty("bluej.kotlin.usePrattParser");

        ParsedNode result = adapter.parseExpression();
        assertNull("Should return null when parsing disabled", result);

        ParsedNode resultWithPrecedence = adapter.parseExpression(0);
        assertNull("Should return null when parsing disabled (with precedence)",
                   resultWithPrecedence);
    }

    /**
     * Test that parseStatement returns null when not configured.
     */
    @Test
    public void testParseStatementWhenDisabled() {
        // Ensure configuration is disabled
        System.clearProperty("bluej.kotlin.usePrattParser.statements");

        ParsedNode result = adapter.parseStatement();
        assertNull("Should return null when statement parsing disabled", result);
    }

    /**
     * Test that parseDeclaration returns null when not configured.
     */
    @Test
    public void testParseDeclarationWhenDisabled() {
        // Ensure configuration is disabled
        System.clearProperty("bluej.kotlin.usePrattParser.declarations");

        ParsedNode result = adapter.parseDeclaration();
        assertNull("Should return null when declaration parsing disabled", result);
    }

    /**
     * Test basic token operations delegation.
     */
    @Test
    public void testTokenOperations() {
        // Test peek
        LocatableToken token = adapter.peek();
        assertNotNull("Should peek at next token", token);

        // Test consume
        LocatableToken consumed = adapter.consume();
        assertNotNull("Should consume token", consumed);

        // Test check
        boolean checkResult = adapter.check(JavaTokenTypes.IDENT);
        // Result depends on actual tokens in stream

        // Test match
        boolean matchResult = adapter.match(JavaTokenTypes.IDENT);
        // Result depends on actual tokens in stream
    }

    /**
     * Test error handling delegation.
     */
    @Test
    public void testErrorHandling() {
        // Initially no errors
        assertFalse("Should have no errors initially", adapter.hasErrors());
        assertEquals("Error list should be empty", 0, adapter.getErrors().size());

        // Report an error
        adapter.error("Test error");

        // Now should have errors
        assertTrue("Should have errors after reporting", adapter.hasErrors());
        assertEquals("Should have one error", 1, adapter.getErrors().size());

        // Clear errors
        adapter.clearErrors();
        assertFalse("Should have no errors after clearing", adapter.hasErrors());
    }

    /**
     * Test synchronization operation delegation.
     */
    @Test
    public void testSynchronize() {
        // Should not throw exception
        adapter.synchronize();
        // Actual behavior depends on token stream state
    }

    /**
     * Test registry access.
     */
    @Test
    public void testGetRegistry() {
        ParseletRegistry retrievedRegistry = adapter.getRegistry();
        assertNotNull("Should return registry", retrievedRegistry);
        assertSame("Should return same registry instance", registry, retrievedRegistry);
    }

    /**
     * Test source parser access.
     */
    @Test
    public void testGetSourceParser() {
        SourceParser sourceParser = adapter.getSourceParser();
        assertNotNull("Should return source parser", sourceParser);
    }

    /**
     * Test end-of-stream checking.
     */
    @Test
    public void testIsAtEnd() {
        // Consume all tokens
        while (!adapter.isAtEnd()) {
            adapter.consume();
        }

        assertTrue("Should be at end after consuming all tokens", adapter.isAtEnd());
    }

    /**
     * Test compilation unit parsing delegation.
     */
    @Test
    public void testParseCompilationUnit() {
        // This would need proper setup with compilation unit parselets
        ParsedNode result = adapter.parseCompilationUnit();
        // Result depends on registered parselets and configuration
    }

    /**
     * Test that configuration changes take effect immediately.
     */
    @Test
    public void testConfigurationChanges() {
        // Register parselets
        registry.registerPrefix(JavaTokenTypes.NUM_INT, new TestPrefixParselet());

        // Initially disabled
        System.clearProperty("bluej.kotlin.usePrattParser");
        assertFalse("Should be disabled initially", adapter.canParseExpression());

        // Enable
        System.setProperty("bluej.kotlin.usePrattParser", "true");
        assertTrue("Should be enabled after config change", adapter.canParseExpression());

        // Disable again
        System.setProperty("bluej.kotlin.usePrattParser", "false");
        assertFalse("Should be disabled after config change", adapter.canParseExpression());
    }

    /**
     * Test multiple parselet type checking.
     */
    @Test
    public void testMixedParseletSupport() {
        // Add different types of parselets
        registry.registerPrefix(JavaTokenTypes.NUM_INT, new TestPrefixParselet());
        registry.registerInfix(JavaTokenTypes.PLUS, new TestInfixParselet(), Precedence.SUM);
        registry.registerPrefix(JavaTokenTypes.LITERAL_if, new TestPrefixParselet());
        registry.registerPrefix(JavaTokenTypes.LITERAL_class, new TestPrefixParselet());

        // Enable all configurations
        System.setProperty("bluej.kotlin.usePrattParser", "true");
        System.setProperty("bluej.kotlin.usePrattParser.statements", "true");
        System.setProperty("bluej.kotlin.usePrattParser.declarations", "true");

        // All should be supported
        assertTrue("Should support expressions", adapter.canParseExpression());
        assertTrue("Should support statements", adapter.canParseStatement());
        assertTrue("Should support declarations", adapter.canParseDeclaration());
    }

    // Test helper classes

    /**
     * Simple test implementation of PrefixParselet.
     */
    private static class TestPrefixParselet implements PrefixParselet {
        @Override
        public ParsedNode parse(KotlinPrattParser parser, LocatableToken token) {
            return null; // Simple stub implementation
        }

        @Override
        public boolean canParse(LocatableToken token) {
            return true;
        }

        @Override
        public int getPriority() {
            return 0;
        }
    }

    /**
     * Simple test implementation of InfixParselet.
     */
    private static class TestInfixParselet implements InfixParselet {
        @Override
        public ParsedNode parse(KotlinPrattParser parser, ParsedNode left, LocatableToken token) {
            return null; // Simple stub implementation
        }

        @Override
        public int getLeftBindingPower() {
            return Precedence.SUM.getLeftBindingPower();
        }

        @Override
        public boolean canParse(ParsedNode left, LocatableToken token) {
            return true;
        }

        @Override
        public boolean isRightAssociative() {
            return false;
        }

        @Override
        public int getPriority() {
            return 0;
        }
    }
}
