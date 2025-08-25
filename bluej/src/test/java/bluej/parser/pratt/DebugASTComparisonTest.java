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
import bluej.extensions2.SourceType;
import bluej.parser.SourceParser;
import bluej.parser.nodes.NodeTree;
import bluej.parser.nodes.ParsedNode;
import org.junit.Before;

import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;

/**
 * Debug test that writes detailed output to a file to analyze AST comparison issues.
 */
//@Category(NonParallelisableTests.class)
public class DebugASTComparisonTest {

    private static final String OUTPUT_FILE = "/tmp/ast_debug_output.txt";

    @Test
    public void testDetailedASTAnalysis() {
        try (FileWriter writer = new FileWriter(OUTPUT_FILE)) {
            writer.write("=== Detailed AST Analysis Debug Output ===\n\n");

            // Test various expression types with detailed analysis
            String[] expressions = {
                "42",
                "\"hello\"",
                "true",
                "null",
                "myVar",
                "a + b",
                "x * y",
                "-z",
                "++i",
                "j++",
                "(expr)",
                "obj.prop",
                "func()",
                "arr[0]"
            };

            for (String expr : expressions) {
                analyzeExpression(expr, writer);
            }

            writer.write("\n=== AST Comparison Tests ===\n\n");

            // Test specific comparison scenarios
            testASTComparison("42", "42", writer);
            testASTComparison("a + b", "x * y", writer);
            testASTComparison("(expr)", "expr", writer);

            writer.write("\n=== Summary ===\n");
            writer.write("Debug output complete. Check file for detailed analysis.\n");

            System.out.println("Debug output written to: " + OUTPUT_FILE);

        } catch (IOException e) {
            fail("Failed to write debug output: " + e.getMessage());
        }
    }

    @OnThread(Tag.FXPlatform)
    private void analyzeExpression(String expression, FileWriter writer) throws IOException {
        writer.write("Analyzing: " + expression + "\n");
        writer.write("-----------------------------------\n");

        try {
            // Parse with Pratt parser
            StringReader reader = new StringReader(expression);
            SourceParser sourceParser = new SourceParser(reader, SourceType.Kotlin);
            TestTokenOperations tokenOps = new TestTokenOperations(sourceParser.getTokenStream());
            TestNodeFactory nodeFactory = new TestNodeFactory();
            KotlinPrattParser prattParser = new KotlinPrattParser(tokenOps, sourceParser, nodeFactory);

            ParsedNode result = prattParser.parseExpression();

            if (result != null) {
                writer.write("✓ Parse successful\n");
                writer.write("Node type: " + result.getClass().getSimpleName() + "\n");
                writer.write("Node toString: " + result.toString() + "\n");
                writer.write("Node type code: " + result.getNodeType() + "\n");

                // Use ASTComparisonUtils to get detailed string representation
                writer.write("AST structure: " + ASTComparisonUtils.astToString(result, 3) + "\n");

                // Analyze children
                analyzeNodeChildren(result, writer, 1);

                // Test node comparison with itself (should always match)
                ASTComparisonUtils.ComparisonResult selfComparison =
                    ASTComparisonUtils.compareAST(result, result, ASTComparisonUtils.ComparisonMode.STRUCTURAL);
                writer.write("Self-comparison: " + selfComparison.matches() + "\n");
                if (!selfComparison.matches()) {
                    writer.write("Self-comparison issues: " + selfComparison.getDifferences() + "\n");
                }

            } else {
                writer.write("✗ Parse returned null\n");
            }

        } catch (Exception e) {
            writer.write("✗ Parse failed with exception: " + e.getClass().getSimpleName() + ": " + e.getMessage() + "\n");
            writer.write("Stack trace:\n");
            for (StackTraceElement element : e.getStackTrace()) {
                if (element.getClassName().contains("bluej.parser.pratt")) {
                    writer.write("  " + element.toString() + "\n");
                }
            }
        }

        writer.write("\n");
    }

    private void analyzeNodeChildren(ParsedNode node, FileWriter writer, int depth) throws IOException {
        if (depth > 3) return; // Prevent infinite recursion

        Iterator<NodeTree.NodeAndPosition<ParsedNode>> children = node.getChildren(0);
        int childCount = 0;

        while (children.hasNext()) {
            NodeTree.NodeAndPosition<ParsedNode> child = children.next();
            childCount++;

            String indent = "  ".repeat(depth);
            writer.write(indent + "Child " + childCount + ": " + child.getNode().getClass().getSimpleName() +
                        " at position " + child.getPosition() + "\n");

            analyzeNodeChildren(child.getNode(), writer, depth + 1);
        }

        if (childCount == 0) {
            String indent = "  ".repeat(depth);
            writer.write(indent + "(no children)\n");
        }
    }

