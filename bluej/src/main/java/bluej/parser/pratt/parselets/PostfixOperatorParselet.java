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
import bluej.parser.pratt.ParseResult;
import org.jetbrains.annotations.NotNull;

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
    public @NotNull ParseResult<ParsedNode> parse(KotlinPrattParser parser, @NotNull ParsedNode left, @NotNull LocatableToken token)
    {
//        if (left == null) {
//            return ParseResult.failure(
//                "Missing left operand for postfix operator" +
//                (token != null ? ": " + token.getText() : ""), token);
//        }
//
//        if (token == null) {
//            return ParseResult.failure("Null token in postfix operator parselet", null);
//        }

        // Use the NodeFactory to create the postfix operator node
        try {
            ParsedNode node = parser.getNodeFactory().createUnaryPostfixNode(left, token);
            if (node == null) {
                return ParseResult.failure("NodeFactory failed to create postfix operator node", token);
            }
            return ParseResult.success(node);
        } catch (Exception e) {
            // If node creation fails, return failure with appropriate error
            return ParseResult.failure(
                "Failed to create postfix operator node: " + e.getMessage(), token);
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
