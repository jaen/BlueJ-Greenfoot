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
package bluej.parser.pratt.integration.benchmark;

import bluej.parser.BenchmarkTest;
import bluej.parser.InitConfig;
import bluej.parser.pratt.integration.benchmark.adapters.*;
import bluej.parser.pratt.integration.benchmark.corpus.TestCorpusGenerator;
import bluej.parser.pratt.integration.benchmark.reporting.BenchmarkReport;
import bluej.parser.pratt.integration.benchmark.reporting.BenchmarkReporter;
import bluej.parser.pratt.integration.benchmark.reporting.StrategyRecommendation;
import bluej.parser.pratt.integration.benchmark.reporting.StrategyResult;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * JMH-based comprehensive benchmark comparing all 7 parser integration strategy adapters.
 *
 * <p>This benchmark measures parsing performance, memory overhead, and scalability across
 * different strategy implementations to provide actionable recommendations for BlueJ
 * parser integration choices.</p>
 *
 * <p>Each strategy adapter has individual benchmark methods for easy test filtering and
 * parallel execution, following the pattern from SimplePerformanceTest.</p>
 *
 * @author BlueJ Team
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-Xms2G", "-Xmx4G"})
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Category(BenchmarkTest.class)
public class JMHStrategyComparisonBenchmark {

    // Shared test corpus generator
    private TestCorpusGenerator corpusGenerator;
    private List<String> simpleTestCases;
    private List<String> mediumTestCases;
    private List<String> complexTestCases;

    static {
        InitConfig.init();
    }

    @Setup(Level.Trial)
    public void setUp() {
        corpusGenerator = new TestCorpusGenerator();
        
        // Pre-generate test cases for all complexity levels
        simpleTestCases = corpusGenerator.generateSimpleExpressions(200);
        mediumTestCases = corpusGenerator.generateMediumComplexityCode(100);
        complexTestCases = corpusGenerator.generateComplexNestedStructures(50);
        
        System.out.println("Test corpus initialized: " +
                          simpleTestCases.size() + " simple, " +
                          mediumTestCases.size() + " medium, " +
                          complexTestCases.size() + " complex cases");
    }

    // =================================
    // DirectCallbackStrategy Benchmarks
    // =================================
    
    @Benchmark
    public void benchmarkDirectCallbackStrategy_Simple(Blackhole bh) {
        benchmarkStrategy(new DirectCallbackStrategyAdapter(), simpleTestCases, bh);
    }
    
    @Benchmark
    public void benchmarkDirectCallbackStrategy_Medium(Blackhole bh) {
        benchmarkStrategy(new DirectCallbackStrategyAdapter(), mediumTestCases, bh);
    }
    
    @Benchmark
    public void benchmarkDirectCallbackStrategy_Complex(Blackhole bh) {
        benchmarkStrategy(new DirectCallbackStrategyAdapter(), complexTestCases, bh);
    }

    // =================================
    // ASTVisitorPattern Benchmarks
    // =================================
    
    @Benchmark
    public void benchmarkASTVisitorPattern_Simple(Blackhole bh) {
        benchmarkStrategy(new ASTVisitorPatternAdapter(), simpleTestCases, bh);
    }
    
    @Benchmark
    public void benchmarkASTVisitorPattern_Medium(Blackhole bh) {
        benchmarkStrategy(new ASTVisitorPatternAdapter(), mediumTestCases, bh);
    }
    
    @Benchmark
    public void benchmarkASTVisitorPattern_Complex(Blackhole bh) {
        benchmarkStrategy(new ASTVisitorPatternAdapter(), complexTestCases, bh);
    }

    // =================================
    // HybridResultPattern Benchmarks
    // =================================
    
    @Benchmark
    public void benchmarkHybridResultPattern_Simple(Blackhole bh) {
        benchmarkStrategy(new HybridResultPatternAdapter(), simpleTestCases, bh);
    }
    
    @Benchmark
    public void benchmarkHybridResultPattern_Medium(Blackhole bh) {
        benchmarkStrategy(new HybridResultPatternAdapter(), mediumTestCases, bh);
    }
    
