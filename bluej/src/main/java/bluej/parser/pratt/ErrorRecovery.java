/*
 This file is part of the BlueJ program.
 Copyright (C) 1999-2009,2010,2011,2012,2014,2016,2017,2018,2019,2021,2022,2023,2024  Michael Kolling and John Rosenberg

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

import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/**
 * Error recovery mechanism for the Kotlin Pratt parser.
 *
 * <p>This class provides sophisticated error recovery capabilities that allow
 * the parser to continue parsing after encountering syntax errors. It implements
 * panic mode recovery by skipping tokens until reaching predefined synchronization
 * points where parsing can safely resume.</p>
 *
 * <p><strong>Key Features:</strong></p>
 * <ul>
 *   <li>Multiple synchronization token sets for different parsing contexts</li>
 *   <li>Intelligent recovery point selection based on parsing state</li>
 *   <li>Error cascade prevention through recovery state tracking</li>
 *   <li>Configurable recovery strategies for different error types</li>
 * </ul>
 *
 * <p><strong>Recovery Strategy:</strong></p>
 * <ol>
 *   <li>When an error occurs, report it with contextual information</li>
 *   <li>Enter recovery mode to prevent error cascades</li>
 *   <li>Skip tokens until reaching an appropriate synchronization point</li>
 *   <li>Exit recovery mode and resume normal parsing</li>
 * </ol>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * ErrorRecovery recovery = new ErrorRecovery();
 *
 * // When an error occurs during expression parsing:
 * recovery.reportError(parser, "Expected expression", token, RecoveryContext.EXPRESSION);
 * if (recovery.shouldAttemptRecovery()) {
 *     recovery.recoverToSynchronizationPoint(parser, RecoveryContext.EXPRESSION);
 * }
 * }</pre>
 *
 * @author BlueJ Kotlin Implementation
 * @since 2025-01-24
 */
public class ErrorRecovery {

    /**
     * Defines different contexts for error recovery, each with its own
     * set of appropriate synchronization tokens.
     */
    public enum RecoveryContext {
        /** Recovery context for expression parsing errors */
        EXPRESSION,
        /** Recovery context for statement parsing errors */
        STATEMENT,
        /** Recovery context for declaration parsing errors */
        DECLARATION,
        /** Recovery context for parameter list parsing errors */
        PARAMETER_LIST,
        /** Recovery context for argument list parsing errors */
        ARGUMENT_LIST,
        /** Recovery context for block parsing errors */
        BLOCK,
        /** Recovery context for type parsing errors */
        TYPE,
        /** General recovery context when specific context is unknown */
        GENERAL
    }

    /**
     * Recovery strategy that determines how aggressive the recovery should be.
     */
    public enum RecoveryStrategy {
        /** Conservative recovery - minimal token skipping */
        CONSERVATIVE,
        /** Aggressive recovery - skip more tokens to find solid synchronization points */
        AGGRESSIVE,
        /** Adaptive recovery - adjust strategy based on error patterns */
        ADAPTIVE
    }

    // ===== SYNCHRONIZATION TOKEN SETS =====

    /** Tokens that can serve as expression synchronization points */
    private static final Set<Integer> EXPRESSION_SYNC_TOKENS = new HashSet<>(Arrays.asList(
        JavaTokenTypes.SEMI,           // Statement terminator
        JavaTokenTypes.COMMA,          // Expression separator
        JavaTokenTypes.RPAREN,         // End of parenthesized expression
        JavaTokenTypes.RBRACK,         // End of array access
        JavaTokenTypes.RCURLY,         // End of block
        JavaTokenTypes.EOF             // End of input
    ));

