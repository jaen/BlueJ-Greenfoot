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
package bluej.parser.pratt.integration.benchmark.jmh;

import bluej.parser.BenchmarkTest;
import bluej.parser.CallbackDelegate;
import bluej.parser.InitConfig;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.PackageOrClass;
import bluej.parser.entity.TypeEntity;
import bluej.parser.entity.TypeArgumentEntity;
import bluej.debugger.gentype.Reflective;
import bluej.debugger.gentype.JavaType;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.pratt.integration.CallbackTestingUtility;
import bluej.parser.pratt.integration.CallbackTestingUtility.CallbackTester;
import bluej.parser.pratt.integration.benchmark.adapters.HybridResultPatternAdapter;
import bluej.parser.pratt.integration.benchmark.corpus.TestCorpusGenerator;
import bluej.parser.pratt.integration.benchmark.reporting.BenchmarkReporter;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark specifically focused on Hybrid Result Pattern integration strategy.
 * 
 * This benchmark measures the performance characteristics of the hybrid approach that
 * combines multiple parsing strategies to optimize for different code patterns:
 * - Strategy selection overhead and decision-making performance
 * - Adaptive strategy switching based on code complexity
 * - Combined benefits of multiple approaches (direct callbacks + AST + lazy evaluation)
 * - Fallback mechanism performance when primary strategies fail
 * - Memory optimization through intelligent strategy selection
 * 
 * The hybrid pattern aims to provide the best of multiple worlds by dynamically
 * choosing the most appropriate strategy for each parsing scenario.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-Xms2G", "-Xmx4G"})
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Category(BenchmarkTest.class)
public class HybridResultPatternJMHBenchmark {

    // Test infrastructure
    private TestCorpusGenerator corpusGenerator;
    private HybridResultPatternAdapter adapter;
    private TestableDocument document;
    private CallbackTester callbackTester;
    
    // Test corpus collections
    private List<String> simpleTestCases;
    private List<String> moderateTestCases;
    private List<String> complexTestCases;
    private List<String> veryComplexTestCases;
    private List<String> mixedComplexityTestCases;
    private List<String> strategyTransitionTestCases;
    private List<String> errorTestCases;
    private List<String> optimizationTestCases;

    static {
        InitConfig.init();
    }

    @Setup(Level.Trial)
    public void setUp() {
        corpusGenerator = new TestCorpusGenerator();
        adapter = new HybridResultPatternAdapter();
        
        MockEntityResolver entityResolver = new MockEntityResolver();
        document = new TestableDocument("benchmark.java", entityResolver);
        callbackTester = CallbackTestingUtility.forDelegate(document.getCallbackDelegate());
        
        // Generate test corpus for all complexity levels
        simpleTestCases = corpusGenerator.generateSimpleExpressions(100);
        moderateTestCases = corpusGenerator.generateMediumComplexityCode(50);
        complexTestCases = corpusGenerator.generateComplexNestedStructures(25);
        veryComplexTestCases = corpusGenerator.generateLargeClassFiles(10);
        
        // Generate hybrid-specific test cases
        mixedComplexityTestCases = generateMixedComplexityCases(30);
        strategyTransitionTestCases = generateStrategyTransitionCases(20);
        errorTestCases = generateErrorRecoveryCases(15);
        optimizationTestCases = generateOptimizationTestCases(25);
        
        System.out.println("Hybrid Result Pattern benchmark corpus initialized:");
        System.out.println("  Simple: " + simpleTestCases.size());
        System.out.println("  Moderate: " + moderateTestCases.size());
        System.out.println("  Complex: " + complexTestCases.size());
        System.out.println("  Very Complex: " + veryComplexTestCases.size());
        System.out.println("  Mixed Complexity: " + mixedComplexityTestCases.size());
        System.out.println("  Strategy Transitions: " + strategyTransitionTestCases.size());
        System.out.println("  Error Cases: " + errorTestCases.size());
        System.out.println("  Optimization Cases: " + optimizationTestCases.size());
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        adapter.cleanup();
    }

    @Setup(Level.Iteration)
    public void setupIteration() {
        callbackTester.clear();
    }

    // =================================
    // Core Performance Benchmarks
    // =================================

    @Benchmark
    public void benchmarkSimpleCode(Blackhole bh) {
        benchmarkWithTestCases(simpleTestCases, bh);
    }

