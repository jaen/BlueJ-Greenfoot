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
package bluej.parser.pratt.integration;

import bluej.parser.CallbackDelegate;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.lexer.LocatableToken;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comprehensive testing utility for callback-based parser integration testing.
 * 
 * This class provides infrastructure for recording, validating, and analyzing
 * parser callbacks during integration testing. It implements CallbackDelegate
 * to capture all callback invocations and provides rich validation methods.
 * 
 * Key features:
 * - Records all callback invocations with metadata
 * - Validates callback balance (matching start/end pairs)
 * - Provides search and counting capabilities
 * - Tracks callback execution order and timing
 * - Supports nested callback scope validation
 * 
 * Used extensively by E2E integration tests and benchmark framework.
 */
public class CallbackTestingUtility implements CallbackDelegate {
    
    // Callback recording infrastructure
    private final List<CallbackRecord> callbackHistory = new ArrayList<>();
    private final Map<String, AtomicInteger> callbackCounts = new HashMap<>();
    private final Stack<String> callbackStack = new Stack<>();
    private boolean recordingEnabled = true;
    
    // Callback balance tracking
    private int classStartCount = 0;
    private int classEndCount = 0;
    private int methodStartCount = 0;
    private int methodEndCount = 0;
    private int exprStartCount = 0;
    private int exprEndCount = 0;
    
    /**
     * Factory method to create a CallbackTester for a given delegate.
     */
    public static CallbackTester forDelegate(CallbackDelegate delegate) {
        if (delegate == null) {
            // If no delegate provided, create a standalone utility
            return new CallbackTester(new CallbackTestingUtility());
        }
        if (delegate instanceof CallbackTestingUtility) {
            return new CallbackTester((CallbackTestingUtility) delegate);
        }
        // If delegate is not our utility, wrap it
        return new CallbackTester(new DelegatingCallbackTestingUtility(delegate));
    }
    
    // ==================== CallbackDelegate Implementation ====================
    
    @Override
    public void beginExpression(LocatableToken token, boolean included) {
        recordCallback("beginExpression", tokenToString(token) + ", included=" + included);
    }
    
    @Override
    public void endExpression(LocatableToken token, boolean included) {
        recordCallback("endExpression", tokenToString(token) + ", included=" + included);
    }
    
    @Override
    public void gotLiteral(LocatableToken token) {
        recordCallback("gotLiteral", tokenToString(token));
    }
    
    @Override
    public void gotIdentifier(LocatableToken token) {
        recordCallback("gotIdentifier", tokenToString(token));
    }
    
    @Override
    public void gotBinaryOperator(LocatableToken token) {
        recordCallback("gotBinaryOperator", tokenToString(token));
    }
    
    @Override
    public void gotMethodCall(LocatableToken token) {
        recordCallback("gotMethodCall", tokenToString(token));
    }
    
    @Override
    public void gotMemberCall(LocatableToken token, List<LocatableToken> typeArgs) {
        int typeArgCount = typeArgs != null ? typeArgs.size() : 0;
        recordCallback("gotMemberCall", tokenToString(token) + " with " + typeArgCount + " type args");
    }
    
    @Override
    public void beginArgumentList(LocatableToken token) {
        recordCallback("beginArgumentList", tokenToString(token));
    }
    
    @Override
    public void endArgumentList(LocatableToken token) {
        recordCallback("endArgumentList", tokenToString(token));
    }
    
    @Override
    public void endArgument() {
        recordCallback("endArgument", "");
    }
    
    @Override
    public void beginIfStmt(LocatableToken token) {
        recordCallback("beginIfStmt", tokenToString(token));
    }
    
    @Override
    public void endIfStmt(LocatableToken token, boolean included) {
        recordCallback("endIfStmt", tokenToString(token) + ", included=" + included);
    }
    
    @Override
    public void beginIfCondBlock(LocatableToken token) {
        recordCallback("beginIfCondBlock", tokenToString(token));
    }
    
    @Override
    public void endIfCondBlock(LocatableToken token, boolean included) {
        recordCallback("endIfCondBlock", tokenToString(token) + ", included=" + included);
    }
    
