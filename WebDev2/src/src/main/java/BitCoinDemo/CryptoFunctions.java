package BitCoinDemo;

import org.json.JSONObject;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;

public class CryptoFunctions {
    private static final String ALGORITHM = "RSA";

    private static byte[] encrypt(byte[] publicKey, byte[] inputData) throws Exception {
        PublicKey key = KeyFactory.getInstance(ALGORITHM).generatePublic(new X509EncodedKeySpec(publicKey));
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.PUBLIC_KEY, key);
        return cipher.doFinal(inputData);
    }

    private static byte[] decrypt(byte[] privateKey, byte[] inputData) throws Exception {
        PrivateKey key = KeyFactory.getInstance(ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(privateKey));
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.PRIVATE_KEY, key);
        return cipher.doFinal(inputData);
    }

    private static KeyPair generateKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
        keyGen.initialize(512, random);
        return keyGen.generateKeyPair();
    }

    public static void main(String[] args) throws Exception {
        KeyPair generateKeyPair = generateKeyPair();
        byte[] publicKey = generateKeyPair.getPublic().getEncoded();
        byte[] privateKey = generateKeyPair.getPrivate().getEncoded();
        byte[] encryptedData = encrypt(publicKey, "hi this is Visruth here".getBytes());
        byte[] decryptedData = decrypt(privateKey, encryptedData);
        System.out.println(new String(decryptedData));

        String s = new String(encryptedData, "ISO-8859-1");
        byte[] d = s.getBytes("ISO-8859-1");
        System.out.println(Arrays.toString(encryptedData));
        System.out.println(Arrays.toString(d));

        SimpleEntry<byte[], byte[]> e = encryptMessage("Hello world how have you been it's been a while");
        System.out.println(decryptMessage(e.getKey(), e.getValue()));
    }

    static SimpleEntry<byte[], byte[]> encryptMessage(String message) {
        try {
            KeyPair generateKeyPair = generateKeyPair();
            byte[] privateKey = generateKeyPair.getPublic().getEncoded();
            byte[] publicKey = generateKeyPair.getPrivate().getEncoded();
            byte[] encryptedData = encrypt(privateKey, message.getBytes());
            return new SimpleEntry<>(encryptedData, publicKey);
        } catch (Exception e) {
            e.printStackTrace();
            return new SimpleEntry<>(new byte[]{}, new byte[]{});
        }
    }

    static String decryptMessage(byte[] msg, byte[] key) {
        try {
            byte[] decryptedData = decrypt(key, msg);
            return new String(decryptedData);
        } catch (Exception e) {
            return "";
        }
    }
}
