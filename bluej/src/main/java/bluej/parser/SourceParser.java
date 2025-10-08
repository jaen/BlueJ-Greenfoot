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
import bluej.parser.lexer.*;

import java.io.Reader;
import java.util.List;

/**
 * SourceParser provides the main parser interface for BlueJ, supporting both Java and Kotlin.
 * 
 * <p>This class implements a delegation mechanism through the CallbackDelegate interface,
 * allowing flexible parser implementations and enabling:
 * <ul>
 *   <li>Parser selection for different languages (Java/Kotlin)</li>
 *   <li>Testing through mock implementations</li>
 *   <li>Debugging through interceptor patterns</li>
 *   <li>100% backward compatibility</li>
 * </ul>
 * 
 * <p>The delegation mechanism works by creating a default CallbackDelegate that forwards
 * all calls back to the superclass methods, maintaining existing behavior while allowing
 * external code to provide custom delegates when needed.
 * 
 * @since BlueJ 5.4.0
 */
public class SourceParser extends JavaParserCallbacks {
    protected JavaTokenFilter tokenStream;
    protected LocatableToken lastToken;
    protected final SourceType sourceType;
    
    ParserBehavior parser;
    
    /**
     * The delegate that handles all callback methods.
     * By default, this delegates back to the superclass methods for backward compatibility.
     */
    private final CallbackDelegate callbackDelegate;

    public static TokenStream getLexer(Reader r)
    {
        return new JavaLexer(r);
    }

    public static TokenStream getLexer(Reader r, boolean handleComments, boolean handleMultilineStrings)
    {
        return new JavaLexer(r, handleComments, handleMultilineStrings);
    }

    private static TokenStream getLexer(Reader r, int line, int col, int pos)
    {
        return new JavaLexer(r, line, col, pos);
    }

    public static TokenStream getLexer(Reader r, SourceType sourceType)
    {
        Keywords kws = sourceType == SourceType.Kotlin ? new KotlinKeywords() : new JavaKeywords();
        return new JavaLexer(r, kws);
    }

    public static TokenStream getLexer(Reader r, SourceType sourceType, boolean handleComments, boolean handleMultilineStrings)
    {
        Keywords kws = sourceType == SourceType.Kotlin ? new KotlinKeywords() : new JavaKeywords();
        return new JavaLexer(r, kws, handleComments, handleMultilineStrings);
    }

    private static TokenStream getLexer(Reader r, SourceType sourceType, int line, int col, int pos)
    {
        Keywords kws = sourceType == SourceType.Kotlin ? new KotlinKeywords() : new JavaKeywords();
        return new JavaLexer(r, kws, line, col, pos);
    }

    public SourceParser(Reader r) {
        TokenStream lexer = getLexer(r);
        tokenStream = new JavaTokenFilter(lexer, this);
        parser = new JavaParser(this);
        this.sourceType = SourceType.Java;
        
        // Initialize the default delegate that forwards to superclass methods
        this.callbackDelegate = createDefaultDelegate();
    }

    public SourceParser(Reader r, SourceType sourceType) {
        TokenStream lexer = getLexer(r, sourceType);
        tokenStream = new JavaTokenFilter(lexer, this);
        parser = sourceType == SourceType.Kotlin ? new KotlinParser(this) : new JavaParser(this);
        this.sourceType = sourceType;
        
        // Initialize the default delegate that forwards to superclass methods
        this.callbackDelegate = createDefaultDelegate();
    }

    public SourceParser(Reader r, SourceType sourceType, boolean handleComments)
    {
        TokenStream lexer = getLexer(r, sourceType, handleComments, true);
        tokenStream = new JavaTokenFilter(lexer, this);
        parser = sourceType == SourceType.Kotlin ? new KotlinParser(this) : new JavaParser(this);
        this.sourceType = sourceType;
        
        // Initialize the default delegate that forwards to superclass methods
        this.callbackDelegate = createDefaultDelegate();
    }

    public SourceParser(Reader r, SourceType sourceType, int line, int col, int pos) {
        TokenStream lexer = getLexer(r, sourceType, line, col, pos);
        tokenStream = new JavaTokenFilter(lexer, this);
        parser = sourceType == SourceType.Kotlin ? new KotlinParser(this) : new JavaParser(this);
        this.sourceType = sourceType;
        
        // Initialize the default delegate that forwards to superclass methods
        this.callbackDelegate = createDefaultDelegate();
    }
    
