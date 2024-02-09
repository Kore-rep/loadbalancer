
import java.io.*;
import java.net.*;

public class LoadBalancer {
    private Socket serverSocket;
    private ServerSocket clientSocket;

    public void start(int serverPort, String ip, int clientPort) {
        try {
            serverSocket = new Socket(ip, serverPort);
            clientSocket = new ServerSocket(clientPort);
            System.out.println("Listening for client on port " + clientPort);
            System.out.println("Connecting to server on port " + serverPort);
            while (true)
                new ClientHandler(clientSocket.accept(), serverSocket).start();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public void stop() {
        try {
            serverSocket.close();
            clientSocket.close();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    private static class ClientHandler extends Thread {
        private Socket clientSocket;
        private Socket serverSocket;
        private PrintWriter serverOut;
        private PrintWriter clientOut;
        private BufferedReader serverIn;
        private BufferedReader clientIn;

        public ClientHandler(Socket socket, Socket serverSocket) {
            this.clientSocket = socket;
            this.serverSocket = serverSocket;
        }

        public void run() {
            try {
                serverOut = new PrintWriter(serverSocket.getOutputStream(), true);
                serverIn = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
                clientOut = new PrintWriter(clientSocket.getOutputStream(), true);
                clientIn = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));
                System.out.println(String.format("Recieved message on from %s on port %s",
                        clientSocket.getLocalAddress(), clientSocket.getLocalPort()));

                String inputLine = clientIn.readLine();
                // Forward messages to Server
                serverOut.println(inputLine);
                System.out.println("Forwarded to server");

                while ((inputLine = serverIn.readLine()) != null) {
                    // Send response from server to client
                    clientOut.println(inputLine);
                }
                System.out.println("Forwarded server reply");
                clientIn.close();
                clientOut.close();
                clientSocket.close();
                System.out.println("Terminated connection");
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("Booting");
        LoadBalancer lb = new LoadBalancer();
        lb.start(5555, "127.0.0.1", 80);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                lb.stop();
            }
        }, "Shutdown-thread"));
    }
}