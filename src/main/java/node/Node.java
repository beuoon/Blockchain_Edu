package node;

import DB.Db;
import blockchain.*;
import blockchain.transaction.Transaction;
import blockchain.transaction.TxInput;
import blockchain.transaction.TxOutput;
import blockchain.transaction.UTXOSet;
import blockchain.wallet.Wallet;
import blockchain.wallet.Wallets;
import node.event.EventHandler;
import node.event.MessageEventArgs;
import node.network.Client;
import node.network.Network;
import utils.Utils;

import java.util.*;

public class Node extends Thread implements EventHandler<MessageEventArgs> {
    public static int NodeCount = 0;
    private int number;
    private boolean bLoop = true;

    // WalletT

    private Wallets wallets;
    private Wallet wallet;

    // Blockchain
    private Db db;
    private Blockchain bc;
    private HashMap<String, Transaction> mempool = new HashMap<>();
    private HashSet<String> invBlock = new HashSet<>(), invTx = new HashSet<>();

    // Network
    private Network network;

    public Node() throws Exception {
        wallets = new Wallets();
        this.db = new Db();

        number = NodeCount++;
        this.network = new Network(number, this);
    }

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

    public ArrayList<String> getAddresses() {
        return wallets.getAddresses();
    }

    public void createGenesisBlock(String address) {
        this.bc = new Blockchain(address, this.db);
    }

    //임시 메소드임!
    public void createNullBlockchain() {
        this.bc = new Blockchain(this.db);
    }

    // TEST
    public void send(String to, int amount) {
        UTXOSet utxoSet = new UTXOSet(bc);
        try {
            Transaction tx = bc.newUTXOTransaction(wallet, to, amount, utxoSet);
            mempool.put(Utils.byteArrayToHexString(tx.getId()), tx);
            network.broadcast(tx);
        } catch (Exception e) {
            System.out.println("잔고가 부족합니다!!");
        }
    }

    public void checkBalance() {
        if(wallet == null) return;
        Functions.getBalance(wallet.getAddress(), bc);
    }

    public void run() {
        if (!bc.validate()) {
            while (bLoop && !bc.validate()) {
                Random random = new Random();
                ArrayList<Client> clients = network.getClients();
                Client client = clients.get(random.nextInt(clients.size()));

                network.sendGetBlocks(client);
                try {
                    sleep(1000);
                } catch (InterruptedException ignored) {}
            }
        }

        while (bLoop) {
            try {
                sleep(100L);
            } catch (InterruptedException ignored) {}

            if (!network.checkConnection()) {
                network.close();
                bLoop = false;
                break;
            }

            getInv();
            mineBlock();
        }

        if (network.checkConnection())
            network.close();
    }

