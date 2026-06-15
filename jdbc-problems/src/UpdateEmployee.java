//4. Update Specific Column
//
//        Problem
//
//updateEmployeeEmail(int id, String email)
//
//Concepts learned:
//        •	Update query
//•	Handling result count
//•	Validating rows affected


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class UpdateEmployee {
    public static void main(String[] args) {
        int re = updateEmployeeEmail(116, "irealaashi@gmai.com");
        System.out.println(re);
        List<Employee> employeeList = GetAllEmployees.getAllEmployees();
        employeeList.stream()
                .forEach(System.out::println);
    }

    static int updateEmployeeEmail(int id, String email){
        try(Connection con = JDBCConnection.getConnection()){
            String query = "Update employees set email = ? where id = ?";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setString(1, email);
            ps.setInt(2, id);
            return ps.executeUpdate();
        } catch (SQLException e){
            System.out.println(e.getMessage());
        }
        return 0;
    }

}
