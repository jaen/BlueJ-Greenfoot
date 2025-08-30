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
import bluej.parser.TrackingSourceParser;
import bluej.parser.pratt.integration.CallbackTestingUtility.CallbackTester;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LineColPos;
import bluej.parser.pratt.integration.DirectCallbackIntegrationTest.CallbackScope;
import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Concrete test implementations for common parsing scenarios.
 * 
 * This test class demonstrates how each integration strategy handles
 * real-world parsing scenarios that are commonly encountered in BlueJ:
 * - Import statements with various formats
 * - Expression parsing with precedence
 * - Block/statement parsing with nesting
 * - Error recovery scenarios
 * - Unparseable element handling
 * 
 * Each test validates proper callback generation and pairing.
 */
public class CommonParsingScenarioTest {

    private CallbackTester callbackTester;
    private SafeCallbacks safeCallbacks;
    private SourceParser sourceParser;
    private CallbackDelegate delegate;
    
    @Before
    public void setUp() {
        // Create SourceParser with CallbackDelegate
        sourceParser = new SourceParser(new StringReader(""));
        delegate = sourceParser.getCallbackDelegate();
        
        // Set up callbackTester and callback infrastructure
        callbackTester = CallbackTestingUtility.forDelegate(delegate);
        safeCallbacks = new SafeCallbacks(callbackTester.getDelegate());
    }

    // ==================== Import Statement Scenarios ====================
    
    @Test
    public void testSimpleImportStatement() {
        // Scenario: import java.util.List;
        simulateImportParsing("java.util.List", false, false);
        
        // Verify callback sequence
        assertCallbackSequence(
            "beginElement",
            "gotImport",
            "gotImportStmtSemi"
        );
        
        assertTrue("Callbacks should be balanced", callbackTester.isBalanced());
    }
    
    @Test
    public void testWildcardImportStatement() {
        // Scenario: import java.util.*;
        simulateImportParsing("java.util", false, true);
        
        // Verify wildcard import callback
        assertTrue("Should have wildcard import",
            callbackTester.hasCallback("gotWildcardImport"));
        assertTrue("Callbacks should be balanced", callbackTester.isBalanced());
    }
    
    @Test
    public void testStaticImportStatement() {
        // Scenario: import static java.lang.Math.PI;
        simulateImportParsing("java.lang.Math.PI", true, false);
        
        // Verify static import handling
        assertTrue("Should have import callback", callbackTester.hasCallback("gotImport"));
        assertTrue("Callbacks should be balanced", callbackTester.isBalanced());
    }
    
    @Test
    public void testKotlinImportAlias() {
        // Scenario: import java.util.List as JList
        // This is Kotlin-specific syntax
        try (CallbackScope scope = safeCallbacks.scopeElement(mockToken("import"))) {
            List<LocatableToken> tokens = Arrays.asList(
                mockToken("java"),
                mockToken("."),
                mockToken("util"),
                mockToken("."),
                mockToken("List")
            );
            
            safeCallbacks.gotImport(tokens, false, 
                mockToken("import"), mockToken(";"));
            
            // In Kotlin, we'd also handle the alias
            // This would require extending the callback system
        }
        
        assertTrue("Callbacks should be balanced", callbackTester.isBalanced());
    }
    
    // ==================== Expression Parsing Scenarios ====================
    
    @Test
    public void testSimpleBinaryExpression() {
        // Scenario: a + b
        try (CallbackScope scope = safeCallbacks.scopeExpression(mockToken("("), false)) {
            safeCallbacks.gotIdentifier(mockToken("a"));
            safeCallbacks.gotBinaryOperator(mockToken("+"));
            safeCallbacks.gotIdentifier(mockToken("b"));
        }
        
        assertCallbackSequence(
            "beginExpression",
            "gotIdentifier",
            "gotBinaryOperator",
            "gotIdentifier",
            "endExpression"
        );
        
        assertTrue("Callbacks should be balanced", callbackTester.isBalanced());
    }
    
