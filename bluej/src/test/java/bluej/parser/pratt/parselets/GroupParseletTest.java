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
package bluej.parser.pratt.parselets;

import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.pratt.KotlinPrattParser;
import bluej.parser.pratt.ParseResult;
import bluej.parser.pratt.TokenOperations;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.List;
import java.util.ArrayList;

/**
 * Comprehensive test suite for {@link GroupParselet}.
 *
 * <p>This test suite validates the GroupParselet's ability to handle parenthesized
 * expressions correctly, including proper parsing of nested expressions, error
 * handling for malformed input, and integration with the parser framework.</p>
 *
 * <p>Tests are organized into nested classes by functionality:</p>
 * <ul>
 *   <li>Basic parsing functionality</li>
 *   <li>Error handling scenarios</li>
 *   <li>Token recognition and validation</li>
 *   <li>Parser integration</li>
 * </ul>
 *
 * @author BlueJ Team
 */
public class GroupParseletTest {

    private GroupParselet parselet;
    private TestTokenOperations tokenOps;
    private TestKotlinPrattParser parser;

    @Before
    public void setUp() {
        parselet = new GroupParselet();
        tokenOps = new TestTokenOperations();
        parser = new TestKotlinPrattParser(tokenOps);
    }

    @Test
    public void testSimpleParenthesizedExpression() {
        // Setup: (42)
        LocatableToken lparen = createToken(JavaTokenTypes.LPAREN, "(", 1, 1);
        LocatableToken literal = createToken(JavaTokenTypes.NUM_INT, "42", 1, 2);
        LocatableToken rparen = createToken(JavaTokenTypes.RPAREN, ")", 1, 4);

        tokenOps.setTokens(List.of(literal, rparen));

        // Act
        ParseResult<ParsedNode> result = parselet.parse(parser, lparen);



        // Assert - With NodeFactory, parselets now create nodes
        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should create a node for parenthesized expression", node);

        assertTrue("All tokens should be consumed", tokenOps.getAllTokens().isEmpty());
        assertFalse("Should not have parsing errors", parser.hasErrors());
    }

    @Test
    public void testNestedParentheses() {
        // Setup: ((42))
        LocatableToken lparen1 = createToken(JavaTokenTypes.LPAREN, "(", 1, 1);
        LocatableToken lparen2 = createToken(JavaTokenTypes.LPAREN, "(", 1, 2);
        LocatableToken literal = createToken(JavaTokenTypes.NUM_INT, "42", 1, 3);
        LocatableToken rparen1 = createToken(JavaTokenTypes.RPAREN, ")", 1, 5);
        LocatableToken rparen2 = createToken(JavaTokenTypes.RPAREN, ")", 1, 6);

        tokenOps.setTokens(List.of(lparen2, literal, rparen1, rparen2));

        // Act
        ParseResult<ParsedNode> result = parselet.parse(parser, lparen1);

        // Assert - With NodeFactory, parselets now create nodes for nested structure
        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should create a node for nested parentheses", node);
        assertTrue("All tokens should be consumed", tokenOps.getAllTokens().isEmpty());
        assertFalse("Should not have parsing errors for nested parentheses", parser.hasErrors());
    }

    @Test
    public void testParenthesizedStringLiteral() {
        // Setup: ("hello")
        LocatableToken lparen = createToken(JavaTokenTypes.LPAREN, "(", 1, 1);
        LocatableToken stringLit = createToken(JavaTokenTypes.STRING_LITERAL, "\"hello\"", 1, 2);
        LocatableToken rparen = createToken(JavaTokenTypes.RPAREN, ")", 1, 9);

        tokenOps.setTokens(List.of(stringLit, rparen));

        // Act
        ParseResult<ParsedNode> result = parselet.parse(parser, lparen);

        // Assert - With NodeFactory, parselets now create nodes
        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should create a node for parenthesized string literal", node);
        assertFalse("Should not have parsing errors for string literal", parser.hasErrors());
    }

    @Test
    public void testParenthesizedBooleanLiteral() {
        // Setup: (true)
        LocatableToken lparen = createToken(JavaTokenTypes.LPAREN, "(", 1, 1);
        LocatableToken boolLit = createToken(JavaTokenTypes.LITERAL_true, "true", 1, 2);
        LocatableToken rparen = createToken(JavaTokenTypes.RPAREN, ")", 1, 6);

        tokenOps.setTokens(List.of(boolLit, rparen));

        // Act
        ParseResult<ParsedNode> result = parselet.parse(parser, lparen);

        // Assert - With NodeFactory, parselets now create nodes
        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should create a node for parenthesized boolean literal", node);
        assertFalse("Should not have parsing errors for boolean literal", parser.hasErrors());
    }

    @Test
    public void testNullToken() {
        // Act
        ParseResult<ParsedNode> result = parselet.parse(parser, null);

        // Assert
        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure", result.isFailure());
    }

    @Test
    public void testWrongTokenType() {
        LocatableToken wrongToken = createToken(JavaTokenTypes.RCURLY, "}", 1, 1);

        // Act
        ParseResult<ParsedNode> result = parselet.parse(parser, wrongToken);

        // Assert
        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure", result.isFailure());
    }

