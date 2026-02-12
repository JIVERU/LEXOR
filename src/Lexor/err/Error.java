package Lexor.err;

public record Error (
        String message,
        int line,
        int column,
        ErrorType errorType
){}
