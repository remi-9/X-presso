package com.xpresso.lexer;

/**
 * Represents a lexical token produced by the Lexer.
 * Each token has a type, value, and its position in the source code.
 */
public class Token {
    private final TokenType type;  // The type of the token (e.g., IDENTIFIER, KEYWORD)
    private final String lexeme;   // The actual value of the token
    private final int line;       // Line number where the token was found
    private final int column;     // Column number where the token starts

     /**
          * Constructs a Token with the specified type, value, line, and column.
          *
          * @param type   the type of the token
          * @param lexeme  the lexeme of the token
          * @param line   the line number of the token
          * @param column the column number of the token
          */
     public Token(TokenType type, String lexeme, int line, int column) {
          this.type = type;
          this.lexeme = lexeme;
          this.line = line;
          this.column = column;
     }

     public TokenType getType() {
          return this.type;
     }

     public String getLexeme() {
          return this.lexeme;
     }

     public int getLine() {
          return this.line;
     }

     public int getColumn() {
          return this.column;
     }

     /**
          * Returns a string representation of the token formatted for column-aligned output.
          * Includes headers for better readability.
          *
          * @return formatted string representation
          */
     @Override
     public String toString() {
          String sanitizedLexeme = lexeme.replace("\n", "\\n").replace("\r", "");
          return String.format("%-20s %-15s Line: %-3d Column: %-3d",
                                   type, sanitizedLexeme, line, column);
     }

     /**
          * Prints a header for the token output, to be displayed before token rows.
          *
          * @return header string
          */
     public static String header() {
          return String.format("%-20s %-15s %-10s %-10s",
                         "TOKEN TYPE", "LEXEME", "LINE", "COLUMN");
     }
}
