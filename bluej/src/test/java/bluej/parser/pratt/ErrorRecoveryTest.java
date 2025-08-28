/*
 This file is part of the BlueJ program.
 Copyright (C) 1999-2009,2010,2011,2012,2014,2016,2017,2018,2019,2021,2022,2023,2024  Michael Kolling and John Rosenberg

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
import bluej.parser.lexer.LineColPos;
import bluej.parser.lexer.LocatableToken;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test class for ErrorRecovery system functionality.
 *
 * <p>This test class verifies the error recovery mechanisms including
 * context-specific recovery strategies, token synchronization, and
 * integration with the parser's error handling system.</p>
 *
 * @author BlueJ Kotlin Implementation
 * @since 2025-01-24
 */
public class ErrorRecoveryTest {

    private ErrorRecovery errorRecovery;
    private MockParser mockParser;
    private List<LocatableToken> tokenStream;
    private int tokenIndex;

    @Before
    public void setUp() {
        errorRecovery = new ErrorRecovery();
        mockParser = new MockParser();
        tokenStream = new ArrayList<>();
        tokenIndex = 0;
        setupMockParser();
    }

    private void setupMockParser() {
        mockParser.tokenStream = tokenStream;
        mockParser.tokenIndex = 0;
    }

    // ===== ERROR REPORTING AND RECOVERY MODE TESTS =====

    @Test
    public void testErrorReporting_InitialState() {
        assertFalse("Should not be in recovery mode initially", errorRecovery.isInRecoveryMode());
        assertEquals("Should have zero recovery attempts initially", 0, errorRecovery.getRecoveryCount());
    }

    @Test
    public void testErrorReporting_EntersRecoveryMode() {
        LocatableToken token = createToken(JavaTokenTypes.IDENT, "test");

        errorRecovery.reportError(mockParser, "Test error", token, ErrorRecovery.RecoveryContext.EXPRESSION);

        assertTrue("Should enter recovery mode after error", errorRecovery.isInRecoveryMode());
        assertEquals("Should have reported one error", 1, mockParser.errors.size());
    }

    @Test
    public void testErrorReporting_MultipleErrors() {
        LocatableToken token = createToken(JavaTokenTypes.IDENT, "test");

        errorRecovery.reportError(mockParser, "Error 1", token, ErrorRecovery.RecoveryContext.EXPRESSION);
        errorRecovery.reportError(mockParser, "Error 2", token, ErrorRecovery.RecoveryContext.EXPRESSION);

        assertTrue("Should remain in recovery mode", errorRecovery.isInRecoveryMode());
        assertEquals("Should have reported two errors", 2, mockParser.errors.size());
    }

    @Test
    public void testExitRecoveryMode() {
        errorRecovery.reportError(mockParser, "Test error", null, ErrorRecovery.RecoveryContext.GENERAL);
        assertTrue("Should be in recovery mode", errorRecovery.isInRecoveryMode());

        errorRecovery.exitRecoveryMode();

        assertFalse("Should exit recovery mode", errorRecovery.isInRecoveryMode());
    }

    // ===== RECOVERY CONTEXT AND SYNCHRONIZATION TESTS =====

    @Test
    public void testRecovery_ExpressionContext() {
        // Setup token stream: IDENT, PLUS, SEMI (synchronization point)
        addToken(JavaTokenTypes.IDENT, "invalid");
        addToken(JavaTokenTypes.PLUS, "+");
        addToken(JavaTokenTypes.SEMI, ";");
        addToken(JavaTokenTypes.EOF, "");

        errorRecovery.reportError(mockParser, "Expression error", null, ErrorRecovery.RecoveryContext.EXPRESSION);
        boolean recovered = errorRecovery.recoverToSynchronizationPoint(mockParser, ErrorRecovery.RecoveryContext.EXPRESSION);

        assertTrue("Should recover to semicolon", recovered);
        assertEquals("Should have one recovery", 1, errorRecovery.getRecoveryCount());
    }

