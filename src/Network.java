import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public class Network {
    private static final int BASE_PORT = 3000;
    private static final int BUFF_SIZE = 1000;

    private static final int MIN_CONNECT_NUM = 2, MAX_CONNECT_NUM = 5; // 최소, 최대 연결 클라이언트 수

    private Server server;
    private ArrayList<Client> clients = new ArrayList<Client>();
    private Client readableClient = null;

    Network(int number) throws Exception {
        // 기존 노드 연결
        Random rand = new Random();
        int connNum = (number > MIN_CONNECT_NUM) ? rand.nextInt(MAX_CONNECT_NUM-MIN_CONNECT_NUM)+MIN_CONNECT_NUM : number;

        HashSet<Integer> bUsePort = new HashSet<>();
        while (connNum > 0) {
            int port = BASE_PORT + rand.nextInt(number);
            if (bUsePort.contains(port)) continue;
            bUsePort.add(port);

            try {
                Socket socket = new Socket("localhost", port);
                Client client = new Client(socket, port);
                client.start(); // Read Thread 시작
                clients.add(client); // 클라이언트 추가
            } catch (Exception ignored) {}

            connNum--;
        }

        // 서버 소켓 생성
        server = new Server(BASE_PORT + number);
        server.start();
    }

    public boolean checkConnection() {
        if (server.getState() == Thread.State.TERMINATED)
            return false;

        // 종료된 클라이언트 제거
        for (int i = 0; i < clients.size(); ) {
            if (clients.get(i).getState() == Thread.State.TERMINATED)
                clients.remove(i);
            else
                i++;
        }

        return true;
    }

    public void close() {
        server.close();
        for (Client client : clients) client.close();
    }

    public void requestBlocks() { }

    public void sendBlock() { }
    public void sendData() { }
    public void sendInv() { }
    public void sendGetBlocks() { }
    public void sendGetData() { }
    public void sendTx() { }
    public void sendVersion() { }

    private void handleBlock() { }
    private void handleInv() { }
    private void handleGetBlocks() { }
    private void handleGetData() { }
    private void handleTx() { }
    private void handleVersion() { }
    private void handleConnection() { }

    // Server
    class Server extends Thread {
        private ServerSocket socket;

        Server(int port) throws Exception {
            this.socket = new ServerSocket(port);
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Socket clientSocket = socket.accept();
                    Client client = new Client(clientSocket, -1);
                    client.start(); // Read Thread 시작
                    clients.add(client); // 클라이언트 추가
                }
            } catch(Exception ignored) {}

            close();
        }

        void close() {
            try {
                if (!socket.isClosed())
                    socket.close();
            } catch (IOException ignored) {}
        }
    }

    // Client
    class Client extends Thread {
        int port;
        Socket socket;
        DataInputStream input;
        DataOutputStream output;

        Client(Socket socket, int port) {
            this.port = port;
            this.socket = socket;

            try {
                input = new DataInputStream(socket.getInputStream());
                output = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                try {
                    this.socket.close();
                } catch (IOException ignored) { }
            }

            if (port != -1) System.out.println("소켓 연결 - " + port);
        }

        @Override
        public void run() {
            byte[] buff = new byte[BUFF_SIZE];

            try {
                while (true) {
                    input.read(buff);
                    handleConnection();
                }
            } catch (Exception ignored) { }

            close();
        }

        void close() {
            try {
                input.close();
                output.close();
            } catch (IOException ignored) { }

            try {
                if (!socket.isClosed())
                    socket.close();
            } catch (IOException ignored) { }
        }

        public void send() throws IOException {
        }
    }
}
