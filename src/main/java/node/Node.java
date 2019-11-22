package node;

import DB.Db;
import blockchain.*;
import blockchain.transaction.Transaction;
import blockchain.transaction.TxInput;
import blockchain.transaction.TxOutput;
import blockchain.transaction.UTXOSet;
import blockchain.wallet.Wallet;
import node.event.EventHandler;
import node.event.MessageEventArgs;
import org.bitcoinj.core.Base58;
import utils.Utils;

import java.util.*;
import java.util.concurrent.Semaphore;

public class Node extends Thread implements EventHandler<MessageEventArgs> {
    public static int NodeCount = 0;
    private int number;
    private boolean bLoop = true;

    // Wallet
    private Wallet wallet;
    private String address;

    // Blockchain
    private Db db;
    private Blockchain bc;
    private HashMap<String, Transaction> mempool = new HashMap<>();
    private HashSet<String> invBlock = new HashSet<>(), invTx = new HashSet<>();

    // Network
    private Network network;

    // Mutex
    private Semaphore mempoolSem = new Semaphore(1);

    public Node() throws Exception {
        wallet = new Wallet();
        address = wallet.getAddress();

        this.db = new Db();

        number = NodeCount++;

        network = new Network(number, this);

        if (number == 0)
            bc = new Blockchain(address, db);
        else {
            bc = new Blockchain(db);

            Client client = network.getClients().get(0);
            sendGetBlocks(client);
        }
    }

    // TEST
    public void send(String to, int amount) throws Exception {
        UTXOSet utxoSet = new UTXOSet(bc);
        Transaction tx = bc.newUTXOTransaction(wallet, to, amount, utxoSet);

        try {
            mempoolSem.acquire();
            try {
                mempool.put(Utils.byteArrayToHexString(tx.getId()), tx);
            } catch (Exception ignored) {}
            finally {
                mempoolSem.release();
            }
        } catch (InterruptedException ignored) {}

        for (Client client : network.getClients())
            sendTx(client, tx);
    }
    public void checkBalance() throws Exception {
        UTXOSet utxoSet = new UTXOSet(bc);
        byte[] pubkeyHash = Base58.decode(address);
        pubkeyHash = Arrays.copyOfRange(pubkeyHash, 1, pubkeyHash.length - 4);
        ArrayList<TxOutput> UTOXs = utxoSet.findUTXO(pubkeyHash);

        int balance = 0;

        for(TxOutput out : UTOXs)
            balance += out.getValue();

        System.out.printf("Balance of '%s' : %d\n", address, balance);
    }
    public String getAddress() { return address; }

    public void run() {
        while (bLoop) {
            try {
                sleep(100L);
            } catch (InterruptedException ignored) {
            }

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

    private void getInv() {
        Iterator<String> blockIter = invBlock.iterator();
        if (blockIter.hasNext()) {
            String hash = blockIter.next();

            Random random = new Random();
            ArrayList<Client> clients = network.getClients();
            Client client = clients.get(random.nextInt(clients.size()));
            sendGetData(client, InvType.Block, Utils.hexStringToByteArray(hash));
        }

        Iterator<String> txIter = invTx.iterator();
        if (txIter.hasNext()) {
            String hash = txIter.next();

            Random random = new Random();
            ArrayList<Client> clients = network.getClients();
            Client client = clients.get(random.nextInt(clients.size()));
            sendGetData(client, InvType.Tx, Utils.hexStringToByteArray(hash));
            invTx.remove(hash);
        }
    }
    private void mineBlock() {
        if (!bc.validate()) return ; // blockchain 준비 안됨

        // Transaction 준비
        Transaction[] txs = null;
        try {
            mempoolSem.acquire();
            try {
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
                        txList.add(new Transaction(address, ""));
                        txs = txList.toArray(new Transaction[]{});
                    }
                }
            } catch (Exception ignored) {
            } finally {
                mempoolSem.release();
            }
        } catch (InterruptedException ignored) {
        }

        if (txs == null) return ; // 채굴 안함

        Block newBlock = bc.mineBlock(txs); // TODO: 채굴 도중 다른 블록 들어오는 거 예외처리 해야 됨

        if (newBlock == null) return ; // 채굴 실패

        // UTXO update
        UTXOSet utxoSet = new UTXOSet(bc);
        utxoSet.update(newBlock);

        System.out.println(number + "번 노드가 블록을 채굴!!");

        // 블록내 트랜잭션 pool 에서 제거
        try {
            mempoolSem.acquire();
            try {
                for (Transaction tx : newBlock.getTransactions())
                    mempool.remove(Utils.byteArrayToHexString(tx.getId()));
            } catch (Exception ignored) {
            } finally {
                mempoolSem.release();
            }
        } catch (InterruptedException ignored) {}

        // 블록 전파
        for (Client client : network.getClients())
            sendInv(client, InvType.Block, newBlock.getHash());
    }

    public void eventReceived(Object sender, MessageEventArgs e) {
        Client client = (Client)sender;
        byte[] buff = e.getMessage();
        this.handleConnection(client, buff, this.bc);
    }

    public void close() { bLoop = false; }

    private void requestBlocks() {
    }

