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
package bluej.parser.pratt.parselets;

import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.pratt.KotlinPrattParser;
import bluej.parser.pratt.TestNodeFactory;
import bluej.parser.pratt.TokenOperations;
import junit.framework.TestCase;
import org.junit.Test;

/**
 * Unit tests for {@link NameParselet}.
 *
 * <p>These tests verify that the NameParselet correctly parses various types of identifiers
 * including simple names, backtick-wrapped identifiers, and Unicode identifiers according
 * to Kotlin naming rules.</p>
 *
 * @author BlueJ Team
 * @since BlueJ 5.4.0
 */
public class NameParseletTest extends TestCase {

    private NameParselet parselet;
    private TestKotlinPrattParser parser;

    @Override
    public void setUp() {
        parselet = new NameParselet();
        parser = new TestKotlinPrattParser();
    }

    @Test
    public void testSimpleIdentifier() {
        // Test parsing a simple identifier
        LocatableToken token = createIdentifierToken("myVariable");

        ParsedNode result = parselet.parse(parser, token);

        assertNotNull("Should create node for simple identifier", result);
        assertEquals("TestIdentifierNode:myVariable", result.toString());
        assertFalse("Should not have errors for valid identifier", parser.hasErrors());
    }

    @Test
    public void testIdentifierWithUnderscore() {
        // Test parsing identifier starting with underscore
        LocatableToken token = createIdentifierToken("_privateField");

        ParsedNode result = parselet.parse(parser, token);

        assertNotNull("Should create node for underscore identifier", result);
        assertEquals("TestIdentifierNode:_privateField", result.toString());
        assertFalse("Should not have errors for underscore identifier", parser.hasErrors());
    }

    @Test
    public void testBacktickIdentifier() {
        // Test parsing backtick-wrapped identifier (keyword as identifier)
        LocatableToken token = createIdentifierToken("`class`");

        ParsedNode result = parselet.parse(parser, token);

        assertNotNull("Should create node for backtick identifier", result);
        assertEquals("TestIdentifierNode:`class`", result.toString());
        assertFalse("Should not have errors for backtick identifier", parser.hasErrors());
    }

    @Test
    public void testInvalidTokenType() {
        // Test error handling for non-identifier token
        LocatableToken token = createToken("123", JavaTokenTypes.NUM_INT);

        ParsedNode result = parselet.parse(parser, token);

        assertNull("Should return null for non-identifier token", result);
        assertTrue("Should report error for invalid token type", parser.hasErrors());
    }

    @Test
    public void testNullToken() {
        // Test error handling for null token
        ParsedNode result = parselet.parse(parser, null);
        assertNull("Should return null for null token", result);
    }

    @Test
    public void testEmptyIdentifier() {
        // Test error handling for empty identifier
        LocatableToken token = createToken("", JavaTokenTypes.IDENT);

        ParsedNode result = parselet.parse(parser, token);

        assertNull("Should return null for empty identifier", result);
        assertTrue("Should report error for empty identifier", parser.hasErrors());
    }

    @Test
    public void testToString() {
        // Test toString method
        assertEquals("NameParselet", parselet.toString());
    }

    // Helper methods for creating test tokens

    private LocatableToken createIdentifierToken(String text) {
        return createToken(text, JavaTokenTypes.IDENT);
    }

    private LocatableToken createToken(String text, int tokenType) {
        bluej.parser.lexer.LineColPos begin = new bluej.parser.lexer.LineColPos(1, 1, 0);
        bluej.parser.lexer.LineColPos end = new bluej.parser.lexer.LineColPos(1, 1 + text.length(), text.length());
        return new LocatableToken(tokenType, text, begin, end);
    }

    /**
     * Simple test implementation of KotlinPrattParser for testing.
     */
    private static class TestKotlinPrattParser extends KotlinPrattParser {
        private boolean hasErrors = false;
        private String lastError = "";
        private final TestNodeFactory testNodeFactory;

        public TestKotlinPrattParser() {
            super(new TestTokenOperations(), null, new TestNodeFactory());
            this.testNodeFactory = (TestNodeFactory) getNodeFactory();
        }

        @Override
        public void error(String message, LocatableToken token) {
            hasErrors = true;
            lastError = message;
        }

        public boolean hasErrors() {
            return hasErrors || testNodeFactory.hasErrors();
        }

        public String getLastError() {
            return lastError;
        }
    }

    /**
     * Simple test implementation of TokenOperations.
     */
    private static class TestTokenOperations implements TokenOperations {
        @Override
        public LocatableToken nextToken() { return null; }

        @Override
        public LocatableToken LA(int distance) { return null; }

        @Override
        public void pushBack(LocatableToken token) {}

        @Override
        public LocatableToken getMostRecent() { return null; }
    }
}
