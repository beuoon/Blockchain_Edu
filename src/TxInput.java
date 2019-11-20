import java.io.Serializable;

public class TxInput implements Serializable {
    public String getTxId() {
        return txId;
    }

    public int getvOut() {
        return vOut;
    }

    private String txId;
    private int vOut;
    private String scriptSig;

    public TxInput(String txId, int vOut, String scriptSig) {
        this.txId = txId;
        this.vOut = vOut;
        this.scriptSig = scriptSig;
    }

    public boolean canUnlockOutputWith(String unlockingData) {
        return scriptSig.equals(unlockingData);
    }
}