    /** Tokens that can serve as statement synchronization points */
    private static final Set<Integer> STATEMENT_SYNC_TOKENS = new HashSet<>(Arrays.asList(
        JavaTokenTypes.SEMI,           // Statement terminator
        JavaTokenTypes.LCURLY,         // Block start
        JavaTokenTypes.RCURLY,         // Block end
        JavaTokenTypes.LITERAL_if,     // Control flow keywords
        JavaTokenTypes.LITERAL_while,
        JavaTokenTypes.LITERAL_for,
        JavaTokenTypes.LITERAL_when,
        JavaTokenTypes.LITERAL_try,
        JavaTokenTypes.LITERAL_fun,    // Declaration keywords
        JavaTokenTypes.LITERAL_val,
        JavaTokenTypes.LITERAL_var,
        JavaTokenTypes.LITERAL_class,
        JavaTokenTypes.EOF             // End of input
    ));

    /** Tokens that can serve as declaration synchronization points */
    private static final Set<Integer> DECLARATION_SYNC_TOKENS = new HashSet<>(Arrays.asList(
        JavaTokenTypes.LITERAL_fun,    // Function declaration
        JavaTokenTypes.LITERAL_val,    // Value declaration
        JavaTokenTypes.LITERAL_var,    // Variable declaration
        JavaTokenTypes.LITERAL_class,  // Class declaration
        JavaTokenTypes.LITERAL_interface, // Interface declaration
        JavaTokenTypes.LITERAL_enum,   // Enum declaration
        JavaTokenTypes.LITERAL_object, // Object declaration
        JavaTokenTypes.RCURLY,         // End of containing block
        JavaTokenTypes.EOF             // End of input
    ));

    /** Tokens that can serve as parameter/argument list synchronization points */
    private static final Set<Integer> LIST_SYNC_TOKENS = new HashSet<>(Arrays.asList(
        JavaTokenTypes.COMMA,          // Parameter/argument separator
        JavaTokenTypes.RPAREN,         // End of parameter list
        JavaTokenTypes.RBRACK,         // End of array/index list
        JavaTokenTypes.RCURLY,         // End of block (escape hatch)
        JavaTokenTypes.EOF             // End of input
    ));

    /** Tokens that can serve as block synchronization points */
    private static final Set<Integer> BLOCK_SYNC_TOKENS = new HashSet<>(Arrays.asList(
        JavaTokenTypes.RCURLY,         // End of current block
        JavaTokenTypes.LCURLY,         // Start of nested block
        JavaTokenTypes.EOF             // End of input
    ));

    /** Tokens that can serve as type synchronization points */
    private static final Set<Integer> TYPE_SYNC_TOKENS = new HashSet<>(Arrays.asList(
        JavaTokenTypes.COMMA,          // Type parameter separator
        JavaTokenTypes.GT,             // End of type parameter list
        JavaTokenTypes.RPAREN,         // End of function type
        JavaTokenTypes.ASSIGN,         // Assignment after type
        JavaTokenTypes.LCURLY,         // Start of block after type
        JavaTokenTypes.EOF             // End of input
    ));

    /** General synchronization tokens for unknown contexts */
    private static final Set<Integer> GENERAL_SYNC_TOKENS = new HashSet<>(Arrays.asList(
        JavaTokenTypes.SEMI,           // Statement boundaries
        JavaTokenTypes.LCURLY,         // Block boundaries
        JavaTokenTypes.RCURLY,
        JavaTokenTypes.LITERAL_fun,    // Major declaration keywords
        JavaTokenTypes.LITERAL_class,
        JavaTokenTypes.EOF             // End of input
    ));

    // ===== INSTANCE STATE =====

    /** Current recovery strategy being used */
    private RecoveryStrategy strategy = RecoveryStrategy.ADAPTIVE;

    /** Whether the parser is currently in recovery mode */
    private boolean inRecoveryMode = false;

    /** Number of consecutive errors encountered */
    private int consecutiveErrors = 0;

    /** Maximum number of tokens to skip during recovery */
    private int maxTokensToSkip = 50;

    /** Number of errors recovered from in this parsing session */
    private int recoveryCount = 0;

    /** Maximum number of recovery attempts before giving up */
    private int maxRecoveryAttempts = 10;

    // ===== PUBLIC INTERFACE =====

