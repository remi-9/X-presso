# Test fixtures

| Folder | Purpose |
|--------|---------|
| `valid/class-*.txt` | Full programs — should **lex + parse** cleanly |
| `valid/*` (other) | Lexical / snippet samples — lex OK; parse may fail without a wrapping `class` |
| `invalid/lex-*.txt` | Expected **lexical** errors |
| `invalid/syn-*.txt` | Expected **syntax** errors |

Naming: `class-*` programs · `stmt-*` / `operators-*` / `literals-*` lex snippets · `lex-*` / `syn-*` negatives.
