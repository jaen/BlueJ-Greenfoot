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

import bluej.extensions2.SourceType;
import bluej.parser.CallbackDelegate;
import bluej.parser.SourceParser;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LineColPos;
import bluej.parser.pratt.*;
import bluej.parser.pratt.InfixParselet;
import bluej.parser.pratt.PrefixParselet;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.nodes.NodeTree;
import bluej.parser.nodes.ReparseableDocument;
import bluej.parser.Token;
import bluej.parser.pratt.integration.CallbackTestingUtility.CallbackTester;
import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * Test implementation for Strategy 4: Hybrid Result Pattern.
 * 
 * This strategy combines the monadic ParseResult for error handling with callback
 * delegation for AST construction. The key innovation is that ParseResult signals
 * success/failure while the actual AST construction is delegated to the existing
 * callback system.
 * 
 * Key features tested:
 * - ParseResult for error tracking and propagation
 * - Callback delegation for AST construction
 * - Lightweight result objects that reference callback-built structures
 * - Error accumulation across multiple parsing operations
 * - Seamless integration with existing NodeFactory patterns
 */
public class HybridResultPatternTest {

    private CallbackTester callbackTester;
    private NodeFactoryBridge nodeFactory;
    private SourceParser sourceParser;
    private CallbackDelegate delegate;
    private KotlinPrattParser parser;
    private ParseletRegistry registry;
    
    @Before
    public void setUp() {
        // Create a SourceParser to get the CallbackDelegate
        sourceParser = new SourceParser(new StringReader(""), SourceType.Java);
        delegate = sourceParser.getCallbackDelegate();
        
        // Set up the test infrastructure using the unified utility
        callbackTester = CallbackTestingUtility.forDelegate(delegate);
        nodeFactory = new NodeFactoryBridge(callbackTester.getDelegate());
        registry = new ParseletRegistry();
        // Parser setup would happen here
    }

    // ==================== Core Components ====================
    
    /**
     * Result wrapper that combines ParseResult with callback delegation.
     * This allows us to track parsing success/failure while delegating
     * the actual construction work.
     */
    public static class HybridResult<T> {
        private final ParseResult<T> parseResult;
        private final Runnable callbackEmitter;
        private boolean callbacksEmitted = false;
        
        public HybridResult(ParseResult<T> parseResult, Runnable callbackEmitter) {
            this.parseResult = parseResult;
            this.callbackEmitter = callbackEmitter;
        }
        
        /**
         * Creates a successful hybrid result.
         */
        public static <T> HybridResult<T> success(T value, Runnable emitter) {
            return new HybridResult<>(ParseResult.success(value), emitter);
        }
        
        /**
         * Creates a failed hybrid result.
         */
        public static <T> HybridResult<T> failure(String message, LocatableToken token) {
            return new HybridResult<>(ParseResult.failure(message, token), () -> {});
        }
        
        /**
         * Emit callbacks if this result is successful and not already emitted.
         */
        public HybridResult<T> emitCallbacks() {
            if (parseResult.isSuccess() && !callbacksEmitted) {
                callbackEmitter.run();
                callbacksEmitted = true;
            }
            return this;
        }
        
        /**
         * Transform the value while preserving callback emission.
         */
        public <U> HybridResult<U> map(Function<T, U> mapper) {
            return new HybridResult<>(
                parseResult.map(mapper),
                callbackEmitter
            );
        }
        
        /**
         * Chain another parsing operation.
         */
        public <U> HybridResult<U> flatMap(Function<T, HybridResult<U>> mapper) {
            if (parseResult.isFailure()) {
                return new HybridResult<>(
                    ParseResult.failure(parseResult.getErrors()),
                    () -> {}
                );
            }
            
            // Emit callbacks for this result first
            emitCallbacks();
            
            // Then proceed with the next operation
            T value = parseResult.getValue();
            return mapper.apply(value);
        }
        
        /**
         * Accumulate errors from another result.
         */
        public HybridResult<T> accumulate(HybridResult<?> other) {
            ParseResult<T> accumulated = parseResult.accumulate(other.parseResult);
            
            // Combine callback emitters
            Runnable combinedEmitter = () -> {
                if (!callbacksEmitted) {
                    callbackEmitter.run();
                    callbacksEmitted = true;
                }
                if (!other.callbacksEmitted) {
                    other.callbackEmitter.run();
                    other.callbacksEmitted = true;
                }
            };
            
            return new HybridResult<>(accumulated, combinedEmitter);
        }
        
        public boolean isSuccess() {
            return parseResult.isSuccess();
        }
        
