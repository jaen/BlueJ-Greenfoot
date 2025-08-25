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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Simplified result type for parsing operations with error accumulation.
 *
 * <p>This sealed interface represents the outcome of a parsing operation,
 * providing either a successful result or accumulated errors. The design
 * supports monadic operations for composing parsing results while collecting
 * all errors encountered during the parsing process.</p>
 *
 * <p>The interface has two implementations:</p>
 * <ul>
 *   <li><b>Success:</b> Represents successful parsing with a result value</li>
 *   <li><b>Failure:</b> Represents failed parsing with accumulated errors</li>
 * </ul>
 *
 * @param <T> The type of the successful parse result
 * @author BlueJ Development Team
 * @since BlueJ 5.4.0
 */
public sealed interface ParseResult<T> {

    /**
     * Successful parse result with a value.
     *
     * @param <T> The type of the result value
     * @param value The parsed value
     */
    record Success<T>(T value) implements ParseResult<T> {}

    /**
     * Failed parse result with accumulated errors.
     *
     * @param <T> The expected type of the result
     * @param errors The list of accumulated errors
     */
    record Failure<T>(List<ParseError> errors) implements ParseResult<T> {}

    /**
     * Parse error information with location.
     *
     * @param message The error message
     * @param token The token where the error occurred (may be null)
     */
    record ParseError(String message, LocatableToken token) {
        /**
         * Gets the line number where the error occurred.
         *
         * @return The line number, or -1 if no token is available
         */
        public int getLine() {
            return token != null ? token.getLine() : -1;
        }

        /**
         * Gets the column number where the error occurred.
         *
         * @return The column number, or -1 if no token is available
         */
        public int getColumn() {
            return token != null ? token.getColumn() : -1;
        }

        /**
         * Gets a formatted error message with location information.
         *
         * @return Formatted error message
         */
        public String getFormattedMessage() {
            if (token != null) {
                return String.format("Error at line %d, column %d: %s",
                    getLine(), getColumn(), message);
            } else {
                return "Error: " + message;
            }
        }
    }

    // ==================== Factory Methods ====================

    /**
     * Creates a successful parse result.
     *
     * @param <T> The type of the result
     * @param value The parsed value
     * @return A successful ParseResult
     */
    static <T> ParseResult<T> success(T value) {
        return new Success<>(value);
    }

    /**
     * Creates a failed parse result with a single error.
     *
     * @param <T> The expected type of the result
     * @param message The error message
     * @param token The token where the error occurred
     * @return A failed ParseResult
     */
    static <T> ParseResult<T> failure(String message, LocatableToken token) {
        return new Failure<>(List.of(new ParseError(message, token)));
    }

    /**
     * Creates a failed parse result with a single error.
     *
     * @param <T> The expected type of the result
     * @param error The parse error
     * @return A failed ParseResult
     */
    static <T> ParseResult<T> failure(ParseError error) {
        return new Failure<>(List.of(Objects.requireNonNull(error)));
    }

    /**
     * Creates a failed parse result with multiple errors.
     *
     * @param <T> The expected type of the result
     * @param errors The list of errors
     * @return A failed ParseResult
     */
    static <T> ParseResult<T> failure(List<ParseError> errors) {
        return new Failure<>(List.copyOf(Objects.requireNonNull(errors)));
    }

    // ==================== Query Methods ====================

    /**
     * Checks if this result represents a successful parse.
     *
     * @return true if successful, false if failed
     */
    default boolean isSuccess() {
        return this instanceof Success;
    }

    /**
     * Checks if this result represents a failed parse.
     *
     * @return true if failed, false if successful
     */
    default boolean isFailure() {
        return this instanceof Failure;
    }

    /**
     * Gets the parsed value.
     *
     * @return The value if successful, null if failed
     */
    default T getValue() {
        return switch (this) {
            case Success(var value) -> value;
            case Failure(var errors) -> null;
        };
    }

    /**
     * Gets the parsed value or null if failed.
     *
     * @return The value if successful, null if failed
     */
    default T getOrNull() {
        return getValue();
    }

    /**
     * Gets the parsed value or a default if failed.
     *
     * @param defaultValue The default value to return if failed
     * @return The value if successful, defaultValue if failed
     */
    default T getOrElse(T defaultValue) {
        return isSuccess() ? getValue() : defaultValue;
    }

    /**
     * Gets the list of errors.
     *
     * @return The errors if failed, empty list if successful
     */
    default List<ParseError> getErrors() {
        return switch (this) {
            case Success(var value) -> List.of();
            case Failure(var errors) -> errors;
        };
    }

    /**
     * Gets the number of errors.
     *
     * @return The error count
     */
    default int getErrorCount() {
        return getErrors().size();
    }

    /**
     * Checks if there are any errors.
     *
     * @return true if there are errors, false otherwise
     */
    default boolean hasErrors() {
        return !getErrors().isEmpty();
    }

    /**
     * Checks if this result has a value (i.e., is successful).
     *
     * @return true if successful and has a value, false if failed
     */
    default boolean hasValue() {
        return isSuccess();
    }

    /**
     * Gets the parsed value or null if failed.
     * This is an alias for getValue() for compatibility.
     *
     * @return The value if successful, null if failed
     */
    default T getValueOrNull() {
        return getValue();
    }



    // ==================== Monadic Operations ====================

    /**
     * Maps the value using the provided function.
     *
     * @param <U> The type of the mapped result
     * @param mapper The mapping function
     * @return A new ParseResult with the mapped value
     */
    default <U> ParseResult<U> map(Function<T, U> mapper) {
        Objects.requireNonNull(mapper);
        return switch (this) {
            case Success(var value) -> success(mapper.apply(value));
            case Failure(var errors) -> failure(errors);
        };
    }

