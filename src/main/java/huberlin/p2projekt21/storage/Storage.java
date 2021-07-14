package huberlin.p2projekt21.storage;

import huberlin.p2projekt21.Helper;
import huberlin.p2projekt21.crypto.Crypto;
import huberlin.p2projekt21.properties.PropertiesSingleton;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

/**
 * Class containing functions for storing and reading data to/from local directory
 */
public class Storage {

    /**
     * Stores the data, signature and signature for a public key in files named after the hash of said key
     * <p>
     * changed to private to prevent different hashes of same keys
     * now only SHA256(PublicKey).hashCode() is allowed
     *
     * @param hashString hex string of public key hash
     * @param data       Data which shall be stored on local machine
     * @param signature  Signature of the data
     * @param key        Public Key of the data
     * @throws IOException .
     */
    private static void store(String hashString, byte[] data, byte[] signature, byte[] key) throws IOException {
        String DATA_PATH = PropertiesSingleton.getInstance().get("DATA_PATH");
        String DATA_FILE_NAME_TEMPLATE = PropertiesSingleton.getInstance().get("DATA_FILE_NAME_TEMPLATE");
        String SIGNATURE_FILE_NAME_TEMPLATE = PropertiesSingleton.getInstance().get("SIGNATURE_FILE_NAME_TEMPLATE");
        String KEY_FILE_NAME_TEMPLATE = PropertiesSingleton.getInstance().get("KEY_FILE_NAME_TEMPLATE");

        Path dataDirP = Paths.get(DATA_PATH);
        if (!Files.exists(dataDirP)) Files.createDirectories(dataDirP);

        Path dataP = Paths.get(DATA_PATH + DATA_FILE_NAME_TEMPLATE.replace("ID", hashString));
        Path sigP = Paths.get(DATA_PATH + SIGNATURE_FILE_NAME_TEMPLATE.replace("ID", hashString));
        Path keyP = Paths.get(DATA_PATH + KEY_FILE_NAME_TEMPLATE.replace("ID", hashString));

        Files.write(dataP, data);
        Files.write(sigP, signature);
        Files.write(keyP, key);
    }

    /**
     * Stores the data and signature for a public key in files named after the hash of said key
     *
     * @param key       HPublic key used for store
     * @param data      Data which shall be stored on local machine
     * @param signature Signature of the data
     * @throws IOException .
     */
    public static void store(byte[] key, byte[] data, byte[] signature) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        //int hashCode = Crypto.getPublicKeyHash(key);
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(key));
        byte[] hashCode = Helper.hashForKey(publicKey);
        String hashString = Helper.bytesToHex(hashCode);
        store(hashString, data, signature, key);
    }

    /**
     * Stores the data and signature for a public key in files named after the hash of said key
     *
     * @param keyHash   SHA256 Hash of Public key used for store
     * @param data      Data which shall be stored on local machine
     * @param signature Signature of the data
     * @throws IOException .
     */
    public static void store(BigInteger keyHash, byte[] data, byte[] signature, byte[] key) throws IOException {
        byte[] hashCode = Helper.bigIntToByteArray(keyHash, 32);
        String hashString = Helper.bytesToHex(hashCode);
        store(hashString, data, signature, key);
    }

    /**
     * Generates a signature for the data and stores both named after the hash of own public key
     *
     * @param data Data to be stored as under own id
     * @throws IOException .
     * @throws NoSuchAlgorithmException .
     * @throws InvalidKeySpecException .
     * @throws SignatureException .
     * @throws InvalidKeyException .
     */
    public static void storeOwn(byte[] data) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        String DATA_PATH = PropertiesSingleton.getInstance().get("DATA_PATH");
        //int ownId = Crypto.getStoredPublicKey().hashCode();
        PublicKey ownKey = Crypto.getStoredPublicKey();
        byte[] hashCode = Helper.hashForKey(ownKey);
        String hashString = Helper.bytesToHex(hashCode);

        byte[] signature = Crypto.signWithStoredKey(data);

        store(hashString, data, signature, ownKey.getEncoded());
    }

    /**
     * Looks if data and signature for a public key hash are available and returns them if they are
     *
     * changed to private to prevent different hashes of same keys
     * now only SHA256(PublicKey).hashCode() is allowed
     *
     * @param hashString Hex string of hash of the public key used for lookup
     * @return null if no file was found or an array containing the data, signature and key (in this order)
     * @throws IOException .
     */
    private static byte[][] read(String hashString) throws IOException {
        String DATA_PATH = PropertiesSingleton.getInstance().get("DATA_PATH");
        String DATA_FILE_NAME_TEMPLATE = PropertiesSingleton.getInstance().get("DATA_FILE_NAME_TEMPLATE");
        String SIGNATURE_FILE_NAME_TEMPLATE = PropertiesSingleton.getInstance().get("SIGNATURE_FILE_NAME_TEMPLATE");
        String KEY_FILE_NAME_TEMPLATE = PropertiesSingleton.getInstance().get("KEY_FILE_NAME_TEMPLATE");

        Path dataP = Paths.get(DATA_PATH + DATA_FILE_NAME_TEMPLATE.replace("ID", hashString));
        Path sigP = Paths.get(DATA_PATH + SIGNATURE_FILE_NAME_TEMPLATE.replace("ID", hashString));
        Path keyP = Paths.get(DATA_PATH + KEY_FILE_NAME_TEMPLATE.replace("ID", hashString));

        if (Files.exists(dataP) && Files.exists(sigP) && Files.exists(keyP)) {
            byte[] data = Files.readAllBytes(dataP);
            byte[] signature = Files.readAllBytes(sigP);
            byte[] key = Files.readAllBytes(keyP);
            return new byte[][]{data, signature, key};
        } else {
            return null;
        }
    }

    /**
     * Looks if data and signature for a public key are available and returns them if they are
     *
     * @param key Public key used for lookup
     * @return null if no file was found or an array containing the data and signature (in this order)
     * @throws IOException .
     */
    public static byte[][] read(byte[] key) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        //int hashCode = Crypto.getPublicKeyHash(key);
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(key));
        byte[] hashCode = Helper.hashForKey(publicKey);
        String hashString = Helper.bytesToHex(hashCode);
        return read(hashString);
    }

    /**
     * Looks if data and signature for a public key are available and returns them if they are
     *
     * @param keyHash SHA256 Hash of Public key used for lookup
     * @return null if no file was found or an array containing the data, signature and key (in this order)
     * @throws IOException .
     */
    public static byte[][] read(BigInteger keyHash) throws IOException {
        byte[] hashCode = Helper.bigIntToByteArray(keyHash, 32);
        String hashString = Helper.bytesToHex(hashCode);
        return read(hashString);
    }
}
