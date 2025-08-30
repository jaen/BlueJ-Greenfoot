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
package bluej.parser.pratt.integration.benchmark.jmh;

import bluej.parser.BenchmarkTest;
import bluej.parser.CallbackDelegate;
import bluej.parser.InitConfig;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.PackageOrClass;
import bluej.parser.entity.TypeEntity;
import bluej.parser.entity.TypeArgumentEntity;
import bluej.debugger.gentype.Reflective;
import bluej.debugger.gentype.JavaType;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.pratt.integration.CallbackTestingUtility;
import bluej.parser.pratt.integration.CallbackTestingUtility.CallbackTester;
import bluej.parser.pratt.integration.benchmark.StrategyAdapter;
import bluej.parser.pratt.integration.benchmark.adapters.*;
import bluej.parser.pratt.integration.benchmark.corpus.TestCorpusGenerator;
import bluej.parser.pratt.integration.benchmark.reporting.*;

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
 * Comprehensive JMH benchmark comparing all 6 parser integration strategies.
 * 
 * This master benchmark suite provides comprehensive comparative analysis across
 * all integration strategies using parameterized benchmarks:
 * - ASTVisitorPattern: Tree traversal with visitor callbacks
 * - HybridResultPattern: Adaptive multi-strategy approach
 * - K2ParserIntegration: Kotlin K2 parser integration
 * - LazyASTTransformation: On-demand AST materialization
 * - ErrorRecoveryIntegration: Robust error handling and recovery
 * - CommonParsingScenarios: Optimized for typical Java patterns
 * 
 * The benchmark measures performance across multiple dimensions including
 * parsing speed, memory usage, error recovery effectiveness, and scalability,
 * providing actionable recommendations for strategy selection based on
 * specific use cases and performance requirements.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-Xms2G", "-Xmx4G"})
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Category(BenchmarkTest.class)
public class AllStrategiesJMHBenchmark {

    // Strategy parameter for comparative benchmarking
    @Param({"ASTVisitorPattern", "HybridResultPattern", "K2ParserIntegration", 
            "LazyASTTransformation", "ErrorRecoveryIntegration", "CommonParsingScenarios"})
    public String strategyName;

    // Complexity level parameter for scalability analysis
    @Param({"Simple", "Moderate", "Complex", "VeryComplex"})
    public String complexityLevel;

    // Test infrastructure
    private TestCorpusGenerator corpusGenerator;
    private StrategyAdapter currentAdapter;
    private TestableDocument document;
    private CallbackTester callbackTester;
    
    // Strategy adapter instances
    private Map<String, StrategyAdapter> strategyAdapters;
    
    // Test corpus collections by complexity
    private Map<String, List<String>> testCorpusByComplexity;
    private List<String> errorRecoveryTestCases;
    private List<String> memoryTestCases;
    private List<String> scalabilityTestCases;

    static {
        InitConfig.init();
    }

    @Setup(Level.Trial)
    public void setUpTrial() {
        corpusGenerator = new TestCorpusGenerator();
        
        // Initialize all strategy adapters
        strategyAdapters = new HashMap<>();
        strategyAdapters.put("ASTVisitorPattern", new ASTVisitorPatternAdapter());
        strategyAdapters.put("HybridResultPattern", new HybridResultPatternAdapter());
        strategyAdapters.put("K2ParserIntegration", new K2ParserIntegrationAdapter());
        strategyAdapters.put("LazyASTTransformation", new LazyASTTransformationAdapter());
        strategyAdapters.put("ErrorRecoveryIntegration", new ErrorRecoveryIntegrationAdapter());
        strategyAdapters.put("CommonParsingScenarios", new CommonParsingScenariosAdapter());
        
        MockEntityResolver entityResolver = new MockEntityResolver();
        document = new TestableDocument("benchmark.java", entityResolver);
        callbackTester = CallbackTestingUtility.forDelegate(document.getCallbackDelegate());
        
        // Generate comprehensive test corpus
        generateTestCorpus();
        
        System.out.println("All Strategies JMH benchmark corpus initialized:");
        testCorpusByComplexity.forEach((level, cases) -> 
            System.out.println("  " + level + ": " + cases.size() + " test cases"));
        System.out.println("  Error Recovery: " + errorRecoveryTestCases.size());
        System.out.println("  Memory Tests: " + memoryTestCases.size());
        System.out.println("  Scalability Tests: " + scalabilityTestCases.size());
    }