    @Benchmark
    public void benchmarkHybridResultPattern_Complex(Blackhole bh) {
        benchmarkStrategy(new HybridResultPatternAdapter(), complexTestCases, bh);
    }

    // =================================
    // K2ParserIntegration Benchmarks
    // =================================
    
    @Benchmark
    public void benchmarkK2ParserIntegration_Simple(Blackhole bh) {
        benchmarkStrategy(new K2ParserIntegrationAdapter(), simpleTestCases, bh);
    }
    
    @Benchmark
    public void benchmarkK2ParserIntegration_Medium(Blackhole bh) {
        benchmarkStrategy(new K2ParserIntegrationAdapter(), mediumTestCases, bh);
    }
    
    @Benchmark
    public void benchmarkK2ParserIntegration_Complex(Blackhole bh) {
        benchmarkStrategy(new K2ParserIntegrationAdapter(), complexTestCases, bh);
    }

    // =================================
    // ErrorRecoveryIntegration Benchmarks
    // =================================
    
    @Benchmark
    public void benchmarkErrorRecoveryIntegration_Simple(Blackhole bh) {
        benchmarkStrategy(new ErrorRecoveryIntegrationAdapter(), simpleTestCases, bh);
    }
    
    @Benchmark
    public void benchmarkErrorRecoveryIntegration_Medium(Blackhole bh) {
        benchmarkStrategy(new ErrorRecoveryIntegrationAdapter(), mediumTestCases, bh);
    }
    
    @Benchmark
    public void benchmarkErrorRecoveryIntegration_Complex(Blackhole bh) {
        benchmarkStrategy(new ErrorRecoveryIntegrationAdapter(), complexTestCases, bh);
    }

    // =================================
    // CommonParsingScenarios Benchmarks
    // =================================
    
    @Benchmark
    public void benchmarkCommonParsingScenarios_Simple(Blackhole bh) {
        benchmarkStrategy(new CommonParsingScenariosAdapter(), simpleTestCases, bh);
    }
    
    @Benchmark
    public void benchmarkCommonParsingScenarios_Medium(Blackhole bh) {
        benchmarkStrategy(new CommonParsingScenariosAdapter(), mediumTestCases, bh);
    }
    
    @Benchmark
    public void benchmarkCommonParsingScenarios_Complex(Blackhole bh) {
        benchmarkStrategy(new CommonParsingScenariosAdapter(), complexTestCases, bh);
    }

    // =================================
    // LazyASTTransformation Benchmarks
    // =================================
    
    @Benchmark
    public void benchmarkLazyASTTransformation_Simple(Blackhole bh) {
        benchmarkStrategy(new LazyASTTransformationAdapter(), simpleTestCases, bh);
    }
    
    @Benchmark
    public void benchmarkLazyASTTransformation_Medium(Blackhole bh) {
        benchmarkStrategy(new LazyASTTransformationAdapter(), mediumTestCases, bh);
    }
    
    @Benchmark
    public void benchmarkLazyASTTransformation_Complex(Blackhole bh) {
        benchmarkStrategy(new LazyASTTransformationAdapter(), complexTestCases, bh);
    }

    /**
     * Helper method to run a specific strategy with given test cases.
     */
    private void benchmarkStrategy(StrategyAdapter strategy, List<String> testCases, Blackhole bh) {
        try {
            for (String testCase : testCases) {
                try {
                    Object result = strategy.parseWithStrategy(testCase);
                    bh.consume(result);
                } catch (Exception e) {
                    bh.consume(e.getMessage());
                }
            }
        } finally {
            // Ensure cleanup
            strategy.cleanup();
        }
    }

    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    // =================================
    // Individual Test Methods (for easy filtering)
    // =================================
    
    @Test
    public void testDirectCallbackStrategy_Simple() throws RunnerException {
        runSingleBenchmarkTest("benchmarkDirectCallbackStrategy_Simple", "DirectCallbackStrategy - Simple");
    }
    
    @Test
    public void testDirectCallbackStrategy_Medium() throws RunnerException {
        runSingleBenchmarkTest("benchmarkDirectCallbackStrategy_Medium", "DirectCallbackStrategy - Medium");
    }
    
