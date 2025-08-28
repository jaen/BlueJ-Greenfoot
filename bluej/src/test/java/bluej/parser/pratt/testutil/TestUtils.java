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
package bluej.parser.pratt.testutil;

import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LineColPos;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.pratt.TestNodeFactory;

import java.util.List;

/**
 * Utility class providing common test helpers for parser tests.
 *
 * <p>This class contains factory methods for creating tokens, nodes,
 * and other test objects commonly used across parser tests.</p>
 *
 * @author BlueJ Team
 * @since BlueJ 5.4.0
 */
public final class TestUtils {

    // Private constructor to prevent instantiation
    private TestUtils() {
        throw new AssertionError("TestUtils is a utility class and should not be instantiated");
    }

    // Token creation methods

    /**
     * Creates a token with the specified type and text at position (1, 1).
     *
     * @param type Token type from JavaTokenTypes
     * @param text Token text
     * @return Created token
     */
    public static LocatableToken createToken(int type, String text) {
        return createToken(type, text, 1, 1);
    }

    /**
     * Creates a token with the specified type, text, and position.
     *
     * @param type Token type from JavaTokenTypes
     * @param text Token text
     * @param line Line number (1-based)
     * @param column Column number (1-based)
     * @return Created token
     */
    public static LocatableToken createToken(int type, String text, int line, int column) {
        LineColPos begin = new LineColPos(line, column, 0);
        LineColPos end = new LineColPos(line, column + text.length(), text.length());
        return new LocatableToken(type, text, begin, end);
    }

    /**
     * Creates an EOF token at the default position (1, 1).
     *
     * @return EOF token
     */
    public static LocatableToken createEOFToken() {
        return createEOFToken(1, 1);
    }

    /**
     * Creates an EOF token at the specified position.
     *
     * @param line Line number (1-based)
     * @param column Column number (1-based)
     * @return EOF token
     */
    public static LocatableToken createEOFToken(int line, int column) {
        LineColPos pos = new LineColPos(line, column, 0);
        return new LocatableToken(JavaTokenTypes.EOF, "", pos, pos);
    }

    // Node creation methods

    /**
     * Creates an identifier node with the specified name.
     *
     * @param name Identifier name
     * @return Identifier node
     */
    public static ParsedNode createIdentifierNode(String name) {
        return createIdentifierNode(name, 1, 1);
    }

    /**
     * Creates an identifier node with the specified name and position.
     *
     * @param name Identifier name
     * @param line Line number (1-based)
     * @param column Column number (1-based)
     * @return Identifier node
     */
    public static ParsedNode createIdentifierNode(String name, int line, int column) {
        TestNodeFactory factory = new TestNodeFactory();
        LocatableToken token = createToken(JavaTokenTypes.IDENT, name, line, column);
        return factory.createIdentifierNode(token);
    }

    /**
     * Creates a literal node for an integer value.
     *
     * @param value Integer value as string
     * @return Literal node
     */
    public static ParsedNode createIntLiteralNode(String value) {
        return createIntLiteralNode(value, 1, 1);
    }

    /**
     * Creates a literal node for an integer value at the specified position.
     *
     * @param value Integer value as string
     * @param line Line number (1-based)
     * @param column Column number (1-based)
     * @return Literal node
     */
    public static ParsedNode createIntLiteralNode(String value, int line, int column) {
        TestNodeFactory factory = new TestNodeFactory();
        LocatableToken token = createToken(JavaTokenTypes.NUM_INT, value, line, column);
        return factory.createLiteralNode(token);
    }

    /**
     * Creates a literal node for a string value.
     *
     * @param value String value (including quotes)
     * @return Literal node
     */
    public static ParsedNode createStringLiteralNode(String value) {
        return createStringLiteralNode(value, 1, 1);
    }

    /**
     * Creates a literal node for a string value at the specified position.
     *
     * @param value String value (including quotes)
     * @param line Line number (1-based)
     * @param column Column number (1-based)
     * @return Literal node
     */
    public static ParsedNode createStringLiteralNode(String value, int line, int column) {
        TestNodeFactory factory = new TestNodeFactory();
        LocatableToken token = createToken(JavaTokenTypes.STRING_LITERAL, value, line, column);
        return factory.createLiteralNode(token);
    }

    /**
     * Creates a literal node for a boolean value.
     *
     * @param value Boolean value
     * @return Literal node
     */
    public static ParsedNode createBooleanLiteralNode(boolean value) {
        return createBooleanLiteralNode(value, 1, 1);
    }

    /**
     * Creates a literal node for a boolean value at the specified position.
     *
     * @param value Boolean value
     * @param line Line number (1-based)
     * @param column Column number (1-based)
     * @return Literal node
     */
    public static ParsedNode createBooleanLiteralNode(boolean value, int line, int column) {
        TestNodeFactory factory = new TestNodeFactory();
        int tokenType = value ? JavaTokenTypes.LITERAL_true : JavaTokenTypes.LITERAL_false;
        String text = value ? "true" : "false";
        LocatableToken token = createToken(tokenType, text, line, column);
        return factory.createLiteralNode(token);
    }

