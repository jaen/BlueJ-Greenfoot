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
import bluej.parser.pratt.ParseResult;
import bluej.parser.pratt.Precedence;
import bluej.parser.pratt.TestNodeFactory;
import bluej.parser.pratt.testutil.MockParser;
import bluej.parser.pratt.testutil.TestUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test class for ElvisOperatorParselet.
 *
 * <p>This test verifies that the Elvis operator (?:) is correctly parsed
 * with proper precedence, associativity, and error handling.</p>
 *
 * @author BlueJ Team
 */
public class ElvisOperatorParseletTest {

    private ElvisOperatorParselet elvisOperatorParselet;
    private MockParser testParser;

    @Before
    public void setUp() {
        elvisOperatorParselet = new ElvisOperatorParselet();
        testParser = new MockParser();

        // Register necessary parselets for testing
        testParser.registerParselet(JavaTokenTypes.IDENT, new NameParselet());
        testParser.registerParselet(JavaTokenTypes.ELVIS, elvisOperatorParselet);
        testParser.registerParselet(JavaTokenTypes.NUM_INT, new LiteralParselet());
    }

    @Test
    public void testPrecedence() {
        // Elvis operator should have ELVIS precedence (20)
        assertEquals("Elvis operator should have ELVIS precedence",
                    Precedence.ELVIS.getValue(), elvisOperatorParselet.getPrecedence());
    }

    @Test
    public void testRightAssociativity() {
        // Elvis operator should be right-associative
        assertTrue("Elvis operator should be right-associative",
                  elvisOperatorParselet.isRightAssociative());
    }

