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
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.nodes.ExpressionNode;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.SourceParser;
import bluej.parser.pratt.parselets.BinaryOperatorParselet;
import bluej.parser.pratt.parselets.GroupParselet;
import bluej.parser.pratt.parselets.LiteralParselet;

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
 * <p>This class is thread-agnostic. For JavaFX platform threading requirements,
 * use the {@link KotlinParserAdapter} class which provides thread-safe access.</p>
 *
 * @author BlueJ Development Team
 * @since BlueJ 5.4.0
 */
public class KotlinPrattParser {

    /** The token operations interface for accessing tokens */
    private final TokenOperations tokenOps;

    /** Registry mapping token types to their parselets */
    private final ParseletRegistry registry;

    /** The parent SourceParser for callbacks and integration */
    private final SourceParser sourceParser;

    /** Current token being processed */
    private LocatableToken currentToken;

    /** List of errors encountered during parsing */
    private final List<ParseError> errors;

    /**
     * Creates a new KotlinPrattParser with the specified token operations and parent parser.
     *
     * @param tokenOps The token operations interface for accessing tokens
     * @param sourceParser The parent SourceParser for integration
     */
    public KotlinPrattParser(TokenOperations tokenOps, SourceParser sourceParser) {
        this.tokenOps = tokenOps;
        this.sourceParser = sourceParser;
        this.registry = new ParseletRegistry();
        this.errors = new ArrayList<>();

        // Initialize the registry with parselets
        initializeRegistry();
    }

    /**
     * Creates a new KotlinPrattParser with the specified token operations, parent parser,
     * and custom parselet registry.
     *
     * @param tokenOps The token operations interface for accessing tokens
     * @param sourceParser The parent SourceParser for integration
     * @param registry Custom parselet registry to use
     */
    public KotlinPrattParser(TokenOperations tokenOps, SourceParser sourceParser,
                           ParseletRegistry registry) {
        this.tokenOps = tokenOps;
        this.sourceParser = sourceParser;
        this.registry = registry;
        this.errors = new ArrayList<>();
    }

    /**
     * Initializes the parselet registry with default parselets.
     * This method will be expanded as new parselets are implemented.
     */
    private void initializeRegistry() {
        // Register literal parselets for all literal token types
        LiteralParselet literalParselet = new LiteralParselet();
        registry.register(JavaTokenTypes.NUM_INT, literalParselet);
        registry.register(JavaTokenTypes.NUM_LONG, literalParselet);
        registry.register(JavaTokenTypes.NUM_FLOAT, literalParselet);
        registry.register(JavaTokenTypes.NUM_DOUBLE, literalParselet);
        registry.register(JavaTokenTypes.STRING_LITERAL, literalParselet);
        registry.register(JavaTokenTypes.STRING_LITERAL_MULTILINE, literalParselet);
        registry.register(JavaTokenTypes.CHAR_LITERAL, literalParselet);
        registry.register(JavaTokenTypes.LITERAL_true, literalParselet);
        registry.register(JavaTokenTypes.LITERAL_false, literalParselet);
        registry.register(JavaTokenTypes.LITERAL_null, literalParselet);

        // Register group parselet for parenthesized expressions
        registry.register(JavaTokenTypes.LPAREN, new GroupParselet());

        // Register binary operator parselets
        registry.register(JavaTokenTypes.PLUS, new BinaryOperatorParselet(Precedence.ADDITIVE));
        registry.register(JavaTokenTypes.MINUS, new BinaryOperatorParselet(Precedence.ADDITIVE));
        registry.register(JavaTokenTypes.STAR, new BinaryOperatorParselet(Precedence.MULTIPLICATIVE));
        registry.register(JavaTokenTypes.DIV, new BinaryOperatorParselet(Precedence.MULTIPLICATIVE));
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
        currentToken = tokenOps.nextToken();
        return currentToken;
    }

