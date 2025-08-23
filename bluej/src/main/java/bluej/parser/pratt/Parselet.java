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

/**
 * Base sealed interface for all parselets in the Pratt parser system.
 *
 * <p>This interface defines the foundation of the parselet hierarchy using Java 21's
 * sealed classes feature to ensure type safety and controlled extensibility. All
 * parselets must be either a {@link PrefixParselet} or an {@link InfixParselet}.</p>
 *
 * <p>The sealed hierarchy ensures that the parser can exhaustively handle all
 * parselet types, enabling better compile-time checking and preventing unauthorized
 * extensions of the parselet system.</p>
 *
 * <h2>Design Rationale</h2>
 * <p>The use of sealed interfaces provides several benefits:</p>
 * <ul>
 *   <li>Type safety - The compiler knows all possible parselet types</li>
 *   <li>Pattern matching - Java 21 pattern matching can be exhaustive</li>
 *   <li>Clear API boundaries - Only intended parselet types can exist</li>
 *   <li>Better documentation - The type hierarchy is explicit</li>
 * </ul>
 *
 * <h2>Extension Model</h2>
 * <p>While the base interface is sealed, concrete implementations of
 * {@link PrefixParselet} and {@link InfixParselet} are not sealed, allowing
 * for unlimited specific parselet implementations for different language
 * constructs.</p>
 *
 * @see PrefixParselet
 * @see InfixParselet
 * @see ParseletRegistry
 *
 * @author BlueJ Development Team
 * @since BlueJ 5.4.0
 */
public sealed interface Parselet permits PrefixParselet, InfixParselet {

    /**
     * Gets a human-readable name for this parselet.
     *
     * <p>This is primarily used for debugging and error reporting. Implementations
     * should return a descriptive name that identifies the language construct
     * this parselet handles.</p>
     *
     * @return The name of this parselet
     */
    default String getName() {
        return getClass().getSimpleName();
    }

    /**
     * Indicates whether this parselet supports error recovery.
     *
     * <p>Parselets that support error recovery can attempt to continue parsing
     * even after encountering syntax errors, producing partial AST nodes that
     * can be used for IDE features like syntax highlighting and code completion.</p>
     *
     * @return true if this parselet supports error recovery, false otherwise
     */
    default boolean supportsErrorRecovery() {
        return false;
    }

    /**
     * Gets the priority of this parselet when multiple parselets could handle
     * the same token type.
     *
     * <p>Higher priority parselets are preferred when multiple parselets are
     * registered for the same token. This is useful for context-sensitive
     * parsing where the same token might have different meanings in different
     * contexts.</p>
     *
     * @return The priority level (higher values = higher priority)
     */
    default int getPriority() {
        return 0;
    }
}
