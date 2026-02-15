package Lexor.interpreter;

import Lexor.lexer.TokenType;

public record Variable(
        TokenType type,
        Object value
) {}
