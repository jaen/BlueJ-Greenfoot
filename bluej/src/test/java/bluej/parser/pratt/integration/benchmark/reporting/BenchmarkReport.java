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
package bluej.parser.pratt.integration.benchmark.reporting;

import bluej.parser.pratt.integration.benchmark.BenchmarkConfiguration;
import bluej.parser.pratt.integration.benchmark.StrategyCharacteristics;
import bluej.parser.pratt.integration.benchmark.metrics.BenchmarkResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Complete benchmark report containing all analysis results and comparisons.
 * 
 * This is the central data structure that aggregates all benchmark findings:
 * - Individual strategy performance results
 * - Comparative analysis across strategies
 * - Performance rankings and recommendations
 * - Summary statistics and insights
 * 
 * Used by various report formatters to generate output in different formats
 * (console, JSON, CSV, Markdown) while maintaining consistent data structure.
 */
public class BenchmarkReport {
    
    private final LocalDateTime reportTime;
    private final BenchmarkConfiguration config;
    private final List<StrategyComparison> strategyComparisons;
    private final PerformanceRanking ranking;
    private final BenchmarkSummary summary;
    private final List<StrategyRecommendation> recommendations;
    
    // Additional fields for JMH integration
    private final List<StrategyResult> strategyResults;
    
    public BenchmarkReport(LocalDateTime reportTime,
                          BenchmarkConfiguration config,
                          List<StrategyComparison> strategyComparisons,
                          PerformanceRanking ranking,
                          BenchmarkSummary summary,
                          List<StrategyRecommendation> recommendations) {
        this.reportTime = reportTime;
        this.config = config;
        this.strategyComparisons = strategyComparisons;
        this.ranking = ranking;
        this.summary = summary;
        this.recommendations = recommendations;
        this.strategyResults = new ArrayList<>();
    }
    
    // Constructor for JMH-based reports
    public BenchmarkReport() {
        this.reportTime = LocalDateTime.now();
        this.config = null;
        this.strategyComparisons = new ArrayList<>();
        this.ranking = null;
        this.summary = null;
        this.recommendations = new ArrayList<>();
        this.strategyResults = new ArrayList<>();
    }
    
    // Getters
    public LocalDateTime getReportTime() { return reportTime; }
    public BenchmarkConfiguration getConfig() { return config; }
    public List<StrategyComparison> getStrategyComparisons() { return strategyComparisons; }
    public PerformanceRanking getRanking() { return ranking; }
    public BenchmarkSummary getSummary() { return summary; }
    public List<StrategyRecommendation> getRecommendations() { return recommendations; }
    
    // JMH integration methods
    public List<StrategyResult> getStrategyResults() { return strategyResults; }
    
    public void addStrategyResult(StrategyResult result) {
        this.strategyResults.add(result);
    }
    
    public void addRecommendation(StrategyRecommendation recommendation) {
        this.recommendations.add(recommendation);
    }
    
    /**
     * Get strategy comparison by name.
     */
    public StrategyComparison getStrategyComparison(String strategyName) {
        return strategyComparisons.stream()
                .filter(c -> c.getStrategyName().equals(strategyName))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Get number of strategies compared.
     */
    public int getStrategyCount() {
        return strategyComparisons.size();
    }
    
    /**
     * Check if report contains any results.
     */
    public boolean hasResults() {
        return !strategyComparisons.isEmpty();
    }
    
    /**
     * Get report generation duration estimate.
     */
    public String getReportScope() {
        if (strategyComparisons.isEmpty()) {
            return "No strategies";
        }
        
        StringBuilder scope = new StringBuilder();
        scope.append(strategyComparisons.size()).append(" strategies");
        
        // Add complexity levels tested
        if (config != null) {
            scope.append(", ").append(config.getComplexityLevels().size()).append(" complexity levels");
            scope.append(", ").append(config.getSamplesPerLevel()).append(" samples per level");
        }
        
        return scope.toString();
    }
    
    @Override
    public String toString() {
        return String.format("BenchmarkReport[strategies=%d, time=%s]",
                getStrategyCount(), reportTime);
    }
}

/**
 * Comparison of a single strategy including results and characteristics.
 */
class StrategyComparison {
    
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
    
    // Getters
    public String getStrategyName() { return strategyName; }
    public BenchmarkResult getResult() { return result; }
    public StrategyCharacteristics getCharacteristics() { return characteristics; }
    public double getOverallScore() { return overallScore; }
    
    /**
     * Calculate overall performance score (0-100).
     */
    private double calculateOverallScore() {
        if (result == null) return 0;
        
        // Weighted combination of different performance aspects
        double memoryScore = calculateMemoryScore();      // 25% weight
        double performanceScore = calculatePerformanceScore(); // 35% weight
        double scalabilityScore = calculateScalabilityScore(); // 25% weight
        double errorRecoveryScore = calculateErrorRecoveryScore(); // 15% weight
        
        return (memoryScore * 0.25) + 
               (performanceScore * 0.35) + 
               (scalabilityScore * 0.25) + 
               (errorRecoveryScore * 0.15);
    }
    
    private double calculateMemoryScore() {
        // Lower memory usage = higher score
        long memoryUsed = (long) result.getMemoryMetrics().getPeakUsedMemory();
        double gcPressure = result.getMemoryMetrics().getGcPressure();
        
        // Normalize based on typical ranges (simplified)
        double memoryScore = Math.max(0, 100 - (memoryUsed / (10 * 1024 * 1024))); // 10MB baseline
        double gcScore = Math.max(0, 100 - (gcPressure * 10));
        
        return (memoryScore + gcScore) / 2;
    }
    