        public boolean isFailure() {
            return parseResult.isFailure();
        }
        
        public T getValue() {
            return parseResult.getValue();
        }
        
        public List<ParseResult.ParseError> getErrors() {
            return parseResult.getErrors();
        }
    }
    
    /**
     * Bridge between the Pratt parser's NodeFactory and the callback system.
     * This allows the ParseResult to delegate construction to callbacks.
     */
    public static class NodeFactoryBridge {
        private final CallbackDelegate callbacks;
        private final Map<String, NodeReference> nodeCache = new HashMap<>();
        
        public NodeFactoryBridge(CallbackDelegate callbacks) {
            this.callbacks = callbacks;
        }
        
        /**
         * Create a binary operator node through callbacks.
         * Returns a lightweight reference, actual node is built by callbacks.
         */
        public HybridResult<NodeReference> createBinaryOperator(
                NodeReference left, LocatableToken operator, NodeReference right) {
            
            String nodeId = UUID.randomUUID().toString();
            
            Runnable emitter = () -> {
                callbacks.beginExpression(operator, false);
                // Emit left operand (already parsed)
                emitNodeCallbacks(left);
                callbacks.gotBinaryOperator(operator);
                // Emit right operand (already parsed)
                emitNodeCallbacks(right);
                callbacks.endExpression(operator, false);
                
                // Cache the reference for later lookup
                NodeReference node = new NodeReference(nodeId, operator);
                nodeCache.put(nodeId, node);
            };
            
            NodeReference reference = new NodeReference(nodeId, operator);
            return HybridResult.success(reference, emitter);
        }
        
        /**
         * Create a literal node through callbacks.
         */
        public HybridResult<NodeReference> createLiteral(LocatableToken token) {
            String nodeId = UUID.randomUUID().toString();
            
            Runnable emitter = () -> {
                callbacks.gotLiteral(token);
                NodeReference node = new NodeReference(nodeId, token);
                nodeCache.put(nodeId, node);
            };
            
            NodeReference reference = new NodeReference(nodeId, token);
            return HybridResult.success(reference, emitter);
        }
        
        /**
         * Create an identifier node through callbacks.
         */
        public HybridResult<NodeReference> createIdentifier(LocatableToken token) {
            String nodeId = UUID.randomUUID().toString();
            
            Runnable emitter = () -> {
                callbacks.gotIdentifier(token);
                NodeReference node = new NodeReference(nodeId, token);
                nodeCache.put(nodeId, node);
            };
            
            NodeReference reference = new NodeReference(nodeId, token);
            return HybridResult.success(reference, emitter);
        }
        
        /**
         * Create a method call node through callbacks.
         */
        public HybridResult<NodeReference> createMethodCall(
                NodeReference target, LocatableToken methodName, List<NodeReference> arguments) {
            
            String nodeId = UUID.randomUUID().toString();
            
            Runnable emitter = () -> {
                // Emit target if present
                if (target != null) {
                    emitNodeCallbacks(target);
                    callbacks.gotMemberCall(methodName, null);
                } else {
                    callbacks.gotMethodCall(methodName);
                }
                
                // Emit arguments
                callbacks.beginArgumentList(methodName);
                for (NodeReference arg : arguments) {
                    emitNodeCallbacks(arg);
                    callbacks.endArgument();
                }
                callbacks.endArgumentList(methodName);
                
                NodeReference node = new NodeReference(nodeId, methodName);
                nodeCache.put(nodeId, node);
            };
            
            NodeReference reference = new NodeReference(nodeId, methodName);
            return HybridResult.success(reference, emitter);
        }
        
        /**
         * Create an if statement through callbacks.
         */
        public HybridResult<Void> createIfStatement(
                LocatableToken ifToken,
                NodeReference condition,
                List<NodeReference> thenStatements,
                List<NodeReference> elseStatements) {
            
            Runnable emitter = () -> {
                callbacks.beginIfStmt(ifToken);
                
                // Emit condition
                callbacks.beginExpression(ifToken, false);
                emitNodeCallbacks(condition);
                callbacks.endExpression(ifToken, false);
                
                // Emit then block
                LocatableToken blockToken = ifToken; // Would get actual token in real impl
                callbacks.beginIfCondBlock(blockToken);
                for (NodeReference stmt : thenStatements) {
                    emitNodeCallbacks(stmt);
                }
                callbacks.endIfCondBlock(blockToken, true);
                
                // Emit else block if present
                if (!elseStatements.isEmpty()) {
                    callbacks.gotElseIf(blockToken);
                    callbacks.beginIfCondBlock(blockToken);
                    for (NodeReference stmt : elseStatements) {
                        emitNodeCallbacks(stmt);
                    }
                    callbacks.endIfCondBlock(blockToken, true);
                }
                
                callbacks.endIfStmt(ifToken, true);
            };
            
            return HybridResult.success(null, emitter);
        }
        
