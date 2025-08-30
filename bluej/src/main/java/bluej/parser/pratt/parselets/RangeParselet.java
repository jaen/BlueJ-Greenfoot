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
 * Parselet for Kotlin range operator (..).
 *
 * <p>The range operator creates inclusive ranges in Kotlin, representing a sequence of values
 * from a start point to an end point. Range expressions are fundamental for iteration,
 * collection operations, and range-based checks in Kotlin.</p>
 *
 * <p>Examples of range operator usage:</p>
 * <ul>
 *   <li>Integer ranges: {@code 1..10}, {@code -5..5}</li>
 *   <li>Character ranges: {@code 'a'..'z'}, {@code 'A'..'Z'}</li>
 *   <li>Complex expressions: {@code start..end}, {@code (min + 1)..(max - 1)}</li>
 *   <li>With variables: {@code first..last}, {@code begin..finish}</li>
 *   <li>In for loops: {@code for (i in 1..10) { ... }}</li>
 *   <li>In when expressions: {@code when (x) { in 1..10 -> ... }</li>
 * </ul>
 *
 * <p>The range operator has RANGE precedence (80), placing it between named checks
 * and infix functions in the precedence hierarchy. This allows natural expression
 * composition:</p>
 * <ul>
 *   <li>{@code a + b..c + d} parses as {@code (a + b)..(c + d)}</li>
 *   <li>{@code x in 1..10} parses as {@code x in (1..10)}</li>
 *   <li>{@code start..end is IntRange} parses as {@code (start..end) is IntRange}</li>
 * </ul>
 *
 * <p>Range expressions are left-associative but chaining ranges is not typical
 * in Kotlin code. Most range usage involves single range expressions or
 * ranges as operands to other operators like {@code in}.</p>
 *
 * <p><strong>Range Types:</strong></p>
 * <ul>
 *   <li><strong>Inclusive ranges (..)</strong>: Include both start and end values</li>
 *   <li><strong>Exclusive ranges</strong>: May be supported through other operators like {@code until} (handled by other parselets)</li>
 * </ul>
 *
 * <p>The range operator works with any type that implements {@code Comparable} and
 * has appropriate range operators defined in the Kotlin standard library.</p>
 *
 * @author BlueJ Team
 * @see <a href="https://kotlinlang.org/docs/ranges.html">Kotlin Ranges and Progressions</a>
 */
public final class RangeParselet implements InfixParselet {

    /**
     * The precedence level for the range operator.
     * RANGE precedence (80) places range operators between named checks and infix functions.
     */
    private static final int PRECEDENCE = Precedence.RANGE.getValue();

    /**
     * Parses a range expression.
     *
     * <p>Range expressions have the form: {@code START .. END}</p>
     * <p>Where:</p>
     * <ul>
     *   <li>START is the beginning value of the range (already parsed as left operand)</li>
     *   <li>END is the ending value of the range (parsed as right operand)</li>
     * </ul>
     *
     * <p>The operator is left-associative, though range chaining is uncommon.
     * Both operands should be expressions that can be used to create a range:</p>
     * <ul>
     *   <li>Integer literals: {@code 1..10}</li>
     *   <li>Character literals: {@code 'a'..'z'}</li>
     *   <li>Variables: {@code start..end}</li>
     *   <li>Complex expressions: {@code (min + offset)..(max - offset)}</li>
     * </ul>
     *
     * <p>The resulting range is inclusive, meaning both start and end values
     * are included in the range. For example, {@code 1..3} includes 1, 2, and 3.</p>
     *
     * @param parser The parser instance for parsing the right operand (end value)
     * @param left The left operand (start value of the range)
     * @param operator The range operator token (..)
     * @return ParseResult containing the range expression node or failure information
     */
    @Override
    public @NotNull ParseResult<ParsedNode> parse(KotlinPrattParser parser, @NotNull ParsedNode left, @NotNull LocatableToken operator) {
        // Validate that we have the correct operator token
        if (operator.getType() != JavaTokenTypes.RANGE) {
            return ParseResult.failure(
                "Expected range operator (..), found: " + operator.getText(), operator);
        }

        // Parse the right operand (end value) with same precedence
        // Range operators are left-associative, so we use the same precedence
        // for parsing the right operand
        ParseResult<ParsedNode> rightResult = parser.parseExpressionResult(PRECEDENCE);
        if (rightResult.isFailure()) {
            return ParseResult.failure(
                "Missing end value for range expression", operator);
        }

        ParsedNode right = rightResult.getValue();

        // Create the range expression node using the NodeFactory
        NodeFactory nodeFactory = parser.getNodeFactory();

        try {
            // Create the range expression node
            // The NodeFactory should create an appropriate node type for range expressions
            ParsedNode result = nodeFactory.createBinaryOperatorNode(left, operator, right);
            return ParseResult.success(result);
        } catch (Exception e) {
            // If node creation fails, return failure with appropriate error
            return ParseResult.failure(
                "Failed to create range expression: " + e.getMessage(), operator);
        }
    }

    /**
     * Returns the precedence level for the range operator.
     *
     * <p>The range operator uses RANGE precedence (80), which positions it
     * appropriately in Kotlin's precedence hierarchy:</p>
     * <ul>
     *   <li>Higher than named checks (70) - {@code x is Int..10} parses as {@code x is (Int..10)} (though not typical)</li>
     *   <li>Lower than infix functions (90) - {@code start add 1..end sub 1} parses as {@code (start add 1)..(end sub 1)}</li>
     *   <li>Much lower than arithmetic - {@code a + b..c * d} parses as {@code (a + b)..(c * d)}</li>
     * </ul>
     *
     * @return The RANGE precedence level (80)
     */
    @Override
    public int getPrecedence() {
        return PRECEDENCE;
    }

    /**
     * Returns whether range operators are left-associative.
     *
     * <p>Range operators are left-associative, meaning expressions like
     * {@code a..b..c} (though uncommon) parse as {@code (a..b)..c}.</p>
     *
     * <p>In practice, chaining ranges is not common in Kotlin code, and
     * ranges are typically used as single expressions or as operands to
     * other operators like {@code in}:</p>
     * <ul>
     *   <li>Typical: {@code x in 1..10}</li>
     *   <li>Typical: {@code for (i in start..end) { ... }}</li>
     *   <li>Uncommon: {@code a..b..c}</li>
     * </ul>
     *
     * @return false (range operators are left-associative)
     */
    public boolean isRightAssociative() {
        return false;
    }

    /**
     * Checks if the given token type is a range operator.
     *
     * <p>This method validates whether a token represents a range operator
     * that this parselet can handle. Currently supports:</p>
     * <ul>
     *   <li>{@code ..} - inclusive range operator</li>
     * </ul>
     *
     * <p>Other range-like operators such as {@code until} (exclusive range)
     * may be handled by separate parselets or as infix functions.</p>
     *
     * @param tokenType The token type to check
     * @return true if the token is a supported range operator
     */
    public boolean isRangeOperator(int tokenType) {
        return tokenType == JavaTokenTypes.RANGE;
    }

    @Override
    public String toString() {
        return "RangeParselet(precedence=" + PRECEDENCE + ", leftAssociative=true)";
    }
}
