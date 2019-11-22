package node;

import node.event.EventHandler;

import java.io.IOException;
import java.lang.Thread.State;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public class Network {
    private static final int BASE_PORT = 10000;
    private static final int MIN_CONNECT_NUM = 2;
    private static final int MAX_CONNECT_NUM = 5;

    private Server server;
    private ArrayList<Client> clients = new ArrayList();
    private EventHandler handler;

    public Network(int number, EventHandler handler) throws Exception {
        Random rand = new Random();
        int connNum = rand.nextInt(MAX_CONNECT_NUM-MIN_CONNECT_NUM) + MIN_CONNECT_NUM;
        if (connNum > number) connNum = number;

        HashSet<Integer> bUsePort = new HashSet<>();

        this.handler = handler;

        while(connNum > 0) {
            int port = BASE_PORT + rand.nextInt(number);
            if (bUsePort.contains(port)) continue;
            bUsePort.add(port);

            try {
                Socket socket = new Socket("localhost", port);
                Client client = new Client(socket, port, handler);
                client.start();
                clients.add(client);
            } catch (Exception ignored) {}

            --connNum;
        }

        server = new Server(BASE_PORT + number);
        server.start();
    }

    public ArrayList<Client> getClients() { return clients; }

    public boolean checkConnection() {
        if (server.getState() == State.TERMINATED)
            return false;

        for (int i = 0; i < clients.size(); ) {
            if (clients.get(i).getState() == State.TERMINATED)
                clients.remove(i);
            else
                ++i;
        }

        return true;
    }

    public void close() {
        try {
            server.close();
            server.join();
        } catch (InterruptedException ignored) {}

        for (int i = 0; i < clients.size(); i++) {
            Client client = clients.get(i);
            try {
                client.close();
                client.join();
            } catch (InterruptedException ignored) {}
        }
    }

    private class Server extends Thread {
        private ServerSocket socket;

        public Server(int port) throws Exception {
            socket = new ServerSocket(port);
        }

        public void run() {
            try {
                while(true) {
                    Socket clientSocket = socket.accept();
                    Client client = new Client(clientSocket, -1, handler);
                    client.start();
                    clients.add(client);
                }
            } catch (Exception ignored) {}

            close();
        }

        public void close() {
            try {
                if (!this.socket.isClosed())
                    this.socket.close();
            } catch (IOException ignored) {}
        }
    }
}
