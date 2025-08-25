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
import bluej.parser.lexer.LocatableToken;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.nodes.ParsedNode;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.List;

/**
 * Unit tests for the KotlinPrattParser main coordinator class.
 *
 * <p>These tests validate the core functionality of the Pratt parser including
 * token stream management, expression parsing, error handling, and parser
 * synchronization.</p>
 *
 * @author BlueJ Development Team
 * @since BlueJ 5.4.0
 */
public class KotlinPrattParserTest {

    /**
     * Creates a KotlinPrattParser for the given source string.
     *
     * @param source The source code to parse
     * @return A configured KotlinPrattParser instance
     */
    private KotlinPrattParser createParser(String source) {
        SourceParser sourceParser = new SourceParser(new StringReader(source), SourceType.Kotlin);
        JavaTokenFilter tokenStream = sourceParser.getTokenStream();
        // Create test token operations that directly delegate to tokenStream
        TokenOperations tokenOps = new TestTokenOperations(tokenStream);
        // Create test node factory for AST node creation
        NodeFactory nodeFactory = new TestNodeFactory();
        return new KotlinPrattParser(tokenOps, sourceParser, nodeFactory);
    }

    /**
     * Creates a KotlinPrattParser with a custom registry for testing.
     *
     * @param source The source code to parse
     * @param registry The custom parselet registry
     * @return A configured KotlinPrattParser instance
     */
    private KotlinPrattParser createParserWithRegistry(String source, ParseletRegistry registry) {
        SourceParser sourceParser = new SourceParser(new StringReader(source), SourceType.Kotlin);
        JavaTokenFilter tokenStream = sourceParser.getTokenStream();
        TokenOperations tokenOps = new TestTokenOperations(tokenStream);
        // Create test node factory for AST node creation
        NodeFactory nodeFactory = new TestNodeFactory();
        return new KotlinPrattParser(tokenOps, sourceParser, nodeFactory, registry);
    }

    // ========== Parser Initialization Tests ==========

    /**
     * Test that the parser can be created with default registry.
     */
    @Test
    public void testParserCreation() {
        KotlinPrattParser parser = createParser("val x = 42");
        assertNotNull("Parser should be created", parser);
        assertNotNull("Parser should have a registry", parser.getRegistry());
        assertNotNull("Parser should have source parser", parser.getSourceParser());
        assertFalse("Parser should not have errors initially", parser.hasErrors());
        assertEquals("Parser should have no errors initially", 0, parser.getErrors().size());
    }

    /**
     * Test that the parser can be created with custom registry.
     */
    @Test
    public void testParserCreationWithCustomRegistry() {
        ParseletRegistry customRegistry = new ParseletRegistry();
        KotlinPrattParser parser = createParserWithRegistry("val x = 42", customRegistry);
        assertNotNull("Parser should be created", parser);
        assertSame("Parser should use custom registry", customRegistry, parser.getRegistry());
    }

    // ========== Token Stream Management Tests ==========

    /**
     * Test basic token consumption.
     */
    @Test
    public void testTokenConsumption() {
        KotlinPrattParser parser = createParser("val x = 42");

        LocatableToken token = parser.consume();
        assertNotNull("Should consume first token", token);
        assertEquals("First token should be 'val'", JavaTokenTypes.LITERAL_val, token.getType());
        assertEquals("Token text should be 'val'", "val", token.getText());

        token = parser.consume();
        assertNotNull("Should consume second token", token);
        assertEquals("Second token should be identifier", JavaTokenTypes.IDENT, token.getType());
        assertEquals("Token text should be 'x'", "x", token.getText());
    }

    /**
     * Test token lookahead without consumption.
     */
    @Test
    public void testTokenLookahead() {
        KotlinPrattParser parser = createParser("fun test() { }");

        LocatableToken peek1 = parser.peek();
        assertNotNull("Should peek at first token", peek1);
        assertEquals("Peek should show 'fun'", JavaTokenTypes.LITERAL_fun, peek1.getType());

        LocatableToken peek2 = parser.peek();
        assertNotNull("Should peek again at first token", peek2);
        assertEquals("Peek should still show 'fun'", JavaTokenTypes.LITERAL_fun, peek2.getType());
        assertSame("Peek should return same token", peek1, peek2);

        LocatableToken consumed = parser.consume();
        assertEquals("Consume should get peeked token", peek1.getType(), consumed.getType());
    }

    /**
     * Test multi-token lookahead.
     */
    @Test
    public void testMultiTokenLookahead() {
        KotlinPrattParser parser = createParser("class Test : Base");

        LocatableToken peek1 = parser.peek(1);
        LocatableToken peek2 = parser.peek(2);
        LocatableToken peek3 = parser.peek(3);

        assertNotNull("Should peek at first token", peek1);
        assertNotNull("Should peek at second token", peek2);
        assertNotNull("Should peek at third token", peek3);

        assertEquals("First peek should be 'class'", JavaTokenTypes.LITERAL_class, peek1.getType());
        assertEquals("Second peek should be identifier", JavaTokenTypes.IDENT, peek2.getType());
        assertEquals("Third peek should be colon", JavaTokenTypes.COLON, peek3.getType());
    }

