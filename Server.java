import java.io.*;
import java.net.*;

public class Server {
    private ServerSocket serverSocket;

    public void start(int port) {
        try {

            serverSocket = new ServerSocket(port);

            System.out.println("Listening on port" + port);
            while (true)
                new ClientHandler(serverSocket.accept(), port).start();
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
        private int port;

        public ClientHandler(Socket socket, int port) {
            this.clientSocket = socket;
            this.port = port;
        }

        public void run() {
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));
                System.out.println(String.format("Recieved message on from %s on port %s",
                        clientSocket.getLocalAddress(), clientSocket.getLocalPort()));

                String inputLine;
                while (true) {
                    inputLine = in.readLine();
                    System.out.println(inputLine);
                    out.println("Pong from server on port: " + port);
                    System.out.println("Replied with Pong message");
                }
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("Booting");
        Server serv = new Server();
        serv.start(Integer.parseInt(args[0]));
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                serv.stop();
            }
        }, "Shutdown-thread"));
    }
}