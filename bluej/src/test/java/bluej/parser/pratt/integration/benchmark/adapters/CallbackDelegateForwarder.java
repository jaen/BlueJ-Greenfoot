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
package bluej.parser.pratt.integration.benchmark.adapters;

import bluej.parser.CallbackDelegate;
import bluej.parser.lexer.LocatableToken;

import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Universal callback forwarder that maps adapter-specific callbacks to 
 * standard CallbackDelegate methods.
 * 
 * This utility class provides the core mapping logic to translate custom
 * callback invocations from various adapters into the appropriate 
 * CallbackDelegate method calls with properly formed synthetic tokens.
 * 
 * Key features:
 * - Complete callback type mapping table
 * - Synthetic token creation for all callback types
 * - Null-safe delegate handling
 * - Performance-optimized callback dispatch
 * - Support for all adapter callback patterns
 */
public class CallbackDelegateForwarder
{
    // Cache for callback type mapping to improve performance
    private static final Map<String, CallbackMapper> CALLBACK_MAPPERS = new HashMap<>();
    
    static
    {
        // Initialize callback mappers for all known callback types
        initializeCallbackMappers();
    }
    
    /**
     * Forward a callback from an adapter to the external delegate.
     * 
     * @param delegate The external callback delegate to forward to
     * @param callbackType The type of callback to forward
     * @param lineNumber The line number associated with the callback
     * @param params Additional parameters for the callback
     */
    public static void forward(CallbackDelegate delegate, String callbackType, 
                               int lineNumber, Object... params)
    {
        if (delegate == null)
        {
            return; // No delegate to forward to
        }
        
        // Get or create synthetic token for this callback
        LocatableToken syntheticToken = createSyntheticToken(callbackType, lineNumber);
        
        // Use cached mapper if available
        CallbackMapper mapper = CALLBACK_MAPPERS.get(callbackType);
        if (mapper != null)
        {
            mapper.map(delegate, syntheticToken, lineNumber, params);
            return;
        }
        
        // Fallback to switch statement for unmapped callbacks
        switch (callbackType)
        {
            case "classStart":
                delegate.beginTypeBody(syntheticToken);
                break;
                
            case "classEnd":
                delegate.endTypeBody(syntheticToken, true);
                break;
                
            case "methodStart":
                delegate.beginMethodBody(syntheticToken);
                break;
                
            case "methodEnd":
                delegate.endMethodBody(syntheticToken, true);
                break;
                
            case "exprStart":
            case "blockStart":
            case "statementStart":
                delegate.beginExpression(syntheticToken, true);
                break;
                
            case "exprEnd":
            case "blockEnd":
            case "statementEnd":
                delegate.endExpression(syntheticToken, true);
                break;
                
            case "importStart":
                List<LocatableToken> importTokens = createTokenList("import", lineNumber);
                delegate.gotImport(importTokens, false, syntheticToken, syntheticToken);
                break;
                
            case "importEnd":
                delegate.gotImportStmtSemi(syntheticToken);
                break;
                
            case "fieldDeclaration":
                List<LocatableToken> fieldTokens = createTokenList("field", lineNumber);
                delegate.gotTypeSpec(fieldTokens);
                break;
                
            case "fileStart":
                delegate.beginPackageStatement(syntheticToken);
                break;
                
            case "packageDeclaration":
                List<LocatableToken> packageTokens = createTokenList("package", lineNumber);
                delegate.gotPackage(packageTokens);
                break;
                
            case "parameter":
                delegate.gotMethodParameter(syntheticToken, null);
                break;
                
            case "lambdaStart":
                delegate.beginLambdaBody(false, syntheticToken);
                break;
                
            case "lambdaEnd":
                delegate.endLambdaBody(syntheticToken);
                break;
                
            case "switchStart":
                delegate.beginSwitchStmt(syntheticToken, false);
                break;
                
            case "switchEnd":
                delegate.endSwitchStmt(syntheticToken, true);
                break;
                
            case "caseStart":
                delegate.beginSwitchCase(syntheticToken);
                break;
                
            case "caseEnd":
                delegate.endSwitchCase(syntheticToken, false);
                break;
                
            case "forLoopStart":
                delegate.beginForLoop(syntheticToken);
                break;
                
            case "forLoopEnd":
                delegate.endForLoop(syntheticToken, true);
                break;
                
            case "ifStart":
                delegate.beginIfStmt(syntheticToken);
                break;
                
            case "ifEnd":
                delegate.endIfStmt(syntheticToken, true);
                break;
                
            case "elementStart":
                delegate.beginElement(syntheticToken);
                break;
                
            case "elementEnd":
                delegate.endElement(syntheticToken, true);
                break;
                
            case "argumentListStart":
                delegate.beginArgumentList(syntheticToken);
                break;
                
            case "argumentListEnd":
                delegate.endArgumentList(syntheticToken);
                break;
                
            case "typeDefStart":
                delegate.gotTypeDef(syntheticToken, 1); // Default type CLASS
                break;
                
            case "typeDefEnd":
                delegate.gotTypeDefEnd(syntheticToken, true);
                break;
                
            case "methodDeclaration":
                delegate.gotMethodDeclaration(syntheticToken, null);
                break;
                
            case "annotation":
                List<LocatableToken> annotationTokens = createTokenList("@Annotation", lineNumber);
                delegate.gotAnnotation(annotationTokens, false);
                break;
                
            default:
                // For unmapped callbacks, try to use generic expression callbacks
                if (callbackType.contains("Start") || callbackType.contains("Begin"))
                {
                    delegate.beginExpression(syntheticToken, true);
                }
                else if (callbackType.contains("End"))
                {
                    delegate.endExpression(syntheticToken, true);
                }
                // If it doesn't match any pattern, ignore it silently
                break;
        }
    }
    