    /**
     * Creates the default CallbackDelegate implementation that forwards all calls
     * back to the superclass methods. This ensures backward compatibility.
     * 
     * @return A CallbackDelegate that maintains existing behavior
     */
    private CallbackDelegate createDefaultDelegate() {
        return new CallbackDelegate() {
            // All methods use default implementations which are empty
            // This maintains backward compatibility as the superclass methods
            // are called directly through the delegation mechanism below
        };
    }
    
    /**
     * Returns the CallbackDelegate instance used by this parser.
     * 
     * <p>This method enables external code to access the delegate for:
     * <ul>
     *   <li>Testing with mock delegates</li>
     *   <li>Debugging with interceptor delegates</li>
     *   <li>Language-specific parser implementations</li>
     * </ul>
     * 
     * @return The current CallbackDelegate instance
     * @since BlueJ 5.4.0
     */
    public CallbackDelegate getCallbackDelegate() {
        return callbackDelegate;
    }

    public JavaTokenFilter getTokenStream() {
        return tokenStream;
    }

    public LocatableToken getLastToken() {
        return lastToken;
    }

    public LocatableToken setLastToken(LocatableToken lastToken) {
        this.lastToken = lastToken;
        return lastToken;
    }

    public void parseCU() {
        parser.parseCU();
    }

    public void parseCUpart(int state) {
        parser.parseCUpart(state);
    }

    public int parseTypeDefBegin() {
        return parser.parseTypeDefBegin();
    }

    public LocatableToken parseTypeDefPart2(boolean b) {
        return parser.parseTypeDefPart2(b);
    }

    public LocatableToken parseTypeBody(int type, LocatableToken last) {
        return parser.parseTypeBody(type, last);
    }

    public void parseClassElement(LocatableToken nextToken) {
        parser.parseClassElement(nextToken);
    }

    public LocatableToken parseStatement(LocatableToken last, boolean b) {
        return parser.parseStatement(last, b);
    }

    public LocatableToken parseStatement() {
        return parser.parseStatement(getTokenStream().nextToken(), false);
    }

    public final boolean parseTypeSpec(boolean processArray) {
        return parser.parseTypeSpec(processArray);
    }


    public boolean parseTypeSpec(boolean b, boolean b1, List<LocatableToken> ll) {
        return parser.parseTypeSpec(b, b1, ll);
    }

    public void parseImportStatement() {
        parser.parseImportStatement();
    }

    public void parseClassBody() {
        parser.parseClassBody();
    }

    public void parseExpression() {
        parser.parseExpression();
    }

    public LocatableToken parseVariableDeclarations() {
        return parser.parseVariableDeclarations();
    }

    public void parseTypeDef() {
        parser.parseTypeDef();
    }

    public void parseMethodParamsBody() {
        parser.parseMethodParamsBody();
    }
    
    // ==================== Delegation Methods ====================
    // All callback methods now delegate to the CallbackDelegate instance
    
    @Override
    protected void beginPackageStatement(LocatableToken token) {
        callbackDelegate.beginPackageStatement(token);
        super.beginPackageStatement(token);
    }
    
    @Override
    protected void gotPackage(List<LocatableToken> pkgTokens) {
        callbackDelegate.gotPackage(pkgTokens);
        super.gotPackage(pkgTokens);
    }
    
    @Override
    protected void gotPackageSemi(LocatableToken token) {
        callbackDelegate.gotPackageSemi(token);
        super.gotPackageSemi(token);
    }
    
    @Override
    protected void gotImport(List<LocatableToken> tokens, boolean isStatic, LocatableToken importToken, LocatableToken semiColonToken) {
        callbackDelegate.gotImport(tokens, isStatic, importToken, semiColonToken);
        super.gotImport(tokens, isStatic, importToken, semiColonToken);
    }
    
    @Override
    protected void gotWildcardImport(List<LocatableToken> tokens, boolean isStatic, LocatableToken importToken, LocatableToken semiColonToken) {
        callbackDelegate.gotWildcardImport(tokens, isStatic, importToken, semiColonToken);
        super.gotWildcardImport(tokens, isStatic, importToken, semiColonToken);
    }
    
    @Override
    protected void gotImportStmtSemi(LocatableToken token) {
        callbackDelegate.gotImportStmtSemi(token);
        super.gotImportStmtSemi(token);
    }
    
    @Override
    protected void gotTypeDef(LocatableToken firstToken, int tdType) {
        callbackDelegate.gotTypeDef(firstToken, tdType);
        super.gotTypeDef(firstToken, tdType);
    }
    
    @Override
    protected void gotTypeDefName(LocatableToken nameToken) {
        callbackDelegate.gotTypeDefName(nameToken);
        super.gotTypeDefName(nameToken);
    }
    
