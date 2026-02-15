package Lexor.interpreter;

import Lexor.err.ErrorManager;
import Lexor.err.RuntimeError;
import Lexor.lexer.Token;
import Lexor.lexer.TokenType;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Environment enclosing;
    private final Map<String, Variable> values = new HashMap<>();

    public Environment(){
        this.enclosing = null;
    }

    public Environment(Environment enclosing){
        this.enclosing = enclosing;
    }

    void define(Token name, Object value, TokenType type){
        if(value != null) verifyTypeMatch(name, type, value);
        values.put(name.lexeme(), new Variable(type, value));
    }

    Object get(Token name) {
        if(values.containsKey(name.lexeme())){
            Variable var = values.get(name.lexeme());
            if (var.value() == null) {
                throw new RuntimeError(name, "Variable '" + name.lexeme() + "' has not been initialized.");
            }
            return var.value();
        }
        if(enclosing != null){return enclosing.get(name);}
        throw new RuntimeError(name,"Undefined variable '" + name.lexeme() + "'");
    }

    public void assign(Token name, Object value) {
        if(values.containsKey(name.lexeme())) {
            Variable var = values.get(name.lexeme());
            if (value != null) {
                verifyTypeMatch(name, var.type(), value);
            }
            values.put(name.lexeme(), new Variable(var.type(), value));
            return;
        }
        if(enclosing != null){
            enclosing.assign(name, value);
            return;
        }
        throw new RuntimeError(name,"Undefined variable '" + name.lexeme() + "'");
    }

    private void verifyTypeMatch(Token name, TokenType expected, Object value) {
        boolean isValid = false;

        switch (expected) {
            case INT_TYPE:
                isValid = (value instanceof Integer);
                break;
            case FLOAT_TYPE:
                isValid = (value instanceof Double || value instanceof Float);
                break;
            case CHAR_TYPE:
                isValid = (value instanceof Character);
                break;
            case BOOL_TYPE:
                isValid = (value instanceof Boolean);
                break;
            case STRING_TYPE:
                isValid = (value instanceof String);
                break;
        }

        if (!isValid) {
            throw new RuntimeError(name, "Type mismatch: Cannot assign " +
                    value.getClass().getSimpleName() + " to variable of type " + expected);
        }
    }
}
