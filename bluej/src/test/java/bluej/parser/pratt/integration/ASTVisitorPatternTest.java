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
package bluej.parser.pratt.integration;

import bluej.parser.CallbackDelegate;
import bluej.parser.pratt.integration.CallbackTestingUtility.CallbackTester;
import bluej.parser.SourceParser;
import bluej.parser.JavaParserCallbacks;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LineColPos;
import bluej.parser.pratt.*;
import bluej.parser.pratt.InfixParselet;
import bluej.parser.pratt.PrefixParselet;
import bluej.parser.nodes.ParsedNode;
import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Test implementation for Strategy 1: Post-Parsing AST Visitor Pattern.
 * 
 * This strategy follows a traditional two-phase approach:
 * 1. The Pratt parser builds a complete AST structure
 * 2. A visitor walks the AST and generates callbacks
 * 
 * Key features tested:
 * - Complete AST construction before callback generation
 * - Visitor pattern for traversing different node types
 * - Guaranteed callback pairing through structured traversal
 * - Ability to analyze/optimize the AST before generating callbacks
 * - Support for multiple visitors (e.g., validation, callback generation)
 */
public class ASTVisitorPatternTest {

    private CallbackTester callbackTester;
    private CallbackEmittingVisitor callbackVisitor;
    private ASTBuilder astBuilder;
    private SourceParser parser;
    private CallbackDelegate callbackDelegate;
    
    @Before
    public void setUp() {
        // Create SourceParser with CallbackDelegate
        parser = new SourceParser(new StringReader(""));
        callbackDelegate = parser.getCallbackDelegate();
        
        // Set up callbackTester and visitor with CallbackDelegate
        callbackTester = CallbackTestingUtility.forDelegate(callbackDelegate);
        callbackVisitor = new CallbackEmittingVisitor(callbackTester.getDelegate());
        astBuilder = new ASTBuilder();
    }

    // ==================== AST Node Types ====================
    
    /**
     * Base interface for all AST nodes that can be visited.
     */
    public interface ASTNode {
        void accept(ASTVisitor visitor);
        LocatableToken getFirstToken();
        LocatableToken getLastToken();
    }
    
    /**
     * Base class providing common functionality for AST nodes.
     */
    public abstract static class BaseASTNode implements ASTNode {
        protected final LocatableToken firstToken;
        protected LocatableToken lastToken;
        
        public BaseASTNode(LocatableToken firstToken) {
            this.firstToken = firstToken;
            this.lastToken = firstToken;
        }
        
        @Override
        public LocatableToken getFirstToken() {
            return firstToken;
        }
        
        @Override
        public LocatableToken getLastToken() {
            return lastToken;
        }
        
        protected void updateLastToken(LocatableToken token) {
            if (token != null) {
                this.lastToken = token;
            }
        }
    }
    
    /**
     * Binary operator AST node.
     */
    public static class BinaryOperatorNode extends BaseASTNode {
        private final ASTNode left;
        private final LocatableToken operator;
        private final ASTNode right;
        
        public BinaryOperatorNode(ASTNode left, LocatableToken operator, ASTNode right) {
            super(left.getFirstToken());
            this.left = left;
            this.operator = operator;
            this.right = right;
            updateLastToken(right.getLastToken());
        }
        
        @Override
        public void accept(ASTVisitor visitor) {
            visitor.visitBinaryOperator(this);
        }
        
        public ASTNode getLeft() { return left; }
        public LocatableToken getOperator() { return operator; }
        public ASTNode getRight() { return right; }
    }
    
    /**
     * Literal AST node.
     */
    public static class LiteralNode extends BaseASTNode {
        private final Object value;
        
        public LiteralNode(LocatableToken token, Object value) {
            super(token);
            this.value = value;
        }
        
        @Override
        public void accept(ASTVisitor visitor) {
            visitor.visitLiteral(this);
        }
        
        public Object getValue() { return value; }
    }
    
    /**
     * Identifier AST node.
     */
    public static class IdentifierNode extends BaseASTNode {
        private final String name;
        
        public IdentifierNode(LocatableToken token) {
            super(token);
            this.name = token.getText();
        }
        
