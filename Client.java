import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {

    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    public void startConnection(String ip, int port) {
        try {
            clientSocket = new Socket(ip, port);
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            System.err.println(e);
        }

    }

    public String sendMessage(String msg) {
        try {
            out.println(msg);
            return in.readLine();
        } catch (Exception e) {
            return null;
        }
    }

    public void stopConnection() {
        try {
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            System.err.println(e);
        }

    }

    public boolean isConnected() {
        return !clientSocket.isClosed();
    }

    public static void main(String[] args) {
        Client c = new Client();
        c.startConnection("127.0.0.1", 80);
        Scanner in = new Scanner(System.in);
        String inputLine = "";
        while (!"q".equals(inputLine)) {
            inputLine = in.nextLine().trim();
            c.startConnection("127.0.0.1", 80);
            String resp = c.sendMessage(inputLine);
            System.out.println(resp);
            c.stopConnection();
        }
        in.close();
    }
}