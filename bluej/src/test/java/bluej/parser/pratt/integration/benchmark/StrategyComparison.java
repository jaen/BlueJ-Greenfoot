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

import bluej.parser.pratt.integration.benchmark.metrics.BenchmarkResult;

/**
 * Represents a comparison between parser integration strategies.
 * 
 * This class encapsulates the benchmark results and characteristics 
 * for a single strategy, allowing comparison with other strategies.
 * Used by the benchmark framework to rank and analyze strategy performance.
 */
public class StrategyComparison {
    private final String strategyName;
    private final BenchmarkResult result;
    private final StrategyCharacteristics characteristics;
    private final double overallScore;
    
    public StrategyComparison(String strategyName,
                             BenchmarkResult result,
                             StrategyCharacteristics characteristics) {
        this.strategyName = strategyName;
        this.result = result;
        this.characteristics = characteristics;
        this.overallScore = calculateOverallScore();
    }
    
    /**
     * Get the strategy name.
     */
    public String getStrategyName() {
        return strategyName;
    }
    
    /**
     * Get the benchmark result.
     */
    public BenchmarkResult getResult() {
        return result;
    }
    
    /**
     * Get the strategy characteristics.
     */
    public StrategyCharacteristics getCharacteristics() {
        return characteristics;
    }
    
    /**
     * Get the overall performance score.
     * Higher scores indicate better performance.
     */
    public double getOverallScore() {
        return overallScore;
    }
    
    /**
     * Calculate overall score based on multiple metrics.
     * Combines performance, memory efficiency, and scalability scores.
     */
    private double calculateOverallScore() {
        double performanceScore = result.getPerformanceMetrics().getEfficiencyScore();
        double memoryScore = result.getMemoryMetrics().getEfficiencyScore();
        double scalabilityScore = result.getScalabilityMetrics().getScalabilityScore();
        
        // Weighted average: performance (40%), memory (35%), scalability (25%)
        return performanceScore * 0.4 + memoryScore * 0.35 + scalabilityScore * 0.25;
    }
    
    /**
     * Compare this strategy with another based on overall score.
     */
    public int compareOverallScore(StrategyComparison other) {
        return Double.compare(this.overallScore, other.overallScore);
    }
    
    /**
     * Check if this strategy is better than another for a specific use case.
     */
    public boolean isBetterForUseCase(StrategyComparison other, StrategyCharacteristics.UseCase useCase) {
        double thisScore = characteristics.getSuitabilityScore(useCase);
        double otherScore = other.characteristics.getSuitabilityScore(useCase);
        
        // If suitability is similar, compare overall performance
        if (Math.abs(thisScore - otherScore) < 0.1) {
            return this.overallScore > other.overallScore;
        }
        
        return thisScore > otherScore;
    }
    
    /**
     * Get performance advantage over another strategy as a percentage.
     * Positive values indicate this strategy is better.
     */
    public double getPerformanceAdvantage(StrategyComparison other) {
        double thisTime = this.result.getPerformanceMetrics().getAvgParseTimeMs();
        double otherTime = other.result.getPerformanceMetrics().getAvgParseTimeMs();
        
        if (otherTime == 0) return 0.0;
        
        return ((otherTime - thisTime) / otherTime) * 100.0;
    }
    
    /**
     * Get memory efficiency advantage over another strategy as a percentage.
     */
    public double getMemoryAdvantage(StrategyComparison other) {
        double thisMemory = this.result.getMemoryMetrics().getPeakUsageMB();
        double otherMemory = other.result.getMemoryMetrics().getPeakUsageMB();
        
        if (otherMemory == 0) return 0.0;
        
        return ((otherMemory - thisMemory) / otherMemory) * 100.0;
    }
    
    /**
     * Get a summary of this strategy comparison.
     */
    public String getSummary() {
        return String.format("%s: Overall=%.1f, Perf=%.1fms, Mem=%.1fMB", 
                strategyName, overallScore,
                result.getPerformanceMetrics().getAvgParseTimeMs(),
                result.getMemoryMetrics().getPeakUsageMB());
    }
    
    @Override
    public String toString() {
        return String.format("StrategyComparison{name='%s', score=%.2f}", strategyName, overallScore);
    }
}