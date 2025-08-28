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
 * Unit tests for {@link ThisParselet} and {@link SuperParselet}.
 *
 * <p>These tests verify that the special keyword parselets correctly parse
 * 'this' and 'super' references and create appropriate AST nodes.</p>
 *
 * @author BlueJ Team
 * @since BlueJ 5.4.0
 */
public class ThisSuperParseletTest {

    private ThisParselet thisParselet;
    private SuperParselet superParselet;
    private TestKotlinPrattParser parser;

    @Before
    public void setUp() {
        thisParselet = new ThisParselet();
        superParselet = new SuperParselet();
        parser = new TestKotlinPrattParser();
    }

    @Test
    public void testThisKeyword() {
        // Test parsing 'this' keyword
        LocatableToken token = createToken("this", JavaTokenTypes.LITERAL_this);

        ParseResult<ParsedNode> result = thisParselet.parse(parser, token);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should create node for 'this' keyword", node);
        assertTrue("Should be a TestNode", node instanceof TestNodeFactory.TestNode);
        assertEquals("This", ((TestNodeFactory.TestNode) node).getTestNodeType());
        assertFalse("Should not have errors for valid 'this'", parser.hasErrors());
    }

    @Test
    public void testSuperKeyword() {
        // Test parsing 'super' keyword
        LocatableToken token = createToken("super", JavaTokenTypes.LITERAL_super);

        ParseResult<ParsedNode> result = superParselet.parse(parser, token);

        assertNotNull("Should return a result", result);
        assertTrue("Should be successful", result.isSuccess());
        ParsedNode node = result.getValue();
        assertNotNull("Should create node for 'super' keyword", node);
        assertTrue("Should be a TestNode", node instanceof TestNodeFactory.TestNode);
        assertEquals("Super", ((TestNodeFactory.TestNode) node).getTestNodeType());
        assertFalse("Should not have errors for valid 'super'", parser.hasErrors());
    }

    @Test
    public void testThisWithInvalidToken() {
        // Test error handling for non-'this' token
        LocatableToken token = createToken("that", JavaTokenTypes.IDENT);

        ParseResult<ParsedNode> result = thisParselet.parse(parser, token);

        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure for invalid token", result.isFailure());
        assertFalse("Should not report error in parser", parser.hasErrors());
        assertTrue("Should have errors in result", !result.getErrors().isEmpty());
    }

    @Test
    public void testSuperWithInvalidToken() {
        // Test error handling for non-'super' token
        LocatableToken token = createToken("parent", JavaTokenTypes.IDENT);

        ParseResult<ParsedNode> result = superParselet.parse(parser, token);

        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure for invalid token", result.isFailure());
        assertFalse("Should not report error in parser", parser.hasErrors());
        assertTrue("Should have errors in result", !result.getErrors().isEmpty());
    }

    @Test
    public void testThisNullToken() {
        // Test error handling for null token
        ParseResult<ParsedNode> result = thisParselet.parse(parser, null);
        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure for null token", result.isFailure());
    }

    @Test
    public void testSuperNullToken() {
        // Test error handling for null token
        ParseResult<ParsedNode> result = superParselet.parse(parser, null);
        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure for null token", result.isFailure());
    }

    @Test
    public void testThisWithWrongTokenType() {
        // Test 'this' parselet with wrong literal token
        LocatableToken token = createToken("true", JavaTokenTypes.LITERAL_true);

        ParseResult<ParsedNode> result = thisParselet.parse(parser, token);

        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure for wrong literal token", result.isFailure());
        assertFalse("Should not report error in parser", parser.hasErrors());
        assertTrue("Should have errors in result", !result.getErrors().isEmpty());
    }

    @Test
    public void testSuperWithWrongTokenType() {
        // Test 'super' parselet with wrong literal token
        LocatableToken token = createToken("false", JavaTokenTypes.LITERAL_false);

        ParseResult<ParsedNode> result = superParselet.parse(parser, token);

        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure for wrong literal token", result.isFailure());
        assertFalse("Should not report error in parser", parser.hasErrors());
        assertTrue("Should have errors in result", !result.getErrors().isEmpty());
    }

    @Test
    public void testThisToString() {
        // Test toString method
        assertEquals("ThisParselet", thisParselet.toString());
    }

    @Test
    public void testSuperToString() {
        // Test toString method
        assertEquals("SuperParselet", superParselet.toString());
    }

    @Test
    public void testNodeFactoryIntegration() {
        // Test that parselets work correctly with NodeFactory
        LocatableToken thisToken = createToken("this", JavaTokenTypes.LITERAL_this);
        LocatableToken superToken = createToken("super", JavaTokenTypes.LITERAL_super);

        ParseResult<ParsedNode> thisResult = thisParselet.parse(parser, thisToken);
        ParseResult<ParsedNode> superResult = superParselet.parse(parser, superToken);

        assertNotNull("Should return a result for this", thisResult);
        assertNotNull("Should return a result for super", superResult);
        assertTrue("This result should be successful", thisResult.isSuccess());
        assertTrue("Super result should be successful", superResult.isSuccess());

        ParsedNode thisNode = thisResult.getValue();
        ParsedNode superNode = superResult.getValue();

        assertNotNull("ThisParselet should create node via factory", thisNode);
        assertNotNull("SuperParselet should create node via factory", superNode);
        assertFalse("Should not have errors with valid factory", parser.hasErrors());

        // Verify nodes are different instances
        assertNotSame("Should create different node instances", thisNode, superNode);
    }

    // Helper methods for creating test tokens

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
