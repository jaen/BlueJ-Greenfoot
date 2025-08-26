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
import bluej.parser.pratt.ParseResult;
import bluej.parser.pratt.Precedence;
import bluej.parser.pratt.TestNodeFactory;
import bluej.parser.pratt.TokenOperations;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Test cases for MemberAccessParselet.
 *
 * Tests parsing of member access expressions including:
 * - Regular access: obj.property
 * - Safe access: obj?.property
 * - Chained access: obj.prop1.prop2
 * - Method access: obj.method (identifier only, calls handled by CallParselet)
 * - Error cases and malformed access expressions
 * - Precedence handling
 * - Integration with other parselets
 *
 * @author BlueJ Team
 */
public class MemberAccessParseletTest
{
    private MemberAccessParselet memberAccessParselet;
    private TestKotlinPrattParser testParser;

    @Before
    public void setUp() {
        memberAccessParselet = new MemberAccessParselet();
        testParser = new TestKotlinPrattParser();

        // Register necessary parselets for testing
        testParser.registerParselet(JavaTokenTypes.IDENT, new NameParselet());
        testParser.registerParselet(JavaTokenTypes.DOT, memberAccessParselet);
        testParser.registerParselet(JavaTokenTypes.SAFE_ACCESS, memberAccessParselet);
    }

    @Test
    public void testRegularMemberAccess() {
        // Test: obj.property
        ParsedNode object = createIdentifierNode("obj");
        LocatableToken dot = createToken(JavaTokenTypes.DOT, ".", 1, 4);
        LocatableToken property = createToken(JavaTokenTypes.IDENT, "property", 1, 5);

        // Setup parser to return the property token when peeked/consumed
        testParser.setNextToken(property);

        ParseResult<ParsedNode> result = memberAccessParselet.parse(testParser, object, dot);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should have a node value", node);
        assertTrue("Should be a TestNode", node instanceof TestNodeFactory.TestNode);
        assertEquals("MemberAccess", ((TestNodeFactory.TestNode) node).getTestNodeType());
        assertFalse("Parser should not have errors", testParser.hasErrors());
    }

    @Test
    public void testSafeMemberAccess() {
        // Test: obj?.property
        ParsedNode object = createIdentifierNode("obj");
        LocatableToken safeAccess = createToken(JavaTokenTypes.SAFE_ACCESS, "?.", 1, 4);
        LocatableToken property = createToken(JavaTokenTypes.IDENT, "property", 1, 6);

        testParser.setNextToken(property);

        ParseResult<ParsedNode> result = memberAccessParselet.parse(testParser, object, safeAccess);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should have a node value", node);
        assertTrue("Should be a TestNode", node instanceof TestNodeFactory.TestNode);
        assertEquals("MemberAccess", ((TestNodeFactory.TestNode) node).getTestNodeType());
        assertFalse("Parser should not have errors", testParser.hasErrors());
    }

    @Test
    public void testChainedMemberAccess() {
        // Test chaining: obj.prop1, then .prop2
        // First access: obj.prop1
        ParsedNode object = createIdentifierNode("obj");
        LocatableToken dot1 = createToken(JavaTokenTypes.DOT, ".", 1, 4);
        LocatableToken prop1 = createToken(JavaTokenTypes.IDENT, "prop1", 1, 5);

        testParser.setNextToken(prop1);

        ParseResult<ParsedNode> firstAccessResult = memberAccessParselet.parse(testParser, object, dot1);
        assertNotNull("Should return a result", firstAccessResult);
        assertTrue("Should be successful", firstAccessResult.isSuccess());
        ParsedNode firstAccess = firstAccessResult.getValue();
        assertNotNull("First member access should succeed", firstAccess);

        // Second access: .prop2
        LocatableToken dot2 = createToken(JavaTokenTypes.DOT, ".", 1, 10);
        LocatableToken prop2 = createToken(JavaTokenTypes.IDENT, "prop2", 1, 11);

        testParser.setNextToken(prop2);

        ParseResult<ParsedNode> secondAccessResult = memberAccessParselet.parse(testParser, firstAccess, dot2);
        assertNotNull("Should return a result", secondAccessResult);
        assertTrue("Should be successful", secondAccessResult.isSuccess());
        ParsedNode secondAccess = secondAccessResult.getValue();
        assertNotNull("Chained member access should succeed", secondAccess);
        assertTrue("Should be a TestNode", secondAccess instanceof TestNodeFactory.TestNode);
        assertEquals("MemberAccess", ((TestNodeFactory.TestNode) secondAccess).getTestNodeType());
    }

