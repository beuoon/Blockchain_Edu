package blockchainCore.blockchain.transaction;

import blockchainCore.utils.Utils;

import java.io.Serializable;

public class TxOutput implements Serializable {
    private int value;
    private byte[] publicKeyHash;

    public TxOutput(int value, String address) {
        this.value = value;
        publicKeyHash = Utils.toBytes(address);
    }
}
