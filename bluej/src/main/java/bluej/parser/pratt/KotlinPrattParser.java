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

import bluej.parser.lexer.JavaTokenFilter;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.SourceParser;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * Main coordinator class for the Kotlin Pratt parser.
 *
 * <p>This class implements the core Pratt parsing algorithm, coordinating between
 * the token stream, parselet registry, and AST node creation. It provides the main
 * expression parsing loop with precedence-driven parsing support.</p>
 *
 * <p>The parser follows the Top-Down Operator Precedence (TDOP) approach, where
 * each token type can have associated prefix and infix parselets that handle
 * the parsing of specific language constructs.</p>
 *
 * @author BlueJ Development Team
 * @since BlueJ 5.4.0
 */
@OnThread(Tag.FXPlatform)
public class KotlinPrattParser {

    /** The token stream providing tokens from the lexer */
    private final JavaTokenFilter tokenStream;

    /** Registry mapping token types to their parselets */
    private final ParseletRegistry registry;

    /** The parent SourceParser for callbacks and integration */
    private final SourceParser sourceParser;

    /** Current token being processed */
    private LocatableToken currentToken;

    /** List of errors encountered during parsing */
    private final List<ParseError> errors;

    /**
     * Creates a new KotlinPrattParser with the specified token stream and parent parser.
     *
     * @param tokenStream The token stream to read tokens from
     * @param sourceParser The parent SourceParser for integration
     */
    public KotlinPrattParser(JavaTokenFilter tokenStream, SourceParser sourceParser) {
        this.tokenStream = tokenStream;
        this.sourceParser = sourceParser;
        this.registry = new ParseletRegistry();
        this.errors = new ArrayList<>();

        // Initialize the registry with parselets
        initializeRegistry();
    }

    /**
     * Creates a new KotlinPrattParser with the specified token stream, parent parser,
     * and custom parselet registry.
     *
     * @param tokenStream The token stream to read tokens from
     * @param sourceParser The parent SourceParser for integration
     * @param registry Custom parselet registry to use
     */
    public KotlinPrattParser(JavaTokenFilter tokenStream, SourceParser sourceParser,
                           ParseletRegistry registry) {
        this.tokenStream = tokenStream;
        this.sourceParser = sourceParser;
        this.registry = registry;
        this.errors = new ArrayList<>();
    }

    /**
     * Initializes the parselet registry with default parselets.
     * This method will be expanded as new parselets are implemented.
     */
    private void initializeRegistry() {
        // TODO: Register parselets as they are implemented
        // Example registrations (to be implemented):
        // registry.register(JavaTokenTypes.LITERAL_int, new LiteralParselet());
        // registry.register(JavaTokenTypes.PLUS, new BinaryOperatorParselet(Precedence.ADDITIVE));
        // registry.register(JavaTokenTypes.LPAREN, new GroupParselet());
    }

    /**
     * Parses an expression with a minimum precedence level.
     *
     * <p>This is the core of the Pratt parsing algorithm. It first parses a prefix
     * expression, then continues parsing infix expressions as long as their precedence
     * is higher than the specified minimum precedence.</p>
     *
     * @param minPrecedence The minimum precedence level to parse
     * @return The parsed AST node, or null if parsing failed
     */
    public ParsedNode parseExpression(int minPrecedence) {
        LocatableToken token = consume();

        if (token == null || token.getType() == JavaTokenTypes.EOF) {
            error("Unexpected end of input", token);
            return null;
        }

        // Look up the prefix parselet for this token type
        PrefixParselet prefix = registry.getPrefix(token.getType());
        if (prefix == null) {
            error("Unexpected token: " + getTokenDescription(token), token);
            return null;
        }

        // Parse the prefix expression
        ParsedNode left = prefix.parse(this, token);
        if (left == null) {
            return null;
        }

        // Continue parsing infix expressions while precedence allows
        while (minPrecedence < getCurrentPrecedence()) {
            token = consume();

            InfixParselet infix = registry.getInfix(token.getType());
            if (infix == null) {
                // This shouldn't happen if getCurrentPrecedence() is correct
                pushBack(token);
                break;
            }

            left = infix.parse(this, left, token);
            if (left == null) {
                return null;
            }
        }

        return left;
    }

    /**
     * Parses a primary expression (with precedence 0).
     *
     * @return The parsed AST node, or null if parsing failed
     */
    public ParsedNode parseExpression() {
        return parseExpression(0);
    }

    /**
     * Gets the precedence of the current token if it has an infix parselet.
     *
     * @return The precedence level, or 0 if no infix parselet exists
     */
    private int getCurrentPrecedence() {
        LocatableToken token = peek();
        if (token == null || token.getType() == JavaTokenTypes.EOF) {
            return 0;
        }
        return registry.getPrecedence(token.getType());
    }

    /**
     * Consumes and returns the next token from the token stream.
     *
     * @return The next token, or null if at end of stream
     */
    public LocatableToken consume() {
        currentToken = tokenStream.nextToken();
        return currentToken;
    }

