package oops.c02_constructors;



public class DeepAndShallowCopyConstructorProblem {
    public static void main(String[] args) {
        // Address address = new Address("Laukahi");
        // Student orginal = new Student("Aashish", address);
        // Student copy = new Student(orginal);
        // copy.address.city = "Hile";
        
        // System.out.println(orginal.toString());
        // System.out.println(copy.toString());

        // Address address2 = new Address("Kakinada");
        // Student orginalStudent = new Student("Bhanu", address2);
        // Student copyStudent = Student.copyOf(orginalStudent);
        // copyStudent.address.city = "Hyderabad";
        // System.out.println(orginalStudent.toString());
        // System.out.println(copyStudent.toString());
        TeacherX teacher = new TeacherX("Oxil");
        CourseX orginalCourse = new CourseX(1, teacher);
        CourseX copyCourse = CourseX.copyOf(orginalCourse);
        copyCourse.changeTeacher("Bomman"); //It will change the name of teacher in orginal as well so to solve this, always make sure to pass via a new object
        System.out.println(orginalCourse.toString());
        System.out.println(copyCourse.toString());
    }
}

class Address {
    String city;
    public Address(String city) {
        this.city = city;
    }
}

class StudentX {
    String name;
    Address address;

    public StudentX(String name, Address address) {
        this.name = name;
        this.address = address;
    }

    /**
     * @param other
     * When there is change in the address of copy, it will change the address of orginal as well
     * so, to deal with this, we need to create a new object of address
     * & this problem can't be solved by encapsulation as well
     * we must inititate with new keyword
     */
    public StudentX(StudentX other) {
        this.name = other.name;
        // this.address = other.address;
        this.address = new Address(other.address.city);
    }

    /**
     * @param other
     * @return
     * The above problem can be solved using a static method as well.
     */
    static StudentX copyOf(StudentX other){
        return new StudentX(other.name, new Address(other.address.city));
    }

    @Override
    public String toString() {
        return "Student [name=" + name + ", address=" + address.city + "]";
    }

}

class TeacherX{
    private String teacherName;
    public TeacherX(String teacherName){
        this.teacherName = teacherName;
    }
    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }
}
class CourseX{
    private int id;
    private TeacherX teacher;

    
    public CourseX(int id, TeacherX teacher) {
        this.id = id;
        this.teacher = teacher;
        // this.teacher = new Teacher(teacher.getTeacherName()); //If we put this & call changeTeacher() method
        //It will create a deep copy rather than shallow copy
    }
    static CourseX copyOf(CourseX other){
        return new CourseX(other.id, other.teacher);
    }
    void changeTeacher(String teacherName){
        this.teacher.setTeacherName(teacherName);
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public TeacherX getTeacher() {
        return teacher;
    }
    public void setTeacher(TeacherX teacher) {
        this.teacher = teacher;
    }
    @Override
    public String toString() {
        return "Course [id=" + id + ", teacher=" + teacher.getTeacherName() + "]";
    }
    
    
}