    private void mineBlock() {
        if (!bc.validate()) return ; // blockchain 준비 안됨

        // Transaction 준비
        Transaction[] txs = null;

        if (mempool.size() >= 2) {
            ArrayList<Transaction> txList = new ArrayList<>();
            Iterator<Transaction> iter = mempool.values().iterator();

            ArrayList<TxInput> usedVin = new ArrayList<>();

            while (iter.hasNext()) {
                Transaction tx = iter.next();
                String key = Utils.byteArrayToHexString(tx.getId());

                // tx 검증
                if (!bc.validateTransaction(tx)) // TODO: 고아 거래 처리
                    continue;

                if (!bc.verifyTransaction(tx)) { // 잘못된 거래
                    mempool.remove(key);
                    continue;
                }

                //＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊//
                // vin 확인
                boolean canUse = true;
                for (TxInput vin : tx.getVin()) {
                    if (usedVin.contains(vin)) {
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

        Block newBlock = bc.mineBlock(txs); // TODO: 채굴 도중 다른 블록 들어오는 거 예외처리 해야 됨

        if (newBlock == null) return; // 채굴 실패

        // UTXO update
        UTXOSet utxoSet = new UTXOSet(bc);
        utxoSet.update(newBlock);

        System.out.println(number + "번 노드가 블록을 채굴!!");

        // 블록내 트랜잭션 pool 에서 제거

        for (Transaction tx : newBlock.getTransactions())
            mempool.remove(Utils.byteArrayToHexString(tx.getId()));

        // 블록 전파
        for (Client client : network.getClients())
            network.sendInv(client, Network.TYPE.BLOCK, newBlock.getHash());
    }

    public void eventReceived(Object sender, MessageEventArgs e) {
        Client client = (Client)sender;
        byte[] buff = e.getMessage();
        this.handleConnection(client, buff, this.bc);
    }

    public void close() {
        bLoop = false;
    }

    private void handleBlock(byte[] data, Blockchain bc) {
        Block block = Utils.toObject(data);

        invBlock.remove(Utils.byteArrayToHexString(block.getHash()));

        if (!bc.validate()) {
            if (!bc.addBlock(block)) {
                return ;
            }

            // UTXO reindex
            UTXOSet utxoSet = new UTXOSet(bc);
            utxoSet.reIndex();
        }
        else {
            if (!bc.addBlock(block))
                return;

            // UTXO update
            UTXOSet utxoSet = new UTXOSet(bc);
            utxoSet.update(block);
        }


        // 블록내 트랜잭션 pool 에서 제거
        for (Transaction tx : block.getTransactions())
            mempool.remove(Utils.byteArrayToHexString(tx.getId()));
    }

    private void handleInv(Client client, byte[] data, Blockchain bc) {
        int TYPE = data[0];
        byte[] items = Arrays.copyOfRange(data, 1, data.length);

        switch(TYPE){
            case Network.TYPE.BLOCK:
                for (int i = 0; i < items.length; i += 32) {
                    byte[] item = Arrays.copyOfRange(items, i, i+32);

                    String hash = Utils.byteArrayToHexString(item);

                    if (!invBlock.contains(hash) && bc.findBlock(item) == null)
                        network.sendGetData(client, Network.TYPE.BLOCK, item); // invBlock.add(hash);
                }
                break;

            case Network.TYPE.TX:
                for (int i = 0; i < items.length; i += 32) {
                    byte[] item = Arrays.copyOfRange(items, i, i+32);

                    String hash = Utils.byteArrayToHexString(item);

                    if (!invTx.contains(hash) && !mempool.containsKey(hash) && bc.findTransaction(item) == null) {
                        invTx.add(hash);
                        network.sendGetData(client, Network.TYPE.TX, item);
                    }
                }
                break;
        }
    }
    private void handleGetBlocks(Client client, Blockchain bc) {
        Iterator<Block> iter = bc.iterator();
        ArrayList<byte[]> blockHashes = new ArrayList<>();

        while (iter.hasNext()) {
            Block block = iter.next();
            blockHashes.add(block.getHash());
        }

        byte[] data = Utils.bytesConcat(blockHashes.toArray(new byte[][]{}));

        network.sendInv(client, Network.TYPE.BLOCK, data);
    }
    private void handleGetData(Client client, byte[] data, Blockchain bc) {
        int TYPE = data[0];
        byte[] hash = Arrays.copyOfRange(data, 1, data.length);

        switch (TYPE) {
            case Network.TYPE.BLOCK:
                Block block = bc.findBlock(hash);

                if (block != null)
                    network.sendBlock(client, block);
                break;
            case Network.TYPE.TX:
                String id = Utils.byteArrayToHexString(hash);
                Transaction tx = null;

                if (mempool.containsKey(id))
                    tx = mempool.get(id);
                else
                    tx = bc.findTransaction(hash);

                if (tx != null)
                    network.sendTx(client, tx);
        }
    }
    private void handleTx(Client sendClient, byte[] data, Blockchain bc) {
        Transaction tx = Utils.toObject(data);
        String id = Utils.byteArrayToHexString(tx.getId());

        invTx.remove(id);

        if (!mempool.containsKey(id)) {
            mempool.put(id, tx);
            System.out.println(number + ": " + id);

            // 전파
            for (Client client : network.getClients()) {
                if (client.equals(sendClient)) continue;
                network.sendInv(client, Network.TYPE.TX, tx.getId());
            }
        }
    }

    private void handleVersion(byte[] data, Blockchain bc) {
    }

    private void handleConnection(Client client, byte[] buff, Blockchain bc) {
        int TYPE = buff[0];
        byte[] data = Arrays.copyOfRange(buff, 1, buff.length);

        switch (TYPE) {
            case Network.TYPE.BLOCK:    handleBlock(data, bc);              break;
            case Network.TYPE.INV:      handleInv(client, data, bc);        break;
            case Network.TYPE.GETBLOCK: handleGetBlocks(client, bc);        break;
            case Network.TYPE.GETDATA:  handleGetData(client, data, bc);    break;
            case Network.TYPE.TX:       handleTx(client, data, bc);         break;
            case Network.TYPE.VERSION:  handleVersion(data, bc);            break;
        }
    }

    private void getInv() {
        Random random = new Random();
        ArrayList<Client> clients = network.getClients();
        if (clients.size() <= 0) return ;

        Iterator<String> blockIter = invBlock.iterator();
        if (blockIter.hasNext()) {
            String hash = blockIter.next();

            Client client = clients.get(random.nextInt(clients.size()));
            network.sendGetData(client, Network.TYPE.BLOCK, Utils.hexStringToByteArray(hash));
            // invBlock.remove(hash);
        }

        Iterator<String> txIter = invTx.iterator();
        if (txIter.hasNext()) {
            String hash = txIter.next();

            Client client = clients.get(random.nextInt(clients.size()));
            network.sendGetData(client, Network.TYPE.TX, Utils.hexStringToByteArray(hash));
            // invTx.remove(hash);
        }
    }
}
