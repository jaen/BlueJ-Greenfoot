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

/**
 * Test utilities for the Pratt parser implementation.
 *
 * <p>This package provides reusable test infrastructure for testing parselets
 * and parser functionality. It contains mock implementations and utility
 * classes that eliminate code duplication across test files.</p>
 *
 * <h2>Key Components</h2>
 *
 * <h3>{@link bluej.parser.pratt.testutil.MockParser}</h3>
 * <p>A controlled implementation of {@link bluej.parser.pratt.KotlinPrattParser}
 * that provides features for:</p>
 * <ul>
 *   <li>Token sequence management</li>
 *   <li>Error tracking and reporting</li>
 *   <li>Parse result simulation</li>
 *   <li>State reset between tests</li>
 * </ul>
 *
 * <h3>{@link bluej.parser.pratt.testutil.MockTokenOperations}</h3>
 * <p>A mock implementation of {@link bluej.parser.pratt.TokenOperations}
 * that provides:</p>
 * <ul>
 *   <li>Programmatic token stream control</li>
 *   <li>EOF token generation</li>
 *   <li>Token position tracking</li>
 *   <li>Proper {@code @NotNull} contract compliance</li>
 * </ul>
 *
 * <h3>{@link bluej.parser.pratt.testutil.TestUtils}</h3>
 * <p>Static utility methods for:</p>
 * <ul>
 *   <li>Creating tokens with various types and positions</li>
 *   <li>Creating AST nodes (identifiers, literals, etc.)</li>
 *   <li>Generating common token sequences</li>
 *   <li>Token type name resolution</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create a mock parser for testing
 * MockParser parser = new MockParser();
 *
 * // Set up a token sequence
 * parser.setTokenSequence(
 *     TestUtils.createToken(JavaTokenTypes.IDENT, "foo"),
 *     TestUtils.createToken(JavaTokenTypes.PLUS, "+"),
 *     TestUtils.createToken(JavaTokenTypes.NUM_INT, "42")
 * );
 *
 * // Test a parselet
 * BinaryOperatorParselet parselet = new BinaryOperatorParselet(Precedence.ADDITIVE);
 * ParsedNode left = TestUtils.createIdentifierNode("foo");
 * LocatableToken operator = TestUtils.createToken(JavaTokenTypes.PLUS, "+");
 *
 * ParseResult<ParsedNode> result = parselet.parse(parser, left, operator);
 *
 * // Verify results
 * assertFalse(parser.hasErrors());
 * assertNotNull(result.getValue());
 * }</pre>
 *
 * <h2>Migration Guide</h2>
 * <p>When refactoring existing tests to use these utilities:</p>
 * <ol>
 *   <li>Replace inline {@code TestKotlinPrattParser} classes with {@code MockParser}</li>
 *   <li>Replace inline {@code TestTokenOperations} classes with {@code MockTokenOperations}</li>
 *   <li>Replace token/node creation methods with {@code TestUtils} factory methods</li>
 *   <li>Remove duplicate helper methods that are now in {@code TestUtils}</li>
 * </ol>
 *
 * @since BlueJ 5.4.0
 * @author BlueJ Team
 */
package bluej.parser.pratt.testutil;
