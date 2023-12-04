package BitCoinDemo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;

public class Client {
    private List<String> servers = new ArrayList<>();
    private List<SimpleEntry<String, String>> transactions = new ArrayList<>(); // <hash, transaction>
    private List<JSONObject> blocks = new ArrayList<>();
    private int port = 0;
    private String name = String.valueOf(port);
    private double money = new Random().nextDouble();

    public static void main(String[] args) throws Exception {
        Client client = new Client();
        int port = 8000 + new Random().nextInt(2699) + 1;
        client.port = port;
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/getblocks", new BlocksHandler(client));
        httpServer.createContext("/getblocks/", new BlocksFromHandler(client));
        httpServer.createContext("/getdata/", new OneBlockHandler(client));
        httpServer.createContext("/inv", new TransactionSender(client));
        httpServer.createContext("/block", new BlocksSender(client));
        httpServer.createContext("/newclient", new NewClientHandler(client));
        httpServer.createContext("/mon", new MoneyCheckHandler(client));
        httpServer.createContext("/blck", new BlockReceiver(client));
        httpServer.setExecutor(null);
        httpServer.start();

        System.out.println("Started Client on port " + port);

        client.getServers(port);
        client.notifyServers(port);

        for (String server : randomize(client.servers)) getBlocksFromOtherClient(client, server);

        String input;
        Scanner scanner = new Scanner(System.in);
        System.out.print("Send block/blockify/kaevanda? (y/n/b/kX): ");
        while ((input = scanner.nextLine()) != null) {
            if (Arrays.asList("y", "n", "b", "k1", "k2", "k3", "k4", "k5").contains(input)) {
                if (input.equals("n")) return;
                if (input.equals("b")) {
                    if (!client.transactions.isEmpty()) {
                        List<String> transes = new ArrayList<>();
                        List<String> hashes = new ArrayList<>();
                        for (SimpleEntry<String, String> se : client.transactions) {
                            if (isBlockified(client, se.getValue())) continue;
                            if (!hasEnoughMoney(client, se.getValue())) continue;
                            if (transes.contains(se.getValue())) continue;
                            transes.add(se.getValue());
                            hashes.add(se.getKey());
                        }
                        JSONObject json = new JSONObject().put("nr", client.blocks.size() - 1).put("prev_hash", hashes.get(hashes.size() - 1))
                                .put("timestamp", OtherFunctions.getTimeStamp()).put("creator", port)
                                .put("merkele_root", merkeleRoot(hashes)).put("count", transes.size())
                                .put("transaction", new JSONArray(transes));
                        String nonce = "";
                        String hash = "";
                        for (int i = 0; i < 300; i++) {
                            nonce = "random" + new Random().nextInt(90000) + "string" + new Random().nextInt(90000);
                            hash = OtherFunctions.sha256(json.toString() + nonce);
                            if (hash.startsWith("0")) break;
                        }
                        json = new JSONObject().put("nr", client.blocks.size() - 1).put("prev_hash", hashes.get(hashes.size() - 1))
                                .put("timestamp", OtherFunctions.getTimeStamp()).put("nonce", nonce)
                                .put("hash", hash).put("creator", port)
                                .put("merkele_root", merkeleRoot(hashes)).put("count", transes.size())
                                .put("transaction", new JSONArray(transes));
                        client.blocks.add(json);
                        System.out.println("New block: " + json.toString());
                        for (String server : randomize(client.servers)) sendBlock(client, server, json.toString(), "false");
                    }
                } else if (input.startsWith("k") && input.length() == 2) {
                    if (!client.transactions.isEmpty()) {
                        int N = Integer.parseInt(input.substring(1, 2));
                        StringBuilder start = new StringBuilder();
                        while (N-- > 0) start.append("0");
                        JSONObject json = new JSONObject(client.getLastBlock());
                        String nonce = "";
                        String hash = "";
                        long t = System.currentTimeMillis();
                        while (System.currentTimeMillis() - t < 5000) {
                            nonce = "random" + new Random().nextInt(90000) + "string" + new Random().nextInt(90000);
                            hash = OtherFunctions.sha256(json.toString() + nonce);
                            if (hash.startsWith(start.toString())) break;
                        }
                        json.remove("hash");
                        json.remove("nonce");
                        json = json.put("hash", hash).put("nonce", nonce);
                        client.blocks.remove(client.blocks.size() - 1);
                        client.blocks.add(json);
                        System.out.println("New hash: " + hash);
                        for (String server : randomize(client.servers)) sendBlock(client, server, json.toString(), "true");
                    }
                } else {
                    /*String block = OtherFunctions.randomSentence();
                    for (String server : randomize(client.servers)) {
                        sendBlock(client, server, null, block);
                    }*/

                    String transaction = OtherFunctions.randomTransaction(client.port).toString();
                    client.money -= new JSONObject(transaction).getDouble("sum");
                    for (String server : randomize(client.servers)) {
                        sendTransactionEncrypted(client, server, null, transaction);
                    }
                }
            }
            System.out.print("Send block/blockify/kaevanda? (y/n/b/kX): ");
        }
    }

