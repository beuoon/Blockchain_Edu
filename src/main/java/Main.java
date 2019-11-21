import org.bitcoinj.core.Base58;

import java.util.Iterator;

public class Main {
    public static void main(String args[]) throws Exception{
        Db db = new Db();
        Wallets wallets = new Wallets();
        String address1 = wallets.createWallet();
        String address2 = wallets.createWallet();
        String address3 = wallets.createWallet();

        //create blockchain
        Blockchain bc = new Blockchain(address1, db);
        //bc.MineBlock("Send 1 BTC to Minsung");
        //bc.addBlock("Send 2 more BTC to Minsung");
        Functions.getBalance(address1, bc);
        Functions.getBalance(address2, bc);
        Functions.getBalance(address3, bc);


        Functions.send(wallets.getWallet(address1), address2, 30, bc);

        Functions.getBalance(address1, bc);
        Functions.getBalance(address2, bc);
        Functions.getBalance(address3, bc);

        Functions.send(wallets.getWallet(address2), address3, 10, bc);

        Functions.getBalance(address1, bc);
        Functions.getBalance(address2, bc);
        Functions.getBalance(address3, bc);

        Functions.send(wallets.getWallet(address1), address3, 30, bc);

        Functions.getBalance(address1, bc);
        Functions.getBalance(address2, bc);
        Functions.getBalance(address3, bc);

    }
}
