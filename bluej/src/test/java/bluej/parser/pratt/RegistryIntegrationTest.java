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
import bluej.parser.pratt.parselets.ArrayAccessParselet;
import bluej.parser.pratt.parselets.BinaryOperatorParselet;
import bluej.parser.pratt.parselets.CallParselet;
import bluej.parser.pratt.parselets.GroupParselet;
import bluej.parser.pratt.parselets.LiteralParselet;
import bluej.parser.pratt.parselets.MemberAccessParselet;
import bluej.parser.pratt.parselets.NameParselet;
import bluej.parser.pratt.parselets.PostfixOperatorParselet;
import bluej.parser.pratt.parselets.PrefixOperatorParselet;
import bluej.parser.pratt.parselets.SuperParselet;
import bluej.parser.pratt.parselets.ThisParselet;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Comprehensive integration tests for the ParseletRegistry and KotlinPrattParser registry initialization.
 *
 * <p>This test suite validates that all parselets are properly registered in the parser registry
 * and can be retrieved with correct precedence values. It ensures that the registry integration
 * is complete and follows Kotlin operator precedence rules.</p>
 *
 * @author BlueJ Development Team
 */
public class RegistryIntegrationTest {

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

    // ===================== PREFIX PARSELET TESTS =====================

    @Test
    public void testLiteralParseletsRegistered() {
        // Test all literal types have the same LiteralParselet instance
        PrefixParselet intParselet = registry.getPrefix(JavaTokenTypes.NUM_INT);
        PrefixParselet longParselet = registry.getPrefix(JavaTokenTypes.NUM_LONG);
        PrefixParselet floatParselet = registry.getPrefix(JavaTokenTypes.NUM_FLOAT);
        PrefixParselet doubleParselet = registry.getPrefix(JavaTokenTypes.NUM_DOUBLE);
        PrefixParselet stringParselet = registry.getPrefix(JavaTokenTypes.STRING_LITERAL);
        PrefixParselet multiStringParselet = registry.getPrefix(JavaTokenTypes.STRING_LITERAL_MULTILINE);
        PrefixParselet charParselet = registry.getPrefix(JavaTokenTypes.CHAR_LITERAL);
        PrefixParselet trueParselet = registry.getPrefix(JavaTokenTypes.LITERAL_true);
        PrefixParselet falseParselet = registry.getPrefix(JavaTokenTypes.LITERAL_false);
        PrefixParselet nullParselet = registry.getPrefix(JavaTokenTypes.LITERAL_null);

        // All should be non-null and instances of LiteralParselet
        assertNotNull("Integer literal parselet should be registered", intParselet);
        assertNotNull("Long literal parselet should be registered", longParselet);
        assertNotNull("Float literal parselet should be registered", floatParselet);
        assertNotNull("Double literal parselet should be registered", doubleParselet);
        assertNotNull("String literal parselet should be registered", stringParselet);
        assertNotNull("Multiline string literal parselet should be registered", multiStringParselet);
        assertNotNull("Character literal parselet should be registered", charParselet);
        assertNotNull("True literal parselet should be registered", trueParselet);
        assertNotNull("False literal parselet should be registered", falseParselet);
        assertNotNull("Null literal parselet should be registered", nullParselet);

        assertTrue("Integer parselet should be LiteralParselet", intParselet instanceof LiteralParselet);
        assertTrue("String parselet should be LiteralParselet", stringParselet instanceof LiteralParselet);
        assertTrue("Boolean parselet should be LiteralParselet", trueParselet instanceof LiteralParselet);
    }

