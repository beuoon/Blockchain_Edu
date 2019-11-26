package blockchainCore.node.network;

import blockchainCore.DB.Db;
import blockchainCore.blockchain.*;
import blockchainCore.blockchain.event.BlockSignalHandler;
import blockchainCore.blockchain.transaction.*;
import blockchainCore.blockchain.wallet.Wallet;
import blockchainCore.blockchain.wallet.Wallets;
import blockchainCore.node.network.event.NetworkHandler;
import blockchainCore.node.network.event.NetworkListener;
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
    private boolean bMining = false, bMineThreadStart = false;

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

    private ConcurrentSkipListSet<String> receivedBlocks = new ConcurrentSkipListSet<>();

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
    public void createGenesisBlock() { this.bc = new Blockchain(wallet.getAddress(), this.db); }
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
            network.sendTx(_nodeId, tx);
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
            try { sleep(10L); } catch (InterruptedException ignored) {}

            fetchInvectory();

            if (!bMining) {
                bMining = true;
                bMineThreadStart = false;
                new Thread(new Runnable() {
                    public void run() {
                        bMineThreadStart = true;
                        mineBlock();
                        bMining = false;
                    }
                }).start();

                while (!bMineThreadStart)
                    try { sleep(10L); } catch (InterruptedException ignored) {}
            }

            // 고아 블록의 이전 블록 가져오기
            ConcurrentHashMap<String, Block> orphanBlocks = bc.getOrphanBlock();
            for (Block block : orphanBlocks.values()) {
                String prevBlock = Utils.toHexString(block.getPrevBlockHash());
                if (!orphanBlocks.containsKey(prevBlock))
                    invBlock.add(prevBlock);
            }

            // 고아 블록 처리 및 처리된 고아 블록 전파
            ArrayList<byte[]> blocks = bc.addOrphanBlock();
            for (String _nodeId : network.getConnList())
                network.sendInv(_nodeId, Network.TYPE.BLOCK, Utils.bytesConcat(blocks.toArray(new byte[][]{})));
        }

        network.close();
        NetworkHandler.removeListener(nodeId);
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

        BlockSignalHandler.callEvent(nodeId, nodeId, newBlock);

        // 블록내 트랜잭션 TxPool 에서 제거
        for (Transaction tx : newBlock.getTransactions())
            txPool.remove(Utils.toHexString(tx.getId()));

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
                network.sendGetData(nodeId, Network.TYPE.BLOCK, Utils.hexToBytes(hash));
            }
        }

        synchronized (invTx) {
            Iterator<String> txIter = invTx.iterator();
            if (txIter.hasNext()) {
                String hash = txIter.next();

                String nodeId = clients.get(random.nextInt(clients.size()));
                network.sendGetData(nodeId, Network.TYPE.TX, Utils.hexToBytes(hash));
            }
        }
    }

    private void handleAddress(String from) {
        if (!network.getConnList().contains(from))
            network.connectTo(from);
    }
    private void handleBlock(String from, byte[] data) {
        Block block = Utils.toObject(data);
        String blockHash = Utils.toHexString(block.getHash());

        BlockSignalHandler.callEvent(from, nodeId, block);

        invBlock.remove(blockHash);

        String blockKey = nodeId + blockHash;
        receivedBlocks.add(blockHash);
        while (receivedBlocks.contains(blockHash))
            try { sleep(50L); } catch (InterruptedException ignored) {}

        if (!bc.addBlock(block)) return;
        System.out.println(nodeId + "에 " + blockHash + " 블록이 추가 되었습니다.");

        BlockSignalHandler.callEvent(nodeId, nodeId, block);

        // 블록내 트랜잭션 pool 에서 제거
        for (Transaction tx : block.getTransactions())
            txPool.remove(Utils.toHexString(tx.getId()));

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

                    String hash = Utils.toHexString(item);

                    if (!invBlock.contains(hash) && bc.findBlock(item) == null)
                        invBlock.add(hash);
                }
                break;

            case Network.TYPE.TX:
                for (int i = 0; i < items.length; i += 32) {
                    byte[] item = Arrays.copyOfRange(items, i, i+32);

                    String hash = Utils.toHexString(item);

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
                    invBlock.add(Utils.toHexString(hash));
                break;
            case Network.TYPE.TX:
                String id = Utils.toHexString(hash);
                Transaction tx = null;

                if (txPool.containsKey(id))
                    tx = txPool.get(id);
                else
                    tx = bc.findTransaction(hash);

                if (tx != null)
                    network.sendTx(from, tx);
                else
                    invTx.add(Utils.toHexString(hash));
        }
    }
    private void handleTx(String from, byte[] data) {
        Transaction tx = Utils.toObject(data);
        String id = Utils.toHexString(tx.getId());

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

    public ArrayList<Transaction> getTxsFromTxPool() {
        Collection col = txPool.values();
        ArrayList<Transaction> txs = new ArrayList<>(col);
        return txs;
    }
    public void endTransmission(String blockHash) {
        receivedBlocks.remove(blockHash);
    }

    @Override
    public void Listen(String from, byte[] data) {
        handleConnection(from, data);
    }
}
