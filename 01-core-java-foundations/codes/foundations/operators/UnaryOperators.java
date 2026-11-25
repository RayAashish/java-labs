package foundations.operators;

public class UnaryOperators {
    private static void mostBasic(){
        int a = +16; //Unary plus (Denotes the positive number)
        int b = -17;//Unary minus (Denotes the negative number)
        System.out.println(a);
        System.out.println(b);
        System.out.println(++a); //pre-increment means the number is increased before operation
        System.out.println(--b); //pre-decrement means the number is decreased before operation
        System.out.println(a++); //post-increment means the operation is done then number is increased
        System.out.println(b--); // post-decrement means the operation is done then number is decreased
        System.out.println(a); // The number should be 18 here
        System.out.println(b); // The number should be -19
    }
    private static void otherDatatypes(){
        Integer a = 10;
        byte b = 127;
        char ch = 'a';
        double d = 3.5;
        a++; //should make it 11
        b++; //should overflow & make it -128
        ch++; //should make it b
        d++; //should make it 4.5
        System.out.println(a);
        System.out.println(b);
        System.out.println(ch);
        System.out.println(d);
    }
    private  static void uniqueUnaryOperators(){
        boolean x = false;
        int b = 9; //Binary is : 1001
        x = !x; // should make it true
        b = ~b; //performs 1's complement (for any n) is (-(n + 1))
        System.out.println(x);
        System.out.println(b);
        
    }
    public static void main(String[] args) {
        // mostBasic();
        // otherDatatypes();
        uniqueUnaryOperators();
    }
}
