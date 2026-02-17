package Lexor;

import Lexor.err.ErrorManager;
import Lexor.interpreter.Interpreter;
import Lexor.lexer.Lexer;
import Lexor.lexer.Token;
import Lexor.parser.Parser;
import Lexor.parser.ast.Stmt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Lexor {
    private static final ErrorManager errorManager = new ErrorManager();
    private static final Interpreter interpreter = new Interpreter(errorManager);
    static void main(String[] args) throws IOException{
        if (args.length == 1){
            String filePath = args[0];

            if (!filePath.toLowerCase().endsWith(".lxr")) {
                System.err.println("Error: Invalid file extension.");
                System.err.println("LEXOR can only execute files ending with '.lxr'");
                System.exit(65);
            }
            runFile(args[0]);
        }else{
            System.out.println("Usage: lexor <file>");
            System.exit(64);
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
