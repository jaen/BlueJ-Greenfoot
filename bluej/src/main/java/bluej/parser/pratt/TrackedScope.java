/*
 This file is part of the BlueJ program. 
 Copyright (C) 2024  Michael Kolling and John Rosenberg

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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A debugging wrapper that tracks callback scope entry and exit.
 * 
 * <p>TrackedScope implements AutoCloseable to work with try-with-resources,
 * ensuring that callback scopes are properly opened and closed. It provides
 * validation to detect scope mismatches and tracking for debugging purposes.</p>
 * 
 * <p>Key features:
 * <ul>
 *   <li>Automatic scope closure with try-with-resources</li>
 *   <li>Scope nesting depth tracking</li>
 *   <li>Entry/exit timing measurements</li>
 *   <li>Scope mismatch detection</li>
 *   <li>Debug logging support</li>
 *   <li>Thread-safe operation</li>
 * </ul>
 * 
 * <p>Usage example:
 * <pre>{@code
 * try (TrackedScope scope = new TrackedScope("expression", context, token)) {
 *     // Callback operations within this scope
 *     callbacks.beginExpression(token, false);
 *     // ... parse expression ...
 * } // Automatically validates and closes scope
 * }</pre>
 * 
 * @since BlueJ 5.4.0
 */
public class TrackedScope implements AutoCloseable {
    
    private static final Logger LOGGER = Logger.getLogger(TrackedScope.class.getName());
    
    /** Global counter for unique scope IDs */
    private static final AtomicInteger SCOPE_ID_COUNTER = new AtomicInteger(0);
    
    /** The unique ID for this scope instance */
    private final int scopeId;
    
    /** The name of this scope (e.g., "expression", "method_body") */
    private final String scopeName;
    
    /** The callback context managing this scope */
    private final CallbackContext context;
    
    /** The token where this scope begins (optional) */
    private final LocatableToken startToken;
    
    /** The nesting depth when this scope was created */
    private final int nestingDepth;
    
    /** Timestamp when scope was entered */
    private final long entryTime;
    
    /** The parent scope (null for root scopes) */
    private final TrackedScope parentScope;
    
    /** Thread that created this scope (for thread-safety validation) */
    private final Thread ownerThread;
    
    /** Flag indicating if this scope has been closed */
    private volatile boolean closed;
    
    /** Optional callback to execute on scope entry */
    private final Runnable onEntry;
    
    /** Optional callback to execute on scope exit */
    private final Runnable onExit;
    
    /** Exception that occurred within this scope (if any) */
    private volatile Throwable scopeException;
    
    /**
     * Creates a tracked scope with basic information.
     * 
     * @param scopeName The name of the scope
     * @param context The callback context managing scopes
     */
    public TrackedScope(String scopeName, CallbackContext context) {
        this(scopeName, context, null, null, null, null);
    }
    
    /**
     * Creates a tracked scope with a start token.
     * 
     * @param scopeName The name of the scope
     * @param context The callback context managing scopes
     * @param startToken The token where scope begins
     */
    public TrackedScope(String scopeName, CallbackContext context, LocatableToken startToken) {
        this(scopeName, context, null, startToken, null, null);
    }
    
    /**
     * Creates a tracked scope with a parent scope.
     * 
     * @param scopeName The name of the scope
     * @param context The callback context managing scopes
     * @param parentScope The parent scope (for nested scopes)
     */
    public TrackedScope(String scopeName, CallbackContext context, TrackedScope parentScope) {
        this(scopeName, context, parentScope, null, null, null);
    }
    
    /**
     * Creates a tracked scope with callbacks.
     * 
     * @param scopeName The name of the scope
     * @param context The callback context managing scopes
     * @param onEntry Callback to execute on scope entry
     * @param onExit Callback to execute on scope exit
     */
    public TrackedScope(String scopeName, CallbackContext context, 
                       Runnable onEntry, Runnable onExit) {
        this(scopeName, context, null, null, onEntry, onExit);
    }
    
