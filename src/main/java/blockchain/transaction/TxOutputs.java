package blockchain.transaction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class TxOutputs implements Serializable {
    public HashMap<Integer, TxOutput> getOutputs(){
        return Outputs;
    }

    public void setOutputs(HashMap<Integer, TxOutput> outputs) {
        Outputs = outputs;
    }

    private HashMap<Integer, TxOutput> Outputs = new HashMap<>();
}
