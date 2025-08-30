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

import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides performance ranking and analysis for parser integration strategies.
 * 
 * This class takes a list of strategy comparisons and provides various ranking
 * mechanisms based on different performance criteria:
 * - Overall performance score
 * - Specific use case suitability
 * - Individual metrics (speed, memory, scalability)
 */
public class PerformanceRanking {
    private final List<StrategyComparison> strategies;
    private final Map<String, Integer> overallRanking;
    
    public PerformanceRanking(List<StrategyComparison> strategies) {
        this.strategies = new ArrayList<>(strategies);
        this.overallRanking = calculateOverallRanking();
    }
    
    /**
     * Get strategies ranked by overall performance score (best first).
     */
    public List<StrategyComparison> getRankedByOverall() {
        return strategies.stream()
                .sorted((a, b) -> Double.compare(b.getOverallScore(), a.getOverallScore()))
                .collect(Collectors.toList());
    }
    
    /**
     * Get strategies ranked by parsing speed (fastest first).
     */
    public List<StrategyComparison> getRankedBySpeed() {
        return strategies.stream()
                .sorted((a, b) -> Double.compare(
                        a.getResult().getPerformanceMetrics().getAvgParseTimeMs(),
                        b.getResult().getPerformanceMetrics().getAvgParseTimeMs()))
                .collect(Collectors.toList());
    }
    
    /**
     * Get strategies ranked by memory efficiency (lowest usage first).
     */
    public List<StrategyComparison> getRankedByMemoryEfficiency() {
        return strategies.stream()
                .sorted((a, b) -> Double.compare(
                        a.getResult().getMemoryMetrics().getPeakUsageMB(),
                        b.getResult().getMemoryMetrics().getPeakUsageMB()))
                .collect(Collectors.toList());
    }
    
    /**
     * Get strategies ranked by scalability (best scaling first).
     */
    public List<StrategyComparison> getRankedByScalability() {
        return strategies.stream()
                .sorted((a, b) -> Double.compare(
                        b.getResult().getScalabilityMetrics().getScalabilityScore(),
                        a.getResult().getScalabilityMetrics().getScalabilityScore()))
                .collect(Collectors.toList());
    }
    
    /**
     * Get strategies ranked by suitability for a specific use case.
     */
    public List<StrategyComparison> getRankedByUseCase(StrategyCharacteristics.UseCase useCase) {
        return strategies.stream()
                .sorted((a, b) -> {
                    double scoreA = a.getCharacteristics().getSuitabilityScore(useCase);
                    double scoreB = b.getCharacteristics().getSuitabilityScore(useCase);
                    return Double.compare(scoreB, scoreA);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Get the top N strategies by overall performance.
     */
    public List<StrategyComparison> getTopStrategies(int n) {
        return getRankedByOverall().stream()
                .limit(n)
                .collect(Collectors.toList());
    }
    
    /**
     * Get the best strategy for a specific use case.
     */
    public StrategyComparison getBestForUseCase(StrategyCharacteristics.UseCase useCase) {
        return getRankedByUseCase(useCase).stream()
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Get the overall winner (best performing strategy).
     */
    public StrategyComparison getWinner() {
        return getRankedByOverall().stream()
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Get the rank of a specific strategy (1-based, lower is better).
     */
    public int getRank(String strategyName) {
        return overallRanking.getOrDefault(strategyName, strategies.size() + 1);
    }
    
    /**
     * Get performance comparison matrix between all strategies.
     */
    public Map<String, Map<String, Double>> getComparisonMatrix() {
        Map<String, Map<String, Double>> matrix = new HashMap<>();
        
        for (StrategyComparison strategy : strategies) {
            Map<String, Double> comparisons = new HashMap<>();
            for (StrategyComparison other : strategies) {
                if (!strategy.getStrategyName().equals(other.getStrategyName())) {
                    double advantage = strategy.getPerformanceAdvantage(other);
                    comparisons.put(other.getStrategyName(), advantage);
                }
            }
            matrix.put(strategy.getStrategyName(), comparisons);
        }
        
        return matrix;
    }
    
    /**
     * Get strategies that are recommended for specific scenarios.
     */
    public Map<StrategyCharacteristics.UseCase, StrategyComparison> getRecommendations() {
        Map<StrategyCharacteristics.UseCase, StrategyComparison> recommendations = new HashMap<>();
        
        for (StrategyCharacteristics.UseCase useCase : StrategyCharacteristics.UseCase.values()) {
            StrategyComparison best = getBestForUseCase(useCase);
            if (best != null) {
                recommendations.put(useCase, best);
            }
        }
        
        return recommendations;
    }
    
    /**
     * Check if there's a clear winner or if performance is close.
     */
    public boolean hasClearWinner() {
        List<StrategyComparison> ranked = getRankedByOverall();
        if (ranked.size() < 2) return true;
        
        double topScore = ranked.get(0).getOverallScore();
        double secondScore = ranked.get(1).getOverallScore();
        
        // Clear winner if there's more than 10% difference
        return (topScore - secondScore) / secondScore > 0.1;
    }
    
    /**
     * Get strategies that are very close in performance to the winner.
     */
    public List<StrategyComparison> getCompetitiveStrategies(double thresholdPercent) {
        List<StrategyComparison> ranked = getRankedByOverall();
        if (ranked.isEmpty()) return Collections.emptyList();
        
        double topScore = ranked.get(0).getOverallScore();
        double threshold = topScore * (1.0 - thresholdPercent / 100.0);
        
        return ranked.stream()
                .filter(s -> s.getOverallScore() >= threshold)
                .collect(Collectors.toList());
    }
    
    /**
     * Calculate overall ranking map for quick lookups.
     */
    private Map<String, Integer> calculateOverallRanking() {
        Map<String, Integer> ranking = new HashMap<>();
        List<StrategyComparison> ranked = getRankedByOverall();
        
        for (int i = 0; i < ranked.size(); i++) {
            ranking.put(ranked.get(i).getStrategyName(), i + 1);
        }
        
        return ranking;
    }
    
    /**
     * Get a summary of the ranking results.
     */
    public String getRankingSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Performance Ranking Summary:\n");
        
        List<StrategyComparison> ranked = getRankedByOverall();
        for (int i = 0; i < ranked.size(); i++) {
            StrategyComparison strategy = ranked.get(i);
            summary.append(String.format("%d. %s (Score: %.1f)\n", 
                    i + 1, strategy.getStrategyName(), strategy.getOverallScore()));
        }
        
        return summary.toString();
    }
    
    @Override
    public String toString() {
        return String.format("PerformanceRanking{strategies=%d, winner=%s}", 
                strategies.size(), 
                getWinner() != null ? getWinner().getStrategyName() : "none");
    }
}