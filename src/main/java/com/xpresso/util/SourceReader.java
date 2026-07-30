package com.xpresso.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Handles reading the source code, character by character, while keeping track 
 * of the current line and column position. 
 * Provides methods for character peeking, advancing, and error recovery.
 */
public class SourceReader implements AutoCloseable {
    /**
     * Custom exception class for SourceReader specific errors.
     */
    public static class SourceReaderException extends IOException {
        private static final long serialVersionUID = 1L;
        
        public SourceReaderException(String message) {
            super(message);
        }

        public SourceReaderException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static final char EOF = (char) -1;

    private final BufferedReader reader;
    private final String filePath;
    private int line = 1;
    private int column = 0;
    private char lastChar = EOF;
    private long fileSize;
    private long bytesRead = 0;
    private boolean fileEnded = false;

    /**
     * Constructs a SourceReader over an in-memory source string (web / API use).
     */
    public SourceReader(String source, String displayName) throws SourceReaderException {
        this.filePath = displayName != null ? displayName : "<source>";
        try {
            byte[] bytes = source.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            this.fileSize = bytes.length;
            this.reader = new BufferedReader(new InputStreamReader(
                new java.io.ByteArrayInputStream(bytes),
                java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new SourceReaderException("Error initializing in-memory reader", e);
        }
    }

    /**
     * Constructs a new SourceReader instance from a file path.
     */
    public SourceReader(String filePath, Charset charset) throws SourceReaderException {
        this.filePath = filePath;
        
        try {
            // Validate file existence and readability
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                throw new SourceReaderException("File does not exist: " + filePath);
            }
            if (!Files.isReadable(path)) {
                throw new SourceReaderException("File is not readable: " + filePath);
            }
            
            // Get file size for progress tracking
            this.fileSize = Files.size(path);
            
            // Initialize reader
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), charset));
        } catch (IOException e) {
            throw new SourceReaderException("Error initializing reader for file: " + filePath, e);
        }
    }

    /**
     * Reads the next character from the file and advances the position.
     * 
     * @return the next character, or EOF if the end of the file is reached
     * @throws SourceReaderException if an error occurs while reading the file
     */
    public char readNext() throws SourceReaderException {
        try {
            int read = reader.read();
            if (read == -1) {
                lastChar = EOF;
                fileEnded = true;
                return EOF;
            }

            bytesRead++;
            lastChar = (char) read;

            if (lastChar == '\n') {
                line++;
                column = 0;
            } else {
                column++;
            }

            return lastChar;
        } catch (IOException e) {
            throw new SourceReaderException("Error reading from file at position " + bytesRead, e);
        }
    }

    /**
     * Peeks at the next character in the file without advancing the position.
     * 
     * @return the next character, or EOF if the end of the file is reached
     * @throws SourceReaderException if an error occurs while reading the file
     */
    public char peek() throws SourceReaderException {
        try {
            reader.mark(1);
            int read = reader.read();
            reader.reset();
            return read == -1 ? EOF : (char) read;
        } catch (IOException e) {
            throw new SourceReaderException(
                "Error peeking at file at line " + line + ", column " + column, e);
        }
    }

    /**
     * Peeks multiple characters ahead without advancing the position.
     * 
     * @param count number of characters to peek ahead
     * @return string containing the peeked characters
     * @throws SourceReaderException if an error occurs while reading the file
     */
    public String peekAhead(int count) throws SourceReaderException {
        if (count <= 0) {
            return "";
        }

        try {
            reader.mark(count);
            char[] chars = new char[count];
            int read = reader.read(chars, 0, count);
            reader.reset();

            if (read == -1) {
                return String.valueOf(EOF);
            }

            return new String(chars, 0, read);
        } catch (IOException e) {
            throw new SourceReaderException(
                "Error peeking ahead " + count + " characters at line " + line + ", column " + column, e);
        }
    }

    /**
     * Skips characters until a condition is met.
     * 
     * @param condition function that determines when to stop skipping
     * @throws SourceReaderException if an error occurs while reading the file
     */
    public void skipUntil(CharPredicate condition) throws SourceReaderException {
        char current;
        while ((current = peek()) != EOF && !condition.test(current)) {
            readNext();
        }
    }

    /**
     * Reads characters into a StringBuilder until a condition is met.
     * 
     * @param condition function that determines when to stop reading
     * @return StringBuilder containing the read characters
     * @throws SourceReaderException if an error occurs while reading the file
     */
    public StringBuilder readUntil(CharPredicate condition) throws SourceReaderException {
        StringBuilder builder = new StringBuilder();
        char current;
        while ((current = peek()) != EOF && !condition.test(current)) {
            builder.append(readNext());
        }
        return builder;
    }

    // Rest of the helper methods (they don't throw exceptions)
    public String getCurrentPosition() {
        return String.format("line %d, column %d", line, column);
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getFilePath() {
        return filePath;
    }

    public char getLastChar() {
        return lastChar;
    }

    public int getProgress() {
        if (fileSize == 0) return 100;
        return (int) ((bytesRead * 100) / fileSize);
    }

    public boolean isFileEnded() {
        return fileEnded;
    }

    /**
     * Closes the underlying reader.
     * 
     * @throws SourceReaderException if an error occurs while closing the reader
     */
    @Override
    public void close() throws SourceReaderException {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                throw new SourceReaderException("Error closing reader", e);
            }
        }
    }

    /**
     * Functional interface for character predicates used in skipUntil and readUntil.
     */
    @FunctionalInterface
    public interface CharPredicate {
        boolean test(char c);
    }
}