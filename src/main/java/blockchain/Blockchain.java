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

import static blockchain.consensus.ProofOfWork.powStopFlag;

public class Blockchain {
    private Db db;
   public byte[] tip;
    int lastHeight;

    Mempool<String, Block> mempool = new Mempool<>();

    final String genesisCoinbaseData = "The Times 03/Jan/2009 Chancellor on brink of second bailout for banks";

    public Blockchain(String address, Db db) {
        Transaction coinbaseTx = new Transaction(address, genesisCoinbaseData);
        Block genesisBlock = new Block(coinbaseTx); // create genesis block
        Bucket b = db.getBucket("blocks");
        ProofOfWork.mine(genesisBlock);

        b.put(Utils.byteArrayToHexString(genesisBlock.getHash()), Utils.toBytes(genesisBlock)); // put genesis block to blockchain
        b.put("l", genesisBlock.getHash());

        this.db = db;
        this.tip = genesisBlock.getHash();
        this.lastHeight = -1;
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
        this.tip = null;
        this.lastHeight = -1;
    }

    public Block mineBlock(Transaction[] transactions) {
        Bucket bucket = db.getBucket("blocks");
        byte[] lastHash = bucket.get("l");
        Block lastBlock = Utils.toObject(bucket.get(Utils.byteArrayToHexString(lastHash)));

        Block newBlock = new Block(transactions, lastHash, lastBlock.getHeight()+1);
        if(!ProofOfWork.mine(newBlock)) return null;

        bucket.put(Utils.byteArrayToHexString(newBlock.getHash()), Utils.toBytes(newBlock));
        bucket.put("l", newBlock.getHash());

        ArrayList<byte[]> blockList = new ArrayList<>();
        blockList.add(newBlock.getHash());
        bucket.put("h" + newBlock.getHeight(), Utils.toBytes(blockList));

        this.tip = newBlock.getHash();
        this.lastHeight = newBlock.getHeight();


        UTXOSet utxoSet = new UTXOSet(this);
        utxoSet.update(newBlock);

        return newBlock;
    }

    public boolean addBlock(Block block) {
        Bucket bucket = db.getBucket("blocks");
       //블록이 PoW를 만족하는가?
        ProofOfWork.validate(block);

        // 트랜잭션 검증
        for (Transaction tx : block.getTransactions()) {
                if (!verifyTransaction(tx)) return false;
        }

        // TODO: 블록 검증
        // 포크

        //지금 온 블록이 최신 블록 보다 작거나 같으면?
        if(block.getHeight() <= lastHeight) {
            if(bucket.get("h"+ (block.getHeight()-1)) == null) {
                System.out.println((block.getHeight()-1) +" "+ lastHeight );
            }

            ArrayList<byte[]> hashList = Utils.toObject(bucket.get("h"+ (block.getHeight()-1)));
            for(byte[] hash : hashList) {
                Block b = Utils.toObject(bucket.get(Utils.byteArrayToHexString(hash)));
                if(Arrays.equals(b.getHash(), block.getPrevBlockHash())) {
                    ArrayList<byte[]> blockList = new ArrayList<>();

                    if(bucket.get("h" + block.getHeight()) != null)
                        blockList = Utils.toObject(bucket.get("h" + block.getHeight()));
                    blockList.add(block.getHash());

                    bucket.put("h" + block.getHeight(), Utils.toBytes(blockList));
                    bucket.put(Utils.byteArrayToHexString(block.getHash()), Utils.toBytes(block));
                    return false;
                }
            }
            return false;
        }

        if(block.getHeight() > lastHeight+1) {
            mempool.put(Utils.byteArrayToHexString(block.getHash()), block);
            return false;
        }

        //이 블록이 내 최신높이에 있는 블록의 다음블록이 맞느냐?
        if(block.getHeight() != 0) {
            boolean flag = false;
            ArrayList<byte[]> lasthashList = Utils.toObject(bucket.get("h" + (block.getHeight() - 1)));
            for (byte[] lasthash : lasthashList) {
                Block lastBlock = Utils.toObject(bucket.get(Utils.byteArrayToHexString(lasthash)));
                if (Arrays.equals(block.getPrevBlockHash(), lastBlock.getHash())) {
                    flag = true;
                }
            }
            if (!flag) return false;
            //PoW 중지.
            powStopFlag = true;
        }


        bucket.put(Utils.byteArrayToHexString(block.getHash()), Utils.toBytes(block));
        bucket.put("l", block.getHash());

        ArrayList<byte[]> blockList = new ArrayList<>();
        if(bucket.get("h" + block.getHeight()) != null)
            blockList = Utils.toObject(bucket.get("h" + block.getHeight()));
        blockList.add(block.getHash());

        bucket.put("h" + block.getHeight(), Utils.toBytes(blockList));
        tip = block.getHash();
        lastHeight = block.getHeight();

        Iterator<Block> itr = mempool.values().iterator();
        while(itr.hasNext()){
            Block b = itr.next();
            if(b.getHeight() == lastHeight+1) {
                addBlock(b);
                break;
            }
        }

        UTXOSet utxoSet = new UTXOSet(this);
        utxoSet.update(block);

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

    public Block findBlock(byte[] hash) {
        Bucket bucket = db.getBucket("blocks");
        byte[] data = bucket.get(Utils.byteArrayToHexString(hash));

        if (data == null) return null;
        return Utils.toObject(data);
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

    public boolean validateTransaction(Transaction tx) {
        if(tx.isCoinBase()) return true;

        for(TxInput vin : tx.getVin()) {
            Transaction prevTx = findTransaction(vin.getTxId());
            if(prevTx == null) return false;
        }

        return true;
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
            byte[] b = db.getBucket("blocks").get(Utils.byteArrayToHexString(currentHash));
            if( b == null) return false;
            return true;
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
