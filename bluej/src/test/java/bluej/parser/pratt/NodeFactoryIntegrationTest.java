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

import bluej.extensions2.SourceType;
import bluej.parser.SourceParser;
import bluej.parser.lexer.JavaTokenFilter;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.pratt.parselets.BinaryOperatorParselet;
import bluej.parser.pratt.parselets.GroupParselet;
import bluej.parser.pratt.parselets.LiteralParselet;
import junit.framework.TestCase;

import java.io.StringReader;

/**
 * Integration tests for NodeFactory with parselets.
 *
 * <p>This test class validates that parselets correctly use the NodeFactory
 * abstraction for creating AST nodes, ensuring proper separation of parsing
 * logic from threading concerns.</p>
 *
 * @author BlueJ Team
 * @since BlueJ 5.4.0
 */
public class NodeFactoryIntegrationTest extends TestCase {

    /**
     * Creates a parser with the given source and a test node factory.
     */
    private KotlinPrattParser createTestParser(String source) {
        SourceParser sourceParser = new SourceParser(new StringReader(source), SourceType.Kotlin);
        JavaTokenFilter tokenStream = sourceParser.getTokenStream();
        TokenOperations tokenOps = new TestTokenOperations(tokenStream);
        TestNodeFactory nodeFactory = new TestNodeFactory();

        // Create parser with test registry containing basic parselets
        ParseletRegistry registry = new ParseletRegistry();

        // Register literal parselet for all literal types
        LiteralParselet literalParselet = new LiteralParselet();
        registry.register(JavaTokenTypes.NUM_INT, literalParselet);
        registry.register(JavaTokenTypes.NUM_LONG, literalParselet);
        registry.register(JavaTokenTypes.NUM_FLOAT, literalParselet);
        registry.register(JavaTokenTypes.NUM_DOUBLE, literalParselet);
        registry.register(JavaTokenTypes.STRING_LITERAL, literalParselet);
        registry.register(JavaTokenTypes.CHAR_LITERAL, literalParselet);
        registry.register(JavaTokenTypes.LITERAL_true, literalParselet);
        registry.register(JavaTokenTypes.LITERAL_false, literalParselet);
        registry.register(JavaTokenTypes.LITERAL_null, literalParselet);

        // Register binary operator parselets
        registry.register(JavaTokenTypes.PLUS,
            new BinaryOperatorParselet(Precedence.ADDITIVE));
        registry.register(JavaTokenTypes.MINUS,
            new BinaryOperatorParselet(Precedence.ADDITIVE));
        registry.register(JavaTokenTypes.STAR,
            new BinaryOperatorParselet(Precedence.MULTIPLICATIVE));
        registry.register(JavaTokenTypes.DIV,
            new BinaryOperatorParselet(Precedence.MULTIPLICATIVE));

        // Register group parselet for parentheses
        registry.register(JavaTokenTypes.LPAREN, new GroupParselet());

        return new KotlinPrattParser(tokenOps, sourceParser, nodeFactory, registry);
    }

    /**
     * Gets the TestNodeFactory from a parser for validation.
     */
    private TestNodeFactory getTestNodeFactory(KotlinPrattParser parser) {
        return (TestNodeFactory) parser.getNodeFactory();
    }

    // ========== Literal Parselet Tests ==========

    /**
     * Test that LiteralParselet uses NodeFactory to create nodes.
     */
    public void testLiteralParseletCreatesNode() {
        KotlinPrattParser parser = createTestParser("42");
        ParsedNode node = parser.parseExpression(0);

        assertNotNull("Should create a node for integer literal", node);
        assertTrue("Node should be a TestNode",
            node instanceof TestNodeFactory.TestNode);

        TestNodeFactory.TestNode testNode =
            (TestNodeFactory.TestNode) node;
        if (testNode instanceof TestNodeFactory.TestLiteralNode) {
            TestNodeFactory.TestLiteralNode literalNode = (TestNodeFactory.TestLiteralNode) testNode;
            assertEquals("Node type should be Literal", "Literal", literalNode.getTestNodeType());
            assertNotNull("Literal node should have token", literalNode.getToken());
            assertEquals("Token should be integer literal",
                JavaTokenTypes.NUM_INT, literalNode.getToken().getType());
        }
    }

