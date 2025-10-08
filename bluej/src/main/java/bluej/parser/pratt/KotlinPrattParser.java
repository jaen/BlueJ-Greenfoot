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
import bluej.parser.pratt.parselets.ArrayAccessParselet;
import bluej.parser.pratt.parselets.BinaryOperatorParselet;
import bluej.parser.pratt.parselets.CallParselet;
import bluej.parser.pratt.parselets.ElvisOperatorParselet;
import bluej.parser.pratt.parselets.GroupParselet;
import bluej.parser.pratt.parselets.InOperatorParselet;
import bluej.parser.pratt.parselets.LambdaExpressionParselet;
import bluej.parser.pratt.parselets.LiteralParselet;
import bluej.parser.pratt.parselets.MemberAccessParselet;
import bluej.parser.pratt.parselets.NameParselet;
import bluej.parser.pratt.parselets.PostfixOperatorParselet;
import bluej.parser.pratt.parselets.PrefixOperatorParselet;
import bluej.parser.pratt.parselets.SuperParselet;
import bluej.parser.pratt.parselets.ThisParselet;
import bluej.parser.pratt.parselets.RangeParselet;
import bluej.parser.pratt.parselets.TypeCastParselet;
import bluej.parser.pratt.parselets.TypeCheckParselet;
import org.jetbrains.annotations.NotNull;

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
    private final List<ParseResult.ParseError> errors;

    /** Factory for creating AST nodes with proper threading */
    private final NodeFactory nodeFactory;

    /** Error recovery mechanism for handling parse errors gracefully */
    private final ErrorRecovery errorRecovery;

    /** SafeCallbacks for managing callback integration with TrackedScope */
    private final SafeCallbacks safeCallbacks;

    /**
     * Creates a new KotlinPrattParser with the specified token operations and parent parser.
     *
     * @param tokenOps The token operations interface for accessing tokens
     * @param sourceParser The parent SourceParser for integration
     * @param nodeFactory The factory for creating AST nodes
     */
    public KotlinPrattParser(TokenOperations tokenOps, SourceParser sourceParser, NodeFactory nodeFactory) {
        this.tokenOps = tokenOps;
        this.sourceParser = sourceParser;
        this.nodeFactory = nodeFactory;
        this.registry = new ParseletRegistry();
        this.errors = new ArrayList<>();
        this.errorRecovery = new ErrorRecovery();
        this.safeCallbacks = new SafeCallbacks(sourceParser.getCallbackDelegate(), nodeFactory);

        // Initialize the registry with parselets
        initializeRegistry();
    }

    /**
     * Creates a new KotlinPrattParser with the specified token operations, parent parser,
     * node factory, and custom parselet registry.
     *
     * @param tokenOps The token operations interface for accessing tokens
     * @param sourceParser The parent SourceParser for integration
     * @param nodeFactory The factory for creating AST nodes
     * @param registry Custom parselet registry to use
     */
    public KotlinPrattParser(TokenOperations tokenOps, SourceParser sourceParser,
                           NodeFactory nodeFactory, ParseletRegistry registry) {
        this.tokenOps = tokenOps;
        this.sourceParser = sourceParser;
        this.nodeFactory = nodeFactory;
        this.registry = registry;
        this.errors = new ArrayList<>();
        this.errorRecovery = new ErrorRecovery();
        this.safeCallbacks = new SafeCallbacks(sourceParser.getCallbackDelegate(), nodeFactory);
    }

    /**
     * Initializes the parselet registry with all implemented parselets.
     * Registers parselets for literals, operators, identifiers, calls, and access expressions.
     *
     * Note: Some tokens can have both prefix and infix parselets (e.g., PLUS can be unary + or binary +).
     * The registry supports this by maintaining separate maps for prefix and infix parselets.
     */
    private void initializeRegistry() {
        // ===================== LITERAL PARSELETS (PREFIX) =====================
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

        // ===================== IDENTIFIER PARSELETS (PREFIX) =====================
        NameParselet nameParselet = new NameParselet();
        registry.register(JavaTokenTypes.IDENT, nameParselet);

        // Special keyword references
        registry.register(JavaTokenTypes.LITERAL_this, new ThisParselet());
        registry.register(JavaTokenTypes.LITERAL_super, new SuperParselet());

        // ===================== GROUPING PARSELET (PREFIX) =====================
        registry.register(JavaTokenTypes.LPAREN, new GroupParselet());

        // ===================== LAMBDA EXPRESSION PARSELET (PREFIX) =====================
        registry.register(JavaTokenTypes.LCURLY, new LambdaExpressionParselet());

        // ===================== PREFIX OPERATOR PARSELETS =====================
        // Arithmetic prefix operators (+, -)
        registry.register(JavaTokenTypes.PLUS, new PrefixOperatorParselet(Precedence.PREFIX.getValue()));
        registry.register(JavaTokenTypes.MINUS, new PrefixOperatorParselet(Precedence.PREFIX.getValue()));

        // Logical not operator (!)
        registry.register(JavaTokenTypes.LNOT, new PrefixOperatorParselet(Precedence.PREFIX.getValue()));

        // Prefix increment/decrement operators (++, --)
        registry.register(JavaTokenTypes.INC, new PrefixOperatorParselet(140));
        registry.register(JavaTokenTypes.DEC, new PrefixOperatorParselet(140));

        // ===================== BINARY OPERATOR PARSELETS (INFIX) =====================
        // Multiplicative operators (*, /, %)
        registry.register(JavaTokenTypes.STAR, new BinaryOperatorParselet(Precedence.MULTIPLICATIVE));
        registry.register(JavaTokenTypes.DIV, new BinaryOperatorParselet(Precedence.MULTIPLICATIVE));
        registry.register(JavaTokenTypes.MOD, new BinaryOperatorParselet(Precedence.MULTIPLICATIVE));

        // Additive operators (+, -) - these can also be prefix operators
        registry.register(JavaTokenTypes.PLUS, new BinaryOperatorParselet(Precedence.ADDITIVE));
        registry.register(JavaTokenTypes.MINUS, new BinaryOperatorParselet(Precedence.ADDITIVE));

        // Comparison operators (<, >, <=, >=)
        registry.register(JavaTokenTypes.LT, new BinaryOperatorParselet(Precedence.COMPARISON));
        registry.register(JavaTokenTypes.GT, new BinaryOperatorParselet(Precedence.COMPARISON));
        registry.register(JavaTokenTypes.LE, new BinaryOperatorParselet(Precedence.COMPARISON));
        registry.register(JavaTokenTypes.GE, new BinaryOperatorParselet(Precedence.COMPARISON));

        // Equality operators (==, !=)
        registry.register(JavaTokenTypes.EQUAL, new BinaryOperatorParselet(Precedence.EQUALITY));
        registry.register(JavaTokenTypes.NOT_EQUAL, new BinaryOperatorParselet(Precedence.EQUALITY));

        // Logical operators (&&, ||)
        registry.register(JavaTokenTypes.LAND, new BinaryOperatorParselet(Precedence.CONJUNCTION));
        registry.register(JavaTokenTypes.LOR, new BinaryOperatorParselet(Precedence.DISJUNCTION));

        // Elvis operator (?:) - null-coalescing operator
        registry.register(JavaTokenTypes.ELVIS, new ElvisOperatorParselet());

        // ===================== TYPE OPERATION PARSELETS (INFIX) =====================
        // Type check operators (is, !is)
        registry.register(JavaTokenTypes.LITERAL_is, new TypeCheckParselet());

        // Type cast operators (as, as?)
        registry.register(JavaTokenTypes.LITERAL_as, new TypeCastParselet());

        // Containment operators (in, !in)
        registry.register(JavaTokenTypes.LITERAL_in, new InOperatorParselet());

        // ===================== RANGE OPERATION PARSELETS (INFIX) =====================
        // Range operator (..)
        registry.register(JavaTokenTypes.RANGE, new RangeParselet());

        // Assignment operators (=, +=, -=, etc.)
        registry.register(JavaTokenTypes.ASSIGN, new BinaryOperatorParselet(Precedence.ASSIGNMENT));
        registry.register(JavaTokenTypes.PLUS_ASSIGN, new BinaryOperatorParselet(Precedence.ASSIGNMENT));
        registry.register(JavaTokenTypes.MINUS_ASSIGN, new BinaryOperatorParselet(Precedence.ASSIGNMENT));
        registry.register(JavaTokenTypes.STAR_ASSIGN, new BinaryOperatorParselet(Precedence.ASSIGNMENT));
        registry.register(JavaTokenTypes.DIV_ASSIGN, new BinaryOperatorParselet(Precedence.ASSIGNMENT));

        // ===================== POSTFIX OPERATOR PARSELETS (INFIX) =====================
        // Postfix increment/decrement (++, --) - these can also be prefix operators
        registry.register(JavaTokenTypes.INC, new PostfixOperatorParselet(Precedence.POSTFIX.getValue()));
        registry.register(JavaTokenTypes.DEC, new PostfixOperatorParselet(Precedence.POSTFIX.getValue()));

        // Note: Null assertion operator (!!) would need a specific token type
        // For now, we don't register EXCLAM for postfix since it conflicts with logical not

        // ===================== ACCESS AND CALL PARSELETS (INFIX) =====================
        // Member access (. and ?.)
        MemberAccessParselet memberAccessParselet = new MemberAccessParselet();
        registry.register(JavaTokenTypes.DOT, memberAccessParselet);
        registry.register(JavaTokenTypes.SAFE_ACCESS, memberAccessParselet);

        // Array access ([])
        registry.register(JavaTokenTypes.LBRACK, new ArrayAccessParselet());

        // Function calls (()) - LPAREN can be both prefix (grouping) and infix (calls)
        registry.register(JavaTokenTypes.LPAREN, new CallParselet());
    }

    /**
     * Parses an expression with a minimum precedence level using monadic ParseResult.
     *
     * <p>This is the core of the Pratt parsing algorithm using the enhanced error handling
     * approach. It first parses a prefix expression, then continues parsing infix expressions
     * while accumulating errors in a monadic fashion.</p>
     *
     * @param minPrecedence The minimum precedence level to parse
     * @return A ParseResult containing the parsed AST node or accumulated errors
     */
    public ParseResult<ParsedNode> parseExpressionResult(int minPrecedence) {
        LocatableToken token = consume();

        if (token.getType() == JavaTokenTypes.EOF) {
            return ParseResult.failure("Unexpected end of input", token);
        }

        // Look up the prefix parselet for this token type
        PrefixParselet prefix = registry.getPrefix(token.getType());
        if (prefix == null) {
            return ParseResult.failure("Unexpected token: " + getTokenDescription(token), token);
        }

        // Parse the prefix expression using the new ParseResult approach
        ParseResult<ParsedNode> leftResult = prefix.parse(this, token);
        if (leftResult.isFailure()) {
            return leftResult;
        }

        ParsedNode left = leftResult.getValue();
        // TODO: I don't think we should need to do this manually
        List<ParseResult.ParseError> accumulatedErrors = new java.util.ArrayList<>(leftResult.getErrors());

        // Continue parsing infix expressions while precedence allows
        while (minPrecedence < getCurrentPrecedence()) {
            token = consume();

            InfixParselet infix = registry.getInfix(token.getType());
            if (infix == null) {
                // This shouldn't happen if getCurrentPrecedence() is correct
                pushBack(token);
                break;
            }

            ParseResult<ParsedNode> result = infix.parse(this, left, token);
            accumulatedErrors.addAll(result.getErrors());

            if (result.hasValue()) {
                left = result.getValue();
            } else {
                // Can't continue without a value
                return ParseResult.failure(accumulatedErrors);
            }
        }

        // Check for invalid token sequences after expression parsing
        // This catches cases like "42 42" where two operands appear without an operator
        LocatableToken nextToken = peek();
        if (nextToken.getType() != JavaTokenTypes.EOF) {
            // Check if we have an operand following another operand without an operator
            if (isOperandToken(nextToken.getType()) && left != null) {
                // We just parsed an expression (operand) and the next token is also an operand
                // This is invalid syntax - two operands without an operator between them
                String errorMsg = "Invalid syntax: two operands without operator between parsed expression and '" +
                                 nextToken.getText() + "'";
                accumulatedErrors.add(new ParseResult.ParseError(errorMsg, nextToken));
                return ParseResult.failure(accumulatedErrors);
            }
        }

        // Return result with accumulated errors
        if (accumulatedErrors.isEmpty()) {
            return ParseResult.success(left);
        } else {
            return ParseResult.failure(accumulatedErrors);
        }
    }

    /**
     * Parses an expression with a minimum precedence level.
     *
     * <p>This method provides backward compatibility by delegating to parseExpressionResult
     * and extracting the value while reporting any errors through the traditional error mechanism.</p>
     *
     * @param minPrecedence The minimum precedence level to parse
     * @return The parsed AST node, or null if parsing failed
     */
    public ParsedNode parseExpression(int minPrecedence) {
        ParseResult<ParsedNode> result = parseExpressionResult(minPrecedence);

        // Report any errors through the traditional mechanism
        for (ParseResult.ParseError error : result.getErrors()) {
            error(error.message(), error.token());
        }

        return result.getValueOrNull();
    }

    /**
     * Parses a primary expression (with precedence 0) using ParseResult.
     *
     * @return A ParseResult containing the parsed AST node or errors
     */
    public ParseResult<ParsedNode> parseExpressionResult() {
        return parseExpressionResult(0);
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
        if (token.getType() == JavaTokenTypes.EOF) {
            return 0;
        }
        return registry.getPrecedence(token.getType());
    }

    /**
     * Consumes and returns the next token from the token stream.
     *
     * @return The next token, or null if at end of stream
     */
    public @NotNull LocatableToken consume() {
        currentToken = tokenOps.nextToken();
        return currentToken;
    }

    /**
     * Looks ahead at the next token without consuming it.
     *
     * @return The next token, or null if at end of stream
     */
    public @NotNull LocatableToken peek() {
        return tokenOps.LA(1);
    }

    /**
     * Looks ahead at a token at the specified distance without consuming it.
     *
     * @param distance The distance to look ahead (1 or greater)
     * @return The token at the specified distance, or null if beyond end of stream
     */
    public @NotNull LocatableToken peek(int distance) {
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
        return token.getType() == tokenType;
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
        if (token.getType() != tokenType) {
            error(errorMessage, token);
            return null;
        }
        return token;
    }

    /**
     * Checks if a token type represents an operand (literal, identifier, etc.).
     * Used for validating token sequences to detect invalid syntax.
     *
     * @param tokenType The token type to check
     * @return true if the token is an operand, false otherwise
     */
    private boolean isOperandToken(int tokenType) {
        switch (tokenType) {
            // Literals
            case JavaTokenTypes.NUM_INT:
            case JavaTokenTypes.NUM_FLOAT:
            case JavaTokenTypes.NUM_LONG:
            case JavaTokenTypes.NUM_DOUBLE:
            case JavaTokenTypes.STRING_LITERAL:
            case JavaTokenTypes.LITERAL_true:
            case JavaTokenTypes.LITERAL_false:
            case JavaTokenTypes.LITERAL_null:
            // Identifiers and keywords that act as operands
            case JavaTokenTypes.IDENT:
            case JavaTokenTypes.LITERAL_this:
            case JavaTokenTypes.LITERAL_super:
            // Opening parenthesis starts a grouped operand
            case JavaTokenTypes.LPAREN:
                return true;
            default:
                return false;
        }
    }

    /**
     * Records a parse error with location information.
     *
     * @param message The error message
     * @param token The token where the error occurred (may be null)
     */
    public void error(String message, LocatableToken token) {
        ParseResult.ParseError error = new ParseResult.ParseError(message, token);
        errors.add(error);

        // Note: We cannot directly report to sourceParser as the error method is protected.
        // The errors are stored in our error list and can be retrieved via getErrors().
    }

    /**
     * Records a parse error and attempts recovery using the specified context.
     *
     * @param message The error message
     * @param token The token where the error occurred (may be null)
     * @param context The parsing context for appropriate recovery strategy
     * @return true if recovery was successful and parsing can continue, false otherwise
     */
    public boolean errorWithRecovery(String message, LocatableToken token, ErrorRecovery.RecoveryContext context) {
        // Create contextual error message
        String contextualMessage = ErrorRecovery.createContextualErrorMessage(message, context);

        // Report the error through the recovery system
        errorRecovery.reportError(this, contextualMessage, token, context);

        // Attempt recovery if appropriate
        if (errorRecovery.shouldAttemptRecovery()) {
            return errorRecovery.recoverToSynchronizationPoint(this, context);
        }

        return false;
    }

    /**
     * Attempts to recover from parsing errors in expression context.
     *
     * @param message The error message
     * @param token The problematic token
     * @return true if recovery successful, false otherwise
     */
    public boolean recoverFromExpressionError(String message, LocatableToken token) {
        return errorWithRecovery(message, token, ErrorRecovery.RecoveryContext.EXPRESSION);
    }

    /**
     * Attempts to recover from parsing errors in statement context.
     *
     * @param message The error message
     * @param token The problematic token
     * @return true if recovery successful, false otherwise
     */
    public boolean recoverFromStatementError(String message, LocatableToken token) {
        return errorWithRecovery(message, token, ErrorRecovery.RecoveryContext.STATEMENT);
    }

    /**
     * Gets the error recovery system for advanced error handling.
     *
     * @return The ErrorRecovery instance used by this parser
     */
    public ErrorRecovery getErrorRecovery() {
        return errorRecovery;
    }

    /**
     * Checks if the parser is currently in error recovery mode.
     *
     * @return true if in recovery mode, false otherwise
     */
    public boolean isInRecoveryMode() {
        return errorRecovery.isInRecoveryMode();
    }

    /**
     * Exits error recovery mode, typically called after successful parsing.
     */
    public void exitRecoveryMode() {
        errorRecovery.exitRecoveryMode();
    }

    /**
     * Gets the list of parse errors encountered during parsing.
     *
     * @return An unmodifiable list of parse errors
     */
    public List<ParseResult.ParseError> getErrors() {
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
     * @return The parent SourceParser instance
     */
    public SourceParser getSourceParser() {
        return sourceParser;
    }

    /**
     * Gets the NodeFactory for creating AST nodes.
     *
     * @return The NodeFactory instance
     */
    public NodeFactory getNodeFactory() {
        return nodeFactory;
    }

    /**
     * Gets the SafeCallbacks instance for callback integration.
     *
     * @return The SafeCallbacks instance
     */
    public SafeCallbacks getCallbacks() {
        return safeCallbacks;
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
        if (token.getType() == expectedType) {
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
        return token.getType() == tokenType;
    }

    /**
     * Checks if the next token matches any of the specified types.
     *
     * @param tokenTypes The token types to check for
     * @return true if the next token matches any of the types, false otherwise
     */
    public boolean checkAny(int... tokenTypes) {
        LocatableToken token = peek();
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
        return token.getType() == JavaTokenTypes.EOF;
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
            if (token.getType() == JavaTokenTypes.EOF) {
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


}
