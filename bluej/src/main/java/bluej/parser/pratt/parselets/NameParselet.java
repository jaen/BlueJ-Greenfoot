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

/**
 * Parselet for parsing identifier names in Kotlin.
 *
 * <p>This parselet handles simple identifiers that can appear at the start of expressions.
 * It creates identifier reference nodes using the parser's NodeFactory. Qualified names
 * (e.g., package.qualified.names) are handled by the combination of this parselet for
 * the first identifier and {@code MemberAccessParselet} for subsequent dot-separated parts.</p>
 *
 * <p>Supported identifier formats:</p>
 * <ul>
 *   <li>Simple identifiers: {@code myVariable}, {@code functionName}</li>
 *   <li>Backtick identifiers: {@code `class`}, {@code `fun`} (keywords as identifiers)</li>
 *   <li>Unicode identifiers following Kotlin identifier rules</li>
 * </ul>
 *
 * <p>This parselet implements {@code PrefixParselet} since identifiers can appear at the
 * beginning of expressions as primary expressions.</p>
 *
 * <h3>Examples:</h3>
 * <pre>
 * myVariable           // Simple identifier
 * `class`              // Backtick-wrapped keyword as identifier
 * _privateField        // Identifier starting with underscore
 * myPackage.MyClass    // First part handled by NameParselet, rest by MemberAccessParselet
 * </pre>
 *
 * @author BlueJ Team
 * @since BlueJ 5.4.0
 * @see MemberAccessParselet
 * @see NodeFactory#createIdentifierNode(LocatableToken)
 */
public class NameParselet implements PrefixParselet {

    /**
     * Creates a new NameParselet instance.
     */
    public NameParselet() {
        // No configuration needed for basic name parsing
    }

    /**
     * Parses an identifier token into an identifier reference node.
     *
     * <p>This method handles the parsing of identifier tokens, including:</p>
     * <ul>
     *   <li>Regular identifiers that follow Kotlin naming conventions</li>
     *   <li>Backtick-wrapped identifiers that allow keywords as names</li>
     *   <li>Unicode identifiers that are valid in Kotlin</li>
     * </ul>
     *
     * <p>The method validates that the token represents a valid identifier and
     * creates an appropriate AST node using the parser's NodeFactory.</p>
     *
     * @param parser The parser instance (used to access NodeFactory)
     * @param token The identifier token to parse
     * @return A ParsedNode representing the identifier reference, or null if parsing failed
     * @throws IllegalArgumentException if token is null
     */
    @Override
    public ParsedNode parse(KotlinPrattParser parser, LocatableToken token) {
        if (token == null) {
            throw new IllegalArgumentException("Token cannot be null");
        }

        NodeFactory nodeFactory = parser.getNodeFactory();
        if (nodeFactory == null) {
            throw new IllegalStateException("Parser must have a NodeFactory to create nodes");
        }

        // Validate that this is actually an identifier token
        if (!isValidIdentifierToken(token)) {
            nodeFactory.reportError("Expected identifier but got: " + token.getType(), token);
            return null;
        }

        // Handle backtick identifiers by extracting the actual name
        String identifierText = extractIdentifierText(token);
        if (identifierText == null || identifierText.isEmpty()) {
            nodeFactory.reportError("Invalid identifier format: " + token.getText(), token);
            return null;
        }

        // Validate identifier according to Kotlin rules
        if (!isValidKotlinIdentifier(identifierText)) {
            nodeFactory.reportError("Invalid Kotlin identifier: " + identifierText, token);
            return null;
        }

        // Create the identifier node using the factory
        try {
            ParsedNode identifierNode = nodeFactory.createIdentifierNode(token);
            if (identifierNode == null) {
                nodeFactory.reportError("Failed to create identifier node", token);
            }
            return identifierNode;
        } catch (Exception e) {
            nodeFactory.reportError("Error creating identifier node: " + e.getMessage(), token);
            return null;
        }
    }

    /**
     * Validates that the given token represents a valid identifier.
     *
     * <p>This method checks the token type to ensure it's appropriate for identifier parsing.
     * Valid identifier tokens include regular identifiers and backtick-wrapped identifiers.</p>
     *
     * @param token The token to validate
     * @return true if the token can be parsed as an identifier, false otherwise
     */
    private boolean isValidIdentifierToken(LocatableToken token) {
        if (token == null) {
            return false;
        }

        // Check for standard identifier token type
        // Note: Backtick identifiers are also IDENT tokens, distinguished by their text format
        return token.getType() == JavaTokenTypes.IDENT;
    }

    /**
     * Extracts the actual identifier text from the token.
     *
     * <p>This method handles different identifier formats:</p>
     * <ul>
     *   <li>Regular identifiers: returns the text as-is</li>
     *   <li>Backtick identifiers: removes the surrounding backticks</li>
     * </ul>
     *
     * @param token The identifier token
     * @return The extracted identifier name, or null if extraction failed
     */
    private String extractIdentifierText(LocatableToken token) {
        String text = token.getText();
        if (text == null) {
            return null;
        }

        // Handle backtick identifiers by removing the backticks
        if (text.length() >= 2 && text.startsWith("`") && text.endsWith("`")) {
            return text.substring(1, text.length() - 1);
        }

        // Regular identifier - return as-is
        return text;
    }

    /**
     * Validates that the extracted text represents a valid Kotlin identifier.
     *
     * <p>This method checks that the identifier follows Kotlin naming rules:</p>
     * <ul>
     *   <li>Must start with a letter or underscore</li>
     *   <li>Can contain letters, digits, and underscores</li>
     *   <li>Cannot be empty</li>
     *   <li>Unicode characters are allowed if they satisfy the above rules</li>
     * </ul>
     *
     * @param identifier The identifier text to validate
     * @return true if the identifier is valid according to Kotlin rules, false otherwise
     */
    private boolean isValidKotlinIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return false;
        }

        // Check first character - must be letter or underscore
        char firstChar = identifier.charAt(0);
        if (!Character.isLetter(firstChar) && firstChar != '_') {
            return false;
        }

        // Check remaining characters - must be letters, digits, or underscores
        for (int i = 1; i < identifier.length(); i++) {
            char c = identifier.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return "NameParselet";
    }
}
