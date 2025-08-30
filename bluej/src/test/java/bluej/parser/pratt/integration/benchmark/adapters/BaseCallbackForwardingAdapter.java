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
package bluej.parser.pratt.integration.benchmark.adapters;

import bluej.parser.CallbackDelegate;
import bluej.parser.pratt.integration.CallbackTestingUtility;

/**
 * Base abstract class for callback-forwarding adapters.
 * 
 * This class provides the infrastructure for adapters to both record callbacks
 * internally (for testing purposes) and forward them to an external delegate
 * (for test infrastructure integration).
 * 
 * Key features:
 * - Extends CallbackTestingUtility for internal callback recording
 * - Provides universal forwarding mechanism via CallbackDelegateForwarder
 * - Maintains backward compatibility with existing adapter code
 * - Supports gradual migration of adapter implementations
 * 
 * Concrete adapter classes should:
 * 1. Extend this base class instead of CallbackTestingUtility directly
 * 2. Override recordInternalCallback() to maintain internal recording logic
 * 3. Use forwardCallback() method in their invokeCallback() implementation
 * 4. Call setCallbackDelegate() to configure the external delegate
 */
public abstract class BaseCallbackForwardingAdapter extends CallbackTestingUtility
{
    /**
     * The external callback delegate to forward callbacks to.
     * This delegate receives callbacks for test infrastructure integration.
     */
    protected CallbackDelegate externalDelegate;
    
    /**
     * Set the external callback delegate.
     * 
     * @param delegate The callback delegate to forward callbacks to
     */
    public void setCallbackDelegate(CallbackDelegate delegate)
    {
        this.externalDelegate = delegate;
    }
    
    /**
     * Get the current external callback delegate.
     * 
     * @return The current external delegate, or null if not set
     */
    public CallbackDelegate getCallbackDelegate()
    {
        return this.externalDelegate;
    }
    
    /**
     * Forward a callback to both internal recording and external delegate.
     * 
     * This method is the core of the callback forwarding mechanism.
     * It ensures callbacks are:
     * 1. Recorded internally for adapter use (backward compatibility)
     * 2. Forwarded to external delegate for test infrastructure
     * 
     * @param callbackType The type of callback to forward
     * @param lineNumber The line number associated with the callback
     * @param params Additional parameters for the callback
     */
    protected void forwardCallback(String callbackType, int lineNumber, Object... params)
    {
        // Record internally for adapter use
        recordInternalCallback(callbackType, lineNumber, params);
        
        // Forward to external delegate if present
        if (externalDelegate != null)
        {
            CallbackDelegateForwarder.forward(externalDelegate, callbackType, lineNumber, params);
        }
    }
    
    /**
     * Record a callback internally for adapter-specific use.
     *
     * This provides a default implementation that handles all standard callback types.
     * Concrete adapter implementations can override this method if they need
     * custom internal callback recording logic.
     *
     * @param type The callback type
     * @param line The line number
     * @param params Additional parameters
     */
    protected void recordInternalCallback(String type, int line, Object... params)
    {
        String identifier = type.replace("Start", "").replace("End", "") + "_" + line;
        
        switch (type)
        {
            case "classStart":
                gotClassStart("class_" + line);
                break;
            case "classEnd":
                gotClassEnd("class_" + line);
                break;
            case "methodStart":
                gotMethodStart("method_" + line);
                break;
            case "methodEnd":
                gotMethodEnd("method_" + line);
                break;
            case "exprStart":
            case "blockStart":
            case "statementStart":
                gotExprStart(type + "_" + line);
                break;
            case "exprEnd":
            case "blockEnd":
            case "statementEnd":
                gotExprEnd(type + "_" + line);
                break;
            case "importStart":
                gotImportStart("import_" + line);
                break;
            case "importEnd":
                gotImportEnd("import_" + line);
                break;
            case "fieldDeclaration":
                if (params.length > 1)
                {
                    gotFieldDeclaration(params[0].toString(), params[1].toString());
                }
                else
                {
                    gotFieldDeclaration("field_" + line, "Type");
                }
                break;
            default:
                // For unmapped callbacks, record as expression
                gotExprStart(type + "_" + line);
                break;
        }
    }
    
    /**
     * Helper method for adapters to invoke callbacks.
     * This ensures proper callback generation through the forwarding infrastructure.
     *
     * @param callbackType The type of callback to invoke
     * @param lineNumber The line number associated with the callback
     */
    public void invokeCallback(String callbackType, int lineNumber)
    {
        forwardCallback(callbackType, lineNumber);
    }
    
    /**
     * Clear both internal state and external delegate.
     * Override this if you need custom cleanup logic.
     */
    @Override
    public void clear()
    {
        super.clear();
        // Don't clear the external delegate on clear() - it should persist
        // across test runs unless explicitly changed
    }
    
    /**
     * Check if an external delegate is configured.
     * 
     * @return true if an external delegate is set, false otherwise
     */
    protected boolean hasExternalDelegate()
    {
        return externalDelegate != null;
    }
    
    /**
     * Template method for adapter-specific initialization.
     * Subclasses can override this to perform initialization tasks.
     */
    protected void initialize()
    {
        // Default empty implementation
        // Subclasses can override to add initialization logic
    }
    
    /**
     * Template method for adapter-specific cleanup.
     * Subclasses can override this to perform cleanup tasks.
     */
    protected void cleanup()
    {
        // Default empty implementation
        // Subclasses can override to add cleanup logic
    }
}