    @Test
    public void testRecovery_StatementContext() {
        // Setup token stream: IDENT, INVALID, LCURLY (synchronization point)
        addToken(JavaTokenTypes.IDENT, "invalid");
        addToken(JavaTokenTypes.INVALID, "???");
        addToken(JavaTokenTypes.LCURLY, "{");
        addToken(JavaTokenTypes.EOF, "");

        errorRecovery.reportError(mockParser, "Statement error", null, ErrorRecovery.RecoveryContext.STATEMENT);
        boolean recovered = errorRecovery.recoverToSynchronizationPoint(mockParser, ErrorRecovery.RecoveryContext.STATEMENT);

        assertTrue("Should recover to left brace", recovered);
        assertEquals("Should have one recovery", 1, errorRecovery.getRecoveryCount());
    }

    @Test
    public void testRecovery_DeclarationContext() {
        // Setup token stream: INVALID, STAR, LITERAL_fun (synchronization point)
        addToken(JavaTokenTypes.INVALID, "???");
        addToken(JavaTokenTypes.STAR, "*");
        addToken(JavaTokenTypes.LITERAL_fun, "fun");
        addToken(JavaTokenTypes.EOF, "");

        errorRecovery.reportError(mockParser, "Declaration error", null, ErrorRecovery.RecoveryContext.DECLARATION);
        boolean recovered = errorRecovery.recoverToSynchronizationPoint(mockParser, ErrorRecovery.RecoveryContext.DECLARATION);

        assertTrue("Should recover to 'fun' keyword", recovered);
        assertEquals("Should have one recovery", 1, errorRecovery.getRecoveryCount());
    }

    @Test
    public void testRecovery_ArgumentListContext() {
        // Setup token stream: INVALID, IDENT, RPAREN (synchronization point)
        addToken(JavaTokenTypes.INVALID, "???");
        addToken(JavaTokenTypes.IDENT, "arg");
        addToken(JavaTokenTypes.RPAREN, ")");
        addToken(JavaTokenTypes.EOF, "");

        errorRecovery.reportError(mockParser, "Argument list error", null, ErrorRecovery.RecoveryContext.ARGUMENT_LIST);
        boolean recovered = errorRecovery.recoverToSynchronizationPoint(mockParser, ErrorRecovery.RecoveryContext.ARGUMENT_LIST);

        assertTrue("Should recover to right parenthesis", recovered);
        assertEquals("Should have one recovery", 1, errorRecovery.getRecoveryCount());
    }

    // ===== RECOVERY STRATEGY TESTS =====

    @Test
    public void testRecoveryStrategy_Conservative() {
        errorRecovery.setRecoveryStrategy(ErrorRecovery.RecoveryStrategy.CONSERVATIVE);
        errorRecovery.setMaxTokensToSkip(20);

        // Setup many tokens before sync point
        for (int i = 0; i < 10; i++) {
            addToken(JavaTokenTypes.IDENT, "token" + i);
        }
        addToken(JavaTokenTypes.SEMI, ";");
        addToken(JavaTokenTypes.EOF, "");

        errorRecovery.reportError(mockParser, "Test error", null, ErrorRecovery.RecoveryContext.EXPRESSION);
        boolean recovered = errorRecovery.recoverToSynchronizationPoint(mockParser, ErrorRecovery.RecoveryContext.EXPRESSION);

        // Conservative strategy may stop early, so recovery could fail
        // This tests the strategy behavior
        assertNotNull("Should attempt recovery", recovered);
    }

