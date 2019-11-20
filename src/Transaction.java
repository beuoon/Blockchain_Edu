import java.io.*;
import java.util.ArrayList;

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
        Vin.add(new TxInput("", -1, data));
        Vout.add(new TxOutput(subsidy, to));

        this.setId();
    }

    public Transaction(byte[] id, ArrayList<TxInput> vin, ArrayList<TxOutput> vout) {
        this.id = id;
        this.Vin = vin;
        this.Vout = vout;
        setId();
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