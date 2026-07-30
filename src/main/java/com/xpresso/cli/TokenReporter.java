package com.xpresso.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.xpresso.analyzer.Analyzer;
import com.xpresso.lexer.Token;
import com.xpresso.lexer.TokenType;

/**
 * Formats analysis results for console or file output.
 */
public final class TokenReporter {
    private static final String OUTPUT_DIR = "output";

    private TokenReporter() {}

    public static void printSummary(List<Token> tokens) {
        Map<TokenType, Long> counts = tokens.stream()
            .filter(t -> t.getType() != TokenType.WHITESPACE
                && t.getType() != TokenType.COMMENT
                && t.getType() != TokenType.EOF)
            .collect(Collectors.groupingBy(Token::getType, Collectors.counting()));

        System.out.println("\nToken summary:");
        counts.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e -> System.out.printf("  %-20s %d%n", e.getKey(), e.getValue()));
    }

    public static void report(Analyzer.Result result, CliOptions options) throws IOException {
        List<Token> tokens = options.verbose ? result.tokens : result.filteredTokens;

        if (options.outputToFile) {
            writeToFile(tokens, options);
        } else if ("json".equals(options.outputFormat)) {
            printJson(tokens);
        } else {
            printText(tokens);
        }
        printSummary(result.tokens);

        if (!result.lexicalErrors.isEmpty()) {
            System.err.println("\nLexical errors:");
            for (Map<String, Object> e : result.lexicalErrors) {
                System.err.printf("  L%s:C%s %s — %s%n",
                    e.get("line"), e.get("column"), e.get("type"), e.get("message"));
            }
        }
        if (!result.syntaxErrors.isEmpty()) {
            System.err.println("\nSyntax errors:");
            for (Map<String, Object> e : result.syntaxErrors) {
                System.err.printf("  L%s:C%s %s%n",
                    e.get("line"), e.get("column"), e.get("message"));
                if (e.get("suggestion") != null) {
                    System.err.printf("    suggestion: %s%n", e.get("suggestion"));
                }
            }
        } else if (options.lexOnly) {
            System.out.println("\n(lex-only — parser skipped)");
        } else if (result.lexicalErrors.isEmpty()) {
            System.out.println("\nNo syntax errors found.");
        }
    }

    private static void printText(List<Token> tokens) {
        System.out.println(Token.header());
        System.out.println("-".repeat(64));
        for (Token t : tokens) {
            if (t.getType() == TokenType.EOF) continue;
            System.out.println(t);
        }
    }

    private static void printJson(List<Token> tokens) {
        System.out.println("[");
        List<Token> list = tokens.stream()
            .filter(t -> t.getType() != TokenType.EOF)
            .collect(Collectors.toList());
        for (int i = 0; i < list.size(); i++) {
            Token t = list.get(i);
            System.out.printf(
                "  {\"type\":\"%s\",\"lexeme\":%s,\"line\":%d,\"column\":%d}%s%n",
                t.getType(),
                jsonString(t.getLexeme()),
                t.getLine(),
                t.getColumn(),
                i + 1 < list.size() ? "," : ""
            );
        }
        System.out.println("]");
    }

    private static void writeToFile(List<Token> tokens, CliOptions options) throws IOException {
        Path dir = Paths.get(OUTPUT_DIR);
        Files.createDirectories(dir);
        String base = Paths.get(options.filePath).getFileName().toString()
            .replaceAll("\\.\\w+$", "");
        String ext = "json".equals(options.outputFormat) ? ".json" : ".txt";
        Path out = dir.resolve(base + "_output" + ext);

        StringBuilder sb = new StringBuilder();
        if ("json".equals(options.outputFormat)) {
            sb.append("[\n");
            List<Token> list = tokens.stream()
                .filter(t -> t.getType() != TokenType.EOF)
                .collect(Collectors.toList());
            for (int i = 0; i < list.size(); i++) {
                Token t = list.get(i);
                sb.append(String.format(
                    "  {\"type\":\"%s\",\"lexeme\":%s,\"line\":%d,\"column\":%d}%s\n",
                    t.getType(), jsonString(t.getLexeme()), t.getLine(), t.getColumn(),
                    i + 1 < list.size() ? "," : ""
                ));
            }
            sb.append("]\n");
        } else {
            sb.append(Token.header()).append('\n').append("-".repeat(64)).append('\n');
            for (Token t : tokens) {
                if (t.getType() == TokenType.EOF) continue;
                sb.append(t).append('\n');
            }
        }
        Files.writeString(out, sb.toString());
        System.out.println("Wrote " + out.toAbsolutePath());
    }

    private static String jsonString(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.append('"').toString();
    }
}
