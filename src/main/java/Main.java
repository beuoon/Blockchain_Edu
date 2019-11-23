import blockchain.transaction.Transaction;
import blockchain.wallet.Wallets;
import node.network.Network;
import node.network.Node;
import utils.Utils;

import java.util.ArrayList;

public class Main {
    public static void main(String args[]) throws Exception {
        ArrayList<Node> nodes = new ArrayList<>();

        Node firstNode = new Node();
        firstNode.createWallet();
        firstNode.createGenesisBlock(firstNode.getWallet().getAddress());
        nodes.add(firstNode);

        for (int i = 1; i < 4; i++) {
            Node node = new Node();
            node.createWallet();
            node.createNullBlockchain();
            node.setGenesisBlock(firstNode.getGenesisBlock());
            nodes.add(node);
        }

        for(Node node : nodes){
            node.getNetwork().autoConnect(3);
        }

        for(Node node : nodes)
            node.start();

        // Test
        nodes.get(0).send(nodes.get(1).getWallet().getAddress(), 30);

        Thread.sleep(10000);

        for (Node node: nodes)
            node.checkBalance();

        nodes.get(0).send(nodes.get(3).getWallet().getAddress(), 20);
        nodes.get(1).send(nodes.get(3).getWallet().getAddress(), 10);

        Thread.sleep(10000);

        for (Node node: nodes)
            node.checkBalance();

        for(Node node : nodes)
            node.close();

        System.out.print("끝");
    }

}
