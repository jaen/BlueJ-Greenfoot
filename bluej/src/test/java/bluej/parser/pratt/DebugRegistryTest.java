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
package bluej.parser.pratt;

import bluej.parser.lexer.JavaTokenTypes;
import org.junit.Before;
import org.junit.Test;

/**
 * Debug test to examine actual registry contents and counts.
 */
public class DebugRegistryTest {

    private KotlinPrattParser parser;
    private ParseletRegistry registry;
    private TestNodeFactory nodeFactory;
    private TestTokenOperations tokenOps;

    @Before
    public void setUp() {
        tokenOps = new TestTokenOperations();
        nodeFactory = new TestNodeFactory();
        parser = new KotlinPrattParser(tokenOps, null, nodeFactory);
        registry = parser.getRegistry();
    }

    @Test
    public void debugRegistryCounts() {
        System.out.println("=== REGISTRY DEBUG INFO ===");
        System.out.println("Prefix count: " + registry.getPrefixCount());
        System.out.println("Infix count: " + registry.getInfixCount());

        System.out.println("\n=== PREFIX PARSELETS ===");
        var prefixTokens = registry.getPrefixTokenTypes();
        for (Integer tokenType : prefixTokens) {
            String parseletType = registry.getPrefix(tokenType).getClass().getSimpleName();
            System.out.println("Token " + tokenType + " (" + getTokenName(tokenType) + "): " + parseletType);
        }

        System.out.println("\n=== INFIX PARSELETS ===");
        var infixTokens = registry.getInfixTokenTypes();
        for (Integer tokenType : infixTokens) {
            String parseletType = registry.getInfix(tokenType).getClass().getSimpleName();
            int precedence = registry.getPrecedence(tokenType);
            System.out.println("Token " + tokenType + " (" + getTokenName(tokenType) + "): " + parseletType + " (precedence: " + precedence + ")");
        }
    }

    private String getTokenName(int tokenType) {
        return switch (tokenType) {
            case JavaTokenTypes.NUM_INT -> "NUM_INT";
            case JavaTokenTypes.NUM_LONG -> "NUM_LONG";
            case JavaTokenTypes.NUM_FLOAT -> "NUM_FLOAT";
            case JavaTokenTypes.NUM_DOUBLE -> "NUM_DOUBLE";
            case JavaTokenTypes.STRING_LITERAL -> "STRING_LITERAL";
            case JavaTokenTypes.STRING_LITERAL_MULTILINE -> "STRING_LITERAL_MULTILINE";
            case JavaTokenTypes.CHAR_LITERAL -> "CHAR_LITERAL";
            case JavaTokenTypes.LITERAL_true -> "LITERAL_true";
            case JavaTokenTypes.LITERAL_false -> "LITERAL_false";
            case JavaTokenTypes.LITERAL_null -> "LITERAL_null";
            case JavaTokenTypes.IDENT -> "IDENT";
            case JavaTokenTypes.LITERAL_this -> "LITERAL_this";
            case JavaTokenTypes.LITERAL_super -> "LITERAL_super";
            case JavaTokenTypes.LPAREN -> "LPAREN";
            case JavaTokenTypes.PLUS -> "PLUS";
            case JavaTokenTypes.MINUS -> "MINUS";
            case JavaTokenTypes.LNOT -> "LNOT";
            case JavaTokenTypes.INC -> "INC";
            case JavaTokenTypes.DEC -> "DEC";
            case JavaTokenTypes.STAR -> "STAR";
            case JavaTokenTypes.DIV -> "DIV";
            case JavaTokenTypes.MOD -> "MOD";
            case JavaTokenTypes.LT -> "LT";
            case JavaTokenTypes.GT -> "GT";
            case JavaTokenTypes.LE -> "LE";
            case JavaTokenTypes.GE -> "GE";
            case JavaTokenTypes.EQUAL -> "EQUAL";
            case JavaTokenTypes.NOT_EQUAL -> "NOT_EQUAL";
            case JavaTokenTypes.LAND -> "LAND";
            case JavaTokenTypes.LOR -> "LOR";
            case JavaTokenTypes.ASSIGN -> "ASSIGN";
            case JavaTokenTypes.PLUS_ASSIGN -> "PLUS_ASSIGN";
            case JavaTokenTypes.MINUS_ASSIGN -> "MINUS_ASSIGN";
            case JavaTokenTypes.STAR_ASSIGN -> "STAR_ASSIGN";
            case JavaTokenTypes.DIV_ASSIGN -> "DIV_ASSIGN";
            case JavaTokenTypes.DOT -> "DOT";
            case JavaTokenTypes.LBRACK -> "LBRACK";
            default -> "UNKNOWN_" + tokenType;
        };
    }
}
