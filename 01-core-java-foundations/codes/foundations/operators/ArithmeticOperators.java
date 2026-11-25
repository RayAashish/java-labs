package foundations.operators;

public class ArithmeticOperators {

    /**
     * @param a
     * @param b 
     * Most important thing to remember is that int
     * divsion operation, the value tends towards 0(zero)
     * instead of floor value
     */
    private static void speicalDivisionRules(int a, int b){
        System.out.println(- 5.0 / 0); // -Infinity
        System.out.println(5.0 / 0); // Infinity
        System.out.println(b / 2.0); // One operand is double so the result will also be a double
        System.out.println(0.0/0);
        System.out.println(a / 0); // ArithmeticException
    }
    public static void main(String[] args) {
        int a = 10, b = 7;

        System.out.println("Addition : " + (a + b)); // addition
        System.out.println("Subtraction : " + (a - b)); // subtraction
        System.out.println("Multipication : " + (a * b)); // multipication
        System.out.println("Division : " + (a / b)); // division
        System.out.println("Remainder : " + (a % b)); //to get modulo/remainder 
        speicalDivisionRules(a, b);

    }
}
