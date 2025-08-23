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

import bluej.parser.nodes.ParsedNode;
import bluej.parser.pratt.KotlinPrattParser.ParseError;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Immutable record representing the outcome of a parsing operation.
 *
 * <p>This record encapsulates both the successful parsing result (if any) and
 * any errors encountered during parsing. It provides a clean, immutable interface
 * for handling parsing outcomes throughout the Pratt parser system.</p>
 *
 * <p>During the foundation phase, successful parsing operations return null for
 * the result node but may still be considered successful if no errors occurred.
 * This allows validation of parsing logic without full AST construction.</p>
 *
 * <p>Usage examples:</p>
 * <pre>{@code
 * // Successful parsing with no errors
 * ParseResult success = ParseResult.success(null); // Foundation phase
 * assert success.isSuccess();
 * assert success.errors().isEmpty();
 *
 * // Successful parsing with result node (future phases)
 * ParseResult withResult = ParseResult.success(someNode);
 * assert withResult.result() == someNode;
 *
 * // Failed parsing with errors
 * List<ParseError> errors = List.of(new ParseError("Syntax error", token));
 * ParseResult failure = ParseResult.failure(errors);
 * assert failure.isFailure();
 * assert !failure.errors().isEmpty();
 *
 * // Parsing with partial result and non-fatal errors
 * ParseResult partial = ParseResult.of(partialNode, warnings);
 * }</pre>
 *
 * @param result The parsed result node, or null during foundation phase or on failure
 * @param errors An immutable list of errors encountered during parsing (never null)
 *
 * @author BlueJ Development Team
 * @since BlueJ 5.4.0
 */
public record ParseResult(ParsedNode result, List<ParseError> errors) {

    /**
     * Canonical constructor with validation.
     *
     * @param result The parsed result node, may be null
     * @param errors The list of parse errors, must not be null
     * @throws NullPointerException if errors is null
     */
    public ParseResult {
        errors = List.copyOf(Objects.requireNonNull(errors, "Errors list cannot be null"));
    }

    /**
     * Creates a successful parse result with no errors.
     *
     * @param result The successfully parsed node, or null during foundation phase
     * @return A ParseResult indicating successful parsing
     */
    public static ParseResult success(ParsedNode result) {
        return new ParseResult(result, List.of());
    }

    /**
     * Creates a successful parse result with no errors and no result node.
     * This is commonly used during the foundation phase for validation.
     *
     * @return A ParseResult indicating successful validation with no result
     */
    public static ParseResult success() {
        return success(null);
    }

    /**
     * Creates a failed parse result with the specified errors.
     *
     * @param errors The list of parse errors that occurred
     * @return A ParseResult indicating failed parsing
     * @throws NullPointerException if errors is null
     */
    public static ParseResult failure(List<ParseError> errors) {
        return new ParseResult(null, errors);
    }

    /**
     * Creates a failed parse result with a single error.
     *
     * @param error The parse error that occurred
     * @return A ParseResult indicating failed parsing
     * @throws NullPointerException if error is null
     */
    public static ParseResult failure(ParseError error) {
        Objects.requireNonNull(error, "Error cannot be null");
        return failure(List.of(error));
    }

    /**
     * Creates a parse result with both a result and errors.
     * This allows for partial success scenarios where parsing continues
     * after recoverable errors.
     *
     * @param result The parsed result, may be null
     * @param errors The list of errors encountered
     * @return A ParseResult containing both result and errors
     * @throws NullPointerException if errors is null
     */
    public static ParseResult of(ParsedNode result, List<ParseError> errors) {
        return new ParseResult(result, errors);
    }

    /**
     * Checks if the parsing was successful (no errors occurred).
     *
     * <p>Note: During foundation phase, a successful parse may have a null result
     * but still be considered successful if no errors were encountered.</p>
     *
     * @return true if no errors occurred during parsing
     */
    public boolean isSuccess() {
        return errors.isEmpty();
    }

    /**
     * Checks if the parsing failed (errors occurred).
     *
     * @return true if one or more errors occurred during parsing
     */
    public boolean isFailure() {
        return !errors.isEmpty();
    }

    /**
     * Checks if a result node is available.
     *
     * <p>During foundation phase, this will typically return false even for
     * successful parsing, as nodes are not constructed until later phases.</p>
     *
     * @return true if a result node is available
     */
    public boolean hasResult() {
        return result != null;
    }

    /**
     * Gets the number of errors encountered during parsing.
     *
     * @return The count of parse errors
     */
    public int getErrorCount() {
        return errors.size();
    }

    /**
     * Gets the first error encountered during parsing, if any.
     *
     * @return The first parse error, or null if no errors occurred
     */
    public ParseError getFirstError() {
        return errors.isEmpty() ? null : errors.get(0);
    }

    /**
     * Gets all error messages as a formatted string.
     *
     * @return A string containing all error messages, one per line
     */
    public String getFormattedErrors() {
        if (errors.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < errors.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(errors.get(i).getFormattedMessage());
        }
        return sb.toString();
    }

    /**
     * Combines this parse result with another, merging errors and taking the
     * result from the other if this result is null.
     *
     * <p>This is useful for chaining parsing operations where later results
     * should incorporate errors from earlier stages.</p>
     *
     * @param other The other parse result to combine with
     * @return A new ParseResult combining both results
     * @throws NullPointerException if other is null
     */
    public ParseResult combine(ParseResult other) {
        Objects.requireNonNull(other, "Other ParseResult cannot be null");

        // Combine errors from both results
        List<ParseError> combinedErrors;
        if (errors.isEmpty() && other.errors.isEmpty()) {
            combinedErrors = List.of();
        } else if (errors.isEmpty()) {
            combinedErrors = other.errors;
        } else if (other.errors.isEmpty()) {
            combinedErrors = errors;
        } else {
            combinedErrors = List.copyOf(
                Stream.concat(errors.stream(), other.errors.stream()).toList()
            );
        }

        // Use the result from other if this result is null, otherwise keep this result
        ParsedNode combinedResult = result != null ? result : other.result;

        return new ParseResult(combinedResult, combinedErrors);
    }

    /**
     * Returns a string representation of this parse result.
     *
     * @return A string describing the parse result and any errors
     */
    @Override
    public String toString() {
        if (isSuccess()) {
            return hasResult()
                ? String.format("ParseResult[success with result: %s]", result)
                : "ParseResult[success, no result]";
        } else {
            return String.format("ParseResult[failure with %d error(s): %s]",
                errors.size(),
                errors.isEmpty() ? "" : getFirstError().message());
        }
    }
}
