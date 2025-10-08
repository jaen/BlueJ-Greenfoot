/*
 This file is part of the BlueJ program. 
 Copyright (C) 2024  Michael Kolling and John Rosenberg

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
 * CallbackDelegate interface that exposes all protected callback methods from JavaParserCallbacks.
 * 
 * <p>This interface enables flexible parser implementations by providing access to all parsing
 * callbacks that were previously protected. It supports:
 * <ul>
 *   <li>Better testing through mock implementations</li>
 *   <li>Debugging through interceptor patterns</li>
 *   <li>Flexible parser selection for Kotlin integration</li>
 *   <li>100% backward compatibility through default methods</li>
 * </ul>
 * 
 * <p>All methods are implemented as default (empty) methods to maintain backward compatibility.
 * Implementations can override only the methods they need.
 * 
 * <p>The methods are organized into the following categories:
 * <ol>
 *   <li>Package Management - Handling package declarations</li>
 *   <li>Import Statements - Processing import declarations</li>
 *   <li>Type Definitions - Class, interface, enum, and annotation definitions</li>
 *   <li>Method/Constructor Declarations - Method and constructor parsing</li>
 *   <li>Field/Variable Declarations - Field and variable handling</li>
 *   <li>Control Flow Structures - Loops, conditionals, and exception handling</li>
 *   <li>Expression Parsing - Expression evaluation and operators</li>
 *   <li>Lambda Expressions - Lambda function support</li>
 *   <li>Records - Record type support</li>
 *   <li>Compilation Unit - Overall compilation unit structure</li>
 *   <li>Annotations - Annotation processing</li>
 *   <li>Error Handling - Parse error management</li>
 * </ol>
 * 
 * @since BlueJ 5.4.0
 * @author BlueJ Development Team
 */
public interface CallbackDelegate {
    
    // ==================== Package Management (3 methods) ====================
    
    /**
     * Called when a package statement is encountered.
     * @param token The "package" token
     */
    default void beginPackageStatement(LocatableToken token) { }
    
    /**
     * Called when the package name has been parsed from a package statement.
     * @param pkgTokens The tokens making up the package name (including the dots)
     */
    default void gotPackage(List<LocatableToken> pkgTokens) { }
    
    /**
     * Called when the semicolon at the end of a package statement is encountered.
     * @param token The semicolon token
     */
    default void gotPackageSemi(LocatableToken token) { }
    
    // ==================== Import Statements (3 methods) ====================
    
    /**
     * Called when a complete import statement has been parsed.
     * @param tokens The tokens making up the imported type/package
     * @param isStatic Whether this is a static import
     * @param importToken The "import" keyword token
     * @param semiColonToken The semicolon token ending the import
     */
    default void gotImport(List<LocatableToken> tokens, boolean isStatic, 
                          LocatableToken importToken, LocatableToken semiColonToken) { }
    
    /**
     * Called when a wildcard import statement has been parsed.
     * @param tokens The tokens making up the imported package
     * @param isStatic Whether this is a static import
     * @param importToken The "import" keyword token
     * @param semiColonToken The semicolon token ending the import
     */
    default void gotWildcardImport(List<LocatableToken> tokens, boolean isStatic,
                                   LocatableToken importToken, LocatableToken semiColonToken) { }
    
    /**
     * Called when the semicolon at the end of an import statement is encountered.
     * @param token The semicolon token
     */
    default void gotImportStmtSemi(LocatableToken token) { }
    
    // ==================== Type Definitions (15 methods) ====================
    
    /**
     * Called when a type definition (class/interface/enum/annotation) is recognized.
     * @param firstToken The first token of the type definition
     * @param tdType The type of definition (TYPEDEF_CLASS, TYPEDEF_INTERFACE, TYPEDEF_ANNOTATION, or TYPEDEF_ENUM)
     */
    default void gotTypeDef(LocatableToken firstToken, int tdType) { }
    
    /**
     * Called when the identifier token for a type definition is encountered.
     * @param nameToken The token containing the type name
     */
    default void gotTypeDefName(LocatableToken nameToken) { }
    