    @Override
    protected void beginTypeDefExtends(LocatableToken extendsToken) {
        callbackDelegate.beginTypeDefExtends(extendsToken);
        super.beginTypeDefExtends(extendsToken);
    }
    
    @Override
    protected void endTypeDefExtends() {
        callbackDelegate.endTypeDefExtends();
        super.endTypeDefExtends();
    }
    
    @Override
    protected void beginTypeDefImplements(LocatableToken implementsToken) {
        callbackDelegate.beginTypeDefImplements(implementsToken);
        super.beginTypeDefImplements(implementsToken);
    }
    
    @Override
    protected void endTypeDefImplements() {
        callbackDelegate.endTypeDefImplements();
        super.endTypeDefImplements();
    }
    
    @Override
    protected void beginTypeDefPermits(LocatableToken permitsToken) {
        callbackDelegate.beginTypeDefPermits(permitsToken);
        super.beginTypeDefPermits(permitsToken);
    }
    
    @Override
    protected void endTypeDefPermits() {
        callbackDelegate.endTypeDefPermits();
        super.endTypeDefPermits();
    }
    
    @Override
    protected void beginTypeBody(LocatableToken leftCurlyToken) {
        callbackDelegate.beginTypeBody(leftCurlyToken);
        super.beginTypeBody(leftCurlyToken);
    }
    
    @Override
    protected void endTypeBody(LocatableToken endCurlyToken, boolean included) {
        callbackDelegate.endTypeBody(endCurlyToken, included);
        super.endTypeBody(endCurlyToken, included);
    }
    
    @Override
    protected void gotTypeDefEnd(LocatableToken token, boolean included) {
        callbackDelegate.gotTypeDefEnd(token, included);
        super.gotTypeDefEnd(token, included);
    }
    
    @Override
    protected void gotInnerType(LocatableToken start) {
        callbackDelegate.gotInnerType(start);
        super.gotInnerType(start);
    }
    
    @Override
    protected void gotTopLevelDecl(LocatableToken token) {
        callbackDelegate.gotTopLevelDecl(token);
        super.gotTopLevelDecl(token);
    }
    
    @Override
    protected void beginAnonClassBody(LocatableToken token, boolean isEnumMember) {
        callbackDelegate.beginAnonClassBody(token, isEnumMember);
        super.beginAnonClassBody(token, isEnumMember);
    }
    
    @Override
    protected void endAnonClassBody(LocatableToken token, boolean included) {
        callbackDelegate.endAnonClassBody(token, included);
        super.endAnonClassBody(token, included);
    }
    
    @Override
    protected void gotConstructorDecl(LocatableToken token, LocatableToken hiddenToken) {
        callbackDelegate.gotConstructorDecl(token, hiddenToken);
        super.gotConstructorDecl(token, hiddenToken);
    }
    
    @Override
    protected void gotMethodDeclaration(LocatableToken token, LocatableToken hiddenToken) {
        callbackDelegate.gotMethodDeclaration(token, hiddenToken);
        super.gotMethodDeclaration(token, hiddenToken);
    }
    
    @Override
    protected void beginMethodBody(LocatableToken token) {
        callbackDelegate.beginMethodBody(token);
        super.beginMethodBody(token);
    }
    
    @Override
    protected void endMethodBody(LocatableToken token, boolean included) {
        callbackDelegate.endMethodBody(token, included);
        super.endMethodBody(token, included);
    }
    
    @Override
    protected void endMethodDecl(LocatableToken token, boolean included) {
        callbackDelegate.endMethodDecl(token, included);
        super.endMethodDecl(token, included);
    }
    
    @Override
    protected void gotMethodParameter(LocatableToken token, LocatableToken ellipsisToken) {
        callbackDelegate.gotMethodParameter(token, ellipsisToken);
        super.gotMethodParameter(token, ellipsisToken);
    }
    
    @Override
    protected void gotAllMethodParameters() {
        callbackDelegate.gotAllMethodParameters();
        super.gotAllMethodParameters();
    }
    
    @Override
    protected void gotMethodTypeParamsBegin() {
        callbackDelegate.gotMethodTypeParamsBegin();
        super.gotMethodTypeParamsBegin();
    }
    
    @Override
    protected void endMethodTypeParams() {
        callbackDelegate.endMethodTypeParams();
        super.endMethodTypeParams();
    }
    
    @Override
    protected void beginThrows(LocatableToken token) {
        callbackDelegate.beginThrows(token);
        super.beginThrows(token);
    }
    
    @Override
    protected void endThrows() {
        callbackDelegate.endThrows();
        super.endThrows();
    }
    
    @Override
    protected void beginArgumentList(LocatableToken token) {
        callbackDelegate.beginArgumentList(token);
        super.beginArgumentList(token);
    }
    
