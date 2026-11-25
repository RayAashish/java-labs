package foundations.operators;

public class RelationalOperators {
    private static void someTrickyPoints(){
        // Cache Gotcha
        Integer a = 127;
        Integer b = 127;
        System.out.println(a == b);// returns true
        a = 128;
        b = 128;
        System.out.println(a == b); //returns false
        String x = "hi";
        String y = "hi";
        String z = new String("hi");
        System.out.println(x == y); // return true because x & y both are pointing to the same object
        System.out.println(x == z); // returns false becase of new keyword used in creation of new object
        //Value of x, y, x is same but the reference is different
        //If we want to compare the value then we should use .equals() method
        System.out.println(x.equals(y)); //returns true
        System.out.println(x.equals(z)); // returns true
    }
    /**
     * @param args
     * always return boolean value
     */
    public static void main(String[] args) {
        int a, b;
        a = b = 10;
        // System.out.println(a == b); //true
        // System.out.println(a != b); //false
        // System.out.println(a > b); //false
        // System.out.println(a >= b); // true
        // System.out.println(a < b); // false
        // System.out.println(a <= b); // true
        someTrickyPoints();
    }
}
