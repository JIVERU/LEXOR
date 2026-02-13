package Lexor.parser;

import Lexor.err.ErrorManager;
import Lexor.err.ParseError;
import Lexor.lexer.Token;
import Lexor.lexer.TokenType;
import Lexor.parser.ast.Expr;
import Lexor.parser.ast.Stmt;

import java.io.InputStreamReader;
import java.util.ArrayList;
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
        consumeNewlines();
        keywordsMatch(
                "Expected 'SCRIPT AREA' at the start of file",
                TokenType.SCRIPT, TokenType.AREA
        );

        consumeNewlines();
        keywordsMatch(
                "Expected 'START SCRIPT' after 'SCRIPT AREA'",
                TokenType.START, TokenType.SCRIPT
        );
    }

    private void parseDeclarations(List<Stmt> statements) {
        consumeNewlines();

        while (check(TokenType.DECLARE) && !isAtEnd()) {
            statements.add(varDeclaration());
            consumeNewlines();
        }
    }

    private void parseExecutableStatements(List<Stmt> statements) {
        while (!check(TokenType.END) && !isAtEnd()) {
            consumeNewlines();

            if (check(TokenType.DECLARE)) {
                throw error(peek(), "Expected declaration before executable statements.");
            }

            statements.add(declaration());
        }
    }

    private void parseFooter() {
        keywordsMatch(
                "Expected 'END SCRIPT' to finish program",
                TokenType.END, TokenType.SCRIPT
        );
        consumeNewlines();
    }


    private Stmt declaration() {
        try {
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt varDeclaration() {
        consume(TokenType.DECLARE, "Expected 'DECLARE'");
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
            if(match(TokenType.EQUAL)) initializer = expression();
            initializers.add(initializer);
        }while(match(TokenType.COMMA));
        consume(TokenType.NEWLINE, "Expected newline after variable declaration.");
        return new Stmt.Declare(names, initializers, type);
    }

    private Stmt statement() {
        if(match(TokenType.PRINT)){
            consume(TokenType.COLON, "Expected ':' after 'print'.");
            return printStatement();
        }
        return expressionStatement();
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(TokenType.NEWLINE, "Expected newline after value.");
        return new Stmt.Print(value);
    }

    private Stmt expressionStatement() {
        Expr value = expression();
        consume(TokenType.NEWLINE, "Expected newline after expression.");
        return new Stmt.Expression(value);
    }

    private Expr expression(){
        return assignment();
    }

    private Expr assignment() {
        Expr expr = equality();

        if (match(TokenType.EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
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
        return peek().type() == TokenType.EOF || peek().type() == TokenType.END;
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

    private void keywordsMatch(String message, TokenType ... type){
        for(TokenType t: type){
            if(!check(t) && !isAtEnd()){
                throw error(peek(), message);
            }
            advance();
        }
    }

    private boolean check(TokenType type){
        if(isAtEnd()) return false;
        return peek().type() == type;
    }


    private Token consume(TokenType type, String message){
        if(check(type)) return advance();
        throw error(peek(), message);
    }

    private ParseError error(Token token, String message){
        errorManager.syntaxError(token, message);
        return new ParseError();
    }

    private void consumeNewlines() {
        while (check(TokenType.NEWLINE) && !isAtEnd()) {
            advance();
        }
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
