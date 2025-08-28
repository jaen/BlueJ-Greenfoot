# Performance Testing Setup

## Overview

This document describes the performance testing infrastructure for the BlueJ parser, specifically for benchmarking the Pratt parser implementation against the monolithic parser.

## Background

The BlueJ project includes performance-critical parser code that has been refactored from a monolithic implementation to a modular Pratt parser. To ensure that the new implementation maintains acceptable performance characteristics, we have comprehensive benchmark tests that measure and compare parsing performance.

## Test Organization

### Consolidated Performance Tests

Previously, there were two similar performance test files:
- `ParserPerformanceBenchmark.java` (removed)
- `SimplePerformanceTest.java` (retained and enhanced)

These have been consolidated into a single `SimplePerformanceTest.java` file that provides both JMH benchmarking capabilities and JUnit test assertions for performance targets.

### Test Categories

Performance tests are marked with the `@Category(BenchmarkTest.class)` annotation to distinguish them from regular unit tests. This allows for:

- **Exclusion from regular test runs**: Benchmark tests take significantly longer to execute and are not needed for basic functionality validation
- **Dedicated execution**: Benchmark tests can be run separately when performance analysis is needed

## Running Performance Tests

### Regular Tests (Excludes Benchmarks)

```bash
# This will run all tests EXCEPT benchmark tests
./gradlew :bluej:test
```

### Benchmark Tests Only

```bash
# This will run ONLY the benchmark tests
./gradlew :bluej:benchmarkTest
```

### Specific Benchmark Test

```bash
# Run a specific benchmark test method
./gradlew :bluej:benchmarkTest --tests "*SimplePerformanceTest*testPerformance_IntegerLiteral*"
```

## Performance Test Coverage

The `SimplePerformanceTest` class includes comprehensive benchmarks covering:

### Simple Expressions
- Integer literals (`42`)
- Variable identifiers (`myVariable`)
- Boolean literals (`true`)
- String literals (`"hello world"`)

### Binary Operations
- Simple arithmetic (`a + b`, `x * y`)
- Comparison operators (`a == b`)
- Logical operators (`flag && other`)

### Complex Expressions
- Arithmetic precedence (`a + b * c - d / e`)
- Parenthesized expressions (`(a + b) * (c - d)`)
- Nested parentheses (`((a + b) * (c - d)) / ((e + f) * (g - h))`)
- Mixed operations with various operator types

### Advanced Cases
- Unary operations (`-a`, `!flag`, `++counter`, `value--`)
- Complex logical chains (`a > b && c < d || e == f && g != h`)
- Large nested expressions (stress testing)

## Performance Targets

The benchmark tests enforce a **performance target of 10%**: the Pratt parser should perform within 10% of the monolithic parser's performance for all test cases.

Each test method:
1. Runs JMH benchmarks for both parser implementations
2. Compares the results and calculates the performance ratio
3. **Fails the test** if the Pratt parser is more than 10% slower than the monolithic parser
4. Provides detailed performance metrics in the test output

## Test Configuration

### JMH Settings
- **Warmup**: 3-5 iterations, 1 second each
- **Measurement**: 5-10 iterations, 1 second each
- **Forks**: 1-2 (configurable)
- **JVM Args**: `-Xms2G -Xmx2G` for consistent memory allocation

### Gradle Configuration
- **Parallelization**: Benchmark tests run sequentially (`maxParallelForks = 1`) for more reliable results
- **Logging**: Enhanced logging shows benchmark progress and results
- **Categories**: Uses JUnit 4 categories for test filtering

## Example Output

When a benchmark test runs, you'll see output like:

```
=== Integer literal Performance ===
Monolithic: 15553.91 ns/op
Pratt:      15373.21 ns/op
Ratio:      0.99 (Pratt/Monolithic)
Target:     ≤ 1.1
Status:     ✅ PASS
```

## Troubleshooting

### Tests Take Too Long
Benchmark tests are designed to be thorough and will take several minutes to complete. This is normal and expected for performance testing.

### Performance Regressions
If a benchmark test fails:
1. Check if there were recent changes to the Pratt parser implementation
2. Run the test multiple times to ensure consistency
3. Consider system load and other factors that might affect performance
4. Investigate specific expressions that are underperforming

### Environment Sensitivity
Benchmark results can be sensitive to:
- System load
- JVM version
- Hardware differences
- Background processes

For reliable results, run benchmarks on a quiet system with consistent conditions.

## Adding New Benchmarks

To add new performance benchmarks:

1. Add a new `@Benchmark` method to `SimplePerformanceTest`
2. Add a corresponding `@Test` method that calls `runSingleBenchmarkTest()`
3. Follow the existing naming conventions
4. Ensure the `@Category(BenchmarkTest.class)` annotation is present on the class

## Dependencies

The performance testing setup requires:
- `org.openjdk.jmh:jmh-core:1.37`
- `org.openjdk.jmh:jmh-generator-annprocess:1.37`
- `junit:junit:4.13.2` (for categories support)

These are already configured in the project's `build.gradle`.