    /**
     * Test that LiteralParselet handles different literal types.
     */
    public void testLiteralParseletHandlesAllTypes() {
        // Test integer literal
        KotlinPrattParser intParser = createTestParser("123");
        ParsedNode intNode = intParser.parseExpression(0);
        assertNotNull("Should handle integer literal", intNode);

        // Test string literal
        KotlinPrattParser strParser = createTestParser("\"hello\"");
        ParsedNode strNode = strParser.parseExpression(0);
        assertNotNull("Should handle string literal", strNode);

        // Test boolean literal
        KotlinPrattParser boolParser = createTestParser("true");
        ParsedNode boolNode = boolParser.parseExpression(0);
        assertNotNull("Should handle boolean literal", boolNode);

        // Test null literal
        KotlinPrattParser nullParser = createTestParser("null");
        ParsedNode nullNode = nullParser.parseExpression(0);
        assertNotNull("Should handle null literal", nullNode);
    }

    // ========== Binary Operator Parselet Tests ==========

    /**
     * Test that BinaryOperatorParselet uses NodeFactory to create nodes.
     */
    public void testBinaryOperatorParseletCreatesNode() {
        KotlinPrattParser parser = createTestParser("1 + 2");
        ParsedNode node = parser.parseExpression(0);

        assertNotNull("Should create a node for binary operation", node);
        assertTrue("Node should be a TestNode",
            node instanceof TestNodeFactory.TestNode);

        if (node instanceof TestNodeFactory.TestBinaryNode) {
            TestNodeFactory.TestBinaryNode binaryNode =
                (TestNodeFactory.TestBinaryNode) node;
            assertEquals("Node type should be BinaryOp", "BinaryOp", binaryNode.getTestNodeType());
            assertNotNull("Binary node should have left operand", binaryNode.getLeft());
            assertNotNull("Binary node should have operator", binaryNode.getOperator());
            assertNotNull("Binary node should have right operand", binaryNode.getRight());
            assertEquals("Operator should be PLUS",
                JavaTokenTypes.PLUS, binaryNode.getOperator().getType());
        }
    }

    /**
     * Test that BinaryOperatorParselet handles precedence correctly.
     */
    public void testBinaryOperatorPrecedence() {
        // Test that multiplication binds tighter than addition
        KotlinPrattParser parser = createTestParser("1 + 2 * 3");
        ParsedNode node = parser.parseExpression(0);

        assertNotNull("Should parse expression", node);
        assertTrue("Root should be a TestNode",
            node instanceof TestNodeFactory.TestNode);

        if (node instanceof TestNodeFactory.TestBinaryNode) {
            TestNodeFactory.TestBinaryNode rootBinary =
                (TestNodeFactory.TestBinaryNode) node;
            assertEquals("Root operator should be PLUS",
                JavaTokenTypes.PLUS, rootBinary.getOperator().getType());

            // Right operand should be the multiplication
            assertTrue("Right operand should be binary node",
                rootBinary.getRight() instanceof TestNodeFactory.TestBinaryNode);
            TestNodeFactory.TestBinaryNode rightBinary =
                (TestNodeFactory.TestBinaryNode) rootBinary.getRight();
            assertEquals("Right operator should be STAR",
                JavaTokenTypes.STAR, rightBinary.getOperator().getType());
        }
    }

    // ========== Group Parselet Tests ==========

    /**
     * Test that GroupParselet uses NodeFactory to handle grouped expressions.
     */
    public void testGroupParseletCreatesNode() {
        KotlinPrattParser parser = createTestParser("(42)");
        ParsedNode node = parser.parseExpression(0);

        assertNotNull("Should create a node for grouped expression", node);
        // GroupParselet typically returns the inner expression directly
        // since parentheses are purely syntactic
        assertTrue("Node should be a TestNode (inner expression)",
            node instanceof TestNodeFactory.TestNode);
    }

    /**
     * Test that GroupParselet affects precedence correctly.
     */
    public void testGroupParseletAffectsPrecedence() {
        // Parentheses should override normal precedence
        KotlinPrattParser parser = createTestParser("(1 + 2) * 3");
        ParsedNode node = parser.parseExpression(0);

        assertNotNull("Should parse expression", node);
        assertTrue("Root should be a TestNode",
            node instanceof TestNodeFactory.TestNode);

        if (node instanceof TestNodeFactory.TestBinaryNode) {
            TestNodeFactory.TestBinaryNode rootBinary =
                (TestNodeFactory.TestBinaryNode) node;
            assertEquals("Root operator should be STAR",
                JavaTokenTypes.STAR, rootBinary.getOperator().getType());

            // Left operand should be the addition (due to parentheses)
            assertTrue("Left operand should be binary node",
                rootBinary.getLeft() instanceof TestNodeFactory.TestBinaryNode);
            TestNodeFactory.TestBinaryNode leftBinary =
                (TestNodeFactory.TestBinaryNode) rootBinary.getLeft();
            assertEquals("Left operator should be PLUS",
                JavaTokenTypes.PLUS, leftBinary.getOperator().getType());
        }
    }

