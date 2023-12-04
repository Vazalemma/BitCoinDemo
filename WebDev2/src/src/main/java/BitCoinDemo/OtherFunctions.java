package BitCoinDemo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.*;

public class OtherFunctions {
    private static List<String> adjectives = Arrays.asList("big", "small", "beautiful", "ugly", "smelly", "sweet",
            "smiling", "adventurous", "noisy", "manly", "colorful", "mangled", "rusty", "crappy", "tired", "sleepy");
    private static List<String> nouns = Arrays.asList("person", "penguin", "cat", "dog", "house", "table", "bunny",
            "laptop", "Werner", "penis", "skyscraper", "park", "museum", "piano", "bookshelf", "snow", "tree", "chair");
    private static List<String> verbs = Arrays.asList("barks", "meows", "defeats", "walks", "swims", "talks", "travels",
            "plays", "falls", "stands", "kills", "fucks", "washes", "yells", "sings", "runs", "jumps", "hops", "coughs");
    private static List<String> mverbs = Arrays.asList("bark", "meow", "sing", "walk", "swim", "talk", "cough",
            "play", "fall", "stand", "kill", "fuck", "wash", "yell", "defeat", "run", "jump", "hop", "travel");
    private static List<String> connectors = Arrays.asList("and", "or", "but", "thus", "therefor", "although");
    private static List<String> comma = Collections.singletonList(",");
    private static List<String> and = Collections.singletonList("and");
    private static List<List<List<String>>> sentences = Arrays.asList(
            Arrays.asList(adjectives, nouns, verbs, adjectives, nouns),
            Arrays.asList(adjectives, nouns, verbs, adjectives, nouns, comma, connectors, adjectives, nouns, verbs, adjectives, nouns),
            Arrays.asList(adjectives, adjectives, nouns, and, adjectives, adjectives, nouns, mverbs, adjectives, nouns),
            Arrays.asList(nouns, verbs, comma, connectors, nouns, verbs),
            Arrays.asList(nouns, verbs, and, nouns, verbs),
            Arrays.asList(nouns, and, nouns, mverbs, adjectives, nouns)
    );

    private static List<Double> sums = Arrays.asList(0.01, 0.001, 0.3, 0.2, 0.005);

    static String randomSentence() {
        Random r = new Random();
        List<List<String>> sentence = sentences.get(r.nextInt(sentences.size()));
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < sentence.size(); i++) {
            List<String> words = sentence.get(i);
            builder.append(words.get(r.nextInt(words.size())));
            builder.append(i == sentence.size() - 1 ? "." : sentence.get(i + 1).equals(comma) ? "" : " ");
        }
        return builder.toString();
    }

    static JSONObject randomTransaction(int port) {
        try {
            return new JSONObject().put("from", Integer.toString(port)).put("to", "9102")
                    .put("sum", sums.get(new Random().nextInt(sums.size())))
                    .put("timestamp", getTimeStamp());
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    static String getTimeStamp() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df.format(new Date());
    }

    static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            return "Error: Failed to generate SHA-256";
        }
    }

    static JSONObject getJsonBlocks(List<SimpleEntry<String, String>> blocks) {
        JSONObject json = new JSONObject();
        try {
            for (SimpleEntry<String, String> block : blocks) json.append("blocks", block.getValue());
            return json;
        } catch (JSONException e) {
            return json;
        }
    }

    static JSONObject getJsonBlocks(List<SimpleEntry<String, String>> blocks, String hash) {
        JSONObject json = new JSONObject();
        try {
            boolean from = false;
            for (SimpleEntry<String, String> block : blocks) {
                if (block.getKey().equals(hash)) from = true;
                if (from) json.append("blocks", block.getValue());
            }
            return json;
        } catch (JSONException e) {
            return json;
        }
    }

    static JSONObject getJsonBlock(List<SimpleEntry<String, String>> blocks, String hash) {
        JSONObject json = new JSONObject();
        try {
            for (SimpleEntry<String, String> block : blocks) {
                if (block.getKey().equals(hash)) {
                    json.append("block", block.getValue());
                    break;
                }
            }
            return json;
        } catch (JSONException e) {
            return json;
        }
    }

    static String decodeBlock(String input) {
        try {
            return URLDecoder.decode(input.split("=")[1].split("&")[0], "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    static String decodeHash(String input) {
        return input.split("=")[2].split("&")[0].replace("+", " ").replace("%2C", ",");
    }

    static byte[] decodeBytes(String input) {
        try {
            return URLDecoder.decode(input.split("=")[3].split("&")[0], "UTF-8").getBytes("ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            return new byte[]{};
        }
    }

    static byte[] decodeMsg(String input) {
        try {
            return URLDecoder.decode(input.split("=")[2].split("&")[0], "UTF-8").getBytes("ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            return new byte[]{};
        }
    }

    static boolean hasKey(String input) {
        return input.split("=").length == 4;
    }

    private static boolean isBadBlock(String block) {
        List<String> badWords = Arrays.asList("crappy", "penis", "kill", "fuck", "kills", "fucks");
        String[] words = block.replace(",", "").replace(".", "").split(" ");
        for (String word : words) if (badWords.contains(word)) return true;
        return false;
    }

    public static void main(String[] args) {
        System.out.println(randomSentence());
        System.out.println(isBadBlock("tree fucks and dog meows."));
        System.out.println(isBadBlock("ugly penis yells manly dog."));
    }
}
