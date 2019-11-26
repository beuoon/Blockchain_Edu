package blockchainCore;

import blockchainCore.node.network.Node;

public class Main {
    public static void main(String[] args) {
        BlockchainCore bc = new BlockchainCore();

        // 노드 생성
        String node1 = bc.createNode();
        String node2 = bc.createNode();

        // 지갑 주소
        String node1Address = bc.getNode(node1).getAddresses().get(0);
        String node2Address = bc.getNode(node2).getAddresses().get(0);

        // 노드 연결
        bc.createConnection(node1, node2);

        // 거래
        String from = node1Address, to = node2Address;
        for (int i = 0; i < 10; i++) {
            bc.sendBTC(node1, from, to, 20);

            while (true) {
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                if (bc.getNode(node1).getBlockChain().getLastHeight() <= i) continue;
                if (bc.getNode(node2).getBlockChain().getLastHeight() <= i) continue;
                break;
            }

            String temp = to;
            to = from;
            from = temp;
        }

        // 블록 검증
        bc.getNode(node1).getBlockChain().getDb().getBucket("blocks");
    }
}
