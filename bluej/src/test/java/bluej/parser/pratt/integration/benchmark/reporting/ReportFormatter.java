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
import java.util.stream.IntStream;

/**
 * Console report formatter for parser integration strategy benchmarks.
 * 
 * Generates formatted ASCII text output with:
 * - Strategy comparison tables
 * - Performance rankings and scores
 * - Visual ASCII charts and graphs
 * - Summary statistics and recommendations
 * - Detailed metric breakdowns
 * 
 * Designed for readable console output during development and testing.
 */
class ReportFormatter {
    
    private static final String HEADER_SEPARATOR = "═";
    private static final String ROW_SEPARATOR = "─";
    private static final String COLUMN_SEPARATOR = "│";
    private static final int TABLE_WIDTH = 120;
    private static final int NAME_COLUMN_WIDTH = 25;
    private static final int METRIC_COLUMN_WIDTH = 12;
    
    /**
     * Print complete benchmark report to console.
     */
    public void printReport(BenchmarkReport report) {
        System.out.println(formatReportToString(report));
    }
    
    /**
     * Format complete benchmark report as string.
     */
    public String formatReportToString(BenchmarkReport report) {
        StringBuilder output = new StringBuilder();
        
        // Report header
        output.append(formatHeader(report));
        output.append("\n");
        
        if (!report.hasResults()) {
            output.append("No benchmark results available.\n");
            return output.toString();
        }
        
        // Strategy comparison table
        output.append(formatStrategyComparisonTable(report));
        output.append("\n");
        
        // Performance rankings
        output.append(formatPerformanceRankings(report));
        output.append("\n");
        
        // Visual performance chart
        output.append(formatPerformanceChart(report));
        output.append("\n");
        
        // Detailed metrics breakdown
        output.append(formatDetailedMetrics(report));
        output.append("\n");
        
        // Summary and recommendations
        output.append(formatSummaryAndRecommendations(report));
        
        return output.toString();
    }
    
