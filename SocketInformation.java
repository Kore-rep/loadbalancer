import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketAddress;

public class SocketInformation {
    private Socket socket;
    private int port;
    private String ip;
    private PrintWriter socketWriter;
    private BufferedReader socketReader;
    private Boolean isHealthy;

    public SocketInformation(int port, String ip, PrintWriter writer, BufferedReader reader, Boolean healthy) {
        this.socket = null;
        this.port = port;
        this.socketWriter = writer;
        this.socketReader = reader;
        this.isHealthy = healthy;
        this.ip = ip;
    }

    public PrintWriter getWriter() throws IOException {
        if (socketWriter == null) {
            socketWriter = new PrintWriter(socket.getOutputStream(), true);
        }
        return socketWriter;
    }

    public BufferedReader getReader() throws IOException {
        if (socketReader == null) {
            socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
        return socketReader;
    }

    public Socket getSocket() {
        return socket;
    }

    public void connect() throws IOException {
        if (socket == null) {
            socket = new Socket(ip, port);
        }
        setHealthy();
    }

    public void disconnect() throws IOException {
        if (socket != null) {
            socketWriter.close();
            socketWriter = null;
            socketReader.close();
            socketReader = null;

            if (socket != null) {
                try {

                    socket.close();
                } catch (IOException e) {
                    // do nothing
                }
                socket = null;
            }
        }
        setUnhealthy();
    }

    public void setUnhealthy() {
        isHealthy = false;
    }

    public void setHealthy() {
        isHealthy = true;
    }

    public Boolean isHealthy() {
        return isHealthy;
    }

    public void sendMessage(String message) throws IOException {
        System.out.println("Sending Message");
        PrintWriter writer = getWriter();
        writer.println(message);
    }

    public String readMessage() throws IOException {
        BufferedReader reader = getReader();
        String line = reader.readLine();
        return line.trim();
    }
}
