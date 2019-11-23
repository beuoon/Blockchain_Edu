package node.network;

import DB.Db;
import blockchain.*;
import blockchain.transaction.*;
import blockchain.wallet.Wallet;
import blockchain.wallet.Wallets;
import node.Mempool;
import node.event.EventHandler;
import node.event.EventListener;
import utils.Utils;

import java.util.*;

public class Node extends Thread implements EventListener {
    public static int NodeCount = 0;

    public String getNodeId() {
        return nodeId;
    }

    private String nodeId;
    private boolean bLoop = true;

    // WalletT
    private Wallets wallets;
    private Wallet wallet;

    // Blockchain
    private Db db;
    private Blockchain bc;
    private Mempool<String, Transaction> mempool = new Mempool<>();
    private HashSet<String> invBlock = new HashSet<>(), invTx = new HashSet<>();

    // Network
    private Network network;

    public Node() {
        wallets = new Wallets();
        this.db = new Db();
        //nodeId = Utils.sha256(Float.valueOf(new SecureRandom().nextFloat()).toString().getBytes());
        nodeId = String.format("node %d", NodeCount++);
        this.network = new Network(this);

        EventHandler.addListener(nodeId, this);
    }

    ///wallet
    public void createWallet() {
        String address = wallets.createWallet();
        wallet = wallets.getWallet(address);
        System.out.printf("wallet '%s' id created\n", address);
    }

    public Wallet getWallet() {
        return wallet;
    }

    public void useWallet(String address) {
        wallet = wallets.getWallet(address);
    }

    public ArrayList<String> getAddresses() { return wallets.getAddresses(); }

    // Genesis Block
    public void createGenesisBlock(String address) { this.bc = new Blockchain(address, this.db); }

    //임시 메소드임!
    public void createNullBlockchain() {
        this.bc = new Blockchain(this.db);
    }

    public Block getGenesisBlock() {
        Iterator<Block> itr = bc.iterator();
        Block block = null;
        while(itr.hasNext()) block = itr.next();
        return block;
    }

    public void setGenesisBlock(Block block) {
        bc.addBlock(block);
        UTXOSet utxoSet = new UTXOSet(bc);
        utxoSet.reIndex();
    }

    public Network getNetwork() {
        return network;
    }

    // TEST
    public void send(String to, int amount) {
        UTXOSet utxoSet = new UTXOSet(bc);
        Transaction tx;
        try {
            tx = bc.newUTXOTransaction(wallet, to, amount, utxoSet);
        } catch (Exception e) {
            System.out.println(nodeId + ": " + e);
            return ;
        }

        System.out.printf("'%s'가 '%s'에게 %d 전송\n", wallet.getAddress(), to, amount);
        mempool.put(Utils.byteArrayToHexString(tx.getId()), tx);
        for (String _nodeId : network.getConnList())
            network.sendTx(_nodeId, tx);
    }

    public void checkBalance() {
        if(wallet == null) return;
        Functions.getBalance(wallet.getAddress(), bc);
    }

    @Override
    public void run() {
        while (bLoop) {
            try { sleep(100L); } catch (InterruptedException ignored) {}

            getInv();
            mineBlock();
        }
    }

    public void close() {
        bLoop = false;
    }

    private void mineBlock() {
        // Transaction 준비
        Transaction[] txs = null;

        if (mempool.size() >= 1) {
            ArrayList<Transaction> txList = new ArrayList<>();
            Iterator<Transaction> iter = mempool.values().iterator();

            ArrayList<TxInput> usedVin = new ArrayList<>();

            while (iter.hasNext()) {
                Transaction tx = iter.next();
                String key = Utils.byteArrayToHexString(tx.getId());

                // tx 검증
                if (!bc.verifyTransaction(tx)) { // 고아 거래 또는 잘못된 거래
                    mempool.remove(key);
                    continue;
                }

                //＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊//
                // vin 확인
                UTXOSet utxoSet = new UTXOSet(bc);
                boolean canUse = true;
                for (TxInput vin : tx.getVin()) {
                    if (usedVin.contains(vin) || !utxoSet.validVin(vin)) {
                        canUse = false;
                        break;
                    }
                }
                if (!canUse) { // 이미 사용한 vin
                    mempool.remove(key);
                    continue;
                }

                usedVin.addAll(tx.getVin());

                //＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊//
                txList.add(tx);
            }

            if (txList.size() >= 1) {
                txList.add(new Transaction(wallet.getAddress(), ""));
                txs = txList.toArray(new Transaction[]{});
            }
        }

        if (txs == null) return; // 채굴 안함


        Block newBlock = bc.mineBlock(txs);
        if (newBlock == null)
            return; // 채굴 실패

        System.out.println(nodeId + "이 " + Utils.byteArrayToHexString(newBlock.getHash()) + " 블록을 채굴!!");

        // 블록내 트랜잭션 pool 에서 제거
        for (Transaction tx : newBlock.getTransactions())
            mempool.remove(Utils.byteArrayToHexString(tx.getId()));

        // 블록 전파
        for (String _nodeId : network.getConnList())
            network.sendInv(_nodeId, Network.TYPE.BLOCK, newBlock.getHash());
    }