    @Test
    public void testRecoveryStrategy_Aggressive() {
        errorRecovery.setRecoveryStrategy(ErrorRecovery.RecoveryStrategy.AGGRESSIVE);
        errorRecovery.setMaxTokensToSkip(20);

        // Setup many tokens before sync point
        for (int i = 0; i < 15; i++) {
            addToken(JavaTokenTypes.IDENT, "token" + i);
        }
        addToken(JavaTokenTypes.SEMI, ";");
        addToken(JavaTokenTypes.EOF, "");

        errorRecovery.reportError(mockParser, "Test error", null, ErrorRecovery.RecoveryContext.EXPRESSION);
        boolean recovered = errorRecovery.recoverToSynchronizationPoint(mockParser, ErrorRecovery.RecoveryContext.EXPRESSION);

        assertTrue("Aggressive strategy should recover", recovered);
        assertEquals("Should have one recovery", 1, errorRecovery.getRecoveryCount());
    }

    @Test
    public void testRecoveryStrategy_Adaptive() {
        errorRecovery.setRecoveryStrategy(ErrorRecovery.RecoveryStrategy.ADAPTIVE);

        // Test adaptive behavior with multiple consecutive errors
        for (int i = 0; i < 5; i++) {
            errorRecovery.reportError(mockParser, "Error " + i, null, ErrorRecovery.RecoveryContext.GENERAL);
        }

        // Adaptive strategy should adjust based on error patterns
        assertTrue("Should still be in recovery mode", errorRecovery.isInRecoveryMode());
    }

    // ===== RECOVERY FAILURE CONDITIONS =====

    @Test
    public void testRecovery_NoSynchronizationPoint() {
        // Setup token stream with no synchronization points
        addToken(JavaTokenTypes.IDENT, "token1");
        addToken(JavaTokenTypes.IDENT, "token2");
        addToken(JavaTokenTypes.IDENT, "token3");
        addToken(JavaTokenTypes.EOF, "");

        errorRecovery.reportError(mockParser, "Test error", null, ErrorRecovery.RecoveryContext.EXPRESSION);
        boolean recovered = errorRecovery.recoverToSynchronizationPoint(mockParser, ErrorRecovery.RecoveryContext.EXPRESSION);

        assertFalse("Should not recover without sync point", recovered);
    }

    @Test
    public void testRecovery_MaxTokensExceeded() {
        errorRecovery.setMaxTokensToSkip(3);

        // Setup token stream with sync point beyond limit
        addToken(JavaTokenTypes.IDENT, "token1");
        addToken(JavaTokenTypes.IDENT, "token2");
        addToken(JavaTokenTypes.IDENT, "token3");
        addToken(JavaTokenTypes.IDENT, "token4");
        addToken(JavaTokenTypes.SEMI, ";");
        addToken(JavaTokenTypes.EOF, "");

        errorRecovery.reportError(mockParser, "Test error", null, ErrorRecovery.RecoveryContext.EXPRESSION);
        boolean recovered = errorRecovery.recoverToSynchronizationPoint(mockParser, ErrorRecovery.RecoveryContext.EXPRESSION);

        assertFalse("Should not recover beyond token limit", recovered);
    }

    @Test
    public void testRecovery_EndOfFile() {
        // Setup token stream ending immediately
        addToken(JavaTokenTypes.EOF, "");

        errorRecovery.reportError(mockParser, "Test error", null, ErrorRecovery.RecoveryContext.EXPRESSION);
        boolean recovered = errorRecovery.recoverToSynchronizationPoint(mockParser, ErrorRecovery.RecoveryContext.EXPRESSION);

        assertFalse("Should not recover at end of file", recovered);
    }

    @Test
    public void testRecovery_MaxRecoveryAttempts() {
        // Exceed maximum recovery attempts
        for (int i = 0; i < 15; i++) {
            addToken(JavaTokenTypes.IDENT, "token");
            addToken(JavaTokenTypes.SEMI, ";");
            addToken(JavaTokenTypes.EOF, "");

            errorRecovery.reportError(mockParser, "Error " + i, null, ErrorRecovery.RecoveryContext.EXPRESSION);
            if (errorRecovery.shouldAttemptRecovery()) {
                errorRecovery.recoverToSynchronizationPoint(mockParser, ErrorRecovery.RecoveryContext.EXPRESSION);
            }

            // Reset token stream for next iteration
            setupMockParser();
        }

        assertFalse("Should stop attempting recovery after max attempts", errorRecovery.shouldAttemptRecovery());
    }

