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

/**
 * Performance timing metrics for parser integration strategy benchmarking.
 * 
 * Measures various aspects of processing time during parsing operations:
 * - Total parse time from start to completion
 * - Callback generation overhead
 * - Setup and teardown costs
 * - Overall throughput in lines of code per second
 * 
 * These metrics help distinguish between strategies like:
 * - Direct Callback (fastest, immediate callback emission)
 * - Lazy AST (variable timing based on access patterns)
 * - AST Visitor (two-phase: build + traverse overhead)
 * - K2 Parser (high setup cost, then fast parsing)
 */
public class PerformanceMetrics {
    private final double avgParseTimeMs;
    private final double stdDevParseTimeMs;
    private final double callbackGenerationTimeMs;
    private final double setupTeardownOverheadMs;
    private final double throughputLOCPerSec;
    private final double minParseTimeMs;
    private final double maxParseTimeMs;
    private final int sampleCount;
    
    public PerformanceMetrics(double avgParseTimeMs,
                             double stdDevParseTimeMs,
                             double callbackGenerationTimeMs,
                             double setupTeardownOverheadMs,
                             double throughputLOCPerSec,
                             double minParseTimeMs,
                             double maxParseTimeMs,
                             int sampleCount) {
        this.avgParseTimeMs = avgParseTimeMs;
        this.stdDevParseTimeMs = stdDevParseTimeMs;
        this.callbackGenerationTimeMs = callbackGenerationTimeMs;
        this.setupTeardownOverheadMs = setupTeardownOverheadMs;
        this.throughputLOCPerSec = throughputLOCPerSec;
        this.minParseTimeMs = minParseTimeMs;
        this.maxParseTimeMs = maxParseTimeMs;
        this.sampleCount = sampleCount;
    }
    
    /**
     * Average parsing time across all benchmark iterations in milliseconds.
     */
    public double getAvgParseTimeMs() {
        return avgParseTimeMs;
    }
    
    /**
     * Standard deviation of parse times, indicating consistency.
     * Lower values mean more predictable performance.
     */
    public double getStdDevParseTimeMs() {
        return stdDevParseTimeMs;
    }
    
    /**
     * Time spent specifically generating callbacks in milliseconds.
     * Some strategies (like Lazy AST) may defer this work.
     */
    public double getCallbackGenerationTimeMs() {
        return callbackGenerationTimeMs;
    }
    
    /**
     * Overhead from initialization and cleanup in milliseconds.
     * K2 Parser typically has higher setup costs.
     */
    public double getSetupTeardownOverheadMs() {
        return setupTeardownOverheadMs;
    }
    
    /**
     * Throughput in lines of code processed per second.
     * Higher values indicate better performance for large files.
     */
    public double getThroughputLOCPerSec() {
        return throughputLOCPerSec;
    }
    
    /**
     * Fastest single parsing operation in milliseconds.
     */
    public double getMinParseTimeMs() {
        return minParseTimeMs;
    }
    
    /**
     * Slowest single parsing operation in milliseconds.
     */
    public double getMaxParseTimeMs() {
        return maxParseTimeMs;
    }
    
    /**
     * Number of samples used to calculate these metrics.
     */
    public int getSampleCount() {
        return sampleCount;
    }
    
    /**
     * Calculate coefficient of variation (std dev / mean).
     * Lower values indicate more consistent performance.
     */
    public double getCoefficientOfVariation() {
        return avgParseTimeMs > 0 ? stdDevParseTimeMs / avgParseTimeMs : 0.0;
    }
    
    /**
     * Get 95th percentile parse time estimate.
     * Assumes normal distribution (rough approximation).
     */
    public double get95thPercentileMs() {
        return avgParseTimeMs + (1.645 * stdDevParseTimeMs);
    }
    
    /**
     * Calculate performance efficiency score (higher is better).
     * Balances speed and consistency.
     */
    public double getEfficiencyScore() {
        double speedScore = Math.max(0, 100 - Math.log(avgParseTimeMs + 1) * 10);
        double consistencyScore = Math.max(0, 100 - getCoefficientOfVariation() * 100);
        double throughputScore = Math.min(100, Math.log(throughputLOCPerSec + 1) * 20);
        
        return (speedScore + consistencyScore + throughputScore) / 3.0;
    }
    
    /**
     * Check if performance is considered fast.
     * Useful for categorizing strategies.
     */
    public boolean isFast() {
        return avgParseTimeMs < 100 && throughputLOCPerSec > 1000;
    }
    
    /**
     * Check if performance is consistent (low variability).
     */
    public boolean isConsistent() {
        return getCoefficientOfVariation() < 0.2;
    }
    
    // ==================== Compatibility Aliases ====================
    
