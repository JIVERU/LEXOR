package Lexor.parser.ast;
import Lexor.lexer.TokenType;

import java.util.List;
import Lexor.lexer.Token;

public abstract class Stmt {
    public interface Visitor<R> {
        R visitIfStmt(If stmt);
        R visitWhenStmt(When stmt);
        R visitBlockStmt(Block stmt);
        R visitExpressionStmt(Expression stmt);
        R visitPrintStmt(Print stmt);
        R visitScanStmt(Scan stmt);
        R visitDeclareStmt(Declare stmt);
    }
    public static class If extends Stmt {
        public final Expr condition;
        public final Stmt thenBranch;
        public final Stmt elseBranch;

        public If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        @Override
        public <R> void accept(Visitor<R> visitor) {
            visitor.visitIfStmt(this);
        }
    }
    public static class When extends Stmt {
        public final Expr condition;
        public final Stmt body;

        public When(Expr condition, Stmt body) {
            this.condition = condition;
            this.body = body;
        }

        @Override
        public <R> void accept(Visitor<R> visitor) {
            visitor.visitWhenStmt(this);
        }
    }
    public static class Block extends Stmt {
        public final List<Stmt> statements;

        public Block(List<Stmt> statements) {
            this.statements = statements;
        }

        @Override
        public <R> void accept(Visitor<R> visitor) {
            visitor.visitBlockStmt(this);
        }
    }
    public static class Expression extends Stmt {
        public final Expr expression;

        public Expression(Expr expression) {
            this.expression = expression;
        }

        @Override
        public <R> void accept(Visitor<R> visitor) {
            visitor.visitExpressionStmt(this);
        }
    }
    public static class Print extends Stmt {
        public final Expr expression;

        public Print(Expr expression) {
            this.expression = expression;
        }

        @Override
        public <R> void accept(Visitor<R> visitor) {
            visitor.visitPrintStmt(this);
        }
    }
    public static class Scan extends Stmt {
        public final List<Token> names;

        public Scan(List<Token> names) {
            this.names = names;
        }

        @Override
        public <R> void accept(Visitor<R> visitor) {
            visitor.visitScanStmt(this);
        }
    }
    public static class Declare extends Stmt {
        public final List<Token> names;
        public final List<Expr> initializer;
        public final TokenType type;

        public Declare(List<Token> names, List<Expr> initializer, TokenType type) {
            this.names = names;
            this.initializer = initializer;
            this.type = type;
        }

        @Override
        public <R> void accept(Visitor<R> visitor) {
            visitor.visitDeclareStmt(this);
        }
    }
    public abstract <R> void accept(Visitor<R> visitor);
}