    /**
     * Called when the "extends" keyword in a type definition is encountered.
     * @param extendsToken The "extends" token
     */
    default void beginTypeDefExtends(LocatableToken extendsToken) { }
    
    /**
     * Called after the last type in an "extends" clause has been parsed.
     */
    default void endTypeDefExtends() { }
    
    /**
     * Called when the "implements" keyword in a type definition is encountered.
     * @param implementsToken The "implements" token
     */
    default void beginTypeDefImplements(LocatableToken implementsToken) { }
    
    /**
     * Called after the last type in an "implements" clause has been parsed.
     */
    default void endTypeDefImplements() { }
    
    /**
     * Called when the "permits" keyword in a sealed type definition is encountered.
     * @param permitsToken The "permits" token
     */
    default void beginTypeDefPermits(LocatableToken permitsToken) { }
    
    /**
     * Called after the last type in a "permits" clause has been parsed.
     */
    default void endTypeDefPermits() { }
    
    /**
     * Called when the opening brace of a type body is encountered.
     * @param leftCurlyToken The opening brace token
     */
    default void beginTypeBody(LocatableToken leftCurlyToken) { }
    
    /**
     * Called when the closing brace of a type body is encountered.
     * @param endCurlyToken The closing brace token
     * @param included Whether the token is included in the type body
     */
    default void endTypeBody(LocatableToken endCurlyToken, boolean included) { }
    
    /**
     * Called when the end of a type definition is reached.
     * @param token The ending token
     * @param included Whether the token is included in the type definition
     */
    default void gotTypeDefEnd(LocatableToken token, boolean included) { }
    
    /**
     * Called when an inner type definition is encountered.
     * @param start The starting token of the inner type
     */
    default void gotInnerType(LocatableToken start) { }
    
    /**
     * Called when a top-level declaration is encountered.
     * @param token The token marking the top-level declaration
     */
    default void gotTopLevelDecl(LocatableToken token) { }
    
    /**
     * Called when the beginning of an anonymous class body is encountered.
     * @param token The opening brace token
     * @param isEnumMember Whether this is an enum member body
     */
    default void beginAnonClassBody(LocatableToken token, boolean isEnumMember) { }
    
    /**
     * Called when the end of an anonymous class body is reached.
     * @param token The closing brace token
     * @param included Whether the token is included in the anonymous class
     */
    default void endAnonClassBody(LocatableToken token, boolean included) { }
    
    // ==================== Method/Constructor Declarations (13 methods) ====================
    
    /**
     * Called when a constructor declaration is encountered.
     * @param token The constructor name token
     * @param hiddenToken The comment token before the constructor (if any)
     */
    default void gotConstructorDecl(LocatableToken token, LocatableToken hiddenToken) { }
    
    /**
     * Called when a method declaration is encountered.
     * @param token The method name token
     * @param hiddenToken The comment token before the method (if any)
     */
    default void gotMethodDeclaration(LocatableToken token, LocatableToken hiddenToken) { }
    
    /**
     * Called when the opening brace of a method or constructor body is encountered.
     * @param token The opening brace token
     */
    default void beginMethodBody(LocatableToken token) { }
    
    /**
     * Called when the closing brace of a method or constructor body is encountered.
     * @param token The closing brace token
     * @param included Whether the token is included in the method body
     */
    default void endMethodBody(LocatableToken token, boolean included) { }
    
    /**
     * Called when the end of a method or constructor declaration is reached.
     * @param token The ending token
     * @param included Whether the token is included in the declaration
     */
    default void endMethodDecl(LocatableToken token, boolean included) { }
    
    /**
     * Called when a method or constructor parameter is encountered.
     * @param token The parameter name token
     * @param ellipsisToken The varargs ellipsis token (if any)
     */
    default void gotMethodParameter(LocatableToken token, LocatableToken ellipsisToken) { }
    
    /**
     * Called when all method parameters have been parsed.
     */
    default void gotAllMethodParameters() { }
    