    @Test
    public void testDirectCallbackStrategy_Complex() throws RunnerException {
        runSingleBenchmarkTest("benchmarkDirectCallbackStrategy_Complex", "DirectCallbackStrategy - Complex");
    }
    
    @Test
    public void testASTVisitorPattern_Simple() throws RunnerException {
        runSingleBenchmarkTest("benchmarkASTVisitorPattern_Simple", "ASTVisitorPattern - Simple");
    }
    
    @Test
    public void testASTVisitorPattern_Medium() throws RunnerException {
        runSingleBenchmarkTest("benchmarkASTVisitorPattern_Medium", "ASTVisitorPattern - Medium");
    }
    
    @Test
    public void testASTVisitorPattern_Complex() throws RunnerException {
        runSingleBenchmarkTest("benchmarkASTVisitorPattern_Complex", "ASTVisitorPattern - Complex");
    }
    
    @Test
    public void testHybridResultPattern_Simple() throws RunnerException {
        runSingleBenchmarkTest("benchmarkHybridResultPattern_Simple", "HybridResultPattern - Simple");
    }
    
    @Test
    public void testHybridResultPattern_Medium() throws RunnerException {
        runSingleBenchmarkTest("benchmarkHybridResultPattern_Medium", "HybridResultPattern - Medium");
    }
    
    @Test
    public void testHybridResultPattern_Complex() throws RunnerException {
        runSingleBenchmarkTest("benchmarkHybridResultPattern_Complex", "HybridResultPattern - Complex");
    }
    
    @Test
    public void testK2ParserIntegration_Simple() throws RunnerException {
        runSingleBenchmarkTest("benchmarkK2ParserIntegration_Simple", "K2ParserIntegration - Simple");
    }
    
    @Test
    public void testK2ParserIntegration_Medium() throws RunnerException {
        runSingleBenchmarkTest("benchmarkK2ParserIntegration_Medium", "K2ParserIntegration - Medium");
    }
    
    @Test
    public void testK2ParserIntegration_Complex() throws RunnerException {
        runSingleBenchmarkTest("benchmarkK2ParserIntegration_Complex", "K2ParserIntegration - Complex");
    }
    
    @Test
    public void testErrorRecoveryIntegration_Simple() throws RunnerException {
        runSingleBenchmarkTest("benchmarkErrorRecoveryIntegration_Simple", "ErrorRecoveryIntegration - Simple");
    }
    
    @Test
    public void testErrorRecoveryIntegration_Medium() throws RunnerException {
        runSingleBenchmarkTest("benchmarkErrorRecoveryIntegration_Medium", "ErrorRecoveryIntegration - Medium");
    }
    
    @Test
    public void testErrorRecoveryIntegration_Complex() throws RunnerException {
        runSingleBenchmarkTest("benchmarkErrorRecoveryIntegration_Complex", "ErrorRecoveryIntegration - Complex");
    }
    
    @Test
    public void testCommonParsingScenarios_Simple() throws RunnerException {
        runSingleBenchmarkTest("benchmarkCommonParsingScenarios_Simple", "CommonParsingScenarios - Simple");
    }
    
    @Test
    public void testCommonParsingScenarios_Medium() throws RunnerException {
        runSingleBenchmarkTest("benchmarkCommonParsingScenarios_Medium", "CommonParsingScenarios - Medium");
    }
    
    @Test
    public void testCommonParsingScenarios_Complex() throws RunnerException {
        runSingleBenchmarkTest("benchmarkCommonParsingScenarios_Complex", "CommonParsingScenarios - Complex");
    }
    
    @Test
    public void testLazyASTTransformation_Simple() throws RunnerException {
        runSingleBenchmarkTest("benchmarkLazyASTTransformation_Simple", "LazyASTTransformation - Simple");
    }
    
    @Test
    public void testLazyASTTransformation_Medium() throws RunnerException {
        runSingleBenchmarkTest("benchmarkLazyASTTransformation_Medium", "LazyASTTransformation - Medium");
    }
    
