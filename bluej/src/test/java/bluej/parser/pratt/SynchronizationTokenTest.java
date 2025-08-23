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
import bluej.parser.lexer.LineColPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for synchronization token sets and recovery methods.
 *
 * Tests verify that:
 * - Predefined token sets contain appropriate tokens for their recovery scenarios
 * - Synchronization methods use the correct token sets
 * - Token sets provide effective recovery points for different parsing contexts
 * - Comprehensive coverage of Kotlin language constructs for error recovery
 *
 * @author BlueJ Development Team
 */
public class SynchronizationTokenTest {

    private KotlinPrattParser parser;
    private TestTokenOperations tokenOps;

    @BeforeEach
    void setUp() {
        tokenOps = new TestTokenOperations();
        parser = new KotlinPrattParser(tokenOps, null);
    }

    @Test
    @DisplayName("Statement sync tokens contain appropriate statement boundaries")
    void testStatementSyncTokens() {
        Set<Integer> tokens = Arrays.stream(KotlinPrattParser.STATEMENT_SYNC_TOKENS)
            .boxed().collect(Collectors.toSet());

        // Core statement terminators
        assertTrue(tokens.contains(JavaTokenTypes.SEMI), "Should include semicolon");
        assertTrue(tokens.contains(JavaTokenTypes.RCURLY), "Should include closing brace");

        // Control flow keywords
        assertTrue(tokens.contains(JavaTokenTypes.LITERAL_if), "Should include if");
        assertTrue(tokens.contains(JavaTokenTypes.LITERAL_else), "Should include else");
        assertTrue(tokens.contains(JavaTokenTypes.LITERAL_while), "Should include while");
        assertTrue(tokens.contains(JavaTokenTypes.LITERAL_for), "Should include for");
        assertTrue(tokens.contains(JavaTokenTypes.LITERAL_return), "Should include return");
        assertTrue(tokens.contains(JavaTokenTypes.LITERAL_break), "Should include break");
        assertTrue(tokens.contains(JavaTokenTypes.LITERAL_continue), "Should include continue");

        // Exception handling
        assertTrue(tokens.contains(JavaTokenTypes.LITERAL_try), "Should include try");
        assertTrue(tokens.contains(JavaTokenTypes.LITERAL_throw), "Should include throw");

        // Kotlin-specific
        assertTrue(tokens.contains(JavaTokenTypes.LITERAL_when), "Should include when");
    }

    @Test
    @DisplayName("Declaration sync tokens contain appropriate declaration boundaries")
    void testDeclarationSyncTokens() {
        Set<Integer> tokens = Arrays.stream(KotlinPrattParser.DECLARATION_SYNC_TOKENS)
            .boxed().collect(Collectors.toSet());

        // Type declarations
        assertTrue(tokens.contains(JavaTokenTypes.LITERAL_class), "Should include class");
        assertTrue(tokens.contains(JavaTokenTypes.LITERAL_interface), "Should include interface");
        assertTrue(tokens.contains(JavaTokenTypes.LITERAL_object), "Should include object");
        assertTrue(tokens.contains(JavaTokenTypes.LITERAL_enum), "Should include enum");

        // Member declarations
        assertTrue(tokens.contains(JavaTokenTypes.LITERAL_fun), "Should include fun");
        assertTrue(tokens.contains(JavaTokenTypes.LITERAL_val), "Should include val");
        assertTrue(tokens.contains(JavaTokenTypes.LITERAL_var), "Should include var");
        assertTrue(tokens.contains(JavaTokenTypes.LITERAL_constructor), "Should include constructor");
        assertTrue(tokens.contains(JavaTokenTypes.LITERAL_init), "Should include init");

        // Top-level constructs
        assertTrue(tokens.contains(JavaTokenTypes.LITERAL_package), "Should include package");
        assertTrue(tokens.contains(JavaTokenTypes.LITERAL_import), "Should include import");

        // Structural
        assertTrue(tokens.contains(JavaTokenTypes.RCURLY), "Should include closing brace");
    }

