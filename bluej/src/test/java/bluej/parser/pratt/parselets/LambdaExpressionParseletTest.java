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
import bluej.parser.pratt.ParseResult;
import bluej.parser.pratt.TestNodeFactory;
import bluej.parser.pratt.testutil.MockParser;
import bluej.parser.pratt.testutil.TestUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test class for LambdaExpressionParselet.
 *
 * <p>This test verifies that lambda expressions ({ x -> x + 1 }) are correctly parsed
 * with proper parameter handling and body parsing.</p>
 *
 * @author BlueJ Team
 */
public class LambdaExpressionParseletTest {

    private LambdaExpressionParselet lambdaParselet;
    private MockParser testParser;

    @Before
    public void setUp() {
        lambdaParselet = new LambdaExpressionParselet();
        testParser = new MockParser();

        // Register necessary parselets for sub-expression parsing
        testParser.registerParselet(JavaTokenTypes.NUM_INT, new LiteralParselet());
        testParser.registerParselet(JavaTokenTypes.IDENT, new NameParselet());
    }

    @Test
    public void testSimpleLambdaNoParameters() {
        // Test parsing simple lambda: { 42 }
        testParser.setTokenSequence(
            TestUtils.createToken(JavaTokenTypes.LCURLY, "{"),
            TestUtils.createToken(JavaTokenTypes.NUM_INT, "42"),
            TestUtils.createToken(JavaTokenTypes.RCURLY, "}"),
            TestUtils.createToken(JavaTokenTypes.EOF, "")
        );

        LocatableToken openBrace = testParser.consume();
        ParseResult<ParsedNode> result = lambdaParselet.parse(testParser, openBrace);

        assertTrue("Lambda parsing should succeed", result.isSuccess());
        assertNotNull("Result should contain a valid node", result.getValue());
    }

    @Test
    public void testLambdaWithSingleParameter() {
        // Test parsing lambda with parameter: { x -> x }
        testParser.setTokenSequence(
            TestUtils.createToken(JavaTokenTypes.LCURLY, "{"),
            TestUtils.createToken(JavaTokenTypes.IDENT, "x"),
            TestUtils.createToken(JavaTokenTypes.ARROW, "->"),
            TestUtils.createToken(JavaTokenTypes.IDENT, "x"),
            TestUtils.createToken(JavaTokenTypes.RCURLY, "}"),
            TestUtils.createToken(JavaTokenTypes.EOF, "")
        );

        LocatableToken openBrace = testParser.consume();
        ParseResult<ParsedNode> result = lambdaParselet.parse(testParser, openBrace);

        assertTrue("Lambda with parameter parsing should succeed", result.isSuccess());
        assertNotNull("Result should contain a valid node", result.getValue());
    }

    @Test
    public void testLambdaWithArrowButNoParameters() {
        // Test parsing lambda: { -> 42 }
        testParser.setTokenSequence(
            TestUtils.createToken(JavaTokenTypes.LCURLY, "{"),
            TestUtils.createToken(JavaTokenTypes.ARROW, "->"),
            TestUtils.createToken(JavaTokenTypes.NUM_INT, "42"),
            TestUtils.createToken(JavaTokenTypes.RCURLY, "}"),
            TestUtils.createToken(JavaTokenTypes.EOF, "")
        );

        LocatableToken openBrace = testParser.consume();
        ParseResult<ParsedNode> result = lambdaParselet.parse(testParser, openBrace);

        assertTrue("Lambda with arrow parsing should succeed", result.isSuccess());
        assertNotNull("Result should contain a valid node", result.getValue());
    }

    @Test
    public void testMissingClosingBrace() {
        // Test error handling: missing closing brace
        testParser.setTokenSequence(
            TestUtils.createToken(JavaTokenTypes.LCURLY, "{"),
            TestUtils.createToken(JavaTokenTypes.NUM_INT, "42"),
            TestUtils.createToken(JavaTokenTypes.EOF, "")
        );

        LocatableToken openBrace = testParser.consume();
        ParseResult<ParsedNode> result = lambdaParselet.parse(testParser, openBrace);

        assertTrue("Lambda with missing brace should fail", result.isFailure());
    }

    @Test
    public void testWrongTokenType() {
        // Test error handling: wrong token type passed to parselet
        LocatableToken wrongToken = TestUtils.createToken(JavaTokenTypes.PLUS, "+");
        ParseResult<ParsedNode> result = lambdaParselet.parse(testParser, wrongToken);

        assertTrue("Lambda with wrong token should fail", result.isFailure());
        assertEquals("Should have one error", 1, result.getErrors().size());
        assertTrue("Error should mention expected brace",
                   result.getErrors().get(0).message().contains("Expected '{'"));
    }

    @Test
    public void testToString() {
        assertEquals("ToString should return parselet name",
                     "LambdaExpressionParselet", lambdaParselet.toString());
    }
}
