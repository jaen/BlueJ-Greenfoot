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
import bluej.parser.pratt.integration.benchmark.adapters.ASTVisitorPatternAdapter;
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
 * JMH benchmark specifically focused on AST Visitor Pattern integration strategy.
 * 
 * This benchmark measures the performance characteristics unique to the AST visitor pattern:
 * - Tree traversal performance and overhead
 * - Callback emission timing during AST visits
 * - Memory allocation patterns during visitor traversal
 * - Error recovery performance with AST visitor pattern
 * - Visitor pattern scalability across different code complexities
 * 
 * The AST visitor pattern approach builds a complete AST first, then traverses it
 * using the visitor pattern to emit callbacks, making it predictable but potentially
 * memory-intensive for large code structures.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-Xms2G", "-Xmx4G"})
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Category(BenchmarkTest.class)
public class ASTVisitorPatternJMHBenchmark {

    // Test infrastructure
    private TestCorpusGenerator corpusGenerator;
    private ASTVisitorPatternAdapter adapter;
    private TestableDocument document;
    private CallbackTester callbackTester;
    
    // Test corpus collections
    private List<String> simpleTestCases;
    private List<String> moderateTestCases;
    private List<String> complexTestCases;
    private List<String> veryComplexTestCases;
    private List<String> errorTestCases;
    private List<String> largeFileTestCases;

    static {
        InitConfig.init();
    }