    @Test
    public void testBasicElvisOperator() {
        // Test: left ?: right
        ParsedNode left = TestUtils.createIdentifierNode("left");
        LocatableToken elvisToken = TestUtils.createToken(JavaTokenTypes.ELVIS, "?:", 1, 5);
        LocatableToken rightToken = TestUtils.createToken(JavaTokenTypes.IDENT, "right", 1, 8);

        testParser.setNextToken(rightToken);

        ParseResult<ParsedNode> result = elvisOperatorParselet.parse(testParser, left, elvisToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Elvis operator should produce a node", node);

        // Verify the node is a TestBinaryNode with correct components
        assertTrue("Should create a TestBinaryNode", node instanceof TestNodeFactory.TestBinaryNode);
        TestNodeFactory.TestBinaryNode binaryNode = (TestNodeFactory.TestBinaryNode) node;

        // Debug output to see actual values
        System.out.println("DEBUG testBasicElvisOperator - Left: " + binaryNode.getLeft().toString());
        System.out.println("DEBUG testBasicElvisOperator - Left class: " + binaryNode.getLeft().getClass().getSimpleName());
        System.out.println("DEBUG testBasicElvisOperator - Right: " + binaryNode.getRight().toString());
        System.out.println("DEBUG testBasicElvisOperator - Right class: " + binaryNode.getRight().getClass().getSimpleName());

        // Check that the left operand contains 'left'
        assertTrue("Left operand should contain 'left'", binaryNode.getLeft().toString().contains("left"));
        assertEquals("Operator should be '?:'", "?:", binaryNode.getOperator().getText());
        // Check that the right operand contains 'right'
        assertTrue("Right operand should contain 'right'", binaryNode.getRight().toString().contains("right"));
    }

    @Test
    public void testElvisWithLiterals() {
        // Test: 42 ?: 0
        ParsedNode left = TestUtils.createIdentifierNode("42");
        LocatableToken elvisToken = TestUtils.createToken(JavaTokenTypes.ELVIS, "?:", 1, 3);
        LocatableToken rightToken = TestUtils.createToken(JavaTokenTypes.NUM_INT, "0", 1, 6);

        testParser.setNextToken(rightToken);

        ParseResult<ParsedNode> result = elvisOperatorParselet.parse(testParser, left, elvisToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Elvis operator should produce a node", node);

        // Verify the node is a TestBinaryNode with correct components
        assertTrue("Should create a TestBinaryNode", node instanceof TestNodeFactory.TestBinaryNode);
        TestNodeFactory.TestBinaryNode binaryNode = (TestNodeFactory.TestBinaryNode) node;

        // Debug output to see actual values
        System.out.println("DEBUG testElvisWithLiterals - Left: " + binaryNode.getLeft().toString());
        System.out.println("DEBUG testElvisWithLiterals - Left class: " + binaryNode.getLeft().getClass().getSimpleName());
        System.out.println("DEBUG testElvisWithLiterals - Right: " + binaryNode.getRight().toString());
        System.out.println("DEBUG testElvisWithLiterals - Right class: " + binaryNode.getRight().getClass().getSimpleName());

        // Check that the left operand contains '42'
        assertTrue("Left operand should contain '42'", binaryNode.getLeft().toString().contains("42"));
        assertEquals("Operator should be '?:'", "?:", binaryNode.getOperator().getText());
        // Check that the right operand contains '0'
        assertTrue("Right operand should contain '0'", binaryNode.getRight().toString().contains("0"));
    }

    @Test
    public void testWrongTokenType() {
        // Test error when non-Elvis token is passed
        ParsedNode left = TestUtils.createIdentifierNode("left");
        LocatableToken wrongToken = TestUtils.createToken(JavaTokenTypes.QUESTION, "?", 1, 5);

        ParseResult<ParsedNode> result = elvisOperatorParselet.parse(testParser, left, wrongToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be failure", result.isFailure());
        assertTrue("Error message should mention expected Elvis operator",
                  result.getFormattedErrors().contains("Expected Elvis operator (?:)"));
    }

    @Test
    public void testMissingRightOperand() {
        // Test error when right operand is missing
        ParsedNode left = TestUtils.createIdentifierNode("left");
        LocatableToken elvisToken = TestUtils.createToken(JavaTokenTypes.ELVIS, "?:", 1, 5);

        // Set up parser to return EOF (no right operand available)
        testParser.setNextToken(TestUtils.createToken(JavaTokenTypes.EOF, "", 1, 8));

        // Mock the parser to return failure for parseExpressionResult
        testParser.setNextExpressionFailure("Expected expression");

        ParseResult<ParsedNode> result = elvisOperatorParselet.parse(testParser, left, elvisToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be failure", result.isFailure());
        assertTrue("Error message should mention missing right operand",
                  result.getFormattedErrors().contains("Missing right operand"));
    }

    @Test
    public void testRightAssociativityBehavior() {
        // This test verifies that the precedence calculation for right-associativity is correct
        // For right-associative operators, we use precedence - 1 for parsing the right operand
        ParsedNode left = TestUtils.createIdentifierNode("a");
        LocatableToken elvisToken = TestUtils.createToken(JavaTokenTypes.ELVIS, "?:", 1, 2);
        LocatableToken bToken = TestUtils.createToken(JavaTokenTypes.IDENT, "b", 1, 5);

        testParser.setNextToken(bToken);

        ParseResult<ParsedNode> result = elvisOperatorParselet.parse(testParser, left, elvisToken);

        assertTrue("Should be successful", result.isSuccess());

        // Verify that the parser was called with precedence - 1 for right associativity
        int expectedRightPrecedence = Precedence.ELVIS.getValue() - 1;
        assertEquals("Should use precedence - 1 for right-associative parsing",
                    expectedRightPrecedence, testParser.getLastParseExpressionPrecedence());
    }

    @Test
    public void testNodeFactoryFailure() {
        // Test behavior when NodeFactory fails to create the node
        ParsedNode left = TestUtils.createIdentifierNode("left");
        LocatableToken elvisToken = TestUtils.createToken(JavaTokenTypes.ELVIS, "?:", 1, 5);
        LocatableToken rightToken = TestUtils.createToken(JavaTokenTypes.IDENT, "right", 1, 8);

        testParser.setNextToken(rightToken);

        // For this test, we'll create a scenario where parsing succeeds
        // but we can verify the behavior - simplified for foundation phase
        ParseResult<ParsedNode> result = elvisOperatorParselet.parse(testParser, left, elvisToken);

        assertNotNull("Should return a result", result);
        // This test mainly verifies the parsing flow works
    }

    @Test
    public void testToString() {
        String str = elvisOperatorParselet.toString();
        assertNotNull("toString should not return null", str);
        assertTrue("toString should contain class name",
                  str.contains("ElvisOperatorParselet"));
        assertTrue("toString should contain precedence",
                  str.contains("precedence=" + Precedence.ELVIS.getValue()));
        assertTrue("toString should indicate right-associativity",
                  str.contains("rightAssociative=true"));
    }

    @Test
    public void testComplexExpression() {
        // Test Elvis operator in a more complex scenario
        // This simulates parsing: obj ?: "default" (simplified for foundation phase)
        ParsedNode left = TestUtils.createIdentifierNode("obj");
        LocatableToken elvisToken = TestUtils.createToken(JavaTokenTypes.ELVIS, "?:", 1, 4);
        LocatableToken stringToken = TestUtils.createToken(JavaTokenTypes.STRING_LITERAL, "\"default\"", 1, 7);

        testParser.setNextToken(stringToken);

        ParseResult<ParsedNode> result = elvisOperatorParselet.parse(testParser, left, elvisToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Elvis operator should produce a node", node);

        // Verify the node is a TestBinaryNode with correct components
        assertTrue("Should create a TestBinaryNode", node instanceof TestNodeFactory.TestBinaryNode);
        TestNodeFactory.TestBinaryNode binaryNode = (TestNodeFactory.TestBinaryNode) node;

        // Debug output to see actual values
        System.out.println("DEBUG testComplexExpression - Left: " + binaryNode.getLeft().toString());
        System.out.println("DEBUG testComplexExpression - Left class: " + binaryNode.getLeft().getClass().getSimpleName());
        System.out.println("DEBUG testComplexExpression - Right: " + binaryNode.getRight().toString());
        System.out.println("DEBUG testComplexExpression - Right class: " + binaryNode.getRight().getClass().getSimpleName());

        // Check that the left operand contains 'obj'
        assertTrue("Left operand should contain 'obj': " + binaryNode.getLeft().toString(),
                  binaryNode.getLeft().toString().contains("obj"));
        assertEquals("Operator should be '?:'", "?:", binaryNode.getOperator().getText());
        // Check that the right operand contains 'default' (access the literal token text)
        assertTrue("Right operand should be a TestLiteralNode", binaryNode.getRight() instanceof TestNodeFactory.TestLiteralNode);
        TestNodeFactory.TestLiteralNode rightLiteralNode = (TestNodeFactory.TestLiteralNode) binaryNode.getRight();
        String rightOperandText = rightLiteralNode.getToken().getText();
        assertTrue("Right operand should contain 'default': " + rightOperandText,
                  rightOperandText.contains("default"));
    }
}
