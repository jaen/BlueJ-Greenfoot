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

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

import bluej.parser.pratt.integration.benchmark.BenchmarkConfiguration;
import bluej.parser.pratt.integration.benchmark.StrategyCharacteristics;
import bluej.parser.pratt.integration.benchmark.metrics.*;

/**
 * Markdown report builder for parser integration strategy benchmarks.
 * 
 * Converts BenchmarkReport objects into well-formatted Markdown suitable for:
 * - GitHub/GitLab documentation
 * - Technical reports and specifications
 * - Wiki documentation
 * - README files and development docs
 * 
 * Generates structured Markdown with:
 * - Table of contents
 * - Executive summary
 * - Detailed performance analysis
 * - Visual charts using ASCII art
 * - Strategy recommendations
 * - Technical appendices
 */
class MarkdownReportBuilder {
    
    private static final String NEWLINE = "\n";
    private static final String DOUBLE_NEWLINE = "\n\n";
    
    /**
     * Build complete Markdown report from benchmark results.
     */
    public String buildMarkdownReport(BenchmarkReport report) {
        StringBuilder md = new StringBuilder();
        
        // Title and metadata
        md.append("# Parser Integration Strategy Benchmark Report").append(DOUBLE_NEWLINE);
        
        // Report metadata
        addReportMetadata(md, report);
        
        // Table of contents
        addTableOfContents(md);
        
        // Executive summary
        addExecutiveSummary(md, report);
        
        // Performance analysis
        addPerformanceAnalysis(md, report);
        
        // Strategy comparisons
        addStrategyComparisons(md, report);
        
        // Detailed metrics
        addDetailedMetrics(md, report);
        
        // Recommendations
        addRecommendations(md, report);
        
        // Configuration appendix
        addConfigurationAppendix(md, report);
        
        return md.toString();
    }
    
