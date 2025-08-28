# JMH Parser Performance Benchmarks

This directory contains JMH (Java Microbenchmark Harness) benchmarks for comparing the performance of the monolithic parser and the Pratt parser implementations.

## Overview

JMH is a Java harness for building, running, and analyzing nano/micro/milli/macro benchmarks. It provides:
- Proper JVM warmup
- Multiple measurement iterations
- Statistical analysis
- Protection against common benchmarking pitfalls

## Running the Benchmarks

### Method 1: As JUnit Tests

Each expression type has its own test method in `SimplePerformanceTest`. You can run:

**All performance tests:**
```bash
./gradlew :bluej:test --tests "bluej.parser.pratt.SimplePerformanceTest"
```

**Individual expression tests:**
```bash
# Test integer literal performance
./gradlew :bluej:test --tests "bluej.parser.pratt.SimplePerformanceTest.testPerformance_IntegerLiteral"

# Test arithmetic precedence performance
./gradlew :bluej:test --tests "bluej.parser.pratt.SimplePerformanceTest.testPerformance_ArithmeticPrecedence"

# Test nested parentheses performance
./gradlew :bluej:test --tests "bluej.parser.pratt.SimplePerformanceTest.testPerformance_NestedParentheses"
```

These tests:
- Run focused benchmarks for specific expression types
- Verify that Pratt parser performance is within 10% of monolithic parser
- Provide clear failure messages indicating which expression type failed

### Method 2: Using the Benchmark Runner

Run the standalone `BenchmarkRunner` with different profiles:

```bash
# Default benchmark (balanced)
./gradlew :bluej:test --tests "bluej.parser.pratt.BenchmarkRunner"

# Quick benchmark (for rapid feedback)
./gradlew :bluej:test --tests "bluej.parser.pratt.BenchmarkRunner" --args="quick"

# Full benchmark (comprehensive analysis)
./gradlew :bluej:test --tests "bluej.parser.pratt.BenchmarkRunner" --args="full"

# Profiling mode (with GC and stack profiling)
./gradlew :bluej:test --tests "bluej.parser.pratt.BenchmarkRunner" --args="profile"
```

### Method 3: Direct JMH Execution

Run specific benchmarks using the main method:

```java
// In SimplePerformanceTest.main()
java -cp <classpath> bluej.parser.pratt.SimplePerformanceTest
```

## Benchmark Structure

Each expression type has its own benchmark method:
- `benchmarkIntegerLiteral()` - Simple integer parsing
- `benchmarkSimpleVariable()` - Variable name parsing
- `benchmarkArithmeticPrecedence()` - Complex arithmetic expressions
- `benchmarkNestedParentheses()` - Deeply nested expressions
- etc.

The `@Param` annotation switches between parsers:
- `usePrattParser=false` - Uses monolithic parser
- `usePrattParser=true` - Uses Pratt parser

## Understanding Results

JMH outputs results in nanoseconds per operation (ns/op). Lower values are better.

Example output:
```
benchmarkIntegerLiteral              Monolithic:    1234.56 ns/op
benchmarkIntegerLiteral              Pratt:         1358.02 ns/op
```

The performance ratio is calculated as: `Pratt time / Monolithic time`
- Ratio < 1.0: Pratt is faster
- Ratio = 1.0: Equal performance  
- Ratio > 1.0: Pratt is slower
- Target: Ratio ≤ 1.10 (Pratt within 10% of monolithic)

## Adding New Benchmarks

To add a new expression type benchmark:

1. Add a new `@Benchmark` method:
```java
@Benchmark
public void benchmarkNewExpressionType() {
    parseExpression("your expression here");
}
```

2. Add a corresponding test method:
```java
@Test
public void testPerformance_NewExpressionType() throws RunnerException {
    runSingleBenchmarkTest("benchmarkNewExpressionType", "New expression type");
}
```

3. Both the benchmark and test will be automatically included in their respective runs.

## JMH Annotations

- `@BenchmarkMode(Mode.AverageTime)` - Measures average time per operation
- `@OutputTimeUnit(TimeUnit.NANOSECONDS)` - Reports in nanoseconds
- `@State(Scope.Benchmark)` - Benchmark state shared across threads
- `@Fork(2)` - Run benchmarks in 2 separate JVM processes
- `@Warmup(iterations = 5, time = 1)` - 5 warmup iterations, 1 second each
- `@Measurement(iterations = 10, time = 1)` - 10 measurement iterations, 1 second each

## Best Practices

1. **Isolation**: Each benchmark measures a single expression type
2. **Warmup**: Proper JVM warmup ensures stable measurements
3. **Multiple Forks**: Reduces noise from JVM variations
4. **Statistical Significance**: Multiple iterations provide reliable averages
5. **Granular Testing**: Individual test methods allow testing specific expression types

## Troubleshooting

- **High variance**: Increase iterations or fork count
- **Unstable results**: Check for background processes, use isolated machine
- **Parser differences**: Verify both parsers produce equivalent ASTs

## Performance Goals

The Pratt parser implementation should:
- Be within 10% of monolithic parser performance for all expression types
- Maintain consistent performance across different expression complexities
- Show predictable performance characteristics (no outliers)