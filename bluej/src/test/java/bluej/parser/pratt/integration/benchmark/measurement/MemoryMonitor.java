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
import bluej.parser.pratt.integration.benchmark.metrics.MemoryMetrics;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.GarbageCollectorMXBean;
import java.util.List;
import java.util.ArrayList;

/**
 * Specialized memory usage monitoring for parser integration benchmarking.
 * 
 * Tracks JVM memory consumption patterns during parsing operations:
 * - Heap memory usage (used, committed, max)
 * - Memory allocation rate estimation
 * - Garbage collection pressure analysis
 * - Memory retention patterns
 * 
 * Provides detailed insights into memory efficiency differences between
 * integration strategies, particularly useful for comparing:
 * - Direct Callback (minimal memory overhead)
 * - AST Visitor (higher memory for tree construction)
 * - Lazy AST (dynamic memory management)
 * - Hybrid patterns (variable memory usage)
 */
public class MemoryMonitor {
    
    private final BenchmarkConfiguration config;
    private final MemoryMXBean memoryBean;
    private final List<GarbageCollectorMXBean> gcBeans;
    
    // Monitoring state
    private boolean monitoring = false;
    private MemoryUsage startUsage;
    private long startTime;
    private long initialGcCollections;
    private long initialGcTime;
    
    // Snapshots for detailed tracking
    private final List<MemorySnapshot> snapshots;
    private long peakUsedMemory = 0;
    
    public MemoryMonitor(BenchmarkConfiguration config) {
        this.config = config;
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        this.snapshots = new ArrayList<>();
    }
    
