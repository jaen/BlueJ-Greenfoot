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
import bluej.parser.pratt.InfixParselet;
import bluej.parser.pratt.KotlinPrattParser;

/**
 * Parselet for postfix unary operators in Kotlin.
 *
 * Handles operators such as:
 * - Increment/Decrement: ++, -- (postfix forms)
 * - Null assertion: !! (Kotlin-specific operator)
 *
 * Postfix operators have very high precedence and are left-associative.
 * They bind more tightly than most other operators, including prefix operators.
 *
 * The general form is: OPERAND OPERATOR
 * where OPERAND is the expression that the operator applies to, and OPERATOR
 * is the postfix operator token.
 *
 * @author BlueJ Team
 */
public class PostfixOperatorParselet implements InfixParselet
{
    private final int precedence;

    /**
     * Creates a postfix operator parselet with the specified precedence.
     * Postfix operators typically have very high precedence (15-16) to ensure
     * they bind tightly to their operands.
     *
     * @param precedence The precedence level for this postfix operator
     */
    public PostfixOperatorParselet(int precedence)
    {
        this.precedence = precedence;
    }

    /**
     * Parses a postfix unary expression.
     *
     * The general form is: OPERAND OPERATOR
     * where OPERAND is the left-hand expression that was already parsed,
     * and OPERATOR is the postfix operator token that triggered this parselet.
     *
     * @param parser The parser instance
     * @param left The left-hand operand that was already parsed
     * @param token The postfix operator token that triggered this parselet
     * @return ParsedNode representing the postfix operator expression, or null on error
     */
    @Override
    public ParsedNode parse(KotlinPrattParser parser, ParsedNode left, LocatableToken token)
    {
        if (left == null) {
            // This should not happen in normal parsing, but handle gracefully
            parser.error("Missing left operand for postfix operator: " + token.getText(), token);
            return null;
        }

        if (token == null) {
            // This should not happen either, but be defensive
            parser.error("Null token in postfix operator parselet", null);
            return null;
        }

        // Use the NodeFactory to create the postfix operator node
        // This maintains thread safety and follows the established pattern
        try {
            return parser.getNodeFactory().createUnaryPostfixNode(left, token);
        } catch (Exception e) {
            // If node creation fails, return null to indicate parse failure
            // The NodeFactory should handle thread safety concerns
            return null;
        }
    }

    /**
     * Returns the precedence level for this postfix operator.
     * This determines how tightly the operator binds to its operand
     * relative to other operators.
     *
     * Postfix operators typically have higher precedence than prefix operators
     * and most binary operators to ensure correct parsing of expressions like:
     * -x++ which should parse as -(x++) not (-x)++
     *
     * @return The precedence level
     */
    @Override
    public int getPrecedence()
    {
        return precedence;
    }
}