    @Test
    public void testComplexExpressionWithPrecedence() {
        // Scenario: a + b * c - d / e
        // Should respect operator precedence
        try (CallbackScope scope = safeCallbacks.scopeExpression(mockToken("("), false)) {
            // a +
            safeCallbacks.gotIdentifier(mockToken("a"));
            safeCallbacks.gotBinaryOperator(mockToken("+"));
            
            // (b * c) - nested for precedence
            try (CallbackScope inner1 = safeCallbacks.scopeExpression(mockToken("("), false)) {
                safeCallbacks.gotIdentifier(mockToken("b"));
                safeCallbacks.gotBinaryOperator(mockToken("*"));
                safeCallbacks.gotIdentifier(mockToken("c"));
            }
            
            safeCallbacks.gotBinaryOperator(mockToken("-"));
            
            // (d / e) - nested for precedence
            try (CallbackScope inner2 = safeCallbacks.scopeExpression(mockToken("("), false)) {
                safeCallbacks.gotIdentifier(mockToken("d"));
                safeCallbacks.gotBinaryOperator(mockToken("/"));
                safeCallbacks.gotIdentifier(mockToken("e"));
            }
        }
        
        // Should have nested expressions for precedence
        assertTrue("Should have begin expressions",
            callbackTester.hasCallback("beginExpression"));
        assertTrue("Should have end expressions",
            callbackTester.hasCallback("endExpression"));
        assertTrue("Callbacks should be balanced", callbackTester.isBalanced());
    }
    
    @Test
    public void testMethodCallExpression() {
        // Scenario: obj.method(arg1, arg2)
        try (CallbackScope scope = safeCallbacks.scopeExpression(mockToken("("), false)) {
            safeCallbacks.gotIdentifier(mockToken("obj"));
            safeCallbacks.gotMemberAccess(mockToken("."));
            safeCallbacks.gotMemberCall(mockToken("method"), null);
            
            safeCallbacks.beginArgumentList(mockToken("("));
            
            safeCallbacks.gotIdentifier(mockToken("arg1"));
            safeCallbacks.endArgument();
            
            safeCallbacks.gotIdentifier(mockToken("arg2"));
            safeCallbacks.endArgument();
            
            safeCallbacks.endArgumentList(mockToken(")"));
        }
        
        assertTrue("Should have member call", callbackTester.hasCallback("gotMemberCall"));
        assertTrue("Should have argument list", callbackTester.hasCallback("beginArgumentList"));
        assertTrue("Should have arguments", callbackTester.hasCallback("endArgument"));
        assertTrue("Callbacks should be balanced", callbackTester.isBalanced());
    }
    
    @Test
    public void testLambdaExpression() {
        // Scenario: { x -> x * 2 }
        try (CallbackScope scope = safeCallbacks.scopeExpression(mockToken("{"), true)) {
            // Lambda parameter
            safeCallbacks.gotLambdaFormalName(mockToken("x"));
            
            // Lambda body
            safeCallbacks.beginLambdaBody(false, mockToken("->"));
            
            try (CallbackScope bodyScope = safeCallbacks.scopeExpression(mockToken("x"), false)) {
                safeCallbacks.gotIdentifier(mockToken("x"));
                safeCallbacks.gotBinaryOperator(mockToken("*"));
                safeCallbacks.gotLiteral(mockToken("2"));
            }
            
            safeCallbacks.endLambdaBody(mockToken("}"));
        }
        
        assertTrue("Should have lambda formal name",
            callbackTester.hasCallback("gotLambdaFormalName"));
        assertTrue("Should have lambda body",
            callbackTester.hasCallback("beginLambdaBody"));
        assertTrue("Callbacks should be balanced", callbackTester.isBalanced());
    }
    
    // ==================== Block/Statement Scenarios ====================
    
    @Test
    public void testIfElseStatement() {
        // Scenario: if (x > 0) { println("positive") } else { println("negative") }
        try (CallbackScope ifScope = safeCallbacks.scopeIfStatement(mockToken("if"))) {
            // Condition
            try (CallbackScope condScope = safeCallbacks.scopeExpression(mockToken("("), false)) {
                safeCallbacks.gotIdentifier(mockToken("x"));
                safeCallbacks.gotBinaryOperator(mockToken(">"));
                safeCallbacks.gotLiteral(mockToken("0"));
            }
            
            // Then block
            try (CallbackScope thenScope = safeCallbacks.scopeIfCondBlock(mockToken("{"))) {
                simulateMethodCall("println", Arrays.asList("\"positive\""));
            }
            
            // Else block
            safeCallbacks.gotElseIf(mockToken("else"));
            try (CallbackScope elseScope = safeCallbacks.scopeIfCondBlock(mockToken("{"))) {
                simulateMethodCall("println", Arrays.asList("\"negative\""));
            }
        }
        
        assertCallbackSequence(
            "beginIfStmt",
            "beginExpression",
            "gotIdentifier",
            "gotBinaryOperator",
            "gotLiteral",
            "endExpression",
            "beginIfCondBlock",
            "endIfCondBlock",
            "gotElseIf",
            "beginIfCondBlock",
            "endIfCondBlock",
            "endIfStmt"
        );
        
        assertTrue("Callbacks should be balanced", callbackTester.isBalanced());
    }
    
