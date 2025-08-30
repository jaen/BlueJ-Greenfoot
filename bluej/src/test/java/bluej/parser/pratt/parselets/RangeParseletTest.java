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
 * Test suite for RangeParselet.
 *
 * <p>Tests the parsing of Kotlin range expressions (..) including:</p>
 * <ul>
 *   <li>Basic integer ranges</li>
 *   <li>Character ranges</li>
 *   <li>Complex expressions as bounds</li>
 *   <li>Precedence validation</li>
 *   <li>Associativity</li>
 *   <li>Error handling</li>
 * </ul>
 *
 * @author BlueJ Team
 */
public class RangeParseletTest {

    private RangeParselet rangeParselet;
    private MockParser testParser;

    @Before
    public void setUp() {
        rangeParselet = new RangeParselet();
        testParser = new MockParser();

        // Register necessary parselets for testing
        testParser.registerParselet(JavaTokenTypes.IDENT, new NameParselet());
        testParser.registerParselet(JavaTokenTypes.RANGE, rangeParselet);
        testParser.registerParselet(JavaTokenTypes.NUM_INT, new LiteralParselet());
        testParser.registerParselet(JavaTokenTypes.CHAR_LITERAL, new LiteralParselet());
        testParser.registerParselet(JavaTokenTypes.PLUS, new BinaryOperatorParselet(Precedence.ADDITIVE));
        testParser.registerParselet(JavaTokenTypes.MINUS, new BinaryOperatorParselet(Precedence.ADDITIVE));
    }

    @Test
    public void testPrecedence() {
        // Range operator should have RANGE precedence (80)
        assertEquals("Range operator should have RANGE precedence",
                     Precedence.RANGE.getValue(), rangeParselet.getPrecedence());
        assertTrue("Range should have higher precedence than named checks",
                   rangeParselet.getPrecedence() > Precedence.NAMED_CHECKS.getValue());
        assertTrue("Range should have lower precedence than infix functions",
                   rangeParselet.getPrecedence() < Precedence.INFIX_FUNCTION.getValue());
    }

    @Test
    public void testLeftAssociativity() {
        // Range operators are left-associative
        assertFalse("Range operator should be left-associative", rangeParselet.isRightAssociative());
    }

    @Test
    public void testBasicIntegerRange() {
        // Test: 1..10
        ParsedNode left = TestUtils.createIdentifierNode("1");
        LocatableToken rangeToken = TestUtils.createToken(JavaTokenTypes.RANGE, "..", 1, 2);
        LocatableToken rightToken = TestUtils.createToken(JavaTokenTypes.NUM_INT, "10", 1, 5);

        testParser.setNextToken(rightToken);

        ParseResult<ParsedNode> result = rangeParselet.parse(testParser, left, rangeToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Range operator should produce a node", node);
    }

    @Test
    public void testCharacterRange() {
        // Test: 'a'..'z'
        ParsedNode left = TestUtils.createIdentifierNode("'a'");
        LocatableToken rangeToken = TestUtils.createToken(JavaTokenTypes.RANGE, "..", 1, 4);
        LocatableToken rightToken = TestUtils.createToken(JavaTokenTypes.CHAR_LITERAL, "'z'", 1, 7);

        testParser.setNextToken(rightToken);

        ParseResult<ParsedNode> result = rangeParselet.parse(testParser, left, rangeToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Character range should produce a node", node);
    }

    @Test
    public void testVariableRange() {
        // Test: start..end
        ParsedNode left = TestUtils.createIdentifierNode("start");
        LocatableToken rangeToken = TestUtils.createToken(JavaTokenTypes.RANGE, "..", 1, 6);
        LocatableToken rightToken = TestUtils.createToken(JavaTokenTypes.IDENT, "end", 1, 9);

        testParser.setNextToken(rightToken);

        ParseResult<ParsedNode> result = rangeParselet.parse(testParser, left, rangeToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Variable range should produce a node", node);
    }

    @Test
    public void testNegativeNumberRange() {
        // Test: -5..5
        ParsedNode left = TestUtils.createIdentifierNode("-5");
        LocatableToken rangeToken = TestUtils.createToken(JavaTokenTypes.RANGE, "..", 1, 3);
        LocatableToken rightToken = TestUtils.createToken(JavaTokenTypes.NUM_INT, "5", 1, 6);

        testParser.setNextToken(rightToken);

        ParseResult<ParsedNode> result = rangeParselet.parse(testParser, left, rangeToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Negative number range should produce a node", node);
    }

    @Test
    public void testWrongOperatorType() {
        // Test with wrong operator type
        ParsedNode left = TestUtils.createIdentifierNode("1");
        LocatableToken wrongToken = TestUtils.createToken(JavaTokenTypes.PLUS, "+", 1, 2);

        ParseResult<ParsedNode> result = rangeParselet.parse(testParser, left, wrongToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should fail with wrong operator", result.isFailure());
    }

    @Test
    public void testMissingRightOperand() {
        // Test when right operand parsing fails
        ParsedNode left = TestUtils.createIdentifierNode("1");
        LocatableToken rangeToken = TestUtils.createToken(JavaTokenTypes.RANGE, "..", 1, 2);

        // Set up parser to fail on next expression
        testParser.setNextExpressionFailure("Missing expression");

        ParseResult<ParsedNode> result = rangeParselet.parse(testParser, left, rangeToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should fail when right operand is missing", result.isFailure());
    }

    @Test
    public void testIsRangeOperator() {
        // Test the utility method
        assertTrue("Should recognize RANGE token",
                   rangeParselet.isRangeOperator(JavaTokenTypes.RANGE));
        assertFalse("Should not recognize PLUS token",
                    rangeParselet.isRangeOperator(JavaTokenTypes.PLUS));
        assertFalse("Should not recognize DOT token",
                    rangeParselet.isRangeOperator(JavaTokenTypes.DOT));
    }

    @Test
    public void testToString() {
        String str = rangeParselet.toString();
        assertTrue("toString should contain class name", str.contains("RangeParselet"));
        assertTrue("toString should contain precedence", str.contains("precedence=80"));
        assertTrue("toString should contain associativity", str.contains("leftAssociative=true"));
    }

    @Test
    public void testChainedRanges() {
        // Test chained range expressions: first..second..third (left-associative)
        // This creates ((first..second)..third)
        ParsedNode firstRange = TestUtils.createIdentifierNode("innerRange"); // Represents first..second
        LocatableToken rangeToken = TestUtils.createToken(JavaTokenTypes.RANGE, "..", 1, 15);
        LocatableToken thirdToken = TestUtils.createToken(JavaTokenTypes.IDENT, "third", 1, 18);

        testParser.setNextToken(thirdToken);

        ParseResult<ParsedNode> result = rangeParselet.parse(testParser, firstRange, rangeToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Chained range should produce a node", node);
    }

    @Test
    public void testComplexExpressionBounds() {
        // Test with identifiers representing complex expressions
        ParsedNode left = TestUtils.createIdentifierNode("complexLeft");
        LocatableToken rangeToken = TestUtils.createToken(JavaTokenTypes.RANGE, "..", 1, 12);
        LocatableToken rightToken = TestUtils.createToken(JavaTokenTypes.IDENT, "complexRight", 1, 15);

        testParser.setNextToken(rightToken);

        ParseResult<ParsedNode> result = rangeParselet.parse(testParser, left, rangeToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Complex expression range should produce a node", node);
    }
}
