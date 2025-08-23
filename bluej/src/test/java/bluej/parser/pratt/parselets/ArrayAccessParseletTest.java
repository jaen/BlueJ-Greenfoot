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
import bluej.parser.pratt.Precedence;
import bluej.parser.pratt.TestNodeFactory;
import bluej.parser.pratt.TokenOperations;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Test cases for ArrayAccessParselet.
 *
 * Tests parsing of array access expressions including:
 * - Simple indexing: arr[0], list[index]
 * - Complex indices: arr[i + 1], matrix[row * cols + col]
 * - Multi-dimensional access: matrix[row][col][depth]
 * - String/collection access: str[position], map["key"]
 * - Chained access with other parselets: obj.getArray()[index]
 * - Error cases and malformed expressions
 * - Precedence handling
 * - Integration with NodeFactory
 *
 * @author BlueJ Team
 */
public class ArrayAccessParseletTest extends TestCase
{
    private ArrayAccessParselet arrayAccessParselet;
    private TestKotlinPrattParser testParser;

    @Override
    public void setUp() {
        arrayAccessParselet = new ArrayAccessParselet();
        testParser = new TestKotlinPrattParser();

        // Register necessary parselets for testing
        testParser.registerParselet(JavaTokenTypes.IDENT, new NameParselet());
        testParser.registerParselet(JavaTokenTypes.NUM_INT, new LiteralParselet());
        testParser.registerParselet(JavaTokenTypes.LBRACK, arrayAccessParselet);
    }

    @Test
    public void testSimpleIntegerIndex() {
        // Test: arr[0]
        ParsedNode array = createIdentifierNode("arr");
        LocatableToken lbrack = createToken(JavaTokenTypes.LBRACK, "[", 1, 4);
        LocatableToken index = createToken(JavaTokenTypes.NUM_INT, "0", 1, 5);
        LocatableToken rbrack = createToken(JavaTokenTypes.RBRACK, "]", 1, 6);

        // Setup parser to return index and closing bracket
        testParser.setTokenSequence(index, rbrack);

        ParsedNode result = arrayAccessParselet.parse(testParser, array, lbrack);

        assertNotNull("Array access with integer index should succeed", result);
        assertTrue("Should be a TestNode", result instanceof TestNodeFactory.TestNode);
        assertEquals("ArrayAccess", ((TestNodeFactory.TestNode) result).getTestNodeType());
        assertFalse("Parser should not have errors", testParser.hasErrors());
    }

    @Test
    public void testStringIndex() {
        // Test: map["key"]
        ParsedNode map = createIdentifierNode("map");
        LocatableToken lbrack = createToken(JavaTokenTypes.LBRACK, "[", 1, 4);
        LocatableToken index = createToken(JavaTokenTypes.STRING_LITERAL, "\"key\"", 1, 5);
        LocatableToken rbrack = createToken(JavaTokenTypes.RBRACK, "]", 1, 10);

        testParser.setTokenSequence(index, rbrack);

        ParsedNode result = arrayAccessParselet.parse(testParser, map, lbrack);

        assertNotNull("Array access with string index should succeed", result);
        assertTrue("Should be a TestNode", result instanceof TestNodeFactory.TestNode);
        assertEquals("ArrayAccess", ((TestNodeFactory.TestNode) result).getTestNodeType());
        assertFalse("Parser should not have errors", testParser.hasErrors());
    }

    @Test
    public void testIdentifierIndex() {
        // Test: arr[index]
        ParsedNode array = createIdentifierNode("arr");
        LocatableToken lbrack = createToken(JavaTokenTypes.LBRACK, "[", 1, 4);
        LocatableToken index = createToken(JavaTokenTypes.IDENT, "index", 1, 5);
        LocatableToken rbrack = createToken(JavaTokenTypes.RBRACK, "]", 1, 10);

        testParser.setTokenSequence(index, rbrack);

        ParsedNode result = arrayAccessParselet.parse(testParser, array, lbrack);

        assertNotNull("Array access with identifier index should succeed", result);
        assertTrue("Should be a TestNode", result instanceof TestNodeFactory.TestNode);
        assertEquals("ArrayAccess", ((TestNodeFactory.TestNode) result).getTestNodeType());
        assertFalse("Parser should not have errors", testParser.hasErrors());
    }

    @Test
    public void testChainedArrayAccess() {
        // Test chaining: arr[i], then [j] for multi-dimensional access
        // First access: arr[i]
        ParsedNode array = createIdentifierNode("arr");
        LocatableToken lbrack1 = createToken(JavaTokenTypes.LBRACK, "[", 1, 4);
        LocatableToken index1 = createToken(JavaTokenTypes.IDENT, "i", 1, 5);
        LocatableToken rbrack1 = createToken(JavaTokenTypes.RBRACK, "]", 1, 6);

        testParser.setTokenSequence(index1, rbrack1);

        ParsedNode firstAccess = arrayAccessParselet.parse(testParser, array, lbrack1);
        assertNotNull("First array access should succeed", firstAccess);

        // Second access: [j]
        LocatableToken lbrack2 = createToken(JavaTokenTypes.LBRACK, "[", 1, 7);
        LocatableToken index2 = createToken(JavaTokenTypes.IDENT, "j", 1, 8);
        LocatableToken rbrack2 = createToken(JavaTokenTypes.RBRACK, "]", 1, 9);

        testParser.setTokenSequence(index2, rbrack2);

        ParsedNode secondAccess = arrayAccessParselet.parse(testParser, firstAccess, lbrack2);
        assertNotNull("Chained array access should succeed", secondAccess);
        assertTrue("Should be a TestNode", secondAccess instanceof TestNodeFactory.TestNode);
        assertEquals("ArrayAccess", ((TestNodeFactory.TestNode) secondAccess).getTestNodeType());
    }

