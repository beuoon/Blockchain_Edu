package blockchainCore.blockchain;

import blockchainCore.blockchain.transaction.Transaction;
import blockchainCore.blockchain.transaction.TxOutput;
import blockchainCore.blockchain.transaction.UTXOSet;
import blockchainCore.blockchain.wallet.Wallet;
import org.bitcoinj.core.Base58;

import java.util.ArrayList;
import java.util.Arrays;

public class Functions {
    public static void getBalance(String address, Blockchain bc) {
        UTXOSet utxoSet = new UTXOSet(bc);
        byte[] pubkeyHash = Base58.decode(address);
        pubkeyHash = Arrays.copyOfRange(pubkeyHash, 1, pubkeyHash.length - 4);
        ArrayList<TxOutput> UTOXs = utxoSet.findUTXO(pubkeyHash);

        int balance = 0;

        for(TxOutput out : UTOXs) {
            balance += out.getValue();
        }

        System.out.printf("Balance of '%s' : %d\n", address, balance);
    }

    public static void send(Wallet from, String to, int amount, Blockchain bc) throws Exception {
        UTXOSet utxoSet = new UTXOSet(bc);

        Transaction tx = bc.newUTXOTransaction(from, to, amount, utxoSet);
        Transaction coinbase = new Transaction(from.getAddress(), "");
        Block newBlock = bc.mineBlock(new Transaction[]{coinbase, tx});

        utxoSet.update(newBlock);

        System.out.println("Success!");
    }
}