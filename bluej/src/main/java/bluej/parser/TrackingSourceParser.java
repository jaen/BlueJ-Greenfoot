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
import bluej.parser.lexer.JavaTokenFilter;
import java.io.Reader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Extension of SourceParser that provides non-intrusive callback tracking
 * and enables parser/lexer switching for testing alternative implementations.
 * 
 * Features:
 * - Zero overhead when tracking is disabled
 * - Exception-safe callback forwarding
 * - Thread-safe implementation
 * - Optional tracking support
 * - Parser/lexer customization capability
 */
public class TrackingSourceParser extends SourceParser {
    
    // Core tracking state
    private final Object tracker;  // Use Object type to avoid dependency issues
    private final boolean trackingEnabled;
    
    // Performance optimization
    private static final boolean DEBUG_MODE = 
        Boolean.parseBoolean(System.getProperty("bluej.debug.tracking", "false"));
    
    // ==================== Constructors ====================
    
    /**
     * Standard constructor with tracking enabled by default.
     * @param r The input reader
     * @param sourceType The type of source being parsed
     * @param tracker The callback tracker instance
     */
    public TrackingSourceParser(Reader r, SourceType sourceType, Object tracker) {
        this(r, sourceType, tracker, true);
    }
    
    /**
     * Full constructor with tracking control.
     * @param r The input reader
     * @param sourceType The type of source being parsed
     * @param tracker The callback tracker instance
     * @param enabled Whether tracking should be enabled
     */
    public TrackingSourceParser(Reader r, SourceType sourceType, Object tracker, boolean enabled) {
        super(r, sourceType);
        this.tracker = tracker;
        this.trackingEnabled = enabled && (tracker != null);
        
        if (DEBUG_MODE) {
            System.out.println("TrackingSourceParser created: tracker=" + tracker + 
                             ", enabled=" + trackingEnabled);
        }
    }
    
    /**
     * Constructor with position information and tracking.
     * @param r The input reader
     * @param sourceType The type of source being parsed
     * @param line Starting line number
     * @param col Starting column number
     * @param pos Starting position
     * @param tracker The callback tracker instance
     */
    public TrackingSourceParser(Reader r, SourceType sourceType, int line, int col, int pos, Object tracker) {
        super(r, sourceType, line, col, pos);
        this.tracker = tracker;
        this.trackingEnabled = tracker != null;
    }
    
    /**
     * Legacy compatibility constructor - no tracking.
     * @param r The input reader
     * @param sourceType The type of source being parsed
     */
    public TrackingSourceParser(Reader r, SourceType sourceType) {
        this(r, sourceType, null, false);
    }
    
    // ==================== Parser/Lexer Switching Support ====================
    
    /**
     * Override to enable alternative lexer implementations.
     * Subclasses can override this for testing different lexer strategies.
     */
    @Override
    protected JavaTokenFilter createLexer(Reader reader, SourceType sourceType, boolean handleComments, 
                                         boolean handleMultilineStrings, int line, int col, int pos) {
        // Default implementation delegates to parent
        return super.createLexer(reader, sourceType, handleComments, handleMultilineStrings, line, col, pos);
    }
    
    /**
     * Override to enable alternative parser implementations.
     * Subclasses can override this for testing different parser strategies.
     */
    @Override
    protected ParserBehavior createParser(JavaTokenFilter tokenStream, SourceType sourceType) {
        // Default implementation delegates to parent
        return super.createParser(tokenStream, sourceType);
    }
    
    // ==================== Critical Callback Method Overrides ====================
    
    @Override
    protected void beginExpression(LocatableToken token, boolean isLambdaBody) {
        super.beginExpression(token, isLambdaBody);
        if (trackingEnabled) {
            forwardToTracker("beginExpression", token, isLambdaBody);
        }
    }
    
    @Override
    protected void endExpression(LocatableToken token, boolean emptyExpression) {
        super.endExpression(token, emptyExpression);
        if (trackingEnabled) {
            forwardToTracker("endExpression", token, emptyExpression);
        }
    }
    
    @Override
    protected void gotIdentifier(LocatableToken token) {
        super.gotIdentifier(token);
        if (trackingEnabled) {
            forwardToTracker("gotIdentifier", token);
        }
    }
    
    @Override
    protected void gotBinaryOperator(LocatableToken token) {
        super.gotBinaryOperator(token);
        if (trackingEnabled) {
            forwardToTracker("gotBinaryOperator", token);
        }
    }
    
    @Override
    protected void beginElement(LocatableToken token) {
        super.beginElement(token);
        if (trackingEnabled) {
            forwardToTracker("beginElement", token);
        }
    }
    
