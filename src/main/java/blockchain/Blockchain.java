package blockchain;

import DB.Bucket;
import DB.Db;
import blockchain.consensus.ProofOfWork;
import blockchain.transaction.*;
import blockchain.wallet.Wallet;
import node.Mempool;
import utils.Pair;
import utils.Utils;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

public class Blockchain {
    private Db db;
   public byte[] tip;
    int lastHeight;

    private ProofOfWork pow = new ProofOfWork();
    private Mempool<String, Block> orphanBlocks = new Mempool<>();
    private final Object mutexAddBlock = new Object();

    final String genesisCoinbaseData = "The Times 03/Jan/2009 Chancellor on brink of second bailout for banks";

    public Blockchain(String address, Db db) {
        Transaction coinbaseTx = new Transaction(address, genesisCoinbaseData);
        Block genesisBlock = new Block(coinbaseTx); // create genesis block
        Bucket b = db.getBucket("blocks");
        pow.mine(genesisBlock);

        b.put(Utils.byteArrayToHexString(genesisBlock.getHash()), Utils.toBytes(genesisBlock)); // put genesis block to blockchain
        b.put("l", genesisBlock.getHash());

        this.db = db;
        this.tip = genesisBlock.getHash();
        this.lastHeight = 0;
        ArrayList<byte[]> blockList = new ArrayList<>();

        if(b.get("h" + genesisBlock.getHeight()) != null)
            blockList = Utils.toObject(b.get("h" + genesisBlock.getHeight()));
        blockList.add(genesisBlock.getHash());

        b.put("h" + genesisBlock.getHeight(), Utils.toBytes(blockList));

        UTXOSet utxoSet = new UTXOSet(this);
        utxoSet.reIndex();
    }
    public Blockchain(Db db) {
        this.db = db;
        this.tip = new byte[]{};
        this.lastHeight = -1;
    }

    public Block mineBlock(Transaction[] transactions) {
        Bucket bucket = db.getBucket("blocks");
        byte[] lastHash = bucket.get("l");
        Block lastBlock = Utils.toObject(bucket.get(Utils.byteArrayToHexString(lastHash)));

        Block newBlock = new Block(transactions, lastHash, lastBlock.getHeight()+1);
        if(!pow.mine(newBlock)) return null;

        if (!addBlock(newBlock))
            return null;

        return newBlock;
    }
    public boolean addBlock(Block block) {
        Bucket bucket = db.getBucket("blocks");

        synchronized (mutexAddBlock) {
            if (bucket.get(Utils.byteArrayToHexString(block.getHash())) != null) return false;

            // 이전 블록이 있는지 검사
            if (block.getHeight() > 0 && bucket.get(Utils.byteArrayToHexString(block.getPrevBlockHash())) == null)  { // 고아 블록
                orphanBlocks.put(Utils.byteArrayToHexString(block.getHash()), block);
                return false;
            }

            // PoW 검증
            if (!ProofOfWork.Validate(block)) return false;

            // Tx 서명 및 UTXO 검증
            if (Arrays.equals(block.getPrevBlockHash(), tip)) { // 메인 체인 블록
                if (!validTransaction(block)) return false;
            }
            else { // 서브 체인 블록
                HashMap<String, TxOutputs> utxoset = findUTXO(block.getPrevBlockHash());
                if (!validTransaction(block, utxoset)) return false;
            }

            // 블록 추가
            ArrayList<byte[]> blockList = new ArrayList<>();
            if (bucket.get("h" + block.getHeight()) != null)
                blockList = Utils.toObject(bucket.get("h" + block.getHeight()));
            blockList.add(block.getHash());
            bucket.put("h" + block.getHeight(), Utils.toBytes(blockList));

            bucket.put(Utils.byteArrayToHexString(block.getHash()), Utils.toBytes(block));

            if (block.getHeight() <= lastHeight) return true;

            // 체인 갱신
            bucket.put("l", block.getHash());

            byte[] prevTip = tip;
            tip = block.getHash();
            lastHeight = block.getHeight();
            pow.renewLastHeight(lastHeight);

            if (!Arrays.equals(prevTip, block.getPrevBlockHash())) { // 체인 변경
                UTXOSet utxoSet = new UTXOSet(this);
                utxoSet.reIndex();
            }
            else if (block.getHeight() > 0) { // 체인 유지
                UTXOSet utxoSet = new UTXOSet(this);
                utxoSet.update(block);
            }
        }

        return true;
    }

