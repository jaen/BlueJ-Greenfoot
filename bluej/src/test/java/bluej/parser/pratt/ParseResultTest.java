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

import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.lexer.LineColPos;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.pratt.ParseResult.ParseError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ParseResult sealed interface.
 *
 * Tests verify that ParseResult properly:
 * - Encapsulates parsing outcomes immutably
 * - Handles success and failure scenarios correctly
 * - Provides appropriate factory methods
 * - Validates input parameters
 * - Supports monadic operations
 * - Formats error messages appropriately
 *
 * @author BlueJ Development Team
 */
public class ParseResultTest {

    @Test
    @DisplayName("Success factory method creates result with value")
    void testSuccessWithValue() {
        String value = "test-value";
        ParseResult<String> result = ParseResult.success(value);

        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertEquals(value, result.getValue());
        assertEquals(value, result.getOrNull());
        assertTrue(result.getErrors().isEmpty());
        assertFalse(result.hasErrors());
        assertEquals(0, result.getErrorCount());
        assertTrue(result.hasValue());
        assertEquals(value, result.getValueOrNull());
    }

    @Test
    @DisplayName("Success factory method with null value works correctly")
    void testSuccessWithNullValue() {
        ParseResult<String> result = ParseResult.success(null);

        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertNull(result.getValue());
        assertNull(result.getOrNull());
        assertNull(result.getValueOrNull());
        assertTrue(result.getErrors().isEmpty());
        assertFalse(result.hasErrors());
        assertEquals(0, result.getErrorCount());
        assertTrue(result.hasValue()); // Still has a value, even if null
    }

    @Test
    @DisplayName("Failure factory method with message and token creates failed result")
    void testFailureWithMessageAndToken() {
        LocatableToken token = createToken(JavaTokenTypes.IDENT, "test", 1, 5);
        ParseResult<String> result = ParseResult.failure("Error message", token);

        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
        assertNull(result.getValue());
        assertNull(result.getOrNull());
        assertNull(result.getValueOrNull());
        assertFalse(result.hasValue());
        assertTrue(result.hasErrors());
        assertEquals(1, result.getErrorCount());

        List<ParseError> errors = result.getErrors();
        assertEquals(1, errors.size());
        ParseError error = errors.get(0);
        assertEquals("Error message", error.message());
        assertEquals(token, error.token());
        assertEquals(1, error.getLine());
        assertEquals(5, error.getColumn());
    }

    @Test
    @DisplayName("Failure factory method with single ParseError")
    void testFailureWithSingleError() {
        ParseError error = createError("Single error", 2, 10);
        ParseResult<String> result = ParseResult.failure(error);

        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
        assertNull(result.getValue());
        assertTrue(result.hasErrors());
        assertEquals(1, result.getErrorCount());
        assertEquals(error, result.getErrors().get(0));
    }

    @Test
    @DisplayName("Failure factory method with error list creates failed result")
    void testFailureWithErrorList() {
        ParseError error1 = createError("First error", 1, 5);
        ParseError error2 = createError("Second error", 2, 10);
        List<ParseError> errors = List.of(error1, error2);

        ParseResult<String> result = ParseResult.failure(errors);

        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
        assertNull(result.getValue());
        assertFalse(result.hasValue());
        assertEquals(2, result.getErrorCount());
        assertEquals(errors, result.getErrors());
    }

    @Test
    @DisplayName("getOrElse returns value for success")
    void testGetOrElseWithSuccess() {
        ParseResult<String> result = ParseResult.success("value");
        assertEquals("value", result.getOrElse("default"));
    }

    @Test
    @DisplayName("getOrElse returns default for failure")
    void testGetOrElseWithFailure() {
        ParseResult<String> result = ParseResult.failure("error", null);
        assertEquals("default", result.getOrElse("default"));
    }

    @Test
    @DisplayName("Map transforms success value")
    void testMapSuccess() {
        ParseResult<Integer> result = ParseResult.success(5);
        ParseResult<String> mapped = result.map(i -> "Number: " + i);

        assertTrue(mapped.isSuccess());
        assertEquals("Number: 5", mapped.getValue());
    }

