package foundations.variables;



/**
 * @author Aashish Kumar Ray
 * @date 15th Sep, 2026
 * Variables
 * Variables are the name allocated to a memory
 * It's not mandatory but in java the variables are preferred in camel casing with these condtions:
 * 1. It will always start with alphabet
 * 2. Numbers can also be used but not in the start
 * 3. No special character to be used except "_".
 * Example : name, spec_1, surter28 etc
 */
public class Variables {
    public static void main(String[] args) {
        int age = 24; // Here the word "age" is a variable used to name a memory allocation where 24 is store
        String name = "Aashish";
        int s1 = 14; // Numbers also can be used but not in the front of the varaible
        int s2 = 15;
        double exact_salary = 27000.78d; //Special character '_' can be used for naming but in java, camel casing is preferred more!
        String fullName = "Aashish Kumar Ray"; //This is the example of camel casing
    }
}
