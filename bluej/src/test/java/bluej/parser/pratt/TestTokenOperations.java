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

import bluej.parser.lexer.JavaTokenFilter;
import bluej.parser.lexer.LocatableToken;

/**
 * Test implementation of TokenOperations for unit testing.
 *
 * <p>This implementation directly delegates to JavaTokenFilter without
 * threading constraints, suitable for test environments. Unlike the
 * production ThreadSafeTokenOperations, this implementation does not
 * require FXPlatform thread access.</p>
 *
 * @author BlueJ Team
 * @since BlueJ 5.4.0
 */
public class TestTokenOperations implements TokenOperations {

    private final JavaTokenFilter tokenStream;

    /**
     * Creates a TestTokenOperations that delegates to the given token stream.
     *
     * @param tokenStream The underlying token stream
     */
    public TestTokenOperations(JavaTokenFilter tokenStream) {
        this.tokenStream = tokenStream;
    }

    /**
     * Default constructor for tests that don't need a real token stream.
     * This creates a null implementation for simple unit tests.
     */
    public TestTokenOperations() {
        this.tokenStream = null;
    }

    @Override
    public LocatableToken nextToken() {
        if (tokenStream == null) {
            return null;
        }
        return tokenStream.nextToken();
    }

    @Override
    public LocatableToken LA(int distance) {
        if (tokenStream == null) {
            return null;
        }
        return tokenStream.LA(distance);
    }

    @Override
    public void pushBack(LocatableToken token) {
        if (tokenStream != null) {
            tokenStream.pushBack(token);
        }
    }

    @Override
    public LocatableToken getMostRecent() {
        if (tokenStream == null) {
            return null;
        }
        return tokenStream.getMostRecent();
    }
}
