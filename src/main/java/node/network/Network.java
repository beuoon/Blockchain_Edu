package node.network;

import blockchain.Block;
import blockchain.transaction.Transaction;
import node.event.EventHandler;
import utils.Utils;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;


public class Network {

    public interface TYPE {
        int NONE = 1, BLOCK = 2, INV = 3, GETBLOCK = 4, GETDATA = 5 , TX = 6, VERSION = 7, ADDRESS = 8;
    }

    private static ConcurrentSkipListSet<String> nodes = new ConcurrentSkipListSet<>();
    private String mNodeId;

    private ConcurrentSkipListSet<String> conns = new ConcurrentSkipListSet<>();

    public Network(String nodeId) {
        nodes.add(nodeId);
        mNodeId = nodeId;
    }

    public void autoConnect(int num) {
        if(conns.size() >= num) return;
        num -= conns.size();

        ArrayList<String> nodeList = new ArrayList<>();
        nodeList.addAll(nodes);

        while(num > 0) {
            Random rand = new Random();
            int idx = rand.nextInt(nodes.size());
            String nodeId = nodeList.get(idx);

            if (nodeId.equals(mNodeId)) continue;
            if (conns.contains(nodeId)) continue;

            conns.add(nodeId);
            num--;
        }
    }
    public void connectTo(String nodeId) { conns.add(nodeId); }
    public void closeConnection() { conns.clear(); }
    public void close() {
        closeConnection();
        nodes.remove(mNodeId);
    }
    public ArrayList<String> getConnList() {
        ArrayList<String> connList = new ArrayList<>();
        connList.addAll(conns);
        return connList;
    }

    public void sendBlock(String nodeId, Block block) {
        byte[] command = new byte[]{TYPE.BLOCK};
        byte[] data = Utils.toBytes(block);

        byte[] buff = Utils.bytesConcat(command, data);

        send(nodeId, buff);
    }
    public void sendInv(String nodeId, int invType, byte[] data) {
        byte[] command = new byte[]{TYPE.INV};
        byte[] binvType = new byte[]{(byte)invType};

        byte[] buff = Utils.bytesConcat(command, binvType, data);

        send(nodeId, buff);
    }
    public void sendGetBlocks(String nodeId) {
        byte[] buff = new byte[]{TYPE.GETBLOCK};
        send(nodeId, buff);
    }
    public void sendGetData(String nodeId, int invType, byte[] data) {
        byte[] command = new byte[]{TYPE.GETDATA};
        byte[] it = new byte[]{(byte)invType};

        byte[] buff = Utils.bytesConcat(command, it, data);

        send(nodeId, buff);
    }
    public void sendTx(String nodeId, Transaction tx) {
        byte[] command = new byte[]{TYPE.TX};
        byte[] data = Utils.toBytes(tx);

        byte[] buff = Utils.bytesConcat(command, data);

        send(nodeId, buff);
    }
    public void sendVersion(String nodeId, byte[] data) {
        byte[] command = new byte[]{TYPE.VERSION};

        byte[] buff = Utils.bytesConcat(command, data);

        send(nodeId, buff);
    }
    public void sendAddress(String nodeId) {
        byte[] buff = new byte[]{TYPE.ADDRESS};
        send(nodeId, buff);
    }

    private void send(String nodeId, byte[] buff) {
        if (!EventHandler.callEvent(Network.class, mNodeId, nodeId, buff))
            conns.remove(nodeId);
    }
}