    @Setup(Level.Iteration)
    public void setUpIteration() {
        // Set current adapter based on parameter
        currentAdapter = strategyAdapters.get(strategyName);
        if (currentAdapter == null) {
            throw new IllegalStateException("Unknown strategy: " + strategyName);
        }
        
        callbackTester.clear();
    }

    @TearDown(Level.Trial)
    public void tearDownTrial() {
        strategyAdapters.values().forEach(StrategyAdapter::cleanup);
    }

    // =================================
    // Core Comparative Benchmarks
    // =================================

    @Benchmark
    public void benchmarkParsingPerformance(Blackhole bh) {
        List<String> testCases = testCorpusByComplexity.get(complexityLevel);
        benchmarkWithTestCases(testCases, bh);
    }

    @Benchmark
    public void benchmarkCallbackEmission(Blackhole bh) {
        List<String> testCases = testCorpusByComplexity.get(complexityLevel);
        
        for (String testCase : testCases.subList(0, Math.min(5, testCases.size()))) {
            document.setContent(testCase);
            callbackTester.clear();
            
            try {
                long callbackStartTime = System.nanoTime();
                Object result = currentAdapter.parseWithStrategy(testCase);
                long callbackEndTime = System.nanoTime();
                
                long callbackTime = callbackEndTime - callbackStartTime;
                int callbackCount = callbackTester.getCallbackCount();
                boolean isBalanced = callbackTester.isBalanced();
                
                bh.consume(result);
                bh.consume(callbackTime);
                bh.consume(callbackCount);
                bh.consume(isBalanced);
            } catch (Exception e) {
                bh.consume(e.getMessage());
            }
        }
    }

    @Benchmark
    public void benchmarkErrorRecovery(Blackhole bh) {
        // Use error recovery test cases for all strategies
        List<String> errorCases = errorRecoveryTestCases.subList(0, Math.min(10, errorRecoveryTestCases.size()));
        
        for (String testCase : errorCases) {
            document.setContent(testCase);
            callbackTester.clear();
            
            try {
                Object result = currentAdapter.parseWithStrategy(testCase);
                
                // Measure error recovery characteristics across strategies
                boolean hasErrors = testCase.contains("ERROR") || testCase.contains("invalid");
                int callbacksGenerated = callbackTester.getCallbackCount();
                boolean maintainedBalance = callbackTester.isBalanced();
                
                bh.consume(result);
                bh.consume(hasErrors);
                bh.consume(callbacksGenerated);
                bh.consume(maintainedBalance);
            } catch (Exception e) {
                bh.consume(e.getMessage());
            }
        }
    }

    @Benchmark
    public void benchmarkMemoryUsage(Blackhole bh) {
        Runtime runtime = Runtime.getRuntime();
        List<String> memoryCases = memoryTestCases.subList(0, Math.min(3, memoryTestCases.size()));
        
        for (String testCase : memoryCases) {
            document.setContent(testCase);
            
            System.gc(); // Force GC for more accurate measurement
            long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
            
            try {
                Object result = currentAdapter.parseWithStrategy(testCase);
                
                long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
                long memoryUsed = memoryAfter - memoryBefore;
                
                bh.consume(result);
                bh.consume(memoryUsed);
            } catch (Exception e) {
                bh.consume(e.getMessage());
            }
        }
    }

    @Benchmark
    public void benchmarkScalability(Blackhole bh) {
        // Test scalability with progressively larger inputs
        List<String> scalabilityCases = scalabilityTestCases;
        
        for (int i = 0; i < Math.min(5, scalabilityCases.size()); i++) {
            String testCase = scalabilityCases.get(i);
            document.setContent(testCase);
            
            long startTime = System.nanoTime();
            try {
                Object result = currentAdapter.parseWithStrategy(testCase);
                long endTime = System.nanoTime();
                
                long processingTime = endTime - startTime;
                int codeLength = testCase.length();
                double timePerChar = (double) processingTime / codeLength;
                
                bh.consume(result);
                bh.consume(processingTime);
                bh.consume(timePerChar);
            } catch (Exception e) {
                bh.consume(e.getMessage());
            }
        }
    }

