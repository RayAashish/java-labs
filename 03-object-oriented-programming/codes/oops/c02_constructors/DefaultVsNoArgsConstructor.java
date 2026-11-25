package oops.c02_constructors;



public class DefaultVsNoArgsConstructor {
    public static void main(String[] args) {
        Student s1 = new Student();
        Employee e1 = new Employee();
    }
}

/**
 * Student
 * This is a no-arg constructor which is a different than
 * default constructor.
 */
class Student{
    String name;
    int rollNo;
    public Student(){
        System.out.println("No Args Constructor From Student Class!");
    }
}

/**
 * Employe
 * Since no constructor is written explictly, the compiler will generate
 * one default constructer by itself which will be equivalent to :
 *      public Employee(){
 *           super();
 *      }
 */
class Employee{
    String name;
    int id;
}