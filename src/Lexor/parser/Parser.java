package Lexor.parser;

import Lexor.err.ErrorManager;
import Lexor.err.ParseError;
import Lexor.lexer.Token;
import Lexor.lexer.TokenType;
import Lexor.parser.ast.Expr;
import Lexor.parser.ast.Stmt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private final ErrorManager errorManager;
    private int current = 0;

    public Parser(List<Token> tokens, ErrorManager errorManager) {
        this.tokens = tokens;
        this.errorManager = errorManager;
    }

    public List<Stmt> parse(){
        try{
            List<Stmt> statements = new ArrayList<>();
            parseHeader();
            parseDeclarations(statements);
            parseExecutableStatements(statements);
            parseFooter();
            return statements;
        }catch (ParseError error){
            synchronize();
            return null;
        }
    }

    private void parseHeader() {
        while(check(TokenType.NEWLINE)) advance();
        consumeKeywords(
                 "Expected 'SCRIPT AREA' at the start of file",
                TokenType.SCRIPT, TokenType.AREA
        );
        consumeKeywords(
                "Expected 'START SCRIPT' after 'SCRIPT AREA'",
                TokenType.START, TokenType.SCRIPT
        );
    }

    private void parseDeclarations(List<Stmt> statements) {
        while (match(TokenType.DECLARE) && !isAtEnd()) {
            statements.add(varDeclaration());
        }
    }

    private void parseExecutableStatements(List<Stmt> statements) {
        while (!check(TokenType.END) && !isAtEnd()) {
            if (check(TokenType.DECLARE)) {
                throw error(peek(), "Expected declaration before executable statements.");
            }

            statements.add(declaration());
        }
    }

    private void parseFooter() {
        consumeKeywords(
                "Expected 'END SCRIPT' to finish program",
                TokenType.END, TokenType.SCRIPT
        );
        if(tokens.get(current).type() != TokenType.EOF){
            throw error(peek(), "No statements allowed after 'END SCRIPT'.");
        }
    }


    private Stmt declaration() {
        return statement();
    }

    private Stmt varDeclaration() {
        if(!match(
                TokenType.INT_TYPE,
                TokenType.STRING_TYPE,
                TokenType.BOOL_TYPE,
                TokenType.FLOAT_TYPE,
                TokenType.CHAR_TYPE
        )) throw error(peek(), "Expected variable type.");
        TokenType type = previous().type();
        List<Token> names = new ArrayList<>();
        List<Expr> initializers = new ArrayList<>();
        Expr initializer;
        do{
            names.add(consume(TokenType.IDENTIFIER, "Expected variable name."));
            initializer = null;
            if(match(TokenType.EQUAL)){
                initializer = expression();
            }
            initializers.add(initializer);
        } while(match(TokenType.COMMA));
        consumeNewlines("Expected newline after variable declaration.");
        return new Stmt.Declare(names, initializers, type);
    }

    private Stmt statement() {
        if(match(TokenType.IF)) return ifStatement();
        if(match(TokenType.REPEAT)) return whenStatement();
        if(match(TokenType.FOR)) return forStatement();
        if(match(TokenType.PRINT)){
            consume(TokenType.COLON, "Expected ':' after 'print'.");
            return printStatement();
        }
        if(match(TokenType.SCAN)){
            consume(TokenType.COLON, "Expected ':' after 'scan'.");
            return scanStatement();
        }
        return expressionStatement();
    }

    private Stmt scanStatement() {
        List<Token> names = new ArrayList<>();
        consume(TokenType.IDENTIFIER, "Expected at least one variable");
        names.add(previous());
        while(match(TokenType.COMMA)){
            names.add(consume(TokenType.IDENTIFIER, "Expected variable name."));
        }
        consumeNewlines( "Expected newline after value.");
        return new Stmt.Scan(names);
    }

    private Stmt ifStatement(){
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'IF'.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expected ')' after if condition.");
        consumeNewlines("Expected newline after condition.");

        consumeKeywords("Expected 'START IF' at the end of condition block.", TokenType.START, TokenType.IF);
        Stmt thenBranch = new Stmt.Block(ifBlock());
        Stmt elseBranch = null;
        if(match(TokenType.ELSE)){
            if(match(TokenType.IF)){
                elseBranch = ifStatement();
            }else{
                consumeNewlines("Expected newline after 'ELSE'.");
                consumeKeywords("Expected 'START IF' after 'ELSE'.", TokenType.START, TokenType.IF);
                elseBranch = new Stmt.Block(ifBlock());
            }
        }
        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private List<Stmt> ifBlock(){
        List<Stmt> statements = new ArrayList<>();
        while(!check(TokenType.END) && !isAtEnd()) {
            if (check(TokenType.NEWLINE)) {
                advance();
                continue;
            }
            statements.add(statement());
        }
        consumeKeywords("Expected 'END IF' at the end of condition block.", TokenType.END, TokenType.IF);
        return statements;
    }

    private List<Stmt> whenBlock(){
        List<Stmt> statements = new ArrayList<>();
        while(!check(TokenType.END) && !isAtEnd()) {
            if (check(TokenType.NEWLINE)) {
                advance();
                continue;
            }
            statements.add(statement());
        }
        consumeKeywords("Expected 'END REPEAT' at the end of condition block.", TokenType.END, TokenType.REPEAT);
        return statements;
    }

    private List<Stmt> forBlock(){
        List<Stmt> statements = new ArrayList<>();
        while(!check(TokenType.END) && !isAtEnd()) {
            if (check(TokenType.NEWLINE)) {
                advance();
                continue;
            }
            statements.add(statement());
        }
        consumeKeywords("Expected 'END FOR' at the end of condition block.", TokenType.END, TokenType.FOR);
        return statements;
    }

    private Stmt whenStatement() {
        consume(TokenType.WHEN, "Expected 'WHEN' after 'REPEAT'.");
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'WHEN'.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Exprected ')' after condition.");
        consumeNewlines("Expected newline after condition.");
        consumeKeywords("Expected 'START REPEAT' at the end of condition block.", TokenType.START, TokenType.REPEAT);
        Stmt body = new Stmt.Block(whenBlock());

        return new Stmt.When(condition, body);
    }

    private Stmt forStatement() {
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'FOR'.");

        Stmt initializer;
        if (match(TokenType.COMMA)) {
            initializer = null;
        } else {
            if (match(TokenType.DECLARE)) {
                initializer = varDeclaration();
            } else {
                initializer = new Stmt.Expression(expression());
            }
            consume(TokenType.COMMA, "Expected ',' after loop initialization.");
        }

        Expr condition = null;
        if (!check(TokenType.COMMA)) {
            condition = expression();
        }

        consume(TokenType.COMMA, "Expected ',' after loop condition.");

        Expr increment = null;
        if (!check(TokenType.RIGHT_PAREN)) {
            increment = expression();
        }
        consume(TokenType.RIGHT_PAREN, "Expected ')' after clauses.");
        consumeNewlines("Expected newline after clauses.");
        consumeKeywords("Expected 'START FOR' to begin loop block.", TokenType.START, TokenType.FOR);
        List<Stmt> statements = forBlock();
        Stmt body = new Stmt.Block(statements);

        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
        }

        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.When(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    private Stmt printStatement() {
        Expr value = expression();
        consumeNewlines( "Expected newline after value.");
        return new Stmt.Print(value);
    }

    private Stmt expressionStatement() {
        Expr value = expression();
        consumeNewlines( "Expected newline after expression.");
        return new Stmt.Expression(value);
    }

    private Expr expression(){
        return assignment();
    }

    private Expr assignment() {
        Expr expr = or();

        if (match(TokenType.EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            throw error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr or(){
        Expr expr = and();

        while(match(TokenType.OR)){
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr and(){
        Expr expr = equality();

        while(match(TokenType.AND)){
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr equality(){
        Expr expr = comparison();

        while(match(TokenType.NOT_EQUAL, TokenType.EQUAL_EQUAL)){
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr comparison(){
        Expr expr = term();

        while(match(TokenType.GREATER, TokenType.GREATER_EQUAL,
                TokenType.LESS, TokenType.LESS_EQUAL)){
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr term(){
        Expr expr = factor();

        while(match(TokenType.PLUS, TokenType.MINUS, TokenType.AMPERSAND, TokenType.MOD)){
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr factor(){
        Expr expr = unary();

        while(match(TokenType.STAR, TokenType.SLASH)){
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr unary(){
        if(match(TokenType.NOT, TokenType.MINUS, TokenType.PLUS)){
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return primary();
    }

    private Expr primary(){
        if(match(TokenType.FALSE)) return new Expr.Literal(false);
        if(match(TokenType.TRUE)) return new Expr.Literal(true);
        if(match(TokenType.NULL)) return new Expr.Literal(null);
        if(match(TokenType.DOLLAR)) return new Expr.Literal("\n");

        if(match(TokenType.FLOAT_LITERAL, TokenType.INTEGER_LITERAL,
                TokenType.STRING_LITERAL, TokenType.CHAR_LITERAL)) {
            return new Expr.Literal(previous().literal());
        }
        if(match(TokenType.LEFT_PAREN)){
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expected ')' after expression.");
            return new Expr.Grouping(expr);
        }

        if(match(TokenType.IDENTIFIER)){
            return new Expr.Variable(previous());
        }
        throw error(peek(), "Expected expression.");
    }
    private boolean isAtEnd(){
        return peek().type() == TokenType.EOF;
    }

    private Token advance(){
        if (!isAtEnd()) current++;
        return previous();
    }

    private Token previous(){
        return tokens.get(current-1);
    }

    private Token peek(){
        return tokens.get(current);
    }

    private boolean match(TokenType ... types){
        for(TokenType type: types){
            if(check(type)){
                advance();
                return true;
            }
        }
        return false;
    }

    private void consumeKeywords(String message, TokenType ... type){
        for(TokenType t: type){
            if(!check(t)){
                throw error(peek(), message);
            }
            advance();
        }
        consumeNewlines("Expected newline after keywords.");
    }

    private boolean check(TokenType type){
        if(isAtEnd()) return false;
        return peek().type() == type;
    }


    private Token consume(TokenType type, String message){
        if(check(type)) return advance();
        throw error(peek(), message);
    }

    private void consumeNewlines(String message) {
        if(isAtEnd()) return;
        if(!check(TokenType.NEWLINE)) throw error(peek(), message);
        while(check(TokenType.NEWLINE)) {
            advance();
        }
    }

    private ParseError error(Token token, String message){
        errorManager.syntaxError(token, message);
        return new ParseError();
    }

    private void synchronize(){
        advance();

        while(!isAtEnd()){
            if(previous().type() == TokenType.NEWLINE) return;

            switch(peek().type()){
                case WHEN:
                case IF:
                case FOR:
                case DECLARE:
                case START:
                case PRINT:
                    return;
            }
            advance();
        }
    }
}