    @Override
    protected void endElement(LocatableToken token, boolean included) {
        super.endElement(token, included);
        if (trackingEnabled) {
            forwardToTracker("endElement", token, included);
        }
    }
    
    @Override
    protected void gotLiteral(LocatableToken token) {
        super.gotLiteral(token);
        if (trackingEnabled) {
            forwardToTracker("gotLiteral", token);
        }
    }
    
    @Override
    protected void gotMethodCall(LocatableToken token) {
        super.gotMethodCall(token);
        if (trackingEnabled) {
            forwardToTracker("gotMethodCall", token);
        }
    }
    
    @Override
    protected void gotMemberCall(LocatableToken token, List<LocatableToken> typeArgs) {
        super.gotMemberCall(token, typeArgs);
        if (trackingEnabled) {
            forwardToTracker("gotMemberCall", token, typeArgs);
        }
    }
    
    @Override
    protected void beginArgumentList(LocatableToken token) {
        super.beginArgumentList(token);
        if (trackingEnabled) {
            forwardToTracker("beginArgumentList", token);
        }
    }
    
    @Override
    protected void endArgumentList(LocatableToken token) {
        super.endArgumentList(token);
        if (trackingEnabled) {
            forwardToTracker("endArgumentList", token);
        }
    }
    
    @Override
    protected void endArgument() {
        super.endArgument();
        if (trackingEnabled) {
            forwardToTracker("endArgument");
        }
    }
    
    @Override
    protected void beginIfStmt(LocatableToken token) {
        super.beginIfStmt(token);
        if (trackingEnabled) {
            forwardToTracker("beginIfStmt", token);
        }
    }
    
    @Override
    protected void endIfStmt(LocatableToken token, boolean included) {
        super.endIfStmt(token, included);
        if (trackingEnabled) {
            forwardToTracker("endIfStmt", token, included);
        }
    }
    
    @Override
    protected void beginIfCondBlock(LocatableToken token) {
        super.beginIfCondBlock(token);
        if (trackingEnabled) {
            forwardToTracker("beginIfCondBlock", token);
        }
    }
    
    @Override
    protected void endIfCondBlock(LocatableToken token, boolean included) {
        super.endIfCondBlock(token, included);
        if (trackingEnabled) {
            forwardToTracker("endIfCondBlock", token, included);
        }
    }
    
    @Override
    protected void gotElseIf(LocatableToken token) {
        super.gotElseIf(token);
        if (trackingEnabled) {
            forwardToTracker("gotElseIf", token);
        }
    }
    
    @Override
    protected void beginForLoop(LocatableToken token) {
        super.beginForLoop(token);
        if (trackingEnabled) {
            forwardToTracker("beginForLoop", token);
        }
    }
    
    @Override
    protected void endForLoop(LocatableToken token, boolean included) {
        super.endForLoop(token, included);
        if (trackingEnabled) {
            forwardToTracker("endForLoop", token, included);
        }
    }
    
    @Override
    protected void beginForLoopBody(LocatableToken token) {
        super.beginForLoopBody(token);
        if (trackingEnabled) {
            forwardToTracker("beginForLoopBody", token);
        }
    }
    
    @Override
    protected void endForLoopBody(LocatableToken token, boolean included) {
        super.endForLoopBody(token, included);
        if (trackingEnabled) {
            forwardToTracker("endForLoopBody", token, included);
        }
    }
    
    @Override
    protected void beginMethodBody(LocatableToken token) {
        super.beginMethodBody(token);
        if (trackingEnabled) {
            forwardToTracker("beginMethodBody", token);
        }
    }
    
    @Override
    protected void endMethodBody(LocatableToken token, boolean included) {
        super.endMethodBody(token, included);
        if (trackingEnabled) {
            forwardToTracker("endMethodBody", token, included);
        }
    }
    
    @Override
    protected void gotImport(List<LocatableToken> tokens, boolean isStatic,
                           LocatableToken importToken, LocatableToken semiToken) {
        super.gotImport(tokens, isStatic, importToken, semiToken);
        if (trackingEnabled) {
            forwardToTracker("gotImport", tokens, isStatic, importToken, semiToken);
        }
    }
    
    @Override
    protected void gotWildcardImport(List<LocatableToken> tokens, boolean isStatic,
                                   LocatableToken importToken, LocatableToken semiToken) {
        super.gotWildcardImport(tokens, isStatic, importToken, semiToken);
        if (trackingEnabled) {
            forwardToTracker("gotWildcardImport", tokens, isStatic, importToken, semiToken);
        }
    }
    
    @Override
    protected void gotImportStmtSemi(LocatableToken token) {
        super.gotImportStmtSemi(token);
        if (trackingEnabled) {
            forwardToTracker("gotImportStmtSemi", token);
        }
    }
    
