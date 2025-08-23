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

/**
 * Utility class providing common token operations and categorization methods
 * for the Kotlin Pratt parser implementation.
 *
 * <p>This class centralizes token-related utilities that are used across
 * multiple parselets, including:</p>
 * <ul>
 *   <li>Human-readable token type descriptions for error messages</li>
 *   <li>Token category validation (literals, operators, brackets, etc.)</li>
 *   <li>Common token checking and validation methods</li>
 *   <li>Error message formatting helpers</li>
 * </ul>
 *
 * <p>All methods in this class are static and thread-safe, making them
 * suitable for use in the thread-agnostic parser architecture.</p>
 *
 * @author BlueJ Team
 * @see bluej.parser.pratt.parselets
 */
public final class TokenUtils {

    // Private constructor to prevent instantiation
    private TokenUtils() {
        throw new UnsupportedOperationException("TokenUtils is a utility class and should not be instantiated");
    }

    /**
     * Gets a human-readable description of a token type.
     *
     * <p>This method provides descriptive names for token types to improve
     * error messages and debugging information throughout the parser.</p>
     *
     * @param tokenType The token type constant from JavaTokenTypes
     * @return A descriptive name for the token type
     */
    public static String getTokenTypeName(int tokenType) {
        return switch (tokenType) {
            // Literals
            case JavaTokenTypes.NUM_INT -> "integer literal";
            case JavaTokenTypes.NUM_LONG -> "long literal";
            case JavaTokenTypes.NUM_FLOAT -> "float literal";
            case JavaTokenTypes.NUM_DOUBLE -> "double literal";
            case JavaTokenTypes.STRING_LITERAL -> "string literal";
            case JavaTokenTypes.STRING_LITERAL_MULTILINE -> "multiline string literal";
            case JavaTokenTypes.CHAR_LITERAL -> "character literal";
            case JavaTokenTypes.LITERAL_true -> "boolean literal 'true'";
            case JavaTokenTypes.LITERAL_false -> "boolean literal 'false'";
            case JavaTokenTypes.LITERAL_null -> "null literal";

            // Identifiers and keywords
            case JavaTokenTypes.IDENT -> "identifier";
            case JavaTokenTypes.LITERAL_class -> "keyword 'class'";
            case JavaTokenTypes.LITERAL_fun -> "keyword 'fun'";
            case JavaTokenTypes.LITERAL_val -> "keyword 'val'";
            case JavaTokenTypes.LITERAL_var -> "keyword 'var'";
            case JavaTokenTypes.LITERAL_if -> "keyword 'if'";
            case JavaTokenTypes.LITERAL_else -> "keyword 'else'";
            case JavaTokenTypes.LITERAL_for -> "keyword 'for'";
            case JavaTokenTypes.LITERAL_while -> "keyword 'while'";
            case JavaTokenTypes.LITERAL_when -> "keyword 'when'";
            case JavaTokenTypes.LITERAL_try -> "keyword 'try'";
            case JavaTokenTypes.LITERAL_catch -> "keyword 'catch'";
            case JavaTokenTypes.LITERAL_finally -> "keyword 'finally'";
            case JavaTokenTypes.LITERAL_return -> "keyword 'return'";
            case JavaTokenTypes.LITERAL_break -> "keyword 'break'";
            case JavaTokenTypes.LITERAL_continue -> "keyword 'continue'";

            // Arithmetic operators
            case JavaTokenTypes.PLUS -> "plus operator '+'";
            case JavaTokenTypes.MINUS -> "minus operator '-'";
            case JavaTokenTypes.STAR -> "multiplication operator '*'";
            case JavaTokenTypes.DIV -> "division operator '/'";
            case JavaTokenTypes.MOD -> "modulo operator '%'";

            // Comparison operators
            case JavaTokenTypes.EQUAL -> "equality operator '=='";
            case JavaTokenTypes.NOT_EQUAL -> "inequality operator '!='";
            case JavaTokenTypes.LT -> "less-than operator '<'";
            case JavaTokenTypes.LE -> "less-than-or-equal operator '<='";
            case JavaTokenTypes.GT -> "greater-than operator '>'";
            case JavaTokenTypes.GE -> "greater-than-or-equal operator '>='";

            // Logical operators
            case JavaTokenTypes.LAND -> "logical AND operator '&&'";
            case JavaTokenTypes.LOR -> "logical OR operator '||'";
            case JavaTokenTypes.LNOT -> "logical NOT operator '!'";

            // Bitwise operators
            case JavaTokenTypes.BAND -> "bitwise AND operator '&'";
            case JavaTokenTypes.BOR -> "bitwise OR operator '|'";
            case JavaTokenTypes.BXOR -> "bitwise XOR operator '^'";
            case JavaTokenTypes.BNOT -> "bitwise NOT operator '~'";
            case JavaTokenTypes.SL -> "left shift operator '<<'";
            case JavaTokenTypes.SR -> "right shift operator '>>'";
            case JavaTokenTypes.BSR -> "unsigned right shift operator '>>>'";

            // Assignment operators
            case JavaTokenTypes.ASSIGN -> "assignment operator '='";
            case JavaTokenTypes.PLUS_ASSIGN -> "plus-assign operator '+='";
            case JavaTokenTypes.MINUS_ASSIGN -> "minus-assign operator '-='";
            case JavaTokenTypes.STAR_ASSIGN -> "multiply-assign operator '*='";
            case JavaTokenTypes.DIV_ASSIGN -> "divide-assign operator '/='";
            case JavaTokenTypes.MOD_ASSIGN -> "modulo-assign operator '%='";

            // Brackets and delimiters
            case JavaTokenTypes.LPAREN -> "left parenthesis '('";
            case JavaTokenTypes.RPAREN -> "right parenthesis ')'";
            case JavaTokenTypes.LBRACK -> "left bracket '['";
            case JavaTokenTypes.RBRACK -> "right bracket ']'";
            case JavaTokenTypes.LCURLY -> "left brace '{'";
            case JavaTokenTypes.RCURLY -> "right brace '}'";

            // Punctuation
            case JavaTokenTypes.SEMI -> "semicolon ';'";
            case JavaTokenTypes.COMMA -> "comma ','";
            case JavaTokenTypes.DOT -> "dot '.'";
            case JavaTokenTypes.COLON -> "colon ':'";
            case JavaTokenTypes.QUESTION -> "question mark '?'";

            // Special tokens
            case JavaTokenTypes.EOF -> "end of file";

            // Default case for unknown tokens
            default -> "token type " + tokenType;
        };
    }

