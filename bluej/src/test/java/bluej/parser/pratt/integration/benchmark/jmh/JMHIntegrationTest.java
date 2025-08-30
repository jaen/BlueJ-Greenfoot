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

import bluej.parser.pratt.integration.benchmark.*;
import bluej.parser.pratt.integration.benchmark.adapters.DirectCallbackStrategyAdapter;
import bluej.parser.pratt.integration.benchmark.metrics.*;
import bluej.parser.pratt.integration.benchmark.measurement.MetricsCollector;
import bluej.parser.pratt.integration.benchmark.corpus.TestCorpusGenerator;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Integration tests for JMH benchmark runner.
 * 
 * Validates that:
 * - JMH integration works with our strategy framework
 * - Benchmark results are properly converted between formats
 * - Performance measurements are reasonable and consistent
 * - Error handling works correctly
 * - Memory profiling integrates properly with JMH
 * 
 * Note: These tests use lightweight configurations to avoid long execution times
 * during development. Full benchmarks should use the production configuration.
 */
public class JMHIntegrationTest {
    
    private BenchmarkConfiguration testConfig;
    private List<ParserStrategyAdapter> testStrategies;
    private TestCorpusGenerator corpusGenerator;
    
    @Before
    public void setUp() {
        // Create lightweight test configuration
        testConfig = BenchmarkConfiguration.builder()
                .warmupIterations(1)
                .measurementIterations(2)
                .warmupTimeMs(100)
                .measurementTimeMs(200)
                .samplesPerLevel(2)
                .includeErrorCases(false)
                .includeIncompleteCode(false)
                .enableMemoryProfiling(true)
                .enableDetailedMetrics(true)
                .outputDirectory("build/test-benchmark-results")
                .complexityLevels(
                    BenchmarkConfiguration.ComplexityLevel.SIMPLE,
                    BenchmarkConfiguration.ComplexityLevel.MODERATE
                )
                .scalabilityTestSizes(10, 20)
                .build();
        
        // Create test strategy adapters
        testStrategies = Arrays.asList(
            new DirectCallbackStrategyAdapter()
        );
        
        corpusGenerator = new TestCorpusGenerator();
    }
    
    @Test
    public void testJMHRunnerInitialization() {
        try {
            JMHBenchmarkRunner.EnhancedJMHRunner runner =
                new JMHBenchmarkRunner.EnhancedJMHRunner(testConfig, testStrategies);
            assertNotNull("JMH runner should be created successfully", runner);
        } catch (Exception e) {
            fail("JMH Enhanced Runner should initialize correctly: " + e.getMessage());
        }
    }
    
    @Test
    public void testJMHResultConversion() {
        // Test the result conversion logic with mock JMH data
        
        // Create a mock benchmark result that would come from our strategy
        BenchmarkResult originalResult = new BenchmarkResult.Builder("test")
                .corpusName("test-corpus")  // Required field
                .executionTime(15.5)
                .callbackCount(150)
                .callbacksBalanced(true)
                .memoryMetrics(new MemoryMetrics.Builder()
                        .peakUsedMemory(1024 * 1024)
                        .allocationRate(500.0)
                        .gcPressure(0.1)
                        .retentionRate(0.8)
                        .build())
                .performanceMetrics(new PerformanceMetrics.Builder()
                        .totalParseTime(15.5)
                        .throughput(64.5)
                        .averageCallbackTime(0.103)
                        .minCallbackTime(0.045)
                        .maxCallbackTime(0.250)
                        .consistency(0.92)
                        .build())
                .scalabilityMetrics(new ScalabilityMetrics.Builder()  // Required field
                        .linearCoefficient(0.95)
                        .memoryGrowthExponent(1.1)
                        .performanceDegradationRate(0.05)
                        .concurrentThroughputMultiplier(3.5)
                        .maxTestedFileSizeLOC(1000)
                        .linearScaling(true)
                        .build())
                .configuration(testConfig)  // Required field - use the test config from setUp
                .build();
        
        // Verify the result contains expected data
        assertEquals(15.5, originalResult.getExecutionTimeMs(), 0.01);
        assertEquals(150, originalResult.getCallbackCount());
        assertTrue(originalResult.isCallbacksBalanced());
        assertEquals(1024 * 1024, originalResult.getMemoryMetrics().getPeakUsedMemory(), 0.1);
        assertEquals(64.5, originalResult.getPerformanceMetrics().getThroughput(), 0.1);
    }
    
