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
import bluej.parser.pratt.integration.benchmark.adapters.CommonParsingScenariosAdapter;
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
 * JMH benchmark specifically focused on Common Parsing Scenarios integration strategy.
 * 
 * This benchmark measures the performance of parsing common, everyday Java code patterns
 * that represent the majority of parsing workload in typical development environments:
 * - POJO classes with fields, getters, and setters
 * - Service classes with business logic methods
 * - Controller classes with request handling patterns
 * - Utility classes with static methods
 * - Data transfer objects and value classes
 * - Common design patterns (Builder, Factory, Observer, etc.)
 * - Typical control flow structures (if/else, loops, try/catch)
 * - Standard Java idioms and conventions
 * 
 * This strategy is optimized for the most frequently encountered code patterns,
 * providing excellent performance for typical Java development workflows while
 * maintaining compatibility with standard language features.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-Xms2G", "-Xmx4G"})
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Category(BenchmarkTest.class)
public class CommonParsingScenariosJMHBenchmark {

    // Test infrastructure
    private TestCorpusGenerator corpusGenerator;
    private CommonParsingScenariosAdapter adapter;
    private TestableDocument document;
    private CallbackTester callbackTester;
    private static final Random random = new Random(42); // For reproducible mock values
    
    // Test corpus collections for common scenarios
    private List<String> pojoClassTestCases;
    private List<String> serviceClassTestCases;
    private List<String> controllerClassTestCases;
    private List<String> utilityClassTestCases;
    private List<String> dataTransferObjectTestCases;
    private List<String> builderPatternTestCases;
    private List<String> factoryPatternTestCases;
    private List<String> commonControlFlowTestCases;
    private List<String> standardJavaIdiomsTestCases;
    private List<String> businessLogicTestCases;

    static {
        InitConfig.init();
    }

