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
 * Parselet for the Elvis operator (?:) in Kotlin.
 *
 * <p>The Elvis operator is Kotlin's null-coalescing operator that returns the right-hand
 * operand when the left-hand operand is null, and the left-hand operand otherwise.</p>
 *
 * <p>Examples of Elvis operator usage:</p>
 * <ul>
 *   <li>Basic usage: {@code val name = person.name ?: "Unknown"}</li>
 *   <li>Chained usage: {@code val result = a ?: b ?: c ?: "default"}</li>
 *   <li>With method calls: {@code val length = text?.length ?: 0}</li>
 *   <li>Complex expressions: {@code val value = computeValue() ?: getDefault()}</li>
 * </ul>
 *
 * <p>The Elvis operator has low precedence (ELVIS level = 20) and is right-associative,
 * meaning {@code a ?: b ?: c} is parsed as {@code a ?: (b ?: c)}.</p>
 *
 * <p>This right-associativity allows for natural chaining of default values,
 * where each subsequent operand serves as a fallback for all previous ones.</p>
 *
 * @author BlueJ Team
 * @see <a href="https://kotlinlang.org/docs/null-safety.html#elvis-operator">Kotlin Null Safety - Elvis Operator</a>
 */
public final class ElvisOperatorParselet implements InfixParselet {

    /**
     * The precedence level for the Elvis operator.
     * Elvis has low precedence (20) to allow most expressions to bind more tightly.
     */
    private static final int PRECEDENCE = Precedence.ELVIS.getValue();

    /**
     * Parses an Elvis operator expression.
     *
     * <p>The Elvis operator has the form: {@code LEFT ?: RIGHT}</p>
     * <p>Where:</p>
     * <ul>
     *   <li>LEFT is the expression to check for null (already parsed)</li>
     *   <li>RIGHT is the fallback expression to use if LEFT is null</li>
     * </ul>
     *
     * <p>The operator is right-associative, so chained expressions like
     * {@code a ?: b ?: c} are parsed as {@code a ?: (b ?: c)}.</p>
     *
     * @param parser The parser instance for parsing the right operand
     * @param left The left operand (already parsed expression to check for null)
     * @param operator The Elvis operator token (?:)
     * @return ParseResult containing the Elvis expression node or failure information
     */
    @Override
    public @NotNull ParseResult<ParsedNode> parse(KotlinPrattParser parser, @NotNull ParsedNode left, @NotNull LocatableToken operator) {
        // Validate that we have the correct operator token
        if (operator.getType() != JavaTokenTypes.ELVIS) {
            return ParseResult.failure(
                "Expected Elvis operator (?:), found: " + operator.getText(), operator);
        }

        // For right-associative operators, use precedence - 1 to allow
        // same-precedence operators to bind to the right
        // This makes "a ?: b ?: c" parse as "a ?: (b ?: c)"
        int rightPrecedence = PRECEDENCE - 1;

        // Parse the right operand (the fallback expression)
        ParseResult<ParsedNode> rightResult = parser.parseExpressionResult(rightPrecedence);
        if (rightResult.isFailure()) {
            return ParseResult.failure(
                "Missing right operand for Elvis operator (?:)", operator);
        }

        ParsedNode right = rightResult.getValue();

        // Create the Elvis operator node using the NodeFactory
        NodeFactory nodeFactory = parser.getNodeFactory();

        try {
            // Create the Elvis expression node
            ParsedNode result = nodeFactory.createBinaryOperatorNode(left, operator, right);
            return ParseResult.success(result);
        } catch (Exception e) {
            // If node creation fails, return failure with appropriate error
            return ParseResult.failure(
                "Failed to create Elvis operator node: " + e.getMessage(), operator);
        }
    }

    /**
     * Returns the precedence level for the Elvis operator.
     *
     * <p>The Elvis operator has low precedence (20) to ensure that most other
     * expressions bind more tightly. This allows expressions like:</p>
     * <ul>
     *   <li>{@code a + b ?: c} to parse as {@code (a + b) ?: c}</li>
     *   <li>{@code obj.prop ?: default} to parse as {@code (obj.prop) ?: default}</li>
     * </ul>
     *
     * @return The ELVIS precedence level (20)
     */
    @Override
    public int getPrecedence() {
        return PRECEDENCE;
    }

    /**
     * Returns whether the Elvis operator is right-associative.
     *
     * @return true (Elvis operator is right-associative)
     */
    public boolean isRightAssociative() {
        return true;
    }

    @Override
    public String toString() {
        return "ElvisOperatorParselet(precedence=" + PRECEDENCE + ", rightAssociative=true)";
    }
}
