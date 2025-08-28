/*
 This file is part of the BlueJ program.
 Copyright (C) 1999-2009,2010,2011,2012,2014,2016,2017,2018,2019,2021,2022,2023,2024  Michael Kolling and John Rosenberg

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
import bluej.parser.pratt.NodeFactory;
import bluej.parser.pratt.ParseResult;
import bluej.parser.pratt.Precedence;
import bluej.parser.pratt.TestNodeFactory;
import bluej.parser.pratt.TokenOperations;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Test cases for CallParselet.
 *
 * Tests parsing of function call expressions including:
 * - Empty argument lists: func()
 * - Single arguments: func(42)
 * - Multiple arguments: func(arg1, arg2, arg3)
 * - Nested expressions as arguments
 * - Error cases and malformed calls
 * - Precedence handling
 * - Chained function calls
 *
 * @author BlueJ Team
 */
public class CallParseletTest
{
    private CallParselet callParselet;
    private TestKotlinPrattParser testParser;

    @Before
    public void setUp() {
        callParselet = new CallParselet();
        testParser = new TestKotlinPrattParser();

        // Register necessary parselets for testing
        testParser.registerParselet(JavaTokenTypes.IDENT, new NameParselet());
        testParser.registerParselet(JavaTokenTypes.NUM_INT, new LiteralParselet());
        testParser.registerParselet(JavaTokenTypes.LPAREN, callParselet);
    }

    @Test
    public void testEmptyArgumentList() {
        // Test: func()
        ParsedNode function = createIdentifierNode("func");
        LocatableToken lparen = createToken(JavaTokenTypes.LPAREN, "(", 1, 5);
        LocatableToken rparen = createToken(JavaTokenTypes.RPAREN, ")", 1, 6);

        // Setup parser to expect closing paren
        testParser.setNextToken(rparen);

        ParseResult<ParsedNode> result = callParselet.parse(testParser, function, lparen);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should have a node value", node);
        assertTrue("Should be a TestNode", node instanceof TestNodeFactory.TestNode);
        assertEquals("Call", ((TestNodeFactory.TestNode) node).getTestNodeType());
        assertFalse("Parser should not have errors", testParser.hasErrors());
    }

    @Test
    public void testSingleArgument() {
        // Test: func(42)
        ParsedNode function = createIdentifierNode("func");
        LocatableToken lparen = createToken(JavaTokenTypes.LPAREN, "(", 1, 5);

        // Setup tokens for argument parsing
        LocatableToken arg = createToken(JavaTokenTypes.NUM_INT, "42", 1, 6);
        LocatableToken rparen = createToken(JavaTokenTypes.RPAREN, ")", 1, 8);

        testParser.setTokenSequence(arg, rparen);

        ParseResult<ParsedNode> result = callParselet.parse(testParser, function, lparen);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should have a node value", node);
        assertTrue("Should be a TestNode", node instanceof TestNodeFactory.TestNode);
        assertEquals("Call", ((TestNodeFactory.TestNode) node).getTestNodeType());
        assertFalse("Parser should not have errors", testParser.hasErrors());
    }

    @Test
    public void testMultipleArguments() {
        // Test: func(42, 84, 126)
        ParsedNode function = createIdentifierNode("func");
        LocatableToken lparen = createToken(JavaTokenTypes.LPAREN, "(", 1, 5);

        // Setup tokens: 42, 84, 126 with commas and closing paren
        LocatableToken arg1 = createToken(JavaTokenTypes.NUM_INT, "42", 1, 6);
        LocatableToken comma1 = createToken(JavaTokenTypes.COMMA, ",", 1, 8);
        LocatableToken arg2 = createToken(JavaTokenTypes.NUM_INT, "84", 1, 10);
        LocatableToken comma2 = createToken(JavaTokenTypes.COMMA, ",", 1, 12);
        LocatableToken arg3 = createToken(JavaTokenTypes.NUM_INT, "126", 1, 14);
        LocatableToken rparen = createToken(JavaTokenTypes.RPAREN, ")", 1, 17);

        testParser.setTokenSequence(arg1, comma1, arg2, comma2, arg3, rparen);

        ParseResult<ParsedNode> result = callParselet.parse(testParser, function, lparen);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should have a node value", node);
        assertTrue("Should be a TestNode", node instanceof TestNodeFactory.TestNode);
        assertEquals("Call", ((TestNodeFactory.TestNode) node).getTestNodeType());
        assertFalse("Parser should not have errors", testParser.hasErrors());
    }

