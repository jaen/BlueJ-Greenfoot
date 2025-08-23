/*
 This file is part of the BlueJ program.
 Copyright (C) 1999-2009,2010,2011,2012,2014,2016,2017,2018,2019,2021,2022,2023,2024  Michael Kolling and John Rosenberg

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
package bluej.parser.pratt.parselets;

import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.pratt.NodeFactory;
import bluej.parser.pratt.KotlinPrattParser;
import bluej.parser.pratt.PrefixParselet;

/**
 * Parselet for prefix unary operators in Kotlin.
 *
 * Handles operators such as:
 * - Arithmetic: +, - (unary plus/minus)
 * - Logical: ! (logical NOT)
 * - Bitwise: ~ (bitwise NOT, though not in Kotlin, kept for completeness)
 * - Increment/Decrement: ++, -- (prefix forms)
 *
 * The precedence of prefix operators is generally higher than most binary operators
 * but the exact precedence depends on the specific operator type.
 *
 * @author BlueJ Team
 */
public class PrefixOperatorParselet implements PrefixParselet
{
    private final int precedence;

    /**
     * Creates a prefix operator parselet with the specified precedence.
     * Different prefix operators may have different precedence levels,
     * so this allows for flexibility in operator handling.
     *
     * @param precedence The precedence level for this prefix operator
     */
    public PrefixOperatorParselet(int precedence)
    {
        this.precedence = precedence;
    }

    /**
     * Parses a prefix unary expression.
     *
     * The general form is: OPERATOR OPERAND
     * where OPERATOR is the prefix operator token and OPERAND is the expression
     * that the operator applies to.
     *
     * @param parser The parser instance
     * @param token The operator token that triggered this parselet
     * @return ParsedNode representing the unary operator expression, or null on error
     */
    @Override
    public ParsedNode parse(KotlinPrattParser parser, LocatableToken token)
    {
        // Validate token is not null
        if (!ParseletErrorHandler.validateToken(parser, token, "prefix operator parselet")) {
            return null;
        }

        // Validate that this is actually a prefix operator token
        int[] validPrefixOperators = {
            JavaTokenTypes.PLUS, JavaTokenTypes.MINUS, JavaTokenTypes.LNOT,
            JavaTokenTypes.INC, JavaTokenTypes.DEC
        };
        if (!ParseletErrorHandler.validateTokenTypes(parser, token, validPrefixOperators, "prefix operator")) {
            return null;
        }

        // Get and validate NodeFactory
        NodeFactory nodeFactory = ParseletErrorHandler.getNodeFactory(parser, token);
        if (nodeFactory == null) {
            return null;
        }

        // Parse the operand with the precedence of this prefix operator
        // This ensures that the operand is parsed correctly with respect to
        // other operators that might follow
        ParsedNode operand = parser.parseExpression(precedence);

        // Validate that we successfully parsed the operand
        String operatorDescription = "prefix " + getOperatorDescription(token);
        if (!ParseletErrorHandler.validatePrefixOperand(parser, operand, token, operatorDescription)) {
            return null;
        }

        // Use the NodeFactory to create the unary operator node safely
        return ParseletErrorHandler.safeCreateNode(
            nodeFactory,
            () -> nodeFactory.createUnaryPrefixNode(token, operand),
            parser,
            token,
            "prefix operator node"
        );
    }

    /**
     * Provides a human-readable description of the operator for error messages.
     *
     * @param token The operator token
     * @return A descriptive string for the operator
     */
    private String getOperatorDescription(LocatableToken token) {
        switch (token.getType()) {
            case JavaTokenTypes.PLUS: return "plus (+)";
            case JavaTokenTypes.MINUS: return "minus (-)";
            case JavaTokenTypes.LNOT: return "logical not (!)";
            case JavaTokenTypes.INC: return "increment (++)";
            case JavaTokenTypes.DEC: return "decrement (--)";
            default: return "operator";
        }
    }

    /**
     * Returns the precedence level for this prefix operator.
     * This is used by the parser to determine how tightly the operator
     * binds to its operand relative to other operators.
     *
     * @return The precedence level
     */
    public int getPrecedence()
    {
        return precedence;
    }
}
