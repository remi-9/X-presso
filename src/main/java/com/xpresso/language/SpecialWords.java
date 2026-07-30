package com.xpresso.language;
/**
 * Manages the keywords and reserved words of the language. 
 * Provides utilities to check if a string is a keyword or a reserved word 
 * to differentiate identifiers from language constructs.
 */

public class SpecialWords {
    private final String[] reservedWords = {"abstract", "after", "ALIAS", "before", "bool", "byte", "char", "class",
            "Complex", "Date", "double", "exclude", "export_as", "Frac", "filter_by", "final", "float", "inline_query",
            "inspect", "int", "long", "main", "modify", "native", "private", "protected", "public", "Rational", "return",
            "short", "static", "STRICT", "strictfp", "str", "today", "toMixed", "transient", "validate", "volatile", "isValid"};
    private final String[] keywords = {"break", "case", "day", "default", "do", "else", "exit", "exit-when", "for",
            "from", "get", "having", "if", "in", "Input", "limit", "month", "order_by", "Output", "print", "select",
            "switch", "switch-fall", "System", "void", "while", "where", "where-type", "year"};

    private static final String noiseWord = "general";

    public boolean isKeyword(String lexeme) {
        for (String keyword : keywords) {
            if (lexeme.equals(keyword)) {
                return true;
            }
        }
        return false;
    }

    public boolean isReservedWord(String lexeme) {
        for (String reservedWord : reservedWords) {
            if (lexeme.equals(reservedWord)) {
                return true;
            }
        }
        return false;
    }

    public boolean isNoiseWord(String lexeme) {
        return lexeme.equals(noiseWord);
    }

    // New methods to expose keywords and reserved words as regex-compatible strings
    public String getKeywordsRegex() {
        return String.join("|", keywords);
    }

    public String getReservedWordsRegex() {
        return String.join("|", reservedWords);
    }
}