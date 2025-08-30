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

import bluej.parser.pratt.integration.benchmark.metrics.MemoryMetrics;
import bluej.parser.pratt.integration.benchmark.metrics.MemoryProfiler;

/**
 * Memory profiler for strategy-specific memory monitoring.
 * Different strategies may need different profiling approaches.
 */
public class DefaultMemoryProfiler implements MemoryProfiler {
    private final Runtime runtime;
    private long baselineMemory;
    private long peakMemory;
    private boolean profiling;
    
    public DefaultMemoryProfiler() {
        this.runtime = Runtime.getRuntime();
    }
    
    @Override
    public void startProfiling() {
        System.gc(); // Try to get a clean baseline
        this.baselineMemory = getUsedMemory();
        this.peakMemory = baselineMemory;
        this.profiling = true;
    }
    
    public void recordMemoryUsage() {
        if (profiling) {
            long currentMemory = getUsedMemory();
            if (currentMemory > peakMemory) {
                peakMemory = currentMemory;
            }
        }
    }
    
    @Override
    public MemoryMetrics stopProfiling() {
        if (!profiling) {
            throw new IllegalStateException("Profiler not started");
        }
        
        profiling = false;
        long finalMemory = getUsedMemory();
        
        double peakUsageMB = (peakMemory - baselineMemory) / (1024.0 * 1024.0);
        double allocationRateMBPerSec = peakUsageMB / 1.0; // Simple approximation
        double gcPressureScore = 1.0; // Default low pressure
        double memoryRetentionMB = (finalMemory - baselineMemory) / (1024.0 * 1024.0);
        int gcCollections = 0; // Not measured in this simple profiler
        long gcTimeMs = 0; // Not measured
        
        return new MemoryMetrics(
            peakUsageMB,
            allocationRateMBPerSec,
            gcPressureScore,
            memoryRetentionMB,
            gcCollections,
            gcTimeMs
        );
    }
    
    @Override
    public boolean isActive() {
        return profiling;
    }
    
    private long getUsedMemory() {
        return runtime.totalMemory() - runtime.freeMemory();
    }
}