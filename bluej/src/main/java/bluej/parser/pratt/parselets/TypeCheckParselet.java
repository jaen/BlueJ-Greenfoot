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
import org.jetbrains.annotations.NotNull;

/**
 * Parselet for Kotlin type check operators (`is` and `!is`).
 *
 * <p>Type check operators perform runtime type checking in Kotlin, allowing you to test
 * whether an expression is of a specific type. These operators are essential for safe
 * type handling and smart casts in Kotlin code.</p>
 *
 * <p>Examples of type check operator usage:</p>
 * <ul>
 *   <li>Basic type check: {@code obj is String}</li>
 *   <li>Negated type check: {@code obj !is String}</li>
 *   <li>With nullable types: {@code value is Int?}</li>
 *   <li>With generic types: {@code list is List<String>}</li>
 *   <li>In conditional expressions: {@code if (obj is String) obj.length}</li>
 * </ul>
 *
 * <p>The type check operators have NAMED_CHECKS precedence (70), placing them between
 * comparison operators and range operators in the precedence hierarchy. This allows
 * natural expression composition:</p>
 * <ul>
 *   <li>{@code a + b is Int} parses as {@code (a + b) is Int}</li>
 *   <li>{@code obj is String && obj.isNotEmpty()} parses as {@code (obj is String) && (obj.isNotEmpty())}</li>
 * </ul>
 *
 * <p><strong>Note on `!is` handling:</strong> The `!is` operator is typically handled as a
 * compound operation. In this implementation, the `!is` operator should be tokenized as a
 * single unit or handled through a prefix parselet for logical NOT that recognizes the
 * following `is` token. This parselet primarily handles the basic `is` operator.</p>
 *
 * <p>Type check operations enable Kotlin's smart cast feature, where the compiler
 * automatically casts variables to more specific types within conditional blocks
 * after successful type checks.</p>
 *
 * @author BlueJ Team
 * @see <a href="https://kotlinlang.org/docs/typecasts.html#is-and-is-operators">Kotlin Type Checks - is and !is operators</a>
 */
public final class TypeCheckParselet implements InfixParselet {

    /**
     * The precedence level for type check operators.
     * NAMED_CHECKS precedence (70) places type checks between comparison and range operators.
     */
    private static final int PRECEDENCE = Precedence.NAMED_CHECKS.getValue();

    /**
     * Parses a type check expression.
     *
     * <p>Type check expressions have the form: {@code EXPRESSION is TYPE}</p>
     * <p>Where:</p>
     * <ul>
     *   <li>EXPRESSION is the value to check (already parsed as left operand)</li>
     *   <li>TYPE is the target type to check against (parsed as right operand)</li>
     * </ul>
     *
     * <p>The operator is left-associative and non-chaining in practice, though
     * multiple type checks can be combined with logical operators:</p>
     * <ul>
     *   <li>{@code obj is String && obj is CharSequence} - valid combination</li>
     *   <li>{@code obj is String is Any} - not idiomatic, but parsed left-to-right</li>
     * </ul>
     *
     * <p>The right operand should represent a type expression, which may include:</p>
     * <ul>
     *   <li>Simple types: {@code String}, {@code Int}, {@code Boolean}</li>
     *   <li>Nullable types: {@code String?}, {@code Int?}</li>
     *   <li>Generic types: {@code List<String>}, {@code Map<String, Int>}</li>
     *   <li>Qualified types: {@code kotlin.collections.List}</li>
     * </ul>
     *
     * @param parser The parser instance for parsing the right operand (type expression)
     * @param left The left operand (expression to check)
     * @param operator The type check operator token (`is`)
     * @return ParseResult containing the type check expression node or failure information
     */
    @Override
    public @NotNull ParseResult<ParsedNode> parse(KotlinPrattParser parser, @NotNull ParsedNode left, @NotNull LocatableToken operator) {
        // Validate that we have the correct operator token
        if (operator.getType() != JavaTokenTypes.LITERAL_is) {
            return ParseResult.failure(
                "Expected 'is' operator, found: " + operator.getText(), operator);
        }

        // Parse the right operand (type expression) with same precedence
        // Type check operators are left-associative, so we use the same precedence
        // for parsing the right operand
        ParseResult<ParsedNode> rightResult = parser.parseExpressionResult(PRECEDENCE);
        if (rightResult.isFailure()) {
            return ParseResult.failure(
                "Missing type expression after 'is' operator", operator);
        }

        ParsedNode right = rightResult.getValue();

        // Create the type check expression node using the NodeFactory
        NodeFactory nodeFactory = parser.getNodeFactory();

        try {
            // Create the type check expression node
            // The NodeFactory should create an appropriate node type for type checking
            ParsedNode result = nodeFactory.createBinaryOperatorNode(left, operator, right);
            return ParseResult.success(result);
        } catch (Exception e) {
            // If node creation fails, return failure with appropriate error
            return ParseResult.failure(
                "Failed to create type check expression: " + e.getMessage(), operator);
        }
    }

    /**
     * Returns the precedence level for type check operators.
     *
     * <p>Type check operators use NAMED_CHECKS precedence (70), which positions them
     * appropriately in Kotlin's precedence hierarchy:</p>
     * <ul>
     *   <li>Higher than comparison operators (60) - {@code a < b is Boolean} parses as {@code (a < b) is Boolean}</li>
     *   <li>Lower than range operators (80) - {@code 1..10 is IntRange} parses as {@code (1..10) is IntRange}</li>
     *   <li>Much lower than arithmetic - {@code a + b is Int} parses as {@code (a + b) is Int}</li>
     * </ul>
     *
     * @return The NAMED_CHECKS precedence level (70)
     */
    @Override
    public int getPrecedence() {
        return PRECEDENCE;
    }

    /**
     * Returns whether type check operators are left-associative.
     *
     * <p>Type check operators are left-associative, meaning expressions like
     * {@code a is B is C} (though not idiomatic) parse as {@code (a is B) is C}.</p>
     *
     * <p>In practice, chaining type checks is not common in Kotlin code, and
     * multiple type checks are typically combined using logical operators:</p>
     * <ul>
     *   <li>Preferred: {@code obj is String && obj.isNotEmpty()}</li>
     *   <li>Less common: {@code obj is String is Any} (parsed as {@code (obj is String) is Any})</li>
     * </ul>
     *
     * @return false (type check operators are left-associative)
     */
    public boolean isRightAssociative() {
        return false;
    }

    /**
     * Checks if the given token type is a type check operator.
     *
     * <p>This method validates whether a token represents a type check operator
     * that this parselet can handle. Currently supports:</p>
     * <ul>
     *   <li>{@code is} - positive type check</li>
     * </ul>
     *
     * <p>Note: {@code !is} (negated type check) may be handled separately,
     * either as a compound token or through prefix operator composition.</p>
     *
     * @param tokenType The token type to check
     * @return true if the token is a supported type check operator
     */
    public boolean isTypeCheckOperator(int tokenType) {
        return tokenType == JavaTokenTypes.LITERAL_is;
    }

    @Override
    public String toString() {
        return "TypeCheckParselet(precedence=" + PRECEDENCE + ", leftAssociative=true)";
    }
}
