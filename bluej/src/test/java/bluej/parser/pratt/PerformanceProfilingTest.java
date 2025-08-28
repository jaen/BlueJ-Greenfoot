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

import bluej.parser.SourceParser;
import bluej.parser.InitConfig;
import bluej.parser.nodes.ParsedNode;
import bluej.extensions2.SourceType;
import org.junit.*;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Focused performance profiling test to identify specific bottlenecks in Pratt parser.
 *
 * <p>This test investigates the root cause of the 21.30x performance degradation observed
 * on large expressions. It profiles different components of the parsing process to identify
 * where the exponential complexity is occurring.</p>
 *
 * <p>Analysis areas:</p>
 * <ul>
 *   <li>Expression complexity scaling - How performance changes with expression depth/width</li>
 *   <li>Parselet dispatch overhead - Registry lookup performance</li>
 *   <li>Recursive parsing efficiency - Call stack depth analysis</li>
 *   <li>Memory allocation patterns - GC pressure analysis</li>
 *   <li>Component isolation - Tokenization vs parsing vs AST creation</li>
 * </ul>
 *
 * @author BlueJ Team
 */
public class PerformanceProfilingTest {

    @BeforeClass
    public static void initConfig() {
        InitConfig.init();
    }

    private static final String PRATT_CONFIG_KEY = "bluej.kotlin.usePrattParser";
    private String originalPrattConfig;

    @Before
    public void setUp() throws Exception {
        originalPrattConfig = System.getProperty(PRATT_CONFIG_KEY);
    }

    @After
    public void tearDown() throws Exception {
        if (originalPrattConfig != null) {
            System.setProperty(PRATT_CONFIG_KEY, originalPrattConfig);
        } else {
            System.clearProperty(PRATT_CONFIG_KEY);
        }
    }

    @Test
    public void profileExpressionComplexityScaling() {
        System.out.println("=== Expression Complexity Scaling Analysis ===");

        // Test expressions of increasing complexity
        String[] expressions = {
            "a + b",                                    // Depth 1, Width 3
            "(a + b) * (c + d)",                       // Depth 2, Width 7
            "((a + b) * (c + d)) + ((e + f) * (g + h))", // Depth 3, Width 15
            buildNestedExpression(4),                   // Depth 4
            buildNestedExpression(5),                   // Depth 5
            buildNestedExpression(6),                   // Depth 6
            buildNestedExpression(7),                   // Depth 7 (similar to failing test)
        };

        System.out.println("Testing expression complexity scaling:");
        System.out.println("Format: Depth | Expression Length | Monolithic (ns) | Pratt (ns) | Ratio");
        System.out.println("-------|------------------|-----------------|------------|-------");

        for (int i = 0; i < expressions.length; i++) {
            String expr = expressions[i];
            int depth = i + 1;

            // Measure both parsers with sufficient iterations for accuracy
            long monolithicTime = measureAverageTime(expr, false, 100);
            long prattTime = measureAverageTime(expr, true, 100);

            double ratio = (double) prattTime / monolithicTime;

            System.out.printf("%6d | %15d | %14d | %10d | %6.2f%n",
                depth, expr.length(), monolithicTime, prattTime, ratio);

            // Alert on exponential growth
            if (i > 0 && ratio > 5.0) {
                System.out.println("  ⚠️  EXPONENTIAL GROWTH DETECTED at depth " + depth);
            }
        }
        System.out.println();
    }

    @Test
    public void profileParseletDispatchOverhead() {
        System.out.println("=== Parselet Dispatch Overhead Analysis ===");

        // Test different types of tokens to see registry lookup performance
        String[] tokenTypes = {
            "42",           // Literal
            "identifier",   // Identifier
            "a + b",        // Binary operator
            "-x",           // Unary prefix
            "x++",          // Unary postfix
            "(expr)",       // Grouping
            "a.b",          // Member access
            "func()",       // Function call
        };

        System.out.println("Measuring parselet dispatch performance:");
        System.out.println("Expression Type      | Direct Pratt (ns) | Via Adapter (ns) | Overhead");
        System.out.println("--------------------|--------------------|-------------------|----------");

        for (String expr : tokenTypes) {
            long directTime = measureDirectPrattTime(expr, 500);
            long adapterTime = measureAdapterPrattTime(expr, 500);

            double overhead = (double) adapterTime / directTime;

            System.out.printf("%-19s | %17d | %16d | %7.2fx%n",
                getExpressionType(expr), directTime, adapterTime, overhead);
        }
        System.out.println();
    }