    @Test
    @DisplayName("Expression sync tokens contain appropriate expression boundaries")
    void testExpressionSyncTokens() {
        Set<Integer> tokens = Arrays.stream(KotlinPrattParser.EXPRESSION_SYNC_TOKENS)
            .boxed().collect(Collectors.toSet());

        // Expression terminators
        assertTrue(tokens.contains(JavaTokenTypes.SEMI), "Should include semicolon");
        assertTrue(tokens.contains(JavaTokenTypes.COMMA), "Should include comma");
        assertTrue(tokens.contains(JavaTokenTypes.RPAREN), "Should include right paren");
        assertTrue(tokens.contains(JavaTokenTypes.RBRACK), "Should include right bracket");
        assertTrue(tokens.contains(JavaTokenTypes.RCURLY), "Should include right brace");

        // Control flow boundaries
        assertTrue(tokens.contains(JavaTokenTypes.LITERAL_else), "Should include else");
        assertTrue(tokens.contains(JavaTokenTypes.LITERAL_catch), "Should include catch");
        assertTrue(tokens.contains(JavaTokenTypes.LITERAL_finally), "Should include finally");

        // Generic and loop boundaries
        assertTrue(tokens.contains(JavaTokenTypes.GT), "Should include greater than");
        assertTrue(tokens.contains(JavaTokenTypes.LITERAL_in), "Should include in");
    }

    @Test
    @DisplayName("Block sync tokens contain appropriate block boundaries")
    void testBlockSyncTokens() {
        Set<Integer> tokens = Arrays.stream(KotlinPrattParser.BLOCK_SYNC_TOKENS)
            .boxed().collect(Collectors.toSet());

        // Closing delimiters
        assertTrue(tokens.contains(JavaTokenTypes.RCURLY), "Should include right brace");
        assertTrue(tokens.contains(JavaTokenTypes.RPAREN), "Should include right paren");
        assertTrue(tokens.contains(JavaTokenTypes.RBRACK), "Should include right bracket");

        // Block transition keywords
        assertTrue(tokens.contains(JavaTokenTypes.LITERAL_else), "Should include else");
        assertTrue(tokens.contains(JavaTokenTypes.LITERAL_catch), "Should include catch");
        assertTrue(tokens.contains(JavaTokenTypes.LITERAL_finally), "Should include finally");

        // End of file
        assertTrue(tokens.contains(JavaTokenTypes.EOF), "Should include EOF");
    }

    @Test
    @DisplayName("Parameter sync tokens contain appropriate parameter boundaries")
    void testParameterSyncTokens() {
        Set<Integer> tokens = Arrays.stream(KotlinPrattParser.PARAMETER_SYNC_TOKENS)
            .boxed().collect(Collectors.toSet());

        // Parameter separators
        assertTrue(tokens.contains(JavaTokenTypes.COMMA), "Should include comma");
        assertTrue(tokens.contains(JavaTokenTypes.RPAREN), "Should include right paren");
        assertTrue(tokens.contains(JavaTokenTypes.RBRACK), "Should include right bracket");

        // Generic and assignment
        assertTrue(tokens.contains(JavaTokenTypes.GT), "Should include greater than");
        assertTrue(tokens.contains(JavaTokenTypes.ASSIGN), "Should include assignment");

        // Function boundaries
        assertTrue(tokens.contains(JavaTokenTypes.LCURLY), "Should include left brace");
        assertTrue(tokens.contains(JavaTokenTypes.SEMI), "Should include semicolon");
    }

    @Test
    @DisplayName("Type sync tokens contain appropriate type boundaries")
    void testTypeSyncTokens() {
        Set<Integer> tokens = Arrays.stream(KotlinPrattParser.TYPE_SYNC_TOKENS)
            .boxed().collect(Collectors.toSet());

        // Type separators
        assertTrue(tokens.contains(JavaTokenTypes.COMMA), "Should include comma");
        assertTrue(tokens.contains(JavaTokenTypes.RPAREN), "Should include right paren");
        assertTrue(tokens.contains(JavaTokenTypes.RBRACK), "Should include right bracket");
        assertTrue(tokens.contains(JavaTokenTypes.GT), "Should include greater than");

        // Type modifiers
        assertTrue(tokens.contains(JavaTokenTypes.QUESTION), "Should include question mark");

        // Declaration boundaries
        assertTrue(tokens.contains(JavaTokenTypes.ASSIGN), "Should include assignment");
        assertTrue(tokens.contains(JavaTokenTypes.LCURLY), "Should include left brace");
        assertTrue(tokens.contains(JavaTokenTypes.SEMI), "Should include semicolon");
    }

