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
package bluej.parser.pratt.testutil;

import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LineColPos;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.pratt.InfixParselet;
import bluej.parser.pratt.KotlinPrattParser;
import bluej.parser.pratt.ParseResult;
import bluej.parser.pratt.PrefixParselet;
import bluej.parser.pratt.TestNodeFactory;
import bluej.parser.pratt.TokenOperations;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock implementation of KotlinPrattParser for testing parselets.
 *
 * <p>This class provides a controlled parser environment for unit tests,
 * with features for token management, error tracking, and expression parsing
 * simulation.</p>
 *
 * @author BlueJ Team
 * @since BlueJ 5.4.0
 */
public class MockParser extends KotlinPrattParser {
    // Error tracking
    private boolean hasErrors;
    private String lastError;
    private final List<String> allErrors;

    // Token management
    private final List<LocatableToken> tokenSequence;
    private int tokenIndex;
    private boolean endOfInput;

    // Parse result management
    private ParsedNode nextParseResult;
    private int lastParseExpressionPrecedence;

    // Token operations
    private final MockTokenOperations mockTokenOps;
    
    // Parselet registry for supporting recursive parsing
    private final Map<Integer, PrefixParselet> prefixParselets;
    private final Map<Integer, InfixParselet> infixParselets;

    /**
     * Creates a new MockParser with default configuration.
     */
    public MockParser() {
        this(new MockTokenOperations(), new TestNodeFactory());
    }

    /**
     * Creates a new MockParser with specified token operations and node factory.
     *
     * @param tokenOps Token operations to use
     * @param nodeFactory Node factory to use
     */
    public MockParser(TokenOperations tokenOps, TestNodeFactory nodeFactory) {
        super(tokenOps, null, nodeFactory);
        this.mockTokenOps = (tokenOps instanceof MockTokenOperations) ? (MockTokenOperations) tokenOps : null;
        this.hasErrors = false;
        this.lastError = "";
        this.allErrors = new ArrayList<>();
        this.tokenSequence = new ArrayList<>();
        this.tokenIndex = 0;
        this.endOfInput = false;
        this.nextParseResult = null;
        this.lastParseExpressionPrecedence = -1;
        this.prefixParselets = new HashMap<>();
        this.infixParselets = new HashMap<>();
    }

    // Token management methods

    /**
     * Sets a single token for the next parsing operation.
     *
     * @param token Token to set
     */
    public void setNextToken(LocatableToken token) {
        tokenSequence.clear();
        tokenSequence.add(token);
        tokenIndex = 0;
        endOfInput = false;

        if (mockTokenOps != null) {
            mockTokenOps.reset();
            mockTokenOps.addToken(token);
        }
    }

    /**
     * Sets a sequence of tokens for parsing operations.
     *
     * @param tokens Tokens to set
     */
    public void setTokenSequence(LocatableToken... tokens) {
        tokenSequence.clear();
        for (LocatableToken token : tokens) {
            tokenSequence.add(token);
        }
        tokenIndex = 0;
        endOfInput = false;

        if (mockTokenOps != null) {
            mockTokenOps.reset();
            mockTokenOps.addTokens(tokens);
        }
    }

    /**
     * Sets whether the parser is at the end of input.
     *
     * @param endOfInput true if at end of input
     */
    public void setEndOfInput(boolean endOfInput) {
        this.endOfInput = endOfInput;
    }

    // Parse result management

    /**
     * Sets the result to return from the next parseExpression call.
     *
     * @param result Parse result to return
     */
    public void setNextParseResult(ParsedNode result) {
        this.nextParseResult = result;
    }

    /**
     * Sets the result to return from the next parseExpression call.
     * Alias for setNextParseResult for test compatibility.
     *
     * @param result Parse result to return
     */
    public void setNextExpression(ParsedNode result) {
        setNextParseResult(result);
    }

