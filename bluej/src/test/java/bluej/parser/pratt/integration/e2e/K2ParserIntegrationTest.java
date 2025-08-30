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

import bluej.parser.CallbackDelegate;
import bluej.parser.pratt.integration.CallbackTestingUtility;
import bluej.parser.pratt.integration.CallbackTestingUtility.CallbackTester;
import bluej.parser.pratt.integration.benchmark.adapters.K2ParserIntegrationAdapter;
import bluej.parser.pratt.integration.benchmark.corpus.TestCorpusGenerator;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.ValueEntity;
import bluej.parser.entity.TypeEntity;
import bluej.parser.entity.PackageOrClass;
import bluej.parser.entity.TypeArgumentEntity;
import bluej.debugger.gentype.Reflective;
import bluej.debugger.gentype.JavaType;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * End-to-End integration test for K2 Parser Integration strategy.
 * 
 * This test validates the Kotlin K2 compiler frontend integration with visitor
 * pattern over PSI (Program Structure Interface) trees to convert Kotlin AST
 * nodes into BlueJ callback sequences. Tests complete integration with BlueJ
 * framework for Kotlin-specific parsing scenarios.
 * 
 * Key validation aspects:
 * - PSI tree structure handling and traversal
 * - K2ToCallbackAdapter visitor pattern implementation
 * - Kotlin language feature parsing (classes, functions, properties)
 * - High-performance tree walking with callback emission
 * - Type-safe node conversion and structural integrity
 */
public class K2ParserIntegrationTest {
    
    private TestableDocument document;
    private MockEntityResolver entityResolver;
    private CallbackTester callbackTester;
    private K2ParserIntegrationAdapter adapter;
    private TestCorpusGenerator corpusGenerator;
    
    @Before
    public void setUp() {
        entityResolver = new MockEntityResolver();
        document = new TestableDocument("test.kt", entityResolver);
        callbackTester = CallbackTestingUtility.forDelegate(document.getCallbackDelegate());
        adapter = new K2ParserIntegrationAdapter();
        adapter.setCallbackDelegate(document.getCallbackDelegate());
        corpusGenerator = new TestCorpusGenerator();
    }
    
    @Test
    public void testKotlinClassWithPropertiesPSITreeTraversal() throws Exception {
        // Given: Kotlin class with various property types
        String kotlinCode = """
            package com.example.kotlin
            
            import kotlin.collections.List
            
            class KotlinTestClass {
                val readOnlyProperty: String = "value"
                var mutableProperty: Int = 42
                private lateinit var lateInitProperty: String
                
                fun processData(): List<String> {
                    return listOf("item1", "item2")
                }
                
                companion object {
                    const val CONSTANT = "constant"
                }
            }
            """;
        
        document.setContent(kotlinCode);
        
        // When: Parse with K2 PSI tree integration
        Object psiResult = adapter.parseWithStrategy(kotlinCode);
        
        // Then: Verify PSI tree structure callbacks
        assertNotNull("PSI tree should be created", psiResult);
        
        // Verify Kotlin-specific callback sequence
        assertTrue(callbackTester.hasCallback("fileStart"));
        assertTrue(callbackTester.hasCallback("packageDeclaration"));
        assertTrue(callbackTester.hasCallback("importStart"));
        assertTrue(callbackTester.hasCallback("importEnd"));
        assertTrue(callbackTester.hasCallback("classStart"));
        assertTrue(callbackTester.hasCallback("fieldDeclaration"));
        assertTrue(callbackTester.hasCallback("methodStart"));
        assertTrue(callbackTester.hasCallback("methodEnd"));
        assertTrue(callbackTester.hasCallback("classEnd"));
        
        // Verify callback balance for PSI tree traversal
        assertTrue("PSI tree traversal should maintain callback balance", callbackTester.isBalanced());
        
        // Verify multiple property declarations
        int fieldCount = callbackTester.countCallbacks("fieldDeclaration");
        assertTrue("Should detect multiple Kotlin properties", fieldCount >= 3);
    }
    
