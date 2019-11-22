import event.EventHandler;
import event.MessageEventArgs;
import network.Client;
import network.Network;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Node extends Thread implements EventHandler<MessageEventArgs> {
    public static int NodeCount = 0;
    private boolean bLoop = true;

    // Blockchain
    private String address;
    private Db db;
    private Blockchain bc;
    private ArrayList<Transaction> mempool = new ArrayList<>();

    // Network
    private static final int COMMAND_LEN = 13, DATA_TYPE_LEN = 5;
    private Network network;

    // Mutex
    private Semaphore mempoolSem = new Semaphore(1);

    public Node(String address) throws Exception {
        this.address = address;
        this.db = new Db();

        if (NodeCount == 0)
            this.bc = new Blockchain(this.address, this.db);
        else {
            this.bc = new Blockchain(this.db);
            // TODO: blockchain 갱신
        }

        this.network = new Network(NodeCount++);
    }

    public void run() {
        while (bLoop) {
            if (!network.checkConnection()) {
                network.close();
                bLoop = false;
                break;
            }

            // mempool
            try {
                mempoolSem.acquire();
                try {
                    if (mempool.size() > 2) {
                        Transaction[] txs = new Transaction[mempool.size()];
                        for (int i = 0; i < mempool.size(); i++)
                            txs[i] = mempool.get(i);
                        mempool.clear();

                        bc.MineBlock(txs);
                    }
                } catch (Exception ignored) {}
                finally {
                    mempoolSem.release();
                }
            } catch (InterruptedException ignored) {
            }

            try {
                sleep(100L);
            } catch (InterruptedException ignored) {}
        }

        if (network.checkConnection())
            network.close();
    }

    public void eventReceived(Object sender, MessageEventArgs e) {
        Client client = (Client)sender;
        byte[] buff = e.getMessage();
        this.handleConnection(client, buff, this.bc);
    }

    public void close() {
        bLoop = false;
    }

    private void requestBlocks() {
    }

    private void sendBlock(Client client, Block b) {
        byte[] command = (new String("block")).getBytes();
        byte[] data = null; // TODO: b.serialize();
        byte[] buff = new byte[COMMAND_LEN + data.length];

        System.arraycopy(command, 0, buff, 0, command.length);
        System.arraycopy(data, 0, buff, COMMAND_LEN, data.length);

        client.send(buff);
    }
    private void sendInv(Client client, String kind, byte[][] items) {
        byte[] command = new String("inv").getBytes();
        byte[] invType = kind.getBytes();

        // Data
        int dataLen = 0;
        for (int i = 0; i < items.length; i++)
            dataLen += items[i].length;

        byte[] data = new byte[dataLen];
        for (int i = 0, j = 0; i < items.length; i++, j += items[i].length)
            System.arraycopy(items[i], 0, data, j, items[i].length);

        // Buff
        byte[] buff = new byte[COMMAND_LEN + DATA_TYPE_LEN + data.length];

        System.arraycopy(command, 0, buff, 0, command.length);
        System.arraycopy(invType, 0, buff, COMMAND_LEN, invType.length);
        System.arraycopy(data, 0, buff, COMMAND_LEN+DATA_TYPE_LEN, data.length);

        client.send(buff);
    }
    private void sendGetBlocks(Client client) {
        byte[] command = (new String("getblocks")).getBytes();
        byte[] buff = new byte[COMMAND_LEN];

        System.arraycopy(buff, 0, command, 0, command.length);

        client.send(buff);
    }
    private void sendGetData(Client client) {
        byte[] command = (new String("getdata")).getBytes();
        byte[] buff = new byte[COMMAND_LEN];

        System.arraycopy(buff, 0, command, 0, command.length);

        client.send(buff);
    }
    private void sendTx(Client client, Transaction tx) {
        byte[] command = (new String("tx")).getBytes();
        byte[] data = null; // TODO: tx.serialize();
        byte[] buff = new byte[COMMAND_LEN + data.length];

        System.arraycopy(command, 0, buff, 0, command.length);
        System.arraycopy(data, 0, buff, COMMAND_LEN, data.length);

        client.send(buff);
    }
    private void sendVersion(Client client, Blockchain bc) {
        byte[] command = (new String("version")).getBytes();
        byte[] data = null; // TODO: bc.getBestHeight();
        byte[] buff = new byte[COMMAND_LEN + data.length];

        System.arraycopy(command, 0, buff, 0, command.length);
        System.arraycopy(data, 0, buff, COMMAND_LEN, data.length);

        client.send(buff);
    }

    private void handleBlock(byte[] data, Blockchain bc) {
    }
    private void handleInv(byte[] data, Blockchain bc) {
    }
    private void handleGetBlocks(Client client, byte[] data, Blockchain bc) {
    }
    private void handleGetData(Client client, byte[] data, Blockchain bc) {
    }
    private void handleTx(byte[] data, Blockchain bc) {
    }
    private void handleVersion(byte[] data, Blockchain bc) {
    }

    private void handleConnection(Client client, byte[] buff, Blockchain bc) {
        byte[] commandByte = new byte[COMMAND_LEN];
        byte[] data = new byte[buff.length];

        System.arraycopy(buff, 0, commandByte, 0, COMMAND_LEN);
        System.arraycopy(buff, COMMAND_LEN, data, 0, buff.length - COMMAND_LEN);

        String command = new String(commandByte);

        switch(command) {
            case "block":       handleBlock(data, bc);              break;
            case "inv":         handleInv(data, bc);                break;
            case "getblocks":   handleGetBlocks(client, data, bc);  break;
            case "getdata":     handleGetData(client, data, bc);    break;
            case "tx":          handleTx(data, bc);                 break;
            case "version":     handleVersion(data, bc);            break;
        }
    }
}
