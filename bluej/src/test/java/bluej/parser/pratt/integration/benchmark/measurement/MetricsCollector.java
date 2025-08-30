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
import bluej.parser.pratt.integration.benchmark.metrics.*;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Central metrics collection orchestrator for parser integration benchmarking.
 * 
 * Coordinates multiple specialized monitors to collect comprehensive performance data:
 * - Memory usage tracking with JVM monitoring
 * - Performance profiling with timing and throughput
 * - Scalability analysis with linear regression
 * - Error recovery tracking with callback validation
 * 
 * Designed to work seamlessly with the BenchmarkCallbackBridge and provide
 * unified metrics collection across all parser integration strategies.
 */
public class MetricsCollector {
    
    private final MemoryMonitor memoryMonitor;
    private final PerformanceProfiler performanceProfiler;
    private final ScalabilityAnalyzer scalabilityAnalyzer;
    private final ErrorRecoveryTracker errorRecoveryTracker;
    
    private final ScheduledExecutorService scheduler;
    private final BenchmarkConfiguration config;
    private boolean collecting = false;
    private long collectionStartTime;
    
    public MetricsCollector(BenchmarkConfiguration config) {
        this.config = config;
        this.memoryMonitor = new MemoryMonitor(config);
        this.performanceProfiler = new PerformanceProfiler(config);
        this.scalabilityAnalyzer = new ScalabilityAnalyzer(config);
        this.errorRecoveryTracker = new ErrorRecoveryTracker(config);
        this.scheduler = Executors.newScheduledThreadPool(2);
    }
    
    /**
     * Start comprehensive metrics collection.
     */
    public void startCollection() {
        if (collecting) {
            throw new IllegalStateException("Metrics collection already in progress");
        }
        
        collecting = true;
        collectionStartTime = System.currentTimeMillis();
        
        memoryMonitor.start();
        performanceProfiler.start();
        scalabilityAnalyzer.start();
        errorRecoveryTracker.start();
        
        // Schedule periodic collection tasks
        if (config.getEnableMemoryProfiling()) {
            scheduler.scheduleAtFixedRate(
                this::collectMemorySnapshot,
                0,
                config.getMemoryMeasurementIntervalMs(),
                TimeUnit.MILLISECONDS
            );
        }
    }
    
    /**
     * Stop collection and compile final metrics.
     */
    public CollectedMetrics stopCollection() {
        if (!collecting) {
            throw new IllegalStateException("No active metrics collection");
        }
        
        collecting = false;
        long collectionDuration = System.currentTimeMillis() - collectionStartTime;
        
        MemoryMetrics memory = memoryMonitor.stop();
        PerformanceMetrics performance = performanceProfiler.stop();
        ScalabilityMetrics scalability = scalabilityAnalyzer.stop();
        ErrorRecoveryMetrics errorRecovery = errorRecoveryTracker.stop();
        
        scheduler.shutdown();
        
        return new CollectedMetrics(memory, performance, scalability, errorRecovery, collectionDuration);
    }
    
    /**
     * Record a callback event for analysis.
     */
    public void recordCallback(String callbackName, long timestamp, Object... args) {
        if (!collecting) return;
        
        performanceProfiler.recordCallback(callbackName, timestamp, args);
        scalabilityAnalyzer.recordCallback(callbackName, timestamp, args);
        errorRecoveryTracker.recordCallback(callbackName, timestamp, args);
    }
    
    /**
     * Record an error event for recovery analysis.
     */
    public void recordError(String errorType, long timestamp, boolean recovered) {
        if (!collecting) return;
        
        errorRecoveryTracker.recordError(errorType, timestamp, recovered);
    }
    
    /**
     * Add a measurement checkpoint.
     */
    public void addCheckpoint(String label) {
        if (!collecting) return;
        
        long timestamp = System.currentTimeMillis() - collectionStartTime;
        performanceProfiler.addCheckpoint(label, timestamp);
        scalabilityAnalyzer.addCheckpoint(label, timestamp);
        
        if (config.getEnableDetailedMetrics()) {
            collectMemorySnapshot();
        }
    }
    