        @Override
        public void accept(ASTVisitor visitor) {
            visitor.visitIdentifier(this);
        }
        
        public String getName() { return name; }
    }
    
    /**
     * Method call AST node.
     */
    public static class MethodCallNode extends BaseASTNode {
        private final ASTNode target; // null for standalone calls
        private final String methodName;
        private final List<ASTNode> arguments;
        
        public MethodCallNode(ASTNode target, LocatableToken methodToken, 
                             List<ASTNode> arguments, LocatableToken closeParen) {
            super(target != null ? target.getFirstToken() : methodToken);
            this.target = target;
            this.methodName = methodToken.getText();
            this.arguments = new ArrayList<>(arguments);
            updateLastToken(closeParen);
        }
        
        @Override
        public void accept(ASTVisitor visitor) {
            visitor.visitMethodCall(this);
        }
        
        public ASTNode getTarget() { return target; }
        public String getMethodName() { return methodName; }
        public List<ASTNode> getArguments() { return arguments; }
    }
    
    /**
     * If statement AST node.
     */
    public static class IfStatementNode extends BaseASTNode {
        private final ASTNode condition;
        private final BlockNode thenBlock;
        private final BlockNode elseBlock; // may be null
        
        public IfStatementNode(LocatableToken ifToken, ASTNode condition,
                              BlockNode thenBlock, BlockNode elseBlock) {
            super(ifToken);
            this.condition = condition;
            this.thenBlock = thenBlock;
            this.elseBlock = elseBlock;
            updateLastToken(elseBlock != null ? elseBlock.getLastToken() : thenBlock.getLastToken());
        }
        
        @Override
        public void accept(ASTVisitor visitor) {
            visitor.visitIfStatement(this);
        }
        
        public ASTNode getCondition() { return condition; }
        public BlockNode getThenBlock() { return thenBlock; }
        public BlockNode getElseBlock() { return elseBlock; }
        public boolean hasElseBlock() { return elseBlock != null; }
    }
    
    /**
     * Block AST node containing statements.
     */
    public static class BlockNode extends BaseASTNode {
        private final List<ASTNode> statements;
        
        public BlockNode(LocatableToken openBrace, List<ASTNode> statements, 
                        LocatableToken closeBrace) {
            super(openBrace);
            this.statements = new ArrayList<>(statements);
            updateLastToken(closeBrace);
        }
        
        @Override
        public void accept(ASTVisitor visitor) {
            visitor.visitBlock(this);
        }
        
        public List<ASTNode> getStatements() { return statements; }
    }
    
    /**
     * For loop AST node.
     */
    public static class ForLoopNode extends BaseASTNode {
        private final ASTNode init;
        private final ASTNode condition;
        private final ASTNode increment;
        private final BlockNode body;
        
        public ForLoopNode(LocatableToken forToken, ASTNode init, ASTNode condition,
                          ASTNode increment, BlockNode body) {
            super(forToken);
            this.init = init;
            this.condition = condition;
            this.increment = increment;
            this.body = body;
            updateLastToken(body.getLastToken());
        }
        
        @Override
        public void accept(ASTVisitor visitor) {
            visitor.visitForLoop(this);
        }
        
        public ASTNode getInit() { return init; }
        public ASTNode getCondition() { return condition; }
        public ASTNode getIncrement() { return increment; }
        public BlockNode getBody() { return body; }
    }
    
    /**
     * Import statement AST node.
     */
    public static class ImportNode extends BaseASTNode {
        private final List<String> packagePath;
        private final boolean isStatic;
        private final boolean isWildcard;
        
        public ImportNode(LocatableToken importToken, List<String> packagePath,
                         boolean isStatic, boolean isWildcard, LocatableToken semiToken) {
            super(importToken);
            this.packagePath = new ArrayList<>(packagePath);
            this.isStatic = isStatic;
            this.isWildcard = isWildcard;
            updateLastToken(semiToken);
        }
        
        @Override
        public void accept(ASTVisitor visitor) {
            visitor.visitImport(this);
        }
        
        public List<String> getPackagePath() { return packagePath; }
        public boolean isStatic() { return isStatic; }
        public boolean isWildcard() { return isWildcard; }
    }
    
