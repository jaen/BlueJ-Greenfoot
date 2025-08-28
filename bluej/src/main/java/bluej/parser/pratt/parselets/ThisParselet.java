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
import bluej.parser.pratt.PrefixParselet;
import bluej.parser.pratt.ParseResult;
import org.jetbrains.annotations.NotNull;

/**
 * Parselet for parsing 'this' keyword references in Kotlin.
 *
 * <p>This parselet handles the 'this' keyword which provides a reference to the current
 * object instance. It creates appropriate AST nodes using the parser's NodeFactory to
 * ensure thread-safe node creation.</p>
 *
 * <p>The 'this' keyword can appear in various contexts:</p>
 * <ul>
 *   <li>Member access: {@code this.property}, {@code this.method()}</li>
 *   <li>Method calls: {@code this.someMethod()}</li>
 *   <li>Constructor delegation: {@code this(parameters)}</li>
 *   <li>Expression context: {@code return this}</li>
 * </ul>
 *
 * <p>This parselet implements {@code PrefixParselet} since 'this' appears at the
 * beginning of expressions as a primary expression.</p>
 *
 * <h3>Examples:</h3>
 * <pre>
 * this                 // Reference to current object
 * this.name            // Access to property (handled by MemberAccessParselet)
 * this.doSomething()   // Method call (handled by CallParselet)
 * return this          // Return current object
 * </pre>
 *
 * @author BlueJ Team
 * @since BlueJ 5.4.0
 * @see NodeFactory#createThisNode(LocatableToken)
 * @see SuperParselet
 */
public class ThisParselet implements PrefixParselet {

    /**
     * Creates a new ThisParselet instance.
     */
    public ThisParselet() {
        // No configuration needed for 'this' keyword parsing
    }

    /**
     * Parses a 'this' keyword token into a 'this' reference node.
     *
     * <p>This method handles the parsing of the 'this' keyword, validating that
     * the token represents the correct keyword and creating an appropriate AST node
     * using the parser's NodeFactory.</p>
     *
     * <p>The method validates that the token is actually a 'this' keyword token
     * and creates a node representing a reference to the current object instance.</p>
     *
     * @param parser The parser instance (used to access NodeFactory)
     * @param token The 'this' keyword token to parse
     * @return A ParsedNode representing the 'this' reference, or null if parsing failed
     * @throws IllegalArgumentException if token is null
     */
    @Override
    public @NotNull ParseResult<ParsedNode> parse(KotlinPrattParser parser, @NotNull LocatableToken token) {
        // Validate token is not null
//        if (token == null) {
//            return ParseResult.failure("Null token in 'this' parselet", null);
//        }

        // Get NodeFactory
        NodeFactory nodeFactory = parser.getNodeFactory();
        if (nodeFactory == null) {
            return ParseResult.failure("NodeFactory not available", token);
        }

        // Validate that this is actually a 'this' token
        if (token.getType() != JavaTokenTypes.LITERAL_this) {
            return ParseResult.failure(
                "Expected 'this' keyword but found: " + getTokenDescription(token), token);
        }

        // Create the 'this' reference node using the factory
        try {
            ParsedNode node = nodeFactory.createThisNode(token);
            return ParseResult.success(node);
        } catch (Exception e) {
            return ParseResult.failure(
                "Failed to create 'this' node: " + e.getMessage(), token);
        }
    }

    /**
     * Validates that the given token represents the 'this' keyword.
     *
     * <p>This method checks the token type to ensure it's the correct 'this' keyword token.</p>
     *
     * @param token The token to validate
     * @return true if the token represents the 'this' keyword, false otherwise
     */
    private boolean isThisToken(LocatableToken token) {
        if (token == null) {
            return false;
        }

        return token.getType() == JavaTokenTypes.LITERAL_this;
    }

    /**
     * Gets a human-readable description of the token for error messages.
     *
     * @param token The token to describe
     * @return A description of the token type and text
     */
    private String getTokenDescription(LocatableToken token) {
        if (token == null) {
            return "null token";
        }

        String tokenText = token.getText();
        if (tokenText == null || tokenText.isEmpty()) {
            return "token type " + token.getType();
        }

        return "'" + tokenText + "' (type " + token.getType() + ")";
    }

    @Override
    public String toString() {
        return "ThisParselet";
    }
}
