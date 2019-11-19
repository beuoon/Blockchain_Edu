import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;

public class Block {

    private long timestamp;
    private String data;
    private byte[] prevBlockHash;
    private byte[] hash;
    private int nonce;

    //genesis block
    public Block() throws Exception{
        this("Genesis Block", new byte[0]);
    }

    //normal block
    public Block(String data, byte[] prevBlockHash) throws Exception {
        this.timestamp = System.currentTimeMillis();
        this.data = data;
        this.prevBlockHash = prevBlockHash;
        new ProofOfWork(this, 18).run();
    }

    public byte[] getBytesExceptHash() throws Exception {
        return Utils.bytesConcat(String.valueOf(timestamp).getBytes(), data.getBytes(), prevBlockHash);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getData() {
        return data;
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
}
