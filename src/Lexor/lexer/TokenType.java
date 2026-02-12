package Lexor.lexer;

public enum TokenType {
    // Single-character tokens.
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    MINUS, PLUS, SLASH, STAR, MOD, CONCAT, CARRIAGE_RETURN,
    COMMA, COLON,

    // One or two character tokens.
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL, NOT_EQUAL,
    LESS, LESS_EQUAL,

    // Literals.
    IDENTIFIER, FLOAT_LITERAL, INTEGER_LITERAL, CHAR_LITERAL, STRING_LITERAL,

    // Type keywords
    INT_TYPE, FLOAT_TYPE, CHAR_TYPE, STRING_TYPE, BOOL_TYPE,


    // keywords
    SCRIPT, AREA, START, END,
    REPEAT, DECLARE, NULL,
    IF, ELSE, ELIF,
    AND, NOT, OR, PRINT, SCAN,
    FALSE, TRUE,
    FOR, WHEN,

    EOF
}