    @Test
    public void testKotlinFunctionsWithParametersAndExpressions() throws Exception {
        // Given: Complex Kotlin function structures
        String kotlinCode = """
            class FunctionTest {
                fun simpleFunction() = "result"
                
                fun functionWithParameters(param1: String, param2: Int): Boolean {
                    return param1.length > param2
                }
                
                fun higherOrderFunction(block: (String) -> Unit): String {
                    val result = "test"
                    block(result)
                    return result
                }
                
                suspend fun suspendFunction(): String {
                    delay(100)
                    return "suspended"
                }
            }
            """;
        
        document.setContent(kotlinCode);
        
        // When: Parse Kotlin functions
        adapter.parseWithStrategy(kotlinCode);
        
        // Then: Verify function parsing callbacks
        assertTrue(callbackTester.hasCallback("classStart"));
        assertTrue(callbackTester.hasCallback("methodStart"));
        assertTrue(callbackTester.hasCallback("methodEnd"));
        assertTrue(callbackTester.hasCallback("classEnd"));
        
        // Verify parameter handling
        assertTrue(callbackTester.hasCallback("parameter"));
        
        // Verify expression handling
        assertTrue(callbackTester.hasCallback("exprStart"));
        assertTrue(callbackTester.hasCallback("exprEnd"));
        
        // Check function count
        int methodCount = callbackTester.countCallbacks("methodStart");
        assertEquals("Should parse all Kotlin functions", 4, methodCount);
        
        // Verify balance with complex Kotlin structures
        assertTrue("Complex Kotlin functions should maintain balance", callbackTester.isBalanced());
    }
    
    @Test
    public void testKotlinDataClassesAndSealedClasses() throws Exception {
        // Given: Kotlin-specific class types
        String kotlinCode = """
            sealed class Result<T> {
                data class Success<T>(val data: T) : Result<T>()
                data class Error(val exception: Throwable) : Result<Nothing>()
                object Loading : Result<Nothing>()
            }
            
            data class User(
                val id: Long,
                val name: String,
                val email: String?
            ) {
                fun isValid(): Boolean = name.isNotBlank() && email != null
            }
            
            enum class Status {
                ACTIVE, INACTIVE, PENDING
            }
            """;
        
        document.setContent(kotlinCode);
        
        // When: Parse Kotlin class variants
        adapter.parseWithStrategy(kotlinCode);
        
        // Then: Verify Kotlin class type handling
        assertTrue(callbackTester.hasCallback("classStart"));
        assertTrue(callbackTester.hasCallback("classEnd"));
        
        // Multiple class declarations should be handled
        int classStartCount = callbackTester.countCallbacks("classStart");
        assertTrue("Should handle sealed, data, object, and enum classes", classStartCount >= 4);
        
        // Verify property handling in data classes
        assertTrue(callbackTester.hasCallback("fieldDeclaration"));
        int fieldCount = callbackTester.countCallbacks("fieldDeclaration");
        assertTrue("Should handle data class properties", fieldCount >= 3);
        
        // Verify method in data class
        assertTrue(callbackTester.hasCallback("methodStart"));
        assertTrue(callbackTester.hasCallback("methodEnd"));
        
        // Balance should be maintained across complex Kotlin structures
        assertTrue("Complex Kotlin class types should maintain balance", callbackTester.isBalanced());
    }
    
