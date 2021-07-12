package huberlin.p2projekt21.kademlia;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.*;
import java.util.logging.Logger;

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
     * @param node kademlia-node to update
     * @param ls time-stamp of the received message
     * @return InetSocketAddress of a node to ping (can be null)
     */
    public KademliaNode update(KademliaNode node, long ls) {
        if (node.getId().equals(referenceID)) return null;  // don't insert own id
        int bucketID = bucketID(node.getId());
        assert (bucketID >= 0);
        assert (bucketID < bucketCount);

        KademliaNode res = buckets[bucketID].update(node, ls);
        long size = 0;
        for (var bucket : buckets) size += bucket.size();
        Logger.getGlobal().info("KBuckets size: " + size);
        return res;
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
        int closest = Math.max(bucketID(id), 0);    // can be -1 if id==referenceID
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
        if (id.equals(referenceID)) return;  // skip own id
        int index = bucketID(id);
        assert (index >= 0 && index < bucketCount);
        buckets[index].pingExpired(id);
    }

    /**
     * Get the time in millis of the last performed nodeLookup on the specified kBucket
     *
     * @param index index of the specific kBucket
     * @return time of the last nodeLookup in millis
     */
    public long getLastLookup(int index) {
        assert (index >= 0);
        assert (index < bucketCount);
        return buckets[index].getLastLookup();
    }

    /**
     * Notify bucket of performed nodeLookup
     */
    public void nodeLookupPerformed(BigInteger id) {
        if (id.equals(referenceID)) return; // skip own id
        int index = bucketID(id);
        buckets[index].nodeLookupPerformed();
    }

    /**
     * Returns number of kBuckets
     *
     * @return number of kBuckets
     */
    public int getBucketCount() {
        return bucketCount;
    }

    /**
     * Returns all nodes of the specified kBucket, with no received messages since the specified time
     *
     * @param index index of the desired kBucket
     * @param time specified time
     * @return list of inactive nodes
     */
    public List<KademliaNode> getInactive(int index, long time) {
        assert (index >= 0);
        assert (index < bucketCount);
        return buckets[index].getInactive(time);
    }

    /**
     * Calculate the id of the bucket responsible for the given ID
     *
     * @param id ID of a searched node or value
     * @return bucket responsible for the id (-1 is returned when id==referenceID)
     */
    public int bucketID(BigInteger id) {
        BigInteger dist = referenceID.xor(id);
        return dist.bitLength()-1;
    }

    public static class KBucket {
        private final int bucketLength;

        private final ArrayList<NodeValues> elements;
        private final NodeValues[] cache;   // Override-Ring-Buffer
        private int cachePos;
        private long lastLookup;

        public KBucket(int k, int cL) {
            this.bucketLength = k;
            elements = new ArrayList<>(1);
            cache = new NodeValues[cL];
            cachePos = cL-1;
            lastLookup = 0;
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

        /**
         * Get number of elements contained
         *
         * @return number of elements
         */
        public synchronized long size() {
            return elements.size();
        }

        /**
         * Get the time in millis of the last performed nodeLookup
         *
         * @return time of the last nodeLookup in millis
         */
        public synchronized long getLastLookup() {
            return lastLookup;
        }

        /**
         * Notify bucket of performed nodeLookup
         */
        public synchronized void nodeLookupPerformed() {
            this.lastLookup = System.currentTimeMillis();
        }

        /**
         * Returns all nodes of the this kBucket, with no received messages since the specified time
         *
         * @param time specified time
         * @return list of inactive nodes
         */
        public synchronized List<KademliaNode> getInactive(long time) {
            List<KademliaNode> result = new ArrayList<>();
            for (var node : elements) {
                if (node.inactiveSince(time)) result.add(node.node);
            }
            return result;
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

            public boolean inactiveSince(long time) {
                return ls < time;
            }
        }
    }

}
