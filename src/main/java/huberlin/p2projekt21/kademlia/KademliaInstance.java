package huberlin.p2projekt21.kademlia;

import datagrams.helpers.MessageConstants;
import datagrams.messages.*;

import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class KademliaInstance implements Runnable{

    //public static final int NODE_ID_LENGTH = 255;   // TODO set 256; quickfix for too short kademliaID
    public static final int NODE_ID_LENGTH = 63;   // TODO testing
    public static final int RANDOM_ID_LENGTH = 159; // TODO set 160; quickfix for wrong randomID
    //public static final int K = 20;
    public static final int K = 10; // TODO testing
    public static final int ALPHA = 3;
    // determines how often bootstrapping is retried [1..]
    public static final int BOOTSTRAPPING_TRIES = 5;
    // timeout until bootstrapping is retried in millis
    public static final long BOOTSTRAPPING_TIMEOUT = 60 * 1000;
    // timeout until new lookup messages are send in millis
    public static final long POLL_TIMEOUT = 1000;
    // timeout until lookup-request is considered unanswered in millis
    public static final long REQUEST_TIMEOUT = 2 * 1000;

    private final ConcurrentLinkedDeque<DatagramPacket> incomingChannel;
    private final ConcurrentLinkedDeque<DatagramPacket> outgoingChannel;
    private final Random random;
    private final BigInteger ownID;
    private final AtomicBoolean running;
    private final Thread mainThread;
    private final Logger logger;
    private BackgroundTasks backgroundTasks;
    private final LocalHashTable localHashTable;

    private final KBuckets kBuckets;
    // maps randomIDs to (receiverID, sendTime, receiverChannel)
    private final Map<BigInteger, RequestCookie> requestMap;
    // holds BlockingQueues to allow for transferring incoming lookup responses to the correct lookup request
    private final Map<Long, BlockingQueue<GenericMessage>> lookupChannels;

    /**
     * Create a new Kademlia instance
     * to start the background task call object.start() afterwards
     *
     * @param incomingChannel datagram channel for incoming messages
     * @param outgoingChannel datagram channel for outgoing messages
     */
    public KademliaInstance(ConcurrentLinkedDeque<DatagramPacket> incomingChannel, ConcurrentLinkedDeque<DatagramPacket> outgoingChannel) {
        this.incomingChannel = incomingChannel;
        this.outgoingChannel = outgoingChannel;
        random = new Random();
        ownID = new BigInteger(NODE_ID_LENGTH, random);
        running = new AtomicBoolean(true);
        mainThread = new Thread(this);
        logger = Logger.getGlobal();
        logger.info("ownID: " + ownID.toString(16));
        localHashTable = new LocalHashTable();

        kBuckets = new KBuckets(K, NODE_ID_LENGTH, ownID);
        requestMap = new ConcurrentHashMap<>();
        lookupChannels = new ConcurrentHashMap<>();
    }

    @Override
    public void run() {
        logger.info("kademlia started");
        while (running.get()) {
            // retrieve next incoming message
            if (incomingChannel.isEmpty()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            DatagramPacket datagram = incomingChannel.poll();
            if (datagram == null) continue;

            // decode incoming message
            try {
                //System.out.println(Arrays.toString(datagram.getData()));

                // TODO remove; quickfix for datagram to long for parser
                var data = datagram.getData();
                int end = data.length;
                while (end > 0 && data[end-1] == 0) end--;
                var newData = Arrays.copyOf(data, end);
                datagram.setData(newData);
                // TODO remove end

                GenericMessage msg = new GenericMessage();
                msg.fromDatagramPacket(datagram);
                logger.info("message received\n" + msg.getRandomID().toString());

                if (!decodeMessage(msg)) continue;

                // update kBucket
                KademliaNode node = kBuckets.update(new KademliaNode(msg.getSenderNodeID(), msg.getSenderIP(), msg.getSenderPort()), System.currentTimeMillis());
                if (node != null) {
                    GenericMessage ping = new GenericMessage(null, -1, node.getAddress(), node.getPort());
                    ping.setSenderNodeID(ownID);
                    BigInteger randomId = getRandomID();
                    requestMap.put(randomId, new RequestCookie(node.getId(), System.currentTimeMillis(), -1));
                    ping.setRandomID(randomId);
                    ping.setTypeHeader(MessageConstants.TYPE_PING);
                    outgoingChannel.add(ping.toDatagram());
                    logger.info("ping node " + node.getId().toString(16));
                }

            } catch (Exception e) {
                e.printStackTrace();
                // just skip msg?
                logger.warning("discarded message\n" + e.getMessage());
            }
        }

        // kademlia stopping
        assert(!running.get());
        // TODO do clean-up stuff here
    }

    /**
     * Decode and handle message
     *
     * @param msg next message
     * @return false if decoding failed
     * @throws Exception thrown by GenericMessage
     */
    private boolean decodeMessage(GenericMessage msg) throws Exception {
        // switch message type
        byte type = msg.getTypeHeader().byteValueExact();

        switch (type) {
            case MessageConstants.TYPE_PING -> {
                logger.info("ping received");
                // reply Pong
                GenericMessage reply = new GenericMessage(null, -1, msg.getSenderIP(), msg.getSenderPort());
                reply.setRandomID(msg.getRandomID());
                reply.setSenderNodeID(this.ownID);
                reply.setTypeHeader(MessageConstants.TYPE_PONG);
                outgoingChannel.add(reply.toDatagram());
                logger.info("pong replied");
            }
            case MessageConstants.TYPE_PONG -> {
                logger.info("pong received");
                // check randomID
                RequestCookie cookie = checkRandomID(msg.getSenderNodeID(), msg.getRandomID());
                if (cookie == null) {
                    // not requested (or too long ago) -> skip
                    logger.warning("no cookie available");
                    return false;
                }
            }
            case MessageConstants.TYPE_STORE_ENTRYKEY -> {
                logger.info("store received");
                // parse payload
                EntryKey payload = (EntryKey) msg.getPayload();
                if (payload == null) return false;
                BigInteger key = payload.getEntryKey();
                byte[] value = payload.getEntryValue();
                // store <key, value>
                localHashTable.store(key, value);
            }
            // TODO Store reply missing? probably not even necessary
            case MessageConstants.TYPE_FINDNODE -> {
                logger.info("findNode received");
                // parse payload
                NodeID payloadMsg = (NodeID) msg.getPayload();
                if (payloadMsg == null) return false;
                List<KademliaNode> list = kBuckets.lookup(payloadMsg.getNodeID(), K);
                // reply FindNodeR
                Tuples payloadRep = new Tuples();
                for (var elem : list) payloadRep.addTuple(elem.getPort(), elem.getAddress(), new NodeID(elem.getId()));
                GenericMessage reply = new GenericMessage(null, -1, msg.getSenderIP(), msg.getSenderPort());
                reply.setRandomID(msg.getRandomID());
                reply.setSenderNodeID(this.ownID);
                reply.setTypeHeader(MessageConstants.TYPE_FINDNODE_R);
                reply.setPayload(payloadRep);
                outgoingChannel.add(reply.toDatagram());
                logger.info("findNodeR replied");
            }
            case MessageConstants.TYPE_FINDNODE_R -> {
                logger.info("findNodeR received");
                // check randomID
                RequestCookie cookie = checkRandomID(msg.getSenderNodeID(), msg.getRandomID());
                if (cookie == null) {
                    // not requested (or too long ago) -> skip
                    logger.warning("no cookie available");
                    return false;
                }

                // forward message to corresponding lookup handle
                long lookupChannelID = cookie.lookupChanelID;
                var channel = lookupChannels.get(lookupChannelID);
                if (channel != null) {
                    logger.info("placing message in channel " + lookupChannelID);
                    channel.add(msg);
                } else {
                    logger.warning("channel removed " + lookupChannelID);
                }
            }
            case MessageConstants.TYPE_FINDVALUE -> {
                logger.info("findValue received");
                GenericMessage reply = handleFindValue(msg);
                if (reply != null) outgoingChannel.add(reply.toDatagram());
                else return false;
            }
            case MessageConstants.TYPE_FINDVALUE_R -> {
                logger.info("findValueR received");
                // check randomID
                RequestCookie cookie = checkRandomID(msg.getSenderNodeID(), msg.getRandomID());
                if (cookie == null) {
                    // not requested (or too long ago) -> skip
                    logger.warning("no cookie available");
                    return false;
                }
                // parse payload
                EntryValue payloadMsg = (EntryValue) msg.getPayload();
                if (payloadMsg == null) return false;

                // forward message to corresponding lookup handle
                long lookupChannelID = cookie.lookupChanelID;
                logger.info("placing message in channel " + lookupChannelID);
                var channel = lookupChannels.get(lookupChannelID);
                if (channel != null) {
                    logger.info("placing message in channel " + lookupChannelID);
                    channel.add(msg);
                } else {
                    logger.warning("channel removed " + lookupChannelID);
                }
            }
            default -> {
                logger.warning("unsupported received");
                return false;
            }
        }
        return true;
    }

    /**
     * Start kademlia (main) thread
     * At first bootstrapping is executed
     * (address == null skips bootstrapping)
     * After it finished, start main thread
     *
     * @param address InetAddress of the bootstrapping peer (null -> no bootstrapping)
     * @param port port of the bootstrapping peer
     */
    public void start(InetAddress address, int port){
        logger.info("starting kademlia");
        bootstrapping(address, port);
    }

    /**
     * Eventually stops the kademlia execution
     */
    public void stop() {
        logger.info("stopping kademlia");
        this.running.set(false);
    }

    /**
     * Get the associated value for the given key
     * null is returned if no value could be found
     *
     * !!!WARNING: Will block for possibly a really long time!!!
     * !!!only call from asynchronous context!!!
     *
     * @param key search key
     * @return data associated with the key (or null)
     * @throws Exception thrown by GenericMessage
     */
    public byte[] getValue(BigInteger key) throws Exception {
        // search locally
        byte[] value = localHashTable.load(key);
        if (value == null) {
            // not found locally -> search in network
            value = valueLookup(key);
        }
        return value;
    }

    /**
     * Store the given key-value pair
     *
     * !!!WARNING: Will block for possibly a really long time!!!
     * !!!only call from asynchronous context!!!
     *
     * @param key key for the given value
     * @param value byte representation of the value to be stored
     * @return true if stored in network, false if only stored locally
     * @throws Exception thrown by GenericMessage
     */
    public boolean store(BigInteger key, byte[] value) throws Exception {
        logger.info("store(" + key.toString(16) + ")");
        // store locally
        localHashTable.store(key, value);
        // store in network
        List<KademliaNode> nodes = nodeLookup(key);
        if (nodes == null || nodes.size() == 0) {
            logger.warning("Could not store message in the network");
            return false;
        }
        for (var node : nodes) {
            GenericMessage store = new GenericMessage(null, -1, node.getAddress(), node.getPort());
            store.setTypeHeader(MessageConstants.TYPE_STORE_ENTRYKEY);
            BigInteger randomID = getRandomID();
            store.setRandomID(randomID);
            store.setSenderNodeID(ownID);
            EntryKey payload = new EntryKey(key);
            payload.setEntryValue(value);
            store.setPayload(payload);
            // TODO uncomment if storeR is added
            // requestMap.put(randomID, new RequestCookie(node.getId(), System.currentTimeMillis(), -1));
            outgoingChannel.add(store.toDatagram());
            logger.info("send store to " + node.getId().toString(16));
        }
        return true;
    }

    public BigInteger getOwnID() {
        return ownID;
    }

    /**
     * Try connecting to a existing kademlia-dht
     * Address == null skips bootstrapping and instead starting a new dht
     * @param address address of a node in the dht
     * @param port port of a node in the dht
     */
    private void bootstrapping(InetAddress address, int port) {
        if (address == null) {
            logger.info("skip bootstrapping");
            // start main thread
            logger.info("starting main thread");
            this.running.set(true);
            this.mainThread.start();
            backgroundTasks = new BackgroundTasks(kBuckets, requestMap, this::nodeLookup, ownID);
            return;
        }
        logger.info("start bootstrapping");
        assert (port >= 0);
        assert (port <= 65535);

        // bootstrapping asynchronously to allow main thread to continue
        new Thread(() -> {
            try {
                for (int tries = 0; tries < BOOTSTRAPPING_TRIES; tries++) {
                    BigInteger randomID = bootstrappingPing(address, port);     // send ping
                    if (bootstrappingWait(randomID)) {                          // answer received?
                        // start main thread
                        logger.info("starting main thread");
                        this.running.set(true);
                        this.mainThread.start();
                        backgroundTasks = new BackgroundTasks(kBuckets, requestMap, this::nodeLookup, ownID);
                        nodeLookup(ownID);
                        logger.info("bootstrapping finished");
                        break;
                    } else if (tries == BOOTSTRAPPING_TRIES-1) {
                        logger.warning("bootstrapping failed");
                    }
                }
            } catch (Exception e) {
                logger.warning("bootstrapping failed\n" + e.getMessage());
                e.printStackTrace();
            }

        }).start();
    }

    /**
     * Bootstrapping: send ping
     *
     * @param address bootstrapping ip address
     * @param port bootstrapping port
     * @return randomId of the send ping
     * @throws Exception thrown by GenericMessage
     */
    private BigInteger bootstrappingPing(InetAddress address, int port) throws Exception {
        GenericMessage ping = new GenericMessage(null, -1, address, port);
        // send Ping to bootstrapping address (to get first kademlia id)
        ping.setSenderNodeID(ownID);
        BigInteger randomId = getRandomID();
        ping.setRandomID(randomId);
        ping.setTypeHeader(MessageConstants.TYPE_PING);
        outgoingChannel.add(ping.toDatagram());
        logger.info("bootstrapping ping message send\nmsg: " + ping.getRandomID().toString() + "\nexpected: " + randomId.toString());
        return randomId;
    }

    /**
     * Bootstrapping: Wait for pong
     * returns after BOOTSTRAPPING_TIMEOUT or when correct answer received
     *
     * @param randomId randomId of the ping
     * @return true if successful, false if timeout exceeded
     * @throws Exception thrown by GenericMessage
     */
    private boolean bootstrappingWait(BigInteger randomId) throws Exception {
        long end = System.currentTimeMillis() + BOOTSTRAPPING_TIMEOUT;

        // wait for (correct) response
        while (System.currentTimeMillis() < end) {
            if (!incomingChannel.isEmpty()) {
                logger.info("received reply");
                // handle pong
                GenericMessage pong = new GenericMessage();
                DatagramPacket response = incomingChannel.poll();
                assert (response != null);
                pong.fromDatagramPacket(response);
                if (pong.getTypeHeader().byteValueExact() != MessageConstants.TYPE_PONG) {
                    logger.warning("no pong!");
                    continue;
                }
                if (!pong.getRandomID().equals(randomId)) {
                    logger.warning("wrong randomID\nis: " + pong.getRandomID().toString() + "\nexpected: " + randomId.toString());
                    continue;
                }
                // add node to kBuckets
                kBuckets.update(new KademliaNode(pong.getSenderNodeID(), pong.getSenderIP(), pong.getSenderPort()), System.currentTimeMillis());
                logger.info("pong received");
                return true;
            }
            Thread.sleep(100);
        }

        // no response received until timeout
        logger.warning("ping timeout!");
        return false;
    }

    /**
     * Find the k closest to id nodes in the network
     * cf. 2.2 Kademlia protocol
     * !!!WARNING: Will block for possibly a really long time!!!
     * !!!only call from asynchronous context!!!
     *
     * @param id reference node to find closest nodes
     * @return list of the k closest nodes
     */
    private List<KademliaNode> nodeLookup(BigInteger id) throws Exception {
        logger.info("nodeLookup(" + id.toString(16) + ")");
        kBuckets.nodeLookupPerformed(id);
        // create lookup id (to be able to forward incoming replies to the correct node-/valueLookup
        long lookupId;
        BlockingQueue<GenericMessage> channel = new LinkedBlockingDeque<>();
        do {
            lookupId = random.nextLong();
        } while (lookupChannels.putIfAbsent(lookupId, channel) != null);

        // initialize list of closest nodes (and status)
        // status{ 0->new; >0-> asked (number corresponds to ask-time); -1->answered }
        Map<KademliaNode, Long> closest = new TreeMap<>(DistanceComparator.getCompareKademliaDistances(id));
        var tmp = kBuckets.lookup(id, K);
        if (tmp.size() == 0) {
            logger.warning("lookup ended as kBuckets are empty");
            return new ArrayList<>();
        }
        for (var elem : tmp) closest.put(elem, (long) 0);

        // recursive until closest k nodes responded:
        while (true) {
            // send FindNode/FindValue to alpha closest (not yet queried) nodes from list
            var iterator = closest.entrySet().iterator();
            int send = 0;
            while (send < ALPHA && iterator.hasNext()) {
                Map.Entry<KademliaNode, Long> next = iterator.next();
                if (next.getValue() == 0) {
                    GenericMessage findNode = new GenericMessage(null, -1, next.getKey().getAddress(), next.getKey().getPort());
                    findNode.setTypeHeader(MessageConstants.TYPE_FINDNODE);
                    BigInteger randomID = getRandomID();
                    findNode.setRandomID(randomID);
                    findNode.setSenderNodeID(ownID);
                    NodeID nodeID = new NodeID(id);
                    findNode.setPayload(nodeID);
                    // register message for own lookupChannel
                    requestMap.put(randomID, new RequestCookie(next.getKey().getId(), System.currentTimeMillis(), lookupId));
                    outgoingChannel.add(findNode.toDatagram());
                    //System.out.println(Arrays.toString(findNode.toDatagram().getData()));
                    closest.put(next.getKey(), System.currentTimeMillis());   // change status to asked
                    send++;
                    logger.info("send findNode to " + next.getKey().getId().toString(16));
                }
            }

            // wait for incoming replies (TIMEOUT -> continue, edge case all queried)
            GenericMessage next = channel.poll(POLL_TIMEOUT, TimeUnit.MILLISECONDS);
            if (next == null) {
                // timeout exceeded -> send further messages
                logger.warning("no reply received (timeout)");
            } else {

                // mark as answered
                closest.put(new KademliaNode(next.getSenderNodeID(), next.getSenderIP(), next.getSenderPort()), (long) -1);
                logger.info("received response " + next.getSenderNodeID().toString(16));

                // get received nodes
                Tuples payload = (Tuples) next.getPayload();
                Tuples.Tuple[] tuples;
                if (payload == null) {
                    tuples = new Tuples.Tuple[0];
                    logger.warning("received 0 nodes");
                } else {
                    tuples = payload.getTuplesAsArray();
                    logger.warning("received " + tuples.length + " nodes");
                }

                // transforms array of Tuples to list of KademliaNodes
                var nodeList = Arrays.stream(tuples.clone()).map(t -> new KademliaNode(t.getNodeID().getNodeID(), t.getIpAddress(), t.getPort())).collect(Collectors.toList());

                for (KademliaNode node : nodeList) {
                    if (node.getId().equals(ownID)) continue;   // skip own id
                    // add to kBucket
                    kBuckets.update(node, 0);
                    // add to closest (if not present)
                    closest.putIfAbsent(node, (long) 0);
                    logger.info("added to closest: " + node.getId().toString(16));
                }
            }

            // remove 'silent' nodes from list
            long time = System.currentTimeMillis();
            int count = 0;
            var removeIterator = closest.entrySet().iterator();
            while (removeIterator.hasNext()) {
                var entry = removeIterator.next();
                long entryVal = entry.getValue();
                if (entryVal > 0 && time > entryVal+REQUEST_TIMEOUT) {  // Request unanswered -> remove
                    removeIterator.remove();
                    count++;
                }
            }
            logger.info("removed " + count + " silent nodes");

            // TODO remove elements if list is very long (e.g. 10*K)?

            // check recursion end
            iterator = closest.entrySet().iterator();
            boolean allAnswered = true;
            for (int i = 0; i < K && iterator.hasNext(); i++) {
                if (iterator.next().getValue() != -1) {
                    // one of the closest K nodes has not answered yet
                    allAnswered = false;
                    break;
                }
            }
            if (allAnswered) {
                // closest K nodes answered -> finish
                logger.info("K closest nodes answered -> nodeLookup finished");
                break;
            }
        }

        kBuckets.nodeLookupPerformed(id);
        lookupChannels.remove(lookupId);
        // collects first (closest) K entries of 'closest' and returns their keys
        return closest.entrySet().stream().limit(K).map(Map.Entry::getKey).collect(Collectors.toList());
    }

    /**
     * Find value for the specified key
     * cf. 2.2 Kademlia protocol
     * !!!WARNING: Will block for possibly a really long time!!!
     * !!!only call from asynchronous context!!!
     *
     * @param key (hash)key of the desired value
     * @return value for that key (or null if not found)
     */
    private byte[] valueLookup(BigInteger key) throws Exception {
        logger.info("valueLookup(" + key.toString(16) + ")");
        kBuckets.nodeLookupPerformed(key);
        // create lookup id (to be able to forward incoming replies to the correct node-/valueLookup
        long lookupId;
        BlockingQueue<GenericMessage> channel = new LinkedBlockingDeque<>();
        do {
            lookupId = random.nextLong();
        } while (lookupChannels.putIfAbsent(lookupId, channel) != null);

        // initialize list of closest nodes (and status)
        // status{ 0->new; >0-> asked (number corresponds to ask-time); -1->answered }
        Map<KademliaNode, Long> closest = new TreeMap<>(DistanceComparator.getCompareKademliaDistances(key));
        var tmp = kBuckets.lookup(key, K);
        if (tmp.size() == 0) {
            logger.warning("lookup ended as kBuckets are empty");
            return null;
        }
        for (var elem : tmp) closest.put(elem, (long) 0);

        byte[] value = null;

        // recursive until closest k nodes responded:
        while (true) {
            // send FindNode/FindValue to alpha closest (not yet queried) nodes from list
            var iterator = closest.entrySet().iterator();
            int send = 0;
            while (send < ALPHA && iterator.hasNext()) {
                Map.Entry<KademliaNode, Long> next = iterator.next();
                if (next.getValue() == 0) {
                    GenericMessage findValue = new GenericMessage(null, -1, next.getKey().getAddress(), next.getKey().getPort());
                    findValue.setTypeHeader(MessageConstants.TYPE_FINDVALUE);
                    BigInteger randomID = getRandomID();
                    findValue.setRandomID(randomID);
                    findValue.setSenderNodeID(ownID);
                    NodeID nodeID = new NodeID(key);
                    findValue.setPayload(nodeID);
                    // register message for own lookupChannel
                    requestMap.put(randomID, new RequestCookie(next.getKey().getId(), System.currentTimeMillis(), lookupId));
                    outgoingChannel.add(findValue.toDatagram());
                    //System.out.println(Arrays.toString(findNode.toDatagram().getData()));
                    closest.put(next.getKey(), System.currentTimeMillis());   // change status to asked
                    send++;
                    logger.info("send findValue to " + next.getKey().getId().toString(16));
                }
            }

            // wait for incoming replies (TIMEOUT -> continue, edge case all queried)
            GenericMessage next = channel.poll(POLL_TIMEOUT, TimeUnit.MILLISECONDS);
            if (next == null) {
                // timeout exceeded -> send further messages
                logger.warning("no reply received (timeout)");
            } else {
                // mark as answered
                closest.put(new KademliaNode(next.getSenderNodeID(), next.getSenderIP(), next.getSenderPort()), (long) -1);
                logger.info("received response " + next.getSenderNodeID().toString(16));

                // get message type
                switch (next.getTypeHeader().byteValueExact()) {
                    case MessageConstants.TYPE_FINDNODE_R -> {
                        // get received nodes
                        Tuples payload = (Tuples) next.getPayload();
                        Tuples.Tuple[] tuples;
                        if (payload == null) {
                            tuples = new Tuples.Tuple[0];
                            logger.warning("received 0 nodes");
                        } else {
                            tuples = payload.getTuplesAsArray();
                            logger.warning("received " + tuples.length + " nodes");
                        }

                        // transforms array of Tuples to list of KademliaNodes
                        var nodeList = Arrays.stream(tuples.clone()).map(t -> new KademliaNode(t.getNodeID().getNodeID(), t.getIpAddress(), t.getPort())).collect(Collectors.toList());

                        for (KademliaNode node : nodeList) {
                            if (node.getId().equals(ownID)) continue;   // skip own id
                            // add to kBucket
                            kBuckets.update(node, 0);
                            // add to closest (if not present)
                            closest.putIfAbsent(node, (long) 0);
                            logger.info("added to closest: " + node.getId().toString(16));
                        }
                    }
                    case MessageConstants.TYPE_FINDVALUE_R -> {
                        EntryValue entryValue = (EntryValue) next.getPayload();
                        byte[] tmpValue = entryValue.getEntryValue();
                        // TODO compare date -> only override if newer
                        if (value == null /* || tmpValue newer than value*/) {
                            value = tmpValue;
                            // TODO remove return (newer values could exist)
                            return value;
                        }
                    }
                }
            }

            // remove 'silent' nodes from list
            long time = System.currentTimeMillis();
            int count = 0;
            var removeIterator = closest.entrySet().iterator();
            while (removeIterator.hasNext()) {
                var entry = removeIterator.next();
                long entryVal = entry.getValue();
                if (entryVal > 0 && time > entryVal+REQUEST_TIMEOUT) {  // Request unanswered -> remove
                    removeIterator.remove();
                    count++;
                }
            }
            logger.info("removed " + count + " silent nodes");

            // TODO remove elements if list is very long (e.g. 10*K)?

            // check recursion end
            iterator = closest.entrySet().iterator();
            boolean allAnswered = true;
            for (int i = 0; i < K && iterator.hasNext(); i++) {
                if (iterator.next().getValue() != -1) {
                    // one of the closest K nodes has not answered yet
                    allAnswered = false;
                    break;
                }
            }
            if (allAnswered) {
                // closest K nodes answered -> finish
                logger.info("K closest nodes answered -> nodeLookup finished");
                break;
            }
        }

        kBuckets.nodeLookupPerformed(key);
        lookupChannels.remove(lookupId);
        // return found value (or null if non found)
        return value;
    }

    /**
     * Generate new RandomID
     * @return new RandomID
     */
    private BigInteger getRandomID() {
        return new BigInteger(RANDOM_ID_LENGTH, random);
    }

    /**
     * Checks if the reply answers a prior request
     *
     * @param otherID KademliaId of the replying node
     * @param randomID RandomId of the reply
     * @return the corresponding RequestCookie if a prior request, null otherwise
     */
    private RequestCookie checkRandomID(BigInteger otherID, BigInteger randomID) {
        RequestCookie cookie = requestMap.remove(randomID);
        if (cookie == null) return null;
        if (!cookie.nodeID.equals(otherID)) return null;
        return cookie;
    }

    /**
     * Handle FindValue request
     * If ths value for the given key is stored locally -> reply FindValueR
     * Otherwise reply FindNodeR
     *
     * @param request FindValue request
     * @return GenericMessage response (or null if failed)
     * @throws Exception thrown by GenericMessage
     */
    private GenericMessage handleFindValue(GenericMessage request) throws Exception {
        // parse payload
        NodeID payloadReq = (NodeID) request.getPayload();
        if (payloadReq == null) return null;

        byte[] value = localHashTable.load(payloadReq.getNodeID());
        GenericMessage reply = new GenericMessage(null, -1, request.getSenderIP(), request.getSenderPort());
        reply.setRandomID(request.getRandomID());
        reply.setSenderNodeID(request.getSenderNodeID());
        if (value == null) {    // value not available  -> FindNodeR
            logger.info("value not available -> answer findNodeR");
            reply.setTypeHeader(MessageConstants.TYPE_FINDNODE_R);
            List<KademliaNode> list = kBuckets.lookup(payloadReq.getNodeID(), K);
            Tuples payloadRes = new Tuples();
            for (var elem : list) payloadRes.addTuple(elem.getPort(), elem.getAddress(), new NodeID(elem.getId()));
            reply.setPayload(payloadRes);
        } else {                // value available      -> FindValueR
            logger.info("value available -> answer findValueR");
            reply.setTypeHeader(MessageConstants.TYPE_FINDVALUE_R);
            EntryValue payloadRes = new EntryValue(true, value);
            reply.setPayload(payloadRes);
        }
        return reply;
    }

    public static class RequestCookie {
        public BigInteger nodeID;
        public long sendTime;
        public long lookupChanelID;

        public RequestCookie(BigInteger nodeID, long sendTime, long lookupChanelID) {
            this.nodeID = nodeID;
            this.sendTime = sendTime;
            this.lookupChanelID = lookupChanelID;
        }
    }
}
