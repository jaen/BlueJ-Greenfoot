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

/**
 * Minimal CallbackContext implementation to satisfy TrackedScope requirements.
 * 
 * <p>This is a temporary bridge class that provides the minimal interface
 * needed by TrackedScope. The actual callback management is handled by
 * SafeCallbacks directly.</p>
 *
 * @author BlueJ Team
 * @since BlueJ 5.4.0
 */
public class CallbackContext {
    
    private final CallbackDelegate delegate;
    
    /**
     * Creates a minimal CallbackContext.
     */
    public CallbackContext() {
        this.delegate = null;
    }
    
    /**
     * Creates a CallbackContext with a delegate.
     *
     * @param delegate The callback delegate
     */
    public CallbackContext(CallbackDelegate delegate) {
        this.delegate = delegate;
    }
    
    /**
     * Gets the callback delegate.
     *
     * @return The callback delegate
     */
    public CallbackDelegate getDelegate() {
        return delegate;
    }
}