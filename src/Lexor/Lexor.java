package Lexor;

import Lexor.err.ErrorManager;
import Lexor.interpreter.Interpreter;
import Lexor.lexer.Lexer;
import Lexor.lexer.Token;
import Lexor.parser.Parser;
import Lexor.parser.ast.Stmt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Lexor {
    private static final ErrorManager errorManager = new ErrorManager();
    private static final Interpreter interpreter = new Interpreter(errorManager);
    static void main(String[] args) throws IOException{
        if(args.length > 1){
            System.out.println("Usage: lexor <file>");
        }else if (args.length == 1){
            runFile(args[0]);
        }else{
            runPrompt();
        }
    }

    static void runPrompt() throws IOException{
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            run(line);
            errorManager.reset();
        }
    }

    static void runFile(String filepath) throws IOException {
        Path filePath = Paths.get(filepath);

        String content = Files.readString(filePath);
        run(content);
        if(errorManager.hadError()) System.exit(65);
        if(errorManager.hadRuntimeError()) System.exit(70);
    }
    static void run(String input) {
        Lexer lexer = new Lexer(input, errorManager);
        List<Token> tokens = lexer.scanTokens();
        Parser parser = new Parser(tokens, errorManager);
        List<Stmt> statements = parser.parse();
        if(errorManager.hadError()) return;
        interpreter.interpret(statements);
    }
}
