//9. Pagination Query
//
//Problem
//
//List<Employee> getEmployeesPage(int page, int size)
//
//SQL:
//
//LIMIT ? OFFSET ?
//
//Concepts learned:
//        •	Pagination
//•	Handling large datasets


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PaginationQuery {
    public static void main(String[] args) {
        List<Employee> employeeList = getEmployeesPage(0, 5);
        employeeList.forEach(System.out::println);
    }
    static List<Employee> getEmployeesPage(int skip, int size){
        if (skip < 0 || size < 0)
            throw new RuntimeException("Page & Size must should be positive");
        List<Employee> employeeList = new ArrayList<>();
        try(Connection con = JDBCConnection.getConnection()){
            String query = "select * from employees order by id limit ? offset ?";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setInt(1, size);
            ps.setInt(2, skip);
            ResultSet rs = ps.executeQuery();
            while (rs.next()){
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String email = rs.getString("email");
                int age = rs.getInt("age");
                employeeList.add(new Employee(id, name, email, age));
            }
            ps.close();
        }catch (SQLException e){
            System.out.println(e.getMessage());
        }
        return employeeList;
    }
}
