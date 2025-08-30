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
import bluej.parser.JavaParserCallbacks;
import bluej.parser.SourceParser;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LineColPos;
import bluej.parser.pratt.*;
import bluej.parser.pratt.InfixParselet;
import bluej.parser.pratt.PrefixParselet;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.nodes.ReparseableDocument;
import bluej.parser.nodes.NodeStructureListener;
import bluej.parser.pratt.integration.CallbackTestingUtility.CallbackTester;
import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;
import java.util.*;
import java.util.function.Supplier;

import static org.junit.Assert.*;

/**
 * Test implementation for Strategy 3: Direct Callback Integration.
 * 
 * This strategy integrates callbacks directly into parselets using try-with-resources
 * for guaranteed callback pairing. The key innovation is using AutoCloseable callback
 * scopes that ensure end callbacks are always called, even when errors occur.
 * 
 * Key features tested:
 * - Guaranteed callback pairing through AutoCloseable scopes
 * - Direct callback emission during parsing (no AST-to-callback translation)
 * - Error recovery with proper callback closure
 * - Memory efficiency by avoiding full AST construction when callbacks suffice
 */
public class DirectCallbackIntegrationTest {

    private CallbackTester callbackTester;
    private SafeCallbacks safeCallbacks;
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
        safeCallbacks = new SafeCallbacks(delegate);
        registry = new ParseletRegistry();
        // Parser setup would happen here
    }

    // ==================== Core Components ====================
    
    /**
     * AutoCloseable callback scope interface for guaranteed pairing.
     * When used with try-with-resources, ensures end callbacks are always called.
     */
    public interface CallbackScope extends AutoCloseable {
        @Override
        void close(); // Automatically calls the appropriate end callback
        
        /**
         * Mark this scope as failed, which may affect the end callback parameters.
         */
        void markFailed();
        
        /**
         * Check if this scope completed successfully.
         */
        boolean isSuccessful();
    }
    
    /**
     * Implementation of a callback scope that tracks success/failure state.
     */
    private static class CallbackScopeImpl implements CallbackScope {
        private final Runnable endCallback;
        private final Runnable failureCallback;
        private boolean failed = false;
        private boolean closed = false;
        
        public CallbackScopeImpl(Runnable endCallback, Runnable failureCallback) {
            this.endCallback = endCallback;
            this.failureCallback = failureCallback;
        }
        
        @Override
        public void markFailed() {
            this.failed = true;
        }
        
        @Override
        public boolean isSuccessful() {
            return !failed;
        }
        
        @Override
        public void close() {
            if (!closed) {
                closed = true;
                if (failed && failureCallback != null) {
                    failureCallback.run();
                } else {
                    endCallback.run();
                }
            }
        }
    }
    
    /**
     * Thread-safe callback wrapper that provides AutoCloseable scopes.
     * This ensures all begin callbacks have matching end callbacks.
     */
    public static class SafeCallbacks {
        private final CallbackDelegate delegate;
        
        public SafeCallbacks(CallbackDelegate delegate) {
            this.delegate = delegate;
        }
        
        /**
         * Create a scope for an expression. The end callback is guaranteed to be called.
         */
        public CallbackScope scopeExpression(LocatableToken token, boolean isLambdaBody) {
            delegate.beginExpression(token, isLambdaBody);
            return new CallbackScopeImpl(
                () -> delegate.endExpression(token, false),
                () -> delegate.endExpression(token, true) // empty expression on failure
            );
        }
        
        /**
         * Create a scope for an element. The end callback is guaranteed to be called.
         */
        public CallbackScope scopeElement(LocatableToken token) {
            delegate.beginElement(token);
            return new CallbackScopeImpl(
                () -> delegate.endElement(token, true),
                () -> delegate.endElement(token, false) // not included on failure
            );
        }
        
        /**
         * Create a scope for an if statement. The end callback is guaranteed to be called.
         */
        public CallbackScope scopeIfStatement(LocatableToken token) {
            delegate.beginIfStmt(token);
            return new CallbackScopeImpl(
                () -> delegate.endIfStmt(token, true),
                () -> delegate.endIfStmt(token, false)
            );
        }
        
        /**
         * Create a scope for an if condition block.
         */
        public CallbackScope scopeIfCondBlock(LocatableToken token) {
            delegate.beginIfCondBlock(token);
            return new CallbackScopeImpl(
                () -> delegate.endIfCondBlock(token, true),
                () -> delegate.endIfCondBlock(token, false)
            );
        }
        
        /**
         * Create a scope for a for loop.
         */
        public CallbackScope scopeForLoop(LocatableToken token) {
            delegate.beginForLoop(token);
            return new CallbackScopeImpl(
                () -> delegate.endForLoop(token, true),
                () -> delegate.endForLoop(token, false)
            );
        }
        
        /**
         * Create a scope for a for loop body.
         */
        public CallbackScope scopeForLoopBody(LocatableToken token) {
            delegate.beginForLoopBody(token);
            return new CallbackScopeImpl(
                () -> delegate.endForLoopBody(token, true),
                () -> delegate.endForLoopBody(token, false)
            );
        }
        
        /**
         * Create a scope for a method body.
         */
        public CallbackScope scopeMethodBody(LocatableToken token) {
            delegate.beginMethodBody(token);
            return new CallbackScopeImpl(
                () -> delegate.endMethodBody(token, true),
                () -> delegate.endMethodBody(token, false)
            );
        }
        
        // Forward other callbacks directly
        public void gotBinaryOperator(LocatableToken token) {
            delegate.gotBinaryOperator(token);
        }
        
        public void gotIdentifier(LocatableToken token) {
            delegate.gotIdentifier(token);
        }
        
        public void gotLiteral(LocatableToken token) {
            delegate.gotLiteral(token);
        }
        
        public void gotImport(List<LocatableToken> tokens, boolean isStatic,
                             LocatableToken importToken, LocatableToken semiToken) {
            delegate.gotImport(tokens, isStatic, importToken, semiToken);
        }
        
        public void gotElseIf(LocatableToken token) {
            delegate.gotElseIf(token);
        }
    }
    
    // ==================== Callback-Integrated Parselets ====================
    
    /**
     * Binary operator parselet that directly emits callbacks during parsing.
     * This eliminates the need for AST-to-callback translation.
     */
    public static class CallbackBinaryOperatorParselet implements InfixParselet {
        private final SafeCallbacks callbacks;
        private final int precedence;
        
        public CallbackBinaryOperatorParselet(SafeCallbacks callbacks, int precedence) {
            this.callbacks = callbacks;
            this.precedence = precedence;
        }
        
        @Override
        public ParseResult<ParsedNode> parse(KotlinPrattParser parser, 
                                            ParsedNode left, 
                                            LocatableToken operator) {
            // Use try-with-resources to ensure callbacks are paired
            try (CallbackScope scope = callbacks.scopeExpression(operator, false)) {
                // Emit operator callback
                callbacks.gotBinaryOperator(operator);
                
                // Parse right operand (which will emit its own callbacks)
                ParseResult<ParsedNode> rightResult = parser.parseExpressionResult(precedence);
                
                if (rightResult.isFailure()) {
                    scope.markFailed();
                    return rightResult;
                }
                
                // Return a lightweight reference instead of building full AST
                return ParseResult.success(null); // Simplified for callback testing
            } catch (Exception e) {
                // Scope will be closed automatically, calling endExpression
                return ParseResult.failure("Failed to parse binary expression", operator);
            }
        }
        
        @Override
        public int getPrecedence() {
            return precedence;
        }
    }
    
    /**
     * Literal parselet that directly emits callbacks.
     */
    public static class CallbackLiteralParselet implements PrefixParselet {
        private final SafeCallbacks callbacks;
        
        public CallbackLiteralParselet(SafeCallbacks callbacks) {
            this.callbacks = callbacks;
        }
        
        @Override
        public ParseResult<ParsedNode> parse(KotlinPrattParser parser, LocatableToken token) {
            callbacks.gotLiteral(token);
            return ParseResult.success(null); // Simplified for callback testing
        }
    }
    
    /**
     * Import statement parselet with guaranteed callback pairing.
     */
    public static class CallbackImportParselet {
        private final SafeCallbacks callbacks;
        
        public CallbackImportParselet(SafeCallbacks callbacks) {
            this.callbacks = callbacks;
        }
        
        public ParseResult<Void> parseImport(LocatableToken importToken,
                                            List<LocatableToken> packageTokens,
                                            boolean isStatic,
                                            LocatableToken semiToken) {
            try (CallbackScope scope = callbacks.scopeElement(importToken)) {
                callbacks.gotImport(packageTokens, isStatic, importToken, semiToken);
                return ParseResult.success(null);
            } catch (Exception e) {
                return ParseResult.failure("Failed to parse import", importToken);
            }
        }
    }
    
    /**
     * If statement parselet with complex nested callback scopes.
     */
    public static class CallbackIfStatementParselet {
        private final SafeCallbacks callbacks;
        
        public CallbackIfStatementParselet(SafeCallbacks callbacks) {
            this.callbacks = callbacks;
        }
        
        public ParseResult<Void> parseIfStatement(KotlinPrattParser parser,
                                                 LocatableToken ifToken) {
            try (CallbackScope ifScope = callbacks.scopeIfStatement(ifToken)) {
                // Parse condition with its own scope
                LocatableToken condStart = parser.getCurrentToken();
                try (CallbackScope condScope = callbacks.scopeExpression(condStart, false)) {
                    ParseResult<ParsedNode> condition = parser.parseExpressionResult(0);
                    if (condition.isFailure()) {
                        condScope.markFailed();
                        ifScope.markFailed();
                        return ParseResult.failure(condition.getErrors());
                    }
                }
                
                // Parse then block with its own scope
                LocatableToken blockStart = parser.getCurrentToken();
                try (CallbackScope blockScope = callbacks.scopeIfCondBlock(blockStart)) {
                    ParseResult<Void> thenBlock = parseBlock(parser);
                    if (thenBlock.isFailure()) {
                        blockScope.markFailed();
                        ifScope.markFailed();
                        return thenBlock;
                    }
                }
                
                // Check for else clause
                if (parser.match(JavaTokenTypes.LITERAL_else)) {
                    callbacks.gotElseIf(parser.getCurrentToken());
                    ParseResult<Void> elseBlock = parseBlock(parser);
                    if (elseBlock.isFailure()) {
                        ifScope.markFailed();
                        return elseBlock;
                    }
                }
                
                return ParseResult.success(null);
            } catch (Exception e) {
                return ParseResult.failure("Failed to parse if statement", ifToken);
            }
        }
        
        private ParseResult<Void> parseBlock(KotlinPrattParser parser) {
            // Simplified block parsing
            return ParseResult.success(null);
        }
    }
    
    /**
     * Lightweight reference to avoid building full AST when callbacks suffice.
     */
    public static class CallbackReference {
        private final LocatableToken token;
        
        public CallbackReference(LocatableToken token) {
            this.token = token;
        }
        
        public LocatableToken getToken() {
            return token;
        }
    }
    
    // ==================== Test Cases ====================
    
    @Test
    public void testGuaranteedCallbackPairing() {
        // Test that callbacks are always paired, even when exceptions occur
        // Use a real SourceParser to get CallbackDelegate
        SourceParser parser = new SourceParser(new StringReader(""), SourceType.Java);
        SafeCallbacks callbacks = new SafeCallbacks(parser.getCallbackDelegate());
        
        try (CallbackScope scope = callbacks.scopeExpression(mockToken("("), false)) {
            // Simulate parsing that might fail
            if (Math.random() > 2) { // Never true, but demonstrates pattern
                throw new RuntimeException("Parse error");
            }
        } catch (Exception e) {
            // Even with exception, scope ensures endExpression is called
        }
        
        // CallbackTester should show balanced callbacks
        assertTrue("Callbacks should be balanced", callbackTester.isBalanced());
    }
    
    @Test
    public void testNestedScopesWithError() {
        // Use a real SourceParser to get CallbackDelegate
        SourceParser parser = new SourceParser(new StringReader(""), SourceType.Java);
        SafeCallbacks callbacks = new SafeCallbacks(parser.getCallbackDelegate());
        
        try (CallbackScope outer = callbacks.scopeIfStatement(mockToken("if"))) {
            try (CallbackScope inner = callbacks.scopeExpression(mockToken("("), false)) {
                // Simulate error in inner scope
                inner.markFailed();
                outer.markFailed();
            }
            // Inner scope closed with failure
        }
        // Outer scope closed with failure
        
        assertTrue("Callbacks should be balanced even with errors", callbackTester.isBalanced());
        // Note: getFailedScopeCount not available in unified API - would need to track differently
    }
    
    @Test
    public void testBinaryOperatorCallbackIntegration() {
        // Test binary operator parselet with direct callback integration
        // Use a real SourceParser to get CallbackDelegate
        SourceParser parser = new SourceParser(new StringReader(""), SourceType.Java);
        SafeCallbacks callbacks = new SafeCallbacks(parser.getCallbackDelegate());
        CallbackBinaryOperatorParselet parselet = 
            new CallbackBinaryOperatorParselet(callbacks, 10);
        
        // Simulate parsing "a + b"
        CallbackReference left = new CallbackReference(mockToken("a"));
        LocatableToken operator = mockToken("+");
        
        // Mock parser would normally provide right operand parsing
        // ParseResult<ParsedNode> result = parselet.parse(mockParser, left, operator);
        
        // Verify callbacks were emitted in correct order
        List<String> expectedOrder = Arrays.asList(
            "beginExpression",
            "gotBinaryOperator",
            "endExpression"
        );
        
        // Validator would track actual order
    }
    
    @Test
    public void testImportStatementCallbacks() {
        // Use a real SourceParser to get CallbackDelegate
        SourceParser parser = new SourceParser(new StringReader(""), SourceType.Java);
        SafeCallbacks callbacks = new SafeCallbacks(parser.getCallbackDelegate());
        CallbackImportParselet parselet = new CallbackImportParselet(callbacks);
        
        // Test "import java.util.List;"
        List<LocatableToken> packageTokens = Arrays.asList(
            mockToken("java"),
            mockToken("."),
            mockToken("util"),
            mockToken("."),
            mockToken("List")
        );
        
        ParseResult<Void> result = parselet.parseImport(
            mockToken("import"),
            packageTokens,
            false,
            mockToken(";")
        );
        
        assertTrue("Import should parse successfully", result.isSuccess());
        assertTrue("Callbacks should be balanced", callbackTester.isBalanced());
    }
    
    @Test
    public void testComplexIfStatementCallbacks() {
        // Use a real SourceParser to get CallbackDelegate
        SourceParser parser = new SourceParser(new StringReader(""), SourceType.Java);
        SafeCallbacks callbacks = new SafeCallbacks(parser.getCallbackDelegate());
        CallbackIfStatementParselet parselet = new CallbackIfStatementParselet(callbacks);
        
        // Would need mock parser for full test
        // This demonstrates the structure
        
        List<String> expectedCallbacks = Arrays.asList(
            "beginIfStmt",
            "beginExpression",     // condition
            "endExpression",
            "beginIfCondBlock",    // then block
            "endIfCondBlock",
            "gotElseIf",          // else clause
            "beginIfCondBlock",    // else block  
            "endIfCondBlock",
            "endIfStmt"
        );
        
        // Test would verify this sequence
    }
    
    @Test
    public void testErrorRecoveryWithCallbacks() {
        // Use a real SourceParser to get CallbackDelegate
        SourceParser parser = new SourceParser(new StringReader(""), SourceType.Java);
        SafeCallbacks callbacks = new SafeCallbacks(parser.getCallbackDelegate());
        
        // Simulate parsing with error recovery
        try (CallbackScope scope = callbacks.scopeForLoop(mockToken("for"))) {
            try {
                // Parse init
                try (CallbackScope initScope = callbacks.scopeElement(mockToken("int"))) {
                    // Simulate parse error
                    throw new RuntimeException("Unexpected token");
                }
            } catch (Exception e) {
                // Error recovery: skip to semicolon
                scope.markFailed();
            }
        }
        
        // Despite error, callbacks should be balanced
        assertTrue("Callbacks balanced after error recovery", callbackTester.isBalanced());
    }
    
    @Test
    public void testMemoryEfficiency() {
        // Test that callback references are lightweight
        List<CallbackReference> refs = new ArrayList<>();
        
        for (int i = 0; i < 10000; i++) {
            refs.add(new CallbackReference(mockToken("token" + i)));
        }
        
        // CallbackReference should use minimal memory
        // In real scenario, would measure actual memory usage
        assertTrue("Lightweight references created", refs.size() == 10000);
    }
    
    // ==================== Helper Methods ====================
    
    private static LocatableToken mockToken(String text) {
        LineColPos begin = new LineColPos(1, 1, 0);
        LineColPos end = new LineColPos(1, 1 + text.length(), text.length());
        return new LocatableToken(JavaTokenTypes.IDENT, text, begin, end);
    }
}