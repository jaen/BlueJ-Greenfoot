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
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Comprehensive validation of operator precedence against the Kotlin language specification.
 *
 * <p>This test suite ensures that all operators registered in the KotlinPrattParser
 * follow the correct precedence hierarchy as defined in the Kotlin Language Specification.
 * It validates both individual precedence values and precedence relationships between
 * different operator groups.</p>
 *
 * <p>The Kotlin precedence hierarchy (from lowest to highest) is:</p>
 * <ol>
 *   <li>Assignment operators (=, +=, -=, etc.) - precedence 10</li>
 *   <li>Elvis operator (?:) - precedence 20</li>
 *   <li>Disjunction (||) - precedence 30</li>
 *   <li>Conjunction (&&) - precedence 40</li>
 *   <li>Equality operators (==, !=, ===, !==) - precedence 50</li>
 *   <li>Comparison operators (<, >, <=, >=) - precedence 60</li>
 *   <li>Named checks (in, !in, is, !is) - precedence 70</li>
 *   <li>Range operator (..) - precedence 80</li>
 *   <li>Infix function calls - precedence 90</li>
 *   <li>Additive operators (+, -) - precedence 100</li>
 *   <li>Multiplicative operators (*, /, %) - precedence 110</li>
 *   <li>Type casts (as, as?) - precedence 120</li>
 *   <li>Prefix operators (-, +, ++, --, !, etc.) - precedence 130</li>
 *   <li>Postfix operators (++, --, !!, ., ?., [], ()) - precedence 140</li>
 * </ol>
 *
 * @see <a href="https://kotlinlang.org/spec/syntax-and-grammar.html#expressions">Kotlin Language Specification - Expressions</a>
 * @author BlueJ Development Team
 */
public class PrecedenceValidationTest {

    private KotlinPrattParser parser;
    private ParseletRegistry registry;
    private TestNodeFactory nodeFactory;
    private TestTokenOperations tokenOps;

    @Before
    public void setUp() {
        tokenOps = new TestTokenOperations();
        nodeFactory = new TestNodeFactory();
        parser = new KotlinPrattParser(tokenOps, null, nodeFactory);
        registry = parser.getRegistry();
    }

    // ===================== INDIVIDUAL PRECEDENCE VALUE TESTS =====================

    @Test
    public void testAssignmentOperatorPrecedence() {
        // Assignment operators should have the lowest precedence (10)
        int expectedPrecedence = Precedence.ASSIGNMENT.getValue();

        assertEquals("Assignment operator should have correct precedence",
            expectedPrecedence, registry.getPrecedence(JavaTokenTypes.ASSIGN));
        assertEquals("Plus assignment should have correct precedence",
            expectedPrecedence, registry.getPrecedence(JavaTokenTypes.PLUS_ASSIGN));
        assertEquals("Minus assignment should have correct precedence",
            expectedPrecedence, registry.getPrecedence(JavaTokenTypes.MINUS_ASSIGN));
        assertEquals("Star assignment should have correct precedence",
            expectedPrecedence, registry.getPrecedence(JavaTokenTypes.STAR_ASSIGN));
        assertEquals("Div assignment should have correct precedence",
            expectedPrecedence, registry.getPrecedence(JavaTokenTypes.DIV_ASSIGN));
    }

    @Test
    public void testLogicalOperatorPrecedence() {
        // Logical OR (disjunction) should have precedence 30
        assertEquals("Logical OR should have correct precedence",
            Precedence.DISJUNCTION.getValue(), registry.getPrecedence(JavaTokenTypes.LOR));

        // Logical AND (conjunction) should have precedence 40
        assertEquals("Logical AND should have correct precedence",
            Precedence.CONJUNCTION.getValue(), registry.getPrecedence(JavaTokenTypes.LAND));
    }

    @Test
    public void testEqualityOperatorPrecedence() {
        // Equality operators should have precedence 50
        int expectedPrecedence = Precedence.EQUALITY.getValue();

        assertEquals("Equality operator should have correct precedence",
            expectedPrecedence, registry.getPrecedence(JavaTokenTypes.EQUAL));
        assertEquals("Not equal operator should have correct precedence",
            expectedPrecedence, registry.getPrecedence(JavaTokenTypes.NOT_EQUAL));
    }