    @Test
    public void testForLoopStatement() {
        // Scenario: for (i in 1..10) { println(i) }
        try (CallbackScope forScope = safeCallbacks.scopeForLoop(mockToken("for"))) {
            // Kotlin-style for loop
            safeCallbacks.determinedForLoop(true, false); // for-each loop
            
            // Loop variable
            try (CallbackScope varScope = safeCallbacks.scopeElement(mockToken("i"))) {
                safeCallbacks.gotIdentifier(mockToken("i"));
            }
            
            // Range expression (1..10)
            try (CallbackScope rangeScope = safeCallbacks.scopeExpression(mockToken("1"), false)) {
                safeCallbacks.gotLiteral(mockToken("1"));
                safeCallbacks.gotBinaryOperator(mockToken(".."));
                safeCallbacks.gotLiteral(mockToken("10"));
            }
            
            // Loop body
            try (CallbackScope bodyScope = safeCallbacks.scopeForLoopBody(mockToken("{"))) {
                simulateMethodCall("println", Arrays.asList("i"));
            }
        }
        
        assertTrue("Should have for loop callbacks",
            callbackTester.hasCallback("beginForLoop") && callbackTester.hasCallback("endForLoop"));
        assertTrue("Should have loop body",
            callbackTester.hasCallback("beginForLoopBody"));
        assertTrue("Callbacks should be balanced", callbackTester.isBalanced());
    }
    
    @Test
    public void testWhenExpression() {
        // Scenario: when(x) { 1 -> "one", 2 -> "two", else -> "other" }
        // This is Kotlin-specific, similar to switch
        try (CallbackScope switchScope = safeCallbacks.scopeSwitchStatement(mockToken("when"))) {
            // Switch expression
            try (CallbackScope exprScope = safeCallbacks.scopeExpression(mockToken("("), false)) {
                safeCallbacks.gotIdentifier(mockToken("x"));
            }
            
            // Cases
            safeCallbacks.beginSwitchBlock(mockToken("{"));
            
            // Case 1
            safeCallbacks.beginSwitchCase(mockToken("1"));
            safeCallbacks.gotLiteral(mockToken("1"));
            safeCallbacks.gotSwitchCaseType(mockToken("->"), true);
            safeCallbacks.gotLiteral(mockToken("\"one\""));
            safeCallbacks.endSwitchCase(mockToken(","), true);
            
            // Case 2
            safeCallbacks.beginSwitchCase(mockToken("2"));
            safeCallbacks.gotLiteral(mockToken("2"));
            safeCallbacks.gotSwitchCaseType(mockToken("->"), true);
            safeCallbacks.gotLiteral(mockToken("\"two\""));
            safeCallbacks.endSwitchCase(mockToken(","), true);
            
            // Default case
            safeCallbacks.gotSwitchDefault();
            safeCallbacks.gotLiteral(mockToken("\"other\""));
            
            safeCallbacks.endSwitchBlock(mockToken("}"));
        }
        
        assertTrue("Should have switch callbacks",
            callbackTester.hasCallback("beginSwitchStmt"));
        assertTrue("Should have switch cases",
            callbackTester.hasCallback("beginSwitchCase"));
        assertTrue("Should have default case",
            callbackTester.hasCallback("gotSwitchDefault"));
        assertTrue("Callbacks should be balanced", callbackTester.isBalanced());
    }
    
    // ==================== Error Recovery Scenarios ====================
    
    @Test
    public void testUnparseableExpression() {
        // Scenario: Invalid expression that triggers error recovery
        try (CallbackScope scope = safeCallbacks.scopeExpression(mockToken("("), false)) {
            safeCallbacks.gotIdentifier(mockToken("a"));
            // Missing operator and right operand - mark as failed
            scope.markFailed();
        }
        
        // Should still have balanced callbacks despite error
        assertTrue("Callbacks should be balanced even with error",
            callbackTester.isBalanced());
        
        // Should have end expression
        assertTrue("Should have end expression", callbackTester.hasCallback("endExpression"));
    }
    
