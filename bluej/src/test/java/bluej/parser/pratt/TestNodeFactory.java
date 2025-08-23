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

import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.nodes.JavaParentNode;
import bluej.parser.nodes.NodeTree;
import bluej.parser.nodes.ReparseableDocument;

import java.util.ArrayList;
import java.util.List;

/**
 * Test implementation of NodeFactory for unit testing.
 *
 * <p>This implementation creates lightweight mock nodes that don't require
 * FXPlatform thread access, allowing parselets to be tested in isolation
 * without the full BlueJ infrastructure.</p>
 *
 * <p>The mock nodes capture essential information about the parse structure
 * for validation in tests, including:</p>
 * <ul>
 *   <li>Node type identification</li>
 *   <li>Token information</li>
 *   <li>Child node relationships</li>
 *   <li>Source position tracking</li>
 * </ul>
 *
 * @author BlueJ Team
 * @since BlueJ 5.4.0
 */
public class TestNodeFactory implements NodeFactory {

    /** List of errors reported during node creation */
    private final List<String> errors = new ArrayList<>();

    /** Counter for generating unique node IDs */
    private int nodeIdCounter = 0;

    /**
     * Base class for all test nodes.
     * Extends JavaParentNode which provides a public constructor.
     */
    public static abstract class TestNode extends JavaParentNode {
        private final String testNodeType;
        private final int nodeId;
        private int beginOffset;
        private int endOffset;
        private final List<ParsedNode> children = new ArrayList<>();

        protected TestNode(String testNodeType, int nodeId) {
            super(null); // No parent initially
            this.testNodeType = testNodeType;
            this.nodeId = nodeId;
        }

        public String getTestNodeType() {
            return testNodeType;
        }

        public int getNodeId() {
            return nodeId;
        }

        public void addChild(ParsedNode child) {
            children.add(child);
        }

        public List<ParsedNode> getChildren() {
            return new ArrayList<>(children);
        }

        @Override
        public int getOffsetFromParent() {
            return beginOffset;
        }

        @Override
        protected boolean marksOwnEnd() {
            return true;
        }

        public int getTestAbsoluteStart() {
            return beginOffset;
        }

        public int getTestAbsoluteEnd() {
            return endOffset;
        }

        public void setOffsets(int begin, int end) {
            this.beginOffset = begin;
            this.endOffset = end;
        }



        @Override
        public String toString() {
            return String.format("%s[id=%d, offset=%d-%d]", testNodeType, nodeId, beginOffset, endOffset);
        }
    }

    /**
     * Test node for literal expressions.
     */
    public static class TestLiteralNode extends TestNode {
        private final LocatableToken token;

        public TestLiteralNode(int nodeId, LocatableToken token) {
            super("Literal", nodeId);
            this.token = token;
            if (token != null) {
                setOffsets(token.getPosition(), token.getEndPosition());
            }
        }

        public LocatableToken getToken() {
            return token;
        }
    }

    /**
     * Test node for binary operations.
     */
    public static class TestBinaryNode extends TestNode {
        private final ParsedNode left;
        private final LocatableToken operator;
        private final ParsedNode right;

        public TestBinaryNode(int nodeId, ParsedNode left, LocatableToken operator, ParsedNode right) {
            super("BinaryOp", nodeId);
            this.left = left;
            this.operator = operator;
            this.right = right;
            if (left != null && right != null) {
                addChild(left);
                addChild(right);
                if (left instanceof TestNode && right instanceof TestNode) {
                    setOffsets(((TestNode)left).getTestAbsoluteStart(), ((TestNode)right).getTestAbsoluteEnd());
                }
            }
        }

        public ParsedNode getLeft() {
            return left;
        }

        public LocatableToken getOperator() {
            return operator;
        }

        public ParsedNode getRight() {
            return right;
        }
    }

    /**
     * Test node for unary prefix operations.
     */
    public static class TestUnaryPrefixNode extends TestNode {
        private final LocatableToken operator;
        private final ParsedNode operand;

        public TestUnaryPrefixNode(int nodeId, LocatableToken operator, ParsedNode operand) {
            super("UnaryPrefix", nodeId);
            this.operator = operator;
            this.operand = operand;
            if (operand != null) {
                addChild(operand);
                if (operator != null) {
                    if (operand instanceof TestNode) {
                        setOffsets(operator.getPosition(), ((TestNode)operand).getTestAbsoluteEnd());
                    }
                }
            }
        }

        public LocatableToken getOperator() {
            return operator;
        }

        public ParsedNode getOperand() {
            return operand;
        }
    }

    /**
     * Test node for identifiers.
     */
    public static class TestIdentifierNode extends TestNode {
        private final LocatableToken identifier;

        public TestIdentifierNode(int nodeId, LocatableToken identifier) {
            super("Identifier", nodeId);
            this.identifier = identifier;
            if (identifier != null) {
                setOffsets(identifier.getPosition(), identifier.getEndPosition());
            }
        }

        public LocatableToken getIdentifier() {
            return identifier;
        }
    }

    @Override
    public ParsedNode createLiteralNode(LocatableToken token) {
        if (token == null) {
            reportError("Cannot create literal node with null token", null);
            return null;
        }
        return new TestLiteralNode(++nodeIdCounter, token);
    }

