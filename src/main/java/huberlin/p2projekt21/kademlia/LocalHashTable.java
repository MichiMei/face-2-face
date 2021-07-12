package huberlin.p2projekt21.kademlia;

import huberlin.p2projekt21.Helper;
import huberlin.p2projekt21.crypto.Crypto;
import huberlin.p2projekt21.storage.Storage;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class maintains a Hashtable of stored data.
 * It is used for quick lookup, if the desired data is stored.
 * Furthermore it is used to allow different instances to run independently with different data-elements stored.
 */
public class LocalHashTable {

    private final Map<BigInteger, Long> localStorage;

    public LocalHashTable() {
        localStorage = new ConcurrentHashMap<>();
    }

    /**
     * Loads the data for the given key
     *
     * @param key key of the desired data
     * @return Data object or null if not found
     */
    public Data load(BigInteger key) {
        if (localStorage.containsKey(key)) {
            try {
                byte[][] tmp = Storage.read(key);
                return new Data(tmp);
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    /**
     * Stores the given data if the signature is correct and no newer version is already stored
     *
     * @param data data to be stored
     * @throws IOException .
     * @throws InvalidKeySpecException .
     * @throws NoSuchAlgorithmException .
     * @throws SignatureException .
     * @throws InvalidKeyException .
     */
    public void store(Data data) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        if (!Crypto.verify(data.getPage().toBytes(), data.getSignature(), Helper.getPublicKeyFromBytes(data.getPublicKey()))) {
            // signature is wrong
            return;
        }
        Long time = localStorage.get(data.getKeyHash());
        if (time != null && time >= data.page.getTimeStamp()) {
            // stored one is newer
            return;
        }
        localStorage.put(data.getKeyHash(), data.page.getTimeStamp());
        Storage.store(data.getKeyHash(), data.getPage().toBytes(), data.getSignature(), data.getPublicKey());
    }

}
