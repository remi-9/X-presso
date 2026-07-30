package com.xpresso.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.xpresso.analyzer.Analyzer;
import com.xpresso.util.SourceReader;

class LexerFixesTest {

    private List<Token> lex(String source) throws Exception {
        try (SourceReader reader = new SourceReader(source, "<test>")) {
            return new Lexer(reader).tokenize().stream()
                .filter(t -> t.getType() != TokenType.WHITESPACE && t.getType() != TokenType.COMMENT)
                .collect(Collectors.toList());
        }
    }

    @Test
    void rangeKeepsLeftNumber() throws Exception {
        List<Token> tokens = lex("5..10");
        assertEquals(TokenType.INT_LIT, tokens.get(0).getType());
        assertEquals("5", tokens.get(0).getLexeme());
        assertEquals(TokenType.LOOP_OP, tokens.get(1).getType());
        assertEquals("10", tokens.get(2).getLexeme());
    }

    @Test
    void nullishAssignIsAssignOp() throws Exception {
        List<Token> tokens = lex("b ?= null;");
        assertTrue(tokens.stream().anyMatch(t ->
            t.getType() == TokenType.ASSIGN_OP && t.getLexeme().equals("?=")));
        assertTrue(tokens.stream().anyMatch(t ->
            t.getType() == TokenType.NULL_LIT && t.getLexeme().equals("null")));
    }

    @Test
    void hyphenatedKeywordStaysOneToken() throws Exception {
        List<Token> tokens = lex("exit-when");
        assertEquals(TokenType.KEYWORD, tokens.get(0).getType());
        assertEquals("exit-when", tokens.get(0).getLexeme());
    }

    @Test
    void validClassParses() throws Exception {
        Analyzer.Result result = Analyzer.analyzeSource(
            "public class Demo :> Base :>> A, B { }",
            Analyzer.Mode.PARSE
        );
        assertTrue(result.syntaxErrors.isEmpty(), result.syntaxErrors.toString());
        assertEquals("Program", result.parseTree.getKind());
    }

    @Test
    void invalidIdentFixtureHasLexicalError() throws Exception {
        Path file = Path.of("test/invalid/lex-ident-starts-digit.txt");
        if (!Files.exists(file)) {
            return; // skip if cwd wrong
        }
        Analyzer.Result result = Analyzer.analyzeFile(file.toString(), Analyzer.Mode.LEX);
        assertFalse(result.lexicalErrors.isEmpty());
    }
}