    // ==================== Additional Critical Callback Method Overrides ====================
    
    @Override
    protected void gotMemberAccess(LocatableToken token) {
        super.gotMemberAccess(token);
        if (trackingEnabled) {
            forwardToTracker("gotMemberAccess", token);
        }
    }
    
    @Override
    protected void gotAnnotation(List<LocatableToken> annName, boolean paramsFollow) {
        super.gotAnnotation(annName, paramsFollow);
        if (trackingEnabled) {
            forwardToTracker("gotAnnotation", annName, paramsFollow);
        }
    }
    
    @Override
    protected void determinedForLoop(boolean forEachLoop, boolean initExpressionFollows) {
        super.determinedForLoop(forEachLoop, initExpressionFollows);
        if (trackingEnabled) {
            forwardToTracker("determinedForLoop", forEachLoop, initExpressionFollows);
        }
    }
    
    // Lambda-related methods
    @Override
    protected void beginLambdaBody(boolean lambdaIsBlock, LocatableToken openCurly) {
        super.beginLambdaBody(lambdaIsBlock, openCurly);
        if (trackingEnabled) {
            forwardToTracker("beginLambdaBody", lambdaIsBlock, openCurly);
        }
    }
    
    @Override
    protected void endLambdaBody(LocatableToken closeCurly) {
        super.endLambdaBody(closeCurly);
        if (trackingEnabled) {
            forwardToTracker("endLambdaBody", closeCurly);
        }
    }
    
    @Override
    protected void gotLambdaFormalParam() {
        super.gotLambdaFormalParam();
        if (trackingEnabled) {
            forwardToTracker("gotLambdaFormalParam");
        }
    }
    
    @Override
    protected void gotLambdaFormalName(LocatableToken name) {
        super.gotLambdaFormalName(name);
        if (trackingEnabled) {
            forwardToTracker("gotLambdaFormalName", name);
        }
    }
    
    @Override
    protected void gotLambdaFormalType(List<LocatableToken> type) {
        super.gotLambdaFormalType(type);
        if (trackingEnabled) {
            forwardToTracker("gotLambdaFormalType", type);
        }
    }
    
    // Switch-related methods
    @Override
    protected void beginSwitchStmt(LocatableToken token, boolean isSwitchExpression) {
        super.beginSwitchStmt(token, isSwitchExpression);
        if (trackingEnabled) {
            forwardToTracker("beginSwitchStmt", token, isSwitchExpression);
        }
    }
    
    @Override
    protected void beginSwitchBlock(LocatableToken token) {
        super.beginSwitchBlock(token);
        if (trackingEnabled) {
            forwardToTracker("beginSwitchBlock", token);
        }
    }
    
    @Override
    protected void endSwitchBlock(LocatableToken token) {
        super.endSwitchBlock(token);
        if (trackingEnabled) {
            forwardToTracker("endSwitchBlock", token);
        }
    }
    
    @Override
    protected void endSwitchStmt(LocatableToken token, boolean included) {
        super.endSwitchStmt(token, included);
        if (trackingEnabled) {
            forwardToTracker("endSwitchStmt", token, included);
        }
    }
    
    @Override
    protected void beginSwitchCase(LocatableToken token) {
        super.beginSwitchCase(token);
        if (trackingEnabled) {
            forwardToTracker("beginSwitchCase", token);
        }
    }
    
    @Override
    protected void gotSwitchCaseType(LocatableToken token, boolean isArrowSyntax) {
        super.gotSwitchCaseType(token, isArrowSyntax);
        if (trackingEnabled) {
            forwardToTracker("gotSwitchCaseType", token, isArrowSyntax);
        }
    }
    
    @Override
    protected void endSwitchCase(LocatableToken token, boolean wasArrowSyntax) {
        super.endSwitchCase(token, wasArrowSyntax);
        if (trackingEnabled) {
            forwardToTracker("endSwitchCase", token, wasArrowSyntax);
        }
    }
    
    @Override
    protected void gotSwitchDefault() {
        super.gotSwitchDefault();
        if (trackingEnabled) {
            forwardToTracker("gotSwitchDefault");
        }
    }
    
    // ==================== Exception-Safe Forwarding Implementation ====================
    
    /**
     * Forward callback to tracker using reflection for loose coupling.
     * This approach avoids hard dependency on CallbackTracker class.
     */
    private void forwardToTracker(String methodName, Object... args) {
        if (tracker == null) return;
        
        try {
            // Find and invoke the method using robust method resolution
            Method method = findTrackerMethod(methodName, args);
            if (method != null) {
                method.setAccessible(true);
                method.invoke(tracker, args);
                
                if (DEBUG_MODE) {
                    System.out.println("TrackingSourceParser forwarded: " + methodName +
                                     " to " + tracker.getClass().getSimpleName());
                }
            } else if (DEBUG_MODE) {
                System.out.println("TrackingSourceParser: method not found: " + methodName +
                                 " with " + args.length + " parameters");
            }
            
        } catch (Exception e) {
            logTrackingError(methodName, e);
        }
    }
    
