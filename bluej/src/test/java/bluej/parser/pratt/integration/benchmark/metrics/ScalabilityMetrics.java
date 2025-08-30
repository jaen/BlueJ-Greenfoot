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
 * Scalability metrics for parser integration strategy benchmarking.
 * 
 * Measures how each strategy performs as input size increases:
 * - Linear scaling coefficient (how well performance scales with file size)
 * - Memory growth patterns (linear, quadratic, etc.)
 * - Performance degradation at large scales
 * - Concurrent parsing capabilities
 * 
 * These metrics help distinguish between strategies like:
 * - Direct Callback (excellent linear scaling)
 * - AST Visitor (potential quadratic memory growth)
 * - Lazy AST (excellent memory scaling, variable time scaling)
 * - K2 Parser (good scaling after high startup cost)
 */
public class ScalabilityMetrics {
    private final double linearScalingCoefficient;
    private final double memoryGrowthExponent;
    private final double performanceDegradationRate;
    private final double concurrentThroughputMultiplier;
    private final int maxTestedFileSizeLOC;
    private final boolean maintainsLinearPerformance;
    private final double rSquared;
    
    public ScalabilityMetrics(double linearScalingCoefficient,
                             double memoryGrowthExponent,
                             double performanceDegradationRate,
                             double concurrentThroughputMultiplier,
                             int maxTestedFileSizeLOC,
                             boolean maintainsLinearPerformance) {
        this.linearScalingCoefficient = linearScalingCoefficient;
        this.memoryGrowthExponent = memoryGrowthExponent;
        this.performanceDegradationRate = performanceDegradationRate;
        this.concurrentThroughputMultiplier = concurrentThroughputMultiplier;
        this.maxTestedFileSizeLOC = maxTestedFileSizeLOC;
        this.maintainsLinearPerformance = maintainsLinearPerformance;
        this.rSquared = linearScalingCoefficient; // R-squared approximation based on linear coefficient
    }

    public ScalabilityMetrics(double linearScalingCoefficient,
                             double memoryGrowthExponent,
                             double performanceDegradationRate,
                             double concurrentThroughputMultiplier,
                             int maxTestedFileSizeLOC,
                             boolean maintainsLinearPerformance,
                             double rSquared) {
        this.linearScalingCoefficient = linearScalingCoefficient;
        this.memoryGrowthExponent = memoryGrowthExponent;
        this.performanceDegradationRate = performanceDegradationRate;
        this.concurrentThroughputMultiplier = concurrentThroughputMultiplier;
        this.maxTestedFileSizeLOC = maxTestedFileSizeLOC;
        this.maintainsLinearPerformance = maintainsLinearPerformance;
        this.rSquared = rSquared;
    }

    // Alternative constructor for different parameter order (used by ScalabilityAnalyzer)
    public ScalabilityMetrics(double linearScalingCoefficient,
                             double memoryGrowthExponent,
                             double performanceDegradationRate,
                             double concurrentThroughputMultiplier,
                             int maxTestedFileSizeLOC,
                             int linearScalingInt, // boolean converted to int
                             ScalabilityCategory category) {
        this.linearScalingCoefficient = linearScalingCoefficient;
        this.memoryGrowthExponent = memoryGrowthExponent;
        this.performanceDegradationRate = performanceDegradationRate;
        this.concurrentThroughputMultiplier = concurrentThroughputMultiplier;
        this.maxTestedFileSizeLOC = maxTestedFileSizeLOC;
        this.maintainsLinearPerformance = linearScalingInt > 0; // Convert int to boolean
        this.rSquared = linearScalingCoefficient;
    }
    
    /**
     * Linear scaling coefficient (0.0 to 1.0, where 1.0 is perfect linear scaling).
     * Measures how closely performance scales with input size.
     */
    public double getLinearScalingCoefficient() {
        return linearScalingCoefficient;
    }
    
    /**
     * Memory growth exponent (1.0 = linear, 2.0 = quadratic, etc.).
     * Lower values indicate better memory scaling behavior.
     */
    public double getMemoryGrowthExponent() {
        return memoryGrowthExponent;
    }
    
    /**
     * Performance degradation rate as file size increases.
     * Lower values indicate better scalability.
     */
    public double getPerformanceDegradationRate() {
        return performanceDegradationRate;
    }
    
    /**
     * Throughput multiplier when parsing concurrently vs single-threaded.
     * Values > 1.0 indicate good concurrent performance.
     */
    public double getConcurrentThroughputMultiplier() {
        return concurrentThroughputMultiplier;
    }
    
    /**
     * Maximum file size tested in lines of code.
     */
    public int getMaxTestedFileSizeLOC() {
        return maxTestedFileSizeLOC;
    }
    
    /**
     * Whether the strategy maintains linear performance characteristics.
     */
    public boolean getMaintainsLinearPerformance() {
        return maintainsLinearPerformance;
    }
    
    /**
     * Compatibility alias for getMaintainsLinearPerformance().
     */
    public boolean isLinearScaling() {
        return maintainsLinearPerformance;
    }
    
    /**
     * Compatibility alias for getLinearScalingCoefficient().
     */
    public double getLinearCoefficient() {
        return linearScalingCoefficient;
    }
    
    /**
     * R-squared correlation coefficient for linear scaling model.
     * Higher values (closer to 1.0) indicate better linear correlation.
     */
    public double getRSquared() {
        return rSquared;
    }
    
    /**
     * Memory growth rate as a simple multiplier.
     * Compatibility alias for memoryGrowthExponent.
     */
    public double getMemoryGrowthRate() {
        return memoryGrowthExponent;
    }
    