    /**
     * Collect a memory usage snapshot.
     */
    private void collectMemorySnapshot() {
        if (collecting && config.getEnableMemoryProfiling()) {
            memoryMonitor.takeSnapshot();
        }
    }
    
    /**
     * Get current collection status.
     */
    public boolean isCollecting() {
        return collecting;
    }
    
    /**
     * Get elapsed collection time in milliseconds.
     */
    public long getElapsedTime() {
        return collecting ? System.currentTimeMillis() - collectionStartTime : 0;
    }
    
    /**
     * Cleanup resources.
     */
    public void cleanup() {
        if (collecting) {
            stopCollection();
        }
        
        if (!scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        
        memoryMonitor.cleanup();
        performanceProfiler.cleanup();
        scalabilityAnalyzer.cleanup();
        errorRecoveryTracker.cleanup();
    }
    
    /**
     * Record JMH benchmark metrics for integration with JMH framework.
     */
    public void recordJMHMetrics(String strategyName, String complexityLevel, BenchmarkResult result) {
        // This method integrates with JMH benchmarking results
        // Store the metrics for later analysis or reporting
        System.out.println("Recording JMH metrics for " + strategyName + " at " + complexityLevel + " complexity");
    }
    
    /**
     * Collect metrics for a completed benchmark.
     */
    public CollectedMetrics collectMetrics(String strategyName, BenchmarkResult result) {
        // Extract metrics from the benchmark result
        return new CollectedMetrics(
            result.getMemoryMetrics(),
            result.getPerformanceMetrics(),
            result.getScalabilityMetrics(),
            result.getErrorRecoveryMetrics(),
            (long) result.getExecutionTimeMs()
        );
    }
    
    /**
     * Container for all collected metrics.
     */
    public static class CollectedMetrics {
        private final MemoryMetrics memory;
        private final PerformanceMetrics performance;
        private final ScalabilityMetrics scalability;
        private final ErrorRecoveryMetrics errorRecovery;
        private final long collectionDurationMs;
        
        public CollectedMetrics(MemoryMetrics memory, 
                              PerformanceMetrics performance,
                              ScalabilityMetrics scalability, 
                              ErrorRecoveryMetrics errorRecovery,
                              long collectionDurationMs) {
            this.memory = memory;
            this.performance = performance;
            this.scalability = scalability;
            this.errorRecovery = errorRecovery;
            this.collectionDurationMs = collectionDurationMs;
        }
        
        public MemoryMetrics getMemory() { return memory; }
        public PerformanceMetrics getPerformance() { return performance; }
        public ScalabilityMetrics getScalability() { return scalability; }
        public ErrorRecoveryMetrics getErrorRecovery() { return errorRecovery; }
        public long getCollectionDurationMs() { return collectionDurationMs; }
        
        /**
         * Create a BenchmarkResult from collected metrics.
         */
        public BenchmarkResult toBenchmarkResult(String testName, int callbackCount, boolean balanced) {
            return new BenchmarkResult.Builder(testName)
                    .executionTime(collectionDurationMs)
                    .callbackCount(callbackCount)
                    .callbacksBalanced(balanced)
                    .memoryMetrics(memory)
                    .performanceMetrics(performance)
                    .scalabilityMetrics(scalability)
                    .errorRecoveryMetrics(errorRecovery)
                    .build();
        }
    }
    
    /**
     * Factory method for creating preconfigured metrics collectors.
     */
    public static MetricsCollector forConfiguration(BenchmarkConfiguration config) {
        return new MetricsCollector(config);
    }
    
    /**
     * Create a lightweight collector for development testing.
     */
    public static MetricsCollector lightweight() {
        BenchmarkConfiguration config = BenchmarkConfiguration.forDevelopment();
        return new MetricsCollector(config);
    }
    
    /**
     * Create a comprehensive collector for thorough analysis.
     */
    public static MetricsCollector comprehensive() {
        BenchmarkConfiguration config = BenchmarkConfiguration.forComprehensiveAnalysis();
        return new MetricsCollector(config);
    }
}