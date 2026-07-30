package com.xpresso.lexer;

import java.util.ArrayList;
import java.util.List;

import com.xpresso.util.ErrorHandler;
import com.xpresso.util.SourceReader;
import com.xpresso.util.ErrorHandler.ErrorType;
import com.xpresso.util.SourceReader.SourceReaderException;

/**
 * Lexer class that performs lexical analysis on a given source code.
 * It converts the input into a list of tokens.
 */
public class Lexer {
    private final SourceReader reader;
    private final List<Token> tokens;
    private final ErrorHandler errorHandler;

    public Lexer(SourceReader reader) {
        this.reader = reader;
        this.tokens = new ArrayList<>();
        this.errorHandler = new ErrorHandler();
        this.errorHandler.setCurrentFile(reader.getFilePath());
    }

    /**
     * Retrieves the error handler associated with this lexer.
     *
     * @return The error handler instance.
     */
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    /**
     * Tokenizes the source code and returns a list of tokens.
     * It performs lexical analysis on the source code, splitting it into individual tokens.
     * Each token is analyzed and added to the token list.
     * If an error is encountered during lexical analysis, an error is reported to the error handler.
     * At the end of the analysis, an EOF token is added to the list of tokens.
     *
     * @return A list of tokens from the source code.
     */
    public List<Token> tokenize() {
        try {
            char currentChar;
            while ((currentChar = reader.readNext()) != SourceReader.EOF) {
                try {
                    // Handle whitespace characters
                    if (Character.isWhitespace(currentChar)) {
                        handleWhitespace(currentChar);
                    }
                    // Handle complex literals
                    else if (currentChar == '$') {
                        handleComplexLiteral(currentChar);
                    }
                    // Handle identifiers and keywords
                    else if (Character.isLetter(currentChar)) {
                        handleIdentifierOrKeyword(currentChar);
                    }
                    // Handle number literals
                    else if (Character.isDigit(currentChar)) {
                        handleNumberLiteral(currentChar);
                    }
                    // Handle colon characters
                    else if (isColon(currentChar)) {
                        handleColon(currentChar);
                    }
                    // Handle period characters
                    else if (currentChar == '.') {
                        handlePeriods(currentChar);
                    }
                    // Handle comments
                    else if (currentChar == '/') {
                        handleComment(currentChar);
                    }
                    // Handle object delimiters or operators
                    else if (currentChar == '<' || currentChar == '>') {
                        handleObjectDelimiterOrOperator(currentChar);
                    }
                    // Handle operator symbols
                    else if (isOperatorSymbol(currentChar) || currentChar == '<') {
                        handleOperator(currentChar);
                    }
                    // Handle delimiter or bracket characters
                    else if (isDelimiterOrBracket(currentChar)) {
                        handleDelimiterOrBracket(currentChar);
                    }
                    // Handle character literals
                    else if (currentChar == '\'') {
                        handleCharLiteral(); 
                    // Handle string literals
                    } else if (currentChar == '"') {
                        handleStringLiteral(); 
                    }
                    // Handle unknown characters
                    else {
                        errorHandler.handleInvalidCharacter(currentChar, reader.getLine(), reader.getColumn());
                    }
                } catch (Exception e) {
                    // Handle unknown errors
                    errorHandler.reportError(
                        ErrorType.UNKNOWN_TOKEN,
                        "Error processing token: " + e.getMessage(),
                        reader.getLine(),
                        reader.getColumn()
                    );
                }
            }

            // Add an EOF token to the list of tokens
            tokens.add(new Token(TokenType.EOF, "", reader.getLine(), reader.getColumn()));
        } catch (SourceReaderException e) {
            // Handle file errors
            errorHandler.reportError(
                ErrorType.FILE_ERROR,
                "Error reading source file: " + e.getMessage(),
                reader.getLine(),
                reader.getColumn()
            );
        }

        // Return the list of tokens
        return tokens;
    }

    // Helper methods for character classification
    private boolean isOperatorSymbol(char c) {
        String operatorChars = "+-*/%=?<>!&|^~.:-";
        boolean result = operatorChars.indexOf(c) != -1;
        return result;
    }

    private boolean isDelimiterOrBracket(char c) {
        return "(){},;[]@".indexOf(c) != -1;
    }

    private boolean isColon(char c) {
        return c == ':';
    }

