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
package bluej.parser.pratt.integration.e2e;

import bluej.parser.pratt.integration.benchmark.adapters.CommonParsingScenariosAdapter;
import bluej.parser.pratt.integration.benchmark.BenchmarkConfiguration;
import bluej.parser.pratt.integration.benchmark.metrics.BenchmarkResult;
import bluej.parser.pratt.integration.benchmark.corpus.TestCorpusItem;
import bluej.parser.pratt.integration.CallbackTestingUtility;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import javax.swing.text.*;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.*;

/**
 * Comprehensive E2E integration test for Common Parsing Scenarios strategy.
 * 
 * This test validates the CommonParsingScenariosAdapter's integration with the BlueJ framework,
 * focusing on SafeCallbacks implementation, comprehensive real-world scenario coverage,
 * and standard parsing workflows typical in BlueJ environments.
 * 
 * Key areas tested:
 * - SafeCallbacks with AutoCloseable scope management and guaranteed cleanup
 * - Comprehensive scenario coverage (imports, classes, methods, expressions, control flow, variables)  
 * - Standard workflow testing for typical BlueJ parsing patterns
 * - Callback pairing validation and balance checking
 * - Integration with TestableDocument and EntityResolver infrastructure
 * - Performance characteristics for optimized common patterns
 * 
 * Follows KotlinEditorParser E2E testing patterns with TestableDocument setup
 * and CallbackTestingUtility validation infrastructure.
 */
public class CommonParsingScenariosIntegrationTest {

    private CommonParsingScenariosAdapter adapter;
    private CallbackTestingUtility.CallbackTester callbackTester;
    private TestableDocument testableDocument;
    private MockEntityResolver mockEntityResolver;
    private BenchmarkConfiguration defaultConfig;

    @Before
    public void setUp() {
        // Initialize common parsing scenarios adapter
        adapter = new CommonParsingScenariosAdapter();
        CallbackTestingUtility callbackUtility = new CallbackTestingUtility();
        callbackTester = CallbackTestingUtility.forDelegate(callbackUtility);
        
        // Setup mock infrastructure following KotlinEditorParser pattern
        mockEntityResolver = new MockEntityResolver();
        testableDocument = new TestableDocument(mockEntityResolver);
        
        // Configure default benchmark settings for scenario testing
        defaultConfig = new BenchmarkConfiguration.Builder()
                .warmupIterations(2)
                .measurementIterations(5)
                .enableMemoryProfiling(true)
                .enableDetailedMetrics(true)
                .build();
        
        // Reset callback state for clean test execution
        callbackTester.reset();
    }

    @After
    public void tearDown() {
        if (adapter != null) {
            adapter.cleanup();
        }
        if (callbackTester != null) {
            callbackTester.validateCallbackBalance();
        }
        
        // Clean up mock infrastructure
        testableDocument = null;
        mockEntityResolver = null;
    }

    // ==================== SafeCallbacks Integration Tests ====================

    @Test
    public void testSafeCallbacksAutoCloseableScopes() throws Exception {
        // Test comprehensive Java class with multiple scenarios
        String javaCode = "package com.bluej.examples;\n" +
            "\n" +
            "import java.util.*;\n" +
            "import java.io.File;\n" +
            "import static java.lang.Math.PI;\n" +
            "\n" +
            "/**\n" +
            " * Example class demonstrating common parsing scenarios.\n" +
            " */\n" +
            "public class ExampleClass extends BaseClass implements Runnable {\n" +
            "    private String name;\n" +
            "    private int count = 0;\n" +
            "    private List<String> items = new ArrayList<>();\n" +
            "    \n" +
            "    public ExampleClass(String name) {\n" +
            "        this.name = name;\n" +
            "        this.count = name.length();\n" +
            "    }\n" +
            "    \n" +
            "    @Override\n" +
            "    public void run() {\n" +
            "        System.out.println(\"Running: \" + name);\n" +
            "    }\n" +
            "    \n" +
            "    public void processItems() {\n" +
            "        for (String item : items) {\n" +
            "            if (item != null && !item.isEmpty()) {\n" +
            "                try {\n" +
            "                    processItem(item);\n" +
            "                } catch (Exception e) {\n" +
            "                    System.err.println(\"Error: \" + e.getMessage());\n" +
            "                } finally {\n" +
            "                    count++;\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "    \n" +
            "    private void processItem(String item) throws Exception {\n" +
            "        if (item == null) {\n" +
            "            throw new IllegalArgumentException(\"Item cannot be null\");\n" +
            "        }\n" +
            "        // Process the item\n" +
            "    }\n" +
            "}";

        TestCorpusItem corpus = new TestCorpusItem(javaCode,
                                                  BenchmarkConfiguration.ComplexityLevel.MODERATE,
                                                  "SafeCallbacks Test");
        
        // Execute with SafeCallbacks infrastructure
        BenchmarkResult result = adapter.executeBenchmark(corpus, defaultConfig);
        
        // Validate SafeCallbacks behavior
        assertNotNull("Benchmark result should not be null", result);
        assertTrue("SafeCallbacks should maintain callback balance", result.isCallbacksBalanced());
        assertTrue("SafeCallbacks should generate callbacks", result.getCallbackCount() > 0);
        
        // Validate scenario completion
        assertTrue("SafeCallbacks should complete execution", result.getExecutionTime() > 0);
        assertNotNull("SafeCallbacks should provide memory metrics", result.getMemoryMetrics());
        
        // Validate AutoCloseable cleanup behavior
        callbackTester.validateCallbackSequence();
        callbackTester.validateCallbackBalance();
    }