    /**
     * Start memory monitoring session.
     */
    public void start() {
        if (monitoring) {
            throw new IllegalStateException("Memory monitoring already active");
        }
        
        monitoring = true;
        startTime = System.currentTimeMillis();
        
        // Force garbage collection for clean baseline
        if (config.getGcBetweenMeasurements() > 0) {
            for (int i = 0; i < config.getGcBetweenMeasurements(); i++) {
                System.gc();
                try {
                    Thread.sleep(10); // Allow GC to complete
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        // Record baseline
        startUsage = memoryBean.getHeapMemoryUsage();
        peakUsedMemory = startUsage.getUsed();
        initialGcCollections = getTotalGcCollections();
        initialGcTime = getTotalGcTime();
        
        snapshots.clear();
        takeSnapshot("start");
    }
    
    /**
     * Stop monitoring and calculate final memory metrics.
     */
    public MemoryMetrics stop() {
        if (!monitoring) {
            throw new IllegalStateException("No active memory monitoring");
        }
        
        monitoring = false;
        
        MemoryUsage endUsage = memoryBean.getHeapMemoryUsage();
        takeSnapshot("end");
        
        long endTime = System.currentTimeMillis();
        long finalGcCollections = getTotalGcCollections();
        long finalGcTime = getTotalGcTime();
        
        return calculateMemoryMetrics(endUsage, endTime, finalGcCollections, finalGcTime);
    }
    
    /**
     * Take a memory usage snapshot at current time.
     */
    public void takeSnapshot() {
        takeSnapshot(null);
    }
    
    /**
     * Take a labeled memory usage snapshot.
     */
    public void takeSnapshot(String label) {
        if (!monitoring) return;
        
        MemoryUsage current = memoryBean.getHeapMemoryUsage();
        long timestamp = System.currentTimeMillis() - startTime;
        
        // Track peak memory usage
        peakUsedMemory = Math.max(peakUsedMemory, current.getUsed());
        
        snapshots.add(new MemorySnapshot(timestamp, current, label));
    }
    
    /**
     * Calculate comprehensive memory metrics from monitoring session.
     */
    private MemoryMetrics calculateMemoryMetrics(MemoryUsage endUsage, 
                                               long endTime, 
                                               long finalGcCollections, 
                                               long finalGcTime) {
        
        // Basic memory usage
        long memoryUsed = endUsage.getUsed() - startUsage.getUsed();
        long peakUsed = peakUsedMemory;
        
        // Calculate allocation rate
        double durationSeconds = (endTime - startTime) / 1000.0;
        double allocationRate = durationSeconds > 0 ? memoryUsed / durationSeconds : 0;
        
        // Calculate GC pressure
        long gcCollections = finalGcCollections - initialGcCollections;
        long gcTime = finalGcTime - initialGcTime;
        double gcPressure = calculateGcPressure(gcCollections, gcTime, durationSeconds);
        
        // Calculate memory retention rate
        double retentionRate = calculateRetentionRate();
        
        return new MemoryMetrics.Builder()
                .peakUsedMemory(peakUsed)
                .allocationRate(allocationRate)
                .gcPressure(gcPressure)
                .retentionRate(retentionRate)
                .gcCollections(0)
                .gcTimeMs(0)
                .build();
    }
    
    /**
     * Calculate garbage collection pressure metric.
     */
    private double calculateGcPressure(long gcCollections, long gcTimeMs, double durationSeconds) {
        if (durationSeconds <= 0) return 0.0;
        
        // Combine collection frequency and time spent in GC
        double collectionRate = gcCollections / durationSeconds; // collections per second
        double gcTimePercent = (gcTimeMs / 1000.0) / durationSeconds * 100; // % time in GC
        
        // Weighted combination: frequency matters more than time
        return (collectionRate * 0.7) + (gcTimePercent * 0.3);
    }
    
    /**
     * Calculate memory retention rate based on snapshots.
     */
    private double calculateRetentionRate() {
        if (snapshots.size() < 2) {
            return startUsage.getUsed() > 0 ? 1.0 : 0.0;
        }
        
        // Find minimum and maximum memory usage
        long minUsage = Long.MAX_VALUE;
        long maxUsage = Long.MIN_VALUE;
        
        for (MemorySnapshot snapshot : snapshots) {
            minUsage = Math.min(minUsage, snapshot.usage.getUsed());
            maxUsage = Math.max(maxUsage, snapshot.usage.getUsed());
        }
        
        // Retention rate: how much memory stayed allocated
        if (maxUsage == minUsage) return 1.0;
        
        MemorySnapshot lastSnapshot = snapshots.get(snapshots.size() - 1);
        double retention = (double) lastSnapshot.usage.getUsed() / maxUsage;
        
        return Math.max(0.0, Math.min(1.0, retention));
    }
    
    /**
     * Get total garbage collection count across all collectors.
     */
    private long getTotalGcCollections() {
        return gcBeans.stream()
                .mapToLong(bean -> bean.getCollectionCount() >= 0 ? bean.getCollectionCount() : 0)
                .sum();
    }
    
    /**
     * Get total garbage collection time across all collectors.
     */
    private long getTotalGcTime() {
        return gcBeans.stream()
                .mapToLong(bean -> bean.getCollectionTime() >= 0 ? bean.getCollectionTime() : 0)
                .sum();
    }
    
    /**
     * Get current memory usage for external monitoring.
     */
    public MemoryUsage getCurrentUsage() {
        return memoryBean.getHeapMemoryUsage();
    }
    
    /**
     * Get all collected snapshots.
     */
    public List<MemorySnapshot> getSnapshots() {
        return new ArrayList<>(snapshots);
    }
    
    /**
     * Check if monitoring is active.
     */
    public boolean isMonitoring() {
        return monitoring;
    }
    
    /**
     * Cleanup monitor resources.
     */
    public void cleanup() {
        if (monitoring) {
            stop();
        }
        snapshots.clear();
    }
    
    /**
     * Memory usage snapshot with timestamp and optional label.
     */
    public static class MemorySnapshot {
        public final long timestamp;
        public final MemoryUsage usage;
        public final String label;
        
        public MemorySnapshot(long timestamp, MemoryUsage usage, String label) {
            this.timestamp = timestamp;
            this.usage = usage;
            this.label = label;
        }
        
        public long getUsedMemory() {
            return usage.getUsed();
        }
        
        public long getCommittedMemory() {
            return usage.getCommitted();
        }
        
        public long getMaxMemory() {
            return usage.getMax();
        }
        
        @Override
        public String toString() {
            return String.format("MemorySnapshot[%dms, used=%d, label='%s']", 
                    timestamp, usage.getUsed(), label != null ? label : "");
        }
    }
    
    /**
     * Get detailed memory statistics for debugging.
     */
    public String getMemoryStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("Memory Monitor Statistics:\n");
        stats.append(String.format("  Monitoring: %s\n", monitoring));
        stats.append(String.format("  Snapshots: %d\n", snapshots.size()));
        stats.append(String.format("  Peak Usage: %d bytes\n", peakUsedMemory));
        
        if (startUsage != null) {
            stats.append(String.format("  Start Usage: %d bytes\n", startUsage.getUsed()));
        }
        
        MemoryUsage current = getCurrentUsage();
        stats.append(String.format("  Current Usage: %d bytes\n", current.getUsed()));
        stats.append(String.format("  Available Memory: %d bytes\n", current.getMax() - current.getUsed()));
        
        return stats.toString();
    }
}