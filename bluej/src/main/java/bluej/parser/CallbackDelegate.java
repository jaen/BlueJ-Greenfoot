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

import bluej.parser.lexer.LocatableToken;
import java.util.List;

/**
 * Interface that provides access to protected callback methods from JavaParserCallbacks.
 * 
 * This allows tests and other external code to invoke parser callbacks without having to 
 * extend JavaParserCallbacks directly. SourceParser provides an implementation via 
 * anonymous class that can directly access the protected methods.
 * 
 * <p><strong>Note:</strong> This interface is intended for testing and parser extension purposes only.
 * The methods should not be called directly in production code - use the standard parser APIs instead.</p>
 */
public interface CallbackDelegate {
    
    // Expression callbacks - used by test parsing logic
    void beginExpression(LocatableToken token, boolean included);
    void endExpression(LocatableToken token, boolean included);
    void gotLiteral(LocatableToken token);
    void gotIdentifier(LocatableToken token);
    void gotBinaryOperator(LocatableToken token);
    
    // Method call callbacks - used by test invocation tracking
    void gotMethodCall(LocatableToken token);
    void gotMemberCall(LocatableToken token, List<LocatableToken> typeArgs);
    void beginArgumentList(LocatableToken token);
    void endArgumentList(LocatableToken token);
    void endArgument();
    
    // Control flow callbacks - used by test statement handling
    void beginIfStmt(LocatableToken token);
    void endIfStmt(LocatableToken token, boolean included);
    void beginIfCondBlock(LocatableToken token);
    void endIfCondBlock(LocatableToken token, boolean included);
    void gotElseIf(LocatableToken token);
    
    // Element callbacks - used by test element scoping
    void beginElement(LocatableToken token);
    void endElement(LocatableToken token, boolean included);
    
    // For loop callbacks - used by test control flow
    void beginForLoop(LocatableToken token);
    void endForLoop(LocatableToken token, boolean included);
    void beginForLoopBody(LocatableToken token);
    void endForLoopBody(LocatableToken token, boolean included);
    
    // Method body callbacks - used by test scoping
    void beginMethodBody(LocatableToken token);
    void endMethodBody(LocatableToken token, boolean included);
    
    // Import callbacks - used by test import parsing
    void gotImport(List<LocatableToken> tokens, boolean isStatic,
                   LocatableToken importToken, LocatableToken semiToken);
    void gotWildcardImport(List<LocatableToken> tokens, boolean isStatic,
                          LocatableToken importToken, LocatableToken semiToken);
    void gotImportStmtSemi(LocatableToken token);
    
    // For loop test callbacks - used by test loop condition handling
    void gotForTest(boolean isPresent);
    void gotForIncrement(boolean isPresent);
    
    // Statement callbacks - used by test statement processing
    void gotStatementExpression();
    
    // K2 parser integration callbacks - used by K2ParserIntegrationTest
    void finishedCU(int state);
    void gotDeclBegin(LocatableToken token);
    void gotTypeDef(LocatableToken token, int tdType);
    void gotTypeDefName(LocatableToken token);
    void beginTypeDefExtends(LocatableToken token);
    void endTypeDefExtends();
    void beginTypeBody(LocatableToken token);
    void endTypeBody(LocatableToken token, boolean included);
    void gotTypeDefEnd(LocatableToken token, boolean included);
    void gotAllMethodParameters();
    void endMethodDecl(LocatableToken token, boolean included);
    void beginPackageStatement(LocatableToken token);
    void gotPackage(List<LocatableToken> tokens);
    void gotPackageSemi(LocatableToken token);
    void gotMethodParameter(LocatableToken nameToken, LocatableToken hiddenToken);
    void gotTypeSpec(List<LocatableToken> tokens);
    void gotModifier(LocatableToken token);
    void gotMethodDeclaration(LocatableToken nameToken, LocatableToken hiddenToken);
    
    // Switch statement callbacks - used by test switch parsing
    void beginSwitchStmt(LocatableToken token, boolean isExpression);
    void endSwitchStmt(LocatableToken token, boolean included);
    void beginSwitchCase(LocatableToken token);
    void endSwitchCase(LocatableToken token, boolean wasArrow);
    void gotSwitchCaseType(LocatableToken token, boolean isArrow);
    void gotSwitchDefault();
    void beginSwitchBlock(LocatableToken token);
    void endSwitchBlock(LocatableToken token);
    
    // Lambda callbacks - used by test lambda parsing
    void gotLambdaFormalName(LocatableToken token);
    void beginLambdaBody(boolean isBlock, LocatableToken token);
    void endLambdaBody(LocatableToken token);
    void gotMemberAccess(LocatableToken token);
    
    // Annotation and other callbacks - used by test parsing scenarios
    void gotAnnotation(List<LocatableToken> name, boolean hasParams);
    void determinedForLoop(boolean forEach, boolean hasInit);
}