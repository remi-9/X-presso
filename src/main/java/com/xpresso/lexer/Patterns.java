package com.xpresso.lexer;

import com.xpresso.language.SpecialWords;

/**
 * Patterns class containing regular expressions and matching methods
 * for various components of the S-presso programming language.
 */
public class Patterns {

    private static final SpecialWords specialWords = new SpecialWords();

    // Identifiers: Starts with a letter, followed by letters, digits, or underscores
    private static final String IDENTIFIER_REGEX = "^[a-zA-Z][a-zA-Z0-9_]*$";

    // Assignment Operators: =, +=, -=, *=, /=, %=, ?=
    private static final String ASSIGN_OP_REGEX = "^(=|\\+=|-=|\\*=|/=|%=|\\?=)$";

    // Arithmetic Operators: +, -, *, /, %, ^
    private static final String ARITHMETIC_OP_REGEX = "^(\\+|-|\\*|/|%|\\^)$";

    // Logical Operators: ||, &&, !
    private static final String LOGICAL_OP_REGEX = "^(\\|\\||&&|!)$";

    // Relational Operators: ==, !=, <, >, <=, >=
    private static final String RELATIONAL_OP_REGEX = "^(==|!=|<=|>=|<|>)$";

    // Bitwise Operators: &, |, ^|, ~, <<, >>, >>>
    private static final String BITWISE_OP_REGEX = "^(>>>|>>|<<|\\^\\||\\&|\\||\\^|~)$";

    // Unary Operators: +, -, ++, --, **, !
    private static final String UNARY_OP_REGEX = "^(\\+|-|\\+\\+|\\-\\-|\\*\\*|!)$";

    // Ternary Operator: ? :
    private static final String TERNARY_OP_REGEX = "^\\?\\s*:\\s*$";

    // Method Reference Operators: ., ::, ->
    private static final String METHOD_OP_REGEX = "^(->|::|\\.)$";

    // Inherit Operators: :>, :>>
    private static final String INHERIT_OP_REGEX = "^(\\:>|\\:>>)$";

    // Loop Operators: .., ...
    private static final String LOOP_OP_REGEX = "^(\\.\\.\\.?|\\.\\.)$";

    // Use SpecialWords to dynamically generate regex for keywords and reserved words
    private static final String KEYWORDS_REGEX = "^(?:" + specialWords.getKeywordsRegex() + ")$";
    private static final String RESERVED_WORDS_REGEX = "^(?:" + specialWords.getReservedWordsRegex() + ")$";

    // Single-Line Comments: Starts with //
    private static final String SINGLE_LINE_COMMENT_REGEX = "^//[^\\n]*$";

    // Delimiters and Brackets: (), {}, [], ,, ;, :, @, .
    private static final String DELIMITERS_REGEX = "^(\\(|\\)|\\{|\\}|\\[|\\]|,|;|:|@|\\.)$";

    // String Literals: Starts and ends with "
    private static final String STRING_LITERAL_REGEX = "^\"([^\"\\\\]|\\\\.)*\"$";

    // Character Literals: Starts and ends with '
    private static final String CHARACTER_LITERAL_REGEX = "^'([^'\\\\]|\\\\.)*'$";

    // Object Delimiters: Starts and ends with <>
    private static final String OBJECT_DELIMITER_REGEX = "^<[a-zA-Z][a-zA-Z0-9_]*>$";

    // Number Literals: Integers and Floats
    private static final String INTEGER_REGEX = "^[-+]?\\d+$";
    private static final String FLOAT_REGEX = "^[-+]?\\d*\\.\\d+([eE][-+]?\\d+)?$";

    // Complex Literals: $(real,imag)
    private static final String COMPLEX_LITERAL_REGEX = 
                    "^\\$\\(\\s*[-+]?\\d*\\.?\\d+\\s*,\\s*[-+]?\\d*\\.?\\d+\\s*\\)$";

    // Fraction Literals: [numerator|denominator]
    private static final String FRACTION_LITERAL_REGEX = "^\\[\\d+\\|[1-9]\\d*]$";

    // Date Literals: [YYYY|MM|DD]
    private static final String DATE_LITERAL_REGEX = "^\\[\\d{4}\\|\\d{2}\\|\\d{2}]$";

    // Methods for Matching
    public static boolean matchIdentifier(String input) {
        return input != null && input.matches(IDENTIFIER_REGEX);
    }

    public static boolean matchAssignOp(String input) {
        return input != null && input.matches(ASSIGN_OP_REGEX);
    }

    public static boolean matchArithmeticOp(String input) {
        return input != null && input.matches(ARITHMETIC_OP_REGEX);
    }

    public static boolean matchLogicalOp(String input) {
        return input != null && input.matches(LOGICAL_OP_REGEX);
    }

    public static boolean matchRelationalOp(String input) {
        return input != null && input.matches(RELATIONAL_OP_REGEX);
    }

    public static boolean matchBitwiseOp(String input) {
        return input != null && input.matches(BITWISE_OP_REGEX);
    }

    public static boolean matchUnaryOp(String input) {
        return input != null && input.matches(UNARY_OP_REGEX);
    }

    public static boolean matchTernaryOp(String input) {
        return input != null && input.matches(TERNARY_OP_REGEX);
    }

    public static boolean matchMethodOp(String input) {
        return input != null && input.matches(METHOD_OP_REGEX);
    }

    public static boolean matchInheritOp(String input) {
        return input != null && input.matches(INHERIT_OP_REGEX);
    }

    public static boolean matchLoopOp(String input) {
        return input != null && input.matches(LOOP_OP_REGEX);
    }

    public static boolean isKeyword(String input) {
        return input != null && input.matches(KEYWORDS_REGEX);
    }

    public static boolean isReservedWord(String input) {
        return input != null && input.matches(RESERVED_WORDS_REGEX);
    }

    public static boolean isSingleLineComment(String input) {
        return input != null && input.matches(SINGLE_LINE_COMMENT_REGEX);
    }

    public static boolean matchDelimiterOrBracket(String input) {
        return input != null && input.matches(DELIMITERS_REGEX);
    }

    public static boolean matchStringLiteral(String input) {
        return input != null && input.matches(STRING_LITERAL_REGEX);
    }

    public static boolean matchCharacterLiteral(String input) {
        return input != null && input.matches(CHARACTER_LITERAL_REGEX);
    }

    public static boolean matchObjectDelimiter(String input) {
        return input != null && input.matches(OBJECT_DELIMITER_REGEX);
    }

    public static boolean matchInteger(String input) {
        return input != null && input.matches(INTEGER_REGEX);
    }
    
    public static boolean matchFloat(String input) {
        return input != null && input.matches(FLOAT_REGEX);
    }

    public static boolean matchComplexLiteral(String input) {
        return input != null && input.matches(COMPLEX_LITERAL_REGEX);
    }

    public static boolean matchFractionLiteral(String input) {
        return input != null && input.matches(FRACTION_LITERAL_REGEX);
    }

    public static boolean matchDateLiteral(String input) {
        return input != null && input.matches(DATE_LITERAL_REGEX);
    }
}
