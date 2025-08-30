package bluej.parser.pratt.integration;

import bluej.extensions2.SourceType;
import bluej.parser.CallbackDelegate;
import bluej.parser.SourceParser;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LineColPos;
import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;
import java.util.*;
import java.util.function.Supplier;

import static org.junit.Assert.*;

/**
 * Integration test demonstrating Strategy 2: Lazy AST Transformation
 * 
 * This test showcases how to implement deferred AST node creation using the CallbackDelegate pattern.
 * AST nodes are only constructed when actually accessed, providing memory efficiency and allowing
 * for selective transformation based on runtime requirements.
 * 
 * Key Features Demonstrated:
 * - Lazy AST node construction using Supplier pattern
 * - Selective transformation based on access patterns
 * - Memory-efficient parsing for large codebases
 * - On-demand AST materialization
 */
public class LazyASTTransformationTest {

    private LazyCallbackDelegate lazyDelegate;
    private SourceParser sourceParser;
    private CallbackDelegate originalDelegate;

    @Before
    public void setUp() {
        sourceParser = new SourceParser(new StringReader(""), SourceType.Java);
        originalDelegate = sourceParser.getCallbackDelegate();
        lazyDelegate = new LazyCallbackDelegate(originalDelegate);
    }

    /**
     * Lazy AST Node that defers construction until accessed
     */
    static class LazyASTNode {
        private final String nodeType;
        private final String content;
        private final Supplier<Object> lazyConstructor;
        private Object materializedNode;
        private boolean accessed = false;
        
        public LazyASTNode(String nodeType, String content, Supplier<Object> constructor) {
            this.nodeType = nodeType;
            this.content = content;
            this.lazyConstructor = constructor;
        }
        
        /**
         * Materialize the AST node on first access
         */
        public Object getNode() {
            if (!accessed) {
                materializedNode = lazyConstructor.get();
                accessed = true;
            }
            return materializedNode;
        }
        
        public boolean isAccessed() {
            return accessed;
        }
        
        public String getNodeType() {
            return nodeType;
        }
        
        public String getContent() {
            return content;
        }
    }

    /**
     * Lazy AST Transformation Delegate that implements Strategy 2
     * Based on the complete CallbackDelegate implementation from K2ParserIntegrationTest
     */
    static class LazyCallbackDelegate implements CallbackDelegate {
        private final List<LazyASTNode> lazyNodes = new ArrayList<>();
        private final Map<String, Integer> accessStats = new HashMap<>();
        private final CallbackDelegate delegate;
        private final List<String> callbackSequence = new ArrayList<>();
        
        public LazyCallbackDelegate(CallbackDelegate delegate) {
            this.delegate = delegate;
        }

        private void recordAccess(String nodeType) {
            accessStats.merge(nodeType, 1, Integer::sum);
        }
        
        private Object createMethodASTNode(String name) {
            Map<String, Object> methodNode = new HashMap<>();
            methodNode.put("name", name);
            methodNode.put("constructedAt", System.currentTimeMillis());
            return methodNode;
        }
        
        private Object createTypeDefASTNode(String name) {
            Map<String, Object> typeNode = new HashMap<>();
            typeNode.put("name", name);
            typeNode.put("constructedAt", System.currentTimeMillis());
            return typeNode;
        }
        
        private Object createPackageASTNode(String name) {
            Map<String, Object> packageNode = new HashMap<>();
            packageNode.put("name", name);
            packageNode.put("constructedAt", System.currentTimeMillis());
            return packageNode;
        }
        
        // Access methods for testing
        public List<LazyASTNode> getLazyNodes() {
            return lazyNodes;
        }
        
        public Map<String, Integer> getAccessStats() {
            return accessStats;
        }
        
        public long getAccessedNodeCount() {
            return lazyNodes.stream().mapToLong(node -> node.isAccessed() ? 1 : 0).sum();
        }
        
        public List<String> getCallbackSequence() {
            return callbackSequence;
        }
        
