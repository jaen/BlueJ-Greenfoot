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

import bluej.JavaFXThreadingRule;
import bluej.NonParallelisableTests;
import bluej.parser.SourceParser;
import bluej.parser.nodes.ParsedNode;
import junit.framework.TestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.StringReader;

/**
 * Simple AST comparison test that logs results without failing.
 *
 * This test helps us understand what's happening with the parser integration
 * by logging detailed results instead of asserting on them.
 */
@Category(NonParallelisableTests.class)
public class SimpleASTComparisonTest extends TestCase {

    @Rule
    public JavaFXThreadingRule javafxRule = new JavaFXThreadingRule();

    @Test
    public void testBasicLiterals() {
        System.out.println("\n=== Basic Literals Test ===");

        String[] testCases = {
            "42",
            "\"hello\"",
            "true",
            "null",
            "myVar"
        };

        for (String source : testCases) {
            System.out.println("\nTesting: " + source);

            // Test monolithic parser
            try {
                ParseResult monolithicResult = parseWithMonolithicParser(source);
                System.out.println("  Monolithic: " +
                    (monolithicResult.success ? "SUCCESS" : "FAILED") +
                    " - " + monolithicResult.message);
                if (monolithicResult.ast != null) {
                    System.out.println("    AST: " + ASTComparisonUtils.astToString(monolithicResult.ast));
                }
            } catch (Exception e) {
                System.out.println("  Monolithic: EXCEPTION - " + e.getMessage());
            }

            // Test Pratt parser
            try {
                ParseResult prattResult = parseWithPrattParser(source);
                System.out.println("  Pratt: " +
                    (prattResult.success ? "SUCCESS" : "FAILED") +
                    " - " + prattResult.message);
                if (prattResult.ast != null) {
                    System.out.println("    AST: " + ASTComparisonUtils.astToString(prattResult.ast));
                }
            } catch (Exception e) {
                System.out.println("  Pratt: EXCEPTION - " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testBinaryOperators() {
        System.out.println("\n=== Binary Operators Test ===");

        String[] testCases = {
            "a + b",
            "x * y",
            "foo == bar"
        };

        for (String source : testCases) {
            System.out.println("\nTesting: " + source);

            try {
                ParseResult monolithicResult = parseWithMonolithicParser(source);
                ParseResult prattResult = parseWithPrattParser(source);

                System.out.println("  Monolithic: " + (monolithicResult.success ? "SUCCESS" : "FAILED"));
                System.out.println("  Pratt: " + (prattResult.success ? "SUCCESS" : "FAILED"));

                if (monolithicResult.success && prattResult.success) {
                    ASTComparisonUtils.ComparisonResult comparison =
                        ASTComparisonUtils.compareAST(monolithicResult.ast, prattResult.ast,
                            ASTComparisonUtils.ComparisonMode.STRUCTURAL);

                    System.out.println("  AST Match: " + comparison.matches());
                    if (!comparison.matches()) {
                        System.out.println("  Differences: " + comparison.getDifferences());
                    }
                }
            } catch (Exception e) {
                System.out.println("  ERROR: " + e.getMessage());
            }
        }
    }

    @Test
    public void testParserSetup() {
        System.out.println("\n=== Parser Setup Test ===");

        try {
            StringReader reader = new StringReader("42");
            SourceParser sourceParser = new SourceParser(reader, bluej.extensions2.SourceType.Kotlin);
            System.out.println("✓ SourceParser created successfully");

            TestTokenOperations tokenOps = new TestTokenOperations(sourceParser.getTokenStream());
            System.out.println("✓ TokenOperations created successfully");

            TestNodeFactory nodeFactory = new TestNodeFactory();
            System.out.println("✓ NodeFactory created successfully");

            KotlinPrattParser prattParser = new KotlinPrattParser(tokenOps, sourceParser, nodeFactory);
            System.out.println("✓ PrattParser created successfully");

            // Try to parse something simple
            ParsedNode result = prattParser.parseExpression();
            System.out.println("✓ parseExpression() completed");
            System.out.println("  Result: " + (result != null ? result.getClass().getSimpleName() : "null"));

        } catch (Exception e) {
            System.out.println("✗ Setup failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Parse with monolithic parser (via SourceParser)
     */
    @OnThread(Tag.FXPlatform)
    private ParseResult parseWithMonolithicParser(String source) {
        try {
            StringReader reader = new StringReader(source);
            SourceParser sourceParser = new SourceParser(reader, bluej.extensions2.SourceType.Kotlin);

            // Call parseExpression which delegates to the actual parser
            sourceParser.parseExpression();

            // For now we can't capture the actual AST from the monolithic parser easily
            // so we just record that it completed without throwing an exception
            return new ParseResult(true, null, "Monolithic parser completed without exception");

        } catch (Exception e) {
            return new ParseResult(false, null, "Parse error: " + e.getMessage());
        }
    }

    /**
     * Parse with Pratt parser
     */
    @OnThread(Tag.FXPlatform)
    private ParseResult parseWithPrattParser(String source) {
        try {
            StringReader reader = new StringReader(source);
            SourceParser sourceParser = new SourceParser(reader, bluej.extensions2.SourceType.Kotlin);

            TestTokenOperations tokenOps = new TestTokenOperations(sourceParser.getTokenStream());
            TestNodeFactory nodeFactory = new TestNodeFactory();
            KotlinPrattParser prattParser = new KotlinPrattParser(tokenOps, sourceParser, nodeFactory);

            ParsedNode result = prattParser.parseExpression();
            return new ParseResult(true, result, "Pratt parser completed successfully");

        } catch (Exception e) {
            return new ParseResult(false, null, "Parse error: " + e.getMessage());
        }
    }

    /**
     * Simple result container
     */
    private static class ParseResult {
        final boolean success;
        final ParsedNode ast;
        final String message;

        ParseResult(boolean success, ParsedNode ast, String message) {
            this.success = success;
            this.ast = ast;
            this.message = message;
        }
    }
}