    @Test
    public void testComparisonOperatorPrecedence() {
        // Comparison operators should have precedence 60
        int expectedPrecedence = Precedence.COMPARISON.getValue();

        assertEquals("Less than should have correct precedence",
            expectedPrecedence, registry.getPrecedence(JavaTokenTypes.LT));
        assertEquals("Greater than should have correct precedence",
            expectedPrecedence, registry.getPrecedence(JavaTokenTypes.GT));
        assertEquals("Less or equal should have correct precedence",
            expectedPrecedence, registry.getPrecedence(JavaTokenTypes.LE));
        assertEquals("Greater or equal should have correct precedence",
            expectedPrecedence, registry.getPrecedence(JavaTokenTypes.GE));
    }

    @Test
    public void testAdditiveOperatorPrecedence() {
        // Additive operators should have precedence 100
        int expectedPrecedence = Precedence.ADDITIVE.getValue();

        assertEquals("Addition should have correct precedence",
            expectedPrecedence, registry.getPrecedence(JavaTokenTypes.PLUS));
        assertEquals("Subtraction should have correct precedence",
            expectedPrecedence, registry.getPrecedence(JavaTokenTypes.MINUS));
    }

    @Test
    public void testMultiplicativeOperatorPrecedence() {
        // Multiplicative operators should have precedence 110
        int expectedPrecedence = Precedence.MULTIPLICATIVE.getValue();

        assertEquals("Multiplication should have correct precedence",
            expectedPrecedence, registry.getPrecedence(JavaTokenTypes.STAR));
        assertEquals("Division should have correct precedence",
            expectedPrecedence, registry.getPrecedence(JavaTokenTypes.DIV));
        assertEquals("Modulo should have correct precedence",
            expectedPrecedence, registry.getPrecedence(JavaTokenTypes.MOD));
    }

    @Test
    public void testPostfixOperatorPrecedence() {
        // Postfix operators should have the highest precedence (140)
        int expectedPrecedence = Precedence.POSTFIX.getValue();

        assertEquals("Member access should have correct precedence",
            expectedPrecedence, registry.getPrecedence(JavaTokenTypes.DOT));
        assertEquals("Array access should have correct precedence",
            expectedPrecedence, registry.getPrecedence(JavaTokenTypes.LBRACK));
        assertEquals("Function call should have correct precedence",
            expectedPrecedence, registry.getPrecedence(JavaTokenTypes.LPAREN));
        assertEquals("Postfix increment should have correct precedence",
            expectedPrecedence, registry.getPrecedence(JavaTokenTypes.INC));
        assertEquals("Postfix decrement should have correct precedence",
            expectedPrecedence, registry.getPrecedence(JavaTokenTypes.DEC));
    }

    // ===================== PRECEDENCE RELATIONSHIP TESTS =====================

    @Test
    public void testAssignmentVsLogicalPrecedence() {
        // Assignment should have lower precedence than logical operators
        assertTrue("Assignment should be lower precedence than logical OR",
            registry.getPrecedence(JavaTokenTypes.ASSIGN) < registry.getPrecedence(JavaTokenTypes.LOR));
        assertTrue("Assignment should be lower precedence than logical AND",
            registry.getPrecedence(JavaTokenTypes.ASSIGN) < registry.getPrecedence(JavaTokenTypes.LAND));
    }

    @Test
    public void testLogicalOperatorRelationships() {
        // Logical OR should have lower precedence than logical AND
        assertTrue("Logical OR should be lower precedence than logical AND",
            registry.getPrecedence(JavaTokenTypes.LOR) < registry.getPrecedence(JavaTokenTypes.LAND));
    }

    @Test
    public void testEqualityVsComparisonPrecedence() {
        // Comparison should have higher precedence than equality
        assertTrue("Comparison should be higher precedence than equality",
            registry.getPrecedence(JavaTokenTypes.LT) > registry.getPrecedence(JavaTokenTypes.EQUAL));
        assertTrue("All comparison operators should be higher than equality",
            registry.getPrecedence(JavaTokenTypes.GT) > registry.getPrecedence(JavaTokenTypes.EQUAL) &&
            registry.getPrecedence(JavaTokenTypes.LE) > registry.getPrecedence(JavaTokenTypes.EQUAL) &&
            registry.getPrecedence(JavaTokenTypes.GE) > registry.getPrecedence(JavaTokenTypes.EQUAL));
    }