    /**
     * Sets up the parser to return a failure result for the next parseExpression call.
     *
     * @param errorMessage Error message for the failure
     */
    public void setNextExpressionFailure(String errorMessage) {
        // For test purposes, we'll store the error and return null from nextParseResult
        // The parseExpressionResult method will detect this and return a failure
        this.nextParseResult = null;
        this.lastError = errorMessage;
        this.hasErrors = true;
        this.allErrors.add(errorMessage);
    }

    /**
     * Gets the precedence value from the last parseExpression call.
     *
     * @return Last precedence value
     */
    public int getLastParseExpressionPrecedence() {
        return lastParseExpressionPrecedence;
    }

    // Error tracking methods

    @Override
    public void error(String message, LocatableToken token) {
        hasErrors = true;
        lastError = message;
        allErrors.add(message);
        // Don't call super to avoid side effects in tests
    }

    /**
     * Checks if any errors have been reported.
     *
     * @return true if errors reported
     */
    public boolean hasErrors() {
        return hasErrors;
    }

    /**
     * Gets the last error message reported.
     *
     * @return Last error message
     */
    public String getLastError() {
        return lastError;
    }

    /**
     * Gets all error messages reported.
     *
     * @return List of all error messages
     */
    public List<String> getAllErrors() {
        return new ArrayList<>(allErrors);
    }

    /**
     * Resets the parser state for a new test.
     */
    public void reset() {
        hasErrors = false;
        lastError = "";
        allErrors.clear();
        nextParseResult = null;
        lastParseExpressionPrecedence = -1;
        tokenSequence.clear();
        tokenIndex = 0;
        endOfInput = false;
        prefixParselets.clear();
        infixParselets.clear();

        if (mockTokenOps != null) {
            mockTokenOps.reset();
        }
    }

    // Parser overrides for controlled behavior

    @Override
    public @NotNull LocatableToken peek() {
        if (endOfInput && tokenIndex >= tokenSequence.size()) {
            return createEOFToken();
        }
        if (tokenIndex < tokenSequence.size()) {
            return tokenSequence.get(tokenIndex);
        }
        return createEOFToken();
    }

    @Override
    public @NotNull LocatableToken consume() {
        if (tokenIndex < tokenSequence.size()) {
            return tokenSequence.get(tokenIndex++);
        }
        return createEOFToken();
    }

    @Override
    public ParsedNode parseExpression() {
        return parseExpression(0);
    }

    @Override
    public ParsedNode parseExpression(int minPrecedence) {
        ParseResult<ParsedNode> result = parseExpressionResult(minPrecedence);

        // Report any errors through the traditional mechanism
        for (ParseResult.ParseError error : result.getErrors()) {
            error(error.message(), error.token());
        }

        return result.getValueOrNull();
    }

