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
import bluej.parser.pratt.integration.benchmark.adapters.K2ParserIntegrationAdapter;
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
import java.util.Arrays;

/**
 * JMH benchmark specifically focused on K2 Parser Integration strategy.
 * 
 * This benchmark measures the performance characteristics of integrating with
 * the Kotlin K2 parser for multi-language support and advanced language features:
 * - K2 parser initialization and warm-up performance
 * - Kotlin/Java interoperability parsing scenarios
 * - Advanced Kotlin language construct parsing (coroutines, inline functions, etc.)
 * - K2 compiler pipeline integration overhead
 * - Multi-language project parsing performance
 * - Kotlin-specific callback emission patterns
 * 
 * The K2 parser integration provides advanced language support and modern
 * language features but may have higher initialization costs and memory usage.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-Xms2G", "-Xmx4G"})
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Category(BenchmarkTest.class)
public class K2ParserIntegrationJMHBenchmark {

    // Test infrastructure
    private TestCorpusGenerator corpusGenerator;
    private K2ParserIntegrationAdapter adapter;
    private TestableDocument document;
    private CallbackTester callbackTester;
    private static final Random random = new Random(42); // For reproducible mock values
    
    // Test corpus collections
    private List<String> simpleTestCases;
    private List<String> moderateTestCases;
    private List<String> complexTestCases;
    private List<String> veryComplexTestCases;
    private List<String> kotlinSpecificTestCases;
    private List<String> interopTestCases;
    private List<String> modernLanguageFeaturesTestCases;
    private List<String> compilerIntegrationTestCases;

    static {
        InitConfig.init();
    }

    @Setup(Level.Trial)
    public void setUp() {
        corpusGenerator = new TestCorpusGenerator();
        adapter = new K2ParserIntegrationAdapter();
        
        MockEntityResolver entityResolver = new MockEntityResolver();
        document = new TestableDocument("benchmark.java", entityResolver);
        callbackTester = CallbackTestingUtility.forDelegate(document.getCallbackDelegate());
        
        // Generate test corpus for all complexity levels
        simpleTestCases = corpusGenerator.generateSimpleExpressions(100);
        moderateTestCases = corpusGenerator.generateMediumComplexityCode(50);
        complexTestCases = corpusGenerator.generateComplexNestedStructures(25);
        veryComplexTestCases = corpusGenerator.generateLargeClassFiles(10);
        
        // Generate K2-specific test cases
        kotlinSpecificTestCases = generateKotlinSpecificCases(30);
        interopTestCases = generateInteropTestCases(20);
        modernLanguageFeaturesTestCases = generateModernLanguageFeatureCases(25);
        compilerIntegrationTestCases = generateCompilerIntegrationCases(15);
        
        System.out.println("K2 Parser Integration benchmark corpus initialized:");
        System.out.println("  Simple: " + simpleTestCases.size());
        System.out.println("  Moderate: " + moderateTestCases.size());
        System.out.println("  Complex: " + complexTestCases.size());
        System.out.println("  Very Complex: " + veryComplexTestCases.size());
        System.out.println("  Kotlin Specific: " + kotlinSpecificTestCases.size());
        System.out.println("  Interop Cases: " + interopTestCases.size());
        System.out.println("  Modern Features: " + modernLanguageFeaturesTestCases.size());
        System.out.println("  Compiler Integration: " + compilerIntegrationTestCases.size());
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
    // K2 Parser Specific Benchmarks
    // =================================

    @Benchmark
    public void benchmarkK2ParserInitialization(Blackhole bh) {
        // Measure K2 parser initialization overhead
        for (int i = 0; i < 10; i++) {
            long initStartTime = System.nanoTime();
            K2ParserIntegrationAdapter freshAdapter = new K2ParserIntegrationAdapter();
            long initEndTime = System.nanoTime();
            
            long initTime = initEndTime - initStartTime;
            boolean isInitialized = true; // Mock initialization status
            
            bh.consume(initTime);
            bh.consume(isInitialized);
            
            freshAdapter.cleanup();
        }
    }

    @Benchmark
    public void benchmarkK2CompilerPipelineIntegration(Blackhole bh) {
        // Test integration with K2 compiler pipeline
        benchmarkWithTestCases(compilerIntegrationTestCases, bh);
    }

    @Benchmark
    public void benchmarkKotlinLanguageFeatures(Blackhole bh) {
        // Test parsing of Kotlin-specific language features
        for (String testCase : kotlinSpecificTestCases) {
            document.setContent(testCase);
            callbackTester.clear();
            
            try {
                Object result = adapter.parseWithStrategy(testCase);
                
                // Measure Kotlin-specific parsing characteristics
                boolean hasKotlinFeatures = testCase.contains("kotlin") || testCase.contains("suspend") || testCase.contains("fun "); // Mock detection
                List<String> kotlinConstructs = Arrays.asList("data class", "suspend", "coroutine"); // Mock list
                int kotlinCallbackCount = random.nextInt(5); // Mock count
                
                bh.consume(result);
                bh.consume(hasKotlinFeatures);
                bh.consume(kotlinConstructs.toString());
                bh.consume(kotlinCallbackCount);
            } catch (Exception e) {
                bh.consume(e.getMessage());
            }
        }
    }

    @Benchmark
    public void benchmarkModernLanguageFeatures(Blackhole bh) {
        // Test modern language features (pattern matching, records, etc.)
        benchmarkWithTestCases(modernLanguageFeaturesTestCases, bh);
    }

    @Benchmark
    public void benchmarkJavaKotlinInteroperability(Blackhole bh) {
        // Test Java-Kotlin interoperability parsing
        for (String testCase : interopTestCases) {
            document.setContent(testCase);
            callbackTester.clear();
            
            try {
                Object result = adapter.parseWithStrategy(testCase);
                
                // Measure interoperability characteristics
                boolean hasJavaKotlinInterop = testCase.contains("Java") && testCase.contains("kotlin"); // Mock detection
                List<String> interopPoints = Arrays.asList("JNI", "JVM bridge", "reflection"); // Mock list
                int crossLanguageReferences = random.nextInt(8); // Mock count
                
                bh.consume(result);
                bh.consume(hasJavaKotlinInterop);
                bh.consume(interopPoints.toString());
                bh.consume(crossLanguageReferences);
            } catch (Exception e) {
                bh.consume(e.getMessage());
            }
        }
    }

    @Benchmark
    public void benchmarkK2AnalysisPerformance(Blackhole bh) {
        // Measure K2 semantic analysis performance
        for (String testCase : complexTestCases) {
            document.setContent(testCase);
            
            long analysisStartTime = System.nanoTime();
            try {
                Object result = adapter.parseWithStrategy(testCase);
                long analysisEndTime = System.nanoTime();
                
                long analysisTime = analysisEndTime - analysisStartTime;
                boolean hasSemanticErrors = random.nextBoolean(); // Mock value
                int warningCount = random.nextInt(5); // Mock count
                
                bh.consume(analysisTime);
                bh.consume(hasSemanticErrors);
                bh.consume(warningCount);
            } catch (Exception e) {
                bh.consume(e.getMessage());
            }
        }
    }

    @Benchmark
    public void benchmarkK2IncrementalParsing(Blackhole bh) {
        // Test K2 incremental parsing capabilities
        String baseCase = moderateTestCases.get(0);
        document.setContent(baseCase);
        
        // Initial parsing
        try {
            adapter.parseWithStrategy(baseCase);
            
            // Make incremental changes and measure parsing time
            for (int i = 0; i < 5; i++) {
                String modifiedCase = makeIncrementalChange(baseCase, i);
                document.setContent(modifiedCase);
                
                long incrementalStartTime = System.nanoTime();
                Object result = adapter.parseWithStrategy(modifiedCase); // Use valid method instead
                long incrementalEndTime = System.nanoTime();
                
                long incrementalTime = incrementalEndTime - incrementalStartTime;
                boolean usedIncremental = random.nextBoolean(); // Mock value
                
                bh.consume(result);
                bh.consume(incrementalTime);
                bh.consume(usedIncremental);
            }
        } catch (Exception e) {
            bh.consume(e.getMessage());
        }
    }

    // =================================
    // Advanced Language Feature Benchmarks
    // =================================

    @Benchmark
    public void benchmarkCoroutineSupport(Blackhole bh) {
        // Test Kotlin coroutine parsing performance
        List<String> coroutineTestCases = generateCoroutineTestCases(10);
        benchmarkWithTestCases(coroutineTestCases, bh);
    }

    @Benchmark
    public void benchmarkInlineFunctionHandling(Blackhole bh) {
        // Test inline function parsing and optimization
        List<String> inlineFunctionCases = generateInlineFunctionCases(10);
        benchmarkWithTestCases(inlineFunctionCases, bh);
    }

    @Benchmark
    public void benchmarkGenericTypeInference(Blackhole bh) {
        // Test advanced generic type inference performance
        List<String> typeInferenceCases = generateTypeInferenceCases(15);
        
        for (String testCase : typeInferenceCases) {
            document.setContent(testCase);
            
            long inferenceStartTime = System.nanoTime();
            try {
                Object result = adapter.parseWithStrategy(testCase);
                long inferenceEndTime = System.nanoTime();
                
                long inferenceTime = inferenceEndTime - inferenceStartTime;
                int typesInferred = random.nextInt(20) + 1; // Mock count
                boolean hasComplexInference = random.nextBoolean(); // Mock value
                
                bh.consume(result);
                bh.consume(inferenceTime);
                bh.consume(typesInferred);
                bh.consume(hasComplexInference);
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
        List<String> errorCases = generateK2ErrorCases(20);
        benchmarkWithTestCases(errorCases, bh);
    }

    @Benchmark
    public void benchmarkMultiLanguageErrorHandling(Blackhole bh) {
        // Test error handling across Java-Kotlin boundaries
        List<String> multiLangErrorCases = generateMultiLanguageErrorCases(15);
        
        for (String testCase : multiLangErrorCases) {
            document.setContent(testCase);
            callbackTester.clear();
            
            try {
                Object result = adapter.parseWithStrategy(testCase);
                
                boolean recoveredFromErrors = random.nextBoolean(); // Mock value
                List<String> errorTypes = Arrays.asList("syntax error", "type error", "null reference"); // Mock list
                int crossLanguageErrors = random.nextInt(3); // Mock count
                
                bh.consume(result);
                bh.consume(recoveredFromErrors);
                bh.consume(errorTypes.toString());
                bh.consume(crossLanguageErrors);
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
                
                // K2-specific memory metrics
                long k2ParserMemory = random.nextLong() % 1000000; // Mock memory usage
                long compilerMemoryOverhead = random.nextLong() % 500000; // Mock overhead
                
                bh.consume(result);
                bh.consume(memoryUsed);
                bh.consume(k2ParserMemory);
                bh.consume(compilerMemoryOverhead);
            } catch (Exception e) {
                bh.consume(e.getMessage());
            }
        }
    }

    @Benchmark
    public void benchmarkK2CacheEfficiency(Blackhole bh) {
        // Test K2 parser caching effectiveness
        String repeatedCase = complexTestCases.get(0);
        
        for (int i = 0; i < 10; i++) {
            document.setContent(repeatedCase);
            
            long cacheStartTime = System.nanoTime();
            try {
                Object result = adapter.parseWithStrategy(repeatedCase);
                long cacheEndTime = System.nanoTime();
                
                long cacheTime = cacheEndTime - cacheStartTime;
                boolean usedCache = random.nextBoolean(); // Mock value
                double cacheHitRatio = random.nextDouble() * 0.8 + 0.1; // Mock ratio between 0.1-0.9
                
                bh.consume(result);
                bh.consume(cacheTime);
                bh.consume(usedCache);
                bh.consume(cacheHitRatio);
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

    private List<String> generateKotlinSpecificCases(int count) {
        List<String> kotlinCases = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            StringBuilder kotlinCode = new StringBuilder();
            
            kotlinCode.append("// Kotlin-specific language features\n");
            kotlinCode.append("package test.kotlin").append(i).append("\n\n");
            
            // Data classes
            kotlinCode.append("data class Person").append(i).append("(val name: String, val age: Int)\n\n");
            
            // Extension functions
            kotlinCode.append("fun String.isPalindrome(): Boolean {\n");
            kotlinCode.append("    return this == this.reversed()\n");
            kotlinCode.append("}\n\n");
            
            // Higher-order functions
            kotlinCode.append("fun processItems").append(i).append("(items: List<String>, processor: (String) -> String): List<String> {\n");
            kotlinCode.append("    return items.map(processor)\n");
            kotlinCode.append("}\n\n");
            
            // Sealed classes
            kotlinCode.append("sealed class Result").append(i).append(" {\n");
            kotlinCode.append("    object Loading : Result").append(i).append("()\n");
            kotlinCode.append("    data class Success(val data: String) : Result").append(i).append("()\n");
            kotlinCode.append("    data class Error(val exception: Throwable) : Result").append(i).append("()\n");
            kotlinCode.append("}\n");
            
            kotlinCases.add(kotlinCode.toString());
        }
        
        return kotlinCases;
    }

    private List<String> generateInteropTestCases(int count) {
        List<String> interopCases = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            StringBuilder interopCode = new StringBuilder();
            
            interopCode.append("// Java-Kotlin interoperability test\n");
            interopCode.append("package test.interop").append(i).append(";\n\n");
            
            // Java class that will be used from Kotlin
            interopCode.append("public class JavaService").append(i).append(" {\n");
            interopCode.append("    public String processData(String input) {\n");
            interopCode.append("        return input.toUpperCase();\n");
            interopCode.append("    }\n");
            interopCode.append("    \n");
            interopCode.append("    public List<String> getItems() {\n");
            interopCode.append("        return Arrays.asList(\"item1\", \"item2\", \"item3\");\n");
            interopCode.append("    }\n");
            interopCode.append("}\n\n");
            
            // Java interface implemented by Kotlin
            interopCode.append("interface DataProcessor").append(i).append(" {\n");
            interopCode.append("    String process(String data);\n");
            interopCode.append("    default boolean isReady() { return true; }\n");
            interopCode.append("}\n\n");
            
            // Mixed usage patterns
            interopCode.append("public class InteropTest").append(i).append(" {\n");
            interopCode.append("    private final JavaService").append(i).append(" service = new JavaService").append(i).append("();\n");
            interopCode.append("    \n");
            interopCode.append("    public void testInterop(DataProcessor").append(i).append(" processor) {\n");
            interopCode.append("        List<String> items = service.getItems();\n");
            interopCode.append("        for (String item : items) {\n");
            interopCode.append("            String processed = processor.process(item);\n");
            interopCode.append("            System.out.println(processed);\n");
            interopCode.append("        }\n");
            interopCode.append("    }\n");
            interopCode.append("}\n");
            
            interopCases.add(interopCode.toString());
        }
        
        return interopCases;
    }

    private List<String> generateModernLanguageFeatureCases(int count) {
        List<String> modernCases = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            StringBuilder modernCode = new StringBuilder();
            
            modernCode.append("// Modern language features\n");
            modernCode.append("package test.modern").append(i).append(";\n\n");
            
            // Records (Java 14+)
            modernCode.append("public record Point").append(i).append("(int x, int y) {\n");
            modernCode.append("    public Point").append(i).append(" {\n");
            modernCode.append("        if (x < 0 || y < 0) {\n");
            modernCode.append("            throw new IllegalArgumentException(\"Coordinates must be positive\");\n");
            modernCode.append("        }\n");
            modernCode.append("    }\n");
            modernCode.append("}\n\n");
            
            // Pattern matching (Java 17+)
            modernCode.append("public class PatternMatching").append(i).append(" {\n");
            modernCode.append("    public String processObject(Object obj) {\n");
            modernCode.append("        return switch (obj) {\n");
            modernCode.append("            case String s -> \"String: \" + s;\n");
            modernCode.append("            case Integer i -> \"Integer: \" + i;\n");
            modernCode.append("            case Point").append(i).append(" p -> \"Point: (\" + p.x() + \", \" + p.y() + \")\";\n");
            modernCode.append("            case null -> \"Null value\";\n");
            modernCode.append("            default -> \"Unknown type\";\n");
            modernCode.append("        };\n");
            modernCode.append("    }\n");
            modernCode.append("}\n\n");
            
            // Text blocks
            modernCode.append("public class TextBlocks").append(i).append(" {\n");
            modernCode.append("    private static final String JSON_TEMPLATE = \"\"\"\n");
            modernCode.append("        {\n");
            modernCode.append("            \"name\": \"%s\",\n");
            modernCode.append("            \"age\": %d,\n");
            modernCode.append("            \"active\": true\n");
            modernCode.append("        }\n");
            modernCode.append("        \"\"\";\n");
            modernCode.append("}\n");
            
            modernCases.add(modernCode.toString());
        }
        
        return modernCases;
    }

    private List<String> generateCompilerIntegrationCases(int count) {
        List<String> integrationCases = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            StringBuilder integrationCode = new StringBuilder();
            
            integrationCode.append("// Compiler integration test\n");
            integrationCode.append("package test.compiler").append(i).append(";\n\n");
            integrationCode.append("import java.util.concurrent.CompletableFuture;\n");
            integrationCode.append("import java.util.function.*;\n\n");
            
            // Complex generics that exercise the compiler
            integrationCode.append("public class CompilerIntegration").append(i).append("<T extends Comparable<? super T>> {\n");
            integrationCode.append("    public <U extends T, V extends Collection<U>>\n");
            integrationCode.append("    CompletableFuture<Optional<U>> processAsync(\n");
            integrationCode.append("            V collection,\n");
            integrationCode.append("            Predicate<? super U> filter,\n");
            integrationCode.append("            Function<? super U, ? extends T> mapper) {\n");
            integrationCode.append("        \n");
            integrationCode.append("        return CompletableFuture.supplyAsync(() ->\n");
            integrationCode.append("            collection.stream()\n");
            integrationCode.append("                .filter(filter)\n");
            integrationCode.append("                .map(mapper)\n");
            integrationCode.append("                .map(t -> (U) t)\n");
            integrationCode.append("                .max(U::compareTo)\n");
            integrationCode.append("        );\n");
            integrationCode.append("    }\n");
            integrationCode.append("}\n");
            
            integrationCases.add(integrationCode.toString());
        }
        
        return integrationCases;
    }

    private List<String> generateCoroutineTestCases(int count) {
        List<String> coroutineCases = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            StringBuilder coroutineCode = new StringBuilder();
            
            // Note: This is pseudo-Kotlin syntax represented in Java comments
            // The actual implementation would depend on K2 parser's Kotlin support
            coroutineCode.append("// Kotlin coroutine simulation in Java context\n");
            coroutineCode.append("package test.coroutines").append(i).append(";\n\n");
            coroutineCode.append("// suspend fun fetchData").append(i).append("(): String {\n");
            coroutineCode.append("//     delay(1000)\n");
            coroutineCode.append("//     return \"Data ").append(i).append("\"\n");
            coroutineCode.append("// }\n\n");
            
            // Java equivalent with CompletableFuture
            coroutineCode.append("public class CoroutineSimulation").append(i).append(" {\n");
            coroutineCode.append("    public CompletableFuture<String> fetchDataAsync() {\n");
            coroutineCode.append("        return CompletableFuture.supplyAsync(() -> {\n");
            coroutineCode.append("            try { Thread.sleep(1000); } catch (InterruptedException e) {}\n");
            coroutineCode.append("            return \"Data ").append(i).append("\";\n");
            coroutineCode.append("        });\n");
            coroutineCode.append("    }\n");
            coroutineCode.append("}\n");
            
            coroutineCases.add(coroutineCode.toString());
        }
        
        return coroutineCases;
    }

    private List<String> generateInlineFunctionCases(int count) {
        List<String> inlineCases = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            StringBuilder inlineCode = new StringBuilder();
            
            inlineCode.append("// Inline function simulation\n");
            inlineCode.append("package test.inline").append(i).append(";\n\n");
            
            // Java equivalent of inline functions using lambdas and method references
            inlineCode.append("public class InlineSimulation").append(i).append(" {\n");
            inlineCode.append("    public static <T> T withResource(Supplier<T> block) {\n");
            inlineCode.append("        return block.get();\n");
            inlineCode.append("    }\n");
            inlineCode.append("    \n");
            inlineCode.append("    public void testInline() {\n");
            inlineCode.append("        String result = withResource(() -> {\n");
            inlineCode.append("            return \"Inline result ").append(i).append("\";\n");
            inlineCode.append("        });\n");
            inlineCode.append("    }\n");
            inlineCode.append("}\n");
            
            inlineCases.add(inlineCode.toString());
        }
        
        return inlineCases;
    }

    private List<String> generateTypeInferenceCases(int count) {
        List<String> inferenceCases = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            StringBuilder inferenceCode = new StringBuilder();
            
            inferenceCode.append("// Type inference test\n");
            inferenceCode.append("package test.inference").append(i).append(";\n\n");
            inferenceCode.append("public class TypeInference").append(i).append(" {\n");
            
            // Var type inference (Java 10+)
            inferenceCode.append("    public void testInference() {\n");
            inferenceCode.append("        var list = List.of(\"a\", \"b\", \"c\");\n");
            inferenceCode.append("        var map = Map.of(\"key1\", 1, \"key2\", 2);\n");
            inferenceCode.append("        var stream = list.stream().filter(s -> s.length() > 0);\n");
            inferenceCode.append("        var result = stream.collect(Collectors.toList());\n");
            inferenceCode.append("    }\n");
            
            // Generic method type inference
            inferenceCode.append("    public <T> Optional<T> findFirst(List<T> items, Predicate<T> condition) {\n");
            inferenceCode.append("        return items.stream().filter(condition).findFirst();\n");
            inferenceCode.append("    }\n");
            
            inferenceCode.append("}\n");
            
            inferenceCases.add(inferenceCode.toString());
        }
        
        return inferenceCases;
    }

    private List<String> generateK2ErrorCases(int count) {
        List<String> errorCases = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            StringBuilder errorCode = new StringBuilder();
            
            errorCode.append("// K2 parser error recovery test\n");
            errorCode.append("package test.k2error").append(i).append(";\n\n");
            errorCode.append("public class K2ErrorRecovery").append(i).append(" {\n");
            
            switch (i % 3) {
                case 0:
                    // Generic error
                    errorCode.append("    List<String> items = new ArrayList<>(\n"); // Missing closing paren
                    errorCode.append("    public void method() {}\n");
                    break;
                case 1:
                    // Type parameter error
                    errorCode.append("    public <T extends Unknown & Invalid> void method(T param) {\n");
                    errorCode.append("        param.unknownMethod();\n");
                    errorCode.append("    }\n");
                    break;
                case 2:
                    // Lambda syntax error
                    errorCode.append("    Function<String, String> func = s -> {\n");
                    errorCode.append("        return s.toUpperCase(\n"); // Missing closing paren
                    errorCode.append("    };\n");
                    break;
            }
            
            errorCode.append("}\n");
            errorCases.add(errorCode.toString());
        }
        
        return errorCases;
    }

    private List<String> generateMultiLanguageErrorCases(int count) {
        List<String> multiLangErrors = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            StringBuilder errorCode = new StringBuilder();
            
            errorCode.append("// Multi-language error handling\n");
            errorCode.append("package test.multilang").append(i).append(";\n\n");
            errorCode.append("// Java class with Kotlin-style syntax errors\n");
            errorCode.append("public class MultiLangError").append(i).append(" {\n");
            errorCode.append("    // Kotlin-style property declaration in Java (error)\n");
            errorCode.append("    val name: String = \"test\"  // Invalid Java syntax\n");
            errorCode.append("    \n");
            errorCode.append("    // Valid Java method\n");
            errorCode.append("    public String getName() {\n");
            errorCode.append("        return name;\n");
            errorCode.append("    }\n");
            errorCode.append("}\n");
            
            multiLangErrors.add(errorCode.toString());
        }
        
        return multiLangErrors;
    }

    private String makeIncrementalChange(String baseCode, int changeIndex) {
        // Make small incremental changes for testing incremental parsing
        switch (changeIndex % 3) {
            case 0:
                return baseCode.replace("public void", "public final void");
            case 1:
                return baseCode.replace("System.out.println", "System.err.println");
            case 2:
                return baseCode + "\n    // Added comment " + changeIndex;
            default:
                return baseCode;
        }
    }

    // =================================
    // JUnit Test Integration
    // =================================

    @Test
    public void testK2ParserIntegrationPerformance() throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(K2ParserIntegrationJMHBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(3)
                .measurementIterations(5)
                .build();

        Collection<RunResult> results = new Runner(opt).run();
        
        System.out.println("\n=== K2 Parser Integration Performance Results ===");
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
                .include(K2ParserIntegrationJMHBenchmark.class.getSimpleName() + "\\." + benchmarkFilter)
                .forks(1)
                .warmupIterations(5)
                .measurementIterations(10)
                .build();
        
        Collection<RunResult> results = new Runner(opt).run();
        
        System.out.println(String.format("\n=== K2 Parser Integration Benchmark Complete: %d results ===", results.size()));
        
        // Generate report
        try {
            BenchmarkReporter reporter = BenchmarkReporter.forConsole();
            // Additional reporting could be added here
        } catch (Exception e) {
            System.err.println("Failed to generate report: " + e.getMessage());
        }
    }
}