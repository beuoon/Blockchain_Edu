package network;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class Websocket extends WebSocketServer {

    public Websocket(InetSocketAddress address) {
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
        System.out.println("received message from "    + webSocket.getRemoteSocketAddress() + ": " + s);
        webSocket.send(s);
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        System.err.println("an error occurred on webSocketection " + webSocket.getRemoteSocketAddress()  + ":" + e);
    }
}
