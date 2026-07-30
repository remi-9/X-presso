# X-presso

Lexical and syntax analyzer for **S-presso**, with a CLI and browser UI.

Language reference: [`docs/LANGUAGE.md`](docs/LANGUAGE.md)

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

With Maven (if installed):

```bash
mvn -q compile
mvn -q exec:java -Dexec.mainClass=com.xpresso.server.WebServer
mvn -q exec:java "-Dexec.mainClass=com.xpresso.cli.Main" "-Dexec.args=test/valid/class-simple.txt"
mvn -q test
```

## Test fixtures

| Folder | Meaning |
|--------|---------|
| `test/valid/` | Correct samples (lex cleanly; class-* meant to parse) |
| `test/invalid/` | Wrong samples (`lex-*` lexical errors, `syn-*` syntax/structure) |

See [`test/README.md`](test/README.md).

## CLI flags

`--lex-only` · `--verbose` · `--file` · `--output=text|json`