    public Mempool<String, Block> getOrphanBlock() { return orphanBlocks; }
    public void addOrphanBlock() {
        Bucket bucket = db.getBucket("blocks");

        for (Block b : orphanBlocks.values()) {
            if (bucket.get(Utils.byteArrayToHexString(b.getPrevBlockHash())) != null) {
                if (addBlock(b))
                    orphanBlocks.remove(Utils.byteArrayToHexString(b.getHash()));
            }
        }
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

    public HashMap<String, TxOutputs> findUTXO() { return findUTXO(tip); }
    public HashMap<String, TxOutputs> findUTXO(byte[] tip) {
        HashMap<String, TxOutputs> UTXO = new HashMap<>();
        HashMap<String, ArrayList<Integer>> spentTXOs = new HashMap<>();

        Iterator<Block> itr = iterator(tip);
        while(itr.hasNext()){
            Block block = itr.next();

            for(Transaction tx : block.getTransactions()) {
                String txId = Utils.byteArrayToHexString(tx.getId());

                TxOutputs outs = new TxOutputs();
                for(int outIdx = 0; outIdx < tx.getVout().size(); outIdx++){
                    TxOutput out = tx.getVout().get(outIdx);

                    if(spentTXOs.containsKey(txId) && spentTXOs.get(txId).contains(outIdx))
                        continue;

                    outs.getOutputs().put(outIdx, out);
                }
                if (outs.getOutputs().size() > 0)
                    UTXO.put(txId, outs);

                if(!tx.isCoinBase()) {
                    for(TxInput in : tx.getVin()) {
                        String inTxId = Utils.byteArrayToHexString(in.getTxId());

                        if (!spentTXOs.containsKey(inTxId))
                            spentTXOs.put(inTxId, new ArrayList<>());
                        spentTXOs.get(inTxId).add(in.getvOut());
                    }
                }
            }
        }

        return UTXO;
    }

    private boolean validTransaction(Block block){ return validTransaction(block, findUTXO()); }
    private boolean validTransaction(Block block, HashMap<String, TxOutputs> utxoset) {
        for (Transaction tx : block.getTransactions()) {
            if (tx.isCoinBase()) continue;

            if (!verifyTransaction(tx)) return false;

            for (TxInput vin : tx.getVin()) {
                String txId = Utils.byteArrayToHexString(vin.getTxId());
                if (!utxoset.containsKey(txId)) return false;
                if (!utxoset.get(txId).getOutputs().containsKey(vin.getvOut())) return false;
            }
        }
        return true;
    }

    public Block findBlock(byte[] hash) {
        Bucket bucket = db.getBucket("blocks");
        byte[] data = bucket.get(Utils.byteArrayToHexString(hash));

        Block block = null;
        if (data == null)
            block = orphanBlocks.get(Utils.byteArrayToHexString(hash));
        else
            block = Utils.toObject(data);
        return block;
    }
    public Transaction findTransaction(byte[] id) {
        Bucket bucket = db.getBucket("blocks");

        for (int i = lastHeight; i >= 0; i--) {
            ArrayList<byte[]> blocks = Utils.toObject(bucket.get("h" + i));
            for (byte[] blockHash : blocks) {
                Block block = Utils.toObject(bucket.get(Utils.byteArrayToHexString(blockHash)));

                for (Transaction tx : block.getTransactions())
                    if(Arrays.equals(tx.getId(), id)) return tx;
            }
        }

        return null;
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
    public void signTransaction(Transaction tx, PrivateKey privateKey) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        HashMap<String, Transaction> prevTxs = new HashMap<>();

        for(TxInput vin : tx.getVin()) {
            Transaction prevTx = new Transaction(findTransaction(vin.getTxId()));
            prevTxs.put(Utils.byteArrayToHexString(prevTx.getId()), prevTx);
        }

        tx.sign(privateKey, prevTxs);
    }
    public boolean verifyTransaction(Transaction tx) {
        if(tx.isCoinBase()) return true;

        HashMap<String, Transaction> prevTxs = new HashMap<>();

        for(TxInput vin : tx.getVin()) {
            Transaction prevTx = findTransaction(vin.getTxId());
            if(prevTx == null) return false;
            prevTxs.put(Utils.byteArrayToHexString(prevTx.getId()), prevTx);
        }

        return tx.Verify(prevTxs);
    }

    public Db getDb() { return db; }

    public Iterator<Block> iterator() { return iterator(tip); }
    public Iterator<Block> iterator(byte[] tip) { return new BcItr(db, tip); }

    private class BcItr implements Iterator<Block> {
        private byte[] currentHash;
        private Db db;

        public BcItr(Db db, byte[] tip) {
            this.currentHash = tip;
            this.db = db;
        }

        public boolean hasNext() {
            return db.getBucket("blocks").get(Utils.byteArrayToHexString(currentHash)) != null;
        }

        public void remove() {
            System.out.println("you can not remove it!");
        }

        public Block next() {
            Block block = new Block(db.getBucket("blocks").get(Utils.byteArrayToHexString(currentHash)));
            currentHash = block.getPrevBlockHash();

            return block;
        }
    }

}
