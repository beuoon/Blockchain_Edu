import WebServer.WebAppServer;
import WebServer.bcWebSocket;
import blockchainCore.node.network.Node;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Random;

public class Main {
    public static void main(String[] args) throws IOException {
        WebAppServer server = new WebAppServer();
        server.run();
        WebSocketServer s = new bcWebSocket(new InetSocketAddress("localhost", 8887));
        s.run();
    }
}
