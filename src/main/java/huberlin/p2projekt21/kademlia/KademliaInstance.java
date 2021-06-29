package huberlin.p2projekt21.kademlia;

import datagrams.helpers.MessageConstants;
import datagrams.messages.EntryValue;
import datagrams.messages.GenericMessage;
import datagrams.messages.NodeID;
import datagrams.messages.Tuples;

import java.awt.*;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class KademliaInstance implements Runnable{

    public static final int NODE_ID_LENGTH = 256;
    public static final int RANDOM_ID_LENGTH = 160;
    public static final int K = 20;
    public static final int ALPHA = 3;
    public static final int BOOTSTRAPPING_TRIES = 5;
    public static final long BOOTSTRAPPING_TIMEOUT = 60 * 1000;

    private final ConcurrentLinkedDeque<DatagramPacket> incomingChannel;
    private final ConcurrentLinkedDeque<DatagramPacket> outgoingChannel;
    private final Random random;
    private final BigInteger ownID;
    private final AtomicBoolean running;
    private final Thread mainThread;

    private final KBuckets kBuckets;
    private final Map<BigInteger, BigInteger> requestMap;

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
                    requestMap.put(randomId, node.getId());
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
                if (!checkRandomID(msg.getSenderNodeID(), msg.getRandomID())) {
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
                if (!checkRandomID(msg.getSenderNodeID(), msg.getRandomID())) {
                    // not requested (or too long ago) -> skip
                    return false;
                }
                // parse payload
                Tuples payloadMsg = (Tuples) msg.getPayload();
                if (payloadMsg == null) return false;

                // TODO handle FindNodeR
                // TODO forward to specific nodeLookup

            }
            case MessageConstants.TYPE_FINDVALUE -> {
                GenericMessage reply = handleFindValue(msg);
                if (reply != null) outgoingChannel.add(reply.toDatagram());
                else return false;
            }
            case MessageConstants.TYPE_FINDVALUE_R -> {
                // check randomID
                if (!checkRandomID(msg.getSenderNodeID(), msg.getRandomID())) {
                    // not requested (or too long ago) -> skip
                    return false;
                }
                // parse payload
                EntryValue payloadMsg = (EntryValue) msg.getPayload();
                if (payloadMsg == null) return false;

                // TODO handle FindValueR
                // TODO forward to specific nodeLookup
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
                    if (bootstrappingWait(randomID)) break;                             // answer received?
                }
                // TODO bootstrapping failed
            } catch (Exception e) {
                // TODO bootstrapping failed
                e.printStackTrace();
            }

            //noinspection ResultOfMethodCallIgnored
            lookup(ownID);
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

    /**
     * Find the k closest to id nodes in the network
     * cf. 2.2 Kademlia protocol
     *
     * @param id reference node to find closest nodes
     * @return list of the k closest nodes
     */
    private List<KademliaNode> lookup(BigInteger id) {
        // TODO local: list = lookup(id, alpha)
        // TODO recursive until closest k nodes responded:
            // TODO async: send FindNode/FindValue to alpha closest (not yet queried) nodes from list
            // TODO register send messages for task particular receive channel
            // TODO wait for incoming replies (TIMEOUT -> continue, edge case all queried)
            // TODO add received nodes to list
            // TODO remove 'silent' nodes from list

        return null;
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
     * @return true if requested, false otherwise
     */
    private boolean checkRandomID(BigInteger otherID, BigInteger randomID) {
        BigInteger value = requestMap.get(otherID);
        return value != null && value.equals(randomID);
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
    public byte[] getValueStub(BigInteger key) {
        // TODO
        return null;
    }
}
