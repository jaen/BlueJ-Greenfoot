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
package bluej.parser.pratt.integration.benchmark.metrics;

import bluej.parser.pratt.integration.benchmark.BenchmarkConfiguration;

import java.time.Instant;
import java.util.Objects;

/**
 * Complete benchmark result for a single parser integration strategy.
 * 
 * This class aggregates all performance metrics collected during benchmarking:
 * - Memory usage patterns
 * - Processing time measurements  
 * - Scalability characteristics
 * - Error recovery performance
 * 
 * Results can be compared across strategies and exported for analysis.
 */
public class BenchmarkResult {
    private final String strategyName;
    private final String corpusName;
    private final MemoryMetrics memoryMetrics;
    private final PerformanceMetrics performanceMetrics;
    private final ScalabilityMetrics scalabilityMetrics;
    private final ErrorRecoveryMetrics errorRecoveryMetrics;
    private final Instant timestamp;
    private final BenchmarkConfiguration configuration;
    // Additional fields for compatibility
    private final double executionTimeMs;
    private final int callbackCount;
    private final boolean callbacksBalanced;
    
    public BenchmarkResult(String strategyName,
                          String corpusName,
                          MemoryMetrics memoryMetrics,
                          PerformanceMetrics performanceMetrics,
                          ScalabilityMetrics scalabilityMetrics,
                          ErrorRecoveryMetrics errorRecoveryMetrics,
                          BenchmarkConfiguration configuration) {
        this.strategyName = Objects.requireNonNull(strategyName);
        this.corpusName = Objects.requireNonNull(corpusName);
        this.memoryMetrics = Objects.requireNonNull(memoryMetrics);
        this.performanceMetrics = Objects.requireNonNull(performanceMetrics);
        this.scalabilityMetrics = Objects.requireNonNull(scalabilityMetrics);
        this.errorRecoveryMetrics = errorRecoveryMetrics; // Can be null for strategies without error recovery
        this.configuration = Objects.requireNonNull(configuration);
        this.timestamp = Instant.now();
        // Initialize compatibility fields from metrics
        this.executionTimeMs = performanceMetrics.getAvgParseTimeMs();
        this.callbackCount = performanceMetrics.getSampleCount();
        this.callbacksBalanced = true; // Default assumption
    }
    
    public String getStrategyName() {
        return strategyName;
    }
    
    public String getCorpusName() {
        return corpusName;
    }
    
    public MemoryMetrics getMemoryMetrics() {
        return memoryMetrics;
    }
    
    public PerformanceMetrics getPerformanceMetrics() {
        return performanceMetrics;
    }
    
    public ScalabilityMetrics getScalabilityMetrics() {
        return scalabilityMetrics;
    }
    