    /**
     * Creates a null literal node.
     *
     * @return Null literal node
     */
    public static ParsedNode createNullLiteralNode() {
        return createNullLiteralNode(1, 1);
    }

    /**
     * Creates a null literal node at the specified position.
     *
     * @param line Line number (1-based)
     * @param column Column number (1-based)
     * @return Null literal node
     */
    public static ParsedNode createNullLiteralNode(int line, int column) {
        TestNodeFactory factory = new TestNodeFactory();
        LocatableToken token = createToken(JavaTokenTypes.LITERAL_null, "null", line, column);
        return factory.createLiteralNode(token);
    }

    // Common token sequences

    /**
     * Creates a sequence of tokens for a simple binary expression.
     * Example: "a + b"
     *
     * @param left Left operand text
     * @param operator Operator token type
     * @param operatorText Operator text
     * @param right Right operand text
     * @return Array of tokens
     */
    public static LocatableToken[] createBinaryExpressionTokens(
            String left, int operator, String operatorText, String right) {
        return new LocatableToken[] {
            createToken(JavaTokenTypes.IDENT, left, 1, 1),
            createToken(operator, operatorText, 1, left.length() + 2),
            createToken(JavaTokenTypes.IDENT, right, 1, left.length() + operatorText.length() + 3)
        };
    }

    /**
     * Creates a sequence of tokens for a function call.
     * Example: "func(arg1, arg2)"
     *
     * @param functionName Function name
     * @param arguments Argument names
     * @return Array of tokens
     */
    public static LocatableToken[] createFunctionCallTokens(String functionName, String... arguments) {
        List<LocatableToken> tokens = new java.util.ArrayList<>();
        int column = 1;

        // Function name
        tokens.add(createToken(JavaTokenTypes.IDENT, functionName, 1, column));
        column += functionName.length();

        // Opening parenthesis
        tokens.add(createToken(JavaTokenTypes.LPAREN, "(", 1, column));
        column++;

        // Arguments
        for (int i = 0; i < arguments.length; i++) {
            tokens.add(createToken(JavaTokenTypes.IDENT, arguments[i], 1, column));
            column += arguments[i].length();

            if (i < arguments.length - 1) {
                tokens.add(createToken(JavaTokenTypes.COMMA, ",", 1, column));
                column++;
                column++; // Space after comma
            }
        }

        // Closing parenthesis
        tokens.add(createToken(JavaTokenTypes.RPAREN, ")", 1, column));

        return tokens.toArray(new LocatableToken[0]);
    }

    /**
     * Gets a human-readable description of a token type.
     *
     * @param tokenType Token type from JavaTokenTypes
     * @return Token type description
     */
    public static String getTokenTypeName(int tokenType) {
        switch (tokenType) {
            // Literals
            case JavaTokenTypes.NUM_INT: return "integer literal";
            case JavaTokenTypes.NUM_FLOAT: return "float literal";
            case JavaTokenTypes.NUM_LONG: return "long literal";
            case JavaTokenTypes.NUM_DOUBLE: return "double literal";
            case JavaTokenTypes.STRING_LITERAL: return "string literal";
            case JavaTokenTypes.LITERAL_true: return "true";
            case JavaTokenTypes.LITERAL_false: return "false";
            case JavaTokenTypes.LITERAL_null: return "null";

            // Identifiers and keywords
            case JavaTokenTypes.IDENT: return "identifier";
            case JavaTokenTypes.LITERAL_this: return "this";
            case JavaTokenTypes.LITERAL_super: return "super";

            // Operators
            case JavaTokenTypes.PLUS: return "+";
            case JavaTokenTypes.MINUS: return "-";
            case JavaTokenTypes.STAR: return "*";
            case JavaTokenTypes.DIV: return "/";
            case JavaTokenTypes.MOD: return "%";
            case JavaTokenTypes.ASSIGN: return "=";
            case JavaTokenTypes.EQUAL: return "==";
            case JavaTokenTypes.NOT_EQUAL: return "!=";
            case JavaTokenTypes.LT: return "<";
            case JavaTokenTypes.LE: return "<=";
            case JavaTokenTypes.GT: return ">";
            case JavaTokenTypes.GE: return ">=";
            case JavaTokenTypes.LAND: return "&&";
            case JavaTokenTypes.LOR: return "||";
            case JavaTokenTypes.LNOT: return "!";
            case JavaTokenTypes.INC: return "++";
            case JavaTokenTypes.DEC: return "--";
            case JavaTokenTypes.DOT: return ".";
            case JavaTokenTypes.SAFE_ACCESS: return "?.";

            // Delimiters
            case JavaTokenTypes.LPAREN: return "(";
            case JavaTokenTypes.RPAREN: return ")";
            case JavaTokenTypes.LBRACK: return "[";
            case JavaTokenTypes.RBRACK: return "]";
            case JavaTokenTypes.LCURLY: return "{";
            case JavaTokenTypes.RCURLY: return "}";
            case JavaTokenTypes.COMMA: return ",";
            case JavaTokenTypes.SEMI: return ";";
            case JavaTokenTypes.COLON: return ":";

            // Special
            case JavaTokenTypes.EOF: return "EOF";

            default: return "token type " + tokenType;
        }
    }
}
