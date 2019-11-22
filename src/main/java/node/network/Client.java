package node.network;

import node.event.Event;
import node.event.EventHandler;
import node.event.MessageEventArgs;
import utils.Utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class Client extends Thread {
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
        try {
            while (true) {
                int readLen;

                byte[] integerBuff = new byte[81];
                if ((readLen = input.read(integerBuff)) == -1) break;
                int messageLen = Utils.toObject(integerBuff);

                byte[] message = new byte[messageLen];
                if ((readLen = input.read(message)) == -1) break;
                if (readLen != messageLen) continue;

                event.raiseEvent(this, new MessageEventArgs(message));

                sleep(100);
            }
        } catch (Exception ignored) {}
    }

    public void send(byte[] buff) {
        try {
            byte[] len = Utils.toBytes(new Integer(buff.length));
            output.write(Utils.bytesConcat(len, buff));
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