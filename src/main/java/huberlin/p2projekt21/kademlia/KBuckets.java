package huberlin.p2projekt21.kademlia;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.*;

public class KBuckets {
    public static final int CACHE_SIZE = 3;

    private final int bucketCount;
    private final BigInteger referenceID;

    private final KBucket[] buckets;

    /**
     * Create new KBucket with i buckets of length k
     * @param k length of each bucket
     * @param i number of buckets (IDs == [0, 2^i))
     * @param ownID own kademlia id as reference for distance calculation
     */
    public KBuckets(int k, int i, BigInteger ownID) {
        this.bucketCount = i;
        this.referenceID = ownID;

        buckets = new KBucket[bucketCount];
        for (int j = 0; j < bucketCount; j++) {
            buckets[j] = new KBucket(k, CACHE_SIZE);
        }
    }

    /**
     * Kademlia received a message of the given node
     * If the node is already contained in a bucket, update that entry
     * If the appropriate bucket has free space, insert that node
     * If the appropriate bucket contains dead nodes, replace them with this node
     *
     * @param id kademlia node id of the node
     * @param address ip-address of the node
     * @param port port of the node
     * @param ls time-stamp of the received message
     * @return InetSocketAddress of a node to ping (can be null)
     */
    public KademliaNode update(BigInteger id, InetAddress address, int port, long ls) {
        int bucketID = bucketID(id);
        assert (bucketID > 0);
        assert (bucketID < bucketCount);
        return buckets[bucketID].update(new KademliaNode(id, address, port), ls);
    }

    /**
     * Kademlia received a message of the given node
     * If the node is already contained in a bucket, update that entry
     * If the appropriate bucket has free space, insert that node
     * If the appropriate bucket contains dead nodes, replace them with this node
     *
     * @param node kademlia-node to update
     * @param ls time-stamp of the received message
     * @return InetSocketAddress of a node to ping (can be null)
     */
    public KademliaNode update(KademliaNode node, long ls) {
        int bucketID = bucketID(node.getId());
        assert (bucketID > 0);
        assert (bucketID < bucketCount);
        return buckets[bucketID].update(node, ls);
    }

    /**
     * Return the "number" closest nodes to "id"
     *
     * @param id searched id
     * @param number number of desired nodes
     * @return List of [0,number] nodes
     */
    public List<KademliaNode> lookup(BigInteger id, int number) {
        // get number elements of closest bucket
        int closest = bucketID(id);
        List<KademliaNode> result = buckets[closest].lookup(id, number);

        // fill with "smaller" buckets
        int index = closest-1;
        while (index >= 0 && result.size() < number) {
            var tmp = buckets[index].lookup(id, number-result.size());
            result.addAll(tmp);
            index--;
        }

        // fill with "larger" buckets
        index = closest+1;
        while (index < buckets.length && result.size() < number) {
            var tmp = buckets[index].lookup(id, number-result.size());
            result.addAll(tmp);
            index++;
        }

        assert (result.size() <= number);

        return result;
    }

    /**
     * Ping to node with 'id' expired
     * Replace with 'waiting' node
     *
     * @param id of the pinged node
     */
    public void pingExpired(BigInteger id) {
        int index = bucketID(id);
        assert (index >= 0 && index < bucketCount);
        buckets[index].pingExpired(id);
    }

    /**
     * Calculate the id of the bucket responsible for the given ID
     *
     * @param id ID of a searched node or value
     * @return bucket responsible for the id
     */
    private int bucketID(BigInteger id) {
        BigInteger dist = referenceID.xor(id);
        return dist.bitLength()-1;
    }

    public static class KBucket {
        private final int bucketLength;

        private final ArrayList<NodeValues> elements;
        private final NodeValues[] cache;   // Override-Ring-Buffer
        private int cachePos;

        public KBucket(int k, int cL) {
            this.bucketLength = k;
            elements = new ArrayList<>(1);
            cache = new NodeValues[cL];
            cachePos = cL-1;
        }

        /**
         * Kademlia received a message of the given node
         * If the node is already contained in this bucket, update that entry
         * If this bucket has free space, insert that node
         * Otherwise add to cache and return node to ping
         *
         * @param node kademlia node to update
         * @param ls time-stamp of the received message
         * @return InetSocketAddress of a node to ping (can be null)
         */
        public synchronized KademliaNode update(KademliaNode node, long ls) {
            // check if contained
            for (var elem : elements) {
                if (elem.getId().equals(node.getId())) {
                    elem.update(ls);
                    return null;
                }
            }

            // free space
            if (elements.size() < bucketLength) {
                elements.add(new NodeValues(node, ls));
                return null;
            }

            // add to cache, return "oldest" nodes address
            cachePos = (cachePos+1)%cache.length;   // move cache-pointer to next position
            assert (cachePos >= 0 && cachePos < cache.length);
            cache[cachePos] = new NodeValues(node, ls);

            NodeValues oldest = null;
            for (var elem : elements) {
                if (elem.compareTo(oldest) < 0) oldest = elem;
            }
            assert (oldest != null);
            return oldest.node;
        }

        /**
         * Return the "number" closest nodes to "id"
         *
         * @param id searched id
         * @param number number of desired nodes
         * @return List of [0,number] nodes
         */
        public List<KademliaNode> lookup(BigInteger id, int number) {
            List<KademliaNode> result = new ArrayList<>(elements.size());
            synchronized (this) {
                for (var elem : elements) result.add(elem.node);
            }
            if (result.size() > number) {
                result.sort(DistanceComparator.getCompareKademliaDistances(id));
                result = result.subList(0, number);
            }
            assert (result.size() <= number);
            return result;
        }

        /**
         * Ping to node with 'id' expired
         * Replace with 'waiting' node from cache
         *
         * @param id of the pinged node
         */
        public synchronized void pingExpired(BigInteger id) {
            if (cache[cachePos] == null) return;
            for (int i = 0; i < elements.size(); i++) {
                if (elements.get(i).getId().equals(id)) {
                    // replace with newest cached node
                    elements.set(i, cache[cachePos]);
                    // remove node from cache
                    cache[cachePos] = null;
                    cachePos = (cachePos-1+cache.length)%cache.length;  // move cache-pointer to previous element
                    assert (cachePos >= 0 && cachePos < cache.length);
                    return;
                }
            }
        }

        public static class NodeValues implements Comparable<NodeValues> {
            private final KademliaNode node;
            private long ls;

            public NodeValues(BigInteger id, InetAddress address, int port, long ls) {
                this(new KademliaNode(id, address, port), ls);
            }

            public NodeValues(KademliaNode node, long ls) {
                this.node = node;
                this.ls = ls;
            }

            public void update(long ls) {
                this.ls = ls;
            }

            @Override
            public int compareTo(NodeValues o) {
                if (o == null) return -1;
                if (this.equals(o)) return 0;

                return Long.compare(this.ls, o.ls);
            }

            public BigInteger getId() {
                return this.node.getId();
            }

            public InetAddress getAddress() {
                return this.node.getAddress();
            }

            public int getPort() {
                return this.node.getPort();
            }
        }
    }

}