    public ErrorRecoveryMetrics getErrorRecoveryMetrics() {
        return errorRecoveryMetrics;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public BenchmarkConfiguration getConfiguration() {
        return configuration;
    }
    
    /**
     * Check if this benchmark result includes error recovery metrics.
     */
    public boolean hasErrorRecoveryMetrics() {
        return errorRecoveryMetrics != null;
    }
    
    // ==================== Compatibility Methods ====================
    
    /**
     * Get execution time in milliseconds. Compatibility method.
     */
    public double getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    /**
     * Get execution time in milliseconds. Legacy compatibility method.
     * Same as getExecutionTimeMs() for backward compatibility.
     */
    public double getExecutionTime() {
        return executionTimeMs;
    }
    
    /**
     * Get callback count. Compatibility method.
     */
    public int getCallbackCount() {
        return callbackCount;
    }
    
    /**
     * Check if callbacks are balanced. Compatibility method.
     */
    public boolean isCallbacksBalanced() {
        return callbacksBalanced;
    }
    
    /**
     * Get test name - same as strategy name for compatibility.
     */
    public String getTestName() {
        return strategyName;
    }
    
    /**
     * Calculate overall performance score for ranking strategies.
     * Lower scores indicate better performance.
     */
    public double calculateOverallScore() {
        double memoryScore = memoryMetrics.getPeakUsageMB() * 0.3;
        double timeScore = performanceMetrics.getAvgParseTimeMs() * 0.4;
        double throughputScore = (1000.0 / Math.max(performanceMetrics.getThroughputLOCPerSec(), 1.0)) * 0.3;
        
        return memoryScore + timeScore + throughputScore;
    }
    
    /**
     * Get a human-readable summary of the benchmark results.
     */
    public String getSummary() {
        return String.format("%s: Memory=%.1fMB, Time=%.1fms, Throughput=%d LOC/s",
                strategyName,
                memoryMetrics.getPeakUsageMB(),
                performanceMetrics.getAvgParseTimeMs(),
                (int) performanceMetrics.getThroughputLOCPerSec());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BenchmarkResult that = (BenchmarkResult) obj;
        return Objects.equals(strategyName, that.strategyName) &&
               Objects.equals(corpusName, that.corpusName) &&
               Objects.equals(timestamp, that.timestamp);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(strategyName, corpusName, timestamp);
    }
    
    @Override
    public String toString() {
        return String.format("BenchmarkResult{strategy='%s', corpus='%s', timestamp='%s'}",
                strategyName, corpusName, timestamp);
    }
    
    /**
     * Builder for BenchmarkResult.
     */
    public static class Builder {
        private String strategyName;
        private String corpusName;
        private MemoryMetrics memoryMetrics;
        private PerformanceMetrics performanceMetrics;
        private ScalabilityMetrics scalabilityMetrics;
        private ErrorRecoveryMetrics errorRecoveryMetrics;
        private BenchmarkConfiguration configuration;
        // Compatibility fields for builder methods
        private double executionTime = 0.0;
        private int callbackCount = 0;
        private boolean callbacksBalanced = true;
        
        public Builder(String strategyName) {
            this.strategyName = strategyName;
        }
        
        public Builder corpusName(String corpusName) {
            this.corpusName = corpusName;
            return this;
        }
        
        public Builder memoryMetrics(MemoryMetrics memoryMetrics) {
            this.memoryMetrics = memoryMetrics;
            return this;
        }
        
        public Builder performanceMetrics(PerformanceMetrics performanceMetrics) {
            this.performanceMetrics = performanceMetrics;
            return this;
        }
        
        public Builder scalabilityMetrics(ScalabilityMetrics scalabilityMetrics) {
            this.scalabilityMetrics = scalabilityMetrics;
            return this;
        }
        
        public Builder errorRecoveryMetrics(ErrorRecoveryMetrics errorRecoveryMetrics) {
            this.errorRecoveryMetrics = errorRecoveryMetrics;
            return this;
        }
        
        public Builder configuration(BenchmarkConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }
        
        // Compatibility builder methods
        public Builder executionTime(double executionTime) {
            this.executionTime = executionTime;
            return this;
        }
        
        public Builder callbackCount(int callbackCount) {
            this.callbackCount = callbackCount;
            return this;
        }
        
        public Builder callbacksBalanced(boolean callbacksBalanced) {
            this.callbacksBalanced = callbacksBalanced;
            return this;
        }
        
        public BenchmarkResult build() {
            BenchmarkResult result = new BenchmarkResult(strategyName, corpusName, memoryMetrics,
                                                        performanceMetrics, scalabilityMetrics,
                                                        errorRecoveryMetrics, configuration);
            // Override compatibility fields if explicitly set
            if (executionTime > 0.0) {
                try {
                    java.lang.reflect.Field field = BenchmarkResult.class.getDeclaredField("executionTimeMs");
                    field.setAccessible(true);
                    field.set(result, executionTime);
                } catch (Exception e) {
                    // Ignore reflection errors - fallback to default value
                }
            }
            if (callbackCount > 0) {
                try {
                    java.lang.reflect.Field field = BenchmarkResult.class.getDeclaredField("callbackCount");
                    field.setAccessible(true);
                    field.set(result, callbackCount);
                } catch (Exception e) {
                    // Ignore reflection errors - fallback to default value
                }
            }
            try {
                java.lang.reflect.Field field = BenchmarkResult.class.getDeclaredField("callbacksBalanced");
                field.setAccessible(true);
                field.set(result, callbacksBalanced);
            } catch (Exception e) {
                // Ignore reflection errors - fallback to default value
            }
            return result;
        }
    }
}