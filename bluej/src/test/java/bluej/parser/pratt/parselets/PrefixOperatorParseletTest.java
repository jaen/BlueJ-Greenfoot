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
import bluej.parser.pratt.ParseResult;
import bluej.parser.pratt.KotlinPrattParser;
import bluej.parser.pratt.NodeFactory;
import bluej.parser.pratt.TestNodeFactory;
import bluej.parser.pratt.TokenOperations;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test cases for PrefixOperatorParselet.
 *
 * Tests parsing of prefix unary operators including:
 * - Arithmetic: +, - (unary plus/minus)
 * - Logical: ! (logical NOT)
 * - Increment/Decrement: ++, -- (prefix forms)
 * - Precedence handling
 * - Error cases
 *
 * @author BlueJ Team
 */
public class PrefixOperatorParseletTest
{
    private PrefixOperatorParselet unaryPlusParselet;
    private PrefixOperatorParselet unaryMinusParselet;
    private PrefixOperatorParselet logicalNotParselet;
    private PrefixOperatorParselet prefixIncrementParselet;
    private PrefixOperatorParselet prefixDecrementParselet;
    private TestKotlinPrattParser testParser;

    @Before
    public void setUp() {
        // Create parselets with appropriate precedence levels
        unaryPlusParselet = new PrefixOperatorParselet(13);    // Unary plus
        unaryMinusParselet = new PrefixOperatorParselet(13);   // Unary minus
        logicalNotParselet = new PrefixOperatorParselet(13);   // Logical NOT
        prefixIncrementParselet = new PrefixOperatorParselet(14); // Prefix increment
        prefixDecrementParselet = new PrefixOperatorParselet(14); // Prefix decrement
        testParser = new TestKotlinPrattParser();
    }

    /**
     * Test parsing unary plus operator.
     */
    @Test
    public void testUnaryPlus() {
        LocatableToken plusToken = createToken(JavaTokenTypes.PLUS, "+", 1, 1);
        LocatableToken operandToken = createToken(JavaTokenTypes.NUM_INT, "42", 1, 2);

        // Set up parser to return the operand when parseExpression is called
        testParser.setNextOperand(operandToken);

        ParseResult<ParsedNode> result = unaryPlusParselet.parse(testParser, plusToken);

        assertNotNull("Should parse unary plus expression", result);
        assertTrue("Should be successful", result.isSuccess());

        ParsedNode node = result.getValue();
        assertNotNull("Should have a node", node);
        assertTrue("Should be a TestUnaryPrefixNode", node instanceof TestNodeFactory.TestUnaryPrefixNode);

        TestNodeFactory.TestUnaryPrefixNode unaryNode = (TestNodeFactory.TestUnaryPrefixNode) node;
        assertEquals("Operator should be +", "+", unaryNode.getOperator().getText());
        assertNotNull("Should have operand", unaryNode.getOperand());
    }

    /**
     * Test parsing unary minus operator.
     */
    @Test
    public void testUnaryMinus() {
        LocatableToken minusToken = createToken(JavaTokenTypes.MINUS, "-", 1, 1);
        LocatableToken operandToken = createToken(JavaTokenTypes.NUM_INT, "42", 1, 2);

        testParser.setNextOperand(operandToken);

        ParseResult<ParsedNode> result = unaryMinusParselet.parse(testParser, minusToken);

        assertNotNull("Should parse unary minus expression", result);
        assertTrue("Should be successful", result.isSuccess());

        ParsedNode node = result.getValue();
        assertNotNull("Should have a node", node);
        assertTrue("Should be a TestUnaryPrefixNode", node instanceof TestNodeFactory.TestUnaryPrefixNode);

        TestNodeFactory.TestUnaryPrefixNode unaryNode = (TestNodeFactory.TestUnaryPrefixNode) node;
        assertEquals("Operator should be -", "-", unaryNode.getOperator().getText());
        assertNotNull("Should have operand", unaryNode.getOperand());
    }

    /**
     * Test parsing logical NOT operator.
     */
    @Test
    public void testLogicalNot() {
        LocatableToken notToken = createToken(JavaTokenTypes.LNOT, "!", 1, 1);
        LocatableToken operandToken = createToken(JavaTokenTypes.LITERAL_true, "true", 1, 2);

        testParser.setNextOperand(operandToken);

        ParseResult<ParsedNode> result = logicalNotParselet.parse(testParser, notToken);

        assertNotNull("Should parse logical NOT expression", result);
        assertTrue("Should be successful", result.isSuccess());

        ParsedNode node = result.getValue();
        assertNotNull("Should have a node", node);
        assertTrue("Should be a TestUnaryPrefixNode", node instanceof TestNodeFactory.TestUnaryPrefixNode);

        TestNodeFactory.TestUnaryPrefixNode unaryNode = (TestNodeFactory.TestUnaryPrefixNode) node;
        assertEquals("Operator should be !", "!", unaryNode.getOperator().getText());
        assertNotNull("Should have operand", unaryNode.getOperand());
    }

