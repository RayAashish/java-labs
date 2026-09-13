/**
@author Aashish Kumar Ray

* Objects are just the instance (result) of the class
* It can also be defined as:
* An object is a real world entity
* Example :: Dog (has feeling, eat, have body parts)


*/

package java_oops.objects_and_class;


public class ObjectsAndClass {
    public static void main(String[] args) {
        Student s1 = new Student("Aashish", "IITM");
        Student s2 = new Student("Bhanu", "IIT Madras");
        s1.setCollge("Saarland University");

        System.out.println(s1.toString());
        System.out.println(s2.toString());
    }
}
/**
 * class is a blueprint of an Object
 * A class can also be said as the group of Objects
 */
class Student{ //<- This is a class
    
    private String name;
    private String collge;

    public Student(){
        super();
    }

    public Student(String name, String college){
        super();
        this.name = name;
        this.collge = college;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCollge() {
        return collge;
    }

    public void setCollge(String collge) {
        this.collge = collge;
    }

    @Override
    public String toString() {
        return "Student [name=" + name + ", collge=" + collge + "]";
    }
}