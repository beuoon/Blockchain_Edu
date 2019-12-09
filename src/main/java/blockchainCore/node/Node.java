package blockchainCore.node;

import blockchainCore.DB.Db;
import blockchainCore.blockchain.*;
import blockchainCore.blockchain.event.SignalHandler;
import blockchainCore.blockchain.event.SignalType;
import blockchainCore.blockchain.transaction.*;
import blockchainCore.blockchain.wallet.Wallet;
import blockchainCore.blockchain.wallet.Wallets;
import blockchainCore.node.network.event.NetworkHandler;
import blockchainCore.node.network.event.NetworkListener;
import blockchainCore.node.network.Network;
import blockchainCore.utils.Utils;
import org.bitcoinj.core.Base58;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class Node extends Thread implements NetworkListener {
    private static int NodeCount = 0;

    private String nodeId;
    private boolean bLoop = true;
    private boolean bStopMine = false, bMining = false;
    private final Object MINE_MUTEX = new Object();

    // Wallett
    private Wallets wallets;
    private Wallet wallet;

    // Blockchain
    private static final int BLOCK_MINE_INTERVAL = 0;
    private static final int BLOCK_TX_NUM = 3;

    private Db db;
    private Blockchain bc = null;
    private LocalTime nextMineTime = LocalTime.now();

    private Thread mineThread = null;
    private ConcurrentHashMap<String, Transaction> txPool = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> invBlock = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> invTx = new ConcurrentHashMap<>();
    private final ConcurrentSkipListSet<String> requestBlock = new ConcurrentSkipListSet<>();
    private final ConcurrentSkipListSet<String> requestTx = new ConcurrentSkipListSet<>();

    // Network
    private Network network;

    public Node() {
        wallets = new Wallets();
        useWallet(createWallet());
        this.db = new Db();
        //nodeId = Utils.sha256(Float.valueOf(new SecureRandom().nextFloat()).toString().getBytes());
        nodeId = Integer.toString(NodeCount++);
        this.network = new Network(nodeId);

        NetworkHandler.addListener(nodeId, this);
    }

    // Wallet
    public String createWallet() {
        String address = wallets.createWallet();
        System.out.printf("wallet '%s' id created\n", address);
        return address;
    }
    public void useWallet(String address) { wallet = wallets.getWallet(address); }
    public ArrayList<String> getAddresses() { return wallets.getAddresses(); }
    public Wallet getWallet(String address) { return wallets.getWallet(address); }
    public ArrayList<Wallet> getWallets() {
        ArrayList<Wallet> _wallets = new ArrayList<>();
        for(String address : getAddresses()) {
            _wallets.add(wallets.getWallet(address));
;        }

        return _wallets;
    }

    // Genesis Block
    public void createGenesisBlock() {
        this.bc = new Blockchain(wallet.getAddress(), this.db);
        SignalHandler.callEvent(SignalType.ADD_BLOCK, nodeId, bc.getBlocks().get(0));
    }
    public void createNullBlockchain() { this.bc = new Blockchain(this.db); }

    public String getNodeId() {
        return nodeId;
    }
    public Blockchain getBlockChain() { return bc; }
    public Network getNetwork() {
        return network;
    }
    public void connect(String to) {
        network.connectTo(to);
        network.sendAddress(to);
        network.sendVersion(to, bc.getTip());
    }
    public void disconnection(String _nodeId) {
        network.disconnectionTo(_nodeId);
    }

    // TEST
    public boolean send(String from, String to, int amount) {
        if (amount <= 0) return false;

        UTXOSet utxoSet = new UTXOSet(bc);
        Transaction tx;
        try {
            tx = bc.newUTXOTransaction(wallets.getWallet(from), to, amount, utxoSet);
        } catch (Exception e) {
            System.out.println(nodeId + ": " + e);
            return false;
        }

        System.out.printf("'%s'가 '%s'에게 %d 전송\n", from, to, amount);
        txPool.put(Utils.toHexString(tx.getId()), tx);
        for (String _nodeId : network.getConnList())
            network.sendInv(_nodeId, Network.TYPE.TX, tx.getId());
        return true;
    }

    public HashMap<String, Integer> getBalances() {
        HashMap<String, Integer> balances = new HashMap<>();
        UTXOSet utxoSet = new UTXOSet(bc);

        for (String address : wallets.getAddresses()) {
            byte[] pubkeyHash = Base58.decode(address);
            pubkeyHash = Arrays.copyOfRange(pubkeyHash, 1, pubkeyHash.length - 4);
            ArrayList<TxOutput> UTOXs = utxoSet.findUTXO(pubkeyHash);

            int balance = 0;
            for (TxOutput out : UTOXs) {
                balance += out.getValue();
            }

            balances.put(address, balance);
        }

        return balances;
    }

    @Override
    public void run() {
        while (bLoop) {
            try {
                sleep(100L);
            } catch (InterruptedException ignored) {
            }

            fetchInventory();

            synchronized (MINE_MUTEX) {
                if (!bStopMine && !bMining) {
                    bMining = true;
                    mineThread = new Thread(() -> {
                        mineBlock();

                        synchronized (MINE_MUTEX) {
                            bMining = false;
                        }
                    });
                    mineThread.start();
                }
            }

            // 고아 블록의 이전 블록 가져오기
            ConcurrentHashMap<String, Block> orphanBlocks = bc.getOrphanBlock();
            for (Block block : orphanBlocks.values()) {
                String prevBlock = Utils.toHexString(block.getPrevBlockHash());
                if (!orphanBlocks.containsKey(prevBlock) && !invBlock.containsKey(prevBlock)) {
                    int number = new Random().nextInt(network.getConnList().size());
                    String targetNodeId = network.getConnList().get(number);
                    invBlock.put(prevBlock, targetNodeId);
                }
            }

            // 고아 블록 처리 및 처리된 고아 블록 전파
            ArrayList<byte[]> blocks = bc.addOrphanBlock();
            for (String _nodeId : network.getConnList())
                network.sendInv(_nodeId, Network.TYPE.BLOCK, Utils.bytesConcat(blocks.toArray(new byte[][]{})));
        }

        network.close();
        NetworkHandler.removeListener(nodeId);
        if (bMining && mineThread != null) {
            mineThread.interrupt();
            try { mineThread.join(); } catch (InterruptedException ignored) { }
        }
    }
    public void close() { bLoop = false; }

    private void mineBlock() {
        if (txPool.size() < BLOCK_TX_NUM-1 && LocalTime.now().isBefore(nextMineTime)) return; // Tx 부족, 시간 필요

        // Transaction 준비
        ArrayList<Transaction> txList = new ArrayList<>();
        ArrayList<TxInput> usedVin = new ArrayList<>();

        for (Transaction tx : txPool.values()) {
            String key = Utils.toHexString(tx.getId());

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

        System.out.println(nodeId + "이 " + Utils.toHexString(newBlock.getHash()) + " 블록을 채굴!!");

        SignalHandler.callEvent(SignalType.ADD_BLOCK, nodeId, newBlock);

        // 블록내 트랜잭션 TxPool 에서 제거
        for (Transaction tx : newBlock.getTransactions())
            txPool.remove(Utils.toHexString(tx.getId()));

        // 블록 전파
        for (String _nodeId : network.getConnList())
            network.sendInv(_nodeId, Network.TYPE.BLOCK, newBlock.getHash());
    }
    private synchronized void fetchInventory() {
        for (String hash : invBlock.keySet()) {
            String targetNodeId = invBlock.get(hash);
            requestBlock.add(hash);
            invBlock.remove(hash);
            network.sendGetData(targetNodeId, Network.TYPE.BLOCK, Utils.hexToBytes(hash));
        }

        for (String hash : invTx.keySet()) {
            String targetNodeId = invTx.get(hash);
            requestTx.add(hash);
            invTx.remove(hash);
            network.sendGetData(targetNodeId, Network.TYPE.TX, Utils.hexToBytes(hash));
        }
    }
    public void switchMine() {
        synchronized (MINE_MUTEX) {
            bStopMine = !bStopMine;
            if (bStopMine && bMining)
                mineThread.interrupt();
        }
    }

    private void handleAddress(String from) {
        if (!network.getConnList().contains(from))
            network.connectTo(from);
    }
    private void handleBlock(String from, byte[] data) {
        Block block = Utils.toObject(data);
        String blockHash = Utils.toHexString(block.getHash());

        SignalHandler.callEvent(SignalType.HANDLE_BLOCK, from, nodeId);

        try {
            if (!bc.addBlock(block)) return;
        } finally {
            requestBlock.remove(blockHash);
        }

        SignalHandler.callEvent(SignalType.ADD_BLOCK, nodeId, block);
        System.out.println(nodeId + "에 " + blockHash + " 블록이 추가 되었습니다.");


        // 블록내 트랜잭션 pool 에서 제거
        for (Transaction tx : block.getTransactions())
            txPool.remove(Utils.toHexString(tx.getId()));

        // 전파
        for (String _nodeId : network.getConnList()) {
            if (_nodeId.equals(from)) continue;
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

                    String hash = Utils.toHexString(item);

                    if (!invBlock.contains(hash) && !requestBlock.contains(hash) && bc.findBlock(item) == null)
                        invBlock.put(hash, from);
                }
                break;

            case Network.TYPE.TX:
                for (int i = 0; i < items.length; i += 32) {
                    byte[] item = Arrays.copyOfRange(items, i, i+32);

                    String hash = Utils.toHexString(item);

                    if (!invTx.contains(hash) && !requestTx.contains(hash) &&
                            !txPool.containsKey(hash) && bc.findTransaction(item) == null)
                        invTx.put(hash, from);
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
                String id = Utils.toHexString(hash);
                Transaction tx;

                if (txPool.containsKey(id))
                    tx = txPool.get(id);
                else
                    tx = bc.findTransaction(hash);

                if (tx != null)
                    network.sendTx(from, tx);
                break;
        }
    }
    private void handleTx(String from, byte[] data) {
        Transaction tx = Utils.toObject(data);
        String id = Utils.toHexString(tx.getId());

        SignalHandler.callEvent(SignalType.HANDLE_TX, from, nodeId);

        if (!txPool.containsKey(id)) {
            txPool.put(id, tx);

            // 전파
            for (String _nodeId : network.getConnList()){
                if (_nodeId.equals(from)) continue;
                network.sendInv(_nodeId, Network.TYPE.TX, tx.getId());
            }
        }
        requestTx.remove(id);
    }
    private void handleVersion(String from, byte[] data) {
        if (Arrays.equals(bc.getTip(), data)) return ; // 같은 체인 유지

        if (bc.findBlock(data) != null || data.length == 0) // 나한테 있는 체인
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

    public ArrayList<Transaction> getTxsFromTxPool() {
        return new ArrayList<>(txPool.values());
    }

    @Override
    public void Listen(String from, byte[] data) {
        handleConnection(from, data);
    }
}