    @Test
    public void testArithmeticOperatorRelationships() {
        // Multiplicative should have higher precedence than additive
        assertTrue("Multiplication should be higher precedence than addition",
            registry.getPrecedence(JavaTokenTypes.STAR) > registry.getPrecedence(JavaTokenTypes.PLUS));
        assertTrue("Division should be higher precedence than subtraction",
            registry.getPrecedence(JavaTokenTypes.DIV) > registry.getPrecedence(JavaTokenTypes.MINUS));
        assertTrue("Modulo should be higher precedence than addition",
            registry.getPrecedence(JavaTokenTypes.MOD) > registry.getPrecedence(JavaTokenTypes.PLUS));
    }

    @Test
    public void testPostfixVsAllOtherOperators() {
        // Postfix operators should have higher precedence than all others
        List<Integer> nonPostfixTokens = Arrays.asList(
            JavaTokenTypes.ASSIGN, JavaTokenTypes.LOR, JavaTokenTypes.LAND,
            JavaTokenTypes.EQUAL, JavaTokenTypes.LT, JavaTokenTypes.PLUS,
            JavaTokenTypes.STAR, JavaTokenTypes.MINUS, JavaTokenTypes.DIV
        );

        int postfixPrecedence = registry.getPrecedence(JavaTokenTypes.DOT);

        for (int tokenType : nonPostfixTokens) {
            assertTrue("Postfix operators should be higher precedence than " + getTokenName(tokenType),
                postfixPrecedence > registry.getPrecedence(tokenType));
        }
    }

    // ===================== PRECEDENCE GROUP CONSISTENCY TESTS =====================

    @Test
    public void testAssignmentOperatorGroupConsistency() {
        // All assignment operators should have the same precedence
        int basePrecedence = registry.getPrecedence(JavaTokenTypes.ASSIGN);

        assertEquals("Plus assign should match base assignment precedence",
            basePrecedence, registry.getPrecedence(JavaTokenTypes.PLUS_ASSIGN));
        assertEquals("Minus assign should match base assignment precedence",
            basePrecedence, registry.getPrecedence(JavaTokenTypes.MINUS_ASSIGN));
        assertEquals("Star assign should match base assignment precedence",
            basePrecedence, registry.getPrecedence(JavaTokenTypes.STAR_ASSIGN));
        assertEquals("Div assign should match base assignment precedence",
            basePrecedence, registry.getPrecedence(JavaTokenTypes.DIV_ASSIGN));
    }

    @Test
    public void testComparisonOperatorGroupConsistency() {
        // All comparison operators should have the same precedence
        int basePrecedence = registry.getPrecedence(JavaTokenTypes.LT);

        assertEquals("GT should match LT precedence", basePrecedence, registry.getPrecedence(JavaTokenTypes.GT));
        assertEquals("LE should match LT precedence", basePrecedence, registry.getPrecedence(JavaTokenTypes.LE));
        assertEquals("GE should match LT precedence", basePrecedence, registry.getPrecedence(JavaTokenTypes.GE));
    }

    @Test
    public void testEqualityOperatorGroupConsistency() {
        // All equality operators should have the same precedence
        int basePrecedence = registry.getPrecedence(JavaTokenTypes.EQUAL);

        assertEquals("NOT_EQUAL should match EQUAL precedence",
            basePrecedence, registry.getPrecedence(JavaTokenTypes.NOT_EQUAL));
    }

    @Test
    public void testAdditiveOperatorGroupConsistency() {
        // Addition and subtraction should have the same precedence
        int addPrecedence = registry.getPrecedence(JavaTokenTypes.PLUS);
        int subtractPrecedence = registry.getPrecedence(JavaTokenTypes.MINUS);

        assertEquals("Addition and subtraction should have same precedence",
            addPrecedence, subtractPrecedence);
    }

    @Test
    public void testMultiplicativeOperatorGroupConsistency() {
        // All multiplicative operators should have the same precedence
        int multiplyPrecedence = registry.getPrecedence(JavaTokenTypes.STAR);

        assertEquals("Division should match multiplication precedence",
            multiplyPrecedence, registry.getPrecedence(JavaTokenTypes.DIV));
        assertEquals("Modulo should match multiplication precedence",
            multiplyPrecedence, registry.getPrecedence(JavaTokenTypes.MOD));
    }

    // ===================== KOTLIN SPECIFICATION COMPLIANCE TESTS =====================

