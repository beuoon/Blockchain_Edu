import java.io.Serializable;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Arrays;

public class TxInput implements Serializable {

    public String getTxId() {
        return txId;
    }

    public int getvOut() {
        return vOut;
    }

    private String txId;
    private int vOut;
    private byte[] pubKey;
    private byte[] signature;

    public TxInput(String txId, int vOut, byte[] pubKey, byte[] signature) {
        this.txId = txId;
        this.vOut = vOut;
        this.pubKey = pubKey;
        this.signature = signature;
    }

    public TxInput(TxInput txinput) {
        this.txId = txinput.txId;
        this.vOut = txinput.vOut;
        this.pubKey = txinput.pubKey;
        this.signature = txinput.signature;
    }

    public boolean usesKey(byte[] pubKeyHash) {
        byte[] lockingHash = Utils.ripemd160(Utils.sha256(pubKey));
        return Arrays.equals(lockingHash, pubKeyHash);
    }

    public void setPubKey(byte[] pubKey) {
        this.pubKey = pubKey;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }
}