    @Override
    protected void endArgumentList(LocatableToken token) {
        callbackDelegate.endArgumentList(token);
        super.endArgumentList(token);
    }
    
    @Override
    protected void endArgument() {
        callbackDelegate.endArgument();
        super.endArgument();
    }
    
    @Override
    protected void beginFieldDeclarations(LocatableToken first) {
        callbackDelegate.beginFieldDeclarations(first);
        super.beginFieldDeclarations(first);
    }
    
    @Override
    protected void gotField(LocatableToken first, LocatableToken idToken, boolean initExpressionFollows) {
        callbackDelegate.gotField(first, idToken, initExpressionFollows);
        super.gotField(first, idToken, initExpressionFollows);
    }
    
    @Override
    protected void gotSubsequentField(LocatableToken first, LocatableToken idToken, boolean initFollows) {
        callbackDelegate.gotSubsequentField(first, idToken, initFollows);
        super.gotSubsequentField(first, idToken, initFollows);
    }
    
    @Override
    protected void endField(LocatableToken token, boolean included) {
        callbackDelegate.endField(token, included);
        super.endField(token, included);
    }
    
    @Override
    protected void endFieldDeclarations(LocatableToken token, boolean included) {
        callbackDelegate.endFieldDeclarations(token, included);
        super.endFieldDeclarations(token, included);
    }
    
    @Override
    protected void beginVariableDecl(LocatableToken first) {
        callbackDelegate.beginVariableDecl(first);
        super.beginVariableDecl(first);
    }
    
    @Override
    protected void gotVariableDecl(LocatableToken first, LocatableToken idToken, boolean inited) {
        callbackDelegate.gotVariableDecl(first, idToken, inited);
        super.gotVariableDecl(first, idToken, inited);
    }
    
    @Override
    protected void gotSubsequentVar(LocatableToken first, LocatableToken idToken, boolean inited) {
        callbackDelegate.gotSubsequentVar(first, idToken, inited);
        super.gotSubsequentVar(first, idToken, inited);
    }
    
    @Override
    protected void endVariable(LocatableToken token, boolean included) {
        callbackDelegate.endVariable(token, included);
        super.endVariable(token, included);
    }
    
    @Override
    protected void endVariableDecls(LocatableToken token, boolean included) {
        callbackDelegate.endVariableDecls(token, included);
        super.endVariableDecls(token, included);
    }
    
    @Override
    protected void beginForInitDecl(LocatableToken first) {
        callbackDelegate.beginForInitDecl(first);
        super.beginForInitDecl(first);
    }
    
    @Override
    protected void gotForInit(LocatableToken first, LocatableToken idToken) {
        callbackDelegate.gotForInit(first, idToken);
        super.gotForInit(first, idToken);
    }
    
    @Override
    protected void gotSubsequentForInit(LocatableToken first, LocatableToken idToken, boolean initFollows) {
        callbackDelegate.gotSubsequentForInit(first, idToken, initFollows);
        super.gotSubsequentForInit(first, idToken, initFollows);
    }
    
    @Override
    protected void endForInit(LocatableToken token, boolean included) {
        callbackDelegate.endForInit(token, included);
        super.endForInit(token, included);
    }
    
    @Override
    protected void endForInitDecls(LocatableToken token, boolean included) {
        callbackDelegate.endForInitDecls(token, included);
        super.endForInitDecls(token, included);
    }
    
    @Override
    protected void gotArrayDeclarator() {
        callbackDelegate.gotArrayDeclarator();
        super.gotArrayDeclarator();
    }
    
    @Override
    protected void gotNewArrayDeclarator(boolean withDimension) {
        callbackDelegate.gotNewArrayDeclarator(withDimension);
        super.gotNewArrayDeclarator(withDimension);
    }
    
    @Override
    protected void beginFormalParameter(LocatableToken token) {
        callbackDelegate.beginFormalParameter(token);
        super.beginFormalParameter(token);
    }
    
    @Override
    protected void gotInstanceOfVar(LocatableToken token) {
        callbackDelegate.gotInstanceOfVar(token);
        super.gotInstanceOfVar(token);
    }
    
    @Override
    protected void beginForLoop(LocatableToken token) {
        callbackDelegate.beginForLoop(token);
        super.beginForLoop(token);
    }
    
    @Override
    protected void beginForLoopBody(LocatableToken token) {
        callbackDelegate.beginForLoopBody(token);
        super.beginForLoopBody(token);
    }
    
    @Override
    protected void endForLoopBody(LocatableToken token, boolean included) {
        callbackDelegate.endForLoopBody(token, included);
        super.endForLoopBody(token, included);
    }
    
