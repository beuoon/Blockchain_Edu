package blockchain.transaction;

import java.io.Serializable;
import java.util.ArrayList;

public class TxOutputs implements Serializable {
    public ArrayList<TxOutput> getOutputs(){
        return Outputs;
    }

    public void setOutputs(ArrayList<TxOutput> outputs) {
        Outputs = outputs;
    }

    private ArrayList<TxOutput> Outputs = new ArrayList<>();
}
