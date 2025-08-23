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

/**
 * Interface for parselets that handle prefix expressions in the Pratt parser.
 *
 * <p>Prefix parselets are responsible for parsing constructs that appear at the
 * beginning of expressions. This includes:</p>
 * <ul>
 *   <li>Literals (numbers, strings, booleans, null)</li>
 *   <li>Identifiers (variable names, type names)</li>
 *   <li>Unary operators (-, +, !, ++, --)</li>
 *   <li>Grouping constructs (parentheses)</li>
 *   <li>Lambda expressions</li>
 *   <li>Object creation expressions (new)</li>
 *   <li>Type casts</li>
 * </ul>
 *
 * <h2>Implementation Guidelines</h2>
 * <p>When implementing a prefix parselet:</p>
 * <ol>
 *   <li>The parselet receives the token that triggered it</li>
 *   <li>It should consume any additional tokens needed for its construct</li>
 *   <li>It may recursively call the parser for sub-expressions</li>
 *   <li>It should create and return an appropriate AST node</li>
 *   <li>Error handling should be done through the parser's error methods</li>
 * </ol>
 *
 * <h2>Example Implementation</h2>
 * <p>A simple literal parselet might look like:</p>
 * <pre>{@code
 * public class LiteralParselet implements PrefixParselet {
 *     @Override
 *     public ParsedNode parse(KotlinPrattParser parser, LocatableToken token) {
 *         return new LiteralNode(token);
 *     }
 * }
 * }</pre>
 *
 * <p>A unary operator parselet that needs to parse its operand:</p>
 * <pre>{@code
 * public class UnaryOperatorParselet implements PrefixParselet {
 *     private final int precedence;
 *
 *     @Override
 *     public ParsedNode parse(KotlinPrattParser parser, LocatableToken token) {
 *         // Parse the operand with appropriate precedence
 *         ParsedNode operand = parser.parseExpression(precedence);
 *         return new UnaryExpressionNode(token, operand);
 *     }
 * }
 * }</pre>
 *
 * @see InfixParselet
 * @see Parselet
 * @see KotlinPrattParser
 *
 * @author BlueJ Development Team
 * @since BlueJ 5.4.0
 */
public non-sealed interface PrefixParselet extends Parselet {

    /**
     * Parses a prefix expression starting with the given token.
     *
     * <p>This method is called when the parser encounters a token that has this
     * parselet registered as its prefix handler. The parselet should parse the
     * complete construct and return an appropriate AST node.</p>
     *
     * <p>The token parameter is the token that triggered this parselet. For example,
     * if this is a parselet for the unary minus operator, the token would be the
     * minus sign. The parselet is responsible for consuming any additional tokens
     * needed to complete the construct.</p>
     *
     * <h3>Error Handling</h3>
     * <p>If the parselet encounters a syntax error, it should:</p>
     * <ol>
     *   <li>Report the error using {@code parser.error()}</li>
     *   <li>Attempt recovery if {@link #supportsErrorRecovery()} returns true</li>
     *   <li>Return null or a partial AST node as appropriate</li>
     * </ol>
     *
     * <h3>Recursive Parsing</h3>
     * <p>Many prefix parselets need to recursively parse sub-expressions. Use
     * {@link KotlinPrattParser#parseExpression(int)} with an appropriate precedence
     * level. For example:</p>
     * <ul>
     *   <li>Unary operators typically use high precedence for their operand</li>
     *   <li>Parentheses parse with precedence 0 (parse complete expression)</li>
     *   <li>Cast expressions parse their operand with cast precedence</li>
     * </ul>
     *
     * @param parser The parser instance providing access to tokens and parsing methods
     * @param token The token that triggered this parselet
     * @return The parsed AST node, or null if parsing failed
     * @throws NullPointerException if parser or token is null
     */
    ParsedNode parse(KotlinPrattParser parser, LocatableToken token);

    /**
     * Checks if this parselet can handle the given token in the current context.
     *
     * <p>This method allows for context-sensitive parsing where the same token
     * might be handled differently based on the parser's state. Most parselets
     * can use the default implementation which always returns true.</p>
     *
     * <p>Example use cases for overriding this method:</p>
     * <ul>
     *   <li>Keywords that are only valid in certain contexts</li>
     *   <li>Operators that have different meanings in different positions</li>
     *   <li>Context-sensitive language constructs</li>
     * </ul>
     *
     * @param parser The parser instance for checking context
     * @param token The token to check
     * @return true if this parselet can handle the token, false otherwise
     */
    default boolean canHandle(KotlinPrattParser parser, LocatableToken token) {
        return true;
    }

    /**
     * Gets the expected token types that can follow this prefix construct.
     *
     * <p>This information can be used for error recovery and code completion.
     * For example, after a unary operator, we expect an expression, while after
     * an opening parenthesis, we expect an expression or a closing parenthesis.</p>
     *
     * @return An array of expected token types, or null if any token is acceptable
     */
    default int[] getExpectedFollowTokens() {
        return null;
    }
}
