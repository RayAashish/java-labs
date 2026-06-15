//Basic Read Operation
//        Goal
//
//        Understand Connection, PreparedStatement, ResultSet.
//
//        Problem
//
//        Write a method:
//
//        List getAllEmployees()
//
//        Requirements: • Read all rows from Employees • Map them into an Employee object • Store in List
//
//        Concepts learned: • ResultSet traversal • Object mapping • Column indexing vs column names


import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GetAllEmployees {

    public static void main(String[] args) {
        List<Employee> employeeList = getAllEmployees();
        Collections.sort(employeeList);
        for (Employee employee : employeeList){
            System.out.println(employee.toString());
        }
    }

    static List<Employee> getAllEmployees(){
        List<Employee> result = new ArrayList<>();
        try(Connection con = JDBCConnection.getConnection()){
            String query = "Select * from employees";
            PreparedStatement ps = con.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                int age = rs.getInt("age");
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String email = rs.getString("email");
                result.add(new Employee(id, name, email, age));
            }
        } catch (SQLException e){
            System.out.println(e.getMessage());
        }
        return result;
    }
}

class Employee implements Comparable<Employee>{
    int id;
    String name;
    String email;
    int age;
    public Employee(int id, String name, String email, int age){
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = age;
    }

    public Employee(String name, String email, int age){
        this.name = name;
        this.email = email;
        this.age = age;
    }

    @Override
    public int compareTo(Employee that){
//        return this.name.length() > that.name.length() ? 1 : -1;
        return this.age > that.age ? 1 : -1;
    }

    @Override
    public String toString() {
        return "Employee{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", age='" + age + '\'' +
                '}';
    }
}