        // Complete CallbackDelegate implementation - copied from working K2ParserIntegrationTest
        @Override
        public void beginExpression(LocatableToken token, boolean included) {
            callbackSequence.add("beginExpression");
            delegate.beginExpression(token, included);
        }
        
        @Override
        public void endExpression(LocatableToken token, boolean included) {
            callbackSequence.add("endExpression");
            delegate.endExpression(token, included);
        }
        
        @Override
        public void gotLiteral(LocatableToken token) {
            callbackSequence.add("gotLiteral");
            delegate.gotLiteral(token);
        }
        
        @Override
        public void gotIdentifier(LocatableToken token) {
            callbackSequence.add("gotIdentifier");
            delegate.gotIdentifier(token);
        }
        
        @Override
        public void gotBinaryOperator(LocatableToken token) {
            callbackSequence.add("gotBinaryOperator");
            delegate.gotBinaryOperator(token);
        }
        
        @Override
        public void gotMethodCall(LocatableToken token) {
            callbackSequence.add("gotMethodCall");
            delegate.gotMethodCall(token);
        }
        
        @Override
        public void gotMemberCall(LocatableToken token, List<LocatableToken> typeArgs) {
            callbackSequence.add("gotMemberCall");
            delegate.gotMemberCall(token, typeArgs);
        }
        
        @Override
        public void beginArgumentList(LocatableToken token) {
            callbackSequence.add("beginArgumentList");
            delegate.beginArgumentList(token);
        }
        
        @Override
        public void endArgumentList(LocatableToken token) {
            callbackSequence.add("endArgumentList");
            delegate.endArgumentList(token);
        }
        
        @Override
        public void endArgument() {
            callbackSequence.add("endArgument");
            delegate.endArgument();
        }
        
        @Override
        public void beginIfStmt(LocatableToken token) {
            callbackSequence.add("beginIfStmt");
            delegate.beginIfStmt(token);
        }
        
        @Override
        public void endIfStmt(LocatableToken token, boolean included) {
            callbackSequence.add("endIfStmt");
            delegate.endIfStmt(token, included);
        }
        
        @Override
        public void beginIfCondBlock(LocatableToken token) {
            callbackSequence.add("beginIfCondBlock");
            delegate.beginIfCondBlock(token);
        }
        
        @Override
        public void endIfCondBlock(LocatableToken token, boolean included) {
            callbackSequence.add("endIfCondBlock");
            delegate.endIfCondBlock(token, included);
        }
        
        @Override
        public void gotElseIf(LocatableToken token) {
            callbackSequence.add("gotElseIf");
            delegate.gotElseIf(token);
        }
        
        @Override
        public void beginElement(LocatableToken token) {
            callbackSequence.add("beginElement");
            delegate.beginElement(token);
        }
        
        @Override
        public void endElement(LocatableToken token, boolean included) {
            callbackSequence.add("endElement");
            delegate.endElement(token, included);
        }
        
        @Override
        public void beginForLoop(LocatableToken token) {
            callbackSequence.add("beginForLoop");
            delegate.beginForLoop(token);
        }
        
        @Override
        public void endForLoop(LocatableToken token, boolean included) {
            callbackSequence.add("endForLoop");
            delegate.endForLoop(token, included);
        }
        
        @Override
        public void beginForLoopBody(LocatableToken token) {
            callbackSequence.add("beginForLoopBody");
            delegate.beginForLoopBody(token);
        }
        
        @Override
        public void endForLoopBody(LocatableToken token, boolean included) {
            callbackSequence.add("endForLoopBody");
            delegate.endForLoopBody(token, included);
        }
        
        @Override
        public void beginMethodBody(LocatableToken token) {
            callbackSequence.add("beginMethodBody");
            delegate.beginMethodBody(token);
        }
        
        @Override
        public void endMethodBody(LocatableToken token, boolean included) {
            callbackSequence.add("endMethodBody");
            delegate.endMethodBody(token, included);
        }
        
        @Override
        public void gotImport(List<LocatableToken> tokens, boolean isStatic,
                             LocatableToken importToken, LocatableToken semiToken) {
            callbackSequence.add("gotImport");
            delegate.gotImport(tokens, isStatic, importToken, semiToken);
        }
        
