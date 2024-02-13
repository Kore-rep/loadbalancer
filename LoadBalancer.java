import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class LoadBalancer {
    private ServerSocket clientSocket;
    private int[] serverPorts;
    private int currentServer;
    private Timer[] healthCheckTimers;
    private final int SOCKET_TIMEOUT = 2;
    private final int HEALTH_CHECK_INTERVAL = 5000;
    private final LoggingUtils logging;

    private HashMap<Integer, SocketInformation> socketTable;

    public LoadBalancer(int[] ports) {
        serverPorts = ports;
        healthCheckTimers = new Timer[serverPorts.length];
        currentServer = 0;
        socketTable = new HashMap<>();
        logging = new LoggingUtils();
    }

    public void start(String ip, int clientPort) {
        try {
            for (int i = 0; i < serverPorts.length; i++) {
                logging.logInfo("Registering port " + serverPorts[i]);
                try {
                    SocketInformation socketInfo = new SocketInformation(serverPorts[i], ip, null, null, false);
                    socketTable.put(serverPorts[i], socketInfo);
                    healthCheckTimers[i] = new Timer();
                    healthCheckTimers[i].scheduleAtFixedRate(new HealthCheckHandler(serverPorts[i]), 0,
                            HEALTH_CHECK_INTERVAL);
                    socketInfo.connect();

                } catch (UnknownHostException e) {

                } catch (IOException e) {
                    logging.logErr("Unable to connect to port " + serverPorts[i]);
                }
            }
            clientSocket = new ServerSocket(clientPort);
            logging.logInfo("Listening for client on port " + clientPort);
            while (true)
                new ClientHandler(clientSocket.accept(), getNextServer()).start();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    // Round-robin load balancing
    public SocketInformation getNextServer() {
        SocketInformation server = socketTable.get(serverPorts[currentServer % serverPorts.length]);
        while (!server.isHealthy()) {
            server = socketTable.get(serverPorts[currentServer++ % serverPorts.length]);
        }
        currentServer++;
        return server;
    }

    public void stop() {
        try {
            socketTable.forEach((port, socketInfo) -> {
                try {
                    socketInfo.disconnect();
                } catch (IOException e) {
                    logging.logErr("Unable to disconnect socket " + port);
                }
            });
            clientSocket.close();
        } catch (IOException e) {
            logging.logErr("Error closing client socket");
        }
    }

    private class ClientHandler extends Thread {
        private Socket clientSocket;
        private SocketInformation serverSocket;

        public ClientHandler(Socket clientSocket, SocketInformation serverSocket) {
            this.clientSocket = clientSocket;
            this.serverSocket = serverSocket;
        }

        public void run() {
            try {
                PrintWriter clientOut = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader clientIn = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));
                logging.logInfo(String.format("Recieved message on from %s on port %s",
                        clientSocket.getLocalAddress(), clientSocket.getLocalPort()));

                String inputLine = clientIn.readLine();
                logging.logInfo("Client requested: " + inputLine);
                // Forward messages to Server
                serverSocket.sendMessage(inputLine);
                inputLine = serverSocket.readMessage();
                // Send response from server to client
                clientOut.println(inputLine);
                clientIn.close();
                clientOut.close();
                clientSocket.close();
                logging.logInfo("Terminated connection");
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    private class HealthCheckHandler extends TimerTask {
        private int port;

        public HealthCheckHandler(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            SocketInformation socketInfo = socketTable.get(port);
            Boolean serverPrevHealthy = socketInfo.isHealthy();
            String serverHealth = healthCheck(socketInfo);
            if (serverHealth.equals("healthy")) {
                setServerHealthy();
                logHealthState(serverPrevHealthy, true);
            } else {
                setServerUnhealthy();
                logHealthState(serverPrevHealthy, false);
            }
        }

        private String healthCheck(SocketInformation s) {
            try {
                if (!s.isHealthy()) {
                    s.connect();
                }
                s.getSocket().setSoTimeout(SOCKET_TIMEOUT);
                s.sendMessage("health");
                String serverResponse = s.readMessage();
                return serverResponse;
            } catch (SocketException e) {
                logging.logErr("Socket took too long to respond to health check");
                return "unhealthy";
            } catch (IOException e) {
                logging.logErr("Issue making health reqeust");
                return "unhealthy";
            }
        }

        private void logHealthState(Boolean serverPrevHealthy, Boolean serverCurrHealthy) {
            if (serverPrevHealthy == serverCurrHealthy)
                return;
            StringBuilder sb = new StringBuilder();
            sb.append(port);
            sb.append(serverPrevHealthy ? " HEALTHY" : " UNHEALTHY");
            sb.append(" -> ");
            sb.append(serverCurrHealthy ? "HEALTHY" : "UNHEALTHY");
            logging.logInfo(sb.toString());
        }

        private void setServerUnhealthy() {
            SocketInformation info = socketTable.get(port);
            try {

                info.disconnect();
                info.setUnhealthy();
            } catch (IOException e) {
                logging.logErr("Unable to disconnect from port" + port);

            }
        }

        private void setServerHealthy() {
            SocketInformation info = socketTable.get(port);
            try {
                info.connect();
                info.setHealthy();
            } catch (IOException e) {
                logging.logErr("Unable to connect to port " + port);
                info.setHealthy();
            }
        }

    }

    public static void main(String[] args) {
        int[] sockets = new int[args.length];
        for (int i = 0; i < args.length; i++) {
            sockets[i] = Integer.parseInt(args[i]);
        }
        LoadBalancer lb = new LoadBalancer(sockets);
        lb.start("127.0.0.1", 80);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                lb.stop();
            }
        }, "Shutdown-thread"));
    }
}