    /**
     * Checks if the context is a unary context. A unary context is a location in the source code
     * where a unary operator can be used. This includes after a delimiter, a punctuation delimiter, 
     * an arithmetic operator, an assignment operator, a relational operator, a logical operator, a method operator, 
     * an inherit operator, or after a whitespace token. If the context is not a unary context, it is a binary context.
     * @return true if the context is a unary context, false otherwise
     */
    private boolean isUnaryContext() {
        if (tokens.isEmpty()) return true;

        int i = tokens.size() - 1;
        while (i >= 0 && tokens.get(i).getType() == TokenType.WHITESPACE) {
            i--;
        }
        if (i < 0) return true;

        Token lastToken = tokens.get(i);
        TokenType type = lastToken.getType();

        if (type == TokenType.INT_LIT ||
            type == TokenType.FLOAT_LIT ||
            type == TokenType.IDENTIFIER ||
            type == TokenType.KEYWORD ||
            type == TokenType.RESERVED ||
            type == TokenType.BOOL_LIT ||
            type == TokenType.NULL_LIT ||
            type == TokenType.STR_LIT ||
            type == TokenType.CHAR_LIT ||
            lastToken.getLexeme().equals(")") ||
            lastToken.getLexeme().equals("]")) {
            return false;
        }

        return true;
    }

    /**
     * Handles whitespace characters in the source code.
     * Accumulates consecutive whitespace characters and adds them as a single token
     * to the token list.
     * 
     * @param firstChar The initial whitespace character.
     * @throws SourceReader.SourceReaderException If reading from the source encounters an error.
     */
    private void handleWhitespace(char firstChar) throws SourceReader.SourceReaderException {
        int startLine = reader.getLine();
        int startColumn = reader.getColumn();

        // StringBuilder to accumulate whitespace characters
        StringBuilder whitespace = new StringBuilder();
        whitespace.append(firstChar);
        
        // Continue reading while the next character is whitespace
        while (Character.isWhitespace(reader.peek())) {
            whitespace.append(reader.readNext());
        }

        // Add the accumulated whitespace as a token
        tokens.add(new Token(TokenType.WHITESPACE, whitespace.toString(), startLine, startColumn));
    }

    /**
     * Handles delimiter and bracket characters in the source code.
     * Accumulates the character and adds it as a single token to the token list.
     * 
     * @param currentChar The delimiter or bracket character to be handled.
     */
    private void handleDelimiterOrBracket(char currentChar) throws SourceReader.SourceReaderException {
        int line = reader.getLine();
        int startColumn = reader.getColumn();
    
        if (currentChar == '[') {
            if (reader.peek() == ']') {
                // If immediately closed, treat as array type delimiters
                tokens.add(new Token(TokenType.DELIM, "[", line, startColumn));
                reader.readNext(); // Consume the closing bracket
                tokens.add(new Token(TokenType.DELIM, "]", reader.getLine(), reader.getColumn()));
            } else {
                // Otherwise, parse as a potential date or fraction literal
                StringBuilder literal = new StringBuilder();
                literal.append(currentChar);
                handleDateOrFraction(literal);
            }
        } else if (currentChar == ']') {
            errorHandler.reportError(
                ErrorHandler.ErrorType.MISMATCHED_DELIMITERS,
                "Unexpected closing bracket ']'",
                line, startColumn
            );
        } else if (Patterns.matchDelimiterOrBracket(String.valueOf(currentChar))) {
            // Handle other delimiters as individual tokens
            tokens.add(new Token(TokenType.DELIM, String.valueOf(currentChar), line, startColumn));
        } else {
            errorHandler.reportError(
                ErrorHandler.ErrorType.INVALID_DELIMITER,
                "Invalid delimiter: " + currentChar,
                line, startColumn
            );
        }        
    }    

    /**
     * Handles string literals in the source code.
     * A string literal is enclosed within either single quotes or double quotes.
     * This method reads the string and adds it to the token list.
     * It also handles escape sequences and reports errors if the string is not properly terminated.
     * 
     * @param quote The quote that starts the string literal.
     * @throws SourceReader.SourceReaderException If reading from the source encounters an error.
     */
    private void handleStringLiteral() throws SourceReader.SourceReaderException {
        int startLine = reader.getLine();
        int startColumn = reader.getColumn();
    
        // Add the opening double quote as a token
        tokens.add(new Token(TokenType.STR_DELIM, "\"", startLine, startColumn));
    
        StringBuilder stringLiteral = new StringBuilder();
        char currentChar;
        boolean terminated = false;
    
        while ((currentChar = reader.readNext()) != '"' && currentChar != SourceReader.EOF) {
            if (currentChar == '\\') {
                // Handle escape sequences
                char nextChar = reader.readNext();
                stringLiteral.append(currentChar).append(nextChar);
            } else {
                stringLiteral.append(currentChar);
            }
        }
    
        if (currentChar == '"') {
            terminated = true;
            // Add the string literal token
            tokens.add(new Token(TokenType.STR_LIT, stringLiteral.toString(), startLine, startColumn));
            // Add the closing double quote as a token
            tokens.add(new Token(TokenType.STR_DELIM, "\"", reader.getLine(), reader.getColumn()));
        }
    
        if (!terminated) {
            // Handle unterminated string literal
            errorHandler.reportError(
                ErrorType.INVALID_LITERAL,
                "Unterminated string literal",
                startLine,
                startColumn
            );
        }
    }    

