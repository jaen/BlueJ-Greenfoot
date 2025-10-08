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
package bluej.parser;

import bluej.extensions2.SourceType;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Performance benchmark for TrackingSourceParser.
 * 
 * <p>This benchmark validates that the tracking overhead when disabled is < 1%,
 * which is critical for production use where tracking is typically disabled.
 * 
 * <p>Benchmark methodology:
 * <ul>
 *   <li>Warm-up phase to ensure JIT compilation</li>
 *   <li>Multiple iterations with statistical analysis</li>
 *   <li>Comparison against baseline SourceParser</li>
 *   <li>Testing with various code complexities</li>
 * </ul>
 * 
 * <p>Run this benchmark with: {@code java -Xmx2g -XX:+UseG1GC bluej.parser.TrackingSourceParserBenchmark}
 * 
 * @since BlueJ 5.4.0
 * @author BlueJ Development Team
 */
public class TrackingSourceParserBenchmark {
    
    // Test code samples of varying complexity
    private static final String MINIMAL_CODE = """
        class A { }
        """;
    
    private static final String SIMPLE_CODE = """
        package test;
        
        public class SimpleClass {
            private int field;
            
            public int getField() {
                return field;
            }
            
            public void setField(int field) {
                this.field = field;
            }
        }
        """;
    
    private static final String MEDIUM_CODE = """
        package test.medium;
        
        import java.util.*;
        
        public class MediumClass {
            private List<String> items = new ArrayList<>();
            private Map<String, Integer> cache = new HashMap<>();
            
            public void processItems() {
                for (String item : items) {
                    if (!cache.containsKey(item)) {
                        cache.put(item, computeValue(item));
                    }
                }
            }
            
            private int computeValue(String item) {
                return item.hashCode() % 100;
            }
            
            public void addItem(String item) {
                if (item != null && !item.isEmpty()) {
                    items.add(item);
                }
            }
        }
        """;
    
    private static final String COMPLEX_CODE = """
        package test.complex;
        
        import java.util.*;
        import java.util.concurrent.*;
        import java.util.stream.*;
        import java.util.function.*;
        
        public class ComplexClass<T extends Comparable<T>> {
            private final List<T> items = new CopyOnWriteArrayList<>();
            private final ExecutorService executor = Executors.newCachedThreadPool();
            
            public CompletableFuture<List<T>> processAsync() {
                return CompletableFuture.supplyAsync(() -> {
                    return items.stream()
                        .filter(Objects::nonNull)
                        .sorted()
                        .distinct()
                        .collect(Collectors.toList());
                }, executor);
            }
            
            public void processWithLambda(Consumer<T> processor) {
                items.parallelStream()
                    .filter(item -> item != null)
                    .forEach(item -> {
                        try {
                            processor.accept(item);
                        } catch (Exception e) {
                            System.err.println("Error: " + e);
                        }
                    });
            }
            
            private class InnerProcessor implements Runnable {
                private final T item;
                
                InnerProcessor(T item) {
                    this.item = item;
                }
                
                @Override
                public void run() {
                    process(item);
                }
                
                private void process(T item) {
                    System.out.println(item);
                }
            }
            
            record Result<R>(R value, long timestamp) {
                public Result {
                    if (value == null) {
                        throw new IllegalArgumentException();
                    }
                }
            }
        }
        """;
    
    // Benchmark configuration
    private static final int WARMUP_ITERATIONS = 1000;
    private static final int BENCHMARK_ITERATIONS = 5000;
    private static final double ACCEPTABLE_OVERHEAD_PERCENT = 1.0;
    
    public static void main(String[] args) {
        System.out.println("=== TrackingSourceParser Performance Benchmark ===\n");
        
        // Run benchmarks for different code complexities
        runBenchmark("Minimal Code", MINIMAL_CODE);
        runBenchmark("Simple Code", SIMPLE_CODE);
        runBenchmark("Medium Code", MEDIUM_CODE);
        runBenchmark("Complex Code", COMPLEX_CODE);
        
        System.out.println("\n=== Summary ===");
        System.out.println("Benchmark completed. Check results above for overhead percentages.");
        System.out.println("Requirement: Overhead when disabled should be < " + 
                         ACCEPTABLE_OVERHEAD_PERCENT + "%");
    }
    
    private static void runBenchmark(String name, String code) {
        System.out.println("\n--- Benchmarking: " + name + " ---");
        System.out.println("Code size: " + code.length() + " characters");
        
        // Warm-up phase
        System.out.print("Warming up...");
        warmUp(code);
        System.out.println(" done");
        
        // Run baseline benchmark
        System.out.print("Running baseline (SourceParser)...");
        BenchmarkResult baseline = benchmarkSourceParser(code);
        System.out.println(" done");
        
        // Run tracking disabled benchmark
        System.out.print("Running TrackingSourceParser (disabled)...");
        BenchmarkResult trackingDisabled = benchmarkTrackingParserDisabled(code);
        System.out.println(" done");
        
        // Run tracking enabled benchmark for comparison
        System.out.print("Running TrackingSourceParser (enabled)...");
        BenchmarkResult trackingEnabled = benchmarkTrackingParserEnabled(code);
        System.out.println(" done");
        
        // Calculate and display results
        displayResults(baseline, trackingDisabled, trackingEnabled);
    }
    
