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
import bluej.parser.nodes.ExpressionNode;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.pratt.KotlinPrattParser;
import bluej.parser.pratt.NodeFactory;
import bluej.parser.pratt.TestNodeFactory;
import bluej.parser.pratt.TokenOperations;
import junit.framework.TestCase;
import org.junit.Test;

/**
 * Unit tests for {@link LiteralParselet}.
 *
 * <p>This test class validates the literal parsing functionality including:</p>
 * <ul>
 *   <li>All supported literal types (integer, long, float, double, string, character, boolean, null)</li>
 *   <li>Error handling for invalid tokens</li>
 *   <li>Token type validation</li>
 *   <li>Parselet registration and dispatch (foundation phase)</li>
 * </ul>
 *
 * <p>Note: During foundation phase, parselets validate tokens but don't create AST nodes yet.</p>
 *
 * @author BlueJ Team
 */
public class LiteralParseletTest extends TestCase {

    private LiteralParselet parselet;
    private TestKotlinPrattParser testParser;

    @Override
    public void setUp() {
        parselet = new LiteralParselet();
        testParser = new TestKotlinPrattParser();
    }

    @Test
    public void testParseIntegerLiteral() {
        LocatableToken token = createToken(JavaTokenTypes.NUM_INT, "42", 1, 1);
        ParsedNode result = parselet.parse(testParser, token);

        // With NodeFactory, parselets now create nodes
        assertNotNull("Should create an AST node for integer literal", result);
        assertFalse("No errors should be reported for valid tokens", testParser.hasErrors());
        // canHandle method removed - parse behavior is sufficient validation
    }

    @Test
    public void testParseLongLiteral() {
        LocatableToken token = createToken(JavaTokenTypes.NUM_LONG, "42L", 1, 1);
        ParsedNode result = parselet.parse(testParser, token);

        assertNotNull("Should create an AST node for long literal", result);
        assertFalse("No errors should be reported for valid tokens", testParser.hasErrors());
        assertTrue("Should be a TestNode", result instanceof TestNodeFactory.TestNode);
    }

    @Test
    public void testParseFloatLiteral() {
        LocatableToken token = createToken(JavaTokenTypes.NUM_FLOAT, "3.14f", 1, 1);
        ParsedNode result = parselet.parse(testParser, token);

        assertNotNull("Should create an AST node for float literal", result);
        assertFalse("No errors should be reported for valid tokens", testParser.hasErrors());
        assertTrue("Should be a TestNode", result instanceof TestNodeFactory.TestNode);
    }

    @Test
    public void testParseDoubleLiteral() {
        LocatableToken token = createToken(JavaTokenTypes.NUM_DOUBLE, "3.14", 1, 1);
        ParsedNode result = parselet.parse(testParser, token);

        assertNotNull("Should create an AST node for double literal", result);
        assertFalse("No errors should be reported for valid tokens", testParser.hasErrors());
        assertTrue("Should be a TestNode", result instanceof TestNodeFactory.TestNode);
    }

    @Test
    public void testParseStringLiteral() {
        LocatableToken token = createToken(JavaTokenTypes.STRING_LITERAL, "\"hello world\"", 1, 1);
        ParsedNode result = parselet.parse(testParser, token);

        assertNotNull("Should create an AST node for string literal", result);
        assertFalse("No errors should be reported for valid tokens", testParser.hasErrors());
        assertTrue("Should be a TestNode", result instanceof TestNodeFactory.TestNode);
    }

    @Test
    public void testParseMultilineStringLiteral() {
        LocatableToken token = createToken(JavaTokenTypes.STRING_LITERAL_MULTILINE, "\"\"\"hello\nworld\"\"\"", 1, 1);
        ParsedNode result = parselet.parse(testParser, token);

        assertNotNull("Should create an AST node for multiline string literal", result);
        assertFalse("No errors should be reported for valid tokens", testParser.hasErrors());
        assertTrue("Should be a TestNode", result instanceof TestNodeFactory.TestNode);
    }

    @Test
    public void testParseCharacterLiteral() {
        LocatableToken token = createToken(JavaTokenTypes.CHAR_LITERAL, "'c'", 1, 1);
        ParsedNode result = parselet.parse(testParser, token);

        assertNotNull("Should create an AST node for character literal", result);
        assertFalse("No errors should be reported for valid tokens", testParser.hasErrors());
        assertTrue("Should be a TestNode", result instanceof TestNodeFactory.TestNode);
    }

    @Test
    public void testParseBooleanTrueLiteral() {
        LocatableToken token = createToken(JavaTokenTypes.LITERAL_true, "true", 1, 1);
        ParsedNode result = parselet.parse(testParser, token);

        assertNotNull("Should create an AST node for boolean true literal", result);
        assertFalse("No errors should be reported for valid tokens", testParser.hasErrors());
        assertTrue("Should be a TestNode", result instanceof TestNodeFactory.TestNode);
    }

    @Test
    public void testParseBooleanFalseLiteral() {
        LocatableToken token = createToken(JavaTokenTypes.LITERAL_false, "false", 1, 1);
        ParsedNode result = parselet.parse(testParser, token);

        assertNotNull("Should create an AST node for boolean false literal", result);
        assertFalse("No errors should be reported for valid tokens", testParser.hasErrors());
        assertTrue("Should be a TestNode", result instanceof TestNodeFactory.TestNode);
    }