    @Override
    public ParseResult<ParsedNode> parseExpressionResult(int precedence) {
        lastParseExpressionPrecedence = precedence;

        // Check if we're in a failure state from setNextExpressionFailure
        if (nextParseResult == null && hasErrors && !allErrors.isEmpty()) {
            LocatableToken currentToken = peek();
            return ParseResult.failure(lastError, currentToken);
        }

        // If a pre-configured result is set, return it as success
        if (nextParseResult != null) {
            ParsedNode result = nextParseResult;
            nextParseResult = null; // Clear it so it's only used once
            return ParseResult.success(result);
        }

        LocatableToken token = consume();
        if (token.getType() == JavaTokenTypes.EOF) {
            return ParseResult.failure("Unexpected end of input", token);
        }

        // Look up the prefix parselet for this token type
        PrefixParselet prefix = prefixParselets.get(token.getType());
        if (prefix == null) {
            // Fallback to simple expression parsing for backward compatibility
            ParsedNode node = parseSimpleLiteral(token);
            if (node != null) {
                return ParseResult.success(node);
            }
            return ParseResult.failure("Unexpected token: " + getTokenDescription(token), token);
        }

        // Parse the prefix expression using the registered parselet
        ParseResult<ParsedNode> leftResult = prefix.parse(this, token);
        if (leftResult.isFailure()) {
            return leftResult;
        }

        ParsedNode left = leftResult.getValue();
        List<ParseResult.ParseError> accumulatedErrors = new ArrayList<>(leftResult.getErrors());

        // Continue parsing infix expressions while precedence allows
        while (precedence < getCurrentPrecedence()) {
            token = consume();

            InfixParselet infix = infixParselets.get(token.getType());
            if (infix == null) {
                // No infix parselet found, push back and stop
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

        // Return result with accumulated errors
        if (accumulatedErrors.isEmpty()) {
            return ParseResult.success(left);
        } else {
            // Return success with warnings if we have a valid node
            return ParseResult.failure(accumulatedErrors);
        }
    }

    /**
     * Creates an EOF token with default position.
     *
     * @return EOF token
     */
    private static LocatableToken createEOFToken() {
        LineColPos pos = new LineColPos(1, 1, 0);
        return new LocatableToken(JavaTokenTypes.EOF, "", pos, pos);
    }

    /**
     * Gets the current token index.
     *
     * @return Current token index
     */
    public int getTokenIndex() {
        return tokenIndex;
    }

    /**
     * Gets the token sequence size.
     *
     * @return Number of tokens in sequence
     */
    public int getTokenSequenceSize() {
        return tokenSequence.size();
    }

    /**
     * Registers a parselet for a given token type.
     * Supports both prefix and infix parselets.
     *
     * @param tokenType Token type to register parselet for
     * @param parselet Parselet to register (can be PrefixParselet or InfixParselet)
     */
    public void registerParselet(int tokenType, Object parselet) {
        if (parselet instanceof PrefixParselet) {
            prefixParselets.put(tokenType, (PrefixParselet) parselet);
        }
        if (parselet instanceof InfixParselet) {
            infixParselets.put(tokenType, (InfixParselet) parselet);
        }
        // Note: Some parselets can be both prefix and infix (like LPAREN)
        // Changed from else-if to if to allow dual registration
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
        InfixParselet infix = infixParselets.get(token.getType());
        return infix != null ? infix.getPrecedence() : 0;
    }

    /**
     * Fallback method for parsing simple literals when no parselet is registered.
     * Maintains backward compatibility with existing tests.
     *
     * @param token Token to parse as literal
     * @return ParsedNode for the literal, or null if not a supported literal
     */
    private ParsedNode parseSimpleLiteral(LocatableToken token) {
        TestNodeFactory factory = (TestNodeFactory) getNodeFactory();
        
        if (token.getType() == JavaTokenTypes.NUM_INT ||
            token.getType() == JavaTokenTypes.STRING_LITERAL ||
            token.getType() == JavaTokenTypes.LITERAL_true ||
            token.getType() == JavaTokenTypes.LITERAL_false ||
            token.getType() == JavaTokenTypes.LITERAL_null) {
            return factory.createLiteralNode(token);
        } else if (token.getType() == JavaTokenTypes.IDENT) {
            return factory.createIdentifierNode(token);
        }
        return null;
    }

    /**
     * Gets a human-readable description of a token for error messages.
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

        return switch (tokenType) {
            case JavaTokenTypes.EOF -> "end of file";
            case JavaTokenTypes.IDENT -> "identifier '" + text + "'";
            case JavaTokenTypes.NUM_INT -> "integer literal '" + text + "'";
            case JavaTokenTypes.STRING_LITERAL -> "string literal";
            case JavaTokenTypes.LPAREN -> "'('";
            case JavaTokenTypes.RPAREN -> "')'";
            case JavaTokenTypes.COMMA -> "','";
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
     * Pushes a token back onto the token stream.
     * The token will be returned by the next call to consume().
     *
     * @param token The token to push back
     */
    public void pushBack(LocatableToken token) {
        if (tokenIndex > 0) {
            tokenIndex--;
            // For simplicity in MockParser, we assume the pushed back token
            // matches what we consumed. In a real implementation, this would
            // need more sophisticated token stream management.
        }
    }
}