    @Test
    public void testKotlinCoroutinesAndExtensionFunctions() throws Exception {
        // Given: Advanced Kotlin features
        String kotlinCode = """
            import kotlinx.coroutines.*
            
            class CoroutineTest {
                suspend fun fetchData(): String = withContext(Dispatchers.IO) {
                    delay(1000)
                    "data"
                }
                
                fun String.isEmailValid(): Boolean {
                    return this.contains("@") && this.contains(".")
                }
                
                inline fun <reified T> processGeneric(value: T): String {
                    return when (value) {
                        is String -> value.uppercase()
                        is Int -> value.toString()
                        else -> "unknown"
                    }
                }
            }
            
            suspend fun topLevelSuspend(): Unit = runBlocking {
                println("Top level suspend function")
            }
            """;
        
        document.setContent(kotlinCode);
        
        // When: Parse advanced Kotlin features
        adapter.parseWithStrategy(kotlinCode);
        
        // Then: Verify advanced feature parsing
        assertTrue(callbackTester.hasCallback("importStart"));
        assertTrue(callbackTester.hasCallback("classStart"));
        assertTrue(callbackTester.hasCallback("methodStart"));
        assertTrue(callbackTester.hasCallback("methodEnd"));
        assertTrue(callbackTester.hasCallback("classEnd"));
        
        // Verify expressions in coroutine contexts
        assertTrue(callbackTester.hasCallback("exprStart"));
        assertTrue(callbackTester.hasCallback("exprEnd"));
        
        // Multiple functions should be parsed
        int methodCount = callbackTester.countCallbacks("methodStart");
        assertTrue("Should parse class methods and top-level functions", methodCount >= 3);
        
        // Advanced Kotlin features should maintain structural integrity
        assertTrue("Advanced Kotlin features should maintain balance", callbackTester.isBalanced());
    }
    
    @Test
    public void testHighPerformancePSITreeWalking() throws Exception {
        // Given: Large Kotlin code structure for performance testing
        StringBuilder largeKotlinCode = new StringBuilder();
        largeKotlinCode.append("class LargeKotlinClass {\n");
        
        // Generate many properties and methods
        for (int i = 0; i < 50; i++) {
            largeKotlinCode.append("    val property").append(i).append(": String = \"value").append(i).append("\"\n");
            largeKotlinCode.append("    fun method").append(i).append("(): Int = ").append(i).append("\n");
        }
        
        largeKotlinCode.append("}\n");
        
        document.setContent(largeKotlinCode.toString());
        
        // When: Parse with performance measurement
        long startTime = System.nanoTime();
        adapter.parseWithStrategy(largeKotlinCode.toString());
        long endTime = System.nanoTime();
        
        // Then: Verify high-performance characteristics
        double executionMs = (endTime - startTime) / 1_000_000.0;
        assertTrue("K2 integration should maintain high performance", executionMs < 200);
        
        // Verify comprehensive callback generation
        int callbackCount = callbackTester.getCallbackCount();
        assertTrue("Large structure should generate many callbacks", callbackCount > 100);
        
        // Verify structural integrity at scale
        assertTrue("Large structures should maintain balance", callbackTester.isBalanced());
        
        // Verify expected callback patterns
        int fieldCount = callbackTester.countCallbacks("fieldDeclaration");
        int methodCount = callbackTester.countCallbacks("methodStart");
        assertEquals("Should detect all properties", 50, fieldCount);
        assertEquals("Should detect all methods", 50, methodCount);
    }
    
    @Test
    public void testTypeSafeNodeConversionAndErrorHandling() throws Exception {
        // Given: Kotlin code with potential parsing challenges
        String challengingKotlinCode = """
            class TypeSafetyTest {
                // Generic type with constraints
                fun <T : Comparable<T>> sortList(list: MutableList<T>): List<T> {
                    return list.sorted()
                }
                
                // Complex type expressions
                val complexType: Map<String, List<Pair<Int, String?>>> = mapOf(
                    "key1" to listOf(1 to "value1", 2 to null),
                    "key2" to listOf(3 to "value3")
                )
                
                // Lambda with receiver
                fun buildString(): String = buildString {
                    append("Hello")
                    append(" ")
                    append("Kotlin")
                }
                
                // Nullable types and safe calls
                fun processNullable(value: String?): Int? {
                    return value?.length?.let { it * 2 }
                }
            }
            """;
        
        document.setContent(challengingKotlinCode);
        
        // When: Parse challenging type structures
        try {
            adapter.parseWithStrategy(challengingKotlinCode);
            // Should not throw - if we get here, test passes this assertion
        } catch (Exception e) {
            fail("Should not throw exception during type-safe handling: " + e.getMessage());
        }
        
        // Then: Verify type-safe handling
        assertTrue(callbackTester.hasCallback("classStart"));
        assertTrue(callbackTester.hasCallback("methodStart"));
        assertTrue(callbackTester.hasCallback("fieldDeclaration"));
        assertTrue(callbackTester.hasCallback("exprStart"));
        assertTrue(callbackTester.hasCallback("exprEnd"));
        assertTrue(callbackTester.hasCallback("methodEnd"));
        assertTrue(callbackTester.hasCallback("classEnd"));
        
        // Verify robust error recovery
        assertTrue("K2 integration supports error recovery", adapter.supportsErrorRecovery());
        
        // Structural integrity maintained with complex types
        assertTrue("Complex types should maintain structural integrity", callbackTester.isBalanced());
    }
    