    /**
     * Test parsing prefix increment operator.
     */
    @Test
    public void testPrefixIncrement() {
        LocatableToken incToken = createToken(JavaTokenTypes.INC, "++", 1, 1);
        LocatableToken operandToken = createToken(JavaTokenTypes.NUM_INT, "42", 1, 3);

        testParser.setNextOperand(operandToken);

        ParseResult<ParsedNode> result = prefixIncrementParselet.parse(testParser, incToken);

        assertNotNull("Should parse prefix increment expression", result);
        assertTrue("Should be successful", result.isSuccess());

        ParsedNode node = result.getValue();
        assertNotNull("Should have a node", node);
        assertTrue("Should be a TestUnaryPrefixNode", node instanceof TestNodeFactory.TestUnaryPrefixNode);

        TestNodeFactory.TestUnaryPrefixNode unaryNode = (TestNodeFactory.TestUnaryPrefixNode) node;
        assertEquals("Operator should be ++", "++", unaryNode.getOperator().getText());
        assertNotNull("Should have operand", unaryNode.getOperand());
    }

    /**
     * Test parsing prefix decrement operator.
     */
    @Test
    public void testPrefixDecrement() {
        LocatableToken decToken = createToken(JavaTokenTypes.DEC, "--", 1, 1);
        LocatableToken operandToken = createToken(JavaTokenTypes.NUM_INT, "42", 1, 3);

        testParser.setNextOperand(operandToken);

        ParseResult<ParsedNode> result = prefixDecrementParselet.parse(testParser, decToken);

        assertNotNull("Should parse prefix decrement expression", result);
        assertTrue("Should be successful", result.isSuccess());

        ParsedNode node = result.getValue();
        assertNotNull("Should have a node", node);
        assertTrue("Should be a TestUnaryPrefixNode", node instanceof TestNodeFactory.TestUnaryPrefixNode);

        TestNodeFactory.TestUnaryPrefixNode unaryNode = (TestNodeFactory.TestUnaryPrefixNode) node;
        assertEquals("Operator should be --", "--", unaryNode.getOperator().getText());
        assertNotNull("Should have operand", unaryNode.getOperand());
    }

