package oops.c02_constructors;


public class CopyConstructor {
    public static void main(String[] args) {
        Course c1 = new Course(116, "Low Level Design", "Aashish", 3);
        Course c2 = new Course(118, "Networking", "Bhanu", 1);
        Course c3 = c1;
        Course c4 = new Course(c2);
        System.out.println(c1 == c3); //Will return true as it's a shallow copy of c1
        System.out.println(c2 == c4); //Returns false because the reference is different
        System.out.println(c2.toString() + "\n" + c4.toString());
        System.out.println(c2.equals(c4)); //returns false because eventhough contents are same because we didn't override the equals() method & hashCode() method
        c3.setDurationInMonth(2); //It will modify c1 as well because of shallow copy
        System.out.println(c1.toString());
        System.out.println(c3.toString());
        c4.setDurationInMonth(2); //It won't modify c2 because of deep copy
        System.out.println(c2.toString());
        System.out.println(c4.toString());

    }
}

/**
 * Course
 * Here, we can make a copy constructor as well by passing the Object of the Class
 * Thing to notice here is, it makes a deep copy of the instance rather than shallow copy.
 */
class Course{
    private int id;
    private String courseName;
    private String facultyName;
    private int durationInMonth;
    public Course(){
        this(0, "Unknown", "Unknown", 0);
    }
    public Course(int id, String courseName, String facultyName, int durationInMonth) {
        this.id = id;
        this.courseName = courseName;
        this.facultyName = facultyName;
        this.durationInMonth = durationInMonth;
    }
    public Course(Course otherCourse){
        this.id = otherCourse.id;
        this.courseName = otherCourse.courseName;
        this.facultyName = otherCourse.facultyName;
        this.durationInMonth = otherCourse.durationInMonth;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getCourseName() {
        return courseName;
    }
    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }
    public String getFacultyId() {
        return facultyName;
    }
    public void setFacultyId(String facultyName) {
        this.facultyName = facultyName;
    }
    public int getDurationInMonth() {
        return durationInMonth;
    }
    public void setDurationInMonth(int durationInMonth) {
        this.durationInMonth = durationInMonth;
    }
    @Override
    public String toString() {
        return "Course [id=" + id + ", courseName=" + courseName + ", facultyName=" + facultyName + ", durationInMonth="
                + durationInMonth + "]";
    }
    
    
}
