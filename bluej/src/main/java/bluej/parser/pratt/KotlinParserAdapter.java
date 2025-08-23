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

import bluej.Config;
import bluej.parser.lexer.JavaTokenFilter;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.SourceParser;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.List;

/**
 * Adapter layer for KotlinPrattParser that handles JavaFX platform threading requirements.
 *
 * <p>This adapter isolates the threading concerns from the core parser implementation,
 * allowing KotlinPrattParser to remain thread-agnostic while still satisfying BlueJ's
 * threading requirements for UI integration.</p>
 *
 * <p>The adapter delegates all parsing operations to the underlying KotlinPrattParser
 * while ensuring proper thread context for JavaFX integration.</p>
 *
 * @author BlueJ Development Team
 * @since BlueJ 5.4.0
 */
@OnThread(Tag.FXPlatform)
public class KotlinParserAdapter {

    /** The underlying parser that performs the actual parsing */
    private final KotlinPrattParser parser;

    /** The token stream for thread-safe token operations */
    private final JavaTokenFilter tokenStream;

    /**
     * Creates a new adapter with the specified token stream and parent parser.
     *
     * @param tokenStream The token stream to read tokens from
     * @param sourceParser The parent SourceParser for integration
     */
    public KotlinParserAdapter(JavaTokenFilter tokenStream, SourceParser sourceParser) {
        this.tokenStream = tokenStream;
        // Create parser with thread-safe token operations
        this.parser = new KotlinPrattParser(new ThreadSafeTokenOperations(), sourceParser);
    }

    /**
     * Creates a new adapter with the specified token stream, parent parser,
     * and custom parselet registry.
     *
     * @param tokenStream The token stream to read tokens from
     * @param sourceParser The parent SourceParser for integration
     * @param registry Custom parselet registry to use
     */
    public KotlinParserAdapter(JavaTokenFilter tokenStream, SourceParser sourceParser,
                              ParseletRegistry registry) {
        this.tokenStream = tokenStream;
        // Create parser with thread-safe token operations and custom registry
        this.parser = new KotlinPrattParser(new ThreadSafeTokenOperations(), sourceParser, registry);
    }

    /**
     * Gets the underlying KotlinPrattParser instance.
     *
     * <p>This method is provided for testing and debugging purposes.
     * Normal usage should go through the adapter methods.</p>
     *
     * @return The underlying parser instance
     */
    protected KotlinPrattParser getParser() {
        return parser;
    }

    /**
     * Gets the parselet registry used by the parser.
     *
     * @return The parselet registry
     */
    public ParseletRegistry getRegistry() {
        return parser.getRegistry();
    }

    /**
     * Checks if the parser can handle expression parsing based on configuration
     * and current parser capabilities.
     *
     * @return true if expression parsing is enabled and supported
     */
    public boolean canParseExpression() {
        // Check configuration setting
        boolean configEnabled = Config.getPropBoolean("bluej.kotlin.usePrattParser", false);

        // Check parser capability based on current state and registered parselets
        boolean parserCapable = supportsExpressionParsing();

        return configEnabled && parserCapable;
    }

    /**
     * Checks if the parser currently supports expression parsing.
     * This checks if the necessary parselets are registered and the parser
     * is in a valid state for expression parsing.
     *
     * @return true if the parser has the necessary parselets for expression parsing
     */
    public boolean supportsExpressionParsing() {
        // Check if we have basic expression parselets registered
        ParseletRegistry registry = parser.getRegistry();

        // Check for essential parselets needed for basic expression parsing
        // This list can be expanded as more parselets are implemented
        boolean hasLiterals = registry.hasPrefix(JavaTokenTypes.NUM_INT) ||
                              registry.hasPrefix(JavaTokenTypes.STRING_LITERAL) ||
                              registry.hasPrefix(JavaTokenTypes.LITERAL_true) ||
                              registry.hasPrefix(JavaTokenTypes.LITERAL_false);

        boolean hasOperators = registry.hasInfix(JavaTokenTypes.PLUS) ||
                               registry.hasInfix(JavaTokenTypes.MINUS) ||
                               registry.hasInfix(JavaTokenTypes.STAR) ||
                               registry.hasInfix(JavaTokenTypes.DIV);

        boolean hasIdentifiers = registry.hasPrefix(JavaTokenTypes.IDENT);

        // For now, consider expression parsing supported if we have at least
        // some basic parselets registered
        return hasLiterals || hasOperators || hasIdentifiers;
    }

