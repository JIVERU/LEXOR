package Tests;

import Lexor.err.ErrorManager;
import Lexor.interpreter.Interpreter;
import Lexor.lexer.Lexer;
import Lexor.lexer.Token;
import Lexor.parser.Parser;
import Lexor.parser.ast.Stmt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InterpreterTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    // 2. Save the originals so we don't break the console forever
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private ErrorManager errorManager = new ErrorManager();

    // --- SETUP & TEARDOWN ---

    @BeforeEach
    public void setUp() {
        // Hijack System.out to capture PRINT statements
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        // Reset the error manager before every single test
        errorManager = new ErrorManager();
    }

    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);

//        System.out.println("====== CAPTURED SYSTEM.OUT ======");
        System.out.println(outContent.toString());
//        System.err.println("====== CAPTURED SYSTEM.ERR ======");
        System.err.println(errContent.toString());
    }

    // --- THE PIPELINE HELPER ---

    // This simulates your Main.java but takes a String instead of a file
    private void runScript(String sourceCode) {
        Lexer lexer = new Lexer(sourceCode, errorManager);
        List<Token> tokens = lexer.scanTokens();

        Parser parser = new Parser(tokens, errorManager);
        List<Stmt> statements = parser.parse();

        if (errorManager.hadError()| statements == null) return;

        Interpreter interpreter = new Interpreter(errorManager);
        interpreter.interpret(statements);
    }

    // --- THE TESTS ---

    @Test
    void testBasicMathAndPrint() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                PRINT: 10 + 5 * 2
                END SCRIPT
                """;

        runScript(code);

        // Assert no errors occurred
        assertFalse(errorManager.hadError(), "Should not have syntax errors");
        assertFalse(errorManager.hadRuntimeError(), "Should not have runtime errors");

        // Assert the console output is exactly "20" (plus a newline)
        assertEquals("20", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testVariableDeclarationAndAssignment() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x = 50
                x = x + 10
                PRINT: x
                END SCRIPT
                """;

        runScript(code);

        assertEquals("60", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testSyntaxErrorIsCaught() {
        // Missing "START SCRIPT"
        String code = """
                SCRIPT AREA
                DECLARE INT x = 5
                PRINT: x
                END SCRIPT
                """;

        runScript(code);

        // Assert that the ErrorManager correctly flagged a syntax error
        assertTrue(errorManager.hadError(), "Parser should have caught the missing START SCRIPT");

        // Ensure the interpreter never ran (nothing should be printed)
        assertEquals("", outContent.toString());
    }

    @Test
    public void testStringConcatenation() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE STRING msg = "Score: "
                DECLARE INT score = 100
                PRINT: msg & score
                END SCRIPT
                """;

        runScript(code);
        assertEquals("Score: 100", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void TestOriginal(){
        String code = """
                %% this is a sample program in LEXOR
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x,y, z=5
                DECLARE CHAR a_1='n'
                DECLARE BOOL t="TRUE"
                x=y=4
                a_1='c'
                %% this is a comment
                PRINT: x & t & z & $ & a_1 & [#] & "last"
                END SCRIPT
                """;

        runScript(code);
        assertEquals("4TRUE5\nc#last", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testSpecificationSampleProgram2_Arithmetic() {
        // Tests arithmetic precedence and unary negative operators [cite: 60-65].
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT xyz, abc=100
                xyz=((abc*5)/10+10)*-1
                PRINT: [[] & xyz & []]
                END SCRIPT
                """;

        runScript(code);
        assertFalse(errorManager.hadError());
        // Expected output from the specification[cite: 66].
        assertEquals("[-60]", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testSpecificationSampleProgram3_Logical() {
        // Tests logical operators AND, NOT EQUAL (<>) [cite: 68-74].
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT a=100, b=200, c=300
                DECLARE BOOL d="FALSE"
                d=(a<b AND c<>200)
                PRINT: d
                END SCRIPT
                """;

        runScript(code);
        assertFalse(errorManager.hadError());
        // AND needs both expressions to be true[cite: 54].
        // Expected output from the specification[cite: 75].
        assertEquals("TRUE", outContent.toString().replace("\r\n", "\n"));
    }

    // ==========================================
    // 2. CONTROL FLOW TESTS
    // ==========================================

    @Test
    public void testIfElseStatement() {
        // Tests if-else conditional blocks [cite: 92-101].
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT val=10
                IF (val > 5)
                START IF
                PRINT: "GREATER"
                END IF
                ELSE
                START IF
                PRINT: "LESSER"
                END IF
                END SCRIPT
                """;

        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("GREATER", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testForLoop() {
        // Tests FOR initialization, condition, update structure [cite: 120-124].
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT i
                FOR (i=0, i<3, i=i+1)
                START FOR
                PRINT: i
                END FOR
                END SCRIPT
                """;

        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("012", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testRepeatWhenLoop() {
        // Tests REPEAT WHEN loop structure [cite: 125-129].
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT count=3
                REPEAT WHEN (count > 0)
                START REPEAT
                PRINT: count
                count = count - 1
                END REPEAT
                END SCRIPT
                """;

        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("321", outContent.toString().replace("\r\n", "\n"));
    }

    // ==========================================
    // 3. EDGE CASES & ERROR HANDLING
    // ==========================================

    @Test
    public void testEdgeCase_DeclarationAfterExecution() {
        // All variable declarations must follow right after START SCRIPT. It cannot be placed anywhere else[cite: 24].
        // Executable codes are placed AFTER variable declaration[cite: 28].
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x=5
                PRINT: x
                DECLARE INT y=10
                END SCRIPT
                """;

        runScript(code);
        assertTrue(errorManager.hadError(), "Parser should reject declarations after executable statements.");
    }

    @Test
    public void testEdgeCase_InvalidVariableName() {
        // Variable names start with letter or underscore, followed by letter, underscore, or digits[cite: 25].
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT 1invalid = 5
                END SCRIPT
                """;

        runScript(code);
        System.out.println(errorManager.hadError());
        assertTrue(errorManager.hadError(), "Parser should reject variable names starting with numbers.");
    }

    @Test
    public void testEdgeCase_ReservedWordAsVariable() {
        // All reserved words are in capital letters and cannot be used as variable names[cite: 29].
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT PRINT = 5
                END SCRIPT
                """;

        runScript(code);
        assertTrue(errorManager.hadError(), "Parser should reject reserved words used as variables.");
    }

    @Test
    public void testEdgeCase_MissingScriptArea() {
        // All codes start with SCRIPT AREA[cite: 22].
        String code = """
                START SCRIPT
                DECLARE INT x=5
                PRINT: x
                END SCRIPT
                """;

        runScript(code);
        assertTrue(errorManager.hadError(), "Parser should reject code missing the SCRIPT AREA header.");
    }

    @Test
    public void testFloatAndModulo() {
        // Tests FLOAT occupying 4 bytes with decimal [cite: 36] and modulo operator[cite: 40, 46].
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE FLOAT f = 5.5
                DECLARE INT rem = 10 % 3
                PRINT: f & $ & rem
                END SCRIPT
                """;

        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("5.5\n1", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testElseIfMultipleAlternatives() {
        // Tests if-else with multiple alternatives.
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT score = 85
                IF (score >= 90)
                START IF
                PRINT: "A"
                END IF
                ELSE IF (score >= 80)
                START IF
                PRINT: "B"
                END IF
                ELSE
                START IF
                PRINT: "F"
                END IF
                END SCRIPT
                """;

        runScript(code);
        assertFalse(errorManager.hadError(), "Should parse ELSE IF correctly.");
        assertEquals("B", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testStandaloneIfStatement() {
        // Tests an IF statement without any ELSE blocks.
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE BOOL flag = "TRUE"
                IF (flag == "TRUE")
                START IF
                PRINT: "FLAG IS TRUE"
                END IF
                PRINT: "DONE"
                END SCRIPT
                """;

        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("FLAG IS TRUEDONE", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testLogicalOrAndNotOperators() {
        // Tests the OR and NOT logical operators[cite: 52, 53].
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE BOOL t = "TRUE"
                DECLARE BOOL f = "FALSE"
                PRINT: (t OR f) & $ & (NOT t)
                END SCRIPT
                """;

        runScript(code);
        assertFalse(errorManager.hadError());
        // OR returns TRUE if one is true[cite: 54]. NOT reverses the value[cite: 54].
        assertEquals("TRUE\nFALSE", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testRelationalOperators() {
        // Tests >=, <=, and == operators[cite: 43, 44, 50].
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x = 10, y = 10, z = 5
                PRINT: (x >= y) & $ & (z <= x) & $ & (x == y) & $ & (x == z)
                END SCRIPT
                """;

        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("TRUE\nTRUE\nTRUE\nFALSE", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testUnaryOperators() {
        // Tests positive (+) and negative (-) unary operators [cite: 55-57].
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT num = 5
                PRINT: +num & $ & -num & $ & -(-num)
                END SCRIPT
                """;

        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("5\n-5\n5", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testVariableCaseSensitivity() {
        // All variable names are case sensitive[cite: 25].
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT var = 1, Var = 2, VAR = 3
                PRINT: var & Var & VAR
                END SCRIPT
                """;

        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("123", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testCommentsAnywhere() {
        // Comments start with %% and can be placed anywhere[cite: 27].
        String code = """
                %% Top comment
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x = 10 %% End of line comment
                %% Comment in the middle
                PRINT: x
                END SCRIPT
                %% Bottom comment
                """;

        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("10", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testScanInputStatement() {
        // Tests SCAN statement which allows user input[cite: 79].
        // Syntax: SCAN: <variableName>[,<variableName>]*[cite: 80].

        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x, y
                SCAN: x, y
                PRINT: x + y
                END SCRIPT
                """;

        // 1. Mock the user typing "15, 25" and pressing Enter
        java.io.ByteArrayInputStream inContent = new java.io.ByteArrayInputStream("15, 25\n".getBytes());
        java.io.InputStream originalIn = System.in;
        System.setIn(inContent);

        try {
            // 2. Run the script. The interpreter should read from our mocked System.in
            runScript(code);

            assertFalse(errorManager.hadError(), "Should not have errors during SCAN.");
            // 15 + 25 = 40
            assertEquals("40", outContent.toString().replace("\r\n", "\n"));
        } finally {
            // 3. Always restore System.in so other tests don't break!
            System.setIn(originalIn);
        }
    }
    // ==========================================
    // 1. ADVANCED PROGRAM STRUCTURE
    // ==========================================

    @Test
    public void testEdgeCase_MissingEndScript() {
        // All codes are placed inside START SCRIPT and END SCRIPT[cite: 23].
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x = 5
                PRINT: x
                """;

        runScript(code);
        assertTrue(errorManager.hadError(), "Parser should catch missing END SCRIPT.");
    }

    @Test
    public void testEdgeCase_CodeOutsideStartEndScript() {
        // All codes are placed inside START SCRIPT and END SCRIPT[cite: 23].
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x = 5
                END SCRIPT
                PRINT: x
                """;

        runScript(code);
        assertTrue(errorManager.hadError(), "Parser should reject executable code outside of SCRIPT blocks.");
    }

    @Test
    public void testEdgeCase_MultipleStatementsOnOneLine() {
        // Every line contains a single statement.
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x, y
                x = 5 y = 10
                END SCRIPT
                """;

        runScript(code);
        assertTrue(errorManager.hadError(), "Parser should reject multiple statements on a single line.");
    }

    // ==========================================
    // 2. DATA TYPES & SEMANTICS
    // ==========================================

    @Test
    public void testSemanticError_TypeMismatchAssignment() {
        // INT is an ordinary number[cite: 33]. Assigning a string should fail.
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x
                x = "HELLO"
                END SCRIPT
                """;

        runScript(code);
        // Assuming your ErrorManager distinguishes between syntax and runtime/semantic errors
        assertTrue(errorManager.hadError() || errorManager.hadRuntimeError(), "Interpreter should throw a type mismatch error.");
    }

    @Test
    public void testRuntimeError_UninitializedVariable() {
        // Use of a declared but uninitialized variable should ideally trigger a runtime error or default value behavior.
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x
                PRINT: x + 5
                END SCRIPT
                """;

        runScript(code);
        assertTrue(errorManager.hadRuntimeError(), "Interpreter should complain about uninitialized variables.");
    }

    // ==========================================
    // 3. EXPRESSIONS & OPERATORS
    // ==========================================

    @Test
    public void testSyntaxError_InvalidExpression() {
        // Testing invalid combination of operators.
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x
                x = 5 * / 2
                END SCRIPT
                """;

        runScript(code);
        assertTrue(errorManager.hadError(), "Parser should catch invalid adjacent operators.");
    }

    @Test
    public void testMixedArithmeticAndLogical() {
        // Logical AND needs the two BOOL expressions[cite: 54].
        // Relational operators return BOOL [cite: 42-50].
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT a=10, b=5
                DECLARE BOOL res
                res = (a > b) AND ((a - b) == 5)
                PRINT: res
                END SCRIPT
                """;

        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("TRUE", outContent.toString());
    }

    // ==========================================
    // 4. PRINT STATEMENT ADVANCED
    // ==========================================

    @Test
    public void testPrintFormattingWithDollarSign() {
        // The dollar sign ($) signifies next line or carriage return.
        // PRINT does not auto-append newlines.
        String code = """
                SCRIPT AREA
                START SCRIPT
                PRINT: "Line1" & $ & "Line2" & $
                END SCRIPT
                """;

        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("Line1\nLine2\n", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testPrintEscapeCodes() {
        // The square braces ([]) are used as escape codes[cite: 32].
        // Useful for printing characters that might otherwise be syntax.
        String code = """
                SCRIPT AREA
                START SCRIPT
                PRINT: [[] & "escaped" & []]
                END SCRIPT
                """;

        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("[escaped]", outContent.toString());
    }

    // ==========================================
    // 5. SCAN STATEMENT ADVANCED
    // ==========================================

    @Test
    public void testScanUndeclaredVariable() {
        // SCAN allows inputting a value to a data type[cite: 79]. Variable must exist.
        String code = """
                SCRIPT AREA
                START SCRIPT
                SCAN: undeclaredVar
                END SCRIPT
                """;

        java.io.ByteArrayInputStream inContent = new java.io.ByteArrayInputStream("10\n".getBytes());
        System.setIn(inContent);

        try {
            runScript(code);
            assertTrue(errorManager.hadError() || errorManager.hadRuntimeError(), "Should fail when scanning into an undeclared variable.");
        } finally {
            System.setIn(System.in); // Reset
        }
    }

    // ==========================================
    // 6. CONTROL FLOW - ADVANCED LOOPS & IF
    // ==========================================

    @Test
    public void testNestedControlFlow() {
        // Testing a REPEAT WHEN loop inside an IF statement[cite: 85, 125].
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT limit = 2
                DECLARE BOOL run = "TRUE"
                
                IF (run == "TRUE")
                START IF
                    REPEAT WHEN (limit > 0)
                    START REPEAT
                        PRINT: limit
                        limit = limit - 1
                    END REPEAT
                END IF
                END SCRIPT
                """;

        runScript(code);
        assertFalse(errorManager.hadError(), "Parser should successfully handle nested blocks.");
        assertEquals("21", outContent.toString());
    }

    @Test
    public void testForLoopZeroIterations() {
        // FOR initialization, condition, update[cite: 120].
        // If condition is false initially, it should not execute.
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT i
                FOR (i=10, i<5, i=i+1)
                START FOR
                    PRINT: i
                END FOR
                PRINT: "DONE"
                END SCRIPT
                """;

        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("DONE", outContent.toString());
    }

    @Test
    public void testIfStatementNonBoolCondition() {
        // IF selection requires a <BOOL expression>[cite: 87].
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x = 10
                IF (x)
                START IF
                    PRINT: "SHOULD FAIL"
                END IF
                END SCRIPT
                """;

        runScript(code);
        assertTrue(errorManager.hadError() || errorManager.hadRuntimeError(), "Should reject non-boolean conditions in IF statements.");
    }

    // ==========================================
    // 7. SCAN STATEMENT EXHAUSTIVE TESTS
    // ==========================================

    @Test
    public void testScanSingleVariable() {
        // Tests basic SCAN functionality with a single variable[cite: 79, 80].
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT age
                SCAN: age
                PRINT: age + 5
                END SCRIPT
                """;

        // Mock user typing "20" and hitting Enter
        java.io.ByteArrayInputStream inContent = new java.io.ByteArrayInputStream("20\n".getBytes());
        java.io.InputStream originalIn = System.in;
        System.setIn(inContent);

        try {
            runScript(code);
            assertFalse(errorManager.hadError() || errorManager.hadRuntimeError(), "Should not have errors on valid single SCAN.");
            assertEquals("25", outContent.toString().replace("\r\n", "\n"));
        } finally {
            System.setIn(originalIn); // Always restore System.in!
        }
    }

    @Test
    public void testScanMultipleVariablesMixedTypes() {
        // Tests scanning multiple variables separated by commas.
        // Ensures the interpreter correctly routes the right value to the right type (INT, FLOAT, BOOL).
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT a
                DECLARE FLOAT b
                DECLARE BOOL c
                SCAN: a, b, c
                PRINT: a & $ & b & $ & c
                END SCRIPT
                """;

        // Mock user typing "10, 3.14, "TRUE"" and hitting Enter
        java.io.ByteArrayInputStream inContent = new java.io.ByteArrayInputStream("10, 3.14, \"TRUE\"\n".getBytes());
        java.io.InputStream originalIn = System.in;
        System.setIn(inContent);

        try {
            runScript(code);
            assertFalse(errorManager.hadError() || errorManager.hadRuntimeError(), "Should handle multiple valid inputs.");
            assertEquals("10\n3.14\nTRUE", outContent.toString().replace("\r\n", "\n"));
        } finally {
            System.setIn(originalIn);
        }
    }

    @Test
    public void testScanCountMismatch_TooFewInputs() {
        // Tests error handling when the user provides fewer inputs than requested.
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x, y
                SCAN: x, y
                END SCRIPT
                """;

        // Mock user typing only ONE value instead of TWO
        java.io.ByteArrayInputStream inContent = new java.io.ByteArrayInputStream("10\n".getBytes());
        java.io.InputStream originalIn = System.in;
        System.setIn(inContent);

        try {
            runScript(code);
            assertTrue(errorManager.hadRuntimeError(), "Interpreter should throw a runtime error when input count is too low.");
        } finally {
            System.setIn(originalIn);
        }
    }

    @Test
    public void testScanCountMismatch_TooManyInputs() {
        // Tests error handling when the user provides more inputs than requested.
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x
                SCAN: x
                END SCRIPT
                """;

        // Mock user typing TWO values instead of ONE
        java.io.ByteArrayInputStream inContent = new java.io.ByteArrayInputStream("10, 20\n".getBytes());
        java.io.InputStream originalIn = System.in;
        System.setIn(inContent);

        try {
            runScript(code);
            assertTrue(errorManager.hadRuntimeError(), "Interpreter should throw a runtime error when input count is too high.");
        } finally {
            System.setIn(originalIn);
        }
    }

    @Test
    public void testScanTypeMismatch() {
        // Tests strict typing during input. If a variable is an INT, it should reject a BOOL input.
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT number
                SCAN: number
                END SCRIPT
                """;

        // Mock user typing a boolean literal instead of a number
        java.io.ByteArrayInputStream inContent = new java.io.ByteArrayInputStream("\"TRUE\"\n".getBytes());
        java.io.InputStream originalIn = System.in;
        System.setIn(inContent);

        try {
            runScript(code);
            assertTrue(errorManager.hadRuntimeError(), "Interpreter should throw a type mismatch error when scanning bad input.");
        } finally {
            System.setIn(originalIn);
        }
    }

    @Test
    public void testscan3() {
        // SCAN allows inputting a value to a data type, so the variable must exist.
        String code = """
                SCRIPT AREA
                START SCRIPT
                SCAN: ghostVar
                END SCRIPT
                """;

        java.io.ByteArrayInputStream inContent = new java.io.ByteArrayInputStream("10\n".getBytes());
        java.io.InputStream originalIn = System.in;
        System.setIn(inContent);

        try {
            runScript(code);
            // It might be caught by the parser/resolver as a syntax error, or by the interpreter as a runtime error.
            assertTrue(errorManager.hadError() || errorManager.hadRuntimeError(), "Should fail when scanning into an undeclared variable.");
        } finally {
            System.setIn(originalIn);
        }
    }
}