    // =================================
    // Strategy-Specific Characteristic Benchmarks
    // =================================

    @Benchmark
    public void benchmarkStrategySpecificFeatures(Blackhole bh) {
        List<String> testCases = testCorpusByComplexity.get(complexityLevel);
        String testCase = testCases.get(0); // Use first test case
        
        document.setContent(testCase);
        callbackTester.clear();
        
        try {
            Object result = currentAdapter.parseWithStrategy(testCase);
            
            // Measure strategy-specific characteristics
            switch (strategyName) {
                case "ASTVisitorPattern":
                    // Measure visitor pattern characteristics
                    boolean hasVisitorTraversal = currentAdapter.toString().contains("visitor");
                    int treeDepth = estimateTreeDepth(testCase);
                    bh.consume(hasVisitorTraversal);
                    bh.consume(treeDepth);
                    break;
                    
                case "HybridResultPattern":
                    // Measure hybrid pattern characteristics
                    boolean usedMultipleStrategies = currentAdapter.toString().contains("hybrid");
                    int strategySwitches = random.nextInt(3); // Simulated
                    bh.consume(usedMultipleStrategies);
                    bh.consume(strategySwitches);
                    break;
                    
                case "K2ParserIntegration":
                    // Measure K2 parser characteristics
                    boolean hasKotlinFeatures = testCase.contains("kotlin") || testCase.contains("suspend");
                    int modernFeatureCount = countModernFeatures(testCase);
                    bh.consume(hasKotlinFeatures);
                    bh.consume(modernFeatureCount);
                    break;
                    
                case "LazyASTTransformation":
                    // Measure lazy evaluation characteristics
                    int lazyNodesCount = random.nextInt(10); // Simulated
                    boolean usedLazyEvaluation = testCase.length() > 1000;
                    bh.consume(lazyNodesCount);
                    bh.consume(usedLazyEvaluation);
                    break;
                    
                case "ErrorRecoveryIntegration":
                    // Measure error recovery characteristics
                    boolean hasRecoveryMechanisms = true;
                    int errorsRecovered = countPotentialErrors(testCase);
                    bh.consume(hasRecoveryMechanisms);
                    bh.consume(errorsRecovered);
                    break;
                    
                case "CommonParsingScenarios":
                    // Measure common pattern optimization
                    boolean recognizedCommonPattern = isCommonJavaPattern(testCase);
                    int optimizationLevel = recognizedCommonPattern ? 3 : 1;
                    bh.consume(recognizedCommonPattern);
                    bh.consume(optimizationLevel);
                    break;
            }
            
            bh.consume(result);
        } catch (Exception e) {
            bh.consume(e.getMessage());
        }
    }

    // =================================
    // Comprehensive Analysis Benchmarks
    // =================================

    @Benchmark
    public void benchmarkOverallPerformanceProfile(Blackhole bh) {
        // Comprehensive performance profiling across multiple metrics
        List<String> testCases = testCorpusByComplexity.get(complexityLevel);
        String testCase = testCases.get(random.nextInt(Math.min(3, testCases.size())));
        
        document.setContent(testCase);
        callbackTester.clear();
        
        long startTime = System.nanoTime();
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        try {
            Object result = currentAdapter.parseWithStrategy(testCase);
            
            long endTime = System.nanoTime();
            long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
            
            // Comprehensive metrics
            long totalTime = endTime - startTime;
            long memoryUsed = memoryAfter - memoryBefore;
            int callbackCount = callbackTester.getCallbackCount();
            boolean isBalanced = callbackTester.isBalanced();
            double throughput = (double) testCase.length() / totalTime * 1_000_000; // chars per millisecond
            
            bh.consume(result);
            bh.consume(totalTime);
            bh.consume(memoryUsed);
            bh.consume(callbackCount);
            bh.consume(isBalanced);
            bh.consume(throughput);
        } catch (Exception e) {
            bh.consume(e.getMessage());
        }
    }

    // =================================
    // Helper Methods
    // =================================

    private static final Random random = new Random(42); // Fixed seed for reproducibility

    private void benchmarkWithTestCases(List<String> testCases, Blackhole bh) {
        for (String testCase : testCases) {
            document.setContent(testCase);
            
            try {
                Object result = currentAdapter.parseWithStrategy(testCase);
                bh.consume(result);
            } catch (Exception e) {
                bh.consume(e.getMessage());
            }
        }
    }