    /**
     * Called when the beginning of method type parameters is encountered.
     */
    default void gotMethodTypeParamsBegin() { }
    
    /**
     * Called when the end of method type parameters is reached.
     */
    default void endMethodTypeParams() { }
    
    /**
     * Called when the "throws" keyword in a method declaration is encountered.
     * @param token The "throws" token
     */
    default void beginThrows(LocatableToken token) { }
    
    /**
     * Called after the throws clause has been parsed.
     */
    default void endThrows() { }
    
    /**
     * Called when the opening parenthesis of an argument list is encountered.
     * @param token The opening parenthesis token
     */
    default void beginArgumentList(LocatableToken token) { }
    
    /**
     * Called when the closing parenthesis of an argument list is encountered.
     * @param token The closing parenthesis token
     */
    default void endArgumentList(LocatableToken token) { }
    
    /**
     * Called when an individual argument in an argument list has been parsed.
     */
    default void endArgument() { }
    
    // ==================== Field/Variable Declarations (19 methods) ====================
    
    /**
     * Called when the beginning of field declarations is encountered.
     * @param first The first token of the field declaration
     */
    default void beginFieldDeclarations(LocatableToken first) { }
    
    /**
     * Called when a field declaration is encountered.
     * @param first The first token of the field declaration
     * @param idToken The field identifier token
     * @param initExpressionFollows Whether an initialization expression follows
     */
    default void gotField(LocatableToken first, LocatableToken idToken, boolean initExpressionFollows) { }
    
    /**
     * Called when a subsequent field in a multi-field declaration is encountered.
     * @param first The first token of the declaration
     * @param idToken The field identifier token
     * @param initFollows Whether an initialization expression follows
     */
    default void gotSubsequentField(LocatableToken first, LocatableToken idToken, boolean initFollows) { }
    
    /**
     * Called when a single field declaration ends.
     * @param token The ending token
     * @param included Whether the token is included in the field
     */
    default void endField(LocatableToken token, boolean included) { }
    
    /**
     * Called when field declarations end.
     * @param token The ending token
     * @param included Whether the token is included in the declarations
     */
    default void endFieldDeclarations(LocatableToken token, boolean included) { }
    
    /**
     * Called when the beginning of a variable declaration is encountered.
     * @param first The first token of the variable declaration
     */
    default void beginVariableDecl(LocatableToken first) { }
    
    /**
     * Called when a variable declaration is encountered.
     * @param first The first token of the declaration
     * @param idToken The variable identifier token
     * @param inited Whether the variable is initialized
     */
    default void gotVariableDecl(LocatableToken first, LocatableToken idToken, boolean inited) { }
    
    /**
     * Called when a subsequent variable in a multi-variable declaration is encountered.
     * @param first The first token of the declaration
     * @param idToken The variable identifier token
     * @param inited Whether the variable is initialized
     */
    default void gotSubsequentVar(LocatableToken first, LocatableToken idToken, boolean inited) { }
    
    /**
     * Called when a single variable declaration ends.
     * @param token The ending token
     * @param included Whether the token is included in the variable
     */
    default void endVariable(LocatableToken token, boolean included) { }
    
    /**
     * Called when variable declarations end.
     * @param token The ending token
     * @param included Whether the token is included in the declarations
     */
    default void endVariableDecls(LocatableToken token, boolean included) { }
    
    /**
     * Called when the beginning of a for loop initialization declaration is encountered.
     * @param first The first token of the initialization
     */
    default void beginForInitDecl(LocatableToken first) { }
    
    /**
     * Called when a for loop initialization variable is encountered.
     * @param first The first token of the declaration
     * @param idToken The variable identifier token
     */
    default void gotForInit(LocatableToken first, LocatableToken idToken) { }
    
    /**
     * Called when a subsequent for loop initialization variable is encountered.
     * @param first The first token of the declaration
     * @param idToken The variable identifier token
     * @param initFollows Whether an initialization expression follows
     */
    default void gotSubsequentForInit(LocatableToken first, LocatableToken idToken, boolean initFollows) { }
    
