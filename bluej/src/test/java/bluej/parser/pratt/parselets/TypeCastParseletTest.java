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
 * Test class for TypeCastParselet.
 *
 * <p>This test verifies that the type cast operator (as) is correctly parsed
 * with proper precedence, associativity, and error handling.</p>
 *
 * @author BlueJ Team
 */
public class TypeCastParseletTest {

    private TypeCastParselet typeCastParselet;
    private MockParser testParser;

    @Before
    public void setUp() {
        typeCastParselet = new TypeCastParselet();
        testParser = new MockParser();

        // Register necessary parselets for testing
        testParser.registerParselet(JavaTokenTypes.IDENT, new NameParselet());
        testParser.registerParselet(JavaTokenTypes.LITERAL_as, typeCastParselet);
        testParser.registerParselet(JavaTokenTypes.NUM_INT, new LiteralParselet());
    }

    @Test
    public void testPrecedence() {
        // Type cast operator should have TYPE_CAST precedence (120)
        assertEquals("Type cast operator should have TYPE_CAST precedence",
                    Precedence.TYPE_CAST.getValue(), typeCastParselet.getPrecedence());
    }

    @Test
    public void testLeftAssociativity() {
        // Type cast operator should be left-associative
        assertFalse("Type cast operator should be left-associative",
                   typeCastParselet.isRightAssociative());
    }