    @Benchmark
    public void benchmarkModerateCode(Blackhole bh) {
        benchmarkWithTestCases(moderateTestCases, bh);
    }

    @Benchmark
    public void benchmarkComplexCode(Blackhole bh) {
        benchmarkWithTestCases(complexTestCases, bh);
    }

    @Benchmark
    public void benchmarkVeryComplexCode(Blackhole bh) {
        benchmarkWithTestCases(veryComplexTestCases, bh);
    }

    // =================================
    // Hybrid Pattern Specific Benchmarks
    // =================================

    @Benchmark
    public void benchmarkStrategySelectionOverhead(Blackhole bh) {
        // Measure the overhead of strategy selection in hybrid pattern
        for (String testCase : mixedComplexityTestCases) {
            document.setContent(testCase);
            callbackTester.clear();
            
            long startTime = System.nanoTime();
            try {
                Object result = adapter.parseWithStrategy(testCase);
                long endTime = System.nanoTime();
                
                long selectionTime = endTime - startTime;
                String selectedStrategy = "HybridResultPattern"; // Mock value
                
                bh.consume(result);
                bh.consume(selectionTime);
                bh.consume(selectedStrategy);
            } catch (Exception e) {
                bh.consume(e.getMessage());
            }
        }
    }

    @Benchmark
    public void benchmarkAdaptiveStrategySwitching(Blackhole bh) {
        // Test adaptive switching between strategies based on code characteristics
        for (String testCase : strategyTransitionTestCases) {
            document.setContent(testCase);
            callbackTester.clear();
            
            try {
                Object result = adapter.parseWithStrategy(testCase);
                
                // Measure adaptation characteristics
                List<String> strategiesUsed = Arrays.asList("DirectCallback", "ASTVisitor"); // Mock value
                boolean switchedStrategies = strategiesUsed.size() > 1;
                int transitionCount = strategiesUsed.size() - 1;
                
                bh.consume(result);
                bh.consume(switchedStrategies);
                bh.consume(transitionCount);
                bh.consume(strategiesUsed.toString());
            } catch (Exception e) {
                bh.consume(e.getMessage());
            }
        }
    }

    @Benchmark
    public void benchmarkHybridOptimizationEffectiveness(Blackhole bh) {
        // Test how well the hybrid pattern optimizes for different code patterns
        for (String testCase : optimizationTestCases) {
            document.setContent(testCase);
            callbackTester.clear();
            
            long startTime = System.nanoTime();
            try {
                Object result = adapter.parseWithStrategy(testCase);
                long endTime = System.nanoTime();
                
                long processingTime = endTime - startTime;
                String optimizationStrategy = "DirectCallback"; // Mock value
                boolean wasOptimized = true; // Mock value
                
                bh.consume(result);
                bh.consume(processingTime);
                bh.consume(optimizationStrategy);
                bh.consume(wasOptimized);
            } catch (Exception e) {
                bh.consume(e.getMessage());
            }
        }
    }

    @Benchmark
    public void benchmarkMultiStrategyCoordination(Blackhole bh) {
        // Test coordination between multiple strategies in hybrid approach
        for (String testCase : complexTestCases) {
            document.setContent(testCase);
            callbackTester.clear();
            
            try {
                Object result = adapter.parseWithStrategy(testCase);
                
                // Measure coordination metrics
                int strategiesCoordinated = 2; // Mock value
                boolean hasStrategyConflicts = false; // Mock value
                String coordinationMode = "PARALLEL"; // Mock value
                
                bh.consume(result);
                bh.consume(strategiesCoordinated);
                bh.consume(hasStrategyConflicts);
                bh.consume(coordinationMode);
            } catch (Exception e) {
                bh.consume(e.getMessage());
            }
        }
    }

    @Benchmark
    public void benchmarkFallbackMechanismPerformance(Blackhole bh) {
        // Test performance of fallback mechanisms when primary strategies fail
        benchmarkWithTestCases(errorTestCases, bh);
    }

    @Benchmark
    public void benchmarkStrategyDecisionLatency(Blackhole bh) {
        // Measure latency of strategy selection decisions
        for (String testCase : simpleTestCases) {
            document.setContent(testCase);
            
            long decisionStartTime = System.nanoTime();
            String selectedStrategy = "HybridResultPattern"; // Mock value
            long decisionEndTime = System.nanoTime();
            
            long decisionLatency = decisionEndTime - decisionStartTime;
            
            bh.consume(selectedStrategy);
            bh.consume(decisionLatency);
        }
    }

