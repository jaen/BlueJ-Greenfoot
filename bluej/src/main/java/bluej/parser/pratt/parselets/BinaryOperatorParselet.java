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
import bluej.parser.pratt.InfixParselet;
import bluej.parser.pratt.KotlinPrattParser;
import bluej.parser.pratt.NodeFactory;
import bluej.parser.pratt.ParseResult;
import bluej.parser.pratt.Precedence;

/**
 * A parselet for handling binary operators in Kotlin code.
 *
 * <p>This parselet handles binary arithmetic and logical operators including:</p>
 * <ul>
 *   <li>Arithmetic operators: +, -, *, /, %</li>
 *   <li>Comparison operators: ==, !=, <, <=, >, >=</li>
 *   <li>Logical operators: &&, ||</li>
 *   <li>Bitwise operators: &, |, ^, <<, >>, >>></li>
 *   <li>Assignment operators: =, +=, -=, *=, /=, %=</li>
 * </ul>
 *
 * <p>The parselet respects operator precedence and associativity rules as defined
 * in the Kotlin Language Specification. Most binary operators are left-associative,
 * with the exception of assignment operators which are right-associative.</p>
 *
 * <p>During the foundation phase, this parselet validates the operator syntax
 * and operand structure but does not create complete AST nodes.</p>
 *
 * @author BlueJ Team
 * @see <a href="https://kotlinlang.org/spec/syntax-and-grammar.html#expressions">Kotlin Language Specification - Expressions</a>
 */
public final class BinaryOperatorParselet implements InfixParselet {

    private final Precedence precedence;
    private final boolean rightAssociative;

    /**
     * Creates a binary operator parselet with the specified precedence.
     *
     * <p>Most binary operators are left-associative. Use the other constructor
     * if you need to specify right associativity (e.g., for assignment operators).</p>
     *
     * @param precedence The precedence level for this operator
     */
    public BinaryOperatorParselet(Precedence precedence) {
        this(precedence, false);
    }

    /**
     * Creates a binary operator parselet with the specified precedence and associativity.
     *
     * @param precedence The precedence level for this operator
     * @param rightAssociative true if the operator is right-associative, false for left-associative
     */
    public BinaryOperatorParselet(Precedence precedence, boolean rightAssociative) {
        this.precedence = precedence;
        this.rightAssociative = rightAssociative;
    }

    /**
     * Parses a binary operator expression.
     *
     * <p>This method validates the binary operator structure:</p>
     * <ol>
     *   <li>Validates the left operand (should not be null)</li>
     *   <li>Validates the operator token type</li>
     *   <li>Parses the right operand with appropriate precedence</li>
     *   <li>Creates the binary expression representation</li>
     * </ol>
     *
     * <p>For the foundation phase, this returns null after validation
     * to test the parselet registration and dispatch mechanism.</p>
     *
     * @param parser The parser instance for parsing the right operand
     * @param left The left operand (already parsed)
     * @param operator The binary operator token
     * @return A ParsedNode representing the binary expression, or null during foundation phase
     */
    @Override
    public ParseResult<ParsedNode> parse(KotlinPrattParser parser, ParsedNode left, LocatableToken operator) {
        // Validate left operand
        if (left == null) {
            return ParseResult.failure("Missing left operand for binary operator", operator);
        }

        if (operator == null) {
            return ParseResult.failure("Null operator token in binary operator parselet", null);
        }

        if (!isBinaryOperator(operator.getType())) {
            return ParseResult.failure("Expected binary operator, got: " + getTokenTypeName(operator.getType()), operator);
        }

        // Determine the precedence for parsing the right operand
        // Right-associative operators use precedence - 1 to allow same-precedence operators to bind to the right
        // Left-associative operators use precedence to bind same-precedence operators to the left
        int rightPrecedence = rightAssociative ? precedence.getValue() - 1 : precedence.getValue();

        // Parse the right operand
        ParseResult<ParsedNode> rightResult = parser.parseExpressionResult(rightPrecedence);
        if (rightResult.isFailure()) {
            return ParseResult.failure("Missing right operand for binary operator '" + operator.getText() + "'", operator);
        }

        ParsedNode right = rightResult.getValue();

        // Create the binary operator node using the NodeFactory
        NodeFactory nodeFactory = parser.getNodeFactory();
        if (nodeFactory == null) {
            return ParseResult.failure("NodeFactory not available for AST node creation", operator);
        }

        // Create and return the binary operator node
        ParsedNode result = nodeFactory.createBinaryOperatorNode(left, operator, right);
        return ParseResult.success(result);
    }