    @Setup(Level.Trial)
    public void setUp() {
        corpusGenerator = new TestCorpusGenerator();
        adapter = new CommonParsingScenariosAdapter();
        
        MockEntityResolver entityResolver = new MockEntityResolver();
        document = new TestableDocument("benchmark.java", entityResolver);
        callbackTester = CallbackTestingUtility.forDelegate(document.getCallbackDelegate());
        
        // Generate common scenario test corpus
        pojoClassTestCases = generatePojoClassCases(40);
        serviceClassTestCases = generateServiceClassCases(35);
        controllerClassTestCases = generateControllerClassCases(30);
        utilityClassTestCases = generateUtilityClassCases(25);
        dataTransferObjectTestCases = generateDataTransferObjectCases(30);
        builderPatternTestCases = generateBuilderPatternCases(20);
        factoryPatternTestCases = generateFactoryPatternCases(20);
        commonControlFlowTestCases = generateCommonControlFlowCases(35);
        standardJavaIdiomsTestCases = generateStandardJavaIdiomsCases(40);
        businessLogicTestCases = generateBusinessLogicCases(30);
        
        System.out.println("Common Parsing Scenarios benchmark corpus initialized:");
        System.out.println("  POJO Classes: " + pojoClassTestCases.size());
        System.out.println("  Service Classes: " + serviceClassTestCases.size());
        System.out.println("  Controller Classes: " + controllerClassTestCases.size());
        System.out.println("  Utility Classes: " + utilityClassTestCases.size());
        System.out.println("  Data Transfer Objects: " + dataTransferObjectTestCases.size());
        System.out.println("  Builder Pattern: " + builderPatternTestCases.size());
        System.out.println("  Factory Pattern: " + factoryPatternTestCases.size());
        System.out.println("  Control Flow: " + commonControlFlowTestCases.size());
        System.out.println("  Java Idioms: " + standardJavaIdiomsTestCases.size());
        System.out.println("  Business Logic: " + businessLogicTestCases.size());
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
    // Common Class Pattern Benchmarks
    // =================================

    @Benchmark
    public void benchmarkPojoClasses(Blackhole bh) {
        benchmarkWithTestCases(pojoClassTestCases, bh);
    }

    @Benchmark
    public void benchmarkServiceClasses(Blackhole bh) {
        benchmarkWithTestCases(serviceClassTestCases, bh);
    }

    @Benchmark
    public void benchmarkControllerClasses(Blackhole bh) {
        benchmarkWithTestCases(controllerClassTestCases, bh);
    }

    @Benchmark
    public void benchmarkUtilityClasses(Blackhole bh) {
        benchmarkWithTestCases(utilityClassTestCases, bh);
    }

    @Benchmark
    public void benchmarkDataTransferObjects(Blackhole bh) {
        benchmarkWithTestCases(dataTransferObjectTestCases, bh);
    }

    // =================================
    // Design Pattern Benchmarks
    // =================================

    @Benchmark
    public void benchmarkBuilderPattern(Blackhole bh) {
        benchmarkWithTestCases(builderPatternTestCases, bh);
    }

    @Benchmark
    public void benchmarkFactoryPattern(Blackhole bh) {
        benchmarkWithTestCases(factoryPatternTestCases, bh);
    }

    // =================================
    // Common Code Structure Benchmarks
    // =================================

    @Benchmark
    public void benchmarkCommonControlFlow(Blackhole bh) {
        benchmarkWithTestCases(commonControlFlowTestCases, bh);
    }

    @Benchmark
    public void benchmarkStandardJavaIdioms(Blackhole bh) {
        benchmarkWithTestCases(standardJavaIdiomsTestCases, bh);
    }

    @Benchmark
    public void benchmarkBusinessLogic(Blackhole bh) {
        benchmarkWithTestCases(businessLogicTestCases, bh);
    }

    // =================================
    // Scenario-Specific Performance Benchmarks
    // =================================

    @Benchmark
    public void benchmarkFieldDeclarationPatterns(Blackhole bh) {
        // Test common field declaration patterns
        List<String> fieldPatterns = generateFieldDeclarationPatterns(25);
        for (String testCase : fieldPatterns) {
            document.setContent(testCase);
            callbackTester.clear();
            
            try {
                Object result = adapter.parseWithStrategy(testCase);
                
                // Measure field-specific parsing characteristics
                int fieldsDeclarationCount = random.nextInt(10) + 1; // Mock value
                boolean hasFieldInitializers = random.nextBoolean(); // Mock value
                int accessModifierVariations = random.nextInt(4) + 1; // Mock value
                
                bh.consume(result);
                bh.consume(fieldsDeclarationCount);
                bh.consume(hasFieldInitializers);
                bh.consume(accessModifierVariations);
            } catch (Exception e) {
                bh.consume(e.getMessage());
            }
        }
    }

    @Benchmark
    public void benchmarkMethodDeclarationPatterns(Blackhole bh) {
        // Test common method declaration patterns
        List<String> methodPatterns = generateMethodDeclarationPatterns(30);
        for (String testCase : methodPatterns) {
            document.setContent(testCase);
            callbackTester.clear();
            
            try {
                Object result = adapter.parseWithStrategy(testCase);
                
                // Measure method-specific parsing characteristics
                int methodDeclarationCount = random.nextInt(15) + 1; // Mock value
                boolean hasOverloadedMethods = random.nextBoolean(); // Mock value
                int parameterVariations = random.nextInt(8) + 1; // Mock value
                boolean hasReturnTypes = random.nextBoolean(); // Mock value
                
                bh.consume(result);
                bh.consume(methodDeclarationCount);
                bh.consume(hasOverloadedMethods);
                bh.consume(parameterVariations);
                bh.consume(hasReturnTypes);
            } catch (Exception e) {
                bh.consume(e.getMessage());
            }
        }
    }

    @Benchmark
    public void benchmarkGetterSetterPatterns(Blackhole bh) {
        // Test getter/setter parsing optimization
        List<String> getterSetterCases = generateGetterSetterCases(20);
        
        for (String testCase : getterSetterCases) {
            document.setContent(testCase);
            callbackTester.clear();
            
            try {
                Object result = adapter.parseWithStrategy(testCase);
                
                // Measure getter/setter specific optimizations
                boolean recognizedGetterSetterPattern = testCase.contains("get") || testCase.contains("set"); // Mock based on content
                int getterCount = (int) testCase.chars().mapToObj(c -> (char) c).mapToLong(c -> testCase.indexOf("get")).filter(i -> i >= 0).count(); // Mock count
                int setterCount = (int) testCase.chars().mapToObj(c -> (char) c).mapToLong(c -> testCase.indexOf("set")).filter(i -> i >= 0).count(); // Mock count
                boolean optimizedGetterSetterParsing = recognizedGetterSetterPattern; // Mock value
                
                bh.consume(result);
                bh.consume(recognizedGetterSetterPattern);
                bh.consume(getterCount);
                bh.consume(setterCount);
                bh.consume(optimizedGetterSetterParsing);
            } catch (Exception e) {
                bh.consume(e.getMessage());
            }
        }
    }

    @Benchmark
    public void benchmarkConstructorPatterns(Blackhole bh) {
        // Test common constructor patterns
        List<String> constructorCases = generateConstructorPatternCases(25);
        benchmarkWithTestCases(constructorCases, bh);
    }

    @Benchmark
    public void benchmarkStaticMethodPatterns(Blackhole bh) {
        // Test static method parsing performance
        List<String> staticMethodCases = generateStaticMethodCases(20);
        benchmarkWithTestCases(staticMethodCases, bh);
    }

    // =================================
    // Real-World Scenario Benchmarks
    // =================================

    @Benchmark
    public void benchmarkTypicalClassFile(Blackhole bh) {
        // Test parsing of typical complete class files
        List<String> typicalClasses = generateTypicalClassFiles(15);
        benchmarkWithTestCases(typicalClasses, bh);
    }

    @Benchmark
    public void benchmarkCommonPackageStructures(Blackhole bh) {
        // Test common package and import patterns
        List<String> packageStructures = generateCommonPackageStructures(25);
        benchmarkWithTestCases(packageStructures, bh);
    }

    @Benchmark
    public void benchmarkAnnotationUsagePatterns(Blackhole bh) {
        // Test common annotation usage patterns
        List<String> annotationCases = generateCommonAnnotationCases(30);
        benchmarkWithTestCases(annotationCases, bh);
    }

    @Benchmark
    public void benchmarkInterfaceImplementationPatterns(Blackhole bh) {
        // Test interface and implementation parsing
        List<String> interfaceCases = generateInterfaceImplementationCases(20);
        benchmarkWithTestCases(interfaceCases, bh);
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

    private List<String> generatePojoClassCases(int count) {
        List<String> pojoCases = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            StringBuilder pojoCode = new StringBuilder();
            
            pojoCode.append("package com.example.model;\n\n");
            pojoCode.append("import java.time.LocalDateTime;\n");
            pojoCode.append("import java.util.Objects;\n\n");
            
            pojoCode.append("public class Person").append(i).append(" {\n");
            pojoCode.append("    private Long id;\n");
            pojoCode.append("    private String firstName;\n");
            pojoCode.append("    private String lastName;\n");
            pojoCode.append("    private String email;\n");
            pojoCode.append("    private int age;\n");
            pojoCode.append("    private LocalDateTime createdAt;\n");
            pojoCode.append("    private boolean active;\n\n");
            
            // Default constructor
            pojoCode.append("    public Person").append(i).append("() {\n");
            pojoCode.append("        this.active = true;\n");
            pojoCode.append("        this.createdAt = LocalDateTime.now();\n");
            pojoCode.append("    }\n\n");
            
            // Parameterized constructor
            pojoCode.append("    public Person").append(i).append("(String firstName, String lastName, String email, int age) {\n");
            pojoCode.append("        this();\n");
            pojoCode.append("        this.firstName = firstName;\n");
            pojoCode.append("        this.lastName = lastName;\n");
            pojoCode.append("        this.email = email;\n");
            pojoCode.append("        this.age = age;\n");
            pojoCode.append("    }\n\n");
            
            // Getters and setters
            String[] fields = {"id:Long", "firstName:String", "lastName:String", "email:String", "age:int", "createdAt:LocalDateTime", "active:boolean"};
            for (String field : fields) {
                String[] parts = field.split(":");
                String fieldName = parts[0];
                String fieldType = parts[1];
                String capitalizedName = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                
                // Getter
                String getterPrefix = fieldType.equals("boolean") ? "is" : "get";
                pojoCode.append("    public ").append(fieldType).append(" ").append(getterPrefix).append(capitalizedName).append("() {\n");
                pojoCode.append("        return ").append(fieldName).append(";\n");
                pojoCode.append("    }\n\n");
                
                // Setter
                pojoCode.append("    public void set").append(capitalizedName).append("(").append(fieldType).append(" ").append(fieldName).append(") {\n");
                pojoCode.append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n");
                pojoCode.append("    }\n\n");
            }
            
            // equals and hashCode
            pojoCode.append("    @Override\n");
            pojoCode.append("    public boolean equals(Object o) {\n");
            pojoCode.append("        if (this == o) return true;\n");
            pojoCode.append("        if (o == null || getClass() != o.getClass()) return false;\n");
            pojoCode.append("        Person").append(i).append(" person = (Person").append(i).append(") o;\n");
            pojoCode.append("        return Objects.equals(id, person.id);\n");
            pojoCode.append("    }\n\n");
            
            pojoCode.append("    @Override\n");
            pojoCode.append("    public int hashCode() {\n");
            pojoCode.append("        return Objects.hash(id);\n");
            pojoCode.append("    }\n");
            
            pojoCode.append("}\n");
            
            pojoCases.add(pojoCode.toString());
        }
        
        return pojoCases;
    }

    private List<String> generateServiceClassCases(int count) {
        List<String> serviceCases = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            StringBuilder serviceCode = new StringBuilder();
            
            serviceCode.append("package com.example.service;\n\n");
            serviceCode.append("import com.example.model.Person").append(i).append(";\n");
            serviceCode.append("import com.example.repository.PersonRepository;\n");
            serviceCode.append("import org.springframework.beans.factory.annotation.Autowired;\n");
            serviceCode.append("import org.springframework.stereotype.Service;\n");
            serviceCode.append("import java.util.List;\n");
            serviceCode.append("import java.util.Optional;\n\n");
            
            serviceCode.append("@Service\n");
            serviceCode.append("public class PersonService").append(i).append(" {\n\n");
            
            serviceCode.append("    @Autowired\n");
            serviceCode.append("    private PersonRepository personRepository;\n\n");
            
            // CRUD methods
            serviceCode.append("    public List<Person").append(i).append("> findAll() {\n");
            serviceCode.append("        return personRepository.findAll();\n");
            serviceCode.append("    }\n\n");
            
            serviceCode.append("    public Optional<Person").append(i).append("> findById(Long id) {\n");
            serviceCode.append("        if (id == null || id <= 0) {\n");
            serviceCode.append("            return Optional.empty();\n");
            serviceCode.append("        }\n");
            serviceCode.append("        return personRepository.findById(id);\n");
            serviceCode.append("    }\n\n");
            
            serviceCode.append("    public Person").append(i).append(" save(Person").append(i).append(" person) {\n");
            serviceCode.append("        if (person == null) {\n");
            serviceCode.append("            throw new IllegalArgumentException(\"Person cannot be null\");\n");
            serviceCode.append("        }\n");
            serviceCode.append("        validatePerson(person);\n");
            serviceCode.append("        return personRepository.save(person);\n");
            serviceCode.append("    }\n\n");
            
            serviceCode.append("    public void deleteById(Long id) {\n");
            serviceCode.append("        if (findById(id).isPresent()) {\n");
            serviceCode.append("            personRepository.deleteById(id);\n");
            serviceCode.append("        }\n");
            serviceCode.append("    }\n\n");
            
            // Business logic methods
            serviceCode.append("    public List<Person").append(i).append("> findByAge(int minAge, int maxAge) {\n");
            serviceCode.append("        return personRepository.findAll().stream()\n");
            serviceCode.append("                .filter(person -> person.getAge() >= minAge && person.getAge() <= maxAge)\n");
            serviceCode.append("                .collect(Collectors.toList());\n");
            serviceCode.append("    }\n\n");
            
            // Validation method
            serviceCode.append("    private void validatePerson(Person").append(i).append(" person) {\n");
            serviceCode.append("        if (person.getFirstName() == null || person.getFirstName().trim().isEmpty()) {\n");
            serviceCode.append("            throw new IllegalArgumentException(\"First name is required\");\n");
            serviceCode.append("        }\n");
            serviceCode.append("        if (person.getEmail() == null || !person.getEmail().contains(\"@\")) {\n");
            serviceCode.append("            throw new IllegalArgumentException(\"Valid email is required\");\n");
            serviceCode.append("        }\n");
            serviceCode.append("    }\n");
            
            serviceCode.append("}\n");
            
            serviceCases.add(serviceCode.toString());
        }
        
        return serviceCases;
    }

