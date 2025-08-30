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
 * Parselet for Kotlin type cast operators (`as` and `as?`).
 *
 * <p>Type cast operators perform explicit type conversions in Kotlin, allowing you to
 * convert expressions from one type to another. These operators are essential for
 * working with type hierarchies and handling type-safe conversions.</p>
 *
 * <p>Examples of type cast operator usage:</p>
 * <ul>
 *   <li>Unsafe cast: {@code obj as String} - throws ClassCastException if cast fails</li>
 *   <li>Safe cast: {@code obj as? String} - returns null if cast fails</li>
 *   <li>With nullable types: {@code value as? Int?}</li>
 *   <li>With generic types: {@code list as List<String>}</li>
 *   <li>Chained operations: {@code (obj as String).uppercase()}</li>
 *   <li>Safe chaining: {@code (obj as? String)?.uppercase()}</li>
 * </ul>
 *
 * <p>The type cast operators have TYPE_CAST precedence (120), making them bind tighter
 * than most other operators but looser than postfix operations:</p>
 * <ul>
 *   <li>{@code obj.prop as String} parses as {@code (obj.prop) as String}</li>
 *   <li>{@code obj as String + "suffix"} parses as {@code (obj as String) + "suffix"}</li>
 *   <li>{@code obj as? String?.length} parses as {@code (obj as? String)?.length}</li>
 * </ul>
 *
 * <p><strong>Operator Differences:</strong></p>
 * <ul>
 *   <li>{@code as} - Unsafe cast: throws {@code ClassCastException} if the cast is invalid</li>
 *   <li>{@code as?} - Safe cast: returns {@code null} if the cast is invalid</li>
 * </ul>
 *
 * <p>The safe cast operator ({@code as?}) is particularly useful when combined with
 * null-safe operations and Elvis operator:</p>
 * <ul>
 *   <li>{@code (obj as? String)?.length ?: 0}</li>
 *   <li>{@code val result = input as? Int ?: return}</li>
 * </ul>
 *
 * @author BlueJ Team
 * @see <a href="https://kotlinlang.org/docs/typecasts.html#unsafe-cast-operator">Kotlin Type Casts - as and as? operators</a>
 */
public final class TypeCastParselet implements InfixParselet {

    /**
     * The precedence level for type cast operators.
     * TYPE_CAST precedence (120) places type casts higher than most operators
     * but lower than postfix operations.
     */
    private static final int PRECEDENCE = Precedence.TYPE_CAST.getValue();

    /**
     * Parses a type cast expression.
     *
     * <p>Type cast expressions have the form: {@code EXPRESSION as TYPE}</p>
     * <p>Where:</p>
     * <ul>
     *   <li>EXPRESSION is the value to cast (already parsed as left operand)</li>
     *   <li>TYPE is the target type for the cast (parsed as right operand)</li>
     * </ul>
     *
     * <p>The operator is left-associative, allowing for chained casts (though not common):</p>
     * <ul>
     *   <li>{@code obj as Any as String} parses as {@code (obj as Any) as String}</li>
     *   <li>{@code value as? Int as? Number} parses as {@code (value as? Int) as? Number}</li>
     * </ul>
     *
     * <p>The right operand represents a type expression, which may include:</p>
     * <ul>
     *   <li>Simple types: {@code String}, {@code Int}, {@code Boolean}</li>
     *   <li>Nullable types: {@code String?}, {@code Int?}</li>
     *   <li>Generic types: {@code List<String>}, {@code Map<String, Int>}</li>
     *   <li>Qualified types: {@code kotlin.collections.List}</li>
     *   <li>Function types: {@code (Int) -> String}, {@code () -> Unit}</li>
     * </ul>
     *
     * <p><strong>Runtime Behavior:</strong></p>
     * <ul>
     *   <li>{@code as} - Performs unsafe cast, throws exception on failure</li>
     *   <li>{@code as?} - Performs safe cast, returns null on failure</li>
     * </ul>
     *
     * @param parser The parser instance for parsing the right operand (type expression)
     * @param left The left operand (expression to cast)
     * @param operator The type cast operator token ({@code as} or {@code as?})
     * @return ParseResult containing the type cast expression node or failure information
     */
    @Override
    public @NotNull ParseResult<ParsedNode> parse(KotlinPrattParser parser, @NotNull ParsedNode left, @NotNull LocatableToken operator) {
        // Validate that we have a supported cast operator token
        if (!isTypeCastOperator(operator.getType())) {
            return ParseResult.failure(
                "Expected type cast operator ('as'), found: " + operator.getText(), operator);
        }

        // Parse the right operand (type expression) with same precedence
        // Type cast operators are left-associative, so we use the same precedence
        // for parsing the right operand to allow left-to-right evaluation
        ParseResult<ParsedNode> rightResult = parser.parseExpressionResult(PRECEDENCE);
        if (rightResult.isFailure()) {
            return ParseResult.failure(
                "Missing target type after '" + operator.getText() + "' operator", operator);
        }

        ParsedNode right = rightResult.getValue();

        // Create the type cast expression node using the NodeFactory
        NodeFactory nodeFactory = parser.getNodeFactory();

        try {
            // Create the type cast expression node
            // The NodeFactory should create an appropriate node type for type casting
            // The node should preserve whether this is a safe cast (as?) or unsafe cast (as)
            ParsedNode result = nodeFactory.createBinaryOperatorNode(left, operator, right);
            return ParseResult.success(result);
        } catch (Exception e) {
            // If node creation fails, return failure with appropriate error
            return ParseResult.failure(
                "Failed to create type cast expression: " + e.getMessage(), operator);
        }
    }

