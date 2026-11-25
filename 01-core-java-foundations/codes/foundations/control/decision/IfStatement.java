package foundations.control.decision;

/**
 * Demonstrates basic 'if' conditional branching with sample inputs.
 */
public class IfStatement {
    private static void checkEligibility(int age) {
        System.out.print("Age " + age + " -> ");
        if (age >= 21) {
            System.out.println("Eligible (meets >= 21 requirement).");
            return;
        }
        System.out.println("Not eligible yet.");
    }

    public static void main(String[] args) {
        checkEligibility(19);
        checkEligibility(24);
    }
}
