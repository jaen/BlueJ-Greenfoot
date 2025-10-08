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

import bluej.parser.CallbackDelegate;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.ParsedNode;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central coordinator for safe callback management in the Pratt parser.
 *
 * <p>SafeCallbacks serves as the primary interface between parselets and the
 * CallbackDelegate, ensuring proper callback pairing and scope management.
 * It uses TrackedScope internally to provide automatic cleanup and validation
 * through the try-with-resources pattern.</p>
 *
 * <p>Key architectural principles:</p>
 * <ul>
 *   <li>SafeCallbacks is the central coordinator (no separate CallbackContext)</li>
 *   <li>Parselets emit callbacks using try-with-resources pattern</li>
 *   <li>Callbacks create AST nodes, not parselets</li>
 *   <li>Use TrackedScope and CallbackIntegrationException for error handling</li>
 * </ul>
 *
 * <p>Usage pattern for parselets:</p>
 * <pre>{@code
 * public ParseResult parse(KotlinPrattParser parser, Token token) {
 *     try (TrackedScope scope = parser.getCallbacks().beginLiteralExpression(token)) {
 *         // Parse logic here
 *         // Let callback create the actual AST node
 *         return scope.createResult(token);
 *     } catch (Exception e) {
 *         throw CallbackIntegrationException.parsingFailed(token, e);
 *     }
 * }
 * }</pre>
 *
 * @author BlueJ Team
 * @since BlueJ 5.4.0
 */
public class SafeCallbacks {

    private static final Logger LOGGER = Logger.getLogger(SafeCallbacks.class.getName());

    /** The underlying callback delegate for IDE integration */
    private final CallbackDelegate delegate;

    /** The node factory for creating AST nodes */
    private final NodeFactory nodeFactory;

    /**
     * Creates a new SafeCallbacks instance with the specified delegate and node factory.
     *
     * @param delegate The callback delegate for IDE integration
     * @param nodeFactory The factory for creating AST nodes
     */
    public SafeCallbacks(@NotNull CallbackDelegate delegate, @NotNull NodeFactory nodeFactory) {
        this.delegate = Objects.requireNonNull(delegate, "CallbackDelegate cannot be null");
        this.nodeFactory = Objects.requireNonNull(nodeFactory, "NodeFactory cannot be null");
    }

    /**
     * Begins a literal expression scope with automatic callback management.
     *
     * <p>This method creates a TrackedScope that automatically calls
     * {@link CallbackDelegate#beginExpression(LocatableToken, boolean)} on entry
     * and {@link CallbackDelegate#endExpression(LocatableToken, boolean)} on exit.</p>
     *
     * @param token The literal token starting the expression
     * @return A TrackedScope for use with try-with-resources
     */
    public TrackedScope beginLiteralExpression(@NotNull LocatableToken token) {
        return createExpressionScope("literal", token, () -> {
            delegate.beginExpression(token, false);
            delegate.gotLiteral(token);
        }, () -> {
            delegate.endExpression(token, false);
        });
    }

    /**
     * Begins an identifier expression scope with automatic callback management.
     *
     * @param token The identifier token starting the expression
     * @return A TrackedScope for use with try-with-resources
     */
    public TrackedScope beginIdentifierExpression(@NotNull LocatableToken token) {
        return createExpressionScope("identifier", token, () -> {
            delegate.beginExpression(token, false);
            delegate.gotIdentifier(token);
        }, () -> {
            delegate.endExpression(token, false);
        });
    }

    /**
     * Begins a 'this' reference expression scope with automatic callback management.
     *
     * @param token The 'this' keyword token
     * @return A TrackedScope for use with try-with-resources
     */
    public TrackedScope beginThisExpression(@NotNull LocatableToken token) {
        return createExpressionScope("this", token, () -> {
            delegate.beginExpression(token, false);
            delegate.gotIdentifier(token);
        }, () -> {
            delegate.endExpression(token, false);
        });
    }

    /**
     * Begins a 'super' reference expression scope with automatic callback management.
     *
     * @param token The 'super' keyword token
     * @return A TrackedScope for use with try-with-resources
     */
    public TrackedScope beginSuperExpression(@NotNull LocatableToken token) {
        return createExpressionScope("super", token, () -> {
            delegate.beginExpression(token, false);
            delegate.gotIdentifier(token);
        }, () -> {
            delegate.endExpression(token, false);
        });
    }

