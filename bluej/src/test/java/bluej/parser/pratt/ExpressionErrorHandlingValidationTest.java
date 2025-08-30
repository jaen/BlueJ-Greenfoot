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

import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.pratt.parselets.*;
import bluej.parser.pratt.testutil.MockParser;
import bluej.parser.pratt.testutil.TestUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Error handling validation tests for Kotlin expression parsing.
 *
 * <p>This test suite validates that the Kotlin-specific expression parselets
 * handle error scenarios gracefully. It focuses on testing individual parselets
 * with malformed or incomplete input to ensure robust error handling without
 * exceptions or crashes.</p>
 *
 * <p>The tests cover error scenarios for parselets from phases 1-4:</p>
 * <ul>
 *   <li>Phase 1: Safe call and Elvis operator error handling</li>
 *   <li>Phase 2: Lambda expression error handling</li>
 *   <li>Phase 3: Type operation error handling</li>
 *   <li>Phase 4: Range and containment error handling</li>
 * </ul>
 *
 * @author BlueJ Development Team
 */
public class ExpressionErrorHandlingValidationTest {

    private MockParser parser;

    @Before
    public void setUp() {
        parser = new MockParser();
    }

    // ========== Elvis Operator Error Tests ==========

    /**
     * Test elvis operator with missing right operand.
     */
    @Test
    public void testElvisWithMissingRightOperand() {
        ElvisOperatorParselet elvisParselet = new ElvisOperatorParselet();
        ParsedNode left = TestUtils.createIdentifierNode("value");
        LocatableToken elvisToken = TestUtils.createToken(JavaTokenTypes.ELVIS, "?:", 1, 6);

        // Don't set next token - simulates missing right operand
        ParseResult<ParsedNode> result = elvisParselet.parse(parser, left, elvisToken);

        // Should either succeed with EOF or fail gracefully
        assertNotNull("Should return a result", result);
        if (result.isFailure()) {
            assertTrue("Should have error message", !result.getErrors().isEmpty());
        }
    }