    @Test
    public void testIdentifierParseletsRegistered() {
        // Test identifier parselets
        PrefixParselet identParselet = registry.getPrefix(JavaTokenTypes.IDENT);
        PrefixParselet thisParselet = registry.getPrefix(JavaTokenTypes.LITERAL_this);
        PrefixParselet superParselet = registry.getPrefix(JavaTokenTypes.LITERAL_super);

        assertNotNull("Identifier parselet should be registered", identParselet);
        assertNotNull("This parselet should be registered", thisParselet);
        assertNotNull("Super parselet should be registered", superParselet);

        assertTrue("Identifier parselet should be NameParselet", identParselet instanceof NameParselet);
        assertTrue("This parselet should be ThisParselet", thisParselet instanceof ThisParselet);
        assertTrue("Super parselet should be SuperParselet", superParselet instanceof SuperParselet);
    }

    @Test
    public void testGroupParseletsRegistered() {
        // Test grouping parselet
        PrefixParselet groupParselet = registry.getPrefix(JavaTokenTypes.LPAREN);

        assertNotNull("Group parselet should be registered for LPAREN", groupParselet);
        assertTrue("Group parselet should be GroupParselet", groupParselet instanceof GroupParselet);
    }

    @Test
    public void testPrefixOperatorParseletsRegistered() {
        // Test prefix operator parselets
        PrefixParselet prefixPlus = registry.getPrefix(JavaTokenTypes.PLUS);
        PrefixParselet prefixMinus = registry.getPrefix(JavaTokenTypes.MINUS);
        PrefixParselet logicalNot = registry.getPrefix(JavaTokenTypes.LNOT);
        PrefixParselet prefixInc = registry.getPrefix(JavaTokenTypes.INC);
        PrefixParselet prefixDec = registry.getPrefix(JavaTokenTypes.DEC);

        assertNotNull("Prefix plus parselet should be registered", prefixPlus);
        assertNotNull("Prefix minus parselet should be registered", prefixMinus);
        assertNotNull("Logical not parselet should be registered", logicalNot);
        assertNotNull("Prefix increment parselet should be registered", prefixInc);
        assertNotNull("Prefix decrement parselet should be registered", prefixDec);

        assertTrue("Prefix plus should be PrefixOperatorParselet", prefixPlus instanceof PrefixOperatorParselet);
        assertTrue("Prefix minus should be PrefixOperatorParselet", prefixMinus instanceof PrefixOperatorParselet);
        assertTrue("Logical not should be PrefixOperatorParselet", logicalNot instanceof PrefixOperatorParselet);
        assertTrue("Prefix increment should be PrefixOperatorParselet", prefixInc instanceof PrefixOperatorParselet);
        assertTrue("Prefix decrement should be PrefixOperatorParselet", prefixDec instanceof PrefixOperatorParselet);
    }

    // ===================== INFIX PARSELET TESTS =====================

