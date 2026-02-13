package Lexor.interpreter;

import Lexor.err.ErrorManager;
import Lexor.err.RuntimeError;
import Lexor.lexer.Token;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();
    private final ErrorManager errorManager;

    public Environment(ErrorManager errorManager){
        this.errorManager = errorManager;
        this.enclosing = null;
    }

    public Environment(Environment enclosing, ErrorManager errorManager){
        this.errorManager = errorManager;
        this.enclosing = enclosing;
    }

    void define(String name, Object value){values.put(name, value);}

    Object get(Token name) {
        if(values.containsKey(name.lexeme())){return values.get(name.lexeme());}
        if(enclosing != null){return enclosing.get(name);}
        errorManager.runtimeError(new RuntimeError(name,"Undefined variable '" + name.lexeme() + "'."));
        return null;
    }

    public void assign(Token name, Object value) {
        if(values.containsKey(name.lexeme())) {
            values.put(name.lexeme(), value);
            return;
        }
        errorManager.runtimeError(new RuntimeError(name,"Undefined variable '" + name.lexeme() + "'."));
    }
}
