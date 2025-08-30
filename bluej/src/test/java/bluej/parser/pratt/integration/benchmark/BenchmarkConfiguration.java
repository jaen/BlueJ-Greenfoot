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

import java.util.List;
import java.util.Arrays;
import java.util.Collections;

/**
 * Configuration settings for parser integration strategy benchmarks.
 * 
 * Controls all aspects of benchmark execution including:
 * - Test corpus selection and complexity levels
 * - Warmup and measurement timing parameters
 * - Memory and performance monitoring settings
 * - Output format and reporting options
 * 
 * Provides preset configurations for different benchmarking scenarios:
 * - Quick development testing
 * - Comprehensive strategy comparison
 * - Performance regression testing
 * - Scalability analysis
 */
public class BenchmarkConfiguration {
    // Test corpus configuration
    private final List<ComplexityLevel> complexityLevels;
    private final int samplesPerLevel;
    private final boolean includeErrorCases;
    private final boolean includeIncompleteCode;
    
    // Timing configuration
    private final int warmupIterations;
    private final int measurementIterations;
    private final long warmupTimeMs;
    private final long measurementTimeMs;
    
    // Memory monitoring configuration
    private final boolean enableMemoryProfiling;
    private final int gcBetweenMeasurements;
    private final long memoryMeasurementIntervalMs;
    
    // Scalability testing configuration
    private final List<Integer> scalabilityTestSizes;
    private final boolean enableLinearityAnalysis;
    
    // Output configuration
    private final boolean enableConsoleOutput;
    private final boolean enableJsonOutput;
    private final boolean enableDetailedMetrics;
    private final String outputDirectory;
    
    // JMH integration
    private final boolean useJmhRunner;
    private final String jmhOutputFormat;
    
    private BenchmarkConfiguration(Builder builder) {
        this.complexityLevels = Collections.unmodifiableList(builder.complexityLevels);
        this.samplesPerLevel = builder.samplesPerLevel;
        this.includeErrorCases = builder.includeErrorCases;
        this.includeIncompleteCode = builder.includeIncompleteCode;
        this.warmupIterations = builder.warmupIterations;
        this.measurementIterations = builder.measurementIterations;
        this.warmupTimeMs = builder.warmupTimeMs;
        this.measurementTimeMs = builder.measurementTimeMs;
        this.enableMemoryProfiling = builder.enableMemoryProfiling;
        this.gcBetweenMeasurements = builder.gcBetweenMeasurements;
        this.memoryMeasurementIntervalMs = builder.memoryMeasurementIntervalMs;
        this.scalabilityTestSizes = Collections.unmodifiableList(builder.scalabilityTestSizes);
        this.enableLinearityAnalysis = builder.enableLinearityAnalysis;
        this.enableConsoleOutput = builder.enableConsoleOutput;
        this.enableJsonOutput = builder.enableJsonOutput;
        this.enableDetailedMetrics = builder.enableDetailedMetrics;
        this.outputDirectory = builder.outputDirectory;
        this.useJmhRunner = builder.useJmhRunner;
        this.jmhOutputFormat = builder.jmhOutputFormat;
    }
    
    // Getters
    public List<ComplexityLevel> getComplexityLevels() { return complexityLevels; }
    public int getSamplesPerLevel() { return samplesPerLevel; }
    public boolean getIncludeErrorCases() { return includeErrorCases; }
    public boolean getIncludeIncompleteCode() { return includeIncompleteCode; }
    public int getWarmupIterations() { return warmupIterations; }
    public int getMeasurementIterations() { return measurementIterations; }
    public long getWarmupTimeMs() { return warmupTimeMs; }
    public long getMeasurementTimeMs() { return measurementTimeMs; }
    public boolean getEnableMemoryProfiling() { return enableMemoryProfiling; }
    public int getGcBetweenMeasurements() { return gcBetweenMeasurements; }
    public long getMemoryMeasurementIntervalMs() { return memoryMeasurementIntervalMs; }
    public List<Integer> getScalabilityTestSizes() { return scalabilityTestSizes; }
    public boolean getEnableLinearityAnalysis() { return enableLinearityAnalysis; }
    public boolean getEnableConsoleOutput() { return enableConsoleOutput; }
    public boolean getEnableJsonOutput() { return enableJsonOutput; }
    public boolean getEnableDetailedMetrics() { return enableDetailedMetrics; }
    public String getOutputDirectory() { return outputDirectory; }
    public boolean getUseJmhRunner() { return useJmhRunner; }
    public String getJmhOutputFormat() { return jmhOutputFormat; }
    
    /**
     * Code complexity levels for test corpus generation.
     */
    public enum ComplexityLevel {
        SIMPLE("Simple expressions and statements", 50, 5),
        MODERATE("Classes with methods and control flow", 200, 15),
        COMPLEX("Nested classes, generics, annotations", 500, 30),
        VERY_COMPLEX("Large files with complex inheritance", 1000, 50);
        
        private final String description;
        private final int typicalLines;
        private final int typicalDepth;
        
        ComplexityLevel(String description, int typicalLines, int typicalDepth) {
            this.description = description;
            this.typicalLines = typicalLines;
            this.typicalDepth = typicalDepth;
        }
        
        public String getDescription() { return description; }
        public int getTypicalLines() { return typicalLines; }
        public int getTypicalDepth() { return typicalDepth; }
    }
    
    /**
     * Quick configuration for development testing.
     */
    public static BenchmarkConfiguration forDevelopment() {
        return new Builder()
                .complexityLevels(ComplexityLevel.SIMPLE, ComplexityLevel.MODERATE)
                .samplesPerLevel(5)
                .warmupIterations(2)
                .measurementIterations(3)
                .enableConsoleOutput(true)
                .build();
    }
    
