package huberlin.p2projekt21.kademlia;

import java.math.BigInteger;
import java.util.Comparator;

public class DistanceComparator {
    /**
     * Compares nodes considering their distance to the reference id
     *
     * @param id reference id
     * @return Comparator
     */
    public static Comparator<KademliaNode> getCompareKademliaDistances(BigInteger id) {
        return (n1, n2) -> {
            BigInteger d1 = n1.getId().xor(id);
            BigInteger d2 = n2.getId().xor(id);
            return d1.compareTo(d2);
        };
    }
}