    /**
     * Checks if the parser can handle statement parsing based on configuration
     * and current parser capabilities.
     *
     * @return true if statement parsing is enabled and supported
     */
    public boolean canParseStatement() {
        // Check configuration setting
        boolean configEnabled = Config.getPropBoolean("bluej.kotlin.usePrattParser.statements", false);

        // Check parser capability
        boolean parserCapable = supportsStatementParsing();

        return configEnabled && parserCapable;
    }

    /**
     * Checks if the parser currently supports statement parsing.
     *
     * @return true if the parser has the necessary parselets for statement parsing
     */
    public boolean supportsStatementParsing() {
        // Statement parsing requires expression parsing plus statement-specific parselets
        if (!supportsExpressionParsing()) {
            return false;
        }

        ParseletRegistry registry = parser.getRegistry();

        // Check for statement-specific parselets
        boolean hasStatements = registry.hasPrefix(JavaTokenTypes.LITERAL_if) ||
                                registry.hasPrefix(JavaTokenTypes.LITERAL_while) ||
                                registry.hasPrefix(JavaTokenTypes.LITERAL_for) ||
                                registry.hasPrefix(JavaTokenTypes.LITERAL_return);

        return hasStatements;
    }

    /**
     * Checks if the parser can handle declaration parsing based on configuration
     * and current parser capabilities.
     *
     * @return true if declaration parsing is enabled and supported
     */
    public boolean canParseDeclaration() {
        // Check configuration setting
        boolean configEnabled = Config.getPropBoolean("bluej.kotlin.usePrattParser.declarations", false);

        // Check parser capability
        boolean parserCapable = supportsDeclarationParsing();

        return configEnabled && parserCapable;
    }

    /**
     * Checks if the parser currently supports declaration parsing.
     *
     * @return true if the parser has the necessary parselets for declaration parsing
     */
    public boolean supportsDeclarationParsing() {
        ParseletRegistry registry = parser.getRegistry();

        // Check for declaration-specific parselets
        boolean hasDeclarations = registry.hasPrefix(JavaTokenTypes.LITERAL_class) ||
                                  registry.hasPrefix(JavaTokenTypes.LITERAL_interface) ||
                                  registry.hasPrefix(JavaTokenTypes.LITERAL_fun) ||
                                  registry.hasPrefix(JavaTokenTypes.LITERAL_val) ||
                                  registry.hasPrefix(JavaTokenTypes.LITERAL_var);

        return hasDeclarations;
    }

    /**
     * Parses an expression starting from the current position with the specified precedence.
     *
     * Note: Callers should check {@link #canParseExpression()} before calling this method
     * to ensure the parser is configured and capable of parsing expressions.
     *
     * @param precedence The minimum precedence level for the expression
     * @return The parsed AST node, or null if parsing fails
     */
    public ParsedNode parseExpression(int precedence) {
        if (!canParseExpression()) {
            // Log or handle the case where parsing is attempted but not supported
            return null;
        }
        return parser.parseExpression(precedence);
    }

    /**
     * Parses an expression starting from the current position.
     *
     * Note: Callers should check {@link #canParseExpression()} before calling this method
     * to ensure the parser is configured and capable of parsing expressions.
     *
     * @return The parsed AST node, or null if parsing fails
     */
    public ParsedNode parseExpression() {
        if (!canParseExpression()) {
            // Log or handle the case where parsing is attempted but not supported
            return null;
        }
        return parser.parseExpression();
    }

