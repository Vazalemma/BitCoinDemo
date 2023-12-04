package BitCoinDemo;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

class IPFunctions {
    static JSONObject formJsonResponse(String address) {
        JSONObject json = new JSONObject();
        try (BufferedReader br = new BufferedReader(new FileReader("src/src/main/java/BitCoinDemo/servers.txt"))) {
            String line;
            int count = 0;
            while ((line = br.readLine()) != null && count++ < 10) {
                if (line.equals(address)) count--;
                else json.append("servers", line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    static void rememberIP(String address) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter("src/src/main/java/BitCoinDemo/servers.txt", true));
            BufferedReader br = new BufferedReader(new FileReader("src/src/main/java/BitCoinDemo/servers.txt"));
            String line;
            while ((line = br.readLine()) != null) if (line.equals(address)) return;
            bw.write(address + "\n");
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void sendPostRequest(HttpURLConnection http, String key, String value, String key2, String value2) throws Exception {
        http.setRequestMethod("POST");
        http.setDoOutput(true);
        HashMap<String,String> arguments = new HashMap<>();
        arguments.put(key, value);
        if (key2 != null) arguments.put(key2, value2);
        StringJoiner sj = new StringJoiner("&");
        for (Map.Entry<String,String> entry : arguments.entrySet())
            sj.add(URLEncoder.encode(entry.getKey(), "UTF-8") + "=" + URLEncoder.encode(entry.getValue(), "UTF-8"));
        byte[] out = sj.toString().getBytes(StandardCharsets.UTF_8);
        int length = out.length;
        http.setFixedLengthStreamingMode(length);
        http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        http.connect();
        try (OutputStream os = http.getOutputStream()) {
            os.write(out);
        }
    }

    static void sendPostRequest(HttpURLConnection http, String key, String value, String key2, byte[] value2, byte[] pubkey) throws Exception {
        http.setRequestMethod("POST");
        http.setDoOutput(true);
        HashMap<String,String> arguments = new HashMap<>();
        arguments.put(key, value);
        if (key2 != null) arguments.put(key2, new String(value2, "ISO-8859-1"));
        arguments.put("key", new String(pubkey, "ISO-8859-1"));
        StringJoiner sj = new StringJoiner("&");
        for (Map.Entry<String,String> entry : arguments.entrySet())
            sj.add(URLEncoder.encode(entry.getKey(), "UTF-8") + "=" + URLEncoder.encode(entry.getValue(), "UTF-8"));
        byte[] out = sj.toString().getBytes(StandardCharsets.UTF_8);
        int length = out.length;
        http.setFixedLengthStreamingMode(length);
        http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        http.connect();
        try (OutputStream os = http.getOutputStream()) {
            os.write(out);
        }
    }

    static void sendPostRequest(HttpURLConnection http, String key, String value) throws Exception {
        sendPostRequest(http, key, value, null, null);
    }

    static String getPort(HttpExchange t) {
        return ParameterDecoder.getParam(ParameterDecoder.getParams(t.getRequestURI()), "port");
    }

    static String getIP(HttpExchange t) {
        return t.getRemoteAddress().toString().substring(1).split(":")[0];
    }
}
