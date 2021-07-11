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
     * @return new RandomID
     */
    public static BigInteger getRandomID() {
        return new BigInteger(KademliaInstance.RANDOM_ID_LENGTH, random);
    }

}