    @Test
    public void testKotlinInteropWithJavaStructures() throws Exception {
        // Given: Kotlin code that interoperates with Java
        String interopKotlinCode = """
            import java.util.concurrent.CompletableFuture
            import javax.annotation.Nullable
            
            class KotlinJavaInterop : JavaInterface {
                @JvmStatic
                fun staticMethod(): String = "static"
                
                @JvmOverloads
                fun overloadedMethod(param1: String, param2: Int = 0): String {
                    return "$param1:$param2"
                }
                
                override fun javaMethod(@Nullable value: String?): CompletableFuture<String> {
                    return CompletableFuture.completedFuture(value ?: "default")
                }
                
                companion object {
                    @JvmField
                    val JAVA_FIELD = "accessible from Java"
                }
            }
            
            interface JavaInterface {
                fun javaMethod(value: String?): CompletableFuture<String>
            }
            """;
        
        document.setContent(interopKotlinCode);
        
        // When: Parse Kotlin-Java interop structures
        adapter.parseWithStrategy(interopKotlinCode);
        
        // Then: Verify interop structure handling
        assertTrue(callbackTester.hasCallback("importStart"));
        assertTrue(callbackTester.hasCallback("classStart"));
        assertTrue(callbackTester.hasCallback("methodStart"));
        assertTrue(callbackTester.hasCallback("parameter"));
        assertTrue(callbackTester.hasCallback("methodEnd"));
        assertTrue(callbackTester.hasCallback("classEnd"));
        
        // Multiple methods should be detected
        int methodCount = callbackTester.countCallbacks("methodStart");
        assertTrue("Should detect interop methods", methodCount >= 2);
        
        // Interface should also be parsed
        int classCount = callbackTester.countCallbacks("classStart");
        assertEquals("Should parse both class and interface", 2, classCount);
        
        // Interop annotations should not break structural integrity
        assertTrue("Interop structures should maintain balance", callbackTester.isBalanced());
    }
    
    @Test
    public void testIntegrationWithTestableDocumentAndEntityResolver() throws Exception {
        // Given: Kotlin code with entity references
        String kotlinCode = """
            import kotlin.collections.List
            import kotlinx.coroutines.flow.Flow
            
            class EntityIntegrationTest {
                private val repository: DataRepository = DataRepository()
                
                suspend fun processEntities(): Flow<Entity> {
                    return repository.getAllEntities()
                        .map { entity -> entity.transform() }
                        .filter { it.isValid() }
                }
                
                inner class Entity {
                    fun transform(): ProcessedEntity = ProcessedEntity()
                    fun isValid(): Boolean = true
                }
                
                class ProcessedEntity
            }
            """;
        
        document.setContent(kotlinCode);
        
        // When: Parse with entity resolution
        adapter.parseWithStrategy(kotlinCode);
        
        // Then: Verify entity resolution integration
        assertTrue("EntityResolver should process Kotlin entity references",
                  entityResolver.hasResolvedEntities());
        
        // Verify document state preservation
        assertEquals("Document content should be preserved", kotlinCode, document.getContent());
        
        // Verify complete callback integration
        assertTrue(callbackTester.hasCallback("importStart"));
        assertTrue(callbackTester.hasCallback("classStart"));
        assertTrue(callbackTester.hasCallback("fieldDeclaration"));
        assertTrue(callbackTester.hasCallback("methodStart"));
        assertTrue(callbackTester.hasCallback("methodEnd"));
        assertTrue(callbackTester.hasCallback("classEnd"));
        
        // Verify nested class handling
        int classCount = callbackTester.countCallbacks("classStart");
        assertEquals("Should handle outer, inner, and nested classes", 3, classCount);
        
        // Integration should maintain structural integrity
        assertTrue("Entity integration should maintain balance", callbackTester.isBalanced());
    }
    