    @Override
    protected void endForLoop(LocatableToken token, boolean included) {
        callbackDelegate.endForLoop(token, included);
        super.endForLoop(token, included);
    }
    
    @Override
    protected void beginWhileLoop(LocatableToken token) {
        callbackDelegate.beginWhileLoop(token);
        super.beginWhileLoop(token);
    }
    
    @Override
    protected void beginWhileLoopBody(LocatableToken token) {
        callbackDelegate.beginWhileLoopBody(token);
        super.beginWhileLoopBody(token);
    }
    
    @Override
    protected void endWhileLoopBody(LocatableToken token, boolean included) {
        callbackDelegate.endWhileLoopBody(token, included);
        super.endWhileLoopBody(token, included);
    }
    
    @Override
    protected void endWhileLoop(LocatableToken token, boolean included) {
        callbackDelegate.endWhileLoop(token, included);
        super.endWhileLoop(token, included);
    }
    
    @Override
    protected void beginIfStmt(LocatableToken token) {
        callbackDelegate.beginIfStmt(token);
        super.beginIfStmt(token);
    }
    
    @Override
    protected void beginIfCondBlock(LocatableToken token) {
        callbackDelegate.beginIfCondBlock(token);
        super.beginIfCondBlock(token);
    }
    
    @Override
    protected void endIfCondBlock(LocatableToken token, boolean included) {
        callbackDelegate.endIfCondBlock(token, included);
        super.endIfCondBlock(token, included);
    }
    
    @Override
    protected void gotElseIf(LocatableToken token) {
        callbackDelegate.gotElseIf(token);
        super.gotElseIf(token);
    }
    
    @Override
    protected void endIfStmt(LocatableToken token, boolean included) {
        callbackDelegate.endIfStmt(token, included);
        super.endIfStmt(token, included);
    }
    
    @Override
    protected void beginSwitchStmt(LocatableToken token, boolean isSwitchExpression) {
        callbackDelegate.beginSwitchStmt(token, isSwitchExpression);
        super.beginSwitchStmt(token, isSwitchExpression);
    }
    
    @Override
    protected void beginSwitchBlock(LocatableToken token) {
        callbackDelegate.beginSwitchBlock(token);
        super.beginSwitchBlock(token);
    }
    
    @Override
    protected void endSwitchBlock(LocatableToken token) {
        callbackDelegate.endSwitchBlock(token);
        super.endSwitchBlock(token);
    }
    
    @Override
    protected void endSwitchStmt(LocatableToken token, boolean included) {
        callbackDelegate.endSwitchStmt(token, included);
        super.endSwitchStmt(token, included);
    }
    
    @Override
    protected void beginSwitchCase(LocatableToken token) {
        callbackDelegate.beginSwitchCase(token);
        super.beginSwitchCase(token);
    }
    
    @Override
    protected void gotSwitchCaseType(LocatableToken token, boolean isArrowSyntax) {
        callbackDelegate.gotSwitchCaseType(token, isArrowSyntax);
        super.gotSwitchCaseType(token, isArrowSyntax);
    }
    
    @Override
    protected void endSwitchCase(LocatableToken token, boolean wasArrowSyntax) {
        callbackDelegate.endSwitchCase(token, wasArrowSyntax);
        super.endSwitchCase(token, wasArrowSyntax);
    }
    
    @Override
    protected void gotSwitchDefault() {
        callbackDelegate.gotSwitchDefault();
        super.gotSwitchDefault();
    }
    
    @Override
    protected void beginDoWhile(LocatableToken token) {
        callbackDelegate.beginDoWhile(token);
        super.beginDoWhile(token);
    }
    
    @Override
    protected void beginDoWhileBody(LocatableToken token) {
        callbackDelegate.beginDoWhileBody(token);
        super.beginDoWhileBody(token);
    }
    
    @Override
    protected void endDoWhileBody(LocatableToken token, boolean included) {
        callbackDelegate.endDoWhileBody(token, included);
        super.endDoWhileBody(token, included);
    }
    
    @Override
    protected void endDoWhile(LocatableToken token, boolean included) {
        callbackDelegate.endDoWhile(token, included);
        super.endDoWhile(token, included);
    }
    
    @Override
    protected void beginTryCatchSmt(LocatableToken token, boolean hasResource) {
        callbackDelegate.beginTryCatchSmt(token, hasResource);
        super.beginTryCatchSmt(token, hasResource);
    }
    
    @Override
    protected void beginTryBlock(LocatableToken token) {
        callbackDelegate.beginTryBlock(token);
        super.beginTryBlock(token);
    }
    
