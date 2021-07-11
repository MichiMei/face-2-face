package huberlin.p2projekt21.storage;

import huberlin.p2projekt21.crypto.Crypto;
import huberlin.p2projekt21.properties.PropertiesSingleton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

/**
 * Class containing functions for storing and reading data to/from local directory
 */
public class Storage {


    /**
     * Stores the data and signature for a public key in files named after the hash of said key
     *
     * @param keyHash   Hash of the public key used for store
     * @param data      Data which shall be stored on local machine
     * @param signature Signature of the data
     * @throws IOException
     */
    public static void store(int keyHash, byte[] data, byte[] signature) throws IOException {
        String DATA_PATH = PropertiesSingleton.getInstance().get("DATA_PATH");
        String DATA_FILE_NAME_TEMPLATE = PropertiesSingleton.getInstance().get("DATA_FILE_NAME_TEMPLATE");
        String SIGNATURE_FILE_NAME_TEMPLATE = PropertiesSingleton.getInstance().get("SIGNATURE_FILE_NAME_TEMPLATE");

        Path dataP = Paths.get(DATA_PATH + DATA_FILE_NAME_TEMPLATE.replace("ID", Integer.toString(keyHash)));
        Path sigP = Paths.get(DATA_PATH + SIGNATURE_FILE_NAME_TEMPLATE.replace("ID", Integer.toString(keyHash)));

        Files.write(dataP, data);
        Files.write(sigP, signature);
    }

    /**
     * Stores the data and signature for a public key in files named after the hash of said key
     *
     * @param key       HPublic key used for store
     * @param data      Data which shall be stored on local machine
     * @param signature Signature of the data
     * @throws IOException
     */
    public static void store(byte[] key, byte[] data, byte[] signature) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        int hashCode = Crypto.getPublicKeyHash(key);
        store(hashCode, data, signature);
    }

    /**
     * Generates a signature for the data and stores both named after the hash of own public key
     *
     * @param data Data to be stored as under own id
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws SignatureException
     * @throws InvalidKeyException
     */
    public static void storeOwn(byte[] data) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        String DATA_PATH = PropertiesSingleton.getInstance().get("DATA_PATH");
        int ownId = Crypto.getStoredPublicKey().hashCode();

        byte[] signature = Crypto.signWithStoredKey(data);

        store(ownId, data, signature);
    }

    /**
     * Looks if data and signature for a public key hash are available and returns them if they are
     *
     * @param keyHash Hash of the public key used for lookup
     * @return null if no file was found or an array containing the data and signature (in this order)
     * @throws IOException
     */
    public static byte[][] read(int keyHash) throws IOException {
        String DATA_PATH = PropertiesSingleton.getInstance().get("DATA_PATH");
        String DATA_FILE_NAME_TEMPLATE = PropertiesSingleton.getInstance().get("DATA_FILE_NAME_TEMPLATE");
        String SIGNATURE_FILE_NAME_TEMPLATE = PropertiesSingleton.getInstance().get("SIGNATURE_FILE_NAME_TEMPLATE");

        Path dataP = Paths.get(DATA_PATH + DATA_FILE_NAME_TEMPLATE.replace("ID", Integer.toString(keyHash)));
        Path sigP = Paths.get(DATA_PATH + SIGNATURE_FILE_NAME_TEMPLATE.replace("ID", Integer.toString(keyHash)));

        if (Files.exists(dataP) && Files.exists(sigP)) {
            byte[] data = Files.readAllBytes(dataP);
            byte[] signature = Files.readAllBytes(sigP);
            return new byte[][]{data, signature};
        } else {
            return null;
        }
    }

    /**
     * Looks if data and signature for a public key are available and returns them if they are
     *
     * @param key Public key used for lookup
     * @return null if no file was found or an array containing the data and signature (in this order)
     * @throws IOException
     */
    public static byte[][] read(byte[] key) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        int hashCode = Crypto.getPublicKeyHash(key);
        return read(hashCode);
    }

}