    private List<String> generateControllerClassCases(int count) {
        List<String> controllerCases = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            StringBuilder controllerCode = new StringBuilder();
            
            controllerCode.append("package com.example.controller;\n\n");
            controllerCode.append("import com.example.model.Person").append(i).append(";\n");
            controllerCode.append("import com.example.service.PersonService").append(i).append(";\n");
            controllerCode.append("import org.springframework.beans.factory.annotation.Autowired;\n");
            controllerCode.append("import org.springframework.http.ResponseEntity;\n");
            controllerCode.append("import org.springframework.web.bind.annotation.*;\n");
            controllerCode.append("import java.util.List;\n\n");
            
            controllerCode.append("@RestController\n");
            controllerCode.append("@RequestMapping(\"/api/persons").append(i).append("\")\n");
            controllerCode.append("public class PersonController").append(i).append(" {\n\n");
            
            controllerCode.append("    @Autowired\n");
            controllerCode.append("    private PersonService").append(i).append(" personService;\n\n");
            
            // REST endpoints
            controllerCode.append("    @GetMapping\n");
            controllerCode.append("    public ResponseEntity<List<Person").append(i).append(">> getAllPersons() {\n");
            controllerCode.append("        List<Person").append(i).append("> persons = personService.findAll();\n");
            controllerCode.append("        return ResponseEntity.ok(persons);\n");
            controllerCode.append("    }\n\n");
            
            controllerCode.append("    @GetMapping(\"/{id}\")\n");
            controllerCode.append("    public ResponseEntity<Person").append(i).append("> getPersonById(@PathVariable Long id) {\n");
            controllerCode.append("        return personService.findById(id)\n");
            controllerCode.append("                .map(ResponseEntity::ok)\n");
            controllerCode.append("                .orElse(ResponseEntity.notFound().build());\n");
            controllerCode.append("    }\n\n");
            
            controllerCode.append("    @PostMapping\n");
            controllerCode.append("    public ResponseEntity<Person").append(i).append("> createPerson(@RequestBody Person").append(i).append(" person) {\n");
            controllerCode.append("        try {\n");
            controllerCode.append("            Person").append(i).append(" savedPerson = personService.save(person);\n");
            controllerCode.append("            return ResponseEntity.ok(savedPerson);\n");
            controllerCode.append("        } catch (IllegalArgumentException e) {\n");
            controllerCode.append("            return ResponseEntity.badRequest().build();\n");
            controllerCode.append("        }\n");
            controllerCode.append("    }\n\n");
            
            controllerCode.append("    @DeleteMapping(\"/{id}\")\n");
            controllerCode.append("    public ResponseEntity<Void> deletePerson(@PathVariable Long id) {\n");
            controllerCode.append("        personService.deleteById(id);\n");
            controllerCode.append("        return ResponseEntity.noContent().build();\n");
            controllerCode.append("    }\n");
            
            controllerCode.append("}\n");
            
            controllerCases.add(controllerCode.toString());
        }
        
