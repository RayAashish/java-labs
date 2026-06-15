//5. Delete Operation
//
//Problem
//
//deleteEmployee(int id)
//
//Concepts learned:
//        •	Data removal
//•	Checking if record exists
//
//⸻


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DeleteEmployee {
    public static void main(String[] args) {
        int re = deleteEmployee(116);
        if (re > 0) {
            System.out.println("Data with given ID deleted successfully");
        } else {
            System.out.println("No employee with such ID exists in database");
        }
    }

    static int deleteEmployee(int id){
        int rowsEffected = 0;
        try(Connection con = JDBCConnection.getConnection()){
            String query = "delete from employees where id = ?";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setInt(1, id);
            rowsEffected = ps.executeUpdate();
        } catch (SQLException e){
            System.out.println(e.getMessage());
        }
        return rowsEffected;
    }
}