    @Override
    public ParsedNode createBinaryOperatorNode(ParsedNode left, LocatableToken operator, ParsedNode right) {
        if (left == null || operator == null || right == null) {
            reportError("Cannot create binary operator node with null components", operator);
            return null;
        }
        return new TestBinaryNode(++nodeIdCounter, left, operator, right);
    }

    @Override
    public ParsedNode createUnaryPrefixNode(LocatableToken operator, ParsedNode operand) {
        if (operator == null || operand == null) {
            reportError("Cannot create unary prefix node with null components", operator);
            return null;
        }
        return new TestUnaryPrefixNode(++nodeIdCounter, operator, operand);
    }

    @Override
    public ParsedNode createUnaryPostfixNode(ParsedNode operand, LocatableToken operator) {
        if (operand == null || operator == null) {
            reportError("Cannot create unary postfix node with null components", operator);
            return null;
        }
        // For simplicity, use a generic TestNode for postfix
        TestNode node = new TestNode("UnaryPostfix", ++nodeIdCounter) {};
        node.addChild(operand);
        if (operand instanceof TestNode) {
            node.setOffsets(((TestNode)operand).getTestAbsoluteStart(), operator.getEndPosition());
        }
        return node;
    }

    @Override
    public ParsedNode createGroupNode(ParsedNode innerExpression) {
        // Parentheses are purely syntactic - just return the inner expression
        return innerExpression;
    }

    @Override
    public ParsedNode createIdentifierNode(LocatableToken identifier) {
        if (identifier == null) {
            reportError("Cannot create identifier node with null token", null);
            return null;
        }
        return new TestIdentifierNode(++nodeIdCounter, identifier);
    }

    @Override
    public ParsedNode createMemberAccessNode(ParsedNode object, LocatableToken memberName, boolean isSafeCall) {
        if (object == null || memberName == null) {
            reportError("Cannot create member access node with null components", memberName);
            return null;
        }
        TestNode node = new TestNode(isSafeCall ? "SafeMemberAccess" : "MemberAccess", ++nodeIdCounter) {};
        node.addChild(object);
        if (object instanceof TestNode) {
            node.setOffsets(((TestNode)object).getTestAbsoluteStart(), memberName.getEndPosition());
        }
        return node;
    }

    @Override
    public ParsedNode createCallNode(ParsedNode function, ParsedNode[] arguments) {
        if (function == null) {
            reportError("Cannot create call node with null function", null);
            return null;
        }
        TestNode node = new TestNode("Call", ++nodeIdCounter) {};
        node.addChild(function);
        if (arguments != null) {
            for (ParsedNode arg : arguments) {
                if (arg != null) {
                    node.addChild(arg);
                }
            }
        }
        // Set offsets based on function and last argument if any
        if (function instanceof TestNode) {
            int endOffset = ((TestNode)function).getTestAbsoluteEnd();
            if (arguments != null && arguments.length > 0 && arguments[arguments.length - 1] != null) {
                ParsedNode lastArg = arguments[arguments.length - 1];
                if (lastArg instanceof TestNode) {
                    endOffset = ((TestNode)lastArg).getTestAbsoluteEnd();
                }
            }
            node.setOffsets(((TestNode)function).getTestAbsoluteStart(), endOffset);
        }
        return node;
    }

    @Override
    public ParsedNode createArrayAccessNode(ParsedNode array, ParsedNode index) {
        if (array == null || index == null) {
            reportError("Cannot create array access node with null components", null);
            return null;
        }
        TestNode node = new TestNode("ArrayAccess", ++nodeIdCounter) {};
        node.addChild(array);
        node.addChild(index);
        if (array instanceof TestNode && index instanceof TestNode) {
            node.setOffsets(((TestNode)array).getTestAbsoluteStart(), ((TestNode)index).getTestAbsoluteEnd());
        }
        return node;
    }

    @Override
    public ParsedNode createThisNode(LocatableToken thisToken) {
        if (thisToken == null) {
            reportError("Cannot create 'this' node with null token", null);
            return null;
        }
        TestNode node = new TestNode("This", ++nodeIdCounter) {};
        node.setOffsets(thisToken.getPosition(), thisToken.getEndPosition());
        return node;
    }

    @Override
    public ParsedNode createSuperNode(LocatableToken superToken) {
        if (superToken == null) {
            reportError("Cannot create 'super' node with null token", null);
            return null;
        }
        TestNode node = new TestNode("Super", ++nodeIdCounter) {};
        node.setOffsets(superToken.getPosition(), superToken.getEndPosition());
        return node;
    }

    @Override
    public void reportError(String message, LocatableToken token) {
        String errorMsg = message;
        if (token != null) {
            errorMsg += " at position " + token.getPosition();
        }
        errors.add(errorMsg);
    }

    @Override
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Gets all errors reported during node creation.
     *
     * @return List of error messages
     */
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    /**
     * Clears all recorded errors.
     */
    public void clearErrors() {
        errors.clear();
    }

    /**
     * Gets the total number of nodes created.
     *
     * @return The node count
     */
    public int getNodeCount() {
        return nodeIdCounter;
    }

    /**
     * Resets the node ID counter for a fresh test.
     */
    public void resetNodeCounter() {
        nodeIdCounter = 0;
    }
}
