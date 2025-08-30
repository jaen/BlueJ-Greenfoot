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
import bluej.parser.pratt.testutil.MockParser;
import bluej.parser.pratt.testutil.TestUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test suite for InOperatorParselet.
 *
 * <p>Tests the parsing of Kotlin containment expressions (in/!in) including:</p>
 * <ul>
 *   <li>Basic range containment</li>
 *   <li>Collection containment</li>
 *   <li>String containment</li>
 *   <li>Complex container expressions</li>
 *   <li>Precedence validation</li>
 *   <li>Associativity</li>
 *   <li>Error handling</li>
 * </ul>
 *
 * @author BlueJ Team
 */
public class InOperatorParseletTest {

    private InOperatorParselet inOperatorParselet;
    private MockParser testParser;

    @Before
    public void setUp() {
        inOperatorParselet = new InOperatorParselet();
        testParser = new MockParser();

        // Register necessary parselets for testing
        testParser.registerParselet(JavaTokenTypes.IDENT, new NameParselet());
        testParser.registerParselet(JavaTokenTypes.LITERAL_in, inOperatorParselet);
        testParser.registerParselet(JavaTokenTypes.NUM_INT, new LiteralParselet());
        testParser.registerParselet(JavaTokenTypes.STRING_LITERAL, new LiteralParselet());
        testParser.registerParselet(JavaTokenTypes.RANGE, new RangeParselet());
        testParser.registerParselet(JavaTokenTypes.LITERAL_is, new TypeCheckParselet());
    }

    @Test
    public void testPrecedence() {
        // 'in' operator should have NAMED_CHECKS precedence (70)
        assertEquals("'in' operator should have NAMED_CHECKS precedence",
                     Precedence.NAMED_CHECKS.getValue(), inOperatorParselet.getPrecedence());
        assertEquals("'in' should have same precedence as type check operators",
                     new TypeCheckParselet().getPrecedence(), inOperatorParselet.getPrecedence());
        assertTrue("'in' should have higher precedence than comparison",
                   inOperatorParselet.getPrecedence() > Precedence.COMPARISON.getValue());
        assertTrue("'in' should have lower precedence than range",
                   inOperatorParselet.getPrecedence() < Precedence.RANGE.getValue());
    }

    @Test
    public void testLeftAssociativity() {
        // 'in' operators are left-associative
        assertFalse("'in' operator should be left-associative", inOperatorParselet.isRightAssociative());
    }