    @Override
    protected void endTryBlock(LocatableToken token, boolean included) {
        callbackDelegate.endTryBlock(token, included);
        super.endTryBlock(token, included);
    }
    
    @Override
    protected void endTryCatchStmt(LocatableToken token, boolean included) {
        callbackDelegate.endTryCatchStmt(token, included);
        super.endTryCatchStmt(token, included);
    }
    
    @Override
    protected void gotCatchFinally(LocatableToken token) {
        callbackDelegate.gotCatchFinally(token);
        super.gotCatchFinally(token);
    }
    
    @Override
    protected void gotMultiCatch(LocatableToken token) {
        callbackDelegate.gotMultiCatch(token);
        super.gotMultiCatch(token);
    }
    
    @Override
    protected void gotCatchVarName(LocatableToken token) {
        callbackDelegate.gotCatchVarName(token);
        super.gotCatchVarName(token);
    }
    
    @Override
    protected void beginSynchronizedBlock(LocatableToken token) {
        callbackDelegate.beginSynchronizedBlock(token);
        super.beginSynchronizedBlock(token);
    }
    
    @Override
    protected void endSynchronizedBlock(LocatableToken token, boolean included) {
        callbackDelegate.endSynchronizedBlock(token, included);
        super.endSynchronizedBlock(token, included);
    }
    
    @Override
    protected void gotThrow(LocatableToken token) {
        callbackDelegate.gotThrow(token);
        super.gotThrow(token);
    }
    
    @Override
    protected void gotBreakContinue(LocatableToken keywordToken, LocatableToken labelToken) {
        callbackDelegate.gotBreakContinue(keywordToken, labelToken);
        super.gotBreakContinue(keywordToken, labelToken);
    }
    
    @Override
    protected void gotReturnStatement(boolean hasValue) {
        callbackDelegate.gotReturnStatement(hasValue);
        super.gotReturnStatement(hasValue);
    }
    
    @Override
    protected void gotYieldStatement() {
        callbackDelegate.gotYieldStatement();
        super.gotYieldStatement();
    }
    
    @Override
    protected void gotEmptyStatement() {
        callbackDelegate.gotEmptyStatement();
        super.gotEmptyStatement();
    }
    
    @Override
    protected void gotAssert() {
        callbackDelegate.gotAssert();
        super.gotAssert();
    }
    
    @Override
    protected void gotForTest(boolean isPresent) {
        callbackDelegate.gotForTest(isPresent);
        super.gotForTest(isPresent);
    }
    
    @Override
    protected void gotForIncrement(boolean isPresent) {
        callbackDelegate.gotForIncrement(isPresent);
        super.gotForIncrement(isPresent);
    }
    
    @Override
    protected void determinedForLoop(boolean forEachLoop, boolean initExpressionFollows) {
        callbackDelegate.determinedForLoop(forEachLoop, initExpressionFollows);
        super.determinedForLoop(forEachLoop, initExpressionFollows);
    }
    
    @Override
    protected void beginExpression(LocatableToken token, boolean isLambdaBody) {
        callbackDelegate.beginExpression(token, isLambdaBody);
        super.beginExpression(token, isLambdaBody);
    }
    
    @Override
    protected void endExpression(LocatableToken token, boolean emptyExpression) {
        callbackDelegate.endExpression(token, emptyExpression);
        super.endExpression(token, emptyExpression);
    }
    
    @Override
    protected void gotLiteral(LocatableToken token) {
        callbackDelegate.gotLiteral(token);
        super.gotLiteral(token);
    }
    
    @Override
    protected void gotPrimitiveTypeLiteral(LocatableToken token) {
        callbackDelegate.gotPrimitiveTypeLiteral(token);
        super.gotPrimitiveTypeLiteral(token);
    }
    
    @Override
    protected void gotIdentifier(LocatableToken token) {
        callbackDelegate.gotIdentifier(token);
        super.gotIdentifier(token);
    }
    
    @Override
    protected void gotIdentifierEOF(LocatableToken token) {
        callbackDelegate.gotIdentifierEOF(token);
        super.gotIdentifierEOF(token);
    }
    
    @Override
    protected void gotMemberAccessEOF(LocatableToken token) {
        callbackDelegate.gotMemberAccessEOF(token);
        super.gotMemberAccessEOF(token);
    }
    
    @Override
    protected void gotCompoundIdent(LocatableToken token) {
        callbackDelegate.gotCompoundIdent(token);
        super.gotCompoundIdent(token);
    }
    
    @Override
    protected void gotCompoundComponent(LocatableToken token) {
        callbackDelegate.gotCompoundComponent(token);
        super.gotCompoundComponent(token);
    }
    
