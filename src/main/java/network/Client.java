package network;

import event.Event;
import event.MessageEventArgs;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Client extends Thread {
    private static final int BUFF_SIZE = 1000;

    public int port;
    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;

    public Client(Socket socket, int port) {
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
        Event<MessageEventArgs> event = new Event<MessageEventArgs>();

        try {
            while (true) {
                byte[] buff = new byte[BUFF_SIZE];
                input.read(buff);
                event.raiseEvent(this, new MessageEventArgs(buff));
            }
        } catch (Exception ignored) { }

        close();
    }

    public void send(byte[] buff) {
        try {
            output.write(buff);
            output.flush();
        } catch (IOException ignored) {
            close();
        }
    }


    public void close() {
        try {
            input.close();
            output.close();
        } catch (IOException ignored) { }

        try {
            if (!socket.isClosed())
                socket.close();
        } catch (IOException ignored) { }

        System.out.println("클라이언트 종료");
    }
}