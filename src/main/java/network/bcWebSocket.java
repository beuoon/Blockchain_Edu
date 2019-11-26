package network;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import blockchainCore.BlockchainCore;
import blockchainCore.blockchain.Blockchain;
import blockchainCore.blockchain.wallet.Wallet;
import blockchainCore.node.network.Node;
import com.google.gson.Gson;
import network.resources.handler.WebSocketHandler;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class bcWebSocket extends WebSocketServer {
    WebSocketHandler webSocketHandler = new WebSocketHandler();

    public bcWebSocket(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        webSocket.send("Welcome to the server!"); //This method sends a message to the new client
        //broadcast( "new connection: " + handshake.getResourceDescriptor() ); //This method sends a message to all clients connected
        System.out.println("new connection to " + webSocket.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        System.out.println("closed " + webSocket.getRemoteSocketAddress() + " with exit code " + i + " additional info: " + s);
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        Gson gson = new Gson();
        Map<String, Object> sendObject = null;

        System.out.println("received message from "    + webSocket.getRemoteSocketAddress() + ": " + s);

        HashMap<String, Object> msg = new HashMap<>();
        msg = gson.fromJson(s, msg.getClass());
        String type = (String)msg.get("type");
        Object data = msg.get("data");

        switch (type) {
            case "Node.C":
                String nodeId = webSocketHandler.createNode();
                sendObject = webSocketHandler.nodeInf(nodeId);
                break;
            case "Node.D":
                webSocketHandler.destoryNode(data);
                break;
            case "Wallet.C":
                Wallet wallet = webSocketHandler.createWallet(data);
                sendObject = webSocketHandler.walletInf(wallet);
                break;
            case "Connection.C":    webSocketHandler.createConnection(data);    break;
            case "Connection.D":    webSocketHandler.destroyConnection(data);   break;
            case "Transmission.E":  webSocketHandler.endTransmission(data);     break;
            case "Send":            webSocketHandler.sendBTC(data);             break;
        }

        webSocket.send(gson.toJson(sendObject));


//        String node1 = webSocketHandler.bcCore.createNode();
//        String node2 = webSocketHandler.bcCore.createNode();
//
//        Node n1 = webSocketHandler.bcCore.getNode(node1);
//        n1.createWallet();
//        n1.createGenesisBlock(n1.getWallet().getAddress());
//
//        Node n2 = webSocketHandler.bcCore.getNode(node2);
//        n2.createWallet();
//        n2.createNullBlockchain();
//        n2.setGenesisBlock(n1.getGenesisBlock());
//
//        n1.getNetwork().autoConnect(1);
//
//        n1.send(n2.getWallet().getAddress(), 1);

//        webSocket.send(gson.toJson(webSocketHandler.nodeInf(n1.getNodeId())));
    }


    @Override
    public void onError(WebSocket webSocket, Exception e) {
        System.err.println("an error occurred on webSocketection " + webSocket.getRemoteSocketAddress()  + ":" + e);
    }
}
