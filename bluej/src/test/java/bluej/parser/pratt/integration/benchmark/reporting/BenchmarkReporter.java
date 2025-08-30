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
import bluej.parser.pratt.integration.benchmark.metrics.MemoryMetrics;
import bluej.parser.pratt.integration.benchmark.metrics.PerformanceMetrics;
import bluej.parser.pratt.integration.benchmark.metrics.ScalabilityMetrics;
import bluej.parser.pratt.integration.benchmark.metrics.ErrorRecoveryMetrics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprehensive benchmark reporting system for parser integration strategies.
 * 
 * Generates detailed reports in multiple formats:
 * - Console output with formatted tables and visual comparisons
 * - JSON format for programmatic analysis and tool integration
 * - CSV format for spreadsheet analysis
 * - Markdown format for documentation
 * 
 * Features:
 * - Strategy comparison matrices
 * - Performance ranking and scoring
 * - Detailed metric breakdowns
 * - Visual ASCII charts and graphs
 * - Recommendations based on use case analysis
 */
public class BenchmarkReporter {
    
    private final BenchmarkConfiguration config;
    private final ReportFormatter consoleFormatter;
    private final JsonReportBuilder jsonBuilder;
    private final CsvReportBuilder csvBuilder;
    private final MarkdownReportBuilder markdownBuilder;
    
    public BenchmarkReporter(BenchmarkConfiguration config) {
        this.config = config;
        this.consoleFormatter = new ReportFormatter();
        this.jsonBuilder = new JsonReportBuilder();
        this.csvBuilder = new CsvReportBuilder();
        this.markdownBuilder = new MarkdownReportBuilder();
    }
    
    /**
     * Generate a comprehensive benchmark report from multiple strategy results.
     * 
     * @param results Map of strategy name to benchmark result
     * @param characteristics Map of strategy name to characteristics
     * @return Complete benchmark report
     */
    public BenchmarkReport generateReport(Map<String, BenchmarkResult> results,
                                        Map<String, StrategyCharacteristics> characteristics) {
        
        LocalDateTime reportTime = LocalDateTime.now();
        List<StrategyComparison> comparisons = createStrategyComparisons(results, characteristics);
        PerformanceRanking ranking = createPerformanceRanking(comparisons);
        BenchmarkSummary summary = createBenchmarkSummary(comparisons, ranking);
        
        return new BenchmarkReport(
            reportTime,
            config,
            comparisons,
            ranking,
            summary,
            generateRecommendations(comparisons, ranking)
        );
    }
    
    /**
     * Generate console output report and return as string.
     */
    public String generateConsoleReport(BenchmarkReport report) {
        return consoleFormatter.formatReportToString(report);
    }
    
    /**
     * Generate JSON report and return as string.
     */
    public String generateJsonReport(BenchmarkReport report) {
        return jsonBuilder.buildJsonReport(report);
    }
    
    /**
     * Generate JSON report file.
     */
    public void generateJsonReport(BenchmarkReport report, String filePath) throws IOException {
        String jsonReport = jsonBuilder.buildJsonReport(report);
        writeToFile(filePath, jsonReport);
    }
    
    /**
     * Generate CSV report and return as string.
     */
    public String generateCsvReport(BenchmarkReport report) {
        return csvBuilder.buildCsvReport(report);
    }
    
    /**
     * Generate CSV report file.
     */
    public void generateCsvReport(BenchmarkReport report, String filePath) throws IOException {
        String csvReport = csvBuilder.buildCsvReport(report);
        writeToFile(filePath, csvReport);
    }
    
    /**
     * Generate Markdown report and return as string.
     */
    public String generateMarkdownReport(BenchmarkReport report) {
        return markdownBuilder.buildMarkdownReport(report);
    }
    
    /**
     * Generate Markdown report file.
     */
    public void generateMarkdownReport(BenchmarkReport report, String filePath) throws IOException {
        String markdownReport = markdownBuilder.buildMarkdownReport(report);
        writeToFile(filePath, markdownReport);
    }
    
    /**
     * Generate all report formats to specified directory.
     */
    public void generateAllReports(BenchmarkReport report) throws IOException {
        String outputDirectory = config.getOutputDirectory();
        generateAllReports(report, outputDirectory);
    }
    
