package com.xpresso.server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import com.xpresso.analyzer.Analyzer;

/**
 * Lightweight web server: static UI + POST /api/analyze
 */
public class WebServer {
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) throws Exception {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        Path webRoot = resolveWebRoot();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/analyze", WebServer::handleAnalyze);
        server.createContext("/", exchange -> serveStatic(exchange, webRoot));

        server.setExecutor(null);
        server.start();
        System.out.println("X-presso web UI: http://localhost:" + port + "/");
        System.out.println("Serving static files from: " + webRoot.toAbsolutePath());
    }

    private static Path resolveWebRoot() {
        Path cwd = Paths.get("").toAbsolutePath();
        Path direct = cwd.resolve("web");
        if (Files.isDirectory(direct)) {
            return direct;
        }
        Path fromParent = cwd.resolve("X-presso").resolve("web");
        if (Files.isDirectory(fromParent)) {
            return fromParent;
        }
        return direct;
    }

    private static void handleAnalyze(HttpExchange exchange) throws IOException {
        addCors(exchange.getResponseHeaders());
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, "{\"error\":\"POST required\"}");
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String source = extractJsonString(body, "source");
        String modeStr = extractJsonString(body, "mode");
        if (source == null) {
            writeJson(exchange, 400, "{\"error\":\"missing source\"}");
            return;
        }

        Analyzer.Mode mode = "lex".equalsIgnoreCase(modeStr) ? Analyzer.Mode.LEX : Analyzer.Mode.PARSE;
        try {
            Analyzer.Result result = Analyzer.analyzeSource(source, mode);
            writeJson(exchange, 200, toJson(result.toMap()));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage().replace("\"", "'") : "analyze failed";
            writeJson(exchange, 500, "{\"error\":\"" + msg + "\"}");
        }
    }

    private static void serveStatic(HttpExchange exchange, Path webRoot) throws IOException {
        addCors(exchange.getResponseHeaders());
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) {
            path = "/index.html";
        }
        // Prevent path traversal
        Path file = webRoot.resolve(path.substring(1)).normalize();
        if (!file.startsWith(webRoot.normalize()) || !Files.isRegularFile(file)) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }

        byte[] bytes = Files.readAllBytes(file);
        String contentType = contentType(file.getFileName().toString());
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String contentType(String name) {
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".css")) return "text/css; charset=utf-8";
        if (name.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (name.endsWith(".svg")) return "image/svg+xml";
        if (name.endsWith(".json")) return "application/json";
        return "application/octet-stream";
    }

    private static void addCors(Headers headers) {
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void writeJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /** Minimal JSON string field extractor (no external deps). */
    private static String extractJsonString(String json, String key) {
        String needle = "\"" + key + "\"";
        int keyIdx = json.indexOf(needle);
        if (keyIdx < 0) return null;
        int colon = json.indexOf(':', keyIdx + needle.length());
        if (colon < 0) return null;
        int i = colon + 1;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        if (i >= json.length()) return null;
        if (json.charAt(i) != '"') return null;
        i++;
        StringBuilder sb = new StringBuilder();
        while (i < json.length()) {
            char c = json.charAt(i++);
            if (c == '\\' && i < json.length()) {
                char n = json.charAt(i++);
                switch (n) {
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'u':
                        if (i + 4 <= json.length()) {
                            String hex = json.substring(i, i + 4);
                            sb.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        }
                        break;
                    default: sb.append(n); break;
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static String toJson(Object value) {
        if (value == null) return "null";
        if (value instanceof String) return quote((String) value);
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> e : ((Map<String, Object>) value).entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append(quote(e.getKey())).append(':').append(toJson(e.getValue()));
            }
            return sb.append('}').toString();
        }
        if (value instanceof Iterable) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : (Iterable<?>) value) {
                if (!first) sb.append(',');
                first = false;
                sb.append(toJson(item));
            }
            return sb.append(']').toString();
        }
        return quote(String.valueOf(value));
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }
}