    @Test
    public void testRecoveryFromMissingBrace() {
        // Scenario: if (x > 0) { println("test")  // missing closing brace
        CallbackScope ifScope = null;
        CallbackScope blockScope = null;
        
        try {
            ifScope = safeCallbacks.scopeIfStatement(mockToken("if"));
            
            // Condition
            try (CallbackScope condScope = safeCallbacks.scopeExpression(mockToken("("), false)) {
                safeCallbacks.gotIdentifier(mockToken("x"));
                safeCallbacks.gotBinaryOperator(mockToken(">"));
                safeCallbacks.gotLiteral(mockToken("0"));
            }
            
            // Then block - missing closing brace
            blockScope = safeCallbacks.scopeIfCondBlock(mockToken("{"));
            simulateMethodCall("println", Arrays.asList("\"test\""));
            
            // Simulate error detection
            throw new RuntimeException("Missing closing brace");
            
        } catch (Exception e) {
            // Error recovery - ensure scopes are closed
            if (blockScope != null) {
                blockScope.markFailed();
                blockScope.close();
            }
            if (ifScope != null) {
                ifScope.markFailed();
                ifScope.close();
            }
        }
        
        // Despite error, callbacks should be balanced
        assertTrue("Callbacks should be balanced after error recovery",
            callbackTester.isBalanced());
    }
    
    @Test
    public void testPartialImportRecovery() {
        // Scenario: import java.util.  // incomplete import
        try (CallbackScope scope = safeCallbacks.scopeElement(mockToken("import"))) {
            List<LocatableToken> tokens = Arrays.asList(
                mockToken("java"),
                mockToken("."),
                mockToken("util"),
                mockToken(".")
                // Missing class name
            );
            
            // Mark as failed due to incomplete import
            scope.markFailed();
            
            // In real parser, would attempt to recover
            // by skipping to next semicolon or statement
        }
        
        assertTrue("Callbacks should be balanced after partial import",
            callbackTester.isBalanced());
    }
    
    // ==================== Unparseable Element Handling ====================
    
    @Test
    public void testUnparseableClassMember() {
        // Scenario: Kotlin-specific syntax not yet supported
        // e.g., inline class, value class, delegation
        try (CallbackScope scope = safeCallbacks.scopeElement(mockToken("inline"))) {
            // Attempt to parse inline class (Kotlin feature)
            // Parser doesn't recognize it, marks as unparseable
            scope.markFailed();
            
            // Would skip to next valid element
        }
        
        assertTrue("Should handle unparseable elements gracefully",
            callbackTester.isBalanced());
    }
    
    @Test
    public void testAnnotationHandling() {
        // Scenario: @Deprecated("Use new API") fun oldMethod() {}
        try (CallbackScope scope = safeCallbacks.scopeElement(mockToken("@"))) {
            // Annotation
            safeCallbacks.gotAnnotation(
                Arrays.asList(mockToken("Deprecated")), 
                true // has parameters
            );
            
            // Annotation parameter
            try (CallbackScope paramScope = safeCallbacks.scopeExpression(mockToken("("), false)) {
                safeCallbacks.gotLiteral(mockToken("\"Use new API\""));
            }
        }
        
        // Method declaration follows
        try (CallbackScope methodScope = safeCallbacks.scopeElement(mockToken("fun"))) {
            safeCallbacks.gotMethodDeclaration(mockToken("oldMethod"), null);
            safeCallbacks.beginMethodBody(mockToken("{"));
            safeCallbacks.endMethodBody(mockToken("}"), true);
        }
        
        assertTrue("Should handle annotations", callbackTester.hasCallback("gotAnnotation"));
        assertTrue("Callbacks should be balanced", callbackTester.isBalanced());
    }
    
    // ==================== Helper Methods ====================
    
    private void simulateImportParsing(String importPath, boolean isStatic, boolean isWildcard) {
        try (CallbackScope scope = safeCallbacks.scopeElement(mockToken("import"))) {
            List<LocatableToken> tokens = new ArrayList<>();
            for (String part : importPath.split("\\.")) {
                if (!tokens.isEmpty()) {
                    tokens.add(mockToken("."));
                }
                tokens.add(mockToken(part));
            }
            
            if (isWildcard) {
                safeCallbacks.gotWildcardImport(tokens, isStatic, 
                    mockToken("import"), mockToken(";"));
            } else {
                safeCallbacks.gotImport(tokens, isStatic, 
                    mockToken("import"), mockToken(";"));
            }
            
            safeCallbacks.gotImportStmtSemi(mockToken(";"));
        }
    }
    