    /**
     * Reports an error and initiates recovery if appropriate.
     *
     * @param parser The parser instance
     * @param message Descriptive error message
     * @param token The token associated with the error (may be null)
     * @param context The parsing context where the error occurred
     */
    public void reportError(KotlinPrattParser parser, String message, LocatableToken token, RecoveryContext context) {
        // Report the actual error
        parser.error(message, token);

        // Update recovery state
        consecutiveErrors++;

        // Enter recovery mode to prevent error cascades
        if (!inRecoveryMode) {
            inRecoveryMode = true;
        }

        // Adjust strategy based on error patterns
        adjustRecoveryStrategy();
    }

    /**
     * Determines whether recovery should be attempted based on current state.
     *
     * @return true if recovery should be attempted, false if parsing should abort
     */
    public boolean shouldAttemptRecovery() {
        return recoveryCount < maxRecoveryAttempts && inRecoveryMode;
    }

    /**
     * Attempts to recover by skipping tokens until a synchronization point is reached.
     *
     * @param parser The parser instance
     * @param context The recovery context to determine appropriate synchronization tokens
     * @return true if recovery was successful and parsing can continue, false otherwise
     */
    public boolean recoverToSynchronizationPoint(KotlinPrattParser parser, RecoveryContext context) {
        if (!shouldAttemptRecovery()) {
            return false;
        }

        Set<Integer> syncTokens = getSynchronizationTokens(context);
        return performRecovery(parser, syncTokens, context);
    }

    /**
     * Exits recovery mode, typically called after successful parsing of a construct.
     */
    public void exitRecoveryMode() {
        if (inRecoveryMode) {
            inRecoveryMode = false;
            consecutiveErrors = 0;
        }
    }

    /**
     * Checks if the parser is currently in recovery mode.
     *
     * @return true if in recovery mode, false otherwise
     */
    public boolean isInRecoveryMode() {
        return inRecoveryMode;
    }

    /**
     * Resets the error recovery state for a new parsing session.
     */
    public void reset() {
        inRecoveryMode = false;
        consecutiveErrors = 0;
        recoveryCount = 0;
        strategy = RecoveryStrategy.ADAPTIVE;
    }

    /**
     * Configures the recovery strategy to use.
     *
     * @param newStrategy The recovery strategy to adopt
     */
    public void setRecoveryStrategy(RecoveryStrategy newStrategy) {
        this.strategy = newStrategy;
    }

    /**
     * Sets the maximum number of tokens to skip during recovery.
     *
     * @param maxTokens The maximum token skip limit
     */
    public void setMaxTokensToSkip(int maxTokens) {
        this.maxTokensToSkip = Math.max(1, maxTokens);
    }

    /**
     * Gets the current number of recovery attempts made.
     *
     * @return The recovery attempt count
     */
    public int getRecoveryCount() {
        return recoveryCount;
    }

    // ===== PRIVATE IMPLEMENTATION =====

    /**
     * Retrieves the appropriate synchronization tokens for the given context.
     *
     * @param context The recovery context
     * @return Set of token types that can serve as synchronization points
     */
    private Set<Integer> getSynchronizationTokens(RecoveryContext context) {
        switch (context) {
            case EXPRESSION:
                return EXPRESSION_SYNC_TOKENS;
            case STATEMENT:
                return STATEMENT_SYNC_TOKENS;
            case DECLARATION:
                return DECLARATION_SYNC_TOKENS;
            case PARAMETER_LIST:
            case ARGUMENT_LIST:
                return LIST_SYNC_TOKENS;
            case BLOCK:
                return BLOCK_SYNC_TOKENS;
            case TYPE:
                return TYPE_SYNC_TOKENS;
            case GENERAL:
            default:
                return GENERAL_SYNC_TOKENS;
        }
    }