    /**
     * Called when a single for loop initialization ends.
     * @param token The ending token
     * @param included Whether the token is included in the initialization
     */
    default void endForInit(LocatableToken token, boolean included) { }
    
    /**
     * Called when for loop initialization declarations end.
     * @param token The ending token
     * @param included Whether the token is included in the declarations
     */
    default void endForInitDecls(LocatableToken token, boolean included) { }
    
    /**
     * Called when array declarators "[]" are encountered after a parameter/field/variable name.
     */
    default void gotArrayDeclarator() { }
    
    /**
     * Called when array declarators are encountered in a new array expression.
     * @param withDimension Whether the array has a specified dimension
     */
    default void gotNewArrayDeclarator(boolean withDimension) { }
    
    /**
     * Called when the beginning of a formal parameter is encountered.
     * @param token The first token of the parameter
     */
    default void beginFormalParameter(LocatableToken token) { }
    
    /**
     * Called when a variable name follows an "instanceof" operator.
     * @param token The variable name token
     */
    default void gotInstanceOfVar(LocatableToken token) { }
    
    // ==================== Control Flow Structures (43 methods) ====================
    
    /**
     * Called when the beginning of a for loop is encountered.
     * @param token The "for" keyword token
     */
    default void beginForLoop(LocatableToken token) { }
    
    /**
     * Called when the beginning of a for loop body is encountered.
     * @param token The opening brace or statement token
     */
    default void beginForLoopBody(LocatableToken token) { }
    
    /**
     * Called when the end of a for loop body is reached.
     * @param token The closing brace or statement end token
     * @param included Whether the token is included in the body
     */
    default void endForLoopBody(LocatableToken token, boolean included) { }
    
    /**
     * Called when the end of a for loop is reached.
     * @param token The ending token
     * @param included Whether the token is included in the loop
     */
    default void endForLoop(LocatableToken token, boolean included) { }
    
    /**
     * Called when the beginning of a while loop is encountered.
     * @param token The "while" keyword token
     */
    default void beginWhileLoop(LocatableToken token) { }
    
    /**
     * Called when the beginning of a while loop body is encountered.
     * @param token The opening brace or statement token
     */
    default void beginWhileLoopBody(LocatableToken token) { }
    
    /**
     * Called when the end of a while loop body is reached.
     * @param token The closing brace or statement end token
     * @param included Whether the token is included in the body
     */
    default void endWhileLoopBody(LocatableToken token, boolean included) { }
    
    /**
     * Called when the end of a while loop is reached.
     * @param token The ending token
     * @param included Whether the token is included in the loop
     */
    default void endWhileLoop(LocatableToken token, boolean included) { }
    
    /**
     * Called when the beginning of an if statement is encountered.
     * @param token The "if" keyword token
     */
    default void beginIfStmt(LocatableToken token) { }
    
    /**
     * Called when the beginning of an if conditional block is encountered.
     * @param token The opening brace or statement token
     */
    default void beginIfCondBlock(LocatableToken token) { }
    
    /**
     * Called when the end of an if conditional block is reached.
     * @param token The closing brace or statement end token
     * @param included Whether the token is included in the block
     */
    default void endIfCondBlock(LocatableToken token, boolean included) { }
    
    /**
     * Called when an "else if" clause is encountered.
     * @param token The "else" or "if" token
     */
    default void gotElseIf(LocatableToken token) { }
    
    /**
     * Called when the end of an if statement is reached.
     * @param token The ending token
     * @param included Whether the token is included in the statement
     */
    default void endIfStmt(LocatableToken token, boolean included) { }
    
    /**
     * Called when the beginning of a switch statement is encountered.
     * @param token The "switch" keyword token
     * @param isSwitchExpression Whether this is a switch expression
     */
    default void beginSwitchStmt(LocatableToken token, boolean isSwitchExpression) { }
    
    /**
     * Called when the opening brace of a switch block is encountered.
     * @param token The opening brace token
     */
    default void beginSwitchBlock(LocatableToken token) { }
    
