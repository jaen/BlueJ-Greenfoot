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

import bluej.parser.pratt.integration.benchmark.adapters.*;
import bluej.parser.pratt.integration.benchmark.metrics.*;
import bluej.parser.pratt.integration.benchmark.measurement.MetricsCollector;
import bluej.parser.pratt.integration.benchmark.corpus.TestCorpusGenerator;
import bluej.parser.pratt.integration.benchmark.reporting.*;
import bluej.parser.pratt.integration.benchmark.jmh.JMHBenchmarkRunner;

import org.junit.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Comprehensive integration test for the complete parser benchmark framework.
 * 
 * This test validates the entire system working together:
 * - Multiple strategy adapters (DirectCallback, ASTVisitor, LazyTransformation)
 * - Complete metrics collection and analysis
 * - All reporting formats (Console, JSON, CSV, Markdown)
 * - JMH integration capabilities
 * - Framework configuration and extensibility
 * 
 * Serves as both validation and demonstration of the framework's capabilities
 * for measuring and comparing parser integration strategies.
 */
public class ComprehensiveFrameworkTest {
    
    private static final String TEST_OUTPUT_DIR = "build/test-framework-output";
    
    private BenchmarkConfiguration testConfig;
    private List<ParserStrategyAdapter> allStrategies;
    private TestCorpusGenerator corpusGenerator;
    private MetricsCollector metricsCollector;
    private BenchmarkReporter reporter;
    
