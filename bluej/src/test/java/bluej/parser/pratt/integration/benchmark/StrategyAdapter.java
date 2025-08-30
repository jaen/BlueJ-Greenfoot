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
package bluej.parser.pratt.integration.benchmark;

/**
 * Common interface for all parser integration strategy adapters.
 * 
 * Provides a unified API for benchmarking different approaches to integrating
 * the Pratt parser with BlueJ's existing parsing infrastructure.
 */
public interface StrategyAdapter {
    
    /**
     * Parse the given code using this strategy's integration approach.
     * 
     * @param code The source code to parse
     * @return The parsed result (type varies by strategy)
     * @throws Exception if parsing fails
     */
    Object parseWithStrategy(String code) throws Exception;
    
    /**
     * Get the name of this integration strategy.
     * 
     * @return Strategy name for identification and reporting
     */
    String getStrategyName();
    
    /**
     * Get a description of this strategy's approach.
     * 
     * @return Human-readable description of the integration method
     */
    String getDescription();
    
    /**
     * Clean up any resources used by this strategy.
     * Called after benchmarking is complete.
     */
    default void cleanup() {
        // Default implementation: no cleanup needed
    }
    
    /**
     * Invoke a callback with the specified type and line number.
     * This method provides a unified way for strategies to trigger callbacks
     * during parsing operations.
     *
     * @param callbackType The type of callback to invoke
     * @param lineNumber The line number associated with the callback
     */
    default void invokeCallback(String callbackType, int lineNumber) {
        // Default implementation: no-op
        // Individual strategies should override this method to provide
        // appropriate callback handling
    }
}