    /**
     * Called when the closing brace of a switch block is encountered.
     * @param token The closing brace token
     */
    default void endSwitchBlock(LocatableToken token) { }
    
    /**
     * Called when the end of a switch statement is reached.
     * @param token The ending token
     * @param included Whether the token is included in the statement
     */
    default void endSwitchStmt(LocatableToken token, boolean included) { }
    
    /**
     * Called when the beginning of a switch case is encountered.
     * @param token The "case" keyword token
     */
    default void beginSwitchCase(LocatableToken token) { }
    
    /**
     * Called when a switch case type has been parsed.
     * @param token The case label token
     * @param isArrowSyntax Whether arrow syntax is used
     */
    default void gotSwitchCaseType(LocatableToken token, boolean isArrowSyntax) { }
    
    /**
     * Called when the end of a switch case is reached.
     * @param token The ending token
     * @param wasArrowSyntax Whether arrow syntax was used
     */
    default void endSwitchCase(LocatableToken token, boolean wasArrowSyntax) { }
    
    /**
     * Called when a switch default case is encountered.
     */
    default void gotSwitchDefault() { }
    
    /**
     * Called when the beginning of a do-while loop is encountered.
     * @param token The "do" keyword token
     */
    default void beginDoWhile(LocatableToken token) { }
    
    /**
     * Called when the beginning of a do-while loop body is encountered.
     * @param token The opening brace or statement token
     */
    default void beginDoWhileBody(LocatableToken token) { }
    
    /**
     * Called when the end of a do-while loop body is reached.
     * @param token The closing brace or statement end token
     * @param included Whether the token is included in the body
     */
    default void endDoWhileBody(LocatableToken token, boolean included) { }
    
    /**
     * Called when the end of a do-while loop is reached.
     * @param token The ending token
     * @param included Whether the token is included in the loop
     */
    default void endDoWhile(LocatableToken token, boolean included) { }
    
    /**
     * Called when the beginning of a try-catch statement is encountered.
     * @param token The "try" keyword token
     * @param hasResource Whether the try statement has resources
     */
    default void beginTryCatchSmt(LocatableToken token, boolean hasResource) { }
    
    /**
     * Called when the opening brace of a try block is encountered.
     * @param token The opening brace token
     */
    default void beginTryBlock(LocatableToken token) { }
    
    /**
     * Called when the closing brace of a try block is encountered.
     * @param token The closing brace token
     * @param included Whether the token is included in the block
     */
    default void endTryBlock(LocatableToken token, boolean included) { }
    
    /**
     * Called when the end of a try-catch statement is reached.
     * @param token The ending token
     * @param included Whether the token is included in the statement
     */
    default void endTryCatchStmt(LocatableToken token, boolean included) { }
    
    /**
     * Called when a catch or finally clause is encountered.
     * @param token The "catch" or "finally" keyword token
     */
    default void gotCatchFinally(LocatableToken token) { }
    
    /**
     * Called when a multi-catch clause is encountered.
     * @param token The pipe token between exception types
     */
    default void gotMultiCatch(LocatableToken token) { }
    
    /**
     * Called when a catch variable name is encountered.
     * @param token The variable name token
     */
    default void gotCatchVarName(LocatableToken token) { }
    
    /**
     * Called when the beginning of a synchronized block is encountered.
     * @param token The "synchronized" keyword token
     */
    default void beginSynchronizedBlock(LocatableToken token) { }
    
    /**
     * Called when the end of a synchronized block is reached.
     * @param token The closing brace token
     * @param included Whether the token is included in the block
     */
    default void endSynchronizedBlock(LocatableToken token, boolean included) { }
    
    /**
     * Called when a throw statement is encountered.
     * @param token The "throw" keyword token
     */
    default void gotThrow(LocatableToken token) { }
    
    /**
     * Called when a break or continue statement is encountered.
     * @param keywordToken The "break" or "continue" keyword token
     * @param labelToken The optional label token
     */
    default void gotBreakContinue(LocatableToken keywordToken, LocatableToken labelToken) { }
    