    private static boolean isBlockified(Client client, String transact) throws Exception {
        for (JSONObject json : client.blocks) {
            JSONArray array = json.getJSONArray("transactions");
            for (int i = 0; i < array.length(); i++) {
                JSONObject j = array.getJSONObject(i);
                if (j.toString().equals(transact)) return true;
            }
        }
        return false;
    }

    private static boolean hasEnoughMoney(Client client, String transact) throws Exception {
        String server = "127.0.0.1:" + new JSONObject(transact).getJSONObject("transaction").getString("from");
        URL url = new URL("http://" + server + "/mon?port=" + client.port);
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        String line;
        while ((line = in.readLine()) != null) {
            double d = Double.valueOf(line);
            if (d > new JSONObject(transact).getJSONObject("transaction").getDouble("sum")) return true;
        }
        return false;
    }

    private static String merkeleRoot(List<String> hashes) {
        List<String> store = new ArrayList<>(hashes);
        List<String> temp = new ArrayList<>();
        while (store.size() > 1) {
            for (int i = 0; i < store.size(); i += 2) {
                if (i + 1 == store.size()) temp.add(OtherFunctions.sha256(store.get(i) + store.get(i)));
                else temp.add(OtherFunctions.sha256(store.get(i) + store.get(i + 1)));
            }
            store = new ArrayList<>(temp);
            temp.clear();
        }
        return store.get(0);
    }

    private static void sendBlock(Client client, String server, String json, String replace) throws Exception {
        HttpURLConnection http = (HttpURLConnection) new URL("http://" + server + "/blck?port=" + client.port).openConnection();
        System.out.println("Sending block to " + server);
        IPFunctions.sendPostRequest(http, "json", json, "replace", replace);
    }

    private static void sendTransaction(Client client, String server, String hash, String block) throws Exception {
        HttpURLConnection http = (HttpURLConnection) new URL("http://" + server + "/inv?port=" + client.port).openConnection();
        if (block == null) {
            // block = OtherFunctions.randomSentence();
            block = OtherFunctions.randomTransaction(client.port).toString();
        }
        System.out.println("Sending \"" + block + "\" to " + server);
        if (hash == null) hash = OtherFunctions.sha256(client.getLastTransaction() + block);
        IPFunctions.sendPostRequest(http, "block", block, "hash", hash);
        BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
        String line;
        while ((line = in.readLine()) != null) {
            System.out.println("Received message \"" + line + "\" from " + server);
            if (line.length() > 30) {
                client.transactions.add(new SimpleEntry<>(hash, line));
            }
        }
    }

    private static void sendTransactionEncrypted(Client client, String server, String hash, String transaction) throws Exception {
        HttpURLConnection http = (HttpURLConnection) new URL("http://" + server + "/inv?port=" + client.port).openConnection();
        System.out.println("Sending block to " + server);
        if (hash == null) hash = OtherFunctions.sha256(client.getLastTransaction() + transaction);
        SimpleEntry<byte[], byte[]> hshKey = CryptoFunctions.encryptMessage(hash);
        IPFunctions.sendPostRequest(http, "block", transaction, "hash", hshKey.getKey(), hshKey.getValue());
        BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
        String line;
        while ((line = in.readLine()) != null) {
            System.out.println("Received message \"" + line + "\" from " + server);
            if (line.length() > 30) {
                client.transactions.add(new SimpleEntry<>(hash, line));
            }
        }
    }

    private static List<String> randomize(List<String> list) {
        Collections.shuffle(list);
        return list;
    }