    @Test
    public void testMixedAccessTypes() {
        // Test: obj.regular?.safe
        ParsedNode object = createIdentifierNode("obj");
        LocatableToken dot = createToken(JavaTokenTypes.DOT, ".", 1, 4);
        LocatableToken regular = createToken(JavaTokenTypes.IDENT, "regular", 1, 5);

        testParser.setNextToken(regular);

        ParseResult<ParsedNode> regularAccessResult = memberAccessParselet.parse(testParser, object, dot);
        assertNotNull("Should return a result", regularAccessResult);
        assertTrue("Should be successful", regularAccessResult.isSuccess());
        ParsedNode regularAccess = regularAccessResult.getValue();
        assertNotNull("Regular access should succeed", regularAccess);

        // Now safe access
        LocatableToken safeAccess = createToken(JavaTokenTypes.SAFE_ACCESS, "?.", 1, 12);
        LocatableToken safeProp = createToken(JavaTokenTypes.IDENT, "safe", 1, 14);

        testParser.setNextToken(safeProp);

        ParseResult<ParsedNode> mixedAccessResult = memberAccessParselet.parse(testParser, regularAccess, safeAccess);
        assertNotNull("Should return a result", mixedAccessResult);
        assertTrue("Should be successful", mixedAccessResult.isSuccess());
        ParsedNode mixedAccess = mixedAccessResult.getValue();
        assertNotNull("Mixed access should succeed", mixedAccess);
        assertTrue("Should be a TestNode", mixedAccess instanceof TestNodeFactory.TestNode);
        assertEquals("MemberAccess", ((TestNodeFactory.TestNode) mixedAccess).getTestNodeType());
    }

    @Test
    public void testMissingLeftOperand() {
        // Test error case: null object
        LocatableToken dot = createToken(JavaTokenTypes.DOT, ".", 1, 1);

        ParseResult<ParsedNode> result = memberAccessParselet.parse(testParser, null, dot);

        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure", result.isFailure());
        assertFalse("Parser should not report error", testParser.hasErrors());
        assertTrue("Should have errors in result", !result.getErrors().isEmpty());
        assertTrue("Error should mention missing object",
                   result.getErrors().get(0).message().contains("Missing object"));
    }

    @Test
    public void testMissingMemberName() {
        // Test: obj. [no member name]
        ParsedNode object = createIdentifierNode("obj");
        LocatableToken dot = createToken(JavaTokenTypes.DOT, ".", 1, 4);

        // No token available after dot
        testParser.setEndOfInput(true);

        ParseResult<ParsedNode> result = memberAccessParselet.parse(testParser, object, dot);

        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure", result.isFailure());
        assertFalse("Parser should not report error", testParser.hasErrors());
        assertTrue("Should have errors in result", !result.getErrors().isEmpty());
        assertTrue("Error should mention expected member name",
                   result.getErrors().get(0).message().contains("Expected member name"));
    }

    @Test
    public void testInvalidMemberName() {
        // Test: obj.123 (number instead of identifier)
        ParsedNode object = createIdentifierNode("obj");
        LocatableToken dot = createToken(JavaTokenTypes.DOT, ".", 1, 4);
        LocatableToken invalidMember = createToken(JavaTokenTypes.NUM_INT, "123", 1, 5);

        testParser.setNextToken(invalidMember);

        ParseResult<ParsedNode> result = memberAccessParselet.parse(testParser, object, dot);

        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure", result.isFailure());
        assertFalse("Parser should not report error", testParser.hasErrors());
        assertTrue("Should have errors in result", !result.getErrors().isEmpty());
        assertTrue("Error should mention expected identifier",
                   result.getErrors().get(0).message().contains("Expected identifier"));
    }