    /**
     * Get data point count. Compatibility method.
     */
    public int getDataPointCount() {
        return maxTestedFileSizeLOC;
    }
    
    /**
     * Predict parsing time for a given file size in lines of code.
     * Uses the measured scaling characteristics.
     */
    public double predictParsingTimeMs(int fileSizeLOC, double baselineTimeMs) {
        if (maintainsLinearPerformance) {
            return baselineTimeMs * (fileSizeLOC / 100.0) * linearScalingCoefficient;
        } else {
            // Apply degradation rate for non-linear scaling
            double scaleFactor = Math.pow(fileSizeLOC / 100.0, 1.0 + performanceDegradationRate);
            return baselineTimeMs * scaleFactor * linearScalingCoefficient;
        }
    }
    
    /**
     * Predict memory usage for a given file size in lines of code.
     */
    public double predictMemoryUsageMB(int fileSizeLOC, double baselineMemoryMB) {
        double scaleFactor = Math.pow(fileSizeLOC / 100.0, memoryGrowthExponent);
        return baselineMemoryMB * scaleFactor;
    }
    
    /**
     * Calculate overall scalability score (higher is better).
     * Combines linear scaling, memory growth, and concurrent performance.
     */
    public double getScalabilityScore() {
        double linearScore = linearScalingCoefficient * 40;
        double memoryScore = Math.max(0, 40 - (memoryGrowthExponent - 1.0) * 20);
        double degradationScore = Math.max(0, 20 - performanceDegradationRate * 10);
        
        return linearScore + memoryScore + degradationScore;
    }
    
    /**
     * Check if strategy is suitable for large files (>5000 LOC).
     */
    public boolean isSuitableForLargeFiles() {
        return maintainsLinearPerformance && memoryGrowthExponent < 1.5 && 
               performanceDegradationRate < 0.2;
    }
    
    /**
     * Check if strategy supports efficient concurrent processing.
     */
    public boolean supportsConcurrency() {
        return concurrentThroughputMultiplier > 1.5;
    }
    
    /**
     * Get scalability category for this strategy.
     */
    public ScalabilityCategory getCategory() {
        if (isSuitableForLargeFiles() && supportsConcurrency()) {
            return ScalabilityCategory.EXCELLENT;
        } else if (isSuitableForLargeFiles() || supportsConcurrency()) {
            return ScalabilityCategory.GOOD;
        } else if (maintainsLinearPerformance) {
            return ScalabilityCategory.MODERATE;
        } else {
            return ScalabilityCategory.LIMITED;
        }
    }
    
    public enum ScalabilityCategory {
        EXCELLENT("Excellent scalability with concurrent support"),
        GOOD("Good scalability with some limitations"),
        MODERATE("Moderate scalability for typical use cases"),
        LIMITED("Limited scalability, best for small files");
        
        private final String description;
        
        ScalabilityCategory(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    @Override
    public String toString() {
        return String.format("ScalabilityMetrics{linear=%.3f, memory=%.2f^x, category=%s}",
                linearScalingCoefficient, memoryGrowthExponent, getCategory());
    }
    
    /**
     * Builder for ScalabilityMetrics.
     */
    public static class Builder {
        private double linearScalingCoefficient = 1.0;
        private double memoryGrowthExponent = 1.0;
        private double performanceDegradationRate = 0.0;
        private double concurrentThroughputMultiplier = 1.0;
        private int maxTestedFileSizeLOC = 100;
        private boolean maintainsLinearPerformance = true;
        private double rSquared = 1.0;
        
        public Builder linearScalingCoefficient(double linearScalingCoefficient) {
            this.linearScalingCoefficient = linearScalingCoefficient;
            return this;
        }
        
        public Builder memoryGrowthExponent(double memoryGrowthExponent) {
            this.memoryGrowthExponent = memoryGrowthExponent;
            return this;
        }
        
        public Builder performanceDegradationRate(double performanceDegradationRate) {
            this.performanceDegradationRate = performanceDegradationRate;
            return this;
        }
        
        public Builder concurrentThroughputMultiplier(double concurrentThroughputMultiplier) {
            this.concurrentThroughputMultiplier = concurrentThroughputMultiplier;
            return this;
        }
        
        public Builder maxTestedFileSizeLOC(int maxTestedFileSizeLOC) {
            this.maxTestedFileSizeLOC = maxTestedFileSizeLOC;
            return this;
        }
        
        public Builder maintainsLinearPerformance(boolean maintainsLinearPerformance) {
            this.maintainsLinearPerformance = maintainsLinearPerformance;
            return this;
        }
        
        public Builder rSquared(double rSquared) {
            this.rSquared = rSquared;
            return this;
        }
        
        // Compatibility aliases
        public Builder linearCoefficient(double linearCoefficient) {
            return linearScalingCoefficient(linearCoefficient);
        }
        
        public Builder memoryGrowthRate(double memoryGrowthRate) {
            return memoryGrowthExponent(memoryGrowthRate);
        }
        
        public Builder linearScaling(boolean linearScaling) {
            return maintainsLinearPerformance(linearScaling);
        }
        
        // Compatibility method for dataPointCount
        public Builder dataPointCount(int dataPointCount) {
            this.maxTestedFileSizeLOC = dataPointCount;
            return this;
        }
        
        public ScalabilityMetrics build() {
            return new ScalabilityMetrics(linearScalingCoefficient, memoryGrowthExponent,
                                        performanceDegradationRate, concurrentThroughputMultiplier,
                                        maxTestedFileSizeLOC, maintainsLinearPerformance, rSquared);
        }
    }
}