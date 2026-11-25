package foundations.control.decision;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class IfElseIfStatement {

    

    /**
     * @param age
     * Suppose we are making an age category for the participants in our Marathon program
     * The kids under 13 years of age can't participate in the Marathon
     */
    private static void ifElseIfStatement(int age){
        if (age < 13){
            System.out.println("You can't participate!");
        } else if (age >= 13 && age < 18){
            System.out.println("You comes under Teenager category");
        } else if (age >= 18 && age < 50){
            System.out.println("You comes under Adult category");
        } else {
            System.out.println("You comes under Old category");
        }
    }
    public static void main(String[] args) throws IOException{
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Enter your age: ");
        int age = Integer.parseInt(reader.readLine());
        ifElseIfStatement(age);
        reader.close();
    }
}
