package node.network;

import blockchain.Block;
import blockchain.Blockchain;
import blockchain.transaction.Transaction;
import node.event.EventHandler;
import node.event.EventListener;
import utils.Utils;

import java.lang.Thread.State;
import java.net.Socket;
import java.util.*;


public class Network {

    public interface TYPE {
        int NONE = 1, BLOCK = 2, INV = 3, GETBLOCK = 4, GETDATA = 5 , TX = 6, VERSION = 7;
    }

    private static final int MIN_CONNECT_NUM = 2;
    private static final int MAX_CONNECT_NUM = 5;
    private static ArrayList<Node> nodes = new ArrayList();
    Node self;

    private ArrayList<Node> connList = new ArrayList<>();

    public Network(Node node) {
        nodes.add(node);
        self = node;
    }

    public ArrayList<String> getConnList() {
        ArrayList<String> list = new ArrayList<>();
        for(Node node : connList) {
            list.add(node.getNodeId());
        }

        return list;
    }

    public void autoConnect(int num) { // TODO: Fully Connection 아니면 네트워크 분리 될 가능성 있음
        if(connList.size() >= num) return;
        num -= connList.size();

        while(num > 0) {
            Random rand = new Random();
            int idx = rand.nextInt(nodes.size());
            Node node = nodes.get(idx);

            if (node == self) continue;
            if(connList.contains(node)) continue;

            connList.add(node);
            num--;
        }
    }

    public void connectTo(String nodeId, String myNodeId){
        for(Node node : nodes) {
            if(node.getNodeId().equals(nodeId)) {
                if(connList.contains(node)) return;
                connList.add(node);
                if(myNodeId == null) return;
                node.getNetwork().connectTo(myNodeId, null);
                return;
            }
        }
    }
    
    public void requestBlocks() {
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

    public void sendVersion(String nodeId) {
        byte[] command = new byte[]{TYPE.VERSION};
        byte[] data = null; // TODO: bc.getBestHeight();
        byte[] buff = Utils.bytesConcat(command, data);
        send(nodeId, buff);
    }

    private void send(String nodeId, byte[] buff) {
        EventHandler.callEvent(Network.class, self.getNodeId(), nodeId, buff);
    }
}
