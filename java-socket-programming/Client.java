import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    public static void main(String[] args)  throws Exception{
        Socket socket = new Socket("localhost", 8080);
        BufferedReader reader  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
        writer.println("Hello from client");

        socket.close();
        reader.close();
    }
}
