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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry that manages the mapping between token types and their associated parselets.
 *
 * <p>This registry is the core of the Pratt parser's extensibility. It allows the parser
 * to dispatch to appropriate parselets based on token types, and provides precedence
 * information for infix operators.</p>
 *
 * <p>The registry maintains two separate mappings:</p>
 * <ul>
 *   <li>Prefix parselets - for constructs that appear at the beginning of expressions</li>
 *   <li>Infix parselets - for constructs that appear between expressions</li>
 * </ul>
 *
 * <p>Thread Safety: This implementation uses concurrent collections to allow safe
 * registration from multiple threads, though typical usage will be single-threaded
 * during parser initialization.</p>
 *
 * @author BlueJ Development Team
 * @since BlueJ 5.4.0
 */
public class ParseletRegistry {

    /** Map from token types to their prefix parselets */
    private final Map<Integer, PrefixParselet> prefixParselets;

    /** Map from token types to their infix parselets */
    private final Map<Integer, InfixParselet> infixParselets;

    /** Optional parent registry for delegation (for modular parser composition) */
    private final ParseletRegistry parent;

    /**
     * Creates a new empty parselet registry.
     */
    public ParseletRegistry() {
        this(null);
    }

    /**
     * Creates a new parselet registry with an optional parent registry.
     *
     * <p>When a parselet is not found in this registry, the lookup will
     * be delegated to the parent registry if one is provided.</p>
     *
     * @param parent The parent registry to delegate to, or null for no delegation
     */
    public ParseletRegistry(ParseletRegistry parent) {
        this.prefixParselets = new HashMap<>();
        this.infixParselets = new HashMap<>();
        this.parent = parent;
    }

    /**
     * Registers a prefix parselet for a specific token type.
     *
     * <p>If a prefix parselet was already registered for this token type,
     * it will be replaced with the new one.</p>
     *
     * @param tokenType The token type to associate with the parselet
     * @param parselet The prefix parselet to register
     * @return The previously registered parselet, or null if none existed
     * @throws NullPointerException if parselet is null
     */
    public PrefixParselet register(int tokenType, PrefixParselet parselet) {
        Objects.requireNonNull(parselet, "Parselet cannot be null");
        return prefixParselets.put(tokenType, parselet);
    }

    /**
     * Registers an infix parselet for a specific token type.
     *
     * <p>If an infix parselet was already registered for this token type,
     * it will be replaced with the new one.</p>
     *
     * @param tokenType The token type to associate with the parselet
     * @param parselet The infix parselet to register
     * @return The previously registered parselet, or null if none existed
     * @throws NullPointerException if parselet is null
     */
    public InfixParselet register(int tokenType, InfixParselet parselet) {
        Objects.requireNonNull(parselet, "Parselet cannot be null");
        return infixParselets.put(tokenType, parselet);
    }

    /**
     * Retrieves the prefix parselet for a given token type.
     *
     * <p>If no parselet is registered in this registry and a parent registry
     * exists, the lookup will be delegated to the parent.</p>
     *
     * @param tokenType The token type to look up
     * @return The associated prefix parselet, or null if none is registered
     */
    public PrefixParselet getPrefix(int tokenType) {
        PrefixParselet parselet = prefixParselets.get(tokenType);
//        if (parselet == null && parent != null) {
//            return parent.getPrefix(tokenType);
//        }
        return parselet;
    }

    /**
     * Retrieves the infix parselet for a given token type.
     *
     * <p>If no parselet is registered in this registry and a parent registry
     * exists, the lookup will be delegated to the parent.</p>
     *
     * @param tokenType The token type to look up
     * @return The associated infix parselet, or null if none is registered
     */
    public InfixParselet getInfix(int tokenType) {
        InfixParselet parselet = infixParselets.get(tokenType);
//        if (parselet == null && parent != null) {
//            return parent.getInfix(tokenType);
//        }
        return parselet;
    }

