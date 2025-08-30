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

import bluej.parser.pratt.integration.benchmark.metrics.*;
import bluej.parser.pratt.integration.benchmark.corpus.TestCorpusItem;

/**
 * Interface for parser strategy adapters that allows unified benchmarking
 * across all 7 parser integration strategies.
 *
 * Each integration strategy (AST Visitor, Lazy AST, Direct Callback, etc.)
 * implements this interface to provide consistent measurement capabilities.
 *
 * The adapter pattern abstracts away the specific implementation details
 * while providing standardized benchmark execution and metrics collection.
 *
 * This interface extends StrategyAdapter to ensure compatibility with
 * JMH benchmarks and unified strategy execution.
 */
public interface ParserStrategyAdapter extends StrategyAdapter {
    
    /**
     * Get the human-readable name of this parser strategy.
     * 
     * @return Strategy name (e.g., "DirectCallbackIntegration", "ASTVisitorPattern")
     */
    String getStrategyName();
    
    /**
     * Get the characteristics of this strategy for analysis and comparison.
     * 
     * @return Strategy characteristics including efficiency profiles
     */
    StrategyCharacteristics getCharacteristics();
    
    /**
     * Get strategy characteristics. Alternative naming for compatibility.
     */
    default StrategyCharacteristics getStrategyCharacteristics() {
        return getCharacteristics();
    }
    
    /**
     * Execute a benchmark run for this strategy using the provided test corpus.
     * This is the core benchmarking method that measures performance.
     * 
     * @param corpus The test code to parse and analyze
     * @param config Benchmark configuration (iterations, profiling settings)
     * @return Complete benchmark results with all metrics
     */
    BenchmarkResult executeBenchmark(TestCorpusItem corpus, BenchmarkConfiguration config);
    
    /**
     * Alternative signature for backward compatibility with List<String> test cases.
     */
    default BenchmarkResult executeBenchmark(java.util.List<String> testCases, BenchmarkConfiguration config) {
        // Convert List<String> to TestCorpusItem for backward compatibility
        if (testCases == null || testCases.isEmpty()) {
            return executeBenchmark(new TestCorpusItem("",
                BenchmarkConfiguration.ComplexityLevel.SIMPLE,
                "empty", false, false), config);
        }
        String combinedCode = String.join("\n", testCases);
        TestCorpusItem corpus = new TestCorpusItem(combinedCode,
            BenchmarkConfiguration.ComplexityLevel.MODERATE,
            "combined test cases", false, false);
        return executeBenchmark(corpus, config);
    }
    
    /**
     * Create a memory profiler instance for this strategy.
     * Different strategies may need different profiling approaches.
     * 
     * @return Memory profiler tailored for this strategy
     */
    MemoryProfiler createMemoryProfiler();
    
    /**
     * Check if this strategy supports error recovery mechanisms.
     * 
     * @return true if the strategy can recover from parsing errors gracefully
     */
    boolean supportsErrorRecovery();
    
    /**
     * Measure error recovery capabilities using malformed code.
     * Only called if supportsErrorRecovery() returns true.
     * 
     * @param malformedCode Code with intentional syntax errors
     * @return Error recovery performance metrics
     */
    ErrorRecoveryMetrics measureErrorRecovery(String malformedCode);
    
    /**
     * Get strategy-specific configuration options.
     * Some strategies may have tunable parameters for benchmarking.
     * 
     * @return Configuration options specific to this strategy
     */
    default StrategyConfiguration getStrategyConfiguration() {
        return new StrategyConfiguration();
    }
    
    /**
     * Validate that this adapter can properly benchmark the strategy.
     * Called during framework initialization to ensure proper setup.
     * 
     * @return Validation result with any setup issues
     */
    default ValidationResult validate() {
        return ValidationResult.success();
    }
    
    /**
     * Clean up any resources allocated during benchmarking.
     * Called after benchmark execution completes.
     */
    default void cleanup() {
        // Default: no cleanup needed
    }
}