    /**
     * Handles character literals in the source code.
     * 
     * This method reads a character literal enclosed in single quotes
     * and validates it. If the character literal contains an escape sequence,
     * it is handled accordingly. The complete character literal is checked
     * for proper syntax. If valid, it is added to the token list as a 
     * CHAR_LIT token. If the character literal is invalid or unterminated,
     * an error is reported to the error handler.
     * 
     * @throws SourceReader.SourceReaderException If reading from the source encounters an error.
     */
    private void handleCharLiteral() throws SourceReader.SourceReaderException {
        int startLine = reader.getLine();
        int startColumn = reader.getColumn();
    
        // Add the opening single quote as a token
        tokens.add(new Token(TokenType.STR_DELIM, "'", startLine, startColumn));
    
        StringBuilder charLiteral = new StringBuilder();
        char currentChar = reader.readNext(); // Read the character inside the single quotes
    
        if (currentChar == '\\') {
            // Handle escape sequences
            char nextChar = reader.readNext();
            charLiteral.append(currentChar).append(nextChar);
        } else {
            charLiteral.append(currentChar);
        }
    
        // Check for the closing single quote
        currentChar = reader.readNext();
        if (currentChar == '\'') {
            // Add the character literal token
            tokens.add(new Token(TokenType.CHAR_LIT, charLiteral.toString(), startLine, startColumn));
            // Add the closing single quote as a token
            tokens.add(new Token(TokenType.STR_DELIM, "'", reader.getLine(), reader.getColumn()));
        } else {
            // Handle unterminated character literal
            errorHandler.reportError(
                ErrorType.INVALID_LITERAL,
                "Unterminated character literal",
                startLine,
                startColumn
            );
        }
    }    

    /**
     * Handles identifiers or keywords in the source code.
     * Identifies and categorizes tokens as keywords, reserved words, boolean literals, or identifiers.
     * Also handles unary minus operators if encountered.
     * 
     * @param firstChar The initial character of the identifier.
     * @throws SourceReader.SourceReaderException If reading from the source encounters an error.
     */
    private void handleIdentifierOrKeyword(char firstChar) throws SourceReader.SourceReaderException {
        StringBuilder identifier = new StringBuilder();
        identifier.append(firstChar);
        int startLine = reader.getLine();
        int startColumn = reader.getColumn();

        while (Character.isLetterOrDigit(reader.peek()) || reader.peek() == '_') {
            identifier.append(reader.readNext());
        }

        String identifierStr = identifier.toString();

        // Longest-match hyphenated keywords: exit-when, switch-fall, where-type
        if (reader.peek() == '-') {
            String twoAhead = reader.peekAhead(2);
            if (!twoAhead.startsWith("->") && !twoAhead.startsWith("--")) {
                String suffix = peekHyphenatedSuffix();
                if (suffix != null) {
                    String candidate = identifierStr + suffix;
                    if (Patterns.isKeyword(candidate) || Patterns.isReservedWord(candidate)) {
                        for (int i = 0; i < suffix.length(); i++) {
                            reader.readNext();
                        }
                        identifierStr = candidate;
                    }
                }
            }
        }

        if (!identifierStr.contains("-") && !Patterns.matchIdentifier(identifierStr)) {
            errorHandler.handleInvalidIdentifier(identifierStr, startLine, startColumn);
            return;
        }

        if (Patterns.isKeyword(identifierStr)) {
            tokens.add(new Token(TokenType.KEYWORD, identifierStr, startLine, startColumn));
        } else if (Patterns.isReservedWord(identifierStr)) {
            tokens.add(new Token(TokenType.RESERVED, identifierStr, startLine, startColumn));
        } else if (identifierStr.equals("true") || identifierStr.equals("false")) {
            tokens.add(new Token(TokenType.BOOL_LIT, identifierStr, startLine, startColumn));
        } else if (identifierStr.equals("null")) {
            tokens.add(new Token(TokenType.NULL_LIT, identifierStr, startLine, startColumn));
        } else {
            tokens.add(new Token(TokenType.IDENTIFIER, identifierStr, startLine, startColumn));
        }

        char next = reader.peek();
        if (next == '-') {
            String twoAhead = reader.peekAhead(2);
            if (twoAhead.startsWith("->")) {
                reader.readNext();
                reader.readNext();
                tokens.add(new Token(TokenType.METHOD_OP, "->", reader.getLine(), reader.getColumn()));
            } else if (twoAhead.startsWith("--")) {
                reader.readNext();
                reader.readNext();
                tokens.add(new Token(TokenType.UNARY_OP, "--", reader.getLine(), reader.getColumn()));
            } else {
                handleOperator(reader.readNext());
            }
        } else if (next == '+') {
            String twoAhead = reader.peekAhead(2);
            if (twoAhead.startsWith("++")) {
                reader.readNext();
                reader.readNext();
                tokens.add(new Token(TokenType.UNARY_OP, "++", reader.getLine(), reader.getColumn()));
            } else {
                handleOperator(reader.readNext());
            }
        }
    }