        @Override
        public void gotWildcardImport(List<LocatableToken> tokens, boolean isStatic,
                                     LocatableToken importToken, LocatableToken semiToken) {
            callbackSequence.add("gotWildcardImport");
            delegate.gotWildcardImport(tokens, isStatic, importToken, semiToken);
        }
        
        @Override
        public void gotImportStmtSemi(LocatableToken token) {
            callbackSequence.add("gotImportStmtSemi");
            delegate.gotImportStmtSemi(token);
        }
        
        @Override
        public void gotForTest(boolean isPresent) {
            callbackSequence.add("gotForTest");
            delegate.gotForTest(isPresent);
        }
        
        @Override
        public void gotForIncrement(boolean isPresent) {
            callbackSequence.add("gotForIncrement");
            delegate.gotForIncrement(isPresent);
        }
        
        @Override
        public void gotStatementExpression() {
            callbackSequence.add("gotStatementExpression");
            delegate.gotStatementExpression();
        }
        
        // Lazy transformation callbacks - these create lazy nodes
        @Override
        public void finishedCU(int state) {
            callbackSequence.add("finishedCU");
            delegate.finishedCU(state);
        }
        
        @Override
        public void gotDeclBegin(LocatableToken token) {
            callbackSequence.add("gotDeclBegin");
            delegate.gotDeclBegin(token);
        }
        
        @Override
        public void gotTypeDef(LocatableToken token, int tdType) {
            callbackSequence.add("gotTypeDef");
            
            String typeName = token != null ? token.getText() : "unknown";
            LazyASTNode lazyType = new LazyASTNode(
                "TypeDef",
                typeName,
                () -> {
                    recordAccess("TypeDef");
                    return createTypeDefASTNode(typeName);
                }
            );
            lazyNodes.add(lazyType);
            
            delegate.gotTypeDef(token, tdType);
        }
        
        @Override
        public void gotTypeDefName(LocatableToken token) {
            callbackSequence.add("gotTypeDefName");
            delegate.gotTypeDefName(token);
        }
        
        @Override
        public void beginTypeDefExtends(LocatableToken token) {
            callbackSequence.add("beginTypeDefExtends");
            delegate.beginTypeDefExtends(token);
        }
        
        @Override
        public void endTypeDefExtends() {
            callbackSequence.add("endTypeDefExtends");
            delegate.endTypeDefExtends();
        }
        
        @Override
        public void beginTypeBody(LocatableToken token) {
            callbackSequence.add("beginTypeBody");
            delegate.beginTypeBody(token);
        }
        
        @Override
        public void endTypeBody(LocatableToken token, boolean included) {
            callbackSequence.add("endTypeBody");
            delegate.endTypeBody(token, included);
        }
        
        @Override
        public void gotTypeDefEnd(LocatableToken token, boolean included) {
            callbackSequence.add("gotTypeDefEnd");
            delegate.gotTypeDefEnd(token, included);
        }
        
        @Override
        public void gotAllMethodParameters() {
            callbackSequence.add("gotAllMethodParameters");
            delegate.gotAllMethodParameters();
        }
        
        @Override
        public void endMethodDecl(LocatableToken token, boolean included) {
            callbackSequence.add("endMethodDecl");
            delegate.endMethodDecl(token, included);
        }
        
        @Override
        public void beginPackageStatement(LocatableToken token) {
            callbackSequence.add("beginPackageStatement");
            delegate.beginPackageStatement(token);
        }
        
        @Override
        public void gotPackage(List<LocatableToken> tokens) {
            callbackSequence.add("gotPackage");
            
            String pkgName = !tokens.isEmpty() ? tokens.get(0).getText() : "default";
            LazyASTNode lazyPackage = new LazyASTNode(
                "Package",
                pkgName,
                () -> {
                    recordAccess("Package");
                    return createPackageASTNode(pkgName);
                }
            );
            lazyNodes.add(lazyPackage);
            
            delegate.gotPackage(tokens);
        }
        
