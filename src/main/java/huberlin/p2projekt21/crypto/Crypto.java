package huberlin.p2projekt21.crypto;

import huberlin.p2projekt21.properties.PropertiesSingleton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Class containing functions for data verification
 */
public class Crypto {

    /**
     * This method verifies a signature of the given data in regards to a public key
     *
     * @param data      Data used for signature verification
     * @param signature Signature used for verification
     * @param key       Public key for signature verification
     * @return true if signature is valid, false otherwise
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws SignatureException
     */
    public static boolean verify(byte[] data, byte[] signature, PublicKey key) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature sigInstance = Signature.getInstance("SHA256withRSA");
        sigInstance.initVerify(key);
        sigInstance.update(data);
        return sigInstance.verify(signature);
    }

    /**
     * This method computes the signature for a given Private key and data
     *
     * @param data Data which is to be signed
     * @param key  Private key used to sign the data
     * @return a byte array containing the signature for the data
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws SignatureException
     */
    public static byte[] sign(byte[] data, PrivateKey key) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature sigInstance = Signature.getInstance("SHA256withRSA");
        sigInstance.initSign(key);
        sigInstance.update(data);

        return sigInstance.sign();
    }

    /**
     * This method takes an amount of data and returns a signature using the private key stored on the local machine
     *
     * @param data Data to be signed with own private key
     * @return The signature for the given amount of data
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws SignatureException
     * @throws IOException
     * @throws InvalidKeySpecException
     */
    public static byte[] signWithStoredKey(byte[] data) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException, InvalidKeySpecException {
        PrivateKey key = getStoredPrivateKey();
        Signature sigInstance = Signature.getInstance("SHA256withRSA");
        sigInstance.initSign(key);
        sigInstance.update(data);

        return sigInstance.sign();
    }

    /**
     * This method generates an RSA key pair and persists the keys on the local machine
     *
     * @return The generated key pair if no key pair was found on the machine, otherwise null
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public static KeyPair createPair() throws NoSuchAlgorithmException, IOException {
        PropertiesSingleton props = PropertiesSingleton.getInstance();
        String privateKeyString = props.get("PRIVATE_KEY_FILE");
        String publicKeyString = props.get("PUBLIC_KEY_FILE");
        String keySize = props.get("KEY_SIZE");
        assert (!privateKeyString.isBlank());
        assert (!publicKeyString.isBlank());
        assert (!keySize.isBlank());

        Path privateKeyFile = Paths.get(privateKeyString);
        Path publicKeyFile = Paths.get(publicKeyString);

        if (Files.notExists(privateKeyFile) && Files.notExists(publicKeyFile)) {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(Integer.parseInt(keySize), new SecureRandom());
            KeyPair pair = generator.generateKeyPair();

            Files.createFile(privateKeyFile);
            Files.createFile(publicKeyFile);
            Files.write(privateKeyFile, pair.getPrivate().getEncoded());
            Files.write(publicKeyFile, pair.getPublic().getEncoded());

            return pair;
        } else {
            return null;
        }
    }

    /**
     * This method computes the numeric hash code of the entered public key
     *
     * @param key Key for which the hash shall be computed
     * @return hash code of the public key
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public static int getPublicKeyHash(byte[] key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(key));
        return publicKey.hashCode();
    }

    /**
     * This method reads and returns the public key stored on the local machine
     *
     * @return The stored public key
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public static PublicKey getStoredPublicKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        PropertiesSingleton props = PropertiesSingleton.getInstance();
        String keyString = props.get("PUBLIC_KEY_FILE");
        assert (!keyString.isBlank());

        Path keyFile = Paths.get(keyString);
        byte[] publicKey = Files.readAllBytes(keyFile);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey));
    }

    /**
     * This method reads and returns the private key stored on the local machine
     *
     * @return The stored private key
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public static PrivateKey getStoredPrivateKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        PropertiesSingleton props = PropertiesSingleton.getInstance();
        String keyString = props.get("PRIVATE_KEY_FILE");
        assert (!keyString.isBlank());

        Path keyFile = Paths.get(keyString);
        byte[] privateKey = Files.readAllBytes(keyFile);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKey));
    }
}
