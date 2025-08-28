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
import bluej.extensions2.SourceType;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * JMH-based performance comparison benchmark between monolithic and Pratt parsers.
 *
 * <p>This benchmark measures parsing times for various expression types and compares
 * the performance between the two parser implementations.</p>
 *
 * <p>Performance goal: Pratt parser should be within 10% of monolithic parser performance.</p>
 *
 * <p>To run benchmarks: execute the test method or use JMH command line tools.</p>
 *
 * @author BlueJ Team
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
public class SimplePerformanceTest {

    private static final String PRATT_CONFIG_KEY = "bluej.kotlin.usePrattParser";
    private static final double PERFORMANCE_TARGET_RATIO = 1.10; // Pratt should be within 10% of monolithic

    @Param({"false", "true"})
    private boolean usePrattParser;

    static {
        InitConfig.init();
    }

    @Setup(Level.Trial)
    public void setUp() {
        System.setProperty(PRATT_CONFIG_KEY, Boolean.toString(usePrattParser));
    }

    // Benchmark methods for each expression type

    @Benchmark
    public void benchmarkIntegerLiteral() {
        parseExpression("42");
    }

    @Benchmark
    public void benchmarkSimpleVariable() {
        parseExpression("myVariable");
    }

    @Benchmark
    public void benchmarkBooleanLiteral() {
        parseExpression("true");
    }

    @Benchmark
    public void benchmarkStringLiteral() {
        parseExpression("\"hello world\"");
    }

    @Benchmark
    public void benchmarkSimpleAddition() {
        parseExpression("a + b");
    }

    @Benchmark
    public void benchmarkSimpleMultiplication() {
        parseExpression("x * y");
    }

    @Benchmark
    public void benchmarkEqualityComparison() {
        parseExpression("a == b");
    }

    @Benchmark
    public void benchmarkLogicalAnd() {
        parseExpression("flag && other");
    }

    @Benchmark
    public void benchmarkArithmeticPrecedence() {
        parseExpression("a + b * c - d / e");
    }

    @Benchmark
    public void benchmarkArithmeticPrecedenceVariant() {
        parseExpression("x * y + z - w / q");
    }

    @Benchmark
    public void benchmarkParenthesizedArithmetic() {
        parseExpression("(a + b) * (c - d)");
    }

    @Benchmark
    public void benchmarkNestedParentheses() {
        parseExpression("((a + b) * (c - d)) / ((e + f) * (g - h))");
    }

    @Benchmark
    public void benchmarkMixedOperations() {
        parseExpression("(x + y) * (z - w) + (a / b) - (c % d)");
    }

    @Benchmark
    public void benchmarkUnaryMinus() {
        parseExpression("-a");
    }

    @Benchmark
    public void benchmarkUnaryNot() {
        parseExpression("!flag");
    }

    @Benchmark
    public void benchmarkPrefixIncrement() {
        parseExpression("++counter");
    }

    @Benchmark
    public void benchmarkPostfixDecrement() {
        parseExpression("value--");
    }

    @Benchmark
    public void benchmarkMixedUnaryOperations() {
        parseExpression("-x + !y + ++z + w--");
    }

    @Benchmark
    public void benchmarkComparisonWithLogical() {
        parseExpression("a > b && c < d");
    }

    @Benchmark
    public void benchmarkEqualityWithLogicalOr() {
        parseExpression("x == y || z != w");
    }

    @Benchmark
    public void benchmarkComplexLogical() {
        parseExpression("a > b && c < d || e == f && g != h");
    }

    @Benchmark
    public void benchmarkComplexArithmetic() {
        parseExpression("(a + b) * (c - d) + e / f - g % h");
    }

    @Benchmark
    public void benchmarkNestedLogical() {
        parseExpression("((x || y) && (z || w)) && ((a < b) || (c > d))");
    }

    @Benchmark
    public void benchmarkLargeNestedExpression() {
        parseExpression(buildLargeExpression());
    }

    /**
     * Helper method to parse an expression.
     */
    private void parseExpression(String expression) {
        try {
            StringReader reader = new StringReader(expression);
            SourceParser parser = new SourceParser(reader, SourceType.Kotlin);
            parser.parseExpression();
        } catch (Exception e) {
            // Ignore exceptions for benchmarking purposes
        }
    }

    private static String buildLargeExpression() {
        return "(((a + b) * (c - d)) / ((e + f) * (g - h))) + " +
               "(((i | j) & (k ^ l)) << ((m + n) - (o * p))) - " +
               "(((q == r) && (s != t)) || ((u < v) && (w > x))) * " +
               "(((y % z) + (aa - bb)) / ((cc * dd) + (ee / ff)))";
    }

    // Individual performance test methods for each expression type

    @Test
    public void testPerformance_IntegerLiteral() throws RunnerException {
        runSingleBenchmarkTest("benchmarkIntegerLiteral", "Integer literal");
    }

    @Test
    public void testPerformance_SimpleVariable() throws RunnerException {
        runSingleBenchmarkTest("benchmarkSimpleVariable", "Simple variable");
    }

    @Test
    public void testPerformance_BooleanLiteral() throws RunnerException {
        runSingleBenchmarkTest("benchmarkBooleanLiteral", "Boolean literal");
    }

    @Test
    public void testPerformance_StringLiteral() throws RunnerException {
        runSingleBenchmarkTest("benchmarkStringLiteral", "String literal");
    }

    @Test
    public void testPerformance_SimpleAddition() throws RunnerException {
        runSingleBenchmarkTest("benchmarkSimpleAddition", "Simple addition");
    }

