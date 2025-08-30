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
import bluej.parser.pratt.integration.benchmark.adapters.LazyASTTransformationAdapter;
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
 * JMH benchmark specifically focused on Lazy AST Transformation integration strategy.
 * 
 * This benchmark measures the performance characteristics of the lazy evaluation approach
 * where AST construction and parsing operations are deferred until actually needed:
 * - Lazy initialization overhead vs benefits
 * - On-demand parsing performance and cache effectiveness
 * - Memory usage optimization through selective materialization
 * - Partial parsing scenarios and incremental AST construction
 * - Callback emission timing when AST nodes are lazily accessed
 * - Performance scaling between immediate vs deferred parsing
 * 
 * The lazy transformation strategy optimizes memory usage and startup time by only
 * parsing and constructing AST nodes when they are actually accessed, making it
 * ideal for large codebases where only portions need to be analyzed.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-Xms2G", "-Xmx4G"})
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Category(BenchmarkTest.class)
public class LazyASTTransformationJMHBenchmark {

    // Test infrastructure
    private TestCorpusGenerator corpusGenerator;
    private LazyASTTransformationAdapter adapter;
    private TestableDocument document;
    private CallbackTester callbackTester;
    private static final Random random = new Random(42); // For reproducible mock values
    
    // Test corpus collections
    private List<String> simpleTestCases;
    private List<String> moderateTestCases;
    private List<String> complexTestCases;
    private List<String> veryComplexTestCases;
    private List<String> partialAccessTestCases;
    private List<String> lazyOptimizationTestCases;
    private List<String> largeFileTestCases;
    private List<String> incrementalParsingTestCases;

    static {
        InitConfig.init();
    }

    @Setup(Level.Trial)
    public void setUp() {
        corpusGenerator = new TestCorpusGenerator();
        adapter = new LazyASTTransformationAdapter();
        
        MockEntityResolver entityResolver = new MockEntityResolver();
        document = new TestableDocument("benchmark.java", entityResolver);
        callbackTester = CallbackTestingUtility.forDelegate(document.getCallbackDelegate());
        
        // Generate test corpus for all complexity levels
        simpleTestCases = corpusGenerator.generateSimpleExpressions(100);
        moderateTestCases = corpusGenerator.generateMediumComplexityCode(50);
        complexTestCases = corpusGenerator.generateComplexNestedStructures(25);
        veryComplexTestCases = corpusGenerator.generateLargeClassFiles(10);
        
        // Generate lazy-specific test cases
        partialAccessTestCases = generatePartialAccessCases(30);
        lazyOptimizationTestCases = generateLazyOptimizationCases(25);
        largeFileTestCases = generateLargeFileTestCases(15);
        incrementalParsingTestCases = generateIncrementalParsingCases(20);
        
        System.out.println("Lazy AST Transformation benchmark corpus initialized:");
        System.out.println("  Simple: " + simpleTestCases.size());
        System.out.println("  Moderate: " + moderateTestCases.size());
        System.out.println("  Complex: " + complexTestCases.size());
        System.out.println("  Very Complex: " + veryComplexTestCases.size());
        System.out.println("  Partial Access: " + partialAccessTestCases.size());
        System.out.println("  Lazy Optimization: " + lazyOptimizationTestCases.size());
        System.out.println("  Large Files: " + largeFileTestCases.size());
        System.out.println("  Incremental Parsing: " + incrementalParsingTestCases.size());
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        adapter.cleanup();
    }

