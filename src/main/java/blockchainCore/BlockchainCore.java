package blockchainCore;

import blockchainCore.node.network.Node;

import java.util.ArrayList;
import java.util.HashMap;

public class BlockchainCore {
    HashMap<String, Node> nodes = new HashMap<>();

    public String createNode() {
        Node node = new Node();
        nodes.put(node.getNodeId(), node);
        return node.getNodeId();
    }

    public Node getNode(String nodeId){
        return nodes.get(nodeId);
    }

}

