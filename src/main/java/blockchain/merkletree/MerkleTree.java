package blockchain.merkletree;

import java.util.ArrayList;

public class MerkleTree {
    MerkleNode rootNode;

    public MerkleTree(ArrayList<byte[]> data) {
        ArrayList<MerkleNode> nodes = new ArrayList<>();

        if(data.size() % 2 != 0) {
            data.add(data.get(data.size()-1));
        }

        for(byte[] datum : data) {
            MerkleNode node = new MerkleNode(null, null, datum);
            nodes.add(node);
        }

        for(int i=0; i<data.size()/2; i++) {
            ArrayList<MerkleNode> newLevel = new ArrayList<>();

            for(int j=0; j<nodes.size(); j+=2) {
                MerkleNode node = new MerkleNode(nodes.get(j), nodes.get(j+1), null);
                newLevel.add(node);
            }
            nodes = newLevel;
        }

        rootNode = nodes.get(0);
    }
}
