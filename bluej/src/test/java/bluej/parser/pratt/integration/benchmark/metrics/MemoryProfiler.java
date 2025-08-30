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
 * Interface for memory profiling during parser integration strategy benchmarking.
 * 
 * Provides a standardized way to measure memory usage patterns across different
 * parser strategies, allowing for consistent comparison of memory consumption
 * characteristics.
 */
public interface MemoryProfiler {
    
    /**
     * Start memory profiling for the current benchmarking session.
     * This should capture initial memory state and begin tracking allocations.
     */
    void startProfiling();
    
    /**
     * Stop memory profiling and return collected metrics.
     * 
     * @return MemoryMetrics containing the profiling results
     */
    MemoryMetrics stopProfiling();
    
    /**
     * Get current memory snapshot without stopping profiling.
     * Useful for intermediate memory usage checks during parsing.
     * 
     * @return Current memory metrics snapshot
     */
    default MemoryMetrics getCurrentSnapshot() {
        // Default implementation returns basic metrics
        return new MemoryMetrics.Builder()
                .peakUsageMB(Runtime.getRuntime().totalMemory() / (1024 * 1024))
                .allocationRateMBPerSec(0.0)
                .gcPressureScore(0.0)
                .memoryRetentionMB(0.0)
                .gcCollections(0)
                .gcTimeMs(0)
                .build();
    }
    
    /**
     * Reset profiler state for a new benchmarking session.
     */
    default void reset() {
        // Default: no action needed
    }
    
    /**
     * Check if profiler is currently active.
     */
    default boolean isActive() {
        return false;
    }
}