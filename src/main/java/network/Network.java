package network;

import java.io.IOException;
import java.lang.Thread.State;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

public class Network {
    private static final int BASE_PORT = 10000;
    private static final int MIN_CONNECT_NUM = 2;
    private static final int MAX_CONNECT_NUM = 5;

    private Server server;
    private ArrayList<Client> clients = new ArrayList();

    public Network(int number) throws Exception {
        Random rand = new Random();
        int connNum = rand.nextInt(MAX_CONNECT_NUM-MIN_CONNECT_NUM) + MIN_CONNECT_NUM;
        if (connNum > number) connNum = number;

        HashSet<Integer> bUsePort = new HashSet();
        System.out.println(number + " 연결 !!");

        while(connNum > 0) {
            int port = BASE_PORT + rand.nextInt(number);
            if (bUsePort.contains(port)) continue;
            bUsePort.add(port);

            try {
                Socket socket = new Socket("localhost", port);
                Client client = new Client(socket, port);
                client.start();
                clients.add(client);
            } catch (Exception ignored) {}

            --connNum;
        }

        server = new Server(BASE_PORT + number);
        server.start();
    }

    public void sendAll(byte[] buff) {
        for (Client client : clients)
            client.send(buff);
    }

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
        this.server.close();

        for (Client client : clients)
            client.close();
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
                    Client client = new Client(clientSocket, -1);
                    client.start();
                    clients.add(client);
                }
            } catch (Exception ignored) {}

            close();
        }

        public void close() {
            try {
                if (!this.socket.isClosed()) {
                    this.socket.close();
                    System.out.println("서버 종료");
                }
            } catch (IOException ignored) {}
        }
    }
}
