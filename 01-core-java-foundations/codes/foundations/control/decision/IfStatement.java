package foundations.control.decision;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class IfStatement {
    public static void main(String[] args) throws IOException{
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter your age : ");
        int age = Integer.parseInt(reader.readLine());
        if (age >= 21){
            System.out.println("You're eligible purchasing hard drinks!");
        }
        reader.close();
    }
}
