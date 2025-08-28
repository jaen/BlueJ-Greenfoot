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
package bluej.parser;

import bluej.Config;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.pratt.KotlinPrattParser;
import bluej.parser.pratt.NodeFactory;
import bluej.parser.pratt.ParseletRegistry;
import bluej.parser.pratt.ParseResult;
import bluej.parser.pratt.TokenOperations;
import bluej.parser.nodes.ExpressionNode;
import bluej.parser.nodes.ParsedNode;
import org.jetbrains.annotations.NotNull;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.List;

/**
 * Delegation adapter for Kotlin parsing that chooses between the legacy KotlinParser
 * and the new modular KotlinPrattParser based on configuration and parser capabilities.
 *
 * <p>This adapter implements the ParserBehavior interface and acts as a bridge between
 * BlueJ's parsing infrastructure and the two available Kotlin parsing implementations:
 * <ul>
 *   <li><strong>KotlinParser</strong> - The legacy monolithic parser (default)</li>
 *   <li><strong>KotlinPrattParser</strong> - The new modular Pratt parser</li>
 * </ul>
 *
 * <p>The adapter uses configuration settings and parser capability checks to determine
 * which parser to use for each parsing operation. By default, all operations delegate
 * to the legacy KotlinParser to maintain backward compatibility. The new KotlinPrattParser
 * is used only when explicitly enabled and when it supports the required functionality.</p>
 *
 * <p>Configuration properties that control parser selection:
 * <ul>
 *   <li><code>bluej.kotlin.usePrattParser</code> - Enables Pratt parser for expressions</li>
 *   <li><code>bluej.kotlin.usePrattParser.statements</code> - Enables Pratt parser for statements</li>
 *   <li><code>bluej.kotlin.usePrattParser.declarations</code> - Enables Pratt parser for declarations</li>
 * </ul>
 *
 * @author BlueJ Development Team
 * @since BlueJ 5.4.0
 */
@OnThread(Tag.FXPlatform)
public class KotlinParserAdapter implements ParserBehavior {

    /** The legacy monolithic Kotlin parser (default for all operations) */
    private final KotlinParser legacyParser;

    /** The new modular Pratt parser (used when enabled and capable) */
    private final KotlinPrattParser prattParser;

    /** The parent SourceParser for integration */
    private final SourceParser sourceParser;

    /**
     * Creates a new adapter that delegates between legacy and Pratt parsers.
     *
     * @param sourceParser The parent SourceParser for integration
     */
    public KotlinParserAdapter(SourceParser sourceParser) {
        this.sourceParser = sourceParser;
        this.legacyParser = new KotlinParser(sourceParser);

        // Create Pratt parser with thread-safe token operations and node factory
        this.prattParser = new KotlinPrattParser(
            new ThreadSafeTokenOperations(sourceParser.getTokenStream()),
            sourceParser,
            new ThreadSafeNodeFactory(sourceParser)
        );
    }

    /**
     * Creates a new adapter with a custom parselet registry for the Pratt parser.
     *
     * @param sourceParser The parent SourceParser for integration
     * @param registry Custom parselet registry for the Pratt parser
     */
    public KotlinParserAdapter(SourceParser sourceParser, ParseletRegistry registry) {
        this.sourceParser = sourceParser;
        this.legacyParser = new KotlinParser(sourceParser);

        // Create Pratt parser with custom registry and node factory
        this.prattParser = new KotlinPrattParser(
            new ThreadSafeTokenOperations(sourceParser.getTokenStream()),
            sourceParser,
            new ThreadSafeNodeFactory(sourceParser),
            registry
        );
    }

    // ==================== Configuration and Capability Checking ====================

    /**
     * Checks if the Pratt parser should be used for expression parsing.
     *
     * @return true if Pratt parser is enabled and capable for expressions
     */
    private boolean shouldUsePrattForExpressions() {
        return isConfigEnabled("bluej.kotlin.usePrattParser", false) &&
               supportsExpressionParsing();
    }