    @Test
    public void testSafeCallbacksErrorHandling() throws Exception {
        // Test malformed code to verify SafeCallbacks error handling
        String malformedCode = "package com.bluej.examples\n" +
            "\n" +
            "import java.util.*\n" +
            "\n" +
            "public class BrokenClass {\n" +
            "    private String name\n" +
            "    \n" +
            "    public BrokenClass(String name {\n" +
            "        this.name = name\n" +
            "    \n" +
            "    public void method() {\n" +
            "        if (name != null {\n" +
            "            System.out.println(name);\n" +
            "        }\n" +
            "    }\n";

        TestCorpusItem corpus = new TestCorpusItem(malformedCode,
                                                  BenchmarkConfiguration.ComplexityLevel.SIMPLE,
                                                  "SafeCallbacks Error Test");
        
        // Execute and validate SafeCallbacks error recovery
        BenchmarkResult result = adapter.executeBenchmark(corpus, defaultConfig);
        
        assertNotNull("SafeCallbacks should handle errors gracefully", result);
        
        // SafeCallbacks should maintain balance even with errors
        // Note: Common scenarios have limited error recovery, but SafeCallbacks should cleanup
        callbackTester.validateCallbackBalance();
    }

    @Test
    public void testSafeCallbacksConcurrentAccess() throws Exception {
        String concurrentCode = "public class ConcurrentExample {\n" +
            "    private volatile boolean running = true;\n" +
            "    \n" +
            "    public void startThreads() {\n" +
            "        Thread t1 = new Thread(() -> processData(\"thread1\"));\n" +
            "        Thread t2 = new Thread(() -> processData(\"thread2\"));\n" +
            "        t1.start();\n" +
            "        t2.start();\n" +
            "    }\n" +
            "    \n" +
            "    private void processData(String threadName) {\n" +
            "        while (running) {\n" +
            "            System.out.println(threadName + \" processing...\");\n" +
            "        }\n" +
            "    }\n" +
            "}";

        TestCorpusItem corpus = new TestCorpusItem(concurrentCode,
                                                  BenchmarkConfiguration.ComplexityLevel.MODERATE,
                                                  "Concurrent SafeCallbacks Test");
        
        // Test concurrent execution with SafeCallbacks
        CompletableFuture<BenchmarkResult> future1 = CompletableFuture.supplyAsync(() -> 
            adapter.executeBenchmark(corpus, defaultConfig));
        CompletableFuture<BenchmarkResult> future2 = CompletableFuture.supplyAsync(() -> 
            adapter.executeBenchmark(corpus, defaultConfig));
        
        BenchmarkResult result1 = future1.get(5, TimeUnit.SECONDS);
        BenchmarkResult result2 = future2.get(5, TimeUnit.SECONDS);
        
        assertNotNull("Concurrent SafeCallbacks execution 1 should succeed", result1);
        assertNotNull("Concurrent SafeCallbacks execution 2 should succeed", result2);
        assertTrue("Concurrent SafeCallbacks should maintain balance", result1.isCallbacksBalanced());
        assertTrue("Concurrent SafeCallbacks should maintain balance", result2.isCallbacksBalanced());
    }

    // ==================== Comprehensive Scenario Coverage Tests ====================

    @Test
    public void testImportsScenario() throws Exception {
        testIndividualScenario("IMPORTS");
    }

    @Test
    public void testClassesScenario() throws Exception {
        testIndividualScenario("CLASSES");
    }

    @Test
    public void testMethodsScenario() throws Exception {
        testIndividualScenario("METHODS");
    }

    @Test
    public void testExpressionsScenario() throws Exception {
        testIndividualScenario("EXPRESSIONS");
    }

    @Test
    public void testControlFlowScenario() throws Exception {
        testIndividualScenario("CONTROL_FLOW");
    }