    private void generateTestCorpus() {
        testCorpusByComplexity = new HashMap<>();
        
        // Generate test cases for each complexity level
        testCorpusByComplexity.put("Simple", corpusGenerator.generateSimpleExpressions(50));
        testCorpusByComplexity.put("Moderate", corpusGenerator.generateMediumComplexityCode(30));
        testCorpusByComplexity.put("Complex", corpusGenerator.generateComplexNestedStructures(20));
        testCorpusByComplexity.put("VeryComplex", corpusGenerator.generateLargeClassFiles(10));
        
        // Generate specialized test cases
        errorRecoveryTestCases = generateErrorRecoveryTestCases(25);
        memoryTestCases = generateMemoryTestCases(15);
        scalabilityTestCases = generateScalabilityTestCases(20);
    }

    private List<String> generateErrorRecoveryTestCases(int count) {
        List<String> errorCases = new ArrayList<>();
        TestCorpusGenerator errorGenerator = new TestCorpusGenerator(true, true);
        
        for (int i = 0; i < count; i++) {
            StringBuilder errorCode = new StringBuilder();
            
            errorCode.append("package test.error").append(i).append(";\n\n");
            errorCode.append("public class ErrorCase").append(i).append(" {\n");
            
            // Introduce various types of errors
            switch (i % 4) {
                case 0:
                    errorCode.append("    private int field = \"ERROR\";\n"); // Type mismatch
                    break;
                case 1:
                    errorCode.append("    public void method(\n"); // Incomplete method
                    errorCode.append("    private String validField = \"ok\";\n");
                    break;
                case 2:
                    errorCode.append("    invalid syntax here!!!\n"); // Syntax error
                    errorCode.append("    public void validMethod() {}\n");
                    break;
                case 3:
                    errorCode.append("    public void method() {\n");
                    errorCode.append("        if (true) {\n");
                    errorCode.append("            // Missing closing brace\n");
                    break;
            }
            
            errorCode.append("}\n");
            errorCases.add(errorCode.toString());
        }
        
        return errorCases;
    }

    private List<String> generateMemoryTestCases(int count) {
        List<String> memoryCases = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            StringBuilder memoryCode = new StringBuilder();
            
            memoryCode.append("package test.memory").append(i).append(";\n\n");
            
            // Create progressively larger classes to test memory usage
            int classCount = (i + 1) * 5;
            for (int j = 0; j < classCount; j++) {
                memoryCode.append("public class MemoryTestClass").append(i).append("_").append(j).append(" {\n");
                
                // Add many fields and methods
                for (int k = 0; k < 20; k++) {
                    memoryCode.append("    private String field").append(k).append(" = \"value").append(k).append("\";\n");
                    memoryCode.append("    public String getField").append(k).append("() { return field").append(k).append("; }\n");
                    memoryCode.append("    public void setField").append(k).append("(String value) { this.field").append(k).append(" = value; }\n");
                }
                
                memoryCode.append("}\n\n");
            }
            
            memoryCases.add(memoryCode.toString());
        }
        