    @Test
    @DisplayName("Map propagates failure")
    void testMapFailure() {
        ParseError error = createError("error", 1, 1);
        ParseResult<Integer> result = ParseResult.failure(error);
        ParseResult<String> mapped = result.map(i -> "Number: " + i);

        assertTrue(mapped.isFailure());
        assertEquals(1, mapped.getErrorCount());
        assertEquals(error, mapped.getErrors().get(0));
    }

    @Test
    @DisplayName("FlatMap chains successful operations")
    void testFlatMapSuccess() {
        ParseResult<Integer> result = ParseResult.success(5);
        ParseResult<String> flatMapped = result.flatMap(i ->
            ParseResult.success("Number: " + i)
        );

        assertTrue(flatMapped.isSuccess());
        assertEquals("Number: 5", flatMapped.getValue());
    }

    @Test
    @DisplayName("FlatMap propagates first failure")
    void testFlatMapFirstFailure() {
        ParseError error = createError("error", 1, 1);
        ParseResult<Integer> result = ParseResult.failure(error);
        ParseResult<String> flatMapped = result.flatMap(i ->
            ParseResult.success("Number: " + i)
        );

        assertTrue(flatMapped.isFailure());
        assertEquals(1, flatMapped.getErrorCount());
        assertEquals(error, flatMapped.getErrors().get(0));
    }

    @Test
    @DisplayName("FlatMap propagates second failure")
    void testFlatMapSecondFailure() {
        ParseResult<Integer> result = ParseResult.success(5);
        ParseError error = createError("transformation error", 2, 2);
        ParseResult<String> flatMapped = result.flatMap(i ->
            ParseResult.failure(error)
        );

        assertTrue(flatMapped.isFailure());
        assertEquals(1, flatMapped.getErrorCount());
        assertEquals(error, flatMapped.getErrors().get(0));
    }

    @Test
    @DisplayName("MapFailure transforms error messages")
    void testMapFailureTransformErrors() {
        ParseError error = createError("original", 1, 1);
        ParseResult<String> result = ParseResult.failure(error);
        ParseResult<String> transformed = result.mapFailure(e ->
            new ParseError("transformed: " + e.message(), e.token())
        );

        assertTrue(transformed.isFailure());
        assertEquals("transformed: original", transformed.getErrors().get(0).message());
    }

    @Test
    @DisplayName("MapFailure leaves success unchanged")
    void testMapFailureWithSuccess() {
        ParseResult<String> result = ParseResult.success("value");
        ParseResult<String> transformed = result.mapFailure(e ->
            new ParseError("should not be called", null)
        );

        assertTrue(transformed.isSuccess());
        assertEquals("value", transformed.getValue());
    }

    @Test
    @DisplayName("WithContext adds context to error messages")
    void testWithContext() {
        ParseError error = createError("error message", 1, 1);
        ParseResult<String> result = ParseResult.failure(error);
        ParseResult<String> contextualized = result.withContext("While parsing");

        assertTrue(contextualized.isFailure());
        assertEquals("While parsing: error message",
                     contextualized.getErrors().get(0).message());
    }

    @Test
    @DisplayName("OrElse returns alternative on failure")
    void testOrElse() {
        ParseResult<String> failure = ParseResult.failure("error", null);
        ParseResult<String> alternative = ParseResult.success("alternative");
        ParseResult<String> result = failure.orElse(() -> alternative);

        assertTrue(result.isSuccess());
        assertEquals("alternative", result.getValue());
    }

    @Test
    @DisplayName("OrElse returns original on success")
    void testOrElseWithSuccess() {
        ParseResult<String> success = ParseResult.success("original");
        ParseResult<String> result = success.orElse(() -> ParseResult.success("alternative"));

        assertTrue(result.isSuccess());
        assertEquals("original", result.getValue());
    }

    @Test
    @DisplayName("Recover transforms failure to success")
    void testRecover() {
        ParseError error = createError("error", 1, 1);
        ParseResult<String> failure = ParseResult.failure(error);
        ParseResult<String> recovered = failure.recover(errors ->
            "Recovered from " + errors.size() + " errors"
        );

        assertTrue(recovered.isSuccess());
        assertEquals("Recovered from 1 errors", recovered.getValue());
    }

