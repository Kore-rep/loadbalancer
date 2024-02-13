import java.io.*;
import java.net.*;

public class Server {
    private ServerSocket serverSocket;
    LoggingUtils logging;

    public Server() {
        logging = new LoggingUtils();
    }

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            logging.logInfo("Listening on port " + port);
            while (true)
                new ClientHandler(serverSocket.accept(), port).start();
        } catch (IOException e) {
            logging.logErr("Error opening Server Socket");
        }
    }

    public void stop() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            logging.logErr("Error when stopping server");
        }
    }

    private class ClientHandler extends Thread {
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
                logging.logInfo(String.format("Recieved message on from %s on port %s",
                        clientSocket.getLocalAddress(), clientSocket.getLocalPort()));

                String inputLine;
                while (true) {
                    inputLine = in.readLine();
                    if (inputLine == null) {
                        continue;
                    }
                    logging.logInfo("Recieved: " + inputLine);
                    if (inputLine.trim().equals("health")) {
                        out.println("healthy");
                    } else {
                        out.println("Pong from server on port: " + port);
                        logging.logInfo("Replied with Pong message");
                    }
                }
            } catch (IOException e) {
                logging.logErr("Error responding to request");
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