# X-presso

Lexical and syntax analyzer for **S-presso** — a Java lexer, recursive-descent parser, CLI, and café-themed web UI.

Language reference: [`docs/LANGUAGE.md`](docs/LANGUAGE.md)

## What it does

X-presso tokenizes S-presso source, reports lexical and syntax errors with line/column, and builds a parse tree. The same analyzer backs both the command line and the browser UI.

- Regex-driven lexer with keywords, reserved words, operators, and S-presso literals (`Complex`, `Frac`, `Date`)
- Recursive-descent parser for classes, members, statements, and expressions
- Shared `Analyzer` facade used by CLI and `POST /api/analyze`
- Web UI at `http://localhost:8080/` — tokens, parse tree, and errors
- Valid/invalid fixtures under `test/` plus JUnit lexer tests

## Requirements

- **JDK 17+**
- **Maven 3.8+** (optional — `javac` works without it)

## Quick start

```powershell
# compile
New-Item -ItemType Directory -Force -Path out | Out-Null
Get-ChildItem -Path src\main\java -Recurse -Filter *.java |
  ForEach-Object { $_.FullName } | Set-Content -Encoding ascii sources.txt
javac -encoding UTF-8 -d out "@sources.txt"

# web UI → http://localhost:8080/
java -cp out com.xpresso.server.WebServer

# CLI
java -cp out com.xpresso.cli.Main test\valid\class-simple.txt
java -cp out com.xpresso.cli.Main test\invalid\lex-ident-starts-digit.txt --lex-only
```

With Maven:

```bash
mvn -q compile
mvn -q exec:java -Dexec.mainClass=com.xpresso.server.WebServer
mvn -q exec:java "-Dexec.mainClass=com.xpresso.cli.Main" "-Dexec.args=test/valid/class-simple.txt"
mvn -q test
```

No arguments starts an interactive CLI prompt.

## CLI flags

| Flag | Meaning |
|------|---------|
| `--lex-only` | Tokenize only; skip the parser |
| `--verbose` | Include whitespace and comments in token output |
| `--file` | Write tokens under `output/` |
| `--output=text\|json` | Console format (default `text`) |

```text
Usage: Main <file> [--lex-only] [--verbose] [--file] [--output=text|json]
```

## Web UI

`com.xpresso.server.WebServer` serves `web/` and `POST /api/analyze`.

| Mode | UI name | Behavior |
|------|---------|----------|
| parse (default) | Doppio | lex + parse |
| lex | Ristretto | tokens only |

Results show as a token table, parse tree, and error list. Optional port: `java -cp out com.xpresso.server.WebServer 9090`.

## Modules

```
com.xpresso
├── language   # keywords / reserved words
├── lexer      # tokenizer
├── parser     # recursive-descent syntax analyzer + parse tree
├── analyzer   # shared lex+parse facade (CLI + web)
├── util       # SourceReader, error handlers
├── cli        # command-line entry (Main)
└── server     # web UI + /api/analyze
```

## S-presso highlights

S-presso is Java-like with extra lexical forms. Full grammar: [`docs/LANGUAGE.md`](docs/LANGUAGE.md).

| Feature | Example |
|---------|---------|
| Inheritance | `class Demo :> Base :>> Printable` |
| Complex | `$(1.0, -2)` |
| Fraction | `[3\|4]` |
| Date | `[2026\|07\|30]` |
| Nullish assign | `label ?= null` |
| Range | `5..10` |
| Control | `exit-when`, `switch-fall` |

## Test fixtures

| Folder | Meaning |
|--------|---------|
| `test/valid/` | Correct samples (lex cleanly; `class-*` meant to parse) |
| `test/invalid/` | Wrong samples (`lex-*` lexical errors, `syn-*` syntax/structure) |

See [`test/README.md`](test/README.md). Run JUnit with `mvn -q test`.

## License

[MIT](LICENSE) © Jeremias Pablo
