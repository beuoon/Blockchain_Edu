import org.bitcoinj.core.Base58;

import java.security.PublicKey;
import java.util.ArrayList;
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



/*        Wallets wallets = new Wallets();
        ArrayList<String> addressArr = new ArrayList<>();
        ArrayList<Node> nodes = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            String address = wallets.createWallet();
            Node node = new Node(address);
            node.start();

            addressArr.add(address);
            nodes.add(node);
        }

        for (Node node: nodes)
            node.close();*/
    }
}
