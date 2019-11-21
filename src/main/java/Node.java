
import java.io.*;
import java.net.Socket;

public class Node extends Thread {
    public static int NodeCount = 0;

    boolean bLoop = true;

    // Blockchain
    private String address;
    private Db db;
    private Blockchain bc;

    // Network
    private Network network;


    public Node(String address) throws Exception {
        this.address = address;
        db = new Db();

        if (NodeCount == 0) // 첫 노드
            bc = new Blockchain(this.address, db);
        else {
            bc = new Blockchain(db);
            // TODO: 블록체인 갱신

        }

        network = new Network(NodeCount++);
    }

    @Override
    public void run() {
        while (bLoop) {
            if (!network.checkConnection())
                network.close();
        }
    }

    public void close() {
        bLoop = false;
    }
}