    private String getLastTransaction() {
        if (transactions.size() == 0) return "";
        return transactions.get(transactions.size() - 1).getKey();
    }

    private String getLastBlock() {
        if (blocks.size() == 0) return "";
        return blocks.get(blocks.size() - 1).toString();
    }

    private static void getBlocksFromOtherClient(Client client, String server) throws Exception {
        URL url = new URL("http://" + server + "/getblocks?port=" + client.port);
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        String line;
        while ((line = in.readLine()) != null) {
            if (line.equals("{}")) return;
            JSONArray array = new JSONObject(line).getJSONArray("transactions");
            for (int i = 0; i < array.length(); i++) {
                String block = array.get(i).toString();
                System.out.println("Got block: " + block + " - FROM " + server);
                client.transactions.add(new SimpleEntry<>(OtherFunctions.sha256(client.getLastTransaction() + block), block));
            }
        }
    }

    private void getServers(int port) throws Exception {
        System.out.println("Asking for servers from the main server");
        URL url = new URL("http://localhost:8000/addr?port="+port);
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        String line;
        while ((line = in.readLine()) != null) {
            try {
                JSONArray array = new JSONObject(line).getJSONArray("servers");
                for (int i = 0; i < array.length(); i++) {
                    System.out.println("Received server " + array.get(i).toString() + " from main server");
                    servers.add(array.get(i).toString());
                }
            } catch (Exception e) {
                return;
            }
        }
    }

    private void notifyServers(int port) {
        System.out.println("Sending info of my existence to all existing servers");
        for (String server : servers) {
            try {
                HttpURLConnection http = (HttpURLConnection) new URL("http://" + server + "/newclient?port=" + port).openConnection();
                IPFunctions.sendPostRequest(http, "port", Integer.toString(port));
            } catch (Exception e) {
                return;
            }
        }
    }







    static class BlocksHandler implements HttpHandler {
        private Client client;

        BlocksHandler(Client client) {
            this.client = client;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            String port = IPFunctions.getPort(t);
            String jsonBlocks = OtherFunctions.getJsonBlocks(client.transactions).toString();
            System.out.println("Sending " + jsonBlocks + " to port " + port);
            t.sendResponseHeaders(200, jsonBlocks.length());
            OutputStream os = t.getResponseBody();
            os.write(jsonBlocks.getBytes());
            os.close();
        }
    }



    static class BlocksFromHandler implements HttpHandler {
        private Client client;

        BlocksFromHandler(Client client) {
            this.client = client;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            String port = IPFunctions.getPort(t);
            String path = t.getRequestURI().toString().split("/")[2];
            String jsonBlocks = OtherFunctions.getJsonBlocks(client.transactions, path).toString();
            System.out.println("Sending " + jsonBlocks + " to port " + port);
            t.sendResponseHeaders(200, jsonBlocks.length());
            OutputStream os = t.getResponseBody();
            os.write(jsonBlocks.getBytes());
            os.close();
        }
    }



    static class OneBlockHandler implements HttpHandler {
        private Client client;

        OneBlockHandler(Client client) {
            this.client = client;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            String port = IPFunctions.getPort(t);
            String path = t.getRequestURI().toString().split("/")[2];
            String jsonBlock = OtherFunctions.getJsonBlock(client.transactions, path).toString();
            System.out.println("Sending " + jsonBlock + " to port " + port);
            t.sendResponseHeaders(200, jsonBlock.length());
            OutputStream os = t.getResponseBody();
            os.write(jsonBlock.getBytes());
            os.close();
        }
    }



    static class TransactionSender implements HttpHandler {
        private Client client;