    @Test
    public void testK2ParserCharacteristicValidation() throws Exception {
        // Given: Code that showcases K2 parser strengths
        String kotlinCode = """
            @JvmInline
            value class UserId(val value: Long)
            
            sealed interface Result<out T>
            data class Success<T>(val data: T) : Result<T>
            data class Failure(val error: String) : Result<Nothing>
            
            class K2StrengthTest {
                context(CoroutineScope)
                suspend fun contextualFunction(): Result<String> = try {
                    Success(fetchData())
                } catch (e: Exception) {
                    Failure(e.message ?: "Unknown error")
                }
                
                private suspend fun fetchData(): String = "data"
            }
            """;
        
        document.setContent(kotlinCode);
        
        // When: Parse with K2 integration
        adapter.parseWithStrategy(kotlinCode);
        
        // Then: Verify K2 parser characteristics
        // 1. Comprehensive language feature handling
        assertTrue(callbackTester.hasCallback("classStart"));
        assertTrue(callbackTester.hasCallback("methodStart"));
        assertTrue(callbackTester.hasCallback("exprStart"));
        
        // 2. High-performance tree walking
        int callbackCount = callbackTester.getCallbackCount();
        assertTrue("K2 should generate comprehensive callbacks", callbackCount > 10);
        
        // 3. Strong structural integrity
        assertTrue("K2 maintains structural integrity", callbackTester.isBalanced());
        
        // 4. Type-safe node conversion
        assertTrue("K2 provides type-safe error handling", adapter.supportsErrorRecovery());
        
        // 5. Multiple class types handled
        int classCount = callbackTester.countCallbacks("classStart");
        assertTrue("K2 should handle value, sealed, and regular classes", classCount >= 3);
    }
    
    // ==================== Mock Infrastructure ====================
    
    /**
     * Mock TestableDocument for E2E testing.
     */
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
    
    /**
     * Mock EntityResolver for E2E testing.
     */
    private static class MockEntityResolver implements EntityResolver {
        private boolean hasResolvedEntities = false;
        
        @Override
        public ValueEntity getValueEntity(String name, Reflective querySource) {
            hasResolvedEntities = true;
            return null;
        }
        
        @Override
        public PackageOrClass resolvePackageOrClass(String name, Reflective querySource) {
            hasResolvedEntities = true;
            return null;
        }
        
        @Override
        public TypeEntity resolveQualifiedClass(String name) {
            hasResolvedEntities = true;
            return null;
        }
        
        public boolean hasResolvedEntities() {
            return hasResolvedEntities;
        }
    }
    
    /**
     * Mock JavaEntity for testing.
     */
    private static class MockJavaEntity extends JavaEntity {
        private final String name;
        
        public MockJavaEntity(String name) {
            this.name = name;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public JavaType getType() {
            // Mock implementation - return null for simplicity
            return null;
        }
        
        @Override
        public JavaEntity getSubentity(String name, Reflective querySource) {
            // Mock implementation - return null for simplicity
            return null;
        }
        
        @Override
        public JavaEntity setTypeArgs(List<TypeArgumentEntity> typeArgs) {
            // Mock implementation - no action needed
            return this;
        }
    }
    
    /**
     * Mock CallbackDelegate that integrates with CallbackTestingUtility.
     */
    private static class MockCallbackDelegate extends CallbackTestingUtility implements CallbackDelegate {
        // Inherits all CallbackDelegate methods from CallbackTestingUtility
        // This provides automatic callback recording and validation
    }
}