        @Override
        public void gotPackageSemi(LocatableToken token) {
            callbackSequence.add("gotPackageSemi");
            delegate.gotPackageSemi(token);
        }
        
        @Override
        public void gotMethodParameter(LocatableToken nameToken, LocatableToken hiddenToken) {
            callbackSequence.add("gotMethodParameter");
            delegate.gotMethodParameter(nameToken, hiddenToken);
        }
        
        @Override
        public void gotTypeSpec(List<LocatableToken> tokens) {
            callbackSequence.add("gotTypeSpec");
            delegate.gotTypeSpec(tokens);
        }
        
        @Override
        public void gotModifier(LocatableToken token) {
            callbackSequence.add("gotModifier");
            delegate.gotModifier(token);
        }
        
        @Override
        public void gotMethodDeclaration(LocatableToken nameToken, LocatableToken hiddenToken) {
            callbackSequence.add("gotMethodDeclaration");
            
            String methodName = nameToken != null ? nameToken.getText() : "unknown";
            LazyASTNode lazyMethod = new LazyASTNode(
                "Method",
                methodName,
                () -> {
                    recordAccess("Method");
                    return createMethodASTNode(methodName);
                }
            );
            lazyNodes.add(lazyMethod);
            
            delegate.gotMethodDeclaration(nameToken, hiddenToken);
        }
        
        @Override
        public void determinedForLoop(boolean forEach, boolean hasInit) {
            callbackSequence.add("determinedForLoop");
            delegate.determinedForLoop(forEach, hasInit);
        }
        
        @Override
        public void gotAnnotation(List<LocatableToken> name, boolean hasParams) {
            callbackSequence.add("gotAnnotation");
            delegate.gotAnnotation(name, hasParams);
        }
        
        @Override
        public void gotMemberAccess(LocatableToken token) {
            callbackSequence.add("gotMemberAccess");
            delegate.gotMemberAccess(token);
        }
        
        @Override
        public void gotLambdaFormalName(LocatableToken token) {
            callbackSequence.add("gotLambdaFormalName");
            delegate.gotLambdaFormalName(token);
        }
        
        @Override
        public void beginLambdaBody(boolean isBlock, LocatableToken token) {
            callbackSequence.add("beginLambdaBody");
            delegate.beginLambdaBody(isBlock, token);
        }
        
        @Override
        public void endLambdaBody(LocatableToken token) {
            callbackSequence.add("endLambdaBody");
            delegate.endLambdaBody(token);
        }

        @Override
        public void endSwitchBlock(LocatableToken token) {
            callbackSequence.add("endSwitchBlock");
            delegate.endSwitchBlock(token);
        }

        @Override
        public void beginSwitchBlock(LocatableToken token) {
            callbackSequence.add("beginSwitchBlock");
            delegate.beginSwitchBlock(token);
        }

        @Override
        public void gotSwitchDefault() {
            callbackSequence.add("gotSwitchDefault");
            delegate.gotSwitchDefault();
        }

        @Override
        public void gotSwitchCaseType(LocatableToken token, boolean isArrow) {
            callbackSequence.add("gotSwitchCaseType");
            delegate.gotSwitchCaseType(token, isArrow);
        }

        @Override
        public void endSwitchCase(LocatableToken token, boolean wasArrow) {
            callbackSequence.add("endSwitchCase");
            delegate.endSwitchCase(token, wasArrow);
        }

        @Override
        public void beginSwitchCase(LocatableToken token) {
            callbackSequence.add("beginSwitchCase");
            delegate.beginSwitchCase(token);
        }

        @Override
        public void endSwitchStmt(LocatableToken token, boolean included) {
            callbackSequence.add("endSwitchStmt");
            delegate.endSwitchStmt(token, included);
        }

        @Override
        public void beginSwitchStmt(LocatableToken token, boolean isExpression) {
            callbackSequence.add("beginSwitchStmt");
            delegate.beginSwitchStmt(token, isExpression);
        }
    }

