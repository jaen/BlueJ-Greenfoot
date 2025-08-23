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
package bluej.parser.pratt;

import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import junit.framework.TestCase;
import org.junit.Test;

/**
 * Comprehensive test suite for {@link TokenUtils}.
 *
 * <p>This test suite validates all utility methods provided by TokenUtils,
 * including token type categorization, error message formatting, and
 * bracket matching functionality.</p>
 *
 * <p>Tests are organized by functionality:</p>
 * <ul>
 *   <li>Token type name descriptions</li>
 *   <li>Token categorization methods</li>
 *   <li>Bracket matching utilities</li>
 *   <li>Error message formatting</li>
 *   <li>Validation methods</li>
 * </ul>
 *
 * @author BlueJ Team
 */
public class TokenUtilsTest extends TestCase {

    @Test
    public void testGetTokenTypeNameLiterals() {
        assertEquals("integer literal", TokenUtils.getTokenTypeName(JavaTokenTypes.NUM_INT));
        assertEquals("long literal", TokenUtils.getTokenTypeName(JavaTokenTypes.NUM_LONG));
        assertEquals("float literal", TokenUtils.getTokenTypeName(JavaTokenTypes.NUM_FLOAT));
        assertEquals("double literal", TokenUtils.getTokenTypeName(JavaTokenTypes.NUM_DOUBLE));
        assertEquals("string literal", TokenUtils.getTokenTypeName(JavaTokenTypes.STRING_LITERAL));
        assertEquals("multiline string literal", TokenUtils.getTokenTypeName(JavaTokenTypes.STRING_LITERAL_MULTILINE));
        assertEquals("character literal", TokenUtils.getTokenTypeName(JavaTokenTypes.CHAR_LITERAL));
        assertEquals("boolean literal 'true'", TokenUtils.getTokenTypeName(JavaTokenTypes.LITERAL_true));
        assertEquals("boolean literal 'false'", TokenUtils.getTokenTypeName(JavaTokenTypes.LITERAL_false));
        assertEquals("null literal", TokenUtils.getTokenTypeName(JavaTokenTypes.LITERAL_null));
    }

    @Test
    public void testGetTokenTypeNameKeywords() {
        assertEquals("identifier", TokenUtils.getTokenTypeName(JavaTokenTypes.IDENT));
        assertEquals("keyword 'class'", TokenUtils.getTokenTypeName(JavaTokenTypes.LITERAL_class));
        assertEquals("keyword 'fun'", TokenUtils.getTokenTypeName(JavaTokenTypes.LITERAL_fun));
        assertEquals("keyword 'if'", TokenUtils.getTokenTypeName(JavaTokenTypes.LITERAL_if));
        assertEquals("keyword 'else'", TokenUtils.getTokenTypeName(JavaTokenTypes.LITERAL_else));
        assertEquals("keyword 'return'", TokenUtils.getTokenTypeName(JavaTokenTypes.LITERAL_return));
    }

    @Test
    public void testGetTokenTypeNameArithmeticOperators() {
        assertEquals("plus operator '+'", TokenUtils.getTokenTypeName(JavaTokenTypes.PLUS));
        assertEquals("minus operator '-'", TokenUtils.getTokenTypeName(JavaTokenTypes.MINUS));
        assertEquals("multiplication operator '*'", TokenUtils.getTokenTypeName(JavaTokenTypes.STAR));
        assertEquals("division operator '/'", TokenUtils.getTokenTypeName(JavaTokenTypes.DIV));
        assertEquals("modulo operator '%'", TokenUtils.getTokenTypeName(JavaTokenTypes.MOD));
    }