    /**
     * Parses a statement starting from the current position.
     *
     * Note: Callers should check {@link #canParseStatement()} before calling this method
     * to ensure the parser is configured and capable of parsing statements.
     *
     * @return The parsed AST node representing the statement, or null if parsing fails
     */
    public ParsedNode parseStatement() {
        if (!canParseStatement()) {
            return null;
        }
        return parser.parseStatement();
    }

    /**
     * Parses a declaration (class, function, property, etc.) starting from the current position.
     *
     * Note: Callers should check {@link #canParseDeclaration()} before calling this method
     * to ensure the parser is configured and capable of parsing declarations.
     *
     * @return The parsed AST node representing the declaration, or null if parsing fails
     */
    public ParsedNode parseDeclaration() {
        if (!canParseDeclaration()) {
            return null;
        }
        return parser.parseDeclaration();
    }

    /**
     * Parses a complete compilation unit (file).
     *
     * @return The parsed AST node representing the compilation unit, or null if parsing fails
     */
    public ParsedNode parseCompilationUnit() {
        return parser.parseCompilationUnit();
    }

    /**
     * Consumes and returns the next token from the token stream.
     *
     * @return The next token
     */
    public LocatableToken consume() {
        return parser.consume();
    }

    /**
     * Consumes a token of the specified type, reporting an error if the current token
     * doesn't match.
     *
     * @param expectedType The expected token type
     * @return The consumed token, or null if the token type doesn't match
     */
    public LocatableToken consume(int expectedType) {
        return parser.consume(expectedType);
    }

    /**
     * Peeks at the next token without consuming it.
     *
     * @return The next token in the stream
     */
    public LocatableToken peek() {
        return parser.peek();
    }

    /**
     * Peeks ahead at the token at the specified offset.
     *
     * @param offset The number of tokens to look ahead (0 = current token)
     * @return The token at the specified offset
     */
    public LocatableToken peek(int offset) {
        return parser.peek(offset);
    }

    /**
     * Checks if the next token is of the specified type.
     *
     * @param tokenType The token type to check for
     * @return true if the next token matches the type, false otherwise
     */
    public boolean check(int tokenType) {
        return parser.check(tokenType);
    }

    /**
     * Checks if the token at the specified offset is of the specified type.
     *
     * @param tokenType The token type to check for
     * @param offset The offset to check at
     * @return true if the token matches the type, false otherwise
     */
    public boolean check(int tokenType, int offset) {
        return parser.check(tokenType, offset);
    }

    /**
     * Checks if the next token matches any of the specified types.
     *
     * @param tokenTypes The token types to check for
     * @return true if the next token matches any of the types, false otherwise
     */
    public boolean checkAny(int... tokenTypes) {
        return parser.checkAny(tokenTypes);
    }

    /**
     * Consumes a token if it matches the expected type.
     *
     * @param expectedType The expected token type
     * @return true if the token was consumed, false otherwise
     */
    public boolean match(int expectedType) {
        return parser.match(expectedType);
    }

    /**
     * Consumes a token if it matches any of the expected types.
     *
     * @param expectedTypes The expected token types
     * @return true if a token was consumed, false otherwise
     */
    public boolean matchAny(int... expectedTypes) {
        return parser.matchAny(expectedTypes);
    }

    /**
     * Reports a parsing error at the current position.
     *
     * @param message The error message
     */
    public void error(String message) {
        parser.error(message);
    }

    /**
     * Reports a parsing error at the specified token.
     *
     * @param token The token where the error occurred
     * @param message The error message
     */
    public void error(LocatableToken token, String message) {
        parser.error(message, token);
    }

    /**
     * Gets all errors encountered during parsing.
     *
     * @return An unmodifiable list of parse errors
     */
    public List<KotlinPrattParser.ParseError> getErrors() {
        return parser.getErrors();
    }

    /**
     * Checks if any errors were encountered during parsing.
     *
     * @return true if there are errors, false otherwise
     */
    public boolean hasErrors() {
        return parser.hasErrors();
    }

    /**
     * Clears all recorded errors.
     */
    public void clearErrors() {
        parser.clearErrors();
    }