    @Test
    public void testTrailingComma() {
        // Test: func(42, 84,) - trailing comma should be accepted
        ParsedNode function = createIdentifierNode("func");
        LocatableToken lparen = createToken(JavaTokenTypes.LPAREN, "(", 1, 5);

        LocatableToken arg1 = createToken(JavaTokenTypes.NUM_INT, "42", 1, 6);
        LocatableToken comma1 = createToken(JavaTokenTypes.COMMA, ",", 1, 8);
        LocatableToken arg2 = createToken(JavaTokenTypes.NUM_INT, "84", 1, 10);
        LocatableToken comma2 = createToken(JavaTokenTypes.COMMA, ",", 1, 12);
        LocatableToken rparen = createToken(JavaTokenTypes.RPAREN, ")", 1, 13);

        testParser.setTokenSequence(arg1, comma1, arg2, comma2, rparen);

        ParseResult<ParsedNode> result = callParselet.parse(testParser, function, lparen);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should have a node value", node);
        assertTrue("Should be a TestNode", node instanceof TestNodeFactory.TestNode);
        assertEquals("Call", ((TestNodeFactory.TestNode) node).getTestNodeType());
        assertFalse("Parser should not have errors", testParser.hasErrors());
    }

    @Test
    public void testMissingLeftOperand() {
        // Test error case: null function
        LocatableToken lparen = createToken(JavaTokenTypes.LPAREN, "(", 1, 1);

        ParseResult<ParsedNode> result = callParselet.parse(testParser, null, lparen);

        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure", result.isFailure());
        assertFalse("Parser should not report error", testParser.hasErrors());
        assertTrue("Should have errors in result", !result.getErrors().isEmpty());
        assertTrue("Error should mention missing function",
                   result.getErrors().get(0).message().contains("Missing function"));
    }

