package Lexor.interpreter;


import Lexor.err.ErrorManager;
import Lexor.err.RuntimeError;
import Lexor.lexer.Lexer;
import Lexor.lexer.Token;
import Lexor.lexer.TokenType;
import Lexor.parser.ast.Expr;
import Lexor.parser.ast.Stmt;

import java.util.List;
import java.util.Scanner;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    private final ErrorManager errorManager;
    private Environment environment;

    public Interpreter(ErrorManager errorManager) {
        this.environment = new Environment(errorManager);
        this.errorManager = errorManager;
    }

    public void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError e) {
            errorManager.runtimeError(e);
        }
    }

    private void execute(Stmt statement) {
        statement.accept(this);
    }

    private void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;
            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    private String stringify(Object object) {
        if (object == null) return "NULL";
        if (object instanceof Boolean) return (boolean) object ? "TRUE" : "FALSE";
        if (object instanceof Character) return object.toString();
        if (object instanceof Variable) return ((Variable) object).value().toString();

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        if(expr.name.type() == TokenType.IDENTIFIER) environment.assign(expr.name, value);
        else throw new RuntimeError(expr.name, "Invalid assignment target.");
        return value;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        return switch (expr.operator.type()) {
            case MINUS -> {
                checkNumberOperand(expr.operator, left, right);
                if (left instanceof Double || right instanceof Double) {
                    yield toDouble(left) - toDouble(right);
                }
                yield (int) left - (int) right;
            }
            case PLUS -> {
                checkNumberOperand(expr.operator, left, right);
                if (left instanceof Double || right instanceof Double) {
                    yield toDouble(left) + toDouble(right);
                }
                yield (int) left + (int) right;
            }
            case SLASH -> {
                checkNumberOperand(expr.operator, left, right);
                if (left instanceof Double || right instanceof Double) {
                    yield toDouble(left) / toDouble(right);
                }
                if ((int) right == 0) throw new RuntimeError(expr.operator, "Cannot divide by zero.");
                yield (int) left / (int) right;
            }
            case STAR -> {
                checkNumberOperand(expr.operator, left, right);
                if (left instanceof Double || right instanceof Double) {
                    yield toDouble(left) * toDouble(right);
                }
                yield (int) left * (int) right;
            }
            case GREATER -> {
                checkNumberOperand(expr.operator, left, right);
                yield toDouble(left) > toDouble(right);
            }
            case GREATER_EQUAL -> {
                checkNumberOperand(expr.operator, left, right);
                yield toDouble(left) >= toDouble(right);
            }
            case LESS -> {
                checkNumberOperand(expr.operator, left, right);
                yield toDouble(left) < toDouble(right);
            }
            case LESS_EQUAL -> {
                checkNumberOperand(expr.operator, left, right);
                yield toDouble(left) <= toDouble(right);
            }
            case EQUAL_EQUAL -> isEqual(left, right);
            case NOT_EQUAL -> !isEqual(left, right);
            case MOD -> {
                checkNumberOperand(expr.operator, left, right);
                if (left instanceof Double || right instanceof Double) {
                    yield toDouble(left) % toDouble(right);
                }
                yield (int) left % (int) right;
            }
            case AMPERSAND -> stringify(left) + stringify(right);
            default -> null;
        };
    }

    private double toDouble(Object object) {
        if (object instanceof Number) return ((Number) object).doubleValue();
        throw new RuntimeException("Not a number.");
    }

    private void checkNumberOperand(Token operator, Object left, Object right) {
        if (!(left instanceof Number) || !(right instanceof Number))
            throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private boolean isEqual(Object left, Object right) {
        if (left == null && right == null) return true;
        if (left == null) return false;
        return left.equals(right);
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if(expr.operator.type() == TokenType.OR) if(isTruthy(left)) return true;
        else {if(!isTruthy(left)) return false;}
        return  evaluate(expr.right);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object operand = evaluate(expr.right);
        return switch (expr.operator.type()) {
            case NOT -> !isTruthy(operand);
            case MINUS -> {
                checkNumberOperand(expr.operator, operand);
                if (operand instanceof Double) yield -(Double) operand;
                yield -(int) operand;
            }
            case PLUS -> operand;
            default -> null;
        };
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Number) return;
        throw new RuntimeError(operator, "Operand must be a number");
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (Boolean) object;
        throw new RuntimeError(null, "Not a boolean.");
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if(isTruthy(evaluate(stmt.condition))) execute(stmt.thenBranch);
        else if(stmt.elseBranch != null) execute(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitWhenStmt(Stmt.When stmt) {
        while(isTruthy(evaluate(stmt.condition))){
            execute(stmt.body);
        }
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        System.out.print(stringify(evaluate(stmt.expression)));
        return null;
    }

    @Override
    public Void visitScanStmt(Stmt.Scan stmt) {
        Scanner scanner = new Scanner(System.in);
        String line = scanner.nextLine();
        Lexer lexer = new Lexer(line, errorManager);

        List<Token> valueTokens = lexer.scanTokens().stream()
                .filter(t -> t.type() != TokenType.EOF && t.type() != TokenType.COMMA && t.type() != TokenType.NEWLINE)
                .toList();

        if (valueTokens.size() != stmt.names.size()) {
            throw new RuntimeError(stmt.names.getFirst(),
                    "Expected " + stmt.names.size() + " inputs, but got " + valueTokens.size() + ".");
        }

        Token value;
        for (Token token : stmt.names) {
            value = valueTokens.get(stmt.names.indexOf(token));
            environment.assign(token, value.literal());
        }
        return null;
    }

    @Override
    public Void visitDeclareStmt(Stmt.Declare stmt) {
        Object value = null;
        Expr initializer;
        TokenType type = stmt.type;
        for (Token token : stmt.names) {
            initializer = stmt.initializer.get(stmt.names.indexOf(token));
            if (initializer != null) {
                value = evaluate(initializer);
            }
            environment.define(token, value, type);
        }
        return null;
    }
}