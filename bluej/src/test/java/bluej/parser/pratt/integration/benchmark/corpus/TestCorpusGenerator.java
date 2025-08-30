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
package bluej.parser.pratt.integration.benchmark.corpus;

import bluej.parser.pratt.integration.benchmark.BenchmarkConfiguration;

import java.util.*;
import java.util.random.RandomGenerator;

/**
 * Comprehensive test corpus generator for parser integration benchmarking.
 * 
 * This class generates Java source code samples across different complexity levels
 * to provide consistent, reproducible test data for parser strategy benchmarking.
 * It creates code samples that exercise various parsing scenarios including:
 * 
 * - Basic syntax (classes, methods, fields)
 * - Advanced language features (generics, lambdas, streams)
 * - Error conditions and edge cases
 * - Performance stress scenarios
 * - Real-world coding patterns
 * 
 * The generator supports customization for error cases, incomplete code,
 * and specific complexity requirements.
 */
public class TestCorpusGenerator {
    
    private final boolean includeErrorCases;
    private final boolean includeIncompleteCode;
    private final RandomGenerator random;
    
    // Code templates for different complexity levels
    private static final Map<BenchmarkConfiguration.ComplexityLevel, List<String>> CODE_TEMPLATES;
    
    static {
        CODE_TEMPLATES = new HashMap<>();
        initializeTemplates();
    }
    
    public TestCorpusGenerator() {
        this(false, false);
    }
    
    public TestCorpusGenerator(boolean includeErrorCases, boolean includeIncompleteCode) {
        this.includeErrorCases = includeErrorCases;
        this.includeIncompleteCode = includeIncompleteCode;
        this.random = RandomGenerator.getDefault();
    }
    
    /**
     * Generate test cases for a specific complexity level.
     */
    public List<String> generateTestCases(BenchmarkConfiguration.ComplexityLevel level, int count) {
        List<String> testCases = new ArrayList<>();
        List<String> templates = CODE_TEMPLATES.get(level);
        
        for (int i = 0; i < count; i++) {
            String template = templates.get(random.nextInt(templates.size()));
            String testCase = generateFromTemplate(template, i);
            
            if (includeErrorCases && random.nextFloat() < 0.2) {
                testCase = introduceError(testCase);
            }
            
            if (includeIncompleteCode && random.nextFloat() < 0.1) {
                testCase = makeIncomplete(testCase);
            }
            
            testCases.add(testCase);
        }
        
        return testCases;
    }
    
    /**
     * Generate from a template with parameter substitution.
     */
    private String generateFromTemplate(String template, int index) {
        return template
                .replace("{CLASS_NAME}", "TestClass" + index)
                .replace("{METHOD_NAME}", "testMethod" + index)
                .replace("{FIELD_NAME}", "testField" + index)
                .replace("{VARIABLE_NAME}", "testVar" + index)
                .replace("{INDEX}", String.valueOf(index))
                .replace("{RANDOM_INT}", String.valueOf(random.nextInt(100)))
                .replace("{RANDOM_STRING}", "\"randomString" + random.nextInt(1000) + "\"");
    }
    
    /**
     * Introduce syntax errors for error recovery testing.
     */
    private String introduceError(String code) {
        String[] errorTypes = {
            "missing_semicolon",
            "missing_brace",
            "invalid_syntax",
            "type_mismatch"
        };
        
        String errorType = errorTypes[random.nextInt(errorTypes.length)];
        
        switch (errorType) {
            case "missing_semicolon":
                return code.replaceFirst(";", "");
            case "missing_brace":
                return code.replaceFirst("\\}", "");
            case "invalid_syntax":
                return code.replace("public", "pubblic");
            case "type_mismatch":
                return code.replace("String", "StringMismatch");
            default:
                return code;
        }
    }
    
    /**
     * Make code incomplete for partial parsing testing.
     */
    private String makeIncomplete(String code) {
        int cutPoint = code.length() * 3 / 4;
        return code.substring(0, cutPoint);
    }
    