    /**
     * Expression statement wrapper.
     */
    public static class ExpressionStatementNode extends BaseASTNode {
        private final ASTNode expression;
        
        public ExpressionStatementNode(ASTNode expression) {
            super(expression.getFirstToken());
            this.expression = expression;
            updateLastToken(expression.getLastToken());
        }
        
        @Override
        public void accept(ASTVisitor visitor) {
            visitor.visitExpressionStatement(this);
        }
        
        public ASTNode getExpression() { return expression; }
    }
    
    // ==================== Visitor Interface ====================
    
    /**
     * Visitor interface for traversing AST nodes.
     */
    public interface ASTVisitor {
        void visitBinaryOperator(BinaryOperatorNode node);
        void visitLiteral(LiteralNode node);
        void visitIdentifier(IdentifierNode node);
        void visitMethodCall(MethodCallNode node);
        void visitIfStatement(IfStatementNode node);
        void visitBlock(BlockNode node);
        void visitForLoop(ForLoopNode node);
        void visitImport(ImportNode node);
        void visitExpressionStatement(ExpressionStatementNode node);
    }
    
    /**
     * Base visitor providing default traversal behavior.
     */
    public abstract static class BaseASTVisitor implements ASTVisitor {
        @Override
        public void visitBinaryOperator(BinaryOperatorNode node) {
            node.getLeft().accept(this);
            node.getRight().accept(this);
        }
        
        @Override
        public void visitLiteral(LiteralNode node) {
            // Leaf node, no children to visit
        }
        
        @Override
        public void visitIdentifier(IdentifierNode node) {
            // Leaf node, no children to visit
        }
        
        @Override
        public void visitMethodCall(MethodCallNode node) {
            if (node.getTarget() != null) {
                node.getTarget().accept(this);
            }
            for (ASTNode arg : node.getArguments()) {
                arg.accept(this);
            }
        }
        
        @Override
        public void visitIfStatement(IfStatementNode node) {
            node.getCondition().accept(this);
            node.getThenBlock().accept(this);
            if (node.hasElseBlock()) {
                node.getElseBlock().accept(this);
            }
        }
        
        @Override
        public void visitBlock(BlockNode node) {
            for (ASTNode stmt : node.getStatements()) {
                stmt.accept(this);
            }
        }
        
        @Override
        public void visitForLoop(ForLoopNode node) {
            if (node.getInit() != null) {
                node.getInit().accept(this);
            }
            if (node.getCondition() != null) {
                node.getCondition().accept(this);
            }
            if (node.getIncrement() != null) {
                node.getIncrement().accept(this);
            }
            node.getBody().accept(this);
        }
        
        @Override
        public void visitImport(ImportNode node) {
            // No children to visit
        }
        
        @Override
        public void visitExpressionStatement(ExpressionStatementNode node) {
            node.getExpression().accept(this);
        }
    }
    
    // ==================== Callback Emitting Visitor ====================
    
    /**
     * Visitor that generates callbacks from AST nodes.
     * This demonstrates the post-parsing callback generation approach.
     */
    public static class CallbackEmittingVisitor extends BaseASTVisitor {
        private final CallbackDelegate callbacks;
        
        public CallbackEmittingVisitor(CallbackDelegate callbacks) {
            this.callbacks = callbacks;
        }
        
        @Override
        public void visitBinaryOperator(BinaryOperatorNode node) {
            callbacks.beginExpression(node.getFirstToken(), false);
            
            // Visit left operand
            node.getLeft().accept(this);
            
            // Emit operator callback
            callbacks.gotBinaryOperator(node.getOperator());
            
            // Visit right operand
            node.getRight().accept(this);
            
            callbacks.endExpression(node.getLastToken(), false);
        }
        
        @Override
        public void visitLiteral(LiteralNode node) {
            callbacks.gotLiteral(node.getFirstToken());
        }
        
        @Override
        public void visitIdentifier(IdentifierNode node) {
            callbacks.gotIdentifier(node.getFirstToken());
        }
        
