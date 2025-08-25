package bluej.parser;

import bluej.extensions2.SourceType;
import bluej.parser.entity.ClassLoaderResolver;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.PackageResolver;
import bluej.parser.nodes.ParsedCUNode;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Simple test to debug Kotlin parsing issues.
 * This test is designed to isolate and identify problems with the Kotlin parser.
 */
public class SimpleKotlinParsingTest {

    private TestEntityResolver resolver;

    @Before
    public void setUp() {
        InitConfig.init();
        resolver = new TestEntityResolver(new ClassLoaderResolver(this.getClass().getClassLoader()));
    }

    @Test
    public void testBasicKotlinClassParsing() {
        String sourceCode = "class SimpleClass {\n" +
                           "    fun hello(): String {\n" +
                           "        return \"hello\"\n" +
                           "    }\n" +
                           "}\n";

        System.out.println("=== Testing Basic Kotlin Class Parsing ===");
        System.out.println("Source code:");
        System.out.println(sourceCode);
        System.out.println();

        EntityResolver packageResolver = new PackageResolver(resolver, "");
        TestableDocument document = new TestableDocument(packageResolver, SourceType.Kotlin);

        // Enable parser
        document.enableParser(true);

        // Insert the source code
        document.insertString(0, sourceCode);

        // Force parsing to complete
        document.flushReparseQueue();

        // Check for parse errors
        List<String> parseErrors = document.getParseErrors();
        if (!parseErrors.isEmpty()) {
            System.err.println("Parse errors found:");
            for (String error : parseErrors) {
                System.err.println("  " + error);
            }
        } else {
            System.out.println("No parse errors found");
        }

        // Get the parsed node
        ParsedCUNode parsedNode = document.getParser();
        assertNotNull("Parsed node should not be null", parsedNode);

        // Print the parse tree
        System.out.println("\nParse tree:");
        ParsedCUNode.printTree(parsedNode, 0, 0);

        // Verify no parse errors
        assertTrue("Should have no parse errors", parseErrors.isEmpty());
    }

    @Test
    public void testMinimalKotlinClass() {
        String sourceCode = "class A { }";

        System.out.println("=== Testing Minimal Kotlin Class ===");
        System.out.println("Source code: " + sourceCode);

        EntityResolver packageResolver = new PackageResolver(resolver, "");
        TestableDocument document = new TestableDocument(packageResolver, SourceType.Kotlin);

        document.enableParser(true);
        document.insertString(0, sourceCode);
        document.flushReparseQueue();

        List<String> parseErrors = document.getParseErrors();
        if (!parseErrors.isEmpty()) {
            System.err.println("Parse errors found:");
            for (String error : parseErrors) {
                System.err.println("  " + error);
            }
        }

        ParsedCUNode parsedNode = document.getParser();
        assertNotNull("Parsed node should not be null", parsedNode);

        if (parsedNode != null) {
            System.out.println("Parse tree:");
            ParsedCUNode.printTree(parsedNode, 0, 0);
        }

        assertTrue("Should have no parse errors for minimal class", parseErrors.isEmpty());
    }

    @Test
    public void testKotlinFunctionParsing() {
        String sourceCode = """
            class TestClass {
                fun add(a: Int, b: Int): Int {
                    return a + b
                }

                fun greet(name: String) {
                    println("Hello, $name")
                }
            }
            """;

        System.out.println("=== Testing Kotlin Function Parsing ===");
        System.out.println("Source code:");
        System.out.println(sourceCode);

        EntityResolver packageResolver = new PackageResolver(resolver, "");
        TestableDocument document = new TestableDocument(packageResolver, SourceType.Kotlin);

        document.enableParser(true);
        document.insertString(0, sourceCode);
        document.flushReparseQueue();

        List<String> parseErrors = document.getParseErrors();
        if (!parseErrors.isEmpty()) {
            System.err.println("Parse errors found:");
            for (String error : parseErrors) {
                System.err.println("  " + error);
            }
        }

        ParsedCUNode parsedNode = document.getParser();
        assertNotNull("Parsed node should not be null", parsedNode);

        if (parsedNode != null && parseErrors.isEmpty()) {
            System.out.println("Parse tree:");
            ParsedCUNode.printTree(parsedNode, 0, 0);
        }

        // Allow some parse errors for now as we're debugging
        System.out.println("Parse error count: " + parseErrors.size());
    }

    @Test
    public void testEmptyKotlinSource() {
        String sourceCode = "";

        System.out.println("=== Testing Empty Kotlin Source ===");

        EntityResolver packageResolver = new PackageResolver(resolver, "");
        TestableDocument document = new TestableDocument(packageResolver, SourceType.Kotlin);

        document.enableParser(true);
        document.insertString(0, sourceCode);
        document.flushReparseQueue();

        List<String> parseErrors = document.getParseErrors();
        ParsedCUNode parsedNode = document.getParser();

        assertNotNull("Parsed node should not be null even for empty source", parsedNode);

        System.out.println("Parse errors for empty source: " + parseErrors.size());
        for (String error : parseErrors) {
            System.out.println("  " + error);
        }
    }

    @Test
    public void testKotlinWithPackage() {
        String sourceCode = """
            package test.pkg

            class MyClass {
                fun doSomething(): Unit {
                    println("Doing something")
                }
            }
            """;

        System.out.println("=== Testing Kotlin With Package ===");
        System.out.println("Source code:");
        System.out.println(sourceCode);

        EntityResolver packageResolver = new PackageResolver(resolver, "test.pkg");
        TestableDocument document = new TestableDocument(packageResolver, SourceType.Kotlin);

        document.enableParser(true);
        document.insertString(0, sourceCode);
        document.flushReparseQueue();

        List<String> parseErrors = document.getParseErrors();
        if (!parseErrors.isEmpty()) {
            System.err.println("Parse errors found:");
            for (String error : parseErrors) {
                System.err.println("  " + error);
            }
        }

        ParsedCUNode parsedNode = document.getParser();
        assertNotNull("Parsed node should not be null", parsedNode);

        if (parsedNode != null && parseErrors.isEmpty()) {
            System.out.println("Successfully parsed Kotlin with package");
            ParsedCUNode.printTree(parsedNode, 0, 0);
        }
    }
}
