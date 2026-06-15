//Insert Operation
//        Goal
//
//        Learn parameter binding.
//
//        Problem
//
//        Create:
//
//        int addEmployee(String name, String email, int age)
//
//        Requirements: • Insert employee • Return affected rows
//
//        Concepts learned: • executeUpdate() • SQL placeholders • Prevent SQL injection


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class AddEmployee {

    public static void main(String[] args) {
        int re = addEmployee("Aarav", "2018aarav@gmai.com", 7);
        System.out.println(re);
        List<Employee> employeeList = GetAllEmployees.getAllEmployees();
        for (Employee employee : employeeList){
            System.out.println(employee);
        }
    }

    static int addEmployee(String name, String email, int age){
        try(Connection con = JDBCConnection.getConnection()){
            String query = "Insert into employees(name, email, age) values (?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setInt(3, age);
            return ps.executeUpdate();
        } catch (SQLException e){
            System.out.println(e.getMessage());
        }
        return 0;
    }
}