    @Test
    public void testMissingLeftOperand() {
        // Test error case: null array
        LocatableToken lbrack = createToken(JavaTokenTypes.LBRACK, "[", 1, 1);

        ParsedNode result = arrayAccessParselet.parse(testParser, null, lbrack);

        assertNull("Array access without array should fail", result);
        assertTrue("Parser should report error", testParser.hasErrors());
        assertTrue("Error should mention missing array expression",
                   testParser.getLastError().contains("Missing array expression"));
    }

    @Test
    public void testMissingIndex() {
        // Test: arr[ [no index]
        ParsedNode array = createIdentifierNode("arr");
        LocatableToken lbrack = createToken(JavaTokenTypes.LBRACK, "[", 1, 4);

        // No tokens available - end of input
        testParser.setEndOfInput(true);

        ParsedNode result = arrayAccessParselet.parse(testParser, array, lbrack);

        assertNull("Array access without index should fail", result);
        assertTrue("Parser should report error", testParser.hasErrors());
        assertTrue("Error should mention expected index expression",
                   testParser.getLastError().contains("Expected index expression"));
    }

    @Test
    public void testMissingClosingBracket() {
        // Test: arr[0 [no closing bracket]
        ParsedNode array = createIdentifierNode("arr");
        LocatableToken lbrack = createToken(JavaTokenTypes.LBRACK, "[", 1, 4);
        LocatableToken index = createToken(JavaTokenTypes.NUM_INT, "0", 1, 5);

        // Index present but no closing bracket
        testParser.setTokenSequence(index);
        testParser.setEndOfInput(true);

        ParsedNode result = arrayAccessParselet.parse(testParser, array, lbrack);

        assertNull("Array access without closing bracket should fail", result);
        assertTrue("Parser should report error", testParser.hasErrors());
        assertTrue("Error should mention end of input",
                   testParser.getLastError().toLowerCase().contains("end of input"));
    }

    @Test
    public void testWrongClosingToken() {
        // Test: arr[0) - wrong closing token
        ParsedNode array = createIdentifierNode("arr");
        LocatableToken lbrack = createToken(JavaTokenTypes.LBRACK, "[", 1, 4);
        LocatableToken index = createToken(JavaTokenTypes.NUM_INT, "0", 1, 5);
        LocatableToken wrongClosing = createToken(JavaTokenTypes.RPAREN, ")", 1, 6);

        testParser.setTokenSequence(index, wrongClosing);

        ParsedNode result = arrayAccessParselet.parse(testParser, array, lbrack);

        assertNull("Array access with wrong closing token should fail", result);
        assertTrue("Parser should report error", testParser.hasErrors());
        assertTrue("Error should mention expected ']'",
                   testParser.getLastError().contains("Expected ']'"));
    }

    @Test
    public void testInvalidToken() {
        // Test error case: wrong token type for array access
        ParsedNode array = createIdentifierNode("arr");
        LocatableToken wrongToken = createToken(JavaTokenTypes.COMMA, ",", 1, 1);

        ParsedNode result = arrayAccessParselet.parse(testParser, array, wrongToken);

        assertNull("Array access with wrong token should fail", result);
        assertTrue("Parser should report error", testParser.hasErrors());
        assertTrue("Error should mention expected '['",
                   testParser.getLastError().contains("Expected '['"));
    }

    @Test
    public void testNullToken() {
        // Test error case: null token
        ParsedNode array = createIdentifierNode("arr");

        ParsedNode result = arrayAccessParselet.parse(testParser, array, null);

        assertNull("Array access with null token should fail", result);
        assertTrue("Parser should report error", testParser.hasErrors());
        assertTrue("Error should mention expected '['",
                   testParser.getLastError().contains("Expected '['"));
    }

    @Test
    public void testPrecedenceLevel() {
        // Verify that ArrayAccessParselet has POSTFIX precedence
        int precedence = arrayAccessParselet.getPrecedence();
        assertEquals("ArrayAccessParselet should have POSTFIX precedence",
                     Precedence.POSTFIX.getValue(), precedence);
        assertTrue("Array access precedence should be higher than binary operators",
                   precedence > Precedence.ADDITIVE.getValue());
        assertTrue("Array access precedence should be higher than prefix operators",
                   precedence > Precedence.PREFIX.getValue());
    }