    @Test
    public void testBinaryOperatorParseletsRegistered() {
        // Test multiplicative operators
        InfixParselet multiply = registry.getInfix(JavaTokenTypes.STAR);
        InfixParselet divide = registry.getInfix(JavaTokenTypes.DIV);
        InfixParselet modulo = registry.getInfix(JavaTokenTypes.MOD);

        assertNotNull("Multiply parselet should be registered", multiply);
        assertNotNull("Divide parselet should be registered", divide);
        assertNotNull("Modulo parselet should be registered", modulo);

        assertTrue("Multiply should be BinaryOperatorParselet", multiply instanceof BinaryOperatorParselet);
        assertTrue("Divide should be BinaryOperatorParselet", divide instanceof BinaryOperatorParselet);
        assertTrue("Modulo should be BinaryOperatorParselet", modulo instanceof BinaryOperatorParselet);

        // Test additive operators
        InfixParselet add = registry.getInfix(JavaTokenTypes.PLUS);
        InfixParselet subtract = registry.getInfix(JavaTokenTypes.MINUS);

        assertNotNull("Add parselet should be registered", add);
        assertNotNull("Subtract parselet should be registered", subtract);

        assertTrue("Add should be BinaryOperatorParselet", add instanceof BinaryOperatorParselet);
        assertTrue("Subtract should be BinaryOperatorParselet", subtract instanceof BinaryOperatorParselet);

        // Test comparison operators
        InfixParselet lessThan = registry.getInfix(JavaTokenTypes.LT);
        InfixParselet greaterThan = registry.getInfix(JavaTokenTypes.GT);
        InfixParselet lessEqual = registry.getInfix(JavaTokenTypes.LE);
        InfixParselet greaterEqual = registry.getInfix(JavaTokenTypes.GE);

        assertNotNull("Less than parselet should be registered", lessThan);
        assertNotNull("Greater than parselet should be registered", greaterThan);
        assertNotNull("Less equal parselet should be registered", lessEqual);
        assertNotNull("Greater equal parselet should be registered", greaterEqual);

        // Test equality operators
        InfixParselet equal = registry.getInfix(JavaTokenTypes.EQUAL);
        InfixParselet notEqual = registry.getInfix(JavaTokenTypes.NOT_EQUAL);

        assertNotNull("Equal parselet should be registered", equal);
        assertNotNull("Not equal parselet should be registered", notEqual);

        // Test logical operators
        InfixParselet logicalAnd = registry.getInfix(JavaTokenTypes.LAND);
        InfixParselet logicalOr = registry.getInfix(JavaTokenTypes.LOR);

        assertNotNull("Logical AND parselet should be registered", logicalAnd);
        assertNotNull("Logical OR parselet should be registered", logicalOr);

        // Test assignment operators
        InfixParselet assign = registry.getInfix(JavaTokenTypes.ASSIGN);
        InfixParselet plusAssign = registry.getInfix(JavaTokenTypes.PLUS_ASSIGN);
        InfixParselet minusAssign = registry.getInfix(JavaTokenTypes.MINUS_ASSIGN);
        InfixParselet starAssign = registry.getInfix(JavaTokenTypes.STAR_ASSIGN);
        InfixParselet divAssign = registry.getInfix(JavaTokenTypes.DIV_ASSIGN);

        assertNotNull("Assign parselet should be registered", assign);
        assertNotNull("Plus assign parselet should be registered", plusAssign);
        assertNotNull("Minus assign parselet should be registered", minusAssign);
        assertNotNull("Star assign parselet should be registered", starAssign);
        assertNotNull("Div assign parselet should be registered", divAssign);
    }

    @Test
    public void testPostfixOperatorParseletsRegistered() {
        // Test postfix increment/decrement
        InfixParselet postfixInc = registry.getInfix(JavaTokenTypes.INC);
        InfixParselet postfixDec = registry.getInfix(JavaTokenTypes.DEC);

        assertNotNull("Postfix increment parselet should be registered", postfixInc);
        assertNotNull("Postfix decrement parselet should be registered", postfixDec);

        assertTrue("Postfix increment should be PostfixOperatorParselet", postfixInc instanceof PostfixOperatorParselet);
        assertTrue("Postfix decrement should be PostfixOperatorParselet", postfixDec instanceof PostfixOperatorParselet);
    }

    @Test
    public void testAccessAndCallParseletsRegistered() {
        // Test member access
        InfixParselet memberAccess = registry.getInfix(JavaTokenTypes.DOT);

        assertNotNull("Member access parselet should be registered", memberAccess);
        assertTrue("Member access should be MemberAccessParselet", memberAccess instanceof MemberAccessParselet);

        // Test array access
        InfixParselet arrayAccess = registry.getInfix(JavaTokenTypes.LBRACK);

        assertNotNull("Array access parselet should be registered", arrayAccess);
        assertTrue("Array access should be ArrayAccessParselet", arrayAccess instanceof ArrayAccessParselet);

        // Test function calls
        InfixParselet functionCall = registry.getInfix(JavaTokenTypes.LPAREN);

        assertNotNull("Function call parselet should be registered", functionCall);
        assertTrue("Function call should be CallParselet", functionCall instanceof CallParselet);
    }

    // ===================== PRECEDENCE TESTS =====================

