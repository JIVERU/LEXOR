package Lexor.parser.ast;

import java.util.List;
import Lexor.lexer.Token;
import Lexor.lexer.TokenType;

public abstract class Stmt {
    public interface Visitor<R> {
        R visitRepeatBlockStmt(RepeatBlock stmt);
        R visitIfBlockStmt(IfBlock stmt);
        R visitIfStmt(If stmt);
        R visitWhenStmt(When stmt);
        R visitExpressionStmt(Expression stmt);
        R visitPrintStmt(Print stmt);
        R visitDeclareStmt(Declare stmt);
    }
    public static class RepeatBlock extends Stmt {
        public final List<Stmt> statements;

        public RepeatBlock(List<Stmt> statements) {
            this.statements = statements;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
        return visitor.visitRepeatBlockStmt(this);
        }
    }
    public static class IfBlock extends Stmt {
        public final List<Stmt> statements;

        public IfBlock(List<Stmt> statements) {
            this.statements = statements;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
        return visitor.visitIfBlockStmt(this);
        }
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
        public <R> R accept(Visitor<R> visitor) {
        return visitor.visitIfStmt(this);
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
        public <R> R accept(Visitor<R> visitor) {
        return visitor.visitWhenStmt(this);
        }
    }
    public static class Expression extends Stmt {
        public final Expr expression;

        public Expression(Expr expression) {
            this.expression = expression;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
        return visitor.visitExpressionStmt(this);
        }
    }
    public static class Print extends Stmt {
        public final Expr expression;

        public Print(Expr expression) {
            this.expression = expression;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
        return visitor.visitPrintStmt(this);
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
        public <R> R accept(Visitor<R> visitor) {
        return visitor.visitDeclareStmt(this);
        }
    }
    public abstract <R> R accept(Visitor<R> visitor);
}
