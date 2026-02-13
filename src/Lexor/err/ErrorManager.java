package Lexor.err;

import Lexor.lexer.Token;
import Lexor.lexer.TokenType;
import java.util.ArrayList;
import java.util.List;

public class ErrorManager {
    private final List<Error> errors;

    private boolean hadRuntimeError = false;
    private boolean hadError = false;

    public ErrorManager() {
        this.errors = new ArrayList<>();
    }

    public void lexicalError(int line, int column, char character, String message) {
        report(line, column, "", message + ": " + character, ErrorType.LEXICAL);
        hadError = true;
    }

    public void error(int line, int column, String message){
        report(line, column,"", message, ErrorType.SYNTAX);
    }

    public void syntaxError(Token token, String message) {
        if (token.type() == TokenType.EOF) {
            report(token.line(), token.column(), " at end", message, ErrorType.SYNTAX);
        } else {
            if( token.type() == TokenType.NEWLINE){
                report(token.line(), token.column(), " at the end of statement", message, ErrorType.SYNTAX);
            }
            report(token.line(), token.column(), " at '" + token.lexeme() + "'", message, ErrorType.SYNTAX);
        }
        hadError = true;
    }

    public void runtimeError(RuntimeError error) {
        report(error.getToken().line(), error.getToken().column(), " at '" + error.getToken().lexeme() + "'", error.getMessage(), ErrorType.RUNTIME);
        errors.add(new Error(
                error.getMessage(),
                error.getToken().line(),
                error.getToken().column(),
                ErrorType.RUNTIME
        ));

        hadRuntimeError = true;
    }

    private void report(int line, int column, String where, String message, ErrorType type) {
        System.err.printf("[line %d] %s ERROR%s: %s%n", line, type.toString(), where, message);
//        printVisualLocation(column);
        errors.add(new Error(message, line, column, type));

        hadError = true;
    }

    private void printVisualLocation(int column) {
        if (column > 0) {
            for (int i = 0; i < column - 1; i++) {
                System.err.print("~");
            }
            System.err.println("^"); // Use println to end the line
        }
    }

    public boolean hadErrors() {
        return hadError;
    }

    public boolean hadRuntimeError() {
        return hadRuntimeError;
    }

    public void reset() {
        errors.clear();
        hadError = false;
        hadRuntimeError = false;
    }
}