package foundations.operators;

public class TernaryOperator {

    private static void oldStyle(int a){
        if (a % 2 == 0)
            System.out.println("Even");
        else
            System.out.println("Odd");
    }
    
    private static void newStyle(int a){
        String ans = a % 2 != 0 ? "Odd" : "Even";
        System.out.println(ans);
    }
    /**
     * @param mark
     * we can use it for nested-if-else as well
     */
    private static void nestedTernary(int mark){
        char result = mark >= 90 ? 'O'
                        : mark >= 80 && mark < 90 ? 'A'
                        : mark >= 70 && mark < 80 ? 'B'
                        : mark >= 60 && mark < 70 ? 'C'
                        : mark >= 50 && mark < 60 ? 'D'
                        : 'F'; 
        System.out.println("Your grade is : " + result);
    }
    public static void main(String[] args) {
        int a = 12, b = 7;
        oldStyle(a);
        newStyle(b);
        nestedTernary(78);
    }
}