        @Override
        public void visitMethodCall(MethodCallNode node) {
            if (node.getTarget() != null) {
                // Member call
                node.getTarget().accept(this);
                callbacks.gotMemberCall(node.getFirstToken(), null);
            } else {
                // Standalone call
                callbacks.gotMethodCall(node.getFirstToken());
            }
            
            // Arguments
            callbacks.beginArgumentList(node.getFirstToken());
            for (ASTNode arg : node.getArguments()) {
                arg.accept(this);
                callbacks.endArgument();
            }
            callbacks.endArgumentList(node.getLastToken());
        }
        
        @Override
        public void visitIfStatement(IfStatementNode node) {
            callbacks.beginIfStmt(node.getFirstToken());
            
            // Condition
            callbacks.beginExpression(node.getCondition().getFirstToken(), false);
            node.getCondition().accept(this);
            callbacks.endExpression(node.getCondition().getLastToken(), false);
            
            // Then block
            callbacks.beginIfCondBlock(node.getThenBlock().getFirstToken());
            visitBlock(node.getThenBlock());
            callbacks.endIfCondBlock(node.getThenBlock().getLastToken(), true);
            
            // Else block
            if (node.hasElseBlock()) {
                callbacks.gotElseIf(node.getElseBlock().getFirstToken());
                callbacks.beginIfCondBlock(node.getElseBlock().getFirstToken());
                visitBlock(node.getElseBlock());
                callbacks.endIfCondBlock(node.getElseBlock().getLastToken(), true);
            }
            
            callbacks.endIfStmt(node.getLastToken(), true);
        }
        
        @Override
        public void visitForLoop(ForLoopNode node) {
            callbacks.beginForLoop(node.getFirstToken());
            
            // Init
            if (node.getInit() != null) {
                callbacks.beginElement(node.getInit().getFirstToken());
                node.getInit().accept(this);
                callbacks.endElement(node.getInit().getLastToken(), true);
            }
            
            // Condition
            callbacks.gotForTest(node.getCondition() != null);
            if (node.getCondition() != null) {
                node.getCondition().accept(this);
            }
            
            // Increment
            callbacks.gotForIncrement(node.getIncrement() != null);
            if (node.getIncrement() != null) {
                node.getIncrement().accept(this);
            }
            
            // Body
            callbacks.beginForLoopBody(node.getBody().getFirstToken());
            visitBlock(node.getBody());
            callbacks.endForLoopBody(node.getBody().getLastToken(), true);
            
            callbacks.endForLoop(node.getLastToken(), true);
        }
        
        @Override
        public void visitImport(ImportNode node) {
            callbacks.beginElement(node.getFirstToken());
            
            // Convert package path to tokens (simplified)
            List<LocatableToken> tokens = new ArrayList<>();
            for (String part : node.getPackagePath()) {
                tokens.add(mockToken(part));
            }
            
            if (node.isWildcard()) {
                callbacks.gotWildcardImport(tokens, node.isStatic(), 
                    node.getFirstToken(), node.getLastToken());
            } else {
                callbacks.gotImport(tokens, node.isStatic(), 
                    node.getFirstToken(), node.getLastToken());
            }
            
            callbacks.gotImportStmtSemi(node.getLastToken());
        }
        
        @Override
        public void visitExpressionStatement(ExpressionStatementNode node) {
            callbacks.beginExpression(node.getFirstToken(), false);
            node.getExpression().accept(this);
            callbacks.endExpression(node.getLastToken(), false);
            callbacks.gotStatementExpression();
        }
    }
    
    // ==================== AST Building Parselets ====================
    
    /**
     * Builder for creating AST nodes.
     */
    public static class ASTBuilder {
        
        public BinaryOperatorNode createBinaryOperator(ASTNode left, 
                                                       LocatableToken operator, 
                                                       ASTNode right) {
            return new BinaryOperatorNode(left, operator, right);
        }
        
        public LiteralNode createLiteral(LocatableToken token, Object value) {
            return new LiteralNode(token, value);
        }
        
        public IdentifierNode createIdentifier(LocatableToken token) {
            return new IdentifierNode(token);
        }
        
        public MethodCallNode createMethodCall(ASTNode target, LocatableToken methodToken,
                                              List<ASTNode> arguments, LocatableToken closeParen) {
            return new MethodCallNode(target, methodToken, arguments, closeParen);
        }
        