    /**
     * Determines if a token type represents a literal value.
     *
     * <p>This method checks whether the given token type is one of the
     * recognized literal types in Kotlin.</p>
     *
     * @param tokenType The token type to check
     * @return true if the token represents a literal, false otherwise
     */
    public static boolean isLiteralToken(int tokenType) {
        return switch (tokenType) {
            case JavaTokenTypes.NUM_INT,
                 JavaTokenTypes.NUM_LONG,
                 JavaTokenTypes.NUM_FLOAT,
                 JavaTokenTypes.NUM_DOUBLE,
                 JavaTokenTypes.STRING_LITERAL,
                 JavaTokenTypes.STRING_LITERAL_MULTILINE,
                 JavaTokenTypes.CHAR_LITERAL,
                 JavaTokenTypes.LITERAL_true,
                 JavaTokenTypes.LITERAL_false,
                 JavaTokenTypes.LITERAL_null -> true;
            default -> false;
        };
    }

    /**
     * Determines if a token type represents a binary arithmetic operator.
     *
     * @param tokenType The token type to check
     * @return true if the token is a binary arithmetic operator, false otherwise
     */
    public static boolean isArithmeticOperator(int tokenType) {
        return switch (tokenType) {
            case JavaTokenTypes.PLUS,
                 JavaTokenTypes.MINUS,
                 JavaTokenTypes.STAR,
                 JavaTokenTypes.DIV,
                 JavaTokenTypes.MOD -> true;
            default -> false;
        };
    }

