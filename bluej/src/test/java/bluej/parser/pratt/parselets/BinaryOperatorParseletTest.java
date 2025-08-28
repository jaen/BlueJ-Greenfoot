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
import bluej.parser.pratt.KotlinPrattParser;
import bluej.parser.pratt.ParseResult;
import bluej.parser.pratt.Precedence;
import bluej.parser.pratt.TestNodeFactory;
import bluej.parser.pratt.testutil.MockParser;
import bluej.parser.pratt.testutil.TestUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link BinaryOperatorParselet}.
 *
 * <p>This test class validates the binary operator parsing functionality including:</p>
 * <ul>
 *   <li>All supported binary operators (arithmetic, comparison, logical, bitwise, assignment)</li>
 *   <li>Precedence and associativity handling</li>
 *   <li>Error handling for invalid tokens and missing operands</li>
 *   <li>Parselet registration and dispatch (foundation phase)</li>
 * </ul>
 *
 * <p>Note: During foundation phase, parselets validate structure but don't create AST nodes yet.</p>
 *
 * @author BlueJ Team
 */
public class BinaryOperatorParseletTest {

    private BinaryOperatorParselet additionParselet;
    private BinaryOperatorParselet assignmentParselet;
    private MockParser testParser;

    @Before
    public void setUp() {
        additionParselet = new BinaryOperatorParselet(Precedence.ADDITIVE);
        assignmentParselet = new BinaryOperatorParselet(Precedence.ASSIGNMENT, true); // right-associative
        testParser = new MockParser();
    }



    @Test
    public void testPrecedence() {
        assertEquals("Addition parselet should have additive precedence", Precedence.ADDITIVE.getValue(), additionParselet.getPrecedence());
        assertEquals("Assignment parselet should have assignment precedence", Precedence.ASSIGNMENT.getValue(), assignmentParselet.getPrecedence());
    }

    @Test
    public void testAssociativity() {
        assertFalse("Addition should be left-associative", additionParselet.isRightAssociative());
        assertTrue("Assignment should be right-associative", assignmentParselet.isRightAssociative());
    }

    @Test
    public void testParseBinaryOperators() {
        int[] binaryOperators = {
            // Arithmetic
            JavaTokenTypes.PLUS, JavaTokenTypes.MINUS, JavaTokenTypes.STAR, JavaTokenTypes.DIV, JavaTokenTypes.MOD,
            // Comparison
            JavaTokenTypes.EQUAL, JavaTokenTypes.NOT_EQUAL, JavaTokenTypes.LT, JavaTokenTypes.LE, JavaTokenTypes.GT, JavaTokenTypes.GE,
            // Logical
            JavaTokenTypes.LAND, JavaTokenTypes.LOR,
            // Bitwise
            JavaTokenTypes.BAND, JavaTokenTypes.BOR, JavaTokenTypes.BXOR, JavaTokenTypes.SL, JavaTokenTypes.SR, JavaTokenTypes.BSR,
            // Assignment
            JavaTokenTypes.ASSIGN, JavaTokenTypes.PLUS_ASSIGN, JavaTokenTypes.MINUS_ASSIGN,
            JavaTokenTypes.STAR_ASSIGN, JavaTokenTypes.DIV_ASSIGN, JavaTokenTypes.MOD_ASSIGN
        };

        for (int tokenType : binaryOperators) {
            LocatableToken token = TestUtils.createToken(tokenType, getOperatorSymbol(tokenType), 1, 1);
            // Test that valid binary operators can be parsed (canHandle method removed)
            // Create a dummy left operand for infix parsing
            ParsedNode dummyLeft = TestUtils.createIdentifierNode("dummy");
            ParseResult<ParsedNode> result = additionParselet.parse(testParser, dummyLeft, token);
            // In foundation phase, should validate without creating nodes
            assertNotNull("Should return a result for operator type " + tokenType, result);
            assertNotNull("Should provide description for operator type " + tokenType,
                additionParselet.getHandledOperator(tokenType));
        }
    }