    @Test
    public void testLazyASTTransformation_Complex() throws RunnerException {
        runSingleBenchmarkTest("benchmarkLazyASTTransformation_Complex", "LazyASTTransformation - Complex");
    }

    /**
     * Comprehensive test that runs all strategies and generates a complete report.
     */
    @Test
    public void testComprehensiveStrategyComparison() throws RunnerException {
        System.out.println("=== Starting Comprehensive Strategy Comparison ===");
        
        // Run all benchmarks and collect results
        Map<String, Collection<RunResult>> allResults = new HashMap<>();
        String[] strategies = {"DirectCallbackStrategy", "ASTVisitorPattern", "HybridResultPattern",
                              "K2ParserIntegration", "ErrorRecoveryIntegration", "CommonParsingScenarios",
                              "LazyASTTransformation"};
        String[] complexities = {"Simple", "Medium", "Complex"};
        
        for (String strategy : strategies) {
            for (String complexity : complexities) {
                String methodName = "benchmark" + strategy + "_" + complexity;
                String testName = strategy + " - " + complexity;
                
                System.out.println("Running " + testName + "...");
                
                Options opt = new OptionsBuilder()
                        .include(JMHStrategyComparisonBenchmark.class.getSimpleName() + "\\." + methodName)
                        .forks(1)
                        .warmupIterations(5)
                        .measurementIterations(10)
                        .build();

                Collection<RunResult> results = new Runner(opt).run();
                allResults.put(testName, results);
            }
        }
        
        // Process all results and generate comprehensive report
        BenchmarkReport report = processAllResults(allResults);
        BenchmarkReporter reporter = BenchmarkReporter.forConsole();
        
        // Generate all report formats
        reporter.generateConsoleReport(report);
        try {
            reporter.generateMarkdownReport(report, "strategy-comparison-results.md");
            reporter.generateCsvReport(report, "strategy-comparison-results.csv");
            reporter.generateJsonReport(report, "strategy-comparison-results.json");
        } catch (java.io.IOException e) {
            System.err.println("Failed to generate report files: " + e.getMessage());
        }
        
        System.out.println("\n=== Comprehensive Benchmark Complete ===");
        System.out.println("Reports generated:");
        System.out.println("- Console output above");
        System.out.println("- Markdown: strategy-comparison-results.md");
        System.out.println("- CSV: strategy-comparison-results.csv");
        System.out.println("- JSON: strategy-comparison-results.json");
    }

    /**
     * Helper method to run a single benchmark and display results.
     */
    private void runSingleBenchmarkTest(String benchmarkMethod, String testName) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(JMHStrategyComparisonBenchmark.class.getSimpleName() + "\\." + benchmarkMethod)
                .forks(1)
                .warmupIterations(5)
                .measurementIterations(10)
                .build();

        Collection<RunResult> results = new Runner(opt).run();

