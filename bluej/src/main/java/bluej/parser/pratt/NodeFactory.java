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

import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.ParsedNode;

/**
 * Factory interface for creating AST nodes in a thread-safe manner.
 *
 * <p>This interface provides an abstraction layer for AST node creation, allowing
 * parselets to remain thread-agnostic while the actual node creation can happen
 * on the appropriate thread (e.g., FXPlatform thread for BlueJ's UI integration).</p>
 *
 * <p>The factory pattern follows the same design principles as {@link TokenOperations},
 * providing a clean separation between the parsing logic and the threading requirements
 * of the AST node creation.</p>
 *
 * <p>Implementations of this interface handle the actual instantiation of AST nodes
 * with proper threading constraints:</p>
 * <ul>
 *   <li>{@code ThreadSafeNodeFactory} - Creates nodes on FXPlatform thread</li>
 *   <li>{@code TestNodeFactory} - Creates lightweight nodes for testing</li>
 *   <li>{@code DirectNodeFactory} - Creates nodes directly (for non-UI contexts)</li>
 * </ul>
 *
 * @author BlueJ Team
 * @since BlueJ 5.4.0
 * @see KotlinPrattParser
 * @see TokenOperations
 */
public interface NodeFactory {

    /**
     * Creates a literal expression node.
     *
     * <p>This method creates an AST node representing a literal value such as
     * numbers, strings, characters, booleans, or null.</p>
     *
     * @param token The literal token containing the value
     * @return A ParsedNode representing the literal, or null if creation failed
     */
    ParsedNode createLiteralNode(LocatableToken token);

    /**
     * Creates a binary operator expression node.
     *
     * <p>This method creates an AST node representing a binary operation
     * with left and right operands and an operator token.</p>
     *
     * @param left The left operand expression
     * @param operator The operator token
     * @param right The right operand expression
     * @return A ParsedNode representing the binary operation, or null if creation failed
     */
    ParsedNode createBinaryOperatorNode(ParsedNode left, LocatableToken operator, ParsedNode right);

    /**
     * Creates a unary prefix operator expression node.
     *
     * <p>This method creates an AST node representing a unary prefix operation
     * such as negation (-), logical not (!), or prefix increment/decrement.</p>
     *
     * @param operator The prefix operator token
     * @param operand The operand expression
     * @return A ParsedNode representing the unary operation, or null if creation failed
     */
    ParsedNode createUnaryPrefixNode(LocatableToken operator, ParsedNode operand);

    /**
     * Creates a unary postfix operator expression node.
     *
     * <p>This method creates an AST node representing a unary postfix operation
     * such as postfix increment/decrement or null assertion (!!).</p>
     *
     * @param operand The operand expression
     * @param operator The postfix operator token
     * @return A ParsedNode representing the unary operation, or null if creation failed
     */
    ParsedNode createUnaryPostfixNode(ParsedNode operand, LocatableToken operator);

    /**
     * Creates a grouped expression node.
     *
     * <p>This method handles parenthesized expressions. In many cases, the
     * parentheses are purely syntactic and don't create a separate AST node,
     * so this method may simply return the inner expression.</p>
     *
     * @param innerExpression The expression within the parentheses
     * @return The inner expression or a wrapper node if needed
     */
    ParsedNode createGroupNode(ParsedNode innerExpression);

    /**
     * Creates an identifier reference node.
     *
     * <p>This method creates an AST node representing a reference to a variable,
     * function, class, or other named entity.</p>
     *
     * @param identifier The identifier token
     * @return A ParsedNode representing the identifier reference, or null if creation failed
     */
    ParsedNode createIdentifierNode(LocatableToken identifier);

    /**
     * Creates a member access expression node.
     *
     * <p>This method creates an AST node representing property or method access
     * using dot notation (e.g., object.property or object.method()).</p>
     *
     * @param object The object expression being accessed
     * @param memberName The member name token
     * @param isSafeCall true if this is a safe call (?.), false for regular access (.)
     * @return A ParsedNode representing the member access, or null if creation failed
     */
    ParsedNode createMemberAccessNode(ParsedNode object, LocatableToken memberName, boolean isSafeCall);

    /**
     * Creates a function call expression node.
     *
     * <p>This method creates an AST node representing a function or method call
     * with the given arguments.</p>
     *
     * @param function The function expression (identifier or member access)
     * @param arguments Array of argument expressions (may be empty)
     * @return A ParsedNode representing the function call, or null if creation failed
     */
    ParsedNode createCallNode(ParsedNode function, ParsedNode[] arguments);

    /**
     * Creates an array/collection access expression node.
     *
     * <p>This method creates an AST node representing indexed access to an array
     * or collection using square bracket notation (e.g., array[index]).</p>
     *
     * @param array The array/collection expression
     * @param index The index expression
     * @return A ParsedNode representing the array access, or null if creation failed
     */
    ParsedNode createArrayAccessNode(ParsedNode array, ParsedNode index);

    /**
     * Creates a 'this' reference node.
     *
     * <p>This method creates an AST node representing a reference to the current
     * object instance.</p>
     *
     * @param thisToken The 'this' keyword token
     * @return A ParsedNode representing the this reference, or null if creation failed
     */
    ParsedNode createThisNode(LocatableToken thisToken);

    /**
     * Creates a 'super' reference node.
     *
     * <p>This method creates an AST node representing a reference to the parent
     * class instance.</p>
     *
     * @param superToken The 'super' keyword token
     * @return A ParsedNode representing the super reference, or null if creation failed
     */
    ParsedNode createSuperNode(LocatableToken superToken);

    /**
     * Reports an error during node creation.
     *
     * <p>This method allows the factory to report errors that occur during
     * node creation, such as invalid token types or structural issues.</p>
     *
     * @param message The error message
     * @param token The token where the error occurred (may be null)
     */
    void reportError(String message, LocatableToken token);

    /**
     * Checks if the factory has encountered any errors.
     *
     * @return true if errors have been reported, false otherwise
     */
    boolean hasErrors();
}