    private void handleBlock(byte[] data) {
        Block block = Utils.toObject(data);

        if( db.getBucket("blocks").get(Utils.byteArrayToHexString(block.getHash())) != null )
            return;

        invBlock.remove(Utils.byteArrayToHexString(block.getHash()));

        if (block.getHeight() == 0) {

            // UTXO reindex
            UTXOSet utxoSet = new UTXOSet(bc);
            utxoSet.reIndex();
        }
        else {
            if (!bc.addBlock(block))
                return;
            System.out.println(nodeId + "에 " + Utils.byteArrayToHexString(block.getHash()) + " 블록이 추가 되었습니다.");
        }

        // 블록내 트랜잭션 pool 에서 제거
        for (Transaction tx : block.getTransactions())
            mempool.remove(Utils.byteArrayToHexString(tx.getId()));
    }

    private void handleInv(String from, byte[] data) {
        int TYPE = data[0];
        byte[] items = Arrays.copyOfRange(data, 1, data.length);

        switch(TYPE){
            case Network.TYPE.BLOCK:
                for (int i = 0; i < items.length; i += 32) {
                    byte[] item = Arrays.copyOfRange(items, i, i+32);

                    String hash = Utils.byteArrayToHexString(item);

                    if (!invBlock.contains(hash) && bc.findBlock(item) == null)
                        network.sendGetData(from, Network.TYPE.BLOCK, item); // invBlock.add(hash);
                }
                break;

            case Network.TYPE.TX:
                for (int i = 0; i < items.length; i += 32) {
                    byte[] item = Arrays.copyOfRange(items, i, i+32);

                    String hash = Utils.byteArrayToHexString(item);

                    if (!invTx.contains(hash) && !mempool.containsKey(hash) && bc.findTransaction(item) == null) {
                        invTx.add(hash);
                        network.sendGetData(from, Network.TYPE.TX, item);
                    }
                }
                break;
        }
    }
    private void handleGetBlocks(String from) {
        Iterator<Block> iter = bc.iterator();
        ArrayList<byte[]> blockHashes = new ArrayList<>();

        while (iter.hasNext()) {
            Block block = iter.next();
            blockHashes.add(block.getHash());
        }

        byte[] data = Utils.bytesConcat(blockHashes.toArray(new byte[][]{}));

        network.sendInv(from, Network.TYPE.BLOCK, data);
    }
    private void handleGetData(String from, byte[] data) {
        int TYPE = data[0];
        byte[] hash = Arrays.copyOfRange(data, 1, data.length);

        switch (TYPE) {
            case Network.TYPE.BLOCK:
                Block block = bc.findBlock(hash);

                if (block != null)
                    network.sendBlock(from, block);
                break;
            case Network.TYPE.TX:
                String id = Utils.byteArrayToHexString(hash);
                Transaction tx = null;

                if (mempool.containsKey(id))
                    tx = mempool.get(id);
                else
                    tx = bc.findTransaction(hash);

                if (tx != null)
                    network.sendTx(from, tx);
        }
    }
    private void handleTx(String from, byte[] data) {
        Transaction tx = Utils.toObject(data);
        String id = Utils.byteArrayToHexString(tx.getId());

        invTx.remove(id);

        if (!mempool.containsKey(id)) {
            mempool.put(id, tx);

            // 전파
            for (String _nodeId : network.getConnList()){
                if (nodeId.equals(from)) continue;
                network.sendInv(_nodeId, Network.TYPE.TX, tx.getId());
            }
        }
    }
    private void handleVersion(byte[] data) {
    }

    private void handleConnection(String from, byte[] buff) {
        int TYPE = buff[0];
        byte[] data = Arrays.copyOfRange(buff, 1, buff.length);

        switch (TYPE) {
            case Network.TYPE.BLOCK:    handleBlock(data);            break;
            case Network.TYPE.INV:      handleInv(from, data);        break;
            case Network.TYPE.GETBLOCK: handleGetBlocks(from);        break;
            case Network.TYPE.GETDATA:  handleGetData(from, data);    break;
            case Network.TYPE.TX:       handleTx(from, data);         break;
            case Network.TYPE.VERSION:  handleVersion(data);          break;
        }
    }

    private void getInv() {
        Random random = new Random();
        ArrayList<String> clients = network.getConnList();
        if (clients.size() <= 0) return ;

        Iterator<String> blockIter = invBlock.iterator();
        if (blockIter.hasNext()) {
            String hash = blockIter.next();

            String nodeId = clients.get(random.nextInt(clients.size()));
            network.sendGetData(nodeId, Network.TYPE.BLOCK, Utils.hexStringToByteArray(hash));
            // invBlock.remove(hash);
        }

        Iterator<String> txIter = invTx.iterator();
        if (txIter.hasNext()) {
            String hash = txIter.next();

            String nodeId = clients.get(random.nextInt(clients.size()));
            network.sendGetData(nodeId, Network.TYPE.TX, Utils.hexStringToByteArray(hash));
            // invTx.remove(hash);
        }
    }

    @Override
    public void onEvent(String from, byte[] data) {
        handleConnection(from, data);
    }
}