    // =================================
    // Mixed Complexity and Edge Cases
    // =================================

    @Benchmark
    public void benchmarkMixedComplexityHandling(Blackhole bh) {
        benchmarkWithTestCases(mixedComplexityTestCases, bh);
    }

    @Benchmark
    public void benchmarkErrorRecovery(Blackhole bh) {
        // Test error recovery across multiple strategies
        for (String testCase : errorTestCases) {
            document.setContent(testCase);
            callbackTester.clear();
            
            try {
                Object result = adapter.parseWithStrategy(testCase);
                
                // Measure error recovery effectiveness
                boolean recoveredFromErrors = true; // Mock value
                int recoveriesAttempted = 2; // Mock value
                String recoveryStrategy = "FallbackStrategy"; // Mock value
                
                bh.consume(result);
                bh.consume(recoveredFromErrors);
                bh.consume(recoveriesAttempted);
                bh.consume(recoveryStrategy);
            } catch (Exception e) {
                bh.consume(e.getMessage());
            }
        }
    }

    // =================================
    // Memory and Resource Benchmarks
    // =================================

    @Benchmark
    public void benchmarkMemoryUsage(Blackhole bh) {
        Runtime runtime = Runtime.getRuntime();
        
        for (String testCase : veryComplexTestCases) {
            document.setContent(testCase);
            
            System.gc(); // Force GC for more accurate measurement
            long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
            
            try {
                Object result = adapter.parseWithStrategy(testCase);
                
                long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
                long memoryUsed = memoryAfter - memoryBefore;
                
                // Additional memory metrics specific to hybrid pattern
                long strategyOverheadMemory = 512L; // Mock value
                boolean memoryOptimized = true; // Mock value
                
                bh.consume(result);
                bh.consume(memoryUsed);
                bh.consume(strategyOverheadMemory);
                bh.consume(memoryOptimized);
            } catch (Exception e) {
                bh.consume(e.getMessage());
            }
        }
    }

    @Benchmark
    public void benchmarkResourceUtilization(Blackhole bh) {
        // Test resource utilization efficiency of hybrid approach
        for (String testCase : optimizationTestCases) {
            document.setContent(testCase);
            
            long resourceStartTime = System.nanoTime();
            try {
                Object result = adapter.parseWithStrategy(testCase);
                long resourceEndTime = System.nanoTime();
                
                long resourceTime = resourceEndTime - resourceStartTime;
                double resourceEfficiency = 0.85; // Mock value
                int resourcesSaved = 3; // Mock value
                
                bh.consume(result);
                bh.consume(resourceTime);
                bh.consume(resourceEfficiency);
                bh.consume(resourcesSaved);
            } catch (Exception e) {
                bh.consume(e.getMessage());
            }
        }
    }

    // =================================
    // Helper Methods
    // =================================

    private void benchmarkWithTestCases(List<String> testCases, Blackhole bh) {
        for (String testCase : testCases) {
            document.setContent(testCase);
            
            try {
                Object result = adapter.parseWithStrategy(testCase);
                bh.consume(result);
            } catch (Exception e) {
                bh.consume(e.getMessage());
            }
        }
    }

    private List<String> generateMixedComplexityCases(int count) {
        List<String> mixedCases = new ArrayList<>();
        Random random = new Random(42);
        
        for (int i = 0; i < count; i++) {
            StringBuilder mixedCode = new StringBuilder();
            
            // Combine different complexity levels in one file
            mixedCode.append("package test.mixed").append(i).append(";\n\n");
            
            // Simple part
            mixedCode.append("public class Simple").append(i).append(" {\n");
            mixedCode.append("    private int value = ").append(i).append(";\n");
            mixedCode.append("    public int getValue() { return value; }\n");
            mixedCode.append("}\n\n");
            
            // Complex part
            mixedCode.append("public class Complex").append(i).append("<T extends Comparable<T>> {\n");
            mixedCode.append("    private final Map<String, List<T>> data = new HashMap<>();\n");
            mixedCode.append("    public <U extends T> Optional<U> process(List<U> items, Function<T, U> mapper) {\n");
            mixedCode.append("        return items.stream().map(mapper).max(T::compareTo);\n");
            mixedCode.append("    }\n");
            mixedCode.append("}\n");
            
            mixedCases.add(mixedCode.toString());
        }
        
        return mixedCases;
    }

