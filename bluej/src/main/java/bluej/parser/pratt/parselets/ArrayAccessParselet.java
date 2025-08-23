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

/**
 * Parselet for array and collection access expressions in Kotlin.
 *
 * Handles indexed access with the general form: ARRAY[INDEX]
 * where ARRAY is any expression representing an array or collection,
 * and INDEX is an expression representing the index or key.
 *
 * Examples of array access parsed by this parselet:
 * - Array indexing: arr[0], matrix[i][j]
 * - Collection access: list[index], map["key"]
 * - Multi-dimensional access: array[x][y][z]
 * - Complex expressions: getArray()[i + 1], obj.property[key]
 * - String indexing: str[position]
 *
 * Array access has very high precedence (POSTFIX level) and is left-associative,
 * meaning multiple accesses chain from left to right and bind tightly to their operands.
 *
 * In Kotlin, array access is syntactic sugar for the get() and set() operators,
 * so array[index] is equivalent to array.get(index).
 *
 * @author BlueJ Team
 */
public class ArrayAccessParselet implements InfixParselet
{
    /**
     * The precedence level for array access.
     * Array access has POSTFIX precedence, which is very high (140),
     * ensuring it binds tightly and chains properly with other postfix operations.
     */
    private static final int PRECEDENCE = Precedence.POSTFIX.getValue();

    /**
     * Parses an array access expression.
     *
     * The general form is: ARRAY[INDEX]
     * where ARRAY is the left-hand expression that was already parsed,
     * and INDEX is the expression between the square brackets.
     *
     * This method handles:
     * - Simple indexing: arr[0]
     * - Complex indices: arr[i + 1]
     * - Multi-dimensional access: matrix[row][col] (through chaining)
     * - Collection access: map["key"]
     *
     * @param parser The parser instance for parsing the index expression
     * @param left The array/collection expression (left-hand side)
     * @param token The opening bracket token that triggered this parselet
     * @return ParsedNode representing the array access, or null on error
     */
    @Override
    public ParsedNode parse(KotlinPrattParser parser, ParsedNode left, LocatableToken token)
    {
        if (left == null) {
            parser.error("Missing array expression for indexing", token);
            return null;
        }

        if (token == null || token.getType() != JavaTokenTypes.LBRACK) {
            parser.error("Expected '[' for array access", token);
            return null;
        }

        // Parse the index expression
        ParsedNode index = parser.parseExpression();
        if (index == null) {
            parser.error("Expected index expression in array access", parser.peek());
            return null;
        }

        // Expect closing bracket
        LocatableToken closingBracket = parser.peek();
        if (closingBracket == null) {
            parser.error("Unexpected end of input in array access", null);
            return null;
        }

        if (closingBracket.getType() != JavaTokenTypes.RBRACK) {
            parser.error("Expected ']' after index expression, found: " + closingBracket.getText(), closingBracket);
            return null;
        }

        // Consume the closing bracket
        parser.consume();

        // Create the array access node using NodeFactory
        try {
            return parser.getNodeFactory().createArrayAccessNode(left, index);
        } catch (Exception e) {
            // If node creation fails, return null to indicate parse failure
            return null;
        }
    }

    /**
     * Returns the precedence level for array access.
     *
     * Array access has POSTFIX precedence (140), which ensures it binds
     * tightly and is evaluated before most other operators. This allows
     * expressions like: -arr[0] to parse as -(arr[0]) rather than (-arr)[0].
     *
     * Array access has the same precedence as member access and function calls,
     * allowing them to chain naturally: obj.getArray()[index].toString()
     *
     * @return The POSTFIX precedence level
     */
    @Override
    public int getPrecedence()
    {
        return PRECEDENCE;
    }
}
