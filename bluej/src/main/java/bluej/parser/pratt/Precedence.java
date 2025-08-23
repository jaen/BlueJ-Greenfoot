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
 * Enumeration of operator precedence levels for the Kotlin Pratt parser.
 *
 * <p>This enum defines the precedence hierarchy for Kotlin operators, with higher
 * numeric values indicating tighter binding (higher precedence). The precedence
 * levels are based on the official Kotlin language specification.</p>
 *
 * <p>The precedence values are designed to match Kotlin's operator precedence rules
 * as specified in the Kotlin Grammar Specification. Each level represents a group
 * of operators that have the same precedence and are evaluated together.</p>
 *
 * <h2>Precedence Order (from lowest to highest)</h2>
 * <ol>
 *   <li>Assignment operators (=, +=, -=, etc.)</li>
 *   <li>Elvis operator (?:)</li>
 *   <li>Disjunction (||)</li>
 *   <li>Conjunction (&&)</li>
 *   <li>Equality operators (==, !=, ===, !==)</li>
 *   <li>Comparison operators (<, >, <=, >=)</li>
 *   <li>Named checks (in, !in, is, !is)</li>
 *   <li>Range operator (..)</li>
 *   <li>Infix function calls</li>
 *   <li>Additive operators (+, -)</li>
 *   <li>Multiplicative operators (*, /, %)</li>
 *   <li>Type casts (as, as?)</li>
 *   <li>Prefix operators (-, +, ++, --, !, etc.)</li>
 *   <li>Postfix operators (++, --, !!, ., ?., [], ())</li>
 * </ol>
 *
 * @see InfixParselet#getPrecedence()
 * @see KotlinPrattParser#parseExpression(int)
 *
 * @author BlueJ Development Team
 * @since BlueJ 5.4.0
 */
public enum Precedence {

    /**
     * Lowest precedence - used as the base for expression parsing.
     * When parsing with LOWEST precedence, all operators will be included.
     */
    LOWEST(0),

    /**
     * Assignment operators: =, +=, -=, *=, /=, %=
     * Right-associative: a = b = c parses as a = (b = c)
     */
    ASSIGNMENT(10),

    /**
     * Elvis operator: ?:
     * Right-associative: a ?: b ?: c parses as a ?: (b ?: c)
     */
    ELVIS(20),

    /**
     * Logical disjunction: ||
     * Left-associative: a || b || c parses as (a || b) || c
     */
    DISJUNCTION(30),

    /**
     * Logical conjunction: &&
     * Left-associative: a && b && c parses as (a && b) && c
     */
    CONJUNCTION(40),

    /**
     * Equality operators: ==, !=, ===, !==
     * Left-associative: comparison chains are evaluated left to right
     */
    EQUALITY(50),

    /**
     * Comparison operators: <, >, <=, >=
     * Non-associative in Kotlin (cannot chain without parentheses)
     */
    COMPARISON(60),

    /**
     * Named checks and containment: in, !in, is, !is
     * Non-associative operators for type and range checking
     */
    NAMED_CHECKS(70),

    /**
     * Range operator: ..
     * Non-associative: used to create ranges
     */
    RANGE(80),

    /**
     * Infix function calls
     * Left-associative: custom infix functions defined with 'infix' modifier
     */
    INFIX_FUNCTION(90),

    /**
     * Additive operators: +, -
     * Left-associative: a + b - c parses as (a + b) - c
     */
    ADDITIVE(100),

    /**
     * Multiplicative operators: *, /, %
     * Left-associative: a * b / c parses as (a * b) / c
     */
    MULTIPLICATIVE(110),

    /**
     * Type cast operators: as, as?
     * Left-associative: used for type casting and safe casting
     */
    TYPE_CAST(120),

    /**
     * Prefix/unary operators: -, +, ++, --, !, labeled returns
     * Right-associative by nature (operate on following expression)
     */
    PREFIX(130),

    /**
     * Postfix operators and member access: ++, --, !!, ., ?., [], ()
     * Left-associative: highest precedence for member access and calls
     */
    POSTFIX(140),

    /**
     * Special precedence for atomic expressions that cannot be split.
     * This includes literals, identifiers, and parenthesized expressions.
     */
    ATOMIC(150);

    /** The numeric precedence value */
    private final int value;

    /**
     * Creates a precedence level with the specified numeric value.
     *
     * @param value The numeric precedence value
     */
    Precedence(int value) {
        this.value = value;
    }

    /**
     * Gets the numeric precedence value.
     *
     * @return The precedence value
     */
    public int getValue() {
        return value;
    }

    /**
     * Checks if this precedence is higher than another.
     *
     * @param other The precedence to compare with
     * @return true if this precedence is higher (binds more tightly)
     */
    public boolean isHigherThan(Precedence other) {
        return this.value > other.value;
    }

    /**
     * Checks if this precedence is higher than or equal to another.
     *
     * @param other The precedence to compare with
     * @return true if this precedence is higher or equal
     */
    public boolean isHigherOrEqualTo(Precedence other) {
        return this.value >= other.value;
    }

    /**
     * Checks if this precedence is lower than another.
     *
     * @param other The precedence to compare with
     * @return true if this precedence is lower (binds less tightly)
     */
    public boolean isLowerThan(Precedence other) {
        return this.value < other.value;
    }

    /**
     * Checks if this precedence is lower than or equal to another.
     *
     * @param other The precedence to compare with
     * @return true if this precedence is lower or equal
     */
    public boolean isLowerOrEqualTo(Precedence other) {
        return this.value <= other.value;
    }

    /**
     * Checks if this precedence is higher than a numeric value.
     *
     * @param value The numeric value to compare with
     * @return true if this precedence is higher
     */
    public boolean isHigherThan(int value) {
        return this.value > value;
    }

    /**
     * Gets the precedence for right-associative operators.
     *
     * <p>For right-associative operators, we use precedence - 1 when parsing
     * the right operand to ensure proper associativity.</p>
     *
     * @return The precedence value minus 1
     */
    public int getRightAssociative() {
        return value - 1;
    }

    /**
     * Gets the precedence for left-associative operators.
     *
     * <p>For left-associative operators, we use the same precedence when
     * parsing the right operand.</p>
     *
     * @return The precedence value unchanged
     */
    public int getLeftAssociative() {
        return value;
    }

    /**
     * Finds the Precedence enum constant closest to the given numeric value.
     *
     * @param value The numeric precedence value
     * @return The closest matching Precedence constant
     */
    public static Precedence fromValue(int value) {
        Precedence closest = LOWEST;
        int minDiff = Math.abs(value - LOWEST.value);

        for (Precedence prec : values()) {
            int diff = Math.abs(value - prec.value);
            if (diff < minDiff) {
                minDiff = diff;
                closest = prec;
            }
        }
        return closest;
    }

    /**
     * Gets a Precedence that is exactly the given numeric value, or null if none exists.
     *
     * @param value The numeric precedence value
     * @return The matching Precedence or null
     */
    public static Precedence exactValue(int value) {
        for (Precedence prec : values()) {
            if (prec.value == value) {
                return prec;
            }
        }
        return null;
    }

    /**
     * Returns a string representation of this precedence level.
     *
     * @return A string in the format "NAME(value)"
     */
    @Override
    public String toString() {
        return name() + "(" + value + ")";
    }
}
