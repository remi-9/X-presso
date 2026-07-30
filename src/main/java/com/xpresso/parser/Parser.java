package com.xpresso.parser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.xpresso.lexer.Token;
import com.xpresso.lexer.TokenType;
import com.xpresso.util.SyntaxErrorHandler;

/**
 * Recursive-descent syntax analyzer for S-presso.
 * Builds a parse tree for programs: classes, members, statements, expressions.
 */
public class Parser {
    private static final Set<String> ACCESS_MODIFIERS = new HashSet<>(
        Arrays.asList("public", "private", "protected")
    );
    private static final Set<String> NON_ACCESS_MODIFIERS = new HashSet<>(
        Arrays.asList("static", "final", "abstract", "native", "strictfp")
    );
    private static final Set<String> TYPE_WORDS = new HashSet<>(Arrays.asList(
        "int", "float", "double", "long", "short", "byte", "char", "bool", "str",
        "void", "Complex", "Date", "Frac", "Rational"
    ));

    private final List<Token> tokens;
    private final SyntaxErrorHandler errorHandler;
    private int current = 0;
    private ParseNode root;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.errorHandler = new SyntaxErrorHandler();
    }

    public SyntaxErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public ParseNode getParseTree() {
        return root;
    }

    public void parse() {
        root = parseProgram();
        errorHandler.printErrors();
    }

    /** Parse without printing (for API / tests). */
    public ParseNode parseSilent() {
        root = parseProgram();
        return root;
    }

    private ParseNode parseProgram() {
        ParseNode program = new ParseNode("Program");
        while (!atEnd()) {
            if (checkClassStart()) {
                program.add(parseClass());
            } else {
                Token t = peek();
                errorHandler.reportError(
                    "Expected class declaration",
                    t.getLine(),
                    t.getColumn(),
                    "Start with modifiers or 'class'"
                );
                synchronizeToClass();
            }
        }
        return program;
    }

    private boolean checkClassStart() {
        Token t = peek();
        if (isClassKeyword(t)) return true;
        if (isAccessModifier(t) || isNonAccessModifier(t)) return true;
        return false;
    }

    private ParseNode parseClass() {
        ParseNode classNode = new ParseNode("ClassDecl");
        ParseNode mods = parseModifiers();
        classNode.add(mods);

        if (!matchReserved("class")) {
            error(peek(), "Expected 'class'", "Add 'class' keyword");
            synchronizeTo("{", "}");
            return classNode;
        }
        classNode.add(leaf("Keyword", previous()));

        if (!check(TokenType.IDENTIFIER)) {
            error(peek(), "Expected class name", "Provide a class identifier");
        } else {
            classNode.add(leaf("Identifier", advance()));
        }

        classNode.add(parseInheritance());

        if (!matchDelim("{")) {
            error(peek(), "Expected '{'", "Open the class body with '{'");
            return classNode;
        }
        classNode.add(leaf("Delim", previous()));

        ParseNode body = new ParseNode("ClassBody");
        while (!atEnd() && !checkDelim("}")) {
            body.add(parseMember());
        }
        classNode.add(body);

        if (!matchDelim("}")) {
            error(peek(), "Expected '}'", "Close the class body with '}'");
        } else {
            classNode.add(leaf("Delim", previous()));
        }
        return classNode;
    }

    private ParseNode parseModifiers() {
        ParseNode mods = new ParseNode("Modifiers");
        boolean sawAccess = false;
        while (isAccessModifier(peek()) || isNonAccessModifier(peek())) {
            if (isAccessModifier(peek())) {
                if (sawAccess) {
                    error(peek(), "Duplicate access modifier", "Only one access modifier is allowed");
                }
                sawAccess = true;
            }
            mods.add(leaf("Modifier", advance()));
        }
        return mods;
    }

    private ParseNode parseInheritance() {
        ParseNode inherit = new ParseNode("Inheritance");
        boolean sawClass = false;
        boolean sawIface = false;

        while (isInheritOp(peek())) {
            Token op = advance();
            inherit.add(leaf("InheritOp", op));

            if (op.getLexeme().equals(":>")) {
                if (sawClass) {
                    error(op, "Duplicate ':>' inheritance clause",
                        "List multiple parents as: :> Parent1, Parent2");
                }
                sawClass = true;
                parseNameList(inherit, "class name after ':>'");
            } else if (op.getLexeme().equals(":>>")) {
                if (sawIface) {
                    error(op, "Duplicate ':>>' inheritance clause",
                        "List multiple interfaces as: :>> I1, I2");
                }
                sawIface = true;
                parseNameList(inherit, "interface name after ':>>'");
            }
        }
        return inherit;
    }

    /** IDENT (',' IDENT)* */
    private void parseNameList(ParseNode parent, String expectedLabel) {
        if (!checkMemberName()) {
            error(peek(), "Expected " + expectedLabel, "Provide a valid type name");
            return;
        }
        parent.add(leaf("Identifier", advance()));
        while (checkDelim(",")) {
            parent.add(leaf("Delim", advance()));
            if (!checkMemberName()) {
                error(peek(), "Expected name after ','", "Ensure: Name1, Name2");
                break;
            }
            parent.add(leaf("Identifier", advance()));
        }
    }

    private ParseNode parseMember() {
        // Access section: private { ... } / public { ... }
        if ((isAccessModifier(peek()) || isNonAccessModifier(peek())) && lookAheadDelim("{")) {
            return parseAccessSection();
        }

        int start = current;
        ParseNode mods = parseModifiers();

        // Typeless main(...) { } allowed in S-presso samples
        if (isMainName(peek()) && lookAheadDelim("(")) {
            ParseNode method = new ParseNode("MethodDecl");
            method.add(mods);
            method.add(new ParseNode("Type", "void"));
            method.add(leaf("Identifier", advance()));
            method.add(parseParams());
            method.add(parseBlock());
            return method;
        }

        if (!checkTypeStart()) {
            current = start;
            return parseStatement();
        }

        ParseNode type = parseType();

        if (!checkMemberName()) {
            error(peek(), "Expected member name", "Provide a field or method name");
            synchronizeTo(";", "{", "}");
            ParseNode bad = new ParseNode("InvalidMember");
            bad.add(mods);
            bad.add(type);
            return bad;
        }
        Token name = advance();

        if (checkDelim("(")) {
            ParseNode method = new ParseNode("MethodDecl");
            method.add(mods);
            method.add(type);
            method.add(leaf("Identifier", name));
            method.add(parseParams());
            method.add(parseBlock());
            return method;
        }

        ParseNode field = new ParseNode("FieldDecl");
        field.add(mods);
        field.add(type);
        field.add(leaf("Identifier", name));
        if (check(TokenType.ASSIGN_OP)) {
            field.add(leaf("AssignOp", advance()));
            field.add(parseExpression());
        }
        if (!matchDelim(";")) {
            error(peek(), "Expected ';'", "Terminate the field declaration with ';'");
        } else {
            field.add(leaf("Delim", previous()));
        }
        return field;
    }

    private ParseNode parseAccessSection() {
        ParseNode section = new ParseNode("AccessSection");
        section.add(leaf("Modifier", advance()));
        if (!matchDelim("{")) {
            error(peek(), "Expected '{'", "Open an access section with '{'");
            return section;
        }
        section.add(leaf("Delim", previous()));
        while (!atEnd() && !checkDelim("}")) {
            section.add(parseMember());
        }
        if (!matchDelim("}")) {
            error(peek(), "Expected '}'", "Close the access section with '}'");
        } else {
            section.add(leaf("Delim", previous()));
        }
        return section;
    }

    private ParseNode parseParams() {
        ParseNode params = new ParseNode("Params");
        expectDelim("(");
        params.add(leaf("Delim", previous()));

        if (!checkDelim(")")) {
            do {
                ParseNode param = new ParseNode("Param");
                if (isTypedParamStart()) {
                    param.add(parseType());
                    if (checkMemberName()) {
                        param.add(leaf("Identifier", advance()));
                    } else {
                        error(peek(), "Expected parameter name", "Provide a parameter identifier");
                    }
                } else if (checkMemberName()) {
                    // Untyped param: main(args)
                    param.add(leaf("Identifier", advance()));
                } else {
                    error(peek(), "Expected parameter", "Provide a type and name, or a name");
                    advance();
                }
                params.add(param);
            } while (matchDelim(","));
        }

        if (!matchDelim(")")) {
            error(peek(), "Expected ')'", "Close the parameter list");
        } else {
            params.add(leaf("Delim", previous()));
        }
        return params;
    }

    private ParseNode parseBlock() {
        ParseNode block = new ParseNode("Block");
        if (!matchDelim("{")) {
            error(peek(), "Expected '{'", "Open a block with '{'");
            return block;
        }
        block.add(leaf("Delim", previous()));

        while (!atEnd() && !checkDelim("}")) {
            block.add(parseStatement());
        }

        if (!matchDelim("}")) {
            error(peek(), "Expected '}'", "Close the block with '}'");
        } else {
            block.add(leaf("Delim", previous()));
        }
        return block;
    }

    private ParseNode parseStatement() {
        Token t = peek();

        if (checkDelim("{")) {
            return parseBlock();
        }
        if (checkDelim(";")) {
            ParseNode empty = new ParseNode("EmptyStmt");
            empty.add(leaf("Delim", advance()));
            return empty;
        }
        if (isKeyword(t, "if")) {
            return parseIf();
        }
        if (isKeyword(t, "while")) {
            return parseWhile();
        }
        if (isKeyword(t, "for")) {
            return parseFor();
        }
        if (isKeyword(t, "do")) {
            return parseDoWhile();
        }
        if (isKeyword(t, "switch") || isKeyword(t, "switch-fall")) {
            return parseSwitch();
        }
        // 'return' is RESERVED in SpecialWords, not KEYWORD
        if (isReservedWord(t, "return")) {
            return parseReturn();
        }
        if (isKeyword(t, "print") || isKeyword(t, "break") || isKeyword(t, "exit") || isKeyword(t, "exit-when")) {
            return parseSimpleKeywordStmt();
        }
        if (checkTypeStart() || isAccessModifier(t) || isNonAccessModifier(t)) {
            return parseLocalDeclOrExpr();
        }

        // Expression statement
        ParseNode exprStmt = new ParseNode("ExprStmt");
        exprStmt.add(parseExpression());
        if (!matchDelim(";")) {
            error(peek(), "Expected ';'", "Terminate the statement with ';'");
        } else {
            exprStmt.add(leaf("Delim", previous()));
        }
        return exprStmt;
    }

    private ParseNode parseLocalDeclOrExpr() {
        int start = current;
        ParseNode mods = parseModifiers();
        if (checkTypeStart()) {
            ParseNode decl = new ParseNode("VarDecl");
            decl.add(mods);
            decl.add(parseType());
            if (checkMemberName()) {
                decl.add(leaf("Identifier", advance()));
                if (check(TokenType.ASSIGN_OP)) {
                    decl.add(leaf("AssignOp", advance()));
                    decl.add(parseExpression());
                }
                if (!matchDelim(";")) {
                    error(peek(), "Expected ';'", "Terminate the declaration with ';'");
                } else {
                    decl.add(leaf("Delim", previous()));
                }
                return decl;
            }
        }
        current = start;
        ParseNode exprStmt = new ParseNode("ExprStmt");
        exprStmt.add(parseExpression());
        if (!matchDelim(";")) {
            error(peek(), "Expected ';'", "Terminate the statement with ';'");
        } else {
            exprStmt.add(leaf("Delim", previous()));
        }
        return exprStmt;
    }

    private ParseNode parseIf() {
        ParseNode node = new ParseNode("IfStmt");
        node.add(leaf("Keyword", advance()));
        expectDelim("(");
        node.add(leaf("Delim", previous()));
        node.add(parseExpression());
        expectDelim(")");
        node.add(leaf("Delim", previous()));
        node.add(parseStatement());
        if (isKeyword(peek(), "else")) {
            node.add(leaf("Keyword", advance()));
            node.add(parseStatement());
        }
        return node;
    }

    private ParseNode parseWhile() {
        ParseNode node = new ParseNode("WhileStmt");
        node.add(leaf("Keyword", advance()));
        expectDelim("(");
        node.add(leaf("Delim", previous()));
        node.add(parseExpression());
        expectDelim(")");
        node.add(leaf("Delim", previous()));
        node.add(parseStatement());
        return node;
    }

    private ParseNode parseDoWhile() {
        ParseNode node = new ParseNode("DoWhileStmt");
        node.add(leaf("Keyword", advance()));
        node.add(parseStatement());
        if (!isKeyword(peek(), "while")) {
            error(peek(), "Expected 'while' after do-body", "Write: do { ... } while (cond);");
            return node;
        }
        node.add(leaf("Keyword", advance()));
        expectDelim("(");
        node.add(leaf("Delim", previous()));
        node.add(parseExpression());
        expectDelim(")");
        node.add(leaf("Delim", previous()));
        if (!matchDelim(";")) {
            error(peek(), "Expected ';'", "Terminate do-while with ';'");
        } else {
            node.add(leaf("Delim", previous()));
        }
        return node;
    }

    private ParseNode parseFor() {
        ParseNode node = new ParseNode("ForStmt");
        node.add(leaf("Keyword", advance()));
        expectDelim("(");
        node.add(leaf("Delim", previous()));

        // init
        if (!checkDelim(";")) {
            if (checkTypeStart()) {
                node.add(parseLocalDeclOrExpr());
            } else {
                node.add(parseExpression());
                if (!matchDelim(";")) {
                    error(peek(), "Expected ';'", "Separate for-clauses with ';'");
                }
            }
        } else {
            advance();
        }

        // condition
        if (!checkDelim(";")) {
            node.add(parseExpression());
        }
        if (!matchDelim(";")) {
            error(peek(), "Expected ';'", "Separate for-clauses with ';'");
        }

        // update
        if (!checkDelim(")")) {
            node.add(parseExpression());
        }
        expectDelim(")");
        node.add(leaf("Delim", previous()));
        node.add(parseStatement());
        return node;
    }

    private ParseNode parseSwitch() {
        ParseNode node = new ParseNode("SwitchStmt");
        node.add(leaf("Keyword", advance()));
        expectDelim("(");
        node.add(leaf("Delim", previous()));
        node.add(parseExpression());
        expectDelim(")");
        node.add(leaf("Delim", previous()));
        node.add(parseBlock());
        return node;
    }

    private ParseNode parseReturn() {
        ParseNode node = new ParseNode("ReturnStmt");
        node.add(leaf("Keyword", advance()));
        if (!checkDelim(";")) {
            node.add(parseExpression());
        }
        if (!matchDelim(";")) {
            error(peek(), "Expected ';'", "Terminate return with ';'");
        } else {
            node.add(leaf("Delim", previous()));
        }
        return node;
    }

    private ParseNode parseSimpleKeywordStmt() {
        ParseNode node = new ParseNode("KeywordStmt");
        node.add(leaf("Keyword", advance()));
        if (!checkDelim(";") && !checkDelim("{") && !checkDelim("}")) {
            node.add(parseExpression());
        }
        if (checkDelim("{")) {
            node.add(parseBlock());
        } else if (!matchDelim(";")) {
            // exit-when may take a condition without requiring more structure
            if (!checkDelim("}") && !atEnd()) {
                error(peek(), "Expected ';'", "Terminate the statement with ';'");
            }
        } else {
            node.add(leaf("Delim", previous()));
        }
        return node;
    }

    // --- Expressions (precedence climbing) ---

    private ParseNode parseExpression() {
        return parseAssignment();
    }

    private ParseNode parseAssignment() {
        ParseNode left = parseTernary();
        if (check(TokenType.ASSIGN_OP)) {
            ParseNode assign = new ParseNode("AssignExpr");
            assign.add(left);
            assign.add(leaf("AssignOp", advance()));
            assign.add(parseAssignment());
            return assign;
        }
        return left;
    }

    private ParseNode parseTernary() {
        ParseNode cond = parseLogicOr();
        if (check(TokenType.TERNARY_OP) && peek().getLexeme().equals("?")) {
            ParseNode ternary = new ParseNode("TernaryExpr");
            ternary.add(cond);
            ternary.add(leaf("TernaryOp", advance()));
            ternary.add(parseExpression());
            if (check(TokenType.PUNC_DELIM) && peek().getLexeme().equals(":")
                || check(TokenType.TERNARY_OP) && peek().getLexeme().equals(":")) {
                ternary.add(leaf("TernaryOp", advance()));
                ternary.add(parseTernary());
            } else {
                error(peek(), "Expected ':' in ternary", "Use cond ? then : else");
            }
            return ternary;
        }
        return cond;
    }

    private ParseNode parseLogicOr() {
        ParseNode left = parseLogicAnd();
        while (check(TokenType.LOG_OP) && peek().getLexeme().equals("||")) {
            ParseNode bin = new ParseNode("BinaryExpr");
            bin.add(left);
            bin.add(leaf("LogOp", advance()));
            bin.add(parseLogicAnd());
            left = bin;
        }
        return left;
    }

    private ParseNode parseLogicAnd() {
        ParseNode left = parseBitwise();
        while (check(TokenType.LOG_OP) && peek().getLexeme().equals("&&")) {
            ParseNode bin = new ParseNode("BinaryExpr");
            bin.add(left);
            bin.add(leaf("LogOp", advance()));
            bin.add(parseBitwise());
            left = bin;
        }
        return left;
    }

    private ParseNode parseBitwise() {
        ParseNode left = parseEquality();
        while (check(TokenType.BIT_OP)) {
            ParseNode bin = new ParseNode("BinaryExpr");
            bin.add(left);
            bin.add(leaf("BitOp", advance()));
            bin.add(parseEquality());
            left = bin;
        }
        return left;
    }

    private ParseNode parseEquality() {
        ParseNode left = parseRelational();
        while (check(TokenType.REL_OP) &&
            (peek().getLexeme().equals("==") || peek().getLexeme().equals("!="))) {
            ParseNode bin = new ParseNode("BinaryExpr");
            bin.add(left);
            bin.add(leaf("RelOp", advance()));
            bin.add(parseRelational());
            left = bin;
        }
        return left;
    }

    private ParseNode parseRelational() {
        ParseNode left = parseShift();
        while (check(TokenType.REL_OP)) {
            String op = peek().getLexeme();
            if (op.equals("<") || op.equals(">") || op.equals("<=") || op.equals(">=")) {
                ParseNode bin = new ParseNode("BinaryExpr");
                bin.add(left);
                bin.add(leaf("RelOp", advance()));
                bin.add(parseShift());
                left = bin;
            } else {
                break;
            }
        }
        return left;
    }

    private ParseNode parseShift() {
        ParseNode left = parseAdditive();
        while (check(TokenType.BIT_OP) &&
            (peek().getLexeme().equals("<<") || peek().getLexeme().equals(">>")
                || peek().getLexeme().equals(">>>"))) {
            ParseNode bin = new ParseNode("BinaryExpr");
            bin.add(left);
            bin.add(leaf("BitOp", advance()));
            bin.add(parseAdditive());
            left = bin;
        }
        return left;
    }

    private ParseNode parseAdditive() {
        ParseNode left = parseMultiplicative();
        while ((check(TokenType.ARITHMETIC_OP) || check(TokenType.UNARY_OP))
            && (peek().getLexeme().equals("+") || peek().getLexeme().equals("-"))) {
            // binary + / - only when not absorbed as unary already
            if (check(TokenType.UNARY_OP)) {
                // lexer may tag binary minus as UNARY in wrong cases; still accept as binary here
            }
            ParseNode bin = new ParseNode("BinaryExpr");
            bin.add(left);
            bin.add(leaf("ArithOp", advance()));
            bin.add(parseMultiplicative());
            left = bin;
        }
        return left;
    }

    private ParseNode parseMultiplicative() {
        ParseNode left = parsePower();
        while (check(TokenType.ARITHMETIC_OP)
            && ("*/%^".contains(peek().getLexeme()) || peek().getLexeme().equals("*")
                || peek().getLexeme().equals("/") || peek().getLexeme().equals("%")
                || peek().getLexeme().equals("^"))) {
            String op = peek().getLexeme();
            if (op.equals("*") || op.equals("/") || op.equals("%") || op.equals("^")) {
                ParseNode bin = new ParseNode("BinaryExpr");
                bin.add(left);
                bin.add(leaf("ArithOp", advance()));
                bin.add(parsePower());
                left = bin;
            } else {
                break;
            }
        }
        return left;
    }

    private ParseNode parsePower() {
        ParseNode left = parseUnary();
        if (check(TokenType.UNARY_OP) && peek().getLexeme().equals("**")) {
            ParseNode bin = new ParseNode("BinaryExpr");
            bin.add(left);
            bin.add(leaf("PowOp", advance()));
            bin.add(parsePower()); // right-assoc
            return bin;
        }
        return left;
    }

    private ParseNode parseUnary() {
        if (check(TokenType.UNARY_OP) || (check(TokenType.LOG_OP) && peek().getLexeme().equals("!"))
            || (check(TokenType.BIT_OP) && peek().getLexeme().equals("~"))) {
            ParseNode unary = new ParseNode("UnaryExpr");
            unary.add(leaf("UnaryOp", advance()));
            unary.add(parseUnary());
            return unary;
        }
        return parsePostfix();
    }

    private ParseNode parsePostfix() {
        ParseNode left = parsePrimary();
        while (true) {
            if (check(TokenType.UNARY_OP)
                && (peek().getLexeme().equals("++") || peek().getLexeme().equals("--"))) {
                ParseNode post = new ParseNode("PostfixExpr");
                post.add(left);
                post.add(leaf("UnaryOp", advance()));
                left = post;
            } else if (check(TokenType.METHOD_OP) || checkDelim("(") || checkDelim("[")) {
                if (checkDelim("(")) {
                    ParseNode call = new ParseNode("CallExpr");
                    call.add(left);
                    call.add(parseArgList());
                    left = call;
                } else if (checkDelim("[")) {
                    ParseNode idx = new ParseNode("IndexExpr");
                    idx.add(left);
                    idx.add(leaf("Delim", advance()));
                    idx.add(parseExpression());
                    expectDelim("]");
                    idx.add(leaf("Delim", previous()));
                    left = idx;
                } else {
                    ParseNode member = new ParseNode("MemberExpr");
                    member.add(left);
                    member.add(leaf("MethodOp", advance()));
                    if (check(TokenType.IDENTIFIER) || check(TokenType.KEYWORD) || check(TokenType.RESERVED)) {
                        member.add(leaf("Identifier", advance()));
                    } else {
                        error(peek(), "Expected member name", "Provide a field or method name after '.'");
                    }
                    left = member;
                }
            } else if (check(TokenType.LOOP_OP)) {
                ParseNode range = new ParseNode("RangeExpr");
                range.add(left);
                range.add(leaf("LoopOp", advance()));
                range.add(parseUnary());
                left = range;
            } else {
                break;
            }
        }
        return left;
    }

    private ParseNode parseArgList() {
        ParseNode args = new ParseNode("ArgList");
        expectDelim("(");
        args.add(leaf("Delim", previous()));
        if (!checkDelim(")")) {
            do {
                args.add(parseExpression());
            } while (matchDelim(","));
        }
        if (!matchDelim(")")) {
            error(peek(), "Expected ')'", "Close the argument list");
        } else {
            args.add(leaf("Delim", previous()));
        }
        return args;
    }

    private ParseNode parsePrimary() {
        Token t = peek();

        if (checkDelim("(")) {
            ParseNode group = new ParseNode("GroupExpr");
            group.add(leaf("Delim", advance()));
            group.add(parseExpression());
            expectDelim(")");
            group.add(leaf("Delim", previous()));
            return group;
        }

        if (t.getType() == TokenType.INT_LIT || t.getType() == TokenType.FLOAT_LIT
            || t.getType() == TokenType.STR_LIT || t.getType() == TokenType.CHAR_LIT
            || t.getType() == TokenType.BOOL_LIT || t.getType() == TokenType.NULL_LIT
            || t.getType() == TokenType.FRAC_LIT || t.getType() == TokenType.DATE_LIT
            || t.getType() == TokenType.COMP_LIT) {
            return leaf("Literal", advance());
        }

        if (t.getType() == TokenType.OBJ_DELIM && t.getLexeme().equals("<")) {
            ParseNode obj = new ParseNode("ObjectType");
            obj.add(leaf("ObjDelim", advance()));
            if (check(TokenType.IDENTIFIER) || check(TokenType.RESERVED)) {
                obj.add(leaf("Identifier", advance()));
            }
            if (check(TokenType.OBJ_DELIM) && peek().getLexeme().equals(">")) {
                obj.add(leaf("ObjDelim", advance()));
            }
            return obj;
        }

        if (t.getType() == TokenType.IDENTIFIER || t.getType() == TokenType.KEYWORD
            || t.getType() == TokenType.RESERVED) {
            return leaf("Identifier", advance());
        }

        if (t.getType() == TokenType.STR_DELIM) {
            // string/char already split: skip delim, take lit if present
            ParseNode lit = new ParseNode("QuotedLiteral");
            lit.add(leaf("StrDelim", advance()));
            if (check(TokenType.STR_LIT) || check(TokenType.CHAR_LIT)) {
                lit.add(leaf("Literal", advance()));
            }
            if (check(TokenType.STR_DELIM)) {
                lit.add(leaf("StrDelim", advance()));
            }
            return lit;
        }

        error(t, "Unexpected token in expression: " + t.getLexeme(),
            "Expected a literal, identifier, or '('");
        advance();
        return new ParseNode("ErrorExpr", t.getLexeme(), t.getLine(), t.getColumn());
    }

    private ParseNode parseType() {
        ParseNode type = new ParseNode("Type");
        if (check(TokenType.OBJ_DELIM) && peek().getLexeme().equals("<")) {
            type.add(parsePrimary()); // ObjectType
        } else if (check(TokenType.RESERVED) || check(TokenType.IDENTIFIER) || check(TokenType.KEYWORD)) {
            type.add(leaf("TypeName", advance()));
        } else {
            error(peek(), "Expected type", "Provide a type name");
            return type;
        }
        while (checkDelim("[") ) {
            type.add(leaf("Delim", advance()));
            if (!matchDelim("]")) {
                error(peek(), "Expected ']'", "Close array brackets");
                break;
            }
            type.add(leaf("Delim", previous()));
        }
        return type;
    }

    private boolean checkTypeStart() {
        Token t = peek();
        if (t.getType() == TokenType.OBJ_DELIM && t.getLexeme().equals("<")) return true;
        // void is KEYWORD; int/str/Complex/etc. are RESERVED — both are types
        if (TYPE_WORDS.contains(t.getLexeme())
            && (t.getType() == TokenType.RESERVED || t.getType() == TokenType.KEYWORD)) {
            return true;
        }
        if (t.getType() == TokenType.IDENTIFIER) return true;
        return false;
    }

    /**
     * True when the next tokens look like a typed parameter (type name),
     * not a bare untyped name like {@code args} before {@code )} or {@code ,}.
     */
    private boolean isTypedParamStart() {
        Token t = peek();
        if (t.getType() == TokenType.OBJ_DELIM && t.getLexeme().equals("<")) return true;
        if (TYPE_WORDS.contains(t.getLexeme())
            && (t.getType() == TokenType.RESERVED || t.getType() == TokenType.KEYWORD)) {
            return true;
        }
        if (t.getType() != TokenType.IDENTIFIER) return false;
        // User type: Type name  OR  Type[] name
        if (lookAheadDelim("[")) return true;
        if (current + 1 >= tokens.size()) return false;
        Token next = tokens.get(current + 1);
        return next.getType() == TokenType.IDENTIFIER || isMainName(next);
    }

    /** Field / method / param name: identifier or reserved 'main'. */
    private boolean checkMemberName() {
        Token t = peek();
        if (t.getType() == TokenType.IDENTIFIER) return true;
        return isMainName(t);
    }

    private boolean isMainName(Token token) {
        return token.getType() == TokenType.RESERVED && token.getLexeme().equals("main");
    }

    private boolean lookAheadDelim(String lexeme) {
        if (current + 1 >= tokens.size()) return false;
        Token t = tokens.get(current + 1);
        return t.getType() == TokenType.DELIM && t.getLexeme().equals(lexeme);
    }

    // --- helpers ---

    private ParseNode leaf(String kind, Token token) {
        return new ParseNode(kind, token.getLexeme(), token.getLine(), token.getColumn());
    }

    private void error(Token token, String message, String suggestion) {
        errorHandler.reportError(message, token.getLine(), token.getColumn(), suggestion);
    }

    private boolean isAccessModifier(Token token) {
        return token.getType() == TokenType.RESERVED && ACCESS_MODIFIERS.contains(token.getLexeme());
    }

    private boolean isNonAccessModifier(Token token) {
        return token.getType() == TokenType.RESERVED && NON_ACCESS_MODIFIERS.contains(token.getLexeme());
    }

    private boolean isClassKeyword(Token token) {
        return token.getType() == TokenType.RESERVED && token.getLexeme().equals("class");
    }

    private boolean isInheritOp(Token token) {
        return token.getType() == TokenType.INHERIT_OP;
    }

    private boolean isKeyword(Token token, String lexeme) {
        return token.getType() == TokenType.KEYWORD && token.getLexeme().equals(lexeme);
    }

    private boolean isReservedWord(Token token, String lexeme) {
        return token.getType() == TokenType.RESERVED && token.getLexeme().equals(lexeme);
    }

    private boolean check(TokenType type) {
        return !atEnd() && peek().getType() == type;
    }

    private boolean checkDelim(String lexeme) {
        return check(TokenType.DELIM) && peek().getLexeme().equals(lexeme);
    }

    private boolean matchDelim(String lexeme) {
        if (checkDelim(lexeme)) {
            advance();
            return true;
        }
        return false;
    }

    private boolean matchReserved(String lexeme) {
        if (check(TokenType.RESERVED) && peek().getLexeme().equals(lexeme)) {
            advance();
            return true;
        }
        return false;
    }

    private void expectDelim(String lexeme) {
        if (!matchDelim(lexeme)) {
            error(peek(), "Expected '" + lexeme + "'", "Insert '" + lexeme + "'");
        }
    }

    private void synchronizeToClass() {
        advance();
        while (!atEnd()) {
            if (checkClassStart()) return;
            advance();
        }
    }

    private void synchronizeTo(String... delims) {
        Set<String> set = new HashSet<>(Arrays.asList(delims));
        while (!atEnd()) {
            if (check(TokenType.DELIM) && set.contains(peek().getLexeme())) {
                return;
            }
            advance();
        }
    }

    private Token advance() {
        if (!atEnd()) current++;
        return previous();
    }

    private boolean atEnd() {
        return peek().getType() == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }
}
