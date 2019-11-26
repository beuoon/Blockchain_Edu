package blockchainCore;

import blockchainCore.blockchain.Block;
import blockchainCore.node.network.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockchainCore {
    private ConcurrentHashMap<String, Node> nodes = new ConcurrentHashMap<>();

    public synchronized String createNode() {
        Node node = new Node();

        if (nodes.size() == 0)
            node.createGenesisBlock();
        else
            node.createNullBlockchain();
        node.start();

        nodes.put(node.getNodeId(), node);

        return node.getNodeId();
    }
    public synchronized void destoryNode(String nodeId) {
        nodes.remove(nodeId);
    }
    public synchronized String createWallet(String nodeId) {
        return nodes.get(nodeId).createWallet();
    }
    public synchronized void createConnection(String src, String dest) {
        nodes.get(src).connect(dest);
    }
    public synchronized void destroyConnection(String src, String dest) {
        nodes.get(src).disconnection(dest);
        nodes.get(dest).disconnection(src);
    }
    public synchronized void endTransmission(String nodeId, String blockHash) {
        nodes.get(nodeId).endTransmission(blockHash);
    }
    public synchronized void sendBTC(String nodeId, String from, String to, int amount) {
        nodes.get(nodeId).send(from, to, amount);
    }

    public synchronized Node getNode(String nodeId){
        return nodes.get(nodeId);
    }

}