    @Ignore("This never finishes")
    @Test
    public void profileComponentIsolation() {
        System.out.println("=== Component Isolation Analysis ===");

        String testExpr = buildNestedExpression(5); // Moderately complex

        System.out.println("Breaking down parsing components for: " +
            (testExpr.length() > 50 ? testExpr.substring(0, 47) + "..." : testExpr));

        // Measure tokenization only
        long tokenizationTime = measureTokenizationTime(testExpr, 200);

        // Measure direct parsing (no adapter)
        long directParsingTime = measureDirectPrattTime(testExpr, 200);

        // Measure full adapter parsing
        long fullParsingTime = measureAdapterPrattTime(testExpr, 200);

        // Estimate component times
        long pureParsingTime = Math.max(0, directParsingTime - tokenizationTime);
        long adapterOverhead = Math.max(0, fullParsingTime - directParsingTime);

        System.out.println("Component breakdown:");
        System.out.printf("Tokenization:     %8d ns (%5.1f%%)%n",
            tokenizationTime, 100.0 * tokenizationTime / fullParsingTime);
        System.out.printf("Pure Parsing:     %8d ns (%5.1f%%)%n",
            pureParsingTime, 100.0 * pureParsingTime / fullParsingTime);
        System.out.printf("Adapter Overhead: %8d ns (%5.1f%%)%n",
            adapterOverhead, 100.0 * adapterOverhead / fullParsingTime);
        System.out.printf("Total:            %8d ns%n", fullParsingTime);
        System.out.println();
    }

    @Test
    public void profileRecursiveCallDepth() {
        System.out.println("=== Recursive Call Depth Analysis ===");

        // Create expressions with different nesting patterns
        String[] nestingPatterns = {
            buildLeftAssociative(5),   // ((((a + b) + c) + d) + e)
            buildRightAssociative(5),  // (a + (b + (c + (d + e))))
            buildBalanced(4),          // ((a + b) + (c + d))
            buildDeepNested(6),        // Deeply nested parentheses
        };

        String[] patternNames = {"Left Associative", "Right Associative", "Balanced", "Deep Nested"};

        System.out.println("Testing different nesting patterns:");
        System.out.println("Pattern           | Expression Length | Parse Time (ns) | Notes");
        System.out.println("------------------|-------------------|-----------------|-------");

        for (int i = 0; i < nestingPatterns.length; i++) {
            String expr = nestingPatterns[i];
            long parseTime = measureDirectPrattTime(expr, 100);

            System.out.printf("%-16s | %16d | %14d | %s%n",
                patternNames[i], expr.length(), parseTime, analyzePattern(parseTime, i));
        }
        System.out.println();
    }

    @Test
    public void profileMemoryAllocation() {
        System.out.println("=== Memory Allocation Analysis ===");

        String testExpr = buildNestedExpression(6);

        // Run GC before measurement
        System.gc();
        Thread.yield();

        Runtime runtime = Runtime.getRuntime();

        // Measure memory before parsing
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // Parse multiple times to amplify memory usage
        for (int i = 0; i < 100; i++) {
            try {
                StringReader reader = new StringReader(testExpr);
                SourceParser sourceParser = new SourceParser(reader, SourceType.Kotlin);
                TestTokenOperations tokenOps = new TestTokenOperations(sourceParser.getTokenStream());
                TestNodeFactory nodeFactory = new TestNodeFactory();
                KotlinPrattParser prattParser = new KotlinPrattParser(tokenOps, sourceParser, nodeFactory);

                ParsedNode result = prattParser.parseExpression();

                // Don't let GC clean up immediately
                if (result == null) {
                    System.out.print(""); // Prevent optimization
                }
            } catch (Exception e) {
                // Continue measuring
            }
        }

        // Measure memory after parsing
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;

        System.out.println("Memory allocation analysis (100 iterations):");
        System.out.println("Memory before: " + formatBytes(memoryBefore));
        System.out.println("Memory after:  " + formatBytes(memoryAfter));
        System.out.println("Memory used:   " + formatBytes(memoryUsed));
        System.out.println("Per operation: " + formatBytes(memoryUsed / 100));
        System.out.println();
    }

    // Helper methods for timing measurements

    private long measureAverageTime(String expression, boolean usePratt, int iterations) {
        System.setProperty(PRATT_CONFIG_KEY, Boolean.toString(usePratt));

        // Warmup
        for (int i = 0; i < 20; i++) {
            parseExpression(expression);
        }

        // Measure
        long totalTime = 0;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            parseExpression(expression);
            long end = System.nanoTime();
            totalTime += (end - start);
        }

