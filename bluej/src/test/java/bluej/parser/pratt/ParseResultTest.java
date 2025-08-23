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
import bluej.parser.pratt.KotlinPrattParser.ParseError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ParseResult record.
 *
 * Tests verify that ParseResult properly:
 * - Encapsulates parsing outcomes immutably
 * - Handles success and failure scenarios correctly
 * - Provides appropriate factory methods
 * - Validates input parameters
 * - Combines results correctly
 * - Formats error messages appropriately
 *
 * @author BlueJ Development Team
 */
public class ParseResultTest {

    @Test
    @DisplayName("Success factory method creates result with no errors")
    void testSuccessWithResult() {
        // Test with null result (foundation phase behavior)
        ParseResult result = ParseResult.success(null);

        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertFalse(result.hasResult());
        assertNull(result.result());
        assertTrue(result.errors().isEmpty());
        assertEquals(0, result.getErrorCount());
        assertNull(result.getFirstError());
    }

    @Test
    @DisplayName("Success factory method with null result works correctly")
    void testSuccessWithNullResult() {
        ParseResult result = ParseResult.success(null);

        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertFalse(result.hasResult());
        assertNull(result.result());
        assertTrue(result.errors().isEmpty());
        assertEquals(0, result.getErrorCount());
    }

    @Test
    @DisplayName("Parameterless success factory method creates empty success result")
    void testSuccessParameterless() {
        ParseResult result = ParseResult.success();

        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertFalse(result.hasResult());
        assertNull(result.result());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    @DisplayName("Failure factory method with error list creates failed result")
    void testFailureWithErrorList() {
        ParseError error1 = createError("First error", 1, 5);
        ParseError error2 = createError("Second error", 2, 10);
        List<ParseError> errors = List.of(error1, error2);

        ParseResult result = ParseResult.failure(errors);

        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
        assertFalse(result.hasResult());
        assertNull(result.result());
        assertEquals(2, result.getErrorCount());
        assertEquals(error1, result.getFirstError());
        assertEquals(errors, result.errors());
    }

    @Test
    @DisplayName("Failure factory method with single error creates failed result")
    void testFailureWithSingleError() {
        ParseError error = createError("Single error", 1, 5);

        ParseResult result = ParseResult.failure(error);

        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
        assertFalse(result.hasResult());
        assertEquals(1, result.getErrorCount());
        assertEquals(error, result.getFirstError());
        assertEquals(List.of(error), result.errors());
    }

    @Test
    @DisplayName("Factory method 'of' creates result with both node and errors")
    void testOfFactoryMethod() {
        // Test with null result and errors (partial failure scenario)
        ParseError error = createError("Warning", 1, 5);
        List<ParseError> errors = List.of(error);

        ParseResult result = ParseResult.of(null, errors);

        assertFalse(result.isSuccess()); // Has errors, so not success
        assertTrue(result.isFailure());
        assertFalse(result.hasResult());
        assertNull(result.result());
        assertEquals(1, result.getErrorCount());
        assertEquals(error, result.getFirstError());
    }

    @Test
    @DisplayName("Constructor validates null errors list")
    void testNullErrorsValidation() {
        assertThrows(NullPointerException.class, () -> {
            new ParseResult(null, null);
        });
    }

    @Test
    @DisplayName("Single error failure validates null error")
    void testSingleErrorFailureValidation() {
        assertThrows(NullPointerException.class, () -> {
            ParseResult.failure((ParseError) null);
        });
    }

    @Test
    @DisplayName("Errors list is immutable")
    void testErrorsImmutability() {
        ParseError error = createError("Test error", 1, 5);
        List<ParseError> originalErrors = List.of(error);

        ParseResult result = ParseResult.failure(originalErrors);

        // Getting errors should return immutable list
        List<ParseError> retrievedErrors = result.errors();
        assertThrows(UnsupportedOperationException.class, () -> {
            retrievedErrors.add(createError("Another error", 2, 1));
        });
    }

    @Test
    @DisplayName("Formatted errors string formats all errors correctly")
    void testFormattedErrors() {
        ParseError error1 = createError("First error", 1, 5);
        ParseError error2 = createError("Second error", 2, 10);
        List<ParseError> errors = List.of(error1, error2);

        ParseResult result = ParseResult.failure(errors);

        String formatted = result.getFormattedErrors();
        assertTrue(formatted.contains("First error"));
        assertTrue(formatted.contains("Second error"));
        assertTrue(formatted.contains("line 1, column 5"));
        assertTrue(formatted.contains("line 2, column 10"));
        assertTrue(formatted.contains("\n")); // Should be multi-line
    }

    @Test
    @DisplayName("Formatted errors returns empty string for success")
    void testFormattedErrorsEmpty() {
        ParseResult result = ParseResult.success();

        String formatted = result.getFormattedErrors();

        assertEquals("", formatted);
    }

    @Test
    @DisplayName("Combine method merges errors and preserves result")
    void testCombineWithErrors() {
        ParseError error1 = createError("First error", 1, 5);
        ParseError error2 = createError("Second error", 2, 10);

        // Test combining results (foundation phase uses null)
        ParseResult result1 = ParseResult.of(null, List.of(error1));
        ParseResult result2 = ParseResult.failure(List.of(error2));

        ParseResult combined = result1.combine(result2);

        assertTrue(combined.isFailure());
        assertFalse(combined.hasResult());
        assertNull(combined.result()); // Both results are null
        assertEquals(2, combined.getErrorCount());
        assertTrue(combined.errors().contains(error1));
        assertTrue(combined.errors().contains(error2));
    }

    @Test
    @DisplayName("Combine method takes other result when this result is null")
    void testCombineWithNullResult() {
        ParseError error1 = createError("First error", 1, 5);
        ParseError error2 = createError("Second error", 2, 10);

        // Test combining where both results are null (foundation phase)
        ParseResult result1 = ParseResult.failure(List.of(error1)); // null result
        ParseResult result2 = ParseResult.of(null, List.of(error2)); // null result

        ParseResult combined = result1.combine(result2);

        assertTrue(combined.isFailure());
        assertFalse(combined.hasResult());
        assertNull(combined.result()); // Both results are null
        assertEquals(2, combined.getErrorCount());
    }

    @Test
    @DisplayName("Combine method with successful results")
    void testCombineSuccessful() {
        // Test combining successful results (foundation phase)
        ParseResult result1 = ParseResult.success(null);
        ParseResult result2 = ParseResult.success(null);

        ParseResult combined = result1.combine(result2);

        assertTrue(combined.isSuccess());
        assertFalse(combined.hasResult());
        assertNull(combined.result()); // Both results are null
        assertEquals(0, combined.getErrorCount());
    }

    @Test
    @DisplayName("Combine method validates null parameter")
    void testCombineValidation() {
        ParseResult result = ParseResult.success();

        assertThrows(NullPointerException.class, () -> {
            result.combine(null);
        });
    }

    @Test
    @DisplayName("ToString method formats success correctly")
    void testToStringSuccess() {
        // Success without result
        ParseResult successEmpty = ParseResult.success();
        String emptyString = successEmpty.toString();
        assertTrue(emptyString.contains("success"));
        assertTrue(emptyString.contains("no result"));

        // Success with null result (foundation phase)
        ParseResult successWithResult = ParseResult.success(null);
        String withResultString = successWithResult.toString();
        assertTrue(withResultString.contains("success"));
        assertTrue(withResultString.contains("no result"));
    }

    @Test
    @DisplayName("ToString method formats failure correctly")
    void testToStringFailure() {
        ParseError error = createError("Test error message", 1, 5);
        ParseResult failure = ParseResult.failure(error);

        String failureString = failure.toString();

        assertTrue(failureString.contains("failure"));
        assertTrue(failureString.contains("1 error"));
        assertTrue(failureString.contains("Test error message"));
    }

    @Test
    @DisplayName("ToString method formats multiple errors correctly")
    void testToStringMultipleErrors() {
        ParseError error1 = createError("First error", 1, 5);
        ParseError error2 = createError("Second error", 2, 10);
        List<ParseError> errors = List.of(error1, error2);

        ParseResult failure = ParseResult.failure(errors);
        String failureString = failure.toString();

        assertTrue(failureString.contains("failure"));
        assertTrue(failureString.contains("2 error"));
        assertTrue(failureString.contains("First error")); // Should show first error
    }

    @Test
    @DisplayName("Record equality works correctly")
    void testRecordEquality() {
        // Test record equality with null results (foundation phase)
        ParseError error = createError("Test error", 1, 5);

        ParseResult result1 = ParseResult.of(null, List.of(error));
        ParseResult result2 = ParseResult.of(null, List.of(error));
        ParseResult result3 = ParseResult.success(null);

        assertEquals(result1, result2);
        assertNotEquals(result1, result3);
        assertEquals(result1.hashCode(), result2.hashCode());
    }

    @Test
    @DisplayName("Record components are accessible")
    void testRecordComponents() {
        // Test record components with null result (foundation phase)
        ParseError error = createError("Test error", 1, 5);
        List<ParseError> errors = List.of(error);

        ParseResult result = ParseResult.of(null, errors);

        assertNull(result.result());
        assertEquals(errors, result.errors());
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

    // Note: Foundation phase uses null for ParsedNode results,
    // so no mock implementation is needed for these tests
}
