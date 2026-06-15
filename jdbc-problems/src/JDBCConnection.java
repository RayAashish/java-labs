import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class JDBCConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/Garrage";
    private static final String USER = "root";
    private static final String PASSWORD = "Raysi@2002";

    static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
