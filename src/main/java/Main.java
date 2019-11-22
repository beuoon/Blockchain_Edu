import blockchain.Transaction;
import blockchain.Wallets;
import node.Node;
import utils.Utils;

import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static void main(String args[]) throws Exception {
        /*
        blockchain.Db db = new blockchain.Db();
        blockchain.Wallets wallets = new blockchain.Wallets();
        String address1 = wallets.createWallet();
        String address2 = wallets.createWallet();
        String address3 = wallets.createWallet();

        //create blockchain
        blockchain.Blockchain bc = new blockchain.Blockchain(address1, db);
        //bc.MineBlock("Send 1 BTC to Minsung");
        //bc.addBlock("Send 2 more BTC to Minsung");
        blockchain.Functions.getBalance(address1, bc);
        blockchain.Functions.getBalance(address2, bc);
        blockchain.Functions.getBalance(address3, bc);


        blockchain.Functions.send(wallets.getWallet(address1), address2, 30, bc);

        blockchain.Functions.getBalance(address1, bc);
        blockchain.Functions.getBalance(address2, bc);
        blockchain.Functions.getBalance(address3, bc);

        blockchain.Functions.send(wallets.getWallet(address2), address3, 10, bc);

        blockchain.Functions.getBalance(address1, bc);
        blockchain.Functions.getBalance(address2, bc);
        blockchain.Functions.getBalance(address3, bc);


        blockchain.Functions.send(wallets.getWallet(address1), address3, 30, bc);

        blockchain.Functions.getBalance(address1, bc);
        blockchain.Functions.getBalance(address2, bc);
        blockchain.Functions.getBalance(address3, bc);

         */

        Wallets wallets = new Wallets();
        ArrayList<String> addressArr = new ArrayList<>();
        ArrayList<Node> nodes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String address = wallets.createWallet();
            Node node = new Node();
            node.start();

            addressArr.add(address);
            nodes.add(node);
        }

        Scanner scan = new Scanner(System.in);
        scan.next();

        // Test
        nodes.get(0).send(nodes.get(1).getAddress(), 5);
        nodes.get(0).send(nodes.get(2).getAddress(), 5);

        scan.next();

        for (Node node: nodes)
            node.checkBalance();

        scan.next();

        for (Node node: nodes)
            node.close();

        for (Node node: nodes)
            node.join();
    }
}
