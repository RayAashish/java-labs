package foundations.datatypes;

/**
 * @author Aashish Kumar Ray
 * @date 15th Sep, 2026
 * DataTypes
 * Java is a statically typed language & all the varaibles are required to be 
 * specified with the datatype else the JVM doesn't know which varabile which like 
 * other dynamically types language
 * 
 * In java, there are 2 main data type : i.e Primitive & Non-Primitive
 * 1. Primitive Data Type : Java is nearly 100% OOP language but primitive datatypes are inbuilt
 * & in this I will just write about Primitive datatype because non-primitive are simply objects
 * that I will write in OOPs file
 * 
 * List of Primitive DataTypes:
 * ---- Related to Integer Value ----
 * 1. int (Takes 4 bytes)
 * 2. long (Takes 8 bytes)
 * 3. short (Takes 2 bytes)
 * 4. byte (Takes 1 byte)
 * ---- Related to decimal value ----
 * 5. float (Takes 4 bytes)
 * 6. double (Takes 8 bytes)
 * ----Related to boolean value ----
 * 7. boolean (Takes 1 byte & it's either true or false)
 * ---- Related to Character value ----
 * 8. char (Takes 2 bytes)
 */
public class DataTypes {
    public static void main(String[] args) {
        // int Example:
        int value = 72569;
        System.out.println(value);

        //long Example 
        long distanceFromSun = 100000000000L;
        System.out.println(distanceFromSun);

        //short Example
        short distanceOfBengaluruAndKolkata = 1400;
        System.out.println(distanceOfBengaluruAndKolkata);
        //byte Example
        byte age = 24;
        System.out.println(age);

        //float example
        float pi = 3.1415f;
        System.out.println(pi);

        //double example
        double area = 5492.089D;
        System.out.println(area);

        //boolean example
        boolean isCoding = true;
        System.out.println(isCoding);

        //char example
        char initial = 'R';
        System.out.println(initial);
    }
}
