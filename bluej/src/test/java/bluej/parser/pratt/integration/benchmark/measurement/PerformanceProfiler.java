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
package bluej.parser.pratt.integration.benchmark.measurement;

import bluej.parser.pratt.integration.benchmark.BenchmarkConfiguration;
import bluej.parser.pratt.integration.benchmark.metrics.PerformanceMetrics;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Performance profiling for parser integration strategy benchmarking.
 * 
 * Tracks detailed timing and throughput metrics during parsing operations:
 * - Individual callback execution times
 * - Total parsing duration and throughput
 * - Performance consistency and variance
 * - Timing distribution analysis
 * 
 * Designed to highlight performance differences between integration strategies:
 * - Direct Callback: Fast, consistent timing
 * - AST Visitor: Slower due to tree construction overhead
 * - Lazy AST: Variable performance based on evaluation patterns
 * - K2 Parser: Potentially high throughput with incremental features
 * - Error Recovery: Additional overhead for error handling
 */
public class PerformanceProfiler {
    
    private final BenchmarkConfiguration config;
    private boolean profiling = false;
    private long profilingStartTime;
    private long profilingStartNanos;
    
    // Callback timing tracking
    private final List<CallbackTiming> callbackTimings;
    private final Map<String, CallbackStats> callbackStats;
    private final Map<String, Long> checkpoints;
    
    // High-precision timing
    private long lastCallbackTime;
    private int totalCallbacks = 0;
    
    public PerformanceProfiler(BenchmarkConfiguration config) {
        this.config = config;
        this.callbackTimings = new ArrayList<>();
        this.callbackStats = new ConcurrentHashMap<>();
        this.checkpoints = new ConcurrentHashMap<>();
    }
    
    /**
     * Start performance profiling session.
     */
    public void start() {
        if (profiling) {
            throw new IllegalStateException("Performance profiling already active");
        }
        
        profiling = true;
        profilingStartTime = System.currentTimeMillis();
        profilingStartNanos = System.nanoTime();
        lastCallbackTime = profilingStartNanos;
        
        callbackTimings.clear();
        callbackStats.clear();
        checkpoints.clear();
        totalCallbacks = 0;
    }
    
    /**
     * Stop profiling and calculate performance metrics.
     */
    public PerformanceMetrics stop() {
        if (!profiling) {
            throw new IllegalStateException("No active performance profiling");
        }
        
        profiling = false;
        long endTime = System.currentTimeMillis();
        long endNanos = System.nanoTime();
        
        double totalTimeMs = (endNanos - profilingStartNanos) / 1_000_000.0;
        
        return calculatePerformanceMetrics(totalTimeMs);
    }
    
    /**
     * Record a callback execution for analysis.
     */
    public void recordCallback(String callbackName, long timestamp, Object... args) {
        if (!profiling) return;
        
        long currentNanos = System.nanoTime();
        long callbackDuration = currentNanos - lastCallbackTime;
        
        // Record detailed timing if enabled
        if (config.getEnableDetailedMetrics()) {
            CallbackTiming timing = new CallbackTiming(
                callbackName,
                currentNanos - profilingStartNanos,
                callbackDuration,
                args != null ? args.length : 0
            );
            callbackTimings.add(timing);
        }
        
        // Update statistics
        callbackStats.computeIfAbsent(callbackName, k -> new CallbackStats())
                   .addTiming(callbackDuration);
        
        lastCallbackTime = currentNanos;
        totalCallbacks++;
    }
    
    /**
     * Add a performance checkpoint.
     */
    public void addCheckpoint(String label, long timestamp) {
        if (profiling) {
            checkpoints.put(label, timestamp);
        }
    }
    
