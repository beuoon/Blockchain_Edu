import org.bitcoinj.core.Base58;

import java.util.ArrayList;
import java.util.Arrays;

public class Functions {
    public static void getBalance(String address, Blockchain bc) {
        byte[] pubkeyHash = Base58.decode(address);
        pubkeyHash = Arrays.copyOfRange(pubkeyHash, 1, pubkeyHash.length - 4);
        ArrayList<TxOutput> UTOXs = bc.findUTXO(pubkeyHash);

        int balance = 0;

        for(TxOutput out : UTOXs) {
            balance += out.getValue();
        }

        System.out.printf("Balance of '%s' : %d\n", address, balance);
    }

    public static void send(Wallet from, String to, int amount, Blockchain bc) throws Exception {
        Transaction tx = bc.newUTXOTransaction(from, to, amount);
        bc.MineBlock(new Transaction[]{tx});
        System.out.println("Success!");
    }
}
