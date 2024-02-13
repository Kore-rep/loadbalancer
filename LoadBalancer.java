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

    private HashMap<Integer, SocketInformation> socketTable;

    public LoadBalancer(int[] ports) {
        serverPorts = ports;
        healthCheckTimers = new Timer[serverPorts.length];
        currentServer = 0;
        socketTable = new HashMap<>();
    }

    public void start(String ip, int clientPort) {
        try {
            for (int i = 0; i < serverPorts.length; i++) {
                System.out.println("Registering port " + serverPorts[i]);
                try {
                    SocketInformation socketInfo = new SocketInformation(serverPorts[i], ip, null, null, false);
                    socketTable.put(serverPorts[i], socketInfo);
                    healthCheckTimers[i] = new Timer();
                    healthCheckTimers[i].scheduleAtFixedRate(new HealthCheckHandler(serverPorts[i]), 0,
                            HEALTH_CHECK_INTERVAL);
                    socketInfo.connect();

                } catch (UnknownHostException e) {

                } catch (IOException e) {
                    System.out.println("Unable to connect to port");
                }
            }
            clientSocket = new ServerSocket(clientPort);
            System.out.println("Listening for client on port " + clientPort);
            while (true)
                new ClientHandler(clientSocket.accept(), getNextServer()).start();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public void healthCheck() {
        System.out.println("Performing health check on port " + serverPorts[0]);
        new Timer().schedule(new HealthCheckHandler(serverPorts[0]), 1000);
    }

    // Round-robin load balancing
    public SocketInformation getNextServer() {
        SocketInformation server = socketTable.get(serverPorts[currentServer % serverPorts.length]);
        while (!server.isHealthy()) {
            currentServer++;
            server = socketTable.get(serverPorts[currentServer % serverPorts.length]);
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
                    System.err.println("Unable to close socket");
                }
            });
            clientSocket.close();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    private static class ClientHandler extends Thread {
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
                System.out.println(String.format("Recieved message on from %s on port %s",
                        clientSocket.getLocalAddress(), clientSocket.getLocalPort()));

                String inputLine = clientIn.readLine();
                System.out.println("Client requested: " + inputLine);
                // Forward messages to Server
                serverSocket.sendMessage(inputLine);
                System.out.println("Forwarded to server");

                inputLine = serverSocket.readMessage();
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

    private class HealthCheckHandler extends TimerTask {
        private int port;

        public HealthCheckHandler(int port) {
            this.port = port;
        }

        // TODO: Clean this function up
        @Override
        public void run() {
            SocketInformation socketInfo = socketTable.get(port);
            if (socketInfo.isHealthy()) {
                String serverResponse = makeHealthCheck(socketInfo);
                if (!serverResponse.equals("healthy")) {
                    setServerUnhealthy();
                    logHealthState(StateTransition.HEALTHY_TO_UNHEALTHY);
                } else {
                    logHealthState(StateTransition.UNCHANGED);
                    setServerHealthy();
                }
            } else {
                try {
                    socketInfo.connect();
                    String serverResponse = makeHealthCheck(socketInfo);
                    if (!serverResponse.equals("healthy")) {
                        setServerUnhealthy();
                        logHealthState(StateTransition.UNCHANGED);
                    } else {
                        setServerHealthy();
                        logHealthState(StateTransition.UNHEALTHY_TO_HEALTHY);
                    }
                } catch (IOException e) {
                    // Keep unhealthy state
                    logHealthState(StateTransition.UNCHANGED);
                }
            }

        }

        private String makeHealthCheck(SocketInformation s) {
            try {
                s.getSocket().setSoTimeout(SOCKET_TIMEOUT);
                s.sendMessage("health");
                String serverResponse = s.readMessage();
                System.out.println("Recieved server health response: " + serverResponse);
                return serverResponse;
            } catch (SocketException e) {
                System.out.println("Socket took too long to respond to health check");
                return "unhealthy";
            } catch (IOException e) {
                System.out.println("Issue making health reqeust");
                System.out.println(e);
                return "unhealthy";
            }
        }

        private void logHealthState(StateTransition t) {
            StringBuilder sb = new StringBuilder();
            sb.append(port);
            switch (t) {
                case HEALTHY_TO_UNHEALTHY:
                    sb.append(" HEALTHY -> UNHEALTHY");
                    break;
                case UNHEALTHY_TO_HEALTHY:
                    sb.append(" UNHEALTHY -> HEALTHY");
                    break;
                case UNCHANGED:
                    sb.append(socketTable.get(port).isHealthy() ? " remained HEALTHY" : " remained UNHEALTHY");
                    break;
                default:
                    break;
            }
            System.out.println(sb.toString());
        }

        private enum StateTransition {
            HEALTHY_TO_UNHEALTHY,
            UNHEALTHY_TO_HEALTHY,
            UNCHANGED
        }

        private void setServerUnhealthy() {
            SocketInformation info = socketTable.get(port);
            try {

                info.disconnect();
                info.setUnhealthy();
            } catch (IOException e) {
                System.err.println("Unable to disconnect from port" + port);
            }
        }

        private void setServerHealthy() {
            SocketInformation info = socketTable.get(port);
            try {
                info.connect();
                info.setHealthy();
            } catch (IOException e) {
                System.out.println("Unable to connect to port " + port);
                info.setHealthy();
            }
        }

    }

    public static void main(String[] args) {
        args = new String[] { "5555" };
        System.out.println("Booting");
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