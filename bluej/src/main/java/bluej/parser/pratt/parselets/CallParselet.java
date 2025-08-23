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

import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.pratt.InfixParselet;
import bluej.parser.pratt.KotlinPrattParser;
import bluej.parser.pratt.Precedence;

import java.util.ArrayList;
import java.util.List;

/**
 * Parselet for function call expressions in Kotlin.
 *
 * Handles function calls with the general form: FUNCTION(ARG1, ARG2, ..., ARGN)
 * where FUNCTION is any expression (identifier, member access, etc.) and the
 * arguments are comma-separated expressions within parentheses.
 *
 * Examples of function calls parsed by this parselet:
 * - Simple calls: foo(), bar(42)
 * - Method calls: obj.method(arg1, arg2)
 * - Chained calls: getData().process().toString()
 * - Constructor calls: MyClass(param1, param2)
 * - Higher-order calls: func()(otherArg)
 *
 * Function calls have very high precedence (POSTFIX level) and are left-associative,
 * meaning they bind tightly and chain from left to right.
 *
 * @author BlueJ Team
 */
public class CallParselet implements InfixParselet
{
    /**
     * The precedence level for function calls.
     * Function calls have POSTFIX precedence, which is very high (140),
     * ensuring they bind tightly to their operands.
     */
    private static final int PRECEDENCE = Precedence.POSTFIX.getValue();

    /**
     * Parses a function call expression.
     *
     * The general form is: FUNCTION(ARG1, ARG2, ..., ARGN)
     * where FUNCTION is the left-hand expression that was already parsed,
     * and the argument list follows within parentheses.
     *
     * This method handles:
     * - Empty argument lists: func()
     * - Single arguments: func(42)
     * - Multiple arguments: func(arg1, arg2, arg3)
     * - Nested expressions as arguments: func(a + b, method(), x.y)
     *
     * @param parser The parser instance for parsing arguments
     * @param left The function expression (left-hand side)
     * @param token The opening parenthesis token that triggered this parselet
     * @return ParsedNode representing the function call, or null on error
     */
    @Override
    public ParsedNode parse(KotlinPrattParser parser, ParsedNode left, LocatableToken token)
    {
        if (left == null) {
            parser.error("Missing function expression for call", token);
            return null;
        }

        if (token == null || token.getType() != JavaTokenTypes.LPAREN) {
            parser.error("Expected '(' for function call", token);
            return null;
        }

        // Parse the argument list
        ParsedNode[] arguments = parseArgumentList(parser);
        if (arguments == null) {
            // Error already reported by parseArgumentList
            return null;
        }

        // Create the call node using NodeFactory
        try {
            return parser.getNodeFactory().createCallNode(left, arguments);
        } catch (Exception e) {
            // If node creation fails, return null to indicate parse failure
            return null;
        }
    }

    /**
     * Parses a comma-separated argument list between parentheses.
     *
     * Expected token sequence:
     * - Current token should be LPAREN (already consumed by parser)
     * - Zero or more expressions separated by COMMA
     * - Terminated by RPAREN
     *
     * @param parser The parser instance
     * @return Array of argument expressions, or null if parsing failed
     */
    private ParsedNode[] parseArgumentList(KotlinPrattParser parser)
    {
        List<ParsedNode> arguments = new ArrayList<>();

        // Check if we have an empty argument list
        LocatableToken nextToken = parser.peek();
        if (nextToken != null && nextToken.getType() == JavaTokenTypes.RPAREN) {
            // Empty argument list - consume the closing paren and return empty array
            parser.consume();
            return new ParsedNode[0];
        }

        // Parse arguments separated by commas
        while (true) {
            // Parse the next argument expression
            ParsedNode argument = parser.parseExpression();
            if (argument == null) {
                parser.error("Expected expression in argument list", parser.peek());
                return null;
            }

            arguments.add(argument);

            // Check what comes next
            LocatableToken token = parser.peek();
            if (token == null) {
                parser.error("Unexpected end of input in argument list", null);
                return null;
            }

            if (token.getType() == JavaTokenTypes.RPAREN) {
                // End of argument list - consume the closing paren
                parser.consume();
                break;
            } else if (token.getType() == JavaTokenTypes.COMMA) {
                // More arguments - consume the comma and continue
                parser.consume();

                // Check for trailing comma before closing paren
                LocatableToken afterComma = parser.peek();
                if (afterComma != null && afterComma.getType() == JavaTokenTypes.RPAREN) {
                    // Trailing comma - consume the closing paren and finish
                    parser.consume();
                    break;
                }
            } else {
                parser.error("Expected ',' or ')' in argument list, found: " + token.getText(), token);
                return null;
            }
        }

        return arguments.toArray(new ParsedNode[0]);
    }

    /**
     * Returns the precedence level for function calls.
     *
     * Function calls have POSTFIX precedence (140), which ensures they bind
     * tightly and are evaluated before most other operators. This allows
     * expressions like: -func() to parse as -(func()) rather than (-func)().
     *
     * @return The POSTFIX precedence level
     */
    @Override
    public int getPrecedence()
    {
        return PRECEDENCE;
    }
}
