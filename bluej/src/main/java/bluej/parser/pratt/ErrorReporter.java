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

/**
 * Interface for reporting parsing errors in a standardized way.
 *
 * <p>This interface provides a bridge between the Pratt parser's error handling
 * and the broader BlueJ parser infrastructure. It allows for flexible error
 * reporting strategies while maintaining compatibility with the existing
 * SourceParser error reporting mechanism.</p>
 *
 * <p>Implementations can choose to report errors immediately, accumulate them
 * for batch reporting, or integrate with IDE-specific error display mechanisms.</p>
 *
 * @author BlueJ Development Team
 * @since BlueJ 5.4.0
 */
public interface ErrorReporter {

    /**
     * Reports a parse error with location information.
     *
     * @param message The error message describing what went wrong
     * @param token The token where the error occurred (may be null for EOF errors)
     */
    void reportError(String message, LocatableToken token);

    /**
     * Reports a parse error with severity level.
     *
     * @param message The error message describing what went wrong
     * @param token The token where the error occurred (may be null for EOF errors)
     * @param severity The severity level of the error
     */
    default void reportError(String message, LocatableToken token, ErrorSeverity severity) {
        // Default implementation ignores severity and delegates to simple reportError
        reportError(message, token);
    }

    /**
     * Reports a parse warning that doesn't prevent successful parsing.
     *
     * @param message The warning message
     * @param token The token where the warning occurred (may be null)
     */
    default void reportWarning(String message, LocatableToken token) {
        // Default implementation treats warnings as errors
        reportError(message, token, ErrorSeverity.WARNING);
    }

    /**
     * Reports an informational message about parsing.
     *
     * @param message The informational message
     * @param token The token related to the information (may be null)
     */
    default void reportInfo(String message, LocatableToken token) {
        // Default implementation ignores info messages
    }

    /**
     * Checks if any errors have been reported.
     *
     * @return true if errors have been reported, false otherwise
     */
    boolean hasErrors();

    /**
     * Gets the count of errors reported.
     *
     * @return The number of errors reported
     */
    int getErrorCount();

    /**
     * Clears all previously reported errors.
     */
    void clearErrors();

    /**
     * Enum representing the severity levels of parse errors.
     */
    enum ErrorSeverity {
        /** A fatal error that prevents parsing from continuing */
        ERROR,

        /** A warning that indicates a potential problem but doesn't stop parsing */
        WARNING,

        /** An informational message for debugging or user awareness */
        INFO
    }

    /**
     * A simple implementation that collects errors in a list.
     * This is useful for testing and batch error reporting.
     */
    class CollectingErrorReporter implements ErrorReporter {
        private final java.util.List<ParseError> errors = new java.util.ArrayList<>();

        /**
         * Record representing a parse error with all relevant information.
         */
        public record ParseError(String message, LocatableToken token, ErrorSeverity severity) {
            /**
             * Creates an error with ERROR severity.
             */
            public ParseError(String message, LocatableToken token) {
                this(message, token, ErrorSeverity.ERROR);
            }

            /**
             * Gets a formatted error message with location information.
             *
             * @return Formatted error message
             */
            public String getFormattedMessage() {
                if (token != null) {
                    return String.format("%s at line %d, column %d: %s",
                        severity, token.getLine(), token.getColumn(), message);
                } else {
                    return String.format("%s: %s", severity, message);
                }
            }
        }

        @Override
        public void reportError(String message, LocatableToken token) {
            errors.add(new ParseError(message, token, ErrorSeverity.ERROR));
        }

        @Override
        public void reportError(String message, LocatableToken token, ErrorSeverity severity) {
            errors.add(new ParseError(message, token, severity));
        }

        @Override
        public void reportWarning(String message, LocatableToken token) {
            errors.add(new ParseError(message, token, ErrorSeverity.WARNING));
        }

        @Override
        public void reportInfo(String message, LocatableToken token) {
            errors.add(new ParseError(message, token, ErrorSeverity.INFO));
        }

        @Override
        public boolean hasErrors() {
            return errors.stream().anyMatch(e -> e.severity() == ErrorSeverity.ERROR);
        }

        @Override
        public int getErrorCount() {
            return (int) errors.stream()
                .filter(e -> e.severity() == ErrorSeverity.ERROR)
                .count();
        }

        @Override
        public void clearErrors() {
            errors.clear();
        }

        /**
         * Gets all collected errors.
         *
         * @return An unmodifiable list of all parse errors
         */
        public java.util.List<ParseError> getErrors() {
            return java.util.Collections.unmodifiableList(errors);
        }

        /**
         * Gets only errors of a specific severity.
         *
         * @param severity The severity to filter by
         * @return A list of errors with the specified severity
         */
        public java.util.List<ParseError> getErrorsBySeverity(ErrorSeverity severity) {
            return errors.stream()
                .filter(e -> e.severity() == severity)
                .toList();
        }
    }

    /**
     * A no-op implementation that discards all errors.
     * Useful for scenarios where error reporting is not needed.
     */
    class NullErrorReporter implements ErrorReporter {
        @Override
        public void reportError(String message, LocatableToken token) {
            // Discard error
        }

        @Override
        public boolean hasErrors() {
            return false;
        }

        @Override
        public int getErrorCount() {
            return 0;
        }

        @Override
        public void clearErrors() {
            // Nothing to clear
        }
    }
}