    /**
     * Initialize code templates for different complexity levels.
     */
    private static void initializeTemplates() {
        // Simple templates
        CODE_TEMPLATES.put(BenchmarkConfiguration.ComplexityLevel.SIMPLE, Arrays.asList(
            """
            public class {CLASS_NAME} {
                public void {METHOD_NAME}() {
                    System.out.println("Hello World");
                }
            }
            """,
            
            """
            public class {CLASS_NAME} {
                private String {FIELD_NAME} = {RANDOM_STRING};
                
                public String get{FIELD_NAME}() {
                    return {FIELD_NAME};
                }
            }
            """,
            
            """
            public class {CLASS_NAME} {
                public int {METHOD_NAME}(int x, int y) {
                    return x + y;
                }
            }
            """
        ));
        
        // Moderate templates
        CODE_TEMPLATES.put(BenchmarkConfiguration.ComplexityLevel.MODERATE, Arrays.asList(
            """
            public class {CLASS_NAME} {
                private List<String> {FIELD_NAME} = new ArrayList<>();
                
                public void {METHOD_NAME}() {
                    for (int i = 0; i < {RANDOM_INT}; i++) {
                        {FIELD_NAME}.add("item" + i);
                    }
                    
                    {FIELD_NAME}.stream()
                            .filter(s -> s.length() > 3)
                            .forEach(System.out::println);
                }
            }
            """,
            
            """
            public class {CLASS_NAME}<T> {
                private final Map<String, T> cache = new HashMap<>();
                
                public Optional<T> {METHOD_NAME}(String key) {
                    return Optional.ofNullable(cache.get(key));
                }
                
                public void put(String key, T value) {
                    cache.put(key, value);
                }
            }
            """,
            
            """
            public class {CLASS_NAME} {
                public void {METHOD_NAME}() {
                    try {
                        processData();
                    } catch (Exception e) {
                        handleError(e);
                    } finally {
                        cleanup();
                    }
                }
                
                private void processData() throws Exception {
                    // Processing logic
                }
                
                private void handleError(Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }
                
                private void cleanup() {
                    // Cleanup resources
                }
            }
            """
        ));
        
        // Complex templates
        CODE_TEMPLATES.put(BenchmarkConfiguration.ComplexityLevel.COMPLEX, Arrays.asList(
            """
            public class {CLASS_NAME} {
                private final ExecutorService executor = Executors.newCachedThreadPool();
                private final ConcurrentMap<String, CompletableFuture<String>> futures = new ConcurrentHashMap<>();
                
                public CompletableFuture<String> {METHOD_NAME}(String input) {
                    return futures.computeIfAbsent(input, key -> 
                        CompletableFuture.supplyAsync(() -> processAsync(key), executor)
                                .exceptionally(throwable -> {
                                    System.err.println("Async processing failed: " + throwable.getMessage());
                                    return "default";
                                })
                                .thenCompose(result -> 
                                    CompletableFuture.supplyAsync(() -> transformResult(result), executor))
                    );
                }
                
                private String processAsync(String input) {
                    return IntStream.range(0, input.length())
                            .parallel()
                            .mapToObj(i -> String.valueOf(input.charAt(i)).toUpperCase())
                            .collect(Collectors.joining("-"));
                }
                
                private String transformResult(String input) {
                    return Optional.ofNullable(input)
                            .filter(s -> !s.isEmpty())
                            .map(s -> s + "_processed")
                            .orElse("empty_result");
                }
            }
            """,
            
            """
            public class {CLASS_NAME} implements AutoCloseable {
                private final BlockingQueue<Task> taskQueue = new LinkedBlockingQueue<>();
                private final List<Thread> workers = new ArrayList<>();
                private volatile boolean shutdown = false;
                
                public {CLASS_NAME}(int workerCount) {
                    for (int i = 0; i < workerCount; i++) {
                        Thread worker = new Thread(this::workerLoop, "Worker-" + i);
                        workers.add(worker);
                        worker.start();
                    }
                }
                
                public <T> Future<T> submit(Supplier<T> task) {
                    CompletableFuture<T> future = new CompletableFuture<>();
                    taskQueue.offer(new Task<>(task, future));
                    return future;
                }
                
                private void workerLoop() {
                    while (!shutdown) {
                        try {
                            Task task = taskQueue.poll(100, TimeUnit.MILLISECONDS);
                            if (task != null) {
                                task.execute();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
                
                @Override
                public void close() {
                    shutdown = true;
                    workers.forEach(Thread::interrupt);
                    workers.forEach(thread -> {
                        try {
                            thread.join(1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                }
                
                private static class Task<T> {
                    private final Supplier<T> supplier;
                    private final CompletableFuture<T> future;
                    
                    Task(Supplier<T> supplier, CompletableFuture<T> future) {
                        this.supplier = supplier;
                        this.future = future;
                    }
                    
                    void execute() {
                        try {
                            T result = supplier.get();
                            future.complete(result);
                        } catch (Exception e) {
                            future.completeExceptionally(e);
                        }
                    }
                }
            }
            """
        ));
    }
    
