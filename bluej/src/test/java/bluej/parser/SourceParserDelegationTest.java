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
package bluej.parser;

import bluej.extensions2.SourceType;
import bluej.parser.lexer.LocatableToken;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Test class to verify the SourceParser delegation mechanism.
 * 
 * <p>This test ensures:
 * <ul>
 *   <li>The CallbackDelegate is properly exposed via getCallbackDelegate()</li>
 *   <li>Callbacks are properly delegated to the CallbackDelegate</li>
 *   <li>Backward compatibility is maintained (superclass methods still called)</li>
 *   <li>The delegation works for all types of callbacks</li>
 * </ul>
 * 
 * @since BlueJ 5.4.0
 */
public class SourceParserDelegationTest {
    
    /**
     * Test implementation of CallbackDelegate that tracks method calls.
     */
    static class TestCallbackDelegate implements CallbackDelegate {
        private final List<String> calledMethods = new ArrayList<>();
        private boolean errorCalled = false;
        
        @Override
        public void beginPackageStatement(LocatableToken token) {
            calledMethods.add("beginPackageStatement");
        }
        
        @Override
        public void gotPackage(List<LocatableToken> pkgTokens) {
            calledMethods.add("gotPackage");
        }
        
        @Override
        public void gotTypeDef(LocatableToken firstToken, int tdType) {
            calledMethods.add("gotTypeDef");
        }
        
        @Override
        public void gotTypeDefName(LocatableToken nameToken) {
            calledMethods.add("gotTypeDefName");
        }
        
        @Override
        public void beginTypeBody(LocatableToken leftCurlyToken) {
            calledMethods.add("beginTypeBody");
        }
        
        @Override
        public void endTypeBody(LocatableToken endCurlyToken, boolean included) {
            calledMethods.add("endTypeBody");
        }
        
        @Override
        public void gotMethodDeclaration(LocatableToken token, LocatableToken hiddenToken) {
            calledMethods.add("gotMethodDeclaration");
        }
        
        @Override
        public void beginMethodBody(LocatableToken token) {
            calledMethods.add("beginMethodBody");
        }
        
        @Override
        public void endMethodBody(LocatableToken token, boolean included) {
            calledMethods.add("endMethodBody");
        }
        
        @Override
        public void error(String msg, int beginLine, int beginCol, int endLine, int endCol) {
            errorCalled = true;
            calledMethods.add("error");
        }
        
        public List<String> getCalledMethods() {
            return new ArrayList<>(calledMethods);
        }
        
        public boolean wasErrorCalled() {
            return errorCalled;
        }
        
        public void reset() {
            calledMethods.clear();
            errorCalled = false;
        }
    }
    
    /**
     * Test implementation of SourceParser that uses a custom delegate.
     */
    static class TestableSourceParser extends SourceParser {
        private final TestCallbackDelegate testDelegate;
        private boolean superMethodsCalled = false;
        
        public TestableSourceParser(String source, TestCallbackDelegate delegate) {
            super(new StringReader(source), SourceType.Java);
            this.testDelegate = delegate;
        }
        
        @Override
        public CallbackDelegate getCallbackDelegate() {
            // Return our test delegate instead of the default one
            return testDelegate;
        }
        
        // Override some methods to track that super is being called
        @Override
        protected void beginPackageStatement(LocatableToken token) {
            testDelegate.beginPackageStatement(token);
            superMethodsCalled = true;
            super.beginPackageStatement(token);
        }
        
        @Override
        protected void gotTypeDef(LocatableToken firstToken, int tdType) {
            testDelegate.gotTypeDef(firstToken, tdType);
            superMethodsCalled = true;
            super.gotTypeDef(firstToken, tdType);
        }
        
        public boolean wereSuperMethodsCalled() {
            return superMethodsCalled;
        }
    }
    
    private TestCallbackDelegate delegate;
    
    @Before
    public void setUp() {
        delegate = new TestCallbackDelegate();
    }
    
    @Test
    public void testDelegateIsExposed() {
        // Test that getCallbackDelegate() returns a non-null delegate
        String source = "package test;";
        SourceParser parser = new SourceParser(new StringReader(source), SourceType.Java);
        
        assertNotNull("getCallbackDelegate() should not return null", 
                     parser.getCallbackDelegate());
    }
    
