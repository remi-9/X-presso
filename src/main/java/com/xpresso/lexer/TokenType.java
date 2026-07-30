package com.xpresso.lexer;

/**
 * Enum representing the various types of tokens recognized by the Lexer.
 */
public enum TokenType {
     // Identifiers
     IDENTIFIER,

     // Keywords and Reserved Words
     KEYWORD,
     RESERVED,

     // Operators
     ARITHMETIC_OP,
     ASSIGN_OP,
     REL_OP,
     LOG_OP,
     UNARY_OP,
     BIT_OP,
     METHOD_OP,
     LOOP_OP,
     INHERIT_OP,
     TERNARY_OP,

     // Literals
     INT_LIT,
     FLOAT_LIT,
     STR_LIT,
     BOOL_LIT,
     CHAR_LIT,
     NULL_LIT,
     FRAC_LIT,
     DATE_LIT,
     COMP_LIT,

     // Delimiters
     DELIM,
     PUNC_DELIM,
     STR_DELIM,
     OBJ_DELIM,

     // Comments
     COMMENT,

     // Whitespace
     WHITESPACE,

     // Escape Characters
     ESCAPE_CHAR,

     // End of File
     EOF,
     UNKNOWN
}