    /**
     * Returns the precedence level for this binary operator parselet.
     *
     * @return The precedence level
     */
    @Override
    public int getPrecedence() {
        return precedence.getValue();
    }



    /**
     * Determines if a token type represents a binary operator.
     *
     * <p>This method checks whether the given token type is one of the
     * recognized binary operator types that this parselet can handle.</p>
     *
     * @param tokenType The token type to check
     * @return true if the token represents a binary operator, false otherwise
     */
    private boolean isBinaryOperator(int tokenType) {
        return switch (tokenType) {
            // Arithmetic operators
            case JavaTokenTypes.PLUS,
                 JavaTokenTypes.MINUS,
                 JavaTokenTypes.STAR,
                 JavaTokenTypes.DIV,
                 JavaTokenTypes.MOD -> true;

            // Comparison operators
            case JavaTokenTypes.EQUAL,
                 JavaTokenTypes.NOT_EQUAL,
                 JavaTokenTypes.LT,
                 JavaTokenTypes.LE,
                 JavaTokenTypes.GT,
                 JavaTokenTypes.GE -> true;

            // Logical operators
            case JavaTokenTypes.LAND,
                 JavaTokenTypes.LOR -> true;

            // Bitwise operators
            case JavaTokenTypes.BAND,
                 JavaTokenTypes.BOR,
                 JavaTokenTypes.BXOR,
                 JavaTokenTypes.SL,
                 JavaTokenTypes.SR,
                 JavaTokenTypes.BSR -> true;

            // Assignment operators
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
     * Gets a human-readable name for a token type.
     *
     * <p>This method provides descriptive names for operator token types
     * to improve error messages and debugging information.</p>
     *
     * @param tokenType The token type to describe
     * @return A descriptive name for the token type
     */
    private String getTokenTypeName(int tokenType) {
        return switch (tokenType) {
            case JavaTokenTypes.PLUS -> "addition operator (+)";
            case JavaTokenTypes.MINUS -> "subtraction operator (-)";
            case JavaTokenTypes.STAR -> "multiplication operator (*)";
            case JavaTokenTypes.DIV -> "division operator (/)";
            case JavaTokenTypes.MOD -> "modulus operator (%)";
            case JavaTokenTypes.EQUAL -> "equality operator (==)";
            case JavaTokenTypes.NOT_EQUAL -> "inequality operator (!=)";
            case JavaTokenTypes.LT -> "less than operator (<)";
            case JavaTokenTypes.LE -> "less than or equal operator (<=)";
            case JavaTokenTypes.GT -> "greater than operator (>)";
            case JavaTokenTypes.GE -> "greater than or equal operator (>=)";
            case JavaTokenTypes.LAND -> "logical and operator (&&)";
            case JavaTokenTypes.LOR -> "logical or operator (||)";
            case JavaTokenTypes.BAND -> "bitwise and operator (&)";
            case JavaTokenTypes.BOR -> "bitwise or operator (|)";
            case JavaTokenTypes.BXOR -> "bitwise xor operator (^)";
            case JavaTokenTypes.SL -> "left shift operator (<<)";
            case JavaTokenTypes.SR -> "right shift operator (>>)";
            case JavaTokenTypes.BSR -> "unsigned right shift operator (>>>)";
            case JavaTokenTypes.ASSIGN -> "assignment operator (=)";
            case JavaTokenTypes.PLUS_ASSIGN -> "plus assignment operator (+=)";
            case JavaTokenTypes.MINUS_ASSIGN -> "minus assignment operator (-=)";
            case JavaTokenTypes.STAR_ASSIGN -> "multiplication assignment operator (*=)";
            case JavaTokenTypes.DIV_ASSIGN -> "division assignment operator (/=)";
            case JavaTokenTypes.MOD_ASSIGN -> "modulus assignment operator (%=)";
            default -> "token type " + tokenType;
        };
    }

    /**
     * Gets the operator that this parselet handles for the given token type.
     * This method is used for validation and testing during the foundation phase.
     *
     * @param tokenType The token type to get information for
     * @return A description of the operator this parselet handles
     */
    public String getHandledOperator(int tokenType) {
        if (isBinaryOperator(tokenType)) {
            return getTokenTypeName(tokenType);
        }
        return "unsupported operator type";
    }

    /**
     * Returns whether this operator parselet is right-associative.
     *
     * @return true if right-associative, false if left-associative
     */
    public boolean isRightAssociative() {
        return rightAssociative;
    }

    @Override
    public String toString() {
        return String.format("BinaryOperatorParselet(precedence=%s, rightAssociative=%s)",
                           precedence, rightAssociative);
    }
}