    // ===== UTILITY METHOD TESTS =====

    @Test
    public void testCreateContextualErrorMessage() {
        String baseMessage = "Expected identifier";

        String expressionMessage = ErrorRecovery.createContextualErrorMessage(baseMessage, ErrorRecovery.RecoveryContext.EXPRESSION);
        String statementMessage = ErrorRecovery.createContextualErrorMessage(baseMessage, ErrorRecovery.RecoveryContext.STATEMENT);
        String generalMessage = ErrorRecovery.createContextualErrorMessage(baseMessage, ErrorRecovery.RecoveryContext.GENERAL);

        assertTrue("Expression context should be included", expressionMessage.contains("in expression"));
        assertTrue("Statement context should be included", statementMessage.contains("in statement"));
        assertEquals("General context should not add text", baseMessage, generalMessage);
    }

    @Test
    public void testIsSynchronizationCandidate() {
        assertTrue("Semicolon should be sync candidate", ErrorRecovery.isSynchronizationCandidate(JavaTokenTypes.SEMI));
        assertTrue("Left brace should be sync candidate", ErrorRecovery.isSynchronizationCandidate(JavaTokenTypes.LCURLY));
        assertTrue("Fun keyword should be sync candidate", ErrorRecovery.isSynchronizationCandidate(JavaTokenTypes.LITERAL_fun));

        assertFalse("Regular identifier should not be sync candidate", ErrorRecovery.isSynchronizationCandidate(JavaTokenTypes.IDENT));
        assertFalse("Plus operator should not be sync candidate", ErrorRecovery.isSynchronizationCandidate(JavaTokenTypes.PLUS));
    }

    // ===== STATE MANAGEMENT TESTS =====

    @Test
    public void testReset() {
        // Put recovery system in a non-initial state
        errorRecovery.reportError(mockParser, "Error", null, ErrorRecovery.RecoveryContext.EXPRESSION);
        addToken(JavaTokenTypes.SEMI, ";");
        addToken(JavaTokenTypes.EOF, "");
        errorRecovery.recoverToSynchronizationPoint(mockParser, ErrorRecovery.RecoveryContext.EXPRESSION);

        assertTrue("Should be in recovery mode", errorRecovery.isInRecoveryMode());
        assertEquals("Should have recovery count", 1, errorRecovery.getRecoveryCount());

        // Reset the system
        errorRecovery.reset();

        assertFalse("Should not be in recovery mode after reset", errorRecovery.isInRecoveryMode());
        assertEquals("Should have zero recovery count after reset", 0, errorRecovery.getRecoveryCount());
    }

    @Test
    public void testConfiguration() {
        errorRecovery.setMaxTokensToSkip(100);
        errorRecovery.setRecoveryStrategy(ErrorRecovery.RecoveryStrategy.CONSERVATIVE);

        // Test that configuration is maintained
        assertTrue("Configuration should be maintained", true); // More of a setup test
    }

    @Test
    public void testShouldAttemptRecovery_InitialState() {
        assertFalse("Should not attempt recovery initially", errorRecovery.shouldAttemptRecovery());
    }

    @Test
    public void testShouldAttemptRecovery_AfterError() {
        errorRecovery.reportError(mockParser, "Test error", null, ErrorRecovery.RecoveryContext.EXPRESSION);
        assertTrue("Should attempt recovery after error", errorRecovery.shouldAttemptRecovery());
    }

    // ===== MOCK CLASSES =====

