package jdbc.models;

import java.util.Objects;

/**
 * Domain entity representing a Student (preserved and enhanced from jdbc-with-IO).
 */
public class Student {

    private int id;
    private String name;
    private int age;

    public Student() {
    }

    public Student(String name, int age) {
        this(0, name, age);
    }

    public Student(int id, String name, int age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Student student)) return false;
        return id == student.id && age == student.age && Objects.equals(name, student.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, age);
    }

    @Override
    public String toString() {
        return String.format("Student[id=%d, name='%s', age=%d]", id, name, age);
    }
}
