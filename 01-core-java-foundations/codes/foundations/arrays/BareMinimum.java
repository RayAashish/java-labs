package foundations.arrays;

import java.util.Arrays;

public class BareMinimum {

    private static void declaration(){
        int[] arr; // preferred one
        int arr2[]; // c-style but discouraged
        String[] names;
        double[][] matrix;

        //Things to focus while declaring multiple things at once
        int[] a, b; //a & b both are array here
        int c[], d; // c is an array whereas d is just a integer variable;
        int[] e, f[];// e is an array & f is 2d array

    }

    private static void creation(){
        int[] arr = new int[5]; // arr is created & initalized with size 5 & all default values are 0
        String[] names = new String[3]; //3-sized array with all default value of null
    }

    private static void instalization(){
        int[] arr = {1, 2, 3, 4, 5};
        int[] arr2 = new int[]{1, 2, 3};
        var arr3 = new int[]{2, 3, 4};
        // var arr4 = {1,9, 3, 3}; // Throws error because it can't infer type from initalizer itself
    }

    private static void traverse(int[] arr){
        //It can be traversed using any loop or enhanced for loop
        for (int n : arr){
            System.out.print(n + ", ");
        }
        System.out.println();
        int n = arr.length - 1;
        //Using while loop
        while (n >= 0){
            System.out.print(arr[n] + ", ");
            n--;
        }
        System.out.println();
        //using stream function
        Arrays.stream(arr)
                .forEach(System.out::print);
    }

    private static void printingEntireArray(int[] arr){
        System.out.println(arr); //Prints the hashcode of arr
        System.out.println(Arrays.toString(arr));//prints the entire array
    }

    public static void main(String[] args) {
        var nums = new int[]{3, 7, -1, 0, 8, 9};
        // traverse(nums);
        printingEntireArray(nums);

    }
}