    /**
     * Creates a tracked scope with the specified callbacks.
     *
     * @param scopeName The name of the scope for debugging
     * @param token The token starting the scope
     * @param onEntry Callback to execute on scope entry
     * @param onExit Callback to execute on scope exit
     * @return A TrackedScope for use with try-with-resources
     */
    private TrackedScope createExpressionScope(String scopeName, LocatableToken token,
                                             Runnable onEntry, Runnable onExit) {
        // Wrap callbacks with error handling
        Runnable safeOnEntry = () -> {
            try {
                onEntry.run();
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Started " + scopeName + " expression at " + 
                               token.getLine() + ":" + token.getColumn());
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to begin " + scopeName + " expression", e);
                throw new CallbackIntegrationException(
                    "Failed to begin " + scopeName + " expression: " + e.getMessage(),
                    CallbackIntegrationException.FailureType.CALLBACK_EXCEPTION,
                    scopeName, token);
            }
        };

        Runnable safeOnExit = () -> {
            try {
                onExit.run();
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Ended " + scopeName + " expression");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to end " + scopeName + " expression", e);
                throw new CallbackIntegrationException(
                    "Failed to end " + scopeName + " expression: " + e.getMessage(),
                    CallbackIntegrationException.FailureType.CALLBACK_EXCEPTION,
                    scopeName, token);
            }
        };

        // Create the tracked scope
        return new SafeCallbackScope(scopeName, token, safeOnEntry, safeOnExit);
    }

    /**
     * Gets the underlying callback delegate.
     *
     * @return The callback delegate
     */
    public CallbackDelegate getDelegate() {
        return delegate;
    }

    /**
     * Gets the node factory for creating AST nodes.
     *
     * @return The node factory
     */
    public NodeFactory getNodeFactory() {
        return nodeFactory;
    }

    /**
     * TrackedScope implementation that provides callback management
     * and convenient methods for creating ParseResults.
     */
    private class SafeCallbackScope extends TrackedScope {
        
        private final Runnable exitCallback;

        public SafeCallbackScope(String scopeName, LocatableToken startToken,
                                Runnable onEntry, Runnable onExit) {
            super(scopeName, new CallbackContext(), startToken);
            this.exitCallback = onExit;
            
            // Execute entry callback immediately
            if (onEntry != null) {
                onEntry.run();
            }
        }

        /**
         * Creates a ParseResult with a literal node.
         *
         * @param token The token for node creation
         * @return A successful ParseResult with the created node
         */
        public ParseResult<ParsedNode> createResult(@NotNull LocatableToken token) {
            try {
                ParsedNode node = nodeFactory.createLiteralNode(token);
                return ParseResult.success(node);
            } catch (Exception e) {
                throw new CallbackIntegrationException(
                    "Failed to create AST node: " + e.getMessage(),
                    CallbackIntegrationException.FailureType.CALLBACK_EXCEPTION,
                    scopeName, token);
            }
        }

        /**
         * Creates a ParseResult for identifier nodes.
         *
         * @param token The identifier token
         * @return A successful ParseResult with the created node
         */
        public ParseResult<ParsedNode> createIdentifierResult(@NotNull LocatableToken token) {
            try {
                ParsedNode node = nodeFactory.createIdentifierNode(token);
                return ParseResult.success(node);
            } catch (Exception e) {
                throw new CallbackIntegrationException(
                    "Failed to create identifier node: " + e.getMessage(),
                    CallbackIntegrationException.FailureType.CALLBACK_EXCEPTION,
                    scopeName, token);
            }
        }

        /**
         * Creates a ParseResult for 'this' reference nodes.
         *
         * @param token The 'this' token
         * @return A successful ParseResult with the created node
         */
        public ParseResult<ParsedNode> createThisResult(@NotNull LocatableToken token) {
            try {
                ParsedNode node = nodeFactory.createThisNode(token);
                return ParseResult.success(node);
            } catch (Exception e) {
                throw new CallbackIntegrationException(
                    "Failed to create 'this' node: " + e.getMessage(),
                    CallbackIntegrationException.FailureType.CALLBACK_EXCEPTION,
                    scopeName, token);
            }
        }

        /**
         * Creates a ParseResult for 'super' reference nodes.
         *
         * @param token The 'super' token
         * @return A successful ParseResult with the created node
         */
        public ParseResult<ParsedNode> createSuperResult(@NotNull LocatableToken token) {
            try {
                ParsedNode node = nodeFactory.createSuperNode(token);
                return ParseResult.success(node);
            } catch (Exception e) {
                throw new CallbackIntegrationException(
                    "Failed to create 'super' node: " + e.getMessage(),
                    CallbackIntegrationException.FailureType.CALLBACK_EXCEPTION,
                    scopeName, token);
            }
        }

        @Override
        public void close() {
            try {
                // Execute exit callback
                if (exitCallback != null) {
                    exitCallback.run();
                }
            } finally {
                super.close();
            }
        }
    }

    @Override
    public String toString() {
        return "SafeCallbacks[delegate=" + delegate.getClass().getSimpleName() + "]";
    }
}