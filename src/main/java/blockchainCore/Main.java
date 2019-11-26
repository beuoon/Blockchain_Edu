package blockchainCore;

import blockchainCore.node.network.Node;

import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static void main(String args[]) throws Exception {
        ArrayList<Node> nodes = new ArrayList<>();

        Node firstNode = new Node();
        firstNode.createWallet();
        firstNode.createGenesisBlock(firstNode.getWallet().getAddress());
        nodes.add(firstNode);

        int NODE_NUM = 5;
        for (int i = 1; i < NODE_NUM; i++) {
            Node node = new Node();
            node.createWallet();
            node.createNullBlockchain();
            node.setGenesisBlock(firstNode.getGenesisBlock());
            nodes.add(node);
        }

        for(Node node : nodes)
            node.getNetwork().autoConnect(NODE_NUM-1);

        for(Node node : nodes)
            node.start();

        Scanner scanner = new Scanner(System.in);

        // Test
        int from = 0, to = 1;
        while (true) {
            nodes.get(from).send(nodes.get(to).getWallet().getAddress(), 10);
            from = to;
            to = (to + 1) % NODE_NUM;

            System.out.println("입력 please");
            if (scanner.next().equals("end"))
                break;
        }

        nodes.get(4).close();
        nodes.remove(--NODE_NUM);

        for (Node node : nodes)
            node.getNetwork().closeConnection();
        ;

        nodes.get(0).connect(nodes.get(1));
        nodes.get(2).connect(nodes.get(3));

        from = 0; to = 1;
        while (true) {
            nodes.get(from).send(nodes.get(to).getWallet().getAddress(), 10);
            from = to;
            to = (to + 1) % NODE_NUM;

            System.out.println("입력 please");
            if (scanner.next().equals("end"))
                break;
        }

        nodes.get(1).connect(nodes.get(2));

        // Test
        while (true) {
            nodes.get(from).send(nodes.get(to).getWallet().getAddress(), 10);
            from = to;
            to = (to + 1) % NODE_NUM;

            System.out.println("입력 please");
            if (scanner.next().equals("end"))
                break;
        }

        for (Node node: nodes)
            node.checkBalance();

        for(Node node : nodes)
            node.close();

        System.out.print("끝");
    }

}
