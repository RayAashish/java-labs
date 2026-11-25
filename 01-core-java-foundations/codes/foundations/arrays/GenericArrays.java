package foundations.arrays;

import java.util.Arrays;
import java.util.List;

public class GenericArrays {
    private static void factsToKnow(){
        // T[] arr = new T[]; //Throws error
        // T[] arr = (T[]) new Object[10];
        List<String>[] arr = new List[3];
        List<String> n1 = List.of("Sammi", "Meri", "Wari");
        List<String> n2 = List.of("Tung", "Tung", "Sahur");
        List<String> n3 = List.of("Chal", "Hatt", "BC");
        arr[0] = n1; arr[1] = n2; arr[2] = n3;
        System.out.println(Arrays.toString(arr));
    }
    private static void specificType(){
        Student[] students = new Student[2];
        Student s1 = new Student(71, "Aashish");
        Student s2 = new Student(16, "Sanchez");
        students[0] = s1; students[1] = s2;
        for (Student s : students){
            System.out.println(s.toString());
        }
    }
    public static void main(String[] args) {
        factsToKnow();
        specificType();
    }
}

class Student{
    
    private int pinNo;
    private String name;
    public String getName() {
        return name;
    }
    public Student(){}
    public Student(int pinNo, String name) {
        this.pinNo = pinNo;
        this.name = name;
    }
    public void setName(String name) {
        this.name = name;
    }
    @Override
    public String toString() {
        return this.pinNo + ", " + this.name + "";
    }
    
}