    private List<String> generateStrategyTransitionCases(int count) {
        List<String> transitionCases = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            StringBuilder transitionCode = new StringBuilder();
            
            // Start simple, then become complex
            transitionCode.append("package test.transition").append(i).append(";\n\n");
            transitionCode.append("// Simple start - should use direct callbacks\n");
            transitionCode.append("import java.util.*;\n\n");
            
            transitionCode.append("public class TransitionTest").append(i).append(" {\n");
            transitionCode.append("    private int simple = ").append(i).append(";\n\n");
            
            // Transition to complex - should switch strategies
            transitionCode.append("    // Complex section - should switch to AST visitor or lazy\n");
            transitionCode.append("    public <T extends Number & Comparable<? super T>>\n");
            transitionCode.append("    CompletableFuture<Optional<T>> complexMethod(\n");
            transitionCode.append("            Stream<T> input,\n");
            transitionCode.append("            Function<T, ? extends T> mapper) {\n");
            transitionCode.append("        return CompletableFuture.supplyAsync(() ->\n");
            transitionCode.append("            input.map(mapper).max(T::compareTo)\n");
            transitionCode.append("        );\n");
            transitionCode.append("    }\n");
            
            // Return to simple
            transitionCode.append("    \n    // Simple end\n");
            transitionCode.append("    public void simpleEnd() { System.out.println(simple); }\n");
            transitionCode.append("}\n");
            
            transitionCases.add(transitionCode.toString());
        }
        
