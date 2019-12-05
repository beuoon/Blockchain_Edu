package blockchainCore;

import blockchainCore.node.Node;

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
        nodes.get(nodeId).close();
        nodes.remove(nodeId);
        // TODO: 제네시스 블록을 유일하게 갖고있는 노드가 없어질 때 예외처리
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
    public synchronized void sendBTC(String nodeId, String from, String to, int amount) {
        nodes.get(nodeId).send(from, to, amount);
    }

    public synchronized Node getNode(String nodeId){
        return nodes.get(nodeId);
    }

    public synchronized void destroyNodeAll() {
        synchronized (nodes) {
            for (String nodeId : nodes.keySet())
                destoryNode(nodeId);
        }
    }

}