    @Setup(Level.Trial)
    public void setUp() {
        corpusGenerator = new TestCorpusGenerator();
        adapter = new ASTVisitorPatternAdapter();
        
        MockEntityResolver entityResolver = new MockEntityResolver();
        document = new TestableDocument("benchmark.java", entityResolver);
        callbackTester = CallbackTestingUtility.forDelegate(document.getCallbackDelegate());
        
        // Generate test corpus for all complexity levels
        simpleTestCases = corpusGenerator.generateSimpleExpressions(100);
        moderateTestCases = corpusGenerator.generateMediumComplexityCode(50);
        complexTestCases = corpusGenerator.generateComplexNestedStructures(25);
        veryComplexTestCases = corpusGenerator.generateLargeClassFiles(10);
        
        // Generate error cases for error recovery testing
        errorTestCases = generateErrorCases(20);
        largeFileTestCases = generateLargeFileCases(5);
        
        System.out.println("AST Visitor Pattern benchmark corpus initialized:");
        System.out.println("  Simple: " + simpleTestCases.size());
        System.out.println("  Moderate: " + moderateTestCases.size());
        System.out.println("  Complex: " + complexTestCases.size());
        System.out.println("  Very Complex: " + veryComplexTestCases.size());
        System.out.println("  Error Cases: " + errorTestCases.size());
        System.out.println("  Large Files: " + largeFileTestCases.size());
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
    // AST Visitor Pattern Specific Benchmarks
    // =================================

    @Benchmark
    public void benchmarkASTConstructionTime(Blackhole bh) {
        // Measure AST building phase specifically for visitor pattern
        for (String testCase : complexTestCases) {
            document.setContent(testCase);
            
            long startTime = System.nanoTime();
            try {
                Object result = adapter.parseWithStrategy(testCase);
                long endTime = System.nanoTime();
                
                bh.consume(result);
                bh.consume(endTime - startTime); // Consume timing
            } catch (Exception e) {
                bh.consume(e.getMessage());
            }
        }
    }

    @Benchmark
    public void benchmarkVisitorTraversalOverhead(Blackhole bh) {
        // Focus on visitor traversal performance
        for (String testCase : moderateTestCases) {
            document.setContent(testCase);
            callbackTester.clear();
            
            try {
                Object result = adapter.parseWithStrategy(testCase);
                
                // Measure visitor-specific metrics
                int callbackCount = callbackTester.getCallbackCount();
                boolean isBalanced = callbackTester.isBalanced();
                
                bh.consume(result);
                bh.consume(callbackCount);
                bh.consume(isBalanced);
            } catch (Exception e) {
                bh.consume(e.getMessage());
            }
        }
    }

    @Benchmark
    public void benchmarkCallbackEmissionTiming(Blackhole bh) {
        // Measure callback emission performance in visitor pattern
        for (String testCase : simpleTestCases) {
            document.setContent(testCase);
            callbackTester.clear();
            
            long callbackStartTime = System.nanoTime();
            try {
                adapter.parseWithStrategy(testCase);
                long callbackEndTime = System.nanoTime();
                
                List<String> sequence = callbackTester.getCallbackSequence();
                long callbackDuration = callbackEndTime - callbackStartTime;
                
                bh.consume(sequence.size());
                bh.consume(callbackDuration);
            } catch (Exception e) {
                bh.consume(e.getMessage());
            }
        }
    }

    @Benchmark
    public void benchmarkTreeDepthScaling(Blackhole bh) {
        // Test performance scaling with increasing AST depth
        for (String testCase : complexTestCases) {
            document.setContent(testCase);
            callbackTester.clear();
            
            try {
                Object result = adapter.parseWithStrategy(testCase);
                
                // Measure depth-related metrics
                int maxDepth = estimateASTDepth(testCase);
                int callbackCount = callbackTester.getCallbackCount();
                
                bh.consume(result);
                bh.consume(maxDepth);
                bh.consume(callbackCount);
            } catch (Exception e) {
                bh.consume(e.getMessage());
            }
        }
    }

    // =================================
    // Error Recovery and Robustness
    // =================================

    @Benchmark
    public void benchmarkErrorRecovery(Blackhole bh) {
        benchmarkWithTestCases(errorTestCases, bh);
    }

    @Benchmark
    public void benchmarkPartialParsing(Blackhole bh) {
        // Test visitor pattern's ability to handle incomplete code
        for (String testCase : errorTestCases) {
            document.setContent(testCase);
            callbackTester.clear();
            
            try {
                Object result = adapter.parseWithStrategy(testCase);
                
                // Even with errors, visitor pattern should generate some callbacks
                int callbackCount = callbackTester.getCallbackCount();
                boolean hasStructuralCallbacks = callbackTester.hasCallback("classStart") 
                                               || callbackTester.hasCallback("methodStart");
                
                bh.consume(result);
                bh.consume(callbackCount);
                bh.consume(hasStructuralCallbacks);
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
        // Test memory allocation patterns for AST visitor pattern
        Runtime runtime = Runtime.getRuntime();
        
        for (String testCase : veryComplexTestCases) {
            document.setContent(testCase);
            
            // Measure memory before and after
            System.gc(); // Force GC for more accurate measurement
            long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
            
            try {
                Object result = adapter.parseWithStrategy(testCase);
                
                long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
                long memoryUsed = memoryAfter - memoryBefore;
                
                bh.consume(result);
                bh.consume(memoryUsed);
            } catch (Exception e) {
                bh.consume(e.getMessage());
            }
        }
    }

    @Benchmark
    public void benchmarkLargeFileHandling(Blackhole bh) {
        benchmarkWithTestCases(largeFileTestCases, bh);
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

    private List<String> generateErrorCases(int count) {
        List<String> errorCases = new ArrayList<>();
        TestCorpusGenerator errorGenerator = new TestCorpusGenerator(true, false);
        
        for (int i = 0; i < count; i++) {
            String baseCase = corpusGenerator.generateMediumComplexityCode(1).get(0);
            errorCases.add(introduceError(baseCase, i));
        }
        
        return errorCases;
    }

    private List<String> generateLargeFileCases(int count) {
        List<String> largeCases = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            StringBuilder largeFile = new StringBuilder();
            largeFile.append("package test.large").append(i).append(";\n\n");
            
            // Create a large file with many classes and methods
            for (int j = 0; j < 10; j++) {
                largeFile.append("public class LargeClass").append(i).append("_").append(j).append(" {\n");
                
                for (int k = 0; k < 20; k++) {
                    largeFile.append("    public void method").append(k).append("() {\n");
                    largeFile.append("        System.out.println(\"Method ").append(k).append("\");\n");
                    largeFile.append("        for (int i = 0; i < 10; i++) {\n");
                    largeFile.append("            System.out.println(i);\n");
                    largeFile.append("        }\n");
                    largeFile.append("    }\n\n");
                }
                
                largeFile.append("}\n\n");
            }
            
            largeCases.add(largeFile.toString());
        }
        
        return largeCases;
    }

    private String introduceError(String code, int variant) {
        String[] lines = code.split("\n");
        List<String> result = new ArrayList<>(Arrays.asList(lines));
        
        switch (variant % 3) {
            case 0: // Missing closing brace
                result.remove(result.size() - 1);
                break;
            case 1: // Invalid syntax
                if (result.size() > 5) {
                    result.set(result.size() / 2, "    invalid syntax here;");
                }
                break;
            case 2: // Incomplete method
                result.add(result.size() - 1, "    public void incompleteMethod(");
                break;
        }
        
        return String.join("\n", result);
    }

    private int estimateASTDepth(String code) {
        // Simple heuristic to estimate AST depth based on nesting
        int maxDepth = 0;
        int currentDepth = 0;
        
        for (char c : code.toCharArray()) {
            if (c == '{') {
                currentDepth++;
                maxDepth = Math.max(maxDepth, currentDepth);
            } else if (c == '}') {
                currentDepth--;
            }
        }
        
        return maxDepth;
    }

    // =================================
    // JUnit Test Integration
    // =================================

    @Test
    public void testASTVisitorPatternPerformance() throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ASTVisitorPatternJMHBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(3)
                .measurementIterations(5)
                .build();

        Collection<RunResult> results = new Runner(opt).run();
        
        System.out.println("\n=== AST Visitor Pattern Performance Results ===");
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
                .include(ASTVisitorPatternJMHBenchmark.class.getSimpleName() + "\\." + benchmarkFilter)
                .forks(1)
                .warmupIterations(5)
                .measurementIterations(10)
                .build();
        
        Collection<RunResult> results = new Runner(opt).run();
        
        System.out.println(String.format("\n=== AST Visitor Pattern Benchmark Complete: %d results ===", results.size()));
        
        // Generate report
        try {
            BenchmarkReporter reporter = BenchmarkReporter.forConsole();
            // Additional reporting could be added here
        } catch (Exception e) {
            System.err.println("Failed to generate report: " + e.getMessage());
        }
    }
}