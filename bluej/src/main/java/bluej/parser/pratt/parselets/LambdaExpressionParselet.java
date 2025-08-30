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
import bluej.parser.pratt.KotlinPrattParser;
import bluej.parser.pratt.NodeFactory;
import bluej.parser.pratt.ParseResult;
import bluej.parser.pratt.PrefixParselet;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A parselet for handling lambda expressions in Kotlin code.
 *
 * <p>This parselet handles Kotlin lambda expressions with the syntax:</p>
 * <ul>
 *   <li>Simple body: {@code { expression }}</li>
 *   <li>With arrow: {@code { -> expression }}</li>
 *   <li>Single parameter: {@code { param -> expression }}</li>
 *   <li>Multiple parameters: {@code { param1, param2 -> expression }}</li>
 * </ul>
 *
 * <p>The implementation uses a simplified approach that first determines whether
 * an arrow token is present, then handles parameter parsing and body parsing
 * accordingly.</p>
 *
 * @author BlueJ Team
 * @see <a href="https://kotlinlang.org/docs/lambdas.html">Kotlin Lambda Expressions</a>
 */
public final class LambdaExpressionParselet implements PrefixParselet {

    /**
     * Parses a lambda expression.
     *
     * <p>This method uses a simplified two-phase approach:</p>
     * <ol>
     *   <li>Look ahead to detect if there's an arrow token</li>
     *   <li>If arrow found: parse parameters, consume arrow, parse body</li>
     *   <li>If no arrow: parse body directly</li>
     *   <li>Consume closing brace and create lambda node</li>
     * </ol>
     *
     * @param parser The parser instance for parsing sub-expressions
     * @param token The opening brace token
     * @return A ParseResult containing the lambda expression node or accumulated errors
     */
    @Override
    public @NotNull ParseResult<ParsedNode> parse(KotlinPrattParser parser, @NotNull LocatableToken token) {
        // Validate that this is indeed a left brace
        if (token.getType() != JavaTokenTypes.LCURLY) {
            return ParseResult.failure("Expected '{' for lambda expression, got: " + getTokenTypeName(token.getType()), token);
        }

        // Look ahead to see if there's an arrow token anywhere before the closing brace
        boolean hasArrow = lookAheadForArrow(parser);

        List<LocatableToken> parameters = new ArrayList<>();
        ParsedNode body;

        if (hasArrow) {
            // Parse parameters until we hit the arrow
            ParseResult<List<LocatableToken>> paramResult = parseParameters(parser);
            if (paramResult.isFailure()) {
                return ParseResult.failure("Error parsing lambda parameters", token);
            }
            parameters = paramResult.getValue();

            // Consume the arrow token
            LocatableToken arrowToken = parser.consume();
            if (arrowToken.getType() != JavaTokenTypes.ARROW) {
                return ParseResult.failure("Expected '->' after lambda parameters", arrowToken);
            }

            // Parse the lambda body
            ParseResult<ParsedNode> bodyResult = parser.parseExpressionResult(0);
            if (bodyResult.isFailure()) {
                return ParseResult.failure("Error parsing lambda body after arrow", token);
            }
            body = bodyResult.getValue();
        } else {
            // No arrow - parse the body directly
            ParseResult<ParsedNode> bodyResult = parser.parseExpressionResult(0);
            if (bodyResult.isFailure()) {
                return ParseResult.failure("Error parsing lambda body", token);
            }
            body = bodyResult.getValue();
        }

        // Consume closing brace
        LocatableToken closingBrace = parser.consume();
        if (closingBrace.getType() == JavaTokenTypes.EOF) {
            return ParseResult.failure(
                "Unmatched braces: expected '}' to match '{' at line " + token.getLine(),
                token);
        }

        if (closingBrace.getType() != JavaTokenTypes.RCURLY) {
            return ParseResult.failure(
                "Unmatched braces: expected '}' but found " + getTokenTypeName(closingBrace.getType()),
                closingBrace);
        }

        // Create the lambda expression node
        NodeFactory nodeFactory = parser.getNodeFactory();
        ParsedNode result = createLambdaNode(nodeFactory, token, parameters, body);
        if (result == null) {
            return ParseResult.failure("Failed to create lambda expression node", token);
        }

        return ParseResult.success(result);
    }