        public IfStatementNode createIfStatement(LocatableToken ifToken, ASTNode condition,
                                                BlockNode thenBlock, BlockNode elseBlock) {
            return new IfStatementNode(ifToken, condition, thenBlock, elseBlock);
        }
        
        public BlockNode createBlock(LocatableToken openBrace, List<ASTNode> statements,
                                    LocatableToken closeBrace) {
            return new BlockNode(openBrace, statements, closeBrace);
        }
        
        public ForLoopNode createForLoop(LocatableToken forToken, ASTNode init,
                                        ASTNode condition, ASTNode increment, BlockNode body) {
            return new ForLoopNode(forToken, init, condition, increment, body);
        }
        
        public ImportNode createImport(LocatableToken importToken, List<String> packagePath,
                                      boolean isStatic, boolean isWildcard, LocatableToken semiToken) {
            return new ImportNode(importToken, packagePath, isStatic, isWildcard, semiToken);
        }
        
        public ExpressionStatementNode createExpressionStatement(ASTNode expression) {
            return new ExpressionStatementNode(expression);
        }
    }
    
    // ==================== Test Cases ====================
    
    @Test
    public void testSimpleBinaryExpression() {
        // Build AST: "a + b"
        IdentifierNode left = astBuilder.createIdentifier(mockToken("a"));
        IdentifierNode right = astBuilder.createIdentifier(mockToken("b"));
        BinaryOperatorNode expr = astBuilder.createBinaryOperator(left, mockToken("+"), right);
        
        // Visit AST to generate callbacks
        expr.accept(callbackVisitor);
        
        // Verify callback sequence
        List<String> expected = Arrays.asList(
            "beginExpression",
            "gotIdentifier",      // a
            "gotBinaryOperator",  // +
            "gotIdentifier",      // b
            "endExpression"
        );
        
        assertTrue("Should generate correct callback sequence",
            callbackTester.hasCallbackSequence(expected));
        assertTrue("Callbacks should be balanced", callbackTester.isBalanced());
    }
    
    @Test
    public void testNestedBinaryExpression() {
        // Build AST: "a + b * c"
        IdentifierNode a = astBuilder.createIdentifier(mockToken("a"));
        IdentifierNode b = astBuilder.createIdentifier(mockToken("b"));
        IdentifierNode c = astBuilder.createIdentifier(mockToken("c"));
        
        BinaryOperatorNode multiply = astBuilder.createBinaryOperator(b, mockToken("*"), c);
        BinaryOperatorNode add = astBuilder.createBinaryOperator(a, mockToken("+"), multiply);
        
        // Visit AST
        add.accept(callbackVisitor);
        
        // Verify nested expression callbacks
        assertTrue("Should have nested expressions",
            callbackTester.getCallbackCount() > 5);
        assertTrue("Callbacks should be balanced", callbackTester.isBalanced());
    }
    
    @Test
    public void testMethodCallExpression() {
        // Build AST: "obj.method(arg1, arg2)"
        IdentifierNode target = astBuilder.createIdentifier(mockToken("obj"));
        LiteralNode arg1 = astBuilder.createLiteral(mockToken("1"), 1);
        LiteralNode arg2 = astBuilder.createLiteral(mockToken("2"), 2);
        
        MethodCallNode call = astBuilder.createMethodCall(
            target, mockToken("method"), 
            Arrays.asList(arg1, arg2), mockToken(")"));
        
        // Visit AST
        call.accept(callbackVisitor);
        
        // Verify callback sequence includes method call
        assertTrue("Should have identifier for target",
            callbackTester.hasCallback("gotIdentifier"));
        assertTrue("Should have member call",
            callbackTester.hasCallback("gotMemberCall"));
        assertTrue("Should have argument list",
            callbackTester.hasCallback("beginArgumentList") &&
            callbackTester.hasCallback("endArgumentList"));
    }
    