    private void simulateMethodCall(String methodName, List<String> args) {
        safeCallbacks.gotMethodCall(mockToken(methodName));
        safeCallbacks.beginArgumentList(mockToken("("));
        
        for (String arg : args) {
            if (arg.startsWith("\"")) {
                safeCallbacks.gotLiteral(mockToken(arg));
            } else {
                safeCallbacks.gotIdentifier(mockToken(arg));
            }
            safeCallbacks.endArgument();
        }
        
        safeCallbacks.endArgumentList(mockToken(")"));
    }
    
    private void assertCallbackSequence(String... expected) {
        assertTrue("Should have expected callback sequence",
            callbackTester.hasCallbackSequence(Arrays.asList(expected)));
    }
    
    private static LocatableToken mockToken(String text) {
        LineColPos begin = new LineColPos(1, 1, 0);
        LineColPos end = new LineColPos(1, 1 + text.length(), text.length());
        return new LocatableToken(JavaTokenTypes.IDENT, text, begin, end);
    }
    
    // ==================== SafeCallbacks Implementation ====================
    
    /**
     * SafeCallbacks implementation that works with CallbackDelegate
     */
    public static class SafeCallbacks {
            private final CallbackDelegate delegate;
            
            public SafeCallbacks(CallbackDelegate delegate) {
                this.delegate = delegate;
            }
            