    @Test
    public void testMissingClosingParen() {
        // Setup: (42 [no closing paren]
        LocatableToken lparen = createToken(JavaTokenTypes.LPAREN, "(", 1, 1);
        LocatableToken literal = createToken(JavaTokenTypes.NUM_INT, "42", 1, 2);
        LocatableToken eof = createToken(JavaTokenTypes.EOF, "", 1, 4);

        tokenOps.setTokens(List.of(literal, eof));

        // Act
        ParseResult<ParsedNode> result = parselet.parse(parser, lparen);

        // Assert
        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure", result.isFailure());
    }

    @Test
    public void testEmptyParentheses() {
        // Setup: ()
        LocatableToken lparen = createToken(JavaTokenTypes.LPAREN, "(", 1, 1);
        LocatableToken rparen = createToken(JavaTokenTypes.RPAREN, ")", 1, 2);

        tokenOps.setTokens(List.of(rparen));

        // Act
        ParseResult<ParsedNode> result = parselet.parse(parser, lparen);

        // Assert - Empty parentheses should fail because parseExpression finds RPAREN
        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure", result.isFailure());
    }

    @Test
    public void testWrongClosingToken() {
        // Setup: (42]
        LocatableToken lparen = createToken(JavaTokenTypes.LPAREN, "(", 1, 1);
        LocatableToken literal = createToken(JavaTokenTypes.NUM_INT, "42", 1, 2);
        LocatableToken rbracket = createToken(JavaTokenTypes.RBRACK, "]", 1, 4);

        tokenOps.setTokens(List.of(literal, rbracket));

        // Act
        ParseResult<ParsedNode> result = parselet.parse(parser, lparen);

        // Assert
        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure", result.isFailure());
    }