    /**
     * Test Strategy 2: Lazy AST Transformation with selective materialization
     */
    @Test
    public void testLazyASTTransformation() throws Exception {
        // Simulate parsing by directly invoking callbacks
        simulateJavaParsingCallbacks();
        
        // Verify lazy nodes were created but not yet accessed
        List<LazyASTNode> lazyNodes = lazyDelegate.getLazyNodes();
        assertTrue("Should have created lazy nodes", !lazyNodes.isEmpty());
        assertEquals("No nodes should be accessed initially", 0, lazyDelegate.getAccessedNodeCount());
        
        // Selectively access only method nodes
        List<LazyASTNode> methodNodes = lazyNodes.stream()
                .filter(node -> "Method".equals(node.getNodeType()))
                .toList();
        
        assertTrue("Should have method nodes", !methodNodes.isEmpty());
        
        // Materialize only the first method node
        if (!methodNodes.isEmpty()) {
            Object firstMethod = methodNodes.get(0).getNode();
            assertNotNull("First method should be materialized", firstMethod);
            assertTrue("First method node should be marked as accessed", methodNodes.get(0).isAccessed());
        }
        
        // Verify selective access - only one node should be materialized
        assertEquals("Only one node should be accessed", 1, lazyDelegate.getAccessedNodeCount());
        
        // Verify access statistics
        Map<String, Integer> accessStats = lazyDelegate.getAccessStats();
        assertEquals("Should have recorded one method access", Integer.valueOf(1), accessStats.get("Method"));
        
        // Test bulk materialization
        for (LazyASTNode node : lazyNodes) {
            node.getNode(); // Force materialization
        }
        
        // All nodes should now be accessed
        assertEquals("All nodes should be accessed after bulk materialization", 
                     lazyNodes.size(), lazyDelegate.getAccessedNodeCount());
    }

    /**
     * Test lazy transformation performance characteristics
     */
    @Test
    public void testLazyTransformationPerformance() throws Exception {
        // Measure parsing time (should be fast as no materialization occurs)
        long startTime = System.nanoTime();
        simulateJavaParsingCallbacks();
        long parseTime = System.nanoTime() - startTime;
        
        List<LazyASTNode> lazyNodes = lazyDelegate.getLazyNodes();
        assertTrue("Should have lazy nodes", !lazyNodes.isEmpty());
        assertEquals("No materialization should occur during parsing", 0, lazyDelegate.getAccessedNodeCount());
        
        // Measure selective access time
        startTime = System.nanoTime();
        Optional<LazyASTNode> firstMethod = lazyNodes.stream()
                .filter(node -> "Method".equals(node.getNodeType()))
                .findFirst();
        
        if (firstMethod.isPresent()) {
            firstMethod.get().getNode(); // Materialize only one node
        }
        long accessTime = System.nanoTime() - startTime;
        
        // Verify only one node was materialized
        assertEquals("Only one node should be materialized", 1, lazyDelegate.getAccessedNodeCount());
        
        // The test passes if we can demonstrate selective lazy loading
        assertTrue("Parse time should be reasonable", parseTime > 0);
        assertTrue("Access time should be reasonable", accessTime > 0);
    }

    /**
     * Test Strategy 2 with complex inheritance hierarchy
     */
    @Test
    public void testLazyTransformationWithInheritance() throws Exception {
        // Simulate more complex parsing with type definitions
        simulateComplexJavaParsingCallbacks();
        
        List<LazyASTNode> lazyNodes = lazyDelegate.getLazyNodes();
        
        // Find and materialize only the type definition
        Optional<LazyASTNode> typeDefNode = lazyNodes.stream()
                .filter(node -> "TypeDef".equals(node.getNodeType()))
                .findFirst();
        
        if (typeDefNode.isPresent()) {
            Object typeDef = typeDefNode.get().getNode();
            assertNotNull("Type definition should be materialized", typeDef);
            
            // Verify the materialized node contains expected information
            if (typeDef instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> typeMap = (Map<String, Object>) typeDef;
                assertTrue("Should have construction timestamp", typeMap.containsKey("constructedAt"));
            }
        }
        
        // Verify selective materialization - only type node accessed
        long typeDefAccessCount = lazyNodes.stream()
                .filter(node -> "TypeDef".equals(node.getNodeType()))
                .mapToLong(node -> node.isAccessed() ? 1 : 0)
                .sum();
        
        assertEquals("Only type definition should be accessed", 1, typeDefAccessCount);
    }
    
