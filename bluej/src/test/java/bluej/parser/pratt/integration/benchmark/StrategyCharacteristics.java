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

import java.util.Set;
import java.util.EnumSet;

/**
 * Describes the characteristics and strengths of parser integration strategies.
 * 
 * Used by the benchmark framework to:
 * - Categorize strategies by their primary approach
 * - Identify expected strengths and weaknesses
 * - Provide context for benchmark results
 * - Guide strategy selection recommendations
 * 
 * Each strategy has unique characteristics that make it suitable for different scenarios.
 * This class captures those characteristics to help interpret benchmark results.
 */
public class StrategyCharacteristics {
    private final String strategyName;
    private final String description;
    private final IntegrationApproach approach;
    private final Set<PerformanceStrength> strengths;
    private final Set<PerformanceWeakness> weaknesses;
    private final Set<UseCase> idealUseCases;
    private final ComplexityLevel implementationComplexity;
    private final boolean maintainsCallbackIntegrity;
    private final boolean supportsErrorRecovery;
    private final boolean supportsIncrementalParsing;
    
    public StrategyCharacteristics(String strategyName,
                                 String description,
                                 IntegrationApproach approach,
                                 Set<PerformanceStrength> strengths,
                                 Set<PerformanceWeakness> weaknesses,
                                 Set<UseCase> idealUseCases,
                                 ComplexityLevel implementationComplexity,
                                 boolean maintainsCallbackIntegrity,
                                 boolean supportsErrorRecovery,
                                 boolean supportsIncrementalParsing) {
        this.strategyName = strategyName;
        this.description = description;
        this.approach = approach;
        this.strengths = EnumSet.copyOf(strengths);
        this.weaknesses = EnumSet.copyOf(weaknesses);
        this.idealUseCases = EnumSet.copyOf(idealUseCases);
        this.implementationComplexity = implementationComplexity;
        this.maintainsCallbackIntegrity = maintainsCallbackIntegrity;
        this.supportsErrorRecovery = supportsErrorRecovery;
        this.supportsIncrementalParsing = supportsIncrementalParsing;
    }
    
    // Getters
    public String getStrategyName() { return strategyName; }
    public String getDescription() { return description; }
    public IntegrationApproach getApproach() { return approach; }
    public Set<PerformanceStrength> getStrengths() { return EnumSet.copyOf(strengths); }
    public Set<PerformanceWeakness> getWeaknesses() { return EnumSet.copyOf(weaknesses); }
    public Set<UseCase> getIdealUseCases() { return EnumSet.copyOf(idealUseCases); }
    public ComplexityLevel getImplementationComplexity() { return implementationComplexity; }
    public boolean getMaintainsCallbackIntegrity() { return maintainsCallbackIntegrity; }
    public boolean getSupportsErrorRecovery() { return supportsErrorRecovery; }
    public boolean getSupportsIncrementalParsing() { return supportsIncrementalParsing; }
    
    /**
     * Primary integration approach categories.
     */
    public enum IntegrationApproach {
        DIRECT_CALLBACK("Direct callback integration with minimal overhead"),
        AST_BASED("AST-based transformation with structured access"),
        AST_TRANSFORMATION("AST-based transformation with structured access"),
        LAZY_EVALUATION("Lazy evaluation approach for memory efficiency"),
        HYBRID_PATTERN("Hybrid approach combining multiple techniques"),
        ERROR_FOCUSED("Specialized error handling and recovery"),
        EXTERNAL_PARSER("Integration with external parser systems");
        
        private final String description;
        
