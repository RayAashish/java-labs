package foundations.control.decision;

/**
 * Demonstrates classic switch statement behavior vs fall-through awareness.
 */
public class SwitchStatement {
    private static String dayOfWeek(int day) {
        switch (day) {
            case 1: return "Sunday";
            case 2: return "Monday";
            case 3: return "Tuesday";
            case 4: return "Wednesday";
            case 5: return "Thursday";
            case 6: return "Friday";
            case 7: return "Saturday";
            default: return "Invalid day (" + day + ")";
        }
    }

    public static void main(String[] args) {
        System.out.println("Day 1 -> " + dayOfWeek(1));
        System.out.println("Day 5 -> " + dayOfWeek(5));
        System.out.println("Day 9 -> " + dayOfWeek(9));
    }
}
