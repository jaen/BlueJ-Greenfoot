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
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.StringReader;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark for comparing performance between the monolithic KotlinParser
 * and the new modular KotlinPrattParser implementation.
 *
 * <p>This benchmark measures parsing performance across various expression types
 * and complexity levels to ensure the Pratt parser meets the performance target
 * of being within 10% of the monolithic parser performance.</p>
 *
 * <p>Benchmark categories:</p>
 * <ul>
 *   <li>Simple expressions: literals, identifiers</li>
 *   <li>Binary operations: arithmetic, comparison, logical</li>
 *   <li>Complex expressions: nested operations, precedence chains</li>
 *   <li>Large expressions: deeply nested and wide expression trees</li>
 * </ul>
 *
 * <p>To run this benchmark:</p>
 * <pre>
 * java -cp "classpath" org.openjdk.jmh.Main ParserPerformanceBenchmark
 * </pre>
 *
 * @author BlueJ Team
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class ParserPerformanceBenchmark {

    private static final String PRATT_CONFIG_KEY = "bluej.kotlin.usePrattParser";

    // Test expressions of varying complexity
    private static final String SIMPLE_LITERAL = "42";
    private static final String SIMPLE_IDENTIFIER = "myVariable";
    private static final String BINARY_OPERATION = "a + b";
    private static final String COMPLEX_ARITHMETIC = "a + b * c - d / e";
    private static final String NESTED_PARENTHESES = "((a + b) * (c - d)) / ((e + f) * (g - h))";
    private static final String COMPARISON_CHAIN = "a > b && c < d || e == f && g != h";
    private static final String UNARY_OPERATIONS = "-a + !b + ++c + d--";
    private static final String MIXED_COMPLEX = "(a + b) * (c - d) + e / f - g % h + (i << j) | (k & l)";

    // Large expression for stress testing
    private static final String LARGE_EXPRESSION = buildLargeExpression();

    // Store original configuration to restore after benchmark
    private String originalPrattConfig;

    @Setup
    public void setup() {
        InitConfig.init();
        originalPrattConfig = System.getProperty(PRATT_CONFIG_KEY);
    }

    @TearDown
    public void tearDown() {
        // Restore original configuration
        if (originalPrattConfig != null) {
            System.setProperty(PRATT_CONFIG_KEY, originalPrattConfig);
        } else {
            System.clearProperty(PRATT_CONFIG_KEY);
        }
    }

    // ==================== Simple Expression Benchmarks ====================

    @Benchmark
    public boolean parseSimpleLiteralMonolithic() {
        return parseWithMonolithic(SIMPLE_LITERAL);
    }

    @Benchmark
    public boolean parseSimpleLiteralPratt() {
        return parseWithPratt(SIMPLE_LITERAL);
    }

    @Benchmark
    public boolean parseSimpleIdentifierMonolithic() {
        return parseWithMonolithic(SIMPLE_IDENTIFIER);
    }

    @Benchmark
    public boolean parseSimpleIdentifierPratt() {
        return parseWithPratt(SIMPLE_IDENTIFIER);
    }

    // ==================== Binary Operation Benchmarks ====================

    @Benchmark
    public boolean parseBinaryOperationMonolithic() {
        return parseWithMonolithic(BINARY_OPERATION);
    }

    @Benchmark
    public boolean parseBinaryOperationPratt() {
        return parseWithPratt(BINARY_OPERATION);
    }

    @Benchmark
    public boolean parseComplexArithmeticMonolithic() {
        return parseWithMonolithic(COMPLEX_ARITHMETIC);
    }

    @Benchmark
    public boolean parseComplexArithmeticPratt() {
        return parseWithPratt(COMPLEX_ARITHMETIC);
    }

    // ==================== Complex Expression Benchmarks ====================

    @Benchmark
    public boolean parseNestedParenthesesMonolithic() {
        return parseWithMonolithic(NESTED_PARENTHESES);
    }

    @Benchmark
    public boolean parseNestedParenthesesPratt() {
        return parseWithPratt(NESTED_PARENTHESES);
    }

    @Benchmark
    public boolean parseComparisonChainMonolithic() {
        return parseWithMonolithic(COMPARISON_CHAIN);
    }

    @Benchmark
    public boolean parseComparisonChainPratt() {
        return parseWithPratt(COMPARISON_CHAIN);
    }

    @Benchmark
    public boolean parseUnaryOperationsMonolithic() {
        return parseWithMonolithic(UNARY_OPERATIONS);
    }

    @Benchmark
    public boolean parseUnaryOperationsPratt() {
        return parseWithPratt(UNARY_OPERATIONS);
    }

    @Benchmark
    public boolean parseMixedComplexMonolithic() {
        return parseWithMonolithic(MIXED_COMPLEX);
    }

    @Benchmark
    public boolean parseMixedComplexPratt() {
        return parseWithPratt(MIXED_COMPLEX);
    }

    // ==================== Large Expression Benchmarks ====================

    @Benchmark
    public boolean parseLargeExpressionMonolithic() {
        return parseWithMonolithic(LARGE_EXPRESSION);
    }

    @Benchmark
    public boolean parseLargeExpressionPratt() {
        return parseWithPratt(LARGE_EXPRESSION);
    }

    // ==================== Combined Parser Benchmarks ====================

    /**
     * Benchmark parsing a variety of expressions with monolithic parser.
     * This tests overall parser performance across different expression types.
     */
    @Benchmark
    public int parseVariedExpressionsMonolithic() {
        int successful = 0;
        if (parseWithMonolithic(SIMPLE_LITERAL)) successful++;
        if (parseWithMonolithic(BINARY_OPERATION)) successful++;
        if (parseWithMonolithic(COMPLEX_ARITHMETIC)) successful++;
        if (parseWithMonolithic(NESTED_PARENTHESES)) successful++;
        if (parseWithMonolithic(COMPARISON_CHAIN)) successful++;
        if (parseWithMonolithic(UNARY_OPERATIONS)) successful++;
        if (parseWithMonolithic(MIXED_COMPLEX)) successful++;
        return successful;
    }

    /**
     * Benchmark parsing a variety of expressions with Pratt parser.
     * This tests overall parser performance across different expression types.
     */
    @Benchmark
    public int parseVariedExpressionsPratt() {
        int successful = 0;
        if (parseWithPratt(SIMPLE_LITERAL)) successful++;
        if (parseWithPratt(BINARY_OPERATION)) successful++;
        if (parseWithPratt(COMPLEX_ARITHMETIC)) successful++;
        if (parseWithPratt(NESTED_PARENTHESES)) successful++;
        if (parseWithPratt(COMPARISON_CHAIN)) successful++;
        if (parseWithPratt(UNARY_OPERATIONS)) successful++;
        if (parseWithPratt(MIXED_COMPLEX)) successful++;
        return successful;
    }

    // ==================== Direct Pratt Parser Benchmarks ====================

    /**
     * Benchmark the Pratt parser directly without going through SourceParser.
     * This measures the raw parsing performance without adapter overhead.
     */
    @Benchmark
    public boolean parseDirectPrattSimple() {
        return parseDirectWithPratt(BINARY_OPERATION);
    }

    @Benchmark
    public boolean parseDirectPrattComplex() {
        return parseDirectWithPratt(MIXED_COMPLEX);
    }

    @Benchmark
    public boolean parseDirectPrattLarge() {
        return parseDirectWithPratt(LARGE_EXPRESSION);
    }

    // ==================== Helper Methods ====================

    /**
     * Parse expression with monolithic parser (Pratt disabled).
     */
    private boolean parseWithMonolithic(String expression) {
        System.setProperty(PRATT_CONFIG_KEY, "false");
        return parseExpression(expression);
    }

    /**
     * Parse expression with Pratt parser (Pratt enabled).
     */
    private boolean parseWithPratt(String expression) {
        System.setProperty(PRATT_CONFIG_KEY, "true");
        return parseExpression(expression);
    }

    /**
     * Parse expression using SourceParser with current configuration.
     */
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

    /**
     * Parse expression directly with Pratt parser (bypassing SourceParser).
     * This measures raw Pratt parser performance.
     */
    private boolean parseDirectWithPratt(String expression) {
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

    /**
     * Build a large expression for stress testing.
     * Creates a deeply nested expression with multiple operators and precedence levels.
     */
    private static String buildLargeExpression() {
        StringBuilder expr = new StringBuilder();

        // Create a large arithmetic expression with nested parentheses
        expr.append("(((a + b) * (c - d)) / ((e + f) * (g - h))) + ");
        expr.append("(((i | j) & (k ^ l)) << ((m + n) - (o * p))) - ");
        expr.append("(((q == r) && (s != t)) || ((u < v) && (w > x))) * ");
        expr.append("(((y % z) + (aa - bb)) / ((cc * dd) + (ee / ff))) + ");
        expr.append("(((gg && hh) || (ii && jj)) && ((kk < ll) || (mm > nn))) - ");
        expr.append("(((oo + pp) * (qq - rr)) / ((ss + tt) * (uu - vv)))");

        return expr.toString();
    }

    /**
     * Main method to run the benchmark.
     * Usage: java -cp "classpath" bluej.parser.pratt.ParserPerformanceBenchmark
     */
    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(ParserPerformanceBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(5)
                .measurementIterations(10)
                .build();

        new Runner(opt).run();
    }
}