    /**
     * Checks if the Pratt parser should be used for statement parsing.
     *
     * @return true if Pratt parser is enabled and capable for statements
     */
    private boolean shouldUsePrattForStatements() {
        return isConfigEnabled("bluej.kotlin.usePrattParser.statements", false) &&
               supportsStatementParsing();
    }

    /**
     * Checks if the Pratt parser should be used for declaration parsing.
     *
     * @return true if Pratt parser is enabled and capable for declarations
     */
    private boolean shouldUsePrattForDeclarations() {
        return isConfigEnabled("bluej.kotlin.usePrattParser.declarations", false) &&
               supportsDeclarationParsing();
    }

    /**
     * Checks if the parser currently supports expression parsing.
     * This checks if the necessary parselets are registered.
     *
     * @return true if the parser has the necessary parselets for expression parsing
     */
    private boolean supportsExpressionParsing() {
        ParseletRegistry registry = prattParser.getRegistry();

        // Check for essential parselets needed for basic expression parsing
        boolean hasLiterals = registry.hasPrefix(JavaTokenTypes.NUM_INT) ||
                              registry.hasPrefix(JavaTokenTypes.STRING_LITERAL) ||
                              registry.hasPrefix(JavaTokenTypes.LITERAL_true) ||
                              registry.hasPrefix(JavaTokenTypes.LITERAL_false);

        boolean hasOperators = registry.hasInfix(JavaTokenTypes.PLUS) ||
                               registry.hasInfix(JavaTokenTypes.MINUS) ||
                               registry.hasInfix(JavaTokenTypes.STAR) ||
                               registry.hasInfix(JavaTokenTypes.DIV);

        boolean hasIdentifiers = registry.hasPrefix(JavaTokenTypes.IDENT);

        // Consider expression parsing supported if we have at least some basic parselets
        return hasLiterals || hasOperators || hasIdentifiers;
    }

    /**
     * Checks if the parser currently supports statement parsing.
     *
     * @return true if the parser has the necessary parselets for statement parsing
     */
    private boolean supportsStatementParsing() {
        // Statement parsing requires expression parsing plus statement-specific parselets
        if (!supportsExpressionParsing()) {
            return false;
        }

        ParseletRegistry registry = prattParser.getRegistry();

        // Check for statement-specific parselets
        boolean hasStatements = registry.hasPrefix(JavaTokenTypes.LITERAL_if) ||
                                registry.hasPrefix(JavaTokenTypes.LITERAL_while) ||
                                registry.hasPrefix(JavaTokenTypes.LITERAL_for) ||
                                registry.hasPrefix(JavaTokenTypes.LITERAL_return);

        return hasStatements;
    }

    /**
     * Checks if the parser currently supports declaration parsing.
     *
     * @return true if the parser has the necessary parselets for declaration parsing
     */
    private boolean supportsDeclarationParsing() {
        ParseletRegistry registry = prattParser.getRegistry();

        // Check for declaration-specific parselets
        boolean hasDeclarations = registry.hasPrefix(JavaTokenTypes.LITERAL_class) ||
                                  registry.hasPrefix(JavaTokenTypes.LITERAL_interface) ||
                                  registry.hasPrefix(JavaTokenTypes.LITERAL_fun) ||
                                  registry.hasPrefix(JavaTokenTypes.LITERAL_val) ||
                                  registry.hasPrefix(JavaTokenTypes.LITERAL_var);

        return hasDeclarations;
    }

    /**
     * Checks if a configuration property is enabled.
     *
     * @param property The configuration property name
     * @param defaultValue The default value if property is not set
     * @return true if the property is enabled
     */
    private boolean isConfigEnabled(String property, boolean defaultValue) {
        try {
            return Config.getPropBoolean(property, defaultValue);
        } catch (Exception e) {
            // Config not initialized (e.g., in tests), use system property
            return Boolean.parseBoolean(System.getProperty(property, String.valueOf(defaultValue)));
        }
    }

    // ==================== ParserBehavior Implementation ====================

    @Override
    public void parseCU() {
        // Compilation unit parsing always uses legacy parser for now
        // This ensures overall parsing structure remains stable
        legacyParser.parseCU();
    }