    /**
     * Generate all report formats to specified directory.
     */
    public void generateAllReports(BenchmarkReport report, String outputDirectory) throws IOException {
        File dir = new File(outputDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // Use simple filenames for testing compatibility
        String baseFilename = "benchmark-report";
        
        generateJsonReport(report, outputDirectory + "/" + baseFilename + ".json");
        generateCsvReport(report, outputDirectory + "/" + baseFilename + ".csv");
        generateMarkdownReport(report, outputDirectory + "/" + baseFilename + ".md");
        
        // Also generate console report to file
        String consoleOutput = consoleFormatter.formatReportToString(report);
        writeToFile(outputDirectory + "/" + baseFilename + ".txt", consoleOutput);
    }
    
    /**
     * Create strategy comparisons from results and characteristics.
     */
    private List<StrategyComparison> createStrategyComparisons(Map<String, BenchmarkResult> results,
                                                             Map<String, StrategyCharacteristics> characteristics) {
        
        return results.entrySet().stream()
                .map(entry -> {
                    String strategyName = entry.getKey();
                    BenchmarkResult result = entry.getValue();
                    StrategyCharacteristics chars = characteristics.get(strategyName);
                    
                    return new StrategyComparison(strategyName, result, chars);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Create performance ranking from strategy comparisons.
     */
    private PerformanceRanking createPerformanceRanking(List<StrategyComparison> comparisons) {
        // Rank by overall performance score
        List<StrategyComparison> rankedByOverall = new ArrayList<>(comparisons);
        rankedByOverall.sort((a, b) -> Double.compare(b.getOverallScore(), a.getOverallScore()));
        
        // Rank by specific metrics
        List<StrategyComparison> rankedByMemory = new ArrayList<>(comparisons);
        rankedByMemory.sort((a, b) -> Double.compare(
                a.getResult().getMemoryMetrics().getPeakUsedMemory(),
                b.getResult().getMemoryMetrics().getPeakUsedMemory()
        ));
        
        List<StrategyComparison> rankedBySpeed = new ArrayList<>(comparisons);
        rankedBySpeed.sort((a, b) -> Double.compare(
                b.getResult().getPerformanceMetrics().getThroughput(),
                a.getResult().getPerformanceMetrics().getThroughput()
        ));
        
        List<StrategyComparison> rankedByScalability = new ArrayList<>(comparisons);
        rankedByScalability.sort((a, b) -> Double.compare(
                b.getResult().getScalabilityMetrics().getRSquared(),
                a.getResult().getScalabilityMetrics().getRSquared()
        ));
        
        return new PerformanceRanking(rankedByOverall, rankedByMemory, rankedBySpeed, rankedByScalability);
    }
    
    /**
     * Create benchmark summary statistics.
     */
    private BenchmarkSummary createBenchmarkSummary(List<StrategyComparison> comparisons, 
                                                  PerformanceRanking ranking) {
        
        if (comparisons.isEmpty()) {
            return new BenchmarkSummary(null, null, 0, 0, 0);
        }
        
        StrategyComparison best = ranking.getRankedByOverall().get(0);
        StrategyComparison worst = ranking.getRankedByOverall().get(ranking.getRankedByOverall().size() - 1);
        
        double avgScore = comparisons.stream()
                .mapToDouble(StrategyComparison::getOverallScore)
                .average()
                .orElse(0);
        
        double maxSpeedDifference = calculateMaxSpeedDifference(comparisons);
        double maxMemoryDifference = calculateMaxMemoryDifference(comparisons);
        
        return new BenchmarkSummary(best, worst, avgScore, maxSpeedDifference, maxMemoryDifference);
    }
    
    /**
     * Generate strategy recommendations based on analysis.
     */
    private List<StrategyRecommendation> generateRecommendations(List<StrategyComparison> comparisons,
                                                               PerformanceRanking ranking) {
        
        List<StrategyRecommendation> recommendations = new ArrayList<>();
        
        // Best overall strategy
        if (!ranking.getRankedByOverall().isEmpty()) {
            StrategyComparison best = ranking.getRankedByOverall().get(0);
            recommendations.add(new StrategyRecommendation(
                StrategyRecommendation.RecommendationCategory.PERFORMANCE,
                best.getStrategyName(),
                String.format("Highest overall score of %.2f with excellent balance across all metrics.",
                        best.getOverallScore()),
                StrategyRecommendation.UseCase.GENERAL_PURPOSE
            ));
        }
        
        // Memory-efficient strategy
        if (!ranking.getRankedByMemory().isEmpty()) {
            StrategyComparison memoryBest = ranking.getRankedByMemory().get(0);
            recommendations.add(new StrategyRecommendation(
                StrategyRecommendation.RecommendationCategory.MEMORY_EFFICIENCY,
                memoryBest.getStrategyName(),
                String.format("Lowest memory usage of %.2f MB, ideal for memory-constrained environments.",
                        memoryBest.getResult().getMemoryMetrics().getPeakUsedMemory() / (1024.0 * 1024.0)),
                StrategyRecommendation.UseCase.MEMORY_CONSTRAINED
            ));
        }
        
        // Fastest strategy
        if (!ranking.getRankedBySpeed().isEmpty()) {
            StrategyComparison speedBest = ranking.getRankedBySpeed().get(0);
            recommendations.add(new StrategyRecommendation(
                StrategyRecommendation.RecommendationCategory.PERFORMANCE,
                speedBest.getStrategyName(),
                String.format("Best throughput of %.1f callbacks/second for performance-critical applications.",
                        speedBest.getResult().getPerformanceMetrics().getThroughput()),
                StrategyRecommendation.UseCase.PERFORMANCE_CRITICAL
            ));
        }
        
        // Best scalability
        if (!ranking.getRankedByScalability().isEmpty()) {
            StrategyComparison scalabilityBest = ranking.getRankedByScalability().get(0);
            recommendations.add(new StrategyRecommendation(
                StrategyRecommendation.RecommendationCategory.SCALABILITY,
                scalabilityBest.getStrategyName(),
                String.format("Most linear scaling with R² = %.3f, excellent for large codebases.",
                        scalabilityBest.getResult().getScalabilityMetrics().getRSquared()),
                StrategyRecommendation.UseCase.LARGE_SCALE
            ));
        }
        
        // Error recovery recommendation
        StrategyComparison errorRecoveryBest = comparisons.stream()
                .max((a, b) -> Double.compare(
                        a.getResult().getErrorRecoveryMetrics().getErrorHandlingScore(),
                        b.getResult().getErrorRecoveryMetrics().getErrorHandlingScore()
                ))
                .orElse(null);
        
        if (errorRecoveryBest != null) {
            recommendations.add(new StrategyRecommendation(
                StrategyRecommendation.RecommendationCategory.ERROR_RESILIENCE,
                errorRecoveryBest.getStrategyName(),
                String.format("Superior error handling with score %.1f, recommended for parsing error-prone code.",
                        errorRecoveryBest.getResult().getErrorRecoveryMetrics().getErrorHandlingScore()),
                StrategyRecommendation.UseCase.ERROR_PRONE_CODE
            ));
        }
        
        return recommendations;
    }
    
    /**
     * Calculate maximum speed difference between strategies.
     */
    private double calculateMaxSpeedDifference(List<StrategyComparison> comparisons) {
        if (comparisons.size() < 2) return 0;
        
        double maxThroughput = comparisons.stream()
                .mapToDouble(c -> c.getResult().getPerformanceMetrics().getThroughput())
                .max()
                .orElse(0);
        
        double minThroughput = comparisons.stream()
                .mapToDouble(c -> c.getResult().getPerformanceMetrics().getThroughput())
                .min()
                .orElse(0);
        
        return minThroughput > 0 ? ((maxThroughput - minThroughput) / minThroughput) * 100 : 0;
    }
    
    /**
     * Calculate maximum memory difference between strategies.
     */
    private double calculateMaxMemoryDifference(List<StrategyComparison> comparisons) {
        if (comparisons.size() < 2) return 0;
        
        double maxMemory = comparisons.stream()
                .mapToDouble(c -> c.getResult().getMemoryMetrics().getPeakUsedMemory())
                .max()
                .orElse(0);
        
        double minMemory = comparisons.stream()
                .mapToDouble(c -> c.getResult().getMemoryMetrics().getPeakUsedMemory())
                .min()
                .orElse(0);
        
        return minMemory > 0 ? ((maxMemory - minMemory) / minMemory) * 100 : 0;
    }
    
    /**
     * Write content to file.
     */
    private void writeToFile(String filePath, String content) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(content);
        }
    }
    
    /**
     * Factory method for creating reporters with different configurations.
     */
    public static BenchmarkReporter forConfiguration(BenchmarkConfiguration config) {
        return new BenchmarkReporter(config);
    }
    
    /**
     * Create a reporter optimized for console output.
     */
    public static BenchmarkReporter forConsole() {
        return new BenchmarkReporter(BenchmarkConfiguration.forDevelopment());
    }
    
    /**
     * Create a reporter optimized for comprehensive analysis.
     */
    public static BenchmarkReporter forComprehensiveAnalysis() {
        return new BenchmarkReporter(BenchmarkConfiguration.forComprehensiveAnalysis());
    }
}