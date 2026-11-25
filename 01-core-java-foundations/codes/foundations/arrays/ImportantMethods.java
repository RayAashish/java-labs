package foundations.arrays;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ImportantMethods {
    private static void regularlyUsedMethods(){
        int[] a = {1, 5, -2, 1, 0, 98, 12};
        int[] b = {1, 5 ,9, 3, -9, -89, -12};
        Integer[] c = {-1, -2, -3, 6, 5, 4, 3};
        int[] d = {-1, -2, -3, 63, 87, 1, 9, 2, 91};
        int[] e = new int[10];

        Arrays.fill(e, (int)(Math.random() * 100));

        Arrays.sort(b);
        Arrays.sort(c, Collections.reverseOrder()); // Collections framwork works only for Objects
        Arrays.sort(d, 0, d.length / 2);

        System.out.println(Arrays.toString(a)); //Prints the array
        System.out.println(Arrays.toString(b));
        System.out.println(Arrays.toString(c));
        System.out.println(Arrays.toString(d));
        System.out.println(Arrays.toString(e));
        System.out.println(Arrays.binarySearch(b, -9));

        Arrays.stream(a)
            .filter(x -> x % 2 == 0)
            .forEach(System.out::println);
    }
    private static void someUniqueMethods(){
        int[] a = {1, 5, -2, 1, 0, 98, 12};
        int[] b = new int[10];
        System.out.println(Arrays.compare(a, b));
        Arrays.setAll(b, i -> (int)(Math.random() * 100));
        System.out.println(Arrays.toString(b));
        Integer[] wrapperArr = Arrays.stream(a)
                                    .boxed()
                                    .toArray(Integer[] :: new);
        List<Integer> list = Arrays.asList(wrapperArr);
        System.out.println(list);
    }
    public static void main(String[] args) {
        regularlyUsedMethods();
        someUniqueMethods();
    }
}