    private double calculatePerformanceScore() {
        double throughput = result.getPerformanceMetrics().getThroughput();
        double consistency = result.getPerformanceMetrics().getConsistency();
        
        // Normalize throughput (callbacks/second)
        double throughputScore = Math.min(100, throughput / 10); // 1000 callbacks/sec = 100 points
        double consistencyScore = consistency * 100;
        
        return (throughputScore * 0.7) + (consistencyScore * 0.3);
    }
    
    private double calculateScalabilityScore() {
        double rSquared = result.getScalabilityMetrics().getRSquared();
        boolean linearScaling = result.getScalabilityMetrics().isLinearScaling();
        
        double linearityScore = rSquared * 100;
        double scalingBonus = linearScaling ? 10 : 0;
        
        return Math.min(100, linearityScore + scalingBonus);
    }
    
    private double calculateErrorRecoveryScore() {
        return result.getErrorRecoveryMetrics().getErrorHandlingScore();
    }
    
    /**
     * Get performance category based on overall score.
     */
    public PerformanceCategory getPerformanceCategory() {
        if (overallScore >= 80) return PerformanceCategory.EXCELLENT;
        if (overallScore >= 65) return PerformanceCategory.GOOD;
        if (overallScore >= 45) return PerformanceCategory.MODERATE;
        return PerformanceCategory.POOR;
    }
    
    public enum PerformanceCategory {
        EXCELLENT("Excellent"), GOOD("Good"), MODERATE("Moderate"), POOR("Poor");
        
        private final String description;
        PerformanceCategory(String description) { this.description = description; }
        public String getDescription() { return description; }
    }
    
    @Override
    public String toString() {
        return String.format("StrategyComparison[%s: score=%.1f, category=%s]",
                strategyName, overallScore, getPerformanceCategory().getDescription());
    }
}

/**
 * Performance ranking of strategies across different metrics.
 */
class PerformanceRanking {
    
    private final List<StrategyComparison> rankedByOverall;
    private final List<StrategyComparison> rankedByMemory;
    private final List<StrategyComparison> rankedBySpeed;
    private final List<StrategyComparison> rankedByScalability;
    
    public PerformanceRanking(List<StrategyComparison> rankedByOverall,
                             List<StrategyComparison> rankedByMemory,
                             List<StrategyComparison> rankedBySpeed,
                             List<StrategyComparison> rankedByScalability) {
        this.rankedByOverall = rankedByOverall;
        this.rankedByMemory = rankedByMemory;
        this.rankedBySpeed = rankedBySpeed;
        this.rankedByScalability = rankedByScalability;
    }
    
    // Getters
    public List<StrategyComparison> getRankedByOverall() { return rankedByOverall; }
    public List<StrategyComparison> getRankedByMemory() { return rankedByMemory; }
    public List<StrategyComparison> getRankedBySpeed() { return rankedBySpeed; }
    public List<StrategyComparison> getRankedByScalability() { return rankedByScalability; }
    
    /**
     * Get rank of strategy by overall performance (1-based).
     */
    public int getOverallRank(String strategyName) {
        for (int i = 0; i < rankedByOverall.size(); i++) {
            if (rankedByOverall.get(i).getStrategyName().equals(strategyName)) {
                return i + 1;
            }
        }
        return -1;
    }
    
    /**
     * Get best strategy overall.
     */
    public StrategyComparison getBestOverall() {
        return rankedByOverall.isEmpty() ? null : rankedByOverall.get(0);
    }
    
    /**
     * Get worst strategy overall.
     */
    public StrategyComparison getWorstOverall() {
        return rankedByOverall.isEmpty() ? null : rankedByOverall.get(rankedByOverall.size() - 1);
    }
}

/**
 * Benchmark summary statistics.
 */
class BenchmarkSummary {
    
    private final StrategyComparison bestStrategy;
    private final StrategyComparison worstStrategy;
    private final double averageScore;
    private final double maxSpeedDifferencePercent;
    private final double maxMemoryDifferencePercent;
    
    public BenchmarkSummary(StrategyComparison bestStrategy,
                           StrategyComparison worstStrategy,
                           double averageScore,
                           double maxSpeedDifferencePercent,
                           double maxMemoryDifferencePercent) {
        this.bestStrategy = bestStrategy;
        this.worstStrategy = worstStrategy;
        this.averageScore = averageScore;
        this.maxSpeedDifferencePercent = maxSpeedDifferencePercent;
        this.maxMemoryDifferencePercent = maxMemoryDifferencePercent;
    }
    
    // Getters
    public StrategyComparison getBestStrategy() { return bestStrategy; }
    public StrategyComparison getWorstStrategy() { return worstStrategy; }
    public double getAverageScore() { return averageScore; }
    public double getMaxSpeedDifferencePercent() { return maxSpeedDifferencePercent; }
    public double getMaxMemoryDifferencePercent() { return maxMemoryDifferencePercent; }
    
    /**
     * Get performance spread (difference between best and worst).
     */
    public double getPerformanceSpread() {
        if (bestStrategy == null || worstStrategy == null) return 0;
        return bestStrategy.getOverallScore() - worstStrategy.getOverallScore();
    }
    
    /**
     * Check if there's significant performance variation.
     */
    public boolean hasSignificantVariation() {
        return getPerformanceSpread() > 20 || 
               maxSpeedDifferencePercent > 50 || 
               maxMemoryDifferencePercent > 100;
    }
}
