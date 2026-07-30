package com.xpresso.util;

import com.xpresso.lexer.Token;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages syntax errors encountered during parsing.
 * Stores errors, provides reporting functionality, and assists in error recovery.
 */
public class SyntaxErrorHandler {
    private final List<SyntaxError> errors; // Stores all syntax errors
    private static final int MAX_ERRORS = 10;

    /**
     * Represents a syntax error with message, location, and suggestion.
     */
    public static class SyntaxError {
        private final String message;
        private final int line;
        private final int column;
        private final String suggestion;

        public SyntaxError(String message, int line, int column, String suggestion) {
            this.message = message;
            this.line = line;
            this.column = column;
            this.suggestion = suggestion;
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
            return String.format("Syntax Error at line %d, column %d: %s\nSuggestion: %s",
                    line, column, message, suggestion);
        }
    }

    public SyntaxErrorHandler() {
        this.errors = new ArrayList<>();
    }

    /**
     * Reports a syntax error and stores it.
     *
     * @param message     Description of the error.
     * @param line        Line number where the error occurred.
     * @param column      Column number where the error occurred.
     * @param suggestion  Suggested fix for the error.
     */
    public void reportError(String message, int line, int column, String suggestion) {
        if (errors.size() >= MAX_ERRORS) {
            return; // Silently stop collecting; avoid spam
        }
        errors.add(new SyntaxError(message, line, column, suggestion));
    }

    /**
     * Handles unexpected token errors.
     *
     * @param foundToken The unexpected token encountered.
     * @param expected   Description of the expected token or structure.
     */
    public void handleUnexpectedToken(Token foundToken, String expected) {
        reportError(
            "Unexpected token: '" + foundToken.getLexeme() + "'",
            foundToken.getLine(),
            foundToken.getColumn(),
            "Expected " + expected
        );
    }

    /**
     * Handles missing token errors.
     *
     * @param expected  Description of the missing token.
     * @param line      Line number where the issue occurred.
     * @param column    Column number where the issue occurred.
     */
    public void handleMissingToken(String expected, int line, int column) {
        reportError(
            "Missing expected token: " + expected,
            line,
            column,
            "Ensure the correct token appears here."
        );
    }

    /**
     * Handles unclosed structures like missing brackets, parentheses, etc.
     *
     * @param structure Description of the unclosed structure.
     * @param line      Line number where the issue occurred.
     * @param column    Column number where the issue occurred.
     */
    public void handleUnclosedStructure(String structure, int line, int column) {
        reportError(
            "Unclosed " + structure,
            line,
            column,
            "Ensure you have a matching closing " + structure
        );
    }

    /**
     * Prints all recorded syntax errors.
     */
    public void printErrors() {
        if (errors.isEmpty()) {
            System.out.println("No syntax errors found.");
        } else {
            System.err.println("\nSyntax Errors:");
            for (SyntaxError error : errors) {
                System.err.println(error);
            }
        }
    }

    /**
     * Returns whether any syntax errors have been recorded.
     *
     * @return True if errors exist, otherwise false.
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Returns the list of all recorded syntax errors.
     */
    public List<SyntaxError> getErrors() {
        return new ArrayList<>(errors);
    }

    /**
     * Clears all recorded syntax errors.
     */
    public void clearErrors() {
        errors.clear();
    }
}