    private static void warmUp(String code) {
        // Warm up all parser types
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            // Baseline
            new SourceParser(new StringReader(code)).parseCU();
            
            // Tracking disabled
            TrackingSourceParser parser1 = new TrackingSourceParser(new StringReader(code));
            parser1.setTrackingEnabled(false);
            parser1.parseCU();
            
            // Tracking enabled
            TrackingSourceParser parser2 = new TrackingSourceParser(new StringReader(code));
            parser2.setTrackingEnabled(true);
            parser2.parseCU();
        }
        
        // Force GC after warmup
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private static BenchmarkResult benchmarkSourceParser(String code) {
        List<Long> durations = new ArrayList<>(BENCHMARK_ITERATIONS);
        
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long start = System.nanoTime();
            new SourceParser(new StringReader(code)).parseCU();
            long duration = System.nanoTime() - start;
            durations.add(duration);
        }
        
        return new BenchmarkResult(durations);
    }
    
    private static BenchmarkResult benchmarkTrackingParserDisabled(String code) {
        List<Long> durations = new ArrayList<>(BENCHMARK_ITERATIONS);
        
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long start = System.nanoTime();
            TrackingSourceParser parser = new TrackingSourceParser(new StringReader(code));
            parser.setTrackingEnabled(false);
            parser.parseCU();
            long duration = System.nanoTime() - start;
            durations.add(duration);
        }
        
        return new BenchmarkResult(durations);
    }
    
    private static BenchmarkResult benchmarkTrackingParserEnabled(String code) {
        List<Long> durations = new ArrayList<>(BENCHMARK_ITERATIONS);
        
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long start = System.nanoTime();
            TrackingSourceParser parser = new TrackingSourceParser(new StringReader(code));
            parser.setTrackingEnabled(true);
            parser.parseCU();
            long duration = System.nanoTime() - start;
            durations.add(duration);
        }
        
        return new BenchmarkResult(durations);
    }
    
    private static void displayResults(BenchmarkResult baseline, 
                                      BenchmarkResult trackingDisabled,
                                      BenchmarkResult trackingEnabled) {
        System.out.println("\nResults:");
        System.out.println("  Baseline (SourceParser):");
        System.out.println("    Mean: " + formatNanos(baseline.mean));
        System.out.println("    Median: " + formatNanos(baseline.median));
        System.out.println("    Std Dev: " + formatNanos(baseline.stdDev));
        System.out.println("    p95: " + formatNanos(baseline.p95));
        System.out.println("    p99: " + formatNanos(baseline.p99));
        
        System.out.println("  TrackingSourceParser (disabled):");
        System.out.println("    Mean: " + formatNanos(trackingDisabled.mean));
        System.out.println("    Median: " + formatNanos(trackingDisabled.median));
        System.out.println("    Std Dev: " + formatNanos(trackingDisabled.stdDev));
        System.out.println("    p95: " + formatNanos(trackingDisabled.p95));
        System.out.println("    p99: " + formatNanos(trackingDisabled.p99));
        
        System.out.println("  TrackingSourceParser (enabled):");
        System.out.println("    Mean: " + formatNanos(trackingEnabled.mean));
        System.out.println("    Median: " + formatNanos(trackingEnabled.median));
        System.out.println("    Std Dev: " + formatNanos(trackingEnabled.stdDev));
        System.out.println("    p95: " + formatNanos(trackingEnabled.p95));
        System.out.println("    p99: " + formatNanos(trackingEnabled.p99));
        
        // Calculate overheads
        double overheadDisabled = ((trackingDisabled.mean - baseline.mean) / baseline.mean) * 100;
        double overheadEnabled = ((trackingEnabled.mean - baseline.mean) / baseline.mean) * 100;
        
        System.out.println("\nOverhead Analysis:");
        System.out.printf("  Disabled: %.3f%% %s%n", 
                         overheadDisabled,
                         overheadDisabled < ACCEPTABLE_OVERHEAD_PERCENT ? "✓ PASS" : "✗ FAIL");
        System.out.printf("  Enabled: %.3f%%%n", overheadEnabled);
        
        // Additional analysis
        double speedupFactor = trackingEnabled.mean / trackingDisabled.mean;
        System.out.printf("  Tracking cost factor: %.2fx slower when enabled%n", speedupFactor);
    }
    
    private static String formatNanos(double nanos) {
        if (nanos < 1000) {
            return String.format("%.0f ns", nanos);
        } else if (nanos < 1_000_000) {
            return String.format("%.2f μs", nanos / 1000);
        } else {
            return String.format("%.2f ms", nanos / 1_000_000);
        }
    }
    
    /**
     * Holds benchmark results with statistical analysis.
     */
    private static class BenchmarkResult {
        final double mean;
        final double median;
        final double stdDev;
        final double p95;
        final double p99;
        
        BenchmarkResult(List<Long> durations) {
            // Sort for percentiles
            List<Long> sorted = new ArrayList<>(durations);
            sorted.sort(Long::compare);
            
            // Calculate mean
            this.mean = durations.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);
            
            // Calculate median
            int size = sorted.size();
            if (size % 2 == 0) {
                this.median = (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
            } else {
                this.median = sorted.get(size / 2);
            }
            
            // Calculate standard deviation
            double variance = durations.stream()
                .mapToDouble(d -> Math.pow(d - mean, 2))
                .average()
                .orElse(0);
            this.stdDev = Math.sqrt(variance);
            
            // Calculate percentiles
            this.p95 = sorted.get((int) (size * 0.95));
            this.p99 = sorted.get((int) (size * 0.99));
        }
    }
}