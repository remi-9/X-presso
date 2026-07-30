package com.xpresso.analyzer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.xpresso.lexer.Lexer;
import com.xpresso.lexer.Token;
import com.xpresso.lexer.TokenType;
import com.xpresso.parser.ParseNode;
import com.xpresso.parser.Parser;
import com.xpresso.util.ErrorHandler;
import com.xpresso.util.SourceReader;
import com.xpresso.util.SyntaxErrorHandler;

/**
 * Shared lex + parse facade for CLI and web API.
 */
public class Analyzer {

    public static class Result {
        public final List<Token> tokens;
        public final List<Token> filteredTokens;
        public final List<Map<String, Object>> lexicalErrors;
        public final List<Map<String, Object>> syntaxErrors;
        public final ParseNode parseTree;
        public final boolean ok;

        Result(List<Token> tokens, List<Token> filteredTokens,
               List<Map<String, Object>> lexicalErrors,
               List<Map<String, Object>> syntaxErrors,
               ParseNode parseTree) {
            this.tokens = tokens;
            this.filteredTokens = filteredTokens;
            this.lexicalErrors = lexicalErrors;
            this.syntaxErrors = syntaxErrors;
            this.parseTree = parseTree;
            this.ok = lexicalErrors.isEmpty() && syntaxErrors.isEmpty();
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("ok", ok);
            map.put("tokens", tokens.stream().map(Analyzer::tokenToMap).collect(Collectors.toList()));
            map.put("lexicalErrors", lexicalErrors);
            map.put("syntaxErrors", syntaxErrors);
            map.put("parseTree", parseTree != null ? parseTree.toMap() : null);
            return map;
        }
    }

    public enum Mode {
        LEX, PARSE
    }

    public static Result analyzeFile(String path, Mode mode) throws Exception {
        try (SourceReader reader = new SourceReader(path, java.nio.charset.StandardCharsets.UTF_8)) {
            return analyze(reader, mode);
        }
    }

    public static Result analyzeSource(String source, Mode mode) throws Exception {
        try (SourceReader reader = new SourceReader(source, "<editor>")) {
            return analyze(reader, mode);
        }
    }

    private static Result analyze(SourceReader reader, Mode mode) {
        Lexer lexer = new Lexer(reader);
        List<Token> tokens = lexer.tokenize();
        List<Map<String, Object>> lexErrors = toLexicalErrorMaps(lexer.getErrorHandler());

        List<Token> filtered = tokens.stream()
            .filter(t -> t.getType() != TokenType.WHITESPACE && t.getType() != TokenType.COMMENT)
            .collect(Collectors.toList());

        ParseNode tree = null;
        List<Map<String, Object>> synErrors = new ArrayList<>();

        if (mode == Mode.PARSE) {
            Parser parser = new Parser(filtered);
            tree = parser.parseSilent();
            synErrors = toSyntaxErrorMaps(parser.getErrorHandler());
        }

        return new Result(tokens, filtered, lexErrors, synErrors, tree);
    }

    private static Map<String, Object> tokenToMap(Token t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", t.getType().name());
        m.put("lexeme", t.getLexeme());
        m.put("line", t.getLine());
        m.put("column", t.getColumn());
        return m;
    }

    private static List<Map<String, Object>> toLexicalErrorMaps(ErrorHandler handler) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ErrorHandler.LexicalError e : handler.getErrors()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("kind", "lexical");
            m.put("type", e.getType().name());
            m.put("message", e.getMessage());
            m.put("line", e.getLine());
            m.put("column", e.getColumn());
            m.put("suggestion", e.getSuggestion());
            list.add(m);
        }
        return list;
    }

    private static List<Map<String, Object>> toSyntaxErrorMaps(SyntaxErrorHandler handler) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (SyntaxErrorHandler.SyntaxError e : handler.getErrors()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("kind", "syntax");
            m.put("message", e.getMessage());
            m.put("line", e.getLine());
            m.put("column", e.getColumn());
            m.put("suggestion", e.getSuggestion());
            list.add(m);
        }
        return list;
    }
}
