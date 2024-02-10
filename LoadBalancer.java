import java.io.*;
import java.net.*;

public class LoadBalancer {
    private Socket[] serverSockets;
    private ServerSocket clientSocket;
    private int[] serverPorts;
    private int currentServer;

    public LoadBalancer(int[] ports) {
        serverPorts = ports;
        serverSockets = new Socket[serverPorts.length];
        currentServer = 0;
    }

    public void start(int serverPort, String ip, int clientPort) {
        try {
            for (int i = 0; i < serverPorts.length; i++) {
                serverSockets[i] = new Socket(ip, serverPorts[i]);
                System.out.println("Connecting to server on port " + serverPorts[i]);
            }
            clientSocket = new ServerSocket(clientPort);
            System.out.println("Listening for client on port " + clientPort);
            while (true)
                new ClientHandler(clientSocket.accept(), getNextServer()).start();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    // Round-robin load balancing
    public Socket getNextServer() {
        return serverSockets[currentServer++ % serverSockets.length];
    }

    public void stop() {
        try {
            for (Socket s : serverSockets) {
                s.close();
            }
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
                System.out.println("Client requested: " + inputLine);
                // Forward messages to Server
                serverOut.println(inputLine);
                System.out.println("Forwarded to server");

                inputLine = serverIn.readLine();
                System.out.println("Sever replied: " + inputLine);
                // Send response from server to client
                clientOut.println(inputLine);
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
        int[] sockets = new int[] { 5555, 5556, 5557 };
        LoadBalancer lb = new LoadBalancer(sockets);
        lb.start(5555, "127.0.0.1", 80);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                lb.stop();
            }
        }, "Shutdown-thread"));
    }
}