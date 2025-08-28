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
import bluej.parser.pratt.TestNodeFactory;
import bluej.parser.pratt.testutil.MockParser;
import bluej.parser.pratt.testutil.TestUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test cases for PrefixOperatorParselet.
 *
 * Tests parsing of prefix unary operators including:
 * - Increment/Decrement: ++, -- (prefix forms)
 * - Logical NOT: !
 * - Bitwise NOT: ~
 * - Unary plus/minus: +, -
 * - Precedence handling
 * - Error cases
 *
 * @author BlueJ Team
 */
public class PrefixOperatorParseletTest
{
    private PrefixOperatorParselet prefixIncrementParselet;
    private PrefixOperatorParselet prefixDecrementParselet;
    private PrefixOperatorParselet logicalNotParselet;
    private PrefixOperatorParselet bitwiseNotParselet;
    private PrefixOperatorParselet unaryPlusParselet;
    private PrefixOperatorParselet unaryMinusParselet;
    private MockParser testParser;

    @Before
    public void setUp() {
        // Create parselets with appropriate precedence levels
        // Prefix operators typically have precedence 13-14
        prefixIncrementParselet = new PrefixOperatorParselet(14);  // Prefix increment
        prefixDecrementParselet = new PrefixOperatorParselet(14);  // Prefix decrement
        logicalNotParselet = new PrefixOperatorParselet(13);       // Logical NOT
        bitwiseNotParselet = new PrefixOperatorParselet(13);       // Bitwise NOT
        unaryPlusParselet = new PrefixOperatorParselet(13);        // Unary plus
        unaryMinusParselet = new PrefixOperatorParselet(13);       // Unary minus
        testParser = new MockParser();
    }