    /**
     * Called when a return statement is encountered.
     * @param hasValue Whether the return statement has a value
     */
    default void gotReturnStatement(boolean hasValue) { }
    
    /**
     * Called when a yield statement is encountered.
     */
    default void gotYieldStatement() { }
    
    /**
     * Called when an empty statement is encountered.
     */
    default void gotEmptyStatement() { }
    
    /**
     * Called when an assert statement is encountered.
     */
    default void gotAssert() { }
    
    /**
     * Called when the test condition of a for loop has been parsed.
     * @param isPresent Whether a test condition is present
     */
    default void gotForTest(boolean isPresent) { }
    
    /**
     * Called when the increment part of a for loop has been parsed.
     * @param isPresent Whether an increment expression is present
     */
    default void gotForIncrement(boolean isPresent) { }
    
    /**
     * Called when the type of for loop has been determined.
     * @param forEachLoop Whether this is a for-each loop
     * @param initExpressionFollows Whether an initialization expression follows
     */
    default void determinedForLoop(boolean forEachLoop, boolean initExpressionFollows) { }
    
    // ==================== Expression Parsing (38 methods) ====================
    
    /**
     * Called when the beginning of an expression is encountered.
     * @param token The first token of the expression
     * @param isLambdaBody Whether this expression is a lambda body
     */
    default void beginExpression(LocatableToken token, boolean isLambdaBody) { }
    
    /**
     * Called when the end of an expression is reached.
     * @param token The token after the expression
     * @param emptyExpression Whether the expression was empty
     */
    default void endExpression(LocatableToken token, boolean emptyExpression) { }
    
    /**
     * Called when a literal value is encountered in an expression.
     * @param token The literal token
     */
    default void gotLiteral(LocatableToken token) { }
    
    /**
     * Called when a primitive type literal is encountered (e.g., int.class).
     * @param token The primitive type token
     */
    default void gotPrimitiveTypeLiteral(LocatableToken token) { }
    
    /**
     * Called when an identifier is encountered in an expression.
     * @param token The identifier token
     */
    default void gotIdentifier(LocatableToken token) { }
    
    /**
     * Called when an identifier is followed by end-of-file.
     * @param token The identifier token
     */
    default void gotIdentifierEOF(LocatableToken token) { }
    
    /**
     * Called when a member access is followed by end-of-file.
     * @param token The member access token
     */
    default void gotMemberAccessEOF(LocatableToken token) { }
    
    /**
     * Called when a compound identifier is encountered.
     * @param token The identifier token
     */
    default void gotCompoundIdent(LocatableToken token) { }
    
    /**
     * Called when a component of a compound identifier is encountered.
     * @param token The component token
     */
    default void gotCompoundComponent(LocatableToken token) { }
    
    /**
     * Called when a compound value is completed.
     * @param token The final token
     */
    default void completeCompoundValue(LocatableToken token) { }
    
    /**
     * Called when a compound value is completed at end-of-file.
     * @param token The final token
     */
    default void completeCompoundValueEOF(LocatableToken token) { }
    
    /**
     * Called when a compound class reference is completed.
     * @param token The final token
     */
    default void completeCompoundClass(LocatableToken token) { }
    
    /**
     * Called when a member access operation is encountered.
     * @param token The member name token
     */
    default void gotMemberAccess(LocatableToken token) { }
    
    /**
     * Called when a member method call is encountered.
     * @param token The method name token
     * @param typeArgs The type arguments for the method
     */
    default void gotMemberCall(LocatableToken token, List<LocatableToken> typeArgs) { }
    
    /**
     * Called when a method call is encountered.
     * @param token The method name token
     */
    default void gotMethodCall(LocatableToken token) { }
    
    /**
     * Called when a constructor call (this() or super()) is encountered.
     * @param token The "this" or "super" keyword token
     */
    default void gotConstructorCall(LocatableToken token) { }
    
    /**
     * Called when a dot operator is followed by end-of-file.
     * @param token The dot token
     */
    default void gotDotEOF(LocatableToken token) { }
    
