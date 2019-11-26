package blockchainCore;

import blockchainCore.node.network.Node;

import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        final int NODE_NUM = 10, TX_NUM = 50;

        // 노드 생성
        ArrayList<Node> nodes = new ArrayList<>();

        Node newNode = new Node(); newNode.createGenesisBlock(); nodes.add(newNode);
        for (int i = 1; i < NODE_NUM; i++) {
            newNode = new Node();
            newNode.createNullBlockchain();
            nodes.add(newNode);
        }

        // 노드 연결
        for (Node node : nodes) node.getNetwork().autoConnect(NODE_NUM / 3 * 2);

        // 스레드 시작
        for (Node node : nodes) node.start();

        // 거래
        int from = 0, to = 1;
        for (int i = 0; i < TX_NUM; i++) {
            String fromAddress = nodes.get(from).getAddresses().get(0);
            String toAddress = nodes.get(to).getAddresses().get(0);

            nodes.get(from).send(fromAddress, toAddress, 20);

            Wait:
            while (true) {
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                for (Node node : nodes)
                    if (node.getBlockChain().getLastHeight() <= i) continue Wait;
                break;
            }

            from = to;
            to = (to + 1) % NODE_NUM;
        }

        // 문제

        // 종료
        for (Node node : nodes) node.close();
    }
}