    /**
     * Looks ahead at the next token without consuming it.
     *
     * @return The next token, or null if at end of stream
     */
    public LocatableToken peek() {
        return tokenStream.LA(1);
    }

    /**
     * Looks ahead at a token at the specified distance without consuming it.
     *
     * @param distance The distance to look ahead (1 or greater)
     * @return The token at the specified distance, or null if beyond end of stream
     */
    public LocatableToken peek(int distance) {
        if (distance < 1) {
            throw new IllegalArgumentException("Look-ahead distance must be 1 or greater");
        }
        return tokenStream.LA(distance);
    }

    /**
     * Pushes a token back onto the token stream.
     * The token will be returned by the next call to consume().
     *
     * @param token The token to push back
     */
    public void pushBack(LocatableToken token) {
        tokenStream.pushBack(token);
        currentToken = tokenStream.getMostRecent();
    }

    /**
     * Checks if the next token matches the expected type.
     *
     * @param tokenType The expected token type
     * @return true if the next token matches, false otherwise
     */
    public boolean match(int tokenType) {
        LocatableToken token = peek();
        return token != null && token.getType() == tokenType;
    }

    /**
     * Consumes a token if it matches the expected type.
     *
     * @param tokenType The expected token type
     * @return The consumed token if it matched, null otherwise
     */
    public LocatableToken consumeIf(int tokenType) {
        if (match(tokenType)) {
            return consume();
        }
        return null;
    }

    /**
     * Expects and consumes a token of the specified type.
     * Reports an error if the token doesn't match.
     *
     * @param tokenType The expected token type
     * @param errorMessage The error message if the token doesn't match
     * @return The consumed token, or null if it didn't match
     */
    public LocatableToken expect(int tokenType, String errorMessage) {
        LocatableToken token = consume();
        if (token == null || token.getType() != tokenType) {
            error(errorMessage, token);
            return null;
        }
        return token;
    }

    /**
     * Records a parse error with location information.
     *
     * @param message The error message
     * @param token The token where the error occurred (may be null)
     */
    public void error(String message, LocatableToken token) {
        ParseError error = new ParseError(message, token);
        errors.add(error);

        // Note: We cannot directly report to sourceParser as the error method is protected.
        // The errors are stored in our error list and can be retrieved via getErrors().
    }

    /**
     * Gets the list of errors encountered during parsing.
     *
     * @return An unmodifiable view of the error list
     */
    public List<ParseError> getErrors() {
        return List.copyOf(errors);
    }

    /**
     * Checks if any errors have been encountered during parsing.
     *
     * @return true if there are errors, false otherwise
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Clears all recorded errors.
     */
    public void clearErrors() {
        errors.clear();
    }

    /**
     * Gets the parselet registry used by this parser.
     *
     * @return The parselet registry
     */
    public ParseletRegistry getRegistry() {
        return registry;
    }

    /**
     * Gets the parent SourceParser.
     *
     * @return The parent SourceParser, may be null
     */
    public SourceParser getSourceParser() {
        return sourceParser;
    }

    /**
     * Gets the current token being processed.
     *
     * @return The current token, may be null
     */
    public LocatableToken getCurrentToken() {
        return currentToken;
    }

    /**
     * Synchronizes the parser to a recovery point after an error.
     *
     * <p>This method consumes tokens until it finds a suitable synchronization
     * point, such as a semicolon, closing brace, or other statement terminator.</p>
     *
     * @param synchronizationTokens Token types to synchronize on
     * @return The synchronization token found, or null if EOF reached
     */
    public LocatableToken synchronize(int... synchronizationTokens) {
        while (true) {
            LocatableToken token = peek();
            if (token == null || token.getType() == JavaTokenTypes.EOF) {
                return null;
            }

            for (int syncToken : synchronizationTokens) {
                if (token.getType() == syncToken) {
                    return consume();
                }
            }

            consume(); // Skip the current token
        }
    }

    /**
     * Gets a human-readable description of a token for error messages.
     *
     * @param token The token to describe
     * @return A descriptive string for the token
     */
    private String getTokenDescription(LocatableToken token) {
        if (token == null) {
            return "null";
        }

        String text = token.getText();
        if (text != null && !text.isEmpty()) {
            return "'" + text + "'";
        }

        // Return token type name for special tokens
        return "token type " + token.getType();
    }

    /**
     * Record for representing parse errors with location information.
     * Uses Java 21 records for immutable error data.
     */
    public record ParseError(String message, LocatableToken token) {
        /**
         * Gets the line number where the error occurred.
         *
         * @return The line number, or -1 if no token
         */
        public int getLine() {
            return token != null ? token.getLine() : -1;
        }

        /**
         * Gets the column number where the error occurred.
         *
         * @return The column number, or -1 if no token
         */
        public int getColumn() {
            return token != null ? token.getColumn() : -1;
        }

        /**
         * Creates a formatted error message with location information.
         *
         * @return The formatted error message
         */
        public String getFormattedMessage() {
            if (token != null) {
                return String.format("Error at line %d, column %d: %s",
                    getLine(), getColumn(), message);
            } else {
                return "Error: " + message;
            }
        }
    }
}
