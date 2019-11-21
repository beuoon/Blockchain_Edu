import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class Blockchain {

    private Db db;
    byte[] tip;
    final String genesisCoinbaseData = "The Times 03/Jan/2009 Chancellor on brink of second bailout for banks";

    public Blockchain(String address, Db db) throws Exception{
        Transaction coinbaseTx = new Transaction(address, genesisCoinbaseData);
        Block genesisBlock = new Block(coinbaseTx); // create genesis block

        db.getBucket("blocks").put(new String(genesisBlock.getHash()), genesisBlock.toBytes()); // put genesis block to blockchain
        db.getBucket("blocks").put("l", genesisBlock.getHash());

        this.db = db;
        this.tip = genesisBlock.getHash();
    }

    public Blockchain(Db db) {
        this.db = db;
        this.tip = new byte[]{};
    }


    public void MineBlock(Transaction[] transactions) throws Exception{
        Db.Bucket bucket = db.getBucket("blocks");
        byte[] lastHash = bucket.get("l");
        Block newBlock = new Block(transactions, lastHash);
        bucket.put(new String(newBlock.getHash()) ,newBlock.toBytes());

        bucket.put("l", newBlock.getHash());
        this.tip = newBlock.getHash();
    }

    public ArrayList<TxOutput> findUTXO(byte[] pubkeyHash) {
        ArrayList<TxOutput> UTXOs = new ArrayList();
        ArrayList<Transaction> unspentTransactions = findUnspentTransactions(pubkeyHash);

        for(Transaction tx : unspentTransactions) {
            for( TxOutput out : tx.getVout()) {
                if(out.isLockedWithKey(pubkeyHash)) {
                    UTXOs.add(out);
                }
            }
        }

        return UTXOs;
    }

    public ArrayList<Transaction> findUnspentTransactions(byte[] pubKeysHash) {
        ArrayList<Transaction> unspentTxs = new ArrayList();
        HashMap<String, ArrayList<Integer>> spentTxOs = new HashMap();
        Iterator<Block> itr = this.iterator();

        while(itr.hasNext()){
            Block block = itr.next();
            for( Transaction tx : block.getTransactions()) {
                String txId = Utils.byteArrayToHexString(tx.id);

                OutPuts:
                for(int i=0; i<tx.getVout().size(); i++) {
                    TxOutput out = tx.getVout().get(i);

                    if(spentTxOs.get(txId) != null) {
                        for (Integer spentOutIdx : spentTxOs.get(txId)) {
                            if(spentOutIdx.equals(i)) {
                                continue OutPuts;
                            }
                        }
                    }

                    if(out.isLockedWithKey(pubKeysHash)) {
                        unspentTxs.add(tx);
                    }
                }

                if(tx.isCoinBase() == false) {
                    for(TxInput in : tx.getVin()) {
                        if (in.usesKey(pubKeysHash)) {
                            String inTxId = in.getTxId();
                            if(spentTxOs.get(inTxId) == null) spentTxOs.put(inTxId, new ArrayList<Integer>());
                            spentTxOs.get(inTxId).add(in.getvOut());
                        }
                    }
                }
            }

            if(block.getPrevBlockHash().length == 0) {
                break;
            }
        }

        return unspentTxs;
    }

    public Pair<Integer, HashMap<String, ArrayList<Integer>>> findSpendableOutputs(byte[] pubkeyHash, int amount) {
        HashMap<String, ArrayList<Integer>> unspentOutputs = new HashMap();
        ArrayList<Transaction> unspentTxs = findUnspentTransactions(pubkeyHash);
        int accumulated = 0;

        Work:
        for(Transaction tx : unspentTxs) {
            String txId = Utils.byteArrayToHexString(tx.getId());

            for(int i=0; i<tx.getVout().size(); i++) {
                TxOutput out = tx.getVout().get(i);
                if(out.isLockedWithKey(pubkeyHash) && accumulated < amount) {
                    accumulated += out.getValue();
                    if(unspentOutputs.get(txId) == null) unspentOutputs.put(txId, new ArrayList<Integer>());
                    unspentOutputs.get(txId).add(i);
                }

                if( accumulated >= amount ){
                    break Work;
                }
            }
        }

        return new Pair(accumulated, unspentOutputs);
    }

    public Transaction newUTXOTransaction(Wallet from, String to, int amount) throws Exception{
        ArrayList<TxInput> inputs = new ArrayList();
        ArrayList<TxOutput> outputs = new ArrayList();

        byte[] pubkeyHash = Utils.ripemd160(Utils.sha256(from.getPublicKey().getEncoded()));
        Pair<Integer, HashMap<String, ArrayList<Integer>>> spendableOutputs = findSpendableOutputs(pubkeyHash, amount);
        int acc = spendableOutputs.getKey();
        HashMap<String, ArrayList<Integer>> validOutputs = spendableOutputs.getValue();

        if(acc < amount){
            throw new Exception("Error : Not Enough funds");
        }

        Iterator<String> keys = validOutputs.keySet().iterator();
        while(keys.hasNext()){
            String txid = keys.next();
            ArrayList<Integer> outs = validOutputs.get(txid);

            for(int out : outs) {
                TxInput input = new TxInput(txid, out, from.getPublicKey().getEncoded(), new byte[]{0});
                inputs.add(input);
            }
        }

        outputs.add(new TxOutput(amount, to));
        if(acc > amount) {
            outputs.add(new TxOutput(acc-amount, from.getAddress()));
        }

        Transaction tx = new Transaction(new byte[]{}, inputs, outputs);

        return tx;
    }

    public Iterator<Block> iterator() {
        return new BcItr(db, tip);
    }

    private class BcItr implements Iterator<Block> {
        private byte[] currentHash;
        private Db db;

        public BcItr(Db db, byte[] tip) {
            this.currentHash = tip;
            this.db = db;
        }

        public boolean hasNext() {
            byte[] b = db.getBucket("blocks").get(new String(currentHash));
            if( b == null) return false;

            Block block = new Block(b);
            return true;
        }

        public void remove() {
            System.out.println("you can not remove it!");
        }

        public Block next() {
            Block block = new Block(db.getBucket("blocks").get(new String(currentHash)));
            currentHash = block.getPrevBlockHash();

            return block;
        }
    }

}