    /** Peeks "-word" without consuming. Returns null if not a hyphenated word suffix. */
    private String peekHyphenatedSuffix() throws SourceReader.SourceReaderException {
        String ahead = reader.peekAhead(48);
        if (ahead.isEmpty() || ahead.charAt(0) != '-') {
            return null;
        }
        int i = 1;
        while (i < ahead.length() && (Character.isLetterOrDigit(ahead.charAt(i)) || ahead.charAt(i) == '_')) {
            i++;
        }
        if (i == 1) {
            return null;
        }
        return ahead.substring(0, i);
    }

    /**
     * Handles the operators encountered in the source code.
     * It differentiates between various operator types such as method, unary,
     * bitwise, logical, and relational operators, and adds them to the tokens list.
     * 
     * @param firstChar The initial character of the operator.
     * @throws SourceReader.SourceReaderException If reading from the source encounters an error.
     */
    private void handleOperator(char firstChar) throws SourceReader.SourceReaderException {
        StringBuilder operator = new StringBuilder();
        operator.append(firstChar);
        int startLine = reader.getLine();
        int startColumn = reader.getColumn();
    
        // Process multi-character operators in order of length
        char nextChar = reader.peek();
        
        // Handle :: as a method reference operator
        if (firstChar == ':' && reader.peek() == ':') {
            operator.append(reader.readNext());  // Consume the second ':'
            if (Patterns.matchMethodOp(operator.toString())) {
                tokens.add(new Token(TokenType.METHOD_OP, "::", startLine, startColumn));
                return;
            }
        }

        // Handle ?= assign before ternary '?'
        if (firstChar == '?') {
            if (reader.peek() == '=') {
                operator.append(reader.readNext());
                tokens.add(new Token(TokenType.ASSIGN_OP, "?=", startLine, startColumn));
                return;
            }
            tokens.add(new Token(TokenType.TERNARY_OP, "?", startLine, startColumn));
            return;
        }

        // Handle ^| as a bitwise operator
        if (firstChar == '^' && reader.peek() == '|') {
            operator.append(reader.readNext());  // Consume the '|'
            if (Patterns.matchBitwiseOp(operator.toString())) {
                tokens.add(new Token(TokenType.BIT_OP, "^|", startLine, startColumn));
                return;
            }
        }

        // Handle ^ as an arithmetic operator
        if (firstChar == '^') {
            if (Patterns.matchArithmeticOp(operator.toString())) {
                tokens.add(new Token(TokenType.ARITHMETIC_OP, "^", startLine, startColumn));
                return;
            }
        }

        // Handle >>> first (longest operator)
        if (firstChar == '>' && nextChar == '>') {
            operator.append(reader.readNext());
            if (reader.peek() == '>') {
                operator.append(reader.readNext());
                if (Patterns.matchBitwiseOp(operator.toString())) {
                    tokens.add(new Token(TokenType.BIT_OP, ">>>", startLine, startColumn));
                    return;
                }
            } else {
                if (Patterns.matchBitwiseOp(operator.toString())) {
                    tokens.add(new Token(TokenType.BIT_OP, ">>", startLine, startColumn));
                    return;
                }
            }
        }

        // Handle << operator
        if (firstChar == '<' && nextChar == '<') {
            operator.append(reader.readNext());
            if (Patterns.matchBitwiseOp(operator.toString())) {
                tokens.add(new Token(TokenType.BIT_OP, "<<", startLine, startColumn));
                return;
            }
        }

        // Handle single character bitwise operators (&, |, ^, ~)
        if ((firstChar == '&' || firstChar == '|' || firstChar == '^' || firstChar == '~') && 
            nextChar != '&' && nextChar != '|') {  // Ensure it's not &&, ||
            if (Patterns.matchBitwiseOp(operator.toString())) {
                tokens.add(new Token(TokenType.BIT_OP, operator.toString(), startLine, startColumn));
                return;
            }
        }

        // Handle || operator
        if (firstChar == '|' && nextChar == '|') {
            operator.append(reader.readNext());
            if (Patterns.matchLogicalOp(operator.toString())) {
                tokens.add(new Token(TokenType.LOG_OP, operator.toString(), startLine, startColumn));
                return;
            }
        }

        // Handle && operator
        if (firstChar == '&' && nextChar == '&') {
            operator.append(reader.readNext());
            if (Patterns.matchLogicalOp(operator.toString())) {
                tokens.add(new Token(TokenType.LOG_OP, operator.toString(), startLine, startColumn));
                return;
            }
        }

        // Handle ! operator
        if (firstChar == '!' && nextChar != '=') {
            if (Patterns.matchLogicalOp(operator.toString())) {
                tokens.add(new Token(TokenType.LOG_OP, operator.toString(), startLine, startColumn));
                return;
            }
        }

        // Compound assignment operators (+=, -=)
        if ((firstChar == '+' || firstChar == '-') && nextChar == '=') {
            operator.append(reader.readNext());
            tokens.add(new Token(TokenType.ASSIGN_OP, operator.toString(), startLine, startColumn));
            return;
        }

        // Handle -> before unary/arithmetic minus
        if (firstChar == '-' && nextChar == '>') {
            operator.append(reader.readNext());
            tokens.add(new Token(TokenType.METHOD_OP, "->", startLine, startColumn));
            return;
        }

        // Handle increment/decrement (++, --) and +/- unary vs arithmetic
        if (firstChar == '+' || firstChar == '-') {
            if (nextChar == firstChar) {
                operator.append(reader.readNext());
                tokens.add(new Token(TokenType.UNARY_OP, operator.toString(), startLine, startColumn));
                return;
            }
            if (!isUnaryContext()) {
                tokens.add(new Token(TokenType.ARITHMETIC_OP, String.valueOf(firstChar), startLine, startColumn));
            } else {
                tokens.add(new Token(TokenType.UNARY_OP, String.valueOf(firstChar), startLine, startColumn));
            }
            return;
        }

        // Handle >= operator
        if (firstChar == '>' && nextChar == '=') {
            operator.append(reader.readNext());
            tokens.add(new Token(TokenType.REL_OP, ">=", startLine, startColumn));
            return;
        }
        
        // Handle power operator
        if (firstChar == '*' && nextChar == '*') {
            operator.append(reader.readNext());
            tokens.add(new Token(TokenType.UNARY_OP, "**", startLine, startColumn));
            return;
        }

        // Handle single > operator
        if (firstChar == '>') {
            tokens.add(new Token(TokenType.REL_OP, ">", startLine, startColumn));
            return;
        }

        // Then check for <= as relational operator
        if (firstChar == '<' && nextChar == '=') {
            operator.append(reader.readNext());  // consume =
            tokens.add(new Token(TokenType.REL_OP, "<=", startLine, startColumn));
            return;
        }

        if (firstChar == '<') {
            tokens.add(new Token(TokenType.REL_OP, "<", startLine, startColumn));
            return;
        }
    
        // Handle other operators
        while (isOperatorSymbol(reader.peek())) {
            String currentOp = operator.toString() + reader.peek();
            
            if (Patterns.matchAssignOp(currentOp) || 
                Patterns.matchRelationalOp(currentOp) ||
                Patterns.matchLogicalOp(currentOp) ||
                Patterns.matchBitwiseOp(currentOp)) {
                operator.append(reader.readNext());
            } else {
                break;
            }
        }
    
        // Match the final operator
        String op = operator.toString();
        if (Patterns.matchAssignOp(op)) {
            tokens.add(new Token(TokenType.ASSIGN_OP, op, startLine, startColumn));
        } else if (Patterns.matchArithmeticOp(op)) {
            tokens.add(new Token(TokenType.ARITHMETIC_OP, op, startLine, startColumn));
        } else if (Patterns.matchRelationalOp(op)) {
            tokens.add(new Token(TokenType.REL_OP, op, startLine, startColumn));
        } else if (Patterns.matchLogicalOp(op)) {
            tokens.add(new Token(TokenType.LOG_OP, op, startLine, startColumn));
        } else if (Patterns.matchBitwiseOp(op)) {
            tokens.add(new Token(TokenType.BIT_OP, op, startLine, startColumn));
        } else if (Patterns.matchUnaryOp(op) && isUnaryContext()) {
            tokens.add(new Token(TokenType.UNARY_OP, op, startLine, startColumn));
        } else {
            errorHandler.reportError(
                ErrorType.INVALID_OPERATOR,
                "Invalid operator: " + op,
                startLine,
                startColumn
            );
        }
    }