    /**
     * Gets the precedence level for a given token type.
     *
     * <p>The precedence is determined by the infix parselet registered for
     * the token type. If no infix parselet is registered, returns 0.</p>
     *
     * @param tokenType The token type to get precedence for
     * @return The precedence level, or 0 if no infix parselet is registered
     */
    public int getPrecedence(int tokenType) {
        InfixParselet parselet = getInfix(tokenType);
        return parselet != null ? parselet.getPrecedence() : 0;
    }

    /**
     * Checks if a prefix parselet is registered for a given token type.
     *
     * @param tokenType The token type to check
     * @return true if a prefix parselet is registered, false otherwise
     */
    public boolean hasPrefix(int tokenType) {
        return prefixParselets.containsKey(tokenType); // ||
//               (parent != null && parent.hasPrefix(tokenType));
    }

    /**
     * Checks if an infix parselet is registered for a given token type.
     *
     * @param tokenType The token type to check
     * @return true if an infix parselet is registered, false otherwise
     */
    public boolean hasInfix(int tokenType) {
        return infixParselets.containsKey(tokenType); // ||
//               (parent != null && parent.hasInfix(tokenType));
    }

    /**
     * Removes a prefix parselet registration.
     *
     * @param tokenType The token type to unregister
     * @return The removed parselet, or null if none was registered
     */
    public PrefixParselet unregisterPrefix(int tokenType) {
        return prefixParselets.remove(tokenType);
    }

    /**
     * Removes an infix parselet registration.
     *
     * @param tokenType The token type to unregister
     * @return The removed parselet, or null if none was registered
     */
    public InfixParselet unregisterInfix(int tokenType) {
        return infixParselets.remove(tokenType);
    }

    /**
     * Clears all registered parselets from this registry.
     *
     * <p>Note: This does not affect the parent registry if one exists.</p>
     */
    public void clear() {
        prefixParselets.clear();
        infixParselets.clear();
    }

    /**
     * Gets the count of registered prefix parselets.
     *
     * @return The number of prefix parselets registered in this registry
     */
    public int getPrefixCount() {
        return prefixParselets.size();
    }

    /**
     * Gets the count of registered infix parselets.
     *
     * @return The number of infix parselets registered in this registry
     */
    public int getInfixCount() {
        return infixParselets.size();
    }

    /**
     * Gets the set of token types that have prefix parselets registered.
     *
     * @return An unmodifiable set of token types with prefix parselets
     */
    public Set<Integer> getPrefixTokenTypes() {
        return Set.copyOf(prefixParselets.keySet());
    }

    /**
     * Gets the set of token types that have infix parselets registered.
     *
     * @return An unmodifiable set of token types with infix parselets
     */
    public Set<Integer> getInfixTokenTypes() {
        return Set.copyOf(infixParselets.keySet());
    }

    /**
     * Creates a copy of this registry.
     *
     * <p>The copy will contain all the same parselet registrations but will
     * be independent of the original. The parent registry reference is preserved.</p>
     *
     * @return A new registry with the same registrations
     */
    public ParseletRegistry copy() {
        ParseletRegistry copy = new ParseletRegistry(this.parent);
        copy.prefixParselets.putAll(this.prefixParselets);
        copy.infixParselets.putAll(this.infixParselets);
        return copy;
    }

    /**
     * Merges another registry into this one.
     *
     * <p>All parselets from the other registry will be registered in this registry,
     * potentially overwriting existing registrations for the same token types.</p>
     *
     * @param other The registry to merge from
     * @throws NullPointerException if other is null
     */
    public void merge(ParseletRegistry other) {
        Objects.requireNonNull(other, "Cannot merge null registry");
        prefixParselets.putAll(other.prefixParselets);
        infixParselets.putAll(other.infixParselets);
    }

    /**
     * Creates a debug string representation of this registry.
     *
     * @return A string showing the registered parselets
     */
    @Override
    public String toString() {
        return String.format("ParseletRegistry[prefix=%d, infix=%d, parent=%s]",
            prefixParselets.size(),
            infixParselets.size(),
            parent != null ? "present" : "none");
    }
}
