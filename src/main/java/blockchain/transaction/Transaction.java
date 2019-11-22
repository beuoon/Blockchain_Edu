package blockchain.transaction;

import utils.Utils;

import java.io.*;
import java.security.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Transaction implements Serializable {

    private byte[] id;
    private ArrayList<TxInput> Vin = new ArrayList();
    private ArrayList<TxOutput> Vout = new ArrayList();

    //temp value
    final int subsidy = 50;

    //coinbasetx
    public Transaction(String to, String data) {
        if(data.equals("")) {
            data = String.format("conbase Tx %s", Utils.sha256(Float.valueOf(new SecureRandom().nextFloat()).toString().getBytes()));
        }

        id = new byte[]{};
        Vin.add(new TxInput(new byte[]{}, -1, null, data.getBytes()));
        Vout.add(new TxOutput(subsidy, to));

        setId(Hash());
    }

    public Transaction(byte[] id, ArrayList<TxInput> vin, ArrayList<TxOutput> vout) {
        this.id = id;
        this.Vin = vin;
        this.Vout = vout;
        setId(Hash());
    }

    public Transaction(Transaction tx) {
        this.id = tx.id;
        this.Vin = tx.Vin;
        this.Vout = tx.Vout;
    }

    public byte[] Hash() {
        return Utils.sha256(Utils.bytesConcat(Utils.toBytes(Vin), Utils.toBytes(Vout)));
    }


    public void sign(PrivateKey privateKey, HashMap<String, Transaction> prevTxs) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        if(isCoinBase()) return;

        Transaction txCopy = trimmedCopy();
        for(int i=0; i<txCopy.getVin().size(); i++) {
            TxInput vin = txCopy.getVin().get(i);
            Transaction prevTx = prevTxs.get(Utils.byteArrayToHexString(vin.getTxId()));
            byte[] digest = Utils.sha256(Utils.bytesConcat(Utils.toBytes(txCopy.Hash()), prevTx.getVout().get(vin.getvOut()).getPublicKeyHash()));

            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initSign(privateKey);
            sig.update(digest);
            byte[] signature = sig.sign();
            Vin.get(i).setSignature(signature);
        }
    }

    public boolean Verify(HashMap<String,Transaction> prevTxs) throws Exception {
        if(isCoinBase()) return true;

        for(TxInput vin : Vin) {
            if(prevTxs.get(Utils.byteArrayToHexString(vin.getTxId())) == null){
                throw new Exception("ERROR: Rrevious transaction is not correct");
            }
        }

        Transaction txCopy = trimmedCopy();

        for(int i=0; i< Vin.size(); i++) {
            TxInput vin = Vin.get(i);
            Transaction prevTx = prevTxs.get(Utils.byteArrayToHexString(vin.getTxId()));
            byte[] digest = Utils.sha256(Utils.bytesConcat(Utils.toBytes(txCopy.Hash()), prevTx.getVout().get(vin.getvOut()).getPublicKeyHash()));

            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initVerify(Vin.get(i).getPubKey());
            sig.update(digest);
            if(!sig.verify(Vin.get(i).getSignature())) return false;
        }
        return true;
    }

    private Transaction trimmedCopy() {
        ArrayList<TxInput> inputs = new ArrayList<TxInput>();
        ArrayList<TxOutput> outputs = new ArrayList<TxOutput>();

        for(TxInput vin : Vin) {
            inputs.add(new TxInput(vin.getTxId(), vin.getvOut(), null, null));
        }

        for(TxOutput vout : Vout) {
            outputs.add(new TxOutput(vout));
        }

        return new Transaction(id, inputs, outputs);
    }

    public boolean isCoinBase() {
        if(Vin.size() == 1 && Vin.get(0).getTxId().length == 0 && Vin.get(0).getvOut() == -1) return true;
        return false;
    }

    public void setId(byte[] id){
        this.id = id;
    }

    public byte[] getId() {
        return id;
    }

    public ArrayList<TxInput> getVin() {
        return Vin;
    }

    public ArrayList<TxOutput> getVout() {
        return Vout;
    }

    public static Transaction bytesToTx(byte[] bytes) {
        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;

        Transaction tx = null;

        try {
            bis = new ByteArrayInputStream(bytes);
            ois = new ObjectInputStream(bis);
            tx = (Transaction)ois.readObject();
            ois.close();
        } catch (Exception ignored) {
        } finally {
            if (bis != null) try { bis.close(); } catch (IOException ignored) {}
            if (ois != null) try { ois.close(); } catch (IOException ignored) {}
        }

        return tx;
    }
}