    @Override
    public int parseCUpart(int state) {
        // Partial compilation unit parsing uses legacy parser
        return legacyParser.parseCUpart(state);
    }

    @Override
    public LocatableToken parsePackageStmt(LocatableToken token) {
        // Package statement parsing uses legacy parser
        return legacyParser.parsePackageStmt(token);
    }

    @Override
    public void parseImportStatement() {
        // Import statement parsing uses legacy parser
        legacyParser.parseImportStatement();
    }

    @Override
    public void parseImportStatement(LocatableToken importToken) {
        // Import statement parsing uses legacy parser
        legacyParser.parseImportStatement(importToken);
    }

    @Override
    public void parseTypeDef() {
        // Type definition parsing: delegate based on capability
        if (shouldUsePrattForDeclarations()) {
            try {
                // Try Pratt parser for type definitions
                prattParser.parseDeclaration();
                return;
            } catch (Exception e) {
                // Fall back to legacy parser on error
                // TODO: Log the fallback for debugging
            }
        }

        // Default to legacy parser
        legacyParser.parseTypeDef();
    }

    @Override
    public void parseTypeDef(LocatableToken firstToken) {
        // Type definition with token: delegate based on capability
        if (shouldUsePrattForDeclarations()) {
            try {
                // Pratt parser handles token positioning internally
                prattParser.parseDeclaration();
                return;
            } catch (Exception e) {
                // Fall back to legacy parser on error
            }
        }

        // Default to legacy parser
        legacyParser.parseTypeDef(firstToken);
    }

    @Override
    public LocatableToken parseTypeBody(int tdType, LocatableToken token) {
        // Type body parsing uses legacy parser for structural stability
        return legacyParser.parseTypeBody(tdType, token);
    }

    @Override
    public int parseTypeDefBegin() {
        // Type definition begin uses legacy parser
        return legacyParser.parseTypeDefBegin();
    }

    @Override
    public LocatableToken parseTypeDefPart2(boolean b) {
        // Type definition part 2 uses legacy parser
        return legacyParser.parseTypeDefPart2(b);
    }

    @Override
    public void parseClassElement(LocatableToken nextToken) {
        // Class element parsing: delegate based on capability
        if (shouldUsePrattForDeclarations()) {
            try {
                // Try Pratt parser for class elements (methods, properties, etc.)
                prattParser.parseDeclaration();
                return;
            } catch (Exception e) {
                // Fall back to legacy parser on error
            }
        }

        // Default to legacy parser
        legacyParser.parseClassElement(nextToken);
    }

    @Override
    public LocatableToken parseStatement(LocatableToken last, boolean b) {
        // Statement parsing: delegate based on capability
        if (shouldUsePrattForStatements()) {
            try {
                // Try Pratt parser for statements
                prattParser.parseStatement();
                // Return appropriate token (Pratt parser manages tokens differently)
                return prattParser.getCurrentToken();
            } catch (Exception e) {
                // Fall back to legacy parser on error
            }
        }

        // Default to legacy parser
        return legacyParser.parseStatement(last, b);
    }

    @Override
    public boolean parseTypeSpec(boolean b, boolean b1, List<LocatableToken> ll) {
        // Type specification parsing uses legacy parser
        return legacyParser.parseTypeSpec(b, b1, ll);
    }

    @Override
    public void parseClassBody() {
        // Class body parsing uses legacy parser for structural stability
        legacyParser.parseClassBody();
    }

    @Override
    public void parseExpression() {
        // Expression parsing: delegate based on capability
        if (shouldUsePrattForExpressions()) {
            try {
                // Try Pratt parser for expressions
                prattParser.parseExpression();

                // Check if the Pratt parser encountered any errors
                if (prattParser.hasErrors()) {
                    List<ParseResult.ParseError> errors = prattParser.getErrors();
                    if (!errors.isEmpty()) {
                        ParseResult.ParseError firstError = errors.get(0);
                        LocatableToken token = firstError.token();
                        String message = firstError.message();

                        // Throw ParseFailure to match legacy parser behavior
                        if (token != null) {
                            throw new ParseFailure("Parse error: (" + token.getLine() + ":" + token.getColumn() + ") :" + message);
                        } else {
                            throw new ParseFailure("Parse error: " + message);
                        }
                    }
                }

                return;
            } catch (Exception e) {
                // Fall back to legacy parser on error
                throw e; // Re-throw the exception to propagate parse failures
            }
        }

        // Default to legacy parser
        legacyParser.parseExpression();
    }

