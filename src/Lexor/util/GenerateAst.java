package Lexor.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
    static void main(String[] args) throws IOException {
        if(args.length != 1){
            System.err.println("Usage: java GenerateAst <output directory>");
            System.exit(64);
        }
        String outputDir = args[0];
        defineAst(outputDir, "Expr", Arrays.asList(
                "Assign:Token name, Expr value",
                "Binary:Expr left, Token operator, Expr right",
                "Grouping:Expr expression",
                "Literal:Object value",
                "Logical:Expr left, Token operator, Expr right",
//                "Ternary:Expr condition, Expr thenBranch, Expr elseBranch",
                "Unary:Token operator, Expr right",
                "Variable:Token name"
        ));
        defineAst(outputDir, "Stmt", Arrays.asList(
                "If: Expr condition, Stmt thenBranch, Stmt elseBranch",
                "When: Expr condition, Stmt body",
                "Block: List<Stmt> statements",
                "Expression: Expr expression",
                "Print: Expr expression",
                "Declare: List<Token> names, List<Expr> initializer, TokenType type"
        ));
    }

    private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException{
        String path = outputDir + "/" + baseName + ".java";
        try (PrintWriter writer = new PrintWriter(path, StandardCharsets.UTF_8)) {
            writer.println("package Lexor.parser.ast;");
            writer.println("import Lexor.lexer.TokenType;");
            writer.println("import java.util.List;");
            writer.println("import Lexor.lexer.Token;");
//            writer.println("import Lexor.parser.Expr;");
            writer.println();
            writer.println("public abstract class " + baseName + " {");

            defineVisitor(writer, baseName, types);

            for(String type : types){
                String className = type.split(":")[0].trim();
                String fields = type.split(":")[1].trim();
                defineType(writer, baseName, className, fields);
            }

            writer.println("    public abstract <R> R accept(Visitor<R> visitor);");
            writer.println("}");
        }
    }

    private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
        writer.println("    public interface Visitor<R> {");

        for(String type : types){
            String typeName = type.split(":")[0].trim();
            writer.println("        R visit" +  typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");
        }

        writer.println("    }");
    }

    private static void defineType(PrintWriter writer, String baseName, String className, String fieldList){
        writer.println("    public static class " + className + " extends " + baseName + " {");
        String[] fields = fieldList.split(",");
        for (String field : fields) {
            field = field.trim();
            writer.println("        public final " + field + ";");
        }

        writer.println();
        writer.println("        public " + className + "(" + fieldList + ") {");
        for(String field : fields){
            field = field.trim();
            String name = field.split(" ")[1];
            writer.println("            this." + name + " = " + name + ";");
        }
        writer.println("        }");

        writer.println();
        writer.println("        @Override");
        writer.println("        public <R> R accept(Visitor<R> visitor) {");
        writer.println("        return visitor.visit" + className + baseName + "(this);");
        writer.println("        }");

        writer.println("    }");
    }
}