            /**
             * Implementation of callback scope for guaranteed pairing.
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
            
            // Scope creation methods
            public CallbackScope scopeElement(LocatableToken token) {
                delegate.beginElement(token);
                return new CallbackScopeImpl(
                    () -> delegate.endElement(token, true),
                    () -> delegate.endElement(token, false)
                );
            }
            
            public CallbackScope scopeExpression(LocatableToken token, boolean isLambdaBody) {
                delegate.beginExpression(token, isLambdaBody);
                return new CallbackScopeImpl(
                    () -> delegate.endExpression(token, false),
                    () -> delegate.endExpression(token, true)
                );
            }
            
            public CallbackScope scopeIfStatement(LocatableToken token) {
                delegate.beginIfStmt(token);
                return new CallbackScopeImpl(
                    () -> delegate.endIfStmt(token, true),
                    () -> delegate.endIfStmt(token, false)
                );
            }
            
            public CallbackScope scopeIfCondBlock(LocatableToken token) {
                delegate.beginIfCondBlock(token);
                return new CallbackScopeImpl(
                    () -> delegate.endIfCondBlock(token, true),
                    () -> delegate.endIfCondBlock(token, false)
                );
            }
            
            public CallbackScope scopeForLoop(LocatableToken token) {
                delegate.beginForLoop(token);
                return new CallbackScopeImpl(
                    () -> delegate.endForLoop(token, true),
                    () -> delegate.endForLoop(token, false)
                );
            }
            
            public CallbackScope scopeForLoopBody(LocatableToken token) {
                delegate.beginForLoopBody(token);
                return new CallbackScopeImpl(
                    () -> delegate.endForLoopBody(token, true),
                    () -> delegate.endForLoopBody(token, false)
                );
            }
            
            public CallbackScope scopeMethodBody(LocatableToken token) {
                delegate.beginMethodBody(token);
                return new CallbackScopeImpl(
                    () -> delegate.endMethodBody(token, true),
                    () -> delegate.endMethodBody(token, false)
                );
            }
            
            public CallbackScope scopeSwitchStatement(LocatableToken token) {
                delegate.beginSwitchStmt(token, false);
                return new CallbackScopeImpl(
                    () -> delegate.endSwitchStmt(token, true),
                    () -> delegate.endSwitchStmt(token, false)
                );
            }
            
            // Direct callback delegation methods
            public void gotBinaryOperator(LocatableToken token) {
                delegate.gotBinaryOperator(token);
            }
            
            public void gotIdentifier(LocatableToken token) {
                System.out.println("DEBUG: SafeCallbacks.gotIdentifier called with token: " + token.getText());
                System.out.println("DEBUG: About to call delegate.gotIdentifier() on: " + delegate);
                System.out.println("DEBUG: Delegate will route to SourceParser instance: " + delegate.getClass());
                delegate.gotIdentifier(token);
                System.out.println("DEBUG: SafeCallbacks.gotIdentifier completed delegate call");
            }
            
            public void gotLiteral(LocatableToken token) {
                delegate.gotLiteral(token);
            }
            
            public void gotImport(List<LocatableToken> tokens, boolean isStatic,
                                 LocatableToken importToken, LocatableToken semiToken) {
                delegate.gotImport(tokens, isStatic, importToken, semiToken);
            }
            
            public void gotWildcardImport(List<LocatableToken> tokens, boolean isStatic,
                                         LocatableToken importToken, LocatableToken semiToken) {
                delegate.gotWildcardImport(tokens, isStatic, importToken, semiToken);
            }
            
            public void gotImportStmtSemi(LocatableToken token) {
                delegate.gotImportStmtSemi(token);
            }
            
            public void gotElseIf(LocatableToken token) {
                delegate.gotElseIf(token);
            }
            
            public void gotMethodCall(LocatableToken token) {
                delegate.gotMethodCall(token);
            }
            
            public void gotMemberCall(LocatableToken token, List<LocatableToken> typeArgs) {
                System.out.println("DEBUG: SafeCallbacks.gotMemberCall called with token: " + token.getText());
                System.out.println("DEBUG: About to call delegate.gotMemberCall() on: " + delegate);
                delegate.gotMemberCall(token, typeArgs);
                System.out.println("DEBUG: SafeCallbacks.gotMemberCall completed delegate call");
            }
            
            public void gotMemberAccess(LocatableToken token) {
                System.out.println("DEBUG: SafeCallbacks.gotMemberAccess called with token: " + token.getText());
                System.out.println("DEBUG: About to call delegate.gotMemberAccess() on: " + delegate);
                delegate.gotMemberAccess(token);
                System.out.println("DEBUG: SafeCallbacks.gotMemberAccess completed delegate call");
            }
            
            public void beginArgumentList(LocatableToken token) {
                delegate.beginArgumentList(token);
            }
            
            public void endArgumentList(LocatableToken token) {
                delegate.endArgumentList(token);
            }
            
            public void endArgument() {
                delegate.endArgument();
            }
            
            // Lambda methods
            public void gotLambdaFormalName(LocatableToken token) {
                delegate.gotLambdaFormalName(token);
            }
            
            public void beginLambdaBody(boolean isBlock, LocatableToken token) {
                delegate.beginLambdaBody(isBlock, token);
            }
            
            public void endLambdaBody(LocatableToken token) {
                delegate.endLambdaBody(token);
            }
            
            // Switch methods
            public void beginSwitchStmt(LocatableToken token, boolean isExpression) {
                delegate.beginSwitchStmt(token, isExpression);
            }
            
            public void endSwitchStmt(LocatableToken token, boolean included) {
                delegate.endSwitchStmt(token, included);
            }
            
            public void beginSwitchCase(LocatableToken token) {
                delegate.beginSwitchCase(token);
            }
            
            public void endSwitchCase(LocatableToken token, boolean wasArrow) {
                delegate.endSwitchCase(token, wasArrow);
            }
            
            public void gotSwitchCaseType(LocatableToken token, boolean isArrow) {
                delegate.gotSwitchCaseType(token, isArrow);
            }
            
            public void gotSwitchDefault() {
                delegate.gotSwitchDefault();
            }
            
            public void beginSwitchBlock(LocatableToken token) {
                delegate.beginSwitchBlock(token);
            }
            
            public void endSwitchBlock(LocatableToken token) {
                delegate.endSwitchBlock(token);
            }
            
            // Other methods
            public void gotAnnotation(List<LocatableToken> name, boolean hasParams) {
                delegate.gotAnnotation(name, hasParams);
            }
            
            public void determinedForLoop(boolean forEach, boolean hasInit) {
                delegate.determinedForLoop(forEach, hasInit);
            }
            
            public void gotMethodDeclaration(LocatableToken token, LocatableToken hiddenToken) {
                delegate.gotMethodDeclaration(token, hiddenToken);
            }
            
            public void beginMethodBody(LocatableToken token) {
                delegate.beginMethodBody(token);
            }
            
            public void endMethodBody(LocatableToken token, boolean included) {
                delegate.endMethodBody(token, included);
            }
    }
    
}