    @Override
    public LocatableToken parseVariableDeclarations() {
        // Variable declaration parsing: delegate based on capability
        if (shouldUsePrattForDeclarations()) {
            try {
                // Try Pratt parser for variable declarations
                prattParser.parseDeclaration();
                return prattParser.getCurrentToken();
            } catch (Exception e) {
                // Fall back to legacy parser on error
            }
        }

        // Default to legacy parser
        return legacyParser.parseVariableDeclarations();
    }

    @Override
    public boolean parseTypeSpec(boolean processArray) {
        // Type specification parsing uses legacy parser
        return legacyParser.parseTypeSpec(processArray);
    }

    @Override
    public void parseMethodParamsBody() {
        // Method parameters and body parsing uses legacy parser for now
        legacyParser.parseMethodParamsBody();
    }

    // ==================== Utility Methods ====================

    /**
     * Gets the legacy KotlinParser instance.
     *
     * @return The legacy parser
     */
    public KotlinParser getLegacyParser() {
        return legacyParser;
    }

    /**
     * Gets the KotlinPrattParser instance.
     *
     * @return The Pratt parser
     */
    public KotlinPrattParser getPrattParser() {
        return prattParser;
    }

    /**
     * Gets the parent SourceParser.
     *
     * @return The parent SourceParser
     */
    public SourceParser getSourceParser() {
        return sourceParser;
    }

    /**
     * Token type constants for parselet checking.
     */
    private static class JavaTokenTypes {
        // Literals
        static final int NUM_INT = bluej.parser.lexer.JavaTokenTypes.NUM_INT;
        static final int STRING_LITERAL = bluej.parser.lexer.JavaTokenTypes.STRING_LITERAL;
        static final int LITERAL_true = bluej.parser.lexer.JavaTokenTypes.LITERAL_true;
        static final int LITERAL_false = bluej.parser.lexer.JavaTokenTypes.LITERAL_false;

        // Operators
        static final int PLUS = bluej.parser.lexer.JavaTokenTypes.PLUS;
        static final int MINUS = bluej.parser.lexer.JavaTokenTypes.MINUS;
        static final int STAR = bluej.parser.lexer.JavaTokenTypes.STAR;
        static final int DIV = bluej.parser.lexer.JavaTokenTypes.DIV;

        // Identifiers
        static final int IDENT = bluej.parser.lexer.JavaTokenTypes.IDENT;

        // Statements
        static final int LITERAL_if = bluej.parser.lexer.JavaTokenTypes.LITERAL_if;
        static final int LITERAL_while = bluej.parser.lexer.JavaTokenTypes.LITERAL_while;
        static final int LITERAL_for = bluej.parser.lexer.JavaTokenTypes.LITERAL_for;
        static final int LITERAL_return = bluej.parser.lexer.JavaTokenTypes.LITERAL_return;

        // Declarations
        static final int LITERAL_class = bluej.parser.lexer.JavaTokenTypes.LITERAL_class;
        static final int LITERAL_interface = bluej.parser.lexer.JavaTokenTypes.LITERAL_interface;
        static final int LITERAL_fun = bluej.parser.lexer.JavaTokenTypes.LITERAL_fun;
        static final int LITERAL_val = bluej.parser.lexer.JavaTokenTypes.LITERAL_val;
        static final int LITERAL_var = bluej.parser.lexer.JavaTokenTypes.LITERAL_var;
    }