    /**
     * Robust method finder that handles method overloading and null parameters.
     */
    private Method findTrackerMethod(String methodName, Object... args) {
        Class<?> clazz = tracker.getClass();
        
        // Strategy 1: Try exact parameter type matching
        try {
            Class<?>[] exactTypes = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                exactTypes[i] = getParameterType(args[i]);
            }
            return findMethodInHierarchy(clazz, methodName, exactTypes);
        } catch (NoSuchMethodException ignored) {
            // Continue to next strategy
        }
        
        // Strategy 2: Find all methods with matching name and parameter count
        Method[] methods = getAllMethodsInHierarchy(clazz);
        for (Method method : methods) {
            if (method.getName().equals(methodName) &&
                method.getParameterCount() == args.length) {
                
                // Check if parameters are compatible
                if (areParametersCompatible(method.getParameterTypes(), args)) {
                    return method;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Find method in class hierarchy (including protected methods).
     */
    private Method findMethodInHierarchy(Class<?> clazz, String methodName, Class<?>[] paramTypes)
            throws NoSuchMethodException {
        
        // Try public methods first
        try {
            return clazz.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException e) {
            // Try declared methods (including protected)
            Class<?> current = clazz;
            while (current != null) {
                try {
                    return current.getDeclaredMethod(methodName, paramTypes);
                } catch (NoSuchMethodException e2) {
                    current = current.getSuperclass();
                }
            }
            throw new NoSuchMethodException(methodName);
        }
    }
    
    /**
     * Get all methods from class hierarchy.
     */
    private Method[] getAllMethodsInHierarchy(Class<?> clazz) {
        List<Method> methods = new ArrayList<>();
        Class<?> current = clazz;
        
        while (current != null) {
            methods.addAll(Arrays.asList(current.getDeclaredMethods()));
            methods.addAll(Arrays.asList(current.getMethods()));
            current = current.getSuperclass();
        }
        
        return methods.toArray(new Method[0]);
    }
    
    /**
     * Check if actual arguments are compatible with method parameter types.
     */
    private boolean areParametersCompatible(Class<?>[] paramTypes, Object[] args) {
        if (paramTypes.length != args.length) return false;
        
        for (int i = 0; i < paramTypes.length; i++) {
            if (args[i] == null) {
                // Null is compatible with any reference type
                if (paramTypes[i].isPrimitive()) return false;
            } else if (!isAssignableFrom(paramTypes[i], args[i].getClass())) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Enhanced type compatibility check.
     */
    private boolean isAssignableFrom(Class<?> paramType, Class<?> argType) {
        if (paramType.isAssignableFrom(argType)) return true;
        
        // Handle primitive/wrapper compatibility
        if (paramType == boolean.class && argType == Boolean.class) return true;
        if (paramType == int.class && argType == Integer.class) return true;
        if (paramType == Boolean.class && argType == boolean.class) return true;
        if (paramType == Integer.class && argType == int.class) return true;
        
        return false;
    }
    
    /**
     * Get best-guess parameter type (used for exact matching strategy).
     */
    private Class<?> getParameterType(Object arg) {
        if (arg == null) {
            // For null, we'll rely on compatible parameter matching
            // rather than guessing the type
            return Object.class;
        }
        if (arg instanceof Boolean) return boolean.class;
        if (arg instanceof Integer) return int.class;
        if (arg instanceof List) return List.class;
        return arg.getClass();
    }
    
    private void logTrackingError(String methodName, Exception e) {
        if (DEBUG_MODE) {
            System.err.println("TrackingSourceParser error in " + methodName + ": " + e.getMessage());
            e.printStackTrace(System.err);
        }
        // In production, we silently continue - tracking errors should never break parsing
    }
    
    // ==================== Query and Utility Methods ====================
    
    /**
     * Check if tracking is currently enabled.
     * @return true if tracking is enabled and tracker is available
     */
    public boolean isTrackingEnabled() {
        return trackingEnabled;
    }
    
    /**
     * Get the current tracker instance.
     * @return the tracker object, or null if not set
     */
    public Object getTracker() {
        return tracker;
    }
    
    /**
     * Check if a tracker is available.
     * @return true if a tracker instance exists (regardless of enabled state)
     */
    public boolean hasTracker() {
        return tracker != null;
    }
}