    @Test
    public void testParseInvalidTokenType() {
        // Test that parselet reports error for non-parenthesis tokens
        LocatableToken invalidToken = createToken(JavaTokenTypes.NUM_INT, "42", 1, 1);
        TestKotlinPrattParser freshParser = new TestKotlinPrattParser(new TestTokenOperations());

        ParseResult<ParsedNode> result = parselet.parse(freshParser, invalidToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure for invalid token type", result.isFailure());
        assertFalse("Should not have errors in parser", freshParser.hasErrors());
        assertTrue("Should have errors in result", !result.getErrors().isEmpty());
        assertTrue("Error message should mention expected token",
                   result.getErrors().get(0).message().contains("Expected"));
    }

    @Test
    public void testParseRightParenError() {
        TestKotlinPrattParser freshParser = new TestKotlinPrattParser(new TestTokenOperations());
        LocatableToken rparen = createToken(JavaTokenTypes.RPAREN, ")", 1, 1);

        // Test that right paren produces error when used as prefix token
        ParseResult<ParsedNode> result = parselet.parse(freshParser, rparen);

        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure for right paren as prefix", result.isFailure());
    }

    @Test
    public void testParseUnsupportedTokenTypes() {
        int[] unsupportedTokenTypes = {
            JavaTokenTypes.STRING_LITERAL,
            JavaTokenTypes.IDENT,
            JavaTokenTypes.PLUS,
            JavaTokenTypes.MINUS,
            JavaTokenTypes.LCURLY,
            JavaTokenTypes.LBRACK
        };

        for (int tokenType : unsupportedTokenTypes) {
            TestKotlinPrattParser freshParser = new TestKotlinPrattParser(new TestTokenOperations());
            LocatableToken token = createToken(tokenType, "token", 1, 1);

            // Test that unsupported tokens produce appropriate errors
            ParseResult<ParsedNode> result = parselet.parse(freshParser, token);
            assertNotNull("Should return a result for unsupported token type: " + tokenType, result);
            assertTrue("Should be a failure for unsupported token type: " + tokenType, result.isFailure());
        }
    }

    @Test
    public void testParseNullToken() {
        TestKotlinPrattParser freshParser = new TestKotlinPrattParser(new TestTokenOperations());

        // Test that null token produces appropriate error
        ParseResult<ParsedNode> result = parselet.parse(freshParser, null);

        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure for null token", result.isFailure());
        assertFalse("Should not report error for null token", freshParser.hasErrors());
        assertTrue("Should have errors in result", !result.getErrors().isEmpty());
    }

    @Test
    public void testGetHandledConstruct() {
        String description = parselet.getHandledConstruct(JavaTokenTypes.LPAREN);
        assertEquals("parenthesized expression", description);

        String unsupported = parselet.getHandledConstruct(JavaTokenTypes.NUM_INT);
        assertEquals("unsupported token type", unsupported);
    }

    @Test
    public void testParserIntegration() {
        // This test would be more comprehensive once binary operator parselets are available
        // For now, we test basic integration structure
        LocatableToken lparen = createToken(JavaTokenTypes.LPAREN, "(", 1, 1);
        LocatableToken literal = createToken(JavaTokenTypes.NUM_INT, "42", 1, 2);
        LocatableToken rparen = createToken(JavaTokenTypes.RPAREN, ")", 1, 4);

        tokenOps.setTokens(List.of(literal, rparen));

        // Act
        ParseResult<ParsedNode> result = parselet.parse(parser, lparen);

        // Assert - With NodeFactory, parselets now create nodes
        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should create a node for integrated parsing", node);
        assertFalse("Should integrate without errors", parser.hasErrors());
    }

    @Test
    public void testValidateStructure() {
        LocatableToken lparen = createToken(JavaTokenTypes.LPAREN, "(", 1, 1);
        LocatableToken literal = createToken(JavaTokenTypes.NUM_INT, "42", 1, 2);
        LocatableToken rparen = createToken(JavaTokenTypes.RPAREN, ")", 1, 4);

        tokenOps.setTokens(List.of(literal, rparen));

        // Act
        boolean isValid = parselet.validateStructure(parser, lparen);

        // Assert - With NodeFactory, validation should succeed for valid structure
        assertTrue("Should validate correct parenthesized structure", isValid);
    }

    @Test
    public void testToString() {
        String str = parselet.toString();
        assertEquals("GroupParselet", str);
    }

    // Helper methods for test setup

    /**
     * Creates a mock LocatableToken for testing.
     */
    private LocatableToken createToken(int type, String text, int line, int column) {
        bluej.parser.lexer.LineColPos begin = new bluej.parser.lexer.LineColPos(line, column, 0);
        bluej.parser.lexer.LineColPos end = new bluej.parser.lexer.LineColPos(line, column + text.length(), text.length());
        return new LocatableToken(type, text, begin, end);
    }

    /**
     * Simple test implementation of KotlinPrattParser for testing.
     */
    private static class TestKotlinPrattParser extends KotlinPrattParser {
        private TestTokenOperations tokenOps;
        private boolean hasErrors = false;
        private String lastError = "";

        public TestKotlinPrattParser(TestTokenOperations tokenOps) {
            super(tokenOps, null, new bluej.parser.pratt.TestNodeFactory());  // Use 3-parameter constructor with NodeFactory
            this.tokenOps = tokenOps;

            // Register parselets needed for testing
            // Without these, parseExpression will fail when trying to parse literals
            bluej.parser.pratt.parselets.LiteralParselet literalParselet = new bluej.parser.pratt.parselets.LiteralParselet();
            getRegistry().register(JavaTokenTypes.NUM_INT, literalParselet);
            getRegistry().register(JavaTokenTypes.STRING_LITERAL, literalParselet);
            getRegistry().register(JavaTokenTypes.LITERAL_true, literalParselet);
            getRegistry().register(JavaTokenTypes.LITERAL_false, literalParselet);
            getRegistry().register(JavaTokenTypes.LITERAL_null, literalParselet);

            // Also register GroupParselet for nested parentheses
            getRegistry().register(JavaTokenTypes.LPAREN, new bluej.parser.pratt.parselets.GroupParselet());
        }

        @Override
        public void error(String message, LocatableToken token) {
            hasErrors = true;
            lastError = message;
        }

        public boolean hasErrors() {
            return hasErrors;
        }

        public String getLastError() {
            return lastError;
        }

        @Override
        public LocatableToken consume() {
            return tokenOps.nextToken();
        }

        @Override
        public LocatableToken peek() {
            return tokenOps.peek();
        }
    }

    /**
     * Test implementation of TokenOperations for testing parser behavior.
     */
    private static class TestTokenOperations implements TokenOperations {
        private List<LocatableToken> tokens = new ArrayList<>();
        private int position = 0;

        public void setTokens(List<LocatableToken> tokens) {
            this.tokens = new ArrayList<>(tokens);
            this.position = 0;
        }

        public List<LocatableToken> getAllTokens() {
            return new ArrayList<>(tokens.subList(position, tokens.size()));
        }

        @Override
        public LocatableToken nextToken() {
            if (position >= tokens.size()) {
                bluej.parser.lexer.LineColPos pos = new bluej.parser.lexer.LineColPos(1, 1, 0);
                return new LocatableToken(JavaTokenTypes.EOF, "", pos, pos);
            }
            return tokens.get(position++);
        }

        @Override
        public LocatableToken LA(int distance) {
            int peekPosition = position + distance - 1;
            if (peekPosition >= tokens.size()) {
                bluej.parser.lexer.LineColPos pos = new bluej.parser.lexer.LineColPos(1, 1, 0);
                return new LocatableToken(JavaTokenTypes.EOF, "", pos, pos);
            }
            return tokens.get(peekPosition);
        }

        public LocatableToken peek() {
            return LA(1);
        }

        public LocatableToken peek(int offset) {
            return LA(offset);
        }

        @Override
        public void pushBack(LocatableToken token) {
            if (position > 0) {
                position--;
                tokens.set(position, token);
            }
        }

        @Override
        public LocatableToken getMostRecent() {
            if (position > 0) {
                return tokens.get(position - 1);
            }
            bluej.parser.lexer.LineColPos pos = new bluej.parser.lexer.LineColPos(1, 1, 0);
            return new LocatableToken(JavaTokenTypes.EOF, "", pos, pos);
        }
    }
}