    /**
     * Synchronizes the parser to a stable state after an error.
     *
     * <p>This method consumes tokens until it finds one that can start a new
     * statement or declaration, allowing parsing to continue after an error.</p>
     */
    public void synchronize() {
        parser.synchronize();
    }

    /**
     * Pushes a token back onto the token stream.
     *
     * <p>This is useful for implementing lookahead and backtracking.</p>
     *
     * @param token The token to push back
     */
    public void pushBack(LocatableToken token) {
        parser.pushBack(token);
    }

    /**
     * Gets the current token position in the source.
     *
     * @return The current token, or null if at end of stream
     */
    public LocatableToken getCurrentToken() {
        return parser.getCurrentToken();
    }

    /**
     * Gets the parent SourceParser.
     *
     * @return The parent SourceParser
     */
    public SourceParser getSourceParser() {
        return parser.getSourceParser();
    }

    /**
     * Checks if we've reached the end of the token stream.
     *
     * @return true if at end of stream, false otherwise
     */
    public boolean isAtEnd() {
        return parser.isAtEnd();
    }

    /**
     * Imports necessary token type constants for parselet checking.
     */
    private static class JavaTokenTypes {
        // Literals
        static final int NUM_INT = bluej.parser.lexer.JavaTokenTypes.NUM_INT;
        static final int STRING_LITERAL = bluej.parser.lexer.JavaTokenTypes.STRING_LITERAL;
        static final int LITERAL_true = bluej.parser.lexer.JavaTokenTypes.LITERAL_true;
        static final int LITERAL_false = bluej.parser.lexer.JavaTokenTypes.LITERAL_false;

        // Operators
        static final int PLUS = bluej.parser.lexer.JavaTokenTypes.PLUS;
        static final int MINUS = bluej.parser.lexer.JavaTokenTypes.MINUS;
        static final int STAR = bluej.parser.lexer.JavaTokenTypes.STAR;
        static final int DIV = bluej.parser.lexer.JavaTokenTypes.DIV;

        // Identifiers
        static final int IDENT = bluej.parser.lexer.JavaTokenTypes.IDENT;

        // Statements
        static final int LITERAL_if = bluej.parser.lexer.JavaTokenTypes.LITERAL_if;
        static final int LITERAL_while = bluej.parser.lexer.JavaTokenTypes.LITERAL_while;
        static final int LITERAL_for = bluej.parser.lexer.JavaTokenTypes.LITERAL_for;
        static final int LITERAL_return = bluej.parser.lexer.JavaTokenTypes.LITERAL_return;

        // Declarations
        static final int LITERAL_class = bluej.parser.lexer.JavaTokenTypes.LITERAL_class;
        static final int LITERAL_interface = bluej.parser.lexer.JavaTokenTypes.LITERAL_interface;
        static final int LITERAL_fun = bluej.parser.lexer.JavaTokenTypes.LITERAL_fun;
        static final int LITERAL_val = bluej.parser.lexer.JavaTokenTypes.LITERAL_val;
        static final int LITERAL_var = bluej.parser.lexer.JavaTokenTypes.LITERAL_var;
    }

    /**
     * Thread-safe implementation of TokenOperations that delegates to JavaTokenFilter.
     * All methods are called within the FXPlatform thread context as required by JavaTokenFilter.
     */
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    private class ThreadSafeTokenOperations implements TokenOperations {
        @Override
        public LocatableToken nextToken() {
            // This is called within @OnThread(Tag.FXPlatform) context
            return tokenStream.nextToken();
        }

        @Override
        public LocatableToken LA(int distance) {
            // This is called within @OnThread(Tag.FXPlatform) context
            return tokenStream.LA(distance);
        }

        @Override
        public void pushBack(LocatableToken token) {
            // This is called within @OnThread(Tag.FXPlatform) context
            tokenStream.pushBack(token);
        }

        @Override
        public LocatableToken getMostRecent() {
            // This is called within @OnThread(Tag.FXPlatform) context
            return tokenStream.getMostRecent();
        }
    }
}
