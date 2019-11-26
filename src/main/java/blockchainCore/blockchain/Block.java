package blockchainCore.blockchain;

import blockchainCore.blockchain.transaction.Transaction;
import blockchainCore.utils.Utils;

import java.io.*;

public class Block implements Serializable{

    private long timestamp;
    private Transaction[] transactions;
    private byte[] prevBlockHash;
    private byte[] hash;
    private int nonce;
    private int height;

    //genesis block
    public Block(Transaction coinbase) {
        this(new Transaction[]{coinbase}, new byte[0], 0);
    }

    //normal block
    public Block(Transaction[] transactions, byte[] prevBlockHash, int height) {
        this.timestamp = System.currentTimeMillis();
        this.transactions = transactions;
        this.prevBlockHash = prevBlockHash;
        this.height = height;
    }

    public byte[] getBytesExceptHash() {
        return Utils.bytesConcat(String.valueOf(timestamp).getBytes(), Utils.sha256(Utils.toBytes(transactions)), prevBlockHash);
    }

    public byte[] getHash() { return hash; }
    public int getNonce() { return nonce; }
    public void setHash(byte[] hash) { this.hash = hash; }
    public void setNonce(int nonce) { this.nonce = nonce; }
}