    @Test
    public void testIfStatementAST() {
        // Build AST: if (x > 0) { y = 1; } else { y = 2; }
        IdentifierNode x = astBuilder.createIdentifier(mockToken("x"));
        LiteralNode zero = astBuilder.createLiteral(mockToken("0"), 0);
        BinaryOperatorNode condition = astBuilder.createBinaryOperator(x, mockToken(">"), zero);
        
        // Then block: y = 1
        IdentifierNode y1 = astBuilder.createIdentifier(mockToken("y"));
        LiteralNode one = astBuilder.createLiteral(mockToken("1"), 1);
        BinaryOperatorNode assign1 = astBuilder.createBinaryOperator(y1, mockToken("="), one);
        BlockNode thenBlock = astBuilder.createBlock(
            mockToken("{"), Arrays.asList(assign1), mockToken("}"));
        
        // Else block: y = 2
        IdentifierNode y2 = astBuilder.createIdentifier(mockToken("y"));
        LiteralNode two = astBuilder.createLiteral(mockToken("2"), 2);
        BinaryOperatorNode assign2 = astBuilder.createBinaryOperator(y2, mockToken("="), two);
        BlockNode elseBlock = astBuilder.createBlock(
            mockToken("{"), Arrays.asList(assign2), mockToken("}"));
        
        IfStatementNode ifStmt = astBuilder.createIfStatement(
            mockToken("if"), condition, thenBlock, elseBlock);
        
        // Visit AST
        ifStmt.accept(callbackVisitor);
        
        // Verify complete if statement callback structure
        List<String> expected = Arrays.asList(
            "beginIfStmt",
            "beginExpression",    // condition
            "endExpression",
            "beginIfCondBlock",   // then
            "endIfCondBlock",
            "gotElseIf",         // else
            "beginIfCondBlock",
            "endIfCondBlock",
            "endIfStmt"
        );
        
        assertTrue("Should have complete if statement structure",
            callbackTester.hasCallbackSequence(expected));
        assertTrue("Callbacks should be balanced", callbackTester.isBalanced());
    }
    
    @Test
    public void testForLoopAST() {
        // Build AST: for (int i = 0; i < 10; i++) { ... }
        IdentifierNode i = astBuilder.createIdentifier(mockToken("i"));
        LiteralNode zero = astBuilder.createLiteral(mockToken("0"), 0);
        BinaryOperatorNode init = astBuilder.createBinaryOperator(i, mockToken("="), zero);
        
        IdentifierNode i2 = astBuilder.createIdentifier(mockToken("i"));
        LiteralNode ten = astBuilder.createLiteral(mockToken("10"), 10);
        BinaryOperatorNode condition = astBuilder.createBinaryOperator(i2, mockToken("<"), ten);
        
        IdentifierNode i3 = astBuilder.createIdentifier(mockToken("i"));
        // Simplified increment (would be unary in real impl)
        BinaryOperatorNode increment = astBuilder.createBinaryOperator(
            i3, mockToken("++"), astBuilder.createLiteral(mockToken("1"), 1));
        
        BlockNode body = astBuilder.createBlock(
            mockToken("{"), new ArrayList<>(), mockToken("}"));
        
        ForLoopNode forLoop = astBuilder.createForLoop(
            mockToken("for"), init, condition, increment, body);
        
        // Visit AST
        forLoop.accept(callbackVisitor);
        
        // Verify for loop callbacks
        assertTrue("Should have for loop begin/end",
            callbackTester.hasCallback("beginForLoop") &&
            callbackTester.hasCallback("endForLoop"));
        assertTrue("Should have for loop body",
            callbackTester.hasCallback("beginForLoopBody") &&
            callbackTester.hasCallback("endForLoopBody"));
        assertTrue("Callbacks should be balanced", callbackTester.isBalanced());
    }
    
    @Test
    public void testImportStatement() {
        // Build AST: import java.util.List;
        ImportNode importNode = astBuilder.createImport(
            mockToken("import"),
            Arrays.asList("java", "util", "List"),
            false, false,
            mockToken(";"));
        
        // Visit AST
        importNode.accept(callbackVisitor);
        
        // Verify import callbacks
        assertTrue("Should have import callback",
            callbackTester.hasCallback("gotImport"));
        assertTrue("Should have import semicolon",
            callbackTester.hasCallback("gotImportStmtSemi"));
    }
    
