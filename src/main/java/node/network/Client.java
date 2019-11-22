package node.network;

import node.event.Event;
import node.event.EventHandler;
import node.event.MessageEventArgs;

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

    private Event<MessageEventArgs> event = new Event<>();

    public Client(Socket socket, int port, EventHandler handler) {
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

        event.addEventHandler(handler);
    }

    @Override
    public void run() {
        byte[] buff = new byte[BUFF_SIZE];
        int buffLen;

        try {
            while (true) {
                if ((buffLen = input.read(buff)) == -1) break;

                byte[] message = new byte[buffLen];
                System.arraycopy(buff, 0, message, 0, buffLen);

                event.raiseEvent(this, new MessageEventArgs(message));

                sleep(100);
            }
        } catch (Exception ignored) {}
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
        } catch (IOException ignored) {}

        try {
            if (!socket.isClosed())
                socket.close();
        } catch (IOException ignored) {}
    }


}