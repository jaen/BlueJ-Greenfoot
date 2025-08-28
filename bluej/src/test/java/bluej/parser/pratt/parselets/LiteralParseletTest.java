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
import bluej.parser.pratt.ParseResult;
import bluej.parser.pratt.TestNodeFactory;
import bluej.parser.pratt.TokenOperations;
import bluej.parser.pratt.testutil.MockParser;
import bluej.parser.pratt.testutil.MockTokenOperations;
import bluej.parser.pratt.testutil.TestUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

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
public class LiteralParseletTest {

    private LiteralParselet parselet;
    private MockParser testParser;

    @Before
    public void setUp() {
        parselet = new LiteralParselet();
        testParser = new MockParser();
    }

    @Test
    public void testParseIntegerLiteral() {
        LocatableToken token = TestUtils.createToken(JavaTokenTypes.NUM_INT, "42", 1, 1);
        ParseResult<ParsedNode> result = parselet.parse(testParser, token);

        // With NodeFactory, parselets now create nodes
        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should create an AST node for integer literal", node);
        assertFalse("No errors should be reported for valid tokens", testParser.hasErrors());
        // canHandle method removed - parse behavior is sufficient validation
    }

    @Test
    public void testParseLongLiteral() {
        LocatableToken token = TestUtils.createToken(JavaTokenTypes.NUM_LONG, "42L", 1, 1);
        ParseResult<ParsedNode> result = parselet.parse(testParser, token);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should create an AST node for long literal", node);
        assertFalse("No errors should be reported for valid tokens", testParser.hasErrors());
        assertTrue("Should be a TestNode", node instanceof TestNodeFactory.TestNode);
    }

    @Test
    public void testParseFloatLiteral() {
        LocatableToken token = TestUtils.createToken(JavaTokenTypes.NUM_FLOAT, "3.14f", 1, 1);
        ParseResult<ParsedNode> result = parselet.parse(testParser, token);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should create an AST node for float literal", node);
        assertFalse("No errors should be reported for valid tokens", testParser.hasErrors());
        assertTrue("Should be a TestNode", node instanceof TestNodeFactory.TestNode);
    }

    @Test
    public void testParseDoubleLiteral() {
        LocatableToken token = TestUtils.createToken(JavaTokenTypes.NUM_DOUBLE, "3.14", 1, 1);
        ParseResult<ParsedNode> result = parselet.parse(testParser, token);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should create an AST node for double literal", node);
        assertFalse("No errors should be reported for valid tokens", testParser.hasErrors());
        assertTrue("Should be a TestNode", node instanceof TestNodeFactory.TestNode);
    }

    @Test
    public void testParseStringLiteral() {
        LocatableToken token = TestUtils.createToken(JavaTokenTypes.STRING_LITERAL, "\"hello world\"", 1, 1);
        ParseResult<ParsedNode> result = parselet.parse(testParser, token);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should create an AST node for string literal", node);
        assertFalse("No errors should be reported for valid tokens", testParser.hasErrors());
        assertTrue("Should be a TestNode", node instanceof TestNodeFactory.TestNode);
    }

    @Test
    public void testParseMultilineStringLiteral() {
        LocatableToken token = TestUtils.createToken(JavaTokenTypes.STRING_LITERAL_MULTILINE, "\"\"\"hello\nworld\"\"\"", 1, 1);
        ParseResult<ParsedNode> result = parselet.parse(testParser, token);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should create an AST node for multiline string literal", node);
        assertFalse("No errors should be reported for valid tokens", testParser.hasErrors());
        assertTrue("Should be a TestNode", node instanceof TestNodeFactory.TestNode);
    }

    @Test
    public void testParseCharacterLiteral() {
        LocatableToken token = TestUtils.createToken(JavaTokenTypes.CHAR_LITERAL, "'c'", 1, 1);
        ParseResult<ParsedNode> result = parselet.parse(testParser, token);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should create an AST node for character literal", node);
        assertFalse("No errors should be reported for valid tokens", testParser.hasErrors());
        assertTrue("Should be a TestNode", node instanceof TestNodeFactory.TestNode);
    }

