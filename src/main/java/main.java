import network.WebAppServer;
import network.bcWebSocket;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class main {
    public static void main(String[] args) throws IOException {
        WebAppServer server = new WebAppServer();
        System.out.println("Server On");
        server.run();
        System.out.println("Se2");
        WebSocketServer s = new bcWebSocket(new InetSocketAddress("localhost", 8887));
        s.run();
    }
}
