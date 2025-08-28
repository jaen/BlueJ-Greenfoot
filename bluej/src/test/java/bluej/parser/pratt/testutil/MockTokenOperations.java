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
package bluej.parser.pratt.testutil;

import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LineColPos;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.pratt.TokenOperations;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock implementation of TokenOperations for testing parselets.
 *
 * <p>This class provides a controlled token stream for unit tests,
 * allowing tests to set up specific token sequences and control
 * the token flow programmatically.</p>
 *
 * @author BlueJ Team
 * @since BlueJ 5.4.0
 */
public class MockTokenOperations implements TokenOperations {
    private final List<LocatableToken> tokens;
    private int position;
    private LocatableToken mostRecent;
    private boolean returnEOFOnEmpty;

    /**
     * Creates a new MockTokenOperations with empty token list.
     */
    public MockTokenOperations() {
        this.tokens = new ArrayList<>();
        this.position = 0;
        this.mostRecent = null;
        this.returnEOFOnEmpty = true;
    }

    /**
     * Creates a new MockTokenOperations with specified tokens.
     *
     * @param tokens Initial tokens to provide
     */
    public MockTokenOperations(List<LocatableToken> tokens) {
        this.tokens = new ArrayList<>(tokens);
        this.position = 0;
        this.mostRecent = null;
        this.returnEOFOnEmpty = true;
    }

    /**
     * Sets whether to return EOF token when no more tokens available.
     *
     * @param returnEOF true to return EOF, false to throw exception
     */
    public void setReturnEOFOnEmpty(boolean returnEOF) {
        this.returnEOFOnEmpty = returnEOF;
    }

    /**
     * Adds a token to the end of the token stream.
     *
     * @param token Token to add
     */
    public void addToken(LocatableToken token) {
        tokens.add(token);
    }

    /**
     * Adds multiple tokens to the end of the token stream.
     *
     * @param tokens Tokens to add
     */
    public void addTokens(LocatableToken... tokens) {
        for (LocatableToken token : tokens) {
            this.tokens.add(token);
        }
    }

    /**
     * Resets the position to the beginning of the token stream.
     */
    public void reset() {
        position = 0;
        mostRecent = null;
    }

    /**
     * Sets the complete token sequence, replacing any existing tokens.
     *
     * @param tokens New token sequence
     */
    public void setTokens(List<LocatableToken> tokens) {
        this.tokens.clear();
        this.tokens.addAll(tokens);
        this.position = 0;
        this.mostRecent = null;
    }

    @Override
    public @NotNull LocatableToken nextToken() {
        if (position < tokens.size()) {
            mostRecent = tokens.get(position++);
            return mostRecent;
        }

        if (returnEOFOnEmpty) {
            return createEOFToken();
        }

        throw new IllegalStateException("No more tokens available");
    }

    @Override
    public @NotNull LocatableToken LA(int distance) {
        int peekPosition = position + distance - 1;

        if (peekPosition >= 0 && peekPosition < tokens.size()) {
            return tokens.get(peekPosition);
        }

        if (returnEOFOnEmpty) {
            return createEOFToken();
        }

        throw new IllegalStateException("Look-ahead beyond available tokens");
    }

    @Override
    public void pushBack(LocatableToken token) {
        if (position > 0) {
            position--;
            // Insert the token at the current position
            tokens.add(position, token);
        } else {
            tokens.add(0, token);
        }
    }

    @Override
    public LocatableToken getMostRecent() {
        if (mostRecent != null) {
            return mostRecent;
        }

        if (returnEOFOnEmpty) {
            return createEOFToken();
        }

        throw new IllegalStateException("No recent token available");
    }

    /**
     * Creates an EOF token with default position.
     *
     * @return EOF token
     */
    private static LocatableToken createEOFToken() {
        LineColPos pos = new LineColPos(1, 1, 0);
        return new LocatableToken(JavaTokenTypes.EOF, "", pos, pos);
    }

    /**
     * Gets the current position in the token stream.
     *
     * @return Current position
     */
    public int getPosition() {
        return position;
    }

    /**
     * Gets the total number of tokens.
     *
     * @return Token count
     */
    public int getTokenCount() {
        return tokens.size();
    }

    /**
     * Checks if there are more tokens available.
     *
     * @return true if more tokens available, false otherwise
     */
    public boolean hasMoreTokens() {
        return position < tokens.size();
    }
}