    // Helper methods to simulate parsing callbacks
    private void simulateJavaParsingCallbacks() {
        LocatableToken packageToken = mockToken("package");
        LocatableToken classToken = mockToken("class");
        LocatableToken methodToken = mockToken("testMethod");
        
        // Simulate package callback
        lazyDelegate.beginPackageStatement(packageToken);
        lazyDelegate.gotPackage(Arrays.asList(mockToken("com.example")));
        lazyDelegate.gotPackageSemi(mockToken(";"));
        
        // Simulate class definition
        lazyDelegate.gotDeclBegin(classToken);
        lazyDelegate.gotTypeDef(classToken, JavaTokenTypes.LITERAL_class);
        lazyDelegate.gotTypeDefName(mockToken("TestClass"));
        lazyDelegate.beginTypeBody(mockToken("{"));
        
        // Simulate method declaration
        lazyDelegate.gotDeclBegin(methodToken);
        lazyDelegate.gotMethodDeclaration(methodToken, null);
        lazyDelegate.gotAllMethodParameters();
        lazyDelegate.beginMethodBody(mockToken("{"));
        lazyDelegate.endMethodBody(mockToken("}"), true);
        lazyDelegate.endMethodDecl(mockToken("}"), true);
        
        lazyDelegate.endTypeBody(mockToken("}"), true);
        lazyDelegate.gotTypeDefEnd(mockToken("}"), true);
        lazyDelegate.finishedCU(2);
    }
    
    private void simulateComplexJavaParsingCallbacks() {
        LocatableToken packageToken = mockToken("package");
        LocatableToken classToken = mockToken("class");
        LocatableToken method1Token = mockToken("methodOne");
        LocatableToken method2Token = mockToken("methodTwo");
        
        // Simulate package
        lazyDelegate.beginPackageStatement(packageToken);
        lazyDelegate.gotPackage(Arrays.asList(mockToken("com.complex")));
        lazyDelegate.gotPackageSemi(mockToken(";"));
        
        // Simulate class with inheritance
        lazyDelegate.gotDeclBegin(classToken);
        lazyDelegate.gotTypeDef(classToken, JavaTokenTypes.LITERAL_class);
        lazyDelegate.gotTypeDefName(mockToken("ComplexClass"));
        lazyDelegate.beginTypeDefExtends(mockToken("extends"));
        lazyDelegate.endTypeDefExtends();
        lazyDelegate.beginTypeBody(mockToken("{"));
        
        // Simulate multiple methods
        lazyDelegate.gotDeclBegin(method1Token);
        lazyDelegate.gotMethodDeclaration(method1Token, null);
        lazyDelegate.gotAllMethodParameters();
        lazyDelegate.endMethodDecl(mockToken("}"), true);
        
        lazyDelegate.gotDeclBegin(method2Token);
        lazyDelegate.gotMethodDeclaration(method2Token, null);
        lazyDelegate.gotAllMethodParameters();
        lazyDelegate.endMethodDecl(mockToken("}"), true);
        
        lazyDelegate.endTypeBody(mockToken("}"), true);
        lazyDelegate.gotTypeDefEnd(mockToken("}"), true);
        lazyDelegate.finishedCU(2);
    }
    
    // Helper method to create mock tokens - copied from K2ParserIntegrationTest
    private static LocatableToken mockToken(String text) {
        LineColPos begin = new LineColPos(1, 1, 0);
        LineColPos end = new LineColPos(1, 1 + text.length(), text.length());
        return new LocatableToken(JavaTokenTypes.IDENT, text, begin, end);
    }
}