    /**
     * Determines if a token type represents a comparison operator.
     *
     * @param tokenType The token type to check
     * @return true if the token is a comparison operator, false otherwise
     */
    public static boolean isComparisonOperator(int tokenType) {
        return switch (tokenType) {
            case JavaTokenTypes.EQUAL,
                 JavaTokenTypes.NOT_EQUAL,
                 JavaTokenTypes.LT,
                 JavaTokenTypes.LE,
                 JavaTokenTypes.GT,
                 JavaTokenTypes.GE -> true;
            default -> false;
        };
    }

    /**
     * Determines if a token type represents a logical operator.
     *
     * @param tokenType The token type to check
     * @return true if the token is a logical operator, false otherwise
     */
    public static boolean isLogicalOperator(int tokenType) {
        return switch (tokenType) {
            case JavaTokenTypes.LAND,
                 JavaTokenTypes.LOR,
                 JavaTokenTypes.LNOT -> true;
            default -> false;
        };
    }

    /**
     * Determines if a token type represents a bitwise operator.
     *
     * @param tokenType The token type to check
     * @return true if the token is a bitwise operator, false otherwise
     */
    public static boolean isBitwiseOperator(int tokenType) {
        return switch (tokenType) {
            case JavaTokenTypes.BAND,
                 JavaTokenTypes.BOR,
                 JavaTokenTypes.BXOR,
                 JavaTokenTypes.BNOT,
                 JavaTokenTypes.SL,
                 JavaTokenTypes.SR,
                 JavaTokenTypes.BSR -> true;
            default -> false;
        };
    }

    /**
     * Determines if a token type represents an assignment operator.
     *
     * @param tokenType The token type to check
     * @return true if the token is an assignment operator, false otherwise
     */
    public static boolean isAssignmentOperator(int tokenType) {
        return switch (tokenType) {
            case JavaTokenTypes.ASSIGN,
                 JavaTokenTypes.PLUS_ASSIGN,
                 JavaTokenTypes.MINUS_ASSIGN,
                 JavaTokenTypes.STAR_ASSIGN,
                 JavaTokenTypes.DIV_ASSIGN,
                 JavaTokenTypes.MOD_ASSIGN -> true;
            default -> false;
        };
    }

    /**
     * Determines if a token type represents any binary operator.
     *
     * @param tokenType The token type to check
     * @return true if the token is any binary operator, false otherwise
     */
    public static boolean isBinaryOperator(int tokenType) {
        return isArithmeticOperator(tokenType) ||
               isComparisonOperator(tokenType) ||
               isLogicalOperator(tokenType) ||
               isBitwiseOperator(tokenType) ||
               isAssignmentOperator(tokenType);
    }

    /**
     * Determines if a token type represents an opening bracket.
     *
     * @param tokenType The token type to check
     * @return true if the token is an opening bracket, false otherwise
     */
    public static boolean isOpeningBracket(int tokenType) {
        return switch (tokenType) {
            case JavaTokenTypes.LPAREN,
                 JavaTokenTypes.LBRACK,
                 JavaTokenTypes.LCURLY -> true;
            default -> false;
        };
    }

    /**
     * Determines if a token type represents a closing bracket.
     *
     * @param tokenType The token type to check
     * @return true if the token is a closing bracket, false otherwise
     */
    public static boolean isClosingBracket(int tokenType) {
        return switch (tokenType) {
            case JavaTokenTypes.RPAREN,
                 JavaTokenTypes.RBRACK,
                 JavaTokenTypes.RCURLY -> true;
            default -> false;
        };
    }

