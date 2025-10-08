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

/**
 * Custom exception for callback integration errors in the Pratt parser.
 * 
 * <p>This exception is thrown when callback coordination fails, scopes are
 * mismanaged, or when the callback sequence violates expected patterns.
 * It provides detailed context about the failure including the callback
 * context, scope information, and token location.</p>
 * 
 * <p>Key failure scenarios include:
 * <ul>
 *   <li>Scope mismatch - callbacks closed out of order</li>
 *   <li>Missing callback implementation</li>
 *   <li>Callback sequence violations</li>
 *   <li>State corruption during callback processing</li>
 *   <li>Resource cleanup failures</li>
 * </ul>
 * 
 * @since BlueJ 5.4.0
 */
public class CallbackIntegrationException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /** The type of callback failure */
    private final FailureType failureType;
    
    /** The callback scope where the failure occurred (optional) */
    private final String scopeName;
    
    /** The token at which the failure occurred (optional) */
    private final LocatableToken failureToken;
    
    /** Additional context information (optional) */
    private final String contextInfo;
    
    /**
     * Enumeration of callback failure types.
     */
    public enum FailureType {
        /** Scope was not closed properly or closed out of order */
        SCOPE_MISMATCH("Callback scope mismatch"),
        
        /** Required callback implementation was missing */
        MISSING_CALLBACK("Missing callback implementation"),
        
        /** Callback sequence violated expected pattern */
        SEQUENCE_VIOLATION("Callback sequence violation"),
        
        /** Callback state became corrupted */
        STATE_CORRUPTION("Callback state corruption"),
        
        /** Resource cleanup failed */
        CLEANUP_FAILURE("Resource cleanup failure"),
        
        /** Callback threw an unexpected exception */
        CALLBACK_EXCEPTION("Callback threw exception"),
        
        /** Callback context was in invalid state */
        INVALID_CONTEXT("Invalid callback context"),
        
        /** Nested scope limit exceeded */
        NESTING_OVERFLOW("Scope nesting overflow");
        
        private final String description;
        
        FailureType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Creates a basic callback integration exception.
     * 
     * @param message The error message
     * @param failureType The type of failure
     */
    public CallbackIntegrationException(String message, FailureType failureType) {
        super(message);
        this.failureType = failureType;
        this.scopeName = null;
        this.failureToken = null;
        this.contextInfo = null;
    }
    
    /**
     * Creates a callback integration exception with scope information.
     * 
     * @param message The error message
     * @param failureType The type of failure
     * @param scopeName The name of the scope where failure occurred
     */
    public CallbackIntegrationException(String message, FailureType failureType, String scopeName) {
        super(formatMessage(message, failureType, scopeName, null));
        this.failureType = failureType;
        this.scopeName = scopeName;
        this.failureToken = null;
        this.contextInfo = null;
    }
    
    /**
     * Creates a callback integration exception with token location.
     * 
     * @param message The error message
     * @param failureType The type of failure
     * @param scopeName The name of the scope where failure occurred
     * @param failureToken The token at the failure location
     */
    public CallbackIntegrationException(String message, FailureType failureType, 
                                       String scopeName, LocatableToken failureToken) {
        super(formatMessage(message, failureType, scopeName, failureToken));
        this.failureType = failureType;
        this.scopeName = scopeName;
        this.failureToken = failureToken;
        this.contextInfo = null;
    }
    
    /**
     * Creates a full callback integration exception with all context.
     * 
     * @param message The error message
     * @param failureType The type of failure
     * @param scopeName The name of the scope where failure occurred
     * @param failureToken The token at the failure location
     * @param contextInfo Additional context information
     */
    public CallbackIntegrationException(String message, FailureType failureType,
                                       String scopeName, LocatableToken failureToken,
                                       String contextInfo) {
        super(formatMessage(message, failureType, scopeName, failureToken, contextInfo));
        this.failureType = failureType;
        this.scopeName = scopeName;
        this.failureToken = failureToken;
        this.contextInfo = contextInfo;
    }
    
    /**
     * Creates a callback integration exception with a cause.
     * 
     * @param message The error message
     * @param failureType The type of failure
     * @param cause The underlying cause
     */
    public CallbackIntegrationException(String message, FailureType failureType, Throwable cause) {
        super(message, cause);
        this.failureType = failureType;
        this.scopeName = null;
        this.failureToken = null;
        this.contextInfo = null;
    }
    
    /**
     * Creates a callback integration exception with scope and cause.
     * 
     * @param message The error message
     * @param failureType The type of failure
     * @param scopeName The name of the scope where failure occurred
     * @param cause The underlying cause
     */
    public CallbackIntegrationException(String message, FailureType failureType,
                                       String scopeName, Throwable cause) {
        super(formatMessage(message, failureType, scopeName, null), cause);
        this.failureType = failureType;
        this.scopeName = scopeName;
        this.failureToken = null;
        this.contextInfo = null;
    }
    
    /**
     * Formats the exception message with context information.
     */
    private static String formatMessage(String message, FailureType failureType,
                                       String scopeName, LocatableToken token) {
        return formatMessage(message, failureType, scopeName, token, null);
    }
    
    /**
     * Formats the exception message with full context information.
     */
    private static String formatMessage(String message, FailureType failureType,
                                       String scopeName, LocatableToken token, String contextInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(failureType.getDescription()).append("] ");
        sb.append(message);
        
        if (scopeName != null) {
            sb.append(" (scope: ").append(scopeName).append(")");
        }
        
        if (token != null) {
            sb.append(" at ").append(token.getLine()).append(":").append(token.getColumn());
            if (token.getText() != null) {
                sb.append(" '").append(token.getText()).append("'");
            }
        }
        
        if (contextInfo != null) {
            sb.append(" - ").append(contextInfo);
        }
        
        return sb.toString();
    }
    
    // Getters for diagnostic information
    
    public FailureType getFailureType() {
        return failureType;
    }
    
    public String getScopeName() {
        return scopeName;
    }
    
    public LocatableToken getFailureToken() {
        return failureToken;
    }
    
    public String getContextInfo() {
        return contextInfo;
    }
    
    /**
     * Checks if this exception has location information.
     * 
     * @return true if token location is available
     */
    public boolean hasLocation() {
        return failureToken != null;
    }
    
    /**
     * Gets the line number where the failure occurred.
     * 
     * @return the line number, or -1 if no location available
     */
    public int getLine() {
        return failureToken != null ? failureToken.getLine() : -1;
    }
    
    /**
     * Gets the column number where the failure occurred.
     * 
     * @return the column number, or -1 if no location available
     */
    public int getColumn() {
        return failureToken != null ? failureToken.getColumn() : -1;
    }
    
    // Static factory methods for common scenarios
    
    /**
     * Creates an exception for scope mismatch.
     * 
     * @param expected The expected scope
     * @param actual The actual scope found
     * @param token The token where mismatch detected
     * @return The configured exception
     */
    public static CallbackIntegrationException scopeMismatch(String expected, String actual, 
                                                            LocatableToken token) {
        String message = String.format("Expected scope '%s' but found '%s'", expected, actual);
        return new CallbackIntegrationException(message, FailureType.SCOPE_MISMATCH, actual, token);
    }
    
    /**
     * Creates an exception for missing callback.
     * 
     * @param callbackName The name of the missing callback
     * @param scopeName The scope where callback was expected
     * @return The configured exception
     */
    public static CallbackIntegrationException missingCallback(String callbackName, String scopeName) {
        String message = String.format("Required callback '%s' not implemented", callbackName);
        return new CallbackIntegrationException(message, FailureType.MISSING_CALLBACK, scopeName);
    }
    
    /**
     * Creates an exception for sequence violation.
     * 
     * @param expected The expected callback sequence
     * @param actual The actual callback invoked
     * @param token The token where violation occurred
     * @return The configured exception
     */
    public static CallbackIntegrationException sequenceViolation(String expected, String actual,
                                                                LocatableToken token) {
        String message = String.format("Expected callback '%s' but got '%s'", expected, actual);
        return new CallbackIntegrationException(message, FailureType.SEQUENCE_VIOLATION, null, token);
    }
    
    /**
     * Creates an exception for nesting overflow.
     * 
     * @param maxDepth The maximum allowed depth
     * @param currentDepth The depth that exceeded the limit
     * @param scopeName The scope being entered
     * @return The configured exception
     */
    public static CallbackIntegrationException nestingOverflow(int maxDepth, int currentDepth,
                                                              String scopeName) {
        String message = String.format("Scope nesting depth %d exceeds maximum %d", 
                                      currentDepth, maxDepth);
        return new CallbackIntegrationException(message, FailureType.NESTING_OVERFLOW, scopeName);
    }
}