    /**
     * Add report metadata section.
     */
    private void addReportMetadata(StringBuilder md, BenchmarkReport report) {
        md.append("**Generated:** ").append(report.getReportTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("  ").append(NEWLINE);
        md.append("**Scope:** ").append(report.getReportScope()).append("  ").append(NEWLINE);
        md.append("**Strategies Tested:** ").append(report.getStrategyCount()).append("  ").append(NEWLINE);
        md.append("**Results Available:** ").append(report.hasResults() ? "✅ Yes" : "❌ No").append("  ");
        md.append(DOUBLE_NEWLINE);
    }
    
    /**
     * Add table of contents.
     */
    private void addTableOfContents(StringBuilder md) {
        md.append("## Table of Contents").append(DOUBLE_NEWLINE);
        md.append("- [Executive Summary](#executive-summary)").append(NEWLINE);
        md.append("- [Performance Analysis](#performance-analysis)").append(NEWLINE);
        md.append("  - [Overall Ranking](#overall-ranking)").append(NEWLINE);
        md.append("  - [Memory Usage](#memory-usage)").append(NEWLINE);
        md.append("  - [Speed Analysis](#speed-analysis)").append(NEWLINE);
        md.append("  - [Scalability](#scalability)").append(NEWLINE);
        md.append("- [Strategy Comparisons](#strategy-comparisons)").append(NEWLINE);
        md.append("- [Detailed Metrics](#detailed-metrics)").append(NEWLINE);
        md.append("- [Recommendations](#recommendations)").append(NEWLINE);
        md.append("- [Configuration](#configuration)").append(DOUBLE_NEWLINE);
    }
    
    /**
     * Add executive summary section.
     */
    private void addExecutiveSummary(StringBuilder md, BenchmarkReport report) {
        md.append("## Executive Summary").append(DOUBLE_NEWLINE);
        
        if (!report.hasResults()) {
            md.append("⚠️ **No benchmark results available for analysis.**").append(DOUBLE_NEWLINE);
            return;
        }
        
        BenchmarkSummary summary = report.getSummary();
        
        md.append("### Key Findings").append(DOUBLE_NEWLINE);
        
        if (summary.getBestStrategy() != null) {
            md.append("🏆 **Best Overall Strategy:** `").append(summary.getBestStrategy().getStrategyName())
              .append("` (Score: ").append(String.format("%.2f", summary.getBestStrategy().getOverallScore())).append(")").append(DOUBLE_NEWLINE);
        }
        
        md.append("📊 **Performance Spread:** ").append(String.format("%.1f%%", summary.getPerformanceSpread() * 100)).append(DOUBLE_NEWLINE);
        md.append("🚀 **Max Speed Difference:** ").append(String.format("%.1f%%", summary.getMaxSpeedDifferencePercent())).append(DOUBLE_NEWLINE);
        md.append("🧠 **Max Memory Difference:** ").append(String.format("%.1f%%", summary.getMaxMemoryDifferencePercent())).append(DOUBLE_NEWLINE);
        
        if (summary.hasSignificantVariation()) {
            md.append("⚡ **Significant performance variation detected** - strategy choice matters for your use case.").append(DOUBLE_NEWLINE);
        } else {
            md.append("📈 **Performance is relatively consistent** across strategies.").append(DOUBLE_NEWLINE);
        }
    }
    
    /**
     * Add performance analysis section.
     */
    private void addPerformanceAnalysis(StringBuilder md, BenchmarkReport report) {
        md.append("## Performance Analysis").append(DOUBLE_NEWLINE);
        
        PerformanceRanking ranking = report.getRanking();
        
        // Overall ranking
        md.append("### Overall Ranking").append(DOUBLE_NEWLINE);
        md.append("| Rank | Strategy | Score | Category |").append(NEWLINE);
        md.append("|------|----------|-------|----------|").append(NEWLINE);
        
        for (int i = 0; i < ranking.getRankedByOverall().size(); i++) {
            StrategyComparison comparison = ranking.getRankedByOverall().get(i);
            String medal = i == 0 ? "🥇" : i == 1 ? "🥈" : i == 2 ? "🥉" : String.valueOf(i + 1);
            md.append("| ").append(medal).append(" | `").append(comparison.getStrategyName()).append("` | ")
              .append(String.format("%.2f", comparison.getOverallScore())).append(" | ")
              .append(comparison.getPerformanceCategory().name()).append(" |").append(NEWLINE);
        }
        md.append(DOUBLE_NEWLINE);
        
        // Memory usage analysis
        addMemoryAnalysis(md, ranking);
        
        // Speed analysis
        addSpeedAnalysis(md, ranking);
        
        // Scalability analysis
        addScalabilityAnalysis(md, ranking);
    }
    
    /**
     * Add memory usage analysis.
     */
    private void addMemoryAnalysis(StringBuilder md, PerformanceRanking ranking) {
        md.append("### Memory Usage").append(DOUBLE_NEWLINE);
        md.append("Lower memory usage is better for resource-constrained environments.").append(DOUBLE_NEWLINE);
        
        md.append("| Rank | Strategy | Peak Memory (MB) | Allocation Rate (MB/s) | GC Pressure |").append(NEWLINE);
        md.append("|------|----------|------------------|------------------------|-------------|").append(NEWLINE);
        
        for (int i = 0; i < ranking.getRankedByMemory().size(); i++) {
            StrategyComparison comparison = ranking.getRankedByMemory().get(i);
            var memory = comparison.getResult().getMemoryMetrics();
            String indicator = i == 0 ? "🟢" : i < ranking.getRankedByMemory().size() / 2 ? "🟡" : "🔴";
            
            md.append("| ").append(indicator).append(" ").append(i + 1).append(" | `")
              .append(comparison.getStrategyName()).append("` | ")
              .append(String.format("%.1f", memory.getPeakUsedMemory() / (1024.0 * 1024))).append(" | ")
              .append(String.format("%.1f", memory.getAllocationRate() / (1024.0 * 1024))).append(" | ")
              .append(String.format("%.3f", memory.getGcPressure())).append(" |").append(NEWLINE);
        }
        md.append(DOUBLE_NEWLINE);
    }
    
    /**
     * Add speed analysis.
     */
    private void addSpeedAnalysis(StringBuilder md, PerformanceRanking ranking) {
        md.append("### Speed Analysis").append(DOUBLE_NEWLINE);
        md.append("Higher throughput indicates better parsing performance.").append(DOUBLE_NEWLINE);
        
        md.append("| Rank | Strategy | Throughput (ops/sec) | Avg Callback Time (ms) | Consistency |").append(NEWLINE);
        md.append("|------|----------|---------------------|-------------------------|-------------|").append(NEWLINE);
        
        for (int i = 0; i < ranking.getRankedBySpeed().size(); i++) {
            StrategyComparison comparison = ranking.getRankedBySpeed().get(i);
            var performance = comparison.getResult().getPerformanceMetrics();
            String indicator = i == 0 ? "🚀" : i < ranking.getRankedBySpeed().size() / 2 ? "⚡" : "🐌";
            
            md.append("| ").append(indicator).append(" ").append(i + 1).append(" | `")
              .append(comparison.getStrategyName()).append("` | ")
              .append(String.format("%.1f", performance.getThroughput())).append(" | ")
              .append(String.format("%.3f", performance.getAverageCallbackTimeMs())).append(" | ")
              .append(String.format("%.3f", performance.getConsistency())).append(" |").append(NEWLINE);
        }
        md.append(DOUBLE_NEWLINE);
    }
    
    /**
     * Add scalability analysis.
     */
    private void addScalabilityAnalysis(StringBuilder md, PerformanceRanking ranking) {
        md.append("### Scalability").append(DOUBLE_NEWLINE);
        md.append("R² values closer to 1.0 indicate more predictable scaling behavior.").append(DOUBLE_NEWLINE);
        
        md.append("| Strategy | R² Score | Scaling | Memory Growth | Category |").append(NEWLINE);
        md.append("|----------|----------|---------|---------------|----------|").append(NEWLINE);
        
        for (StrategyComparison comparison : ranking.getRankedByScalability()) {
            var scalability = comparison.getResult().getScalabilityMetrics();
            String scalingIcon = scalability.isLinearScaling() ? "📈" : "📊";
            String categoryIcon = getCategoryIcon(scalability.getCategory());
            
            md.append("| `").append(comparison.getStrategyName()).append("` | ")
              .append(String.format("%.3f", scalability.getRSquared())).append(" | ")
              .append(scalingIcon).append(" ").append(scalability.isLinearScaling() ? "Linear" : "Non-linear").append(" | ")
              .append(String.format("%.2fx", scalability.getMemoryGrowthRate())).append(" | ")
              .append(categoryIcon).append(" ").append(scalability.getCategory().name()).append(" |").append(NEWLINE);
        }
        md.append(DOUBLE_NEWLINE);
    }
    
    /**
     * Add strategy comparisons section.
     */
    private void addStrategyComparisons(StringBuilder md, BenchmarkReport report) {
        md.append("## Strategy Comparisons").append(DOUBLE_NEWLINE);
        
        for (StrategyComparison comparison : report.getStrategyComparisons()) {
            md.append("### ").append(comparison.getStrategyName()).append(DOUBLE_NEWLINE);
            
            // Strategy characteristics
            if (comparison.getCharacteristics() != null) {
                addStrategyCharacteristics(md, comparison.getCharacteristics());
            }
            
            // Performance summary
            md.append("#### Performance Summary").append(DOUBLE_NEWLINE);
            md.append("- **Overall Score:** ").append(String.format("%.2f", comparison.getOverallScore())).append("/100").append(NEWLINE);
            md.append("- **Category:** ").append(comparison.getPerformanceCategory().name()).append(NEWLINE);
            md.append("- **Peak Memory:** ").append(String.format("%.1f MB", comparison.getResult().getMemoryMetrics().getPeakUsedMemory() / (1024.0 * 1024))).append(NEWLINE);
            md.append("- **Throughput:** ").append(String.format("%.1f ops/sec", comparison.getResult().getPerformanceMetrics().getThroughput())).append(NEWLINE);
            md.append("- **Callback Balance:** ").append(comparison.getResult().isCallbacksBalanced() ? "✅ Balanced" : "❌ Imbalanced").append(DOUBLE_NEWLINE);
        }
    }
    
    /**
     * Add strategy characteristics.
     */
    private void addStrategyCharacteristics(StringBuilder md, StrategyCharacteristics characteristics) {
        md.append("#### Overview").append(DOUBLE_NEWLINE);
        md.append(characteristics.getDescription()).append(DOUBLE_NEWLINE);
        
        md.append("**Approach:** ").append(characteristics.getApproach().name()).append(" - ").append(characteristics.getApproach().getDescription()).append(DOUBLE_NEWLINE);
        md.append("**Implementation Complexity:** ").append(characteristics.getImplementationComplexity().name()).append(DOUBLE_NEWLINE);
        
        // Capabilities
        md.append("**Capabilities:**").append(NEWLINE);
        md.append("- Callback Integrity: ").append(characteristics.getMaintainsCallbackIntegrity() ? "✅" : "❌").append(NEWLINE);
        md.append("- Error Recovery: ").append(characteristics.getSupportsErrorRecovery() ? "✅" : "❌").append(NEWLINE);
        md.append("- Incremental Parsing: ").append(characteristics.getSupportsIncrementalParsing() ? "✅" : "❌").append(DOUBLE_NEWLINE);
        
        // Strengths and weaknesses
        if (!characteristics.getStrengths().isEmpty()) {
            md.append("**Strengths:**").append(NEWLINE);
            for (var strength : characteristics.getStrengths()) {
                md.append("- ").append(strength.getDescription()).append(NEWLINE);
            }
            md.append(NEWLINE);
        }
        
        if (!characteristics.getWeaknesses().isEmpty()) {
            md.append("**Weaknesses:**").append(NEWLINE);
            for (var weakness : characteristics.getWeaknesses()) {
                md.append("- ").append(weakness.getDescription()).append(NEWLINE);
            }
            md.append(NEWLINE);
        }
    }
    
    /**
     * Add detailed metrics section.
     */
    private void addDetailedMetrics(StringBuilder md, BenchmarkReport report) {
        md.append("## Detailed Metrics").append(DOUBLE_NEWLINE);
        
        md.append("Complete performance measurements for all strategies.").append(DOUBLE_NEWLINE);
        
        for (StrategyComparison comparison : report.getStrategyComparisons()) {
            md.append("### ").append(comparison.getStrategyName()).append(" - Detailed Metrics").append(DOUBLE_NEWLINE);
            
            BenchmarkResult result = comparison.getResult();
            
            // Execution metrics
            md.append("#### Execution Metrics").append(DOUBLE_NEWLINE);
            md.append("```").append(NEWLINE);
            md.append("Execution Time: ").append(String.format("%.2f ms", result.getExecutionTimeMs())).append(NEWLINE);
            md.append("Callback Count: ").append(result.getCallbackCount()).append(NEWLINE);
            md.append("Callbacks Balanced: ").append(result.isCallbacksBalanced()).append(NEWLINE);
            md.append("```").append(DOUBLE_NEWLINE);
            
            // Memory metrics
            addDetailedMemoryMetrics(md, result.getMemoryMetrics());
            
            // Performance metrics
            addDetailedPerformanceMetrics(md, result.getPerformanceMetrics());
            
            // Scalability metrics
            addDetailedScalabilityMetrics(md, result.getScalabilityMetrics());
            
            // Error recovery metrics
            addDetailedErrorRecoveryMetrics(md, result.getErrorRecoveryMetrics());
        }
    }
    
    /**
     * Add detailed memory metrics.
     */
    private void addDetailedMemoryMetrics(StringBuilder md, MemoryMetrics memory) {
        md.append("#### Memory Metrics").append(DOUBLE_NEWLINE);
        md.append("```").append(NEWLINE);
        md.append("Peak Memory Usage: ").append(String.format("%.1f MB", memory.getPeakUsedMemory() / (1024.0 * 1024))).append(NEWLINE);
        md.append("Allocation Rate: ").append(String.format("%.1f MB/sec", memory.getAllocationRate() / (1024.0 * 1024))).append(NEWLINE);
        md.append("GC Pressure: ").append(String.format("%.3f", memory.getGcPressure())).append(NEWLINE);
        md.append("Retention Rate: ").append(String.format("%.3f", memory.getRetentionRate())).append(NEWLINE);
        md.append("```").append(DOUBLE_NEWLINE);
    }
    
    /**
     * Add detailed performance metrics.
     */
    private void addDetailedPerformanceMetrics(StringBuilder md, PerformanceMetrics performance) {
        md.append("#### Performance Metrics").append(DOUBLE_NEWLINE);
        md.append("```").append(NEWLINE);
        md.append("Total Parse Time: ").append(String.format("%.2f ms", performance.getTotalParseTimeMs())).append(NEWLINE);
        md.append("Throughput: ").append(String.format("%.1f ops/sec", performance.getThroughput())).append(NEWLINE);
        md.append("Average Callback Time: ").append(String.format("%.3f ms", performance.getAverageCallbackTimeMs())).append(NEWLINE);
        md.append("Min Callback Time: ").append(String.format("%.3f ms", performance.getMinCallbackTimeMs())).append(NEWLINE);
        md.append("Max Callback Time: ").append(String.format("%.3f ms", performance.getMaxCallbackTimeMs())).append(NEWLINE);
        md.append("Consistency Score: ").append(String.format("%.3f", performance.getConsistency())).append(NEWLINE);
        md.append("```").append(DOUBLE_NEWLINE);
    }
    
    /**
     * Add detailed scalability metrics.
     */
    private void addDetailedScalabilityMetrics(StringBuilder md, ScalabilityMetrics scalability) {
        md.append("#### Scalability Metrics").append(DOUBLE_NEWLINE);
        md.append("```").append(NEWLINE);
        md.append("Linear Coefficient: ").append(String.format("%.6f", scalability.getLinearCoefficient())).append(NEWLINE);
        md.append("R² Score: ").append(String.format("%.3f", scalability.getRSquared())).append(NEWLINE);
        md.append("Memory Growth Rate: ").append(String.format("%.2fx", scalability.getMemoryGrowthRate())).append(NEWLINE);
        md.append("Data Point Count: ").append(scalability.getDataPointCount()).append(NEWLINE);
        md.append("Linear Scaling: ").append(scalability.isLinearScaling() ? "Yes" : "No").append(NEWLINE);
        md.append("Scaling Category: ").append(scalability.getCategory().name()).append(NEWLINE);
        md.append("```").append(DOUBLE_NEWLINE);
    }
    
    /**
     * Add detailed error recovery metrics.
     */
    private void addDetailedErrorRecoveryMetrics(StringBuilder md, ErrorRecoveryMetrics errorRecovery) {
        md.append("#### Error Recovery Metrics").append(DOUBLE_NEWLINE);
        md.append("```").append(NEWLINE);
        md.append("Error Detection Time: ").append(String.format("%.2f ms", errorRecovery.getErrorDetectionTimeMs())).append(NEWLINE);
        md.append("Recovery Time: ").append(String.format("%.2f ms", errorRecovery.getRecoveryTimeMs())).append(NEWLINE);
        md.append("Callback Pairing Rate: ").append(String.format("%.3f", errorRecovery.getCallbackPairingRate())).append(NEWLINE);
        md.append("Errors Detected: ").append(errorRecovery.getErrorsDetected()).append(NEWLINE);
        md.append("Errors Recovered: ").append(errorRecovery.getErrorsRecovered()).append(NEWLINE);
        md.append("Recovery Success Rate: ").append(String.format("%.3f", errorRecovery.getRecoverySuccessRate())).append(NEWLINE);
        md.append("Performance Impact: ").append(String.format("%.1f%%", errorRecovery.getPerformanceImpactPercent())).append(NEWLINE);
        md.append("Error Handling Score: ").append(String.format("%.2f", errorRecovery.getErrorHandlingScore())).append(NEWLINE);
        md.append("```").append(DOUBLE_NEWLINE);
    }
    
    /**
     * Add recommendations section.
     */
    private void addRecommendations(StringBuilder md, BenchmarkReport report) {
        md.append("## Recommendations").append(DOUBLE_NEWLINE);
        md.append("Based on the benchmark analysis, here are our recommendations for different use cases:").append(DOUBLE_NEWLINE);
        
        for (StrategyRecommendation recommendation : report.getRecommendations()) {
            String icon = getUseCaseIcon(recommendation.getUseCase());
            md.append("### ").append(icon).append(" ").append(recommendation.getCategory()).append(DOUBLE_NEWLINE);
            
            md.append("**Recommended Strategy:** `").append(recommendation.getStrategyName()).append("`").append(DOUBLE_NEWLINE);
            md.append("**Use Case:** ").append(recommendation.getUseCase().getDescription()).append(DOUBLE_NEWLINE);
            md.append("**Reasoning:** ").append(recommendation.getReasoning()).append(DOUBLE_NEWLINE);
        }
    }
    
    /**
     * Add configuration appendix.
     */
    private void addConfigurationAppendix(StringBuilder md, BenchmarkReport report) {
        md.append("## Configuration").append(DOUBLE_NEWLINE);
        md.append("Benchmark configuration used for this analysis:").append(DOUBLE_NEWLINE);
        
        if (report.getConfig() != null) {
            var config = report.getConfig();
            md.append("```yaml").append(NEWLINE);
            md.append("warmup:").append(NEWLINE);
            md.append("  iterations: ").append(config.getWarmupIterations()).append(NEWLINE);
            md.append("  time_ms: ").append(config.getWarmupTimeMs()).append(NEWLINE);
            md.append("measurement:").append(NEWLINE);
            md.append("  iterations: ").append(config.getMeasurementIterations()).append(NEWLINE);
            md.append("  time_ms: ").append(config.getMeasurementTimeMs()).append(NEWLINE);
            md.append("test_corpus:").append(NEWLINE);
            md.append("  samples_per_level: ").append(config.getSamplesPerLevel()).append(NEWLINE);
            md.append("  include_error_cases: ").append(config.getIncludeErrorCases()).append(NEWLINE);
            md.append("  include_incomplete_code: ").append(config.getIncludeIncompleteCode()).append(NEWLINE);
            md.append("profiling:").append(NEWLINE);
            md.append("  enable_memory_profiling: ").append(config.getEnableMemoryProfiling()).append(NEWLINE);
            md.append("  enable_detailed_metrics: ").append(config.getEnableDetailedMetrics()).append(NEWLINE);
            md.append("```").append(DOUBLE_NEWLINE);
        }
    }
    
    /**
     * Get icon for scaling category.
     */
    private String getCategoryIcon(ScalabilityMetrics.ScalabilityCategory category) {
        switch (category) {
            case EXCELLENT: return "🟢";
            case GOOD: return "🟡";
            case MODERATE: return "🟠";
            case LIMITED: return "🔴";
            default: return "⚪";
        }
    }
    
    /**
     * Get icon for use case.
     */
    private String getUseCaseIcon(StrategyRecommendation.UseCase useCase) {
        // These enum values may need to be mapped to actual UseCase enum values
        // For now, using a safe approach with string matching
        String caseName = useCase.name();
        switch (caseName) {
            case "HIGH_PERFORMANCE": return "🚀";
            case "LOW_MEMORY": return "🧠";
            case "LARGE_FILES": return "📁";
            case "ERROR_PRONE_CODE": return "🛡️";
            case "GENERAL_PURPOSE": return "⚡";
            case "DEVELOPMENT_TOOLING": return "🔧";
            default: return "📋";
        }
    }
}