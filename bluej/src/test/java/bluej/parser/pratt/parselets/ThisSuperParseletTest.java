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
import bluej.parser.pratt.testutil.MockParser;
import bluej.parser.pratt.testutil.TestUtils;
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
    private MockParser parser;

    @Before
    public void setUp() {
        thisParselet = new ThisParselet();
        superParselet = new SuperParselet();
        parser = new MockParser();
    }

    @Test
    public void testThisKeyword() {
        // Test parsing 'this' keyword
        LocatableToken token = TestUtils.createToken(JavaTokenTypes.LITERAL_this, "this");

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
        LocatableToken token = TestUtils.createToken(JavaTokenTypes.LITERAL_super, "super");

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
        LocatableToken token = TestUtils.createToken(JavaTokenTypes.IDENT, "that");

        ParseResult<ParsedNode> result = thisParselet.parse(parser, token);

        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure for invalid token", result.isFailure());
        assertFalse("Should not report error in parser", parser.hasErrors());
        assertTrue("Should have errors in result", !result.getErrors().isEmpty());
    }

    @Test
    public void testSuperWithInvalidToken() {
        // Test error handling for non-'super' token
        LocatableToken token = TestUtils.createToken(JavaTokenTypes.IDENT, "parent");

        ParseResult<ParsedNode> result = superParselet.parse(parser, token);

        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure for invalid token", result.isFailure());
        assertFalse("Should not report error in parser", parser.hasErrors());
        assertTrue("Should have errors in result", !result.getErrors().isEmpty());
    }



    @Test
    public void testThisWithWrongTokenType() {
        // Test 'this' parselet with wrong literal token
        LocatableToken token = TestUtils.createToken(JavaTokenTypes.LITERAL_true, "true");

        ParseResult<ParsedNode> result = thisParselet.parse(parser, token);

        assertNotNull("Should return a result", result);
        assertTrue("Should be a failure for wrong literal token", result.isFailure());
        assertFalse("Should not report error in parser", parser.hasErrors());
        assertTrue("Should have errors in result", !result.getErrors().isEmpty());
    }

    @Test
    public void testSuperWithWrongTokenType() {
        // Test 'super' parselet with wrong literal token
        LocatableToken token = TestUtils.createToken(JavaTokenTypes.LITERAL_false, "false");

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
        LocatableToken thisToken = TestUtils.createToken(JavaTokenTypes.LITERAL_this, "this");
        LocatableToken superToken = TestUtils.createToken(JavaTokenTypes.LITERAL_super, "super");

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


}
