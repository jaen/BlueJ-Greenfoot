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

public class SourceParser extends JavaParserCallbacks {
    protected JavaTokenFilter tokenStream;
    protected LocatableToken lastToken;
    protected final SourceType sourceType;

    ParserBehavior parser;

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
        this.sourceType = SourceType.Java;
        
        // Use protected methods for initialization (allows overriding)
        this.tokenStream = createLexer(r, sourceType, true, true, 1, 1, 0);
        this.parser = createParser(tokenStream, sourceType);
    }

    public SourceParser(Reader r, SourceType sourceType) {
        this.sourceType = sourceType;
        
        // Use protected methods for initialization (allows overriding)
        this.tokenStream = createLexer(r, sourceType, true, true, 1, 1, 0);
        this.parser = createParser(tokenStream, sourceType);
    }

    public SourceParser(Reader r, SourceType sourceType, boolean handleComments)
    {
        this.sourceType = sourceType;
        
        // Use protected methods for initialization (allows overriding)
        this.tokenStream = createLexer(r, sourceType, handleComments, true, 1, 1, 0);
        this.parser = createParser(tokenStream, sourceType);
    }

    public SourceParser(Reader r, SourceType sourceType, int line, int col, int pos) {
        this.sourceType = sourceType;
        
        // Use protected methods for initialization (allows overriding)
        this.tokenStream = createLexer(r, sourceType, true, true, line, col, pos);
        this.parser = createParser(tokenStream, sourceType);
    }

    /**
     * Creates and configures the lexer for this parser.
     * Subclasses can override this to provide alternative lexer implementations.
     *
     * @param reader The input reader
     * @param sourceType The type of source being parsed
     * @param handleComments Whether to handle comments during lexing
     * @param handleMultilineStrings Whether to handle multiline strings during lexing
     * @param line Starting line number
     * @param col Starting column number
     * @param pos Starting position
     * @return Configured lexer instance
     */
    protected JavaTokenFilter createLexer(Reader reader, SourceType sourceType, boolean handleComments, boolean handleMultilineStrings, int line, int col, int pos) {
        TokenStream lexer;
        if (line == 1 && col == 1 && pos == 0) {
            // Use the simpler constructor when at default position
            if (handleComments && handleMultilineStrings) {
                lexer = getLexer(reader, sourceType);
            } else {
                lexer = getLexer(reader, sourceType, handleComments, handleMultilineStrings);
            }
        } else {
            lexer = getLexer(reader, sourceType, line, col, pos);
        }
        return new JavaTokenFilter(lexer, this);
    }

    /**
     * Creates and configures the parser for this SourceParser.
     * Subclasses can override this to provide alternative parser implementations.
     *
     * @param tokenStream The lexer/token stream to use
     * @param sourceType The type of source being parsed
     * @return Configured parser instance
     */
    protected ParserBehavior createParser(JavaTokenFilter tokenStream, SourceType sourceType) {
        return sourceType == SourceType.Kotlin ? new KotlinParserAdapter(this) : new JavaParser(this);
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

    // ==================== CallbackDelegate Support ====================

    /**
     * Provides access to callback functionality for testing and extension.
     * Uses anonymous implementation to directly access protected callback methods.
     * @return A CallbackDelegate instance for making callback calls
     */
    public CallbackDelegate getCallbackDelegate() {
        return new CallbackDelegate() {
            @Override
            public void beginExpression(LocatableToken token, boolean included) {
                SourceParser.this.beginExpression(token, included);
            }
            
            @Override
            public void endExpression(LocatableToken token, boolean included) {
                SourceParser.this.endExpression(token, included);
            }
            
            @Override
            public void gotLiteral(LocatableToken token) {
                SourceParser.this.gotLiteral(token);
            }
            
            @Override
            public void gotIdentifier(LocatableToken token) {
                SourceParser.this.gotIdentifier(token);
            }
            
            @Override
            public void gotBinaryOperator(LocatableToken token) {
                SourceParser.this.gotBinaryOperator(token);
            }
            
            @Override
            public void gotMethodCall(LocatableToken token) {
                SourceParser.this.gotMethodCall(token);
            }
            
            @Override
            public void gotMemberCall(LocatableToken token, List<LocatableToken> typeArgs) {
                SourceParser.this.gotMemberCall(token, typeArgs);
            }
            
            @Override
            public void beginArgumentList(LocatableToken token) {
                SourceParser.this.beginArgumentList(token);
            }
            
            @Override
            public void endArgumentList(LocatableToken token) {
                SourceParser.this.endArgumentList(token);
            }
            
            @Override
            public void endArgument() {
                SourceParser.this.endArgument();
            }
            
            @Override
            public void beginIfStmt(LocatableToken token) {
                SourceParser.this.beginIfStmt(token);
            }
            
            @Override
            public void endIfStmt(LocatableToken token, boolean included) {
                SourceParser.this.endIfStmt(token, included);
            }
            
            @Override
            public void beginIfCondBlock(LocatableToken token) {
                SourceParser.this.beginIfCondBlock(token);
            }
            
            @Override
            public void endIfCondBlock(LocatableToken token, boolean included) {
                SourceParser.this.endIfCondBlock(token, included);
            }
            
            @Override
            public void gotElseIf(LocatableToken token) {
                SourceParser.this.gotElseIf(token);
            }
            
            @Override
            public void beginElement(LocatableToken token) {
                SourceParser.this.beginElement(token);
            }
            
            @Override
            public void endElement(LocatableToken token, boolean included) {
                SourceParser.this.endElement(token, included);
            }
            
            @Override
            public void beginForLoop(LocatableToken token) {
                SourceParser.this.beginForLoop(token);
            }
            
            @Override
            public void endForLoop(LocatableToken token, boolean included) {
                SourceParser.this.endForLoop(token, included);
            }
            
            @Override
            public void beginForLoopBody(LocatableToken token) {
                SourceParser.this.beginForLoopBody(token);
            }
            
            @Override
            public void endForLoopBody(LocatableToken token, boolean included) {
                SourceParser.this.endForLoopBody(token, included);
            }
            
            @Override
            public void beginMethodBody(LocatableToken token) {
                SourceParser.this.beginMethodBody(token);
            }
            
            @Override
            public void endMethodBody(LocatableToken token, boolean included) {
                SourceParser.this.endMethodBody(token, included);
            }
            
            @Override
            public void gotImport(List<LocatableToken> tokens, boolean isStatic,
                                LocatableToken importToken, LocatableToken semiToken) {
                SourceParser.this.gotImport(tokens, isStatic, importToken, semiToken);
            }
            
            @Override
            public void gotWildcardImport(List<LocatableToken> tokens, boolean isStatic,
                                        LocatableToken importToken, LocatableToken semiToken) {
                SourceParser.this.gotWildcardImport(tokens, isStatic, importToken, semiToken);
            }
            
            @Override
            public void gotImportStmtSemi(LocatableToken token) {
                SourceParser.this.gotImportStmtSemi(token);
            }
            
            @Override
            public void gotForTest(boolean isPresent) {
                SourceParser.this.gotForTest(isPresent);
            }
            
            @Override
            public void gotForIncrement(boolean isPresent) {
                SourceParser.this.gotForIncrement(isPresent);
            }
            
            @Override
            public void gotStatementExpression() {
                SourceParser.this.gotStatementExpression();
            }
            
            // K2 parser integration callbacks
            @Override
            public void finishedCU(int state) {
                SourceParser.this.finishedCU(state);
            }
            
            @Override
            public void gotDeclBegin(LocatableToken token) {
                SourceParser.this.gotDeclBegin(token);
            }
            
            @Override
            public void gotTypeDef(LocatableToken token, int tdType) {
                SourceParser.this.gotTypeDef(token, tdType);
            }
            
            @Override
            public void gotTypeDefName(LocatableToken token) {
                SourceParser.this.gotTypeDefName(token);
            }
            
            @Override
            public void beginTypeDefExtends(LocatableToken token) {
                SourceParser.this.beginTypeDefExtends(token);
            }
            
            @Override
            public void endTypeDefExtends() {
                SourceParser.this.endTypeDefExtends();
            }
            
            @Override
            public void beginTypeBody(LocatableToken token) {
                SourceParser.this.beginTypeBody(token);
            }
            
            @Override
            public void endTypeBody(LocatableToken token, boolean included) {
                SourceParser.this.endTypeBody(token, included);
            }
            
            @Override
            public void gotTypeDefEnd(LocatableToken token, boolean included) {
                SourceParser.this.gotTypeDefEnd(token, included);
            }
            
            @Override
            public void gotAllMethodParameters() {
                SourceParser.this.gotAllMethodParameters();
            }
            
            @Override
            public void endMethodDecl(LocatableToken token, boolean included) {
                SourceParser.this.endMethodDecl(token, included);
            }
            
            @Override
            public void beginPackageStatement(LocatableToken token) {
                SourceParser.this.beginPackageStatement(token);
            }
            
            @Override
            public void gotPackage(List<LocatableToken> tokens) {
                SourceParser.this.gotPackage(tokens);
            }
            
            @Override
            public void gotPackageSemi(LocatableToken token) {
                SourceParser.this.gotPackageSemi(token);
            }
            
            @Override
            public void gotMethodParameter(LocatableToken nameToken, LocatableToken hiddenToken) {
                SourceParser.this.gotMethodParameter(nameToken, hiddenToken);
            }
            
            @Override
            public void gotTypeSpec(List<LocatableToken> tokens) {
                SourceParser.this.gotTypeSpec(tokens);
            }
            
            @Override
            public void gotModifier(LocatableToken token) {
                SourceParser.this.gotModifier(token);
            }
            
            @Override
            public void gotMethodDeclaration(LocatableToken nameToken, LocatableToken hiddenToken) {
                SourceParser.this.gotMethodDeclaration(nameToken, hiddenToken);
            }
            
            // Switch statement callbacks
            @Override
            public void beginSwitchStmt(LocatableToken token, boolean isExpression) {
                SourceParser.this.beginSwitchStmt(token, isExpression);
            }
            
            @Override
            public void endSwitchStmt(LocatableToken token, boolean included) {
                SourceParser.this.endSwitchStmt(token, included);
            }
            
            @Override
            public void beginSwitchCase(LocatableToken token) {
                SourceParser.this.beginSwitchCase(token);
            }
            
            @Override
            public void endSwitchCase(LocatableToken token, boolean wasArrow) {
                SourceParser.this.endSwitchCase(token, wasArrow);
            }
            
            @Override
            public void gotSwitchCaseType(LocatableToken token, boolean isArrow) {
                SourceParser.this.gotSwitchCaseType(token, isArrow);
            }
            
            @Override
            public void gotSwitchDefault() {
                SourceParser.this.gotSwitchDefault();
            }
            
            @Override
            public void beginSwitchBlock(LocatableToken token) {
                SourceParser.this.beginSwitchBlock(token);
            }
            
            @Override
            public void endSwitchBlock(LocatableToken token) {
                SourceParser.this.endSwitchBlock(token);
            }
            
            // Lambda callbacks
            @Override
            public void gotLambdaFormalName(LocatableToken token) {
                SourceParser.this.gotLambdaFormalName(token);
            }
            
            @Override
            public void beginLambdaBody(boolean isBlock, LocatableToken token) {
                SourceParser.this.beginLambdaBody(isBlock, token);
            }
            
            @Override
            public void endLambdaBody(LocatableToken token) {
                SourceParser.this.endLambdaBody(token);
            }
            
            @Override
            public void gotMemberAccess(LocatableToken token) {
                SourceParser.this.gotMemberAccess(token);
            }
            
            // Annotation and other callbacks
            @Override
            public void gotAnnotation(List<LocatableToken> name, boolean hasParams) {
                SourceParser.this.gotAnnotation(name, hasParams);
            }
            
            @Override
            public void determinedForLoop(boolean forEach, boolean hasInit) {
                SourceParser.this.determinedForLoop(forEach, hasInit);
            }
            
            // ==================== Additional Missing Method Implementations ====================
            
            // Modifier and declaration callbacks
            @Override
            public void modifiersConsumed() {
                SourceParser.this.modifiersConsumed();
            }
            
            @Override
            public void reachedCUstate(int i) {
                SourceParser.this.reachedCUstate(i);
            }
            
            @Override
            public void endDecl(LocatableToken token) {
                SourceParser.this.endDecl(token);
            }
            
            // While loop callbacks
            @Override
            public void beginWhileLoop(LocatableToken token) {
                SourceParser.this.beginWhileLoop(token);
            }
            
            @Override
            public void beginWhileLoopBody(LocatableToken token) {
                SourceParser.this.beginWhileLoopBody(token);
            }
            
            @Override
            public void endWhileLoopBody(LocatableToken token, boolean included) {
                SourceParser.this.endWhileLoopBody(token, included);
            }
            
            @Override
            public void endWhileLoop(LocatableToken token, boolean included) {
                SourceParser.this.endWhileLoop(token, included);
            }
            
            // Do-while loop callbacks
            @Override
            public void beginDoWhile(LocatableToken token) {
                SourceParser.this.beginDoWhile(token);
            }
            
            @Override
            public void beginDoWhileBody(LocatableToken token) {
                SourceParser.this.beginDoWhileBody(token);
            }
            
            @Override
            public void endDoWhileBody(LocatableToken token, boolean included) {
                SourceParser.this.endDoWhileBody(token, included);
            }
            
            @Override
            public void endDoWhile(LocatableToken token, boolean included) {
                SourceParser.this.endDoWhile(token, included);
            }
            
            // Try-catch callbacks
            @Override
            public void beginTryCatchSmt(LocatableToken token, boolean hasResource) {
                SourceParser.this.beginTryCatchSmt(token, hasResource);
            }
            
            @Override
            public void beginTryBlock(LocatableToken token) {
                SourceParser.this.beginTryBlock(token);
            }
            
            @Override
            public void endTryBlock(LocatableToken token, boolean included) {
                SourceParser.this.endTryBlock(token, included);
            }
            
            @Override
            public void endTryCatchStmt(LocatableToken token, boolean included) {
                SourceParser.this.endTryCatchStmt(token, included);
            }
            
            @Override
            public void gotCatchFinally(LocatableToken token) {
                SourceParser.this.gotCatchFinally(token);
            }
            
            @Override
            public void gotMultiCatch(LocatableToken token) {
                SourceParser.this.gotMultiCatch(token);
            }
            
            @Override
            public void gotCatchVarName(LocatableToken token) {
                SourceParser.this.gotCatchVarName(token);
            }
            
            // Synchronized block callbacks
            @Override
            public void beginSynchronizedBlock(LocatableToken token) {
                SourceParser.this.beginSynchronizedBlock(token);
            }
            
            @Override
            public void endSynchronizedBlock(LocatableToken token, boolean included) {
                SourceParser.this.endSynchronizedBlock(token, included);
            }
            
            // Expression and operator callbacks
            @Override
            public void gotExprNew(LocatableToken token) {
                SourceParser.this.gotExprNew(token);
            }
            
            @Override
            public void endExprNew(LocatableToken token, boolean included) {
                SourceParser.this.endExprNew(token, included);
            }
            
            @Override
            public void gotUnaryOperator(LocatableToken token) {
                SourceParser.this.gotUnaryOperator(token);
            }
            
            @Override
            public void gotQuestionOperator(LocatableToken token) {
                SourceParser.this.gotQuestionOperator(token);
            }
            
            @Override
            public void gotQuestionColon(LocatableToken token) {
                SourceParser.this.gotQuestionColon(token);
            }
            
            @Override
            public void gotInstanceOfOperator(LocatableToken token) {
                SourceParser.this.gotInstanceOfOperator(token);
            }
            
            @Override
            public void gotInstanceOfVar(LocatableToken token) {
                SourceParser.this.gotInstanceOfVar(token);
            }
            
            @Override
            public void gotArrayElementAccess() {
                SourceParser.this.gotArrayElementAccess();
            }
            
            @Override
            public void gotPostOperator(LocatableToken token) {
                SourceParser.this.gotPostOperator(token);
            }
            
            @Override
            public void gotClassLiteral(LocatableToken token) {
                SourceParser.this.gotClassLiteral(token);
            }
            
            @Override
            public void gotPrimitiveTypeLiteral(LocatableToken token) {
                SourceParser.this.gotPrimitiveTypeLiteral(token);
            }
            
            @Override
            public void gotConstructorCall(LocatableToken token) {
                SourceParser.this.gotConstructorCall(token);
            }
            
            @Override
            public void gotDotEOF(LocatableToken token) {
                SourceParser.this.gotDotEOF(token);
            }
            
            @Override
            public void gotTypeCast(List<LocatableToken> tokens) {
                SourceParser.this.gotTypeCast(tokens);
            }
            
            // Array callbacks
            @Override
            public void beginArrayInitList(LocatableToken token) {
                SourceParser.this.beginArrayInitList(token);
            }
            
            @Override
            public void endArrayInitList(LocatableToken token) {
                SourceParser.this.endArrayInitList(token);
            }
            
            @Override
            public void gotArrayDeclarator() {
                SourceParser.this.gotArrayDeclarator();
            }
            
            @Override
            public void gotNewArrayDeclarator(boolean withDimension) {
                SourceParser.this.gotNewArrayDeclarator(withDimension);
            }
            
            // Anonymous class callbacks
            @Override
            public void beginAnonClassBody(LocatableToken token, boolean isEnumMember) {
                SourceParser.this.beginAnonClassBody(token, isEnumMember);
            }
            
            @Override
            public void endAnonClassBody(LocatableToken token, boolean included) {
                SourceParser.this.endAnonClassBody(token, included);
            }
            
            // Statement block callbacks
            @Override
            public void beginStmtblockBody(LocatableToken token) {
                SourceParser.this.beginStmtblockBody(token);
            }
            
            @Override
            public void endStmtblockBody(LocatableToken token, boolean included) {
                SourceParser.this.endStmtblockBody(token, included);
            }
            
            // Initializer block callbacks
            @Override
            public void beginInitBlock(LocatableToken first, LocatableToken lcurly) {
                SourceParser.this.beginInitBlock(first, lcurly);
            }
            
            @Override
            public void endInitBlock(LocatableToken rcurly, boolean included) {
                SourceParser.this.endInitBlock(rcurly, included);
            }
            
            // Type definition extends/implements/permits callbacks
            @Override
            public void beginTypeDefImplements(LocatableToken implementsToken) {
                SourceParser.this.beginTypeDefImplements(implementsToken);
            }
            
            @Override
            public void endTypeDefImplements() {
                SourceParser.this.endTypeDefImplements();
            }
            
            @Override
            public void beginTypeDefPermits(LocatableToken permitsToken) {
                SourceParser.this.beginTypeDefPermits(permitsToken);
            }
            
            @Override
            public void endTypeDefPermits() {
                SourceParser.this.endTypeDefPermits();
            }
            
            // Variable declaration callbacks
            @Override
            public void beginVariableDecl(LocatableToken first) {
                SourceParser.this.beginVariableDecl(first);
            }
            
            @Override
            public void gotVariableDecl(LocatableToken first, LocatableToken idToken, boolean inited) {
                SourceParser.this.gotVariableDecl(first, idToken, inited);
            }
            
            @Override
            public void gotSubsequentVar(LocatableToken first, LocatableToken idToken, boolean inited) {
                SourceParser.this.gotSubsequentVar(first, idToken, inited);
            }
            
            @Override
            public void endVariable(LocatableToken token, boolean included) {
                SourceParser.this.endVariable(token, included);
            }
            
            @Override
            public void endVariableDecls(LocatableToken token, boolean included) {
                SourceParser.this.endVariableDecls(token, included);
            }
            
            // For loop initialization callbacks
            @Override
            public void beginForInitDecl(LocatableToken first) {
                SourceParser.this.beginForInitDecl(first);
            }
            
            @Override
            public void gotForInit(LocatableToken first, LocatableToken idToken) {
                SourceParser.this.gotForInit(first, idToken);
            }
            
            @Override
            public void gotSubsequentForInit(LocatableToken first, LocatableToken idToken, boolean initFollows) {
                SourceParser.this.gotSubsequentForInit(first, idToken, initFollows);
            }
            
            @Override
            public void endForInit(LocatableToken token, boolean included) {
                SourceParser.this.endForInit(token, included);
            }
            
            @Override
            public void endForInitDecls(LocatableToken token, boolean included) {
                SourceParser.this.endForInitDecls(token, included);
            }
            
            // Field declaration callbacks
            @Override
            public void beginFieldDeclarations(LocatableToken first) {
                SourceParser.this.beginFieldDeclarations(first);
            }
            
            @Override
            public void gotField(LocatableToken first, LocatableToken idToken, boolean initExpressionFollows) {
                SourceParser.this.gotField(first, idToken, initExpressionFollows);
            }
            
            @Override
            public void gotSubsequentField(LocatableToken first, LocatableToken idToken, boolean initFollows) {
                SourceParser.this.gotSubsequentField(first, idToken, initFollows);
            }
            
            @Override
            public void endField(LocatableToken token, boolean included) {
                SourceParser.this.endField(token, included);
            }
            
            @Override
            public void endFieldDeclarations(LocatableToken token, boolean included) {
                SourceParser.this.endFieldDeclarations(token, included);
            }
            
            // Constructor callbacks
            @Override
            public void gotConstructorDecl(LocatableToken token, LocatableToken hiddenToken) {
                SourceParser.this.gotConstructorDecl(token, hiddenToken);
            }
            
            // Type parameter callbacks
            @Override
            public void gotTypeParam(LocatableToken idToken) {
                SourceParser.this.gotTypeParam(idToken);
            }
            
            @Override
            public void gotTypeParamBound(List<LocatableToken> tokens) {
                SourceParser.this.gotTypeParamBound(tokens);
            }
            
            @Override
            public void gotMethodTypeParamsBegin() {
                SourceParser.this.gotMethodTypeParamsBegin();
            }
            
            @Override
            public void endMethodTypeParams() {
                SourceParser.this.endMethodTypeParams();
            }
            
            // Throws clause callbacks
            @Override
            public void beginThrows(LocatableToken token) {
                SourceParser.this.beginThrows(token);
            }
            
            @Override
            public void endThrows() {
                SourceParser.this.endThrows();
            }
            
            // Identifier and compound access callbacks
            @Override
            public void gotIdentifierEOF(LocatableToken token) {
                SourceParser.this.gotIdentifierEOF(token);
            }
            
            @Override
            public void gotMemberAccessEOF(LocatableToken token) {
                SourceParser.this.gotMemberAccessEOF(token);
            }
            
            @Override
            public void gotCompoundIdent(LocatableToken token) {
                SourceParser.this.gotCompoundIdent(token);
            }
            
            @Override
            public void gotCompoundComponent(LocatableToken token) {
                SourceParser.this.gotCompoundComponent(token);
            }
            
            @Override
            public void completeCompoundValue(LocatableToken token) {
                SourceParser.this.completeCompoundValue(token);
            }
            
            @Override
            public void completeCompoundValueEOF(LocatableToken token) {
                SourceParser.this.completeCompoundValueEOF(token);
            }
            
            @Override
            public void completeCompoundClass(LocatableToken token) {
                SourceParser.this.completeCompoundClass(token);
            }
            
            @Override
            public void gotArrayTypeIdentifier(LocatableToken token) {
                SourceParser.this.gotArrayTypeIdentifier(token);
            }
            
            @Override
            public void gotParentIdentifier(LocatableToken token) {
                SourceParser.this.gotParentIdentifier(token);
            }
            
            // Statement callbacks
            @Override
            public void gotThrow(LocatableToken token) {
                SourceParser.this.gotThrow(token);
            }
            
            @Override
            public void gotBreakContinue(LocatableToken keywordToken, LocatableToken labelToken) {
                SourceParser.this.gotBreakContinue(keywordToken, labelToken);
            }
            
            @Override
            public void gotReturnStatement(boolean hasValue) {
                SourceParser.this.gotReturnStatement(hasValue);
            }
            
            @Override
            public void gotYieldStatement() {
                SourceParser.this.gotYieldStatement();
            }
            
            @Override
            public void gotEmptyStatement() {
                SourceParser.this.gotEmptyStatement();
            }
            
            @Override
            public void gotAssert() {
                SourceParser.this.gotAssert();
            }
            
            @Override
            public void gotTopLevelDecl(LocatableToken token) {
                SourceParser.this.gotTopLevelDecl(token);
            }
            
            @Override
            public void gotInnerType(LocatableToken start) {
                SourceParser.this.gotInnerType(start);
            }
            
            // Lambda parameter callbacks
            @Override
            public void gotLambdaFormalParam() {
                SourceParser.this.gotLambdaFormalParam();
            }
            
            @Override
            public void gotLambdaFormalType(List<LocatableToken> type) {
                SourceParser.this.gotLambdaFormalType(type);
            }
            
            @Override
            public void beginFormalParameter(LocatableToken token) {
                SourceParser.this.beginFormalParameter(token);
            }
            
            // Record parameter callbacks
            @Override
            public void beginRecordParameters(LocatableToken parenToken) {
                SourceParser.this.beginRecordParameters(parenToken);
            }
            
            @Override
            public void gotRecordParameter(LocatableToken first, LocatableToken idToken, LocatableToken varargsToken) {
                SourceParser.this.gotRecordParameter(first, idToken, varargsToken);
            }
            
            @Override
            public void endRecordParameters(LocatableToken closeParen) {
                SourceParser.this.endRecordParameters(closeParen);
            }
        };
    }
}
