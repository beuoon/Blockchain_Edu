package blockchainCore.blockchain.merkletree;

import blockchainCore.utils.Utils;

public class MerkleNode {
    private MerkleNode left;
    private MerkleNode right;
    private byte[] data;


    public MerkleNode() {
        left = null;
        right = null;
        data = null;
    }

    public MerkleNode(MerkleNode left, MerkleNode right, byte[] data) {
        MerkleNode mNode = new MerkleNode();

        if (left == null && right == null) {
            byte[] hash = Utils.sha256(data);
            mNode.data = hash;
        } else {
            byte[] prevHashes = Utils.bytesConcat(left.data, right.data);
            byte[] hash = Utils.sha256(prevHashes);
            mNode.data = hash;
        }

        mNode.left = left;
        mNode.right = right;
    }

    public MerkleNode getLeft() {
        return left;
    }

    public void setLeft(MerkleNode left) {
        this.left = left;
    }

    public MerkleNode getRight() {
        return right;
    }

    public void setRight(MerkleNode right) {
        this.right = right;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}