    @Test
    public void testOperatorPrecedenceHierarchy() {
        // Test that precedence levels follow Kotlin specification order
        // From lowest to highest precedence

        // Assignment operators should have lowest precedence
        assertEquals("Assignment should have correct precedence",
            Precedence.ASSIGNMENT.getValue(), registry.getPrecedence(JavaTokenTypes.ASSIGN));

        // Logical OR should be higher than assignment
        assertEquals("Logical OR should have correct precedence",
            Precedence.DISJUNCTION.getValue(), registry.getPrecedence(JavaTokenTypes.LOR));

        // Logical AND should be higher than OR
        assertEquals("Logical AND should have correct precedence",
            Precedence.CONJUNCTION.getValue(), registry.getPrecedence(JavaTokenTypes.LAND));

        // Equality should be higher than logical operators
        assertEquals("Equality should have correct precedence",
            Precedence.EQUALITY.getValue(), registry.getPrecedence(JavaTokenTypes.EQUAL));

        // Comparison should be higher than equality
        assertEquals("Comparison should have correct precedence",
            Precedence.COMPARISON.getValue(), registry.getPrecedence(JavaTokenTypes.LT));

        // Additive should be higher than comparison
        assertEquals("Additive should have correct precedence",
            Precedence.ADDITIVE.getValue(), registry.getPrecedence(JavaTokenTypes.PLUS));

        // Multiplicative should be higher than additive
        assertEquals("Multiplicative should have correct precedence",
            Precedence.MULTIPLICATIVE.getValue(), registry.getPrecedence(JavaTokenTypes.STAR));

        // Postfix should have highest precedence
        assertEquals("Postfix should have correct precedence",
            Precedence.POSTFIX.getValue(), registry.getPrecedence(JavaTokenTypes.DOT));
    }

    @Test
    public void testPrecedenceRelationships() {
        // Test precedence relationships are correct
        assertTrue("Assignment should be lower than disjunction",
            registry.getPrecedence(JavaTokenTypes.ASSIGN) < registry.getPrecedence(JavaTokenTypes.LOR));

        assertTrue("Disjunction should be lower than conjunction",
            registry.getPrecedence(JavaTokenTypes.LOR) < registry.getPrecedence(JavaTokenTypes.LAND));

        assertTrue("Conjunction should be lower than equality",
            registry.getPrecedence(JavaTokenTypes.LAND) < registry.getPrecedence(JavaTokenTypes.EQUAL));

        assertTrue("Equality should be lower than comparison",
            registry.getPrecedence(JavaTokenTypes.EQUAL) < registry.getPrecedence(JavaTokenTypes.LT));

        assertTrue("Comparison should be lower than additive",
            registry.getPrecedence(JavaTokenTypes.LT) < registry.getPrecedence(JavaTokenTypes.PLUS));

        assertTrue("Additive should be lower than multiplicative",
            registry.getPrecedence(JavaTokenTypes.PLUS) < registry.getPrecedence(JavaTokenTypes.STAR));

        assertTrue("Multiplicative should be lower than postfix",
            registry.getPrecedence(JavaTokenTypes.STAR) < registry.getPrecedence(JavaTokenTypes.DOT));
    }

    // ===================== DUAL-ROLE TOKEN TESTS =====================