    /**
     * Comprehensive configuration for thorough strategy comparison.
     */
    public static BenchmarkConfiguration forComprehensiveAnalysis() {
        return new Builder()
                .allComplexityLevels()
                .samplesPerLevel(20)
                .includeErrorCases(true)
                .includeIncompleteCode(true)
                .warmupIterations(5)
                .measurementIterations(10)
                .enableMemoryProfiling(true)
                .enableLinearityAnalysis(true)
                .enableDetailedMetrics(true)
                .enableJsonOutput(true)
                .outputDirectory("benchmark-results")
                .build();
    }
    
    /**
     * Performance regression testing configuration.
     */
    public static BenchmarkConfiguration forRegressionTesting() {
        return new Builder()
                .complexityLevels(ComplexityLevel.MODERATE, ComplexityLevel.COMPLEX)
                .samplesPerLevel(15)
                .warmupIterations(3)
                .measurementIterations(7)
                .useJmhRunner(true)
                .jmhOutputFormat("json")
                .enableJsonOutput(true)
                .build();
    }
    
    /**
     * Scalability analysis configuration.
     */
    public static BenchmarkConfiguration forScalabilityAnalysis() {
        return new Builder()
                .allComplexityLevels()
                .samplesPerLevel(10)
                .scalabilityTestSizes(10, 50, 100, 500, 1000)
                .enableLinearityAnalysis(true)
                .enableMemoryProfiling(true)
                .memoryMeasurementIntervalMs(100)
                .enableDetailedMetrics(true)
                .build();
    }
    
    /**
     * Create a new Builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder class for creating benchmark configurations.
     */
    public static class Builder {
        private List<ComplexityLevel> complexityLevels = Arrays.asList(ComplexityLevel.SIMPLE, ComplexityLevel.MODERATE);
        private int samplesPerLevel = 10;
        private boolean includeErrorCases = false;
        private boolean includeIncompleteCode = false;
        private int warmupIterations = 3;
        private int measurementIterations = 5;
        private long warmupTimeMs = 1000;
        private long measurementTimeMs = 2000;
        private boolean enableMemoryProfiling = false;
        private int gcBetweenMeasurements = 0;
        private long memoryMeasurementIntervalMs = 250;
        private List<Integer> scalabilityTestSizes = Arrays.asList(10, 50, 100);
        private boolean enableLinearityAnalysis = false;
        private boolean enableConsoleOutput = true;
        private boolean enableJsonOutput = false;
        private boolean enableDetailedMetrics = false;
        private String outputDirectory = "target/benchmark-results";
        private boolean useJmhRunner = false;
        private String jmhOutputFormat = "text";
        
        public Builder complexityLevels(ComplexityLevel... levels) {
            this.complexityLevels = Arrays.asList(levels);
            return this;
        }
        
        public Builder allComplexityLevels() {
            this.complexityLevels = Arrays.asList(ComplexityLevel.values());
            return this;
        }
        
        public Builder samplesPerLevel(int samples) {
            this.samplesPerLevel = samples;
            return this;
        }
        
        public Builder includeErrorCases(boolean include) {
            this.includeErrorCases = include;
            return this;
        }
        
        public Builder includeIncompleteCode(boolean include) {
            this.includeIncompleteCode = include;
            return this;
        }
        
        public Builder warmupIterations(int iterations) {
            this.warmupIterations = iterations;
            return this;
        }
        
        public Builder measurementIterations(int iterations) {
            this.measurementIterations = iterations;
            return this;
        }
        
        public Builder warmupTimeMs(long timeMs) {
            this.warmupTimeMs = timeMs;
            return this;
        }
        
        public Builder measurementTimeMs(long timeMs) {
            this.measurementTimeMs = timeMs;
            return this;
        }
        
        public Builder enableMemoryProfiling(boolean enable) {
            this.enableMemoryProfiling = enable;
            return this;
        }
        
        public Builder gcBetweenMeasurements(int gcCount) {
            this.gcBetweenMeasurements = gcCount;
            return this;
        }
        
        public Builder memoryMeasurementIntervalMs(long intervalMs) {
            this.memoryMeasurementIntervalMs = intervalMs;
            return this;
        }
        
        public Builder scalabilityTestSizes(Integer... sizes) {
            this.scalabilityTestSizes = Arrays.asList(sizes);
            return this;
        }
        
        public Builder enableLinearityAnalysis(boolean enable) {
            this.enableLinearityAnalysis = enable;
            return this;
        }
        
        public Builder enableConsoleOutput(boolean enable) {
            this.enableConsoleOutput = enable;
            return this;
        }
        
        public Builder enableJsonOutput(boolean enable) {
            this.enableJsonOutput = enable;
            return this;
        }
        
        public Builder enableDetailedMetrics(boolean enable) {
            this.enableDetailedMetrics = enable;
            return this;
        }
        
        public Builder outputDirectory(String directory) {
            this.outputDirectory = directory;
            return this;
        }
        
        public Builder useJmhRunner(boolean use) {
            this.useJmhRunner = use;
            return this;
        }
        
        public Builder jmhOutputFormat(String format) {
            this.jmhOutputFormat = format;
            return this;
        }
        
        public BenchmarkConfiguration build() {
            return new BenchmarkConfiguration(this);
        }
    }
    
    @Override
    public String toString() {
        return String.format("BenchmarkConfiguration{levels=%s, samples=%d, iterations=%d/%d}",
                complexityLevels, samplesPerLevel, warmupIterations, measurementIterations);
    }
}