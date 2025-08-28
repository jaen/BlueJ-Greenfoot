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
import bluej.parser.pratt.ParseResult;
import bluej.parser.pratt.TestNodeFactory;
import bluej.parser.pratt.TokenOperations;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

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
public class NameParseletTest {

    private NameParselet parselet;
    private TestKotlinPrattParser parser;

    @Before
    public void setUp() {
        parselet = new NameParselet();
        parser = new TestKotlinPrattParser();
    }

    @Test
    public void testSimpleIdentifier() {
        // Test parsing a simple identifier
        LocatableToken token = createIdentifierToken("myVariable");

        ParseResult<ParsedNode> result = parselet.parse(parser, token);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should have a node value", node);
        assertEquals("TestIdentifierNode:myVariable", node.toString());
        assertFalse("Should not have errors for valid identifier", parser.hasErrors());
    }

    @Test
    public void testIdentifierWithUnderscore() {
        // Test parsing identifier starting with underscore
        LocatableToken token = createIdentifierToken("_privateField");

        ParseResult<ParsedNode> result = parselet.parse(parser, token);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should have a node value", node);
        assertEquals("TestIdentifierNode:_privateField", node.toString());
        assertFalse("Should not have errors for underscore identifier", parser.hasErrors());
    }

    @Test
    public void testBacktickIdentifier() {
        // Test parsing backtick-wrapped identifier (keyword as identifier)
        LocatableToken token = createIdentifierToken("`class`");

        ParseResult<ParsedNode> result = parselet.parse(parser, token);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should have a node value", node);
        assertEquals("TestIdentifierNode:`class`", node.toString());
        assertFalse("Should not have errors for backtick identifier", parser.hasErrors());
    }

    @Test
    public void testInvalidTokenType() {
        // Test error handling for non-identifier token
        LocatableToken token = createToken("123", JavaTokenTypes.NUM_INT);

        ParseResult<ParsedNode> result = parselet.parse(parser, token);

        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure", result.isFailure());
        assertFalse("Should not have errors in parser", parser.hasErrors());
        assertTrue("Should have error in result", !result.getErrors().isEmpty());
    }

    @Test
    public void testNullToken() {
        // Test error handling for null token
        ParseResult<ParsedNode> result = parselet.parse(parser, null);
        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure", result.isFailure());
    }

    @Test
    public void testEmptyIdentifier() {
        // Test error handling for empty identifier
        LocatableToken token = createToken("", JavaTokenTypes.IDENT);

        ParseResult<ParsedNode> result = parselet.parse(parser, token);

        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure", result.isFailure());
        assertFalse("Should not have errors in parser", parser.hasErrors());
        assertTrue("Should have error in result", !result.getErrors().isEmpty());
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
        public @NotNull LocatableToken nextToken() { return null; }

        @Override
        public @NotNull LocatableToken LA(int distance) { return null; }

        @Override
        public void pushBack(LocatableToken token) {}

        @Override
        public LocatableToken getMostRecent() { return null; }
    }
}
