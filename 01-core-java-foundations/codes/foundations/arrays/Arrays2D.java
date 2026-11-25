package foundations.arrays;

import java.lang.reflect.Array;
import java.util.Arrays;

public class Arrays2D {

    static void rectangular2D(){
        int[][] a = new int[3][4]; //3 rows 4 colums, all value is 0
        int[][] b = {
            {1, 2, 3, 4},
            {4, 3, 2, 1},
            {8, 9, 10, 12},
            {12, 3, 12, 9},
            {3, 8, 2, 11}
        };
        System.out.println(b[1].length);
        System.out.println(a[0][3]);
        System.out.println(b[4][3]);
        System.out.println(Arrays.deepToString(b));
    }
    static void jaggedArray(){
        int[][] a = {
            {1, 2, 3},
            {1},
            {23, -1}
        };
        System.out.println("Length of a[1] : " + a[1].length);
        System.out.println("---------------------------");
        for (int i = 0; i < a.length; i++){
            for(int j = 0; j < a[i].length; j++){
                System.out.print(a[i][j] + ", ");
            }
            System.out.println();
        }
        System.out.println("---------------------------");
        System.out.println("All elements : " + Arrays.deepToString(a));
    }
    public static void main(String[] args) {
        // rectangular2D();
        jaggedArray();
    }
}