    /**
     * Calculate comprehensive performance metrics.
     */
    private PerformanceMetrics calculatePerformanceMetrics(double totalTimeMs) {
        // Basic metrics
        double throughput = totalCallbacks > 0 ? (totalCallbacks / (totalTimeMs / 1000.0)) : 0;
        
        // Timing analysis
        TimingAnalysis timing = analyzeTiming();
        
        // Consistency analysis
        double consistency = calculateConsistency();
        
        return new PerformanceMetrics.Builder()
                .totalParseTime(totalTimeMs)
                .throughput(throughput)
                .averageCallbackTime(timing.averageCallbackTime)
                .minCallbackTime(timing.minCallbackTime)
                .maxCallbackTime(timing.maxCallbackTime)
                .consistency(consistency)
                .dataPointCount(totalCallbacks)
                .performanceImpact(0.0)
                .build();
    }
    
    /**
     * Analyze callback timing patterns.
     */
    private TimingAnalysis analyzeTiming() {
        if (callbackTimings.isEmpty() && callbackStats.isEmpty()) {
            return new TimingAnalysis(0, 0, 0);
        }
        
        // Collect all timing data
        List<Double> allTimings = new ArrayList<>();
        
        for (CallbackStats stats : callbackStats.values()) {
            for (long timing : stats.timings) {
                allTimings.add(timing / 1_000_000.0); // Convert to milliseconds
            }
        }
        
        if (allTimings.isEmpty()) {
            return new TimingAnalysis(0, 0, 0);
        }
        
        Collections.sort(allTimings);
        
        double min = allTimings.get(0);
        double max = allTimings.get(allTimings.size() - 1);
        double average = allTimings.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        
        return new TimingAnalysis(average, min, max);
    }
    
    /**
     * Calculate performance consistency score.
     */
    private double calculateConsistency() {
        if (callbackStats.isEmpty()) {
            return 1.0;
        }
        
        // Calculate coefficient of variation across all callbacks
        List<Double> coefficients = new ArrayList<>();
        
        for (CallbackStats stats : callbackStats.values()) {
            if (stats.timings.size() > 1) {
                double cv = stats.getCoefficientOfVariation();
                if (!Double.isNaN(cv)) {
                    coefficients.add(cv);
                }
            }
        }
        
        if (coefficients.isEmpty()) {
            return 1.0;
        }
        
        // Average coefficient of variation
        double avgCoefficient = coefficients.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        
        // Convert to consistency score (lower coefficient = higher consistency)
        return Math.max(0, Math.min(1, 1.0 - (avgCoefficient / 2.0)));
    }
    
    /**
     * Get performance statistics for specific callback.
     */
    public CallbackStats getCallbackStats(String callbackName) {
        return callbackStats.get(callbackName);
    }
    
    /**
     * Get all callback performance statistics.
     */
    public Map<String, CallbackStats> getAllCallbackStats() {
        return new HashMap<>(callbackStats);
    }
    
    /**
     * Get detailed timing information.
     */
    public List<CallbackTiming> getDetailedTimings() {
        return new ArrayList<>(callbackTimings);
    }
    
    /**
     * Get checkpoint timings.
     */
    public Map<String, Long> getCheckpoints() {
        return new HashMap<>(checkpoints);
    }
    
    /**
     * Get current profiling status.
     */
    public boolean isProfiling() {
        return profiling;
    }
    
    /**
     * Get total number of recorded callbacks.
     */
    public int getTotalCallbacks() {
        return totalCallbacks;
    }
    
    /**
     * Cleanup profiler resources.
     */
    public void cleanup() {
        if (profiling) {
            stop();
        }
        callbackTimings.clear();
        callbackStats.clear();
        checkpoints.clear();
    }
    
    /**
     * Individual callback timing record.
     */
    public static class CallbackTiming {
        public final String name;
        public final long timestampNanos;
        public final long durationNanos;
        public final int argumentCount;
        
        public CallbackTiming(String name, long timestampNanos, long durationNanos, int argumentCount) {
            this.name = name;
            this.timestampNanos = timestampNanos;
            this.durationNanos = durationNanos;
            this.argumentCount = argumentCount;
        }
        
        public double getDurationMs() {
            return durationNanos / 1_000_000.0;
        }
        
        public double getTimestampMs() {
            return timestampNanos / 1_000_000.0;
        }
        