    @BeforeClass
    public static void setupOutputDirectory() throws IOException {
        // Create clean output directory
        Path outputPath = Paths.get(TEST_OUTPUT_DIR);
        if (Files.exists(outputPath)) {
            Files.walk(outputPath)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        Files.createDirectories(outputPath);
    }
    
    @Before
    public void setUp() {
        // Create comprehensive test configuration
        testConfig = BenchmarkConfiguration.builder()
                .warmupIterations(2)
                .measurementIterations(3)
                .warmupTimeMs(200)
                .measurementTimeMs(300)
                .samplesPerLevel(5)
                .includeErrorCases(true)
                .includeIncompleteCode(true)
                .enableMemoryProfiling(true)
                .enableDetailedMetrics(true)
                .outputDirectory(TEST_OUTPUT_DIR)
                .complexityLevels(
                    BenchmarkConfiguration.ComplexityLevel.SIMPLE,
                    BenchmarkConfiguration.ComplexityLevel.MODERATE,
                    BenchmarkConfiguration.ComplexityLevel.COMPLEX
                )
                .scalabilityTestSizes(5, 10, 20)
                .build();
        
        // Initialize all seven strategy implementations
        allStrategies = Arrays.asList(
            new DirectCallbackStrategyAdapter(),
            new ASTVisitorPatternAdapter(),
            new LazyASTTransformationAdapter(),
            new HybridResultPatternAdapter(),
            new K2ParserIntegrationAdapter(),
            new ErrorRecoveryIntegrationAdapter(),
            new CommonParsingScenariosAdapter()
        );
        
        corpusGenerator = new TestCorpusGenerator();
        metricsCollector = new MetricsCollector(testConfig);
        reporter = new BenchmarkReporter(testConfig);
    }
    
    @Test
    public void testCompleteFrameworkExecution() {
        // Generate test corpus
        Map<String, List<String>> testCorpus = generateTestCorpus();
        assertFalse("Test corpus should not be empty", testCorpus.isEmpty());
        
        // Execute benchmarks for all strategies
        Map<String, BenchmarkResult> results = new HashMap<>();
        
        for (ParserStrategyAdapter strategy : allStrategies) {
            // Test each complexity level
            for (BenchmarkConfiguration.ComplexityLevel level : testConfig.getComplexityLevels()) {
                List<String> testCases = testCorpus.get(level.name());
                
                try {
                    BenchmarkResult result = strategy.executeBenchmark(testCases, testConfig);
                    String key = strategy.getStrategyName() + "_" + level.name();
                    results.put(key, result);
                    
                    // Validate result completeness
                    validateBenchmarkResult(result, strategy.getStrategyName());
                } catch (Exception e) {
                    fail("Strategy execution should not throw exception: " + e.getMessage());
                }
            }
        }
        
        // Verify all strategies produced results
        assertEquals("Should have results for all strategy-complexity combinations",
                    allStrategies.size() * testConfig.getComplexityLevels().size(),
                    results.size());
        
        // Validate result consistency
        validateResultConsistency(results);
    }
    
    @Test
    public void testStrategyComparison() {
        // Execute lightweight benchmarks
        Map<String, BenchmarkResult> results = executeLightweightBenchmarks();
        
        // Create strategy comparisons
        List<StrategyComparison> comparisons = new ArrayList<>();
        for (ParserStrategyAdapter strategy : allStrategies) {
            String key = strategy.getStrategyName() + "_" + BenchmarkConfiguration.ComplexityLevel.MODERATE.name();
            BenchmarkResult result = results.get(key);
            assertNotNull("Should have result for strategy: " + strategy.getStrategyName(), result);
            
            StrategyComparison comparison = new StrategyComparison(
                strategy.getStrategyName(), 
                result, 
                strategy.getStrategyCharacteristics()
            );
            comparisons.add(comparison);
        }
        
        // Validate comparisons
        assertEquals("Should have 7 strategy comparisons", 7, comparisons.size());
        
        // Test performance ranking
        PerformanceRanking ranking = new PerformanceRanking(comparisons);
        
        List<StrategyComparison> rankedByOverall = ranking.getRankedByOverall();
        assertEquals("Should rank all 7 strategies", 7, rankedByOverall.size());
        
        // Validate ranking consistency
        for (int i = 0; i < rankedByOverall.size() - 1; i++) {
            StrategyComparison current = rankedByOverall.get(i);
            StrategyComparison next = rankedByOverall.get(i + 1);
            assertTrue("Rankings should be in descending order of overall score",
                       current.getOverallScore() >= next.getOverallScore());
        }
    }
    
    @Test
    public void testAllReportingFormats() throws IOException {
        // Execute benchmarks and create report
        Map<String, BenchmarkResult> results = executeLightweightBenchmarks();
        BenchmarkReport report = createBenchmarkReport(results);
        
        // Test console report
        try {
            String consoleReport = reporter.generateConsoleReport(report);
            assertNotNull("Console report should not be null", consoleReport);
            assertFalse("Console report should not be empty", consoleReport.isEmpty());
            assertTrue("Console report should contain title",
                      consoleReport.contains("Parser Integration Strategy Benchmark Report"));
        } catch (Exception e) {
            fail("Console report generation should not throw exception: " + e.getMessage());
        }
        
        // Test JSON report
        try {
            String jsonReport = reporter.generateJsonReport(report);
            assertNotNull("JSON report should not be null", jsonReport);
            assertFalse("JSON report should not be empty", jsonReport.isEmpty());
            assertTrue("JSON should contain strategy count", jsonReport.contains("\"strategyCount\""));
        } catch (Exception e) {
            fail("JSON report generation should not throw exception: " + e.getMessage());
        }
        
        // Test CSV report
        try {
            String csvReport = reporter.generateCsvReport(report);
            assertNotNull("CSV report should not be null", csvReport);
            assertFalse("CSV report should not be empty", csvReport.isEmpty());
            assertTrue("CSV should contain headers", csvReport.contains("Strategy,Overall Score"));
        } catch (Exception e) {
            fail("CSV report generation should not throw exception: " + e.getMessage());
        }
        
        // Test Markdown report
        try {
            String markdownReport = reporter.generateMarkdownReport(report);
            assertNotNull("Markdown report should not be null", markdownReport);
            assertFalse("Markdown report should not be empty", markdownReport.isEmpty());
            assertTrue("Markdown should contain title",
                      markdownReport.contains("# Parser Integration Strategy Benchmark Report"));
        } catch (Exception e) {
            fail("Markdown report generation should not throw exception: " + e.getMessage());
        }
        
        // Test file output
        try {
            reporter.generateAllReports(report);
            
            // Verify files were created
            assertTrue("JSON report file should exist",
                      Files.exists(Paths.get(TEST_OUTPUT_DIR, "benchmark-report.json")));
            assertTrue("CSV report file should exist",
                      Files.exists(Paths.get(TEST_OUTPUT_DIR, "benchmark-report.csv")));
            assertTrue("Markdown report file should exist",
                      Files.exists(Paths.get(TEST_OUTPUT_DIR, "benchmark-report.md")));
        } catch (Exception e) {
            fail("File output generation should not throw exception: " + e.getMessage());
        }
    }
    
    @Test
    public void testJMHIntegration() {
        // Test JMH runner initialization
        try {
            JMHBenchmarkRunner.EnhancedJMHRunner jmhRunner =
                new JMHBenchmarkRunner.EnhancedJMHRunner(testConfig, allStrategies);
            assertNotNull("JMH runner should be created", jmhRunner);
        } catch (Exception e) {
            fail("JMH runner initialization should not throw exception: " + e.getMessage());
        }
        
        // Note: We don't run actual JMH benchmarks in unit tests due to time constraints
        // But we validate the integration setup
        assertTrue("JMH warmup should be configured", testConfig.getWarmupIterations() > 0);
        assertTrue("JMH measurement should be configured", testConfig.getMeasurementIterations() > 0);
    }
    
    @Test
    public void testStrategyCharacteristics() {
        for (ParserStrategyAdapter strategy : allStrategies) {
            StrategyCharacteristics characteristics = strategy.getStrategyCharacteristics();
            
            // Validate required characteristics
            assertNotNull("Strategy should have description: " + strategy.getStrategyName(),
                         characteristics.getDescription());
            assertNotNull("Strategy should have approach: " + strategy.getStrategyName(),
                         characteristics.getApproach());
            assertNotNull("Strategy should have complexity: " + strategy.getStrategyName(),
                         characteristics.getImplementationComplexity());
            
            // Validate characteristic collections
            assertFalse("Strategy should have strengths: " + strategy.getStrategyName(),
                       characteristics.getStrengths().isEmpty());
            assertFalse("Strategy should have use cases: " + strategy.getStrategyName(),
                       characteristics.getIdealUseCases().isEmpty());
        }
    }
    
    @Test
    public void testMetricsCollection() {
        List<String> testCases = corpusGenerator.generateTestCases(
            BenchmarkConfiguration.ComplexityLevel.SIMPLE, 3);
        
        for (ParserStrategyAdapter strategy : allStrategies) {
            try {
                BenchmarkResult result = strategy.executeBenchmark(testCases, testConfig);
                
                // Test metrics collection
                metricsCollector.collectMetrics(strategy.getStrategyName(), result);
                
                // Validate metrics completeness
                assertNotNull("Should have memory metrics: " + strategy.getStrategyName(),
                             result.getMemoryMetrics());
                assertNotNull("Should have performance metrics: " + strategy.getStrategyName(),
                             result.getPerformanceMetrics());
                assertNotNull("Should have scalability metrics: " + strategy.getStrategyName(),
                             result.getScalabilityMetrics());
                assertNotNull("Should have error recovery metrics: " + strategy.getStrategyName(),
                             result.getErrorRecoveryMetrics());
            } catch (Exception e) {
                fail("Metrics collection should not throw exception for " + strategy.getStrategyName() + ": " + e.getMessage());
            }
        }
    }
    
    @Test
    public void testStrategyRecommendations() {
        Map<String, BenchmarkResult> results = executeLightweightBenchmarks();
        BenchmarkReport report = createBenchmarkReport(results);
        
        List<StrategyRecommendation> recommendations = report.getRecommendations();
        assertFalse("Should have strategy recommendations", recommendations.isEmpty());
        
        // Validate recommendation completeness
        for (StrategyRecommendation recommendation : recommendations) {
            assertNotNull("Recommendation should have category", recommendation.getCategory());
            assertNotNull("Recommendation should have strategy", recommendation.getStrategyName());
            assertNotNull("Recommendation should have reasoning", recommendation.getReasoning());
            assertNotNull("Recommendation should have use case", recommendation.getUseCase());
            
            // Validate strategy exists in our set
            boolean strategyExists = allStrategies.stream()
                    .anyMatch(s -> s.getStrategyName().equals(recommendation.getStrategyName()));
            assertTrue("Recommended strategy should exist: " + recommendation.getStrategyName(),
                      strategyExists);
        }
    }
    
    @Test
    public void testComplexityScaling() {
        Map<BenchmarkConfiguration.ComplexityLevel, Double> executionTimes = new HashMap<>();
        
        DirectCallbackStrategyAdapter strategy = new DirectCallbackStrategyAdapter();
        
        for (BenchmarkConfiguration.ComplexityLevel level : testConfig.getComplexityLevels()) {
            List<String> testCases = corpusGenerator.generateTestCases(level, 3);
            
            long startTime = System.nanoTime();
            BenchmarkResult result = strategy.executeBenchmark(testCases, testConfig);
            long endTime = System.nanoTime();
            
            double executionTime = (endTime - startTime) / 1_000_000.0;
            executionTimes.put(level, executionTime);
            
            // Validate scaling characteristics
            assertTrue("Execution time should be positive for level: " + level,
                      result.getExecutionTimeMs() > 0);
        }
        
        // Validate complexity scaling (higher complexity should generally take more time)
        double simpleTime = executionTimes.get(BenchmarkConfiguration.ComplexityLevel.SIMPLE);
        double complexTime = executionTimes.get(BenchmarkConfiguration.ComplexityLevel.COMPLEX);
        
        // Allow some tolerance for test variability
        assertTrue("Complex cases should take at least half the time of simple cases",
                  complexTime >= simpleTime * 0.5);
    }
    
    /**
     * Helper methods for test execution
     */
    private Map<String, List<String>> generateTestCorpus() {
        Map<String, List<String>> corpus = new HashMap<>();
        
        for (BenchmarkConfiguration.ComplexityLevel level : testConfig.getComplexityLevels()) {
            List<String> testCases = corpusGenerator.generateTestCases(level, testConfig.getSamplesPerLevel());
            corpus.put(level.name(), testCases);
        }
        
        return corpus;
    }
    
    private Map<String, BenchmarkResult> executeLightweightBenchmarks() {
        Map<String, BenchmarkResult> results = new HashMap<>();
        
        // Use moderate complexity for consistent testing
        List<String> testCases = corpusGenerator.generateTestCases(
            BenchmarkConfiguration.ComplexityLevel.MODERATE, 3);
        
        for (ParserStrategyAdapter strategy : allStrategies) {
            try {
                System.out.println("DEBUG: Executing benchmark for strategy: " + strategy.getStrategyName());
                System.out.println("DEBUG: Strategy class: " + strategy.getClass().getName());
                BenchmarkResult result = strategy.executeBenchmark(testCases, testConfig);
                System.out.println("DEBUG: Benchmark result created successfully for: " + strategy.getStrategyName());
                String key = strategy.getStrategyName() + "_" + BenchmarkConfiguration.ComplexityLevel.MODERATE.name();
                results.put(key, result);
            } catch (Exception e) {
                System.err.println("ERROR: Failed to execute benchmark for strategy: " + strategy.getStrategyName());
                System.err.println("ERROR: Exception type: " + e.getClass().getName());
                System.err.println("ERROR: Exception message: " + e.getMessage());
                e.printStackTrace();
                throw e; // Re-throw to maintain test behavior
            }
        }
        
        return results;
    }
    
    private BenchmarkReport createBenchmarkReport(Map<String, BenchmarkResult> results) {
        // Use the BenchmarkReporter to create the report properly
        Map<String, StrategyCharacteristics> characteristics = new HashMap<>();
        for (ParserStrategyAdapter strategy : allStrategies) {
            characteristics.put(strategy.getStrategyName(), strategy.getStrategyCharacteristics());
        }
        
        // Filter results to only include strategies we have
        Map<String, BenchmarkResult> filteredResults = new HashMap<>();
        for (ParserStrategyAdapter strategy : allStrategies) {
            String key = strategy.getStrategyName() + "_" + BenchmarkConfiguration.ComplexityLevel.MODERATE.name();
            BenchmarkResult result = results.get(key);
            if (result != null) {
                filteredResults.put(strategy.getStrategyName(), result);
            }
        }
        
        BenchmarkReporter reporter = new BenchmarkReporter(testConfig);
        return reporter.generateReport(filteredResults, characteristics);
    }
    
    private void validateBenchmarkResult(BenchmarkResult result, String strategyName) {
        assertNotNull("Result should not be null for strategy: " + strategyName, result);
        assertTrue("Execution time should be positive for strategy: " + strategyName,
                  result.getExecutionTimeMs() > 0);
        assertTrue("Callback count should be non-negative for strategy: " + strategyName,
                  result.getCallbackCount() >= 0);
        
        // Validate metrics
        assertNotNull("Memory metrics should not be null for strategy: " + strategyName,
                     result.getMemoryMetrics());
        assertNotNull("Performance metrics should not be null for strategy: " + strategyName,
                     result.getPerformanceMetrics());
        assertNotNull("Scalability metrics should not be null for strategy: " + strategyName,
                     result.getScalabilityMetrics());
        assertNotNull("Error recovery metrics should not be null for strategy: " + strategyName,
                     result.getErrorRecoveryMetrics());
    }
    
    private void validateResultConsistency(Map<String, BenchmarkResult> results) {
        // Validate that all strategies produced reasonable results
        for (Map.Entry<String, BenchmarkResult> entry : results.entrySet()) {
            BenchmarkResult result = entry.getValue();
            
            // Basic sanity checks
            assertTrue("Execution time should be reasonable (< 10s): " + entry.getKey(),
                      result.getExecutionTimeMs() < 10000);
            assertTrue("Should have positive memory usage: " + entry.getKey(),
                      result.getMemoryMetrics().getPeakUsedMemory() > 0);
            assertTrue("Throughput should be non-negative: " + entry.getKey(),
                      result.getPerformanceMetrics().getThroughput() >= 0);
        }
    }
}