    /**
     * Generate specific test patterns for targeted testing.
     */
    public List<String> generateSpecificPatterns(String... patterns) {
        List<String> testCases = new ArrayList<>();
        
        for (String pattern : patterns) {
            switch (pattern) {
                case "lambda_expressions":
                    testCases.add(generateLambdaTestCase());
                    break;
                case "stream_operations":
                    testCases.add(generateStreamTestCase());
                    break;
                case "generic_types":
                    testCases.add(generateGenericTestCase());
                    break;
                case "annotation_processing":
                    testCases.add(generateAnnotationTestCase());
                    break;
                default:
                    // Generate from templates
                    testCases.addAll(generateTestCases(BenchmarkConfiguration.ComplexityLevel.MODERATE, 1));
            }
        }
        
        return testCases;
    }
    
    private String generateLambdaTestCase() {
        return """
            public class LambdaTest {
                public void testLambdas() {
                    List<String> items = Arrays.asList("a", "bb", "ccc");
                    
                    items.stream()
                         .filter(s -> s.length() > 1)
                         .map(s -> s.toUpperCase())
                         .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                         .forEach(System.out::println);
                }
            }
            """;
    }
    
    private String generateStreamTestCase() {
        return """
            public class StreamTest {
                public void testStreams() {
                    IntStream.range(1, 100)
                            .parallel()
                            .filter(n -> n % 2 == 0)
                            .mapToObj(n -> "Number: " + n)
                            .collect(Collectors.groupingBy(
                                s -> s.length(),
                                Collectors.counting()
                            ));
                }
            }
            """;
    }
    
    private String generateGenericTestCase() {
        return """
            public class GenericTest<T extends Comparable<T>> {
                private final List<T> items = new ArrayList<>();
                
                public <U extends T> void addItem(U item) {
                    items.add(item);
                }
                
                public Optional<T> findMax() {
                    return items.stream().max(T::compareTo);
                }
            }
            """;
    }
    
    private String generateAnnotationTestCase() {
        return """
            @Component
            @Service("testService")
            public class AnnotationTest {
                @Autowired
                private DataService dataService;
                
                @PostConstruct
                public void init() {
                    System.out.println("Initializing service");
                }
                
                @Transactional
                @Cacheable("testCache")
                public String processData(@NonNull String input) {
                    return dataService.process(input);
                }
            }
            """;
    }
    // ==================== Compatibility Methods ====================
    
    /**
     * Generate simple expressions for compatibility with legacy tests.
     * Maps to SIMPLE complexity level.
     */
    public List<String> generateSimpleExpressions(int count) {
        return generateTestCases(BenchmarkConfiguration.ComplexityLevel.SIMPLE, count);
    }
    
    /**
     * Generate medium complexity code for compatibility with legacy tests.
     * Maps to MODERATE complexity level.
     */
    public List<String> generateMediumComplexityCode(int count) {
        return generateTestCases(BenchmarkConfiguration.ComplexityLevel.MODERATE, count);
    }
    
    /**
     * Generate complex nested structures for compatibility with legacy tests.
     * Maps to COMPLEX complexity level.
     */
    public List<String> generateComplexNestedStructures(int count) {
        return generateTestCases(BenchmarkConfiguration.ComplexityLevel.COMPLEX, count);
    }
    
    /**
     * Generate large class files for compatibility with legacy tests.
     * Maps to COMPLEX complexity level with larger templates.
     */
    public List<String> generateLargeClassFiles(int count) {
        return generateTestCases(BenchmarkConfiguration.ComplexityLevel.COMPLEX, count);
    }
    
    /**
     * Generate complexity samples for compatibility with legacy tests.
     * Maps to MODERATE complexity level.
     */
    public List<String> generateComplexitySamples(int count) {
        return generateTestCases(BenchmarkConfiguration.ComplexityLevel.MODERATE, count);
    }
}