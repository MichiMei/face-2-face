package huberlin.p2projekt21.kademlia;

import huberlin.p2projekt21.storage.Storage;

import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is mainly for testing purposes and may be removed later on
 */
public class LocalHashTable {

    private final Set<BigInteger> localStorage;

    public LocalHashTable() {
        localStorage = ConcurrentHashMap.newKeySet();
    }

    public Data load(BigInteger key) {
        if (localStorage.contains(key)) {
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

    public void store(Data data) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        localStorage.add(data.getKeyHash());
        Storage.store(data.getKeyHash(), data.getPage().toBytes(), data.getSignature(), data.getPublicKey());
    }

}
