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

/**
 * A parselet for handling parenthesized expressions (grouping) in Kotlin code.
 *
 * <p>This parselet handles expressions enclosed in parentheses, which are used to:</p>
 * <ul>
 *   <li>Override operator precedence: {@code (a + b) * c}</li>
 *   <li>Group complex expressions for clarity: {@code (condition1 && condition2) || condition3}</li>
 *   <li>Ensure correct evaluation order in nested expressions</li>
 * </ul>
 *
 * <p>The parselet expects the opening parenthesis token and parses the inner expression
 * with the lowest precedence to allow any expression type. It then validates that
 * a closing parenthesis follows the expression.</p>
 *
 * <p>Parenthesized expressions inherit the precedence of their inner expression
 * but are parsed as atomic units, making them effectively have the highest precedence
 * for parsing purposes.</p>
 *
 * <p>During the foundation phase, this parselet validates the parenthesis matching
 * and inner expression structure but does not create complete AST nodes.</p>
 *
 * @author BlueJ Team
 * @see <a href="https://kotlinlang.org/spec/syntax-and-grammar.html#expressions">Kotlin Language Specification - Expressions</a>
 */
public final class GroupParselet implements PrefixParselet {

    /**
     * Parses a parenthesized expression.
     *
     * <p>This method handles the complete parsing of a grouped expression:</p>
     * <ol>
     *   <li>Validates the opening parenthesis token</li>
     *   <li>Parses the inner expression with lowest precedence (allowing any expression)</li>
     *   <li>Validates the closing parenthesis token</li>
     *   <li>Returns the inner expression (parentheses are purely syntactic)</li>
     * </ol>
     *
     * <p>The method provides comprehensive error handling for missing closing
     * parentheses and empty expressions between parentheses.</p>
     *
     * @param parser The parser instance for parsing the inner expression
     * @param token The opening parenthesis token
     * @return A ParseResult containing the inner expression or accumulated errors
     */
    @Override
    public ParseResult<ParsedNode> parse(KotlinPrattParser parser, LocatableToken token) {
        if (token == null) {
            return ParseResult.failure("Null token in group parselet", null);
        }

        // Validate that this is indeed a left parenthesis
        if (token.getType() != JavaTokenTypes.LPAREN) {
            return ParseResult.failure("Expected '(' for grouped expression, got: " + getTokenTypeName(token.getType()), token);
        }

        // Parse the inner expression with the lowest precedence
        // This allows any expression type to be parsed inside the parentheses
        ParseResult<ParsedNode> innerResult = parser.parseExpressionResult(0);
        if (innerResult.isFailure()) {
            return ParseResult.failure("Error parsing expression in parentheses", token);
        }

        ParsedNode innerExpression = innerResult.getValue();

        // Expect closing parenthesis
        LocatableToken closingParen = parser.consume();
        if (closingParen == null) {
            return ParseResult.failure(
                "Unbalanced parentheses: expected ')' to match '(' at line " + token.getLine(),
                token);
        }

        if (closingParen.getType() != JavaTokenTypes.RPAREN) {
            return ParseResult.failure(
                "Unbalanced parentheses: expected ')' but found " + getTokenTypeName(closingParen.getType()),
                closingParen);
        }

        // Create the grouped expression node using the NodeFactory
        NodeFactory nodeFactory = parser.getNodeFactory();
        if (nodeFactory == null) {
            return ParseResult.failure("NodeFactory not available for AST node creation", token);
        }

        // Return the grouped expression
        // The factory may return the inner expression directly since
        // parentheses are often purely syntactic and don't create separate AST nodes
        ParsedNode result = nodeFactory.createGroupNode(innerExpression);
        return ParseResult.success(result);
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
            case JavaTokenTypes.LPAREN -> "left parenthesis '('";
            case JavaTokenTypes.RPAREN -> "right parenthesis ')'";
            case JavaTokenTypes.LBRACK -> "left bracket '['";
            case JavaTokenTypes.RBRACK -> "right bracket ']'";
            case JavaTokenTypes.LCURLY -> "left brace '{'";
            case JavaTokenTypes.RCURLY -> "right brace '}'";
            case JavaTokenTypes.SEMI -> "semicolon ';'";
            case JavaTokenTypes.COMMA -> "comma ','";
            case JavaTokenTypes.DOT -> "dot '.'";
            case JavaTokenTypes.EOF -> "end of file";
            default -> "token type " + tokenType;
        };
    }



    /**
     * Gets a description of what this parselet handles for the given token type.
     * This method is used for validation and testing during the foundation phase.
     *
     * @param tokenType The token type to get information for
     * @return A description of what this parselet handles for the token type
     */
    public String getHandledConstruct(int tokenType) {
        if (tokenType == JavaTokenTypes.LPAREN) {
            return "parenthesized expression";
        }
        return "unsupported token type";
    }

    /**
     * Validates that a parenthesized expression has proper structure.
     *
     * <p>This method can be used during testing to verify that the parselet
     * correctly identifies balanced parentheses and valid inner expressions.</p>
     *
     * @param parser The parser instance to use for validation
     * @param token The starting left parenthesis token
     * @return true if the structure is valid, false otherwise
     */
    public boolean validateStructure(KotlinPrattParser parser, LocatableToken token) {
        if (token == null || token.getType() != JavaTokenTypes.LPAREN) {
            return false;
        }

        try {
            // Try to parse the grouped expression
            ParseResult<ParsedNode> result = parse(parser, token);
            return result.isSuccess(); // Success means parsing worked correctly
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return "GroupParselet";
    }
}