    @Test
    public void testParseNonBinaryOperators() {
        int[] nonBinaryOperators = {
            JavaTokenTypes.IDENT, JavaTokenTypes.NUM_INT, JavaTokenTypes.STRING_LITERAL,
            JavaTokenTypes.LPAREN, JavaTokenTypes.RPAREN, JavaTokenTypes.LITERAL_class,
            JavaTokenTypes.LITERAL_fun, JavaTokenTypes.EOF, JavaTokenTypes.SEMI
        };

        for (int tokenType : nonBinaryOperators) {
            MockParser freshParser = new MockParser();
            LocatableToken token = TestUtils.createToken(tokenType, "test", 1, 1);
            // Test that invalid tokens produce errors when parsed (canHandle method removed)
            ParsedNode dummyLeft = TestUtils.createIdentifierNode("dummy");
            ParseResult<ParsedNode> result = additionParselet.parse(freshParser, dummyLeft, token);
            assertNotNull("Should return a result for invalid operator tokens", result);
            assertTrue("Should be a failure for non-binary operator tokens", result.isFailure());
            assertEquals("Should indicate unsupported operator type",
                "unsupported operator type", additionParselet.getHandledOperator(tokenType));
        }
    }

    @Test
    public void testToString() {
        String additionString = additionParselet.toString();
        String assignmentString = assignmentParselet.toString();

        assertTrue("Addition toString should contain precedence", additionString.contains("ADDITIVE"));
        assertTrue("Addition toString should contain associativity", additionString.contains("rightAssociative=false"));

        assertTrue("Assignment toString should contain precedence", assignmentString.contains("ASSIGNMENT"));
        assertTrue("Assignment toString should contain associativity", assignmentString.contains("rightAssociative=true"));
    }

    @Test
    public void testConstructors() {
        // Test single-parameter constructor (defaults to left-associative)
        BinaryOperatorParselet leftAssoc = new BinaryOperatorParselet(Precedence.MULTIPLICATIVE);
        assertFalse("Single-parameter constructor should create left-associative parselet", leftAssoc.isRightAssociative());
        assertEquals("Should have correct precedence", Precedence.MULTIPLICATIVE.getValue(), leftAssoc.getPrecedence());

        // Test two-parameter constructor
        BinaryOperatorParselet rightAssoc = new BinaryOperatorParselet(Precedence.ASSIGNMENT, true);
        assertTrue("Two-parameter constructor should allow right-associative", rightAssoc.isRightAssociative());
        assertEquals("Should have correct precedence", Precedence.ASSIGNMENT.getValue(), rightAssoc.getPrecedence());
    }



    /**
     * Helper method to get operator symbol for token type.
     */
    private String getOperatorSymbol(int tokenType) {
        return switch (tokenType) {
            case JavaTokenTypes.PLUS -> "+";
            case JavaTokenTypes.MINUS -> "-";
            case JavaTokenTypes.STAR -> "*";
            case JavaTokenTypes.DIV -> "/";
            case JavaTokenTypes.MOD -> "%";
            case JavaTokenTypes.EQUAL -> "==";
            case JavaTokenTypes.NOT_EQUAL -> "!=";
            case JavaTokenTypes.LT -> "<";
            case JavaTokenTypes.LE -> "<=";
            case JavaTokenTypes.GT -> ">";
            case JavaTokenTypes.GE -> ">=";
            case JavaTokenTypes.LAND -> "&&";
            case JavaTokenTypes.LOR -> "||";
            case JavaTokenTypes.BAND -> "&";
            case JavaTokenTypes.BOR -> "|";
            case JavaTokenTypes.BXOR -> "^";
            case JavaTokenTypes.SL -> "<<";
            case JavaTokenTypes.SR -> ">>";
            case JavaTokenTypes.BSR -> ">>>";
            case JavaTokenTypes.ASSIGN -> "=";
            case JavaTokenTypes.PLUS_ASSIGN -> "+=";
            case JavaTokenTypes.MINUS_ASSIGN -> "-=";
            case JavaTokenTypes.STAR_ASSIGN -> "*=";
            case JavaTokenTypes.DIV_ASSIGN -> "/=";
            case JavaTokenTypes.MOD_ASSIGN -> "%=";
            default -> "op";
        };
    }


}