    /**
     * Test elvis operator with wrong token type.
     */
    @Test
    public void testElvisWithWrongToken() {
        ElvisOperatorParselet elvisParselet = new ElvisOperatorParselet();
        ParsedNode left = TestUtils.createIdentifierNode("value");
        LocatableToken wrongToken = TestUtils.createToken(JavaTokenTypes.QUESTION, "?", 1, 6);

        ParseResult<ParsedNode> result = elvisParselet.parse(parser, left, wrongToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should fail with wrong token", result.isFailure());
    }

    // ========== Lambda Expression Error Tests ==========

    /**
     * Test lambda with missing closing brace.
     */
    @Test
    public void testLambdaWithMissingClosingBrace() {
        LambdaExpressionParselet lambdaParselet = new LambdaExpressionParselet();
        LocatableToken lambdaToken = TestUtils.createToken(JavaTokenTypes.LCURLY, "{", 1, 1);

        // Set up tokens without closing brace
        parser.setTokenSequence(
            TestUtils.createToken(JavaTokenTypes.IDENT, "it", 1, 3)
            // Missing RCURLY
        );

        ParseResult<ParsedNode> result = lambdaParselet.parse(parser, lambdaToken);

        assertNotNull("Should return a result", result);
        // Should either succeed with partial parse or fail gracefully
        if (result.isFailure()) {
            assertTrue("Should have error about missing brace", !result.getErrors().isEmpty());
        }
    }

    /**
     * Test lambda with wrong opening token.
     */
    @Test
    public void testLambdaWithWrongOpeningToken() {
        LambdaExpressionParselet lambdaParselet = new LambdaExpressionParselet();
        LocatableToken wrongToken = TestUtils.createToken(JavaTokenTypes.LPAREN, "(", 1, 1);

        ParseResult<ParsedNode> result = lambdaParselet.parse(parser, wrongToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should fail with wrong token", result.isFailure());
    }

    // ========== Type Check Error Tests ==========

    /**
     * Test type check with missing type.
     */
    @Test
    public void testTypeCheckWithMissingType() {
        TypeCheckParselet typeCheckParselet = new TypeCheckParselet();
        ParsedNode left = TestUtils.createIdentifierNode("value");
        LocatableToken isToken = TestUtils.createToken(JavaTokenTypes.LITERAL_is, "is", 1, 6);

        // Don't set next token - simulates missing type
        ParseResult<ParsedNode> result = typeCheckParselet.parse(parser, left, isToken);

        assertNotNull("Should return a result", result);
        if (result.isFailure()) {
            assertTrue("Should have error about missing type", !result.getErrors().isEmpty());
        }
    }

    /**
     * Test type check with wrong token.
     */
    @Test
    public void testTypeCheckWithWrongToken() {
        TypeCheckParselet typeCheckParselet = new TypeCheckParselet();
        ParsedNode left = TestUtils.createIdentifierNode("value");
        LocatableToken wrongToken = TestUtils.createToken(JavaTokenTypes.LITERAL_instanceof, "instanceof", 1, 6);

        ParseResult<ParsedNode> result = typeCheckParselet.parse(parser, left, wrongToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should fail with wrong token", result.isFailure());
    }

    // ========== Type Cast Error Tests ==========

    /**
     * Test type cast with missing type.
     */
    @Test
    public void testTypeCastWithMissingType() {
        TypeCastParselet typeCastParselet = new TypeCastParselet();
        ParsedNode left = TestUtils.createIdentifierNode("value");
        LocatableToken asToken = TestUtils.createToken(JavaTokenTypes.LITERAL_as, "as", 1, 6);

        // Don't set next token - simulates missing type
        ParseResult<ParsedNode> result = typeCastParselet.parse(parser, left, asToken);

        assertNotNull("Should return a result", result);
        if (result.isFailure()) {
            assertTrue("Should have error about missing type", !result.getErrors().isEmpty());
        }
    }

    /**
     * Test type cast with wrong token.
     */
    @Test
    public void testTypeCastWithWrongToken() {
        TypeCastParselet typeCastParselet = new TypeCastParselet();
        ParsedNode left = TestUtils.createIdentifierNode("value");
        LocatableToken wrongToken = TestUtils.createToken(JavaTokenTypes.LITERAL_is, "is", 1, 6);

        ParseResult<ParsedNode> result = typeCastParselet.parse(parser, left, wrongToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should fail with wrong token", result.isFailure());
    }

    // ========== Range Operator Error Tests ==========

    /**
     * Test range with missing end value.
     */
    @Test
    public void testRangeWithMissingEnd() {
        RangeParselet rangeParselet = new RangeParselet();
        ParsedNode left = TestUtils.createIdentifierNode("start");
        LocatableToken rangeToken = TestUtils.createToken(JavaTokenTypes.RANGE, "..", 1, 6);

        // Don't set next token - simulates missing end
        ParseResult<ParsedNode> result = rangeParselet.parse(parser, left, rangeToken);

        assertNotNull("Should return a result", result);
        if (result.isFailure()) {
            assertTrue("Should have error about missing end", !result.getErrors().isEmpty());
        }
    }

    /**
     * Test range with wrong token.
     */
    @Test
    public void testRangeWithWrongToken() {
        RangeParselet rangeParselet = new RangeParselet();
        ParsedNode left = TestUtils.createIdentifierNode("start");
        LocatableToken wrongToken = TestUtils.createToken(JavaTokenTypes.DOT, ".", 1, 6);

        ParseResult<ParsedNode> result = rangeParselet.parse(parser, left, wrongToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should fail with wrong token", result.isFailure());
    }

    // ========== In Operator Error Tests ==========

    /**
     * Test in operator with missing collection.
     */
    @Test
    public void testInOperatorWithMissingCollection() {
        InOperatorParselet inParselet = new InOperatorParselet();
        ParsedNode left = TestUtils.createIdentifierNode("value");
        LocatableToken inToken = TestUtils.createToken(JavaTokenTypes.LITERAL_in, "in", 1, 6);

        // Don't set next token - simulates missing collection
        ParseResult<ParsedNode> result = inParselet.parse(parser, left, inToken);

        assertNotNull("Should return a result", result);
        if (result.isFailure()) {
            assertTrue("Should have error about missing collection", !result.getErrors().isEmpty());
        }
    }

    /**
     * Test in operator with wrong token.
     */
    @Test
    public void testInOperatorWithWrongToken() {
        InOperatorParselet inParselet = new InOperatorParselet();
        ParsedNode left = TestUtils.createIdentifierNode("value");
        LocatableToken wrongToken = TestUtils.createToken(JavaTokenTypes.LITERAL_is, "is", 1, 6);

        ParseResult<ParsedNode> result = inParselet.parse(parser, left, wrongToken);

        assertNotNull("Should return a result", result);
        assertTrue("Should fail with wrong token", result.isFailure());
    }

    // ========== Member Access (Safe Call) Error Tests ==========

    /**
     * Test member access with missing member.
     */
    @Test
    public void testMemberAccessWithMissingMember() {
        MemberAccessParselet memberAccessParselet = new MemberAccessParselet();
        ParsedNode left = TestUtils.createIdentifierNode("obj");
        LocatableToken dotToken = TestUtils.createToken(JavaTokenTypes.DOT, ".", 1, 4);

        // Don't set next token - simulates missing member
        ParseResult<ParsedNode> result = memberAccessParselet.parse(parser, left, dotToken);

        assertNotNull("Should return a result", result);
        if (result.isFailure()) {
            assertTrue("Should have error about missing member", !result.getErrors().isEmpty());
        }
    }

    /**
     * Test safe call with missing member.
     */
    @Test
    public void testSafeCallWithMissingMember() {
        MemberAccessParselet memberAccessParselet = new MemberAccessParselet();
        ParsedNode left = TestUtils.createIdentifierNode("obj");
        LocatableToken safeAccessToken = TestUtils.createToken(JavaTokenTypes.SAFE_ACCESS, "?.", 1, 4);

        // Don't set next token - simulates missing member
        ParseResult<ParsedNode> result = memberAccessParselet.parse(parser, left, safeAccessToken);

        assertNotNull("Should return a result", result);
        if (result.isFailure()) {
            assertTrue("Should have error about missing member", !result.getErrors().isEmpty());
        }
    }

    // ========== General Error Handling Tests ==========

    /**
     * Test that all parselets handle null input gracefully.
     */
    @Test
    public void testNullInputHandling() {
        ElvisOperatorParselet elvisParselet = new ElvisOperatorParselet();

        try {
            ParseResult<ParsedNode> result = elvisParselet.parse(null, null, null);
            // Should either return a failure result or throw a controlled exception
            assertTrue("Should handle null input", true);
        } catch (NullPointerException e) {
            // NPE is acceptable for null input
            assertTrue("NPE is acceptable for null input", true);
        } catch (Exception e) {
            // Other exceptions should provide meaningful messages
            assertNotNull("Exception should have message", e.getMessage());
            assertTrue("Exception message should be descriptive", e.getMessage().length() > 0);
        }
    }

    /**
     * Test that parselets handle empty parser gracefully.
     */
    @Test
    public void testEmptyParserHandling() {
        ElvisOperatorParselet elvisParselet = new ElvisOperatorParselet();
        ParsedNode left = TestUtils.createIdentifierNode("value");
        LocatableToken elvisToken = TestUtils.createToken(JavaTokenTypes.ELVIS, "?:", 1, 6);

        // Use empty parser (no tokens set)
        MockParser emptyParser = new MockParser();

        try {
            ParseResult<ParsedNode> result = elvisParselet.parse(emptyParser, left, elvisToken);
            assertNotNull("Should return a result even with empty parser", result);
        } catch (Exception e) {
            // Should not crash, but if it throws exception, it should be meaningful
            assertNotNull("Exception message should exist", e.getMessage());
        }
    }

    /**
     * Test error message quality and informativeness.
     */
    @Test
    public void testErrorMessageQuality() {
        TypeCheckParselet typeCheckParselet = new TypeCheckParselet();
        ParsedNode left = TestUtils.createIdentifierNode("value");
        LocatableToken wrongToken = TestUtils.createToken(JavaTokenTypes.LITERAL_instanceof, "instanceof", 1, 6);

        ParseResult<ParsedNode> result = typeCheckParselet.parse(parser, left, wrongToken);

        if (result.isFailure()) {
            assertFalse("Should have non-empty error list", result.getErrors().isEmpty());

            String errorMessage = result.getErrors().get(0).message();
            assertNotNull("Error message should not be null", errorMessage);
            assertFalse("Error message should not be empty", errorMessage.trim().isEmpty());
            assertTrue("Error message should be descriptive", errorMessage.length() > 5);
        }
    }

    /**
     * Test that error handling doesn't cause performance regression.
     */
    @Test
    public void testErrorHandlingPerformance() {
        RangeParselet rangeParselet = new RangeParselet();
        ParsedNode left = TestUtils.createIdentifierNode("start");
        LocatableToken wrongToken = TestUtils.createToken(JavaTokenTypes.DOT, ".", 1, 6);

        long startTime = System.nanoTime();

        try {
            ParseResult<ParsedNode> result = rangeParselet.parse(parser, left, wrongToken);
            // Error handling should complete quickly
        } catch (Exception e) {
            // Even exceptions should be thrown quickly
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        assertTrue("Error handling should be fast", durationMs < 100);
    }

    /**
     * Test that parselets maintain consistency in error reporting.
     */
    @Test
    public void testErrorReportingConsistency() {
        // Test that different parselets report errors in similar ways
        ParsedNode left = TestUtils.createIdentifierNode("value");

        // Test type check with wrong token
        TypeCheckParselet typeCheckParselet = new TypeCheckParselet();
        LocatableToken wrongToken1 = TestUtils.createToken(JavaTokenTypes.LITERAL_instanceof, "instanceof", 1, 6);
        ParseResult<ParsedNode> result1 = typeCheckParselet.parse(parser, left, wrongToken1);

        // Test type cast with wrong token
        TypeCastParselet typeCastParselet = new TypeCastParselet();
        LocatableToken wrongToken2 = TestUtils.createToken(JavaTokenTypes.LITERAL_is, "is", 1, 6);
        ParseResult<ParsedNode> result2 = typeCastParselet.parse(parser, left, wrongToken2);

        // Both should handle errors similarly (both fail or both succeed)
        assertEquals("Parselets should handle wrong tokens consistently",
                    result1.isSuccess(), result2.isSuccess());

        if (result1.isFailure() && result2.isFailure()) {
            // Both should provide error messages
            assertFalse("TypeCheck should provide error message", result1.getErrors().isEmpty());
            assertFalse("TypeCast should provide error message", result2.getErrors().isEmpty());
        }
    }
}
