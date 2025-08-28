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

import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.ParsedNode;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for parselets that handle infix expressions in the Pratt parser.
 *
 * <p>Infix parselets are responsible for parsing constructs that appear between
 * expressions, binding a left-hand expression with a right-hand expression or
 * performing operations on the left-hand expression. This includes:</p>
 * <ul>
 *   <li>Binary operators (+, -, *, /, %, &&, ||, etc.)</li>
 *   <li>Comparison operators (==, !=, <, >, <=, >=)</li>
 *   <li>Assignment operators (=, +=, -=, etc.)</li>
 *   <li>Member access (. operator)</li>
 *   <li>Method calls and indexing (parentheses and brackets)</li>
 *   <li>Ternary conditional operator (?:)</li>
 *   <li>Type parameters and arguments</li>
 *   <li>Elvis operator (?:) and null-safe calls (?.)</li>
 * </ul>
 *
 * <h2>Precedence and Associativity</h2>
 * <p>Each infix parselet has an associated precedence level that determines
 * the order of operations. Higher precedence values bind more tightly. For
 * example, multiplication has higher precedence than addition.</p>
 *
 * <p>Associativity determines how operators of the same precedence are grouped:</p>
 * <ul>
 *   <li>Left-associative: a + b + c = (a + b) + c</li>
 *   <li>Right-associative: a = b = c = a = (b = c)</li>
 * </ul>
 *
 * <h2>Implementation Guidelines</h2>
 * <p>When implementing an infix parselet:</p>
 * <ol>
 *   <li>The parselet receives the already-parsed left expression</li>
 *   <li>It receives the token that triggered it (the operator)</li>
 *   <li>It should parse the right-hand expression with appropriate precedence</li>
 *   <li>For left-associative operators, use the same precedence</li>
 *   <li>For right-associative operators, use precedence - 1</li>
 *   <li>It should create and return an appropriate AST node</li>
 * </ol>
 *
 * <h2>Example Implementation</h2>
 * <p>A binary operator parselet:</p>
 * <pre>{@code
 * public class BinaryOperatorParselet implements InfixParselet {
 *     private final int precedence;
 *     private final boolean rightAssociative;
 *
 *     @Override
 *     public int getPrecedence() {
 *         return precedence;
 *     }
 *
 *     @Override
 *     public ParseResult<ParsedNode> parse(KotlinPrattParser parser, ParsedNode left,
 *                           LocatableToken operator) {
 *         // Adjust precedence for associativity
 *         int rightPrec = rightAssociative ? precedence - 1 : precedence;
 *         return parser.parseExpressionResult(rightPrec)
 *             .map(right -> new BinaryExpressionNode(left, operator, right));
 *     }
 * }
 * }</pre>
 *
 * @see PrefixParselet
 * @see Parselet
 * @see KotlinPrattParser
 * @see Precedence
 *
 * @author BlueJ Development Team
 * @since BlueJ 5.4.0
 */
public non-sealed interface InfixParselet extends Parselet {

    /**
     * Gets the precedence level of this infix parselet.
     *
     * <p>Precedence determines the order of operations when parsing expressions.
     * Higher precedence values indicate tighter binding. For example:</p>
     * <ul>
     *   <li>Assignment: 1</li>
     *   <li>Logical OR: 2</li>
     *   <li>Logical AND: 3</li>
     *   <li>Equality: 4</li>
     *   <li>Comparison: 5</li>
     *   <li>Addition/Subtraction: 6</li>
     *   <li>Multiplication/Division: 7</li>
     *   <li>Unary: 8</li>
     *   <li>Postfix/Call: 9</li>
     * </ul>
     *
     * <p>The exact precedence values should align with the language specification
     * being parsed. For Kotlin, refer to the official Kotlin grammar specification.</p>
     *
     * @return The precedence level (higher values bind more tightly)
     * @see Precedence
     */
    int getPrecedence();

    /**
     * Parses an infix expression with the given left-hand expression and operator token.
     *
     * <p>This method is called when the parser has already parsed a left-hand
     * expression and encounters a token that has this parselet registered as its
     * infix handler. The parselet should parse any right-hand expression needed
     * and return a complete AST node representing the full expression.</p>
     *
     * <h3>Precedence Handling</h3>
     * <p>When parsing the right-hand expression, use the appropriate precedence:</p>
     * <ul>
     *   <li>For left-associative operators: use {@code getPrecedence()}</li>
     *   <li>For right-associative operators: use {@code getPrecedence() - 1}</li>
     * </ul>
     *
     * <h3>Error Handling</h3>
     * <p>If the parselet encounters a syntax error, it should:</p>
     * <ol>
     *   <li>Return a ParseResult.failure() with appropriate error information</li>
     *   <li>Attempt recovery if {@link #supportsErrorRecovery()} returns true</li>
     *   <li>Return a ParseResult.partial() with recoverable errors when appropriate</li>
     * </ol>
     *
     * @param parser The parser instance providing access to tokens and parsing methods
     * @param left The already-parsed left-hand expression
     * @param token The operator/infix token that triggered this parselet
     * @return A ParseResult containing the complete parsed expression or accumulated errors
     * @throws NullPointerException if any parameter is null
     */
    @NotNull ParseResult<ParsedNode> parse(KotlinPrattParser parser, @NotNull ParsedNode left, @NotNull LocatableToken token);

    /**
     * Indicates whether this infix operator is right-associative.
     *
     * <p>Most operators are left-associative (a + b + c = (a + b) + c), but some
     * operators like assignment are right-associative (a = b = c = a = (b = c)).</p>
     *
     * <p>This information affects how the parselet should parse its right-hand
     * expression. Right-associative operators should parse with precedence - 1
     * to allow operators of the same precedence to group to the right.</p>
     *
     * @return true if this operator is right-associative, false if left-associative
     */
    default boolean isRightAssociative() {
        return false;
    }



    /**
     * Indicates whether this infix parselet requires a right-hand expression.
     *
     * <p>Most infix operators require a right-hand expression (e.g., binary operators),
     * but some may not (e.g., postfix operators like ++ or --, or nullable type
     * operator ?).</p>
     *
     * @return true if a right-hand expression is required, false otherwise
     */
    default boolean requiresRightExpression() {
        return true;
    }
}
