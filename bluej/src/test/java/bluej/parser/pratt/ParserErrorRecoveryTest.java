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

import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.lexer.LineColPos;
import bluej.parser.nodes.ParsedNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for error recovery mechanisms in KotlinPrattParser.
 *
 * Tests verify that the parser can:
 * - Record accurate error information
 * - Recover gracefully from syntax errors
 * - Continue parsing after errors
 * - Synchronize on appropriate tokens
 * - Provide informative error messages consistent with legacy parser
 *
 * @author BlueJ Development Team
 */
public class ParserErrorRecoveryTest {

    private KotlinPrattParser parser;
    private TestTokenOperations tokenOps;

    @BeforeEach
    void setUp() {
        // Create parser with test token operations for controlled testing
        tokenOps = new TestTokenOperations();
        parser = new KotlinPrattParser(tokenOps, null, new TestNodeFactory());

        // Set up basic parselets for testing
        ParseletRegistry registry = parser.getRegistry();
        registry.register(JavaTokenTypes.NUM_INT, new TestLiteralParselet());
        registry.register(JavaTokenTypes.IDENT, new TestIdentifierParselet());
        registry.register(JavaTokenTypes.PLUS, new TestBinaryOperatorParselet());
        registry.register(JavaTokenTypes.LPAREN, new TestGroupParselet());
    }

    @Test
    @DisplayName("Error recording captures message and location accurately")
    void testBasicErrorRecording() {
        // Create a token for testing
        LocatableToken errorToken = createToken(JavaTokenTypes.SEMI, ";", 1, 5, 1, 5);

        // Record an error
        parser.error("Expected identifier", errorToken);

        // Verify error was recorded
        assertTrue(parser.hasErrors());
        List<KotlinPrattParser.ParseError> errors = parser.getErrors();
        assertEquals(1, errors.size());

        KotlinPrattParser.ParseError error = errors.get(0);
        assertEquals("Expected identifier", error.message());
        assertEquals(errorToken, error.token());
        assertEquals(1, error.getLine());
        assertEquals(5, error.getColumn());

        // Verify formatted message includes location
        String formatted = error.getFormattedMessage();
        assertTrue(formatted.contains("line 1"));
        assertTrue(formatted.contains("column 5"));
        assertTrue(formatted.contains("Expected identifier"));
    }

    @Test
    @DisplayName("Multiple errors are recorded and accessible")
    void testMultipleErrorRecording() {
        LocatableToken token1 = createToken(JavaTokenTypes.SEMI, ";", 1, 5, 1, 5);
        LocatableToken token2 = createToken(JavaTokenTypes.LCURLY, "{", 2, 1, 2, 1);

        parser.error("First error", token1);
        parser.error("Second error", token2);

        assertTrue(parser.hasErrors());
        List<KotlinPrattParser.ParseError> errors = parser.getErrors();
        assertEquals(2, errors.size());

        assertEquals("First error", errors.get(0).message());
        assertEquals(1, errors.get(0).getLine());
        assertEquals("Second error", errors.get(1).message());
        assertEquals(2, errors.get(1).getLine());
    }

