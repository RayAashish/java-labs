import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;

public class JDBCRevision {

    private static final String URL = "jdbc:mysql://localhost:3306/JDBCRevision";
    private static final String USER = "root";
    private static final String PASSWORD = "Raysi@2002";

    public static void main(String[] args) throws IOException, SQLException {
//        BufferedReader reader = new BufferedReader(new FileReader("src/laptop.txt"));
//        String line;
//        while ((line = reader.readLine()) != null){
//            String[] tokens = line.split("\s+");
//            String name = tokens[0];
//            int ram = Integer.parseInt(tokens[1]);
//            int ssd = Integer.parseInt(tokens[2]);
//            insertData(name, ram, ssd);
//        }

        readData();
    }

    public static Connection getConnection() throws SQLException{
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void insertData(String name, int ram, int ssd) throws SQLException{
        try{
            Connection con = getConnection();
            String query = "INSERT INTO laptop(name, ram, ssd) values (?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setString(1, name);
            ps.setInt(2, ram);
            ps.setInt(3, ssd);
            int rowsEffected = ps.executeUpdate();
            System.out.println(rowsEffected + " Rows Effected");

        } catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }

    public static void readData () throws SQLException{
        try{
            Connection con = getConnection();
            String query = "SELECT * FROM laptop";
            PreparedStatement ps = con.prepareStatement(query);
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()){
                String name = resultSet.getString("name");
                int id = resultSet.getInt("id");
                int ram = resultSet.getInt("ram");
                int ssd = resultSet.getInt("ssd");
                System.out.println(id + name +  ram + ssd);
            }
        } catch (RuntimeException e){
            throw new RuntimeException("Something went wrong");
        }
    }
}
