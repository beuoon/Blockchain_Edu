package blockchain;

import blockchain.consensus.ProofOfWork;
import blockchain.merkletree.MerkleTree;
import blockchain.transaction.Transaction;
import utils.Utils;

import java.io.*;
import java.util.ArrayList;

public class Block implements Serializable{

    private long timestamp;
    private Transaction[] transactions;
    private byte[] prevBlockHash;
    private byte[] hash;
    private int nonce;
    private int height;

    //genesis block
    public Block(Transaction coinbase) {
        this(new Transaction[]{coinbase}, new byte[0]);
    }

    //normal block
    public Block(Transaction[] transactions, byte[] prevBlockHash) {
        this.timestamp = System.currentTimeMillis();
        this.transactions = transactions;
        this.prevBlockHash = prevBlockHash;
        ProofOfWork.mine(this);
    }

    //bytes to block
    public Block(byte[] b) {
        final ByteArrayInputStream bis = new ByteArrayInputStream(b);
        ObjectInputStream ois = null;
        Block block = null;
        try {
            ois = new ObjectInputStream(bis);
            block = (Block) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.timestamp = block.timestamp;
        this.transactions = block.transactions;
        this.prevBlockHash = block.prevBlockHash;
        this.hash = block.hash;
        this.nonce = block.nonce;
    }

    public byte[] getBytesExceptHash() {
        return Utils.bytesConcat(String.valueOf(timestamp).getBytes(), Utils.sha256(Utils.toBytes(transactions)), prevBlockHash);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public byte[] getPrevBlockHash() {
        return prevBlockHash;
    }

    public byte[] getHash() {
        return hash;
    }

    public int getNonce() {
        return nonce;
    }

    public void setHash(byte[] hash) {
        this.hash = hash;
    }

    public void setNonce(int nonce) {
        this.nonce = nonce;
    }

    public Transaction[] getTransactions() {
        return transactions;
    }

    public byte[] hashTransactions() {
        ArrayList<byte[]> txHashes = new ArrayList<>();

        for(Transaction tx : getTransactions()){
            txHashes.add(Utils.toBytes(tx.Hash()));
        }

        return Utils.sha256(Utils.toBytes(txHashes));
    }
}
