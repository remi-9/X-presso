package com.xpresso.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Logs and manages lexical errors encountered during the analysis phase.
 * Includes utilities to report unrecognized tokens, invalid inputs, and
 * suggestions for potential recovery mechanisms.
 */
public class ErrorHandler {
    // List to store all encountered errors
    private final List<LexicalError> errors;
    
    // Current file being processed
    private String currentFile;
    
    // Whether to print errors immediately
    private final boolean immediateLogging;
    
    /**
     * Represents different types of lexical errors that can occur
     */
    public enum ErrorType {
        INVALID_CHARACTER,
        INVALID_DELIMITER,
        UNTERMINATED_STRING,
        UNTERMINATED_COMMENT,
        INVALID_LITERAL,
        INVALID_NUMBER_FORMAT,
        INVALID_ESCAPE_SEQUENCE,
        INVALID_IDENTIFIER,
        INVALID_STRING,
        INVALID_OPERATOR,
        UNKNOWN_TOKEN,
        INVALID_COMPLEX_LITERAL,
        INVALID_DATE_FORMAT,
        INVALID_FRACTION_FORMAT,
        MISMATCHED_DELIMITERS,
        FILE_ERROR
    }

    /**
     * Represents a single lexical error
     */
    public static class LexicalError {
        private final ErrorType type;
        private final String message;
        private final int line;
        private final int column;
        private final String suggestion;

        public LexicalError(ErrorType type, String message, int line, int column, String suggestion) {
            this.type = type;
            this.message = message;
            this.line = line;
            this.column = column;
            this.suggestion = suggestion;
        }

        public ErrorType getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }

        public int getLine() {
            return line;
        }

        public int getColumn() {
            return column;
        }

        public String getSuggestion() {
            return suggestion;
        }

        @Override
        public String toString() {
            StringBuilder error = new StringBuilder();
            error.append(String.format("Error at line %d, column %d: %s%n", line, column, type));
            error.append(String.format("Description: %s%n", message));
            if (suggestion != null && !suggestion.isEmpty()) {
                error.append(String.format("Suggestion: %s%n", suggestion));
            }
            return error.toString();
        }
    }

    public ErrorHandler() {
        this(false); // Main / API print once; avoid duplicate stderr spam
    }

    public ErrorHandler(boolean immediateLogging) {
        this.errors = new ArrayList<>();
        this.immediateLogging = immediateLogging;
    }

    /**
     * Records a lexical error with optional recovery suggestion
     */
    public void reportError(ErrorType type, String message, int line, int column, String suggestion) {
        LexicalError error = new LexicalError(type, message, line, column, suggestion);
        errors.add(error);
        
        if (immediateLogging) {
            System.err.println(error);
        }
    }

    /**
     * Records a lexical error without recovery suggestion
     */
    public void reportError(ErrorType type, String message, int line, int column) {
        reportError(type, message, line, column, null);
    }

    /**
     * Handles invalid character errors
     */
    public void handleInvalidCharacter(char c, int line, int column) {
        reportError(
            ErrorType.INVALID_CHARACTER,
            String.format("Invalid character '%c' (ASCII: %d)", c, (int) c),
            line,
            column,
            "Remove or replace the invalid character"
        );
    }

    /**
     * Handles unterminated string literal errors
     */
    public void handleUnterminatedString(int line, int column) {
        reportError(
            ErrorType.UNTERMINATED_STRING,
            "String literal is not properly terminated",
            line,
            column,
            "Add matching quote to terminate the string"
        );
    }

    /**
     * Handles unterminated comment errors
     */
    public void handleUnterminatedComment(int line, int column) {
        reportError(
            ErrorType.UNTERMINATED_COMMENT,
            "Multi-line comment is not properly terminated",
            line,
            column,
            "Add */ to terminate the comment"
        );
    }

    /**
     * Handles invalid number format errors
     */
    public void handleInvalidNumber(String number, int line, int column) {
        reportError(
            ErrorType.INVALID_NUMBER_FORMAT,
            String.format("Invalid number format: %s", number),
            line,
            column,
            "Ensure the number follows the correct format"
        );
    }

    /**
     * Handles invalid escape sequence errors
     */
    public void handleInvalidEscapeSequence(String sequence, int line, int column) {
        reportError(
            ErrorType.INVALID_ESCAPE_SEQUENCE,
            String.format("Invalid escape sequence: %s", sequence),
            line,
            column,
            "Use valid escape sequences: \\n, \\t, \\r, \\\", \\\\"
        );
    }

    /**
     * Handles invalid identifier errors
     */
    public void handleInvalidIdentifier(String identifier, int line, int column) {
        reportError(
            ErrorType.INVALID_IDENTIFIER,
            String.format("Invalid identifier: %s", identifier),
            line,
            column,
            "Identifiers must start with a letter and contain only letters, digits, and underscores"
        );
    }

    /**
     * Handles invalid complex literal errors
     */
    public void handleInvalidComplexLiteral(String literal, int line, int column) {
        reportError(
            ErrorType.INVALID_COMPLEX_LITERAL,
            String.format("Invalid complex number literal: %s", literal),
            line,
            column,
            "Complex literals should be in the format $(real,imag)"
        );
    }

    /**
     * Handles invalid date format errors
     */
    public void handleInvalidDateFormat(String date, int line, int column) {
        reportError(
            ErrorType.INVALID_DATE_FORMAT,
            String.format("Invalid date format: %s", date),
            line,
            column,
            "Date literals should be in the format [YYYY|MM|DD]"
        );
    }

    public void handleInvalidString(String str, int line, int column) {
        reportError(
            ErrorType.INVALID_STRING,
            "Invalid string format: " + str,
            line,
            column,
            "Check string format and escape sequences"
        );
    }

    /**
     * Returns all recorded errors
     */
    public List<LexicalError> getErrors() {
        return new ArrayList<>(errors);
    }

    /**
     * Returns whether any errors have been recorded
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Prints all recorded errors
     */
    public void printErrors() {
        if (errors.isEmpty()) {
            System.out.println("No lexical errors found.");
            return;
        }

        System.err.println("\nLexical Errors:");
        for (LexicalError error : errors) {
            System.err.println(error);
        }
    }

    /**
     * Clears all recorded errors
     */
    public void clearErrors() {
        errors.clear();
    }

    /**
     * Sets the current file being processed
     */
    public void setCurrentFile(String filename) {
        this.currentFile = filename;
    }

    /**
     * Gets the current file being processed
     */
    public String getCurrentFile() {
        return this.currentFile;
    }
}