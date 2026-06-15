import java.sql.*;
import java.util.Optional;

public class JDBCBasics {
    private static final String URL = "jdbc:mysql://localhost:3306/Garrage";
    private static final String PASSWORD = "Raysi@2002";
    private static final String USER = "root";
    static Connection getConnection() throws SQLException{
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
    public static void main(String[] args) {
//        int id = 3;
//        Optional<String> name = getName(id);
//        name.ifPresentOrElse(System.out::println ,() -> System.out.println("No name found with the corresponding id : " + id));
//        String name = getName1(10);
//        System.out.println(name.toUpperCase());

        int rowsEffected = update("email", "sk.rukiyabhanu@gmail.com", 2);
        System.out.println(rowsEffected);
        int re2 = update("id", "116", 1);
        int re3 = update("age", "24", 116);

        System.out.println(re2);
        lookDb();
    }

    static  void lookDb(){
        try{
            Connection connection = getConnection();
            String query = "SELECT * FROM Employees";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            ResultSet employees = preparedStatement.executeQuery();
            while (employees.next()){
                int id = employees.getInt(1);
                String name = employees.getString(2);
                String email = employees.getString(3);
                int age = employees.getInt(4);
                System.out.println(id + "   " + name + "    " + email + "   " + age);
            }
            preparedStatement.close();

        } catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }

    static Optional<String> getName(int id){
        try{
            String query = "SELECT name FROM Employees WHERE id = ?";
            Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, id);
            ResultSet name = statement.executeQuery();
            while(name.next()){
                return Optional.ofNullable(name.getString("name"));
            }

        } catch (SQLException e){
            System.out.println(e.getMessage());
        }
        return Optional.empty();
    }

    static String getName1(int id){
        try{
            String query = "SELECT name FROM Employees WHERE id = ?";
            Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, id);
            ResultSet name = statement.executeQuery();
            while (name.next()){
                return name.getString("name");
            }

        } catch (SQLException e){
            System.out.println(e.getMessage());
        }
        return null;
    }

    static int update(String columnName, String replaceValue, int id){
        if (!columnName .equals("name".trim())
                && !columnName.equals("id".trim())
                && !columnName.equals("age".trim())
                && !columnName.equals("email".trim())){
            throw new IllegalArgumentException("Illegal Argument");
        }
        try(Connection connection = getConnection()){
            String query = "UPDATE Employees SET " + columnName + " = ? WHERE id = ?";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, replaceValue);
            ps.setInt(2, id);
            return ps.executeUpdate();
        } catch (SQLException e){
            System.out.println(e.getMessage());
        }
        return 0;
    }


}