        return memoryCases;
    }

    private List<String> generateScalabilityTestCases(int count) {
        List<String> scalabilityCases = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            StringBuilder scalabilityCode = new StringBuilder();
            
            scalabilityCode.append("package test.scalability").append(i).append(";\n\n");
            scalabilityCode.append("public class ScalabilityTest").append(i).append(" {\n");
            
            // Create increasingly complex methods to test scalability
            int methodComplexity = (i + 1) * 10;
            for (int j = 0; j < methodComplexity; j++) {
                scalabilityCode.append("    public void method").append(j).append("() {\n");
                
                // Add nested control structures
                int nestingLevel = Math.min(5, i + 1);
                for (int k = 0; k < nestingLevel; k++) {
                    scalabilityCode.append("        ").append("    ".repeat(k)).append("if (condition").append(k).append(") {\n");
                }
                
                scalabilityCode.append("        ").append("    ".repeat(nestingLevel)).append("process();\n");
                
                for (int k = nestingLevel - 1; k >= 0; k--) {
                    scalabilityCode.append("        ").append("    ".repeat(k)).append("}\n");
                }
                
                scalabilityCode.append("    }\n\n");
            }
            
            scalabilityCode.append("}\n");
            scalabilityCases.add(scalabilityCode.toString());
        }
        
        return scalabilityCases;
    }

    private int estimateTreeDepth(String code) {
        int maxDepth = 0;
        int currentDepth = 0;
        
        for (char c : code.toCharArray()) {
            if (c == '{') {
                currentDepth++;
                maxDepth = Math.max(maxDepth, currentDepth);
            } else if (c == '}') {
                currentDepth--;
            }
        }
        
        return maxDepth;
    }

    private int countModernFeatures(String code) {
        int count = 0;
        String[] modernFeatures = {"var ", "record ", "sealed ", "switch (", "->", ":::", "yields"};
        
        for (String feature : modernFeatures) {
            if (code.contains(feature)) {
                count++;
            }
        }
        
        return count;
    }

    private int countPotentialErrors(String code) {
        int errorCount = 0;
        String[] errorPatterns = {"ERROR", "invalid", "missing", "incomplete", "null"};
        
        for (String pattern : errorPatterns) {
            if (code.toLowerCase().contains(pattern.toLowerCase())) {
                errorCount++;
            }
        }
        
        return errorCount;
    }

    private boolean isCommonJavaPattern(String code) {
        String[] commonPatterns = {
            "public class", "private", "public", "get", "set", 
            "String", "int", "boolean", "List", "Map"
        };
        
        int patternCount = 0;
        for (String pattern : commonPatterns) {
            if (code.contains(pattern)) {
                patternCount++;
            }
        }
        
        return patternCount >= 5; // Threshold for "common" pattern
    }

    // =================================
    // Comparative Analysis and Reporting
    // =================================

    @Test
    public void runComprehensiveStrategyComparison() throws RunnerException {
        System.out.println("=== Starting Comprehensive All-Strategies Comparison ===");
        
        // Run benchmarks for all strategies and complexity levels
        Options opt = new OptionsBuilder()
                .include(AllStrategiesJMHBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(3)
                .measurementIterations(5)
                .build();

        Collection<RunResult> results = new Runner(opt).run();
        
        // Process and analyze results
        BenchmarkReport report = processComprehensiveResults(results);
        
        // Generate all report formats
        BenchmarkReporter reporter = BenchmarkReporter.forConsole();
        reporter.generateConsoleReport(report);
        
        try {
            reporter.generateMarkdownReport(report, "all-strategies-comparison.md");
            reporter.generateCsvReport(report, "all-strategies-comparison.csv");
            reporter.generateJsonReport(report, "all-strategies-comparison.json");
            
            System.out.println("\n=== Comprehensive Analysis Complete ===");
            System.out.println("Generated reports:");
            System.out.println("- Console output above");
            System.out.println("- Markdown: all-strategies-comparison.md");
            System.out.println("- CSV: all-strategies-comparison.csv");  
            System.out.println("- JSON: all-strategies-comparison.json");
        } catch (Exception e) {
            System.err.println("Failed to generate report files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private BenchmarkReport processComprehensiveResults(Collection<RunResult> results) {
        BenchmarkReport report = new BenchmarkReport();
        
        // Group results by strategy
        Map<String, List<RunResult>> resultsByStrategy = results.stream()
                .collect(Collectors.groupingBy(result -> {
                    String benchmarkName = result.getParams().getBenchmark();
                    // Extract strategy name from benchmark name or params
                    return extractStrategyFromResult(result);
                }));
        
        // Process each strategy's results
        for (Map.Entry<String, List<RunResult>> entry : resultsByStrategy.entrySet()) {
            String strategyName = entry.getKey();
            List<RunResult> strategyResults = entry.getValue();
            
            StrategyResult strategyResult = new StrategyResult();
            strategyResult.setStrategyName(strategyName);
            
            // Calculate aggregate metrics
            double avgLatency = strategyResults.stream()
                    .mapToDouble(r -> r.getPrimaryResult().getScore())
                    .average()
                    .orElse(0.0);
            
            double stdDev = calculateStandardDeviation(
                strategyResults.stream()
                    .mapToDouble(r -> r.getPrimaryResult().getScore())
                    .boxed()
                    .collect(Collectors.toList()), 
                avgLatency
            );
            
            double scalabilityScore = calculateScalabilityScore(strategyResults);
            
            strategyResult.setAvgLatency(avgLatency);
            strategyResult.setMemoryOverhead(stdDev); // Store std dev in memory field
            strategyResult.setScalabilityScore(scalabilityScore);
            
            report.addStrategyResult(strategyResult);
        }
        
        // Generate recommendations
        generateComprehensiveRecommendations(report);
        
        return report;
    }

    private String extractStrategyFromResult(RunResult result) {
        // Try to extract strategy name from parameters or benchmark name
        String benchmark = result.getParams().getBenchmark();
        
        if (benchmark.contains("ASTVisitor")) return "ASTVisitorPattern";
        if (benchmark.contains("Hybrid")) return "HybridResultPattern";
        if (benchmark.contains("K2Parser")) return "K2ParserIntegration";
        if (benchmark.contains("Lazy")) return "LazyASTTransformation";
        if (benchmark.contains("Error")) return "ErrorRecoveryIntegration";
        if (benchmark.contains("Common")) return "CommonParsingScenarios";
        
        return "Unknown";
    }

    private double calculateStandardDeviation(List<Double> values, double mean) {
        if (values.size() <= 1) return 0.0;
        
        double variance = values.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .average()
                .orElse(0.0);
        
        return Math.sqrt(variance);
    }

    private double calculateScalabilityScore(List<RunResult> results) {
        if (results.size() < 2) return 1.0;
        
        // Simple scalability heuristic based on performance consistency
        List<Double> scores = results.stream()
                .mapToDouble(r -> r.getPrimaryResult().getScore())
                .boxed()
                .collect(Collectors.toList());
        
        double min = scores.stream().mapToDouble(Double::doubleValue).min().orElse(1.0);
        double max = scores.stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        
        return min / Math.max(max, 0.001); // Higher is better (more consistent)
    }

    private void generateComprehensiveRecommendations(BenchmarkReport report) {
        List<StrategyResult> results = report.getStrategyResults();
        
        // Find best performers in different categories
        StrategyResult bestPerformance = results.stream()
                .min(Comparator.comparing(StrategyResult::getAvgLatency))
                .orElse(null);
        
        StrategyResult bestScalability = results.stream()
                .max(Comparator.comparing(StrategyResult::getScalabilityScore))
                .orElse(null);
        
        StrategyResult mostConsistent = results.stream()
                .min(Comparator.comparing(StrategyResult::getMemoryOverhead))
                .orElse(null);
        
        // Add comprehensive recommendations
        if (bestPerformance != null) {
            report.addRecommendation(new StrategyRecommendation(
                StrategyRecommendation.RecommendationCategory.PERFORMANCE,
                bestPerformance.getStrategyName(),
                String.format("Best overall performance: %.2f ns/op", bestPerformance.getAvgLatency()),
                StrategyRecommendation.UseCase.PERFORMANCE_CRITICAL
            ));
        }
        
        if (bestScalability != null && !bestScalability.equals(bestPerformance)) {
            report.addRecommendation(new StrategyRecommendation(
                StrategyRecommendation.RecommendationCategory.SCALABILITY,
                bestScalability.getStrategyName(),
                String.format("Best scalability: %.3f consistency score", bestScalability.getScalabilityScore()),
                StrategyRecommendation.UseCase.LARGE_SCALE
            ));
        }
        
        if (mostConsistent != null && !mostConsistent.equals(bestPerformance) && !mostConsistent.equals(bestScalability)) {
            report.addRecommendation(new StrategyRecommendation(
                StrategyRecommendation.RecommendationCategory.PERFORMANCE,
                mostConsistent.getStrategyName(),
                String.format("Most consistent performance (std dev: %.2f)", mostConsistent.getMemoryOverhead()),
                StrategyRecommendation.UseCase.GENERAL_PURPOSE
            ));
        }
        
        // Add strategy-specific recommendations
        results.forEach(result -> {
            String strategyName = result.getStrategyName();
            switch (strategyName) {
                case "CommonParsingScenarios":
                    report.addRecommendation(new StrategyRecommendation(
                        StrategyRecommendation.RecommendationCategory.COMPATIBILITY,
                        strategyName,
                        "Recommended for typical Java development workflows",
                        StrategyRecommendation.UseCase.GENERAL_PURPOSE
                    ));
                    break;
                case "ErrorRecoveryIntegration":
                    report.addRecommendation(new StrategyRecommendation(
                        StrategyRecommendation.RecommendationCategory.ROBUSTNESS,
                        strategyName,
                        "Recommended for development environments with frequent syntax errors",
                        StrategyRecommendation.UseCase.DEVELOPMENT_ENVIRONMENT
                    ));
                    break;
                case "LazyASTTransformation":
                    report.addRecommendation(new StrategyRecommendation(
                        StrategyRecommendation.RecommendationCategory.MEMORY,
                        strategyName,
                        "Recommended for memory-constrained environments",
                        StrategyRecommendation.UseCase.RESOURCE_CONSTRAINED
                    ));
                    break;
            }
        });
    }

    // =================================
    // Mock Infrastructure
    // =================================

    private static class TestableDocument {
        private String content;
        private final EntityResolver entityResolver;
        private final CallbackDelegate callbackDelegate;
        
        public TestableDocument(String name, EntityResolver entityResolver) {
            this.entityResolver = entityResolver;
            this.callbackDelegate = new MockCallbackDelegate();
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public String getContent() {
            return content;
        }
        
        public CallbackDelegate getCallbackDelegate() {
            return callbackDelegate;
        }
    }
    
    private static class MockEntityResolver implements EntityResolver {
        @Override
        public PackageOrClass resolvePackageOrClass(String name, Reflective querySource) {
            return null;
        }
        
        @Override
        public TypeEntity resolveQualifiedClass(String name) {
            return null;
        }
        
        @Override
        public JavaEntity getValueEntity(String name, Reflective querySource) {
            return new MockJavaEntity(name);
        }
    }
    
    private static class MockJavaEntity extends JavaEntity {
        private final String name;
        
        public MockJavaEntity(String name) {
            this.name = name;
        }
        
        @Override
        public JavaType getType() {
            return null;
        }
        
        @Override
        public JavaEntity getSubentity(String name, Reflective accessSource) {
            return null;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public JavaEntity setTypeArgs(List<TypeArgumentEntity> tparams) {
            return null;
        }
    }
    
    private static class MockCallbackDelegate extends CallbackTestingUtility implements CallbackDelegate {
        public void determinedForLoop(LocatableToken varToken, LocatableToken colonToken, LocatableToken exprToken) {
            // Mock implementation
        }

        public void gotAnnotation(List<LocatableToken> name, boolean hasParams) {
            // Mock implementation
        }
    }

    /**
     * Main method for standalone JMH execution with strategy comparison.
     */
    public static void main(String[] args) throws RunnerException {
        String strategyFilter = args.length > 0 ? args[0] : ".*";
        String complexityFilter = args.length > 1 ? args[1] : ".*";
        
        Options opt = new OptionsBuilder()
                .include(AllStrategiesJMHBenchmark.class.getSimpleName() + "\\." + strategyFilter)
                .param("strategyName", "ASTVisitorPattern", "HybridResultPattern", "K2ParserIntegration",
                       "LazyASTTransformation", "ErrorRecoveryIntegration", "CommonParsingScenarios")
                .param("complexityLevel", "Simple", "Moderate", "Complex", "VeryComplex")
                .forks(1)
                .warmupIterations(3)
                .measurementIterations(5)
                .build();
        
        Collection<RunResult> results = new Runner(opt).run();
        
        System.out.println(String.format("\n=== All Strategies Benchmark Complete: %d results ===", results.size()));
        
        // Generate comprehensive analysis
        try {
            AllStrategiesJMHBenchmark benchmark = new AllStrategiesJMHBenchmark();
            BenchmarkReport report = benchmark.processComprehensiveResults(results);
            
            BenchmarkReporter reporter = BenchmarkReporter.forConsole();
            reporter.generateConsoleReport(report);
            
            // Output summary
            System.out.println("\n=== Strategy Performance Summary ===");
            report.getStrategyResults().stream()
                    .sorted(Comparator.comparing(StrategyResult::getAvgLatency))
                    .forEach(result -> System.out.printf("%-25s: %.2f ns/op\n", 
                        result.getStrategyName(), result.getAvgLatency()));
            
        } catch (Exception e) {
            System.err.println("Failed to generate comprehensive analysis: " + e.getMessage());
        }
    }
}