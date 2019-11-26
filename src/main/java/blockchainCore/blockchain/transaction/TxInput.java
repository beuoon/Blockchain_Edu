package blockchainCore.blockchain.transaction;

import blockchainCore.utils.Utils;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.Arrays;

public class TxInput implements Serializable {

    private byte[] txId;
    private int vOut;
    private PublicKey pubKey;
    private byte[] signature;


    public TxInput(byte[] txId, int vOut, PublicKey pubKey, byte[] signature) {
        this.txId = txId;
        this.vOut = vOut;
        this.pubKey = pubKey;
        this.signature = signature;
    }

    public boolean usesKey(byte[] pubKeyHash) {
        byte[] lockingHash = Utils.ripemd160(Utils.sha256(pubKey.getEncoded()));
        return Arrays.equals(lockingHash, pubKeyHash);
    }

    public void setPubKey(PublicKey pubKey) {
        this.pubKey = pubKey;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }


    @Override
    public boolean equals(Object o) {
        if (o instanceof TxInput) {
            TxInput txInput = (TxInput)o;
            return (Arrays.equals(txId, txInput.txId) && vOut == txInput.vOut);
        }
        return false;
    }

    public byte[] getTxId() {
        return txId;
    }

    public int getvOut() {
        return vOut;
    }
    public byte[] getSignature() {
        return signature;
    }
    public PublicKey getPubKey() {
        return pubKey;
    }
}