    @Override
    public void gotElseIf(LocatableToken token) {
        recordCallback("gotElseIf", tokenToString(token));
    }
    
    @Override
    public void beginElement(LocatableToken token) {
        recordCallback("beginElement", tokenToString(token));
    }
    
    @Override
    public void endElement(LocatableToken token, boolean included) {
        recordCallback("endElement", tokenToString(token) + ", included=" + included);
    }
    
    @Override
    public void beginForLoop(LocatableToken token) {
        recordCallback("beginForLoop", tokenToString(token));
    }
    
    @Override
    public void endForLoop(LocatableToken token, boolean included) {
        recordCallback("endForLoop", tokenToString(token) + ", included=" + included);
    }
    
    @Override
    public void beginForLoopBody(LocatableToken token) {
        recordCallback("beginForLoopBody", tokenToString(token));
    }
    
    @Override
    public void endForLoopBody(LocatableToken token, boolean included) {
        recordCallback("endForLoopBody", tokenToString(token) + ", included=" + included);
    }
    
    @Override
    public void beginMethodBody(LocatableToken token) {
        recordCallback("beginMethodBody", tokenToString(token));
    }
    
    @Override
    public void endMethodBody(LocatableToken token, boolean included) {
        recordCallback("endMethodBody", tokenToString(token) + ", included=" + included);
    }
    
    @Override
    public void gotImport(List<LocatableToken> tokens, boolean isStatic, LocatableToken importToken, LocatableToken semiToken) {
        recordCallback("gotImport", tokensToString(tokens) + ", static=" + isStatic);
    }
    
    @Override
    public void gotWildcardImport(List<LocatableToken> tokens, boolean isStatic, LocatableToken importToken, LocatableToken semiToken) {
        recordCallback("gotWildcardImport", tokensToString(tokens) + ", static=" + isStatic);
    }
    
    @Override
    public void gotImportStmtSemi(LocatableToken token) {
        recordCallback("gotImportStmtSemi", tokenToString(token));
    }
    
    @Override
    public void gotForTest(boolean isPresent) {
        recordCallback("gotForTest", "present=" + isPresent);
    }
    
    @Override
    public void gotForIncrement(boolean isPresent) {
        recordCallback("gotForIncrement", "present=" + isPresent);
    }
    
    @Override
    public void gotStatementExpression() {
        recordCallback("gotStatementExpression", "");
    }
    
    @Override
    public void finishedCU(int state) {
        recordCallback("finishedCU", "state=" + state);
    }
    
    @Override
    public void gotDeclBegin(LocatableToken token) {
        recordCallback("gotDeclBegin", tokenToString(token));
    }
    
    @Override
    public void gotTypeDef(LocatableToken token, int tdType) {
        recordCallback("gotTypeDef", tokenToString(token) + ", type=" + tdType);
    }
    
    @Override
    public void gotTypeDefName(LocatableToken token) {
        recordCallback("gotTypeDefName", tokenToString(token));
    }
    
    @Override
    public void beginTypeDefExtends(LocatableToken token) {
        recordCallback("beginTypeDefExtends", tokenToString(token));
    }
    
    @Override
    public void endTypeDefExtends() {
        recordCallback("endTypeDefExtends", "");
    }
    
    @Override
    public void beginTypeBody(LocatableToken token) {
        recordCallback("beginTypeBody", tokenToString(token));
    }
    
    @Override
    public void endTypeBody(LocatableToken token, boolean included) {
        recordCallback("endTypeBody", tokenToString(token) + ", included=" + included);
    }
    
    @Override
    public void gotTypeDefEnd(LocatableToken token, boolean included) {
        recordCallback("gotTypeDefEnd", tokenToString(token) + ", included=" + included);
    }
    
    @Override
    public void gotAllMethodParameters() {
        recordCallback("gotAllMethodParameters", "");
    }
    
    @Override
    public void endMethodDecl(LocatableToken token, boolean included) {
        recordCallback("endMethodDecl", tokenToString(token) + ", included=" + included);
    }
    
