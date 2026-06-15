import java.io.*;
import java.sql.*;

public class CRUDOperationsInJDBC {
    private static final String URL = "jdbc:mysql://localhost:3306/StudentDB";
    private static final String USER = "root";
    private static final String PASSWORD = "Raysi@2002";

    public static Connection getConnection() throws SQLException{
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
    public static void insertData(String name, int age) throws SQLException {
        int success = 0;
        try{
            Connection con = getConnection();
            String query = "INSERT INTO Student (name, age) values (?, ?)";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setString(1, name);
            ps.setInt(2, age);
            success = ps.executeUpdate();
        }catch(SQLException e){
            e.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }
        if(success == 1)
            System.out.println("Data inserted successfully with \nName : %s\nAge : %d\n".formatted(name, age));
        else
            System.out.println("Data Insertion Failed");
    }
    public static void update(String name, int id) throws SQLException{
        try {
            Connection con = getConnection();
            String query = "UPDATE student SET name = ? where id = ?";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setString(1, name);
            ps.setInt(2, id);
            int rowsEffected = ps.executeUpdate();
            System.out.println(rowsEffected + " rows effected");
        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    public static void getData(){
        try{
            Connection con = getConnection();
            String query = "SELECT * FROM student";
            PreparedStatement ps = con.prepareStatement(query);
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()){
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                int age = resultSet.getInt("age");
                System.out.println("ID : %d\nNAME : %s\nAGE : %d".formatted(id, name, age));
            }
        }catch (SQLException e){
            e.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void deleteData(int id){
        try{
            Connection con = getConnection();
            String query = "DELETE FROM student WHERE id = ?";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setInt(1, id);
            int rowsEffected = ps.executeUpdate();
            System.out.println(rowsEffected + " Rows Effected");
        }catch (SQLException e){
            e.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void main(String[] args) throws SQLException, IOException {
        BufferedReader reader = new BufferedReader(new FileReader("src/students.txt"));
        String line;
        while((line = reader.readLine()) != null){
            String[] tokens = line.trim().split("\\s+");
            String name = tokens[0];
            int age = Integer.parseInt(tokens[1]);
            insertData(name, age);
        }
        getData();
        update("Nikkey", 3);
        getData();
        for (int i = 1; i < 8; i++){
            deleteData(i);
        }

    }
}
