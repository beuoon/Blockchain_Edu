package network.resources.handler;

import blockchainCore.BlockchainCore;
import blockchainCore.DB.Bucket;
import blockchainCore.blockchain.Block;
import blockchainCore.blockchain.Blockchain;
import blockchainCore.blockchain.transaction.Transaction;
import blockchainCore.blockchain.transaction.TxInput;
import blockchainCore.blockchain.transaction.TxOutput;
import blockchainCore.blockchain.wallet.Wallet;
import blockchainCore.node.network.Node;
import blockchainCore.utils.Utils;
import com.google.gson.Gson;
import network.WebAppServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class WebSocketHandler {
    BlockchainCore bcCore = new BlockchainCore();
    Gson gson = new Gson();

    public WebSocketHandler() {



    }

    public String nodeInf(String nodeId) {
        Node node = bcCore.getNode(nodeId);
        Map<String, String> nodeInf = new HashMap<>();

        nodeInf.put("id", node.getNodeId());
        nodeInf.put("knownNodes", gson.toJson(node.getNetwork().getConnList()));
        nodeInf.put("transactionPool", transactionInfs(node.getTxsFromTxPool()));
        nodeInf.put("blocks", blockInfs(node.getBlockChain().getBlocks()));
        nodeInf.put("wallets", walletInfs(node.getWallets()));

        return gson.toJson(nodeInf);
    }

    public String walletInfs(ArrayList<Wallet> wallets) {
        ArrayList<String> walletInfs = new ArrayList<>();

        for ( Wallet wallet : wallets ){
            walletInfs.add(walletInf(wallet));
        }

        return gson.toJson(walletInfs);

    }

    public String walletInf(Wallet wallet) {
        HashMap<String, Object> walletInf = new HashMap<>();
        walletInf.put("address", wallet.getAddress());
        walletInf.put("privateKey", Utils.byteArrayToHexString(wallet.getPrivateKey().getEncoded()));
        walletInf.put("publickKey", Utils.byteArrayToHexString(wallet.getPublicKey().getEncoded()));

        return gson.toJson(walletInf);
    }

    public String blockInfs(ArrayList<Block> blocks) {
        ArrayList<String> blockInfs = new ArrayList<>();

        for (Block block : blocks) {
            blockInfs.add(blockInf(block));
        }

        return gson.toJson(blockInfs);
    }


    public String blockInf(Block block) {
        HashMap<String, Object> blockInf = new HashMap<>();
        blockInf.put("timestamp", block.getTimestamp());
        blockInf.put("transactions", transactionInfs(new ArrayList<Transaction>(Arrays.asList(block.getTransactions()))));
        blockInf.put("prevBlockHash", Utils.byteArrayToHexString(block.getPrevBlockHash()));
        blockInf.put("hash", Utils.byteArrayToHexString(block.getHash()));
        blockInf.put("nonce", block.getNonce());
        blockInf.put("height", block.getHeight());

        return gson.toJson(blockInf);
    }


    public String transactionInfs(ArrayList<Transaction> txs) {
        ArrayList<String> txInfs = new ArrayList<>();

        for(Transaction tx : txs) {
            txInfs.add(transactionInf(tx));
        }

        return gson.toJson(txInfs);
    }

    public String transactionInf(Transaction tx) {
        HashMap<String, Object> txInf = new HashMap<>();
        txInf.put("id", Utils.byteArrayToHexString(tx.getId()));
        txInf.put("transactionInput", txInputInfs(tx.getVin()));
        txInf.put("transactionOutput", txOutputInfs(tx.getVout()));

        return gson.toJson(txInf);
    }

    public String txOutputInfs(ArrayList<TxOutput> txOutputs){
        ArrayList<String> txOnputInfs = new ArrayList<>();

        for(TxOutput txOutput : txOutputs) {
            txOnputInfs.add(txOutputInf(txOutput));
        }

        return gson.toJson(txOnputInfs);
    }

    public String txOutputInf(TxOutput txOutput) {
        HashMap<String, Object> txInputInf = new HashMap<>();
        txInputInf.put("value", txOutput.getValue());
        txInputInf.put("pubKeyHash", txOutput.getPublicKeyHash());

        return gson.toJson(txInputInf);
    }

    public String txInputInfs(ArrayList<TxInput> txinputs){
        ArrayList<String> txInputInfs = new ArrayList<>();

        for(TxInput txinput : txinputs) {
            txInputInfs.add(txInputInf(txinput));
        }

        return gson.toJson(txInputInfs);
    }

    public String txInputInf(TxInput txinput) {
        HashMap<String, Object> txInputInf = new HashMap<>();
        txInputInf.put("txId", txinput);
        txInputInf.put("outputIndex", txinput.getvOut());
        txInputInf.put("pubkey", txinput.getPubKey());
        txInputInf.put("signature", txinput.getSignature());

        return gson.toJson(txInputInf);
    }
}
