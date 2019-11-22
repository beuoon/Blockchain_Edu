import DB.Db;
import blockchain.Blockchain;
import blockchain.wallet.Wallets;
import blockchain.Functions;
import node.Node;
import org.bitcoinj.core.Base58;
import utils.Utils;

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
        ArrayList<Node> nodes = new ArrayList<>();

        Node firstNode = new Node();
        firstNode.createWallet();
        firstNode.createGenesisBlock(firstNode.getWallet().getAddress());
        firstNode.start();
        nodes.add(firstNode);

        System.out.println("제네시스 블록 만들때까지 대기..");
        Thread.sleep(1000);

        for (int i = 1; i < 4; i++) {
            Node node = new Node();
            node.createWallet();
            node.createNullBlockchain();
            node.start();
            nodes.add(node);
        }

        // Test
        nodes.get(0).send(nodes.get(1).getWallet().getAddress(), 10);
        nodes.get(0).send(nodes.get(2).getWallet().getAddress(), 10);

        Thread.sleep(2000);

        for (Node node: nodes)
            node.checkBalance();

        nodes.get(0).send(nodes.get(3).getWallet().getAddress(), 10);
        nodes.get(1).send(nodes.get(3).getWallet().getAddress(), 3);
        nodes.get(2).send(nodes.get(3).getWallet().getAddress(), 3);

        Thread.sleep(2000);

        for (Node node: nodes)
            node.checkBalance();

        for (Node node: nodes)
            node.close();

        for (Node node: nodes)
            node.join();

        System.out.print("끝");
    }

}