    @Test
    @DisplayName("All sync tokens provide comprehensive coverage")
    void testAllSyncTokensComprehensive() {
        Set<Integer> allTokens = Arrays.stream(KotlinPrattParser.ALL_SYNC_TOKENS)
            .boxed().collect(Collectors.toSet());

        // Should contain key tokens from each category
        assertTrue(allTokens.contains(JavaTokenTypes.SEMI), "Should include semicolon");
        assertTrue(allTokens.contains(JavaTokenTypes.RCURLY), "Should include right brace");
        assertTrue(allTokens.contains(JavaTokenTypes.LITERAL_class), "Should include class");
        assertTrue(allTokens.contains(JavaTokenTypes.LITERAL_fun), "Should include fun");
        assertTrue(allTokens.contains(JavaTokenTypes.LITERAL_if), "Should include if");
        assertTrue(allTokens.contains(JavaTokenTypes.EOF), "Should include EOF");

        // Should be reasonably comprehensive
        assertTrue(allTokens.size() >= 20, "Should contain at least 20 tokens for comprehensive coverage");
    }

    @Test
    @DisplayName("synchronizeOnStatements uses statement sync tokens")
    void testSynchronizeOnStatements() {
        // Set up token stream with a statement sync token
        tokenOps.setTokens(
            createToken(JavaTokenTypes.IDENT, "error", 1, 1),
            createToken(JavaTokenTypes.STAR, "*", 1, 2),
            createToken(JavaTokenTypes.LITERAL_if, "if", 1, 3), // statement sync point
            createToken(JavaTokenTypes.IDENT, "next", 1, 4),
            createToken(JavaTokenTypes.EOF, "", 1, 5)
        );

        parser.initializeWithTokenStream();

        LocatableToken syncToken = parser.synchronizeOnStatements();

        assertNotNull(syncToken);
        assertEquals(JavaTokenTypes.LITERAL_if, syncToken.getType());
        assertEquals("if", syncToken.getText());
    }

    @Test
    @DisplayName("synchronizeOnDeclarations uses declaration sync tokens")
    void testSynchronizeOnDeclarations() {
        // Set up token stream with a declaration sync token
        tokenOps.setTokens(
            createToken(JavaTokenTypes.IDENT, "error", 1, 1),
            createToken(JavaTokenTypes.STAR, "*", 1, 2),
            createToken(JavaTokenTypes.LITERAL_class, "class", 1, 3), // declaration sync point
            createToken(JavaTokenTypes.IDENT, "next", 1, 4),
            createToken(JavaTokenTypes.EOF, "", 1, 5)
        );

        parser.initializeWithTokenStream();

        LocatableToken syncToken = parser.synchronizeOnDeclarations();

        assertNotNull(syncToken);
        assertEquals(JavaTokenTypes.LITERAL_class, syncToken.getType());
        assertEquals("class", syncToken.getText());
    }

    @Test
    @DisplayName("synchronizeOnExpressions uses expression sync tokens")
    void testSynchronizeOnExpressions() {
        // Set up token stream with an expression sync token
        tokenOps.setTokens(
            createToken(JavaTokenTypes.IDENT, "error", 1, 1),
            createToken(JavaTokenTypes.STAR, "*", 1, 2),
            createToken(JavaTokenTypes.COMMA, ",", 1, 3), // expression sync point
            createToken(JavaTokenTypes.IDENT, "next", 1, 4),
            createToken(JavaTokenTypes.EOF, "", 1, 5)
        );

        parser.initializeWithTokenStream();

        LocatableToken syncToken = parser.synchronizeOnExpressions();

        assertNotNull(syncToken);
        assertEquals(JavaTokenTypes.COMMA, syncToken.getType());
        assertEquals(",", syncToken.getText());
    }