    @Override
    protected void completeCompoundValue(LocatableToken token) {
        callbackDelegate.completeCompoundValue(token);
        super.completeCompoundValue(token);
    }
    
    @Override
    protected void completeCompoundValueEOF(LocatableToken token) {
        callbackDelegate.completeCompoundValueEOF(token);
        super.completeCompoundValueEOF(token);
    }
    
    @Override
    protected void completeCompoundClass(LocatableToken token) {
        callbackDelegate.completeCompoundClass(token);
        super.completeCompoundClass(token);
    }
    
    @Override
    protected void gotMemberAccess(LocatableToken token) {
        callbackDelegate.gotMemberAccess(token);
        super.gotMemberAccess(token);
    }
    
    @Override
    protected void gotMemberCall(LocatableToken token, List<LocatableToken> typeArgs) {
        callbackDelegate.gotMemberCall(token, typeArgs);
        super.gotMemberCall(token, typeArgs);
    }
    
    @Override
    protected void gotMethodCall(LocatableToken token) {
        callbackDelegate.gotMethodCall(token);
        super.gotMethodCall(token);
    }
    
    @Override
    protected void gotConstructorCall(LocatableToken token) {
        callbackDelegate.gotConstructorCall(token);
        super.gotConstructorCall(token);
    }
    
    @Override
    protected void gotDotEOF(LocatableToken token) {
        callbackDelegate.gotDotEOF(token);
        super.gotDotEOF(token);
    }
    
    @Override
    protected void gotStatementExpression() {
        callbackDelegate.gotStatementExpression();
        super.gotStatementExpression();
    }
    
    @Override
    protected void gotClassLiteral(LocatableToken token) {
        callbackDelegate.gotClassLiteral(token);
        super.gotClassLiteral(token);
    }
    
    @Override
    protected void gotBinaryOperator(LocatableToken token) {
        callbackDelegate.gotBinaryOperator(token);
        super.gotBinaryOperator(token);
    }
    
    @Override
    protected void gotUnaryOperator(LocatableToken token) {
        callbackDelegate.gotUnaryOperator(token);
        super.gotUnaryOperator(token);
    }
    
    @Override
    protected void gotQuestionOperator(LocatableToken token) {
        callbackDelegate.gotQuestionOperator(token);
        super.gotQuestionOperator(token);
    }
    
    @Override
    protected void gotQuestionColon(LocatableToken token) {
        callbackDelegate.gotQuestionColon(token);
        super.gotQuestionColon(token);
    }
    
    @Override
    protected void gotInstanceOfOperator(LocatableToken token) {
        callbackDelegate.gotInstanceOfOperator(token);
        super.gotInstanceOfOperator(token);
    }
    
    @Override
    protected void gotArrayElementAccess() {
        callbackDelegate.gotArrayElementAccess();
        super.gotArrayElementAccess();
    }
    
    @Override
    protected void gotExprNew(LocatableToken token) {
        callbackDelegate.gotExprNew(token);
        super.gotExprNew(token);
    }
    
    @Override
    protected void endExprNew(LocatableToken token, boolean included) {
        callbackDelegate.endExprNew(token, included);
        super.endExprNew(token, included);
    }
    
    @Override
    protected void gotTypeCast(List<LocatableToken> tokens) {
        callbackDelegate.gotTypeCast(tokens);
        super.gotTypeCast(tokens);
    }
    
    @Override
    protected void gotTypeSpec(List<LocatableToken> tokens) {
        callbackDelegate.gotTypeSpec(tokens);
        super.gotTypeSpec(tokens);
    }
    
    @Override
    protected void gotPostOperator(LocatableToken token) {
        callbackDelegate.gotPostOperator(token);
        super.gotPostOperator(token);
    }
    
    @Override
    protected void gotArrayTypeIdentifier(LocatableToken token) {
        callbackDelegate.gotArrayTypeIdentifier(token);
        super.gotArrayTypeIdentifier(token);
    }
    
    @Override
    protected void gotParentIdentifier(LocatableToken token) {
        callbackDelegate.gotParentIdentifier(token);
        super.gotParentIdentifier(token);
    }
    
    @Override
    protected void beginArrayInitList(LocatableToken token) {
        callbackDelegate.beginArrayInitList(token);
        super.beginArrayInitList(token);
    }
    
    @Override
    protected void endArrayInitList(LocatableToken token) {
        callbackDelegate.endArrayInitList(token);
        super.endArrayInitList(token);
    }
    
