package node;

import DB.Db;
import blockchain.*;
import blockchain.transaction.Transaction;
import blockchain.transaction.TxOutput;
import blockchain.transaction.UTXOSet;
import blockchain.wallet.Wallets;
import node.event.EventHandler;
import node.event.MessageEventArgs;
import org.bitcoinj.core.Base58;
import utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Semaphore;

public class Node extends Thread implements EventHandler<MessageEventArgs> {
    public static int NodeCount = 0;
    private int number;
    private boolean bLoop = true;

    // Wallet
    private Wallets wallets;
    private String address;

    // Blockchain
    private Db db;
    private Blockchain bc;
    private HashMap<String, Transaction> mempool = new HashMap<>();

    // Network
    private static final int COMMAND_LEN = 13, DATA_TYPE_LEN = 5;
    private Network network;

    // Mutex
    private Semaphore mempoolSem = new Semaphore(1);

    public Node() throws Exception {
        wallets = new Wallets();
        address = wallets.createWallet();

        this.db = new Db();

        number = NodeCount++;
        if (number == 0)
            this.bc = new Blockchain(address, this.db);
        else {
            this.bc = new Blockchain(this.db);
            // TODO: blockchain 갱신
        }

        this.network = new Network(number, this);
    }

    // TEST
    public void send(String to, int amount) throws Exception {
        UTXOSet utxoSet = new UTXOSet(bc);
        Transaction tx = bc.newUTXOTransaction(wallets.getWallet(address), to, amount, utxoSet);

        try {
            mempoolSem.acquire();
            try {
                mempool.put(Base58.encode(tx.getId()), tx);
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

            mineBlock();
        }

        if (network.checkConnection())
            network.close();
    }

    private void mineBlock() {
        // Transaction 준비
        Transaction[] txs = null;
        try {
            mempoolSem.acquire();
            try {
                if (mempool.size() >= 2) {
                    int txLen = mempool.size();
                    txs = new Transaction[txLen + 1];
                    Iterator<Transaction> iter = mempool.values().iterator();

                    for (int i = 0; i < txLen && iter.hasNext(); i++)
                        txs[i] = iter.next();
                    txs[txLen] = new Transaction(address, "");
                }
            } catch (Exception ignored) {
            } finally {
                mempoolSem.release();
            }
        } catch (InterruptedException ignored) {
        }

        if (txs == null) return ; // 채굴 안함

        Block newBlock = null;
        try {
            newBlock = bc.MineBlock(txs); // TODO: 채굴 도중 다른 블록 들어오는 거 예외처리 해야 됨
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (newBlock == null) return ; // 채굴 실패

        // UTXO reindex
        UTXOSet utxoSet = new UTXOSet(bc);
        utxoSet.reIndex();

        // 블록내 트랜잭션 pool 에서 제거
        try {
            mempoolSem.acquire();
            try {
                for (Transaction tx : newBlock.getTransactions())
                    mempool.remove(Base58.encode(tx.getId()));
            } catch (Exception ignored) {
            } finally {
                mempoolSem.release();
            }
        } catch (InterruptedException ignored) {}

        // 블록 전파
        for (Client client : network.getClients())
            sendInv(client, InvType.Tx, new byte[][]{newBlock.getHash()});
    }

    public void eventReceived(Object sender, MessageEventArgs e) {
        Client client = (Client)sender;
        byte[] buff = e.getMessage();
        this.handleConnection(client, buff, this.bc);
    }

    public void close() {
        bLoop = false;
    }

    private void requestBlocks() {
    }

    private void sendBlock(Client client, Block b) {
        byte[] command = new byte[]{CommandType.Block.getNumber()};
        byte[] data = Utils.toBytes(b);

        byte[] buff = Utils.bytesConcat(command, data);

        client.send(buff);
    }
    private void sendInv(Client client, InvType kind, byte[][] items) {
        byte[] command = new byte[]{CommandType.Inv.getNumber()};
        byte[] invType = new byte[]{kind.getNumber()};

        // Data
        int dataLen = 0;
        for (int i = 0; i < items.length; i++)
            dataLen += items[i].length;

        byte[] data = new byte[dataLen];
        for (int i = 0, j = 0; i < items.length; i++, j += items[i].length)
            System.arraycopy(items[i], 0, data, j, items[i].length);

        // Buff
        byte[] buff = Utils.bytesConcat(command, invType, data);

        client.send(buff);
    }
    private void sendGetBlocks(Client client) {
        byte[] command = new byte[]{CommandType.GetBlock.getNumber()};

        client.send(command);
    }
    private void sendGetData(Client client) {
        byte[] command = new byte[]{CommandType.GetData.getNumber()};

        client.send(command);
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

        // TODO: bc.addBlock(block);

        // UTXO reindex
        UTXOSet utxoSet = new UTXOSet(bc);
        utxoSet.reIndex();

        // 블록내 트랜잭션 pool 에서 제거
        try {
            mempoolSem.acquire();
            try {
                for (Transaction tx : block.getTransactions())
                    mempool.remove(Base58.encode(tx.getId()));
            } catch (Exception ignored) {
            } finally {
                mempoolSem.release();
            }
        } catch (InterruptedException ignored) {}

    }
    private void handleInv(byte[] data, Blockchain bc) {
    }
    private void handleGetBlocks(Client client, byte[] data, Blockchain bc) {
    }
    private void handleGetData(Client client, byte[] data, Blockchain bc) {
    }
    private void handleTx(Client sendClient, byte[] data, Blockchain bc) {
        Transaction tx = Utils.toObject(data);

        try {
            mempoolSem.acquire();
            try {
                String id = Base58.encode(tx.getId());
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
            case Inv:         handleInv(data, bc);                break;
            case GetBlock :   handleGetBlocks(client, data, bc);  break;
            case GetData:     handleGetData(client, data, bc);    break;
            case Tx:          handleTx(client, data, bc);         break;
            case Version:     handleVersion(data, bc);            break;
        }
    }
}