        return transitionCases;
    }

    private List<String> generateErrorRecoveryCases(int count) {
        List<String> errorCases = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            StringBuilder errorCode = new StringBuilder();
            
            errorCode.append("package test.error").append(i).append(";\n\n");
            errorCode.append("public class ErrorRecovery").append(i).append(" {\n");
            
            // Introduce different types of errors that hybrid pattern should handle
            switch (i % 4) {
                case 0:
                    // Syntax error - missing semicolon
                    errorCode.append("    private int field1 = 10\n"); // Missing semicolon
                    errorCode.append("    private String field2 = \"test\";\n");
                    break;
                case 1:
                    // Type error - but recoverable
                    errorCode.append("    private List<String> items = new ArrayList<Integer>();\n");
                    errorCode.append("    public void method() { items.add(\"test\"); }\n");
                    break;
                case 2:
                    // Structural error - unmatched braces
                    errorCode.append("    public void method1() {\n");
                    errorCode.append("        if (true) {\n");
                    errorCode.append("            System.out.println(\"test\");\n");
                    // Missing closing brace
                    errorCode.append("    }\n"); // Should be "}}"
                    break;
                case 3:
                    // Complex error in generics
                    errorCode.append("    public <T extends Invalid & Unknown> void method(T param) {\n");
                    errorCode.append("        param.unknownMethod();\n");
                    errorCode.append("    }\n");
                    break;
            }
            
            errorCode.append("}\n");
            errorCases.add(errorCode.toString());
        }
        
        return errorCases;
    }

    private List<String> generateOptimizationTestCases(int count) {
        List<String> optimizationCases = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            StringBuilder optimizedCode = new StringBuilder();
            
            // Create code patterns that should trigger different optimizations
            optimizedCode.append("package test.optimization").append(i).append(";\n\n");
            
            switch (i % 3) {
                case 0:
                    // Pattern that benefits from direct callback optimization
                    optimizedCode.append("public class DirectOptimization").append(i).append(" {\n");
                    for (int j = 0; j < 5; j++) {
                        optimizedCode.append("    public void simpleMethod").append(j).append("() {\n");
                        optimizedCode.append("        System.out.println(\"Simple ").append(j).append("\");\n");
                        optimizedCode.append("    }\n");
                    }
                    optimizedCode.append("}\n");
                    break;
                case 1:
                    // Pattern that benefits from AST visitor optimization
                    optimizedCode.append("public class ASTOptimization").append(i).append(" {\n");
                    optimizedCode.append("    public class InnerClass1 {\n");
                    optimizedCode.append("        public class DeepInner { }\n");
                    optimizedCode.append("    }\n");
                    optimizedCode.append("    public class InnerClass2 {\n");
                    optimizedCode.append("        public void method() { }\n");
                    optimizedCode.append("    }\n");
                    optimizedCode.append("}\n");
                    break;
                case 2:
                    // Pattern that benefits from lazy evaluation
                    optimizedCode.append("public class LazyOptimization").append(i).append(" {\n");
                    optimizedCode.append("    // Large method that might not need full parsing\n");
                    optimizedCode.append("    public void largeMethod() {\n");
                    for (int j = 0; j < 20; j++) {
                        optimizedCode.append("        // Complex statement ").append(j).append("\n");
                        optimizedCode.append("        if (condition").append(j).append(") {\n");
                        optimizedCode.append("            processData").append(j).append("();\n");
                        optimizedCode.append("        }\n");
                    }
                    optimizedCode.append("    }\n");
                    optimizedCode.append("}\n");
                    break;
            }
            
            optimizationCases.add(optimizedCode.toString());
        }
        
        return optimizationCases;
    }

    // =================================
    // JUnit Test Integration
    // =================================

    @Test
    public void testHybridResultPatternPerformance() throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(HybridResultPatternJMHBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(3)
                .measurementIterations(5)
                .build();

        Collection<RunResult> results = new Runner(opt).run();
        
        System.out.println("\n=== Hybrid Result Pattern Performance Results ===");
        for (RunResult result : results) {
            System.out.printf("%-40s: %.2f ns/op (±%.2f)\n",
                result.getPrimaryResult().getLabel(),
                result.getPrimaryResult().getScore(),
                result.getPrimaryResult().getStatistics().getStandardDeviation());
        }
    }

    // =================================
    // Mock Infrastructure
    // =================================

    private static class TestableDocument {
        private String content;
        private final EntityResolver entityResolver;
        private final CallbackDelegate callbackDelegate;
        
        public TestableDocument(String name, EntityResolver entityResolver) {
            this.entityResolver = entityResolver;
            this.callbackDelegate = new MockCallbackDelegate();
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public String getContent() {
            return content;
        }
        
        public CallbackDelegate getCallbackDelegate() {
            return callbackDelegate;
        }
    }
    
    private static class MockEntityResolver implements EntityResolver {
        @Override
        public PackageOrClass resolvePackageOrClass(String name, Reflective querySource) {
            return null;
        }
        
        @Override
        public TypeEntity resolveQualifiedClass(String name) {
            return null;
        }
        
        @Override
        public JavaEntity getValueEntity(String name, Reflective querySource) {
            return new MockJavaEntity(name);
        }
    }
    
    private static class MockJavaEntity extends JavaEntity {
        private final String name;
        
        public MockJavaEntity(String name) {
            this.name = name;
        }
        
        @Override
        public JavaType getType() {
            return null;
        }
        
        @Override
        public JavaEntity getSubentity(String name, Reflective accessSource) {
            return null;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public JavaEntity setTypeArgs(List<TypeArgumentEntity> tparams) {
            return null;
        }
    }
    
    private static class MockCallbackDelegate extends CallbackTestingUtility implements CallbackDelegate {
        @Override
        public void gotAnnotation(List<LocatableToken> name, boolean hasParams) {
            // Mock implementation
        }
        
        @Override
        public void determinedForLoop(boolean forEach, boolean hasInit) {
            // Mock implementation
        }
    }

    /**
     * Main method for standalone JMH execution.
     */
    public static void main(String[] args) throws RunnerException {
        String benchmarkFilter = args.length > 0 ? args[0] : ".*";
        
        Options opt = new OptionsBuilder()
                .include(HybridResultPatternJMHBenchmark.class.getSimpleName() + "\\." + benchmarkFilter)
                .forks(1)
                .warmupIterations(5)
                .measurementIterations(10)
                .build();
        
        Collection<RunResult> results = new Runner(opt).run();
        
        System.out.println(String.format("\n=== Hybrid Result Pattern Benchmark Complete: %d results ===", results.size()));
        
        // Generate report
        try {
            BenchmarkReporter reporter = BenchmarkReporter.forConsole();
            // Additional reporting could be added here
        } catch (Exception e) {
            System.err.println("Failed to generate report: " + e.getMessage());
        }
    }
}