    @Test
    @DisplayName("synchronizeOnBlocks uses block sync tokens")
    void testSynchronizeOnBlocks() {
        // Set up token stream with a block sync token
        tokenOps.setTokens(
            createToken(JavaTokenTypes.IDENT, "error", 1, 1),
            createToken(JavaTokenTypes.STAR, "*", 1, 2),
            createToken(JavaTokenTypes.RCURLY, "}", 1, 3), // block sync point
            createToken(JavaTokenTypes.IDENT, "next", 1, 4),
            createToken(JavaTokenTypes.EOF, "", 1, 5)
        );

        parser.initializeWithTokenStream();

        LocatableToken syncToken = parser.synchronizeOnBlocks();

        assertNotNull(syncToken);
        assertEquals(JavaTokenTypes.RCURLY, syncToken.getType());
        assertEquals("}", syncToken.getText());
    }

    @Test
    @DisplayName("synchronizeOnParameters uses parameter sync tokens")
    void testSynchronizeOnParameters() {
        // Set up token stream with a parameter sync token
        tokenOps.setTokens(
            createToken(JavaTokenTypes.IDENT, "error", 1, 1),
            createToken(JavaTokenTypes.STAR, "*", 1, 2),
            createToken(JavaTokenTypes.RPAREN, ")", 1, 3), // parameter sync point
            createToken(JavaTokenTypes.IDENT, "next", 1, 4),
            createToken(JavaTokenTypes.EOF, "", 1, 5)
        );

        parser.initializeWithTokenStream();

        LocatableToken syncToken = parser.synchronizeOnParameters();

        assertNotNull(syncToken);
        assertEquals(JavaTokenTypes.RPAREN, syncToken.getType());
        assertEquals(")", syncToken.getText());
    }

    @Test
    @DisplayName("synchronizeOnTypes uses type sync tokens")
    void testSynchronizeOnTypes() {
        // Set up token stream with a type sync token
        tokenOps.setTokens(
            createToken(JavaTokenTypes.IDENT, "error", 1, 1),
            createToken(JavaTokenTypes.STAR, "*", 1, 2),
            createToken(JavaTokenTypes.GT, ">", 1, 3), // type sync point
            createToken(JavaTokenTypes.IDENT, "next", 1, 4),
            createToken(JavaTokenTypes.EOF, "", 1, 5)
        );

        parser.initializeWithTokenStream();

        LocatableToken syncToken = parser.synchronizeOnTypes();

        assertNotNull(syncToken);
        assertEquals(JavaTokenTypes.GT, syncToken.getType());
        assertEquals(">", syncToken.getText());
    }

    @Test
    @DisplayName("synchronizeComprehensive uses all sync tokens")
    void testSynchronizeComprehensive() {
        // Test with various tokens from different categories
        tokenOps.setTokens(
            createToken(JavaTokenTypes.IDENT, "error", 1, 1),
            createToken(JavaTokenTypes.STAR, "*", 1, 2),
            createToken(JavaTokenTypes.LITERAL_return, "return", 1, 3), // comprehensive sync point
            createToken(JavaTokenTypes.IDENT, "next", 1, 4),
            createToken(JavaTokenTypes.EOF, "", 1, 5)
        );

        parser.initializeWithTokenStream();

        LocatableToken syncToken = parser.synchronizeComprehensive();

        assertNotNull(syncToken);
        assertEquals(JavaTokenTypes.LITERAL_return, syncToken.getType());
        assertEquals("return", syncToken.getText());
    }

    @Test
    @DisplayName("Synchronization methods return null at EOF when no sync tokens found")
    void testSynchronizationAtEOF() {
        tokenOps.setTokens(
            createToken(JavaTokenTypes.IDENT, "error", 1, 1),
            createToken(JavaTokenTypes.STAR, "*", 1, 2),
            createToken(JavaTokenTypes.EOF, "", 1, 3)
        );

        parser.initializeWithTokenStream();

        // Test each synchronization method
        assertNull(parser.synchronizeOnStatements());

        // Reset parser state
        tokenOps.setTokens(
            createToken(JavaTokenTypes.IDENT, "error", 1, 1),
            createToken(JavaTokenTypes.STAR, "*", 1, 2),
            createToken(JavaTokenTypes.EOF, "", 1, 3)
        );
        parser.initializeWithTokenStream();

        assertNull(parser.synchronizeOnExpressions());
    }