        IntegrationApproach(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    /**
     * Performance strengths that strategies may exhibit.
     */
    public enum PerformanceStrength {
        LOW_MEMORY_OVERHEAD("Minimal memory allocation and usage"),
        MEMORY_EFFICIENT("Efficient memory usage patterns"),
        FAST_PARSING("Excellent parsing speed and throughput"),
        EXCELLENT_SCALABILITY("Linear scaling with input size"),
        EXCELLENT_ERROR_RECOVERY("Strong error handling and recovery"),
        ROBUST_ERROR_RECOVERY("Strong error handling and recovery"),
        CALLBACK_INTEGRITY("Guaranteed callback pairing"),
        INCREMENTAL_PARSING("Support for incremental parsing"),
        DEFERRED_COMPUTATION("Lazy evaluation capabilities"),
        CLEAN_SEPARATION("Clean separation of concerns"),
        PREDICTABLE_PERFORMANCE("Consistent performance characteristics"),
        SIMPLE_IMPLEMENTATION("Easy to implement and maintain"),
        FLEXIBLE_INTEGRATION("Easy to integrate with different systems");
        
        // Removed alias enum - use PerformanceStrength directly
        
        private final String description;
        
        PerformanceStrength(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    /**
     * Performance weaknesses that strategies may have.
     */
    public enum PerformanceWeakness {
        HIGH_MEMORY_OVERHEAD("Significant memory allocation"),
        HIGHER_MEMORY_USAGE("Increased memory consumption"),
        AST_CONSTRUCTION_OVERHEAD("Overhead from AST construction"),
        SLOWER_PARSING("Reduced parsing speed"),
        POOR_SCALABILITY("Non-linear scaling characteristics"),
        LIMITED_ERROR_RECOVERY("Basic or unreliable error handling"),
        CALLBACK_COMPLEXITY("Complex callback management"),
        DEFERRED_ERROR_DETECTION("Delayed error detection"),
        VARIABLE_PERFORMANCE("Variable performance characteristics"),
        CACHE_MANAGEMENT_COMPLEXITY("Complex cache management"),
        IMPLEMENTATION_COMPLEXITY("Difficult to implement correctly"),
        INTEGRATION_COMPLEXITY("Complex integration requirements"),
        PERFORMANCE_VARIABILITY("Inconsistent performance characteristics");
        
        // Removed alias enum - use PerformanceWeakness directly
        
        private final String description;
        
        PerformanceWeakness(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    /**
     * Use cases where different strategies excel.
     */
    public enum UseCase {
        IDE_REAL_TIME_PARSING("Real-time parsing in IDE environments"),
        DEVELOPMENT_TOOLING("Development tooling and IDE features"),
        LARGE_CODEBASE_ANALYSIS("Analysis of large codebases"),
        LARGE_FILES("Processing of large source files"),
        ERROR_PRONE_CODE("Handling incomplete or malformed code"),
        INCREMENTAL_UPDATES("Incremental code analysis and updates"),
        PARTIAL_ANALYSIS("Partial code analysis scenarios"),
        LOW_MEMORY("Low memory environment usage"),
        PERFORMANCE_CRITICAL("Performance-critical applications"),
        SIMPLE_INTEGRATION("Simple integration scenarios"),
        COMPLEX_ANALYSIS("Complex semantic analysis requirements"),
        BATCH_PROCESSING("Batch processing of multiple files");
        
        private final String description;
        
        UseCase(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    /**
     * Implementation complexity levels.
     */
    public enum ComplexityLevel {
        SIMPLE("Simple to implement and maintain"),
        MODERATE("Moderate implementation complexity"),
        COMPLEX("Complex implementation requiring expertise"),
        VERY_COMPLEX("Very complex, requires deep understanding");
        
        // Removed alias enum - use ComplexityLevel directly
        
        private final String description;
        
        ComplexityLevel(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    /**
     * Predefined characteristics for the Direct Callback Integration strategy.
     */
    public static StrategyCharacteristics directCallbackIntegration() {
        return new StrategyCharacteristics(
                "Direct Callback Integration",
                "Direct integration with AutoCloseable callback scopes for guaranteed pairing",
                IntegrationApproach.DIRECT_CALLBACK,
                EnumSet.of(
                        PerformanceStrength.LOW_MEMORY_OVERHEAD,
                        PerformanceStrength.FAST_PARSING,
                        PerformanceStrength.CALLBACK_INTEGRITY,
                        PerformanceStrength.SIMPLE_IMPLEMENTATION
                ),
                EnumSet.of(
                        PerformanceWeakness.LIMITED_ERROR_RECOVERY
                ),
                EnumSet.of(
                        UseCase.PERFORMANCE_CRITICAL,
                        UseCase.SIMPLE_INTEGRATION,
                        UseCase.IDE_REAL_TIME_PARSING
                ),
                ComplexityLevel.SIMPLE,
                true,
                false,
                false
        );
    }
    
    /**
     * Predefined characteristics for the AST Visitor Pattern strategy.
     */
    public static StrategyCharacteristics astVisitorPattern() {
        return new StrategyCharacteristics(
                "AST Visitor Pattern",
                "Traditional visitor pattern for structured AST traversal",
                IntegrationApproach.AST_TRANSFORMATION,
                EnumSet.of(
                        PerformanceStrength.FLEXIBLE_INTEGRATION,
                        PerformanceStrength.SIMPLE_IMPLEMENTATION
                ),
                EnumSet.of(
                        PerformanceWeakness.HIGH_MEMORY_OVERHEAD,
                        PerformanceWeakness.SLOWER_PARSING
                ),
                EnumSet.of(
                        UseCase.COMPLEX_ANALYSIS,
                        UseCase.BATCH_PROCESSING
                ),
                ComplexityLevel.MODERATE,
                false,
                false,
                false
        );
    }
    
    /**
     * Predefined characteristics for the Lazy AST Transformation strategy.
     */
    public static StrategyCharacteristics lazyAstTransformation() {
        return new StrategyCharacteristics(
                "Lazy AST Transformation",
                "Lazy evaluation of AST transformations for memory efficiency",
                IntegrationApproach.AST_TRANSFORMATION,
                EnumSet.of(
                        PerformanceStrength.LOW_MEMORY_OVERHEAD,
                        PerformanceStrength.EXCELLENT_SCALABILITY,
                        PerformanceStrength.INCREMENTAL_PARSING
                ),
                EnumSet.of(
                        PerformanceWeakness.IMPLEMENTATION_COMPLEXITY,
                        PerformanceWeakness.PERFORMANCE_VARIABILITY
                ),
                EnumSet.of(
                        UseCase.LARGE_CODEBASE_ANALYSIS,
                        UseCase.INCREMENTAL_UPDATES
                ),
                ComplexityLevel.COMPLEX,
                false,
                false,
                true
        );
    }
    
    /**
     * Predefined characteristics for the Hybrid Result Pattern strategy.
     */
    public static StrategyCharacteristics hybridResultPattern() {
        return new StrategyCharacteristics(
                "Hybrid Result Pattern",
                "Monadic error handling with sophisticated result aggregation",
                IntegrationApproach.HYBRID_PATTERN,
                EnumSet.of(
                        PerformanceStrength.ROBUST_ERROR_RECOVERY,
                        PerformanceStrength.FLEXIBLE_INTEGRATION
                ),
                EnumSet.of(
                        PerformanceWeakness.IMPLEMENTATION_COMPLEXITY,
                        PerformanceWeakness.HIGH_MEMORY_OVERHEAD
                ),
                EnumSet.of(
                        UseCase.ERROR_PRONE_CODE,
                        UseCase.COMPLEX_ANALYSIS
                ),
                ComplexityLevel.COMPLEX,
                false,
                true,
                false
        );
    }
    
    /**
     * Predefined characteristics for the K2 Parser Integration strategy.
     */
    public static StrategyCharacteristics k2ParserIntegration() {
        return new StrategyCharacteristics(
                "K2 Parser Integration",
                "Integration with Kotlin K2 compiler frontend for advanced features",
                IntegrationApproach.EXTERNAL_PARSER,
                EnumSet.of(
                        PerformanceStrength.ROBUST_ERROR_RECOVERY,
                        PerformanceStrength.INCREMENTAL_PARSING,
                        PerformanceStrength.FAST_PARSING
                ),
                EnumSet.of(
                        PerformanceWeakness.INTEGRATION_COMPLEXITY,
                        PerformanceWeakness.HIGH_MEMORY_OVERHEAD
                ),
                EnumSet.of(
                        UseCase.IDE_REAL_TIME_PARSING,
                        UseCase.INCREMENTAL_UPDATES,
                        UseCase.ERROR_PRONE_CODE
                ),
                ComplexityLevel.VERY_COMPLEX,
                false,
                true,
                true
        );
    }
    
    /**
     * Predefined characteristics for the Error Recovery Integration strategy.
     */
    public static StrategyCharacteristics errorRecoveryIntegration() {
        return new StrategyCharacteristics(
                "Error Recovery Integration",
                "Specialized error recovery with continuation strategies",
                IntegrationApproach.ERROR_FOCUSED,
                EnumSet.of(
                        PerformanceStrength.ROBUST_ERROR_RECOVERY,
                        PerformanceStrength.CALLBACK_INTEGRITY
                ),
                EnumSet.of(
                        PerformanceWeakness.SLOWER_PARSING,
                        PerformanceWeakness.IMPLEMENTATION_COMPLEXITY
                ),
                EnumSet.of(
                        UseCase.ERROR_PRONE_CODE,
                        UseCase.IDE_REAL_TIME_PARSING
                ),
                ComplexityLevel.COMPLEX,
                true,
                true,
                false
        );
    }
    
    /**
     * Predefined characteristics for the Common Parsing Scenarios strategy.
     */
    public static StrategyCharacteristics commonParsingScenarios() {
        return new StrategyCharacteristics(
                "Common Parsing Scenarios",
                "Optimized handling of common parsing patterns and scenarios",
                IntegrationApproach.HYBRID_PATTERN,
                EnumSet.of(
                        PerformanceStrength.FAST_PARSING,
                        PerformanceStrength.LOW_MEMORY_OVERHEAD,
                        PerformanceStrength.SIMPLE_IMPLEMENTATION
                ),
                EnumSet.of(
                        PerformanceWeakness.LIMITED_ERROR_RECOVERY,
                        PerformanceWeakness.POOR_SCALABILITY
                ),
                EnumSet.of(
                        UseCase.SIMPLE_INTEGRATION,
                        UseCase.BATCH_PROCESSING
                ),
                ComplexityLevel.MODERATE,
                false,
                false,
                false
        );
    }
    
    /**
     * Check if this strategy is suitable for a specific use case.
     */
    public boolean isSuitableFor(UseCase useCase) {
        return idealUseCases.contains(useCase);
    }
    
    /**
     * Check if this strategy has a specific strength.
     */
    public boolean hasStrength(PerformanceStrength strength) {
        return strengths.contains(strength);
    }
    
    /**
     * Check if this strategy has a specific weakness.
     */
    public boolean hasWeakness(PerformanceWeakness weakness) {
        return weaknesses.contains(weakness);
    }
    
    /**
     * Calculate a suitability score for a specific use case (0.0 to 1.0).
     */
    public double getSuitabilityScore(UseCase useCase) {
        if (!isSuitableFor(useCase)) {
            return 0.0;
        }
        
        // Base score for being suitable
        double score = 0.5;
        
        // Add points for relevant strengths
        score += strengths.size() * 0.1;
        
        // Subtract points for weaknesses
        score -= weaknesses.size() * 0.05;
        
        // Adjust based on implementation complexity
        switch (implementationComplexity) {
            case SIMPLE: score += 0.1; break;
            case MODERATE: break;
            case COMPLEX: score -= 0.05; break;
            case VERY_COMPLEX: score -= 0.1; break;
        }
        
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    @Override
    public String toString() {
        return String.format("StrategyCharacteristics{name='%s', approach=%s, complexity=%s}",
                strategyName, approach, implementationComplexity);
    }
    
    /**
     * Builder for StrategyCharacteristics.
     */
    public static class Builder {
        private String strategyName;
        private String description;
        private IntegrationApproach approach;
        private Set<PerformanceStrength> strengths = EnumSet.noneOf(PerformanceStrength.class);
        private Set<PerformanceWeakness> weaknesses = EnumSet.noneOf(PerformanceWeakness.class);
        private Set<UseCase> idealUseCases = EnumSet.noneOf(UseCase.class);
        private ComplexityLevel implementationComplexity = ComplexityLevel.MODERATE;
        private boolean maintainsCallbackIntegrity = false;
        private boolean supportsErrorRecovery = false;
        private boolean supportsIncrementalParsing = false;
        
        public Builder strategyName(String strategyName) {
            this.strategyName = strategyName;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder approach(IntegrationApproach approach) {
            this.approach = approach;
            return this;
        }
        
        public Builder addStrength(PerformanceStrength strength) {
            this.strengths.add(strength);
            return this;
        }
        
        public Builder addWeakness(PerformanceWeakness weakness) {
            this.weaknesses.add(weakness);
            return this;
        }
        
        public Builder addIdealUseCase(UseCase useCase) {
            this.idealUseCases.add(useCase);
            return this;
        }
        
        public Builder implementationComplexity(ComplexityLevel complexity) {
            this.implementationComplexity = complexity;
            return this;
        }
        
        public Builder maintainsCallbackIntegrity(boolean maintains) {
            this.maintainsCallbackIntegrity = maintains;
            return this;
        }
        
        public Builder supportsErrorRecovery(boolean supports) {
            this.supportsErrorRecovery = supports;
            return this;
        }
        
        public Builder supportsIncrementalParsing(boolean supports) {
            this.supportsIncrementalParsing = supports;
            return this;
        }
        
        public StrategyCharacteristics build() {
            return new StrategyCharacteristics(strategyName, description, approach, strengths,
                                             weaknesses, idealUseCases, implementationComplexity,
                                             maintainsCallbackIntegrity, supportsErrorRecovery,
                                             supportsIncrementalParsing);
        }
    }
    
    // Compatibility aliases for backward compatibility
    /**
     * @deprecated Use PerformanceStrength instead
     */
    @Deprecated
    public static class Strength {
        public static final PerformanceStrength LOW_MEMORY_OVERHEAD = PerformanceStrength.LOW_MEMORY_OVERHEAD;
        public static final PerformanceStrength MEMORY_EFFICIENT = PerformanceStrength.MEMORY_EFFICIENT;
        public static final PerformanceStrength FAST_PARSING = PerformanceStrength.FAST_PARSING;
        public static final PerformanceStrength EXCELLENT_SCALABILITY = PerformanceStrength.EXCELLENT_SCALABILITY;
        public static final PerformanceStrength EXCELLENT_ERROR_RECOVERY = PerformanceStrength.EXCELLENT_ERROR_RECOVERY;
        public static final PerformanceStrength ROBUST_ERROR_RECOVERY = PerformanceStrength.ROBUST_ERROR_RECOVERY;
        public static final PerformanceStrength CALLBACK_INTEGRITY = PerformanceStrength.CALLBACK_INTEGRITY;
        public static final PerformanceStrength INCREMENTAL_PARSING = PerformanceStrength.INCREMENTAL_PARSING;
        public static final PerformanceStrength DEFERRED_COMPUTATION = PerformanceStrength.DEFERRED_COMPUTATION;
        public static final PerformanceStrength CLEAN_SEPARATION = PerformanceStrength.CLEAN_SEPARATION;
        public static final PerformanceStrength PREDICTABLE_PERFORMANCE = PerformanceStrength.PREDICTABLE_PERFORMANCE;
        public static final PerformanceStrength SIMPLE_IMPLEMENTATION = PerformanceStrength.SIMPLE_IMPLEMENTATION;
        public static final PerformanceStrength FLEXIBLE_INTEGRATION = PerformanceStrength.FLEXIBLE_INTEGRATION;
    }
    
    /**
     * @deprecated Use PerformanceWeakness instead
     */
    @Deprecated
    public static class Weakness {
        public static final PerformanceWeakness HIGH_MEMORY_OVERHEAD = PerformanceWeakness.HIGH_MEMORY_OVERHEAD;
        public static final PerformanceWeakness HIGHER_MEMORY_USAGE = PerformanceWeakness.HIGHER_MEMORY_USAGE;
        public static final PerformanceWeakness AST_CONSTRUCTION_OVERHEAD = PerformanceWeakness.AST_CONSTRUCTION_OVERHEAD;
        public static final PerformanceWeakness SLOWER_PARSING = PerformanceWeakness.SLOWER_PARSING;
        public static final PerformanceWeakness POOR_SCALABILITY = PerformanceWeakness.POOR_SCALABILITY;
        public static final PerformanceWeakness LIMITED_ERROR_RECOVERY = PerformanceWeakness.LIMITED_ERROR_RECOVERY;
        public static final PerformanceWeakness CALLBACK_COMPLEXITY = PerformanceWeakness.CALLBACK_COMPLEXITY;
        public static final PerformanceWeakness DEFERRED_ERROR_DETECTION = PerformanceWeakness.DEFERRED_ERROR_DETECTION;
        public static final PerformanceWeakness VARIABLE_PERFORMANCE = PerformanceWeakness.VARIABLE_PERFORMANCE;
        public static final PerformanceWeakness CACHE_MANAGEMENT_COMPLEXITY = PerformanceWeakness.CACHE_MANAGEMENT_COMPLEXITY;
        public static final PerformanceWeakness IMPLEMENTATION_COMPLEXITY = PerformanceWeakness.IMPLEMENTATION_COMPLEXITY;
        public static final PerformanceWeakness INTEGRATION_COMPLEXITY = PerformanceWeakness.INTEGRATION_COMPLEXITY;
        public static final PerformanceWeakness PERFORMANCE_VARIABILITY = PerformanceWeakness.PERFORMANCE_VARIABILITY;
    }
    
    /**
     * @deprecated Use ComplexityLevel instead
     */
    @Deprecated
    public static class ImplementationComplexity {
        public static final ComplexityLevel SIMPLE = ComplexityLevel.SIMPLE;
        public static final ComplexityLevel MODERATE = ComplexityLevel.MODERATE;
        public static final ComplexityLevel COMPLEX = ComplexityLevel.COMPLEX;
        public static final ComplexityLevel VERY_COMPLEX = ComplexityLevel.VERY_COMPLEX;
    }
}