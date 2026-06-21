import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    public static void main(String[] args)  throws Exception{

        Socket socket = new Socket("localhost", 8080);

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println("Hello from client!");

        BufferedReader in = new BufferedReader(
            new InputStreamReader(socket.getInputStream()));
        String response = in.readLine();

        socket.close();
    }
}
