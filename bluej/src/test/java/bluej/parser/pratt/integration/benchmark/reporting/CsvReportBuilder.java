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

import bluej.parser.pratt.integration.benchmark.metrics.BenchmarkResult;

/**
 * CSV report builder for parser integration strategy benchmarks.
 * 
 * Converts BenchmarkReport objects into CSV format suitable for:
 * - Spreadsheet analysis (Excel, Google Sheets)
 * - Data analysis tools (R, Python pandas)
 * - Quick comparison and filtering
 * - Statistical analysis and visualization
 * 
 * Provides multiple CSV views:
 * - Summary view with key metrics per strategy
 * - Detailed metrics with all measurements
 * - Performance ranking table
 * - Scalability data points
 */
class CsvReportBuilder {
    
    private static final String DELIMITER = ",";
    private static final String QUOTE = "\"";
    private static final String NEWLINE = "\n";
    
    /**
     * Build complete CSV report with multiple sheets/sections.
     */
    public String buildCsvReport(BenchmarkReport report) {
        StringBuilder csv = new StringBuilder();
        
        // Report header with metadata
        csv.append("# Parser Integration Strategy Benchmark Report").append(NEWLINE);
        csv.append("# Generated: ").append(report.getReportTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append(NEWLINE);
        csv.append("# Scope: ").append(report.getReportScope()).append(NEWLINE);
        csv.append("# Strategies: ").append(report.getStrategyCount()).append(NEWLINE);
        csv.append(NEWLINE);
        
        // Strategy summary table
        csv.append("# Strategy Summary").append(NEWLINE);
        csv.append(buildStrategySummaryTable(report));
        csv.append(NEWLINE).append(NEWLINE);
        
        // Performance ranking
        csv.append("# Performance Ranking").append(NEWLINE);
        csv.append(buildPerformanceRankingTable(report.getRanking()));
        csv.append(NEWLINE).append(NEWLINE);
        
        // Detailed metrics
        csv.append("# Detailed Metrics").append(NEWLINE);
        csv.append(buildDetailedMetricsTable(report));
        csv.append(NEWLINE).append(NEWLINE);
        
        // Memory analysis
        csv.append("# Memory Analysis").append(NEWLINE);
        csv.append(buildMemoryAnalysisTable(report));
        csv.append(NEWLINE).append(NEWLINE);
        
        // Scalability analysis
        csv.append("# Scalability Analysis").append(NEWLINE);
        csv.append(buildScalabilityAnalysisTable(report));
        csv.append(NEWLINE).append(NEWLINE);
        
        // Error recovery analysis
        csv.append("# Error Recovery Analysis").append(NEWLINE);
        csv.append(buildErrorRecoveryAnalysisTable(report));
        csv.append(NEWLINE).append(NEWLINE);
        
        // Recommendations
        csv.append("# Recommendations").append(NEWLINE);
        csv.append(buildRecommendationsTable(report));
        
        return csv.toString();
    }
    
    /**
     * Build strategy summary table with key metrics.
     */
    private String buildStrategySummaryTable(BenchmarkReport report) {
        StringBuilder csv = new StringBuilder();
        
        // Headers
        csv.append(csvRow("Strategy", "Overall Score", "Category", "Peak Memory (MB)", 
                         "Throughput (ops/sec)", "Parse Time (ms)", "Callback Count",
                         "Callbacks Balanced", "R² Score", "Error Recovery Score"));
        
        // Data rows
        for (StrategyComparison comparison : report.getStrategyComparisons()) {
            BenchmarkResult result = comparison.getResult();
            csv.append(csvRow(
                comparison.getStrategyName(),
                String.format("%.2f", comparison.getOverallScore()),
                comparison.getPerformanceCategory().name(),
                String.format("%.1f", result.getMemoryMetrics().getPeakUsedMemory() / (1024.0 * 1024)),
                String.format("%.1f", result.getPerformanceMetrics().getThroughput()),
                String.format("%.2f", result.getPerformanceMetrics().getTotalParseTimeMs()),
                String.valueOf(result.getCallbackCount()),
                String.valueOf(result.isCallbacksBalanced()),
                String.format("%.3f", result.getScalabilityMetrics().getRSquared()),
                String.format("%.2f", result.getErrorRecoveryMetrics().getErrorHandlingScore())
            ));
        }
        
        return csv.toString();
    }
    
    /**
     * Build performance ranking table.
     */
    private String buildPerformanceRankingTable(PerformanceRanking ranking) {
        StringBuilder csv = new StringBuilder();
        
        // Overall ranking
        csv.append("# Overall Performance Ranking").append(NEWLINE);
        csv.append(csvRow("Rank", "Strategy", "Overall Score"));
        
        for (int i = 0; i < ranking.getRankedByOverall().size(); i++) {
            StrategyComparison comparison = ranking.getRankedByOverall().get(i);
            csv.append(csvRow(
                String.valueOf(i + 1),
                comparison.getStrategyName(),
                String.format("%.2f", comparison.getOverallScore())
            ));
        }
        
        csv.append(NEWLINE);
        
        // Memory ranking
        csv.append("# Memory Usage Ranking (Lower is Better)").append(NEWLINE);
        csv.append(csvRow("Rank", "Strategy", "Peak Memory (MB)"));
        
        for (int i = 0; i < ranking.getRankedByMemory().size(); i++) {
            StrategyComparison comparison = ranking.getRankedByMemory().get(i);
            csv.append(csvRow(
                String.valueOf(i + 1),
                comparison.getStrategyName(),
                String.format("%.1f", comparison.getResult().getMemoryMetrics().getPeakUsedMemory() / (1024.0 * 1024))
            ));
        }
        
        csv.append(NEWLINE);
        
        // Speed ranking
        csv.append("# Speed Ranking (Higher is Better)").append(NEWLINE);
        csv.append(csvRow("Rank", "Strategy", "Throughput (ops/sec)"));
        
        for (int i = 0; i < ranking.getRankedBySpeed().size(); i++) {
            StrategyComparison comparison = ranking.getRankedBySpeed().get(i);
            csv.append(csvRow(
                String.valueOf(i + 1),
                comparison.getStrategyName(),
                String.format("%.1f", comparison.getResult().getPerformanceMetrics().getThroughput())
            ));
        }
        
        return csv.toString();
    }
    
    /**
     * Build detailed metrics table with all measurements.
     */
    private String buildDetailedMetricsTable(BenchmarkReport report) {
        StringBuilder csv = new StringBuilder();
        
        csv.append(csvRow("Strategy", "Execution Time (ms)", "Peak Memory (bytes)", "Allocation Rate (bytes/sec)",
                         "GC Pressure", "Retention Rate", "Total Parse Time (ms)", "Average Callback Time (ms)",
                         "Min Callback Time (ms)", "Max Callback Time (ms)", "Consistency Score"));
        
        for (StrategyComparison comparison : report.getStrategyComparisons()) {
            BenchmarkResult result = comparison.getResult();
            var memory = result.getMemoryMetrics();
            var performance = result.getPerformanceMetrics();
            
            csv.append(csvRow(
                comparison.getStrategyName(),
                String.format("%.2f", result.getExecutionTimeMs()),
                String.valueOf(memory.getPeakUsedMemory()),
                String.format("%.0f", memory.getAllocationRate()),
                String.format("%.3f", memory.getGcPressure()),
                String.format("%.3f", memory.getRetentionRate()),
                String.format("%.2f", performance.getTotalParseTimeMs()),
                String.format("%.3f", performance.getAverageCallbackTimeMs()),
                String.format("%.3f", performance.getMinCallbackTimeMs()),
                String.format("%.3f", performance.getMaxCallbackTimeMs()),
                String.format("%.3f", performance.getConsistency())
            ));
        }
        
        return csv.toString();
    }
    
    /**
     * Build memory analysis table.
     */
    private String buildMemoryAnalysisTable(BenchmarkReport report) {
        StringBuilder csv = new StringBuilder();
        
        csv.append(csvRow("Strategy", "Peak Memory (MB)", "Allocation Rate (MB/sec)", 
                         "GC Pressure", "Retention Rate", "Memory Efficiency"));
        
        for (StrategyComparison comparison : report.getStrategyComparisons()) {
            var memory = comparison.getResult().getMemoryMetrics();
            double peakMB = memory.getPeakUsedMemory() / (1024.0 * 1024);
            double allocationMBps = memory.getAllocationRate() / (1024.0 * 1024);
            double efficiency = 1.0 / (peakMB + 1); // Simple efficiency metric
            
            csv.append(csvRow(
                comparison.getStrategyName(),
                String.format("%.1f", peakMB),
                String.format("%.1f", allocationMBps),
                String.format("%.3f", memory.getGcPressure()),
                String.format("%.3f", memory.getRetentionRate()),
                String.format("%.3f", efficiency)
            ));
        }
        
        return csv.toString();
    }
    
    /**
     * Build scalability analysis table.
     */
    private String buildScalabilityAnalysisTable(BenchmarkReport report) {
        StringBuilder csv = new StringBuilder();
        
        csv.append(csvRow("Strategy", "Linear Coefficient", "R² Score", "Memory Growth Rate", 
                         "Data Points", "Linear Scaling", "Scaling Category"));
        
        for (StrategyComparison comparison : report.getStrategyComparisons()) {
            var scalability = comparison.getResult().getScalabilityMetrics();
            
            csv.append(csvRow(
                comparison.getStrategyName(),
                String.format("%.6f", scalability.getLinearCoefficient()),
                String.format("%.3f", scalability.getRSquared()),
                String.format("%.3f", scalability.getMemoryGrowthRate()),
                String.valueOf(scalability.getDataPointCount()),
                String.valueOf(scalability.isLinearScaling()),
                scalability.getCategory().name()
            ));
        }
        
        return csv.toString();
    }
    
    /**
     * Build error recovery analysis table.
     */
    private String buildErrorRecoveryAnalysisTable(BenchmarkReport report) {
        StringBuilder csv = new StringBuilder();
        
        csv.append(csvRow("Strategy", "Error Detection Time (ms)", "Recovery Time (ms)", 
                         "Callback Pairing Rate", "Errors Detected", "Errors Recovered",
                         "Recovery Success Rate", "Performance Impact %", "Error Handling Score",
                         "Maintains Callback Integrity", "Robust Error Recovery", "Low Performance Impact"));
        
        for (StrategyComparison comparison : report.getStrategyComparisons()) {
            var errorRecovery = comparison.getResult().getErrorRecoveryMetrics();
            
            csv.append(csvRow(
                comparison.getStrategyName(),
                String.format("%.2f", errorRecovery.getErrorDetectionTimeMs()),
                String.format("%.2f", errorRecovery.getRecoveryTimeMs()),
                String.format("%.3f", errorRecovery.getCallbackPairingRate()),
                String.valueOf(errorRecovery.getErrorsDetected()),
                String.valueOf(errorRecovery.getErrorsRecovered()),
                String.format("%.3f", errorRecovery.getRecoverySuccessRate()),
                String.format("%.1f", errorRecovery.getPerformanceImpactPercent()),
                String.format("%.2f", errorRecovery.getErrorHandlingScore()),
                String.valueOf(errorRecovery.getMaintainsCallbackIntegrity()),
                String.valueOf(errorRecovery.isRobustErrorRecovery()),
                String.valueOf(errorRecovery.hasLowPerformanceImpact())
            ));
        }
        
        return csv.toString();
    }
    
    /**
     * Build recommendations table.
     */
    private String buildRecommendationsTable(BenchmarkReport report) {
        StringBuilder csv = new StringBuilder();
        
        csv.append(csvRow("Category", "Strategy", "Use Case", "Reasoning"));
        
        for (StrategyRecommendation recommendation : report.getRecommendations()) {
            csv.append(csvRow(
                recommendation.getCategory().getDisplayName(),
                recommendation.getStrategyName(),
                recommendation.getUseCase().name(),
                recommendation.getReasoning()
            ));
        }
        
        return csv.toString();
    }
    
    /**
     * Create a CSV row from values.
     */
    private String csvRow(String... values) {
        return String.join(DELIMITER, 
                          java.util.Arrays.stream(values)
                                         .map(this::escapeCsv)
                                         .toArray(String[]::new)) + NEWLINE;
    }
    
    /**
     * Escape CSV value by quoting and escaping quotes.
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        
        // If value contains comma, quote, or newline, wrap in quotes
        if (value.contains(DELIMITER) || value.contains(QUOTE) || value.contains("\n") || value.contains("\r")) {
            return QUOTE + value.replace(QUOTE, QUOTE + QUOTE) + QUOTE;
        }
        
        return value;
    }
}