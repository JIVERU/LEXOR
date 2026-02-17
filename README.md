# LEXOR Programming Language Interpreter

Language Specification of LEXOR Programming Language
Introduction
LEXOR is a strongly – typed programming language developed to teach Senior High School students the basics of
programming. LEXOR is a pure interpreter.

## Project Structure
```
LEXOR-Interpreter/
├── src/
│   └── Lexor/
│       ├── err/                   # Error Handling
│       │   ├── ErrorManager.java  # Tracks and reports syntax/runtime errors
│       │   ├── ParseError.java    # Internal parsing exception
│       │   └── RuntimeError.java  # Exception for execution-time failures
│       ├── interpreter/           # Execution Engine
│       │   ├── Interpreter.java   # AST-walking visitor implementation
│       │   ├── Environment.java   # Manages variable memory and scoping
│       │   └── Variable.java      # Record for Type-checked variable storage
│       ├── lexer/                 # Lexical Analysis
│       │   ├── Lexer.java         # Converts source text into Tokens
│       │   ├── Token.java         # Token data structure
│       │   └── TokenType.java     # Enum for all LEXOR reserved words/symbols
│       ├── parser/                # Syntax Analysis
│       │   ├── Parser.java        # Recursive Descent Parser
│       │   └── ast/               # Abstract Syntax Tree (AST) Node definitions
│       │       ├── Expr.java      # Expression nodes (Binary, Unary, Literal)
│       │       └── Stmt.java      # Statement nodes (If, When, Print, Declare)
│       ├──Lexor.java              # Entry point
|       ├── my_program             # Test Script
|       ├── Tests/                 # Quality Assurance
|         └── InterpreterTest.java # JUnit 5 tests
├── LICENSE                        # MIT License
└── README.md                      # Project documentation
```
## Language Grammar
Program Structure:
- all code starts with SCRIPT AREA
- all code are placed inside START SCRIPT and END SCRIPT
- all variable declaration follow right after the START SCRIPT keyword. It cannot be placed anywhere.
- all variable names are case sensitive and starts with letter or an underscore (_) and followed by a letter,
underscore or digits.
- every line contains a single statement
- comments starts with double percent sign (%%) and it can be placed anywhere in the program
- executable codes are placed after variable declaration
- all reserved words are in capital letters and cannot be used as variable names
- dollar sign($) signifies next line or carriage return
- ampersand(&) serves as a concatenator
- the square braces([]) are as escape code
### YOU MAY CHOOSE TO COMPILE ALL OF THE FILES
### Usage
1. Navigate to src/Lexor
```
cd /src/
```
3. Run the interpreter by passing a script file:
```
java Lexor.java .\my_program
```
## Variables and Data Types
All variable declarations must follow right after the `START SCRIPT` keyword; they cannot be placed anywhere else. Executable codes are placed after variable declarations. Variable names are case-sensitive.
```text
SCRIPT AREA
START SCRIPT
%% This is a comment
DECLARE INT x, y = 10
DECLARE FLOAT pi = 3.14
DECLARE CHAR letter = 'A'
DECLARE BOOL flag = "TRUE"

x = 20
END SCRIPT
```
## Printing and Formatting
The `PRINT:` statement writes formatted output to the screen.
 - Use & to concatenate values.
 - Use $ to print a newline or carriage return.
 - Use [] as an escape code.

```Plaintext
SCRIPT AREA
START SCRIPT
DECLARE INT score = 100
PRINT: "Your score is: " & score & $ & "Good job!"
END SCRIPT
```
## User Input
The `SCAN:` statement allows users to input values directly into declared variables from the console, separated by commas .
```Plaintext
SCRIPT AREA
START SCRIPT
DECLARE INT a, b
SCAN: a, b
PRINT: "Sum: " & (a + b)
END SCRIPT
```
## Control Flow 
LEXOR utilizes explicit start and end block keywords for conditional logic .
```Plaintext
SCRIPT AREA
START SCRIPT
DECLARE INT health = 50

IF (health > 80)
START IF
    PRINT: "Healthy"
END IF
ELSE IF (health > 20)
START IF
    PRINT: "Warning"
END IF
ELSE
START IF
    PRINT: "Danger"
END IF

END SCRIPT
```
## Loops
LEXOR supports `FOR` loops and `REPEAT WHEN` loops .
```Plaintext
SCRIPT AREA
START SCRIPT
DECLARE INT count

%% A standard FOR loop
FOR (count = 0, count < 5, count = count + 1)
START FOR
    PRINT: count & $
END FOR

%% A REPEAT WHEN loop 
count = 3
REPEAT WHEN (count > 0)
START REPEAT
    PRINT: count & $
    count = count - 1
END REPEAT

END SCRIPT
```

## License
MIT License at `LICENSE` 
