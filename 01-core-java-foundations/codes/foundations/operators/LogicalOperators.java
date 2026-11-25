package foundations.operators;

public class LogicalOperators {
    private static void andOperation(int age, boolean isCitizen){
        if (age >= 18 && isCitizen)
            System.out.println("You're eligible for voting!");
        else
            System.out.println("You're not eligible for voting");
    }

    private static void orOperation(boolean isPhysicalDocumentAvilable, boolean isDigitalDocumentAvilable){
        if (isPhysicalDocumentAvilable || isDigitalDocumentAvilable)
            System.out.println("You are allowed to enter the event");
        else
            System.out.println("You're not allowed for the event");
    }
    /**
     * @param args
     * returns the boolean value (applicable for && (Logical AND) and ||(Lofgical OR))
     */
    public static void main(String[] args) {
        andOperation(18, true);
        orOperation(false, false);
    }
}
