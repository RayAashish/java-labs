//Generated Keys (Very Important)
//Most beginners ignore this and regret it later.
//
//Problem
//
//int createEmployee(Employee e)
//
//Return the generated ID from database.
//
//Concepts learned: • RETURN_GENERATED_KEYS • getGeneratedKeys()

import java.sql.*;

public class CreateEmployee {
    public static void main(String[] args) {
        Employee employee = new Employee("Shiyaram", "shiyaram@gmail.com", 55);
        int key = createEmployee(employee);
        System.out.println(key);
    }

    static int createEmployee(Employee e){
        try(Connection con = JDBCConnection.getConnection()){
            String query = "insert into employees (name, email, age) values (?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, e.name);
            ps.setString(2, e.email);
            ps.setInt(3, e.age);
            int rowEffected = ps.executeUpdate();
            if (rowEffected == 1){
                ResultSet keys = ps.getGeneratedKeys();
                while(keys.next()){
                    return keys.getInt(1);
                }
            }
        } catch (SQLException exception){
            System.out.println(exception.getMessage());
        }
        return -1;
    }


}