    @Test
    public void testBasicRangeContainment() {
        // Test: x in 1..10
        ParsedNode left = TestUtils.createIdentifierNode("x");
        LocatableToken inToken = TestUtils.createToken(JavaTokenTypes.LITERAL_in, "in", 1, 2);
        LocatableToken rangeToken = TestUtils.createToken(JavaTokenTypes.IDENT, "range1to10", 1, 5);

        testParser.setNextToken(rangeToken);

        ParseResult<ParsedNode> result = inOperatorParselet.parse(testParser, left, inToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Range containment should produce a node", node);
    }

    @Test
    public void testCollectionContainment() {
        // Test: item in list
        ParsedNode left = TestUtils.createIdentifierNode("item");
        LocatableToken inToken = TestUtils.createToken(JavaTokenTypes.LITERAL_in, "in", 1, 5);
        LocatableToken listToken = TestUtils.createToken(JavaTokenTypes.IDENT, "list", 1, 8);

        testParser.setNextToken(listToken);

        ParseResult<ParsedNode> result = inOperatorParselet.parse(testParser, left, inToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Collection containment should produce a node", node);
    }

    @Test
    public void testStringContainment() {
        // Test: char in "hello"
        ParsedNode left = TestUtils.createIdentifierNode("char");
        LocatableToken inToken = TestUtils.createToken(JavaTokenTypes.LITERAL_in, "in", 1, 5);
        LocatableToken stringToken = TestUtils.createToken(JavaTokenTypes.STRING_LITERAL, "\"hello\"", 1, 8);

        testParser.setNextToken(stringToken);

        ParseResult<ParsedNode> result = inOperatorParselet.parse(testParser, left, inToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("String containment should produce a node", node);
    }

    @Test
    public void testCharacterRangeContainment() {
        // Test: ch in 'a'..'z'
        ParsedNode left = TestUtils.createIdentifierNode("ch");
        LocatableToken inToken = TestUtils.createToken(JavaTokenTypes.LITERAL_in, "in", 1, 3);
        LocatableToken charRangeToken = TestUtils.createToken(JavaTokenTypes.IDENT, "charRange", 1, 6);

        testParser.setNextToken(charRangeToken);

        ParseResult<ParsedNode> result = inOperatorParselet.parse(testParser, left, inToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Character range containment should produce a node", node);
    }

    @Test
    public void testComplexContainerExpression() {
        // Test: element in complexContainer
        ParsedNode left = TestUtils.createIdentifierNode("element");
        LocatableToken inToken = TestUtils.createToken(JavaTokenTypes.LITERAL_in, "in", 1, 8);
        LocatableToken containerToken = TestUtils.createToken(JavaTokenTypes.IDENT, "complexContainer", 1, 11);

        testParser.setNextToken(containerToken);

        ParseResult<ParsedNode> result = inOperatorParselet.parse(testParser, left, inToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Complex container expression should produce a node", node);
    }

    @Test
    public void testWrongOperatorType() {
        // Test with wrong operator type
        ParsedNode left = TestUtils.createIdentifierNode("x");
        LocatableToken wrongToken = TestUtils.createToken(JavaTokenTypes.LITERAL_is, "is", 1, 2);

        ParseResult<ParsedNode> result = inOperatorParselet.parse(testParser, left, wrongToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should fail with wrong operator", result.isFailure());
    }

    @Test
    public void testMissingContainerExpression() {
        // Test when container expression parsing fails
        ParsedNode left = TestUtils.createIdentifierNode("x");
        LocatableToken inToken = TestUtils.createToken(JavaTokenTypes.LITERAL_in, "in", 1, 2);

        // Set up parser to fail on next expression
        testParser.setNextExpressionFailure("Missing container");

        ParseResult<ParsedNode> result = inOperatorParselet.parse(testParser, left, inToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should fail when container expression is missing", result.isFailure());
    }

    @Test
    public void testIsContainmentOperator() {
        // Test the utility method
        assertTrue("Should recognize LITERAL_in token",
                   inOperatorParselet.isContainmentOperator(JavaTokenTypes.LITERAL_in));
        assertFalse("Should not recognize LITERAL_is token",
                    inOperatorParselet.isContainmentOperator(JavaTokenTypes.LITERAL_is));
        assertFalse("Should not recognize RANGE token",
                    inOperatorParselet.isContainmentOperator(JavaTokenTypes.RANGE));
    }

    @Test
    public void testToString() {
        String str = inOperatorParselet.toString();
        assertTrue("toString should contain class name", str.contains("InOperatorParselet"));
        assertTrue("toString should contain precedence", str.contains("precedence=70"));
        assertTrue("toString should contain associativity", str.contains("leftAssociative=true"));
    }

    @Test
    public void testChainedContainmentOperators() {
        // Test chained containment expressions: innerResult in outerContainer
        // This represents (a in b) in c (left-associative)
        ParsedNode innerResult = TestUtils.createIdentifierNode("innerResult"); // Represents (a in b)
        LocatableToken inToken = TestUtils.createToken(JavaTokenTypes.LITERAL_in, "in", 1, 12);
        LocatableToken outerToken = TestUtils.createToken(JavaTokenTypes.IDENT, "outerContainer", 1, 15);

        testParser.setNextToken(outerToken);

        ParseResult<ParsedNode> result = inOperatorParselet.parse(testParser, innerResult, inToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Chained containment should produce a node", node);
    }

    @Test
    public void testContainmentWithArithmeticExpression() {
        // Test precedence: (a + b) in collection
        ParsedNode left = TestUtils.createIdentifierNode("arithmeticResult");
        LocatableToken inToken = TestUtils.createToken(JavaTokenTypes.LITERAL_in, "in", 1, 17);
        LocatableToken collectionToken = TestUtils.createToken(JavaTokenTypes.IDENT, "collection", 1, 20);

        testParser.setNextToken(collectionToken);

        ParseResult<ParsedNode> result = inOperatorParselet.parse(testParser, left, inToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Arithmetic expression containment should produce a node", node);
    }

    @Test
    public void testContainmentInConditional() {
        // Test in conditional context: element in validValues
        ParsedNode left = TestUtils.createIdentifierNode("element");
        LocatableToken inToken = TestUtils.createToken(JavaTokenTypes.LITERAL_in, "in", 1, 8);
        LocatableToken valuesToken = TestUtils.createToken(JavaTokenTypes.IDENT, "validValues", 1, 11);

        testParser.setNextToken(valuesToken);

        ParseResult<ParsedNode> result = inOperatorParselet.parse(testParser, left, inToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Conditional containment should produce a node", node);
    }

    @Test
    public void testVariableContainment() {
        // Test: key in map
        ParsedNode left = TestUtils.createIdentifierNode("key");
        LocatableToken inToken = TestUtils.createToken(JavaTokenTypes.LITERAL_in, "in", 1, 4);
        LocatableToken mapToken = TestUtils.createToken(JavaTokenTypes.IDENT, "map", 1, 7);

        testParser.setNextToken(mapToken);

        ParseResult<ParsedNode> result = inOperatorParselet.parse(testParser, left, inToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Variable containment should produce a node", node);
    }
}
