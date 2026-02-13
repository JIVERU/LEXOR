package Lexor.parser;

import Lexor.parser.ast.Expr;
import Lexor.parser.ast.Stmt;

import java.util.List;

public class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {
    public String print(List<Stmt> statements) {
        StringBuilder builder = new StringBuilder();
        for (Stmt statement : statements) {
            builder.append(statement.accept(this)).append("\n");
        }
        return builder.toString();
    }

    public String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return expr.name.lexeme() + " = " + print(expr.value);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme(), expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "NULL";
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme(), expr.right);
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return expr.name.lexeme();
    }

    private String parenthesize(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();
        builder.append("(").append(name);
        for (Expr expr : exprs) {
            builder.append(" ").append(expr.accept(this));
        }
        return builder.append(")").toString();
    }

    @Override
    public String visitIfBlockStmt(Stmt.IfBlock stmt) {
        StringBuilder builder = new StringBuilder();
        builder.append("if block {\n");
        for (Stmt statement : stmt.statements) {
            builder.append("  ").append(statement.accept(this)).append("\n");
        }
        builder.append("}");
        return builder.toString();
    }

    @Override
    public String visitRepeatBlockStmt(Stmt.RepeatBlock stmt) {
        StringBuilder builder = new StringBuilder();
        builder.append("repeat block {\n");
        for (Stmt statement : stmt.statements) {
            builder.append("  ").append(statement.accept(this)).append("\n");
        }
        builder.append("}");
        return builder.toString();
    }

    @Override
    public String visitIfStmt(Stmt.If stmt) {
        StringBuilder builder = new StringBuilder();
        builder.append("if (").append(print(stmt.condition)).append(") ");
        builder.append(stmt.thenBranch.accept(this));
        if (stmt.elseBranch != null) {
            builder.append(" else ").append(stmt.elseBranch.accept(this));
        }
        return builder.toString();
    }

    @Override
    public String visitWhenStmt(Stmt.When stmt) {
        return "when (" + print(stmt.condition) + ") " + stmt.body.accept(this);
    }

    @Override
    public String visitExpressionStmt(Stmt.Expression stmt) {
        return print(stmt.expression);
    }

    @Override
    public String visitPrintStmt(Stmt.Print stmt) {
        return "print " + print(stmt.expression);
    }

    @Override
    public String visitDeclareStmt(Stmt.Declare stmt) {
        String name;
        String init = null;
        StringBuilder builder = new StringBuilder();
        for(Expr initializer : stmt.initializer) {
            name = stmt.names.get(stmt.initializer.indexOf(initializer)).lexeme();
            if(initializer != null){
                init = print(initializer);
            }
            builder.append(name).append(" = ").append(init).append(" ");
        }
        return builder.toString();
    }
}