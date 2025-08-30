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
 * Test class for TypeCheckParselet.
 *
 * <p>This test verifies that the type check operator (is) is correctly parsed
 * with proper precedence, associativity, and error handling.</p>
 *
 * @author BlueJ Team
 */
public class TypeCheckParseletTest {

    private TypeCheckParselet typeCheckParselet;
    private MockParser testParser;

    @Before
    public void setUp() {
        typeCheckParselet = new TypeCheckParselet();
        testParser = new MockParser();

        // Register necessary parselets for testing
        testParser.registerParselet(JavaTokenTypes.IDENT, new NameParselet());
        testParser.registerParselet(JavaTokenTypes.LITERAL_is, typeCheckParselet);
        testParser.registerParselet(JavaTokenTypes.NUM_INT, new LiteralParselet());
    }

    @Test
    public void testPrecedence() {
        // Type check operator should have NAMED_CHECKS precedence (70)
        assertEquals("Type check operator should have NAMED_CHECKS precedence",
                    Precedence.NAMED_CHECKS.getValue(), typeCheckParselet.getPrecedence());
    }

    @Test
    public void testLeftAssociativity() {
        // Type check operator should be left-associative
        assertFalse("Type check operator should be left-associative",
                   typeCheckParselet.isRightAssociative());
    }