        private void emitNodeCallbacks(NodeReference node) {
            // In real implementation, would recursively emit callbacks for the node
            // Node callbacks already scheduled or emitted
        }
        
        public NodeReference getCachedNode(String nodeId) {
            return nodeCache.get(nodeId);
        }
    }
    
    /**
     * Lightweight reference to a node being constructed through callbacks.
     * This doesn't extend ParsedNode to avoid package visibility issues.
     */
    public static class NodeReference {
        private final String nodeId;
        private final LocatableToken token;
        
        public NodeReference(String nodeId, LocatableToken token) {
            this.nodeId = nodeId;
            this.token = token;
        }
        
        public String getNodeId() {
            return nodeId;
        }
        
        public LocatableToken getToken() {
            return token;
        }
    }
    
    // ==================== Hybrid Parselets ====================
    
    /**
     * Binary operator parselet using hybrid result pattern.
     */
    public static class HybridBinaryOperatorParselet implements InfixParselet {
        private final NodeFactoryBridge factory;
        private final int precedence;
        
        public HybridBinaryOperatorParselet(NodeFactoryBridge factory, int precedence) {
            this.factory = factory;
            this.precedence = precedence;
        }
        
        @Override
        public ParseResult<ParsedNode> parse(KotlinPrattParser parser,
                                            ParsedNode left,
                                            LocatableToken operator) {
            // Parse right operand
            ParseResult<ParsedNode> rightResult = parser.parseExpressionResult(precedence);
            
            if (rightResult.isFailure()) {
                return rightResult;
            }
            
            ParsedNode right = rightResult.getValue();
            
            // Create hybrid result that defers callback emission
            // Note: In real implementation, would convert ParsedNode to NodeReference
            NodeReference leftRef = new NodeReference("left-" + System.nanoTime(), HybridResultPatternTest.mockToken("left"));
            NodeReference rightRef = new NodeReference("right-" + System.nanoTime(), HybridResultPatternTest.mockToken("right"));
            HybridResult<NodeReference> hybrid = factory.createBinaryOperator(leftRef, operator, rightRef);
            
            // Emit callbacks immediately for this subtree
            hybrid.emitCallbacks();
            
            // Return the ParseResult part for error tracking - simplified for test
            return ParseResult.success(null);
        }
        
        @Override
        public int getPrecedence() {
            return precedence;
        }
    }
    
    /**
     * Literal parselet using hybrid result pattern.
     */
    public static class HybridLiteralParselet implements PrefixParselet {
        private final NodeFactoryBridge factory;
        
        public HybridLiteralParselet(NodeFactoryBridge factory) {
            this.factory = factory;
        }
        
        @Override
        public ParseResult<ParsedNode> parse(KotlinPrattParser parser, LocatableToken token) {
            HybridResult<NodeReference> hybrid = factory.createLiteral(token);
            hybrid.emitCallbacks();
            // Return simplified result for test
            return ParseResult.success(null);
        }
    }
    
    /**
     * Method call parselet using hybrid result pattern.
     */
    public static class HybridMethodCallParselet implements InfixParselet {
        private final NodeFactoryBridge factory;
        
        public HybridMethodCallParselet(NodeFactoryBridge factory) {
            this.factory = factory;
        }
        
        @Override
        public ParseResult<ParsedNode> parse(KotlinPrattParser parser,
                                            ParsedNode target,
                                            LocatableToken parenToken) {
            // Parse method name (would be before paren in real impl)
            LocatableToken methodName = parenToken; // Simplified
            
            // Parse arguments
            List<NodeReference> arguments = new ArrayList<>();
            List<ParseResult.ParseError> allErrors = new ArrayList<>();
            
            while (!parser.check(JavaTokenTypes.RPAREN)) {
                ParseResult<ParsedNode> argResult = parser.parseExpressionResult(0);
                
                if (argResult.isFailure()) {
                    allErrors.addAll(argResult.getErrors());
                    // Try to recover by skipping to comma or rparen
                    parser.synchronize(JavaTokenTypes.COMMA, JavaTokenTypes.RPAREN);
                } else {
                    // Convert to NodeReference for simplified test
                    arguments.add(new NodeReference("arg-" + System.nanoTime(), parenToken));
                }
                
                if (parser.match(JavaTokenTypes.COMMA)) {
                    // Continue to next argument
                } else {
                    break;
                }
            }
            
            parser.consume(JavaTokenTypes.RPAREN); // Consume closing paren
            
            if (!allErrors.isEmpty()) {
                return ParseResult.failure(allErrors);
            }
            
            // Convert target to NodeReference for simplified test
            NodeReference targetRef = new NodeReference("target-" + System.nanoTime(), parenToken);
            HybridResult<NodeReference> hybrid = factory.createMethodCall(
                targetRef, methodName, arguments);
            hybrid.emitCallbacks();
            
            return ParseResult.success(null);
        }
        