    @Test
    public void testNodeFactoryIntegration() {
        // Test that the parselet uses NodeFactory correctly
        ParsedNode array = createIdentifierNode("test");
        LocatableToken lbrack = createToken(JavaTokenTypes.LBRACK, "[", 1, 5);
        LocatableToken index = createToken(JavaTokenTypes.NUM_INT, "0", 1, 6);
        LocatableToken rbrack = createToken(JavaTokenTypes.RBRACK, "]", 1, 7);

        testParser.setTokenSequence(index, rbrack);

        ParsedNode result = arrayAccessParselet.parse(testParser, array, lbrack);

        assertNotNull("NodeFactory should create array access node", result);

        // Verify the factory was called with correct parameters
        TestNodeFactory factory = (TestNodeFactory) testParser.getNodeFactory();
        assertTrue("Factory should have recorded createArrayAccessNode call",
                   factory.wasCreateArrayAccessNodeCalled());
    }

    @Test
    public void testNodeFactoryFailure() {
        // Test handling of NodeFactory failures
        ParsedNode array = createIdentifierNode("test");
        LocatableToken lbrack = createToken(JavaTokenTypes.LBRACK, "[", 1, 5);
        LocatableToken index = createToken(JavaTokenTypes.NUM_INT, "0", 1, 6);
        LocatableToken rbrack = createToken(JavaTokenTypes.RBRACK, "]", 1, 7);

        // Configure factory to fail
        TestNodeFactory factory = (TestNodeFactory) testParser.getNodeFactory();
        factory.setShouldFail(true);

        testParser.setTokenSequence(index, rbrack);

        ParsedNode result = arrayAccessParselet.parse(testParser, array, lbrack);

        assertNull("Should return null when NodeFactory fails", result);
    }

    @Test
    public void testComplexIndexExpression() {
        // Test with complex index expressions that would be parsed recursively
        // This is a simplified test since our test parser doesn't handle full expressions
        ParsedNode array = createIdentifierNode("matrix");
        LocatableToken lbrack = createToken(JavaTokenTypes.LBRACK, "[", 1, 7);
        LocatableToken complexIndex = createToken(JavaTokenTypes.IDENT, "row", 1, 8); // Simplified
        LocatableToken rbrack = createToken(JavaTokenTypes.RBRACK, "]", 1, 11);

        testParser.setTokenSequence(complexIndex, rbrack);

        ParsedNode result = arrayAccessParselet.parse(testParser, array, lbrack);

        assertNotNull("Array access with complex index should succeed", result);
        assertTrue("Should be a TestNode", result instanceof TestNodeFactory.TestNode);
        assertEquals("ArrayAccess", ((TestNodeFactory.TestNode) result).getTestNodeType());
    }

    @Test
    public void testMultipleDimensionalAccess() {
        // Test three-dimensional access: arr[x][y][z]
        ParsedNode array = createIdentifierNode("arr");

        // First dimension: arr[x]
        LocatableToken lbrack1 = createToken(JavaTokenTypes.LBRACK, "[", 1, 4);
        LocatableToken index1 = createToken(JavaTokenTypes.IDENT, "x", 1, 5);
        LocatableToken rbrack1 = createToken(JavaTokenTypes.RBRACK, "]", 1, 6);

        testParser.setTokenSequence(index1, rbrack1);
        ParsedNode firstDim = arrayAccessParselet.parse(testParser, array, lbrack1);
        assertNotNull("First dimension access should succeed", firstDim);

        // Second dimension: [y]
        LocatableToken lbrack2 = createToken(JavaTokenTypes.LBRACK, "[", 1, 7);
        LocatableToken index2 = createToken(JavaTokenTypes.IDENT, "y", 1, 8);
        LocatableToken rbrack2 = createToken(JavaTokenTypes.RBRACK, "]", 1, 9);

        testParser.setTokenSequence(index2, rbrack2);
        ParsedNode secondDim = arrayAccessParselet.parse(testParser, firstDim, lbrack2);
        assertNotNull("Second dimension access should succeed", secondDim);

        // Third dimension: [z]
        LocatableToken lbrack3 = createToken(JavaTokenTypes.LBRACK, "[", 1, 10);
        LocatableToken index3 = createToken(JavaTokenTypes.IDENT, "z", 1, 11);
        LocatableToken rbrack3 = createToken(JavaTokenTypes.RBRACK, "]", 1, 12);

        testParser.setTokenSequence(index3, rbrack3);
        ParsedNode thirdDim = arrayAccessParselet.parse(testParser, secondDim, lbrack3);
        assertNotNull("Third dimension access should succeed", thirdDim);
        assertEquals("ArrayAccess", ((TestNodeFactory.TestNode) thirdDim).getTestNodeType());
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
     * Creates a test identifier node for use as array expressions.
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

            // Simple expression parsing for literals and identifiers
            if (token.getType() == JavaTokenTypes.NUM_INT || token.getType() == JavaTokenTypes.STRING_LITERAL) {
                return getNodeFactory().createLiteralNode(token);
            } else if (token.getType() == JavaTokenTypes.IDENT) {
                return getNodeFactory().createIdentifierNode(token);
            }

            return null;
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