    @Override
    public void beginPackageStatement(LocatableToken token) {
        recordCallback("beginPackageStatement", tokenToString(token));
    }
    
    @Override
    public void gotPackage(List<LocatableToken> tokens) {
        recordCallback("gotPackage", tokensToString(tokens));
    }
    
    @Override
    public void gotPackageSemi(LocatableToken token) {
        recordCallback("gotPackageSemi", tokenToString(token));
    }
    
    @Override
    public void gotMethodParameter(LocatableToken nameToken, LocatableToken hiddenToken) {
        recordCallback("gotMethodParameter", tokenToString(nameToken) + ", hidden=" + tokenToString(hiddenToken));
    }
    
    @Override
    public void gotTypeSpec(List<LocatableToken> tokens) {
        recordCallback("gotTypeSpec", tokensToString(tokens));
    }
    
    @Override
    public void gotModifier(LocatableToken token) {
        recordCallback("gotModifier", tokenToString(token));
    }
    
    @Override
    public void gotMethodDeclaration(LocatableToken nameToken, LocatableToken hiddenToken) {
        recordCallback("gotMethodDeclaration", tokenToString(nameToken) + ", hidden=" + tokenToString(hiddenToken));
    }
    
    @Override
    public void beginSwitchStmt(LocatableToken token, boolean isExpression) {
        recordCallback("beginSwitchStmt", tokenToString(token) + ", expr=" + isExpression);
    }
    
    @Override
    public void endSwitchStmt(LocatableToken token, boolean included) {
        recordCallback("endSwitchStmt", tokenToString(token) + ", included=" + included);
    }
    
    @Override
    public void beginSwitchCase(LocatableToken token) {
        recordCallback("beginSwitchCase", tokenToString(token));
    }
    
    @Override
    public void endSwitchCase(LocatableToken token, boolean wasArrow) {
        recordCallback("endSwitchCase", tokenToString(token) + ", arrow=" + wasArrow);
    }
    
    @Override
    public void gotSwitchCaseType(LocatableToken token, boolean isArrow) {
        recordCallback("gotSwitchCaseType", tokenToString(token) + ", arrow=" + isArrow);
    }
    
    @Override
    public void gotSwitchDefault() {
        recordCallback("gotSwitchDefault", "");
    }
    
    @Override
    public void beginSwitchBlock(LocatableToken token) {
        recordCallback("beginSwitchBlock", tokenToString(token));
    }
    
    @Override
    public void endSwitchBlock(LocatableToken token) {
        recordCallback("endSwitchBlock", tokenToString(token));
    }
    
    @Override
    public void gotLambdaFormalName(LocatableToken token) {
        recordCallback("gotLambdaFormalName", tokenToString(token));
    }
    
    @Override
    public void beginLambdaBody(boolean isBlock, LocatableToken token) {
        recordCallback("beginLambdaBody", "block=" + isBlock + ", " + tokenToString(token));
    }
    
    @Override
    public void endLambdaBody(LocatableToken token) {
        recordCallback("endLambdaBody", tokenToString(token));
    }
    
    @Override
    public void gotMemberAccess(LocatableToken token) {
        recordCallback("gotMemberAccess", tokenToString(token));
    }
    
    @Override
    public void gotAnnotation(List<LocatableToken> name, boolean hasParams) {
        recordCallback("gotAnnotation", tokensToString(name) + ", hasParams=" + hasParams);
    }
    
    @Override
    public void determinedForLoop(boolean forEach, boolean hasInit) {
        recordCallback("determinedForLoop", "forEach=" + forEach + ", hasInit=" + hasInit);
    }
    
    // ==================== Helper Methods ====================
    
    private String tokenToString(LocatableToken token) {
        if (token == null) return "null";
        return token.getText() != null ? token.getText() : "token@" + token.getPosition();
    }
    