    /**
     * Format report header with metadata.
     */
    private String formatHeader(BenchmarkReport report) {
        StringBuilder header = new StringBuilder();
        
        header.append(createSeparatorLine(HEADER_SEPARATOR, TABLE_WIDTH)).append("\n");
        header.append(centerText("Parser Integration Strategy Benchmark Report", TABLE_WIDTH)).append("\n");
        header.append(createSeparatorLine(HEADER_SEPARATOR, TABLE_WIDTH)).append("\n");
        header.append("\n");
        
        // Report metadata
        header.append(String.format("Generated: %s\n", 
                report.getReportTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        header.append(String.format("Scope: %s\n", report.getReportScope()));
        
        if (report.getConfig() != null) {
            header.append(String.format("Configuration: %d warmup iterations, %d measurement iterations\n",
                    report.getConfig().getWarmupIterations(),
                    report.getConfig().getMeasurementIterations()));
        }
        
        return header.toString();
    }
    
    /**
     * Format strategy comparison table.
     */
    private String formatStrategyComparisonTable(BenchmarkReport report) {
        StringBuilder table = new StringBuilder();
        
        table.append("STRATEGY COMPARISON\n");
        table.append(createSeparatorLine(HEADER_SEPARATOR, TABLE_WIDTH)).append("\n");
        
        // Table header
        table.append(String.format("%-" + NAME_COLUMN_WIDTH + "s", "Strategy"));
        table.append(COLUMN_SEPARATOR);
        table.append(String.format("%" + METRIC_COLUMN_WIDTH + "s", "Overall"));
        table.append(COLUMN_SEPARATOR);
        table.append(String.format("%" + METRIC_COLUMN_WIDTH + "s", "Memory (MB)"));
        table.append(COLUMN_SEPARATOR);
        table.append(String.format("%" + METRIC_COLUMN_WIDTH + "s", "Throughput"));
        table.append(COLUMN_SEPARATOR);
        table.append(String.format("%" + METRIC_COLUMN_WIDTH + "s", "Scalability"));
        table.append(COLUMN_SEPARATOR);
        table.append(String.format("%" + METRIC_COLUMN_WIDTH + "s", "Error Rec."));
        table.append(COLUMN_SEPARATOR);
        table.append(String.format("%" + METRIC_COLUMN_WIDTH + "s", "Category"));
        table.append("\n");
        
        table.append(createSeparatorLine(ROW_SEPARATOR, TABLE_WIDTH)).append("\n");
        
        // Table rows
        for (StrategyComparison comparison : report.getStrategyComparisons()) {
            table.append(formatStrategyRow(comparison));
            table.append("\n");
        }
        
        return table.toString();
    }
    
    /**
     * Format individual strategy row in comparison table.
     */
    private String formatStrategyRow(StrategyComparison comparison) {
        StringBuilder row = new StringBuilder();
        
        // Strategy name (truncated if necessary)
        String strategyName = comparison.getStrategyName();
        if (strategyName.length() > NAME_COLUMN_WIDTH - 1) {
            strategyName = strategyName.substring(0, NAME_COLUMN_WIDTH - 4) + "...";
        }
        row.append(String.format("%-" + NAME_COLUMN_WIDTH + "s", strategyName));
        row.append(COLUMN_SEPARATOR);
        
        // Overall score
        row.append(String.format("%" + METRIC_COLUMN_WIDTH + ".1f", comparison.getOverallScore()));
        row.append(COLUMN_SEPARATOR);
        
        // Memory usage (MB)
        double memoryMB = comparison.getResult().getMemoryMetrics().getPeakUsedMemory() / (1024.0 * 1024.0);
        row.append(String.format("%" + METRIC_COLUMN_WIDTH + ".1f", memoryMB));
        row.append(COLUMN_SEPARATOR);
        
        // Throughput
        row.append(String.format("%" + METRIC_COLUMN_WIDTH + ".0f", 
                comparison.getResult().getPerformanceMetrics().getThroughput()));
        row.append(COLUMN_SEPARATOR);
        
        // Scalability (R²)
        row.append(String.format("%" + METRIC_COLUMN_WIDTH + ".3f", 
                comparison.getResult().getScalabilityMetrics().getRSquared()));
        row.append(COLUMN_SEPARATOR);
        
        // Error Recovery Score
        row.append(String.format("%" + METRIC_COLUMN_WIDTH + ".1f", 
                comparison.getResult().getErrorRecoveryMetrics().getErrorHandlingScore()));
        row.append(COLUMN_SEPARATOR);
        
        // Performance Category
        row.append(String.format("%" + METRIC_COLUMN_WIDTH + "s", 
                comparison.getPerformanceCategory().getDescription()));
        
        return row.toString();
    }
    
    /**
     * Format performance rankings section.
     */
    private String formatPerformanceRankings(BenchmarkReport report) {
        StringBuilder rankings = new StringBuilder();
        
        rankings.append("PERFORMANCE RANKINGS\n");
        rankings.append(createSeparatorLine(HEADER_SEPARATOR, TABLE_WIDTH)).append("\n");
        
        PerformanceRanking ranking = report.getRanking();
        
        // Overall ranking
        rankings.append("Overall Performance:\n");
        for (int i = 0; i < ranking.getRankedByOverall().size(); i++) {
            StrategyComparison strategy = ranking.getRankedByOverall().get(i);
            rankings.append(String.format("  %d. %-30s (Score: %.1f)\n", 
                    i + 1, strategy.getStrategyName(), strategy.getOverallScore()));
        }
        rankings.append("\n");
        
        // Memory efficiency ranking
        rankings.append("Memory Efficiency:\n");
        for (int i = 0; i < ranking.getRankedByMemory().size(); i++) {
            StrategyComparison strategy = ranking.getRankedByMemory().get(i);
            double memoryMB = strategy.getResult().getMemoryMetrics().getPeakUsedMemory() / (1024.0 * 1024.0);
            rankings.append(String.format("  %d. %-30s (%.1f MB)\n", 
                    i + 1, strategy.getStrategyName(), memoryMB));
        }
        rankings.append("\n");
        
        // Speed ranking
        rankings.append("Processing Speed:\n");
        for (int i = 0; i < ranking.getRankedBySpeed().size(); i++) {
            StrategyComparison strategy = ranking.getRankedBySpeed().get(i);
            rankings.append(String.format("  %d. %-30s (%.0f callbacks/sec)\n", 
                    i + 1, strategy.getStrategyName(), 
                    strategy.getResult().getPerformanceMetrics().getThroughput()));
        }
        
        return rankings.toString();
    }
    
    /**
     * Format visual performance chart.
     */
    private String formatPerformanceChart(BenchmarkReport report) {
        StringBuilder chart = new StringBuilder();
        
        chart.append("PERFORMANCE VISUALIZATION\n");
        chart.append(createSeparatorLine(HEADER_SEPARATOR, TABLE_WIDTH)).append("\n");
        
        // Simple ASCII bar chart of overall scores
        double maxScore = report.getStrategyComparisons().stream()
                .mapToDouble(StrategyComparison::getOverallScore)
                .max()
                .orElse(100);
        
        int chartWidth = 60;
        
        for (StrategyComparison comparison : report.getStrategyComparisons()) {
            String name = truncateString(comparison.getStrategyName(), 20);
            double score = comparison.getOverallScore();
            int barLength = (int) ((score / maxScore) * chartWidth);
            
            chart.append(String.format("%-20s ", name));
            chart.append(COLUMN_SEPARATOR);
            chart.append(" ");
            
            // Draw bar
            chart.append("█".repeat(Math.max(0, barLength)));
            chart.append(" ".repeat(Math.max(0, chartWidth - barLength)));
            chart.append(String.format(" %.1f", score));
            chart.append("\n");
        }
        
        // Chart scale
        chart.append(" ".repeat(22));
        chart.append(COLUMN_SEPARATOR);
        chart.append(" 0");
        chart.append(" ".repeat(chartWidth - 10));
        chart.append(String.format("%.0f", maxScore));
        chart.append("\n");
        
        return chart.toString();
    }
    
    /**
     * Format detailed metrics breakdown.
     */
    private String formatDetailedMetrics(BenchmarkReport report) {
        StringBuilder details = new StringBuilder();
        
        details.append("DETAILED METRICS\n");
        details.append(createSeparatorLine(HEADER_SEPARATOR, TABLE_WIDTH)).append("\n");
        
        for (StrategyComparison comparison : report.getStrategyComparisons()) {
            details.append(formatStrategyDetails(comparison));
            details.append("\n");
        }
        
        return details.toString();
    }
    
    /**
     * Format detailed metrics for a single strategy.
     */
    private String formatStrategyDetails(StrategyComparison comparison) {
        StringBuilder details = new StringBuilder();
        
        details.append(String.format("%s:\n", comparison.getStrategyName()));
        details.append(createSeparatorLine(ROW_SEPARATOR, 80)).append("\n");
        
        // Memory metrics
        details.append("  Memory Metrics:\n");
        details.append(String.format("    Peak Usage: %.2f MB\n", 
                comparison.getResult().getMemoryMetrics().getPeakUsedMemory() / (1024.0 * 1024.0)));
        details.append(String.format("    Allocation Rate: %.2f MB/sec\n", 
                comparison.getResult().getMemoryMetrics().getAllocationRate() / (1024.0 * 1024.0)));
        details.append(String.format("    GC Pressure: %.3f\n", 
                comparison.getResult().getMemoryMetrics().getGcPressure()));
        
        // Performance metrics
        details.append("  Performance Metrics:\n");
        details.append(String.format("    Total Time: %.2f ms\n", 
                comparison.getResult().getPerformanceMetrics().getTotalParseTimeMs()));
        details.append(String.format("    Throughput: %.0f callbacks/sec\n", 
                comparison.getResult().getPerformanceMetrics().getThroughput()));
        details.append(String.format("    Consistency: %.3f\n", 
                comparison.getResult().getPerformanceMetrics().getConsistency()));
        
        // Scalability metrics
        details.append("  Scalability Metrics:\n");
        details.append(String.format("    Linear Coefficient: %.6f\n", 
                comparison.getResult().getScalabilityMetrics().getLinearCoefficient()));
        details.append(String.format("    R-Squared: %.3f\n", 
                comparison.getResult().getScalabilityMetrics().getRSquared()));
        details.append(String.format("    Scaling Category: %s\n", 
                comparison.getResult().getScalabilityMetrics().getCategory()));
        
        // Error recovery metrics
        details.append("  Error Recovery Metrics:\n");
        details.append(String.format("    Error Handling Score: %.1f\n", 
                comparison.getResult().getErrorRecoveryMetrics().getErrorHandlingScore()));
        details.append(String.format("    Callback Pairing Rate: %.3f\n", 
                comparison.getResult().getErrorRecoveryMetrics().getCallbackPairingRate()));
        details.append(String.format("    Maintains Integrity: %s\n", 
                comparison.getResult().getErrorRecoveryMetrics().getMaintainsCallbackIntegrity()));
        
        return details.toString();
    }
    
    /**
     * Format summary and recommendations section.
     */
    private String formatSummaryAndRecommendations(BenchmarkReport report) {
        StringBuilder summary = new StringBuilder();
        
        summary.append("SUMMARY AND RECOMMENDATIONS\n");
        summary.append(createSeparatorLine(HEADER_SEPARATOR, TABLE_WIDTH)).append("\n");
        
        BenchmarkSummary benchmarkSummary = report.getSummary();
        
        // Summary statistics
        summary.append("Summary Statistics:\n");
        if (benchmarkSummary.getBestStrategy() != null) {
            summary.append(String.format("  Best Overall: %s (Score: %.1f)\n", 
                    benchmarkSummary.getBestStrategy().getStrategyName(),
                    benchmarkSummary.getBestStrategy().getOverallScore()));
        }
        if (benchmarkSummary.getWorstStrategy() != null) {
            summary.append(String.format("  Worst Overall: %s (Score: %.1f)\n", 
                    benchmarkSummary.getWorstStrategy().getStrategyName(),
                    benchmarkSummary.getWorstStrategy().getOverallScore()));
        }
        summary.append(String.format("  Average Score: %.1f\n", benchmarkSummary.getAverageScore()));
        summary.append(String.format("  Performance Spread: %.1f points\n", benchmarkSummary.getPerformanceSpread()));
        summary.append(String.format("  Max Speed Difference: %.1f%%\n", benchmarkSummary.getMaxSpeedDifferencePercent()));
        summary.append(String.format("  Max Memory Difference: %.1f%%\n", benchmarkSummary.getMaxMemoryDifferencePercent()));
        summary.append("\n");
        
        // Recommendations
        summary.append("Recommendations by Use Case:\n");
        for (StrategyRecommendation recommendation : report.getRecommendations()) {
            summary.append(String.format("  %s: %s\n", 
                    recommendation.getCategory(), recommendation.getStrategyName()));
            summary.append(String.format("    %s\n", recommendation.getReasoning()));
            summary.append("\n");
        }
        
        return summary.toString();
    }
    
    // Helper methods
    
    private String createSeparatorLine(String character, int width) {
        return character.repeat(width);
    }
    
    private String centerText(String text, int width) {
        int padding = Math.max(0, width - text.length());
        int leftPadding = padding / 2;
        int rightPadding = padding - leftPadding;
        return " ".repeat(leftPadding) + text + " ".repeat(rightPadding);
    }
    
    private String truncateString(String str, int maxLength) {
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }
}