    @Test
    public void testVariablesScenario() throws Exception {
        testIndividualScenario("VARIABLES");
    }

    private void testIndividualScenario(String scenarioType) throws Exception {
        String code = generateScenarioSpecificCode(scenarioType);
        
        TestCorpusItem corpus = new TestCorpusItem(code,
                                                  BenchmarkConfiguration.ComplexityLevel.MODERATE,
                                                  scenarioType + " Scenario Test");
        
        BenchmarkResult result = adapter.executeBenchmark(corpus, defaultConfig);
        
        assertNotNull(scenarioType + " scenario should execute successfully", result);
        assertTrue(scenarioType + " should generate callbacks", result.getCallbackCount() > 0);
        assertTrue(scenarioType + " callbacks should be balanced", result.isCallbacksBalanced());
        
        // Validate scenario-specific characteristics
        validateScenarioCharacteristics(result, scenarioType);
    }

    @Test
    public void testCombinedScenariosComprehensive() throws Exception {
        String comprehensiveCode = "// IMPORTS scenario\n" +
            "package com.bluej.comprehensive;\n" +
            "import java.util.*;\n" +
            "import java.io.*;\n" +
            "import java.util.concurrent.Future;\n" +
            "import static java.lang.Math.*;\n" +
            "\n" +
            "// CLASSES scenario\n" +
            "@SuppressWarnings(\"unchecked\")\n" +
            "public class ComprehensiveExample implements Comparable<ComprehensiveExample> {\n" +
            "    // VARIABLES scenario\n" +
            "    private final String name;\n" +
            "    private int value = 0;\n" +
            "    private List<String> data = new ArrayList<>();\n" +
            "    private Map<String, Object> properties = new HashMap<>();\n" +
            "    \n" +
            "    // METHODS scenario\n" +
            "    public ComprehensiveExample(String name, int value) {\n" +
            "        this.name = name;\n" +
            "        this.value = value;\n" +
            "    }\n" +
            "    \n" +
            "    public void processData() {\n" +
            "        // CONTROL_FLOW scenario\n" +
            "        if (data != null && !data.isEmpty()) {\n" +
            "            for (String item : data) {\n" +
            "                try {\n" +
            "                    // EXPRESSIONS scenario\n" +
            "                    int result = value + item.length() * 2;\n" +
            "                    boolean isValid = result > 0 && result < 1000;\n" +
            "                    \n" +
            "                    if (isValid) {\n" +
            "                        properties.put(item, result);\n" +
            "                    } else {\n" +
            "                        System.err.println(\"Invalid result: \" + result);\n" +
            "                    }\n" +
            "                } catch (Exception e) {\n" +
            "                    e.printStackTrace();\n" +
            "                } finally {\n" +
            "                    value++;\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "        \n" +
            "        // Additional control flow\n" +
            "        while (value < 100) {\n" +
            "            value += random() * 10;\n" +
            "        }\n" +
            "        \n" +
            "        do {\n" +
            "            value--;\n" +
            "        } while (value > 50);\n" +
            "    }\n" +
            "    \n" +
            "    @Override\n" +
            "    public int compareTo(ComprehensiveExample other) {\n" +
            "        return Integer.compare(this.value, other.value);\n" +
            "    }\n" +
            "}";

        TestCorpusItem corpus = new TestCorpusItem(comprehensiveCode,
                                                  BenchmarkConfiguration.ComplexityLevel.MODERATE,
                                                  "Comprehensive Scenarios Test");
        
        BenchmarkResult result = adapter.executeBenchmark(corpus, defaultConfig);
        
        // Validate comprehensive scenario execution
        assertNotNull("Comprehensive scenarios should execute successfully", result);
        assertTrue("Comprehensive scenarios should generate many callbacks", result.getCallbackCount() >= 50);
        assertTrue("Comprehensive scenarios should maintain callback balance", result.isCallbacksBalanced());
        assertTrue("Comprehensive scenarios should complete execution", result.getExecutionTime() > 0);
        
        // Validate performance characteristics for comprehensive scenarios
        assertNotNull("Should provide performance metrics", result.getPerformanceMetrics());
        assertTrue("Should have positive throughput", result.getPerformanceMetrics().getThroughput() > 0);
        assertTrue("Should have reasonable consistency", result.getPerformanceMetrics().getConsistency() > 0.5);
        
        callbackTester.validateCallbackSequence();
    }

    // ==================== Standard Workflow Tests ====================