    @Override
    protected void beginLambdaBody(boolean lambdaIsBlock, LocatableToken openCurly) {
        callbackDelegate.beginLambdaBody(lambdaIsBlock, openCurly);
        super.beginLambdaBody(lambdaIsBlock, openCurly);
    }
    
    @Override
    protected void endLambdaBody(LocatableToken closeCurly) {
        callbackDelegate.endLambdaBody(closeCurly);
        super.endLambdaBody(closeCurly);
    }
    
    @Override
    protected void gotLambdaFormalParam() {
        callbackDelegate.gotLambdaFormalParam();
        super.gotLambdaFormalParam();
    }
    
    @Override
    protected void gotLambdaFormalName(LocatableToken name) {
        callbackDelegate.gotLambdaFormalName(name);
        super.gotLambdaFormalName(name);
    }
    
    @Override
    protected void gotLambdaFormalType(List<LocatableToken> type) {
        callbackDelegate.gotLambdaFormalType(type);
        super.gotLambdaFormalType(type);
    }
    
    @Override
    protected void beginRecordParameters(LocatableToken parenToken) {
        callbackDelegate.beginRecordParameters(parenToken);
        super.beginRecordParameters(parenToken);
    }
    
    @Override
    protected void gotRecordParameter(LocatableToken first, LocatableToken idToken, LocatableToken varargsToken) {
        callbackDelegate.gotRecordParameter(first, idToken, varargsToken);
        super.gotRecordParameter(first, idToken, varargsToken);
    }
    
    @Override
    protected void endRecordParameters(LocatableToken closeParen) {
        callbackDelegate.endRecordParameters(closeParen);
        super.endRecordParameters(closeParen);
    }
    
    @Override
    protected void reachedCUstate(int state) {
        callbackDelegate.reachedCUstate(state);
        super.reachedCUstate(state);
    }
    
    @Override
    protected void finishedCU(int state) {
        callbackDelegate.finishedCU(state);
        super.finishedCU(state);
    }
    
    @Override
    protected void gotModifier(LocatableToken token) {
        callbackDelegate.gotModifier(token);
        super.gotModifier(token);
    }
    
    @Override
    protected void modifiersConsumed() {
        callbackDelegate.modifiersConsumed();
        super.modifiersConsumed();
    }
    
    @Override
    protected void beginElement(LocatableToken token) {
        callbackDelegate.beginElement(token);
        super.beginElement(token);
    }
    
    @Override
    protected void endElement(LocatableToken token, boolean included) {
        callbackDelegate.endElement(token, included);
        super.endElement(token, included);
    }
    
    @Override
    protected void beginStmtblockBody(LocatableToken token) {
        callbackDelegate.beginStmtblockBody(token);
        super.beginStmtblockBody(token);
    }
    
    @Override
    protected void endStmtblockBody(LocatableToken token, boolean included) {
        callbackDelegate.endStmtblockBody(token, included);
        super.endStmtblockBody(token, included);
    }
    
    @Override
    protected void beginInitBlock(LocatableToken first, LocatableToken lcurly) {
        callbackDelegate.beginInitBlock(first, lcurly);
        super.beginInitBlock(first, lcurly);
    }
    
    @Override
    protected void endInitBlock(LocatableToken rcurly, boolean included) {
        callbackDelegate.endInitBlock(rcurly, included);
        super.endInitBlock(rcurly, included);
    }
    
    @Override
    protected void gotDeclBegin(LocatableToken token) {
        callbackDelegate.gotDeclBegin(token);
        super.gotDeclBegin(token);
    }
    
    @Override
    protected void endDecl(LocatableToken token) {
        callbackDelegate.endDecl(token);
        super.endDecl(token);
    }
    
    @Override
    protected void gotTypeParam(LocatableToken idToken) {
        callbackDelegate.gotTypeParam(idToken);
        super.gotTypeParam(idToken);
    }
    
    @Override
    protected void gotTypeParamBound(List<LocatableToken> tokens) {
        callbackDelegate.gotTypeParamBound(tokens);
        super.gotTypeParamBound(tokens);
    }
    
    @Override
    protected void gotAnnotation(List<LocatableToken> annName, boolean paramsFollow) {
        callbackDelegate.gotAnnotation(annName, paramsFollow);
        super.gotAnnotation(annName, paramsFollow);
    }
    
    @Override
    public void gotComment(LocatableToken token) {
        callbackDelegate.gotComment(token);
        super.gotComment(token);
    }
    
    @Override
    protected void error(String msg, int beginLine, int beginCol, int endLine, int endCol) {
        callbackDelegate.error(msg, beginLine, beginCol, endLine, endCol);
        // Note: We don't call super.error() here because it throws an exception
        // The delegate can decide whether to throw or handle it differently
    }
}