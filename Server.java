import java.io.*;
import java.net.*;

public class Server {
    private ServerSocket serverSocket;

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);

            System.out.println("Listening on port" + port);
            while (true)
                new ClientHandler(serverSocket.accept()).start();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public void stop() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    private static class ClientHandler extends Thread {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));
                System.out.println(String.format("Recieved message on from %s on port %s",
                        clientSocket.getLocalAddress(), clientSocket.getLocalPort()));
                String inputLine = in.readLine();
                System.out.println(inputLine);
                System.out.println("Replied with Hello message");
                out.println("Pong");
                in.close();
                out.close();

            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("Booting");
        Server serv = new Server();
        serv.start(5555);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                serv.stop();
            }
        }, "Shutdown-thread"));
    }
}