    @Test
    public void testInvalidToken() {
        // Test error case: wrong token type
        ParsedNode function = createIdentifierNode("func");
        LocatableToken wrongToken = createToken(JavaTokenTypes.COMMA, ",", 1, 1);

        ParseResult<ParsedNode> result = callParselet.parse(testParser, function, wrongToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure", result.isFailure());
        assertFalse("Parser should not report error", testParser.hasErrors());
        assertTrue("Should have errors in result", !result.getErrors().isEmpty());
        assertTrue("Error should mention expected '('",
                   result.getErrors().get(0).message().contains("Expected '('"));
    }

    @Test
    public void testMissingClosingParen() {
        // Test: func(42 [missing closing paren]
        ParsedNode function = createIdentifierNode("func");
        LocatableToken lparen = createToken(JavaTokenTypes.LPAREN, "(", 1, 5);

        LocatableToken arg = createToken(JavaTokenTypes.NUM_INT, "42", 1, 6);
        // No closing paren - end of input

        testParser.setTokenSequence(arg);
        testParser.setEndOfInput(true);

        ParseResult<ParsedNode> result = callParselet.parse(testParser, function, lparen);

        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure", result.isFailure());
        assertFalse("Parser should not report error", testParser.hasErrors());
        assertTrue("Should have errors in result", !result.getErrors().isEmpty());
        assertTrue("Error should mention end of input",
                   result.getErrors().get(0).message().toLowerCase().contains("end of input"));
    }

    @Test
    public void testMalformedArgumentList() {
        // Test: func(42 84) - missing comma
        ParsedNode function = createIdentifierNode("func");
        LocatableToken lparen = createToken(JavaTokenTypes.LPAREN, "(", 1, 5);

        LocatableToken arg1 = createToken(JavaTokenTypes.NUM_INT, "42", 1, 6);
        LocatableToken arg2 = createToken(JavaTokenTypes.NUM_INT, "84", 1, 9); // Missing comma before this

        testParser.setTokenSequence(arg1, arg2);

        ParseResult<ParsedNode> result = callParselet.parse(testParser, function, lparen);

        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure", result.isFailure());
        assertFalse("Parser should not report error", testParser.hasErrors());
        assertTrue("Should have errors in result", !result.getErrors().isEmpty());
        assertTrue("Error should mention expected comma or closing paren",
                   result.getErrors().get(0).message().contains("Expected"));
    }

    @Test
    public void testNullToken() {
        // Test error case: null token
        ParsedNode function = createIdentifierNode("func");

        ParseResult<ParsedNode> result = callParselet.parse(testParser, function, null);

        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure", result.isFailure());
        assertFalse("Parser should not report error", testParser.hasErrors());
        assertTrue("Should have errors in result", !result.getErrors().isEmpty());
    }

    @Test
    public void testPrecedenceLevel() {
        // Verify that CallParselet has POSTFIX precedence
        int precedence = callParselet.getPrecedence();
        assertEquals("CallParselet should have POSTFIX precedence",
                     Precedence.POSTFIX.getValue(), precedence);
        assertTrue("Call precedence should be higher than binary operators",
                   precedence > Precedence.ADDITIVE.getValue());
        assertTrue("Call precedence should be higher than prefix operators",
                   precedence > Precedence.PREFIX.getValue());
    }

    @Test
    public void testNodeFactoryIntegration() {
        // Test that the parselet uses NodeFactory correctly
        ParsedNode function = createIdentifierNode("test");
        LocatableToken lparen = createToken(JavaTokenTypes.LPAREN, "(", 1, 5);
        LocatableToken rparen = createToken(JavaTokenTypes.RPAREN, ")", 1, 6);

        testParser.setNextToken(rparen);

        ParseResult<ParsedNode> result = callParselet.parse(testParser, function, lparen);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("NodeFactory should create call node", node);

        // Verify the factory was called with correct parameters
        TestNodeFactory factory = (TestNodeFactory) testParser.getNodeFactory();
        assertTrue("Factory should have recorded createCallNode call",
                   factory.wasCreateCallNodeCalled());
    }

    @Test
    public void testNodeFactoryFailure() {
        // Test handling of NodeFactory failures
        ParsedNode function = createIdentifierNode("test");
        LocatableToken lparen = createToken(JavaTokenTypes.LPAREN, "(", 1, 5);
        LocatableToken rparen = createToken(JavaTokenTypes.RPAREN, ")", 1, 6);

        // Configure factory to fail
        TestNodeFactory factory = (TestNodeFactory) testParser.getNodeFactory();
        factory.setShouldFail(true);

        testParser.setNextToken(rparen);

        ParseResult<ParsedNode> result = callParselet.parse(testParser, function, lparen);

        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure when NodeFactory fails", result.isFailure());
    }

    @Test
    public void testChainedCalls() {
        // This tests that calls can be chained: func()()
        // The first call returns a function that can be called again
        ParsedNode function = createIdentifierNode("func");
        LocatableToken lparen1 = createToken(JavaTokenTypes.LPAREN, "(", 1, 5);
        LocatableToken rparen1 = createToken(JavaTokenTypes.RPAREN, ")", 1, 6);

        testParser.setNextToken(rparen1);

        ParseResult<ParsedNode> firstCallResult = callParselet.parse(testParser, function, lparen1);
        assertNotNull("Should return a result", firstCallResult);
        assertTrue("Should be successful", firstCallResult.isSuccess());
        ParsedNode firstCall = firstCallResult.getValue();
        assertNotNull("First call should succeed", firstCall);

        // Now chain another call
        LocatableToken lparen2 = createToken(JavaTokenTypes.LPAREN, "(", 1, 7);
        LocatableToken rparen2 = createToken(JavaTokenTypes.RPAREN, ")", 1, 8);

        testParser.setNextToken(rparen2);

        ParseResult<ParsedNode> secondCallResult = callParselet.parse(testParser, firstCall, lparen2);
        assertNotNull("Should return a result", secondCallResult);
        assertTrue("Should be successful", secondCallResult.isSuccess());
        ParsedNode secondCall = secondCallResult.getValue();
        assertNotNull("Chained call should succeed", secondCall);
        assertTrue("Should be a TestNode", secondCall instanceof TestNodeFactory.TestNode);
        assertEquals("Call", ((TestNodeFactory.TestNode) secondCall).getTestNodeType());
    }

    // Helper methods for test setup

    /**
     * Creates a test token with the specified type, text, and position.
     */
    private LocatableToken createToken(int type, String text, int line, int column) {
        bluej.parser.lexer.LineColPos begin = new bluej.parser.lexer.LineColPos(line, column, 0);
        bluej.parser.lexer.LineColPos end = new bluej.parser.lexer.LineColPos(line, column + text.length(), text.length());
        return new LocatableToken(type, text, begin, end);
    }

    /**
     * Creates a test identifier node for use as function expressions.
     */
    private ParsedNode createIdentifierNode(String name) {
        TestNodeFactory factory = new TestNodeFactory();
        LocatableToken token = createToken(JavaTokenTypes.IDENT, name, 1, 1);
        return factory.createIdentifierNode(token);
    }

    /**
     * Simple test implementation of KotlinPrattParser for testing.
     */
    private static class TestKotlinPrattParser extends KotlinPrattParser {
        private boolean hasErrors = false;
        private String lastError = "";
        private List<LocatableToken> tokenSequence = new ArrayList<>();
        private int tokenIndex = 0;
        private boolean endOfInput = false;

        public TestKotlinPrattParser() {
            super(new TestTokenOperations(), null, new TestNodeFactory());
        }

        public void setNextToken(LocatableToken token) {
            tokenSequence.clear();
            tokenSequence.add(token);
            tokenIndex = 0;
            endOfInput = false;
        }

        public void setTokenSequence(LocatableToken... tokens) {
            tokenSequence.clear();
            for (LocatableToken token : tokens) {
                tokenSequence.add(token);
            }
            tokenIndex = 0;
            endOfInput = false;
        }

        public void setEndOfInput(boolean endOfInput) {
            this.endOfInput = endOfInput;
        }

        @Override
        public @NotNull LocatableToken peek() {
            if (endOfInput && tokenIndex >= tokenSequence.size()) {
                return null;
            }
            if (tokenIndex < tokenSequence.size()) {
                return tokenSequence.get(tokenIndex);
            }
            return null;
        }

        @Override
        public @NotNull LocatableToken consume() {
            if (tokenIndex < tokenSequence.size()) {
                return tokenSequence.get(tokenIndex++);
            }
            return null;
        }

        @Override
        public ParsedNode parseExpression() {
            LocatableToken token = consume();
            if (token == null) {
                return null;
            }

            // Simple expression parsing for literals and identifiers
            if (token.getType() == JavaTokenTypes.NUM_INT) {
                return getNodeFactory().createLiteralNode(token);
            } else if (token.getType() == JavaTokenTypes.IDENT) {
                return getNodeFactory().createIdentifierNode(token);
            }

            return null;
        }

        @Override
        public ParseResult<ParsedNode> parseExpressionResult(int precedence) {
            LocatableToken token = consume();
            if (token == null) {
                return ParseResult.failure("No token available for expression", null);
            }

            // Simple expression parsing for literals and identifiers
            ParsedNode node = null;
            if (token.getType() == JavaTokenTypes.NUM_INT) {
                node = getNodeFactory().createLiteralNode(token);
            } else if (token.getType() == JavaTokenTypes.IDENT) {
                node = getNodeFactory().createIdentifierNode(token);
            }

            if (node != null) {
                return ParseResult.success(node);
            } else {
                return ParseResult.failure("Cannot parse token type: " + token.getType(), token);
            }
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

        public void registerParselet(int tokenType, Object parselet) {
            // Simple registration for testing
        }
    }

    /**
     * Test implementation of TokenOperations.
     */
    private static class TestTokenOperations implements TokenOperations {
        private LocatableToken mostRecent = null;

        @Override
        public @NotNull LocatableToken nextToken() { return null; }

        @Override
        public @NotNull LocatableToken LA(int distance) { return null; }

        public LocatableToken createToken(int type, String text, int line, int column) {
            bluej.parser.lexer.LineColPos begin = new bluej.parser.lexer.LineColPos(line, column, 0);
            bluej.parser.lexer.LineColPos end = new bluej.parser.lexer.LineColPos(line, column + text.length(), text.length());
            return new LocatableToken(type, text, begin, end);
        }

        public boolean isKeyword(LocatableToken token) {
            return false;
        }

        public boolean isIdentifier(LocatableToken token) {
            return token != null && token.getType() == JavaTokenTypes.IDENT;
        }

        public boolean isLiteral(LocatableToken token) {
            if (token == null) return false;
            int type = token.getType();
            return type == JavaTokenTypes.NUM_INT || type == JavaTokenTypes.NUM_FLOAT ||
                   type == JavaTokenTypes.STRING_LITERAL || type == JavaTokenTypes.CHAR_LITERAL ||
                   type == JavaTokenTypes.LITERAL_true || type == JavaTokenTypes.LITERAL_false ||
                   type == JavaTokenTypes.LITERAL_null;
        }

        public boolean isOperator(LocatableToken token) {
            return false;
        }

        public int getOperatorPrecedence(LocatableToken token) {
            return 0;
        }

        public boolean isRightAssociative(LocatableToken token) {
            return false;
        }

        @Override
        public void pushBack(LocatableToken token) {
            // Simple implementation for testing
        }

        @Override
        public LocatableToken getMostRecent() {
            return mostRecent;
        }
    }
}
