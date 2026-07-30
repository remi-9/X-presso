# S-presso Language Spec (from codebase)

Canonical reference derived from `SpecialWords`, `Patterns`, and the lexer/parser.
Anything unique to S-presso (vs typical Java-like languages) is marked **unique**.

---

## Lexical overview

| Category | Forms |
|----------|--------|
| Identifiers | `[A-Za-z][A-Za-z0-9_]*` |
| Keywords | see below |
| Reserved words | see below |
| Comments | `//` line, `/* … */` block |
| Strings | `"…"` with escapes |
| Chars | `'…'` with escapes |
| Numbers | integers, floats (`1.0`, optional exponent) |
| Booleans | `true` / `false` (as bool literals when recognized) |
| Null | `null` → `NULL_LIT` |

---

## Keywords

`break`, `case`, `day`, `default`, `do`, `else`, `exit`, `exit-when`, `for`, `from`, `get`, `having`, `if`, `in`, `Input`, `limit`, `month`, `order_by`, `Output`, `print`, `select`, `switch`, `switch-fall`, `System`, `void`, `while`, `where`, `where-type`, `year`

### Unique / notable keywords

| Lexeme | Notes |
|--------|--------|
| `exit-when` | **unique** hyphenated control keyword |
| `switch-fall` | **unique** hyphenated switch variant |
| `where-type` | **unique** type-filter style keyword |
| `order_by` | **unique** SQL-ish underscore keyword |
| `select`, `from`, `where`, `having`, `limit`, `in` | query-oriented vocabulary |
| `day`, `month`, `year` | date-field accessors |
| `Input`, `Output`, `System`, `print` | I/O / runtime surface |

---

## Reserved words

`abstract`, `after`, `ALIAS`, `before`, `bool`, `byte`, `char`, `class`, `Complex`, `Date`, `double`, `exclude`, `export_as`, `Frac`, `filter_by`, `final`, `float`, `inline_query`, `inspect`, `int`, `long`, `main`, `modify`, `native`, `private`, `protected`, `public`, `Rational`, `return`, `short`, `static`, `STRICT`, `strictfp`, `str`, `today`, `toMixed`, `transient`, `validate`, `volatile`, `isValid`

### Unique / notable reserved words

| Lexeme | Notes |
|--------|--------|
| `Complex`, `Frac`, `Rational`, `Date` | **unique** first-class domain types |
| `bool`, `str` | short aliases for boolean/string |
| `ALIAS`, `STRICT` | **unique** uppercase language flags |
| `after`, `before` | temporal / ordering hooks |
| `exclude`, `export_as`, `filter_by`, `inline_query` | **unique** query/export vocabulary |
| `inspect`, `modify`, `validate`, `isValid`, `toMixed`, `today` | **unique** reflective / conversion helpers |

Noise word (ignored if encountered as noise): `general`.

---

## Operators

| Kind | Lexemes |
|------|---------|
| Assign | `=`, `+=`, `-=`, `*=`, `/=`, `%=`, `?=` **unique** nullish/assign-if |
| Arithmetic | `+`, `-`, `*`, `/`, `%`, `^` |
| Unary | `+`, `-`, `++`, `--`, `**`, `!` |
| Relational | `==`, `!=`, `<`, `>`, `<=`, `>=` |
| Logical | `\|\|`, `&&`, `!` |
| Bitwise | `&`, `\|`, `^\|` **unique**, `~`, `<<`, `>>`, `>>>` |
| Ternary | `?` `:` |
| Method / member | `.`, `::`, `->` |
| Inheritance | `:>` (class) **unique**, `:>>` (interfaces) **unique** |
| Loop / range | `..`, `...` **unique** |

---

## Literals (S-presso specialties)

| Literal | Form | Example | Notes |
|---------|------|---------|--------|
| Complex | `$(re, im)` | `$(1.0, -2)` | **unique** |
| Fraction | `[n\|d]` | `[3\|4]` | **unique**; denom ≠ 0 |
| Date | `[YYYY\|MM\|DD]` | `[2026\|07\|30]` | **unique** |
| Object type delim | `<TypeName>` | `<Rational>` | **unique** typed object wrapper syntax |

---

## Delimiters

`( ) { } [ ] , ; : @`

`,` is token type `DELIM` (parser must match `DELIM`, not `PUNC_DELIM`).

---

## Program structure (syntax target)

```
program        → classDecl+
classDecl      → modifiers? 'class' IDENT inherit? '{' member* '}'
modifiers      → accessMod? nonAccessMod*
accessMod      → 'public' | 'private' | 'protected'
nonAccessMod   → 'static' | 'final' | 'abstract' | 'native' | 'strictfp'
inherit        → (':>' nameList)? (':>>' nameList)?
nameList       → IDENT (',' IDENT)*
member         → accessSection | fieldDecl | methodDecl
accessSection  → accessMod '{' member* '}'          # unique S-presso visibility block
fieldDecl      → modifiers? type name ('=' expr)? ';'
methodDecl     → modifiers? type? name '(' params? ')' block   # type optional for main
params         → param (',' param)*
param          → type name | name                   # untyped params allowed
name           → IDENT | 'main'
type           → typeWord | IDENT | type '[]' | '<' IDENT '>'
typeWord       → 'int'|'float'|...|'void'|'str'|'Complex'|...
block          → '{' statement* '}'
statement      → varDecl | ifStmt | whileStmt | forStmt | switchStmt
               | returnStmt | printStmt | block | exprStmt | empty ';'
```

Expression precedence (high → low): primary → unary → `**` → `*`/`/`/`%` → `+`/`-` → shifts → relational → equality → bitwise → logical → ternary → assign.

Notes:
- `return` is a **reserved** word (not keyword); parser must accept `RESERVED return`.
- `void` is a **keyword** used as a type; parser must treat it as a type start.
- `main` is **reserved** but valid as a method name.
- Statement-only snippets (no `class`) are **lex fixtures**, not full parse programs.

---

## Implementation map

| Spec area | Source of truth |
|-----------|-----------------|
| Keywords / reserved | `src/main/java/com/xpresso/language/SpecialWords.java` |
| Regex patterns | `src/main/java/com/xpresso/lexer/Patterns.java` |
| Token kinds | `src/main/java/com/xpresso/lexer/TokenType.java` |
| Lexing rules | `src/main/java/com/xpresso/lexer/Lexer.java` |
| Syntax rules | `src/main/java/com/xpresso/parser/Parser.java` |