    /**
     * Handles the colon (:) operator and its variants.
     * A single colon is a method operator, a colon followed by a greater than symbol (:) is an
     * inheritance operator, and a colon followed by two greater than symbols (:>) is also an
     * inheritance operator.
     * If more than one colon is encountered, or if the colon is not followed by a valid operator,
     * an error is reported.
     * 
     * @param firstChar The initial colon character.
     * @throws SourceReader.SourceReaderException If reading from the source encounters an error.
     */
    private void handleColon(char firstChar) throws SourceReader.SourceReaderException {
        StringBuilder operator = new StringBuilder();
        operator.append(firstChar);
        int startLine = reader.getLine();
        int startColumn = reader.getColumn();
    
        // Check if the next character forms a valid `::`
        if (reader.peek() == ':') {
            operator.append(reader.readNext());  // Consume the second colon
            if (Patterns.matchMethodOp(operator.toString())) {
                tokens.add(new Token(TokenType.METHOD_OP, "::", startLine, startColumn));
                return;
            }
        }
    
        // Handle :> or :>> as inheritance operators
        char nextChar = reader.peek();
        if (nextChar == '>') {
            operator.append(reader.readNext());
            
            // Check for :>> operator
            if (reader.peek() == '>') {
                operator.append(reader.readNext());
                if (Patterns.matchInheritOp(operator.toString())) {
                    tokens.add(new Token(TokenType.INHERIT_OP, operator.toString(), startLine, startColumn));
                    return;
                }
            } 
            // Check for :>
            else if (Patterns.matchInheritOp(operator.toString())) {
                tokens.add(new Token(TokenType.INHERIT_OP, operator.toString(), startLine, startColumn));
                return;
            }
        }
    
        // If not an operator, treat as a punctuation delimiter
        tokens.add(new Token(TokenType.PUNC_DELIM, ":", startLine, startColumn));
    }
    

