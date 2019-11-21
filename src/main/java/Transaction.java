import java.io.*;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.HashMap;

public class Transaction implements Serializable {

    final static int COINBASE = 1;

    byte[] id;
    ArrayList<TxInput> Vin = new ArrayList();
    ArrayList<TxOutput> Vout = new ArrayList();

    //temp value
    final int subsidy = 50;

    //coinbasetx
    public Transaction(String to, String data) {
        if(data.equals("")) {
            data = String.format("Reward to '%s'", to);
        }

        id = new byte[]{};
        Vin.add(new TxInput("", -1, new byte[]{}, data.getBytes()));
        Vout.add(new TxOutput(subsidy, to));

        this.setId();
    }

    public Transaction(byte[] id, ArrayList<TxInput> vin, ArrayList<TxOutput> vout) {
        this.id = id;
        this.Vin = vin;
        this.Vout = vout;
        setId();
    }

    public void sign(PrivateKey privateKey, HashMap<String, Transaction> prevTxs) {
        if(isCoinBase()) return;

        Transaction txCopy = trimmedCopy();
        for(int i=0; i<txCopy.getVin().size(); i++) {
            TxInput vin = txCopy.getVin().get(i);
            Transaction prevTx = prevTxs.get(vin.getTxId());
            txCopy.getVin().get(i).setSignature(null);
            txCopy.getVin().get(i).setPubKey(prevTx.getVout().get(vin.getvOut()).getPublicKeyHash());

        }
    }

    private Transaction trimmedCopy() {
        ArrayList<TxInput> inputs = new ArrayList<TxInput>();
        ArrayList<TxOutput> outputs = new ArrayList<TxOutput>();

        for(TxInput vin : Vin) {
            inputs.add(new TxInput(vin));
        }

        for(TxOutput vout : Vout) {
            outputs.add(vout);
        }

        return new Transaction(id, inputs, outputs);
    }


    public boolean isCoinBase() {
        if(Vin.size() == 1 && Vin.get(0).getTxId().length() == 0 && Vin.get(0).getvOut() == -1) return true;
        return false;
    }

    private void setId(){
        byte[] b = Utils.toBytes(Vin, Vout);
        id = Utils.sha256(b);
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

}