    /**
     * Create a synthetic token for a given callback type and line number.
     * 
     * @param type The callback type
     * @param lineNumber The line number
     * @return A synthetic locatable token
     */
    private static LocatableToken createSyntheticToken(String type, int lineNumber)
    {
        return SyntheticLocatableToken.create(type, lineNumber);
    }
    
    /**
     * Create a list of synthetic tokens.
     * 
     * @param prefix The prefix for the token text
     * @param lineNumber The line number
     * @return A list containing a single synthetic token
     */
    private static List<LocatableToken> createTokenList(String prefix, int lineNumber)
    {
        return Arrays.asList(createSyntheticToken(prefix, lineNumber));
    }
    
    /**
     * Initialize the callback mappers for performance optimization.
     */
    private static void initializeCallbackMappers()
    {
        // Class-related callbacks
        CALLBACK_MAPPERS.put("classStart", 
            (delegate, token, line, params) -> delegate.beginTypeBody(token));
        CALLBACK_MAPPERS.put("classEnd", 
            (delegate, token, line, params) -> delegate.endTypeBody(token, true));
        
        // Method-related callbacks
        CALLBACK_MAPPERS.put("methodStart", 
            (delegate, token, line, params) -> delegate.beginMethodBody(token));
        CALLBACK_MAPPERS.put("methodEnd", 
            (delegate, token, line, params) -> delegate.endMethodBody(token, true));
        
        // Expression-related callbacks
        CALLBACK_MAPPERS.put("exprStart", 
            (delegate, token, line, params) -> delegate.beginExpression(token, true));
        CALLBACK_MAPPERS.put("exprEnd", 
            (delegate, token, line, params) -> delegate.endExpression(token, true));
        CALLBACK_MAPPERS.put("blockStart", 
            (delegate, token, line, params) -> delegate.beginExpression(token, true));
        CALLBACK_MAPPERS.put("blockEnd", 
            (delegate, token, line, params) -> delegate.endExpression(token, true));
        CALLBACK_MAPPERS.put("statementStart", 
            (delegate, token, line, params) -> delegate.beginExpression(token, true));
        CALLBACK_MAPPERS.put("statementEnd", 
            (delegate, token, line, params) -> delegate.endExpression(token, true));
        
        // Import-related callbacks
        CALLBACK_MAPPERS.put("importStart", 
            (delegate, token, line, params) -> {
                List<LocatableToken> tokens = createTokenList("import", line);
                delegate.gotImport(tokens, false, token, token);
            });
        CALLBACK_MAPPERS.put("importEnd", 
            (delegate, token, line, params) -> delegate.gotImportStmtSemi(token));
        
        // Field-related callbacks
        CALLBACK_MAPPERS.put("fieldDeclaration", 
            (delegate, token, line, params) -> {
                List<LocatableToken> tokens = createTokenList("field", line);
                delegate.gotTypeSpec(tokens);
            });
        
        // Package-related callbacks
        CALLBACK_MAPPERS.put("fileStart", 
            (delegate, token, line, params) -> delegate.beginPackageStatement(token));
        CALLBACK_MAPPERS.put("packageDeclaration", 
            (delegate, token, line, params) -> {
                List<LocatableToken> tokens = createTokenList("package", line);
                delegate.gotPackage(tokens);
            });
        
        // Parameter callbacks
        CALLBACK_MAPPERS.put("parameter", 
            (delegate, token, line, params) -> delegate.gotMethodParameter(token, null));
        
        // Lambda callbacks
        CALLBACK_MAPPERS.put("lambdaStart", 
            (delegate, token, line, params) -> delegate.beginLambdaBody(false, token));
        CALLBACK_MAPPERS.put("lambdaEnd", 
            (delegate, token, line, params) -> delegate.endLambdaBody(token));
        
        // Switch callbacks
        CALLBACK_MAPPERS.put("switchStart", 
            (delegate, token, line, params) -> delegate.beginSwitchStmt(token, false));
        CALLBACK_MAPPERS.put("switchEnd", 
            (delegate, token, line, params) -> delegate.endSwitchStmt(token, true));
        CALLBACK_MAPPERS.put("caseStart", 
            (delegate, token, line, params) -> delegate.beginSwitchCase(token));
        CALLBACK_MAPPERS.put("caseEnd", 
            (delegate, token, line, params) -> delegate.endSwitchCase(token, false));
        
        // Loop callbacks
        CALLBACK_MAPPERS.put("forLoopStart", 
            (delegate, token, line, params) -> delegate.beginForLoop(token));
        CALLBACK_MAPPERS.put("forLoopEnd", 
            (delegate, token, line, params) -> delegate.endForLoop(token, true));
        
        // Conditional callbacks
        CALLBACK_MAPPERS.put("ifStart", 
            (delegate, token, line, params) -> delegate.beginIfStmt(token));
        CALLBACK_MAPPERS.put("ifEnd", 
            (delegate, token, line, params) -> delegate.endIfStmt(token, true));
        
        // Element callbacks
        CALLBACK_MAPPERS.put("elementStart", 
            (delegate, token, line, params) -> delegate.beginElement(token));
        CALLBACK_MAPPERS.put("elementEnd", 
            (delegate, token, line, params) -> delegate.endElement(token, true));
        
        // Argument list callbacks
        CALLBACK_MAPPERS.put("argumentListStart", 
            (delegate, token, line, params) -> delegate.beginArgumentList(token));
        CALLBACK_MAPPERS.put("argumentListEnd", 
            (delegate, token, line, params) -> delegate.endArgumentList(token));
        
        // Type definition callbacks
        CALLBACK_MAPPERS.put("typeDefStart", 
            (delegate, token, line, params) -> delegate.gotTypeDef(token, 1));
        CALLBACK_MAPPERS.put("typeDefEnd", 
            (delegate, token, line, params) -> delegate.gotTypeDefEnd(token, true));
        
        // Method declaration callbacks
        CALLBACK_MAPPERS.put("methodDeclaration", 
            (delegate, token, line, params) -> delegate.gotMethodDeclaration(token, null));
        
        // Annotation callbacks
        CALLBACK_MAPPERS.put("annotation", 
            (delegate, token, line, params) -> {
                List<LocatableToken> tokens = createTokenList("@Annotation", line);
                delegate.gotAnnotation(tokens, false);
            });
    }
    
    /**
     * Functional interface for callback mapping.
     */
    @FunctionalInterface
    private interface CallbackMapper
    {
        void map(CallbackDelegate delegate, LocatableToken token, int lineNumber, Object... params);
    }
}