    @Test
    public void testBasicTypeCast() {
        // Test: obj as String
        ParsedNode left = TestUtils.createIdentifierNode("obj");
        LocatableToken asToken = TestUtils.createToken(JavaTokenTypes.LITERAL_as, "as", 1, 4);
        LocatableToken rightToken = TestUtils.createToken(JavaTokenTypes.IDENT, "String", 1, 7);

        testParser.setNextToken(rightToken);

        ParseResult<ParsedNode> result = typeCastParselet.parse(testParser, left, asToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Type cast operator should produce a node", node);

        // Verify the node is a TestBinaryNode with correct components
        assertTrue("Should create a TestBinaryNode", node instanceof TestNodeFactory.TestBinaryNode);
        TestNodeFactory.TestBinaryNode binaryNode = (TestNodeFactory.TestBinaryNode) node;

        // Debug output to see actual values
        System.out.println("DEBUG testBasicTypeCast - Left: " + binaryNode.getLeft().toString());
        System.out.println("DEBUG testBasicTypeCast - Left class: " + binaryNode.getLeft().getClass().getSimpleName());
        System.out.println("DEBUG testBasicTypeCast - Right: " + binaryNode.getRight().toString());
        System.out.println("DEBUG testBasicTypeCast - Right class: " + binaryNode.getRight().getClass().getSimpleName());

        // Check that the left operand contains 'obj'
        assertTrue("Left operand should contain 'obj'", binaryNode.getLeft().toString().contains("obj"));
        assertEquals("Operator should be 'as'", "as", binaryNode.getOperator().getText());
        // Check that the right operand contains 'String'
        assertTrue("Right operand should contain 'String'", binaryNode.getRight().toString().contains("String"));
    }

    @Test
    public void testTypeCastWithComplexType() {
        // Test: value as List
        ParsedNode left = TestUtils.createIdentifierNode("value");
        LocatableToken asToken = TestUtils.createToken(JavaTokenTypes.LITERAL_as, "as", 1, 6);
        LocatableToken rightToken = TestUtils.createToken(JavaTokenTypes.IDENT, "List", 1, 9);

        testParser.setNextToken(rightToken);

        ParseResult<ParsedNode> result = typeCastParselet.parse(testParser, left, asToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Type cast operator should produce a node", node);

        // Verify the node is a TestBinaryNode with correct components
        assertTrue("Should create a TestBinaryNode", node instanceof TestNodeFactory.TestBinaryNode);
        TestNodeFactory.TestBinaryNode binaryNode = (TestNodeFactory.TestBinaryNode) node;

        // Debug output to see actual values
        System.out.println("DEBUG testTypeCastWithComplexType - Left: " + binaryNode.getLeft().toString());
        System.out.println("DEBUG testTypeCastWithComplexType - Left class: " + binaryNode.getLeft().getClass().getSimpleName());
        System.out.println("DEBUG testTypeCastWithComplexType - Right: " + binaryNode.getRight().toString());
        System.out.println("DEBUG testTypeCastWithComplexType - Right class: " + binaryNode.getRight().getClass().getSimpleName());

        // Check that the left operand contains 'value'
        assertTrue("Left operand should contain 'value'", binaryNode.getLeft().toString().contains("value"));
        assertEquals("Operator should be 'as'", "as", binaryNode.getOperator().getText());
        // Check that the right operand contains 'List'
        assertTrue("Right operand should contain 'List'", binaryNode.getRight().toString().contains("List"));
    }

    @Test
    public void testWrongTokenType() {
        // Test error when non-as token is passed
        ParsedNode left = TestUtils.createIdentifierNode("obj");
        LocatableToken wrongToken = TestUtils.createToken(JavaTokenTypes.LITERAL_is, "is", 1, 4);

        ParseResult<ParsedNode> result = typeCastParselet.parse(testParser, left, wrongToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be failure", result.isFailure());
        assertTrue("Error message should mention expected 'as' operator",
                  result.getFormattedErrors().contains("Expected type cast operator ('as')"));
    }

    @Test
    public void testMissingRightOperand() {
        // Test error when right operand is missing
        ParsedNode left = TestUtils.createIdentifierNode("obj");
        LocatableToken asToken = TestUtils.createToken(JavaTokenTypes.LITERAL_as, "as", 1, 4);

        // Set up parser to return EOF (no right operand available)
        testParser.setNextToken(TestUtils.createToken(JavaTokenTypes.EOF, "", 1, 7));

        // Mock the parser to return failure for parseExpressionResult
        testParser.setNextExpressionFailure("Expected target type");

        ParseResult<ParsedNode> result = typeCastParselet.parse(testParser, left, asToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be failure", result.isFailure());
        assertTrue("Error message should mention missing target type",
                  result.getFormattedErrors().contains("Missing target type"));
    }

    @Test
    public void testLeftAssociativityBehavior() {
        // This test verifies that the precedence calculation for left-associativity is correct
        // For left-associative operators, we use the same precedence for parsing the right operand
        ParsedNode left = TestUtils.createIdentifierNode("obj");
        LocatableToken asToken = TestUtils.createToken(JavaTokenTypes.LITERAL_as, "as", 1, 4);
        LocatableToken typeToken = TestUtils.createToken(JavaTokenTypes.IDENT, "String", 1, 7);

        testParser.setNextToken(typeToken);

        ParseResult<ParsedNode> result = typeCastParselet.parse(testParser, left, asToken);

        assertTrue("Should be successful", result.isSuccess());

        // Verify that the parser was called with same precedence for left-associative parsing
        int expectedRightPrecedence = Precedence.TYPE_CAST.getValue();
        assertEquals("Should use same precedence for left-associative parsing",
                    expectedRightPrecedence, testParser.getLastParseExpressionPrecedence());
    }

    @Test
    public void testIsTypeCastOperatorUtility() {
        // Test the utility method for recognizing type cast operators
        assertTrue("Should recognize 'as' as type cast operator",
                  typeCastParselet.isTypeCastOperator(JavaTokenTypes.LITERAL_as));
        assertFalse("Should not recognize 'is' as type cast operator",
                   typeCastParselet.isTypeCastOperator(JavaTokenTypes.LITERAL_is));
        assertFalse("Should not recognize '+' as type cast operator",
                   typeCastParselet.isTypeCastOperator(JavaTokenTypes.PLUS));
    }

    @Test
    public void testSafeCastDetection() {
        // Test safe cast detection (should return false for basic 'as' operator)
        LocatableToken asToken = TestUtils.createToken(JavaTokenTypes.LITERAL_as, "as", 1, 4);
        assertFalse("Should return false for 'as' operator (unsafe cast)",
                   typeCastParselet.isSafeCast(asToken));
    }

    @Test
    public void testCastTypeDescription() {
        // Test cast type description
        LocatableToken asToken = TestUtils.createToken(JavaTokenTypes.LITERAL_as, "as", 1, 4);
        String description = typeCastParselet.getCastTypeDescription(asToken);
        assertEquals("Should describe 'as' as unsafe cast", "unsafe cast (as)", description);
    }

    @Test
    public void testToString() {
        String str = typeCastParselet.toString();
        assertNotNull("toString should not return null", str);
        assertTrue("toString should contain class name",
                  str.contains("TypeCastParselet"));
        assertTrue("toString should contain precedence",
                  str.contains("precedence=" + Precedence.TYPE_CAST.getValue()));
        assertTrue("toString should indicate left-associativity",
                  str.contains("leftAssociative=true"));
    }

    @Test
    public void testTypeCastWithLiteral() {
        // Test: 42 as Double (casting literal to type)
        ParsedNode left = TestUtils.createIdentifierNode("42");
        LocatableToken asToken = TestUtils.createToken(JavaTokenTypes.LITERAL_as, "as", 1, 3);
        LocatableToken typeToken = TestUtils.createToken(JavaTokenTypes.IDENT, "Double", 1, 6);

        testParser.setNextToken(typeToken);

        ParseResult<ParsedNode> result = typeCastParselet.parse(testParser, left, asToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Type cast operator should produce a node", node);

        // Verify the node is a TestBinaryNode with correct components
        assertTrue("Should create a TestBinaryNode", node instanceof TestNodeFactory.TestBinaryNode);
        TestNodeFactory.TestBinaryNode binaryNode = (TestNodeFactory.TestBinaryNode) node;

        // Debug output to see actual values
        System.out.println("DEBUG testTypeCastWithLiteral - Left: " + binaryNode.getLeft().toString());
        System.out.println("DEBUG testTypeCastWithLiteral - Left class: " + binaryNode.getLeft().getClass().getSimpleName());
        System.out.println("DEBUG testTypeCastWithLiteral - Right: " + binaryNode.getRight().toString());
        System.out.println("DEBUG testTypeCastWithLiteral - Right class: " + binaryNode.getRight().getClass().getSimpleName());

        // Check that the left operand contains '42'
        assertTrue("Left operand should contain '42'", binaryNode.getLeft().toString().contains("42"));
        assertEquals("Operator should be 'as'", "as", binaryNode.getOperator().getText());
        // Check that the right operand contains 'Double'
        assertTrue("Right operand should contain 'Double'", binaryNode.getRight().toString().contains("Double"));
    }

    @Test
    public void testComplexExpression() {
        // Test type cast operator in a more complex scenario
        // This simulates parsing: result as Any
        ParsedNode left = TestUtils.createIdentifierNode("result");
        LocatableToken asToken = TestUtils.createToken(JavaTokenTypes.LITERAL_as, "as", 1, 7);
        LocatableToken typeToken = TestUtils.createToken(JavaTokenTypes.IDENT, "Any", 1, 10);

        testParser.setNextToken(typeToken);

        ParseResult<ParsedNode> result = typeCastParselet.parse(testParser, left, asToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Type cast operator should produce a node", node);

        // Verify the node is a TestBinaryNode with correct components
        assertTrue("Should create a TestBinaryNode", node instanceof TestNodeFactory.TestBinaryNode);
        TestNodeFactory.TestBinaryNode binaryNode = (TestNodeFactory.TestBinaryNode) node;

        // Debug output to see actual values
        System.out.println("DEBUG testComplexExpression - Left: " + binaryNode.getLeft().toString());
        System.out.println("DEBUG testComplexExpression - Left class: " + binaryNode.getLeft().getClass().getSimpleName());
        System.out.println("DEBUG testComplexExpression - Right: " + binaryNode.getRight().toString());
        System.out.println("DEBUG testComplexExpression - Right class: " + binaryNode.getRight().getClass().getSimpleName());

        // Check that the left operand contains 'result'
        assertTrue("Left operand should contain 'result': " + binaryNode.getLeft().toString(),
                  binaryNode.getLeft().toString().contains("result"));
        assertEquals("Operator should be 'as'", "as", binaryNode.getOperator().getText());
        // Check that the right operand contains 'Any'
        assertTrue("Right operand should contain 'Any': " + binaryNode.getRight().toString(),
                  binaryNode.getRight().toString().contains("Any"));
    }

    @Test
    public void testHighPrecedenceBehavior() {
        // Verify that TYPE_CAST has appropriate precedence relative to other operators
        assertTrue("Type cast should have higher precedence than addition",
                  Precedence.TYPE_CAST.getValue() > Precedence.ADDITIVE.getValue());
        assertTrue("Type cast should have higher precedence than multiplication",
                  Precedence.TYPE_CAST.getValue() > Precedence.MULTIPLICATIVE.getValue());
        assertTrue("Type cast should have lower precedence than postfix operators",
                  Precedence.TYPE_CAST.getValue() < Precedence.POSTFIX.getValue());
    }
}