    @Test
    public void testBasicTypeCheck() {
        // Test: obj is String
        ParsedNode left = TestUtils.createIdentifierNode("obj");
        LocatableToken isToken = TestUtils.createToken(JavaTokenTypes.LITERAL_is, "is", 1, 4);
        LocatableToken rightToken = TestUtils.createToken(JavaTokenTypes.IDENT, "String", 1, 7);

        testParser.setNextToken(rightToken);

        ParseResult<ParsedNode> result = typeCheckParselet.parse(testParser, left, isToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Type check operator should produce a node", node);

        // Verify the node is a TestBinaryNode with correct components
        assertTrue("Should create a TestBinaryNode", node instanceof TestNodeFactory.TestBinaryNode);
        TestNodeFactory.TestBinaryNode binaryNode = (TestNodeFactory.TestBinaryNode) node;

        // Debug output to see actual values
        System.out.println("DEBUG testBasicTypeCheck - Left: " + binaryNode.getLeft().toString());
        System.out.println("DEBUG testBasicTypeCheck - Left class: " + binaryNode.getLeft().getClass().getSimpleName());
        System.out.println("DEBUG testBasicTypeCheck - Right: " + binaryNode.getRight().toString());
        System.out.println("DEBUG testBasicTypeCheck - Right class: " + binaryNode.getRight().getClass().getSimpleName());

        // Check that the left operand contains 'obj'
        assertTrue("Left operand should contain 'obj'", binaryNode.getLeft().toString().contains("obj"));
        assertEquals("Operator should be 'is'", "is", binaryNode.getOperator().getText());
        // Check that the right operand contains 'String'
        assertTrue("Right operand should contain 'String'", binaryNode.getRight().toString().contains("String"));
    }

    @Test
    public void testTypeCheckWithComplexType() {
        // Test: value is List
        ParsedNode left = TestUtils.createIdentifierNode("value");
        LocatableToken isToken = TestUtils.createToken(JavaTokenTypes.LITERAL_is, "is", 1, 6);
        LocatableToken rightToken = TestUtils.createToken(JavaTokenTypes.IDENT, "List", 1, 9);

        testParser.setNextToken(rightToken);

        ParseResult<ParsedNode> result = typeCheckParselet.parse(testParser, left, isToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Type check operator should produce a node", node);

        // Verify the node is a TestBinaryNode with correct components
        assertTrue("Should create a TestBinaryNode", node instanceof TestNodeFactory.TestBinaryNode);
        TestNodeFactory.TestBinaryNode binaryNode = (TestNodeFactory.TestBinaryNode) node;

        // Debug output to see actual values
        System.out.println("DEBUG testTypeCheckWithComplexType - Left: " + binaryNode.getLeft().toString());
        System.out.println("DEBUG testTypeCheckWithComplexType - Left class: " + binaryNode.getLeft().getClass().getSimpleName());
        System.out.println("DEBUG testTypeCheckWithComplexType - Right: " + binaryNode.getRight().toString());
        System.out.println("DEBUG testTypeCheckWithComplexType - Right class: " + binaryNode.getRight().getClass().getSimpleName());

        // Check that the left operand contains 'value'
        assertTrue("Left operand should contain 'value'", binaryNode.getLeft().toString().contains("value"));
        assertEquals("Operator should be 'is'", "is", binaryNode.getOperator().getText());
        // Check that the right operand contains 'List'
        assertTrue("Right operand should contain 'List'", binaryNode.getRight().toString().contains("List"));
    }

    @Test
    public void testWrongTokenType() {
        // Test error when non-is token is passed
        ParsedNode left = TestUtils.createIdentifierNode("obj");
        LocatableToken wrongToken = TestUtils.createToken(JavaTokenTypes.LITERAL_as, "as", 1, 4);

        ParseResult<ParsedNode> result = typeCheckParselet.parse(testParser, left, wrongToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be failure", result.isFailure());
        assertTrue("Error message should mention expected 'is' operator",
                  result.getFormattedErrors().contains("Expected 'is' operator"));
    }

    @Test
    public void testMissingRightOperand() {
        // Test error when right operand is missing
        ParsedNode left = TestUtils.createIdentifierNode("obj");
        LocatableToken isToken = TestUtils.createToken(JavaTokenTypes.LITERAL_is, "is", 1, 4);

        // Set up parser to return EOF (no right operand available)
        testParser.setNextToken(TestUtils.createToken(JavaTokenTypes.EOF, "", 1, 7));

        // Mock the parser to return failure for parseExpressionResult
        testParser.setNextExpressionFailure("Expected type expression");

        ParseResult<ParsedNode> result = typeCheckParselet.parse(testParser, left, isToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be failure", result.isFailure());
        assertTrue("Error message should mention missing type expression",
                  result.getFormattedErrors().contains("Missing type expression"));
    }

    @Test
    public void testLeftAssociativityBehavior() {
        // This test verifies that the precedence calculation for left-associativity is correct
        // For left-associative operators, we use the same precedence for parsing the right operand
        ParsedNode left = TestUtils.createIdentifierNode("obj");
        LocatableToken isToken = TestUtils.createToken(JavaTokenTypes.LITERAL_is, "is", 1, 4);
        LocatableToken typeToken = TestUtils.createToken(JavaTokenTypes.IDENT, "String", 1, 7);

        testParser.setNextToken(typeToken);

        ParseResult<ParsedNode> result = typeCheckParselet.parse(testParser, left, isToken);

        assertTrue("Should be successful", result.isSuccess());

        // Verify that the parser was called with same precedence for left-associative parsing
        int expectedRightPrecedence = Precedence.NAMED_CHECKS.getValue();
        assertEquals("Should use same precedence for left-associative parsing",
                    expectedRightPrecedence, testParser.getLastParseExpressionPrecedence());
    }

    @Test
    public void testIsTypeCheckOperatorUtility() {
        // Test the utility method for recognizing type check operators
        assertTrue("Should recognize 'is' as type check operator",
                  typeCheckParselet.isTypeCheckOperator(JavaTokenTypes.LITERAL_is));
        assertFalse("Should not recognize 'as' as type check operator",
                   typeCheckParselet.isTypeCheckOperator(JavaTokenTypes.LITERAL_as));
        assertFalse("Should not recognize '+' as type check operator",
                   typeCheckParselet.isTypeCheckOperator(JavaTokenTypes.PLUS));
    }

    @Test
    public void testToString() {
        String str = typeCheckParselet.toString();
        assertNotNull("toString should not return null", str);
        assertTrue("toString should contain class name",
                  str.contains("TypeCheckParselet"));
        assertTrue("toString should contain precedence",
                  str.contains("precedence=" + Precedence.NAMED_CHECKS.getValue()));
        assertTrue("toString should indicate left-associativity",
                  str.contains("leftAssociative=true"));
    }

    @Test
    public void testTypeCheckWithLiteral() {
        // Test: 42 is Int (checking literal against type)
        ParsedNode left = TestUtils.createIdentifierNode("42");
        LocatableToken isToken = TestUtils.createToken(JavaTokenTypes.LITERAL_is, "is", 1, 3);
        LocatableToken typeToken = TestUtils.createToken(JavaTokenTypes.IDENT, "Int", 1, 6);

        testParser.setNextToken(typeToken);

        ParseResult<ParsedNode> result = typeCheckParselet.parse(testParser, left, isToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Type check operator should produce a node", node);

        // Verify the node is a TestBinaryNode with correct components
        assertTrue("Should create a TestBinaryNode", node instanceof TestNodeFactory.TestBinaryNode);
        TestNodeFactory.TestBinaryNode binaryNode = (TestNodeFactory.TestBinaryNode) node;

        // Debug output to see actual values
        System.out.println("DEBUG testTypeCheckWithLiteral - Left: " + binaryNode.getLeft().toString());
        System.out.println("DEBUG testTypeCheckWithLiteral - Left class: " + binaryNode.getLeft().getClass().getSimpleName());
        System.out.println("DEBUG testTypeCheckWithLiteral - Right: " + binaryNode.getRight().toString());
        System.out.println("DEBUG testTypeCheckWithLiteral - Right class: " + binaryNode.getRight().getClass().getSimpleName());

        // Check that the left operand contains '42'
        assertTrue("Left operand should contain '42'", binaryNode.getLeft().toString().contains("42"));
        assertEquals("Operator should be 'is'", "is", binaryNode.getOperator().getText());
        // Check that the right operand contains 'Int'
        assertTrue("Right operand should contain 'Int'", binaryNode.getRight().toString().contains("Int"));
    }

    @Test
    public void testComplexExpression() {
        // Test type check operator in a more complex scenario
        // This simulates parsing: result is Boolean
        ParsedNode left = TestUtils.createIdentifierNode("result");
        LocatableToken isToken = TestUtils.createToken(JavaTokenTypes.LITERAL_is, "is", 1, 7);
        LocatableToken typeToken = TestUtils.createToken(JavaTokenTypes.IDENT, "Boolean", 1, 10);

        testParser.setNextToken(typeToken);

        ParseResult<ParsedNode> result = typeCheckParselet.parse(testParser, left, isToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Type check operator should produce a node", node);

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
        assertEquals("Operator should be 'is'", "is", binaryNode.getOperator().getText());
        // Check that the right operand contains 'Boolean'
        assertTrue("Right operand should contain 'Boolean': " + binaryNode.getRight().toString(),
                  binaryNode.getRight().toString().contains("Boolean"));
    }
}
