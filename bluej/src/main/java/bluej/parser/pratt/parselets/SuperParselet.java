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
 * Parselet for parsing 'super' keyword references in Kotlin.
 *
 * <p>This parselet handles the 'super' keyword which provides a reference to the parent
 * class instance. It creates appropriate AST nodes using the parser's NodeFactory to
 * ensure thread-safe node creation.</p>
 *
 * <p>The 'super' keyword can appear in various contexts:</p>
 * <ul>
 *   <li>Member access: {@code super.property}, {@code super.method()}</li>
 *   <li>Method calls: {@code super.someMethod()}</li>
 *   <li>Constructor delegation: {@code super(parameters)}</li>
 *   <li>Qualified super access: {@code super<ParentClass>.method()}</li>
 * </ul>
 *
 * <p>This parselet implements {@code PrefixParselet} since 'super' appears at the
 * beginning of expressions as a primary expression.</p>
 *
 * <h3>Examples:</h3>
 * <pre>
 * super                // Reference to parent object
 * super.name           // Access to parent property (handled by MemberAccessParselet)
 * super.doSomething()  // Parent method call (handled by CallParselet)
 * super(args)          // Constructor delegation (handled by CallParselet)
 * return super.value   // Return parent property value
 * </pre>
 *
 * @author BlueJ Team
 * @since BlueJ 5.4.0
 * @see NodeFactory#createSuperNode(LocatableToken)
 * @see ThisParselet
 */
public class SuperParselet implements PrefixParselet {

    /**
     * Creates a new SuperParselet instance.
     */
    public SuperParselet() {
        // No configuration needed for 'super' keyword parsing
    }

    /**
     * Parses a 'super' keyword token into a 'super' reference node.
     *
     * <p>This method handles the parsing of the 'super' keyword, validating that
     * the token represents the correct keyword and creating an appropriate AST node
     * using the parser's NodeFactory.</p>
     *
     * <p>The method validates that the token is actually a 'super' keyword token
     * and creates a node representing a reference to the parent class instance.</p>
     *
     * @param parser The parser instance (used to access NodeFactory)
     * @param token The 'super' keyword token to parse
     * @return A ParsedNode representing the 'super' reference, or null if parsing failed
     * @throws IllegalArgumentException if token is null
     */
    @Override
    public @NotNull ParseResult<ParsedNode> parse(KotlinPrattParser parser, @NotNull LocatableToken token) {
        // Validate token is not null
//        if (token == null) {
//            return ParseResult.failure("Null token in 'super' parselet", null);
//        }

        // Get NodeFactory
        NodeFactory nodeFactory = parser.getNodeFactory();
        if (nodeFactory == null) {
            return ParseResult.failure("NodeFactory not available", token);
        }

        // Validate that this is actually a 'super' token
        if (token.getType() != JavaTokenTypes.LITERAL_super) {
            return ParseResult.failure(
                "Expected 'super' keyword but found: " + getTokenDescription(token), token);
        }

        // Create the 'super' reference node using the factory
        try {
            ParsedNode node = nodeFactory.createSuperNode(token);
            return ParseResult.success(node);
        } catch (Exception e) {
            return ParseResult.failure(
                "Failed to create 'super' node: " + e.getMessage(), token);
        }
    }

    /**
     * Validates that the given token represents the 'super' keyword.
     *
     * <p>This method checks the token type to ensure it's the correct 'super' keyword token.</p>
     *
     * @param token The token to validate
     * @return true if the token represents the 'super' keyword, false otherwise
     */
    private boolean isSuperToken(LocatableToken token) {
        if (token == null) {
            return false;
        }

        return token.getType() == JavaTokenTypes.LITERAL_super;
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
        return "SuperParselet";
    }
}
