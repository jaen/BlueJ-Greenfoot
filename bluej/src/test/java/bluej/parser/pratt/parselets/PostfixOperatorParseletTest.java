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
 * Test cases for PostfixOperatorParselet.
 *
 * Tests parsing of postfix unary operators including:
 * - Increment/Decrement: ++, -- (postfix forms)
 * - Null assertion: !! (Kotlin-specific operator)
 * - Precedence handling
 * - Error cases
 *
 * @author BlueJ Team
 */
public class PostfixOperatorParseletTest
{
    private PostfixOperatorParselet postfixIncrementParselet;
    private PostfixOperatorParselet postfixDecrementParselet;
    private PostfixOperatorParselet nullAssertionParselet;
    private MockParser testParser;

    @Before
    public void setUp() {
        // Create parselets with appropriate precedence levels
        // Postfix operators have higher precedence than prefix (15 vs 13-14)
        postfixIncrementParselet = new PostfixOperatorParselet(15);  // Postfix increment
        postfixDecrementParselet = new PostfixOperatorParselet(15);  // Postfix decrement
        nullAssertionParselet = new PostfixOperatorParselet(15);     // Null assertion
        testParser = new MockParser();
    }

    /**
     * Test parsing postfix increment operator.
     */
    @Test
    public void testPostfixIncrement() {
        LocatableToken incToken = TestUtils.createToken(JavaTokenTypes.INC, "++", 1, 3);
        ParsedNode leftOperand = TestUtils.createIntLiteralNode("42", 1, 1);

        ParseResult<ParsedNode> result = postfixIncrementParselet.parse(testParser, leftOperand, incToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should parse postfix increment expression", node);
        assertTrue("Should be a TestNode", node instanceof TestNodeFactory.TestNode);

        TestNodeFactory.TestNode postfixNode = (TestNodeFactory.TestNode) node;
        assertEquals("Should be UnaryPostfix node", "UnaryPostfix", postfixNode.getTestNodeType());
        assertNotNull("Should have children", postfixNode.getChildren());
        assertEquals("Should have one child (operand)", 1, postfixNode.getChildren().size());
        assertSame("Left operand should be preserved", leftOperand, postfixNode.getChildren().get(0));
    }

    /**
     * Test parsing postfix decrement operator.
     */
    @Test
    public void testPostfixDecrement() {
        LocatableToken decToken = TestUtils.createToken(JavaTokenTypes.DEC, "--", 1, 3);
        ParsedNode leftOperand = TestUtils.createIntLiteralNode("42", 1, 1);

        ParseResult<ParsedNode> result = postfixDecrementParselet.parse(testParser, leftOperand, decToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should parse postfix decrement expression", node);
        assertTrue("Should be a TestNode", node instanceof TestNodeFactory.TestNode);

        TestNodeFactory.TestNode postfixNode = (TestNodeFactory.TestNode) node;
        assertEquals("Should be UnaryPostfix node", "UnaryPostfix", postfixNode.getTestNodeType());
        assertNotNull("Should have children", postfixNode.getChildren());
        assertEquals("Should have one child (operand)", 1, postfixNode.getChildren().size());
        assertSame("Left operand should be preserved", leftOperand, postfixNode.getChildren().get(0));
    }

    /**
     * Test parsing null assertion operator (Kotlin-specific).
     */
    @Test
    public void testNullAssertion() {
        LocatableToken nullAssertToken = TestUtils.createToken(JavaTokenTypes.NOT_EQUAL, "!!", 1, 3); // Using NOT_EQUAL as closest token
        ParsedNode leftOperand = TestUtils.createNullLiteralNode(1, 1);

        ParseResult<ParsedNode> result = nullAssertionParselet.parse(testParser, leftOperand, nullAssertToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should parse null assertion expression", node);
        assertTrue("Should be a TestNode", node instanceof TestNodeFactory.TestNode);

        TestNodeFactory.TestNode postfixNode = (TestNodeFactory.TestNode) node;
        assertEquals("Should be UnaryPostfix node", "UnaryPostfix", postfixNode.getTestNodeType());
        assertNotNull("Should have children", postfixNode.getChildren());
        assertEquals("Should have one child (operand)", 1, postfixNode.getChildren().size());
        assertSame("Left operand should be preserved", leftOperand, postfixNode.getChildren().get(0));
    }

    /**
     * Test that postfix operators have correct precedence values.
     */
    @Test
    public void testPrecedenceValues() {
        assertEquals("Postfix increment precedence should be 15", 15, postfixIncrementParselet.getPrecedence());
        assertEquals("Postfix decrement precedence should be 15", 15, postfixDecrementParselet.getPrecedence());
        assertEquals("Null assertion precedence should be 15", 15, nullAssertionParselet.getPrecedence());

        // Compare with prefix operator precedence (13-14)
        PrefixOperatorParselet prefixParselet = new PrefixOperatorParselet(13);
        assertTrue("Postfix should have higher precedence than prefix",
                postfixIncrementParselet.getPrecedence() > prefixParselet.getPrecedence());
    }

    /**
     * Test with different operand types.
     */
    @Test
    public void testWithDifferentOperands() {
        // Test with string literal
        ParsedNode stringOperand = TestUtils.createStringLiteralNode("\"test\"", 1, 1);
        LocatableToken incToken = TestUtils.createToken(JavaTokenTypes.INC, "++", 1, 7);

        ParseResult<ParsedNode> result1 = postfixIncrementParselet.parse(testParser, stringOperand, incToken);
        assertNotNull("Should return a result", result1);
        assertTrue("Should be successful", result1.isSuccess());
        ParsedNode node1 = result1.getValue();
        assertNotNull("Should parse with string operand", node1);
        assertTrue("Should be postfix node", node1 instanceof TestNodeFactory.TestNode);
        assertEquals("Should be UnaryPostfix node", "UnaryPostfix", ((TestNodeFactory.TestNode)node1).getTestNodeType());

        // Test with boolean literal
        ParsedNode boolOperand = TestUtils.createBooleanLiteralNode(true, 2, 1);
        LocatableToken decToken = TestUtils.createToken(JavaTokenTypes.DEC, "--", 2, 5);

        testParser.reset(); // Clear any previous errors
        ParseResult<ParsedNode> result2 = postfixDecrementParselet.parse(testParser, boolOperand, decToken);
        assertNotNull("Should return a result", result2);
        assertTrue("Should be successful", result2.isSuccess());
        ParsedNode node2 = result2.getValue();
        assertNotNull("Should parse with boolean operand", node2);
        assertTrue("Should be postfix node", node2 instanceof TestNodeFactory.TestNode);
        assertEquals("Should be UnaryPostfix node", "UnaryPostfix", ((TestNodeFactory.TestNode)node2).getTestNodeType());
    }

    /**
     * Test NodeFactory integration.
     */
    @Test
    public void testNodeFactoryIntegration() {
        LocatableToken incToken = TestUtils.createToken(JavaTokenTypes.INC, "++", 1, 3);
        ParsedNode leftOperand = TestUtils.createIntLiteralNode("42", 1, 1);

        ParseResult<ParsedNode> result = postfixIncrementParselet.parse(testParser, leftOperand, incToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should create node via factory", node);
        assertTrue("Should be TestNodeFactory node", node instanceof TestNodeFactory.TestNode);

        // Verify factory was used by checking node type and structure
        TestNodeFactory.TestNode postfixNode = (TestNodeFactory.TestNode) node;
        assertEquals("Should be UnaryPostfix node", "UnaryPostfix", postfixNode.getTestNodeType());
        assertNotNull("Should have children", postfixNode.getChildren());
        assertEquals("Should have one child (operand)", 1, postfixNode.getChildren().size());
        assertSame("Should preserve left operand", leftOperand, postfixNode.getChildren().get(0));
    }



    /**
     * Test chained postfix operators: x++-- should be parsed as (x++)--
     */
    @Test
    public void testChainedPostfixOperators() {
        // Start with a base operand
        ParsedNode baseOperand = TestUtils.createIdentifierNode("x", 1, 1);

        // Apply first postfix operator (++)
        LocatableToken incToken = TestUtils.createToken(JavaTokenTypes.INC, "++", 1, 2);
        ParseResult<ParsedNode> firstResult = postfixIncrementParselet.parse(testParser, baseOperand, incToken);
        assertNotNull("Should return first result", firstResult);
        assertTrue("First result should be successful", firstResult.isSuccess());
        ParsedNode firstNode = firstResult.getValue();

        // Apply second postfix operator (--) to the result
        LocatableToken decToken = TestUtils.createToken(JavaTokenTypes.DEC, "--", 1, 4);
        ParseResult<ParsedNode> finalResult = postfixDecrementParselet.parse(testParser, firstNode, decToken);

        assertNotNull("Should return final result", finalResult);
        assertTrue("Final result should be successful", finalResult.isSuccess());
        ParsedNode finalNode = finalResult.getValue();
        assertNotNull("Should parse chained postfix operators", finalNode);
        assertTrue("Final result should be postfix node", finalNode instanceof TestNodeFactory.TestNode);

        TestNodeFactory.TestNode outerPostfix = (TestNodeFactory.TestNode) finalNode;
        assertEquals("Should be UnaryPostfix node", "UnaryPostfix", outerPostfix.getTestNodeType());
        assertEquals("Should have one child", 1, outerPostfix.getChildren().size());

        assertTrue("Inner operand should also be postfix node",
                outerPostfix.getChildren().get(0) instanceof TestNodeFactory.TestNode);
        TestNodeFactory.TestNode innerPostfix =
                (TestNodeFactory.TestNode) outerPostfix.getChildren().get(0);
        assertEquals("Inner should be UnaryPostfix node", "UnaryPostfix", innerPostfix.getTestNodeType());
    }

    /**
     * Test precedence comparison with other operator types.
     */
    @Test
    public void testPrecedenceComparison() {
        PostfixOperatorParselet postfixParselet = new PostfixOperatorParselet(15);
        PrefixOperatorParselet prefixParselet = new PrefixOperatorParselet(13);

        assertTrue("Postfix should have higher precedence than prefix",
                postfixParselet.getPrecedence() > prefixParselet.getPrecedence());

        // Test with different precedence levels
        PostfixOperatorParselet higherPostfix = new PostfixOperatorParselet(16);
        assertTrue("Higher precedence postfix should bind tighter",
                higherPostfix.getPrecedence() > postfixParselet.getPrecedence());
    }

    /**
     * Test error handling with invalid token types.
     */
    @Test
    public void testInvalidTokenType() {
        LocatableToken invalidToken = TestUtils.createToken(JavaTokenTypes.PLUS, "+", 1, 1);
        ParsedNode leftOperand = TestUtils.createIntLiteralNode("42", 1, 1);

        ParseResult<ParsedNode> result = postfixIncrementParselet.parse(testParser, leftOperand, invalidToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful - PostfixOperatorParselet accepts any token", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should create a node even with non-postfix token", node);
        assertFalse("Should not report error in parser", testParser.hasErrors());
    }

    /**
     * Test toString method.
     */
    @Test
    public void testToString() {
        String result = postfixIncrementParselet.toString();
        assertNotNull("ToString should not return null", result);
        assertTrue("ToString should not be empty", result.length() > 0);
    }

    /**
     * Test with identifier operand.
     */
    @Test
    public void testWithIdentifierOperand() {
        ParsedNode identifierOperand = TestUtils.createIdentifierNode("variable", 1, 1);
        LocatableToken incToken = TestUtils.createToken(JavaTokenTypes.INC, "++", 1, 9);

        ParseResult<ParsedNode> result = postfixIncrementParselet.parse(testParser, identifierOperand, incToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should parse with identifier operand", node);
        assertTrue("Should be postfix node", node instanceof TestNodeFactory.TestNode);

        TestNodeFactory.TestNode postfixNode = (TestNodeFactory.TestNode) node;
        assertEquals("Should be UnaryPostfix node", "UnaryPostfix", postfixNode.getTestNodeType());
        assertEquals("Should preserve identifier operand", identifierOperand, postfixNode.getChildren().get(0));
    }
}