    @Test
    public void testGetTokenTypeNameComparisonOperators() {
        assertEquals("equality operator '=='", TokenUtils.getTokenTypeName(JavaTokenTypes.EQUAL));
        assertEquals("inequality operator '!='", TokenUtils.getTokenTypeName(JavaTokenTypes.NOT_EQUAL));
        assertEquals("less-than operator '<'", TokenUtils.getTokenTypeName(JavaTokenTypes.LT));
        assertEquals("less-than-or-equal operator '<='", TokenUtils.getTokenTypeName(JavaTokenTypes.LE));
        assertEquals("greater-than operator '>'", TokenUtils.getTokenTypeName(JavaTokenTypes.GT));
        assertEquals("greater-than-or-equal operator '>='", TokenUtils.getTokenTypeName(JavaTokenTypes.GE));
    }

    @Test
    public void testGetTokenTypeNameBrackets() {
        assertEquals("left parenthesis '('", TokenUtils.getTokenTypeName(JavaTokenTypes.LPAREN));
        assertEquals("right parenthesis ')'", TokenUtils.getTokenTypeName(JavaTokenTypes.RPAREN));
        assertEquals("left bracket '['", TokenUtils.getTokenTypeName(JavaTokenTypes.LBRACK));
        assertEquals("right bracket ']'", TokenUtils.getTokenTypeName(JavaTokenTypes.RBRACK));
        assertEquals("left brace '{'", TokenUtils.getTokenTypeName(JavaTokenTypes.LCURLY));
        assertEquals("right brace '}'", TokenUtils.getTokenTypeName(JavaTokenTypes.RCURLY));
    }

    @Test
    public void testGetTokenTypeNameSpecialTokens() {
        assertEquals("end of file", TokenUtils.getTokenTypeName(JavaTokenTypes.EOF));
        assertEquals("semicolon ';'", TokenUtils.getTokenTypeName(JavaTokenTypes.SEMI));
        assertEquals("comma ','", TokenUtils.getTokenTypeName(JavaTokenTypes.COMMA));
        assertEquals("dot '.'", TokenUtils.getTokenTypeName(JavaTokenTypes.DOT));
    }

    @Test
    public void testGetTokenTypeNameUnknownToken() {
        int unknownTokenType = 99999;
        String result = TokenUtils.getTokenTypeName(unknownTokenType);
        assertEquals("token type 99999", result);
    }

    @Test
    public void testIsLiteralToken() {
        // Test literal tokens
        assertTrue("Integer should be literal", TokenUtils.isLiteralToken(JavaTokenTypes.NUM_INT));
        assertTrue("Long should be literal", TokenUtils.isLiteralToken(JavaTokenTypes.NUM_LONG));
        assertTrue("Float should be literal", TokenUtils.isLiteralToken(JavaTokenTypes.NUM_FLOAT));
        assertTrue("Double should be literal", TokenUtils.isLiteralToken(JavaTokenTypes.NUM_DOUBLE));
        assertTrue("String should be literal", TokenUtils.isLiteralToken(JavaTokenTypes.STRING_LITERAL));
        assertTrue("Multiline string should be literal", TokenUtils.isLiteralToken(JavaTokenTypes.STRING_LITERAL_MULTILINE));
        assertTrue("Character should be literal", TokenUtils.isLiteralToken(JavaTokenTypes.CHAR_LITERAL));
        assertTrue("True should be literal", TokenUtils.isLiteralToken(JavaTokenTypes.LITERAL_true));
        assertTrue("False should be literal", TokenUtils.isLiteralToken(JavaTokenTypes.LITERAL_false));
        assertTrue("Null should be literal", TokenUtils.isLiteralToken(JavaTokenTypes.LITERAL_null));

        // Test non-literal tokens
        assertFalse("Identifier should not be literal", TokenUtils.isLiteralToken(JavaTokenTypes.IDENT));
        assertFalse("Plus should not be literal", TokenUtils.isLiteralToken(JavaTokenTypes.PLUS));
        assertFalse("Left paren should not be literal", TokenUtils.isLiteralToken(JavaTokenTypes.LPAREN));
        assertFalse("Class keyword should not be literal", TokenUtils.isLiteralToken(JavaTokenTypes.LITERAL_class));
    }

