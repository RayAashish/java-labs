package foundations.operators;

/**
 * Demonstrates Java arithmetic operators, integer truncation toward zero,
 * floating-point Infinity/NaN behavior, and safe division-by-zero handling.
 */
public class ArithmeticOperators {

    private static void demonstrateSpecialDivisionRules(int a, int b) {
        System.out.println("\n=== Special Division & Floating-Point Rules ===");
        System.out.println("Integer truncation (-7 / 2): " + (-7 / 2));   // -3 (toward 0, not floor -4)
        System.out.println("Mixed operand (7 / 2.0):     " + (b / 2.0));  // 3.5 (int promoted to double)
        System.out.println("Double +5.0 / 0:             " + (5.0 / 0));  // Infinity
        System.out.println("Double -5.0 / 0:             " + (-5.0 / 0)); // -Infinity
        System.out.println("Double  0.0 / 0:             " + (0.0 / 0));  // NaN

        try {
            int crash = a / 0; // Integral division by zero ALWAYS throws ArithmeticException
            System.out.println(crash);
        } catch (ArithmeticException ex) {
            System.out.println("Caught expected integer / 0 error: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        int a = 10, b = 7;

        System.out.println("=== Standard Arithmetic Operators ===");
        System.out.println("Addition (10 + 7):       " + (a + b));
        System.out.println("Subtraction (10 - 7):    " + (a - b));
        System.out.println("Multiplication (10 * 7): " + (a * b));
        System.out.println("Integer Div (10 / 7):    " + (a / b));
        System.out.println("Remainder (10 % 7):      " + (a % b));

        demonstrateSpecialDivisionRules(a, b);
    }
}
