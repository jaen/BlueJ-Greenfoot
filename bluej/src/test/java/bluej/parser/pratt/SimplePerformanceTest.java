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
import org.junit.Before;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple performance comparison test between monolithic and Pratt parsers.
 *
 * <p>This test provides basic performance benchmarking without external dependencies
 * like JMH. It measures parsing times for various expression types and compares
 * the performance between the two parser implementations.</p>
 *
 * <p>Performance goal: Pratt parser should be within 10% of monolithic parser performance.</p>
 *
 * @author BlueJ Team
 */
public class SimplePerformanceTest {

    @BeforeClass
    public static void initConfig() {
        InitConfig.init();
    }

    private static final String PRATT_CONFIG_KEY = "bluej.kotlin.usePrattParser";

    // Store original config value to restore after tests
    private String originalPrattConfig;

    // Test expressions of varying complexity
    private static final String[] TEST_EXPRESSIONS = {
        // Simple expressions
        "42",
        "myVariable",
        "true",
        "\"hello world\"",

        // Binary operations
        "a + b",
        "x * y",
        "a == b",
        "flag && other",

        // Complex arithmetic
        "a + b * c - d / e",
        "x * y + z - w / q",
        "(a + b) * (c - d)",

        // Nested expressions
        "((a + b) * (c - d)) / ((e + f) * (g - h))",
        "(x + y) * (z - w) + (a / b) - (c % d)",

        // Unary operations
        "-a",
        "!flag",
        "++counter",
        "value--",
        "-x + !y + ++z + w--",

        // Comparison chains
        "a > b && c < d",
        "x == y || z != w",
        "a > b && c < d || e == f && g != h",

        // Mixed complex expressions
        "(a + b) * (c - d) + e / f - g % h",
        "((x || y) && (z || w)) && ((a < b) || (c > d))",

        // Large nested expression
        buildLargeExpression()
    };

    // Number of iterations for timing tests
    private static final int WARMUP_ITERATIONS = 100;
    private static final int MEASUREMENT_ITERATIONS = 1000;

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
    public void testPerformanceComparison() {
        System.out.println("=== Parser Performance Comparison ===");
        System.out.println("Warmup iterations: " + WARMUP_ITERATIONS);
        System.out.println("Measurement iterations: " + MEASUREMENT_ITERATIONS);
        System.out.println("Target: Pratt parser within 10% of monolithic parser performance");
        System.out.println();

        List<PerformanceResult> results = new ArrayList<>();

        for (int i = 0; i < TEST_EXPRESSIONS.length; i++) {
            String expression = TEST_EXPRESSIONS[i];
            String testName = getTestName(expression, i);

            System.out.println("Testing: " + testName);
            System.out.println("Expression: " + (expression.length() > 60 ?
                expression.substring(0, 57) + "..." : expression));

            PerformanceResult result = measureExpressionPerformance(expression, testName);
            results.add(result);

            System.out.println("Monolithic: " + formatTime(result.monolithicTime) + " ns/op");
            System.out.println("Pratt:      " + formatTime(result.prattTime) + " ns/op");
            System.out.println("Ratio:      " + String.format("%.2f", result.getRatio()) +
                              " (Pratt/Monolithic)");
            System.out.println("Performance: " +
                (result.isWithinTarget() ? "✅ WITHIN TARGET" : "❌ OUTSIDE TARGET"));
            System.out.println();
        }

        // Overall performance summary
        System.out.println("=== Overall Performance Summary ===");
        PerformanceResult overall = calculateOverallPerformance(results);

        System.out.println("Overall average ratio: " + String.format("%.2f", overall.getRatio()));
        System.out.println("Tests within target: " + countWithinTarget(results) + "/" + results.size());

        // Verify overall performance is within target
        assertTrue("Overall Pratt parser performance should be within 10% of monolithic parser. " +
                  "Actual ratio: " + String.format("%.2f", overall.getRatio()) +
                  " (target: ≤ 1.10)", overall.isWithinTarget());

        // Verify that at least 80% of individual tests are within target
        double withinTargetPercentage = (double) countWithinTarget(results) / results.size();
        assertTrue("At least 80% of tests should be within performance target. " +
                  "Actual: " + String.format("%.1f%%", withinTargetPercentage * 100),
                  withinTargetPercentage >= 0.8);
    }

