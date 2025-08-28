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
import bluej.parser.pratt.NodeFactory;
import bluej.parser.pratt.ParseResult;
import bluej.parser.pratt.Precedence;
import org.jetbrains.annotations.NotNull;

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
    public @NotNull ParseResult<ParsedNode> parse(KotlinPrattParser parser, @NotNull ParsedNode left, @NotNull LocatableToken token)
    {
        if (token.getType() != JavaTokenTypes.LPAREN) {
            return ParseResult.failure("Expected '(' for function call", token);
        }

        // Parse the argument list
        List<ParseResult<ParsedNode>> argumentResults = parseArgumentList(parser);

        // Use sequence to combine all argument results and accumulate any errors
        ParseResult<List<ParsedNode>> argumentsResult = ParseResult.sequence(argumentResults);
        if (argumentsResult.isFailure()) {
            return ParseResult.failure(argumentsResult.getErrors());
        }

        List<ParsedNode> argumentsList = argumentsResult.getValue();
        ParsedNode[] arguments = argumentsList.toArray(new ParsedNode[0]);

        // Create the call node using NodeFactory
        NodeFactory nodeFactory = parser.getNodeFactory();
        if (nodeFactory == null) {
            return ParseResult.failure("NodeFactory not available for AST node creation", token);
        }

        try {
            ParsedNode result = nodeFactory.createCallNode(left, arguments);
            if (result == null) {
                return ParseResult.failure("NodeFactory failed to create call node", token);
            }
            return ParseResult.success(result);
        } catch (Exception e) {
            return ParseResult.failure("Failed to create call node: " + e.getMessage(), token);
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
     * @return List of ParseResults for each argument expression
     */
    private List<ParseResult<ParsedNode>> parseArgumentList(KotlinPrattParser parser)
    {
        List<ParseResult<ParsedNode>> argumentResults = new ArrayList<>();

        // Check if we have an empty argument list
        LocatableToken nextToken = parser.peek();
        if (nextToken.getType() == JavaTokenTypes.RPAREN) {
            // Empty argument list - consume the closing paren and return empty list
            parser.consume();
            return new ArrayList<>();
        }

        // Parse arguments separated by commas
        while (true) {
            // Parse the next argument expression using ParseResult
            ParseResult<ParsedNode> argumentResult = parser.parseExpressionResult(0);
            argumentResults.add(argumentResult);

            // Check what comes next
            LocatableToken token = parser.peek();
            if (token.getType() == JavaTokenTypes.EOF) {
                argumentResults.add(ParseResult.failure("Unexpected end of input in argument list", null));
                break;
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
                if (afterComma.getType() == JavaTokenTypes.RPAREN) {
                    // Trailing comma - consume the closing paren and finish
                    parser.consume();
                    break;
                }
            } else {
                argumentResults.add(ParseResult.failure("Expected ',' or ')' in argument list, found: " + token.getText(), token));
                break;
            }
        }

        return argumentResults;
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