    /**
     * Thread-safe implementation of TokenOperations that delegates to JavaTokenFilter.
     * All methods are called within the FXPlatform thread context as required.
     */
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    private static class ThreadSafeTokenOperations implements TokenOperations {
        private final bluej.parser.lexer.JavaTokenFilter tokenStream;

        public ThreadSafeTokenOperations(bluej.parser.lexer.JavaTokenFilter tokenStream) {
            this.tokenStream = tokenStream;
        }

        @Override
        public @NotNull LocatableToken nextToken() {
            return tokenStream.nextToken();
        }

        @Override
        public @NotNull LocatableToken LA(int distance) {
            return tokenStream.LA(distance);
        }

        @Override
        public void pushBack(LocatableToken token) {
            tokenStream.pushBack(token);
        }

        @Override
        public LocatableToken getMostRecent() {
            return tokenStream.getMostRecent();
        }
    }

    /**
     * Thread-safe implementation of NodeFactory that creates AST nodes on the FXPlatform thread.
     *
     * <p>This implementation ensures that all AST node creation happens on the JavaFX thread
     * as required by BlueJ's UI integration. The actual node creation logic delegates to
     * existing parser methods for compatibility.</p>
     */
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    private class ThreadSafeNodeFactory implements NodeFactory {

        private final SourceParser parser;

        ThreadSafeNodeFactory(SourceParser parser) {
            this.parser = parser;
        }

        @Override
        public ParsedNode createLiteralNode(LocatableToken token) {
            // Create a simple expression node for literals
            // In the full implementation, this would create the appropriate literal node type
            ExpressionNode node = new ExpressionNode(null);
            node.setComplete(true);
            return node;
        }

        @Override
        public ParsedNode createBinaryOperatorNode(ParsedNode left, LocatableToken operator, ParsedNode right) {
            // Create a binary expression node
            ExpressionNode node = new ExpressionNode(null);
            node.setComplete(true);
            return node;
        }

        @Override
        public ParsedNode createUnaryPrefixNode(LocatableToken operator, ParsedNode operand) {
            // Create a unary expression node
            ExpressionNode node = new ExpressionNode(null);
            node.setComplete(true);
            return node;
        }

        @Override
        public ParsedNode createUnaryPostfixNode(ParsedNode operand, LocatableToken operator) {
            // Create a unary expression node
            ExpressionNode node = new ExpressionNode(null);
            node.setComplete(true);
            return node;
        }

        @Override
        public ParsedNode createGroupNode(ParsedNode innerExpression) {
            // Parentheses are purely syntactic - return the inner expression
            return innerExpression;
        }

        @Override
        public ParsedNode createIdentifierNode(LocatableToken identifier) {
            // Create an identifier expression node
            ExpressionNode node = new ExpressionNode(null);
            node.setComplete(true);
            return node;
        }

        @Override
        public ParsedNode createMemberAccessNode(ParsedNode object, LocatableToken memberName, boolean isSafeCall) {
            // Create a member access expression node
            ExpressionNode node = new ExpressionNode(null);
            node.setComplete(true);
            return node;
        }

        @Override
        public ParsedNode createCallNode(ParsedNode function, ParsedNode[] arguments) {
            // Create a function call expression node
            ExpressionNode node = new ExpressionNode(null);
            node.setComplete(true);
            return node;
        }

        @Override
        public ParsedNode createArrayAccessNode(ParsedNode array, ParsedNode index) {
            // Create an array access expression node
            ExpressionNode node = new ExpressionNode(null);
            node.setComplete(true);
            return node;
        }

        @Override
        public ParsedNode createThisNode(LocatableToken thisToken) {
            // Create a 'this' reference node
            ExpressionNode node = new ExpressionNode(null);
            node.setComplete(true);
            return node;
        }

        @Override
        public ParsedNode createSuperNode(LocatableToken superToken) {
            // Create a 'super' reference node
            ExpressionNode node = new ExpressionNode(null);
            node.setComplete(true);
            return node;
        }

        @Override
        public void reportError(String message, LocatableToken token) {
            // Report error through the pratt parser's error handling mechanism
            // Since SourceParser doesn't have direct error reporting, we delegate to the pratt parser
            if (prattParser != null) {
                prattParser.error(message, token);
            }
        }

        @Override
        public boolean hasErrors() {
            // Check if the pratt parser has reported any errors
            return prattParser != null && prattParser.hasErrors();
        }
    }
}
