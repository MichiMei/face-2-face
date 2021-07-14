package huberlin.p2projekt21;

import huberlin.p2projekt21.kademlia.KademliaInstance;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Random;

public class Helper {

    private static Random random = new Random();

    /**
     * Calculates the 256-bit hash for the given public key
     *
     * @param key public key
     * @return 256bit hash for the given public key, null if failed
     */
    public static byte[] hashForKey(PublicKey key) throws NoSuchAlgorithmException {
        byte[] bytesKey = key.getEncoded();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(bytesKey);
        assert(hash.length == 256/8);
        return hash;
    }

    /**
     * Calculates the 256-bit hash for the given public key
     *
     * @param key public key
     * @return 256bit hash for the given public key as BigInteger, null if failed
     */
    public static BigInteger bigIntHashForKey(PublicKey key) throws NoSuchAlgorithmException {
        byte[] bytes = hashForKey(key);
        return new BigInteger(1, bytes);
    }

    /**
     * Generates PublicKey object from byte representation
     * @param key byte representation of PublicKey
     * @return key as PublicKey object
     * @throws NoSuchAlgorithmException .
     * @throws InvalidKeySpecException .
     */
    public static PublicKey getPublicKeyFromBytes(byte[] key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(key));
    }

    /**
     * Generate new RandomID
     *
     * @return new RandomID
     */
    public static BigInteger getRandomID() {
        return new BigInteger(KademliaInstance.RANDOM_ID_LENGTH, random);
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    /**
     * Transforms byte array into hex string
     * <p>
     * Source: maybeWeCouldStealAVan,
     * https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
     *
     * @param bytes byte array
     * @return Hex-string representation
     */
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Converts BigInteger to byte array
     *
     * @param bigInt BigInteger to be converted
     * @param length length of resulting byte array
     * @return byte array
     */
    public static byte[] bigIntToByteArray(BigInteger bigInt, int length) {
        byte[] out = new byte[length];
        byte[] bigIntArr = bigInt.toByteArray();
        int arrLen = bigIntArr.length;

        if (bigIntArr.length == length) {
            return bigIntArr;
        } else if (bigIntArr.length < length) {
            System.arraycopy(bigIntArr, 0, out, length - arrLen, arrLen);
        } else {
            System.arraycopy(bigIntArr, 1, out, 0, length);
        }
        return out;
    }

}
