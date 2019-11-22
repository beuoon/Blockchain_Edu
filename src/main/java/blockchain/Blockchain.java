package blockchain;

import DB.Bucket;
import DB.Db;
import blockchain.consensus.ProofOfWork;
import blockchain.transaction.*;
import blockchain.wallet.Wallet;
import utils.Pair;
import utils.Utils;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

public class Blockchain {
    private Db db;
    byte[] tip;
    final String genesisCoinbaseData = "The Times 03/Jan/2009 Chancellor on brink of second bailout for banks";

    public Blockchain(String address, Db db) {
        Transaction coinbaseTx = new Transaction(address, genesisCoinbaseData);
        Block genesisBlock = new Block(coinbaseTx); // create genesis block

        db.getBucket("blocks").put(new String(genesisBlock.getHash()), Utils.toBytes(genesisBlock)); // put genesis block to blockchain
        db.getBucket("blocks").put("l", genesisBlock.getHash());

        this.db = db;
        this.tip = genesisBlock.getHash();

        UTXOSet utxoSet = new UTXOSet(this);
        utxoSet.reIndex();
    }

    public Blockchain(Db db) {
        this.db = db;
        this.tip = null;
    }


    public Block mineBlock(Transaction[] transactions)  {
        Bucket bucket = db.getBucket("blocks");
        byte[] lastHash = bucket.get("l");

        Block newBlock = new Block(transactions, lastHash);
        bucket.put(new String(newBlock.getHash()), Utils.toBytes(newBlock));

        bucket.put("l", newBlock.getHash());
        this.tip = newBlock.getHash();

        return newBlock;
    }
    public boolean addBlock(Block block) {
        Bucket bucket = db.getBucket("blocks");
        byte[] lastHash = tip;

        try { // 합의
            ProofOfWork.validate(block);
        } catch (Exception e) { return false; }

        // 트랜잭션 검증
        for (Transaction tx : block.getTransactions()) {
            try {
                if (!VerifyTransaction(tx))
                    return false;
            } catch (Exception e) { return false; }
        }

        // TODO: 블록 검증

        bucket.put(new String(block.getHash()), Utils.toBytes(block));
        bucket.put("l", block.getHash());
        tip = block.getHash();

        return true;
    }

    public ArrayList<Transaction> findUnspentTransactions(byte[] pubKeysHash) {
        ArrayList<Transaction> unspentTxs = new ArrayList();
        HashMap<String, ArrayList<Integer>> spentTxOs = new HashMap();
        Iterator<Block> itr = this.iterator();

        while(itr.hasNext()){
            Block block = itr.next();
            for( Transaction tx : block.getTransactions()) {
                String txId = Utils.byteArrayToHexString(tx.getId());

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
                            byte[] inTxId = in.getTxId();
                            if(spentTxOs.get(Utils.byteArrayToHexString(inTxId)) == null) spentTxOs.put(Utils.byteArrayToHexString(inTxId), new ArrayList<Integer>());
                            spentTxOs.get(Utils.byteArrayToHexString(inTxId)).add(in.getvOut());
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

    public HashMap<String, TxOutputs> findUTXO() {
        HashMap<String, TxOutputs> UTXO = new HashMap<>();
        HashMap<String, ArrayList<Integer>> spentTXOs = new HashMap<>();

        Iterator<Block> itr = iterator();
        while(itr.hasNext()){
            Block block = itr.next();

            for(Transaction tx : block.getTransactions()) {
                String txId = Utils.byteArrayToHexString(tx.getId());

                Outputs:
                for(int outIdx = 0; outIdx < tx.getVout().size(); outIdx++){
                    TxOutput out = tx.getVout().get(outIdx);

                    if(spentTXOs.get(txId) != null) {
                        for(Integer spentOutIdx : spentTXOs.get(txId)) {
                            if(spentOutIdx == outIdx) {
                                continue Outputs;
                            }
                        }
                    }

                    TxOutputs outs = UTXO.get(txId);
                    if(outs == null) {
                        outs = new TxOutputs();
                        UTXO.put(txId, outs);
                    }
                    outs.getOutputs().add(out);
                    UTXO.put(txId, outs);
                }

                if(tx.isCoinBase() == false) {
                    for(TxInput in : tx.getVin()) {
                        String inTxId = Utils.byteArrayToHexString(in.getTxId());
                        spentTXOs.get(inTxId).add(in.getvOut());
                    }
                }
            }

            if(block.getPrevBlockHash().length == 0) break;
        }
        return UTXO;
    }

    public Transaction newUTXOTransaction(Wallet wallet, String to, int amount, UTXOSet utxoSet) throws Exception{
        ArrayList<TxInput> inputs = new ArrayList();
        ArrayList<TxOutput> outputs = new ArrayList();

        byte[] pubkeyHash = Utils.ripemd160(Utils.sha256(wallet.getPublicKey().getEncoded()));
        Pair<Integer, HashMap<String, ArrayList<Integer>>> spendableOutputs = utxoSet.findSpendableOutputs(pubkeyHash, amount);
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
                TxInput input = new TxInput(Utils.hexStringToByteArray(txid), out, wallet.getPublicKey(), null);
                inputs.add(input);
            }
        }

        outputs.add(new TxOutput(amount, to));
        if(acc > amount) {
            outputs.add(new TxOutput(acc-amount, wallet.getAddress()));
        }

        Transaction tx = new Transaction(new byte[]{}, inputs, outputs);
        tx.setId(tx.Hash());
        utxoSet.getBc().signTransaction(tx, wallet.getPrivateKey());
        return tx;
    }

    public Transaction findTransaction(byte[] id) {
        Iterator<Block> itr = iterator();
        while(itr.hasNext()){
            Block block = itr.next();
            for(Transaction tx : block.getTransactions()) {
                if(Arrays.equals(tx.getId(), id)) return tx;
                if(block.getPrevBlockHash().length == 0) break;
            }
        }

        return null;
    }

    public void signTransaction(Transaction tx, PrivateKey privateKey) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, InvalidKeySpecException {
        HashMap<String, Transaction> prevTxs = new HashMap<>();

        for(TxInput vin : tx.getVin()) {
            Transaction prevTx = new Transaction(findTransaction(vin.getTxId()));
            prevTxs.put(Utils.byteArrayToHexString(prevTx.getId()), prevTx);
        }

        tx.sign(privateKey, prevTxs);
    }

    public boolean VerifyTransaction(Transaction tx) throws Exception {
        if(tx.isCoinBase()) return true;

        HashMap<String, Transaction> prevTxs = new HashMap<>();

        for(TxInput vin : tx.getVin()) {
            Transaction prevTx = findTransaction(vin.getTxId());
            if(prevTx == null) return false;
            prevTxs.put(Utils.byteArrayToHexString(prevTx.getId()), prevTx);
        }

        return tx.Verify(prevTxs);

    }

    public Db getDb() {
        return db;
    }
    public boolean validate() {
        return tip != null;
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
