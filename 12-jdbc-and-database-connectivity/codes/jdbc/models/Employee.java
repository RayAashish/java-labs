package jdbc.models;

import java.util.Objects;

/**
 * Domain entity representing an Employee.
 * Implements {@link Comparable} for sorting strategies (e.g. by age or id).
 */
public class Employee implements Comparable<Employee> {

    private int id;
    private String name;
    private String email;
    private int age;
    private Integer departmentId;

    public Employee() {
    }

    public Employee(String name, String email, int age) {
        this(0, name, email, age, null);
    }

    public Employee(String name, String email, int age, Integer departmentId) {
        this(0, name, email, age, departmentId);
    }

    public Employee(int id, String name, String email, int age) {
        this(id, name, email, age, null);
    }

    public Employee(int id, String name, String email, int age, Integer departmentId) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = age;
        this.departmentId = departmentId;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    @Override
    public int compareTo(Employee o) {
        if (o == null) return 1;
        // Primary sort: by age ascending; Secondary: by id
        int cmp = Integer.compare(this.age, o.age);
        if (cmp != 0) return cmp;
        return Integer.compare(this.id, o.id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Employee employee)) return false;
        return id == employee.id && age == employee.age &&
                Objects.equals(name, employee.name) &&
                Objects.equals(email, employee.email) &&
                Objects.equals(departmentId, employee.departmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, email, age, departmentId);
    }

    @Override
    public String toString() {
        return String.format("Employee[id=%-3d | name=%-15s | email=%-25s | age=%-3d | deptId=%s]",
                id, name, email, age, (departmentId == null ? "None" : departmentId.toString()));
    }
}