    @Test
    @DisplayName("Token sets contain no duplicates")
    void testTokenSetsNoDuplicates() {
        // Test each token set for uniqueness
        assertNoDuplicates("STATEMENT_SYNC_TOKENS", KotlinPrattParser.STATEMENT_SYNC_TOKENS);
        assertNoDuplicates("DECLARATION_SYNC_TOKENS", KotlinPrattParser.DECLARATION_SYNC_TOKENS);
        assertNoDuplicates("EXPRESSION_SYNC_TOKENS", KotlinPrattParser.EXPRESSION_SYNC_TOKENS);
        assertNoDuplicates("BLOCK_SYNC_TOKENS", KotlinPrattParser.BLOCK_SYNC_TOKENS);
        assertNoDuplicates("PARAMETER_SYNC_TOKENS", KotlinPrattParser.PARAMETER_SYNC_TOKENS);
        assertNoDuplicates("TYPE_SYNC_TOKENS", KotlinPrattParser.TYPE_SYNC_TOKENS);
        assertNoDuplicates("ALL_SYNC_TOKENS", KotlinPrattParser.ALL_SYNC_TOKENS);
    }

    @Test
    @DisplayName("Common sync points are effective recovery positions")
    void testCommonSyncPointsEffective() {
        // Test that isCommonSyncPoint method recognizes key recovery tokens
        // Note: isCommonSyncPoint is private, so we test it indirectly through synchronization behavior

        // Test with semicolon (should be recognized as common sync point)
        tokenOps.setTokens(
            createToken(JavaTokenTypes.IDENT, "error", 1, 1),
            createToken(JavaTokenTypes.SEMI, ";", 1, 2),
            createToken(JavaTokenTypes.EOF, "", 1, 3)
        );

        parser.initializeWithTokenStream();

        // Synchronize without explicit token list - should find common sync points
        LocatableToken syncToken = parser.synchronize(); // No explicit tokens, uses common sync points

        assertNotNull(syncToken);
        assertEquals(JavaTokenTypes.SEMI, syncToken.getType());
    }

    // Helper methods

    private void assertNoDuplicates(String setName, int[] tokens) {
        Set<Integer> uniqueTokens = Arrays.stream(tokens).boxed().collect(Collectors.toSet());
        assertEquals(tokens.length, uniqueTokens.size(),
            setName + " should not contain duplicate tokens");
    }

    private LocatableToken createToken(int type, String text, int line, int column) {
        LineColPos pos = new LineColPos(line, column, 0);
        return new LocatableToken(type, text, pos, pos);
    }

    /**
     * Test implementation of TokenOperations for controlled testing.
     */
    private static class TestTokenOperations implements TokenOperations {
        private LocatableToken[] tokens;
        private int position = 0;
        private LocatableToken mostRecent = null;

        public void setTokens(LocatableToken... tokens) {
            this.tokens = tokens;
            this.position = 0;
            this.mostRecent = null;
        }

        @Override
        public LocatableToken nextToken() {
            if (position < tokens.length) {
                mostRecent = tokens[position++];
                return mostRecent;
            }
            mostRecent = createEOFToken();
            return mostRecent;
        }

        @Override
        public LocatableToken LA(int distance) {
            int lookPosition = position + distance - 1;
            if (lookPosition < tokens.length && lookPosition >= 0) {
                return tokens[lookPosition];
            }
            return createEOFToken();
        }

        @Override
        public void pushBack(LocatableToken token) {
            if (position > 0) {
                position--;
            }
        }

        @Override
        public LocatableToken getMostRecent() {
            return mostRecent;
        }

        public LocatableToken peek() {
            return LA(1);
        }

        private LocatableToken createEOFToken() {
            LineColPos pos = new LineColPos(1, 1, 0);
            return new LocatableToken(JavaTokenTypes.EOF, "", pos, pos);
        }
    }
}
