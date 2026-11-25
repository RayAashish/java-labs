package foundations.arrays;

/**
 * Varargs
 * Sometimes we need a function with variable argument
 * those argument is treated as the array
 */
public class Varargs {

    private static void varargs(int... something){
        int totalSum = 0;

        //Here we can see that the enchanced for loop designed for 
        // objects works because (Type... name) is treated as
        // arrays only
        for (int n : something){
            totalSum += n;
        }
        System.out.println("Total sum: " + totalSum);
    }
    public static void main(String[] args) {
        varargs(1, 4, 5, 7, -1, 2);
        varargs();
        varargs(new int[]{3, 4, 1});
        int[] nums = {0, -2, 9, -9, 8};
        varargs(nums);
    }
}