        @Override
        public String toString() {
            return String.format("CallbackTiming[%s, %.3fms, args=%d]", 
                    name, getDurationMs(), argumentCount);
        }
    }
    
    /**
     * Statistical analysis for a specific callback type.
     */
    public static class CallbackStats {
        private final List<Long> timings = new ArrayList<>();
        private double sum = 0;
        private double sumSquares = 0;
        private long min = Long.MAX_VALUE;
        private long max = Long.MIN_VALUE;
        
        public void addTiming(long nanoseconds) {
            timings.add(nanoseconds);
            
            double ms = nanoseconds / 1_000_000.0;
            sum += ms;
            sumSquares += ms * ms;
            min = Math.min(min, nanoseconds);
            max = Math.max(max, nanoseconds);
        }
        
        public int getCount() {
            return timings.size();
        }
        
        public double getAverageMs() {
            return timings.isEmpty() ? 0 : sum / timings.size();
        }
        
        public double getMinMs() {
            return timings.isEmpty() ? 0 : min / 1_000_000.0;
        }
        
        public double getMaxMs() {
            return timings.isEmpty() ? 0 : max / 1_000_000.0;
        }
        
        public double getStandardDeviationMs() {
            if (timings.size() < 2) return 0;
            
            double mean = getAverageMs();
            double variance = (sumSquares / timings.size()) - (mean * mean);
            return Math.sqrt(Math.max(0, variance));
        }
        
        public double getCoefficientOfVariation() {
            double mean = getAverageMs();
            double stdDev = getStandardDeviationMs();
            return mean > 0 ? stdDev / mean : 0;
        }
        
        public double getMedianMs() {
            if (timings.isEmpty()) return 0;
            
            List<Long> sorted = new ArrayList<>(timings);
            Collections.sort(sorted);
            
            int size = sorted.size();
            if (size % 2 == 0) {
                return (sorted.get(size/2 - 1) + sorted.get(size/2)) / 2_000_000.0;
            } else {
                return sorted.get(size/2) / 1_000_000.0;
            }
        }
        
        public double getPercentile(double percentile) {
            if (timings.isEmpty() || percentile < 0 || percentile > 100) {
                return 0;
            }
            
            List<Long> sorted = new ArrayList<>(timings);
            Collections.sort(sorted);
            
            int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
            index = Math.max(0, Math.min(index, sorted.size() - 1));
            
            return sorted.get(index) / 1_000_000.0;
        }
        
        @Override
        public String toString() {
            return String.format("CallbackStats[count=%d, avg=%.3fms, min=%.3fms, max=%.3fms, cv=%.3f]",
                    getCount(), getAverageMs(), getMinMs(), getMaxMs(), getCoefficientOfVariation());
        }
    }
    
    /**
     * Helper class for timing analysis results.
     */
    private static class TimingAnalysis {
        final double averageCallbackTime;
        final double minCallbackTime;
        final double maxCallbackTime;
        
        TimingAnalysis(double average, double min, double max) {
            this.averageCallbackTime = average;
            this.minCallbackTime = min;
            this.maxCallbackTime = max;
        }
    }
    
    /**
     * Get performance summary for debugging.
     */
    public String getPerformanceSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Performance Profiler Summary:\n");
        summary.append(String.format("  Profiling: %s\n", profiling));
        summary.append(String.format("  Total Callbacks: %d\n", totalCallbacks));
        summary.append(String.format("  Callback Types: %d\n", callbackStats.size()));
        summary.append(String.format("  Detailed Timings: %d\n", callbackTimings.size()));
        summary.append(String.format("  Checkpoints: %d\n", checkpoints.size()));
        
        if (!callbackStats.isEmpty()) {
            summary.append("  Top Callbacks by Count:\n");
            callbackStats.entrySet().stream()
                    .sorted((e1, e2) -> Integer.compare(e2.getValue().getCount(), e1.getValue().getCount()))
                    .limit(5)
                    .forEach(entry -> summary.append(String.format("    %s: %s\n", 
                            entry.getKey(), entry.getValue())));
        }
        
        return summary.toString();
    }
}