    /**
     * Creates a fully configured tracked scope.
     * 
     * @param scopeName The name of the scope
     * @param context The callback context managing scopes
     * @param parentScope The parent scope (optional)
     * @param startToken The token where scope begins (optional)
     * @param onEntry Callback to execute on scope entry (optional)
     * @param onExit Callback to execute on scope exit (optional)
     */
    public TrackedScope(String scopeName, CallbackContext context, TrackedScope parentScope,
                       LocatableToken startToken, Runnable onEntry, Runnable onExit) {
        this.scopeId = SCOPE_ID_COUNTER.incrementAndGet();
        this.scopeName = Objects.requireNonNull(scopeName, "Scope name cannot be null");
        this.context = Objects.requireNonNull(context, "Callback context cannot be null");
        this.parentScope = parentScope;
        this.startToken = startToken;
        this.onEntry = onEntry;
        this.onExit = onExit;
        this.ownerThread = Thread.currentThread();
        this.entryTime = System.nanoTime();
        this.closed = false;
        
        // Calculate nesting depth
        this.nestingDepth = (parentScope != null) ? parentScope.nestingDepth + 1 : 0;
        
        // Register with context
        registerScope();
        
        // Execute entry callback
        if (onEntry != null) {
            try {
                onEntry.run();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Entry callback failed for scope: " + scopeName, e);
            }
        }
        
        // Log entry if debugging
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(formatScopeEntry());
        }
    }
    
    /**
     * Registers this scope with the callback context.
     */
    private void registerScope() {
        try {
            // This will be implemented in CallbackContext
            // For now, just validate nesting depth
            if (nestingDepth > getMaxNestingDepth()) {
                throw CallbackIntegrationException.nestingOverflow(
                    getMaxNestingDepth(), nestingDepth, scopeName);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to register scope: " + scopeName, e);
            throw e;
        }
    }
    
    /**
     * Closes this scope, performing validation and cleanup.
     */
    @Override
    public void close() {
        if (closed) {
            LOGGER.warning("Scope already closed: " + formatScope());
            return;
        }
        
        // Validate thread safety
        if (Thread.currentThread() != ownerThread) {
            throw new IllegalStateException("Scope closed from different thread: " + formatScope());
        }
        
        try {
            // Execute exit callback
            if (onExit != null) {
                try {
                    onExit.run();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Exit callback failed for scope: " + scopeName, e);
                    recordException(e);
                }
            }
            
            // Calculate duration
            long duration = System.nanoTime() - entryTime;
            
            // Log exit if debugging
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(formatScopeExit(duration));
            }
            
            // Log performance warning for slow scopes
            if (duration > getSlowScopeThreshold()) {
                LOGGER.warning(String.format("Slow scope detected: %s took %d ms",
                    scopeName, duration / 1_000_000));
            }
            
            // Notify context of scope closure
            notifyContextClosure();
            
        } finally {
            closed = true;
        }
    }
    
    /**
     * Notifies the callback context that this scope is closing.
     */
    private void notifyContextClosure() {
        try {
            // This will be implemented in CallbackContext
            // It should validate that this is the expected scope to close
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to notify context of scope closure: " + scopeName, e);
            throw e;
        }
    }
    
    /**
     * Records an exception that occurred within this scope.
     * 
     * @param exception The exception to record
     */
    public void recordException(Throwable exception) {
        this.scopeException = exception;
        LOGGER.log(Level.WARNING, "Exception in scope " + scopeName, exception);
    }
    
    /**
     * Validates that the given scope matches this scope.
     * 
     * @param other The scope to validate against
     * @throws CallbackIntegrationException if scopes don't match
     */
    public void validateMatch(TrackedScope other) {
        if (other == null) {
            throw CallbackIntegrationException.scopeMismatch(
                scopeName, "null", startToken);
        }
        
        if (!this.scopeName.equals(other.scopeName)) {
            throw CallbackIntegrationException.scopeMismatch(
                this.scopeName, other.scopeName, startToken);
        }
        
        if (this.scopeId != other.scopeId) {
            throw new CallbackIntegrationException(
                "Scope ID mismatch: expected " + this.scopeId + " but got " + other.scopeId,
                CallbackIntegrationException.FailureType.SCOPE_MISMATCH,
                scopeName);
        }
    }
    
    /**
     * Checks if this scope is properly nested within the given parent.
     * 
     * @param expectedParent The expected parent scope
     * @return true if properly nested
     */
    public boolean isNestedWithin(TrackedScope expectedParent) {
        if (expectedParent == null) {
            return parentScope == null;
        }
        return parentScope != null && parentScope.scopeId == expectedParent.scopeId;
    }
    
    /**
     * Creates a child scope nested within this scope.
     * 
     * @param childScopeName The name of the child scope
     * @return The new child scope
     */
    public TrackedScope createChildScope(String childScopeName) {
        return createChildScope(childScopeName, null);
    }
    
    /**
     * Creates a child scope with a start token.
     * 
     * @param childScopeName The name of the child scope
     * @param childStartToken The start token for the child
     * @return The new child scope
     */
    public TrackedScope createChildScope(String childScopeName, LocatableToken childStartToken) {
        if (closed) {
            throw new IllegalStateException("Cannot create child scope on closed parent: " + scopeName);
        }
        return new TrackedScope(childScopeName, context, this, childStartToken, null, null);
    }
    
    // Formatting methods for debugging
    
    private String formatScope() {
        return String.format("TrackedScope[id=%d, name=%s, depth=%d]",
            scopeId, scopeName, nestingDepth);
    }
    
    private String formatScopeEntry() {
        StringBuilder sb = new StringBuilder();
        sb.append(getIndent()).append("→ Entering ").append(formatScope());
        if (startToken != null) {
            sb.append(" at ").append(startToken.getLine()).append(":").append(startToken.getColumn());
        }
        return sb.toString();
    }
    
    private String formatScopeExit(long durationNanos) {
        return String.format("%s← Exiting %s (duration: %.2f ms)",
            getIndent(), formatScope(), durationNanos / 1_000_000.0);
    }
    
    private String getIndent() {
        return "  ".repeat(nestingDepth);
    }
    
    // Configuration methods (can be overridden or configured)
    
    /**
     * Gets the maximum allowed nesting depth.
     * 
     * @return The maximum nesting depth
     */
    protected int getMaxNestingDepth() {
        return 100; // Reasonable default, can be configured
    }
    
    /**
     * Gets the threshold for slow scope warnings (in nanoseconds).
     * 
     * @return The slow scope threshold
     */
    protected long getSlowScopeThreshold() {
        return 100_000_000L; // 100ms default
    }
    
    // Getters for diagnostic information
    
    public int getScopeId() {
        return scopeId;
    }
    
    public String getScopeName() {
        return scopeName;
    }
    
    public LocatableToken getStartToken() {
        return startToken;
    }
    
    public int getNestingDepth() {
        return nestingDepth;
    }
    
    public long getEntryTime() {
        return entryTime;
    }
    
    public TrackedScope getParentScope() {
        return parentScope;
    }
    
    public boolean isClosed() {
        return closed;
    }
    
    public Throwable getScopeException() {
        return scopeException;
    }
    
    public boolean hasException() {
        return scopeException != null;
    }
    
    /**
     * Gets the duration this scope has been open (in nanoseconds).
     * 
     * @return The duration, or -1 if scope is closed
     */
    public long getDuration() {
        if (closed) {
            return -1;
        }
        return System.nanoTime() - entryTime;
    }
    
    @Override
    public String toString() {
        return formatScope();
    }
}