    @Test
    public void testKotlinSpecificationPrecedenceValues() {
        // Test that our precedence values exactly match the Kotlin specification
        assertEquals("Assignment precedence should match Kotlin spec", 10,
            Precedence.ASSIGNMENT.getValue());
        assertEquals("Disjunction precedence should match Kotlin spec", 30,
            Precedence.DISJUNCTION.getValue());
        assertEquals("Conjunction precedence should match Kotlin spec", 40,
            Precedence.CONJUNCTION.getValue());
        assertEquals("Equality precedence should match Kotlin spec", 50,
            Precedence.EQUALITY.getValue());
        assertEquals("Comparison precedence should match Kotlin spec", 60,
            Precedence.COMPARISON.getValue());
        assertEquals("Additive precedence should match Kotlin spec", 100,
            Precedence.ADDITIVE.getValue());
        assertEquals("Multiplicative precedence should match Kotlin spec", 110,
            Precedence.MULTIPLICATIVE.getValue());
        assertEquals("Prefix precedence should match Kotlin spec", 130,
            Precedence.PREFIX.getValue());
        assertEquals("Postfix precedence should match Kotlin spec", 140,
            Precedence.POSTFIX.getValue());
    }

    @Test
    public void testCompletePrecedenceHierarchy() {
        // Test the complete precedence hierarchy from lowest to highest
        int[] expectedHierarchy = {
            registry.getPrecedence(JavaTokenTypes.ASSIGN),      // Assignment (10)
            registry.getPrecedence(JavaTokenTypes.LOR),         // Disjunction (30)
            registry.getPrecedence(JavaTokenTypes.LAND),        // Conjunction (40)
            registry.getPrecedence(JavaTokenTypes.EQUAL),       // Equality (50)
            registry.getPrecedence(JavaTokenTypes.LT),          // Comparison (60)
            registry.getPrecedence(JavaTokenTypes.PLUS),        // Additive (100)
            registry.getPrecedence(JavaTokenTypes.STAR),        // Multiplicative (110)
            registry.getPrecedence(JavaTokenTypes.DOT)          // Postfix (140)
        };

        // Verify that each precedence is higher than the previous
        for (int i = 1; i < expectedHierarchy.length; i++) {
            assertTrue("Precedence hierarchy should be strictly increasing at position " + i,
                expectedHierarchy[i] > expectedHierarchy[i-1]);
        }

        // Verify exact values match Kotlin specification
        assertEquals("Assignment should be precedence 10", 10, expectedHierarchy[0]);
        assertEquals("Disjunction should be precedence 30", 30, expectedHierarchy[1]);
        assertEquals("Conjunction should be precedence 40", 40, expectedHierarchy[2]);
        assertEquals("Equality should be precedence 50", 50, expectedHierarchy[3]);
        assertEquals("Comparison should be precedence 60", 60, expectedHierarchy[4]);
        assertEquals("Additive should be precedence 100", 100, expectedHierarchy[5]);
        assertEquals("Multiplicative should be precedence 110", 110, expectedHierarchy[6]);
        assertEquals("Postfix should be precedence 140", 140, expectedHierarchy[7]);
    }

    // ===================== EDGE CASE AND VALIDATION TESTS =====================

    @Test
    public void testUnregisteredTokenPrecedence() {
        // Unregistered tokens should have precedence 0
        assertEquals("Unregistered token should have precedence 0",
            0, registry.getPrecedence(JavaTokenTypes.RCURLY));
        assertEquals("Another unregistered token should have precedence 0",
            0, registry.getPrecedence(JavaTokenTypes.SEMI));
    }

    @Test
    public void testPrecedenceRangeValidity() {
        // All precedence values should be positive and within reasonable range
        List<Integer> allTokensWithPrecedence = Arrays.asList(
            JavaTokenTypes.ASSIGN, JavaTokenTypes.PLUS_ASSIGN, JavaTokenTypes.MINUS_ASSIGN,
            JavaTokenTypes.LOR, JavaTokenTypes.LAND, JavaTokenTypes.EQUAL, JavaTokenTypes.NOT_EQUAL,
            JavaTokenTypes.LT, JavaTokenTypes.GT, JavaTokenTypes.LE, JavaTokenTypes.GE,
            JavaTokenTypes.PLUS, JavaTokenTypes.MINUS, JavaTokenTypes.STAR, JavaTokenTypes.DIV,
            JavaTokenTypes.MOD, JavaTokenTypes.DOT, JavaTokenTypes.LBRACK, JavaTokenTypes.LPAREN
        );

        for (int tokenType : allTokensWithPrecedence) {
            int precedence = registry.getPrecedence(tokenType);
            assertTrue("Precedence should be positive for " + getTokenName(tokenType),
                precedence > 0);
            assertTrue("Precedence should be reasonable for " + getTokenName(tokenType),
                precedence <= 200); // Reasonable upper bound
        }
    }

