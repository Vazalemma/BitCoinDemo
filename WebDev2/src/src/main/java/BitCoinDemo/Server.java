package BitCoinDemo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import org.json.JSONObject;
import java.io.*;

public class Server {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/addr", new AddressHandler());
        System.out.println("Started server on port 8000");
        server.setExecutor(null);
        server.start();
    }

    static class AddressHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String port = IPFunctions.getPort(t);
            String IP = IPFunctions.getIP(t);
            String server = IP + ":" + port;
            JSONObject json = IPFunctions.formJsonResponse(server);
            IPFunctions.rememberIP(server);
            System.out.println("Sent existing IPs to " + server);
            t.sendResponseHeaders(200, json.toString().length());
            OutputStream os = t.getResponseBody();
            os.write(json.toString().getBytes());
            os.close();
        }
    }
}