package foundations.control.decision;

import java.util.Scanner;

public class SwitchStatement {

    private static String switchStatement(int day){
        switch (day) {
            case 1:
                return "Sunday";
            case 2:
                return "Monday";
            case 3:
                return "Tuesday";
            case 4:
                return "Wednesday";
            case 5:
                return "Thursday";
            case 6:
                return "Friday";
            case 7:
                return "Saturday";
            default:
                return "Invalid day";
        }
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter day : ");
        int day = sc.nextInt();
        System.out.println(switchStatement(day));
    }
}