    /**
     * Handles the period operator (.) and its variants.
     * A period by itself is a method operator, two periods (..) is a loop operator,
     * and three periods (...) is a loop operator.
     * If more than three consecutive periods are encountered, an error is reported.
     * 
     * @param firstChar The initial period character.
     * @throws SourceReader.SourceReaderException If reading from the source encounters an error.
     */
    // Add this specific handler for periods
    private void handlePeriods(char firstChar) throws SourceReader.SourceReaderException {
        StringBuilder symbol = new StringBuilder();
        symbol.append(firstChar); // Start with the first '.'
        int startLine = reader.getLine();
        int startColumn = reader.getColumn();
    
        // Count consecutive periods
        while (reader.peek() == '.') {
            symbol.append(reader.readNext());
            if (symbol.length() > 3) {
                // Report error for more than three periods
                errorHandler.reportError(
                    ErrorType.INVALID_OPERATOR,
                    "More than three consecutive periods are not allowed",
                    startLine, startColumn
                );
                return;
            }
        }
    
        // Validate and tokenize the operator using Patterns.matchLoopOp
        String periodOp = symbol.toString();
        if (Patterns.matchLoopOp(periodOp)) {
            tokens.add(new Token(TokenType.LOOP_OP, periodOp, startLine, startColumn));
        } else if (periodOp.equals(".")) {
            // Single period is a method operator
            tokens.add(new Token(TokenType.METHOD_OP, periodOp, startLine, startColumn));
        } else {
            // Invalid case (shouldn't occur due to length check)
            errorHandler.reportError(
                ErrorType.INVALID_OPERATOR,
                "Invalid operator: " + periodOp,
                startLine, startColumn
            );
        }
    }