    @Test
    public void testStandardBlueJWorkflow() throws Exception {
        // Simulate standard BlueJ project structure parsing
        List<String> projectFiles = Arrays.asList(
            // Main application class
            "package bluej.project;\n" +
            "\n" +
            "import java.awt.*;\n" +
            "import javax.swing.*;\n" +
            "import java.util.*;\n" +
            "\n" +
            "public class BlueJApplication extends JFrame {\n" +
            "    private static final String VERSION = \"1.0\";\n" +
            "    private List<Project> projects = new ArrayList<>();\n" +
            "    \n" +
            "    public BlueJApplication() {\n" +
            "        super(\"BlueJ IDE\");\n" +
            "        initializeUI();\n" +
            "    }\n" +
            "    \n" +
            "    private void initializeUI() {\n" +
            "        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);\n" +
            "        setSize(800, 600);\n" +
            "        setLayout(new BorderLayout());\n" +
            "    }\n" +
            "    \n" +
            "    public static void main(String[] args) {\n" +
            "        SwingUtilities.invokeLater(() -> {\n" +
            "            new BlueJApplication().setVisible(true);\n" +
            "        });\n" +
            "    }\n" +
            "}",
            
            // Project model class
            "package bluej.project;\n" +
            "\n" +
            "import java.io.*;\n" +
            "import java.util.*;\n" +
            "\n" +
            "public class Project {\n" +
            "    private String name;\n" +
            "    private File projectDirectory;\n" +
            "    private List<JavaClass> classes = new ArrayList<>();\n" +
            "    \n" +
            "    public Project(String name, File directory) {\n" +
            "        this.name = name;\n" +
            "        this.projectDirectory = directory;\n" +
            "        loadClasses();\n" +
            "    }\n" +
            "    \n" +
            "    private void loadClasses() {\n" +
            "        File[] javaFiles = projectDirectory.listFiles((dir, name) -> \n" +
            "            name.endsWith(\".java\"));\n" +
            "        \n" +
            "        if (javaFiles != null) {\n" +
            "            for (File file : javaFiles) {\n" +
            "                try {\n" +
            "                    JavaClass javaClass = new JavaClass(file);\n" +
            "                    classes.add(javaClass);\n" +
            "                } catch (IOException e) {\n" +
            "                    System.err.println(\"Failed to load: \" + file.getName());\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}",
            
            // Utility class
            "package bluej.util;\n" +
            "\n" +
            "import java.io.*;\n" +
            "import java.nio.file.*;\n" +
            "\n" +
            "public class FileUtils {\n" +
            "    private FileUtils() {\n" +
            "        // Utility class\n" +
            "    }\n" +
            "    \n" +
            "    public static String readFile(Path path) throws IOException {\n" +
            "        return Files.readString(path);\n" +
            "    }\n" +
            "    \n" +
            "    public static void writeFile(Path path, String content) throws IOException {\n" +
            "        Files.writeString(path, content, StandardOpenOption.CREATE, \n" +
            "                        StandardOpenOption.TRUNCATE_EXISTING);\n" +
            "    }\n" +
            "    \n" +
            "    public static boolean isJavaFile(Path path) {\n" +
            "        return path.toString().toLowerCase().endsWith(\".java\");\n" +
            "    }\n" +
            "}"
        );

        // Combine all project files into a single string
        String combinedCode = String.join("\n\n", projectFiles);
        TestCorpusItem corpus = new TestCorpusItem(combinedCode,
                                                  BenchmarkConfiguration.ComplexityLevel.MODERATE,
                                                  "Standard BlueJ Workflow");
        
        // Execute standard workflow
        BenchmarkResult result = adapter.executeBenchmark(corpus, defaultConfig);
        
        // Validate standard workflow execution
        assertNotNull("Standard workflow should execute successfully", result);
        assertTrue("Standard workflow should generate substantial callbacks", result.getCallbackCount() >= 80);
        assertTrue("Standard workflow callbacks should be balanced", result.isCallbacksBalanced());
        assertTrue("Standard workflow should complete execution", result.getExecutionTime() > 0);
        
        // Validate workflow performance characteristics
        assertNotNull("Should provide performance metrics", result.getPerformanceMetrics());
        assertTrue("Standard workflow should have high consistency", 
                  result.getPerformanceMetrics().getConsistency() > 0.8);
        assertTrue("Standard workflow should have good throughput", 
                  result.getPerformanceMetrics().getThroughput() > 100);
        
        // Validate memory efficiency for standard patterns
        assertNotNull("Should provide memory metrics", result.getMemoryMetrics());
        assertTrue("Standard workflow should have low memory retention", 
                  result.getMemoryMetrics().getRetentionRate() < 0.5);
        
        callbackTester.validateCallbackSequence();
        callbackTester.validateCallbackBalance();
    }