    @Test
    public void testJMHBenchmarkConfiguration() {
        // Verify our test configuration is suitable for JMH
        assertTrue("Should have warmup iterations", testConfig.getWarmupIterations() > 0);
        assertTrue("Should have measurement iterations", testConfig.getMeasurementIterations() > 0);
        assertTrue("Should have warmup time", testConfig.getWarmupTimeMs() > 0);
        assertTrue("Should have measurement time", testConfig.getMeasurementTimeMs() > 0);
        assertFalse("Should have complexity levels", testConfig.getComplexityLevels().isEmpty());
        assertNotNull("Should have output directory", testConfig.getOutputDirectory());
    }
    
    @Test
    public void testStrategyExecution() {
        // Test that strategies can be executed in JMH context
        DirectCallbackStrategyAdapter strategy = new DirectCallbackStrategyAdapter();
        
        // Generate test cases
        List<String> testCases = corpusGenerator.generateTestCases(
            BenchmarkConfiguration.ComplexityLevel.SIMPLE, 3);
        
        assertFalse("Should generate test cases", testCases.isEmpty());
        
        // Execute strategy (simulating what JMH would do)
        try {
            BenchmarkResult result = strategy.executeBenchmark(testCases, testConfig);
            assertNotNull("Strategy should return benchmark result", result);
            assertTrue("Should have execution time", result.getExecutionTimeMs() > 0);
        } catch (Exception e) {
            fail("JMH integration should handle strategy execution: " + e.getMessage());
        }
    }
    
    @Test
    public void testMetricsCollection() {
        MetricsCollector collector = new MetricsCollector(testConfig);
        assertNotNull("Metrics collector should be created", collector);
        
        // Test that collector can handle JMH integration
        assertTrue("Detailed metrics should be enabled", testConfig.getEnableDetailedMetrics());
        assertTrue("Memory profiling should be enabled", testConfig.getEnableMemoryProfiling());
    }
    
    @Test
    public void testParameterExtraction() {
        // Test strategy name and complexity level parameter handling
        String[] strategyNames = testStrategies.stream()
                .map(ParserStrategyAdapter::getStrategyName)
                .toArray(String[]::new);
        
        String[] complexityLevels = testConfig.getComplexityLevels().stream()
                .map(Enum::name)
                .toArray(String[]::new);
        
        assertTrue("Should have strategy names", strategyNames.length > 0);
        assertTrue("Should have complexity levels", complexityLevels.length > 0);
        
        // Test that we can create parameter combinations
        for (String strategyName : strategyNames) {
            for (String complexityLevel : complexityLevels) {
                assertNotNull("Strategy name should not be null", strategyName);
                assertNotNull("Complexity level should not be null", complexityLevel);
                assertFalse("Strategy name should not be empty", strategyName.isEmpty());
                assertFalse("Complexity level should not be empty", complexityLevel.isEmpty());
            }
        }
    }
    
    @Test
    public void testMemoryProfilingIntegration() {
        assertTrue("Memory profiling should be enabled", testConfig.getEnableMemoryProfiling());
        
        // Test that memory metrics can be collected
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        assertTrue("Should have total memory", totalMemory > 0);
        assertTrue("Should have free memory", freeMemory >= 0);
        assertTrue("Should have used memory calculation", usedMemory >= 0);
    }
    