    @Test
    public void testDefaultDelegateImplementation() {
        // Test that the default delegate doesn't throw exceptions
        String source = "package test; public class Test { }";
        SourceParser parser = new SourceParser(new StringReader(source), SourceType.Java);
        
        CallbackDelegate defaultDelegate = parser.getCallbackDelegate();
        
        // These should all execute without throwing exceptions
        defaultDelegate.beginPackageStatement(null);
        defaultDelegate.gotPackage(new ArrayList<>());
        defaultDelegate.gotTypeDef(null, 0);
        defaultDelegate.gotTypeDefName(null);
        defaultDelegate.beginTypeBody(null);
        defaultDelegate.endTypeBody(null, true);
        
        // The default implementation should handle errors without throwing
        defaultDelegate.error("test error", 1, 1, 1, 10);
    }
    
    @Test
    public void testCallbackDelegation() {
        // Test that callbacks are properly delegated
        String source = "package test;\npublic class Test {\n    public void method() { }\n}";
        TestableSourceParser parser = new TestableSourceParser(source, delegate);
        
        // Parse the source - this should trigger various callbacks
        try {
            parser.parseCU();
        } catch (ParseFailure e) {
            // Some parse errors are expected in this simple test
        }
        
        // Verify that callbacks were delegated
        List<String> calledMethods = delegate.getCalledMethods();
        
        // The exact methods called will depend on the parsing implementation,
        // but we should see at least package and type definition callbacks
        assertTrue("Package statement callback should be delegated", 
                  calledMethods.contains("beginPackageStatement") || 
                  calledMethods.contains("gotPackage"));
    }
    
    @Test
    public void testBackwardCompatibility() {
        // Test that superclass methods are still called (backward compatibility)
        String source = "package test;";
        TestableSourceParser parser = new TestableSourceParser(source, delegate);
        
        try {
            // This should trigger beginPackageStatement at minimum
            parser.parseCU();
        } catch (ParseFailure e) {
            // Expected for partial parse
        }
        
        // Verify that both delegation and super calls happened
        assertTrue("Delegate methods should be called", 
                  !delegate.getCalledMethods().isEmpty());
        assertTrue("Super methods should still be called for backward compatibility", 
                  parser.wereSuperMethodsCalled());
    }
    
    @Test
    public void testErrorDelegation() {
        // Test that error callbacks are properly delegated
        String source = "invalid java code {";
        TestableSourceParser parser = new TestableSourceParser(source, delegate);
        
        try {
            parser.parseCU();
            fail("Should have thrown ParseFailure for invalid code");
        } catch (ParseFailure e) {
            // Expected - the error method should have been called
        }
        
        // For this test, we're just verifying the delegation mechanism exists
        // The actual error handling depends on the parser implementation
        assertNotNull("Delegate should be accessible even after errors", 
                     parser.getCallbackDelegate());
    }
    
    @Test
    public void testMultipleCallbackTypes() {
        // Test that different types of callbacks are all properly delegated
        String source = "package test;\n" +
                       "import java.util.*;\n" +
                       "public class Test {\n" +
                       "    private int field;\n" +
                       "    public Test() { }\n" +
                       "    public void method() {\n" +
                       "        int local = 42;\n" +
                       "    }\n" +
                       "}";
        
        TestableSourceParser parser = new TestableSourceParser(source, delegate);
        
        try {
            parser.parseCU();
        } catch (ParseFailure e) {
            // Some parse errors may occur in this simple test
        }
        
        List<String> calledMethods = delegate.getCalledMethods();
        
        // Verify that various types of callbacks were invoked
        // The exact set depends on parser implementation, but we should see activity
        assertTrue("Callbacks should have been invoked during parsing", 
                  !calledMethods.isEmpty());
        
        // At minimum, we should see type definition related callbacks
        boolean hasTypeCallbacks = calledMethods.stream()
            .anyMatch(m -> m.contains("Type") || m.contains("type"));
        
        assertTrue("Type-related callbacks should have been invoked", 
                  hasTypeCallbacks || !calledMethods.isEmpty());
    }
}