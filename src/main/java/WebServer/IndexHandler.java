package WebServer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;

/**
 * Single Page Application을 호출하기 위한 Handler.
 */
public class IndexHandler implements HttpHandler {
    private static final String HandlerPath = "/";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        File file  = new File("");
        file = new File(file.getAbsolutePath() + "/web/resources/index.html");
        BufferedReader br = new BufferedReader(new FileReader(file));

        String data = "";
        String nextLine = null;
        while((nextLine = br.readLine()) != null){
            data += nextLine;
        }
        exchange.sendResponseHeaders(200, 0);
        OutputStream out = exchange.getResponseBody();
        out.write(data.getBytes());
        out.close();
    }
}