    @Test
    public void testParseBooleanTrueLiteral() {
        LocatableToken token = TestUtils.createToken(JavaTokenTypes.LITERAL_true, "true", 1, 1);
        ParseResult<ParsedNode> result = parselet.parse(testParser, token);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should create an AST node for boolean true literal", node);
        assertFalse("No errors should be reported for valid tokens", testParser.hasErrors());
        assertTrue("Should be a TestNode", node instanceof TestNodeFactory.TestNode);
    }

    @Test
    public void testParseBooleanFalseLiteral() {
        LocatableToken token = TestUtils.createToken(JavaTokenTypes.LITERAL_false, "false", 1, 1);
        ParseResult<ParsedNode> result = parselet.parse(testParser, token);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should create an AST node for boolean false literal", node);
        assertFalse("No errors should be reported for valid tokens", testParser.hasErrors());
        assertTrue("Should be a TestNode", node instanceof TestNodeFactory.TestNode);
    }

    @Test
    public void testParseNullLiteral() {
        LocatableToken token = TestUtils.createToken(JavaTokenTypes.LITERAL_null, "null", 1, 1);
        ParseResult<ParsedNode> result = parselet.parse(testParser, token);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should create an AST node for null literal", node);
        assertFalse("No errors should be reported for valid tokens", testParser.hasErrors());
        assertTrue("Should be a TestNode", node instanceof TestNodeFactory.TestNode);
    }



    @Test
    public void testHandleNonLiteralToken() {
        LocatableToken token = TestUtils.createToken(JavaTokenTypes.IDENT, "identifier", 1, 1);
        ParseResult<ParsedNode> result = parselet.parse(testParser, token);

        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure", result.isFailure());
        assertFalse("Should not report error in parser", testParser.hasErrors());
        assertTrue("Should have errors in result", !result.getErrors().isEmpty());
        assertTrue("Should report literal token error", result.getErrors().get(0).message().contains("Expected literal token"));
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
            MockParser freshParser = new MockParser();
            LocatableToken token = TestUtils.createToken(tokenType, "test", 1, 1);
            // Test that valid literal tokens create nodes
            ParseResult<ParsedNode> result = parselet.parse(freshParser, token);
            assertNotNull("Should return a result", result);
            assertTrue("Should be successful for valid literal tokens", result.isSuccess());
            ParsedNode node = result.getValue();
            assertNotNull("Should create AST nodes for valid literal tokens", node);
            assertFalse("Should not produce errors for valid literal tokens", freshParser.hasErrors());
            assertTrue("Should be a TestNode", node instanceof TestNodeFactory.TestNode);
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
            MockParser freshParser = new MockParser();
            LocatableToken token = TestUtils.createToken(tokenType, "test", 1, 1);
            // Test that invalid tokens produce errors when parsed
            ParseResult<ParsedNode> result = parselet.parse(freshParser, token);
            assertNotNull("Should return a result", result);
            assertTrue("Should be a failure for non-literal token", result.isFailure());
            assertFalse("Should not report error in parser", freshParser.hasErrors());
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
        LocatableToken token1 = TestUtils.createToken(JavaTokenTypes.NUM_INT, "42", 1, 1);
        LocatableToken token2 = TestUtils.createToken(JavaTokenTypes.STRING_LITERAL, "\"test\"", 2, 1);

        ParseResult<ParsedNode> result1 = parselet.parse(testParser, token1);
        ParseResult<ParsedNode> result2 = parselet.parse(testParser, token2);

        assertNotNull("First parse should return a result", result1);
        assertNotNull("Second parse should return a result", result2);
        assertTrue("First parse should be successful", result1.isSuccess());
        assertTrue("Second parse should be successful", result2.isSuccess());
        assertNotNull("First parse should create an AST node", result1.getValue());
        assertNotNull("Second parse should create an AST node", result2.getValue());
        assertFalse("No errors should be reported", testParser.hasErrors());
        assertTrue("First should be a TestNode", result1.getValue() instanceof TestNodeFactory.TestNode);
        assertTrue("Second should be a TestNode", result2.getValue() instanceof TestNodeFactory.TestNode);
    }
}