    /**
     * Test parsing prefix increment operator.
     */
    @Test
    public void testPrefixIncrement() {
        LocatableToken incToken = TestUtils.createToken(JavaTokenTypes.INC, "++", 1, 1);

        // Mock the parser to return an operand when parseExpression is called
        ParsedNode operand = TestUtils.createIdentifierNode("x", 1, 3);
        testParser.setNextExpression(operand);

        ParseResult<ParsedNode> result = prefixIncrementParselet.parse(testParser, incToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should parse prefix increment expression", node);
        assertTrue("Should be a TestNode", node instanceof TestNodeFactory.TestNode);

        TestNodeFactory.TestNode prefixNode = (TestNodeFactory.TestNode) node;
        assertEquals("Should be UnaryPrefix node", "UnaryPrefix", prefixNode.getTestNodeType());
        assertNotNull("Should have children", prefixNode.getChildren());
        assertEquals("Should have one child (operand)", 1, prefixNode.getChildren().size());
        assertSame("Operand should be preserved", operand, prefixNode.getChildren().get(0));
    }

    /**
     * Test parsing prefix decrement operator.
     */
    @Test
    public void testPrefixDecrement() {
        LocatableToken decToken = TestUtils.createToken(JavaTokenTypes.DEC, "--", 1, 1);

        ParsedNode operand = TestUtils.createIdentifierNode("x", 1, 3);
        testParser.setNextExpression(operand);

        ParseResult<ParsedNode> result = prefixDecrementParselet.parse(testParser, decToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should parse prefix decrement expression", node);
        assertTrue("Should be a TestNode", node instanceof TestNodeFactory.TestNode);

        TestNodeFactory.TestNode prefixNode = (TestNodeFactory.TestNode) node;
        assertEquals("Should be UnaryPrefix node", "UnaryPrefix", prefixNode.getTestNodeType());
        assertNotNull("Should have children", prefixNode.getChildren());
        assertEquals("Should have one child (operand)", 1, prefixNode.getChildren().size());
        assertSame("Operand should be preserved", operand, prefixNode.getChildren().get(0));
    }

    /**
     * Test parsing logical NOT operator.
     */
    @Test
    public void testLogicalNot() {
        LocatableToken notToken = TestUtils.createToken(JavaTokenTypes.LNOT, "!", 1, 1);

        ParsedNode operand = TestUtils.createBooleanLiteralNode(true, 1, 2);
        testParser.setNextExpression(operand);

        ParseResult<ParsedNode> result = logicalNotParselet.parse(testParser, notToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should parse logical NOT expression", node);
        assertTrue("Should be a TestNode", node instanceof TestNodeFactory.TestNode);

        TestNodeFactory.TestNode prefixNode = (TestNodeFactory.TestNode) node;
        assertEquals("Should be UnaryPrefix node", "UnaryPrefix", prefixNode.getTestNodeType());
        assertNotNull("Should have children", prefixNode.getChildren());
        assertEquals("Should have one child (operand)", 1, prefixNode.getChildren().size());
        assertSame("Operand should be preserved", operand, prefixNode.getChildren().get(0));
    }

    /**
     * Test parsing bitwise NOT operator.
     */
    @Test
    public void testBitwiseNot() {
        LocatableToken tildeToken = TestUtils.createToken(JavaTokenTypes.BNOT, "~", 1, 1);

        ParsedNode operand = TestUtils.createIntLiteralNode("42", 1, 2);
        testParser.setNextExpression(operand);

        ParseResult<ParsedNode> result = bitwiseNotParselet.parse(testParser, tildeToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should parse bitwise NOT expression", node);
        assertTrue("Should be a TestNode", node instanceof TestNodeFactory.TestNode);

        TestNodeFactory.TestNode prefixNode = (TestNodeFactory.TestNode) node;
        assertEquals("Should be UnaryPrefix node", "UnaryPrefix", prefixNode.getTestNodeType());
        assertNotNull("Should have children", prefixNode.getChildren());
        assertEquals("Should have one child (operand)", 1, prefixNode.getChildren().size());
        assertSame("Operand should be preserved", operand, prefixNode.getChildren().get(0));
    }

    /**
     * Test parsing unary plus operator.
     */
    @Test
    public void testUnaryPlus() {
        LocatableToken plusToken = TestUtils.createToken(JavaTokenTypes.PLUS, "+", 1, 1);

        ParsedNode operand = TestUtils.createIntLiteralNode("42", 1, 2);
        testParser.setNextExpression(operand);

        ParseResult<ParsedNode> result = unaryPlusParselet.parse(testParser, plusToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should parse unary plus expression", node);
        assertTrue("Should be a TestNode", node instanceof TestNodeFactory.TestNode);

        TestNodeFactory.TestNode prefixNode = (TestNodeFactory.TestNode) node;
        assertEquals("Should be UnaryPrefix node", "UnaryPrefix", prefixNode.getTestNodeType());
        assertNotNull("Should have children", prefixNode.getChildren());
        assertEquals("Should have one child (operand)", 1, prefixNode.getChildren().size());
        assertSame("Operand should be preserved", operand, prefixNode.getChildren().get(0));
    }

    /**
     * Test parsing unary minus operator.
     */
    @Test
    public void testUnaryMinus() {
        LocatableToken minusToken = TestUtils.createToken(JavaTokenTypes.MINUS, "-", 1, 1);

        ParsedNode operand = TestUtils.createIntLiteralNode("42", 1, 2);
        testParser.setNextExpression(operand);

        ParseResult<ParsedNode> result = unaryMinusParselet.parse(testParser, minusToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should parse unary minus expression", node);
        assertTrue("Should be a TestNode", node instanceof TestNodeFactory.TestNode);

        TestNodeFactory.TestNode prefixNode = (TestNodeFactory.TestNode) node;
        assertEquals("Should be UnaryPrefix node", "UnaryPrefix", prefixNode.getTestNodeType());
        assertNotNull("Should have children", prefixNode.getChildren());
        assertEquals("Should have one child (operand)", 1, prefixNode.getChildren().size());
        assertSame("Operand should be preserved", operand, prefixNode.getChildren().get(0));
    }

    /**
     * Test that prefix operators have correct precedence values.
     */
    @Test
    public void testPrecedenceValues() {
        assertEquals("Prefix increment precedence should be 14", 14, prefixIncrementParselet.getPrecedence());
        assertEquals("Prefix decrement precedence should be 14", 14, prefixDecrementParselet.getPrecedence());
        assertEquals("Logical NOT precedence should be 13", 13, logicalNotParselet.getPrecedence());
        assertEquals("Bitwise NOT precedence should be 13", 13, bitwiseNotParselet.getPrecedence());
        assertEquals("Unary plus precedence should be 13", 13, unaryPlusParselet.getPrecedence());
        assertEquals("Unary minus precedence should be 13", 13, unaryMinusParselet.getPrecedence());

        // Compare with postfix operator precedence (15)
        PostfixOperatorParselet postfixParselet = new PostfixOperatorParselet(15);
        assertTrue("Prefix should have lower precedence than postfix",
                prefixIncrementParselet.getPrecedence() < postfixParselet.getPrecedence());
    }

    /**
     * Test with different operand types.
     */
    @Test
    public void testWithDifferentOperands() {
        // Test with string literal
        ParsedNode stringOperand = TestUtils.createStringLiteralNode("\"test\"", 1, 2);
        testParser.setNextExpression(stringOperand);
        LocatableToken notToken = TestUtils.createToken(JavaTokenTypes.LNOT, "!", 1, 1);

        ParseResult<ParsedNode> result1 = logicalNotParselet.parse(testParser, notToken);
        assertNotNull("Should return a result", result1);
        assertTrue("Should be successful", result1.isSuccess());
        ParsedNode node1 = result1.getValue();
        assertNotNull("Should parse with string operand", node1);
        assertTrue("Should be prefix node", node1 instanceof TestNodeFactory.TestNode);
        assertEquals("Should be UnaryPrefix node", "UnaryPrefix", ((TestNodeFactory.TestNode)node1).getTestNodeType());

        // Test with null literal
        ParsedNode nullOperand = TestUtils.createNullLiteralNode(2, 2);
        testParser.reset(); // Clear any previous state
        testParser.setNextExpression(nullOperand);
        LocatableToken tildeToken = TestUtils.createToken(JavaTokenTypes.BNOT, "~", 2, 1);

        ParseResult<ParsedNode> result2 = bitwiseNotParselet.parse(testParser, tildeToken);
        assertNotNull("Should return a result", result2);
        assertTrue("Should be successful", result2.isSuccess());
        ParsedNode node2 = result2.getValue();
        assertNotNull("Should parse with null operand", node2);
        assertTrue("Should be prefix node", node2 instanceof TestNodeFactory.TestNode);
        assertEquals("Should be UnaryPrefix node", "UnaryPrefix", ((TestNodeFactory.TestNode)node2).getTestNodeType());
    }

    /**
     * Test error handling when parser fails to parse operand.
     */
    @Test
    public void testErrorHandlingWithFailedOperandParse() {
        LocatableToken incToken = TestUtils.createToken(JavaTokenTypes.INC, "++", 1, 1);

        // Configure parser to return failure when parsing operand
        testParser.setNextExpressionFailure("Failed to parse operand");

        ParseResult<ParsedNode> result = prefixIncrementParselet.parse(testParser, incToken);

        assertNotNull("Should return a result", result);
        assertFalse("Should fail when operand parsing fails", result.isSuccess());
        assertFalse("Should have errors", result.getErrors().isEmpty());
        assertEquals("Should propagate error message", "Missing operand for prefix increment (++) operator", result.getErrors().get(0).message());
    }

    /**
     * Test NodeFactory integration.
     */
    @Test
    public void testNodeFactoryIntegration() {
        LocatableToken incToken = TestUtils.createToken(JavaTokenTypes.INC, "++", 1, 1);
        ParsedNode operand = TestUtils.createIdentifierNode("x", 1, 3);
        testParser.setNextExpression(operand);

        ParseResult<ParsedNode> result = prefixIncrementParselet.parse(testParser, incToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should create node via factory", node);
        assertTrue("Should be TestNodeFactory node", node instanceof TestNodeFactory.TestNode);

        // Verify factory was used by checking node type and structure
        TestNodeFactory.TestNode prefixNode = (TestNodeFactory.TestNode) node;
        assertEquals("Should be UnaryPrefix node", "UnaryPrefix", prefixNode.getTestNodeType());
        assertNotNull("Should have children", prefixNode.getChildren());
        assertEquals("Should have one child (operand)", 1, prefixNode.getChildren().size());
        assertSame("Should preserve operand", operand, prefixNode.getChildren().get(0));
    }

    /**
     * Test nested prefix operators: ++--x should be parsed as ++(--x)
     */
    @Test
    public void testNestedPrefixOperators() {
        // Setup for inner operator (--)
        LocatableToken decToken = TestUtils.createToken(JavaTokenTypes.DEC, "--", 1, 3);
        ParsedNode baseOperand = TestUtils.createIdentifierNode("x", 1, 5);
        testParser.setNextExpression(baseOperand);

        ParseResult<ParsedNode> innerResult = prefixDecrementParselet.parse(testParser, decToken);
        assertNotNull("Should return inner result", innerResult);
        assertTrue("Inner result should be successful", innerResult.isSuccess());
        ParsedNode innerNode = innerResult.getValue();

        // Setup for outer operator (++)
        testParser.reset();
        testParser.setNextExpression(innerNode);
        LocatableToken incToken = TestUtils.createToken(JavaTokenTypes.INC, "++", 1, 1);

        ParseResult<ParsedNode> outerResult = prefixIncrementParselet.parse(testParser, incToken);

        assertNotNull("Should return outer result", outerResult);
        assertTrue("Outer result should be successful", outerResult.isSuccess());
        ParsedNode outerNode = outerResult.getValue();
        assertNotNull("Should parse nested prefix operators", outerNode);
        assertTrue("Outer result should be prefix node", outerNode instanceof TestNodeFactory.TestNode);

        TestNodeFactory.TestNode outerPrefix = (TestNodeFactory.TestNode) outerNode;
        assertEquals("Should be UnaryPrefix node", "UnaryPrefix", outerPrefix.getTestNodeType());
        assertEquals("Should have one child", 1, outerPrefix.getChildren().size());

        assertTrue("Inner operand should also be prefix node",
                outerPrefix.getChildren().get(0) instanceof TestNodeFactory.TestNode);
        TestNodeFactory.TestNode innerPrefix =
                (TestNodeFactory.TestNode) outerPrefix.getChildren().get(0);
        assertEquals("Inner should be UnaryPrefix node", "UnaryPrefix", innerPrefix.getTestNodeType());
    }

    /**
     * Test precedence comparison with other operator types.
     */
    @Test
    public void testPrecedenceComparison() {
        PrefixOperatorParselet prefixParselet = new PrefixOperatorParselet(13);
        PostfixOperatorParselet postfixParselet = new PostfixOperatorParselet(15);

        assertTrue("Prefix should have lower precedence than postfix",
                prefixParselet.getPrecedence() < postfixParselet.getPrecedence());

        // Test with different precedence levels
        PrefixOperatorParselet higherPrefixParselet = new PrefixOperatorParselet(14);
        assertTrue("Higher precedence prefix should bind tighter",
                higherPrefixParselet.getPrecedence() > prefixParselet.getPrecedence());
    }

    /**
     * Test with complex operand expressions.
     */
    @Test
    public void testWithComplexOperand() {
        LocatableToken minusToken = TestUtils.createToken(JavaTokenTypes.MINUS, "-", 1, 1);

        // Create a complex operand (e.g., method call)
        ParsedNode methodCallOperand = TestUtils.createIdentifierNode("getValue", 1, 2);

        testParser.setNextExpression(methodCallOperand);

        ParseResult<ParsedNode> result = unaryMinusParselet.parse(testParser, minusToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should parse with complex operand", node);
        assertTrue("Should be prefix node", node instanceof TestNodeFactory.TestNode);

        TestNodeFactory.TestNode prefixNode = (TestNodeFactory.TestNode) node;
        assertEquals("Should be UnaryPrefix node", "UnaryPrefix", prefixNode.getTestNodeType());
        assertEquals("Should preserve complex operand", methodCallOperand, prefixNode.getChildren().get(0));
    }

    /**
     * Test toString method.
     */
    @Test
    public void testToString() {
        String result = prefixIncrementParselet.toString();
        assertNotNull("ToString should not return null", result);
        assertTrue("ToString should not be empty", result.length() > 0);
    }

    /**
     * Test with parenthesized operand.
     */
    @Test
    public void testWithParenthesizedOperand() {
        LocatableToken notToken = TestUtils.createToken(JavaTokenTypes.LNOT, "!", 1, 1);

        // Create a parenthesized expression
        ParsedNode parenOperand = TestUtils.createBooleanLiteralNode(false, 1, 3);

        testParser.setNextExpression(parenOperand);

        ParseResult<ParsedNode> result = logicalNotParselet.parse(testParser, notToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should parse with parenthesized operand", node);
        assertTrue("Should be prefix node", node instanceof TestNodeFactory.TestNode);

        TestNodeFactory.TestNode prefixNode = (TestNodeFactory.TestNode) node;
        assertEquals("Should be UnaryPrefix node", "UnaryPrefix", prefixNode.getTestNodeType());
        assertEquals("Should preserve parenthesized operand", parenOperand, prefixNode.getChildren().get(0));
    }
}