    private String tokensToString(List<LocatableToken> tokens) {
        if (tokens == null) return "null";
        return tokens.stream()
                .map(this::tokenToString)
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "." + b);
    }
    
    // Extended callback methods for advanced integration testing
    
    public void gotClassStart(String className) {
        classStartCount++;
        callbackStack.push("class:" + className);
        recordCallback("classStart", className);
    }
    
    public void gotClassEnd(String className) {
        classEndCount++;
        if (!callbackStack.isEmpty() && callbackStack.peek().startsWith("class:")) {
            callbackStack.pop();
        }
        recordCallback("classEnd", className);
    }
    
    public void gotMethodStart(String methodName) {
        methodStartCount++;
        callbackStack.push("method:" + methodName);
        recordCallback("methodStart", methodName);
    }
    
    public void gotMethodEnd(String methodName) {
        methodEndCount++;
        if (!callbackStack.isEmpty() && callbackStack.peek().startsWith("method:")) {
            callbackStack.pop();
        }
        recordCallback("methodEnd", methodName);
    }
    
    public void gotExprStart(String exprType) {
        exprStartCount++;
        callbackStack.push("expr:" + exprType);
        recordCallback("exprStart", exprType);
    }
    
    public void gotExprEnd(String exprType) {
        exprEndCount++;
        if (!callbackStack.isEmpty() && callbackStack.peek().startsWith("expr:")) {
            callbackStack.pop();
        }
        recordCallback("exprEnd", exprType);
    }
    
    public void gotImportStart(String importPath) {
        recordCallback("importStart", importPath);
    }
    
    public void gotImportEnd(String importPath) {
        recordCallback("importEnd", importPath);
    }
    
    public void gotFieldDeclaration(String fieldName, String fieldType) {
        recordCallback("fieldDeclaration", fieldName + ":" + fieldType);
    }
    
    // Additional legacy callback methods for test compatibility
    public void gotLambdaExpression(String parameters, String body) {
        recordCallback("lambdaExpression", parameters + " -> " + body);
    }
    
    public void gotMethodReference(String target, String method) {
        recordCallback("methodReference", target + "::" + method);
    }
    
    public void gotStreamOperation(String operation, String details) {
        recordCallback("streamOperation", operation + "(" + details + ")");
    }
    
    public void gotGenericType(String baseType, String[] typeParameters) {
        recordCallback("genericType", baseType + "<" + String.join(",", typeParameters) + ">");
    }
    
    // ==================== Recording Infrastructure ====================
    
    private void recordCallback(String callbackType, String details) {
        if (!recordingEnabled) return;
        
        CallbackRecord record = new CallbackRecord(
            callbackType, 
            details, 
            System.nanoTime(),
            callbackHistory.size()
        );
        callbackHistory.add(record);
        callbackCounts.computeIfAbsent(callbackType, k -> new AtomicInteger(0)).incrementAndGet();
    }
    
    // ==================== Validation and Query Methods ====================
    
    public boolean hasCallback(String callbackType) {
        return callbackCounts.containsKey(callbackType) && callbackCounts.get(callbackType).get() > 0;
    }
    
    public int countCallbacks(String callbackType) {
        return callbackCounts.getOrDefault(callbackType, new AtomicInteger(0)).get();
    }
    
    public int getCallbackCount() {
        return callbackHistory.size();
    }
    
    public boolean isBalanced() {
        return classStartCount == classEndCount &&
               methodStartCount == methodEndCount &&
               exprStartCount == exprEndCount &&
               callbackStack.isEmpty();
    }
    
    public void clear() {
        callbackHistory.clear();
        callbackCounts.clear();
        callbackStack.clear();
        classStartCount = classEndCount = 0;
        methodStartCount = methodEndCount = 0;
        exprStartCount = exprEndCount = 0;
    }
    
    public List<CallbackRecord> getCallbackHistory() {
        return new ArrayList<>(callbackHistory);
    }
    
    public Map<String, Integer> getCallbackCounts() {
        Map<String, Integer> result = new HashMap<>();
        callbackCounts.forEach((k, v) -> result.put(k, v.get()));
        return result;
    }
    
    public void setRecordingEnabled(boolean enabled) {
        this.recordingEnabled = enabled;
    }
    
    // ==================== CallbackRecord Inner Class ====================
    
    public static class CallbackRecord {
        private final String callbackType;
        private final String details;
        private final long timestamp;
        private final int sequenceNumber;
        
        public CallbackRecord(String callbackType, String details, long timestamp, int sequenceNumber) {
            this.callbackType = callbackType;
            this.details = details;
            this.timestamp = timestamp;
            this.sequenceNumber = sequenceNumber;
        }
        
        public String getCallbackType() { return callbackType; }
        public String getDetails() { return details; }
        public long getTimestamp() { return timestamp; }
        public int getSequenceNumber() { return sequenceNumber; }
        
        @Override
        public String toString() {
            return String.format("%d: %s(%s) @%d", sequenceNumber, callbackType, details, timestamp);
        }
    }
    
    // ==================== CallbackTester Inner Class ====================
    
    public static class CallbackTester {
        private final CallbackTestingUtility utility;
        
        public CallbackTester(CallbackTestingUtility utility) {
            this.utility = utility;
        }
        
        /**
         * Get the underlying CallbackDelegate for use in tests that expect the delegate interface.
         */
        public CallbackDelegate getDelegate() {
            return utility;
        }
        
        public boolean hasCallback(String callbackType) {
            return utility.hasCallback(callbackType);
        }
        
        public int countCallbacks(String callbackType) {
            return utility.countCallbacks(callbackType);
        }
        
        public int getCallbackCount() {
            return utility.getCallbackCount();
        }
        
        public boolean isBalanced() {
            return utility.isBalanced();
        }
        
        public void clear() {
            utility.clear();
        }
        
        public List<CallbackRecord> getCallbackHistory() {
            return utility.getCallbackHistory();
        }
        
        public Map<String, Integer> getCallbackCounts() {
            return utility.getCallbackCounts();
        }
        
        // Additional validation methods
        public boolean hasMinimumCallbacks(int minimum) {
            return utility.getCallbackCount() >= minimum;
        }
        
        public boolean hasCallbackSequence(String... expectedSequence) {
            List<CallbackRecord> history = utility.getCallbackHistory();
            if (history.size() < expectedSequence.length) return false;
            
            for (int i = 0; i < expectedSequence.length; i++) {
                if (!history.get(i).getCallbackType().equals(expectedSequence[i])) {
                    return false;
                }
            }
            return true;
        }
        
        // Overloaded method for List parameter (fixing compilation errors)
        public boolean hasCallbackSequence(List<String> expectedSequence) {
            return hasCallbackSequence(expectedSequence.toArray(new String[0]));
        }
        
        public boolean hasBalancedPairs(String startType, String endType) {
            return utility.countCallbacks(startType) == utility.countCallbacks(endType);
        }
        
        // Additional methods needed by BenchmarkCallbackBridge
        public List<String> getCallbackSequence() {
            return utility.getCallbackHistory().stream()
                    .map(CallbackRecord::getCallbackType)
                    .collect(java.util.stream.Collectors.toList());
        }
        
        public CallbackRecord findCallback(String methodName) {
            return utility.getCallbackHistory().stream()
                    .filter(record -> record.getCallbackType().equals(methodName))
                    .findFirst()
                    .orElse(null);
        }
        
        public List<CallbackRecord> getAllEvents() {
            return utility.getCallbackHistory();
        }
        
        public Map<String, Object> getCallbackMetadata(String methodName) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("count", utility.countCallbacks(methodName));
            metadata.put("present", utility.hasCallback(methodName));
            return metadata;
        }
        
        // Additional methods required by Phase 2 fixes
        
        public boolean hasInconsistentSequence() {
            // Check for unbalanced callbacks
            int depth = 0;
            for (CallbackRecord record : utility.getCallbackHistory()) {
                String callback = record.getCallbackType();
                if (callback.contains("start") || callback.contains("enter") ||
                    callback.contains("begin") || callback.contains("Start")) {
                    depth++;
                } else if (callback.contains("end") || callback.contains("exit") ||
                           callback.contains("End")) {
                    depth--;
                }
                if (depth < 0) {
                    return true; // More exits than entries
                }
            }
            return depth != 0; // Unbalanced if depth is not zero
        }
        
        public void reset() {
            utility.clear();
        }
        
        public void validateCallbackBalance() {
            if (hasInconsistentSequence()) {
                List<String> history = utility.getCallbackHistory().stream()
                    .map(CallbackRecord::getCallbackType)
                    .collect(java.util.stream.Collectors.toList());
                throw new AssertionError("Callback sequence is unbalanced: " + history);
            }
        }
        
        public void validateCallbackSequence() {
            if (hasInconsistentSequence()) {
                List<String> history = utility.getCallbackHistory().stream()
                    .map(CallbackRecord::getCallbackType)
                    .collect(java.util.stream.Collectors.toList());
                throw new AssertionError("Callback sequence is inconsistent: " + history);
            }
        }
    }
    
    // ==================== DelegatingCallbackTestingUtility Inner Class ====================
    
    /**
     * A wrapper that extends CallbackTestingUtility to both record callbacks for testing
     * and forward them to an actual delegate. This maintains the connection between
     * the parser and the test recorder.
     */
    private static class DelegatingCallbackTestingUtility extends CallbackTestingUtility
    {
        private final CallbackDelegate delegate;
        
        public DelegatingCallbackTestingUtility(CallbackDelegate delegate)
        {
            this.delegate = delegate;
        }
        
        @Override
        public void beginExpression(LocatableToken token, boolean included)
        {
            super.beginExpression(token, included); // Record for testing
            delegate.beginExpression(token, included); // Forward to actual delegate
        }
        
        @Override
        public void endExpression(LocatableToken token, boolean included)
        {
            super.endExpression(token, included);
            delegate.endExpression(token, included);
        }
        
        @Override
        public void gotLiteral(LocatableToken token)
        {
            super.gotLiteral(token);
            delegate.gotLiteral(token);
        }
        
        @Override
        public void gotIdentifier(LocatableToken token)
        {
            super.gotIdentifier(token);
            delegate.gotIdentifier(token);
        }
        
        @Override
        public void gotBinaryOperator(LocatableToken token)
        {
            super.gotBinaryOperator(token);
            delegate.gotBinaryOperator(token);
        }
        
        @Override
        public void gotMethodCall(LocatableToken token)
        {
            super.gotMethodCall(token);
            delegate.gotMethodCall(token);
        }
        
        @Override
        public void gotMemberCall(LocatableToken token, List<LocatableToken> typeArgs)
        {
            super.gotMemberCall(token, typeArgs);
            delegate.gotMemberCall(token, typeArgs);
        }
        
        @Override
        public void beginArgumentList(LocatableToken token)
        {
            super.beginArgumentList(token);
            delegate.beginArgumentList(token);
        }
        
        @Override
        public void endArgumentList(LocatableToken token)
        {
            super.endArgumentList(token);
            delegate.endArgumentList(token);
        }
        
        @Override
        public void endArgument()
        {
            super.endArgument();
            delegate.endArgument();
        }
        
        @Override
        public void beginIfStmt(LocatableToken token)
        {
            super.beginIfStmt(token);
            delegate.beginIfStmt(token);
        }
        
        @Override
        public void endIfStmt(LocatableToken token, boolean included)
        {
            super.endIfStmt(token, included);
            delegate.endIfStmt(token, included);
        }
        
        @Override
        public void beginIfCondBlock(LocatableToken token)
        {
            super.beginIfCondBlock(token);
            delegate.beginIfCondBlock(token);
        }
        
        @Override
        public void endIfCondBlock(LocatableToken token, boolean included)
        {
            super.endIfCondBlock(token, included);
            delegate.endIfCondBlock(token, included);
        }
        
        @Override
        public void gotElseIf(LocatableToken token)
        {
            super.gotElseIf(token);
            delegate.gotElseIf(token);
        }
        
        @Override
        public void beginElement(LocatableToken token)
        {
            super.beginElement(token);
            delegate.beginElement(token);
        }
        
        @Override
        public void endElement(LocatableToken token, boolean included)
        {
            super.endElement(token, included);
            delegate.endElement(token, included);
        }
        
        @Override
        public void beginForLoop(LocatableToken token)
        {
            super.beginForLoop(token);
            delegate.beginForLoop(token);
        }
        
        @Override
        public void endForLoop(LocatableToken token, boolean included)
        {
            super.endForLoop(token, included);
            delegate.endForLoop(token, included);
        }
        
        @Override
        public void beginForLoopBody(LocatableToken token)
        {
            super.beginForLoopBody(token);
            delegate.beginForLoopBody(token);
        }
        
        @Override
        public void endForLoopBody(LocatableToken token, boolean included)
        {
            super.endForLoopBody(token, included);
            delegate.endForLoopBody(token, included);
        }
        
        @Override
        public void beginMethodBody(LocatableToken token)
        {
            super.beginMethodBody(token);
            delegate.beginMethodBody(token);
        }
        
        @Override
        public void endMethodBody(LocatableToken token, boolean included)
        {
            super.endMethodBody(token, included);
            delegate.endMethodBody(token, included);
        }
        
        @Override
        public void gotImport(List<LocatableToken> tokens, boolean isStatic, LocatableToken importToken, LocatableToken semiToken)
        {
            super.gotImport(tokens, isStatic, importToken, semiToken);
            delegate.gotImport(tokens, isStatic, importToken, semiToken);
        }
        
        @Override
        public void gotWildcardImport(List<LocatableToken> tokens, boolean isStatic, LocatableToken importToken, LocatableToken semiToken)
        {
            super.gotWildcardImport(tokens, isStatic, importToken, semiToken);
            delegate.gotWildcardImport(tokens, isStatic, importToken, semiToken);
        }
        
        @Override
        public void gotImportStmtSemi(LocatableToken token)
        {
            super.gotImportStmtSemi(token);
            delegate.gotImportStmtSemi(token);
        }
        
        @Override
        public void gotForTest(boolean isPresent)
        {
            super.gotForTest(isPresent);
            delegate.gotForTest(isPresent);
        }
        
        @Override
        public void gotForIncrement(boolean isPresent)
        {
            super.gotForIncrement(isPresent);
            delegate.gotForIncrement(isPresent);
        }
        
        @Override
        public void gotStatementExpression()
        {
            super.gotStatementExpression();
            delegate.gotStatementExpression();
        }
        
        @Override
        public void finishedCU(int state)
        {
            super.finishedCU(state);
            delegate.finishedCU(state);
        }
        
        @Override
        public void gotDeclBegin(LocatableToken token)
        {
            super.gotDeclBegin(token);
            delegate.gotDeclBegin(token);
        }
        
        @Override
        public void gotTypeDef(LocatableToken token, int tdType)
        {
            super.gotTypeDef(token, tdType);
            delegate.gotTypeDef(token, tdType);
        }
        
        @Override
        public void gotTypeDefName(LocatableToken token)
        {
            super.gotTypeDefName(token);
            delegate.gotTypeDefName(token);
        }
        
        @Override
        public void beginTypeDefExtends(LocatableToken token)
        {
            super.beginTypeDefExtends(token);
            delegate.beginTypeDefExtends(token);
        }
        
        @Override
        public void endTypeDefExtends()
        {
            super.endTypeDefExtends();
            delegate.endTypeDefExtends();
        }
        
        @Override
        public void beginTypeBody(LocatableToken token)
        {
            super.beginTypeBody(token);
            delegate.beginTypeBody(token);
        }
        
        @Override
        public void endTypeBody(LocatableToken token, boolean included)
        {
            super.endTypeBody(token, included);
            delegate.endTypeBody(token, included);
        }
        
        @Override
        public void gotTypeDefEnd(LocatableToken token, boolean included)
        {
            super.gotTypeDefEnd(token, included);
            delegate.gotTypeDefEnd(token, included);
        }
        
        @Override
        public void gotAllMethodParameters()
        {
            super.gotAllMethodParameters();
            delegate.gotAllMethodParameters();
        }
        
        @Override
        public void endMethodDecl(LocatableToken token, boolean included)
        {
            super.endMethodDecl(token, included);
            delegate.endMethodDecl(token, included);
        }
        
        @Override
        public void beginPackageStatement(LocatableToken token)
        {
            super.beginPackageStatement(token);
            delegate.beginPackageStatement(token);
        }
        
        @Override
        public void gotPackage(List<LocatableToken> tokens)
        {
            super.gotPackage(tokens);
            delegate.gotPackage(tokens);
        }
        
        @Override
        public void gotPackageSemi(LocatableToken token)
        {
            super.gotPackageSemi(token);
            delegate.gotPackageSemi(token);
        }
        
        @Override
        public void gotMethodParameter(LocatableToken nameToken, LocatableToken hiddenToken)
        {
            super.gotMethodParameter(nameToken, hiddenToken);
            delegate.gotMethodParameter(nameToken, hiddenToken);
        }
        
        @Override
        public void gotTypeSpec(List<LocatableToken> tokens)
        {
            super.gotTypeSpec(tokens);
            delegate.gotTypeSpec(tokens);
        }
        
        @Override
        public void gotModifier(LocatableToken token)
        {
            super.gotModifier(token);
            delegate.gotModifier(token);
        }
        
        @Override
        public void gotMethodDeclaration(LocatableToken nameToken, LocatableToken hiddenToken)
        {
            super.gotMethodDeclaration(nameToken, hiddenToken);
            delegate.gotMethodDeclaration(nameToken, hiddenToken);
        }
        
        @Override
        public void beginSwitchStmt(LocatableToken token, boolean isExpression)
        {
            super.beginSwitchStmt(token, isExpression);
            delegate.beginSwitchStmt(token, isExpression);
        }
        
        @Override
        public void endSwitchStmt(LocatableToken token, boolean included)
        {
            super.endSwitchStmt(token, included);
            delegate.endSwitchStmt(token, included);
        }
        
        @Override
        public void beginSwitchCase(LocatableToken token)
        {
            super.beginSwitchCase(token);
            delegate.beginSwitchCase(token);
        }
        
        @Override
        public void endSwitchCase(LocatableToken token, boolean wasArrow)
        {
            super.endSwitchCase(token, wasArrow);
            delegate.endSwitchCase(token, wasArrow);
        }
        
        @Override
        public void gotSwitchCaseType(LocatableToken token, boolean isArrow)
        {
            super.gotSwitchCaseType(token, isArrow);
            delegate.gotSwitchCaseType(token, isArrow);
        }
        
        @Override
        public void gotSwitchDefault()
        {
            super.gotSwitchDefault();
            delegate.gotSwitchDefault();
        }
        
        @Override
        public void beginSwitchBlock(LocatableToken token)
        {
            super.beginSwitchBlock(token);
            delegate.beginSwitchBlock(token);
        }
        
        @Override
        public void endSwitchBlock(LocatableToken token)
        {
            super.endSwitchBlock(token);
            delegate.endSwitchBlock(token);
        }
        
        @Override
        public void gotLambdaFormalName(LocatableToken token)
        {
            super.gotLambdaFormalName(token);
            delegate.gotLambdaFormalName(token);
        }
        
        @Override
        public void beginLambdaBody(boolean isBlock, LocatableToken token)
        {
            super.beginLambdaBody(isBlock, token);
            delegate.beginLambdaBody(isBlock, token);
        }
        
        @Override
        public void endLambdaBody(LocatableToken token)
        {
            super.endLambdaBody(token);
            delegate.endLambdaBody(token);
        }
        
        @Override
        public void gotMemberAccess(LocatableToken token)
        {
            super.gotMemberAccess(token);
            delegate.gotMemberAccess(token);
        }
        
        @Override
        public void gotAnnotation(List<LocatableToken> name, boolean hasParams)
        {
            super.gotAnnotation(name, hasParams);
            delegate.gotAnnotation(name, hasParams);
        }
        
        @Override
        public void determinedForLoop(boolean forEach, boolean hasInit)
        {
            super.determinedForLoop(forEach, hasInit);
            delegate.determinedForLoop(forEach, hasInit);
        }
    }
}