    /**
     * Gets the matching closing bracket for an opening bracket.
     *
     * @param openingBracket The opening bracket token type
     * @return The corresponding closing bracket token type, or -1 if not a valid opening bracket
     */
    public static int getMatchingClosingBracket(int openingBracket) {
        return switch (openingBracket) {
            case JavaTokenTypes.LPAREN -> JavaTokenTypes.RPAREN;
            case JavaTokenTypes.LBRACK -> JavaTokenTypes.RBRACK;
            case JavaTokenTypes.LCURLY -> JavaTokenTypes.RCURLY;
            default -> -1;
        };
    }

    /**
     * Gets the matching opening bracket for a closing bracket.
     *
     * @param closingBracket The closing bracket token type
     * @return The corresponding opening bracket token type, or -1 if not a valid closing bracket
     */
    public static int getMatchingOpeningBracket(int closingBracket) {
        return switch (closingBracket) {
            case JavaTokenTypes.RPAREN -> JavaTokenTypes.LPAREN;
            case JavaTokenTypes.RBRACK -> JavaTokenTypes.LBRACK;
            case JavaTokenTypes.RCURLY -> JavaTokenTypes.LCURLY;
            default -> -1;
        };
    }

    /**
     * Creates a formatted error message for an unexpected token.
     *
     * @param expectedDescription Description of what was expected
     * @param actualToken The actual token that was encountered
     * @return A formatted error message
     */
    public static String formatUnexpectedTokenError(String expectedDescription, LocatableToken actualToken) {
        if (actualToken == null) {
            return String.format("Expected %s, but encountered null token", expectedDescription);
        }
        return String.format("Expected %s, but got %s", expectedDescription, getTokenTypeName(actualToken.getType()));
    }

    /**
     * Creates a formatted error message for a missing token.
     *
     * @param expectedDescription Description of what was expected
     * @param context Optional context description (can be null)
     * @return A formatted error message
     */
    public static String formatMissingTokenError(String expectedDescription, String context) {
        if (context != null && !context.isEmpty()) {
            return String.format("Expected %s %s", expectedDescription, context);
        }
        return String.format("Expected %s", expectedDescription);
    }

    /**
     * Validates that a token is not null and reports an appropriate error if it is.
     *
     * @param token The token to validate
     * @param context Description of the parsing context for error reporting
     * @return true if the token is valid (not null), false otherwise
     */
    public static boolean validateTokenNotNull(LocatableToken token, String context) {
        return token != null;
    }

    /**
     * Gets a description of the construct that a token type typically represents.
     * This is useful for error messages and documentation.
     *
     * @param tokenType The token type to describe
     * @return A description of the construct this token type represents
     */
    public static String getConstructDescription(int tokenType) {
        if (isLiteralToken(tokenType)) {
            return "literal value";
        } else if (isBinaryOperator(tokenType)) {
            return "binary operator";
        } else if (isOpeningBracket(tokenType)) {
            return "opening bracket";
        } else if (isClosingBracket(tokenType)) {
            return "closing bracket";
        } else if (tokenType == JavaTokenTypes.IDENT) {
            return "identifier";
        } else if (tokenType == JavaTokenTypes.EOF) {
            return "end of file";
        } else {
            return "language construct";
        }
    }

    /**
     * Determines if a token represents a unary operator.
     *
     * @param tokenType The token type to check
     * @return true if the token can be used as a unary operator, false otherwise
     */
    public static boolean isUnaryOperator(int tokenType) {
        return switch (tokenType) {
            case JavaTokenTypes.PLUS,    // unary plus: +x
                 JavaTokenTypes.MINUS,   // unary minus: -x
                 JavaTokenTypes.LNOT,    // logical not: !x
                 JavaTokenTypes.BNOT,    // bitwise not: ~x
                 JavaTokenTypes.INC,     // increment: ++x
                 JavaTokenTypes.DEC -> true;  // decrement: --x
            default -> false;
        };
    }
}
