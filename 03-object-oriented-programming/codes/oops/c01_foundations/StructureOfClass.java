package oops.c01_foundations;

public class StructureOfClass {
    public static void main(String[] args) {
        Student student = new Student(1, "Anivya", 9.16);
        student.name = "Anivya Dimtri"; //Can directly be modified since it's not private member of the class Student
        student.studentDetails();
    }
}

class Student{
    /**
     *These are the field or member variable of a class
     *It has the access all over the class (depends upon the access modifiers)
     *Life span depends upon the life of the object associated with it
     */
    int rollNo;
    String name;
    double cgpa;

    /**
     * @param rollNo
     * @param name
     * @param cgpa
     * This is called a constructor
     */
    public Student(int rollNo, String name, double cgpa){
        this.rollNo = rollNo;
        this.name = name;
        this.cgpa = cgpa;
    }

    /**
     * This is called the method of a class
     */
    public void studentDetails(){
        //This is a method variable & can only be accessed inside the method
        String info = "Students infom : ";
        System.out.print(info);
        System.out.println(this.name + ", " + this.rollNo + ", " + this.cgpa);
    }

}