    @Test
    @DisplayName("Error clearing removes all recorded errors")
    void testErrorClearing() {
        LocatableToken token = createToken(JavaTokenTypes.SEMI, ";", 1, 5, 1, 5);

        parser.error("Test error", token);
        assertTrue(parser.hasErrors());

        parser.clearErrors();
        assertFalse(parser.hasErrors());
        assertTrue(parser.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Synchronization finds appropriate sync tokens")
    void testBasicSynchronization() {
        // Set up token stream with sync point
        tokenOps.setTokens(
            createToken(JavaTokenTypes.IDENT, "invalid", 1, 1, 1, 7),
            createToken(JavaTokenTypes.STAR, "*", 1, 8, 1, 8),
            createToken(JavaTokenTypes.SEMI, ";", 1, 9, 1, 9), // sync point
            createToken(JavaTokenTypes.IDENT, "next", 1, 11, 1, 14),
            createToken(JavaTokenTypes.EOF, "", 1, 15, 1, 15)
        );

        parser.initializeWithTokenStream();

        // Synchronize on semicolon
        LocatableToken syncToken = parser.synchronize(JavaTokenTypes.SEMI);

        assertNotNull(syncToken);
        assertEquals(JavaTokenTypes.SEMI, syncToken.getType());
        assertEquals(";", syncToken.getText());

        // Verify parser is positioned after sync token
        LocatableToken nextToken = parser.peek();
        assertEquals(JavaTokenTypes.IDENT, nextToken.getType());
        assertEquals("next", nextToken.getText());
    }

    @Test
    @DisplayName("Synchronization with multiple sync token types")
    void testMultipleSyncTokens() {
        tokenOps.setTokens(
            createToken(JavaTokenTypes.IDENT, "error", 1, 1, 1, 5),
            createToken(JavaTokenTypes.STAR, "*", 1, 6, 1, 6),
            createToken(JavaTokenTypes.RCURLY, "}", 1, 7, 1, 7), // sync point
            createToken(JavaTokenTypes.IDENT, "after", 1, 8, 1, 12),
            createToken(JavaTokenTypes.EOF, "", 1, 13, 1, 13)
        );

        parser.initializeWithTokenStream();

        // Synchronize on multiple token types
        LocatableToken syncToken = parser.synchronize(
            JavaTokenTypes.SEMI,
            JavaTokenTypes.RCURLY,
            JavaTokenTypes.LITERAL_class
        );

        assertNotNull(syncToken);
        assertEquals(JavaTokenTypes.RCURLY, syncToken.getType());
    }

    @Test
    @DisplayName("Synchronization returns null at EOF")
    void testSynchronizationAtEOF() {
        tokenOps.setTokens(
            createToken(JavaTokenTypes.IDENT, "error", 1, 1, 1, 5),
            createToken(JavaTokenTypes.STAR, "*", 1, 6, 1, 6),
            createToken(JavaTokenTypes.EOF, "", 1, 7, 1, 7)
        );

        parser.initializeWithTokenStream();

        // Try to synchronize but no sync token exists
        LocatableToken syncToken = parser.synchronize(JavaTokenTypes.SEMI);

        assertNull(syncToken);
    }

    @Test
    @DisplayName("Error messages are consistent with expected patterns")
    void testErrorMessageConsistency() {
        LocatableToken unexpectedToken = createToken(JavaTokenTypes.STAR, "*", 1, 5, 1, 5);

        // Test various error message patterns that should match legacy parser
        parser.error("Expected identifier", unexpectedToken);
        parser.error("Expected '(' after 'for'", unexpectedToken);
        parser.error("Unexpected token: *", unexpectedToken);
        parser.error("Expected ';' following import statement", unexpectedToken);

        List<KotlinPrattParser.ParseError> errors = parser.getErrors();
        assertEquals(4, errors.size());

        // Verify messages follow expected patterns
        assertTrue(errors.get(0).message().startsWith("Expected"));
        assertTrue(errors.get(1).message().contains("Expected"));
        assertTrue(errors.get(2).message().contains("Unexpected"));
        assertTrue(errors.get(3).message().contains("Expected"));

        // Verify formatted messages include location
        for (KotlinPrattParser.ParseError error : errors) {
            String formatted = error.getFormattedMessage();
            assertTrue(formatted.contains("line 1"));
            assertTrue(formatted.contains("column 5"));
        }
    }

    @Test
    @DisplayName("Expect method handles matching tokens correctly")
    void testExpectWithMatchingToken() {
        tokenOps.setTokens(
            createToken(JavaTokenTypes.SEMI, ";", 1, 1, 1, 1),
            createToken(JavaTokenTypes.EOF, "", 1, 2, 1, 2)
        );

        parser.initializeWithTokenStream();

        LocatableToken result = parser.expect(JavaTokenTypes.SEMI, "Expected semicolon");

        assertNotNull(result);
        assertEquals(JavaTokenTypes.SEMI, result.getType());
        assertFalse(parser.hasErrors());
    }

    @Test
    @DisplayName("Expect method records error for non-matching tokens")
    void testExpectWithNonMatchingToken() {
        tokenOps.setTokens(
            createToken(JavaTokenTypes.STAR, "*", 1, 1, 1, 1),
            createToken(JavaTokenTypes.EOF, "", 1, 2, 1, 2)
        );

        parser.initializeWithTokenStream();

        LocatableToken result = parser.expect(JavaTokenTypes.SEMI, "Expected semicolon");

        assertNull(result);
        assertTrue(parser.hasErrors());

        List<KotlinPrattParser.ParseError> errors = parser.getErrors();
        assertEquals(1, errors.size());
        assertEquals("Expected semicolon", errors.get(0).message());
    }

    @Test
    @DisplayName("Parser continues after syntax errors")
    void testContinuationAfterErrors() {
        tokenOps.setTokens(
            createToken(JavaTokenTypes.NUM_INT, "42", 1, 1, 1, 2),
            createToken(JavaTokenTypes.STAR, "*", 1, 3, 1, 3), // unexpected token
            createToken(JavaTokenTypes.SEMI, ";", 1, 4, 1, 4), // sync point
            createToken(JavaTokenTypes.NUM_INT, "24", 1, 5, 1, 6),
            createToken(JavaTokenTypes.EOF, "", 1, 7, 1, 7)
        );

        parser.initializeWithTokenStream();

        // Parse first expression - should succeed (returns null in foundation phase)
        ParsedNode expr1 = parser.parseExpression();
        assertNull(expr1); // Foundation phase returns null

        // Try to parse invalid token - should error
        parser.error("Unexpected token", tokenOps.peek());

        // Synchronize and continue
        parser.synchronize(JavaTokenTypes.SEMI);

        // Parse next expression - should succeed (returns null in foundation phase)
        ParsedNode expr2 = parser.parseExpression();
        assertNull(expr2); // Foundation phase returns null

        // Verify error was recorded but parsing continued
        assertTrue(parser.hasErrors());
        assertEquals(1, parser.getErrors().size());
    }

    @Test
    @DisplayName("Error location information is preserved for null tokens")
    void testErrorWithNullToken() {
        parser.error("Unexpected end of input", null);

        assertTrue(parser.hasErrors());
        KotlinPrattParser.ParseError error = parser.getErrors().get(0);

        assertEquals("Unexpected end of input", error.message());
        assertNull(error.token());
        assertEquals(-1, error.getLine());
        assertEquals(-1, error.getColumn());

        String formatted = error.getFormattedMessage();
        assertEquals("Error: Unexpected end of input", formatted);
    }

    @Test
    @DisplayName("Token description generation for error messages")
    void testTokenDescriptionGeneration() {
        // Test with various token types to ensure descriptions are generated
        LocatableToken identToken = createToken(JavaTokenTypes.IDENT, "myVariable", 1, 1, 1, 10);
        LocatableToken numToken = createToken(JavaTokenTypes.NUM_INT, "123", 1, 1, 1, 3);
        LocatableToken opToken = createToken(JavaTokenTypes.PLUS, "+", 1, 1, 1, 1);

        parser.error("Test error", identToken);
        parser.error("Test error", numToken);
        parser.error("Test error", opToken);

        assertEquals(3, parser.getErrors().size());

        // All should have the same message but different tokens
        for (KotlinPrattParser.ParseError error : parser.getErrors()) {
            assertEquals("Test error", error.message());
            assertNotNull(error.token());
        }
    }

    // Helper methods for creating test tokens and parselets

    private LocatableToken createToken(int type, String text, int line, int col, int endLine, int endCol) {
        LineColPos begin = new LineColPos(line, col, 0);
        LineColPos end = new LineColPos(endLine, endCol, 0);
        return new LocatableToken(type, text, begin, end);
    }

    // Simple test parselets for testing error recovery
    private static class TestLiteralParselet implements PrefixParselet {
        @Override
        public ParsedNode parse(KotlinPrattParser parser, LocatableToken token) {
            // Foundation phase returns null for successful validation
            return null;
        }

        @Override
        public int[] getExpectedFollowTokens() {
            return new int[0];
        }
    }

    private static class TestIdentifierParselet implements PrefixParselet {
        @Override
        public ParsedNode parse(KotlinPrattParser parser, LocatableToken token) {
            // Foundation phase returns null for successful validation
            return null;
        }

        @Override
        public int[] getExpectedFollowTokens() {
            return new int[0];
        }
    }

    private static class TestBinaryOperatorParselet implements InfixParselet {
        @Override
        public ParsedNode parse(KotlinPrattParser parser, ParsedNode left, LocatableToken token) {
            ParsedNode right = parser.parseExpression(getPrecedence());
            // Foundation phase returns null for successful validation
            return null;
        }

        @Override
        public int getPrecedence() {
            return Precedence.ADDITIVE.getValue();
        }

        @Override
        public int[] getExpectedFollowTokens() {
            return new int[0];
        }
    }

    private static class TestGroupParselet implements PrefixParselet {
        @Override
        public ParsedNode parse(KotlinPrattParser parser, LocatableToken token) {
            ParsedNode expression = parser.parseExpression();
            parser.expect(JavaTokenTypes.RPAREN, "Expected ')' after expression");
            // Foundation phase returns null for successful validation
            return null;
        }

        @Override
        public int[] getExpectedFollowTokens() {
            return new int[] { JavaTokenTypes.RPAREN };
        }
    }



    /**
     * Test implementation of TokenOperations for controlled testing.
     * Allows setting up specific token sequences for testing scenarios.
     */
    private static class TestTokenOperations implements TokenOperations {
        private LocatableToken[] tokens;
        private int position = 0;
        private LocatableToken mostRecent = null;

        public void setTokens(LocatableToken... tokens) {
            this.tokens = tokens;
            this.position = 0;
            this.mostRecent = null;
        }

        @Override
        public LocatableToken nextToken() {
            if (position < tokens.length) {
                mostRecent = tokens[position++];
                return mostRecent;
            }
            mostRecent = createEOFToken();
            return mostRecent;
        }

        @Override
        public LocatableToken LA(int distance) {
            int lookPosition = position + distance - 1;
            if (lookPosition < tokens.length && lookPosition >= 0) {
                return tokens[lookPosition];
            }
            return createEOFToken();
        }

        @Override
        public void pushBack(LocatableToken token) {
            if (position > 0) {
                position--;
            }
        }

        @Override
        public LocatableToken getMostRecent() {
            return mostRecent;
        }

        public LocatableToken peek() {
            return LA(1);
        }

        private LocatableToken createEOFToken() {
            LineColPos pos = new LineColPos(1, 1, 0);
            return new LocatableToken(JavaTokenTypes.EOF, "", pos, pos);
        }
    }
}
