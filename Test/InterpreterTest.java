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

    // Added tests from issue_description
    @Test
    public void testIssue_MainSampleProgram() {
        String code = """
                SCRIPT AREA
                    START SCRIPT
                        DECLARE INT x, y, z=5
                        DECLARE CHAR a_1='n'
                        DECLARE BOOL t = "TRUE"
                        DECLARE FLOAT f = 6.7
                        f = f + 6
                        x=y=4
                        a_1='c'
                        
                        PRINT: x & t & z & $ & a_1
                        PRINT: f
                        
                        IF (6 < 7)
                            START IF
                                PRINT: "IF BLOCK"
                            END IF
                        ELSE IF (4 >= 9)
                            START IF
                                PRINT: "ELSE IF BLOCK (4 >= 9)"
                            END IF
                        ELSE IF (4 <= 4)
                            START IF
                                PRINT: "ELSE IF BLOCK (4 <= 4)"
                            END IF
                        ELSE
                            START IF
                                PRINT: "ELSE BLOCK"
                            END IF
                    END SCRIPT
                """;

        runScript(code);
        assertFalse(errorManager.hadError());
        assertFalse(errorManager.hadRuntimeError());
        assertEquals("4TRUE5\nc12.7IF BLOCK", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testIssue_ArithmeticOperations() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                        DECLARE INT xyz, abc=100
                        xyz= ((abc *5)/10 + 10) * -1
                        PRINT: [[] & xyz & []]
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("[-60]", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testIssue_BooleanOperation() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                    DECLARE INT a=100, b=200, c=300
                    DECLARE BOOL d="FALSE"
                    d = (a < b AND c <>200)
                    PRINT: d
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("TRUE", outContent.toString());
    }

    @Test
    public void testIssue_ChainedAssignmentWithScan() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                       DECLARE INT x, y, z, a
                       PRINT: "Enter a number: "
                       SCAN: a
                       x = y = z = a
                       PRINT: "x = " & x & $
                       PRINT: "y = " & y & $
                       PRINT: "z = " & z & $
                END SCRIPT
                """;
        java.io.InputStream originalIn = System.in;
        try {
            System.setIn(new java.io.ByteArrayInputStream("7\n".getBytes()));
            runScript(code);
        } finally {
            System.setIn(originalIn);
        }
        assertFalse(errorManager.hadError());
        assertEquals("Enter a number: x = 7\ny = 7\nz = 7\n", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testIssue_UserInputsTwoInts() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                        DECLARE INT x, y
                        SCAN: x, y
                        PRINT: "X = " & x & $ & "Y = " & y
                END SCRIPT
                """;
        java.io.InputStream originalIn = System.in;
        try {
            System.setIn(new java.io.ByteArrayInputStream("3 4\n".getBytes()));
            runScript(code);
        } finally {
            System.setIn(originalIn);
        }
        assertFalse(errorManager.hadError());
        assertEquals("X = 3\nY = 4", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testIssue_RepeatWhen1toN() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                    DECLARE INT input, i = 1
                    PRINT: "Enter a number: "
                    SCAN: input
                    
                    REPEAT WHEN (i <= input)
                        START REPEAT
                            PRINT: "i = " & i & $
                            i=i+1
                        END REPEAT
                END SCRIPT
                """;
        java.io.InputStream originalIn = System.in;
        try {
            System.setIn(new java.io.ByteArrayInputStream("3\n".getBytes()));
            runScript(code);
        } finally {
            System.setIn(originalIn);
        }
        assertFalse(errorManager.hadError());
        assertEquals("Enter a number: i = 1\ni = 2\ni = 3\n", outContent.toString().replace("\r\n", "\n"));
    }

    // INCREMENT 2 TEST CASES (selected, with clear expectations)
    @Test
    public void testInc2_InvalidTrailingCharInPrintExpression() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE BOOL t1, t2
                SCAN: t1
                SCAN: t2
                PRINT:  (NOT t1) AND t2 s
                END SCRIPT
                """;
        java.io.InputStream originalIn = System.in;
        try {
            System.setIn(new java.io.ByteArrayInputStream("TRUE\nFALSE\n".getBytes()));
            runScript(code);
        } finally {
            System.setIn(originalIn);
        }
        assertTrue(errorManager.hadError() || errorManager.hadRuntimeError(), "Should flag syntax/runtime error due to stray 's'.");
    }

    @Test
    public void testInc2_InvalidIdentifierStartingDigit() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE CHAR  0d
                END SCRIPT
                """;
        runScript(code);
        assertTrue(errorManager.hadError(), "Identifier starting with digit should be rejected.");
    }

    @Test
    public void testInc2_IntArithmeticFromScan() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT a, b, c, d
                SCAN: a, b, c
                d = (a * b) / c
                PRINT: d
                END SCRIPT
                """;
        java.io.InputStream originalIn = System.in;
        try {
            System.setIn(new java.io.ByteArrayInputStream("6 4 5\n".getBytes()));
            runScript(code);
        } finally {
            System.setIn(originalIn);
        }
        assertFalse(errorManager.hadError());
        assertEquals("4", outContent.toString());
    }

    @Test
    public void testInc2_FloatAdditionAndBoolPrint() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE BOOL b = "TRUE"
                DECLARE FLOAT z, y
                SCAN: z, y
                z = z + y
                PRINT: b & "  " & z
                END SCRIPT
                """;
        java.io.InputStream originalIn = System.in;
        try {
            System.setIn(new java.io.ByteArrayInputStream("1.5 2.25\n".getBytes()));
            runScript(code);
        } finally {
            System.setIn(originalIn);
        }
        assertFalse(errorManager.hadError());
        assertEquals("TRUE  3.75", outContent.toString());
    }

    @Test
    public void testInc2_MixedScanAndUpdates() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT n, o
                DECLARE FLOAT f
                SCAN: n, o
                SCAN: f
                n = n + 1
                f = f + 1.1
                PRINT: f & " " & n & " " & o
                END SCRIPT
                """;
        java.io.InputStream originalIn = System.in;
        try {
            System.setIn(new java.io.ByteArrayInputStream("2 10\n3.4\n".getBytes()));
            runScript(code);
        } finally {
            System.setIn(originalIn);
        }
        assertFalse(errorManager.hadError());
        assertEquals("4.5 3 10", outContent.toString());
    }

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
        // Reset the error manager before every single my_program.lxr
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

    // ==========================================
    // 8. PROGRAM STRUCTURE EDGE CASES
    // ==========================================

    @Test
    public void testEdgeCase_MissingStartScript() {
        String code = """
                SCRIPT AREA
                DECLARE INT x = 5
                PRINT: x
                END SCRIPT
                """;
        runScript(code);
        assertTrue(errorManager.hadError(), "Should reject missing START SCRIPT.");
    }

    @Test
    public void testEmptyProgram() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("", outContent.toString());
    }

    @Test
    public void testCommentOnlyProgram() {
        String code = """
                %% just a comment
                SCRIPT AREA
                START SCRIPT
                %% another comment
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("", outContent.toString());
    }

    // ==========================================
    // 9. VARIABLE DECLARATION EXPANDED
    // ==========================================

    @Test
    public void testDeclareMultipleVarsPartialInit() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x, y, z = 5
                PRINT: z
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("5", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testDeclareAllDataTypes() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT a = 42
                DECLARE FLOAT b = 3.14
                DECLARE CHAR c = 'Z'
                DECLARE BOOL d = "TRUE"
                DECLARE STRING e = "hello"
                PRINT: a & $ & b & $ & c & $ & d & $ & e
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("42\n3.14\nZ\nTRUE\nhello", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testDeclareStringVariable() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE STRING s = "Hello World"
                PRINT: s
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("Hello World", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testVariableWithUnderscoreStart() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT _myVar = 99
                PRINT: _myVar
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("99", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testVariableWithDigitsAfterStart() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT abc123 = 7
                PRINT: abc123
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("7", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testMultipleDeclarationStatements() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT a = 1
                DECLARE INT b = 2
                DECLARE INT c = 3
                PRINT: a & b & c
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("123", outContent.toString().replace("\r\n", "\n"));
    }

    // ==========================================
    // 10. DATA TYPE & TYPE MISMATCH ERRORS
    // ==========================================

    @Test
    public void testTypeMismatch_BoolToInt() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x
                x = "TRUE"
                END SCRIPT
                """;
        runScript(code);
        assertTrue(errorManager.hadError() || errorManager.hadRuntimeError(), "Cannot assign BOOL to INT.");
    }

    @Test
    public void testTypeMismatch_IntToChar() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE CHAR c
                c = 5
                END SCRIPT
                """;
        runScript(code);
        assertTrue(errorManager.hadError() || errorManager.hadRuntimeError(), "Cannot assign INT to CHAR.");
    }

    @Test
    public void testTypeMismatch_FloatToInt() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x
                x = 3.14
                END SCRIPT
                """;
        runScript(code);
        assertTrue(errorManager.hadError() || errorManager.hadRuntimeError(), "Cannot assign FLOAT to INT.");
    }

    @Test
    public void testCharLiteralAssignment() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE CHAR letter = 'A'
                PRINT: letter
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("A", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testBoolAssignmentFromExpression() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE BOOL b = (5 > 3)
                PRINT: b
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("TRUE", outContent.toString().replace("\r\n", "\n"));
    }

    // ==========================================
    // 11. ARITHMETIC OPERATORS EXPANDED
    // ==========================================

    @Test
    public void testAddition() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                PRINT: 3 + 7
                END SCRIPT
                """;
        runScript(code);
        assertEquals("10", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testSubtraction() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                PRINT: 20 - 8
                END SCRIPT
                """;
        runScript(code);
        assertEquals("12", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testMultiplication() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                PRINT: 6 * 7
                END SCRIPT
                """;
        runScript(code);
        assertEquals("42", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testIntegerDivision() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                PRINT: 7 / 2
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("3", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testFloatDivision() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                PRINT: 7.0 / 2.0
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("3.5", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testDivisionByZero() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                PRINT: 10 / 0
                END SCRIPT
                """;
        runScript(code);
        assertTrue(errorManager.hadRuntimeError(), "Division by zero should be a runtime error.");
    }

    @Test
    public void testArithmeticPrecedence() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                PRINT: 2 + 3 * 4
                END SCRIPT
                """;
        runScript(code);
        assertEquals("14", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testMixedIntFloatArithmetic() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                PRINT: 5 + 2.5
                END SCRIPT
                """;
        runScript(code);
        assertEquals("7.5", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testModuloOperator() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                PRINT: 17 % 5
                END SCRIPT
                """;
        runScript(code);
        assertEquals("2", outContent.toString().replace("\r\n", "\n"));
    }

    // ==========================================
    // 12. RELATIONAL OPERATORS EXPANDED
    // ==========================================

    @Test
    public void testNotEqualOperator() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                PRINT: (5 <> 3) & $ & (5 <> 5)
                END SCRIPT
                """;
        runScript(code);
        assertEquals("TRUE\nFALSE", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testRelationalWithFloats() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                PRINT: (3.14 > 2.71) & $ & (1.0 < 2.0)
                END SCRIPT
                """;
        runScript(code);
        assertEquals("TRUE\nTRUE", outContent.toString().replace("\r\n", "\n"));
    }

    // ==========================================
    // 13. LOGICAL OPERATORS EXPANDED
    // ==========================================

    @Test
    public void testLogicalAndShortCircuit() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE BOOL r = ("FALSE" AND "TRUE")
                PRINT: r
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("FALSE", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testLogicalOrShortCircuit() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE BOOL r = ("TRUE" OR "FALSE")
                PRINT: r
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("TRUE", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testNotWithFalse() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                PRINT: NOT "FALSE"
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("TRUE", outContent.toString().replace("\r\n", "\n"));
    }

    // ==========================================
    // 14. UNARY OPERATORS EXPANDED
    // ==========================================

    @Test
    public void testUnaryOnFloat() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE FLOAT f = 3.14
                PRINT: -f
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("-3.14", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testUnaryOnExpression() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                PRINT: -(5 + 3)
                END SCRIPT
                """;
        runScript(code);
        assertEquals("-8", outContent.toString().replace("\r\n", "\n"));
    }

    // ==========================================
    // 15. PRINT & CONCATENATION EXPANDED
    // ==========================================

    @Test
    public void testPrintMultipleStatements() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                PRINT: "Hello"
                PRINT: " "
                PRINT: "World"
                END SCRIPT
                """;
        runScript(code);
        assertEquals("Hello World", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testPrintBooleanValue() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE BOOL b = "FALSE"
                PRINT: b
                END SCRIPT
                """;
        runScript(code);
        assertEquals("FALSE", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testConcatenationMixedTypes() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT i = 1
                DECLARE FLOAT f = 2.5
                DECLARE CHAR c = 'X'
                DECLARE BOOL b = "TRUE"
                PRINT: i & f & c & b
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("12.5XTRUE", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testPrintHashEscapeCode() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                PRINT: [#] & "tag"
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("#tag", outContent.toString().replace("\r\n", "\n"));
    }

    // ==========================================
    // 16. IF/ELSE EXPANDED
    // ==========================================

    @Test
    public void testIfFalseConditionNoElse() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                IF (5 > 10)
                START IF
                PRINT: "NOPE"
                END IF
                PRINT: "DONE"
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("DONE", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testNestedIfStatements() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x = 10
                IF (x > 5)
                START IF
                    IF (x > 8)
                    START IF
                        PRINT: "DEEP"
                    END IF
                END IF
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("DEEP", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testElseIfAllFalse() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x = 1
                IF (x == 10)
                START IF
                PRINT: "A"
                END IF
                ELSE IF (x == 20)
                START IF
                PRINT: "B"
                END IF
                ELSE
                START IF
                PRINT: "C"
                END IF
                END SCRIPT
                """;
        runScript(code);
        assertEquals("C", outContent.toString().replace("\r\n", "\n"));
    }

    // ==========================================
    // 17. FOR LOOP EXPANDED
    // ==========================================

    @Test
    public void testForLoopCountdown() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT i
                FOR (i=3, i>0, i=i-1)
                START FOR
                PRINT: i
                END FOR
                END SCRIPT
                """;
        runScript(code);
        assertEquals("321", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testNestedForLoops() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT i, j
                FOR (i=0, i<2, i=i+1)
                START FOR
                    FOR (j=0, j<2, j=j+1)
                    START FOR
                        PRINT: i & j
                    END FOR
                END FOR
                END SCRIPT
                """;
        runScript(code);
        assertEquals("00011011", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testForLoopWithBodyAssignment() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT i, sum = 0
                FOR (i=1, i<=5, i=i+1)
                START FOR
                    sum = sum + i
                END FOR
                PRINT: sum
                END SCRIPT
                """;
        runScript(code);
        assertEquals("15", outContent.toString().replace("\r\n", "\n"));
    }

    // ==========================================
    // 18. REPEAT WHEN EXPANDED
    // ==========================================

    @Test
    public void testRepeatWhenFalseInitially() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x = 0
                REPEAT WHEN (x > 10)
                START REPEAT
                PRINT: "NOPE"
                END REPEAT
                PRINT: "DONE"
                END SCRIPT
                """;
        runScript(code);
        assertEquals("DONE", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testRepeatWhenComplexCondition() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x = 5
                REPEAT WHEN (x > 0 AND x < 10)
                START REPEAT
                x = x - 1
                END REPEAT
                PRINT: x
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("0", outContent.toString().replace("\r\n", "\n"));
    }

    // ==========================================
    // 19. ASSIGNMENT & SCOPING
    // ==========================================

    @Test
    public void testChainedAssignment() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x, y
                x = y = 4
                PRINT: x & y
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("44", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testReassignment() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x = 1
                x = 2
                x = 3
                PRINT: x
                END SCRIPT
                """;
        runScript(code);
        assertEquals("3", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testVariableScopeInLoop() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT i, total = 0
                FOR (i=0, i<3, i=i+1)
                START FOR
                    total = total + 1
                END FOR
                PRINT: total
                END SCRIPT
                """;
        runScript(code);
        assertEquals("3", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testUndefinedVariableUsage() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                PRINT: ghost
                END SCRIPT
                """;
        runScript(code);
        assertTrue(errorManager.hadError() || errorManager.hadRuntimeError(), "Should fail for undefined variable.");
    }

    // ==========================================
    // 20. SCAN EXPANDED
    // ==========================================

    @Test
    public void testScanCharInput() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE CHAR c
                SCAN: c
                PRINT: c
                END SCRIPT
                """;
        java.io.ByteArrayInputStream inContent = new java.io.ByteArrayInputStream("'A'\n".getBytes());
        java.io.InputStream originalIn = System.in;
        System.setIn(inContent);
        try {
            runScript(code);
            assertFalse(errorManager.hadError() || errorManager.hadRuntimeError());
            assertEquals("A", outContent.toString().replace("\r\n", "\n"));
        } finally {
            System.setIn(originalIn);
        }
    }

    @Test
    public void testScanFloatInput() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE FLOAT f
                SCAN: f
                PRINT: f
                END SCRIPT
                """;
        java.io.ByteArrayInputStream inContent = new java.io.ByteArrayInputStream("6.28\n".getBytes());
        java.io.InputStream originalIn = System.in;
        System.setIn(inContent);
        try {
            runScript(code);
            assertFalse(errorManager.hadError() || errorManager.hadRuntimeError());
            assertEquals("6.28", outContent.toString().replace("\r\n", "\n"));
        } finally {
            System.setIn(originalIn);
        }
    }

    @Test
    public void testScanNegativeIntSingle() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT n
                SCAN: n
                PRINT: n
                END SCRIPT
                """;
        java.io.ByteArrayInputStream inContent = new java.io.ByteArrayInputStream("-42\n".getBytes());
        java.io.InputStream originalIn = System.in;
        System.setIn(inContent);
        try {
            runScript(code);
            assertFalse(errorManager.hadError() || errorManager.hadRuntimeError(), "Negative int input via SCAN should be accepted.");
            assertEquals("-42", outContent.toString().replace("\r\n", "\n"));
        } finally {
            System.setIn(originalIn);
        }
    }

    @Test
    public void testScanNegativeFloatSingle() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE FLOAT f
                SCAN: f
                PRINT: f
                END SCRIPT
                """;
        java.io.ByteArrayInputStream inContent = new java.io.ByteArrayInputStream("-3.5\n".getBytes());
        java.io.InputStream originalIn = System.in;
        System.setIn(inContent);
        try {
            runScript(code);
            assertFalse(errorManager.hadError() || errorManager.hadRuntimeError(), "Negative float input via SCAN should be accepted.");
            assertEquals("-3.5", outContent.toString().replace("\r\n", "\n"));
        } finally {
            System.setIn(originalIn);
        }
    }

    @Test
    public void testScanMultipleWithNegatives() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT a
                DECLARE FLOAT b
                SCAN: a, b
                PRINT: a & $ & b
                END SCRIPT
                """;
        java.io.ByteArrayInputStream inContent = new java.io.ByteArrayInputStream("-10, -2.25\n".getBytes());
        java.io.InputStream originalIn = System.in;
        System.setIn(inContent);
        try {
            runScript(code);
            assertFalse(errorManager.hadError() || errorManager.hadRuntimeError(), "Multiple negative inputs via SCAN should be accepted.");
            assertEquals("-10\n-2.25", outContent.toString().replace("\r\n", "\n"));
        } finally {
            System.setIn(originalIn);
        }
    }

    @Test
    public void testScanWithLeadingPlusAndNegativeMix() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x
                DECLARE FLOAT y
                DECLARE INT z
                SCAN: x, y, z
                PRINT: x & $ & y & $ & z
                END SCRIPT
                """;
        java.io.ByteArrayInputStream inContent = new java.io.ByteArrayInputStream("+5, -3.5, +7\n".getBytes());
        java.io.InputStream originalIn = System.in;
        System.setIn(inContent);
        try {
            runScript(code);
            assertFalse(errorManager.hadError() || errorManager.hadRuntimeError(), "Signed numeric inputs via SCAN should be accepted.");
            assertEquals("5\n-3.5\n7", outContent.toString().replace("\r\n", "\n"));
        } finally {
            System.setIn(originalIn);
        }
    }

    // ==========================================
    // 21. INTEGRATION / COMPLEX PROGRAMS
    // ==========================================

    @Test
    public void testFullProgramFromReadme() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT count
                FOR (count = 0, count < 5, count = count + 1)
                START FOR
                    PRINT: count & $
                END FOR
                count = 3
                REPEAT WHEN (count > 0)
                START REPEAT
                    PRINT: count & $
                    count = count - 1
                END REPEAT
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("0\n1\n2\n3\n4\n3\n2\n1\n", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testAccumulatorProgram() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT i, sum = 0
                FOR (i=1, i<=10, i=i+1)
                START FOR
                    sum = sum + i
                END FOR
                PRINT: sum
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("55", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testFizzBuzzStyleProgram() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT i
                FOR (i=1, i<=6, i=i+1)
                START FOR
                    IF (i % 3 == 0)
                    START IF
                        PRINT: "F"
                    END IF
                    ELSE IF (i % 2 == 0)
                    START IF
                        PRINT: "B"
                    END IF
                    ELSE
                    START IF
                        PRINT: i
                    END IF
                END FOR
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("1BFB5F", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testSyntaxError_InvalidExpressions() {
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

    // ==========================================
    // 22. NESTED SCOPING TESTS
    // ==========================================

    @Test
    public void testForLoopVariableScoping() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                FOR (DECLARE INT loopVar=1, loopVar<3, loopVar=loopVar+1)
                START FOR
                    PRINT: loopVar
                END FOR
                PRINT: loopVar
                END SCRIPT
                """;
        runScript(code);
        // loopVar is scoped to the FOR loop, so printing it afterwards should cause an error
        assertTrue(errorManager.hadError() || errorManager.hadRuntimeError(), "loopVar should be out of scope.");
    }

    @Test
    public void testForLoopShadowing() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT i = 100
                FOR (DECLARE INT i=1, i<=2, i=i+1)
                START FOR
                    PRINT: i
                END FOR
                PRINT: i
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError(), "Shadowing should be allowed in FOR loops.");
        assertEquals("12100", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testOuterScopeModification() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT x = 10
                IF (x == 10)
                START IF
                    x = 20
                END IF
                PRINT: x
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        // The modification inside the IF block should persist outside because it targets the outer environment's variable
        assertEquals("20", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testNestedBlockVisibility() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT globalVar = 5
                FOR (DECLARE INT outer=1, outer<=1, outer=outer+1)
                START FOR
                    FOR (DECLARE INT inner=1, inner<=1, inner=inner+1)
                    START FOR
                        PRINT: globalVar & outer & inner
                    END FOR
                END FOR
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        // Inner loop can access outer loop's variables and global variables
        assertEquals("511", outContent.toString().replace("\r\n", "\n"));
    }
    // ==========================================
    // 23. FLOAT FORMATTING & EDGE CASES TESTS
    // ==========================================

    @Test
    public void testFloatPrecisionAndFormatting() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE FLOAT f = 5.6
                f = f + 1.1
                PRINT: f
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("6.7", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testFloatHighValues() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE FLOAT high = 999999.99
                high = high * 10.0
                PRINT: high
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("9999999.9", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testFloatAndIntMixedArithmetic() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE FLOAT f = 2.5
                DECLARE INT i = 4
                PRINT: f * i
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("10", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testFloatDivisionByZero() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE FLOAT f = 5.0
                PRINT: f / 0
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertTrue(outContent.toString().contains("Infinity") || outContent.toString().contains("∞"), "Should print Infinity or ∞");
    }

    @Test
    public void testFloatNegativeEdgeCases() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE FLOAT f = -0.5
                PRINT: f * -2.0
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        assertEquals("1", outContent.toString().replace("\r\n", "\n"));
    }

    // ==========================================
    // 24. ADDITIONAL LOOP NESTING TESTS
    // ==========================================

    @Test
    public void testRepeatInsideFor_NestedLoops() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT i, j
                FOR (i=1, i<=3, i=i+1)
                START FOR
                    j = 0
                    REPEAT WHEN (j < i)
                    START REPEAT
                        PRINT: i
                        j = j + 1
                    END REPEAT
                END FOR
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError(), "Nested REPEAT inside FOR should work.");
        // For i=1 => 1; i=2 => 22; i=3 => 333
        assertEquals("122333", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testForInsideRepeat_NestedLoops() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT cnt = 1, limit = 3, k
                REPEAT WHEN (cnt <= limit)
                START REPEAT
                    FOR (k=0, k<cnt, k=k+1)
                    START FOR
                        PRINT: cnt
                    END FOR
                    cnt = cnt + 1
                END REPEAT
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError(), "Nested FOR inside REPEAT should work.");
        // cnt=1 => 1; cnt=2 => 22; cnt=3 => 333
        assertEquals("122333", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testTripleNestedForLoops_SmallBounds() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT i, j, k
                FOR (i=0, i<2, i=i+1)
                START FOR
                    FOR (j=0, j<2, j=j+1)
                    START FOR
                        FOR (k=0, k<2, k=k+1)
                        START FOR
                            PRINT: i & j & k
                        END FOR
                    END FOR
                END FOR
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError(), "Triple-nested FOR loops should execute correctly.");
        assertEquals("000001010011100101110111", outContent.toString().replace("\r\n", "\n"));
    }

    @Test
    public void testInnerRepeatZeroIterationsDependingOnOuter() {
        String code = """
                SCRIPT AREA
                START SCRIPT
                DECLARE INT i, j
                FOR (i=0, i<3, i=i+1)
                START FOR
                    j = 0
                    REPEAT WHEN (j < i)
                    START REPEAT
                        PRINT: j
                        j = j + 1
                    END REPEAT
                END FOR
                END SCRIPT
                """;
        runScript(code);
        assertFalse(errorManager.hadError());
        // i=0 => none; i=1 => 0; i=2 => 01
        assertEquals("001", outContent.toString().replace("\r\n", "\n"));
    }
}