    /**
     * Handles number literals.
     * It checks if the given string is a valid number, and if so, adds it to the tokens list.
     * If the string is neither a valid integer nor a valid float, an error is reported to the error handler.
     * 
     * @param firstChar The initial character indicating a number ('0' - '9')
     * @throws SourceReader.SourceReaderException If reading from the source encounters an error.
     */
    private void handleNumberLiteral(char firstChar) throws SourceReader.SourceReaderException {
        StringBuilder number = new StringBuilder();
        number.append(firstChar); // Start with the first digit
        int startLine = reader.getLine();
        int startColumn = reader.getColumn();
    
        boolean isFloat = false;
    
        // Read digits and decimal points
        while (Character.isDigit(reader.peek()) || reader.peek() == '.') {
            char nextChar = reader.peek();
    
            // Range operators: emit number first, then .. / ...
            if (nextChar == '.' && reader.peekAhead(2).startsWith("..")) {
                break;
            }

            if (nextChar == '.') {
                if (isFloat) break; // Already a float, terminate number parsing
                isFloat = true;
            }
            number.append(reader.readNext());
        }

        // Validate the number (float or integer)
        String numberStr = number.toString();
        if (isFloat) {
            if (Patterns.matchFloat(numberStr)) {
                tokens.add(new Token(TokenType.FLOAT_LIT, numberStr, startLine, startColumn));
            } else {
                errorHandler.reportError(
                    ErrorType.INVALID_NUMBER_FORMAT,
                    "Invalid number format: " + numberStr,
                    startLine, startColumn
                );
            }
        } else {
            if (Patterns.matchInteger(numberStr)) {
                tokens.add(new Token(TokenType.INT_LIT, numberStr, startLine, startColumn));
            } else {
                errorHandler.reportError(
                    ErrorType.INVALID_NUMBER_FORMAT,
                    "Invalid number format: " + numberStr,
                    startLine, startColumn
                );
            }
        }

        // Check if loop operators follow the number
        if (reader.peek() == '.') {
            handlePeriods(reader.readNext());
        }
    }

    /**
     * Handles date and fraction literals.
     * It checks if the given string is a valid date or fraction, and if so, adds it to the tokens list.
     * If the string is neither a valid date nor a valid fraction, an error is reported to the error handler.
     * 
     * @param value The string to be checked for a valid date or fraction.
     * @throws SourceReader.SourceReaderException If reading from the source encounters an error.
     */
    private void handleDateOrFraction(StringBuilder value) throws SourceReader.SourceReaderException {
        int startLine = reader.getLine();
        int startColumn = reader.getColumn();
    
        // Read the literal until the closing bracket or EOF
        while (reader.peek() != ']' && reader.peek() != SourceReader.EOF) {
            char next = reader.readNext();
            if (!Character.isDigit(next) && next != '|') {
                errorHandler.reportError(
                    ErrorType.INVALID_LITERAL,
                    "Invalid character in literal: " + next,
                    startLine, reader.getColumn()
                );
                return;
            }
            value.append(next);
        }
    
        // Add closing bracket if present
        if (reader.peek() == ']') {
            value.append(reader.readNext());
            String literal = value.toString();
    
            // Validate the literal format
            if (literal.chars().filter(ch -> ch == '|').count() == 2 && Patterns.matchDateLiteral(literal)) {
                tokens.add(new Token(TokenType.DATE_LIT, literal, startLine, startColumn));
            } else if (literal.chars().filter(ch -> ch == '|').count() == 1 && Patterns.matchFractionLiteral(literal)) {
                tokens.add(new Token(TokenType.FRAC_LIT, literal, startLine, startColumn));
            } else {
                errorHandler.reportError(
                    ErrorType.INVALID_LITERAL,
                    "Unrecognized format: " + literal,
                    startLine, startColumn
                );
            }
        } else {
            errorHandler.reportError(
                ErrorType.MISMATCHED_DELIMITERS,
                "Missing closing bracket for: " + value.toString(),
                startLine, startColumn
            );
        }
    }    

