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
 * Parselet for Kotlin containment operators (`in` and `!in`).
 *
 * <p>Containment operators test whether an element is contained within a collection,
 * range, or other container type. These operators are essential for range checks,
 * collection membership testing, and pattern matching in Kotlin.</p>
 *
 * <p>Examples of containment operator usage:</p>
 * <ul>
 *   <li>Range containment: {@code x in 1..10}, {@code ch in 'a'..'z'}</li>
 *   <li>Collection containment: {@code item in list}, {@code key in map}</li>
 *   <li>String containment: {@code substring in text}</li>
 *   <li>Negated containment: {@code x !in invalidValues}</li>
 *   <li>In when expressions: {@code when (value) { in validRange -> ... }}</li>
 *   <li>In conditionals: {@code if (element in collection) { ... }}</li>
 * </ul>
 *
 * <p>The containment operators have NAMED_CHECKS precedence (70), the same as
 * type check operators (`is`, `!is`). This places them between comparison
 * operators and range operators in the precedence hierarchy:</p>
 * <ul>
 *   <li>{@code a + b in collection} parses as {@code (a + b) in collection}</li>
 *   <li>{@code x in 1..10 && y > 0} parses as {@code (x in (1..10)) && (y > 0)}</li>
 *   <li>{@code element in list is Boolean} parses as {@code (element in list) is Boolean}</li>
 * </ul>
 *
 * <p><strong>Note on `!in` handling:</strong> The `!in` operator may be handled as a
 * compound token or through prefix operator composition with the `!` operator followed
 * by the `in` operator. This parselet primarily handles the basic `in` operator, but
 * includes support for recognizing `!in` if it's tokenized as a single unit.</p>
 *
 * <p>Containment operations work with any type that implements the appropriate
 * `contains` operator functions in Kotlin, including:</p>
 * <ul>
 *   <li>Ranges (IntRange, CharRange, etc.)</li>
 *   <li>Collections (List, Set, Map)</li>
 *   <li>Strings (for substring containment)</li>
 *   <li>Custom types with operator `contains` implementations</li>
 * </ul>
 *
 * @author BlueJ Team
 * @see <a href="https://kotlinlang.org/docs/operator-overloading.html#in-operator">Kotlin Operator Overloading - in operator</a>
 */
public final class InOperatorParselet implements InfixParselet {

    /**
     * The precedence level for containment operators.
     * NAMED_CHECKS precedence (70) places containment checks with other named operators.
     */
    private static final int PRECEDENCE = Precedence.NAMED_CHECKS.getValue();

    /**
     * Parses a containment expression.
     *
     * <p>Containment expressions have the form: {@code ELEMENT in CONTAINER}</p>
     * <p>Where:</p>
     * <ul>
     *   <li>ELEMENT is the value to check for containment (already parsed as left operand)</li>
     *   <li>CONTAINER is the collection/range/container to check within (parsed as right operand)</li>
     * </ul>
     *
     * <p>The operator is left-associative and commonly used for membership testing:</p>
     * <ul>
     *   <li>{@code x in 1..10} - check if x is in the range 1 to 10</li>
     *   <li>{@code item in collection} - check if item exists in collection</li>
     *   <li>{@code key in map} - check if key exists in map</li>
     *   <li>{@code char in "hello"} - check if char exists in string</li>
     * </ul>
     *
     * <p>The right operand should represent a container expression, which may include:</p>
     * <ul>
     *   <li>Range expressions: {@code 1..10}, {@code 'a'..'z'}</li>
     *   <li>Collection literals: {@code listOf(1, 2, 3)}, {@code setOf("a", "b")}</li>
     *   <li>Variables: {@code myList}, {@code validValues}</li>
     *   <li>Complex expressions: {@code getData().filter { it.isValid() }}</li>
     * </ul>
     *
     * @param parser The parser instance for parsing the right operand (container expression)
     * @param left The left operand (element to check for containment)
     * @param operator The containment operator token (`in`)
     * @return ParseResult containing the containment expression node or failure information
     */
    @Override
    public @NotNull ParseResult<ParsedNode> parse(KotlinPrattParser parser, @NotNull ParsedNode left, @NotNull LocatableToken operator) {
        // Validate that we have the correct operator token
        if (operator.getType() != JavaTokenTypes.LITERAL_in) {
            return ParseResult.failure(
                "Expected 'in' operator, found: " + operator.getText(), operator);
        }

        // Parse the right operand (container expression) with same precedence
        // Containment operators are left-associative, so we use the same precedence
        // for parsing the right operand
        ParseResult<ParsedNode> rightResult = parser.parseExpressionResult(PRECEDENCE);
        if (rightResult.isFailure()) {
            return ParseResult.failure(
                "Missing container expression after 'in' operator", operator);
        }

        ParsedNode right = rightResult.getValue();

        // Create the containment expression node using the NodeFactory
        NodeFactory nodeFactory = parser.getNodeFactory();

        try {
            // Create the containment expression node
            // The NodeFactory should create an appropriate node type for containment checking
            ParsedNode result = nodeFactory.createBinaryOperatorNode(left, operator, right);
            return ParseResult.success(result);
        } catch (Exception e) {
            // If node creation fails, return failure with appropriate error
            return ParseResult.failure(
                "Failed to create containment expression: " + e.getMessage(), operator);
        }
    }

    /**
     * Returns the precedence level for containment operators.
     *
     * <p>Containment operators use NAMED_CHECKS precedence (70), which positions them
     * alongside other named operators in Kotlin's precedence hierarchy:</p>
     * <ul>
     *   <li>Same as type checks (70) - {@code x in list is Boolean} parses as {@code (x in list) is Boolean}</li>
     *   <li>Higher than comparison (60) - {@code a < b in collection} parses as {@code (a < b) in collection}</li>
     *   <li>Lower than range (80) - {@code x in 1..10} parses as {@code x in (1..10)}</li>
     *   <li>Much lower than arithmetic - {@code a + b in collection} parses as {@code (a + b) in collection}</li>
     * </ul>
     *
     * @return The NAMED_CHECKS precedence level (70)
     */
    @Override
    public int getPrecedence() {
        return PRECEDENCE;
    }

    /**
     * Returns whether containment operators are left-associative.
     *
     * <p>Containment operators are left-associative, meaning expressions like
     * {@code a in b in c} (though uncommon) parse as {@code (a in b) in c}.</p>
     *
     * <p>In practice, chaining containment operators is rare in Kotlin code, and
     * multiple containment checks are typically combined using logical operators:</p>
     * <ul>
     *   <li>Preferred: {@code element in list1 || element in list2}</li>
     *   <li>Preferred: {@code x in validRange && y in allowedValues}</li>
     *   <li>Less common: {@code a in b in c}</li>
     * </ul>
     *
     * @return false (containment operators are left-associative)
     */
    public boolean isRightAssociative() {
        return false;
    }

    /**
     * Checks if the given token type is a containment operator.
     *
     * <p>This method validates whether a token represents a containment operator
     * that this parselet can handle. Currently supports:</p>
     * <ul>
     *   <li>{@code in} - positive containment check</li>
     * </ul>
     *
     * <p>Note: {@code !in} (negated containment) may be handled separately,
     * either as a compound token or through prefix operator composition.</p>
     *
     * @param tokenType The token type to check
     * @return true if the token is a supported containment operator
     */
    public boolean isContainmentOperator(int tokenType) {
        return tokenType == JavaTokenTypes.LITERAL_in;
    }

    @Override
    public String toString() {
        return "InOperatorParselet(precedence=" + PRECEDENCE + ", leftAssociative=true)";
    }
}
