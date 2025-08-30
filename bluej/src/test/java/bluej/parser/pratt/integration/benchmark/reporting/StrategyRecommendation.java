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

/**
 * Represents a strategy recommendation based on benchmark results and analysis.
 * 
 * Provides contextual guidance on when and why to use specific parser integration
 * strategies based on measured performance characteristics and use case requirements.
 */
public class StrategyRecommendation {
    
    /**
     * Categories for strategy recommendations
     */
    public enum RecommendationCategory {
        PERFORMANCE("Performance"),
        MEMORY_EFFICIENCY("Memory Efficiency"),
        SCALABILITY("Scalability"),
        ERROR_RESILIENCE("Error Resilience"),
        DEVELOPMENT_SIMPLICITY("Development Simplicity"),
        GENERAL_PURPOSE("General Purpose"),
        COMPATIBILITY("Compatibility"),
        ROBUSTNESS("Robustness"),
        MEMORY("Memory"),
        DEVELOPMENT_ENVIRONMENT("Development Environment"),
        RESOURCE_CONSTRAINED("Resource Constrained");
        
        private final String displayName;
        
        RecommendationCategory(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Use cases for strategy recommendations
     */
    public enum UseCase {
        HIGH_PERFORMANCE("High Performance Applications"),
        MEMORY_CONSTRAINED("Memory Constrained Environments"),
        PERFORMANCE_CRITICAL("Performance Critical Applications"),
        LARGE_SCALE("Large Scale Applications"),
        ERROR_PRONE_CODE("Error-Prone Code Scenarios"),
        GENERAL_PURPOSE("General Purpose Applications"),
        DEVELOPMENT_TOOLING("Development Tooling"),
        DEVELOPMENT_ENVIRONMENT("Development Environment"),
        RESOURCE_CONSTRAINED("Resource Constrained Environment");
        
        private final String description;
        
        UseCase(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private final RecommendationCategory category;
    private final String strategyName;
    private final String reasoning;
    private final UseCase useCase;
    private final double confidence;
    
    /**
     * Creates a new strategy recommendation.
     * 
     * @param category The category of recommendation
     * @param strategyName The name of the recommended strategy
     * @param reasoning The reasoning behind the recommendation
     * @param useCase The specific use case for this recommendation
     * @param confidence Confidence level in this recommendation (0.0 to 1.0)
     */
    public StrategyRecommendation(RecommendationCategory category, String strategyName,
                                String reasoning, UseCase useCase, double confidence) {
        this.category = category;
        this.strategyName = strategyName;
        this.reasoning = reasoning;
        this.useCase = useCase;
        this.confidence = Math.max(0.0, Math.min(1.0, confidence)); // Clamp between 0.0 and 1.0
    }
    
    /**
     * Creates a new strategy recommendation with default confidence.
     * 
     * @param category The category of recommendation
     * @param strategyName The name of the recommended strategy
     * @param reasoning The reasoning behind the recommendation  
     * @param useCase The specific use case for this recommendation
     */
    public StrategyRecommendation(RecommendationCategory category, String strategyName,
                                String reasoning, UseCase useCase) {
        this(category, strategyName, reasoning, useCase, 0.8);
    }
    
    // Getters
    
    public RecommendationCategory getCategory() {
        return category;
    }
    
    public String getStrategyName() {
        return strategyName;
    }
    
    public String getReasoning() {
        return reasoning;
    }
    
    public UseCase getUseCase() {
        return useCase;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    /**
     * Gets a human-readable confidence level description.
     */
    public String getConfidenceDescription() {
        if (confidence >= 0.9) return "Very High";
        if (confidence >= 0.8) return "High";  
        if (confidence >= 0.6) return "Moderate";
        if (confidence >= 0.4) return "Low";
        return "Very Low";
    }
    
    @Override
    public String toString() {
        return String.format("StrategyRecommendation{category=%s, strategy='%s', confidence=%.2f}",
                           category, strategyName, confidence);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        StrategyRecommendation that = (StrategyRecommendation) obj;
        return Double.compare(that.confidence, confidence) == 0 &&
               category == that.category &&
               strategyName.equals(that.strategyName) &&
               reasoning.equals(that.reasoning) &&
               useCase == that.useCase;
    }
    
    @Override
    public int hashCode() {
        int result = category.hashCode();
        result = 31 * result + strategyName.hashCode();
        result = 31 * result + reasoning.hashCode();
        result = 31 * result + useCase.hashCode();
        result = 31 * result + Double.hashCode(confidence);
        return result;
    }
}