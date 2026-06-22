import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

class Server{
    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(8080);
        Socket clientSocket = serverSocket.accept();    // blocks until client connects

        // Read from client
        BufferedReader in = new BufferedReader(
            new InputStreamReader(clientSocket.getInputStream()));
        String msg = in.readLine();

        // Write to client
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        out.println("Hello from server!");

        clientSocket.close();
        serverSocket.close();

    }
}