    /**
     * Called when a statement expression is encountered.
     */
    default void gotStatementExpression() { }
    
    /**
     * Called when a class literal is encountered (e.g., String.class).
     * @param token The "class" keyword token
     */
    default void gotClassLiteral(LocatableToken token) { }
    
    /**
     * Called when a binary operator is encountered.
     * @param token The operator token
     */
    default void gotBinaryOperator(LocatableToken token) { }
    
    /**
     * Called when a unary operator is encountered.
     * @param token The operator token
     */
    default void gotUnaryOperator(LocatableToken token) { }
    
    /**
     * Called when a ternary question mark operator is encountered.
     * @param token The "?" token
     */
    default void gotQuestionOperator(LocatableToken token) { }
    
    /**
     * Called when the colon in a ternary expression is encountered.
     * @param token The ":" token
     */
    default void gotQuestionColon(LocatableToken token) { }
    
    /**
     * Called when an instanceof operator is encountered.
     * @param token The "instanceof" keyword token
     */
    default void gotInstanceOfOperator(LocatableToken token) { }
    
    /**
     * Called when an array element access operation is encountered.
     */
    default void gotArrayElementAccess() { }
    
    /**
     * Called when a "new" expression is encountered.
     * @param token The "new" keyword token
     */
    default void gotExprNew(LocatableToken token) { }
    
    /**
     * Called when the end of a "new" expression is reached.
     * @param token The ending token
     * @param included Whether the token is included in the expression
     */
    default void endExprNew(LocatableToken token, boolean included) { }
    
    /**
     * Called when a type cast operation is encountered.
     * @param tokens The tokens making up the cast type
     */
    default void gotTypeCast(List<LocatableToken> tokens) { }
    
    /**
     * Called when a type specification is encountered.
     * @param tokens The tokens making up the type
     */
    default void gotTypeSpec(List<LocatableToken> tokens) { }
    
    /**
     * Called when a post-increment or post-decrement operator is encountered.
     * @param token The operator token
     */
    default void gotPostOperator(LocatableToken token) { }
    
    /**
     * Called when an array type identifier is encountered.
     * @param token The identifier token
     */
    default void gotArrayTypeIdentifier(LocatableToken token) { }
    
    /**
     * Called when a parent identifier is encountered.
     * @param token The identifier token
     */
    default void gotParentIdentifier(LocatableToken token) { }
    
    /**
     * Called when the beginning of an array initializer list is encountered.
     * @param token The opening brace token
     */
    default void beginArrayInitList(LocatableToken token) { }
    
    /**
     * Called when the end of an array initializer list is reached.
     * @param token The closing brace token
     */
    default void endArrayInitList(LocatableToken token) { }
    
    // ==================== Lambda Expressions (7 methods) ====================
    
    /**
     * Called when the beginning of a lambda body is encountered.
     * @param lambdaIsBlock Whether the lambda body is a block
     * @param openCurly The opening brace token (if block)
     */
    default void beginLambdaBody(boolean lambdaIsBlock, LocatableToken openCurly) { }
    
    /**
     * Called when the end of a lambda body is reached.
     * @param closeCurly The closing brace token (if block)
     */
    default void endLambdaBody(LocatableToken closeCurly) { }
    
    /**
     * Called when a lambda formal parameter is encountered.
     */
    default void gotLambdaFormalParam() { }
    
    /**
     * Called when a lambda formal parameter name is encountered.
     * @param name The parameter name token
     */
    default void gotLambdaFormalName(LocatableToken name) { }
    
    /**
     * Called when a lambda formal parameter type is encountered.
     * @param type The tokens making up the parameter type
     */
    default void gotLambdaFormalType(List<LocatableToken> type) { }
    
    // ==================== Records (4 methods) ====================
    
    /**
     * Called when the opening parenthesis of record parameters is encountered.
     * @param parenToken The opening parenthesis token
     */
    default void beginRecordParameters(LocatableToken parenToken) { }
    
