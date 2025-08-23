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
 * Parselet for member access expressions in Kotlin.
 *
 * Handles member access with the general form: OBJECT.MEMBER or OBJECT?.MEMBER
 * where OBJECT is any expression and MEMBER is an identifier representing a
 * property, method, or nested class.
 *
 * Examples of member access parsed by this parselet:
 * - Property access: obj.property, person.name
 * - Method access: obj.method (method calls combine with CallParselet)
 * - Chained access: obj.prop1.prop2.method()
 * - Safe access: obj?.nullableProperty, chain?.method()
 * - Nested class access: OuterClass.InnerClass
 *
 * Member access has very high precedence (POSTFIX level) and is left-associative,
 * meaning accesses chain from left to right and bind tightly to their operands.
 *
 * The safe call operator (?.) prevents NullPointerException by returning null
 * if the left-hand side is null, rather than attempting the member access.
 *
 * @author BlueJ Team
 */
public class MemberAccessParselet implements InfixParselet
{
    /**
     * The precedence level for member access.
     * Member access has POSTFIX precedence, which is very high (140),
     * ensuring it binds tightly and chains properly with function calls.
     */
    private static final int PRECEDENCE = Precedence.POSTFIX.getValue();

    /**
     * Parses a member access expression.
     *
     * The general form is: OBJECT.MEMBER or OBJECT?.MEMBER
     * where OBJECT is the left-hand expression that was already parsed,
     * and MEMBER is the identifier following the access operator.
     *
     * This method handles:
     * - Regular access: obj.property
     * - Safe access: obj?.property
     * - Chained access: obj.prop1.prop2
     * - Method access: obj.method (combined with CallParselet for obj.method())
     *
     * @param parser The parser instance for parsing the member name
     * @param left The object expression (left-hand side)
     * @param token The access operator token (. or ?.) that triggered this parselet
     * @return ParsedNode representing the member access, or null on error
     */
    @Override
    public ParsedNode parse(KotlinPrattParser parser, ParsedNode left, LocatableToken token)
    {
        if (left == null) {
            parser.error("Missing object expression for member access", token);
            return null;
        }

        if (token == null) {
            parser.error("Missing access operator", null);
            return null;
        }

        // Determine if this is a safe call
        boolean isSafeCall;
        if (token.getType() == JavaTokenTypes.DOT) {
            isSafeCall = false;
        } else if (token.getType() == JavaTokenTypes.SAFE_ACCESS) {
            isSafeCall = true;
        } else {
            parser.error("Expected '.' or '?.' for member access, found: " + token.getText(), token);
            return null;
        }

        // Parse the member name (should be an identifier)
        LocatableToken memberToken = parser.peek();
        if (memberToken == null) {
            parser.error("Expected member name after " + token.getText(), token);
            return null;
        }

        if (memberToken.getType() != JavaTokenTypes.IDENT) {
            parser.error("Expected identifier for member name, found: " + memberToken.getText(), memberToken);
            return null;
        }

        // Consume the member name token
        parser.consume();

        // Create the member access node using NodeFactory
        try {
            return parser.getNodeFactory().createMemberAccessNode(left, memberToken, isSafeCall);
        } catch (Exception e) {
            // If node creation fails, return null to indicate parse failure
            return null;
        }
    }

    /**
     * Returns the precedence level for member access.
     *
     * Member access has POSTFIX precedence (140), which ensures it binds
     * tightly and is evaluated before most other operators. This allows
     * expressions like: -obj.prop to parse as -(obj.prop) rather than (-obj).prop.
     *
     * Member access has the same precedence as function calls, allowing them
     * to chain naturally: obj.method().property.toString()
     *
     * @return The POSTFIX precedence level
     */
    @Override
    public int getPrecedence()
    {
        return PRECEDENCE;
    }
}