    /**
     * Performs the actual token skipping recovery.
     *
     * @param parser The parser instance
     * @param syncTokens Set of synchronization tokens to look for
     * @param context The recovery context for logging
     * @return true if a synchronization point was found, false otherwise
     */
    private boolean performRecovery(KotlinPrattParser parser, Set<Integer> syncTokens, RecoveryContext context) {
        int tokensSkipped = 0;

        while (tokensSkipped < maxTokensToSkip) {
            LocatableToken currentToken = parser.peek();

            // If we've reached end of input, recovery failed
            if (currentToken == null || currentToken.getType() == JavaTokenTypes.EOF) {
                break;
            }

            // If we found a synchronization token, recovery succeeded
            if (syncTokens.contains(currentToken.getType())) {
                recoveryCount++;
                return true;
            }

            // Skip this token and continue
            parser.consume();
            tokensSkipped++;

            // Apply strategy-specific optimizations
            if (shouldStopRecovery(currentToken, tokensSkipped)) {
                break;
            }
        }

        // Recovery failed - couldn't find a synchronization point
        return false;
    }

    /**
     * Determines if recovery should stop based on strategy and current state.
     *
     * @param currentToken The token currently being considered
     * @param tokensSkipped Number of tokens skipped so far
     * @return true if recovery should stop, false to continue
     */
    private boolean shouldStopRecovery(LocatableToken currentToken, int tokensSkipped) {
        switch (strategy) {
            case CONSERVATIVE:
                // Stop early if we've skipped a few tokens
                return tokensSkipped > maxTokensToSkip / 4;

            case AGGRESSIVE:
                // Keep going until we hit the limit
                return false;

            case ADAPTIVE:
                // Stop if we encounter certain "dangerous" tokens that might indicate
                // we've gone too far
                int tokenType = currentToken.getType();
                return tokensSkipped > maxTokensToSkip / 2 &&
                       (tokenType == JavaTokenTypes.LCURLY || tokenType == JavaTokenTypes.RCURLY);

            default:
                return false;
        }
    }

    /**
     * Adjusts the recovery strategy based on error patterns.
     */
    private void adjustRecoveryStrategy() {
        if (strategy == RecoveryStrategy.ADAPTIVE) {
            // If we're seeing many consecutive errors, become more aggressive
            if (consecutiveErrors > 3) {
                // Temporarily use aggressive strategy
                strategy = RecoveryStrategy.AGGRESSIVE;
            } else if (consecutiveErrors == 1 && recoveryCount > 5) {
                // If we've had many recoveries but few consecutive errors,
                // try being more conservative
                strategy = RecoveryStrategy.CONSERVATIVE;
            }
        }
    }

    /**
     * Creates a context-specific error message for better diagnostics.
     *
     * @param baseMessage The base error message
     * @param context The recovery context
     * @return Enhanced error message with context information
     */
    public static String createContextualErrorMessage(String baseMessage, RecoveryContext context) {
        String contextInfo;
        switch (context) {
            case EXPRESSION:
                contextInfo = "in expression";
                break;
            case STATEMENT:
                contextInfo = "in statement";
                break;
            case DECLARATION:
                contextInfo = "in declaration";
                break;
            case PARAMETER_LIST:
                contextInfo = "in parameter list";
                break;
            case ARGUMENT_LIST:
                contextInfo = "in argument list";
                break;
            case BLOCK:
                contextInfo = "in block";
                break;
            case TYPE:
                contextInfo = "in type specification";
                break;
            case GENERAL:
            default:
                contextInfo = "";
                break;
        }

        return contextInfo.isEmpty() ? baseMessage : baseMessage + " " + contextInfo;
    }

    /**
     * Determines if a token is generally safe to use as a synchronization point.
     *
     * @param tokenType The token type to check
     * @return true if this token is a good synchronization candidate
     */
    public static boolean isSynchronizationCandidate(int tokenType) {
        return GENERAL_SYNC_TOKENS.contains(tokenType) ||
               STATEMENT_SYNC_TOKENS.contains(tokenType) ||
               DECLARATION_SYNC_TOKENS.contains(tokenType);
    }
}
