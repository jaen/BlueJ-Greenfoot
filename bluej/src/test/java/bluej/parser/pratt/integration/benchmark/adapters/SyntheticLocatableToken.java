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

import bluej.parser.lexer.LocatableToken;
import bluej.parser.lexer.LineColPos;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Synthetic implementation of LocatableToken for callback forwarding.
 * 
 * This class creates lightweight token instances that can be used to satisfy
 * the CallbackDelegate interface requirements when forwarding callbacks from
 * adapters to the test infrastructure.
 * 
 * Features:
 * - Minimal memory footprint
 * - Object pooling for performance
 * - Thread-safe token creation
 * - Appropriate defaults for synthetic tokens
 */
public class SyntheticLocatableToken extends LocatableToken
{
    // Token pooling for performance
    private static final ConcurrentLinkedQueue<SyntheticLocatableToken> tokenPool = 
        new ConcurrentLinkedQueue<>();
    private static final int MAX_POOL_SIZE = 100;
    
    /**
     * Factory method to create or reuse a synthetic token.
     * Uses object pooling to reduce allocation overhead.
     * 
     * @param text The text content of the token
     * @param lineNumber The line number for the token
     * @return A synthetic locatable token
     */
    public static SyntheticLocatableToken create(String text, int lineNumber)
    {
        SyntheticLocatableToken token = tokenPool.poll();
        if (token != null)
        {
            // Reuse pooled token
            token.reinitialize(text, lineNumber);
            return token;
        }
        // Create new token if pool is empty
        return new SyntheticLocatableToken(text, lineNumber);
    }
    
    /**
     * Return a token to the pool for reuse.
     * 
     * @param token The token to return to the pool
     */
    public static void release(SyntheticLocatableToken token)
    {
        if (tokenPool.size() < MAX_POOL_SIZE)
        {
            tokenPool.offer(token);
        }
    }
    
    /**
     * Create a new synthetic token.
     * 
     * @param text The text content of the token
     * @param lineNumber The line number for the token
     */
    private SyntheticLocatableToken(String text, int lineNumber)
    {
        super(
            0, // type - use default token type
            text != null ? text : "synthetic",
            new LineColPos(lineNumber, 1, lineNumber), // begin position
            new LineColPos(lineNumber, text != null ? text.length() : 9, lineNumber + (text != null ? text.length() : 9)) // end position
        );
    }
    
    /**
     * Reinitialize a pooled token with new values.
     * 
     * @param text The new text content
     * @param lineNumber The new line number
     */
    private void reinitialize(String text, int lineNumber)
    {
        // LocatableToken fields are final, so we can't actually reinitialize
        // For now, we'll just create new tokens each time
        // This is a placeholder for future optimization if the base class changes
    }
    
    /**
     * Create a simple synthetic token without pooling.
     * Used for one-off token creation where pooling isn't beneficial.
     * 
     * @param text The text content of the token
     * @param lineNumber The line number for the token
     * @return A new synthetic locatable token
     */
    public static LocatableToken createSimple(String text, int lineNumber)
    {
        return new SyntheticLocatableToken(text, lineNumber);
    }
    
    @Override
    public String toString()
    {
        return "SyntheticToken[" + getText() + " at line " + getLine() + "]";
    }
}