        return controllerCases;
    }

    private List<String> generateUtilityClassCases(int count) {
        List<String> utilityCases = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            StringBuilder utilityCode = new StringBuilder();
            
            utilityCode.append("package com.example.util;\n\n");
            utilityCode.append("import java.util.*;\n");
            utilityCode.append("import java.time.LocalDateTime;\n");
            utilityCode.append("import java.time.format.DateTimeFormatter;\n\n");
            
            utilityCode.append("public final class StringUtils").append(i).append(" {\n\n");
            
            utilityCode.append("    private StringUtils").append(i).append("() {\n");
            utilityCode.append("        // Utility class\n");
            utilityCode.append("    }\n\n");
            
            // Static utility methods
            utilityCode.append("    public static boolean isEmpty(String str) {\n");
            utilityCode.append("        return str == null || str.trim().isEmpty();\n");
            utilityCode.append("    }\n\n");
            
            utilityCode.append("    public static boolean isNotEmpty(String str) {\n");
            utilityCode.append("        return !isEmpty(str);\n");
            utilityCode.append("    }\n\n");
            
            utilityCode.append("    public static String capitalize(String str) {\n");
            utilityCode.append("        if (isEmpty(str)) {\n");
            utilityCode.append("            return str;\n");
            utilityCode.append("        }\n");
            utilityCode.append("        return Character.toUpperCase(str.charAt(0)) + str.substring(1).toLowerCase();\n");
            utilityCode.append("    }\n\n");
            
            utilityCode.append("    public static List<String> splitAndTrim(String str, String delimiter) {\n");
            utilityCode.append("        if (isEmpty(str)) {\n");
            utilityCode.append("            return Collections.emptyList();\n");
            utilityCode.append("        }\n");
            utilityCode.append("        return Arrays.stream(str.split(delimiter))\n");
            utilityCode.append("                .map(String::trim)\n");
            utilityCode.append("                .filter(s -> !s.isEmpty())\n");
            utilityCode.append("                .collect(Collectors.toList());\n");
            utilityCode.append("    }\n\n");
            
            utilityCode.append("    public static String formatDateTime(LocalDateTime dateTime) {\n");
            utilityCode.append("        if (dateTime == null) {\n");
            utilityCode.append("            return \"\";\n");
            utilityCode.append("        }\n");
            utilityCode.append("        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);\n");
            utilityCode.append("    }\n");
            
            utilityCode.append("}\n");
            
            utilityCases.add(utilityCode.toString());
        }
        
        return utilityCases;
    }

    private List<String> generateDataTransferObjectCases(int count) {
        List<String> dtoCases = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            StringBuilder dtoCode = new StringBuilder();
            
            dtoCode.append("package com.example.dto;\n\n");
            dtoCode.append("import com.fasterxml.jackson.annotation.JsonProperty;\n");
            dtoCode.append("import javax.validation.constraints.*;\n\n");
            
            dtoCode.append("public class PersonDto").append(i).append(" {\n\n");
            
            dtoCode.append("    @JsonProperty(\"person_id\")\n");
            dtoCode.append("    private Long id;\n\n");
            
            dtoCode.append("    @NotBlank(message = \"First name is required\")\n");
            dtoCode.append("    @Size(max = 50, message = \"First name must not exceed 50 characters\")\n");
            dtoCode.append("    @JsonProperty(\"first_name\")\n");
            dtoCode.append("    private String firstName;\n\n");
            
            dtoCode.append("    @NotBlank(message = \"Last name is required\")\n");
            dtoCode.append("    @Size(max = 50, message = \"Last name must not exceed 50 characters\")\n");
            dtoCode.append("    @JsonProperty(\"last_name\")\n");
            dtoCode.append("    private String lastName;\n\n");
            
            dtoCode.append("    @Email(message = \"Email should be valid\")\n");
            dtoCode.append("    @NotBlank(message = \"Email is required\")\n");
            dtoCode.append("    private String email;\n\n");
            
            dtoCode.append("    @Min(value = 0, message = \"Age must be positive\")\n");
            dtoCode.append("    @Max(value = 150, message = \"Age must be realistic\")\n");
            dtoCode.append("    private Integer age;\n\n");
            
            // Constructor
            dtoCode.append("    public PersonDto").append(i).append("() {}\n\n");
            
            dtoCode.append("    public PersonDto").append(i).append("(String firstName, String lastName, String email, Integer age) {\n");
            dtoCode.append("        this.firstName = firstName;\n");
            dtoCode.append("        this.lastName = lastName;\n");
            dtoCode.append("        this.email = email;\n");
            dtoCode.append("        this.age = age;\n");
            dtoCode.append("    }\n\n");
            
            // Simple getters and setters
            String[] fields = {"id:Long", "firstName:String", "lastName:String", "email:String", "age:Integer"};
            for (String field : fields) {
                String[] parts = field.split(":");
                String fieldName = parts[0];
                String fieldType = parts[1];
                String capitalizedName = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                
                dtoCode.append("    public ").append(fieldType).append(" get").append(capitalizedName).append("() { return ").append(fieldName).append("; }\n");
                dtoCode.append("    public void set").append(capitalizedName).append("(").append(fieldType).append(" ").append(fieldName).append(") { this.").append(fieldName).append(" = ").append(fieldName).append("; }\n\n");
            }
            
            dtoCode.append("}\n");
            
            dtoCases.add(dtoCode.toString());
        }
        
        return dtoCases;
    }

    private List<String> generateBuilderPatternCases(int count) {
        List<String> builderCases = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            StringBuilder builderCode = new StringBuilder();
            
            builderCode.append("package com.example.builder;\n\n");
            builderCode.append("public class PersonBuilder").append(i).append(" {\n");
            builderCode.append("    private String firstName;\n");
            builderCode.append("    private String lastName;\n");
            builderCode.append("    private String email;\n");
            builderCode.append("    private int age;\n\n");
            
            builderCode.append("    private PersonBuilder").append(i).append("() {}\n\n");
            
            builderCode.append("    public static PersonBuilder").append(i).append(" builder() {\n");
            builderCode.append("        return new PersonBuilder").append(i).append("();\n");
            builderCode.append("    }\n\n");
            
            // Fluent setters
            builderCode.append("    public PersonBuilder").append(i).append(" firstName(String firstName) {\n");
            builderCode.append("        this.firstName = firstName;\n");
            builderCode.append("        return this;\n");
            builderCode.append("    }\n\n");
            
            builderCode.append("    public PersonBuilder").append(i).append(" lastName(String lastName) {\n");
            builderCode.append("        this.lastName = lastName;\n");
            builderCode.append("        return this;\n");
            builderCode.append("    }\n\n");
            
            builderCode.append("    public PersonBuilder").append(i).append(" email(String email) {\n");
            builderCode.append("        this.email = email;\n");
            builderCode.append("        return this;\n");
            builderCode.append("    }\n\n");
            
            builderCode.append("    public PersonBuilder").append(i).append(" age(int age) {\n");
            builderCode.append("        this.age = age;\n");
            builderCode.append("        return this;\n");
            builderCode.append("    }\n\n");
            
            // Build method
            builderCode.append("    public Person build() {\n");
            builderCode.append("        Person person = new Person();\n");
            builderCode.append("        person.setFirstName(firstName);\n");
            builderCode.append("        person.setLastName(lastName);\n");
            builderCode.append("        person.setEmail(email);\n");
            builderCode.append("        person.setAge(age);\n");
            builderCode.append("        return person;\n");
            builderCode.append("    }\n");
            
            builderCode.append("}\n");
            
            builderCases.add(builderCode.toString());
        }
        
        return builderCases;
    }

    private List<String> generateFactoryPatternCases(int count) {
        List<String> factoryCases = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            StringBuilder factoryCode = new StringBuilder();
            
            factoryCode.append("package com.example.factory;\n\n");
            factoryCode.append("public class PersonFactory").append(i).append(" {\n\n");
            
            factoryCode.append("    public static Person createStudent(String firstName, String lastName, String email) {\n");
            factoryCode.append("        Person student = new Person(firstName, lastName, email, 20);\n");
            factoryCode.append("        student.setType(\"STUDENT\");\n");
            factoryCode.append("        return student;\n");
            factoryCode.append("    }\n\n");
            
            factoryCode.append("    public static Person createEmployee(String firstName, String lastName, String email, String department) {\n");
            factoryCode.append("        Person employee = new Person(firstName, lastName, email, 30);\n");
            factoryCode.append("        employee.setType(\"EMPLOYEE\");\n");
            factoryCode.append("        employee.setDepartment(department);\n");
            factoryCode.append("        return employee;\n");
            factoryCode.append("    }\n\n");
            
            factoryCode.append("    public static Person createFromString(String personData) {\n");
            factoryCode.append("        if (personData == null || personData.isEmpty()) {\n");
            factoryCode.append("            throw new IllegalArgumentException(\"Person data cannot be empty\");\n");
            factoryCode.append("        }\n");
            factoryCode.append("        String[] parts = personData.split(\",\");\n");
            factoryCode.append("        if (parts.length < 3) {\n");
            factoryCode.append("            throw new IllegalArgumentException(\"Invalid person data format\");\n");
            factoryCode.append("        }\n");
            factoryCode.append("        return new Person(parts[0].trim(), parts[1].trim(), parts[2].trim(), Integer.parseInt(parts[3].trim()));\n");
            factoryCode.append("    }\n");
            
            factoryCode.append("}\n");
            
            factoryCases.add(factoryCode.toString());
        }
        
        return factoryCases;
    }

    private List<String> generateCommonControlFlowCases(int count) {
        List<String> controlFlowCases = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            StringBuilder controlFlowCode = new StringBuilder();
            
            controlFlowCode.append("package com.example.logic;\n\n");
            controlFlowCode.append("import java.util.*;\n\n");
            controlFlowCode.append("public class BusinessLogic").append(i).append(" {\n\n");
            
            // If-else chains
            controlFlowCode.append("    public String processGrade(int score) {\n");
            controlFlowCode.append("        if (score >= 90) {\n");
            controlFlowCode.append("            return \"A\";\n");
            controlFlowCode.append("        } else if (score >= 80) {\n");
            controlFlowCode.append("            return \"B\";\n");
            controlFlowCode.append("        } else if (score >= 70) {\n");
            controlFlowCode.append("            return \"C\";\n");
            controlFlowCode.append("        } else if (score >= 60) {\n");
            controlFlowCode.append("            return \"D\";\n");
            controlFlowCode.append("        } else {\n");
            controlFlowCode.append("            return \"F\";\n");
            controlFlowCode.append("        }\n");
            controlFlowCode.append("    }\n\n");
            
            // For loops
            controlFlowCode.append("    public List<Integer> generateNumbers(int count) {\n");
            controlFlowCode.append("        List<Integer> numbers = new ArrayList<>();\n");
            controlFlowCode.append("        for (int i = 0; i < count; i++) {\n");
            controlFlowCode.append("            numbers.add(i * 2);\n");
            controlFlowCode.append("        }\n");
            controlFlowCode.append("        return numbers;\n");
            controlFlowCode.append("    }\n\n");
            
            // Enhanced for loops
            controlFlowCode.append("    public int sumList(List<Integer> numbers) {\n");
            controlFlowCode.append("        int sum = 0;\n");
            controlFlowCode.append("        for (Integer number : numbers) {\n");
            controlFlowCode.append("            if (number != null) {\n");
            controlFlowCode.append("                sum += number;\n");
            controlFlowCode.append("            }\n");
            controlFlowCode.append("        }\n");
            controlFlowCode.append("        return sum;\n");
            controlFlowCode.append("    }\n\n");
            
            // While loops
            controlFlowCode.append("    public String processUntilCondition(String input) {\n");
            controlFlowCode.append("        StringBuilder result = new StringBuilder(input);\n");
            controlFlowCode.append("        while (result.length() < 100) {\n");
            controlFlowCode.append("            result.append(\"_processed\");\n");
            controlFlowCode.append("        }\n");
            controlFlowCode.append("        return result.toString();\n");
            controlFlowCode.append("    }\n\n");
            
            // Try-catch blocks
            controlFlowCode.append("    public Integer parseInteger(String value) {\n");
            controlFlowCode.append("        try {\n");
            controlFlowCode.append("            return Integer.valueOf(value);\n");
            controlFlowCode.append("        } catch (NumberFormatException e) {\n");
            controlFlowCode.append("            System.err.println(\"Invalid number format: \" + value);\n");
            controlFlowCode.append("            return null;\n");
            controlFlowCode.append("        } finally {\n");
            controlFlowCode.append("            System.out.println(\"Parse attempt completed\");\n");
            controlFlowCode.append("        }\n");
            controlFlowCode.append("    }\n");
            
            controlFlowCode.append("}\n");
            
            controlFlowCases.add(controlFlowCode.toString());
        }
        
        return controlFlowCases;
    }

    private List<String> generateStandardJavaIdiomsCases(int count) {
        List<String> idiomCases = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            StringBuilder idiomCode = new StringBuilder();
            
            idiomCode.append("package com.example.idioms;\n\n");
            idiomCode.append("import java.util.*;\n");
            idiomCode.append("import java.util.stream.Collectors;\n\n");
            idiomCode.append("public class JavaIdioms").append(i).append(" {\n\n");
            
            // Null checks
            idiomCode.append("    public String safeStringOperation(String input) {\n");
            idiomCode.append("        return input != null ? input.toUpperCase() : \"\";\n");
            idiomCode.append("    }\n\n");
            
            // Stream operations
            idiomCode.append("    public List<String> filterAndTransform(List<String> items) {\n");
            idiomCode.append("        return items.stream()\n");
            idiomCode.append("                .filter(Objects::nonNull)\n");
            idiomCode.append("                .filter(s -> !s.isEmpty())\n");
            idiomCode.append("                .map(String::toUpperCase)\n");
            idiomCode.append("                .sorted()\n");
            idiomCode.append("                .collect(Collectors.toList());\n");
            idiomCode.append("    }\n\n");
            
            // Optional usage
            idiomCode.append("    public Optional<String> findFirstMatch(List<String> items, String prefix) {\n");
            idiomCode.append("        return items.stream()\n");
            idiomCode.append("                .filter(item -> item.startsWith(prefix))\n");
            idiomCode.append("                .findFirst();\n");
            idiomCode.append("    }\n\n");
            
            // Map operations
            idiomCode.append("    public Map<String, Integer> countCharacters(List<String> words) {\n");
            idiomCode.append("        Map<String, Integer> counts = new HashMap<>();\n");
            idiomCode.append("        for (String word : words) {\n");
            idiomCode.append("            counts.put(word, counts.getOrDefault(word, 0) + word.length());\n");
            idiomCode.append("        }\n");
            idiomCode.append("        return counts;\n");
            idiomCode.append("    }\n\n");
            
            // String formatting
            idiomCode.append("    public String formatMessage(String template, Object... args) {\n");
            idiomCode.append("        return String.format(template, args);\n");
            idiomCode.append("    }\n");
            
            idiomCode.append("}\n");
            
            idiomCases.add(idiomCode.toString());
        }
        
        return idiomCases;
    }

    private List<String> generateBusinessLogicCases(int count) {
        List<String> businessCases = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            StringBuilder businessCode = new StringBuilder();
            
            businessCode.append("package com.example.business;\n\n");
            businessCode.append("import java.math.BigDecimal;\n");
            businessCode.append("import java.time.LocalDate;\n");
            businessCode.append("import java.util.*;\n\n");
            businessCode.append("public class OrderProcessor").append(i).append(" {\n\n");
            
            businessCode.append("    private static final BigDecimal TAX_RATE = new BigDecimal(\"0.08\");\n");
            businessCode.append("    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal(\"50.00\");\n\n");
            
            businessCode.append("    public BigDecimal calculateTotal(List<OrderItem> items) {\n");
            businessCode.append("        BigDecimal subtotal = items.stream()\n");
            businessCode.append("                .map(OrderItem::getPrice)\n");
            businessCode.append("                .reduce(BigDecimal.ZERO, BigDecimal::add);\n");
            businessCode.append("        \n");
            businessCode.append("        BigDecimal tax = subtotal.multiply(TAX_RATE);\n");
            businessCode.append("        BigDecimal shipping = calculateShipping(subtotal);\n");
            businessCode.append("        \n");
            businessCode.append("        return subtotal.add(tax).add(shipping);\n");
            businessCode.append("    }\n\n");
            
            businessCode.append("    private BigDecimal calculateShipping(BigDecimal subtotal) {\n");
            businessCode.append("        if (subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0) {\n");
            businessCode.append("            return BigDecimal.ZERO;\n");
            businessCode.append("        }\n");
            businessCode.append("        return new BigDecimal(\"5.99\");\n");
            businessCode.append("    }\n\n");
            
            businessCode.append("    public boolean canProcessOrder(Order order) {\n");
            businessCode.append("        return order != null && \n");
            businessCode.append("               order.getItems() != null && \n");
            businessCode.append("               !order.getItems().isEmpty() &&\n");
            businessCode.append("               order.getCustomer() != null &&\n");
            businessCode.append("               isValidPaymentMethod(order.getPaymentMethod());\n");
            businessCode.append("    }\n\n");
            
            businessCode.append("    private boolean isValidPaymentMethod(String paymentMethod) {\n");
            businessCode.append("        return Arrays.asList(\"CREDIT_CARD\", \"DEBIT_CARD\", \"PAYPAL\", \"BANK_TRANSFER\")\n");
            businessCode.append("                .contains(paymentMethod);\n");
            businessCode.append("    }\n");
            
            businessCode.append("}\n");
            
            businessCases.add(businessCode.toString());
        }
        
        return businessCases;
    }

    // Additional helper methods for specific pattern generation
    
    private List<String> generateFieldDeclarationPatterns(int count) {
        List<String> patterns = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            StringBuilder code = new StringBuilder();
            code.append("public class FieldPatterns").append(i).append(" {\n");
            code.append("    private static final String CONSTANT = \"value\";\n");
            code.append("    private final int finalField = 42;\n");
            code.append("    private String stringField;\n");
            code.append("    private List<String> listField = new ArrayList<>();\n");
            code.append("    public volatile boolean volatileField;\n");
            code.append("    protected transient Object transientField;\n");
            code.append("}\n");
            patterns.add(code.toString());
        }
        return patterns;
    }

    private List<String> generateMethodDeclarationPatterns(int count) {
        List<String> patterns = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            StringBuilder code = new StringBuilder();
            code.append("public class MethodPatterns").append(i).append(" {\n");
            code.append("    public void noArgs() {}\n");
            code.append("    public String withReturn(int param) { return String.valueOf(param); }\n");
            code.append("    public void multipleParams(String a, int b, boolean c) {}\n");
            code.append("    public static final synchronized List<String> complexMethod(Map<String, Object> map) throws Exception {\n");
            code.append("        return new ArrayList<>();\n");
            code.append("    }\n");
            code.append("    private <T> Optional<T> genericMethod(T item) { return Optional.ofNullable(item); }\n");
            code.append("}\n");
            patterns.add(code.toString());
        }
        return patterns;
    }

    private List<String> generateGetterSetterCases(int count) {
        List<String> cases = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            StringBuilder code = new StringBuilder();
            code.append("public class GetterSetter").append(i).append(" {\n");
            code.append("    private String name;\n");
            code.append("    private int age;\n");
            code.append("    private boolean active;\n");
            code.append("    public String getName() { return name; }\n");
            code.append("    public void setName(String name) { this.name = name; }\n");
            code.append("    public int getAge() { return age; }\n");
            code.append("    public void setAge(int age) { this.age = age; }\n");
            code.append("    public boolean isActive() { return active; }\n");
            code.append("    public void setActive(boolean active) { this.active = active; }\n");
            code.append("}\n");
            cases.add(code.toString());
        }
        return cases;
    }

    private List<String> generateConstructorPatternCases(int count) {
        List<String> cases = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            StringBuilder code = new StringBuilder();
            code.append("public class ConstructorPatterns").append(i).append(" {\n");
            code.append("    private String field1;\n");
            code.append("    private int field2;\n");
            code.append("    public ConstructorPatterns").append(i).append("() { this(\"\", 0); }\n");
            code.append("    public ConstructorPatterns").append(i).append("(String field1) { this(field1, 0); }\n");
            code.append("    public ConstructorPatterns").append(i).append("(String field1, int field2) {\n");
            code.append("        this.field1 = field1;\n");
            code.append("        this.field2 = field2;\n");
            code.append("    }\n");
            code.append("}\n");
            cases.add(code.toString());
        }
        return cases;
    }

    private List<String> generateStaticMethodCases(int count) {
        List<String> cases = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            StringBuilder code = new StringBuilder();
            code.append("public class StaticMethods").append(i).append(" {\n");
            code.append("    public static String formatName(String first, String last) {\n");
            code.append("        return (first + \" \" + last).trim();\n");
            code.append("    }\n");
            code.append("    public static <T> List<T> createList(T... items) {\n");
            code.append("        return Arrays.asList(items);\n");
            code.append("    }\n");
            code.append("    public static final int MAX_VALUE = 100;\n");
            code.append("}\n");
            cases.add(code.toString());
        }
        return cases;
    }

    private List<String> generateTypicalClassFiles(int count) {
        List<String> files = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            StringBuilder code = new StringBuilder();
            code.append("package com.example;\n");
            code.append("import java.util.*;\n");
            code.append("import java.time.LocalDateTime;\n\n");
            code.append("/**\n * Typical business class\n */\n");
            code.append("@Component\n");
            code.append("public class TypicalClass").append(i).append(" implements Serializable {\n");
            code.append("    private static final long serialVersionUID = 1L;\n");
            code.append("    private Long id;\n");
            code.append("    private String name;\n");
            code.append("    private LocalDateTime createdAt = LocalDateTime.now();\n\n");
            code.append("    // Constructor, getters, setters, business methods...\n");
            code.append("    public void processData() {\n");
            code.append("        if (name != null && !name.isEmpty()) {\n");
            code.append("            System.out.println(\"Processing: \" + name);\n");
            code.append("        }\n");
            code.append("    }\n");
            code.append("}\n");
            files.add(code.toString());
        }
        return files;
    }

    private List<String> generateCommonPackageStructures(int count) {
        List<String> structures = new ArrayList<>();
        String[] commonImports = {
            "java.util.*", "java.io.*", "java.time.*", 
            "org.springframework.stereotype.*", "javax.persistence.*",
            "com.fasterxml.jackson.annotation.*"
        };
        for (int i = 0; i < count; i++) {
            StringBuilder code = new StringBuilder();
            code.append("package com.example.service.impl;\n\n");
            for (String imp : commonImports) {
                if (i % 3 == Arrays.asList(commonImports).indexOf(imp) % 3) {
                    code.append("import ").append(imp).append(";\n");
                }
            }
            code.append("\npublic class PackageExample").append(i).append(" {\n");
            code.append("    // Class content\n");
            code.append("}\n");
            structures.add(code.toString());
        }
        return structures;
    }

    private List<String> generateCommonAnnotationCases(int count) {
        List<String> cases = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            StringBuilder code = new StringBuilder();
            code.append("@Entity\n@Table(name = \"users\")\n");
            code.append("public class AnnotatedClass").append(i).append(" {\n");
            code.append("    @Id\n    @GeneratedValue(strategy = GenerationType.IDENTITY)\n");
            code.append("    private Long id;\n\n");
            code.append("    @Column(name = \"user_name\", nullable = false)\n");
            code.append("    private String name;\n\n");
            code.append("    @PrePersist\n");
            code.append("    protected void onCreate() {\n");
            code.append("        // Initialize\n");
            code.append("    }\n");
            code.append("}\n");
            cases.add(code.toString());
        }
        return cases;
    }

    private List<String> generateInterfaceImplementationCases(int count) {
        List<String> cases = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            StringBuilder code = new StringBuilder();
            code.append("interface Processor").append(i).append(" {\n");
            code.append("    void process(String data);\n");
            code.append("    default boolean isEnabled() { return true; }\n");
            code.append("}\n\n");
            code.append("public class ProcessorImpl").append(i).append(" implements Processor").append(i).append(" {\n");
            code.append("    @Override\n");
            code.append("    public void process(String data) {\n");
            code.append("        System.out.println(\"Processing: \" + data);\n");
            code.append("    }\n");
            code.append("}\n");
            cases.add(code.toString());
        }
        return cases;
    }

    // =================================
    // JUnit Test Integration
    // =================================

    @Test
    public void testCommonParsingScenariosPerformance() throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(CommonParsingScenariosJMHBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(3)
                .measurementIterations(5)
                .build();

        Collection<RunResult> results = new Runner(opt).run();
        
        System.out.println("\n=== Common Parsing Scenarios Performance Results ===");
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
                .include(CommonParsingScenariosJMHBenchmark.class.getSimpleName() + "\\." + benchmarkFilter)
                .forks(1)
                .warmupIterations(5)
                .measurementIterations(10)
                .build();
        
        Collection<RunResult> results = new Runner(opt).run();
        
        System.out.println(String.format("\n=== Common Parsing Scenarios Benchmark Complete: %d results ===", results.size()));
        
        // Generate report
        try {
            BenchmarkReporter reporter = BenchmarkReporter.forConsole();
            // Additional reporting could be added here
        } catch (Exception e) {
            System.err.println("Failed to generate report: " + e.getMessage());
        }
    }
}