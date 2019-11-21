import org.bitcoinj.core.Base58;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.Arrays;

public class TxOutput implements Serializable {
    private int value;
    private byte[] publicKeyHash;

    public TxOutput(int value, String address) {
        this.value = value;
        Lock(address);
    }

    public TxOutput(TxOutput txOutput) {
        this.value = txOutput.value;
        this.publicKeyHash = txOutput.publicKeyHash;
    }

    public void Lock(String address) {
        byte[] pubkeyHash = Base58.decode(address);
        pubkeyHash = Arrays.copyOfRange(pubkeyHash, 1, pubkeyHash.length - 4);
        this.publicKeyHash = pubkeyHash;
    }

    public boolean isLockedWithKey(byte[] pubKeyHash) {
        return Arrays.equals(this.publicKeyHash, pubKeyHash);
    }

    public int getValue() {
        return value;
    }
    public byte[] getPublicKeyHash() {
        return publicKeyHash;
    }

}