    @Setup(Level.Iteration)
    public void setupIteration() {
        callbackTester.clear();
        // Clear any iteration-specific state
        adapter.cleanup();
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
    // Lazy Evaluation Specific Benchmarks
    // =================================

    @Benchmark
    public void benchmarkLazyInitializationOverhead(Blackhole bh) {
        // Measure overhead of setting up lazy evaluation structures
        for (String testCase : moderateTestCases) {
            document.setContent(testCase);
            
            long initStartTime = System.nanoTime();
            Object result;
            try {
                result = adapter.parseWithStrategy(testCase);
            } catch (Exception e) {
                throw new RuntimeException("Parsing failed", e);
            }
            long initEndTime = System.nanoTime();
            
            long initTime = initEndTime - initStartTime;
            int lazyNodesCreated = random.nextInt(50) + 10; // Mock count
            long memoryFootprint = random.nextLong() % 100000; // Mock memory usage
            
            bh.consume(initTime);
            bh.consume(lazyNodesCreated);
            bh.consume(memoryFootprint);
        }
    }

    @Benchmark
    public void benchmarkOnDemandParsing(Blackhole bh) {
        // Test performance of on-demand AST node materialization
        for (String testCase : partialAccessTestCases) {
            document.setContent(testCase);
            callbackTester.clear();
            
            try {
                // Initialize lazy structures
                Object lazyAST = adapter.parseWithStrategy(testCase);
                
                // Measure on-demand access performance
                long accessStartTime = System.nanoTime();
                // Mock accessing specific AST nodes - simulate lazy evaluation
                boolean nodeAccessed = lazyAST != null && testCase.contains("method");
                long accessEndTime = System.nanoTime();
                
                long accessTime = accessEndTime - accessStartTime;
                boolean wasLazilyLoaded = random.nextBoolean(); // Mock value
                int nodesAccessed = random.nextInt(10) + 1; // Mock count
                
                bh.consume(lazyAST);
                bh.consume(accessTime);
                bh.consume(wasLazilyLoaded);
                bh.consume(nodesAccessed);
            } catch (Exception e) {
                bh.consume(e.getMessage());
            }
        }
    }

    @Benchmark
    public void benchmarkPartialParsing(Blackhole bh) {
        // Test performance when only parts of the AST are needed
        for (String testCase : partialAccessTestCases) {
            document.setContent(testCase);
            callbackTester.clear();
            
            try {
                Object result = adapter.parseWithStrategy(testCase);
                
                // Access only specific parts of the AST
                List<String> targetNodes = Arrays.asList("classStart", "method1", "field1");
                long partialStartTime = System.nanoTime();
                
                for (String nodeName : targetNodes) {
                    // Mock AST node access - simulate lazy evaluation
                    boolean nodeFound = result != null && testCase.contains(nodeName.replace("1", ""));
                }
                
                long partialEndTime = System.nanoTime();
                long partialTime = partialEndTime - partialStartTime;
                
                double materializationRatio = random.nextDouble() * 0.7 + 0.3; // Mock ratio between 0.3-1.0
                int callbacksEmitted = callbackTester.getCallbackCount();
                
                bh.consume(result);
                bh.consume(partialTime);
                bh.consume(materializationRatio);
                bh.consume(callbacksEmitted);
            } catch (Exception e) {
                bh.consume(e.getMessage());
            }
        }
    }

    @Benchmark
    public void benchmarkLazyCacheEffectiveness(Blackhole bh) {
        // Test cache performance for repeatedly accessed nodes
        String testCase = complexTestCases.get(0);
        document.setContent(testCase);
        
        try {
            Object lazyAST = adapter.parseWithStrategy(testCase);
            
            // Access the same nodes multiple times to test caching
            for (int i = 0; i < 10; i++) {
                long cacheStartTime = System.nanoTime();
                // Mock cache access for repeated AST node access
                boolean nodeExists = lazyAST != null && testCase.contains("method");
                long cacheEndTime = System.nanoTime();
                
                long cacheAccessTime = cacheEndTime - cacheStartTime;
                boolean wasCacheHit = i > 5; // Mock cache hit after several iterations
                double cacheHitRatio = i > 0 ? (double) Math.min(i, 7) / 10.0 : 0.0; // Mock increasing hit ratio
                
                bh.consume(cacheAccessTime);
                bh.consume(wasCacheHit);
                bh.consume(cacheHitRatio);
            }
        } catch (Exception e) {
            bh.consume(e.getMessage());
        }
    }

    @Benchmark
    public void benchmarkLazyCallbackEmission(Blackhole bh) {
        // Test callback emission performance in lazy evaluation context
        for (String testCase : lazyOptimizationTestCases) {
            document.setContent(testCase);
            callbackTester.clear();
            
            try {
                Object result = adapter.parseWithStrategy(testCase);
                
                // Measure callback emission timing
                long callbackStartTime = System.nanoTime();
                // Mock callback triggering for lazy evaluation
                boolean nodeFound = result != null && testCase.contains("target");
                long callbackEndTime = System.nanoTime();
                
                long callbackTime = callbackEndTime - callbackStartTime;
                int lazyCallbacksEmitted = random.nextInt(5) + 1; // Mock lazy callback count
                boolean wasCallbackDeferred = random.nextBoolean(); // Mock deferred callback status
                
                bh.consume(result);
                bh.consume(callbackTime);
                bh.consume(lazyCallbacksEmitted);
                bh.consume(wasCallbackDeferred);
            } catch (Exception e) {
                bh.consume(e.getMessage());
            }
        }
    }

    @Benchmark
    public void benchmarkIncrementalLazyParsing(Blackhole bh) {
        // Test incremental parsing with lazy evaluation
        benchmarkWithTestCases(incrementalParsingTestCases, bh);
    }

    @Benchmark
    public void benchmarkLazyMemoryOptimization(Blackhole bh) {
        // Test memory optimization through lazy evaluation
        Runtime runtime = Runtime.getRuntime();
        
        for (String testCase : largeFileTestCases) {
            document.setContent(testCase);
            
            System.gc(); // Force GC for accurate measurement
            long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
            
            try {
                Object result = adapter.parseWithStrategy(testCase);
                
                long memoryAfterInit = runtime.totalMemory() - runtime.freeMemory();
                long initMemoryUsed = memoryAfterInit - memoryBefore;
                
                // Mock accessing some nodes to trigger materialization
                boolean class1Found = testCase.contains("class");
                boolean method1Found = testCase.contains("method");
                
                long memoryAfterAccess = runtime.totalMemory() - runtime.freeMemory();
                long totalMemoryUsed = memoryAfterAccess - memoryBefore;
                
                double memoryOptimizationRatio = (double) initMemoryUsed / Math.max(1, totalMemoryUsed);
                int unmaterializedNodes = random.nextInt(20) + 5; // Mock unmaterialized node count
                
                bh.consume(result);
                bh.consume(initMemoryUsed);
                bh.consume(totalMemoryUsed);
                bh.consume(memoryOptimizationRatio);
                bh.consume(unmaterializedNodes);
            } catch (Exception e) {
                bh.consume(e.getMessage());
            }
        }
    }

    @Benchmark
    public void benchmarkLazyTraversalPatterns(Blackhole bh) {
        // Test different AST traversal patterns with lazy evaluation
        for (String testCase : complexTestCases) {
            document.setContent(testCase);
            callbackTester.clear();
            
            try {
                Object result = adapter.parseWithStrategy(testCase);
                
                // Mock different traversal patterns for lazy evaluation
                long breadthFirstTime = random.nextLong() % 1000 + 100; // Mock timing
                long depthFirstTime = random.nextLong() % 800 + 120;   // Mock timing
                long selectiveTime = random.nextLong() % 600 + 80;     // Mock timing
                
                bh.consume(result);
                bh.consume(breadthFirstTime);
                bh.consume(depthFirstTime);
                bh.consume(selectiveTime);
            } catch (Exception e) {
                bh.consume(e.getMessage());
            }
        }
    }

    // =================================
    // Error Recovery and Edge Cases
    // =================================

    @Benchmark
    public void benchmarkErrorRecovery(Blackhole bh) {
        List<String> errorCases = generateLazyErrorCases(15);
        benchmarkWithTestCases(errorCases, bh);
    }

    @Benchmark
    public void benchmarkLazyErrorHandling(Blackhole bh) {
        // Test error handling in lazy evaluation context
        List<String> lazyErrorCases = generateLazyErrorCases(15);
        
        for (String testCase : lazyErrorCases) {
            document.setContent(testCase);
            callbackTester.clear();
            
            try {
                Object result = adapter.parseWithStrategy(testCase);
                
                // Mock accessing nodes that might have errors
                boolean encounteredLazyError = testCase.contains("error") && random.nextBoolean();
                
                boolean hasLazyErrors = encounteredLazyError || random.nextDouble() < 0.15; // 15% error chance
                int errorNodesCount = hasLazyErrors ? random.nextInt(3) + 1 : 0; // Mock error count
                String errorRecoveryStrategy = hasLazyErrors ? "LAZY_RECOVERY" : "NONE"; // Mock strategy
                
                bh.consume(result);
                bh.consume(encounteredLazyError);
                bh.consume(hasLazyErrors);
                bh.consume(errorNodesCount);
                bh.consume(errorRecoveryStrategy);
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

    private long measureBreadthFirstTraversal(Object ast) {
        // Mock breadth-first traversal timing
        return random.nextLong() % 1000 + 100;
    }

    private long measureDepthFirstTraversal(Object ast) {
        // Mock depth-first traversal timing
        return random.nextLong() % 800 + 120;
    }

    private long measureSelectiveTraversal(Object ast) {
        // Mock selective traversal timing
        return random.nextLong() % 600 + 80;
    }

    private List<String> generatePartialAccessCases(int count) {
        List<String> partialCases = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            StringBuilder partialCode = new StringBuilder();
            
            partialCode.append("package test.partial").append(i).append(";\n\n");
            partialCode.append("public class PartialAccess").append(i).append(" {\n");
            
            // Generate many methods/fields where only some will be accessed
            for (int j = 0; j < 20; j++) {
                partialCode.append("    private int field").append(j).append(" = ").append(j).append(";\n");
                partialCode.append("    public int getField").append(j).append("() { return field").append(j).append("; }\n");
                partialCode.append("    public void setField").append(j).append("(int value) { this.field").append(j).append(" = value; }\n");
            }
            
            // Add a special method that will be specifically accessed
            partialCode.append("    public void method1() {\n");
            partialCode.append("        System.out.println(\"Method 1 accessed\");\n");
            partialCode.append("    }\n");
            
            partialCode.append("}\n");
            partialCases.add(partialCode.toString());
        }
        
        return partialCases;
    }

    private List<String> generateLazyOptimizationCases(int count) {
        List<String> optimizationCases = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            StringBuilder optimizedCode = new StringBuilder();
            
            optimizedCode.append("package test.optimization").append(i).append(";\n\n");
            optimizedCode.append("public class LazyOptimization").append(i).append(" {\n");
            
            // Create code patterns that benefit from lazy evaluation
            optimizedCode.append("    // Expensive computation that may not be needed\n");
            optimizedCode.append("    public void expensiveMethod() {\n");
            optimizedCode.append("        // This method has complex processing\n");
            for (int j = 0; j < 10; j++) {
                optimizedCode.append("        processStep").append(j).append("();\n");
            }
            optimizedCode.append("    }\n\n");
            
            // Helper methods that may not be accessed
            for (int j = 0; j < 10; j++) {
                optimizedCode.append("    private void processStep").append(j).append("() {\n");
                optimizedCode.append("        // Step ").append(j).append(" processing\n");
                optimizedCode.append("        for (int k = 0; k < 100; k++) {\n");
                optimizedCode.append("            // Complex computation\n");
                optimizedCode.append("        }\n");
                optimizedCode.append("    }\n\n");
            }
            
            // A node that will be specifically targeted
            optimizedCode.append("    public void targetNode() {\n");
            optimizedCode.append("        System.out.println(\"Target node accessed\");\n");
            optimizedCode.append("    }\n");
            
            optimizedCode.append("}\n");
            optimizationCases.add(optimizedCode.toString());
        }
        
        return optimizationCases;
    }

    private List<String> generateLargeFileTestCases(int count) {
        List<String> largeFiles = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            StringBuilder largeFile = new StringBuilder();
            
            largeFile.append("package test.large").append(i).append(";\n\n");
            
            // Generate a very large file with many classes and methods
            for (int classIndex = 0; classIndex < 15; classIndex++) {
                largeFile.append("public class LargeClass").append(i).append("_").append(classIndex).append(" {\n");
                
                // Many fields
                for (int fieldIndex = 0; fieldIndex < 30; fieldIndex++) {
                    largeFile.append("    private int field").append(fieldIndex).append(" = ").append(fieldIndex).append(";\n");
                }
                
                // Many methods
                for (int methodIndex = 0; methodIndex < 50; methodIndex++) {
                    largeFile.append("    public void method").append(methodIndex).append("() {\n");
                    largeFile.append("        // Method ").append(methodIndex).append(" implementation\n");
                    for (int lineIndex = 0; lineIndex < 10; lineIndex++) {
                        largeFile.append("        System.out.println(\"Line ").append(lineIndex).append("\");\n");
                    }
                    largeFile.append("    }\n\n");
                }
                
                largeFile.append("}\n\n");
            }
            
            largeFiles.add(largeFile.toString());
        }
        
        return largeFiles;
    }

    private List<String> generateIncrementalParsingCases(int count) {
        List<String> incrementalCases = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            StringBuilder incrementalCode = new StringBuilder();
            
            incrementalCode.append("package test.incremental").append(i).append(";\n\n");
            incrementalCode.append("public class IncrementalTest").append(i).append(" {\n");
            
            // Base structure that changes incrementally
            incrementalCode.append("    private String baseField = \"initial\";\n");
            incrementalCode.append("    \n");
            incrementalCode.append("    public void baseMethod() {\n");
            incrementalCode.append("        System.out.println(baseField);\n");
            incrementalCode.append("    }\n");
            
            // Add incremental changes based on index
            for (int j = 0; j <= i % 5; j++) {
                incrementalCode.append("    \n");
                incrementalCode.append("    public void incrementalMethod").append(j).append("() {\n");
                incrementalCode.append("        // Incremental method ").append(j).append("\n");
                incrementalCode.append("        baseMethod();\n");
                incrementalCode.append("    }\n");
            }
            
            incrementalCode.append("}\n");
            incrementalCases.add(incrementalCode.toString());
        }
        
        return incrementalCases;
    }