        @Override
        public int getPrecedence() {
            return 100; // High precedence for method calls
        }
    }
    
    /**
     * Complex expression parselet demonstrating error accumulation.
     */
    public static class HybridComplexExpressionParselet {
        private final NodeFactoryBridge factory;
        
        public HybridComplexExpressionParselet(NodeFactoryBridge factory) {
            this.factory = factory;
        }
        
        public HybridResult<NodeReference> parseComplexExpression(
                KotlinPrattParser parser) {
            
            List<HybridResult<NodeReference>> results = new ArrayList<>();
            List<ParseResult.ParseError> allErrors = new ArrayList<>();
            
            // Parse multiple sub-expressions
            while (!parser.isAtEnd()) {
                ParseResult<ParsedNode> subResult = parser.parseExpressionResult(0);
                
                if (subResult.isFailure()) {
                    allErrors.addAll(subResult.getErrors());
                    // Try to recover
                    parser.synchronize(JavaTokenTypes.SEMI);
                } else {
                    // Wrap in hybrid result with NodeReference
                    NodeReference nodeRef = new NodeReference("expr-" + System.nanoTime(), HybridResultPatternTest.mockToken("expr"));
                    HybridResult<NodeReference> hybrid = HybridResult.success(
                        nodeRef,
                        () -> {} // Callbacks already emitted by parselets
                    );
                    results.add(hybrid);
                }
                
                if (!parser.match(JavaTokenTypes.SEMI)) {
                    break;
                }
            }
            
            if (!allErrors.isEmpty()) {
                return HybridResult.failure("Complex expression failed", null);
            }
            
            // Combine all results
            HybridResult<NodeReference> combined = results.get(0);
            for (int i = 1; i < results.size(); i++) {
                combined = combined.accumulate(results.get(i));
            }
            
            return combined;
        }
    }
    
    // ==================== Test Cases ====================
    
    @Test
    public void testHybridResultSuccess() {
        // Create a simple literal
        HybridResult<NodeReference> result = nodeFactory.createLiteral(mockToken("42"));
        
        assertTrue("Result should be successful", result.isSuccess());
        assertNotNull("Should have a value", result.getValue());
        assertEquals("Should have no errors", 0, result.getErrors().size());
        
        // Emit callbacks
        result.emitCallbacks();
        
        // Verify callback was recorded
        assertTrue("Should have recorded literal callback",
            callbackTester.hasCallback("gotLiteral"));
    }
    
    @Test
    public void testHybridResultFailure() {
        HybridResult<NodeReference> result = HybridResult.failure(
            "Unexpected token", mockToken("@"));
        
        assertTrue("Result should be failure", result.isFailure());
        assertNull("Should have no value", result.getValue());
        assertEquals("Should have one error", 1, result.getErrors().size());
        
        // Callbacks should not be emitted for failures
        result.emitCallbacks();
        assertEquals("No callbacks for failures", 0, callbackTester.getCallbackCount());
    }
    
    @Test
    public void testBinaryOperatorHybrid() {
        // Create left and right operands
        NodeReference left = new NodeReference("left", mockToken("a"));
        NodeReference right = new NodeReference("right", mockToken("b"));
        
        // Create binary operator
        HybridResult<NodeReference> result = nodeFactory.createBinaryOperator(
            left, mockToken("+"), right);
        
        assertTrue("Result should be successful", result.isSuccess());
        
        // Emit callbacks
        result.emitCallbacks();
        
        // Verify callback sequence
        List<String> expectedCallbacks = Arrays.asList(
            "beginExpression",
            "gotBinaryOperator",
            "endExpression"
        );
        
        assertTrue("Should have correct callback sequence",
            callbackTester.hasCallbackSequence(expectedCallbacks));
    }
    
