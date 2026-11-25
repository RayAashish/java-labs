package foundations.arrays;

import java.util.Arrays;

public class DeepAndShallowCopyGotcha {

    private static void copyByValueAndReferenceGotcha(int[] a){
        a[0] = 116;
        a = new int[]{2, 3, 4, 5};
        System.out.println(Arrays.toString(a));

    }
    /**
     * @param a
     * Changing data in one doesn't affect 
     * on other because it's of primitive type
     * Here, the array is holding an actual number
     * & when a copy if modified, it goes to the reference point
     * of the copied array & change the value there
     */
    private static void shallowCopy(int[] a){
        int[] b = Arrays.copyOf(a, a.length);
        int[] c = a.clone();
        System.out.println(Arrays.toString(b));
        System.out.println(Arrays.toString(c));
        // Now let's try changing something in b & c & see if it reflects in a or not
        b[0] = 123;
        c[c.length - 1] = 987;
        a[2] = 71;
        System.out.println(Arrays.toString(b));
        System.out.println(Arrays.toString(c));
        System.out.println(Arrays.toString(a));
    }
    /**
     * @param a
     * Changing data in one does affect 
     * on other because it's of Object type
     * Here the array is holding array of arrays
     * so when the things is modified into the inner arrays
     * the reference point goes to the copied reference point 
     * & make the changes there so it refelects everywhere
     */
    private static void shallowCopyOnObjects(int[][] a){
        int[][] b = Arrays.copyOf(a, a.length);
        int[][] c = a.clone();
        System.out.println(Arrays.deepToString(b));
        System.out.println(Arrays.deepToString(c));
        // Now let's try changing something in b & c & see if it reflects in a or not
        b[0][0] = 123;
        c[c.length - 1][0] = 987;
        a[2][0] = 71;
        System.out.println(Arrays.deepToString(b));
        System.out.println(Arrays.deepToString(c));
        System.out.println(Arrays.deepToString(a));
    }
    public static void main(String[] args) {
        int[][] a = {{1, 2}, {3, 4}, {2, -3}, {-9, 0}};
        // copyByValueAndReferenceGotcha(a);//Should change the a[0] = 116 but not the entire array
        // System.out.println(Arrays.toString(a));
        // shallowCopy(a);
        shallowCopyOnObjects(a);

    }
}
