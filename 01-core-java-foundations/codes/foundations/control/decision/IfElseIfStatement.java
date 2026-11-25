package foundations.control.decision;

/**
 * Demonstrates the 'if-else-if' ladder for mutually exclusive range checks.
 */
public class IfElseIfStatement {
    private static void classifyParticipant(int age) {
        System.out.print("Age " + age + ": ");
        if (age < 13) {
            System.out.println("Cannot participate in marathon (< 13).");
        } else if (age < 18) {
            System.out.println("Teenager category (13-17).");
        } else if (age < 50) {
            System.out.println("Adult category (18-49).");
        } else {
            System.out.println("Senior category (50+).");
        }
    }

    public static void main(String[] args) {
        classifyParticipant(10);
        classifyParticipant(16);
        classifyParticipant(28);
        classifyParticipant(55);
    }
}
