package node.network;

import blockchain.Block;
import blockchain.Blockchain;
import blockchain.transaction.Transaction;
import node.event.EventHandler;
import utils.Utils;

import java.lang.Thread.State;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;


public class Network {

    public static interface TYPE {
        int NONE = 1, BLOCK = 2, INV = 3, GETBLOCK = 4, GETDATA = 5 , TX = 6, VERSION = 7;
    }

    private static final int BASE_PORT = 10000;
    private static final int MIN_CONNECT_NUM = 2;
    private static final int MAX_CONNECT_NUM = 5;

    private Server server;
    private ArrayList<Client> clients = new ArrayList();
    private EventHandler handler;

    public Network(int nodeId, EventHandler handler) throws Exception {
        Random rand = new Random();
        int connNum = rand.nextInt(MAX_CONNECT_NUM-MIN_CONNECT_NUM) + MIN_CONNECT_NUM;
        if (connNum > nodeId) connNum = nodeId;

        HashSet<Integer> bUsePort = new HashSet<>();

        this.handler = handler;

        while(connNum > 0) {
            int port = BASE_PORT + rand.nextInt(nodeId);
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

        server = new Server(BASE_PORT + nodeId, clients, handler);
        server.start();
    }

    public ArrayList<Client> getClients() { return clients; }

    public void broadcast(Object o) {
        String className = o.getClass().getSimpleName();
        switch(className) {
            case "Trnasaction" :
                for (Client client : clients)
                    sendTx(client, (Transaction)o);
                break;
        }

    }

    public void sendTx(Client client, Transaction tx) {
        byte[] command = new byte[]{TYPE.TX};
        byte[] data = Utils.toBytes(tx);

        byte[] buff = Utils.bytesConcat(command, data);

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

    public void requestBlocks() {
    }

    public void sendBlock(Client client, Block b) {
        byte[] command = new byte[]{TYPE.BLOCK};
        byte[] data = Utils.toBytes(b);

        byte[] buff = Utils.bytesConcat(command, data);

        client.send(buff);
    }

    public void sendInv(Client client, int invType, byte[] data) {
        byte[] command = new byte[]{TYPE.INV};
        byte[] binvType = new byte[]{(byte)invType};

        byte[] buff = Utils.bytesConcat(command, binvType, data);

        client.send(buff);
    }

    public void sendGetBlocks(Client client) {
        byte[] command = new byte[]{TYPE.GETBLOCK};
        client.send(command);
    }

    public void sendGetData(Client client, int invType, byte[] data) {
        byte[] command = new byte[]{TYPE.GETDATA};
        byte[] it = new byte[]{(byte)invType};
        byte[] buffer = Utils.bytesConcat(command, it, data);
        client.send(buffer);
    }

    public void sendVersion(Client client, Blockchain bc) {
        byte[] command = new byte[]{TYPE.VERSION};
        byte[] data = null; // TODO: bc.getBestHeight();
        byte[] buff = Utils.bytesConcat(command, data);
        client.send(buff);
    }


}
