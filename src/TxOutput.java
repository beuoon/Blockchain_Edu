import java.io.Serializable;

public class TxOutput implements Serializable {
    public int getValue() {
        return value;
    }

    int value;
    String scriptPubKey;

    public TxOutput(int value, String scriptPubKey) {
        this.value = value;
        this.scriptPubKey = scriptPubKey;
    }

    public boolean canBelockedWith(String unlockingData) {
        return scriptPubKey.equals(unlockingData);
    }

}