    @Test
    @DisplayName("RecoverWith transforms failure to new ParseResult")
    void testRecoverWith() {
        ParseError error = createError("error", 1, 1);
        ParseResult<String> failure = ParseResult.failure(error);
        ParseResult<String> recovered = failure.recoverWith(errors ->
            ParseResult.success("Recovered")
        );

        assertTrue(recovered.isSuccess());
        assertEquals("Recovered", recovered.getValue());
    }

    @Test
    @DisplayName("Peek performs side effect on success value")
    void testPeek() {
        AtomicBoolean called = new AtomicBoolean(false);
        ParseResult<String> result = ParseResult.success("value");
        ParseResult<String> peeked = result.peek(v -> {
            assertEquals("value", v);
            called.set(true);
        });

        assertTrue(called.get());
        assertSame(result, peeked); // Should return same instance
    }

    @Test
    @DisplayName("PeekErrors performs side effect on errors")
    void testPeekErrors() {
        AtomicBoolean called = new AtomicBoolean(false);
        ParseError error = createError("error", 1, 1);
        ParseResult<String> result = ParseResult.failure(error);
        ParseResult<String> peeked = result.peekErrors(errors -> {
            assertEquals(1, errors.size());
            called.set(true);
        });

        assertTrue(called.get());
        assertSame(result, peeked); // Should return same instance
    }

    @Test
    @DisplayName("Combine merges two successful results")
    void testCombineSuccess() {
        ParseResult<Integer> first = ParseResult.success(5);
        ParseResult<String> second = ParseResult.success("test");
        ParseResult<String> combined = ParseResult.combine(first, second,
            (i, s) -> i + "-" + s
        );

        assertTrue(combined.isSuccess());
        assertEquals("5-test", combined.getValue());
    }

    @Test
    @DisplayName("Combine propagates first failure")
    void testCombineFirstFailure() {
        ParseError error = createError("first error", 1, 1);
        ParseResult<Integer> first = ParseResult.failure(error);
        ParseResult<String> second = ParseResult.success("test");
        ParseResult<String> combined = ParseResult.combine(first, second,
            (i, s) -> i + "-" + s
        );

        assertTrue(combined.isFailure());
        assertEquals(1, combined.getErrorCount());
        assertEquals(error, combined.getErrors().get(0));
    }

    @Test
    @DisplayName("Combine accumulates errors from both failures")
    void testCombineBothFailures() {
        ParseError error1 = createError("first error", 1, 1);
        ParseError error2 = createError("second error", 2, 2);
        ParseResult<Integer> first = ParseResult.failure(error1);
        ParseResult<String> second = ParseResult.failure(error2);
        ParseResult<String> combined = ParseResult.combine(first, second,
            (i, s) -> i + "-" + s
        );

        assertTrue(combined.isFailure());
        assertEquals(2, combined.getErrorCount());
        assertTrue(combined.getErrors().contains(error1));
        assertTrue(combined.getErrors().contains(error2));
    }

    @Test
    @DisplayName("Sequence collects all successful values")
    void testSequenceAllSuccess() {
        List<ParseResult<Integer>> results = List.of(
            ParseResult.success(1),
            ParseResult.success(2),
            ParseResult.success(3)
        );
        ParseResult<List<Integer>> sequenced = ParseResult.sequence(results);

        assertTrue(sequenced.isSuccess());
        assertEquals(List.of(1, 2, 3), sequenced.getValue());
    }

    @Test
    @DisplayName("Sequence accumulates all errors")
    void testSequenceWithErrors() {
        ParseError error1 = createError("error1", 1, 1);
        ParseError error2 = createError("error2", 2, 2);
        List<ParseResult<Integer>> results = List.of(
            ParseResult.success(1),
            ParseResult.failure(error1),
            ParseResult.failure(error2)
        );
        ParseResult<List<Integer>> sequenced = ParseResult.sequence(results);

        assertTrue(sequenced.isFailure());
        assertEquals(2, sequenced.getErrorCount());
        assertTrue(sequenced.getErrors().contains(error1));
        assertTrue(sequenced.getErrors().contains(error2));
    }