    /**
     * Called when a record parameter is encountered.
     * @param first The first token of the parameter
     * @param idToken The parameter identifier token
     * @param varargsToken The varargs token (if any)
     */
    default void gotRecordParameter(LocatableToken first, LocatableToken idToken, LocatableToken varargsToken) { }
    
    /**
     * Called when the closing parenthesis of record parameters is encountered.
     * @param closeParen The closing parenthesis token
     */
    default void endRecordParameters(LocatableToken closeParen) { }
    
    // ==================== Compilation Unit (2 methods) ====================
    
    /**
     * Called when a compilation unit state is reached.
     * @param state 1 = package statement parsed, 2 = one or more type definitions parsed
     */
    default void reachedCUstate(int state) { }
    
    /**
     * Called when parsing of a compilation unit is finished.
     * @param state The final state
     */
    default void finishedCU(int state) { }
    
    // ==================== Miscellaneous (13 methods) ====================
    
    /**
     * Called when a modifier (public, private, etc.) is encountered.
     * @param token The modifier token
     */
    default void gotModifier(LocatableToken token) { }
    
    /**
     * Called when modifiers have been consumed.
     */
    default void modifiersConsumed() { }
    
    /**
     * Called when the beginning of an arbitrary grammatical element is encountered.
     * @param token The starting token
     */
    default void beginElement(LocatableToken token) { }
    
    /**
     * Called when the end of an arbitrary grammatical element is reached.
     * @param token The ending token
     * @param included Whether the token is part of the element
     */
    default void endElement(LocatableToken token, boolean included) { }
    
    /**
     * Called when the beginning of a statement block body is encountered.
     * @param token The opening brace token
     */
    default void beginStmtblockBody(LocatableToken token) { }
    
    /**
     * Called when the end of a statement block body is reached.
     * @param token The closing brace token
     * @param included Whether the token is included in the block
     */
    default void endStmtblockBody(LocatableToken token, boolean included) { }
    
    /**
     * Called when the beginning of an initialization block is encountered.
     * @param first The first token (either "static" or "{")
     * @param lcurly The opening brace token
     */
    default void beginInitBlock(LocatableToken first, LocatableToken lcurly) { }
    
    /**
     * Called when the end of an initialization block is reached.
     * @param rcurly The closing brace token
     * @param included Whether the token is included in the block
     */
    default void endInitBlock(LocatableToken rcurly, boolean included) { }
    
    /**
     * Called when the beginning of a declaration is encountered.
     * @param token The first token of the declaration
     */
    default void gotDeclBegin(LocatableToken token) { }
    
    /**
     * Called when a declaration ends unsuccessfully.
     * @param token The ending token
     */
    default void endDecl(LocatableToken token) { }
    
    /**
     * Called when a type parameter is encountered.
     * @param idToken The type parameter identifier token
     */
    default void gotTypeParam(LocatableToken idToken) { }
    
    /**
     * Called when a type parameter bound is encountered.
     * @param tokens The tokens making up the bound
     */
    default void gotTypeParamBound(List<LocatableToken> tokens) { }
    
    // ==================== Annotations (1 method) ====================
    
    /**
     * Called when an annotation is encountered.
     * @param annName The tokens making up the annotation name
     * @param paramsFollow Whether parameters follow the annotation
     */
    default void gotAnnotation(List<LocatableToken> annName, boolean paramsFollow) { }
    
    // ==================== Comments (1 method) ====================
    
    /**
     * Called when a comment is encountered by the lexer.
     * @param token The comment token
     */
    default void gotComment(LocatableToken token) { }
    
    // ==================== Error Handling (1 method) ====================
    
    /**
     * Called when a parse error occurs.
     * @param msg The error message
     * @param beginLine The line where the error begins
     * @param beginCol The column where the error begins
     * @param endLine The line where the error ends
     * @param endCol The column where the error ends
     */
    default void error(String msg, int beginLine, int beginCol, int endLine, int endCol) {
        throw new ParseFailure("Parse error: (" + beginLine + ":" + beginCol + ") :" + msg);
    }
}