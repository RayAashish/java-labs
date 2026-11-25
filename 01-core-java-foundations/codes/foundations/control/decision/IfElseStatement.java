package foundations.control.decision;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class IfElseStatement {

    private static void ifElseStatement(int n){
        if (n % 2 == 0){
            System.out.println("It's a even number!");
        } else {
            System.out.println("It's an odd number!");
        }
    }
    public static void main(String[] args) throws IOException{
        BufferedReader  reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Enter number: ");
        int n = Integer.parseInt(reader.readLine());
        ifElseStatement(n);
        reader.close();
    }
}