    @Test
    public void testSimpleClassWorkflow() throws Exception {
        testCommonClassPatternWorkflow("SimpleClass", "Simple class with basic structure", 15);
    }

    @Test
    public void testComplexClassWorkflow() throws Exception {
        testCommonClassPatternWorkflow("ComplexClass", "Complex class with multiple features", 40);
    }

    @Test
    public void testInterfaceClassWorkflow() throws Exception {
        testCommonClassPatternWorkflow("InterfaceClass", "Interface with multiple methods", 20);
    }

    @Test
    public void testEnumClassWorkflow() throws Exception {
        testCommonClassPatternWorkflow("EnumClass", "Enum with values and methods", 25);
    }

    private void testCommonClassPatternWorkflow(String className, String description, int expectedMinCallbacks) throws Exception {
        String classCode = generateClassPatternCode(className);
        
        TestCorpusItem corpus = new TestCorpusItem(classCode,
                                                  BenchmarkConfiguration.ComplexityLevel.MODERATE,
                                                  className + " Workflow");
        
        BenchmarkResult result = adapter.executeBenchmark(corpus, defaultConfig);
        
        assertNotNull(className + " workflow should execute successfully", result);
        assertTrue(className + " should generate at least " + expectedMinCallbacks + " callbacks", 
                  result.getCallbackCount() >= expectedMinCallbacks);
        assertTrue(className + " callbacks should be balanced", result.isCallbacksBalanced());
        
        callbackTester.validateCallbackBalance();
    }

    // ==================== Performance and Integration Tests ====================

    @Test
    public void testPerformanceCharacteristics() throws Exception {
        // Test performance with typical BlueJ patterns
        String performanceCode = "package bluej.performance;\n" +
            "\n" +
            "import java.util.*;\n" +
            "import java.util.stream.*;\n" +
            "import java.util.concurrent.*;\n" +
            "\n" +
            "public class PerformanceTestClass {\n" +
            "    private final Map<String, List<Integer>> dataMap = new ConcurrentHashMap<>();\n" +
            "    private final ExecutorService executor = Executors.newFixedThreadPool(4);\n" +
            "    \n" +
            "    public void performanceMethod() {\n" +
            "        IntStream.range(0, 1000)\n" +
            "                .parallel()\n" +
            "                .forEach(i -> {\n" +
            "                    String key = \"key\" + (i % 10);\n" +
            "                    dataMap.computeIfAbsent(key, k -> new ArrayList<>()).add(i);\n" +
            "                });\n" +
            "        \n" +
            "        dataMap.entrySet().stream()\n" +
            "              .sorted(Map.Entry.comparingByKey())\n" +
            "              .forEach(entry -> {\n" +
            "                  String key = entry.getKey();\n" +
            "                  List<Integer> values = entry.getValue();\n" +
            "                  \n" +
            "                  double average = values.stream()\n" +
            "                                        .mapToInt(Integer::intValue)\n" +
            "                                        .average()\n" +
            "                                        .orElse(0.0);\n" +
            "                  \n" +
            "                  System.out.println(key + \": \" + average);\n" +
            "              });\n" +
            "    }\n" +
            "    \n" +
            "    public CompletableFuture<String> asyncMethod(String input) {\n" +
            "        return CompletableFuture.supplyAsync(() -> {\n" +
            "            try {\n" +
            "                Thread.sleep(10);\n" +
            "                return input.toUpperCase();\n" +
            "            } catch (InterruptedException e) {\n" +
            "                Thread.currentThread().interrupt();\n" +
            "                return input;\n" +
            "            }\n" +
            "        }, executor);\n" +
            "    }\n" +
            "}";

        TestCorpusItem corpus = new TestCorpusItem(performanceCode,
                                                  BenchmarkConfiguration.ComplexityLevel.MODERATE,
                                                  "Performance Test");
        
        // Execute multiple times to measure consistency
        List<BenchmarkResult> results = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            BenchmarkResult result = adapter.executeBenchmark(corpus, defaultConfig);
            results.add(result);
        }
        
        // Validate performance characteristics
        for (BenchmarkResult result : results) {
            assertNotNull("Performance test should execute successfully", result);
            assertTrue("Performance test callbacks should be balanced", result.isCallbacksBalanced());
            assertTrue("Performance test should generate substantial callbacks", result.getCallbackCount() >= 30);
        }
        
        // Analyze performance consistency
        double[] executionTimes = results.stream().mapToDouble(BenchmarkResult::getExecutionTime).toArray();
        double avgTime = Arrays.stream(executionTimes).average().orElse(0.0);
        double maxTime = Arrays.stream(executionTimes).max().orElse(0.0);
        double minTime = Arrays.stream(executionTimes).min().orElse(0.0);
        