    @Test
    public void testMultipleVisitors() {
        // Build a simple AST
        LiteralNode literal = astBuilder.createLiteral(mockToken("42"), 42);
        
        // First visitor: validation
        ValidationVisitor validationVisitor = new ValidationVisitor();
        literal.accept(validationVisitor);
        assertTrue("Validation should pass", validationVisitor.isValid());
        
        // Second visitor: callback generation
        literal.accept(callbackVisitor);
        assertTrue("Should generate callbacks", callbackTester.hasCallback("gotLiteral"));
        
        // Third visitor: statistics gathering
        StatisticsVisitor statsVisitor = new StatisticsVisitor();
        literal.accept(statsVisitor);
        assertEquals("Should count one node", 1, statsVisitor.getNodeCount());
    }
    
    @Test
    public void testASTModificationBeforeCallbacks() {
        // Build initial AST
        IdentifierNode a = astBuilder.createIdentifier(mockToken("a"));
        IdentifierNode b = astBuilder.createIdentifier(mockToken("b"));
        BinaryOperatorNode expr = astBuilder.createBinaryOperator(a, mockToken("+"), b);
        
        // Transform AST (e.g., constant folding, optimization)
        ASTTransformer transformer = new ASTTransformer();
        ASTNode transformed = transformer.transform(expr);
        
        // Generate callbacks from transformed AST
        transformed.accept(callbackVisitor);
        
        assertTrue("Should generate callbacks from transformed AST",
            callbackTester.getCallbackCount() > 0);
        assertTrue("Callbacks should be balanced", callbackTester.isBalanced());
    }
    
    // ==================== Additional Visitor Implementations ====================
    
    /**
     * Visitor for validating AST structure.
     */
    public static class ValidationVisitor extends BaseASTVisitor {
        private boolean valid = true;
        private final List<String> errors = new ArrayList<>();
        
        @Override
        public void visitBinaryOperator(BinaryOperatorNode node) {
            if (node.getLeft() == null || node.getRight() == null) {
                valid = false;
                errors.add("Binary operator missing operand");
            }
            super.visitBinaryOperator(node);
        }
        
        @Override
        public void visitMethodCall(MethodCallNode node) {
            if (node.getMethodName() == null || node.getMethodName().isEmpty()) {
                valid = false;
                errors.add("Method call missing method name");
            }
            super.visitMethodCall(node);
        }
        
        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
    }
    
    /**
     * Visitor for gathering statistics about the AST.
     */
    public static class StatisticsVisitor extends BaseASTVisitor {
        private int nodeCount = 0;
        private int maxDepth = 0;
        private int currentDepth = 0;
        private final Map<Class<?>, Integer> nodeTypeCounts = new HashMap<>();
        
        private void enterNode(ASTNode node) {
            nodeCount++;
            currentDepth++;
            maxDepth = Math.max(maxDepth, currentDepth);
            
            Class<?> nodeType = node.getClass();
            nodeTypeCounts.merge(nodeType, 1, Integer::sum);
        }
        
        private void exitNode() {
            currentDepth--;
        }
        
        @Override
        public void visitBinaryOperator(BinaryOperatorNode node) {
            enterNode(node);
            super.visitBinaryOperator(node);
            exitNode();
        }
        
        @Override
        public void visitLiteral(LiteralNode node) {
            enterNode(node);
            super.visitLiteral(node);
            exitNode();
        }
        
        @Override
        public void visitIdentifier(IdentifierNode node) {
            enterNode(node);
            super.visitIdentifier(node);
            exitNode();
        }
        
        public int getNodeCount() { return nodeCount; }
        public int getMaxDepth() { return maxDepth; }
        public Map<Class<?>, Integer> getNodeTypeCounts() { return nodeTypeCounts; }
    }
    
    /**
     * Simple AST transformer for testing modification capabilities.
     */
    public static class ASTTransformer {
        public ASTNode transform(ASTNode node) {
            // Simple identity transformation for testing
            // In real implementation, would perform optimizations
            return node;
        }
    }
    
    
    // ==================== Helper Methods ====================
    
    private static LocatableToken mockToken(String text) {
        return new LocatableToken(JavaTokenTypes.IDENT, text,
                                 new LineColPos(1, 1, 0),
                                 new LineColPos(1, 1 + text.length(), text.length()));
    }
}