        // Display results
        for (RunResult result : results) {
            double score = result.getPrimaryResult().getScore();
            String unit = result.getPrimaryResult().getScoreUnit();
            
            System.out.println(String.format("\n=== %s Performance ===", testName));
            System.out.println(String.format("Average Time: %.2f %s", score, unit));
            System.out.println(String.format("Std Deviation: %.2f", result.getPrimaryResult().getStatistics().getStandardDeviation()));
            System.out.println(String.format("Iterations: %d", result.getPrimaryResult().getStatistics().getN()));
        }
    }

    private BenchmarkReport processAllResults(Map<String, Collection<RunResult>> allResults) {
        BenchmarkReport report = new BenchmarkReport();
        Map<String, List<Double>> strategyScores = new HashMap<>();
        
        // Collect scores by strategy
        for (Map.Entry<String, Collection<RunResult>> entry : allResults.entrySet()) {
            String testName = entry.getKey();
            String strategyName = testName.split(" - ")[0]; // Extract strategy name
            
            for (RunResult result : entry.getValue()) {
                double score = result.getPrimaryResult().getScore();
                strategyScores.computeIfAbsent(strategyName, k -> new ArrayList<>()).add(score);
            }
        }
        
        // Create strategy results
        for (Map.Entry<String, List<Double>> entry : strategyScores.entrySet()) {
            String strategyName = entry.getKey();
            List<Double> scores = entry.getValue();
            
            double avgScore = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double stdDev = calculateStandardDeviation(scores, avgScore);
            
            StrategyResult strategyResult = new StrategyResult();
            strategyResult.setStrategyName(strategyName);
            strategyResult.setAvgLatency(avgScore);
            strategyResult.setMemoryOverhead(stdDev); // Use this field to store standard deviation
            strategyResult.setScalabilityScore(calculateScalability(scores));
            
            report.addStrategyResult(strategyResult);
        }
        
        // Generate recommendations
        generateRecommendations(report);
        
        return report;
    }
    
    private double calculateStandardDeviation(List<Double> scores, double mean) {
        if (scores.size() <= 1) return 0.0;
        
        double variance = scores.stream()
                .mapToDouble(score -> Math.pow(score - mean, 2))
                .average()
                .orElse(0.0);
        
        return Math.sqrt(variance);
    }
    
    private double calculateScalability(List<Double> scores) {
        if (scores.size() < 3) return 1.0;
        
        // Simple scalability heuristic: ratio of best to worst performance
        double min = scores.stream().mapToDouble(Double::doubleValue).min().orElse(1.0);
        double max = scores.stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        
        return min / Math.max(max, 0.001); // Higher is better (closer to 1.0 means more consistent)
    }


    private void generateRecommendations(BenchmarkReport report) {
        List<StrategyResult> results = report.getStrategyResults();
        
        // Find best performers in different categories
        StrategyResult bestLatency = results.stream()
            .min(Comparator.comparing(StrategyResult::getAvgLatency))
            .orElse(null);
            
        StrategyResult bestScalability = results.stream()
            .max(Comparator.comparing(StrategyResult::getScalabilityScore))
            .orElse(null);

        StrategyResult mostConsistent = results.stream()
            .min(Comparator.comparing(StrategyResult::getMemoryOverhead)) // Using std dev stored in memoryOverhead
            .orElse(null);

        // Add recommendations to report
        if (bestLatency != null) {
            report.addRecommendation(new StrategyRecommendation(
                StrategyRecommendation.RecommendationCategory.PERFORMANCE,
                bestLatency.getStrategyName(),
                String.format("Best average latency: %.2f ns/op", bestLatency.getAvgLatency()),
                StrategyRecommendation.UseCase.PERFORMANCE_CRITICAL
            ));
        }
        
        if (bestScalability != null) {
            report.addRecommendation(new StrategyRecommendation(
                StrategyRecommendation.RecommendationCategory.SCALABILITY,
                bestScalability.getStrategyName(),
                String.format("Best scalability score: %.2f", bestScalability.getScalabilityScore()),
                StrategyRecommendation.UseCase.LARGE_SCALE
            ));
        }
        
        if (mostConsistent != null) {
            report.addRecommendation(new StrategyRecommendation(
                StrategyRecommendation.RecommendationCategory.PERFORMANCE,
                mostConsistent.getStrategyName(),
                String.format("Most consistent performance (std dev: %.2f ns)",
                             mostConsistent.getMemoryOverhead()),
                StrategyRecommendation.UseCase.GENERAL_PURPOSE
            ));
        }
    }

    /**
     * Main method for standalone JMH execution.
     */
    public static void main(String[] args) throws RunnerException {
        String strategy = args.length > 0 ? args[0] : "all";
        String complexity = args.length > 1 ? args[1] : "Simple";
        
        Options opt;
        if ("all".equals(strategy)) {
            opt = new OptionsBuilder()
                    .include(JMHStrategyComparisonBenchmark.class.getSimpleName())
                    .build();
        } else {
            // Run specific strategy and complexity
            String methodName = "benchmark" + strategy + "_" + complexity;
            opt = new OptionsBuilder()
                    .include(JMHStrategyComparisonBenchmark.class.getSimpleName() + "\\." + methodName)
                    .build();
        }
        
        Collection<RunResult> results = new Runner(opt).run();
        
        System.out.println(String.format("\n=== Benchmark Complete: %d results generated ===", results.size()));
    }
}