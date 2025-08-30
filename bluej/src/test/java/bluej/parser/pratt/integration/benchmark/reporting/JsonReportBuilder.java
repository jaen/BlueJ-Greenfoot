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
import bluej.parser.pratt.integration.benchmark.metrics.*;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * JSON report builder for parser integration strategy benchmarks.
 * 
 * Converts BenchmarkReport objects into structured JSON format suitable for:
 * - Programmatic analysis and tool integration
 * - Web dashboard consumption
 * - Data export and archival
 * - CI/CD pipeline integration
 * 
 * The JSON format is designed to be both human-readable and machine-parseable,
 * with complete preservation of all benchmark data and metadata.
 */
class JsonReportBuilder {
    
    private static final String INDENT = "  ";
    
    /**
     * Build complete JSON report from benchmark results.
     */
    public String buildJsonReport(BenchmarkReport report) {
        JsonBuilder json = new JsonBuilder();
        
        json.startObject();
        
        // Report metadata
        json.addProperty("reportTime", report.getReportTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        json.addProperty("reportScope", report.getReportScope());
        json.addProperty("strategyCount", report.getStrategyCount());
        json.addProperty("hasResults", report.hasResults());
        
        // Configuration
        if (report.getConfig() != null) {
            json.startObject("configuration");
            addConfigurationToJson(json, report);
            json.endObject();
        }
        
        // Strategy comparisons
        json.startArray("strategyComparisons");
        for (StrategyComparison comparison : report.getStrategyComparisons()) {
            json.startObject();
            addStrategyComparisonToJson(json, comparison);
            json.endObject();
        }
        json.endArray();
        
        // Performance ranking
        json.startObject("performanceRanking");
        addPerformanceRankingToJson(json, report.getRanking());
        json.endObject();
        
        // Summary
        json.startObject("summary");
        addSummaryToJson(json, report.getSummary());
        json.endObject();
        
        // Recommendations
        json.startArray("recommendations");
        for (StrategyRecommendation recommendation : report.getRecommendations()) {
            json.startObject();
            addRecommendationToJson(json, recommendation);
            json.endObject();
        }
        json.endArray();
        
        json.endObject();
        
        return json.toString();
    }
    
    /**
     * Add configuration details to JSON.
     */
    private void addConfigurationToJson(JsonBuilder json, BenchmarkReport report) {
        var config = report.getConfig();
        
        json.addProperty("warmupIterations", config.getWarmupIterations());
        json.addProperty("measurementIterations", config.getMeasurementIterations());
        json.addProperty("warmupTimeMs", config.getWarmupTimeMs());
        json.addProperty("measurementTimeMs", config.getMeasurementTimeMs());
        json.addProperty("samplesPerLevel", config.getSamplesPerLevel());
        json.addProperty("includeErrorCases", config.getIncludeErrorCases());
        json.addProperty("includeIncompleteCode", config.getIncludeIncompleteCode());
        json.addProperty("enableMemoryProfiling", config.getEnableMemoryProfiling());
        json.addProperty("enableDetailedMetrics", config.getEnableDetailedMetrics());
        json.addProperty("outputDirectory", config.getOutputDirectory());
        
        // Complexity levels
        json.startArray("complexityLevels");
        for (var level : config.getComplexityLevels()) {
            json.startObject();
            json.addProperty("name", level.name());
            json.addProperty("description", level.getDescription());
            json.addProperty("typicalLines", level.getTypicalLines());
            json.addProperty("typicalDepth", level.getTypicalDepth());
            json.endObject();
        }
        json.endArray();
        
        // Scalability test sizes
        json.startArray("scalabilityTestSizes");
        for (Integer size : config.getScalabilityTestSizes()) {
            json.addValue(size);
        }
        json.endArray();
    }
    
    /**
     * Add strategy comparison to JSON.
     */
    private void addStrategyComparisonToJson(JsonBuilder json, StrategyComparison comparison) {
        json.addProperty("strategyName", comparison.getStrategyName());
        json.addProperty("overallScore", comparison.getOverallScore());
        json.addProperty("performanceCategory", comparison.getPerformanceCategory().name());
        json.addProperty("performanceCategoryDescription", comparison.getPerformanceCategory().getDescription());
        
        // Characteristics
        if (comparison.getCharacteristics() != null) {
            json.startObject("characteristics");
            addCharacteristicsToJson(json, comparison.getCharacteristics());
            json.endObject();
        }
        
        // Benchmark result
        json.startObject("result");
        addBenchmarkResultToJson(json, comparison.getResult());
        json.endObject();
    }
    
    /**
     * Add strategy characteristics to JSON.
     */
    private void addCharacteristicsToJson(JsonBuilder json, StrategyCharacteristics characteristics) {
        json.addProperty("description", characteristics.getDescription());
        json.addProperty("approach", characteristics.getApproach().name());
        json.addProperty("approachDescription", characteristics.getApproach().getDescription());
        json.addProperty("implementationComplexity", characteristics.getImplementationComplexity().name());
        json.addProperty("maintainsCallbackIntegrity", characteristics.getMaintainsCallbackIntegrity());
        json.addProperty("supportsErrorRecovery", characteristics.getSupportsErrorRecovery());
        json.addProperty("supportsIncrementalParsing", characteristics.getSupportsIncrementalParsing());
        
        // Strengths
        json.startArray("strengths");
        for (var strength : characteristics.getStrengths()) {
            json.startObject();
            json.addProperty("name", strength.name());
            json.addProperty("description", strength.getDescription());
            json.endObject();
        }
        json.endArray();
        
        // Weaknesses
        json.startArray("weaknesses");
        for (var weakness : characteristics.getWeaknesses()) {
            json.startObject();
            json.addProperty("name", weakness.name());
            json.addProperty("description", weakness.getDescription());
            json.endObject();
        }
        json.endArray();
        
        // Ideal use cases
        json.startArray("idealUseCases");
        for (var useCase : characteristics.getIdealUseCases()) {
            json.startObject();
            json.addProperty("name", useCase.name());
            json.addProperty("description", useCase.getDescription());
            json.endObject();
        }
        json.endArray();
    }
    
    /**
     * Add benchmark result to JSON.
     */
    private void addBenchmarkResultToJson(JsonBuilder json, BenchmarkResult result) {
        json.addProperty("testName", result.getTestName());
        json.addProperty("executionTimeMs", result.getExecutionTimeMs());
        json.addProperty("callbackCount", result.getCallbackCount());
        json.addProperty("callbacksBalanced", result.isCallbacksBalanced());
        
        // Memory metrics
        json.startObject("memoryMetrics");
        var memory = result.getMemoryMetrics();
        json.addProperty("peakUsedMemory", memory.getPeakUsedMemory());
        json.addProperty("allocationRate", memory.getAllocationRate());
        json.addProperty("gcPressure", memory.getGcPressure());
        json.addProperty("retentionRate", memory.getRetentionRate());
        json.endObject();
        
        // Performance metrics
        json.startObject("performanceMetrics");
        var performance = result.getPerformanceMetrics();
        json.addProperty("totalParseTimeMs", performance.getTotalParseTimeMs());
        json.addProperty("throughput", performance.getThroughput());
        json.addProperty("averageCallbackTimeMs", performance.getAverageCallbackTimeMs());
        json.addProperty("minCallbackTimeMs", performance.getMinCallbackTimeMs());
        json.addProperty("maxCallbackTimeMs", performance.getMaxCallbackTimeMs());
        json.addProperty("consistency", performance.getConsistency());
        json.endObject();
        
        // Scalability metrics
        json.startObject("scalabilityMetrics");
        var scalability = result.getScalabilityMetrics();
        json.addProperty("linearCoefficient", scalability.getLinearCoefficient());
        json.addProperty("rSquared", scalability.getRSquared());
        json.addProperty("memoryGrowthRate", scalability.getMemoryGrowthRate());
        json.addProperty("dataPointCount", scalability.getDataPointCount());
        json.addProperty("linearScaling", scalability.isLinearScaling());
        json.addProperty("scalingCategory", scalability.getCategory().name());
        json.endObject();
        
        // Error recovery metrics
        json.startObject("errorRecoveryMetrics");
        var errorRecovery = result.getErrorRecoveryMetrics();
        json.addProperty("errorDetectionTimeMs", errorRecovery.getErrorDetectionTimeMs());
        json.addProperty("recoveryTimeMs", errorRecovery.getRecoveryTimeMs());
        json.addProperty("callbackPairingRate", errorRecovery.getCallbackPairingRate());
        json.addProperty("errorsDetected", errorRecovery.getErrorsDetected());
        json.addProperty("errorsRecovered", errorRecovery.getErrorsRecovered());
        json.addProperty("recoverySuccessRate", errorRecovery.getRecoverySuccessRate());
        json.addProperty("performanceImpactPercent", errorRecovery.getPerformanceImpactPercent());
        json.addProperty("errorHandlingScore", errorRecovery.getErrorHandlingScore());
        json.addProperty("maintainsCallbackIntegrity", errorRecovery.getMaintainsCallbackIntegrity());
        json.addProperty("robustErrorRecovery", errorRecovery.isRobustErrorRecovery());
        json.addProperty("lowPerformanceImpact", errorRecovery.hasLowPerformanceImpact());
        json.addProperty("category", errorRecovery.getCategory().name());
        
        // Error types handled
        json.startArray("errorTypesHandled");
        for (var errorType : errorRecovery.getErrorTypesHandled()) {
            json.startObject();
            json.addProperty("name", errorType.name());
            json.addProperty("description", errorType.getDescription());
            json.endObject();
        }
        json.endArray();
        json.endObject();
    }
    
    /**
     * Add performance ranking to JSON.
     */
    private void addPerformanceRankingToJson(JsonBuilder json, PerformanceRanking ranking) {
        // Overall ranking
        json.startArray("rankedByOverall");
        for (StrategyComparison comparison : ranking.getRankedByOverall()) {
            json.startObject();
            json.addProperty("strategyName", comparison.getStrategyName());
            json.addProperty("overallScore", comparison.getOverallScore());
            json.addProperty("rank", ranking.getOverallRank(comparison.getStrategyName()));
            json.endObject();
        }
        json.endArray();
        
        // Memory ranking
        json.startArray("rankedByMemory");
        for (int i = 0; i < ranking.getRankedByMemory().size(); i++) {
            StrategyComparison comparison = ranking.getRankedByMemory().get(i);
            json.startObject();
            json.addProperty("strategyName", comparison.getStrategyName());
            json.addProperty("peakMemoryUsage", comparison.getResult().getMemoryMetrics().getPeakUsedMemory());
            json.addProperty("rank", i + 1);
            json.endObject();
        }
        json.endArray();
        
        // Speed ranking
        json.startArray("rankedBySpeed");
        for (int i = 0; i < ranking.getRankedBySpeed().size(); i++) {
            StrategyComparison comparison = ranking.getRankedBySpeed().get(i);
            json.startObject();
            json.addProperty("strategyName", comparison.getStrategyName());
            json.addProperty("throughput", comparison.getResult().getPerformanceMetrics().getThroughput());
            json.addProperty("rank", i + 1);
            json.endObject();
        }
        json.endArray();
        
        // Scalability ranking
        json.startArray("rankedByScalability");
        for (int i = 0; i < ranking.getRankedByScalability().size(); i++) {
            StrategyComparison comparison = ranking.getRankedByScalability().get(i);
            json.startObject();
            json.addProperty("strategyName", comparison.getStrategyName());
            json.addProperty("rSquared", comparison.getResult().getScalabilityMetrics().getRSquared());
            json.addProperty("rank", i + 1);
            json.endObject();
        }
        json.endArray();
    }
    
    /**
     * Add summary to JSON.
     */
    private void addSummaryToJson(JsonBuilder json, BenchmarkSummary summary) {
        if (summary.getBestStrategy() != null) {
            json.startObject("bestStrategy");
            json.addProperty("strategyName", summary.getBestStrategy().getStrategyName());
            json.addProperty("overallScore", summary.getBestStrategy().getOverallScore());
            json.endObject();
        }
        
        if (summary.getWorstStrategy() != null) {
            json.startObject("worstStrategy");
            json.addProperty("strategyName", summary.getWorstStrategy().getStrategyName());
            json.addProperty("overallScore", summary.getWorstStrategy().getOverallScore());
            json.endObject();
        }
        
        json.addProperty("averageScore", summary.getAverageScore());
        json.addProperty("performanceSpread", summary.getPerformanceSpread());
        json.addProperty("maxSpeedDifferencePercent", summary.getMaxSpeedDifferencePercent());
        json.addProperty("maxMemoryDifferencePercent", summary.getMaxMemoryDifferencePercent());
        json.addProperty("hasSignificantVariation", summary.hasSignificantVariation());
    }
    
    /**
     * Add recommendation to JSON.
     */
    private void addRecommendationToJson(JsonBuilder json, StrategyRecommendation recommendation) {
        json.addProperty("category", recommendation.getCategory().getDisplayName());
        json.addProperty("strategyName", recommendation.getStrategyName());
        json.addProperty("reasoning", recommendation.getReasoning());
        json.addProperty("useCase", recommendation.getUseCase().name());
        json.addProperty("useCaseDescription", recommendation.getUseCase().getDescription());
    }
    
    /**
     * Simple JSON builder for constructing JSON strings.
     */
    private static class JsonBuilder {
        private final StringBuilder json = new StringBuilder();
        private int indentLevel = 0;
        private boolean firstProperty = true;
        
        public void startObject() {
            json.append("{\n");
            indentLevel++;
            firstProperty = true;
        }
        
        public void startObject(String name) {
            addCommaIfNeeded();
            addIndent();
            json.append("\"").append(name).append("\": {\n");
            indentLevel++;
            firstProperty = true;
        }
        
        public void endObject() {
            json.append("\n");
            indentLevel--;
            addIndent();
            json.append("}");
            firstProperty = false;
        }
        
        public void startArray(String name) {
            addCommaIfNeeded();
            addIndent();
            json.append("\"").append(name).append("\": [\n");
            indentLevel++;
            firstProperty = true;
        }
        
        public void endArray() {
            json.append("\n");
            indentLevel--;
            addIndent();
            json.append("]");
            firstProperty = false;
        }
        
        public void addProperty(String name, String value) {
            addCommaIfNeeded();
            addIndent();
            json.append("\"").append(name).append("\": \"").append(escapeJson(value)).append("\"");
            firstProperty = false;
        }
        
        public void addProperty(String name, int value) {
            addCommaIfNeeded();
            addIndent();
            json.append("\"").append(name).append("\": ").append(value);
            firstProperty = false;
        }
        
        public void addProperty(String name, long value) {
            addCommaIfNeeded();
            addIndent();
            json.append("\"").append(name).append("\": ").append(value);
            firstProperty = false;
        }
        
        public void addProperty(String name, double value) {
            addCommaIfNeeded();
            addIndent();
            json.append("\"").append(name).append("\": ").append(value);
            firstProperty = false;
        }
        
        public void addProperty(String name, boolean value) {
            addCommaIfNeeded();
            addIndent();
            json.append("\"").append(name).append("\": ").append(value);
            firstProperty = false;
        }
        
        public void addValue(int value) {
            addCommaIfNeeded();
            addIndent();
            json.append(value);
            firstProperty = false;
        }
        
        private void addCommaIfNeeded() {
            if (!firstProperty) {
                json.append(",\n");
            }
        }
        
        private void addIndent() {
            json.append(INDENT.repeat(indentLevel));
        }
        
        private String escapeJson(String str) {
            if (str == null) return "";
            return str.replace("\\", "\\\\")
                     .replace("\"", "\\\"")
                     .replace("\n", "\\n")
                     .replace("\r", "\\r")
                     .replace("\t", "\\t");
        }
        
        @Override
        public String toString() {
            return json.toString();
        }
    }
}