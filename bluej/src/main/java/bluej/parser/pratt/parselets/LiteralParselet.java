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
import bluej.parser.nodes.ExpressionNode;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.pratt.CallbackIntegrationException;
import bluej.parser.pratt.KotlinPrattParser;
import bluej.parser.pratt.NodeFactory;
import bluej.parser.pratt.ParseResult;
import bluej.parser.pratt.PrefixParselet;
import bluej.parser.pratt.TrackedScope;
import org.jetbrains.annotations.NotNull;

/**
 * A parselet for handling literal values in Kotlin code.
 *
 * <p>This parselet handles all basic literal types including:</p>
 * <ul>
 *   <li>Integer literals (42, 0xFF, 0b1010)</li>
 *   <li>Long literals (42L, 0xFFL)</li>
 *   <li>Float literals (3.14f, 1e6f)</li>
 *   <li>Double literals (3.14, 1e6)</li>
 *   <li>String literals ("hello world")</li>
 *   <li>Character literals ('c', '\n')</li>
 *   <li>Boolean literals (true, false)</li>
 *   <li>Null literal (null)</li>
 * </ul>
 *
 * <p>The parselet creates an {@link ExpressionNode} to represent the literal
 * in the AST. The actual value interpretation is handled separately by the
 * type system and evaluation components.</p>
 *
 * <p>This implementation follows the Kotlin Language Specification for
 * literal syntax and precedence handling.</p>
 *
 * @author BlueJ Team
 * @see <a href="https://kotlinlang.org/spec/syntax-and-grammar.html#literals">Kotlin Language Specification - Literals</a>
 */
public final class LiteralParselet implements PrefixParselet {

    /**
     * Parses a literal token into an AST node using SafeCallbacks integration.
     *
     * <p>This method uses the SafeCallbacks pattern with try-with-resources to ensure
     * proper callback pairing and automatic cleanup. The SafeCallbacks system handles
     * both the callback emission and AST node creation, following the architectural
     * principle that callbacks create AST nodes, not parselets.</p>
     *
     * <p>Integration pattern:</p>
     * <ul>
     *   <li>Use try-with-resources for SafeCallbacks scope management</li>
     *   <li>Emit appropriate callback sequences (begin → gotLiteral → end)</li>
     *   <li>Handle parsing errors with CallbackIntegrationException</li>
     *   <li>Let SafeCallbacks create the actual AST node</li>
     * </ul>
     *
     * @param parser The parser instance providing SafeCallbacks access
     * @param token The literal token to parse
     * @return A ParseResult containing the literal AST node or error information
     * @throws CallbackIntegrationException if callback integration fails
     */
    @Override
    public @NotNull ParseResult<ParsedNode> parse(KotlinPrattParser parser, @NotNull LocatableToken token) {
        // Validate that this is indeed a literal token type
        if (!isLiteralToken(token.getType())) {
            return ParseResult.failure("Expected literal token, got: " + getTokenTypeName(token.getType()), token);
        }

        // Use SafeCallbacks with try-with-resources pattern for proper callback management
        try (var scope = parser.getCallbacks().beginLiteralExpression(token)) {
            // Parse logic here - validation is already done above
            // Let SafeCallbacks handle the callback emission and AST node creation
            return scope.createResult(token);
        } catch (CallbackIntegrationException e) {
            // Re-throw callback integration exceptions as-is
            throw e;
        } catch (Exception e) {
            // Wrap other exceptions in CallbackIntegrationException
            throw new CallbackIntegrationException(
                "Failed to parse literal: " + e.getMessage(),
                CallbackIntegrationException.FailureType.CALLBACK_EXCEPTION,
                "literal", token);
        }
    }

    /**
     * Determines if a token type represents a literal value.
     *
     * <p>This method checks whether the given token type is one of the
     * recognized literal types that this parselet can handle.</p>
     *
     * @param tokenType The token type to check
     * @return true if the token represents a literal, false otherwise
     */
    private boolean isLiteralToken(int tokenType) {
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
     * Gets a human-readable name for a token type.
     *
     * <p>This method provides descriptive names for token types to improve
     * error messages and debugging information.</p>
     *
     * @param tokenType The token type to describe
     * @return A descriptive name for the token type
     */
    private String getTokenTypeName(int tokenType) {
        return switch (tokenType) {
            case JavaTokenTypes.NUM_INT -> "integer literal";
            case JavaTokenTypes.NUM_LONG -> "long literal";
            case JavaTokenTypes.NUM_FLOAT -> "float literal";
            case JavaTokenTypes.NUM_DOUBLE -> "double literal";
            case JavaTokenTypes.STRING_LITERAL -> "string literal";
            case JavaTokenTypes.STRING_LITERAL_MULTILINE -> "multiline string literal";
            case JavaTokenTypes.CHAR_LITERAL -> "character literal";
            case JavaTokenTypes.LITERAL_true -> "true literal";
            case JavaTokenTypes.LITERAL_false -> "false literal";
            case JavaTokenTypes.LITERAL_null -> "null literal";
            default -> "token type " + tokenType;
        };
    }



    /**
     * Gets the token that this parselet can handle for the given type.
     * This method is used for validation and testing during the foundation phase.
     *
     * @param tokenType The token type to get information for
     * @return A description of what this parselet handles for the token type
     */
    public String getHandledConstruct(int tokenType) {
        if (isLiteralToken(tokenType)) {
            return getTokenTypeName(tokenType);
        }
        return "unsupported token type";
    }

    @Override
    public String toString() {
        return "LiteralParselet";
    }
}