    /**
     * FlatMaps the value to another ParseResult.
     *
     * @param <U> The type of the mapped result
     * @param mapper The mapping function that returns a ParseResult
     * @return A new ParseResult
     */
    default <U> ParseResult<U> flatMap(Function<T, ParseResult<U>> mapper) {
        Objects.requireNonNull(mapper);
        return switch (this) {
            case Success(var value) -> mapper.apply(value);
            case Failure(var errors) -> failure(errors);
        };
    }

    /**
     * Transforms error messages.
     *
     * @param transformer Function to transform each error
     * @return A new ParseResult with transformed errors
     */
    default ParseResult<T> mapFailure(Function<ParseError, ParseError> transformer) {
        Objects.requireNonNull(transformer);
        return switch (this) {
            case Success(var value) -> this;
            case Failure(var errors) ->
                failure(errors.stream().map(transformer).toList());
        };
    }

    /**
     * Adds context to all error messages.
     *
     * @param context The context to prepend to error messages
     * @return A new ParseResult with contextualized errors
     */
    default ParseResult<T> withContext(String context) {
        return mapFailure(error ->
            new ParseError(context + ": " + error.message(), error.token()));
    }

    /**
     * Returns an alternative result if this is a failure.
     *
     * @param alternative Supplier for the alternative result
     * @return This result if successful, alternative if failed
     */
    default ParseResult<T> orElse(Supplier<ParseResult<T>> alternative) {
        return isSuccess() ? this : alternative.get();
    }

    /**
     * Recovers from failure by providing a value.
     *
     * @param recovery Function to create a value from the errors
     * @return A successful result with the recovered value
     */
    default ParseResult<T> recover(Function<List<ParseError>, T> recovery) {
        return switch (this) {
            case Success(var value) -> this;
            case Failure(var errors) -> success(recovery.apply(errors));
        };
    }

    /**
     * Recovers from failure with another ParseResult.
     *
     * @param recovery Function to create another ParseResult from the errors
     * @return The recovered ParseResult
     */
    default ParseResult<T> recoverWith(Function<List<ParseError>, ParseResult<T>> recovery) {
        return switch (this) {
            case Success(var value) -> this;
            case Failure(var errors) -> recovery.apply(errors);
        };
    }

    // ==================== Side Effects ====================

    /**
     * Performs an action on the success value without changing the result.
     *
     * @param action The action to perform
     * @return This result unchanged
     */
    default ParseResult<T> peek(Consumer<T> action) {
        if (this instanceof Success(var value)) {
            action.accept(value);
        }
        return this;
    }

    /**
     * Performs an action on errors without changing the result.
     *
     * @param action The action to perform on the error list
     * @return This result unchanged
     */
    default ParseResult<T> peekErrors(Consumer<List<ParseError>> action) {
        if (this instanceof Failure(var errors)) {
            action.accept(errors);
        }
        return this;
    }

    // ==================== Static Utility Methods ====================

    /**
     * Combines two results using a binary function.
     *
     * @param <A> The type of the first result
     * @param <B> The type of the second result
     * @param <C> The type of the combined result
     * @param first The first ParseResult
     * @param second The second ParseResult
     * @param combiner Function to combine the values
     * @return A combined ParseResult
     */
    static <A, B, C> ParseResult<C> combine(
            ParseResult<A> first,
            ParseResult<B> second,
            BiFunction<A, B, C> combiner) {
        return switch (first) {
            case Success(var a) -> switch (second) {
                case Success(var b) -> success(combiner.apply(a, b));
                case Failure(var errors) -> failure(errors);
            };
            case Failure(var errors1) -> switch (second) {
                case Success(var value) -> failure(errors1);
                case Failure(var errors2) -> failure(concat(errors1, errors2));
            };
        };
    }

    /**
     * Sequences a list of results, accumulating all errors.
     *
     * @param <T> The type of the individual results
     * @param results The list of ParseResults to sequence
     * @return A ParseResult containing a list of all values or all errors
     */
    static <T> ParseResult<List<T>> sequence(List<ParseResult<T>> results) {
        List<T> values = new ArrayList<>();
        List<ParseError> allErrors = new ArrayList<>();

        for (ParseResult<T> result : results) {
            switch (result) {
                case Success(var value) -> values.add(value);
                case Failure(var errors) -> allErrors.addAll(errors);
            }
        }

        return allErrors.isEmpty()
            ? success(values)
            : failure(allErrors);
    }

    /**
     * Accumulates errors from another result.
     *
     * @param other The other ParseResult to accumulate errors from
     * @return A new ParseResult with accumulated errors
     */
    default ParseResult<T> accumulate(ParseResult<?> other) {
        return switch (this) {
            case Success(var value) -> switch (other) {
                case Success(var otherValue) -> this;
                case Failure(var errors) -> failure(errors);
            };
            case Failure(var myErrors) -> switch (other) {
                case Success(var value) -> this;
                case Failure(var otherErrors) -> failure(concat(myErrors, otherErrors));
            };
        };
    }

    /**
     * Helper to concatenate error lists.
     */
    private static List<ParseError> concat(List<ParseError> a, List<ParseError> b) {
        if (a.isEmpty()) return b;
        if (b.isEmpty()) return a;
        var result = new ArrayList<ParseError>(a.size() + b.size());
        result.addAll(a);
        result.addAll(b);
        return List.copyOf(result);
    }

    /**
     * Gets all error messages as a formatted string.
     *
     * @return A string containing all error messages, one per line
     */
    default String getFormattedErrors() {
        List<ParseError> errors = getErrors();
        if (errors.isEmpty()) {
            return "";
        }

        return errors.stream()
            .map(ParseError::getFormattedMessage)
            .collect(java.util.stream.Collectors.joining("\n"));
    }
}
