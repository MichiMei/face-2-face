package huberlin.p2projekt21.kademlia;

import datagrams.helpers.MessageConstants;
import datagrams.messages.EntryValue;
import datagrams.messages.GenericMessage;
import datagrams.messages.NodeID;
import datagrams.messages.Tuples;

import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class KademliaInstance implements Runnable{

    public static final int NODE_ID_LENGTH = 256;
    public static final int RANDOM_ID_LENGTH = 160;
    public static final int K = 20;
    public static final int ALPHA = 3;
    public static final int BOOTSTRAPPING_TRIES = 5;
    public static final long BOOTSTRAPPING_TIMEOUT = 60 * 1000;     // in millis
    public static final long POLL_TIMEOUT = 1000;                   // in millis
    public static final long REQUEST_TIMEOUT = 10 * 1000;           // in millis

    private final ConcurrentLinkedDeque<DatagramPacket> incomingChannel;
    private final ConcurrentLinkedDeque<DatagramPacket> outgoingChannel;
    private final Random random;
    private final BigInteger ownID;
    private final AtomicBoolean running;
    private final Thread mainThread;

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
     * @param address InetAddress of the bootstrapping peer (null -> no bootstrapping)
     * @param port port of the bootstrapping peer
     */
    public KademliaInstance(ConcurrentLinkedDeque<DatagramPacket> incomingChannel, ConcurrentLinkedDeque<DatagramPacket> outgoingChannel, InetAddress address, int port) {
        this.incomingChannel = incomingChannel;
        this.outgoingChannel = outgoingChannel;
        random = new Random();
        ownID = new BigInteger(NODE_ID_LENGTH, random);
        running = new AtomicBoolean(true);
        mainThread = new Thread(this);

        kBuckets = new KBuckets(K, NODE_ID_LENGTH, ownID);
        requestMap = new ConcurrentHashMap<>();
        lookupChannels = new ConcurrentHashMap<>();

        bootstrapping(address, port);
    }

    @Override
    public void run() {
        while (running.get()) {
            // retrieve next incoming message
            if (incomingChannel.isEmpty()) continue;
            DatagramPacket datagram = incomingChannel.poll();
            if (datagram == null) continue;

            // decode incoming message
            try {
                GenericMessage msg = new GenericMessage();

                if (decodeMessage(msg)) continue;

                // update kBucket
                KademliaNode node = kBuckets.update(msg.getSenderNodeID(), msg.getSenderIP(), msg.getSenderPort(), System.currentTimeMillis());
                if (node != null) {
                    GenericMessage ping = new GenericMessage(null, -1, node.getAddress(), node.getPort());
                    ping.setSenderNodeID(ownID);
                    BigInteger randomId = getRandomID();
                    requestMap.put(randomId, new RequestCookie(node.getId(), System.currentTimeMillis(), -1));
                    ping.setRandomID(randomId);
                    ping.setTypeHeader(MessageConstants.TYPE_PING);
                    outgoingChannel.add(ping.toDatagram());
                }

            } catch (Exception e) {
                e.printStackTrace();
                // just skip msg?
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
                // reply Pong
                GenericMessage reply = new GenericMessage(null, -1, msg.getSenderIP(), msg.getSenderPort());
                reply.setRandomID(msg.getRandomID());
                reply.setSenderNodeID(this.ownID);
                reply.setTypeHeader(MessageConstants.TYPE_PONG);
                outgoingChannel.add(reply.toDatagram());
            }
            case MessageConstants.TYPE_PONG -> {
                // check randomID
                RequestCookie cookie = checkRandomID(msg.getSenderNodeID(), msg.getRandomID());
                if (cookie == null) {
                    // not requested (or too long ago) -> skip
                    return false;
                }
            }
            case MessageConstants.TYPE_STORE_ENTRYKEY -> {
                // TODO parse payload
                // TODO store message
            }
            // TODO Store reply missing
            case MessageConstants.TYPE_FINDNODE -> {
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
            }
            case MessageConstants.TYPE_FINDNODE_R -> {
                // check randomID
                RequestCookie cookie = checkRandomID(msg.getSenderNodeID(), msg.getRandomID());
                if (cookie == null) {
                    // not requested (or too long ago) -> skip
                    return false;
                }
                // parse payload
                Tuples payloadMsg = (Tuples) msg.getPayload();
                if (payloadMsg == null) return false;

                // forward message to corresponding lookup handle
                long lookupChannelID = cookie.lookupChanelID;
                lookupChannels.get(lookupChannelID).add(msg);
            }
            case MessageConstants.TYPE_FINDVALUE -> {
                GenericMessage reply = handleFindValue(msg);
                if (reply != null) outgoingChannel.add(reply.toDatagram());
                else return false;
            }
            case MessageConstants.TYPE_FINDVALUE_R -> {
                // check randomID
                RequestCookie cookie = checkRandomID(msg.getSenderNodeID(), msg.getRandomID());
                if (cookie == null) {
                    // not requested (or too long ago) -> skip
                    return false;
                }
                // parse payload
                EntryValue payloadMsg = (EntryValue) msg.getPayload();
                if (payloadMsg == null) return false;

                // forward message to corresponding lookup handle
                long lookupChannelID = cookie.lookupChanelID;
                lookupChannels.get(lookupChannelID).add(msg);
            }
            default -> {
                // TODO handle unsupported message
                return false;
            }
        }
        return true;
    }

    /**
     * Start kademlia (main) thread
     */
    public void start(){
        this.running.set(true);
        this.mainThread.start();
    }

    /**
     * Eventually stops the kademlia execution
     */
    public void stop() {
        this.running.set(false);
    }

    /**
     * Get the associated value for the given key
     * null is returned if no value could be found
     * @param key search key
     * @return data associated with the key (or null)
     */
    public byte[] getValue(BigInteger key) {
        // TODO
        return null;
    }

    /**
     * Store the given key-value pair
     * @param key key for the given value
     * @param data byte representation of the data to be stored
     * @return TODO probably return void?
     */
    public boolean store(BigInteger key, byte[] data) {
        // TODO
        return false;
    }

    /**
     * Try connecting to a existing kademlia-dht
     * Address == null skips bootstrapping and instead starting a new dht
     * @param address address of a node in the dht
     * @param port port of a node in the dht
     */
    private void bootstrapping(InetAddress address, int port) {
        if (address == null) {
            return;
        }
        assert (port >= 0);
        assert (port <= 65535);

        // bootstrapping asynchronously to allow main thread to continue
        new Thread(() -> {
            try {
                for (int tries = 0; tries < BOOTSTRAPPING_TRIES; tries++) {
                    BigInteger randomID = bootstrappingPing(address, port);     // send ping
                    if (bootstrappingWait(randomID)) {                          // answer received?
                        nodeLookup(ownID);
                        return;
                    }
                }
                // TODO bootstrapping failed
            } catch (Exception e) {
                // TODO bootstrapping failed
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
                // handle pong
                GenericMessage pong = new GenericMessage();
                DatagramPacket response = incomingChannel.poll();
                assert (response != null);
                pong.fromDatagramPacket(response);
                if (pong.getTypeHeader().byteValueExact() != MessageConstants.TYPE_PONG) continue;
                if (!pong.getRandomID().equals(randomId)) continue;
                // add node to kBuckets
                kBuckets.update(pong.getSenderNodeID(), pong.getSenderIP(), pong.getSenderPort(), System.currentTimeMillis());
                return true;
            }
            Thread.sleep(100);
        }

        // no response received until timeout
        return false;
    }

    // TODO handle valueLookup
    // TODO handle findValueR-messages
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
        // create lookup id
        long lookupId;
        BlockingQueue<GenericMessage> channel = new LinkedBlockingDeque<>();
        do {
            lookupId = random.nextLong();
        } while (lookupChannels.putIfAbsent(lookupId, channel) != null);

        // List of closest nodes (and status)
        // status{ 0->new; >0-> asked (number corresponds to ask-time); -1->answered }
        Map<KademliaNode, Long> closest = new TreeMap<>(DistanceComparator.getCompareKademliaDistances(id));
        var tmp = kBuckets.lookup(id, K);
        for (var elem : tmp) closest.put(elem, (long) 0);

        int kFactor = 1;

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
                    closest.put(next.getKey(), System.currentTimeMillis());   // change status to asked
                    send++;
                }
            }
            // EdgeCase: all nodes asked -> add more nodes
            if (send == 0) {
                kFactor++;
                // get more close nodes
                tmp = kBuckets.lookup(id, K*kFactor);
                for (var elem : tmp) closest.putIfAbsent(elem, (long) 0);
                // ask again
                continue;
            }

            // wait for incoming replies (TIMEOUT -> continue, edge case all queried)
            GenericMessage next = channel.poll(POLL_TIMEOUT, TimeUnit.MILLISECONDS);
            if (next == null) {
                // timeout exceeded -> send further messages
                continue;
            }

            // mark as answered
            closest.put(new KademliaNode(next.getSenderNodeID(), next.getSenderIP(), next.getSenderPort()), (long)-1);

            // get received nodes
            Tuples payload = (Tuples) next.getPayload();
            var tuples = payload.getTuplesAsArray();
            // I'm proud of this... OK?!? (transforms a array of tuples to a map, mapping KademliaNodes to status 0)
            var list = Arrays.stream(tuples.clone()).map(tuple -> new KademliaNode(tuple.getNodeID().getNodeID(), tuple.getIpAddress(), tuple.getPort())).collect(Collectors.toMap(n -> n, n -> (long)0));

            // add to kBuckets
            for (var elem : list.keySet()) {
                kBuckets.update(elem, 0);
            }

            // add nodes to closest list
            closest.putAll(list);

            // remove 'silent' nodes from list
            long time = System.currentTimeMillis();
            for (var entry : closest.entrySet()) {
                if (time > entry.getValue()+REQUEST_TIMEOUT) {  // Request unanswered -> remove
                    closest.remove(entry.getKey());
                }
            }

            // TODO remove elements if list is very long (e.g. 10*K)

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
                break;
            }
        }

        // collects first (closest) K entries of 'closest' and returns their keys
        return closest.entrySet().stream().limit(K).map(Map.Entry::getKey).collect(Collectors.toList());
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

        byte[] value = getValueStub(payloadReq.getNodeID());
        GenericMessage reply = new GenericMessage(null, -1, request.getSenderIP(), request.getSenderPort());
        reply.setRandomID(request.getRandomID());
        reply.setSenderNodeID(request.getSenderNodeID());
        if (value == null) {    // value not available  -> FindNodeR
            reply.setTypeHeader(MessageConstants.TYPE_FINDNODE_R);
            List<KademliaNode> list = kBuckets.lookup(payloadReq.getNodeID(), K);
            Tuples payloadRes = new Tuples();
            for (var elem : list) payloadRes.addTuple(elem.getPort(), elem.getAddress(), new NodeID(elem.getId()));
            reply.setPayload(payloadRes);
        } else {                // value available      -> FindValueR
            reply.setTypeHeader(MessageConstants.TYPE_FINDVALUE_R);
            EntryValue payloadRes = new EntryValue(true, value);
            reply.setPayload(payloadRes);
        }
        return reply;
    }

    /**
     * TODO replace with proper method
     * look for value in storage
     *
     * @param key key of the requested value
     * @return value if available, null otherwise
     */
    private byte[] getValueStub(BigInteger key) {
        // TODO
        return null;
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