    /**
     * Looks ahead at the next token without consuming it.
     *
     * @return The next token, or null if at end of stream
     */
    public LocatableToken peek() {
        return tokenOps.LA(1);
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
        return tokenOps.LA(distance);
    }

    /**
     * Pushes a token back onto the token stream.
     * The token will be returned by the next call to consume().
     *
     * @param token The token to push back
     */
    public void pushBack(LocatableToken token) {
        tokenOps.pushBack(token);
        currentToken = tokenOps.getMostRecent();
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
     * Parses a statement starting from the current position.
     * This is a placeholder implementation that will be expanded
     * when statement parselets are implemented.
     *
     * @return The parsed AST node representing the statement, or null if parsing fails
     */
    public ParsedNode parseStatement() {
        // TODO: Implement statement parsing once statement parselets are available
        // For now, try to parse as an expression
        return parseExpression();
    }

    /**
     * Parses a declaration (class, function, property, etc.) starting from the current position.
     * This is a placeholder implementation that will be expanded
     * when declaration parselets are implemented.
     *
     * @return The parsed AST node representing the declaration, or null if parsing fails
     */
    public ParsedNode parseDeclaration() {
        // TODO: Implement declaration parsing once declaration parselets are available
        return null;
    }

    /**
     * Parses a complete compilation unit (file).
     * This is a placeholder implementation that will be expanded
     * when compilation unit parsing is fully implemented.
     *
     * @return The parsed AST node representing the compilation unit, or null if parsing fails
     */
    public ParsedNode parseCompilationUnit() {
        // TODO: Implement full compilation unit parsing
        return null;
    }

    /**
     * Consumes a token of the specified type, reporting an error if the current token
     * doesn't match.
     *
     * @param expectedType The expected token type
     * @return The consumed token, or null if the token type doesn't match
     */
    public LocatableToken consume(int expectedType) {
        LocatableToken token = peek();
        if (token != null && token.getType() == expectedType) {
            return consume();
        }
        error("Expected token type " + expectedType + " but found " + getTokenDescription(token), token);
        return null;
    }

    /**
     * Checks if the next token is of the specified type.
     *
     * @param tokenType The token type to check for
     * @return true if the next token matches the type, false otherwise
     */
    public boolean check(int tokenType) {
        return match(tokenType);
    }

    /**
     * Checks if the token at the specified offset is of the specified type.
     *
     * @param tokenType The token type to check for
     * @param offset The offset to check at (0 = current peek position)
     * @return true if the token matches the type, false otherwise
     */
    public boolean check(int tokenType, int offset) {
        if (offset == 0) {
            return check(tokenType);
        }
        LocatableToken token = peek(offset + 1);
        return token != null && token.getType() == tokenType;
    }

    /**
     * Checks if the next token matches any of the specified types.
     *
     * @param tokenTypes The token types to check for
     * @return true if the next token matches any of the types, false otherwise
     */
    public boolean checkAny(int... tokenTypes) {
        LocatableToken token = peek();
        if (token == null) {
            return false;
        }
        int type = token.getType();
        for (int tokenType : tokenTypes) {
            if (type == tokenType) {
                return true;
            }
        }
        return false;
    }

    /**
     * Consumes a token if it matches any of the expected types.
     *
     * @param expectedTypes The expected token types
     * @return true if a token was consumed, false otherwise
     */
    public boolean matchAny(int... expectedTypes) {
        if (checkAny(expectedTypes)) {
            consume();
            return true;
        }
        return false;
    }

    /**
     * Checks if we've reached the end of the token stream.
     *
     * @return true if at end of stream, false otherwise
     */
    public boolean isAtEnd() {
        LocatableToken token = peek();
        return token == null || token.getType() == JavaTokenTypes.EOF;
    }

    /**
     * Reports a parsing error at the current position.
     * Convenience method that uses the current token.
     *
     * @param message The error message
     */
    public void error(String message) {
        error(message, getCurrentToken());
    }

    /**
     * Synchronizes the parser to a stable state after an error.
     *
     * <p>This method consumes tokens until it finds a suitable synchronization
     * point, such as a semicolon, closing brace, or other statement terminator.
     * This enhanced version includes common Kotlin synchronization patterns.</p>
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

            // Check explicit synchronization tokens first
            for (int syncToken : synchronizationTokens) {
                if (token.getType() == syncToken) {
                    return consume();
                }
            }

            // Check common recovery points that are generally safe
            int tokenType = token.getType();
            if (isCommonSyncPoint(tokenType)) {
                return consume();
            }

            consume(); // Skip the current token
        }
    }

    /**
     * Initializes the parser to start reading from the token stream.
     * This method is primarily used for testing to set up the parser state
     * after configuring the token operations.
     */
    public void initializeWithTokenStream() {
        // Initialize current token by peeking at the first token
        // This sets up the parser to be in a consistent state for parsing
        currentToken = tokenOps.LA(1);
    }

    /**
     * Checks if a token type is a common synchronization point.
     * These are tokens where we can safely resume parsing after an error.
     */
    private boolean isCommonSyncPoint(int tokenType) {
        return tokenType == JavaTokenTypes.SEMI ||
               tokenType == JavaTokenTypes.RCURLY ||
               tokenType == JavaTokenTypes.LITERAL_class ||
               tokenType == JavaTokenTypes.LITERAL_interface ||
               tokenType == JavaTokenTypes.LITERAL_fun ||
               tokenType == JavaTokenTypes.LITERAL_val ||
               tokenType == JavaTokenTypes.LITERAL_var ||
               tokenType == JavaTokenTypes.LITERAL_if ||
               tokenType == JavaTokenTypes.LITERAL_while ||
               tokenType == JavaTokenTypes.LITERAL_for ||
               tokenType == JavaTokenTypes.LITERAL_return;
    }

    /**
     * Synchronizes on statement boundaries.
     * Common recovery strategy for statement-level errors.
     *
     * @return The synchronization token found, or null if EOF reached
     */
    public LocatableToken synchronizeOnStatements() {
        return synchronize(STATEMENT_SYNC_TOKENS);
    }

    /**
     * Synchronizes on declaration boundaries.
     * Common recovery strategy for declaration-level errors.
     *
     * @return The synchronization token found, or null if EOF reached
     */
    public LocatableToken synchronizeOnDeclarations() {
        return synchronize(DECLARATION_SYNC_TOKENS);
    }

    /**
     * Synchronizes on expression boundaries.
     * Common recovery strategy for expression-level errors.
     *
     * @return The synchronization token found, or null if EOF reached
     */
    public LocatableToken synchronizeOnExpressions() {
        return synchronize(EXPRESSION_SYNC_TOKENS);
    }

    /**
     * Synchronizes on block boundaries.
     * Common recovery strategy for nested structure errors.
     *
     * @return The synchronization token found, or null if EOF reached
     */
    public LocatableToken synchronizeOnBlocks() {
        return synchronize(BLOCK_SYNC_TOKENS);
    }

    /**
     * Synchronizes on parameter or argument boundaries.
     * Common recovery strategy for function parameter or call argument errors.
     *
     * @return The synchronization token found, or null if EOF reached
     */
    public LocatableToken synchronizeOnParameters() {
        return synchronize(PARAMETER_SYNC_TOKENS);
    }

    /**
     * Synchronizes on type boundaries.
     * Common recovery strategy for type expression or annotation errors.
     *
     * @return The synchronization token found, or null if EOF reached
     */
    public LocatableToken synchronizeOnTypes() {
        return synchronize(TYPE_SYNC_TOKENS);
    }

    /**
     * Comprehensive synchronization using all major recovery points.
     * Used as a fallback when specific recovery strategies don't apply.
     *
     * @return The synchronization token found, or null if EOF reached
     */
    public LocatableToken synchronizeComprehensive() {
        return synchronize(ALL_SYNC_TOKENS);
    }

    // ==================== Predefined Synchronization Token Sets ====================

    /**
     * Tokens that commonly mark statement boundaries and are safe recovery points.
     * These include terminators and statement-starting keywords.
     */
    public static final int[] STATEMENT_SYNC_TOKENS = {
        JavaTokenTypes.SEMI,
        JavaTokenTypes.RCURLY,
        JavaTokenTypes.LITERAL_if,
        JavaTokenTypes.LITERAL_else,
        JavaTokenTypes.LITERAL_while,
        JavaTokenTypes.LITERAL_for,
        JavaTokenTypes.LITERAL_do,
        JavaTokenTypes.LITERAL_return,
        JavaTokenTypes.LITERAL_break,
        JavaTokenTypes.LITERAL_continue,
        JavaTokenTypes.LITERAL_throw,
        JavaTokenTypes.LITERAL_try,
        JavaTokenTypes.LITERAL_when
    };

    /**
     * Tokens that mark declaration boundaries and top-level constructs.
     * Safe recovery points for class-level and file-level parsing errors.
     */
    public static final int[] DECLARATION_SYNC_TOKENS = {
        JavaTokenTypes.LITERAL_class,
        JavaTokenTypes.LITERAL_interface,
        JavaTokenTypes.LITERAL_object,
        JavaTokenTypes.LITERAL_enum,
        JavaTokenTypes.LITERAL_fun,
        JavaTokenTypes.LITERAL_val,
        JavaTokenTypes.LITERAL_var,
        JavaTokenTypes.LITERAL_constructor,
        JavaTokenTypes.LITERAL_init,
        JavaTokenTypes.RCURLY,
        JavaTokenTypes.LITERAL_package,
        JavaTokenTypes.LITERAL_import
    };

    /**
     * Tokens that mark expression boundaries and are safe for expression recovery.
     * Used when recovering from expression parsing errors.
     */
    public static final int[] EXPRESSION_SYNC_TOKENS = {
        JavaTokenTypes.SEMI,
        JavaTokenTypes.COMMA,
        JavaTokenTypes.RPAREN,
        JavaTokenTypes.RBRACK,
        JavaTokenTypes.RCURLY,
        JavaTokenTypes.LITERAL_else,
        JavaTokenTypes.LITERAL_catch,
        JavaTokenTypes.LITERAL_finally,
        JavaTokenTypes.GT, // End of generic parameter
        JavaTokenTypes.LITERAL_in // for-in loop boundary
    };

    /**
     * Tokens that mark block boundaries, useful for recovering from nested structure errors.
     * These tokens typically indicate the end of a block scope.
     */
    public static final int[] BLOCK_SYNC_TOKENS = {
        JavaTokenTypes.RCURLY,
        JavaTokenTypes.RPAREN,
        JavaTokenTypes.RBRACK,
        JavaTokenTypes.LITERAL_else,
        JavaTokenTypes.LITERAL_catch,
        JavaTokenTypes.LITERAL_finally,
        JavaTokenTypes.EOF
    };

    /**
     * Tokens that mark parameter or argument list boundaries.
     * Used for recovering from function parameter or call argument parsing errors.
     */
    public static final int[] PARAMETER_SYNC_TOKENS = {
        JavaTokenTypes.COMMA,
        JavaTokenTypes.RPAREN,
        JavaTokenTypes.RBRACK,
        JavaTokenTypes.GT, // End of generic parameters
        JavaTokenTypes.SEMI,
        JavaTokenTypes.LCURLY, // Start of function body
        JavaTokenTypes.ASSIGN // Default parameter value
    };

    /**
     * Tokens that mark type boundaries, useful for type expression recovery.
     * Used when parsing type annotations, generic parameters, or type declarations.
     */
    public static final int[] TYPE_SYNC_TOKENS = {
        JavaTokenTypes.COMMA,
        JavaTokenTypes.RPAREN,
        JavaTokenTypes.RBRACK,
        JavaTokenTypes.GT,
        JavaTokenTypes.QUESTION, // Nullable type marker
        JavaTokenTypes.ASSIGN,
        JavaTokenTypes.LCURLY,
        JavaTokenTypes.SEMI,
        JavaTokenTypes.LITERAL_where // Generic constraints
    };

    /**
     * Comprehensive set of all major synchronization points.
     * Used as a fallback when specific recovery strategies don't apply.
     */
    public static final int[] ALL_SYNC_TOKENS = {
        // Statement boundaries
        JavaTokenTypes.SEMI,
        JavaTokenTypes.RCURLY,
        JavaTokenTypes.RPAREN,
        JavaTokenTypes.RBRACK,

        // Declaration keywords
        JavaTokenTypes.LITERAL_class,
        JavaTokenTypes.LITERAL_interface,
        JavaTokenTypes.LITERAL_object,
        JavaTokenTypes.LITERAL_enum,
        JavaTokenTypes.LITERAL_fun,
        JavaTokenTypes.LITERAL_val,
        JavaTokenTypes.LITERAL_var,

        // Control flow
        JavaTokenTypes.LITERAL_if,
        JavaTokenTypes.LITERAL_else,
        JavaTokenTypes.LITERAL_while,
        JavaTokenTypes.LITERAL_for,
        JavaTokenTypes.LITERAL_return,
        JavaTokenTypes.LITERAL_break,
        JavaTokenTypes.LITERAL_continue,
        JavaTokenTypes.LITERAL_try,
        JavaTokenTypes.LITERAL_catch,
        JavaTokenTypes.LITERAL_finally,
        JavaTokenTypes.LITERAL_when,

        // Special boundaries
        JavaTokenTypes.LITERAL_package,
        JavaTokenTypes.LITERAL_import,
        JavaTokenTypes.EOF
    };

    /**
     * Attempts to recover from an unexpected token by suggesting what was expected.
     * Provides error messages consistent with legacy parser patterns.
     *
     * @param expectedDescription Description of what was expected
     * @param actualToken The actual token encountered
     */
    public void reportUnexpectedToken(String expectedDescription, LocatableToken actualToken) {
        String actualDescription = getTokenDescription(actualToken);
        if (actualToken != null && actualToken.getType() == JavaTokenTypes.EOF) {
            error("Unexpected end-of-file; expected " + expectedDescription, actualToken);
        } else {
            error("Expected " + expectedDescription + " but found " + actualDescription, actualToken);
        }
    }

    /**
     * Reports a missing token error consistent with legacy parser patterns.
     *
     * @param missingTokenDescription Description of the missing token
     * @param position Token indicating where the missing token should have been
     */
    public void reportMissingToken(String missingTokenDescription, LocatableToken position) {
        error("Expected " + missingTokenDescription, position);
    }

    /**
     * Enhanced expect method with better error reporting.
     * Provides specific error messages for common token types.
     */
    public LocatableToken expectWithMessage(int tokenType, LocatableToken currentToken) {
        if (currentToken != null && currentToken.getType() == tokenType) {
            return consume();
        }

        // Generate specific error message based on token type
        String expectedDescription = getExpectedTokenDescription(tokenType);
        reportUnexpectedToken(expectedDescription, currentToken);
        return null;
    }

    /**
     * Gets a description of what token type was expected for error messages.
     */
    private String getExpectedTokenDescription(int tokenType) {
        return switch (tokenType) {
            case JavaTokenTypes.LPAREN -> "'('";
            case JavaTokenTypes.RPAREN -> "')'";
            case JavaTokenTypes.LCURLY -> "'{'";
            case JavaTokenTypes.RCURLY -> "'}'";
            case JavaTokenTypes.SEMI -> "';'";
            case JavaTokenTypes.COMMA -> "','";
            case JavaTokenTypes.IDENT -> "identifier";
            case JavaTokenTypes.LITERAL_class -> "keyword 'class'";
            case JavaTokenTypes.LITERAL_fun -> "keyword 'fun'";
            case JavaTokenTypes.LITERAL_val -> "keyword 'val'";
            case JavaTokenTypes.LITERAL_var -> "keyword 'var'";
            default -> "token type " + tokenType;
        };
    }

    /**
     * Creates a literal AST node for the given token.
     * 
     * <p>This method creates an appropriate AST node to represent a literal value.
     * For now, it returns null as a placeholder while we're working on threading issues.
     * The actual node creation needs to happen on the appropriate thread.</p>
     * 
     * @param token The literal token to create a node for
     * @return An ExpressionNode representing the literal (currently null for foundation)
     */
    public ParsedNode createLiteralNode(LocatableToken token) {
        // TODO: Implement proper AST node creation with threading support
        // The node creation needs to happen on the FXPlatform thread
        // This will be implemented in the threading adapter phase
        return null;
    }

    /**
     * Gets a human-readable description of a token for error messages.
     * Provides consistent descriptions matching the legacy parser patterns.
     *
     * @param token The token to describe
     * @return A descriptive string for the token
     */
    private String getTokenDescription(LocatableToken token) {
        if (token == null) {
            return "end of input";
        }

        int tokenType = token.getType();
        String text = token.getText();

        // Handle special token types with descriptive names
        return switch (tokenType) {
            case JavaTokenTypes.EOF -> "end of file";
            case JavaTokenTypes.IDENT -> "identifier '" + text + "'";
            case JavaTokenTypes.NUM_INT -> "integer literal '" + text + "'";
            case JavaTokenTypes.NUM_FLOAT -> "float literal '" + text + "'";
            case JavaTokenTypes.STRING_LITERAL -> "string literal";
            case JavaTokenTypes.CHAR_LITERAL -> "character literal";
            case JavaTokenTypes.LITERAL_true, JavaTokenTypes.LITERAL_false -> "boolean literal '" + text + "'";
            case JavaTokenTypes.LITERAL_null -> "null literal";
            case JavaTokenTypes.LPAREN -> "'('";
            case JavaTokenTypes.RPAREN -> "')'";
            case JavaTokenTypes.LCURLY -> "'{'";
            case JavaTokenTypes.RCURLY -> "'}'";
            case JavaTokenTypes.LBRACK -> "'['";
            case JavaTokenTypes.RBRACK -> "']'";
            case JavaTokenTypes.SEMI -> "';'";
            case JavaTokenTypes.COMMA -> "','";
            case JavaTokenTypes.DOT -> "'.'";
            case JavaTokenTypes.LITERAL_class -> "keyword 'class'";
            case JavaTokenTypes.LITERAL_interface -> "keyword 'interface'";
            case JavaTokenTypes.LITERAL_fun -> "keyword 'fun'";
            case JavaTokenTypes.LITERAL_val -> "keyword 'val'";
            case JavaTokenTypes.LITERAL_var -> "keyword 'var'";
            case JavaTokenTypes.LITERAL_if -> "keyword 'if'";
            case JavaTokenTypes.LITERAL_else -> "keyword 'else'";
            case JavaTokenTypes.LITERAL_while -> "keyword 'while'";
            case JavaTokenTypes.LITERAL_for -> "keyword 'for'";
            case JavaTokenTypes.LITERAL_return -> "keyword 'return'";
            case JavaTokenTypes.LITERAL_import -> "keyword 'import'";
            case JavaTokenTypes.LITERAL_package -> "keyword 'package'";
            default -> {
                if (text != null && !text.isEmpty()) {
                    yield "'" + text + "'";
                } else {
                    yield "token type " + tokenType;
                }
            }
        };
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