    @Test
    public void testIsArithmeticOperator() {
        // Test arithmetic operators
        assertTrue("Plus should be arithmetic", TokenUtils.isArithmeticOperator(JavaTokenTypes.PLUS));
        assertTrue("Minus should be arithmetic", TokenUtils.isArithmeticOperator(JavaTokenTypes.MINUS));
        assertTrue("Star should be arithmetic", TokenUtils.isArithmeticOperator(JavaTokenTypes.STAR));
        assertTrue("Division should be arithmetic", TokenUtils.isArithmeticOperator(JavaTokenTypes.DIV));
        assertTrue("Modulo should be arithmetic", TokenUtils.isArithmeticOperator(JavaTokenTypes.MOD));

        // Test non-arithmetic operators
        assertFalse("Equality should not be arithmetic", TokenUtils.isArithmeticOperator(JavaTokenTypes.EQUAL));
        assertFalse("Logical AND should not be arithmetic", TokenUtils.isArithmeticOperator(JavaTokenTypes.LAND));
        assertFalse("Assignment should not be arithmetic", TokenUtils.isArithmeticOperator(JavaTokenTypes.ASSIGN));
    }

    @Test
    public void testIsComparisonOperator() {
        // Test comparison operators
        assertTrue("Equality should be comparison", TokenUtils.isComparisonOperator(JavaTokenTypes.EQUAL));
        assertTrue("Inequality should be comparison", TokenUtils.isComparisonOperator(JavaTokenTypes.NOT_EQUAL));
        assertTrue("Less than should be comparison", TokenUtils.isComparisonOperator(JavaTokenTypes.LT));
        assertTrue("Less than or equal should be comparison", TokenUtils.isComparisonOperator(JavaTokenTypes.LE));
        assertTrue("Greater than should be comparison", TokenUtils.isComparisonOperator(JavaTokenTypes.GT));
        assertTrue("Greater than or equal should be comparison", TokenUtils.isComparisonOperator(JavaTokenTypes.GE));

        // Test non-comparison operators
        assertFalse("Plus should not be comparison", TokenUtils.isComparisonOperator(JavaTokenTypes.PLUS));
        assertFalse("Logical AND should not be comparison", TokenUtils.isComparisonOperator(JavaTokenTypes.LAND));
    }

    @Test
    public void testIsLogicalOperator() {
        // Test logical operators
        assertTrue("Logical AND should be logical", TokenUtils.isLogicalOperator(JavaTokenTypes.LAND));
        assertTrue("Logical OR should be logical", TokenUtils.isLogicalOperator(JavaTokenTypes.LOR));
        assertTrue("Logical NOT should be logical", TokenUtils.isLogicalOperator(JavaTokenTypes.LNOT));

        // Test non-logical operators
        assertFalse("Bitwise AND should not be logical", TokenUtils.isLogicalOperator(JavaTokenTypes.BAND));
        assertFalse("Plus should not be logical", TokenUtils.isLogicalOperator(JavaTokenTypes.PLUS));
        assertFalse("Equality should not be logical", TokenUtils.isLogicalOperator(JavaTokenTypes.EQUAL));
    }

    @Test
    public void testIsBitwiseOperator() {
        // Test bitwise operators
        assertTrue("Bitwise AND should be bitwise", TokenUtils.isBitwiseOperator(JavaTokenTypes.BAND));
        assertTrue("Bitwise OR should be bitwise", TokenUtils.isBitwiseOperator(JavaTokenTypes.BOR));
        assertTrue("Bitwise XOR should be bitwise", TokenUtils.isBitwiseOperator(JavaTokenTypes.BXOR));
        assertTrue("Bitwise NOT should be bitwise", TokenUtils.isBitwiseOperator(JavaTokenTypes.BNOT));
        assertTrue("Left shift should be bitwise", TokenUtils.isBitwiseOperator(JavaTokenTypes.SL));
        assertTrue("Right shift should be bitwise", TokenUtils.isBitwiseOperator(JavaTokenTypes.SR));
        assertTrue("Unsigned right shift should be bitwise", TokenUtils.isBitwiseOperator(JavaTokenTypes.BSR));

        // Test non-bitwise operators
        assertFalse("Logical AND should not be bitwise", TokenUtils.isBitwiseOperator(JavaTokenTypes.LAND));
        assertFalse("Plus should not be bitwise", TokenUtils.isBitwiseOperator(JavaTokenTypes.PLUS));
    }

