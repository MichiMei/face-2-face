package huberlin.p2projekt21;

import huberlin.p2projekt21.kademlia.KBuckets;
import huberlin.p2projekt21.kademlia.KademliaNode;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;

import static huberlin.p2projekt21.kademlia.KBuckets.getCompareKademliaDistances;

/**
 * Unit test KBuckets
 */
public class KBucketTest {

    @Test
    public void checkNodeValueComparator() {
        long t1 = System.currentTimeMillis();
        long t0 = t1-100;
        long t2 = t1+100;

        KBuckets.KBucket.NodeValues nv0 = new KBuckets.KBucket.NodeValues(BigInteger.valueOf(1), null, -1, t1);
        KBuckets.KBucket.NodeValues nv1 = new KBuckets.KBucket.NodeValues(BigInteger.valueOf(2), null, -1, t1);
        KBuckets.KBucket.NodeValues nv2 = new KBuckets.KBucket.NodeValues(BigInteger.valueOf(3), null, -1, t0);
        KBuckets.KBucket.NodeValues nv3 = new KBuckets.KBucket.NodeValues(BigInteger.valueOf(4), null, -1, t2);

        assert(nv0.compareTo(null) < 0);
        assert(nv0.compareTo(nv1) == 0);
        assert(nv0.compareTo(nv2) > 0);
        assert(nv0.compareTo(nv3) < 0);
    }

    @Test
    public void kademliaDistanceComparatorTest() {
        BigInteger reference = BigInteger.valueOf(3);
        KademliaNode n0 = new KademliaNode(BigInteger.valueOf(1), null, -1);
        KademliaNode n1 = new KademliaNode(BigInteger.valueOf(2), null, -1);
        KademliaNode n2 = new KademliaNode(BigInteger.valueOf(3), null, -1);
        ArrayList<KademliaNode> list = new ArrayList<>(3);
        list.add(n0);list.add(n1);list.add(n2);

        list.sort(getCompareKademliaDistances(reference));

        assert (list.get(0) == n2);
        assert (list.get(1) == n1);
        assert (list.get(2) == n0);
    }

    @Test
    public void t() {
        int cacheSize = 3;
        int cachePos = cacheSize-1;

        for (int i = 0; i < 1000; i++) {
            int prev = cachePos;
            cachePos = (cachePos+1)%cacheSize;
            assert (cachePos >= 0 && cachePos < cacheSize);
            assert (Math.abs(prev-cachePos) == 1 || (prev == cacheSize-1 && cachePos == 0));
        }

        for (int i = 0; i < 1000; i++) {
            int prev = cachePos;
            cachePos = (cachePos-1+cacheSize)%cacheSize;
            assert (cachePos >= 0 && cachePos < cacheSize);
            assert (Math.abs(prev-cachePos) == 1 || (prev == 0 && cachePos == cacheSize-1));
        }
    }

}
