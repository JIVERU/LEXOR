package Lexor.lexer;


import Lexor.err.ErrorManager;

import java.util.*;

public class Lexer {
    private final ErrorManager errorManager;
    private final String source;
    private List<Token> tokens = new ArrayList<>();
    private static Map<String, TokenType> keywords;
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int column = 1;

    static{
        keywords = new HashMap<>();
        keywords.put("IF", TokenType.IF);
        keywords.put("ELSE", TokenType.ELSE);
        keywords.put("ELSE IF", TokenType.ELIF);
        keywords.put("FOR", TokenType.FOR);
        keywords.put("WHEN", TokenType.WHEN);
        keywords.put("AND", TokenType.AND);
        keywords.put("OR", TokenType.OR);
        keywords.put("NOT", TokenType.NOT);
        keywords.put("NULL", TokenType.NULL);
        keywords.put("PRINT", TokenType.PRINT);
        keywords.put("DECLARE", TokenType.DECLARE);
        keywords.put("SCAN", TokenType.SCAN);
        keywords.put("SCRIPT", TokenType.SCRIPT);
        keywords.put("AREA", TokenType.AREA);
        keywords.put("START", TokenType.START);
        keywords.put("END", TokenType.END);
        keywords.put("REPEAT", TokenType.REPEAT);
        keywords.put("INT", TokenType.INT_TYPE);
        keywords.put("FLOAT", TokenType.FLOAT_TYPE);
        keywords.put("STRING", TokenType.STRING_TYPE);
        keywords.put("BOOL", TokenType.BOOL_TYPE);
        keywords.put("CHAR", TokenType.CHAR_TYPE);
    }


    public Lexer(String source, ErrorManager errormanager) {
        this.source = source;
        this.errorManager = errormanager;
    }

    public List<Token> scanTokens(){
        while(!isAtEnd()){
            column = start + 1;
            start = current;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", null, line, column));
        return tokens;
    }

    private void scanToken(){
        char c = advance();
        switch(c){
            case '(': addToken(TokenType.LEFT_PAREN); break;
            case ')': addToken(TokenType.RIGHT_PAREN); break;
            case '[': escapeSequence(); break;
            case '+': addToken(TokenType.PLUS); break;
            case '-': addToken(TokenType.MINUS); break;
            case '*': addToken(TokenType.STAR); break;
            case '/': addToken(TokenType.SLASH); break;
            case ',': addToken(TokenType.COMMA); break;
            case '&': addToken(TokenType.CONCAT); break;
            case '$': addToken(TokenType.CARRIAGE_RETURN); break;
            case '=': addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL); break;
            case '>': addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER); break;
            case '<':
                if(match('=')){
                    addToken(TokenType.LESS_EQUAL);
                }else if(match('>')){
                    addToken(TokenType.NOT_EQUAL);
                }else{
                    addToken(TokenType.LESS);
                }
                break;
            case '%':
                if(match('%') ){
                    while(peek() != '\n' && !isAtEnd()) advance();
                }else{
                    addToken(TokenType.MOD);
                }
                break;
            case '"': string(); break;
            case ':': addToken(TokenType.COLON); break;
            case '\'': character(); break;
            case ' ':
            case '\r':
            case '\t': break;
            case '\n': line++; break;
            default:
                if(isDigit(c)){
                    number();
                }else if(isAlpha(c)){
                    identifier();
                }else{
                    errorManager.lexicalError(line, column, c, "Unexpected character.");
                }
        }
    }

    private boolean isDigit(char c){
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c){
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private boolean isAlphaNumeric(char c){
        return isAlpha(c) || isDigit(c);
    }

    private void character(){
        if (isAtEnd()) {
            errorManager.error(line, column, "Unterminated character.");
            return;
        }
        char value = advance();

        if (peek() == '\'') {
            advance();
            addToken(TokenType.CHAR_LITERAL, value);
        } else {
            errorManager.lexicalError(line, column, value,"Unterminated character literal. Expected single quote.");
        }
    }

    private void string() {
        StringBuilder value = new StringBuilder();

        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '[') {
                value = escapeSequence(value);
                if (value == null) return;
            } else {
                value.append(peek());
                advance();
            }
        }

        if (isAtEnd()) {
            errorManager.error(line, column, "Unterminated string.");
            return;
        }

        advance();
        addToken(TokenType.STRING_LITERAL, value.toString());
    }

    private void escapeSequence(){
        StringBuilder value = escapeSequence(new StringBuilder());
        if (value == null) return;
        addToken(TokenType.STRING_LITERAL, value.toString());
    }

    private StringBuilder escapeSequence(StringBuilder value){
        if (isAtEnd()) {
            errorManager.lexicalError(line, column, peek(), "Unterminated string inside escape sequence.");
            return null;
        }
        char escapeCode = peek();
        advance();

        if (peek() != ']') {
            errorManager.lexicalError(line, column, peek(),"Expected ']' to close escape sequence");
            return null;
        }
        advance();

        switch (escapeCode) {
            case 'n': value.append('\n'); break;
            case 't': value.append('\t'); break;
            case '$': value.append('\r'); break;
            case '"': value.append('"');  break;
            case '[': value.append('[');  break;
            case ']': value.append(']');  break;
            default:
                value.append(escapeCode);
        }
        return value;
    }

    private void number(){
        while(isDigit(peek()) && !isAtEnd()) advance();
        if (peek() == '.' && isDigit(peekNext())) {
            do advance();
            while (isDigit(peek()));
            addToken(TokenType.FLOAT_LITERAL, Double.parseDouble(source.substring(start, current)));
        } else {
            addToken(TokenType.INTEGER_LITERAL, Integer.parseInt(source.substring(start, current)));
        }
    }

    private void identifier(){
        while(isAlphaNumeric(peek())){
            advance();
        }
        String text = source.substring(start, current);
        TokenType type = keywords.get(text.strip());
        addToken(type != null ? type : TokenType.IDENTIFIER);
    }

    private boolean match(char expected){
        if(isAtEnd()) return false;
        if(source.charAt(current) != expected) return false;
        current++;
        return true;
    }

    private char peek(){
        if(isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext(){
        if(current+1 >= source.length()) return '\0';
        return source.charAt(current+1);
    }

    private void addToken(TokenType type){
        addToken(type,null);
    }

    private void addToken(TokenType type, Object literal){
        tokens.add(new Token(type, source.substring(start, current), literal, line, column));
    }

    private char advance(){
        return source.charAt(current++);
    }

    private boolean isAtEnd(){
        return current >= source.length();
    }
}
