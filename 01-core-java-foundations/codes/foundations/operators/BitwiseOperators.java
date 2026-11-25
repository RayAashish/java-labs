package foundations.operators;

public class BitwiseOperators {
    /**
     * @param a
     * @param b
     * it perform's the AND operation between 2 integers
     * example : a = 5, b = 9
     * a = 101 & b = 1001 (in binary notation)
     * 0 1 0 1
     * 1 0 0 1
     * ---------
     * 0 0 0 1 (Output should be 1) 
     * because for ON (both should be 1) else offf
     */
    private static void andOperationOnNumbers(int a, int b){
        System.out.println(a & b);
    }
    /**
     * @param a
     * @param b
     * it perform's the OR operation between 2 integers
     * example : a = 5, b = 9
     * a = 101 & b = 1001 (in binary notation)
     * 0 1 0 1
     * 1 0 0 1
     * ---------
     * 1 1 0 1 (Output should be 13) 
     * because for ON (any one should be 1) else offf
     */
    private static void orOperationOnNumbers(int a, int b){
        System.out.println(a | b);
    }
    /**
     * @param a
     * @param b
     * it perform's the XOR operation between 2 integers
     * example : a = 5, b = 9
     * a = 101 & b = 1001 (in binary notation)
     * 0 1 0 1
     * 1 0 0 1
     * ---------
     * 1 1 0 0 (Output should be 12) 
     * because for ON (one should be 1 & one should be 0) else offf
     */
    private static void xorOperationOnNumbers(int a, int b){
        System.out.println(a ^ b);
    }
    /**
     * @param a
     * @param b
     * It performs 1's complement & provides in negative form
     * Always returns : n = -(n + 1)
     */
    private static void norOperationOnNumbers(int a){
        System.out.println(~a);
    }
    public static void main(String[] args) {
        int a = 5, b = 9;
        andOperationOnNumbers(a, b);
        orOperationOnNumbers(a, b);
        xorOperationOnNumbers(a, b);
        norOperationOnNumbers(a);
    }
}