        assertTrue("Average execution time should be positive", avgTime > 0);
        assertTrue("Execution time variance should be reasonable", (maxTime - minTime) / avgTime < 0.5);
        
        callbackTester.validateCallbackBalance();
    }

    @Test
    public void testTestableDocumentIntegration() throws Exception {
        // Test integration with TestableDocument and EntityResolver
        String integrationCode = "package bluej.integration;\n" +
            "\n" +
            "import bluej.editor.*;\n" +
            "import bluej.parser.*;\n" +
            "\n" +
            "public class IntegrationTestClass {\n" +
            "    private EditorDocument document;\n" +
            "    private ParserCallback callback;\n" +
            "    \n" +
            "    public void integrationMethod() {\n" +
            "        if (document != null) {\n" +
            "            try {\n" +
            "                String content = document.getContent();\n" +
            "                if (callback != null) {\n" +
            "                    callback.parseComplete();\n" +
            "                }\n" +
            "            } catch (Exception e) {\n" +
            "                if (callback != null) {\n" +
            "                    callback.parseError(e);\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}";

        // Setup TestableDocument with EntityResolver
        testableDocument.insertString(0, integrationCode, null);
        mockEntityResolver.addEntity("bluej.editor.EditorDocument", "Mock editor document");
        mockEntityResolver.addEntity("bluej.parser.ParserCallback", "Mock parser callback");

        TestCorpusItem corpus = new TestCorpusItem(integrationCode,
                                                  BenchmarkConfiguration.ComplexityLevel.MODERATE,
                                                  "TestableDocument Integration");
        
        BenchmarkResult result = adapter.executeBenchmark(corpus, defaultConfig);
        
        // Validate TestableDocument integration
        assertNotNull("TestableDocument integration should execute successfully", result);
        assertTrue("TestableDocument integration callbacks should be balanced", result.isCallbacksBalanced());
        assertTrue("TestableDocument integration should generate callbacks", result.getCallbackCount() > 0);
        
        // Validate EntityResolver interaction
        assertTrue("EntityResolver should have resolved entities", mockEntityResolver.hasResolvedEntities());
        assertEquals("Should resolve expected number of entities", 2, mockEntityResolver.getResolvedEntityCount());
        
        callbackTester.validateCallbackSequence();
    }

    // ==================== Helper Methods ====================

    private String generateScenarioSpecificCode(String scenarioType) {
        switch (scenarioType) {
            case "IMPORTS":
                return "package com.bluej.imports;\n" +
                    "import java.util.*;\n" +
                    "import java.io.File;\n" +
                    "import java.util.concurrent.Future;\n" +
                    "import static java.lang.Math.PI;\n" +
                    "import javax.swing.JFrame;\n" +
                    "\n" +
                    "public class ImportExample {\n" +
                    "    // Simple class for import testing\n" +
                    "}";
            case "CLASSES":
                return "@SuppressWarnings(\"unused\")\n" +
                    "public class ClassExample extends BaseClass implements Runnable, Comparable<ClassExample> {\n" +
                    "    @Deprecated\n" +
                    "    private String field1;\n" +
                    "    protected int field2 = 42;\n" +
                    "    public static final double PI = 3.14159;\n" +
                    "    \n" +
                    "    public ClassExample() {\n" +
                    "        super();\n" +
                    "    }\n" +
                    "    \n" +
                    "    public ClassExample(String field1) {\n" +
                    "        this.field1 = field1;\n" +
                    "    }\n" +
                    "    \n" +
                    "    @Override\n" +
                    "    public void run() {}\n" +
                    "    \n" +
                    "    @Override\n" +
                    "    public int compareTo(ClassExample other) { return 0; }\n" +
                    "}";
            case "METHODS":
                return "public class MethodExample {\n" +
                    "    public void simpleMethod() {}\n" +
                    "    \n" +
                    "    public int methodWithParams(String param1, int param2, boolean param3) {\n" +
                    "        return param2;\n" +
                    "    }\n" +
                    "    \n" +
                    "    private static <T> List<T> genericMethod(T... items) {\n" +
                    "        return Arrays.asList(items);\n" +
                    "    }\n" +
                    "    \n" +
                    "    protected synchronized final void complexMethod() throws Exception {\n" +
                    "        // Method body with statements\n" +
                    "        int a = 1;\n" +
                    "        String b = \"test\";\n" +
                    "        System.out.println(a + b);\n" +
                    "    }\n" +
                    "}";
            case "EXPRESSIONS":
                return "public class ExpressionExample {\n" +
                    "    public void testExpressions() {\n" +
                    "        int arithmetic = 5 + 3 * 2 - 1 / 2;\n" +
                    "        boolean comparison = arithmetic > 10 && arithmetic < 20;\n" +
                    "        boolean logical = comparison || !comparison;\n" +
                    "        \n" +
                    "        String[] array = {\"a\", \"b\", \"c\"};\n" +
                    "        String element = array[0];\n" +
                    "        int length = element.length();\n" +
                    "        \n" +
                    "        Object obj = new Object();\n" +
                    "        String result = obj.toString().substring(0, 5);\n" +
                    "        \n" +
                    "        int conditional = comparison ? 1 : 0;\n" +
                    "        \n" +
                    "        Runnable lambda = () -> System.out.println(\"Lambda\");\n" +
                    "    }\n" +
                    "}";
            case "CONTROL_FLOW":
                return "public class ControlFlowExample {\n" +
                    "    public void testControlFlow() {\n" +
                    "        // If-else\n" +
                    "        if (Math.random() > 0.5) {\n" +
                    "            System.out.println(\"Greater\");\n" +
                    "        } else if (Math.random() > 0.25) {\n" +
                    "            System.out.println(\"Middle\");\n" +
                    "        } else {\n" +
                    "            System.out.println(\"Lower\");\n" +
                    "        }\n" +
                    "        \n" +
                    "        // For loop\n" +
                    "        for (int i = 0; i < 10; i++) {\n" +
                    "            System.out.println(i);\n" +
                    "        }\n" +
                    "        \n" +
                    "        // Enhanced for loop\n" +
                    "        String[] items = {\"a\", \"b\", \"c\"};\n" +
                    "        for (String item : items) {\n" +
                    "            System.out.println(item);\n" +
                    "        }\n" +
                    "        \n" +
                    "        // While loop\n" +
                    "        int count = 0;\n" +
                    "        while (count < 5) {\n" +
                    "            count++;\n" +
                    "        }\n" +
                    "        \n" +
                    "        // Do-while loop\n" +
                    "        do {\n" +
                    "            count--;\n" +
                    "        } while (count > 0);\n" +
                    "        \n" +
                    "        // Try-catch-finally\n" +
                    "        try {\n" +
                    "            Integer.parseInt(\"abc\");\n" +
                    "        } catch (NumberFormatException e) {\n" +
                    "            e.printStackTrace();\n" +
                    "        } catch (Exception e) {\n" +
                    "            System.err.println(\"Other error\");\n" +
                    "        } finally {\n" +
                    "            System.out.println(\"Cleanup\");\n" +
                    "        }\n" +
                    "    }\n" +
                    "}";
            case "VARIABLES":
                return "public class VariableExample {\n" +
                    "    // Field declarations\n" +
                    "    private int intField = 42;\n" +
                    "    private String stringField = \"Hello\";\n" +
                    "    private double doubleField = 3.14;\n" +
                    "    private boolean booleanField = true;\n" +
                    "    private Object objectField = new Object();\n" +
                    "    private List<String> listField = new ArrayList<>();\n" +
                    "    private Map<String, Integer> mapField = new HashMap<>();\n" +
                    "    \n" +
                    "    public void testVariables() {\n" +
                    "        // Local variable declarations\n" +
                    "        int localInt;\n" +
                    "        String localString = \"World\";\n" +
                    "        double localDouble = stringField.length() * 2.5;\n" +
                    "        boolean localBoolean = localDouble > 10.0;\n" +
                    "        \n" +
                    "        // Variable assignments\n" +
                    "        localInt = intField + 10;\n" +
                    "        localString = stringField + \" \" + localString;\n" +
                    "        localDouble *= 1.5;\n" +
                    "        localBoolean = !localBoolean;\n" +
                    "        \n" +
                    "        // Complex assignments\n" +
                    "        listField.add(localString);\n" +
                    "        mapField.put(localString, localInt);\n" +
                    "        objectField = mapField.get(localString);\n" +
                    "    }\n" +
                    "}";
            default:
                return "public class DefaultExample {}";
        }
    }

    private void validateScenarioCharacteristics(BenchmarkResult result, String scenarioType) {
        // Scenario-specific validation based on expected characteristics
        switch (scenarioType) {
            case "IMPORTS":
                assertTrue("IMPORTS should generate callbacks for package and imports", result.getCallbackCount() >= 8);
                break;
            case "CLASSES":
                assertTrue("CLASSES should generate callbacks for class structure", result.getCallbackCount() >= 15);
                break;
            case "METHODS":
                assertTrue("METHODS should generate callbacks for method declarations", result.getCallbackCount() >= 20);
                break;
            case "EXPRESSIONS":
                assertTrue("EXPRESSIONS should generate callbacks for various expressions", result.getCallbackCount() >= 25);
                break;
            case "CONTROL_FLOW":
                assertTrue("CONTROL_FLOW should generate callbacks for control structures", result.getCallbackCount() >= 30);
                break;
            case "VARIABLES":
                assertTrue("VARIABLES should generate callbacks for variable handling", result.getCallbackCount() >= 20);
                break;
        }
    }

    private String generateClassPatternCode(String pattern) {
        switch (pattern) {
            case "SimpleClass":
                return "public class SimpleClass {\n" +
                    "    private String name;\n" +
                    "    \n" +
                    "    public SimpleClass(String name) {\n" +
                    "        this.name = name;\n" +
                    "    }\n" +
                    "    \n" +
                    "    public String getName() {\n" +
                    "        return name;\n" +
                    "    }\n" +
                    "}";
            case "ComplexClass":
                return "public abstract class ComplexClass implements Runnable, Comparable<ComplexClass> {\n" +
                    "    protected final String id;\n" +
                    "    private volatile boolean active = true;\n" +
                    "    private static int instanceCount = 0;\n" +
                    "    \n" +
                    "    public ComplexClass(String id) {\n" +
                    "        this.id = id;\n" +
                    "        instanceCount++;\n" +
                    "    }\n" +
                    "    \n" +
                    "    public abstract void processData();\n" +
                    "    \n" +
                    "    @Override\n" +
                    "    public void run() {\n" +
                    "        while (active) {\n" +
                    "            processData();\n" +
                    "            Thread.yield();\n" +
                    "        }\n" +
                    "    }\n" +
                    "    \n" +
                    "    @Override\n" +
                    "    public int compareTo(ComplexClass other) {\n" +
                    "        return this.id.compareTo(other.id);\n" +
                    "    }\n" +
                    "    \n" +
                    "    public static int getInstanceCount() {\n" +
                    "        return instanceCount;\n" +
                    "    }\n" +
                    "}";
            case "InterfaceClass":
                return "public interface InterfaceClass {\n" +
                    "    String DEFAULT_VALUE = \"default\";\n" +
                    "    \n" +
                    "    void execute();\n" +
                    "    String getName();\n" +
                    "    void setName(String name);\n" +
                    "    \n" +
                    "    default boolean isValid() {\n" +
                    "        return getName() != null && !getName().isEmpty();\n" +
                    "    }\n" +
                    "}";
            case "EnumClass":
                return "public enum EnumClass {\n" +
                    "    FIRST(\"First Value\", 1),\n" +
                    "    SECOND(\"Second Value\", 2),\n" +
                    "    THIRD(\"Third Value\", 3);\n" +
                    "    \n" +
                    "    private final String displayName;\n" +
                    "    private final int value;\n" +
                    "    \n" +
                    "    EnumClass(String displayName, int value) {\n" +
                    "        this.displayName = displayName;\n" +
                    "        this.value = value;\n" +
                    "    }\n" +
                    "    \n" +
                    "    public String getDisplayName() {\n" +
                    "        return displayName;\n" +
                    "    }\n" +
                    "    \n" +
                    "    public int getValue() {\n" +
                    "        return value;\n" +
                    "    }\n" +
                    "    \n" +
                    "    public static EnumClass findByValue(int value) {\n" +
                    "        for (EnumClass e : values()) {\n" +
                    "            if (e.value == value) {\n" +
                    "                return e;\n" +
                    "            }\n" +
                    "        }\n" +
                    "        return null;\n" +
                    "    }\n" +
                    "}";
            default:
                return "public class DefaultClass {}";
        }
    }

    // ==================== Mock Infrastructure ====================

    /**
     * Mock EntityResolver for testing integration with BlueJ infrastructure.
     */
    private static class MockEntityResolver {
        private final Map<String, String> entities = new HashMap<>();
        private final Set<String> resolvedEntities = new HashSet<>();

        public void addEntity(String name, String description) {
            entities.put(name, description);
        }

        public String resolveEntity(String name) {
            resolvedEntities.add(name);
            return entities.get(name);
        }

        public boolean hasResolvedEntities() {
            return !resolvedEntities.isEmpty();
        }

        public int getResolvedEntityCount() {
            return resolvedEntities.size();
        }
    }

    /**
     * TestableDocument for integration testing following KotlinEditorParser pattern.
     */
    private static class TestableDocument extends PlainDocument {
        private final MockEntityResolver entityResolver;

        public TestableDocument(MockEntityResolver entityResolver) {
            this.entityResolver = entityResolver;
        }

        public MockEntityResolver getEntityResolver() {
            return entityResolver;
        }

        public String getTextContent() {
            try {
                return getText(0, getLength());
            } catch (BadLocationException e) {
                return "";
            }
        }
    }
}