    /**
     * Test error case: parsing without operand available.
     */
    @Test
    public void testMissingOperand() {
        LocatableToken plusToken = createToken(JavaTokenTypes.PLUS, "+", 1, 1);

        // Don't set up any operand - parseExpression will return null
        testParser.setNextOperand(null);

        ParseResult<ParsedNode> result = unaryPlusParselet.parse(testParser, plusToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure when operand missing", result.isFailure());
    }

    /**
     * Test that prefix operators have correct precedence values.
     */
    @Test
    public void testPrecedenceValues() {
        assertEquals("Arithmetic unary precedence should be 13", 13, unaryPlusParselet.getPrecedence());
        assertEquals("Arithmetic unary precedence should be 13", 13, unaryMinusParselet.getPrecedence());
        assertEquals("Logical NOT precedence should be 13", 13, logicalNotParselet.getPrecedence());
        assertEquals("Prefix increment precedence should be 14", 14, prefixIncrementParselet.getPrecedence());
        assertEquals("Prefix decrement precedence should be 14", 14, prefixDecrementParselet.getPrecedence());

        assertTrue("Increment should have higher precedence than arithmetic unary",
                prefixIncrementParselet.getPrecedence() > unaryPlusParselet.getPrecedence());
    }

    /**
     * Test NodeFactory integration: ensure parselets use factory for node creation.
     */
    @Test
    public void testNodeFactoryIntegration() {
        LocatableToken plusToken = createToken(JavaTokenTypes.PLUS, "+", 1, 1);
        LocatableToken operandToken = createToken(JavaTokenTypes.NUM_INT, "42", 1, 2);

        testParser.setNextOperand(operandToken);

        ParseResult<ParsedNode> result = unaryPlusParselet.parse(testParser, plusToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("NodeFactory should create unary prefix node", node);
        assertTrue("Should be TestNodeFactory node", node instanceof TestNodeFactory.TestNode);

        // Verify factory was used by checking node type and structure
        TestNodeFactory.TestNode unaryNode = (TestNodeFactory.TestNode) node;
        assertEquals("Should be UnaryPrefix node", "UnaryPrefix", unaryNode.getTestNodeType());
    }

    /**
     * Test various operator types with different operands.
     */
    @Test
    public void testWithDifferentOperands() {
        // Test unary minus with numeric literal
        LocatableToken minusToken = createToken(JavaTokenTypes.MINUS, "-", 1, 1);
        LocatableToken numToken = createToken(JavaTokenTypes.NUM_INT, "123", 1, 2);

        testParser.setNextOperand(numToken);
        ParseResult<ParsedNode> result1 = unaryMinusParselet.parse(testParser, minusToken);

        assertNotNull("Should return a result", result1);
        assertTrue("Should be successful", result1.isSuccess());
        ParsedNode node1 = result1.getValue();
        assertNotNull("Should parse with numeric operand", node1);
        assertTrue("Should be unary prefix node", node1 instanceof TestNodeFactory.TestNode);

        // Test logical NOT with boolean literal
        LocatableToken notToken = createToken(JavaTokenTypes.LNOT, "!", 2, 1);
        LocatableToken boolToken = createToken(JavaTokenTypes.LITERAL_false, "false", 2, 2);

        testParser.setNextOperand(boolToken);
        ParseResult<ParsedNode> result2 = logicalNotParselet.parse(testParser, notToken);

        assertNotNull("Should return a result", result2);
        assertTrue("Should be successful", result2.isSuccess());
        ParsedNode node2 = result2.getValue();
        assertNotNull("Should parse with boolean operand", node2);
        assertTrue("Should be unary prefix node", node2 instanceof TestNodeFactory.TestNode);
    }

    /**
     * Test error handling when NodeFactory fails.
     */
    @Test
    public void testNodeFactoryFailure() {
        LocatableToken plusToken = createToken(JavaTokenTypes.PLUS, "+", 1, 1);
        LocatableToken operandToken = createToken(JavaTokenTypes.NUM_INT, "42", 1, 2);

        testParser.setNextOperand(operandToken);
        testParser.setNodeFactoryFailure(true);  // Make NodeFactory return null

        ParseResult<ParsedNode> result = unaryPlusParselet.parse(testParser, plusToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure when NodeFactory fails", result.isFailure());
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
     * Test implementation of KotlinPrattParser for testing prefix operators.
     */
    private static class TestKotlinPrattParser extends KotlinPrattParser {
        private boolean hasErrors = false;
        private String lastError = "";
        private LocatableToken nextOperand = null;
        private boolean nodeFactoryFailure = false;

        public TestKotlinPrattParser() {
            super(new TestTokenOperations(), null, new TestNodeFactory());
        }

        @Override
        public void error(String message, LocatableToken token) {
            hasErrors = true;
            lastError = message;
        }

        /**
         * Override parseExpression to return a mock operand for testing.
         */
        @Override
        public ParsedNode parseExpression(int precedence) {
            if (nextOperand == null) {
                return null;  // Simulate missing operand
            }

            // Create a simple literal node as the operand using the NodeFactory
            LiteralParselet literalParselet = new LiteralParselet();
            ParseResult<ParsedNode> result = literalParselet.parse(this, nextOperand);
            return result != null && result.isSuccess() ? result.getValue() : null;
        }

        @Override
        public NodeFactory getNodeFactory() {
            if (nodeFactoryFailure) {
                return new FailingNodeFactory();
            }
            return super.getNodeFactory();
        }

        public void setNextOperand(LocatableToken operand) {
            this.nextOperand = operand;
        }

        public void setNodeFactoryFailure(boolean failure) {
            this.nodeFactoryFailure = failure;
        }

        public boolean hasErrors() {
            return hasErrors;
        }

        public String getLastError() {
            return lastError;
        }
    }

    /**
     * NodeFactory that always fails for testing error conditions.
     */
    private static class FailingNodeFactory implements NodeFactory {
        @Override
        public ParsedNode createLiteralNode(LocatableToken token) { return null; }

        @Override
        public ParsedNode createBinaryOperatorNode(ParsedNode left, LocatableToken operator, ParsedNode right) { return null; }

        @Override
        public ParsedNode createUnaryPrefixNode(LocatableToken operator, ParsedNode operand) { return null; }

        @Override
        public ParsedNode createUnaryPostfixNode(ParsedNode operand, LocatableToken operator) { return null; }

        @Override
        public ParsedNode createGroupNode(ParsedNode innerExpression) { return null; }

        @Override
        public ParsedNode createIdentifierNode(LocatableToken identifier) { return null; }

        @Override
        public ParsedNode createMemberAccessNode(ParsedNode object, LocatableToken memberName, boolean isSafeCall) { return null; }

        @Override
        public ParsedNode createCallNode(ParsedNode function, ParsedNode[] arguments) { return null; }

        @Override
        public ParsedNode createArrayAccessNode(ParsedNode array, ParsedNode index) { return null; }

        @Override
        public ParsedNode createThisNode(LocatableToken thisToken) { return null; }

        @Override
        public ParsedNode createSuperNode(LocatableToken superToken) { return null; }

        @Override
        public void reportError(String message, LocatableToken token) {}

        @Override
        public boolean hasErrors() { return false; }
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
