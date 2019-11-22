package node.network;

import node.event.EventHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server extends Thread{
    private ServerSocket socket;
    private ArrayList<Client> clients;
    private EventHandler handler;

    public Server(int port, ArrayList<Client> clients, EventHandler handler) throws Exception {
        this.clients = clients;
        this.handler = handler;
        socket = new ServerSocket(port);
    }

    public void run() {
        try {
            while (true) {
                Socket clientSocket = socket.accept();
                Client client = new Client(clientSocket, -1, handler);
                client.start();
                clients.add(client);
            }
        } catch (Exception ignored) {
        }

        close();
    }

    public void close() {
        try {
            if (!this.socket.isClosed())
                this.socket.close();
        } catch (IOException ignored) {
        }
    }

}