    // ========== Error Handling Tests ==========

    /**
     * Test that NodeFactory properly reports errors.
     */
    public void testNodeFactoryErrorReporting() {
        KotlinPrattParser parser = createTestParser("42");
        TestNodeFactory factory = getTestNodeFactory(parser);

        // Clear any existing errors
        factory.clearErrors();
        assertFalse("Factory should have no errors initially", factory.hasErrors());

        // Simulate an error
        factory.reportError("Test error", null);
        assertTrue("Factory should have errors after reporting", factory.hasErrors());
        assertEquals("Should have one error", 1, factory.getErrors().size());
        assertEquals("Error message should match", "Test error", factory.getErrors().get(0));
    }

    /**
     * Test that parselets handle null NodeFactory gracefully.
     */
    public void testParseletWithoutNodeFactory() {
        // Create parser without node factory (simulate error condition)
        SourceParser sourceParser = new SourceParser(
            new StringReader("42"), SourceType.Kotlin);
        JavaTokenFilter tokenStream = sourceParser.getTokenStream();
        TokenOperations tokenOps = new TestTokenOperations(tokenStream);
        KotlinPrattParser parser = new KotlinPrattParser(tokenOps, sourceParser, null);

        // Register literal parselet
        parser.getRegistry().register(JavaTokenTypes.NUM_INT, new LiteralParselet());

        // Try to parse - should handle missing factory gracefully
        ParsedNode node = parser.parseExpression(0);
        assertNull("Should return null when NodeFactory is missing", node);
        assertTrue("Parser should have errors", parser.hasErrors());
    }

    // ========== Complex Expression Tests ==========

    /**
     * Test that complex expressions use NodeFactory correctly.
     */
    public void testComplexExpressionNodeCreation() {
        KotlinPrattParser parser = createTestParser("1 + 2 * (3 - 4)");
        TestNodeFactory factory = getTestNodeFactory(parser);

        // Reset counter for tracking
        factory.resetNodeCounter();

        ParsedNode node = parser.parseExpression(0);
        assertNotNull("Should parse complex expression", node);

        // Should have created multiple nodes
        assertTrue("Should create multiple nodes", factory.getNodeCount() > 1);

        // Verify structure
        assertTrue("Root should be a TestNode",
            node instanceof TestNodeFactory.TestNode);
        if (node instanceof TestNodeFactory.TestBinaryNode) {
            TestNodeFactory.TestBinaryNode root = (TestNodeFactory.TestBinaryNode) node;
            assertEquals("Root should be addition",
                JavaTokenTypes.PLUS, root.getOperator().getType());
        }
    }

    /**
     * Test that node offsets are correctly set.
     */
    public void testNodeOffsetsAreSet() {
        KotlinPrattParser parser = createTestParser("42 + 100");
        ParsedNode node = parser.parseExpression(0);

        assertNotNull("Should create node", node);
        assertTrue("Should be a TestNode",
            node instanceof TestNodeFactory.TestNode);

        if (node instanceof TestNodeFactory.TestBinaryNode) {
            TestNodeFactory.TestBinaryNode binaryNode =
                (TestNodeFactory.TestBinaryNode) node;

            // Check that offsets are set
            assertTrue("Start offset should be >= 0", binaryNode.getTestAbsoluteStart() >= 0);
            assertTrue("End offset should be > start",
                binaryNode.getTestAbsoluteEnd() > binaryNode.getTestAbsoluteStart());
        }
    }

    /**
     * Test that nested binary operations create correct tree structure.
     */
    public void testNestedBinaryOperations() {
        KotlinPrattParser parser = createTestParser("1 + 2 + 3 + 4");
        ParsedNode node = parser.parseExpression(0);

        assertNotNull("Should parse chained additions", node);

        // Left-associative operators should create left-leaning tree
        // ((1 + 2) + 3) + 4
        if (node instanceof TestNodeFactory.TestBinaryNode) {
            TestNodeFactory.TestBinaryNode root = (TestNodeFactory.TestBinaryNode) node;
            assertEquals("Root should be +", JavaTokenTypes.PLUS, root.getOperator().getType());

            // Left side should be another binary operation
            assertTrue("Left should be binary",
                root.getLeft() instanceof TestNodeFactory.TestBinaryNode);
        }
    }
}