    private void sendBlock(Client client, Block b) {
        byte[] command = new byte[]{CommandType.Block.getNumber()};
        byte[] data = Utils.toBytes(b);

        byte[] buff = Utils.bytesConcat(command, data);

        client.send(buff);
    }
    private void sendInv(Client client, InvType kind, byte[] data) {
        byte[] command = new byte[]{CommandType.Inv.getNumber()};
        byte[] invType = new byte[]{kind.getNumber()};

        byte[] buff = Utils.bytesConcat(command, invType, data);

        client.send(buff);
    }
    private void sendGetBlocks(Client client) {
        byte[] command = new byte[]{CommandType.GetBlock.getNumber()};

        client.send(command);
    }
    private void sendGetData(Client client, InvType invType, byte[] data) {
        byte[] command = new byte[]{CommandType.GetData.getNumber()};
        byte[] it = new byte[]{invType.getNumber()};

        byte[] buffer = Utils.bytesConcat(command, it, data);

        client.send(buffer);
    }
    private void sendTx(Client client, Transaction tx) {
        byte[] command = new byte[]{CommandType.Tx.getNumber()};
        byte[] data = Utils.toBytes(tx);

        byte[] buff = Utils.bytesConcat(command, data);

        client.send(buff);
    }
    private void sendVersion(Client client, Blockchain bc) {
        byte[] command = new byte[]{CommandType.Version.getNumber()};
        byte[] data = null; // TODO: bc.getBestHeight();

        byte[] buff = Utils.bytesConcat(command, data);

        client.send(buff);
    }

    private void handleBlock(byte[] data, Blockchain bc) {
        Block block = Utils.toObject(data);

        System.out.println(Utils.byteArrayToHexString(block.getHash()));

        if (!bc.validate()) return ;

        if (!bc.addBlock(block)) return ;

        // UTXO reindex
        UTXOSet utxoSet = new UTXOSet(bc);
        utxoSet.update(block);

        // 블록내 트랜잭션 pool 에서 제거
        try {
            mempoolSem.acquire();
            try {
                for (Transaction tx : block.getTransactions())
                    mempool.remove(Utils.byteArrayToHexString(tx.getId()));
            } catch (Exception ignored) {
            } finally {
                mempoolSem.release();
            }
        } catch (InterruptedException ignored) {}

    }
    private void handleInv(Client client, byte[] data, Blockchain bc) {
        InvType it = InvType.valueOf(data[0]);
        byte[] items = new byte[data.length-1];
        System.arraycopy(data, 1, items, 0, items.length);

        if (it == InvType.Block) {
            for (int i = 0; i < items.length; i += 32) {
                byte[] item = new byte[32];
                System.arraycopy(items, i, item, 0, 32);

                String hash = Utils.byteArrayToHexString(item);

                if (!invBlock.contains(hash) && bc.findBlock(item) == null)
                    invBlock.add(hash);
            }
        }
        else if (it == InvType.Tx) {
            for (int i = 0; i < items.length; i += 32) {
                byte[] item = new byte[32];
                System.arraycopy(items, i, item, 0, 32);

                String hash = Utils.byteArrayToHexString(item);

                if (!invTx.contains(hash) && !mempool.containsKey(hash) && bc.findTransaction(item) == null)
                    invTx.add(hash);
            }
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

        sendInv(client, InvType.Block, data);
    }
    private void handleGetData(Client client, byte[] data, Blockchain bc) {
        InvType it = InvType.valueOf(data[0]);
        byte[] hash = new byte[data.length-1];
        System.arraycopy(data, 1, hash, 0, hash.length);

        if (it == InvType.Block) {
            Block block = bc.findBlock(hash);
            if (block != null)
                sendBlock(client, block);
        }
        else if (it == InvType.Tx) {
            String id = Utils.byteArrayToHexString(hash);
            Transaction tx = null;

            if (mempool.containsKey(id))
                tx = mempool.get(id);
            else
                tx = bc.findTransaction(hash);

            if (tx != null)
                sendTx(client, tx);
        }
    }
    private void handleTx(Client sendClient, byte[] data, Blockchain bc) {
        Transaction tx = Utils.toObject(data);

        try {
            mempoolSem.acquire();
            try {
                String id = Utils.byteArrayToHexString(tx.getId());
                if (!mempool.containsKey(id)) {
                    mempool.put(id, tx);
                    System.out.printf("recived(%d)[%d]: %s\n", number, mempool.size(), id);

                    // 전파
                    for (Client client : network.getClients()) {
                        if (client.equals(sendClient)) continue;
                        sendTx(client, tx);
                        // sendInv(client, InvType.Tx, new byte[][]{tx.getId()});
                    }
                }

            } catch (Exception ignored) {
            } finally {
                mempoolSem.release();
            }

        } catch (InterruptedException ignored) {}
    }
    private void handleVersion(byte[] data, Blockchain bc) {
    }

    private void handleConnection(Client client, byte[] buff, Blockchain bc) {
        CommandType ct = CommandType.valueOf(buff[0]);
        byte[] data = new byte[buff.length-1];
        System.arraycopy(buff, 1, data, 0, data.length);

        switch(ct) {
            case Block:       handleBlock(data, bc);              break;
            case Inv:         handleInv(client, data, bc);        break;
            case GetBlock :   handleGetBlocks(client, bc);        break;
            case GetData:     handleGetData(client, data, bc);    break;
            case Tx:          handleTx(client, data, bc);         break;
            case Version:     handleVersion(data, bc);            break;
        }
    }
}
