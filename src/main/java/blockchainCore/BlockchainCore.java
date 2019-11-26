package blockchainCore;

import blockchainCore.node.network.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockchainCore {
    HashMap<String, Node> nodes = new HashMap<>();

    public String createNode() {
        Node node = new Node();
        nodes.put(node.getNodeId(), node);
        return node.getNodeId();
    }
    public void getTransmitionTx() {
        for (Node node : nodes.values()) {
            ConcurrentHashMap<String, String> receivedBlocks = node.getReceivedBlocks();
            for (String blockHash : receivedBlocks.keySet()) {
                String from;
                if (receivedBlocks.get(blockHash) != null) {

                    Map<String, String> nodeInf = new HashMap<>();

                    nodeInf.put("from", node.getNodeId());
                    nodeInf.put("to", gson.toJson(node.getNetwork().getConnList()));
                    nodeInf.put("blockHash", transactionInfs(node.getTxsFromTxPool()));

                    return gson.toJson(nodeInf);
                    // TODO: 전송
                    receivedBlocks.put(blockHash, null);
                }
            }
        }
    }

    public Node getNode(String nodeId){
        return nodes.get(nodeId);
    }

}