    @Test
    public void testPerformance_SimpleMultiplication() throws RunnerException {
        runSingleBenchmarkTest("benchmarkSimpleMultiplication", "Simple multiplication");
    }

    @Test
    public void testPerformance_EqualityComparison() throws RunnerException {
        runSingleBenchmarkTest("benchmarkEqualityComparison", "Equality comparison");
    }

    @Test
    public void testPerformance_LogicalAnd() throws RunnerException {
        runSingleBenchmarkTest("benchmarkLogicalAnd", "Logical AND");
    }

    @Test
    public void testPerformance_ArithmeticPrecedence() throws RunnerException {
        runSingleBenchmarkTest("benchmarkArithmeticPrecedence", "Arithmetic precedence");
    }

    @Test
    public void testPerformance_ArithmeticPrecedenceVariant() throws RunnerException {
        runSingleBenchmarkTest("benchmarkArithmeticPrecedenceVariant", "Arithmetic precedence variant");
    }

    @Test
    public void testPerformance_ParenthesizedArithmetic() throws RunnerException {
        runSingleBenchmarkTest("benchmarkParenthesizedArithmetic", "Parenthesized arithmetic");
    }

    @Test
    public void testPerformance_NestedParentheses() throws RunnerException {
        runSingleBenchmarkTest("benchmarkNestedParentheses", "Nested parentheses");
    }

    @Test
    public void testPerformance_MixedOperations() throws RunnerException {
        runSingleBenchmarkTest("benchmarkMixedOperations", "Mixed operations");
    }

    @Test
    public void testPerformance_UnaryMinus() throws RunnerException {
        runSingleBenchmarkTest("benchmarkUnaryMinus", "Unary minus");
    }

    @Test
    public void testPerformance_UnaryNot() throws RunnerException {
        runSingleBenchmarkTest("benchmarkUnaryNot", "Unary not");
    }

    @Test
    public void testPerformance_PrefixIncrement() throws RunnerException {
        runSingleBenchmarkTest("benchmarkPrefixIncrement", "Prefix increment");
    }

    @Test
    public void testPerformance_PostfixDecrement() throws RunnerException {
        runSingleBenchmarkTest("benchmarkPostfixDecrement", "Postfix decrement");
    }

    @Test
    public void testPerformance_MixedUnaryOperations() throws RunnerException {
        runSingleBenchmarkTest("benchmarkMixedUnaryOperations", "Mixed unary operations");
    }

    @Test
    public void testPerformance_ComparisonWithLogical() throws RunnerException {
        runSingleBenchmarkTest("benchmarkComparisonWithLogical", "Comparison with logical");
    }

    @Test
    public void testPerformance_EqualityWithLogicalOr() throws RunnerException {
        runSingleBenchmarkTest("benchmarkEqualityWithLogicalOr", "Equality with logical OR");
    }

    @Test
    public void testPerformance_ComplexLogical() throws RunnerException {
        runSingleBenchmarkTest("benchmarkComplexLogical", "Complex logical");
    }

    @Test
    public void testPerformance_ComplexArithmetic() throws RunnerException {
        runSingleBenchmarkTest("benchmarkComplexArithmetic", "Complex arithmetic");
    }

    @Test
    public void testPerformance_NestedLogical() throws RunnerException {
        runSingleBenchmarkTest("benchmarkNestedLogical", "Nested logical");
    }

    @Test
    public void testPerformance_LargeNestedExpression() throws RunnerException {
        runSingleBenchmarkTest("benchmarkLargeNestedExpression", "Large nested expression");
    }

    /**
     * Helper method to run a single benchmark and verify performance target.
     */
    private void runSingleBenchmarkTest(String benchmarkMethod, String testName) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SimplePerformanceTest.class.getSimpleName() + "\\." + benchmarkMethod)
                .forks(1)
                .warmupIterations(3)
                .measurementIterations(5)
                .build();

        Collection<RunResult> results = new Runner(opt).run();

        // Extract results for both parser configurations
        double monolithicTime = 0;
        double prattTime = 0;

        for (RunResult result : results) {
            boolean isPratt = result.getParams().getParam("usePrattParser").equals("true");
            double score = result.getPrimaryResult().getScore();

            if (isPratt) {
                prattTime = score;
            } else {
                monolithicTime = score;
            }
        }

        double ratio = prattTime / monolithicTime;

        System.out.println(String.format("\n=== %s Performance ===", testName));
        System.out.println(String.format("Monolithic: %.2f ns/op", monolithicTime));
        System.out.println(String.format("Pratt:      %.2f ns/op", prattTime));
        System.out.println(String.format("Ratio:      %.2f (Pratt/Monolithic)", ratio));
        System.out.println("Target:     ≤ " + PERFORMANCE_TARGET_RATIO);
        System.out.println("Status:     " + (ratio <= PERFORMANCE_TARGET_RATIO ? "✅ PASS" : "❌ FAIL"));

        assertTrue(
            String.format("Pratt parser performance for '%s' should be within %.0f%% of monolithic parser. " +
                         "Actual ratio: %.2f (target: ≤ %.2f)",
                         testName, (PERFORMANCE_TARGET_RATIO - 1) * 100, ratio, PERFORMANCE_TARGET_RATIO),
            ratio <= PERFORMANCE_TARGET_RATIO
        );
    }

    /**
     * Main method to run benchmarks directly.
     * Can be used for standalone benchmark execution outside of JUnit.
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SimplePerformanceTest.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }
}