    /**
     * Looks ahead to detect if there's an arrow token before the closing brace.
     *
     * <p>This method scans ahead in the token stream to determine if this lambda
     * expression contains parameter declarations (indicated by the presence of
     * an arrow token).</p>
     *
     * @param parser The parser instance for lookahead operations
     * @return true if an arrow token is found, false otherwise
     */
    private boolean lookAheadForArrow(KotlinPrattParser parser) {
        // Look ahead at most 20 tokens to find arrow or closing brace
        for (int i = 1; i <= 20; i++) {
            LocatableToken token = parser.peek(i);

            if (token.getType() == JavaTokenTypes.EOF) {
                return false; // Hit end of input
            }

            if (token.getType() == JavaTokenTypes.RCURLY) {
                return false; // Hit closing brace without finding arrow
            }

            if (token.getType() == JavaTokenTypes.ARROW) {
                return true; // Found arrow token
            }
        }

        return false; // Didn't find arrow within reasonable lookahead
    }

    /**
     * Parses lambda parameters until an arrow token is encountered.
     *
     * <p>This method expects a comma-separated list of identifiers followed
     * by an arrow token. It consumes tokens until the arrow is reached.</p>
     *
     * @param parser The parser instance for token consumption
     * @return A ParseResult containing the list of parameter tokens
     */
    private ParseResult<List<LocatableToken>> parseParameters(KotlinPrattParser parser) {
        List<LocatableToken> parameters = new ArrayList<>();

        while (true) {
            LocatableToken currentToken = parser.peek();

            if (currentToken.getType() == JavaTokenTypes.EOF) {
                return ParseResult.failure("Unexpected end of file while parsing lambda parameters", currentToken);
            }

            if (currentToken.getType() == JavaTokenTypes.ARROW) {
                // Found arrow - stop parsing parameters
                break;
            }

            if (currentToken.getType() == JavaTokenTypes.IDENT) {
                // Consume parameter name
                parameters.add(parser.consume());

                // Check what follows the identifier
                LocatableToken next = parser.peek();
                if (next.getType() == JavaTokenTypes.COMMA) {
                    parser.consume(); // consume comma and continue
                } else if (next.getType() == JavaTokenTypes.ARROW) {
                    // Arrow follows - we're done with parameters
                    break;
                } else {
                    return ParseResult.failure("Expected ',' or '->' after parameter name", next);
                }
            } else {
                return ParseResult.failure("Expected parameter name, got " + getTokenTypeName(currentToken.getType()), currentToken);
            }
        }

        return ParseResult.success(parameters);
    }

    /**
     * Creates a lambda expression node.
     *
     * <p>This is a helper method to create lambda nodes. Since the NodeFactory interface
     * doesn't currently have a specific method for lambda expressions, this method
     * creates an appropriate node representation using existing factory methods.</p>
     *
     * @param nodeFactory The factory for creating nodes
     * @param openBrace The opening brace token
     * @param parameters The list of parameter tokens
     * @param body The lambda body expression
     * @return A ParsedNode representing the lambda expression, or null if creation failed
     */
    private ParsedNode createLambdaNode(NodeFactory nodeFactory, LocatableToken openBrace,
                                       List<LocatableToken> parameters, ParsedNode body) {
        // TODO: When NodeFactory is extended with lambda support, replace this with:
        // return nodeFactory.createLambdaNode(openBrace, parameters, body);

        // For now, we'll create a grouped expression containing the body
        // This is a temporary solution until proper lambda AST nodes are implemented
        return nodeFactory.createGroupNode(body);
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
            case JavaTokenTypes.LCURLY -> "left brace '{'";
            case JavaTokenTypes.RCURLY -> "right brace '}'";
            case JavaTokenTypes.ARROW -> "arrow '->'";
            case JavaTokenTypes.COMMA -> "comma ','";
            case JavaTokenTypes.IDENT -> "identifier";
            case JavaTokenTypes.EOF -> "end of file";
            default -> "token type " + tokenType;
        };
    }

    @Override
    public String toString() {
        return "LambdaExpressionParselet";
    }
}