    @Test
    public void testParseNullLiteral() {
        LocatableToken token = createToken(JavaTokenTypes.LITERAL_null, "null", 1, 1);
        ParsedNode result = parselet.parse(testParser, token);

        assertNotNull("Should create an AST node for null literal", result);
        assertFalse("No errors should be reported for valid tokens", testParser.hasErrors());
        assertTrue("Should be a TestNode", result instanceof TestNodeFactory.TestNode);
    }

    @Test
    public void testHandleNullToken() {
        ParsedNode result = parselet.parse(testParser, null);

        assertNull("Null token should result in null parse result", result);
        assertTrue("Should report error for null token", testParser.hasErrors());
        assertTrue("Should report null token error", testParser.getLastError().contains("Null token"));
    }

    @Test
    public void testHandleNonLiteralToken() {
        LocatableToken token = createToken(JavaTokenTypes.IDENT, "identifier", 1, 1);
        ParsedNode result = parselet.parse(testParser, token);

        assertNull("Non-literal token should result in null parse result", result);
        assertTrue("Should report error for non-literal token", testParser.hasErrors());
        assertTrue("Should report literal token error", testParser.getLastError().contains("Expected literal token"));
    }

    @Test
    public void testParseLiteralTokenTypes() {
        int[] literalTypes = {
            JavaTokenTypes.NUM_INT,
            JavaTokenTypes.NUM_LONG,
            JavaTokenTypes.NUM_FLOAT,
            JavaTokenTypes.NUM_DOUBLE,
            JavaTokenTypes.STRING_LITERAL,
            JavaTokenTypes.STRING_LITERAL_MULTILINE,
            JavaTokenTypes.CHAR_LITERAL,
            JavaTokenTypes.LITERAL_true,
            JavaTokenTypes.LITERAL_false,
            JavaTokenTypes.LITERAL_null
        };

        for (int tokenType : literalTypes) {
            TestKotlinPrattParser freshParser = new TestKotlinPrattParser();
            LocatableToken token = createToken(tokenType, "test", 1, 1);
            // Test that valid literal tokens create nodes
            ParsedNode result = parselet.parse(freshParser, token);
            assertNotNull("Should create AST nodes for valid literal tokens", result);
            assertFalse("Should not produce errors for valid literal tokens", freshParser.hasErrors());
            assertTrue("Should be a TestNode", result instanceof TestNodeFactory.TestNode);
            assertNotNull("Should provide description for token type " + tokenType,
                parselet.getHandledConstruct(tokenType));
        }
    }

    @Test
    public void testParseNonLiteralTokenTypes() {
        int[] nonLiteralTypes = {
            JavaTokenTypes.IDENT,
            JavaTokenTypes.PLUS,
            JavaTokenTypes.MINUS,
            JavaTokenTypes.LPAREN,
            JavaTokenTypes.RPAREN,
            JavaTokenTypes.LITERAL_class,
            JavaTokenTypes.LITERAL_fun,
            JavaTokenTypes.EOF
        };

        for (int tokenType : nonLiteralTypes) {
            TestKotlinPrattParser freshParser = new TestKotlinPrattParser();
            LocatableToken token = createToken(tokenType, "test", 1, 1);
            // Test that invalid tokens produce errors when parsed
            ParsedNode result = parselet.parse(freshParser, token);
            assertNull("Should return null for invalid tokens", result);
            assertTrue("Should report error for non-literal tokens", freshParser.hasErrors());
            assertEquals("Should indicate unsupported token type",
                "unsupported token type", parselet.getHandledConstruct(tokenType));
        }
    }

    @Test
    public void testToString() {
        assertEquals("Should return parselet class name", "LiteralParselet", parselet.toString());
    }

    @Test
    public void testMultipleParseCallsWorkIndependently() {
        LocatableToken token1 = createToken(JavaTokenTypes.NUM_INT, "42", 1, 1);
        LocatableToken token2 = createToken(JavaTokenTypes.STRING_LITERAL, "\"test\"", 2, 1);

        ParsedNode result1 = parselet.parse(testParser, token1);
        ParsedNode result2 = parselet.parse(testParser, token2);

        assertNotNull("First parse should create an AST node", result1);
        assertNotNull("Second parse should create an AST node", result2);
        assertFalse("No errors should be reported", testParser.hasErrors());
        assertTrue("First should be a TestNode", result1 instanceof TestNodeFactory.TestNode);
        assertTrue("Second should be a TestNode", result2 instanceof TestNodeFactory.TestNode);
    }

    /**
     * Helper method to create test tokens with specified properties.
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
        private boolean hasErrors = false;
        private String lastError = "";

        public TestKotlinPrattParser() {
            super(new TestTokenOperations(), null, new TestNodeFactory());  // Use 3-parameter constructor with NodeFactory
        }

        @Override
        public void error(String message, LocatableToken token) {
            hasErrors = true;
            lastError = message;
        }

        // NodeFactory handles node creation now, not the parser

        public boolean hasErrors() {
            return hasErrors;
        }

        public String getLastError() {
            return lastError;
        }
    }

    /**
     * Simple test implementation of TokenOperations.
     */
    private static class TestTokenOperations implements TokenOperations {
        @Override
        public LocatableToken nextToken() { return null; }

        @Override
        public LocatableToken LA(int distance) { return null; }

        @Override
        public void pushBack(LocatableToken token) {}

        @Override
        public LocatableToken getMostRecent() { return null; }
    }
}