    private PerformanceResult measureExpressionPerformance(String expression, String testName) {
        // Warmup both parsers
        warmupParser(expression, true);  // Pratt
        warmupParser(expression, false); // Monolithic

        // Measure monolithic parser
        long monolithicTime = measureParsingTime(expression, false);

        // Measure Pratt parser
        long prattTime = measureParsingTime(expression, true);

        return new PerformanceResult(testName, expression, monolithicTime, prattTime);
    }

    private void warmupParser(String expression, boolean usePratt) {
        System.setProperty(PRATT_CONFIG_KEY, Boolean.toString(usePratt));

        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            try {
                StringReader reader = new StringReader(expression);
                SourceParser parser = new SourceParser(reader, SourceType.Kotlin);
                parser.parseExpression();
            } catch (Exception e) {
                // Ignore exceptions during warmup
            }
        }
    }

    private long measureParsingTime(String expression, boolean usePratt) {
        System.setProperty(PRATT_CONFIG_KEY, Boolean.toString(usePratt));

        long totalTime = 0;
        int successfulRuns = 0;

        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            long startTime = System.nanoTime();

            try {
                StringReader reader = new StringReader(expression);
                SourceParser parser = new SourceParser(reader, SourceType.Kotlin);
                parser.parseExpression();

                long endTime = System.nanoTime();
                totalTime += (endTime - startTime);
                successfulRuns++;
            } catch (Exception e) {
                // Count failed runs as taking maximum time to penalize failures
                long endTime = System.nanoTime();
                totalTime += (endTime - startTime) + 1000000; // Add 1ms penalty
                successfulRuns++;
            }
        }

        return totalTime / successfulRuns;
    }

    private String getTestName(String expression, int index) {
        if (expression.length() <= 20) {
            return "Test " + (index + 1) + ": \"" + expression + "\"";
        } else {
            return "Test " + (index + 1) + ": Complex Expression";
        }
    }

    private String formatTime(long nanos) {
        if (nanos < 1000) {
            return String.valueOf(nanos);
        } else if (nanos < 1000000) {
            return String.format("%.1f", nanos / 1000.0) + "k";
        } else {
            return String.format("%.1f", nanos / 1000000.0) + "M";
        }
    }

    private PerformanceResult calculateOverallPerformance(List<PerformanceResult> results) {
        long totalMonolithic = 0;
        long totalPratt = 0;

        for (PerformanceResult result : results) {
            totalMonolithic += result.monolithicTime;
            totalPratt += result.prattTime;
        }

        long avgMonolithic = totalMonolithic / results.size();
        long avgPratt = totalPratt / results.size();

        return new PerformanceResult("Overall Average", "", avgMonolithic, avgPratt);
    }

    private int countWithinTarget(List<PerformanceResult> results) {
        int count = 0;
        for (PerformanceResult result : results) {
            if (result.isWithinTarget()) {
                count++;
            }
        }
        return count;
    }

    private static String buildLargeExpression() {
        return "(((a + b) * (c - d)) / ((e + f) * (g - h))) + " +
               "(((i | j) & (k ^ l)) << ((m + n) - (o * p))) - " +
               "(((q == r) && (s != t)) || ((u < v) && (w > x))) * " +
               "(((y % z) + (aa - bb)) / ((cc * dd) + (ee / ff)))";
    }

    /**
     * Represents the performance measurement result for a single test case.
     */
    private static class PerformanceResult {
        final String testName;
        final String expression;
        final long monolithicTime;
        final long prattTime;

        public PerformanceResult(String testName, String expression, long monolithicTime, long prattTime) {
            this.testName = testName;
            this.expression = expression;
            this.monolithicTime = monolithicTime;
            this.prattTime = prattTime;
        }

        public double getRatio() {
            return (double) prattTime / monolithicTime;
        }

        public boolean isWithinTarget() {
            return getRatio() <= 1.10; // Within 10% (ratio ≤ 1.10)
        }
    }
}