    /**
     * Returns the precedence level for type cast operators.
     *
     * <p>Type cast operators use TYPE_CAST precedence (120), which positions them
     * appropriately in Kotlin's precedence hierarchy:</p>
     * <ul>
     *   <li>Higher than multiplicative operators (110) - {@code a * b as Int} parses as {@code (a * b) as Int}</li>
     *   <li>Lower than postfix operators (140) - {@code obj.prop as String} parses as {@code (obj.prop) as String}</li>
     *   <li>Higher than most binary operators - allows natural cast grouping</li>
     * </ul>
     *
     * <p>This precedence enables natural usage patterns:</p>
     * <ul>
     *   <li>{@code value + offset as Double} → {@code (value + offset) as Double}</li>
     *   <li>{@code obj.method() as String} → {@code (obj.method()) as String}</li>
     * </ul>
     *
     * @return The TYPE_CAST precedence level (120)
     */
    @Override
    public int getPrecedence() {
        return PRECEDENCE;
    }

    /**
     * Returns whether type cast operators are left-associative.
     *
     * <p>Type cast operators are left-associative, meaning expressions with
     * multiple casts are evaluated from left to right:</p>
     * <ul>
     *   <li>{@code obj as Any as String} parses as {@code (obj as Any) as String}</li>
     *   <li>{@code value as? Int as? Long} parses as {@code (value as? Int) as? Long}</li>
     * </ul>
     *
     * <p>While chained casts are syntactically valid, they are not common in
     * idiomatic Kotlin code. More typically, casts are combined with other operations:</p>
     * <ul>
     *   <li>{@code (obj as String).uppercase()}</li>
     *   <li>{@code (value as? Int)?.let { ... }}</li>
     * </ul>
     *
     * @return false (type cast operators are left-associative)
     */
    public boolean isRightAssociative() {
        return false;
    }

    /**
     * Checks if the given token type is a type cast operator.
     *
     * <p>This method validates whether a token represents a type cast operator
     * that this parselet can handle. Currently supports:</p>
     * <ul>
     *   <li>{@code as} - unsafe cast (throws on failure)</li>
     * </ul>
     *
     * <p><strong>Note on {@code as?} handling:</strong> The safe cast operator may be
     * tokenized as a compound token or handled through a separate mechanism. If it's
     * tokenized as separate {@code as} and {@code ?} tokens, additional parsing
     * logic may be needed to detect and handle the safe cast pattern.</p>
     *
     * @param tokenType The token type to check
     * @return true if the token is a supported type cast operator
     */
    public boolean isTypeCastOperator(int tokenType) {
        return tokenType == JavaTokenTypes.LITERAL_as;
        // TODO: Add support for LITERAL_as_safe or similar token if/when implemented
        // || tokenType == JavaTokenTypes.LITERAL_as_safe;
    }

    /**
     * Determines if a cast operation is a safe cast based on the operator token.
     *
     * <p>This method helps distinguish between unsafe casts ({@code as}) and
     * safe casts ({@code as?}) for proper semantic analysis and code generation.</p>
     *
     * <p>Currently, this implementation assumes all {@code LITERAL_as} tokens
     * represent unsafe casts. If safe cast support is added with a separate token
     * type, this method should be updated accordingly.</p>
     *
     * @param operator The cast operator token
     * @return true if this is a safe cast ({@code as?}), false for unsafe cast ({@code as})
     */
    public boolean isSafeCast(LocatableToken operator) {
        // TODO: Implement safe cast detection when LITERAL_as_safe token is available
        // return operator.getType() == JavaTokenTypes.LITERAL_as_safe;

        // For now, assume all casts are unsafe (as) casts
        // Safe cast (as?) detection may require compound token handling
        return false;
    }

    /**
     * Gets the cast type description for error messages and debugging.
     *
     * @param operator The cast operator token
     * @return A descriptive string for the cast type
     */
    public String getCastTypeDescription(LocatableToken operator) {
        if (isSafeCast(operator)) {
            return "safe cast (as?)";
        } else {
            return "unsafe cast (as)";
        }
    }

    @Override
    public String toString() {
        return "TypeCastParselet(precedence=" + PRECEDENCE + ", leftAssociative=true)";
    }
}