    @Test
    public void testDualRoleTokens() {
        // Test tokens that can be both prefix and infix

        // PLUS can be both unary (+x) and binary (x + y)
        PrefixParselet prefixPlus = registry.getPrefix(JavaTokenTypes.PLUS);
        InfixParselet infixPlus = registry.getInfix(JavaTokenTypes.PLUS);

        assertNotNull("PLUS should have prefix parselet", prefixPlus);
        assertNotNull("PLUS should have infix parselet", infixPlus);
        assertTrue("Prefix PLUS should be PrefixOperatorParselet", prefixPlus instanceof PrefixOperatorParselet);
        assertTrue("Infix PLUS should be BinaryOperatorParselet", infixPlus instanceof BinaryOperatorParselet);

        // MINUS can be both unary (-x) and binary (x - y)
        PrefixParselet prefixMinus = registry.getPrefix(JavaTokenTypes.MINUS);
        InfixParselet infixMinus = registry.getInfix(JavaTokenTypes.MINUS);

        assertNotNull("MINUS should have prefix parselet", prefixMinus);
        assertNotNull("MINUS should have infix parselet", infixMinus);
        assertTrue("Prefix MINUS should be PrefixOperatorParselet", prefixMinus instanceof PrefixOperatorParselet);
        assertTrue("Infix MINUS should be BinaryOperatorParselet", infixMinus instanceof BinaryOperatorParselet);

        // INC can be both prefix (++x) and postfix (x++)
        PrefixParselet prefixInc = registry.getPrefix(JavaTokenTypes.INC);
        InfixParselet postfixInc = registry.getInfix(JavaTokenTypes.INC);

        assertNotNull("INC should have prefix parselet", prefixInc);
        assertNotNull("INC should have postfix parselet", postfixInc);
        assertTrue("Prefix INC should be PrefixOperatorParselet", prefixInc instanceof PrefixOperatorParselet);
        assertTrue("Postfix INC should be PostfixOperatorParselet", postfixInc instanceof PostfixOperatorParselet);

        // DEC can be both prefix (--x) and postfix (x--)
        PrefixParselet prefixDec = registry.getPrefix(JavaTokenTypes.DEC);
        InfixParselet postfixDec = registry.getInfix(JavaTokenTypes.DEC);

        assertNotNull("DEC should have prefix parselet", prefixDec);
        assertNotNull("DEC should have postfix parselet", postfixDec);
        assertTrue("Prefix DEC should be PrefixOperatorParselet", prefixDec instanceof PrefixOperatorParselet);
        assertTrue("Postfix DEC should be PostfixOperatorParselet", postfixDec instanceof PostfixOperatorParselet);

        // LPAREN can be both grouping ((expr)) and function calls (func())
        PrefixParselet groupParselet = registry.getPrefix(JavaTokenTypes.LPAREN);
        InfixParselet callParselet = registry.getInfix(JavaTokenTypes.LPAREN);

        assertNotNull("LPAREN should have prefix parselet", groupParselet);
        assertNotNull("LPAREN should have infix parselet", callParselet);
        assertTrue("Prefix LPAREN should be GroupParselet", groupParselet instanceof GroupParselet);
        assertTrue("Infix LPAREN should be CallParselet", callParselet instanceof CallParselet);
    }

    // ===================== REGISTRY OPERATION TESTS =====================

    @Test
    public void testRegistryHasChecks() {
        // Test hasPrefix and hasInfix methods
        assertTrue("Should have prefix parselet for IDENT", registry.hasPrefix(JavaTokenTypes.IDENT));
        assertTrue("Should have prefix parselet for NUM_INT", registry.hasPrefix(JavaTokenTypes.NUM_INT));
        assertTrue("Should have prefix parselet for LPAREN", registry.hasPrefix(JavaTokenTypes.LPAREN));

        assertTrue("Should have infix parselet for PLUS", registry.hasInfix(JavaTokenTypes.PLUS));
        assertTrue("Should have infix parselet for STAR", registry.hasInfix(JavaTokenTypes.STAR));
        assertTrue("Should have infix parselet for DOT", registry.hasInfix(JavaTokenTypes.DOT));
        assertTrue("Should have infix parselet for LPAREN", registry.hasInfix(JavaTokenTypes.LPAREN));

        // Test tokens that should not be registered
        assertFalse("Should not have prefix parselet for RCURLY", registry.hasPrefix(JavaTokenTypes.RCURLY));
        assertFalse("Should not have infix parselet for RCURLY", registry.hasInfix(JavaTokenTypes.RCURLY));
    }

    @Test
    public void testRegistryPrecedenceForUnregisteredTokens() {
        // Unregistered tokens should have precedence 0
        assertEquals("Unregistered token should have precedence 0",
            0, registry.getPrecedence(JavaTokenTypes.RCURLY));
        assertEquals("Unregistered token should have precedence 0",
            0, registry.getPrecedence(JavaTokenTypes.SEMI));
    }