    @Test
    public void testIsAssignmentOperator() {
        // Test assignment operators
        assertTrue("Assignment should be assignment", TokenUtils.isAssignmentOperator(JavaTokenTypes.ASSIGN));
        assertTrue("Plus assign should be assignment", TokenUtils.isAssignmentOperator(JavaTokenTypes.PLUS_ASSIGN));
        assertTrue("Minus assign should be assignment", TokenUtils.isAssignmentOperator(JavaTokenTypes.MINUS_ASSIGN));
        assertTrue("Multiply assign should be assignment", TokenUtils.isAssignmentOperator(JavaTokenTypes.STAR_ASSIGN));
        assertTrue("Divide assign should be assignment", TokenUtils.isAssignmentOperator(JavaTokenTypes.DIV_ASSIGN));
        assertTrue("Modulo assign should be assignment", TokenUtils.isAssignmentOperator(JavaTokenTypes.MOD_ASSIGN));

        // Test non-assignment operators
        assertFalse("Plus should not be assignment", TokenUtils.isAssignmentOperator(JavaTokenTypes.PLUS));
        assertFalse("Equality should not be assignment", TokenUtils.isAssignmentOperator(JavaTokenTypes.EQUAL));
    }

    @Test
    public void testIsBinaryOperator() {
        // Test various binary operators
        assertTrue("Plus should be binary", TokenUtils.isBinaryOperator(JavaTokenTypes.PLUS));
        assertTrue("Equality should be binary", TokenUtils.isBinaryOperator(JavaTokenTypes.EQUAL));
        assertTrue("Logical AND should be binary", TokenUtils.isBinaryOperator(JavaTokenTypes.LAND));
        assertTrue("Bitwise AND should be binary", TokenUtils.isBinaryOperator(JavaTokenTypes.BAND));
        assertTrue("Assignment should be binary", TokenUtils.isBinaryOperator(JavaTokenTypes.ASSIGN));

        // Test non-binary tokens
        assertFalse("Identifier should not be binary", TokenUtils.isBinaryOperator(JavaTokenTypes.IDENT));
        assertFalse("Left paren should not be binary", TokenUtils.isBinaryOperator(JavaTokenTypes.LPAREN));
        assertFalse("Integer literal should not be binary", TokenUtils.isBinaryOperator(JavaTokenTypes.NUM_INT));
    }

    @Test
    public void testIsOpeningBracket() {
        // Test opening brackets
        assertTrue("Left paren should be opening bracket", TokenUtils.isOpeningBracket(JavaTokenTypes.LPAREN));
        assertTrue("Left bracket should be opening bracket", TokenUtils.isOpeningBracket(JavaTokenTypes.LBRACK));
        assertTrue("Left brace should be opening bracket", TokenUtils.isOpeningBracket(JavaTokenTypes.LCURLY));

        // Test non-opening brackets
        assertFalse("Right paren should not be opening bracket", TokenUtils.isOpeningBracket(JavaTokenTypes.RPAREN));
        assertFalse("Right bracket should not be opening bracket", TokenUtils.isOpeningBracket(JavaTokenTypes.RBRACK));
        assertFalse("Right brace should not be opening bracket", TokenUtils.isOpeningBracket(JavaTokenTypes.RCURLY));
        assertFalse("Plus should not be opening bracket", TokenUtils.isOpeningBracket(JavaTokenTypes.PLUS));
    }