    /**
     * Test invalid lookahead distance.
     */
    @Test
    public void testInvalidLookaheadDistance() {
        KotlinPrattParser parser = createParser("test");

        try {
            parser.peek(0);
            fail("Should throw exception for invalid lookahead distance");
        } catch (IllegalArgumentException e) {
            assertTrue("Exception message should mention distance",
                      e.getMessage().contains("distance"));
        }

        try {
            parser.peek(-1);
            fail("Should throw exception for negative lookahead distance");
        } catch (IllegalArgumentException e) {
            assertTrue("Exception message should mention distance",
                      e.getMessage().contains("distance"));
        }
    }

    /**
     * Test token push back functionality.
     */
    @Test
    public void testTokenPushBack() {
        KotlinPrattParser parser = createParser("a b c");

        LocatableToken tokenA = parser.consume();
        assertEquals("Should consume 'a'", "a", tokenA.getText());

        LocatableToken tokenB = parser.consume();
        assertEquals("Should consume 'b'", "b", tokenB.getText());

        parser.pushBack(tokenB);

        LocatableToken tokenB2 = parser.consume();
        assertEquals("Should get pushed back token 'b'", "b", tokenB2.getText());

        LocatableToken tokenC = parser.consume();
        assertEquals("Should consume 'c'", "c", tokenC.getText());
    }

    /**
     * Test token matching.
     */
    @Test
    public void testTokenMatching() {
        KotlinPrattParser parser = createParser("if (true)");

        assertTrue("Should match 'if' token", parser.match(JavaTokenTypes.LITERAL_if));
        assertFalse("Should not match 'else' token", parser.match(JavaTokenTypes.LITERAL_else));

        parser.consume(); // consume 'if'

        assertTrue("Should match '(' token", parser.match(JavaTokenTypes.LPAREN));
        assertFalse("Should not match ')' token", parser.match(JavaTokenTypes.RPAREN));
    }

    /**
     * Test conditional token consumption.
     */
    @Test
    public void testConsumeIf() {
        KotlinPrattParser parser = createParser("var x : Int");

        LocatableToken token = parser.consumeIf(JavaTokenTypes.LITERAL_val);
        assertNull("Should not consume non-matching token", token);

        token = parser.consumeIf(JavaTokenTypes.LITERAL_var);
        assertNotNull("Should consume matching token", token);
        assertEquals("Should consume 'var'", "var", token.getText());

        token = parser.consumeIf(JavaTokenTypes.IDENT);
        assertNotNull("Should consume identifier", token);
        assertEquals("Should consume 'x'", "x", token.getText());
    }

    /**
     * Test expect token with error reporting.
     */
    @Test
    public void testExpectToken() {
        KotlinPrattParser parser = createParser("val x = 42");

        LocatableToken token = parser.expect(JavaTokenTypes.LITERAL_val, "Expected 'val'");
        assertNotNull("Should get expected token", token);
        assertEquals("Should get 'val'", "val", token.getText());
        assertFalse("Should not have errors", parser.hasErrors());

        token = parser.expect(JavaTokenTypes.LITERAL_var, "Expected 'var' but found identifier");
        assertNull("Should return null for unexpected token", token);
        assertTrue("Should have errors", parser.hasErrors());
        assertEquals("Should have one error", 1, parser.getErrors().size());

        ParseResult.ParseError error = parser.getErrors().get(0);
        assertEquals("Error message should match", "Expected 'var' but found identifier", error.message());
    }

    // ========== Error Handling Tests ==========

    /**
     * Test error recording with location information.
     */
    @Test
    public void testErrorRecording() {
        KotlinPrattParser parser = createParser("val 123 = x");

        LocatableToken valToken = parser.consume();
        LocatableToken numberToken = parser.consume();

        parser.error("Unexpected number in identifier position", numberToken);

        assertTrue("Should have errors", parser.hasErrors());
        assertEquals("Should have one error", 1, parser.getErrors().size());

        ParseResult.ParseError error = parser.getErrors().get(0);
        assertEquals("Error message should match", "Unexpected number in identifier position", error.message());
        assertEquals("Error line should match token", numberToken.getLine(), error.getLine());
        assertEquals("Error column should match token", numberToken.getColumn(), error.getColumn());
    }

    /**
     * Test error recording with null token.
     */
    @Test
    public void testErrorRecordingWithNullToken() {
        KotlinPrattParser parser = createParser("");

        parser.error("Unexpected end of input", null);

        assertTrue("Should have errors", parser.hasErrors());
        assertEquals("Should have one error", 1, parser.getErrors().size());

        ParseResult.ParseError error = parser.getErrors().get(0);
        assertEquals("Error message should match", "Unexpected end of input", error.message());
        assertEquals("Error line should be -1 for null token", -1, error.getLine());
        assertEquals("Error column should be -1 for null token", -1, error.getColumn());
    }