    @Test
    public void testMethodCallHybrid() {
        // Create target and arguments
        NodeReference target = new NodeReference("obj", mockToken("obj"));
        List<NodeReference> args = Arrays.asList(
            new NodeReference("arg1", mockToken("1")),
            new NodeReference("arg2", mockToken("2"))
        );
        
        // Create method call
        HybridResult<NodeReference> result = nodeFactory.createMethodCall(
            target, mockToken("method"), args);
        
        assertTrue("Result should be successful", result.isSuccess());
        
        // Emit callbacks
        result.emitCallbacks();
        
        // Verify callbacks include method call and argument list
        assertTrue("Should have method call callback",
            callbackTester.hasCallback("gotMemberCall"));
        assertTrue("Should have argument list callbacks",
            callbackTester.hasCallback("beginArgumentList") &&
            callbackTester.hasCallback("endArgumentList"));
    }
    
    @Test
    public void testErrorAccumulation() {
        // Test accumulating errors across multiple operations
        HybridResult<NodeReference> result1 = HybridResult.failure(
            "Error 1", mockToken("bad1"));
        HybridResult<NodeReference> result2 = HybridResult.failure(
            "Error 2", mockToken("bad2"));
        
        HybridResult<NodeReference> combined = result1.accumulate(result2);
        
        assertEquals("Should accumulate both errors", 2, combined.getErrors().size());
        assertTrue("Should be failure", combined.isFailure());
    }
    
    @Test
    public void testFlatMapChaining() {
        // Create a chain of operations
        HybridResult<NodeReference> result = nodeFactory.createLiteral(mockToken("1"))
            .flatMap(left -> nodeFactory.createBinaryOperator(
                left, mockToken("+"), new NodeReference("2", mockToken("2"))))
            .flatMap(expr -> nodeFactory.createBinaryOperator(
                expr, mockToken("*"), new NodeReference("3", mockToken("3"))));
        
        assertTrue("Chain should be successful", result.isSuccess());
        
        // All callbacks should be emitted in order
        result.emitCallbacks();
        
        assertTrue("Should have multiple expression callbacks",
            callbackTester.getCallbackCount() > 3);
    }
    
    @Test
    public void testIfStatementHybrid() {
        // Create condition and branches
        NodeReference condition = new NodeReference("cond", mockToken("true"));
        List<NodeReference> thenBranch = Arrays.asList(
            new NodeReference("stmt1", mockToken("x")),
            new NodeReference("stmt2", mockToken("y"))
        );
        List<NodeReference> elseBranch = Arrays.asList(
            new NodeReference("stmt3", mockToken("z"))
        );
        
        // Create if statement
        HybridResult<Void> result = nodeFactory.createIfStatement(
            mockToken("if"), condition, thenBranch, elseBranch);
        
        assertTrue("Result should be successful", result.isSuccess());
        
        // Emit callbacks
        result.emitCallbacks();
        
        // Verify complete if statement callback sequence
        List<String> expectedCallbacks = Arrays.asList(
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
        
        assertTrue("Should have complete if statement callbacks",
            callbackTester.hasCallbackSequence(expectedCallbacks));
    }
    
    @Test
    public void testDeferredCallbackEmission() {
        // Create results but don't emit callbacks yet
        HybridResult<NodeReference> result1 = nodeFactory.createLiteral(mockToken("1"));
        HybridResult<NodeReference> result2 = nodeFactory.createLiteral(mockToken("2"));
        
        assertEquals("No callbacks emitted yet", 0, callbackTester.getCallbackCount());
        
        // Emit first result's callbacks
        result1.emitCallbacks();
        assertEquals("One literal callback", 1, callbackTester.getCallbackCount());
        
        // Emit second result's callbacks
        result2.emitCallbacks();
        assertEquals("Two literal callbacks", 2, callbackTester.getCallbackCount());
        
        // Multiple emit calls should be idempotent
        result1.emitCallbacks();
        assertEquals("Still two callbacks", 2, callbackTester.getCallbackCount());
    }
    
    @Test
    public void testNodeCaching() {
        // Create and emit a literal
        HybridResult<NodeReference> result = nodeFactory.createLiteral(mockToken("42"));
        result.emitCallbacks();
        
        // The node should be cached
        NodeReference ref = result.getValue();
        NodeReference cached = nodeFactory.getCachedNode(ref.getNodeId());
        
        assertNotNull("Node should be cached", cached);
        assertEquals("Cached node should match", ref.getNodeId(),
            cached.getNodeId());
    }
    
    // CallbackRecorder has been replaced by the unified CallbackTestingUtility
    
    // ==================== Helper Methods ====================
    
    private static LocatableToken mockToken(String text) {
        LineColPos begin = new LineColPos(1, 1, 0);
        LineColPos end = new LineColPos(1, 1 + text.length(), text.length());
        return new LocatableToken(JavaTokenTypes.IDENT, text, begin, end);
    }
}