    @Test
    public void testIsClosingBracket() {
        // Test closing brackets
        assertTrue("Right paren should be closing bracket", TokenUtils.isClosingBracket(JavaTokenTypes.RPAREN));
        assertTrue("Right bracket should be closing bracket", TokenUtils.isClosingBracket(JavaTokenTypes.RBRACK));
        assertTrue("Right brace should be closing bracket", TokenUtils.isClosingBracket(JavaTokenTypes.RCURLY));

        // Test non-closing brackets
        assertFalse("Left paren should not be closing bracket", TokenUtils.isClosingBracket(JavaTokenTypes.LPAREN));
        assertFalse("Left bracket should not be closing bracket", TokenUtils.isClosingBracket(JavaTokenTypes.LBRACK));
        assertFalse("Left brace should not be closing bracket", TokenUtils.isClosingBracket(JavaTokenTypes.LCURLY));
        assertFalse("Plus should not be closing bracket", TokenUtils.isClosingBracket(JavaTokenTypes.PLUS));
    }

    @Test
    public void testGetMatchingClosingBracket() {
        assertEquals("Left paren should match right paren",
                     JavaTokenTypes.RPAREN, TokenUtils.getMatchingClosingBracket(JavaTokenTypes.LPAREN));
        assertEquals("Left bracket should match right bracket",
                     JavaTokenTypes.RBRACK, TokenUtils.getMatchingClosingBracket(JavaTokenTypes.LBRACK));
        assertEquals("Left brace should match right brace",
                     JavaTokenTypes.RCURLY, TokenUtils.getMatchingClosingBracket(JavaTokenTypes.LCURLY));

        // Test invalid input
        assertEquals("Invalid bracket should return -1",
                     -1, TokenUtils.getMatchingClosingBracket(JavaTokenTypes.PLUS));
        assertEquals("Right paren should return -1",
                     -1, TokenUtils.getMatchingClosingBracket(JavaTokenTypes.RPAREN));
    }

    @Test
    public void testGetMatchingOpeningBracket() {
        assertEquals("Right paren should match left paren",
                     JavaTokenTypes.LPAREN, TokenUtils.getMatchingOpeningBracket(JavaTokenTypes.RPAREN));
        assertEquals("Right bracket should match left bracket",
                     JavaTokenTypes.LBRACK, TokenUtils.getMatchingOpeningBracket(JavaTokenTypes.RBRACK));
        assertEquals("Right brace should match left brace",
                     JavaTokenTypes.LCURLY, TokenUtils.getMatchingOpeningBracket(JavaTokenTypes.RCURLY));

        // Test invalid input
        assertEquals("Invalid bracket should return -1",
                     -1, TokenUtils.getMatchingOpeningBracket(JavaTokenTypes.PLUS));
        assertEquals("Left paren should return -1",
                     -1, TokenUtils.getMatchingOpeningBracket(JavaTokenTypes.LPAREN));
    }

    @Test
    public void testFormatUnexpectedTokenError() {
        LocatableToken token = createToken(JavaTokenTypes.PLUS, "+", 1, 5);
        String result = TokenUtils.formatUnexpectedTokenError("identifier", token);
        assertEquals("Expected identifier, but got plus operator '+'", result);
    }

    @Test
    public void testFormatUnexpectedTokenErrorWithNull() {
        String result = TokenUtils.formatUnexpectedTokenError("identifier", null);
        assertEquals("Expected identifier, but encountered null token", result);
    }

    @Test
    public void testFormatMissingTokenError() {
        String result = TokenUtils.formatMissingTokenError("semicolon", "after statement");
        assertEquals("Expected semicolon after statement", result);
    }

