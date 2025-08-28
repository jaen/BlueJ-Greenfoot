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
package bluej.parser.pratt;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

/**
 * Standalone JMH benchmark runner for parser performance tests.
 *
 * <p>This class provides various configurations for running the benchmarks
 * with different settings. Use the main method with arguments to select
 * different benchmark profiles.</p>
 *
 * @author BlueJ Team
 */
public class BenchmarkRunner {

    public static void main(String[] args) throws RunnerException {
        String mode = args.length > 0 ? args[0] : "default";

        Options opt = switch (mode) {
            case "quick" -> quickBenchmark();
            case "full" -> fullBenchmark();
            case "profile" -> profilingBenchmark();
            default -> defaultBenchmark();
        };

        new Runner(opt).run();
    }

    /**
     * Default benchmark configuration - balanced between accuracy and speed.
     */
    private static Options defaultBenchmark() {
        return new OptionsBuilder()
                .include(SimplePerformanceTest.class.getSimpleName())
                .forks(2)
                .warmupIterations(5)
                .warmupTime(TimeValue.seconds(1))
                .measurementIterations(10)
                .measurementTime(TimeValue.seconds(1))
                .build();
    }

    /**
     * Quick benchmark for rapid feedback during development.
     */
    private static Options quickBenchmark() {
        return new OptionsBuilder()
                .include(SimplePerformanceTest.class.getSimpleName())
                .forks(1)
                .warmupIterations(2)
                .warmupTime(TimeValue.milliseconds(500))
                .measurementIterations(3)
                .measurementTime(TimeValue.milliseconds(500))
                .build();
    }

    /**
     * Full benchmark for comprehensive performance analysis.
     */
    private static Options fullBenchmark() {
        return new OptionsBuilder()
                .include(SimplePerformanceTest.class.getSimpleName())
                .forks(3)
                .warmupIterations(10)
                .warmupTime(TimeValue.seconds(2))
                .measurementIterations(20)
                .measurementTime(TimeValue.seconds(2))
                .resultFormat(ResultFormatType.JSON)
                .result("benchmark-results.json")
                .build();
    }

    /**
     * Benchmark configuration suitable for profiling.
     */
    private static Options profilingBenchmark() {
        return new OptionsBuilder()
                .include(SimplePerformanceTest.class.getSimpleName())
                .forks(0) // No forking for profiler attachment
                .warmupIterations(5)
                .measurementIterations(10)
                .addProfiler("gc")
                .addProfiler("stack")
                .build();
    }
}
