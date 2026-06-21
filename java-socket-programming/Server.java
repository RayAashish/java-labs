import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

class Server{
    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(8090);
        Socket clientServer = new Socket();

        BufferedReader reader = new BufferedReader(new InputStreamReader(clientServer.getInputStream()));
        String message = reader.readLine();

        PrintWriter out = new PrintWriter(clientServer.getOutputStream(), true);

        out.print("Hello from server");

        serverSocket.close();
        clientServer.close();
        reader.close();

    }
}