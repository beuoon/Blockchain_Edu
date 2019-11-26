package network;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListSet;

import blockchainCore.BlockchainCore;
import blockchainCore.blockchain.Block;
import blockchainCore.blockchain.Blockchain;
import blockchainCore.blockchain.event.BlockSignalHandler;
import blockchainCore.blockchain.event.BlockSignalListener;
import blockchainCore.blockchain.wallet.Wallet;
import blockchainCore.node.network.Node;
import blockchainCore.utils.Utils;
import com.google.gson.Gson;
import network.resources.handler.WebSocketHandler;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class bcWebSocket extends WebSocketServer implements BlockSignalListener {
    private WebSocketHandler webSocketHandler = new WebSocketHandler();

    private ConcurrentSkipListSet<WebSocket> sockets = new ConcurrentSkipListSet<>();

    public bcWebSocket(InetSocketAddress address) {
        super(address);
        BlockSignalHandler.setListener(this);
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        webSocket.send("Welcome to the server!"); //This method sends a message to the new client
        //broadcast( "new connection: " + handshake.getResourceDescriptor() ); //This method sends a message to all clients connected
        sockets.add(webSocket);
        System.out.println("new connection to " + webSocket.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        sockets.remove(webSocket);
        System.out.println("closed " + webSocket.getRemoteSocketAddress() + " with exit code " + i + " additional info: " + s);
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        Gson gson = new Gson();
        Map<String, Object> sendObject = new HashMap<>();
        Object obj;

        System.out.println("received message from "    + webSocket.getRemoteSocketAddress() + ": " + s);

        HashMap<String, Object> msg = new HashMap<>();
        msg = gson.fromJson(s, msg.getClass());
        String type = (String)msg.get("type");
        Object data = msg.get("data");

        switch (type) {
            case "Node.C":
                String nodeId = webSocketHandler.createNode();
                obj = webSocketHandler.nodeInf(nodeId);

                sendObject.put("type", "Node.I");
                sendObject.put("data", obj);
                break;
            case "Node.D":
                webSocketHandler.destoryNode(data);
                break;
            case "Wallet.C":
                Wallet wallet = webSocketHandler.createWallet(data);
                obj = webSocketHandler.walletInf(wallet);

                sendObject.put("type", "Wallet.I");
                sendObject.put("data", obj);
                break;
            case "Connection.C":    webSocketHandler.createConnection(data);    break;
            case "Connection.D":    webSocketHandler.destroyConnection(data);   break;
            case "Transmission.E":  webSocketHandler.endTransmission(data);     break;
            case "Send":            webSocketHandler.sendBTC(data);             break;
        }

        if (!sendObject.isEmpty())
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

    public void broadCast(String sendMessage) {
        synchronized (sockets) {
            for (WebSocket socket : sockets.toArray(new WebSocket[]{}))
                socket.send(sendMessage);
        }
    }

    @Override
    public void onEvent(String from, String to, Block block) {
        Gson gson = new Gson();
        Map<String, Object> sendObject = new HashMap<>();

        if (from.equals(to)) {
            Object obj = webSocketHandler.blockInf(block);
            sendObject.put("type", "Block.T");
            sendObject.put("data", obj);
        }
        else {
            HashMap<String, Object> obj = new HashMap<>();
            obj.put("from", from);
            obj.put("to", from);
            obj.put("block", Utils.byteArrayToHexString(block.getHash()));
            sendObject.put("type", "Block.G");
            sendObject.put("data", obj);
        }

        if (!sendObject.isEmpty())
            broadCast(gson.toJson(sendObject));
    }
}
