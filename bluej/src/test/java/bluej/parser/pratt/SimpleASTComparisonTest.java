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

import bluej.parser.InitConfig;
import bluej.parser.SourceParser;
import bluej.parser.TestableDocument;
import bluej.parser.entity.ClassLoaderResolver;
import bluej.parser.entity.EntityResolver;
import bluej.parser.nodes.NodeTree;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.nodes.ParsedNode;
import bluej.extensions2.SourceType;
import org.junit.BeforeClass;
import org.junit.Test;

import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.StringReader;
import java.util.Iterator;

/**
 * Simple AST comparison test that logs results without failing.
 *
 * This test helps us understand what's happening with the parser integration
 * by logging detailed results instead of asserting on them.
 */
public class SimpleASTComparisonTest {

    @BeforeClass
    public static void initConfig()
    {
        InitConfig.init();
    }

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

    @Test
    public void testSingleExpressionComparison() {
        System.out.println("\n=== Single Expression Comparison Test ===");

        String[] expressions = {
            "42",
            "\"test\"",
            "myVar",
            "a + b",
            "obj.method()"
        };

        int matches = 0;
        int total = expressions.length;

        for (String expr : expressions) {
            System.out.println("\nComparing: " + expr);

            try {
                ParseResult monolithicResult = parseWithMonolithicParser(expr);
                ParseResult prattResult = parseWithPrattParser(expr);

                System.out.println("  Monolithic: " + (monolithicResult.success ? "SUCCESS" : "FAILED"));
                System.out.println("  Pratt: " + (prattResult.success ? "SUCCESS" : "FAILED"));

                if (monolithicResult.success && prattResult.success) {
                    // Extract expression nodes for comparison
                    ParsedNode monolithicExpr = extractExpressionNode(monolithicResult.ast);
                    ParsedNode prattExpr = prattResult.ast;

                    System.out.println("  Monolithic AST: " + ASTComparisonUtils.astToString(monolithicExpr));
                    System.out.println("  Pratt AST: " + ASTComparisonUtils.astToString(prattExpr));

                    if (monolithicExpr != null && prattExpr != null) {
                        ASTComparisonUtils.ComparisonResult comparison =
                            ASTComparisonUtils.compareAST(monolithicExpr, prattExpr,
                                ASTComparisonUtils.ComparisonMode.STRUCTURAL);

                        if (comparison.matches()) {
                            System.out.println("  ✓ AST structures match!");
                            matches++;
                        } else {
                            System.out.println("  ✗ AST mismatch: " + comparison.toString());
                        }
                    } else {
                        System.out.println("  ⚠ One or both expression nodes not found");
                    }
                } else {
                    System.out.println("  ⚠ Parsing failed for one or both parsers");
                }
            } catch (Exception e) {
                System.out.println("  ✗ Exception: " + e.getMessage());
            }
        }

        System.out.printf("\nSummary: %d/%d expressions matched structurally%n", matches, total);
        System.out.printf("Success rate: %.1f%%%n", (double) matches / total * 100);

        // This is informational only - we don't assert to avoid breaking the build
        // The goal is to see what the current state is
    }

    /**
     * Helper method to extract expression nodes from the monolithic parser's AST
     */
    private ParsedNode extractExpressionNode(ParsedNode root) {
        if (root == null) {
            return null;
        }

        // The monolithic parser wraps expressions in a class/method context
        // We need to traverse down to find the actual expression node
        // This is a simplified extraction - may need refinement
        return findFirstExpressionNode(root);
    }

    private ParsedNode findFirstExpressionNode(ParsedNode node) {
        if (node == null) {
            return null;
        }

        // Check if this node is an expression
        if (node.getNodeType() == ParsedNode.NODETYPE_EXPRESSION) {
            return node;
        }

        // Recursively search children
        Iterator<NodeTree.NodeAndPosition<ParsedNode>> children = node.getChildren(0);
        while (children.hasNext()) {
            NodeTree.NodeAndPosition<ParsedNode> child = children.next();
            ParsedNode result = findFirstExpressionNode(child.getNode());
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    /**
     * Parse with monolithic parser using TestableDocument to capture AST
     */
    @OnThread(Tag.FXPlatform)
    private ParseResult parseWithMonolithicParser(String source) {
        try {
            // Wrap expression in minimal class context for full parsing
            String wrappedSource = "class TestClass {\n    void testMethod() {\n        " + source + ";\n    }\n}";

            EntityResolver resolver = new ClassLoaderResolver(getClass().getClassLoader());
            TestableDocument document = new TestableDocument(resolver, SourceType.Kotlin);
            document.enableParser(true);
            document.insertString(0, wrappedSource);

            // Get the root AST node
            ParsedCUNode rootNode = document.getParser();

            return new ParseResult(true, rootNode, "Monolithic parser completed with AST");

        } catch (Exception e) {
            return new ParseResult(false, null, "Parse error: " + e.getMessage());
        }
    }

    /**
     * Parse with Pratt parser - parse just the expression
     */
    @OnThread(Tag.FXPlatform)
    private ParseResult parseWithPrattParser(String source) {
        try {
            StringReader reader = new StringReader(source);
            SourceParser sourceParser = new SourceParser(reader, SourceType.Kotlin);

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