        TransactionSender(Client client) {
            this.client = client;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            boolean send = true;
            String port = IPFunctions.getPort(t);
            InputStream input = t.getRequestBody();
            StringBuilder stringBuilder = new StringBuilder();
            new BufferedReader(new InputStreamReader(input)).lines().forEach((String s) -> stringBuilder.append(s).append("\n"));
            String uri = stringBuilder.toString().replace("\n", "");
            String block = OtherFunctions.decodeBlock(uri);
            String hash;
            if (OtherFunctions.hasKey(uri)) hash = CryptoFunctions.decryptMessage(OtherFunctions.decodeMsg(uri), OtherFunctions.decodeBytes(uri));
            else hash = java.net.URLDecoder.decode(OtherFunctions.decodeHash(stringBuilder.toString()).replace("\n", ""), "UTF-8");
            String correctHash = OtherFunctions.sha256(client.getLastTransaction() + block);
            System.out.println("Received " + block.replace("\n", ""));
            try {
                JSONObject json = new JSONObject(block);
                JSONObject fullJson = new JSONObject().put("signature", "sign19839821").put("transaction", json);
                block = fullJson.toString();
            } catch (JSONException ignored) {}
            String response;
            for (SimpleEntry<String , String> entry : client.transactions) if (entry.getKey().equals(hash)) send = false;
            if (!send) {
                response = "\"-1\"";
            } else if (!correctHash.equals(hash)) {
                response = "\"Error: Not a valid block\"";
            } else {
                client.transactions.add(new SimpleEntry<>(hash, block));
                response = block;
            }
            System.out.println("Sending " + response);
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            t.close();
            try {
                if (send) for (String server : randomize(client.servers)) {
                    System.out.println(server + " - " + port);
                    if (!server.split(":")[1].equals(port)) sendTransaction(client, server, hash, block);
                }
            } catch (Exception ignored) {}
        }
    }



    static class BlocksSender implements HttpHandler {
        private Client client;

        BlocksSender(Client client) {
            this.client = client;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            boolean send = true;
            InputStream input = t.getRequestBody();
            StringBuilder stringBuilder = new StringBuilder();
            new BufferedReader(new InputStreamReader(input)).lines().forEach((String s) -> stringBuilder.append(s).append("\n"));
            try {
                JSONObject json = new JSONObject(stringBuilder.toString()).getJSONObject("block");
                JSONArray temp = json.getJSONArray("block1");
                for (SimpleEntry<String , String> entry : client.transactions) if (entry.getKey().equals(temp.get(0).toString())) send = false;
                for (int i = 1; i < 31 && send; i++) {
                    JSONArray array = json.getJSONArray("block" + i);
                    client.transactions.add(new SimpleEntry<>(array.get(0).toString(), array.get(1).toString()));
                }
                System.out.println("Received 30 transactions");
                String response;
                if (new Random().nextInt(100) > 97) response = "\"Error: Not a valid block\"";
                else response = "\"1\"";
                System.out.println("Sending " + response);
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                t.close();
                if (send) for (String server : randomize(client.servers)) sendTransaction(client, server, null, null);
            } catch (Exception ignored) {}
        }
    }



    static class NewClientHandler implements HttpHandler {
        private Client client;

        NewClientHandler(Client client) {
            this.client = client;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            InputStream input = t.getRequestBody();
            StringBuilder stringBuilder = new StringBuilder();
            new BufferedReader(new InputStreamReader(input)).lines().forEach((String s) -> stringBuilder.append(s).append("\n"));
            String port = stringBuilder.toString().split("=")[1].replace("\n", "");
            System.out.println("Received new client on port " + port);
            client.servers.add("127.0.0.1:" + port);
            String response = "1";
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            t.close();
        }
    }



    static class MoneyCheckHandler implements HttpHandler {
        private Client client;

        MoneyCheckHandler(Client client) {
            this.client = client;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = String.valueOf(client.money);
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            t.close();
        }
    }



    static class BlockReceiver implements HttpHandler {
        private Client client;

        BlockReceiver(Client client) {
            this.client = client;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            String port = IPFunctions.getPort(t);
            InputStream input = t.getRequestBody();
            StringBuilder stringBuilder = new StringBuilder();
            new BufferedReader(new InputStreamReader(input)).lines().forEach((String s) -> stringBuilder.append(s).append("\n"));
            String replace = stringBuilder.toString().split("=")[1].split("&")[0];
            String json = URLDecoder.decode(stringBuilder.toString().split("=")[2], "UTF-8");
            boolean send = true;
            for (JSONObject obj : client.blocks) if (obj.toString().equals(json)) send = false;
            if (send) {
                System.out.println("Received new block: " + json);
                if (replace.equals("true")) client.blocks.remove(client.blocks.size() - 1);
                try {
                    client.blocks.add(new JSONObject(json));
                    for (String server : randomize(client.servers)) {
                        if (!server.split(":")[1].equals(port)) sendBlock(client, server, json, replace);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
