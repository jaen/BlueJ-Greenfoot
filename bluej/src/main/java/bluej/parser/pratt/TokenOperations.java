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

/**
 * Interface for token stream operations to abstract threading requirements.
 *
 * <p>This interface allows the parser to remain thread-agnostic while
 * implementations can handle thread-specific requirements. The KotlinParserAdapter
 * provides a thread-safe implementation that respects JavaFX platform threading
 * requirements, while test implementations can provide simpler alternatives.</p>
 *
 * <p>Note: Implementations are responsible for handling their own threading
 * requirements. Production implementations that interact with JavaTokenFilter
 * must ensure they are called on the appropriate thread.</p>
 *
 * @author BlueJ Development Team
 * @since BlueJ 5.4.0
 */
public interface TokenOperations {
    /**
     * Gets the next token from the token stream and advances the position.
     *
     * @return The next token, or null if at end of stream
     */
    LocatableToken nextToken();

    /**
     * Looks ahead at a token without consuming it.
     *
     * @param distance The look-ahead distance (1 = next token, 2 = token after that, etc.)
     * @return The token at the specified distance, or null if beyond end of stream
     */
    LocatableToken LA(int distance);

    /**
     * Pushes a token back onto the token stream.
     *
     * <p>This is useful for implementing lookahead and backtracking.</p>
     *
     * @param token The token to push back
     */
    void pushBack(LocatableToken token);

    /**
     * Gets the most recently consumed token.
     *
     * @return The most recent token, or null if no tokens have been consumed
     */
    LocatableToken getMostRecent();
}