    /**
     * Test clearing errors.
     */
    @Test
    public void testClearErrors() {
        KotlinPrattParser parser = createParser("test");

        parser.error("Test error 1", null);
        parser.error("Test error 2", null);

        assertEquals("Should have two errors", 2, parser.getErrors().size());
        assertTrue("Should have errors", parser.hasErrors());

        parser.clearErrors();

        assertEquals("Should have no errors after clear", 0, parser.getErrors().size());
        assertFalse("Should not have errors after clear", parser.hasErrors());
    }

    /**
     * Test error formatting.
     */
    @Test
    public void testErrorFormatting() {
        KotlinPrattParser parser = createParser("val x");
        LocatableToken token = parser.consume();

        parser.error("Test error", token);

        ParseResult.ParseError error = parser.getErrors().get(0);
        String formatted = error.getFormattedMessage();

        assertTrue("Formatted message should contain error text", formatted.contains("Test error"));
        assertTrue("Formatted message should contain line number", formatted.contains("line " + token.getLine()));
        assertTrue("Formatted message should contain column number", formatted.contains("column " + token.getColumn()));
    }

    // ========== Parser Synchronization Tests ==========

    /**
     * Test synchronization to recovery points.
     */
    @Test
    public void testSynchronization() {
        KotlinPrattParser parser = createParser("error error ; valid code");

        // Consume past errors
        parser.consume(); // 'error'
        parser.consume(); // 'error'

        LocatableToken syncToken = parser.synchronize(JavaTokenTypes.SEMI, JavaTokenTypes.RCURLY);

        assertNotNull("Should find synchronization token", syncToken);
        assertEquals("Should synchronize on semicolon", JavaTokenTypes.SEMI, syncToken.getType());

        // Next token should be after semicolon
        LocatableToken nextToken = parser.consume();
        assertEquals("Should continue after sync point", JavaTokenTypes.IDENT, nextToken.getType());
        assertEquals("Should be at 'valid'", "valid", nextToken.getText());
    }

    /**
     * Test synchronization with multiple sync tokens.
     */
    @Test
    public void testSynchronizationMultipleTokens() {
        KotlinPrattParser parser = createParser("error error } more");

        parser.consume(); // 'error'

        LocatableToken syncToken = parser.synchronize(JavaTokenTypes.SEMI, JavaTokenTypes.RCURLY);

        assertNotNull("Should find synchronization token", syncToken);
        assertEquals("Should synchronize on closing brace", JavaTokenTypes.RCURLY, syncToken.getType());
    }

    /**
     * Test synchronization when EOF is reached.
     */
    @Test
    public void testSynchronizationToEOF() {
        KotlinPrattParser parser = createParser("error error");

        parser.consume(); // 'error'

        LocatableToken syncToken = parser.synchronize(JavaTokenTypes.SEMI, JavaTokenTypes.RCURLY);

        assertNull("Should return null when EOF reached", syncToken);
    }

    // ========== Expression Parsing Tests (with mock parselets) ==========

    /**
     * Test expression parsing with no registered parselets.
     */
    @Test
    public void testExpressionParsingNoParselets() {
        KotlinPrattParser parser = createParserWithRegistry("42", new ParseletRegistry());

        ParsedNode node = parser.parseExpression();

        assertNull("Should return null with no parselets", node);
        assertTrue("Should have error", parser.hasErrors());
        assertTrue("Error should mention unexpected token",
                  parser.getErrors().get(0).message().contains("Unexpected token"));
    }

    /**
     * Test expression parsing with EOF.
     */
    @Test
    public void testExpressionParsingEOF() {
        KotlinPrattParser parser = createParser("");

        ParsedNode node = parser.parseExpression();

        assertNull("Should return null at EOF", node);
        assertTrue("Should have error", parser.hasErrors());
        assertTrue("Error should mention end of input",
                  parser.getErrors().get(0).message().toLowerCase().contains("end of input"));
    }

    /**
     * Test getCurrentToken tracking.
     */
    @Test
    public void testGetCurrentToken() {
        KotlinPrattParser parser = createParser("a b c");

        assertNull("Current token should be null initially", parser.getCurrentToken());

        LocatableToken tokenA = parser.consume();
        assertEquals("Current token should be 'a'", tokenA, parser.getCurrentToken());

        LocatableToken tokenB = parser.consume();
        assertEquals("Current token should be 'b'", tokenB, parser.getCurrentToken());

        parser.pushBack(tokenB);
        // After pushback, current token should revert
        assertNotNull("Current token should be set after pushback", parser.getCurrentToken());
    }


}