    /**
     * Alias for getThroughputLOCPerSec() for backward compatibility.
     */
    public double getThroughput() {
        return throughputLOCPerSec;
    }
    
    /**
     * Alias for getAvgParseTimeMs() for backward compatibility.
     */
    public double getTotalParseTimeMs() {
        return avgParseTimeMs;
    }
    
    /**
     * Average callback time - same as average parse time for most strategies.
     */
    public double getAverageCallbackTimeMs() {
        return avgParseTimeMs;
    }
    
    /**
     * Alias for getMinParseTimeMs() for backward compatibility.
     */
    public double getMinCallbackTimeMs() {
        return minParseTimeMs;
    }
    
    /**
     * Alias for getMaxParseTimeMs() for backward compatibility.
     */
    public double getMaxCallbackTimeMs() {
        return maxParseTimeMs;
    }
    
    /**
     * Consistency score based on coefficient of variation.
     */
    public double getConsistency() {
        return Math.max(0, 1.0 - getCoefficientOfVariation());
    }
    
    @Override
    public String toString() {
        return String.format("PerformanceMetrics{avg=%.1fms, throughput=%.0f LOC/s, consistency=%.3f}",
                avgParseTimeMs, throughputLOCPerSec, getCoefficientOfVariation());
    }
    
    /**
     * Builder for PerformanceMetrics.
     */
    public static class Builder {
        private double avgParseTimeMs = 0.0;
        private double stdDevParseTimeMs = 0.0;
        private double callbackGenerationTimeMs = 0.0;
        private double setupTeardownOverheadMs = 0.0;
        private double throughputLOCPerSec = 0.0;
        private double minParseTimeMs = 0.0;
        private double maxParseTimeMs = 0.0;
        private int sampleCount = 1;
        
        public Builder avgParseTimeMs(double avgParseTimeMs) {
            this.avgParseTimeMs = avgParseTimeMs;
            return this;
        }
        
        public Builder stdDevParseTimeMs(double stdDevParseTimeMs) {
            this.stdDevParseTimeMs = stdDevParseTimeMs;
            return this;
        }
        
        public Builder callbackGenerationTimeMs(double callbackGenerationTimeMs) {
            this.callbackGenerationTimeMs = callbackGenerationTimeMs;
            return this;
        }
        
        public Builder setupTeardownOverheadMs(double setupTeardownOverheadMs) {
            this.setupTeardownOverheadMs = setupTeardownOverheadMs;
            return this;
        }
        
        public Builder throughputLOCPerSec(double throughputLOCPerSec) {
            this.throughputLOCPerSec = throughputLOCPerSec;
            return this;
        }
        
        public Builder minParseTimeMs(double minParseTimeMs) {
            this.minParseTimeMs = minParseTimeMs;
            return this;
        }
        
        public Builder maxParseTimeMs(double maxParseTimeMs) {
            this.maxParseTimeMs = maxParseTimeMs;
            return this;
        }
        
        public Builder sampleCount(int sampleCount) {
            this.sampleCount = sampleCount;
            return this;
        }
        
        // Compatibility aliases for Builder
        public Builder totalParseTime(double totalParseTime) {
            this.avgParseTimeMs = totalParseTime;
            return this;
        }
        
        public Builder executionTime(double executionTime) {
            this.avgParseTimeMs = executionTime;
            return this;
        }
        
        // Additional compatibility methods
        public Builder dataPointCount(int dataPointCount) {
            this.sampleCount = dataPointCount;
            return this;
        }
        
        public Builder performanceImpact(double performanceImpact) {
            // Store performance impact as setup overhead
            this.setupTeardownOverheadMs = performanceImpact;
            return this;
        }
        
        public Builder throughput(double throughput) {
            this.throughputLOCPerSec = throughput;
            return this;
        }
        
        public Builder averageCallbackTime(double avgTime) {
            this.avgParseTimeMs = avgTime;
            return this;
        }
        
        public Builder minCallbackTime(double minTime) {
            this.minParseTimeMs = minTime;
            return this;
        }
        
        public Builder maxCallbackTime(double maxTime) {
            this.maxParseTimeMs = maxTime;
            return this;
        }
        
        public Builder consistency(double consistency) {
            // Convert consistency back to coefficient of variation
            this.stdDevParseTimeMs = avgParseTimeMs * Math.max(0, 1.0 - consistency);
            return this;
        }
        
        public PerformanceMetrics build() {
            return new PerformanceMetrics(avgParseTimeMs, stdDevParseTimeMs, callbackGenerationTimeMs,
                                        setupTeardownOverheadMs, throughputLOCPerSec, minParseTimeMs,
                                        maxParseTimeMs, sampleCount);
        }
    }
}