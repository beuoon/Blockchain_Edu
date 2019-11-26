package blockchainCore.blockchain.transaction;

import blockchainCore.utils.Utils;

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

        Vin.add(new TxInput(new byte[]{}, -1, null, data.getBytes()));
        Vout.add(new TxOutput(subsidy, to));
        id = Hash();
    }

    public byte[] Hash() {
        return Utils.sha256(Utils.bytesConcat(Utils.toBytes(Vin), Utils.toBytes(Vout)));
    }
}