    /**
     * Handles complex literals.
     * Complex literals are defined as a string of the format $(real,imag),
     * where real and imag are numbers. The method checks the validity of the
     * complex literal and adds it to the token list if it is valid.
     * 
     * @throws SourceReaderException 
     */
    private void handleComplexLiteral(char firstChar) throws SourceReader.SourceReaderException {
        StringBuilder complex = new StringBuilder();
        complex.append(firstChar);
        int startLine = reader.getLine();
        int startColumn = reader.getColumn();
    
        if (reader.peek() == '(') {
            complex.append(reader.readNext());
            int commaCount = 0;
            boolean valid = true;
            
            while (reader.peek() != ')' && reader.peek() != SourceReader.EOF) {
                char current = reader.readNext();
                if (current == ',') {
                    commaCount++;
                    if (commaCount > 1) {
                        valid = false;
                        break;
                    }
                } else if (!Character.isDigit(current) && current != '-' && current != '.') {
                    valid = false;
                    break;
                }
                complex.append(current);
            }
    
            if (reader.peek() == ')') {
                complex.append(reader.readNext());
                if (valid && commaCount == 1 && Patterns.matchComplexLiteral(complex.toString())) {
                    tokens.add(new Token(TokenType.COMP_LIT, complex.toString(), startLine, startColumn));
                    return;
                }
            } else {
                // Skip junk until ')' so invalid complex does not leak trailing tokens
                while (reader.peek() != ')' && reader.peek() != SourceReader.EOF && reader.peek() != '\n') {
                    reader.readNext();
                }
                if (reader.peek() == ')') {
                    reader.readNext();
                }
            }

            errorHandler.handleInvalidComplexLiteral(complex.toString(), startLine, startColumn);
        } else {
            errorHandler.reportError(
                ErrorType.INVALID_COMPLEX_LITERAL,
                "Complex literal must start with $(",
                startLine,
                startColumn,
                "Use format $(real,imag)"
            );
        }
    }

    /**
     * Handles both single-line and multi-line comments.
     * Adds the comment as a token if properly terminated.
     * 
     * @param firstChar The initial character indicating a comment ('/')
     * @throws SourceReader.SourceReaderException If reading from the source encounters an error
     */
    private void handleComment(char firstChar) throws SourceReader.SourceReaderException {
        StringBuilder comment = new StringBuilder();
        comment.append(firstChar);
        int startLine = reader.getLine();
        int startColumn = reader.getColumn();
    
        // Check for single-line comment
        if (reader.peek() == '/') { 
            comment.append(reader.readNext());  // add the second '/'
            
            // Read until the end of the line or EOF
            while (reader.peek() != '\n' && reader.peek() != SourceReader.EOF) {
                comment.append(reader.readNext());
            }
    
            // Validate single-line comment format
            if (Patterns.isSingleLineComment(comment.toString())) {
                tokens.add(new Token(TokenType.COMMENT, comment.toString(), startLine, startColumn));
            } else {
                handleOperator('/');
            }
        }
        // Check for multi-line comment
        else if (reader.peek() == '*') { 
            comment.append(reader.readNext());  // add the initial '*'
            boolean terminated = false;
            
            // Read until the comment is terminated or EOF
            while (!terminated && reader.peek() != SourceReader.EOF) {
                char current = reader.readNext();
                comment.append(current);
                
                // Check for comment termination sequence '*/'
                if (current == '*' && reader.peek() == '/') {
                    comment.append(reader.readNext());
                    terminated = true;
                    tokens.add(new Token(TokenType.COMMENT, comment.toString(), startLine, startColumn));
                }
            }
            
            // Handle unterminated multi-line comment error
            if (!terminated) {
                errorHandler.handleUnterminatedComment(startLine, startColumn);
            }
        } 
        // If not a comment, treat as division operator
        else {
            handleOperator('/');
        }
    }

    /**
     * Handles object delimiters and operators, differentiating between object type declarations
     * and operator usage. Recognizes "<type>" for object delimiters and handles operators
     * like '<' and '>'.
     *
     * @param firstChar The first character to check ('<' or '>').
     * @throws SourceReader.SourceReaderException If reading from the source encounters an error.
     */
    private void handleObjectDelimiterOrOperator(char firstChar) throws SourceReader.SourceReaderException {
        int startLine = reader.getLine();
        int startColumn = reader.getColumn();

        if (firstChar == '<') {
            String ahead = reader.peekAhead(64);
            int i = 0;
            while (i < ahead.length() && (Character.isLetterOrDigit(ahead.charAt(i)) || ahead.charAt(i) == '_')) {
                i++;
            }
            if (i > 0 && i < ahead.length() && ahead.charAt(i) == '>') {
                String candidate = "<" + ahead.substring(0, i + 1);
                if (Patterns.matchObjectDelimiter(candidate)) {
                    for (int n = 0; n < i + 1; n++) {
                        reader.readNext();
                    }
                    String typeName = ahead.substring(0, i);
                    tokens.add(new Token(TokenType.OBJ_DELIM, "<", startLine, startColumn));
                    tokens.add(new Token(TokenType.IDENTIFIER, typeName, startLine, startColumn));
                    tokens.add(new Token(TokenType.OBJ_DELIM, ">", reader.getLine(), reader.getColumn()));
                    return;
                }
            }
            handleOperator(firstChar);
        } else if (firstChar == '>') {
            handleOperator(firstChar);
        }
    }
}