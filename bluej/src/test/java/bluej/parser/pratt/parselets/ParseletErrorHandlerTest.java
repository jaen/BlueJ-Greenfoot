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
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test class for ParseletErrorHandler utility methods.
 *
 * <p>This test class verifies the standardized error handling functionality
 * provided by ParseletErrorHandler, focusing on the utility methods and
 * token type name resolution.</p>
 *
 * @author BlueJ Kotlin Implementation
 * @since 2025-01-24
 */
public class ParseletErrorHandlerTest {

    // ===== TOKEN TYPE UTILITY TESTS =====

    @Test
    public void testGetTokenTypeName_LiteralTypes() {
        assertEquals("Should return correct name for integer literal",
                    "integer literal", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.NUM_INT));
        assertEquals("Should return correct name for long literal",
                    "long literal", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.NUM_LONG));
        assertEquals("Should return correct name for float literal",
                    "float literal", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.NUM_FLOAT));
        assertEquals("Should return correct name for double literal",
                    "double literal", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.NUM_DOUBLE));
        assertEquals("Should return correct name for string literal",
                    "string literal", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.STRING_LITERAL));
        assertEquals("Should return correct name for character literal",
                    "character literal", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.CHAR_LITERAL));
        assertEquals("Should return correct name for true literal",
                    "'true'", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.LITERAL_true));
        assertEquals("Should return correct name for false literal",
                    "'false'", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.LITERAL_false));
        assertEquals("Should return correct name for null literal",
                    "'null'", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.LITERAL_null));
    }

    @Test
    public void testGetTokenTypeName_IdentifierAndKeywords() {
        assertEquals("Should return correct name for identifier",
                    "identifier", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.IDENT));
        assertEquals("Should return correct name for this keyword",
                    "'this'", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.LITERAL_this));
        assertEquals("Should return correct name for super keyword",
                    "'super'", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.LITERAL_super));
    }

    @Test
    public void testGetTokenTypeName_ArithmeticOperators() {
        assertEquals("Should return correct name for plus operator",
                    "'+'", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.PLUS));
        assertEquals("Should return correct name for minus operator",
                    "'-'", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.MINUS));
        assertEquals("Should return correct name for multiply operator",
                    "'*'", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.STAR));
        assertEquals("Should return correct name for divide operator",
                    "'/'", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.DIV));
        assertEquals("Should return correct name for modulo operator",
                    "'%'", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.MOD));
        assertEquals("Should return correct name for increment operator",
                    "'++'", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.INC));
        assertEquals("Should return correct name for decrement operator",
                    "'--'", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.DEC));
    }

    @Test
    public void testGetTokenTypeName_ComparisonOperators() {
        assertEquals("Should return correct name for assignment operator",
                    "'='", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.ASSIGN));
        assertEquals("Should return correct name for equality operator",
                    "'=='", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.EQUAL));
        assertEquals("Should return correct name for inequality operator",
                    "'!='", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.NOT_EQUAL));
        assertEquals("Should return correct name for less than operator",
                    "'<'", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.LT));
        assertEquals("Should return correct name for greater than operator",
                    "'>'", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.GT));
        assertEquals("Should return correct name for less than or equal operator",
                    "'<='", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.LE));
        assertEquals("Should return correct name for greater than or equal operator",
                    "'>='", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.GE));
    }

    @Test
    public void testGetTokenTypeName_LogicalOperators() {
        assertEquals("Should return correct name for logical and operator",
                    "'&&'", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.LAND));
        assertEquals("Should return correct name for logical or operator",
                    "'||'", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.LOR));
        assertEquals("Should return correct name for logical not operator",
                    "'!'", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.LNOT));
    }

    @Test
    public void testGetTokenTypeName_DelimiterTypes() {
        assertEquals("Should return correct name for left parenthesis",
                    "'('", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.LPAREN));
        assertEquals("Should return correct name for right parenthesis",
                    "')'", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.RPAREN));
        assertEquals("Should return correct name for left bracket",
                    "'['", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.LBRACK));
        assertEquals("Should return correct name for right bracket",
                    "']'", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.RBRACK));
        assertEquals("Should return correct name for left brace",
                    "'{'", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.LCURLY));
        assertEquals("Should return correct name for right brace",
                    "'}'", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.RCURLY));
        assertEquals("Should return correct name for semicolon",
                    "';'", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.SEMI));
        assertEquals("Should return correct name for comma",
                    "','", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.COMMA));
        assertEquals("Should return correct name for dot",
                    "'.'", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.DOT));
        assertEquals("Should return correct name for question mark",
                    "'?'", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.QUESTION));
        assertEquals("Should return correct name for colon",
                    "':'", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.COLON));
    }

    @Test
    public void testGetTokenTypeName_SpecialCases() {
        assertEquals("Should return correct name for end of file",
                    "end of file", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.EOF));
        assertEquals("Should return correct name for whitespace",
                    "whitespace", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.WHITESPACE));
        assertEquals("Should return correct name for invalid token",
                    "invalid token", ParseletErrorHandler.getTokenTypeName(JavaTokenTypes.INVALID));
    }

    @Test
    public void testGetTokenTypeName_UnknownType() {
        int unknownType = 99999;
        String result = ParseletErrorHandler.getTokenTypeName(unknownType);

        assertTrue("Should return generic description for unknown type",
                  result.contains("token type") && result.contains(String.valueOf(unknownType)));
        assertEquals("Should return expected generic format",
                    "token type " + unknownType, result);
    }

    @Test
    public void testGetTokenTypeName_NegativeType() {
        int negativeType = -1;
        String result = ParseletErrorHandler.getTokenTypeName(negativeType);

        assertEquals("Should return generic description for negative type",
                    "token type -1", result);
    }

    @Test
    public void testGetTokenTypeName_ZeroType() {
        String result = ParseletErrorHandler.getTokenTypeName(0);
        assertEquals("Should return generic description for zero type",
                    "token type 0", result);
    }

    // ===== CONSTRUCTOR TEST =====

    @Test(expected = AssertionError.class)
    public void testConstructorThrowsAssertionError() {
        // This should throw AssertionError since ParseletErrorHandler is a utility class
        // We use reflection to bypass the private constructor for testing
        try {
            java.lang.reflect.Constructor<ParseletErrorHandler> constructor =
                ParseletErrorHandler.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        } catch (Exception e) {
            if (e.getCause() instanceof AssertionError) {
                throw (AssertionError) e.getCause();
            }
            fail("Expected AssertionError but got: " + e.getClass().getSimpleName());
        }
    }

    // ===== COMPREHENSIVE TOKEN TYPE COVERAGE TEST =====

    @Test
    public void testGetTokenTypeName_AllKnownTypes() {
        // Test that all commonly used token types have descriptive names
        // (not just "token type X")

        // These are the token types that should have specific descriptions
        int[] knownTypes = {
            // Literals
            JavaTokenTypes.NUM_INT, JavaTokenTypes.NUM_LONG, JavaTokenTypes.NUM_FLOAT,
            JavaTokenTypes.NUM_DOUBLE, JavaTokenTypes.CHAR_LITERAL, JavaTokenTypes.STRING_LITERAL,
            JavaTokenTypes.LITERAL_true, JavaTokenTypes.LITERAL_false, JavaTokenTypes.LITERAL_null,

            // Identifiers and keywords
            JavaTokenTypes.IDENT, JavaTokenTypes.LITERAL_this, JavaTokenTypes.LITERAL_super,

            // Operators
            JavaTokenTypes.PLUS, JavaTokenTypes.MINUS, JavaTokenTypes.STAR, JavaTokenTypes.DIV,
            JavaTokenTypes.MOD, JavaTokenTypes.ASSIGN, JavaTokenTypes.EQUAL, JavaTokenTypes.NOT_EQUAL,
            JavaTokenTypes.LT, JavaTokenTypes.GT, JavaTokenTypes.LE, JavaTokenTypes.GE,
            JavaTokenTypes.LAND, JavaTokenTypes.LOR, JavaTokenTypes.LNOT,
            JavaTokenTypes.INC, JavaTokenTypes.DEC,

            // Delimiters
            JavaTokenTypes.LPAREN, JavaTokenTypes.RPAREN, JavaTokenTypes.LBRACK, JavaTokenTypes.RBRACK,
            JavaTokenTypes.LCURLY, JavaTokenTypes.RCURLY, JavaTokenTypes.SEMI, JavaTokenTypes.COMMA,
            JavaTokenTypes.DOT, JavaTokenTypes.QUESTION, JavaTokenTypes.COLON,

            // Special cases
            JavaTokenTypes.EOF, JavaTokenTypes.WHITESPACE, JavaTokenTypes.INVALID
        };

        for (int tokenType : knownTypes) {
            String name = ParseletErrorHandler.getTokenTypeName(tokenType);
            assertFalse("Token type " + tokenType + " should have a specific name, not generic format",
                       name.startsWith("token type "));
            assertNotNull("Token type " + tokenType + " should have a non-null name", name);
            assertFalse("Token type " + tokenType + " should have a non-empty name", name.isEmpty());
        }
    }

    @Test
    public void testTokenTypeNames_NotNull() {
        // Test that getTokenTypeName never returns null for any reasonable input
        int[] testTypes = {0, -1, 1, 100, 999, Integer.MAX_VALUE, Integer.MIN_VALUE};

        for (int tokenType : testTypes) {
            String name = ParseletErrorHandler.getTokenTypeName(tokenType);
            assertNotNull("getTokenTypeName should never return null for type " + tokenType, name);
            assertFalse("getTokenTypeName should never return empty string for type " + tokenType,
                       name.isEmpty());
        }
    }
}
