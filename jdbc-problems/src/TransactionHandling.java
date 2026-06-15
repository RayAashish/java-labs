//7. Transaction Handling (VERY IMPORTANT)
//Problem
//Transfer employees between departments.
//transferEmployee(int empId, int fromDept, int toDept)
//Two queries:
//remove from dept
//add to dept
//Requirements:
//        •	If one fails → rollback.
//Concepts learned:
//setAutoCommit(false)
//commit()
//rollback()

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TransactionHandling {
    public static void main(String[] args) {
        String re = transferAmount(10000, 123, 567);
        System.out.println(re);
    }

    static String transferAmount(double amount, int fromAccNo, int toAccNo) {
        Connection con = null;
        try{
            con = JDBCConnection.getConnection();
            con.setAutoCommit(false);
            String query = "update accounts set balance = balance - ? where accountNumber = ?";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setDouble(1, amount);
            ps.setInt(2, fromAccNo);
            int rowsEffected = ps.executeUpdate();
            String query2 = "update accounts set balance = balance + ? where accountNumber = ?";
            PreparedStatement ps2 = con.prepareStatement(query2);
            ps2.setDouble(1, amount);
            ps2.setInt(2, toAccNo);
            int rowsEffected2 = ps2.executeUpdate();
            ps.close();
            ps2.close();
            con.commit();
            con.close();
            return rowsEffected + " " + rowsEffected2;

        }catch (SQLException e){
            try{
                if (con != null){
                    con.rollback();
                    System.out.println("Rolling back to the previous :: Something has gone wrong");
                }
            }catch (SQLException sqlException){
                sqlException.printStackTrace();
            }
            System.out.println(e.getMessage());
        } finally{
            try{
                if (con != null){
                    con.close();
                    System.out.println("Database closed successfully");
                }
            } catch (SQLException e){
                e.printStackTrace();
            }
        }
        return "0 rows effected";
    }
}