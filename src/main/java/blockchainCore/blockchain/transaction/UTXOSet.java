package blockchainCore.blockchain.transaction;

import blockchainCore.DB.Bucket;
import blockchainCore.DB.Cursor;
import blockchainCore.DB.Db;
import blockchainCore.blockchain.Block;
import blockchainCore.blockchain.Blockchain;
import blockchainCore.utils.Utils;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class UTXOSet {
    private final String utxoBucket = "chainstate";
    private Blockchain bc;
    private Db db;

    public UTXOSet(Blockchain bc) {
        this.bc = bc;
        this.db = bc.getDb();
    }
    public void reIndex() {
        db.getBucket(utxoBucket).clear();
        HashMap<String, TxOutputs> UTXO = bc.findUTXO();

        Iterator<String> itr = UTXO.keySet().iterator();
        while(itr.hasNext()){
            String txId = itr.next();
            TxOutputs outs = UTXO.get(txId);

            db.getBucket(utxoBucket).put(txId, Utils.toBytes(outs));
        }
    }

    public void reIndex(HashMap<String, TxOutputs> utxoset) {
        db.getBucket(utxoBucket).clear();
        Iterator<String> itr = utxoset.keySet().iterator();
        while(itr.hasNext()){
            String txId = itr.next();
            TxOutputs outs = utxoset.get(txId);

            db.getBucket(utxoBucket).put(txId, Utils.toBytes(outs));
        }
    }

    public ArrayList<TxOutput> findUTXO(byte[] pubkeyHash) {
        ArrayList<TxOutput> UTXOs = new ArrayList();

        Bucket b = db.getBucket(utxoBucket);
        Cursor c = b.Cursor();

        while(c.hasNext()){
            Pair<String, byte[]> kv = c.next();
            TxOutputs outs = Utils.toObject(kv.getValue());

            for(TxOutput out : outs.getOutputs().values()) {
                if(out.isLockedWithKey(pubkeyHash)) {
                    UTXOs.add(out);
                }
            }
        }

        return UTXOs;
    }

    public void update(Block block) {
        Bucket b = db.getBucket(utxoBucket);

        for(Transaction tx : block.getTransactions()) {
            if(!tx.isCoinBase()) {
                for(TxInput vin : tx.getVin()) {
                    String txId = Utils.toHexString(vin.getTxId());
                    TxOutputs outs = Utils.toObject(b.get(txId));

                    outs.getOutputs().remove(vin.getvOut());

                    if(outs.getOutputs().isEmpty()) b.delete(txId);
                    else b.put(txId, Utils.toBytes(outs));
                }
            }

            TxOutputs newOutputs = new TxOutputs();
            ArrayList<TxOutput> vouts = tx.getVout();
            for (int i = 0; i < vouts.size(); i++)
                newOutputs.getOutputs().put(i, vouts.get(i));

            b.put(Utils.toHexString(tx.getId()), Utils.toBytes(newOutputs));
        }

    }
    public boolean validVin(TxInput txInput) {
        Bucket b = db.getBucket(utxoBucket);
        byte[] temp = b.get(Utils.toHexString(txInput.getTxId()));
        if (temp == null) return false;

        TxOutputs txOutputs = Utils.toObject(temp);
        return txOutputs.getOutputs().containsKey(txInput.getvOut());
    }

    public Pair<Integer, HashMap<String, ArrayList<Integer>>> findSpendableOutputs(byte[] pubkeyHash, int amount) {
        HashMap<String, ArrayList<Integer>> unspentOutputs = new HashMap<>();
        int accumulated = 0;

        Bucket b = db.getBucket(utxoBucket);
        Cursor c = b.Cursor();

        while(c.hasNext()) {
            Pair<String, byte[]> kv = c.next();
            String txId = kv.getKey();
            if (kv.getValue() == null)
                System.out.println("DEBUGGGGGG");
            TxOutputs outs = Utils.toObject(kv.getValue());

            for (Integer key : outs.getOutputs().keySet()) {
                TxOutput out = outs.getOutputs().get(key);

                if(out.isLockedWithKey(pubkeyHash) && accumulated < amount) {
                    accumulated+= out.getValue();
                    if(!unspentOutputs.containsKey(txId)) unspentOutputs.put(txId, new ArrayList<>());
                    unspentOutputs.get(txId).add(key);
                }
            }
        }

        return new Pair<>(accumulated, unspentOutputs);
    }

    public Blockchain getBc() {
        return bc;
    }

}