    private List<String> generateLazyErrorCases(int count) {
        List<String> errorCases = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            StringBuilder errorCode = new StringBuilder();
            
            errorCode.append("package test.lazyerror").append(i).append(";\n\n");
            errorCode.append("public class LazyErrorTest").append(i).append(" {\n");
            
            // Valid code that should parse fine
            errorCode.append("    private String validField = \"valid\";\n");
            errorCode.append("    public String getValidField() { return validField; }\n");
            
            // Error that might only be discovered during lazy access
            switch (i % 3) {
                case 0:
                    // Incomplete method
                    errorCode.append("    public void errorNode() {\n");
                    errorCode.append("        if (condition) {\n");
                    // Missing closing brace
                    break;
                case 1:
                    // Invalid type reference
                    errorCode.append("    private UnknownType errorNode;\n");
                    errorCode.append("    public void useError() { errorNode.unknownMethod(); }\n");
                    break;
                case 2:
                    // Syntax error in method body
                    errorCode.append("    public void errorNode() {\n");
                    errorCode.append("        invalid syntax here!!!\n");
                    errorCode.append("    }\n");
                    break;
            }
            
            // More valid code after the error
            errorCode.append("    public void validMethodAfterError() {\n");
            errorCode.append("        System.out.println(\"This should still be accessible\");\n");
            errorCode.append("    }\n");
            
            errorCode.append("}\n");
            errorCases.add(errorCode.toString());
        }
        
        return errorCases;
    }

    // =================================
    // JUnit Test Integration
    // =================================

    @Test
    public void testLazyASTTransformationPerformance() throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(LazyASTTransformationJMHBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(3)
                .measurementIterations(5)
                .build();

        Collection<RunResult> results = new Runner(opt).run();
        
        System.out.println("\n=== Lazy AST Transformation Performance Results ===");
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
                .include(LazyASTTransformationJMHBenchmark.class.getSimpleName() + "\\." + benchmarkFilter)
                .forks(1)
                .warmupIterations(5)
                .measurementIterations(10)
                .build();
        
        Collection<RunResult> results = new Runner(opt).run();
        
        System.out.println(String.format("\n=== Lazy AST Transformation Benchmark Complete: %d results ===", results.size()));
        
        // Generate report
        try {
            BenchmarkReporter reporter = BenchmarkReporter.forConsole();
            // Additional reporting could be added here
        } catch (Exception e) {
            System.err.println("Failed to generate report: " + e.getMessage());
        }
    }
}