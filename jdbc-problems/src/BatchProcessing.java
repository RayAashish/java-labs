//8. Batch Processing
//
//Problem
//
//Insert 1000 employees efficiently.
//
//addBatch()
//executeBatch()
//
//Concepts learned:
//        •	Batch execution
//•	Performance optimization


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class BatchProcessing {
    public static void main(String[] args) {
//        addBatch();
        List<Employee> employeeList = GetAllEmployees.getAllEmployees();
        employeeList.forEach(System.out::println);
    }

    static void addBatch(){
        Scanner sc = new Scanner(System.in);
        try(Connection con = JDBCConnection.getConnection()){
            String query = "insert into employees (name, email, age) values (?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(query);
            while (true){
                System.out.print("Enter name : ");
                String name = sc.next();
                System.out.print("Enter email : ");
                String email = sc.next();
                System.out.print("Enter ge : ");
                int age = sc.nextInt();
                System.out.println("Do you want to continue adding employees ? y/n");
                String choice = sc.next();
                ps.setString(1, name);
                ps.setString(2, email);
                ps.setInt(3, age);
                ps.addBatch();
                if (choice.equalsIgnoreCase("N")) break;
            }
            int[] arr = ps.executeBatch();
            for (int num : arr){
                if (num == 0){
                    System.out.println("Some data aren't inserted");
                }
            }
        }catch (SQLException e){
            System.out.println(e.getMessage());
        } finally {
            sc.close();
        }
    }

}
