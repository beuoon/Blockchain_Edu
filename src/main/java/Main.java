import DB.Db;
import blockchain.Blockchain;
import blockchain.wallet.Wallets;
import blockchain.Functions;
import node.Node;
import org.bitcoinj.core.Base58;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

public class Main {
    public static void main(String args[]) throws Exception {
        /*
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

         */

        Wallets wallets = new Wallets();
        ArrayList<String> addressArr = new ArrayList<>();
        ArrayList<Node> nodes = new ArrayList<>();

        Node firstNode = new Node();
        firstNode.start();
        addressArr.add(firstNode.getAddress());
        nodes.add(firstNode);

        System.out.println("제네시스 블록 만들때까지 대기..");
        Thread.sleep(100);

        for (int i = 1; i < 10; i++) {
            Node node = new Node();
            node.start();

            addressArr.add(node.getAddress());
            nodes.add(node);
        }

        // Test
        nodes.get(0).send(nodes.get(1).getAddress(), 5);
        nodes.get(0).send(nodes.get(2).getAddress(), 5);

        System.out.println("블록 만들때까지 대기..");
        Thread.sleep(100);

        for (Node node: nodes)
            node.checkBalance();

        for (Node node: nodes)
            node.close();

        for (Node node: nodes)
            node.join();

        System.out.print("끝");
    }

}