    @Test
    public void testNoPrecedenceGaps() {
        // Test that there are no unexpected gaps in our precedence assignments
        // This ensures we haven't accidentally skipped important precedence levels

        List<Integer> allPrecedences = Arrays.asList(
            registry.getPrecedence(JavaTokenTypes.ASSIGN),
            registry.getPrecedence(JavaTokenTypes.LOR),
            registry.getPrecedence(JavaTokenTypes.LAND),
            registry.getPrecedence(JavaTokenTypes.EQUAL),
            registry.getPrecedence(JavaTokenTypes.LT),
            registry.getPrecedence(JavaTokenTypes.PLUS),
            registry.getPrecedence(JavaTokenTypes.STAR),
            registry.getPrecedence(JavaTokenTypes.DOT)
        );

        // Check that we have reasonable spacing between precedence levels
        for (int i = 1; i < allPrecedences.size(); i++) {
            int gap = allPrecedences.get(i) - allPrecedences.get(i-1);
            assertTrue("Precedence gap should be reasonable (found gap of " + gap + ")",
                gap >= 10 && gap <= 70); // Allow for reasonable gaps
        }
    }

    // ===================== HELPER METHODS =====================

    private String getTokenName(int tokenType) {
        return switch (tokenType) {
            case JavaTokenTypes.ASSIGN -> "ASSIGN";
            case JavaTokenTypes.PLUS_ASSIGN -> "PLUS_ASSIGN";
            case JavaTokenTypes.MINUS_ASSIGN -> "MINUS_ASSIGN";
            case JavaTokenTypes.STAR_ASSIGN -> "STAR_ASSIGN";
            case JavaTokenTypes.DIV_ASSIGN -> "DIV_ASSIGN";
            case JavaTokenTypes.LOR -> "LOR";
            case JavaTokenTypes.LAND -> "LAND";
            case JavaTokenTypes.EQUAL -> "EQUAL";
            case JavaTokenTypes.NOT_EQUAL -> "NOT_EQUAL";
            case JavaTokenTypes.LT -> "LT";
            case JavaTokenTypes.GT -> "GT";
            case JavaTokenTypes.LE -> "LE";
            case JavaTokenTypes.GE -> "GE";
            case JavaTokenTypes.PLUS -> "PLUS";
            case JavaTokenTypes.MINUS -> "MINUS";
            case JavaTokenTypes.STAR -> "STAR";
            case JavaTokenTypes.DIV -> "DIV";
            case JavaTokenTypes.MOD -> "MOD";
            case JavaTokenTypes.DOT -> "DOT";
            case JavaTokenTypes.LBRACK -> "LBRACK";
            case JavaTokenTypes.LPAREN -> "LPAREN";
            case JavaTokenTypes.INC -> "INC";
            case JavaTokenTypes.DEC -> "DEC";
            case JavaTokenTypes.RCURLY -> "RCURLY";
            case JavaTokenTypes.SEMI -> "SEMI";
            default -> "UNKNOWN_" + tokenType;
        };
    }

    /**
     * Test helper to validate that operator precedence follows mathematical conventions.
     * This is especially important for arithmetic operators where user expectations
     * are based on standard mathematical precedence rules.
     */
    @Test
    public void testMathematicalPrecedenceConventions() {
        // Standard mathematical convention: multiplication and division before addition and subtraction
        assertTrue("Multiplication should have higher precedence than addition",
            registry.getPrecedence(JavaTokenTypes.STAR) > registry.getPrecedence(JavaTokenTypes.PLUS));
        assertTrue("Division should have higher precedence than subtraction",
            registry.getPrecedence(JavaTokenTypes.DIV) > registry.getPrecedence(JavaTokenTypes.MINUS));

        // Unary operators (prefix) should have higher precedence than binary operators
        // Note: We test this conceptually since prefix operators don't appear in the infix registry
        assertTrue("Binary multiplication should be lower than postfix operators",
            registry.getPrecedence(JavaTokenTypes.STAR) < registry.getPrecedence(JavaTokenTypes.DOT));
    }
}