    @Test
    @DisplayName("Accumulate merges errors from another result")
    void testAccumulate() {
        ParseError error1 = createError("error1", 1, 1);
        ParseError error2 = createError("error2", 2, 2);
        ParseResult<String> result1 = ParseResult.failure(error1);
        ParseResult<Integer> result2 = ParseResult.failure(error2);
        ParseResult<String> accumulated = result1.accumulate(result2);

        assertTrue(accumulated.isFailure());
        assertEquals(2, accumulated.getErrorCount());
        assertTrue(accumulated.getErrors().contains(error1));
        assertTrue(accumulated.getErrors().contains(error2));
    }

    @Test
    @DisplayName("GetFormattedErrors returns formatted error string")
    void testGetFormattedErrors() {
        ParseError error1 = createError("First error", 1, 5);
        ParseError error2 = createError("Second error", 2, 10);
        ParseResult<String> result = ParseResult.failure(List.of(error1, error2));

        String formatted = result.getFormattedErrors();
        assertTrue(formatted.contains("First error"));
        assertTrue(formatted.contains("Second error"));
        assertTrue(formatted.contains("line 1"));
        assertTrue(formatted.contains("column 5"));
        assertTrue(formatted.contains("line 2"));
        assertTrue(formatted.contains("column 10"));
        assertTrue(formatted.contains("\n")); // Should be multi-line
    }

    @Test
    @DisplayName("GetFormattedErrors returns empty string for success")
    void testGetFormattedErrorsEmpty() {
        ParseResult<String> result = ParseResult.success("value");
        assertEquals("", result.getFormattedErrors());
    }

    @Test
    @DisplayName("ParseError formats message with location")
    void testParseErrorFormatting() {
        LocatableToken token = createToken(JavaTokenTypes.IDENT, "test", 3, 15);
        ParseError error = new ParseError("Test error", token);

        assertEquals("Test error", error.message());
        assertEquals(token, error.token());
        assertEquals(3, error.getLine());
        assertEquals(15, error.getColumn());

        String formatted = error.getFormattedMessage();
        assertTrue(formatted.contains("Error at line 3, column 15: Test error"));
    }

    @Test
    @DisplayName("ParseError handles null token")
    void testParseErrorWithNullToken() {
        ParseError error = new ParseError("Test error", null);

        assertEquals("Test error", error.message());
        assertNull(error.token());
        assertEquals(-1, error.getLine());
        assertEquals(-1, error.getColumn());

        String formatted = error.getFormattedMessage();
        assertEquals("Error: Test error", formatted);
    }

    @Test
    @DisplayName("Null validation for failure factory")
    void testNullValidation() {
        assertThrows(NullPointerException.class, () -> {
            ParseResult.failure((ParseError) null);
        });

        assertThrows(NullPointerException.class, () -> {
            ParseResult.failure((List<ParseError>) null);
        });
    }

    @Test
    @DisplayName("Errors list is immutable")
    void testErrorsImmutability() {
        ParseError error = createError("Test error", 1, 5);
        ParseResult<String> result = ParseResult.failure(error);

        List<ParseError> errors = result.getErrors();
        assertThrows(UnsupportedOperationException.class, () -> {
            errors.add(createError("Another error", 2, 1));
        });
    }

    @Test
    @DisplayName("Success and Failure are properly typed as sealed records")
    void testSealedRecordTypes() {
        ParseResult<String> success = ParseResult.success("value");
        ParseResult<String> failure = ParseResult.failure("error", null);

        assertTrue(success instanceof ParseResult.Success);
        assertTrue(failure instanceof ParseResult.Failure);

        // Test record component access
        ParseResult.Success<String> successRecord = (ParseResult.Success<String>) success;
        assertEquals("value", successRecord.value());

        ParseResult.Failure<String> failureRecord = (ParseResult.Failure<String>) failure;
        assertEquals(1, failureRecord.errors().size());
    }

    // Helper methods

    private ParseError createError(String message, int line, int column) {
        LocatableToken token = createToken(JavaTokenTypes.IDENT, "test", line, column);
        return new ParseError(message, token);
    }

    private LocatableToken createToken(int type, String text, int line, int column) {
        LineColPos pos = new LineColPos(line, column, 0);
        return new LocatableToken(type, text, pos, pos);
    }
}