    @Test
    public void testRegistryCounts() {
        // Test that we have the expected number of registered parselets
        assertTrue("Should have prefix parselets registered", registry.getPrefixCount() > 0);
        assertTrue("Should have infix parselets registered", registry.getInfixCount() > 0);

        // Expected counts based on our initialization
        // Literals: 10, Identifiers: 3, Grouping: 1, Lambda: 1, Prefix operators: 5 = 20 prefix parselets
        assertEquals("Should have 20 prefix parselets", 20, registry.getPrefixCount());

        // Binary: 13, Kotlin: 5, Assignment: 5, Postfix: 2, Access/Call: 4 = 29 infix parselets
        assertEquals("Should have 29 infix parselets", 29, registry.getInfixCount());
    }

    @Test
    public void testRegistryTokenTypesSets() {
        // Test that we can get the token types that are registered
        var prefixTokenTypes = registry.getPrefixTokenTypes();
        var infixTokenTypes = registry.getInfixTokenTypes();

        assertNotNull("Prefix token types should not be null", prefixTokenTypes);
        assertNotNull("Infix token types should not be null", infixTokenTypes);

        assertTrue("Should contain IDENT in prefix", prefixTokenTypes.contains(JavaTokenTypes.IDENT));
        assertTrue("Should contain NUM_INT in prefix", prefixTokenTypes.contains(JavaTokenTypes.NUM_INT));
        assertTrue("Should contain PLUS in both prefix and infix",
            prefixTokenTypes.contains(JavaTokenTypes.PLUS) && infixTokenTypes.contains(JavaTokenTypes.PLUS));
        assertTrue("Should contain DOT in infix", infixTokenTypes.contains(JavaTokenTypes.DOT));
    }

    // ===================== ERROR HANDLING TESTS =====================

    @Test
    public void testRegistryLookupForUnregisteredTokens() {
        // Looking up unregistered tokens should return null
        assertNull("Unregistered prefix token should return null",
            registry.getPrefix(JavaTokenTypes.RCURLY));
        assertNull("Unregistered infix token should return null",
            registry.getInfix(JavaTokenTypes.SEMI));
    }

    @Test
    public void testRegistryIntegrityAfterInitialization() {
        // After initialization, the registry should be in a consistent state
        assertNotNull("Registry should not be null", registry);

        // All literal types should be registered consistently
        for (int literalType : new int[]{
            JavaTokenTypes.NUM_INT, JavaTokenTypes.NUM_LONG, JavaTokenTypes.NUM_FLOAT,
            JavaTokenTypes.NUM_DOUBLE, JavaTokenTypes.STRING_LITERAL, JavaTokenTypes.CHAR_LITERAL,
            JavaTokenTypes.LITERAL_true, JavaTokenTypes.LITERAL_false, JavaTokenTypes.LITERAL_null
        }) {
            assertNotNull("Literal type " + literalType + " should be registered",
                registry.getPrefix(literalType));
        }

        // All binary operators should have consistent precedence relationships
        int[] precedenceSequence = {
            registry.getPrecedence(JavaTokenTypes.ASSIGN),
            registry.getPrecedence(JavaTokenTypes.LOR),
            registry.getPrecedence(JavaTokenTypes.LAND),
            registry.getPrecedence(JavaTokenTypes.EQUAL),
            registry.getPrecedence(JavaTokenTypes.LT),
            registry.getPrecedence(JavaTokenTypes.PLUS),
            registry.getPrecedence(JavaTokenTypes.STAR),
            registry.getPrecedence(JavaTokenTypes.DOT)
        };

        // Each precedence should be higher than the previous
        for (int i = 1; i < precedenceSequence.length; i++) {
            assertTrue("Precedence sequence should be increasing at position " + i,
                precedenceSequence[i] > precedenceSequence[i-1]);
        }
    }
}
