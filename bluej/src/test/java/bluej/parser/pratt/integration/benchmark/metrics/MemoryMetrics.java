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
package bluej.parser.pratt.integration.benchmark.metrics;

/**
 * Memory usage metrics for parser integration strategy benchmarking.
 * 
 * Captures various aspects of memory consumption during parsing:
 * - Peak memory usage during parsing operations
 * - Rate of memory allocation 
 * - Garbage collection pressure
 * - Memory retained after parsing completes
 * 
 * These metrics help distinguish between strategies like:
 * - Direct Callback (minimal memory usage)
 * - AST Visitor (high peak usage from complete AST)
 * - Lazy AST (variable usage based on access patterns)
 * - K2 Parser (high baseline due to compiler infrastructure)
 */
public class MemoryMetrics {
    private final double peakUsageMB;
    private final double allocationRateMBPerSec;
    private final double gcPressureScore;
    private final double memoryRetentionMB;
    private final int gcCollections;
    private final long gcTimeMs;
    
    public MemoryMetrics(double peakUsageMB, 
                        double allocationRateMBPerSec, 
                        double gcPressureScore,
                        double memoryRetentionMB,
                        int gcCollections,
                        long gcTimeMs) {
        this.peakUsageMB = peakUsageMB;
        this.allocationRateMBPerSec = allocationRateMBPerSec;
        this.gcPressureScore = gcPressureScore;
        this.memoryRetentionMB = memoryRetentionMB;
        this.gcCollections = gcCollections;
        this.gcTimeMs = gcTimeMs;
    }
    
    /**
     * Peak memory usage during parsing operation in megabytes.
     * Higher values indicate strategies that build large intermediate structures.
     */
    public double getPeakUsageMB() {
        return peakUsageMB;
    }
    
    /**
     * Rate of memory allocation during parsing in MB per second.
     * High allocation rates can cause GC pressure even with reasonable peak usage.
     */
    public double getAllocationRateMBPerSec() {
        return allocationRateMBPerSec;
    }
    
    /**
     * GC pressure score (0.0 to 10.0, where higher is worse).
     * Combines frequency and duration of garbage collections.
     */
    public double getGcPressureScore() {
        return gcPressureScore;
    }
    
    /**
     * Memory retained after parsing completes in megabytes.
     * Indicates whether the strategy keeps references to parsed structures.
     */
    public double getMemoryRetentionMB() {
        return memoryRetentionMB;
    }
    
    /**
     * Number of garbage collection cycles during parsing.
     */
    public int getGcCollections() {
        return gcCollections;
    }
    
    /**
     * Total time spent in garbage collection during parsing in milliseconds.
     */
    public long getGcTimeMs() {
        return gcTimeMs;
    }
    
    // ==================== Compatibility Aliases ====================
    
    /**
     * Alias for getPeakUsageMB() for backward compatibility.
     */
    public double getPeakUsedMemory() {
        return peakUsageMB;
    }
    
    /**
     * Alias for getAllocationRateMBPerSec() for backward compatibility.
     */
    public double getAllocationRate() {
        return allocationRateMBPerSec;
    }
    
    /**
     * Alias for getGcPressureScore() for backward compatibility.
     */
    public double getGcPressure() {
        return gcPressureScore;
    }
    
    /**
     * Alias for getMemoryRetentionMB() for backward compatibility.
     */
    public double getRetentionRate() {
        return memoryRetentionMB;
    }
    
    /**
     * Calculate memory efficiency score (higher is better).
     * Balances peak usage, allocation rate, and GC impact.
     */
    public double getEfficiencyScore() {
        double peakPenalty = Math.log(peakUsageMB + 1) * 10;
        double allocationPenalty = Math.log(allocationRateMBPerSec + 1) * 5;
        double gcPenalty = gcPressureScore * 2;
        
        return Math.max(0, 100 - peakPenalty - allocationPenalty - gcPenalty);
    }
    
    /**
     * Check if memory usage is considered efficient.
     * Useful for categorizing strategies.
     */
    public boolean isMemoryEfficient() {
        return peakUsageMB < 50 && gcPressureScore < 3.0 && allocationRateMBPerSec < 100;
    }
    
    @Override
    public String toString() {
        return String.format("MemoryMetrics{peak=%.1fMB, allocation=%.1fMB/s, gc=%.1f, retention=%.1fMB}",
                peakUsageMB, allocationRateMBPerSec, gcPressureScore, memoryRetentionMB);
    }
    
    /**
     * Builder for MemoryMetrics.
     */
    public static class Builder {
        private double peakUsageMB = 0.0;
        private double allocationRateMBPerSec = 0.0;
        private double gcPressureScore = 0.0;
        private double memoryRetentionMB = 0.0;
        private int gcCollections = 0;
        private long gcTimeMs = 0;
        
        public Builder peakUsageMB(double peakUsageMB) {
            this.peakUsageMB = peakUsageMB;
            return this;
        }
        
        // Compatibility alias
        public Builder peakUsedMemory(double peakUsedMemory) {
            this.peakUsageMB = peakUsedMemory;
            return this;
        }
        
        // Compatibility alias
        public Builder allocationRate(double allocationRate) {
            this.allocationRateMBPerSec = allocationRate;
            return this;
        }
        
        // Compatibility alias
        public Builder gcPressure(double gcPressure) {
            this.gcPressureScore = gcPressure;
            return this;
        }
        
        // Compatibility alias
        public Builder retentionRate(double retentionRate) {
            this.memoryRetentionMB = retentionRate;
            return this;
        }
        
        public Builder allocationRateMBPerSec(double allocationRateMBPerSec) {
            this.allocationRateMBPerSec = allocationRateMBPerSec;
            return this;
        }
        
        public Builder gcPressureScore(double gcPressureScore) {
            this.gcPressureScore = gcPressureScore;
            return this;
        }
        
        public Builder memoryRetentionMB(double memoryRetentionMB) {
            this.memoryRetentionMB = memoryRetentionMB;
            return this;
        }
        
        public Builder gcCollections(int gcCollections) {
            this.gcCollections = gcCollections;
            return this;
        }
        
        public Builder gcTimeMs(long gcTimeMs) {
            this.gcTimeMs = gcTimeMs;
            return this;
        }
        
        public MemoryMetrics build() {
            return new MemoryMetrics(peakUsageMB, allocationRateMBPerSec, gcPressureScore,
                                   memoryRetentionMB, gcCollections, gcTimeMs);
        }
    }
}