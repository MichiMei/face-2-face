package huberlin.p2projekt21.kademlia;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is mainly for testing purposes and may be removed later on
 */
public class LocalHashTable {

    private final Map<BigInteger, byte[]> localStorage;

    public LocalHashTable() {
        localStorage = new ConcurrentHashMap<>();
    }

    public byte[] load(BigInteger key) {
        return localStorage.get(key);
    }

    public void store(BigInteger key, byte[] value) {
        localStorage.put(key, value);
    }

}
