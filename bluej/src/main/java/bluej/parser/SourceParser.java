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
        };
    }
}
