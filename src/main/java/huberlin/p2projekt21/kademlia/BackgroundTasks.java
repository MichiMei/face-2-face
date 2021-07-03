package huberlin.p2projekt21.kademlia;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Manages different Kademlia Background-tasks
 */
public class BackgroundTasks{

    public static final long PING_TIMEOUT = 2 * 1000;                   // 2 sec
    public static final long K_BUCKET_LOOKUP_TIMEOUT = 60 * 60 * 1000;  // 1 hour
    public static final long K_BUCKET_LOOKUP_DELAY = 10 * 60 * 1000;    // 10 min (delay lookup, when network is small)
    public static final long UNANSWERED_TIMEOUT = 10 * 1000;            // 10 seconds

    // TODO implement regular-ping

    private final BGT_UnansweredRequests unansweredRequests;
    private final BGT_KBucketLookup kBucketLookup;

    /**
     * Creates and starts all Background tasks
     */
    public BackgroundTasks(KBuckets kBuckets, Map<BigInteger, KademliaInstance.RequestCookie> requestMap, NodeLookupMethod method, BigInteger ownID) {
        unansweredRequests = new BGT_UnansweredRequests(kBuckets, requestMap);
        kBucketLookup = new BGT_KBucketLookup(kBuckets, method, ownID);
    }

    /**
     * Eventually stop the BG-tasks
     */
    public void stop() {
        unansweredRequests.stop();
        kBucketLookup.stop();
    }

    public interface NodeLookupMethod {
        List<KademliaNode> nodeLookup(BigInteger id) throws Exception;
    }

    public static class BGT_KBucketLookup implements Runnable {
        private final KBuckets kBuckets;
        private final NodeLookupMethod nodeLookupMethod;
        private final BigInteger ownID;

        private final Logger logger;
        private final AtomicBoolean running;
        private final int kBucketCount;
        private final Random random;

        /**
         * Creates and starts a background task initiating regular lookups in kBuckets
         *
         * @param kBuckets kBuckets reference
         */
        public BGT_KBucketLookup(KBuckets kBuckets, NodeLookupMethod nodeLookupMethod, BigInteger ownID) {
            this.kBuckets = kBuckets;
            this.nodeLookupMethod = nodeLookupMethod;
            this.ownID = ownID;
            logger = Logger.getGlobal();
            running = new AtomicBoolean(true);
            kBucketCount = kBuckets.getBucketCount();
            random = new Random();
            new Thread(this).start();
            logger.info("started BGT_KBucketLookup");
        }

        @Override
        public void run() {
            while (running.get()) {
                long currentTime = System.currentTimeMillis();

                // counts how often less than K nodes are returned
                // when a lookup returns with less than K nodes, the next one will most likely too (N < K)
                // to reduce unnecessary lookups, delay lookup in such situations
                int count = 0;
                for (int i = 0; i < kBucketCount; i++) {    // iterate over kBuckets
                    long lastTime = kBuckets.getLastLookup(i);
                    if (lastTime+K_BUCKET_LOOKUP_TIMEOUT < currentTime) {   // Timeout exceeded -> perform lookup
                        try {
                            var res = nodeLookupMethod.nodeLookup(randomIdForBucket(i));
                            // check if last lookup returned less than K items
                            if (res.size() < KademliaInstance.K) count++;
                            else count = 0;
                        } catch (Exception e) {
                            e.printStackTrace();
                            logger.warning("nodeLookup failed\n" + e.getMessage());
                        }
                    }
                    if (count >= 3) {   // last 3 lookups less than K elements -> delay next lookup
                        try {
                            //noinspection BusyWait
                            Thread.sleep(K_BUCKET_LOOKUP_DELAY);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            logger.warning("sleep interrupted\n" + e.getMessage());
                        }
                    }
                }
                try {
                    //noinspection BusyWait
                    Thread.sleep(K_BUCKET_LOOKUP_TIMEOUT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    logger.warning("sleep interrupted\n" + e.getMessage());
                }
            }
            logger.info("stopped BGT_KBucketLookup");
        }

        /**
         * Calculates a random ID in the bounds of kBucket[index]
         *
         * @param index index of the specific bucket
         * @return a random id in the range of the bucket
         */
        private BigInteger randomIdForBucket(int index) {
            assert (index >= 0);
            assert (index < kBucketCount);

            // remove 'random bits' by shifting
            BigInteger result = ownID.shiftRight(index);
            // invert 'sibling sub-tree bit'
            result = result.flipBit(0);
            // shift back
            result = result.shiftLeft(index);

            // verify borders
            assert (kBuckets.bucketID(result) == index);
            assert (kBuckets.bucketID(result.subtract(BigInteger.ONE)) != index);
            BigInteger tmp = result.add(BigInteger.ONE.shiftLeft(index).subtract(BigInteger.ONE));
            assert (kBuckets.bucketID(tmp) == index);
            assert (kBuckets.bucketID(tmp.add(BigInteger.ONE)) != index);

            // fill remaining bits randomly
            BigInteger rnd = new BigInteger(index, random);
            result = result.add(rnd);
            assert (kBuckets.bucketID(result) == index);

            return result;
        }

        /**
         * Eventually stop the BG-task
         */
        public void stop() {
            running.set(false);
        }

    }

    public static class BGT_UnansweredRequests implements Runnable {

        private final KBuckets kBuckets;
        private final Map<BigInteger, KademliaInstance.RequestCookie> requestMap;

        private final Logger logger;
        private final AtomicBoolean running;

        /**
         * Creates and starts a unanswered request cleaning background task
         *
         * @param kBuckets kBuckets reference to remove not answering nodes
         * @param requestMap list of send requests
         */
        public BGT_UnansweredRequests(KBuckets kBuckets, Map<BigInteger, KademliaInstance.RequestCookie> requestMap) {
            this.kBuckets = kBuckets;
            this.requestMap = requestMap;
            logger = Logger.getGlobal();
            running = new AtomicBoolean(true);
            new Thread(this).start();
            logger.info("started BGT_UnansweredRequests");
        }

        @Override
        public void run() {
            while (running.get()) {
                long currentTime = System.currentTimeMillis();
                for (var entry : requestMap.entrySet()) {
                    if (entry.getValue().sendTime + UNANSWERED_TIMEOUT < currentTime) {   // Timeout exceeded
                        logger.info("timeout exceeded\nreceiver: " + entry.getValue().nodeID.toString(16) + "\nrandomID: " + entry.getKey().toString(16));
                        kBuckets.pingExpired(entry.getValue().nodeID);                      // replace node with waiting
                        requestMap.remove(entry.getKey(), entry.getValue());                // remove request
                    }
                }
                try {
                    //noinspection BusyWait
                    Thread.sleep(UNANSWERED_TIMEOUT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    logger.warning("sleep interrupted\n" + e.getMessage());
                }
            }
            logger.info("stopped BGT_UnansweredRequests");
        }

        /**
         * Eventually stop the BG-task
         */
        public void stop() {
            running.set(false);
        }
    }
}