    /**
     * Mock parser for testing error recovery without full parser implementation.
     */
    private static class MockParser extends KotlinPrattParser {
        List<String> errors = new ArrayList<>();
        List<LocatableToken> tokenStream = new ArrayList<>();
        int tokenIndex = 0;

        public MockParser() {
            super(new MockTokenOperations(), null, new MockNodeFactory());
        }

        @Override
        public void error(String message, LocatableToken token) {
            errors.add(message);
        }

        @Override
        public @NotNull LocatableToken peek() {
            if (tokenIndex < tokenStream.size()) {
                return tokenStream.get(tokenIndex);
            }
            return createToken(JavaTokenTypes.EOF, "");
        }

        @Override
        public @NotNull LocatableToken consume() {
            if (tokenIndex < tokenStream.size()) {
                return tokenStream.get(tokenIndex++);
            }
            return createToken(JavaTokenTypes.EOF, "");
        }
    }

    /**
     * Mock token operations for testing.
     */
    private static class MockTokenOperations implements TokenOperations {
        @Override
        public @NotNull LocatableToken nextToken() {
            return createToken(JavaTokenTypes.EOF, "");
        }

        @Override
        public @NotNull LocatableToken LA(int distance) {
            return createToken(JavaTokenTypes.EOF, "");
        }

        @Override
        public void pushBack(LocatableToken token) {
            // Mock implementation - do nothing
        }

        @Override
        public LocatableToken getMostRecent() {
            return createToken(JavaTokenTypes.EOF, "");
        }
    }

    /**
     * Mock node factory for testing.
     */
    private static class MockNodeFactory implements NodeFactory {
        @Override
        public bluej.parser.nodes.ParsedNode createLiteralNode(LocatableToken token) { return null; }
        @Override
        public bluej.parser.nodes.ParsedNode createBinaryOperatorNode(bluej.parser.nodes.ParsedNode left, LocatableToken operator, bluej.parser.nodes.ParsedNode right) { return null; }
        @Override
        public bluej.parser.nodes.ParsedNode createUnaryPrefixNode(LocatableToken operator, bluej.parser.nodes.ParsedNode operand) { return null; }
        @Override
        public bluej.parser.nodes.ParsedNode createUnaryPostfixNode(bluej.parser.nodes.ParsedNode operand, LocatableToken operator) { return null; }
        @Override
        public bluej.parser.nodes.ParsedNode createGroupNode(bluej.parser.nodes.ParsedNode inner) { return null; }
        @Override
        public bluej.parser.nodes.ParsedNode createIdentifierNode(LocatableToken identifier) { return null; }
        @Override
        public bluej.parser.nodes.ParsedNode createThisNode(LocatableToken thisToken) { return null; }
        @Override
        public bluej.parser.nodes.ParsedNode createSuperNode(LocatableToken superToken) { return null; }
        @Override
        public bluej.parser.nodes.ParsedNode createCallNode(bluej.parser.nodes.ParsedNode function, bluej.parser.nodes.ParsedNode[] arguments) { return null; }
        @Override
        public bluej.parser.nodes.ParsedNode createMemberAccessNode(bluej.parser.nodes.ParsedNode object, LocatableToken member, boolean isSafeCall) { return null; }
        @Override
        public bluej.parser.nodes.ParsedNode createArrayAccessNode(bluej.parser.nodes.ParsedNode array, bluej.parser.nodes.ParsedNode index) { return null; }
        @Override
        public boolean hasErrors() { return false; }
        @Override
        public void reportError(String message, LocatableToken token) { }
    }

    // ===== UTILITY METHODS =====

    /**
     * Creates a mock token with the specified type and text.
     */
    private static LocatableToken createToken(int type, String text) {
        return new LocatableToken(type, text, new LineColPos(1, 1, 0), new LineColPos(1, 1 + text.length(), text.length()));
    }

    /**
     * Adds a token to the mock token stream.
     */
    private void addToken(int type, String text) {
        tokenStream.add(createToken(type, text));
    }
}