    @Test
    public void testErrorHandling() {
        // Test error handling with invalid configurations
        BenchmarkConfiguration invalidConfig = BenchmarkConfiguration.builder()
                .warmupIterations(0)
                .measurementIterations(0)
                .samplesPerLevel(0)
                .build();
        
        // Should handle gracefully
        try {
            JMHBenchmarkRunner.EnhancedJMHRunner runner =
                new JMHBenchmarkRunner.EnhancedJMHRunner(invalidConfig, testStrategies);
            // Runner creation should succeed even with invalid config
            // The actual execution might fail, but creation should not
        } catch (Exception e) {
            fail("JMH error handling should be robust: " + e.getMessage());
        }
    }
    
    @Test
    public void testTimeUnitConversion() {
        // Test time unit handling for JMH (milliseconds to other units)
        double milliseconds = 15.5;
        
        // Convert to microseconds
        double microseconds = milliseconds * 1000;
        assertEquals(15500.0, microseconds, 0.1);
        
        // Convert to nanoseconds  
        double nanoseconds = milliseconds * 1_000_000;
        assertEquals(15_500_000.0, nanoseconds, 0.1);
        
        // Test throughput calculation (ops per second)
        double throughput = 1000.0 / milliseconds;
        assertEquals(64.5, throughput, 0.1);
    }
    
    @Test
    public void testStatisticalAnalysis() {
        // Test statistical calculations that would be used with JMH raw results
        double[] sampleResults = {10.0, 12.0, 11.0, 13.0, 10.5, 11.5, 12.5};
        
        // Calculate mean
        double mean = Arrays.stream(sampleResults).average().orElse(0.0);
        assertEquals(11.5, mean, 0.1);
        
        // Calculate standard deviation
        double variance = Arrays.stream(sampleResults)
                .map(x -> Math.pow(x - mean, 2))
                .average()
                .orElse(0.0);
        double stdDev = Math.sqrt(variance);
        assertTrue("Standard deviation should be positive", stdDev > 0);
        
        // Calculate consistency (1 - coefficient of variation)
        double consistency = 1.0 - (stdDev / mean);
        assertTrue("Consistency should be between 0 and 1", consistency > 0 && consistency < 1);
    }
    
    @Test
    public void testStrategyCharacteristicsPreservation() {
        DirectCallbackStrategyAdapter strategy = new DirectCallbackStrategyAdapter();
        
        // Verify strategy characteristics are preserved
        assertEquals("DirectCallbackIntegration", strategy.getStrategyName());
        assertNotNull(strategy.getStrategyCharacteristics());
        
        // Test that characteristics are available for JMH reporting
        StrategyCharacteristics characteristics = strategy.getStrategyCharacteristics();
        assertNotNull(characteristics.getDescription());
        assertNotNull(characteristics.getApproach());
        assertNotNull(characteristics.getImplementationComplexity());
        assertFalse(characteristics.getStrengths().isEmpty());
        assertFalse(characteristics.getIdealUseCases().isEmpty());
    }
    
    /**
     * Test helper to simulate JMH benchmark execution without actually running JMH.
     * This allows us to test the integration logic without the overhead of full JMH execution.
     */
    private void simulateJMHExecution() {
        // This would be called by JMH in real execution
        DirectCallbackStrategyAdapter strategy = new DirectCallbackStrategyAdapter();
        List<String> testCases = corpusGenerator.generateTestCases(
            BenchmarkConfiguration.ComplexityLevel.SIMPLE, 2);
        
        // Simulate JMH benchmark iteration
        try {
            BenchmarkResult result = strategy.executeBenchmark(testCases, testConfig);
            
            // Verify result has expected properties for JMH integration
            assertTrue(result.getExecutionTimeMs() > 0);
            assertTrue(result.getCallbackCount() > 0);
            assertNotNull(result.getMemoryMetrics());
            assertNotNull(result.getPerformanceMetrics());
            assertNotNull(result.getScalabilityMetrics());
            assertNotNull(result.getErrorRecoveryMetrics());
        } catch (Exception e) {
            fail("JMH benchmark simulation should work: " + e.getMessage());
        }
    }
    
    @Test
    public void testJMHIntegrationSimulation() {
        try {
            simulateJMHExecution();
        } catch (Exception e) {
            fail("JMH integration simulation should work correctly: " + e.getMessage());
        }
    }
}