package node.network;

import DB.Db;
import blockchain.*;
import blockchain.transaction.*;
import blockchain.wallet.Wallet;
import blockchain.wallet.Wallets;
import node.event.EventHandler;
import node.event.EventListener;
import utils.Utils;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class Node extends Thread implements EventListener {
    private static int NodeCount = 0;

    private String nodeId;
    private boolean bLoop = true;
    private boolean bMining = false;

    // Wallett
    private Wallets wallets;
    private Wallet wallet;

    // Blockchain
    private static final int BLOCK_MINE_INTERVAL = 0;
    private static final int BLOCK_TX_NUM = 3;

    private Db db;
    private Blockchain bc;
    private LocalTime nextMineTime = LocalTime.now();
    private ConcurrentHashMap<String, Transaction> txPool = new ConcurrentHashMap<>();
    private ConcurrentSkipListSet<String> invBlock = new ConcurrentSkipListSet<>(), invTx = new ConcurrentSkipListSet<>();

    // Network
    private Network network;

    public Node() {
        wallets = new Wallets();
        this.db = new Db();
        //nodeId = Utils.sha256(Float.valueOf(new SecureRandom().nextFloat()).toString().getBytes());
        nodeId = String.format("node %d", NodeCount++);
        this.network = new Network(nodeId);

        EventHandler.addListener(nodeId, this);
    }

    // Wallet
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

    public String getNodeId() {
        return nodeId;
    }
    public Blockchain getBlockChain() { return bc; }
    public Network getNetwork() {
        return network;
    }
    public void connect(Node to) {
        network.connectTo(to.nodeId);
        network.sendAddress(to.nodeId);
        network.sendVersion(to.nodeId, bc.getTip());
    }

    // TEST
    public boolean send(String to, int amount) {
        UTXOSet utxoSet = new UTXOSet(bc);
        Transaction tx;
        try {
            tx = bc.newUTXOTransaction(wallet, to, amount, utxoSet);
        } catch (Exception e) {
            System.out.println(nodeId + ": " + e);
            return false;
        }

        System.out.printf("'%s'가 '%s'에게 %d 전송\n", wallet.getAddress(), to, amount);
        txPool.put(Utils.byteArrayToHexString(tx.getId()), tx);
        for (String _nodeId : network.getConnList())
            network.sendTx(_nodeId, tx);
        return true;
    }

    public void checkBalance() {
        if(wallet == null) return;
        Functions.getBalance(wallet.getAddress(), bc);
    }

    @Override
    public void run() {
        while (bLoop) {
            try { sleep(1L); } catch (InterruptedException ignored) {}

            fetchInvectory();

            if (!bMining) {
                bMining = true;
                new Thread(new Runnable() {
                    public void run() {
                        mineBlock();
                        bMining = false;
                    }
                }).start();
            }

            // 고아 블록의 이전 블록 가져오기
            ConcurrentHashMap<String, Block> orphanBlocks = bc.getOrphanBlock();
            for (Block block : orphanBlocks.values()) {
                String prevBlock = Utils.byteArrayToHexString(block.getPrevBlockHash());
                if (!orphanBlocks.containsKey(prevBlock))
                    invBlock.add(prevBlock);
            }

            // 고아 블록 처리 및 처리된 고아 블록 전파
            ArrayList<byte[]> blocks = bc.addOrphanBlock();
            for (String _nodeId : network.getConnList())
                network.sendInv(_nodeId, Network.TYPE.BLOCK, Utils.bytesConcat(blocks.toArray(new byte[][]{})));
        }

        network.close();
        EventHandler.removeListener(nodeId);
    }
    public void close() { bLoop = false; }

    private void mineBlock() {
        if (txPool.size() < BLOCK_TX_NUM-1 && LocalTime.now().isBefore(nextMineTime)) return; // Tx 부족, 시간 필요

        // Transaction 준비
        ArrayList<Transaction> txList = new ArrayList<>();
        ArrayList<TxInput> usedVin = new ArrayList<>();

        for (Transaction tx : txPool.values()) {
            String key = Utils.byteArrayToHexString(tx.getId());

            // Tx 서명 검증
            if (!bc.verifyTransaction(tx)) {
                txPool.remove(key);
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
                txPool.remove(key);
                continue;
            }

            usedVin.addAll(tx.getVin());

            //＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊//
            txList.add(tx);
        }

        if (txList.size() == 0) return ; // Tx 없음
        txList.add(new Transaction(wallet.getAddress(), ""));

        Block newBlock = bc.mineBlock(txList.toArray(new Transaction[]{}));
        if (newBlock == null) return; // 채굴 실패

        nextMineTime = LocalTime.now().plusSeconds(BLOCK_MINE_INTERVAL);

        System.out.println(nodeId + "이 " + Utils.byteArrayToHexString(newBlock.getHash()) + " 블록을 채굴!!");

        // 블록내 트랜잭션 TxPool 에서 제거
        for (Transaction tx : newBlock.getTransactions())
            txPool.remove(Utils.byteArrayToHexString(tx.getId()));

        // 블록 전파
        for (String _nodeId : network.getConnList())
            network.sendBlock(_nodeId, newBlock);
    }
    private void fetchInvectory() {
        Random random = new Random();
        ArrayList<String> clients = network.getConnList();
        if (clients.size() <= 0) return ;

        synchronized (invBlock) {
            Iterator<String> blockIter = invBlock.iterator();
            if (blockIter.hasNext()) {
                String hash = blockIter.next();

                String nodeId = clients.get(random.nextInt(clients.size()));
                network.sendGetData(nodeId, Network.TYPE.BLOCK, Utils.hexStringToByteArray(hash));
            }
        }

        synchronized (invTx) {
            Iterator<String> txIter = invTx.iterator();
            if (txIter.hasNext()) {
                String hash = txIter.next();

                String nodeId = clients.get(random.nextInt(clients.size()));
                network.sendGetData(nodeId, Network.TYPE.TX, Utils.hexStringToByteArray(hash));
            }
        }
    }

    private void handleAddress(String from) {
        if (!network.getConnList().contains(from))
            network.connectTo(from);
    }
    private void handleBlock(String from, byte[] data) {
        Block block = Utils.toObject(data);

        invBlock.remove(Utils.byteArrayToHexString(block.getHash()));

        if (!bc.addBlock(block)) return;
        System.out.println(nodeId + "에 " + Utils.byteArrayToHexString(block.getHash()) + " 블록이 추가 되었습니다.");

        // 블록내 트랜잭션 pool 에서 제거
        for (Transaction tx : block.getTransactions())
            txPool.remove(Utils.byteArrayToHexString(tx.getId()));

        // 전파
        for (String _nodeId : network.getConnList()) {
            if (nodeId.equals(from)) continue;
            network.sendInv(_nodeId, Network.TYPE.BLOCK, block.getHash());
        }
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
                        invBlock.add(hash);
                }
                break;

            case Network.TYPE.TX:
                for (int i = 0; i < items.length; i += 32) {
                    byte[] item = Arrays.copyOfRange(items, i, i+32);

                    String hash = Utils.byteArrayToHexString(item);

                    if (!invTx.contains(hash) && !txPool.containsKey(hash) && bc.findTransaction(item) == null) {
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
                else
                    invBlock.add(Utils.byteArrayToHexString(hash));
                break;
            case Network.TYPE.TX:
                String id = Utils.byteArrayToHexString(hash);
                Transaction tx = null;

                if (txPool.containsKey(id))
                    tx = txPool.get(id);
                else
                    tx = bc.findTransaction(hash);

                if (tx != null)
                    network.sendTx(from, tx);
                else
                    invTx.add(Utils.byteArrayToHexString(hash));
        }
    }
    private void handleTx(String from, byte[] data) {
        Transaction tx = Utils.toObject(data);
        String id = Utils.byteArrayToHexString(tx.getId());

        invTx.remove(id);

        if (!txPool.containsKey(id)) {
            txPool.put(id, tx);

            // 전파
            for (String _nodeId : network.getConnList()){
                if (nodeId.equals(from)) continue;
                network.sendInv(_nodeId, Network.TYPE.TX, tx.getId());
            }
        }
    }
    private void handleVersion(String from, byte[] data) {
        if (Arrays.equals(bc.getTip(), data)) return ; // 같은 체인 유지

        if (bc.findBlock(data) != null) // 나한테 있는 체인
            network.sendVersion(from, bc.getTip());
        else // 나한테 없는 체인
            network.sendGetBlocks(from);
    }

    private void handleConnection(String from, byte[] buff) {
        int TYPE = buff[0];
        byte[] data = Arrays.copyOfRange(buff, 1, buff.length);

        switch (TYPE) {
            case Network.TYPE.BLOCK:    handleBlock(from, data);      break;
            case Network.TYPE.INV:      handleInv(from, data);        break;
            case Network.TYPE.GETBLOCK: handleGetBlocks(from);        break;
            case Network.TYPE.GETDATA:  handleGetData(from, data);    break;
            case Network.TYPE.TX:       handleTx(from, data);         break;
            case Network.TYPE.VERSION:  handleVersion(from, data);    break;
            case Network.TYPE.ADDRESS:  handleAddress(from);          break;
        }
    }
    @Override
    public void onEvent(String from, byte[] data) {
        handleConnection(from, data);
    }
}