    @Test
    public void testInvalidOperatorToken() {
        // Test error case: wrong token type
        ParsedNode object = createIdentifierNode("obj");
        LocatableToken wrongToken = createToken(JavaTokenTypes.COMMA, ",", 1, 1);

        ParseResult<ParsedNode> result = memberAccessParselet.parse(testParser, object, wrongToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure", result.isFailure());
        assertFalse("Parser should not report error", testParser.hasErrors());
        assertTrue("Should have errors in result", !result.getErrors().isEmpty());
        assertTrue("Error should mention expected '.' or '?.'",
                   result.getErrors().get(0).message().contains("Expected '.' or '?.'"));
    }

    @Test
    public void testNullToken() {
        // Test error case: null token
        ParsedNode object = createIdentifierNode("obj");

        ParseResult<ParsedNode> result = memberAccessParselet.parse(testParser, object, null);

        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure", result.isFailure());
        assertFalse("Parser should not report error", testParser.hasErrors());
        assertTrue("Should have errors in result", !result.getErrors().isEmpty());
        assertTrue("Error should mention missing access operator",
                   result.getErrors().get(0).message().contains("Missing access operator"));
    }

    @Test
    public void testPrecedenceLevel() {
        // Verify that MemberAccessParselet has POSTFIX precedence
        int precedence = memberAccessParselet.getPrecedence();
        assertEquals("MemberAccessParselet should have POSTFIX precedence",
                     Precedence.POSTFIX.getValue(), precedence);
        assertTrue("Member access precedence should be higher than binary operators",
                   precedence > Precedence.ADDITIVE.getValue());
        assertTrue("Member access precedence should be higher than prefix operators",
                   precedence > Precedence.PREFIX.getValue());
    }

    @Test
    public void testNodeFactoryIntegration() {
        // Test that the parselet uses NodeFactory correctly
        ParsedNode object = createIdentifierNode("test");
        LocatableToken dot = createToken(JavaTokenTypes.DOT, ".", 1, 5);
        LocatableToken property = createToken(JavaTokenTypes.IDENT, "property", 1, 6);

        testParser.setNextToken(property);

        ParseResult<ParsedNode> result = memberAccessParselet.parse(testParser, object, dot);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("NodeFactory should create member access node", node);

        // Verify the factory was called with correct parameters
        TestNodeFactory factory = (TestNodeFactory) testParser.getNodeFactory();
        assertTrue("Factory should have recorded createMemberAccessNode call",
                   factory.wasCreateMemberAccessNodeCalled());
    }

    @Test
    public void testNodeFactoryFailure() {
        // Test handling of NodeFactory failures
        ParsedNode object = createIdentifierNode("test");
        LocatableToken dot = createToken(JavaTokenTypes.DOT, ".", 1, 5);
        LocatableToken property = createToken(JavaTokenTypes.IDENT, "property", 1, 6);

        // Configure factory to fail
        TestNodeFactory factory = (TestNodeFactory) testParser.getNodeFactory();
        factory.setShouldFail(true);

        testParser.setNextToken(property);

        ParseResult<ParsedNode> result = memberAccessParselet.parse(testParser, object, dot);

        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure when NodeFactory fails", result.isFailure());
    }

    @Test
    public void testSafeCallDetection() {
        // Test that safe calls are properly detected and passed to NodeFactory
        ParsedNode object = createIdentifierNode("obj");
        LocatableToken safeAccess = createToken(JavaTokenTypes.SAFE_ACCESS, "?.", 1, 4);
        LocatableToken property = createToken(JavaTokenTypes.IDENT, "property", 1, 6);

        TestNodeFactory factory = (TestNodeFactory) testParser.getNodeFactory();
        testParser.setNextToken(property);

        ParseResult<ParsedNode> result = memberAccessParselet.parse(testParser, object, safeAccess);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Safe access should succeed", node);
        assertTrue("Factory should have recorded safe call",
                   factory.getLastSafeCallValue());
    }

    @Test
    public void testRegularCallDetection() {
        // Test that regular calls are properly detected
        ParsedNode object = createIdentifierNode("obj");
        LocatableToken dot = createToken(JavaTokenTypes.DOT, ".", 1, 4);
        LocatableToken property = createToken(JavaTokenTypes.IDENT, "property", 1, 5);

        TestNodeFactory factory = (TestNodeFactory) testParser.getNodeFactory();
        testParser.setNextToken(property);

        ParseResult<ParsedNode> result = memberAccessParselet.parse(testParser, object, dot);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Regular access should succeed", node);
        assertFalse("Factory should have recorded regular call (not safe)",
                    factory.getLastSafeCallValue());
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
     * Creates a test identifier node for use as object expressions.
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
        public LocatableToken peek() {
            if (endOfInput && tokenIndex >= tokenSequence.size()) {
                return null;
            }
            if (tokenIndex < tokenSequence.size()) {
                return tokenSequence.get(tokenIndex);
            }
            return null;
        }

        @Override
        public LocatableToken consume() {
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

            // Simple expression parsing for identifiers
            if (token.getType() == JavaTokenTypes.IDENT) {
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

            // Simple expression parsing for identifiers
            if (token.getType() == JavaTokenTypes.IDENT) {
                ParsedNode node = getNodeFactory().createIdentifierNode(token);
                return ParseResult.success(node);
            }

            return ParseResult.failure("Cannot parse token type: " + token.getType(), token);
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
        public LocatableToken nextToken() { return null; }

        @Override
        public LocatableToken LA(int distance) { return null; }

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