    private void testASTComparison(String expr1, String expr2, FileWriter writer) throws IOException {
        writer.write("Comparing: '" + expr1 + "' vs '" + expr2 + "'\n");
        writer.write("-----------------------------------\n");

        try {
            ParsedNode ast1 = parseExpression(expr1);
            ParsedNode ast2 = parseExpression(expr2);

            writer.write("AST1: " + (ast1 != null ? ast1.getClass().getSimpleName() : "null") + "\n");
            writer.write("AST2: " + (ast2 != null ? ast2.getClass().getSimpleName() : "null") + "\n");

            if (ast1 != null && ast2 != null) {
                // Test different comparison modes
                testComparisonMode(ast1, ast2, ASTComparisonUtils.ComparisonMode.EXACT, writer);
                testComparisonMode(ast1, ast2, ASTComparisonUtils.ComparisonMode.STRUCTURAL, writer);
                testComparisonMode(ast1, ast2, ASTComparisonUtils.ComparisonMode.FOUNDATION_PHASE, writer);
            } else {
                writer.write("Cannot compare - one or both ASTs are null\n");
            }

        } catch (Exception e) {
            writer.write("Comparison failed: " + e.getMessage() + "\n");
        }

        writer.write("\n");
    }

    private void testComparisonMode(ParsedNode ast1, ParsedNode ast2, ASTComparisonUtils.ComparisonMode mode,
                                  FileWriter writer) throws IOException {
        ASTComparisonUtils.ComparisonResult result = ASTComparisonUtils.compareAST(ast1, ast2, mode);

        writer.write(mode.name() + " comparison: " + result.matches() + "\n");
        writer.write("  Nodes compared: " + result.getComparedNodes() + "/" + result.getNodeCount() + "\n");

        if (!result.matches() && !result.getDifferences().isEmpty()) {
            writer.write("  Differences:\n");
            for (String diff : result.getDifferences()) {
                writer.write("    " + diff + "\n");
            }
        }
    }

    @OnThread(Tag.FXPlatform)
    private ParsedNode parseExpression(String expression) {
        try {
            StringReader reader = new StringReader(expression);
            SourceParser sourceParser = new SourceParser(reader, SourceType.Kotlin);
            TestTokenOperations tokenOps = new TestTokenOperations(sourceParser.getTokenStream());
            TestNodeFactory nodeFactory = new TestNodeFactory();
            KotlinPrattParser prattParser = new KotlinPrattParser(tokenOps, sourceParser, nodeFactory);

            return prattParser.parseExpression();
        } catch (Exception e) {
            return null;
        }
    }

    @Test
    public void testSpecificFailureCases() {
        try (FileWriter writer = new FileWriter("/tmp/failure_analysis.txt")) {
            writer.write("=== Specific Failure Case Analysis ===\n\n");

            // Test cases that are likely failing
            writer.write("Testing cases that might be causing assertion failures...\n\n");

            // Simple expressions that should work
            String[] basicCases = {"42", "\"test\"", "myVar", "true", "null"};

            int successCount = 0;
            int totalCount = basicCases.length;

            for (String testCase : basicCases) {
                writer.write("Testing: " + testCase + "\n");

                try {
                    ParsedNode result = parseExpression(testCase);

                    if (result != null) {
                        writer.write("  ✓ SUCCESS: " + result.getClass().getSimpleName() + "\n");
                        successCount++;
                    } else {
                        writer.write("  ✗ FAILED: null result\n");
                    }
                } catch (Exception e) {
                    writer.write("  ✗ EXCEPTION: " + e.getMessage() + "\n");
                }
            }

            writer.write("\nResults: " + successCount + "/" + totalCount + " basic cases succeeded\n");
            writer.write("Success rate: " + (100.0 * successCount / totalCount) + "%\n");

            if (successCount < totalCount) {
                writer.write("\nThis explains why ParserASTComparisonIntegrationTest is failing!\n");
                writer.write("The assertion at line 321 requires zero failures for critical test suites,\n");
                writer.write("but we have " + (totalCount - successCount) + " failures out of " + totalCount + " tests.\n");
            } else {
                writer.write("\nBasic cases all pass - issue might be in comparison logic or test setup.\n");
            }

            System.out.println("Failure analysis written to: /tmp/failure_analysis.txt");

        } catch (IOException e) {
            fail("Failed to write failure analysis: " + e.getMessage());
        }
    }
}