        return totalTime / iterations;
    }

    private long measureDirectPrattTime(String expression, int iterations) {
        // Warmup
        for (int i = 0; i < 20; i++) {
            parseDirectPratt(expression);
        }

        // Measure
        long totalTime = 0;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            parseDirectPratt(expression);
            long end = System.nanoTime();
            totalTime += (end - start);
        }

        return totalTime / iterations;
    }

    private long measureAdapterPrattTime(String expression, int iterations) {
        System.setProperty(PRATT_CONFIG_KEY, "true");
        return measureAverageTime(expression, true, iterations);
    }

    private long measureTokenizationTime(String expression, int iterations) {
        // Warmup
        for (int i = 0; i < 20; i++) {
            tokenizeOnly(expression);
        }

        // Measure
        long totalTime = 0;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            tokenizeOnly(expression);
            long end = System.nanoTime();
            totalTime += (end - start);
        }

        return totalTime / iterations;
    }

    private boolean parseExpression(String expression) {
        try {
            StringReader reader = new StringReader(expression);
            SourceParser parser = new SourceParser(reader, SourceType.Kotlin);
            parser.parseExpression();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean parseDirectPratt(String expression) {
        try {
            StringReader reader = new StringReader(expression);
            SourceParser sourceParser = new SourceParser(reader, SourceType.Kotlin);
            TestTokenOperations tokenOps = new TestTokenOperations(sourceParser.getTokenStream());
            TestNodeFactory nodeFactory = new TestNodeFactory();
            KotlinPrattParser prattParser = new KotlinPrattParser(tokenOps, sourceParser, nodeFactory);

            ParsedNode result = prattParser.parseExpression();
            return result != null && !prattParser.hasErrors();
        } catch (Exception e) {
            return false;
        }
    }

    private void tokenizeOnly(String expression) {
        try {
            StringReader reader = new StringReader(expression);
            SourceParser sourceParser = new SourceParser(reader, SourceType.Kotlin);

            // Just consume all tokens without parsing
            while (sourceParser.getTokenStream().LA(1).getType() != -1) { // EOF
                sourceParser.getTokenStream().nextToken();
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    // Helper methods for building test expressions

    private String buildNestedExpression(int depth) {
        if (depth <= 1) {
            return "a + b";
        }

        String inner = buildNestedExpression(depth - 1);
        return "(" + inner + ") * (" + inner.replace("a", "x").replace("b", "y") + ")";
    }

    private String buildLeftAssociative(int terms) {
        if (terms <= 1) return "a";

        StringBuilder sb = new StringBuilder();
        sb.append("((((");
        for (int i = 0; i < terms - 1; i++) {
            sb.append("a + ");
        }
        sb.append("a");
        for (int i = 0; i < terms - 1; i++) {
            sb.append(")");
        }

        return sb.toString();
    }

    private String buildRightAssociative(int terms) {
        if (terms <= 1) return "a";
        if (terms == 2) return "a + b";

        return "a + (" + buildRightAssociative(terms - 1).replace("a", "b") + ")";
    }

    private String buildBalanced(int depth) {
        if (depth <= 0) return "a";
        if (depth == 1) return "a + b";

        String left = buildBalanced(depth - 1);
        String right = buildBalanced(depth - 1).replace("a", "x").replace("b", "y");
        return "(" + left + ") + (" + right + ")";
    }

    private String buildDeepNested(int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append("(");
        }
        sb.append("a + b");
        for (int i = 0; i < depth; i++) {
            sb.append(")");
        }
        return sb.toString();
    }

    private String getExpressionType(String expr) {
        if (expr.matches("\\d+")) return "Literal";
        if (expr.matches("\\w+") && !expr.equals("true") && !expr.equals("false")) return "Identifier";
        if (expr.contains("+") || expr.contains("-") || expr.contains("*") || expr.contains("/")) return "Binary Op";
        if (expr.startsWith("-") || expr.startsWith("!")) return "Unary Prefix";
        if (expr.endsWith("++") || expr.endsWith("--")) return "Unary Postfix";
        if (expr.startsWith("(") && expr.endsWith(")")) return "Grouping";
        if (expr.contains(".")) return "Member Access";
        if (expr.contains("()")) return "Function Call";
        return "Other";
    }

    private String analyzePattern(long parseTime, int patternIndex) {
        // Basic heuristics for identifying performance issues
        if (parseTime > 100000) return "SLOW";
        if (parseTime > 50000) return "Medium";
        return "Fast";
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
