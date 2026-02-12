package Lexor;

import Lexor.err.ErrorManager;
import Lexor.lexer.Lexer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Lexor {
    private static final ErrorManager errorManager = new ErrorManager();
    static void main(String[] args) throws IOException{
        if(args.length > 1){
            System.out.println("Usage: lexor <file>");
        }else if (args.length == 1){
            runFile(args[0]);
        }else{
//            runPrompt();
        }
    }

    static void runPrompt(String[] args) {

    }

    static void runFile(String filepath) throws IOException {
        Path filePath = Paths.get(filepath);

        String content = Files.readString(filePath);
        run(content);
    }
    static void run(String input) {
        Lexer lexer = new Lexer(input, errorManager);
        lexer.scanTokens().forEach(System.out::println);
    }
}