    @Test
    public void testFormatMissingTokenErrorWithoutContext() {
        String result = TokenUtils.formatMissingTokenError("semicolon", null);
        assertEquals("Expected semicolon", result);

        result = TokenUtils.formatMissingTokenError("semicolon", "");
        assertEquals("Expected semicolon", result);
    }

    @Test
    public void testValidateTokenNotNull() {
        LocatableToken token = createToken(JavaTokenTypes.IDENT, "test", 1, 1);
        assertTrue("Valid token should pass validation", TokenUtils.validateTokenNotNull(token, "test context"));

        assertFalse("Null token should fail validation", TokenUtils.validateTokenNotNull(null, "test context"));
    }

    @Test
    public void testGetConstructDescription() {
        assertEquals("literal value", TokenUtils.getConstructDescription(JavaTokenTypes.NUM_INT));
        assertEquals("literal value", TokenUtils.getConstructDescription(JavaTokenTypes.STRING_LITERAL));
        assertEquals("binary operator", TokenUtils.getConstructDescription(JavaTokenTypes.PLUS));
        assertEquals("binary operator", TokenUtils.getConstructDescription(JavaTokenTypes.EQUAL));
        assertEquals("opening bracket", TokenUtils.getConstructDescription(JavaTokenTypes.LPAREN));
        assertEquals("closing bracket", TokenUtils.getConstructDescription(JavaTokenTypes.RPAREN));
        assertEquals("identifier", TokenUtils.getConstructDescription(JavaTokenTypes.IDENT));
        assertEquals("end of file", TokenUtils.getConstructDescription(JavaTokenTypes.EOF));
        assertEquals("language construct", TokenUtils.getConstructDescription(JavaTokenTypes.LITERAL_class));
    }

    @Test
    public void testIsUnaryOperator() {
        // Test unary operators
        assertTrue("Plus should be unary", TokenUtils.isUnaryOperator(JavaTokenTypes.PLUS));
        assertTrue("Minus should be unary", TokenUtils.isUnaryOperator(JavaTokenTypes.MINUS));
        assertTrue("Logical NOT should be unary", TokenUtils.isUnaryOperator(JavaTokenTypes.LNOT));
        assertTrue("Bitwise NOT should be unary", TokenUtils.isUnaryOperator(JavaTokenTypes.BNOT));
        assertTrue("Increment should be unary", TokenUtils.isUnaryOperator(JavaTokenTypes.INC));
        assertTrue("Decrement should be unary", TokenUtils.isUnaryOperator(JavaTokenTypes.DEC));

        // Test non-unary tokens
        assertFalse("Multiplication should not be unary", TokenUtils.isUnaryOperator(JavaTokenTypes.STAR));
        assertFalse("Equality should not be unary", TokenUtils.isUnaryOperator(JavaTokenTypes.EQUAL));
        assertFalse("Identifier should not be unary", TokenUtils.isUnaryOperator(JavaTokenTypes.IDENT));
        assertFalse("Left paren should not be unary", TokenUtils.isUnaryOperator(JavaTokenTypes.LPAREN));
    }

    @Test
    public void testUtilityClassCannotBeInstantiated() {
        try {
            // Use reflection to try to instantiate the utility class
            java.lang.reflect.Constructor<TokenUtils> constructor = TokenUtils.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
            fail("Should not be able to instantiate TokenUtils");
        } catch (Exception e) {
            // Expected - should throw an exception
            assertTrue("Should throw UnsupportedOperationException",
                      e.getCause() instanceof UnsupportedOperationException);
        }
    }

    // Helper methods

    /**
     * Creates a mock LocatableToken for testing.
     */
    private LocatableToken createToken(int type, String text, int line, int column) {
        bluej.parser.lexer.LineColPos begin = new bluej.parser.lexer.LineColPos(line, column, 0);
        bluej.parser.lexer.LineColPos end = new bluej.parser.lexer.LineColPos(line, column + text.length(), text.length());
        return new LocatableToken(type, text, begin, end);
    }
}
